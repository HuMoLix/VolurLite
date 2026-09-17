# 竞赛列表赛制动态判别显示与 Filter Tabs 工具栏排版重构设计记录

**变更日期**：2026-09-16  
**变更模块**：`volur-lite-view`（前端竞赛中心视图）  
**影响范围**：`src/views/competition/CompetitionListView.vue`  

---

## 1. 变更背景与问题分析

在原公开竞赛列表页面（`CompetitionListView.vue`）中，赛制过滤标签栏与页面排版存在如下缺陷：

1. **赛制分类硬编码且与后端响应数据脱节**：
   - 原标签栏硬编码了全部 6 个赛制项（`ALL` 全部赛制、`CTF` 解题赛、`THEORY` 理论知识赛、`AWD` 攻防对抗、`AWD_PLUS` 综合攻防、`ISW` 靶场竞技）；
   - 当后端 API `GET /api/v1/competitions?page=1&size=10` 响应数据（如示例中仅包含 `"type": "CTF"` 的单项比赛）时，界面上依旧死板地展示所有 6 个赛制标签；
   - 用户点击无任何比赛数据的赛制标签（如 THEORY、AWD、ISW）后呈现空态，缺乏直观的数量感知与可用性提示。
2. **布局死板与自适应问题**：
   - 原 `.filter-tabs` 被固定限制在 `max-width: 620px;`，在 Naive UI 的 `type="segment"`（分段控制器）模式下，若当前有效赛制较少（如仅有全部和 CTF 2 项），分段控制器会将每一个选项横向拉伸至 310px，造成界面极其空旷、失调；而在移动窄屏设备上 620px 又易发生溢出截断；
   - 原顶部操作区将搜索框置于大标题右侧，而将过滤标签孤立在第二行左侧，致使第二行右侧存在大片无意义空白，视觉层级割裂。

---

## 2. 改造方案与实现细节

### 2.1 基于 API 响应数据的赛制动态判别与计数绑定 (`availableTypeTabs`)
- **数据源动态分析**：
  - 基于响应数据 `competitionList`，通过 `computed` 实时派生 `typeCountMap`，统计当前结果集内各赛制出现频次；
  - 动态计算并构建可用标签页列表 `availableTypeTabs`：
    - 首项始终为“全部赛制（`ALL`）”，其计数统计当前列表总数；
    - 紧跟当前数据集中真实存在的赛制类型（如 `CTF`），按标准预定义类型顺序（`SUPPORTED_COMPETITION_TYPES`）动态生成对应 Tab 项；
    - 支持未知/扩展赛制类型的动态优雅追加与兜底；
    - 当响应数据集中无对应类型的竞赛时，不再强行生成该类型的标签页。
- **平滑切换守卫 (`watch(availableTypeTabs)`)**：
  - 监听可用赛制标签列表变更，若用户在搜索或数据刷新后，当前选中的 `selectedType` 在最新可用列表中不复存在，自动平滑回退至 `'ALL'`，杜绝无效/空过滤状态残留。
- **赛制徽标（Count Badge）内嵌**：
  - 在每个赛制 Tab 内部组合标签名与数量徽标：`<div class="tab-item-inner"><span class="tab-label">...</span><span class="tab-count-badge">...</span></div>`；
  - 采用轻量圆角胶囊徽标，选中态反色（黑白）高亮，与 Volur 设计原则极致对齐。

### 2.2 工具栏与排版布局重构 (`filter-toolbar-row`)
- **视觉层级重组**：
  - 第一层（`.page-title-row`）：大标题与描述副标题，拥有宽裕的视觉呼吸空间；
  - 第二层（`.filter-toolbar-row`）：将“赛制分类分段控制条”与“关键词搜索栏”合流至同一水平工具行，两端对齐（`justify-content: space-between`），使页面控制工具高度内聚。
- **分段控制器自适应 (`width: fit-content`)**：
  - 移除原有的固定 `max-width: 620px;` 约束，改用 `width: fit-content; max-width: 100%;`；
  - 分段轨道紧凑贴合实际 Tab 项宽度，当仅有 1~2 个类型时紧致居左，不会过度拉伸变形；
  - 增加小屏幕横向溢出滚动（`overflow-x: auto; scrollbar-width: none;`），确保在不同屏幕分辨率下均可优雅自适应；
  - 在移动设备（<= 768px）下，工具行自动流式换行，搜索框自适应撑满宽度，显著提升移动端交互体验。

---

## 3. 影响范围与验证结果

- **修改文件**：
  - `src/volur-lite-view/src/views/competition/CompetitionListView.vue`
- **构建验证**：
  - 执行 `npm run build`（`vue-tsc --noEmit && vite build`），TypeScript 类型检查与 Vite 打包通过，无编译警告与错误。
