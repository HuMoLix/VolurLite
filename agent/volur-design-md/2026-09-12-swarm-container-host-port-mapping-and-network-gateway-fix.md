# Docker Swarm 靶机容器宿主机端口映射与网络网关隔离根因排查及修复审计

## 1. 变更背景与全链路分析 (Root Cause & Full-Chain Analysis)

在管理员控制台容器管理视图（[`AdminContainerView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/container/AdminContainerView.vue)）手动启动 Docker 镜像（如 `nginx:alpine`）并对端口映射的主机端口留空（期望系统自动分配主机端口，如 `20000-30000`）时，出现以下两个关键现象：
1. **容器表格 / 数据库显示异常**：在前端表格的“端口映射”列中，未显示已分配的具体宿主机端口，而是显示类似 `*:80/TCP`；
2. **终端 `docker ps` 输出端口丢失或仅显示暴露端口**：
   - 在 Swarm 默认 ingress 模式下，`docker ps` 仅输出 `80/tcp`（容器暴露端口），并未显示 `0.0.0.0:<hPort>->80/tcp`；
   - 在启用了宿主机直接映射模式（`mode=host`）后，`docker ps` 里的 `PORTS` 列甚至完全空白，没有任何端口字符串。

通过全链路逆向追踪与本地真实 Docker Swarm 运行时复现，查明导致该问题的两个根本原因：

### 根因 1：后端随机端口写回丢失 (DTO Mutation Failure)
- **调用链路**：`AdminContainerController.manualStartInstance` -> `AdminContainerServiceImpl.manualStartInstance` -> `DockerSwarmRuntimeStrategy.startManualContainer`。
- **缺陷分析**：
  在 [`DockerSwarmRuntimeStrategy.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/strategy/DockerSwarmRuntimeStrategy.java) 中，系统虽为留空的宿主机端口分配了随机端口 `hPort = allocateRandomPort(portStart, portEnd)`，并作为服务创建参数传递给 Docker 命令，**但未将 `hPort` 写回入参 `portCfg.setHostPort(hPort)`**。
  因此，方法返回后，`request.getPorts()` 中的 `hostPort` 依然为 `null`。在后续调用 `formatPortsSummary(request.getPorts(), ...)` 时，由于 `hostPort == null`，其格式化结果被回退为 `*:80/TCP`，并持久化到数据库 `ctf_challenge_instance.ports`，导致前后端数据视图丢失分配的真实端口。

### 根因 2：`--internal` Overlay 网络对 `docker_gwbridge` 的剥夺与端口穿透瘫痪
- **调用链路**：`DockerSwarmRuntimeStrategy.resolveEffectiveSwarmNetwork` -> `ensureOverlayNetwork` -> `docker service create`。
- **缺陷分析**：
  先前引入的出网控制逻辑在 `outboundEnabled == false` 时，自动创建了带有 `--internal` 标志的 Swarm Overlay 网络（如 `volur-swarm-network-internal`）。
  根据 Docker 官方内核网络架构与 libnetwork 原理：
  - 带有 `--internal` 属性的 Overlay 网络，会**彻底剥夺容器连接宿主机默认网关桥接（`docker_gwbridge`）的权限**，容器仅持有 Overlay VXLAN 虚拟网卡 `eth0`；
  - 当通过 `--publish published=<hPort>,target=<cPort>,mode=host` 尝试将宿主机端口直接映射给该容器时，由于缺少 `docker_gwbridge` 与宿主机物理/虚拟网桥通信，Docker 无法创建相应的宿主机端口绑定规则，导致 `NetworkSettings.Ports` 为空 `{}`，`docker ps` 的 `PORTS` 列完全留空，宿主机发起的一切 TCP 连接皆无法转发至容器并发生超时。

---

## 2. 核心改动点 (Key Changes)

### 2.1 后端：Swarm 调度策略修复 ([`DockerSwarmRuntimeStrategy.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/strategy/DockerSwarmRuntimeStrategy.java))
1. **自动分配端口显式写回**：
   - 在 `startManualContainer` 中，分配随机端口后强制执行 `portCfg.setHostPort(hPort)`，确保当前请求实体的每个端口均持有确切分配的主机端口；
   - 若传入端口列表为空，则构建默认映射 DTO `new ContainerPortMappingDTO(primaryPort, containerPort, "TCP")` 存入 `request.setPorts(...)`。
2. **统一使用可外联的 Overlay 基础网络**：
   - 彻底废除将网络名强制重定向为 `-internal` 并在网络上叠加 `--internal` 破坏网桥的行为；
   - `ensureOverlayNetwork` 保证创建的是标准的 Swarm attachable 基础网络，确保 `docker_gwbridge` 正常附着到容器，使得 `mode=host` 端口映射能够正确穿透并在 `docker ps` 中完整展示 `0.0.0.0:<hPort>-><cPort>/tcp`。

### 2.2 后端：端口摘要持久化兜底 ([`AdminContainerServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/service/admin/impl/AdminContainerServiceImpl.java))
1. **`formatPortsSummary` 增强**：
   - 强化了端口格式化方法，当 `p.getHostPort()` 存在且大于 0 时优先展示；若单端口为缺省值则采用实际运行分配的 `defaultPort`，杜绝无意义的通配符 `*`，协议统一转换为大写。

### 2.3 前端：出网隔离交互与 DTO 对齐 ([`AdminContainerView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/container/AdminContainerView.vue))
1. **表单状态补充**：
   - 在 `manualStartForm` 中增加 `outboundEnabled: true`，并在弹窗打开时默认重置为 `true`；
   - 基础配置区域新增出网权限开关（`n-switch`），支持管理员在手动启动容器时明确指定网络出网隔离属性；
   - 提交 API 时显式将 `outboundEnabled` 字段提交至后端 `AdminContainerManualStartDTO`。
2. **中英文国际化语言包对齐**：
   - 在 `zh_CN/admin/containers.json` 与 `en_US/admin/containers.json` 中增补 `outboundLabel`、`outboundAllowed`、`outboundBlocked` 键值。

---

## 3. 验证与测试 (Verification & Testing)

1. **后端编译验证**：
   - 执行 `mvn clean compile -DskipTests`，全部 12 个模块编译通过 (`BUILD SUCCESS`, Total time: 13.281 s)。
2. **前端构建验证**：
   - 执行 `npm run build`，Vite 生产构建成功 (`✓ built in 4.74s`)。
3. **Docker Swarm 容器端口发布实测**：
   - 执行测试服务创建并在真实 Swarm 环境下验证：
     `docker ps` 正常输出：`0.0.0.0:29996->80/tcp`，成功在宿主机端口与容器端口之间建立直接映射。
