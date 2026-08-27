# 系统设置与运行健康概览逻辑统一改造文档

**文档编号**：ARCH-20260827-SYS-OVERVIEW  
**日期**：2026-08-27  
**作者**：Antigravity Agent  
**模块范围**：`volur-system`、`volur-lite-view` (AdminSystemView)  

---

## 1. 改造背景

在后台管理端系统设置页面（[`AdminSystemView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/admin/system/AdminSystemView.vue)）中，原先存在以下逻辑脱节与体验缺陷：
1. **系统运行概览指标硬编码**：顶部的「系统内核版本」、「运行底层环境」、「Redis 缓存状态」以及「实时 SSE 推送总线」等关键指标在前端模板写死，未与后端实时健康状态联动；
2. **按钮文本设计冗长**：「开放新用户注册」等开关按钮显示了冗长的中文字符串（如“允许公开自由注册”），不符合简洁直观的后台 UI 设计规范；
3. **配置键名与业务层解耦不一致**：业务模块（如 `volur-auth`、`volur-verification`）使用的常量配置项与前端表单的点分键名缺乏自动化双向兼容机制；
4. **SMTP 自检为模拟逻辑**：发信自检此前仅有前端 `setTimeout` 假数据，未打通后端真实 SMTP 探活接口。

---

## 2. 核心架构设计与改动点

### 2.1 后端运行健康与底层环境概览接口 (`volur-system`)
- **新增 VO 模型**：[`AdminSystemOverviewVO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/domain/vo/AdminSystemOverviewVO.java)
  - `kernelVersion`: 系统内核版本 (`Volur Lite v1.0.0-SNAPSHOT`)；
  - `springBootVersion`: `org.springframework.boot.SpringBootVersion.getVersion()`；
  - `javaVersion`: `System.getProperty("java.version")` + `System.getProperty("java.vendor")`；
  - `runtimeEnv`: `Spring Boot 3.4.3 / JDK 25`；
  - `osInfo`: `System.getProperty("os.name")` + `os.version` + `os.arch`；
  - `redisStatus` & `redisLatencyMs`: 通过 `redisTemplate.getConnectionFactory().getConnection().ping()` 探测 Redis 实时连接与网络/执行延迟；
  - `sseStatus`: SSE 广播总线状态；
  - `uptimeSeconds` / `heapMemoryUsedMb` / `heapMemoryMaxMb`: 运行时 JVM 堆内存与开机运行时长。
- **控制器端点**：[`AdminSystemSettingController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/controller/v1/admin/AdminSystemSettingController.java)
  - 新增 `GET /api/v1/admin/system/settings/overview`。

### 2.2 配置键名智能别名互通机制与参数校验修复
- **参数校验修复**：
  - 在 [`SystemSettingUpdateDTO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/domain/dto/SystemSettingUpdateDTO.java) 中，原 `value` 字段标注了 `@NotBlank`。当用户未填写 ICP 备案号或密码等允许为空的配置项时（传递空字符串 `""`），触发了 Spring 校验失败（`HandlerMethodValidationException`）。
  - 将 `value` 字段由 `@NotBlank` 修改为 `@NotNull`，允许配置项保存合法空字符串值；并在服务层 `SystemSettingServiceImpl.updateSetting` 中确保非空安全回退 (`val != null ? val : ""`)。
- **全局异常捕获增强**：
  - 在 [`GlobalExceptionHandler.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-framework/src/main/java/com/humolix/volur/framework/common/exception/handler/GlobalExceptionHandler.java) 中补充了对 Spring 6 / Spring Boot 3 `HandlerMethodValidationException` 的标准捕获与友好 400 提示解析，彻底避免未受检异常退化为 500 内部错误。
- **双向别名机制**：
  - 在 [`SystemSettingServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/service/impl/SystemSettingServiceImpl.java) 中引入 `KEY_ALIASES` 映射表：
  - 自动将 `REGISTER_ENABLED` 与 `system.auth.registration_enabled`、`SMTP_HOST` 与 `system.mail.smtp_host`、`FLAG_PREFIX` 与 `system.challenge.flag_prefix` 等进行双向绑定；
  - 查询与更新时自动级联查询与失效缓存，彻底消除业务代码与设置界面键名不一致的隐患。

### 2.3 前端视图重构与 UI 统一 (`AdminSystemView.vue`)
- **动态健康概览绑定**：
  - 引入强类型 `AdminSystemOverviewVO`，通过 `getAdminSystemOverviewApi()` 实时渲染内核版本、环境信息、Redis 延迟及状态样式（`.status-online` / `.status-offline`）；
- **开关按钮规范化为 ON / OFF**：
  - 开放新用户注册：`<template #checked>ON</template><template #unchecked>OFF</template>`；
  - 重置密码强制人机验证：`ON / OFF`；
  - SSL / TLS 加密传输：`ON / OFF`；
  - 全站维护模式开关：`ON / OFF`；
  - 在各项表单右侧添加优雅的说明文本（`.field-hint`）。
- **真实 SMTP 连通性测试**：
  - 接入 [`testSmtpConnectionApi()`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/api/system/index.ts) (`POST /api/v1/email/test`)，实现真实的邮件服务联通性自检。

---

## 3. 验证结果

1. **后端编译**：`mvn clean test-compile -pl volur-system -am` -> **BUILD SUCCESS**
2. **前端编译与类型检查**：`vue-tsc --noEmit && vite build` -> **BUILD SUCCESS**
