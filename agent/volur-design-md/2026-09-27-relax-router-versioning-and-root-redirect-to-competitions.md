# 施工记录：放宽前端路由版本控制为按需添加、根路径重定向至 /competitions 与显式 requiredAuth 鉴权机制

- **变更日期**：2026-09-27
- **涉及模块**：`agent/volur-design-skills/view-NEXT-rules.md`、`GEMINI.md`、`src/volur-lite-view-NEXT/src/router/index.ts`、`src/volur-lite-view/src/router/index.ts`、`src/volur-lite-view/src/types/router.d.ts`
- **变更类型**：`refactor(router)` & `docs(rules)`
- **主要目标**：
  1. 修改 `view-NEXT-rules.md` 与 `GEMINI.md` 中的路由版本控制规范，明确不再强制所有业务路由都具备版本前缀，而是按需演进、根据用户具体要求进行添加；
  2. 修改前端路由配置，将根路径 `/` 默认重定向至公开的赛事大厅 `/competitions`；
  3. 优化路由守卫（Navigation Guard），严格依据 `routes` 中显式配置的 `requiredAuth`（及 `requiresAuth`）判定是否引导拦截至登录页面，未标记需鉴权的公开路由不受任何阻断。

---

## 1. 变更背景与需求说明

在之前的规范设定中，曾将“所有业务路由强制携带版本前缀（如 `/v1/...`）”作为硬性规则。然而在实际业务中：
- 核心公共入口如赛事大厅（`/competitions`）通常作为全站免登公开首页呈现，强加 `/v1/` 会破坏简洁的直达 URL；
- 根路径 `/` 直接粗暴重定向到登录页会严重阻碍未登录访客浏览公开赛事信息；
- 因此，用户指示：
  1. 放宽 `view-NEXT-rules.md` 中强制所有路由带版本号的限制，改为后续根据用户具体需求按需添加；
  2. 将根路径重定向目标统一改为 `/competitions`；
  3. 路由守卫根据路由配置中显式声明的 `requiredAuth` 决定是否引导去登录页。

---

## 2. 核心改动点

### 2.1 规范与技能文档更新
1. **`agent/volur-design-skills/view-NEXT-rules.md`**：
   - 将第三章标题更新为“路由架构与按需版本控制规范”；
   - 明确指出不再强制全部路由使用版本前缀，通用公开路由直接使用标准语义路径（如 `/competitions`），特定流程（如 Google 风格两步认证 `/v1/signin/identifier` 等）按要求应用版本前缀；
   - 规范根路径重定向至 `/competitions`；
   - 规定路由守卫仅在显式配置 `requiredAuth: true`（或 `requiresAuth: true`）且无 Token 时方才引导至登录页。
2. **`GEMINI.md`**：
   - 同步更新第 1.3 节关于前端路由的描述，明确“按需渐进式版本控制”策略。

### 2.2 新一代前端路由更新 (`src/volur-lite-view-NEXT`)
1. **`src/types/router.d.ts`**：
   - 扩展 `RouteMeta` 接口，定义 `requiredAuth?: boolean` 与 `requiresAuth?: boolean`；
2. **`src/router/index.ts`**：
   - 根路径 `{ path: '/', redirect: '/competitions' }`；
   - 注册 `/competitions` 路由（`meta: { title: '赛事大厅', requiredAuth: false }`）；
   - 保留 `/v1/signin/identifier`、`/v1/signin/challenge` 等 Google 风格认证路由；
   - 在 `router.beforeEach` 守卫中：
     ```ts
     const isAuthRequired = to.matched.some(
       (record) => record.meta.requiredAuth === true || record.meta.requiresAuth === true
     )
     if (isAuthRequired && !token) {
       return { path: '/v1/signin/identifier', query: { redirect: to.fullPath } }
     }
     ```

### 2.3 经典前端路由守卫对齐 (`src/volur-lite-view`)
1. **`src/types/router.d.ts`**：在 `RouteMeta` 中补充 `requiredAuth?: boolean` 强类型定义；
2. **`src/router/index.ts`**：在路由鉴权判定中增加对 `record.meta.requiredAuth === true` 的双向兼容支持。

---

## 3. 验证结果

- `src/volur-lite-view-NEXT` 运行 `npm run build` 成功完成构建，0 错误；
- `src/volur-lite-view` 运行 `npm run build` 完成类型检查与打包，无任何兼容性破损。
