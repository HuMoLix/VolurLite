# 施工记录：Favicon 与 Logo 矢量图标归拢至 public/static/svg 并同步更新引用路径

- **变更日期**：2026-09-28
- **涉及模块**：`src/volur-lite-view-NEXT`（`public/static/svg/`、`index.html`、`src/store/modules/app.ts`、`src/views/auth/signin/IdentifierView.vue`）
- **变更类型**：`refactor(assets)` & `style(view-NEXT)`
- **主要目标**：
  1. 将 `public/static/` 根目录下的 SVG 矢量图标（`favicon.svg`、`favicon-dark.svg`、`favicon-long.svg`、`favicon-long-dark.svg`）整体迁移归拢至 `public/static/svg/` 子目录；
  2. 实现 `public/static/` 目录下按文件类型物理划分：`img/` 存放位图资源（如 `beian.png`），`svg/` 存放所有矢量图形资源；
  3. 同步修改 HTML 根标签、全局应用状态管理及视图组件中的资源引用路径至 `/r/static/svg/**`；
  4. 验证 Vite 与 `publicResourceMappingPlugin` 构建输出，确保开发与生产打包产物无缝兼容。

---

## 1. 变更背景与架构动因

此前在 `volur-lite-view-NEXT` 工程中，静态资源目录 `public/static/` 下直接平铺了 4 个 SVG 矢量文件：
- `favicon.svg`
- `favicon-dark.svg`
- `favicon-long.svg`
- `favicon-long-dark.svg`

同时建立了 `img/` 存放 `beian.png`。为了使静态资源目录组织更为规整、职责划分更加单一清晰，需要将所有 SVG 矢量图标统筹归入专用的 `public/static/svg/` 目录下，并确保在全工程中引用的虚拟静态映射路径 `/r/static/svg/...` 保持 100% 严谨对齐。

---

## 2. 核心改动点

### 2.1 物理资源迁移至 `public/static/svg/`
将以下 4 个文件从 `public/static/` 移动至 `public/static/svg/`：
- `favicon.svg` -> `public/static/svg/favicon.svg`（亮色模式浏览器 Favicon）
- `favicon-dark.svg` -> `public/static/svg/favicon-dark.svg`（暗色模式浏览器 Favicon）
- `favicon-long.svg` -> `public/static/svg/favicon-long.svg`（亮色模式品牌长横幅 Logo）
- `favicon-long-dark.svg` -> `public/static/svg/favicon-long-dark.svg`（暗色模式品牌长横幅 Logo）

迁移后，`public/static/` 物理目录结构如下：
```text
public/static/
├── img/
│   └── beian.png
└── svg/
    ├── favicon-dark.svg
    ├── favicon-long-dark.svg
    ├── favicon-long.svg
    └── favicon.svg
```

### 2.2 同步更新项目引用路径

1. **HTML 入口 (`index.html`)**：
   - 更新默认 Favicon 链接为 `/r/static/svg/favicon.svg`：
     ```html
     <link rel="icon" type="image/svg+xml" href="/r/static/svg/favicon.svg" />
     <link rel="alternate icon" href="/r/static/svg/favicon.svg" />
     ```

2. **全局状态管理 (`src/store/modules/app.ts`)**：
   - 更新 `updateFavicon` 动态切换 Favicon 的路径逻辑：
     ```ts
     link.href = mode === 'dark' ? '/r/static/svg/favicon-dark.svg' : '/r/static/svg/favicon.svg'
     ```

3. **账号识别视图 (`src/views/auth/signin/IdentifierView.vue`)**：
   - 更新长条 Logo 的动态绑定路径：
     ```vue
     <img
       :src="appStore.theme === 'dark' ? '/r/static/svg/favicon-long-dark.svg' : '/r/static/svg/favicon-long.svg'"
       alt="Volur Lite NEXT"
       class="h-8 w-auto object-contain"
     />
     ```

---

## 3. 构建与功能验证

1. **类型检查与构建测试**：
   - 执行 `npm run build`（`vue-tsc --noEmit && vite build`）：
   - 构建耗时 4.54s，零错误、零警告通过；
2. **构建产物验证**：
   - 检查 `dist/r/static/` 产物目录，`publicResourceMappingPlugin` 的 `closeBundle` 钩子已成功将 `public/static/svg/` 完整复制并打包至 `dist/r/static/svg/`，包含全部 4 个矢量 SVG 文件；
3. **安全与路径穿透验证**：
   - `/r/static/svg/**` 经由 `publicResourceMappingPlugin` 的多层规范化解码与安全路径校验，符合工程防路径穿越与严格 MIME 类型标准。
