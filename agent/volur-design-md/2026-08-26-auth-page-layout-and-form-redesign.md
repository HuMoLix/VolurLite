# 认证与登录页面布局重构施工记录

**施工日期**：2026-08-26  
**施工模块**：`src/volur-lite-view/src/layout/AuthLayout.vue`、`src/volur-lite-view/src/components/auth/AuthForm.vue`、`src/volur-lite-view/src/views/auth/LoginView.vue`、`src/volur-lite-view/src/components/common/Footer.vue`、`src/volur-lite-view/src/store/modules/system.ts`  
**责任主体**：Antigravity AI Agent

---

## 1. 变更背景与设计分析

参考 `VolurView` 原型设计（`AuthPageLayout.vue`、`AuthView.vue`、`AuthForm.vue`），对 Volur Lite 前端登录与认证体系进行全量重构优化，核心目标包括：
1. **视觉规范与品牌形象统一**：
   - 引入品牌专属 Logo 矢量图 [`public/long-logo.svg`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/public/long-logo.svg)；
   - 提取极简现代排版（标题字距 `-0.5px`、正文字体、主辅色对比与高质感圆角）。
2. **多模式一体化认证表单**：
   - 实现登录（`login`）、注册（`register`）、找回密码（`forgot`）三模态一体化切换表单；
   - 继承行为式/图形验证码刷新机制与邮箱验证码 60 秒倒计时机制。
3. **系统配置动态加载**：
   - 封装 Pinia `useSystemStore` 模块，异步拉取 `/api/v1/system/settings/public` 公开设置，支持 Logo、注册开关与版权文字的动态渲染。

---

## 2. 核心改动点与实施细节

### 2.1 认证页面布局重构 ([`src/layout/AuthLayout.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/layout/AuthLayout.vue))
- **顶部导航栏 (Top Bar)**：
  - 左侧展示 `long-logo.svg` 并支持动态系统 Logo 替换，点击可平滑路由至首页；
  - 右侧提供轻量圆形主题切换按钮（暗色/亮色切换）。
- **主工作区 (Main Auth Content)**：
  - 采用纵向居中 Flex 容器，最大宽度限制为 `440px`，适配移动端（自适应内边距与顶部弹性对齐）。
- **底部页脚 (Footer)**：
  - 引入可复用 [`src/components/common/Footer.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/components/common/Footer.vue)，动态渲染平台版权声明。

### 2.2 多模式认证表单开发 ([`src/components/auth/AuthForm.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/components/auth/AuthForm.vue))
- **表单模式与文案排版**：
  - **登录模式**：标语「欢迎来到 Volur / 开启极客夺旗新视界」，提供用户名/密码校验、忘记密码跳转及验证码扩展插槽；
  - **注册模式**：标语「创建账号 / 注册以开启你的旅程」，提供用户名长度检查（3-20 位）、密码长度检查（≥6 位）、邮箱合法性校验与邮箱验证码倒计时组件；
  - **找回密码模式**：标语「重置密码 / 输入详情以接收重置链接」。
- **API 严格对齐**：
  - 登录：`POST /api/v1/auth/login`
  - 注册：`POST /api/v1/auth/register`
  - 找回密码：`POST /api/v1/auth/forgot-password`
  - 验证码：`GET /api/v1/captcha`
  - 邮箱验证码：`POST /api/v1/email/captcha/register`

### 2.3 系统设置状态流 ([`src/store/modules/system.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/store/modules/system.ts))
- 在 `App.vue` 挂载期自动执行 `systemStore.fetchPublicSettings()`，支持全局读取 `REGISTER_ENABLED`、`ENABLE_CAPTCHA`、`COPYRIGHT_TEXT` 等关键开关。

---

## 3. 验证与构建测试

在 `src/volur-lite-view` 目录下执行编译构建：
```powershell
npm run build
```
**验证结果**：
- `vue-tsc --noEmit`：0 错误；
- `vite build`：生产环境打包成功（耗时 ~5.34s）。

---

## 4. 影响范围与审计评估

- **改动文件**：
  - `src/volur-lite-view/src/layout/AuthLayout.vue`
  - `src/volur-lite-view/src/components/auth/AuthForm.vue`
  - `src/volur-lite-view/src/views/auth/LoginView.vue`
  - `src/volur-lite-view/src/components/common/Footer.vue`
  - `src/volur-lite-view/src/store/modules/system.ts`
  - `src/volur-lite-view/src/App.vue`
- **用户体验提升**：
  - 提供了与 VolurView 视觉高度一致、极具现代感与极客风格的认证交互界面。
