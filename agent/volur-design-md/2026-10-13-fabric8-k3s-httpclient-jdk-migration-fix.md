# Fabric8 Kubernetes/K3s 客户端 HTTP SPI 迁移至 JDK 原生实现施工记录

## 1. 变更背景与故障根因

在通过 K3s 集群调度器执行连接探测（`K3sRuntimeStrategy.getClient` / `testConnection`）时，系统抛出未捕获的严重类加载异常：
```text
Caused by: java.lang.ExceptionInInitializerError: Exception java.lang.NoSuchFieldError: Class io.netty.handler.codec.http.HttpHeaderNames does not have member field 'io.netty.util.AsciiString ACCEPT_QUERY'
        at io.vertx.core.http.HttpHeaders.<clinit>(HttpHeaders.java:78)
        at io.vertx.core.http.HttpClientOptions.<clinit>(HttpClientOptions.java:182)
        at io.fabric8.kubernetes.client.vertx5.Vertx5HttpClientBuilder.build(Vertx5HttpClientBuilder.java:207)
Caused by: java.lang.NoClassDefFoundError: Could not initialize class io.vertx.ext.web.client.WebClientOptions
```

### 1.1 根因分析
1. **Netty 与 Vert.x 5 二进制接口不兼容**：
   - Fabric8 Kubernetes Client 8.0.0 默认引入了 `kubernetes-httpclient-vertx-5` 传输层，其依赖 `vertx-core:5.2.0`；
   - `vertx-core:5.2.0` 依赖 Netty 4.1.x 中的 `HttpHeaderNames.ACCEPT_QUERY` 静态常量；
   - 当前微服务工程（基于 Spring Boot 4.1 与 Lettuce Redis 客户端）运行时强制引入了 Netty 4.2 (`4.2.15.Final`)；
   - Netty 4.2 中无 `ACCEPT_QUERY` 属性，导致 JVM 在执行 `io.vertx.core.http.HttpHeaders.<clinit>` 类静态初始化时抛出 `NoSuchFieldError`，使 `WebClientOptions` 被 JVM 判定为不可初始化，进而级联引发 `NoClassDefFoundError`。

---

## 2. 施工改动详述

### 2.1 依赖架构重构：全面切换至 `kubernetes-httpclient-jdk`
Fabric8 Kubernetes Client 设计了可拔插的 HTTP Client SPI 机制。Java 25 原生自带高性能 `java.net.http.HttpClient`，无需依赖任何第三方 Vert.x 或 Netty 运行时，彻底杜绝类库版本冲突。

1. **父工程 POM (`src/volur-lite-platform/pom.xml`)**：
   - 在 `dependencyManagement` 中，为 `io.fabric8:kubernetes-client` 显式排除 `kubernetes-httpclient-vertx-5`；
   - 引入原生 JDK 实现模块 `io.fabric8:kubernetes-httpclient-jdk:${fabric8-kubernetes.version}`。
2. **容器模块 POM (`src/volur-lite-platform/volur-container/pom.xml`)**：
   - 引入 `io.fabric8:kubernetes-httpclient-jdk` 依赖。
3. **主聚合服务 POM (`src/volur-lite-platform/volur-server/pom.xml`)**：
   - 显式打包 `io.fabric8:kubernetes-httpclient-jdk`，确保构建出的 Spring Boot Fat JAR 运行时包含 JDK SPI 提供者。
4. **单元测试验证 (`K3sClientFactoryTest.java`)**：
   - 新增 `K3sClientFactoryTest`，断言 `KubernetesClientBuilder` 能通过 JDK HttpClient 洁净初始化并安全关闭。

---

## 3. 效益与影响范围

1. **零冲突、轻量化**：剔除了整套冗余的 Vert.x 运行时依赖，完全复用 JDK 25 原生异步 HTTP 客户端，内存占用更低且杜绝 Netty 冲突；
2. **影响范围**：
   - 模块：`volur-container`、`volur-server`；
   - 外部依赖树：彻底移除 `kubernetes-httpclient-vertx-5` 与 `io.vertx:*`。
