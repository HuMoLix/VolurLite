# 竞赛运维管理视图解耦拆分与组件标准化改造方案

## 1. 变更背景与设计动机
在平台管理端赛事运维模块中，原先的 `AdminCompetitionEditView.vue` 视图承担了多种异构赛制（解题赛 Jeopardy CTF、理论题库试卷 Theory 等）的编辑管理职责。这种混合模式导致代码体积庞大（超过 1500 行），内部存在大量赛制类型的分支判定（`v-if="formData.type === 'THEORY'"`），赛题挂载与试卷抽题规则相互耦合，维护成本高昂。

为了提升代码可维护性、降低组件复杂度并统一平台规范，现将该视图进行彻底物理拆分为两个独立的专业管理视图：
1. **`AdminCtfCompetitionEditView.vue`**：专门负责 CTF 解题赛的基本信息维护、赛题挂载与分值衰减管理、公告发布、战队资质审核、题解 (Writeup) 评阅与答题操作日志；
2. **`AdminTheoryCompetitionEditView.vue`**：专门负责理论答题赛的基本信息维护、引用题库绑定、试卷抽题规则配置（单选/多选/判断/问答配额与分值）、正式考题列表管理、公告、战队审核与日志。

同时，针对页面交互、控件规范和视觉层级进行全面现代化对齐：
- 删除标题栏冗余视觉元素（移除返回按钮、去除无意义的 `Slug: test` 文本、移除 `RUNNING` 胶囊标识）；
- 移除 CTF 管理页面中的手动“封锁天梯榜”按钮与相应逻辑；
- 统一视图内所有 `VBtn` 按钮尺寸为 `medium`；
- 将标签页切换重构为通用的 `VFilterBtnGroup` 控件；
- 全面标准化表单控件：输入框统一使用 `VInput`，下拉选择统一使用 `VSelection`，赛事介绍与规则采用 `VMarkdownEditor`；
- 新增横幅封面上传控件 `VBanner.vue`，支持点击查看原图弹窗与毛玻璃模糊背景；
- 基于 `VInput.vue` 与 `VBtn.vue` 封装通用日期时间选择组件 `VDatePicker.vue`，将起止时间收敛在单组件内进行范围选取；
- 将参赛队伍报名资质审核开关从基本信息抽离至队伍管理 Tab 中；
- 剔除理论考试中的题解 (WP) 管理，并将赛制及状态选项净化为纯中文，消除双语括号后缀。

---

## 2. 核心改动点说明

### 2.1 新增与重构通用基础控件
1. **`VBanner.vue` (`src/components/common/VBanner.vue`)**:
   - 参考 `VAvatar.vue` 的设计范式与状态机，支持图片预览、拖拽/点击上传、进度加载、清除与格式校验（支持 JPG/PNG/WebP/GIF，5MB 限制）；
   - **新增“查看原图”功能**：点击封面操作栏的眼睛图标，以 `<Teleport to="body">` 弹出视口居中的原图预览画廊；
   - **毛玻璃背景**：预览全屏蒙版采用 `backdrop-blur-md bg-black/75`，支持点击遮罩、右上角关闭按钮或键盘 `Escape` 键平滑退出。
2. **`VDatePicker.vue` (`src/components/common/VDatePicker.vue`)**:
   - **控件解耦对齐**：输入框全面采用 `VInput.vue`（支持前缀日历图标与只读点击响应），浮层操作栏的“清空”、“现在”与“确定”按钮全面采用 `VBtn.vue`；
   - **单双时间范围自适应**：支持 `type="datetimerange"` 与 `type="daterange"` 范围选择，内部采用两个 `VInput` 由“至”分隔形成一个整体控件框；
   - **双向数据绑定增强**：支持 `v-model:start-value` 与 `v-model:end-value`，以及 `v-model:value` 数组绑定，对外无损保持 ISO 8601 本地标准时间格式（`yyyy-MM-ddTHH:mm:ss`）。
3. **导出注册 (`src/components/common/index.ts` & `types.ts`)**:
   - 集中导出 `VBanner` 与 `VDatePicker`，并在 `types.ts` 中增补 `VBannerProps` 与 `VDatePickerProps` 接口定义。

### 2.2 CTF 竞赛运维视图 (`AdminCtfCompetitionEditView.vue`)
- **视觉去噪**：
  - 移除标题左侧的“返回列表”按钮；
  - 移除标题右侧的状态胶囊标识；
  - 移除标题下方的 `Slug: ...` 说明；
  - 彻底删除顶部“封锁天梯榜”/“解封天梯榜”按钮、相关接口引用 (`setFreezeScoreboardApi`) 与状态变量 (`freezing`)。
- **控件与组件替换**：
  - Tabs 导航采用 `VFilterBtnGroup` 组件，支持图标化选项卡渲染；
  - 赛事封面使用 `VBanner`，支持原图查看与模糊弹窗；
  - 简介与规则使用 `VMarkdownEditor`；
  - 竞赛开始与结束时间、报名开始与截止时间分别收敛为一个 `VDatePicker` 范围选择器；
  - 页面全部按钮尺寸统一重构为 `size="medium"`；
  - 状态与赛制选项移除英语后缀（改为纯中文：“草稿”、“已发布”、“报名中”、“进行中”、“已结束”、“已归档”）。

### 2.3 理论竞赛运维视图 (`AdminTheoryCompetitionEditView.vue`)
- **去除 WP 题解管理**：
  - 彻底删除 `writeups` 选项卡与对应数据请求 (`fetchWriteups`, `listAdminCompetitionWriteupsApi`)、表格与加载状态。
- **纯中文状态与题型标签**：
  - 消除双语模式，将题型与状态统一为简洁纯中文（如“已结束”、“单选题”、“多选题”、“判断题”、“问答题”）。
- **时间范围选择器整合**：
  - “竞赛起止时间”收敛在同一个 `VDatePicker` 框内；
  - “报名起止时间”收敛在同一个 `VDatePicker` 框内。
- **资质审核开关位置调整**：
  - 从 `basic` 基本信息 Tab 移出，迁移至 `teams` 战队审核 Tab 顶部独立卡片，采用 `n-switch` 开关控制 `formData.requireTeamAudit`。

### 2.4 路由架构与兼容调度 (`admin.ts` & `AdminCompetitionEditView.vue`)
- **专用路由直通**：
  - `/admin/competitions/ctf/create` 与 `/admin/competitions/ctf/:id/edit` 映射至 `AdminCtfCompetitionEditView`；
  - `/admin/competitions/theory/create` 与 `/admin/competitions/theory/:id/edit` 映射至 `AdminTheoryCompetitionEditView`。
- **列表页智能跳转 (`AdminCompetitionListView.vue`)**：
  - 点击赛事名称或“管理”按钮时，依据 `row.type === 'THEORY'` 自动分流跳转至理论或 CTF 专属编辑路径。
- **统一调度器保留 (`AdminCompetitionEditView.vue`)**：
  - 将原文件精简重构为动态调度路由组件（Router Dispatcher），确保历史链接 `/admin/competitions/:id/edit` 及通用创建路径 `/admin/competitions/create` 无缝分发，保障全链路高兼容性与平滑过渡。

### 2.5 后端 `java.time.Instant` 反序列化兼容与时间序列化改造
- **缺陷背景**：
  后端接口使用 `java.time.Instant` 映射赛事时间字段。若前端提交无时区偏移或 Zulu 标识的本地时间字符串（如 `"2026-10-13T12:00:00"`），Spring Boot Jackson 的 `JavaTimeModule` 在解析时会因缺少时区信息而在字符索引 19 处抛出 `DateTimeParseException: Text '2026-10-13T12:00:00' could not be parsed at index 19` 导致请求 400 失败。
- **治理方案**：
  1. **`VDatePicker.vue` 内部序列化改造**：对于 `datetime` 与 `datetimerange` 类型的选择结果，在点击“确定”时统一调用 `new Date(ts).toISOString()` 发射标准 ISO-8601 UTC Instant 字符串（如 `2026-10-13T04:00:00.000Z`）；输入框回显依然保持本地时区格式化展示（`yyyy-MM-dd HH:mm:ss`）；
  2. **视图层防御性转换函数 (`formatToInstant`)**：在 `AdminTheoryCompetitionEditView.vue` 与 `AdminCtfCompetitionEditView.vue` 的 `handleSaveBasic()` 提交载荷前，对 `startTime`、`endTime`、`regStartTime` 与 `regEndTime` 进行强类型转换，若缺少时区标识统一转为标准 ISO UTC 字符串，杜绝 400 反序列化报错。

---

## 3. 影响范围与回归测试建议
1. **构建与类型验证**：
   - 执行 `npm run build`（`vue-tsc --noEmit && vite build`），全工程通过严格类型检查，无构建告警；
2. **测试建议**：
   - 进入后台管理 -> 竞赛管理 -> 理论竞赛运维；
   - 验证无 WP 题解 Tab，仅展示基本信息、试卷考题、公告、战队审核与日志；
   - 验证状态与赛制下拉中为纯中文（无英文括注）；
   - 在基本信息中测试 Banner 图片上传与“查看原图”点击，验证居中模态框与毛玻璃模糊背景；
   - 测试“竞赛起止时间”与“报名起止时间”单个 `VDatePicker` 内的时间范围点选与回显；
   - 验证保存赛事时网络请求载荷中时间字段包含标准 `Z` 标识（如 `2026-10-13T04:00:00.000Z`），后端控制台无 `DateTimeParseException`，基本信息成功保存；
   - 切换到战队审核 Tab，验证“开启参赛报名战队资质审核”开关能正常交互；
   - 保存竞赛基本信息，验证数据持久化成功。
