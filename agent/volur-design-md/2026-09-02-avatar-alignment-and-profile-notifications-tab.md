# 施工与审计记录：顶部栏头像对齐修复与个人中心站内信完整模块实现

## 1. 基本信息

- **任务名称**：顶部栏头像与用户名垂直对齐优化、个人中心新增站内信消息独立选项卡与“查看更多”联动
- **执行时间**：2026-09-02
- **影响模块**：
  - `volur-lite-view`：
    - `src/layout/AppLayout.vue`（`.user-profile` 垂直对齐优化与 `line-height: 1` 修正）
    - `src/layout/AdminLayout.vue`（管理员端 `.user-profile` 垂直对齐优化）
    - `src/components/notification/NotificationCenter.vue`（底部添加“查看更多”按钮，跳转至 `/profile?tab=notifications`）
    - `src/views/profile/ProfileView.vue`（新增 `notifications` 站内信独立 Tab 模块，包含分类过滤、分页查询、详情弹窗、已读同步与一键已读、删除功能）
    - `src/locales/zh_CN/profile.json` & `src/locales/en_US/profile.json`（新增站内信相关完整中英双语国际化词条）

---

## 2. 变更说明

### 2.1 顶部栏头像与用户名对齐
- **问题**：在 `AppLayout.vue` 与 `AdminLayout.vue` 中，`<n-avatar>` 和 `<span>` 文本在默认行高和基线对齐机制下存在微小高度偏差；
- **优化**：
  - `.user-profile` 设置为 `display: inline-flex; align-items: center; gap: 10px;`；
  - `:deep(.n-avatar)` 显式声明 `display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0;`；
  - `.user-nickname` 声明 `line-height: 1; display: inline-flex; align-items: center;`，实现像素级居中对齐。

### 2.2 个人中心新增站内信消息模块
- 在 `ProfileView.vue` 的主要选项卡中新增 `tab_notifications`（站内消息），支持：
  1. **分类过滤**：全部 / 未读 / 系统通知 / 比赛事件 / 组队协作；
  2. **批量与快捷操作**：一键全部已读、手动刷新、删除单条通知；
  3. **详情弹窗与自动已读**：点击列表卡片弹出详情 Modal 并调用 `GET /api/v1/notifications/{id}` 自动标为已读，未读小红点与计数器实时同步扣减；
  4. **分页支持**：支持 `10 / 20 / 50` 条每页无缝切换；
  5. **路由联动**：支持 URL 参数直接激活（`/profile?tab=notifications`），并在顶部栏浮窗的“查看更多”按钮中直接路由跳转。

---

## 3. 验证结论

- **前端构建**：执行 `npm run build` 打包编译 100% 通过（0 错误）。
- **后端测试**：全平台 12 个模块执行 `mvn test` 单元测试全部通过（0 失败，0 错误）。
