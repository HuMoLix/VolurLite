# 远程生产服务器 UTC 全链路迁移方案与前端时区架构设计

## 1. 变更背景与架构动机

在分析系统时间体系设计后确认，原系统在容器环境（`TZ=Asia/Shanghai`）、JVM（`-Duser.timezone=Asia/Shanghai`）、JDBC 驱动参数（`serverTimezone=Asia/Shanghai`）、Jackson 序列化配置（`GMT+8`）以及业务代码层（如 `ZoneId.of("Asia/Shanghai")`）深度绑定了 UTC+8 中国标准时间，且前端缺乏统一的时区转换工具层。

为了让系统无缝支持全球任意时区客户端访问，系统必须进行 UTC+00 全链路改造。然而，直接将运行中的生产系统改为 UTC 存在以下重大工程破坏风险：
1. **数据库历史数据 8 小时位移陷阱**：MySQL 中若有历史 `DATETIME` 列以 UTC+8 字面量存储，连接改为 UTC 后会被驱动误读为 UTC 绝对时间，导致所有历史业务时间凭空向未来跳跃 8 小时；
2. **进行中竞赛与定时任务崩溃**：比赛 `start_time` / `end_time` 若发生 8 小时偏移，状态机会误将进行中的比赛判定为已结束，导致正在解题的选手被强制踢出；容器实例 `expireAt` 被误判导致靶机被提前批量销毁；
3. **Redis 缓存反序列化与 TTL 紊乱**：Redis 中已缓存的旧版时间格式（`yyyy-MM-dd HH:mm:ss`）在升级为严格 ISO-8601 反序列化时会触发 `DateTimeParseException` 导致接口 500；
4. **客户端旧前端缓存断裂**：未刷新页面的用户运行旧前端代码，直接接收带 `Z` 后缀的标准 ISO 串，手写的非标准日期正则或切分逻辑可能抛出异常导致白屏；
5. **运营自然日统计位移**：统计“今日邮件数”、“今日封禁 IP 数”时，若机械改为 UTC 零点结算，中国运营人员的业务日结时间将变成早晨 08:00。

本方案针对远程生产环境制定安全平滑的迁移 SOP，并在本地启动第一阶段架构落地：构建前端标准时区核心库 `timezone.ts`。

---

## 2. 远程生产服务器迁移解决方案 (Standard Operating Procedure)

### 2.1 数据库历史数据核验与平滑清洗
1. **排查字段类型**：
   通过 `information_schema.COLUMNS` 审查所有涉及时间的核心业务表（`competition`, `user`, `ctf_challenge_instance`, `syslog`, `email_log` 等）。
2. **区分处理机制**：
   - **`TIMESTAMP` 列**：MySQL 内部已按 UTC 秒数存储，JDBC 切换为 `connectionTimeZone=UTC` 后读取结果不变，无需物理数据清洗；
   - **`DATETIME` 列**：若此前按 UTC+8 写入，必须在切换 JDBC 的同时通过幂等升级脚本执行 `CONVERT_TZ(col, '+08:00', '+00:00')` 进行平移校准。

### 2.2 后端 Jackson 序列化宽进严出规范
后端配置 `FlexibleInstantDeserializer`：
- **严出**：统一输出带 `Z` 后缀的 RFC 3339 / ISO-8601 UTC 字符串；
- **宽进**：反序列化时兼容 ISO-8601、旧版无时区裸串（`yyyy-MM-dd HH:mm:ss` 按 UTC 补偿解析）以及纯数字毫秒时间戳，确保旧 Redis 缓存与旧客户端请求不会报错。

### 2.3 解耦物理时间与平台业务时区
- 竞赛判定、容器存活、Token 过期等物理时间一律使用 `Instant.now()` 与 UTC 绝对时间比对；
- “今日统计”等自然日聚合逻辑，统一引入系统配置 `system.common.business_timezone`（默认为运营方指定时区，如 `Asia/Shanghai`），既保持数据底层纯净，又符合运营人员在当地的自然日结算直觉。

### 2.4 生产发布标准执行步骤 (SOP)
1. **低峰维护窗口**：避开正在进行的重大比赛期；
2. **全量数据冷备**：执行数据库全量快照与 `mysqldump` 备份；
3. **前端优先部署**：先行上线具备多时区解析能力的新版前端；
4. **Redis 业务缓存刷新**：清理旧版时间格式的实体缓存；
5. **数据库时区校准**：执行升级脚本并修改数据库连接为 `connectionTimeZone=UTC`；
6. **后端服务重启**：加载 `TZ=UTC` 容器环境变量与新配置。

---

## 3. 本地架构升级（阶段一）：前端核心时区工具库实现 (`timezone.ts`)

### 3.1 废除旧文件与命名纯洁性
按照平台规范彻底重构原则，彻底删除原 0 字节的 `src/volur-lite-view-NEXT/src/utils/time.ts`，不保留任何旧命名别名或兼容垫片，新建标准化模块 [`src/volur-lite-view-NEXT/src/utils/timezone.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view-NEXT/src/utils/timezone.ts)。

### 3.2 核心函数实现清单（收敛为 5 类标准函数）

根据精准需求收敛，`timezone.ts` 严格且仅向外导出以下 5 类函数，杜绝任何外部多余导出：

1. **获取客户端时区**：
   - `getTimeZone(): string`
   - 利用浏览器原生 `Intl.DateTimeFormat().resolvedOptions().timeZone` 获取 IANA 时区标识，异常时回退到 `'UTC'`。
2. **本地时间串与 UTC 互转**：
   - `date2utc(dateStr: string): string`：将本地时间字符串（如 `"2026-10-22 15:30:00"`）转换为标准 ISO-8601 UTC 字符串（`YYYY-MM-DDTHH:mm:ss.sssZ`）。
   - `utc2date(utcStr: string, pattern: string = 'YYYY-MM-DD HH:mm:ss'): string`：将 UTC 字符串（如 `"2026-10-22T07:30:00.000Z"`）转换为客户端本地时间字符串。
3. **时间戳与 UTC 互转**：
   - `ts2utc(ts: number | string): string`：将时间戳转换为标准 ISO-8601 UTC 字符串。
   - `utc2ts(utcStr: string): number`：将 UTC 字符串解析为绝对纪元毫秒时间戳。
4. **本地时间串与本地时间戳互转**：
   - `date2ts(dateStr: string): number`：将本地时间字符串解析为其对应的毫秒时间戳。
   - `ts2date(ts: number | string, pattern: string = 'YYYY-MM-DD HH:mm:ss'): string`：将毫秒时间戳格式化为本地时间字符串。彻底去除防御性的 `if (ts === null || ...)` 及假值 UI 字符（`'-'`）兜底，遵循函数单一职责与强类型规范。
5. **本地时间与世界标准时间核验（防篡改检测）**：
   - `isTimeVerify(): Promise<boolean>`
   - **纯粹无参判定函数**：无需调用方传入任何参考时间或容差参数，调用即自动执行世界标准时钟对齐校验。
   - **容差固定为 30 秒**：将时钟偏差容差严格设定为 30s（`30000ms`），精准捕获恶意倒拨或快进系统时钟的行为。
   - **时区安全保障**：世界标准时间校验基准一律以绝对 UTC 时间戳（Unix Epoch Milliseconds）为依据进行无偏对齐，确保全球任何时区用户（如伦敦 UTC+0、纽约 UTC-5、北京 UTC+8）均不会因本地时区差异导致误报。
   - **分级授时校验机制（外网优先 + 内网降级）**：
     - **第一级（公网世界标准授时源）**：优先请求权威公网时间校准服务器（`timeapi.io`、Cloudflare 标准授时节点）获取毫秒级世界标准时间；
     - **第二级（内网与离线环境自动回退）**：若在纯内网或专网比赛环境中，外网校准源连接超时或不可达，自动无缝降级为内网源站校准，向同源基座（`window.location.origin`）发送轻量级 HTTP `HEAD` 请求，提取服务端权威 `Date` 响应头进行时钟校准。

7. **内部高内聚辅助函数命名规范**：
   - 内部格式化函数由 `formatDate` 统一规范命名为 `toDate(date: Date, pattern: string): string`；
   - 内部 UTC 解析函数由 `parseUtcString` 统一规范命名为 `toUtc(utcStr: string): Date`；
   - 杜绝冗余前缀与修饰词，直击类型转换本质。

### 3.3 代码洁净度规范遵从
严格遵守代码零注释铁律，`timezone.ts` 源码中绝不包含任何说明性注释与冗余备注，所有设计逻辑收敛于本文档。

---

## 4. 后端基础设施与配置改造落地（阶段二）

### 4.1 数据库连接切换为 UTC
- **配置文件**：[`src/volur-lite-platform/volur-server/src/main/resources/application.yml`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-server/src/main/resources/application.yml)
- **改动点**：
  - 将 MySQL JDBC URL 中原有的 `serverTimezone=Asia/Shanghai` 替换为符合 MySQL Connector/J 现代标准的 `connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true`，强制 JDBC 客户端与 MySQL 会话以标准 UTC 运行。
  - 调整 `spring.jackson`：移除字面量格式 `date-format: yyyy-MM-dd HH:mm:ss`，将全局默认时区由 `time-zone: GMT+8` 调整为 `time-zone: UTC`。

### 4.2 JacksonConfig 纯粹化升级（彻底去除旧格式兜底）
- **配置文件**：[`src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/config/JacksonConfig.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/config/JacksonConfig.java)
- **改动点**：
  - 彻底移除 `SimpleDateFormat("yyyy-MM-dd HH:mm:ss")` 格式定义与未使用的导包；
  - 默认时区配置由 `Asia/Shanghai` 变更为 `UTC`（`TimeZone.getTimeZone("UTC")`）；
  - 严禁隐式兜底解析旧格式，所有 `Instant` 时间戳序列化/反序列化严格采用国际标准 RFC 3339 / ISO-8601 UTC（带 `Z` 标识），保持 API 契约的绝对纯粹与高精度。

### 4.3 容器模块 VO 残留注解彻底清除
- **涉及文件**：
  - [`AdminClusterImageDetailVO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/domain/vo/AdminClusterImageDetailVO.java)
  - [`AdminClusterNodeVO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/domain/vo/AdminClusterNodeVO.java)
  - [`AdminClusterImageVO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/domain/vo/AdminClusterImageVO.java)
- **改动点**：
  - 彻底移除全部 `@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")` 注解与未使用的 `JsonFormat` 导包，消除全库所有残留的本地时区序列化破坏点。

### 4.4 Docker 镜像系统时区与 JVM 启动参数同步更新
- **涉及文件**：
  - 后端 Dockerfile：[`src/volur-lite-platform/Dockerfile`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/Dockerfile)
  - 前端 Dockerfile：[`src/volur-lite-view-NEXT/Dockerfile`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view-NEXT/Dockerfile)
- **改动点**：
  - 后端 `ENV TZ=Asia/Shanghai` 与 JVM 参数 `-Duser.timezone=Asia/Shanghai` 统一变更为 `ENV TZ=UTC` 与 `-Duser.timezone=UTC`；
  - 前端 Nginx 运行底座 `ENV TZ=Asia/Shanghai` 统一变更为 `ENV TZ=UTC`。

### 4.5 核心物理时间全面锁定 Instant.now() 与绝对 UTC 时间戳比较
- **比赛开始/结束状态判定**：[`CompetitionStatusValidAspect.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/aspect/CompetitionStatusValidAspect.java) 严格使用 `Instant.now().isBefore(comp.getStartTime())`、`Instant.now().isAfter(comp.getEndTime())` 与数据库绝对时间戳比对；
- **Token 到期与签名校验**：[`JwtUtil.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/security/jwt/JwtUtil.java) 颁发与验证一律采用 `Instant.now()`；
- **容器靶机到期与自动销毁**：[`CtfInstanceServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/service/impl/CtfInstanceServiceImpl.java) 中的实例存活与清理调度器 `findByExpiresAtBeforeAndStatus(Instant.now(), ...)` 严格基于绝对时间戳计算；
- **业务操作日志记录**：[`CompetitionOperationLog.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/entity/CompetitionOperationLog.java) 与 [`Syslog.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-logging/src/main/java/com/humolix/volur/logging/domain/entity/Syslog.java) 的 `createdAt` 全面由 JPA 实体映射为绝对 `Instant`，排查与过滤按 `greaterThanOrEqualTo` 直接比对 UTC。
- **清除业务层残留的硬编码时区**：
  - `AdminEmailServiceImpl.java:98` 中将硬编码的 `ZoneId.of("Asia/Shanghai")` 调整为绝对 UTC 零点 `LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant()`；
  - `AdminRiskServiceImpl.java:334` 与 `AdminContainerServiceImpl.java:174` 将隐式 `ZoneId.systemDefault()` 显式收敛为 `ZoneOffset.UTC`；
  - `AdminCompetitionScoreExportServiceImpl.java:52` 与 `NotificationEventListener.java:27` 中的文本格式化器统一锚定 `ZoneOffset.UTC`；
  - `RandomPlaceholderResolver.java:65` 消除废弃的 `new Date()`，采用 `DateTimeFormatter.BASIC_ISO_DATE.format(LocalDate.now(ZoneOffset.UTC))`。

---

## 5. 系统后续待修改项：前端全量业务视图接入 timezone.ts

目前全系统后端平台、数据库连接、容器配置、VO 注解、时区计算已实现 100% 纯粹的 UTC 绝对时间戳规范。下一阶段核心工作为前端视图层的统一接入：

- **管理后台 18+ 处视图**（`AdminUserView.vue`, `AdminLogView.vue`, `AdminTaskView.vue`, `AdminEmailView.vue`, `AdminCtfChallengeView.vue` 等）：将原生 `new Date(row.createdAt).toLocaleString()` 统一替换为 `@/utils/timezone` 的 `utc2date(row.createdAt)`；
- **前台竞赛与队伍组件 7 处**（`VCompetitionCard.vue`, `CompetitionDetailView.vue`, `TeamInvitationsModal.vue` 等）：废除各自手写的 `formatDateTime`，统一切换为 `utc2date`；
- **日期选择器组件**：[`VDatePicker.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view-NEXT/src/components/common/VDatePicker.vue) 统一采用 `date2utc` 与 `utc2date` 进行本地选择与 UTC 传输转换。
