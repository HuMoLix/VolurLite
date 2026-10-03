# Volur Lite 架构演进与设计审计记录：彻底移除 JJWT 并原生集成 Spring Security OAuth2 JOSE

## 1. 变更背景与演进动机

在 Volur Lite 平台原有的认证与授权架构中，JWT 令牌的签发与验签由 `io.jsonwebtoken:jjwt-*` (0.12.6) 承担。随着平台向 Spring Boot 4.x / Spring Security 7.1.0 以及现代安全基准演化，引入 JJWT 产生了一系列架构与依赖层面的阻抗：

1. **隐式引入 Jackson 2 传递依赖**：
   `jjwt-jackson` (0.12.6) 强依赖 `com.fasterxml.jackson.core:jackson-databind:2.x`。在全平台推进 Jackson 3（`tools.jackson.*`）统一化与轻量化治理过程中，JJWT 成为引入旧版 Jackson 2 的核心根源之一。
2. **安全技术栈割裂与重复发明轮子**：
   平台核心安全框架已经基于 Spring Security 7.1.0 构建。Spring Security 官方原生提供了基于 Nimbus JOSE+JWT 的成熟实现 `spring-security-oauth2-jose`，具备对 RFC 7519 (JWT)、RFC 7515 (JWS) 等标准的完备支持与最高等级的安全审计维护。继续使用第三方 JJWT 导致安全框架与令牌处理机制割裂。
3. **架构纯粹性诉求**：
   移除第三方 JJWT 库，全面转为 Spring 官方第一方安全模块 `org.springframework.security:spring-security-oauth2-jose`，统一密钥管理、JWS 编解码器（`NimbusJwtEncoder`、`NimbusJwtDecoder`）与异常处理体系。

---

## 2. 核心架构改造与改动清单

### 2.1 依赖体系重构 (Maven POMs)

1. **父工程与依赖管理 (`src/volur-lite-platform/pom.xml`)**：
   - 彻底删除 `<jjwt.version>0.12.6</jjwt.version>` 属性配置；
   - 在 `<dependencyManagement>` 中移除 `io.jsonwebtoken:jjwt-api`、`jjwt-impl`、`jjwt-jackson`；
   - 引入 Spring Security 官方统一版本属性 `<spring-security.version>7.1.0</spring-security.version>`，并声明 `org.springframework.security:spring-security-oauth2-jose:${spring-security.version}` 的集中依赖管理。
2. **各子模块依赖对齐**：
   - **`volur-framework/pom.xml`**：移除 `jjwt-api`，引入 `spring-security-oauth2-jose`；
   - **`volur-security/pom.xml`**：移除 `jjwt-api`，引入 `spring-security-oauth2-jose`；
   - **`volur-auth/pom.xml`**：移除 `jjwt-api`、`jjwt-impl`、`jjwt-jackson`，引入 `spring-security-oauth2-jose`。

### 2.2 核心组件改造 (`volur-auth`)

1. **`JwtUtil.java` 重构**：
   - **编码与解码引擎**：
     - 使用 `javax.crypto.SecretKeySpec` 构建基于 HMAC-SHA256 的密钥对象，密钥自动支持 Base64 解码与 256 位安全长度填充；
     - 注入基于 Nimbus 的官方 JWS 编码器与解码器：
       - `jwtEncoder = NimbusJwtEncoder.withSecretKey(this.cachedSecretKey).algorithm(MacAlgorithm.HS256).build()`；
       - `jwtDecoder = NimbusJwtDecoder.withSecretKey(this.cachedSecretKey).macAlgorithm(MacAlgorithm.HS256).build()`。
   - **令牌签发 (Token Issuance)**：
     - 摒弃 JJWT 的流式 `Jwts.builder()`；
     - 采用 Spring Security 标准的 `JwtClaimsSet` 与 `JwsHeader`：
       - 包含 `subject`、`id` (jti)、`issuedAt` (`Instant`)、`expiresAt` (`Instant`)；
       - 自定义 Claims：`authorities` (角色列表)、`userId`、`type` ("access" / "refresh")、`rememberMe` (布尔值)；
       - 通过 `jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claimsSet)).getTokenValue()` 生成标准 JWS Compact 字符串。
   - **令牌解析与类型映射 (Token Parsing & Claims Extraction)**：
     - 将原 JJWT 的 `io.jsonwebtoken.Claims` 替换为 Spring Security 标准的 `org.springframework.security.oauth2.jwt.Jwt`；
     - 提供 `getClaimsFromToken(token)`，通过 `jwtDecoder.decode(token)` 校验并返回 `Jwt` 实例；
     - 提供便捷属性提取方法：`getUsernameFromToken`、`getUserIdFromTokenAsLong`、`getJtiFromToken`、`getRolesFromToken`、`getExpirationTimeFromToken` 等。
   - **异常处理体系对齐**：
     - `JwtValidationException`（例如令牌过期 `Jwt expired at ...`）-> 统一映射并抛出平台核心异常 `TokenExpiredException`；
     - `BadJwtException`（签名错误、格式损毁）-> 统一映射并抛出 `TokenMalformedException`；
     - `JwtException`（通用解析异常）-> 统一映射并抛出 `InvalidTokenException`。
2. **`JwtAuthFilter.java` 拦截器对齐**：
   - 移除所有 `io.jsonwebtoken` 异常类（`ExpiredJwtException`、`MalformedJwtException`、`SecurityException` 等）的 `catch` 分支；
   - 统一捕获 `TokenExpiredException`、`TokenMalformedException`、`InvalidTokenException`、`JwtException` 与平台业务异常；
   - 提取 Claim 逻辑对齐 `Jwt` 对象方法（`jwt.getId()`、`jwt.getSubject()`、`jwt.getIssuedAt()` 等）。
3. **`AuthServiceImpl.java` 业务层对齐**：
   - 彻底删除 `import io.jsonwebtoken.Claims`；
   - 将 `refreshToken(token)` 与 `logout(token)` 中的令牌声明类型无缝迁移至 `org.springframework.security.oauth2.jwt.Jwt`。

### 2.3 跨模块协作改造 (`volur-competition`)

1. **`CompetitionChatHandshakeInterceptor.java`**：
   - 彻底删除 `import io.jsonwebtoken.Claims`；
   - 统一使用 `org.springframework.security.oauth2.jwt.Jwt` 提取用户身份与权限信息，保障 WebSocket 鉴权链路的一致性与稳定性。

### 2.4 单元测试套件与自动化验证

1. **新增 `JwtUtilTest.java`**：
   - 验证 `NimbusJwtEncoder` / `NimbusJwtDecoder` 的完整生命周期：
     - Access Token 签发、验签、用户 ID 与角色提取；
     - Refresh Token 签发与 RememberMe 标志位保持；
     - 黑名单拦截机制与令牌防篡改测试（篡改签名捕获 `TokenMalformedException`）；
     - HTTP Header（`Authorization: Bearer <token>`）及 Query 参数提取解析。
2. **全平台回归测试**：
   - 执行 `mvn test-compile` 与 `mvn test`，覆盖全平台 13 个微模块；
   - 43+ 个核心单元测试全部通过，无任何回归问题。

---

## 3. 架构对比与收益总结

| 维度 | JJWT (io.jsonwebtoken 0.12.6) | Spring Security OAuth2 JOSE (7.1.0) | 改进收益 |
| :--- | :--- | :--- | :--- |
| **底层引擎** | JJWT 自研流式构建器 + Jackson 2 序列化适配器 | Nimbus JOSE+JWT 10.9（行业黄金标准） | 遵循 RFC 7515/7519 规范，无专有黑盒实现 |
| **第三方依赖** | 强绑定 `com.fasterxml.jackson.core:jackson-databind:2.x` | 零 Jackson 依赖（原生轻量编解码） | 彻底斩断 `volur-auth` 对 Jackson 2 的依赖传递 |
| **框架集成度** | 第三方库，与 Spring Security 架构割裂 | Spring Security 官方第一方标准模块 | 与 Spring Security 7.x 过滤器与异常无缝融合 |
| **代码可维护性** | 易与 Spring Security 原生类型（如 `Jwt`）冲突混淆 | 全链路统一采用 `org.springframework.security.oauth2.jwt.Jwt` | 消除异构模型，提升类型安全性 |

---

## 4. 影响范围与迁移结论

- **影响模块**：`volur-framework`、`volur-security`、`volur-auth`、`volur-competition`。
- **全库检索状态**：经全局检索，整个 Volur Lite 项目中已**完全不存在**任何 `io.jsonwebtoken` 或 `jjwt` 命名空间的代码引用与依赖声明。
- **构建状态**：全平台编译通过，单元测试全部通过，依赖树中 `volur-auth` 模块已彻底消除 Jackson 2。
