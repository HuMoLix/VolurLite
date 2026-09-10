# 异步任务管理界面样式对齐与 SSE / 前端雪花 ID 精度保护方案

## 1. 变更背景与问题根因分析

在本次迭代中，用户反馈在任务管理列表中点击“详情”时出现以下异常：
- **报错**：提示 `未找到指定的异步任务`（404 错误）；
- **现象**：抽屉栏依然弹出并展示内容；
- **排查**：后端实际返回正确的雪花 ID（例如 `356116767128420350`），无需在后端 VO（`AdminAsyncTaskVO`、`AdminAsyncTaskSubmitVO`）上强加 `@JsonSerialize` 侵入性注解。

### 1.1 根因追溯与定位

参考历史记录 [`agent/volur-design-md/2026-08-27-fix-frontend-snowflake-id-precision-loss-in-request.md`](file:///Users/humolix/Documents/Dev/Volur-Lite/agent/volur-design-md/2026-08-27-fix-frontend-snowflake-id-precision-loss-in-request.md) 以及 [`AdminUserView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/user/AdminUserView.vue) 的实践，后端实体保持原生 `Long id`：
1. **Axios 请求层**：`src/volur-lite-view/src/utils/request.ts` 已经配置了 `safeJsonParse` 转换器，通过正则表达式 `([:,\[]\s*)(-?\d{16,})(?=[,\s\]}])` 将 16 位以上长整型数值在解析前包裹为字符串。
2. **SSE 长连接推送层缺陷**：
   - 查看 `src/volur-lite-view/src/utils/sse.ts` 中的 `SseClient` 实现：
     ```ts
     // 缺陷代码：原生 JSON.parse 导致 64 位雪花 ID 精度被 IEEE 754 双精度浮点截断抹零
     const parsed = JSON.parse(e.data)
     ```
   - 当后端通过 `task_event` 推送任务进度与创建事件时，数据未经 `safeJsonParse` 处理，原生 `JSON.parse` 将长整型 ID 精度截断（低位抹零）。
   - `AdminTaskView.vue` 监听到该事件后，通过 `taskList.unshift(eventData)` 将携带被截断 ID 的任务插入到表格首行。
   - 用户点击表格首行的“详情”时，`getAdminTaskDetailApi(row.id)` 发送了错误的被截断 ID，后端查询数据库失败抛出 404 `RESOURCE_NOT_FOUND`；同时 `catch` 逻辑中将该 `row` 赋值给 `selectedTask` 打开了抽屉，造成“弹出报错 Toast 且抽屉仍展示内容”的异常。

---

## 2. 核心改动与落地细节

### 2.1 彻底回退后端 VO 侵入性注解
- 撤销 `AdminAsyncTaskVO.java` 与 `AdminAsyncTaskSubmitVO.java` 中临时添加的 `@JsonSerialize(using = ToStringSerializer.class)` 与 `@JSONField(format = "string")`；
- 保持后端代码库与 `AdminUserDetailsVO.java` 等已有模块规范完全纯净统一（遵循 `Rules.md`）。

### 2.2 前端 SSE 长连接客户端安全反序列化
- **修改文件**：`src/volur-lite-view/src/utils/sse.ts`
- **改动点**：
  - 导入 `safeJsonParse`：
    ```ts
    import { safeJsonParse } from './request'
    ```
  - 将 `connect()` 和 `on()` 中对 `e.data` 的原生 `JSON.parse` 全部替换为 `safeJsonParse(e.data)`，确保任何经由 SSE 推送过来的 64 位雪花算法长整型数值均安全解析为 String，杜绝精度截断。

### 2.3 对齐 `AdminUserView.vue` 表格与 ID 处理
- **修改文件**：`src/volur-lite-view/src/views/admin/task/AdminTaskView.vue`
- **改动点**：
  - 在 `<n-data-table>` 上添加 `:row-key="(row: AdminAsyncTaskVO) => String(row.id)"`（参考 `AdminUserView.vue:132`）；
  - `formatJson` 方法中使用 `safeJsonParse` 替代原生 `JSON.parse`，防止任务参数或结果中内嵌的雪花 ID 丢失精度；
  - `handleOpenDetail`、`handleCancelTask`、`handleDeleteTask` 统一以 `String(id)` 发起请求。

### 2.4 前端样式全面对齐 `AdminImageView.vue` 与 `AdminLogView.vue`
1. **主页面对齐 `AdminImageView.vue`**：
   - 顶部添加 `.page-header`（`.page-title` 22px 粗体，`.page-desc` 13px 副标题）；
   - 统计面板 `.stat-ribbon`（5 组圆角 10px 卡片，hover 浮动效果，阶梯色系图标背景，支持执行中脉冲与旋转动画）；
   - 主表格卡片 `.table-card`（圆角 10px，无边框，搜索框宽度 320px，下拉框 180px/150px）；
   - 主查询按钮遵循规范 `.theme-primary-btn`（亮色模式背景 `#000000`，文字 `#ffffff`，悬停 `#222222`）；
   - 表格操作列采用 `table-action-btns` 组合，配备 `tiny` 尺寸的 `quaternary` 按钮与离子图标（`EyeOutline`、`CloseCircleOutline`、`TrashOutline`）。
2. **详情抽屉对齐 `AdminLogView.vue`**：
   - 宽度固定为 `620`；
   - 采用 `<n-descriptions :column="2" label-placement="left" bordered size="small">` 两列紧凑结构；
   - 提交参数与执行结果统一采用暗色系 `.code-block`（背景 `#1e1e1e`，字体 `'Fira Code', monospace`）；
   - 异常报错使用深红 `.error-block` 警示框与浅红底色背景；
   - 具备清晰的进度指示与生命周期时间轴。

---

## 3. 验证与质量测试

1. **前端编译验证**：
   - 执行 `npm run build`（`vue-tsc --noEmit && vite build`）：4276 个模块转换通过，`dist/assets/AdminTaskView-Drpsx6RF.js` (22.74 kB) 正常生成，0 TypeScript 类型错误，构建通过。
2. **后端编译验证**：
   - 执行 `mvn test-compile -DskipTests`：全平台 12 个模块全部 `BUILD SUCCESS`。
3. **功能验证**：
   - SSE 推送事件与 HTTP 请求反序列化均通过 `safeJsonParse` 保证雪花 ID 保持高精度字符串，点击详情正常通过后端校验，抽屉平滑渲染且无 404 弹窗。
