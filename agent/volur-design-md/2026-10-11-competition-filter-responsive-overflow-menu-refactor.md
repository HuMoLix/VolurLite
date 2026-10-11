# 竞赛列表赛制筛选栏响应式折叠菜单重构记录 (2026-10-11)

## 1. 变更背景与问题定位

在 `src/volur-lite-view-NEXT` 工程的竞赛列表页中，顶部赛制筛选栏（全部赛制、解题赛、理论赛、攻防对抗赛、攻防进阶赛、空间安全赛）在移动端/手机分辨率等窄屏视口下存在以下体验缺陷：
1. **容器默认不换行**：外层容器设置了 `flex`，默认 `flex-wrap: nowrap`，子项无法自动折行；
2. **强制水平滚动与滚动条外溢**：声明了 `overflow-x-auto scrollbar-none`，由于 Tailwind 未配置滚动条隐藏插件，移动端及模拟器直接渲染出了原生的横向滚动条，破坏了 UI 的一体感；
3. **空间利用率不足**：一味横向滚动或硬性多行堆叠在移动端易导致页面首屏纵向空间被占用，或右侧赛制不易被用户感知。

## 2. 方案设计与交互规范

针对上述问题，并结合用户对于“窄屏下将超出按钮替换为 `MenuOutline` 按钮、点击弹出 Tooltip/Popover 调用 `VCard`”的交互诉求，重构方案如下：

1. **响应式断点与自适应切分**：
   - 监听窗口宽度及响应式状态：
     - **超窄移动屏 (< 460px)**：展示前 2 个核心赛制（全部、解题赛）+ 1 个 `MenuOutline` 更多赛制按钮；
     - **移动横屏 / 平板 (< 860px)**：展示前 3 个赛制（全部、解题赛、理论赛）+ 1 个 `MenuOutline` 更多赛制按钮；
     - **桌面宽屏 (>= 860px)**：直接平铺展示全部 6 个赛制，隐藏更多菜单按钮；
   - 移除 `overflow-x-auto scrollbar-none`，配合 `flex-wrap` 提供双重防护，杜绝任何水平滚动条产生。

2. **Popover 与 VCard 组件深度协同**：
   - 使用 Naive UI 的 `NPopover`（`trigger="click"`，`placement="bottom-start"`，`:show-arrow="false"`，`raw` 模式）；
   - 内部封装调用 `@/components/sidebar` 导出的平台级基础卡片组件 `VCard`，宽度设为 `190px`，内边距为 `6px`，自动跟随 `currentTheme`（`light` / `dark`）；
   - 弹出卡片中列出被收起的赛制列表项，采用 Google Material 3 风格：悬浮高亮、选中项主色高亮并展示 `CheckmarkOutline` 勾选图标。

3. **激活态双向反馈设计**：
   - 当用户在弹出的 `VCard` 中选中了某个被收起的赛制（例如“攻防对抗赛”）时，主栏上的 `MenuOutline` 按钮自动变为激活态（`.is-selected`），并动态显示该赛制的名称标签，使用户在未展开菜单时也能清晰获知当前筛选维度；
   - 当用户点击主栏可见项（如“全部”）时，`MenuOutline` 按钮重置为仅展示图标的常规状态；
   - 点击下拉列表项后自动关闭弹窗并触发赛事分页列表重新加载。

## 3. 代码变动与实现细节

### 3.1 视图模板重构 (`CompetitionListView.vue`)
- 赛制按钮循环由 `availableTypeTabs` 改为响应式计算属性 `visibleTabs`；
- 条件渲染 `showMoreMenu` 并在末尾挂载 `MenuOutline` 触发器与 `VCard` 弹窗卡片；
- 为选中的下拉赛制添加 `CheckmarkOutline` 对勾状态。

### 3.2 脚本逻辑扩展
- 引入 `NPopover`、`MenuOutline`、`CheckmarkOutline`、`VCard`、`useAppStore`；
- 维护 `windowWidth` 响应式宽度并在 `onMounted` / `onUnmounted` 中绑定/解绑 `resize` 监听；
- 新增 `visibleTabs`、`dropdownTabs`、`showMoreMenu`、`selectedDropdownTab` 与 `isDropdownTabSelected` 计算属性；
- 实现 `handleDropdownSelect` 选中回调与弹窗关闭逻辑。

### 3.3 样式规范对齐 (Google M3)
- 优化 `.competition-filter-btn.menu-trigger-btn` 在选中态下的内边距与渐变圆点剔除，避免图标与圆点重叠；
- 添加 `.competition-menu-list` 与 `.competition-menu-item` 样式，严格对齐 `view-NEXT-rules.md` 中的 Google Blue（`#1a73e8` / 暗色 `#a8c7fa`）及分层背景规范。

## 4. 影响范围与验证结果

- **影响文件**：
  - `src/views/competition/CompetitionListView.vue`
- **构建与类型验证**：
  - 执行 `vue-tsc --noEmit && vite build`，编译顺利通过（Exit Code 0），无类型错误及构建警告；
- **视觉与交互表现**：
  - 手机分辨率下水平滚动条彻底消除；
  - 赛制筛选区域在单行内整齐呈现；点击菜单按钮可顺畅呼出 `VCard` 进行赛制切换并精确回显。
