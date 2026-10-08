# 用户名找回选项流转架构与前后端国际化重构

## 1. 变更背景与需求目标

在 Volur Next-gen 前端（`src/volur-lite-view-NEXT`）与后端认证子系统（`volur-auth` / `volur-system`）中，登录与账号找回流程紧跟 Google Workspace / Material Design 现代安全认证规范：
1. **文案语义纠正**：`IdentifierView.vue` 中原“忘记了邮箱？”文案存在语病（登录界面输入项为用户名/邮箱，找回意图系找回账户的用户名标识），现统一重构为“忘记了用户名？”（`auth.forgot_username`），并在中英繁三套语言包中完成彻底替换，坚决消除 `forgot_email` 历史别名残留；
2. **用户名找回选项探测架构（Username Recovery Options Flow）**：
   - 用户在登录第一步点击“忘记了用户名？”后，触发流转至找回引导页 `/v1/recovery/identifier`；
   - 找回页面第一步需要“给出列表选项”，供用户选择通过何种渠道接收用户名；
   - **后端前置校验与严格判定（Fail-Fast & Zero-Mock）**：后端必须先判断系统动态配置中邮箱（Email）与手机号（SMS）服务是否开启，若已配置且开启则开放对应方式，否则严禁伪造，向前端精准返回可用找回选项集合（`UsernameRecoveryOptionsVO`）；
   - 前端根据后端判定响应，动态渲染可选方式列表（电子邮箱、手机短信）或展示服务未就绪的警示卡片，并全程配备 Google 风格的 Scrim 遮罩层与 1000ms 平滑转场延迟。

---

## 2. 后端架构实现细节

### 2.1 动态系统配置项规范扩充 (`volur-system`)
- **常量定义 (`SystemSettingKeys.java`)**：
  - `SystemSettingKeys.MAIL_ENABLED = "system.mail.enabled"`（纳管邮件服务总开关）；
  - `SystemSettingKeys.SMS_ENABLED = "system.sms.enabled"`（纳管短信服务总开关）。
- **自动化初始化与公共设置映射 (`SystemSettingInitializer.java`)**：
  - 启动阶段自动为 `system.mail.enabled` 和 `system.sms.enabled` 注册默认值（`false`）；
  - 将 `MAIL` 和 `SMS` 纳入分组解析器与 `isPublicSetting` 白名单，保证单一真实数据源。
- **数据库增量平滑迁移 (`scripts/upgrade/upgrade_to_latest.sql`)**：
  - 严格遵循幂等设计，在增量升级存储过程添加针对 `system.mail.enabled` 和 `system.sms.enabled` 的 `IF NOT EXISTS` 探测插入语句。

### 2.2 找回领域模型 (`volur-auth/domain/vo`)
- **`RecoveryMethodVO`**：
  - 声明字段：`method`（`EMAIL` / `PHONE`）、`name`、`description`、`icon`；
  - 遵循 VO 标准注解：`@Data`、`@Builder`、`@NoArgsConstructor`、`@AllArgsConstructor`、`@JsonInclude(NON_NULL)`。
- **`UsernameRecoveryOptionsVO`**：
  - 极简化与紧凑化设计：移除冗余布尔标志 `emailEnabled` 与 `phoneEnabled`，统一由单一真实源 `methods` 承载所有生效中的找回方式；
  - 默认值使用 `@Builder.Default`。

### 2.3 服务层与控制器 (`volur-auth`)
- **`RecoveryService` & `RecoveryServiceImpl`**：
  - 基于 Lombok `@RequiredArgsConstructor` 进行不可变构造注入；
  - 依据 `systemSettingService.getBoolean(SystemSettingKeys.MAIL_ENABLED, false)` 并校验发件人地址非空（`StringUtils.hasText`）确定 `emailEnabled`；
  - 依据 `systemSettingService.getBoolean(SystemSettingKeys.SMS_ENABLED, false)` 确定 `phoneEnabled`；
  - 动态组装 `methods` 列表并返回，杜绝硬编码假数据。
- **`RecoveryController` (`/api/v1/auth/recovery`)**：
  - 端点：`GET /api/v1/auth/recovery/username/options`；
  - 遵循 Thin Controller 准则与小写驼峰工厂方法 `ApiResponseEntity.ok("获取找回用户名选项成功", response)`；
  - 添加系统操作审计切面注解 `@Syslog(module = "AUTH", value = "获取找回用户名方式选项", logType = "AUTH", recordParams = false)`。
- **白名单配置放行 (`SecurityWhitelistProperties.java`)**：
  - 在 `WHITE_LIST_PATHS` 中注册 `"/api/v1/auth/recovery/**"`，允许免密匿名访问选项探测接口。

---

## 3. 前端架构实现细节 (`volur-lite-view-NEXT`)

### 3.1 语言包彻底重构
- 在 `zh-CN`、`en-US`、`zh-TW` 语言包中，彻底废除 `forgot_email`，规范重命名为 `forgot_username`；
- 补充完整的找回流程词条：`recovery_username_title`、`recovery_username_subtitle`、`recovery_method_email`、`recovery_method_phone`、`recovery_disabled_tip`、`back_to_signin` 等。

### 3.2 登录视图对齐与 Scrim 延迟 (`IdentifierView.vue`)
- 链接文案更新为 `{{ t('auth.forgot_username') }}`；
- 链接元素添加 `:isScrim="true"`；
- `handleForgotAccount` 重构为 `async` 方法，在调用 `router.push('/v1/recovery/identifier')` 前执行 `await signinFlowStore.triggerScrim(undefined, 1000)`，实现无缝平滑转场。

### 3.3 找回用户名视图 (`UsernameRecoveryView.vue`)
- **路径与路由**：注册 `/v1/recovery/identifier` 路由（挂载至 `AuthLayout`）；
- **动态卡片头部**：通过 `useCardHeader` 响应式同步左侧品牌区域的标题与副标题；
- **选项列表渲染与状态流转**：
  - 骨架屏加载状态：当处于网络拉取状态时（`isFetching=true`），展示 Google 风格的脉冲骨架占位；
  - 纯由 `methods` 状态驱动：前端仅判断 `methods.length > 0` 进行渲染，包含 SVG 邮箱/手机图标、标题与说明、前进指示箭头；
  - 列表项配备 `:isScrim="true"` 与键盘无障碍 Enter/Space 快捷激活；
- **快速失败警示状态**：
  - 当 `methods` 为空时，展示标准安全提示卡片（`recovery_disabled_tip`），引导联系平台管理员；
- **完整全链路国际化 (Full i18n)**：
  - 视图中所有静态文案、动态选项标题/描述、报错信息与操作按钮均由 `vue-i18n` 的 `t(...)` 驱动，在中、英、繁三套语言包中 100% 完整覆盖，杜绝硬编码字符；
- **底部操作**：
  - 提供 `auth-action-btn btn-secondary-action`“返回登录”胶囊按钮，高度 36px、圆角 9999px，点击触发带 Scrim 遮罩平滑退回登录页。

### 3.4 严格类型与 API 封装
- 创建 `src/types/auth/recovery.ts`，导出强类型 `RecoveryMethodVO` 与 `UsernameRecoveryOptionsVO`，严格杜绝 `any`；
- 在 `src/api/auth.ts` 中封装 `getUsernameRecoveryOptionsApi`。

---

## 4. 回归与质量验证

1. **后端验证**：
   - 单元测试 `RecoveryServiceImplTest`（全覆盖开启、部分开启与全关闭场景）、`RecoveryControllerTest`、`SecurityWhitelistMatcherTest` 均 100% 通过；
   - 运行 `mvn test -pl volur-system,volur-auth`，54 项测试全部通过（0 failures, 0 errors, 0 skipped）。
2. **前端验证**：
   - 运行 `vue-tsc --noEmit && vite build`，TypeScript 类型检查通过，生产 Bundle 打包成功。
3. **规范符合性**：
   - 所有生产代码文件 100% 保持极致纯净，无任何行内解释性注释；
   - 无任何历史别名保留；无任何隐式 Mock 假数据。
