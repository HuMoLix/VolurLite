# 比赛管理编辑视图 Tabs 弹窗组件化排版重构施工记录

**施工日期**：2026-09-18  
**施工模块**：`src/volur-lite-view/src/views/admin/competition/tabs/`  
**责任主体**：Antigravity AI Agent  

---

## 1. 变更背景与缺陷根因分析

在管理端比赛编辑视图（`AdminCompetitionEditView.vue`）下属的各个功能子标签页（比赛公告、赛题管理、团队审核、Writeup 评审等）中，存在弹出模态框（Modal Card）排版异常的问题，典型表现为：
- “取消”按钮与“确认发布 / 确认驳回 / 提交评审”按钮紧紧贴在一起，缺少合理间距与对齐；
- 底部操作区域未能与弹窗主体内容形成清晰的边界分割。

### 缺陷根因剖析：
1. **Naive UI 模态框 Teleport 与 Scoped CSS 隔离失效**：
   Naive UI 的 `<n-modal preset="card">` 默认会被 Teleport 传送到 `document.body` 根节点下。而在各个 Tab 的 Vue 单文件组件中，开发者将 `.modal-actions` 样式嵌套在组件作用域类（如 `.announce-tab`、`.challenge-tab`、`.team-audit-tab`、`.writeup-tab`）的 `<style scoped lang="scss">` 下。
   在 Vite / Vue 编译后，由于弹窗 DOM 渲染在 `document.body`，其外层并不存在宿主组件的根 class 及 scoped 属性属性，导致 `.modal-actions` 的 Flex 布局及 `gap: 12px` 样式完全未命中。
2. **反模式的自定义底部结构**：
   未采用 Naive UI 标准推荐的 `<template #footer>` 插槽规范，而是将按钮放置在表单内部的普通 `<div>` 中，破坏了组件化封装的规范性。

---

## 2. 核心重构方案与规范实施

根据平台统一的前端规范体系（`view-Rules.md`）及 Naive UI 标准组件化设计：
1. **统一使用 `<template #footer>` 插槽**：
   将所有弹窗内的动作控制按钮从表单主体内剥离，统一收敛至 `<n-modal>` 的原生 `#footer` 插槽中，保证语义清晰、布局独立；
2. **使用 `<n-space justify="end" :size="12">` 进行弹性布局**：
   `<n-space>` 组件通过内联注入弹性容器样式与间距，天然免疫 Teleport 跨 DOM 挂载和 Scoped CSS 样式隔离问题，彻底解决按钮粘连缺陷；
3. **清理失效冗余样式**：
   全量清理各 Tab 内部已被弃用的 `.modal-actions` SCSS 规则；
4. **组件库收敛对齐 (`volurEx`)**：
   顺带将相关视图中引用的 `AppTableCard` 全面切换为平台级标准自研组件库导入路径：`import { AppTableCard } from '@/public/volurEx'`。

---

## 3. 重构明细列表

| 文件路径 | 涉及弹窗 | 重构前问题 | 实施改造 |
| :--- | :--- | :--- | :--- |
| `tabs/AnnounceTab.vue` | `showModal` (发布/编辑公告)<br>`showDetailModal` (公告详情) | 按钮紧贴；详情弹窗无规范 footer | 按钮迁移至 `#footer` + `<n-space justify="end" :size="12">`；详情补充标准关闭 footer；清理失效 SCSS |
| `tabs/ChallengeTab.vue` | `showImportModal` (题库批量挂载)<br>`showConfigModal` (配置赛题参数) | 导入底部两端对齐失效；参数配置按钮粘连 | 导入模态框 `#footer` 采用两端对齐（提示文本 + `<n-space :size="12">`）；参数配置迁移至 `#footer` |
| `tabs/TeamAuditTab.vue` | `showRejectModal` (驳回队伍报名)<br>`showDetailModal` (战队详情名册) | 驳回理由内按钮紧贴；详情弹窗无标准 footer | 驳回操作按钮迁移至 `#footer` + `<n-space justify="end" :size="12">`；详情弹窗补充关闭按钮 |
| `tabs/WriteupTab.vue` | `showAuditModal` (评审队伍题解)<br>`showPreviewModal` (实时在线渲染) | 评审按钮紧贴；预览弹窗缺乏底栏关闭控制 | 评审操作按钮迁移至 `#footer` + `<n-space justify="end" :size="12">`；预览弹窗补充底栏关闭按钮 |
| `AdminCompetitionListView.vue`<br>`tabs/ContainerTab.vue` | 引用层级 | 历史引用路径 | 升级为 `import { AppTableCard } from '@/public/volurEx'` |

---

## 4. 验证与构建测试

在 `src/volur-lite-view` 目录下执行全量编译检查与打包验证：
```bash
npm run build
# 执行: vue-tsc --noEmit && vite build
```

**验证结果**：
- `vue-tsc --noEmit`：0 错误，TypeScript 类型检查全部通过；
- `vite build`：生产环境打包成功（耗时 5.40s），包含 `AdminCompetitionEditView` 在内的所有模块顺利构建产出。

---

## 5. 影响范围与审计评估

- **改动文件**：
  - `src/volur-lite-view/src/views/admin/competition/tabs/AnnounceTab.vue`
  - `src/volur-lite-view/src/views/admin/competition/tabs/ChallengeTab.vue`
  - `src/volur-lite-view/src/views/admin/competition/tabs/TeamAuditTab.vue`
  - `src/volur-lite-view/src/views/admin/competition/tabs/WriteupTab.vue`
  - `src/volur-lite-view/src/views/admin/competition/tabs/ContainerTab.vue`
  - `src/volur-lite-view/src/views/admin/competition/AdminCompetitionListView.vue`
- **视觉一致性**：
  - 管理后台所有比赛相关弹窗的按钮均遵循统一右对齐（`justify="end"`）和 12px 弹性间隙标准，取消与确认按钮样式层级分明；
  - 彻底杜绝了因 Teleport 穿透失效导致的不稳定布局现象，符合 Volur 平台统一的前端组件化设计规范。
