# 认证流转响应实体层级重构与 SignIn/SignUp/Recovery VO 模块化迁移记录

## 1. 变更背景与需求溯源

在 Volur Lite 平台此前的认证流转重构中，各个流程控制器的响应模型（Flow Response VOs）存在职责与归属倒挂、层级缺失的问题：
1. **模块职责与依赖倒挂**：
   - 登录流程控制器 [`SigninController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/controller/v1/SigninController.java) 位于 `volur-auth`，但其响应模型 `SignInFlowResponseVO` 却存放在底层 `volur-security` 模块中；
   - 注册流程控制器 [`SignupController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/controller/v1/SignupController.java) 位于 `volur-auth`，其响应模型 `SignUpFlowResponseVO` 独立平铺在 `volur-auth`，未能与登录流程复用相同的流转状态机契约；
   - 找回密码/用户名控制器 [`RecoveryController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/controller/v1/RecoveryController.java) 位于 `volur-auth`，此前缺乏专属的领域响应模型，临时借用了 `SignInFlowResponseVO`，违反单一职责原则（SRP）。
2. **Maven 单向依赖与抽象下沉**：
   - `volur-auth` 依赖于 `volur-security`。根据 Maven 反应堆单向依赖原则，`volur-security` 不能反向依赖 `volur-auth`；
   - 因此，将所有认证流转的通用基础字段（`dsh`, `tl`, `cid`, `nextStep`, `status`, `identifier`, `errorMessage`）抽象为一个通用基底模型 `FlowResponseVO` 并放置于底层 `volur-security`（`com.humolix.volur.security.domain.vo`），由 `volur-security` 的安全核验挑战控制器与底层核销服务统一使用；
   - 各上层业务领域的专属 VO（`SignInFlowResponseVO`、`SignUpFlowResponseVO`、`RecoveryFlowResponseVO`）统一收敛并继承于 `volur-auth`（`com.humolix.volur.auth.domain.vo`）。

---

## 2. 核心架构设计与改动清单

### 2.1 后端工程架构调整

#### 1. 基础基底 VO 下沉 (`volur-security`)
- **新增抽象基类**：[`FlowResponseVO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/domain/vo/FlowResponseVO.java)
  - 声明在 `com.humolix.volur.security.domain.vo`；
  - 标注 `@Data`, `@SuperBuilder`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@JsonInclude(JsonInclude.Include.NON_NULL)`；
  - 承载会话与流转核心状态：`dsh`, `tl`, `cid`, `nextStep`, `status`, `identifier`, `errorMessage`。
- **物理删除旧类**：移除 `volur-security` 中的 `SignInFlowResponseVO.java`。
- **服务与控制器对齐**：
  - [`ChallengeService.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/service/ChallengeService.java) & [`ChallengeServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/service/impl/ChallengeServiceImpl.java)：各挑战核验方法返回值统一调整为 `FlowResponseVO`；
  - [`PasskeyService.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/service/PasskeyService.java) & [`PasskeyServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/service/impl/PasskeyServiceImpl.java)：`verifyPasskeyAssertion` 返回值统一调整为 `FlowResponseVO`；
  - 挑战控制器统一更新返回值类型为 `ApiResponseEntity<FlowResponseVO>`：
    - [`PasswordController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/controller/v1/challenge/PasswordController.java) (`/api/v1/captcha/challenge/pwd`)
    - [`EmailOTPChallengeController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/controller/v1/challenge/EmailOTPChallengeController.java) (`/api/v1/captcha/challenge/emailotp`)
    - [`SMSOTPChallengeController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/controller/v1/challenge/SMSOTPChallengeController.java) (`/api/v1/captcha/challenge/smsotp`)
    - [`ReCAPTCHAChallengeController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/controller/v1/challenge/ReCAPTCHAChallengeController.java) (`/api/v1/captcha/challenge/recaptcha`)
    - [`PasskeyController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/controller/v1/challenge/PasskeyController.java) (`/api/v1/captcha/challenge/pk`)
    - [`ChallengeController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/controller/v1/challenge/ChallengeController.java) (`/api/v1/captcha/challenge/select`)
  - 对应单元测试 [`ChallengeServiceImplTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/test/java/com/humolix/volur/security/service/impl/ChallengeServiceImplTest.java) 与 [`PasskeyServiceImplTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/test/java/com/humolix/volur/security/service/PasskeyServiceImplTest.java) 对应断言类型同步适配。

#### 2. 领域流转模型收敛与继承 (`volur-auth`)
- **迁移并继承 `SignInFlowResponseVO`**：
  - 新建 [`SignInFlowResponseVO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/vo/SignInFlowResponseVO.java)；
  - 继承自 `FlowResponseVO`，使用 `@SuperBuilder`、`@EqualsAndHashCode(callSuper = true)`、`@ToString(callSuper = true)`；
  - 扩展专属字段 `captchaType`。
- **重构并继承 `SignUpFlowResponseVO`**：
  - 更新 [`SignUpFlowResponseVO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/vo/SignUpFlowResponseVO.java)；
  - 继承自 `FlowResponseVO`，消除重复声明的 `dsh`, `tl`, `cid`, `nextStep`, `status`, `errorMessage` 字段，保留 `username`, `email`。
- **新建专属 `RecoveryFlowResponseVO`**：
  - 新建 [`RecoveryFlowResponseVO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/vo/RecoveryFlowResponseVO.java)；
  - 继承自 `FlowResponseVO`，作为找回账号流转状态契约。
- **业务控制器与服务层全面对齐**：
  - [`SigninController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/controller/v1/SigninController.java)、[`SigninService.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/SigninService.java)、[`SigninServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/SigninServiceImpl.java)：引用的 `SignInFlowResponseVO` 100% 切换为 `com.humolix.volur.auth.domain.vo.SignInFlowResponseVO`；
  - [`RecoveryController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/controller/v1/RecoveryController.java)、[`RecoveryService.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/RecoveryService.java)、[`RecoveryServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/RecoveryServiceImpl.java)：全面将原借用的 `SignInFlowResponseVO` 替换为 `RecoveryFlowResponseVO`；
  - [`SignupController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/controller/v1/SignupController.java)：清理冗余无用导包（`ApiResponse`, `ResponseEntity`）。

---

### 2.2 前端工程对齐 (`volur-lite-view-NEXT`)

1. **类型层契约继承与扩展**：
   - [`src/types/auth/flow.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/types/auth/flow.ts)：
     - 新增 `FlowResponseVO` 基础接口定义；
     - `SignInFlowResponseVO extends FlowResponseVO`；
     - `SignUpFlowResponseVO extends FlowResponseVO`；
     - 新增 `RecoveryFlowResponseVO extends FlowResponseVO`。
   - [`src/types/auth/recovery.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/types/auth/recovery.ts)：
     - 显式向外导出 `RecoveryFlowResponseVO`。
2. **API 模块类型绑定**：
   - [`src/api/auth.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/api/auth.ts)：
     - `submitRecoveryEmailApi` 返回值由 `ApiResponse<SignInFlowResponseVO>` 调整为 `ApiResponse<RecoveryFlowResponseVO>`；
     - `submitRecoveryRecaptchaApi` 返回值由 `ApiResponse<SignInFlowResponseVO>` 调整为 `ApiResponse<RecoveryFlowResponseVO>`；
     - 确保所有导入均具备显式消费，无任何未使用局部变量。

---

## 3. 测试与规范审查

1. **零注释铁律与规范审查**：
   - 所有修改及新增的 Java/TS 代码严格遵循零冗余行内解释性注释的约束；
   - DTO / VO 领域模型命名统一，严格遵循 Lombok 规范与 `@SuperBuilder` 继承规范。
2. **测试验证结果**：
   - `volur-security` 单元测试：93/93 全部通过；
   - `volur-auth` 单元测试：58/58 全部通过（新增针对 `RecoveryFlowResponseVO` 的控制器与服务集成单测覆盖）；
   - 全平台全量 Maven 构建测试：13 个模块全部通过（`BUILD SUCCESS`，Total time: 14.856 s）；
   - 前端工程类型检查与构建：`npm run build`（`vue-tsc --noEmit && vite build`）通过，0 errors。
