# 战队报名表单排版重构、单位类型差异化与领队/指导老师独立列表支持

## 1. 变更背景与需求说明

为了更好地适应高校竞赛与企业/社会机构赛事的不同参赛属性，并在组队环节对不同类型的人员进行权责解耦，本次对战队报名（`TeamFormModal`）、领队管理、领域模型及后端服务进行了系统性重构与细节调整：

1. **战队表单基础信息排版升级**：
   - 顶层信息修改为纵向“上下排版”：先展示战队头像与名称，再展示战队简介，最后展示新增的“队伍单位”与“单位类型”。
   - 新增“队伍单位”（`organization`，输入框）与“单位类型”（`organizationType`，下拉选择框，支持学校 `SCHOOL` 与企业 `ENTERPRISE`，默认为学校）。
2. **单位类型（学校 vs 企业）成员字段差异化展现**：
   - **学校类型 (`SCHOOL`)**：参赛成员名册动态展示“学号 (`studentNumber`)”、“学院 (`college`)”、“班级 (`className`)”；
   - **企业类型 (`ENTERPRISE`)**：参赛成员名册动态展示“工号 (`employeeNumber`)”、“部门 (`department`)”、“职位 (`position`)”。
3. **参赛成员角色固定与简化**：
   - 移除参赛成员列表中的角色下拉选择框；第 0 行固定为“队长”Tag，其余成员均为“队员”Tag。
4. **领队/指导老师独立解耦**：
   - 领队不再混杂于普通参赛成员列表中，而在参赛成员列表下方以类似“忘记密码？”的弱化文本链接“添加领队/指导老师”进行折叠/展开；
   - 领队无需邀请接受机制（不生成入队邀请通知），因此领队列表中彻底移除了“加入状态”列；
   - 后端 DTO 中使用独立的 `List<TeamLeadersSignupDTO> leaders = new ArrayList<>()` 接收领队名册，不对 `TeamLeadersSignupDTO` 做必填约束；
   - 参赛成员 `TeamMemberSignupDTO` 恢复严格的 `@NotBlank` 必填约束，删除过渡性的 `RosterUniquenessChecker.java`，恢复清晰的服务层校验循环。

---

## 2. 改动点汇总

### 2.1 后端改动 (Java / Spring Boot)

1. **新增领域枚举与实体字段**：
   - [`OrganizationType.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/enums/OrganizationType.java)：新增 `SCHOOL` 与 `ENTERPRISE` 枚举；
   - [`CompetitionTeam.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/entity/CompetitionTeam.java)：新增 `organization` (`VARCHAR(128)`) 与 `organizationType` (`VARCHAR(32)`, 默认 `SCHOOL`)；
   - [`CompetitionMember.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/entity/CompetitionMember.java)：新增 `className` (`VARCHAR(64)`)、`employeeNumber` (`VARCHAR(64)`)、`department` (`VARCHAR(128)`)、`position` (`VARCHAR(64)`)。

2. **DTO / VO 领域模型对齐**：
   - [`TeamLeadersSignupDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/dto/TeamLeadersSignupDTO.java)：新建领队/指导老师专用 DTO，无 `@NotBlank` 约束；
   - [`TeamMemberSignupDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/dto/TeamMemberSignupDTO.java)：恢复 `realName`, `phone`, `email`, `idCard` 上的 `@NotBlank` 约束，补充 `className`, `employeeNumber`, `department`, `position` 字段；
   - [`TeamSignupRequestDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/dto/TeamSignupRequestDTO.java) & [`TeamUpdateRequestDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/dto/TeamUpdateRequestDTO.java)：补充 `organization`, `organizationType` 及 `private List<TeamLeadersSignupDTO> leaders = new ArrayList<>()`；
   - [`MyTeamInfoVO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/vo/MyTeamInfoVO.java), [`CompetitionTeamVO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/vo/CompetitionTeamVO.java), [`AdminTeamDetailVO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/vo/AdminTeamDetailVO.java)：补充 `organization`, `organizationType`；
   - [`CompetitionMemberVO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/vo/CompetitionMemberVO.java)：补充 `className`, `employeeNumber`, `department`, `position`。

3. **业务逻辑层 (`CompetitionTeamServiceImpl.java` & `AdminTeamServiceImpl.java`)**：
   - 彻底删除 `RosterUniquenessChecker.java`，报名与修改名册还原为服务层内联的循环校验与单位类型校验；
   - 报名与更新时，区分学校与企业，分别校验学号/学院/班级或工号/部门/职位必填与排重；
   - 领队列表独立存储为 `MemberRole.LEADER`，初始状态直接赋为 `INVITED_ACCEPTED`，免除邀请流；
   - `saveInvitedMemberAndNotify` 补充新成员信息入库，并在 `getMyTeam` 与 `getTeam` 中回填 `organization` 和 `organizationType`。

4. **单元测试与集成测试修复**：
   - 更新 `CompetitionTeamServiceImplTest.java` 消除已废弃的构造器调用，设置 `className`；
   - 更新 `TeamMemberSignupDTOValidationTest.java` 测试 `TeamLeadersSignupDTO` 免填约束及成员校验。

---

### 2.2 前端改动 (Vue 3 / TypeScript)

1. **类型定义层 ([`src/types/competition/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/types/competition/index.ts))**：
   - 导出 `OrganizationType = 'SCHOOL' | 'ENTERPRISE'`；
   - 导出 `TeamLeadersSignupDTO`；
   - 更新 `TeamMemberSignupDTO`、`TeamSignupRequestDTO`、`TeamUpdateRequestDTO`、`MyTeamInfoVO`、`CompetitionTeamVO`、`AdminTeamDetailVO`、`CompetitionMemberVO`。

2. **国际化多语言 ([`src/locales/{zh_CN,en_US}/competition.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/zh_CN/competition.json))**：
   - 补充 `team_organization_label`, `team_organization_placeholder`, `team_org_type_label`, `org_type_school`, `org_type_enterprise`, `col_class_name`, `col_employee_number`, `col_department_real`, `col_position`, `add_leader_link`, `hide_leader_link`, `leader_table_title`, `add_leader_row` 等键。

3. **战队表单组件重构 ([`src/views/competition/components/TeamFormModal.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/components/TeamFormModal.vue))**：
   - **排版重组**：头像 -> 战队名称 -> 队伍简介 -> 队伍单位 & 单位类型，清晰上下层级排列；
   - **动态表头与单元格**：依据 `formData.organizationType` 动态切换展示学号/学院/班级与工号/部门/职位；
   - **角色下拉框移除**：队员行角色下拉移除，统一使用静态 Tag 标注；
   - **独立领队名册**：使用无下划线/文本链接样式的“添加领队/指导老师”控制展示，移除了“加入状态”列；
   - **表单校验与提交**：前端校验逻辑 1:1 对齐后端单位差异化必填要求，提交时向后端分别投递 `members` 与 `leaders`。

---

## 3. 安全隐患修复与质量保障

1. **NPE 防御**：由于领队无需在平台注册账号，`CompetitionMember.user` 允许为空。后台所有基于 `member.getUser()` 读取用户信息的逻辑均已增加非空防护，杜绝报空指针。
2. **身份证与手机号跨队伍排重防护**：在队员报名和修改中，严格检查手机号、二代身份证号跨队伍冲突，防止同一选手以不同身份多头参赛。
3. **编译与自动化测试验证**：
   - 后端全部 12 个模块通过 `mvn compile` 编译；
   - `volur-competition` 模块通过全部 25 个单元测试（`mvn test -pl volur-competition`）；
   - 前端通过 Vite 构建编译（`npm run build`），TypeScript 类型与模板校验 100% 通过。
