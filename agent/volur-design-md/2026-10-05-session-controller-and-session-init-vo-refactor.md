# SessionController 规范重命名与 SessionInitVO 强校验架构重构施工记录

- **施工日期**：2026-10-05
- **施工模块**：
  - 后端服务：`volur-security` (`controller/v1`, `service`, `domain/vo`)、`volur-auth` (`config`, `filter`, `test`)
  - 前端系统：`volur-lite-view-NEXT` (`api`, `types`, `store/modules/flow`)
- **执行规范**：严格遵循 `GEMINI.md`、`Rules.md`、`view-NEXT-rules.md` 之零注释铁律与单一真实源原则。

---

## 1. 变更背景与架构目标

在 Google Accounts 认证体系的领域建模演进中，针对全站会话初始化的职责分层提出了进一步明确的要求：
1. **聚合根语义归正**：客户端访问 `/initSess` 初始化的真实实体是安全上下文会话（Session），而非具体的单一业务流。原 `FlowController` 与 `FlowInitVO` 命名存在概念偏差，需统一归正为 `SessionController` 与 `SessionInitVO`；
2. **零 Mock 与全链路快速失败（Fast-Fail）**：
   - 过去 `initSess` 接口中，`flowName` 与 `flowEntry` 带有隐式默认值（`defaultValue = "VlifWebLogin"`），`fp`（设备指纹）与 `t`（客户端时间戳）为可选字段；
   - 这导致客户端探针损坏或调用方发起无参探测时，服务端隐式兜底创建了缺省会话，违背了零 Mock 与快速失败原则；
   - 本次重构将 `flowName`、`flowEntry`、`fp`、`t` 提升为**必须（Required）入参**，一旦缺失直接快速失败拒绝，杜绝隐式隐匿意图。

---

## 2. 核心改动清单

### 2.1 后端领域模型与控制层 (`volur-security`)
1. **新建并迁移视图对象 [`SessionInitVO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/domain/vo/SessionInitVO.java)**：
   - 物理删除原 `FlowInitVO.java`；
   - 新建 `SessionInitVO`，承载 `dsh`、`tl`、`flowName`、`flowEntry`、`cid`、`fp`、`serverTime`。
2. **服务层重构 [`SessionService.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/service/SessionService.java) 与 [`SessionServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/service/impl/SessionServiceImpl.java)**：
   - 物理删除原 `FlowService.java` 与 `FlowServiceImpl.java`；
   - 强校验 `flowName`、`flowEntry`、`fp`、`t` 必填性，缺失时抛出 `HttpStatus.BAD_REQUEST` 业务异常。
3. **新建并规范控制层 [`SessionController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/controller/v1/SessionController.java)**：
   - 物理删除原 `FlowController.java`；
   - 路径统一声明为 `@RequestMapping("/api/v1/session")`；
   - 端点 `GET /initSess` 参数配置为强校验：`@RequestParam String flowName`、`@RequestParam String flowEntry`、`@RequestParam String fp`、`@RequestParam Long t`、`@RequestParam(required = false) String dsh`；
   - 返回 `ApiResponseEntity<SessionInitVO>`。
4. **单元测试迁移 [`SessionControllerTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/test/java/com/humolix/volur/security/SessionControllerTest.java)**：
   - 物理替换原 `FlowControllerTest.java`，100% 覆盖重构后接口。

### 2.2 授权白名单与维护模式过滤器 (`volur-auth`)
1. **[`SecurityWhitelistProperties.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/config/SecurityWhitelistProperties.java)**：
   - 将原 `"/api/v1/flow/**"` 全链路平滑更替为 `"/api/v1/session/**"`；
2. **[`MaintenanceModeFilter.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/security/filter/MaintenanceModeFilter.java)**：
   - 豁免路径同步更新为 `"/api/v1/session/**"`；
3. **[`SecurityWhitelistMatcherTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/test/java/com/humolix/volur/auth/security/SecurityWhitelistMatcherTest.java)**：
   - 单元测试断言更新为验证 `"/api/v1/session/initSess"`。

### 2.3 新一代前端同步重构 (`volur-lite-view-NEXT`)
1. **[`src/types/auth/flow.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/types/auth/flow.ts)**：
   - `FlowInitVO` 重命名为 `SessionInitVO`；
   - `FlowInitParams` 重命名为 `SessionInitParams`，`flowName`、`flowEntry`、`fp`、`t` 显式设为 `required` 属性；
2. **[`src/api/auth.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/api/auth.ts)**：
   - 封装强类型 `initSessionApi(params: SessionInitParams): Promise<ApiResponse<SessionInitVO>>`，请求路径改为 `'/session/initSess'`；
3. **Pinia 流程 Store 同步**：
   - [`signinFlow.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/store/modules/flow/signinFlow.ts) 与 [`signupFlow.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/store/modules/flow/signupFlow.ts) 统一调用 `initSessionApi`。

---

## 3. 验证与回归测试

1. **后端全量编译与单测**：
   - 执行 `mvn test -pl volur-security,volur-auth`，29 项安全单测与 2 项白名单单测全部 `BUILD SUCCESS`。
2. **前端全量类型检查与打包**：
   - 执行 `npm run build`（`vue-tsc --noEmit && vite build`），0 报错 0 告警，前端产物打包成功。
