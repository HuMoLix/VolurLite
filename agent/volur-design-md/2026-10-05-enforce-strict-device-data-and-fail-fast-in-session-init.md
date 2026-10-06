# 会话初始化强制要求设备参数密文与快速失败重构

本文档记录了在 Volur 系统会话初始化（`initSess`）阶段，彻底移除对未传递 `data` 或解密失败场景的隐式降级逻辑，全面贯彻 Fast-Fail（快速失败）与 Zero-Mock（零隐式兜底）铁律的设计与实施详情。

---

## 1. 变更背景与反模式治理

在初版设备参数加密改造中，为了保证“即使前端加密失败或未提供设备密文，初始化接口也能正常返回”，`SessionServiceImpl` 中采用了如下兜底容错逻辑：
- 若未提供 `data`，则静默回退生成仅包含基础 IP 和 `fp` 的 `RequestDeviceParams`；
- 若解密 `data` 产生异常（如凭证篡改、密钥派生不匹配或 Base64 损坏），仅打印 `log.warn`，随后同样回退到基础指纹。

这种做法虽然在表面上维持了请求的顺利响应，但在生产与高安全风控场景下存在致命安全隐患：
1. **掩盖客户端/前端安全异常**：若前端加解密算法或时间戳时钟发生偏差，会话降级机制将掩盖该异常，导致安全系统静默失去了对恶意客户端全维度设备指纹识别的能力；
2. **违背 Fast-Fail 准则**：绕过了零信任架构中“参数不合规即阻断”的防御边界，使自动化工具或攻击者能够通过直接剥离 `data` 规避指纹风控校验；
3. **违背全链路 Zero-Mock 铁律**：没提供就是没提供，解密失败就是不合法，严禁在后端私自伪造兜底数据。

---

## 2. 核心改动点

### 2.1 后端 DTO 约束对齐 (`InitSessionRequestDTO`)
- 对 `data` 字段添加 `@NotBlank(message = "设备参数密文不能为空")` 强约束注解；
- 结合 `@Trimmed`，在 HTTP 控制器入口层（`@Valid`）阻断任何未传 `data`、传递空字符串或纯空白符的非法请求。

### 2.2 服务层强校验与解密异常透传 (`SessionServiceImpl`)
- **必填强校验**：显式判定 `if (!StringUtils.hasText(encryptedData))`，若为空则直接抛出 `BusinessException(HttpStatus.BAD_REQUEST, "设备参数密文不能为空")`；
- **异常直接暴露**：调用 `flowCryptoService.decrypt(...)` 时，由解密层抛出的 `BusinessException`（如 `凭证校验失败，数据被篡改或参数不匹配`）直接原样透传，彻底移除吞没异常的 `try-catch` 降级兜底；
- **反序列化强校验**：使用 Jackson 反序列化为 `RequestDeviceParams` 时，若 JSON 格式损坏或无法映射，捕获并抛出 `BusinessException(HttpStatus.BAD_REQUEST, "设备参数解密或解析失败: ...")`；
- **反序列化空对象阻断**：若解析结果为 `null`，直接抛出 `BusinessException(HttpStatus.BAD_REQUEST, "设备参数载荷不能为空")`；
- **彻底删除降级代码**：彻底移除 `RequestDeviceParams.of(clientIp, fp)` 兜底分支。

### 2.3 前端类型系统与流程对齐 (`volur-lite-view-NEXT`)
- **接口类型强约束**：在 `types/auth/flow.ts` 的 `SessionInitParams` 中将 `data?: string` 修改为必填字段 `data: string`；
- **移除前端吞错捕获**：在 `signinFlow.ts` 与 `signupFlow.ts` 中，移除原先包裹 `encryptFlowCredential` 的吞错 `try/catch` 逻辑，确保加解密链路任一环节出现异常时立即抛出中断，触发统一错误提示。

---

## 3. 安全隐患修复与防御提升

1. **杜绝脱壳绕过攻击**：
   - 攻击者无法通过删除 `data` 参数绕过设备指纹探针与动态密钥校验；
2. **强签名与完整性保障**：
   - 依赖 AES-GCM 128 位认证标签与动态盐派生机制，任何重放、参数篡改与伪造请求都将被服务端直接以 400 Bad Request 拒绝；
3. **零注释铁律与代码整洁度**：
   - 所有生产代码保持零行内解释性注释，自解释逻辑由测试用例与本规范文档提供支撑。

---

## 4. 影响范围与验证结果

- **后端工程**：
  - 更新 `SessionServiceImplTest`：增加未传密文抛出异常断言、密文损坏与校验失败断言，测试用例全部通过；
  - `volur-security` 全量 32 项单元测试 100% 通过（`BUILD SUCCESS`）。
- **前端工程**：
  - `volur-lite-view-NEXT` 通过 `vue-tsc --noEmit && vite build` 强类型检查与生产编译（2859 模块无告警构建成功）。
