# 施工与审计记录：请求上下文身份自动注入机制落地与 UserController 重构规范化

## 1. 基本信息

- **任务名称**：落地全平台请求上下文身份自动注入机制（`@CurrentUserId` / `@CurrentUser`），消除控制器层手动获取身份与反射反模式，重构 `UserController` 及相关控制器
- **执行时间**：2026-08-25
- **影响模块**：
  - `volur-framework`（下沉身份抽象契约、注解、方法参数解析器及 WebMvcConfigurer 注册）
  - `volur-auth`（`UserDetails` 实现统一身份契约 `UserIdentity`）
  - `volur-user`（重构 `UserController`，消除反射私有方法与 0L 漏洞）
  - `volur-challenge`（重构 `CtfChallengeController` 与 `AdminCtfChallengeController`）
  - `volur-container`（重构 `CtfInstanceController`）
  - `volur-scoreboard`（重构 `RankController`）
- **关联文件**：
  - [`UserIdentity.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/security/UserIdentity.java)
  - [`CurrentUserId.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/security/annotation/CurrentUserId.java)
  - [`CurrentUser.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/security/annotation/CurrentUser.java)
  - [`CurrentUserIdArgumentResolver.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/security/resolver/CurrentUserIdArgumentResolver.java)
  - [`CurrentUserArgumentResolver.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/security/resolver/CurrentUserArgumentResolver.java)
  - [`WebMvcConfig.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/config/WebMvcConfig.java)
  - [`UserDetails.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/security/entity/UserDetails.java)
  - [`UserController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/controller/v1/UserController.java)
  - [`CtfChallengeController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/controller/v1/CtfChallengeController.java)
  - [`AdminCtfChallengeController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/controller/v1/admin/AdminCtfChallengeController.java)
  - [`CtfInstanceController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/controller/v1/CtfInstanceController.java)
  - [`RankController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-scoreboard/src/main/java/com/humolix/volur/scoreboard/controller/v1/RankController.java)

---

## 2. 变更背景与核心痛点

### 2.1 既有代码反模式与安全缺陷
1. **违背 Thin Controller 原则**：在 [`UserController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/controller/v1/UserController.java) 中存在 23 行私有方法 `getCurrentUserId()`，侵入了底层 Spring Security 上下文解析、类型判定与异常处理逻辑。
2. **跨模块依赖导致的反射黑魔法**：由于 `volur-user` 是底层业务模块，未直接依赖 `volur-auth`（避免循环依赖），导致无法使用 `SecurityUtils`，开发者使用了 `getMethod("getUserId").invoke(principal)` 反射代码，破坏了编译期类型安全性。
3. **未登录兜底逻辑越权漏洞**：原 `getCurrentUserId()` 方法最后一行返回 `0L`，若认证解析失败且未抛异常，会携带 `userId = 0` 继续执行下游业务逻辑，存在越权与脏数据安全风险。
4. **冗余样板代码**：在 `CtfChallengeController`、`CtfInstanceController`、`RankController` 等多个控制器中，每个方法均需手动编写 `Long userId = SecurityUtils.getCurrentUserId();`。

---

## 3. 具体改动点与实现方案

### 3.1 基础框架层 (`volur-framework`) 抽象下沉
1. **统一契约接口**：新增 [`UserIdentity`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/security/UserIdentity.java) 接口，定义 `Long getUserId()` 与 `String getUsername()`。
2. **声明式注入注解**：
   - 新增 [`@CurrentUserId`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/security/annotation/CurrentUserId.java)，支持 `required` 属性（默认 `true`，设置为 `false` 支持游客/可选登录模式）。
   - 新增 [`@CurrentUser`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/security/annotation/CurrentUser.java)，支持直接注入当前用户认证主体对象。
3. **参数解析器实现**：
   - 实现 [`CurrentUserIdArgumentResolver`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/security/resolver/CurrentUserIdArgumentResolver.java)，支持 `Long` 与 `long` 参数类型，优先通过 `UserIdentity` 强类型提取，并兼容 Spring Security 命名解析；未登录且必填时统一抛出规范的 401 `BusinessException`。
   - 实现 [`CurrentUserArgumentResolver`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/security/resolver/CurrentUserArgumentResolver.java)，支持按目标类型动态匹配注入 Principal。
4. **Web MVC 自动注册**：新增 [`WebMvcConfig`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/config/WebMvcConfig.java)，将解析器自动注册至 Spring MVC 调度链中。

### 3.2 认证模块与控制器层标准化重构
1. **契约实现**：[`UserDetails`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/security/entity/UserDetails.java) 显式实现 `UserIdentity` 接口，实现 100% 编译期类型安全与零反射性能损耗。
2. **控制器极净化**：
   - 重构 [`UserController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/controller/v1/UserController.java)，彻底删除 23 行私有反射方法，直接在方法参数声明 `@CurrentUserId Long userId`，规范类注解排列顺序（`@RestController` ➔ `@RequestMapping` ➔ `@RequiredArgsConstructor`）。
   - 同步重构 [`CtfChallengeController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/controller/v1/CtfChallengeController.java)、[`AdminCtfChallengeController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/controller/v1/admin/AdminCtfChallengeController.java)、[`CtfInstanceController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/controller/v1/CtfInstanceController.java) 与 [`RankController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-scoreboard/src/main/java/com/humolix/volur/scoreboard/controller/v1/RankController.java)，全面采用 `@CurrentUserId` 参数注入。

---

## 4. 验证与审计结果

- 执行 `mvn clean test-compile` 对平台全部 10 个子模块进行全量构建验证，全量编译通过（`BUILD SUCCESS`，总构建耗时 7.1s）。
- 验证各项规则：
  - 严格满足 `Rules.md` 依赖注入、薄控制器（Thin Controller）、异常处理与注解排版要求；
  - 彻底解除了跨模块直接依赖与反射调用，消除潜在越权漏洞，统一全站身份注入标准。
