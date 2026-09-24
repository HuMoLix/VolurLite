# 统计数据接口统一化(/stats)与选项卡切换双重触发请求缺陷排查修复报告

## 1. 变更背景与问题分析

### 1.1 选项卡点击双重请求排查分析
在用户访问比赛管理详情页（`AdminCompetitionEditView.vue`）并点击切换至“队伍管理”选项卡（`TeamAuditTab.vue`）时，前端浏览器网络面板出现两次 `/api/v1/admin/competitions/{competitionId}/teams/stats` 及队伍列表请求。

经过深层源码排查与调用栈追踪分析：
1. **直接诱因**：
   - 全局布局框架 `AdminLayout.vue` 与 `AppLayout.vue` 中对 `<router-view>` 的动态组件绑定了路由 Key：
     ```vue
     <router-view v-slot="{ Component }">
       <transition name="fade" mode="out-in">
         <component :is="Component" :key="currentRoute.fullPath" />
       </transition>
     </router-view>
     ```
2. **连锁反应机制**：
   - 比赛管理详情页 `AdminCompetitionEditView.vue` 通过 URL Query 参数记录当前激活的选项卡（如 `?tab=teams`）。
   - 在用户点击选项卡时，`handleTabChange` 执行了 `router.replace({ query: { ...route.query, tab: key } })`。
   - 由于 `:key="currentRoute.fullPath"` 绑定了包含 Query 的完整 URL 路径，当 `tab` 发生变化时，Vue 认为路由对应的宿主组件发生了变化，导致父级视图组件 `AdminCompetitionEditView.vue` 触发整体卸载（Unmount）并重新挂载（Mount）。
   - 首次请求由选项卡切换触发挂载发出，随后由于父组件重建重新挂载，子组件 `TeamAuditTab.vue` 的 `onMounted` 钩子再次被执行，导致向后端连发两次一模一样的统计与列表数据请求。
3. **修复策略**：
   - 将 `AdminLayout.vue` 与 `AppLayout.vue` 中的 `:key="currentRoute.fullPath"` 规范调整为 `:key="currentRoute.path"`。
   - 在纯查询参数（Query / Hash）变化（如分页参数、过滤器、内部选项卡切换）时复用现有组件实例，杜绝无意义的父组件频繁销毁与重挂载，彻底消除双重重复请求。

### 1.2 全平台数据统计接口统一标准化 (/stats)
在系统演进过程中，部分模块采用 `/statistics` 路径，部分模块（如 `teams/stats`、`realtime/stats`）采用 `/stats` 路径。为了严格遵守 RESTful 精炼设计风格与项目 API 命名规范，全系统将全部数据统计接口统一收敛为简洁的 `/stats`，并确保前后端 100% 严格对齐。

---

## 2. 变更详细内容

### 2.1 布局层组件复用优化 (Frontend Layouts)
- **`src/volur-lite-view/src/public/volurEx/layouts/AdminLayout.vue`**：
  - 将 `<component :is="Component" :key="currentRoute.fullPath" />` 调整为 `:key="currentRoute.path"`。
- **`src/volur-lite-view/src/public/volurEx/layouts/AppLayout.vue`**：
  - 将 `<component :is="Component" :key="currentRoute.fullPath" />` 调整为 `:key="currentRoute.path"`。

### 2.2 后端控制器统计接口路由统一规范 (@GetMapping("/stats"))
将全平台所有定义了 `@GetMapping("/statistics")` 的控制器统一变更为 `@GetMapping("/stats")`：
1. **`volur-competition`**：
   - `AdminCompetitionLogController.java`：`@GetMapping("/statistics")` -> `@GetMapping("/stats")`
2. **`volur-container`**：
   - `AdminContainerController.java`：`@GetMapping("/statistics")` -> `@GetMapping("/stats")`
   - `AdminImageController.java`：`@GetMapping("/statistics")` -> `@GetMapping("/stats")`
3. **`volur-logging`**：
   - `AdminLogController.java`：`@GetMapping("/statistics")` -> `@GetMapping("/stats")`
4. **`volur-notification`**：
   - `AdminBroadcastController.java`：`@GetMapping("/statistics")` -> `@GetMapping("/stats")`
5. **`volur-system`**：
   - `AdminAsyncTaskController.java`：`@GetMapping("/statistics")` -> `@GetMapping("/stats")`
6. **`volur-user`**：
   - `AdminUserController.java`：`@GetMapping("/statistics")` -> `@GetMapping("/stats")`
7. **`volur-verification`**：
   - `AdminEmailController.java`：`@GetMapping("/statistics")` -> `@GetMapping("/stats")`

### 2.3 前端 API 调用层严格对齐 (Frontend API)
将 `src/volur-lite-view/src/api/**` 下所有数据统计接口统一改为请求 `/stats`：
1. `src/volur-lite-view/src/api/competition/index.ts`：`/admin/competitions/${competitionId}/logs/statistics` -> `/admin/competitions/${competitionId}/logs/stats`
2. `src/volur-lite-view/src/api/container/index.ts`：`/admin/containers/statistics` -> `/admin/containers/stats`
3. `src/volur-lite-view/src/api/image/index.ts`：`/admin/images/statistics` -> `/admin/images/stats`
4. `src/volur-lite-view/src/api/log/index.ts`：`/admin/logs/statistics` -> `/admin/logs/stats`
5. `src/volur-lite-view/src/api/notification/index.ts`：`/admin/broadcasts/statistics` -> `/admin/broadcasts/stats`
6. `src/volur-lite-view/src/api/task/index.ts`：`/admin/tasks/statistics` -> `/admin/tasks/stats`
7. `src/volur-lite-view/src/api/user/index.ts`：`/admin/users/statistics` -> `/admin/users/stats`
8. `src/volur-lite-view/src/api/verification/index.ts`：`/admin/emails/statistics` -> `/admin/emails/stats`

---

## 3. 验证与构建结果

1. **前端生产打包构建测试**：
   - 执行 `npm run build`，TypeScript 类型校验及 Vite 打包全部通过，无任何编译或路由异常。
2. **后端子模块编译测试**：
   - 执行 `mvn test-compile -pl volur-challenge,volur-container,volur-competition,volur-logging,volur-system,volur-user,volur-verification`，所有涉及模块编译完全通过（BUILD SUCCESS）。
3. **全局检索验证**：
   - 全局扫描 `"/statistics"`，前端与后端控制器均已 100% 清零并统一为 `"/stats"`。
