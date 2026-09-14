# 竞赛前台预览横幅清理与编辑视图操作栏重构审计说明

## 1. 变更背景与需求说明
根据用户的最新指令：
1. **清理 Preview 页面的提示横幅**：
   > 「修改 preview 页面中不要显示"前台视图预览模式 当前正在以管理员视角实时预览竞赛前台效果。实际参赛选手看到的页面与当前展示完全一致。"字样」
2. **迁移与覆盖前台预览入口**：
   > 「然后把 @[src/volur-lite-view/src/views/admin/competition/AdminCompetitionEditView.vue] 中的预览竞赛前台放到 @[src/volur-lite-view/src/views/admin/competition/tabs/BasicInfoTab.vue] 中（进行覆盖）。」
3. **清理编辑视图顶栏操作**：
   > 「然后删除 AdminCompetitionEditView 中的实时封榜和右侧的返回列表。」

---

## 2. 核心改动点与实现

### 2.1 详情预览视图横幅清理 ([`CompetitionDetailView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionDetailView.vue))
- 从模板中彻底移除了 `admin-preview-banner`（`<n-alert>` 提示条及内部文案与操作按钮）；
- 清理了 `<style lang="scss" scoped>` 中对应的 `.admin-preview-banner` 样式块；
- 保留标题栏小巧的「前台预览」圆角徽章与顶部「返回比赛管理」导航按钮，保持页面纯净度的同时不失便捷导航能力。

### 2.2 竞赛编辑页操作栏精简与重构 ([`AdminCompetitionEditView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/AdminCompetitionEditView.vue))
- **移除顶栏操作区**：
  - 删除了顶部的「预览竞赛前台」按钮；
  - 删除了「实时封榜 / 解封榜单」按钮及相关的 `toggleFreezeScoreboard()` 弹窗逻辑与 `setFreezeScoreboardApi` API 引用；
  - 删除了右侧重复的「返回列表」按钮（左侧返回面包屑按钮继续保留，由用户控制回退）；
  - 彻底移除了空的 `<div class="header-actions">` 及其样式。
- **清理无用依赖与声明**：
  - 移除了 `useDialog`、`EyeOutline`、`setFreezeScoreboardApi` 等不再被使用的依赖和实例，保持脚本纯净度。

### 2.3 基础信息 Tab 预览入口覆盖 ([`BasicInfoTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/tabs/BasicInfoTab.vue))
- **操作栏覆盖整合**：
  - 移除了外链跳转至前台的 `openPublicPage`（「打开前台公开页面」按钮）及 `OpenOutline` 图标；
  - 由 SPA 单页预览按钮「预览竞赛前台」（`goToPreview()`，路由指向 `/admin/competitions/:id/preview`）直接覆盖其功能位置；
  - 基础信息操作栏收敛为统一的两个核心操作：`预览竞赛前台` + `保存基础信息`。

---

## 3. 规范与性能收益

1. **统一路由导航心智**：
   - 杜绝了新开浏览器标签页（`_blank`）造成的页面脱节，将前台预览全部统一收敛到 SPA 路由体系中；
2. **精简操作层次（Thin View）**：
   - 将与具体基础配置强相关的预览动作下沉到「基础信息与赛程」Tab，顶栏更聚焦于展示当前赛事的名称、赛制及状态；
3. **保持类型与编译整洁**：
   - 严格清理未使用导入与多余函数，无任何 TypeScript 报错与废弃样式规则。

---

## 4. 构建与验证结论

执行前端编译检查 `npm run build`（`vue-tsc --noEmit && vite build`）：
```bash
> volur-lite-view@1.0.0 build
> vue-tsc --noEmit && vite build
✓ built in 4.46s
```
- **TypeScript 类型检查**：0 错误、0 警告；
- **Vite 生产构建**：全量打包成功通过。
