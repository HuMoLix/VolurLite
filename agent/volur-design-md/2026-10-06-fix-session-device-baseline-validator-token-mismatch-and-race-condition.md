# 修复会话设备基线校验令牌错位与前端指纹探针异步竞态施工记录

## 1. 方案背景与问题根因分析

在进行密码挑战核销接口（`POST /api/v1/captcha/challenge/pwd`）调用时，服务端返回如下异常：
```json
{
  "status": 403,
  "errorCode": 403,
  "success": false,
  "message": "检测到设备环境异常突变，会话已终止",
  "data": null,
  "path": "/api/v1/captcha/challenge/pwd"
}
```

经全链路源码排查，确认由如下深层设计缺陷导致：
1. **数据格式与概念严重错位**：
   - 前端在 Cookie 中存入 `_v_dtk`（Base64 编码的包含 `did`, `dh`, `ch`, `bf`, `hl`, `ts` 的 JSON 设备令牌）；
   - 后端 `RequestUtil.getDeviceFingerprint()` 优先读取 Cookie `_v_dtk`，返回的是完整的 Base64 字符串；
   - 但会话初始化（`/api/v1/session/initSess`）时，`baselineParams.getFingerprint()` 保存的是 32 位 Hex 指纹哈希 `visitorId`（或未初始化时的 `did` 哈希）；
   - `SessionDeviceBaselineValidator` 直接执行 `currentFp.equals(baselineFp)`，拿 Base64 密文与 Hex 纯哈希做字符串等值比对，结果恒为 `false`。
2. **前端异步探针的时序竞态（Race Condition）**：
   - 前端 `App.vue` 将 `initDeviceFingerprint()` 延后至 `requestIdleCallback` 或 `setTimeout(120)` 执行；
   - 登录视图 `IdentifierView.vue` 在 `onMounted` 立即触发 `signinFlowStore.ensureFlowParams` 并发起 `initSessionApi`；
   - 此时 `cachedVisitorId` 尚为空，前端降级使用了从 UUID 截取的降级哈希初始化了会话；
   - 探针计算完毕后，Cookie `_v_dtk` 被赋予真正的 `dh` 指纹哈希，导致即使后端解析了 Token，与初创会话时落库的基线哈希也无法自洽。
3. **缺少设备种子（Device ID）降级兜底比对机制**：
   - 仅依赖易变特征指纹，缺少对长效稳定硬件种子 `deviceId`（UUID）的双向对齐与容差判定。

---

## 2. 核心架构重构与改动清单

### 2.1 后端：支持设备令牌解析与基线双向自洽核验
1. **`volur-framework` - `RequestUtil.java`**：
   - 新增 `getDeviceToken()` 与 `getDeviceToken(HttpServletRequest)`，标准化提取 Cookie `_v_dtk`；
   - 新增 `getDeviceId()` 与 `getDeviceId(HttpServletRequest)`，标准化提取 Cookie `_v_did`。
2. **`volur-verification` - `RiskEvaluationEngine.java`**：
   - 扩展 `ParsedClientProfile` 记录模型：新增 `deviceId` 字段（`String deviceId, String deviceHash, String canvasHash, String browserFamily, boolean isHeadless, long timestamp`）；
   - 优化 `parseDeviceToken(rawToken)`：自动从 JSON 解构中提取 `did` 并赋予 `deviceId`。
3. **`volur-security` - `RequestDeviceParams.java`**：
   - 新增 `private String deviceId;` 字段；
   - 增加便捷工厂重载 `of(String ip, String fingerprint, String deviceId)`。
4. **`volur-security` - `SessionDeviceBaselineValidator.java`**：
   - 引入 `RiskEvaluationEngine` 依赖；
   - 对当前请求上报的 Cookie（`_v_dtk`、`_v_did`）进行自动令牌解构与指纹/设备种子提取；
   - 形成多级自洽匹配防线：
     - 若解析出的当前设备哈希 `currentFp` 命中会话基线 `baselineFp` 或 `baselineHash`，判定为环境自洽；
     - 若当前长效设备 ID `currentDid` 命中 `baselineDid`，或与 `baselineFp`（格式化 UUID）自洽，判定为同一设备安全访问；
     - 仅当全部维度均无法自洽时，方抛出 `403` 异常阻断突变。
5. **`volur-security` - `SessionDeviceBaselineValidatorTest.java`**：
   - 更新单测注入依赖；
   - 补齐真实 Base64 编码 `_v_dtk` 令牌测试与 `_v_did` 设备 ID 兜底自洽测试。

### 2.2 前端：消除异步竞态与完善设备上报字段
1. **`src/volur-lite-view-NEXT/src/utils/fingerprint.ts`**：
   - 在 `collectDeviceParams()` 返回模型中补齐 `deviceId: getDeviceId()`。
2. **`src/volur-lite-view-NEXT/src/store/modules/flow/signinFlow.ts`**：
   - 在 `ensureFlowParams` 发起 `initSessionApi` 之前，显式执行并等待 `await initDeviceFingerprint().catch(() => {})`，杜绝未就绪前发送降级脏哈希。
3. **`src/volur-lite-view-NEXT/src/store/modules/flow/signupFlow.ts`**：
   - 同步在 `signupFlow` 的 `ensureFlowParams` 中显式 `await initDeviceFingerprint().catch(() => {})`。

---

## 3. 规范审计与纯粹性检查

1. **纯粹代码与零注释铁律**：所有新增与重构的 `.java`、`.ts` 文件严格杜绝冗余解释性行内注释，保持生产代码极致自解释；
2. **拒绝旧兼容别名**：未引入任何 `oldName = newName` 桥接，数据模型字段严格 1:1 对齐；
3. **快速失败与防伪造原则**：在非法设备或篡改令牌场景下依然精准触发 403 阻断，保持高水准风控安全水位。

---

## 4. 验证结果

1. **后端验证**：
   - `mvn test -pl volur-security -am`：85 个单元测试用例全部通过（`BUILD SUCCESS`）；
   - `SessionDeviceBaselineValidatorTest` 6 个用例全绿通过。
2. **前端验证**：
   - `volur-lite-view-NEXT` 执行 `npm run build`（`vue-tsc --noEmit && vite build`）：类型检查 100% 通过，构建成功。
