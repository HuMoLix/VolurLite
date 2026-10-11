# 2026-10-12 CompetitionDetailView 实现与赛事详情全链路联调记录

## 1. 变更背景与需求概述
在 Volur Lite 新一代前端工程（`src/volur-lite-view-NEXT`）中，用户在赛事列表页（`CompetitionListView.vue`）点击 `VCompetitionCard` 卡片后，路由会跳转至 `/competitions/:id` 或短链接 `/c/:slug`。此前该路由仅配置了占位重定向，缺乏完整的赛事详情页面。

本阶段任务是在 `src/volur-lite-view-NEXT/src/views/competition/` 目录下完整实现 `CompetitionDetailView.vue`，为用户提供赛事概要、赛程展示、规则与须知、公告展示、战队报名与状态感知，以及进入对应竞技场工作台的完整功能闭环。

---

## 2. 核心架构设计与变更内容

### 2.1 页面视觉与 Google Material Design 3 规范对齐
- **表面层级与色彩体系**：严格对齐 Google Workspace 与 Material Design 规范，基底背景采用浅灰 `#f8f9fa`，暗色采用 `#1e1f20` / `#202124` / `#0e0e0e`；正文文本统一采用 Google Gray 体系（`#202124` / `#3c4043` / `#5f6368`，暗色 `#e3e3e3` / `#bdc1c6` / `#9aa0a6`），彻底杜绝刺眼纯黑 `#000000`。
- **主交互按钮与药丸标签**：主要操作按钮采用 Google Blue（亮色 `#1a73e8`，暗色高对比反色 `#8ab4f8`）；状态胶囊徽章具备运行中心跳动效（`animate-ping`）；导航采用全药丸（Pill）圆角。
- **顶部加载指示器**：在数据加载与流转时，统一使用 Material 3 风格双段推进线性进度条（`progress-bar-primary` / `progress-bar-secondary`）。

### 2.2 赛事状态机与战队主控制流转 (Action Status Lifecycle)
在主控制卡片中，根据当前用户的登录凭证、组队状态与赛事时序构建了严格的状态机闭环：
1. **未登录状态**：友好引导卡片，展示“登录开启参赛体验”，并携带当前页面路径进行 redirect 导航至 `/v1/signin/identifier`；
2. **已登录且未报名**：
   - 当比赛已开始时，严格遵循防作弊规则隐藏报名通道，仅保留赛程与规则浏览；
   - 当处于报名开放期时，展示战队人数约束、领队约束、收到的待处理组队邀请提示，以及“立即报名参赛”唤起弹窗；
   - 报名未开放或截止时，显示精确状态提示；
3. **已登录且已组队**：
   - 展现战队信息摘要、身份角色（队长/领队/队员）、所属单位及待处理组队邀请；
   - 状态分支：
     - `PENDING_AUDIT` / `AUDIT_DRAFT`：显示黄色告警“队伍报名审核中”；
     - `AUDIT_REJECTED`：显示红色告警“战队审核未通过”及审核理由；
     - `AUDIT_PASSED` + 未开始：显示审核通过状态与开启倒计时（`countdownToStart`）；
     - `AUDIT_PASSED` + 进行中：高亮展示比赛结束倒计时（`countdownToEnd`）与核心“进入竞赛工作台”按钮；
     - `AUDIT_PASSED` + 已结束：显示已结束状态并保留“回顾复盘”按钮。

### 2.3 模块化解耦与组件封装
在 `src/volur-lite-view-NEXT/src/views/competition/components/` 下抽离三个高内聚子组件：
- **`TeamSignupModal.vue`**：战队创建与报名弹窗，支持团队基本信息、队长身份、队员增删与领队信息收集，并提交至后端 `signupTeamApi`；
- **`TeamDetailModal.vue`**：战队成员与信息查看弹窗，队长拥有解散战队、移除队员与重新发送邀请权限；
- **`TeamInvitationsModal.vue`**：组队邀请处理弹窗，支持接受或拒绝待处理邀请。

### 2.4 API 与类型对齐
1. **类型层（`src/types/competition/index.ts`）**：
   - 补充完善 `TeamStatus`、`MemberRole`、`MemberStatus`、`OrganizationType`；
   - 补充 `CompetitionMemberVO`、`MyTeamInfoVO`、`TeamInvitationVO`、`TeamMemberSignupDTO`、`TeamLeadersSignupDTO`、`TeamSignupRequestDTO`、`TeamUpdateRequestDTO`、`TeamInviteAcceptDTO`。
2. **API 请求层（`src/api/competition.ts`）**：
   - 新增 `signupTeamApi`、`getMyTeamApi`、`updateMyTeamApi`、`deleteMyTeamApi`、`getMyInvitationsApi`、`acceptInvitationApi`、`rejectInvitationApi`、`cancelInvitationApi`、`reinviteMemberApi`。
3. **路由层（`src/router/routes/competition.ts`）**：
   - 将 `competitions/:id` 与 `c/:id` 绑定至 `CompetitionDetailView.vue`；
   - 将理论赛工作台路由 `competitions/:id/theory` 绑定至 `TheoryCompetitionView.vue`。

---

## 3. 规范执行与安全考量
1. **文档外置与零注释铁律**：所有新增及修改的代码文件（`.vue`、`.ts`）均严格执行零行内解释性注释、零文档字符串，全部逻辑与技术细节收敛至本文档中；
2. **Fail-Fast & Zero-Mock 原则**：杜绝任何假数据和隐式默认值，API 数据通过 Axios 强类型响应管道实时获取，未匹配数据或缺失字段直接触发异常提示与边界阻断；
3. **资源清理与内存防护**：在组件卸载（`onUnmounted`）时主动清理倒计时定时器（`clearInterval`），避免内存泄漏与后台无意义消耗。

---

## 4. 影响范围与验证结果
- **变更文件清单**：
  - `src/volur-lite-view-NEXT/src/types/competition/index.ts`
  - `src/volur-lite-view-NEXT/src/api/competition.ts`
  - `src/volur-lite-view-NEXT/src/router/routes/competition.ts`
  - `src/volur-lite-view-NEXT/src/views/competition/CompetitionDetailView.vue`
  - `src/volur-lite-view-NEXT/src/views/competition/components/TeamSignupModal.vue`
  - `src/volur-lite-view-NEXT/src/views/competition/components/TeamDetailModal.vue`
  - `src/volur-lite-view-NEXT/src/views/competition/components/TeamInvitationsModal.vue`
- **构建验证**：
  - 执行 `vue-tsc --noEmit`：0 错误，严格类型校验全部通过；
  - 执行 `npm run build`：生产环境打包成功，成功生成 `CompetitionDetailView` 产物。
