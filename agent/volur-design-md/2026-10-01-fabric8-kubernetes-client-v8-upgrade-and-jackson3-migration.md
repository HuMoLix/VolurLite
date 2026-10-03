# Fabric8 Kubernetes Client 8.0.0 升级与 Jackson 3 迁移审计报告

## 1. 升级背景与目标

针对系统中 Jackson 2 与 Jackson 3 的双轨依赖排查，验证将 `io.fabric8:kubernetes-client` 从 `7.8.0` 升级至 `8.0.0`，核查其是否已经完成向 Jackson 3 (`tools.jackson.*`) 的迁移，并清理平台内部对 Jackson 2 的间接传递依赖。

---

## 2. 升级版本与依赖树对比

### 2.1 依赖版本定义
在根 POM [`pom.xml`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/pom.xml) 中升级属性定义：
```xml
<fabric8-kubernetes.version>8.0.0</fabric8-kubernetes.version>
```

### 2.2 升级前后 Jackson 依赖对比

#### 升级前 (7.8.0) 依赖树
```
\- io.fabric8:kubernetes-client:jar:7.8.0:compile
   \- io.fabric8:kubernetes-client-api:jar:7.8.0:compile
      +- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:jar:2.21.4:compile
      +- com.fasterxml.jackson.datatype:jackson-datatype-jsr310:jar:2.21.4:compile
      \- com.fasterxml.jackson.core:jackson-core:jar:2.21.4:compile
```

#### 升级后 (8.0.0) 依赖树
```
\- io.fabric8:kubernetes-client:jar:8.0.0:compile
   \- io.fabric8:kubernetes-client-api:jar:8.0.0:compile
      +- tools.jackson.dataformat:jackson-dataformat-yaml:jar:3.1.4:compile
      +- tools.jackson.core:jackson-databind:jar:3.1.4:compile
      \- tools.jackson.core:jackson-core:jar:3.1.4:compile
```

### 2.3 结论
**Fabric8 Kubernetes Client 8.0.0 已全面迁移至 Jackson 3**，自身及其全部子模块（`kubernetes-model-*`、`kubernetes-httpclient-vertx-5` 等）已经**彻底移除了对 Jackson 2 (`com.fasterxml.jackson.*`) 的任何引用**。

---

## 3. 全系统剩余 Jackson 2 依赖全景清查

在 Fabric8 彻底完成 Jackson 3 迁移后，全平台（13 个模块）中剩余引入 `com.fasterxml.jackson.*` 的组件仅剩：
1. **JJWT 核心库**：`io.jsonwebtoken:jjwt-jackson:0.13.0` -> `com.fasterxml.jackson.core:jackson-databind:2.21.4`；
2. **Swagger/OpenAPI 规范引擎**：`org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.0` -> `io.swagger.core.v3:swagger-core-jakarta:2.2.52` -> `com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.21.4`。

---

## 4. 平台兼容性验证结果

1. **编译构建**：执行 `mvn test-compile`，全平台 13 个子模块均无报错，成功构建；
2. **单元测试**：执行 `mvn test -Dsurefire.failIfNoSpecifiedTests=false`，全平台所有单测用例（包括 `volur-container` 动态容器分配、Flag 生成等）100% 通过（0 失败，0 错误）。
