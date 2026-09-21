# 施工记录：ApiResponseEntity 全面支持错误识别码 (ErrorCode) 与规范升级

**施工日期**：2026-09-19  
**施工模块**：`volur-framework` / 架构核心设计规范 (`docs/errorCode.md`)  
**审计人员**：Antigravity Agent  

---

## 1. 变更背景与需求分析

在 Volur Lite 架构中，HTTP 状态码（如 400, 401, 403, 404, 500）仅能在传输协议层面提供粗粒度的结果标识。随着业务复杂度上升与前后端深度分离，仅靠 HTTP 状态码或字符串文本对比存在诸多问题：
1. **认证凭据精细流转痛点**：在认证体系中，客户端在收到 HTTP 401 时，无法单纯通过状态码区分“Token 缺失 (`131301`)”、“Token 过期可自动无感刷新 (`131303`)”、“Token 伪造 (`131304`)”、“Token 黑名单注销 (`131305`)”或“异地多端顶号强制下线 (`131306`)”。
2. **`ApiResponseEntity` 重载不平衡**：此前 `ApiResponseEntity` 仅为 `unauthorized` 和底层通用 `error` 提供了带有 `int errorCode` 的重载版本，而 `badRequest`、`forbidden`、`notFound`、`methodNotAllowed`、`internalServerError` 等高频响应方法均缺乏对 `errorCode` 的直接重载支持。
3. **错误代码规范说明模糊**：既有 `docs/errorCode.md` 较为简陋，且开头存在“8 位”但示例实为 6 位（`131301`）的位长矛盾，缺少完整的位权定义、来源判定矩阵、枚举字典、前端拦截联动策略及全生命周期治理准则。

---

## 2. 具体改动点

### 2.1 后端工程：`ApiResponseEntity.java` 扩展全量 `errorCode` 重载

在 `com.humolix.volur.framework.common.api.ApiResponseEntity` 中新增并补齐了全套基于 `errorCode` 的重载工厂方法：
- **`badRequest`**：
  - `badRequest(int errorCode, String messageKey)`
  - `badRequest(int errorCode, String messageKey, T data)`
- **`forbidden`**：
  - `forbidden(int errorCode, String messageKey)`
- **`notFound`**：
  - `notFound(int errorCode, String messageKey)`
- **`methodNotAllowed`**：
  - `methodNotAllowed(int errorCode, String messageKey)`
- **`conflict`**（新增补齐 HTTP 409 常用状态）：
  - `conflict(String messageKey)`
  - `conflict(int errorCode, String messageKey)`
- **`tooManyRequests`**（新增补齐 HTTP 429 常用限频状态）：
  - `tooManyRequests(String messageKey)`
  - `tooManyRequests(int errorCode, String messageKey)`
- **`error` / `internalServerError`**：
  - `error(int errorCode, String messageKey)`
  - `internalServerError(int errorCode, String messageKey)`

所有重载方法均严格遵循无状态不可变原则，底层统一调用 `of(status, errorCode, messageKey, data)`。

### 2.2 单元测试覆盖：`ApiResponseEntityTest.java`

在 `volur-framework` 测试目录中创建了 `ApiResponseEntityTest.java`，编写了涵盖 10 个测试用例的高覆盖度测试集，分别验证：
- `ok` 与 `success` 的默认 `errorCode = 0` 及返回值；
- `created` 资源创建返回；
- `badRequest`（含无 code、带 code、带附加 payload 重载）；
- `unauthorized`、`forbidden`、`notFound`、`methodNotAllowed`；
- `conflict`、`tooManyRequests`；
- `error` 与 `internalServerError`。

### 2.3 规范文档重构：`docs/errorCode.md`

将 `docs/errorCode.md` 彻底重构成一份结构严谨、内容完备的企业级工程规范，包含：
1. **设计目标与背景**：明确 HTTP 状态码与业务 ErrorCode 的职责边界。
2. **位权规范（6 位定长 A-B-C-D-EE）**：
   - 彻底澄清历史“8 位”表述为初期流水号预留设想，明确全系统正式统一以 **6 位定长整数** 为工程基准，保持与已上线的 `131301`~`131306` 100% 兼容。
3. **五大维度枚举字典**：
   - 第 1 位（来源）：明确 `1`（服务端核心处理/主动安全阻断）与 `2`（客户端显式调用契约违背）的判定准则；
   - 第 2 位（大类）：1-系统、2-业务、3-参数凭据、4-认证权限、5-资源状态、6-网络通信、7-第三方服务、8-其他未知；
   - 第 3 位（业务模块）：0-系统基础、1-认证安全、2-用户身份、3-通知邮件、4-容器靶机、5-竞赛天梯、6-赛题题库、7-数据统计、8-审计日志、9-扩展预留；
   - 第 4 位（严重等级）：1-致命、2-严重、3-一般、4-轻微；
   - 第 5~6 位（具体流水号）：01~99 递增分配。
4. **完整实装与规划编码字典**：完整收录 `TokenAuthenticationException` 体系及典型系统、用户、竞赛等场景编码。
5. **前端与后端集成范例**：展示了 Axios 响应拦截器如何利用 `errorCode` 优雅实现无感刷新、异地登录强弹窗下线、验证码自动刷新等联动。
6. **生命周期四大铁律**：唯一性、不可变性、零码即成功、禁止魔数散落。

---

## 3. 影响范围与兼容性说明

1. **完全向后兼容**：所有原有的 `ApiResponseEntity` 方法签名和调用链路未受任何影响，无破坏性变更（Breaking Change）。
2. **前端无感知升级**：原有前端依赖 `success` 布尔判定与 `message` 保持正常工作；前端拦截器后续可渐进式按需引入基于 `errorCode` 的精准流转逻辑。
3. **测试验证**：`volur-framework` 内部 115 个既有单元测试与新增的 10 个 `ApiResponseEntityTest` 单元测试全部通过。
