# CTF 队伍视图表格与头像组件重构设计方案

## 1. 变更背景与目标
在 CTF 赛事大厅的队伍详情页面（`src/views/competition/tabs/CtfCompetitionTeamView.vue`）中：
1. 参赛成绩（选手战绩）与答题记录（解题记录）此前采用手写原生 `<table>` 元素渲染，存在样式不统一、缺乏通用空状态及分页协同能力等问题。
2. 队伍头像（`VAvatar`）无法直接进行用户交互式更换。需要支持编辑模式（`:editable="true"`），在用户点击头像时唤起本地文件系统选择框并自动上传至服务端。
3. 能力雷达图表下方展示的冗余文本（“总积分: 2200 pts” 与 “总解出: 2 道”）信息密度分散，需予以移除。
4. 全局通用表格组件（`VTable.vue`）与排行榜（`CtfCompetitionScoreboardView.vue`）排版规范统一：
   - 整体字体统一为 `Inter, sans-serif`；
   - 普通表头使用 `color: #202124`，`font-size: 13px`；
   - 切分后的一级标题（赛题分类）使用 `12px`、`font-weight: 700`；二级标题（赛题名称）使用 `12px`、`font-weight: 500`；三级标题（分值）使用 `10px`、`font-weight: 500`；
   - 单元格数据内容使用 `color: #3c4043`、`font-weight: 500`。
5. 队伍视图中的“答题方式”、“题目类型”、“得分”、“是否一血”等列去除手写外挂徽章（Badge/Pill）和 `font-mono`，统一继承 `VTable` 基础字体与颜色规范。
6. 表格列对齐与格式精炼：
   - 答题记录（`solveColumns`）全部列统一采用 `align: 'center'` 居中对齐；
   - 答题记录中的得分由带有单位符号的 `+1100 pts` 调整为纯数字 `1100`；
   - 是否一血由 `'一血' / '否'` 精炼为标准布尔描述 `'是' / '否'`；
   - 参赛成绩（`memberColumns`）中的“角色、强项、解题数、一血数量、得分”均统一居中对齐，删除队长/队员角色的彩色胶囊背景直接使用纯文本，得分由 `2000 pts` 调整为纯数字 `2000`；
   - 表格内部完全去除额外粗体与颜色覆盖，纯粹继承 `VTable` 预设样式。
7. 答题记录下方的分页功能完整交付：
   - 修复此前仅在 `teamSolves.length > pageSize` 时才展示分页组件导致小样本数据下分页栏消失的体验缺陷；
   - 当 `teamSolves.length > 0` 时常驻展示精美分页栏，支持条目范围摘要展示（如 `显示第 1 - 10 条，共 25 条`）；
   - 支持动态每页容量切换（`show-size-picker` 10/20/50 条）与快速跳页（`show-quick-jumper`）；
   - 添加 `watch(pageSize)` 响应式监听，重置当前页码至第 1 页，避免切页后页码越界。

---

## 2. 核心改动点

### 2.1 头像组件 `VAvatar.vue` 与类型定义
- **扩展类型**：在 `src/components/sidebar/types.ts` 中的 `VAvatarProps` 添加 `editable?: boolean`、`uploadFolder?: string`、`accept?: string` 与 `uploadApi?: (file: File) => Promise<any>`。
- **编辑交互与上传**：
  - 当 `editable=true` 时，鼠标悬停显示半透明暗色浮层与相机编辑图标（`CameraOutline`），同时挂载不可见 `<input type="file" ref="fileInputRef" />`；
  - 点击头像触发文件选择，校验格式（JPG/PNG/WebP/GIF）与大小限制（<= 5MB）；
  - 调用 `uploadImageApi` 异步上传，上传中展示 `NSpin` 转圈状态；
  - 上传成功后通过 `update:src`、`upload-success`、`change` 事件通知父组件并回显。

### 2.2 队伍视图 `CtfCompetitionTeamView.vue` 迁移与精简
- **表格替换**：将参赛成绩与答题记录由原生 `<table>` 整体重构成 `<VTable>` 组件；
- **头像可编辑**：主队伍头像绑定 `:editable="true"`，监听 `@upload-success="handleTeamAvatarUploaded"` 同步更新队伍头像至数据库；
- **能力雷达精炼**：删除雷达图下方“总积分”和“总解出”冗余文案；
- **列样式去定制化与纯文本继承**：
  - 移除原表格及列定义中的 `font-mono` 与 `font-bold` 等字体覆盖类；
  - 答题记录与参赛成绩中删除所有徽章类函数（如 `getRoleBadgeClass`），角色直接输出纯文本 `'队长'` / `'领队'` / `'队员'`；
  - 分值（`points`、`score`）直接返回纯数字（`row.points ?? 0`、`row.score ?? 0`），无需多余符号与单位，纯净继承 `VTable` 样式。

### 2.3 居中对齐与格式标准化
- **答题记录列表（`solveColumns`）**：
  - 序号、答题时间、答题人、题目名称、答题方式、题目类型、得分、是否一血所有 8 列统一配置 `align: 'center'`；
  - 是否一血判定统一为 `row.isFirstBlood ? '是' : '否'`。
- **参赛成绩列表（`memberColumns`）**：
  - 角色、强项、解题数、一血数量、得分 5 列全部配置 `align: 'center'`；
  - 选手名称列保持左对齐并渲染头像与名称组合。

### 2.4 答题记录分页能力升级
- 在 `VTable` 的 `#footer` 插槽中配置自适应分页栏；
- 条件放宽为 `teamSolves.length > 0`，左侧输出条目范围详情（`显示第 (currentPage - 1) * pageSize + 1 - min(...) 条，共 N 条`），右侧提供 `NPagination`；
- 开启 `show-size-picker`（`[10, 20, 50]`）与 `show-quick-jumper`；
- 增加 `watch(pageSize, () => { currentPage.value = 1 })` 防越界监听。

### 2.5 `VTable.vue` 排版与多级表头统一
- 全局注入 `font-family: Inter, sans-serif`；
- 通过 `themeOverrides` 与 Deep SCSS 明确各级表头排版层级：
  - 普通单级表头：`font-size: 13px; font-weight: 500; color: #202124;`（暗色 `#e3e3e3`）；
  - 一级切分标题：`font-size: 12px; font-weight: 700; color: #202124;`；
  - 二级切分标题：`font-size: 12px; font-weight: 500; color: #202124;`；
  - 三级分值标题：`font-size: 10px; font-weight: 500;`；
  - 单元格内容：`font-size: 13px / 14px / 15px; font-weight: 500; color: #3c4043;`（暗色 `#e8eaed`）。

---

## 3. 影响范围与规范对齐
- **模块对齐**：完全遵循 `GEMINI.md` 与 `view-NEXT-rules.md`；
- **色彩体系**：严格对齐 Google Workspace / Material Design 灰阶（`#202124` / `#3c4043` / `#f8f9fa` / `#e8eaed`）；
- **代码整洁**：严禁在代码中遗留任何行内解释性注释，保证代码自解释性。
