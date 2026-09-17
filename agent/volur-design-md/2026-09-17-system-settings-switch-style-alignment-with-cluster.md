# 系统设置视图开关组件样式对齐与微调施工文档

## 1. 变更背景

在管理后台中，系统设置模块（`AdminSystemView.vue` 及其下属 tabs）与集群设置模块（`AdminClusterSettingsView.vue` 及其下属 `ParamsTab.vue`、`ClusterNodeModal.vue`）对开关控件（`n-switch`）的使用存在风格不一致问题。
通过对二者开关样式的深入对比分析，将系统设置中的开关样式全面向集群设置对齐，提升后台配置界面的视觉统一感与精致度。

---

## 2. 两种开关按钮样式差异分析

| 维度 | 系统设置（原样式） | 集群设置（参考规范） |
| :--- | :--- | :--- |
| **内部文本插槽** | 包含 `<template #checked>ON</template>` 与 `<template #unchecked>OFF</template>` 插槽，滑块轨道内部强制渲染英文单词。 | 不包含任何内部文本插槽，直接自闭合 `<n-switch v-model:value="..." />`。 |
| **控件尺寸与形态** | 因需容纳 ON/OFF 文本，开关滑轨宽度被拉宽变长，显得厚重和拥挤，且英文与中文系统语境不协调。 | 保持 Naive UI 原生标准药丸尺寸，纯净无字，小巧轻盈，滑块平滑流畅。 |
| **布局容器与垂直对齐** | `<n-switch>` 与 `<span class="field-hint">` 直接平铺在 `<n-form-item>` 内部，易受表单基线和行高影响出现细微错位。 | 使用 `<div class="switch-row">`（配置 `display: flex; align-items: center;`）将开关与提示包裹在统一的 Flex 容器内，实现绝对垂直居中对齐。 |

---

## 3. 详细改动清单

### 3.1 下属 Tabs 开关微调 (`src/volur-lite-view/src/views/admin/system/tabs/`)

1. **`GeneralSettingsTab.vue`**：
   - 开放新用户注册开关（`form.registrationEnabled`）：移除了 `#checked` 与 `#unchecked` 插槽，将开关与提示包裹在 `<div class="switch-row">` 中；
   - 样式表增加 `.switch-row { display: flex; align-items: center; }`。

2. **`CaptchaSettingsTab.vue`**：
   - 全站人机验证总开关（`form.captchaEnabled`）与重置密码强制人机验证开关（`form.forceCaptchaOnReset`）：移除内部 ON/OFF 插槽，使用 `<div class="switch-row">` 统一包裹；
   - 样式表增加 `.switch-row { display: flex; align-items: center; }`。

3. **`MailSettingsTab.vue`**：
   - SSL/TLS 加密传输开关（`form.smtpSsl`）与启用 Webhook 回调开关（`form.webhookEnabled`）：移除内部 ON/OFF 插槽，使用 `<div class="switch-row">` 包裹；
   - 样式表增加 `.switch-row { display: flex; align-items: center; }`。

4. **`MaintenanceSettingsTab.vue`**：
   - 全站维护模式开关（`form.maintenanceMode`）：移除内部 ON/OFF 插槽，使用 `<div class="switch-row">` 统一包裹；
   - 样式表增加 `.switch-row { display: flex; align-items: center; }`。

5. **`AdminSystemView.vue`**：
   - 在 `.settings-card` 样式层级下增加 `.switch-row { display: flex; align-items: center; }`，确保宿主容器样式完备。

---

## 4. 验证结果

- **前端打包编译**：执行 `npm run build` 成功完成，耗时 5.34s，无任何编译错误或样式警告。
- **全平台一致性**：系统设置中 6 处核心开关按钮现与集群设置保持完全一致的轻量无字设计和居中布局。
