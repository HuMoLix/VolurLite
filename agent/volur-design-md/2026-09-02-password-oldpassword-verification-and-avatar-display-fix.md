# 施工与审计记录：密码修改原密码校验引入与头像回显全流程修复

## 1. 基本信息

- **任务名称**：密码修改接口强化原密码核验、彻底解决前端头像回显只显示首字母的问题
- **执行时间**：2026-09-02
- **影响模块**：
  - `volur-auth`（DTO 增加 `oldPassword` 字段、服务端核验原密码与防新旧密码相同）
  - `volur-lite-view`（TS 实体同步 `oldPassword`、表单渲染与校验、用户资料启动全量同步、头像 URL 跨域/环境安全格式化解析）
- **关联文件**：
  - 后端：
    - [`ChangePasswordRequestDTO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/dto/ChangePasswordRequestDTO.java)
    - [`AuthServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/AuthServiceImpl.java)
  - 前端：
    - [`src/types/auth/index.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/types/auth/index.ts)
    - [`src/utils/auth.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/utils/auth.ts)
    - [`src/App.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/App.vue)
    - [`src/views/profile/ProfileView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/profile/ProfileView.vue)
    - [`src/layout/AppLayout.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/layout/AppLayout.vue)
    - [`src/layout/AdminLayout.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/layout/AdminLayout.vue)

---

## 2. 根因剖析与解答（针对“可以访问到头像API但前端仍显示首字母”）

### 为什么可以直接访问到头像API，但前端页面组件仍显示用户名首字母？

1. **Pinia Store 与 LocalStorage 缓存过期未刷新**：
   - 前端应用在启动时（[`App.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/App.vue)），原逻辑为 `if (userStore.isLoggedIn && !userStore.user)`。由于 `user` 在用户登录时已经存入 `localStorage`，重新打开或刷新页面时 `userStore.user` 始终非空，因此**永远不会主动向后端拉取最新的 `avatarUrl`**。用户即使上传成功，页面一旦刷新或者由 Layout 渲染时，读取的依旧是历史的无头像对象；
2. **Naive UI `<n-avatar>` 相对路径跨域/Host 解析断层**：
   - 后端存储头像生成的地址是相对路径 `/api/v1/uploads/avatars/xxx.png`。
   - 当浏览器通过 `<img>` 渲染时，若前端独立部署或处于不同端口/域名环境下，浏览器默认根据 `window.location.origin` 发起静态请求，不会自动带上后端 BaseURL；
   - 一旦 `<img src="...">` 产生 404/加载错误，Naive UI 会自动切换为 `hasLoadError = true` 并回退渲染默认插槽（即首字母）；
3. **缺少头像 URL 统一格式化器**：
   - 各 Layout 组件及个人中心组件直接使用了原始字符串，缺少统一解析与协议校验（`formatAvatarUrl`）。

---

## 3. 具体修改与功能落地

### 3.1 密码修改引入原密码校验 (`oldPassword`)
1. **后端 DTO 与业务层 (`volur-auth`)**：
   - [`ChangePasswordRequestDTO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/dto/ChangePasswordRequestDTO.java) 增加 `@NotBlank(message = "原密码不能为空") private String oldPassword;`；
   - [`AuthServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/AuthServiceImpl.java) 增加 `passwordEncoder.matches(request.getOldPassword(), user.getPassword())` 校验，原密码错误时拦截；同时校验新旧密码不能相同；
2. **前端交互与表单 (`ProfileView.vue`)**：
   - 增加“原密码”输入框与必填校验；
   - 提交改密时将 `{ oldPassword, token, newPassword }` 完整传递。

### 3.2 头像回显全流程修复
1. **前端统一 URL 解析器 (`src/utils/auth.ts`)**：
   - 封装 `formatAvatarUrl(url)`，自动根据 `VITE_API_BASE_URL` 处理绝对 URL 与相对路径补全；
2. **状态自动更新 (`App.vue`)**：
   - 将启动条件调整为只要处于登录状态（`userStore.isLoggedIn`），立即在后台静默执行 `userStore.fetchUserProfile()` 同步最新头像与资料；
3. **全站布局组件适配 (`AppLayout.vue` / `AdminLayout.vue` / `ProfileView.vue`)**：
   - 所有 `<n-avatar>` 统一通过 `formatAvatarUrl` 绑定 `src`，并赋予响应式 Key 强制重新加载图片。

---

## 4. 验证结论

- **后端测试**：执行 `mvn test`，全平台 12 个模块所有单元测试全部通过（0 失败，0 错误）。
- **前端构建**：执行 `npm run build`，编译与类型检查 100% 通过（0 错误）。
