# VSideBar 分组导航与双层 Flex 布局重构方案与审计记录

## 1. 变更背景与需求目标

为进一步提升 Volur Lite 平台新一代前端（`volur-lite-view-NEXT`）主导航交互体验与功能清晰度，对侧边栏导航组件进行功能扩展与布局层级重构：
1. **列表项分组化与展示态适配**：
   - 展开模式下，各分组渲染左对齐分组标题（位于该组第一个列表项正上方）；
   - 收缩模式下，隐藏分组标题文字，在各组之间以水平分隔线（Divider）实现轻量视觉分割。
2. **探索分组（EXPLORE / 探索）**：
   - 将现有“竞赛演练”文本调整为“赛事大厅”；
   - 新增“题目训练”（challenges）导航项；
   - 组标题定义为 `EXPLORE`（中文模式下为 `探索`）。
3. **学习分组（LEARN / 学习）**：
   - 新增分组 2，组标题为 `LEARN`（中文模式下为 `学习`）；
   - 组内包含“学习中心”（learning）导航项。
4. **双层 Flex 布局拓扑（Flex-in-Flex）**：
   - 将 SideBar 从过去的“单层列表项平铺 Flex”转变为“分组外层 Flex”；
   - 外层分组之间设定 `gap: 12px`；
   - 每个分组内部同样采用 Flex 纵向布局：第一行为左对齐分组标题，组内各列表项之间设定 `gap: 6px`。

---

## 2. 架构设计与改动点详情

### 2.1 数据模型扩展 (`types.ts`)
在 `src/volur-lite-view-NEXT/src/components/sidebar/types.ts` 中新增分组规范定义：
- `SidebarNavGroup`：业务端侧边栏分组接口（包含 `key`、`title`、`titleKey`、`label` 与 `items: SidebarNavItem[]`）；
- `VSideBarGroup`：Naive UI 菜单选项分组视图模型（包含 `key`、`title`、`titleKey`、`label` 与 `options: MenuOption[]`）；
- `VSideBarProps` 与 `SideBarProps` 扩展支持 `groups?: SidebarNavGroup[]` 与 `menuGroups?: VSideBarGroup[]`。

### 2.2 多语言字典补齐与语义更名 (`locales/**/common.json`)
同步在 `zh-CN`、`en-US`、`zh-TW` 的 `common.json` 维护多语言真实源：
- `competitions`：简中由“竞赛演练”调整为“赛事大厅”，繁中调整为“競賽大廳”，英文保持“Competitions”；
- `challenges`：简中为“题目训练”，繁中为“題目訓練”，英文为“Challenges”，图标更新为 `RocketOutline`；
- `learning`：简中为“学习中心”，繁中为“學習中心”，英文为“Learning”（由“Learning Center”精简）；
- `nav_group_explore`：简中为“探索”，繁中为“探索”，英文为“EXPLORE”；
- `nav_group_learn`：简中为“学习”，繁中为“學習”，英文为“LEARN”；
- `competition.json` 中的 `type_theory`：英文由“Theory Exam”精简为“Theory”。

### 2.3 双层 Flex 布局重塑 (`VSideBar.vue`)
- **外层容器 `.sidebar-nav-groups`**：
  - `display: flex; flex-direction: column; gap: 12px;`
  - 承载所有的导航分组，实现组与组之间严格 12px 间距。
- **内层容器 `.sidebar-nav-group`**：
  - `display: flex; flex-direction: column; gap: 6px;`
  - 第一行渲染分组标题 `.sidebar-group-title`：
    - 展开状态：`height: 20px; line-height: 20px; padding: 0 12px; font-size: 11px; font-weight: 600; letter-spacing: 0.08em; text-transform: uppercase;`，亮色采用 Google Gray `#5f6368`，暗色采用 `#9aa0a6`，左边距与菜单项内部图标对齐；
    - 收缩状态：通过 `display: none !important;` 彻底消除占位。
  - 分组分隔线 `.sidebar-group-divider`：
    - 仅在收缩模式且非首个分组处展示（`isCollapsed && groupIndex > 0`）；
    - 规格为 `width: 32px; height: 1px; margin: 0 auto;`，亮色边框色为 `#d7d8da`，暗色为 `#3c4043`。
  - 组内菜单项 `.v-menu`：
    - `display: flex; flex-direction: column; gap: 6px;`
    - 去除过去硬编码的 `.n-menu-item { margin: 2px 0 }`，由 Flex Gap 全权掌管 6px 间距。

### 2.4 上层包装与全端兼容 (`SideBar.vue` & `AppLayout.vue`)
- **桌面端与移动端分流**：
  - `SideBar.vue` 将分组配置映射并计算为 `resolvedMenuGroups`（供给桌面端 `VSideBar.vue`）和 `resolvedFlatMenuOptions`（供给移动端 `VBottomBar.vue`）；
  - 移动端底部导航栏扁平展示“赛事大厅”、“题目训练”与“学习中心”三项，杜绝层级渲染溢出。
- **全链路路由自动高亮 (`keyRouteMap`)**：
  - 递归遍历各分组内 `items`，确保路径匹配 `/competitions`、`/challenges`、`/learning` 时精准激活对应项。
- **向下无缝兼容管理端 (`AdminLayout.vue`)**：
  - `computedNavGroups` 探测到外层传入未分组的普通 `menuOptions` 时，自动包裹为无标题的默认组，管理后台菜单继续平稳展示，保持零破坏。

---

## 3. 规范遵循与代码纯净度审计

1. **零注释铁律 (Zero Comments Rule)**：
   - 所有在本次改动中新增或调整的代码文件（`VSideBar.vue`、`SideBar.vue`、`AppLayout.vue`、`types.ts` 等）均未掺杂任何解释性注释或临时标记，所有设计细节与演进逻辑全部外置记录于本文档中。
2. **ES2026 与 Google Workspace 色彩体系规范**：
   - 标题与分隔线严格对齐 Google Workspace 设计标准（亮色 `#5f6368` / `#d7d8da`，暗色 `#9aa0a6` / `#3c4043`）。
3. **构建与类型验证**：
   - 运行 `vue-tsc --noEmit` 通过，无类型断言与未声明错误；
   - 运行 Vite 生产打包通过（`✓ built in 15.56s`）。
