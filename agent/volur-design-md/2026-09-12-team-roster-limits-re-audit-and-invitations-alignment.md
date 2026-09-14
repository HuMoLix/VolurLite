# 战队名册限制、队伍修改再审核、解散战队及站内信邀请通知重构审计记录

## 1. 变更背景与需求概述
在战队报名与组队协作机制中，根据业务最新规范与安全合规要求进行精细化调整：
1. **战队报名表单精简与行数上限控制**：
   - 取消表单中冗余的“战队口号/宣言”（`teamSlogan`）输入；
   - 移除原表单顶部硬编码规则提示，根据后端返回的比赛人数限制动态约束名册成员行数的上限（`maxTotalRows = maxTeamMembers + maxLeaders`），超出时禁止新增行；
   - 成员字段校验严格化：性别仅允许“男”与“女”；身份证号、邮箱、手机号均为必填且做格式及行间查重校验；
   - 统一使用**邮箱**（`email`）校验并匹配平台已注册账号（与登录、账户安全体系统一）。
2. **战队审核未通过与队伍信息修改**：
   - 若管理员驳回了队伍审核申请（`AUDIT_REJECTED`），在比赛详情页清晰展示驳回原因，并提供“修改队伍信息”按钮；
   - 队伍信息在报名后亦支持修改；修改后状态重新流转为等待审核（`AUDIT_DRAFT`，若开启队伍审核）；
   - 修改队伍信息时，**队伍名称（teamName）锁定禁止修改**；
   - 修改队伍信息时，队长拥有“解散战队”功能（带二次确认），解散后清空队伍并重置报名状态。
3. **领队与成员角色多队伍约束**：
   - 选手（队长 `CAPTAIN`、队员 `MEMBER`）在同一比赛中仅能加入一支战队；
   - 领队（`LEADER`）允许担任多支队伍的领队；
   - 修复数据库唯一性约束与关联查询导致的单结果异常，支持领队多队任职。
4. **站内信通知与组队邀请流程**：
   - 管理员审核通过或驳回时，向队伍全体成员（队长、队员、领队）发送站内信通知；
   - 队长邀请队员/领队时，向被邀请人发送站内信通知，站内信携带比赛详情跳转数据（`actionData`）；
   - 在比赛详情页展示“收到的组队邀请”按钮（仅在存在待处理邀请时展示），支持弹窗查看并一键接受或拒绝邀请；
   - 站内信中心支持直接一键跳转至对应竞赛详情页（`/competitions/:slug` 或 `/:id`）。

---

## 2. 改动点清单与技术实现

### 2.1 后端改动 (Java / Spring Boot)

1. **`TeamMemberSignupDTO.java`** (`volur-competition`):
   - 性别（`gender`）添加 `@NotNull` 注解，仅接受 `MALE` / `FEMALE`；
   - 身份证号（`idCard`）添加 `@NotBlank` 注解；
   - 邮箱（`email`）添加 `@NotBlank` 与 `@Email` 校验注解；
   - 手机号（`phone`）保持 `@NotBlank` 与正则约束。

2. **`TeamInvitationVO.java`** (`volur-competition`):
   - 新建 `TeamInvitationVO` 视图模型，包含 `competitionId`、`competitionTitle`、`teamId`、`teamName`、`captainName`、`role`、`realName`、`email` 等信息，满足待处理邀请列表展示需求。

3. **`CompetitionMemberRepository.java`** (`volur-competition`):
   - 增加 `findAllByCompetitionIdAndUserId(Long competitionId, Long userId)`，避免多队领队时 `findByCompetitionIdAndUserId` 抛出 `NonUniqueResultException`；
   - 增加 `findUserInvitations(Long competitionId, Long userId, MemberStatus status)`，查询用户在指定赛事下的待处理邀请。

4. **`CompetitionTeamService.java` & `CompetitionTeamServiceImpl.java`** (`volur-competition`):
   - 队长及成员校验全面切换为基于 `email` 匹配注册用户（`userRepository.findByEmail(email)`）；
   - 校验队长邮箱必须与当前登录账号绑定的邮箱一致；
   - 新增/更新队员逻辑中，严格校验：
     - 若成员角色为普通队员，校验其在当前赛事中未担任任何队伍的参赛选手；
     - 若角色为领队，允许跨队伍担任领队（但同一队伍内不可重复加入）；
     - 名册总行数限制：`members.size() <= maxMembers + maxLeaders`；
   - 队伍信息更新（`updateTeam`）：
     - 队伍名称锁定不可修改；
     - 修改提交后，若比赛配置 `requireTeamAudit == true`，将队伍状态重置为 `AUDIT_DRAFT` 重新送审，并清除历史 `auditReason`；
   - 新增 `deleteMyTeam(competitionId, captainUserId)`：队长解散战队，级联删除成员及战队记录；
   - 新增 `getMyInvitations(competitionId, userId)`：获取用户在指定赛事下的受邀列表；
   - 接受邀请（`acceptInvitation`）：增加普通成员单队限制校验与领队多队豁免逻辑。

5. **`CompetitionTeamController.java`** (`volur-competition`):
   - 新增 `DELETE /api/v1/competitions/{competitionId}/teams/me`：解散我的战队（Thin Controller，返回 `ApiResponseEntity.success("战队已解散")`）；
   - 新增 `GET /api/v1/competitions/{competitionId}/teams/invitations/me`：获取当前登录用户收到的组队邀请列表。

6. **`AdminTeamServiceImpl.java`** (`volur-competition`):
   - 队伍审核结果通知（`AUDIT_PASSED` / `AUDIT_REJECTED`）：由仅通知队长升级为遍历通知该队伍名下**全体成员（队长、队员、领队）**；
   - 站内信附带 `actionData` 序列化数据（`competitionId`、`slug`、`teamId`）。

7. **`TeamInvitationSentEvent.java` & `CompetitionNotificationListener.java`**:
   - 邀请事件附带 `competitionSlug`，通知监听器在组队邀请站内信中注入 `actionData`。

---

### 2.2 前端改动 (Vue 3 / TypeScript / Naive UI)

1. **类型与 API 对齐 (`src/types/competition/index.ts`, `src/api/competition/index.ts`)**:
   - `TeamMemberSignupDTO` 更新：`gender: 'MALE' | 'FEMALE'`，`idCard: string`，`email: string`；
   - 新增 `TeamInvitationVO` 类型定义；
   - 导出 `deleteMyTeamApi(competitionId)` 与 `getMyInvitationsApi(competitionId)`。

2. **模态框重构 (`src/views/competition/components/TeamFormModal.vue`)**:
   - 替代原 `TeamSignupModal.vue`，支持 `create` 与 `edit` 双模式；
   - 移除口号（`teamSlogan`）输入；移除硬编码提示 alert；
   - `isEditMode` 下队伍名称（`teamName`）锁定只读；
   - 成员行数控制：最大限制为 `maxTeamMembers + maxLeaders`，达标后禁用“添加成员行”；
   - 表格从左到右依次为：姓名、性别（仅男/女）、角色（首行为队长且锁定，其余可选队员/领队）、身份证号（必填）、邮箱（必填）、手机号（必填）、单位、部门、职位、操作；
   - 客户端行间唯一性查重校验（邮箱、手机号、身份证号去重）；
   - 编辑模式下为队长提供“解散战队”（`NPopconfirm`）操作。

3. **组队邀请弹窗 (`src/views/competition/components/TeamInvitationsModal.vue`)**:
   - 独立呈现用户在当前赛事中收到的战队邀请；
   - 展示战队名、邀请角色、队长姓名及填报姓名；
   - 提供“接受邀请”与“拒绝邀请”操作。

4. **赛事主页整合 (`src/views/competition/CompetitionDetailView.vue`)**:
   - 未报名态：若存在待处理邀请，动态展示“收到的组队邀请 (N)”按钮；若无邀请则隐藏；
   - 已报名态：
      - 队长显示“修改队伍信息”按钮；
      - 审核被拒（`AUDIT_REJECTED`）态清晰展示驳回原因；统一收敛由战队操作栏常驻的“修改队伍信息”按钮负责弹窗编辑与重新送审（移除提示卡片内重复的“修改队伍信息并重新送审”按钮，避免界面重复）；
      - 领队等多重角色亦可随时查看待处理邀请；
    - 对接 `TeamFormModal` 与 `TeamInvitationsModal`。

5. **站内信导航联动 (`src/components/notification/NotificationCenter.vue`)**:
   - 解析 `actionData` 中的 `slug` 或 `competitionId`；
   - 列表项与详情弹窗中增加“前往竞赛主页 / 竞赛详情”按钮，点击后自动关闭弹窗并跳转至 `/competitions/:slug`。

6. **中英文国际化 (`locales/zh_CN/competition.json`, `locales/en_US/competition.json`)**:
   - 补充完善修改队伍、解散队伍、邀请列表及各项表单校验文案。

---

## 3. 安全隐患与架构防御说明
1. **身份越权防护**：
   - 队长解散战队及修改队伍信息在后端接口强制鉴权当前登录用户与队长 ID（`findByCompetitionIdAndCaptain_Id`），非队长访问直接抛出 `403 FORBIDDEN`；
   - 队长修改队员名册时，队长邮箱强行比对当前登录账号绑定邮箱，杜绝冒用他人邮箱假冒队长。
2. **多重参赛冲突与雪崩防御**：
   - 参赛选手严格限制单赛单队，防作弊与替赛；
   - 领队允许多队，但同一队伍内不允许重复加入；
   - 名册行数受后端数据库及配置硬性截断（`members.size() <= maxMembers + maxLeaders`），防范恶意批量注入。
3. **数据一致性与审核闭环**：
   - 队伍信息修改后，立即强制将状态打回 `AUDIT_DRAFT` 重新送审，防止先过审后篡改恶意成员参赛名册的作弊漏洞。

---

## 4. 影响范围与验证结果
- **后端测试**：`volur-lite-platform` 全体 12 个模块执行 `mvn test-compile`，结果 `100% BUILD SUCCESS`；
- **前端构建**：`volur-lite-view` 执行 `npm run build`（包含 `vue-tsc` 类型检查与 Vite 打包），0 错误通过构建；
- **兼容性**：历史单队查询与 AWD 赛段服务保持平滑兼容。
