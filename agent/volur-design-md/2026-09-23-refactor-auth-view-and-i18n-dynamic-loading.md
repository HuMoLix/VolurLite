# 登录视图去重构与多语言动态按需加载施工记录

## 1. 变更背景与需求分析

在针对远端测试部署环境（基于开发模式映射）的性能审计中，发现首屏与登录页存在以下关键问题：
1. **多语言全量静态打包膨胀**：`i18n.ts` 原先通过 `import.meta.glob('./locales/**/*.json', { eager: true })` 贪婪拉取全平台中英双语下全部 40+ 个 JSON 语言切片，不仅抢占浏览器并发 HTTP 连接池，而且在开发模式下产生数十个细碎网络往返；
2. **多语言切换体验缺失**：用户点击语言切换键时缺乏过渡状态，直接发生界面突变；
3. **`AuthView.vue` 与 `AuthForm.vue` 双重冗余挂载**：`AuthView.vue` 曾仅作为一个空壳包裹 `AuthForm.vue`，两层组件各自定义了模式判断与状态流转；同时，`AuthForm.vue` 与其子组件 `VolurCaptcha.vue` 中均静态全量挂载了 `<SlidePuzzleModal>` 行为式滑块拼图弹窗，导致首屏不必要地解析和执行弹窗组件逻辑；
4. **`volurEx` 根入口桶导出（Barrel Export）雪崩**：`App.vue` 与 `routes.ts` 静态从 `@/public/volurEx` 引入单项组件/布局，触发了富文本编辑器（marked/dompurify）、聊天 WebSocket、Ionicons 全量图标库及后台管理布局的链式提前加载。

---

## 2. 施工与实施方案

### 2.1 `i18n.ts` 异步按需加载与首屏剪枝
- **动态按需导入改造**：移除 `{ eager: true }`，使 `import.meta.glob('./locales/**/*.json')` 转为动态加载器字典；
- **首屏单语言预载**：在 [`src/main.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/main.ts) 的 `bootstrap()` 异步流程中，首屏仅按需拉取当前默认语言包（如 `zh_CN`），未使用语言包零请求；
- **切换时全屏 Loading 覆盖**：
  - 在 [`src/store/modules/app.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/store/modules/app.ts) 中新增 `isLangSwitching` 与 `switchLanguage` 统一调度逻辑；
  - 在 [`src/App.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/App.vue) 根层级挂载毛玻璃全屏遮罩动画（`.global-lang-loading-mask`），配合 `NSpin` 指示器；
  - 统一更新 [`AuthLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/AuthLayout.vue)、[`VolurHeader.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/components/VolurHeader.vue) 及 [`CompetitionLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/competitions/CompetitionLayout.vue) 中的 `toggleLanguage()` 为统一调用 `appStore.switchLanguage(...)`。

### 2.2 `AuthView.vue` 与 `AuthForm.vue` 深度合并与冗余清理
- **视图层收敛下沉**：将认证卡片（登录、注册、找回密码表单及验证码流程）直接归置合并入 [`src/views/auth/AuthView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/auth/AuthView.vue)，消除中间壳层组件以及多余的包装 DOM；
- **路由状态双向对齐**：使用计算属性 `computed` 的 getter/setter 直接将 `mode` 映射至路由路径与参数，消除外部 wrapper 与内部 `mode` 的不同步缺陷；
- **弹窗组件异步化与 `v-if` 条件渲染**：
  - 在 [`AuthView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/auth/AuthView.vue) 与 [`VolurCaptcha.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/components/verification/VolurCaptcha.vue) 中统一将 `SlidePuzzleModal` 改造为 `defineAsyncComponent(() => import('@/components/verification/SlidePuzzleModal.vue'))`；
  - 增加 `v-if="showPuzzleModal"`，确保在未触发滑块前，DOM 树上 **0 个** `SlidePuzzleModal` 实例，网络上 **0 个** 对应脚本请求；
- **平滑兼容保证**：[`src/components/auth/AuthForm.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/components/auth/AuthForm.vue) 保留为转发代理，保障既有代码引用无感兼容。

### 2.3 解耦 `@/public/volurEx` 桶导出
- **`App.vue` 精准懒加载**：将 `import { VolurBroadcastModal } from '@/public/volurEx'` 替换为异步懒加载 `defineAsyncComponent(() => import('@/public/volurEx/components/VolurBroadcastModal.vue'))`；
- **`routes.ts` 路由布局异步化**：将 `AppLayout`、`AuthLayout`、`AdminLayout` 由静态全量导入改为动态函数导入 `() => import(...)`，彻底切断后台/平台主布局向登录页的依赖渗透；
- **多语言键补全**：在 `zh_CN/auth.json` 与 `en_US/auth.json` 中补齐 `click_to_verify` 和 `verified_success`，消除控制台 intlify 报错。

---

## 3. 影响范围与测试验证

- **构建验证**：执行 `npm run build`，编译打包耗时 ~5.89s，核心静态产物 `index.js` 由 1.03 MB 骤降至 538 KB，所有未使用的模块与语言切片均成功独立拆包；
- **性能收益**：
  - 首屏语言文件网络请求数减少 50% 以上；
  - 登录页不再加载 `marked.js`、`dompurify.js`、`websocket.ts` 以及管理后台组件；
  - 语言切换过程中具备沉浸式平滑遮罩反馈，体验符合高质量 Web 交互标准。
