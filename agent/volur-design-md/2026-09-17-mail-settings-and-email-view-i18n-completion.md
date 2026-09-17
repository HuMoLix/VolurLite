# 邮件系统设置与邮件管理视图 i18n 国际化键名完善与规范化重构

## 1. 变更背景

在先前的邮件系统投递 Webhook 与细粒度状态机升级中，管理端系统设置选项卡（`MailSettingsTab.vue`）与邮件管理详情视图（`AdminEmailView.vue`）中引入了复制 Webhook 地址及原始邮件内容等功能。
由于 `common.json` 语言包中仅收录了 `copy_success`，缺少规范的 `copy`、`close` 与 `copied_to_clipboard` 标准通用词条，导致前端模板和方法中采用了 `{{ t('common.copy') || '复制' }}`、`{{ t('common.close') || '关闭' }}` 以及 `message.success(t('common.copied_to_clipboard') || '...')` 等行内硬编码回退写法。

为彻底消除行内硬编码字符串，满足 `view-Rules.md` 与 `GEMINI.md` 的国际化完整覆盖要求，本次对 `common.json` 国际化资源进行了补充，并统一重构了相关组件的引用。

---

## 2. 改动清单

### 2.1 语言包补充 (`src/volur-lite-view/src/locales/`)
- **`zh_CN/common.json`**：
  - 新增 `"close": "关闭"`
  - 新增 `"copy": "复制"`
  - 新增 `"copied_to_clipboard": "已复制到剪贴板"`
- **`en_US/common.json`**：
  - 新增 `"close": "Close"`
  - 新增 `"copy": "Copy"`
  - 新增 `"copied_to_clipboard": "Copied to clipboard"`

### 2.2 系统配置邮件选项卡 (`MailSettingsTab.vue`)
- **清除行内回退**：
  - 复制 Webhook 按钮文本：由 `{{ t('common.copy') || '复制' }}` 规范为 `{{ t('common.copy') }}`。
  - 复制成功 Toast 提示：由 `message.success(t('common.copied_to_clipboard') || 'Webhook 回调地址已复制')` 规范为 `message.success(t('common.copied_to_clipboard'))`。

### 2.3 邮件管理详情抽屉 (`AdminEmailView.vue`)
- **清除行内回退**：
  - 复制原始邮件文本按钮：由 `{{ t('common.copy') || '复制' }}` 规范为 `{{ t('common.copy') }}`。
  - 抽屉底部关闭按钮：由 `{{ t('common.close') || '关闭' }}` 规范为 `{{ t('common.close') }}`。
  - 复制原始内容 Toast 提示：由 `window.$message.success(t('common.copied_to_clipboard') || '已复制到剪贴板')` 规范为 `window.$message.success(t('common.copied_to_clipboard'))`。

---

## 3. 规范对齐与安全分析

1. **多语言单一真实源**：完全由 `vue-i18n` 统一驱动，消除模板及业务脚本中的行内 fallback 逻辑，支持中英文平滑无缝切换。
2. **前后端与视图规范**：严格遵从 `GEMINI.md` 与 `view-Rules.md` 约束，无 `any` 类型引入，无多余日志与未清理依赖。
3. **构建验证**：前端执行 `npm run build` 全部打包成功，无 TypeScript 校验错误与静态资源冲突。
