# NotFoundView 404 视图精简与按钮重构设计文档

## 1. 变更背景与需求说明
根据交互精简化需求，对 `src/views/error/NotFoundView.vue`（404 页面）进行视觉层级降噪：
1. 移除顶部的 `<header>` 导航栏与 Logo 链接；
2. 移除左下角的浮动主题切换 `<aside>` 控制器（避免与全局规范或布局冲突）；
3. 操作按钮区仅保留“返回上一页”（`t('common.back_prev')`）按钮，移除冗余的“返回赛事大厅”（`t('common.back_to_competitions')`）按钮。

## 2. 核心架构与改动点

### 2.1 模板精简 (`src/views/error/NotFoundView.vue`)
1. **结构清理**：
   - 彻底删除 `<header>` 节点；
   - 彻底删除固定在左下角的 `<aside class="fixed bottom-5 left-5 z-[1000]">` 主题切换按钮节点；
   - 移除“返回赛事大厅”主操作按钮，将“返回上一页”按钮提升为主操作按钮样式（Google Blue 药丸按钮 `h-10 px-6 rounded-full bg-google-blue ...`）。
2. **状态与方法收敛**：
   - 移除已无用处的 `goToCompetitions` 方法；
   - 移除由于删除 aside 主题切换而不再需要的 `useAppStore` 与 `appStore` 实例；
   - 仅保留 `router`、`t`（i18n）以及具备回退 fallback 能力的 `goBack` 方法。

## 3. 代码纯洁性与规范审计
- **零注释铁律**：代码文件完全遵循零注释原则，无任何解释性注释；
- **类型安全**：TypeScript 严格模式无任何未使用变量报警（TS6133）；
- **工程构建**：通过 `vue-tsc --noEmit && vite build` 生产级构建验证，零错误零警告。
