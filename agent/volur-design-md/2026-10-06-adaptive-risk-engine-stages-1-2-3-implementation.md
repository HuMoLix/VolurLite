# 现代自适应风控系统（阶段一、阶段二、阶段三）全链路实现记录

## 1. 施工背景与核心目标
针对原风控系统仅消费扁平 IP/账号计数、未消费 `initSess` 上报的多维硬件密文资产、缺乏环境自洽性判定、会话易被中途劫持以及缺失用户受信设备自适应 MFA 等问题，本次任务完成了前置三阶段的全面落地：
- **阶段一（环境自洽性风控评估引擎落地）**：消费 WebGL 显卡、无头浏览器、Client Hints、屏幕尺寸与颜色深度，拦截自动化与环境伪造；
- **阶段二（会话设备基线防御落地）**：在会话生命周期内对 IP、设备指纹、UA 家族进行防劫持比对；
- **阶段三（用户受信设备与新设备自适应 MFA 落地）**：引入 `user_trusted_device` 体系，支持常用设备免打扰直通、未知新设备强制触发 `etotp` 挑战并下发安全验证邮件。

---

## 2. 核心架构与落地组件清单

### 2.1 阶段一：多维环境矩阵评估引擎 (`DeviceEnvironmentMatrixEvaluator`)
- **实现文件**：[`DeviceEnvironmentMatrixEvaluator.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/risk/DeviceEnvironmentMatrixEvaluator.java)
- **核心判定维度**：
  1. **虚拟化/无头渲染器特征**：`renderer` 包含 `swiftshader`、`llvmpipe`、`mesa offscreen`、`virtualbox`、`vmware` 等软件驱动直接评定为 `RiskLevel.HIGH`；
  2. **跨维度特征矛盾**：Windows 操作系统上报 Apple GPU/Metal、macOS 操作系统上报 Direct3D/NVIDIA、Client Hints `Sec-CH-UA-Platform` 与 JS `osFamily` 冲突等，直接评定为 `RiskLevel.HIGH`；
  3. **屏幕与物理参数畸变**：`screenResolution` 为 `0x0` 或 `800x600`（无头默认尺寸）评定为 `RiskLevel.HIGH`；色深异常评定为 `RiskLevel.MEDIUM`。
- **调用链路集成**：
  - `SessionServiceImpl.initSess`：在创建会话时执行矩阵评估并持久化到 `Session.riskLevel`；
  - `SigninServiceImpl.identifier`：结合 `RiskEvaluationEngine` 与 `DeviceEnvironmentMatrixEvaluator` 综合评级，高风险自动阻断并插入 `recaptcha`。

### 2.2 阶段二：会话生命周期设备基线防御 (`SessionDeviceBaselineValidator`)
- **实现文件**：[`SessionDeviceBaselineValidator.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/session/validator/SessionDeviceBaselineValidator.java)
- **核心比对维度**：
  - 指纹一致性：核验当前请求中的设备指纹与会话初创时的基线指纹，发生突变直接抛出 403 异常阻断；
  - 异构环境跳跃：若 IP 发生跳变且浏览器家族（Browser Family）不一致，判定为跨端窃取，立即阻断。
- **调用链路集成**：
  - 在 `SignInServiceImpl` 的 `challenge`、`selectChallenge`、`authenticate` 方法入口处前置调用 `sessionDeviceBaselineValidator.validateBaseline(session)`。

### 2.3 阶段三：用户受信设备体系与新设备自适应 MFA
- **持久化实体与仓库**：
  - [`UserTrustedDevice.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/domain/entity/UserTrustedDevice.java)
  - [`UserTrustedDeviceRepository.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/repository/UserTrustedDeviceRepository.java)
- **业务服务**：
  - [`UserTrustedDeviceService.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/service/UserTrustedDeviceService.java)
  - [`UserTrustedDeviceServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/service/impl/UserTrustedDeviceServiceImpl.java)
- **数据库增量平滑迁移**：
  - 在 [`scripts/upgrade/upgrade_to_latest.sql`](file:///H:/HuMoLix/github/Volur-Lite/scripts/upgrade/upgrade_to_latest.sql) 中幂等新建 `user_trusted_device` 表结构与索引。
- **自适应登录状态机编排 (`SignInServiceImpl`)**：
  - 密码验证成功后：
    - 若设备为受信设备：记录活跃时间并直接推进至 `authenticate` 终态；
    - 若为新设备/异地登录且绑定邮箱：动态将 `SigninFlow.waitingSteps` 调整为 `["etotp", "authenticate"]`，自动发送邮箱验证码并向前端返回 `nextStep: "etotp"`；
  - 邮箱验证码通过后：
    - 自动将新设备登记为用户受信设备，并推进至 `authenticate` 终态。

---

## 3. 测试与构建验证

1. **单元测试与集成测试**：
   - 编写并执行新增单元测试：
     - `DeviceEnvironmentMatrixEvaluatorTest.java` (5/5 绿灯)
     - `SessionDeviceBaselineValidatorTest.java` (4/4 绿灯)
     - `UserTrustedDeviceServiceImplTest.java` (4/4 绿灯)
     - `SigninServiceImplTest.java` 扩充受信直通与新设备自适应 MFA 测试用例 (11/11 绿灯)
   - 运行平台测试：`mvn test -pl volur-auth -am`，58/58 测试 100% 通过。
2. **前端构建验证**：
   - 运行 `npm run build`，耗时 4.72s 零警告零错误构建成功。
