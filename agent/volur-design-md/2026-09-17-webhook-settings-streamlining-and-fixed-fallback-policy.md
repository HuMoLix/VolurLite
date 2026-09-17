# 邮件 Webhook 设置界面精简与备用策略固定化施工文档

## 1. 变更背景

针对系统设置中的邮件管理配置（`MailSettingsTab.vue`）与国际化资源，根据最新业务需求进行界面去冗余与文案规范化：
1. 移除过细或冗长的提示文案（field-hints），保持界面精炼干净；
2. 调整核心标题与标签：将 Webhook 模块标题、启用开关、密钥及地址标签修改为标准直观的称谓；
3. 彻底移除前端对“备用策略”的选择交互，固化备用策略为：未启用 Webhook 回调时，SMTP 投递成功（SMTP 250 OK）即判定为 `DELIVERED`（已送达）；开启 Webhook 回调时，先标记为 `SENT`（已发送），由外部服务商事件推进至 `DELIVERED`、`BOUNCED` 或 `COMPLAINED`。

---

## 2. 详细改动清单

### 2.1 国际化资源更新 (`src/volur-lite-view/src/locales/`)

- **`zh_CN/admin/system.json`**：
  - 修改 `webhook_section_title`：由“邮件投递 Webhook 回调追踪 (Resend / SendGrid / 自建中继)”变更为“**Webhook回调设置**”
  - 修改 `webhook_enabled`：由“启用 Webhook 状态追踪”变更为“**启用 Webhook 回调**”
  - 修改 `webhook_secret`：由“Webhook 验签密钥 / Token”变更为“**Secret Token**”
  - 修改 `webhook_url`：由“系统 Webhook 回调地址”变更为“**Webhook回调地址**”
  - **删除下列冗余文案与键名**：
    - 删除 `webhook_enabled_hint`（“接收邮件服务商的真实入箱/退信/垃圾投诉回调事件以推进终态状态机”）
    - 删除 `webhook_secret_hint`（“对应请求头 X-Webhook-Secret 或 Bearer Token，保障接口防伪造安全”）
    - 删除 `webhook_url_hint`（“在 Resend / SendGrid / 邮件网关管理后台填写此地址以接收投递事件”）
    - 删除 `webhook_fallback`、`webhook_fallback_hint`（“当未开启 Webhook 或外部服务商未提供回调时采取的判定策略”）
    - 删除 `fallback_auto_delivered`、`fallback_keep_sent`
- **`en_US/admin/system.json`**：
  - 同步更新标题、开关标签、Secret Token 与 URL，移除对应 hints 与备用策略选项键名。

### 2.2 前端系统设置视图 (`MailSettingsTab.vue`)

- **删除多余提示说明**：
  - 移除了 Webhook 开关、Secret Token、Webhook 地址下方的 `<span class="field-hint">`。
- **删除“备用策略”选择器**：
  - 移除 `<n-form-item :label="t('admin.system.mail.webhook_fallback')" path="webhookFallbackStrategy">` 表单项。
  - 移除 `fallbackOptions` 计算属性及未使用的 `NSelect` 组件导入。
  - 界面呈现更为紧凑统一。

### 2.3 后端状态机流转固化 (`MailTaskExecutor.java`)

- **固化备用策略判定**：
  - 在异步邮件发送任务完成后，当 `sent == true`（SMTP 250 OK）：
    - 若 `system.mail.webhook_enabled == true`：状态标记为 `SENT`，等待外部回调推进入箱/退信终态；
    - 若 `system.mail.webhook_enabled == false`：备用策略生效，直接标记为 `DELIVERED`。
  - 不再依赖动态的 `fallbackStrategy` 选择，逻辑更可靠明确。

---

## 3. 验证结果

- **后端测试**：`volur-verification` 执行单元测试全部通过（19 测试用例全部 PASS，涵盖 Webhook 密钥鉴权、Resend/通用格式解析、SMTP 投递与滑块拼图验证）。
- **前端构建**：`npm run build` 打包构建成功，耗时 5.48s，无类型与打包异常。
