# WebRTC 考场监考大屏 P2P 连线中断与黑屏问题修复及 STUN 架构分析

## 1. 变更背景与问题根因分析

在理论赛考场监考大屏 `AdminTheoryCompetitionMonitorView.vue`（具体实现由 `AdminTheoryCompetitionTeamMonitor.vue` 承载）的“WebRTC 实时巡视 (P2P)”功能中，监考人员频繁遇到 WebRTC 连线失败或被中断，且视频监控区域呈现大面积黑屏的问题。

经过端到端链路排查，确认存在以下五个维度的核心缺陷：
1. **STUN 节点受阻与零 TURN 中继**：原实现硬编码 Google STUN（`stun.l.google.com:19302`），在国内绝大多数网络（公网/校园网/政企专网）下被阻断，无法搜集外网反射地址（`srflx`）；在对称型 NAT（Symmetric NAT）环境下 P2P 穿透率基本为 0，且无 TURN 中继保底。
2. **ICE Candidate 竞态条件（Race Condition）与静默丢失**：考生端在 `setLocalDescription(offer)` 之后立即触发 `onicecandidate` 向服务端推送候选者。若管理端尚未完成 `setRemoteDescription(offer)`，直接调用 `addIceCandidate()` 会抛出 `InvalidStateError`。原代码以空 `catch {}` 吞掉异常，且无暂存队列，关键打洞地址被静默丢弃。
3. **视觉黑屏与缺乏优雅降级**：当 P2P 状态处于 `FAILED` 或连线中时，界面原有的遮罩层为 `bg-black/85`，底层又是全黑 `<video>`，导致界面形成深色黑洞。同时未利用考生已上报的 320P/1080P 屏幕快照作为降级底图。
4. **考生端 Track 生命周期断裂未同步**：当考生点击浏览器自带的“停止屏幕共享”或流异常中断时，考生端未向管理端发送 `HANGUP` 信令，导致管理端连接悬空后超时中断。
5. **DOM 挂载时序与 MediaStream 绑定隐患**：`pc.ontrack` 触发时若 `rtcVideoRef` 尚未完成挂载（如模态框动效未完成），媒体流未正确挂载至 video 标签，导致虚假 `CONNECTED` 状态下的绝对黑屏；且未对 `event.streams[0]` 为空时使用 `new MediaStream([event.track])` 兜底。

---

## 2. Spring Boot Controller 实现 STUN 的可行性分析

用户询问：“**对于更换可用国内 STUN，是否能在 Spring 上通过 Controller 实现一个 STUN？**”

### 2.1 结论
**不能在 Spring MVC/WebFlux 的 `@RestController` 或 `@Controller` 上实现 STUN 协议。**

### 2.2 核心原理与协议冲突
1. **网络层与传输层根本矛盾**：
   - **STUN 协议规范（RFC 5389 / RFC 8489）**：STUN 是一个工作在**传输层 UDP 协议（默认端口 3478）**（或特定 TLS/TCP 5349）的**纯二进制报文协议**。
   - **Spring Controller**：基于应用层 HTTP 协议（HTTP/1.1 或 HTTP/2），运行在 Servlet 容器（Tomcat/Undertow）上，解析的是 HTTP 请求头、请求体与动词。
2. **浏览器原生 WebRTC ICE Agent 行为限制**：
   - 浏览器 WebRTC 内核（如 libwebrtc）解析 `urls: 'stun:...'` 时，由操作系统底层 C++ Socket 直接向目标 IP/域名和 3478 端口发送原始 **UDP 二进制数据包**（以 20 字节 Header、Magic Cookie `0x2112A442`、Binding Request `0x0001` 等组织），**完全不走 HTTP 协议栈，也不会发起任何 HTTP 请求**。
   - 因此，HTTP Controller 根本收不到、也无法响应浏览器的 STUN 探针。
3. **Java/Spring Boot 实现 STUN 的工程局限与推荐方案**：
   - 若执意在 Spring Boot 中实现 STUN，必须脱离 Controller，在 Spring Boot 启动阶段通过 **Netty UDP Server（`NioDatagramChannel` 绑定 3478 UDP 端口）** 手动解析二进制报文。
   - 但 STUN 只能解决简单锥形 NAT 打洞；在考场复杂的对称 NAT 环境下，必须依靠 **TURN 中继（RFC 5766/8656）**。TURN 会产生极高吞吐的音视频 UDP 数据转发，在 JVM 中做转发会消耗海量带宽和产生 GC 抖动。
   - **业界标准架构**：
     - **轻量环境**：使用成熟稳定的国内公网 STUN（如阿里云 `stun.aliyun.com:3478`、腾讯云 `stun.cloud.tencent.com:3478` 等），完全免费且高可用；
     - **生产专网/强对抗环境**：独立部署成熟的开源 C 语言服务 **Coturn**，Spring Boot Controller 仅充当**凭证网关**，通过 REST API 为前端颁发带有效期的 TURN 认证用户名与 HMAC-SHA1 凭据（REST API Token）。

---

## 3. 详细代码重构与改动说明

### 3.1 创建全局 WebRTC 统一配置工具 (`src/utils/webrtc.ts`)
- 集中维护高可用国内 STUN 服务器列表：
  - `stun:stun.aliyun.com:3478`（阿里云 STUN）
  - `stun:stun.cloud.tencent.com:3478`（腾讯云 STUN）
  - `stun:stun.qq.com:3478`
  - `stun:stun.miwifi.com:3478`
  - `stun:stun.l.google.com:19302`（保留为海外网络备用）
- 统一 `iceCandidatePoolSize: 2`，提前预搜集候选者以减少首帧握手延迟。

### 3.2 管理端巡视大屏重构 (`AdminTheoryCompetitionTeamMonitor.vue`)
1. **引入 ICE Candidate 双向缓冲队列**：
   - 声明 `adminPendingIceCandidates: RTCIceCandidateInit[]`；
   - 当收到 `ICE` 信令时，若 `pc.remoteDescription` 尚未就绪，将候选者暂存至队列；在 `setRemoteDescription` 执行成功后，一次性顺序排出并安全执行 `addIceCandidate()`，彻底消灭信令竞态导致的候选节点丢失。
2. **流媒体安全挂载与 DOM 时序对齐**：
   - 封装 `bindRemoteStreamToVideo()`，采用 `nextTick()` 确保 DOM 节点挂载完毕后再绑定 `srcObject`；
   - 兼容处理 `(event.streams && event.streams[0]) ? event.streams[0] : new MediaStream([event.track])`；
   - 添加 `watch(rtcVideoRef)` 侦听器，当模态框 DOM 延迟挂载完成时自动补绑已有媒体流。
3. **软故障优雅降级（彻底消除纯黑屏）**：
   - 底层始终渲染考生上报的最新静态屏幕帧（`currentDisplayFrame`），在非 `CONNECTED` 状态下保持 `opacity-80` 可见；
   - 将原先全黑遮罩替换为带有毛玻璃质感的居中提示卡片（`bg-black/60 backdrop-blur-[2px]`），明确提示当前网络连接状态及失败原因（NAT 阻隔/防火墙等）；
   - 在中断状态下提供“重新发起连线”和“查看静态快照”快捷按钮，杜绝黑屏恐慌。
4. **连接与 Track 生命周期监听完善**：
   - 增加对 `pc.oniceconnectionstatechange` 的监听，当 `iceConnectionState === 'failed'` 时及时将状态同步为 `FAILED`；
   - 监听远端 `event.track.onended`，当考生端断流时立即更新状态。

### 3.3 考生端推流与信令协同优化 (`TheoryCompetitionView.vue`)
1. **信令时序与候选者队列同步**：
   - 引入 `candidatePendingIceCandidates: RTCIceCandidateInit[]`，在收到管理端 `ANSWER` 前到达的 ICE 候选者一律进入缓冲队列，避免被静默丢弃。
2. **屏幕共享终止主动通知**：
   - 在 `track.onended` 事件中，向管理端发送 `HANGUP` 信令，释放当前 RTCPeerConnection，确保管理端能第一时间捕获流终结状态。
3. **推流不可用快速失败回执**：
   - 当收到 `CALL` 但自身无可用屏幕流（`!proctorStore.screenStream`）时，立即向管理端回传 `HANGUP`，避免管理端无限处于 `CALLING` 悬挂状态。

---

## 4. 影响范围与验证情况

- **影响范围**：
  - `src/views/admin/competition/components/AdminTheoryCompetitionTeamMonitor.vue`
  - `src/views/competition/TheoryCompetitionView.vue`
  - `src/utils/webrtc.ts`
- **编译与类型校验**：
  - 运行 `npm run build`，构建耗时 15.12s，TypeScript 类型检查通过，无任何语法错误与样式冲突。
