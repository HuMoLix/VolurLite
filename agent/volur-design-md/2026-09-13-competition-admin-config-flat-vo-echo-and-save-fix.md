# 竞赛管理端配置平铺 VO 回显与队伍限制保存修复施工记录

## 1. 变更背景与问题归因

### 1.1 问题现象
在管理员进入竞赛编辑管理界面（`AdminCompetitionEditView.vue`）中的“基础信息与赛程”子选项卡（`BasicInfoTab.vue`）时，修改“队伍人数限制与封榜规则”中的“单队人数下限”为 `1`、“单队人数上限”为 `1` 并点击保存。保存后刷新页面或在子选项卡间切换再切回时，上限数值重新恢复为默认的 `4`，造成“保存后没有完成保存 / 保存未生效”的故障表象。

### 1.2 根因深度剖析
1. **前后端 VO 结构不对称（主要原因）**：
   - 后端控制器 `AdminCompetitionController.java` 中的 `getCompetition` 接口返回的是 `AdminCompetitionDetailVO`。
   - 在后端 `AdminCompetitionDetailVO.java` 中，所有配置项属性（`minTeamMembers`、`maxTeamMembers`、`maxLeaders`、`maxTeams`、`freezeScoreboardMinutes`、`allowWriteupSubmission`、`maxContainersPerTeam` 等）均为**根层级平铺字段**，不存在嵌套的 `config` 响应对象；`CompetitionMapper.java` 亦使用 `@Mapping(source = "competition.config.*", target = "*")` 平铺映射。
   - 而前端子选项卡组件 `BasicInfoTab.vue`、`WriteupTab.vue`、`TeamAuditTab.vue` 及父组件 `AdminCompetitionEditView.vue` 在回显数据时，均错误地使用了 `if (d.config)` 或 `d.config.xxx` 来提取配置。
   - 结果：后端返回的 `d.config` 为 `undefined`，导致 `if (d.config)` 判断恒为 `false`，组件无法读取后端已经持久化的 `minTeamMembers` 和 `maxTeamMembers`。
   - 当页面刷新、重新进入或在 Tab 间切换触发组件重新挂载时，`formConfig` 均重新回落至组件初始化的硬编码默认值（`minTeamMembers: 1`, `maxTeamMembers: 4`），给用户造成保存失败/被覆盖的假象。
2. **容器配额与关联 Tab 回显同步缺陷**：
   - `AdminCompetitionEditView.vue` 传给 `container-tab` 的 `:initial-max-containers` 取值为 `competitionDetail?.config?.maxContainersPerTeam || 2`，导致容器配额恒为 2；
   - `ContainerTab.vue` 的 `maxContainers` 未对 props 的异步更新设置 watcher；
   - `WriteupTab.vue` 与 `TeamAuditTab.vue` 同样依赖 `d.config`，导致 WP 提交设置和队伍审核开关无法正常回显。

---

## 2. 改动点清单

### 2.1 前端：`BasicInfoTab.vue`
- **文件路径**：`src/volur-lite-view/src/views/admin/competition/tabs/BasicInfoTab.vue`
- **改动说明**：
  - 重构 `populateData` 方法，采用 `const cfg = d.config || d` 策略，优先直接读取后端 `AdminCompetitionDetailVO` 根层级平铺字段（`minTeamMembers`, `maxTeamMembers`, `maxLeaders`, `maxTeams`, `freezeScoreboardMinutes`, `allowWriteupSubmission`, `maxContainersPerTeam`, `writeupTemplateUrl`, `writeupTemplateName`, `writeupStartTime`, `writeupEndTime`），同时兼容可能的嵌套对象。
  - 确保保存后刷新重新获取到的平铺字段能够 100% 正确同步到 `formConfig` 响应式状态中。

### 2.2 前端：`AdminCompetitionEditView.vue`
- **文件路径**：`src/volur-lite-view/src/views/admin/competition/AdminCompetitionEditView.vue`
- **改动说明**：
  - 将 `container-tab` 的 `:initial-max-containers` 调整为优先读取 `competitionDetail?.maxContainersPerTeam`，再回退至 `competitionDetail?.config?.maxContainersPerTeam` 及兜底默认值 `2`。
  - `handleQuotaUpdated` 同步更新 `competitionDetail.value.maxContainersPerTeam`。

### 2.3 前端：`ContainerTab.vue`
- **文件路径**：`src/volur-lite-view/src/views/admin/competition/tabs/ContainerTab.vue`
- **改动说明**：
  - 增加对 `props.initialMaxContainers` 的响应式 `watch` 监听，确保父组件异步获取竞赛详情完成后，配额输入框能实时响应并同步初始值。
  - 清理未使用的无用组件引用。

### 2.4 前端：`WriteupTab.vue` 与 `TeamAuditTab.vue`
- **文件路径**：
  - `src/volur-lite-view/src/views/admin/competition/tabs/WriteupTab.vue`
  - `src/volur-lite-view/src/views/admin/competition/tabs/TeamAuditTab.vue`
- **改动说明**：
  - 统一更新 `watch(() => props.initialData)` 逻辑，将 `d?.config` 修改为兼容平铺字段的 `const cfg = d.config || d`，彻底修复 WP 设置回显与队伍审核开关回显缺陷。
  - 完善 `TeamAuditTab.vue` 表格 row-key 的 TypeScript 类型声明。

### 2.5 前端：Tab 标签去除序号前缀
- **文件路径**：
  - `src/volur-lite-view/src/locales/zh_CN/admin/competitions.json`
  - `src/volur-lite-view/src/locales/en_US/admin/competitions.json`
  - `src/volur-lite-view/src/views/admin/competition/AdminCompetitionEditView.vue`
- **改动说明**：
  - 去除 `tab_basic`、`tab_announcements`、`tab_challenges`、`tab_containers`、`tab_writeups`、`tab_teams` 等各选项卡标签文字中的 `1. `、`2. `、`3. `、`4. `、`5. `、`6. ` 序号前缀，中英文文本保持统一自然、简洁优雅的现代风格。

---

## 3. 安全隐患说明及影响范围

1. **安全性与数据一致性保障**：
   - 保证了前端请求入参（DTO 带有 `config` 契约）与后端接收实体对齐，同时使响应视图（VO 平铺属性）在前端消费时具备单一真实数据源。
   - 杜绝了由于回显失败，导致管理员再次保存基本信息时以错误的默认值（4人）意外覆写数据库中已经设定的人数限制规则（1人）的高危数据踩踏风险。
2. **影响范围**：
   - 竞赛管理端编辑页（`AdminCompetitionEditView.vue`）；
   - 竞赛基础信息与赛程 Tab（`BasicInfoTab.vue`）；
   - 竞赛容器管理 Tab（`ContainerTab.vue`）；
   - 竞赛 WP 管理 Tab（`WriteupTab.vue`）；
   - 竞赛队伍审核 Tab（`TeamAuditTab.vue`）；
   - 管理端中英文国际化语言包（`competitions.json`）。
3. **验证结果**：
   - 前端工程运行 `npm run build`（包含 `vue-tsc --noEmit` 严格类型检测和 Vite 生产构建），全量编译通过，无任何类型及构建报错。
