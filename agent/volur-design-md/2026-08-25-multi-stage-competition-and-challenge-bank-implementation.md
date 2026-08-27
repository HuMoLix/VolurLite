# 2026-08-25 Volur-Lite 多阶段竞赛系统与母题库资产架构落地实施报告

---

## 1. 变更背景与需求目标

在原有的系统设计中，存在以下历史局限：
1. **题库与训练场耦合**：原 `volur-challenge` 包含大量 `Training` 相关的实体和控制器，未能专注于作为纯粹的母题资产池；
2. **缺乏多阶段混合赛制支持**：无法支撑由理论赛（THEORY）、夺旗解题赛（CTF）、经典攻防赛（AWD）、进阶攻防赛（AWD+）及场景实战渗透赛（ISW）组成的多阶段复合赛程与加权折算总分；
3. **战队与用户模型强耦合**：全局用户表直接存储了 `total_points` 与 `solves`，未做到按比赛作用域隔离；原战队为常态化公会制，无法满足单场赛事自定义报名表单和临时组队的需求。

本次重构旨在彻底解耦用户与题库，完整落地全新构建的 `volur-competition` 模块以及 5 大赛制类型的母题库资产池。

---

## 2. 核心改动点与架构设计

### 2.1 用户模型解耦 (`volur-user`)
- 从 [`User.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/domain/entity/User.java) 中移除 `totalPoints`、`solves` 字段及对应的数据库单列索引；
- 从 [`UserProfileResponseVO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/domain/vo/UserProfileResponseVO.java) 中移除全局排名与总分字段；
- 同步精简 `UserRepository`、`UserMapper`、`UserServiceImpl`、`AuthServiceImpl` 中对全局积分的依赖。

### 2.2 题库与竞赛边界隔离 (`volur-challenge` vs `volur-competition`)
- **`volur-challenge` (母题库资产中心)**：
  - 纯粹用于后台管理员进行母题录入、分类维护与资产管理，不承载任何比赛与公开答题逻辑；
  - 包含 5 类标准化母题：`CtfChallenge`、`TheoryChallenge`、`AwdChallenge`、`AwdPlusChallenge`、`IswChallenge`。
- **`volur-competition` (多阶段竞赛与实战中心)**：
  - 承载所有竞赛创建、阶段挂载、动态报名表单、临时队伍组建、CTF/理论/AWD/AWD+/ISW 专属实战答题与平滑动态积分计算；
  - 提供竞赛上下文下的动态容器实例开启/延期/销毁路由（`/api/v1/competitions/{id}/stage/ctf/challenges/{challengeId}/instance/*`）。

### 2.3 容器调度层轻量化升级 (`volur-container`)
- **全面移除 Docker / Docker Swarm 依赖**：从根 POM 与 `volur-container` 中彻底清理 `docker-java` 库；
- **全量切换为 K3s / Kubernetes 架构**：
  - 引入 `io.fabric8:kubernetes-client` (6.13.4)；
  - 实现 [`K3sRuntimeStrategy.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/strategy/K3sRuntimeStrategy.java)，通过 K3s Kubeconfig / Master API 自动化管理 Pod 隔离沙箱与 NodePort 服务映射，支持无集群环境下的平滑降级模拟。

### 2.4 竞赛模型多阶段统一与枚举精简 (`volur-competition`)
- **取消单/多阶段枚举区分**：彻底废弃并删除 `CompetitionStageType.java` 枚举（不再区分 `SINGLE` 与 `MULTIPLE`）；
- **全赛制统一为多阶段架构**：所有竞赛天然支持多阶段配置；若某比赛仅进行单一赛制（如仅 CTF 解题），只需在 `stageConfigs` 中配置一个包含该阶段的配置项即可；
- **领域模型与 DTO/VO 精简**：从 `Competition` 实体、`CompetitionCreateRequestDTO`、`CompetitionUpdateRequestDTO`、`CompetitionDetailVO`、`AdminCompetitionDetailVO` 等类中全面移除冗余的 `stageType` 字段。
- **动态报名与临时战队**：
  - `CompetitionFieldSchema`：支持管理员自定义多字段报名表单（支持正则表达式校验、单选/多选、全员/仅队长填写）；
  - `CompetitionTeam` + `CompetitionMember` + `CompetitionRegistrationData`：实现赛事作用域内的临时队伍组建、邀请码入队、资质审核与表单数据留存；
- **五大阶段专属挂载与流转**：
  - `CTF`：`CtfCompetitionChallenge`、`CtfCompetitionSolved`、平滑动态积分衰减算法（Smooth Decay）、一血奖励与提交日志记录；
  - `THEORY`：`TheoryCompetitionPaper`、`TheoryCompetitionAnswer`、考生答题与毫秒级自动判卷；
  - `AWD`：`AwdCompetitionChallenge`、`AwdCompetitionRound`、`AwdCompetitionLog`、轮次推进与攻击/防守得分流转；
  - `AWD+`：`AwdPlusCompetitionChallenge`、`AwdPlusCompetitionPatch`、补丁上传与状态跟踪；
  - `ISW`：`IswCompetitionChallenge`、网络靶场场景拓扑与多阶段渗透推进。
- **全阶段综合天梯榜与封榜系统**：
  - `CompetitionScoreboardService`：支持 CTF 单阶段实时榜，以及基于各阶段加权公式计算的综合天梯榜（`CompositeScoreboardVO`），支持一键手动封榜与解封。

---

## 3. 构建与验证结果

执行全工程 clean test-compile 编译：
```bash
mvn clean test-compile
```
**编译结果**：11 个子模块全部一次性编译通过（`BUILD SUCCESS`）：
- `volur-lite-platform` ................................ SUCCESS
- `volur-framework` .................................... SUCCESS
- `volur-system` ....................................... SUCCESS
- `volur-user` ......................................... SUCCESS
- `volur-verification` ................................. SUCCESS
- `volur-auth` ......................................... SUCCESS
- `volur-challenge` .................................... SUCCESS (60 source files)
- `volur-container` .................................... SUCCESS (10 source files)
- `volur-scoreboard` ................................... SUCCESS (5 source files)
- `volur-competition` .................................. SUCCESS (124 source files)
- `volur-server` ....................................... SUCCESS
- **总耗时**：8.962 s

---

## 4. 影响范围与后续指引

- **影响范围**：
  - 用户表与认证模块不再维护全局解题数与积分；
  - 题库中心作为公共资产池，不直接提供面向公众的答题端点，所有解题行为统一收敛至具体竞赛与阶段路由下（`/api/v1/competitions/{id}/stage/*`）。
- **后续扩展建议**：
  - 后续可根据需要接入 WebSocket 推送（如一血全屏广播、AWD 轮次实时推送、封榜倒计时通知等）；
  - 可在 `volur-container` 中进一步丰富 AWD 多靶机与 ISW 复杂子网拓扑的自动化 Docker Compose / K8s CRD 调度。
