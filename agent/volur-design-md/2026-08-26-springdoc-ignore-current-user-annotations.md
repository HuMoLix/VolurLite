# Volur Lite OpenAPI 全局忽略自定义身份注解参数配置归档

- **执行日期**：2026-08-26
- **施工模块**：`volur-framework`
- **文档类型**：API 文档与 Swagger 参数解析优化

---

## 1. 变更背景与问题描述

在 Spring MVC 架构中，`@CurrentUserId` 和 `@CurrentUser` 注解配合 `HandlerMethodArgumentResolver`，用于从 Spring Security 上下文（JWT 凭证）中自动解析当前登录用户的 `userId` 或 `UserIdentity`。

然而，SpringDoc OpenAPI 默认无法自动感知 Controller 方法入参中的自定义注入参数，会将其回退并识别为普通的 HTTP Query 请求参数（`in: query`），且因普通类型入参默认非空而标记为必填（`required: true`）。这导致 Swagger-UI 在测试接口（如 `/api/v1/users/me`）时强行要求调用者手动输入 `userId`，造成与实际鉴权机制脱节并产生安全认知误导。

---

## 2. 改造方案与落地详情

### 2.1 方案选型
采用 **方案 1：全局无侵入忽略配置**。
通过 `org.springdoc.core.utils.SpringDocUtils` 在 OpenAPI 配置初始化时将 `@CurrentUserId` 与 `@CurrentUser` 纳入忽略注解清单。该方案无需修改任何业务 Controller 代码或侵入注解定义，全局即时生效。

### 2.2 修改文件
- [`OpenApiConfig.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/config/OpenApiConfig.java)

### 2.3 核心实现代码
```java
@Configuration
public class OpenApiConfig {

    static {
        // 忽略自定义身份解析注解修饰的方法入参，避免在 Swagger/OpenAPI 文档中暴露为必填请求参数
        SpringDocUtils.getConfig().addAnnotationsToIgnore(
                CurrentUserId.class,
                CurrentUser.class
        );
    }

    // ...
}
```

---

## 3. 影响范围与验证结果

1. **影响范围**：
   - 全平台所有使用 `@CurrentUserId Long userId` 或 `@CurrentUser UserIdentity user` 作为入参的 Controller 接口（涵盖 `volur-user`、`volur-challenge`、`volur-competition`、`volur-notification`、`volur-container`、`volur-scoreboard` 等模块）。
2. **效果**：
   - Swagger-UI / OpenAPI 文档中不再生成多余的 `userId` Query 参数。
   - 接口调用仅依赖 Header 中的 `Authorization: Bearer <token>` 鉴权，文档与实际代码行为完全一致。
3. **编译与构建验证**：
   - 全平台 12 个模块执行 `mvn clean test-compile` 全部构建通过（BUILD SUCCESS）。
