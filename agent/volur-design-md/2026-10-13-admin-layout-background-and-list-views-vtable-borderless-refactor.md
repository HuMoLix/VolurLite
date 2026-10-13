# 管理后台布局背景与多列表视图 VTable 无边框风格重构记录

## 1. 变更背景
为了进一步统一下一代管理后台（`volur-lite-view-NEXT`）的视觉层次与阅读体验：
1. `AdminLayout.vue` 中主内容区域 `.admin-main` 需统一设置为纯白背景（`#fff`），以与卡片容器及无边框表格形成清晰通透的底色契合；
2. 管理后台各核心业务列表视图（竞赛管理、系统广播、异步任务、系统日志、邮件记录、CTF 题库、理论题库、容器实例、Docker 镜像、IP 风控）中的 `VTable.vue` 组件，统一配置 `:bordered="false"`，实现无外边框的现代清爽视觉风格，与 `AdminUserView.vue` 等已对齐视图保持一致。

## 2. 改动详情

### 2.1 管理后台主布局 (`src/components/layouts/AdminLayout.vue`)
- 调整 `.admin-main` 样式中的背景颜色声明为 `background-color: #fff;`；
- 保持暗色模式下 `html.dark &` 的背景色变量适配（`#202124`）。

### 2.2 核心业务列表视图统一配置 `:bordered="false"`
在以下 10 个管理后台核心列表视图中，为 `<VTable>` 显式添加 `:bordered="false"`：
1. **`AdminCompetitionListView.vue`**（竞赛管理列表）
2. **`AdminBroadcastView.vue`**（系统广播与运营公告列表）
3. **`AdminTaskView.vue`**（异步任务列表）
4. **`AdminLogView.vue`**（系统审计与操作日志列表）
5. **`AdminEmailView.vue`**（邮件外发与投递记录列表）
6. **`AdminCtfChallengeView.vue`**（CTF 解题题目库管理列表）
7. **`AdminTheoryChallengeView.vue`**（理论题库管理列表）
8. **`AdminContainerView.vue`**（动态靶机容器实例调度列表）
9. **`AdminImageView.vue`**（Docker 靶机镜像资产列表）
10. **`AdminRiskIpView.vue`**（IP 风控治理黑白名单列表）

## 3. 影响范围与验证
- 影响范围仅限于管理后台布局背景与指定的 10 个管理端列表视图表格外框样式。
- 经 `vue-tsc --noEmit` 静态类型验证，全部类型检查通过（退出码 0），无任何编译或类型错误。
