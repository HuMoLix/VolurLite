# Volur Lite 移除动态默认角色配置并强制固化 USER 角色归档

- **执行日期**：2026-08-26
- **施工模块**：`volur-auth`, `volur-user`
- **文档类型**：安全基线加固与角色分配逻辑收敛

---

## 1. 变更背景与安全分析

在既有系统中，用户注册时尝试通过 `systemSettingService.getSettingValue("DEFAULT_USER_ROLE", "USER")` 动态读取默认角色配置。

### 存在的设计缺陷与安全隐患：
1. **违背最小权限原则 (Least Privilege)**：在 CTF 与网络攻防竞赛平台中，前台开放注册的新用户必须且只能是普通选手/用户（`USER` 角色）。允许通过系统设置项修改默认注册角色，一旦被误配或篡改为 `ADMIN` / `TEACHER`，会导致全站新注册用户直接获得高权提权，产生严重安全漏洞；
2. **冗余的动态配置**：系统内置基础角色体系由 `SUPERADMIN` > `ADMIN` > `TEACHER` > `USER` 四级层级构成，默认普通角色具有绝对确定性，无需引入动态配置读取与缓存查询开销。

---

## 2. 详细改动清单

### 2.1 认证模块 (`volur-auth`)
- **`AuthServiceImpl.register()`**：
  - 彻底移除对 `systemSettingService.getSettingValue("DEFAULT_USER_ROLE", "USER")` 的动态读取；
  - 强制固化直接通过 `roleRepository.findByName("USER")` 获取基础角色并完成实体绑定；
  - 若系统基础角色 `USER` 缺失，则抛出明确的内部服务异常阻断注册。

---

### 2.2 用户管理模块 (`volur-user`)
- **`AdminUserServiceImpl.createUser()`**：
  - 收敛角色分配判定逻辑：管理员若未显式指定 `roleName`，默认统一精准绑定基础角色 `USER`；
  - 若指定了不存在的自定义角色，抛出 HTTP 400 明确提示，杜绝因 `orElse(null)` 导致产生脏数据实体。

---

## 3. 验证结果

执行全平台 Maven 构建：
```bash
mvn clean compile -DskipTests
```
全平台 12 个子模块全部构建成功（`BUILD SUCCESS`）。
