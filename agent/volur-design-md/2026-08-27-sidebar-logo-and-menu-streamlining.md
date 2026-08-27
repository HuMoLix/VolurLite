# 侧边栏 SVG Logo 切换、普通用户功能精简与管理员面板入口改造

## 1. 变更背景与需求
1. **侧边栏 Logo 矢量化与状态切换**：
   - 彻底废弃旧有文本图标 `<div class="logo-icon">V</div>` 与 `<span class="logo-text">Volur Lite</span>`；
   - 侧边栏展开时使用完整的 [`/long-logo.svg`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/public/long-logo.svg)；
   - 侧边栏收缩时切换为方形徽标 [`/favicon.svg`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/public/favicon.svg)。
2. **菜单精简（聚焦核心竞赛业务）**：
   - 针对普通用户，Volur Lite 定位为轻量化竞赛平台，移除【首页】与【题库训练】菜单项及对应路由，默认仅开放【竞赛演练】。
3. **管理员专属入口**：
   - 当登录用户角色为管理员（`userStore.isAdmin` 为 `true`）时，在侧边栏底部固定常驻【管理员面板】快捷入口。

---

## 2. 改动清单

### 2.1 [`src/layout/AppLayout.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/layout/AppLayout.vue)
1. **Logo 容器改造**：
   ```html
   <div class="logo-container" @click="router.push('/competitions')">
     <img
       v-if="!appStore.isSidebarCollapsed"
       src="/long-logo.svg"
       alt="Volur Lite"
       class="logo-img-expanded"
     />
     <img
       v-else
       src="/favicon.svg"
       alt="Volur Lite"
       class="logo-img-collapsed"
     />
   </div>
   ```
2. **菜单项精简**：
   - `menuOptions` 仅保留 `Competitions`（竞赛演练）；
3. **侧边栏底部管理员入口**：
   - 使用 flexbox 上下布局（`sidebar-top` 与 `sidebar-bottom-admin`）；
   - 展开状态展示完整按钮（带 `ShieldCheckmarkOutline` 图标与文本）；
   - 收缩状态展示圆形悬浮图标与 Tooltip 气泡提示。

### 2.2 [`src/router/routes.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/router/routes.ts)
- 根路径 `/` 默认重定向至 `/competitions`；
- 移除普通路由中的 Home 及 Challenges 子路由。

---

## 3. 验证结论
- **前端构建**：`vue-tsc --noEmit && vite build` 编译打包 100% 成功。
- **视觉效果**：展开/折叠状态下 Logo 与管理员入口过渡平滑自然。
