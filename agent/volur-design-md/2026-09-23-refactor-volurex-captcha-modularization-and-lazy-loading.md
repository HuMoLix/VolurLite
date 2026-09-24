# 施工记录：自研组件库 volurEx 验证码模块化重构与按需惰性加载

## 1. 变更背景与架构动机
在 Volur 平台前端工程中，先前的人机验证模块存在以下架构层级模糊与冗余挂载缺陷：
1. **职责划分不清**：`<SlidePuzzleModal>` 本质上是人机验证中的一种（滑动拼图），此前却独立于验证码协调器在视图层（如 `AuthView.vue`、`ProfileView.vue`）和 `VolurCaptcha.vue` 中同时被直接引用并挂载，导致了多处重复挂载与状态不同步。
2. **缺乏平台组件库规范收敛**：`VolurCaptcha.vue` 作为平台通用的全局安全组件，未收敛至自主研发的前端组件库目录（`@/public/volurEx`）下，违背了 `GEMINI.md` 与 `view-Rules.md` 规范。
3. **未做验证码模态惰性分包 (Lazy Loading)**：图形验证码、滑块拼图（含 Canvas/拖动轨迹算法）、邮箱/短信动态验证码全部内嵌或紧密耦合在单一文件中，导致用户在登录等场景下即使仅需要字符验证码，也会直接拉取滑块拼图的全部依赖。

---

## 2. 详细改造范围与文件结构

按照“统一验证码入口、各验证码模态物理隔离、按需惰性加载、外部仅调用主协调器”的设计原则，进行了如下改造：

### 2.1 新建 `@/public/volurEx/components/captcha` 模块
在 `src/volur-lite-view/src/public/volurEx/components/captcha/` 下实现微模块解耦：
1. **`types.ts`**：
   - 统一定义 `CaptchaDisplayType`（`'NONE' | 'IMAGE' | 'SLIDER' | 'EMAIL' | 'SMS'`）。
   - 定义 `VolurCaptchaProps` 与 `VolurCaptchaExpose` 接口（`getPayload()`, `verifyBehavior()`, `handleFailure()`, `reset()`, `refresh()` 等）。
2. **`ImageCaptcha.vue`**：
   - 字符图形验证码子模块：封装输入框、验证码图片预览、点击防抖刷新、Enter 快捷提交事件。
3. **`SlidePuzzleCaptcha.vue`**：
   - 行为式滑块拼图子模块：既支持内联触发按钮模式（“点击进行人机验证”），又支持纯弹窗（`headless`）编程式调用；完整封装 Canvas、滑块碎片、拖拽轨迹跟踪、向后端核销凭证（`verifyCaptchaTicketApi`）及成败动效。
   - 对外暴露 `openModal(scene?: string): Promise<CaptchaTicketVO>`。
4. **`EmailCaptcha.vue`**：
   - 邮箱动态安全码子模块：封装 6 位验证码输入、发送防刷冷却倒计时（60s）与加载状态。
5. **`SmsCaptcha.vue`**：
   - 短信动态安全码子模块：支持短信验证码的统一交互与倒计时。
6. **`VolurCaptcha.vue`**（核心协调器/门面）：
   - 采用 `defineAsyncComponent` 分别对 `ImageCaptcha`、`SlidePuzzleCaptcha`、`EmailCaptcha`、`SmsCaptcha` 实施按需动态异步加载。
   - 根据自适应风控或外部指定类型，在界面中仅渲染当前需要的具体验证码组件；未激活的模块其 JavaScript 代码和资源绝不提前加载。
   - 提供编程式 `verifyBehavior(customScene?: string): Promise<CaptchaTicketVO>` 方法，允许父级视图在无需显式引入或挂载任何弹窗组件的前提下，拉起行为验证并等待凭证。
   - 封装 `handleFailure(err)` 自适应防御跃迁（自动降级/跃迁为滑块拼图或动态码）。
7. **`index.ts`**：
   - 统一导出 `VolurCaptcha` 及各个子模块，并在 `src/public/volurEx/components/index.ts` 中集中透出。

---

### 2.2 业务视图 `AuthView.vue` 全面去冗余
- **移除直接弹窗挂载**：彻底删除 `AuthView.vue` 内部引入的 `SlidePuzzleModal.vue`、`showPuzzleModal`、`puzzleScene`、`pendingAction` 等冗余状态。
- **仅挂载 `VolurCaptcha`**：
  - 登录表单内：挂载 `<VolurCaptcha ref="loginCaptchaRef" scene="LOGIN" :account="loginForm.username" @enter="handleAction" />`；
  - 视图顶层：挂载全局无界面协调器 `<VolurCaptcha ref="behaviorCaptchaRef" headless />`；
  - 注册与找回密码流程：直接调用 `await behaviorCaptchaRef.value?.verifyBehavior('REGISTER')` 或 `verifyBehavior('RESET_PASSWORD')` 获取凭证，表单提交逻辑极致扁平。

---

### 2.3 向后兼容性保障
- **`src/components/verification/VolurCaptcha.vue`**：转换为轻量代理层，透明转发属性、事件与对外暴漏方法到 `@/public/volurEx/components/captcha/VolurCaptcha.vue`。
- **`src/components/verification/SlidePuzzleModal.vue`**：转换为轻量代理层，透明转发到 `SlidePuzzleCaptcha.vue`，确保历史遗留的直接调用（如 `ProfileView.vue`）零报错、平滑兼容。

---

## 3. 验证结果
执行 TypeScript 静态类型检查与 Vite 打包：
- `vue-tsc --noEmit`: 0 错误；
- `vite build`: 成功打包，耗时 6.45s。
- 登录页与认证页的首屏依赖关键路径上，拼图算法、拖拽事件与邮箱短信逻辑全部被拆解为独立的 Async Chunks，仅在触发时动态拉取。
