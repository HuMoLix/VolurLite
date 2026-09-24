# 认证域组件分层架构重构与正向依赖恢复施工记录

## 1. 变更背景与架构审计

在既有代码库中，针对前端认证域的调用关系存在以下反模式与层级倒置问题：
1. **反向依赖反模式（Inverted Dependency）**：
   通用业务组件 [`src/components/auth/AuthForm.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/components/auth/AuthForm.vue) 仅保留了 8 行代码，反向导入并渲染了顶层路由页面 [`src/views/auth/AuthView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/auth/AuthView.vue)；
2. **职责边界混乱与缺乏复用能力**：
   所有的表单状态（`loginForm`、`registerForm`、`forgotForm`）、输入校验规则（`loginRules`、`registerRules`、`forgotRules`）、邮箱验证码倒计时、风控滑块验证码联动等 680+ 行核心逻辑全盘堆砌在路由页面 `AuthView.vue` 中。导致未来如果要在 Modal / 弹窗中复用登录/注册表单，将无法抽离使用；
3. **正统三层架构断裂**：
   按 Vue 架构哲学与 Volur Lite 规范，正确的层级与职责应为：
   - **Layout（布局骨架层）**：[`AuthLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/AuthLayout.vue)，提供整体外围骨架（Header、Footer、主题与国际化控制），通过 `<router-view />` 承载子路由；
   - **View（路由页面层）**：[`AuthView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/auth/AuthView.vue)，作为路由节点（`/auth/login`、`/auth/register`、`/auth/forgot`）的响应式路由承载页，监听和双向映射路由参数与状态，协调重定向与页面过渡；
   - **Component（业务组件层）**：[`AuthForm.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/components/auth/AuthForm.vue)，封装纯粹的认证表单 UI、字段校验、人机风控与 API 交互。

---

## 2. 施工方案与核心改动

### 2.1 业务表单完全回归 [`AuthForm.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/components/auth/AuthForm.vue)
- **状态与交互下沉**：
  将登录、注册、找回密码三套 Naive UI 表单、验证码组件调用（`VolurCaptcha`）、`FormRules`、邮件验证码倒计时及请求调用完整收敛至 `AuthForm.vue`；
- **现代化双向数据绑定与事件通知**：
  - 基于 Vue 3.4+ `defineModel<AuthMode>('mode', { default: 'login' })` 实现表单当前模式与外部驱动的双向解耦绑定；
  - 声明 `emit('success', type)` 和 `emit('mode-change', mode)`，在完成登录、注册成功或发送重置邮件后，向上层抛出事件；
  - 兼容 `props.initialMode`，确保即使脱离路由上下文（例如在弹窗场景中直接使用 `<AuthForm />`）亦可正常运行；
- **纯净性与解耦**：
  组件内部不再直接耦合 `vue-router` 的 `useRouter` / `useRoute`，转为纯组件化输入输出。

### 2.2 路由适配与胶水层收敛入 [`AuthView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/auth/AuthView.vue)
- **路由双向同步（Two-way Route Binding）**：
  通过 `computed` getter 与 setter 计算 `currentMode`：
  - **getter**：根据 `route.name`（`Register`、`ForgotPassword`、`Login`）、`route.path` 或 `route.query.mode` 自动派生当前模式；
  - **setter**：当表单组件触发模式切换时，检查目标路由路径（`/auth/register`、`/auth/forgot`、`/auth/login`），若不一致则使用 `router.push()` 执行平滑导航并保留 `redirect` 参数；
- **处理认证成功跳转**：
  在监听到 `handleAuthSuccess('login')` 时，精准提取 `route.query.redirect` 并跳转至对应目标地址（默认 `/`）。

---

## 3. 架构依赖关系对比

| 维度 | 重构前（反向依赖反模式） | 重构后（正统分层架构） |
| :--- | :--- | :--- |
| **调用链条** | `AuthLayout` $\to$ `<router-view>` (`AuthView`) <br> `AuthForm` $\to$ `AuthView` (倒置代理) | `AuthLayout` $\to$ `<router-view>` (`AuthView`) $\to$ `<AuthForm>` |
| **`AuthForm.vue`** | 仅 8 行空壳，反向包裹 `AuthView`，强耦合页面路由 | 纯净业务组件，使用 `defineModel` 与 `emit`，高可复用 |
| **`AuthView.vue`** | 680+ 行巨大单文件，揉杂表单 UI、校验及路由 | 精简为 40 行的路由控制器与胶水容器，关注路由状态与跳转 |
| **组件复用性** | 无法被弹窗（Modal）等其它场景复用 | 可无缝作为子组件在任何地方挂载 |

---

## 4. 验证与审计结果

1. **类型检查与前端编译构建**：
   运行 `npm run build`（`vue-tsc --noEmit && vite build`）：
   - TypeScript 类型推导 100% 通过，0 类型错误；
   - Vite 打包构建成功，`AuthView` 与相关模块产物正常生成，首屏懒加载与分包策略完全保持；
2. **路由与交互状态流转验证**：
   - 访问 `/auth/login` $\to$ 显示登录表单；
   - 点击“没有账号？立即注册” $\to$ URL 响应式更新为 `/auth/register`，表单切换为注册表单；
   - 点击“忘记密码？” $\to$ URL 响应式更新为 `/auth/forgot`，表单切换为找回密码表单；
   - 点击浏览器后退键 $\to$ URL 变化驱动 `currentMode` 更新，`AuthForm` 模式自动同步切换。
