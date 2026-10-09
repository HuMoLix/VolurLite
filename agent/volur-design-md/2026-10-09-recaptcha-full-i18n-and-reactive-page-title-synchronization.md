# reCAPTCHA 全链路国际化与页面标题响应式同步架构设计

## 1. 变更背景与核心目标

在 `src/volur-lite-view-NEXT` 前端工程中，人机核验模块（`recaptcha.vue` 及 `RecaptchaModal.vue`）承担着防御恶意撞库、密码喷洒与脚本滥用的核心安全挑战。在此前实现中：
1. **reCAPTCHA 沙箱内硬编码残留**：沙箱 iframe（`sandboxHtml`）内部存在诸如“我不是机器人”、“隐私权”、“条款”、“验证已通过”等静态中文文本，且部分异常提示（如“缺少人机验证会话凭据”、“人机安全核验失败”、“刷新挑战失败”等）未接入 `vue-i18n` 字典。在用户切换语言为 English (`en-US`) 或繁体中文 (`zh-TW`) 时，由于 iframe 的隔离性与文本硬编码，导致英文环境下仍展示中文，严重破坏了国际化体验；
2. **页面标题缺少响应式同步机制**：浏览器 Tab 标题（`document.title`）与卡片头部标题（`AuthCardShell`）在用户切换多语言时无法实时同步。路由定义（`src/router/routes/auth.ts`）中仅配置了静态中文 `meta.title`，导致在 SPA 内部通过右下角下拉菜单切换语言时，页面正文已切换，但浏览器标题与卡片标题依然残留旧语言，页面刷新后也无法按当前语言动态解析。

针对上述两项缺陷，本次重构完成了：
- reCAPTCHA 沙箱 iframe 与交互流程的完整全语言国际化；
- 基于声明式路由元信息 `titleKey` / `cardTitleKey` / `cardSubtitleKey` 的浏览器标签页标题与卡片标题响应式即时同步架构。

---

## 2. 架构设计与实现方案

### 2.1 reCAPTCHA 沙箱 iframe 动态双通道国际化

由于 `recaptcha.vue` 内部采用受限沙箱 iframe（`srcdoc`）渲染以隔离主应用的 DOM 并进行人机行为轨迹捕获，传统的 Vue 模板响应式绑定无法直接穿透 iframe 边界。为此设计了**初始化模板插值 + 跨沙箱消息响应通道**双轨机制：

#### 2.1.1 初始渲染与模板插值
在 `sandboxHtml` 计算属性中，动态绑定主应用的当前语言与国际化文本：
- `<html lang="${currentLang}" class="${htmlClass}">`：同步声明当前 HTML 语言标识；
- `rc-text-label` 初始内容通过 `${notRobotLabel}` 插值渲染，隐私权与服务条款链接通过 `${privacyLabel}`、`${termsLabel}` 插值；
- 沙箱内部声明 `var I18N = { notRobot: ..., verified: ... }` 全局字典对象，解耦组件内部对静态文本的依赖。

#### 2.1.2 运行时无感语言热切换通道 (`VM_LOCALE_CHANGE`)
在主应用中，通过 Vue 响应式侦听器监听 `appStore.locale`：
```typescript
watch(
  () => appStore.locale,
  () => {
    sendToSandbox({
      type: 'VM_LOCALE_CHANGE',
      notRobot: t('auth.recaptcha_not_robot'),
      verified: t('auth.recaptcha_verified')
    })
  }
)
```
沙箱内部消息监听器（`window.addEventListener('message', ...)`）捕获 `VM_LOCALE_CHANGE` 消息：
- 动态更新沙箱内部 `I18N` 字典；
- 依据当前验证状态（`state.isVerified` 与 `state.isProcessing`），即时刷新 `labelElem.textContent`；
- **优势**：避免了重新生成 `srcdoc` 导致 iframe 重新加载、DOM 闪烁或打断用户正在进行的核验交互。

#### 2.1.3 异常与状态提示语全面接入 i18n
组件所有业务抛错与弹窗错误提示均替换为 `t('auth.recaptcha_*')`：
- `auth.recaptcha_missing_credentials`
- `auth.recaptcha_failed`
- `auth.recaptcha_refresh_failed`
- `auth.recaptcha_verify_failed`

---

### 2.2 页面标题与卡片标题响应式同步架构

#### 2.2.1 路由元信息契约扩展 (`RouteMeta`)
在 `src/types/router.d.ts` 中扩展 `RouteMeta` 类型定义：
```typescript
interface RouteMeta {
  title?: string
  titleKey?: string
  cardTitle?: string
  cardTitleKey?: string
  cardSubtitle?: string
  cardSubtitleKey?: string
  ...
}
```
在 `src/router/routes/auth.ts` 中全面迁移所有认证路由：
- 将静态中文标题解耦，显式注入声明式 `titleKey`（如 `auth.meta_title_accountchooser`、`auth.meta_title_signin`、`auth.meta_title_signup`、`auth.meta_title_recovery_*` 等）；
- 同步注入 `cardTitleKey` 与 `cardSubtitleKey`。

#### 2.2.2 异步语言包预加载与路由守卫对齐
在 `src/i18n/index.ts` 中修正路由命名空间映射：
- 将 `/v1/recovery` 纳入包含 `auth.json` 命名空间的路径前缀，确保找回用户名和重置密码相关路由直接访问或刷新时能完整加载认证语言包；
- 在 `src/router/index.ts` 的 `router.beforeEach` 守卫中，将 `document.title` 的设置时机移至 `await loadLocaleForRouteAsync` 之后：
  ```typescript
  if (typeof document !== 'undefined') {
    if (to.meta.titleKey) {
      document.title = `${(i18n.global as any).t(to.meta.titleKey)}`
    } else if (to.meta.title) {
      document.title = `${to.meta.title}`
    }
  }
  ```
  保证初次访问或页面刷新时，优先使用加载完成的真实语言包渲染正确标题。

#### 2.2.3 根层响应式标题监听器 (`App.vue`)
在 `src/App.vue` 引入 `watchEffect` 统一监听路由与全局语言切换：
```typescript
watchEffect(() => {
  if (typeof document === 'undefined') return
  const titleKey = route.meta?.titleKey as string | undefined
  if (titleKey) {
    document.title = `${t(titleKey)}`
  } else if (route.meta?.title) {
    document.title = `${route.meta.title}`
  }
})
```
由于 `useI18n()` 的 `t` 函数具备 Vue 响应式依赖追踪能力，当用户在页面右下角切换语言时，`watchEffect` 将在下一个微任务周期自动执行并即时重写 `document.title`。

#### 2.2.4 卡片标题组件回退与响应式驱动 (`AuthCardShell.vue`)
在 `AuthCardShell.vue` 中将内部标题与副标题升级为计算属性：
```typescript
const effectiveTitle = computed(() => {
  if (props.title) return props.title
  if (route.meta?.cardTitleKey) return t(route.meta.cardTitleKey as string)
  if (route.meta?.cardTitle) return route.meta.cardTitle
  return ''
})
```
无论页面采用直接通过属性传参（`:title="t('...')"`）还是依赖路由元信息（`cardTitleKey`），均能在多语言切换时实现毫秒级自动刷新。

---

## 3. 语言字典定义清单 (`auth.json`)

在 `src/locales/{zh-CN,en-US,zh-TW}/auth.json` 中统一维护以下标准词条：

| 键名 | 简体中文 (`zh-CN`) | 英文 (`en-US`) | 繁体中文 (`zh-TW`) |
| :--- | :--- | :--- | :--- |
| `recaptcha_not_robot` | 我不是机器人 | I'm not a robot | 我不是機器人 |
| `recaptcha_verified` | 验证已通过 | Verified | 驗證已通過 |
| `recaptcha_missing_credentials` | 缺少人机验证会话凭据 | Missing verification credentials | 缺少人機驗證工作階段憑證 |
| `recaptcha_refresh_failed` | 刷新挑战失败 | Failed to refresh challenge | 重新整理挑戰失敗 |
| `recaptcha_verify_failed` | 验证失败，请重试 | Verification failed, please try again | 驗證失敗，請重試 |
| `meta_title_accountchooser` | 选择账号 - Volur 账号 | Choose an account - Volur Account | 選擇帳號 - Volur 帳號 |
| `meta_title_signin` | 登录 - Volur 账号 | Sign in - Volur Account | 登入 - Volur 帳號 |
| `meta_title_challenge` | 登录 - 安全核验与挑战 | Sign in - Security Challenge | 登入 - 安全核驗與挑戰 |
| `meta_title_signup` | 注册 - Volur 账号 | Create your Volur Account | 註冊 - Volur 帳號 |
| `meta_title_recovery_username` | 找回用户名 - Volur 账号 | Recover your username - Volur Account | 找回使用者名稱 - Volur 帳號 |
| `meta_title_recovery_email` | 找回用户名 - 电子邮箱 | Recover username - Email | 找回使用者名稱 - 電子郵件 |
| `meta_title_recovery_password` | 找回密码 - Volur 账号 | Reset password - Volur Account | 找回密碼 - Volur 帳號 |
| `meta_title_recovery_emailotp` | 找回密码 - 邮箱验证码 | Verify your identity - Email code | 找回密碼 - 信箱驗證碼 |
| `meta_title_recovery_reset` | 重置密码 - Volur 账号 | Set a new password - Volur Account | 重設密碼 - Volur 帳號 |
| `meta_title_recovery_challenge` | 安全核验 - Volur 账号 | Security verification - Volur Account | 安全核驗 - Volur 帳號 |
| `meta_title_sessionexpire` | 会话已超时 - Volur 账号 | Session expired - Volur Account | 工作階段已逾時 - Volur 帳號 |

---

## 4. 影响范围与验证结果

- **影响文件**：
  - `src/volur-lite-view-NEXT/src/components/security/recaptcha.vue`
  - `src/volur-lite-view-NEXT/src/components/auth/AuthCardShell.vue`
  - `src/volur-lite-view-NEXT/src/App.vue`
  - `src/volur-lite-view-NEXT/src/i18n/index.ts`
  - `src/volur-lite-view-NEXT/src/router/index.ts`
  - `src/volur-lite-view-NEXT/src/router/routes/auth.ts`
  - `src/volur-lite-view-NEXT/src/types/router.d.ts`
  - `src/volur-lite-view-NEXT/src/locales/{zh-CN,en-US,zh-TW}/auth.json`
- **验证项**：
  - `vue-tsc --noEmit && vite build` 编译 100% 通过（2903 modules transformed，0 errors）；
  - 全工程严格遵循 `GEMINI.md` 的代码零注释（Zero-Comment）铁律与 Google Material 规范。
