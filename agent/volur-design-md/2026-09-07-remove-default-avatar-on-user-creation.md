# 施工记录：取消创建用户时设置硬编码默认头像并加固资源删除防护

## 1. 基本信息

- **模块名称**：`volur-auth`、`volur-user`
- **变更目标**：
  - 移除新用户注册、管理员新增用户及超级管理员初始化时写入伪默认头像地址（`/api/v1/uploads/avatar/default_avatar.jpg`）的逻辑，使未上传头像用户统一保持 `avatar_url = null`；
  - 增强 `UserServiceImpl.uploadAndSetAvatar` 中对旧头像物理文件清理时的防御机制，规避针对系统预置资源的误删风险。
- **执行时间**：2026-09-07
- **规范依据**：[`Rules.md`](file:///H:/HuMoLix/github/Volur-Lite/agent/volur-design-skills/Rules.md) 及 [`GEMINI.md`](file:///H:/HuMoLix/github/Volur-Lite/GEMINI.md)

---

## 2. 变更背景与风险分析

在原实现中，用户注册（`AuthServiceImpl`）、管理员新增用户（`AdminUserServiceImpl`）及超管初始化（`SuperAdminInitializer`）均会在创建 `UserProfile` 时硬编码设置 `.avatarUrl("/api/v1/uploads/avatar/default_avatar.jpg")`。经审查，该做法存在以下问题与安全隐患：

1. **物理资源缺失与 HTTP 404 错误**：
   本地静态资源目录与 OSS 存储中并不存在 `default_avatar.jpg` 文件。硬编码写入该伪路径后，前端尝试拉取图片必然触发 `GET 404 Not Found`，造成网络控制台异常报错。
2. **破坏前端原生首字母兜底头像机制**：
   前端通用头像组件 `UserAvatar.vue` 针对 `displayUrl` 为空的状态已经内置了优雅的黑色高质感背景（`#000000`）+ 大写首字母文本兜底展示。若后端返回非空伪链接，将导致前端 `!displayUrl` 判定失效，页面呈现裂图或空白。
3. **共享默认资源误删安全隐患（File Deletion Hazard）**：
   在 `UserServiceImpl.uploadAndSetAvatar` 中，若用户更换新头像，后端会自动删除旧头像文件（`storageService.deleteFile(oldAvatarUrl)`）。若未来服务器部署了公共默认头像文件，某个用户首次更换头像时，物理删除逻辑会将全局共享的默认头像文件直接删除，导致全站其他未修改头像的用户头像集体失效。
4. **数据层语义失真与单一真实数据源违背**：
   `UserProfile.avatarUrl = null` 在领域模型中具有明确的语义（“用户尚未设置/上传头像”）。硬编码假数据污染了数据库，使得“判断用户是否已自定义头像”无法简单通过判空完成，后续若调整默认展示策略还需要全量清洗历史数据。

---

## 3. 具体改动内容

### 3.1 用户注册方法（`AuthServiceImpl.java`）

- **文件路径**：`src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/AuthServiceImpl.java`
- **改动点**：构建 `UserProfile` 时移除 `.avatarUrl("/api/v1/uploads/avatar/default_avatar.jpg")` 链式调用，保持 `avatarUrl` 为默认的 `null`。

```diff
@@ -173,7 +173,6 @@
         }
 
         UserProfile userProfile = UserProfile.builder()
-                .avatarUrl("/api/v1/uploads/avatar/default_avatar.jpg")
                 .user(savedUser)
                 .isHidden(false)
                 .build();
```

### 3.2 管理员新增用户（`AdminUserServiceImpl.java`）

- **文件路径**：`src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/service/admin/impl/AdminUserServiceImpl.java`
- **改动点**：管理员创建用户时，构建 `UserProfile` 同样移除硬编码的 `avatarUrl`。

```diff
@@ -155,7 +155,6 @@
 
         UserProfile profile = UserProfile.builder()
                 .user(savedUser)
-                .avatarUrl("/api/v1/uploads/avatar/default_avatar.jpg")
                 .isHidden(false)
                 .build();
         userProfileRepository.save(profile);
```

### 3.3 超管账号初始化（`SuperAdminInitializer.java`）

- **文件路径**：`src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/init/SuperAdminInitializer.java`
- **改动点**：系统首次启动自动初始化超级管理员档案时，移除硬编码的 `avatarUrl`。

```diff
@@ -60,7 +60,6 @@
 
             UserProfile profile = UserProfile.builder()
                     .user(savedUser)
-                    .avatarUrl("/api/v1/uploads/avatar/default_avatar.jpg")
                     .org("Volur Lite")
                     .bio("Volur Lite System Super Administrator")
                     .isHidden(false)
```

### 3.4 头像上传与清理加固（`UserServiceImpl.java`）

- **文件路径**：`src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/service/impl/UserServiceImpl.java`
- **改动点**：在 `uploadAndSetAvatar` 执行旧文件物理删除前，增加前置安全检查，跳过包含 `default_avatar` 的系统保留资源，防止历史脏数据或系统级预设文件被意外物理删除。

```diff
@@ -143,8 +143,8 @@
         StorageResponseVO storageResponse = storageService.uploadImage(file, "avatars");
         String newAvatarUrl = updateAvatar(userId, storageResponse.getFilePath());
 
-        // 头像更新后，清理旧头像物理文件以释放空间
-        if (StringUtils.hasText(oldAvatarUrl) && !oldAvatarUrl.equals(newAvatarUrl)) {
+        // 头像更新后，清理旧头像物理文件以释放空间（避免误删默认头像等系统预置资源）
+        if (StringUtils.hasText(oldAvatarUrl) && !oldAvatarUrl.equals(newAvatarUrl) && !oldAvatarUrl.contains("default_avatar")) {
             storageService.deleteFile(oldAvatarUrl);
         }
```

---

## 4. 影响范围与验证结果

1. **编译构建**：
   - 执行 `mvn clean compile -DskipTests` 对 12 个子模块进行全量构建，构建结果为 `BUILD SUCCESS`，无语法或类型冲突。
2. **前后端交互**：
   - 接口返回 `avatarUrl: null`，前端 `UserAvatar.vue` 及各个视图自动回退至高质量的首字母大写纯色头像，消除了 404 资源加载异常。
3. **数据库兼容性**：
   - `user_profile.avatar_url` 本身为可空字段，无需调整 DDL。
   - 历史存量数据若存在该假路径，可通过 `UPDATE user_profile SET avatar_url = NULL WHERE avatar_url = '/api/v1/uploads/avatar/default_avatar.jpg';` 简单清洗。
