# Volur Lite 业务模块系统审计日志 (@Syslog) 管理与集成施工文档

## 1. 变更背景与设计动机

在分布式与单体混合架构的 CTF 竞赛平台中，系统操作审计与关键安全事件追踪至关重要。为了满足安全合规、用户行为审计以及异常回溯需求，平台在 `volur-logging` 模块中提供了 `@Syslog` 注解及切面拦截器 (`SyslogAspect`)。

本次任务将 `@Syslog` 注解精准、合理地应用到以下 5 个核心业务模块的控制器层：
- `volur-auth` (认证与安全会话)
- `volur-challenge` (题库与赛题管理)
- `volur-competition` (多阶段竞赛与战队协同)
- `volur-container` (动态靶机与沙箱容器)
- `volur-verification` (安全核验与邮件通知)

### 核心设计原则 (精确过滤与高频排除)
1. **状态变更与重要业务审计**：仅针对具有状态修改（增删改、挂载/卸载、启动/销毁/延期、队伍资质审核、答卷与 Flag 提交）及核心安全认证（注册、登录、登出、修改密码、注销账号）的行为进行审计记录。
2. **高频只读与轮询接口全面排除**：
   - 严禁对普通的 GET 查询接口（题目列表、详情、排行榜、关于规则、赛题分类查询等）添加日志，避免高并发场景下的日志风暴与数据库存储膨胀。
   - 明确排除高频轮询及无状态变更接口，如 `TokenController.refreshToken`（用户明确要求不记录）、人机验证验证码渲染与场景拉取（`CaptchaController.getCaptcha` / `getCaptchaChallenge`）等。
3. **安全凭据与敏感入参保护 (OWASP Compliance)**：
   - 涉及密码明文、Token 凭证的敏感操作（用户注册、登录、通过 Token 或验证码重置密码、修改密码、核验修改密码凭据等），强制声明 `recordParams = false` 和 `recordResponse = false`，杜绝将明文密码或鉴权 Token 持久化至数据库日志表中造成敏感信息泄露。

---

## 2. 详细改动清单与模块配置

### 2.1 依赖管理更新 (`pom.xml`)
在各模块的 `pom.xml` 中引入 `volur-logging` 模块依赖：
- `volur-auth/pom.xml`
- `volur-challenge/pom.xml`
- `volur-competition/pom.xml`
- `volur-container/pom.xml`
- `volur-verification/pom.xml`

### 2.2 `volur-auth` 模块
- `AuthController.java`:
  - `register`: `@Syslog(module = "AUTH", value = "用户注册", logType = "AUTH", recordParams = false)`
  - `login`: `@Syslog(module = "AUTH", value = "用户登录", logType = "AUTH", recordParams = false, recordResponse = false)`
  - `forgotPassword`: `@Syslog(module = "AUTH", value = "发起找回密码申请", logType = "AUTH", recordParams = true)`
  - `resetPasswordByToken`: `@Syslog(module = "AUTH", value = "根据令牌重置密码", logType = "AUTH", recordParams = false)`
  - `resetPasswordConfirm`: `@Syslog(module = "AUTH", value = "根据邮箱验证码重置密码", logType = "AUTH", recordParams = false)`
  - `sendChangePasswordEmail`: `@Syslog(module = "AUTH", value = "发送修改密码邮箱验证码", logType = "AUTH", recordParams = true)`
  - `verifyChangePasswordCode`: `@Syslog(module = "AUTH", value = "核验修改密码验证码", logType = "AUTH", recordParams = false, recordResponse = false)`
  - `changePassword`: `@Syslog(module = "AUTH", value = "修改当前用户密码", logType = "AUTH", recordParams = false)`
  - `deleteAccount`: `@Syslog(module = "AUTH", value = "注销当前用户账号", logType = "AUTH")`
- `TokenController.java`:
  - `logout`: `@Syslog(module = "AUTH", value = "用户登出", logType = "AUTH")`
  - `refreshToken`: **保持不记录日志**（遵照用户指令与高频排除原则）。

### 2.3 `volur-challenge` 模块
- `AdminCtfCategoryController.java`:
  - `createCategory`: `@Syslog(module = "CHALLENGE", value = "创建 CTF 赛题分类", recordParams = true)`
  - `updateCategory`: `@Syslog(module = "CHALLENGE", value = "更新 CTF 赛题分类", recordParams = true)`
  - `deleteCategory`: `@Syslog(module = "CHALLENGE", value = "删除 CTF 赛题分类", recordParams = true)`
- `AdminCtfChallengeController.java`:
  - `createChallenge`: `@Syslog(module = "CHALLENGE", value = "创建 CTF 题目", recordParams = true)`
  - `updateChallenge`: `@Syslog(module = "CHALLENGE", value = "更新 CTF 题目", recordParams = true)`
  - `deleteChallenge`: `@Syslog(module = "CHALLENGE", value = "删除 CTF 题目", recordParams = true)`
- `AdminAwdChallengeController.java`:
  - `createChallenge`: `@Syslog(module = "CHALLENGE", value = "创建 AWD 题目", recordParams = true)`
  - `updateChallenge`: `@Syslog(module = "CHALLENGE", value = "更新 AWD 题目", recordParams = true)`
  - `deleteChallenge`: `@Syslog(module = "CHALLENGE", value = "删除 AWD 题目", recordParams = true)`
- `AdminAwdPlusChallengeController.java`:
  - `createChallenge`: `@Syslog(module = "CHALLENGE", value = "创建 AWD+ 题目", recordParams = true)`
  - `updateChallenge`: `@Syslog(module = "CHALLENGE", value = "更新 AWD+ 题目", recordParams = true)`
  - `deleteChallenge`: `@Syslog(module = "CHALLENGE", value = "删除 AWD+ 题目", recordParams = true)`
- `AdminIswChallengeController.java`:
  - `createChallenge`: `@Syslog(module = "CHALLENGE", value = "创建 ISW 题目", recordParams = true)`
  - `updateChallenge`: `@Syslog(module = "CHALLENGE", value = "更新 ISW 题目", recordParams = true)`
  - `deleteChallenge`: `@Syslog(module = "CHALLENGE", value = "删除 ISW 题目", recordParams = true)`
- `AdminTheoryChallengeController.java`:
  - `createChallenge`: `@Syslog(module = "CHALLENGE", value = "创建理论题目", recordParams = true)`
  - `updateChallenge`: `@Syslog(module = "CHALLENGE", value = "更新理论题目", recordParams = true)`
  - `deleteChallenge`: `@Syslog(module = "CHALLENGE", value = "删除理论题目", recordParams = true)`

### 2.4 `volur-competition` 模块
- `CompetitionController.java` & `CompetitionScoreboardController.java`:
  - 全为公开查询与天梯榜单读取接口，**不添加日志注解**。
- `CompetitionTeamController.java`:
  - `signupTeam`: `@Syslog(module = "COMPETITION", value = "创建队伍并报名竞赛", recordParams = true)`
  - `updateTeam`: `@Syslog(module = "COMPETITION", value = "更新参赛队伍信息", recordParams = true)`
  - `acceptInvitation`: `@Syslog(module = "COMPETITION", value = "接受竞赛组队邀请", recordParams = true)`
  - `rejectInvitation`: `@Syslog(module = "COMPETITION", value = "拒绝竞赛组队邀请", recordParams = true)`
  - `cancelInvitation`: `@Syslog(module = "COMPETITION", value = "取消队员组队邀请", recordParams = true)`
- `AdminCompetitionController.java`:
  - `createCompetition`: `@Syslog(module = "COMPETITION", value = "创建竞赛", recordParams = true)`
  - `updateCompetition`: `@Syslog(module = "COMPETITION", value = "更新竞赛信息", recordParams = true)`
  - `deleteCompetition`: `@Syslog(module = "COMPETITION", value = "删除竞赛", recordParams = true)`
  - `setFreezeScoreboard`: `@Syslog(module = "COMPETITION", value = "设置竞赛封榜状态", recordParams = true)`
- `AdminCompetitionTeamController.java`:
  - `auditTeam`: `@Syslog(module = "COMPETITION", value = "审核参赛队伍资质", recordParams = true)`
  - `deleteTeam`: `@Syslog(module = "COMPETITION", value = "删除参赛队伍", recordParams = true)`
- `AdminCtfMountController.java`:
  - `mountChallenge`: `@Syslog(module = "COMPETITION", value = "挂载题目到竞赛", recordParams = true)`
  - `batchMountChallenges`: `@Syslog(module = "COMPETITION", value = "批量挂载题目到竞赛", recordParams = true)`
  - `unmountChallenge`: `@Syslog(module = "COMPETITION", value = "从竞赛卸载赛题", recordParams = true)`
- `CtfStageController.java`:
  - `submitFlag`: `@Syslog(module = "COMPETITION", value = "提交 CTF 赛题 Flag", recordParams = true, recordResponse = true)`
  - `startInstance`: `@Syslog(module = "CONTAINER", value = "竞赛启动靶机容器", recordParams = true)`
  - `stopInstance`: `@Syslog(module = "CONTAINER", value = "竞赛销毁靶机容器", recordParams = true)`
  - `extendInstance`: `@Syslog(module = "CONTAINER", value = "竞赛延长靶机容器租期", recordParams = true)`
- `AwdStageController.java`:
  - `submitFlag`: `@Syslog(module = "COMPETITION", value = "提交 AWD 攻击 Flag", recordParams = true, recordResponse = true)`
- `AwdPlusStageController.java`:
  - `uploadPatch`: `@Syslog(module = "COMPETITION", value = "上传 AWD+ 靶机防御补丁", recordParams = true)`
- `IswStageController.java`:
  - `submitFlag`: `@Syslog(module = "COMPETITION", value = "提交 ISW 靶标 Flag", recordParams = true, recordResponse = true)`
- `TheoryStageController.java`:
  - `submitPaper`: `@Syslog(module = "COMPETITION", value = "提交理论考核试卷", recordParams = true, recordResponse = true)`

### 2.5 `volur-container` 模块
- `CtfInstanceController.java`:
  - `startInstance`: `@Syslog(module = "CONTAINER", value = "启动靶机容器", recordParams = true)`
  - `stopInstance`: `@Syslog(module = "CONTAINER", value = "销毁靶机容器", recordParams = true)`
  - `extendInstance`: `@Syslog(module = "CONTAINER", value = "延长靶机容器租期", recordParams = true)`

### 2.6 `volur-verification` 模块
- `CaptchaController.java`:
  - 属于高频人机挑战生成与快速凭证交换接口，**不添加日志注解**。
- `EmailController.java`:
  - `sendVerificationEmail`: `@Syslog(module = "VERIFICATION", value = "发送注册邮箱验证码", logType = "AUTH", recordParams = true)`
  - `testSmtpConnection`: `@Syslog(module = "VERIFICATION", value = "测试 SMTP 邮件连接", logType = "SYSTEM", recordParams = false)`

### 2.7 `volur-logging` DTO 完善与操作者/统计查询修复
- `AdminLogQueryDTO.java`:
  - 将 `@Schema` 描述中的模块枚举描述更新为包含 `VERIFICATION`。
- `AdminLogServiceImpl.java` & `SyslogRepository.java`（针对认证与鉴权统计条数重复翻倍问题的修复）：
  - **根因分析**：原统计方法中采用了 `countByModule("AUTH") + countByLogType("AUTH")` 的算术相加。当单条日志同时标记 `module = 'AUTH'` 和 `logType = 'AUTH'` 时，会被两个条件分别统计一次并相加，导致单条记录被重复计数 2 次（总条数 4 条时计算出 8 条）。
  - **修复方案**：在 Repository 中新增 `countByModuleOrLogType("AUTH", "AUTH")` 方法，使用底层数据库的逻辑或 (`OR`) 集合并集统计，杜绝重复计算。
- `SyslogAspect.java`（针对登录与登出操作者为 ANONYMOUS 问题的修复）：
  - **前置捕获操作者 (`preOperator`)**：在 `joinPoint.proceed()` 执行前捕获 `SecurityContextHolder` 中的身份。解决登出操作中服务层提前清空上下文 (`SecurityUtils.logout()`) 导致在 `finally` 阶段读取为空的问题。
  - **入参反射萃取 (`extractOperatorFromArgs`)**：若前后置上下文均为 ANONYMOUS（如登录接口 `login`、注册接口 `register`、忘记密码接口 `forgotPassword`），自动通过反射检测入参 DTO 的 `getUsername()` / `getAccount()` 属性，准确提取目标账户名称。
  - **JWT 请求头后备解析 (`extractOperatorFromJwt`)**：若上述方式未获取到，尝试从 HTTP `Authorization: Bearer <token>` 请求头解码 Payload 中的 `sub` 声明。

### 2.8 前端管理端系统日志视图 (`AdminLogView.vue`) 单元格排版优化
- 参考 [`AdminUserView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/admin/user/AdminUserView.vue) 的文字分层排版风格，重构 [`AdminLogView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/admin/log/AdminLogView.vue) 的「操作者」列：
  - 操作者名称（主标题）与 操作 IP（次标题）采用上下双行排版（`flex-direction: column`），紧凑垂直居中对齐；
  - 操作者名称字号 `13px`，字重 `600`，支持超长文本省略与 `title` 浮动提示；
  - 操作 IP 继承全局默认字体规范（取消显式 `font-family` 指定），字号 `11px`，颜色统一为 `#444444`。取消了头像渲染以保持日志表格的简洁与高信息密度。

---

## 3. 影响范围与安全说明

1. **零性能损耗**：
   - Syslog 切面基于异步持久化存储服务 (`syslogPersistenceService.saveLogAsync`) 执行，不阻塞关键 HTTP 请求主链路。
   - 过滤了所有的 GET 读操作及 JWT 刷新操作，不会带来无意义的 I/O 负担与数据库空间浪费。
2. **安全合规 (Zero Leaks)**：
   - 密码、验证码凭证等敏感信息已在入参记录处配置 `recordParams = false`，规避数据库泄漏风险。
   - 操作者字段在未认证/登录/登出等特殊生命周期中能准确获取真实用户名，不再出现误判为 ANONYMOUS 的情况。
