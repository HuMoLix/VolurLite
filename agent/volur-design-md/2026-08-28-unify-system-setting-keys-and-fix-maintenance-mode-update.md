# 系统动态配置项键名规范统一重构与全站维护模式更新修复

## 1. 变更背景与问题剖析

在既有系统的运行与交互中，暴露了如下三项关键问题：
1. **重复发起公开设置请求**：当访问 `http://localhost:3000/admin/system` 或刷新页面时，Vue Router 的全局前置守卫（`router.beforeEach`）与根组件（`App.vue` 的 `onMounted` 钩子）同时/先后无条件调用了 `systemStore.fetchPublicSettings()`，且 Store 内部未实现在途请求去重（Promise Deduplication）与加载态校验，导致网络层连续发起两次相同的 `/api/v1/system/settings/public` HTTP 请求。
2. **多套历史别名双写与混乱输出**：后端 `SystemSettingServiceImpl.getPublicSettingsMap()` 中同时存在扁平大写下划线键（如 `MAINTENANCE_MODE`, `MAINTENANCE_NOTICE`, `ENABLE_CAPTCHA`）与树状层级点分键（如 `system.maintenance.enabled`, `system.maintenance.notice_message`）的兼容与双写，造成多处数据冗余、配置源分裂。
3. **后台更新全站维护模式开关不生效**：
   - 前端管理后台保存时提交的键为 `system.maintenance.enabled`；
   - 后端 `SystemSettingServiceImpl` 的 `KEY_ALIASES` 遗漏了 `MAINTENANCE_MODE` 的映射；
   - 后端过滤器 `MaintenanceModeFilter` 与鉴权服务直接硬编码读取 `MAINTENANCE_MODE`，无法读取到新配置亦无法淘汰缓存；
   - 数据库若同时存在新旧两条记录，前端在 `fetchSettings()` 遍历回填表单时，旧记录的值会后置覆盖新值，使得开关在保存后看起来未发生更新。

---

## 2. 改造方案与改动点

根据项目架构规范与设计指令，**彻底废除“历史版本兼容”与“别名双写映射机制”**，确立**全系统统一的小写点分层级规范（`system.<group>.<field_name>`）**。

### 2.1 后端改动
1. **统一键名常量管理**：
   - 新增 [`SystemSettingKeys.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/constant/SystemSettingKeys.java)，统一定义基础、鉴权、人机验证、邮件、容器靶机、全站运维与安全风控等全量层级键名常量。
2. **服务实现重构与双写彻底移除**：
   - 重构 [`SystemSettingServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/service/impl/SystemSettingServiceImpl.java)：
     - 彻底移除 `KEY_ALIASES` 字典与别名双写/多级淘汰冗余逻辑；
     - `getPublicSettingsMap()` 仅输出公开配置与标准的 `system.maintenance.enabled`、`system.maintenance.notice_message`；
     - `loadFromL2OrDb`、`updateSetting` 与 `evictCache` 均直接针对标准层级键名进行操作。
3. **安全过滤器与认证服务对齐**：
   - 修改 [`MaintenanceModeFilter.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/security/filter/MaintenanceModeFilter.java)：统一读取 `system.maintenance.enabled` 与 `system.maintenance.notice_message`；
   - 修改 [`AuthServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/AuthServiceImpl.java)：统一读取 `system.captcha.enabled`、`system.maintenance.enabled`、`system.auth.registration_enabled` 等层级键名；
   - 修改 [`EmailServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/service/impl/EmailServiceImpl.java)、[`SmtpMailDeliveryProvider.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/mail/provider/SmtpMailDeliveryProvider.java)、[`AliyunDirectMailDeliveryProvider.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/mail/provider/AliyunDirectMailDeliveryProvider.java)、[`CloudTurnstileCaptchaProvider.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/captcha/provider/CloudTurnstileCaptchaProvider.java) 与 [`DynamicFlagGenerator.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/flag/DynamicFlagGenerator.java)。
4. **单元测试修复**：
   - 同步修复 [`AuthVerificationCrossValidationTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/test/java/com/humolix/volur/auth/AuthVerificationCrossValidationTest.java)、[`EmailServiceVerificationTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/test/java/com/humolix/volur/verification/EmailServiceVerificationTest.java) 与 [`DynamicFlagGeneratorTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-container/src/test/java/com/humolix/volur/container/flag/DynamicFlagGeneratorTest.java)。

5. **自动化数据清洗迁移与初始化器**：
   - 新增 [`SystemSettingInitializer.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/init/SystemSettingInitializer.java)（实现 `ApplicationRunner`）：系统启动时自动检测数据库历史遗留键（如 `ENABLE_CAPTCHA`、`MAINTENANCE_MODE` 等），平滑迁移至标准层级键（`system.captcha.enabled` 等）并物理清除旧记录；同时自动初始化平台基础默认配置行。
   - 强化公开设置输出安全：在 `getPublicSettingsMap()` 中增加 `startsWith("system.")` 过滤，彻底封堵任何非规范历史残留键向前端输出。

### 2.2 前端改动
1. **Pinia Store 请求去重与标准层级键名**：
   - 重构 [`src/store/modules/system.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/store/modules/system.ts)：
     - `fetchPublicSettings(force = false)` 引入 `inFlightFetchPromise` 在途请求复用与 `isLoaded` 缓存校验，彻底杜绝重复并发请求；
     - 计算属性 `isMaintenanceMode` 和 `maintenanceNotice` 统一指向 `system.maintenance.enabled` 与 `system.maintenance.notice_message`。
2. **生命周期调用优化**：
   - 优化 [`src/App.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/App.vue)：`onMounted` 中仅在 `!systemStore.isLoaded` 时发起请求，防止与路由守卫竞态。
3. **管理端系统设置视图优化**：
   - 重构 [`src/views/admin/system/AdminSystemView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/admin/system/AdminSystemView.vue)：
     - 在 Tab 2（人机验证与风控）中补齐“全站人机验证总开关 (`system.captcha.enabled`)”的表单交互与状态联动；
     - 彻底移除所有历史大写别名判断，统一仅匹配层级标准键名，杜绝回填覆盖冲突。
4. **认证与维护视图优化**：
   - 修改 [`src/components/auth/AuthForm.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/components/auth/AuthForm.vue) 与 [`src/views/error/MaintenanceView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/error/MaintenanceView.vue)。

### 2.3 规范文档更新
- 在 [`Rules.md`](file:///H:/HuMoLix/github/Volur-Lite/agent/volur-design-skills/Rules.md) 补充第 16 章节“系统动态配置项命名规范”；
- 在 [`view-Rules.md`](file:///H:/HuMoLix/github/Volur-Lite/agent/volur-design-skills/view-Rules.md) 补充前端配置读取与去重规范；
- 在 [`GEMINI.md`](file:///H:/HuMoLix/github/Volur-Lite/GEMINI.md) 根协作指令中明确系统动态配置规范条款。

---

## 3. 验证与测试结果

- **后端构建与测试**：Maven 执行 `mvn clean test` 编译 12 个模块，所有单元测试 100% 通过（`BUILD SUCCESS`）。
- **前端类型检查与构建**：执行 `vue-tsc --noEmit && vite build`，0 错误，打包完全成功。
