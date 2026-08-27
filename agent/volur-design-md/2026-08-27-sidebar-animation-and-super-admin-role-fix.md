# 侧边栏折叠平滑过渡动画与超级管理员角色识别修复

## 1. 现象与根因分析

### 1.1 侧边栏 Logo 变换突兀现象
在侧边栏折叠/展开时，直接使用 `v-if` / `v-else` 会在侧边栏宽度 300ms 过渡期间瞬间切换 DOM 节点，导致大 Logo 骤消、小 Logo 突现，缺乏平滑过渡动画。

### 1.2 超级管理员登录后未显示管理员面板
- **根因**：后端用户角色枚举为 [`UserRole.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/domain/enums/UserRole.java)（定义为 `SUPER_ADMIN`、`ADMIN`、`USER`），MapStruct 在生成 `UserProfileResponseVO` 时序列化为 `"SUPER_ADMIN"` 或 `"ADMIN"`（**不带 `ROLE_` 前缀**）；
- 前端 [`user.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/store/modules/user.ts) 中 `isAdmin` 之前只严格匹配 `'ROLE_SUPER_ADMIN'` 或 `'ROLE_ADMIN'`，导致判断始终返回 `false`。

---

## 2. 优化与修复清单

### 2.1 用户角色判定兼容修复 ([`src/store/modules/user.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/store/modules/user.ts))
更新 `isAdmin` 计算属性：
```ts
const isAdmin = computed(() => {
  const r = (user.value?.role || '').toUpperCase()
  return r === 'SUPER_ADMIN' || r === 'ADMIN' || r === 'ROLE_SUPER_ADMIN' || r === 'ROLE_ADMIN'
})
```
无论后端返回 `SUPER_ADMIN` 还是 `ROLE_SUPER_ADMIN`，均能 100% 准确识别。

### 2.2 页面挂载自动补全 Profile ([`src/App.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/App.vue))
在应用根组件 `onMounted` 钩子中，检测到存在 Token 但无内存 User Profile 时自动调用 `fetchUserProfile()`，确保刷新页面后管理员权限即时恢复。

### 2.3 Logo 与管理员按钮平滑过渡动画 ([`src/layout/AppLayout.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/layout/AppLayout.vue))
1. **Logo 动画**：使用 `<transition name="logo-fade" mode="out-in">` 包装展开态和收缩态图片；
2. **管理员按钮动画**：使用 `<transition name="admin-fade" mode="out-in">` 包装展开态整行按钮与收缩态 Tooltip 圆形按钮；
3. **动画曲线**：采用 `0.22s cubic-bezier(0.4, 0, 0.2, 1)` 配以微缩放（`scale(0.88)`）与淡入淡出，消除视觉跳跃。

---

## 3. 验证结论
- **前端构建**：`vue-tsc --noEmit && vite build` 编译打包 100% 成功。
- **视觉体验**：折叠侧边栏时 Logo 与管理员按钮均呈渐变缩放过渡，动效流畅丝滑。
