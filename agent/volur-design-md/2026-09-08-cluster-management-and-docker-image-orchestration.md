# 集群管理与 Docker 镜像全生命周期运维系统实施记录

**日期**：2026-09-08  
**模块**：`volur-container`, `volur-system`, `volur-lite-view`  
**责任人员**：Antigravity Agent  
**文档状态**：已完成并验证通过 (Completed & Verified)

---

## 1. 变更背景与架构决策

Volur Lite 作为支持高并发、分布式的网络安全竞赛演练平台，需要针对不同基础设施规格支持灵活的容器集群编排（轻量级 Docker Swarm 集群与生产级 K3s / Kubernetes 集群）。为了满足跨节点扩展、主从控制面隔离以及题目录入镜像的分发需求，本次重构完成了以下核心建设：

1. **Docker Swarm 与 K3s 统一 Docker 镜像体系确证**：
   - Docker Swarm 每个节点均为 Docker 守护进程，原生支持 Docker Registry v2 协议镜像；
   - K3s 默认容器运行时（containerd/docker）同样原生拉取并运行 OCI / Docker 镜像规范；
   - 因此，**镜像管理统一采用 Docker 镜像规范**（`repository:tag`、Docker Hub / 阿里云 ACR / 私有 Harbor），无需分化多套镜像标准。
2. **侧边栏导航重构与收敛**：
   - 侧边栏新增一级菜单【集群管理】（`AdminClusterGroup`），统领容器集群基础设施；
   - 下设三个业务子菜单：
     - **集群设置**（`AdminClusterSettings`，`/admin/clusters`）：移植并升级原有 `ContainerSettingsTab.vue` 的全部功能，并提供主从资源专属隔离、集群节点查看与横向扩展；
     - **容器管理**（`AdminContainers`，`/admin/containers`）：原有靶机实例生命周期监控与交互；
     - **镜像管理**（`AdminImages`，`/admin/images`）：Docker 镜像查看、拉取、节点定向分发与自动同步。
3. **主从资源专属调度与控制面安全保护 (Resource Isolation)**：
   - 平台所在的节点默认定义为主节点（`MASTER`）；
   - 当集群通过添加 IP 扩展了工作资源节点（`WORKER`）后，启用资源隔离策略，**禁止在 Master 节点上调度运行赛题容器，所有动态靶机 Pod / Service 均约束调度到资源工作节点**，保护平台主控制面的安全与稳定性，防止赛题资源耗尽或容器逃逸威胁主控服务器。
4. **镜像定向下载约束与集群扩展自动迁移**：
   - 在镜像管理中支持选择下载的目标节点：
     - 若当前集群仅存在单个主节点（单机开发/演练模式），允许并默认下载至主节点；
     - 若当前集群存在多个节点，系统强制禁用主节点下载选项，**必须选择资源工作节点或广播至所有资源节点**；
   - 当管理员对集群进行横向扩展（添加 Worker 节点）时，后台自动异步将主节点已缓存的所有赛题镜像（`imageType == CHALLENGE`）转移同步至新增的资源工作节点，实现新节点“即插即用，立即可调度赛题”。

---

## 2. 详细改动清单

### 2.1 系统设置与常量定义 (`volur-system`)
- **`SystemSettingKeys.java`**:
  - 新增 `CONTAINER_RESOURCE_ISOLATION_ENABLED = "system.container.resource_isolation_enabled"`（是否开启多节点资源隔离）；
  - 新增 `CONTAINER_AUTO_MIGRATE_IMAGES_ENABLED = "system.container.auto_migrate_images_enabled"`（集群扩展时是否自动迁移主节点题目镜像）。
- **`SystemSettingInitializer.java`**:
  - 自动为上述新配置注入默认值（`"true"`）。

### 2.2 领域实体与持久层 (`volur-container`)
- **`ClusterNode.java` (`domain.entity.ClusterNode`)**:
  - 记录集群节点名称、IP 地址、SSH 端口、集群类别（`DOCKER_SWARM` / `K3S`）、角色（`MASTER` / `WORKER`）、是否资源节点（`isResourceNode`）、健康状态（`READY` / `OFFLINE`）、硬件规格（CPU 核心、内存、容器数、镜像数）及心跳时间。
- **`ClusterImage.java` (`domain.entity.ClusterImage`)**:
  - 记录 Docker 镜像仓库名、标签 Tag、镜像摘要 ID、体积大小、分类属性（`CHALLENGE` / `BASE` / `SYSTEM`）、关联题目 ID 及名称、已下载缓存的节点 IP 列表（`nodeIps`，逗号分隔）、拉取同步状态。
- **`ClusterNodeRepository.java` & `ClusterImageRepository.java`**:
  - 继承 `JpaRepository` 与 `JpaSpecificationExecutor`，支持动态多条件组合查询与 IP 检索。

### 2.3 传输对象与视图模型 (`volur-container`)
- **DTO 层 (`domain.dto`)**:
  - `AdminClusterSettingsDTO.java`：集群编排类型、端口映射范围、主机 Host、Swarm/K3s 专属参数、资源隔离开关、镜像自同步开关；
  - `AdminClusterNodeCreateDTO.java`：节点名称、IP 地址、SSH 端口、集群类型、角色、是否为资源节点；
  - `AdminClusterNodeRoleUpdateDTO.java`：更新节点角色与资源调度分配；
  - `AdminImagePullRequestDTO.java`：待拉取的 Docker 镜像名与目标下载节点 IP；
  - `AdminImagePageRequestDTO.java`：继承 `PageRequestDTO`，支持关键词、镜像类别与所在节点 IP 分页检索。
- **VO 层 (`domain.vo`)**:
  - `AdminClusterSettingsVO.java`：返回集群参数及总节点数、Master 数、Worker 数、就绪数统计；
  - `AdminClusterNodeVO.java`：节点详情视图（雪花 ID 字符串序列化、格式化内存等）；
  - `AdminClusterImageVO.java`：镜像详情视图（完整镜像名、格式化体积、已覆盖节点列表等）；
  - `AdminClusterImageStatisticsVO.java`：总镜像数、题目镜像数、基础镜像数、总存储大小、覆盖节点数统计。
- **MapStruct 映射器 (`mapper`)**:
  - `ClusterNodeMapper.java`：实体到 VO 声明式转换；
  - `ClusterImageMapper.java`：实体到 VO 声明式转换与节点 IP 列表切分。

### 2.4 服务层与控制器 (`volur-container`)
- **`AdminClusterService.java` & `AdminClusterServiceImpl.java`**:
  - 业务逻辑完全收敛在 Service：
    - `getSettings` / `updateSettings`：全量读写系统参数与集群节点统计；
    - `testConnection`：转发至具体编排引擎执行连通性探测；
    - `listNodes`：列出所有节点（无节点时自动初始化本地 Master 节点）；
    - `addNode`：IP 查重校验、创建节点；检测到 Worker 节点加入且开启资源隔离时，自动将 Master 的 `isResourceNode` 设为 `false`；触发题目镜像向新节点的后台自动同步；
    - `updateNodeRole` / `removeNode`：支持修改角色，严格校验禁止移除唯一的 Master 节点；
    - `syncImagesToNode`：向指定节点下发题目镜像同步。
- **`AdminImageService.java` & `AdminImageServiceImpl.java`**:
  - `getStatistics`：概况指标聚合；
  - `listImages`：Specification 动态多条件分页查询；
  - `pullImage`：**严格执行多节点安全校验规则**（多节点下主节点禁止下载题目镜像，强制要求资源节点）；
  - `deleteImage`：支持从指定节点清除或全量清除；
  - `migrateMasterImagesToWorker` / `syncAllImagesToResourceNodes`：将 Master 节点的赛题镜像批量同步至工作节点。
- **`AdminClusterController.java` (`/api/v1/admin/clusters`)**:
  - 提供 `/settings`、`/test-connection`、`/nodes`、`/nodes/{id}/role`、`/nodes/{id}`、`/nodes/{id}/sync-images` RESTful API；
- **`AdminImageController.java` (`/api/v1/admin/images`)**:
  - 提供 `/statistics`、GET 列表、`/pull`、`/{id}` 删除、`/sync-all` RESTful API。
- **`DockerSwarmRuntimeStrategy.java`**:
  - 动态读取 `SystemSettingKeys.CONTAINER_RESOURCE_ISOLATION_ENABLED`，开启时自动为创建的 Service 添加 `--constraint node.role == worker`，确保题目容器仅落入资源节点。

### 2.5 前端界面与交互 (`volur-lite-view`)
- **API 与类型实体对齐**：
  - `src/types/cluster/index.ts` & `src/types/image/index.ts`：1:1 映射后端 DTO/VO；
  - `src/api/cluster/index.ts` & `src/api/image/index.ts`：100% 严格对齐 Controller 路由与 HTTP 动词。
- **国际化字典覆盖**：
  - `zh_CN/layout.json` & `en_US/layout.json`：补充 `clusters`、`cluster_settings`、`images` 词条；
  - `zh_CN/admin/clusters.json` & `en_US/admin/clusters.json`：覆盖集群配置、测试、节点拓扑、扩展模态框等全部中英文文本；
  - `zh_CN/admin/images.json` & `en_US/admin/images.json`：覆盖镜像管理统计项、表格、拉取对话框与多节点安全约束提示。
- **路由与侧边栏改造**：
  - `src/router/routes.ts`：增加 `/admin/clusters`（`AdminClusterSettings`）与 `/admin/images`（`AdminImages`）；
  - `src/layout/AdminLayout.vue`：侧边栏将原单一容器项升级为 `AdminClusterGroup` 下设【集群设置】、【容器管理】、【镜像管理】三级子菜单。
- **业务视图页面实现**：
  - `src/views/admin/cluster/AdminClusterSettingsView.vue`：
    - 顶部节点概览 Ribbon 卡片；
    - Tab 1（集群参数与调度设置）：移植原有 ContainerSettingsTab 全部表单，并新增主从资源专属隔离开关与自动镜像迁移开关，支持连通性测试与配置保存；
    - Tab 2（节点拓扑与集群扩展）：节点数据表格（角色彩色标签、资源节点状态、规格、容器数、镜像数、操作菜单）；包含【扩展集群 - 添加工作节点】弹窗；
  - `src/views/admin/image/AdminImageView.vue`：
    - 顶部统计色块（镜像总数、题目镜像、基础镜像、存储占用、覆盖节点数）；
    - 过滤搜索与操作栏（模糊查询、类别下拉、拉取镜像、一键全量同步）；
    - 【下载/拉取镜像】弹窗：动态判断单节点 vs 多节点架构，多节点下禁用主节点选项，强制选择资源工作节点，附带安全警示 Alert；
    - 数据表格支持雪花 ID 与完整镜像名称一键复制、类型彩色 Tag、已部署节点列表与删除气泡确认。
  - `src/views/admin/system/tabs/ContainerSettingsTab.vue`：
    - 增加友好指引横幅与快捷跳转按钮至【集群管理 > 集群设置】，保持系统设置页面的向后兼容性。

---

## 3. 规范审查与验证结果

| 检查项 | 规范要求 | 实施状态 |
| :--- | :--- | :---: |
| **依赖注入** | 严禁字段注入，强制 `private final` + `@RequiredArgsConstructor` | ✅ PASS |
| **领域模型包收敛** | 实体、DTO、VO 收敛在 `domain.*`，无样板手写 Getter/Setter | ✅ PASS |
| **Thin Controller** | 业务逻辑在 Service 处理，返回强类型 `ApiResponse<T>` | ✅ PASS |
| **MapStruct 映射** | 严禁手写转换方法，使用 MapStruct 接口 | ✅ PASS |
| **系统设置键名** | 全系统统一使用小写点分层级规范（`system.container.*`） | ✅ PASS |
| **前端 API 动词对齐** | HTTP Method 与 URI 路径 100% 严格与 Controller 对齐 | ✅ PASS |
| **前端视觉铁律** | 主要按钮黑底白字（`#000000` / `#ffffff`），描述文本 `#444444` | ✅ PASS |
| **类型安全** | 严禁 `any` 泛滥，雪花 ID 采用 `string \| number` 防精度丢失 | ✅ PASS |
| **国际化覆盖** | 中英文双语完整覆盖 | ✅ PASS |

### 编译与测试结果：
1. **后端 12 个模块编译**：`mvn test-compile -DskipTests` -> **BUILD SUCCESS**
2. **后端单元测试**：`mvn test -pl volur-container` -> **Tests run: 6, Failures: 0, Errors: 0, BUILD SUCCESS**
3. **前端 TypeScript 与 Vite 构建**：`npm run build` -> **`vue-tsc --noEmit` 0 错误，打包正常生成产物**
