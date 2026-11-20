# 认证模块源文件命名大小写对齐与 Linux 容器编译修复

## 1. 变更背景与问题溯源

在 Linux 容器环境（Docker 构建多阶段环境）执行 `mvn package -DskipTests -T 1C` 时，`volur-auth` 模块编译失败并爆出 64 个关联错误。

分析其根本原因：
1. **Linux 文件系统大小写敏感**：Windows NTFS 默认为 Case-Insensitive，而 Linux（Docker 基础镜像）文件系统为 Case-Sensitive。
2. **Git 索引未记录文件名大小写变更**：在 Windows 上开发重构时，Git 默认配置 `core.ignorecase=true`，导致文件名由 `Signin`/`Signup` 修改为驼峰 `SignIn`/`SignUp` 后，Git 暂存区索引仍记录为全小写形式。
3. **Java 编译规范强制约束 (JLS §7.6)**：声明为 `public` 的顶层类/接口文件名必须与类名大小写严格匹配。由于 Linux 下拉取的源文件名为小写，`javac` 直接抛出：
   - `class SignInController is public, should be declared in a file named SignInController.java`
   - `class SignUpController is public, should be declared in a file named SignUpController.java`
   - `interface SignInService is public, should be declared in a file named SignInService.java`
   - `interface SignUpService is public, should be declared in a file named SignUpService.java`
   - `class SignInServiceImpl is public, should be declared in a file named SignInServiceImpl.java`
   - `class SignUpServiceImpl is public, should be declared in a file named SignUpServiceImpl.java`
4. **编译期级联雪崩**：核心公共类型在第一阶段符号解析失败，导致后续 Lombok 注解处理器（Annotation Processor）发生级联故障，未能对当前编译单元注入 AST 节点（`log`、`builder()`、`getter/setter`），派生出大批 `cannot find symbol` 虚假错误。

---

## 2. 改动清单

通过 `git mv -f` 强制修正 Git 索引中的文件路径大小写，使其与公共类名 1:1 精确对齐：

### 2.1 主源码层 (main)
- `src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/controller/v1/SigninController.java` -> `SignInController.java`
- `src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/controller/v1/SignupController.java` -> `SignUpController.java`
- `src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/SigninService.java` -> `SignInService.java`
- `src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/SignupService.java` -> `SignUpService.java`
- `src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/SigninServiceImpl.java` -> `SignInServiceImpl.java`
- `src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/SignupServiceImpl.java` -> `SignUpServiceImpl.java`

### 2.2 测试源码层 (test)
- `src/volur-lite-platform/volur-auth/src/test/java/com/humolix/volur/auth/controller/v1/SigninControllerTest.java` -> `SignInControllerTest.java`
- `src/volur-lite-platform/volur-auth/src/test/java/com/humolix/volur/auth/service/impl/SigninServiceImplTest.java` -> `SignInServiceImplTest.java`

---

## 3. 影响范围与验证

- **影响范围**：仅变更 Git 索引中的物理文件名大小写，不改变 Java 业务逻辑或 API 路由。
- **构建验证**：
  - 本地环境及 Linux Docker 容器环境均能正确通过 `javac` 严格文件名校验；
  - 彻底消除了由类型扫描失败引起的 Lombok 级联符号缺失。
