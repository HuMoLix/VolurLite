# 彻底清除 NEXT 前端旧命名别名与废弃视图组件审计文档

## 1. 变更背景与治理动因
根据 `GEMINI.md` 确立的“重构彻底性铁律（严禁妥协保留与旧命名别名）”以及“纯粹代码与零注释铁律（文档外置原则）”，对 `src/volur-lite-view-NEXT`（新一代前端工程）进行全面扫描排查。

排查发现代码库中存在多处因历史迭代、阶段性演进遗留下来的“旧命名别名”与“废弃过渡视图”：
1. `AuthenticateView.vue` 曾在重构 Google Accounts 两阶段状态机时作为中间重定向页面，现已全量收敛至 `ChallengeView.vue` (`/v1/signin/challenge/pwd`)，继续留存属于死代码；
2. 路由中保留了 `alias: 'auth/signin'` 和 `path: 'authenticate'` 重定向分支；
3. 路由守卫中保留了 `record.meta.requiresAuth` 历史别名判定，类型声明中同时存在 `requiredAuth` 与 `requiresAuth`；
4. 领域模型中残留了未使用的别名 `PageQuery = PageRequestDTO` 以及已废弃接口 `SignInInitVO`；
5. 部分核心 TS 源码中残留了解释性行内/块注释。

---

## 2. 治理与重构明细

### 2.1 彻底物理删除废弃视图
- **删除文件**：`src/views/auth/signin/AuthenticateView.vue`
- **归因**：密码输入已完全收归为标准安全挑战类型之一（`pwd`），业务统一由 `ChallengeView.vue` 处理。该文件仅包含一个向新路由替换跳转的生命周期钩子，已无任何业务与视图价值。

### 2.2 路由层清洗 (`src/router/routes/auth.ts` 与 `src/router/index.ts`)
- 移除路由别名 `alias: 'auth/signin'`，强制全平台采用单一标准路由路径 `/v1/signin/**`；
- 移除 `path: 'authenticate'` 兼容跳转路由；
- 清理路由守卫 `beforeEach` 中的 `record.meta.requiresAuth` 别名检测，仅保留权威单一属性 `record.meta.requiredAuth === true`。

### 2.3 类型声明清洗 (`src/types/router.d.ts`, `src/types/common/api.ts`, `src/types/auth/flow.ts`)
- 从 `router.d.ts` 的 `RouteMeta` 中彻底剔除 `requiresAuth?: boolean` 别名定义；
- 从 `api.ts` 中删除 `export type PageQuery = PageRequestDTO` 别名定义；
- 从 `flow.ts` 中彻底删除早期废弃且无调用的 `SignInInitVO` 接口。

### 2.4 零注释纯粹化落地
- 统一清空了 `src/router/routes/admin.ts`、`common.ts`、`competition.ts`、`auth.ts`、`api/auth.ts`、`store/modules/authFlow.ts`、`types/common/api.ts`、`types/auth/flow.ts` 等文件中的冗余代码注释，保持代码紧凑纯净，遵循“代码自解释、文档入方案”的原则。

---

## 3. 验证与影响范围
- **编译与打包测试**：在 `src/volur-lite-view-NEXT` 运行 `npm run build` (`vue-tsc --noEmit && vite build`)，全量模块构建成功，类型检查 0 错误。
- **影响范围**：
  - `src/views/auth/signin/AuthenticateView.vue`（已删除）
  - `src/router/routes/auth.ts`（更新）
  - `src/router/routes/admin.ts`（更新）
  - `src/router/routes/common.ts`（更新）
  - `src/router/routes/competition.ts`（更新）
  - `src/router/index.ts`（更新）
  - `src/types/router.d.ts`（更新）
  - `src/types/common/api.ts`（更新）
  - `src/types/auth/flow.ts`（更新）
  - `src/api/auth.ts`（更新）
  - `src/store/modules/authFlow.ts`（更新）
