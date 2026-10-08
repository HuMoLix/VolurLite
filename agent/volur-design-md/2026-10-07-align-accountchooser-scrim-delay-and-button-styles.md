# AccountChooser 与 Identifier 转场延迟、遮罩层及按钮样式对齐规范

## 1. 变更背景与问题溯源

在 Volur Next-gen 前端（`src/volur-lite-view-NEXT`）中，登录认证流（Sign-in Flow）全面遵循 Google Workspace / Material Design 规范体系。根据系统规范设计，所有跨视图的前端路由切换与流程推进动作，均应配合 `triggerScrim(undefined, 1000)` 唤起全局与卡片 Scrim 遮罩层、激活卡片顶部线性进度条（Progress Bar），并保证至少 1000ms 的平滑转场动画，避免页面瞬间切换所带来的视觉突兀感。

但在前期实现中，存在以下几处交互与样式缺陷：
1. **IdentifierView.vue “选择账号”按钮缺失延迟与遮罩层**：
   在 `IdentifierView.vue` 中，“选择账号”按钮（`t('auth.choose_account')`）触发的 `handleBackToChooser` 直接调用了同步的 `router.push`，未调用 `triggerScrim` 亦未配置 `:isScrim="true"`，点击后瞬间切页，缺乏 Google 质感的加载阻断与平滑转场动画。
2. **AccountChooserView.vue 列表项缺失延迟与遮罩层**：
   在 `AccountChooserView.vue` 中：
   - “使用其他账号”列表项（`handleUseAnotherAccount`）直接执行 `router.push`，无任何遮罩层与延迟过渡；
   - 处于已登录激活态（`ACTIVE`）的账号项直接执行 `router.replace`，同样缺失 Scrim 遮罩与延迟过渡；
   - 列表项元素本身未携带 `:isScrim="true"` 属性声明。
3. **AccountChooserView.vue “移除账号”按钮样式与规范脱节**：
   `AccountChooserView.vue` 底部的“移除账号”按钮（`t('auth.remove_account')`）错误套用了行内超链接样式 `.auth-forgot-link`（内边距 4px 6px、圆角 4px、无固定高度），而未采用与 `IdentifierView.vue` 中次级操作按钮（`btn-create-account`）一致的标准胶囊药丸状按钮（高度 36px、圆角 9999px、内边距 0 16px、品牌色 hover/active 色阶）。导致卡片底部左右两侧操作按钮视觉失衡，且在切换移除模式时底部高度发生跳变。

---

## 2. 核心改动点说明

### 2.1 IdentifierView.vue 转场遮罩对齐
- **模板增强**：为“选择账号”链接按钮声明 `:isScrim="true"` 属性；
- **异步延迟调度**：将 `handleBackToChooser` 改造为 `async` 方法，在执行 `router.push` 跳转至 `/v1/signin/accountchooser` 前显式调用 `await signinFlowStore.triggerScrim(undefined, 1000)`，实现与“创建账号”按钮一致的平滑遮罩转场效果。

### 2.2 AccountChooserView.vue 列表项转场与遮罩闭环
- **模板属性对齐**：为所有账号列表项（`auth-selection-item`）及“使用其他账号”列表项（`auth-selection-item--add`）显式添加 `:isScrim="true"`；
- **使用其他账号流程**：在 `handleUseAnotherAccount` 执行 `router.push` 前，加入 `await signinFlowStore.triggerScrim(undefined, 1000)`；
- **已登录账号快捷进入**：在 `handleSelect` 的 `acc.sessionStatus === 'ACTIVE'` 分支中，执行 `router.replace` 前补充 `await signinFlowStore.triggerScrim(undefined, 1000)`；
- **严格类型安全**：消除 `catch (err: any)` 反模式，重构为强类型安全守卫 `catch (err: unknown)`，彻底杜绝 `any` 泛滥。

### 2.3 AccountChooserView.vue 移除按钮视觉规范对齐
- **按钮结构改造**：将底部“移除账号”按钮类名由 `auth-forgot-link` 变更为 `auth-action-btn btn-remove-account`；
- **样式体系重构**：
  - 废除废弃的 `.auth-forgot-link` 样式规则，杜绝残留兼容别名；
  - 引入 `.btn-remove-account` 完整样式规约，严格对齐 `IdentifierView.vue`（高度 36px、最小宽度 88px、padding 0 16px、圆角 9999px、文字颜色 `#0b57d0` / 暗色 `#8ab4f8`、hover/active 透明度过渡）；
  - 完善单个账号删除图标按钮（`.remove-icon-btn`）在亮色与深色模式下的 active 反馈和平滑过渡色阶。

---

## 3. 影响范围与回归验证

- **影响视图范围**：
  - `src/volur-lite-view-NEXT/src/views/auth/signin/IdentifierView.vue`
  - `src/volur-lite-view-NEXT/src/views/auth/signin/AccountChooserView.vue`
- **构建与回归验证**：
  - 执行 `vue-tsc --noEmit && vite build`，TypeScript 类型检查 0 报错，全量打包构建成功；
  - 严格保持代码文件的极致纯粹与零行内注释，所有技术推导与变更记录外置于本文档。
