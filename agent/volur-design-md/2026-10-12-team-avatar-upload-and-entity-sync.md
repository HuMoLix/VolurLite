# 战队头像上传支持与领域实体同步改造技术方案

## 1. 变更背景与需求概述

在 `volur-lite-view-NEXT` 中统一了战队创建、查看与编辑弹窗（`TeamFormModal.vue`）后，需要进一步完善战队元信息的维护能力，支持战队头像（Avatar）的动态上传与更新。同时，需要对后端实体（`CompetitionTeam`）、服务实现（`CompetitionTeamServiceImpl`）以及数据库平滑升级脚本（`scripts/upgrade/upgrade_to_latest.sql`）进行全链路一致性对齐与 Fail-Fast / Zero-Mock 防护。

---

## 2. 核心改动点与架构设计

### 2.1 新一代前端系统存储接口扩展
- **类型定义 (`src/volur-lite-view-NEXT/src/types/system.ts`)**：
  - 新增 `StorageResponseVO` 强类型接口，严格映射后端 `com.humolix.volur.system.domain.vo.StorageResponseVO`（包含 `fileName`, `filePath`, `fileSize`, `sha256`）。
- **API 封装 (`src/volur-lite-view-NEXT/src/api/system.ts`)**：
  - 新增 `uploadImageApi(file: File | FormData, folder?: string)`，支持传入原生 `File` 对象或 `FormData`，自动构建 `multipart/form-data` 请求并以 `POST /api/v1/system/upload/image` 与后端存储微服务对接。
  - 新增 `uploadResourceApi(file: File | FormData, folder?: string)` 通用资源上传接口。

### 2.2 统一表单组件头像上传与交互设计 (`TeamFormModal.vue`)
- **Google Material / Workspace 风格交互遮罩**：
  - 将 `VAvatar` 包装在相对定位容器中，在 `isEditing && isCaptain` 状态下激活悬浮交互；
  - 悬浮时展示带圆角的半透明暗色遮罩（`bg-black/40`）与相机图标（`CameraOutline`），提示“上传战队头像”或“更换战队头像”；
  - 上传中自动展示白色旋转指示器（`NSpin`），并进入等待状态（`cursor-wait`）；
  - 已上传头像时，右上角提供快速清除按钮（`CloseOutline`），支持一键清空头像数据。
- **文件校验与防抖上传逻辑**：
  - 格式限制：严格限制 `image/jpeg`、`image/png`、`image/webp`、`image/gif`；
  - 体积限制：严格限制 5MB，超出时前端提前拦截并弹出明确错误提示；
  - 上传成功后自动绑定至 `formData.avatarUrl`。
- **更新与报名请求载荷优化**：
  - 编辑提交（`updatePayload`）时，若头像被清空则传递空字符串 `""`，以便后端感知清空动作；
  - 报名提交（`signupPayload`）时，若无头像则传递 `undefined`。

### 2.3 多语言国际化补齐 (`competition.json`)
- 同步补齐 `zh-CN`、`en-US`、`zh-TW` 中的头像上传文案：
  - `upload_team_avatar`
  - `change_team_avatar`
  - `avatar_upload_success`
  - `avatar_upload_failed`
  - `avatar_type_error`
  - `avatar_size_error`

### 2.4 后端服务规范化与 Fail-Fast / Zero-Mock 防护
- **领域实体 (`CompetitionTeam.java`) 验证**：
  - `avatarUrl` 字段通过 `@Column(name = "avatar_url", columnDefinition = "VARCHAR(255)")` 正确映射数据表字段。
- **服务层实现 (`CompetitionTeamServiceImpl.java`)**：
  - **消除隐式 Mock 假数据**：移除报名战队时 `StringUtils.hasText(request.getAvatarUrl()) ? request.getAvatarUrl() : "/api/v1/uploads/avatar/default_team.png"` 中的硬编码假数据兜底，彻底杜绝违背 Zero-Mock 原则的伪造图片路径；
  - **支持战队头像更新与安全清空**：在 `updateTeam` 中，当 `request.getAvatarUrl() != null` 时，依据 `StringUtils.hasText` 判断安全赋值真实 URL 或清空为 `null`。

### 2.5 数据库平滑升级脚本幂等性补齐 (`scripts/upgrade/upgrade_to_latest.sql`)
- 在存储过程 `migrate_volur_schema_20260921` 的 `3.2 competition_team 表结构扩充` 小节中：
  - 补充对 `avatar_url` 字段的存在性探测与增量 DDL：
    ```sql
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'competition_team' AND COLUMN_NAME = 'avatar_url'
    ) THEN
        ALTER TABLE `competition_team` ADD COLUMN `avatar_url` varchar(255) COLLATE utf8mb4_unicode_ci;
    END IF;
    ```
  - 严格保持幂等性设计，确保在升级远程生产库时平滑无阻断。

---

## 3. 安全性与影响范围

1. **类型安全**：前后端 DTO/VO 属性名称 100% 映射对齐，消除 `any` 类型。
2. **权限与状态隔离**：只有在队长身份且进入编辑/创建模式下才展示头像上传控件，普通队员或仅查看模式保持纯静态渲染。
3. **无破坏性变更**：更新和报名接口完全向下兼容，未提供头像时正确置空。
