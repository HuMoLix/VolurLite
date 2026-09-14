# 管理端比赛管理全生命周期多标签页架构改造审计记录

- **日期**：2026-09-11
- **模块**：`volur-competition` (平台后端) / `volur-lite-view` (管理端前端)
- **施工目标**：将原「编辑竞赛」全面升级重构为「比赛管理」，采用多 Tab 独立组件架构，覆盖比赛基础信息维护与前台实时预览、比赛内专属广播公告、题库赛题挂载与动态计分配置、队伍并发靶机容器监控与配额限制、WP 规则配置/模板发布/多格式在线渲染评审、战队资质审核全生命周期流程。

---

## 一、变更背景与设计考量

在 CTF 与多元化网络安全竞技对抗中，赛事的管理中枢需具备全流程、模块化且高内聚的控制能力。原先管理端的编辑页面仅具备基础元数据编辑功能，无法直接在比赛上下文中调度题目、监测容器、发布内部通报、查阅队员 Writeup 与审核团队资质。
本次改造严格依照系统设计规范（`Rules.md` 与 `view-Rules.md`）进行前后端 1:1 对齐，将单一的大表单重构成高内聚、职责分离的 6 大独立 Tab 页面架构。

---

## 二、详细改动点说明

### 1. 后端架构扩展 (`volur-competition`, `volur-container`)
1. **比赛配置扩展 (`CompetitionConfig.java`, `CompetitionConfigDTO.java`, `AdminCompetitionDetailVO.java`)**：
   - 新增 `maxContainersPerTeam`: 限制单个队伍在当前比赛中最多可同时启动的靶机容器数量；
   - 新增 `writeupTemplateUrl`, `writeupTemplateName`: 赛事官方 Writeup 标准模版下载链接与文件名；
   - 新增 `writeupStartTime`, `writeupEndTime`: 赛后解题报告开放提交与截止时间窗口；
   - 更新 MapStruct 转换器 `CompetitionMapper.java` 完成配置双向实体映射。
2. **比赛容器监控隔离 (`AdminContainerPageRequestDTO.java`, `AdminContainerServiceImpl.java`)**：
   - 容器分页查询请求 DTO 扩充 `stageId?: Long` 过滤参数；
   - 在容器实例动态查询条件中注入 `stage_id = ?` 约束，精确限定只监控当前比赛产生的容器，防止与日常练习题容器混淆。
3. **已挂载赛题配置接口 (`AdminCtfMountService.java`, `AdminCtfMountController.java`)**：
   - 新增 `PUT /api/v1/admin/competitions/{competitionId}/stage/ctf/mount/{competitionChallengeId}` 端点，支持动态调整初始基准分、保底分、衰减梯度解出次数与赛区排序。
4. **比赛专属公告系统 (`AdminCompetitionAnnouncementController.java`)**：
   - 实现了比赛私域公告实体与控制器，提供 CRUD、置顶、关联赛题及级别区分（INFO / WARNING / CRITICAL / HINT），区别于全站公共公告，专属于已报名参赛并进入赛场的选手。
5. **选手 Writeup 评审系统 (`AdminCompetitionWriteupController.java`)**：
   - 实现了队伍 WP 提交、分页查询、打分评分与技术评语反馈接口。

### 2. 前端架构与组件封装 (`src/volur-lite-view`)
1. **主管理视图重构 (`AdminCompetitionEditView.vue`)**：
   - 页面标题与路由更新为「比赛管理」，头部集成天梯封榜/解封切换开关及赛制/状态徽章；
   - 基于 `<n-tabs>` 构建扁平组件体系，未创建竞赛前自动阻止子 Tab 加载；
   - 将 URL query 参数与当前 Tab 动态双向同步（`?tab=basic|announcements|challenges|containers|writeups|teams`）。
2. **Tab 1: 基础信息与前台效果预览 (`tabs/BasicInfoTab.vue`)**：
   - 维护比赛标题、Slug、开始/结束日期、报名周期、Markdown 竞赛简述及横幅 Banner；
   - 集成「前台样式实时预览」模态框，1:1 复刻 `CompetitionDetailView.vue` 前台视觉效果（包括倒计时指示牌、赛制说明、报名规则、Markdown 渲染及自适应布局）。
3. **Tab 2: 比赛专属公告 (`tabs/AnnounceTab.vue`)**：
   - 比赛内部专用公告管理，支持置顶、关联当前挂载的 CTF 赛题、公告严重级别标记，即时反馈到赛场。
4. **Tab 3: 比赛题目管理 (`tabs/ChallengeTab.vue`)**：
   - 左侧展示已挂载赛题总览与总分统计，支持单题与批量从中央公共题库中导入题目；
   - 支持已挂载题目的动态计分衰减曲线（初始分、保底分、衰减阈值）及排序配置；
   - 支持一键从比赛中卸载赛题。
5. **Tab 4: 容器监控与并发配额 (`tabs/ContainerTab.vue`)**：
   - 配置单战队最大容器并发数（`maxContainersPerTeam`，1~10 个/队）；
   - 展示比赛中当前所有存活的靶机容器（统计指示牌 + 数据表格），提供直观的 TTL 剩余时间显示、复制访问链接、直接续期与强制销毁释放功能；
   - 轮询机制（15s 静默刷新）保持容器状态实时性。
6. **Tab 5: Writeup 题解管理与多格式在线实时渲染 (`tabs/WriteupTab.vue`)**：
   - 配置开放提交开关、设定提交时间范围，上传官方题解模版（支持 `.docx` / `.pdf` / `.md` / `.zip` 等）；
   - 列表展示已提交 WP，支持下载、评审打分与评语下发；
   - **多格式在线实时渲染模态框**：
     - **Markdown (`.md`)**：使用内部安全渲染引擎解析呈现；
     - **PDF (`.pdf`)**：基于原生 Sandboxed iframe 实时查阅；
     - **Word (`.docx`)**：集成 `docx-preview` 引擎，将 Word 二进制文件流直接在 DOM 内解析渲染排版，无需选手或管理员下载到本地即可快速查阅。
7. **Tab 6: 队伍审核与人员名册 (`tabs/TeamAuditTab.vue`)**：
   - 开关比赛是否启用人工资质审核（`requireTeamAudit`）；
   - 展示报名队伍名册、各状态数量统计指示牌，支持资质审核通过与驳回（带驳回原因反馈）；
   - 队伍详情模态框：查看队长信息、完整队员名册（含角色、邮箱、加入时间）及自定义报名表单扩展字段数据。

### 3. 类型与 API 对齐 (`types/competition`, `types/container`, `types/challenge`)
- 全面消除 `any` 类型声明；
- 严格对齐后端 `TeamStatus`（`AUDIT_DRAFT`、`AUDIT_PASSED`、`AUDIT_REJECTED`）；
- 严格对齐 `AdminTeamListVO`、`AdminTeamDetailVO`、`CompetitionMemberVO`、`TeamAuditRequestDTO`；
- 修正 `AdminContainerInstanceVO`（`challengeTitle`）、`AdminContainerRenewRequestDTO`（`durationMinutes`）以及分页参数 `stageId`；
- 完善中英文双语国际化配置（`zh_CN/admin/competitions.json`、`en_US/admin/competitions.json`）。

---

## 三、验证与审计

1. **前端编译验证**：
   - 执行 `npm run build`（`vue-tsc --noEmit && vite build`）：**无错误通过，构建耗时 4.42s，退出代码 0**。
2. **后端编译验证**：
   - 在 `src/volur-lite-platform` 下执行 `mvn compile -DskipTests`：**所有 12 个模块全部 SUCCESS，无错误通过**。
3. **视觉与设计规范符合度**：
   - 符合 `Rules.md` 与 `view-Rules.md` 中关于 Thin Controller、MapStruct、不可变依赖、ES2026、Naive UI Provider 与色彩排版规范的要求。
