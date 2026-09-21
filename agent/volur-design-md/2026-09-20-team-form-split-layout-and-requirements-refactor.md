# TeamFormModal 左右分栏重构、报名须知卡片与领队折叠逻辑修复

## 1. 变更背景

在战队报名与队伍管理流程（`TeamFormModal`）中，原有的队伍基本信息展示区域宽度利用率不均匀，缺少参赛须知与报名规则说明，导致参赛队员在报名阶段无法直观掌握当前竞赛的起止时间、组队规模限制、是否需要管理员审核以及特别限定要求（如仅限特定赛区高校、线下决赛资格认证等）。

此外，原组件在编辑已有战队信息模式下存在领队折叠逻辑缺陷：
1. 当队长在编辑态点击“添加领队/指导老师”展开列表后，点击“收起领队/指导老师”无法正常收起（由于视图层 `v-if` 条件将只读态判断条件永久激活）；
2. 缺乏内容保护机制，若用户在领队列表中已填写了实质信息，直接折叠可能导致输入内容被意外清除。
3. 领队列表内部新增按钮文案“添加领队/指导老师行”与整体视觉不搭，需优化简化为“添加成员行”。

为此，本轮重构完成了前后端 `regRequirements` 字段的全链路支持、`TeamFormModal` 顶部左右 50% 等分布局重构与报名须知卡片设计，以及领队列表的智能折叠与安全校验逻辑。

---

## 2. 核心改动点

### 2.1 后端与管理端配置扩展 (`regRequirements`)
1. **实体与 DTO 支持**：
   - `CompetitionConfig.java`: 新增 `@Column(name = "reg_requirements", columnDefinition = "TEXT") private String regRequirements;`；
   - `CompetitionConfigDTO.java`: 新增 `private String regRequirements;`；
   - `CompetitionDetailVO.java` / `AdminCompetitionDetailVO.java` / `CompetitionAboutVO.java`: 均补充 `regRequirements` 映射字段；
   - `CompetitionMapper.java`: 显式配置 `@Mapping(source = "competition.config.regRequirements", target = "regRequirements")`。
2. **管理端业务层持久化**：
   - `AdminCompetitionServiceImpl.java`: 在 `createCompetition` 与 `updateCompetition` 中将 `regRequirements` 写入 `CompetitionConfig`。
3. **管理端前端界面适配**：
   - `BasicInfoTab.vue`: 在竞赛参数配置子卡片中添加“自定义报名要求与须知”文本域，支持赛事主办方/管理员输入非系统自动校验的特殊补充要求。

### 2.2 前端类型定义对齐 (`src/types/competition/index.ts`)
- 在 `CompetitionConfigDTO`、`CompetitionConfigVO`、`CompetitionDetailVO`、`CompetitionAboutVO`、`AdminCompetitionDetailVO` 中补充 `regRequirements?: string` 属性声明，严格符合 1:1 对齐规范与零 `any` 原则。

### 2.3 TeamFormModal 左右分栏 (50% / 50%) 优雅排版
1. **容器栅格划分**：
   - 使用 CSS Grid `display: grid; grid-template-columns: 1fr 1px 1fr; gap: 20px; align-items: stretch;` 将顶部队伍信息区域分为左框与右框，并在中间渲染 1px 竖向分割线（`meta-divider`）；移动端/小屏响应式自适应折叠为单列。
2. **左框输入框铺平**：
   - 保持紧凑三行排版：
     - 第 1 行：52px 队伍头像 + 战队名称输入框（`flex: 1; width: 100%`，填满剩余宽度）；
     - 第 2 行：队伍单位（`1fr`）+ 单位类型选择框（`130px`），整体填满左框宽度；
     - 第 3 行：队伍简介文本域（`width: 100%`，填满左框宽度）。
3. **右框竞赛报名要求与须知卡片 (`requirements-card`)**：
   - 采用主题感知容器（`var(--v-bg-card-alt)`，自适应日间浅灰与夜间沉浸炭黑）；
   - **标题与审核状态徽标**：展示“竞赛报名要求与须知”，右侧根据 `competition.requireTeamAudit` 显示“需人工审核”或“免审核”状态标签；
   - **系统配置生成部分 (`system-req-list`)**：
     - 报名时间：格式化显示 `regStartTime` ~ `regEndTime`；
     - 比赛时间：格式化显示 `startTime` ~ `endTime`；
     - 人数要求：显示队伍规模限制（最少至最多人数，含队长）；
     - 领队要求：显示允许配置的指导老师/领队人数上限（可选填）。
   - **管理员自定义补充说明 (`custom-req-wrapper`)**：
     - 专门以虚线分割展示“特别说明（非系统自动校验）”，展示主办方自定义要求（如区域高校限制、学生证核验等）；当未配置时展示优雅斜体提示。

### 2.4 领队/指导老师名册智能收起与安全折叠保护
1. **修复折叠失效 Bug**：
   - 原代码中 `v-if="showLeadersSection || (props.mode === 'view' && leaders.length > 0)"` 导致在查看模式下通过“修改队伍信息”进入编辑态（`isEditing = true` 但 `props.mode` 仍为 `'view'`）时，只要添加了一行领队，`(props.mode === 'view' && leaders.length > 0)` 永远为 `true`，使得 `showLeadersSection` 切换无法隐藏列表。
   - 重构条件为：`v-if="isEditing ? showLeadersSection : (leaders.length > 0)"`，编辑态完全由 `showLeadersSection` 控制，只读态根据是否有领队数据决定是否展示。
2. **实质内容检测与防误触清空保护 (`hasSubstantiveLeaderInfo`)**：
   - 新增实质内容校验函数，遍历检查 `leaders` 数组中任意行的真实姓名、手机号、邮箱、身份证、工号/学号、院系/部门、班级/职位等字段是否包含非空字符串；
   - 用户点击“收起领队/指导老师”时：
     - **已填写实质内容**：终止折叠并弹出 `message.warning('领队/指导老师列表中已填写相关信息，无法直接收起；若不需要请先清空或删除成员行')`；
     - **未填写实质内容**（空行或无数据）：安全清空临时空行并收起列表，保证数据整洁无冗余空对象提交。
3. **文案规范更新**：
   - 领队列表内部新增行按钮由“添加领队/指导老师行”修改为“添加成员行”，同步更新中英文语言包 (`zh_CN/competition.json` 与 `en_US/competition.json`)。

---

## 3. 安全与防误触说明

| 场景 | 潜在隐患 | 解决方案与防护机制 |
| :--- | :--- | :--- |
| 用户误点击“收起领队” | 原本已录入的领队姓名、手机号等信息可能在收起或提交时被丢弃 | 引入 `hasSubstantiveLeaderInfo()` 防护拦截，阻断直接收起并提示用户必须显式删除或清空行 |
| 领队与战队成员数据冲突 | 领队手机号或身份证与参赛成员重复 | `validateMembers()` 严格对领队手机号、身份证进行全队跨角色查重，杜绝重复报备 |
| 报名要求误解与争议 | 参赛者因未注意主办方特殊要求（如仅限长三角高校）导致赛后争议 | 将管理员特别要求置于报名弹窗顶层显著位置，与队伍基础信息并行呈现 |

---

## 4. 影响范围与验证结果

- **受影响文件**：
  - 后端：
    - `src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/admin/impl/AdminCompetitionServiceImpl.java`
  - 前端：
    - `src/volur-lite-view/src/types/competition/index.ts`
    - `src/volur-lite-view/src/locales/zh_CN/competition.json`
    - `src/volur-lite-view/src/locales/en_US/competition.json`
    - `src/volur-lite-view/src/views/admin/competition/tabs/BasicInfoTab.vue`
    - `src/volur-lite-view/src/views/competition/components/TeamFormModal.vue`
- **验证结果**：
  - 前端 `npm run build`: TypeScript 类型检查及 Vite 打包通过，零错误；
  - 后端 `mvn test -pl volur-competition`: 全部 26 项单元与集成测试均通过，零失败。
