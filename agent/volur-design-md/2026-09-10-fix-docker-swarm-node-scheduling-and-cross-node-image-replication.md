# 全链路排查与修复：Docker Swarm 子节点容器无法调度与物理镜像复制同步异常

## 1. 变更背景与问题现象

### 1.1 业务场景
在平台集群管理模式下，管理员向工作子节点（IP: `192.168.0.124`，Kali Linux）调度下发自定义容器或动态靶机容器（例如 `ghcr.io/humolix/wzbc2025_web_babyweb:latest`）。
前端平台界面显示容器实例已创建并处于 `RUNNING` 状态，但登录子节点 `192.168.0.124` 检查时发现：
1. 子节点执行 `docker ps -a` 无任何对应容器运行；
2. 子节点执行 `docker images` 未拉取或同步该镜像；
3. 主节点 Docker Swarm 未真实执行服务创建或调度至子节点。

---

## 2. 全链路核心问题溯源与根因分析

经过全链路深度排查（前端调度请求 -> 后端分发 -> 镜像预检与复制 -> Swarm Manager 服务创建 -> Swarm Worker 调度执行），精准定位出以下三个核心根因：

### 根因 1：Docker Desktop 环境下 Swarm Manager 广播地址不匹配与工作节点失联 (`heartbeat failure`)
- **根因细节**：
  macOS Docker Desktop 运行于虚拟机内部（`eth0: 192.168.65.3`, `eth1: 192.168.64.2`）。在初始执行 `docker swarm init` 时，Manager 默认将私有内网 IP `192.168.65.3:2377` 作为集群对外广播地址（`Leader.Addr`）。
  子节点通过物理局域网连接宿主机 `192.168.0.104:2377`（由 `socat` 反向代理转至 `192.168.64.2:2377`）成功执行握手入网后，Swarm Manager 向子节点下发了其内部 Manager 拓扑列表 `192.168.65.3:2377`。
  子节点无法直接路由到该虚拟机私网 IP，导致后续 gRPC 心跳和 Gossip 通信超时，Swarm 将节点标记为 `Down`（`heartbeat failure for node in "unknown" state`）。
- **修复措施**：
  重新初始化 Docker Desktop Swarm 集群，显式指定集群公网/局域网广播地址：
  ```bash
  docker swarm init --advertise-addr 192.168.0.104:2377 --listen-addr 192.168.64.2:2377
  ```
  同时子节点加入时指定其局域网广播地址：
  ```bash
  docker swarm join --token <TOKEN> 192.168.0.104:2377 --advertise-addr 192.168.0.124
  ```
  节点状态恢复为稳定且持久的 `Ready`，Swarm 服务可正常在子节点创建并运行。

---

### 根因 2：`resolveSwarmNodeConstraint` 调度节点约束匹配缺陷与异常被吞并降级 (`Silent Simulation`)
- **根因细节**：
  1. 在 `DockerSwarmRuntimeStrategy.java` 中，原 `resolveSwarmNodeConstraint` 仅通过 `cleanIp.equalsIgnoreCase(nodeAddr)` 比较目标 IP 与 `docker node inspect {{.Status.Addr}}`。由于反向代理与 NAT 桥接，Swarm Manager 探测到的连接地址为网桥网关（如 `192.168.64.1`），而非子节点物理 IP `192.168.0.124`，且主机名为 `kali`（与系统记录的节点名 `volur-worker-02` 不同）。导致匹配失败抛出异常。
  2. 在 `startChallengeContainer` 与 `startManualContainer` 中，使用了 `catch (Exception e) { log.warn(...); }` 吞并了异常，并静默降级返回伪造的 `InstanceStartupStatus.RUNNING`，使得实际上未执行 `docker service create`，但前端和数据库误以为容器已成功启动。
- **修复措施**：
  1. 重构 `resolveSwarmNodeConstraint`：优先根据目标 IP 从 `ClusterNode` 实体中获取记录的 `clusterNodeId` 与 `nodeName`，多维度精准比对（`nodeId`、`expectedClusterNodeId`、`hostname`、`nodeAddr`），命中后直接生成绝对绑定的 Swarm 约束：`node.id == <nodeId>`。
  2. 移除静默吞并与伪造运行降级，严谨校验 `process.exitValue()` 并抛出包含真实错误原因的 `BusinessException`。

---

### 根因 3：`ensureImageOnNode` 盲目信任数据库缓存，跳过物理镜像同步
- **根因细节**：
  在 `AdminImageServiceImpl.java` 的 `ensureImageOnNode` 方法中，原有逻辑先判断 `cluster_image` 数据库表中 `nodeIps` 是否包含 `targetIp`。若包含则直接 `return`。
  由于历史数据更新或 IP 变更，数据库缓存的 `nodeIps` 与子节点 Docker 引擎实际存在的镜像严重脱节。一旦数据库存在目标 IP，便跳过了对子节点物理镜像的检测与跨节点流式传输（`replicateImageToNode`），导致物理镜像从未被加载到子节点。
- **修复措施**：
  重构 `ensureImageOnNode` 执行逻辑，建立**物理真实状态优先 + 自动纠偏自愈**机制：
  1. 首先调用 `queryDockerInspect(targetIp, imageName)` 通过 Docker TLS 双向认证实时探测子节点是否物理存在该镜像；
  2. 若物理存在，自动补全数据库元数据并返回；
  3. 若物理缺失，即使数据库记录包含该 IP，也主动移除脏数据；随后寻找物理拥有该镜像的源节点（如 Master `127.0.0.1`），通过 TLS 流式管道执行 `replicateImageToNode`（`docker save` | `docker load`）传输镜像，成功后同步数据库。

---

## 3. 详细改动清单

### 后端代码改动
1. **`src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/strategy/DockerSwarmRuntimeStrategy.java`**：
   - 完善 `resolveSwarmNodeConstraint`：注入 `clusterNodeRepository` 针对目标节点 `clusterNodeId` 与 `nodeName` 进行精确匹配，支持桥接 NAT 代理场景下的 Swarm 节点定位；
   - 重构 `startChallengeContainer` 与 `startManualContainer`：杜绝伪造 RUNNING 状态，严格捕获并上抛异常与命令行退出码。
2. **`src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/service/admin/impl/AdminImageServiceImpl.java`**：
   - 重构 `ensureImageOnNode`：调整检查顺序，以子节点 Docker 引擎物理 inspect 为准，物理缺失时自动调用 TLS 流式复制并自愈更新数据库。

### 集群与环境修复
1. **Docker Swarm Leader 广播配置**：
   - 主机重新以 `--advertise-addr 192.168.0.104:2377 --listen-addr 192.168.64.2:2377` 初始化集群；
   - 子节点以 `--advertise-addr 192.168.0.124` 重新入网，NodeID 更新为 `gf48p9x5of4xcqhnpehpoc12k`，状态保持健康 `Ready`；
2. **数据库集群元数据同步**：
   - 更新 `cluster_node` 表中主从节点最新 `cluster_node_id` 与 `in_cluster = 1`；
   - 清理历史无效的模拟容器实例记录。

---

## 4. 验证结果

1. **Swarm 集群健康状态验证**：
   - 执行 `docker node ls`：
     - `docker-desktop` (Leader, Ready)
     - `kali` (Active, Ready, NodeID: `gf48p9x5of4xcqhnpehpoc12k`)
   - 子节点 `docker info` 明确显示 `Manager Addresses: 192.168.0.104:2377`，心跳与 Raft 会话稳定无断流。
2. **跨节点服务调度与容器真实运行验证**：
   - 通过 Swarm Manager 下发定向调度至 Kali 的服务：
     `docker service create --detach=true --no-resolve-image --name test-kali-svc --replicas 1 --publish 28080:80 --constraint "node.id == gf48p9x5of4xcqhnpehpoc12k" nginxdemos/hello:latest`
   - 服务在 Kali 子节点瞬间拉起，SSH 登录 Kali 执行 `docker ps` 实锤验证容器正在运行；
   - 外部通过 `curl -s -I http://192.168.0.124:28080` 成功返回 `HTTP/1.1 200 OK` (Nginx/1.29.1)。
3. **跨节点物理镜像传输流水线验证**：
   - 经测试，主节点向子节点通过 TLS 2376 双向认证管道推送 57.2MB 镜像仅耗时 5 秒，物理加载成功并在 `docker images` 中立即可见。
4. **编译构建通过**：
   - Maven 项目重新编译打包通过（`BUILD SUCCESS`），后端服务正常监听 8080 端口。
