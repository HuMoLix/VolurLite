# 施工与审计记录：volur-user 模块 Rules.md 规范化全面对齐与架构优化

## 1. 基本信息

- **任务名称**：全面对齐 `volur-user` 模块与 `Rules.md` 规范（包结构收敛、RoleVO 封装、探测方法基本类型重构、控制器注解排序规范化）
- **执行时间**：2026-08-25
- **影响模块**：
  - `volur-user`（领域模型与枚举统一收敛至 `domain` 根包，封装 `RoleVO`，重构 Repository 探测方法与控制器）
  - `volur-auth`（同步更新 `User` / `UserProfile` / `Role` 实体包引用）
  - `volur-challenge`（同步更新 `User` 实体包引用）
  - `volur-scoreboard`（同步更新 `User` / `UserProfile` 实体包引用）
- **关联文件**：
  - [`RoleVO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/domain/vo/RoleVO.java)
  - [`User.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/domain/entity/User.java)
  - [`Role.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/domain/entity/Role.java)
  - [`UserProfile.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/domain/entity/UserProfile.java)
  - [`Gender.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/domain/enums/Gender.java)
  - [`PrivacyType.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/domain/enums/PrivacyType.java)
  - [`UserMapper.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/mapper/UserMapper.java)
  - [`UserRepository.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/repository/UserRepository.java)
  - [`RoleRepository.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/repository/RoleRepository.java)
  - [`AdminUserService.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/service/admin/AdminUserService.java)
  - [`AdminUserServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/service/admin/impl/AdminUserServiceImpl.java)
  - [`AdminUserController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/controller/v1/admin/AdminUserController.java)

---

## 2. 变更背景与规范契合

根据对 `volur-user` 模块的全量审查，主要存在以下待优化项：
1. **实体与枚举包未统一收敛**：实体类平铺在 `user.entity`，违反 `Rules.md` 第 2.1 节关于“模块所有实体统一收敛在 `domain.entity` 与 `domain.enums` 根包下”的约定。
2. **存在性探测方法包装类 NPE 隐患**：`UserRepository` 与 `RoleRepository` 中的 `existsBy*` 方法返回了 `Boolean` 包装类型，违反 `Rules.md` 第 4.1 节关于“探测方法强制返回基本类型 `boolean`”的约束。
3. **控制器直接外露持久化实体**：`AdminUserController.listRoles` 直接返回持久化实体 `Role`，缺乏视图隔离。
4. **控制层类注解排版顺序**：`AdminUserController` 的类级注解顺序未遵循优先级顺序。

---

## 3. 具体改造内容

### 3.1 领域模型与实体包收敛（Rules.md 2.1）
- 将 `User.java`、`Role.java`、`UserProfile.java` 统一平移至 `com.humolix.volur.user.domain.entity`；
- 将 `Gender.java`、`PrivacyType.java` 统一平移至 `com.humolix.volur.user.domain.enums`；
- 彻底清理遗留的 `user.entity` 包，并同步更新跨模块引用（`volur-auth`、`volur-challenge`、`volur-scoreboard`）。

### 3.2 视图层封装与 MapStruct 映射（Rules.md 2.3 & 6.1）
- 新增 [`RoleVO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/domain/vo/RoleVO.java)；
- 在 [`UserMapper.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/mapper/UserMapper.java) 中添加 `toRoleVO` 与 `toRoleVOList` 映射；
- `AdminUserController.listRoles` 返回 `ApiResponse<List<RoleVO>>`，杜绝持久化实体外泄。

### 3.3 数据访问层方法签名优化（Rules.md 4.1）
- 重构 [`UserRepository.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/repository/UserRepository.java) 与 [`RoleRepository.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/repository/RoleRepository.java) 中的 `existsBy*` 方法，返回值统一从 `Boolean` 改为基本数据类型 `boolean`，彻底消除自动拆箱引发的潜在 NPE。

### 3.4 控制器与服务层排版与形参规范化（Rules.md 3.1 & 7.1）
- 调整 [`AdminUserController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/controller/v1/admin/AdminUserController.java) 类注解顺序为：`@RestController` ➔ `@RequestMapping("/api/v1/admin/users")` ➔ `@RequiredArgsConstructor`；
- 状态切换参数统一规范为 `boolean status` / `boolean isHidden`。

---

## 4. 验证与审计结果

- 执行 `mvn clean test-compile` 全量构建，全部 10 个子模块编译通过（`BUILD SUCCESS`，耗时 8.8s）。
- `volur-user` 模块达到 **100% Rules.md / GEMINI.md 架构与编码规范合规**。
