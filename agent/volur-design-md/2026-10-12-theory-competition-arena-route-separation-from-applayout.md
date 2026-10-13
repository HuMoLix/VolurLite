# 理论赛考场路由独立提升与 AppLayout 侧边栏解耦方案

## 1. 问题背景与现象归因

### 1.1 缺陷现象
在访问理论知识赛考场路由 `/competitions/:id/theory`（如 `/competitions/test2/theory`）时，界面左侧异常出现了全站侧边栏导航组件 `SideBar.vue`（以及移动端底栏 `VBottomBar.vue`），并且整个竞技场工作台 `CompetitionLayout.vue` 被挤压在右侧内容区域，破坏了沉浸式考场的全宽全屏布局。

### 1.2 根因分析
在排查 `CompetitionLayout.vue` 时发现，该组件本身并未直接导入 `SideBar.vue`（仅导入了原子头像组件 `VAvatar.vue`）。真正的根因出在路由配置 `src/volur-lite-view-NEXT/src/router/routes/competition.ts`：
- `competitions/:id/theory` 路由此前被配置在根路径 `/` 对应的通用布局组件 `AppLayout.vue` 的 `children` 数组中；
- 当选手访问 `/competitions/test2/theory` 时，Vue Router 首先匹配父级路由并挂载 `AppLayout.vue`；
- `AppLayout.vue` 默认在其左侧强制实例化并渲染全站侧边栏 `SideBar.vue`；
- `AppLayout.vue` 主视口插槽内加载了 `TheoryCompetitionView.vue`，而 `TheoryCompetitionView.vue` 内部已自带独立的全局考场布局 `CompetitionLayout.vue`；
- 由此引发了典型的“父子双重 Layout 嵌套与样式冲突”问题，导致全局侧边栏侵入考场工作台。

---

## 2. 重构与修复方案

### 2.1 路由层级扁平化与顶级提升
在 `src/volur-lite-view-NEXT/src/router/routes/competition.ts` 中：
1. 将 `competitions/:id/theory` 从 `AppLayout.vue` 的 `children` 子路由中剥离；
2. 将其提升为与 `AppLayout.vue` 平级的顶级独立路由规则（Top-level Route）；
3. 同步补齐短链考场顶级路由 `/c/:id/theory`，确保直达链接与标准路径一致脱离 `AppLayout`。

### 2.2 修复后路由拓扑
- **全站浏览与详情**（使用 `AppLayout` + `SideBar`）：
  - `/competitions`：赛事大厅
  - `/competitions/:id`：赛事详情
  - `/c/:id`：赛事详情短链
- **独立限时考核工作台**（脱离 `AppLayout`，直接由 `CompetitionLayout` 接管）：
  - `/competitions/:id/theory`：理论知识赛考场
  - `/c/:id/theory`：理论知识赛考场短链

---

## 3. 架构规范检验与构建验证
1. **纯粹代码与零注释约束**：`src/volur-lite-view-NEXT/src/router/routes/competition.ts` 中保持纯净代码，所有设计背景外置至本方案文档；
2. **构建检验**：执行 `npm --prefix src/volur-lite-view-NEXT run build`（`vue-tsc --noEmit && vite build`）通过，无任何类型错误与打包异常；
3. **视觉与交互保障**：考场视图不再受 `SideBar` 与移动端 `VBottomBar` 遮挡干扰，完全由 `CompetitionLayout.vue` 提供 100% 全宽沉浸式作答界面。
