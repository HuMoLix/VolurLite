# VolurEx 组件库主题 SCSS 落地、深曜暗色模式重构与全页面框架迁移 (2026-09-16)

## 1. 变更背景与设计动机
在平台早期的前端演进中，主题样式与全站骨架布局分散在应用层目录（`src/styles` 与 `src/layout`）中，且原有的夜晚模式（Dark Mode）使用偏蓝海蓝底色（`#121826`）与生硬色块，与 Naive UI 现代暗色体系及网络安全实战竞技场的高级感存在明显割裂。

为了让 Volur 平台的自研前端组件库 **`volurEx`** 成为名副其实、高度内聚、开箱即用的前端资产体系，本次重构完成了两项核心工程任务：
1. **主题 SCSS 体系入驻 `volurEx/styles/`**：保持日间模式（Light Mode）既有视觉规范 100% 不变的前提下，对黑夜模式（Dark Mode）进行全面重构与美学升级，确立 **深曜炭黑 (Obsidian & Cyber Slate)** 设计语言；
2. **全站页面框架迁移至 `volurEx/layouts/`**：将 `src/layout/` 下的四大核心布局骨架（`AdminLayout`、`AppLayout`、`AuthLayout`、`CompetitionLayout`）完整迁移至 `volurEx/layouts/`，并通过 `volurEx/index.ts` 集中统一聚合导出。

---

## 2. 核心架构与美学规范

### 2.1 主题设计令牌架构 (`volurEx/styles/theme.scss`)
```
volurEx/styles/
├── theme.scss         # 日间模式 (经典保持) 与 黑夜模式 (深曜炭黑重构) CSS 自定义属性
├── variables.scss     # SCSS 变量、断点尺寸、动画时长与核心色值
├── mixins.scss        # 毛玻璃表面 (glass-surface)、暗亮模式选择器、自定义滚动条等 Mixins
└── index.scss         # 样式主入口与全量转发
```

#### 2.1.1 日间模式（Light Mode）保持平台既有规范
- **背景画布**：`--v-bg-base: #fafafa`，卡片表面 `--v-bg-card: #ffffff`；
- **排版标准**：核心遵循 Volur 铁律，正文描述文本与表单标签统一使用 `--v-text-desc: #444444`；
- **主要操作按钮**：纯黑背景 `--v-btn-primary-bg: #000000`，纯白文字 `--v-btn-primary-text: #ffffff`；
- **边框与阴影**：`--v-border-base: #e5e4e7`，温润轻量阴影。

#### 2.1.2 黑夜模式（Dark Mode）全新美学重构
- **深曜炭黑基底 (Obsidian Canvas)**：
  - 基底画布 `--v-bg-base: #0b0c10`（消除廉价蓝灰反光，提供极致深邃视觉底色）；
  - 侧边栏专属阶梯沉浸色 `--v-bg-sidebar: #0f1117`；
  - 精细抬升卡片与面板 `--v-bg-card: #151821`；
  - 模态窗/浮层 `--v-bg-elevated: #1b1e2a`；
  - 代码块/Flag 专属高对比容器 `--v-bg-code: #08090d`；
- **零眩光高对比度排版 (Anti-Glare Typography)**：
  - 正文白 `--v-text-primary: #f8fafc`；
  - 描述与副文本 `--v-text-desc: #94a3b8` (Slate-400)，长久阅读不刺眼；
  - 弱说明文字 `--v-text-muted: #64748b`；
- **半透明光泽边框 (Hairline Alpha Borders)**：
  - `--v-border-base: rgba(255, 255, 255, 0.09)`，自适应融入不同表面高程；
- **反色纯白主按钮 (High-Contrast Inverted Action Button)**：
  - 底色 `--v-btn-primary-bg: #ffffff`，文字 `--v-btn-primary-text: #0b0c10`，悬停 `--v-btn-primary-hover: #e2e8f0`；
- **网络安全语义色 (Cyber Security Accents)**：
  - 主色科技靛蓝 `#818cf8`、成功翡翠绿 `#10b981`、告警琥珀橙 `#f59e0b`、破坏性珊瑚红 `#f87171`、电光青 `#38bdf8`。

---

### 2.2 全页面框架迁移 (`volurEx/layouts/`)
组件库中提供标准页面框架，通过 `src/public/volurEx/index.ts` 集中向外暴露：
1. [AdminLayout.vue](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/AdminLayout.vue)：管理控制台框架，集成 [VolurSidebar](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/components/VolurSidebar.vue)、面包屑导航、多语言切换、暗黑模式切换、用户菜单及实时异步任务 SSE 事件通知；
2. [AppLayout.vue](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/AppLayout.vue)：前台用户端标准布局，具备动态面包屑联动、站内通知中心、竞赛直达入口与全站页脚；
3. [AuthLayout.vue](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/AuthLayout.vue)：登录、注册、密码找回认证居中框架，搭载自适应品牌矢量 Logo 与左下角全局语言/主题悬浮控制器；
4. [CompetitionLayout.vue](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/CompetitionLayout.vue)：网络安全实战竞技场沉浸式顶栏框架，包含比赛退场按钮、赛项类型/状态徽章、毫秒级倒计时组件与战队标识。

---

## 3. 改动点汇总

1. **新建 `volurEx/styles/` 样式子系统**：
   - `src/volur-lite-view/src/public/volurEx/styles/theme.scss`
   - `src/volur-lite-view/src/public/volurEx/styles/variables.scss`
   - `src/volur-lite-view/src/public/volurEx/styles/mixins.scss`
   - `src/volur-lite-view/src/public/volurEx/styles/index.scss`
2. **重构全局主题引用与 Naive UI 覆写**：
   - [src/styles/theme.scss](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/styles/theme.scss)：改为统一转发自 `@/public/volurEx/styles/theme.scss`；
   - [App.vue](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/App.vue)：在 `themeOverrides` 中全面匹配新暗色炭黑调色盘（`baseColor`、`cardColor`、`modalColor`、`borderColor`、`Button`、`Card`、`Layout` 等）；
3. **新建 `volurEx/layouts/` 页面框架**：
   - `src/volur-lite-view/src/public/volurEx/layouts/AdminLayout.vue`
   - `src/volur-lite-view/src/public/volurEx/layouts/AppLayout.vue`
   - `src/volur-lite-view/src/public/volurEx/layouts/AuthLayout.vue`
   - `src/volur-lite-view/src/public/volurEx/layouts/CompetitionLayout.vue`
   - `src/volur-lite-view/src/public/volurEx/layouts/index.ts`
4. **聚合向外导出与路由/旧路径兼容**：
   - [volurEx/index.ts](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/index.ts)：导出 `components` 与 `layouts`；
   - [router/routes.ts](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/router/routes.ts)：直接从 `@/public/volurEx` 引入 `AppLayout`、`AuthLayout`、`AdminLayout`、`CompetitionLayout`；
   - `src/layout/*.vue`：保留向前兼容包装器，委托转发至组件库。
5. **规范文件同步**：
   - 更新 [GEMINI.md](file:///Users/humolix/Documents/Dev/Volur-Lite/GEMINI.md) 与 [view-Rules.md](file:///Users/humolix/Documents/Dev/Volur-Lite/agent/volur-design-skills/view-Rules.md)，确立 `volurEx` 的主题体系与 `layouts` 规范。

---

## 4. 影响与安全性评估

- **TypeScript 类型与构建**：执行 `npm run build`（`vue-tsc --noEmit && vite build`）顺利通过，产物无报错、0 告警；
- **视觉连贯性**：日间模式 100% 保持 Volur 原色，黑夜模式视觉质感获得飞跃式提升，与 Naive UI 暗色体系完全融为一体。
