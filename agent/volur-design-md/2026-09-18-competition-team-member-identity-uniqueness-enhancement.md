# 竞赛战队成员身份唯一性校验（手机号/身份证/邮箱）重构施工审计文档

## 1. 变更背景与动机
在 Volur Lite 竞赛报名体系中，参赛队伍通过前端 [`TeamFormModal.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/components/TeamFormModal.vue) 进行队长建队及名册填报。
原有逻辑中：
- 前端仅在单次名册提交表格内部使用 `Set` 集合进行了行间防重校验；
- 后端 [`CompetitionTeamServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/impl/CompetitionTeamServiceImpl.java) 仅校验了队长邮箱绑定、内存入参循环去重以及已确认队员的账号 ID（`userId`）跨队冲突，**完全缺失跨队伍层面对手机号（`phone`）和身份证号（`idCard`）的唯一性校验**；
- 若用户 A 已经在战队 1 成功使用某手机号或身份证号报名，战队 2 依然可以填入相同的手机号或身份证号并成功入库，存在严重的冒用他人身份报名、替赛和违规一人多队的业务安全隐患。

---

## 2. 详细改动点说明

### 2.1 实体索引增强（[`CompetitionMember.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/entity/CompetitionMember.java)）
为支撑高并发抢位报名，在 `competition_member` 表上新增基于 `competition_id` 与手机号、身份证号、邮箱号的复合索引：
```java
@Table(name = "competition_member", indexes = {
        @Index(name = "idx_comp_user_status", columnList = "competition_id, user_id, status"),
        @Index(name = "idx_team_status", columnList = "team_id, status"),
        @Index(name = "idx_comp_phone", columnList = "competition_id, phone"),
        @Index(name = "idx_comp_idcard", columnList = "competition_id, id_card"),
        @Index(name = "idx_comp_email", columnList = "competition_id, email")
})
```

### 2.2 数据访问层增强（[`CompetitionMemberRepository.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/repository/CompetitionMemberRepository.java)）
新增跨队伍冲突成员批量检索方法，支持排除当前队伍自身（`excludeTeamId`），避免队长更新本队信息时发生“自冲突”：
```java
@Query("SELECT m FROM CompetitionMember m " +
        "WHERE m.competition.id = :competitionId " +
        "  AND m.status IN :activeStatuses " +
        "  AND (:excludeTeamId IS NULL OR m.team.id != :excludeTeamId) " +
        "  AND (m.phone IN :phones OR UPPER(m.idCard) IN :idCards OR LOWER(m.email) IN :emails)")
List<CompetitionMember> findConflictingMembers(
        @Param("competitionId") Long competitionId,
        @Param("activeStatuses") Collection<MemberStatus> activeStatuses,
        @Param("excludeTeamId") Long excludeTeamId,
        @Param("phones") Collection<String> phones,
        @Param("idCards") Collection<String> idCards,
        @Param("emails") Collection<String> emails);
```

### 2.3 业务服务层重构（[`CompetitionTeamServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/impl/CompetitionTeamServiceImpl.java)）
1. **抽取专用校验方法 `validateMemberIdentitiesAcrossTeams`**：
   - 提取入参中的 `phones`、`idCards`（规范化为大写）、`emails`（规范化为小写）；
   - 查询有效占用状态（`INVITED` 待确认、`INVITED_ACCEPTED` 已确认）；
   - 精细化角色冲突判定：
     - 普通选手/队长（`CAPTAIN` / `MEMBER`）：严格禁止同一手机号、身份证号或邮箱被其他队伍使用；
     - 领队（`LEADER`）：系统允许同一指导老师账号带领多支队伍参赛，但若与参赛选手（`CAPTAIN` / `MEMBER`）身份冲突，或冒用其他人员手机/身份证，则坚决拦截；
   - 错误提示中对身份证号码采用 [`DesensitizationUtil.maskIdCard`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/util/DesensitizationUtil.java) 统一脱敏，兼顾友好定位与合规隐私安全。
2. **挂载点集成**：
   - `signupTeam`：建队保存前调用 `validateMemberIdentitiesAcrossTeams(competitionId, null, memberList)`；
   - `updateTeam`：修改名册保存前调用 `validateMemberIdentitiesAcrossTeams(competitionId, team.getId(), memberList)`；
   - 数据入库规范化：队长及队员的 `idCard` 统一调用 `.trim().toUpperCase()`，`email` 统一调用 `.trim().toLowerCase()`。

### 2.4 单元测试补齐（[`CompetitionTeamServiceImplTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/test/java/com/humolix/volur/competition/service/CompetitionTeamServiceImplTest.java)）
补齐 3 个核心业务场景的自动化单元测试：
- `testSignupTeam_whenPhoneAlreadyRegistered_shouldThrowConflict`：验证手机号已被本场比赛其他队伍注册时抛出 409 CONFLICT；
- `testSignupTeam_whenIdCardAlreadyRegistered_shouldThrowConflict`：验证身份证号已被本场比赛其他队伍注册时抛出 409 CONFLICT；
- `testUpdateTeam_whenPhoneConflictsWithOtherTeam_shouldThrowConflict`：验证更新战队名册时跨队手机号冲突拦截。

---

## 3. 安全隐患修复与影响范围说明

1. **安全隐患修复**：
   - 彻底修复了跨队伍冒用他人手机号、身份证号、邮箱报名的漏洞；
   - 杜绝了一人以相同身份证或手机号跨战队多次参赛（替赛/马甲队伍）的作弊行为；
   - 身份证错误提示采用掩码脱敏，防止通过报错信息刺探其他参赛人员的完整公民身份隐私。
2. **影响范围**：
   - 覆盖所有竞赛战队创建（`POST /api/v1/competitions/{id}/teams/signup`）和战队名册修改（`PUT /api/v1/competitions/{id}/teams/me`）接口；
   - 前端 [`TeamFormModal.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/components/TeamFormModal.vue) 无需改动接口契约，后端通过标准的 409 异常响应由全局 Axios 拦截器与 Naive UI Message 给出精确提示。

---

## 4. 验证与审计结果

- **测试执行**：在 `volur-competition` 模块执行 `mvn test`：
  - 全模块 27 个单元测试（含 9 个 `CompetitionTeamServiceImplTest` 测试）全部 100% 通过（Failures: 0, Errors: 0）；
- **编译检查**：Java 25 字节码编译通过，无任何语法与兼容性错误。
