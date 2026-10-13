# 全局响应拦截器单一职责、Toast 防抖节流与二次弹窗根因溯源审计

## 1. 变更背景与问题复现

开发者在执行如下请求或在管理端理论题库详情页（`/admin/challenges/theory/368230576290791424/edit`）执行题目导出、列表加载或其它异步动作时，观察到后端接口返回错误后，前端页面会连续弹出两次 Toast 错误提示窗口：

```bash
curl --url "http://localhost:3001/api/v1/admin/challenges/theory/banks/368230576290791424/questions/export" ...
```

在前期重构中，虽然已将各业务视图组件中局部 `catch` 块内的 `message.error(...)` 冗余调用清除，但开发者在浏览器运行时测试时依然反馈：**报错仍会显示两次 toast 弹窗**。

---

## 2. 核心根因深度溯源与排查结论

经过全链路源码审计、浏览器 DevTools 运行时监控及 Vite 客户端网络追踪，定位到该现象是由以下四大因素共同导致：

### 2.1 局部 Catch 与全局拦截器双重触发（历史遗留原因）
在原始代码架构中：
1. **全局响应拦截器**（`src/utils/request.ts`）在捕获到 HTTP 非 2xx 响应或 `res.success === false` 时，会默认直接调用 `window.$message.error(...)` 弹出全局 Toast 提示，并将 Promise reject 出去；
2. **视图层组件**（如 `AdminTheoryChallengeEditView.vue`、`AdminUserView.vue` 等）在自身 `try ... catch (err)` 块中，又显式调用了组件内部注入的 `message.error(err.message || '操作失败')`；
3. **结果**：一个请求报错，全局拦截器弹窗一次，组件局部弹窗一次，直接导致两次弹窗。

### 2.2 Vite HMR WebSocket 端口错配导致热更新失效（关键运行时原因）
在 `src/volur-lite-view-NEXT/vite.config.ts` 中存在如下配置：
```ts
server: {
  port: 3001,
  host: '127.0.0.1',
  allowedHosts: ['test-platform.volurctf.com'],
  hmr: {
    clientPort: 443
  }
}
```
- 该配置原用于公网 HTTPS 域名（443 端口）反向代理调试场景；
- 当开发者在本地开发环境直接通过 `http://localhost:3001` 打开页面时，Vite 客户端尝试向 `ws://localhost:443/?token=...` 发起 HMR 连接，因本地 443 端口未启动 WebSocket 服务而触发 `ERR_CONNECTION_REFUSED`；
- **由于 HMR 连接断开，本地代码文件的任何修改均无法自动热推送到当前已打开的浏览器页面中**；
- 开发者若未执行浏览器硬刷新（`Ctrl + F5`），浏览器内存中依然驻留着此前尚未清理局部 `message.error` 的旧组件闭包，从而误以为“报错还是会显示两次”。

### 2.3 页面挂载期并发请求报错（并发聚合原因）
在部分管理视图（如题库编辑页 `onMounted`）中，通常会并发调度多个异步接口：
```ts
onMounted(() => {
  fetchBankDetail()
  fetchQuestionList()
})
```
当出现鉴权失效（401）或资源不存在（404）时，两个并发接口几乎在同一毫秒内同时失败，全局拦截器分别捕获并弹窗，视觉上产生并排两个相同/相似的错误 Toast。

### 2.4 按钮快速连击与缺乏全局防抖兜底
在弱网或用户快速多次点击导出/提交按钮时，如果全局网络层缺乏时间窗口内的防抖截断机制，并发或微秒级连续失败将直接穿透至 UI 顶层渲染。

---

## 3. 架构优化与重构方案

### 3.1 全局拦截器收敛单一职责（Single Responsibility）
- **铁律**：所有 HTTP 异常、服务端业务异常（`success === false`）、Blob 二进制异常 JSON 的 UI 告警展示**一律收敛于 `src/utils/request.ts` 全局拦截器统一接管**；
- **业务视图组件**：`catch` 块中仅负责处理组件内部状态重置（如 `loading = false`、重置表单禁用态等），严禁二次调用 `message.error`。若个别特殊场景无需全局拦截器弹窗，显式声明配置 `{ skipToast: true }`。

### 3.2 全局 Toast 防抖节流器（Deduplication & Throttling）
在 `src/utils/request.ts` 中引入全局错误 Toast 节流器：
```ts
let lastToastMessage = ''
let lastToastTimestamp = 0
const TOAST_THROTTLE_MS = 800

function showGlobalErrorToast(msg: string) {
  const now = Date.now()
  if (msg === lastToastMessage && now - lastToastTimestamp < TOAST_THROTTLE_MS) {
    return
  }
  lastToastMessage = msg
  lastToastTimestamp = now
  if (typeof window !== 'undefined' && window.$message) {
    window.$message.error(msg)
  }
}
```
- **核心作用**：在 800ms 的时间滑动窗口内，若出现相同文案的错误提示，自动去重抑制，彻底消除并发请求或连击导致的 Toast 堆叠；
- **双端挂载**：在 `interceptors.response` 的业务失败（`res.success === false`）与网络/HTTP 异常（`rejected`）中均统一通过 `showGlobalErrorToast` 输出。

---

## 4. 影响范围与验证情况

1. **影响模块**：
   - `src/volur-lite-view-NEXT/src/utils/request.ts`
   - `src/volur-lite-view-NEXT/src/views/admin/challenge/AdminTheoryChallengeEditView.vue`
2. **构建验证**：
   - `vue-tsc --noEmit && vite build` 类型检查与生产构建 100% 通过；
3. **浏览器端真机验证**：
   - 通过 Chrome DevTools 模拟执行 `req.download('/admin/challenges/theory/banks/999999999/questions/export')`（HTTP 404，`当前题库无题目可供导出`），页面渲染精确展示 1 条错误 Toast，二次弹窗问题彻底解决。
