# CtfCompetitionChallengeModal 已解出战队列表表头与网格对齐重构设计文档

## 1. 变更背景与诉求分析
在 CTF 竞赛演练大厅中，选手打开题目弹窗（`CtfCompetitionChallengeModal.vue`）切换至「已解出战队」（`activeTab === 'solves'`）面板时，原实现存在以下问题：
1. **表头元信息缺失**：列表直接平铺展示各解题队伍卡片，缺乏顶部统一表头指示，用户无法即时明确各列数据的具体业务属性；
2. **两端弹性布局导致纵向错位**：原代码在行内采用 `flex items-center justify-between` 左右双容器分割，左侧队伍名称长短不一时会引起宽度伸缩，进而导致右侧的「提交时间」和「获得分数」列在不同数据行之间无法垂直对齐，产生明显的水平抖动与错位；
3. **移动端响应式未联动**：原实现仅在行内对时间使用了 `hidden sm:inline-block`，若无对应表头协同联动，小屏幕（`< 640px`）体验不完整；
4. **多语言与国际化支持**：需要对表头依次展示的「序号」、「队伍」、「提交时间」、「获得分数」四列配置标准的多语言词条回退。

---

## 2. 方案设计与技术选型

### 2.1 CSS Grid 网格对齐模型
为彻底解决列表列对齐漂移问题，重构后放弃不可控的松散 Flex 容器布局，采用统一的 CSS Grid 栅格体系，使表头行与各数据项共享完全相同的列模板：
- **桌面端（`sm:` 及以上）**：`grid-cols-[48px_1fr_160px_84px]`
  - **第 1 列（序号）**：固定 `48px`，水平居中对齐，支持展示前三名一血/二血/三血 Medal 勋章图标及等宽数字排名；
  - **第 2 列（队伍）**：弹性自适应 `1fr`，左对齐，容纳战队头像 `VAvatar`（`30px`）与超长截断战队名称 `truncate`；
  - **第 3 列（提交时间）**：固定 `160px`，等宽字体 `font-mono`，展示标准解题时间戳；
  - **第 4 列（获得分数）**：固定 `84px`，右对齐，高亮等宽数字 `font-mono font-bold text-google-blue dark:text-google-blue-dark`。
- **移动端（`< 640px` 窄屏）**：`grid-cols-[48px_1fr_80px]`
  - 自动通过 `hidden sm:inline-block` / `hidden sm:block` 将「提交时间」列在表头和数据项中同步折叠，保证移动端布局精致紧凑不折行。

### 2.2 视觉与设计规范（Google Material / Workspace）
严格遵循 `view-NEXT-rules.md`：
- 表头文字采用 Google Gray 弱化次级文本（`text-[11px] font-semibold uppercase text-google-text-muted dark:text-google-text-darkMuted`）；
- 表头底部配备轻量分割线 `border-b border-google-border-light/70 dark:border-google-border-dark/70`；
- 数据行保留 Google 风格精致微悬浮圆角卡片（`rounded-2xl bg-google-surface-light/60 dark:bg-google-surface-dark`），悬停高亮边框；
- 杜绝纯黑，暗色模式完美适配深曜灰黑表面。

### 2.3 国际化词条统一管理
在 `zh-CN/competition.json`、`en-US/competition.json`、`zh-TW/competition.json` 中统一注入：
- `solve_index`: "序号" / "No." / "序號"
- `solve_team`: "队伍" / "Team" / "隊伍"
- `solve_time`: "提交时间" / "Solve Time" / "提交時間"
- `solve_points`: "获得分数" / "Points" / "獲得分數"

---

## 3. 改动影响范围
1. `src/views/competition/components/CtfCompetitionChallengeModal.vue`：已解出战队列表结构与样式；
2. `src/locales/zh-CN/competition.json`：新增 4 项表头国际化词条；
3. `src/locales/en-US/competition.json`：新增 4 项表头国际化词条；
4. `src/locales/zh-TW/competition.json`：新增 4 项表头国际化词条。
