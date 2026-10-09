# 认证身份胶囊组件 (AuthAccountChip) Naive UI 统一化改造

## 1. 变更背景与组件功能

### 1.1 组件功能与业务定位
`AuthAccountChip.vue`（位于 `src/volur-lite-view-NEXT/src/components/auth/AuthAccountChip.vue`）是 Volur 认证流程体系中对齐 Google 账户设计标准的**当前身份账号胶囊组件**：
1. **身份可视化（Avatar & Label）**：在用户进入二次挑战（如密码校验、邮箱验证码、手机短信、通行密钥等）或多步骤流程时，放置在卡片标题旁或顶部挂载插槽（`#header-extra`）中，展示当前正进行身份核验的用户账号；
2. **首字母微头像（Initial Circle）**：提取用户名或邮箱的首字符，转为大写渲染在左侧圆形头像微徽章中（默认 Google Blue 背景白字，暗色反转）；
3. **文本截断与保护（Ellipsis）**：支持用户名/邮箱超长时自动省略展示（`max-width: 200px`），避免挤占卡片头部空间；
4. **交互式切换入口（Switch Account Trigger）**：尾部渲染 Material 下拉箭头图标，点击该胶囊即可触发向账号选择列表（`/v1/signin/accountchooser`）的切换跳转，附带全局 Scrim 延迟与遮罩过渡防护。

### 1.2 改造动因
在之前的初步抽取中，该组件内部采用了原生 `<button type="button">`。为全面对齐平台 UI 框架标准，避免原生按钮缺少 Naive UI 统一组件树生命周期、水波纹与统一属性支持，按指示将该原生按钮重构为 Naive UI 的 `<n-button>`。

---

## 2. 改动详情

1. **[`AuthAccountChip.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/auth/AuthAccountChip.vue)**：
   - 引入 Naive UI 的 `NButton`；
   - 将根元素 `<button type="button">` 升级为 `<n-button quaternary :round="true">`；
   - 保持所有原生语义 Props 透传（`:disabled="disabled"`, `:isScrim="true"`, `:title="..."`, `@click="emit('click')"`）；
   - 内部保持首字母圆形头像 `<span>`、标识文本 `<span>` 与下拉箭头 `<svg>` 结构。

2. **[`buttons.scss`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/styles/buttons.scss)**：
   - 为 `.auth-account-chip` 补充关键样式加权（`!important`），锁定高 `32px`、边框、透明背景与圆角，杜绝 Naive UI 默认 CSS 变量破坏胶囊视觉尺寸；
   - 针对 Naive UI 内部渲染树调整 `.n-button__content`（`display: inline-flex; align-items: center; max-width: 100%;`）与 `.n-button__state-border`（`border: none !important;`）；
   - 规范 Hover（`rgba(60, 64, 67, 0.08)`）与 Active（`rgba(60, 64, 67, 0.12)`）以及暗色下的半透明高亮交互。

---

## 3. 验证与影响范围

1. **构建与类型验证**：
   - 运行 `npm run build`（`vue-tsc --noEmit && vite build`），全工程 2,903 个模块无警告构建通过，打包耗时 2.87s；
2. **零代码注释与规范遵循**：
   - 组件与样式源码内 100% 保持零代码注释，设计思路与技术选型外置于本文档；
3. **影响范围**：
   - [`src/volur-lite-view-NEXT/src/components/auth/AuthAccountChip.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/auth/AuthAccountChip.vue)
   - [`src/volur-lite-view-NEXT/src/styles/buttons.scss`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/styles/buttons.scss)
