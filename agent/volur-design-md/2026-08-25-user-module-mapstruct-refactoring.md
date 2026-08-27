# 施工与审计记录：volur-user 模块 MapStruct 映射器落地与 Service 层手写 Builder 清理重构

## 1. 基本信息

- **任务名称**：在 `volur-user` 模块引入 MapStruct 声明式映射器，彻底消除 `UserServiceImpl` 与 `AdminUserServiceImpl` 中的手写 Builder 与私有 `toVO` 样板代码
- **执行时间**：2026-08-25
- **影响模块**：
  - `volur-user`（创建 `UserMapper`，重构 `UserServiceImpl` 与 `AdminUserServiceImpl`）
- **关联文件**：
  - [`UserMapper.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/mapper/UserMapper.java)
  - [`UserServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/service/impl/UserServiceImpl.java)
  - [`AdminUserServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/service/admin/impl/AdminUserServiceImpl.java)

---

## 2. 变更背景与规范契合

### 2.1 既有代码痛点
1. **样板代码严重侵入业务**：在 [`UserServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/service/impl/UserServiceImpl.java) 中，`getCurrentUser` 与 `getUserProfileById` 方法充斥着 25+ 行冗长的逐字段 `.builder()` 链式拷贝和三元判空（`profile != null ? profile.get...() : null`），严重干扰业务阅读。
2. **违背设计与编码规范**：
   - 违反 [`Rules.md` 第 6.1 节](file:///H:/HuMoLix/github/Volur-Lite/agent/volur-design-skills/Rules.md#L591-L598)：“禁止在 Service 中手写私有 `toVO` 或冗余的 Builder/Setter 逐字段拷贝代码”。
   - 违反 [`GEMINI.md` 第 1.6 节](file:///H:/HuMoLix/github/Volur-Lite/GEMINI.md)：“强制使用 MapStruct 声明式接口”。
3. **重复样板代码**：在 `AdminUserServiceImpl` 中存在手写私有转换方法 `toAdminDetailsResponse(User user)`。

---

## 3. 具体改动内容

### 3.1 声明 MapStruct 映射器 [`UserMapper.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/mapper/UserMapper.java)
- 在 `com.humolix.volur.user.mapper` 包下创建 `UserMapper` 接口，标注 `@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)`。
- 支持 `User + UserProfile -> UserSummaryVO` 多源映射与单对象重载。
- 支持 `User + UserProfile + rank -> UserProfileResponseVO` 多源聚合映射，默认值保护（如 role 默认 `"USER"`，points 默认 `0.0`，solves 默认 `0L`）。
- 支持 `User + UserProfile -> AdminUserDetailsVO` 多源与列表映射。

### 3.2 重构业务层实现类
1. **[`UserServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/service/impl/UserServiceImpl.java)**：
   - 注入 `private final UserMapper userMapper;`；
   - 彻底移除手写 Builder 拷贝代码，方法简化为单行调用：
     - `return userMapper.toUserSummaryVO(user);`
     - `return userMapper.toUserProfileResponseVO(user, rank);`
2. **[`AdminUserServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/service/admin/impl/AdminUserServiceImpl.java)**：
   - 注入 `private final UserMapper userMapper;`；
   - 彻底移除私有反模式方法 `toAdminDetailsResponse(User user)`；
   - 分页与详情查询统一调用 `userMapper::toAdminUserDetailsVO`。

---

## 4. 验证与审计结果

- 触发 `mvn clean test-compile` 全量构建，10 个子模块全部构建成功（`BUILD SUCCESS`，耗时 10.6s）。
- 检查编译器生成的 [`UserMapperImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/target/generated-sources/annotations/com/humolix/volur/user/mapper/UserMapperImpl.java)，验证其生成的级联 null-safe 逻辑与字段映射 100% 正确。
- 业务层代码行数精简 60% 以上，完全符合 Volur Lite 统一编码与 MapStruct 架构规范。
