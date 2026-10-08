# 统一安全核验人机挑战架构、消除 403 越级跳转与路由全小写标准化施工记录

## 1. 变更背景与问题剖析

### 1.1 现象与问题溯源
用户在调用 `GET /api/v1/captcha/challenge/recaptcha?dsh=...&tl=...` 获取人机验证挑战时收到 HTTP 403 错误：
```json
{
    "status": 403,
    "errorCode": 403,
    "success": false,
    "message": "检测到非法越级跳转，请按顺序完成验证",
    "data": null,
    "path": "/api/v1/captcha/challenge/recaptcha"
}
```

### 1.2 根本原因剖析
1. **状态机未合法跃迁**：
   - 会话通过 `initSession` 初始化后，`flow.getCurrentStep()` 为初始状态（如 `"identifier"`）；
   - 用户在 `SelectionView.vue` 选择了找回方式（如电子邮箱）后，原前端仅进行了客户端路由跳转，未向服务端通知推进状态机；
   - 挑战页面加载时，`<Recaptcha>` 组件请求 `GET /api/v1/captcha/challenge/recaptcha`，服务端 `RecaptchaServiceImpl.getRecaptchaChallenge` 调用 `transitionLockManager.validateTransLock(dsh, tl, "recaptcha")` 校验会话当前步骤；
   - 状态机当前步骤与期望步骤 `"recaptcha"` 不一致，防跳步安全机制触发 403 拒绝；
2. **架构割裂与冗余端点反模式**：
   - 系统在 `volur-security` 模块中已具备通用的人机验证控制器 [`ReCAPTCHAChallengeController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/controller/v1/challenge/ReCAPTCHAChallengeController.java) (`/api/v1/captcha/challenge/recaptcha`)；
   - 但 `volur-auth` 的 `RecoveryController` 却显式维护了一个专用的 `submitRecoveryRecaptcha` (`/api/v1/auth/recovery/username/recaptcha`)，破坏了全局安全挑战的单一职责与高内聚设计；
   - 通用控制器中的 `verifyReCAPTCHA` 原先硬编码要求存在 `identifier` 并固定流转至 `pwd`，导致找回流程无法复用。

---

## 2. 核心架构与重构实施

### 2.1 消除 403 越级跳转：方式选择与状态机显式初始化
1. **新增找回方式选择端点**：
   - 在 `volur-auth` 的 `RecoveryController` 新增 `POST /api/v1/auth/recovery/username/select`；
   - 入参 DTO：[`UsernameRecoverySelectRequestDTO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/dto/recovery/UsernameRecoverySelectRequestDTO.java)（包含 `dsh`、`tl`、`cid`、`method`）；
   - 在 `RecoveryServiceImpl.selectRecoveryMethod` 中：
     - 校验找回方式服务可用性（如 `EMAIL` 检查邮件配置是否完备）；
     - 恢复会话并由 `transitionLockManager.nextTransLock` 合法推进状态机至 `"recaptcha"` 步骤；
     - 签发并返回当前 `"recaptcha"` 步骤有效的新 `tl` 与 `cid`；
2. **前端选择联动**：
   - 在 [`SelectionView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/recovery/SelectionView.vue) 中，点击选项时先调用 `selectRecoveryMethodApi` 推进状态机，成功获得针对 `recaptcha` 步骤的新 `tl` 后，再携带该合法凭证跳转至挑战页；
   - 此时组件挂载请求题目时，会话步骤与请求凭证完全匹配，彻底消除 403 非法越级异常。

### 2.2 统一复用通用的 `ReCAPTCHAChallengeController`
1. **增强通用挑战服务层 (`ChallengeServiceImpl.java`)**：
   - 调整通用 `verifyReCAPTCHA` 逻辑：当检测到会话中未设置 `identifier`（找回流程上下文）时，核验人机票据通过后直接推进状态机至 `"email"` 步骤，并返回 `nextStep: "email"`；
   - 保持登录流程（具备 `identifier`）原有风控与流转逻辑不变；
2. **全面清理冗余端点**：
   - 从 `RecoveryController.java` 和 `RecoveryService.java` 中彻底删除 `submitRecoveryRecaptcha` 接口及实现；
   - 物理删除已废弃的 `UsernameRecoveryRecaptchaRequestDTO.java`；
   - 前端 [`recaptchaChallengeView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/recovery/challenge/recaptchaChallengeView.vue) 移除专有 API 引入，直接调用 `@/api/captcha` 提供的通用 `submitRecaptchaChallengeApi`。

### 2.3 路由全小写标准化
- 前端全局将 `/v1/recovery/challenge/reCAPTCHA` 统一变更为规范的全小写语义路径 `/v1/recovery/challenge/recaptcha`。

---

## 3. 规范与安全审查 (Compliance Check)

- **纯粹代码与零注释铁律**：所有修改和新增的 Java、TypeScript、Vue 代码中，杜绝任何解释性行内注释与冗余备注；
- **重构彻底性铁律**：彻底移除专有接口与废弃 DTO，不保留任何别名或兼容层；
- **工程构建与测试验证**：
  - 后端执行 `mvn test "-Dtest=Recovery*,Challenge*,Recaptcha*" "-Dsurefire.failIfNoSpecifiedTests=false"`，全量测试均绿灯通过；
  - 前端执行 `vue-tsc --noEmit && vite build`，零类型错误零构建警告通过。
