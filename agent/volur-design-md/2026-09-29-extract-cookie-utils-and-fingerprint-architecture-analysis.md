# 客户端 Cookie 工具解耦与指纹风控架构溯源审计文档

## 1. 变更背景与动因
在 `volur-lite-view-NEXT`（新一代前端）项目中，为了满足渐进式风控与设备安全矩阵需求，此前在 `src/utils/fingerprint.ts` 中直接内嵌了 `getCookie` 与 `setCookie` 基础函数。随着工程业务拓展，不仅设备风控探针需要操作 Cookie，后续国际化语言凭证、认证会话状态维持以及通用偏好等场景同样需要统一且安全的基础 Cookie 工具支持。

此外，针对设备指纹与风控探针体系中 hand-written（手写）函数（`detectBrowserFamily`、`generateUUID`、`getOrHealDeviceId`、`getDeviceFingerprint`）与第三方开源库 `@fingerprintjs/fingerprintjs` 的职责边界进行了体系化解剖与架构归因。

---

## 2. 改动点汇总

### 2.1 新建 `src/utils/cookie.ts` 通用工具模块
- **安全读取 (`getCookie`)**：采用严格的正规表达式匹配和安全转义，防止特殊字符注入或脏匹配；自动进行 `decodeURIComponent` 处理；支持在 SSR 或 Node.js 环境下优雅降级。
- **安全设置 (`setCookie`)**：
  - 默认 `SameSite=Lax` 策略；
  - 自动检测 `window.location.protocol === 'https:'`，在安全上下文中强制追加 `; Secure` 标记；
  - 提供 `CookieOptions` 统一配置重载（支持自定义 `days`、`path`、`sameSite`、`secure`）。
- **安全清理 (`removeCookie`)**：通过重置过期时间戳（`Thu, 01 Jan 1970 00:00:00 GMT`）实现规范的客户端 Cookie 清理。

### 2.2 重构与规范化 `src/utils/fingerprint.ts`
- **清理冗余导出与导入收敛**：移除 `export { getCookie, setCookie } from './cookie'` 这一容易引发混淆的二次导出，仅保留模块内部所需的 `import { getCookie, setCookie } from './cookie'`；如外部模块需要使用 Cookie 操作，统一强制直连 `@/utils/cookie`。
- **命名规范化重构 (Function Naming Normalization)**：
  - `detectBrowserFamily()` 重命名为语义更加纯粹、标准的 **`getBrowserFamily()`**（并提供 `export const detectBrowserFamily = getBrowserFamily` 别名兼容）；
  - `getOrHealDeviceId()` 重命名为简洁直观、符合 Caller 视角的 **`getDeviceId()`**（并保留 `export const getOrHealDeviceId = getDeviceId` 兼容），将 Evercookie 双向自愈互保机制作为函数内部自然实现，对外提供统一清晰的 `getDeviceId` 契约；
- 项目经 `npm run build` (`vue-tsc --noEmit && vite build`) 验证无任何编译与类型错误。

---

## 3. 设备指纹与风控架构深度审计（为什么开源 FingerprintJS 不能取代这些手写函数）

### 3.1 FingerprintJS 开源库的能力边界
- `@fingerprintjs/fingerprintjs`（开源版）的核心职责是**环境熵采集与数学哈希计算**（提取诸如 Canvas、WebGL、Audio、Screen Resolution、系统字体等 30+ 维度的底层特征，经 MurmurHash 计算出 `visitorId`）。
- **没有提供上层业务风控契约模型**：它不提供直接映射为后端业务白名单的浏览器家族枚举（其内部导出的 `isChromium`、`isWebKit` 等仅为非稳态内部布尔探测，官方声明标注不受语义化版本保护）。
- **开源版无法解决环境漂移与跨版本追踪**：静态指纹在用户升级浏览器、调整系统缩放、开启无痕/隐私模式（Safari 17+ / Firefox Canvas 动态加噪）、切换显卡时均会发生哈希突变。

### 3.2 各手写函数存在的技术与架构必然性
1. **`detectBrowserFamily()`**：
   - **后端风控契约对齐**：与平台服务端 `RiskEvaluationEngine.java` 中的 `ParsedClientProfile` 和白名单保护严格 1:1 对齐（`CHROME` / `EDGE` / `FIREFOX` / `SAFARI` / `OTHER`）；
   - **防伪造一致性交叉校验**：在后端与 Nginx 原生透传的 TLS 握手特征（`X-TLS-Protocol`、`X-TLS-Cipher`、`X-TLS-ALPN`）以及 HTTP `User-Agent` 进行三位一体交叉验真，快速识破使用 Python/Go 伪造 UA 的恶意攻击者。
2. **`generateUUID()` 与 `getOrHealDeviceId()`**：
   - **工业级 Evercookie 双向自愈机制**：大厂工业级设备风控严格区分“长效持久化设备种子 (`_v_did`)”与“环境指纹 (`visitorId`)”。
   - 通过 `LocalStorage` 与 `Cookie` 互相锚定自愈：单侧被清除时另一侧立刻复原回填；双侧全空时通过符合 RFC 4122 v4 规范的 `generateUUID()` 生成新种子，赋予设备长达 365 天的高稳定性追踪生命周期。
3. **`getDeviceFingerprint()`**：
   - **彻底解决首屏冷启动阻塞 (Non-blocking & Zero-latency)**：FingerprintJS 的加载与采集（`fp.get()`）是一个重度异步过程（涉及离屏 Canvas 渲染、Audio 信号合成等，耗时在 50~300ms）。
   - 若业务端（如 `authFlow.ts` 发送登录预检）在页面刚打开时强行 `await`，将导致关键链路产生瀑布流延迟；`getDeviceFingerprint()` 支持同步快速返回已缓存的指纹，或以已持久化的 `did` 紧凑哈希作为零阻塞降级凭证，保障用户体验丝滑。

---

## 4. 影响范围与规范固化
- **根规范固化 (`GEMINI.md`)**：
  - 新增第 3 条“重构彻底性铁律（严禁妥协保留与旧命名别名）”；
  - 新增第 4 条“纯粹代码与零注释铁律（文档外置原则）”，明确注释统一书写在 Markdown 方案中，代码文件必须极致纯粹。
- **代码重构与纯粹化执行**：
  - `src/volur-lite-view-NEXT/src/utils/cookie.ts` (新建，零注释纯粹代码)
  - `src/volur-lite-view-NEXT/src/utils/fingerprint.ts` (彻底清除所有兼容别名与行内注释，保持全链路纯净)
- **编译与构建状态**：通过（Vite 生产打包、`vue-tsc` 类型检查全绿）。
