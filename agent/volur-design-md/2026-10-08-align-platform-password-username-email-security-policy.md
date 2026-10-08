# 全平台凭据安全策略对齐施工记录 (2026-10-08)

## 1. 变更背景与对齐目标
在用户认证与账号安全体系中，注册流程（包括一步注册与分步式流转注册）、密码重置找回流程的凭据校验规则此前存在细微标准偏差（如密码长度下限与上限、特殊字符复杂度约束、邮箱校验器类型等）。为了确保全系统遵循一致的安全基准，本次施工对全平台的用户名、密码及邮箱校验策略进行彻底统一与对齐，与后端基准 DTO `RegisterRequestDTO` 及框架增强型校验器保持 100% 严格一致。

---

## 2. 统一策略规范明细

### 2.1 用户名安全策略 (Username Policy)
- **长度范围**：3 到 32 个字符之间（`[3, 32]`）。
- **字符集合**：仅支持英文字母、数字、下划线及连字符（短横线 `-`）。
- **正则模式**：`^[a-zA-Z0-9_-]+$`。
- **空白清洗**：`@Trimmed` 自动消除首尾空白。
- **校验提示信息**：
  - 必填提示："用户名不能为空"
  - 长度提示："用户名长度必须在3到32个字符之间"
  - 字符集提示："用户名只能包含字母、数字、下划线和连字符"

### 2.2 密码安全策略 (Password Policy)
- **长度范围**：6 到 32 个字符之间（`[6, 32]`）。
- **字符复杂度**：必须同时包含英文字母、数字，以及至少一个特殊符号。
- **特殊符号白名单**：`@$!%*?&._#^&+=`。
- **正则模式**：`^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*?&._#^&+=])[A-Za-z\d@$!%*?&._#^&+=]{6,32}$`。
- **端到端加密兼容**：流式认证 DTO 支持 WebCrypto GCM 密文（`$V1$GCM$`）在 Controller 层的正则穿透放行，并在 Service 层完成解密后再进行明文复杂度与长度区间强制核验。
- **校验提示信息**：
  - 必填提示："密码不能为空"
  - 复杂度与长度提示："密码必须包含字母、数字和至少一个特殊字符，且长度在6到32个字符之间"
  - 一致性提示："两次输入的密码不一致"

### 2.3 邮箱安全策略 (Email Policy)
- **框架增强型注解**：统一采用 `@com.humolix.volur.framework.common.validation.annotation.Email(message = "邮箱格式不正确或邮件地址不合法")`。
- **大小写规范化**：统一结合 `@Trimmed(toLowerCase = true)` 进行首尾空白剔除与全局小写转换。
- **安全白名单联动**：底层由 `EmailValidator` 联动系统配置 `system.auth.email_whitelist_suffixes`，实现白名单后缀过滤防护。

---

## 3. 改动代码清单

### 3.1 后端领域模型与服务层 (Backend: volur-auth)
1. **`SignUpPasswordRequestDTO.java`**
   - 增加正则约束与长度约束，支持密文放行并在明文时强校验复杂度与 6-32 字符区间。
2. **`SignUpNameRequestDTO.java`**
   - 增加用户名长度与字符集正则约束，规范化错误提示。
3. **`SignUpEmailRequestDTO.java`**
   - 移除标准 `jakarta.validation.constraints.Email`，统一替换为框架增强型 `@Email`，并启用 `@Trimmed(toLowerCase = true)`。
4. **`PasswordRecoveryResetRequestDTO.java`**
   - 密码与确认密码字段引入统一密码安全复杂度正则约束。
5. **`UsernameRecoveryEmailRequestDTO.java`**
   - 统一替换为框架增强型 `@Email` 与 `@Trimmed(toLowerCase = true)`。
6. **`ForgotPasswordRequestDTO.java` / `ResetPasswordRequestDTO.java`**
   - 邮箱注解统一替换为框架增强型 `@Email`。
7. **`SignupServiceImpl.java`**
   - `name`: 解密后强校验 3-32 位及 `^[a-zA-Z0-9_-]+$` 正则。
   - `password`: 解密后强校验 6-32 位及密码复杂度正则。
8. **`RecoveryServiceImpl.java`**
   - `resetPassword`: 解密后强校验 6-32 位及密码复杂度正则。
9. **`RecoveryServiceImplTest.java`**
   - 更新单元测试 Mock 密码与入参，符合新密码复杂度策略（`NewPass1234!`）。

### 3.2 新一代前端 (Frontend: volur-lite-view-NEXT)
1. **`PasswordStepView.vue` (注册流程设置密码)**
   - 引入标准 `PASSWORD_REGEX`。
   - 前端校验由原来 `>= 8` 位提升为 `6 <= length <= 32` 且必须包含字母、数字与特殊字符。
   - 密码提交前统一进行首尾空白清洗。
2. **`ResetView.vue` (找回密码重置新密码)**
   - 引入标准 `PASSWORD_REGEX`。
   - 长度由原 `[8, 64]` 统一对齐为 `[6, 32]` 并校验字母、数字与特殊字符。
3. **`NameStepView.vue` (注册流程用户名)**
   - 确认前端校验区间 `[3, 32]` 与正则 `^[a-zA-Z0-9_-]+$`。
4. **国际化语言文件 (`zh-CN/auth.json` 与 `en-US/auth.json`)**
   - 统一更新 `username_length_error`、`username_pattern_error`、`password_complexity_error`、`password_hint`、`recovery_password_length_error` 等文案。

---

## 4. 验证与回归测试

1. **后端单元测试**：
   - 模块：`volur-framework`, `volur-security`, `volur-auth`
   - 执行命令：`mvn test -pl volur-auth,volur-security,volur-framework`
   - 结果：159/159 测试用例全部通过，0 错误，0 失败。
2. **前端类型检查与产物构建**：
   - 执行命令：`npm run build` (`vue-tsc --noEmit && vite build`)
   - 结果：0 类型错误，产物编译打包成功。
