# 施工记录：账号选择视图 (AccountChooserView) 移除账号全交互 Scrim 遮罩与延迟对齐

## 1. 变更背景与需求目标

在 Volur Lite 新一代前端（`volur-lite-view-NEXT`）账号选择页面（`/v1/signin/accountchooser`）中，原“移除账号”相关操作（包括进入移除模式、单项删除、退出移除模式）缺乏全局 Scrim 遮罩与时钟延迟控制：
1. **“移除账号”按钮直接同步切换模式**：点击后直接执行 `isRemoveMode = true`，缺少 Google 风格的 1000ms 最小处理等待与顶层进度反馈；
2. **列表项中的“删除”（垃圾桶）按钮无遮罩延迟**：点击后直接发起删除并在接口响应后瞬间剔除数据，缺少平滑的卡片 Scrim 遮罩和 1000ms 时序保护；
3. **“完成”按钮直接同步退出模式**：点击后直接执行 `isRemoveMode = false`，未能提供统一的平滑过渡体验。

---

## 2. 核心架构设计与竞态规避

### 2.1 卡片代理点击防双发与事件过滤
在 [`AuthLayout.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/layouts/auth/AuthLayout.vue) 中，卡片全局委托点击函数 `handleCardActionClick` 对带有 `:isScrim="true"` 的交互元素进行监听。
为避免全局代理触发与组件内部显式调用的异步时钟发生竞争，将以下类名显式纳入排他列表：
- `.btn-remove-account`（“移除账号”按钮）；
- `.remove-icon-btn`（列表项垃圾桶删除按钮）。
确保上述按钮的 Scrim 遮罩与延迟由 [`AccountChooserView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/AccountChooserView.vue) 内部方法精确管控。

### 2.2 异步时钟与视图状态流转协同
- **进入移除模式 (`handleEnterRemoveMode`)**：
  点击后执行 `await signinFlowStore.triggerScrim(undefined, 1000)`，保持 1000ms 的全局 Scrim 遮罩与线性进度条，遮罩结束后平滑切换为 `isRemoveMode = true`；
- **执行账号删除 (`handleRemove`)**：
  采用 `await signinFlowStore.withLoading(action, 1000)` 包裹 `removeAccountChooserApi` 调用与本地缓存清退逻辑，维持满 1000ms 的卡片加载与遮罩态；若全部账号均被移除，自动平滑退出移除模式；
- **完成退出移除模式 (`handleExitRemoveMode`)**：
  点击后执行 `await signinFlowStore.triggerScrim(undefined, 1000)`，维持 1000ms 遮罩延迟后退出移除模式（`isRemoveMode = false`），恢复常规账号选择态。

---

## 3. 具体修改实施

### 3.1 [`AccountChooserView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/AccountChooserView.vue)
1. **模板属性绑定**：
   - 为 `.btn-remove-account` 增加 `:isScrim="true"`，并绑定 `@click="handleEnterRemoveMode"`；
   - 为 `.remove-icon-btn` 增加 `:isScrim="true"`；
   - 为 `.btn-next-step`（“完成”按钮）增加 `:isScrim="true"`，并绑定 `@click="handleExitRemoveMode"`；
2. **逻辑方法重构**：
   - 重构 `handleRemove`，使用 `signinFlowStore.withLoading(..., 1000)` 显式驱动并包裹 API 请求与数据剔除；
   - 新增 `handleEnterRemoveMode` 与 `handleExitRemoveMode` 两个异步处理函数；
3. **交互样式加固**：
   - 为 `.btn-next-step` 补齐 `cursor: pointer` 与 `&:disabled { cursor: not-allowed; opacity: 0.6; }`（含深色模式）；
   - 为 `.remove-icon-btn` 补齐 `&:disabled { cursor: not-allowed; opacity: 0.6; }`（含深色模式）。

### 3.2 [`AuthLayout.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/layouts/auth/AuthLayout.vue)
- 在 `handleCardActionClick` 排除类名判定中追加 `.btn-remove-account` 与 `.remove-icon-btn`，防止产生并发无意义的 `triggerScrim` 穿透。

---

## 4. 验证与质量保证

1. **构建与类型检查**：
   执行 `npm run build`（`vue-tsc --noEmit && vite build`），TypeScript 0 错误通过，生产打包完成（耗时 6.59s）。
2. **规范遵守**：
   严格恪守全代码零行内注释铁律（Zero Comments Rule），改动逻辑清晰自解释。
