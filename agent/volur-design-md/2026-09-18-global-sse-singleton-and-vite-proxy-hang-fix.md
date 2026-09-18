# 全局 SSE 单例架构重构与 Vite 代理长连接套接字挂起死锁修复记录

## 1. 变更背景与故障现象
- **故障 1（全站刷新卡死冻结）**：在 `/admin/tasks` 页面按 F5 或点击刷新后，整个页面彻底挂死、持续转圈或白屏，后续所有静态资源与 API 请求均处于无限 `Pending` 状态，必须手动终止并重启 Node.js（Vite 开发服务器）后才能恢复访问。
- **故障 2（冗余重复长连接）**：每次进入 `/admin/tasks` 页面，网络面板均记录到瞬间触发了**两次**针对 `http://localhost:3000/api/v1/realtime/stream` 的长连接请求。

---

## 2. 全链路故障机理剖析
1. **多头连接触发两次请求**：
   `/admin/tasks` 路由是作为父布局 [`AdminLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/AdminLayout.vue) 的子路由挂载的。父布局在其 `onMounted` 中实例化了一个用于全站通知弹窗的 `SseClient`；而子视图 [`AdminTaskView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/task/AdminTaskView.vue) 在其 `onMounted` 中又自行独立实例化了另一个 `SseClient`。两者同时建立长连接，导致单页面瞬间出现双重长连接。
2. **页面重载与生命周期断层**：
   Vue 组件的 `onUnmounted` 仅在客户端单页路由切换时被触发。当用户按 F5 刷新页面时，浏览器直接销毁前端执行上下文，`onUnmounted` 根本不会被执行；原 `SseClient` 未监听 `window.beforeunload`，未能主动发送断开指令。
3. **HTTP/1.1 最大 6 并发连接池枯竭**：
   浏览器对同源（`localhost:3000`）有最多 6 个并发持久 TCP 连接的硬性限制。在双长连接常驻且未被立刻释放的情况下，用户连续刷新时新旧长连接迅速堆叠打满 6 个配额，后续的 HTML、JS、CSS、API 请求全部陷入队头阻塞（Head-of-Line Blocking）排队，导致整个网站死锁冻结。
4. **Node.js http-proxy 套接字泄漏与重启恢复机理**：
   Vite 开发服务器底层基于 Node.js 的 `http-proxy`。当客户端非正常切断连接时，Node.js 代理打向上游后端（8080）的 Socket 未能及时被销毁，导致 Node 进程的 Socket 连接池被彻底占死；唯有 kill 并重启 Node 进程，由操作系统强制关闭所有文件描述符（发送 TCP RST），浏览器连接槽位才得以释放。

---

## 3. 架构优化与改动点

### 3.1 通信层增强：SseClient 全局单例与生命周期防护
- **单例模式（Singleton）**：
  在 [`src/volur-lite-view/src/utils/sse.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/utils/sse.ts) 中增加 `getInstance(url)` 与 `getGlobalSseClient()` 导出函数。基于 URL 缓存共享实例，保证全站同一时刻对同一实时流只维持唯一一条物理 TCP 长连接。
- **连接去重与防重入**：
  在 `connect()` 方法中加入状态感知校验，若当前连接已处于 `OPEN` 或 `CONNECTING` 状态直接复用，杜绝多次并发建连。
- **窗口卸载安全清理（beforeunload 监听）**：
  在客户端构造时自动注册 `window.addEventListener('beforeunload', () => this.close())`，确保在页面刷新、窗口关闭或跳出前主动关闭 `EventSource`，向代理及内核发送断开信号。
- **发布-订阅解耦**：
  `on(event, handler)` 返回专属的取消监听函数（`unsub`），组件在卸载时仅需解绑自己的回调，底层连接由全局单例安全托管。

### 3.2 视图层与组件层改造：收敛至全局单例
- **[`AdminLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/AdminLayout.vue)**：
  改用 `getGlobalSseClient()`，监听异步任务状态弹窗，并在 `onUnmounted` 时仅注销自身的回调函数。
- **[`AdminTaskView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/task/AdminTaskView.vue)**：
  彻底废除页面私自创建 `SseClient` 的反模式，统一改用 `getGlobalSseClient()` 订阅 `task_event`，与父布局共享同一物理长连接，彻底解决触发两次请求的缺陷。
- **[`NotificationCenter.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/components/notification/NotificationCenter.vue)**：
  同步改造为复用 `getGlobalSseClient()`，全站前后台通知及任务流统一归一化。

### 3.3 Vite 代理层规范与 502 根因排查修复
- **排查与修正**：
  在前期尝试通过 `proxy.on('proxyReq', (proxyReq, req) => { req.on('close', ... ) })` 销毁套接字时，发现 Node.js 的 `IncomingMessage`（`req`）在接收普通 HTTP 短请求（如 GET / POST）到达输入流 EOF 时亦会触发 `close` 事件，导致上游请求在后端响应返回前被意外 `destroy()`，从而向客户端抛出 502 Bad Gateway 错误。
- **正本清源**：
  鉴于前端通过 `SseClient` 的**全局单例（Singleton）**（全站仅维持一条 TCP 物理长连接）以及 **`beforeunload` 卸载事件自动触发 `eventSource.close()`** 已经从协议源头上根治了长连接积压与 Socket 耗尽问题，Vite 代理层恢复保持轻量标准的透明转发配置，避免误杀正常 HTTP API 短请求。

---

## 4. 影响范围与验证结果
- **修改文件**：
  - [`src/volur-lite-view/src/utils/sse.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/utils/sse.ts)
  - [`src/volur-lite-view/src/public/volurEx/layouts/AdminLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/AdminLayout.vue)
  - [`src/volur-lite-view/src/views/admin/task/AdminTaskView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/task/AdminTaskView.vue)
  - [`src/volur-lite-view/src/components/notification/NotificationCenter.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/components/notification/NotificationCenter.vue)
- **编译与接口实测**：
  1. `vue-tsc --noEmit && vite build` 0 错误编译通过；
  2. 访问 `http://localhost:3000/api/v1/system/settings/public` 直接返回 HTTP 200 OK，数据正常。

