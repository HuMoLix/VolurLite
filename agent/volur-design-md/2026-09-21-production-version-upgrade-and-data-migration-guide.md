# 远程生产环境版本平滑升级与全量数据无损保留实施审计方案

## 1. 升级背景与现状数据评估

### 1.1 现状数据资产（基于 2026-09-21 远程 MySQL 数据备份分析）
经对远程服务器导出的生产数据镜像（`volur_users_competitions_20260921.sql` 与 `volur_db_20260921.sql`）进行深度逆向审计，远程环境当前承载着极为关键的高校真实赛事实时数据资产：
- **竞赛业务实体**：
  - `357062546458411012`：温州商学院 2026 年度第九届浙江省大学生网络与信息安全竞赛校内选拔赛创新实践挑战赛（CTF 夺旗赛）；
  - `357064550358450184`：温州商学院 2026 年度第九届浙江省大学生网络与信息安全竞赛校内选拔赛网络安全知识挑战赛（THEORY 知识赛）；
- **用户与档案资产**：72 名真实注册选手/教师用户（`user`）及完整个人档案（`user_profile`，含加密密码、邮箱、联系方式、学籍学院信息）；
- **战队与名册资产**：46 支已通过审核（`AUDIT_PASSED`）的参赛队伍（`competition_team`），56 名已完成填报认证的参赛成员（`competition_member`，含队长、选手之真实姓名、身份证号、手机号、专业职务等）；
- **静态持久化资产**：9 项用户与赛事关联静态附件（赛事宣传 Banner 图、战队队徽、选手头像，存储于宿主机 `./uploads` 挂载卷）。

### 1.2 架构演进与版本差异矩阵
远程服务器部署版本落后当前代码库若干关键迭代周期，期间系统完成了重大架构升级：
1. **即时通讯与实时仲裁系统引入 (`099526e`)**：新增 `competition_chat_message` 实体与 WebSocket/Redis 消息通道；
2. **高并发竞赛运行日志与系统审计解耦 (`2026-09-21`)**：新增 `competition_operation_log` 实体，引入虚拟线程异步削峰微批落库；
3. **战队表格化报名与身份唯一性治理 (`2026-09-12`, `2026-09-18`)**：
   - 彻底废除 `fieldSchema` 动态扩展表体系，统一固化结构；
   - `competition_member` 新增 `employee_number`（学号/工号），放宽 `user_id` 允许为空（支持预报名），新增手机号/身份证/邮箱复合防冲突索引；
   - `competition_team` 新增 `description`（承接原有 `team_slogan`）、`organization`（所属机构/学院）、`organization_type`；
4. **CTF 赛题多附件支持 (`2026-09-18`)**：`ctf_challenge` 新增 `attachments`（JSON 数组）并向下兼容；`ctf_challenge_category` 新增 `icon`；
5. **镜像管理中心 Registry 化与状态解耦 (`2026-09-12`)**：`cluster_image` 引入 `in_registry`，废弃宿主机直接扫描；
6. **邮件服务与系统配置层级化 (`2026-08-28`)**：`sys_setting` 全面采用标准小写点分键名，由 `SystemSettingInitializer` 启动自动清洗。

---

## 2. 变更改动点清单

### 2.1 增量升级迁移 SQL 脚本实现 (`scripts/upgrade/upgrade_to_latest.sql`)
针对新旧版本的差异点，设计了具备完全幂等性（Idempotent）、可安全反复重入执行的专有迁移脚本：
1. **新建数据表**：
   - `competition_chat_message`：竞赛公屏、队伍群聊与裁判私信表（含外键与复合索引）；
   - `competition_operation_log`：隔离记录选手容器启停、Flag 提交等高频操作流水；
2. **结构升级与约束调整（通过 MySQL 存储过程动态条件探测执行）**：
   - `competition_config`：安全追加 `reg_requirements` TEXT；
   - `competition_team`：安全追加 `description`、`organization`、`organization_type`；将历史 `team_slogan` 自动无损回填入 `description`；通过队长角色自动级联回填战队所属院系 `organization`；
   - `competition_member`：安全追加 `employee_number`；放宽 `user_id` 约束允许 `NULL`；新增 `idx_comp_phone`、`idx_comp_idcard`、`idx_comp_email` 3 组安全防护索引；
   - `ctf_challenge`：安全追加 `attachments` TEXT；
   - `ctf_challenge_category`：安全追加 `icon` VARCHAR(64)；
   - `cluster_image`：安全追加 `in_registry` TINYINT(1)；**关键隐患修复**：将旧版遗留的 `image_type` 字段显式放宽为 `NULL`，彻底避免新代码插入镜像时由于未传该字段导致 MySQL 严格模式抛出异常；
   - `email_log`：安全追加 `message_id` 及专属索引 `idx_email_log_message_id`。

### 2.2 打包脚本升级 (`package-amd64.sh`)
- 在打包构建装配环节中，自动将 `scripts/upgrade/upgrade_to_latest.sql` 打包至交付套件根目录，使解压后的生产环境直接具备一键升级脚本。

---

## 3. 安全隐患防范与零停机/零数据丢失原则

1. **Docker 卷安全禁忌**：
   - 远程服务器在容器维护与重拉时，**绝对禁止使用 `docker compose down -v`**。`-v` 参数会销毁 `volur-mysql-data` 与 `volur-redis-data` 命名卷，造成毁灭性数据丢失。必须统一执行 `docker compose down`。
2. **双重快照冗余防护**：
   - 实施升级前必须首先在宿主机对运行中的 MySQL 容器执行全量逻辑导出（`mysqldump`），并打包宿主机物理上传目录 `./uploads`。
3. **配置凭证一致性保护**：
   - 远程服务器的 `.env` 文件中，`VOLUR_JWT_SECRET` 与 `VOLUR_MYSQL_PASSWORD` 必须保持原值不变。一旦更改 `VOLUR_JWT_SECRET`，所有已下发的用户 JWT Token 均将失效，导致选手在线会话全部强退；若更改 MySQL 密码且未同步容器内部，会导致数据库连接鉴权失败。
4. **数据库严格模式防爆**：
   - 针对 `cluster_image.image_type` 存在 `NOT NULL` 但 Java 代码中已移除映射的问题，迁移脚本主动将其变更为 `NULL`，防止运行时因缺少默认值发生 SQL 执行阻断。

---

## 4. 验证与审计结论

- **脚本幂等性验证**：迁移脚本通过 `information_schema` 探测表名与列名，执行两次以上不会产生任何重复列或重复索引报错；
- **业务数据无损验证**：既有 72 名用户、2 场竞赛、46 支战队的名册数据在演练中字段完全对齐，旧版口号及学院归属完全回填至新版相应属性，满足生产上线标准。
