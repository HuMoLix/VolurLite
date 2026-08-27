# 基础 PageRequestDTO 抽象与管理端用户分页查询逻辑重构

## 1. 变更背景与目标

在既有的控制层（Controller）与服务层（Service）实现中，`AdminUserController.listUsers` 接口采用平铺 `@RequestParam` 的方式接收 9 个离散请求参数（`page`, `size`, `keyword`, `role`, `status`, `startTime`, `endTime`, `sortBy`, `sortOrder`），导致方法签名冗长、扩展性较差，且缺乏统一的分页请求模型基类。

参考 Volur 架构设计（如 `WafInterfaceController` 与 `WafInterfacePageRequestDTO`），本次重构对分页请求与查询逻辑进行规范化改造：
1. 在 `volur-framework` 框架核心公共包下建立统一的基础分页请求传输对象基类 `PageRequestDTO`，封装通用分页、动态排序与 `Pageable` 转换能力；
2. 在 `volur-user` 业务模块中定义专用的 `AdminUserPageRequestDTO`，继承 `PageRequestDTO` 并声明该领域特有的筛选字段；
3. 重构 `AdminUserController.listUsers` 与 `AdminUserService.getUserList` 方法签名，使 Controller 极简化为对象入参并统一通过 `new PageResponse<>(pageResponse)` 输出标准响应；
4. 同步对齐前端 TypeScript 类型定义 `PageRequestDTO` 与 `AdminUserPageRequestDTO`，确保前后端实体与 API 契约 100% 严格对齐。

---

## 2. 核心改动点

### 2.1 新增基础分页请求类 `PageRequestDTO`
- **路径**：`com.humolix.volur.framework.common.dto.PageRequestDTO`
- **特性**：
  - 继承 `Serializable`，包含默认值 `page = 1`、`size = 10`、`sortOrder = "DESC"` 及可选的 `sortField`；
  - 提供 `setSortBy` / `getSortBy` 兼容别名方法，平滑适配前端可能传入的 `sortBy` 参数；
  - 内置 `toPageable(Class<?> entityClass, String defaultSortField)` 与 `toPageable(Class<?> entityClass, String defaultSortField, Sort.Direction defaultDirection)` 实用方法，结合 `PageSortUtil` 自动执行实体字段白名单校验，防止 SQL 注入风险。

### 2.2 新增管理端用户筛选请求类 `AdminUserPageRequestDTO`
- **路径**：`com.humolix.volur.user.domain.dto.AdminUserPageRequestDTO`
- **特性**：
  - 继承 `PageRequestDTO`，使用 Lombok `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@EqualsAndHashCode(callSuper = true)`；
  - 声明 `keyword`（用户名/邮箱/所属机构模糊匹配）、`role`（角色筛选）、`status`（状态筛选）、`startTime` 与 `endTime`（注册时间区间）。

### 2.3 重构 `AdminUserController` 与 `AdminUserService`
- **`AdminUserController.listUsers`**：
  - 由原先 9 个 `@RequestParam` 平铺参数重构为单一强类型入参 `AdminUserPageRequestDTO pageRequest`；
  - 返回值统一封装为 `ApiResponse<PageResponse<AdminUserDetailsVO>>`，使用 `ApiResponseEntity.ok("获取用户列表成功", new PageResponse<>(pageResponse))`。
- **`AdminUserService.getUserList`**：
  - 接口签名修改为 `Page<AdminUserDetailsVO> getUserList(AdminUserPageRequestDTO pageRequest)`；
  - 内部利用 `request.toPageable(User.class, "id")` 构建分页与排序规格，通过 JPA `Specification` 动态组装多条件查询。

### 2.4 前端 TypeScript 类型与 API 契约对齐
- **`src/volur-lite-view/src/types/common/api.ts`**：
  - 新增 `PageRequestDTO` 接口定义，并保留 `PageQuery` 类型别名保持向后兼容。
- **`src/volur-lite-view/src/types/user/index.ts`**：
  - 导出 `AdminUserPageRequestDTO extends PageRequestDTO`，并提供 `AdminUserQueryDTO` 别名。
- **`src/volur-lite-view/src/api/user/index.ts`**：
  - 更新 `getAdminUserListApi` 参数类型为 `AdminUserPageRequestDTO`。

### 2.5 后端编码规范更新 (`Rules.md`)
- 在 [`Rules.md`](file:///H:/HuMoLix/github/Volur-Lite/agent/volur-design-skills/Rules.md) 第 8 节（分页响应模型与业务场景选型规范）中新增 **8.4 分页请求入参与动态多条件查询规范 (Page Query DTO & Dynamic Multi-Condition Queries)**；
- 明确要求所有业务分页查询入参必须继承通用基类 `PageRequestDTO`，严禁在 Controller 平铺离散 `@RequestParam` 参数；
- 详细规范 Service 层基于 `request.toPageable(...)`、JPA `Specification`、关键字防御截断（`length() > 50`）、大小写不敏感（`cb.lower()`）与 `@Transactional(readOnly = true)` 的动态多条件查询标准实现模式，并补充完整反模式与推荐实践代码范例。

---

## 3. 安全隐患修复与质量保障说明

1. **SQL 注入与排序字段篡改防御**：
   - 沿用 `PageSortUtil` 机制，通过 `request.toPageable(User.class, "id")` 在运行时对客户端传入的 `sortField` 进行反射字段白名单校验与 Caffeine 缓存加速。非法字段将自动回退至默认排序字段（如 `"id"`），彻底杜绝非法字段引发的 SQL 注入或 ORM 报错。
2. **文本检索拒绝服务 (DoS) 与大小写注入防御**：
   - 严格在 Service 层对入参 `keyword` 执行 `.trim()` 去空，限制最大长度为 50 字符并进行截断保护，结合 `cb.lower(...)` 进行不区分大小写的模糊匹配，提高检索稳定性与健壮性。
3. **前后端类型契约一致性**：
   - 前后端字段命名统一，完全遵照 `Rules.md` 与 `view-Rules.md` 规范。
4. **验证与测试**：
   - 全模块 Maven 编译通过（`mvn clean test-compile`）；
   - 后端全部单元测试 100% 通过（`mvn test`）；
   - 前端 TypeScript 严格类型检查 100% 通过（`npx vue-tsc --noEmit` 无告警）。

---

## 4. 涉及文件清单

- **新增**：
  - `src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/dto/PageRequestDTO.java`
  - `src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/domain/dto/AdminUserPageRequestDTO.java`
  - `agent/volur-design-md/2026-08-27-page-query-dto-base-class-and-admin-user-controller-optimization.md`
- **修改**：
  - `agent/volur-design-skills/Rules.md`
  - `src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/service/admin/AdminUserService.java`
  - `src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/service/admin/impl/AdminUserServiceImpl.java`
  - `src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/controller/v1/admin/AdminUserController.java`
  - `src/volur-lite-view/src/types/common/api.ts`
  - `src/volur-lite-view/src/types/user/index.ts`
  - `src/volur-lite-view/src/api/user/index.ts`

