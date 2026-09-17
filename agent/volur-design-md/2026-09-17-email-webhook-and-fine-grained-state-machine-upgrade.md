# 邮件系统细粒度生命周期状态机与 Webhook 送达追踪架构进阶升级审计报告

## 1. 变更背景与架构升级目标

在传统的基于 SMTP 协议的发信体系中，客户端仅能获知第一跳中继服务器返回的 `250 OK`（即已收件并进入排队）。在此模式下，系统将状态直接判定为“成功”，无法获知邮件在互联网中继中是否发生真实送达、退信（Bounce）或被收件人垃圾邮件投诉（Spam Complaint）。

为了向工业级邮件系统对齐，本次升级落地了：
1. **六态细粒度邮件生命周期状态机**；
2. **多邮件服务商（Resend / SendGrid / 自建中继）标准化 Webhook 回调监听与状态跃迁机制**；
3. **全局唯一定位凭证 `Message-ID` 注入与追踪**；
4. **灵活的双模备用策略（Fallback Strategy）**：当未开启 Webhook 或外部服务商不支持回调时，平滑降级自动判定送达，保障零阻滞体验；
5. **系统管理设置与审计视图全套 UI 联动**。

---

## 2. 核心状态机设计与流转拓扑

```
                           [提交发信任务]
                                 │
                                 ▼
                             (SENDING)  <───┐ (指数退避重试 1s, 2s, 4s)
                                 │          │
                     ┌───────────┴──────────┤
               (投递失败/重试耗尽)          │ (第 1/2 次失败重试)
                     │                      │
                     ▼                      │
                 (FAILED)                   │
                                            │
                        (SMTP 250 OK 接收完成)
                                 │
                 ┌───────────────┴───────────────┐
                 │                               │
        [Webhook 开启且 KEEP_SENT]      [未开启 Webhook 或 AUTO_DELIVERED]
                 │                               │ (备用策略降级)
                 ▼                               ▼
               (SENT)                       (DELIVERED)
                 │
       ┌─────────┼─────────┐
(email.delivered)│         │(email.bounced)
       │         │         │
       ▼         │         ▼
  (DELIVERED)    │     (BOUNCED)
                 │
                 │(email.complained)
                 ▼
            (COMPLAINED)
```

### 状态枚举定义
- **`SENDING`（发送中）**：任务在 Java 25 虚拟线程池中执行或处于指数退避重试中。
- **`SENT`（已转交服务商）**：外部发信服务商（Gmail / Resend）已通过 SMTP 250 OK 接收，正在等待真实送达回调。
- **`DELIVERED`（真实送达）**：收件方服务器确认入箱（Webhook 确认或备用策略判定）。
- **`BOUNCED`（发生退信）**：收件人邮箱不存在、邮箱已满或被外部 MX 强行拒收，记录具体退信原因。
- **`COMPLAINED`（垃圾投诉）**：收件人点击了“举报垃圾邮件”，记录风控事件。
- **`FAILED`（发送失败）**：本地网络中断、SMTP 凭据错误或三次重试全部耗尽。
- **`SUCCESS`**：兼容历史旧数据，业务逻辑中等同于已送达。

---

## 3. 详细改造实施清册

### 3.1 实体扩展与消息追踪 (`EmailLog.java`, `EmailLogVO.java`, `MailMessageBO.java`)
- 在 `EmailLog` 表中新增 `message_id VARCHAR(128)` 字段并建立索引 `idx_email_log_message_id`；
- 在 `SmtpMailDeliveryProvider` 中，为所有向外发出的 MIME 消息显式注入规范的 RFC 5322 头：
  - `Message-ID: <messageId>`
  - `X-Entity-Ref-ID: messageId`
  - `X-Volur-Message-Id: messageId`
- 确保外部服务商在触发 Webhook 回调时携带该唯一 ID，实现精准反查。

### 3.2 动态系统设置扩展 (`SystemSettingKeys.java` & `SystemSettingInitializer.java`)
- 新增统一小写点分规范配置键：
  - `system.mail.webhook_enabled`：是否开启 Webhook 回调监听（默认 `false`）；
  - `system.mail.webhook_secret`：回调验签密钥 / Bearer Token（默认空，配置后严格验签）；
  - `system.mail.webhook_fallback_strategy`：备用策略（默认 `AUTO_DELIVERED` 自动认定送达，支持 `KEEP_SENT` 等待回调）。

### 3.3 状态机执行器与容灾备用策略 (`MailTaskExecutor.java`)
- 虚拟线程发信成功后，动态读取 `system.mail.webhook_enabled` 与 `system.mail.webhook_fallback_strategy`：
  - 若开启 Webhook 且策略为 `KEEP_SENT`：写入 `SENT` 状态；
  - 若未配置 Webhook 或策略为 `AUTO_DELIVERED`：**自动降级写入 `DELIVERED`**，保证普通 SMTP 用户无需额外配置即可获得直观的“已送达”反馈。

### 3.4 Webhook 服务与控制器 (`MailWebhookService.java`, `MailWebhookController.java`)
- 提供开放公网接口：`POST /api/v1/verification/webhook/mail`（已在 Spring Security 白名单中）；
- **安全防护**：配置密钥后校验 `X-Webhook-Secret`、`Authorization: Bearer <secret>` 或 `?secret=...`，非法请求直接驳回 HTTP 401；
- **多平台协议自适应**：
  - 兼容 **Resend**（`type` 字段，如 `email.delivered`, `email.bounced`，提取 `data.email_id` 和 `data.bounce.message`）；
  - 兼容 **SendGrid / 通用格式**（`event` 字段，支持数组批量回调，提取 `sg_message_id`, `email`, `reason`）；
- **双重查找兜底**：优先根据 `message_id` 精确检索；若未匹配则按 `recipient` + 状态在途近 48 小时记录倒序匹配；
- 状态机精准跃迁并留存退信异常原因。

### 3.5 前端系统设置与发信审计重构 (`MailSettingsTab.vue`, `AdminSystemView.vue`, `AdminEmailView.vue`)
- **系统设置 Tab**：
  - 增加 Webhook 回调设置卡片（开关、密钥密码框、备用策略下拉选择器）；
  - 一键复制系统完整 Webhook 回调 URL（`${origin}/api/v1/verification/webhook/mail`），便于直接粘贴至 Resend / SendGrid 控制台。
- **邮件管理视图**：
  - 状态筛选与标签全量支持 6 态色彩标识（`SENDING` 蓝 / `SENT` 橙 / `DELIVERED` 绿 / `BOUNCED` 红 / `COMPLAINED` 红 / `FAILED` 红）；
  - 详情抽屉展示 `Message-ID` 与具体的退信失败详情；
  - 自动轮询机制支持对 `SENDING` 与 `SENT` 状态的在途任务自适应轮询与终态终止。

---

## 4. 验证与测试结果

1. **后端单元与集成测试**：
   - 编写专门的单元测试用例 `MailWebhookServiceTest`，覆盖：
     - Webhook 未开启时静默拦截；
     - Webhook 密钥不匹配阻断；
     - Resend `email.delivered` 正常流转；
     - Resend `email.bounced` 退信原因记录；
     - SendGrid 数组载荷兼容性。
   - 执行全套单元测试：
     ```bash
     mvn test -pl volur-verification -am
     ```
   - **测试结果**：`Tests run: 19, Failures: 0, Errors: 0, Skipped: 0`，全量测试 100% 绿灯通过。
2. **前端类型校验与生产构建**：
   - 执行 `npm run build`：
     ```bash
     npm run build
     ```
   - **构建结果**：TypeScript 严格模式类型校验 100% 通过，生产包打包成功（`✓ built in 5.33s`）。
