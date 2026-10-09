# 认证视图组件化解耦、通用组合式函数提取与全局按钮架构重构

## 1. 变更背景与核心目标

在 `src/volur-lite-view-NEXT/src/views/auth` 认证模块的长期迭代中，各个认证流程（登录 Sign-in、注册 Sign-up、找回恢复 Recovery、会话过期 SessionExpire）积累了大量的样板代码与样式冗余：
1. **按钮与链接样式高度冗余**：几乎每个子视图（共 20 余个文件）的 `<style scoped lang="scss">` 中都存在针对 `.auth-action-btn`、`.btn-primary-action`、`.btn-next-step`、`.btn-secondary-action`、`.auth-forgot-link`、`.auth-resend-link`、`.auth-account-chip` 等类的重复定义（单个文件冗余样式多达 150-250 行），导致全工程超过 2,500 行的重复 SCSS，且若有视觉规范调整需要跨数十个文件逐一修改；
2. **底部操作栏与头像 Chip 结构重复**：在各视图的模板中，底部的主/次操作按钮排列、插槽、响应式布局及 Naive UI 按钮参数存在高度模板复制；同时，用户身份标识胶囊 Chip（带首字母圆形头像与下拉小箭头）在多处视图以手写形式硬编码；
3. **认证手段选择列表多处硬编码**：找回密码手段选择（`recovery/password/SelectionView.vue`）、找回用户名手段选择（`recovery/username/SelectionView.vue`）、登录二次挑战选择（`signin/challenge/selectionView.vue`）以及注册安全保护选项（`OptionsChallengeStepView.vue`）逻辑与骨架屏、列表渲染、图标分支高度一致，却维护了 4 套近乎相同的模板与样式；
4. **外层卡片布局外壳与标头管理脱节**：找回恢复流程（`recovery/password/*` 与 `recovery/username/*`）各自手写了外层的 `.auth-card__body`、`.auth-card__left` 与 `.auth-card__right` 布局，并分散调用 `useCardHeader`；
5. **表单错误管理与倒计时定时器逻辑样板代码**：各步骤视图均重复编写 `errorKey`、`rawErrorMsg`、`errorMsg` 计算属性及 `watch` 清除逻辑；发送验证码各视图重复编写 `countdown = 60`、`setInterval`、`onUnmounted` 清理生命周期。

为消除上述全模块冗余，提升代码纯洁性与架构可维护性，遵循前端新架构规范（`view-NEXT-rules.md`）和根指令（`GEMINI.md`），决定实施全局解耦与架构统一改造。

---

## 2. 详细重构方案与改动点

### 2.1 建议 1：全局化按钮与操作链接样式体系 (`src/styles/buttons.scss`)
- 按照用户要求，将原计划的 `auth-buttons.scss` 统一命名为 `src/styles/buttons.scss`，确立为前端跨模块统一的主从按钮与操作链接设计标准；
- 在 `buttons.scss` 中完整收敛：
  - `.auth-action-btn`：高 36px，最小宽度 88px，圆角 20px 胶囊；
  - 主操作按钮（`.btn-primary-action`, `.btn-next-step`, `.btn-verify-action`）：亮色 Google Blue `#0b57d0`（Hover `#0e4eb5` / Active `#063980`），暗色 Google Blue `#8ab4f8`（文字 `#202124`）；
  - 次操作按钮（`.btn-secondary-action`, `.btn-create-account`, `.btn-try-another-way`, `.btn-signin-instead`, `.btn-remove-account`）：透明背景，无边框，Naive UI `quaternary`，文字 `#0b57d0`（Hover `rgba(11, 87, 208, 0.08)` / Active `rgba(11, 87, 208, 0.16)`），暗色对应 `#8ab4f8`；
  - 文本链接与重发按钮（`.auth-forgot-link`, `.auth-learn-more-link`, `.auth-resend-link`, `.auth-resend-btn`）；
  - 账号胶囊（`.auth-account-chip`、`.auth-account-chip__avatar`、`.auth-account-chip__label`）；
- 在 `src/styles/index.scss` 中统一 `@use './buttons.scss';` 导入；
- 清理了 28 个视图文件中所有的重复按钮与链接 scoped SCSS。

### 2.2 建议 2：提取高内聚认证通用 UI 组件库 (`src/components/auth/`)
在 `src/components/auth/` 下创建了 4 个高复用业务组件及集中导出入口 `index.ts`：
1. **`<AuthActionBar>` (`AuthActionBar.vue`)**：
   - 统一封装表单底部的“次操作按钮（Quaternary） + 主操作按钮（Primary）”响应式操作栏，支持自定义文本、禁用、加载态、插槽 `extra`；
2. **`<AuthAccountChip>` (`AuthAccountChip.vue`)**：
   - 统一封装当前正在进行挑战的用户头像胶囊，自动解析用户名/邮箱的首字符并大写展示，附带 Material 下拉箭头，点击触发账号切换；
3. **`<AuthMethodSelector>` (`AuthMethodSelector.vue`)**：
   - 统一封装认证手段与保护选项的选择列表；
   - 内置 Google 风格骨架屏加载态与空提示；
   - 内置根据 `icon`/`type`/`method`（设备/通行密钥、邮箱、手机短信、密码、安全盾）自动派发 Material 图标；
   - 提供 `extra-items` 插槽，支持扩展自定义选项（如“使用其他账号”）；
4. **`<AuthCardShell>` (`AuthCardShell.vue`)**：
   - 统一封装 `.auth-card__body` -> `.auth-card__left` 与 `.auth-card__right` 两栏式容器外壳；
   - 内部自动连接 `provideCardHeader`，通过 Props `title` 与 `subtitle` 响应式同步头部文字，并提供 `#header-extra` 悬挂插槽。

### 2.3 建议 3：提取通用组合式函数 (`src/composables/`)
1. **`useFormError(watchSources?)` (`src/composables/useFormError.ts`)**：
   - 响应式托管 `errorKey` 与 `rawErrorMsg`，统一通过 `errorMsg` 计算属性提供国际化翻译或原始错误文案；
   - 提供 `clearError`、`setErrorKey`、`setRawError`、`setError`（智能区分 i18n 键或文本）；
   - 支持传入监听源（如输入字段 `ref`），在用户重新输入时自动清除错误高亮与提示；
2. **`useCountdown(defaultSeconds)` (`src/composables/useCountdown.ts`)**：
   - 统一托管 60s 倒计时状态机，包含 `countdown`、`isCounting`、`start`、`reset`、`stop`；
   - 内置在组件销毁时（`onUnmounted`）自动清理 `setInterval` 定时器，杜绝内存泄漏。

### 2.4 建议 4：认证全流程页面解耦与架构统一
全面重构了 `src/views/auth/` 下的所有视图：
- **登录主流程 (`signin/`)**：
  - `IdentifierView.vue`：使用 `useFormError(identifier)`，底部采用全局按钮体系；
  - `ChallengeView.vue`：使用 `<AuthAccountChip>` 替换手写胶囊，清除冗余样式；
  - `AccountChooserView.vue`：包裹进 `<AuthCardShell>`，修复已知账户映射与探测 API 强类型调用，清理 180+ 行冗余样式；
  - `signin/challenge/` 全子步骤（`pwd`、`emailotp`、`smsotp`、`passkey`、`recaptcha`、`reject`、`selection`）：全量接入 `<AuthActionBar>`、`useFormError`、`useCountdown` 或 `<AuthMethodSelector>`；
- **注册主流程 (`signup/`)**：
  - `SignUpView.vue`：清理冗余 scoped 样式；
  - `signup/steps/`（`NameStepView`、`EmailStepView`、`EtotpChallengeStepView`、`PasswordStepView`、`OptionsChallengeStepView`）：全量接入 `<AuthActionBar>`、`useFormError`、`useCountdown` 与 `<AuthMethodSelector>`；
  - `signup/protect/`（`passkeyProtectView`、`totpProtectView`）：接入 `<AuthActionBar>`，清理冗余样式；
- **恢复与找回流程 (`recovery/`)**：
  - `recovery/password/`（`SelectionView`、`EmailOtpView`、`ResetView`）：外层统一迁移为 `<AuthCardShell>`，消除重复左右栏布局；选用 `<AuthMethodSelector>`、`<AuthActionBar>`、`useFormError` 与 `useCountdown`；
  - `recovery/username/`（`SelectionView`、`EmailView`）：外层统一迁移为 `<AuthCardShell>`，采用 `<AuthMethodSelector>` 与 `<AuthActionBar>`；
  - `recovery/challenge/recaptchaChallengeView` 与 `recovery/ChallengeView`：清理冗余卡片与按钮样式；
- **会话与模态组件 (`sessionExpireView`、`PasskeyModal`)**：
  - 清理手写按钮样式，复用全局 `.btn-primary-action` / `.btn-secondary-action`。

---

## 3. 代码纯洁性与规范遵循说明

1. **零代码注释铁律（Zero-Comment Rule）**：
   - 所有新建及重构后的代码文件（`.ts`、`.vue`、`.scss`）严格遵循 `GEMINI.md` 铁律，不包含任何解释性注释、TODO 或冗余行内备注；
2. **纯粹重构与无兼容包袱铁律**：
   - 未保留任何废弃的导出或别名兼容层；
3. **Fail-Fast & 强类型规范**：
   - 彻底修复 `AccountChooserView`、`EtotpChallengeStepView` 等处的强类型缺失与未读变量；
4. **技术栈与色彩规范**：
   - 严格遵循 `view-NEXT-rules.md` 中的 Google Material Design 灰阶及色彩规范，无缝支持亮暗色切换。

---

## 4. 影响范围与构建验证

- **构建验证**：
  - 运行 `npm run build`（`vue-tsc --noEmit && vite build`），全工程 2,903 个模块构建通过，零类型报错，Vite 打包耗时 6.32 秒；
- **代码行数精简成效**：
  - 修改/优化 28 个既有文件，新增 7 个组件/工具/样式文件；
  - **净删除 4,686 行重复代码与样式**（总计删除 5,547 行，新增 861 行）；
- **影响范围**：
  - `src/volur-lite-view-NEXT/src/styles/`
  - `src/volur-lite-view-NEXT/src/composables/`
  - `src/volur-lite-view-NEXT/src/components/auth/`
  - `src/volur-lite-view-NEXT/src/views/auth/`
