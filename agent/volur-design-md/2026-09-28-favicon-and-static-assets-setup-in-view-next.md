# 施工记录：Volur Lite NEXT Favicon 适配、静态资源目录体系构建与 Logo 重命名

- **变更日期**：2026-09-28
- **涉及模块**：`src/volur-lite-view-NEXT`
- **变更类型**：`feat(assets)` & `style(view-NEXT)`
- **主要目标**：
  1. 完善 `src/volur-lite-view-NEXT` 的 favicon 设置，参考 `src/volur-lite-view` 配置现代 SVG 矢量图标（支持日间/夜间模式）；
  2. 建立 `src/volur-lite-view-NEXT/public/static/img` 静态资源目录；
  3. 将备案图标 `beian.png` 放置在 `public/static/img/` 下；
  4. 将经典长标 `long-logo.svg` 改名为 `favicon-long.svg`（暗色版对应为 `favicon-long-dark.svg`），放置在 `public/static/img/` 及 `public/` 根目录下；
  5. 在 `index.html` 注册 Favicon 标签，并在 Pinia `useAppStore` 中实现切换亮/暗主题时动态同步浏览器标签页 Favicon 图标。

---

## 1. 变更背景与需求说明

在新一代前端工程 `volur-lite-view-NEXT` 初始化后，缺少完整的浏览器标签页 Favicon 与通用静态媒体资源（如公安备案图标、平台长 Logo）。

根据用户明确指令：
1. 完善 Favicon 设置，参考经典前端 `src/volur-lite-view`；
2. 静态图标规范收敛在 `public/static/img/` 目录下；
3. 将备案图标 `beian.png` 迁入 `public/static/img/`；
4. 将原本的 `long-logo.svg`（及暗色变体 `long-logo-dark.svg`）统一更名为 `favicon-long.svg`（与 `favicon-long-dark.svg`）。

---

## 2. 核心改动点

### 2.1 静态资源放置与重命名
- 创建目录：[`src/volur-lite-view-NEXT/public/static/img`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/public/static/img)；
- 资源放置清单：
  - `public/static/img/beian.png`（工信部/公安备案图标）；
  - `public/static/img/favicon-long.svg`（由原 `long-logo.svg` 重命名而来）；
  - `public/static/img/favicon-long-dark.svg`（由原 `long-logo-dark.svg` 重命名而来）；
  - `public/static/img/favicon.svg` & `favicon-dark.svg`（高保真 SVG 矢量图标）；
  - 并在 `public/` 根目录同时提供一份副本，确保直接访问 `/favicon.svg`、`/favicon-dark.svg`、`/beian.png`、`/favicon-long.svg` 亦可无阻断加载。

### 2.2 HTML 根标签与 Store 动态 Favicon 适配
1. **[`index.html`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/index.html)**：
   - 注入 `<link rel="icon" type="image/svg+xml" href="/favicon.svg" />` 与备选图标；
2. **[`src/store/modules/app.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/store/modules/app.ts)**：
   - 在 `applyThemeToDom(mode)` 中新增 `updateFavicon(mode)` 逻辑：
     - 当切换为暗色模式时，自动将 `<link rel="icon">` 的 `href` 动态指向 `/favicon-dark.svg`；
     - 当切换为亮色模式时，自动指向 `/favicon.svg`；
   - 实现了浏览器标签页图标与用户主题设定的毫秒级同步。

### 2.3 业务视图 Logo 与备案呈现
- 在 [`SignInIdentifierView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/v1/auth/SignInIdentifierView.vue) 中应用 `favicon-long.svg` / `favicon-long-dark.svg`；
- 在底部页脚中引入 `/static/img/beian.png` 呈现备案入口链接。

---

## 3. 验证与审计结果

- 执行 `npm run build` 全量编译校验：
  - 打包时长 2.16s，0 错误 0 告警；
  - 静态资源正确纳入构建产物中，无路径缺失。
