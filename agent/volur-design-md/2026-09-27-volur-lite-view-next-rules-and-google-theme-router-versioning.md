# 施工记录：Volur Lite NEXT 规范制定、Google 风格主题色彩重构与版本化路由集成

- **变更日期**：2026-09-27
- **涉及模块**：`GEMINI.md`、`agent/volur-design-skills/view-NEXT-rules.md`、`src/volur-lite-view-NEXT`
- **变更类型**：`feat(view-NEXT)` & `docs(rules)`
- **主要目标**：
  1. 更新项目根协作规范 `GEMINI.md`，明确新一代前端 `src/volur-lite-view-NEXT` 必须严格遵循 `agent/volur-design-skills/view-NEXT-rules.md`；
  2. 制定 `view-NEXT-rules.md` 规范文档，将主题色彩全面对齐 Google Material Design 3 与 Google Workspace 色彩体系，并强制实行前端 Router 路径版本控制规范（如 `/v1/signin/identifier` 等）；
  3. 全面重构 `src/volur-lite-view-NEXT` 现有主题变量与路由架构，落地实现 `/v1/signin/identifier` 及 `/v1/signin/challenge` 视图。

---

## 1. 变更背景与架构动因

随着 Volur Lite 开启新一代前端演进（Vue 3 + TypeScript + SCSS + Tailwind CSS + Naive UI），为了统一开发范式并防止架构退化，需要制定针对新一代前端的独立规范体系：
1. **规范解耦与明确边界**：经典前端 `src/volur-lite-view` 与新前端 `src/volur-lite-view-NEXT` 架构存在差异（NEXT 引入了 Tailwind CSS 且对视觉与路由做出了新定义），因此在 `GEMINI.md` 中做明确区分；
2. **Google 风格色彩重构**：平台界面从以往纯硬黑硬白风格转向 Google Material Design 3 / Workspace 色彩体系，以提升界面的亲和力、专业感与护眼体验；
3. **路由版本控制**：借鉴现代大型 Web 应用的演进机制，前端路由全面引入版本前缀（默认 `/v1/`），登录等认证流程细分为阶段式子路由（如 `/v1/signin/identifier` 输入账号、`/v1/signin/challenge` 安全凭证挑战），避免大单页臃肿并利于渐进式重构。

---

## 2. 核心改动点

### 2.1 根规范与技能文档更新
1. **`GEMINI.md`**：
   - 导言区明确区分经典前端（遵守 `view-Rules.md`）与新一代前端（遵守 `view-NEXT-rules.md`）；
   - 新增 `1.3 新一代前端架构与编码规范 (Next-gen Frontend: volur-lite-view-NEXT)` 章节；
   - 导航区与 AI 行为指引增加对应要求；
   - Git Scope 补充 `view-NEXT`。
2. **`agent/volur-design-skills/view-NEXT-rules.md`**：
   - 制定了完整的前端规范：ES2026 基准、Pinia Setup Store、Tailwind Preflight 隔离；
   - 详细定义了 **Google Material Theme System**：
     - 主品牌色：Google Blue（亮色 `#1a73e8` / 暗色 `#8ab4f8`）；
     - 语义辅色：Google Red (`#ea4335` / `#f28b82`)、Google Green (`#34a853` / `#81c995`)、Google Yellow (`#fbbc04` / `#fdd663`)、Google Cyan (`#24c1e0` / `#78d9ec`)；
     - 表面与文字阶梯：亮色 Google Gray 50 (`#f8f9fa`) 底色、白卡片、Google Gray 900 (`#202124`) 正文（杜绝刺眼全黑 `#000`）、`#3c4043` / `#5f6368` 描述文字；暗色 Google Dark 900 (`#202124`)、Dark 800 (`#292a2d`) 卡片、Dark 700 (`#303134`) 浮层；
   - 确立了 **前端 Router 版本控制规范**（对照表与反模式警示，登录阶段必须使用 `/v1/signin/identifier` 等）。

### 2.2 新一代工程代码落地 (`src/volur-lite-view-NEXT`)
1. **主题变量体系**：
   - 更新 `src/styles/variables.scss` 与 `src/styles/index.scss`，注入完整 Google Material CSS 变量与 SCSS 变量；
   - 更新 `tailwind.config.ts`，扩展 `colors.google.*` 语义化原子类体系；
   - 更新 `src/App.vue`，使 Naive UI `themeOverrides` 与 Google 主题变量 100% 呼应联动。
2. **版本化路由与页面视图**：
   - 更新 `src/router/index.ts`，根路径 `/` 默认重定向至 `/v1/signin/identifier`；
   - 新建 `src/views/v1/auth/SignInIdentifierView.vue`：Google 风格第一步账号输入页；
   - 新建 `src/views/v1/auth/SignInChallengeView.vue`：Google 风格第二步凭据验证挑战页；
   - 迁移并重构 `src/views/v1/home/HomeView.vue`：展现 Google 配色体系与组件混编。

---

## 3. 验证与审计结果

- 运行 `npm run build` 执行完整编译：
  ```
  > vue-tsc --noEmit && vite build
  ✓ 2705 modules transformed.
  dist/assets/SignInIdentifierView-CH-qCan-.js     3.94 kB
  dist/assets/SignInChallengeView-kpRTEvXz.js      3.81 kB
  dist/assets/HomeView-CNOtMneh.js                17.22 kB
  ✓ built in 4.45s
  ```
- 0 编译错误，0 类型告警。
