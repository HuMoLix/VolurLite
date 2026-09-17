# 竞赛战队名册字段重构、必填校验增强与口号/邀请码清理设计记录

**变更日期**：2026-09-16  
**变更模块**：`volur-competition`（后端）、`volur-lite-view`（前端竞赛详情、战队报名与审核）  
**影响范围**：战队实体与名册实体、报名/修改 DTO、名册表单模态框、后台审核详情、动态容器 Flag 上下文等。

---

## 1. 变更背景与需求

在竞赛报名与名册管理业务中：
1. **战队口号（`teamSlogan`）与邀请码（`inviteCode`）彻底废除**：
   - 报名与队伍展示中不再需要“战队宣言/口号（`teamSlogan`）”，此前部分前端界面仍残留口号展示；
   - 组队机制已全面转为直接基于实名名单定向邀请与用户确认机制，历史遗留的战队独立邀请码（`inviteCode`）及其相关查询接口彻底废止。
2. **战队成员名册字段语义重塑与顺序调整**：
   - 原“单位（`organization`）”调整为“学院（`college`）”；
   - 原“部门（`department`）”调整为“学号（`studentNumber`）”；
   - 彻底删除无用的“职位（`position`）”字段；
   - 前端表单展示顺序严格遵循：**姓名 -> 性别 -> 角色 -> 学号 -> 手机号 -> 邮箱 -> 身份证 -> 学院**（末尾附带加入状态 tag 与移除操作按钮）；
   - 名册中全部项强制必填，并对学号、手机号、邮箱、身份证进行队伍内唯一性查重校验。

---

## 2. 后端改动详情 (Java / Spring Boot)

### 2.1 实体层变更
- **`CompetitionTeam` (`com.humolix.volur.competition.domain.entity.CompetitionTeam`)**：
  - 移除 `teamSlogan` 属性及 `@Column(name = "team_slogan")`；
  - 移除 `inviteCode` 属性及 `@Column(name = "invite_code")`。
- **`CompetitionMember` (`com.humolix.volur.competition.domain.entity.CompetitionMember`)**：
  - 移除 `organization`、`department`、`position` 字段；
  - 新增 `studentNumber`（`@Column(name = "student_number", length = 64, columnDefinition = "VARCHAR(64)")`）；
  - 新增 `college`（`@Column(name = "college", length = 128, columnDefinition = "VARCHAR(128)")`）。

### 2.2 数据访问与 DTO / VO 变更
- **`CompetitionTeamRepository`**：
  - 移除废弃的方法 `findByCompetitionIdAndInviteCode`。
- **`TeamMemberSignupDTO`**：
  - 调整字段顺序：`realName`, `gender`, `role`, `studentNumber`, `phone`, `email`, `idCard`, `college`；
  - 全字段标注 Jakarta Validation 注解（`@NotBlank`, `@NotNull`, `@Email`），保障全量字段必填约束；
  - 移除 `organization`, `department`, `position`。
- **`TeamSignupRequestDTO` & `TeamUpdateRequestDTO` & `TeamRegisterRequestDTO`**：
  - 移除 `teamSlogan` 字段。
- **`CompetitionMemberVO`**：
  - 移除 `organization`, `org`, `department`, `position`；
  - 新增 `studentNumber`, `college` 字段。
- **`CompetitionTeamVO` & `MyTeamInfoVO` & `AdminTeamDetailVO`**：
  - 移除 `teamSlogan` 与 `inviteCode` 属性。
- **`TeamJoinRequestDTO`**：
  - 彻底删除该冗余类文件。

### 2.3 对象映射与服务层逻辑变更
- **`CompetitionMapper`**：
  - 更新 `toMemberVO` 映射方法：显式映射 `member.studentNumber -> studentNumber`、`member.college -> college`，移除对职位、部门、单位旧属性的映射。
- **`CompetitionTeamServiceImpl`**：
  - `signupTeam`：移除 `teamSlogan` 链式调用；在成员查重逻辑中加入 `studentNumber` 非空与队内重复校验（`seenStudentNumbers`）、`college` 非空校验；队长行构建使用 `studentNumber` 与 `college`。
  - `updateTeam`：移除 `teamSlogan` 更新逻辑；全量校验成员学号唯一性与学院非空；更新队长、领队、普通队员记录时保存 `studentNumber` 与 `college`，移除 `position`。
  - `getMyTeam`：移除 `MyTeamInfoVO` 中 `teamSlogan` 与 `inviteCode` 的设置。
  - `hidePrivateInfo`：将脱敏字段对齐为 `studentNumber` 与 `college`。
  - `saveInvitedMemberAndNotify`：写入 `studentNumber` 与 `college`。
- **`AdminTeamServiceImpl`**：
  - `getTeam` 移除 `teamSlogan` 与 `inviteCode` 赋值。
- **`CtfCompetitionServiceImpl`**：
  - `startInstance`：移除 `team.getInviteCode()` 调用，`teamToken` 置为 `null`（Flag 上下文自动 fallback）。

---

## 3. 前端改动详情 (Vue 3 / TypeScript / Naive UI)

### 3.1 实体与类型定义 (`src/types/competition/index.ts`)
- `TeamMemberSignupDTO`：字段重构为 `realName`, `gender`, `role`, `studentNumber`, `phone`, `email`, `idCard`, `college`；
- `TeamSignupRequestDTO` & `TeamUpdateRequestDTO`：移除 `teamSlogan`；
- `MyTeamInfoVO`：移除 `inviteCode`, `teamSlogan`, `motto`；
- `CompetitionMemberVO`：移除旧字段，对齐 `studentNumber` 与 `college`；
- `AdminTeamDetailVO`：移除 `teamSlogan` 与 `inviteCode`。

### 3.2 国际化语言包 (`zh_CN/competition.json` & `en_US/competition.json`)
- 新增 `col_student_number`（学号 / Student ID）、`col_college`（学院 / College）；
- 移除 `col_position`；
- 新增表单校验错误文案：`validation_student_number_required`、`validation_college_required`、`validation_duplicate_student_number`。

### 3.3 参赛名册录入弹窗 (`TeamFormModal.vue`)
- **表格列头与输入单元格顺序重构**：
  1. 姓名 (`col_name`, `req`)
  2. 性别 (`col_gender`, `req`)
  3. 角色 (`col_role`, `req`)
  4. 学号 (`col_student_number`, `req`)
  5. 手机号 (`col_phone`, `req`)
  6. 邮箱 (`col_email`, `req`)
  7. 身份证 (`col_id_card`, `req`)
  8. 学院 (`col_college`, `req`)
  9. 加入状态 (`col_status`)
  10. 操作 (`col_action`, 仅编辑态)
- **校验逻辑全量覆盖**：
  - 姓名、性别、角色必填；
  - 学号非空且队伍内不允许重复；
  - 手机号非空、正则校验且队伍内不允许重复；
  - 邮箱非空、正则校验且队伍内不允许重复；
  - 身份证非空且队伍内不允许重复；
  - 学院非空。
- **数据回显与初始化**：队长初始行与添加成员行均正确支持 `studentNumber` 与 `college`。

### 3.4 竞赛详情与战队展示清理
- **`CompetitionDetailView.vue`**：
  - 彻底移除战队名称下方的 `<p class="slogan">{{ myTeam.teamSlogan || myTeam.motto || '暂无战队宣言' }}</p>` 元素；
  - 清理对应的 `.slogan` SCSS 样式。
- **`CompetitionArenaView.vue`**：
  - 移除战队概览卡片中的口号展示项。
- **`TeamAuditTab.vue`**：
  - 移除战队详情弹窗中的队伍口号宣言；
  - 成员名册卡片展示信息更新为“学号”与“学院”。

---

## 4. 影响与回归验证

1. **后端验证**：
   - 执行 `mvn clean test-compile`，全工程 12 个模块全部 `BUILD SUCCESS`，零编译错误。
2. **前端验证**：
   - 执行 `pnpm build`，Vite + Vue-TSC 类型检查与构建零错误通过。
3. **数据库兼容性**：
   - Hibernate `ddl-auto: update` 模式下将自动在 `competition_member` 表增建 `student_number` 与 `college` 列，对历史数据无破坏性副作用。
