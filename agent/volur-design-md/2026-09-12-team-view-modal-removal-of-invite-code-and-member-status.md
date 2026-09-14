# 战队信息查看模态框重构、移除邀请码展示及成员加入状态展示审计记录

## 1. 变更背景与需求概述
根据竞赛管理与队伍协作的业务迭代要求，对前端竞赛详情页与战队信息管理流程进行重构与收敛：
1. **移除队伍邀请码机制**：
   - 彻底废除队伍信息展示栏上的队伍邀请码（`inviteCode`）标签及点击复制功能，杜绝通过邀请码被动加入队伍的旧式机制，组队全面依赖通过邮箱或系统流程定向邀请；
2. **“修改队伍信息”变更为“查看队伍信息”模态框**：
   - 将战队卡片右侧的“修改队伍信息”按钮修改为“查看队伍信息”（`view_team_btn`）；
   - 该按钮面向战队全体成员（队长、队员、领队）开放，不再限制仅队长可见；
   - 点击后打开 `TeamFormModal.vue` 并默认以查看模式（`view`）呈现；
3. **在查看队伍信息中展示成员加入状态**：
   - 成员列表表格中增加“加入状态”列（`col_status`），直观展示成员是否已经接受邀请并加入战队：
     - `INVITED_ACCEPTED`（已加入战队，绿标 `success`）；
     - `INVITED`（待接受邀请，橙标 `warning`）；
     - `NOT_PARTICIPATE`（已拒绝，灰标 `default`）；
     - 未保存的新增行显示待邀请（蓝标 `info`）；
4. **支持在查看模态框中切换进入修改模式**：
   - 队长在查看队伍信息时，模态框底部提供“修改队伍信息”按钮，点击后切换至可编辑状态（`isEditing = true`）；
   - 普通成员与领队在查看模态框中仅可浏览信息，无权进入编辑模式；
   - 编辑模式下提供“取消修改”（恢复原始数据并切回查看模式）、“保存并重新提交审核”与“解散战队”操作；
5. **后端 API、逻辑与行为对齐**：
   - `MyTeamInfoVO` 补充 `isCaptain` 属性，方便前端精确判定当前用户是否为该队伍队长；
   - `CompetitionTeamServiceImpl.getMyTeam` 对成员名册进行稳定有序排序（队长排在最前，其次队员、领队，同角色按 ID 升序排列），并填充 `isCaptain`。

---

## 2. 改动点清单与技术实现

### 2.1 后端改动 (Java / Spring Boot)

1. **`MyTeamInfoVO.java` (`volur-competition`)**:
   - 增加字段 `private Boolean isCaptain;`，显式标识当前请求用户在队伍中是否具备队长权限。

2. **`CompetitionTeamServiceImpl.java` (`volur-competition`)**:
   - 在 `getMyTeam` 方法中，通过用户在当前队伍的 `role == MemberRole.CAPTAIN` 计算并注入 `.isCaptain(...)`；
   - 对 `members` 成员列表添加顺序比较器排序：
     ```java
     members.sort(Comparator
         .comparingInt((CompetitionMember m) -> switch (m.getRole()) {
             case CAPTAIN -> 0;
             case MEMBER -> 1;
             case LEADER -> 2;
         })
         .thenComparing(CompetitionMember::getId));
     ```
     确保队长永远稳定占据首行，队员与领队紧随其后。
   - `CompetitionMemberVO` 中的 `status` 字段由 MapStruct 自动完成枚举映射，返回 `INVITED`、`INVITED_ACCEPTED`、`NOT_PARTICIPATE`。

---

### 2.2 前端改动 (Vue 3 / TypeScript / Naive UI)

1. **类型与国际化字典 (`src/types/competition/index.ts`, `src/locales/`)**:
   - 导出 `MemberStatus = 'INVITED' | 'INVITED_ACCEPTED' | 'NOT_PARTICIPATE'`；
   - 在 `CompetitionMemberVO` 中明确声明 `status?: MemberStatus`；
   - 在 `zh_CN/competition.json` 与 `en_US/competition.json` 中添加多语言翻译项：
     - `view_team_modal_title`: 查看战队信息
     - `view_team_btn`: 查看队伍信息
     - `edit_team_btn`: 修改队伍信息
     - `cancel_edit_btn`: 取消修改
     - `col_status`: 加入状态
     - `status_joined`: 已加入战队
     - `status_invited`: 待接受邀请
     - `status_rejected`: 已拒绝
     - `status_pending`: 待邀请

2. **战队表单与查看模态框 (`src/views/competition/components/TeamFormModal.vue`)**:
   - `props.mode` 支持 `'create' | 'view' | 'edit'`，默认为 `'view'`；
   - 内部维护 `isEditing` 响应式变量：
     - 在 `create` 模式下直接处于编辑态；
     - 在 `view` 模式下默认只读；
   - 表格动态增加“加入状态”列：使用 `NTag` 渲染成员实时加入状态（已加入、待接受邀请、已拒绝）；
   - 在只读模式下，禁用表单输入控件（姓名、性别、身份证号、邮箱等），隐藏末尾操作列与“添加成员行”按钮；
   - 底部按钮控制：
     - 查看模式下提供“关闭”按钮；若当前用户是队长（`isCaptain`），额外呈现“修改队伍信息”与“解散战队”按钮；
     - 编辑模式下提供“取消修改”（重置数据）、“保存并重新提交审核”与“解散战队”按钮；
   - 移除队伍名称输入框下方“队伍名称已锁定不可修改”辅助提示文字，清理 `.form-item-tip` 样式及多语言词条 `team_name_disabled_tip`。

3. **赛事详情页 (`src/views/competition/CompetitionDetailView.vue`)**:
   - 战队信息卡片顶部彻底移除 `invite-tag` 邀请码标签与复制逻辑（及相关 CSS 样式与无用 `useMessage` 依赖）；
   - 战队操作栏提供“查看队伍信息”按钮（全体战队成员可见），使用 `EyeOutline` 图标；
   - 移除旧的 `openEditTeamModal`，接入 `openViewTeamModal`，以 `'view'` 模式呼出 `TeamFormModal`。

---

## 3. 验证情况
1. **前端构建验证**：
   - 运行 `npm run build`，TypeScript 与 Vite 静态编译全部通过，0 错误，打包完成。
2. **后端编译验证**：
   - 运行 `mvn test-compile -pl volur-competition`，编译状态为 `BUILD SUCCESS`。
