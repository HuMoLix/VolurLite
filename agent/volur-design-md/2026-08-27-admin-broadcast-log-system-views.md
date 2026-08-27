# 后台管理系统：公告管理、系统日志与系统设置三大核心模块实现

## 1. 变更背景与需求
- 完成后台管理三大核心功能模块的完整交互与视觉设计：
  1. **公告管理 ([`AdminBroadcastView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/admin/broadcast/AdminBroadcastView.vue))**：全站系统公告发布、运营活动弹窗推送与 SSE 实时广播记录表格；
  2. **系统日志 ([`AdminLogView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/admin/log/AdminLogView.vue))**：系统审计流水、认证记录、安全拦截、异常报错指标卡片与抽屉详情查看；
  3. **系统设置 ([`AdminSystemView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/admin/system/AdminSystemView.vue))**：基础设施概览卡片 + 分组表单（基础信息、人机验证、SMTP发信、容器策略、全站维护风控模式）。
- 全面参考 [`AdminUserView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/admin/user/AdminUserView.vue) 的上下卡片布局与 Volur Lite 统一主题规范。

---

## 2. 详细设计与实现明细

### 2.1 公告管理模块 ([`AdminBroadcastView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/admin/broadcast/AdminBroadcastView.vue))
- **上部指标**：系统公告数、运营活动数、紧急弹窗数、SSE 实时推送总线在线状态；
- **下部表格**：标题、展示形式 Tag（`MODAL` 居中弹窗、`BANNER` 顶部横幅、`TOAST` 浮层提示、`DRAWER` 侧边抽屉）、正文摘要、优先级、发布人与时间；
- **发布 Modal**：支持选择展示形式、输入封面 URL、跳转链接、优先级及是否强制阻断弹窗，并实时调用 `publishAnnouncementApi` / `publishPromotionApi`。

### 2.2 系统日志模块 ([`AdminLogView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/admin/log/AdminLogView.vue))
- **上部指标**：总日志流水、认证鉴权、安全拦截、系统异常报错 4 大卡片；
- **下部检索表格**：支持关键字（操作者/IP/动作/URL）、日志级别、所属业务模块、执行结果与日期时间段筛选；
- **审计详情抽屉 (`n-drawer`)**：结构化展示请求 URI、方法、客户端 IP、耗时、User-Agent、请求载荷 JSON 高亮代码块及错误堆栈。

### 2.3 系统设置模块 ([`AdminSystemView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/admin/system/AdminSystemView.vue))
- **上部集群健康概览**：内核版本、底层环境、Redis 缓存集群状态、实时推送通道状态；
- **5 大分组配置 Tabs**：
  1. **平台基础设置**：平台全称、描述标语、ICP 备案号、默认语言、开放注册开关；
  2. **人机验证与风控**：滑块拼图容差阈值 (px)、凭证 TTL (s)、最大失败次数、重置密码强制图形验证码；
  3. **邮件服务配置 (SMTP)**：SMTP 服务器、端口、发信账号、授权 Token、SSL/TLS 开关、测试发信联通性；
  4. **容器与靶场策略**：默认容器存活时长 (min)、动态端口映射范围 (20000-30000)、最大并发靶机数、Flag 前缀；
  5. **平台运维与风控**：全站维护模式开关（附高亮警示 Alert）、维护公告、IP 速率限制窗口与上限；
- **底部操作栏**：批量保存配置（调用 `batchUpdateAdminSystemSettingsApi` 热重载生效）与重置默认值。

---

## 3. 验证与审计结论
- **TypeScript 严格类型检查**：无任何类型报错或 `any` 滥用，完整对齐后端 DTO/VO 规范。
- **前端编译构建**：`vue-tsc --noEmit && vite build` 100% 成功构建打包。
