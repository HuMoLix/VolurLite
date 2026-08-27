# 后台管理多级侧边栏升级（题库子分类与系统日志集成）

## 1. 变更背景与需求
- 将后台管理布局 [`AdminLayout.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/layout/AdminLayout.vue) 升级为**多级折叠菜单**；
- 题库管理模块细化为 5 个核心子分类：
  1. **CTF 题库** (`/admin/challenges/ctf`)
  2. **理论题库** (`/admin/challenges/theory`)
  3. **ISW 题库** (`/admin/challenges/isw`)
  4. **AWD 题库** (`/admin/challenges/awd`)
  5. **AWD+ 题库** (`/admin/challenges/awdp`)
- 在侧边栏中新增【系统日志】模块 (`/admin/logs`)，用于审计用户操作轨迹与安全事件。

---

## 2. 改动清单

### 2.1 [`src/layout/AdminLayout.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/layout/AdminLayout.vue)
- 将 `AdminChallengesGroup` 配置为树形多级菜单，下挂 5 个赛题类型子项，各配以专属矢量图标（`FlagOutline`, `DocumentTextOutline`, `TerminalOutline`, `ShieldOutline`, `ShieldCheckmarkOutline`）；
- 新增 `AdminLogs` 菜单项（图标：`ReaderOutline`）；
- 面包屑支持两级嵌套显示（`管理后台 / 题库管理 / CTF 题库`）。

### 2.2 [`src/router/routes.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/router/routes.ts)
- 配置 `challenges` 嵌套子路由及其元数据 `parentTitle: '题库管理'`；
- 注册 `logs` 路由（组件：[`AdminLogView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/admin/log/AdminLogView.vue)）。

### 2.3 题库与日志视图组件
- **[`AdminCtfChallengeView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/admin/challenge/AdminCtfChallengeView.vue)**
- **[`AdminTheoryChallengeView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/admin/challenge/AdminTheoryChallengeView.vue)**
- **[`AdminIswChallengeView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/admin/challenge/AdminIswChallengeView.vue)**
- **[`AdminAwdChallengeView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/admin/challenge/AdminAwdChallengeView.vue)**
- **[`AdminAwdPlusChallengeView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/admin/challenge/AdminAwdPlusChallengeView.vue)**
- **[`AdminLogView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/admin/log/AdminLogView.vue)**

---

## 3. 验证结论
- **前端构建**：`vue-tsc --noEmit && vite build` 编译打包 100% 成功。
- **层级体验**：题库管理支持展开/收起子菜单，各级路由高亮与面包屑导航无缝对齐。
