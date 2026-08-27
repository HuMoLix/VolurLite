# 前端全面响应式集成公开系统配置（平台名称、语言与备案号）

## 1. 变更背景与需求

后端接口 `GET /api/v1/system/settings/public` 返回全站公开配置：
- `system.general.platform_name`：平台全称
- `system.general.platform_desc`：平台描述标语
- `system.general.default_locale`：系统默认初始语言
- `system.general.icp_number`：ICP 备案号
- `system.auth.registration_enabled`：是否开放用户注册
- `system.captcha.enabled`：人机验证是否开启
- `system.maintenance.enabled` & `system.maintenance.notice_message`：全站维护模式开关与公告

前端将这些公开参数全方位响应式绑定至系统的各个视图与组件中，确保后台系统设置发生更改并热重载时，前端能够即时呈现最新的平台名称、语言偏好与备案信息。

---

## 2. 核心改动点

### 2.1 状态管理增强 ([`src/store/modules/system.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/store/modules/system.ts))
- 在 `useSystemStore` 中增加了强类型 Computed Getters：
  - `platformName`: 响应式获取平台名称（默认 `'Volur Lite'`）；
  - `platformDesc`: 响应式获取平台标语（默认 `'High-Performance Distributed Cybersecurity Competition Platform'`）；
  - `icpNumber`: 响应式获取 ICP 备案号；
  - `defaultLocale`: 响应式获取默认语言；
  - `isRegistrationEnabled`: 响应式获取开放注册状态；
  - `isCaptchaEnabled`: 响应式获取人机验证开关。
- 在 `fetchPublicSettings` 中实现：
  - 自动将无本地偏好的用户语言初始化为系统 `defaultLocale`；
  - 动态响应并同步更新网页 `document.title` 后缀。

### 2.2 路由守卫与网页标题动态化 ([`src/router/index.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/router/index.ts))
- 在 `router.beforeEach` 中优先确保加载公开设置，并将网页标题构建为：`${title} | ${systemStore.platformName}`。

### 2.3 页脚与 ICP 备案号联动 ([`Footer.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/components/common/Footer.vue)、[`AppLayout.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/layout/AppLayout.vue)、[`AdminLayout.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/layout/AdminLayout.vue))
- 前台布局、后台管理布局与通用 Footer 全面动态绑定 `systemStore.platformName` 与 `systemStore.platformDesc`；
- 当后台配置了 `system.general.icp_number` 时，页脚自动呈现带工信部官方备案系统链接（`https://beian.miit.gov.cn/`）的 ICP 备案号。

### 2.4 认证页面注册开关与纯净文案 ([`AuthForm.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/components/auth/AuthForm.vue))
- 登录/注册副标题保持纯净的本地化多语言欢迎文案（`t('auth.welcome_subtitle')`）；
- 注册入口与滑块验证联动 `systemStore.isRegistrationEnabled` 与 `systemStore.isCaptchaEnabled`。

---

## 3. 验证结果

- **前端构建**：执行 `npm run build`（`vue-tsc --noEmit && vite build`），0 错误，打包构建成功（`built in 4.34s`）。
