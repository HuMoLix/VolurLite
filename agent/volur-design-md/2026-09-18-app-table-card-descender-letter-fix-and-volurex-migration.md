# AppTableCard 输入框 "g" 字母下延截断修复与 volurEx 架构迁移施工记录

**施工日期**：2026-09-18  
**施工模块**：`src/volur-lite-view/src/public/volurEx/components/AppTableCard.vue`、`src/volur-lite-view/src/public/volurEx/components/index.ts`、`src/volur-lite-view/src/styles/index.scss`、`src/volur-lite-view/src/components/common/AppTableCard.vue`、`src/volur-lite-view/src/views/admin/user/AdminUserView.vue`  
**责任主体**：Antigravity AI Agent

---

## 1. 变更背景与缺陷根因

### 1.1 输入框含字母 "g" 等显示不全缺陷分析
- **现象描述**：在包含字母 "g"（以及 "y"、"p"、"q"、"j" 等基线以下带有 descender 下延部英文字符）的输入框或占位符中，字符底部环/尾部出现物理裁切、残缺不全的视觉缺陷。
- **排查根因**：
  1. `src/styles/index.scss` 中存在 `.n-input:not(.n-input--textarea) .n-input__placeholder > span { line-height: 1 !important; }`，过小的行高使行框高度受限于字号（em-box），基线以下的下延字符被强制剪裁；
  2. 输入框内部容器高度紧凑，在配置了 `overflow: hidden` 与垂直居中时，缺乏充足的底部内边距与垂直安全裕量。

### 1.2 组件库架构归集需求
- 按照 Volur 前端工程规范（`view-Rules.md` 第 10 条），平台自研的核心高复用通用组件统一归集在 `src/public/volurEx`（别名 `@/public/volurEx`）组件库下，遵循组件化解耦与统一导出的设计哲学。将 `AppTableCard` 正式提升迁移至 `volurEx/components`。

---

## 2. 改造方案与实施细节

### 2.1 修复 "g" 字母下延显示截断
1. **全局行高解封**：
   在 `src/styles/index.scss` 中，将 `.n-input:not(.n-input--textarea) .n-input__placeholder > span` 的 `line-height: 1 !important;` 修正为安全舒适的 `line-height: 1.4 !important;`，避免行框高度压迫基线字符。
2. **AppTableCard 内部专属高度与裕量微调**：
   在 `AppTableCard` 中针对单行输入框强化样式声明：
   - 配置舒适高度 `--n-height: 36px; height: 36px;`，让内容呈现更舒展；
   - 内部 `.n-input__input` 配置 `display: flex; align-items: center; height: 100%;`；
   - `.n-input__placeholder > span` 显式设置 `line-height: 1.5 !important; padding-bottom: 2px;`，为 descender 下延预留足够的基线渲染空间；
   - 输入框原生元素 `.n-input__input-el` 及其伪元素 `::placeholder` 配置 `line-height: 1.5` 与 `box-sizing: border-box`，彻底消除任何英文字母被削切的瑕疵。

### 2.2 AppTableCard 迁移归集到 volurEx 组件库
1. **组件落地与声明**：
   在 `src/volur-lite-view/src/public/volurEx/components/AppTableCard.vue` 创建完整的自研卡片组件实现，命名为 `VolurTableCard`（别名 `AppTableCard`），包含完整的 Props、Emits、Slots 定义与响应式 DOM 监听。
2. **集中向外导出**：
   在 `src/public/volurEx/components/index.ts` 中导出：
   - `AppTableCard`、`VolurTableCard`
   - `AppTableCardProps`、`VolurTableCardProps`
   通过 `volurEx/index.ts` 集中向外导出供全平台业务视图与布局消费。
3. **向后兼容与平滑过渡**：
   在原 `src/components/common/AppTableCard.vue` 中重构为强类型透明代理组件，引入 `@/public/volurEx` 并透传 Props 与 Slots，保证历史调用链路零破坏、TypeScript 类型推断零丢失。
4. **业务视图引用重构**：
   在 `src/views/admin/user/AdminUserView.vue` 中将原 `@/components/common/AppTableCard.vue` 引入方式重构为组件库标准：
   `import { AppTableCard } from '@/public/volurEx'`。

---

## 3. 验证与构建测试

在 `src/volur-lite-view` 根目录下执行：
```bash
npm run build
```
**构建结果**：
- `vue-tsc --noEmit`：0 错误，类型推断全部精准通过；
- `vite build`：生产环境编译打包顺利完成（耗时 6.73s）。

---

## 4. 影响范围与审计评估

- **新增与迁移文件**：
  - [`src/volur-lite-view/src/public/volurEx/components/AppTableCard.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/components/AppTableCard.vue)
  - [`src/volur-lite-view/src/public/volurEx/components/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/components/index.ts)
- **更新文件**：
  - [`src/volur-lite-view/src/styles/index.scss`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/styles/index.scss)
  - [`src/volur-lite-view/src/components/common/AppTableCard.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/components/common/AppTableCard.vue)
  - [`src/volur-lite-view/src/views/admin/user/AdminUserView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/user/AdminUserView.vue)
- **改进效益**：
  - 彻底攻克包括 "g", "y", "p", "q", "j" 等下延字母在内的文本展示截断问题，排版质感与视觉体验显著提升；
  - 架构完全契合 `volurEx` 自研组件库规范，职责物理隔离明确，扩展性与规范性更加优异。
