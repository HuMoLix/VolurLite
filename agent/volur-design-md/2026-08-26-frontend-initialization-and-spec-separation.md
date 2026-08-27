# 前端工程初始化与前后端规范分离施工记录

**施工日期**：2026-08-26  
**施工模块**：`src/volur-lite-view`、`GEMINI.md`、`agent/volur-design-skills/view-Rules.md`  
**责任主体**：Antigravity AI Agent

---

## 1. 变更背景与需求概述

为了支撑 Volur Lite 平台的完整全栈能力，需在 `src/volur-lite-view` 目录下初始化现代化前端工程，并对项目根协作指令 `GEMINI.md` 进行前后端规范解耦与独立约束，具体要求包括：
1. **前端工程基建**：采用 **Vue 3 + Vite + TypeScript + SCSS** 技术栈，引入 **Naive UI** 作为 UI 组件库，安装并引入 **`vfonts`** 作为全局组件字体库。
2. **架构镜像对齐**：前端目录结构、API 请求层与 TypeScript 实体层全面镜像对齐后端微内核工程（`src/volur-lite-platform`）。
3. **前后端规范分离**：
   - 修改 `GEMINI.md`，明确分离后端规范（遵守 `Rules.md`）与前端规范（遵守 `view-Rules.md`）；
   - 在 `agent/volur-design-skills/view-Rules.md` 中详尽制定前端开发规范，并增加前端 TS API 实体与后端 API 实体（DTO、VO、ApiResponse 及分页 Records）严格 1:1 对齐的强制约束。

---

## 2. 核心改动点与实施细节

### 2.1 前端工程初始化 (`src/volur-lite-view`)
- **工程配置文件**：
  - `package.json`：配置 `vue` (3.5+), `naive-ui` (2.41+), `vfonts` (0.0.3+), `pinia` (3.0+), `vue-router` (4.5+), `axios`, `sass`, `typescript` (5.7+), `vite` (6.2+)；
  - `vite.config.ts`：配置 `@` 路径别名、本地后端代理 `/api` -> `http://localhost:8080`、SCSS 全局变量与 Mixins 现代预处理器注入（`@use "@/styles/variables.scss" as *;`）；
  - `tsconfig.json`：配置严格模式（`strict: true`）、`bundler` 模块解析与路径映射；
  - `index.html` 与 `public/favicon.svg`：配置站点元信息与轻量矢量图标。
- **全局字体库与样式系统**：
  - `main.ts`：显式导入 `vfonts/Lato.css`（无衬线字体）与 `vfonts/FiraCode.css`（代码等宽字体）；
  - `styles/`：构建 `variables.scss`、`mixins.scss`、`reset.scss` 与 `index.scss`，定义色彩体系（主色调 `#6366f1`）与暗色/亮色适配。
- **Naive UI Provider 容器与全局离散 API**：
  - `App.vue`：组织 `NConfigProvider` -> `NLoadingBarProvider` -> `NDialogProvider` -> `NNotificationProvider` -> `NMessageProvider` 层次结构，配合 `NaiveProvider` 将 `$message`、`$dialog`、`$notification`、`$loadingBar` 注入至全局 `window` 实例；
  - `types/naive.d.ts`：补充全局 `window` 对象的 Naive UI 类型声明。
- **网络与状态层**：
  - `utils/request.ts`：封装强类型 Axios 客户端，配置 JWT `Authorization: Bearer` 请求拦截、401 自动无感 `refreshToken` 刷新并发队列，以及业务异常统一 Toast 拦截；
  - `utils/sse.ts`：实现 `SseClient` 支持实时榜单与广播流订阅；
  - `store/`：基于 Pinia Setup 语法编写 `user`、`app`、`notification` 模块。
- **基础页面与路由**：
  - `router/`：配置带有权限守卫（`requiresAuth`）和动态标题的路由表；
  - `views/`：构建 `HomeView.vue`、`LoginView.vue`、`ChallengeListView.vue`、`CompetitionListView.vue` 与 `NotFoundView.vue`；
  - `layout/`：提供 `AppLayout.vue` 与 `AuthLayout.vue`。

### 2.2 实体与 API 镜像对齐后端 (`src/types` & `src/api`)
对照 `src/volur-lite-platform` 模块划分，构建 1:1 对应的 TypeScript 实体层与请求函数层：
- **通用模型 (`types/common/api.ts`)**：
  - `ApiResponse<T>`：镜像 `com.humolix.volur.framework.common.api.ApiResponse`
  - `PageResponse<T>`：镜像 `PageResponse`
  - `CursorResponse<T>`：镜像 `CursorResponse`
  - `SliceResponse<T>`：镜像 `SliceResponse`
- **业务领域模型 (`types/{module}/`) 与 API (`api/{module}/`)**：
  - `auth`：`AuthenticateRequestDTO`、`AuthenticateResponseVO`、`RegisterRequestDTO` 等；
  - `user`：`UserProfileResponseVO`、`AdminUserDetailsVO` 等；
  - `verification`：`CaptchaResponseVO`、`CaptchaTicketVO`、`CaptchaVerifyDTO` 等；
  - `challenge`：`CtfChallengeListVO`、`CtfChallengeDetailVO`、`CtfCategoryVO` 等；
  - `competition`：`CompetitionSummaryVO`、`CompetitionDetailVO`、`CompositeScoreboardVO` 等；
  - `container`：`ChallengeInstanceResponseVO`、`ContainerInstanceDTO` 等；
  - `notification`：`NotificationVO`、`BroadcastMessageVO` 等；
  - `system`：`SystemSettingVO`、`SystemSettingUpdateDTO` 等。

### 2.3 根协作规范分离 (`GEMINI.md`)
- 重新编排根指令结构，明确规定：
  - 后端统一遵循 [`agent/volur-design-skills/Rules.md`](file:///H:/HuMoLix/github/Volur-Lite/agent/volur-design-skills/Rules.md)
  - 前端统一遵循 [`agent/volur-design-skills/view-Rules.md`](file:///H:/HuMoLix/github/Volur-Lite/agent/volur-design-skills/view-Rules.md)
  - 自动化脚本遵循 [`agent/volur-design-scripts/Script-rules.md`](file:///H:/HuMoLix/github/Volur-Lite/agent/volur-design-scripts/Script-rules.md)
- 分别提炼前后端核心规范与 AI 行为指引。

### 2.4 前端设计规范撰写 (`view-Rules.md`)
- 详尽编制前端规范文档，涵盖技术栈基准、目录结构镜像、前后端实体 1:1 对齐、Java-TS 类型映射矩阵、雪花 ID 精度防溢出策略、Axios/SSE 架构、Naive UI Provider 模式与合规检查清单。

---

## 3. 验证与构建测试

在 `src/volur-lite-view` 目录下执行编译与构建：
```powershell
npm run build
```
**验证结果**：
- `vue-tsc --noEmit`：0 类型错误；
- `vite build`：成功生成生产静态资源产物（`dist/`），耗时 ~2.2s。

---

## 4. 影响范围与审计评估

- **代码影响范围**：
  - 初始化新增前端项目 `src/volur-lite-view`（独立前端目录，不影响后端 Maven 结构）；
  - 更新根规则文件 `GEMINI.md` 与前端规则文件 `view-Rules.md`。
- **安全与稳定性**：
  - 前端 Axios 拦截器实现了 Token 过期无感刷新与自动回退登录，杜绝无限 401 循环；
  - 严格 TypeScript 类型约束保障了与后端 API 接口通信的契约稳定性与类型安全。
