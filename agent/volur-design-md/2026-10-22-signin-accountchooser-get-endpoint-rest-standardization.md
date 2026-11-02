# 登录账号选择器端点 RESTful 规范化改造报告 (`GET /accountchooser`)

## 1. 变更背景与架构动机

在原有的身份认证流设计中，客户端获取/探测已记住账号列表的接口采用了 RPC 风格的 `POST /api/v1/auth/signin/accountchooser/probe`。

根据 RESTful 架构准则与 Volur 平台统一编码规范：
1. **语义与幂等性对齐**：账号列表探测及当前会话探测属于只读、幂等的查询操作，不应对服务器持久化状态产生副作用，语义上应当使用 HTTP `GET` 动词；
2. **端点路径与动词规范化**：废除动作子路径 `/probe`，将资源路径简化收敛为标准名词路径 `/api/v1/auth/signin/accountchooser`；
3. **方法命名一致性**：控制器与服务层方法由 `probeAccounts` 统一规范更名为符合 CRUD 标准查询语义的 `getAccounts`。

---

## 2. 核心改动清单与实现细节

### 2.1 控制器层 (`volur-auth`)
- **文件**：[`SigninController.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/controller/v1/SigninController.java)
- **改动点**：
  - 将端点注解由 `@PostMapping("/accountchooser/probe")` 调整为 `@GetMapping("/accountchooser")`；
  - 函数名由 `probeAccounts` 重命名为 `getAccounts`；
  - 入参由 `@Valid @RequestBody AccountChooserProbeRequestDTO request` 调整为 `@Valid AccountChooserProbeRequestDTO request`，遵循 Thin Controller 规范，由 Spring MVC 自动将 Query String 参数绑定至强类型 DTO 实体中；
  - 响应消息规范更新为 `"获取账号列表成功"`。

### 2.2 服务契约与业务实现层 (`volur-auth`)
- **接口文件**：[`SigninService.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/SigninService.java)
  - 声明 `AccountChooserProbeResponseVO getAccounts(AccountChooserProbeRequestDTO request)`；
  - 新增默认无参重载 `default AccountChooserProbeResponseVO getAccounts() { return getAccounts(new AccountChooserProbeRequestDTO()); }`，便于在未携带标识时快速查询当前已登录会话。
- **实现文件**：[`SigninServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/SigninServiceImpl.java)
  - 实现 `getAccounts(AccountChooserProbeRequestDTO request)` 与 `getAccounts()` 重载；
  - 内部逻辑保持不变，安全检测当前上下文认证状态并批量查询匹配用户与用户画像。

### 2.3 单元测试覆盖 (`volur-auth`)
- **控制器单元测试**：[`SigninControllerTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/test/java/com/humolix/volur/auth/controller/v1/SigninControllerTest.java)
  - 新增 `getAccounts_shouldReturnOkResponseEntityWithAccounts` 测试用例，验证控制器响应状态码、消息文本及视图数据包装。
- **服务层单元测试**：[`SigninServiceImplTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/test/java/com/humolix/volur/auth/service/impl/SigninServiceImplTest.java)
  - 更新测试方法为 `getAccounts_withExistingAndNonExistingUsers_shouldReturnStatusAndDetails`；
  - 新增 `getAccounts_withoutParams_shouldReturnEmptyAccountsWhenUnauthenticated` 校验无参查询场景。

### 2.4 前端协同与彻底重构 (`volur-lite-view-NEXT`)
遵循 GEMINI 规范中“API 请求路径与 HTTP 动词严格 1:1 对齐 Controller”及“重构彻底性铁律”：
- **请求客户端配置**：[`request.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view-NEXT/src/utils/request.ts)
  - 在 Axios 实例中配置 `paramsSerializer: { indexes: null }`，确保数组类型查询参数以标准 `identifiers=a&identifiers=b` 形式传递，与 Spring MVC 保持零配置兼容。
- **API 请求封装**：[`api/auth.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view-NEXT/src/api/auth.ts)
  - 将 `probeAccountChooserApi` 重构为 `getAccountChooserApi`，使用 `get<AccountChooserProbeResponseVO>('/auth/signin/accountchooser', { params, skipToast: true })`。
- **视图组件**：[`AccountChooserView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/AccountChooserView.vue)
  - 同步修改调用处为 `getAccountChooserApi`。

---

## 3. 验证与审计结果

1. **后端平台测试**：
   - 执行 `mvn test -pl volur-auth -am -Dtest="com.humolix.volur.auth.**.*Test"`；
   - 运行结果：64 项单元测试全部通过（0 Failures, 0 Errors）。
2. **前端静态检查与打包构建**：
   - 执行 `npm run build`（包含 `vue-tsc --noEmit` 与 Vite 生产打包）；
   - 运行结果：所有模块类型检查通过并成功构建。
