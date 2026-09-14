# 战队报名表格化重构、领队限制与自定义报名扩展字段彻底下线审计报告

## 1. 变更背景与需求说明

在早期的赛事设计中，平台曾引入了基于键值对的自定义动态字段表单（`CompetitionFieldSchema` 与 `CompetitionRegistrationData`），允许管理员定义自定义报名填报字段。然而在实际竞赛组织与演练中，该模式存在诸多工程与安全弊端：
1. **数据结构非标准化**：动态键值对通过 JSON 或非强类型关系表存储，破坏了关系型数据库的强一致性约束与索引优化能力；
2. **缺乏统一审核与身份对齐**：传统动态表单无法对队员身份（真实姓名、身份证、手机号、单位及职务）进行强类型校验及平台账号关联；
3. **缺少领队（Leader）角色约束**：线下赛事或高校/行业对抗赛中，领队作为不计入常规上场解题名额的角色，亟需独立的配额管控（`maxLeaders`）；
4. **前端交互体验薄弱**：原有报名弹窗仅填写队伍名称与宣言，队员需后续逐个通过邀请码加入，流程冗长。

为此，本次重构执行以下核心调整：
- **彻底废除与清理自定义扩展字段数据体系**（实体、数据仓储、DTO、VO、枚举及服务逻辑全量移除）；
- **全栈对齐领队人数限制 (`maxLeaders`)**，并在队伍成员角色中正式确立 `LEADER`（领队）、`CAPTAIN`（队长）、`MEMBER`（队员）三元模型；
- **抽离独立战队报名组件 (`TeamSignupModal.vue`) 并重构为表格化录入**：支持队长/队员/领队信息在报名时以表格行的方式一键批量填报（姓名、性别下拉、角色下拉、身份证、邮箱、手机号、单位、部门、职位及动态增删行）；
- **后端统一校验与自动发函**：强校验队长唯一性、领队配额、队员区间、手机号注册态及重复性，并自动持久化至 `CompetitionMember` 实体字段与分发战队加入事件。

---

## 2. 变更内容与改动点清单

### 2.1 后端平台改造 (Java & Spring Boot)

#### 2.1.1 彻底删除的废弃类与文件（共 8 个）
- `com.humolix.volur.competition.domain.entity.CompetitionFieldSchema`
- `com.humolix.volur.competition.domain.entity.CompetitionRegistrationData`
- `com.humolix.volur.competition.repository.CompetitionFieldSchemaRepository`
- `com.humolix.volur.competition.repository.CompetitionRegistrationDataRepository`
- `com.humolix.volur.competition.domain.dto.CompetitionFieldSchemaDTO`
- `com.humolix.volur.competition.domain.vo.CompetitionFieldSchemaVO`
- `com.humolix.volur.competition.domain.enums.FieldType`
- `com.humolix.volur.competition.domain.enums.FillScope`

#### 2.1.2 新增与调整的模型实体与 DTO/VO
1. **新增 `TeamMemberSignupDTO.java`**：
   - 封装表格填报的 9 项成员明细：`realName`（@NotBlank）、`gender`、`role`（CompetitionMemberRole）、`idCard`、`email`（@Email）、`phone`（@NotBlank, 正则校验）、`organization`、`department`、`position`。
2. **重构 `CompetitionMember.java` (PO 实体)**：
   - 新增 `real_name`, `gender`, `id_card`, `email`, `phone`, `organization`, `department`, `position` 持久化字段，确保队员详细申报信息强类型存储并跟随参赛名册沉淀。
3. **重构 `CompetitionMemberVO.java` (VO 视图)**：
   - 增加所有上述成员明细视图属性，并保持驼峰命名规范。
4. **重构 `TeamSignupRequestDTO.java` & `TeamUpdateRequestDTO.java`**：
   - 入参结构统一调整为 `teamName`, `teamSlogan`, 以及 `List<TeamMemberSignupDTO> members`。
5. **对齐 `maxLeaders` 领队配置**：
   - 在 `CompetitionConfigDTO`、`CompetitionDetailVO`、`AdminCompetitionDetailVO`、`CompetitionAboutVO`、`CompetitionCreateRequestDTO`、`CompetitionUpdateRequestDTO` 中全量增加 `maxLeaders` 字段，并移除已废弃的字段模式列表。
6. **映射器 `CompetitionMapper.java` 更新**：
   - 映射 `config.maxLeaders`，并在 `toMemberVO` 中完整映射成员姓名、联系方式、单位等信息；移除废弃的 Schema 映射方法。

#### 2.1.3 核心业务逻辑重构 (`CompetitionTeamServiceImpl.java`)
- **表格化多成员批量报名处理**：
  1. 角色分类统计：强制校验 `CAPTAIN` 有且仅有 1 人；`LEADER` 不超过竞赛配置的 `maxLeaders`（默认 1）；常规队员（`CAPTAIN` + `MEMBER`）总数在 `[minTeamMembers, maxTeamMembers]` 合法区间内；
  2. 身份合法性判定：队长手机号对应用户必须严格等于当前登录用户（防止越权冒充建队）；
  3. 平台注册强校验：遍历成员手机号，严格要求所有成员手机号已在平台注册；若未注册则抛出明确业务异常；
  4. 成员去重与防兼赛：排查同队内重复手机号，以及各成员是否已参加该竞赛的其他战队；
  5. 数据持久化与事件驱动：将报名填写的 9 项信息完整落库至 `CompetitionMember`，队长直接设为激活态，其余队员发出邀请通知。
- **清理管理员服务 (`AdminCompetitionServiceImpl` & `AdminTeamServiceImpl`)**：
  - 彻底剥离对 `fieldSchemaRepository` 和 `registrationDataRepository` 的注入与数据操作。

---

### 2.2 前端工程改造 (Vue 3 / TypeScript / Naive UI)

#### 2.2.1 独立报名组件提取 (`src/volur-lite-view/src/views/competition/components/TeamSignupModal.vue`)
- **组件化隔离**：从 `CompetitionDetailView.vue` 剥离内联弹窗，封装为响应式卡片模态弹窗；
- **顶部战队基础信息**：支持输入战队名称（必填，2-32 字符）与战队宣言/口号（最多 128 字符）；
- **动态表格布局 (Responsive Table)**：
  - 列定义（自左向右）：**姓名**（必填）、**性别**（下拉选择：男/女/未知）、**角色**（队长/队员/领队）、**身份证**、**邮箱**、**手机号**（必填）、**单位**、**部门**、**职位**、**操作**（移除行）；
  - **首行预填**：自动读取当前登录用户的个人资料（Profile / Store），将第 1 行初始化为队长（锁定角色为队长不可更改/不可删除），预填用户名、邮箱、联系电话及单位；
  - **动态增减行**：提供“添加成员行”动作，动态扩充表格；首行防误删保护；
  - **前置严格校验**：在提交前执行纯前端闭环校验（包含队长唯一性、领队配额、常规成员人数区间、手机号正规性、邮箱格式、重复手机号检测），并与 Naive UI 提示体系无缝结合；
  - **视觉设计规范遵循**：提交主要操作按钮严格遵循 `#000000` 背景与 `#ffffff` 字体；说明文案严格使用 `#444444`。

#### 2.2.2 赛事详情页收敛 (`CompetitionDetailView.vue`)
- 引入 `<team-signup-modal>` 替代原有内联弹窗，保持视图简洁与关注点分离；
- 在参赛规则提示区中展示领队限额（`leader_limit`）。

#### 2.2.3 战队审核后台适配 (`TeamAuditTab.vue`)
- 彻底移除废弃的“队员报名自定义填报数据”块与 CSS；
- 升级战队成员名册卡片展示，清晰呈现队员真实姓名、账号、性别、手机、邮箱、身份证以及“单位/部门/职务”组合标签，大幅提升管理员审核效率。

#### 2.2.4 国际化与类型系统全量对齐
- 更新 `src/types/competition/index.ts`；
- 更新 `zh_CN` 与 `en_US` 语言包中与名册表格、字段列名、校验提示及领队限额相关文案。

---

## 3. 验证与构建结果

1. **后端编译与单测**：
   - 执行 `mvn clean test-compile`：全平台 12 个模块（`volur-framework` ~ `volur-server`）**100% BUILD SUCCESS**；
   - 执行 `mvn test -pl volur-competition`：**0 失败，0 错误**。
2. **前端类型检查与生产构建**：
   - 执行 `vue-tsc --noEmit && vite build`：全量 TypeScript 类型检查与静态资源打包**耗时 4.53s 零错误通过**。

---

## 4. 影响范围与审计结论

- **影响范围**：
  - 赛事创建与编辑（Admin BasicInfoTab、API 映射）；
  - 赛事详情与报名通道（CompetitionDetailView、TeamSignupModal）；
  - 战队成员数据存储与审核（CompetitionMember 实体、Admin TeamAuditTab）。
- **兼容性与风险评估**：
  - 旧有自定义表单数据已被结构化名册替代，新模式极大规避了非结构化数据泄露与恶意脚本注入风险；
  - 队长与成员信息与系统账号紧密绑定，杜绝冒名与幽灵队员参赛，显著增强实战对抗与竞赛审核的合规性。
