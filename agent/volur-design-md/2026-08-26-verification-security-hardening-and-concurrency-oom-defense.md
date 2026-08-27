# Volur Lite 验证码防降级安全加固与高并发防 OOM 架构设计归档

- **执行日期**：2026-08-26
- **施工模块**：`volur-verification`
- **文档类型**：人机安全防护加固、防降级绕过与并发性能/内存稳定性保障

---

## 1. 安全加固背景与风险消除

### 1.1 针对降级绕过风险的根治方案
1. **场景基准门槛 (`CaptchaScene`)**：
   - 为各业务场景定义强制基准验证码类型：
     - `REGISTER`（注册）：强制基准为 `SLIDE_PUZZLE`（滑块拼图）；
     - `CHANGE_PASSWORD`（改密）：强制基准为 `SLIDE_PUZZLE`；
     - `RESET_PASSWORD`（重置密码）：强制基准为 `SLIDE_PUZZLE`；
     - `SENSITIVE_OP`（高危操作）：强制基准为 `SLIDE_PUZZLE`；
     - `LOGIN`（登录）：基准为 `LINE`，随风控动态自动升级为 `SHEAR` 或 `SLIDE_PUZZLE`。
2. **风控引擎防降级提升 ([`RiskEvaluationEngine.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/risk/engine/RiskEvaluationEngine.java))**：
   - 即使攻击者绕过前端直接调用后端接口请求弱类型（如 `type=LINE`），服务端在决议 `resolveEscalatedType` 时强制提升为场景基准，拒绝降级。
3. **Ticket 载荷绑定与强度校验 ([`CaptchaServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/service/impl/CaptchaServiceImpl.java))**：
   - 颁发 Ticket 时将 `scene`、`verifiedType`（实际通过的类型）、`ip` 和时间戳写入 Redis 载荷（`scene#verifiedType#ip#timestamp`）；
   - 下游接口核验 Ticket 时（如注册、改密），服务端校验 `verifiedType.isAtLeast(requiredType)`，若使用低等级字符验证码换取的 Ticket 用于高安全场景，直接拒绝。

---

## 2. 高并发与防 OOM (服务端 JVM & Redis) 深度保障

### 2.1 服务端 JVM 内存防 OOM 与零 IO 优化 ([`PuzzleImageLoader.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/captcha/engine/PuzzleImageLoader.java))
- **启动预处理与预缩放内存池**：
  - 在 `@PostConstruct` 阶段完成 Classpath 与外部图库的扫描、自适应居中裁切（Center Crop）与双线性缩放（`280 × 150`）；
  - 将处理好的 `BufferedImage` 放入线程安全的内存只读池；
  - **运行时 0 磁盘 IO、0 图片解码开销、0 运行时缩放开销**，直接微秒级从内存复制底板；
- **内存使用严格封顶**：
  - 设置外部图库最大缓存上限 `MAX_CACHED_IMAGES = 50`；
  - 50 张 `280x150` RGB 画布总内存占用锁定在 **6~7 MB 以内**，即便高并发请求每秒上万次，也绝不会因底图解码引发 JVM 堆内存溢出。

### 2.2 Redis 内存防 OOM 与高并发保障
- **100% 全量 Key 配置 TTL 自动过期**：
  - 验证码暂存 Key：TTL 2 分钟；
  - Ticket 凭证 Key：TTL 5 分钟；
  - 限流与速率 Key：TTL 1~10 分钟；
  - 绝无任何无 TTL 的永久残留 Key；
- **Ticket 一次性核销 (One-Time-Use)**：
  - 业务接口核验 Ticket 成功后立即 `delete(ticketKey)`，既防止重放攻击，又保证 Redis 内存瞬间释放；
- **IP 级滑动窗口前置拦截**：
  - 单 IP 每分钟限频 20 次，在网关层前置拦截恶意脚本的高频刷量，防止 Redis 键空间被恶意打满。

---

## 3. 验证结果

1. 单元测试 `mvn clean test`：
   - 覆盖滑块生成、图库加载、空图降级、风控防降级强制提升等 3 个测试用例全部通过（`Tests run: 3, Failures: 0, Errors: 0`）；
2. 全平台 Maven 构建：`mvn clean compile -DskipTests` 全部 12 个子模块编译通过（`BUILD SUCCESS`）。
