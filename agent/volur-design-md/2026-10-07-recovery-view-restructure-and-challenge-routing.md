# 找回用户名视图目录重构与挑战路由调度器改造实施记录

## 1. 变更背景与需求概述
在 Volur Lite 新一代前端工程（`src/volur-lite-view-NEXT`）中，原找回用户名入口视图文件为平铺式的 `src/views/auth/recovery/UsernameRecoveryView.vue`，存在以下设计与交互不一致问题：
1. **底部按钮定位未对齐标准操作栏规范**：原“返回登录”按钮使用 `justify-between` 排版，导致单一按钮孤立靠左，未与 `IdentifierView.vue` 及标准 Google 交互中右对齐（`justify-end`）的操作栏行为对齐；
2. **列表项视觉样式不统一**：原方式选择列表项中额外渲染了右侧 `>`（Chevron-Right）箭头，与 `src/views/auth/signin/challenge/selectionView.vue` 的 Google Flat 列表项规范不符；
3. **缺少挑战外壳路由调度机制**：用户选择“电子邮箱”找回方式后，需平滑跳转到挑战核验页面 `/v1/recovery/challenge/etotp`，需参考 `src/views/auth/signin` 的容器与子组件分层设计进行解耦；
4. **路由命名规范统一**：将旧的 `/v1/recovery/identifier` 路由路径与组件名重构为具备清晰语义的 `/v1/recovery/selection` 与 `SelectionView.vue`。

---

## 2. 核心改动点与架构设计

### 2.1 目录架构模块化重构 (对齐 Signin 体系)
将 `src/views/auth/recovery/` 目录组织对齐 `src/views/auth/signin/` 的外壳与子组件分离模式：
```
src/views/auth/recovery/
├── SelectionView.vue            // 方式选择主视图 (映射 /v1/recovery/selection)
├── ChallengeView.vue            // 挑战调度器外壳容器 (映射 /v1/recovery/challenge/:type(.*)*)
└── challenge/                   // 挑战核验子组件目录
    ├── etotpChallengeView.vue   // 邮箱安全码找回用户名挑战页面
    └── index.ts                 // 挑战子组件集中导出入口
```

### 2.2 列表项样式与按钮布局对齐
1. **删除右侧箭头**：移除 `SelectionView.vue` 列表项中的 `<div class="auth-selection-item__right">` 及其 svg 箭头图标，使列表项布局与 `signin/challenge/selectionView.vue` 保持 100% 结构一致；
2. **按钮靠右排布**：将原操作按钮容器修改为 `flex items-center justify-end pt-6 sm:pt-8`，使“返回登录”按钮居于右侧，与 `IdentifierView.vue` 一致；
3. **跳转路由演进**：选择 `EMAIL` 时推进至 `/v1/recovery/challenge/etotp`，选择 `PHONE` 时推进至 `/v1/recovery/challenge/smsotp`，并附带流程查询参数。

### 2.3 挑战调度器与子视图实现
1. **`ChallengeView.vue`**：
   - 采用 `provideCardHeader` 提供统一的卡片头部；
   - 动态解析路由参数 `route.params.type` 并计算激活挑战组件 `activeChallengeComponent`；
2. **`etotpChallengeView.vue`**：
   - 采用 `useCardHeader` 声明动态标题与副标题；
   - 提供 `GoogleTextField` 邮箱输入框、非空与邮箱正则表达式校验；
   - 提供底部操作栏（“返回” `btn-secondary-action` 返回 `/v1/recovery/selection`，“下一步” `btn-primary-action` 推进）。

### 2.4 路由与上游入口全链路更新
1. **`src/router/routes/auth.ts`**：
   - 根重定向：`/v1/recovery` 默认重定向至 `/v1/recovery/selection`；
   - 新增 `selection` 路由节点（组件指向 `SelectionView.vue`）；
   - 新增 `challenge/:type(.*)*` 路由节点（组件指向 `ChallengeView.vue`）；
   - 彻底删除旧 `identifier` 路由声明，杜绝历史兼容别名；
2. **`IdentifierView.vue`**：
   - 将 `handleForgotAccount` 跳转目标由 `/v1/recovery/identifier` 更新为 `/v1/recovery/selection`；
3. **物理清理**：
   - 彻底删除旧文件 `UsernameRecoveryView.vue`。

---

## 3. 安全与规范审查 (Compliance Check)

- **纯粹代码与零注释铁律**：所有新增及修改的 Vue 与 TypeScript 代码严格做到 0 解释性注释、0 行内备注；
- **重构彻底性铁律**：全链路清除旧命名与冗余别名，未遗留兼容重定向或中间别名导出；
- **Google Material Design 规范**：使用标准 Google Blue `#0b57d0` / `#8ab4f8` 调色板及透明扁平操作按钮；
- **编译与构建验证**：执行 `vue-tsc --noEmit && vite build`，编译产物无任何报错，类型检查全部通过。
