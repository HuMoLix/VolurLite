# 智能风控与人机验证设置收敛及邮箱注册白名单治理施工记录

## 1. 变更背景与架构审计

### 1.1 注册邮箱域名后缀白名单配置缺失治理
在平台后端鉴权模块 [`AuthServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/service/impl/AuthServiceImpl.java#L117-L129) 的 `register` 函数中，系统支持根据系统配置项 `SystemSettingKeys.AUTH_EMAIL_WHITELIST_SUFFIXES` (`system.auth.email_whitelist_suffixes`) 进行邮箱域名白名单校验：
```java
String whitelistStr = systemSettingService.getSettingValue(SystemSettingKeys.AUTH_EMAIL_WHITELIST_SUFFIXES, "");
if (StringUtils.hasText(whitelistStr)) {
    String emailDomain = normalizedEmail.substring(normalizedEmail.lastIndexOf("@") + 1);
    boolean isWhitelisted = Arrays.stream(whitelistStr.split(","))
            .map(String::trim)
            .map(String::toLowerCase)
            .filter(StringUtils::hasText)
            .map(suffix -> suffix.startsWith("@") ? suffix.substring(1) : suffix)
            .anyMatch(allowedDomain -> emailDomain.equals(allowedDomain) || emailDomain.endsWith("." + allowedDomain));
    if (!isWhitelisted) {
        throw new BusinessException(HttpStatus.BAD_REQUEST, "不支持该邮箱后缀注册，仅限: " + whitelistStr);
    }
}
```
经过对前端工程的全局检索审计，发现：
- 在系统设置后台 [`AdminSystemView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/system/AdminSystemView.vue) 以及基础设置标签页 [`GeneralSettingsTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/system/tabs/GeneralSettingsTab.vue) 中，**未提供任何针对该白名单项的查看或编辑入口**；
- 在发信模块 [`EmailServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/service/impl/EmailServiceImpl.java#L117) 中，虽然也进行了白名单校验，但此前使用了硬编码字符串 `"system.auth.email_whitelist_suffixes"` 且匹配方式仅使用了简单的 `endsWith`，存在无法支持多级子域名（如 `stu.edu.cn` 匹配 `edu.cn`）的不一致缺陷。

### 1.2 人机验证与风控管理边界分离与收敛
此前，平台在系统设置（`AdminSystemView.vue`）中提供了 Tab 2“人机验证与风控”（`CaptchaSettingsTab.vue`）。而平台现已建立完整的“风控管理”一级模块（`/admin/risk`，包含设置、策略与 IP 治理）。将人机验证孤立地保留在系统设置中，造成了安全与风控配置的职责割裂和管理分散。

### 1.3 `CaptchaSDK` 与 `RiskEvaluationEngine` 核心语义区分与联动模型
在深入分析中，明确区分了两个关键组件及对应开关控制的生命周期：
1. **`RiskEvaluationEngine` (风控研判层，受 `system.risk.enabled` 控制)**：
   - 基于 Redis 滑动窗口限流器，维护 IP 和账号的连续失败计数与突发访问速率；
   - 负责对请求进行动态安全研判，输出 `LOW` / `MEDIUM` / `HIGH` 三阶风险等级，并在严重时触发 `RiskAutoBanEvent` 自动封禁；
   - 若关闭此引擎，所有请求的风险判定直接降为 `LOW`，不再统计滑动窗口攻击频次，不再发生自适应安全提级与自动封禁。
2. **`CaptchaSDK` (安全编排与执行门禁层，受 `system.captcha.enabled` 与综合风控等级联动控制)**：
   - 作为所有敏感业务模块（登录、注册、注销、发信）的统一出入口；
   - **当关闭 `system.captcha.enabled` 时，并非关闭 CaptchaSDK，更不是关闭 Risk Engine**：
     - 对于良性常态流量（`LOW` 风险），系统直接判定为免验通行（`mode = "NONE"`），不弹出任何验证码，大幅提升正常用户体验；
     - **但风控引擎仍在后台巡检**，一旦某个 IP 或账号遭遇恶意爆破达到 `MEDIUM` 或 `HIGH` 阈值，`CaptchaSDK` 将**瞬时激活自适应挑战拦截**（强制弹出滑块拼图或带外邮件 OTP）；
     - 因此，关闭人机验证开关在业务语义上代表**“进入被动智能防御模式”**；若要彻底停用所有风控和频次监测，则需同时关闭风控引擎开关（`system.risk.enabled`）。

---

## 2. 具体改动点与实施清单

### 2.1 后端工程改造 (Java & Spring Boot)
1. **风控管理基础设置领域模型扩充**：
   - 在 [`AdminRiskSettingsVO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/domain/vo/AdminRiskSettingsVO.java) 中补充人机验证参数（`captchaEnabled`, `captchaTolerance`, `captchaTicketTtl`, `captchaMaxAttempts`, `forceCaptchaOnReset`），遵守 `@Builder.Default` 规范；
   - 在 [`AdminRiskSettingsDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/domain/dto/AdminRiskSettingsDTO.java) 中补充对应字段，严格添加 Jakarta Validation 约束（`@NotNull`, `@Min`, `@Max`）。
2. **风控服务持久化实现**：
   - 在 [`AdminRiskServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/service/admin/impl/AdminRiskServiceImpl.java) 的 `getSettings()` 与 `updateSettings()` 中，将人机验证的 5 项配置统一纳入读取与保存事务中。
3. **风控引擎与发信模块边界加固**：
   - 在 [`RiskEvaluationEngine.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/risk/engine/RiskEvaluationEngine.java) 的 `evaluateAccountRisk` 中补齐 `if (!isRiskEnabled()) return RiskLevel.LOW;`，使全局风控关闭时账号级风控亦严格保持一致；
   - 在 [`EmailServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/service/impl/EmailServiceImpl.java) 中消除硬编码字符串，引用 `SystemSettingKeys.AUTH_EMAIL_WHITELIST_SUFFIXES`，并将邮箱域名匹配逻辑与 `AuthServiceImpl.java` 100% 对齐（支持全等及 `.domain` 子域名匹配）；
   - 在 [`SystemSettingInitializer.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/init/SystemSettingInitializer.java) 中补齐 `SystemSettingKeys.AUTH_EMAIL_WHITELIST_SUFFIXES` 的默认值注册。
4. **单测用例覆盖**：
   - 在 [`AdminRiskServiceTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-verification/src/test/java/com/humolix/volur/verification/AdminRiskServiceTest.java) 中补充 `testGetSettingsWithCaptcha` 与 `testUpdateSettingsWithCaptcha` 单元测试，保证回归质量。

### 2.2 前端工程改造 (Vue 3 & TypeScript)
1. **类型定义对齐**：
   - 更新 [`src/volur-lite-view/src/types/risk/index.ts`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/types/risk/index.ts)，将 `AdminRiskSettingsVO` 与 `AdminRiskSettingsDTO` 补充人机验证字段。
2. **风控设置视图收敛与重构**：
   - 重构 [`AdminRiskSettingsView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/risk/AdminRiskSettingsView.vue)，在单一卡片中清晰拆分为：
     - **智能风控引擎与网络防御**：全局风控总开关、IP 阻断、本地回环放行、默认封禁时长、拦截提示语；
     - **人机验证与行为挑战**：常态人机验证开关、滑块容差、凭证 TTL、最大重试次数、重置密码强制验证；
   - 增加详尽的参数提示说明，清晰引导管理员理解“常态人机验证”与“智能风控引擎”的联动关系。
3. **系统设置精简与邮箱白名单配置**：
   - 移除 [`AdminSystemView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/system/AdminSystemView.vue) 中的 `CaptchaSettingsTab` 标签页引用及对应的持久化字段；
   - 在 [`GeneralSettingsTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/system/tabs/GeneralSettingsTab.vue) 中新增“注册邮箱白名单后缀”配置输入项（`emailWhitelistSuffixes`），并在 `AdminSystemView.vue` 的 `fetchSettings`、`handleSaveSettings` 与 `handleResetDefault` 中完成双向绑定与保存；
   - 彻底删除已无引用依赖的 [`CaptchaSettingsTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/system/tabs/CaptchaSettingsTab.vue)。
4. **国际化支持**：
   - 在 `zh_CN/admin/risk.json` 与 `en_US/admin/risk.json` 中补齐人机验证及区域标题国际化；
   - 在 `zh_CN/admin/system.json` 与 `en_US/admin/system.json` 中补充邮箱白名单配置项文案，调整标签页文案为“平台运维与系统限流”。

---

## 3. 验证结果与影响评估

1. **后端验证**：
   - 执行 `mvn test -pl volur-verification` 全部 27 个单元测试 100% 通过；
   - 执行 `mvn test -pl volur-auth -Dtest=AuthVerificationCrossValidationTest` 7 个交叉验证单测 100% 通过；
   - 执行全平台 `mvn test-compile -DskipTests` 构建成功，无编译错误。
2. **前端验证**：
   - 执行 `npm run build`（包含 `vue-tsc --noEmit` 类型静态检查与 Vite 打包），在 6.28s 内构建成功，无任何 TS 类型错误。
3. **影响评估**：
   - 数据库表结构无需变更，已有系统设置键名天然保持幂等向后兼容；
   - 权限与路由层级无破坏性变更，管理员在“风控管理 -> 风控设置”中即可一站式配置风控引擎与人机验证挑战的所有参数。
