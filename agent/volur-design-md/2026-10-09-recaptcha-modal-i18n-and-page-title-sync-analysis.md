# RecaptchaModal 国际化重构与页面标题 i18n 响应式同步深度分析

## 1. 变更背景与工作概述

在 `src/volur-lite-view-NEXT` 工程中，人机核验模态弹窗组件（`RecaptchaModal.vue`）承担着滑动拼图（PUZZLE）、字符验证码（IMAGE）等多种安全验证挑战的弹窗交互。此前该组件内部存在大量硬编码的中文字符串（如刷新、关闭工具提示、滑动提示文案、输入占位符、操作按钮及标题判定），未能完全接入 Vue I18n 国际化体系。

同时，针对整个应用在多语言切换（`switchLanguage`）与浏览器页面刷新（F5）场景下，**页面标题（`document.title` 与卡片标题 `cardTitle`）如何实现即时、一致的响应式同步**这一核心架构问题，展开了全链路的现状诊断、成因剖析与落地方案设计。

---

## 2. RecaptchaModal 国际化（i18n）改造详情

### 2.1 语言包补全（zh-CN / en-US / zh-TW）
在三套语言配置文件的 `auth.json` 中统一增补了如下键名映射：
- `auth.recaptcha_refresh`：刷新验证码 / Refresh verification code / 重新整理驗證碼
- `auth.recaptcha_drag_slider`：拖动滑块完成拼图 / Drag the slider to complete the puzzle / 拖曳滑塊完成拼圖
- `auth.recaptcha_click_to_refresh`：点击刷新 / Click to refresh / 點擊重新整理
- `auth.recaptcha_enter_image_text`：请输入图片中的文字 / Please enter characters from the image / 請輸入圖片中的文字
- `auth.recaptcha_puzzle_title`：请完成滑动拼图 / Please complete the sliding puzzle / 請完成滑動拼圖
- `auth.recaptcha_image_title`：请输入字符验证码 / Please enter the character verification code / 請輸入字元驗證碼

同时复用通用键名：
- `common.close`：关闭 / Close / 關閉
- `common.verify`：验证 / Verify / 驗證
- `auth.security_verification`：安全核验 / Security verification / 安全核驗

### 2.2 组件改造 (`RecaptchaModal.vue`)
- 引入 `useI18n` 并获取 `const { t } = useI18n()`；
- 移除模板中所有硬编码中文，改用 `t(...)` 动态求值；
- 重构 `displayTitle` 计算属性：当未显式传入 `promptText` 时，根据 `challengeType` 动态返回对应的多语言标题；
- 源码保持 100% 纯净（零代码注释）。

---

## 3. 页面标题（`document.title` 与卡片标题）i18n 切换与刷新同步深度分析

### 3.1 现有机制现状与缺陷诊断

#### 3.1.1 缺陷 1：浏览器 Tab 标题（`document.title`）静态中文化
- **现状**：在 `src/router/index.ts` 的前置守卫中：
  ```typescript
  router.beforeEach(async (to) => {
    if (to.meta.title && typeof document !== 'undefined') {
      document.title = `${to.meta.title}`
    }
  ```
  而在 `src/router/routes/auth.ts` 中，`to.meta.title` 被声明为静态字符串：
  ```typescript
  meta: {
    title: '选择账号 - Volur 账号', // 静态中文硬编码
    cardTitle: '选择账号',
    cardSubtitle: '以继续访问 Volur',
    requiredAuth: false
  }
  ```
- **问题**：
  1. **SPA 内切换语言失效**：当用户在底部下拉框将语言切换为 English 时，仅触发了 `appStore.switchLanguage` 与 `router.replace({ query: { lang: 'en' } })`，页面正文切换为了英文，但 `document.title` 依然停留在中文 `"选择账号 - Volur 账号"`，出现严重割裂感；
  2. **带参刷新与直接访问失效**：即使用户以 `?lang=en` 访问或直接刷新页面，由于 `meta.title` 本身就是中文常量，`document.title` 永远无法展示英文标题。

#### 3.1.2 缺陷 2：缺少响应式联动监听（Watcher）
- `router.beforeEach` 仅在路由发生跳转或初次加载时执行一次。
- 当 `appStore.switchLanguage` 改变全局 `i18n.global.locale` 时，并未向浏览器 DOM 的 `<title>` 派发变更，缺少从 `locale` 变更到 `document.title` 的响应式通道。

---

### 3.2 为什么必须在 i18n 切换与页面刷新时同步修改页面标题？

1. **Google Material / Workspace 设计规范的一致性**：
   - Volur-Lite 整体对齐 Google 账户交互标准。在 Google Accounts 页面中，只要切换右下角语言，浏览器 Tab 标题与卡片左侧标题会毫秒级无缝联动更新；
2. **无障碍访问（Accessibility & a11y）与屏幕阅读器**：
   - 屏幕阅读器在页面刷新或语言切换后，首先朗读的是 `<title>` 与 `<html lang="...">`。若语言切换后 `document.title` 仍为中文，语音合成引擎会出现中英文乱码或语调冲突；
3. **书签、多标签页与历史记录一致性**：
   - 浏览器历史与书签记录直接捕获 `document.title`。若英文用户保存书签，却记录为中文标题，将极大损坏产品的国际化可用性；
4. **认证状态机流程感知**：
   - 认证流程包含多个子阶段（账号选择、密码验证、人机验证、OTP 验证等），各阶段标签页标题必须精准映射当前步骤与当前语言。

---

### 3.3 推荐解决方案与架构演进路径

#### 方案 A：路由元信息升级为 `titleKey`（声明式国际化键）
将路由元信息中的静态 `title` 升级为 i18n 键名：
```typescript
// src/router/routes/auth.ts
meta: {
  titleKey: 'auth.meta_title_accountchooser', // i18n 键名
  cardTitleKey: 'auth.choose_account',
  cardSubtitleKey: 'auth.choose_account_subtitle',
  requiredAuth: false
}
```

#### 方案 B：全局响应式 Watcher（自动无感刷新）
在根布局或全局应用层建立对 `route` 和 `locale` 的响应式监听：
```typescript
watchEffect(() => {
  const titleKey = route.meta.titleKey
  if (titleKey) {
    document.title = `${t(titleKey)} - Volur`
  } else if (route.meta.title) {
    document.title = `${route.meta.title}`
  }
})
```
- **优势**：
  - 用户在下拉菜单切换语言瞬间，`t(titleKey)` 自动重新求值，`document.title` 在 1ms 内自动刷新；
  - 刷新网页时，路由加载完语言包后立即渲染正确语言标题；
  - 彻底解耦路由静态配置与多语言字典。

#### 方案 C：卡片标题统一接入 `<AuthCardShell :title="t(...)">`
- 视图模板内部统一采用已经封装完毕的 `<AuthCardShell :title="t('...')">`，直接由 Vue Composition API 响应式驱动，天然支持语言动态切换即时更新，无需依赖静态 meta 传值。
