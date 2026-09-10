# 镜像管理解耦“关联赛题”、支持分层详情 (Layers)、类别编辑与节点物理镜像初始化校验

## 1. 变更背景与需求说明

在原有镜像管理中存在以下设计局限与痛点：
1. **模块职责耦合与反模式**：镜像管理模型中关联了 `challengeId` 与 `challengeTitle`，使底层的物理集群镜像与上层 CTF 赛题产生强耦合。赛题和镜像应为多对一引用关系，镜像本身无需反向绑定赛题，因此需要彻底取消镜像管理中的“关联赛题”所有前后端字段与逻辑。
2. **缺乏镜像分层与深入探查能力**：管理员在管理物理节点镜像时，无法查看镜像的 Layer（构建层）、指令（RUN/COPY/ENV/CMD）、尺寸、架构及容器基础配置，使用体验逊色于 Docker Hub。需要参考 Docker Hub 提供镜像分层 (Layers) 与 Inspect 详情查看界面。
3. **缺少类别维护机制**：镜像导入或被检测到后，无法在界面上动态调整其分类（例如赛题镜像 CHALLENGE、基础镜像 BASE、系统镜像 SYSTEM、工具镜像 TOOL 等）。需要支持在前端和后端修改镜像类别。
4. **数据库与物理节点脱节（残留孤儿镜像）**：在系统重启或服务启动初始化时，若节点上的镜像已被手工删除或节点下线，MySQL 中仍可能残留不存在的镜像记录，造成“幽灵镜像”。需在程序启动初始化时（以及手动扫描时），通过 Docker API 与节点探针进行双向比对与一致性对齐，彻底清除不存在的镜像记录。

---

## 2. 核心改动点与技术实现

### 2.1 后端改动 (volur-container)

1. **移除关联赛题相关字段与依赖**：
   - 实体类 [`ClusterImage.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/domain/entity/ClusterImage.java)：彻底移除 `challengeId` 与 `challengeTitle` 字段，数据库表结构不再维护此类冗余信息。
   - 视图模型 [`AdminClusterImageVO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/domain/vo/AdminClusterImageVO.java)：移除 `challengeId` 与 `challengeTitle`。
   - 服务实现类 [`AdminImageServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/service/admin/impl/AdminImageServiceImpl.java)：移除 `CtfChallengeRepository` 依赖注入，删除 JPA 动态查询中按赛题标题模糊匹配的逻辑，彻底杜绝模块间循环依赖隐患。

2. **新增分层视图与详情模型**：
   - 创建 [`DockerImageLayerVO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/domain/vo/DockerImageLayerVO.java)：
     包含 `layerIndex`, `layerId`, `instruction` (RUN/COPY/ENV/CMD等), `createdBy`, `sizeBytes`, `sizeFormatted`, `createdAt`, `comment`。
   - 创建 [`AdminClusterImageDetailVO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/domain/vo/AdminClusterImageDetailVO.java)：
     继承基础信息，聚合 `os`, `architecture`, `dockerVersion`, `author`, `workingDir`, `user`, `entrypoint`, `cmd`, `env`, `exposedPorts`, `labels` 及 `List<DockerImageLayerVO> layers`。
   - 创建 [`AdminImageTypeUpdateDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/domain/dto/AdminImageTypeUpdateDTO.java)：
     包含 `@NotBlank String imageType` 校验注解。

3. **镜像详情与分层解析实现**：
   - 在 [`AdminImageServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/service/admin/impl/AdminImageServiceImpl.java) 中实现 `getImageDetail(Long id)`：
     - 查询本地 Docker API / CLI (`docker inspect` & `docker history --no-trunc`)；
     - 结构化提取构建指令（自动剥离 `nop` 指令前缀），按步骤自顶向下倒序或自底向上排列展示；
     - 提取容器默认运行配置（Entrypoint, Cmd, Env, ExposedPorts, Labels, WorkingDir, User）。
   - 实现类别修改接口 `updateImageType(Long id, AdminImageTypeUpdateDTO request)`，校验类别合法性后持久化更新。

4. **系统初始化校验与孤儿镜像自动清理机制**：
   - 在 `loadAndSyncClusterImages()` 方法中（注册于 Spring `ApplicationReadyEvent` 监听器）：
     - 启动阶段首先通过本地 Docker 探针获取本地物理镜像集合；
     - 遍历所有注册且处于可用状态的子节点（Worker Nodes），探测各节点实际存在的镜像集合；
     - **反向对比校验 MySQL 中现存的全部 `ClusterImage` 记录**：
       - 逐个校验数据库记录的 `nodeIps` 列表中包含的节点 IP 是否仍旧承载该镜像；
       - 若某节点已不存在该镜像，则从该记录的 `nodeIps` 中剔除该 IP；
       - **若某镜像在本地及所有集群节点中均已无物理存在（`nodeIps.isEmpty()`），系统自动从 MySQL 物理硬删除该条幽灵记录**；
     - 校验完毕后，更新并补全已在物理节点上但数据库缺失的镜像元数据。

5. **控制器路由对齐 (AdminImageController)**：
   - 新增 `GET /api/v1/admin/images/{id}` 获取完整分层与详细 inspect 信息；
   - 新增 `PUT /api/v1/admin/images/{id}/type` 修改镜像类型。

---

### 2.2 前端改动 (volur-lite-view)

1. **类型与接口对齐 (`src/types/image/index.ts`, `src/api/image/index.ts`)**：
   - 移除 `challengeId` 与 `challengeTitle` 字段声明；
   - 补充 `DockerImageLayerVO`, `AdminClusterImageDetailVO`, `AdminImageTypeUpdateDTO` 强类型定义；
   - 导出 `getAdminImageDetailApi(id)` 与 `updateAdminImageTypeApi(id, data)`。

2. **多语言词条对齐 (`src/locales/zh_CN/admin/images.json`, `en_US/admin/images.json`)**：
   - 移除所有关于“关联赛题”的列标题、筛选提示与操作项；
   - 新增分层详情抽屉相关多语言词条：`layers`, `layerStep`, `command`, `size`, `created`, `config`, `env`, `labels`, `rawInspect` 等；
   - 新增编辑类别对话框相关多语言词条：`editType`, `category`, `categoryOptions` 等。

3. **镜像管理主界面重构 ([`AdminImageView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/image/AdminImageView.vue))**：
   - **表格列重构**：移除“关联赛题”列；优化“镜像类型”列（附带编辑按钮），优化“节点分布”与“镜像大小”展示。
   - **Docker Hub 风格镜像分层与 Inspect 抽屉**：
     - 顶部横幅展示全名、Tag、架构、OS、更新时间及分层总数；
     - 左侧时间线列表（Timeline）参考 Docker Hub：以卡片形式倒序排列各 Layer，以彩色 Tag 标明 Docker 指令（RUN, COPY, ADD, CMD, ENTRYPOINT, ENV 等），展示分层大小与创建时间，支持一键复制完整指令；
     - 右侧标签页：支持查看基础元数据、容器运行时配置（工作目录、用户、入口点、暴露端口）、环境变量键值对、标签键值对以及格式化高亮的原始 JSON 数据。
   - **编辑类别弹窗**：
     - 支持在 `CHALLENGE`、`BASE`、`SYSTEM`、`TOOL`、`OTHER` 类别间切换，修改后自动刷新表格列表。

4. **容器管理兼容性修复 ([`AdminContainerView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/container/AdminContainerView.vue))**：
   - 移除了容器手动启动弹窗中读取 `matched.challengeId` 的无效代码，彻底阻断旧字段依赖。

---

## 3. 安全性与影响范围说明

1. **解耦安全性**：
   - 赛题的创建与销毁完全通过镜像名称/标签进行逻辑引用，避免镜像删除或重构时级联污染赛题；
   - 镜像服务初始化校验只针对未在任何节点存在的孤儿镜像进行清理，不会误删仍被节点挂载的镜像。
2. **影响范围**：
   - 仅涉及容器/镜像模块（`volur-container`）及管理端视图（`AdminImageView.vue`、`AdminContainerView.vue`）；
   - 前端 TypeScript 编译（`vue-tsc --noEmit`）与 Vite 构建全部通过；
   - 后端 Maven 编译（`mvn test-compile`）全部 12 个模块 BUILD SUCCESS。
