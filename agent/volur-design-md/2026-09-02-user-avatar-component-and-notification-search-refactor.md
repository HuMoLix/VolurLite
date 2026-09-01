# 施工与审计记录：站内信搜索与状态过滤重构、独立无边框头像组件 UserAvatar 实现

## 1. 基本信息

- **任务名称**：个人中心站内信去除分类标签改为搜索栏与状态过滤、无边框头像组件抽取与全站统一替换
- **执行时间**：2026-09-02
- **影响模块**：
  - `volur-notification`（后端）：
    - `NotificationQueryDTO.java`（新增 `keyword` 关键词搜索参数）
    - `NotificationServiceImpl.java`（`listNotifications` 方法中增加根据 `keyword` 对 `title` 和 `content` 的模糊匹配 `Predicate`）
  - `volur-lite-view`（前端）：
    - `src/components/common/UserAvatar.vue`（新创建可复用的强类型通用头像组件，支持各种尺寸、首字母降级、无描边设计与可选悬浮更换头像遮罩）
    - `src/api/notification/index.ts`（更新 `getNotificationListApi` 支持 `keyword` 和 `status` 参数）
    - `src/views/profile/ProfileView.vue`（移除通知分类 Tab，改用 `AdminBroadcastView.vue` 风格的搜索栏 `filter-toolbar` 与状态选择器，并使用 `UserAvatar` 组件）
    - `src/layout/AppLayout.vue`（替换顶部栏用户头像为 `UserAvatar`）
    - `src/layout/AdminLayout.vue`（替换管理端顶部栏用户头像为 `UserAvatar`）
    - `src/locales/zh_CN/profile.json` & `src/locales/en_US/profile.json`（新增搜索栏与状态查询国际化词条）

---

## 2. 变更说明

### 2.1 独立无边框头像组件 (`UserAvatar.vue`)
- **设计目标**：统一全站头像渲染机制，彻底去除头像外框描边，并封装 Naive UI 文本插槽防劫持逻辑。
- **核心特性**：
  1. **无描边设计**：彻底移除所有 border、outline 与 box-shadow；
  2. **智能降级展示**：仅当没有图片或图片加载失败时渲染用户名首字母大写文本；
  3. **交互与定制能力**：支持 `size`（像素或语义化尺寸）、`round`（默认圆形）、`editable`（悬浮半透明相机遮罩与更换头像提示）；
  4. **全站替换**：统一应用于个人中心主卡片、前台顶部栏、管理端顶部栏。

### 2.2 站内信搜索与状态过滤重构
- **变更对比**：
  - **旧方案**：使用固定分类 Tab（全部/未读/系统/比赛/组队）且不支持模糊搜索；
  - **新方案**：参考 `AdminBroadcastView.vue` 的工具栏设计，提供：
    1. **搜索输入框**：支持通知标题与内容的实时或回车模糊搜索；
    2. **状态筛选器**：支持「全部状态 / 未读通知 / 已读通知」精准过滤；
    3. **操作按钮**：「查询」黑色主按钮与「重置」按钮；
    4. **批量操作**：右侧保留「全部标记为已读」按钮（未读数为 0 时自动置灰）。

---

## 3. 验证结论

- **前端构建**：执行 `npm run build` 打包构建 100% 通过（0 错误）。
- **后端测试**：全平台 12 个模块执行 `mvn clean test` 编译测试 100% 通过（0 失败，0 错误）。
