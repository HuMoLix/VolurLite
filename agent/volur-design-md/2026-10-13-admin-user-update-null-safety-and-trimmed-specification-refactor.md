# 管理员更新用户信息空指针异常修复与 `@Trimmed` 规范对齐报告

## 1. 变更背景与问题定位

在后台管理员调用更新用户接口（`PUT /api/v1/admin/users/{id}`）时，系统抛出如下空指针异常：

```text
java.lang.NullPointerException: Cannot invoke "String.equals(Object)" because the return value of "com.humolix.volur.user.domain.entity.User.getEmail()" is null
	at com.humolix.volur.user.service.admin.impl.AdminUserServiceImpl.updateUser(AdminUserServiceImpl.java:197)
```

### 1.1 根本原因分析
定位至 [`AdminUserServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/service/admin/impl/AdminUserServiceImpl.java) 的 `updateUser` 方法第 197 行：
```java
if (StringUtils.hasText(request.getEmail()) && !user.getEmail().equals(request.getEmail())) {
    ...
}
```
1. 当目标用户在数据库中的 `email` 字段为 `null`（如早期初始化、通过非邮箱方式注册或尚未绑定邮箱的用户）时，`user.getEmail()` 返回 `null`；
2. 管理员在后台表单保存该用户时（表单携带了非空邮箱值），执行 `!user.getEmail().equals(...)` 直接在 `null` 引用上调用了实例方法，必现 `NullPointerException`；
3. 同方法内还存在与项目后端规范 [`Rules.md`](file:///H:/HuMoLix/github/Volur-Lite/agent/volur-design-skills/Rules.md) 冲突的反模式：入参 DTO 字段已统一声明 `@Trimmed`，但 Service 层仍重复使用 `StringUtils.hasText(...)` 判定以及手动 `.trim()`。

---

## 2. 架构设计与改动点

### 2.1 空安全与入参规范治理 ([`AdminUserServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/service/admin/impl/AdminUserServiceImpl.java))

1. **`updateUser` 邮箱更新空安全重构**：
   - 将原脆弱的 `!user.getEmail().equals(...)` 重构为以非空入参主导的判定：
     ```java
     if (request.getEmail() != null && !request.getEmail().isEmpty() && !request.getEmail().equals(user.getEmail()))
     ```
   - 彻底避免 `user.getEmail()` 为 `null` 时触发 NPE，当旧邮箱为 `null` 时安全进入更新逻辑并执行 `existsByEmail` 排重检测。
2. **`updateUser` 用户名更新空安全与规范治理**：
   - 调整为 `request.getUsername() != null && !request.getUsername().isEmpty() && !request.getUsername().equals(user.getUsername())`。
3. **消除 `@Trimmed` 字段的防御性冗余代码**：
   - 按照 `Rules.md` 第 4.1 节要求：对于仅标注 `@Trimmed` 未标注 `@NotBlank` 的可选入参字段，统一使用 `field != null && !field.isEmpty()` 替代 `StringUtils.hasText`；
   - 在 `updateUser` 与 `createUser` 中，移除针对 `roleName` 字段的重复 `.trim()` 调用（直接使用 `toUpperCase()` 处理标准化大写枚举）。

---

## 3. 规范遵循与测试验证

1. **零注释铁律 (Zero Comments Rule)**：
   - [`AdminUserServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/service/admin/impl/AdminUserServiceImpl.java) 及新建测试类中无任何解释性注释，全部逻辑依靠自解释命名。
2. **单元测试补齐**：
   - 新增 [`AdminUserServiceImplTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/test/java/com/humolix/volur/user/service/admin/impl/AdminUserServiceImplTest.java)；
   - 覆盖场景一：用户初始邮箱为 `null` 时更新邮箱，断言不抛出 NPE 并正确更新邮箱持久化；
   - 覆盖场景二：用户更新与原邮箱相同的邮箱时，断言不触发数据库排重逻辑。
3. **构建与测试验证**：
   - 执行 `mvn test -pl volur-user` 运行通过，所有 6 个单元测试用例全部绿灯通过。
