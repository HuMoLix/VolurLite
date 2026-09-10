# 容器节点定向调度、跨节点镜像自动同步与手动启动靶机解耦重构

## 1. 变更背景与问题根因分析 (Background & Root Cause Analysis)

### 1.1 问题现象
在多节点集群环境下，当启动一个镜像仅存在于子节点（Worker 节点）中的容器实例时，发现子节点中一直未能成功运行容器，导致容器状态长时间卡在 `STARTING`（启动中），无法就绪且无法访问。

### 1.2 根因定位与机制分析
经过对 Docker Swarm / K3s 编排策略、节点状态探测与镜像加载逻辑的深入审查，发现存在以下复合缺陷：
1. **调度节点未加约束（Node Placement Constraint 缺失）**：
   在 `DockerSwarmRuntimeStrategy` 与 `K3sRuntimeStrategy` 中启动容器服务时，未指定 `--constraint` 或 `nodeSelector` 定向绑定镜像所在的节点。Docker Swarm 会根据各节点负载随机将任务分配给集群中的任意节点（如 Master 节点或其他未缓存该镜像的 Worker 节点）。
2. **缺乏私有/本地镜像拉取凭据导致 Swarm 陷入拉取死循环**：
   当 Swarm 将服务任务调度到未持有该镜像的节点时，由于该镜像为靶机平台本地构建或私有镜像，目标节点的 Docker 守护进程尝试向公网 Docker Hub 发起 `docker pull`。拉取必定失败（报错 `Pulling image...` 或 `Rejected`），Swarm 会不断重试重启任务，状态持续处于 `Preparing...`。
3. **状态探测机制被阻塞**：
   后台健康核验与状态轮询服务读取 Swarm Task 状态时，`Preparing...` 状态被映射为 `STARTING`，由于镜像拉取失败持续重试，导致实例永久卡在 `STARTING` 状态。
4. **访问链接 IP 硬编码反模式**：
   实例访问地址 `accessUrl` 过去直接读取系统全局设置 `CONTAINER_PUBLIC_HOST`（即 Master 节点公网 IP），而在 Docker Swarm 或单节点映射模式下，未将容器实际宿主节点 IP 正确拼装给选手与管理员。
5. **镜像缺乏跨节点流式同步机制**：
   集群在启动容器前未核验目标 Worker 节点是否存在该镜像，若目标节点无镜像，既无前置复制机制，亦无法跨节点调度。

---

## 2. 核心架构与功能改动点 (Key Implementations)

### 2.1 容器启动与跨节点镜像自动复制流水线
1. **跨节点物理镜像流式输送 (`AdminImageService.replicateImageToNode` & `ensureImageOnNode`)**：
   - 当选定运行节点目标 IP 后，先核验目标节点 Docker 引擎中是否已有该镜像。
   - 若目标节点缺失该镜像，自动寻找持有该镜像的源节点（Worker 或 Master）。
   - 通过高效流式管道：`docker save <fullImageName>` 的标准输出与目标节点 `docker load` 的标准输入在 JVM 内存中进行 `transferTo` 直连流转，无须借助中转磁盘文件或外部 Registry。
   - 镜像传输完成后，自动同步 MySQL 中的 `cluster_image` 记录与节点持有镜像计数。

2. **多节点容器定向调度与亲和约束**：
   - **Docker Swarm (`DockerSwarmRuntimeStrategy`)**：通过 Swarm API 自动反查目标节点 IP 对应的 Swarm Node ID，动态追加 `--constraint "node.id == <nodeId>"`；
   - **K3s / Kubernetes (`K3sRuntimeStrategy`)**：动态在 Pod 规范中配置 `nodeSelector: kubernetes.io/hostname: <nodeName>` 或 `k3s.io/internal-ip: <targetNodeIp>`；
   - **精确动态入口地址**：`accessUrl` 统一采用实际部署节点的 `targetNodeIp` 拼装对外访问端口，避免流量请求打到未暴露或未运行服务的节点。

### 2.2 选手与管理员容器分配策略
1. **普通用户（选手端 `CtfInstanceServiceImpl`）**：
   - 过滤集群中状态就绪 (`status == READY`) 且为资源节点 (`isResourceNode == true`) 的节点池；
   - 优先严格限制在 `role == WORKER` 的子节点；
   - 优先随机分配**已缓存该镜像**的 Worker 节点；
   - 若当前所有 Worker 节点均未缓存镜像，则在 Worker 候选池中随机指派一个节点，并在启动容器前触发 `adminImageService.ensureImageOnNode` 自动跨节点同步镜像；
   - 若集群仅有一个节点且无 Worker 节点，则自动回退至 Master 资源节点。

2. **管理端手动启动容器 (`AdminContainerServiceImpl.manualStartInstance`)**：
   - **彻底解耦题目绑定**：移除必须提供 `challengeId` 与关联题目的硬性校验，无需依赖 `CtfChallenge` 实体，管理员可直接指定任意镜像独立启动运行；
   - **移除旧题目实例销毁逻辑**：启动新容器时，不再强行扫描并删除用户同题目的旧实例；
   - **移除强制动态 Flag 生成**：仅在管理员显式输入 `customFlag` 时向容器环境变量注入 `FLAG` 与 `GZCTF_FLAG`；若留空则不生成任何 UUID Flag，保持原生镜像环境纯净；
   - **支持管理员指定目标 Worker 节点**：管理端启动请求 DTO 支持传入 `nodeIp`；若未指定则自动遍历候选资源节点，并由 `ensureImageOnNode` 确保镜像副本就绪。

### 2.3 数据库模型与实体拓展
1. **`CtfChallengeInstance` 实体拓展**：
   - 新增 `@Column(name = "node_ip", columnDefinition = "VARCHAR(64)") private String nodeIp;` 记录容器所在节点真实 IP；
   - 放宽 `flag` 字段约束为 `@Column(name = "flag", nullable = true, length = 255)`，支持无 Flag 纯净容器；
2. **DTO / VO 模型对齐**：
   - `AdminContainerManualStartDTO`：新增 `private String nodeIp;`；
   - `AdminContainerInstanceVO`：新增 `private String nodeIp;`、`private String nodeName;`、`private String nodeRole;`；
   - `AdminContainerController`：新增 `@GetMapping("/candidate-nodes")` 接口，供前端渲染节点选择器。

### 2.4 前端界面与国际化增强 (`volur-lite-view`)
1. **类型定义与 API 对齐**：
   - `src/types/container/index.ts`：更新 `AdminContainerManualStartDTO` 与 `AdminContainerInstanceVO`；
   - `src/api/container/index.ts`：导出 `getAdminContainerCandidateNodesApi()`；
2. **表格直观展示运行节点**：
   - 在 `AdminContainerView.vue` 数据表格中新增「运行节点 (Node)」列；
   - 悬浮卡片展示节点角色标签（Worker / Master）、节点名称及等宽字体 IP，并在节点脱机时清晰标识离线预警；
3. **手动启动弹窗体验优化**：
   - 移除旧有的赛题 ID 输入框；
   - 新增「运行节点 (Target Node)」下拉单选框，动态拉取可用 Worker 候选节点，支持清空选择以走自动调度；
   - 自定义 Flag 提示调整为「留空则不注入 Flag」；
4. **多语言更新**：
   - 补全 `src/locales/zh_CN/admin/containers.json` 与 `src/locales/en_US/admin/containers.json` 中的 `table.node` 与 `actions.targetNode*` 国际化文案。

---

## 3. 影响范围与测试验证 (Scope & Verification)

### 3.1 影响模块范围
- 后端：`volur-container`（实体、调度策略、服务实现、接口控制器）；
- 前端：`volur-lite-view`（容器管理视图、API 层、TS 类型、国际化资源）。

### 3.2 验证结果
- **后端单元与平台编译**：执行 `mvn test-compile -pl volur-container -am`，全部 10 个 Maven 模块构建通过，0 编译错误。
- **前端类型校验与打包**：执行 `npx vue-tsc --noEmit` 与 `npm run build`，生产构建打包通过，0 报错。
