# 2026-09-20 竞赛详情页倒计时、战队管理、指导老师豁免及聊天指示灯细节重构文档

## 1. 变更背景与需求概述

为了优化竞赛模块与通用交互组件的用户体验并完善业务逻辑闭环，本次重构对前端视图与后端业务逻辑进行了统一对齐与深度打磨：
1. **VolurChatDrawer 连接状态**：精简侧边抽屉顶部状态栏，移除“实时在线”中文文本，仅保留连接状态小圆圈：绿色标识已连接（`CONNECTED`），红色标识连接断开/失败（`DISCONNECTED`），黄色标识正在重连（`CONNECTING`）。
2. **CompetitionDetailView 视觉与倒计时**：
   - 移除原有的“🔥 竞赛正在进行中”标识胶囊及“靶机与竞技题库已全面开放，点击下方按钮立即进入专属竞技场！”等宣传引导文案；
   - 保留核心“进入竞赛”行动按钮；
   - 依据后端返回的 `competition.endTime`，在前端维护实时驱动的 1 秒精度定时器，实时展示“竞赛结束倒计时: DD天 HH:mm:ss”，在倒计时归零时自动重置为 `00:00:00` 并保持进入竞赛按钮可用。
3. **战队管理与信息展示（CompetitionDetailView & TeamFormModal）**：
   - 移除战队信息卡片中队长姓名旁展示的“队长”标签；
   - 战队名下方展示队伍简介（`description`），且在字数超过 30 字符时智能截断并追加 `"...."` 后缀；
   - 队长可在 `TeamFormModal` 中上传队伍头像，集成 `UserAvatar` 与 `AvatarCropperModal` 头像裁剪组件（与 `ProfileView` 个人中心风格完全统一）；
   - 在 `TeamFormModal` 中支持编辑战队简介；
   - 将“参赛成员名册”文案更新为“参赛成员”，“名册上限”更新为“上限”；
   - 当受邀队员拒绝邀请（状态为 `NOT_PARTICIPATE`）时，在操作列中呈现“重新邀请”操作按钮；队长点击后调用重新邀请接口，后端重置成员状态为 `INVITED` 并重新向队员投递站内组队邀请通知与更新卡片状态。
4. **指导老师（LEADER）免注册与免非空校验**：
   - 指导老师角色不需要在平台提前注册账号，也不需要通过平台登录确认接受组队邀请；
   - 前端表单中指导老师的手机号、身份证号、学号、学院全部设为可选输入项；
   - 后端放宽 `TeamMemberSignupDTO` 上的 `@NotBlank` 限制，并在 `RosterUniquenessChecker` 中排除指导老师角色的强约束；
   - `CompetitionMember.user_id` 外键置为可空（`nullable = true`），指导老师在战队报名与修改时直接标记为 `INVITED_ACCEPTED`，全流程做全链路 NPE 防御。

---

## 2. 变更详情清单

### 2.1 前端工程 (`volur-lite-view`)

1. **[`VolurChatDrawer.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/components/VolurChatDrawer.vue)**：
   - 移除“实时在线”文本标签，保留 `.status-dot` 指示灯，增加 tooltip 提示；
   - 状态色彩规范：连接成功绿色（`#10b981`）、断开连接红色（`#ef4444`）、重连中黄色（`#f59e0b`）。
2. **[`CompetitionDetailView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionDetailView.vue)**：
   - 移除“队长”标签；
   - 添加队伍简介展示区域与 `formatTeamDescription` 格式化方法（超过 30 字符自动裁剪并加 `"...."`）；
   - 移除宣传提示词条与火焰图标，新增 `.competition-countdown-bar` 倒计时栏，展示“竞赛结束倒计时: {countdownToEnd}”；
   - 组件内新增 `countdownToEnd` 响应式变量，`updateCountdown()` 双路更新开始与结束倒计时，卸载时清理定时器。
3. **[`TeamFormModal.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/components/TeamFormModal.vue)**：
   - 引入 `UserAvatar`、`AvatarCropperModal` 及 `uploadUserAvatarApi` 实现队伍头像上传裁剪；
   - 新增队伍简介多行文本输入框（`description`）；
   - 调整表格标题文案为“参赛成员”，上限文案为“（上限: X 人）”；
   - 当 `row.role === 'LEADER'` 时，学号、手机号、身份证、学院输入框占位符显示为“可选”；
   - `validateMembers` 校验逻辑中针对 `LEADER` 免除手机号、身份证号、学号和学院的必填校验；
   - 在队员状态为 `NOT_PARTICIPATE`（已拒绝）时，在操作列展示“重新邀请”按钮，点击后触发 `reinviteMemberApi` 并刷新战队信息。
4. **[`src/types/competition/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/types/competition/index.ts)**：
   - `TeamMemberSignupDTO` 字段 `studentNumber`、`phone`、`email`、`idCard`、`college` 改为可选类型；
   - 在 `TeamSignupRequestDTO`、`TeamUpdateRequestDTO`、`MyTeamInfoVO`、`CompetitionTeamVO`、`AdminTeamDetailVO` 中新增 `description?: string` 属性。
5. **[`src/api/competition/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/api/competition/index.ts)**：
   - 新增 `reinviteMemberApi(competitionId, teamId, userId)`，向后端 `POST /api/v1/competitions/{competitionId}/teams/{teamId}/invite/reinvite?userId={userId}` 发起请求。
6. **国际化语言文件**：
   - 更新 `src/locales/zh_CN/competition.json` 与 `src/locales/en_US/competition.json`，补充新增文案键名。

---

### 2.2 后端工程 (`volur-competition`)

1. **实体与模型扩展**：
   - **[`CompetitionTeam.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/entity/CompetitionTeam.java)**：新增 `description` 字段（`VARCHAR(512)`）。
   - **[`CompetitionMember.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/entity/CompetitionMember.java)**：将 `user_id` 外键设置为 `nullable = true`，适配未注册平台的指导老师。
   - **DTOs / VOs**：在 `TeamSignupRequestDTO`、`TeamUpdateRequestDTO`、`MyTeamInfoVO`、`CompetitionTeamVO`、`AdminTeamDetailVO` 中统一补充 `description` 字段。
   - **[`TeamMemberSignupDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/dto/TeamMemberSignupDTO.java)**：移除 `studentNumber`、`phone`、`email`、`idCard`、`college` 的 `@NotBlank` 约束（保留 `@Phone`、`@Email`、`@IdentificationCard` 格式约束，当且仅当非空时生效）。
2. **业务逻辑与指导老师豁免**：
   - **[`RosterUniquenessChecker.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/record/RosterUniquenessChecker.java)**：对 `MemberRole.LEADER` 豁免非空强制校验，若有填写则进行队内查重。
   - **[`CompetitionTeamServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/impl/CompetitionTeamServiceImpl.java)**：
     - `signupTeam`：对指导老师免除账号注册强校验，状态直接置为 `INVITED_ACCEPTED`，若邮箱存在系统账号则可选绑定，无账号则 `user` 置 `null`，不发送组队邀请站内信；持久化保存 `description`；
     - `updateTeam`：同样免除指导老师的注册校验，保存或更新指导老师记录与战队 `description`，并对 `m.getUser()` 进行安全判空；
     - `getMyTeam`：填充 `MyTeamInfoVO` 的 `description` 字段；
     - 实现 `reinviteMember(competitionId, teamId, captainUserId, targetUserId)`：
       - 鉴权：仅允许队长操作；
       - 状态检查：已接受成员不可重复邀请，检查被邀请人是否已在其他战队确认参赛；
       - 重置成员状态为 `MemberStatus.INVITED`，重置加入时间戳；
       - 更新原邀请卡片状态为已取消并重新推送全新 `COMPETITION_INVITE` 站内通知。
   - **[`AdminTeamServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/admin/impl/AdminTeamServiceImpl.java)**：
     - 在构建 `AdminTeamDetailVO` 时映射 `description`；
     - 审核通过日志打印与通知发送中对 `m.getUser()` 进行判空保护。
3. **控制器层**：
   - **[`CompetitionTeamController.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/CompetitionTeamController.java)**：
     - 新增 `@PostMapping("/{teamId}/invite/reinvite")` 端点，支持队长重新邀请队员。
4. **单元测试与验证**：
   - 更新 `CompetitionTeamServiceImplTest.java` 匹配 DTO 全参构造函数；
   - 更新 `TeamMemberSignupDTOValidationTest.java` 验证指导老师免填字段校验通过。

---

## 3. 安全性与影响范围说明

1. **防空指针异常（NPE Defense）**：
   - 由于指导老师允许不关联已注册平台账号，`CompetitionMember.getUser()` 在此场景下可能为 `null`；
   - 已在所有检索、映射、日志输出及通知派发场景增加 `m.getUser() != null` 的严格防御。
2. **越权防范（Authorization）**：
   - 重新邀请接口强制通过 `@CurrentUserId` 与 `team.getCaptain().getId()` 进行队长身份比对，防止非队长选手恶意重发邀请。
3. **前端编译与测试**：
   - 前端通过 `npm run build` 生产环境构建，无 TypeScript 及 Sass 编译错误；
   - 后端全部 25 项测试均执行通过（`Tests run: 25, Failures: 0, Errors: 0, Skipped: 0`）。
