# Volur Lite 角色模型重构与 VolurSuperAdmin 动态密码初始化归档

- **执行日期**：2026-08-26
- **施工模块**：`volur-user`, `volur-auth`
- **文档类型**：系统架构精简与安全基线加固

---

## 1. 变更背景

根据 Volur-Lite 轻量化架构定位：
1. 删除既有动态数据库角色表（`Role` 实体、`RoleRepository` 及关联的外键依赖）；
2. 系统角色精简收敛为三种枚举角色：`SUPER_ADMIN`（超级管理员）、`ADMIN`（管理员）、`USER`（普通用户）；
3. 普通用户前台注册时自动且固定配置 `USER` 角色；
4. 系统中 `SUPER_ADMIN` 角色有且仅能存在一个专属单例账号，固定名称为 `VolurSuperAdmin`；
5. 每次系统启动时，自动生成强随机密码并覆写更新 `VolurSuperAdmin` 凭据，同时在启动日志中醒目输出，实现“零默认硬编码密码”与高等级启动安全防护。

---

## 2. 详细改造清单

### 2.1 角色枚举与实体精简 (`volur-user`)
- **创建角色枚举 [`UserRole.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/domain/enums/UserRole.java)**：
  - `SUPER_ADMIN` ("超级管理员")
  - `ADMIN` ("管理员")
  - `USER` ("普通用户")
- **重构实体 [`User.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/domain/entity/User.java)**：
  - 移除 `@ManyToOne private Role role;` 关联外键；
  - 替换为 `@Enumerated(EnumType.STRING) @Column(name = "role", nullable = false, length = 32) private UserRole role = UserRole.USER;`。
- **删除冗余类**：
  - 删除 `Role.java` 实体与 `RoleRepository.java` 接口。
- **更新映射器与仓储**：
  - 更新 [`UserMapper.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/mapper/UserMapper.java) 与 [`UserRepository.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/repository/UserRepository.java)，去除对关联角色表的联表查询。

---

### 2.2 用户管理与安全防护 (`AdminUserServiceImpl.java`)
- **密码加密统一**：由原 `SmUtil.sm3()` 统一接入 Spring 容器注入的标准 `PasswordEncoder`（PBKDF2-SM3 加盐哈希）；
- **超级管理员账号边界防护**：
  - 禁止管理员在后台创建 `SUPER_ADMIN` 角色账号；
  - 禁止将其他普通用户/管理员修改为 `SUPER_ADMIN`；
  - 禁止删除、禁用或修改 `VolurSuperAdmin` 的用户名与角色，批量禁用操作自动跳过 `VolurSuperAdmin`。

---

### 2.3 认证与 Spring Security 配置 (`volur-auth`)
- **注册流程 [`AuthServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/AuthServiceImpl.java)**：
  - 注册直接赋予 `UserRole.USER`，彻底移除 `RoleRepository` 依赖；
- **Spring Security 权限解析 [`UserDetails.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/security/entity/UserDetails.java)**：
  - 将 `user.getRole().name()` 转为 `ROLE_SUPER_ADMIN`, `ROLE_ADMIN`, `ROLE_USER` 授权标；
- **角色层级配置 [`SecurityConfig.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/config/SecurityConfig.java)**：
  - 配置 `SUPER_ADMIN.implies(ADMIN).implies(USER)` 层级体系。

---

### 2.4 超级管理员启动初始化器 ([`SuperAdminInitializer.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/init/SuperAdminInitializer.java))
- 实现 `ApplicationRunner`，在容器启动完成时（`@Order(HIGHEST_PRECEDENCE)`）自动触发；
- 生成包含大小写字母、数字与特殊字符的 20 位高强度随机密码；
- 初始化或更新 `VolurSuperAdmin` 账号（设置 `SUPER_ADMIN` 角色与 PBKDF2-SM3 密文）；
- 扫描排查并自动将任何非 `VolurSuperAdmin` 的冗余 `SUPER_ADMIN` 账号降级为 `ADMIN`，严格确保系统内超级管理员的唯一性；
- 在控制台打印高可见度的凭据 Banner：
  ```
  ========================================================================================
  >> [Volur Lite] SuperAdmin Credentials Initialized:
  >> Username : VolurSuperAdmin
  >> Password : <randomly-generated-password>
  >> Notice   : A new secure random password is dynamically generated on every system restart.
  ========================================================================================
  ```

---

## 3. 验证结果

执行全平台 Maven 构建：
```bash
mvn clean compile -DskipTests
```
全平台 12 个子模块全部构建成功（`BUILD SUCCESS`）。
