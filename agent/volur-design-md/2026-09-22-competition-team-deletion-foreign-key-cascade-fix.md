# 参赛队伍删除与解散外键约束级联修复与安全防御记录

## 1. 变更背景与问题定位

### 1.1 异常现象
在管理端竞赛详情页的「团队审核」选项卡（[`TeamAuditTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/tabs/TeamAuditTab.vue)）中，管理员点击删除某一参赛队伍时，后端抛出 `DataIntegrityViolationException (ErrorCode: 1451, SQLState: 23000)` 外键约束异常：
```
Cannot delete or update a parent row: a foreign key constraint fails (`volur_db`.`competition_chat_message`, CONSTRAINT `FKmu7lagqwexpc547f3o009lrwc` FOREIGN KEY (`team_id`) REFERENCES `competition_team` (`id`))
```

### 1.2 根因剖析
1. **直接根因**：
   - 战队在创建或报名阶段，队员在赛场或队伍频道中发送过团队聊天（`TEAM` 频道）或咨询过裁判客服（`REFEREE` 工单），`competition_chat_message` 表中写入了引用 `team_id` 的记录。
   - 数据库中的 `competition_chat_message` 表对 `competition_team(id)` 存在物理外键约束，且约束未配置 `ON DELETE CASCADE`。
   - 管理端业务实现 [`AdminTeamServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/admin/impl/AdminTeamServiceImpl.java) 中的 `deleteTeam` 方法仅执行了 `memberRepository.deleteByTeamId(teamId)`，未清理聊天记录，导致 Hibernate 在执行 `delete from competition_team where id=?` 时被 MySQL 底层拦截。
2. **连锁隐患挖掘**：
   - **队伍邀请表 (`competition_team_invitation`)**：同样外键强引用 `team_id`，且在原有业务删除逻辑中缺失清理。
   - **队长端解散队伍（[`CompetitionTeamServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/impl/CompetitionTeamServiceImpl.java)）**：`deleteMyTeam` 方法同样只删除了 `memberRepository`，队长解散存在聊天或邀请记录的队伍时必然 100% 崩溃。
   - **数据破坏风险**：若战队已参与比赛并提交 flag 或理论答题（`ctf_competition_submission`、`theory_competition_answer` 等），无拦截直接物理删除会导致成绩榜单与对战日志遭破坏，缺失业务层前置安全拦截。

---

## 2. 核心改动点说明

### 2.1 数据访问层扩展（Repository）
1. **[`CompetitionChatMessageRepository.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/repository/CompetitionChatMessageRepository.java)**：
   - 新增基于 `@Modifying` 与 `@Query` 的高性能批量级联删除方法：
     ```java
     @org.springframework.data.jpa.repository.Modifying
     @Query("DELETE FROM CompetitionChatMessage m WHERE m.team.id = :teamId")
     void deleteByTeamId(@Param("teamId") Long teamId);
     ```
2. **[`CompetitionTeamInvitationRepository.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/repository/CompetitionTeamInvitationRepository.java)**：
   - 新增基于 `@Modifying` 与 `@Query` 的队伍邀请批量删除方法：
     ```java
     @org.springframework.data.jpa.repository.Modifying
     @org.springframework.data.jpa.repository.Query("DELETE FROM CompetitionTeamInvitation i WHERE i.team.id = :teamId")
     void deleteByTeamId(@org.springframework.data.repository.query.Param("teamId") Long teamId);
     ```
3. **赛题作答 Repository 存在性探测方法补充**：
   - [`CtfCompetitionSubmissionRepository.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/repository/ctf/CtfCompetitionSubmissionRepository.java)：新增 `boolean existsByTeamId(Long teamId);`；
   - [`TheoryCompetitionAnswerRepository.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/repository/theory/TheoryCompetitionAnswerRepository.java)：新增 `boolean existsByTeamId(Long teamId);`；
   - [`AwdPlusCompetitionPatchRepository.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/repository/awdp/AwdPlusCompetitionPatchRepository.java)：新增 `boolean existsByTeamId(Long teamId);`。

### 2.2 业务逻辑层防御与级联清理（Service）
1. **[`AdminTeamServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/admin/impl/AdminTeamServiceImpl.java)**：
   - 依赖注入遵循 `Rules.md`（`private final` + Lombok `@RequiredArgsConstructor`）；
   - 在 `deleteTeam(Long teamId)` 中新增：
     1. 战队存在性校验；
     2. **赛题作答前置安全校验**：检查 CTF / 理论题 / AWD+ 等是否存在提交记录，若已产生作答则抛出 `BusinessException` 阻止破坏性删除，友好提示管理员通过驳回资质处理；
     3. **安全级联清理附属数据**：在事务内依序执行 `teamInvitationRepository.deleteByTeamId(teamId)`、`chatMessageRepository.deleteByTeamId(teamId)`、`memberRepository.deleteByTeamId(teamId)`；
     4. 删除战队主体 `teamRepository.delete(team)`。
2. **[`CompetitionTeamServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/impl/CompetitionTeamServiceImpl.java)**：
   - 在队长解散战队方法 `deleteMyTeam` 中，同步补齐队伍邀请记录与聊天工单记录的级联清理，杜绝队长解散队伍时的同类外键约束异常。

### 2.3 数据库平滑升级脚本幂等更新（DDL）
在 [`scripts/upgrade/upgrade_to_latest.sql`](file:///Users/humolix/Documents/Dev/Volur-Lite/scripts/upgrade/upgrade_to_latest.sql) 中：
1. 更新 `competition_chat_message` 表的建表 DDL，外键显式指定 `ON DELETE CASCADE`；
2. 在升级存储过程 `migrate_volur_schema_20260921` 中新增 **3.9** 与 **3.10** 小节：
   - 通过 `information_schema.KEY_COLUMN_USAGE` 自动探测已有外键（兼容 Hibernate 自动命名的随机外键如 `FKmu7lag...` 及标准命名 `fk_chat_team`）；
   - 动态安全剔除旧约束并重新添加带 `ON DELETE CASCADE` 的外键约束；
   - 对 `competition_team_invitation` 进行相同幂等级联约束更新。

---

## 3. 单元测试与验证结果

1. **测试用例扩展与维护**：
   - **[`AdminTeamServiceImplTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/test/java/com/humolix/volur/competition/service/AdminTeamServiceImplTest.java)**：
     - 新增 `testDeleteTeam_success_cascadesDeletions`：验证成功删除时级联清理全流程；
     - 新增 `testDeleteTeam_whenHasSubmissions_throwsBusinessException`：验证存在赛题作答时前置拦截并拒绝删除；
     - 新增 `testDeleteTeam_whenNotFound_throwsBusinessException`：验证不存在战队时的 404 异常。
   - **[`CompetitionTeamServiceImplTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/test/java/com/humolix/volur/competition/service/CompetitionTeamServiceImplTest.java)**：
     - 更新 `testDeleteMyTeam_cancelsActionStatusAndNotifiesMembers`：验证队长解散队伍时对邀请与聊天记录的级联调用。
2. **测试结果**：
   - `mvn test -pl volur-competition`：全模块 29 个单元测试全部通过（Failures: 0, Errors: 0, Skipped: 0）；
   - `mvn test-compile`：全系统 12 个模块全部构建成功（Reactor Summary: 100% SUCCESS）。

---

## 4. 影响范围分析

- **受影响模块**：`volur-competition`（后台审核队伍删除、前台队长解散队伍）、MySQL 数据库增量脚本；
- **兼容性保障**：
  - 增量迁移脚本具备 100% 幂等性，支持在远程生产环境直接安全执行；
  - 业务层显式事务清理机制保证即使在禁用数据库物理级联的环境下也能稳定运行；
  - 前端 `TeamAuditTab.vue` 无需变更接口契约，删除操作与提示更加稳定可靠。
