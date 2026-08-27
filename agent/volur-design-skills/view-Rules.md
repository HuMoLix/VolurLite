# Volur Lite 前端开发与设计规范 (view-Rules.md)

本文档定义 Volur Lite 前端项目（`src/volur-lite-view`）的核心架构约束、设计规范与 TypeScript / ES2026 编码标准。所有前端模块的新增开发、组件编写、全局状态管理、网络请求以及代码重构均须严格遵循本规范。

---

## 1. 技术栈与架构基准 (Tech Stack & Architecture)

Volur Lite 前端采用现代化轻量高可用技术体系构建：
- **基础框架**：Vue 3 (全面基于 `<script setup lang="ts">` 与 Composition API)
- **语言标准**：**ECMAScript 2026 (ES2026)**（强制基准，开启现代语言特性与原生异步能力）
- **构建工具**：Vite (ESNext 模块化、按需编译、极速 HMR、`target: 'esnext'`)
- **开发语言**：TypeScript (严格模式 `strict: true`，`target: 'ESNext'` / `lib: ['ESNext']`，杜绝 `any` 泛滥)
- **全局状态管理**：**Pinia** (统一强制采用 Composition API Setup Store 语法风格，杜绝 Options Store)
- **样式方案**：SCSS (模块化 SCSS、BEM 规范、CSS 变量与 Mixins 现代预处理器注入)
- **组件库**：Naive UI (轻量、TS 原生支持、高性能暗色/亮色主题系统)
- **组件字体库**：`vfonts` (导入 Lato、Fira Code 等高品质排版与等宽字体)
- **路由管理**：Vue Router 4 (HTML5 History 模式、强类型路由元信息与权限守卫)
- **网络通信**：Axios (强类型 `ApiResponse<T>`、无感 Token 刷新机制、SSE 实时流订阅)

---

## 2. ECMAScript 2026 (ES2026) 语言与编译规范

### 2.1 强制基准与编译器配置
本工程强制以 **ES2026** 作为统一语言标准。严禁降级使用 ES2020 或更旧的编译目标配置。

#### `tsconfig.json` 配置规范：
```json
{
  "compilerOptions": {
    "target": "ESNext",
    "lib": ["ESNext", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "moduleResolution": "bundler",
    "strict": true,
    "isolatedModules": true,
    "noEmit": true
  }
}
```

#### `vite.config.ts` 配置规范：
```typescript
export default defineConfig({
  build: {
    target: 'esnext',
    modulePreload: {
      polyfill: false
    }
  },
  esbuild: {
    target: 'esnext'
  }
})
```

### 2.2 现代 ES2026 语言特性应用指南
在编写业务代码与工具库时，应优先采用 ES2026 现代化语言特性：
1. **顶层 Await (Top-level Await)**：在模块顶层直接使用 `await` 进行异步初始化；
2. **`Promise.withResolvers()`**：简化 Promise 的手动状态控制，避免冗余的闭包提升；
3. **结构化分组 (`Object.groupBy` / `Map.groupBy`)**：高效处理列表分类聚合，严禁手写繁琐的 `reduce` 分组；
4. **集合运算原生方法 (Set Methods)**：原生使用 `Set.prototype.union()`, `intersection()`, `difference()`, `isSubsetOf()` 等进行赛题标签与权限比对；
5. **显式资源管理与清理 (`using` / `await using`)**：利用 `Symbol.dispose` / `Symbol.asyncDispose` 自动释放 SSE 连接与定时器资源；
6. **空值与逻辑赋值语法**：广泛使用可选链 (`?.`)、空值合并 (`??`) 以及逻辑赋值运算符 (`??=`, `||=`, `&&=`)。

---

## 3. Pinia 全局状态管理规范 (Pinia State Management)

### 3.1 核心原则与设计约束
1. **统一 Setup Store 风格**：所有 Store 必须使用 `defineStore('id', () => { ... })` 组合式函数语法，**严禁使用陈旧的 Options API（`state()`, `actions`, `getters`）写法**。
2. **响应式状态声明**：
   - 基础类型与对象引用统一使用 `ref<T>()`；
   - 衍生计算属性统一使用 `computed()` 作为 Store Getters；
   - 异步操作与业务命令直接声明为 `async function` 作为 Store Actions。
3. **模块化目录收敛**：
   - 所有状态模块统一放置在 `src/store/modules/{module}.ts`（如 `user.ts`, `app.ts`, `notification.ts`, `competition.ts`）；
   - 在 `src/store/index.ts` 中统一聚合实例化与导出。
4. **状态隔离与单一职责**：
   - `user`：用户身份、Token、个人资料与权限角色；
   - `app`：全局主题（深色/浅色）、侧边栏折叠状态、国际化；
   - `notification`：站内未读通知数、全局广播消息队列；
   - `competition`：当前正在进行/观看的比赛详情与实时排行榜状态。

### 3.2 [反模式 vs 推荐实践] Pinia Store 编写

#### ❌ 反模式 (使用陈旧 Options API、缺乏强类型推导、隐式 this 污染)
```typescript
// 错误示例: Options Store 风格 (已废弃模式)
import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: null as any,
    user: null as any
  }),
  getters: {
    isLoggedIn: (state) => !!state.token
  },
  actions: {
    async login(data: any) {
      // 缺失强类型与组合式 API 优势
    }
  }
})
```

#### ✅ 推荐实践 (现代 Setup Store、强类型 TS 对齐、组合式响应)
```typescript
// src/store/modules/user.ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { AuthenticateRequestDTO, AuthenticateResponseVO } from '@/types/auth'
import type { UserProfileResponseVO } from '@/types/user'
import { loginApi, logoutApi } from '@/api/auth'
import { getCurrentUserProfileApi } from '@/api/user'
import { getAccessToken, setAccessToken, setRefreshToken, clearAuthStorage } from '@/utils/auth'

export const useUserStore = defineStore('user', () => {
  // State
  const token = ref<string | null>(getAccessToken())
  const user = ref<UserProfileResponseVO | null>(null)

  // Getters
  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ROLE_SUPER_ADMIN' || user.value?.role === 'ROLE_ADMIN')

  // Actions
  async function login(credentials: AuthenticateRequestDTO): Promise<AuthenticateResponseVO> {
    const res = await loginApi(credentials)
    token.value = res.data.accessToken
    setAccessToken(res.data.accessToken)
    setRefreshToken(res.data.refreshToken)
    await fetchUserProfile()
    return res.data
  }

  async function fetchUserProfile(): Promise<UserProfileResponseVO | null> {
    try {
      const res = await getCurrentUserProfileApi()
      user.value = res.data
      return res.data
    } catch {
      return null
    }
  }

  async function logout(): Promise<void> {
    try {
      if (token.value) await logoutApi()
    } finally {
      token.value = null
      user.value = null
      clearAuthStorage()
    }
  }

  return {
    token,
    user,
    isLoggedIn,
    isAdmin,
    login,
    logout,
    fetchUserProfile
  }
})
```

---

## 4. 目录结构与后端模块化对齐规范 (Directory Structure Alignment)

前端目录结构全面参考并对齐后端微内核架构（`src/volur-lite-platform`），实现 API 接口层、领域模型层（Types）、业务页面层与后端模块的 **1:1 映射与镜像收敛**。

### 4.1 标准目录树
```
src/volur-lite-view/
├── public/
│   ├── favicon.svg                  # 站点图标
├── src/
│   ├── api/                         # API 请求函数层 (与后端 volur-* 模块 1:1 对齐)
│   │   ├── auth/                    # 认证授权接口 (对应 volur-auth 控制器)
│   │   ├── user/                    # 用户管理接口 (对应 volur-user 控制器)
│   │   ├── verification/            # 验证码/凭证接口 (对应 volur-verification 控制器)
│   │   ├── challenge/               # 题库/赛题接口 (对应 volur-challenge 控制器)
│   │   ├── competition/             # 竞赛演练接口 (对应 volur-competition 控制器)
│   │   ├── container/               # 靶机容器接口 (对应 volur-container 控制器)
│   │   ├── notification/            # 站内通知/实时广播 (对应 volur-notification 控制器)
│   │   ├── system/                  # 系统设置/审计接口 (对应 volur-system 控制器)
│   │   └── index.ts                 # 统一聚合导出
│   ├── types/                       # TypeScript 实体定义层 (与后端 domain.* 1:1 对齐)
│   │   ├── common/                  # 通用模型 (ApiResponse, PageResponse, CursorResponse, SliceResponse)
│   │   ├── auth/                    # 认证模型 (AuthenticateRequestDTO, AuthenticateResponseVO 等)
│   │   ├── user/                    # 用户模型 (UserProfileResponseVO, UserDetailsVO 等)
│   │   ├── verification/            # 验证码模型 (CaptchaResponseVO, CaptchaTicketVO 等)
│   │   ├── challenge/               # 题目模型 (CtfChallengeListVO, CtfChallengeDetailVO 等)
│   │   ├── competition/             # 竞赛模型 (CompetitionDetailVO, CompositeScoreboardVO 等)
│   │   ├── container/               # 靶机模型 (ChallengeInstanceResponseVO 等)
│   │   ├── notification/            # 通知模型 (NotificationVO, BroadcastMessageVO 等)
│   │   ├── system/                  # 系统模型 (SystemSettingVO 等)
│   │   ├── naive.d.ts               # Naive UI 全局 window 注入声明
│   │   ├── router.d.ts              # Vue Router RouteMeta 扩展类型
│   │   └── index.ts                 # 统一聚合导出
│   ├── router/                      # 路由定义与权限守卫
│   │   ├── index.ts                 # 路由实例与全局拦截守卫
│   │   └── routes.ts                # 静态与动态路由表配置
│   ├── store/                       # Pinia 全局状态管理
│   │   ├── modules/                 # Setup Store 分模块 (user, app, notification, competition 等)
│   │   └── index.ts                 # Pinia 根入口与聚合导出
│   ├── layout/                      # 全局布局组件 (AppLayout, AuthLayout 等)
│   ├── views/                       # 业务视图页面 (按业务功能模块划分)
│   │   ├── auth/                    # 登录、注册、找回密码视图
│   │   ├── home/                    # 平台首页
│   │   ├── challenge/               # 题库训练大厅与答题详情
│   │   ├── competition/             # 竞赛大厅、赛段演练与排行榜
│   │   └── error/                   # 403, 404, 500 异常缺省页
│   ├── components/                  # 通用/业务可复用组件
│   │   └── common/                  # 全局基础组件 (如 NaiveProvider 等)
│   ├── utils/                       # 核心通用工具库
│   │   ├── request.ts               # 强类型 Axios 请求实例与双拦截器
│   │   ├── auth.ts                  # Token 本地存储与凭据存取
│   │   └── sse.ts                   # Server-Sent Events 客户端工具
│   ├── styles/                      # 全局样式与 SCSS 变量/Mixins
│   │   ├── variables.scss           # 颜色、尺寸、动效全局变量
│   │   ├── mixins.scss              # Flex、文本省略、毛玻璃卡片等公共 Mixins
│   │   ├── reset.scss               # 基础标签样式重置
│   │   └── index.scss               # 全局入口样式表
│   ├── App.vue                      # 根组件 (Naive UI Providers 体系)
│   ├── env.d.ts                     # 环境变量与 SFC 声明
│   └── main.ts                      # 客户端启动入口 (引入 vfonts、挂载应用)
├── index.html                       # HTML 模板入口
├── package.json                     # 依赖与脚本定义
├── tsconfig.json                    # TypeScript 编译配置 (ES2026/ESNext)
└── vite.config.ts                   # Vite 构建配置 (target: esnext)
```

---

## 5. API 请求路径与后端 Controller 路由严格 1:1 对齐规范 (API Route Alignment Standard)

> [!IMPORTANT]
> **核心强制约束**：前端所有在 `src/api/{module}/*.ts` 中定义的请求函数，其 **HTTP Method**（`GET` / `POST` / `PUT` / `DELETE` / `PATCH`）与 **请求 URI 路径**，**必须 100% 严格与后端对应 Controller 类级别 `@RequestMapping` 及方法级别注解（`@GetMapping` / `@PostMapping` / `@PutMapping` / `@DeleteMapping` / `@PatchMapping`）的路由声明保持一致**。严禁前端凭空臆测、主观拼凑不存在的子路径或使用非标准动词！

### 5.1 路由对齐核心设计原则
1. **Base URL 作用域约定**：
   前端 Axios 统一配置了 `baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1'`。
   因此，后端 Controller 类上的 `@RequestMapping("/api/v1/{module-path}")` 在前端 `request` 请求时，**必须直接使用 `/api/v1` 之后的相对路径**。
2. **严禁臆测不存在的动作子路径**：
   - ❌ **严禁**擅自添加 `/list`, `/getList`, `/fetch`, `/query`, `/doLogin`, `/updateInfo` 等非 Controller 声明的子路径。
   - ✅ **标准 RESTful 映射**：若后端 Controller 类级别为 `@RequestMapping("/api/v1/competitions")`，方法上为 `@GetMapping`（获取列表），前端请求路径必须为 `request.get('/competitions')`（拼装 baseURL 即为 `/api/v1/competitions`），**绝不允许写成 `request.get('/competition/list')`**！
3. **HTTP 动词严格映射**：
   - `@GetMapping` -> `request.get(...)`
   - `@PostMapping` -> `request.post(...)`
   - `@PutMapping` -> `request.put(...)`
   - `@PatchMapping` -> `request.patch(...)`
   - `@DeleteMapping` -> `request.delete(...)`
4. **路径参数与模板字符串**：
   - 后端 `@GetMapping("/{id}")` -> 前端 `request.get('/competitions/' + id)` 或 `request.get(\`/competitions/\${id}\`)`；
   - 后端 `@GetMapping("/by-slug/{slug}")` -> 前端 `request.get(\`/competitions/by-slug/\${slug}\`)`。

---

### 5.2 [反模式 vs 推荐实践] 路由对齐剖析

#### ❌ 反模式 (臆测路径、单复数不分、拼凑非标准动词)
```typescript
// 错误示例 1: 臆测加了 /list，且将复数 competitions 误写为单数 competition
// 后端实际为: @RequestMapping("/api/v1/competitions") 下的 @GetMapping
export function getCompetitionListApi(params: any) {
  return request.get('/competition/list', { params }) // ❌ 404 错误！后端根本没有 /competition/list
}

// 错误示例 2: 动词不匹配 (后端是 PATCH 接口，前端用了 POST)
// 后端实际为: @PatchMapping("/{id}/read")
export function markNotificationReadApi(id: number) {
  return request.post(`/notification/${id}/read`) // ❌ 405 Method Not Allowed！
}

// 错误示例 3: 臆测拼装用户资料路径
// 后端实际为: @RequestMapping("/api/v1/users") 下的 @GetMapping("/profile")
export function getUserProfileApi() {
  return request.get('/user/getUserProfile') // ❌ 404 错误！
}
```

#### ✅ 推荐实践 (100% 对齐后端 Controller 路由与动词)

```java
// 后端 Java Controller (CompetitionController.java)
package com.humolix.volur.competition.controller.v1;

@RestController
@RequestMapping("/api/v1/competitions")
public class CompetitionController {

    @GetMapping
    public ApiResponse<PageResponse<CompetitionSummaryVO>> listCompetitions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return ApiResponseEntity.ok("获取竞赛列表成功", competitionService.listCompetitions(page, size, keyword));
    }

    @GetMapping("/{id}")
    public ApiResponse<CompetitionDetailVO> getCompetitionById(@PathVariable Long id) {
        return ApiResponseEntity.ok("获取竞赛详情成功", competitionService.getCompetitionById(id));
    }
}
```

```typescript
// 前端 TypeScript API (src/api/competition/index.ts)
import { request } from '@/utils/request'
import type { ApiResponse, PageResponse } from '@/types/common/api'
import type { CompetitionSummaryVO, CompetitionDetailVO } from '@/types/competition'

/**
 * 分页获取竞赛列表
 * 对应后端: GET /api/v1/competitions
 */
export function getCompetitionListApi(params?: {
  page?: number
  size?: number
  keyword?: string
}): Promise<ApiResponse<PageResponse<CompetitionSummaryVO>>> {
  return request.get<PageResponse<CompetitionSummaryVO>>('/competitions', { params })
}

/**
 * 根据 ID 获取竞赛详情
 * 对应后端: GET /api/v1/competitions/{id}
 */
export function getCompetitionByIdApi(id: number | string): Promise<ApiResponse<CompetitionDetailVO>> {
  return request.get<CompetitionDetailVO>(`/competitions/${id}`)
}
```

---

### 5.3 核心模块 Controller 路由对照矩阵表

| 业务模块 | 后端 Controller 类 | HTTP 动词 | 后端注解路径 | 前端 Axios 请求路径 (`baseURL: '/api/v1'`) |
| :--- | :--- | :--- | :--- | :--- |
| **竞赛模块** | `CompetitionController` | `GET` | `@RequestMapping("/api/v1/competitions")` + `@GetMapping` | `request.get('/competitions', { params })` |
| | `CompetitionController` | `GET` | `@GetMapping("/{id}")` | `request.get(\`/competitions/\${id}\`)` |
| | `CompetitionController` | `GET` | `@GetMapping("/by-slug/{slug}")` | `request.get(\`/competitions/by-slug/\${slug}\`)` |
| | `CompetitionScoreboardController` | `GET` | `@RequestMapping("/api/v1/competitions/{competitionId}/scoreboard")` | `request.get(\`/competitions/\${competitionId}/scoreboard\`)` |
| | `CompetitionTeamController` | `POST` | `@RequestMapping("/api/v1/competitions/{competitionId}/teams")` | `request.post(\`/competitions/\${competitionId}/teams\`, data)` |
| | `CompetitionTeamController` | `POST` | `@PostMapping("/invite")` | `request.post(\`/competitions/\${competitionId}/teams/invite\`, data)` |
| | `CompetitionTeamController` | `GET` | `@GetMapping("/my")` | `request.get(\`/competitions/\${competitionId}/teams/my\`)` |
| | `CtfStageController` | `POST` | `@RequestMapping("/api/v1/competitions/{competitionId}/stage/ctf")` + `.../submit` | `request.post(\`/competitions/\${competitionId}/stage/ctf/challenges/\${challengeId}/submit\`, data)` |
| **认证模块** | `AuthController` | `POST` | `@RequestMapping("/api/v1/auth")` + `@PostMapping("/login")` | `request.post('/auth/login', data)` |
| | `AuthController` | `POST` | `@PostMapping("/register")` | `request.post('/auth/register', data)` |
| | `TokenController` | `POST` | `@RequestMapping("/api/v1/auth")` + `@PostMapping("/refresh")` | `request.post('/auth/refresh', data)` |
| | `TokenController` | `POST` | `@PostMapping("/logout")` | `request.post('/auth/logout')` |
| **用户模块** | `UserController` | `GET` | `@RequestMapping("/api/v1/users")` + `@GetMapping("/profile")` | `request.get('/users/profile')` |
| | `UserController` | `GET` | `@GetMapping("/me")` | `request.get('/users/me')` |
| | `AdminUserController` | `GET` | `@RequestMapping("/api/v1/admin/users")` + `@GetMapping` | `request.get('/admin/users', { params })` |
| | `AdminUserController` | `DELETE` | `@DeleteMapping("/{id}")` | `request.delete(\`/admin/users/\${id}\`)` |
| **验证模块** | `CaptchaController` | `GET` | `@RequestMapping("/api/v1/captcha")` + `@GetMapping("/challenge")` | `request.get('/captcha/challenge', { params })` |
| | `CaptchaController` | `POST` | `@PostMapping("/verify-ticket")` | `request.post('/captcha/verify-ticket', data)` |
| | `EmailController` | `POST` | `@RequestMapping("/api/v1/email")` + `@PostMapping("/captcha/register")` | `request.post('/email/captcha/register', data)` |
| **通知模块** | `NotificationController` | `GET` | `@RequestMapping("/api/v1/notifications")` + `@GetMapping` | `request.get('/notifications', { params })` |
| | `NotificationController` | `GET` | `@GetMapping("/unread-count")` | `request.get('/notifications/unread-count')` |
| | `NotificationController` | `PATCH` | `@PatchMapping("/{id}/read")` | `request.patch(\`/notifications/\${id}/read\`)` |
| | `NotificationController` | `PATCH` | `@PatchMapping("/read-all")` | `request.patch('/notifications/read-all')` |
| **靶机容器** | `CtfInstanceController` | `POST` | `@RequestMapping("/api/v1/challenges/instances")` + `/{id}/start` | `request.post(\`/challenges/instances/\${id}/start\`)` |
| | `CtfInstanceController` | `POST` | `@PostMapping("/{id}/stop")` | `request.post(\`/challenges/instances/\${id}/stop\`)` |
| **系统模块** | `SystemSettingController` | `GET` | `@RequestMapping("/api/v1/system/settings")` + `/public` | `request.get('/system/settings/public')` |
| | `AdminSystemSettingController` | `POST` | `@RequestMapping("/api/v1/admin/system/settings")` + `/update` | `request.post('/admin/system/settings/update', data)` |

---

## 6. 前后端 API 实体严格对齐规范 (TS API & Domain Entity Alignment)

### 6.1 核心原则与设计约束
1. **1:1 镜像对齐**：前端在 `src/types/{module}` 中定义的 TypeScript 接口，必须与后端 `com.humolix.volur.{module}.domain` 下的 Java 实体保持 **命名、字段结构、驼峰格式和语义的严格 1:1 对齐**。
2. **区分 DTO 与 VO**：
   - **请求入参（DTO）**：命名以 `*DTO` 或 `*RequestDTO` 结尾（如 `AuthenticateRequestDTO`, `RegisterRequestDTO`），对应后端 `domain.dto.*DTO`。
   - **响应视图（VO）**：命名以 `*VO` 或 `*ResponseVO` 结尾（如 `AuthenticateResponseVO`, `CtfChallengeListVO`），对应后端 `domain.vo.*VO`。
3. **消除 `any` 与类型断言反模式**：
   - 严禁在 API 响应和请求入参中使用 `any` 类型。
   - 所有接口函数的参数与返回值必须附带明确的泛型标注（如 `Promise<ApiResponse<UserProfileResponseVO>>`）。
4. **雪花算法 ID 精度保护**：
   - 后端使用 64 位雪花算法（Snowflake ID）生成的 ID，由于 JavaScript `Number.MAX_SAFE_INTEGER`（$2^{53}-1$）的精度限制，前端 TS 实体中统一声明为 `number | string` 或 `string`，杜绝大整数精度丢失。
5. **可选值与 Nullability 标注**：
   - 后端允许为 `null` 或可能不存在的字段，必须在 TS 接口中显式使用可选修饰符 `?`（如 `avatar?: string`）或联合类型 `string | null`。

---

### 6.2 通用包装响应模型映射表 (Common Generic Records)

所有后端返回的顶层响应均严格封装在 `ApiResponse<T>` 中，分页响应统一封装在三种轻量 Record 中：

| 后端 Java Record (`com.humolix.volur.framework.common.api`) | 前端 TypeScript Interface (`@/types/common/api.ts`) | 业务应用场景 |
| :--- | :--- | :--- |
| `ApiResponse<T>` | `ApiResponse<T>` | 全局所有 HTTP 请求的基础包装结构 |
| `PageResponse<T>` | `PageResponse<T>` | 管理端后台表格、支持精准总数与跳页的传统列表 |
| `CursorResponse<T>` | `CursorResponse<T>` | 站内通知信箱、实时动态流、无限滚动场景 |
| `SliceResponse<T>` | `SliceResponse<T>` | 公开题库广场、高并发顺序切片浏览场景 |

#### 前端通用接口标准定义 (`src/types/common/api.ts`)：
```typescript
/**
 * 统一 API 响应包装模型 (对应 com.humolix.volur.framework.common.api.ApiResponse)
 */
export interface ApiResponse<T = any> {
  status: number
  errorCode: number
  success: boolean
  message: string
  data: T
  timestamp: number
  path: string
}

/**
 * 标准传统分页响应 (对应 com.humolix.volur.framework.common.api.PageResponse)
 */
export interface PageResponse<T> {
  list: T[]
  total: number
  totalPages: number
  currentPage: number
}

/**
 * 游标 / 流式无限滚动分页响应 (对应 com.humolix.volur.framework.common.api.CursorResponse)
 */
export interface CursorResponse<T> {
  list: T[]
  nextPageToken: string | null
  hasMore: boolean
}

/**
 * 无总数切片分页响应 (对应 com.humolix.volur.framework.common.api.SliceResponse)
 */
export interface SliceResponse<T> {
  list: T[]
  hasNext: boolean
  hasPrevious: boolean
  currentPage: number
  pageSize: number
}

/**
 * 标准分页查询入参 DTO
 */
export interface PageQuery {
  page?: number
  size?: number
  sort?: string
}

/**
 * 游标分页查询入参 DTO
 */
export interface CursorQuery {
  pageToken?: string
  size?: number
}
```

---

### 6.3 类型转换对照基准 (Java <-> TypeScript Mapping Matrix)

在将后端 Java 实体转译为前端 TS 接口时，必须遵守以下映射规则：

| Java 类型 | TypeScript 类型 | 转换规则与注意事项 |
| :--- | :--- | :--- |
| `String` | `string` | 字符型直接对齐 |
| `Long` / `long` (数据库主键/雪花ID) | `number \| string` | 优先推荐联合类型或 `string`，防范 JS 精度溢出 |
| `Integer` / `int` / `Short` / `Byte` | `number` | 整数直接映射 |
| `Double` / `Float` / `BigDecimal` | `number` | 浮点数/金额百分比映射 |
| `Boolean` / `boolean` | `boolean` | 布尔型直接对齐 |
| `LocalDateTime` / `Instant` / `Date` | `string` | 后端序列化为 ISO 8601 字符串（如 `2026-08-26 12:00:00`） |
| `List<T>` / `Set<T>` / `Collection<T>` | `T[]` | 数组泛型 |
| `Map<K, V>` | `Record<K, V>` | 键值映射对象 |
| `Enum` (如 `StageType`) | `'CTF' \| 'AWD' \| 'AWD_PLUS'` | 联合字面量类型或 TS `enum` |
| `void` / `ApiResponse<Void>` | `void` / `ApiResponse<void>` | 动作执行型响应，`data` 为 `undefined`/`null` |

---

### 6.4 前端表单校验与后端 DTO 校验约束严格 1:1 对齐规范 (Frontend Form Validation & Backend DTO Alignment)

> [!IMPORTANT]
> **核心校验对齐铁律**：前端所有表单校验（Naive UI `FormRules` / `async-validator` 或自定义校验器），**必须严格参考后端对应 DTO 中的 Jakarta Validation (`jakarta.validation.constraints`) 注解定义**。
> 包括必填校验（`@NotBlank`, `@NotNull`）、字段长度约束（`@Size(min = ..., max = ...)`）、正则表达式规则（`@Pattern(regexp = ...)`）以及格式校验（`@Email` 等）。
> **严禁前端设置比后端更宽松的校验规则**（避免无效数据通过前端校验却被后端 400 拦截），也**严禁前端设置比后端更严苛或冲突的校验规则**（避免阻碍合规输入）。

#### 1. 约束注解与 Naive UI FormRule 映射对照表

| 后端 Jakarta 注解 | 前端 Naive UI `FormRule` 配置 | 错误触发条件与说明 |
| :--- | :--- | :--- |
| `@NotBlank(message = "...")` / `@NotNull` | `{ required: true, message: t('...'), trigger: 'blur' }` | 必填项为空 |
| `@Size(min = X, max = Y, message = "...")` | `{ min: X, max: Y, message: t('...'), trigger: 'blur' }` | 字符串长度不在 `[X, Y]` 范围内 |
| `@Pattern(regexp = "...", message = "...")` | `{ pattern: /.../, message: t('...'), trigger: ['blur', 'input'] }` | 正则表达式不匹配（如密码复杂度、用户名合法字符） |
| `@Email(message = "...")` | `{ type: 'email', message: t('...'), trigger: ['blur', 'input'] }` | 邮箱格式不符合标准 |
| `@Min(V)` / `@Max(V)` | `{ type: 'number', min: V, max: V, message: t('...'), trigger: 'blur' }` | 数值范围越界 |

#### 2. 标准落地范例（以 `RegisterRequestDTO.java` 为例）：

- **后端 DTO 声明 (`com.humolix.volur.auth.domain.dto.RegisterRequestDTO.java`)**：
```java
public class RegisterRequestDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 32, message = "用户名长度必须在3到32个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "用户名只能包含字母、数字、下划线和连字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度必须在6到32个字符之间")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*?&._#^&+=])[A-Za-z\\d@$!%*?&._#^&+=]{6,32}$",
            message = "密码必须包含字母、数字和至少一个特殊字符，且长度在6到32个字符之间"
    )
    private String password;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
}
```

- **前端 Vue 3 FormRules 100% 对齐实现 (`AuthForm.vue`)**（使用顺序校验器避免堆叠重复报错）：
```typescript
const registerRules = computed<FormRules>(() => ({
  username: [
    { required: true, message: t('auth.username_required'), trigger: 'blur' },
    {
      validator(_rule, value) {
        if (!value) return true
        if (value.length < 3 || value.length > 32) {
          return new Error(t('auth.username_length'))
        }
        if (!/^[a-zA-Z0-9_-]+$/.test(value)) {
          return new Error(t('auth.username_pattern'))
        }
        return true
      },
      trigger: ['blur', 'input']
    }
  ],
  password: [
    { required: true, message: t('auth.password_required'), trigger: 'blur' },
    {
      validator(_rule, value) {
        if (!value) return true
        // 步骤 1：先校验长度
        if (value.length < 6 || value.length > 32) {
          return new Error(t('auth.password_length'))
        }
        // 步骤 2：长度满足后再校验字符复杂度（避免同时堆叠多条报错）
        if (!/^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*?&._#^&+=])[A-Za-z\d@$!%*?&._#^&+=]{6,32}$/.test(value)) {
          return new Error(t('auth.password_pattern'))
        }
        return true
      },
      trigger: ['blur', 'input']
    }
  ],
  email: [
    { required: true, message: t('auth.email_required'), trigger: 'blur' },
    { type: 'email', message: t('auth.invalid_email'), trigger: ['blur', 'input'] }
  ]
}))
```

---

## 7. 网络层与请求架构规范 (Network & Axios Architecture)

### 7.1 统一请求客户端 (`src/utils/request.ts`)
- **Base URL 规范**：默认统一使用 `/api/v1`，可通过环境变量 `VITE_API_BASE_URL` 覆盖。
- **JWT 自动装配**：请求拦截器自动从本地缓存获取 `accessToken`，并通过 HTTP Header `Authorization: Bearer <token>` 携带。
- **无感 Token 刷新队列**：
  - 当接口返回 HTTP 401 时，挂起当前请求队列；
  - 自动使用 `refreshToken` 请求 `/api/v1/auth/refresh` 获取新 `accessToken`；
  - 刷新成功后更新本地凭据，并重放挂起的请求队列；
  - 刷新失败则彻底清空凭据，重定向至 `/auth/login`。
- **统一异常提示**：配合 Naive UI 的 `$message` 或 `$notification` 对业务异常（`success === false`）与网络异常进行统一 Toast 报错。

### 7.2 实时通信 (SSE / Server-Sent Events)
- 对于实时榜单、系统广播与站内通知，统一使用 `src/utils/sse.ts` 封装的 `SseClient` 进行订阅。
- 自动携带当前用户身份 Token，并在连接断开时提供重试与事件派发机制。

---

## 8. UI 组件库与字体库集成规范 (Naive UI & vfonts)

### 8.1 字体库集成 (`vfonts`)
项目必须在 `src/main.ts` 入口显式引入 `vfonts`，确保全平台字体渲染的一致性与高美观度：
```typescript
// main.ts
import 'vfonts/Lato.css'      // 通用高品质无衬线西文字体
import 'vfonts/FiraCode.css'  // 等宽代码与数字字体 (适用于 Token、Flag、代码片段)
```

### 8.2 Naive UI Provider 嵌套层次与国际化桥接
为保证 Naive UI 的样式注入、主题切换、国际化语言环境及全局独立 API（`$message`, `$dialog`, `$notification`, `$loadingBar`）正常工作，`App.vue` 必须按以下标准结构进行 Provider 嵌套并绑定 `:locale` 与 `:date-locale`：
```html
<template>
  <n-config-provider
    :theme="theme"
    :theme-overrides="themeOverrides"
    :locale="naiveLocale"
    :date-locale="naiveDateLocale"
  >
    <n-loading-bar-provider>
      <n-dialog-provider>
        <n-notification-provider>
          <n-message-provider>
            <naive-provider>
              <router-view />
            </naive-provider>
          </n-message-provider>
        </n-notification-provider>
      </n-dialog-provider>
    </n-loading-bar-provider>
    <n-global-style />
  </n-config-provider>
</template>
```

---

## 9. 国际化与多语言集成规范 (i18n & Naive UI Locale Standard)
> 参考官方规范：[Naive UI 国际化 (i18n)](https://www.naiveui.com/zh-CN/os-theme/docs/i18n)

### 9.1 核心配置原则
1. **统一 `vue-i18n` 组合式架构**：
   - 全局语言与业务国际化统一采用 `vue-i18n`（`legacy: false` 模式），通过 `createI18n` 初始化并挂载至根应用；
   - Naive UI 组件语言和日期语言通过根节点 `NConfigProvider` 上的 `:locale` 与 `:date-locale` 属性进行动态绑定；
   - 保证应用业务语言与 Naive UI 内部组件（如分页、日期选择器、上传组件等）的语言状态 100% 保持同步响应。
2. **多语言包目录结构与自动加载**：
   - 语言文件统一收敛于 `src/locales/{locale}/{module}.json`（如 `src/locales/zh_CN/auth.json`, `src/locales/en_US/auth.json`）；
   - 通过 Vite 原生 `import.meta.glob('./locales/**/*.json', { eager: true })` 自动完成模块命名空间映射，严禁手动逐个文件 `import`。

### 9.2 `App.vue` 响应式语言桥接标准
```typescript
import { computed } from 'vue'
import { zhCN, dateZhCN, enUS, dateEnUS } from 'naive-ui'
import { useI18n } from 'vue-i18n'

const { locale } = useI18n()

// 动态映射 Naive UI 的组件语言与日期语言
const naiveLocale = computed(() => (locale.value === 'en_US' || locale.value === 'en' ? enUS : zhCN))
const naiveDateLocale = computed(() => (locale.value === 'en_US' || locale.value === 'en' ? dateEnUS : dateZhCN))
```

### 9.3 组件内使用规范
- 在 `<script setup lang="ts">` 中通过 `const { t } = useI18n()` 引入；
- 模板中使用 `{{ t('auth.welcome_title') }}`，表单校验规则中使用 `message: t('auth.username_required')`；
- 切换语言时同步写入 `localStorage.setItem('locale', nextLocale)` 保证持久化。

---

## 10. 全局主题与 SCSS 样式体系规范 (Global Day/Night Theme Architecture)

### 10.1 核心视觉三大铁律
1. **主要操作按钮视觉规范 (Primary Button Styling)**：
   - 核心主要操作按钮（登录、注册、重置密码、主要业务提交与 CTA）参考 Volur 项目风格：**亮色模式下按钮背景必须为 `#000000`，字体颜色必须为 `#ffffff`**；暗色模式下采用高反差反色（背景 `#ffffff`，字体 `#000000`）或黑白极简对比。
   - 按钮圆角统一为 `8px`（`border-radius: 8px`），大尺寸按钮高度统一为 `48px`，字重为 `600`。
2. **描述文本与表单标签颜色规范 (Description & Typography Standards)**：
   - **绝大多数描述字体、表单项标签（Label）、副标题（Subtitle）以及辅助说明文字，字体颜色必须使用 `#444` (`#444444`) 或 SCSS 变量 `$text-desc`，严禁使用纯黑 `#000000` 作为描述字体**。
   - 在暗色模式下对应映射为 `#9ca3af` 或 `#cbd5e1`，确保视觉层次清晰，避免纯黑造成的视觉疲劳。
3. **全局语言与夜间模式切换控制器定位规范 (Fixed Bottom-Left Switchers)**：
   - **中英文语言切换与夜间/亮色主题切换按钮必须固定在页面视口左下角**（`position: fixed; bottom: 20px; left: 20px; z-index: 1000; display: flex; gap: 10px;`）。
   - 采用轻量圆形悬浮按钮（`circle`、`secondary`），保持主视区与顶栏的极致简洁。

### 10.2 全局主题 CSS 变量与设计令牌体系 (`src/styles/theme.scss`)
系统基于 CSS Custom Properties 实现零闪烁、平滑渐变的日间与夜间双模态切换：

| 设计令牌 (CSS 变量) | 日间模式 (Light Mode) | 夜间模式 (Dark Mode) | 应用场景说明 |
| :--- | :--- | :--- | :--- |
| `--v-bg-base` | `#fafafa` | `#121826` | 页面最底层的视口背景色 |
| `--v-bg-card` / `--v-bg-surface` | `#ffffff` | `#1e293b` | 卡片容器、Modal 对话框、下拉菜单背景 |
| `--v-bg-header` | `rgba(250, 250, 250, 0.85)` | `rgba(18, 24, 38, 0.85)` | 顶部毛玻璃导航条背景 |
| `--v-bg-code` | `#f4f3ec` | `#1f2028` | 代码块、Token、Flag 展示区域底色 |
| `--v-text-primary` | `#111111` | `#f1f5f9` | 页面主要标题、核心数据主文字 |
| `--v-text-desc` | `#444444` | `#9ca3af` | **绝大多数描述文字、表单项 Label、副标题** |
| `--v-text-muted` | `#666666` | `#6b7280` | 次级弱化辅助文字、版权声明 |
| `--v-text-sub` | `#767676` | `#94a3b8` | 标题副标题及次要提示 |
| `--v-border-base` | `#e5e4e7` | `#2e303a` | 标准边框、分割线 |
| `--v-btn-primary-bg` | `#000000` | `#ffffff` | **主要操作按钮背景色** |
| `--v-btn-primary-text` | `#ffffff` | `#000000` | **主要操作按钮字体色** |
| `--v-btn-primary-hover` | `#222222` | `#e5e5e5` | 主要操作按钮悬浮色 |
| `--v-shadow-sm` / `--v-shadow-md` | 柔和浅阴影 | 柔和深阴影 | 卡片及悬浮窗投影 |

### 10.3 SCSS 模块化与 Mixins 工具库 (`src/styles/mixins.scss`)
1. **主题模式辅助 Mixins**：
   ```scss
   // 暗色主题作用域
   @mixin theme-dark { html.dark &, [data-theme='dark'] & { @content; } }
   // 亮色主题作用域
   @mixin theme-light { html:not(.dark) &, [data-theme='light'] & { @content; } }
   ```
2. **标准容器与按钮 Mixins**：
   ```scss
   // 标准卡片容器
   @mixin v-card {
     background-color: var(--v-bg-card);
     border: 1px solid var(--v-border-base);
     border-radius: 12px;
     box-shadow: var(--v-shadow-sm);
     transition: background-color 0.3s ease, border-color 0.3s ease;
   }

   // Volur 标准主要操作按钮
   @mixin v-primary-button {
     background-color: var(--v-btn-primary-bg);
     color: var(--v-btn-primary-text);
     border: none;
     border-radius: 8px;
     font-weight: 500;
     height: 48px;
     font-size: 16px;
     cursor: pointer;
     transition: background-color 0.2s ease;
     &:hover { background-color: var(--v-btn-primary-hover); }
   }
   ```
3. **全局样式自动注入**：
   通过 `vite.config.ts` 的 `additionalData` 自动全局注入 `@/styles/variables.scss` 与 `@/styles/mixins.scss`，组件内直接调用，无需手动 `@import`。

---

## 11. 页面与组件编码规范 (Vue 3 Component Standards)

1. **统一 `<script setup lang="ts">`**：所有 Vue 页面与组件统一使用 Composition API `<script setup lang="ts">` 语法糖。
2. **强类型 Props 与 Emits**：
   - 必须使用基于类型的声明：`defineProps<{ ... }>()` 与 `defineEmits<{ ... }>()`。
   - 默认值使用 `withDefaults`。
3. **响应式状态声明**：
   - 优先使用 `ref` 声明基本类型与列表状态；
   - 结构化表单对象使用 `reactive` 或 `ref`，结合 Naive UI `FormInst` 与 `FormRules` 进行强类型校验。

---

## 12. 系统动态设置键名规范 (System Setting Keys in Frontend)

1. **统一点分层级命名**：前端（`useSystemStore`、`AdminSystemView.vue`、`AuthForm.vue` 等）读取与修改系统动态设置时，统一使用标准点分层级 Key（如 `system.maintenance.enabled`, `system.auth.registration_enabled`, `system.captcha.enabled`），彻底杜绝历史遗留的扁平大写下划线键名（如 `MAINTENANCE_MODE`, `ENABLE_CAPTCHA`）。
2. **请求防抖与 Promise 复用**：`useSystemStore.fetchPublicSettings()` 必须支持在途请求 Promise 缓存与去重，杜绝页面加载/路由守卫与根组件挂载期间的并发重复请求。

---

## 13. 规范合规速查清单 (Frontend Compliance Checklist)

- [ ] 项目语言标准是否强制采用 **ES2026 (ESNext)**？
- [ ] 全局状态管理是否采用 **Pinia Setup Store 语法风格**，彻底杜绝 Options Store？
- [ ] **所有 API 函数请求的 HTTP 动词与 URI 路径是否 100% 严格与后端 Controller 注解声明一致（杜绝臆测如 `/competition/list` 反模式）？**
- [ ] **系统动态设置 Key 是否 100% 统一采用点分层级规范（如 `system.maintenance.enabled`），杜绝历史别名与大写下划线键名？**
- [ ] **前端表单校验规则（FormRules 必填 / 长度范围 / 正则表达式 / 邮箱等）是否 100% 严格参考后端 DTO 中的 Jakarta Validation 注解（如 `@Size`, `@Pattern`, `@Email`）？**
- [ ] **主要操作按钮是否遵循背景 `#000000` / 字体 `#ffffff` 的 Volur 视觉标准？**
- [ ] **表单标签、副标题及描述文字颜色是否统一为 `#444` 而非纯黑 `#000`？**
- [ ] **中英文与夜间模式切换按钮是否固定在视口左下角（`bottom: 20px; left: 20px; z-index: 1000;`）？**
- [ ] 目录结构是否与后端 `volur-lite-platform` 模块 1:1 对齐？
- [ ] 所有 TS 实体是否严格镜像后端 `domain.dto.*DTO` 与 `domain.vo.*VO`？
- [ ] 是否彻底杜绝了 `any` 类型的滥用？
- [ ] 雪花 ID 字段是否声明为 `number | string` 防止精度丢失？
- [ ] API 函数是否全部返回强类型的 `Promise<ApiResponse<T>>` 或分页 Record？
- [ ] `vfonts` 是否已在 `main.ts` 中正确导入？
- [ ] Naive UI `NConfigProvider` 是否正确绑定了 `:locale` 与 `:date-locale`（参考 Naive UI 官方 i18n 指南）？
- [ ] 组件是否采用 `<script setup lang="ts">` 与 `<style lang="scss" scoped>`？
- [ ] 每次前端代码修改完成后，是否在 `agent/volur-design-md` 中记录了审计日志？
