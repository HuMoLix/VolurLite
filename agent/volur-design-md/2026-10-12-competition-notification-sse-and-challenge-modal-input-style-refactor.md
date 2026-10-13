# CTF 竞赛实时解题播报 (VCompetitionNotification) 与挑战弹窗输入框样式对齐

## 1. 变更背景与需求目标
为提升 CTF 竞技场景下的沉浸感与动态对抗态势感知，需在竞赛大厅（`CtfCompetitionView.vue`）中引入实时解题通报能力：
1. **实时解题播报组件 (`VCompetitionNotification.vue`)**：
   - 布局位置：置于赛题/公告/排行榜/战队过滤标签组（`VFilterBtnGroup`）右侧；
   - 视觉样式：无背景颜色（透明），使用上边框和下边框（`border-t border-b`），与导航栏下边框视觉基调融合；
   - 实时长连接：参考 `volur-lite-view` 的 SSE 机制，在 `volur-lite-view-NEXT` 中构建单例长连接管理器 `SseClient`；
   - 业务态势响应：当选手提交 Flag 并夺得题目积分（尤其是首血、二血、三血）时，后端通过实时推送服务派发全站/全场广播事件，前端组件滚动展示战报。
2. **挑战弹窗输入框样式规范化 (`CtfCompetitionChallengeModal.vue`)**：
   - Flag 提交输入框的边框、背景及字体样式与侧边栏搜索组件（`VSearch.vue`）完全对齐；
   - 严格设置圆角 `radius` 为 `12px`；
   - 保持亮暗主题适配（亮色 `#dadce0` 边框、`#202124` 文字；暗色 `#3c4043` 边框、`#e3e3e3` 文字）。

---

## 2. 核心改动点与技术方案

### 2.1 前端 SSE 客户端封装 (`src/utils/sse.ts`)
- 参考经典前端架构中的 `SseClient` 实现，基于浏览器标准 `EventSource` 构建具备单例维护、凭证携带（`withCredentials: true`）及防重入能力的 SSE 客户端；
- 提供 `getGlobalSseClient` 统一工厂入口，支持多监听器注册解绑与窗口卸载（`beforeunload`）资源释放。

### 2.2 实时解题播报组件 (`VCompetitionNotification.vue`)
- **组件路径**：`src/views/competition/components/VCompetitionNotification.vue`，并在 `src/views/competition/components/index.ts` 集中导出；
- **视觉风格**：
  - 容器高度 `40px`，背景透明（`bg-transparent`），具备标准上下细线边框（`border-top: 1px solid #dadce0; border-bottom: 1px solid #dadce0`，暗色联动 `#3c4043`）；
  - 左侧常驻扬声器播报图标与竖向分割线；
  - 中部区域采用垂直平滑滚动过渡动画（`Transition name="ticker-slide"`），每 4 秒轮播一条解题战报；
  - 鼠标悬停暂停轮播，鼠标移开恢复轮播；
  - 针对一血（`FIRST_BLOOD`，高亮红）、二血（`SECOND_BLOOD`，沉稳灰）、三血（`THIRD_BLOOD`，琥珀金）以及普通解出（`NORMAL`，谷歌蓝）渲染专属视觉标签；
  - 展示战队名称、解题动作、题目分类、题目名称、获得分值以及提交时间戳。
- **无战报兜底态**：比赛尚未产生解题记录时，展示温和状态提示与绿色脉冲圆点，杜绝生硬留白。

### 2.3 竞赛大厅页面排版集成 (`CtfCompetitionView.vue`)
- 调整顶部操作栏布局结构：从单纯的换行容器重构为弹性响应式栏目（`flex flex-col lg:flex-row lg:items-center justify-between gap-3`）；
- `VFilterBtnGroup` 保持左侧收敛（`shrink-0`），`VCompetitionNotification` 占据右侧展示空间（`flex-1 min-w-0 lg:max-w-xl xl:max-w-2xl`），与当前赛事上下文 ID 绑定。

### 2.4 挑战弹窗输入框样式对齐 (`CtfCompetitionChallengeModal.vue`)
- 移除原行内单调的 `font-mono text-xs rounded-xl` 样式；
- 继承 `VSearch.vue` 输入框规范：
  - 亮色边框 `#dadce0`，聚焦为谷歌蓝 `#1a73e8`；暗色边框 `#3c4043`，聚焦为 `#a8c7fa`；
  - 字体继承全局体系（`font-family: inherit`），字号优化为 `text-sm sm:text-base`；
  - 占位符颜色对齐 `#80868b`（暗色 `#9aa0a6`）；
  - 圆角统一声明为 `12px`（`border-radius: 12px; rounded-[12px]`）。

### 2.5 后端解题事件广播 (`CtfCompetitionServiceImpl.java`)
- 引入 `RealtimePushService` 依赖注入；
- 新增领域视图实体 `CtfSolveNotificationVO`（收敛于 `com.humolix.volur.competition.domain.vo.ctf`）；
- 在选手提交 Flag 校验正确（`submitFlag`）并完成积分衰减计算和入库保存后，基于当前解出顺序（`newSolvesCount`）自动判定血统（1 -> `FIRST_BLOOD`, 2 -> `SECOND_BLOOD`, 3 -> `THIRD_BLOOD`, 其余 -> `NORMAL`）；
- 组装包含赛题、分类、战队、选手、分值与时间戳的完整报文，调用 `realtimePushService.broadcastAll("competition_event", notificationVO)` 触发全链路实时派发。

---

## 3. 安全考量与影响范围
- **连接安全与资源防泄漏**：`SseClient` 与组件生命周期严格绑定，在 `onBeforeUnmount` 与页面卸载时自动注销监听并释放 EventSource 套接字，杜绝内存与句柄泄漏；
- **上下文隔离防护**：`VCompetitionNotification` 内部对接收到的广播数据执行严格的 `competitionId` 匹配校验，防止跨赛事信息串扰；
- **全链路快速失败与单向依赖**：后端事件触发在核心事务内安全结算后进行，不阻断主解题逻辑；前端纯事件驱动，不包含任何伪造数据。
