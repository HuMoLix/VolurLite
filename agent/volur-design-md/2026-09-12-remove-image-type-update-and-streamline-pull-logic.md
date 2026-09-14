# 镜像管理 updateImageType 移除与 pullImage 逻辑精简

## 1. 变更背景
在中心私有镜像仓库（Docker Registry）为中心的架构模式下：
1. 镜像的类别属性（`imageType`，如 `CHALLENGE` / `BASE`）应根据其仓库名称或系统上下文由平台自动化推导识别，无需通过用户交互界面显式编辑更改，保留原有的 `updateImageType` 接口容易引入人为错误分类或不一致状态；
2. 在通过主控节点拉取镜像并推送到中心私有 Registry 后，所有集群工作节点均可在启动容器实例时通过统一的 Registry 凭证秒级拉取镜像。因此在拉取阶段显式指定单个节点预热（`targetNodeIp`）及指定 `imageType` 已经失去实际意义，且增加界面复杂度与前后端交互负担；
3. 需要从前后端彻底清理 `updateImageType` 接口与相关表单、DTO 及操作列，精简 `pullImage` 任务执行流程。

---

## 2. 核心改动点

### 2.1 后端代码与接口清理
1. **删除 `AdminImageTypeUpdateDTO.java`**：
   彻底移除 [`AdminImageTypeUpdateDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/domain/dto/AdminImageTypeUpdateDTO.java) 文件。
2. **移除 `AdminImageService` 与 `AdminImageServiceImpl` 中的 `updateImageType`**：
   - 从 [`AdminImageService`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/service/admin/AdminImageService.java) 接口中移除 `void updateImageType(Long id, AdminImageTypeUpdateDTO request);` 声明；
   - 从 [`AdminImageServiceImpl`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/service/admin/impl/AdminImageServiceImpl.java) 中彻底删除 `updateImageType` 方法实现与相关 DTO 导入。
3. **移除 `AdminImageController` 中的路由**：
   删除 `PUT /api/v1/admin/images/{id}/type` 控制器端点方法 `updateImageType`。
4. **精简 `AdminImagePullRequestDTO.java`**：
   - 移除了 `targetNodeIp` 字段；
   - 移除了 `imageType` 字段；
   - 仅保留核心必填字段 `image: String`。
5. **重构并精简 `AdminImageServiceImpl.pullImage` 函数逻辑**：
   - 移除从请求获取 `request.getImageType()` 的依赖，对新创建的 `ClusterImage` 根据镜像仓储名称自动化识别分类（含有 `ctf` / `challenge` / `pwn` / `web` 则设为 `CHALLENGE`，否则识别为 `BASE`）；
   - 彻底删除原第 4 步“若指定了目标预热节点，则按需同步该节点”的逻辑（包括 `ensureImageOnNode` 冗余触发）；
   - 异步任务流程更为紧凑：连接主控节点准备 (10%) -> 执行 Docker Pull 拉取源镜像 (30%) -> 打标并推送到中心私有 Registry (60%) -> 解析 Registry Manifest (80%) -> 同步更新数据库镜像元数据与拓扑 (90%) -> 镜像拉取与中心入库完成 (100%)。

### 2.2 前端代码与界面同步调整
1. **类型与 API 清理**：
   - 在 [`src/types/image/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/types/image/index.ts) 中精简 `AdminImagePullRequestDTO` 为仅包含 `image: string`，并移除 `AdminImageTypeUpdateDTO` 接口；
   - 在 [`src/api/image/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/api/image/index.ts) 中彻底移除 `updateAdminImageTypeApi` 请求函数。
2. **视图 [`AdminImageView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/image/AdminImageView.vue) 更新**：
   - 移除表格列定义中的“镜像类别”列（`imageType`），不再渲染可点击修改类别的 Tag 及编辑按钮；
   - 彻底删除“修改镜像类别”模态弹窗（`Edit Category Modal`）的模板及全部状态控制逻辑（`showEditTypeModal`, `editTypeForm`, `editTypeRules`, `openEditTypeModal`, `handleSubmitEditType` 等）；
   - 清除拉取新镜像弹窗（`pullModal`）中的“镜像类别”选择项；
   - 清理相关的未引用图标组件（`CreateOutline`）与未引用的 CSS 样式类（`.category-cell`）。

---

## 3. 安全隐患修复与质量保证
1. **防止误操作破坏镜像用途**：移除了管理员手动改写内部镜像类型的接口，避免由于类别误改导致赛题实例调度异常或环境错乱；
2. **避免多节点拉取竞争与不必要开销**：去除了拉取时的单节点预热逻辑，完全交由中心 Registry 托管按需分发，避免冗余节点带宽损耗；
3. **类型安全与构建一致性**：前后端 TypeScript 与 Java 强类型完全 1:1 对齐，删除无用字段后双方编译零警告、零报错。

---

## 4. 影响范围
- **受影响后端文件**：
  - `com.humolix.volur.container.domain.dto.AdminImagePullRequestDTO`
  - `com.humolix.volur.container.domain.dto.AdminImageTypeUpdateDTO` (已删除)
  - `com.humolix.volur.container.service.admin.AdminImageService`
  - `com.humolix.volur.container.service.admin.impl.AdminImageServiceImpl`
  - `com.humolix.volur.container.controller.v1.admin.AdminImageController`
- **受影响前端文件**：
  - `src/types/image/index.ts`
  - `src/api/image/index.ts`
  - `src/views/admin/image/AdminImageView.vue`
- **回归验证**：
  - `mvn compile -pl volur-container -DskipTests` -> SUCCESS
  - `mvn test-compile` (平台全模块) -> SUCCESS
  - `npm run build` (前端 Vite + vue-tsc) -> SUCCESS
