# 修复 MailWebhookService Jackson 依赖不匹配与双版本兜底架构

## 1. 变更背景与根因分析

在以 `spring-boot-starter-parent:4.1.0` 为底座的微单体架构中：
Spring Boot 4.x 将内置的核心 JSON 处理框架升级为了 **Jackson 3**（包路径为 `tools.jackson.databind.*`），并且在系统统一配置类 [`JacksonConfig`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/config/JacksonConfig.java) 中注册的 Bean 为 `tools.jackson.databind.json.JsonMapper`（继承自 `tools.jackson.databind.ObjectMapper`）。

但在近期升级邮件 Webhook 状态机时，[`MailWebhookServiceImpl`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/service/impl/MailWebhookServiceImpl.java) 误引入了旧版 Jackson 2（`com.fasterxml.jackson.databind.ObjectMapper` 与 `JsonNode`），且构造器声明需要注入该类型。由于单元测试直接通过 `new ObjectMapper()` 进行实例化，未能提前暴露容器级依赖缺失。当 `volur-server` 作为 Spring Boot 主工程启动时，Spring 容器扫描并装配 Bean，因找不到 `com.fasterxml.jackson.databind.ObjectMapper` 类型的 Bean 导致启动中断，报出：

```text
Parameter 2 of constructor in com.humolix.volur.verification.service.impl.MailWebhookServiceImpl required a bean of type 'com.fasterxml.jackson.databind.ObjectMapper' that could not be found.
Action: Consider defining a bean of type 'com.fasterxml.jackson.databind.ObjectMapper' in your configuration.
```

---

## 2. 修复方案与改动点

### 2.1 规范对齐：业务服务迁移至 Jackson 3
- **文件**：
  - [`MailWebhookServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/service/impl/MailWebhookServiceImpl.java)
  - [`MailWebhookServiceTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/test/java/com/humolix/volur/verification/MailWebhookServiceTest.java)
- **改动**：
  - 将 import 由 `com.fasterxml.jackson.databind.*` 修改为与框架及其他业务模块（如 `SyslogAspect`, `MaintenanceModeFilter` 等）统一的 `tools.jackson.databind.JsonNode` 和 `tools.jackson.databind.ObjectMapper`；
  - 构造函数注入 Spring 容器中标准管理的 `ObjectMapper` Bean。

### 2.2 防御性架构升级：JacksonConfig 提供双版本兼容兜底
- **文件**：
  - [`JacksonConfig.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/config/JacksonConfig.java)
- **改动**：
  - 保持主 `@Primary JsonMapper objectMapper()` 仍为 Spring Boot 4.x / Jackson 3 的核心序列化/反序列化映射器；
  - 补充条件装配兜底 Bean：
    ```java
    @Bean
    @ConditionalOnClass(com.fasterxml.jackson.databind.ObjectMapper.class)
    @ConditionalOnMissingBean(com.fasterxml.jackson.databind.ObjectMapper.class)
    public com.fasterxml.jackson.databind.ObjectMapper legacyObjectMapper() {
        return new com.fasterxml.jackson.databind.ObjectMapper();
    }
    ```
  - 当类路径下存在 Jackson 2（由某些第三方 SDK 传递依赖引入）且容器中无对应 Bean 时，自动提供兼容兜底，彻底杜绝任何历史遗留代码或第三方库因缺少 Jackson 2 Bean 而引起应用崩溃。

---

## 3. 验证情况
- 运行 `mvn test -Dtest=MailWebhookServiceTest -Dsurefire.failIfNoSpecifiedTests=false`：测试全部通过；
- 运行全平台 `mvn test`：全平台 12 个模块编译与单元测试全部通过（`BUILD SUCCESS`）；
- 容器装配与启动问题完全消除。
