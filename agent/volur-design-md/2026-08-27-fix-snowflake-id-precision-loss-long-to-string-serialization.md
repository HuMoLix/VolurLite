# 修复雪花算法 Long ID 在前端精度丢失问题 (Long to String 序列化加固)

## 1. 变更背景与问题根因

在 Web 前端开发中，JavaScript `Number` 遵循 IEEE 754 双精度浮点数标准，其最大安全整数值为 `Number.MAX_SAFE_INTEGER` 即 $2^{53} - 1 = 9007199254740991$（16 位有效数字）。
而后端采用 64 位雪花算法（Snowflake ID）生成的数据库主键为 `Long` 整数类型（通常为 18~19 位数字，如 `351000953698123783`）。

当后端将实体以原始数字形态（Raw Number）返回给客户端时，浏览器的 `JSON.parse` 会在反序列化期间发生精度截断与四舍五入（例如 `351000953698123783` 被解析为 `351000953698123800`）。这导致管理端在执行以下操作时携带了错误的篡改 ID：
`PATCH /api/v1/admin/users/351000953698123800/status/active?status=false`
导致服务端返回 `404 Not Found (用户不存在)`。

---

## 2. 核心修复与加固方案

### 2.1 全局 Jackson 转换器与 Builder 定制
- **修改文件**：`src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/config/JacksonConfig.java`
- **改动点**：
  1. 注册 `Jackson2ObjectMapperBuilder` Bean，全局为 `Long.class`、`Long.TYPE`（基本类型 `long`）以及 `BigInteger.class` 配置 `ToStringSerializer.instance`；
  2. 注册 `@Primary ObjectMapper` 与 `MappingJackson2HttpMessageConverter`，统一禁用日期时间戳、空 Bean 报错与未知属性校验；
  3. 新增单元测试 `JacksonConfigTest`，自动化断言验证 18 位长整型雪花 ID 及大整数序列化输出必须为带有双引号的字符串格式。

### 2.2 Spring MVC 消息转换器队列优先级对齐
- **修改文件**：`src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/config/WebMvcConfig.java`
- **改动点**：
  - 在 `WebMvcConfigurer.extendMessageConverters` 中移除默认转换器，将绑有定制 `ObjectMapper` 的 `MappingJackson2HttpMessageConverter` 强制置顶插入至转换器链首位（Index 0），确保所有 REST 控制器响应体 100% 走此转换器输出。

### 2.3 领域模型 VO 字段显式序列化双重保护
- **修改文件**：
  - `AdminUserDetailsVO.java`
  - `UserDetailsVO.java`
  - `UserProfileResponseVO.java`
  - `UserSummaryVO.java`
- **改动点**：
  - 在 `id` 属性上添加 `@JsonSerialize(using = ToStringSerializer.class)` 注解，建立局部与全局双重防御。

---

## 3. 验证与质量测试

1. **自动化单元测试**：
   - 运行 `JacksonConfigTest`：针对 `351000953698123783L`、`350983824294805508L` 等雪花 ID 序列化验证，生成 JSON 准确包含 `"id":"351000953698123783"`；
   - 运行平台全模块单元测试：`mvn test` 全部通过（0 Errors, 0 Failures）。
2. **前端类型校验**：
   - 运行 `npx vue-tsc --noEmit`：0 告警，前端 `AdminUserDetailsVO.id` 类型为 `string | number`，完全兼容字符串型雪花 ID。

---

## 4. 涉及文件清单

- **新增**：
  - `src/volur-lite-platform/volur-framework/src/test/java/com/humolix/volur/framework/common/config/JacksonConfigTest.java`
  - `agent/volur-design-md/2026-08-27-fix-snowflake-id-precision-loss-long-to-string-serialization.md`
- **修改**：
  - `src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/config/JacksonConfig.java`
  - `src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/config/WebMvcConfig.java`
  - `src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/domain/vo/AdminUserDetailsVO.java`
  - `src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/domain/vo/UserDetailsVO.java`
  - `src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/domain/vo/UserProfileResponseVO.java`
  - `src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/domain/vo/UserSummaryVO.java`
