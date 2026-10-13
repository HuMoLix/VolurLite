# 通用搜索框组件 VSearch 研发与侧边栏 VSearchTool 重命名施工记录

## 1. 变更背景与目标

为规范前端通用组件库与侧边栏工具组件的职责边界，统一平台内各类列表搜索输入交互视觉规范：
1. 将原侧边栏弹窗搜索组件 `src/components/sidebar/VSearch.vue` 全链路彻底重命名为 `VSearchTool.vue`，并同步更新导出与调用方；
2. 在通用组件库 `src/components/common/` 下研发独立的通用搜索框组件 `VSearch.vue`，基于 Naive UI `<n-input>` 进行封装与视觉定制；
3. 将赛事列表视图 `CompetitionListView.vue` 中的搜索框替换为该全新的通用组件，并将占位符统一调整为 “搜索竞赛名称”。

---

## 2. 核心改动点

### 2.1 侧边栏搜索弹窗重构与重命名为 `VSearchTool`
- **文件重命名**：`src/components/sidebar/VSearch.vue` -> `src/components/sidebar/VSearchTool.vue`；
- **类型与组件名调整**：
  - `name: 'VSearch'` 改为 `name: 'VSearchTool'`；
  - `src/components/sidebar/types.ts` 中 `VSearchProps` 重命名为 `VSearchToolProps`；
  - `src/components/sidebar/index.ts` 集中导出调整为 `export { default as VSearchTool } from './VSearchTool.vue'`；
- **调用处无缝对接**：
  - `src/components/sidebar/VSideBar.vue` 中组件引用由 `VSearch` 更新为 `VSearchTool`。

### 2.2 通用搜索输入组件 `VSearch.vue` 实现
- **文件路径**：`src/components/common/VSearch.vue`；
- **基础架构**：
  - 基于 `<n-input>` 进行二次封装，默认内置 `SearchOutline` 前缀搜索图标与清空功能；
  - 支持 `v-model` 与 `v-model:value` 双向数据流绑定；
  - 暴露 `search`、`clear`、`focus`、`blur` 等强类型事件，暴露 `focus`、`blur`、`select`、`clear` 等实例控制方法；
- **样式定制（严格对齐设计规范）**：
  - 边框颜色：亮色模式 `#d7d8da80`，暗夜模式 `#262626`；
  - 聚焦边框颜色：亮色模式 `#b7babd`，暗夜模式 `#333`；
  - 圆角：`border-radius: 8px`；
  - 字体排版：`font-size: 14px; font-weight: 400; line-height: 21; font-family: Inter, sans-serif; font-optical-sizing: auto;`；
  - 文本颜色：亮色模式 `#1b1b1e`，暗夜模式 `#d4d4d4`；
  - 配合 Naive UI `theme-overrides` 与 Scoped SCSS 深度选择器（`:deep`），彻底消除 Naive UI 默认的聚焦蓝色/绿色投影，保障纯净的高对比极简边框状态切换。

### 2.3 通用组件库导出与类型扩充
- **类型定义**：`src/components/common/types.ts` 新增 `VSearchProps` 接口；
- **模块导出**：`src/components/common/index.ts` 统一导出 `VSearch`。

### 2.4 赛事列表页 `CompetitionListView.vue` 接入
- **模板替换**：将原 `<n-input>` 替换为 `<VSearch>` 组件，设置 `placeholder="搜索竞赛名称"`，绑定 `@search="handleSearch"` 与 `@clear="handleSearch"`；
- **依赖清理**：清理未使用到的 `NInput`、`NIcon` 与 `SearchOutline` 导入，统一从 `@/components/common` 引入 `VSearch`；
- **样式适配**：调整 `.competition-search-input` 样式，移除原 `border-radius: 12px !important` 强覆盖，保留 `height: 36px` 与相邻筛选按钮组的基准线对齐。

### 2.5 光标颜色与聚焦占位符消失缺陷根因定位与修复
1. **光标蓝色根因与修复**：
   - **根因**：Naive UI `<n-input>` 默认继承组件库主题的主品牌色（Google Blue `#1a73e8`），通过 `--n-caret-color` 变量作用于光标；组件此前未显式覆盖该变量；
   - **修复**：在 `inputThemeOverrides` 中显式指定 `caretColor: '#2b2d31'`（暗夜模式 `#d4d4d4`），并在 SCSS 中深度注入 `--n-caret-color: #2b2d31 !important;` 以及 `.n-input__input-el { caret-color: #2b2d31 !important; }`。
2. **聚焦时 Placeholder 消失且失焦不恢复根因与修复**：
   - **根因**：原规则中指定了无单位的 `line-height: 21`。在 W3C 标准 CSS 中，无单位数字视为行高倍数系数（即 $21 \times 14\text{px} = 294\text{px}$ 行高）；
   - Naive UI 内部容器 `.n-input__input` 高度仅为 36px（且 `overflow: hidden`）。当行高高达 294px 时，内部滚动高度 `scrollHeight` 膨胀为 294px；
   - 当输入框获得焦点时，浏览器原生的焦点定位机制自动对 `.n-input__input` 执行内部滚动居中（`scrollTop` 突增至 `129px`）；
   - 由于 Naive UI 的占位符是由绝对定位的 `.n-input__placeholder` 模拟渲染，父容器发生 `scrollTop = 129px` 偏移后直接将占位符推出了 36px 的可视区域；且失焦时浏览器不会回滚 `scrollTop`，导致占位符永久消失；
   - **修复**：将 `line-height: 21` 修正为规范像素单位 `line-height: 21px`，并同步校正 `inputThemeOverrides.lineHeight` 与 `.n-input__placeholder` 的行高样式，使内部行高与可视区域严格吻合，彻底杜绝异常内部滚动。

### 2.6 抽象底层通用输入框 VInput 与 VSearch 组合架构重构
- **底层基础组件 `src/components/common/VInput.vue`**：
  - 将所有关于 `<n-input>` 的视觉基底样式（边框 `#d7d8da80` / `#262626`、圆角 `8px`、字体与 `line-height: 21px`、文本颜色 `#1b1b1e` / `#d4d4d4`、光标颜色 `#2b2d31` / `#d4d4d4`、聚焦边框 `#b7babd` / `#333`、占位符排版等）全面下沉并收敛于 `VInput.vue`；
  - 完整暴露输入框常用属性（`type`、`maxlength`、`showCount`、`readonly` 等）与插槽（`prefix`、`suffix`、`clear-icon` 等）；
  - 集中导出：在 `common/index.ts` 中对外导出 `VInput`，并在 `common/types.ts` 中定义 `VInputProps`（支持 `'text' | 'password' | 'textarea'` 等标准类型）；
- **上层业务组件 `src/components/common/VSearch.vue`**：
  - 全面基于 `VInput` 构建，复用底层样式基底，自身仅负责内置 `SearchOutline` 前缀搜索图标、默认启用 `clearable` 以及回车/清空时触发 `@search` 语义事件；
  - `VSearchProps` 继承自 `VInputProps`，保持强类型的一致与扩展性。

### 2.7 自定义圆角 radius 属性支持
- **属性定义**：在 `VInputProps` 与 `VSearchProps` 中增加 `radius?: number | string`；
- **智能解析计算**：通过 `resolvedRadius` 统一规范化：
  - 当 `round === true` 时优先赋予 `9999px` 胶囊圆角；
  - 当传入数字（如 `:radius="12"`）时自动追加 `px` 单位；
  - 当传入字符串（如 `radius="16px"` 或 `radius="1rem"`）时原样应用；
  - 未指定时默认使用设计基准 `8px`；
- **响应式样式透传**：
  - 将 `resolvedRadius` 同步注入至 Naive UI `theme-overrides` 的 `borderRadius`；
  - 通过 CSS 自定义属性 `--v-input-radius` 动态赋能，精准作用于 `.v-input`、`.n-input-wrapper`、`.n-input__border` 及 `.n-input__state-border`，实现跨层级无死角平滑定制；
  - `VSearch` 通过属性透传将其透明代理至底层 `VInput`。

---

## 3. 规范审计与验证

1. **重构彻底性原则**：侧边栏组件全链路重命名为 `VSearchTool`，无遗留废弃别名；输入框基础样式完整下沉至 `VInput`，`VSearch` 职责纯粹解耦；
2. **纯粹代码与零注释原则**：所有新增及修改的代码文件（`VInput.vue`、`VSearch.vue`、`VSearchTool.vue`、`CompetitionListView.vue` 等）均实现纯净零注释；
3. **类型与构建验证**：执行 `npm run build`（`vue-tsc --noEmit && vite build`），全端 TypeScript 类型检查与生产打包一次性 0 错误通过；
4. **DevTools 视觉与交互实测**：通过浏览器控制台与实时截图验证，`VInput` / `VSearch` 获得焦点时占位符常驻、光标准确呈现 `#2b2d31`，自定义圆角动态生效且无任何视口异常滚动。
