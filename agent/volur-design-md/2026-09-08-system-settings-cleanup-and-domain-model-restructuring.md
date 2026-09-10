# 系统设置容器环境Tab剥离与模块Domain领域模型规范重构施工文档

## 1. 变更背景与架构目标

1. **统一容器/集群管理入口**：
   在前期引入完整的“集群管理”（Cluster Management）模块后，容器集群的编排引擎类型（Docker Swarm / K3s）、节点扩展、连通性探测以及镜像分发管理均已在 `AdminClusterSettingsView.vue`、`AdminImageView.vue` 及 `api/cluster/index.ts` 中完整落地。原有“系统设置”中的“容器环境与动态靶机”Tab 存在功能重叠，且易造成运维人员修改参数时的脏写冲突。本次需将该 Tab 及相关平级 API 彻底移除，仅保留集群管理作为唯一真实配置源。
2. **严格遵循 Rules.md 2.1 领域模型包组织规范**：
   `Rules.md` 第 2.1 节明确规范：
   > 各业务模块中的所有实体与领域模型（包括 DTO、VO、PO、BO、Entity、DO 等），**必须统一存放在模块的 `domain` 包路径下**。严禁在模块根路径下建立与 `domain` 平级的 `dto/`、`vo/`、`entity/` 等包。
   前期历史代码中，`volur-container` 存在与 `domain` 平级的 `dto/`、`entity/`、`vo/` 目录，且枚举类放于 `entity.enums`；`volur-framework` 存在 `common.dto`、`common.entity.enums`、`common.entity.converter` 等非标准分布。本次对上述两个核心基础模块进行系统化收敛重构，使全平台 100% 对齐规范。

---

## 2. 详细改动点与实施清单

### 2.1 前端：系统设置清理与 API 归口

1. **视图层清理 (`AdminSystemView.vue`)**：
   - 移除 `<n-tab-pane name="container">` 及其子组件 `<ContainerSettingsTab>`；
   - 移除 `ContainerSettingsTab.vue` 组件导入；
   - 从表单状态 `settingsForm` 中移除 `containerType`, `portRangeStart`, `portRangeEnd`, `publicHost`, `dockerSwarmApiUrl`, `dockerSwarmNetwork`, `dockerSwarmCertPath`, `k3sMasterUrl`, `k3sNamespace`, `k3sKubeconfigPath` 等 10 项容器底层基础设施属性；
   - 同步更新 `fetchSettings()`、`handleSaveSettings()` 以及 `handleResetDefault()`。
2. **组件移除**：
   - 彻底删除 `src/volur-lite-view/src/views/admin/system/tabs/ContainerSettingsTab.vue`。
3. **接口与实体归口**：
   - `src/volur-lite-view/src/api/container/index.ts`：移除 `testAdminContainerConnectionApi`；
   - `src/volur-lite-view/src/types/container/index.ts`：移除 `AdminContainerTestConnectionDTO` 与 `AdminContainerTestConnectionVO`；
   - `src/volur-lite-view/src/types/cluster/index.ts`：显式收敛定义 `AdminContainerTestConnectionDTO` 与 `AdminContainerTestConnectionVO`，集群测试能力完全归属于 `@/api/cluster`。
4. **多语言词条清理**：
   - `src/volur-lite-view/src/locales/zh_CN/admin/system.json` 与 `src/volur-lite-view/src/locales/en_US/admin/system.json`：移除 `tabs.container` 标签及 `container: { ... }` 废弃词条块。

### 2.2 后端：容器控制器与服务精简

1. **`AdminContainerController.java`**：
   - 移除冗余的 `POST /api/v1/admin/containers/test-connection` (`testConnection`) 路由。连通性自检由 `AdminClusterController` 独占提供（`POST /api/v1/admin/clusters/test-connection`）。
2. **`AdminContainerService.java` & `AdminContainerServiceImpl.java`**：
   - 移除 `testConnection(AdminContainerTestConnectionDTO request)` 接口方法与实现，解耦多余的连通性探测职责。

### 2.3 后端：volur-container 领域模型收敛重构

按照 `Rules.md` 规范，对模型文件进行目录迁移与包名重构：

| 原路径/包名 | 新路径/包名 | 类别 |
| :--- | :--- | :--- |
| `com.humolix.volur.container.dto.ContainerRunResultDTO` | `com.humolix.volur.container.domain.dto.ContainerRunResultDTO` | DTO |
| `com.humolix.volur.container.entity.enums.InstanceStartupStatus` | `com.humolix.volur.container.domain.enums.InstanceStartupStatus` | Enums |
| `com.humolix.volur.container.entity.CtfChallengeInstance` | `com.humolix.volur.container.domain.entity.CtfChallengeInstance` | Entity |
| `com.humolix.volur.container.vo.ChallengeInstanceResponseVO` | `com.humolix.volur.container.domain.vo.ChallengeInstanceResponseVO` | VO |

清理了 `volur-container` 根包下平铺的 `dto/`, `entity/`, `vo/` 目录，并同步全量更新如下消费方：
- `ContainerRuntimeStrategy.java`
- `DockerSwarmRuntimeStrategy.java`
- `K3sRuntimeStrategy.java`
- `CtfChallengeInstanceRepository.java`
- `ContainerMapper.java`
- `AdminContainerPageRequestDTO.java`
- `AdminContainerInstanceVO.java`
- `CtfInstanceService.java` & `CtfInstanceServiceImpl.java`
- `AdminContainerService.java` & `AdminContainerServiceImpl.java`
- `CtfInstanceController.java`
- `volur-competition` 模块中的 `CtfStageService.java`、`CtfStageServiceImpl.java` 与 `CtfStageController.java`。

### 2.4 后端：volur-framework 领域模型规范收敛

在 `volur-framework` 下统一建立 `domain` 顶层包，消除散落在 `common` 下的模型定义：

| 原路径/包名 | 新路径/包名 | 类别 |
| :--- | :--- | :--- |
| `com.humolix.volur.framework.common.dto.AttachmentDTO` | `com.humolix.volur.framework.domain.dto.AttachmentDTO` | DTO |
| `com.humolix.volur.framework.common.dto.PageRequestDTO` | `com.humolix.volur.framework.domain.dto.PageRequestDTO` | DTO |
| `com.humolix.volur.framework.common.entity.enums.EnumEntity` | `com.humolix.volur.framework.domain.enums.EnumEntity` | Enums |
| `com.humolix.volur.framework.common.entity.converter.AbstractEnumConverter` | `com.humolix.volur.framework.domain.converter.AbstractEnumConverter` | Converter |
| `com.humolix.volur.framework.common.converter.AttachmentConverter` | `com.humolix.volur.framework.domain.converter.AttachmentConverter` | Converter |

清理了 `volur-framework` 根路径下的 `common/dto/`, `common/entity/`, `common/converter/` 目录，并更新全平台所有下游模块：
- `volur-user`：`AdminUserPageRequestDTO`、`Gender`、`PrivacyType`
- `volur-challenge`：`ChallengeType`、`AwdCategory`、`ChallengeStageType`、`TheoryType`
- `volur-competition`：`CompetitionStage`、`CompetitionStatus`、`FieldType`、`FillScope`、`MemberRole`、`MemberStatus`、`StageStatus`、`SubmissionStatus`、`TeamStatus`
- `volur-container`：`AdminImagePageRequestDTO`、`AdminContainerPageRequestDTO`

---

## 3. 安全性与架构收益评估

1. **数据一致性与单点配置保障**：
   彻底杜绝了管理员在“系统设置”和“集群设置”两个页面分别修改导致不同缓存/数据库覆盖的问题，保障集群调度配置在运行时具备唯一受控入口。
2. **架构合规性与可维护性提升**：
   全系统各模块统一遵循 `domain.dto`、`domain.vo`、`domain.entity`、`domain.enums`、`domain.converter` 分层，消除歧义，大幅降低新开发者上手与 AI 辅助代码生成的复杂度。
3. **零侵入平滑迁移**：
   通过全局编译与测试验证，所有下游业务、ORM 映射（JPA Specification / Hibernate）、JSON 序列化（Jackson）、Spring MVC 参数绑定与前端强类型调用均保持 100% 连贯性。

---

## 4. 验证与测试记录

1. **前端工程构建验证**：
   - 执行 `npm run build`（包含 `vue-tsc --noEmit` 强类型检查与 Vite 生产级打包）。
   - 结果：**4264 modules transformed, 0 errors, build success**。
2. **后端全量编译验证**：
   - 执行 `mvn clean test-compile -DskipTests`。
   - 结果：**12 modules BUILD SUCCESS**。
3. **后端单元与集成测试验证**：
   - 执行 `mvn test`。
   - 结果：`volur-framework`、`volur-container`、`volur-competition`、`volur-verification` 等所有模块单元测试全部 **SUCCESS（Failures: 0, Errors: 0, Skipped: 0）**。
