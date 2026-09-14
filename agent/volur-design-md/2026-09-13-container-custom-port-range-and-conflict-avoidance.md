# K3s 与 Docker Swarm 统一自定义端口范围控制与全链路端口冲突避免机制实现

## 1. 变更背景
在集群管理与靶机容器调度中，平台支持配置动态端口映射区间（`system.container.port_range_start` 与 `system.container.port_range_end`，默认 `20000-30000`）。但此前存在以下架构与运行态缺陷：
1. **K3s 模式下端口范围完全失效**：
   - 动态靶机启动（`startContainer`）构建 Kubernetes `NodePort` Service 时，未声明 `.withNodePort(...)`，导致 Kubernetes / K3s 的 API Server 强制接管并从其默认的 `30000-32767` 范围分配端口；后续读取分配端口后，兜底的 `RandomUtil.randomInt` 沦为无法执行的死代码；
   - 手动容器启动（`startManualContainer`）硬编码限制了 `30000 <= hostPort <= 32767`，直接忽略并丢弃了用户的自定义范围；
2. **端口碰撞风险**：
   - 无论是 Docker Swarm 还是 K3s，此前均通过纯数学随机 `RandomUtil.randomInt` 计算端口，未进行网络层面与业务层面的冲突检测；
   - 若随机生成的端口已被系统其他守护进程、外部网络应用、或正在启动中的并发容器占用，容器启动即会报错失败。

## 2. 核心改动说明

### 2.1 引入多层级防冲突分配组件 `ContainerPortAllocator`
- **新增文件**：[`ContainerPortAllocator.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/util/ContainerPortAllocator.java)
- **核心实现机制**：
  1. **数据库活跃实例排查**：在分配前，自动从 [`CtfChallengeInstanceRepository`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/repository/CtfChallengeInstanceRepository.java) 查询处于 `RUNNING`、`STARTING`、`DOWNLOADING_IMAGE` 状态的活跃容器，正则提取已绑定的所有主机端口进行排除；
  2. **外部/集群已占端口排查**：支持由运行时策略传入集群现有服务的端口集合（如 K3s 全集群现有 Service 的 NodePort 列表）；
  3. **并发内存临时预占表**：基于 `ConcurrentHashMap` 维护 `host:port` 与 `*:port` 的预占表，并设置 60 秒 TTL 自动过期淘汰，杜绝高并发毫秒级启动时的端口冲突；
  4. **实时网络探活探测**：
     - 若目标为主机本地地址（`127.0.0.1`, `localhost` 或当前宿主机网络接口），利用 `ServerSocket.bind` 探测是否可绑定；
     - 若目标为远程 Worker 节点 IP，通过短超时（200ms）TCP `Socket.connect` 探测该端口是否处于监听状态；
  5. **均匀离散与范围遍历**：在 `[portStart, portEnd]` 区间内随机起点递增取模探测，既保证端口分布均匀安全，又确保在有空闲端口时 100% 能快速分配成功。

### 2.2 重构 `DockerSwarmRuntimeStrategy`
- **文件**：[`DockerSwarmRuntimeStrategy.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/strategy/DockerSwarmRuntimeStrategy.java)
- **改动**：
  - 接入 `ContainerPortAllocator`，移除原私有纯随机方法 `allocateRandomPort`；
  - `startContainer`：调用 `allocateAvailablePort` 分配经过冲突校验的可用端口，并以 `mode=host` 发布；
  - `startManualContainer`：对手动填写的 `hostPort` 执行 `verifyPortAvailable` 可用性校验，对未填写的自动调用 `allocateAvailablePort` 分配并写回配置。

### 2.3 重构 `K3sRuntimeStrategy`
- **文件**：[`K3sRuntimeStrategy.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/strategy/K3sRuntimeStrategy.java)
- **改动**：
  - 新增 `getExistingK3sNodePorts` 方法，查询当前 K3s 集群命名空间下已存在 Service 的全部 `nodePort`；
  - `startContainer`：使用 `ContainerPortAllocator` 分配可用端口，并在构建 `NodePort` Service 时**显式声明 `.withNodePort(targetPort)`**，使自定义端口范围真正落地到 K3s ；
  - `startManualContainer`：移除 30000~32767 的硬编码，接入 `ContainerPortAllocator` 进行防冲突校验与自动空闲分配；
  - **友好异常与配置指引**：在捕获 K3s 错误时，针对 K3s 未开启自定义 NodePort 范围时抛出的 `provided port is not in the valid range` 异常，精确转换为引导指引，告知管理员若使用低于 30000 的端口，需在 K3s 启动参数增加 `--kube-apiserver-arg=service-node-port-range=...`。

### 2.4 数据层与国际化提示对齐
- **数据库接口**：[`CtfChallengeInstanceRepository.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/repository/CtfChallengeInstanceRepository.java) 新增 `findByStatusIn` 派生查询；
- **前端文案**：[`zh_CN/admin/clusters.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/zh_CN/admin/clusters.json) 与 [`en_US/admin/clusters.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/en_US/admin/clusters.json) 在 `port_range_hint` 中补充说明 K3s 自定义范围需启动参数对齐。

## 3. 安全隐患说明与设计遵循
1. **规范遵循**：严格遵守 [`Rules.md`](file:///Users/humolix/Documents/Dev/Volur-Lite/agent/volur-design-skills/Rules.md) 构造注入、不可变声明规范；
2. **防死锁与防挂死**：TCP 探活使用短超时（200ms），内存预占具有绝对过期时间；
3. **编译与构建验证**：
   - 后端 10 个子模块通过 `mvn test-compile -DskipTests` 全量编译成功；
   - 前端通过 `npm run build` 打包验证无误。

## 4. 影响范围
- Docker Swarm 动态容器及手动容器的宿主机端口发布；
- K3s NodePort Service 端口绑定与手动容器发布；
- 平台高并发场景下动态端口冲突的自动规避能力。
