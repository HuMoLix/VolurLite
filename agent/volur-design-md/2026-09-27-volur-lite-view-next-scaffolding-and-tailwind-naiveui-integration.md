# 施工记录：Volur Lite NEXT 新一代前端脚手架初始化与 Tailwind + Naive UI 架构集成

- **变更日期**：2026-09-27
- **涉及模块**：`src/volur-lite-view-NEXT`（新一代前端工程）
- **变更类型**：`feat(view-NEXT)`
- **主要目标**：构建基于 Vue 3 + TypeScript (ES2026) + SCSS + Tailwind CSS + Naive UI 的新一代前端脚手架体系，实现原子化布局与企业级受控组件协同无冲突运行。

---

## 1. 变更背景与架构动机

当前 `src/volur-lite-view` 为平台现有前端工程。随着新一代业务界面排版、响应式栅格及开发效率诉求的提升，团队决定在 `src/volur-lite-view-NEXT` 开启新一代前端架构演进：
1. **保留核心工程基准**：沿用 Vue 3（Composition API `<script setup lang="ts">`）、TypeScript（ES2026 / ESNext 严格模式）、Pinia Setup Store 规范与现代 SCSS 预处理器支持；
2. **引入 Tailwind CSS**：赋予新一代前端快速原子化样式排版、动态响应式断点控制、Flexbox/Grid 栅格能力；
3. **结合 Naive UI**：保留其高性能、TS 深度整合、多层次主题定制与全套 Provider（Message, Dialog, Notification, LoadingBar）交互支持；
4. **防御 Preflight 冲突**：Tailwind CSS 的全局样式重置机制若不加约束容易冲掉 Naive UI 的基础组件外观（如按钮、边框、下拉菜单等），因此必须建立严密的隔离与优先级策略。

---

## 2. 核心改动点与落地规范

### 2.1 依赖与脚手架环境 (`package.json`)
- 初始化工程为 ESM 模式（`"type": "module"`）；
- 统一集成：
  - `vue` (^3.5.13)、`vue-router` (^4.5.0)、`pinia` (^3.0.1)；
  - `naive-ui` (^2.41.0)、`vfonts` (^0.0.3)、`@vicons/ionicons5`、`@vicons/utils`；
  - `axios` (^1.7.9)；
  - `tailwindcss` (^3.4.17)、`postcss` (^8.5.3)、`autoprefixer` (^10.4.20)；
  - `vite` (^6.2.0)、`sass` (^1.85.0)、`typescript` (^5.7.3)、`vue-tsc` (^2.2.4)。

### 2.2 防冲突与原子化配置 (`tailwind.config.ts` & `postcss.config.js`)
- **`corePlugins: { preflight: false }`**：显式关闭 Tailwind CSS 默认 Preflight 重置，交由 Naive UI 的重置与基础系统接管，杜绝按钮透明、输入框边框消失等经典样式冲突；
- **`darkMode: 'class'`**：与 Volur 全局主题体系（`html.dark`）完美同频，支持 `dark:` 前缀响应式切换；
- **Volur 设计令牌注入**：
  - 将 Volur 核心视觉铁律（日间主要按钮纯黑 `#000000`、正文与表单标签 `#444444`、弱化辅助色 `#666666`、深曜炭黑 `#0b0c10` / `#151821` / 半透明边框）注册为 Tailwind `colors.volur.*` 语义类；
  - 配置 `Lato` 与 `Fira Code` 等宽排版字体。

### 2.3 语言与构建基准 (`vite.config.ts` & `tsconfig.json`)
- **严格遵循 ES2026 / ESNext**：构建目标设为 `esnext`，启用现代 SCSS 编译（`api: 'modern-compiler'`）与全局变量自动注入（`additionalData: @use "@/styles/variables.scss" as *; @use "@/styles/mixins.scss" as *;`）；
- 配置 `@/` 别名映射到 `src/`；
- 配置反向代理至后端 `http://localhost:8080`（支持 `/api` 与 `/ws`）。

### 2.4 主题体系与 Naive UI 全局 Provider (`App.vue` & `main.ts`)
- `index.html` 显式注入 `<meta name="naive-ui-style" />` 占位符，锁定样式注入优先级；
- `App.vue` 构建全套 Provider 链条：`NConfigProvider` -> `NLoadingBarProvider` -> `NDialogProvider` -> `NNotificationProvider` -> `NMessageProvider` -> `NaiveProvider`；
- `themeOverrides` 严格对齐 Volur 核心视觉铁律（日间纯黑主要按钮白字、#444 描述文本；夜间 Cyber Slate 炭黑阶梯反色高对比）；
- `HomeView.vue` 完整示范了：
  - Naive UI 受控组件（`NCard`, `NButton`, `NTag`, `NModal`, `NInput`）；
  - Tailwind CSS 响应式原子栅格（`grid grid-cols-1 md:grid-cols-3 gap-6`）；
  - 严格根据 `view-Rules.md` 铁律将全局多语言/主题切换控制器锚定在视口左下角（`bottom: 20px; left: 20px; z-index: 1000;`）。

---

## 3. 安全隐患修复与兼容性考量

1. **Preflight 冲刷风险清零**：禁用 Tailwind 默认 Preflight 后，Naive UI 组件不受外部重置影响；原子类（如 `p-4`, `flex`, `grid`, `text-sm`）正常输出且各司其职；
2. **主题同步无缝切换**：Pinia `useAppStore` 统一管控 `theme` 状态，持久化至 `localStorage`，并在 DOM 根节点同步操作 `html.classList.toggle('dark')` 与 `html.setAttribute('data-theme', mode)`，确保 Tailwind `dark:` 规则、Naive UI `darkTheme` 与原生 CSS 自定义属性三位一体毫秒级响应。

---

## 4. 影响范围与后续指引

- **影响范围**：新目录 `src/volur-lite-view-NEXT` 内部，完全独立于生产前端 `src/volur-lite-view`，零回归风险。
- **后续指引**：
  - 可在此基础上逐步迁移或重构自研组件库 `volurEx`；
  - 按照后端 Controller 1:1 对齐原则补齐业务 API 请求函数与强类型 VO/DTO 契约定义。
