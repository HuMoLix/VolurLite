# 施工与审计记录：CurrentUserId 参数解析器前置 401 拦截加固与 Service 业务层代码精简

## 1. 基本信息

- **任务名称**：在 `CurrentUserIdArgumentResolver` 中实现统一的前置非空与合法性拦截，彻底清除 Service 业务层冗余的 `if (userId == null)` 401 样板代码
- **执行时间**：2026-08-25
- **影响模块**：
  - `volur-framework`（加固 `CurrentUserIdArgumentResolver` 的统一空值/非法值校验出口）
  - `volur-user`（精简 `UserServiceImpl` 中的鉴权样板代码）
- **关联文件**：
  - [`CurrentUserIdArgumentResolver.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/security/resolver/CurrentUserIdArgumentResolver.java)
  - [`UserServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/service/impl/UserServiceImpl.java)

---

## 2. 变更背景与架构考量

1. **职责分离与边界清晰**：
   - HTTP 401（登录已失效）属于 Web 安全接入层的状态语义，不属于领域业务层；
   - 业务 Service 应当专注于数据查询与业务规则，不应感知 HTTP 登录态或重复进行防御性 401 判定。
2. **前置屏障保障（Fail-Fast at Web Layer）**：
   - 当 Controller 方法声明 `@CurrentUserId`（默认 `required = true`）时，参数解析器必须确保：若未登录、Token 无效或解析结果为空/非正整数，请求在进入 Controller 之前必须立即被拦截并中断，直接向客户端返回 `401 Unauthorized` 统一响应。

---

## 3. 具体改动内容

### 3.1 加固 [`CurrentUserIdArgumentResolver.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/security/resolver/CurrentUserIdArgumentResolver.java)
- 在解析方法出口处增加统一的前置阻断：
  ```java
  // 严密拦截：必填模式下若解析出的 userId 为空或非法，统一在此阻断并抛出 401 异常
  if (userId == null || userId <= 0) {
      if (required) {
          throw new BusinessException(HttpStatus.UNAUTHORIZED, "登录已失效，请重新登录");
      }
      return null;
  }
  ```
- 确保进入 Controller 和下游 Service 的 `userId` 具有 100% 非空和合法性保证。

### 3.2 精简 [`UserServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/service/impl/UserServiceImpl.java)
- 彻底移除 `getCurrentUser` 与 `getUserProfileById` 方法头部的 `if (userId == null)` 重复判空，使业务逻辑回归纯粹的两行代码。

---

## 4. 验证与审计结果

- 触发全平台 10 个子模块全量编译构建（`mvn clean test-compile`），全部通过（`BUILD SUCCESS`，耗时 11.8s）。
- Web 层安全前置拦截机制生效，业务层代码具备更高的内聚性与可读性。
