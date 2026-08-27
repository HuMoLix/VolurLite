# 全局异常统一处理与分页工具链强化落地

## 1. 变更背景与需求

1. **统一全局异常处理体系**：参考 `VolurCTF` 生产级异常拦截与响应规范，为 Volur Lite 构建标准化的全局异常拦截中心 [`GlobalExceptionHandler`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/exception/handler/GlobalExceptionHandler.java)，统一错误响应格式，避免向外部泄露内部异常堆栈细节。
2. **企业级分页与排序工具链完善**：参考 `PageSortUtil` 与 `PageTokenUtil`，实现高并发防 SQL 注入的动态排序校验工具 [`PageSortUtil`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/util/PageSortUtil.java) 以及基于 Base64 URL 编解码的游标分页令牌工具 [`PageTokenUtil`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/util/PageTokenUtil.java)。

---

## 2. 核心改动点

### 2.1 全局异常拦截器 (`GlobalExceptionHandler`)
在 `volur-framework` 模块的 `com.humolix.volur.framework.common.exception.handler` 包下新增 [`GlobalExceptionHandler`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/exception/handler/GlobalExceptionHandler.java)：
- **入参及约束校验异常**：
  - `MethodArgumentNotValidException`：提取 `FieldError` 拼接具体字段校验提示信息（400）；
  - `BindException` & `ConstraintViolationException`：规范化参数绑定与约束校验失败提示（400）；
  - `HttpMessageNotReadableException`：拦截请求体解析与 JSON 反序列化异常（400）；
  - `MissingServletRequestParameterException` & `MethodArgumentTypeMismatchException`：拦截 Query/Path 参数缺失与类型不匹配（400）；
- **安全与权限异常**：
  - `BadCredentialsException`：拦截凭据错误并返回 401；
  - `AccessDeniedException` & `AuthorizationDeniedException`：拦截权限不足并返回 403；
- **网络与上传异常**：
  - `MultipartException` & `MaxUploadSizeExceededException`：处理文件上传异常（400）；
  - `ClientAbortException`：处理客户端主动断开连接（400）；
- **路由与资源异常**：
  - `NoResourceFoundException`：处理 404 资源未找到；
  - `HttpRequestMethodNotSupportedException`：处理 405 不支持的 HTTP 请求方法；
- **业务与系统异常**：
  - `BusinessException`：精准提取状态码 `status`、错误码 `errorCode` 及消息并包装为 `ApiResponse`；
  - `IllegalArgumentException` / `IllegalStateException`：拦截非法参数与状态（400）；
  - `MailException`：统一拦截邮件服务异常并返回友好提示（500）；
  - `Exception`：兜底捕获并记录 ERROR 日志，向客户端返回友好错误提示（500）。

### 2.2 响应工厂增强 (`ApiResponseEntity`)
- 新增 `methodNotAllowed(String messageKey)`；
- 新增 `error(HttpStatusCode status, int errorCode, String messageKey)` 支持自定义业务错误码响应；
- 严格遵循全小写驼峰工厂方法规范。

### 2.3 分页与游标工具链 (`PageSortUtil` & `PageTokenUtil`)
1. **`PageSortUtil`**：
   - 基于 Caffeine 进程内高性能缓存实体合法字段树（`ENTITY_VALID_FIELDS_CACHE`，最大 500 容量）；
   - 递归提取实体字段（深度最大为 2），智能过滤 `static`、`transient`、`@Transient`、集合与映射类型；
   - 支持多字段排序（逗号分隔，如 `"sortField=username,score"`）与防 SQL 注入校验（非法字段自动降级回退默认字段或 unsorted）；
   - 规范分页参数区间（默认 10，最大 200，页码从 1 自适应转 0-indexed）。
2. **`PageTokenUtil`**：
   - 提供 `CursorPayload` 模型（`sortValue` + `uniqueId`）；
   - 提供 URL 安全的 Base64 JSON 序列化与反序列化工具方法（`encodeToken`、`decodeToken`），用于支持流式无限滚动游标分页 [`CursorResponse<T>`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/api/CursorResponse.java)。
3. **`SortUtils` 兼容性收敛**：
   - 重构既有 [`SortUtils`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/util/SortUtils.java)，所有方法全面委托给 `PageSortUtil`，保证既有代码无缝兼容。

---

## 3. 单元测试与验证结果

新增 3 个单元测试类并完成全平台测试覆盖：
- [`GlobalExceptionHandlerTest`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/test/java/com/humolix/volur/framework/common/exception/handler/GlobalExceptionHandlerTest.java)
- [`PageSortUtilTest`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/test/java/com/humolix/volur/framework/common/util/PageSortUtilTest.java)
- [`PageTokenUtilTest`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/test/java/com/humolix/volur/framework/common/util/PageTokenUtilTest.java)

执行 `mvn test`：全平台 12 个模块全部编译测试通过（12/12 SUCCESS，0 错误 0 失败）。
