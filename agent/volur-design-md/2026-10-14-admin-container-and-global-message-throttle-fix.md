# 全局 Naive UI 消息去重代理与管理端容器视图二次 Toast 根治重构方案

## 1. 变更背景与问题现象

开发者在管理端容器管理页面（`/admin/containers`）或调用如下接口时：
```bash
curl --url "https://platform.volurctf.com/api/v1/admin/containers?page=1&size=10" ...
```
若服务端返回异常（例如 HTTP 500，`"message": "服务器内部故障，请联系管理员"`）：
```json
{
  "status": 500,
  "errorCode": 0,
  "success": false,
  "message": "服务器内部故障，请联系管理员",
  "data": null,
  "timestamp": "1791913248145",
  "path": "/api/v1/admin/containers"
}
```
前端页面会连续弹出两个视觉上重叠的 Toast 弹窗，且均显示 `"服务器内部故障，请联系管理员"`。

---

## 2. 根因剖析

该问题由以下两个层面的架构与交互机制共同导致：

1. **全局响应拦截器先行弹窗抛出异常**：
   - 在 `src/utils/request.ts` 的 Axios 响应拦截器（`interceptors.response`）中，当 HTTP 状态码为 500 时，全局拦截器自动提取服务端错误信息并调用 `window.$message.error(errorMsg)` 弹出第 1 个 Toast；
   - 拦截器随后通过 `return Promise.reject(normalizedError)` 将异常向下游抛出。
2. **视图层组件 catch 块二次调用 message.error**：
   - 在 `AdminContainerView.vue`（以及 `AdminUserView.vue` 等）的 `fetchContainers` 方法中，存在如下代码：
     ```ts
     } catch (err: any) {
       message.error(err.message || '获取容器实例失败')
     }
     ```
   - 由于捕获到了被全局拦截器抛出的异常对象，其 `err.message` 正是服务端返回的 `"服务器内部故障，请联系管理员"`，导致组件局部通过 Naive UI 的 `message.error(...)` 弹出了第 2 个完全相同的 Toast。
3. **全局节流器仅局限于 request.ts，未覆盖组件注入实例**：
   - 历史方案在 `request.ts` 中定义的 `showGlobalErrorToast` 仅为局部私有节流器，业务组件直接通过 `useMessage()` 获取 Naive UI 实例，绕过了该节流器，导致节流防护穿透。

---

## 3. 重构与修复方案

本项目采取“全局底座防抖拦截 + 业务组件单一职责收敛”的纵深防御架构：

### 3.1 全局底座：在 NaiveProvider 中实现统一消息防抖代理
在 `src/components/common/NaiveProvider.vue` 中：
- 使用 `Proxy` 拦截原始 Naive UI `useMessage()` 的 `error` 方法；
- 维护全局滑动时间窗口（`THROTTLE_MS = 1000`）与 `recentErrorMessages` 状态映射表；
- 在 1 秒内若接收到相同错误内容（无论来源是 `window.$message.error` 还是各组件内的 `useMessage().error`），自动拦截并返回静默 `MessageReactive` 虚拟对象，彻底抑制重复弹窗；
- 通过 `provide('n-message-api', throttledMessage)` 覆盖子树依赖注入，使得所有通过 `useMessage()` 注入的组件无感接入防抖代理。

### 3.2 业务组件：遵循单一职责原则，清理冗余 catch 弹窗
在 `src/views/admin/container/AdminContainerView.vue` 及 `src/views/admin/AdminUserView.vue` 中：
- 严格遵循单一职责铁律：**API 级别的错误提示全权由 `src/utils/request.ts` 全局拦截器接管**；
- 业务组件内的 `catch` 块中彻底移除冗余的 `message.error` 调用，仅保留 `loading.value = false`、`submitting.value = false` 等纯净的状态复位逻辑。

---

## 4. 影响范围与验证结果

1. **影响模块**：
   - `src/volur-lite-view-NEXT/src/components/common/NaiveProvider.vue`
   - `src/volur-lite-view-NEXT/src/views/admin/container/AdminContainerView.vue`
   - `src/volur-lite-view-NEXT/src/views/admin/AdminUserView.vue`
2. **构建验证**：
   - 执行 `vue-tsc --noEmit && vite build`，生产环境构建编译 100% 通过（耗时 14.33s），零类型错误与告警；
3. **运行时效果**：
   - 当任何接口返回 500 或业务错误时，全链路只会触发一次唯一的错误 Toast，彻底杜绝双弹窗现象。
