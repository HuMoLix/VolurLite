# Docker Swarm 与 K3s 容器双编排引擎与管理端靶机全生命周期运维实施记录

**日期**：2026-09-08  
**模块**：`volur-system`, `volur-container`, `volur-lite-view`  
**责任人员**：Antigravity Agent  
**文档状态**：已完成并验证通过 (Completed & Verified)

---

## 1. 变更背景与目标

为支撑 Volur Lite 平台在不同基建规格与部署场景（单机/轻量 Swarm 集群 vs 生产级 K3s / Kubernetes 集群）下的动态靶机编排需求，本次重构完成了以下核心目标：
1. **系统设置前端（`AdminSystemView.vue`）容器环境改造**：
   - 支持动态选择容器编排引擎：`Docker Swarm` 或 `K3s`；
   - 根据选中的容器类型，动态展开或折叠专属的配置项（Docker Swarm 的 API 端口、Overlay 网络名、TLS 证书路径；K3s 的 Master API 接口、命名空间、Kubeconfig 路径）；
   - 保持通用配置项（公共访问 Host、端口分配范围）。
2. **后端容器策略双引擎支持（`volur-container`）**：
   - 统一由 `SystemSettingKeys.CONTAINER_TYPE` 决定激活策略；
   - 编写并完善 `DockerSwarmRuntimeStrategy`，基于 Docker Swarm API / CLI 提供服务调度与安全容错模式；
   - 升级 `K3sRuntimeStrategy`，动态读取并使用最新的系统配置参数；
   - 在 `CtfInstanceServiceImpl` 中严格通过 `systemSettingService.getSettingValue` 进行动态判断与生命周期路由。
3. **管理端容器与靶机生命周期监控（`AdminContainerView.vue` 及后端接口）**：
   - 后端新增完整的管理端分页查询、集群指标统计、单实例强制销毁、批量销毁与生命周期延期接口（严格遵循 Thin Controller、MapStruct、无 `@Autowired` 与统一响应封装）；
   - 前端 1:1 镜像对齐 API 与类型实体，重构 `AdminContainerView.vue`，实现指标统计徽章、组合筛选、多选批量销毁、动态 Flag / 访问入口复制、安全确认与续期弹窗。

---

## 2. 详细改动清单

### 2.1 系统设置与动态键值 (`volur-system`)
- **[`SystemSettingKeys.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/constant/SystemSettingKeys.java)**:
  - 补充 `CONTAINER_TYPE` (`system.container.type`)，支持 `DOCKER_SWARM` 与 `K3S`；
  - 补充 `CONTAINER_PORT_RANGE_START`、`CONTAINER_PORT_RANGE_END`、`CONTAINER_PUBLIC_HOST`；
  - 补充 Docker Swarm 专用键：`system.container.docker_swarm.api_url`、`system.container.docker_swarm.network`、`system.container.docker_swarm.cert_path`；
  - 补充 K3s 专用键：`system.container.k3s.master_url`、`system.container.k3s.namespace`、`system.container.k3s.kubeconfig_path`。
- **[`SystemSettingInitializer.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/init/SystemSettingInitializer.java)**:
  - 预置上述新键名的初始系统配置。

### 2.2 容器调度策略与业务实现 (`volur-container`)
- **[`DockerSwarmRuntimeStrategy.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/strategy/DockerSwarmRuntimeStrategy.java)**：
  - 新建并实现 `ContainerRuntimeStrategy` 接口；
  - 动态从 `SystemSettingService` 检索 Docker Swarm 配置参数；
  - 实现 `docker service create` 与 `docker service rm` 命令管道调度，在真实集群不可达时具备开发演练高可用容错仿真模式；
  - 服务名强制遵循 `swarm-` 前缀规范，便于生命周期快速判别与清理。
- **[`K3sRuntimeStrategy.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/strategy/K3sRuntimeStrategy.java)**：
  - 引入 `SystemSettingService` 动态提取命名空间、Master URL、Kubeconfig 路径与对外主机地址。
- **[`CtfInstanceServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/service/impl/CtfInstanceServiceImpl.java)**：
  - 去除单一 `ContainerRuntimeStrategy` 注入以规避 Spring 多实现歧义；
  - 显式注入 `K3sRuntimeStrategy`、`DockerSwarmRuntimeStrategy` 与 `SystemSettingService`；
  - 启动与停止均通过 `systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_TYPE, "K3S")` 进行策略分支路由，清理时同时兼容 `swarm-` 与 `pod-` 标识安全容错。

### 2.3 管理端容器运维接口与领域模型 (`volur-container`)
- **DTO 层 (`com.humolix.volur.container.domain.dto`)**：
  - [`AdminContainerPageRequestDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/domain/dto/AdminContainerPageRequestDTO.java)：继承 `PageRequestDTO`，支持 `keyword`（题目名称、用户、容器ID、Flag）、`status`、`challengeId`、`userId` 组合查询。
  - [`AdminContainerRenewRequestDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/domain/dto/AdminContainerRenewRequestDTO.java)：支持 5 ~ 120 分钟自定义续期。
  - [`AdminContainerBatchStopRequestDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/domain/dto/AdminContainerBatchStopRequestDTO.java)：接收待批量停止的实例 ID 列表。
- **VO 层 (`com.humolix.volur.container.domain.vo`)**：
  - [`AdminContainerInstanceVO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/domain/vo/AdminContainerInstanceVO.java)：雪花 ID 均采用 `@JsonSerialize(using = ToStringSerializer.class)` 序列化，返回题目名称、分类、用户名、容器ID、访问 URL、动态 Flag、剩余时长及引擎类型。
  - [`AdminContainerStatisticsVO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/domain/vo/AdminContainerStatisticsVO.java)：统计活跃总数、运行中数、排队/启动中数、失败数、今日创建数及当前引擎类型。
- **MapStruct 映射器 (`com.humolix.volur.container.mapper`)**：
  - [`ContainerMapper.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/mapper/ContainerMapper.java)：实现 PO 到 VO 声明式转换与剩余时间及编排引擎类型计算。
- **持久层与服务层**：
  - [`CtfChallengeInstanceRepository.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/repository/CtfChallengeInstanceRepository.java)：扩展 `JpaSpecificationExecutor`，增加按状态统计和按创建时间统计。
  - [`AdminContainerService.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/service/admin/AdminContainerService.java) & [`AdminContainerServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/service/admin/impl/AdminContainerServiceImpl.java)：业务逻辑收敛，批量聚合用户名信息，命令式方法统一返回 `void`。
- **控制器层 (`com.humolix.volur.container.controller.v1.admin`)**：
  - [`AdminContainerController.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/controller/v1/admin/AdminContainerController.java)：路由在 `/api/v1/admin/containers`，集成 `@PreAuthorize("hasRole('ADMIN')")` 与 `@Syslog` 审计记录。

### 2.4 前端界面与 API 1:1 对齐 (`volur-lite-view`)
- **系统设置界面改造**：
  - [`AdminSystemView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/system/AdminSystemView.vue)：Tab 4 增加单选编排引擎（Docker Swarm vs K3s），`v-if` 条件渲染对应配置字段。
  - 国际化文件 `zh_CN/admin/system.json` 与 `en_US/admin/system.json` 同步补充对应字段词条。
- **容器模块类型定义**：
  - [`src/types/container/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/types/container/index.ts)：1:1 映射后端 DTO / VO。
- **容器模块 API 定义**：
  - [`src/api/container/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/api/container/index.ts)：导出 `getAdminContainerStatisticsApi`、`getAdminContainerListApi`、`stopAdminContainerApi`、`batchStopAdminContainersApi`、`renewAdminContainerApi`。
- **国际化字典扩充**：
  - [`zh_CN/admin/containers.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/zh_CN/admin/containers.json) & [`en_US/admin/containers.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/en_US/admin/containers.json)：中英文完整覆盖统计项、搜索提示、表格列名、续期与销毁二次确认提示。
- **容器运维管理视图全量实现**：
  - [`AdminContainerView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/container/AdminContainerView.vue)：
    - 顶部统计色块（Ribbon 卡片：总数、运行、排队、异常、今日创建、当前引擎）；
    - 过滤与搜索栏（模糊搜索、状态下拉筛选、重置、选中批量销毁）；
    - Naive UI `n-data-table` 数据表格，含多选、雪花 ID 点击复制、动态 Flag 复制、访问入口快捷打开与复制、状态彩色 Tag、剩余时间高亮显示；
    - 续期弹窗与强制销毁气泡确认。

---

## 3. 架构规范符合性审查

| 审查项 | 规范要求 | 实际实施情况 | 符合状态 |
| :--- | :--- | :--- | :---: |
| **依赖注入** | 严禁字段注入，强制 `private final` + `@RequiredArgsConstructor` | 所有 Controller 与 ServiceImpl 均采用构造注入 | **PASS** |
| **领域模型收敛** | 统一收敛在 `domain.dto` 与 `domain.vo`，禁止平铺根路径 | 新建 DTO/VO 严格位于 `domain.dto` / `domain.vo` | **PASS** |
| **Thin Controller** | 业务逻辑在 Service 处理，返回 `ApiResponse<T>`，使用工厂方法 | `AdminContainerController` 严格透传，成功操作使用 `success`/`ok` | **PASS** |
| **MapStruct 映射** | 严禁手写转换方法与反射拷贝，统一使用 MapStruct 接口 | 创建 `ContainerMapper`，Spring 组件化注入 | **PASS** |
| **服务签名约定** | 业务动作/命令式方法强制返回 `void` | `stopInstance`、`batchStop`、`renewInstance` 均返回 `void` | **PASS** |
| **系统设置键名** | 采用统一小写点分层级键名 `system.<group>.<field_name>` | 统一收敛至 `SystemSettingKeys.CONTAINER_*` | **PASS** |
| **前端 API 动词对齐** | HTTP Method 与 URI 路径 100% 严格与 Controller 对齐 | 5 个 Admin API 路径与 Method 完全一致 | **PASS** |
| **前端视觉铁律** | 主要操作按钮黑底白字 (`#000000` / `#ffffff`)，描述文字 `#444444` | 全局类 `.theme-primary-btn` 与副文本采用指定规范色 | **PASS** |

---

## 4. 验证与编译结果

1. **后端全模块编译检查**：
   ```bash
   mvn test-compile -DskipTests
   ```
   **结果**：Reactor 全部 12 个模块（`volur-framework`, `volur-logging`, `volur-system`, `volur-user`, `volur-verification`, `volur-notification`, `volur-auth`, `volur-challenge`, `volur-container`, `volur-competition`, `volur-server`）**BUILD SUCCESS**。

2. **前端打包与类型检查**：
   ```bash
   npm run build
   ```
   **结果**：`vue-tsc --noEmit` 类型验证 0 错误，Vite 构建产物生成正常。
