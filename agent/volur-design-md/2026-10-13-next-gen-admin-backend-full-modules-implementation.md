# 新一代前端管理后台全模块架构设计与落地实施文档

## 1. 变更背景与需求概述

本项目致力于将经典管理后台（`src/volur-lite-view`）能力全面迁移并重构至基于现代技术栈（Vue 3 + Vite + TypeScript + SCSS + Tailwind CSS + Naive UI）的新一代前端工程（`src/volur-lite-view-NEXT`）。

在第一阶段完成了后台管理基础框架、管理员路由守卫（`requiredAdmin`）、侧边栏底栏（Bottom Dock）平台管理入口、管理员控制台（Dashboard）、用户管理（Users）与系统动态设置（System）之后，本阶段全面完成了剩余 8 个核心业务治理模块：
1. **题库管理 (Challenge Management)**：涵盖 CTF 题库列表与编辑、题目分类字典模态框、ZIP 题目批量导入与导出、理论题库（单选/多选/判断/简答）、以及 ISW、AWD、AWD+ 赛题演练预留视图；
2. **竞赛管理 (Competition Management)**：竞赛大厅管理列表、多标签页全方位比赛运维视图（基本信息、赛题挂载/解挂与分值动态衰减、站内公告发布与置顶、参赛队伍审核/驳回与成员透视、Writeup 题解提交与批阅、比赛专属操作审计日志）；
3. **公告管理 (Broadcasts Management)**：全局系统公告发布、活动促销弹窗运营、横幅投放与 SSE 实时事件总线监控；
4. **集群管理 (Cluster Management)**：调度策略配置、Docker/Kubernetes 多节点拓扑与连通性嗅探、容器实例生命周期管理（启动/停止/批量终止/续期/exec 终端实时调试）、集群镜像资产管控（导入/拉取/迁移/导出）；
5. **系统日志 (System Logs)**：管理员全站审计日志、模块与等级筛选、明细深度核验；
6. **任务管理 (Task Center)**：异步批处理任务监控中心、实时进度环比、任务熔断取消与历史清理；
7. **邮件管理 (Email Management)**：发信记录与投递状态审计、实时投递统计、手动重试与紧急邮件发送；
8. **风控管理 (Risk Governance)**：风控引擎开关、限流与惩罚阶梯策略配置、IP 黑白名单治理与实时行为特征诊断探测（TLS、设备指纹、速率与失败溯源）。

---

## 2. 核心架构与设计决策

### 2.1 铁律遵守与质量基准
- **文档外置与零注释铁律 (Zero-Comment Rule)**：所有 `.vue`、`.ts`、`.scss` 代码文件均未书写任何多余解释性行内或块级注释，代码结构通过高内聚自解释命名呈现；
- **全链路杜绝隐式 Mock 与快速失败 (Fail-Fast & Zero-Mock 原则)**：所有 API 均真实接入后端 `/api/v1/admin/*` 接口，对于失败响应与网络异常均通过 Naive UI Message 组件直接报错并中断流转；
- **平台标准化公用组件体系**：
  - 按钮组件统一采用 `VBtn`（支持 `clz: 'primary' | 'normal' | 'warning' | 'danger'` 与 `size: 'tiny' | 'small' | 'medium' | 'large'`）；
  - 表格数据统一采用 `VTable`（配合 Naive UI `DataTableColumns` 渲染与响应式分页）；
  - 模态弹窗统一采用 `VModal`（支持 `:panel-style="{ maxWidth: '...' }"` 与 `#footer` 受控插槽）；
  - 输入框与选项框统一采用 `VInput` 与 `VSelection`；
  - 检索栏统一采用 `VSearch`。
- **Google Material Design 色彩体系与无全黑正文铁律**：
  - 主品牌色采用 Google Blue (`#1a73e8` / `#8ab4f8`)；
  - 状态辅助色采用 Google Red (`#ea4335` / `#f28b82`)、Google Green (`#34a853` / `#81c995`)、Google Yellow (`#fbbc04` / `#fdd663`)；
  - 表面阶梯亮色为浅灰 `#f8f9fa` 与纯白卡片 `#ffffff`，暗色为 Google Workspace 灰黑阶梯（`#202124` / `#292a2d` / `#303134` / `#3c4043`）；
  - 严禁正文出现全黑 `#000`。

---

## 3. 改动点与模块清单

### 3.1 实体类型层 (`src/types/`)
- `src/types/challenge.ts`：定义 CTF 赛题、分类、附件、理论题库题目模型及导入导出模型；
- `src/types/competition/index.ts`：补充管理端赛事列表、运维详情、队伍批量审核、赛题挂载、比赛公告与比赛操作日志等模型；
- `src/types/notification.ts`：扩充系统公告发布、促销运营配置与统计概览模型；
- `src/types/cluster.ts`：定义集群节点拓扑、通信协议、调度配置与连通性测试模型；
- `src/types/container.ts`：定义容器实例、生命周期动作、exec 执行结果及统计模型；
- `src/types/image.ts`：定义集群镜像资产、分层元数据、拉取/迁移/导出任务模型；
- `src/types/log.ts`：定义管理员审计日志、查询入参与统计模型；
- `src/types/task.ts`：定义异步任务中心任务状态、类型与统计模型；
- `src/types/email.ts`：定义发信日志、发信统计与手动发信 DTO；
- `src/types/risk.ts`：定义风控基础设置、防御策略规则、IP 治理规则、实时行为诊断探测模型；
- `src/types/common/api.ts`：对 `PageResponse<T>` 增加兼容字段支持（`records`, `content`, `totalElements`）。

### 3.2 强类型 API 层 (`src/api/`)
- `src/api/challenge.ts`：CTF 题目 CRUD、分类管理、附件上传、题目导出及 ZIP 导入；
- `src/api/competition.ts`：管理端赛事列表、运维详情更新、归档/取消归档、赛题挂载/解挂、队伍审核/驳回、比赛公告发布及 WP 批阅；
- `src/api/notification.ts`：获取管理端广播统计、广播列表、发布公告、发布促销活动及撤销删除；
- `src/api/cluster.ts`：集群调度配置获取与保存、节点 CRUD、心跳与节点连通性测试；
- `src/api/container.ts`：容器统计、实例分页列表、手动启动、exec 命令执行、容器续期、单实例/批量停止；
- `src/api/image.ts`：镜像资产统计、镜像分页列表、拉取、迁移至节点、导出及清理；
- `src/api/log.ts`：管理员审计日志列表、日志级别统计与详情查看；
- `src/api/task.ts`：异步任务列表、任务统计、取消执行、删除记录与批量清空历史；
- `src/api/email.ts`：发信统计、发信记录分页、手动发送与失败重试；
- `src/api/risk.ts`：风控总开关于设置更新、防御规则阈值配置、IP 规则增删撤销、实时 IP 诊断探测；
- `src/utils/request.ts`：强类型包装 `request.get`、`post`、`put`、`delete`、`patch` 及 `download` 方法，确保与 `ApiResponse<T>` 契约强类型完全契合。

### 3.3 视图层 (`src/views/admin/`)
- **题库模块**：
  - `challenge/AdminCtfChallengeView.vue`
  - `challenge/AdminCtfChallengeEditView.vue`
  - `challenge/AdminTheoryChallengeView.vue`
  - `challenge/AdminIswChallengeView.vue`
  - `challenge/AdminAwdChallengeView.vue`
  - `challenge/AdminAwdPlusChallengeView.vue`
  - `challenge/components/CtfCategoryModal.vue`
  - `challenge/components/CtfImportModal.vue`
- **竞赛模块**：
  - `competition/AdminCompetitionListView.vue`
  - `competition/AdminCompetitionEditView.vue`
- **公告模块**：
  - `broadcast/AdminBroadcastView.vue`
- **集群与容器镜像模块**：
  - `cluster/AdminClusterSettingsView.vue`
  - `container/AdminContainerView.vue`
  - `image/AdminImageView.vue`
- **日志模块**：
  - `log/AdminLogView.vue`
- **任务模块**：
  - `task/AdminTaskView.vue`
- **邮件模块**：
  - `email/AdminEmailView.vue`
- **风控模块**：
  - `risk/AdminRiskSettingsView.vue`
  - `risk/AdminRiskRulesView.vue`
  - `risk/AdminRiskIpView.vue`

### 3.4 路由与导航系统
- `src/router/routes/admin.ts`：注册所有 8 大模块子路由，统一注入 `meta: { requiredAuth: true, requiredAdmin: true, roles: ['ROLE_ADMIN', 'ROLE_SUPER_ADMIN'] }`；
- `src/router/index.ts`：全局路由守卫严格拦截非管理员账号访问 `/admin/*` 路由；
- `src/components/layouts/AdminLayout.vue`：全量配置 `adminMenuOptions` 导航拓扑（树形分组、子菜单折叠、高亮匹配 `activeKey`）。

### 3.5 国际化多语言
- 整合历史语言资产并平滑补充全模块文案至：
  - `src/locales/zh-CN/admin.json`
  - `src/locales/en-US/admin.json`
  - `src/locales/zh-TW/admin.json`

---

## 4. 验证与构建结果

在 `src/volur-lite-view-NEXT` 根目录下执行了全量构建校验：
- **`vue-tsc --noEmit`**：0 错误，TypeScript 严格模式全量通过；
- **`vite build`**：0 错误，全量静态资产（包含所有 Admin 模块代码分割 Chunks）顺利编译打包完成（构建耗时 18.37s）。
