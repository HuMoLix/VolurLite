# 理论考试强反作弊升级部署与监考安全体系设计文档

- **记录日期**：2026-10-13
- **涉及模块**：
  - `volur-competition`（后端竞赛业务模块）
  - `volur-lite-view-NEXT`（新一代前端工程）
- **变更文件清单**：
  - `volur-competition/.../TheoryCheatReportDTO.java`（新增）
  - `volur-competition/.../TheoryCheatReportVO.java`（新增）
  - `volur-competition/.../TheoryCompetitionService.java`（接口升级）
  - `volur-competition/.../TheoryCompetitionServiceImpl.java`（实现更新）
  - `volur-competition/.../TheoryCompetitionController.java`（接口暴露）
  - `src/types/competition/index.ts`（前端类型拓展）
  - `src/api/competition.ts`（前端 API 拓展）
  - `src/views/competition/TheoryCompetitionView.vue`（防作弊视图深度重构）

---

## 1. 架构总览与五大核心命题分析

针对理论考试（Theory Competition）的严肃考评场景，本方案构建了覆盖“环境准入、运行时探针、后端熔断处置、反脚本对抗”四重维度的纵深防御（Defense-in-Depth）闭环。

### 1.1 强反作弊体系设计（System Architecture）
理论考试的核心威胁模型分为三步：
1. **题干外泄**（复制、审查元素、DOM 自动化提取、截屏）；
2. **作弊搜题**（Alt+Tab 切屏、多屏幕扩展、浏览器多标签页、聊天软件/AI 工具搜题）；
3. **恶意作答**（油猴脚本自动填充、重放请求批量秒答）。

为此，方案建立了准入守卫（Gatekeeper）、行为探针（Probes）、审计管道（Pipeline）与熔断决策（Governor）全链路。

### 1.2 浏览器原生 API 屏幕监控可行性与技术实现
- **Screen Capture API (`navigator.mediaDevices.getDisplayMedia`)**：
  - **可行性**：现代浏览器完全支持原生桌面共享流拉取。通过指定 `displaySurface: 'monitor'` 引导用户共享整块显示器屏幕；
  - **断流侦测**：监听 `MediaStreamTrack.onended` 事件，当考生试图通过浏览器悬浮条点击“停止共享”时，立即触发高危违规上报并冻结答题；
  - **限制与真实边界**：Web 环境受同源安全策略与隐私约束，必须在用户显式手势（User Gesture）下申请授权，无法隐式无感知静默录制；且无法感知物理机位（手机拍照、外置物理采集卡等），终极方案需结合双机位（PC 原生屏幕录制 + 移动端广角副摄像头）。
- **Fullscreen API**：
  - 请求 `document.documentElement.requestFullscreen()` 全屏沉浸式答题，监听 `fullscreenchange` 事件，脱离全屏立即记为违规。
- **Page Visibility & Window Blur API**：
  - 结合 `visibilitychange`（标签页切换/最小化）与 `window.blur`（焦点丢失/Alt+Tab/双屏移出），毫秒级捕获切屏动作。

### 1.3 危险操作捕获、前端告警与后端数据同步
- **违规行为归一化**：定义 `WINDOW_BLUR`、`VISIBILITY_HIDDEN`、`EXIT_FULLSCREEN`、`STOP_SCREEN_SHARE`、`DEVTOOLS_OPEN`、`RESTRICTED_KEY`、`COPY_PASTE_ATTEMPT` 等事件；
- **防抖上报机制**：设置 2000ms 防抖上报窗口，防止短时间内切屏或按键风暴打崩后端接口；
- **交互阻断告警**：前端弹出高危警示模态弹窗，展示当前违规次数与系统上限，阻断作答并要求考生手动确认并重入全屏。

### 1.4 后端阈值管控、Redis 原子累积与自动封禁策略
- **原子计数**：利用 Redis `vredis:competition:theory:cheat:{competitionId}:{userId}` 执行原子自增 `INCR`，设置 24 小时 TTL，天然并发安全；
- **阈值熔断**：默认阈值设定为 3 次。当计数达到阈值时：
  1. 调用 `UserRepository` 将 `user.setStatus(false)` 执行全平台账号冻结封禁；
  2. 持久化记录 `CompetitionOperationLog`（`logType = "THEORY_CHEAT"`），标记状态为违规异常并记录违规链条；
  3. 返回 `TheoryCheatReportVO(isBanned = true)`，前端立即销毁草稿、全屏置灰锁定并禁用所有答题控件。

### 1.5 防御油猴脚本（Tampermonkey）、禁用 JS 等绕过手段
- **防御“禁用 JS”**：
  - 考场属于 Vue 3 SPA 应用，题目数据由动态鉴权 API 获取，禁用 JS 连试题都无法加载；结合原生 `<noscript>` 防御，无脚本环境下彻底拒绝访问。
- **防御油猴脚本原型链篡改（Prototype Hooking）**：
  - 在页面加载最前置阶段保存原生函数快照（`addEventListener`、`getDisplayMedia` 等），并对关键原生原型属性进行深层保护，破坏油猴脚本对 `document.hidden` 或切屏事件的重写。
- **独立 Web Worker 双向加密心跳**：
  - 油猴脚本受限于 Window 主执行上下文，难以篡改独立的 Web Worker 沙箱环境。在 Worker 内运行高精度计时器与随机挑战，超时或主线程篡改即判违规。
- **服务端作答时序行为学分析（Zero-Trust on Server）**：
  - 坚持“客户端不可信”铁律，所有交卷行为必须经过服务端作答耗时、选项点击时间方差分析；如在十几秒内完成大量复杂题目，服务端直接判伪作废。

---

## 2. 代码实现与改造详情

### 2.1 后端工程改造
1. **DTO & VO 声明**：
   - `TheoryCheatReportDTO`：包含 `eventType`、`detail`、`timestamp`，满足 Jakarta Validation 约束。
   - `TheoryCheatReportVO`：返回 `violationCount`、`maxAllowedViolations`、`isBanned`、`message`。
2. **服务层（Service）**：
   - 增加 `reportCheatEvent` 方法，结合 `StringRedisTemplate` 进行原子计数与超时管理；
   - 阈值触发时级联更新 `User.status = false` 并向 `CompetitionOperationLog` 写入审计日志。
3. **控制器（Controller）**：
   - 在 `TheoryCompetitionController` 暴露 `@PostMapping("/cheat-event")`，遵循 Thin Controller 规范返回 `ApiResponseEntity<TheoryCheatReportVO>`。

### 2.2 前端工程改造
1. **类型与 API 对齐**：
   - 在 `types/competition/index.ts` 补充 `TheoryCheatReportDTO`、`TheoryCheatReportVO`、`TheoryCheatEventType`；
   - 在 `api/competition.ts` 导出 `reportTheoryCheatEventApi`。
2. **考场准入守卫（Proctored Gate）**：
   - 未开始答题前展示“理论考试严格反作弊准入”说明卡，要求点击“开启监考并开始作答”；
   - 用户授权全屏与屏幕共享监控流后方可进入试题作答界面。
3. **全局高危事件监听与拦截**：
   - 绑定 `visibilitychange`、`blur`、`fullscreenchange`、`copy`、`paste`、`cut`、`contextmenu` 及受限快捷键（F12、Ctrl+Shift+I/J/C、Ctrl+U/S/P）；
   - 监听屏幕共享流的 `track.onended` 事件。
4. **实时告警弹窗与封禁锁定屏**：
   - 违规事件弹出不可穿透的警告模态框，告知违规累计情况；
   - 达到阈值后切入全屏锁死卡片（Banned Lockout Screen），销毁本地草稿，杜绝继续答题。
