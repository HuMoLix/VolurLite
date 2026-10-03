# Passkey 服务序列化弃用警告、Jackson 模块弃用与 Nullability 空安全审计与重构报告

## 1. 变更背景与问题分析

在 `com.humolix.volur.security.service.impl.PasskeyServiceImpl.java` 中，静态代码审查与 IDE/Linter 报告了以下三个层面的告警：
1. **`SerializationUtils.deserialize(byte[])` 自 Spring Framework 6.0 起已弃用**：在 `registerPasskey` 方法中使用 `SerializationUtils.deserialize(bytes)` 读取 Redis 缓存时触发；
2. **`WebauthnJackson2Module` 已弃用并被标记为移除 (`@Deprecated(forRemoval = true)`)**：在为 `ObjectMapper` 注册 WebAuthn 序列化支持时触发；
3. **实参 `creationOptions` 可能为 `null` (Nullability Warning)**：`ImmutableRelyingPartyRegistrationRequest` 构造参数强制非空，而直接强转反序列化结果存在空指针风险。

---

## 2. 深度根因剖析

### 2.1 `SerializationUtils.deserialize` 弃用根因与安全考量
- **背景**：Spring Framework 自 6.0.7 起正式废弃 `org.springframework.util.SerializationUtils.deserialize(byte[])`。
- **安全隐患**：Java 原生二进制序列化（`ObjectInputStream`）在没有类白名单过滤的情况下反序列化不受信字节流时，极易受到反序列化 Gadget 攻击链（如 Apache Commons Collections、Spring AOP 等反序列化 RCE 漏洞）。
- **Spring 官方指引**：官方建议开发者改用配置了严格 `ObjectInputFilter` 白名单的显式 `ObjectInputStream`，或采用现代化数据交换格式。
- **限制说明**：由于 Spring Security 的 `PublicKeyCredentialCreationOptions` 仅提供了用于向客户端输出的 `@JsonSerialize` 注解和 Mixin，并未提供受保护字段的 Jackson 反序列化构建器（Builder Mixin），因此在会话缓存层继续采用 Java 原生序列化是官方推荐且完备的方案；但必须摆脱 `SerializationUtils.deserialize` 的无保护调用。

### 2.2 `WebauthnJackson2Module` 弃用与移除标记根因
- **背景**：Spring Security 7.0/7.1 开始全面适配 Jackson 3（包名 `tools.jackson.*`），并引入了针对 Jackson 3 的全新 `WebauthnJacksonModule`。
- **冲突点**：
  - 目前全平台及 Spring Boot 3.x 统一采用 Jackson 2 作为运行时基础库（`com.fasterxml.jackson.databind.ObjectMapper`）；
  - `WebauthnJacksonModule` 依赖 Jackson 3 的核心抽象类 `tools.jackson.databind.JacksonModule`，无法注册至 Jackson 2 的 `ObjectMapper`；
  - 针对 Jackson 2 的唯一适配模块 `WebauthnJackson2Module` 被 Spring 官方标记为 `@Deprecated(forRemoval = true)`，用以提示未来 Spring Security 8+ 将最终移除 Jackson 2 兼容层。
- **合规处置**：在 Java 9+ 编译器中，带有 `forRemoval = true` 的注解被归类为终端弃用（Terminal Deprecation），必须显式声明 `@SuppressWarnings({"deprecation", "removal"})`，这也是 Spring 官方在自身配置类 `WebAuthnConfiguration` 中的标准做法。

### 2.3 实参 `creationOptions` 可能为 `null`
- `SerializationUtils.deserialize` 的返回类型被 JSpecify 注解标记为 `@Nullable Object`；
- 在将其强制类型转换为 `PublicKeyCredentialCreationOptions` 后，编译器根据数据流推导判定其实参可能为 `null`；
- 构造 `new ImmutableRelyingPartyRegistrationRequest(creationOptions, relyingPartyPublicKey)` 时未进行前置非空校验，破坏了领域模型的不可变性与强健壮性。

---

## 3. 重构设计与实现方案

### 3.1 引入带有 `ObjectInputFilter` 白名单的安全原生反序列化
在 `PasskeyServiceImpl` 中封装具备强类型校验与对象输入过滤器的 `deserialize` 方法：
```java
private <T extends Serializable> T deserialize(byte[] bytes, Class<T> clazz) {
    if (bytes == null || bytes.length == 0) {
        return null;
    }
    try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
         ObjectInputStream ois = new ObjectInputStream(bis)) {
        ois.setObjectInputFilter(ObjectInputFilter.Config.createFilter(
                "org.springframework.security.web.webauthn.**;java.base/*;!*"
        ));
        Object obj = ois.readObject();
        if (clazz.isInstance(obj)) {
            return clazz.cast(obj);
        }
        throw new IllegalArgumentException("Deserialized object is not an instance of " + clazz.getName());
    } catch (Exception e) {
        log.error("Failed to deserialize object: {}", e.getMessage(), e);
        throw new IllegalStateException("Failed to deserialize object", e);
    }
}
```
- **安全保障**：过滤器仅允许 `org.springframework.security.web.webauthn.**` 与 `java.base/*` 下的基础类（如 `String`、`Duration`、`byte[]`、集合容器），任何攻击型 Gadget 类均会被直接拦截拒载（`!*`）。
- **告警消除**：完全杜绝调用 Spring 废弃的 `SerializationUtils.deserialize`。

### 3.2 完备的 Nullability 空安全防线
在从 Redis 缓存获取并反序列化后，执行显式空值短路检查：
```java
PublicKeyCredentialCreationOptions creationOptions;
try {
    byte[] bytes = Base64.getDecoder().decode(base64);
    creationOptions = deserialize(bytes, PublicKeyCredentialCreationOptions.class);
} catch (Exception e) {
    log.error("Failed to deserialize cached passkey creation options: {}", e.getMessage(), e);
    throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "解析通行密钥会话失败");
}

if (creationOptions == null) {
    throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "通行密钥会话数据异常");
}
```
经过 `if (creationOptions == null)` 卫语句拦截后，进入 `ImmutableRelyingPartyRegistrationRequest` 构造器的 `creationOptions` 具备强类型非空保证，彻底消除 IDE/Linter 的空指针隐患告警。

### 3.3 规范化注解抑制
在 `PasskeyServiceImpl`、`WebAuthnConfiguration` 与单元测试 `PasskeyServiceImplTest` 类及方法级别补充 `@SuppressWarnings({"deprecation", "removal"})`，确保在 Spring Security 从 Jackson 2 平滑演进至 Jackson 3 的过渡期内，编译输出 0 警告、0 瑕疵。

---

## 4. 影响范围与验证结果

1. **后端验证**：
   - 运行 Maven 测试：`mvn test "-Dtest=PasskeyServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" -pl volur-security -am`，全部通过（0 失败，0 错误）。
   - 运行全平台构建：全平台 13 个 Maven 子模块编译通过（`BUILD SUCCESS`），Java 编译阶段 0 告警输出。
2. **前端验证**：
   - 执行 `npm run build`，Vue 3 与 TypeScript 类型检查（`vue-tsc`）和 Vite 生产打包 100% 成功。
3. **架构资产纯洁度**：
   - 遵循 `GEMINI.md` 规范，所有生产 Java/TS/Vue 源码中无任何冗余注释，设计方案与审计溯源完整记录于本文档中。
