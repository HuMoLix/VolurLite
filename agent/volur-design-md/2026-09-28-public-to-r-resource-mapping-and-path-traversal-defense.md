# 施工记录：Volur Lite NEXT 静态资源 /r/ 路由映射与防路径穿越 (Path Traversal) 安全架构实现

- **变更日期**：2026-09-28
- **涉及模块**：`src/volur-lite-view-NEXT/vite.config.ts`、`src/volur-lite-view-NEXT/index.html`、`src/volur-lite-view-NEXT/src/store/modules/app.ts`、`src/volur-lite-view-NEXT/src/views/v1/auth/SignInIdentifierView.vue`、`agent/volur-design-skills/view-NEXT-rules.md`
- **变更类型**：`feat(security)` & `feat(assets)`
- **主要目标**：
  1. 取消生硬的根目录 alias 暴露，将 `public/` 静态资源目录统一映射到 `/r/` 虚拟路由路径下（如访问 `public/favicon.svg` 对应 `/r/static/favicon.svg`）；
  2. 实现严密的防路径穿越（Path Traversal / CWE-22）安全拦截机制，杜绝使用 `../`、URL 编码、双重编码、空字节（Null Byte）等方式逃逸 `public/` 根目录；
  3. 配置 Vite 构建钩子与 Rollup 模块解析扩展，开发模式中间件与生产构建 `dist/r/` 产物无缝闭环。

---

## 1. 变更背景与安全考量

此前静态资源直接暴露在开发服务器或静态服务器根路径，若简单以别名访问缺乏隔离控制。

为提供规范、收敛的静态资源路径体系：
1. 统一将前端静态文件引用前缀定义为 `/r/`（例如 `/r/static/favicon.svg`、`/r/static/img/beian.png`）；
2. **安全防护铁律**：静态映射层极易遭受路径穿越（Path Traversal）漏洞攻击，若攻击者构造 `/r/../package.json`、`/r/%2e%2e/`、`/r/%252e%252e/` 等请求可能非法读取项目敏感源码或配置文件，必须内置彻底防御机制。

---

## 2. 核心改动点与安全实现

### 2.1 Vite 插件实现 (`publicResourceMappingPlugin`)
在 [`vite.config.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/vite.config.ts) 中自定义封装并挂载 `publicResourceMappingPlugin`：
1. **多重路径解码与反绕过**：
   - 采用循环 `decodeURIComponent` 消除嵌套与多重 URL 编码绕过攻击；
   - 正则检测并直接拒收含 `\0`（空字节截断）、`\r`、`\n` 的恶意路径；
2. **段级审查与规范化校验**：
   - 将所有反斜杠 `\` 统一折叠为标准正斜杠 `/`；
   - 拆分路径段并阻断任何包含 `..` 或 `.` 目录跳跃的段落；
   - 利用 `path.resolve` 与 `path.normalize` 计算绝对目标物理路径，强制核验 `targetNormalized` 必须严格以 `publicDir + path.sep` 开头；
   - 任何越界尝试直接返回 `403 Forbidden: Path traversal attempt blocked`；
3. **Rollup 外部资源标识 (`resolveId`)**：
   - 拦截以 `/r/` 开头的模块请求并标记为 `{ id, external: true }`，避免 Rollup 在解析 Vue 单文件组件中的 `<img src="/r/static/..." />` 时误作为 JavaScript 模块解析导致打包失败；
4. **开发服务器与预览服务器中间件 (`configureServer` & `configurePreviewServer`)**：
   - 正确识别并返回 SVG、PNG、WebP、ICO 等媒体类型的 `Content-Type` 与 `Cache-Control`；
5. **生产打包自动输出 (`closeBundle`)**：
   - 在构建完成时自动将 `public/` 静态资源目录复制到 `dist/r/` 下，使生产部署（如 Nginx 反向代理）直接命中静态资源。

### 2.2 前端资源引用重构
1. **[`index.html`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/index.html)**：
   - Favicon 引用改为 `/r/static/favicon.svg`；
2. **[`src/store/modules/app.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/store/modules/app.ts)**：
   - `updateFavicon` 动态切换 `/r/static/favicon-dark.svg` 与 `/r/static/favicon.svg`；
3. **[`SignInIdentifierView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/v1/auth/SignInIdentifierView.vue)**：
   - 平台 Logo 切换为 `/r/static/favicon-long.svg` 与 `/r/static/favicon-long-dark.svg`；
   - 备案图标指向 `/r/static/img/beian.png`。

---

## 3. 安全验证与测试

执行自动化防路径穿越攻击用例测试：
- `../package.json`：**拦截成功 (safe = false)**；
- `..%2fpackage.json`：**拦截成功 (safe = false)**；
- `%2e%2e%2fpackage.json`：**拦截成功 (safe = false)**；
- `%252e%252e%252fpackage.json`：**拦截成功 (safe = false)**；
- `static/../../package.json`：**拦截成功 (safe = false)**；
- `..\\..\\package.json`：**拦截成功 (safe = false)**；
- `favicon.svg\0.png`：**拦截成功 (safe = false)**；
- `favicon.svg%00.png`：**拦截成功 (safe = false)**；
- 合法资源路径 `static/favicon.svg`、`static/img/beian.png`：**正常访问通过 (safe = true)**。

运行 `npm run build` 全量生产打包构建通过（耗时 4.91s，0 错误）。
