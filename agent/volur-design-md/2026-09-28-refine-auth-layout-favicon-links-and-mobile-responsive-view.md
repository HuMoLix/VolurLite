# 施工记录：Favicon 路径修复、卡片无边框与 Google 风格手机端/电脑端响应式深度优化

- **变更日期**：2026-09-28
- **涉及模块**：
  - `src/volur-lite-view-NEXT/src/components/layouts/auth/AuthLayout.vue`
  - `src/volur-lite-view-NEXT/plugins/publicResourceMapping.ts`
  - `src/volur-lite-view-NEXT/src/styles/index.scss`
  - `agent/volur-design-skills/view-NEXT-rules.md`
- **变更类型**：`fix(assets)` & `refactor(auth-ui)`

---

## 1. 变更背景与问题排查

1. **Favicon 静态路径修复与组件图标更迭**：用户将 Favicon 资源从 `public/static/` 整理至 `public/static/svg/` 子目录下，导致旧路径 `/r/static/favicon-long.svg` 无法请求。同时，按照要求在 `AuthLayout.vue` 中将 Logo 切换为正方形的 `favicon.svg` 而非长条形 `favicon-long.svg`；
2. **主题切换按钮清理**：移除 `AuthLayout.vue` 中内嵌的固定主题切换浮动按钮；
3. **背景颜色成因排查 (`#f8f9fa` vs `#f0f4f9`)**：
   - 经排查，`rgb(248, 249, 250)` (`#f8f9fa`) 为系统全局基底色 `--v-bg-base` 以及 Naive UI `<n-global-style>` 默认注入给 `body` 的 `bodyColor`；
   - 在认证场景下，已在 `.auth-layout` 样式中强制将电脑端背景声明为 `#f0f4f9`（`rgb(240, 244, 249)`，即 Material 3 容器表面色 `sys-color-surface-container`）；
4. **卡片视觉与链接交互规范化**：
   - 移除卡片外边框，实现无边框悬浮感；
   - 消除 `<a>` 链接被点击或访问后的默认样式，普通链接保持 hover 时显示下划线；
   - 区分普通链接与按钮型 `<a>`（`auth-btn-link`）：按钮型链接 hover 时无下划线，仅展示浅色半透明背景底色；
5. **语言与功能操作优化**：
   - 语言选择按钮尺寸适度放大，右侧“帮助”、“隐私权”、“条款”统一采用按钮型 `<a>` 呈现；
   - 语言选项精简为仅保留“简体中文”、“繁体中文”与“English”；
6. **手机端响应式深度重构**：
   - 当视口缩小至手机端（`< 840px`）时，取消 PC 端的卡片边框、背景及投影对比，直接展示卡片内容（上下布局）；
   - 底部操作与版权信息置底，并在同一行中无缝融合展示语言切换、帮助/隐私权/条款按钮、版权（`© 2026 Volur`）及纯备案图标（无文字）。

---

## 2. 核心改动点

### 2.1 Favicon 与静态资源服务增强
1. **[`AuthLayout.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/layouts/auth/AuthLayout.vue)**：
   - Logo 路径调整为 `/r/static/svg/favicon.svg`（暗色模式自适应为 `/r/static/svg/favicon-dark.svg`）；
2. **[`publicResourceMapping.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/plugins/publicResourceMapping.ts)**：
   - 增加对浏览器原生直接请求根路径 `/favicon.ico` 与 `/favicon.svg` 的直接映射支持，直接输出 `image/svg+xml`。

### 2.2 超链接与按钮链接样式规范 (`index.scss`)
在 [`src/styles/index.scss`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/styles/index.scss) 中配置全局规则：
- 普通 `<a>`：`&:hover { text-decoration: underline; }`，消除 `:visited`、`:active` 的杂色与虚线框；
- 按钮式 `<a>` (`.auth-btn-link`)：内边距与圆角设计，`&:hover { text-decoration: none !important; background-color: rgba(60, 64, 67, 0.08); }`（暗色下为 `rgba(255, 255, 255, 0.08)`）。

### 2.3 `AuthLayout.vue` 响应式与交互重构
1. **电脑端视口 ($\ge 840\text{px}$)**：
   - `.auth-layout` 背景固定为 `#f0f4f9`（`sys-color-surface-container`）；
   - `.auth-card` 采用 `#ffffff`、无边框（`border: none !important`）、28px 圆角与柔和阴影，左右分栏呈现；
   - 卡片正下方展示独立辅助栏（放大版语言切换按钮 + 按钮式帮助/隐私权/条款链接）；
   - 视口底部渲染全局透明页脚；
2. **手机端视口 ($< 840\text{px}$)**：
   - `.auth-layout` 背景与内容融合无生硬卡片外框，`.auth-card` 背景透明、无边框、无阴影、上下结构排布；
   - 底部融合单行栏 `.auth-mobile-bar`：
     - 左侧：紧凑版语言切换下拉按钮；
     - 右侧同一行：帮助、隐私权、条款超链接 + `© 2026 Volur` + 工信部备案纯图标（`<img src="/r/static/img/beian.png" />`，点击直达官网）。
3. **语言切换菜单项精简**：
   - 仅保留“简体中文”（默认）、“繁体中文”、“English”，移除了“日本語”。

---

## 3. 验证与审计结果

1. **类型检查与全量构建**：
   - 执行 `npm run build`，`vue-tsc --noEmit` 0 错误通过，Vite 打包产物生成无告警，耗时 2.21s；
2. **开发服务器验证**：
   - `GET /r/static/svg/favicon.svg` $\rightarrow$ `HTTP 200 OK` (MIME: `image/svg+xml`)；
   - `GET /favicon.ico` $\rightarrow$ `HTTP 200 OK` (MIME: `image/svg+xml`)；
   - `GET /src/components/layouts/auth/AuthLayout.vue` $\rightarrow$ `HTTP 200 OK`；
3. **视觉布局验证**：
   - 电脑端背景呈现为精确的 `#f0f4f9`，卡片无生硬边框线；
   - 帮助/隐私权/条款 hover 时仅显示半透明背景，不再产生下划线；
   - 缩窄至手机端时，卡片背景和边框隐去，底部所有辅助功能、版权与纯备案图完美收敛于同一行。
