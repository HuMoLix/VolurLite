# 竞赛布局模块化收敛与历史遗留 Layout 清理审计说明

## 1. 变更背景与架构分析

在前序前端框架演进与规范重构中，平台自研前端组件库规范（[`view-Rules.md`](file:///Users/humolix/Documents/Dev/Volur-Lite/agent/volur-design-skills/view-Rules.md) 与 [`GEMINI.md`](file:///Users/humolix/Documents/Dev/Volur-Lite/GEMINI.md)）明确指出：
- 平台自主研发的组件库命名为 **`volurEx`**，物理路径为 `src/volur-lite-view/src/public/volurEx`（别名 `@/public/volurEx`）；
- 全平台页面骨架与框架布局统一收敛在 `@/public/volurEx/layouts/` 下；
- 业务视图与路由统一从 `@/public/volurEx` 引入。

对原项目中的两个布局目录进行对比与调用链审计：
1. **`src/volur-lite-view/src/public/volurEx/layouts`**：真正投入使用的核心实现目录，包含 `AdminLayout.vue`、`AppLayout.vue`、`AuthLayout.vue`、`CompetitionLayout.vue` 完整视图实现及 `index.ts` 聚合导出；
2. **`src/volur-lite-view/src/layout`**：历史迁移留下的空壳转发层（每个文件仅 8 行，单纯 `import` 转发自 `@/public/volurEx`）。全局检索确认没有任何业务组件或路由引用 `@/layout`。

同时，随着平台"一赛一制"架构向多模式（CTF 夺旗解题赛、THEORY 理论知识赛等）纵深演进，单一的 `CompetitionLayout` 无法精准表征解题竞技场与理论答题考场的差异化交互诉求。

---

## 2. 施工改动与重构细节

### 2.1 彻底清理无用转发目录 (`src/layout`)
- 物理删除 `src/volur-lite-view/src/layout/` 目录及其下 4 个冗余桥接文件：
  - `AdminLayout.vue`
  - `AppLayout.vue`
  - `AuthLayout.vue`
  - `CompetitionLayout.vue`
- 消除目录双轨制歧义，统一以 `src/public/volurEx/layouts` 为全平台唯一的布局真实数据源。

### 2.2 建立赛事专属布局子目录 (`volurEx/layouts/competitions/`)
在 `src/volur-lite-view/src/public/volurEx/layouts/` 下创建 `competitions/` 目录，用于针对不同赛制类型沉淀专属沉浸式布局：

1. **调整 `CompetitionLayout` 为 `CtfCompetitionLayout`**：
   - 物理路径：[`CtfCompetitionLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/competitions/CtfCompetitionLayout.vue)；
   - 命名空间与语义对齐：根样式类升级为 `.competition-arena-layout.ctf-competition-layout`；
   - **移除冗余赛制徽章**：去除标题栏旁显式挂载的模式标签，使顶部左侧 Brand 区域保持极简大气的标题呈现；
   - **高精度动态字号自适应与极端截断机制**：
     - 当可用宽度充足时，标题使用标准字号（16px）完整呈现，无省略号；
     - 当可用宽度受限时，优先通过 Canvas 精确测宽动态平滑缩小字号（16px 逐步微调收缩至 11px），确保文字尽量完整展开；
     - **当且仅当字号缩小至最小极限（11px）仍无法容纳标题时（“无法再缩小的时候”）**，才由 CSS 的 `overflow: hidden; text-overflow: ellipsis; white-space: nowrap;` 触发 `...` 省略截断，配合 `:title` 原生 Tooltip 满足长文本悬浮查验。
   - 维持在途 Promise 合并机制（`fetchCompetitionBySlug` 与 `fetchMyTeam`）及全功能顶部状态栏（倒计时警示、战队徽章、多语言、夜间模式切换与个人中心下拉）。

2. **新建理论考场专属布局 (`TheoryCompetitionLayout`)**：
   - 物理路径：[`TheoryCompetitionLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/competitions/TheoryCompetitionLayout.vue)；
   - 针对理论知识赛场景定制：
     - 保持与 CTF 布局一致的纯粹标题呈现，同步采用相同的“无法再缩小才触发 `...` 省略”的高精度动态字号自适应算法；
     - 退出按钮语义化为"退出考场"；
     - 右侧支持考生个人名册徽章（`candidate-pill`）与战队名册徽章（`team-pill`）动态切换；
     - 倒计时标签语义化为"答题剩余时间 / 考核未开始 / 理论考核已结束"，且阈值小于 15 分钟触发高亮警示态；
     - 主工作区支持 `<slot>` 插槽与 `<router-view>` 双模式，便于作为页面级包裹容器或路由骨架直接复用。

3. **统一聚合导出与向后兼容**：
   - 创建 [`competitions/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/competitions/index.ts)：
     ```ts
     export { default as CtfCompetitionLayout } from './CtfCompetitionLayout.vue'
     export { default as TheoryCompetitionLayout } from './TheoryCompetitionLayout.vue'
     ```
   - 重构 [`layouts/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/index.ts)：
     ```ts
     export { default as AdminLayout } from './AdminLayout.vue'
     export { default as AppLayout } from './AppLayout.vue'
     export { default as AuthLayout } from './AuthLayout.vue'
     export * from './competitions'
     // 保持向后兼容别名导出
     export { CtfCompetitionLayout as CompetitionLayout } from './competitions'
     ```
   - 移除了原根路径的旧文件 [`CompetitionLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/CompetitionLayout.vue)。

### 2.3 路由层映射路径对齐 (`router/routes.ts`)
- 将 `/arena/:slug` 路由引用的布局组件直接更新为新路径：
  ```ts
  {
    path: '/arena/:slug',
    component: () => import('@/public/volurEx/layouts/competitions/CtfCompetitionLayout.vue'),
    meta: { requiresAuth: true },
    children: [ ... ]
  }
  ```

---

## 3. 安全隐患与规范符合性说明

1. **防范幽灵依赖与维护割裂**：彻底移除历史桥接目录 `src/layout`，防止未来开发者误在该目录下新增、修改或维护重复的布局文件导致线上/线下行为不一致；
2. **严格遵循组件库设计规范**：所有平台级布局组件均归拢于 `@/public/volurEx/layouts`，并通过 `volurEx/index.ts` 集中导出，100% 契合 `GEMINI.md` 与 `view-Rules.md` 铁律；
3. **平滑向后兼容**：在 `layouts/index.ts` 中保留 `export { CtfCompetitionLayout as CompetitionLayout }` 别名导出，确保任何以旧命名解构引用的历史模块或第三方扩展不发生破坏性中断。

---

## 4. 影响范围与编译验证

### 4.1 影响文件清单
- **已删除文件**：
  - `src/volur-lite-view/src/layout/AdminLayout.vue`
  - `src/volur-lite-view/src/layout/AppLayout.vue`
  - `src/volur-lite-view/src/layout/AuthLayout.vue`
  - `src/volur-lite-view/src/layout/CompetitionLayout.vue`
  - `src/volur-lite-view/src/public/volurEx/layouts/CompetitionLayout.vue`
- **新建文件**：
  - `src/volur-lite-view/src/public/volurEx/layouts/competitions/CtfCompetitionLayout.vue`
  - `src/volur-lite-view/src/public/volurEx/layouts/competitions/TheoryCompetitionLayout.vue`
  - `src/volur-lite-view/src/public/volurEx/layouts/competitions/index.ts`
- **修改文件**：
  - `src/volur-lite-view/src/public/volurEx/layouts/index.ts`
  - `src/volur-lite-view/src/router/routes.ts`

### 4.2 编译与类型检查验证
1. 执行 `npx vue-tsc --noEmit`：通过，0 errors；
2. 执行 `npm run build`：通过，耗时 5.29s，产物 `dist/assets/CtfCompetitionLayout-DwmJGqMR.js (5.96 kB)` 正确生成。
