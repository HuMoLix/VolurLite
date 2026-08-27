# 系统设置容器策略职责边界收敛与动态 Flag 模板引擎设计明确

## 1. 变更背景与架构分析

### 1.1 全局系统设置与竞赛规则的职责混淆问题
在既有 [`AdminSystemView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/admin/system/AdminSystemView.vue) 的「容器与赛题策略」（Tab 4）中，此前配置了：
- 单容器默认存活时长（`containerDuration`）
- 单用户最大并发靶机数（`maxUserContainers`）
- 动态 Flag 前缀规范（`flagPrefix`）
- 靶机动态端口映射范围（`portRangeStart` - `portRangeEnd`）

**设计问题与职责边界剖析**：
1. **全局物理基础设施资源**：
   - 宿主机/K8s 集群的端口映射池（如 `20000 - 30000`）属于平台服务器层级的物理端口规划，必须在全局系统配置中统一调度分配。
2. **竞赛维度的业务策略与规则**：
   - **单容器存活时长**与**单用户最大并发靶机数**：不同的赛事有完全不同的规则（如 CTF 个人限时赛 30 分钟/1 台、AWD 攻防赛全天/多台），将其硬编码在全局系统设置中会导致无法适配多赛事独立规则，必须下放到具体竞赛（`CompetitionConfig` / `CompetitionStageConfig`）中独立配置。
   - **动态 Flag 模板**：各个竞赛、赛题主办方要求的 Flag 格式各异（如 `flag{...}`, `CISCN{...}`, `NSSCTF{...}`），不应在全站全局写死，应由赛事/题目挂载规则决定。

### 1.2 动态 Flag 模板引擎的原本设计意图澄清
- 在 [`DynamicFlagGenerator.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/flag/DynamicFlagGenerator.java) 与 [`FlagTemplateParser.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/flag/FlagTemplateParser.java) 中，原本设计的核心是**高性能动态 Flag 模板引擎（Template Engine）**，而非简单的“前缀字符串拼接”。
- 模板引擎全面支持占位符惰性解析求值，例如：
  - `flag{{UUID}}` -> `flag{123e4567-e89b-12d3-a456-426614174000}`
  - `volur{{UUID_NO_DASH}}` -> `volur{123e4567e89b12d3a456426614174000}`
  - `ciscn{{RANDOM:16}}` -> `ciscn{a1b2c3d4e5f67890}`
  - `flag{{USER_ID}_{TIMESTAMP}}` -> `flag{1001_1787850000000}`
- 此前在全局系统设置中将其展示为一个简单的“前缀（如 `volur{`）”文本框，既矮化了模板引擎的能力，又放错了配置层级。

---

## 2. 前后端与数据库全方位对齐动作

### 2.1 前端全局系统设置（`AdminSystemView.vue`）彻底瘦身
- 将 Tab 4 重命名为「容器环境与动态靶机」，彻底移除 `containerDuration`、`maxUserContainers` 与 `flagPrefix` 表单项；
- **Tab 4 仅保留「靶机动态端口映射范围」（`portRangeStart` - `portRangeEnd`）**，并完善了字段用途说明（宿主机/K8s 集群动态端口池）；
- 清理了 `settingsForm`、`fetchSettings`、`handleSaveSettings` 与 `handleResetDefault` 中的废弃字段交互。

### 2.2 后端代码与常量全量同步移除
1. [`SystemSettingKeys.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/constant/SystemSettingKeys.java)：
   - 彻底删除已下放的废弃键常量：
     - `CONTAINER_DEFAULT_DURATION_MINUTES`
     - `CONTAINER_MAX_USER_CONTAINERS`
     - `CHALLENGE_FLAG_PREFIX`
     - `CHALLENGE_FLAG_SUFFIX`
     - `CHALLENGE_FLAG_TEMPLATE`
   - 全局容器分组仅保留基础设施动态端口池常量：`CONTAINER_PORT_RANGE_START` 与 `CONTAINER_PORT_RANGE_END`。
2. [`DynamicFlagGenerator.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/flag/DynamicFlagGenerator.java)：
   - 彻底解除对全局 `SystemSettingService` 的强耦合，默认模板修正为 `"flag{{UUID}}"`；
   - 提供 `generateFlag(String template, DynamicFlagContext context)` 方法，优先接收具体题目/赛事所配置的自定义 Flag 模板。
3. [`CtfInstanceServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/service/impl/CtfInstanceServiceImpl.java)：
   - 启动动态靶机生成 Flag 时，直接读取题库/赛题中设置的模板 `challenge.getFlag()` 并传入 `dynamicFlagGenerator`。

### 2.3 数据库历史遗留数据自动化物理清理
- 在 [`SystemSettingInitializer.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/init/SystemSettingInitializer.java) 中增加 `OBSOLETE_KEYS_TO_CLEAN` 列表：
  - 启动时自动扫描 `sys_setting` 表，物理删除 `system.challenge.flag_*`、`system.container.default_duration_minutes`、`system.container.max_user_containers` 及旧版历史残留键并驱逐 Redis 缓存，确保数据库单一真实数据源的绝对纯净。

### 2.4 规范与文档更新
- 在 [`Rules.md`](file:///H:/HuMoLix/github/Volur-Lite/agent/volur-design-skills/Rules.md) 第 16 章中明确「配置职责边界铁律」：全局系统配置仅保留基础设施级参数，业务级规则（Flag 模板、容器生命周期、最大并发数）严格下放由 `Competition` / `Challenge` 承载。

---

## 3. 验证结果

- **前端构建**：执行 `npm run build`，编译打包 0 错误（`built in 7.48s`）。
- **后端测试**：全量单元测试与 12 个模块构建全部通过（`BUILD SUCCESS`）。
