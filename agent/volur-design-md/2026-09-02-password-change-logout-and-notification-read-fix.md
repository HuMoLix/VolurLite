# 施工与审计记录：密码修改后会话清理与站内通知已读状态逻辑对齐

## 1. 基本信息

- **任务名称**：修改密码会话清除与重定向、站内信 `status` 字段已读对齐与合并详情已读接口
- **执行时间**：2026-09-02
- **影响模块**：
  - `volur-notification`：
    - `NotificationController.java`（移除单独的 `PATCH /api/v1/notifications/{id}/read` 路由，统一合并收敛至 `GET /api/v1/notifications/{id}`）
  - `volur-lite-view`：
    - `src/types/notification/index.ts`（更新 `NotificationVO` 为后端的 `status: 'UNREAD' | 'READ'` 枚举模型）
    - `src/api/notification/index.ts`（移除 `markNotificationReadApi`）
    - `src/components/notification/NotificationCenter.vue`（改用 `item.status === 'UNREAD'` 判定未读红点与高亮，点击直接调用 `getNotificationDetailApi` 自动置为已读）
    - `src/views/profile/ProfileView.vue`（密码修改成功后调用 `await userStore.logout()` 并立即 `await router.replace('/auth/login')`）

---

## 2. 变更说明与根本原因

### 2.1 为什么通知返回了 `READ` 但前端还是有红点？
- **根因**：后端 `NotificationVO` 的状态字段名为 `status`（枚举值 `UNREAD` / `READ`），而前端 TypeScript 实体与模板中错误地绑定了 `!item.read`。由于 `item.read` 始终为 `undefined`，`!undefined` 恒等于 `true`，导致所有通知都被误判为未读红点；
- **修复**：
  1. 将 TypeScript 接口和组件模板中的判定统一修正为 `item.status === 'UNREAD'`；
  2. 点击通知列表项时，调用 `GET /api/v1/notifications/{id}` 详情接口，后端在返回详情的同时自动标记为已读并更新 Redis 缓存，前端同步更新 `item.status = 'READ'` 并递减未读数；
  3. 彻底废除了冗余的 `PATCH /api/v1/notifications/{id}/read` 接口。

### 2.2 密码修改后会话清理与重定向
- 在 `ProfileView.vue` 中，用户成功修改密码或注销账号后：
  1. 弹出操作成功提示并清空密码输入表单；
  2. 执行 `await userStore.logout()` 彻底清空 Pinia 状态树与 LocalStorage 中的 Token 与用户信息；
  3. 执行 `await router.replace('/auth/login')` 强制无痕重定向至登录页。

---

## 3. 验证结论

- **前端构建**：执行 `npm run build` 打包构建 100% 通过（0 错误）。
- **后端测试**：全平台 12 个模块执行 `mvn test` 单元测试全部通过（0 失败，0 错误）。
