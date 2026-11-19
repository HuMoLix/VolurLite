# 施工与修改记录：统一设备指纹获取方式至 RequestUtil 并净化 DTO 实体

**日期**：2026-10-05  
**作者**：Antigravity Agent  
**模块**：`volur-auth`、`volur-lite-view-NEXT`

---

## 1. 变更背景与安全动机

在先前 [`SigninServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/SigninServiceImpl.java) 中，账号标识校验方法采用三元表达式获取设备指纹：
```java
String rawFp = StringUtils.hasText(request.getDeviceFingerprint())
        ? request.getDeviceFingerprint()
        : RequestUtil.getDeviceFingerprint();
```
该实现存在以下安全隐患与架构瑕疵：
1. **防伪造漏洞（Rate-Limit & Anti-Brute-Force Bypass）**：
   - 显式优先信任请求体中用户上报的 `deviceFingerprint`。恶意攻击者或撞库自动化脚本只需在每次发包时在 JSON 中随机生成不同的 `deviceFingerprint` 字符串，即可轻易逃逸基于 `subjectKey`（`ip:fingerprint`）的失败次数频控（`vredis:auth:identifier_failures:...`）与风控打分；
2. **违背全域单一权威数据源规范**：
   - 平台双 Token 架构改造（`_v_did` / `_v_dtk`）明确规定以 Cookie 为唯一权威来源，`RiskIpFilter` 与 `CaptchaSDK` 均已直接从 `RequestUtil.getDeviceFingerprint()` 提取；
3. **前端实际从未传递该字段**：
   - 前端 `submitSignInIdentifierApi` 早已不再发送 `deviceFingerprint`，生产流量实质上全部走 Cookie 分支；保留该字段徒增契约复杂性；
4. **覆盖完整设备特征矩阵的风险**：
   - 原代码在 `identifier` 中构造了仅有 2 个字段的 `RequestDeviceParams.of(clientIp, rawFp)` 传给 `recoverySession`，会导致会话在 `initSess` 阶段解密采集的 14 项完整设备参数（WebGL、Canvas、屏幕分辨率等）被覆盖；应统一像 `SignUpServiceImpl` 一样传入 `null` 保留原始会话特征。

---

## 2. 详细改造清单

### 2.1 后端领域 DTO 与服务层重构 (`volur-auth`)
1. **`SignInIdentifierRequestDTO.java` 实体净化**：
   - 移除 `private String deviceFingerprint;` 字段；
   - 移除行内 JavaDoc 解释性注释，保持零注释纯净代码规范。
2. **`SigninServiceImpl.java` 统一指纹获取与设备特征保护**：
   - 直接使用 `String rawFp = RequestUtil.getDeviceFingerprint();` 从权威 Cookie 体系提取指纹；
   - 调用 `sessionManager.recoverySession(request.getDsh(), "VlifWebLogin", "WebServiceLogin", requestHeader, null)`，传入 `null` 设备参数，确保完好继承 `initSess` 绑定的全量高精度设备矩阵；
3. **`SigninServiceImplTest.java` 单元测试同步适配**：
   - 在 `setUp()` 中为 `MockHttpServletRequest` 配置 `_v_dtk` Cookie 桩（`fp_sample_123`）；
   - 从测试用例中彻底移除已废弃的 `dto.setDeviceFingerprint(...)`。

### 2.2 前端类型契约对齐 (`src/volur-lite-view-NEXT`)
1. **`src/types/auth/flow.ts` 强类型对齐**：
   - 从 `SignInIdentifierRequestDTO` 接口中删除 `deviceFingerprint?: string` 字段，前后端 DTO 字段 1:1 严格对齐。

---

## 3. 验证结果

1. **后端单元测试**：
   - 执行 `mvn test -pl "volur-security,volur-auth" "-Dtest=SigninServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false"`；
   - 结果：`BUILD SUCCESS`，7 项单元测试全部通过（0 Failure, 0 Error）。
2. **前端强类型检查与生产打包**：
   - 执行 `npm run build`（`vue-tsc --noEmit && vite build`）；
   - 结果：2859 个模块类型审查与打包全部成功，0 错误，0 警告。
