# 施工与审计记录：统一 avatarUrl 字段规范与移除多余兼容代码

## 1. 基本信息

- **任务名称**：全站彻底统一使用 `avatarUrl` 命名规范、清理 LocalStorage 迁移兼容代码
- **执行时间**：2026-09-02
- **影响模块**：
  - `volur-lite-view`：
    - `src/utils/auth.ts`（彻底移除 `getUserInfo` 中的 `avatar` 属性降级逻辑，全量统一为原生 `avatarUrl`）
    - `src/types/auth/index.ts`（`AuthenticateResponseVO` 的 `avatar?: string` 修正为 `avatarUrl?: string`）
    - `src/store/modules/user.ts`（严格使用 `UserSummaryVO` 的 `avatarUrl`）
    - `src/layout/AppLayout.vue` 与 `src/layout/AdminLayout.vue`（统一绑定 `avatarUrl`）

---

## 2. 变更说明

- 前端存储与状态模型不再包含历史遗留的 `avatar` 兼容字段，全站 API（`/api/v1/users/me`、`/api/v1/users/profile`、`/api/v1/users/avatar`）与 Pinia 实体、LocalStorage 数据均唯一使用 `avatarUrl`；
- 构建与测试验证均 100% 通过。
