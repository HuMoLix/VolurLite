# 战队创建模式下“添加领队/指导老师”入口可见性修复与全链路审计

## 1. 变更背景与问题溯源

在竞赛详情页（`CompetitionDetailView.vue`）中，用户点击【报名参赛】弹出战队创建与报名弹窗（`TeamFormModal.vue`）时，界面未展示“添加领队/指导老师”的操作入口与名册模块。然而在报名成功后，再次打开【查看队伍信息】并点击【修改队伍信息】时，该功能却能正常显现。

经过对前端视图层、API 层、领域模型 DTO、后端 Thin Controller、Service 业务编排层及 MySQL 持久化实体（`CompetitionMember`）的全链路分析与审查：
1. **全链路准备就绪**：
   - 数据库表 `volur_competition_member` 支持 `role = 'LEADER'` 成员；
   - 后端 `TeamSignupRequestDTO` 与 `TeamUpdateRequestDTO` 均已声明 `@Valid private List<TeamLeadersSignupDTO> leaders`；
   - 后端 `CompetitionTeamServiceImpl.signupTeam` 与 `updateTeam` 均完整实现了领队人数限制判定（`maxLeaders`）、字段非空校验、防重校验及落库持久化；
   - 前端表单校验 `validateMembers()` 与请求 Payload 组装函数 `handleSubmit()` 在创建模式（`mode === 'create'`）下，均已完整收集并校验了 `sanitizedLeaders`；
   - 国际化（`zh_CN` 与 `en_US`）及右侧“报名须知”均明确声明了“最多 X 名指导老师/领队”。
2. **阻断点定位**：
   - 在 `TeamFormModal.vue` 模板层中，控制领队展开/收起切换文本链接的元素被硬编码添加了排他性判定：
     ```html
     <div v-if="isEditing && props.mode !== 'create'" class="leader-toggle-link-wrapper">
     ```
   - 此条件在创建战队时（`props.mode === 'create'`）恒为 `false`，导致切换入口从 DOM 树中被销毁；
   - 与此同时，创建战队时领队名册区域 `showLeadersSection` 初始值为 `false`，因无交互入口触发切换，导致领队录入表格彻底无法被用户打开；
   - 而在后续查看战队进入修改态时，由于 `props.mode` 保持为 `'view'` 且 `isEditing` 置为 `true`，`props.mode !== 'create'` 成立，该链接才得以渲染。

---

## 2. 核心改动点

### 2.1 视图层条件解耦与赛事配置联动

在 [`TeamFormModal.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/components/TeamFormModal.vue) 中：
- 移除硬编码的 `props.mode !== 'create'` 排除条件；
- 增加与赛事配置 `competition.maxLeaders` 的有效性联动判定：当处于编辑状态（无论是创建战队还是修改战队），且本场赛事配置允许配置领队（`maxLeaders > 0`）时，均展示【添加领队/指导老师】切换链接。

```diff
-      <!-- 领队/指导老师切换文本行: 仅在编辑模式下显示（编辑已有战队时） -->
-      <div v-if="isEditing && props.mode !== 'create'" class="leader-toggle-link-wrapper">
+      <!-- 领队/指导老师切换文本行: 编辑态下且赛事允许配置领队时显示 (创建与修改态均支持) -->
+      <div v-if="isEditing && (competition?.maxLeaders ?? 1) > 0" class="leader-toggle-link-wrapper">
         <span class="leader-toggle-link" @click="toggleLeadersSection">
           {{ showLeadersSection ? t('competition.hide_leader_link', '收起领队/指导老师') : t('competition.add_leader_link', '添加领队/指导老师') }}
         </span>
       </div>
```

---

## 3. 交互逻辑与边界条件验证

1. **创建模式（`mode === 'create'`）**：
   - 弹窗打开时，默认保持折叠，领队不占用初始可视空间；
   - 成员名册下方展示【添加领队/指导老师】链接；
   - 用户点击后，触发 `toggleLeadersSection()`，展开领队名册区域并自动初始化 1 行领队信息输入框；
   - 若用户不需要填写，可保持折叠状态不影响队伍创建提交；
   - 若用户填写了领队信息，提交时通过 `signupPayload` 自动携带至 `signupTeamApi`，后端正常入库。
2. **编辑模式（`mode === 'edit'` 或查看后点击“修改队伍信息”）**：
   - 若战队已存在领队，打开编辑时默认展开领队表格；
   - 若战队原无领队，展示【添加领队/指导老师】链接，点击后展开新增；
   - 点击【收起领队/指导老师】时，若已有实质内容填写，继续受到安全拦截保护（提示先清空）。
3. **赛事限制领队数量为 0 时（`maxLeaders === 0`）**：
   - 切换链接自动隐藏，防止用户在不支持领队的比赛中录入领队导致后端校验拦截。

---

## 4. 影响范围评估与验证

- **改动文件**：
  - `src/volur-lite-view/src/views/competition/components/TeamFormModal.vue`
- **编译与构建验证**：
  - 执行 `npx vue-tsc --noEmit`：0 类型报错，类型检查通过；
  - 执行 `npm run build`：Vite 前端完整打包通过（`built in 6.07s`）。
- **后端与数据库**：
  - 本次改动纯粹修复前端视图层限制条件，未涉及数据库 DDL/实体结构变更，无需调整数据库迁移脚本。
