# Volur Lite 精简：移除前台首页与独立题库训练模块审计

## 变更背景
根据 Volur Lite（轻量竞技版）的产品定位，前台核心聚焦于竞赛演练（`Competitions`）与管理控制中心（`Admin Console`）。独立的营销落地页（`HomeView.vue`）与前台通用题库训练大厅（`ChallengeListView.vue`）属于标准版功能，在 Lite 版中无需暴露，需清理相关视图文件、路由配置与多语言包。

## 改动点详述

### 1. 文件与目录清理
- 删除 `src/volur-lite-view/src/views/home/HomeView.vue` 及 `views/home/` 目录；
- 删除 `src/volur-lite-view/src/views/challenge/ChallengeListView.vue` 及 `views/challenge/` 目录；
- 移除中英文语言包中无引用的 `src/locales/zh_CN/home.json`、`src/locales/en_US/home.json`、`src/locales/zh_CN/challenge.json` 与 `src/locales/en_US/challenge.json`。

### 2. 路由与菜单配置更新
- **`src/router/routes.ts`**：
  - 移除根路由下 `/challenges` 路由项；
  - 根路径 `/` 保持直接重定向至 `/competitions`（竞赛演练大厅）。
- **`src/layout/AppLayout.vue`**：
  - 侧边栏菜单移除 `Challenges`（题库训练）项，仅保留 `Competitions`（竞赛演练）；
  - 清理未使用的 `@vicons/ionicons5` 图标导入。
- **`src/locales/zh_CN/layout.json` & `src/locales/en_US/layout.json`**：
  - 同步清理前台侧边栏无效的 `challenges` 多语言键名。

## 安全与影响范围分析
1. **打包体积精简**：清理了非必要的首页和题库组件后，前端生产包 chunk 体积进一步优化；
2. **路由守卫安全**：系统默认入口直接进入竞赛大厅，未登录用户可查看赛事列表，登录/鉴权逻辑与管理员面板跳转保持完全正常；
3. **构建验证**：全量执行 `vue-tsc --noEmit && vite build` 生产构建成功，无任何隐式引用或未定义报错。
