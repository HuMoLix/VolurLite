# Volur Lite 架构演进与设计审计记录：彻底移除 SpringDoc OpenAPI 并全量升级至 Jackson 3

## 1. 变更背景与演进动机

在 Volur Lite 平台原有的技术架构中，API 文档由 SpringDoc OpenAPI (`springdoc-openapi-starter-webmvc-ui:3.1.0`) 支撑，而 JSON 序列化层随着向 Spring Boot 4.1.0 / Spring Security 7.1.0 演进，形成了 Jackson 2 (`com.fasterxml.jackson.*`) 与 Jackson 3 (`tools.jackson.*`) 并存的双轨状态。

为了保障架构纯粹性、消除潜在安全暴露面并达成统一的高性能序列化标准，平台推进了如下彻底治理：

1. **阻断隐式传递 Jackson 2 的核心根源**：
   SpringDoc OpenAPI 3.1.0 强依赖 `io.swagger.core.v3:swagger-core-jakarta:2.2.52`，后者传递引入了 `com.fasterxml.jackson.core:jackson-databind:2.21.4`、`jackson-core:2.21.4`、`jackson-dataformat-yaml:2.21.4` 及 `jackson-datatype-jsr310:2.21.4`，导致平台 13 个子模块类路径上长期残留 Jackson 2 构件。
2. **生产安全收敛与端点暴露消除**：
   Swagger / OpenAPI 在生产环境下存在接口结构泄露与自动化渗透嗅探风险。移除 SpringDoc 后，彻底删除所有相关过滤器白名单与静态资源放行逻辑，进一步加固攻击面防护。
3. **全平台向 Jackson 3 (`tools.jackson.*`) 深度统一**：
   Spring Boot 4 原生采用 Jackson 3 作为下一代 JSON 引擎。全面消除业务代码中的 `com.fasterxml.jackson.databind` 与 `com.fasterxml.jackson.core` 引用，将 WebAuthn、人机风控、密文解密、请求清洗反序列化器等关键链路无缝迁移至 Jackson 3 原生生态。

---

## 2. 核心架构改造与改动清单

### 2.1 依赖体系彻底剔除 (Maven POMs)

1. **根 POM (`src/volur-lite-platform/pom.xml`)**：
   - 删除属性 `<springdoc.version>3.1.0</springdoc.version>`；
   - 从 `<dependencyManagement>` 中彻底移除 `org.springdoc:springdoc-openapi-starter-webmvc-ui`。
2. **各子模块 POM 对齐**：
   - **`volur-framework/pom.xml`**：移除 `springdoc-openapi-starter-webmvc-ui`；
   - **`volur-logging/pom.xml`**：移除 `springdoc-openapi-starter-webmvc-ui`；
   - **`volur-security/pom.xml`**：移除 `springdoc-openapi-starter-webmvc-ui`。

### 2.2 源码层 SpringDoc / Swagger 注解与配置全量清理

1. **删除配置类**：
   - 彻底删除 [`OpenApiConfig.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/config/OpenApiConfig.java)。
2. **配置文件清理**：
   - 在 [`application.yml`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-server/src/main/resources/application.yml) 与 [`application-dev.yml`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-server/src/main/resources/application-dev.yml) 中彻底删除 `springdoc:` 相关配置块。
3. **批量清理注解与导包**：
   - 编写并执行 Agent 批量处理脚本 [`strip_swagger.py`](file:///H:/HuMoLix/github/Volur-Lite/agent/volur-design-scripts/strip_swagger.py)，全量扫描并清理了平台 106 个 Java 文件中的 Swagger 痕迹：
     - 控制器类及方法级别的 `@Tag(...)`、`@Operation(...)`、`@Parameter(...)`；
     - 领域模型（DTO / VO）属性与类级别的 `@Schema(...)`；
     - `import io.swagger.v3.oas.annotations.*` 以及 `import org.springdoc.*`。
4. **安全过滤器与白名单去冗余**：
   - **[`SecurityWhitelistProperties.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/config/SecurityWhitelistProperties.java)**：移除 `/v3/api-docs/**`、`/swagger-ui/**`、`/swagger-ui.html`、`/doc.html`、`/webjars/**`；
   - **[`MaintenanceModeFilter.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/security/filter/MaintenanceModeFilter.java)**：同步移除上述 Swagger 白名单路径；
   - **[`RiskIpFilter.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/security/filter/RiskIpFilter.java)**：移除请求路径前缀为 `/v3/api-docs` 与 `/swagger-ui` 的跳过拦截分支。

### 2.3 Jackson 3 全量统一改造

1. **核心配置层 (`volur-framework`)**：
   - **[`JacksonConfig.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/config/JacksonConfig.java)**：
     - 彻底删除兼容旧版的 `legacyObjectMapper` Bean（原返回 `com.fasterxml.jackson.databind.ObjectMapper`）；
     - 保持 `@Primary JsonMapper objectMapper(ObjectProvider<List<JacksonModule>>)`，并在构建器中自动注册 Spring 上下文中注入的全部 `JacksonModule`，使各业务模块的自定义序列化扩展开箱即用。
2. **声明式清洗注解与反序列化器 (`volur-framework`)**：
   - **[`Trimmed.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/annotation/Trimmed.java)**：将注解挂载目标迁移至 Jackson 3 原生 `tools.jackson.databind.annotation.JsonDeserialize`；
   - **[`StringTrimmingDeserializer.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/jackson/StringTrimmingDeserializer.java)**：
     - 继承体系重构为 Jackson 3 原生的 `tools.jackson.databind.ValueDeserializer<String>`；
     - 重写 `createContextual(DeserializationContext, BeanProperty)` 方法与 `deserialize(JsonParser, DeserializationContext)`；
     - 抛出标准 `tools.jackson.core.JacksonException`。
   - **[`StringTrimmingDeserializerTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/test/java/com/humolix/volur/framework/common/StringTrimmingDeserializerTest.java)**：测试套件升级使用 Jackson 3 `JsonMapper`。
3. **WebAuthn 与安全模块 (`volur-security`)**：
   - **[`WebAuthnConfiguration.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/config/WebAuthnConfiguration.java)**：
     - 将原已弃用的 `WebauthnJackson2Module` 升级为 Spring Security 7 原生现代模块 `org.springframework.security.web.webauthn.jackson.WebauthnJacksonModule`（实现自 `tools.jackson.databind.JacksonModule`）；
     - 移除 `@SuppressWarnings({"deprecation", "removal"})` 注解。
   - **[`PasskeyServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/service/impl/PasskeyServiceImpl.java)**：
     - 依赖注入类型切换至 `tools.jackson.databind.ObjectMapper` 与 `tools.jackson.core.type.TypeReference`；
     - 凭据反序列化借助全局已注册的 `WebauthnJacksonModule` 自动完成，消除旧有动态注册逻辑。
   - **[`PasskeyServiceImplTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/test/java/com/humolix/volur/security/service/PasskeyServiceImplTest.java)**：单测通过 `JsonMapper.builder().addModule(new WebauthnJacksonModule()).build()` 建立隔离测试实例。
   - **[`RecaptchaServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/service/impl/RecaptchaServiceImpl.java)**：
     - 客户端行为报文解析链路切换至 `tools.jackson.databind.JsonNode` 与 `tools.jackson.databind.ObjectMapper`。
   - **[`RecaptchaServiceImplTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/test/java/com/humolix/volur/security/service/RecaptchaServiceImplTest.java)**：测试套件对齐 Jackson 3 `JsonMapper`。
4. **人机验证加密服务 (`volur-verification`)**：
   - **[`CaptchaCryptoService.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/crypto/CaptchaCryptoService.java)**：
     - 密文反序列化全面升级至 `tools.jackson.databind.json.JsonMapper` 与 `tools.jackson.databind.JsonNode`，彻底斩断对 Jackson 2 的引用。

---

## 3. 架构对比与升级收益

| 维度 | 改造前 (SpringDoc + Jackson 2 混用) | 改造后 (零 SpringDoc + 全面 Jackson 3) | 改造收益 |
| :--- | :--- | :--- | :--- |
| **API 文档与注解污染** | 100+ 类强侵入 `@Schema` / `@Operation` / `@Tag` | 零第三方侵入，纯净自解释领域模型与干净控制器 | 降低模型与框架耦合，提升构建与阅读效率 |
| **安全攻击面** | 存在 Swagger UI 与 OpenAPI 静态资源/端点暴露风险 | 完全封堵 OpenAPI 端点，安全过滤器无死角防护 | 杜绝未经授权的端点探测与元数据泄露 |
| **JSON 依赖纯粹度** | 双轨运行：Jackson 2 与 Jackson 3 同时驻留类路径 | 单一真实源：全平台仅保留 `tools.jackson.*` (Jackson 3) | 彻底消除类加载冲突与反序列化语义不一致隐患 |
| **WebAuthn 适配** | 使用标记弃用的 `WebauthnJackson2Module` | 原生集成 Spring Security 7 `WebauthnJacksonModule` | 符合最新安全框架标准，消除编译器告警 |
| **入参清洗性能** | 基于 Jackson 2 反射上下文提取 | 基于 Jackson 3 现代 `ValueDeserializer` 上下文解析 | 提升 HTTP 请求入参绑定性能 |

---

## 4. 验证与审计结论

1. **依赖树审计结果**：
   - 执行 `mvn dependency:tree -Dincludes="*jackson*"` 验证：全平台 13 个子模块中已**完全不存在任何 Jackson 2 数据绑定构件**（`com.fasterxml.jackson.core:jackson-databind` 彻底归零）；
   - 依赖中仅存在 Jackson 3 官方规范所定义的 `com.fasterxml.jackson.core:jackson-annotations:2.21`（标准通用注解）以及 `tools.jackson.*:3.1.4` 核心构件。
2. **源码全局检索审计**：
   - 全局检索 `com.fasterxml.jackson.databind` 与 `com.fasterxml.jackson.core`：**匹配数为 0**；
   - 全局检索 `WebauthnJackson2Module`：**匹配数为 0**；
   - 全局检索 `io.swagger` 与 `org.springdoc`：**匹配数为 0**。
3. **编译构建与单元测试验证**：
   - `mvn clean test-compile`：全平台 13 个微模块全部通过，0 编译错误；
   - `mvn test`：覆盖认证流、Passkey 凭据、行为验证码、令牌签发、竞赛排行榜等全部单元测试，**100% 测试通过（BUILD SUCCESS）**。
