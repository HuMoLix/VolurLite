# 验证码魔改 RC4 加解密体系重构与规范化审计文档

## 1. 变更背景与目标
在此前的验证码重构中引入了针对自适应人机验证场景的魔改 RC4 + RSA 混合加密传输机制，但原实现类命名为 `VolurModifiedRc4`，存在以下与工程整体规范不一致的问题：
1. **组件设计风格割裂**：平台在 `volur-auth` 模块中的国密与加解密设计存在清晰规范——算法级静态工具以 `Sm3Util` 风格承载，业务编码器组件以 `Sm3PasswordEncoder`（`@Component` + `@RequiredArgsConstructor` + `VolurThreadManager` 计算线程池挂起卸载 + 原生 CompletableFuture 异步流）承载；而 `VolurModifiedRc4` 既未声明为 Spring Bean，也未提供统一的 `encode` / `decode` 接口；
2. **前后端接口对齐不够标准化**：前端 `captcha-crypto.ts` 中直接通过 `new VolurModifiedRc4(sessionKey)` 耦合了底层流状态机的创建，缺乏标准化的编码器单例与静态辅助方法。

基于仓库 Git 历史分析与开发者确认，本次对魔改 RC4 体系进行全面重构：
- 服务端以 `Sm3PasswordEncoder` 为典范，构建 Spring 组件 `@Component Rc4CaptchaEncoder`，支持 CPU 密集计算线程池卸载调度与 CompletableFuture 异步管道；
- 同时提供 `@Component Rc4Util` 静态工具类（参考 `Sm3Util`），形成双层架构；
- 前端同步重构为 `Rc4CaptchaEncoder` 与 `rc4CaptchaEncoder` 实例，提供完整的多态调用接口并保持向后兼容。

---

## 2. 核心改动点与实现细节

### 2.1 后端改动 (`volur-verification`)

1. **废除 `VolurModifiedRc4.java` 并新增 [`Rc4CaptchaEncoder.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/crypto/Rc4CaptchaEncoder.java)**：
   - 标注 `@Component` 与 `@RequiredArgsConstructor`，通过构造注入 `VolurThreadManager`，并提供无参构造函数以保障离线/单元测试降级；
   - 暴露标准编码器方法：
     - `encode(byte[] data, byte[] key)` / `decode(byte[] data, byte[] key)`
     - `encode(String text, byte[] key)` / `decodeToString(byte[] data, byte[] key)`
     - `process(byte[] data, byte[] key)`
     - `encodeAsync(byte[] data, byte[] key)` / `decodeAsync(byte[] data, byte[] key)` / `decodeToStringAsync(byte[] data, byte[] key)`
   - 严格在调度前执行入参防御性校验（空密钥、空数据处理），防止无效任务进入计算线程池；
   - 将内部魔改 RC4 状态机抽象为内嵌静态类 `Rc4CipherState`。
2. **新增静态算法工具类 [`Rc4Util.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/crypto/Rc4Util.java)**：
   - 参考 `Sm3Util.java` 设计，声明 `@Component`；
   - 提供 `process`、`encrypt`、`decrypt`、`encrypt(String)`、`decryptToString` 等纯函数式静态方法，委托至 `Rc4CaptchaEncoder.processBytes`。
3. **重构服务解密依赖 [`CaptchaCryptoService.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/crypto/CaptchaCryptoService.java)**：
   - 声明 `@RequiredArgsConstructor`，通过 `private final Rc4CaptchaEncoder rc4CaptchaEncoder` 注入编码器 Bean；
   - 在 `decryptScenePath` 中使用 `rc4CaptchaEncoder.decode(cipherBytes, sessionKey)` 替代原先手写实例化 `new VolurModifiedRc4(sessionKey).process(...)`。
4. **测试套件补全与适配**：
   - 更新 [`CaptchaCryptoServiceTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/test/java/com/humolix/volur/verification/CaptchaCryptoServiceTest.java) 注入并使用 `Rc4CaptchaEncoder`；
   - 新增 [`Rc4CaptchaEncoderTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/test/java/com/humolix/volur/verification/Rc4CaptchaEncoderTest.java)，覆盖基于 `VolurThreadManager` 的加解密对称性、异步管道 `CompletableFuture`、无容器环境同步回退、非法密钥入参防御与 `Rc4Util` 委托一致性。

### 2.2 前端改动 (`volur-lite-view`)

1. **更新前端加解密工具库 [`src/utils/captcha-crypto.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/utils/captcha-crypto.ts)**：
   - 新增并导出 `Rc4CaptchaEncoder` 类，提供 `encode`、`decode`、`process` 实例方法及静态方法；
   - 内部封装 `Rc4CipherState` 状态机；
   - 导出全局单例 `rc4CaptchaEncoder`；
   - 保留 `Rc4Util` 与 `VolurModifiedRc4` 兼容别名导出，确保平滑升级；
   - 在 `encryptCaptchaScene` 方法中统一调用 `rc4CaptchaEncoder.encode(plainBytes, sessionKey)`。

---

## 3. 涉及文件与变更清单

| 模块 | 文件路径 | 变更类型 | 说明 |
| :--- | :--- | :--- | :--- |
| `volur-verification` | [`Rc4CaptchaEncoder.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/crypto/Rc4CaptchaEncoder.java) | 新增 | 按照 `Sm3PasswordEncoder` 规范实现的验证码 RC4 编码器组件 |
| `volur-verification` | [`Rc4Util.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/crypto/Rc4Util.java) | 新增 | 按照 `Sm3Util` 规范实现的静态算法工具类 |
| `volur-verification` | `VolurModifiedRc4.java` | 删除 | 移除历史命名不规范的类文件 |
| `volur-verification` | [`CaptchaCryptoService.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/crypto/CaptchaCryptoService.java) | 重构 | 构造注入 `Rc4CaptchaEncoder`，调用 `decode` 解密载荷 |
| `volur-verification` | [`CaptchaCryptoServiceTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/test/java/com/humolix/volur/verification/CaptchaCryptoServiceTest.java) | 适配 | 适配注入 `Rc4CaptchaEncoder` |
| `volur-verification` | [`Rc4CaptchaEncoderTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/test/java/com/humolix/volur/verification/Rc4CaptchaEncoderTest.java) | 新增 | 单元测试，全面覆盖对称性、异步管道与防御性边界 |
| `volur-lite-view` | [`src/utils/captcha-crypto.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/utils/captcha-crypto.ts) | 重构 | 导出 `Rc4CaptchaEncoder`、`rc4CaptchaEncoder`、`Rc4Util` 并适配加密调用 |

---

## 4. 验证结果
1. **后端单测验证**：
   - 执行 `mvn test -pl volur-verification,volur-auth`：
   - `volur-verification` 全部 54 项单元测试 100% 通过（0 Failure, 0 Error）；
   - `volur-auth` 全部 32 项单元测试 100% 通过（0 Failure, 0 Error）。
2. **前端构建验证**：
   - 执行 `npx vue-tsc --noEmit && npm run build`：
   - TypeScript 类型推导检查 100% 通过，生产构建打包成功。
