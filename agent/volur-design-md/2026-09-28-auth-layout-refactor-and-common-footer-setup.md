# 施工记录：Auth 布局解耦、通用透明页脚与 sys-color-surface-container 主题体系重构

- **变更日期**：2026-09-28
- **涉及模块**：
  - `src/volur-lite-view-NEXT/src/views/auth/`
  - `src/volur-lite-view-NEXT/src/components/layouts/auth/AuthLayout.vue`
  - `src/volur-lite-view-NEXT/src/components/common/Footer.vue`
  - `src/volur-lite-view-NEXT/src/styles/variables.scss`
  - `src/volur-lite-view-NEXT/src/styles/index.scss`
  - `src/volur-lite-view-NEXT/tailwind.config.ts`
  - `src/volur-lite-view-NEXT/src/router/routes/auth.ts`
  - `src/volur-lite-view-NEXT/src/router/routes/common.ts`
  - `src/volur-lite-view-NEXT/src/router/routes/competition.ts`
  - `agent/volur-design-skills/view-NEXT-rules.md`
- **变更类型**：`feat(view-NEXT)` & `refactor(layout)`

---

## 1. 变更背景与架构动因

此前在 `volur-lite-view-NEXT` 视图体系中：
1. 视图文件存放在 `src/views/v1/auth/` 嵌套路径中，造成了路径冗余；
2. 各登录/注册页面（`SignInIdentifierView.vue`、`SignInChallengeView.vue`、`SignUpView.vue`）均内联了全屏高度容器 `min-h-screen`、背景颜色、浮动主题控制器及独立的页脚，导致大量重复样板代码，缺乏统一的布局容器（Layout）约束；
3. 需要引入 Google Material Design 3 (M3) 规范中的容器表面色体系（`sys-color-surface-container: #f0f4f9`），并将页脚抽离为全局可复用组件，统一展示透明背景、版权声明与国家工信部备案信息。

---

## 2. 核心改动点

### 2.1 视图目录扁平化与解耦 (`src/views/`)
1. **删除 `src/views/v1` 文件夹**：
   - 将原 `src/views/v1/auth/` 下的全部认证视图移动至 `src/views/auth/`：
     - `SignInIdentifierView.vue`
     - `SignInChallengeView.vue`
     - `SignUpView.vue`
   - 将原 `src/views/v1/home/HomeView.vue` 移动至 `src/views/home/HomeView.vue`；
   - 彻底删除 `src/views/v1` 目录；
2. **路由配置同步更新**：
   - 同步更新 `common.ts` 与 `competition.ts` 中对 `HomeView.vue` 的引用路径（`@/views/home/HomeView.vue`）。

### 2.2 认证布局容器封装 (`components/layouts/auth/AuthLayout.vue`)
新建 [`src/components/layouts/auth/AuthLayout.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/layouts/auth/AuthLayout.vue)：
1. **统一视口与层级规范**：
   - 容器根节点配置 `min-h-screen flex flex-col justify-between`；
   - 整体背景色采用 `bg-sys-color-surface-container dark:bg-google-surface-dark transition-colors duration-300`；
2. **中心内容插槽与动态路由无缝兼容**：
   - 采用 `<router-view v-slot="{ Component }">` 结合 `<slot />` 兜底，既支持在路由表中作为父级 Layout 嵌套子路由，也支持作为单组件插槽复用；
   - 增加微平移动画过渡 `fade-slide`；
3. **全局控制器与统一页脚集成**：
   - 底部引入透明页脚组件 `<Footer />`；
   - 视口左下角集成全局主题切换浮动控制器（严格满足 `view-NEXT-rules.md` 规范）。

### 2.3 全局通用透明页脚 (`components/common/Footer.vue`)
新建 [`src/components/common/Footer.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/common/Footer.vue)：
1. **样式与布局**：
   - `bg-transparent`（透明背景，无额外底色侵入）；
   - 水平居中（小屏幕响应式垂直排列），字号 `text-xs text-google-text-muted dark:text-google-text-darkMuted`；
2. **信息展示要素**：
   - 版权声明：`© 2026 Volur, Inc.`；
   - 备案信息：展示工信部备案图标 `/r/static/img/beian.png` + 备案文字 `京ICP备`，点击在新标签页直达 `https://beian.miit.gov.cn/`（不采用 mock 数据）。

### 2.4 主题色体系拓展 (`sys-color-surface-container`)
1. **SCSS 预处理变量**：
   - 在 [`src/styles/variables.scss`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/styles/variables.scss) 中定义 `$sys-color-surface-container: #f0f4f9` 与暗色 `$sys-color-surface-container-dark: #1e1f20`；
2. **CSS 变量动态注入**：
   - 在 [`src/styles/index.scss`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/styles/index.scss) 中为 `:root` 与 `html.dark` 注入 `--sys-color-surface-container`；
3. **Tailwind 配置联动**：
   - 在 [`tailwind.config.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/tailwind.config.ts) 的 `colors` 中拓展 `'sys-color-surface-container': '#f0f4f9'` 与 `'sys-color-surface-container-dark': '#1e1f20'`。

### 2.5 认证子视图精简化
重构 `SignInIdentifierView.vue`、`SignInChallengeView.vue` 与 `SignUpView.vue`：
- 剥离各子视图内冗余的 `min-h-screen` 外部容器、内联页脚与主题切换按钮，组件仅专注渲染 Google 风格的白底/暗灰底居中卡片容器与表单逻辑，彻底消除样式冗余。

---

## 3. 验证与审计结果

1. **类型检查与全量生产打包**：
   - 执行 `npm run build`：`vue-tsc --noEmit` 严格类型检测 0 错误通过；
   - Vite 生产打包耗时 4.67s，生成包含 `AuthLayout` 独立 chunk 的产物，0 错误 0 告警；
2. **开发服务器与路由验证**：
   - `GET /src/views/auth/SignInIdentifierView.vue` $\rightarrow$ `HTTP 200 OK` (MIME: `text/javascript`)；
   - `GET /src/components/layouts/auth/AuthLayout.vue` $\rightarrow$ `HTTP 200 OK` (MIME: `text/javascript`)；
   - `GET /src/components/common/Footer.vue` $\rightarrow$ `HTTP 200 OK` (MIME: `text/javascript`)；
   - `GET /v1/signin/identifier` SPA 路由响应正常；
   - 认证页面背景精确呈现 `#f0f4f9`（暗色模式自适应为 Google Dark），页脚呈现透明并展示版权 `© 2026 Volur, Inc.` 及备案信息。
