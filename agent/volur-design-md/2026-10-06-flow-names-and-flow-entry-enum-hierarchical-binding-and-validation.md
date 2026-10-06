# FlowNames 与 FlowEntry 层级枚举强绑定与全链路快速失败校验规范

## 1. 变更背景与架构动机

在 Volur 认证与安全状态机架构中，认证流程通过 `flowName`（流程名称，如 `VlifWebLogin`、`VlifWebSignUp`）与 `flowEntry`（流程入口，如 `WebServiceLogin`、`WebSignUp`）进行区分与端到端上下文隔离。

在此前的实现中：
1. `flowName` 与 `flowEntry` 散落为无强约束的字符串字面量（Raw String Literals）；
2. 缺乏层级隶属约束：系统未能在接口入口层校验传入的 `flowName` 是否为平台已知受支持的认证工作流，也未能校验传入的 `flowEntry` 是否从属于该具体的 `flowName`；
3. 存在跨流伪造（Cross-Flow Forgery）与越权跳步隐患：恶意攻击者可能通过构造合法的 `flowName` 搭配非法的或跨流程的 `flowEntry`（如在 `VlifWebLogin` 中注入 `WebSignUp`），绕过不同工作流的边界隔离或导致下游密钥派生与状态机紊乱。

为了实现端到端强类型约束、层级归属明确性与快速失败（Fail-Fast），在 `com.humolix.volur.security.flow.FlowNames` 中全面实现具备层级从属绑定的双向枚举体系，并在全平台关键节点强制执行归属校验。

---

## 2. 核心架构设计

### 2.1 枚举模型设计与层级归属绑定

`FlowNames` 定义为工作流主枚举，并在其内部定义高内聚子枚举 `FlowEntry`：

```
FlowNames (主流程枚举)
  ├── VLIF_WEB_LOGIN ("VlifWebLogin")
  │     └── entries: [ WEB_SERVICE_LOGIN ("WebServiceLogin") ]
  └── VLIF_WEB_SIGNUP ("VlifWebSignUp")
        └── entries: [ WEB_SIGN_UP ("WebSignUp") ]
```

#### 特性设计：
1. **多入口支持与可扩展性**：`FlowNames` 构造器支持变长入参 `FlowEntry... entries`，底层使用不可变有序集合 `Collections.unmodifiableSet(new LinkedHashSet<>(...))` 维护，各流支持单入口或多入口拓展。
2. **双向导航与层级溯源**：
   - `FlowNames` 可通过 `hasEntry(...)`、`findEntry(...)` 检索并校验归属于本流程的 `FlowEntry`；
   - `FlowEntry` 内部反向持有其所属的 `flowNames` 实例（类加载时由静态代码块安全注入），并提供 `getFlowNames()` 与全局反向解析 `FlowNames.fromEntry(...)`，确保入口与流程的双向溯源。
3. **默认入口安全提取**：提供 `getDefaultEntry()`，方便单入口场景直接获取标准入口。

### 2.2 严格快速失败与校验判定 API

遵循 `Rules.md` 规范：
- 校验判定类方法强制返回基本类型 `boolean`（`isValidFlowName(String)`、`isValid(String, String)`、`FlowEntry.isValid(String)`）；
- 实体获取/解析类方法严格快速失败：当 `flowName` 未知或不匹配时抛出 `HttpStatus.BAD_REQUEST` 的 `BusinessException`；
- 联合校验方法 `FlowNames.validate(String flowName, String flowEntry)`：
  - 校验 `flowName` 是否属于 `FlowNames`；
  - 校验 `flowEntry` 是否属于该 `flowName` 下声明的合法入口集合；
  - 任意一项不满足均直接抛出强类型业务异常，阻断非法请求。

---

## 3. 改动影响与集成点

### 3.1 核心枚举与测试套件
1. **`FlowNames.java`** (`volur-security`)：
   - 实现包含 `VLIF_WEB_LOGIN`、`VLIF_WEB_SIGNUP` 及其绑定的 `FlowEntry`。
   - 彻底零行内注释，保持纯净自解释代码风格。
2. **`FlowNamesTest.java`** (`volur-security`)：
   - 编写完整的单元测试矩阵（11 个测试用例）：覆盖有效/无效流程名解析、空值边界、大小写容错、入口合法性、归属校验、跨流非法入口阻断、双向层级关系。

### 3.2 服务层与会话管理器深度集成
1. **`SessionServiceImpl.java` (`initSess`)**：
   - 在会话初始化入口第一时间调用 `FlowNames.validate(requestDTO.getFlowName(), requestDTO.getFlowEntry())`；
   - 杜绝任意非法或越权流派生 AES 密钥或创建未授权会话。
2. **`SessionManager.java` (`createSession` & `recoverySession`)**：
   - `createSession` 强制通过 `FlowNames.validate` 解析标准规范化流名与入口；
   - `recoverySession` 对传入的更新流名/入口执行归属校验，杜绝非法跨流状态污染；
   - 移除引发重载歧义的冗余方法，保持精炼稳定的 API 签名。
3. **`TransitionLockManager.java`**：
   - 将原硬编码字符串 `"VlifWebLogin"` 替换为 `FlowNames.VLIF_WEB_LOGIN.getFlowName()`。
4. **`SigninServiceImpl.java`**：
   - `identifier` 与 `challenge` 中的流名与入口统一从 `FlowNames` 常量中获取，消除魔术字符串。
5. **`SignupServiceImpl.java`**：
   - `name` 与 `password` 中的流名与入口统一从 `FlowNames` 常量中获取。

---

## 4. 验证结果与质量保证

1. **后端单元测试全量通过**：
   - `volur-security`：59 个单元测试用例全部通过（涵盖 `FlowNamesTest`、`SessionManagerTest`、`TransitionLockManagerTest`、`DeviceSessionHashGeneratorTest`、`SessionServiceImplTest` 等）；
   - `volur-auth`：54 个测试用例全部通过（涵盖跨模块联动校验、密码加密流、Filter 链路等）；
   - 平台全模块测试编译（13 个模块）0 告警 0 错误编译通过。
2. **前端构建验证**：
   - `src/volur-lite-view-NEXT` 执行 `npm run build`（`vue-tsc --noEmit && vite build`）耗时 6.29s 成功构建，类型系统与后端状态机流名称完全保持一致。
