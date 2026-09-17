# 真实客户端 IP 溯源穿透与登录人机风控滑动挑战闭环修复审计文档

## 1. 变更背景与问题溯源

在通过内网穿透（公网服务器 `47.76.65.240` 部署 FRP + 远程 Nginx 反代，解析域名 `platform.volurctf.com`，转发至内网 Docker 前端 Nginx 并反代至后端 Spring Boot）运行环境进行登录压力测试和并发验证码测试时，暴露出三个系统性缺陷：
1. **全站访客共享同一 IP 触发风控（牵连所有真实用户）**：
   无论外部来自不同公网 IP 或不同设备发起请求，后端获取到的远程 IP 恒为内网反代/网桥 IP（如 `172.18.0.1`）。压测导致该 IP 的请求频次和失败计数瞬时超标，全网所有用户访问登录页时均被判定为 `HIGH` 风险。
2. **图形验证码框错乱渲染滑动拼图背景图（问题 1）**：
   后端在 `riskLevel == HIGH` 时无条件将所有验证码请求强制升级为 `SLIDE_PUZZLE`。调用普通图形验证码接口（`GET /api/v1/captcha`）时，后端将滑动拼图的带缺口背景大图作为结果返回；前端将其硬塞进 `130x40` 的小验证码框中，导致严重压缩变形且无法输入。
3. **风控业务与人机核验闭环缺失（问题 2）**：
   原有流程缺乏“阻断式行为验证”机制。正确的防御流程应为：当 IP 处于高风险时，普通图形验证码不应直接返回，前端应仅展示“验证码”按钮；当用户主动点击该按钮时唤起滑动验证码弹窗，完成拼图核验并获得凭证（Ticket）后，后端扣减风险计数，前端方可换取真正的图形验证码。

### 历史架构溯源：为什么当初选择了“后端 Java IP 解析反向取值”？
通过审查 Git 历史提交（`8dc19352`）与设计文档（`2026-09-11-nginx-conf-and-1gb-upload-limit-alignment.md`）：
- **防客户端伪造 XFF 的历史惯性**：在早期防作弊模板中，普遍认为攻击者可通过 HTTP 请求头自行注入 `X-Forwarded-For: <fake-ip>`。代理服务器（如 Nginx）在转发时会将实际连接的 IP 追加至末尾（`$proxy_add_x_forwarded_for`），因此早期实现认为“末尾的 IP 才是真正不可伪造的直连 IP”，写下了 `ips[ips.length - 1]`。
- **多层代理拓扑下的致命缺陷**：当架构演进为“远程公网 Nginx -> FRP 隧道 -> 内网前端 Nginx -> Spring Boot”多层代理时，内网 Nginx 会将本地连接 IP（如 `172.18.0.1`）追加至末尾，且内网 Nginx 原配置中使用了 `proxy_set_header X-Real-IP $remote_addr;` 覆盖了上游传入的真实客户端 IP。这导致 `X-Real-IP` 与 `X-Forwarded-For` 末位双双沦为内部代理网关 IP。

---

## 2. 核心改动点与实现细节

### 2.1 远程服务器端口安全加固与内网 Nginx 多拓扑级联映射 (`nginx.conf`)
1. **远程服务器 iptables 防火墙加固**：
   在远程服务器 `47.76.65.240` 上配置防火墙规则，禁止外部公网绕过 Nginx 直接访问 8080 端口，确保所有 HTTP/HTTPS 流量均经过公网 Nginx（注入真实的 `X-Real-IP` 和 `X-Forwarded-For`）：
   ```bash
   iptables -I INPUT -p tcp --dport 8080 ! -s 127.0.0.1 -j DROP
   ```
2. **内网前端 Nginx 多拓扑自适应级联映射 (`src/volur-lite-view/nginx/conf/nginx.conf`)**：
   重构多级 `map` 指令，实现同时完美自适应 CDN、反向代理/FRP 及单机直连：
   ```nginx
   # 1. 优先提取 CDN/WAF 专用真实头 (Cloudflare CF-Connecting-IP, True-Client-IP)
   map $http_cf_connecting_ip $cdn_real_ip {
       default $http_cf_connecting_ip;
       ""      $http_true_client_ip;
   }
   # 2. 其次继承上游反代/FRP 隧道透传的 X-Real-IP
   map $cdn_real_ip $upstream_or_cdn_ip {
       default $cdn_real_ip;
       ""      $http_x_real_ip;
   }
   # 3. 最后在直连开箱即用模式下自动采用直连 $remote_addr
   map $upstream_or_cdn_ip $client_real_ip {
       default $upstream_or_cdn_ip;
       ""      $remote_addr;
   }
   ```
   并在 `location = /api/v1/realtime/stream` 与 `location ^~ /api/` 中配置 `proxy_set_header X-Real-IP $client_real_ip;`。

### 2.2 后端多拓扑自适应与防伪造可信代理回溯算法 (`RequestHolder.java`)
采用工业界标准的可信代理反向回溯算法，解决直连、FRP 穿透、CDN 及线下局域网对抗等多种拓扑：
1. **CDN 专用头高置信度优先提取**：优先检查不可伪造的 `CF-Connecting-IP` 与 `True-Client-IP`；
2. **`X-Forwarded-For` 自右向左可信回溯（Right-to-Left Traversal）**：
   - **公网/FRP 拓扑**（链路中包含公网 IP）：从右向左逐级跳过内部私网反代（Docker 网桥 `172.16.0.0/12`、回环 `127.0.0.1`、私网中继），遇到的第一个公网 IP 即为直连公网边缘的真实客户端，同时**天然阻断最左侧由攻击者伪造的欺骗 IP**；
   - **纯内网/局域网拓扑**（线下 CTF 比赛，全为私网 IP）：从右向左跳过容器内部网桥与回环，提取真实的局域网选手客户端 IP（`192.168.x.x` / `10.x.x.x`），确保机房内各个参赛队伍拥有独立 IP，互不冲突与牵连；
3. **回退保护与合法性校验**：IPv4/IPv6 正则校验合法性，剥离误带端口号，回退至底层 TCP 连接对端 IP。

### 2.3 风控系统的多拓扑自适应与平滑降险机制 (`RiskEvaluationEngine`)
- **本地回环白名单豁免**：对 `127.0.0.1`、`0:0:0:0:0:0:0:1` 与 `localhost` 进行风控旁路放行，杜绝本地单机开发与容器健康检查被误封；
- **废除一刀切 `delete` 清空**：在 `RedisSlidingWindowRateLimiter` 中实现 `reduceCount(key, count)`，通过 `removeRange` 移除指定数量的最旧记录；
- **降低惩罚机制**：当用户通过滑动拼图完成核验时，调用 `riskEngine.reduceRisk(ip)` 扣减 6 次失败计数，使其脱离高危阈值（>=10次），但保留部分历史风险底数；
- **持续请求升险**：保留请求速率累加（`recordVelocity`），若用户在核验后仍频繁恶意重复获取，计数再度累积将迅速升回 `HIGH` 风险。

### 2.4 图形验证码接口解耦与 Ticket 解锁 (`CaptchaServiceImpl` & `CaptchaController`)
- **VO 增强**：`CaptchaResponseVO` 扩充 `@Builder.Default private Boolean requireBehaviorVerify = false;`；
- **接口扩展**：`GET /api/v1/captcha` 新增支持可选查询参数 `ticket`；
- **防错乱逻辑**：
  - 若未携带 `ticket` 且评估为 `HIGH` 风险：直接返回 `requireBehaviorVerify = true` 且 `imageBase64 = null`，严禁下发滑动拼图大图；
  - 若携带合法 `ticket`：核销 Ticket 并执行 `reduceRisk` 降险，生成并返回真正的字符图形验证码（LINE 或 SHEAR）。

### 2.5 前端登录表单人机风控联动 (`AuthForm.vue` & `verification` API)
- 拓展前端 `CaptchaResponseVO` 类型及 `getCaptchaApi(params)` 请求定义；
- 登录页自动尝试拉取验证码：若命中 `requireBehaviorVerify: true`，前端清空图片，仅显示“验证码”按钮，不自动弹出模态弹窗；
- 用户主动点击“验证码”按钮时，弹出 `<SlidePuzzleModal>` 滑块弹窗（场景设为 `LOGIN`）；
- 滑动拼图通过后，凭借 Ticket 重新调用 `fetchCaptcha(ticket)` 获取真实的字符图形验证码并渲染展示，用户输入图形验证码后完成登录。

---

## 3. 验证与测试结果

1. **后端单元测试**：
   - 新增并扩充 `RequestHolderIpTest`（7 个综合场景用例全部通过）：
     - `testSingleProxyPublicIp`：单层反代提取公网 IP；
     - `testMultiProxyFRPAndDockerNetwork`：FRP 穿透 + Docker 网桥多层代理，精准提取公网真实客户端；
     - `testAntiSpoofingRightToLeftTraversalBlocksFakeIp`：攻击者伪造 XFF 欺骗，自右向左回溯成功阻断伪造 IP；
     - `testCloudflareConnectingIpPriority`：Cloudflare CDN 头优先采信；
     - `testTrueClientIpPriority`：True-Client-IP 优先采信；
     - `testPureInternalLANMultiPlayers`：线下 CTF 局域网开箱即用，不同选手独立识别，互不冲突；
     - `testLocalhostDirect`：本地单机开发开箱即用。
   - 验证 `CaptchaRiskFlowTest`：高风控阻断、图片防泄露、Ticket 降险与图形码发放，断言全通过；
   - 执行 `mvn test-compile` 与全模块单元测试，全平台 12 个模块全部 `BUILD SUCCESS`。
2. **前端静态检查与构建验证**：
   - 执行 `npm run build`，5.50 秒打包成功，无任何 TypeScript 类型冲突。

---

## 4. 影响范围

- **受影响模块**：`volur-framework`、`volur-verification`、`volur-lite-view`（Nginx 配置、API 层、登录表单组件）。
- **向下兼容性**：
  - **开箱即用（`docker compose up -d`）**：无须任何外部域名或反代配置，系统自动识别直连客户端 IP，本地回环自动免受风控误伤；
  - **内网穿透（FRP / Ngrok / Cloudflare Tunnel）**：多层代理中继自动穿透，公网访客真实 IP 准确还原；
  - **企业级 CDN / WAF 接入**：自动采信专用真实客户端 Header；
  - **线下 CTF 局域网对抗**：机房内各个选手的局域网私网 IP 独立辨识，杜绝一人封禁全场受牵连。
