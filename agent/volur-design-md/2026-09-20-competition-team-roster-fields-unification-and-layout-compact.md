# 战队名册字段归一化与表单紧凑排版改造设计文档

## 1. 变更背景与需求拆分

为精简竞赛战队报名及管理业务的数据模型、杜绝冗余字段分散维护并优化队伍详情弹窗在桌面视口下的展示密度，对前后端进行了以下两阶段系统级调整与细节重构：

### 需求拆分 1：前端队伍表单弹窗 (`TeamFormModal.vue`) 细节交互与排版
1. **领队/指导老师切换链接与必填项约束**：
   - 切换文本链接“添加领队/指导老师”仅在编辑模式（`props.mode === 'edit'`）下显示；创建模式或查看模式下隐藏该链接（查看模式下若已有领队则直接渲染领队表格）。
   - 移除该文本链接的下划线样式（删除 `text-decoration: underline`）。
   - 当填写领队/指导老师时，**姓名 (`realName`) 和手机号 (`phone`) 为必填项**，在表头展示红色必填星号；其余属性（性别、工号/学号、邮箱、身份证号、部门/学院、职位/班级）均为选填项。
2. **队伍信息紧凑三行排版重构**：
   - 模态卡片宽度由 `1200px` 紧凑收缩至 `1040px`。
   - 删除战队头像下方的“点击更换战队头像”文字提示。
   - 调整为紧凑三行布局：
     - **第 1 行**：战队头像（`UserAvatar`）+ 战队名称（`teamName`）；
     - **第 2 行**：战队单位（`organization`）+ 单位类型（`organizationType`）；
     - **第 3 行**：队伍简介（`description` 多行文本）。

### 需求拆分 2：字段归并与统一模型对齐 (Field Unification)
1. 废除前端与后端中对学校与企业场景分别命名两套重复字段（`studentNumber`/`college`/`className` 与 `employeeNumber`/`department`/`position`）的冗余设计。
2. 全流程统一归并在底层三组通用字段上：
   - 学号 / 工号 统一收敛为 **`employeeNumber`**；
   - 学院 / 部门 统一收敛为 **`department`**；
   - 班级 / 职位 统一收敛为 **`position`**。
3. 后端 DTO 约束：
   - `TeamMemberSignupDTO`：`realName`(@NotBlank), `gender`(@NotNull), `role`(@NotNull), `phone`(@NotBlank, @Phone), `email`(@NotBlank, @Email), `idCard`(@NotBlank, @IdentificationCard), `employeeNumber`, `department`, `position`。
   - `TeamLeadersSignupDTO`：`realName`(@NotNull, @NotBlank), `phone`(@NotNull, @NotBlank, @Phone), 选填 `gender`, `email`, `idCard`, `employeeNumber`, `department`, `position`。

---

## 2. 影响范围与改动明细

### 2.1 后端工程 (`volur-competition`)

1. **DTO 层**：
   - `TeamMemberSignupDTO.java`：删除已过时的 `studentNumber`、`college`、`className` 字段。
   - `TeamLeadersSignupDTO.java`：删除 `studentNumber`、`college`、`className` 字段；为 `realName` 和 `phone` 添加 `@NotNull`、`@NotBlank` 及 `@Phone` 注解约束。
2. **Entity & VO 层**：
   - `CompetitionMember.java`：移除 `studentNumber`、`college`、`className` 列映射，统一使用 `employeeNumber`、`department`、`position`。
   - `CompetitionMemberVO.java`：移除 `studentNumber`、`college`、`className`，对齐最新 VO 定义。
3. **MapStruct 映射器**：
   - `CompetitionMapper.java`：移除过时的针对 `studentNumber` 与 `college` 的显式映射配置。
4. **业务服务层 (`CompetitionTeamServiceImpl.java`)**：
   - `signupTeam`、`updateTeam`：统一读取并校验 `employeeNumber`、`department`、`position`。
   - 领队保存与防重校验：严格要求 `realName` 与 `phone` 必填，使用 `StringUtils.hasText` 进行安全截断保护；其余信息为空时安全写入 `null`。
5. **单元测试 (`volur-competition/src/test/java`)**：
   - `TeamMemberSignupDTOValidationTest.java`：同步更新字段断言，增加针对领队姓名和手机必填 `@NotNull` 的测试覆盖。
   - `CompetitionTeamServiceImplTest.java`：测试 Mock 数据适配归一化字段。所有 26 个单元测试全部验证通过。

### 2.2 前端工程 (`volur-lite-view`)

1. **类型定义层 (`src/types/competition/index.ts`)**：
   - 更新 `TeamMemberSignupDTO`：精简掉冗余的 `studentNumber`、`college`、`className`。
   - 更新 `TeamLeadersSignupDTO`：`realName: string`，`phone: string` 改为必填，保留统一字段。
   - 更新 `CompetitionMemberVO`：移除冗余字段，对齐后端响应模型。
2. **管理端审计标签页 (`TeamAuditTab.vue`)**：
   - 表格中统一渲染 `row.employeeNumber || '—'` 与 `row.department || '—'`。
3. **战队表单模态窗 (`TeamFormModal.vue`)**：
   - 样式与宽度：卡片最大宽度调整为 `1040px`。
   - 表单顶部元数据：重构为 3 行紧凑流式布局，删除“点击更换战队头像”副文本；表单整体约束在 `max-width: 620px` 内，战队名称宽度收敛在 `360px`，单位输入与单位类型为 `320px + 160px`，队伍简介最大宽度 `500px`，避免输入框在宽屏下过度横向拉伸；将行间距收敛至 `6px`，收缩反馈占位及标签留白，排版紧凑。
   - 领队切换链接：修正可见性判定为 `isEditing && props.mode !== 'create'`，确保从查看状态点击“修改队伍信息”后该链接正常显现，并移除下划线样式。
   - 角色样式重构：将队员名册及领队名册中的队内角色由原胶囊标签（`<n-tag round>`）重构为纯文本样式（`.role-text`），视觉更为简约干练。
   - 领队名册展示：查看模式下若已有领队自动渲染，编辑模式下可自由折叠展开。
   - 队员/领队表格表头与单元格：基于 `organizationType === 'ENTERPRISE'` 动态呈现“工号/部门/职位”与“学号/学院/班级”，内部统一双向绑定同一组状态字段。
   - 表单校验 `validateMembers()`：严格校验领队姓名和手机号的非空与正则合法性。
   - 接口载荷 `handleSubmit()`：统一序列化生成标准化 DTO。

---

## 3. 验证与构建结果

1. **后端验证**：
   - 运行命令：`mvn test -pl volur-competition`
   - 结果：`Tests run: 26, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
2. **前端验证**：
   - 运行命令：`npm run build` (`vue-tsc --noEmit && vite build`)
   - 结果：TypeScript 编译零错误，Vite 构建打包成功（耗时 5.59s）。
