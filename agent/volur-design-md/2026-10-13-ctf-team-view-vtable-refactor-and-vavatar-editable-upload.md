# CTF 队伍视图 VTable 重构、VAvatar 头像修改与能力雷达精炼报告

## 1. 变更背景与需求概述

在 Volur-Lite CTF 竞赛系统中，战队详情视图（`CtfCompetitionTeamView.vue`）承担着展示队伍基本信息、核心数据统计、队员个人战绩以及队伍解题流水的关键职责。为进一步统一平台 UI 组件规范、提升用户交互体验并精简图表信息密度，本次变更执行以下核心改造：
1. **统一列表组件规范 (`VTable`)**：将“参赛成绩”与“答题记录”两个板块中原有的手写原生 HTML `<table>` 彻底重构为平台标准通用列表组件 [`VTable.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/common/VTable.vue)，继承统一的高对比度表头、边框阶梯、等宽字体与水平滚动排版；
2. **头像编辑与文件上传集成 (`VAvatar`)**：分析通用头像组件 [`VAvatar.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/sidebar/VAvatar.vue)，引入 `:editable` 参数。当 `editable` 为 `true` 时，鼠标悬停呈现微交互遮罩与相机图标，用户点击后唤起系统文件选择对话框（File Picker），选择图片后自动调用系统上传接口 `uploadImageApi` 上传至服务端；并在 `CtfCompetitionTeamView.vue` 中将战队主头像配置为 `:editable="true"`；
3. **能力雷达图文案精简**：移除分值雷达图与解题数雷达图下方的“总积分: xxx pts”与“总解出: x 道”冗余字样，使图谱呈现更紧凑纯粹。

---

## 2. 核心改动点与实现细节

### 2.1 VAvatar 头像组件添加 `:editable` 参数与上传集成
- **组件文件**：[`src/components/sidebar/VAvatar.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/sidebar/VAvatar.vue)
- **类型声明**：[`src/components/sidebar/types.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/sidebar/types.ts)
- **具体改造**：
  1. 在 `VAvatarProps` 接口中扩充属性：`editable?: boolean`（默认 `false`）、`uploadFolder?: string`（默认 `'avatars'`）、`accept?: string`、`uploadApi?: (file: File) => Promise<any>`；
  2. 扩展事件声明：`update:src`、`upload-success`、`upload-error`、`change`；
  3. 模板新增 `.v-avatar-edit-overlay` 遮罩层与相机图标（`CameraOutline`）及上传加载动画（`NSpin`），并在内部挂载受控的隐藏文件选择框 `<input type="file" ref="fileInputRef" />`；
  4. 当 `editable` 为 `true` 时，鼠标悬停展示半透明浮层与提示，点击触发 `triggerUpload()` 唤起系统文件选择窗口；
  5. 校验文件格式（支持 JPG、PNG、WebP、GIF）与文件大小（<= 5MB），通过平台系统上传 API `uploadImageApi` 进行文件传输；
  6. 上传成功后自动刷新内部响应式展示并在触发 `upload-success` 事件的同时给予全局轻量通知反馈；
  7. 对外暴露 `hasError` 与 `triggerUpload` 方法。

### 2.2 CtfCompetitionTeamView 战队头像启用编辑
- **视图文件**：[`src/views/competition/tabs/CtfCompetitionTeamView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/competition/tabs/CtfCompetitionTeamView.vue)
- **具体改造**：
  1. 将第一层队伍名称旁的 `VAvatar` 设置 `:editable="true"`；
  2. 移除原有的 `v-if="displayAvatarUrl"` 条件限制，配置首字母兜底文案，使得无头像战队亦能正常展示圆环头像并允许点击更换；
  3. 监听 `@upload-success="handleTeamAvatarUploaded"` 事件，头像更新后实时同步 `activeTeamData.avatarUrl`，并在是本队时自动调用 `updateMyTeamApi` 同步更新战队头像至后端数据库。

### 2.3 参赛成绩与答题记录列表全面迁移为 VTable
- **视图文件**：[`src/views/competition/tabs/CtfCompetitionTeamView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/competition/tabs/CtfCompetitionTeamView.vue)
- **具体改造**：
  1. **参赛成绩板块**：
     - 使用 `<VTable :bordered="false" size="medium" :columns="memberColumns" :data="memberAchievements" :loading="loading" :scroll-x="760">` 代替手写 `<table>`；
     - 结构化声明 `memberColumns`（`DataTableColumns<MemberAchievement>`）：序号（居中）、选手名称（含头像与姓名）、角色（高亮胶囊）、强项（题型标签）、解题数（等宽加粗）、一血数量（红字强调）、得分（蓝字强调）；
     - 插槽 `#empty` 保持统一的“暂无参赛选手成绩数据”空状态。
  2. **答题记录板块**：
     - 使用 `<VTable :bordered="false" size="medium" :columns="solveColumns" :data="paginatedSolves" :loading="loading" :scroll-x="920">` 代替手写 `<table>`；
     - 结构化声明 `solveColumns`（`DataTableColumns<CtfTeamSolveRecordVO>`）：序号、答题时间（等宽次级灰）、答题人、题目名称、答题方式（常规/容器）、题目类型（Misc/Web 等）、得分（绿字）、是否一血（带图标的一血胶囊）；
     - 利用 `#footer` 插槽与 `n-pagination` 无缝组合，保留完整分页功能（10/20/50 条每页与当前页切换）。

### 2.4 能力雷达区域冗余文案精简
- **视图文件**：[`src/views/competition/tabs/CtfCompetitionTeamView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/competition/tabs/CtfCompetitionTeamView.vue)
- **具体改造**：
  - 删除了分值雷达图下方的 `<div class="text-xs text-google-text-muted ...">总积分: {{ totalScore }} pts</div>`；
  - 删除了解题数雷达图下方的 `<div class="text-xs text-google-text-muted ...">总解出: {{ totalSolvesCount }} 道</div>`；
  - 雷达图两两并排，视觉效果更加纯粹。

---

## 3. 验证与审计

1. **零注释铁律遵守**：所有修改与新增的 TypeScript 及 Vue 代码中绝无任何行内注释与解释性备注，严格保证代码纯度；
2. **静态类型检查 (`vue-tsc --noEmit`)**：类型校验通过，代码零错误、零 TS 告警；
3. **构建测试 (`npm run build`)**：打包无任何报错。
