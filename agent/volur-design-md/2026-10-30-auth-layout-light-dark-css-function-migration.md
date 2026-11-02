# 认证布局 light-dark() CSS 函数迁移与暗黑模式冗余规则重构方案

## 1. 变更背景与动因
在 `src/volur-lite-view-NEXT` 工程中，全站已建立基于现代 CSS `color-scheme` 与 `light-dark()` 函数的设计系统规范（详见 `src/styles/vcss.scss`）。
此前在 `src/layouts/primary/AuthLayout.vue` 中，除了基底声明使用的 CSS 变量（如 `--v-bg-surface`、`--sys-color-surface-container`、`--v-bg-card`）已内建 `light-dark()` 响应机制外，在底部额外书写了 `html.dark { ... }` 作用域覆盖块。
这导致在浏览器开发者工具（Chrome DevTools）审查 `.auth-layout` 及相关卡片元素时，同一个属性（如 `background-color`）存在多处声明与多层覆盖，产生了不必要的规则冗余与选择器特异度干扰。

## 2. 改造范围与核心改动点
1. **`src/layouts/primary/AuthLayout.vue` 样式重构、冗余清理与语义变量对齐**：
   - 彻底移除底部 `html.dark { ... }` 冗余样式块（包括 `.auth-layout`、`.auth-scrim`、`.auth-card`、`.auth-footer`、`.auth-card__progress-track`、`.auth-card__progress-inner` 等夜间硬编码声明）；
   - 清理 `.auth-card` 移动端及桌面端中残留的全部 `!important` 声明，依托移动优先（Mobile-First）自然层叠顺序实现干净的桌面端样式覆盖；
   - 将 `.auth-layout` 容器背景色统一指定为 `var(--color-v1-auth-surface)`，杜绝媒体查询内的重复背景声明；
   - 将 `.auth-card` 桌面端卡片背景色统一指定为 `var(--color-v1-auth-surface-container-lowest)`；
   - 将 `.auth-card` 及其标题与副标题文字颜色统一指定为 `var(--color-v1-auth-on-surface)`；
   - 将 `.auth-scrim` 与 `.auth-card__blocker` 的遮罩背景统一改为 `light-dark(rgba(255, 255, 255, 0.6), rgba(0, 0, 0, 0.6))`；
   - 将 `.auth-card` 桌面端阴影改为 `box-shadow: 0 1px 3px 0 light-dark(rgba(60, 64, 67, 0.08), rgba(0, 0, 0, 0.4))`；
   - 将线性进度条轨道与滑块色值重构为 `light-dark(#e8f0fe, rgba(168, 199, 250, 0.24))` 与 `light-dark(#0b57d0, #a8c7fa)`；
   - 将 `.auth-footer` 文字颜色直接收敛为 `light-dark(var(--v-text-muted, #5f6368), #e8eaed)`，其子项（链接与图标）统一采用 `color: inherit`。

2. **`src/styles/buttons.scss` 与 `src/styles/vcss.scss` 语义颜色统一**：
   - 将 `.auth-forgot-link` 文字颜色统一修改为 `var(--color-v1-auth-primary) !important;`，并使用 `light-dark()` 函数清理原有 `html.dark .auth-forgot-link` 冗余块；
   - 矫正 `vcss.scss` 中 `--color-v1-auth-on-surface` 为 `light-dark(#1f1f1f, #e3e3e3)`，确保其在亮色下呈现高对比深灰正文，在暗色下呈现灰白正文。

3. **`GoogleTextField` 彻底重构为 `VFieldInput` 并归入 `volur-ui` 组件库**：
   - 将原 `src/components/auth/GoogleTextField.vue` 升级重构为 `VFieldInput`，迁移至 `src/volur-ui/components/input/` 下统一管理；
   - 在 `src/volur-ui/index.ts` 中集中导出 `VFieldInput` 并注册至全局组件列表中；
   - 彻底删除原 `GoogleTextField.vue` 文件及 `src/components/auth/index.ts` 中的导出，全链路同步更新所有 12 处业务视图（登录/注册/找回密码）引用，不留旧别名兼容层；
   - 将聚焦边框与高亮文字颜色统一迁移至 `var(--color-v1-auth-primary)`，全面采用 `light-dark()` 消除组件内全部 `html.dark` 覆盖规则。

4. **系统全局 `html.dark` 全面审计与遍历分类**：
   - 全局扫描系统内所有组件与样式表，将其划分为：
     1. **底层机制支撑类**（如 `vcss.scss` 的 `color-scheme: dark` 根驱动器，必须保留以支持 `light-dark()` 函数执行）；
     2. **历史手动覆盖类**（如 `PasskeyModal.vue`、`RecaptchaVerifier.vue`、部分旧版通用组件与视图组件，具备后续平滑迁移至 `light-dark()` 的重构空间）；
     3. **第三方与浮层类**（Naive UI popover/tooltip 等组件库深层穿透类）。

5. **`VFieldInput` 首屏聚焦边框延迟消除（`:focus-within` 方案）**：
   - **问题现象**：页面刷新时（如 `identifier` 用户名输入框带有 `autofocus`），浮动标签（placeholder/label）动画在第一帧立即就位，但外边框从灰转为 `--color-v1-auth-primary`（及发光阴影）存在一瞬间的延迟。
   - **机理分析**：
     - 原先浮动标签由纯 CSS 兄弟选择器 `&__input:focus ~ &__label` 控制，在浏览器构建布局树执行原生 `autofocus` 时，DOM 原生匹配 `:focus` 即可在第 0 帧完成绘制；
     - 原先外层边框 `.v-field-input__box` 依赖 Vue 响应式状态 `isFocused`（`ref(false)`）绑定动态类名 `.v-field-input__box--focused`。原生 `focus` 事件必须经历 Vue 组件挂载、事件循环调度、响应式依赖收集及 DOM Patch，导致在首屏渲染时存在约十几至几十毫秒的渲染滞后，产生动效脱节。
   - **解决方案**：在 `.v-field-input__box` 上增加 `&:not(&--disabled):focus-within` 与 `&--error:focus-within` 规则，使得只要其内部原生 input 获得焦点，父级容器边框颜色和外发光阴影立刻在首帧由 CSS 引擎同步应用，彻底消除对 JS 挂载时序的依赖。

## 3. 验证与影响分析
- **桌面端与移动端表现**：在 Chrome DevTools 与真实浏览器渲染引擎下，验证 `color-scheme: light` 与 `color-scheme: dark` 切换；
- 亮色模式下 `.auth-layout` 正确呈现 `#f0f4f9`（桌面端）/ `#ffffff`（移动端），卡片背景为 `#ffffff`；
- 暗色模式下 `.auth-layout` 正确自适应为 `#1e1f20`，卡片背景正确自适应为 `#0e0e0e`，阴影与文字颜色无损保持原有 Google Material 3 规范；
- **首屏刷新验证**：通过 DevTools 脚本评估确认，原生 `autofocus` 触发时，`.v-field-input__box` 的 `:focus-within` 在初始帧直接为 `true`，`borderColor` 为 `rgb(11, 87, 208)`，`boxShadow` 为 `rgb(11, 87, 208) 0px 0px 0px 1px`，消除所有视觉滞后；
- 消除 DevTools 上的同名属性多重冗余提示，提升样式解析效率。
