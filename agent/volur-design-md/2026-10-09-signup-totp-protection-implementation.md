# 注册流程动态身份验证器（TOTP）端到端落地实现方案与审计报告

## 1. 变更背景与问题审计

在用户注册流程的保护配置步骤 `/v1/signup/steps/pt/totp`（组件 [`totpProtectView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signup/protect/totpProtectView.vue)）中，此前仅为一个纯展示型占位静态视图：
- 页面仅展示提示性静态文案，操作按钮仅有“我知道了”与“返回”，点击后均直接回退至安全选项页；
- 前端缺乏二维码渲染、Base32 密钥复制及 6 位动态验证码校验绑定的完整闭环；
- 后端数据库 [`User.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/domain/entity/User.java) 及 `user` 表缺乏存储 TOTP 密钥及启用状态的字段；
- 后端 [`SignupController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/controller/v1/SignupController.java) 缺乏获取 TOTP 配置及核验绑定的端点；
- 后端 [`ChallengeServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/service/impl/ChallengeServiceImpl.java) 历史逻辑中存在将 `totp` 错误归流为 `smsotp`（短信验证码）并强依赖手机号的遗留缺陷。

---

## 2. 改造方案设计与实施

### 2.1 数据模型与平滑迁移
1. **领域实体扩展**：
   - 在 [`User.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/domain/entity/User.java) 中新增 `totpSecret` (VARCHAR(64)) 与 `isTotpEnabled` (TINYINT(1) DEFAULT 0) 成员属性。
2. **增量数据库升级脚本**：
   - 在 [`upgrade_to_latest.sql`](file:///H:/HuMoLix/github/Volur-Lite/scripts/upgrade/upgrade_to_latest.sql) 的存储过程 `migrate_volur_schema_20260921` 中，使用 `information_schema.COLUMNS` 进行幂等存在性探测，安全添加 `totp_secret` 与 `is_totp_enabled` 列。

### 2.2 核心安全算法类与挑战流解耦
1. **RFC 6238 标准实现**：
   - 新增 [`TotpUtil.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/util/TotpUtil.java)，支持：
     - RFC 4648 标准 Base32 安全随机密钥生成与编解码；
     - HMAC-SHA1 动态截断与 6 位动态验证码生成；
     - ±1 时间步长（±30 秒容差）时钟偏差校验；
     - 标准 `otpauth://totp/{issuer}:{account}?secret={secret}&issuer={issuer}&algorithm=SHA1&digits=6&period=30` URI 构建。
2. **安全挑战归流解耦**：
   - 修正 [`ChallengeServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/service/impl/ChallengeServiceImpl.java)，将真正的 `totp` 与短信验证码 `smsotp` 解耦，判定用户 TOTP 凭证状态，杜绝错误的手机号强绑定校验。

### 2.3 注册服务层与接口实现
1. **模型定义**：
   - DTO：[`SignUpTotpVerifyRequestDTO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/dto/signup/SignUpTotpVerifyRequestDTO.java)
   - VO：[`SignUpTotpSetupVO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/vo/SignUpTotpSetupVO.java)、[`SignUpTotpVerifyResponseVO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/vo/SignUpTotpVerifyResponseVO.java)
2. **业务逻辑**：
   - [`SignupServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/SignupServiceImpl.java) 实现：
     - `setupTotp(dsh, tl)`：校验流转锁，生成 Base32 密钥并写入 Redis 临时缓存（`vredis:auth:signup:totp:temp:{dsh}`，TTL 10 分钟），返回配置信息；
     - `verifyAndEnableTotp(request)`：校验流转锁，读取 Redis 临时密钥并核验 6 位验证码；校验成功后持久化更新用户 `totpSecret` 与 `isTotpEnabled = true`，清除临时缓存。
3. **控制器暴露**：
   - [`SignupController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/controller/v1/SignupController.java) 暴露 `GET /api/v1/auth/signup/protect/totp/setup` 与 `POST /api/v1/auth/signup/protect/totp/verify`。

### 2.4 前端体验重构
1. **组件重构**：
   - 彻底重构 [`totpProtectView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signup/protect/totpProtectView.vue)：
     - 挂载 Naive UI 原生 `<n-qr-code>`，渲染后端签发的 `otpAuthUrl`；
     - 提供一键切换“手动输入密钥”折叠区与剪贴板复制能力；
     - 限制 6 位数字验证码输入，支持回车触发表单提交；
     - 验证成功后无缝切换至高质感绿标已绑定态，引导用户进入主站或完成流程。
2. **国际化支持**：
   - 完整补齐 `zh-CN`、`en-US`、`zh-TW` 语言包中扫码提示、复制反馈、错误校验及绑定成功说明。

### 2.5 步骤法与 GoogleTextField 深度交互重构
1. **分步流转设计（Step-by-step UX Flow）**：
   - **子步骤 1（显示二维码）**：
     - 文案更新为：“请使用动态身份验证器应用扫描下方二维码：“；
     - 切换手动密钥按钮重构为类似 `<a>` 标签样式的原生纯文字按钮，消除背景；展开的手动密钥区域同样采用通透无外底设计；
     - 底部主按钮更新为“下一步”（`t('common.next')`），点击后过渡切换至输入子步骤。
   - **子步骤 2（输入动态验证码）**：
     - 引入统一的 [`GoogleTextField.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/google/GoogleTextField.vue)，支持 6 位动态验证码浮动标签、清空、错误展示与回车事件触发；
     - 底部主按钮为“验证”（`t('common.verify')`），次按钮为“返回”（`t('common.back')`）可回退到二维码步骤；
     - 卡片顶部 Header 标题与副标题随子步骤自动联动切换。
   - **成功完成态**：
     - 验证通过后展示对勾成功徽章与说明，主按钮变为“下一步”，引导用户完成注册。

### 2.6 二维码纯净展示与后端 Session 元数据类型安全修复
1. **后端 ClassCastException 缺陷修复**：
   - 缺陷根因：Session metadata 在经过 Redis 序列化/反序列化后，Snowflake ID 可能被还原为 `String`，直接 `(Long) sessionManager.getMetadata(...)` 会抛出 `ClassCastException: class java.lang.String cannot be cast to class java.lang.Long`。
   - 修复方案：在 [`SignupServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/SignupServiceImpl.java) 中增加强类型安全适配（支持 `Number.longValue()` 与 `String` 解析，并 fallback 至 `username` 反查），新增单元测试保障。
2. **前端视觉与切换细节精细化**：
   - 彻底移除二维码外层的 `box-shadow`（`shadow-sm`）与背景色（`bg-white`），保持透明和居中；
   - 切换按钮支持动态文案：“无法扫描二维码？点击手动输入密钥” 与 “使用二维码进行扫描”；
   - 展开的手动密钥卡片增加精致外边框，并移除“（Base32）”冗余字样，直接展示“密钥”。

### 2.7 验证按钮 isScrim 延迟与加载遮罩层接入
1. **缺陷根因定位**：
   - 此前在 [`totpProtectView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signup/protect/totpProtectView.vue) 的输入 6 位验证码阶段，`handleVerify` 仅设置了本地组件的 `isSubmitting = true` 并直接裸调了 `submitSignUpTotpVerifyApi`；
   - 未调用 `signupFlowStore` 的 `withLoading` / `triggerScrim` 机制，导致 `signupFlowStore.loading` 始终为 `false`；
   - [`AuthLayout.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/layouts/auth/AuthLayout.vue) 无法检测到全局加载状态，未能渲染 `auth-scrim` 遮罩层、未激活动态 Google 线性进度条 `auth-card__progress`、未生成点击拦截器 `auth-card__blocker`，且缺少了 1000ms（`MIN_LOADING_DURATION_MS`）的标准平滑过渡延迟。
2. **重构实施**：
   - **Pinia Store 层**：在 [`signupFlow.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/store/modules/flow/signupFlow.ts) 中收敛 `submitSignUpTotpVerify(code: string)` 方法，使用 `checkDshExpiration()` 校验会话有效性，并严格通过 `withLoading`（默认 1000ms 最小加载周期）进行异步请求管控；
   - **重置流状态安全保护**：为 `signupFlow.ts` 中的 `resetFlow(keepLoading = false)` 补充 `keepLoading` 标识，避免外部处于 Scrim 延迟动画阶段时提前注销 `loading` 状态导致闪退；
   - **视图层组件**：
     - 在 [`totpProtectView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signup/protect/totpProtectView.vue) 的 `handleVerify` 中，统一改由 `signupFlowStore.submitSignUpTotpVerify(cleanCode)` 发起核验，全程由 Scrim 驱动 1000ms 延迟、阻断遮罩及卡片顶部进度条；
     - 为 `<AuthActionBar>` 显式绑定 `:is-scrim="true"`，并保持 `:secondary-disabled="loading || isSubmitting"` 全局禁用联动；
     - 为 `<GoogleTextField>` 增加 `@update:model-value="clearCodeError"`，实现用户重新键入时即时清理错误信息。

---

## 3. 测试与验证结果

1. **后端单元测试**：
   - [`TotpUtilTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/test/java/com/humolix/volur/security/util/TotpUtilTest.java) 3 项算法测试全部通过；
   - [`SignupServiceImplTotpTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/test/java/com/humolix/volur/auth/service/impl/SignupServiceImplTotpTest.java) 5 项服务层单元测试全部通过（包含 String 类型 ID 兼容性测试）；
   - `volur-auth` 模块全量测试绿灯通过。
2. **前端类型与编译测试**：
   - `vue-tsc --noEmit && vite build` 生产构建成功，类型检查 0 错误（2916 个模块转化完成）。
