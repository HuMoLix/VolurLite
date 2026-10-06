# Next 前端 i18n 依赖集成、按需模块拆分懒加载、全屏 Loading 遮罩与全视图国际化覆盖

## 1. 变更背景与需求分析
在新一代前端工程（`src/volur-lite-view-NEXT`）中，此前认证流布局 `AuthLayout.vue` 中的语言切换器为纯静态 Mock/Dummy UI，未安装国际化依赖，且认证流程各个视图页面（如 `IdentifierView.vue`、`ChallengeView.vue`、`SignUpView.vue`、`sessionExpireView.vue` 等）均存在大量硬编码中文，导致切换至英文（`en-us`）或其他语言时无法生效。

在后续实机联调中，进一步暴露出两项关键缺陷：
1. **Intlify 告警**：控制台输出 `[intlify] Not found 'auth.next' key in 'zh' locale messages.`；
2. **表单校验错误缺乏响应式联动**：在 `IdentifierView.vue` 点击“下一步”后触发“请输入邮箱或账户名称”校验，切换语言时错误文本仍停留在切换前的语言，未能动态翻译为目标语言。

---

## 2. 核心架构与技术实现

### 2.1 语言文件模块化拆分与键名对齐
按命名空间将多语言字典拆分为独立 JSON 文件：
- `src/locales/zh-CN/common.json` & `auth.json`
- `src/locales/zh-TW/common.json` & `auth.json`
- `src/locales/en-US/common.json` & `auth.json`

针对 `auth.next` 缺失问题：
- 在各语言版本的 `auth.json` 与 `common.json` 中均完整补齐 `next`、`back`、`retry`、`cancel` 等通用动作词条，避免因命名空间引用差异导致 intlify 缺失警告；
- 在 `createI18n` 配置中建立 `fallbackLocale` 映射：`{ 'zh-TW': ['zh-CN'], 'zh': ['zh-CN'], default: ['zh-CN'] }`，且静态导入基准 `zh-CN` 词条预挂载在 `messages['zh-CN']` 与 `messages['zh']` 中，杜绝异步首屏竞争阶段对 `zh` 的父级回退报错。

### 2.2 动态按需懒加载机制 (`src/i18n/index.ts`) 与路由守卫前置拦截
- 使用 Vite 的 `import.meta.glob(['../locales/**/*.json', '!../locales/zh-CN/*.json'])` 实现对非默认语言包的动态按需分包；
- 暴露 `loadLocaleMessagesAsync(locale, namespaces)` 与 `loadLocaleForRouteAsync(locale, routePath)`；
- 在 `src/router/index.ts` 的 `router.beforeEach` 守卫中执行 `await loadLocaleForRouteAsync(appStore.locale, to.path)`，确保在路由组件挂载渲染前，当前路由所需的目标语言包已 100% 异步加载并合并完成。

### 2.3 全屏 Loading 遮罩与状态机流转 (`App.vue` & `app.ts`)
- 在 Pinia `useAppStore` 中维护 `isLangSwitching` 与 `langSwitchingText`，提供异步 action `switchLanguage(targetLocale, routePath)`；
- 在 `src/App.vue` 中配置支持暗黑模式的 `.global-lang-loading-mask` 全屏遮罩与过渡动效；
- 切换语言时：开启遮罩 -> 加载目标语言包 -> 更新 Pinia、i18n 实例与 LocalStorage -> 200ms 视觉过渡缓冲 -> 关闭遮罩。

### 2.4 表单校验错误状态全响应式治理 (`computed` + `errorKey`)
此前表单校验采用 `errorMsg.value = t('...')` 静态赋值，导致语言切换时底层响应式丢失。现全面重构为双轨响应式驱动：
- 声明 `errorKey = ref('')` 与 `rawErrorMsg = ref('')`；
- 派生响应式计算属性：
  ```ts
  const errorMsg = computed(() => {
    if (errorKey.value) {
      return t(errorKey.value)
    }
    return rawErrorMsg.value
  })
  ```
- 当用户触发校验（如未填写直接点击下一步）时，仅设置 `errorKey.value = 'auth.identifier_required'`；
- 当语言切换引发 vue-i18n 的响应式 trigger 时，`computed` 自动重算并渲染目标语言文本，彻底解决语言切换后已激活错误信息不随之切换的缺陷；
- 该模式已全量推行至：
  - `IdentifierView.vue` (`auth.identifier_required` / `auth.identifier_failed`)
  - `pwdChallengeView.vue` (`auth.password_required` / `auth.password_incorrect`)
  - `NameStepView.vue` (`auth.username_required` / `auth.username_length_error` / `auth.username_pattern_error` / `auth.username_verify_failed`)
  - `EmailStepView.vue` (`auth.email_required` / `auth.email_format_error` / `auth.email_submit_failed`)
  - `PasswordStepView.vue` (`auth.password_required` / `auth.password_length_min8` / `auth.confirm_password_required` / `auth.passwords_not_match`)
  - `EtotpChallengeStepView.vue` (`auth.code_required` / `auth.code_digits_only` / `auth.code_verify_failed`)
  - `totpChallengeView.vue` (`auth.security_code_required` / `auth.security_code_invalid`)
  - `deviceChallengeView.vue` (`auth.passkey_not_supported` / `auth.passkey_ip_not_supported` / `auth.passkey_cancelled` / `auth.passkey_verify_failed`)
  - `recaptchaChallengeView.vue` (`auth.recaptcha_required` / `auth.recaptcha_failed` / `auth.account_not_found`)
  - `AccountChooserView.vue` (`auth.account_disabled` / `auth.account_flow_failed`)

### 2.5 全视图硬编码国际化清洗
彻底清洗并改造所有硬编码提示词条，包括：
- `AccountChooserView.vue`：选择账号、以继续访问 Volur、使用其他账号、移除账号、完成、已登录、已禁用、已退出等全部词条；
- `ChallengeView.vue`：安全核验顶栏与提示语；
- `GoogleTextField.vue`：清空按钮提示与密码显隐按钮提示；
- 底部页脚外链参数跟随当前语言动态拼接：`/accounts/help?lang=${currentUrlLang}` 等。

---

## 3. 构建与验证结果
1. **词条完整性自动化校验**：通过 Node.js 校验脚本扫描全量代码中调用的 `t()` 键名，与 `zh-CN`、`zh-TW`、`en-US` 字典比对，缺失词条均为 0；
2. **纯净代码审计**：通过自动化注释扫描器确认所有修改的 `.ts`、`.vue` 文件严格遵守零行内注释铁律；
3. **打包构建**：执行 `npm run build`，TypeScript 类型检查无报错，Vite 成功生成精细化分包产物（Exit Code 0）。
