# Volur UI 架构设计与基建落地方案

## 变更背景
在对平台前端样式全链路分析中发现，当前组件层存在两大核心痛点：
1. **样式权重冲突与暴力压制**：为了覆盖 Naive UI 内部组件样式，业务组件（如 `VSideBar.vue`）中大量使用了 `!important`，导致样式层级难以维护与复用；
2. **主题代码重复与脱节**：平台在 `vcss.scss` 中引入了基于 CSS Color Module Level 5 规范的 `light-dark()` 设计系统 Tokens，但业务组件多采用 `html.dark` 重复编写整块选择器覆盖，不仅产生了大量冗余样板代码，也破坏了单一真实数据源（SSOT）。

为此，决定基于 Naive UI 内核启动二次开发，构建平台级统一自研组件库 **Volur UI**。

---

## Naive UI 目录与内核架构解包分析

通过对 Naive UI (`node_modules/naive-ui/es`) 的解构剖析，其核心体系具备极高的模块解耦度：
1. **顶层基建层 (`_mixins/`, `_styles/`, `_utils/`)**：
   - `_styles/`: 维护基础通用的标尺规范（`common/`）与亮暗双色主题工厂（`light.ts`, `dark.ts`）；
   - `_mixins/`: 提供响应式状态逻辑，包括 `use-theme`（主题上下文接入）、`use-config`（配置提供者接入）、`use-locale`（国际化）；
   - `_utils/`: 提供强类型的 Vue 节点处理、DOM 操作与色彩计算工具。
2. **独立组件单元结构 (`<component>/`)**：
   - `index.ts`: 负责对外部暴露组件实例与 Props 类型；
   - `src/`: 承载组件视图实现（`[Name].tsx` / `[Name].vue`）与属性声明（`types.ts`）；
   - `styles/`: 专属于该组件的 CSS 变量工厂。通过 `_common.ts`（尺寸、圆角）与 `light.ts`/`dark.ts`（色值），将属性动态映射到组件私有的 `--n-*` 变量上。
3. **样式注入与隔离哲学**：
   - Naive UI 节点本身**不写死任何固定色值**，而是将所有状态与外观抽象为 `--n-*` CSS 变量（如 `--n-color`, `--n-text-color`, `--n-border`）；
   - 这意味着进行二次封装时，**只需重写 `--n-*` 变量的值**，即可在不改动底层复杂逻辑、且 **0 个 `!important`** 的前提下完成完全自主的视觉重塑。

---

## Volur UI 目录与工程架构设计

基于 Naive UI 解包经验，结合 Google Material Design 3 (M3) 规范、ES2026 与原生 `light-dark()` 机制，设计 Volur UI 架构如下：

```
src/volur-ui/
├── index.ts                      # 统一导出入口（提供 Vue Plugin install 及按需导出）
├── _styles/                      # 设计规范与全局主题中枢
│   ├── tokens.ts                 # 桥接 Google M3 变量与 vcss.scss
│   └── theme.ts                  # 生成 Naive UI GlobalThemeOverrides 的映射工厂
├── _mixins/                      # 基础 Composables
│   ├── use-namespace.ts          # BEM 规范生成器 (volur-*)
│   └── use-theme.ts              # 响应式消费 Volur 根上下文
├── provider/                     # 统一根配置提供者
│   ├── VolurConfigProvider.vue   # 聚合 NConfigProvider 与各类通知 Provider
│   ├── context.ts                # InjectionKey 规范
│   └── index.ts
└── components/                   # 二次封装组件库
    └── button/                   # VButton (验证组件，0 !important 原生反色药丸按钮)
        ├── src/
        │   ├── VButton.vue
        │   └── types.ts
        └── index.ts
```

---

## 改动点与基建落地

1. **全局设计标尺与主题中枢**：
   - 新建 `src/volur-ui/_styles/tokens.ts`：收敛调色板、圆角系统与排版字体映射；
   - 新建 `src/volur-ui/_styles/theme.ts`：封装 `createVolurTheme(isDark)`，将 Google Gray 表面阶梯与高对比反色主按钮规则注入 Naive UI 体系。
2. **组合式函数与上下文体系**：
   - 新建 `src/volur-ui/_mixins/use-namespace.ts`：规范 `volur-` 前缀的 BEM 类名和局部 CSS 变量名生成；
   - 新建 `src/volur-ui/_mixins/use-theme.ts`：提供深浅色判定与全局上下文访问能力；
   - 新建 `src/volur-ui/provider/context.ts` 与 `VolurConfigProvider.vue`：统一接管 `NConfigProvider`、加载条、对话框、消息通知及全局样式注入，对外暴露高度精简的 `<VolurConfigProvider>` 标签。
3. **彻底去除 Naive UI 底层依赖：VButton 100% 纯自研重构**：
   - 彻底删除 `import { NButton } from 'naive-ui'`，由外层 Facade 包裹层蜕变升级为 100% 纯自主实现的 Native Button；
   - **自研水波纹光晕扩散系统 (Ripple Wave)**：深度复刻 Naive UI 的 `Wave` 状态机，结合 `@keyframes volur-wave-spread` 与 `volur-wave-opacity`，利用 `nextTick` 与 `offsetWidth` 强制重绘，在点击/键盘触发时生成高保真扩散阴影；
   - **自研极速矢量旋转器 (SVG Loading Spinner)**：组件内嵌纯轻量矢量圆环指示器与 `@keyframes volur-spin`，随父级文字色自动着色与无缝切换，无需引用任何外部图标包；
   - **工业级全场景属性覆盖**：
     - 变体：`primary`（高对比反色）、`normal`、`tonal`、`danger`、`warning`、`text`；
     - 尺寸：`tiny` (22px)、`small` (28px)、`medium` (36px)、`large` (44px)；
     - 形状：`round` (胶囊药丸)、`circle` (圆形图标按钮)、`block` (全宽伸展)、`dashed` (虚线边框)；
     - 无障碍：完整实现 `focus-visible` 专用 Google 聚焦环与键盘触发；
   - **纯粹现代 CSS 标准驱动**：全生命周期 0 个 `!important`，0 行 `html.dark` 重复选择器，100% 依托 `light-dark()` 变量瞬时响应深浅色。
4. **解包重写 1:1 独立 Volur Provider 套件**：
   - 新建 `VConfigProvider.vue` (`<v-config-provider>`)：作为全树配置中枢，解耦 `n-config-provider`；
   - 新建 `VLoadingBarProvider.vue` (`<v-loading-bar-provider>`)：独立单例加载条桥接；
   - 新建 `VDialogProvider.vue` (`<v-dialog-provider>`)：独立命令式弹窗桥接；
   - 新建 `VNotificationProvider.vue` (`<v-notification-provider>`)：独立四角通知系统桥接；
   - 新建 `VMessageProvider.vue` (`<v-message-provider>`)：独立轻量 Toast 消息桥接，内置 1000ms 错误去重防抖 Proxy；
   - 新建 `VGlobalStyle.vue` (`<v-global-style>`)：无头全局样式同步器，彻底终结 Naive UI 对 `body.style` 的硬编码写入，原生驱动 `colorScheme` 与 `var(--v-bg-base)`；
   - 保留 `VolurConfigProvider.vue` 作为集成版别名，供全量一键装配；
   - 在 `src/main.ts` 中注册 `app.use(VolurUI)`，在全局模板中直接支持 `<v-config-provider>` 等标签调用；
   - 重构 `src/App.vue`：全量将 `n-config-provider` 等旧有标签替换为 `<v-config-provider>` 系列。
5. **组件库入口集成**：
   - 新建 `src/volur-ui/index.ts`：聚合所有组件与类型导出，并提供标准 Vue 插件 `VolurUI.install` 方法。
6. **VButton 与旧版 VBtn normal 样式像素级高保真对齐**：
   - **差异分析**：
     - **圆角 (Border Radius)**：旧版 `VBtn` 在 normal 变体下为 `12px`，自研 `VButton` 初始按照 Google M3 标准设定为 `8px`；
     - **内边距 (Padding)**：旧版 `VBtn` 为 `0 12px`，自研 `VButton` 初始设为 `0 16px`；
     - **间距 (Gap)**：旧版 `VBtn` 图标与文本间距为 `6px`，自研 `VButton` 初始设为 `8px`；
     - **文本颜色 (Text Color)**：旧版 `VBtn` 亮色采用炭黑 `#2b2d31`，暗色采用高亮纯白 `#fcfcfc`；自研 `VButton` 初始采用 Google Text 灰阶 `#1b1b1e` / `#d4d4d4`；
     - **悬浮背景 (Hover Background)**：旧版 `VBtn` 亮色为 `#EAEAEB`，暗色为 `#2a2b2d`；自研 `VButton` 初始为 `#f4f5f5` / `#2a2a2a`；
     - **字间距与行高**：旧版 `VBtn` 为 `letter-spacing: normal; line-height: 20px;`，`VButton` 初始设置略有差异。
   - **重构对齐**：
     - 重构 `VButton.vue` 的 `.volur-btn--normal` 规则，全面将圆角、内边距、图标间距、行高与字间距对齐至旧版 `VBtn` 视觉基准；
     - 全量使用 `light-dark()` 原生双模色彩体系（如 `color: light-dark(#2b2d31, #fcfcfc)`、`background-color: light-dark(#EAEAEB, #2a2b2d)`），在保持 100% 视觉高保真对齐的同时彻底消除 `html.dark` 冗余样式选择器与 `!important`；
     - 在 `CompetitionListView.vue` 中将“导入竞赛”按钮正式升级替换为 `<VButton variant="normal">`，并通过类型与打包构建验证（0 错误）。
7. **`--color-v1-button-container` 动态双模设计令牌与纯粹 SSOT 架构**：
   - 在 `vcss.scss` 全局主题作用域（`:root`、`.light-theme`、`.dark-theme`）中正式声明设计令牌 `--color-v1-button-container: light-dark(#fff, #323232);` 作为单一真实源；
   - 在 `VButton.vue` 的 `.volur-btn--normal` 中采用纯粹的 `background-color: var(--color-v1-button-container);` 绑定，坚决杜绝在组件层进行色值隐式双写，彻底消除未来变更时的不同步维护成本。
8. **设计令牌全面 SSOT 化演进 (`--color-v1-outline`, `--color-v1-text-on-button`)**：
   - 深入解构 `--volur-btn-ripple`（点击/敲击空格时光晕水波纹动画的扩散阴影色）与 `--volur-btn-focus-ring`（键盘 Tab 键导航时遵循 Google Workspace 标准的无障碍外聚焦光环色）；
   - 在 `vcss.scss` 中声明 `--color-v1-outline: light-dark(#d7d8da, #333);` 与 `--color-v1-text-on-button: light-dark(#2b2d31, #fcfcfc);`；
   - 在 `VButton.vue` 的 `.volur-btn--normal` 中将 `border-color` 与 `color` 全面解耦并绑定至对应全局设计令牌，实现纯粹的单一真实源消费。
9. **旧版 `VBtn.vue` 同步 SSOT 改造与 `html.dark` 清理**：
   - 彻底将 `VBtn.vue` 中的 `.v-btn-normal` 样式重构为消费 `--color-v1-button-container`、`--color-v1-outline` 与 `--color-v1-text-on-button` 变量；
   - 移除冗余的 `html.dark` 覆盖块，确保全站正在使用 `VBtn` 的遗留组件（如竞赛卡片 `VCompetitionCard.vue`、弹窗按钮等）在浏览器 DevTools 中 100% 显示 `var()` 变量，彻底杜绝硬编码色值。
10. **`VButton` 引入 `:radius` 与 `:size` 响应式控制与样式分层架构重构**：
    - 在 `types.ts` 中将 `VButtonSize` 扩充为 `VButtonPresetSize ('tiny' | 'small' | 'medium' | 'large') | number | string`，并引入 `radius?: number | string` 属性；
    - 在 `VButton.vue` 中封装 `computedStyle`，支持数字自动补全 `px` 单位及纯字面值解析；当传入自定义尺寸且结合 `circle` 时自动等比设置宽高；
    - 重构 SCSS 规则层叠顺序，将尺寸修饰类置于变体类之后，实现关注点完全分离（变体只管色彩与边框，尺寸修饰类管排版与基础圆角），并通过内联样式最高优先级确保 `:radius` 与自定义 `:size` 的绝对控制权。
11. **`--color-v1-button-container-high` 悬浮双模令牌落地**：
    - 在 `vcss.scss` 全局主题作用域中注册 `--color-v1-button-container-high: light-dark(#eaeaeb, #424242);`；
    - 在 `VButton.vue` 与 `VBtn.vue` 的 `normal` 变体下将 `&:hover:not(.is-disabled)` 的背景色统一解耦重构为 `background-color: var(--color-v1-button-container-high);`，彻底消除组件内悬停色硬编码。

---

## 影响范围
- 新增 `src/volur-ui/` 及其完整基建与 Provider 目录体系
- 重写 `src/App.vue` 顶层结构（从 235 行缩减至 109 行）
- `src/views/competition/CompetitionListView.vue` 中的“导入竞赛”按钮平滑替换为自研 `VButton` (`variant="normal"`)
- 全平台反馈 API 升级至 `window.$volur*` 并保持历史兼容性

