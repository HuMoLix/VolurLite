# 优化全局 SSE 客户端断开连接与 AsyncRequestNotUsableException 静默处理记录

**施工日期**：2026-08-27  
**施工模块**：
- `src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/exception/handler/GlobalExceptionHandler.java`
- `src/volur-lite-platform/volur-notification/src/main/java/com/humolix/volur/notification/sse/GlobalSseManager.java`  
**责任主体**：Antigravity AI Agent

---

## 1. 异常现象与日志报错

在浏览器关闭标签页、页面刷新或网络连接重连时，服务端日志抛出以下错误：
1. `AsyncRequestNotUsableException: Servlet container error notification for disconnected client`（底层为 `IOException: 你的主机中的软件中止了一个已建立的连接`）；
2. 随后 `GlobalExceptionHandler` 捕获该异常并尝试写入 JSON 格式的 `ApiResponse`，触发二次告警：
   `HttpMessageNotWritableException: No converter for [class com.humolix.volur.framework.common.api.ApiResponse] with preset Content-Type 'text/event-stream;charset=UTF-8'`。

---

## 2. 原因分析

1. **正常网络生命周期事件**：
   - 当客户端（浏览器）刷新页面、关闭窗口或导航切换路由时，浏览器会主动中止 SSE（Server-Sent Events）长连接的 TCP Socket。
   - 服务端在触发定时保活心跳（`sendHeartbeat`）或消息下发时，检测到 Socket 已被对端关闭，Tomcat 与 Spring Web 抛出 `AsyncRequestNotUsableException` / `ClientAbortException`。这是 HTTP 长连接场景下完全正常的网络事件。
2. **全局异常拦截器冲突**：
   - 此前 `GlobalExceptionHandler` 将 `AsyncRequestNotUsableException` 视为系统级未处理错误（`ERROR`），并尝试向该响应输出 JSON 格式的 `ApiResponse`。
   - 但 SSE 请求的响应头预设为 `text/event-stream`，Spring 内部缺少将其转换为 JSON 的序列化处理器，从而引发了 `HttpMessageNotWritableException`。

---

## 3. 优化与修复方案

1. **全局异常处理优化**：
   - 在 [`GlobalExceptionHandler.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/exception/handler/GlobalExceptionHandler.java) 中将 `AsyncRequestNotUsableException.class` 与 `ClientAbortException.class` 联合捕获；
   - 响应状态设为 `HttpStatus.NO_CONTENT`，返回值设为 `void`（避免尝试往 `text/event-stream` 写入 JSON）；
   - 日志降级为 `DEBUG` 级别，避免将正常的网络断开误报为系统故障。
2. **连接管理器安全清理**：
   - 在 [`GlobalSseManager.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-notification/src/main/java/com/humolix/volur/notification/sse/GlobalSseManager.java) 的 `removeEmitter` 中安全包裹 `emitter.complete()`，确保失效连接资源被干净释放。

---

## 4. 验证与审计

- **编译构建**：执行 `mvn compile -pl volur-framework,volur-notification -am`，编译通过（`BUILD SUCCESS`）。
- **优化效果**：页面刷新/关闭标签页时不再打印无意义的堆栈报错与格式转换告警，日志清晰整洁。
