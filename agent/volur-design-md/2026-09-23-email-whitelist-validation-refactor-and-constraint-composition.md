# 邮箱域名白名单校验重构与 Bean Validation 复合约束下沉

## 1. 变更背景与架构目标

在原有的用户注册业务流程中，邮箱域名白名单后缀的校验逻辑位于业务服务层 [`AuthServiceImpl.register()`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/AuthServiceImpl.java)：
```java
String whitelistStr = systemSettingService.getSettingValue(SystemSettingKeys.AUTH_EMAIL_WHITELIST_SUFFIXES, "");
if (StringUtils.hasText(whitelistStr)) {
    String emailDomain = normalizedEmail.substring(normalizedEmail.lastIndexOf("@") + 1);
    boolean isWhitelisted = Arrays.stream(whitelistStr.split(","))
            .map(String::trim)
            .map(String::toLowerCase)
            .filter(StringUtils::hasText)
            .map(suffix -> suffix.startsWith("@") ? suffix.substring(1) : suffix)
            .anyMatch(allowedDomain -> emailDomain.equals(allowedDomain) || emailDomain.endsWith("." + allowedDomain));
    if (!isWhitelisted) {
        throw new BusinessException(HttpStatus.BAD_REQUEST, "不支持该邮箱后缀注册，仅限: " + whitelistStr);
    }
}
```

该实现存在以下架构设计上的不足：
1. **参数校验与业务逻辑耦合**：邮箱格式与域名白名单属于输入参数合法性校验（Input Parameter Validation），应当在 Controller 层借助框架的声明式校验机制快速前置拦截，而非侵入到核心业务事务层执行。
2. **复用性低**：如果其他入口（例如邀请成员、修改邮箱等）需要同样的白名单限制，往往会导致重复编写域名解析与正则切割逻辑。
3. **报错不统一**：若通过业务异常手动抛出，其异常结构与 Spring MVC 标准 `@Valid` 引发的 `MethodArgumentNotValidException` 产生路径分离。

为彻底解决上述问题，本轮重构将邮箱白名单校验下沉至框架级 Bean Validation 注解体系，并遵循复合约束（Constraint Composition / 约束继承）标准设计，将其直接融合到平台增强型 `@Email` 注解中。

---

## 2. 核心架构设计与改动点

### 2.1 框架级 SPI 抽象：`EmailWhitelistProvider`
在 `volur-framework` 中定义轻量级函数式接口 [`EmailWhitelistProvider`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/validation/provider/EmailWhitelistProvider.java)：
- 负责向校验器解耦提供系统当前配置的邮箱白名单后缀字符串（`system.auth.email_whitelist_suffixes`）；
- 避免 `volur-framework` 对 `volur-system` 产生反向依赖，彻底保证 Maven 模块单向依赖（DAG）架构。

### 2.2 系统级实现：`SystemEmailWhitelistProvider`
在 `volur-system` 中实现该接口 [`SystemEmailWhitelistProvider`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/provider/SystemEmailWhitelistProvider.java)：
- 通过 `SystemSettingService` 动态读取二级缓存及数据库中的 `SystemSettingKeys.AUTH_EMAIL_WHITELIST_SUFFIXES`。

### 2.3 复合约束注解：`@Email`
在 `volur-framework` 的 `com.humolix.volur.framework.common.validation.annotation` 包下创建 [`Email`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/validation/annotation/Email.java)：
- 在注解声明上组合了标准 `@jakarta.validation.constraints.Email`；
- 通过 `@OverridesAttribute` 将 `message`、`groups`、`payload`、`regexp`、`flags` 等属性完全透传给底层的 Jakarta Validation 原生注解；
- 扩展 `checkWhitelist` 属性（默认 `true`），支持业务方灵活选择是否开启系统域名白名单校验。

### 2.4 校验器实现：`EmailValidator`
创建 [`EmailValidator`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/validation/validator/EmailValidator.java)：
- **标准兼容**：对于 `null` 或全空白字符串直接返回 `true`（严格遵循 Bean Validation 规范，将非空约束职责让渡给 `@NotBlank` / `@NotNull`）；
- **动态违规信息**：当命中白名单配置且邮箱域名未在允许范围内时，通过 `context.disableDefaultConstraintViolation()` 与 `context.buildConstraintViolationWithTemplate("不支持该邮箱后缀注册，仅限: " + whitelistStr)` 动态构造精准的错误提示；
- **双模构造与解耦**：同时支持无参构造（Spring 运行时通过 `SpringContextHolder` 自动解析 Provider）与有参构造（便于独立单元测试或无 Spring 上下文环境测试）；
- **静态工具抽取**：提供 `public static boolean isDomainWhitelisted(String emailDomain, String whitelistStr)` 方便在其他非注解场景下复用判定。

### 2.5 业务层与 DTO 适配
1. **[`RegisterRequestDTO`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/dto/RegisterRequestDTO.java)**：
   将 `jakarta.validation.constraints.Email` 替换为增强型 `@Email`：
   ```java
   @NotBlank(message = "邮箱不能为空")
   @Email(message = "邮箱格式不正确")
   private String email;
   ```
2. **[`AuthServiceImpl`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/AuthServiceImpl.java)**：
   清理 `register()` 中冗余的手动白名单提取与异常抛出逻辑，代码更加内聚轻量。

---

## 3. 非空约束分析（强制要求 email 有值判定）

针对**刚创建的 `@Email` 是否会强制要求 email 有值**的问题，核心结论与技术分析如下：

1. **`@Email` 自身绝不会强制要求字段非空（No Mandatory Value Enforcement）**：
   - **规范原则**：根据 Jakarta Bean Validation (JSR 380) 铁律与业界最佳实践，所有格式型校验器（如 `@Email`、`@Phone`、`@Pattern`、`@Size`、`@IdentificationCard` 等）对 `null` 与空字符串必须视作合法（返回 `true`）。
   - **源码保证**：
     ```java
     if (value == null) {
         return true;
     }
     String emailStr = value.toString().trim().toLowerCase();
     if (emailStr.isEmpty()) {
         return true;
     }
     ```
   - **设计考量**：若 `@Email` 强制非空，会导致在诸如“个人资料修改（邮箱可选更新）”、“第三方 OAuth 补全资料”等场景下，可选的邮箱字段若未填写也会被误拦截。
2. **动态开关模式下的非空控制**：
   - 平台对于邮件验证采用开关模式（`system.auth.require_email_verification` 可动态开启或关闭）：
     - 因此在 [`RegisterRequestDTO`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/dto/RegisterRequestDTO.java) 中，**彻底移除了静态的 `@NotBlank` / `@NotNull` 约束**，仅保留 `@Email(message = "邮箱格式不正确")`；
     - 当传入 `null` 或空字符串 `""` 时，`@Email` 按照 Jakarta Bean Validation 规范直接放行；
     - 当系统开启邮件验证（`requireEmailVerification == true`）时，由业务层 [`AuthServiceImpl.register()`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/AuthServiceImpl.java) 动态判定：
       ```java
       if (requireEmailVerification) {
           if (!StringUtils.hasText(normalizedEmail)) {
               throw new BusinessException(HttpStatus.BAD_REQUEST, "邮箱不能为空");
           }
           // 校验邮箱验证码
       }
       ```
       从而保证在强验证场景下邮箱必填，而在离线/无需邮箱验证的场景下入参 DTO 具备最大的动态扩展性与解耦度。

---

## 4. 影响范围与测试验证

- **影响范围**：
  - 平台通用校验包（`volur-framework`）：新增复合注解 `@Email`、提供者接口 `EmailWhitelistProvider` 及校验器 `EmailValidator`；
  - 系统配置包（`volur-system`）：新增 `SystemEmailWhitelistProvider`；
  - 认证鉴权包（`volur-auth`）：优化 `RegisterRequestDTO` 字段约束（移除静态 `@NotBlank` / `@NotNull`，仅保留增强型 `@Email`），并在 `AuthServiceImpl.register()` 中进行空安全提取与开关级动态拦截。
- **单元测试验证**：
  - 新增 [`EmailValidatorTest`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/test/java/com/humolix/volur/framework/common/validation/EmailValidatorTest.java)：14 个测试用例全部通过，覆盖空值放行、白名单精确匹配、子域名匹配、前缀 `@` 容错、格式非法拦截与白名单关闭等场景；
  - 新增 [`RegisterRequestDTOValidationTest`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/test/java/com/humolix/volur/auth/RegisterRequestDTOValidationTest.java)：4 个测试用例全部通过，验证了白名单拦截与动态报错、合法白名单放行、格式错误拦截、以及在无 `@NotNull` 下空值安全放行；
  - 强化 [`AuthVerificationCrossValidationTest`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/test/java/com/humolix/volur/auth/AuthVerificationCrossValidationTest.java)：覆盖开启强邮箱核验但未传邮箱时抛出异常的业务用例；
  - `volur-auth` 模块全量 32 个单元测试 100% 通过。
