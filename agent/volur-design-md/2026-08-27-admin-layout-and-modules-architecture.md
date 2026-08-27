# 后台管理架构 AdminLayout 与 7 大核心管理模块路由设计

## 1. 变更背景与需求
- 以前台 [`AppLayout.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/layout/AppLayout.vue) 为视觉与交互基准，构建 Volur Lite 专用的后台管理布局 [`AdminLayout.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/layout/AdminLayout.vue)；
- 需包含 7 大核心管理模块：
  1. **控制台 (Dashboard)**：`/admin/dashboard`
  2. **用户管理**：`/admin/users`
  3. **题库管理**：`/admin/challenges`
  4. **竞赛管理**：`/admin/competitions`
  5. **公告管理**：`/admin/broadcasts`
  6. **容器管理**：`/admin/containers`
  7. **系统设置**：`/admin/system`
- 视觉风格严格遵守 Volur 统一暗亮色主题设计体系（`#000000` 主色按钮、`#444444` 描述色、矢量 xicons 图标及平滑折叠过渡）。

---

## 2. 架构设计与实现

### 2.1 布局组件 [`src/layout/AdminLayout.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/layout/AdminLayout.vue)
- **侧边栏顶部**：展示平滑过渡的 Volur 矢量 Logo 与专属 `ADMIN` 标识徽标；
- **侧边栏菜单**：集成 7 大核心管理模块专属矢量图标；
- **侧边栏底部**：常驻【返回前台竞赛】入口，展开与折叠态自动切换 Tooltip 与文本；
- **顶部 Header**：面包屑导航（`管理后台 / [当前模块]`）、多语言切换、暗亮色模式切换及管理员个人菜单。

### 2.2 路由与权限守卫 ([`src/router/routes.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/router/routes.ts) & [`src/router/index.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/router/index.ts))
- `/admin` 路由树使用 `AdminLayout`，配置 `requiresAuth: true` 与 `roles: ['ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ADMIN', 'SUPER_ADMIN']`；
- 全局路由守卫自动拦截非管理员访问 `/admin` 并无缝回退。

### 2.3 管理端模块视图初始化
- **[`AdminDashboardView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/admin/dashboard/AdminDashboardView.vue)**：核心业务概览卡片大屏；
- **[`AdminUserView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/admin/user/AdminUserView.vue)**：用户与权限管理；
- **[`AdminChallengeView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/admin/challenge/AdminChallengeView.vue)**：题库环境配置；
- **[`AdminCompetitionView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/admin/competition/AdminCompetitionView.vue)**：多阶段竞赛编排；
- **[`AdminBroadcastView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/admin/broadcast/AdminBroadcastView.vue)**：全站通知公告广播；
- **[`AdminContainerView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/admin/container/AdminContainerView.vue)**：靶机容器监控；
- **[`AdminSystemView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/admin/system/AdminSystemView.vue)**：系统参数与安全风控。

---

## 3. 验证结论
- **前端构建**：`vue-tsc --noEmit && vite build` 编译打包 100% 成功。
- **全链路闭环**：前台管理员入口一键跳转后台 $\to$ 7 大模块菜单自由切换 $\to$ 随时点击底部一键返回前台。
