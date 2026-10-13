# 管理后台多列表视图 PageResponse 数据提取与分页类型规范化修复记录

## 1. 变更背景与缺陷根因分析
在管理后台列表页面（如 `AdminCompetitionListView.vue` 等）联调时，API 请求成功（HTTP 200，返回成功结果集），但前端表格渲染为空，且数据显示异常。

经审计全链路契约：
1. **分页契约定义差异**：
   - 后端统一分页模型 `PageResponse<T>` 为 Java Record：`record PageResponse<T>(List<T> list, long total, int totalPages, int currentPage)`，其列表数据属性名为 `list`；
   - 前端管理后台多个视图在 `fetchData` 解析响应时，错误读取了 `res.data.records`（该字段为 MyBatis-Plus 默认命名，在当前后端中为 `undefined`）；
   - 回退逻辑 `res.data.records || []` 导致解析出的数据源永远为空数组 `[]`，从而导致表格无数据渲染。
2. **总数字段类型精度与规范**：
   - 长整型和雪花 ID 在 Jackson 序列化时按统一规范转换为字符串（例如 `"total": "2"`）；
   - 前端 `pagination.itemCount` 直接赋值未进行显式数值类型转换（`Number(res.data.total) || 0`），存在与 Naive UI / VPagination 数值计算类型冲突的潜在隐患。

## 2. 改动详情

遵循规范中的“重构彻底性铁律”与“单一真实源原则”，全链路同步修复了管理后台所有相关视图中的分页解析逻辑，统一从 `res.data.list` 读取数据，并将 `total` 安全转换为 `number`：

### 2.1 竞赛管理模块
- **`AdminCompetitionListView.vue`**：
  - 将 `const records = res.data.records || []` 修改为 `const list = res.data.list ?? []`；
  - 赛制筛选 `filterType` 判定与数据绑定改为基于 `list`；
  - `pagination.itemCount` 更新为 `Number(res.data.total) || 0`。
- **`AdminCompetitionEditView.vue`**：
  - `openMountModal`：挂载题库题目列表提取改为 `repoChallenges.value = res.data.list ?? []`；
  - `fetchTeams`：报名战队列表改为 `teams.value = listRes.data.list ?? []`，`teamTotal` 改为 `Number(listRes.data.total) || 0`；
  - `fetchLogs`：竞赛审计日志改为 `compLogs.value = res.data.list ?? []`，`logTotal` 改为 `Number(res.data.total) || 0`。

### 2.2 题库与系统服务模块
- **`AdminCtfChallengeView.vue`**：
  - `challengeList.value = res.data.list ?? []`，`pagination.itemCount = Number(res.data.total) || 0`。
- **`AdminTheoryChallengeView.vue`**：
  - `theoryList.value = res.data.list ?? []`，`pagination.itemCount = Number(res.data.total) || 0`。
- **`AdminBroadcastView.vue`**：
  - `broadcastList.value = res.data.list ?? []`，`pagination.itemCount = Number(res.data.total) || 0`。
- **`AdminContainerView.vue`**：
  - `containerList.value = res.data.list ?? []`，`pagination.itemCount = Number(res.data.total) || 0`。
- **`AdminEmailView.vue`**：
  - `emailList.value = res.data.list ?? []`，`pagination.itemCount = Number(res.data.total) || 0`。
- **`AdminImageView.vue`**：
  - `imageList.value = res.data.list ?? []`，`pagination.itemCount = Number(res.data.total) || 0`。
- **`AdminLogView.vue`**：
  - `logList.value = res.data.list ?? []`，`pagination.itemCount = Number(res.data.total) || 0`。
- **`AdminTaskView.vue`**：
  - `taskList.value = res.data.list ?? []`，`pagination.itemCount = Number(res.data.total) || 0`。

## 3. 影响范围与验证
- 影响范围涵盖管理端 9 个业务视图的数据拉取与表格加载；
- 遵循零注释与代码紧凑铁律，代码中无任何冗余注释；
- 执行 `vue-tsc --noEmit` 进行 TypeScript 严格类型检查，检查通过无任何报错（退出码 0）。
