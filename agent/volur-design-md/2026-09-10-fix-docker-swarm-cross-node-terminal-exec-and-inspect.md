# 修复说明：Docker Swarm 跨节点工作子节点容器虚拟终端与命令执行连接失败

## 1. 变更背景与问题现象

### 1.1 问题现象
在平台中将容器成功调度到工作子节点（Kali Linux，IP: `192.168.0.124`）运行后，子节点执行 `docker ps -a` 已能看到容器处于 `Up` 运行状态：
```text
CONTAINER ID   IMAGE                                         COMMAND      STATUS          PORTS     NAMES
afa6d09e405c   ghcr.io/humolix/wzbc2025_web_babyweb:latest   "/flag.sh"   Up 19 seconds   80/tcp    swarm-volur-admin-u...
```
但在平台的“管理端容器管理”或“赛题虚拟终端”中连接该容器时：
- 虚拟终端顶部提示：`[Connecting to container ... (DOCKER_SWARM)]... [Docker Swarm 提示] 容器实例尚未就绪 (Task State: Running 33 seconds ago)`
- 输入命令（如 `ls`）返回：`[Docker Swarm 提示] 容器实例尚未处于运行就绪状态 (Task State: Running 59 seconds ago)。exit: 1`
- 导致无法连接与交互。

---

## 2. 根因深度剖析

1. **Docker Swarm 架构与本地容器视图隔离**：
   Docker Swarm 是分布式编排架构。当一个服务（Service）的任务调度到工作子节点（Worker Node）运行时：
   - 真实的容器实例仅在**工作节点的 Docker 引擎**中创建并运行；
   - **主节点（Manager Node）本地 Docker 守护进程的 `docker ps` 中不存在该容器**。
2. **原终端交互与探测逻辑仅在主节点本地执行**：
   在 `DockerSwarmRuntimeStrategy.java` 中：
   - `findSwarmTaskContainerId` 原先使用 `docker ps --filter label=com.docker.swarm.service.name=...`，未指定节点 `-H`，始终在主节点本地执行；
   - 主节点本地执行 `docker ps` 返回空结果，导致 `actualContainerId == null`；
   - `getTerminalInfo` 与 `execCommand` 判定 `actualContainerId == null`，随后调用 `getServiceTaskStatus`；
   - `getServiceTaskStatus` 从 Swarm Manager 查出该 Task 状态为 `Running 59 seconds ago`，但因无法在主节点本地找到容器，误判为“容器尚未启动就绪”，退化并抛出提示拦截。
   - `execCommand` 原先执行 `docker exec` 同样使用本地 `swarmApiUrl`，未定向到实际运行容器的 Worker 节点。

---

## 3. 修复方案与改动细节

### 核心解法：基于 Swarm Task 智能寻址与 TLS 远程定向 `exec`
1. **构建跨节点容器信息模型 (`SwarmTaskContainerInfo`)**：
   封装 `(containerId, nodeIp, clusterNode)` 三元组，统一标识分布式容器的真实宿主节点与连接凭证。
2. **基于 Swarm Manager 拓扑的精准两步溯源 (`findSwarmTaskContainerInfo`)**：
   - 第一步：通过 Swarm Manager 查询该服务当前运行中的 Task ID：
     `docker service ps <serviceName> --filter desired-state=running --no-trunc --format "{{.ID}}"`
   - 第二步：inspect 该 Task 提取其运行所在的 Swarm NodeID 与底层的物理 ContainerID：
     `docker inspect <taskId> --format "{{.NodeID}}:::{{.Status.ContainerStatus.ContainerID}}"`
   - 第三步：通过 `findClusterNodeBySwarmNodeId` 匹配数据库中对应的 `ClusterNode`（按 `clusterNodeId` / `nodeName` / `ipAddress` 智能匹配）；
     - 若为 Worker 节点：解析为对应的子节点 IP（如 `192.168.0.124`）及其 TLS 认证配置；
     - 若为主节点：解析为 `127.0.0.1`。
3. **安全双向认证 CLI 管道构建 (`buildDockerCliCommand`)**：
   - 利用 `DockerTlsUtil.buildDockerBaseCliCommand`，根据目标节点的 TLS 证书（`ca.pem`, `cert.pem`, `key.pem`）自动生成携带 `--tlsverify -H tcp://<worker-ip>:2376` 的安全通信命令行。
4. **定向远程 Inspect 与 Exec**：
   - `inspectContainerConfig`：定向至目标节点查询该容器的默认 `User`、`WorkingDir` 与 `Hostname`；
   - `execCommand`：直接通过 TLS 管道在目标节点执行 `docker exec -i -w <wd> <containerId> <shell> -c <cmd>`；
   - `getTerminalInfo`：准确获取运行态 `shortId`、工作目录与提示符（如 `root@afa6d09e405c:/var/www/html#`），状态标记为 `ready: true`。

---

## 4. 验证与影响范围

1. **子节点真实容器内命令执行验证**：
   使用目标节点 TLS 管道对 Kali 运行中的容器直接执行 `docker exec -i <containerId> /bin/sh -c "ls -la /var/www/html"`：
   ```text
   total 40
   drwxr-xr-x    1 www-data www-data      4096 Oct  8  2025 .
   -rwxr-xr-x    1 www-data www-data      4286 Oct 24  2024 favicon.ico
   -rwxr-xr-x    1 www-data www-data      3460 Sep 29  2025 index.html
   drwxr-xr-x    1 www-data www-data      4096 Sep  9 16:29 js
   ...
   ```
   毫秒级响应并成功返回真实靶机目录文件。
2. **编译构建**：
   执行 `mvn clean compile -f src/volur-lite-platform/pom.xml -pl volur-system,volur-container -am` 构建成功（`BUILD SUCCESS`）。
