# 施工与修改记录：TransitionLockGenerator 安全加固与功能升级方案

**日期**：2026-10-05  
**作者**：Antigravity Agent  
**模块**：`volur-security`、`volur-auth`

---

## 1. 现状剖析与安全痛点

原 `TransitionLockGenerator`（位于 `com.humolix.volur.security.flow.generator.TransitionLockGenerator`）负责生成认证状态跃迁票据（Transition Lock, `tl`），存在以下关键隐患与代码异味：

1. **静默吞异常与假令牌降级（Zero-Mock 原则违背）**：
   - 原代码包含 `catch (Exception e) { return "ADG-" + RandomUtil.randomString(...); }`。
   - 当底层 HMAC 加密因配置异常或算法不可用失败时，静默吞掉异常并伪造假随机令牌，掩盖了底层密码学故障，直接违背了系统的快速失败（Fail-Fast & Zero-Mock）铁律。
2. **弱伪随机数生成器（Weak PRNG Risk）**：
   - 原代码直接使用 Hutool 的 `RandomUtil.randomString()`，底层基于 `java.util.Random`（线性同余算法 LCG），不具备密码学安全抗预测性。
3. **高并发重复初始化 SecretKey（Object Allocation Overhead）**：
   - 每次调用 `generate()` 均重复执行 `new SecretKeySpec(...)`，在高并发认证请求下造成不必要的堆内存垃圾回收压力。
4. **校验能力缺失与散落坏味道**：
   - 原实现缺乏标准强类型格式校验工具方法，业务层（`TransitionLockManager`）仅能手写 `!StringUtils.hasText(tl) || !tl.startsWith("ADG-")` 进行脆弱的前缀判断，无法校验长度或非法字符。

---

## 2. 升级改造实施

1. **工业级密码学随机数（SecureRandom & Base64URL）**：
   - 弃用 LCG `RandomUtil`，全面改用单例 `SecureRandom` 提取高熵随机字节（16 字节 nonce 与 24 字节额外噪声熵），经 URL 安全的 Base64 编码，杜绝令牌被预测或重放。
2. **密钥规范化与并发复用**：
   - 预初始化不可变静态常量 `SECRET_KEY = new SecretKeySpec(HMAC_SECRET_BYTES, HMAC_ALGORITHM)`，并发复用消除对象重复分配。
3. **快速失败与严禁伪造（Fail-Fast）**：
   - 移除 `catch` 吞异常假随机伪装返回，若发生 `GeneralSecurityException`，立即转换为 `IllegalStateException` 快速中断并报错，符合 Fail-Fast 规范。
4. **完备领域校验能力（`isValid`）与防御深度强化**：
   - 增加 `public boolean isValid(String tl)`，基于正则 `^ADG-[A-Za-z0-9_-]{60}$` 严格校验长度（64 字符）、前缀（`ADG-`）及合规 Base64URL 字符集；
   - 在 [`TransitionLockManager.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/flow/TransitionLockManager.java) 的 `validateTransitionTicket` 和 `validateStepAndTl` 中，将原硬编码的前缀判断统一升级委托为 `transitionLockGenerator.isValid(tl)`，形成闭环防御。
5. **纯粹代码与零行内注释**：
   - 严格遵循代码库纯净代码规范，设计方案与推导统一收敛在外置 Markdown 文档中。

---

## 3. 测试与验证

1. **新增专用单元测试** [`TransitionLockGeneratorTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/test/java/com/humolix/volur/security/flow/generator/TransitionLockGeneratorTest.java)：
   - `generate_shouldProduceValidFormatAndEntropy`：验证 `ADG-` 前缀、精确 64 字符长度与 Base64URL 字符集合规；
   - `generate_consecutiveCallsShouldDifferDueToNonce`：验证相同入参连续调用由高熵 Nonce 保证结果离散互斥；
   - `generate_handlesNullParametersGracefully`：验证入参为 `null` 时的健壮性；
   - `isValid_shouldCorrectlyDetectValidAndInvalidTokens`：测试空值、前缀错误、长度不足/溢出、非法符号等边界；
   - `generate_concurrencyShouldProduceAllUniqueTokens`：16 线程 8000 请求并发测试，所有令牌 100% 互斥且格式合法。
2. **回归测试**：
   - 执行 `mvn test -pl "volur-security,volur-auth" "-Dtest=TransitionLockManagerTest,TransitionLockGeneratorTest,SigninServiceImplTest"`，全部通过（BUILD SUCCESS，0 Failures, 0 Errors）。
