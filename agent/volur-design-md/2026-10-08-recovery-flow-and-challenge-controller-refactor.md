# 找回用户名邮箱链路重构、EmailOTP API 地址调整与 Signin 挑战组件拆分记录

## 1. 变更背景与需求概述
根据用户交互与安全架构升级指令，对 Volur Lite 平台的后端安全挑战控制器（`volur-security`）及新一代前端工程（`src/volur-lite-view-NEXT`）进行以下四项核心重构：
1. **找回用户名邮箱流程流转升级**：从 `/v1/recovery/selection` 点击“电子邮箱”选项后，跳转至专用的 `/v1/recovery/username/email` 页面；
2. **安全核验平滑插入**：在 `/v1/recovery/username/email` 中由用户输入账号绑定的电子邮箱，点击“下一步”后引导进入 `/v1/recovery/challenge/recaptcha` 人机验证挑战（页面风格对齐 `signin/challenge/recaptchaChallengeView.vue`）；
3. **验证码校验流转**：通过人机验证后，平滑流转进入 `/v1/recovery/username/emailotp` 页面核验邮箱验证码；
4. **安全 API 地址标准化与挑战组件拆分**：
   - 后端 `EmailOTPChallengeController` 的 RequestMapping 路径由 `/api/v1/captcha/challenge/etotp` 重命名为规范的 `/api/v1/captcha/challenge/emailotp`；
   - 前端对应的接口与类型全面由 `etotp` 调整为 `emailotp`；
   - 登录核验组件 `deviceChallengeView.vue` 更名为 `passkeyChallengeView.vue`（组件名 `PassKeyChallengeView`）；
   - 将原揉合了短信与邮箱两用逻辑的 `totpChallengeView.vue` 彻底拆分为职责独立的 `emailotpChallengeView.vue` 与 `smsotpChallengeView.vue`。

---

## 2. 核心改动点与架构设计

### 2.1 后端控制器 API 地址标准化
- **控制器调整**：[`EmailOTPChallengeController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/controller/v1/challenge/EmailOTPChallengeController.java) 类级别注解由 `@RequestMapping("/api/v1/captcha/challenge/etotp")` 变更为 `@RequestMapping("/api/v1/captcha/challenge/emailotp")`；
- **测试覆盖**：运行 `volur-security` 单测，93 个用例全部绿色通过。

### 2.2 前端 API 与类型对齐
- **请求方法重命名**：[`src/api/auth.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/api/auth.ts) 中的 `submitEmailOTPChallengeApi` 与 `resendEmailOTPChallengeApi` 请求路径分别更新为 `/captcha/challenge/emailotp` 与 `/captcha/challenge/emailotp/send`；
- **动态挑战路由**：`submitDynamicChallengeApi` 动态拼接 `/captcha/challenge/${step}`，当 step 为 `emailotp` 时自动命中后端对应地址。

### 2.3 登录挑战组件拆分与演进
1. **PassKey 组件重命名**：
   - 将原 `deviceChallengeView.vue` 重命名为 [`passkeyChallengeView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/challenge/passkeyChallengeView.vue)；
   - 组件名称声明为 `PassKeyChallengeView`；
   - 物理清理旧文件。
2. **TOTP 组件拆分为 EmailOTP 与 SMSOTP**：
   - 新增 [`emailotpChallengeView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/challenge/emailotpChallengeView.vue)：专门用于处理邮箱动态核验码输入与提交流程，调用 `signinFlowStore.submitChallenge('emailotp', ...)`；
   - 新增 [`smsotpChallengeView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/challenge/smsotpChallengeView.vue)：专门用于处理短信动态验证码，调用 `signinFlowStore.submitChallenge('smsotp', ...)`；
   - 物理删除旧复合组件 `totpChallengeView.vue`；
   - 更新 [`index.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/challenge/index.ts) 与外壳 [`ChallengeView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/ChallengeView.vue) 的映射关系。

### 2.4 找回用户名完整业务链路搭建
1. **方式选择页更新**：[`SelectionView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/recovery/SelectionView.vue) 点击“电子邮箱”选项时推进至 `/v1/recovery/username/email`；
2. **邮箱输入视图**：创建 [`EmailView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/recovery/username/EmailView.vue)，用户输入绑定邮箱并进行格式校验，点击下一步流转至 `/v1/recovery/challenge/recaptcha`；
3. **人机验证挑战视图**：创建 [`recaptchaChallengeView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/recovery/challenge/recaptchaChallengeView.vue)，集成 `Recaptcha` 挑战组件，核验通过后流转至 `/v1/recovery/username/emailotp`；
4. **邮箱核验视图**：创建 [`EmailOtpView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/recovery/username/EmailOtpView.vue)，用户输入 6 位验证码，提供重新发送倒计时与修改邮箱链接；
5. **路由表维护**：在 [`src/router/routes/auth.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/router/routes/auth.ts) 中注册 `username/email`、`username/emailotp` 及 `challenge/:type(.*)*`。

---

## 3. 安全与规范审查 (Compliance Check)

- **纯粹代码与零注释铁律**：所有修改及新增的 Vue/TS/Java 代码绝不包含任何解释性注释或行内备注；
- **重构彻底性铁律**：彻底清理历史遗留文件与别名导出，杜绝废弃桥接兼容；
- **全链路快速失败原则**：对未满足的输入直接进行客户端与参数强校验拦截；
- **全链路构建验证**：
  - 后端执行 `mvn clean test -pl volur-security -am`，93 项测试全部通过（0 失败）；
  - 前端执行 `vue-tsc --noEmit && vite build`，编译打包全部通过（0 错误）。
