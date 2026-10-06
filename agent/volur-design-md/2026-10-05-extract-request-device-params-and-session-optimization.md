# 优化设备参数模型 RequestDeviceParams 与 Session 体系施工记录

## 1. 方案背景与目标

在 Volur Lite 分布式会话与安全体系演进中，针对会话中记录的客户端设备与硬件环境参数进行了深度优化：
1. **解除内部类偶联与序列化缺陷**：原先临时放在 `Session.java` 内部的 `private class RequestDeviceParams` 属于私有非静态内部类，无法对外暴露，且在 Redis / Jackson 序列化与反序列化时存在作用域限制与上下文持有问题；
2. **设备环境特征维度扩展**：将原本扁平零散的 `fingerprint` 和 `ip` 扩展为多维度的终端硬件与运行环境特征模型，与前端设备指纹探针（Canvas、WebGL、显卡硬件、时区、屏幕参数等）以及服务端风控引擎（`RiskEvaluationEngine`）的设备解析特征全面对齐；
3. **架构解耦与独立模型沉淀**：将 `RequestDeviceParams` 提炼为独立的领域模型类 `com.humolix.volur.security.domain.session.RequestDeviceParams`，并对 `Session.java` 进行纯粹化重构。

---

## 2. 核心架构设计与改动点

### 2.1 独立设备特征参数模型 (`RequestDeviceParams.java`)

位置：`src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/domain/session/RequestDeviceParams.java`

- **网络与核心特征**：`ip`、`fingerprint`、`deviceHash`、`deviceToken`；
- **渲染与微特征指纹**：`canvasHash`、`renderer`（WebGL 显卡渲染器）、`vendor`（显卡供应商）；
- **运行环境与模拟器探测**：`browserFamily`、`osFamily`、`isHeadless`（无头浏览器/模拟器识别）；
- **设备屏幕与硬件**：`screenResolution`、`colorDepth`、`timezone`、`language`、`platform`；
- **动态扩展字典**：`@Builder.Default private Map<String, Object> extraParams = new HashMap<>();`；
- **便捷构造工厂**：提供 `RequestDeviceParams.of(String ip, String fingerprint)`。

### 2.2 基础会话模型更新 (`Session.java`)

位置：`src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/domain/session/Session.java`

- 移除内部类 `private class RequestDeviceParams`；
- 维护 `private RequestDeviceParams requestDeviceParams;` 强类型独立引用，配合 `clientIp` 与 `deviceFingerprint` 保持向后平滑兼容。

---

## 3. 规范与纯粹性审计

1. **纯粹代码与零注释铁律**：
   - `RequestDeviceParams.java` 与 `Session.java` 严格杜绝任何解释性注释与冗余行内备注；
   - 架构设计考量统一外置于本设计文档。
2. **彻底解耦原则**：
   - 彻底将内部类转变为独立标准领域模型，具备标准的 Serializable 契约与 Lombok 注解规范。

---

## 4. 验证与回归测试

1. **单元测试与双向序列化验证**：
   - 在 `SessionInheritanceTest.java` 中增加 `requestDeviceParams_propertiesAndSerialization_correct` 测试用例；
   - 验证了所有设备字段赋值、多字段组装、Jackson JSON 序列化与反序列化全量一致性。
2. **平台全量回归测试**：
   - 运行平台 13 个子模块的认证与安全测试，全部构建成功（`BUILD SUCCESS`）。
