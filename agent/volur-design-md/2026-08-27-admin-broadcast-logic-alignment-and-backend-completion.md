# AdminBroadcastView 与 volur-notification 前后端逻辑对齐与功能补齐记录

**施工日期**：2026-08-27  
**施工模块**：
- 前端：`src/volur-lite-view/src/views/admin/broadcast/AdminBroadcastView.vue`、`src/volur-lite-view/src/api/notification/index.ts`、`src/volur-lite-view/src/types/notification/index.ts`
- 后端：`src/volur-lite-platform/volur-notification`（Controller、Service、Repository、Entity、DTO、VO、Mapper）  
**责任主体**：Antigravity AI Agent

---

## 1. 变更背景与对齐目标

此前，前端页面 `AdminBroadcastView.vue` 与后端 `volur-notification` 模块之间存在严重的逻辑未对齐与功能缺失：
1. **数据持久化缺失**：后端原 `publishAnnouncement` 和 `publishPromotion` 仅将消息推送到 Redis Pub/Sub，未落库存储，导致前端历史列表查询 404/405，刷新页面后数据丢失。
2. **列表分页与过滤接口缺失**：缺少 `GET /api/v1/admin/broadcasts` 端点及其分页、标题/内容模糊检索、分类与展示类型过滤及动态排序能力。
3. **统计指标接口缺失**：前端顶部指标卡片使用的是静态数据，缺少后端 `GET /api/v1/admin/broadcasts/statistics` 动态聚合统计接口。
4. **广播删除与撤回能力缺失**：管理员无法对已发布的广播进行删除或撤回。
5. **前端交互与管理功能不完善**：缺少广播预览详情 Modal、分类筛选下拉框以及表格删除二次确认气泡（`NPopconfirm`）。

---

## 2. 核心改动点与实施细节

### 2.1 后端领域模型与持久化层建设 (volur-notification)
1. **实体类** `BroadcastMessage`：
   - 表名 `broadcast_message`，主键使用 `@SnowflakeGenerated` 生成雪花算法 ID；
   - 字段包含 `publisherId`、`publisherName`、`title`、`content`、`category`（`ANNOUNCEMENT` / `PROMOTION`）、`displayType`、`coverImageUrl`、`linkUrl`、`forcePopup`、`priority`、`expireTime`、`createdAt`、`updatedAt`；
   - 创建 `created_at` 与 `category, created_at` 联合索引。
2. **Repository 层** `BroadcastMessageRepository`：
   - 继承 `JpaRepository<BroadcastMessage, Long>` 与 `JpaSpecificationExecutor<BroadcastMessage>`；
   - 提供 `countByCategory` 与 `countByForcePopupTrue` 快速聚合统计方法。
3. **DTO / VO 扩展**：
   - `AdminBroadcastPageRequestDTO`：支持 `keyword`、`category`、`displayType`、`page`、`size`、`sortBy`、`sortOrder`；
   - `AdminBroadcastStatisticsVO`：返回 `totalAnnouncements`、`totalPromotions`、`urgentModals`、`sseOnlineStatus`；
   - `BroadcastMessageVO`：雪花算法 ID 序列化为 String（`ToStringSerializer`），补齐发布人与分类字段。

### 2.2 后端服务层与控制器实现
1. **服务层** `AdminBroadcastService` & `AdminBroadcastServiceImpl`：
   - `listBroadcasts(AdminBroadcastPageRequestDTO)`：动态 Specification 构建多条件检索与 `SortUtils` 分页；
   - `getStatistics()`：实时聚合全站系统公告数、运营推广数与紧急弹窗数；
   - `publishAnnouncement` / `publishPromotion`：自动解析并持久化发布人信息（`@CurrentUserId` -> `publisherName`），入库后通过 `realtimePushService.broadcastAll` 分发 SSE 广播；
   - `deleteBroadcast(Long id)`：按 ID 安全删除指定广播。
2. **控制器层** `AdminBroadcastController`：
   - 暴露 `GET /api/v1/admin/broadcasts`（分页查询）；
   - 暴露 `GET /api/v1/admin/broadcasts/statistics`（统计数据）；
   - 暴露 `POST /api/v1/admin/broadcasts/announcement`（发布公告）；
   - 暴露 `POST /api/v1/admin/broadcasts/promotion`（发布运营弹窗）；
   - 暴露 `DELETE /api/v1/admin/broadcasts/{id}`（删除广播）。

### 2.3 前端 API 与视图层升级
1. **API 与类型层**：
   - 在 `types/notification/index.ts` 中定义 `AdminBroadcastPageRequestDTO` 与 `AdminBroadcastStatisticsVO`；
   - 在 `api/notification/index.ts` 中封装 `getAdminBroadcastStatisticsApi`、`getAdminBroadcastListApi` 与 `deleteAdminBroadcastApi`。
2. **视图层 `AdminBroadcastView.vue`**：
   - 页面加载时自动请求 `getAdminBroadcastStatisticsApi` 渲染顶部指标卡片；
   - 过滤器增加“消息分类”（系统公告/运营推广/全部分类）以及展示形式下拉筛选；
   - 数据表格新增“操作”列：
     - **预览详情**：点击弹出富文本/配图/链接/元数据预览弹窗；
     - **删除/撤回**：集成 `NPopconfirm` 二次确认弹窗，删除后自动刷新表格并更新顶部统计。

---

## 3. 验证与构建测试

1. **后端验证**：
   ```powershell
   mvn clean compile
   ```
   **结果**：Reactor 全部 11 个子模块编译通过，`BUILD SUCCESS`（耗时 13.7s）。

2. **前端验证**：
   ```powershell
   npm run build
   ```
   **结果**：`vue-tsc` 0 错误，Vite 生产构建成功（耗时 4.99s）。

---

## 4. 影响范围与审计评估

- **改动文件清单**：
  - `src/volur-lite-platform/volur-notification/src/main/java/com/humolix/volur/notification/domain/entity/BroadcastMessage.java`
  - `src/volur-lite-platform/volur-notification/src/main/java/com/humolix/volur/notification/repository/BroadcastMessageRepository.java`
  - `src/volur-lite-platform/volur-notification/src/main/java/com/humolix/volur/notification/domain/dto/AdminBroadcastPageRequestDTO.java`
  - `src/volur-lite-platform/volur-notification/src/main/java/com/humolix/volur/notification/domain/vo/AdminBroadcastStatisticsVO.java`
  - `src/volur-lite-platform/volur-notification/src/main/java/com/humolix/volur/notification/domain/vo/BroadcastMessageVO.java`
  - `src/volur-lite-platform/volur-notification/src/main/java/com/humolix/volur/notification/mapper/NotificationMapper.java`
  - `src/volur-lite-platform/volur-notification/src/main/java/com/humolix/volur/notification/service/admin/AdminBroadcastService.java`
  - `src/volur-lite-platform/volur-notification/src/main/java/com/humolix/volur/notification/service/admin/impl/AdminBroadcastServiceImpl.java`
  - `src/volur-lite-platform/volur-notification/src/main/java/com/humolix/volur/notification/controller/v1/admin/AdminBroadcastController.java`
  - `src/volur-lite-view/src/types/notification/index.ts`
  - `src/volur-lite-view/src/api/notification/index.ts`
  - `src/volur-lite-view/src/views/admin/broadcast/AdminBroadcastView.vue`
- **业务闭环与缺陷排查说明**：
  - 完整打通了广播通知从“管理员创建 -> 数据库持久化 -> Redis 实时广播 -> 列表分页过滤 -> 详情预览 -> 删除撤回 -> 实时统计指标更新”的端到端全生命周期闭环。
  - **缺陷修复 (ObjectOptimisticLockingFailureException)**：实体类主键已声明 `@SnowflakeGenerated`，在执行 `save(entity)` 时严禁在业务层手动调用 `entity.setId(...)`，否则会导致 Spring Data JPA `SimpleJpaRepository.save()` 误将新实体识别为脱管游离态对象并调用 `em.merge()`，从而引发 Hibernate `StaleObjectStateException`。现已移除手动 `setId`，完全由 Hibernate ID 生成器在插入时自动生成。

