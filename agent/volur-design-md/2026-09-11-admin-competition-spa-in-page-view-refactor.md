# 管理端竞赛创建与编辑 SPA 页内页（In-Page Subview）重构审计说明

## 1. 变更背景与需求说明
在之前的管理端实现中，竞赛的创建与配置是通过弹出卡片模态框（`<n-modal preset="card">`）完成的。
然而网络安全竞技赛事（一赛一制架构）具有丰富的业务配置字段：
1. **基础信息与双时钟赛程**：竞赛名称、URL 短链接（Slug）、赛制模式（`CompetitionType`）、运行状态、比赛起止时间窗口与报名起止时间窗口、宣传海报封面、富文本描述；
2. **队伍竞技规则与防作弊机制**：队伍人数上限/下限、队伍总数上限、天梯榜封榜倒计时时长、战队人工审核开关、Writeup 赛后题解提交开关；
3. **单赛制引导与挂载指引**：选定赛制模式（CTF、THEORY、AWD、AWD+、ISW）的规则说明与后续公共题库赛题挂载操作指引。

在弹窗模态框中展示上述配置时，容易受限于窗口宽度产生多重滚动条或拥挤布局，且缺乏 SPA 单页应用深度沉浸式的操作体验。
为此，根据用户需求：**取消弹出 Card 的模态框方式，使用 SPA 页面内嵌视图（页内页）方式重构竞赛创建与编辑功能**。

---

## 2. 核心改动点

### 2.1 视图容器重构与 SPA 状态机控制 (`AdminCompetitionView.vue`)
1. **状态驱动的页内页切换**：
   - 引入 `viewMode = ref<'list' | 'form'>('list')`。
   - 移除原有 `<n-modal v-model:show="modalVisible" preset="card">` 弹窗。
   - 使用 `<transition name="page-transition" mode="out-in">` 容器包裹两套视口：
     - `list-container`（竞赛列表大厅）；
     - `form-container`（竞赛创建与编辑页内页）。
2. **页内页专属头部导航区 (`subview-header`)**：
   - 顶部提供快捷返回操作：带有 `ArrowBackOutline` 矢量的“返回竞赛列表”按钮；
   - 动态主标题（`创建新竞赛` / `编辑竞赛`）配合“一赛一制架构”圆角徽章（`n-tag`）；
   - 副标题与描述说明严格遵循 Volur 视觉规范，字体颜色采用 `#444444`；
   - 右侧常驻“取消”与“保存竞赛”（带 `SaveOutline` 图标，背景 `#000000`，字色 `#ffffff`）快捷操作。
3. **分段式 Tab 表单排版 (`form-card`)**：
   - **Tab 1: 基础信息与赛程**：基于 Naive UI `n-grid` 响应式栅格，布局标题、短链接、单一赛制下拉选框、状态、比赛时间跨度与报名时间跨度选择器；
   - **Tab 2: 队伍规则与竞技机制**：队伍规模限制数字输入框、封榜时长、审核开关与 Writeup 提交开关；
   - **Tab 3: 当前赛制指引**：根据当前选中的赛制模式（CTF / THEORY / AWD / AWD_PLUS / ISW），动态高亮展示赛制描述与后续赛题挂载操作指引；
   - 表单底部提供对齐的“返回列表”与“保存竞赛”主按钮。

### 2.2 逻辑与交互对齐
1. **创建入口 (`openCreateView`)**：
   - 清空表单，重置默认配置，将 `viewMode` 置为 `'form'`。
2. **编辑入口 (`openEditView`)**：
   - 切换到 `'form'` 模式，并异步调用 `getAdminCompetitionByIdApi(row.id)` 获取数据库完整配置，平滑回填赛程时间、队伍规则等配置。
3. **返回列表 (`backToList`)**：
   - 一键安全切回列表视图，无需破坏表单内部状态。
4. **提交与刷新 (`handleSubmit`)**：
   - 表单验证通过后，调用后端创建/更新 API，保存成功后自动切回 `'list'` 视图并静默刷新列表。

### 2.3 规范与设计遵循
1. **视觉规范（`view-Rules.md` 铁律）**：
   - 主操作按钮 `.theme-primary-btn` 显式声明：`background: #000000 !important; color: #ffffff !important;`；
   - 描述文本、副标题及说明文字颜色严格统一使用 `#444444`，杜绝纯黑 `#000000`；
   - 遵循 SPA 动效规范，采用轻量平滑的 `translateY(6px)` 页面切换动画。
2. **类型安全**：
   - 彻底杜绝 `any`，全面使用 `AdminCompetitionListVO`、`CompetitionType`、`CompetitionStatus`、`CompetitionCreateRequestDTO`、`CompetitionUpdateRequestDTO`、`CompetitionConfigDTO` 严格类型。

---

## 3. 验证与构建结果

### 3.1 前端工程验证 (`volur-lite-view`)
- 运行打包与类型检查命令：
  ```bash
  npm run build
  # 触发 vue-tsc --noEmit && vite build
  ```
- **构建结果**：
  - `vue-tsc` 静态类型检查 0 错误；
  - Vite 生成产物成功：`AdminCompetitionView-DVVxVwNS.js (18.26 kB)`，打包用时 4.13s，构建通过。

---

## 4. 影响范围与审计结论
- **影响范围**：仅限于管理端竞赛管理视图组件 [`AdminCompetitionView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/AdminCompetitionView.vue)，前后端 API 协议与领域模型保持 100% 稳定一致。
- **审计结论**：消除了模态框小窗口嵌套复杂表单的用户体验缺陷，提供了符合现代化 Web 竞技平台标准的页内页交互体系，完全符合 Volur Lite 视觉与前端工程设计规范。
