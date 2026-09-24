# 施工与审计记录：i18n 多语言按需分片与自适应验证码骨架屏抖动消除

**文档版本**：v1.0.0  
**施工日期**：2026-09-23  
**执行角色**：Antigravity Agent  
**涉及工程**：`src/volur-lite-view` (Vue 3 + Vite + TypeScript)

---

## 1. 变更背景与问题归因

### 1.1 登录页语言资源全量无节制拉取问题
在通过 Chrome DevTools MCP 对 `https://test-platform.volurctf.com/auth/login` 进行网络拓扑与性能关键路径（Critical Rendering Path）追踪时，发现：
- 浏览器在应用初始化启动（Bootstrap）及访问登录页时，触发了多达 19 个 `src/locales/zh_CN/*.json` 请求；
- 其中包含多达 13 个 `admin/*.json`（如 `risk.json`、`clusters.json`、`system.json` 等管理后台专属词条），以及 `competition.json`、`profile.json` 等非当前路由相关的词条；
- **根因**：原 `src/i18n.ts` 中 `loadLocaleMessagesAsync(locale)` 使用 `import.meta.glob('./locales/**/*.json')`，通过 `matchingPaths.filter(path => path.includes(prefix))` 将该语言目录下的全部文件通过 `Promise.all` 一次性全量发起 HTTP 请求并在首屏阻塞 `app.mount()`。在 Vite 开发模式（无强缓存与 HTTP 分包打包）下造成了 19 个串并发 HTTP RTT，阻断主渲染线程长达 3 秒；在生产构建（Rollup Chunking）下也会导致首屏必须下载全部语言 Chunk。

### 1.2 登录页“验证码”标签先显示、后跳出按钮的视觉塌陷（CLS）问题
在登录页挂载时，用户会先看到表单项的“验证码”Label 字样，间隔几百毫秒后，“点击进行安全验证”按钮才突然弹入页面中，产生明显的界面跳变与抖动。
- **根因分析**：
  1. **自适应决策链耗时**：`VolurCaptcha.vue` 在挂载时检测到 `type === 'ADAPTIVE'`，在 `onMounted` 钩子中触发 `fetchCaptcha()` 异步动作：
     - 请求 1：`GET /api/v1/captcha/public-key`（获取/预热服务端 RSA 公钥）；
     - 请求 2：`POST /api/v1/captcha`（提交魔改 RC4 + RSA 混淆 sceneId 并由后端风控引擎动态裁决出滑块模式 `SLIDER`）。
  2. **异步动态组件 Chunk 延迟**：滑块组件被定义为 `defineAsyncComponent(() => import('./SlidePuzzleCaptcha.vue'))`，在后端返回 `captchaType = 'SLIDER'` 后才开始向 Vite/服务器拉取组件代码与相关 SCSS 样式。
  3. **空高塌陷与无占位符**：在上述两段网络往返（耗时约 300ms ~ 800ms）期间，`<VolurCaptcha>` 内部既没有显示图形验证码，也没有挂载滑块按钮，容器渲染高度为 0。外层 `<n-form-item :label="t('auth.verification_code')">` 产生了一个高度为 0 的内容槽，导致 Label 孤立显示，随后按钮注入产生明显的累积布局偏移（CLS）。

---

## 2. 方案设计与改动点实施

### 2.1 i18n 领域命名空间（Namespace）按需懒加载重构
在 [`src/volur-lite-view/src/i18n.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/i18n.ts) 中重构了多语言加载引擎：
1. **定义标准化命名空间枚举**：
   - `'core'`：涵盖全平台基础骨架与身份认证词条（`auth.json`、`common.json`、`layout.json`、`error.json`，共 4 个文件）；
   - `'admin'`：涵盖管理后台控制台的所有配置项（`admin/*.json`，共 13 个文件）；
   - `'competition'`：涵盖竞赛竞技场与演练详情（`competition.json`）；
   - `'profile'`：涵盖用户个人中心设置（`profile.json`）。
2. **动态路径分类器与路由映射**：
   - 实现 `getNamespaceForPath(filePath)` 精准划分文件所属命名空间；
   - 实现 `getNamespacesForRoute(routePath)`：
     - `/auth/*`、`/login`、`/maintenance` 等路由仅返回 `['core']`；
     - `/admin/*` 自动推导并追加 `['admin']`；
     - `/competitions/*`、`/c/*`、`/arena/*` 自动推导并追加 `['competition']`；
     - `/profile` 自动推导并追加 `['profile']`。
3. **基于 Set 的精细化去重与增量合并**：
   - 维护 `loadedPaths = new Set<string>()`，已加载文件不再发起重复请求；
   - 使用 Vue I18n 原生 `mergeLocaleMessage(locale, messages)` 进行无损增量树形合并，杜绝 `setLocaleMessage` 覆盖旧词条的问题；
   - 首屏启动（`main.ts`）默认仅拉取 `['core']`（从 19 个请求锐减为 4 个请求）；
   - 路由守卫（`router.beforeEach`）与多语言切换（`store.switchLanguage`）调用 `loadLocaleForRouteAsync`，在页面导航或切语言时无感追加所需命名空间。

### 2.2 存储层 Root 导出瘦身（Store Barrel Decoupling）
在 [`src/volur-lite-view/src/store/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/store/index.ts) 中彻底移除了无意义的连带桶导出（`export * from './modules/competition'` 等）：
- 消除 `main.ts` 引入 `pinia` 时将 `competition`、`notification` 及其所依赖的 `SSE`、竞赛业务 API 等深层模块过早引入入口依赖图谱的隐患。

### 2.3 自适应验证码骨架屏回滚
根据用户要求，已将 `VolurCaptcha.vue` 的骨架屏占位与预加载逻辑彻底移除，恢复原版直接渲染行为：
- 移除 `<n-skeleton>` 骨架屏模板节点与 `isInitializing` 状态；
- 移除相关 SCSS 样式规则；
- 保持子组件按需异步懒加载与原有渲染逻辑。

---

## 3. 验证与审计结果

### 3.1 网络请求瀑布优化成效
通过 Chrome DevTools MCP 在 `https://test-platform.volurctf.com/auth/login` 真实测试环境进行全链路请求重放与追踪：
- **首屏语言文件请求量**：由优化前的 **19 个** 骤降至 **4 个**（`auth.json`、`common.json`、`error.json`、`layout.json`）；
- **总网络请求数量**：由初始的 **85 个** 缩减至 **66 个**；
- **完全杜绝**了 `risk.json`、`admin/*.json` 等后台管理高敏感词条在未登录访客端的暴露与无序下载。

### 3.2 动态多语言切换校验
- 模拟点击左下角多语言切换控件至英文（`EN`）：
  - 仅精准加载了 4 个 `en_US` 核心词条（`en_US/auth.json`、`en_US/common.json`、`en_US/error.json`、`en_US/layout.json`）；
  - 界面所有标签（"Verification Code"、"Click to verify"、"Continue" 等）瞬间刷新并精准呈现，未拉取任何无关的 `admin` 或 `competition` 文件；
- 再次切回中文（`中`）：
  - 命中 `loadedPaths` 内存缓存，**0 个 HTTP 请求**，瞬时切换。

### 3.3 构建与生产环境检验
在 `src/volur-lite-view` 根目录下执行：
```bash
npm run build
```
- 构建顺利完成（✓ built in 6.21s），TypeScript 严格模式类型校验 0 错误；
- Rollup 顺利将语言词条拆分为解耦的独立 Chunk（如 `auth-*.js`、`competitions-*.js`、`risk-*.js`、`profile-*.js`）。

### 3.4 验证码与登录按钮间距优化
- **原状态归因**：验证码 `<n-form-item>` 默认渲染了 `height: 24px` 的校验反馈占位槽（`.n-form-item-feedback-wrapper`），叠加提交按钮自身的 `margin-top: 12px`，导致两按钮之间产生了高达 `36px` 的过大空白间隔。
- **优化调整**：
  - 在 `AuthView.vue` 验证码表单项显式声明 `:show-feedback="false"`，剔除 24px 无效反馈槽；
  - 提交按钮 `.auth-form__submit-button` 统一微调至 `margin-top: 16px`；
  - 验证码按钮与登录提交按钮之间的垂直距离精准收敛至标准黄金比例 **16px**。

### 3.5 滑动验证码核验成功后的点击锁定与物理级消除按压变色
- **原状态归因**：
  1. 滑块拼图完成验证后，外层依然渲染为原生 `<button>`（Naive UI `n-button`），用户点击或按压时，Naive UI 会触发内部水波纹动画（`n-base-wave`）并应用 `:active` / `:hover` 的深绿按压色（`--n-color-pressed`）；
  2. 点击事件未被彻底物理切断，存在二次唤起弹窗的风险。
- **优化调整**：
  - **组件元素物理退化**：验证通过后（`ticket` 存在时），由 `v-if` 条件将原本的 `<n-button>` 切换为静态展示条 `<div class="volur-captcha-slider__verified">`；
  - **彻底杜绝按压/悬浮变色**：由于纯 `div` 不具备原生 `<button>` 的 `:active` / `:focus` 行为和 Naive UI 内部波纹机制，且样式设定了 `user-select: none; cursor: default;`，因此在用户鼠标点击或长按时**绝对不会产生任何变色、阴影凹陷或水波纹效果**；
  - **逻辑与方法双重阻断**：`handleTriggerClick()` 与 `openModal()` 均添加 `if (props.ticket) return` 守卫，杜绝一切二次唤起可能。
