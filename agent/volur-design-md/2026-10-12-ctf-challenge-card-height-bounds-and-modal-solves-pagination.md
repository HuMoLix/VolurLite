# CTF 竞技场赛题卡片高度边界约束与模态框已解出战队分页重构方案

## 1. 变更背景与架构目标

在 Volur Lite NEXT（`src/volur-lite-view-NEXT`）前端的 CTF 竞技场与赛题交互中，存在以下两项需要规范化优化的交互体验与排版问题：

1. **赛题卡片高度不规则伸缩**：原赛题卡片（`CtfCompetitionChallengeCard.vue`）仅声明了 `min-h-[140px]`，未设置 `max-height` 边界。当卡片题目标题为多行文本或分类名称较长时，各行卡片纵向高度弹性拉伸过大，影响网格规整度；需通过设定科学的最小和最大高度范围（`min-height: 160px; max-height: 180px;`）并配合 Flex 与截断限制，确保卡片在整齐美观的前提下自适应不同文本长度。
2. **已解出战队列表无限纵向滚动与溢出隐患**：在赛题详情模态框（`CtfCompetitionChallengeModal.vue`）中，当赛题被较多战队解出（如数十支队伍）时，「已解出战队」Tab 仅依靠最大滚动高度（`max-h-[420px]`）承载全部战队条目，不仅缺乏分页可查性，还会在多端设备或不同分辨率弹窗中造成纵向撑开、溢出以及滚动体验不佳的问题。

---

## 2. 核心改动点与技术实现

### 2.1 赛题卡片高度边界与内部自适应 (`CtfCompetitionChallengeCard.vue`)

1. **设定最小与最大高度约束**：
   - 在模板外层容器声明 `min-h-[160px] max-h-[180px]` 类名；
   - 在 Scoped SCSS 的 `.ctf-challenge-card` 类选择器中同步声明 `min-height: 160px; max-height: 180px;`，保障在 Tailwind Preflight 隔离环境下样式精准生效。
2. **内部 Flex 盒模型收敛防溢出**：
   - 将卡片主体内容容器设置 `min-h-0`；
   - 顶部题目分类/分值网格设置 `shrink-0`；
   - 中间题目标题区域设置 `min-h-0 overflow-hidden flex items-center justify-center`，结合 `line-clamp-2` 防止多行文字溢出破坏卡片边界；
   - 底部分割线与前三血队伍容器（`.challenge-card-footer`）设置 `shrink-0`，保持 34px 固定高度。

### 2.2 模态框已解出战队分页机制引入 (`CtfCompetitionChallengeModal.vue`)

1. **分页状态与切片计算**：
   - 引入 Naive UI 官方分页组件 `NPagination`；
   - 声明单页容量常量 `const SOLVES_PAGE_SIZE = 5` 与当前页响应式变量 `const solvesCurrentPage = ref<number>(1)`；
   - 新增计算属性 `pagedSolvedTeamsList`，基于 `(solvesCurrentPage - 1) * SOLVES_PAGE_SIZE` 对全部解题队伍 `solvedTeamsList` 进行纯函数切片。
2. **首血勋章与全域排名动态索引映射**：
   - 动态计算队伍在全量解题列表中的真实位次 `(solvesCurrentPage - 1) * SOLVES_PAGE_SIZE + index`；
   - 保证仅当真实位次为 0、1、2（即全场前三血）时展示金色、银色、铜色奖章，其余翻页位次按 `realIndex + 1` 真实展示名次编号。
3. **弹窗生命周期重置联动**：
   - 监听 `props.show` 打开动作与 `props.challenge.id` 切换动作，自动重置 `solvesCurrentPage.value = 1`，保证每次打开弹窗或切换题目时均从第一页开始展示；
   - 当 `solvedTeamsList.length > SOLVES_PAGE_SIZE` 时，渲染小型居中分页器 `<n-pagination size="small" />`。

### 2.3 模态框固定高度 540px 约束与无抖动布局 (`CtfCompetitionChallengeModal.vue`)

1. **绝对等高约束与 Naive UI 内容区（.n-card-content）穿透**：
   - 经源码剖析，Naive UI `NCard` 实际渲染的内容类名为 `.n-card-content`（单连字符，非 BEM 双下划线 `__`）；
   - 在模板 `<n-modal>` 上直接配置 `content-class="flex-1 min-h-0 flex flex-col"` 与 `content-style="flex: 1 1 0%; min-height: 0; display: flex; flex-direction: column;"`，直接将样式直通注入卡片内部内容节点；
   - 在 Scoped SCSS 中同步兼容补充 `:global(.challenge-detail-modal .n-card-content), :global(.challenge-detail-modal .n-card__content), :deep(.n-card-content), :deep(.n-card__content)`，彻底打通整个弹性盒纵向链路；
2. **Flag 提交框常驻弹窗最底部**：
   - 题目内容 Tab（`content`）重构为 Flex 上下分离架构：
     - **上层可滚动区域**：`flex-1 min-h-0 overflow-y-auto pr-1`，承载赛题描述、附件下载与动态 Docker 靶机操作，内容过长时内部平滑滚动；
     - **下层常驻底部提交区**：`shrink-0 pt-3 border-t mt-auto`，配合 `mt-auto` 与内部容器 `h-full`，确保无论赛题描述有多短，Flag 输入框与提交按钮均牢牢锚定停靠在 540px 弹窗的最底端，彻底消除下方大面积未撑满的留白；
   - 已解出战队 Tab（`solves`）采用 `justify-between`，5 条战队条目与底部分页器各司其职，两 Tab 切换时弹窗高度 100% 绝对一致，消除视觉抖动。

---

## 3. 安全隐患防范与规范审查

1. **纯粹代码与零注释铁律**：修改后的 Vue 组件代码中严禁写入任何行内解释性注释或多余备注，所有方案及推导收敛在本文档中；
2. **快速失败与防溢出保证**：弹窗高度收敛在合理视口范围内，杜绝无节制长列表渲染；切片基于纯响应式数据计算，不会产生越界异常。

---

## 4. 影响范围与验证结果

- **影响文件**：
  - `src/volur-lite-view-NEXT/src/views/competition/components/CtfCompetitionChallengeCard.vue`
  - `src/volur-lite-view-NEXT/src/views/competition/components/CtfCompetitionChallengeModal.vue`
- **构建验证**：
  - 执行 `npm run build`：Vite 生产环境构建全部通过（0 errors，耗时 11.96s，生产产物打包完成）。
