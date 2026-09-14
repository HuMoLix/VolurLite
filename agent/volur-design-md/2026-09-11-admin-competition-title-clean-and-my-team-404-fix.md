# 竞赛编辑标题区域修整与查询战队 404 报错修复审计说明

## 1. 变更背景与需求说明
根据用户的最新指令：
1. **清理编辑页面标题区冗余标签与描述**：
   > 「修改 @[src/volur-lite-view/src/views/admin/competition/AdminCompetitionEditView.vue] 中删除标题旁边的“一赛一制架构“字样和”CTF“字样，仅保留竞赛状态。删除“比赛全生命周期中枢：统一管理赛事基础信息与赛程周期、专属公告通报、赛题挂载策略、容器并发配额、选手题解 WP 及战队准入资质审核。”字样。」
2. **排查并解决预览页面提示「您尚未加入该竞赛的任何队伍」的异常现象**：
   > 「然后/admin/competitions/356683249897963529/preview为什么还会显示“您尚未加入该竞赛的任何队伍”。」

---

## 2. 核心改动点与实现

### 2.1 编辑视图标题区精简 ([`AdminCompetitionEditView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/AdminCompetitionEditView.vue))
- 从标题栏右侧移除了「一赛一制架构」信息标签与赛事类型（如「CTF」）标签，**仅保留当前赛事实时状态标签**（如 `RUNNING`、`REGISTRATION`、`DRAFT` 等）；
- 彻底移除了副标题描述段落（*「比赛全生命周期中枢：统一管理赛事基础信息与赛程周期...」*）；
- 清理了 `<style lang="scss" scoped>` 中不再被使用的 `.subview-desc` 样式规则。

### 2.2 「您尚未加入该竞赛的任何队伍」异常根因分析与修复
#### 根因剖析：
1. 当用户访问 `/admin/competitions/:id/preview` 时，前台组件 [`CompetitionDetailView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionDetailView.vue) 会挂载并执行 `loadCompetition()`。
2. 数据加载后，逻辑判定管理员为已登录用户（`userStore.isLoggedIn === true`），继而调用 `fetchMyTeamStatus(compId)` 发起 `GET /api/v1/competitions/{compId}/teams/me` 请求，探测当前用户是否加入了参赛战队。
3. 在后端 [`CompetitionTeamServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/impl/CompetitionTeamServiceImpl.java) 中，历史代码为：
   ```java
   CompetitionMember member = memberRepository.findMemberWithTeam(competitionId, userId)
           .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "您尚未加入该竞赛的任何队伍"));
   ```
   后端将「当前用户尚未加入任何队伍」视为异常抛出 `404 NOT_FOUND`，返回消息文案即为 **“您尚未加入该竞赛的任何队伍”**。
4. 前端 Axios 统一响应拦截器在捕获到 404 错误响应时，无条件触发 `window.$message.error(message)` 弹出红色 Toast 错误提示。这不仅在管理员预览时产生无端报警，也导致任何已登录普通参赛选手首次进入公开赛事详情页时，均会无端遭遇该错误 Toast。

#### 解决方案与实现：
1. **后端契约重构 ([`CompetitionTeamServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/impl/CompetitionTeamServiceImpl.java))**：
   - 将查询当前队伍接口判定改为：当 `memberRepository.findMemberWithTeam(competitionId, userId)` 为空时，**直接返回 `null`**，代表用户目前无队伍归属；
   - 控制器正常返回 HTTP 200 `ApiResponse<MyTeamInfoVO>`（`data: null`），符合 RESTful 查询语义，彻底消除了非预期的 404 业务异常抛出。
2. **前端预览模式与队伍拉取状态隔离 ([`CompetitionDetailView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionDetailView.vue))**：
   - 在 `fetchMyTeamStatus` 与 `loadCompetition` 中加入预览模式防护：当 `isAdminPreview.value === true` 时，直接将 `myTeam.value` 置为 `null` 并跳过网络请求；
   - 在正常公开详情页访问时，安全解析 `res.data`，无论有无队伍均平稳流转，不再冒泡任何红色 Toast 提示。

---

## 3. 规范与性能收益

1. **RESTful 接口语义规范化**：
   - 区分了「系统错误/资源不存在」与「正常业务状态（用户尚未加入战队）」，杜绝使用 404 响应传递普通查询的空值状态；
2. **避免无效网络请求**：
   - 管理员在后台预览赛事时无需产生无意义的个人队伍状态探测请求；
3. **视觉层次更加聚焦**：
   - 竞赛编辑顶栏不再堆砌冗长说明与模式标签，视觉重点聚焦于赛事名称与状态。

---

## 4. 构建与验证结论

1. **后端工程编译验证**：
   - 运行 `mvn clean compile -DskipTests`：**BUILD SUCCESS**，12 个子模块全部一次性编译成功（耗时 11.536s）。
2. **前端工程构建验证**：
   - 运行 `npm run build`（`vue-tsc --noEmit && vite build`）：**0 错误、0 警告**，顺利构建打包（耗时 4.41s）。
