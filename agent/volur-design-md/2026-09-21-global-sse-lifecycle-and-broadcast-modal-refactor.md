# 全局 SSE 实时通信生命周期重构与系统居中广播弹窗落地施工归档

- **执行日期**：2026-09-21
- **施工模块**：`volur-lite-view` (前端实时流与全局通知/广播交互)
- **文档类型**：全局 SSE 生命周期管理与居中广播弹窗施工归档

---

## 1. 变更背景与核心动因

1. **居中阻断弹窗公告无响应问题**：
   - 管理员在后台公告管理（[`AdminBroadcastView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/broadcast/AdminBroadcastView.vue)）中发布展示形式为 `MODAL`（居中阻断弹窗）的系统公告后，虽然接口调用成功且后端通过 SSE 向全站广播了该事件，但前端全站没有任何组件注册对 `announcement` 或 `promotion` 事件的监听，且没有用于承载居中弹窗展示的全局 UI 组件，导致公告发布后前端没有任何弹窗响应。

2. **SSE 连接分散与生命周期割裂**：
   - 既有架构中，`NotificationCenter.vue`、`AdminLayout.vue` 与 `AdminTaskView.vue` 各自独立调用 `sse.connect()` 并注册监听器；
   - 在未登录状态下若建立连接，会产生无效且无鉴权 Token 的匿名长连接；登录后由于旧连接未销毁重连，导致无法接收定向单播推送；而在后台管理视图下由于未挂载 `NotificationCenter`，站内信推送在后台彻底静默。

---

## 2. 核心改动点与实现明细

### 2.1 全局 SSE 事件收敛与状态中心 ([`notification.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/store/modules/notification.ts))
- 将 `useNotificationStore` 重构为全站实时通信与全局通知/广播中枢：
  - **`initSse()`**：统一判定用户登录态，仅在 `userStore.isLoggedIn` 为 `true` 时调用 `sse.connect()` 建立长连接，并在单例中统一定向监听四大核心业务事件：
    - `notification`：处理个人站内信，更新未读计数，触发轻量全局 Message 提示，并通过 `volur:new-notification` 派发全局事件；
    - `announcement` 与 `promotion`：接收全局系统公告与活动推广，若 `displayType === 'MODAL'` 且未在当前会话被关闭过（或声明了 `forcePopup`），自动拉起全局居中弹窗；若为 `BANNER`、`TOAST`、`DRAWER` 则分别通过 `$notification`、`$message` 对应降级呈现；
    - `task_event`：针对管理员账号（`userStore.isAdmin`）实时推送异步任务成功/失败浮窗，并分发 `volur:async-task-completed` 与 `volur:async-task-failed`；
  - **`destroySse()`**：在用户登出或会话过期时，主动注销全部事件监听、调用 `sse.close()` 释放网络套接字，并重置私有状态。

### 2.2 自研平台级居中广播弹窗组件 ([`VolurBroadcastModal.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/components/VolurBroadcastModal.vue))
- 遵循 `volurEx` 自研组件库规范，开发并集中导出 `VolurBroadcastModal`：
  - 基于 Naive UI `NModal`（`preset="card"`）构建，居中遮罩展示；
  - **视觉设计铁律**：亮色模式下主要操作按钮背景为 `#000000`、文字 `#ffffff`，描述文字统一为 `#444444`；暗色模式下完美融入 Obsidian & Cyber Slate 色彩层级体系；
  - 支持封面图自适应渲染、正文多行排版保护、分类与优先级 Tag（`ANNOUNCEMENT` / `PROMOTION` / `P1-P10`）、发布人与发布时间展示以及外链跳转卡片；
  - 导出于 [`components/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/components/index.ts) 与 [`volurEx/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/index.ts)。

### 2.3 全局生命周期注入与登录态响应 ([`App.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/App.vue) & [`user.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/store/modules/user.ts))
- **`App.vue`**：
  - 在 `<naive-provider>` 顶层常驻挂载 `<VolurBroadcastModal />`，确保全站所有路由下均可捕获并弹出居中公告；
  - 在 `onMounted` 钩子中，若用户带着登录态进入网页（`userStore.isLoggedIn`），立即执行 `notificationStore.initSse()`；
  - 通过 `watch(() => userStore.isLoggedIn)` 进行全局反应式监听，登录态变化时自动联动 `initSse` 与 `destroySse`。
- **`user.ts`**：
  - 在 `login()` 方法填充完用户概要后显式触发 `notificationStore.initSse()`；
  - 在 `logout()` 清理 LocalStorage 后显式触发 `notificationStore.destroySse()`。

### 2.4 组件冗余清理与去重收敛 ([`NotificationCenter.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/components/notification/NotificationCenter.vue) & [`AdminLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/AdminLayout.vue))
- **`NotificationCenter.vue`**：移除组件内部私有注册的 SSE 监听与重复建连逻辑，未读数统一读取并同步 `notificationStore.unreadCount`，通过监听 `volur:new-notification` 自适应刷新列表；
- **`AdminLayout.vue`**：移除局部重复的 `task_event` 监听与重复通知弹窗，由全局 store 统一通知并派发事件给具体视图。

---

## 3. 安全隐患防范与影响范围

1. **连接泄漏与资源节约**：
   - 彻底消除了游客未登录时盲目建立长连接占用的服务端并发文件描述符；
   - 规避了“未登录建连 -> 登录后未更新 Token -> 接收不到个人信”的严重鉴权脱节隐患。
2. **防重复轰炸机制**：
   - 居中弹窗结合 `sessionStorage` 记录已关闭广播 ID，普通公告用户关闭后刷新页面不会重复弹窗；重要公告带 `forcePopup: true` 时仍确保强制弹出。

---

## 4. 验证与审计结论

- **TypeScript 严格类型检查与 Vite 构建**：
  - 执行 `npm run build`（`vue-tsc --noEmit && vite build`），全工程无任何类型报错或警告，编译构建 100% 成功通过。
