# CTF 竞技场左侧搜索框重构为通用 VSearch 组件方案

## 1. 变更背景与目标

在 `volur-lite-view-NEXT` 工程中，CTF 竞技场页面（`CtfCompetitionView.vue`）的“赛题挑战”主视图（`CtfCompetitionChallengeListView.vue`）左侧侧边栏原本使用了专属定制的 `VChallengeSearch.vue` 组件。

随着平台通用基础组件体系的演进，全局已抽离出统一的规范化通用搜索组件 `src/components/common/VSearch.vue`（底层基于 Naive UI `<n-input>` 与统一 Google Material Design 规范深度定制，原生支持双向数据绑定、动态圆角控制、暗黑模式色彩层级与强类型事件）。

为消除冗余专用搜索组件、统一搜索交互视觉标准并消除维护成本，本次任务针对 CTF 竞技场视图中的左侧搜索框进行重构：
1. 分析 `CtfCompetitionView.vue` 与其子视图 `CtfCompetitionChallengeListView.vue` 左侧搜索框的结构与过滤逻辑；
2. 彻底替换原专用组件为平台通用组件 `VSearch.vue`；
3. 移除已被完全取代的冗余组件 `VChallengeSearch.vue` 及其导出定义，杜绝历史包袱遗留。

---

## 2. 核心分析与改动点

### 2.1 CTF 竞技场左侧搜索框结构分析
- **路由视图层级**：`CtfCompetitionView.vue` 作为 `/competitions/:id/ctf` 路由的主入口，通过标签页导航（赛题挑战、官方公告、计分排行榜、我的战队）调度子视图；
- **左侧侧边栏定位**：在默认的“赛题挑战”视图 `CtfCompetitionChallengeListView.vue` 中，左侧侧边栏 `<aside class="w-full lg:w-60 flex-shrink-0 flex flex-col gap-4">` 依次排列了分类导航 `VCtfCategory`、搜索框以及战队/个人统计卡片 `VCtfCompetitionStatistic`；
- **响应式过滤机制**：搜索框与响应式变量 `searchQuery` 绑定，驱动 `filteredChallenges` 计算属性实时对赛题标题（`title` / `name`）与分类（`category` / `categoryName`）进行大小写无关的过滤匹配。

### 2.2 替换为通用组件 VSearch
- **模板调用调整**：
  在 `CtfCompetitionChallengeListView.vue` 中，将原 `<VChallengeSearch v-model="searchQuery" />` 替换为：
  ```vue
  <VSearch
    v-model:value="searchQuery"
    :placeholder="t('competition.search_challenge', '搜索赛题名称或分类...')"
    clearable
    round
  />
  ```
- **配置与特性说明**：
  - `v-model:value="searchQuery"`：与现有响应式状态双向联动，在用户键入或点击清空按钮时实时更新过滤条件；
  - `round`：启用药丸状完全胶囊圆角，与左侧分类交互项及平台整体 Material 设计语言完美契合；
  - `clearable`：内置一键清空按钮，配合原生快捷 Enter 搜索；
  - `placeholder`：复用多语言键 `competition.search_challenge`。

### 2.3 依赖梳理与彻底性清理
- **移除冗余组件**：彻底删除 `src/components/competition/ctf/VChallengeSearch.vue`；
- **导出同步更新**：在 `src/components/competition/ctf/index.ts` 中移除 `VChallengeSearch` 的导出声明；
- **无别名无残留**：全局检索确认无任何残留的 `VChallengeSearch` 引用或兼容桥接。

---

## 3. 规范审计与验证结果

1. **纯粹代码与零注释原则**：所有修改的代码文件中无任何解释性注释或废弃代码块，严格符合代码纯净性规范；
2. **构建与类型验证**：执行 `npm run build`（包含 `vue-tsc --noEmit` 与 Vite 生产打包），全工程 0 错误一次性通过，产物生成正常。
