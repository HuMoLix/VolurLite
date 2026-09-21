# TeamFormModal 文案精简与单位类型全链路拓展

## 1. 变更背景

在竞赛战队报名与管理模态框（`TeamFormModal`）中，原有的部分文本提示较为冗长或存在误导，且单位类型枚举限制过窄：
1. **右侧审核状态标签冗余**：右侧卡片顶部标题右侧展示的“需人工审核”/“免审核”状态标签对于参赛者填报产生不必要的心理预期压力，用户明确要求彻底去除此审核标签；
2. **说明后缀冗长**：自定义特别说明部分的标题“特别说明（非系统自动校验）”带有过多的技术实现色彩，需精简为“特别说明”；
3. **输入占位符冗余**：
   - 队伍所属单位输入框的原 placeholder 为“请输入队伍所属单位（学校或企业等）”，文案冗长，需修改为“请输入队伍所属单位”；
   - 队伍简介原 placeholder 为“请输入队伍简介（可选，展示于战队主页）”，需精简为“请输入队伍简介”；
4. **单位类型维度拓展**：原系统仅支持高校（`SCHOOL`）与企业单位（`ENTERPRISE`）两类，无法满足政务、国防军工、能源电力、金融科技及科研院所等多元化参赛主体的归属划分，需要对前后端枚举与数据验证逻辑进行全链路拓展。

---

## 2. 核心改动点

### 2.1 后端枚举与校验拓展 (`volur-competition`)
1. **枚举定义拓展 (`OrganizationType.java`)**：
   - 新增 `MILITARY("军工")`、`ENERGY("能源")`、`FINANCE("金融")`、`RESEARCH("科研院所")`、`OTHER("其他")`；
   - 将原 `ENTERPRISE("企业")` 描述规范为 `ENTERPRISE("企业单位")`。
2. **报名与更新业务校验逻辑抽象 (`CompetitionTeamServiceImpl.java`)**：
   - 原代码中通过 `request.getOrganizationType() == OrganizationType.ENTERPRISE` 判定是否采用工号/部门/职位等企业型身份字段；
   - 重构为 `!= OrganizationType.SCHOOL`，使得所有非高校单位类型（企业单位、军工、能源、金融、科研院所、其他）均统一步调，使用工号、部门、职位等字段校验，仅高校（`SCHOOL`）要求学号、学院、班级。

### 2.2 前端类型定义与国际化拓展
1. **类型实体 (`src/types/competition/index.ts`)**：
   - 拓展 `OrganizationType` 联合类型为 `'SCHOOL' | 'ENTERPRISE' | 'MILITARY' | 'ENERGY' | 'FINANCE' | 'RESEARCH' | 'OTHER'`，严格契合前后端 1:1 对齐与零 `any` 原则。
2. **多语言包更新 (`zh_CN/competition.json` 与 `en_US/competition.json`)**：
   - 更新 `team_organization_placeholder` 为“请输入队伍所属单位” / "Enter organization"；
   - 更新 `team_desc_placeholder` 为“请输入队伍简介” / "Enter team description"；
   - 更新 `req_admin_custom_title` 为“特别说明” / "Special Notice"；
   - 补充新增单位类型的双语词条：`org_type_military`、`org_type_energy`、`org_type_finance`、`org_type_research`、`org_type_other`；
   - 清理底部历史遗留的重复标签键。

### 2.3 TeamFormModal 视图与交互微调 (`TeamFormModal.vue`)
1. **左侧表单输入项文案精简**：
   - 队伍单位输入框 placeholder 调整为 `t('competition.team_organization_placeholder', '请输入队伍所属单位')`；
   - 队伍简介输入框 placeholder 调整为 `t('competition.team_desc_placeholder', '请输入队伍简介')`。
2. **单位类型下拉选项扩充**：
   - `organizationTypeOptions` 计算属性扩充为包含学校、企业单位、军工、能源、金融、科研院所、其他的完整配置项，均通过 `t()` 国际化动态读取。
3. **名册表格列头与占位逻辑统一**：
   - 将原有表头与输入框内 `formData.organizationType === 'ENTERPRISE'` 的硬编码判定，统一重构为 `formData.organizationType !== 'SCHOOL'`；
   - 参赛成员与领队老师表格均自适应区分高校（学号/学院/班级）与企业及各行业单位（工号/部门/职位）。
4. **右侧须知卡片标题与内容文案深度精简**：
   - 彻底移除 `requirements-header` 中原有的审核状态徽标（`需人工审核` / `免审核`），使头部聚焦于“竞赛报名要求与须知”；
   - 人数要求规则行由原“每队 1 ~ 4 人（含队长）”精简为“每队 1 ~ 4 人”，移除冗余的“（含队长）”字样；
   - 领队要求规则行由原“最多 1 名指导老师/领队（可选填）”精简为“最多 1 名指导老师/领队”，移除冗余的“（可选填）”字样；
   - 管理员自定义补充要求卡片标题由“特别说明（非系统自动校验）”简化为“特别说明”。
5. **客户端提交校验逻辑适配**：
   - 表单提交前名册字段校验规则 `isEnterprise = formData.organizationType !== 'SCHOOL'`，确保军工、金融等类型成员录入工号与部门职位时能够顺利通过前端验证。

---

## 3. 安全与兼容性说明

| 考量维度 | 潜在风险 | 防范与解决方案 |
| :--- | :--- | :--- |
| **数据库兼容性** | 拓展后的枚举字段超出数据库字段长度 | `CompetitionTeam.java` 中 `organization_type` 映射定义为 `VARCHAR(32)`，最长枚举值 `ENTERPRISE`（10 字符）完全处于安全容量范围内，无需更改 DDL |
| **老旧数据反序列化** | 历史创建战队缺少新枚举值导致报错 | 新增枚举类型完全向后兼容，历史已有战队（`SCHOOL`、`ENTERPRISE`）数据不受任何影响 |
| **多语言回退保障** | 国际化词条加载异常导致文本缺失 | 在 `TeamFormModal.vue` 的 `t()` 函数中均提供了安全中文 Fallback 兜底文案 |

---

## 4. 影响范围与验证结果

- **影响文件清单**：
  - 后端：
    - `src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/enums/OrganizationType.java`
    - `src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/impl/CompetitionTeamServiceImpl.java`
  - 前端：
    - `src/volur-lite-view/src/types/competition/index.ts`
    - `src/volur-lite-view/src/locales/zh_CN/competition.json`
    - `src/volur-lite-view/src/locales/en_US/competition.json`
    - `src/volur-lite-view/src/views/competition/components/TeamFormModal.vue`
- **构建与测试验证**：
  - 前端执行 `npm run build`：TypeScript 严格类型检查与 Vite 打包通过，零错误；
  - 后端执行 `mvn test -pl volur-competition`：全部 26 项单元及集成测试用例均 100% 通过。
