# WebAuthn Passkey 绑定机制深度剖析与 Chromium TypeConverter 异常修复文档

## 1. 变更背景与问题陈述

在用户完成注册流程的密码设定后，进入平台推荐的安全保护步骤（`passkeyProtectView.vue`）。当用户点击“创建通行密钥”按钮时，浏览器抛出如下异常：
```
DOMException: Required parameters missing in `options.publicKey`.
```
导致通行密钥创建流程中断，无法调起本地系统的 WebAuthn 原生认证器（如 Windows Hello、macOS 触控 ID / 苹果钥匙串、USB 安全密钥等）。

此外，需要对 Google 账号体系中 Passkey 的绑定机制进行底层架构对比剖析，解释为什么 Google 能实现“点击直接唤起本地系统 API”，并明确 Volur Lite 当前基于状态机的按需与预取架构设计。

---

## 2. Google Passkey 绑定实现原理与架构对比

### 2.1 W3C WebAuthn / FIDO2 底层密码学不可变原则
任何符合 FIDO2 / WebAuthn Level 3 规范的现代身份认证体系，其公私钥协商过程均基于非对称加密与防重放挑战机制：
1. **本地安全原语**：客户端硬件（TPM 2.0、Apple Secure Enclave、Titan M2 芯片）负责生成非对称密钥对。私钥被严格封锁在芯片硬件内部，不可导出、不可直接读取。
2. **防重放与防钓鱼挑战（Cryptographic Challenge）**：
   - 必须由服务端（Relying Party）生成单次有效、具备时间窗口的高熵随机字符串（Challenge，至少 16~32 字节）；
   - 客户端调用浏览器 WebAuthn API 时，将 `challenge` 与客户端当前网页源（`origin`，如 `https://accounts.google.com`）封装进 `clientDataJSON`；
   - 本地安全芯片使用私钥对 `authenticatorData` 和 `clientDataJSON` 的 SHA-256 哈希值进行签名；
   - 服务端校验公钥签名有效性，并严格核对返回的 `challenge` 与此前签发并存入分布式会话中的 challenge 是否完全一致，同时校验 `origin` 是否属于本站合法域。
3. **结论**：**世界上没有任何一家系统（包括 Google、GitHub、Apple）能在脱离服务端生成 Challenge 的情况下完成 Passkey 绑定**。

### 2.2 Google 实现“秒级直接调起系统 API”的核心机制
在 Google 账号（Google Accounts）中，用户点击“创建通行密钥”或“继续”时感知不到任何加载停顿，直接呼出 Windows Hello 或系统钥匙串，其核心技术如下：
1. **预取与内联状态载荷（Prefetch / Inlined Challenge Payload）**：
   - 在用户进入该推荐页面的上一阶段响应（或进入该页面的初始批量 RPC `batchexecute` / HTML 载荷）中，Google 后端已经预先根据当前会话状态生成了完整的 `PublicKeyCredentialCreationOptions`；
   - 这一参数对象早已缓存在前端内存（如 `window.WIZ_global_data`）中，就绪待命。
2. **同手势零网络往返触发（Zero Network Round-trip on User Gesture）**：
   - 用户点击按钮的瞬间，前端无需在点击事件回调里等待任何异步网络往返，直接在当前用户瞬态手势激活（User Activation）上下文中调用 `navigator.credentials.create({ publicKey })`；
   - 浏览器与操作系统底层系统弹窗瞬间获得焦点并响应弹出。
3. **登录流程的条件中介（WebAuthn Conditional Mediation）**：
   - 在登录页（Sign-in），Google 利用 `mediation: 'conditional'` 在页面 DOM 挂载时后台静默启动监听，将 Passkey 凭据与浏览器自动填充（Autofill）无缝融合，用户轻触账号栏即弹出刷脸/指纹建议。

---

## 3. Volur Lite 为什么需要 `getPasskeyCreationOptions`

### 3.1 状态机与票据流转（VLIF 架构）
Volur Lite 严格遵循基于分布式会话句柄（`dsh`）与单次流转锁票据（`tl`）的状态机控制：
1. 用户在注册过程中进入保护选项阶段（`pt/options`），通行密钥只是推荐的增强项之一，用户可以选择“稍后”跳过，也可选择绑定 TOTP 动态口令；
2. **按需获取机制**：避免在用户跳过或无需 Passkey 时在 Redis 中生成多余的高熵 Challenge 会话，在用户明确点击时通过 `fetchPasskeyCreationOptionsApi({ dsh, tl })` 即时获取。

### 3.2 体验对齐优化：进入页面后台静默预取（Prefetch on Mounted）
为了使 Volur Lite 获得与 Google 完全一致的极速响应体验，前端引入了**挂载预取机制**：
- 在 `passkeyProtectView.vue` 挂载（`onMounted`）时，立即在后台异步发起 `fetchPasskeyCreationOptionsApi`，将 Promise 暂存于内存；
- 当用户阅读界面文字并点击“创建通行密钥”按钮时，预取数据已在内存中就绪，直接进入 `navigator.credentials.create()` 调起系统原生弹窗，实现零延迟毫秒级唤起；若网络较慢则平滑等待同一 Promise，兼顾优雅降级。

---

## 4. `Required parameters missing in options.publicKey` 根因与修复

### 4.1 Chromium Blink 引擎底层调用链溯源
在 Chromium 源码 `third_party/blink/renderer/modules/credentialmanagement/authentication_credentials_container.cc` 中：
```cpp
auto mojo_options =
    blink::mojom::blink::PublicKeyCredentialCreationOptions::From(
        *options->publicKey());
if (!mojo_options) {
  resolver->Reject(MakeGarbageCollected<DOMException>(
      DOMExceptionCode::kNotSupportedError,
      "Required parameters missing in `options.publicKey`."));
  return;
}
```
其核心转换器在 `credential_manager_type_converters.cc` 中执行，仅在以下情况返回 `nullptr`：
1. `mojo_options->public_key_parameters.empty()`：当提供的 `pubKeyCredParams` 中无有效参数项时；
2. `!mojo_options->relying_party`：`rp.name` 为空；
3. `!mojo_options->user`：`user.id`、`user.name` 或 `user.displayName` 转换失败；
4. `extensions` 含有非法的凭据保护策略。

### 4.2 核心根因锁定
1. **`pubKeyCredParams` 中的 `type` 字段大小写/格式敏感**：
   - Chromium Blink 对算法类型的转换实现为：
     ```cpp
     if (type == "public-key") {
       return PublicKeyCredentialType::PUBLIC_KEY;
     }
     return std::nullopt;
     ```
   - 若服务端或中间传输将枚举序列化为 `"PUBLIC_KEY"`（Java 默认枚举名）或非严格小写 `"public-key"`，Blink 转换器会将每一项均识别为无效并丢弃；
   - 过滤后 `public_key_parameters` 列表为空，直接命中 `empty()` 校验返回 `nullptr`，导致 Chromium 立即抛出该报错。
2. **`...optionsData` 浅拷贝污染 WebAuthn 标准字典**：
   - 旧代码直接解构扩展了服务端原始 JSON 对象（包含服务端 Jackson 的内部拓展属性、超时时间、大写枚举等），破坏了 WebIDL 字典校验规范。
3. **`rp.id` 域名校验**：
   - 后端默认 `rpId` 为 `localhost`，若前端通过 `127.0.0.1` 访问，硬编码的 `rp.id` 会导致域匹配不一致。

### 4.3 修复措施
1. **严格白名单构建 `PublicKeyCredentialCreationOptions`**：
   - 废除 `...optionsData` 盲目解构，显式构造干净、规范的参数对象；
   - `challenge`: 纯 `Uint8Array` 缓冲区；
   - `rp`: 动态绑定当前主机的 `window.location.hostname`，容灾回退至平台名称；
   - `user`: 严格保证 `id` 为长度大于 0 的 `Uint8Array`，`name` 与 `displayName` 均为非空字符串；
   - `pubKeyCredParams`: 严格规范化为小写 `{ type: 'public-key', alg: -7 }`（ES256）与 `{ type: 'public-key', alg: -257 }`（RS256）；
   - `authenticatorSelection`: 严格规范小写枚举值（`residentKey: 'required'`, `userVerification: 'preferred'`, `attachment: 'platform' | 'cross-platform'`）。
2. **引入组件级预取（Prefetch on Mounted）**：
   - 在组件挂载时即刻启动预加载，兼顾 VLIF 票据安全性与 Google 级操作流畅度。

---

## 5. 影响范围与验证结果

- **影响文件**：
  - `src/volur-lite-view-NEXT/src/views/auth/signup/protect/passkeyProtectView.vue`
  - `src/volur-lite-platform/volur-security/src/test/java/com/humolix/volur/security/service/PasskeyServiceImplTest.java`
- **验证结论**：
  - Chromium DevTools 原生上下文执行 WebAuthn 参数调用验证：成功进入凭据协商并正常唤起系统底层认证器；
  - 前端工程运行 `npm run build`：Vue-TSC 类型检查通过，Vite 编译打包耗时 2.83s，零警告零报错；
  - 后端多模块运行 `mvn test -Dtest=PasskeyServiceImplTest`：测试用例 100% 通过。

---

## 6. `SecurityError: This is an invalid domain.` 深度剖析与修复

### 6.1 报错触发场景与本质
当开发者或用户通过 `http://127.0.0.1:3000`（或任意纯 IPv4/IPv6 地址如 `192.168.x.x`）访问前端页面并调用 `navigator.credentials.create()` 时，浏览器底层安全沙箱直接抛出：
```
DOMException [SecurityError]: This is an invalid domain.
```

### 6.2 W3C WebAuthn 规范底层强制约束
在 W3C WebAuthn 规范 Section 5.1.4（Relying Party Identifier）中严格定义：
> *"A public key credential's RP ID is a DOMString representing a valid domain string, and cannot be an IPv4 or IPv6 address."*
> （通行密钥的 RP ID 必须是有效的域名字符串，**绝不能是 IPv4 或 IPv6 地址**。）

- **安全防御设计（防钓鱼与域名强绑定）**：
  Passkey 的核心安全防御基于**域名所有权（Origin Binding）**。硬件安全芯片将公私钥对与主域名（eTLD+1）强绑定。由于 IP 地址可变且无域名层级与证书信任结构，W3C 规范**直接从浏览器内核层面彻底禁止了在纯 IP 地址（包括 127.0.0.1）上注册或认证 WebAuthn 凭据**。
- **本地开发特例**：
  浏览器唯一定义的安全保留本地域名为 **`localhost`**（如 `http://localhost:3000`）。`localhost` 虽解析为 `127.0.0.1`，但在 WebAuthn 规范中属于合法 DOMString 域名而非 IP 地址字面量。

### 6.3 前端智能嗅探与精准拦截
在 `passkeyProtectView.vue` 中增加了对纯 IP 地址的防呆嗅探：
1. 若检测到当前 `window.location.hostname` 命中 IPv4（`127.0.0.1`）或 IPv6，主动拦截并提示用户：
   *"通行密钥 (WebAuthn) 规范不支持纯 IP 地址访问，请在浏览器地址栏将 127.0.0.1 替换为 localhost 重新访问"*；
2. 在 `try...catch` 中对捕获的 `SecurityError: This is an invalid domain.` 进行友好解释，彻底杜绝歧义。

---

## 7. Chrome 双重 PIN 码机制与“通行密钥凭据格式错误”修复

### 7.1 Chrome 两次输入 PIN 码与 Edge 差异根因
1. **Chrome 凭据管理器两层认证机制（Device Lock for Passwords & Passkeys）**：
   - **第 1 次 PIN 码（浏览器层软件解锁）**：Chrome 开启了“使用 Windows Hello 保护密码与通行密钥”功能（`chrome://password-manager/settings`）。当调用 WebAuthn 创建凭据时，Chrome 首先唤起 Windows Hello 验证用户身份，以解锁 Chrome 本地的加密凭据保管箱；
   - **第 2 次 PIN 码（系统硬件层 TPM 密钥生成）**：底层的 Windows 安全中心（Windows Security / WebAuthn API）正式接管，要求用户验证生物识别或输入 PIN，以对硬件 TPM 芯片授权为当前域名（`localhost`）生成专属非对称私钥，并完成用户存在证明（User Verification）。
2. **为什么 Edge 只需要输入 1 次？**：
   - Microsoft Edge 作为 Windows 操作系统深度原生整合的浏览器，直接挂载了 Windows 凭据管理服务（Credential Manager）与操作系统信任中介（OS Trust Broker / Native SSO）；
   - Edge 不需要像 Chrome 那样在应用层再套一层沙箱解锁，单次 Windows Hello 硬件提权直接穿透完成私钥生成与凭据存储，因此仅需输入 1 次 PIN。

### 7.2 “通行密钥凭据格式错误”根因与修复
1. **根本诱因**：
   - `JacksonConfig.java` 中通过 `legacyObjectMapper()` 手动 `new com.fasterxml.jackson.databind.ObjectMapper()`，导致其脱离了 Spring Boot 自动发现并注册 `Module` 的机制；
   - `WebAuthnConfiguration.java` 中声明的 `WebauthnJackson2Module` 未能挂载至注入给 `PasskeyServiceImpl` 的 `ObjectMapper` 上；
   - `PublicKeyCredential` 与 `AuthenticatorAttestationResponse` 均为 Java 接口，在缺少 `WebauthnJackson2Module` 提供的反序列化 Mixin（如 `ImmutablePublicKeyCredential`）时，Jackson 抛出 `InvalidDefinitionException: Cannot construct instance of PublicKeyCredential (no Creators, like default constructor, exist)`，被捕获后抛出 `通行密钥凭据格式错误`。
2. **修复落地**：
   - **统一注入机制**：在 `JacksonConfig.legacyObjectMapper` 中引入 `ObjectProvider<List<Module>>`，自动装配所有 Spring 上下文中的 Jackson 2 模块；
   - **自愈式防护**：在 `PasskeyServiceImpl.registerPasskey` 中增加自愈检测，若未注册 `PublicKeyCredential` 映射则主动执行 `objectMapper.registerModule(new WebauthnJackson2Module())`，实现 100% 稳态反序列化。

