# 异步任务统计请求范围收敛至任务管理视图施工记录

## 1. 变更背景

此前在后台主布局框架 [`AdminLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/layout/AdminLayout.vue) 中，为了在左侧菜单栏“任务管理 (AdminTasks)”项旁边展示正在运行与排队任务的数字角标（Badge），在页面组件初次挂载（`onMounted`）以及每次接收到 SSE 任务事件时均会发起一次 `/api/v1/admin/tasks/statistics` 异步请求。

这导致管理员在后台任意页面（如镜像管理、用户中心、竞赛管理、系统设置等）进行刷新操作时，均会无差别触发一次任务统计接口调用，增加了非必要的网络开销与后端查询压力。根据最新规范，该任务统计请求应当严格收敛至【任务管理 (AdminTasks)】页面本身（[`AdminTaskView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/task/AdminTaskView.vue)）。

---

## 2. 具体改动点

### 前端工程改造 (`src/volur-lite-view`)

1. **移除全局布局中的任务统计请求 (`src/layout/AdminLayout.vue`)**：
   - 移除 `import { getAdminTaskStatisticsApi } from '@/api/task'`；
   - 彻底删除 `runningTaskCount` 响应式变量与 `updateTaskBadge()` 方法；
   - 移除 `onMounted()` 钩子内以及 SSE 长连接 `task_event` 事件回调中的 `updateTaskBadge()` 调用；
   - 保持 SSE 完成/失败 Toast 提示及 `volur:async-task-completed` / `volur:async-task-failed` 跨组件 CustomEvent 广播功能不受任何影响。

2. **侧边栏菜单项精简 (`src/layout/AdminLayout.vue`)**：
   - 移除“任务管理”菜单项上的 `extra: () => runningTaskCount.value > 0 ? h(NBadge, ...) : null` 渲染函数；
   - 移除 Naive UI 中多余的 `NBadge` 组件导入，并清理无用的 `ref` 导入。

3. **保留并明确单一请求源 (`src/views/admin/task/AdminTaskView.vue`)**：
   - 任务统计接口 `/api/v1/admin/tasks/statistics` 仅在管理员访问【任务管理】页面（`AdminTaskView.vue`）加载/刷新/操作时触发（`fetchStatistics()`），实现了页面级高内聚，避免全站级广播请求。

---

## 3. 验证与影响范围

- **前端打包编译**：执行 `npm run build`，Vite 打包与 TypeScript 严格模式类型校验 100% 通过（`built in 4.52s`）；
- **行为影响**：
  - 管理员在后台任意子页面刷新时，不再触发 `/api/v1/admin/tasks/statistics` 请求；
  - 进入【任务管理】页面时，任务总数、运行中、待处理、失败数统计卡片仍保持正常请求与实时展示。
