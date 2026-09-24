# User 与 UserProfile 聚合根彻底解耦与登录并发防重提交缺陷修复

## 1. 变更背景

### 1.1 Fastjson2 循环引用与递归栈溢出崩溃 (`level too large : 2049`)
在远程测试环境调用登录接口 `/api/v1/auth/login` 时，服务端出现未捕获致命异常：
```log
com.alibaba.fastjson2.JSONException: level too large : 2049
    at com.alibaba.fastjson2.JSONWriter.overflowLevel(JSONWriter.java:2722)
    at com.alibaba.fastjson2.JSONWriterUTF16.startObject(JSONWriterUTF16.java:140)
    at com.alibaba.fastjson2.writer.OWG_5_12_UserProfile.write(Unknown Source)
    at com.alibaba.fastjson2.writer.OWG_4_14_User.write(Unknown Source)
    at com.alibaba.fastjson2.writer.OWG_5_12_UserProfile.write(Unknown Source)
    at com.alibaba.fastjson2.writer.OWG_4_14_User.write(Unknown Source)
```
**根本诱因剖析**：
1. `User` 实体与 `UserProfile` 实体原本通过 JPA 双向 `@OneToOne` 关联；在 `User` 属性上标记了 Jackson 的 `@JsonManagedReference`，在 `UserProfile` 属性上标记了 `@JsonBackReference`。
2. 平台最新引入的高性能多级缓存框架 `VolurCache` 基于 Fastjson2（`CachedValue.of(value)`）进行 L2 Redis 序列化。Fastjson2 在默认情况下不感知 Jackson 注解，且未开启引用探测（`ReferenceDetection`），在将 `UserDetails`（内含 `User`）写回缓存时触发互相死循环序列化，当层级超过 2048 时抛出溢出异常，导致登录接口阻断。
3. 从领域驱动设计（DDD）角度，`User`（认证鉴权核心、账户状态、安全凭据）与 `UserProfile`（用户展示性扩展信息、个人资料）在生命周期、查询频率与安全敏感度上存在本质差异。强行物理双向绑死不仅导致序列化陷阱，还在高并发场景下引入多余的级联拉取和脏数据失效风险。

### 1.2 前端登录回车与点击重复提交（并发双请求）
在前端登录界面（`AuthForm.vue` / `VolurCaptcha.vue`）输入验证码并敲击回车或快速点击登录按钮时，浏览器几乎在同一瞬间（间隔 < 5ms）向后端发出两条一模一样的 `POST /api/v1/auth/login` 请求：
```
11:48:49.201+08:00 [mcat-handler-71] POST /api/v1/auth/login
11:48:49.206+08:00 [mcat-handler-72] POST /api/v1/auth/login
```
**根本诱因剖析**：
1. **键盘事件冒泡回环**：在 `VolurCaptcha.vue` 中，验证码输入框 `<n-input @keyup.enter="$emit('enter')">` 向父组件发射了自定义 `enter` 事件，但未阻止原生 DOM 事件冒泡（缺少 `.stop` 修饰符）。原生 `keyup.enter` 沿 DOM 树继续冒泡至外层的 `<n-form @keyup.enter="handleAction">`。
2. **缺乏同步可重入状态锁**：在 `AuthForm.vue` 的 `handleAction` 中，未在函数入口做同步加锁，而是先执行异步的 `formRef.value?.validate(async (errors) => { ... })`，随后才设置 `loading.value = true`。由于两个事件在 5ms 内连续触发，两次调用在校验期间 `loading` 均为 `false`，导致双重并发请求穿透到后端。
3. 第一条请求校验并消费了图形验证码在 Redis 中的 Token，第二条请求几乎同时到达，引发并发竞争或验码失效。

---

## 2. 核心架构设计与改造方案 (DDD 聚合根彻底解耦)

### 2.1 方案选型与 DDD 聚合根解耦
放弃单纯给字段加 `@JSONField(serialize = false)` 的“治标”方案，采用彻底解耦的**方案二（DDD 独立聚合根）**：
- **`User` 聚合根**：聚焦账户凭据、角色（Role）、账户启停状态（Status）、风控指标，不感知任何 `UserProfile` 存在。
- **`UserProfile` 独立聚合根**：以 `user_id`（即 `@Id private Long id;`）作为主键，不再持有 `User` 实体引用，也不设置 `@MapsId`。
- **持久化层与数据库结构无缝兼容**：底层 MySQL 表结构、字段定义与外键保持完全不变，零 DDL 迁移成本与零业务中断。

---

## 3. 具体改动明细

### 3.1 后端工程改动 (Backend)

1. **`UserProfile.java` (`volur-user`)**：
   - 将主键定义重构为独立主键：
     ```java
     @Id
     @Column(name = "user_id")
     private Long id;
     ```
   - 彻底删除 `@OneToOne`、`@MapsId` 与 `private User user;`。

2. **`User.java` (`volur-user`)**：
   - 彻底移除 `private UserProfile userProfile;` 字段以及 Jackson 关联注解。

3. **`UserProfileRepository.java` (`volur-user`)**：
   - 新增方法 `List<Long> findUserIdsByOrgLike(String keyword)`，用于后台用户搜索通过组织架构名反查 user_id 集合。

4. **`UserRepository.java` (`volur-user`)**：
   - 清理所有已失效的 `LEFT JOIN FETCH u.userProfile` 查询（如 `findByIdWithProfileAndRole` 等）。

5. **`UserMapper.java` (`volur-user`)**：
   - 将单参数快捷映射方法降级默认传 `profile = null`，引导全量视图通过 `toUserProfileResponseVO(User user, UserProfile profile)` 显式双源装配。

6. **`UserServiceImpl.java` (`volur-user`)**：
   - 重构 `getCurrentUser`、`getUserProfileById`、`updateUserProfile`、`updateAvatar`、`uploadAndSetAvatar`：显式通过 `userProfileRepository.findById(userId)` 查询与保存，彻底打破双向耦合。

7. **`AdminUserServiceImpl.java` (`volur-user`)**：
   - 组织机构关键字检索采用 `findUserIdsByOrgLike` 反查 ID 集合配合 `IN (:orgUserIds)` 进行过滤；
   - 分页列表数据拉取后，通过 `userProfileRepository.findAllById(userIds)` 进行批量预热与内存 Map 组装，彻底杜绝 N+1 查询与跨表 Join 开销。

8. **`AuthServiceImpl.java` & `SuperAdminInitializer.java` (`volur-auth`)**：
   - 注册与初始化逻辑中，创建 Profile 时显式设置 `profile.setId(savedUser.getId())` 并独立调用 `userProfileRepository.save(profile)`。
   - 账户注销时独立清理 Profile：`userProfileRepository.findById(user.getId()).ifPresent(userProfileRepository::delete)`.

9. **`UserDetails.java` (`volur-auth`)**：
   - 添加 `@JsonCreator` 与 `@JSONCreator` 注解，保障 Fastjson2 与 Jackson 反序列化均能正常构造。

10. **`volur-competition` 模块对齐解耦**：
    - `CompetitionMemberRepository.java`：移除 JPQL 中的 `LEFT JOIN FETCH u.userProfile`。
    - `CompetitionMapper.java`：在映射 `CompetitionMemberVO` 时标记 `@Mapping(target = "avatarUrl", ignore = true)`。
    - `AdminTeamServiceImpl.java` & `CompetitionTeamServiceImpl.java`：注入 `UserProfileRepository`，通过提取队员 `userIds` 进行 `findAllById` 批量补全成员头像 `avatarUrl`。
    - `CompetitionChatServiceImpl.java` & `CompetitionChatHandshakeInterceptor.java`：通过 `userProfileRepository.findById(userId)` 独立安全获取用户头像。

11. **多级缓存框架防御加固 (`volur-framework`)**：
    - `CachedValue.java`：`CachedValue.of(value)` 增加 `JSONWriter.Feature.ReferenceDetection` 特性，序列化时主动探测循环引用并使用引用标识，杜绝 2049 栈溢出。
    - `VolurCache.java`：在 `put` 与 `loadAndPopulate` 中对 `CachedValue.of(value)` 添加完整的 `try-catch` 降级保护；即便发生不可预知的序列化错误，只记录日志并跳过写入缓存，绝不阻断正常业务流。

---

### 3.2 前端工程改动 (Frontend)

1. **`VolurCaptcha.vue` (`src/components/verification/VolurCaptcha.vue`)**：
   - 对字符验证码、邮箱动态码、短信验证码的 `<n-input>` 绑定增加 `.stop` 修饰符：
     ```vue
     @keyup.enter.stop="$emit('enter')"
     ```
   - 彻底截断键盘回车事件沿 DOM 树向上冒泡，杜绝父组件表单与输入框的双重触发。

2. **`AuthForm.vue` (`src/components/auth/AuthForm.vue`)**：
   - **立即同步加锁**：在 `handleAction` 与 `handleForgotPasswordAction` 入口处第一行立即执行：
     ```ts
     if (loading.value) return
     loading.value = true
     ```
   - **统一 Promise 异步校验**：改用 Naive UI 的 Promise 风格校验：
     ```ts
     const valid = await formRef.value?.validate().then(() => true).catch(() => false)
     if (!valid) {
       loading.value = false
       return
     }
     ```
     确保多处并发调用在第一毫秒就被拦截。
   - **操作按钮硬防护**：所有操作按钮显式添加 `attr-type="button"` 与 `:disabled="loading"`：
     ```vue
     <n-button
       type="primary"
       block
       size="large"
       attr-type="button"
       :disabled="loading"
       :loading="loading"
       @click="handleAction"
     >
     ```

---

## 4. 验证与审计

1. **后端完整性测试**：
   - 全平台 12 个模块重新编译：`mvn compile -f src/volur-lite-platform/pom.xml`，全部 `BUILD SUCCESS`。
   - 单元测试与切面测试执行：`mvn test -f src/volur-lite-platform/pom.xml`，43 个测试用例全部通过，零失败（`Failures: 0, Errors: 0`）。
2. **前端类型与打包构建验证**：
   - 前端执行 `npm run build`，TypeScript 类型检查与 Vite 打包通过（`built in 5.98s`），零报错。
3. **安全与稳定性收益**：
   - 彻底根除了 Fastjson2 循环引用与深度递归导致的系统瘫痪隐患；
   - 实现了聚合根之间的松耦合与清晰边界，消除了 JPA 双向关联暗含的级联与懒加载/序列化陷阱；
   - 解决了高并发登录场景下的重复提交与验证码竞争失效问题。
