# 施工记录：Auth 视图目录子结构划分 (signin/signup) 与移除组件内重复背景

- **变更日期**：2026-09-28
- **涉及模块**：
  - `src/volur-lite-view-NEXT/src/views/auth/signin/IdentifierView.vue`
  - `src/volur-lite-view-NEXT/src/views/auth/signin/ChallengeView.vue`
  - `src/volur-lite-view-NEXT/src/views/auth/signup/SignUpView.vue`
  - `src/volur-lite-view-NEXT/src/router/routes/auth.ts`
- **变更类型**：`refactor(auth-views)`

---

## 1. 变更背景与架构动因

1. **认证流程层级细分**：在 `src/views/auth/` 下增加 `signin` 与 `signup` 子业务目录，将登录各个阶段的视图模块（账号身份校验、安全凭据挑战）收敛于 `signin/` 目录下，注册流程收敛于 `signup/` 目录下；
2. **命名语义去冗余**：处于 `auth/signin/` 目录下的组件不再需要重复的 `SignIn` 前缀，重构为语义更精炼的 `IdentifierView.vue` 与 `ChallengeView.vue`；
3. **背景归一化管理**：由于所有认证相关路由均已被 `AuthLayout.vue` 统一包裹，其根容器已经提供了标准的 Google Material 3 表面容器背景色（`bg-sys-color-surface-container`，亮色 `#f0f4f9` / 暗色 `#1e1f20`）。因此，视图组件内部不再硬编码独立背景色（移除 `bg-white dark:bg-google-surface-darkCard`），使整体卡片与布局深度融合，杜绝背景重复与视觉断层。

---

## 2. 核心改动点

### 2.1 目录划分与文件迁移
1. 创建目录：
   - `src/volur-lite-view-NEXT/src/views/auth/signin/`
   - `src/volur-lite-view-NEXT/src/views/auth/signup/`
2. 文件重命名与迁移：
   - `src/views/auth/SignInIdentifierView.vue` $\rightarrow$ [`src/views/auth/signin/IdentifierView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/IdentifierView.vue)；
   - `src/views/auth/SignInChallengeView.vue` $\rightarrow$ [`src/views/auth/signin/ChallengeView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/ChallengeView.vue)；
   - `src/views/auth/SignUpView.vue` $\rightarrow$ [`src/views/auth/signup/SignUpView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signup/SignUpView.vue)；
   - 清理 `src/views/auth/` 根目录下的孤立旧文件。

### 2.2 移除组件内背景类
在 [`IdentifierView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/IdentifierView.vue)（以及同序列的 [`ChallengeView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/ChallengeView.vue) 与 [`SignUpView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signup/SignUpView.vue)）中：
- 移除了卡片根节点的 `bg-white dark:bg-google-surface-darkCard` 类；
- 组件背景完全由 `AuthLayout.vue` 提供的 `bg-sys-color-surface-container` 接管，保持容器内表面色彩纯粹与统一。

### 2.3 路由引用对齐 (`src/router/routes/auth.ts`)
更新 [`src/router/routes/auth.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/router/routes/auth.ts)：
```ts
{
  path: 'identifier',
  name: 'V1SignInIdentifier',
  component: () => import('@/views/auth/signin/IdentifierView.vue'),
  meta: { title: '登录 - Volur 账号', requiredAuth: false }
},
{
  path: 'challenge',
  name: 'V1SignInChallenge',
  component: () => import('@/views/auth/signin/ChallengeView.vue'),
  meta: { title: '登录 - 安全验证', requiredAuth: false }
}
```

---

## 3. 验证与审计结果

1. **类型检查与全量生产打包**：
   - 执行 `npm run build`，`vue-tsc --noEmit` 0 错误通过；
   - Vite 生产构建顺利打包，正确生成 `IdentifierView`、`ChallengeView` 与 `SignUpView` chunks，0 错误 0 告警；
2. **开发服务器端点验证**：
   - `GET /src/views/auth/signin/IdentifierView.vue` $\rightarrow$ `HTTP 200 OK` (MIME: `text/javascript`)；
   - `GET /src/views/auth/signin/ChallengeView.vue` $\rightarrow$ `HTTP 200 OK` (MIME: `text/javascript`)；
   - `GET /src/views/auth/signup/SignUpView.vue` $\rightarrow$ `HTTP 200 OK` (MIME: `text/javascript`)；
   - 登录页面无白底生硬遮罩，由 `AuthLayout` 的 `sys-color-surface-container` 背景统一承托。
