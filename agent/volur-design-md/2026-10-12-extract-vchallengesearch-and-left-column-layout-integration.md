# 赛题搜索功能组件提取 (VChallengeSearch) 与左侧竖向弹性布局重构方案

## 1. 变更背景与目标

为进一步提升 CTF 赛题列表页面的模块化程度与交互层次，需要将赛题过滤搜索框从赛题网格流区域中解耦出来：
1. 封装独立的赛题搜索输入组件 `VChallengeSearch.vue`，集中维护搜索输入、清空按钮及回车触发等交互；
2. 在 `src/components/competition/ctf/` 下维护并在其 `index.ts` 导出该组件；
3. 将 `VChallengeSearch.vue` 与分类列表组件 `VCtfCategory.vue` 统一放置在页面左侧侧边栏区域，并采用垂直弹性布局（`flex flex-col gap-4`），使分类导航与关键字搜索在侧边形成统一紧凑的过滤控制器。

---

## 2. 核心改动点

### 2.1 抽取 `VChallengeSearch.vue` 组件
- 路径：`src/components/competition/ctf/VChallengeSearch.vue`
- 结构设计：
  - 基于 Vue 3 `defineModel<string>()` 实现响应式 `v-model` 双向数据绑定；
  - 带有内置放大镜前置图标（`SearchOutline`）；
  - 当输入框有值时动态展现带微动画的右侧圆形清空按钮（`CloseCircleOutline`），点击即清空并派发 `clear` 事件；
  - 支持回车派发 `search` 事件；
  - 样式遵循 Google Material Design 现代轻量输入框规范（圆角 `rounded-2xl`，高度 40px，聚焦边框对齐 Google Blue）。
- 在 `src/components/competition/ctf/index.ts` 中集中导出。

### 2.2 重构 `CtfCompetitionChallengeListView.vue` 左侧与主内容布局
- 路径：`src/views/competition/tabs/CtfCompetitionChallengeListView.vue`
- 左侧 `aside` 布局改造：
  ```html
  <aside class="w-full lg:w-60 flex-shrink-0 flex flex-col gap-4">
    <VCtfCategory
      v-model="selectedCategory"
      :categories="categories"
      :category-counts="categoryCountsMap"
      :total-count="challenges.length"
    />
    <VChallengeSearch
      v-model="searchQuery"
    />
  </aside>
  ```
  通过 `flex flex-col gap-4` 实现分类导航在上方、搜索框在下方的清晰垂直层次；
- 右侧 `main` 区域移除原冗余的单侧搜索框，直接聚焦于赛题卡片网格列表呈现，减少视觉噪点。

---

## 3. 规范执行与验证

1. **纯粹代码与零注释铁律**：全部新增及修改文件通过 `Select-String` 模式扫描验证，不存在任何注释内容；
2. **构建验证**：执行 `npm run build` (`vue-tsc --noEmit && vite build`)，打包与类型检查 0 错误通过。
