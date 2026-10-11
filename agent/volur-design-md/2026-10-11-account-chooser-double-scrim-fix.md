# 账号选择器切换账号重复遮罩层问题修复

## 1. 变更背景与问题定位

在 [/v1/signin/accountchooser](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/AccountChooserView.vue) 页面中，用户点击账号列表项切换账号时，界面会出现明显的“两次遮罩”卡顿现象（遮罩层淡入显示 1 秒 ➔ 淡出消失 ➔ 再次淡入显示 1 秒 ➔ 路由跳转）。

经审计分析，根本原因存在于 [src/views/auth/signin/AccountChooserView.vue](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/AccountChooserView.vue) 的 `handleSelect` 方法中：
1. **第一次冗余遮罩**：在赋值账号标识符之前，无条件先行执行了 `await signinFlowStore.triggerScrim(undefined, 1000)`，导致 `loading` 被置为 `true`，全屏遮罩层亮起 1000ms 后关闭；
2. **第二次业务遮罩**：
   - 若账号为已登录状态（`sessionStatus === 'ACTIVE'`），进入分支又执行了一次 `await signinFlowStore.triggerScrim(undefined, 1000)` 准备重定向；
   - 若账号需进入重新认证流程（`sessionStatus !== 'ACTIVE'`），调用 `signinFlowStore.selectAccount(acc.identifier)`，该接口方法底层通过 `withLoading` 包裹，请求期间自动将 `loading` 置为 `true` 并保持至少 1000ms（`MIN_LOADING_DURATION_MS`），再次触发遮罩层。

因此，无论选择哪种状态的账号，都会连续触发两次独立的遮罩动画。

---

## 2. 核心改动点

在 [src/views/auth/signin/AccountChooserView.vue](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/AccountChooserView.vue) 的 `handleSelect` 处理逻辑中：
- **移除了前置无条件调用的 `await signinFlowStore.triggerScrim(undefined, 1000)`**；
- `signinFlowStore.setIdentifier(...)` 仅为客户端内存同步赋值，无需任何加载等待；
- 需认证账号由 `signinFlowStore.selectAccount(...)` 底层的 `withLoading` 统一管理单次平滑请求遮罩；
- 已处于登录态的活跃账号保留分支内部的 `triggerScrim`，实现跳转前的平滑过渡。

---

## 3. 影响范围与规范审计

- **受影响文件**：
  - [src/views/auth/signin/AccountChooserView.vue](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/AccountChooserView.vue)
- **代码零注释规范**：
  - 严格遵守纯粹代码与零注释铁律，未在任何 Vue/TS 代码中加入行内注释或冗余说明。
