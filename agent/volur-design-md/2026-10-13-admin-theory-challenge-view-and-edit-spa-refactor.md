# 理论题库管理列表样式对齐与 SPA 题目管理页面重构记录

## 1. 变更背景与需求说明
在管理后台理论赛题管理模块中，原有 `AdminTheoryChallengeView.vue` 将题库列表与题库下题目的增删改查混合在同一个视图中，采用弹窗（Modal）的方式管理题目，且列表样式与 `AdminCtfChallengeView.vue` 不一致：
1. **列表表格样式**：原题库名称未采用规范的 `redirectable` 样式；题目数量使用了胶囊 Badge 样式包裹；创建时间和更新时间使用了 `font-mono` / Fira Code，未直接继承 `VTable.vue` 的原生排版基准；
2. **题目管理交互**：管理题目通过多层嵌套 Modal 展开，交互空间局促且无法支持通过 URL 直接导航定位；
3. **编辑题库弹窗**：在编辑题库基础信息（名称、分类、描述）的 Modal 内混合了“向本题库导入题目”的复合动作，职责不够单一；
4. **题目编辑弹窗与选项交互**：原有题目创建/编辑模态框内嵌在页面中，选项需通过手写 JSON 字符串录入，缺乏可视化选项增删与标准答案直观指定体验；题目分值由试卷规则决定，不应在单题编辑中硬编码。

按照规划，参照 `AdminCtfChallengeView.vue`、`AdminCtfChallengeEditView.vue` 与 `AdminUserView.vue` 进行全面重构。

---

## 2. 核心改动点

### 2.1 题库列表视图样式重构 (`AdminTheoryChallengeView.vue`)
- **题库名称 (`name`)**：
  - 采用 `redirectable` CSS 样式类及 `fontWeight: '500'`；
  - 点击跳转至对应题库的独立 SPA 编辑页面 `/admin/challenges/theory/${row.id}/edit`；
  - 题库分类以浅灰色次要文字呈现在题库名称下方。
- **题目数量 (`questionCount`)**：
  - 彻底移除 `px-2 py-0.5 rounded-full ... font-mono ...` 药丸胶囊样式包裹；
  - 直接渲染数值，继承 `VTable.vue` 的表格单元格字体与排版样式。
- **时间列 (`createdAt` / `updatedAt`)**：
  - 移除 `font-mono` / Fira Code 样式包裹；
  - 直接使用 `new Date(...).toLocaleString()` 格式化并继承 `VTable.vue` 默认字族规范。
- **操作列 (`actions`)**：
  - “管理题目”按钮点击直接导航至 `/admin/challenges/theory/${row.id}/edit`；
  - 保留“编辑”（弹出编辑题库基本信息弹窗）与“删除”按钮。
- **编辑弹窗精简**：
  - 彻底删除 `showBankModal` 中“向本题库导入题目”区域与对应导入按钮；
  - 清理相关的 `importingQuestions`、`triggerImportQuestionForCurrentBank` 等状态。
- **移除冗余题目 Modal 与状态**：
  - 移除原视图内内嵌的题目管理 Modal（`showManageModal`）和题目编辑 Modal（`showQuestionModal`）；
  - 移除相关题型与题目状态，显著缩减单文件复杂度。

### 2.2 独立 SPA 题目管理页面实现 (`AdminTheoryChallengeEditView.vue`)
- **路由挂载**：在 `src/router/routes/admin.ts` 中注册路由 `path: 'challenges/theory/:id/edit'`，组件为 `AdminTheoryChallengeEditView.vue`；
- **顶部 Header**：删除标题左侧的“返回”按钮，保留题库名称、分类徽章和描述信息；
- **一体化工具栏布局**：
  - 参照 `AdminUserView.vue` 工具栏规范，将搜索栏（`VSearch`）、题型筛选（`VSelection`）、搜索与重置按钮（`VBtn`）以及“导入题目”、“导出题目”、“新增题目”功能操作按钮置于同一 Flex 容器中；
  - 功能操作按钮使用 `sm:ml-auto` 置右排放，结构紧凑且适配移动与桌面端；
- **题目表格样式继承**：
  - 确保题目列表 `VTable` 所有字段均直接继承 `VTable.vue` 基础样式，杜绝任何 `font-mono` / Fira Code 或胶囊显示；
  - 题目名称采用 `redirectable` 样式类支持点击唤起编辑弹窗；
- **题目列表表头顺序对齐**：
  - 表头列严格按指定顺序排列：`勾选列` -> `题目名称` -> `题目题型` -> `所属方向` -> `创建人` -> `创建时间` -> `操作`；
- **受控分页组件集成**：
  - 列表底部接入 `VPagination.vue` 组件，支持每页条数切换与快速跳转，显示数据范围与总条数。

### 2.3 抽离题目创建与编辑模态框组件 (`AdminTheoryCreateChallengeModal.vue`)
- **独立组件抽离**：提取至 `src/views/admin/challenge/components/AdminTheoryCreateChallengeModal.vue`，实现题目创建与编辑逻辑内聚解耦；
- **基础控件规范**：全模态框严格统一采用平台通用组件：
  - 输入框统一使用 `VInput.vue`（支持单行、textarea、clearable）；
  - 题型选择框统一使用 `VSelection.vue`；
  - 动作与交互按钮统一使用 `VBtn.vue`（彻底替换原生 `<button>`）；
  - 弹窗外壳统一使用 `VModal.vue`；
- **题目选项与正确答案按钮全面采用 `VBtn.vue`**：
  - 单选与多选选项行前部的字母选项标记全面重构为 `VBtn`（选定后呈现 `clz="primary"` 蓝色高亮并带对勾图标 `CheckmarkOutline`，未选定呈现 `clz="normal"`）；
  - 选项行右部设置答案状态按钮统一采用 `VBtn`（`正确答案` / `设为答案`）；
  - 判断题（`TRUE_FALSE`）中的“正确 (True)”与“错误 (False)”选项卡片按钮全面采用 `VBtn` 进行交互切换，杜绝任何裸原生按钮；
- **可视化选项列表交互（彻底废除 JSON 输入）**：
  - 单选题（`SINGLE_CHOICE`）与多选题（`MULTIPLE_CHOICE`）由原 JSON 文本框全面重构为可视化选项列表（支持 2~10 项动态添加与删除，选项字母自动重排）；
  - 选项内容直接通过 `VInput` 录入；
- **在选项列表中直接指定标准答案**：
  - 单选题直接在选项行前点击单选圈指定唯一标准答案；
  - 多选题直接在选项行前勾选一个或多个正确答案，系统自动生成排序后的标准答案（如 `A,B`）；
  - 只有主观问答题（`ESSAY`）展示“参考标准答案 / 裁判评判标准”文本域输入，选择题与判断题无需重复录入答案；
- **彻底删除题目分值字段**：
  - 移除 `score` 分值字段输入框，题目分值完全交由答题考试试卷规则定义。

### 2.4 通用复选框组件设计与实现 (`VSelectBox.vue`)
- **全新通用组件**：在 `src/components/common/VSelectBox.vue` 中封装符合 Google Material Design 体系的通用复选框组件，并在 `types.ts` 与 `index.ts` 中集中导出 `VSelectBox` 与 `VSelectBoxProps`；
- **边框与 Hover 样式严格对齐 `VInput.vue`**：
  - 默认状态边框：日间模式 `1px solid #d7d8da80`，夜间模式 `1px solid #262626`；
  - Hover / Focus 状态边框：日间模式平滑过渡至 `#b7babd`，夜间模式平滑过渡至 `#333`；
  - 选中高亮状态：日间模式使用 Google Blue `#1a73e8` 背景与白对勾图标，夜间模式使用 Google Blue Dark `#8ab4f8` 背景与深色对勾；
  - 禁用状态：日间 `#f5f6f7` 底色，夜间 `rgba(255, 255, 255, 0.04)` 底色，光标变为 `not-allowed`；
- **属性与交互规范**：
  - 支持 `checked`、`modelValue`（双向绑定）、`disabled`、`size`（`small` / `medium` / `large`）、`round`（圆角胶囊/圆形单选风格）、`radius` 及可选标签文本插槽；
  - 原生无障碍支持（`role="checkbox"`、`aria-checked` 与空格/回车键快捷操作）。

### 2.5 题目选项列表结构精简与按钮重构 (`AdminTheoryCreateChallengeModal.vue`)
- **左侧仅保留复选框**：
  - 选项行左侧原有的复合字母按钮（`VBtn`）替换为独立的 `<VSelectBox :checked="isOptionCorrect(opt.key)" @change="toggleOptionAnswer(opt.key)" />`；
  - 紧随复选框其后保留字母标记（`A.`、`B.` 等）与选项文本输入框 `VInput`；
- **彻底删除右侧冗余答案按钮**：
  - 移除每一行右侧原有的“设为答案”与“正确答案”胶囊按钮（`VBtn`），界面更加紧凑扁平、直观清爽；
### 2.6 题型选择框规范化与单语说明对齐 (`AdminTheoryCreateChallengeModal.vue`)
- **控件规范**：题型选择框严格采用平台通用组件 [`VSelection.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/common/VSelection.vue)，配置 `class="w-full"`、占位符提示及 `@update:value` 强类型处理；
- **双语后缀清理为纯净单语**：
  - 彻底清理原各选项中夹带的括号英文命名（如 `单项选择题 (SINGLE_CHOICE)`）；
  - 全面统一为纯净单语标准：`单项选择题`、`多项选择题`、`判断题`、`主观问答题`；
  - 配合 `t('admin.challenges.type_*', ...)` 机制支持完整国际化动态切换；
  - 同步对齐 [`AdminTheoryChallengeEditView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/admin/challenge/AdminTheoryChallengeEditView.vue) 中题目列表 `getTypeName` 的中文映射，确保全模块命名规范高度一致。

---

## 3. 影响范围与验证结果
- `src/components/common/VSelectBox.vue`：新建通用复选框组件；
- `src/components/common/types.ts` & `src/components/common/index.ts`：导出 `VSelectBox` 与类型；
- `src/views/admin/challenge/AdminTheoryChallengeView.vue`：精简重构；
- `src/views/admin/challenge/AdminTheoryChallengeEditView.vue`：独立 SPA 页面样式、工具栏对齐与组件解耦，题型列名称对齐；
- `src/views/admin/challenge/components/AdminTheoryCreateChallengeModal.vue`：新建可视化选项管理与答案选择模态框组件，选项行左侧接入 `VSelectBox`，删除右侧多余设为答案按钮，题型选择器对齐 `VSelection` 并采用纯单语说明；
- `src/router/routes/admin.ts`：路由注册；
- Vite 生产构建多次验证通过（`built in 31.24s`，退出码 0），TypeScript 类型校验无任何报错与警告。
