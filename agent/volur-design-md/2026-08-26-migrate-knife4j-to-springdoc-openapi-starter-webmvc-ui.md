# Volur Lite 迁移 Knife4j 至 SpringDoc 原生 UI (3.1.0) 归档

- **执行日期**：2026-08-26
- **施工模块**：`volur-framework`, `volur-server`, `pom.xml` (根工程)
- **文档类型**：API 文档与 Swagger UI 升级施工档案

---

## 1. 变更背景

为了进一步提升系统对 Spring Boot 3.x / Java 25 现代特性的兼容性与原生支持，移除原 Knife4j 4.5.0 依赖，统一升级为 SpringDoc 官方原生 UI Starter (`org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.0`)。

---

## 2. 详细改动清单

### 2.1 POM 依赖变更
1. **根工程 `pom.xml`**：
   - 属性更新：`<springdoc.version>3.1.0</springdoc.version>`（替换 `<knife4j.version>`）；
   - `<dependencyManagement>` 注册：
     ```xml
     <!-- SpringDoc OpenAPI WebMvc UI -->
     <dependency>
         <groupId>org.springdoc</groupId>
         <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
         <version>${springdoc.version}</version>
     </dependency>
     ```
2. **`volur-framework/pom.xml`**：
   - 将 `knife4j-openapi3-jakarta-spring-boot-starter` 替换为 `springdoc-openapi-starter-webmvc-ui`。

---

### 2.2 配置类与元数据设计 (`volur-framework`)
- **新增 `com.humolix.volur.framework.common.config.OpenApiConfig`**：
  - 声明 `OpenAPI` Bean，配置平台 API 元信息（标题：`Volur Lite CTF Platform API`，版本：`v1.0.0`，License：`Apache 2.0`）；
  - 配置全局 JWT Bearer 认证方案（`SecurityScheme.Type.HTTP`, `scheme("bearer")`, `bearerFormat("JWT")`），方便开发者在 Swagger UI 界面直接携带 Token 调试受保护接口。

---

### 2.3 配置文件与启动 Banner 更新 (`volur-server`)
1. **`application.yml`**：
   - 移除 `knife4j:` 配置块；
   - 增加 SpringDoc 标准配置：
     ```yaml
     springdoc:
       api-docs:
         enabled: true
         path: /v3/api-docs
       swagger-ui:
         enabled: true
         path: /swagger-ui.html
         tags-sorter: alpha
         operations-sorter: alpha
         doc-expansion: none
       show-actuator: false
     ```
2. **`VolurLiteApplication.java`**：
   - 更新启动控制台 Banner，输出 Swagger UI (`/swagger-ui/index.html` 或 `/swagger-ui.html`) 与 OpenAPI Spec (`/v3/api-docs`) 访问地址。

---

## 3. 验证结果

执行 Maven 整体编译验证：
```bash
mvn clean compile -DskipTests
```
全平台 12 个子模块全部构建成功（`BUILD SUCCESS`）。
