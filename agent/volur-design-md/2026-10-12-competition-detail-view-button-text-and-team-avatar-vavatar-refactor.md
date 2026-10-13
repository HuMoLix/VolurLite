# 竞赛详情视图按钮文案调整与战队头像统一采用 VAvatar 组件重构报告

## 1. 变更背景与需求说明
在 Volur Lite 新一代前端工程（`src/volur-lite-view-NEXT`）中：
1. 竞赛详情视图（`CompetitionDetailView.vue`）中，原先当竞赛处于进行中状态时，进入考场的引导按钮文案为“进入竞赛工作台”，为进一步提升界面凝练度并与整体极简设计风格保持一致，按要求精简调整为“竞赛”；
2. 竞赛模块中战队头像（包括 `CompetitionDetailView.vue`、`TeamDetailModal.vue` 与 `TeamSignupModal.vue`）此前采用了手写的原生 `div` 配合 `img` 与 `n-icon` 降级显示，未充分复用系统封装的平台通用 `VAvatar.vue`（`@/components/sidebar/VAvatar.vue`）组件，存在图片加载错误兜底缺失、组件体系不统一及样式维护割裂等问题。因此将战队头像统一收敛重构为采用 `VAvatar.vue` 组件渲染。

---

## 2. 核心改动点

### 2.1 按钮文案与国际化支持
- **组件视图**：
  - 在 `src/volur-lite-view-NEXT/src/views/competition/CompetitionDetailView.vue` 中，将进行中状态的进入按钮文案从 `t('competition.enter_arena', '进入竞赛工作台')` 统一修改为 `t('competition.enter_arena', '竞赛')`。
- **国际化配置**：
  - `src/volur-lite-view-NEXT/src/locales/zh-CN/competition.json`：新增 `"enter_arena": "竞赛"` 键值；
  - `src/volur-lite-view-NEXT/src/locales/zh-TW/competition.json`：新增 `"enter_arena": "競賽"` 键值；
  - `src/volur-lite-view-NEXT/src/locales/en-US/competition.json`：新增 `"enter_arena": "Compete"` 键值。

### 2.2 战队头像统一采用 VAvatar 组件
- **主详情视图 (`CompetitionDetailView.vue`)**：
  - 引入 `import VAvatar from '@/components/sidebar/VAvatar.vue'`；
  - 移除此前手写的 `w-14 h-14 rounded-2xl overflow-hidden ... img` 结构，替换为 `<VAvatar :src="myTeam.avatarUrl || ''" :alt="myTeam.teamName" :size="56" :radius="16" class="border border-google-border-light dark:border-google-border-dark shrink-0">`；
  - 针对无头像或图片加载失败的场景，通过 `#guest` 插槽优雅呈现 `<n-icon :size="28" :component="PeopleOutline" class="text-google-blue dark:text-google-blue-dark" />`，完美契合战队团队属性。
- **战队详情弹窗 (`TeamDetailModal.vue`)**：
  - 引入 `VAvatar` 组件，将战队头部头像重构为一致的 `VAvatar` 结构，确保详情弹窗与外层卡片头像视觉与行为 100% 对齐。
- **战队报名弹窗 (`TeamSignupModal.vue`)**：
  - 引入 `VAvatar` 组件，将创建战队表单中的头像占位统一升级为 `VAvatar` 实例，提供统一的圆角、边框与主题自适应。

---

## 3. 安全与质量验证
- **TypeScript 严格类型检查**：运行 `npx vue-tsc --noEmit`，全项目无任何类型报错与隐式 `any` 警告，顺利通过。
- **Vite 生产构建验证**：运行 `npx vite build` 生产构建成功，所有产物与 chunk 正常输出，零语法/样式冲突。
