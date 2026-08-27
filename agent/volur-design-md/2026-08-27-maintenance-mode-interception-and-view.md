# 全站维护模式全链路拦截与维护通知页面设计实施文档

**文档编号**：ARCH-20260827-SYS-MAINTENANCE  
**日期**：2026-08-27  
**作者**：Antigravity Agent  
**模块范围**：`volur-system`、`volur-auth`、`volur-lite-view`  

---

## 1. 业务背景与改造目标

为了在底层演练环境升级、集群变更或紧急安全风控时实现对平台的全站维护管制，提出以下三项核心控制要求：
1. **维护期间禁止普通用户登录与注册**：全站维护开启时，仅限具有管理员权限（`ADMIN` / `SUPER_ADMIN`）的用户登录管理后台，普通用户禁止登录与注册；
2. **全局流量与接口拦截**：全站维护开启时，普通用户与匿名访客除读取公开配置（`GET /api/v1/system/settings/public`）、管理登录、验证码及 Swagger 文档等必要白名单接口外，其他所有接口一律拦截并返回 HTTP 503 与维护公告通知内容；
3. **前端自动化状态感知与路由跳转**：前端获取 `public_settings` 后若识别到处于维护模式，非管理员用户自动重定向到专用的 `/maintenance` 维护通知页面，实时展示后台配置的维护公告内容，并提供“刷新检测”与“管理员通道”。

---

## 2. 核心架构设计与改动点

### 2.1 后端公开配置注入 (`volur-system`)
- **文件**：[`SystemSettingServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/service/impl/SystemSettingServiceImpl.java)
- **改动点**：在 `getPublicSettingsMap()` 中强制注入维护状态与通知文本：
  - `system.maintenance.enabled` / `MAINTENANCE_MODE`
  - `system.maintenance.notice_message` / `MAINTENANCE_NOTICE`
- **效果**：确保前端在任何时刻（包括未登录状态下）均能无阻碍获取最新的全站维护状态与维护公告。

---

### 2.2 登录注册拦截控制 (`volur-auth`)
- **文件**：[`AuthServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/AuthServiceImpl.java)
- **改动点**：
  - **`register`**：维护模式下直接抛出 `BusinessException(HttpStatus.SERVICE_UNAVAILABLE, notice)`，禁止任何新账号注册；
  - **`authenticate`**：登录凭证校验通过后，判定当前用户权限。若维护模式开启且权限非管理员，则抛出 `BusinessException(HttpStatus.SERVICE_UNAVAILABLE, notice)` 并拒绝发放 Token。

---

### 2.3 全局维护过滤器 (`MaintenanceModeFilter.java` & `SecurityConfig.java`)
- **文件**：[`MaintenanceModeFilter.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/security/filter/MaintenanceModeFilter.java)
- **改动点**：
  - 继承 `OncePerRequestFilter`，挂载在 `SecurityConfig` 中的 `JwtAuthFilter` 之后；
  - 若 `MAINTENANCE_MODE == true`：
    1. 放行 OPTIONS 跨域预检与白名单（`/api/v1/system/settings/public`, `/api/v1/auth/login`, `/api/v1/captcha/**`, `/swagger-ui/**`, 等）；
    2. 对已由 `JwtAuthFilter` 解析并持有 `ROLE_ADMIN` 的请求予以放行；
    3. 其余一切请求（普通用户、匿名用户）直接拦截并返回 HTTP 503 JSON：
       ```json
       {
         "code": 503,
         "message": "维护公告通知内容...",
         "data": null
       }
       ```

---

### 2.4 前端维护感知、路由守卫与维护视图 (`volur-lite-view`)
- **状态存储**：[`src/store/modules/system.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/store/modules/system.ts)
  - 导出 `isMaintenanceMode` 与 `maintenanceNotice` 计算属性。
- **维护视图**：[`src/views/error/MaintenanceView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/error/MaintenanceView.vue)
  - 采用极简设计，包含维护图标、全站维护标题、维护公告通知卡片、刷新检测按钮与管理员通道。
- **路由守卫与拦截**：
  - 在 [`src/router/routes.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/router/routes.ts) 注册 `/maintenance` 路由；
  - 在 [`src/router/index.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/router/index.ts) 中强化 `router.beforeEach`，非管理员访问普通页面时自动重定向至 `/maintenance`；维护恢复后访问 `/maintenance` 自动跳回首页；
  - 在 [`src/App.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/App.vue) 挂载期执行维护态路由跳转；
  - 在 [`src/utils/request.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/utils/request.ts) 响应拦截器中针对 503 异常自动引导至 `/maintenance` 页面。

---

## 3. 验证结果

1. **后端编译与自动化单元测试**：
   - `mvn clean test-compile -pl volur-auth,volur-system -am` -> **BUILD SUCCESS**
   - `mvn test -pl volur-auth,volur-system,volur-verification -am` -> **BUILD SUCCESS (15 tests passed, 0 failures)**
2. **前端编译与类型检查**：
   - `npm run build` -> **BUILD SUCCESS**
