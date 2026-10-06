# 会话创建与恢复职责解耦与设备特征强约束落地规范

- **施工日期**：2026-10-05
- **施工模块**：`volur-security`、`volur-auth`
- **相关核心文件**：
  - [`TransitionLockManager.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/flow/TransitionLockManager.java)
  - [`SessionServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/service/impl/SessionServiceImpl.java)
  - [`RequestDeviceParams.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/domain/session/RequestDeviceParams.java)
  - [`SigninServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/SigninServiceImpl.java)
  - [`SignupServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/SignupServiceImpl.java)
  - [`TransitionLockManagerTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/test/java/com/humolix/volur/security/flow/TransitionLockManagerTest.java)
  - [`SessionServiceImplTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/test/java/com/humolix/volur/security/service/SessionServiceImplTest.java)
  - [`SigninServiceImplTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/test/java/com/humolix/volur/auth/service/impl/SigninServiceImplTest.java)
- **执行规范**：严格遵循 `GEMINI.md` 之单一职责、零注释铁律、重构彻底性铁律与全链路 Fast-Fail / Zero-Mock 原则。

---

## 1. 变更背景与设计动机

1. **会话创建与恢复职责混乱**：
   - 原 `TransitionLockManager.createOrGetSession` 同时承担“根据现有 `dsh` 检索恢复会话”和“在缺少会话或入参为空时自动创建新会话”的双重职责；
   - 这种隐式兜底机制掩盖了非法或已过期 `dsh` 的时序缺陷（若客户端传了过期 `dsh`，可能被误认为新建会话并默默赋予新句柄），违背了快速失败（Fast-Fail）原则。
2. **历史指纹降级兜底残留**：
   - 在前期会话初始化中，解密得到的 `RequestDeviceParams` 若未包含 `fingerprint` 或 `deviceHash`，通过 `if (!StringUtils.hasText(...))` 强制覆盖为外层传参 `fp`；
   - 随着前端特征采集模块与对称加密传输改造完成，客户端上报必须满足强类型设备完整性要求，不应在服务端以静默形式容忍残缺上报。
3. **缺少 Bean Validation 实体校验**：
   - 反序列化得到的 `RequestDeviceParams` 仅依靠 Jackson 反序列化，若 JSON 缺失核心风控字段（如 `fingerprint` 与 `deviceHash`），Jackson 不会主动报错，存在脏数据注入隐患。

---

## 2. 核心改动清单

### 2.1 会话管理层解耦 (`TransitionLockManager.java`)
- 彻底废除 `createOrGetSession` 混合方法；
- 拆分为两个职责绝对单一且明确的语义方法：
  1. **`createSession`**：专用于首建会话。调用雪花算法生成全新 `dsh`，初始化会话元数据并持久化至 Redis；
  2. **`recoverySession`**：专用于现有会话的对齐恢复与状态同步。入参 `dsh` 必须非空且必须在 Redis 中真实有效；若 `dsh` 为空或已失效/过期，直接抛出 `BusinessException(HttpStatus.UNAUTHORIZED, ...)` 阻断，杜绝隐式新建；若会话存在则原子更新变更的属性。

### 2.2 服务层强校验与流程分流 (`SessionServiceImpl.java`)
- 注入 Spring 标准 `jakarta.validation.Validator validator;`；
- 解密得到 `RequestDeviceParams` 后，立即执行 `validator.validate(deviceParams)`，若违背约束直接抛出 `HttpStatus.BAD_REQUEST` 快速失败；
- 彻底删除历史上的二次兜底判空逻辑（`deviceParams.setFingerprint(fp)` 与 `deviceParams.setDeviceHash(fp)`）；
- 根据客户端是否传递 `dsh` 明确分流：
  - `StringUtils.hasText(dsh)` -> 调用 `transitionLockManager.recoverySession(...)`；
  - 否则 -> 调用 `transitionLockManager.createSession(...)`。

### 2.3 领域模型声明式约束 (`RequestDeviceParams.java`)
- 在核心设备标识字段上添加 Jakarta 强约束注解：
  - `@NotBlank(message = "设备指纹不能为空") private String fingerprint;`
  - `@NotBlank(message = "设备哈希不能为空") private String deviceHash;`

### 2.4 上层消费模块对齐 (`volur-auth`)
- **`SigninServiceImpl.java`**：在提交用户标识阶段，此时会话必然由 `initSess` 预先建立，因此明确调用 `transitionLockManager.recoverySession(...)`；
- **`SignupServiceImpl.java`**：在注册流程中，会话同样由 `initSess` 建立，改为调用 `transitionLockManager.recoverySession(...)`，且传入 `deviceParams = null` 以严防覆盖掉 `initSess` 已采集绑定的完整设备特征。

---

## 3. 测试与验证结果

- **`volur-security`**：
  - 执行 `mvn test -pl :volur-security`，38 项单元测试 100% 通过；
  - 覆盖了 `createSession` 正常建联、`recoverySession` 属性更新、解密缺失指纹触发 Bean Validation 校验失败等场景。
- **`volur-auth`**：
  - 执行 `mvn test -pl :volur-auth -am`，54 项单元测试 100% 通过；
  - 成功验证认证状态机在 `recoverySession` 架构下的全链路流转。
