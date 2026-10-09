# 认证语言包模块化拆分与层级命名空间架构演进

## 1. 变更背景与架构动机

在 `src/volur-lite-view-NEXT` 工程的持续演进中，原 `auth.json` 语言文件逐渐膨胀至近 200 个词条，单文件承载了包含：
- 常规认证与会话状态（Common Auth & Session State）
- 登录与账号选择流程（Sign-in & Account Chooser）
- 注册与凭据创建流程（Sign-up & Onboarding）
- 找回用户名与重置密码流程（Recovery Flow）
- 人机核验与行为挑战（reCAPTCHA & Challenge Modals）

这种巨石型（Monolithic）字典结构存在如下痛点：
1. **职责边界混乱**：不同独立业务流程的文案混合存放，缺少领域隔离；
2. **包体积与加载性能**：按需动态加载无法对认证内部子模块进行细粒度代码拆分，导致首屏或单流加载冗余；
3. **命名空间冲突与代码自解释性弱**：扁平键名容易产生命名割裂或冗余前缀（如 `recaptcha_not_robot`、`recovery_password_reset_title`），缺乏层级化领域语义。

为此，对全工程的认证国际化字典实施深度模块化拆解，并在代码引用中全面落地**严格层级命名空间**（Hierarchical Namespaces）。

---

## 2. 目录结构与分包架构

语言包物理结构由单文件拆解为物理目录与领域模型 1:1 对齐的模块化架构：

```
locales/{zh-CN,en-US,zh-TW}/
├── common.json
├── auth/
│   ├── auth.json       # 平台通用认证、会话超时、通用状态与基础校验文案
│   ├── signin.json     # 登录、账号选择、两步验证与登录方式选择
│   ├── signup.json     # 账号注册、分步表单、通行密钥/TOTP 保护
│   └── recovery.json   # 找回用户名、找回密码、邮箱验证码与重置密码
└── captcha/
    └── recaptcha.json  # 滑动拼图、字符验证码、人机复选框及核验提示
```

三套语言体系（简体中文 `zh-CN`、美式英语 `en-US`、繁体中文 `zh-TW`）同步完成结构拆分与 100% 键名对齐，杜绝缺失与漏译。

---

## 3. 层级命名空间设计规范

在代码层全面演进为高内聚的层级命名空间：

| 领域模块 | 物理文件路径 | i18n 命名空间前缀 | 范例调用 |
| :--- | :--- | :--- | :--- |
| **通用认证** | `auth/auth.json` | `auth.*` | `t('auth.guest_mode_tip')`, `t('auth.session_expired')` |
| **登录业务** | `auth/signin.json` | `auth.signin.*` | `t('auth.signin.title')`, `t('auth.signin.choose_account')` |
| **注册业务** | `auth/signup.json` | `auth.signup.*` | `t('auth.signup.title')`, `t('auth.signup.finish_signup')` |
| **找回业务** | `auth/recovery.json` | `auth.recovery.*` | `t('auth.recovery.username_title')`, `t('auth.recovery.reset_title')` |
| **人机核验** | `captcha/recaptcha.json` | `captcha.recaptcha.*` | `t('captcha.recaptcha.not_robot')`, `t('captcha.recaptcha.drag_slider')` |

彻底剔除键名内部冗余的类型前缀，如 `recaptcha_not_robot` 精简为 `captcha.recaptcha.not_robot`，`signin_title` 精简为 `auth.signin.title`。

---

## 4. i18n 加载器与路由按需合并改造 (`src/i18n/index.ts`)

1. **首屏静态字典构造**：
   - 静态导入 `zh-CN` 各模块片段，合并构建深层对象：
     ```typescript
     const zhCnAuth = {
       ...zhCnAuthCommon,
       signin: zhCnAuthSignin,
       signup: zhCnAuthSignup,
       recovery: zhCnAuthRecovery
     }
     const zhCnCaptcha = {
       recaptcha: zhCnCaptchaRecaptcha
     }
     ```
   - 注入初始 `messages`，确保核心首屏即时渲染，零网络阻塞。
2. **路由命名空间与动态加载推导**：
   - `getNamespaceForPath` 适配多级子目录（识别 `/auth/`、`/captcha/`）；
   - `getNamespacesForRoute` 在认证路由下动态加载 `auth` 与 `captcha`；
   - `loadLocaleMessagesAsync` 递归解析二级路径 `[dir, file]`，动态合并为深层树形结构，使 `en-US` 与 `zh-TW` 运行时的消息树结构与 `zh-CN` 保持 100% 同构。

---

## 5. 影响范围与验证结果

- **影响文件**：
  - 核心配置：`src/volur-lite-view-NEXT/src/i18n/index.ts`
  - 路由声明：`src/volur-lite-view-NEXT/src/router/routes/auth.ts`
  - 基础组件：`RecaptchaModal.vue`、`recaptcha.vue`、`AuthAccountChip.vue`、`GoogleTextField.vue`
  - 业务视图：`views/auth/signin/**`、`views/auth/signup/**`、`views/auth/recovery/**`（共 27 个代码文件）
  - 国际化文件：新增 15 个模块化 JSON 文件，彻底移除旧版 3 个单体 `auth.json`。
- **验证项**：
  - `verify_locales_integrity.py` 脚本全量校验：代码中引用的 179 个 i18n 键在 `zh-CN`、`en-US`、`zh-TW` 中 100% 存在，无一遗漏；
  - `npm run build`（`vue-tsc --noEmit && vite build`）编译一次性通过（2915 modules transformed，0 errors，构建耗时 2.92s）；
  - Vite 产物正确生成细粒度 chunk（`signin-*.js`, `signup-*.js`, `recovery-*.js`, `recaptcha-*.js`）。
