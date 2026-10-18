# 竞赛 LazyInitializationException 修复、@Valid 废弃消除与任意字段导出架构重构记录

## 一、 故障背景与根因深度剖析

### 1. HV000271 校验器注解容器废弃告警 (Hibernate Validator Deprecation)
#### 故障日志
```text
2026-10-18T16:50:31.835+08:00  WARN 1 --- [volur-server] [mcat-handler-33] o.h.v.i.m.a.CascadingMetaDataBuilder     : HV000271: Using `@Valid` on a container (java.util.List) is deprecated. You should apply the annotation on the type argument(s). Affected element: leaders
2026-10-18T16:50:31.847+08:00  WARN 1 --- [volur-server] [mcat-handler-33] o.h.v.i.m.a.CascadingMetaDataBuilder     : HV000271: Using `@Valid` on a container (java.util.List) is deprecated. You should apply the annotation on the type argument(s). Affected element: members
```
#### 根因剖析
在 Jakarta Bean Validation 2.0+ 与 Hibernate Validator 7+ 规范中，对泛型集合/容器字段直接标记 `@Valid`（如 `@Valid private List<TeamMemberSignupDTO> members`）已被标记为废弃（Deprecated）。规范要求将级联校验注解显式标注在类型实参（Type Argument）上，即声明为 `List<@Valid TeamMemberSignupDTO> members`，以提供更精确的容器元素约束上下文与编译期类型安全性。
受影响 DTO 包括：
- `TeamSignupRequestDTO.java`
- `TeamUpdateRequestDTO.java`
- `AdminTeamUpdateDTO.java`

### 2. Hibernate LazyInitializationException 懒加载断链异常
#### 故障日志
```text
org.hibernate.LazyInitializationException: Could not initialize proxy [com.humolix.volur.competition.domain.entity.Competition#370131266680389638] - no session
	at org.hibernate.proxy.AbstractLazyInitializer.initialize(AbstractLazyInitializer.java:178)
	at com.humolix.volur.competition.domain.entity.Competition$HibernateProxy.getTitle(Unknown Source)
	at com.humolix.volur.competition.service.impl.CompetitionTeamServiceImpl.getMyTeam(CompetitionTeamServiceImpl.java:815)
	at com.humolix.volur.competition.controller.v1.CompetitionTeamController.getMyTeam(CompetitionTeamController.java:39)
```
#### 根因剖析
1. **实体延迟加载属性未被预抓取 (Lack of Join Fetch)**：
   - `CompetitionTeam` 实体中持有竞赛引用 `@ManyToOne(fetch = FetchType.LAZY) private Competition competition;`。
   - `memberRepository.findMembersWithTeam(competitionId, userId)` 的 JPQL 仅抓取了 `m.team t`，未对 `t.competition` 执行 `JOIN FETCH`。
   - 在执行查询后，Hibernate 仅为 `team.competition` 注入了一个轻量级代理对象（`Competition$HibernateProxy`）。
2. **事务会话脱节 (Detached Session Context)**：
   - `CompetitionTeamServiceImpl` 类及 `getMyTeam(competitionId, userId)`、`getTeamById(competitionId, teamId, userId)` 等读方法上均未标注事务注解 `@Transactional`。
   - 当 Repository 执行完成离开后，持久化上下文（Hibernate Session）即刻释放关闭。
   - 随后的业务代码执行 `team.getCompetition().getTitle()` 时，代理试图向数据库发送按需查询，因 Session 已关闭而直接抛出 `LazyInitializationException`。

---

## 二、 修复方案实现

### 1. 消除 HV000271 校验器注解废弃
将上述三个 DTO 中的集合声明严格重构为类型实参标注：
- `TeamSignupRequestDTO.java`: `private List<@Valid TeamMemberSignupDTO> members = new ArrayList<>();` 及 `leaders`
- `TeamUpdateRequestDTO.java`: `private List<@Valid TeamMemberSignupDTO> members = new ArrayList<>();` 及 `leaders`
- `AdminTeamUpdateDTO.java`: `private List<@Valid AdminTeamMemberDTO> members;` 及 `leaders`

### 2. 补齐预抓取与服务层只读事务边界
1. **Repository 层抓取增强 (`CompetitionMemberRepository.java`)**：
   - `findMemberWithTeam` 与 `findMembersWithTeam` 追加 `JOIN FETCH t.competition c LEFT JOIN FETCH t.captain cap`，在单次 SQL 连表中完整装配队伍所属竞赛与队长实体；
   - 新增方法 `findByCompetitionIdWithUserAndTeam`，供成绩与报名数据全量加载使用。
2. **Service 层事务防护 (`CompetitionTeamServiceImpl.java`)**：
   - 类级别标注 `@Transactional(readOnly = true)`，写方法显式保留 `@Transactional(rollbackFor = Exception.class)`；
   - `getTeamById` 调整为调用 `teamRepository.findByIdWithCompetitionAndCaptain(teamId)`，确保多租户与信息装配在持久化会话保护下安全执行。

---

## 三、 竞赛任意字段导出功能重构与增强 (Custom Fields Export Architecture)

### 1. 业务需求与技术难点
此前竞赛仅提供固定列的成绩总榜和解题流水。实际赛务场景（学分核算、获奖颁奖、线下身份校验、保险购买）强烈要求支持**任意自定义字段导出**，特别是用户报名提交的真实信息（**学号/工号、身份证号、手机号、电子邮箱、真实姓名、学院/部门、班级/专业/职务**等）。

### 2. 后端架构重构 (`volur-competition`)
1. **领域模型扩展 (`domain.vo.export`)**：
   - `ExportFieldOptionVO`: 统一返回可选导出字段元数据（字段键名、中文标签、业务分组、默认选中状态）；
   - `CtfTeamRankExportVO`: 扩充队长报名信息字段（`leaderGender`, `leaderPhone`, `leaderEmail`, `leaderIdCard`, `leaderEmployeeNumber`, `leaderDepartment`, `leaderPosition`）；
   - `CtfTeamMemberExportVO`: 新增全量队员与领队报名信息载体；
   - `CtfScoreExportVO`: 聚合加入 `members` 列表；
   - `TheoryCandidateScoreExportVO`: 扩充考生报名信息字段（学号、身份证、手机、邮箱、部门、职位、性别）。
2. **多 Sheet 与多 Section 导出引擎增强**：
   - `ExcelExportUtil`: 新增 `SheetData(sheetName, headers, rows)` 与 `writeSheets(os, sheets)` 流式写入多 Sheet 工作簿；
   - `CsvExportUtil`: 新增 `CsvSection(title, headers, rows)` 与 `writeSections(os, sections)` 统一支持单段/多段分节输出。
3. **动态列定义与萃取引擎 (`AdminCompetitionScoreExportServiceImpl`)**：
   - 抽象类型安全且零反射开销的 `ColumnDefinition<T>(key, header, group, defaultSelected, extractor)`；
   - 建立完备的字段字典与别名映射机制（如 `employeeNumber` 自动适配 `leaderEmployeeNumber`）；
   - 支持客户端传递任意逗号分隔或数组格式的 `fields` 参数，动态解析并按请求顺序构建表头与数据行；若未指定则使用系统默认推荐字段集；
   - 统一支持 `summary`（总榜）、`members`（选手报名全量名单）、`detail`（答题流水）、`all`（完整聚合）四大业务范围；
   - JSON 模式动态序列化仅包含选中字段的有序键值对。
4. **Thin Controller 与字段探测端点 (`AdminCompetitionScoreExportController`)**：
   - 新增 `GET /api/v1/admin/competitions/{competitionId}/scores/export/fields`，返回可选字段列表；
   - 导出接口 `GET /api/v1/admin/competitions/{competitionId}/scores/export` 支持 `fields` 动态过滤。

### 3. 前端界面与交互设计 (`volur-lite-view-NEXT`)
1. **API 层扩充 (`src/api/competition.ts`)**：
   - 定义 `ExportFieldOptionVO` 接口；
   - 导出 `getAdminCompetitionExportFieldsApi` 与升级后的 `exportAdminCompetitionScoresApi`。
2. **全功能导出交互模态框 (`AdminCompetitionScoreExportModal.vue`)**：
   - 遵循 Google Material Design 现代扁平规范与阶梯表面设计；
   - 增加【选手及报名明细】（包含学号、身份证、手机号等）及【完整聚合导出】范围单选卡片；
   - 增加【自定义导出字段】面板，按业务分组（选手报名信息、成绩与考评数据、基础与战队信息、流水明细）提供多选；
   - 标配【全选】、【默认】与【清空】快捷操作；
   - 切换导出范围时自适应拉取与联动候选字段；
   - 支持 Blob 动态命名与无缝本地触发下载。
3. **管理端多场景入口植入**：
   - `AdminCtfCompetitionEditView.vue` & `AdminTheoryCompetitionEditView.vue`: 顶部操作区统一升级为【导出数据报表】；
   - `AdminCtfCompetitionTeamView.vue` & `AdminTheoryCompetitionTeamView.vue`: 工具栏新增【导出名单】快捷按钮，直接以 `members` 模式调起导出弹窗。

---

## 四、 影响范围与验证结果

- **影响后端模块**：`volur-competition`
- **影响前端模块**：`volur-lite-view-NEXT`
- **验证结果**：
  1. 后端执行 `mvn test-compile -DskipTests` 全平台 13 个模块全部编译通过，退出码 0；
  2. 前端执行 `npm run build`（经 `vue-tsc --noEmit` 严格类型推导与 Vite 打包）编译构建通过，退出码 0。
