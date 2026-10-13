# CTF 竞技场与赛题分类徽标徽章移除方案

## 1. 变更背景与目标

在 CTF 竞技场视图（`CtfCompetitionView`）和赛题分类组件（`VCtfCategory.vue`）的界面呈现中：
1. 顶部 Tab 栏（赛题挑战、官方公告等）渲染了数字徽章（`.v-filter-btn-count`），界面信息密度过高；
2. 侧边栏赛题分类列表项右侧渲染了分类题目统计徽章（`.challenge-btn-badge`）。

为了使 CTF 竞赛界面视觉更加统一简洁、降低杂乱干扰，本次优化要求：
1. 分析并移除 `CtfCompetitionView` 中由 `tabOptions` 驱动渲染的 `v-filter-btn-count` 徽章；
2. 彻底移除 `VCtfCategory.vue` 模板中的 `challenge-btn-badge` 节点及关联的所有 SCSS 样式定义。

---

## 2. 核心改动点

### 2.1 `CtfCompetitionView` 移除 `v-filter-btn-count` 徽标数据
- 文件路径：`src/views/competition/CTFCompetitionView.vue`
- 原逻辑：在 `tabOptions` 计算属性中为 `challenges` 传入了 `count: challenges.value.length`，为 `announcements` 传入了 `count: announcements.value.length`。底层组件 `VFilterBtnGroup` 检测到 `count !== undefined` 时自动渲染了 `.v-filter-btn-count` 胶囊徽章。
- 改动：从 `tabOptions` 中移除 `count` 属性配置，使 Tab 仅保留图标与标题文本，彻底消除 `.v-filter-btn-count` 元素。

### 2.2 `VCtfCategory.vue` 移除 `challenge-btn-badge` 徽标及样式
- 文件路径：`src/components/competition/ctf/VCtfCategory.vue`
- 模板改动：
  - 删除 `<span v-if="item.count !== undefined" class="challenge-btn-badge">{{ item.count }}</span>` 节点。
- 样式改动：
  - 删除日间模式下 `.challenge-btn-badge` 的定位、字体、背景与边框圆角样式规则；
  - 删除夜间模式 `:global(.dark)` 下在 hover、selected 以及默认状态下针对 `.challenge-btn-badge` 的所有颜色覆盖规则。

---

## 3. 规范执行与验证

1. **纯粹代码与零注释铁律**：`CtfCompetitionView` 与 `VCtfCategory.vue` 中无任何行内解释性注释或多余备注。
2. **构建与类型验证**：执行 `npm run build`（包含 `vue-tsc --noEmit` 与 Vite 打包），项目编译通过无任何类型错误或语法警告。
