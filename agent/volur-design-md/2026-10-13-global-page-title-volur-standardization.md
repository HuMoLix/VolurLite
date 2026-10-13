# 全局网页标题格式统一标准化设计与落地方案

## 1. 变更背景与需求概述

在 `src/volur-lite-view-NEXT` 前端工程中，此前页面标题（`document.title`）在路由流转和语言切换时缺少统一的全局命名规范：
- 管理后台路由（`/admin/*`，如 `/admin/dashboard`）与赛事模块路由（`/competitions` 等）在计算标题时仅使用了纯功能名称（如 `"工作台"`、`"赛事大厅"`），未包含品牌后缀；
- 认证流路由（`/v1/signin/*`、`/v1/signup/*` 等）历史实现中硬编码了各语言版本的后缀（如 `"登录 - Volur 账号"`、`"Sign in - Volur Account"`、`"登入 - Volur 帳號"`）；
- 系统缺乏统一的标题格式化中枢，导致浏览器标签页标题风格割裂、不一致。

针对用户提出的**“现在对系统中所有的地方都需要为 ‘组件名’ - Volur”**规范，本方案在保持代码纯净（零注释）和架构解耦的前提下，进行了全链路标准化重构。

---

## 2. 核心架构设计与改造要点

### 2.1 标题格式化中枢：`formatPageTitle`
新建轻量级工具函数 [`src/utils/title.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/utils/title.ts)：
1. **防空保护**：入参为空或纯空白时兜底展示 `"Volur"`；
2. **幂等防护**：若入参已以 `" - Volur"` 结尾，直接返回，杜绝重复拼接；
3. **历史遗留清洗**：自动匹配并剥离历史遗留的 `" - Volur 账号"`、`" - Volur Account"`、`" - Volur 帳號"` 后缀，归一化收敛至标准格式；
4. **统一规范**：最终统一输出为 `"${pageTitle} - Volur"` 形式（如 `"工作台 - Volur"`、`"赛事大厅 - Volur"`、`"登录 - Volur"`）。

### 2.2 全局双通道接管
在系统运行时，页面标题由两处机制协同驱动，本次均无缝接入 `formatPageTitle`：
1. **全局路由前置守卫**（[`src/router/index.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/router/index.ts)）：
   在初次加载及路由跳转前置拦截期，从 `to.meta.titleKey`（通过 i18n 解析）或 `to.meta.title` 获取原始页面名称，经 `formatPageTitle` 处理后赋予 `document.title`。
2. **根组件多语言响应式监听**（[`src/App.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/App.vue)）：
   在 `watchEffect` 中侦听 `route` 与当前语言切换，当用户在页面视口切换多语言（如中文切英文）时，`t(titleKey)` 动态重新计算，经 `formatPageTitle` 毫秒级刷新 `document.title`（如 `"工作台 - Volur"` 自动切换为 `"Workspace - Volur"`）。

### 2.3 路由元信息与多语言字典全面治理
按照“重构彻底性铁律（严禁妥协保留与旧命名别名）”，对历史遗留数据进行彻底清洗：
1. **路由元信息**（[`src/router/routes/auth.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/router/routes/auth.ts)）：
   所有 fallback 静态 `title` 统一修正为 `"${组件名} - Volur"`（如 `"登录 - Volur"`、`"选择账号 - Volur"`、`"找回密码 - Volur"`）。
2. **多语言词条**（`zh-CN`、`en-US`、`zh-TW`）：
   清洗 `signin.json`、`signup.json`、`recovery.json`、`auth.json` 中的所有 `meta_title_*` 键值，彻底消除 `"账号"` / `"Account"` / `"帳號"` 历史残留，全语种统一对齐：
   - 简体中文：`"登录 - Volur"`、`"选择账号 - Volur"`、`"安全核验 - Volur"` 等；
   - 英文：`"Sign in - Volur"`、`"Choose an account - Volur"`、`"Security verification - Volur"` 等；
   - 繁体中文：`"登入 - Volur"`、`"選擇帳號 - Volur"`、`"安全核驗 - Volur"` 等。
3. **HTML 默认模板**（[`index.html`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/index.html)）：
   默认 `<title>` 统一收敛为 `"Volur"`。

---

## 3. 影响范围与回归验证

- **组件内部 UI 无任何副作用**：管理后台侧边栏（`AdminLayout.vue`）、页面标题与面包屑仍直接消费 `t('admin.dashboard.title')` 等纯净文本，排版不受影响；
- **全站构建验证**：执行 `vue-tsc --noEmit && vite build`，编译打包 100% 通过（exit code: 0），无任何类型报错或打包警告；
- **各主流路由展示效果**：
  - `/admin/dashboard` -> `工作台 - Volur`（英文：`Workspace - Volur`）
  - `/admin/users` -> `用户管理 - Volur`（英文：`User Management - Volur`）
  - `/competitions` -> `赛事大厅 - Volur`
  - `/v1/signin/identifier` -> `登录 - Volur`（英文：`Sign in - Volur`）
  - `/v1/signup` -> `注册 - Volur`（英文：`Sign up - Volur`）
  - `/notFound` -> `404 - 页面未找到 - Volur`
