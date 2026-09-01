# 施工与审计记录：个人中心 2FA 验证码人机滑动验证集成与头像静态资源加载调试修复

## 1. 基本信息

- **任务名称**：修复个人中心获取验证码未弹出滑动人机验证与重复提示、修复头像上传后无法正常回显图片的问题
- **执行时间**：2026-09-02
- **影响模块**：
  - `volur-framework`（增强静态文件资源映射路径解析）
  - `volur-auth`（维护模式与鉴权过滤器放行静态资源 `/api/v1/uploads/**`）
  - `volur-lite-view`（集成 `SlidePuzzleModal` 滑动拼图组件、优化 `n-avatar` 响应式重绘与提示去重）
- **关联文件**：
  - 后端：
    - [`WebMvcConfig.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/config/WebMvcConfig.java)
    - [`MaintenanceModeFilter.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/security/filter/MaintenanceModeFilter.java)
    - [`SecurityConfig.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/config/SecurityConfig.java)
  - 前端：
    - [`ProfileView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/profile/ProfileView.vue)

---

## 2. 根因剖析 (Root Cause Analysis)

### 2.1 问题 1：点击获取验证码弹出两次“请先完成人机安全验证”且未出现滑动验证码
1. **未唤起滑动验证码**：后端在 `system.captcha.enabled` 为 `true` 时，`EmailServiceImpl.java` 会严格校验 `captchaTicket`。而前端 [`ProfileView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/profile/ProfileView.vue) 在发送改密/注销验证码时，未判断 `systemStore.isCaptchaEnabled` 状态，直接发起了 HTTP 请求，导致后端直接拒绝并抛出 `400 BAD_REQUEST: "请先完成人机安全验证"`；
2. **重复弹出两次 Toast**：强类型 Axios 封装实例 [`src/utils/request.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/utils/request.ts) 在拦截器中捕获到 400 错误时自动通过 `window.$message.error(message)` 弹出一次，组件内部的 `catch (err)` 代码块又调用了一次 `message.error()`，导致重复弹窗。

### 2.2 问题 2：头像上传成功后前端依然显示首字母而未显示真实头像
1. **静态资源映射与跨域安全访问**：本地存储头像生成的文件路径为 `/api/v1/uploads/avatars/xxx.png`。浏览器发起 `<img>` 加载请求时为匿名加载，后端过滤器（`MaintenanceModeFilter`）未对 `/api/v1/uploads/**` 路径进行无条件放行；
2. **ResourceLocation 路径解析差异**：后端在 Windows 环境下若只配置 `file:uploads/` 相对路径，在多模块微服务启动目录下可能无法准确指向根路径目录；
3. **Naive UI `n-avatar` 组件内部缓存与响应式重绘**：`n-avatar` 在初次图片加载失败回退到字母插槽后，若不强制重新渲染（`key` 变更），部分情况下无法立即感知到外部 `src` 变量的变化。

---

## 3. 修复方案与执行动作

### 3.1 人机验证与滑动拼图集成 (`ProfileView.vue`)
1. **集成 `SlidePuzzleModal` 组件**：
   - 引入 [`SlidePuzzleModal.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/components/verification/SlidePuzzleModal.vue)；
   - 点击“获取验证码”时，若 `systemStore.isCaptchaEnabled` 为 `true`，优先弹出滑动拼图弹窗（场景设置为 `CHANGE_PASSWORD`）；
   - 滑动验证通过后，获取服务端签发的 `CaptchaTicketVO` 并附带在请求体中发送，无缝完成 2FA 邮件派发；
2. **Toast 提示去重**：组件内部移除冗余的 `message.error()`，交由全局 Axios 拦截器统一提示，杜绝重复弹窗。

### 3.2 静态资源访问与头像回显修复
1. **后端增强资源路径映射 (`WebMvcConfig.java`)**：
   - 采用 `Paths.get("uploads").toAbsolutePath().normalize().toUri().toString()` 动态生成绝对路径 URI，兼容绝对路径与相对路径（`uploadUri`, `file:uploads/`, `file:./uploads/`），确保跨操作系统与启动路径时资源均能准确命中；
2. **安全过滤器放行 (`MaintenanceModeFilter.java`)**：
   - 在 `EXEMPT_PATHS` 白名单中增加 `/api/v1/uploads/**`，保证头像等公共上传文件全局无阻碍加载；
3. **前端头像响应式更新 (`ProfileView.vue`)**：
   - 引入 `avatarDisplayUrl` 计算属性（优先使用 `profile.avatarUrl`，回退至 `userStore.user.avatarUrl`）；
   - 为 `<n-avatar>` 绑定 `:key="avatarDisplayUrl"`，当上传成功并更新 URL 后，强制触发组件重建并加载最新图片；
   - 在 `handleAvatarCropSuccess` 中同时刷新当前页面 Profile 与 Pinia Store 中的用户资料。

---

## 4. 验证与审计结论

- **后端测试**：全平台 12 个模块通过 `mvn test-compile` 与 `mvn test` 编译测试，0 错误。
- **前端构建**：通过 `npm run build` 打包构建，TypeScript 类型校验 100% 通过（0 错误）。
