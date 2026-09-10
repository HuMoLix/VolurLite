# 管理端邮件管理功能与全站发信审计落库实施记录

## 1. 变更背景与需求分析
平台原邮件发送模块（`volur-verification`）具备向用户发送验证码、密码重置邮件等功能，但存在以下管理与可观测性诉求：
1. **全站发信流水无记录**：系统异步发送或同步测试的邮件（注册验证码、密码重置、改密验证、注销账号、系统连通性测试等）未在数据库中持久化留存，管理员无法排查发信失败原因或审计发信历史。
2. **管理端缺少邮件管理控制台**：`AdminLayout` 侧边栏缺少邮件管理入口，管理员无法查看全站发信列表、查看邮件具体内容（含富文本 HTML 渲染）、排查投递异常。
3. **缺少管理员手动发信与失败重发能力**：运营或管理人员需要支持向指定邮箱手动发信（如官方通知、特殊通知），并针对历史投递失败的邮件一键重发。

---

## 2. 总体架构与前后端对齐方案

### 2.1 后端工程设计 (`volur-verification`)
- **数据持久化层**：
  - 新增实体 `EmailLog`（表 `email_log`），使用雪花算法生成主键 ID；
  - 核心字段：`id`、`recipient`（收件人）、`subject`（主题）、`content`（`LONGTEXT` 邮件富文本内容）、`type`（业务场景枚举）、`status`（`SUCCESS`/`FAILED`）、`errorMessage`（异常堆栈或失败原因）、`sender`（发信人/服务器）、`operatorId`、`operatorIp`、`createdAt`。
  - 新增 `EmailLogRepository`，继承 `JpaRepository` 与 `JpaSpecificationExecutor`，支持动态多条件组合分页检索与统计。
- **发信拦截与自动审计**：
  - 在 `MailTaskExecutor.executeAsyncMail()` 中全面织入异步发信持久化审计，保证系统所有业务触发的异步邮件（注册验证码、密码重置、改密、注销）均能自动记录在册；
  - 在 `EmailServiceImpl.sendEmail()` 中记录同步发信流水（测试邮件等）；
  - 邮件任务执行成功记录 `SUCCESS`，失败记录 `FAILED` 并持久化捕获的异常信息。
- **服务层与接口层**：
  - `AdminEmailService` / `AdminEmailServiceImpl`：
    - `getStatistics()`：统计总发信量、今日发信量、成功数、失败数；
    - `listEmails(AdminEmailQueryDTO)`：多维度检索（关键词、类型、状态、时间范围）并返回标准 `PageResponse<EmailLogVO>`；
    - `getEmailDetail(Long id)`：获取包含完整邮件富文本内容的详情；
    - `sendManualEmail(AdminSendEmailDTO, Long currentUserId)`：管理员手动发送邮件并记录流水（`type = "MANUAL"`）；
    - `resendEmail(Long id, Long currentUserId)`：读取历史邮件内容重新投递并追加重发审计记录。
  - `AdminEmailController`：
    - 路径遵循 Thin Controller 规范：`/api/v1/admin/emails`，方法遵循 `@GetMapping`、`@PostMapping` 严格对齐。
    - 统一返回 `ApiResponse<T>`，使用 `ApiResponseEntity` 工厂方法。

### 2.2 前端工程设计 (`volur-lite-view`)
- **类型定义层 (`src/types/verification/index.ts`)**：
  - 严格映射后端 DTO/VO：`EmailLogVO`、`AdminEmailStatisticsVO`、`AdminEmailQueryDTO`、`AdminSendEmailDTO`，杜绝 `any`。
- **API 请求层 (`src/api/verification/index.ts`)**：
  - `getAdminEmailStatisticsApi()` -> `GET /api/v1/admin/emails/statistics`
  - `getAdminEmailListApi(params)` -> `GET /api/v1/admin/emails`
  - `getAdminEmailDetailApi(id)` -> `GET /api/v1/admin/emails/{id}`
  - `sendAdminManualEmailApi(data)` -> `POST /api/v1/admin/emails/send`
  - `resendAdminEmailApi(id)` -> `POST /api/v1/admin/emails/{id}/resend`
- **路由与菜单集成**：
  - `AdminLayout.vue` 侧边栏引入 `MailOutline` 图标并挂载 `AdminEmails` 菜单；
  - `src/router/routes.ts` 注册 `/admin/emails` 路由。
- **视图层 (`src/views/admin/email/AdminEmailView.vue`)**：
  - **顶部指标看板**：展示总发信量、今日发送量、成功送达量、失败量；
  - **检索过滤栏**：支持收件人/主题/IP 模糊搜索、邮件类型筛选、发送状态筛选、时间范围选择，支持一键查询与重置；
  - **发信记录表格**：呈现 ID、收件人、主题、业务类型、发信地址、状态、发送时间及操作列；
  - **详情审计抽屉**：以 Descriptions 表格展示完整元数据；支持富文本渲染（使用 `iframe` 容器安全沙箱隔离渲染，防止全局 CSS 污染）与源码查看/复制两种视图；
  - **手动发信弹窗**：提供收件人、主题、正文编辑（支持 HTML/纯文本）、实时预览沙箱及严格的表单校验。
- **国际化多语言支持**：
  - `src/locales/zh_CN/admin/emails.json` 与 `src/locales/en_US/admin/emails.json` 完整支持中英文切换。

---

## 3. 核心改动文件清单

### 后端平台
1. `src/volur-lite-platform/volur-verification/pom.xml`：引入 JPA 与 MapStruct 支持。
2. `src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/domain/entity/EmailLog.java`：邮件日志持久化实体。
3. `src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/repository/EmailLogRepository.java`：邮件日志数据访问层。
4. `src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/domain/vo/EmailLogVO.java`：邮件日志视图对象。
5. `src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/domain/vo/AdminEmailStatisticsVO.java`：发信统计指标对象。
6. `src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/domain/dto/AdminEmailQueryDTO.java`：分页查询检索入参。
7. `src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/domain/dto/AdminSendEmailDTO.java`：手动发信入参校验。
8. `src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/mapper/EmailLogMapper.java`：MapStruct 对象映射接口。
9. `src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/service/admin/AdminEmailService.java`：管理服务接口。
10. `src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/service/admin/impl/AdminEmailServiceImpl.java`：管理服务实现。
11. `src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/controller/v1/admin/AdminEmailController.java`：管理端控制器。
12. `src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/mail/executor/MailTaskExecutor.java`：拦截记录异步发信流水。
13. `src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/service/impl/EmailServiceImpl.java`：记录同步发信流水。

### 前端视图
1. `src/volur-lite-view/src/types/verification/index.ts`：补充邮件领域模型。
2. `src/volur-lite-view/src/api/verification/index.ts`：对齐管理端邮件 5 大 API。
3. `src/volur-lite-view/src/layout/AdminLayout.vue`：侧边栏挂载邮件管理导航。
4. `src/volur-lite-view/src/router/routes.ts`：添加 `/admin/emails` 路由配置。
5. `src/volur-lite-view/src/views/admin/email/AdminEmailView.vue`：邮件管理全功能主视图。
6. `src/volur-lite-view/src/locales/zh_CN/layout.json` & `en_US/layout.json`：侧边栏菜单词条。
7. `src/volur-lite-view/src/locales/zh_CN/admin/emails.json` & `en_US/admin/emails.json`：管理端邮件中英文国际化语言包。

---

## 4. 验证结果
1. **后端编译与测试**：
   - 运行 `mvn test-compile` 及 `mvn compile`，整个 `volur-lite-platform` 12 个模块全部 `BUILD SUCCESS`。
   - `volur-verification` 单元测试全部通过。
2. **前端构建验证与异常排查**：
   - 修复 `vue-i18n` 消息编译器中保留字符 `@` 未转义导致的 `SyntaxError: Message compilation error: Invalid linked format` 异常：在 `emails.json` 中占位符将 `user@example.com` 严格转义为 `user{'@'}example.com`；
   - 运行 `npm run build`，`vue-tsc` 类型校验 0 错误，Vite 构建成功打包出 `AdminEmailView` 相关静态资源文件。
