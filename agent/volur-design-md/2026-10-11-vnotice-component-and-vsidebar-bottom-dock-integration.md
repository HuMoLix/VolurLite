# VNotice 组件实现与 VSideBar 底部通知卡片集成方案

## 1. 变更背景与需求目标
在 `volur-lite-view-NEXT` 项目中，侧边栏底部操作区（Bottom Dock）原有通知按钮仅支持跳转至全量页面路由（`/notifications`）。为了提升工作台沉浸式交互体验，要求：
1. 在 `src/components/sidebar/` 目录下实现一个独立的通知卡片组件 `VNotice.vue`；
2. 参考 `usercard`（用户卡片）的交互设计规范，用户点击 Bottom Dock 中的通知按钮时，使用 `n-popover` 向上延展弹出一个具备高内聚功能的通知卡片；
3. 支持展开与折叠态（Expanded / Collapsed）双重场景适配；
4. 全面遵守 Google Material Design / Workspace 色彩与阶梯规范（Google Blue、Google Red、Google Green、Google Yellow 表面与状态指示），严禁全黑文本；
5. 全面遵守零注释铁律与单一真实源规范。

---

## 2. 核心架构设计与改动点

### 2.1 实体与接口层补充 (Types & API)
1. **类型定义 (`src/types/notification.ts`)**：
   - 严格映射后端 `com.humolix.volur.notification.domain` 实体规范；
   - 定义 `NotificationVO`、`NotificationQueryDTO`、`BroadcastMessageVO` 等强类型接口；
   - 彻底杜绝 `any`。
2. **请求客户端扩展 (`src/utils/request.ts` & `src/api/notification.ts`)**：
   - 在 `src/utils/request.ts` 中补充导出标准 `patch` 请求方法；
   - 在 `src/api/notification.ts` 中声明 `getNotificationListApi`（`GET /api/v1/notifications`）、`getUnreadNotificationCountApi`（`GET /api/v1/notifications/unread-count`）、`getNotificationDetailApi`（`GET /api/v1/notifications/{id}`）、`markAllNotificationsReadApi`（`PATCH /api/v1/notifications/read-all`）以及 `deleteNotificationApi`（`DELETE /api/v1/notifications/{id}`）。

### 2.2 状态管理层 (Pinia Setup Store: `notification.ts`)
在 `src/store/modules/notification.ts` 中构建 Pinia Setup Store：
- 集中管理 `notifications` 列表、`unreadCount` 未读计数、`totalCount` 总数与 `loading` 状态；
- 提供 `fetchUnreadCount`、`fetchNotifications`、`markAllAsRead`、`markAsRead`、`deleteNotification` 与 `reset` 方法；
- 与 `userStore.isLoggedIn` 全链路联动，登录时自动同步未读状态，登出时即刻清理。

### 2.3 独立通知组件 (`src/components/sidebar/VNotice.vue`)
组件定位为可独立复用的通知卡片，内部结构包括：
- **卡片顶部栏 (`card-top-layer`)**：
  - 通知标题与未读角标标签；
  - 快捷操作按钮组：包括“全部已读”按钮（当未读数大于 0 时可用）、“刷新”动画按钮与“关闭”按钮（`CloseOutline`）。
- **分类筛选标签 (`card-filter-tabs`)**：
  - Google 药丸风格（Pill）分类切换：“全部”、“未读”（带红色未读指示点）、“竞赛”、“系统”、“组队”。
- **卡片内容区 (`card-middle-layer`)**：
  - 未登录访客态：展示登录引导文案与 Google Blue 纯色“登录”按钮；
  - 骨架/加载态：轻量微调 Spinner；
  - 空数据态：展示 `NotificationsOffOutline` 及对应空列表提示；
  - 通知列表滚动区：
    - 按业务类别显示彩色语义图标背景（竞赛-琥珀黄、组队-翡翠绿、奖励-珊瑚红、系统-谷歌蓝）；
    - 标题（未读粗体）、相对时间计算（刚刚、N分钟前、N小时前、昨天、MM-DD）；
    - 正文两行溢出省略截断；
    - 竞赛关联快捷动作链（解析 `actionData` 并在点击时自动跳转目标赛事）；
    - 悬浮显示单条删除按钮与未读小蓝点。
- **卡片底部栏 (`card-bottom-layer`)**：
  - 总数展示（“共 N 条通知”）；
  - “前往通知中心”跳转动作（带 Chevron 图标，引导至完整通知管理页）。
- **通知详情弹窗 (`v-notice-detail-modal`)**：
  - Naive UI `NModal` 挂载展示完整通知长文案与操作流转按钮。

### 2.4 侧边栏与底部 Dock 集成 (`VSideBar.vue` & `SideBar.vue` & `AppLayout.vue`)
1. **参考 Usercard 机制的 Popover 向上延展**：
   - 展开态下，将通知按钮置于 `n-popover` 触发区内，配置 `:placement="'top'"`，关闭默认箭头，并移除 Naive UI 默认投影，由 `dock-notice-popover-card` 精准承载圆角与高斯弥散阴影；
   - 折叠态下，在应用工具弹出层中同样配置 `n-popover`，并使折叠应用按钮具备未读数 `n-badge` 感知；
2. **互斥互闭机制**：
   - 监听 `isNoticePopoverShow` 与 `isUserCardPopoverShow`，确保同一时刻底部 Dock 仅展开单一功能卡片；
   - 侧边栏展开/折叠切换时自动收起所有在途浮层；
3. **槽位与事件透传**：
   - `VSideBar.vue`、`SideBar.vue` 与 `AppLayout.vue` 统一向上透传 `#notice-card` 插槽以及 `@notice-card-open`、`@notice-card-close` 事件。

---

## 3. 影响范围与回归验证

1. **类型检查**：运行 `npx vue-tsc --noEmit`，结果 0 错误，类型系统保持 100% 严格对齐。
2. **构建验证**：运行 `npm run build`，打包产物构建成功（14.11s），生产 Chunk 完整无报错。
3. **文件变更清单**：
   - `src/utils/request.ts`
   - `src/types/notification.ts`
   - `src/api/notification.ts`
   - `src/store/modules/notification.ts`
   - `src/components/sidebar/types.ts`
   - `src/components/sidebar/index.ts`
   - `src/components/sidebar/VNotice.vue`
   - `src/components/sidebar/VSideBar.vue`
   - `src/components/sidebar/SideBar.vue`
   - `src/components/layouts/AppLayout.vue`
