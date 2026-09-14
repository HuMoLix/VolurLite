# 竞赛草稿状态公开端访问拦截与404跳转对齐设计与实现文档

## 1. 变更背景与需求概述
在 Volur Lite 平台中：
1. **公开端控制器数据泄漏风险**：原 `CompetitionController.java` 和 `CompetitionServiceImpl.java` 在通过 ID、Slug 或获取竞赛介绍（about）时，仅判断了竞赛是否存在，未对竞赛状态处于 `DRAFT`（草稿状态）做访问控制。普通选手只要获知竞赛 ID 或 Slug，即可调用公开接口提前读取草稿竞赛的详情与规则，造成非预期的信息泄漏；
2. **前台缺失空内容与未发布态跳转 404 机制**：原 `CompetitionDetailView.vue` 在接口返回空数据或报错时，仅简单弹出轻提示（Toast），选手仍停留在白屏或残缺的竞赛详情骨架页中，未能像标准 Web 体验一样直接进入 404 页面。

为此，本改动对公开接口进行草稿内容阻断，并在前端详情页对返回空内容（`data == null`）或加载异常实现统一跳转到 404 页面。

---

## 2. 核心架构与改动点说明

### 2.1 后端公开端访问控制改造

#### 2.1.1 业务服务层：`CompetitionServiceImpl.java`
- 对 `getCompetitionBySlug(String slug)`、`getCompetitionById(Long id)` 以及 `getCompetitionAbout(Long id)` 进行防御：
  - 若竞赛不存在，直接返回 `null`；
  - 刷新生命周期状态（`refreshStatus`）；
  - 若刷新后竞赛状态为 `CompetitionStatus.DRAFT`，直接返回 `null`，确保草稿状态竞赛对非管理端服务层完全不可见。

#### 2.1.2 控制器层：`CompetitionController.java`
- 遵循 Controller 薄层与规范：
  - `getCompetitionBySlug(@PathVariable String slug)`：当服务层返回 `null` 或状态为 `DRAFT` 时，返回 `ApiResponseEntity.ok(null)`，不暴露草稿竞赛详情；
  - `getCompetitionById(@PathVariable Long id)`：当服务层返回 `null` 或状态为 `DRAFT` 时，返回 `ApiResponseEntity.ok(null)`；
  - `getCompetitionAbout(@PathVariable Long id)`：当关于信息为空或状态为 `DRAFT` 时，返回 `ApiResponseEntity.ok(null)`。

> **注**：管理端接口（`AdminCompetitionController.java`）保持不变，管理员可在后台管理界面正常获取草稿竞赛以供编辑和前台预览。

---

### 2.2 前端竞赛详情页跳转与管理员预览支持

#### 2.2.1 路由配置补充：`routes.ts`
- 显式声明 `/404` 路由指向 `NotFoundView.vue`，并将 `/:pathMatch(.*)*` 通配兜底重定向到 `/404`。

#### 2.2.2 竞赛详情页：`CompetitionDetailView.vue`
1. **管理员前台预览模式适配 (`isAdminPreview`)**：
   - 处于管理员前台预览模式（`/admin/competitions/:id/preview`）时，调用管理端专用的 `getAdminCompetitionByIdApi(rawTarget)`，确保管理员可以预览尚未发布的草稿竞赛；
   - 处于普通公开端访问时，调用公开端 `getCompetitionByIdApi` / `getCompetitionBySlugApi`。
2. **返回内容为空跳转 404 (`router.replace('/404')`)**：
   - 当 `res?.data` 为空时（如后端返回 `data: null`），立即执行 `router.replace('/404')`；
   - 当网络请求发生异常（`catch` 块）时，同样统一跳转 `router.replace('/404')`；
   - 规范映射 `CompetitionDetailVO` 字段，避免白屏或不一致问题。

---

## 3. 构建与验证
1. **后端构建验证**：
   - 执行 `mvn clean compile -DskipTests`，全部 12 个模块成功编译（`BUILD SUCCESS`）。
2. **前端构建验证**：
   - 执行 `npm run build`，Vue 严格类型检查与 Vite 生产打包通过（`built in 4.96s`），无任何 TS 报错。

---

## 4. 影响范围与回归建议
- **受影响范围**：
  - 后端：`volur-competition` 公开端控制器及详情查询实现；
  - 前端：竞赛详情视图 `CompetitionDetailView.vue` 及全局 404 路由映射。
- **回归建议**：
  1. 新建一个状态为 `DRAFT` 的竞赛；
  2. 使用普通选手身份访问 `/competitions/{slug}` 或 `/competitions/{id}`，确认接口返回 `data: null`，页面立即重定向至 `/404` 页面；
  3. 管理员在后台点击“前台预览”，确认能够正常渲染预览页面（显示“当前正在以管理员视角实时预览竞赛前台效果”）。
