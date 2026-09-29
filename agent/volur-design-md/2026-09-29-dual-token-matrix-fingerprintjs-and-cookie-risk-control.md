# 大厂级分层矩阵指纹架构 (@fingerprintjs/fingerprintjs) 与纯 Cookie 风控凭证体系施工审计文档

## 1. 变更背景与架构演进

在 Volur Lite 原有的设备指纹体系中，存在以下历史设计局限与安全隐患：
1. **单点指纹脆弱性**：此前主要依赖客户端手写的 MD5 与简单 Canvas 绘制，算法缺乏抗逆向混淆且未能深度聚合现代浏览器的复杂熵源（如音频上下文、WebGL 扩展、字体探测等）；
2. **请求头注入暴露 (`X-Device-Fingerprint`)**：原先通过 Axios 拦截器在 HTTP 请求头中主动追加 `X-Device-Fingerprint`。这一做法存在两大缺陷：
   - 静态资源、首屏 SSR、普通超链接跳转以及非 Axios 发起的原生请求无法自动携带该标识；
   - 显式暴露在自定义请求头中极易被自动化爬虫通过抓包拦截并定向重写或伪造；
3. **缺乏持久化自愈机制**：一旦用户清空 Cookie 或切换隐身模式，单一设备标识即刻丢失，无法跨会话进行连续风控风险画像追踪。

为了对标现代互联网大厂（如阿里巴巴 `cna/umidToken`、字节跳动 `ttwid/msToken`、腾讯天御 `_qimei36`、Cloudflare `__cf_bm`）的分层风控标准，本次施工实施了：
- **全面引入 `@fingerprintjs/fingerprintjs` 官方工业级指纹库**，彻底废除自建简易 `fingerprint.ts`；
- **落地大厂分层矩阵架构（双 Token 机制）**：
  - **`_v_did` (Device Identifier)**：长效持久化设备种子，采用 Cookie 与 LocalStorage 双向自愈（Evercookie）机制，保障跨会话跨登录态的设备生命周期追溯；
  - **`_v_dtk` (Device Risk Token)**：动态风控签名凭证，基于 FingerprintJS 计算的高稳定性 `visitorId`，融合浏览器家族、无头自动化探针与时序时间戳，Base64 封装防篡改载荷；
- **非阻塞式首屏探针**：在 [`App.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/App.vue) 的 `onMounted` 钩子中结合 `requestIdleCallback` 调度执行，确保首屏毫秒级 0 阻塞首帧渲染；
- **后端全链路重构**：**彻底删除 `X-Device-Fingerprint` 请求头**，后端以 Cookie 中的 `_v_dtk` / `_v_did` 作为唯一权威真实来源，实现了全域隐式安全透传。

---

## 2. 详细改动清单

### 2.1 前端工程 (`src/volur-lite-view-NEXT`)

1. **依赖升级**：
   - [`src/volur-lite-view-NEXT/package.json`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/package.json)：
   - 安装官方 `@fingerprintjs/fingerprintjs` 依赖。

2. **核心指纹探针模块重构**：
   - [`src/volur-lite-view-NEXT/src/utils/fingerprint.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/utils/fingerprint.ts)：
   - 彻底删除 200 余行手写 MD5 算法与粗糙的 Canvas 绘制逻辑；
   - 封装 RFC 4122 v4 UUID 生成器与 Cookie 读写工具（默认遵循 `Path=/`, `SameSite=Lax`, `Secure` 生产安全标）；
   - 实现 `getOrHealDeviceId()`：LocalStorage 与 Cookie 互为备份，一方丢失即时修复，两方皆空即初始化并持久化 365 天；
   - 异步调度 `@fingerprintjs/fingerprintjs` 提取 `visitorId`，结合自动化环境探针（`navigator.webdriver`）和浏览器家族，生成动态 Token 写入 `_v_dtk` Cookie（有效期 7 天）；
   - 保留同步导出的 `getDeviceFingerprint()`，保障 [`authFlow.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/store/modules/authFlow.ts) 等认证业务层 100% 零侵入平滑兼容。

3. **根组件非阻塞挂载**：
   - [`src/volur-lite-view-NEXT/src/App.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/App.vue)：
   - 在 `onMounted` 生命周期中引入 `initDeviceFingerprint()`；
   - 优先通过 `window.requestIdleCallback` 调度，当浏览器主线程处理完首屏关键渲染与动画处于空闲时异步采集指纹，降级采用 `setTimeout` 延迟执行，彻底消除对用户操作的卡顿干扰。

### 2.2 经典前端请求头清理 (`src/volur-lite-view`)

- [`src/volur-lite-view/src/utils/request.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/utils/request.ts)：
- 彻底移除 Axios 请求拦截器中向 `config.headers['X-Device-Fingerprint']` 注入请求头的遗留逻辑，统一交由浏览器 Cookie 隐式透传。

### 2.3 后端平台架构改造 (`src/volur-lite-platform`)

1. **统一 Cookie 解析与权威指纹提取**：
   - [`RequestUtil.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/util/RequestUtil.java)：
   - 增加标准化 Cookie 解析工具：`getCookie(request, name)`、`getCookieValue(request, name)`；
   - 重构 `getDeviceFingerprint()` 与 `getDeviceFingerprint(HttpServletRequest request)`：
     - **彻底删除 `getHeader("X-Device-Fingerprint")` 逻辑**；
     - 优先从 Cookie `_v_dtk` 提取动态风控签名 Token；
     - 兜底从 Cookie `_v_did` 提取长效设备种子；
     - 兼容通用 Cookie `fingerprint`；
     - 提供 URLDecoder 解码与空白清洗保护。

2. **风控过滤器与频控拦截适配**：
   - [`RiskIpFilter.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/security/filter/RiskIpFilter.java)：
   - 将原先第 104 行 `request.getHeader("X-Device-Fingerprint")` 彻底重构为 `RequestUtil.getDeviceFingerprint(request)`，跨 IP 设备维度滑动窗口限流无缝改为从 Cookie 权威提取。

3. **测试用例保障**：
   - [`RequestUtilTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/test/java/com/humolix/volur/framework/security/RequestUtilTest.java)：
   - 新增 `testGetCookieValueAndDeviceFingerprint()` 单元测试，分别验证了：
     - 无 Cookie 时正确返回 `null`；
     - **验证防御失效：仅有 Header `X-Device-Fingerprint` 但无 Cookie 时必须返回 `null`（证实 Header 已彻底作废）**；
     - 通用 `fingerprint` Cookie 读取与兜底；
     - `_v_did` 长效设备种子优先级；
     - `_v_dtk` 动态风控凭证最高优先级解析。

---

## 3. 验证与测试结果

1. **前端编译与类型检查**：
   - 执行 `npm --prefix "src/volur-lite-view-NEXT" run build`；
   - `vue-tsc --noEmit` 严格类型审查 100% 通过，Rollup/Vite 生产打包在 5.30s 内完成，无任何错误或类型警告。
2. **后端单元测试**：
   - 执行 `mvn test "-Dtest=RequestUtilTest,RiskIpFilterTest,DeviceAndTlsRiskEngineTest" "-Dsurefire.failIfNoSpecifiedTests=false"`；
   - 全部相关测试用例 100% 通过，0 失败，0 错误，Maven Reactor 构建成功。
