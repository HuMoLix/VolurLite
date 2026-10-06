# 会话初始化设备参数加密传输与后端解析落地规范

本文档记录了在 Volur 系统的 `initSess` 会话初始化阶段，引入设备特征全量收集、前端流式对称加密打包（`flow-crypto`）传输与后端解密还原至 `RequestDeviceParams` 的重构设计与落地详情。

---

## 1. 变更背景与设计动机

在之前的会话初始化交互中：
- 前端仅在 URL 查询参数或请求体中明文传递了基础 `fp`（设备指纹）与 `t`（时间戳）；
- 后端 `RequestDeviceParams` 领域模型已设计包含 15 个维度的精细设备风控特征（如 `fingerprint`、`deviceHash`、`deviceToken`、`canvasHash`、`browserFamily`、`osFamily`、`renderer`、`vendor`、`isHeadless`、`screenResolution`、`colorDepth`、`timezone`、`language`、`platform` 等），但在 `initSess` 阶段无法全量获取，导致会话上下文中的设备指纹特征残缺；
- 若直接在 HTTP 请求参数中明文平铺数十个设备属性，容易被嗅探、篡改或撞库爬虫伪造，违背了 Volur 平台针对客户端环境的零信任安全设计准则。

为此，结合前端现有的 `flow-crypto`（基于动态盐与 AES-GCM 的会话级加密机制），将设备参数打包为 JSON 并加密为 `data` 参数随 `initSess` 提交，后端进行动态密钥推导、解密与反序列化，实现全维度设备参数的静默安全建联。

---

## 2. 核心改动点

### 2.1 后端 DTO 与服务层重构 (`volur-security`)
1. **`InitSessionRequestDTO` 扩展**：
   - 增加 `private String data;` 属性，用于接收前端加密后的设备参数密文；
   - 添加 `@Trimmed` 消除前后空格。
2. **`SessionServiceImpl` 解密与装配**：
   - 注入 `FlowCryptoService` 与 Jackson 3 `ObjectMapper`；
   - 在 `initSess` 接收到 `requestDTO.getData()` 时，使用会话标识（`flowName`、`flowEntry`、`dsh`）推导对称密钥并解密；
   - 将解密得到的 JSON 反序列化为 `RequestDeviceParams`；
   - 注入客户端真实 IP（`RequestUtil.getClientIp()`），并在指纹缺省时使用 `fp` 兜底填充；
   - 若未提供 `data` 或解密失败，平滑降级至 `RequestDeviceParams.of(clientIp, fp)`，确保全链路鲁棒性。
3. **`FlowCryptoServiceImpl` 传输容错修复**：
   - 针对 GET 请求中 Base64 编码的 `+` 可能被反转义为空格 `' '` 的问题，在解码前统一执行 `.replace(' ', '+')` 容错处理。

### 2.2 前端设备参数收集与加密提交流程 (`volur-lite-view-NEXT`)
1. **`fingerprint.ts` 特征采集器增强**：
   - 新增 `getCanvasHash()`：基于 Canvas 2D 上下文渲染固定文本与色块并生成哈希；
   - 新增 `getWebGLInfo()`：安全提取 WebGL 的 `UNMASKED_RENDERER_WEBGL` 与 `UNMASKED_VENDOR_WEBGL`；
   - 新增 `getOsFamily()`：解析 User-Agent 获取规范化操作系统大类（`WINDOWS`、`MACOS`、`LINUX`、`ANDROID`、`IOS`、`OTHER`）；
   - 导出 `collectDeviceParams()`：聚合 15 个维度的设备指纹属性字典。
2. **`flow-crypto.ts` 密钥推导适配**：
   - 将 `encryptFlowCredential` 方法的 `dsh` 参数调整为可选（`dsh?: string | null`）；
   - 在初始建联无 `dsh` 场景下，采用空字符串 `""` 参与动态盐派生，前后端严格对齐 `${flowName}:${flowEntry}::${timestamp}`。
3. **Store 流程接入 (`signinFlow.ts` & `signupFlow.ts`)**：
   - 在 `ensureFlowParams` 触发 `initSessionApi` 时，调用 `collectDeviceParams()` 采集设备参数；
   - 将采集结果序列化为 JSON 并通过 `encryptFlowCredential` 进行 AES-GCM 加密；
   - 携带 `data: encryptedData` 发起会话初始化请求。

---

## 3. 安全隐患修复与防御效果

1. **防设备指纹嗅探与伪造**：
   - 客户端设备硬件信息、分辨率与渲染器等隐私/风控特征通过 AES-GCM 密文在传输链路上隐蔽，外部中间人与恶意逆向者无法直接嗅探采集结构；
2. **AES-GCM 完整性校验与防篡改**：
   - 密文中包含 128 位认证标签（Tag），任何对 `data` 密文的篡改都将在后端解密时触发认证失败并快速识别；
3. **零注释铁律与代码整洁度**：
   - 所有新增与变更代码均遵循纯粹代码规范，不包含任何行内冗余注释，设计方案与推导逻辑全量归档于本文档。

---

## 4. 影响范围与验证结果

- **后端工程**：
  - `volur-security` 全量单元测试（31 tests）执行 100% 通过（`BUILD SUCCESS`）；
  - `SessionServiceImplTest` 成功验证未加密降级与全量密文解密绑定场景。
- **前端工程**：
  - `volur-lite-view-NEXT` 通过 `vue-tsc --noEmit && vite build` 强类型检查与生产编译（2859 模块无告警构建成功）。
