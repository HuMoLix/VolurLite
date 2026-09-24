# 验证码系统重构：端点统一 POST /captcha、数据精简与魔改 RC4+RSA 场景加密审计文档

## 1. 变更背景与目标
原有验证码系统采用 `GET /api/v1/captcha/decision` 预检探测与 `GET /api/v1/captcha` 结合的设计，存在以下结构性缺陷与安全风险：
1. **内部风控泄露**：探测接口返回了 `riskLevel`（如 LOW、MEDIUM）、`mode`、`requireBehaviorVerify`、`requireOtp` 等内部判定变量，给攻击者逆向分析系统风控规则和绕过拦截提供了特征信息；
2. **场景明文参数伪造风险**：客户端以明文 `?scene=LOGIN` 发送请求，攻击者可任意篡改 scene 探测不同场景防线，且后端无法校验该请求是否真实来自于特定组件或路由；
3. **接口端点冗余**：`/decision` 与 `/captcha` 职责重叠。

本次重构统一接口为 `POST /api/v1/captcha`，精简响应载荷（仅返回 `captchaType`、`captchaId`、`imageBase64`），并引入**魔改 RC4 + RSA 混合加密**，将发起验证码请求的客户端组件源路由（如 `/auth/login`）加密后作为 `captchaSceneId` 传输，并在服务端基于对照表匹配基准验证码并执行自适应风控升级。

---

## 2. 核心改动点与实现细节

### 2.1 后端改动 (`volur-verification`)
1. **魔改 RC4 算法实现 ([`VolurModifiedRc4.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/crypto/VolurModifiedRc4.java))**：
   - S-box 引入 `(k ^ 0x5A) & 0xFF` 掩码初始化；
   - KSA 执行双轮交换并引入偏移量 `0x3C`；
   - PRGA 丢弃前 256 字节伪随机输出流以消除标准 RC4 早期弱密钥泄露统计偏置，输出流字节计算引入次级动态映射。
2. **混合解密与防重放服务 ([`CaptchaCryptoService.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/crypto/CaptchaCryptoService.java))**：
   - 拆分客户端提交的 `${rsaEncryptedSessionKey}.${rc4Ciphertext}`；
   - 使用 RSA 2048 私钥解出 16 字节 sessionKey；
   - 使用 `VolurModifiedRc4` 解密得到明文 JSON `{ path, ts }`；
   - 校验时间戳防重放（时间漂移限制在 120 秒内，超时拒绝请求）；
   - 返回规范化的小写路径字符串。
3. **路由基准策略映射管理器 ([`CaptchaRoutePolicyManager.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/risk/policy/CaptchaRoutePolicyManager.java))**：
   - 维护路由对照表：
     - `/auth/login`, `/login` $\rightarrow$ 场景 `LOGIN`，基准类型 `IMAGE` (LINE)；
     - `/auth/register`, `/register` $\rightarrow$ 场景 `REGISTER`，基准类型 `SLIDE_PUZZLE`；
     - `/auth/forgot`, `/auth/reset-password` $\rightarrow$ 场景 `RESET_PASSWORD`，基准类型 `SLIDE_PUZZLE`；
     - `/profile` $\rightarrow$ 场景 `CHANGE_PASSWORD`，基准类型 `SLIDE_PUZZLE`；
     - 默认回退 $\rightarrow$ 场景 `DEFAULT`，基准类型 `IMAGE`。
4. **请求与响应实体规范化**：
   - 新增 [`CaptchaGenerateRequestDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/domain/dto/CaptchaGenerateRequestDTO.java)：包含 `captchaSceneId` 与 `account`；
   - 优化 [`CaptchaResponseVO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/domain/vo/CaptchaResponseVO.java)：添加 `@JsonInclude(JsonInclude.Include.NON_NULL)`，仅返回 `captchaType`（`"IMAGE"`、`"SLIDER"`、`"EMAIL"`、`"SMS"`、`"NONE"`）、`captchaId` 以及仅在图片模式下存在的 `imageBase64`。
5. **控制器与 SDK 适配**：
   - [`CaptchaController.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/controller/v1/CaptchaController.java)：废除 `/decision`，新增 `@PostMapping` 映射 `/api/v1/captcha`；
   - [`CaptchaSDKImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/sdk/CaptchaSDKImpl.java)：实现 `generateCaptchaWithEncryptedScene`，将解密、路由策略匹配、风控引擎综合研判及图形挑战生成完全闭环。

### 2.2 前端改动 (`volur-lite-view`)
1. **原生前端加解密工具库 ([`src/utils/captcha-crypto.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/utils/captcha-crypto.ts))**：
   - 基于原生 ES2020+ `BigInt` 实现标准 RSA PKCS#1 v1.5 加密；
   - 原生 TypeScript 实现与服务端字节级完全一致的 `VolurModifiedRc4`；
   - 零第三方 npm 依赖，不受非 HTTPS 环境限制；
   - 导出 `encryptCaptchaScene(path)` 方法。
2. **API 与类型严格对齐**：
   - [`src/types/verification/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/types/verification/index.ts)：新增 `CaptchaGenerateRequestDTO`，精简 `CaptchaResponseVO`；
   - [`src/api/verification/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/api/verification/index.ts)：更新 `getCaptchaApi` 为 `POST /api/v1/captcha`。
3. **验证码组件重构 ([`VolurCaptcha.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/components/verification/VolurCaptcha.vue))**：
   - 挂载或刷新时获取当前页面路由（`route.path` 或 props `sceneId`），调用 `encryptCaptchaScene` 生成密文凭据；
   - 向 `POST /api/v1/captcha` 发起请求；
   - 状态重构为单一响应式变量 `captchaType`（`'IMAGE' | 'SLIDER' | 'EMAIL' | 'SMS' | 'NONE'`）；
   - 依据 `captchaType` 呈现对应的 UI（字符图片框、滑块触发盾牌按钮、邮箱动态安全码、短信动态安全码）；
   - 业务失败回调 `handleFailure` 依据服务端错误提示自适应跃迁为 `SLIDER` 或 `EMAIL`。

### 2.3 密钥配置收敛与动态分发
1. **`application.yml` 集中配置**：在 `volur.captcha.crypto` 节点下统一支持配置私钥、公钥与防重放时间窗口 `anti-replay-window-ms`，并支持环境变量注入；
2. **动态公钥接口 (`GET /api/v1/captcha/public-key`)**：服务端暴露公钥接口，前端 `captcha-crypto.ts` 实现了在途请求合并去重（In-flight Promise Deduplication）与公钥缓存，支持生产环境密钥无感轮换，并保留静态公钥作为离线兜底；
3. **RC4 会话密钥安全性**：严格坚持采用客户端动态生成的 16 字节随机密钥（一次一密，Ephemeral Session Key），杜绝在前端暴露任何静态对称私密密钥。

---

## 3. 安全隐患修复与审计说明
1. **防逆向风控**：服务端不再下发 `riskLevel` 等指标，攻击者无法通过探测接口自动化测试风控阈值；
2. **防重放攻击 (Anti-Replay)**：密文内含客户端实时生成的时间戳，服务端强制校验 120 秒有效窗口，超出窗口或无时间戳一律阻断；
3. **防篡改与混淆**：使用 RSA 加密每次随机会话密钥，魔改 RC4 加密路由路径，密文每次请求均发生随机改变，阻断了彩虹表和静态报文伪造。

---

## 4. 影响范围与验证结果
- **后端测试**：新增 `CaptchaCryptoServiceTest`，测试加解密一致性、路由场景解析、过期重放阻断、报文损坏防御；`mvn test -pl volur-verification` 全部 48 项单元测试 100% 通过。
- **前端构建**：执行 `npm run build`，TypeScript 类型检查与 Vite 打包通过，零语法与类型报错。
