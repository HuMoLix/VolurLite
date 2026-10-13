# CTF 赛题卡片边框色彩规范与底部高度不对齐根因分析及对齐方案

## 1. 变更背景与架构目标

在 Volur Lite NEXT（`src/volur-lite-view-NEXT`）前端工程中，CTF 赛题卡片 `CtfCompetitionChallengeCard.vue` 存在以下视觉与排版问题：

1. **边框色值规范对齐**：
   - 默认边框需调整为更加轻盈内敛的 `#d7d8da80`（50% 半透明 Google 灰）；
   - 鼠标 Hover 悬停时的边框高亮色调整为 `#d7d8da`（100% 不透明 Google 灰），消除原先蓝色过于抢眼的问题；
   - 底部分割线（`.challenge-card-footer` 的 `border-top`）同步对齐为 `#d7d8da80`。
2. **两张卡片底部高度不一致导致 `border-top` 无法水平对齐问题**：
   - 当两个卡片在同一网格行中并排展示时，其中一个卡片已被队伍攻克（有解出战队 Top 3），另一个卡片尚未被攻克（显示“暂无队伍解出”）；
   - 选手会明显发现两个卡片下方的 `border-top` 分割线**上下错位、不在同一水平线上**，严重破坏了网格排版的工整感。

---

## 2. 为什么有解出队伍与没有解出队伍时高度不同导致 `border-top` 不对齐？（深度根因分析）

经对 CSS 盒子模型、Flex 弹性布局与 CSS Grid 排版机制的全链路深入审计，导致分割线错位的根本原因包含以下三点：

### 2.1 内容项内禀高度不同 (Intrinsic Height Mismatch)
- **有解题队伍分支 (`v-if="topTeamsList.length > 0"`)**：
  - 内容由勋章图标 `<n-icon :size="14" :component="Medal">` 与截断文字 `<span ... text-[14px] leading-[14px]>` 构成；
  - Naive UI 的 `NIcon` 渲染为 `display: inline-flex` 的 SVG 容器，加上字体基线与 FontAwesome 图标字形计算，在浏览器中的**实际渲染行高在 `18px ~ 20px` 之间**；
  - 该分支此前**没有设置任何固定高度约束**，随内容自适应撑开（约 `20px`）。
- **未解题分支 (`v-else`)**：
  - 此前该分支直接写死了 Tailwind 类名 **`h-[14px]`**；
  - 导致未解题分支的实际高度被强制卡死在 **`14px`**。
- **直接结果**：
  两分支的内容高度差达到 `20px - 14px = 6px`。

### 2.2 底部容器自适应包裹累加高度差 (Unconstrained Footer Wrapper)
- 底部的 `.challenge-card-footer` 容器此前仅设置了 `pt-3`（即 `padding-top: 12px;`）和 `border-top: 1px solid`，**未声明固定的总高度**；
- 因此，`.challenge-card-footer` 的总外高等于：`1px (border-top) + 12px (padding-top) + 内容高度`：
  - **有解题队伍的卡片**：总高 = `1px + 12px + 20px = 33px`；
  - **无解题队伍的卡片**：总高 = `1px + 12px + 14px = 27px`。

### 2.3 Flex 底部贴靠使得高度差直接转化为 `border-top` 的 Y 轴偏移
- 卡片主体是 `flex flex-col flex-1 justify-between` 容器；
- 中间的题目标题容器配置了 `my-auto`，在 Flexbox 中 `margin-top: auto; margin-bottom: auto` 会吸收垂直方向上的所有剩余自由空间，并将底部的 `.challenge-card-footer` **强制贴紧在卡片最底端**；
- 在 CSS Grid 中，同一行的卡片外容器总高完全一致（`align-items: stretch`）；
- 贴底容器的顶部边界距离卡片底部的距离恰好等于其自身总高度：
  - **卡片 A（有解出）**：`border-top` 距离底部为 `33px`；
  - **卡片 B（无解出）**：`border-top` 距离底部为 `27px`；
- **最终现象**：**卡片 A 的分割线比卡片 B 整整高出 6 个像素，肉眼可见地出现上下跳阶与对不齐！**

---

## 3. 修复方案与技术落地

### 3.1 锁定底部 Footer 固定尺寸 (`CtfCompetitionChallengeCard.vue`)
为了彻底消除不同内容导致的任何微小高度差，必须从容器层面做**绝对高度恒定化**设计：

1. **固定 Footer 标称总高度**：
   在 `.challenge-card-footer` 中设置严格固定的盒模型：
   ```scss
   .challenge-card-footer {
     border-top: 1px solid #d7d8da80;
     height: 34px;
     box-sizing: border-box;
   }
   ```
2. **清除内部死高度，统一弹性垂直居中**：
   - 移除 `v-else` 上的 `h-[14px]` 死高度类名；
   - 顶部内边距微调为 `pt-2.5`（`10px`），使得内容区拥有 `34px - 10px - 1px = 23px` 的舒适排版空间；
   - 两个分支均在统一的 `34px` 高度空间中自适应垂直对齐。
3. **实现效果**：
   所有赛题卡片的底部区域总高度严格、绝对恒定为 `34px`，`border-top` 距离卡片底部的距离永远等于 `34px`，在同一 Grid 行中**达到 100% 像素级对齐**。

### 3.2 边框色值规范升级 (`CtfCompetitionChallengeCard.vue`)
- **常规态**：`border: 1px solid #d7d8da80;`（暗色 `rgba(60, 64, 67, 0.6)`）；
- **悬停态 (Hover)**：`border-color: #d7d8da;`（暗色 `#3c4043`）；
- **底部分割线**：`border-top: 1px solid #d7d8da80;`（暗色 `rgba(60, 64, 67, 0.6)`）。

---

## 4. 影响范围与验证结果

- **影响文件**：
  - `src/volur-lite-view-NEXT/src/views/competition/components/CtfCompetitionChallengeCard.vue`
- **验证结果**：
  - 执行 `npm run build`（Vite + vue-tsc）：成功编译打包（`built in 16.04s`），样式成功注入，零 TypeScript 报错。
