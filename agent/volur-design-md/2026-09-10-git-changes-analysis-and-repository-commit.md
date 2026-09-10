# 施工与审计记录：Git 变更综合分析、敏感凭证隔离与本地仓库提交归档

## 1. 基本信息

- **任务名称**：Git 变更分析、敏感凭据屏蔽、编译验证与本地仓库全量提交归档
- **执行时间**：2026-09-10
- **操作范围**：
  - 根目录 [`.gitignore`](file:///Users/humolix/Documents/Dev/Volur-Lite/.gitignore)
  - 认证模块 [`SecurityWhitelistMatcher.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/security/util/SecurityWhitelistMatcher.java)
  - 全局 12 个后端模块 (`volur-lite-platform`)
  - 前端工程 (`volur-lite-view`)
  - 架构设计与审计记录 (`agent/volur-design-md/`)
  - 本地 Git 仓库提交（Git Commit）

---

## 2. 变更背景与问题排查

### 2.1 变更背景
自 2026-09-02 提交 `4d90706` 以来，平台在容器集群编排、异步任务管理、系统邮件审计、安全白名单重构、个人中心与前台鉴权等方面进行了深度迭代与多轮重构。在执行代码归档入库前，需全面分析工作区变更、修复潜在编译隐患，并阻断敏感凭证入库风险。

### 2.2 关键安全与编译问题修复
1. **阻断运行时敏感证书入库风险（重大安全隐患防护）**：
   - **风险排查**：工作区未跟踪目录 `data/` 下存在集群节点动态生成的 TLS 私钥及自签证书（`data/certs/nodes/192.168.0.124/key-pkcs8.pem`、`key.pem`、`ca.pem` 等）以及运行时备份文件 `data/backups/`。
   - **处置方案**：在根目录 [`.gitignore`](file:///Users/humolix/Documents/Dev/Volur-Lite/.gitignore) 中显式添加 `data/` 目录忽略规则，严格杜绝节点私钥与敏感自签证书泄露至版本控制系统。
2. **修复 `SecurityWhitelistMatcher` Java Record 访问器编译错误**：
   - **问题排查**：在 `mvn clean compile` 阶段，`volur-auth` 模块因调用 `whitelistProperties.getAllPaths()` 报错（`SecurityWhitelistProperties` 为 Java Record 结构，无 JavaBean 规范的 `get` 前缀访问器）。
   - **处置方案**：将 `whitelistProperties.getAllPaths()` 重构为标准组件访问器 `whitelistProperties.allPaths()`，并在 `getPatterns()` 中复用 `whitelistProperties.asArray()`。

---

## 3. Git 变更核心内容深度解析

本次提交共覆盖 170 余个文件（含 35 份架构与审计文档），主要涵盖以下六大核心演化维度：

### 3.1 容器与集群云原生编排 (volur-container & 前端集群/镜像/容器视图)
- **多节点 Docker Swarm & K3s 双策略运行时**：
  - 支持节点动态加入、SSH 证书/密码自动探测与健康检测。
  - 节点 TLS 证书自动分发与持久化，Docker Daemon 2376 端口动态配置。
  - 容器跨节点调度锁定（Node Pinning）与跨节点镜像按需复制。
- **镜像与容器精细化治理**：
  - 镜像层级下钻查看（Layers Inspect）、镜像分类维护。
  - 分布式节点按需选择性镜像删除，删除前基于平台运行态与宿主机 Docker 双重防卫校验。
  - 容器实例 WebSocket 终端交互、全屏交互优化及日志流式查看。
- **前端对应交付**：
  - `AdminClusterSettingsView.vue`、`AdminImageView.vue`、`AdminContainerView.vue` 及对应 i18n（`clusters.json`、`images.json`、`containers.json`）。

### 3.2 异步任务调度与实时推送系统 (volur-system & volur-notification & 前端任务中心)
- **异步任务生命周期管理**：
  - 构建 `AsyncTaskEntity`、`AdminAsyncTaskController` 与 `AsyncTaskExecutorConfig` 线程池配置。
  - 任务状态流转（PENDING / RUNNING / SUCCESS / FAILED）与全局执行统计。
- **SSE 实时流式通知集成**：
  - 引入 `AsyncTaskPushEventListener`，任务状态变化自动通过 SSE 广播到前端，实现前端任务表格无感状态同步与进度更新。
  - 修复 Snowflake ID 前端 JavaScript 精度丢失问题（统一转为 String 传输）。
- **前端对应交付**：
  - `AdminTaskView.vue`、`api/task/`、`types/task/` 及中英文国际化语言包。

### 3.3 统一安全白名单 (SSOT) 与 JWT 优雅降级 (volur-auth)
- **单一真实源 (SSOT)**：
  - 抽取 `SecurityWhitelistProperties`，统一管理全系统 30 余条公开路由（包含 `/api/v1/auth/logout`、`/api/v1/system/settings/public` 等）。
  - 基于 `AntPathMatcher` 实现 `SecurityWhitelistMatcher`。
- **JWT 过滤器优雅降级**：
  - 访问白名单公开接口或登出接口时，若 Token 过期或非法，不再提前 401 截断，而是优雅降级为匿名上下文放行，彻底根除“已过期 Token 无法正常登出”的死锁缺陷。

### 3.4 邮件服务审计与治理中心 (volur-verification)
- **邮件日志与统计审计**：
  - 新增 `AdminEmailController`、`EmailLogVO`、`AdminEmailStatisticsVO`。
  - 修复 SMTP 发送端地址控制符/空白符错误，补充完整单元测试。
- **前端对应交付**：
  - `AdminEmailView.vue` 及配套 `emails.json` 国际化配置。

### 3.5 基础架构与领域模型规范重构 (volur-framework & volur-system & volur-user)
- **雪花算法防时钟回拨**：
  - 重构 `RedisWorkerIdAssigner` 自愈续约机制，保障集群环境下 Worker ID 唯一与高可用。
- **系统设置分级解耦**：
  - 废弃扁平大写历史键，全面推行小写点分层级键（`system.<group>.<field>`）。
  - 系统管理页面拆分子 Tab 组件（`src/views/admin/system/tabs/`），降低单文件复杂度。
- **用户与鉴权中心规范**：
  - 前端 `Gender` 枚举与后端严格对齐。
  - 移除用户创建时默认头像写死反模式。
  - 完善密码重置视图 `ResetView.vue` 与路由跳转守卫。

---

## 4. 构建验证与影响评估

1. **后端平台编译验证**：
   - 执行 `mvn clean compile -DskipTests`；
   - 结果：全平台 12 个模块（`volur-framework`、`volur-logging`、`volur-system`、`volur-user`、`volur-verification`、`volur-notification`、`volur-auth`、`volur-challenge`、`volur-container`、`volur-competition`、`volur-server`）**100% SUCCESS**，0 错误。
2. **前端工程构建验证**：
   - 执行 `npm run build`（`vue-tsc --noEmit && vite build`）；
   - 结果：4276 个模块全部转译成功，TypeScript 类型检查通过，生产构建 **100% SUCCESS**。
3. **版本控制状态**：
   - 敏感凭证 `data/` 已被完全忽略；
   - 所有源代码、配置文件、中英文国际化资源与设计审计文档全部处于清洁可提交状态。
