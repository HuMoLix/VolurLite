# 后台管理系统开关组件风格全域标准化重构施工文档

## 1. 变更背景

根据平台 UI/UX 设计规范升级要求，继系统设置模块（`AdminSystemView.vue`）完成向集群设置模块（`AdminClusterSettingsView.vue`）轻量纯净开关风格的对齐后，本次对整个管理后台（Admin Portal）中所有涉及开关（`n-switch`）的模块进行全局同步重构。

消除全部内部内嵌文字插槽（`<template #checked>` 与 `<template #unchecked>`），采用极简原生药丸滑块造型，并使用统一的 Flex 布局容器配合右侧轻量描述标签（`field-hint`），彻底统一全站开关交互体验。

---

## 2. 涉及文件与改动清单

### 2.1 用户管理模块 ([`AdminUserView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/user/AdminUserView.vue))
- **账号状态开关**：
  - 移除原 `<template #checked>正常启用</template>` 与 `<template #unchecked>封禁禁用</template>` 插槽；
  - 采用 `<div class="switch-row">` 统一包裹，右侧增加响应式状态提示文本：`{{ userFormData.status ? '正常启用' : '封禁禁用' }}`；
  - 样式表增加 `.switch-row` 布局定义（`display: flex; align-items: center;`）。

### 2.2 赛事管理模块 ([`AdminCompetitionEditView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/AdminCompetitionEditView.vue) 下属 Tabs)
1. **WP 提交管理 ([`WriteupTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/tabs/WriteupTab.vue))**：
   - 开放选手提交 WP 开关（`wpConfig.allowWriteupSubmission`）：移除原 `允许提交` / `禁止提交` 插槽；
   - 使用 `.switch-row` 包裹开关与动态提示 `{{ wpConfig.allowWriteupSubmission ? '允许提交' : '禁止提交' }}`。
2. **队伍审核管理 ([`TeamAuditTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/tabs/TeamAuditTab.vue))**：
   - 人工审核模式开关（`requireAudit`）：移除原 `需审核` / `免审直接参赛` 插槽；
   - 使用 `.switch-wrap` 布局结构，将开关与描述标签分离为右侧提示文字。
3. **赛事公告管理 ([`AnnounceTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/tabs/AnnounceTab.vue))**：
   - 公告置顶开关（`formModel.isPinned`）：移除原 `置顶于赛场顶部` / `普通排序` 插槽；
   - 使用 `.switch-row` 包裹开关与动态提示。

### 2.3 全局广播发布模块 ([`AdminBroadcastView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/broadcast/AdminBroadcastView.vue))
- **强制弹窗提醒开关（`formData.forcePopup`）**：
  - 移除内部文字插槽，采用 `.switch-row` 包裹自闭合开关与右侧对应国际化状态文本。

### 2.4 容器运维管理模块 ([`AdminContainerView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/container/AdminContainerView.vue))
- **手动启动出网权限开关（`manualStartForm.outboundEnabled`）**：
  - 移除内部文字插槽，采用 `.switch-row` 容器垂直居中对齐开关与出网/断网说明文本。

---

## 3. 规范对齐与安全分析

1. **零残留内嵌文本**：全项目前端代码库中经 Ripgrep 检索，已实现 100% 清理 `#checked` 与 `#unchecked` 插槽，彻底告别文字撑裂滑轨的视觉反模式。
2. **响应式与排版优化**：所有开关均配合 Flex 容器进行行高与水平中线对其，杜绝了表单基线漂移。
3. **类型与构建安全**：严格符合 Vue 3 `<script setup lang="ts">` 与 SCSS 隔离规范，无任何 `any` 类型引入。

---

## 4. 验证结果

- **前端打包编译**：执行 `npm run build` 成功完成，耗时 5.43s，0 错误，0 警告。
- **视觉一致性**：全后台所有配置界面的开关已达到完全统一的工业级质感。
