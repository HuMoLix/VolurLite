# 集群设置移除私有镜像仓库管理配置与集群调度隔离机制审计方案

## 1. 变更背景与架构动因 (Context & Architectural Rationale)

在 Volur Lite 的多节点集群管理与容器编排架构中，前期在管理后台集群设置视图（`AdminClusterSettingsView` / `ParamsTab`）中暴露了“私有镜像仓库管理 (Docker Registry)”表单配置项（包含 `registryEnabled` 开关、`registryInternalUrl` 容器内网 REST 地址、`registryPublicPort` 宿主机映射端口以及实时在线探针）。

然而在系统生产与容器化交付架构（`docker-compose.yml`）中：
1. **单一数据真实源与基础设施固化**：`volur-registry`（基于 `registry:2`）已经作为平台底层不可分割的一体化基础设施组件显式编排，其容器服务名 `http://volur-registry:5000` 与对外端口映射 `${REGISTRY_PORT:-5000}:5000` 属于基础设施代码管理范畴；后端平台主程序 `volur-app` 通过环境变量强绑定了此项配置。
2. **防范配置割裂与系统级雪崩**：在 Web 界面暴露该底层参数，若运维人员误修改 URL 或端口、或误关闭开关，而底层 Docker 编排未联动变更，将直接造成后端的 `DockerRegistryClient` 连通失败，进而导致全平台的题目镜像构建、导入、跨节点推送同步与靶机拉取功能直接瘫痪。
3. **视图职责收敛**：`AdminClusterSettingsView` 的核心职责为**计算资源调度与集群节点拓扑管控**；而 Registry 镜像仓库的健康状态与镜像元数据管理已经在 `AdminImageView`（镜像管理中心）中具备专属的“Registry 仓库状态”卡片和管理交互。在集群设置中重复提供低层级配置不仅职责冗余，且极易造成误配置。

为此，本次重构对前端集群设置中的 Docker Registry 配置项、后端 DTO/VO 实体属性、数据库初始化与系统配置键名、以及客户端硬编码进行了彻底的清理和解耦重构。

---

## 2. 详细重构改动点清单 (Refactoring Changes)

### 2.1 数据库与系统配置层 (Database & System Setting Keys)
1. **`SystemSettingKeys.java`**：
   - 彻底删除废弃的私有镜像仓库配置键常量：
     - `CONTAINER_REGISTRY_ENABLED` (`"system.container.registry.enabled"`)
     - `CONTAINER_REGISTRY_INTERNAL_URL` (`"system.container.registry.internal_url"`)
     - `CONTAINER_REGISTRY_PUBLIC_PORT` (`"system.container.registry.public_port"`)
2. **`SystemSettingInitializer.java`**：
   - 移除系统启动时针对上述三个 Registry 键的数据库初始化逻辑，消除数据库多余配置噪音。
3. **`scripts/upgrade/upgrade_to_latest.sql`**：
   - 在迁移存储过程 `migrate_volur_schema_20260921` 中增加幂等数据清理 DML：
     ```sql
     IF EXISTS (
         SELECT 1 FROM information_schema.TABLES
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_setting'
     ) THEN
         DELETE FROM `sys_setting` WHERE `setting_key` IN (
             'system.container.registry.enabled',
             'system.container.registry.internal_url',
             'system.container.registry.public_port'
         );
     END IF;
     ```

### 2.2 基础设施与服务端架构 (Platform & Backend Refactoring)
1. **`application.yml`**：
   - 在 `volur.container` 配置块下新增标准化的 Spring Boot 配置项：
     ```yaml
     registry:
       enabled: ${VOLUR_CONTAINER_REGISTRY_ENABLED:true}
       internal-url: "${VOLUR_CONTAINER_REGISTRY_INTERNAL_URL:http://volur-registry:5000}"
       public-port: ${VOLUR_CONTAINER_REGISTRY_PUBLIC_PORT:5000}
     ```
2. **`DockerRegistryClient.java`**：
   - 解除对 `SystemSettingService` 动态查表的依赖，注入 Spring Boot `@Value` 配置属性，支持自动绑定环境变量与默认基础设施参数；
   - 移除了方法和类级别的废弃注释，消除硬编码回退逻辑，当内网服务名 `volur-registry` 在单机本地测试环境无法解析时自动平滑回退至 `127.0.0.1:{publicPort}`。
3. **`AdminClusterSettingsVO.java` & `AdminClusterSettingsDTO.java`**：
   - 移除 `registryEnabled`、`registryInternalUrl`、`registryPublicPort`、`registryOnline` 字段，使集群设置领域模型严格聚焦于计算节点调度与拓扑。
4. **`AdminClusterServiceImpl.java`**：
   - 解除 `AdminClusterServiceImpl` 对 `DockerRegistryClient` 的构造注入依赖；
   - 在 `getSettings()` 中移除 Registry 键值的读取与 VO 拼装；
   - 在 `updateSettings()` 中移除 Registry 键值的持久化写入逻辑。

### 2.3 前端工程 (Frontend UI Refactoring)
1. **新一代前端工程 (`src/volur-lite-view-NEXT`)**：
   - `src/types/cluster.ts`：移除 `AdminClusterSettingsVO` 和 `AdminClusterSettingsDTO` 中的 Registry 冗余属性；
   - `src/views/admin/cluster/tabs/ParamsTab.vue`：
     - 从模板中彻底移除“私有镜像仓库管理 (Docker Registry)”卡片及其对应的输入项；
     - 移除表单响应式对象 `form` 中的 `registry*` 属性及 `registryPublicPortStr` 计算属性，并清理 `watch` 监听中的赋值；
   - `src/views/admin/cluster/AdminClusterSettingsView.vue`：
     - 清理根视图中 `settings` 响应式初始状态中的废弃 Registry 属性。
2. **经典前端工程 (`src/volur-lite-view`)**：
   - `src/types/cluster/index.ts`：同步移除 VO 与 DTO 中的 Registry 属性；
   - `src/views/admin/cluster/tabs/ParamsTab.vue`：移除 Registry 表单展示块与 script 中的响应式字段，保持全端契约 100% 严格一致。

---

## 3. 集群调度与隔离策略的技术实现深度解析

平台的集群调度与隔离策略（`resourceIsolationEnabled` / `system.container.resource_isolation_enabled`）实现了控制面安全沙箱与计算面工作负载的彻底解耦：

### 3.1 核心设计动机
- **Master 控制面**：承载 Spring Boot 平台应用、MySQL 关系型数据库、Redis 高性能缓存、系统管理界面与认证授权等核心资产；
- **Worker 计算面**：承载选手题目容器，运行不可信二进制程序、Web 服务以及潜在包含提权利用链的用户代码。
- **物理隔离铁律**：Master 控制节点仅行使编排调度职责，绝对禁止任何不可信题目容器在 Master 主机上运行。

### 3.2 自适应降级与状态机实现
在 `AdminClusterServiceImpl.applyResourceIsolation(boolean enabled)` 中：
1. **多机生产集群环境**：
   - 当 `enabled = true` 且检测到集群中存在 `role == WORKER` 的节点时，将所有 `MASTER` 节点的 `isResourceNode` 强制标记为 `false`，将所有 `WORKER` 节点的 `isResourceNode` 标记为 `true`，完成主从控制面与数据面的严格物理分割。
2. **单机开发/测试环境平滑自适应容灾**：
   - 当 `enabled = false`，或虽然配置开启了隔离但集群当前仅有 Master 节点（单机轻量化部署模式）时，系统自适应将 `MASTER` 节点的 `isResourceNode` 置为 `true`，避免单机模式下无 Worker 节点可用而导致的调度死锁报错。

### 3.3 两阶段节点调度算法 (`CtfInstanceServiceImpl` & `AdminContainerServiceImpl`)
1. **资源资格初筛**：从数据库通过 `findByIsResourceNodeTrue()` 筛选出当前处于 `READY` 就绪状态的存活算力节点；
2. **Worker 角色硬过滤**：在候选池中优先保留 `role == WORKER` 的计算节点；
3. **高并发负载均衡打散**：通过 `ThreadLocalRandom` 在就绪计算节点池中进行随机选择，有效打散多选手并发开题请求对单一 Worker 节点的 CPU/内存冲击；
4. **兜底回退机制**：若节点列表为空，平滑回退至平台系统配置的 `CONTAINER_PUBLIC_HOST`。

### 3.4 编排引擎底层的硬约束下发 (Runtime Strategy Enforcement)
根据集群编排架构类型，由对应的运行时策略在执行创建指令时下发系统级强约束：
1. **Docker Swarm 编排模式 (`DockerSwarmRuntimeStrategy`)**：
   - **定向绑定**：通过 `--constraint "node.id == <id>"` 或 `"node.hostname == <name>"` 指定目标节点；
   - **弹性隔离**：若未指定单一节点，当开启隔离且存在 Worker 时，自动追加底层 Swarm 约束：
     ```bash
     --constraint "node.role == worker"
     ```
     从 Docker Swarm 内核调度器（SwarmKit）层面彻底杜绝将题目容器调度到 Manager 节点；
   - **配额沙箱**：通过 `--limit-cpu`、`--limit-memory` 施加 cgroups 硬限制，配合 Overlay 独立网络和外网访问开关（`outboundEnabled`）实现网络与进程级隔离。
2. **Kubernetes / K3s 编排模式 (`K3sRuntimeStrategy`)**：
   - **定向绑定**：在 PodSpec 中注入 `kubernetes.io/hostname: <nodeName>` 标签选择器；
   - **弹性隔离**：当开启隔离且存在 Worker 时，通过 PodSpec 自动注入：
     ```java
     podSpecBuilder.withNodeSelector(Map.of("node-role.kubernetes.io/worker", "true"));
     ```
     利用 Kubernetes 原生 `kube-scheduler` 调度器机制，强制过滤并绕开带有 `control-plane` 标签的 Master 节点；
   - **命名空间隔离**：题目 Pod 全部限制在专属的 Kubernetes Namespace（如 `volur-challenges`）内，实现租户级安全隔离。

---

## 4. 验证与测试结果 (Verification Results)

1. **后端全模块编译构建验证**：
   - 执行 `mvn clean compile -DskipTests`，全部 13 个子模块（`volur-framework`、`volur-system`、`volur-container`、`volur-server` 等）编译构建全部成功（BUILD SUCCESS，耗时 ~9.5s）。
2. **前端类型与打包验证**：
   - `volur-lite-view-NEXT` 执行 `vue-tsc --noEmit && vite build`，TypeScript 类型安全检查 100% 通过无任何类型错误。
3. **数据库兼容性验证**：
   - `scripts/upgrade/upgrade_to_latest.sql` 中的 DDL/DML 存储过程幂等执行逻辑完备，安全清理无残留。
