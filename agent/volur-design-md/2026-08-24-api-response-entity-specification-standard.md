# 规范确立与施工记录：ApiResponseEntity 与 ApiResponse 统一使用规范

## 1. 基本信息

- **规范名称**：统一 API 响应封装规范 (`ApiResponseEntity` & `ApiResponse`)
- **修订文件**：
  - [`volur-design-skills/Rules.md`](file:///H:/HuMoLix/github/Volur-Lite/volur-design-skills/Rules.md)（第 3.3 节）
  - [`GEMINI.md`](file:///H:/HuMoLix/github/Volur-Lite/GEMINI.md)（第 1.3 节）
  - [`ApiResponseEntity.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/api/ApiResponseEntity.java)
- **生效时间**：2026-08-24
- **规范等级**：强制性约束（Mandatory）

---

## 2. 规范背景与解决的核心问题

在项目前期的 Controller 开发中，存在以下响应封装混用与不规范问题：
1. **大小写混用**：部分 Controller 使用 `ApiResponseEntity.OK(...)` / `SUCCESS(...)`，部分使用小写 `ok(...)` / `success(...)`，命名不统一且违背 Java 静态工厂方法小写驼峰规范；
2. **`ok` 与 `success` 语义混淆**：在删除、状态切换、命令式发信等无数据返回的方法中，大量存在 `ApiResponseEntity.OK("删除成功", null)` 的反模式；
3. **`created` 语义缺失**：在新建实体资源（HTTP 201）的接口中，普遍误用 200 OK 响应，未充分体现 RESTful 架构语义。

---

## 3. 约束性规范详细定义

### 3.1 方法命名大小写规范
- **强制小写驼峰**：Controller 中调用 `ApiResponseEntity` 必须统一使用小写驼峰静态工厂方法（如 `ok`, `success`, `created`, `badRequest` 等），与 Spring `ResponseEntity` 设计哲学保持高度一致。
- **大写方法已废弃**：`ApiResponseEntity` 中的全部大写方法（`OK`, `SUCCESS`, `CREATED` 等）已标注 `@Deprecated`，新编写代码严禁调用。

### 3.2 `ok` vs `success` 使用边界
- **`ApiResponseEntity.ok(data)` / `ApiResponseEntity.ok(message, data)`**：
  - **核心定位**：**专用于有数据负载（Payload / Data）**的成功场景。
  - **返回类型**：`ApiResponse<T>`（`data != null`，如 `ApiResponse<UserSummaryVO>`, `ApiResponse<Page<UserVO>>`, `ApiResponse<Boolean>` 等）。
  - **适用操作**：GET 查询详情/列表/分页、登录获取 Token、提交判题返回判定结果等。
- **`ApiResponseEntity.success(message)`**：
  - **核心定位**：**专用于无具体数据返回（纯命令式操作、状态确认或动作执行完成）**的成功场景。
  - **返回类型**：`ApiResponse<Void>`（内部 `data` 统一包装为 `null`）。
  - **适用操作**：DELETE 删除数据、PATCH/PUT 状态切换、命令式 POST（如发送验证码、重置密码、修改密码、注销账号、退出登录）。
  - **反模式杜绝**：严禁使用 `ok("...", null)` 或 `OK("...", null)`。

### 3.3 `created` 资源创建规范 (HTTP 201 Created)
- **`ApiResponseEntity.created(data)` / `ApiResponseEntity.created(message, data)`**：
  - **核心定位**：严格遵循 RESTful 规范（RFC 7231 / HTTP 201），用于在服务端**成功持久化新增了独立资源实体**的场景。
  - **返回类型**：`ApiResponse<T>`，携带新创建实体的主键 ID 或详细 VO 视图。
  - **适用操作**：后台新增用户（`createUser`）、管理员创建赛题（`createChallenge`）、发布新文章等。
  - **不适用操作**：用户登录、发送验证码、提交 Flag 等不生成持久化新实体的操作严禁使用 `created`。

---

## 4. 常用响应方法对照速查表 (Cheat Sheet)

| 业务场景 | HTTP Method | 规范方法 | 返回类型 | HTTP 状态码 | Data 负载 | 典型示例 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **查询详情 / 列表 / 分页** | `GET` | `ApiResponseEntity.ok(msg, data)` | `ApiResponse<T>` | 200 OK | ✅ 有实体/VO | `return ApiResponseEntity.ok("获取详情成功", userVO);` |
| **创建新资源实体** | `POST` | `ApiResponseEntity.created(msg, data)` | `ApiResponse<T>` | 201 Created | ✅ 实体或主键 ID | `return ApiResponseEntity.created("创建题目成功", detailVO);` |
| **非创建实体的带返回操作** | `POST` / `PUT` | `ApiResponseEntity.ok(msg, data)` | `ApiResponse<T>` | 200 OK | ✅ 结果载荷 | `return ApiResponseEntity.ok("登录成功", authResponseVO);` |
| **删除资源** | `DELETE` | `ApiResponseEntity.success(msg)` | `ApiResponse<Void>` | 200 OK | ❌ 固定 `null` | `return ApiResponseEntity.success("删除用户成功");` |
| **状态切换 / 纯更新动作** | `PATCH` / `PUT` | `ApiResponseEntity.success(msg)` | `ApiResponse<Void>` | 200 OK | ❌ 固定 `null` | `return ApiResponseEntity.success("状态切换成功");` |
| **命令式动作 (发邮件/重置/登出)** | `POST` | `ApiResponseEntity.success(msg)` | `ApiResponse<Void>` | 200 OK | ❌ 固定 `null` | `return ApiResponseEntity.success("验证码已发送至邮箱");` |

---

## 5. 编译验证

执行 `mvn test-compile`，整个平台的 9 个模块全部编译通过（`BUILD SUCCESS`）。
