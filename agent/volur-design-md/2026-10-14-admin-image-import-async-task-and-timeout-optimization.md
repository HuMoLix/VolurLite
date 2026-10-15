# 管理端镜像导入异步任务驱动与大文件防超时优化施工记录

## 1. 变更背景与核心痛点

在 Volur Lite 容器与镜像管理模块中，管理员可通过 Web 界面导入 Docker 离线镜像归档（`.tar`、`.tar.gz`、`.tgz`），系统在主控节点执行 `docker load` 并自动打标推送到中心私有 Registry。

然而，在实际生产和测试环境中，Docker 镜像归档体积通常达到数百兆字节甚至数千兆字节（例如 500MB ~ 2GB+）。此前前端实现存在以下核心缺陷：
1. **全局 Axios 15 秒超时误判**：前端全局请求配置了 `timeout: 15000`（15秒）。对于大体积镜像归档，客户端通过 HTTP 上传数据耗时远超 15 秒，导致 Axios 提前抛出 `timeout of 15000ms exceeded` 错误，前端误判为请求失败中断操作；
2. **缺乏上传进度反馈**：点击确认导入后仅呈现普通按钮转圈态，用户无法获知已上传大小、总大小及上传百分比，大文件传输体验呈现“假死”感；
3. **未跟踪服务端异步任务生命周期**：服务端接口已将耗时的解压加载（`docker load`）与私有仓库推送（`docker push`）交由系统异步任务引擎（`IMAGE_IMPORT`）在后台处理并立即返回任务凭证（`AdminAsyncTaskSubmitVO`）。但前端在上传完成后直接关闭弹窗并刷新列表，此时后台解压尚未完成，镜像并未立即出现在资产列表中，且用户无法得知解压进度与执行报错信息。

为彻底解决上述问题，本次施工对 `src/volur-lite-view-NEXT` 的镜像导入链路进行了全方位异步任务驱动化与大文件防超时重构。

---

## 2. 施工改动与实现细节

### 2.1 API 请求层优化 (`src/volur-lite-view-NEXT/src/api/image.ts`)
- 将 `importAdminImageApi` 方法入参拓展为可选 `config?: AxiosRequestConfig`；
- 显式配置 `timeout: 0`，覆盖全局 15 秒超时限制，启用大文件全量无超时传输；
- 支持透传 `onUploadProgress` 进度回调与 `signal: AbortSignal` 取消信号。

### 2.2 任务类型对齐 (`src/volur-lite-view-NEXT/src/types/task.ts`)
- 在前端 `AsyncTaskType` 联合类型中补充 `'IMAGE_IMPORT'` 枚举值，100% 严格对齐后端领域模型。

### 2.3 视图组件状态机重构 (`src/volur-lite-view-NEXT/src/views/admin/image/AdminImageView.vue`)

1. **三阶段导入状态机 (`importPhase: 'select' | 'uploading' | 'task'`)**：
   - **`select`（文件选择与参数配置）**：支持文件点击选择与拖拽（Drag & Drop）投放（校验 `.tar`、`.tar.gz`、`.tgz` 后缀），支持自定义仓库名称与标签；
   - **`uploading`（大文件无超时传输）**：展示上传动画、文件尺寸、已上传字节数/总字节数、百分比进度条，并提供【取消上传】操作（通过 `AbortController` 优雅中止网络流）；
   - **`task`（服务端异步解压与入库执行）**：文件上传完毕后自动过渡至异步任务视图，展示任务名称、任务 ID、状态徽章、阶段进度条以及由主控节点实时上报的进度信息（如 `docker load` 阶段、私有 Registry 推送阶段、Manifest 同步阶段等）。

2. **异步任务轮询与实时驱动机制**：
   - 上传成功后立即启动 `startPollingTask(taskId)`，以 1.5 秒间隔调用 `getAdminTaskDetailApi`；
   - 任务成功时：弹出 Naive UI 成功提示，自动刷新镜像资产列表及统计卡片，引导用户完成；
   - 任务失败时：展示失败原因与红标提示，提供【重新上传】与【关闭】通道；
   - 任务取消时：正确清理状态并给出友好反馈。

3. **后台运行机制与常驻悬浮进度条**：
   - 在任务执行中（`PENDING` / `RUNNING`），用户可随时点击【后台运行】收起弹窗；
   - 弹窗收起后，表格上方常驻展示轻量级蓝调动态进度条，展示任务名称、ID、状态描述与百分比，并提供【查看进度】按钮以便随时重新唤起弹窗；
   - 即使弹窗已收起，后台轮询依然持续工作，任务执行成功或失败后自动弹出 Toast 并静默刷新数据。

4. **生命周期防护与内存防泄漏**：
   - 在 `onUnmounted` 钩子中主动执行 `stopTaskPolling()` 与 `uploadAbortController.abort()`，组件销毁时彻底清理定时器与网络请求，杜绝悬空操作。

---

## 3. 设计规范与色彩约束合规性

- **Google Material Design 3 色彩体系**：
  - 上传与进行中进度条采用 Google Blue（亮色 `#1a73e8` / 暗色 `#8ab4f8`）；
  - 成功状态采用 Google Green（`#34a853` / `#81c995`）；
  - 失败状态采用 Google Red（`#ea4335` / `#f28b82`）；
  - 排队状态采用 Google Yellow（`#fbbc04` / `#fdd663`）；
  - 描述与尺寸文本遵循 Google Gray 灰阶规范，无粗暴 `#000000` 正文；
- **纯粹代码与零注释铁律**：所有修改涉及的代码文件（`.vue`、`.ts`）中严禁且未引入任何冗余注释，逻辑全部自解释并外置于本文档；
- **工程编译验证**：通过 `vue-tsc --noEmit && vite build` 生产构建全量核验，零类型错误，零语法警告。
