# VTooltip 全局组件复用与 CTFCompetitionView 交互提示改造方案

## 1. 变更背景与目标
在 Volur Lite NEXT 前端工程中，此前已提取了符合 Google Workspace / Material Design 极简设计标准的全局悬浮提示组件 `VTooltip.vue`（具备无尖角气泡、8px 圆角、细微边框微阴影、双模式插槽与 Popover 深度穿透委派能力）。
为了统一平台交互体验，消除 `CtfCompetitionView` 中原生的 HTML `:title` 属性（解决浏览器原生黄色提示框延迟高、样式生硬、无法自适应暗黑模式的问题），并将关键交互节点全面赋能 `VTooltip`，本次改造在 `CtfCompetitionView` 全量复用了 `VTooltip`。

## 2. 改造范围与落地细节

### 2.1 统计数据看板 (Overview KPI Cards)
- **总赛题数**：包裹 `VTooltip`，提示“竞技场全部赛题总量”；
- **已解出**：包裹 `VTooltip`，提示“战队成员累计攻克成功的赛题数”；
- **战队总分**：包裹 `VTooltip`，提示“根据赛题解题数累计计算的战队总积分”；
- **当前排名**：包裹 `VTooltip`，提示“实时解题天梯榜当前位次”。

### 2.2 赛题卡片与列表交互 (Challenge Cards)
- **动态靶机标识**：卡片顶部的动态容器小火箭图标包裹 `VTooltip`，提示“动态靶机环境”；
- **已攻克徽章**：绿色的 `已解出` 徽章包裹 `VTooltip`，提示“您的战队已攻克此题并夺旗获得积分”；
- **赛题长标题**：标题文本由于开启了 `line-clamp-2` 截断，使用 `VTooltip` 并配置 `placement="top"` 展示完整题目标题。

### 2.3 天梯排行榜 (Real-time Scoreboard)
- **冠亚季军奖牌**：
  - 第 1 名金牌徽章包裹 `VTooltip`，提示“冠军 (第 1 名)”；
  - 第 2 名银牌徽章包裹 `VTooltip`，提示“亚军 (第 2 名)”；
  - 第 3 名铜牌徽章包裹 `VTooltip`，提示“季军 (第 3 名)”；
- **本队高亮标识**：天梯榜上己方战队的“本队”标签包裹 `VTooltip`，提示“当前登录账号所在战队”。

### 2.4 我的战队与已解记录 (My Team & Solves)
- **队长权限标识**：战队成员列表中的“队长”身份徽章包裹 `VTooltip`，提示“战队队长，具备管理成员权限”；
- **已解赛题记录**：底部战队已解出赛题胶囊标签包裹 `VTooltip`，悬浮时展示题目分类、完整标题与得分增量（如 `${ch.category} · ${ch.title} (+${ch.currentPoints} pts)`）。

### 2.5 赛题详情与动态靶机弹窗 (Challenge Modal & Target Actions)
- **附件离线下载**：下载按钮包裹 `VTooltip`，提示“点击下载离线赛题附件包”；
- **靶标连接复制**：彻底废除原生的 `:title="t('common.copy', '复制')"`，包裹 `VTooltip` 提供无感的复制操作提示；
- **靶机生命周期动作**：
  - “续期 +15m”按钮包裹 `VTooltip`，提示“延长靶机运行时间 15 分钟”；
  - “销毁靶机”按钮包裹 `VTooltip`，提示“立即停止并释放当前靶机容器实例”。

## 3. 规范与质量验证
- **代码零注释原则**：所有 `.vue` 与组件代码均无行内解释性注释，设计思路与逻辑完整沉淀于本外置文档；
- **暗黑模式与色彩规范**：亮色模式采用 Google Gray 文本与微阴影，暗色模式采用 Google Workspace 深色底色（`#202124` / `#292a2d` / `#3c4043`），对比度清晰；
- **工程构建与类型安全**：通过 `npm run build`（Vite + vue-tsc），零类型错误，生产打包通过。
