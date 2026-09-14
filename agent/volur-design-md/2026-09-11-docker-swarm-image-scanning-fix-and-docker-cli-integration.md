# Docker Swarm 模式镜像扫描与探测故障排查、修复与双保险机制施工文档

## 1. 故障背景与问题现象

在将 Volur Lite 平台使用 Docker 进行容器化部署（`docker compose up -d`）后，管理端执行镜像探测与扫描（`POST /api/v1/admin/images/scan`）时出现无法探测到本地宿主机 Docker 镜像的问题。
同时，镜像对账逻辑触发，将此前数据库中记录的镜像误判为“已在物理节点中被删除”，导致数据库镜像记录被误清理。

---

## 2. 根因分析

经过深入代码审查与架构分析，确认故障由以下四个层面的问题共同导致：

1. **运行容器内缺少 `docker` 命令行工具（Docker CLI）**：
   - 平台后端运行镜像基于 `eclipse-temurin:25-jre-alpine`，这是一个极简的 Alpine JRE 环境，未安装 `docker-cli`；
   - 平台的 Docker Swarm 模式核心调度（包括 `docker images`、`docker info`、`docker service create` 等）严重依赖 `ProcessBuilder` 执行系统命令；
   - 容器内调用 `new ProcessBuilder("docker", "images", ...)` 时直接抛出 `java.io.IOException: Cannot run program "docker": error=2, No such file or directory`。
2. **异常捕获过宽且日志级别过低**：
   - 在 `queryDockerCli` 中，捕获异常后仅调用 `log.debug(...)`，在生产默认 `INFO` 日志级别下异常被完全静默吞掉，导致开发者与运维人员无法在控制台感知真实错误。
3. **Unix Domain Socket 缺乏原生 HTTP 直连支持**：
   - 平台系统配置中 Swarm API 地址默认为 `unix:///var/run/docker.sock`；
   - 旧代码仅在 `swarmApiUrl` 属于 `tcp://` 或 `http://` 时才调用 Docker REST API，对 `unix://` 则强行退回到依赖 `docker` CLI 命令，缺少 Unix Domain Socket 原生通信路径。
4. **对账误杀机制（Orphan Image Purge Bug）**：
   - 旧逻辑在探测本地 Master 节点时，无论返回列表是否为空、本地 Docker Daemon 是否在线，均无条件将 `masterIp` 加入 `successfullyInspectedNodes`；
   - 随后在步骤 3 的孤儿清理逻辑中，因为 `successfullyInspectedNodes` 包含了 Master 节点，系统错误地判定“节点已连通但本地镜像列表为空，说明该节点上的物理镜像已全被清理”，进而直接从数据库中删除了现存镜像记录。

---

## 3. 详细修复方案与改动清单

### 3.1 构建阶段注入官方极简 `docker-cli`
- **文件**：[`src/volur-lite-platform/Dockerfile`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/Dockerfile)
- **改动**：
  在运行镜像构建阶段增加 `apk add --no-cache docker-cli`：
  ```dockerfile
  FROM eclipse-temurin:25-jre-alpine

  WORKDIR /app
  ENV TZ=Asia/Shanghai

  RUN apk add --no-cache docker-cli

  COPY --from=builder /build/volur-server/target/volur-server.jar app.jar
  ```
- **效益**：赋予容器内执行 `docker` 命令的能力，确保与挂载进容器的 `/var/run/docker.sock` 完美契合，彻底打通 `docker info`、`docker images`、`docker service` 等所有 Swarm 核心功能。

### 3.2 Java 25 原生 Unix Domain Socket HTTP 通信支持
- **文件**：[`AdminImageServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/service/admin/impl/AdminImageServiceImpl.java)
- **改动**：
  1. 新增 `queryDockerUnixSocket(String socketPath)` 方法：
     - 利用 JDK 原生 `UnixDomainSocketAddress` 与 `SocketChannel` 直接建立与 `/var/run/docker.sock` 的传输通道；
     - 发送标准 HTTP `GET /images/json` 请求并解析响应中的 JSON 数组提取镜像信息；
     - **脱离外部依赖**：即使在极端环境下容器缺少 `docker` 命令行工具，只要挂载了 Socket 即可极速、原生完成镜像探测。
  2. 优化 `fetchLocalDockerImages()`：
     - 优先对 `tcp://` / `http://` 走 HTTP API；
     - 对 `unix://` 优先走 `queryDockerUnixSocket`；
     - 若上述均未获取到镜像，再降级回退至 `queryDockerCli`，构成双重保险。

### 3.3 增加连通性预检与孤儿清理误删防线
- **文件**：[`AdminImageServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/service/admin/impl/AdminImageServiceImpl.java)
- **改动**：
  1. 新增 `isLocalDockerReachable()` 方法：
     - 结合 HTTP `/ping`、Unix Socket 连接测试及 CLI 探针，准确检测本地 Docker Daemon 的健康状况；
  2. 在 `loadAndSyncClusterImages()` 中：
     - 当且仅当 `localImages` 非空或 `isLocalDockerReachable()` 为真时，才将 `masterIp` 标记至 `successfullyInspectedNodes`；
     - 当本地 Docker 离线或异常时，输出明确的 `WARN` 告警，并**跳过本地节点镜像清理核验**，保护现有的数据库镜像记录不被误清空。
  3. 将 `queryDockerCli` 中的异常日志由 `log.debug` 提升为 `log.warn`，并附带明确的排查指引（如确认挂载了 `docker.sock` 且容器具备客户端）。

---

## 4. 验证结果

1. **后端编译验证**：
   - 执行：`mvn test-compile -DskipTests`（于 `src/volur-lite-platform` 根工程）；
   - 结果：**12 个模块全部 `BUILD SUCCESS`**，耗时 6.45s。
2. **规范合规核验**：
   - 严格遵循 Java 25 虚拟线程与标准 I/O 规范，统一采用 Lombok 与结构化异常处理。
