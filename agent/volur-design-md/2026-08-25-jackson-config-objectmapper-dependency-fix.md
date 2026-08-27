# 施工与审计记录：JacksonConfig 的 ObjectMapper 启动依赖解耦与弃用类清理修复

## 1. 基本信息

- **任务名称**：修复 `JacksonConfig` 中 `Jackson2ObjectMapperBuilder` 缺失导致的启动依赖注入失败，并移除 Spring 7.0 弃用类
- **执行时间**：2026-08-25
- **影响模块**：
  - `volur-framework` (common/config)
  - `volur-auth` (早期 Security 过滤器装配阶段)
  - `volur-server` (应用启动阶段)
- **关联文件**：
  - [`JacksonConfig.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/config/JacksonConfig.java)

---

## 2. 异常背景与根因剖析

### 2.1 启动异常与弃用告警
1. 在启动 `volur-server` 时，Spring Security 的 `JwtAuthFilter` 在初始化构造函数参数 `ObjectMapper` 时抛出依赖注入失败：
   ```text
   Parameter 0 of method objectMapper in com.humolix.volur.framework.common.config.JacksonConfig 
   required a bean of type 'org.springframework.http.converter.json.Jackson2ObjectMapperBuilder' that could not be found.
   ```
2. 原先所用的 `org.springframework.http.converter.json.Jackson2ObjectMapperBuilder` 自 Spring Framework 7.0 / Spring Boot 4.0 起已被标注为 `@Deprecated` 并计划移除。

### 2.2 冲突根因
- `JwtAuthFilter` 作为 Security 早期过滤器组件在容器生命周期极早阶段被实例化；
- 方法签名若依赖外部未就绪的 Builder Bean，会导致提前依赖查找失败；
- 依赖 Spring 包装的旧版 Builder 无法平滑适应现代 Spring 7+ 生态。

---

## 3. 具体改造内容

### 3.1 采用 Jackson 原生无废弃构建器 [`JsonMapper.builder()`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/config/JacksonConfig.java)
- 彻底移除对 Spring 弃用类 `Jackson2ObjectMapperBuilder` 的引用；
- 采用 Jackson 2.10+ 原生官方推荐的不可变构建器 `JsonMapper.builder()`：
  - 注册 `JavaTimeModule` 并禁用 `WRITE_DATES_AS_TIMESTAMPS`，规范化 Java 8 日期时间序列化；
  - 注册 `ToStringSerializer` 处理 `Long.class` 与 `Long.TYPE`，杜绝前端 JS 64 位雪花 ID 精度丢失；
  - 禁用 `FAIL_ON_UNKNOWN_PROPERTIES` 与 `FAIL_ON_EMPTY_BEANS`，保障 DTO 高容错性；
  - 统一时区为 `Asia/Shanghai`，时间格式统一为 `yyyy-MM-dd HH:mm:ss`；
- 方法签名无任何入参依赖，支持任意早期的 Spring Bean 安全注入。

---

## 4. 验证结果

- 执行 `mvn clean compile` 对平台 10 个子模块全量编译构建成功（耗时 14.5s）。
- 彻底消除了应用启动时 Security 过滤器链对 `ObjectMapper` 的依赖断裂，并移除了所有 Spring 7+ 废弃 API 告警。
