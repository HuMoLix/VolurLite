# AppLayout 暗黑模式背景色修复方案 (2026-10-11)

## 1. 变更背景与问题溯源

在暗黑模式下访问赛事大厅页面（`CompetitionListView.vue`）时，页面主内容区域背景依然显示为刺眼的纯白色（`#fff`），未能正确呈现为平台侧边栏与整体设计所要求的暗色底色 `#191919`。

经排查，链路依赖与成因如下：
1. **路由与布局依赖**：`CompetitionListView.vue` 由 `src/router/routes/competition.ts` 路由挂载，其外层父级布局组件为 `AppLayout.vue`；
2. **样式覆盖问题**：`CompetitionListView.vue` 内部无全局背景样式，完全继承父级容器样式。而在 `AppLayout.vue` 的 Scoped SCSS 中，`.app-main` 容器硬编码了 `background: #fff;`，且未声明任何暗夜模式覆盖样式（缺少 `html.dark &` 规则）；
3. **颜色不统一**：`VSideBar.vue`、`VCard.vue`、`VNotice.vue` 等组件在暗黑模式下均使用 `#191919` 作为底层背景色，`.app-main` 的纯白硬编码破坏了暗夜模式的整体一致性。

---

## 2. 改动详情

### 2.1 布局组件更新 (`src/components/layouts/AppLayout.vue`)

1. **模板根容器**：
   - 将外层根元素 `.app-layout` 的 Tailwind 暗黑背景类从 `dark:bg-google-surface-dark` 调整为 `dark:bg-[#191919]`，与侧边栏基底保持严格一致。

2. **样式块扩展**：
   - 为 `.app-layout` 添加 `html.dark & { background-color: #191919; }`；
   - 为 `.app-main` 添加平滑过渡动画 `transition: background-color 0.3s ease;`；
   - 为 `.app-main` 添加暗黑模式背景样式覆盖：
     ```scss
     .app-main {
       position: relative;
       background: #fff;
       transition: background-color 0.3s ease;

       html.dark & {
         background-color: #191919;
       }
     }
     ```

---

## 3. 影响范围与验证结果

- **受影响范围**：
  - `src/components/layouts/AppLayout.vue`；
  - 所有挂载在 `AppLayout` 下的子视图页面（包括但不限于 `/competitions`、`/competitions/:id`、`/c/:id` 等）。
- **验证结论**：
  - 执行 `npm run build`（包含 `vue-tsc --noEmit` 与 `vite build`）全量类型检查与打包，构建耗时 14.45s，打包结果零错误零告警退出（Exit Code 0）。
