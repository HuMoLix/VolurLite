# CTF 赛题详情模态框 Header、Tabs 与关闭按钮重构施工记录

## 1. 变更背景与需求概述
在 CTF 竞技场答题交互中，原赛题详情模态框（`CtfCompetitionChallengeModal.vue`）存在以下待改进点：
1. 模态框关闭按钮边角半径默认为小圆角，需要统一修改为 `9999px` 纯圆形按钮；
2. 题目类型标签与难度标签原先平铺于模态框内容区顶部胶囊栏中，占用主体垂直空间；需将其移至弹窗标题右侧后方，且题目类型显示参考 `CtfCompetitionChallengeCard.vue` 调整为「Icon + 文字」无胶囊底色设计，难度标签置于题目类型后；
3. 模态框内需要提供 Tabs 切换交互：
   - 第一个 Tab（赛题内容）：包含赛题描述、附件下载、动态靶机环境调度与 Flag 提交；
   - 第二个 Tab（已解出战队）：展示攻克该赛题的战队列表，采用 Flex 布局排列，包含队伍头像、队伍名称、解出时间与获得分数；前三名分别采用与 `CtfCompetitionChallengeCard.vue` 相同的一血（金牌）、二血（银牌）、三血（铜牌）Medal 图标替代普通数字序号。

## 2. 核心改动点与实现方案

### 2.1 关闭按钮样式圆角改造
- 在 `n-modal` 上启用 `closable` 属性；
- 在组件 scoped 样式中深度穿透定制 Naive UI 关闭按钮选择器：
  ```scss
  :deep(.n-card-header__close .n-base-close),
  :deep(.n-base-close) {
    border-radius: 9999px !important;
  }
  ```
  使关闭按钮及其 Hover 态背景呈现 `9999px` 完美圆形。

### 2.2 模态框 Header 结构优化与类型标签改造
- 移除模态框内容区内原有的冗余类型胶囊顶栏；
- 在 `n-modal` 的 `#header` 插槽中重组标题区：
  - 赛题标题：高对比自适应截断；
  - 题目类型：采用 `getCategoryIcon` 与 `getCategoryColorClass` 渲染「Icon + 类别名称」文本，移除胶囊外框与底色；
  - 难度标签：跟随在题目类型之后，以轻量字号与等宽字体展示难度等级；
- 在 `#header-extra` 插槽中展示题目当前分值（`currentPoints`）与解题数（`solvesCount`）。

### 2.3 双 Tab 交互与解题战队列表 (Flex 布局)
- 在模态框内部引入轻量 Google Material 风格的 Underline Tabs（赛题内容 vs 已解出战队），支持队伍解出数量徽章；
- **Tab 1（赛题内容）**：收敛赛题描述正文、离线附件下载链接、独立靶机容器管理控制台（靶机拉起、倒计时、续期、销毁）与 Flag 提交验证表单；
- **Tab 2（已解出战队）**：
  - 单项列表项采用 `flex items-center justify-between` 布局；
  - 左侧：序号/Medal 图标 + 队伍圆形头像（`VAvatar`） + 队伍名称；
  - 右侧：解题时间（`TimeOutline` 图标 + 格式化时间） + 获得积分（`+pts`）；
  - 序号判定：一血、二血、三血分别渲染 `@vicons/fa` 的 `Medal` 组件，颜色分别为金牌（`text-amber-500`）、银牌（`text-slate-400`）与铜牌（`text-orange-500`），第四名及以后显示等宽数字序号；
  - 无队伍解出时展示「暂无队伍解出 · 等待一血」空状态。

### 2.4 类型与上下文串联
- 在 `src/types/competition/index.ts` 中扩展 `CtfChallengeSolveVO` 接口，并为 `CtfCompetitionChallengeDetailVO` 补充 `topTeams` 与 `solves` 属性；
- 在 `CtfCompetitionChallengeListView.vue` 中传递 `scoreboard` 并同步赛题卡片的 `topTeams` 数据，确保解题战队信息在前端能无缝映射。

## 3. 规范遵循与代码纯净性
- 严格遵循 `view-NEXT-rules.md`：采用 Google Blue 主色系、Google Gray 次级色阶与无刺眼全黑原则；
- 遵循纯粹代码与零注释铁律：代码文件不添加冗余行内解释性注释，全部设计与改动均在此文档中完备沉淀；
- 构建验证：通过 `vue-tsc --noEmit && vite build`，零类型报错与构建警告。

## 4. 影响范围
- `src/views/competition/components/CtfCompetitionChallengeModal.vue`
- `src/types/competition/index.ts`
- `src/views/competition/tabs/CtfCompetitionChallengeListView.vue`
