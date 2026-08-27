# 侧边栏全站公告与顶栏站内信通知中心施工记录

**施工日期**：2026-08-27  
**施工模块**：
- 前端：`src/volur-lite-view/src/layout/AppLayout.vue`、`src/volur-lite-view/src/components/notification/NotificationCenter.vue`、`src/volur-lite-view/src/views/announcement/AnnouncementListView.vue`、`src/volur-lite-view/src/router/routes.ts`、`src/volur-lite-view/src/api/notification/index.ts`、`src/volur-lite-view/src/locales/`
- 后端：`src/volur-lite-platform/volur-notification/src/main/java/com/humolix/volur/notification/controller/v1/BroadcastController.java`、`src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/config/SecurityConfig.java`  
**责任主体**：Antigravity AI Agent

---

## 1. 变更背景与架构目标

为了让普通前台用户能够便捷地查看管理员发布的全站系统公告/运营活动，并在登录后及时查收和处理个人站内信通知（如系统通知、组队邀请、赛事动态、荣誉奖励等），在主布局 `AppLayout.vue` 中补齐两大核心交互入口：
1. **侧边栏公告栏导航（Sidebar Announcement Navigation）**：
   - 在侧边栏导航菜单中新增“全站公告”（`Announcements`）条目，配置 `MegaphoneOutline` 图标，点击路由直达 `/announcements`。
   - 建设前台用户全站公告展示页 [`AnnouncementListView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/announcement/AnnouncementListView.vue)，支持卡片展示、分类检索、展示形式过滤、封面配图与详情预览弹窗。
2. **顶栏站内信通知中心（Header In-App Notification Center）**：
   - 在顶栏右侧用户信息（头像/昵称）左侧，新增站内信铃铛入口 [`NotificationCenter.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/components/notification/NotificationCenter.vue)；
   - 结合 `NBadge` 动态展示未读消息数（99+ 截断），点击弹出气泡卡片，包含全量/未读/分类 Tab 切换、一键已读、点击查看详情并自动标已读、删除通知等完整功能；
   - 接入 SSE 实时长连接监听（`notification` 事件），新通知毫秒级提示并递增未读角标。
3. **后端公共公告路由与开放放行**：
   - 新增 [`BroadcastController`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-notification/src/main/java/com/humolix/volur/notification/controller/v1/BroadcastController.java) 提供用户端 `/api/v1/broadcasts` 分页及详情查询；
   - 在 `volur-auth` 的 [`SecurityConfig`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/config/SecurityConfig.java) 中对 `/api/v1/broadcasts/**` 执行 `permitAll()`，允许游客及登录用户免认证查阅公共公告。

---

## 2. 核心改动点与实施细节

### 2.1 侧边栏菜单与路由配置
- 在 `AppLayout.vue` 的 `menuOptions` 中追加：
  ```ts
  {
    label: () => t('layout.announcements'),
    key: 'Announcements',
    icon: () => h(NIcon, null, { default: () => h(MegaphoneOutline) })
  }
  ```
- 在 `router/routes.ts` 的 `AppLayout` 子路由中添加 `/announcements`。

### 2.2 站内信组件封装 (`NotificationCenter.vue`)
- **角标与触发器**：`NBadge` + 铃铛按钮；
- **气泡通知面板**：
  - 顶部操作栏：未读计数高亮、"全部已读"、"刷新"；
  - 分类切换：`ALL` (全部), `UNREAD` (未读), `SYSTEM` (系统), `COMPETITION` (比赛), `TEAM` (组队)；
  - 列表交互：未读蓝/红点提示、类型图标、时间格式化（刚刚、X分钟前、X小时前）、删除按钮；
  - 详情 Modal：点击条目弹出详情，自动调用 `markNotificationReadApi` 标为已读并递减未读数；
  - 实时推送：基于 `SseClient` 监听 `notification` 事件。

### 2.3 前台公告视图 (`AnnouncementListView.vue`)
- 采用响应式自适应网格 `repeat(auto-fill, minmax(340px, 1fr))`；
- 卡片包含封面配图、优先级徽章（`P8`、`P5`）、展示形式与分类 Tag、发布人、发布时间与内容摘要；
- 详情弹窗支持查看完整内容与跳转关联链接。

---

## 3. 验证与构建测试

1. **后端验证**：
   ```powershell
   mvn clean compile
   ```
   **结果**：Reactor 全部 11 个模块编译通过，`BUILD SUCCESS`（耗时 18.1s）。

2. **前端验证**：
   ```powershell
   npm run build
   ```
   **结果**：`vue-tsc` 0 错误，Vite 打包成功（耗时 6.20s）。

---

## 4. 影响范围与审计评估

- **改动文件清单**：
  - `src/volur-lite-view/src/layout/AppLayout.vue`
  - `src/volur-lite-view/src/components/notification/NotificationCenter.vue`
  - `src/volur-lite-view/src/views/announcement/AnnouncementListView.vue`
  - `src/volur-lite-view/src/router/routes.ts`
  - `src/volur-lite-view/src/api/notification/index.ts`
  - `src/volur-lite-view/src/locales/zh_CN/layout.json`
  - `src/volur-lite-view/src/locales/en_US/layout.json`
  - `src/volur-lite-platform/volur-notification/src/main/java/com/humolix/volur/notification/controller/v1/BroadcastController.java`
  - `src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/config/SecurityConfig.java`
- **用户体验闭环**：
  - 实现了普通用户与竞赛选手在前台对系统通告、赛事公告的完整查阅能力；
  - 实现了用户登录后对个人站内信的高效收发、实时提醒、分类检索与批量处理。
