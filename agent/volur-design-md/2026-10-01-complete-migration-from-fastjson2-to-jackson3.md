# Volur Lite 后端全量迁移至 Jackson 3 与彻底剔除 Fastjson2 架构重构

## 1. 变更背景与架构诱因

在 Volur Lite 原后端架构中，底层数据序列化与反序列化长期混用 **Alibaba Fastjson2** 与 **Jackson 3**（`tools.jackson.*`），由此引发了一系列架构缺陷、安全隐患与维护成本：

1. **双序列化引擎冲突与认知负担**：
   - Spring Boot 4.1.0 与 Spring Security 7.x 深度原生绑定 Jackson 3，Spring Web 视图层与 HTTP 控制器均由 Spring 容器管理的 `JsonMapper` 进行处理；
   - 内部缓存、WebSocket 消息队列、定时任务及部分业务服务（题目导入导出、队长转移记录等）却使用 Fastjson2 的静态工具类（`JSON.toJSONString` / `JSON.parseObject`）；
   - 实体类被迫同时引入两套注解（例如同时标注 `@JsonCreator` 与 `@JSONCreator`、`@JsonProperty` 与 `@JSONField`），造成样板代码冗余；
   - 两套引擎对雪花算法大整数（Snowflake ID `Long`）序列化规则不一致：Jackson 统一在 Spring 容器中注册了 Long 精度保护，而 Fastjson2 需要显式注入 `JSONWriter.Feature.WriteLongAsString`，极易因遗漏导致前端精度丢失截断。

2. **多级缓存与循环引用隐患**：
   - 历史版本中，多级缓存 L2 曾因 Fastjson2 不感知 Jackson 的引用关系与深度递归限制，在序列化包含双向绑定的实体时触发栈溢出异常（`com.alibaba.fastjson2.JSONException: level too large : 2049`）；
   - Fastjson2 在反射反序列化私有类或 JDK 不可变集合（如 `List.of()` 生成的 `ImmutableCollections$ListN`）时极易抛出反射构造异常。

3. **依赖纯粹性与安全合规性**：
   - 消除跨模块依赖混乱，全平台遵循单一真实数据源原则（Single Source of Truth），实现前后端 JSON 协议、缓存数据格式与 WebSocket 消息传输的全链路统一；
   - 彻底从所有模块 POM 与根 POM 中物理删除 Fastjson2，杜绝任何历史遗留类与代码包袱。

---

## 2. 核心架构设计与重构方案

### 2.1 基础工具层统合：`JsonUtils`
在 `volur-framework` 模块提供统一线程安全的轻量级工具门面 [`com.humolix.volur.framework.common.util.JsonUtils`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/util/JsonUtils.java)：
- 基于 Jackson 3 `tools.jackson.databind.json.JsonMapper` 构建单例实例；
- 注册 `JavaTimeModule`，并配置全局 Long/long 序列化为 String（防止前端 JS 53 位精度截断）；
- 配置 `DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES = false`，保证对向后兼容字段的容错反序列化；
- 暴露极简泛型 API：`toJson`、`toJsonPretty`、`fromJson`、`parseList`、`parseMap`、`readTree`、`createObjectNode`、`createArrayNode`，统一全平台无状态 JSON 处理。

### 2.2 多级缓存 L2 信封重构：`CachedValue` 与 `CacheEvictMessage`
- 重构 `com.humolix.volur.framework.cache.CachedValue`：
  - 去除 Fastjson2 静态方法，使用 Jackson 3 `JsonMapper` 进行信封序列化与反序列化；
  - 维持三字段精简安全信封结构 `{"n":false,"t":"...","v":"..."}`（`n`: 是否空值标记，`t`: 全限定类名，`v`: 载荷原始 JSON 字符串）；
  - 保留白名单校验机制（基础类型、集合包装、`java.time`、`com.humolix.volur.*`、`org.springframework.security.*`），彻底免疫反序列化 autoType 式 RCE 攻击；
  - 对 JDK 不可变集合（`List.of()`、`Map.of()`、`Set.of()`）及数组类型进行类型归一化。
- 重构 `com.humolix.volur.framework.cache.invalidation.CacheEvictMessage`：
  - 编解码全面切换至 Jackson 3 `JsonMapper`，保证 Redis 广播消息序列化规范统一。

### 2.3 状态机与安全认证：`volur-security` & `volur-auth`
- **`TransitionLockManager`**：
  - 注册流程状态锁 `RegistrationTransitionState` 读写全面迁移至 `ObjectMapper`；
- **`RecaptchaServiceImpl`**：
  - 移除 Fastjson2 依赖，直接复用已注入的 `ObjectMapper`，通过 `readTree` 树模型解析 Google reCAPTCHA 响应；
- **`UserDetails`**：
  - 移除冗余的 Fastjson2 `@JSONCreator` 与 `@JSONField` 注解，仅保留 Jackson 标准注解。

### 2.4 异步任务跟踪：`volur-system`
- **`AdminAsyncTaskServiceImpl`**：
  - 异步任务载荷（Payload）与执行结果（Result）在写入 Redis 临时缓存时，由构造器注入的 `ObjectMapper` 进行强类型序列化，取代原先的 `JSON.toJSONString`。

### 2.5 实时通知与 WebSocket 广播：`volur-notification` & `volur-competition`
- **`volur-notification`**：
  - `NotificationEventListener`、`RealtimePushServiceImpl`、`RealtimeRedisSubscriber` 统一采用 `JsonUtils.toJson` 与 `JsonUtils.fromJson` 传输实时事件 payload 与用户通知。
- **`volur-competition`**：
  - `CompetitionChatWebSocketHandler`、`CompetitionChatRedisPublisher`、`CompetitionChatRedisSubscriber` 统一采用 Jackson 3 进行聊天消息帧的编解码；
  - `AdminTeamServiceImpl` 与 `CompetitionTeamServiceImpl` 中涉及的附件与队长变更流水序列化切换至 `JsonUtils`。

### 2.6 赛题管理与包导入导出：`volur-challenge`
- **`ChallengeMapper`**：
  - 将 MapStruct 字符串与附件列表转换函数切换为 `JsonUtils.parseList`；
- **`AdminCtfChallengeServiceImpl`**：
  - 附件列表与创建/更新入参序列化统合为 `JsonUtils.toJson`；
  - CTF 赛题 ZIP 导入：使用 Jackson 3 `readTree` 解析 `challenge.json` / `build.json`，通过 `JsonNode` 解析动态 Flag、容器配置与难度枚举；
  - CTF 赛题 ZIP 导出：使用 Jackson 3 `ObjectNode` 与 `ArrayNode` 组装题目规范元数据，通过 `writerWithDefaultPrettyPrinter().writeValueAsBytes(root)` 输出美化格式 JSON 题目包。
- **`AdminTheoryChallengeServiceImpl`**：
  - 理论赛题批量导入：使用 Jackson 3 `readTree` 兼容数组根节点与 `{ "questions": [...] }` 对象包装格式，通过 `JsonNode` 提取题目属性；
  - 理论赛题导出：组装 `ArrayNode` / `ObjectNode` 并通过 Jackson 3 漂亮格式化字节流直接写入 HTTP 响应。

---

## 3. 依赖清理与物理瘦身

1. **子模块 POM 清理**：
   从以下所有子模块的 `pom.xml` 中彻底删除 `com.alibaba.fastjson2:fastjson2` 依赖：
   - `volur-framework`
   - `volur-auth`
   - `volur-system`
   - `volur-security`
   - `volur-notification`
   - `volur-competition`
   - `volur-challenge`
2. **父级 POM 统一收敛**：
   从根目录 `src/volur-lite-platform/pom.xml` 中彻底移除：
   - `<fastjson2.version>2.0.56</fastjson2.version>` 版本属性；
   - `<dependencyManagement>` 中的 `com.alibaba.fastjson2:fastjson2` 依赖定义。
3. **全局检索验证**：
   全工程扫描 `git grep -i "fastjson"`，除本历史设计文档溯源外，源码工程所有 Java 类、XML 配置文件及依赖配置中 Fastjson2 引用彻底清零（0 匹配）。

---

## 4. 规范遵守与安全考量

1. **不可变依赖注入规范**：
   所有服务实现类（`AdminCtfChallengeServiceImpl`、`AdminTheoryChallengeServiceImpl`、`AdminAsyncTaskServiceImpl`、`TransitionLockManager` 等）中新注入的 `ObjectMapper` 均严格遵循 `private final` 修饰符，并通过 Lombok `@RequiredArgsConstructor` 实现依赖注入，杜绝字段注入与手写构造器反模式。
2. **零注释铁律**：
   所有新增与修改的 Java 代码中未残留任何解释性行内注释与设计说明，所有逻辑背景统一外置归档于本方案文档中。
3. **彻底重构与拒绝兼容别名**：
   无任何保留的“兼容别名”（如静态代理类或弃用标记），直接完成编译期强类型校验。
4. **Fast-Fail 原则**：
   在 JSON 解析异常与非法格式时，统一通过标准 `BusinessException` 快速失败阻断，绝不隐式降级或伪造数据兜底。
