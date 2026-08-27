# Volur Lite 站内信与竞赛组队邀请通知系统设计与施工归档

- **执行日期**：2026-08-25
- **施工模块**：`volur-notification` (新增), `volur-competition` (扩展), `volur-auth` (接入), `volur-server` (聚合)
- **文档类型**：系统架构与业务功能施工档案

---

## 1. 变更背景与职责边界

在 Volur Lite 平台中，为了实现对用户的精准触达以及支持需要双向确认的交互业务（如竞赛组队邀请、安全操作提醒等），平台需要一套专用的**个人站内信通知系统（In-App Notification System）**。

### 1.1 与公告系统的清晰边界
- **全站广播与公共公告**：由系统公告模块（`announcement`）统一承载，负责全站维护广播、公开大屏消息与赛题一血流；
- **个人站内信系统（本模块）**：严格聚焦于**针对具体用户个人的私有收件箱**，核心覆盖两大场景：
  1. **通知型通知（Informational Notification）**：密码修改成功提醒、账号安全告警、敏感操作审计提示等（只读展示）；
  2. **交互型通知（Actionable Notification）**：竞赛组队邀请、入队申请等需要接收方进行确认/拒绝决策的操作卡片。

---

## 2. 架构设计与改动清单

### 2.1 新建模块 `volur-notification`
- **POM 注册**：在父工程 `pom.xml` 中将 `volur-notification` 注册为子模块，并在 `volur-server` 中完成依赖聚合；
- **领域模型与枚举 (`domain`)**：
  - `domain.enums.NotificationType`：`SECURITY_ALERT`, `ACCOUNT_CHANGE`, `COMPETITION_INVITE`, `COMPETITION_APPLICATION`；
  - `domain.enums.NotificationStatus`：`UNREAD`, `READ`；
  - `domain.enums.NotificationActionStatus`：`NONE`, `PENDING`, `ACCEPTED`, `REJECTED`, `EXPIRED`, `CANCELLED`；
  - `domain.enums.SecurityEventType`：`PASSWORD_CHANGED`, `PASSWORD_RESET`, `ACCOUNT_LOCKED` 等；
  - `domain.entity.Notification`：站内信主体实体（使用 `@SnowflakeGenerated` 雪花主键，支持结构化 JSON `action_data` 与有效截止时间 `expire_time`）；
  - `domain.dto.NotificationQueryDTO`：分页查询 DTO；
  - `domain.vo.NotificationVO`：视图响应对象（带 `@Builder.Default`）；
  - `domain.bo.SendNotificationBO` / `UpdateNotificationActionBO`：内部业务流转对象；
- **持久层与对象映射 (`repository` & `mapper`)**：
  - `repository.NotificationRepository`：JPA 规范查询，含未读计数与一键全部标已读；
  - `mapper.NotificationMapper`：MapStruct 声明式映射器；
- **服务层与控制层 (`service` & `controller.v1`)**：
  - `service.impl.NotificationServiceImpl`：未读数基于 Redis 缓存（`VREDIS_NOTIFICATION_UNREAD_COUNT_KEY_PREFIX = "vredis:notification:unread_count:"`，`Duration.ofMinutes(10)`），实现通知入库、未读计数增减、已读标记与动作状态同步；
  - `controller.v1.NotificationController`：RESTful 规范接口（`/api/v1/notifications`，提供未读数、列表、详情、单标已读、全标已读、删除通知）；
- **事件监听器 (`listener`)**：
  - `listener.NotificationEventListener`：异步消费 `SecurityNotificationEvent`，实现安全审计站内信的自动下发。

---

### 2.2 扩展竞赛模块 `volur-competition`
- **新增实体与枚举**：
  - `domain.enums.InvitationStatus`：`PENDING`, `ACCEPTED`, `REJECTED`, `EXPIRED`, `CANCELLED`；
  - `domain.entity.CompetitionTeamInvitation`：竞赛队伍邀请记录表；
  - `repository.CompetitionTeamInvitationRepository`：邀请数据检索；
- **新增 DTO / VO 与 Mapper**：
  - `domain.dto.TeamInviteRequestDTO`：队长邀请入参；
  - `domain.dto.TeamInviteAcceptDTO`：受邀人确认入队入参（含动态表单数据绑定）；
  - `domain.vo.CompetitionInvitationVO`：邀请详情视图对象；
  - `mapper.CompetitionMapper`：添加邀请对象 MapStruct 映射；
- **服务与控制器**：
  - `service.CompetitionTeamInvitationService` & `impl`：处理发送邀请、查询收到/发出的邀请、接受邀请入队、拒绝邀请、撤销邀请等完整业务流，并在并发事务下进行严格的资格检查（报名窗口有效性、队伍满员校验、用户重复组队校验、邀请过期校验）；
  - `controller.v1.CompetitionTeamInvitationController`：提供 `/api/v1/competitions/{competitionId}/teams/invitations` 路由；
- **事件解耦与站内信联动 (`event` & `listener`)**：
  - `event.TeamInvitationSentEvent`：邀请发起事件；
  - `event.TeamInvitationHandledEvent`：邀请处理完成事件；
  - `listener.CompetitionNotificationListener`：
    - 监听到 `TeamInvitationSentEvent` 时，向受邀人发送一条 `COMPETITION_INVITE` 类型的交互型站内信（`actionStatus = PENDING`，携带比赛与队伍元数据）；
    - 监听到 `TeamInvitationHandledEvent` 时，同步更新受邀人站内信状态（`ACCEPTED` / `REJECTED`），并向邀请人（队长）回执一条通知型站内信。

---

### 2.3 接入认证安全通知 `volur-auth`
- 在 `AuthServiceImpl` 中，当用户成功通过重置令牌（`resetPasswordByToken`）、邮箱验证码（`resetPassword`）或直接修改密码（`changePassword`）重置/修改密码后，通过 `SpringContextHolder.publishEvent` 触发 `SecurityNotificationEvent`，自动向用户发送包含操作 IP 与时间的安全提醒站内信。

---

## 3. 安全防护与并发一致性设计

1. **防重复邀请与满员并发防刷**：
   - 在发送邀请前，进行 `existsByCompetitionIdAndTeamIdAndInviteeIdAndStatus(..., PENDING)` 校验，杜绝多次并发刷邀请；
   - 接受邀请时，实时统计当前队伍人数（`memberRepository.countByTeamId(teamId)`），防范受邀人接受瞬间队伍已被其他成员填满；
2. **邀请时效性控制 (TTL)**：
   - 邀请默认设置 24 小时过期时间（`expire_time`），查询与接受时若检测到已超期，自动标记为 `EXPIRED` 并阻断入队流程；
3. **未读数高频访问保护**：
   - Redis 缓存用户的未读消息总数，避免前端导航栏高频轮询小红点接口直接击穿 MySQL。

---

## 4. 规范遵循核验

- [x] **依赖注入**：全部使用 `private final` 字段结合 Lombok `@RequiredArgsConstructor`，无 `@Autowired`，无手写构造函数；
- [x] **领域模型**：统一存放在各模块的 `domain` 包下（`domain.dto`, `domain.vo`, `domain.entity`, `domain.bo`, `domain.enums`）；
- [x] **模型注解**：DTO 统一 `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`（无 `@Builder`）；VO 统一包含 `@Builder.Default` 保护默认值；
- [x] **MapStruct 规范**：统一在 `mapper` 包下声明接口，配置 `@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)`；
- [x] **Controller 规范**：包路径显式带版本 `controller/v1`，路由前缀 `/api/v1/*`，统一小写工厂方法 `ApiResponseEntity.ok / success / created`；
- [x] **Redis 规范**：常量以 `VREDIS_` 开头、Key 值为小写冒号结尾格式，过期时间统一采用 `java.time.Duration`；
- [x] **Service 签名**：动作型方法返回 `void`，判定型方法返回基本类型 `boolean`。

---

## 5. 编译与验证结果

执行 Maven 整体编译打包：
```bash
mvn clean compile -DskipTests
```
全平台 12 个子模块全部构建成功：
- `volur-lite-platform`: SUCCESS
- `volur-framework`: SUCCESS
- `volur-system`: SUCCESS
- `volur-user`: SUCCESS
- `volur-verification`: SUCCESS
- `volur-notification`: SUCCESS
- `volur-auth`: SUCCESS
- `volur-challenge`: SUCCESS
- `volur-container`: SUCCESS
- `volur-scoreboard`: SUCCESS
- `volur-competition`: SUCCESS
- `volur-server`: SUCCESS
