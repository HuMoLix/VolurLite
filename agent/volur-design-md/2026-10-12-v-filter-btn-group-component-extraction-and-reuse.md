# 2026-10-12 VFilterBtnGroup 通用筛选按钮组组件提取与多场景复用改造记录

## 1. 变更背景与需求概述

在 `src/volur-lite-view-NEXT` 前端工程中，竞赛列表视图 `src/views/competition/CompetitionListView.vue` 原先内嵌了一套高定制化且具备优雅动画的筛选按钮组（`.competition-filter-btn`），包含：
1. 单选项的高对比度选中状态、左侧内边距展开过渡与径向渐变圆点指示器（`radial-gradient`）；
2. 自适应屏幕宽度的折叠逻辑（在移动端或平板尺寸下自动将超出数量的筛选项收敛至带有 `MenuOutline` 图标的 Popover 更多菜单中）；
3. 弹出层使用平台自研 `VCard`，支持黑白昼夜模式自动感知、选中项高亮（Google Blue）与 `CheckmarkOutline` 勾选图标；
4. 当下拉菜单中的项被选中时，触发器按钮自动呈现选中态并显示当前选中的子项标签。

然而，该部分逻辑此前高度耦合在 `CompetitionListView.vue` 单一视图内部，代码分散且难以被其他模块复用；而在 `src/views/competition/CTFCompetitionView.vue` 等业务视图中，Tab 切换仍使用原生手写的普通按钮组合，缺少统一的视觉规范与响应式折叠能力。

为了提升组件复用度并践行平台设计规范，本任务将该组件的结构、响应式折叠逻辑、状态流转与样式完整提取为独立的通用组件 `VFilterBtnGroup.vue`，并在 `src/components/common/` 统一导出，随后将其深度接入 `CompetitionListView.vue` 与 `CtfCompetitionView` 中。

---

## 2. 核心改动详情

### 2.1 组件类型定义 (`src/components/common/types.ts`)
在公共类型模块中新增强类型接口定义：
1. **`FilterOption<T>`**：
   - `value: T`：项唯一标识值；
   - `label?: string`：显示标签文案；
   - `icon?: any`：图标组件（支持 Naive UI / Ionicons 等）；
   - `count?: number | string`：数值徽标（支持计数呈现）；
   - `disabled?: boolean`：禁用状态；
   - `title?: string`：悬停提示信息。
2. **`VFilterBtnGroupProps<T>`**：
   - `modelValue?: T` / `value?: T`：双向绑定的当前选中值；
   - `options: (FilterOption<T> | T)[]`：选项数组，支持复杂对象或基础字面量标量；
   - `responsive?: boolean`：是否开启窗口尺寸响应式自适应折叠（默认 `true`）；
   - `bullet?: boolean`：当无图标时是否展示单选圆点（默认 `true`）；
   - `disabled?: boolean`：全局禁用态；
   - `maxVisible?: number`：显式强制最大展示项数；
   - `breakpoints` 与 `collapseVisible`：支持自定义移动端与平板端的折叠临界阈值和可视项数；
   - `dropdownWidth`：下拉浮层 `VCard` 宽度（默认 `190`）；
   - `size?: 'small' | 'medium' | 'large'`：尺寸变体控制。

### 2.2 提取构建 `VFilterBtnGroup.vue` (`src/components/common/VFilterBtnGroup.vue`)
1. **纯粹与自解释架构**：
   - 严格遵循零注释铁律，采用 Vue 3.5 `<script setup lang="ts" generic="T extends string | number = string | number">` 泛型实现。
   - 内部利用 `useAppStore()` 响应式感知全局主题（`currentTheme`），无缝传递给弹出层 `VCard`。
   - 采用标准事件体系：同时支持 `update:modelValue`、`update:value` 及 `change(val, option)`。
2. **槽位扩展性 (Slots)**：
   - 提供 `#prefix`、`#label`、`#suffix` 等具名插槽，支持在按钮内自由注入自定义图标、状态标或计数小徽章。
3. **视觉与交互细节**：
   - 封装 `.v-filter-btn` 与 `.v-filter-menu-item`；
   - 当项具有 `icon` 时，智能屏蔽圆点径向背景，采用紧凑间距；无图标时优雅展示单选动效点；
   - 封装徽章样式 `.v-filter-btn-count` 与 `.v-filter-menu-count`，在选中态与日间/夜间模式下均提供符合 Google Material 规范的高对比对比度。
4. **统一导出 (`src/components/common/index.ts`)**：
   - 导出 `VFilterBtnGroup` 以及对应的 `FilterOption`、`VFilterBtnGroupProps`。

### 2.3 重构 `CompetitionListView.vue`
1. 移除视图内原先冗余的手写按钮与 `n-popover` 模板结构，替换为单行声明：
   ```html
   <VFilterBtnGroup
     v-model="selectedType"
     :options="availableTypeTabs"
     @change="handleTypeChange"
   />
   ```
2. 彻底清理组件内不再需要的局部响应式状态与监听器：
   - 移除 `visibleTabs`、`dropdownTabs`、`showMoreMenu`、`selectedDropdownTab`、`isDropdownTabSelected`、`isMoreMenuShow`、`windowWidth`；
   - 移除 `handleDropdownSelect` 与 `handleResize` 函数；
   - 移除 `onMounted` 与 `onUnmounted` 中的 `window.resize` 监听器；
   - 移除已无外部引用的 `VCard`、`NPopover`、`MenuOutline`、`CheckmarkOutline` 导入；
   - 清理 `<style>` 中长达 160+ 行的 `.competition-filter-btn` 与 `.competition-menu-*` 样式规则，使得视图文件体积大幅精简。

### 2.4 重构 `CtfCompetitionView`
1. 将原先手写的 4 个 Tab 按钮（赛题挑战、官方公告、计分排行榜、我的战队）统一抽象为标准 `tabOptions` 计算属性，注入对应图标（`FlashOutline`、`MegaphoneOutline`、`TrophyOutline`、`PeopleOutline`）与动态计数（`challenges.length`、`announcements.length`）。
2. 在模板中无缝引入 `<VFilterBtnGroup v-model="activeTab" :options="tabOptions" />`。
3. 在宽屏下无损展示全部 4 个选项并完美支持图标与计数小徽标；在移动端或窄屏下自动响应折叠至弹出菜单中，大幅改善了 CTF 竞技场在小屏设备下的横向溢出问题。

---

## 3. 规范遵循与质量验证

1. **零注释铁律**：所有新增及修改的代码文件（`.vue`、`.ts`）无任何行内注释或设计解释，保持纯净自解释；
2. **色彩规范**：严格遵循 Google Blue (`#1a73e8` / `#8ab4f8`)、暗黑阶梯（`#202124`、`#262626`、`#333`）与禁止刺眼纯黑文字规范；
3. **构建验证**：执行 `npm run build`（`vue-tsc --noEmit && vite build`）通过，无任何 TypeScript 类型报错及打包告警。
