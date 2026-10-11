# 2026-10-10 Passkey 与 TOTP 凭据生命周期与解绑端点收敛施工记录

## 1. 变更背景与架构演进
在统一安全与认证体系（`volur-security`）中，通行密钥（Passkey / WebAuthn）与动态身份验证器（TOTP）已实现了预认证阶段（Pre-Auth）的注册绑定保护与登录动态挑战核验。
然而，在用户完成登录后（Post-Auth / In-Session），缺乏针对已绑定安全凭据的生命周期管理能力：
- 用户无法查询名下已绑定的 Passkey 硬件密钥列表（名称、创建时间、最后使用时间、传输类型等）；
- 用户无法安全解绑/注销已绑定的某个 Passkey；
- 用户无法查看当前账号的 TOTP 启用状态；
- 用户无法在个人中心解绑/关闭 TOTP。

根据高内聚与单一职责设计原则，将 Passkey 与 TOTP 的凭据列表查询及解绑端点分别收敛到各自的领域控制器：
1. `PasskeyController` (`/api/v1/captcha/challenge/pk`)
2. `TOTPChallengeController` (`/api/v1/captcha/challenge/totp`)

---

## 2. 具体改动点

### 2.1 后端领域模型（Domain VOs）
- 新增 [`PasskeyCredentialVO`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/domain/vo/PasskeyCredentialVO.java)：
  - `credentialId`：Base64URL 编码凭据标识符；
  - `label`：凭据别名/备注名称；
  - `created`：创建注册时间；
  - `lastUsed`：最近使用核验时间；
  - `signatureCount`：签名计数；
  - `authenticatorTransports`：认证器传输信道（如 internal, usb, nfc 等）。
- 新增 [`TotpStatusVO`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/domain/vo/TotpStatusVO.java)：
  - `enabled`：当前用户是否已开启 TOTP 动态验证；
  - `updatedAt`：最近更新时间。

### 2.2 服务层扩展（Service Layer）
- [`PasskeyService`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/service/PasskeyService.java) 与 [`PasskeyServiceImpl`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/service/impl/PasskeyServiceImpl.java)：
  - `listCredentials(Long userId)`：通过 `userCredentialRepository.findByUserId` 查询当前用户的凭据列表，并映射为 `PasskeyCredentialVO`；
  - `deleteCredential(Long userId, String credentialId)`：严格限定仅能解绑当前 `userId` 名下的指定通行密钥，杜绝越权（IDOR）风险；调用 `userCredentialRepository.delete` 进行安全持久化解绑。
- [`TotpService`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/service/TotpService.java) 与 [`TotpServiceImpl`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/service/impl/TotpServiceImpl.java)：
  - `getTotpStatus(Long userId)`：查询当前登录用户的 `isTotpEnabled` 状态并返回；
  - `disableTotp(Long userId)`：事务级解绑 TOTP，校验当前是否处于启用状态，清除密钥 `totpSecret` 并将 `isTotpEnabled` 重置为 `false`。

### 2.3 控制器层路由（Controller Layer）
- [`PasskeyController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/controller/v1/challenge/PasskeyController.java) (`/api/v1/captcha/challenge/pk`)：
  - `GET /credentials`：获取当前登录用户的通行密钥凭据列表；
  - `DELETE /credentials/{credentialId}`（兼顾兼容 `DELETE /{credentialId}`）：解绑指定的通行密钥凭据。
- [`TOTPChallengeController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/controller/v1/challenge/TOTPChallengeController.java) (`/api/v1/captcha/challenge/totp`)：
  - `GET /status`（方法名 `getStatus`）：获取当前登录用户的 TOTP 开启状态；
  - `POST /disable`（方法名 `disable`）：解绑/停用当前用户的动态身份验证器。

### 2.4 前端类型与 API 契约对齐（volur-lite-view-NEXT）
- [`types/auth/flow.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/types/auth/flow.ts)：
  - 声明 `PasskeyCredentialVO` 与 `TotpStatusVO` 强类型接口；
- [`api/auth.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/api/auth.ts)：
  - `fetchPasskeyCredentialsApi`
  - `deletePasskeyCredentialApi`
  - `fetchTotpStatusApi`
  - `disableTotpApi`

---

## 3. 安全防护与越权防范（IDOR Prevention）
1. **强制 `@CurrentUserId` 上下文绑定**：所有凭据列表与解绑操作不接受来自前端请求体伪造的 `userId`，严格依赖框架层的 JWT / 会话解析器注入真实用户 ID；
2. **所有权隔离校验**：在 `deleteCredential` 中，服务端根据当前用户的 `userBytes` 先行在底层查询其名下凭据子集，仅允许在属于当前用户的集合中匹配 `credentialId` 执行删除，彻底阻断越权删除任意其他用户凭据的攻击路径；
3. **零 Mock & 快速失败**：未登录直接抛出 `HttpStatus.UNAUTHORIZED`，未找到目标凭据抛出 `HttpStatus.NOT_FOUND`，未启用 TOTP 尝试解绑抛出 `HttpStatus.BAD_REQUEST`。

---

## 4. 验证与测试
- **单元测试**：
  - 新增 [`PasskeyControllerTest`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/test/java/com/humolix/volur/security/controller/v1/challenge/PasskeyControllerTest.java)；
  - 更新 [`PasskeyServiceImplTest`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/test/java/com/humolix/volur/security/service/PasskeyServiceImplTest.java)；
  - 更新 [`TOTPChallengeControllerTest`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/test/java/com/humolix/volur/security/controller/v1/challenge/TOTPChallengeControllerTest.java)；
  - 更新 [`TotpServiceImplTest`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/test/java/com/humolix/volur/security/service/impl/TotpServiceImplTest.java)；
  - `mvn test -pl :volur-security` 全部 146 个测试用例通过；
  - `mvn test -pl :volur-auth` 全部 62 个测试用例通过。
- **前端编译**：
  - `npm run build`（`vue-tsc --noEmit && vite build`）通过，0 错误。
