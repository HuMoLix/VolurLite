# 比赛公告提示清理与赛题管理创建弹窗排版优化施工记录

**施工日期**：2026-09-18  
**施工模块**：`src/volur-lite-view/src/views/admin/competition/tabs/AnnounceTab.vue`、`src/volur-lite-view/src/views/admin/competition/tabs/ChallengeTab.vue`  
**责任主体**：Antigravity AI Agent  

---

## 1. 变更背景与需求分析

根据用户最新反馈，针对管理端比赛编辑视图（`AdminCompetitionEditView.vue`）下属子标签页进行了以下视觉与交互优化：
1. **比赛公告（AnnounceTab）**：
   - 彻底删除顶部常驻的提示信息卡片（“比赛专属实时通报：本模块发布的公告仅向参与当前比赛的选手进行推送与展示，区别于全站公开公告，适用于赛题解题提示 (Hint)、赛程变动、靶机维护与紧急规则澄清。”），精简视觉噪音，突出核心公告列表。
2. **赛题管理（ChallengeTab）文案升级**：
   - 将原“从题库导入赛题”字样全面升级为“创建题目”，使赛题操作入口更加符合常规出题和挂载的操作心智。
   - 同步更新空数据状态下的引导文案（“暂无挂载赛题，请点击右上角「创建题目」进行添加”）。
3. **“创建题目”卡片弹窗排版紧凑与粘连问题修复**：
   - 弹窗顶部筛选栏中，“按赛题分类筛选”、“搜索题库题目名称”输入框与“查询”按钮紧贴粘连在一起，视觉拥挤。
   - 根因分析：弹窗 Teleport 到 `document.body` 后，原有嵌套在 `.challenge-tab` 作用域下的 `.bank-filter-bar` flex 及 gap 样式未能命中；同时按钮与输入框高度不统一。

---

## 2. 核心改动点与实施细节

### 2.1 比赛公告提示卡片清理 (`AnnounceTab.vue`)
- 移除了 `<AppTableCard>` 中包含的 `#table-header` 插槽及 `<n-alert>` 组件；
- 清理了 `<style scoped lang="scss">` 中遗留的 `.tab-intro-alert` 样式定义；
- 从 `naive-ui` 导入列表中移除了未被引用的 `NAlert` 组件，保证 `vue-tsc` 严格类型检查 0 告警。

### 2.2 赛题管理文案更新与弹窗组件化排版重构 (`ChallengeTab.vue`)
1. **文案升级**：
   - 顶栏主操作按钮文本由“从题库导入赛题”变更为“创建题目”；
   - 弹窗标题统一为“创建题目”；
   - 空状态引导文案由“请点击右上角「从题库导入赛题」进行挂载”变更为“请点击右上角「创建题目」进行添加”。
2. **筛选栏组件化弹性重构**：
   - 使用 `<n-space align="center" :size="12" class="bank-filter-bar">` 容器替换普通 `div`，彻底解决 Teleport 后 CSS 样式失效问题；
   - 搜索输入框宽度调整为 `280px`，并内嵌 `<template #prefix><n-icon :component="SearchOutline" /></template>` 图标；
   - 查询按钮升级为标准高度与主色调按钮（`type="primary" class="theme-primary-btn"`），附带 `SearchOutline` 图标，实现与输入框严格垂直居中并保持 12px 舒适间距；
   - 弹窗主体包裹在 `<n-space vertical :size="16" style="width: 100%;">` 容器中，保证筛选栏、默认分值参数卡片与题目选择表格之间垂直间距稳定；
   - 清理了 SCSS 中失效的 `.import-modal-container` 样式块。

---

## 3. 验证与构建测试

在 `src/volur-lite-view` 目录下执行编译检查与打包：
```bash
npm run build
# 执行: vue-tsc --noEmit && vite build
```

**验证结果**：
- `vue-tsc --noEmit`：0 错误，TypeScript 严格类型检查完全通过；
- `vite build`：生产环境打包成功（耗时 5.53s）。

---

## 4. 影响范围与审计评估

- **改动文件**：
  - `src/volur-lite-view/src/views/admin/competition/tabs/AnnounceTab.vue`
  - `src/volur-lite-view/src/views/admin/competition/tabs/ChallengeTab.vue`
- **视觉一致性**：
  - 公告列表页界面更加简洁清晰；
  - 创建题目弹窗顶部各筛选控件间距规整、层级分明、按钮高度统一且回车即搜，交互体验显著提升。
