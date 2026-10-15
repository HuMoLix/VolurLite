# 赛事队伍容器限额配置与赛题弹窗容器状态持久化重构设计方案

## 1. 变更背景

在 Volur CTF 竞赛系统中，赛题可能具备动态容器（Dynamic Container）环境。先前存在两个核心缺陷与功能缺失：
1. **容器数量限制缺乏赛事级细粒度管控**：平台先前仅在用户个人级别存在写死的限制（最多 2 个个人容器），未与比赛（`Competition`）和战队（`CompetitionTeam`）进行绑定。管理员在竞赛设置中无法配置“单个队伍在本次比赛中在同一时刻最多允许存在的容器数量”，导致无法根据集群算力动态调控资源配额。
2. **赛题详情卡片重新进入后容器状态丢失**：参赛队员在赛题卡片中点击启动动态容器后，若关闭卡片弹窗重新点击卡片进入，界面仍然显示“一键拉起独立动态靶机”，原有运行中的容器连接信息、剩余倒计时与销毁按钮全部消失。此外，按钮文案“一键拉起独立动态靶机”不够简炼规范，需调整为标准命名的“启动容器”。

---

## 2. 根因分析 (Root Cause Analysis)

### 2.1 赛题弹窗容器状态丢失
经过全链路排查，定位出如下两项核心原因：
1. **服务端响应视图缺失容器实例对象**：
   `CtfCompetitionChallengeDetailVO` 中未包含任何容器实例字段（`instance`）。当参赛用户调用 `GET /api/v1/competitions/{competitionId}/ctf/challenges/{challengeId}` 获取赛题详情时，服务端即使检测到队伍有活跃容器，也无法通过此 VO 序列化下发给前端。
2. **前端弹窗组件状态无条件清空**：
   在 `CtfCompetitionChallengeModal.vue` 中，监听弹窗可见性的 `watch(() => props.show)` 在打开弹窗时无条件执行了 `activeInstance.value = null`，且从未尝试利用 `props.challenge.instance` 或调用接口拉取已有实例状态。这导致即使后端返回或之前拉起过实例，每次打开弹窗都会被强制重置为 `null`，从而命中 `v-else` 降级渲染启动按钮。

### 2.2 容器与战队配额脱节
在后端的容器启动逻辑中，`CtfInstanceServiceImpl` 仅根据 `userId` 统计用户个人全局容器数，没有按 `stageId`（即 `competitionId`）和 `teamId` 统计活跃容器。且 `startInstance` 之前传入的 `stageId` 均为 `0L`，导致容器实例记录 `ctf_challenge_instance` 无法归属于特定的比赛与队伍。

---

## 3. 架构与设计方案

### 3.1 赛事配置：单队并发容器上限 (`maxContainersPerTeam`)
1. **数据库与配置扩展**：
   在 `competition_config` 表中新增 `max_containers_per_team` 整数列，默认值为 2。
   实体 `CompetitionConfig`、入参 `CompetitionConfigDTO` 与视图 `AdminCompetitionDetailVO` 均同步映射该字段。
2. **管理员运维视图 (`AdminCtfCompetitionDetailView.vue`)**：
   在 CTF 竞赛详情运维页面中新增“容器限制”卡片，使用 Google Material 风格及系统规范的 `VInput` 组件，绑定 `formData.maxContainersPerTeam`。
   在保存时增加强类型前置校验：必须为大于等于 1 的整数。
3. **配额执行拦截**：
   在队员启动容器（`startCtfInstance`）时，根据 `competitionId`、`team.getId()` 以及队伍全体成员的 `userId` 列表，统计当前队伍在本次比赛中处于运行中（`RUNNING`/`STARTING` 且未过期）的容器数量。
   若已达到或超过 `competition.getConfig().getMaxContainersPerTeam()`，直接抛出 HTTP 429 业务异常：`"本场比赛单支队伍在同一时刻最多允许存在 X 个容器，请先销毁已有容器后重试"`。

### 3.2 容器实例与战队绑定 (`team_id`)
1. **数据模型扩展**：
   在 `ctf_challenge_instance` 实体与持久化表中新增 `team_id` 字段及联合索引 `idx_ctf_inst_team_chal (team_id, challenge_id)`。
   当比赛场景拉起容器时，将 `stageId` 设为 `competitionId`，将 `teamId` 设为当前队伍 ID。
2. **全队共享靶机与防重复启动**：
   启动容器前，先查询队伍是否已存在该题目的运行中容器。若已存在，直接返回现有容器信息；若不存在，通过 Redis 分布式锁防护并发竞争，避免同一队伍多队员并发拉起产生重复资源浪费。
3. **团队动态 Flag 校验对齐**：
   在 `submitFlag` 提交动态 Flag 时，通过 `containerInstanceService.verifyTeamFlag(...)` 校验队伍名下该赛题运行中容器的 Flag，确保同一队伍各队员共享同题解题进度。

### 3.3 赛题弹窗容器状态自动恢复与倒计时联动
1. **后端 VO 对齐**：
   在 `CtfCompetitionChallengeDetailVO` 中增加 `ChallengeInstanceResponseVO instance` 字段。
   `CtfCompetitionServiceImpl.getChallengeDetail` 在组装 VO 时，自动读取当前队伍针对该赛题的活跃容器并注入。
2. **专属查询接口**：
   在 `CtfCompetitionController` 新增 `GET /api/v1/competitions/{competitionId}/ctf/challenges/{challengeId}/instance`，便于前端按需拉取。
3. **前端状态机流转**：
   在 `CtfCompetitionChallengeModal.vue` 中封装 `syncInstance()` 函数：
   - 打开弹窗或切换题目时触发；
   - 若 `props.challenge.instance` 存在且未过期，立即恢复并启动倒计时定时器；
   - 若未携带但赛题为容器题，调用 `getCtfInstanceApi` 异步校准；
   - 在启动容器、续期容器、销毁容器或倒计时结束时，同步更新 `activeInstance` 与 `props.challenge.instance`。
4. **文案更新**：
   将启动按钮文案从“一键拉起独立动态靶机”统一调整为“启动容器”，并在中英三语语言包中规范对应键值。

---

## 4. 改动点明细

### 4.1 后端工程 (Java & Spring Boot)
1. **`volur-container` 模块**：
   - `CtfChallengeInstance.java`：新增 `@Column(name = "team_id") private Long teamId` 及联合索引。
   - `CtfChallengeInstanceRepository.java`：新增队伍维度查询与统计方法，包含 `countActiveByStageAndTeamOrTargetIds`。
   - `CtfInstanceService.java` & `impl/CtfInstanceServiceImpl.java`：
     - 新增 `getTeamInstance`、`countActiveInstancesForTeam`、`stopTeamInstance`、`extendTeamInstance`、`verifyTeamFlag`；
     - `startInstance` 签名扩展接收 `stageId`、`teamId` 与 `teamToken`。
   - `ContainerSchemaInitializer.java`：新增动态补齐 `team_id` 列。
2. **`volur-competition` 模块**：
   - `CtfCompetitionChallengeDetailVO.java`：新增 `private ChallengeInstanceResponseVO instance`。
   - `CtfCompetitionController.java`：新增 `@GetMapping("/challenges/{challengeId}/instance")` 接口。
   - `CtfCompetitionServiceImpl.java`：
     - `getChallengeDetail`：查询队伍实例填充 `instance` 属性；
     - `startInstance`：绑定 `teamId` 与 `stageId = competitionId`，校验 `maxContainersPerTeam` 配额，防止超额创建；
     - `submitFlag`：动态 Flag 校验改用队伍容器实例；
     - `listChallenges`：使用 `ObjectMapper.convertValue` 保证多类型平滑兼容。
   - `AdminCompetitionServiceImpl.java`：在创建与更新竞赛时校验并持久化 `maxContainersPerTeam`（约束 `>= 1`）。

### 4.2 前端工程 (`src/volur-lite-view-NEXT`)
1. **类型定义与 API 封装**：
   - `src/types/competition/index.ts`：在 `CtfCompetitionChallengeDetailVO` 补充 `instance?: CtfChallengeInstanceVO | null`。
   - `src/api/competition.ts`：导出强类型 `getCtfInstanceApi`，并修正 `startCtfInstanceApi`、`extendCtfInstanceApi` 返回类型为 `ApiResponse<CtfChallengeInstanceVO>`。
2. **国际化语言包**：
   - `src/locales/{zh-CN,zh-TW,en-US}/competition.json`：更新 `instance_start` 为 `启动容器` / `啟動容器` / `Start Container`。
   - `src/locales/{zh-CN,zh-TW,en-US}/admin.json`：新增 `container_limit_settings`、`form_max_containers_per_team`、`invalid_max_containers`。
3. **管理员 CTF 竞赛详情页 (`AdminCtfCompetitionDetailView.vue`)**：
   - 新增“容器限制”卡片，通过 `VInput` 绑定 `formData.maxContainersPerTeam`；
   - `fetchDetail` 读取 `res.data.maxContainersPerTeam`；
   - `handleSave` 校验正整数约束并回填入 `payload.config`。
4. **赛题详情模态框 (`CtfCompetitionChallengeModal.vue`)**：
   - 修复打开弹窗无条件清空实例的 Bug，实现 `syncInstance` 自动恢复与倒计时同步；
   - 启动、续期、销毁操作全量双向同步 `props.challenge.instance`；
   - 更新降级操作按钮文案为 `启动容器`。

### 4.3 数据库迁移脚本 (`scripts/upgrade/upgrade_to_latest.sql`)
- 严格遵循幂等性规范，在存储过程内探测字段存在性后执行 DDL：
  - `competition_config.max_containers_per_team` INT DEFAULT 2；
  - `ctf_challenge_instance.team_id` BIGINT DEFAULT NULL。

---

## 5. 验证结果

1. **前端工程构建**：
   在 `src/volur-lite-view-NEXT` 执行 `npm run build`（`vue-tsc --noEmit && vite build`），全模块类型检查与打包通过，Exit Code 0。
2. **后端编译与测试**：
   在 `src/volur-lite-platform` 执行 `mvn test-compile -pl volur-container,volur-competition -am -DskipTests`，编译成功。
   执行 `mvn test -pl volur-competition -am -Dtest=CtfCompetitionCacheTest -Dsurefire.failIfNoSpecifiedTests=false`，8 项单元测试全部通过。
