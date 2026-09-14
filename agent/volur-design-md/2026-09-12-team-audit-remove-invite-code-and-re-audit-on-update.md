# 战队审核详情移除邀请码、后端废除邀请码生成及队伍修改强制重新送审审计记录

## 1. 变更背景与需求概述
根据竞赛战队管理与资质审核规范，执行以下三大改动：
1. **管理后台队伍详情移除邀请码**：在管理员界面的 [`TeamAuditTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/tabs/TeamAuditTab.vue) 参赛队伍详情弹窗中，删除邀请码展示项；同步清理比赛主赛场 [`CompetitionArenaView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionArenaView.vue) 中的邀请码展示与复制逻辑，彻底消除前端邀请码痕迹。
2. **后端废除邀请码生成逻辑**：在 [`CompetitionTeamServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/impl/CompetitionTeamServiceImpl.java) 中，彻底删除 `signupTeam` 报名建队时随机生成 8 位邀请码及持久化邀请码的代码；将实体 [`CompetitionTeam.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/entity/CompetitionTeam.java) 中的 `invite_code` 列约束改为可空（`nullable = true`），并移除针对 `invite_code` 的多余索引。
3. **队伍修改强制重置为未审核状态**：在用户（队长）修改队伍信息时（[`CompetitionTeamServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/impl/CompetitionTeamServiceImpl.java) 的 `updateTeam` 方法），无论比赛初始设置是否开启免审，均强制将队伍状态重置为待审核（`TeamStatus.AUDIT_DRAFT`），并清除历史驳回原因，必须由管理员在后台重新审核通过（`AUDIT_PASSED`）后方可重新获得参赛与解题资格。

---

## 2. 改动点清单与技术实现

### 2.1 前端改动 (Vue 3 / TypeScript / Naive UI)
1. **`TeamAuditTab.vue` (`src/views/admin/competition/tabs`)**:
   - 在参赛队伍详情弹窗中，删除邀请码展示项 `<span class="meta-item">邀请码：<code>{{ detailTeam.inviteCode }}</code></span>`。
2. **`CompetitionArenaView.vue` (`src/views/competition`)**:
   - 移除队伍标题右侧的 `.invite-box` 邀请码展示及点击复制节点；
   - 移除 `copyInviteCode` 函数及 `CopyOutline` 图标导入；
   - 清理 SCSS 中对应的 `.invite-box` 样式规则。

---

### 2.2 后端改动 (Java / Spring Boot)
1. **`CompetitionTeam.java` (`volur-competition`)**:
   - 移除 `@Table` 注解中的 `idx_team_comp_invite` 索引；
   - 将 `inviteCode` 字段上的 `@Column(name = "invite_code", nullable = false, ...)` 改为允许为 `null`。
2. **`CompetitionTeamServiceImpl.java` (`volur-competition`)**:
   - 移除无用 `RandomUtil` 导入；
   - 在 `signupTeam` 中移除 `RandomUtil.randomString(8).toUpperCase()` 生成逻辑与 `.inviteCode(...)` 赋值；
   - 在 `updateTeam` 中移除原根据配置决定的分支，改为无条件重置状态：
     ```java
     // 队伍信息修改后，强制重置为未审核状态 (AUDIT_DRAFT)，必须由管理员重新审核
     team.setStatus(TeamStatus.AUDIT_DRAFT);
     team.setAuditReason(null);
     teamRepository.save(team);
     log.info("[Competition-Team] 队伍 {} 修改了队伍信息，状态重置为未审核 (AUDIT_DRAFT)，须等待管理员审核通过", team.getTeamName());
     ```

---

## 3. 验证情况
1. **后端编译验证**：
   - 运行 `mvn test-compile -pl volur-competition`，编译成功（`BUILD SUCCESS`）。
2. **前端构建验证**：
   - 运行 `npm run build`，TypeScript 与 Vite 静态编译全部通过，0 错误。
