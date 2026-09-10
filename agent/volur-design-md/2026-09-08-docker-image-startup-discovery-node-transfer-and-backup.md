# Docker 集群镜像动态启动探测、节点转移与容灾备份功能施工审计记录

## 1. 变更背景与需求概述
在原有的镜像管理设计中，`AdminImageServiceImpl` 存在 `checkAndSeedInitialImages` 伪造/预置测试镜像的机制，导致开发与生产环境未能直接反映底层宿主机与集群节点的真实镜像现状。
本次重构要求：
1. **彻底废除 `checkAndSeedInitialImages` 假数据预置机制**，移除所有种子数据初始化残留逻辑；
2. **构建启动期自动化多级 Docker 探测器**：在服务启动时，优先通过 Docker API 发现加载本地宿主机的真实 Docker 镜像，随后巡检集群各子节点（Worker Nodes）中的镜像分布元数据；
3. **支持按需主动探测同步**：除了启动期自动扫描外，管理端提供实时扫描重新同步 API；
4. **前端视图节点分布可视化呈现**：在 `AdminImageView.vue` 的镜像列表中，清晰展示每个镜像所部署的节点详情（包含节点名称、节点角色 Master/Worker、IP 地址及状态 Tooltip 提示）；
5. **镜像节点跨节点转移功能 (Transfer)**：支持将镜像从指定源节点调度迁移至目的节点，并同步调整节点拓扑关系与镜像计数；
6. **镜像多节点容灾与归档备份功能 (Backup)**：支持创建跨节点容灾副本（NODE，保留原节点副本并同步至目标节点）与本地离线归档包导出（ARCHIVE，通过 `docker save` 导出至平台归档目录）；
7. **前后端接口、数据模型与校验铁律 100% 严格对齐**。

---

## 2. 关键改动点说明

### 2.1 后端工程改动 (volur-container)
1. **DTO 新增与标准化声明**：
   - `AdminImageTransferDTO`: 包含 `imageId`, `sourceNodeIp`, `targetNodeIp`，严格添加 Jakarta Validation 注解；
   - `AdminImageBackupDTO`: 包含 `imageId`, `backupType` ("NODE" | "ARCHIVE"), `targetNodeIp`。
   - 遵循规范：统一添加 `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`，严禁使用 `@Builder`。
2. **`AdminImageService` 与 `AdminImageServiceImpl` 重构**：
   - 移除 `checkAndSeedInitialImages()` 及其在 `getStatistics`, `listImages`, `migrateMasterImagesToWorker` 中的所有调用；
   - 新增 `loadAndSyncClusterImages()`：
     - 本地探测：通过 Docker HTTP REST API 或 Docker CLI 动态获取本地主机镜像列表，过滤 `<none>` 虚镜像，并与 `ctf_challenge` 关联匹配题目标题及类别；
     - 集群子节点探测：遍历 `clusterNodeRepository` 中所有非 Master 的资源子节点，探测各节点 2375 端口或远程 Docker API 并同步镜像元数据；
     - 自动更新节点镜像数量统计 (`updateNodeImageCounts()`)；
   - 新增 `transferImage(AdminImageTransferDTO request)`：
     - 校验源节点存在性与拓扑关系；
     - 遵循主节点保护铁律：多节点集群禁止将赛题镜像转移至控制面主节点；
     - 在目的节点拉取部署镜像，从源节点移除旧镜像并更新数据库；
   - 新增 `backupImage(AdminImageBackupDTO request)`：
     - `NODE` 模式：向目标节点分发镜像副本，源节点与目标节点均保留该镜像，提升集群容灾能力；
     - `ARCHIVE` 模式：调用 `docker save` 导出离线 tar 镜像文件并落盘至 `data/backups/images/`；
3. **启动期初始化器组件**：
   - 新增 `ContainerImageStartupInitializer`：实现 `ApplicationRunner`，注解 `@Order(30)`，确保在数据库表结构与配置迁移就绪后自动执行镜像探测与集群核验。
4. **Thin Controller 与路由标准化**：
   - `AdminImageController` 新增：
     - `POST /api/v1/admin/images/scan`: 重新扫描加载集群镜像；
     - `POST /api/v1/admin/images/transfer`: 镜像跨节点转移；
     - `POST /api/v1/admin/images/backup`: 镜像容灾备份或离线导出；
   - 所有命令动作统一返回 `ApiResponse<Void>`，使用 `ApiResponseEntity.success(msg)`，配合 `@Syslog` 审计注解。

### 2.2 前端工程改动 (volur-lite-view)
1. **强类型 API 与 DTO 严格对齐**：
   - `src/types/image/index.ts`: 新增 `AdminImageTransferDTO` 与 `AdminImageBackupDTO`；
   - `src/api/image/index.ts`: 补充 `scanAndSyncAdminImagesApi`, `transferAdminImageApi`, `backupAdminImageApi`。
2. **`AdminImageView.vue` 视图升级**：
   - **所在节点分布增强**：在“所在节点分布”表格列中，结合 `clusterNodes` 状态，将节点 IP 映射为包含节点名与角色标签（主节点蓝/从节点绿）的胶囊徽标，并提供悬浮 Tooltip（展示节点名称、IP、角色、Docker 引擎与就绪状态）；
   - **顶栏新增操作**：新增“扫描同步节点镜像”确认按钮，支持管理员一键触发 Docker API 重新探查；
   - **行级操作按钮扩展**：在操作列增加“节点转移”与“备份镜像”按钮；
   - **“镜像节点转移”弹窗 (Transfer Modal)**：
     - 限制源节点为该镜像已部署的节点；
     - 限制目的节点排除源节点，并在多节点赛题场景下保护主节点；
     - 前端表单校验规则与后端 Jakarta 注解 1:1 对齐；
   - **“镜像备份”弹窗 (Backup Modal)**：
     - 支持 RadioGroup 切换“节点容灾副本备份”与“本地离线包归档备份”；
     - 节点容灾模式下自动过滤并标记已部署副本的节点；
   - **视觉规范执行**：主要操作按钮采用亮色模式下的 `#000000` 背景与 `#ffffff` 字体，次要与描述文本采用 `#444444`。
3. **中英文多语言对齐**：
   - `src/locales/zh_CN/admin/images.json` 与 `src/locales/en_US/admin/images.json` 同步补充了转移、备份、扫描及弹窗相关的全部双语词条。

---

## 3. 安全隐患修复与容灾设计
1. **防止假数据误导生产**：彻底废弃 mock 假镜像初始化，杜绝因假镜像导致动态容器启动调度失败的情况；
2. **多节点控制面隔离保护**：转移与拉取操作均禁止将题目运行镜像调度至保护状态的主控制面节点；
3. **高可用容灾与无损备份**：备份功能仅进行副本增量调度，绝不影响源节点的现有运行状态；
4. **命令注入与路径穿越防范**：所有文件名生成经过严格字符清洗（将 `/` 统一转义为 `_`），目录统一锁定在受限的数据归档目录下。

---

## 4. 影响范围与验证结果
- **后端编译**：`mvn clean test-compile -DskipTests` 全平台 12 个模块全部 `BUILD SUCCESS`；
- **前端编译**：`vue-tsc --noEmit` 0 错误通过，`npm run build` Vite 生产打包 100% 构建成功；
- **兼容性**：原有镜像拉取、删除、按需同步功能 100% 保持向前兼容。
