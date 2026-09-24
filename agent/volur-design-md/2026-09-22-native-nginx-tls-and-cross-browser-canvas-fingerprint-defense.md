# 生产环境原生 Nginx TLS 握手特征与跨浏览器兼容 Canvas 设备指纹纵深防御施工审计文档

## 1. 变更背景与核心目标

在防范针对 Volur Lite 平台的恶意自动化发包脚本攻击时，既有风控体系主要依托 IP 频控（Nginx 与 `RiskIpFilter`）及验证码难度跃迁（`RiskEvaluationEngine`）。
然而在实战网络攻防对抗中，攻击者常采用以下手法绕过传统基于单 IP 的防御：
1. **分布式住宅代理池 (Residential Proxy Pool)**：攻击者在 1 秒内轮换数以千计的真实出口 IP，单 IP 失败计数极难累积触发阈值；
2. **伪造 User-Agent 字符串**：发包脚本（如 Python `requests`、`urllib3`、Go `net/http`、`curl`）在 HTTP Header 中伪装成 `Mozilla/5.0 ... Chrome/128`；
3. **无头自动化模拟器 (Puppeteer / Playwright / Selenium)**：通过无 GPU 渲染的云服务器或 Docker 容器无头运行 Chromium，自动化批量刷接口。

针对上述对抗盲区，本次施工实施了 **“协议层握手特征 (Native TLS) + 前端离屏微观渲染 (Canvas 2D) + 显卡硬件渲染器探针 (WebGL) + 跨 IP 设备滑动窗口限流”** 的立体协同防御，同时严格遵守两大铁律：
- **浏览器多样性兼容铁律**：严密支持并白名单适配 Safari (macOS/iOS WebKit)、Edge (Windows/macOS Blink)、Firefox (Gecko)、Chrome 用户正常访问业务，杜绝误杀误拦截；
- **生产环境零编译开箱铁律**：利用官方原生 Nginx 内建变量透传 TLS 协商特征，无需重新编译或加载第三方 C 扩展，并在非 HTTPS / 本地开发环境中具备优雅降级。

---

## 2. 详细改动点清单

### 2.1 原生纯 Nginx 零编译开箱 TLS 协商特征透传
- **修改文件**：[`src/volur-lite-view/nginx/conf/nginx.conf`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/nginx/conf/nginx.conf)
- **改动说明**：
  - 在所有 API 反向代理节点（`/api/v1/auth/login`、`/api/v1/auth/register`、`/api/v1/email/captcha`、`/api/v1/captcha` 以及全局 `/api/`）中注入 Nginx 原生 SSL 变量：
    ```nginx
    proxy_set_header X-TLS-Protocol $ssl_protocol;
    proxy_set_header X-TLS-Cipher $ssl_cipher;
    proxy_set_header X-TLS-Ciphers $ssl_ciphers;
    proxy_set_header X-TLS-Curves $ssl_curves;
    proxy_set_header X-TLS-ALPN $ssl_alpn_protocol;
    proxy_set_header X-TLS-Session-Reused $ssl_session_reused;
    ```
  - **优雅降级设计**：在本地开发环境或普通 HTTP 直连场景下，上述变量天然输出为空字符串，后端接收后自动跳过 TLS 强一致性核验，保证本地联调与普通开发零报错。

### 2.2 前端设备与环境指纹探针模块开发 (`volur-lite-view`)
- **新增文件**：
  1. [`src/volur-lite-view/src/utils/fingerprint/canvas.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/utils/fingerprint/canvas.ts)：
     - 创建 240x60 离屏 Canvas，利用线性与径向渐变、复杂多层混合（`globalCompositeOperation = 'multiply'`）、半透明重叠路径以及带有特殊 Unicode/Emoji（`Volur 🛡️ <canvas> πΩ~`）的多语言字体回退排版，测试 GPU 与底层字体光栅化（Core Text / DirectWrite / FreeType）的次像素抗锯齿微差；
     - 采用纯位运算实现的 32 位 MurmurHash3 算法生成稳定紧凑散列，单次渲染计算耗时仅 1-2ms。
  2. [`src/volur-lite-view/src/utils/fingerprint/webgl.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/utils/fingerprint/webgl.ts)：
     - 提取 `WEBGL_debug_renderer_info` 硬件渲染器与显卡厂商；
     - **主流浏览器合法白名单**：将 Safari 17+ / macOS Sonoma 隐私保护模式下的 `Apple GPU`、Firefox 的 `Mozilla`、Edge 的 `Microsoft Direct3D11` 与 Chrome 的真实 GPU 统一列为合法真机；
     - **云端无头软件光栅化识别**：精准匹配 `SwiftShader`、`llvmpipe`、`softpipe`、`Mesa OffScreen`、`VirtualBox`、`VMware` 等无头容器或黑客自动化环境，标记 `isHeadlessRenderer`。
  3. [`src/volur-lite-view/src/utils/fingerprint/browser.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/utils/fingerprint/browser.ts)：
     - 精准区分 `SAFARI`、`EDGE`、`FIREFOX`、`CHROME` 浏览器家族；
     - 嗅探 `navigator.webdriver` 以及 Puppeteer / Selenium 注入特征（`__puppeteer_evaluation_script__`、`__webdriver_evaluate` 等）。
  4. [`src/volur-lite-view/src/utils/fingerprint/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/utils/fingerprint/index.ts)：
     - 聚合计算并在内存中单例缓存 `DeviceFingerprint`（包含 16 位紧凑散列 `deviceHash` 与 Base64 凭证 `deviceToken`）；
     - 页面仅在首次启动时执行一次，后续请求 0 开销复用。

### 2.3 前端业务层与网络请求拦截深度集成
- **修改文件**：
  1. [`src/volur-lite-view/src/types/verification/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/types/verification/index.ts)：
     - 在 `CaptchaVerifyRequestDTO` 中扩充 `deviceFingerprint` 与 `browserFamily` 字段；
  2. [`src/volur-lite-view/src/types/risk/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/types/risk/index.ts)：
     - 在 `AdminIpDiagnosisVO` 中增加 `deviceFingerprint`、`browserFamily`、`tlsProtocol`、`tlsCipher`、`tlsAlpn` 诊断字段；
  3. [`src/volur-lite-view/src/utils/request.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/utils/request.ts)：
     - Axios 请求拦截器中自动为所有发往后端的 API 请求注入请求头 `X-Device-Fingerprint`；
  4. [`src/volur-lite-view/src/components/verification/VolurCaptcha.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/components/verification/VolurCaptcha.vue)：
     - 人机验证派发载荷中自动填充当前客户端设备指纹与浏览器家族；
  5. [`src/volur-lite-view/src/views/admin/risk/AdminRiskIpView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/risk/AdminRiskIpView.vue)：
     - IP 实时诊断探针弹窗中补充渲染客户端浏览器、设备环境指纹、TLS 协议与加密套件；
     - 并在 `zh_CN/admin/risk.json` 与 `en_US/admin/risk.json` 中补齐对应多语言条目。

### 2.4 后端 Java 框架与风控研判层改造
- **修改文件**：
  1. [`RequestHolder.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/security/util/RequestHolder.java)：
     - 新增 `getDeviceFingerprint()`、`getTlsProtocol()`、`getTlsCipher()`、`getTlsCiphers()`、`getTlsCurves()`、`getTlsAlpn()` 辅助方法；
  2. [`SystemSettingKeys.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/constant/SystemSettingKeys.java) & [`SystemSettingInitializer.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/init/SystemSettingInitializer.java)：
     - 注册 `system.risk.fingerprint.enabled`（设备指纹风控开关，默认 true）；
     - 注册 `system.risk.headless_penalty.enabled`（无头模拟器严控惩罚开关，默认 true）；
     - 注册 `system.risk.tls_check.enabled`（TLS 握手特征一致性核验开关，默认 true）；
     - 注册 `system.risk.device_rate_limit_max`（单设备跨 IP 滑动窗口最大请求数，默认 100）；
  3. [`CaptchaVerifyRequestDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/domain/dto/CaptchaVerifyRequestDTO.java)：
     - 增加 `deviceFingerprint` 与 `browserFamily` 字段，并保留向后兼容的 6 参数构造方法；
  4. [`AdminIpDiagnosisVO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/domain/vo/AdminIpDiagnosisVO.java)：
     - 扩充设备指纹、浏览器类型与 TLS 详细握手属性；
  5. [`RiskEvaluationEngine.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/risk/engine/RiskEvaluationEngine.java)：
     - **跨 IP 设备频控与失败惩罚**：维护 `vredis:verification:risk:device_failures:{deviceHash}` 与 `vredis:verification:risk:device_velocity:{deviceHash}` 滑动窗口；
     - **无头浏览器识破**：当检测到无头标记（`hl=1`）且开启惩罚时，直接提级为 `RiskLevel.HIGH`；
     - **主流浏览器白名单放行**：对 Safari、Edge、Firefox、Chrome 的正常访问判定为 `RiskLevel.LOW`，保障业务正常访问；
     - **自动化发包脚本惩罚**：识别 Python `requests`、`urllib3`、Go `net/http`、`curl`，直接评级为 `RiskLevel.HIGH`；
     - **实时画像缓存**：将解析结果写入 `vredis:verification:risk:client_profile:{ip}`（TTL 24小时），供管理后台秒级排查；
  6. [`CaptchaSDKImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/sdk/CaptchaSDKImpl.java)：
     - 核验门禁与挑战决策自动联动设备 Token，并在验证成功/失败时同步记录设备维度的奖惩计数；
  7. [`AdminRiskServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/service/admin/impl/AdminRiskServiceImpl.java)：
     - IP 诊断探针自动从 Redis 提取客户端画像，并组装返回包含设备指纹与 TLS 握手特征的诊断视图；
  8. [`RiskIpFilter.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/security/filter/RiskIpFilter.java)：
     - 在前置过滤器链中增加针对 `X-Device-Fingerprint` 的设备级滑动窗口频控，防范海量代理池分散请求。

---

## 3. 验证与测试结果

1. **后端单元测试**：
   - 编写专用测试类 [`DeviceAndTlsRiskEngineTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/test/java/com/humolix/volur/verification/DeviceAndTlsRiskEngineTest.java)，验证了：
     - Safari 用户正常访问放行（`RiskLevel.LOW`）；
     - Edge 用户正常访问放行（`RiskLevel.LOW`）；
     - Firefox 用户正常访问放行（`RiskLevel.LOW`）；
     - Chrome 用户正常访问放行（`RiskLevel.LOW`）；
     - Python requests 发包工具判定为 `RiskLevel.HIGH`；
     - curl 命令行工具判定为 `RiskLevel.HIGH`；
     - 无头浏览器 / Puppeteer 特征判定为 `RiskLevel.HIGH`；
     - 跨 IP 相同设备失败超限触发高风险升阶；
     - 无 TLS 协议头（本地开发/HTTP）优雅降级放行。
   - 运行 `mvn test -pl volur-verification,volur-auth`：**共 60 个单元测试 100% 全部通过，0 失败，0 错误**。
2. **前端工程编译**：
   - 执行 `npm --prefix src/volur-lite-view run build`（含 `vue-tsc --noEmit` 严格类型静态审查与 Rollup/Vite 生产构建）：**6.09 秒内构建成功，0 错误，0 警告**。

---

## 4. 多层反向代理拓扑（宿主机前置 Nginx + 内部容器 Nginx）协同部署规范

在典型的生产环境部署中，系统通常由**宿主机前置 Nginx**（监听公网 443 SSL，负责域名证书解析与端口转发）与**内部容器/网关 Nginx**（监听 8080 并代理静态资源与 Java 后端）组成多层代理链路：

```
[ 客户端浏览器 / 脚本 ] (HTTPS:443)
          │
          ▼
[ 宿主机前置 Nginx (终结 HTTPS 握手，持有真实 $ssl_* 变量) ]
          │ (通过 HTTP 协议转发至 127.0.0.1:8080，注入 X-TLS-* 请求头)
          ▼
[ 内部容器 Nginx (`src/volur-lite-view/nginx/conf/nginx.conf`) ]
          │ (通过 map 机制自适应继承 $http_x_tls_*，透传给 Java 后端)
          ▼
[ Volur Lite Java 后端 (`volur-app:8080`) ]
  (RequestHolder 提取 X-TLS-* ➔ RiskEvaluationEngine 进行握手特征与设备指纹协同研判)
```

### 4.1 宿主机前置 Nginx (`/etc/nginx/conf.d/platform.conf`) 配置
宿主机前置 Nginx 是客户端 TLS 握手的终结点，必须在 `location /` 块中配置向后透传 TLS 协商特征变量：
```nginx
location / {
    proxy_pass http://127.0.0.1:8080;

    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;

    # 必须显式透传客户端真实的 TLS 协商握手特征给下游容器
    proxy_set_header X-TLS-Protocol $ssl_protocol;
    proxy_set_header X-TLS-Cipher $ssl_cipher;
    proxy_set_header X-TLS-Ciphers $ssl_ciphers;
    proxy_set_header X-TLS-Curves $ssl_curves;
    proxy_set_header X-TLS-ALPN $ssl_alpn_protocol;
    proxy_set_header X-TLS-Session-Reused $ssl_session_reused;

    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection $connection_upgrade;

    proxy_buffering off;
    proxy_request_buffering off;
    proxy_read_timeout 86400s;
    proxy_send_timeout 86400s;
}
```

### 4.2 内部容器自适应继承机制 (`nginx.conf`)
下游容器通过 `map` 实现了多拓扑自适应判断：
- 若上游宿主机反向代理已传入 `X-TLS-*`，下游容器自动继承上游变量；
- 若当前容器自身终结 HTTPS，则自动回退使用本容器的 `$ssl_*` 变量；
- 若全链路无 SSL（纯 HTTP 本地调试），则变量为空并触发后端优雅降级，保障了全场景下的零编译与平滑兼容。
