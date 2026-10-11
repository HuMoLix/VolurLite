# 密码安全挑战服务解耦与 MaskUtil 数据脱敏工具落地设计方案

## 1. 变更背景与架构审计

### 1.1 密码挑战核销服务的职责耦合问题
在 Volur Lite 安全核验体系中，认证挑战（Challenge）逐步走向细粒度独立控制器与微服务架构。此前：
- 通行密钥挑战（Passkey）拥有独立的 [`PasskeyController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/controller/v1/challenge/PasskeyController.java) 与 [`PasskeyService.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/service/PasskeyService.java)；
- 人机核验挑战（reCAPTCHA）拥有独立的 [`ReCAPTCHAChallengeController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/controller/v1/challenge/ReCAPTCHAChallengeController.java) 与 [`RecaptchaService.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/service/RecaptchaService.java)；
- 密码核销虽然拥有独立的控制器 [`PasswordController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/controller/v1/challenge/PasswordController.java)，但在底层却强耦合了巨石服务 [`ChallengeService.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/service/ChallengeService.java) 下的 `verifyPassword`；
- 导致 `ChallengeServiceImpl` 依赖膨胀，注入了仅用于密码校验的 `PasswordEncoder` 以及专属的 Redis 登录失败与锁定键前缀常量（`VREDIS_AUTH_LOGIN_ATTEMPTS_KEY_PREFIX`、`VREDIS_AUTH_LOGIN_LOCK_KEY_PREFIX`），违背了单一职责原则（SRP）与接口隔离原则（ISP）。

### 1.2 敏感数据脱敏逻辑碎片化
在 [`ChallengeServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/service/impl/ChallengeServiceImpl.java) 中，备选核验方式下发（`getChallengeSelection`）时需要对用户的安全邮箱与手机号进行脱敏展示，此前通过私有方法 `maskEmail` 与 `maskPhone` 内联实现，缺乏通用性与可复用性。安全模块需要一套统一、规范且高性能的数据脱敏工具集。

---

## 2. 改造方案设计与实施

### 2.1 密码挑战核销服务独立化解耦
1. **定义独立领域服务接口 [`PasswordService.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/service/PasswordService.java)**：
   - 位于 `com.humolix.volur.security.service` 包下；
   - 暴露接口方法：`FlowResponseVO verifyPassword(PasswordChallengeRequestDTO request);`。
2. **实现 [`PasswordServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/service/impl/PasswordServiceImpl.java)**：
   - 位于 `com.humolix.volur.security.service.impl` 包下，实现 `PasswordService`；
   - 完整承接密码校验全生命周期逻辑：
     - 设备基线校验（`SessionDeviceBaselineValidator`）与事务流转锁断言（`TransitionLockManager` 验证 `pwd` 步骤）；
     - 端到端加密密码解密（支持 `$V1$GCM$` 密文流解密）；
     - 登录失败频次与锁定检测（基于 Redis 计数器与防爆破锁定）；
     - 账号查找与密码哈希比对（`PasswordEncoder.matches`）；
     - 账号状态正常性与维护模式判断（非管理员在全站维护时快速失败）；
     - 受信设备检测与新设备自动跃迁为 `etotp` 邮箱二次挑战；
     - 受信设备记录、步骤标记完成及下一阶段 `authenticate` 事务锁下发。
3. **改造 [`PasswordController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/controller/v1/challenge/PasswordController.java)**：
   - 将原注入的 `ChallengeService` 彻底替换为 `private final PasswordService passwordService`；
   - 方法体内调用 `passwordService.verifyPassword(request)`。
4. **清理 [`ChallengeService.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/service/ChallengeService.java) 与 [`ChallengeServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/service/impl/ChallengeServiceImpl.java)**：
   - 严格遵循《GEMINI.md》第 3 条“重构彻底性铁律”，杜绝冗余的兼容别名或废弃桥接层；
   - 从 `ChallengeService` 接口与 `ChallengeServiceImpl` 中彻底移除 `verifyPassword`；
   - 清除 `ChallengeServiceImpl` 中不再使用的 `PasswordEncoder` 依赖、相关 imports 以及登录失败计数常量。

### 2.2 数据脱敏工具类 [`MaskUtil.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/util/MaskUtil.java)
在 `com.humolix.volur.security.util` 下新建 `MaskUtil` 工具类，提供以下通用脱敏算法，并同时支持小驼峰与大驼峰双调用形式：
1. **身份证脱敏 (`identifierCardMask` / `IdentifierCardMask`)**：
   - 18 位身份证保留前 6 后 4，中间以 8 个 `*` 遮蔽（如 `110101********1234`）；
   - 15 位身份证保留前 6 后 3，中间以 6 个 `*` 遮蔽；
   - 大于等于 8 位保留前 4 后 4，中间以 4 个 `*` 遮蔽；
   - 短于 8 位返回默认脱敏掩码 `***`；支持 `null` 与空白字符串安全防空。
2. **手机号脱敏 (`phoneMask` / `PhoneMask`)**：
   - 11 位大陆手机号保留前 3 后 4，中间以 4 个 `*` 遮蔽（如 `138****5678`）；
   - 其它大于 7 位号码保留前 3 后 4，中间以 4 个 `*` 遮蔽（如 `138****6789`）；
   - 短于等于 7 位支持安全缩略或返回 `***`。
3. **电子邮箱脱敏 (`emailMask` / `EmailMask`)**：
   - 用户名前缀大于 2 字符保留首尾，中间以 `***` 遮蔽（如 `zhangsan@example.com` -> `z***n@example.com`）；
   - 用户名前缀 1 或 2 字符保留首字符，尾接 `***`（如 `a@example.com` -> `a***@example.com`）；
   - 非法格式或无 `@` 符号返回 `***`。
4. **真实姓名脱敏 (`nameMask` / `NameMask`)**：
   - 单字姓名：遮蔽为 `*`；
   - 双字姓名：保留姓氏，隐藏名（如 `张三` -> `张*`）；
   - 三字姓名：保留首尾，隐藏中间字（如 `李四五` -> `李*五`）；
   - 四字及以上姓名：保留首尾，中间所有字符均以 `*` 遮蔽（如 `诸葛孔明` -> `诸**明`）。
5. **银行卡号脱敏 (`bankCardMask` / `BankCardMask`)**：
   - 去除空格后保留前 6 后 4，中间所有位以 `*` 遮蔽。
6. **API Key / 凭据脱敏 (`keyMask` / `KeyMask`)**：
   - 保留前 4 后 4，中间以 `*` 遮蔽；过短密钥全量遮蔽。
7. **通用自定义脱敏 (`genericMask` / `GenericMask`)**：
   - 灵活指定保留前缀长度、后缀长度以及自定义掩码字符。

在 [`ChallengeServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/service/impl/ChallengeServiceImpl.java) 中将原有的内联 `maskEmail` 和 `maskPhone` 移除，全面接入 `MaskUtil.emailMask` 与 `MaskUtil.phoneMask`。

---

## 3. 测试与验证矩阵

1. **`PasswordServiceImplTest`**：
   - `verifyPassword_success_trustedDevice_shouldAdvanceToAuthenticate`：验证受信设备登录成功且直通 `authenticate` 流程；
   - `verifyPassword_untrustedDevice_shouldTriggerEtotp`：验证新设备登录触发邮件验证码下发并切至 `etotp` 挑战；
   - `verifyPassword_missingIdentifier_shouldThrowBadRequest`：验证缺失账号标识时的快速失败；
   - `verifyPassword_whenPasswordWrong_shouldRecordFailureAndThrowUnauthorized`：验证密码错误时记录风控事件及 Redis 失败频次；
   - `verifyPassword_whenUserDisabled_shouldThrowForbidden`：验证封禁账号拒绝登录；
   - `verifyPassword_whenMaintenanceModeAndNonAdmin_shouldThrowMaintenanceModeException`：验证维护模式下普通用户阻断。
2. **`PasswordControllerTest`**：
   - 验证控制器入参绑定与委派调用 `passwordService.verifyPassword`，并断言返回响应实体状态码、信息和 VO 结构。
3. **`MaskUtilTest`**：
   - 全面覆盖身份证（18/15/8/短位/空值）、手机号（11/12/5/3位/空值）、邮箱（正常/单字/双字/非法/空值）、姓名（1/2/3/4/6字/空值）、银行卡、Key 与通用脱敏算法，共 7 组测试全数通过。
4. **平台全模块编译验证**：
   - `volur-security` 模块 130 个单元测试全部执行成功，通过率 100%；
   - 全平台 13 个子模块 `mvn test-compile` 反应堆无错误。

---

## 4. 架构合规性说明

- **零注释原则**：所有新增及修改的 Java 源码文件严格杜绝行内解释性注释与冗余 Javadoc，代码通过清晰的包结构、方法签名及强类型定义实现高度自解释；
- **接口契约分离**：严格遵循 Spring Boot 薄控制器与接口隔离准则，控制器仅面向 `PasswordService` 接口编程；
- **构造器注入**：一律采用 Lombok `@RequiredArgsConstructor` 与 `private final` 不可变属性声明。
