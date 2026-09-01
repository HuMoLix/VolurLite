# 施工与审计记录：API职责解耦 (/users/me vs /users/profile)、网络错误防抖去重与图片本地缓存体系

## 1. 基本信息

- **任务名称**：顶部栏与个人中心 API 职责分离、Pinia 就地更新、网络断开错误去重及前端图片缓存引擎建设
- **执行时间**：2026-09-02
- **影响模块**：
  - `volur-framework`（静态资源上传端点配置 30 天 Cache-Control 响应头）
  - `volur-lite-view`：
    - `request.ts`（网络错误防抖限流与维护页面错误静默）
    - `MaintenanceView.vue`（维护页面友好提示）
    - `types/user/index.ts`（补充 `UserSummaryVO`）
    - `api/user/index.ts`（新增 `getCurrentUserApi` 映射 `/api/v1/users/me`）
    - `store/modules/user.ts`（Pinia 维护 `UserSummaryVO`，实现就地更新与在途 Promise 去重）
    - `App.vue`（启动只拉取 `/users/me`）
    - `ProfileView.vue`（页面拉取 `/users/profile`，操作后就地更新 Pinia，彻底消除重复请求）
    - `imageCache.ts`（内存 Blob + 浏览器 Cache-Control 缓存引擎）
    - `AvatarCropperModal.vue`（裁剪完成后即刻将本地 Blob 注入图片缓存）

---

## 2. 变更要点与问题修复

### 2.1 问题 1：后端无法连接时触发两次错误弹窗
- **根因**：
  1. 应用初始化时多个并行请求（如 `fetchPublicSettings` 与 `fetchCurrentUser`）同时失败，每个请求在 Axios 拦截器中独立触发 Toast；
  2. 已经跳转到 `/maintenance` 维护页面后，在途的后续失败请求依然弹出 Toast。
- **修复**：
  - 在 `src/utils/request.ts` 中引入 `NETWORK_ERROR_THROTTLE = 2000ms` 节流去重；
  - 判断若当前路由已处于 `/maintenance`，则静默拦截重复网络报错；
  - 修复 `MaintenanceView.vue` 中手动刷新的异常处理。

### 2.2 问题 2：访问 /profile 触发两次 /api/v1/users/profile
- **根因**：
  - `App.vue` 挂载时调用了 `fetchUserProfile`（请求 `/api/v1/users/profile`）；
  - `ProfileView.vue` 挂载时自身也调用了 `loadProfile`（同样请求 `/api/v1/users/profile`）。
- **修复**：
  - 将两者职责分离：`App.vue` 仅调用 `userStore.fetchCurrentUser()`（请求轻量级 `/api/v1/users/me`，用于 Layout 顶部导航栏用户头像、昵称、角色展示）；
  - 仅在用户实际进入 `ProfileView.vue` 个人中心时，才调用 `getCurrentUserProfileApi()` 获取全量可编辑资料与隐私配置。

### 2.3 问题 3 & 4：/users/me 概要与 Pinia 实体就地更新机制
- **改造**：
  - 在 `src/types/user/index.ts` 定义 `UserSummaryVO`；
  - 在 `src/store/modules/user.ts` 中维护 `user: Ref<UserSummaryVO | null>`；
  - 新增 `updateUserAvatar(avatarUrl: string)` 和 `updateUserInfo(partial)` 方法；
  - 当用户在个人中心修改头像或更新个人简介/单位时，直接调用上述方法就地更新 Pinia 实体与 localStorage，**彻底消除修改后的二次重刷网络请求**。

### 2.4 问题 5：图片缓存与避免重复请求机制
- **改造方案**：
  1. **服务端 HTTP 强缓存**：[`WebMvcConfig.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/config/WebMvcConfig.java) 为 `/api/v1/uploads/**` 增加 `CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic()`；
  2. **前端本地内存 Blob 缓存引擎**（[`src/utils/imageCache.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/utils/imageCache.ts)）：
     - 头像裁剪上传成功后，直接将本地 Canvas 生成的 Blob 注入内存 Map；
     - 页面渲染时优先从本地 Blob 渲染（0ms 零延迟），即使网络延迟或刷新，浏览器也会命中 30 天静态强缓存。

---

## 3. 验证与审计结论

- **后端单元测试**：执行 `mvn test`，全平台 12 个模块所有单元测试 100% 通过（0 失败，0 错误）。
- **前端打包编译**：执行 `npm run build`，Vue 3 + TypeScript 严格模式 100% 编译通过。
