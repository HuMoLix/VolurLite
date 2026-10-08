# 修复 WebAuthn 通行密钥交叉越权登录高危漏洞与可选核验自适应拒绝流转方案

## 1. 缺陷背景与根因深度剖析

### 1.1 缺陷现象
1. **未绑定通行密钥却显示选项**：管理员或普通用户在密码登录核验阶段（`/v1/signin/challenge/pwd`），该用户未绑定过任何 Passkey 凭据，但在点击“尝试其他方式（Try another way）”进入选择列表（`/v1/signin/challenge/selection`）时，依然出现了“使用安全密钥或通行密钥”选项。
2. **交叉越权登录高危漏洞（Critical Bypass）**：使用未配置 Passkey 的账号（如 `VolurSuperAdmin`）进入通行密钥验证环节后，系统弹窗允许使用客户端中已绑定给其他低权限账号（如 `test123`）的 Passkey 进行签名断言；断言提交后，后端判定验证成功，并为 `VolurSuperAdmin` 签发了正式的管理员身份 JWT 凭证，造成任意 Passkey 交叉越权直接登录目标高权限账号的致命安全漏洞。

---

### 1.2 根因定位分析

#### 1. 列表渲染与挑战切换缺少凭据存在性过滤 (`ChallengeServiceImpl`)
- 在 `ChallengeServiceImpl.getChallengeSelection(dsh, currentType)` 中，仅对用户的邮箱（`user.getEmail()`）与手机（`user.getPhone()`）进行了绑定状态校验，对 `pk`（通行密钥）则是无条件直接调用 `allOptions.add(...)` 硬编码追加；
- 当 `currentType = "pwd"` 时，代码滤除了 `pwd`，导致未配置 Passkey 的用户列表中依然暴露了 `pk` 选项；
- 另外在切换挑战接口 `selectChallenge` 中，对 `pk` 步骤未做任何守卫校验，允许非法切换。

#### 2. 生成断言选项时允许凭据列表为空触发全局凭据选择 (`PasskeyServiceImpl.getAssertionOptions`)
- 当为未配置 Passkey 的用户生成断言请求选项时，Spring Security 底层查不到任何已注册凭据，生成的 `PublicKeyCredentialRequestOptions` 中 `allowCredentials` 为空列表；
- 根据 W3C WebAuthn 规范，当 `allowCredentials` 为空或未指定时，浏览器将触发 Discoverable Credentials（可发现凭据选择器）模式，允许使用当前域名下任意已存在的凭据（例如曾经为 `test123` 绑定的 Passkey）进行签名。

#### 3. 断言核验阶段完全缺失身份所属匹配校验 (`PasskeyServiceImpl.verifyPasskeyAssertion`)
- 在断言核验阶段，代码调用 `rpOperations.authenticate(authRequest)` 验证了签名合法性，底层根据凭据 ID 返回了对应的凭据所有者实体 `PublicKeyCredentialUserEntity`（即 `test123`）；
- 但此前业务代码直接忽略了返回值，未比对 `authenticatedUser` 的用户名与用户 ID 是否与当前会话正在登录的目标用户匹配，直接认定核验通过并推进会话，导致身份校验旁路越权。

---

## 2. 解决方案与改动设计

### 2.1 依赖注入与凭据探测能力补齐 (`PasskeyService` & `PasskeyServiceImpl`)
1. 在 `PasskeyService` 中声明 `boolean hasPasskey(Long userId)`，在 `PasskeyServiceImpl` 中注入 Spring Security 的 `UserCredentialRepository`；
2. 构造用户 8 字节二进制 ID `Bytes userBytes`，调用 `userCredentialRepository.findByUserId(userBytes)` 查询是否存在凭据记录，并联动 `system.webauthn.enabled` 系统配置判定；
3. **断言选项 Fail-Fast**：在 `getAssertionOptions` 中，若 `options.getAllowCredentials()` 为空，直接抛出 `BusinessException(HttpStatus.BAD_REQUEST, "当前账号未绑定通行密钥，无法使用此方式验证")`，坚决不向客户端下发空凭据描述符；
4. **断言所有权严格核验（Identity Verification Barrier）**：在 `verifyPasskeyAssertion` 中，接收 `rpOperations.authenticate(authRequest)` 返回的 `authenticatedUser`，双重核对用户名与用户 ID 是否与当前会话用户完全一致，若不一致直接记录风险失败并抛出 401 UNAUTHORIZED。

### 2.2 备选方式自适应收敛与无可用方式跳转拒绝 (`ChallengeServiceImpl`)
1. 在 `ChallengeServiceImpl` 中注入 `PasskeyService`；
2. 在 `getChallengeSelection` 中通过 `passkeyService.hasPasskey(user.getId())` 动态判定是否追加 `pk` 选项；
3. 在 `selectChallenge` 中增加对 `pk` / `device` 的凭据存在性前置拦截校验；
4. 当用户处于密码挑战页（`currentType = "pwd"`）点击“尝试其他方式”，且该用户未绑定邮箱、手机和 Passkey 时，过滤后返回空列表 `[]`；前端 `selectionView.vue` 检测到数据为空，立即触发 `handleSelectionRejected()`，顺畅跳转至 `/v1/signin/challenge/rejected`（`rejectView.vue`）。

---

## 3. 涉及改动文件清单

1. [`PasskeyService.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/service/PasskeyService.java)
   - 增加 `boolean hasPasskey(Long userId)` 接口定义。
2. [`PasskeyServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/service/impl/PasskeyServiceImpl.java)
   - 注入 `UserCredentialRepository`，实现 `hasPasskey`；
   - 在 `getAssertionOptions` 中校验 `allowCredentials` 必须非空（Fail-Fast）；
   - 在 `verifyPasskeyAssertion` 中校验 `authenticatedUser` 与当前登录账号双重一致性。
3. [`ChallengeServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/service/impl/ChallengeServiceImpl.java)
   - 注入 `PasskeyService`；
   - 在 `getChallengeSelection` 与 `selectChallenge` 中接入 `passkeyService.hasPasskey` 守卫，无其他方式时返回空列表。
4. [`PasskeyServiceImplTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/test/java/com/humolix/volur/security/service/PasskeyServiceImplTest.java)
   - 补齐 Mock 与测试用例，覆盖空凭据请求快速失败、凭据所有者不匹配拒绝以及 `hasPasskey` 判定。
5. [`ChallengeServiceImplTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/test/java/com/humolix/volur/security/service/impl/ChallengeServiceImplTest.java)
   - 补齐 Mock 与测试用例，验证包含 Passkey 时正确展示，以及无任何备选方式时返回空列表。

---

## 4. 验证与审计结论

- **后端单元测试**：`volur-security` 全部 102 个测试用例 100% 通过（`Tests run: 102, Failures: 0, Errors: 0, Skipped: 0`）；
- **全平台编译构建**：`volur-lite-platform` 13 个子模块 `mvn test-compile` 100% 成功；
- **前端工程编译构建**：`volur-lite-view-NEXT` 运行 `vue-tsc --noEmit && vite build` 0 错误编译通过；
- **安全性审计**：彻底消除了跨账号 Passkey 交叉伪造签名的严重安全隐患，可选核验方式展示与流程流转完全遵循 Fail-Fast 原则。
