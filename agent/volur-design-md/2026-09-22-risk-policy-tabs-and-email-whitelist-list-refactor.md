# 平台安全功能收敛至风控策略设置与邮箱域名白名单动态列表重构施工记录

## 1. 变更背景与架构审计

### 1.1 业务诉求
根据系统安全演进与管理收敛要求，需要对风控管理体系进行以下深度重构：
1. **平台安全限流与防御策略收敛**：将此前分散在“系统设置”平台运维模块中的安全限流配置（单 IP 限流时间窗口、窗口内最大请求数），全面收敛整合至“风控管理 -> 策略设置”（`/admin/risk/rules`）中，确保所有与网络防御、攻击拦截相关的参数在单一业务模块内闭环；
2. **策略设置模块结构化分类 (Tabs)**：在“策略设置”视图中划分选项卡（Tabs），分别设立：
   - **“邮件策略”**：管控全站注册邮箱准入域名；
   - **“拦截策略”**：管控全站接口滑动窗口安全限流、1分钟突发速率中风险阈值、单账号全网分布式撞库中/高风险阈值；
   - **“封禁策略”**：管控连续验证失败升中/高风险阈值、连续失败自动封禁黑名单阈值以及人机挑战成功后的降险恢复步长。
3. **注册邮箱白名单后缀动态列表设计**：将此前暂放在系统基础设置（`GeneralSettingsTab.vue`）中的“注册邮箱白名单后缀”配置迁移至策略设置的“邮件策略”选项卡中。**特别要求**：该功能界面设计为**可动态添加与移除的独立条目列表（明确排除下拉框设计）**，提供直观的域名录入、规则解析、去重校验与条目治理交互。

---

## 2. 核心改动点与实施细节

### 2.1 后端领域模型与服务层升级 (Java & Spring Boot)
1. **领域传输模型对齐与扩展**：
   - 在 [`AdminRiskPolicyVO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/domain/vo/AdminRiskPolicyVO.java) 中增加：
     - `emailWhitelistSuffixes` (`List<String>`): 注册邮箱域名后缀白名单列表；
     - `rateLimitWindow` (`Integer`): 接口安全限流时间窗口（秒）；
     - `rateLimitMaxRequests` (`Integer`): 窗口期内最大允许请求数；
   - 在 [`AdminRiskPolicyDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/domain/dto/AdminRiskPolicyDTO.java) 中对齐上述三个字段，并严格配置 `@Min`、`@Max` 等 Jakarta Validation 校验注解。
2. **服务实现与数据持久化**：
   - 在 [`AdminRiskServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/service/admin/impl/AdminRiskServiceImpl.java) 中：
     - `getPolicy()`: 读取 `SystemSettingKeys.AUTH_EMAIL_WHITELIST_SUFFIXES`，自动去除空白与 `@` 前缀，格式化为 `List<String>`；同时读取 `SystemSettingKeys.SECURITY_RATE_LIMIT_WINDOW` 和 `SECURITY_RATE_LIMIT_MAX_REQUESTS`；
     - `updatePolicy()`: 接收前端传入的 `emailWhitelistSuffixes` 列表，使用 `Collectors.joining(",")` 重新序列化后写回 `system_settings` 表，并热重载 Redis 缓存；同时保存限流窗口与最大请求数。
3. **自动化测试套件完备性保障**：
   - 在 [`AdminRiskServiceTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/test/java/com/humolix/volur/verification/AdminRiskServiceTest.java) 中新增 `testGetPolicyWithEmailAndInterception` 与 `testUpdatePolicyWithEmailAndInterception` 两个完整单测，验证白名单序列化/反序列化及限流设置的持久化。

### 2.2 前端工程架构与视图交互重构 (Vue 3 & TypeScript & Naive UI)
1. **类型定义层更新**：
   - 更新 [`src/volur-lite-view/src/types/risk/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/types/risk/index.ts)，强类型声明 `AdminRiskPolicyVO` 和 `AdminRiskPolicyDTO` 中的 `emailWhitelistSuffixes: string[]`、`rateLimitWindow: number`、`rateLimitMaxRequests: number`。
2. **系统管理视图精简**：
   - 从 [`GeneralSettingsTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/system/tabs/GeneralSettingsTab.vue) 中移除邮箱白名单项；
   - 从 [`MaintenanceSettingsTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/system/tabs/MaintenanceSettingsTab.vue) 中移除 `rateLimitWindow` 与 `rateLimitMaxRequests`；
   - 在 [`AdminSystemView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/system/AdminSystemView.vue) 中移除对应属性的响应式状态、数据提取、保存 payload 以及重置逻辑，保持系统设置仅关注平台基础参数与维护模式。
3. **“策略设置”视图重构为三栏式 Tabs 与动态列表** ([`AdminRiskRulesView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/risk/AdminRiskRulesView.vue))：
   - **选项卡体系**：
     - `<n-tab-pane name="email" :tab="t('admin.risk.policy.tabs.email')">`（邮件策略）；
     - `<n-tab-pane name="interception" :tab="t('admin.risk.policy.tabs.interception')">`（拦截策略）；
     - `<n-tab-pane name="ban" :tab="t('admin.risk.policy.tabs.ban')">`（封禁策略）。
   - **邮件策略白名单动态列表交互**：
     - **非下拉框设计**：提供 `@` 前缀单行输入框，支持直接输入域名（如 `edu.cn`），支持回车键或点击“添加后缀”按钮；
     - **输入自动清洗与排重**：自动清洗前后空格、剔除可能重复输入的 `@` 字符、转换为小写，并通过正则校验合法域名格式，若已存在则给出预警提示；
     - **状态与条目展示**：
       - 当列表为空时，展示友好的 `<n-empty>` 空状态，提示“暂未配置白名单后缀，当前全平台开放注册”；
       - 当列表有数据时，渲染精美条目卡片列表：包含 `@` 徽章、等宽字体展示域名、规则匹配说明（支持子域名匹配）以及单项“移除”按钮；
       - 列表上方提供实时统计标签与一键“清空列表”气泡确认框。
   - **拦截与封禁策略参数配置**：
     - 细化各参数说明标签，配置符合数值范围约束的 `<n-input-number>`；
     - 底部提供统一的“重置为默认值”与“保存策略配置”操作按钮，严格遵守 Volur 亮色模式纯黑 `#000000` 主按钮及深色主题规范。
4. **中英文国际化语言包更新**：
   - 在 [`zh_CN/admin/risk.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/zh_CN/admin/risk.json) 与 [`en_US/admin/risk.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/en_US/admin/risk.json) 中补齐 `policy.tabs.*`、`policy.email.*` 及 `policy.interception.*` 文案；
   - 在 [`zh_CN/admin/system.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/zh_CN/admin/system.json) 与 [`en_US/admin/system.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/en_US/admin/system.json) 中调整 Tab 3 名称为“平台运维模式”。

---

## 3. 验证结果与质量审计

1. **前端编译与类型检查验证**：
   - 运行 `npm run build`（`vue-tsc --noEmit && vite build`）：
   - 输出：`✓ built in 5.71s`，全工程通过 TypeScript 严格类型检查，无任何未捕获错误或警告。
2. **后端单元测试回归验证**：
   - 运行 `mvn test -pl volur-verification`：
   - 包含 `AdminRiskServiceTest`、`CaptchaRiskFlowTest`、`SlidePuzzleEngineTest`、`EmailServiceVerificationTest`、`MailWebhookServiceTest` 在内的所有 29 个单元测试 100% 通过（Failures: 0, Errors: 0）。
3. **交互规范审计**：
   - 注册邮箱白名单交互完全采用独立的可动态增删列表呈现，彻底杜绝下拉框设计；
   - 严格遵循 Volur 设计规范：亮色模式主要按钮背景 `#000000`、文字 `#ffffff`，描述文本颜色 `#444444`。
