# 施工记录：提取认证主卡片容器至 AuthLayout 并实现 Google Accounts 风格响应式左右/上下布局

- **变更日期**：2026-09-28
- **涉及模块**：
  - `src/volur-lite-view-NEXT/src/components/layouts/auth/AuthLayout.vue`
  - `src/volur-lite-view-NEXT/src/views/auth/signin/IdentifierView.vue`
  - `src/volur-lite-view-NEXT/src/views/auth/signin/ChallengeView.vue`
  - `src/volur-lite-view-NEXT/src/views/auth/signup/SignUpView.vue`
  - `src/volur-lite-view-NEXT/src/router/routes/auth.ts`
- **变更类型**：`feat(auth-layout)` & `refactor(responsive)`

---

## 1. 变更背景与架构动因

1. **容器职责收敛与解耦**：将各认证子视图中分散维护的卡片外框、圆角、阴影与标题排版彻底上提至布局容器 [`AuthLayout.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/layouts/auth/AuthLayout.vue)，使子视图仅专注表单业务与状态交互；
2. **对齐 accounts.google.com 现代视觉体系**：
   - 页面背景维持 Material 3 的 `sys-color-surface-container`（`#f0f4f9`，暗色模式为 `#1e1f20`）；
   - 卡片框使用纯白 `#ffffff`（暗色模式自适应为 `#292a2d`），并带有 28px 圆角与柔和边框；
   - 电脑端与手机端通过显式 `@media` 响应式查询区分显示结构：
     - **电脑端 (PC)**：左右分栏显示。左侧展示平台 Logo、主标题（如“登录”）及副标题（“使用您的 Volur 账号”）；右侧展示表单输入框、辅助链接与底部动作栏（“创建账号”、“下一步”）；
     - **手机端 (Mobile)**：上下垂直堆叠显示。上方展示 Logo 与标题信息，下方展示表单与操作按钮；
3. **卡片下方 Google 风格操作栏**：
   - 卡片框下方左侧提供多语言文字切换按钮（支持简体中文、繁体中文、英文、日文切换菜单）；
   - 右侧提供“帮助”、“隐私权”、“条款”直达链接；
   - 视口最底部保留平台级全局透明页脚（版权 `© 2026 Volur, Inc.` 及工信部备案链接）。

---

## 2. 核心改动点

### 2.1 `AuthLayout.vue` 结构重构
1. **主卡片布局 (`.auth-card`)**：
   - 默认（移动端）采用 `display: flex; flex-direction: column; max-width: 448px;`；
   - 使用 `@media (min-width: 840px)` 切换为 `flex-direction: row; max-width: 960px; min-height: 440px;`；
   - 内部拆分为 `.auth-card__left` 与 `.auth-card__right`：
     - 左栏：承载 Logo、`h1` 主标题、`p` 副标题及 `<div id="auth-header-extra" />` 动态传送锚点；
     - 右栏：承载 `<router-view>` 动态挂载的表单内容；
2. **卡片下快捷辅助栏 (`.auth-subbar`)**：
   - 宽度与卡片严格响应式对齐（移动端 `448px`，电脑端 `960px`）；
   - 左侧集成 Naive UI `<n-dropdown>` 语言切换器；
   - 右侧包含“帮助”、“隐私权”、“条款”超链接；
3. **暗色模式无缝适配**：
   - 卡片背景由 `#ffffff` 自动切至 `var(--v-bg-card, #292a2d)`，边框切至 `var(--v-border-base, #3c4043)`。

### 2.2 视图组件纯粹化 (`IdentifierView.vue` / `ChallengeView.vue` / `SignUpView.vue`)
1. **[`IdentifierView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/IdentifierView.vue)**：
   - 移除外层卡片框、Logo 及标题元素，整体作为右栏表单内容注入；
   - 顶部为输入框、错误提示与“忘记了电子邮件地址？”，底部两端对齐“创建账号”与“下一步”按钮；
2. **[`ChallengeView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/ChallengeView.vue)**：
   - 使用 `<Teleport to="#auth-header-extra">` 将当前已识别的账号芯片标签传送至左侧标题下方；
   - 右侧承载密码表单与“返回”、“登录”操作；
3. **[`SignUpView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signup/SignUpView.vue)**：
   - 右侧承载账号名、邮箱、密码及确认密码表单，底部对齐“改用现有账号登录”与“注册”。

### 2.3 路由元信息绑定 (`auth.ts`)
在 [`src/router/routes/auth.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/router/routes/auth.ts) 中显式为各认证路由注入 `cardTitle` 与 `cardSubtitle`，由 `AuthLayout.vue` 统一响应式计算。

---

## 3. 验证与审计结果

1. **类型检查与生产打包**：
   - 执行 `npm run build`，`vue-tsc --noEmit` 严格类型检测与 Vite 构建 **0 错误 0 告警通过**，打包耗时 4.46s；
2. **开发服务器与接口验证**：
   - `GET /src/components/layouts/auth/AuthLayout.vue` $\rightarrow$ `HTTP 200 OK` (MIME: `text/javascript`)；
   - `GET /src/views/auth/signin/IdentifierView.vue` $\rightarrow$ `HTTP 200 OK` (MIME: `text/javascript`)；
   - `GET /src/views/auth/signin/ChallengeView.vue` $\rightarrow$ `HTTP 200 OK` (MIME: `text/javascript`)；
   - `GET /src/views/auth/signup/SignUpView.vue` $\rightarrow$ `HTTP 200 OK` (MIME: `text/javascript`)；
3. **视觉布局实测**：
   - 电脑端视口（$\ge 840\text{px}$）呈现左右分栏，左侧为 Logo 与主副标题，右侧为表单与操作按钮；
   - 手机端视口（$< 840\text{px}$）自适应为上下结构，卡片居中；
   - 卡片下方左侧语言选择按钮与右侧链接对齐展示，卡片颜色为 `#ffffff`。
