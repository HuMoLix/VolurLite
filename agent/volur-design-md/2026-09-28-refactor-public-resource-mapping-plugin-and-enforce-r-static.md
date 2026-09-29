# 施工记录：提取 publicResourceMappingPlugin 插件至独立模块并统一 /r/static/ 严格路径规范

- **变更日期**：2026-09-28
- **涉及模块**：`src/volur-lite-view-NEXT/plugins/publicResourceMapping.ts`、`src/volur-lite-view-NEXT/vite.config.ts`、`src/volur-lite-view-NEXT/tsconfig.node.json`、`src/volur-lite-view-NEXT/public`
- **变更类型**：`refactor(build)` & `feat(security)`
- **主要目标**：
  1. 将 `publicResourceMappingPlugin` 从 `vite.config.ts` 抽离，在 `plugins/publicResourceMapping.ts` 中维护，保持 Vite 根配置精简纯粹；
  2. 彻底删除向 `public/` 根目录查找的 fallback 宽松逻辑，强制统一所有静态资源必须通过 `/r/static/xxx` 访问；
  3. 清理 `public/` 根目录下遗留的冗余文件，使 `public/` 仅收敛 `static/` 结构；
  4. 保持严格的防路径穿越（Path Traversal）安全拦截体系。

---

## 1. 变更背景与架构动因

此前在 `vite.config.ts` 中内嵌了临时映射函数，为了容纳历史别名保留了“若在 `static/` 下找不到则 fallback 到根目录”的容错逻辑。

为了满足更高标准的架构解耦与静态资源访问严格约束：
1. **单一职责解耦**：将 Vite 扩展逻辑收敛至独立目录 `src/volur-lite-view-NEXT/plugins/`，便于工程化维护；
2. **规范统一与杜绝路径歧义**：全系统所有前端静态资源严格规范为 `/r/static/**`，消除向 `public/` 根目录 fallback 的模糊分支；
3. **彻底杜绝根目录别名副本冗余**：清理 `public/` 根目录散落的同名文件，使物理结构（`public/static/**`）与虚拟路由（`/r/static/**`）形成精确严格的单一映射源。

---

## 2. 核心改动点

### 2.1 独立插件模块化 (`plugins/publicResourceMapping.ts`)
新建 [`src/volur-lite-view-NEXT/plugins/publicResourceMapping.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/plugins/publicResourceMapping.ts)：
1. **严格 URL 前缀核验**：
   - 仅拦截严格以 `/r/static/` 开头的请求；
   - 截取 `/r/static/` 后的子路径，直接映射到物理 `public/static/` 目录；
   - 彻底删除向 `public/` 根目录回退寻找的逻辑；
2. **Path Traversal 深度防御**：
   - 多重 URL 递归解码，消除双重编码（`%252e%252e`）等变形绕过；
   - 拒绝空字节 `\0` 与换行回车等截断字符；
   - 拆解路径段并阻断任何包含 `..` 或 `.` 的片段；
   - 规范化绝对目标路径并强制匹配 `staticDir` 前缀，防止目录跳跃逃逸；
3. **Rollup 外部资源标识与产物输出**：
   - `resolveId` 识别 `/r/static/` 为外部资源，杜绝 Rollup 解析报错；
   - `load` 返回 ESM 默认导出，支持作为模块导入；
   - `closeBundle` 自动将 `public/static` 复制至 `dist/r/static`。
4. **MIME 严格类型冲突彻底修复 (Strict MIME Type Checking)**：
   - **成因排查**：在 Vue SFC 中使用 `<img src="/r/static/img/beian.png">` 时，Vite Vue 插件默认会将以 `/` 开头的绝对静态路径转换为 ES 模块导入脚本（`import _imports_0 from '/r/static/img/beian.png'`）。浏览器在作为 module script 加载该资源时，若服务端返回二进制 `image/png` 会触发浏览器严格安全报错 `Failed to load module script: Expected a JavaScript-or-Wasm module script but the server responded with a MIME type of "image/png"` 并导致动态路由组件 `SignInIdentifierView.vue` 加载中断；
   - **双重协同解决方案**：
     1. 在 `vite.config.ts` 中针对 `vue` 插件配置 `template: { transformAssetUrls: { includeAbsolute: false } }`，防止绝对路径被强制重写为 JS 模块导入，保持原生 HTML 字符串属性；
     2. 在 `publicResourceMappingPlugin` 的 connect 中间件中增加脚本请求识别（`Sec-Fetch-Dest: script` 或 `?import`），针对任何被浏览器误当做 JS 模块拉取的静态资源，返回合法的 `application/javascript; charset=utf-8`（内容为 `export default "/r/static/..."`），彻底消灭 MIME 类型冲突。

### 2.2 构建配置简化 (`vite.config.ts` & `tsconfig.node.json`)
1. **[`vite.config.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/vite.config.ts)**：
   - 移除内联函数，导入 `import { publicResourceMappingPlugin } from './plugins/publicResourceMapping'`；
   - 增加 `vue({ template: { transformAssetUrls: { includeAbsolute: false } } })`。
2. **[`tsconfig.node.json`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/tsconfig.node.json)**：
   - `include` 补充 `"plugins/**/*.ts"`，确保 TypeScript 严格类型检查覆盖插件。

### 2.3 物理目录收敛 (`public/`)
- 删除了 `public/` 根目录下原有的冗余别名文件（`favicon.svg`, `favicon-dark.svg`, `favicon-long.svg`, `favicon-long-dark.svg`, `beian.png`）；
- 清理了 `public/static/img/` 下误拷贝的冗余 favicon 副本；
- `public/` 目前仅保留纯净的 `static/` 目录树（包含 `static/favicon.svg`、`static/img/beian.png` 等）。

---

## 3. 验证与审计结果

- 运行 `npm run build` 全量生产打包验证：
  - `vue-tsc --noEmit` 严格类型检查通过；
  - 生产打包耗时 2.33s，生成纯净的 `dist/r/static` 产物结构，0 错误 0 告警；
- 开发服务器端点验证：
  - `GET /src/views/v1/auth/SignInIdentifierView.vue` 正常返回 `200 OK` (MIME: `text/javascript`)；
  - `GET /r/static/img/beian.png` 正常返回 `200 OK` (MIME: `image/png`)；
  - `GET /r/static/favicon.svg` 正常返回 `200 OK` (MIME: `image/svg+xml`)；
  - 针对 `GET /r/static/../../package.json` 及 `%2e%2e` 的路径穿越请求，插件精确阻断并返回 `403 Forbidden: Path traversal attempt blocked`。

