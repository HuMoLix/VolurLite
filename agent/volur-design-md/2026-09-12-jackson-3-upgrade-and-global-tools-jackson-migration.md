# 施工与审计记录：Jackson 3.x 升级与全局 tools.jackson 迁移改造

## 1. 基本信息

- **任务名称**：Jackson 从 2.x 全面平滑升级至 3.x，全工程全局迁移为 `tools.jackson.*`
- **执行时间**：2026-09-12
- **影响模块**：
  - `volur-framework`（核心配置 `JacksonConfig`、工具类 `PageTokenUtil`、持久层属性转换器 `AttachmentConverter`）
  - `volur-auth`（Security 过滤器链与异常响应：`JwtAuthFilter`、`MaintenanceModeFilter`、`SecurityAccessDeniedHandler`、`SecurityAuthenticationEntryPoint`）
  - `volur-logging`（审计切面 `SyslogAspect` 与日志视图 `AdminLogVO`）
  - `volur-notification`（站内消息视图 `BroadcastMessageVO`）
  - `volur-container`（容器与集群视图：`AdminClusterImageDetailVO`、`AdminClusterImageVO`、`AdminClusterNodeVO`、`AdminContainerInstanceVO`）
  - `volur-server`（全局 Web 序列化与应用主入口）
- **关联核心文件**：
  - [`JacksonConfig.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/config/JacksonConfig.java)
  - [`PageTokenUtil.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/util/PageTokenUtil.java)
  - [`AttachmentConverter.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/domain/converter/AttachmentConverter.java)
  - [`SyslogAspect.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-logging/src/main/java/com/humolix/volur/logging/aspect/SyslogAspect.java)
  - [`SecurityAccessDeniedHandler.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/security/exception/SecurityAccessDeniedHandler.java)
  - [`SecurityAuthenticationEntryPoint.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/security/exception/SecurityAuthenticationEntryPoint.java)
  - [`MaintenanceModeFilter.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/security/filter/MaintenanceModeFilter.java)
  - [`JwtAuthFilter.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/security/jwt/JwtAuthFilter.java)

---

## 2. 变更背景与架构对齐

1. **Spring Boot 4.x / Spring 7 原生 Jackson 3.x 体系**：
   - Volur Lite 运行在 Spring Boot 4.1.0 与 Java 25 运行时上，底层默认引入 `tools.jackson.core:jackson-databind:3.1.4` 与 `tools.jackson.core:jackson-core:3.1.4`；
   - 此前平台中部分配置类（如 `JacksonConfig`）及业务过滤器、VO 注解仍在使用 Jackson 2.x 的 `com.fasterxml.jackson.databind.*` 和 `com.fasterxml.jackson.core.*` 包名；
   - 这会导致两套 Jackson 库共存时的隐式冲突（例如 Jackson 2 的 `@JsonSerialize` 无法被 Spring Boot 4 默认激活的 Jackson 3 `ObjectMapper` 识别，导致前端接收雪花 ID 时丢失精度）。
2. **Jackson 3.x 核心设计演进**：
   - **包名重构**：`com.fasterxml.jackson.core` 和 `com.fasterxml.jackson.databind` 统一迁移至 `tools.jackson.core.*` 与 `tools.jackson.databind.*`；
   - **基础注解保留**：FasterXML 架构设计中标准元注解（`@JsonValue`, `@JsonFormat`, `@JsonIgnore`, `@JsonBackReference` 等）保留在 `com.fasterxml.jackson.annotation`；而 Databind 扩展注解（`@JsonSerialize`, `@JsonDeserialize`, `@JsonNaming`）迁移至 `tools.jackson.databind.annotation.*`；
   - **JavaTime 原生内置**：Jackson 3 原生整合了 Java 8 Date/Time API，废弃并移除了独立的 `JavaTimeModule`，日期特性统一收敛在 `DateTimeFeature` 枚举中（如 `DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS`）；
   - **异常体系非受检化**：`JacksonException` 继承自 `RuntimeException`，替代旧版 checked exception `JsonProcessingException`。

---

## 3. 具体改动点详情

### 3.1 `JacksonConfig.java` 全面升级 Jackson 3
- 废弃并移除 `com.fasterxml.jackson.*` 与 `com.fasterxml.jackson.datatype.jsr310.JavaTimeModule` 依赖；
- 引入 `tools.jackson.databind.*` 与 `tools.jackson.databind.cfg.DateTimeFeature`；
- 构建器配置更新：
  - 返回类型变更为 `tools.jackson.databind.json.JsonMapper`（其继承自 `tools.jackson.databind.ObjectMapper`，同时完美满足 `ObjectMapper` 和 `JsonMapper` 依赖注入需求，并阻止 Spring Boot 4 `JacksonAutoConfiguration` 重复创建默认 Bean）；
  - 使用 `disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)` 配置日期时间格式化；
  - 注册 `SimpleModule` 将 `Long.class` 和 `Long.TYPE` 绑定至 `tools.jackson.databind.ser.std.ToStringSerializer.instance`，确保雪花 ID 在 JSON 序列化为 String。

### 3.2 序列化与工具类全局迁移
- **`PageTokenUtil.java`**：
  - 将 `com.fasterxml.jackson.databind.ObjectMapper` 迁移至 `tools.jackson.databind.ObjectMapper`。
- **`AttachmentConverter.java`**：
  - 将 `com.fasterxml.jackson.core.JsonProcessingException` 替换为 `tools.jackson.core.JacksonException`；
  - 将 `TypeReference` 替换为 `tools.jackson.core.type.TypeReference`；
  - 异常捕获收敛为 `JacksonException`。
- **`SyslogAspect.java`**：
  - 将 `ObjectMapper` 与行内声明的 `com.fasterxml.jackson.databind.JsonNode` 统一迁移为 `tools.jackson.databind.ObjectMapper` 和 `tools.jackson.databind.JsonNode`。

### 3.3 安全过滤器链与异常处理器全局迁移
- **`JwtAuthFilter.java`**、**`MaintenanceModeFilter.java`**、**`SecurityAuthenticationEntryPoint.java`**、**`SecurityAccessDeniedHandler.java`**：
  - 全部替换注入类型为 `tools.jackson.databind.ObjectMapper`；
  - 顺带按后端规范修复了 `SecurityAccessDeniedHandler` 的手动构造器为 Lombok `@RequiredArgsConstructor`。

### 3.4 VO 视图层雪花 ID 序列化注解升级
- 针对使用 `@JsonSerialize(using = ToStringSerializer.class)` 的所有 VO 类：
  - `AdminLogVO.java`
  - `BroadcastMessageVO.java`
  - `AdminClusterImageDetailVO.java`
  - `AdminContainerInstanceVO.java`
  - `AdminClusterNodeVO.java`
  - `AdminClusterImageVO.java`
- 将 `@JsonSerialize` 统一修改为 `tools.jackson.databind.annotation.JsonSerialize`，`ToStringSerializer` 统一修改为 `tools.jackson.databind.ser.std.ToStringSerializer`，确保与 Jackson 3 的核心序列化链无缝协作。

---

## 4. 验证结果与审计分析

1. **编译构建验证**：
   - 执行 `mvn compile`，平台全部 12 个模块均通过构建（耗时 ~9.5s）；
2. **测试用例验证**：
   - 执行 `mvn test`，全平台自动化测试用例全部通过（0 failures, 0 errors）；
3. **无缝兼容性**：
   - 杜绝了 Jackson 2 与 Jackson 3 混合使用导致的序列化断层风险，确保系统单数据源与框架统一性。
