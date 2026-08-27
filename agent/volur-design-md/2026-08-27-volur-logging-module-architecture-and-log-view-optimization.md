# Volur Lite 全局日志与审计系统 (volur-logging) 建设与 AdminLogView 排版优化记录

**施工日期**：2026-08-27  
**施工模块**：
- 后端：`src/volur-lite-platform/volur-logging` (新建独立模块), `src/volur-lite-platform/volur-system`, `src/volur-lite-platform/volur-notification`, `src/volur-lite-platform/volur-server`
- 前端：`src/volur-lite-view/src/views/admin/log/AdminLogView.vue`  
**责任主体**：Antigravity AI Agent

---

## 1. 变更背景与架构目标

1. **废弃临时旧审计注解**：
   - 彻底删除临时注解 `com.humolix.volur.framework.security.annotation.Audit`，解耦安全鉴权与业务审计。
2. **构建现代化审计日志模块 `volur-logging`**：
   - 参考 `VolurCTF` 的 `volur-logging` 架构与设计规范，建立 `volur-logging` 独立平台子模块；
   - 提供 `@Syslog` 切面声明式日志拦截与异步持久化机制，自动采集 `traceId`、操作人、IP、URI、请求方法、耗时、执行状态、入参载荷（支持大字段截断与敏感过滤）、响应结果与异常报错信息；
   - 暴露 `GET /api/v1/admin/logs`（多条件动态过滤与分页）、`GET /api/v1/admin/logs/statistics`（日志统计指标）与 `GET /api/v1/admin/logs/{id}`（详情）。
3. **前端日志视图排版优化 (`AdminLogView.vue`)**：
   - 严格参考 `AdminUserView.vue` 的设计风格，将顶部指标卡片重构为紧凑的单行 `.stat-ribbon` + `.stat-mini-card` 结构；
   - 统一间距（`16px`）、筛选表单样式、黑色主操作按钮（`#000000`/`#ffffff`）与表格字段排版；
   - 支持后端远程多字段动态排序（耗时、记录时间）与全链路抽屉审计详情渲染。

---

## 2. 核心改动点与实施细节

### 2.1 后端 `volur-logging` 模块架构设计
1. **Maven 模块规划**：
   - 在平台根 `pom.xml` 中将 `volur-logging` 加入 `<modules>` 与 `<dependencyManagement>`；
   - 在 `volur-server`、`volur-system`、`volur-notification` 中引入 `volur-logging` 依赖；
   - 配置 `maven-compiler-plugin` 绑定 Lombok 与 MapStruct 注解处理器。
2. **注解与切面设计**：
   - **`@Syslog`** (`annotation.Syslog`)：支持定义 `module`、`value`（操作描述）、`recordParams`、`recordResponse`、`logType`（`OPERATE`/`AUTH`/`SECURITY`/`SYSTEM`）；
   - **`SyslogAspect`** (`aspect.SyslogAspect`)：基于 `@Around("@annotation(syslog)")` 环绕切面，自动解析客户端 IP、User-Agent、链路追踪 ID 及登录上下文。
3. **异步持久化与领域模型**：
   - **实体类 `Syslog`**：对应表 `sys_log`，采用 `@SnowflakeGenerated` 自动生成主键，建立 `created_at`、`level, module`、`log_type`、`operator` 等核心索引；
   - **异步存储服务 `SyslogPersistenceService`**：使用 `@Async` 与 `Propagation.REQUIRES_NEW` 进行独立事务异步持久化，彻底杜绝日志记录对核心业务流程的阻塞。
4. **控制层与管理接口**：
   - **`AdminLogController`** (`controller/v1/admin`)：
     - `GET /api/v1/admin/logs`：支持按关键词、日志级别、业务模块、执行结果、起止时间区间等动态构建 Specification 查询与标准分页 `PageResponse<AdminLogVO>`；
     - `GET /api/v1/admin/logs/statistics`：实时统计总流水数、认证鉴权数、安全拦截数与系统异常数；
     - `GET /api/v1/admin/logs/{id}`：查询指定审计流水完整上下文。

### 2.2 前端视图层优化 (`AdminLogView.vue`)
1. **顶部指标卡片重构**：
   - 将原四列网格卡片重构为与 `AdminUserView.vue` 100% 对齐的 `.stat-ribbon` + `.stat-mini-card` 结构；
   - 包含四组关键指标：总日志流水条数、认证与鉴权记录、高危安全拦截、系统异常报错。
2. **筛选区与表格组件升级**：
   - 筛选栏统一支持模糊搜索、级别选择、模块选择、状态选择与时间范围拾取器；
   - 表格支持远程分页与动态排序，行操作提供“详情”按钮打开抽屉；
   - 抽屉内支持高亮展示请求 JSON 载荷、响应数据及异常堆栈详情块。

---

## 3. 验证与构建测试

1. **后端验证**：
   ```powershell
   mvn clean compile
   ```
   **结果**：Reactor 全部 12 个模块编译通过，`BUILD SUCCESS`（耗时 10.55s）。

2. **前端验证**：
   ```powershell
   npm run build
   ```
   **结果**：`vue-tsc` 0 错误，Vite 打包成功（耗时 5.16s）。

---

## 4. 影响范围与审计评估

- **改动文件清单**：
  - `src/volur-lite-platform/pom.xml`
  - `src/volur-lite-platform/volur-server/pom.xml`
  - `src/volur-lite-platform/volur-system/pom.xml`
  - `src/volur-lite-platform/volur-notification/pom.xml`
  - `src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/controller/v1/admin/AdminSystemSettingController.java`
  - `src/volur-lite-platform/volur-notification/src/main/java/com/humolix/volur/notification/controller/v1/admin/AdminBroadcastController.java`
  - `src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/security/annotation/Audit.java` (已删除)
  - `src/volur-lite-platform/volur-logging/**` (新建完整子模块)
  - `src/volur-lite-view/src/views/admin/log/AdminLogView.vue`
- **系统提升**：
  - 成功建立了全平台统一的 AOP 声明式审计日志总线与异步持久化机制；
  - 统一了后台所有模块（用户、广播、日志）的顶部指标卡片、过滤器与表格视觉语言。
