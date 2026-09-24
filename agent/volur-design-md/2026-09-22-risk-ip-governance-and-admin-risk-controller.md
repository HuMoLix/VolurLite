# 平台智能风控体系升维、基于 IP 封禁治理机制与 AdminRiskController 施工审计文档

## 1. 变更背景与现状溯源

在 Volur Lite 平台既有架构中：
1. **封禁仅停留在人机验证提级，缺乏全站网络阻断（缺少硬封禁）**：
   原有风控引擎 [`RiskEvaluationEngine`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/risk/engine/RiskEvaluationEngine.java) 主要服务于验证码难度阶梯跃迁（`LOW` -> `LINE`, `MEDIUM` -> `SHEAR`, `HIGH` -> `SLIDE_PUZZLE`），即使攻击者发起高频暴力撞库或恶意漏洞扫描，系统也仅要求滑块拼图，并没有在网关/过滤器级别直接拒绝恶意来源 IP 的连接。
2. **规则与阈值硬编码，缺少动态配置能力**：
   原引擎中的频控判定数值（如 10 分钟内 10 次失败、1 分钟内 12 次速率等）均为类常量硬编码，管理员无法根据生产实战攻防需要动态调整阈值与策略。
3. **缺少 IP 名单持久化与生命周期管理**：
   缺少针对恶意 IP、黑客网段的黑名单封禁数据库持久化记录，缺乏自动到期解封、手动解封、管理员白名单豁免以及实时 IP 风险画像诊断机制。
4. **管理端缺少风控治理入口**：
   在后台管理侧边栏中仅有系统日志与邮件管理，缺乏专门的“风控管理”模块与配套 REST API。

---

## 2. 总体方案设计：风控管理三大二级菜单功能规格

针对用户提出的在后台侧边栏新增“风控管理”（下设“风控设置”、“策略设置”、“IP管理”三个二级菜单）的需求，系统设计如下功能规格与职责边界：

### 2.1 二级菜单一：风控设置 (Risk Settings)
- **定位**：全站风控引擎全局基础开关、拦截响应行为及运维豁免策略。
- **核心功能**：
  1. **全局风控引擎总开关 (`system.risk.enabled`)**：一键开启或关闭全平台风险评估；
  2. **IP 拦截总开关 (`system.risk.ip_filter_enabled`)**：控制底层安全过滤器 `RiskIpFilter` 是否执行黑名单物理拦截；
  3. **默认封禁时长 (`system.risk.default_ban_duration_minutes`)**：设定策略自动触发封禁时的默认惩罚有效期（如 1440 分钟 / 24 小时）；
  4. **拦截提示文案 (`system.risk.ban_notice_message`)**：自定义被拦截客户端收到的 403 提示语；
  5. **本地回环/内网穿透豁免 (`system.risk.bypass_loopback`)**：自动旁路放行 `127.0.0.1`、`::1` 及容器健康检查，防止本地误封。

### 2.2 二级菜单二：策略设置 (Policy Settings)
- **定位**：动态安全阈值矩阵、阶梯跃迁规则及自动封禁联动。
- **核心功能**：
  1. **连续失败升中风险阈值 (`failuresMediumThreshold`)**：默认 5 次，触发图形码扭曲加噪；
  2. **连续失败升高风险阈值 (`failuresHighThreshold`)**：默认 10 次，强制弹出滑动拼图挑战；
  3. **连续失败自动封禁阈值 (`failuresBanThreshold`)**：默认 20 次，达到后自动触发 `RiskAutoBanEvent` 将恶意 IP 加入黑名单并阻断访问；
  4. **突发速率阈值 (`velocityMediumThreshold`)**：默认 12 次/分钟，防高频刷接口；
  5. **账号维度防撞库阈值 (`accountFailuresMediumThreshold` / `accountFailuresHighThreshold`)**：跨 IP 保护目标账户，高危时要求邮箱带外 OTP；
  6. **降险恢复步长 (`recoveryStepCount`)**：核验成功后扣减失败惩罚步长（默认 6 次）。

### 2.3 二级菜单三：IP管理 (IP Management)
- **定位**：IP 黑白名单全生命周期管理、实时诊断探针与拦截审计。
- **核心功能**：
  1. **黑白名单列表与过滤**：支持按 IP、名单类型（BLACKLIST / WHITELIST）、来源（MANUAL / AUTO_TRIGGER）、状态（ACTIVE / EXPIRED / REVOKED）多维分页检索；
  2. **手动添加名单**：管理员可手动录入指定 IP，设置封禁时长（支持指定分钟数或永久封禁）与封禁原因；支持录入白名单放行可信 IP；
  3. **一键解封与撤销**：管理员可一键撤销封禁或白名单，立即同步失效 Redis 缓存；
  4. **IP 实时风险诊断探针 (`/api/v1/admin/risk/ip-rules/diagnose`)**：输入任意 IP，秒级返回其在 Redis 内存中的实时失败数、速率计数、当前风险等级、封禁到期倒计时及原因；
  5. **风控大盘统计指标 (`/api/v1/admin/risk/stats`)**：累计封禁总数、当前生效黑名单数、白名单数、今日自动封禁与手动封禁数。

---

## 3. 核心改动点清单

### 3.1 数据库与增量迁移脚本 (`scripts/upgrade/upgrade_to_latest.sql`)
- 严格遵循 `GEMINI.md` 规范，通过幂等脚本新建 `risk_ip_rule` 数据表：
  - 包含 `id`（雪花算法）、`ip`、`rule_type`、`source_type`、`reason`、`status`、`expires_at`、`operator_id`、`operator_name`、`operator_ip`、`created_at`、`updated_at`；
  - 建立联合索引 `idx_risk_ip_rule_ip_type_status`、`idx_risk_ip_rule_status_expires`、`idx_risk_ip_rule_created_at`。

### 3.2 领域实体与仓储层 (`volur-verification`)
- 新增枚举：`IpRuleType`、`IpRuleStatus`、`IpRuleSource`；
- 新增实体：`RiskIpRule`（继承自雪花主键与 JPA 注解）；
- 新增仓储：`RiskIpRuleRepository`（支持 Specification 动态复合查询与聚合统计）；
- 新增映射器：`RiskIpRuleMapper`（MapStruct 映射 VO）。

### 3.3 系统动态设置拓展 (`volur-system`)
- 在 `SystemSettingKeys` 中注册 `system.risk.*` 及 `system.risk.policy.*` 标准键名；
- 在 `SystemSettingInitializer` 中注册默认配置及自动迁移分组解析。

### 3.4 核心业务服务与事件解耦 (`volur-verification`)
- 新增 `AdminRiskService` 接口及其实现 `AdminRiskServiceImpl`：
  - 整合 Redis 极速黑白名单缓存（`vredis:verification:risk:ban:ip:` / `vredis:verification:risk:whitelist:ip:`）；
  - 实现双级穿透保护：优先读取 Redis，未命中时探测 DB 并按剩余 TTL 动态回填 Redis；
  - 监听 `RiskAutoBanEvent` 实现风控引擎与封禁落库的解耦异步联动。

### 3.5 高性能全局安全过滤器 (`volur-auth`)
- 新增 `RiskIpFilter`，注册在 `SecurityConfig` 过滤链最前端（`JwtAuthFilter` 之前）：
  - 拦截已被封禁的客户端 IP，零开销短路，直接通过 `HandlerExceptionResolver` 抛出 403 Forbidden 并输出标准 `ApiResponse` JSON；
  - 白名单与本地回环自动放行。

### 3.6 控制器层 (`volur-verification`)
- 实现 `AdminRiskController`（路由前缀 `/api/v1/admin/risk`）：
  - `GET/PUT /settings`：风控设置；
  - `GET/PUT /policy`：策略设置；
  - `GET /ip-rules`、`POST /ip-rules`、`POST /ip-rules/{id}/revoke`、`DELETE /ip-rules/{id}`：IP 名单生命周期管理；
  - `GET /ip-rules/diagnose`：IP 风险探针；
  - `GET /stats`：统计概览。

### 3.7 前端类型定义与 API 封装 (`volur-lite-view`)
- 严格对齐后端领域模型：
  - 在 `src/volur-lite-view/src/types/risk/index.ts` 中声明 `AdminRiskSettingsVO/DTO`、`AdminRiskPolicyVO/DTO`、`AdminIpRuleVO/DTO`、`AdminIpDiagnosisVO`、`AdminRiskStatisticsVO` 等；
  - 在 `src/volur-lite-view/src/api/risk/index.ts` 中以强类型 Axios 封装 7 大管理 API，请求路径 100% 严格对齐后端 `AdminRiskController`。

### 3.8 前端子菜单三大管理页面实现 (`volur-lite-view`)
1. **风控设置页面 (`AdminRiskSettingsView.vue`)**：
   - 路径：`/admin/risk/settings`，组件名称：`AdminRiskSettings`；
   - 布局：遵循 `AdminSystemView` 与 Naive UI 体系规范，支持响应式与暗黑/明亮主题适配；
   - 包含全局风控总开关、IP 物理拦截开关、本地回环/中继豁免开关、默认自动封禁时长数字输入与 403 自定义提示语配置，具备在途保存状态与表单校验。
2. **策略设置页面 (`AdminRiskRulesView.vue`)**：
   - 路径：`/admin/risk/rules`，组件名称：`AdminRiskRules`；
   - 布局：阶梯式攻防阈值矩阵配置卡片，支持 10 分钟连续失败升中/高风险、自动封禁阈值、1 分钟突发速率、单账号全局中/高风险阈值及降险恢复步长。
3. **IP 管理治理页面 (`AdminRiskIpView.vue`)**：
   - 路径：`/admin/risk/ip`，组件名称：`AdminRiskIp`；
   - 视觉规范：严格沿用 `AdminLogView.vue` 与 `AdminUserView.vue` 的顶层指标绶带（`.stat-ribbon` + `.stat-mini-card`），展示累计封禁总数、当前生效黑名单数、白名单数、今日自动封禁数及今日手动封禁数；
   - 核心表格：采用 `VolurTableCard` 组件，支持远程分页、排序、搜索（IP/网段模糊搜索）、规则类型（黑/白名单）筛选、来源（自动/手动）筛选、状态（生效/过期/已解封）筛选；
   - 交互弹窗：
     - **添加 IP 规则弹窗**：支持录入 IP/CIDR、选择名单类型、设置封禁时长（分钟，留空为永久）及说明；
     - **IP 实时风险诊断探针弹窗**：输入任意 IP，秒级实时探测其在 Redis 内存滑动窗口中的失败计数、请求速率、评级及封禁状态；
     - **安全操作列**：一键撤销解封（`NPopconfirm`）、物理删除（`NPopconfirm`）。

### 3.9 路由注册与后台导航整合
- 在 `src/volur-lite-view/src/router/routes.ts` 中注册 `risk` 子路由及其三个页面组件（支持路由懒加载与元数据 `titleKey`）；
- 在 `AdminLayout.vue` 侧边栏的 `AdminRiskGroup` 中无缝导航，且高亮计算属性自适应适配 `AdminRisk*` 路由前缀；
- 补齐中英文多语言资源字典：
  - `src/volur-lite-view/src/locales/zh_CN/admin/risk.json`
  - `src/volur-lite-view/src/locales/en_US/admin/risk.json`
  - `src/volur-lite-view/src/locales/zh_CN/layout.json`
  - `src/volur-lite-view/src/locales/en_US/layout.json`

---

## 4. 验证结果与质量保障

1. **后端单元测试**：
   - 新增 `AdminRiskServiceTest`（覆盖 Redis 缓存命中、白名单放行、DB 探测回填、实时 IP 诊断）；
   - 执行 `mvn test -pl volur-verification,volur-auth -am`：全部 25 个 verification 测试用例与 17 个 auth 测试用例 100% 通过（`BUILD SUCCESS`）。
2. **前端构建与类型检查**：
   - 执行 `npm run build`（`vue-tsc --noEmit && vite build`）：所有新页面、路由、类型及组件 0 错误 0 警告，成功生成生产分包产物（`✓ built in 6.97s`）。
3. **视觉与体验一致性**：
   - 严格遵循 Volur 设计铁律（主要按钮亮色下纯黑 `#000000` / 字体 `#ffffff`、描述文字 `#444444`、monospace 代码高亮展示 IP、Naive UI Provider 体系完美兼容）。

