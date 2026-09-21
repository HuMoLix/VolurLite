# 竞赛战队报名与更新方法结构化优化与 Record 领域建模审计文档

## 1. 变更背景与动机
在 Volur Lite 竞赛管理模块中，[`CompetitionTeamServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/impl/CompetitionTeamServiceImpl.java) 中的 `signupTeam`（战队报名）与 `updateTeam`（战队信息与名册修改）两个方法承载了队伍与选手关系的核心生命周期。
随着报名校验规则的逐步丰富，原方法逐渐累积了大量样板式逻辑：
1. 成员列表内部字段（学号、手机号、邮箱、身份证号）的判空与判重代码在两个方法中各自冗余了近 40 行，完全属于重复样板代码；
2. 传统 `if-else if` 的成员角色分类模式冗长且对枚举缺少穷举防御；
3. 开发维护过程中改动了集合变量名（`leaderRows`/`normalMemberRows` -> `leaders`/`members`）以及提取了 `TeamLimits` Record 后，两处方法的变量命名与结构尚未对齐。

同时，架构设计要求：
- 严格遵循 Volur Lite 规范（不可变依赖注入、领域模型收敛于 `domain` 目录）；
- 避免过度拆分出大量琐碎零散的类级辅助私有方法（过度碎化会导致长流程业务被割裂，破坏直观自顶向下的单流向可读性）；
- 充分借助 **Java 25**（模式匹配 Switch、Sequenced Collections、不可变 Record）与 **Spring 7** 现代特性进行内聚式结构化提精。

---

## 2. 详细改动点说明

### 2.1 新增领域 Record 模型（[`RosterUniquenessChecker.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/record/RosterUniquenessChecker.java)）
按照项目包结构规范，将名册防重 Record 存放在 `com.humolix.volur.competition.domain.record` 包下，与 [`TeamLimits.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/record/TeamLimits.java) 保持统一层级：
- 定义 4 个不可变集合组件（`seenStudentNumbers`, `seenPhones`, `seenEmails`, `seenIdCards`）；
- 提供紧凑高内聚的 `checkMember` 与通用 `checkField` 逻辑；
- 提供静态便捷入口 `RosterUniquenessChecker.validate(memberList)`。

### 2.2 服务实现层优化（[`CompetitionTeamServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/impl/CompetitionTeamServiceImpl.java)）
在 `signupTeam` 和 `updateTeam` 两个方法中全面进行结构化对齐优化：
1. **统一引用与有序集合 (Sequenced Collections)**：
   - 统一提取唯一队长：`TeamMemberSignupDTO captainRow = captains.getFirst();`
   - 对齐下游命名规范：`leaders` 和 `members`；
2. **模式匹配分流 (Pattern Matching for Switch)**：
   - 全面采用包含 `case null, default` 守卫分支的增强型 `switch (m.getRole())`，避免遗漏角色类型并保证 Null-Safety；
3. **消除两处共 80 行重复判重模板**：
   - `signupTeam` 与 `updateTeam` 统一调用 `RosterUniquenessChecker.validate(memberList);`，一键完成名册内字段判空、格式归一化与集合防重；
4. **配额统一建模封装**：
   - 统一由 `TeamLimits limits = TeamLimits.from(config);` 承载配额计算与边界判定；
5. **保持零新增类级方法**：
   - 绝不在 Service 类中增加碎片化的单次调用私有方法，保持整段报名与修改主流程纯粹自包含与线性直读。

---

## 3. 影响范围与回归验证

1. **影响范围**：
   - 竞赛队伍报名服务 `CompetitionTeamService.signupTeam`；
   - 竞赛队伍更新服务 `CompetitionTeamService.updateTeam`；
   - 外部 HTTP 接口（`POST /api/v1/competitions/{id}/teams/signup` 与 `PUT /api/v1/competitions/{id}/teams/me`）契约与前端行为完全保持 1:1 兼容，异常状态码与提示信息无破坏性变更。
2. **测试与编译审计**：
   - 执行 `mvn test-compile -pl volur-competition -am`：全平台模块在 **Java 25 release** 目标下 100% 编译成功；
   - 执行 `mvn test -pl volur-competition`：全模块 24 个单元测试全部通过（Failures: 0, Errors: 0, Skipped: 0）。
