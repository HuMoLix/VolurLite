# 施工记录：Volur Lite NEXT 路由层模块化拆分与 routes 目录解耦设计

- **变更日期**：2026-09-27
- **涉及模块**：`src/volur-lite-view-NEXT/src/router`
- **变更类型**：`refactor(router)`
- **主要目标**：
  1. 拆分 `src/volur-lite-view-NEXT/src/router/index.ts`，抽离出专门用于汇聚路由表的 `router.ts`，`index.ts` 纯粹负责路由器实例创建、生命周期与导航守卫逻辑；
  2. 对 `router.ts` 进一步解耦，建立 `src/volur-lite-view-NEXT/src/router/routes/` 目录，按业务模块维护独立的路由定义文件（`auth.ts`, `competition.ts`, `admin.ts`, `common.ts`）。

---

## 1. 变更背景与架构动机

此前 `src/volur-lite-view-NEXT/src/router/index.ts` 集中混合了：
- 路由表声明（`routes` 数组）；
- 视图动态导入；
- 全局守卫逻辑（`router.beforeEach` 鉴权与标题处理）；
- 路由器实例创建与导出。

随着系统模块逐渐丰富（包含身份认证 `/v1/signin/**`、`/v1/signup`，公开赛事 `/competitions/**`，短链直达 `/c/**`，后台管理 `/admin/**` 等），单文件维护路由表会导致职责模糊与代码臃肿。

为了保持与经典前端工程架构设计思想一致并具备更高模块解耦度，决定对 NEXT 路由体系进行专业拆分。

---

## 2. 核心架构与改动点

### 2.1 目录分层结构
重构后的 `src/volur-lite-view-NEXT/src/router` 目录结构如下：
```
src/router/
├── index.ts              # 路由器实例、页面标题同步与基于 requiredAuth 的导航守卫
├── router.ts             # 路由表总装配器 (Aggregator)，合并导出各个子模块 routes
└── routes/               # 业务模块路由定义目录
    ├── auth.ts           # 维护 /v1/signin/**、/v1/signup/** 等认证模块路由
    ├── competition.ts    # 维护 /competitions/**、/c/** 等公开赛事模块路由
    ├── admin.ts          # 维护 /admin/** 等后台管理模块路由 (requiredAuth: true)
    └── common.ts         # 维护根路径重定向 (/)、控制台工作台 (/v1/home) 与 404 兜底
```

### 2.2 具体模块职责说明

1. **`routes/auth.ts`**：
   - 收敛分步式登录 `/v1/signin/identifier`（账号输入）与 `/v1/signin/challenge`（凭证挑战）；
   - 收敛注册入口 `/v1/signup`。
2. **`routes/competition.ts`**：
   - 维护 `/competitions`（赛事大厅列表，`requiredAuth: false`）；
   - 维护 `/competitions/:id` 与 `/c/:id` 赛事工作台与短链直达。
3. **`routes/admin.ts`**：
   - 维护 `/admin` 与 `/admin/dashboard`，显式声明 `requiredAuth: true` 与 `roles: ['ROLE_ADMIN', 'ROLE_SUPER_ADMIN']`。
4. **`routes/common.ts`**：
   - 根路径 `/` 默认重定向至公共页面 `/competitions`；
   - 未匹配路由通配符 `/:pathMatch(.*)*` 兜底至 `/competitions`。
5. **`router.ts`**：
   - 导入上述各个模块数组，展平合并导出 `export const routes: RouteRecordRaw[]`。
6. **`index.ts`**：
   - 单纯承载 `createRouter` 与 `router.beforeEach`；
   - 严格根据路由链元信息中的 `requiredAuth`（与 `requiresAuth`）判定是否需要引导拦截至 `/v1/signin/identifier`。

---

## 3. 验证与审计结果

- 执行 `npm run build` 全量生产打包编译：
  ```
  > vue-tsc --noEmit && vite build
  ✓ 2714 modules transformed.
  dist/assets/AdminDashboardView-Cv5G4UgU.js       1.98 kB
  dist/assets/SignUpView-DoERcVQ3.js               3.56 kB
  dist/assets/SignInChallengeView-2-JgJMK1.js      3.81 kB
  dist/assets/SignInIdentifierView-DhMyz5cQ.js     3.94 kB
  dist/assets/HomeView-C2flov3Q.js                17.22 kB
  dist/assets/index-CHkdaL3A.js                  360.45 kB
  ✓ built in 4.54s
  ```
- TypeScript 类型检查严格通过，路由懒加载 Chunk 分包清晰，无任何破坏性变更。
