# Volur 视觉设计规范与左下角全局控制器施工记录

**施工日期**：2026-08-26  
**施工模块**：`agent/volur-design-skills/view-Rules.md`、`GEMINI.md`、`src/volur-lite-view/src/styles/variables.scss`、`src/volur-lite-view/src/App.vue`、`src/volur-lite-view/src/layout/AuthLayout.vue`、`src/volur-lite-view/src/layout/AppLayout.vue`、`src/volur-lite-view/src/components/auth/AuthForm.vue`  
**责任主体**：Antigravity AI Agent

---

## 1. 变更背景与规范目标

根据 Volur 原型设计与极简极客视觉设计准则，在 [`view-Rules.md`](file:///H:/HuMoLix/github/Volur-Lite/agent/volur-design-skills/view-Rules.md) 和前端工程中确立并固化三大核心视觉铁律：
1. **主要操作按钮规范**：参考 Volur 项目，主操作按钮背景为 `#000000`，字体颜色为 `#ffffff`（在暗色模式下反色或高反差黑白配色），圆角为 `8px`；
2. **描述文本颜色规范**：绝大多数次级文本、副标题、表单项 Label、说明文字的颜色统一使用 `#444` (`#444444`)，杜绝大面积使用纯黑 `#000000` 作为描述字体；
3. **全局控制器定位规范**：中英文多语言切换与夜间/亮色主题切换按钮必须固定在页面视口**左下角**（`position: fixed; bottom: 20px; left: 20px; z-index: 1000;`）。

---

## 2. 核心改动点与实施细节

### 2.1 规范文档固化 ([`view-Rules.md`](file:///H:/HuMoLix/github/Volur-Lite/agent/volur-design-skills/view-Rules.md) & [`GEMINI.md`](file:///H:/HuMoLix/github/Volur-Lite/GEMINI.md))
- 在 `view-Rules.md` **第 10 节「视觉设计与 SCSS 样式编写规范」** 中明确设立三大视觉铁律；
- 在 `view-Rules.md` **第 12 节「规范合规速查清单」** 中加入针对按钮 `#000000` / `#ffffff`、描述字体 `#444`、左下角固定控制器的合规核验项；
- 在根指令 `GEMINI.md` 的前端核心规范准则中同步强化此项要求。

### 2.2 样式变量与主题适配 ([`variables.scss`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/styles/variables.scss) & [`App.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/App.vue))
- 在 `variables.scss` 中定义 `$btn-bg-primary: #000000;`、`$btn-text-primary: #ffffff;`、`$text-desc: #444444;` 等设计令牌；
- 在 `App.vue` 中配置 Naive UI `themeOverrides`：
  - `Button.colorPrimary = '#000000'`，`Button.textColorPrimary = '#ffffff'`；
  - `common.textColor2 = '#444444'`，`Form.labelTextColor = '#444444'`；
  - 针对暗色模式与亮色模式提供响应式配色。

### 2.3 布局与组件改造
- **[`AuthLayout.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/layout/AuthLayout.vue)**：移除顶栏冗余切换按钮，将多语言与暗色主题切换按钮重构为悬浮在左下角（`bottom: 20px; left: 20px; z-index: 1000;`）的圆形浮动控制器组；
- **[`AuthForm.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/components/auth/AuthForm.vue)**：副标题、表单项 Label、忘记密码与底部提示文字统一收敛至 `#444`（`$text-desc`），提交按钮高度设为 `48px` 并适配 `#000000` / `#ffffff` 配色。

---

## 3. 验证与构建测试

在 `src/volur-lite-view` 目录下执行全量构建：
```powershell
npm run build
```
**验证结果**：
- `vue-tsc --noEmit`：0 错误；
- `vite build`：生产环境构建顺利通过（耗时 ~5.34s）。

---

## 4. 影响范围与审计评估

- **改动文件**：
  - `agent/volur-design-skills/view-Rules.md`
  - `GEMINI.md`
  - `src/volur-lite-view/src/styles/variables.scss`
  - `src/volur-lite-view/src/App.vue`
  - `src/volur-lite-view/src/layout/AuthLayout.vue`
  - `src/volur-lite-view/src/layout/AppLayout.vue`
  - `src/volur-lite-view/src/components/auth/AuthForm.vue`
- **设计体验**：
  - 完美契合 Volur 原型高水准极客设计风格，视觉层级分明，操作入口统一收敛。
