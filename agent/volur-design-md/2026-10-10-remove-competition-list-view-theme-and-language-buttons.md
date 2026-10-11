# 移除赛事列表页面浮动主题与语言切换按钮施工方案

## 1. 变更背景与需求分析
在 `src/volur-lite-view-NEXT` 工程中，平台已在全局通用响应式侧边栏（`SideBar` / `VSideBar` / `VBottomBar`）中集成了统一的主题模式切换、系统设置与通用导航交互能力。

在 [CompetitionListView.vue](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/competition/CompetitionListView.vue) 视图中，左下角原先通过 `<aside>` 固定悬浮了一组局部的“切换日夜间模式按钮”和“切换语言下拉菜单”。该浮动控制器存在以下问题：
1. 与全平台统一的侧边栏主题/设置交互功能产生重叠与视觉遮挡；
2. 页面中保留了仅由该浮动控件使用的额外响应式状态、计算属性及未被其他逻辑引用的导入；
3. 根据平台统一 UI/UX 体验与规范要求，需将该浮动按钮组彻底移除并清理相关死代码。

## 2. 改动点清单

### 2.1 模板清理
- 移除 `<aside>` 容器及其包裹的主题切换按钮与 `n-dropdown` 语言下拉选择菜单组件。
- 修复并保证最外层 Flex 容器与页脚闭合标签层级一致。

### 2.2 脚本与依赖瘦身
- **导入清理**：
  - 从 `naive-ui` 导入中移除未使用的 `NDropdown`；
  - 从 `@vicons/ionicons5` 导入中移除未使用的 `GlobeOutline` 图标；
  - 移除完全未使用的 `@/i18n` 模块导入（`normalizeLocale`, `toUrlLang`）。
- **状态与方法移除**：
  - 移除用于语言下拉列表渲染的配置数组 `languageOptions`；
  - 移除用于展示当前语言标签的计算属性 `currentLangLabel`；
  - 移除语言切换触发的处理函数 `handleLanguageSelect`。

## 3. 安全隐患修复与影响范围评估
1. **代码零冗余与文档外置**：在本次修改中严格遵守零注释铁律，未在 Vue 单文件组件中添加任何行内注释或废弃别名。
2. **构建与类型验证**：执行 `vue-tsc --noEmit && vite build`，编译与类型检查均 100% 通过（0 错误）。
3. **功能影响**：
   - 页面主要赛事浏览、分类筛选、分页导航及路由跳转逻辑保持不变；
   - 布局不再被左下角悬浮控件遮挡，界面更符合 Google Material Design 极简通透风格。
