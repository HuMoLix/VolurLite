# 管理后台核心视图（系统设置、审计日志、公告广播）国际化 (i18n) 全面改造文档

## 1. 变更背景与目标
在 Volur Lite 国际化规范演进中，管理后台三大核心运维控制视图——**系统设置 (`AdminSystemView.vue`)**、**系统日志 (`AdminLogView.vue`)** 与 **公告管理 (`AdminBroadcastView.vue`)** 仍存在大量静态硬编码中文字符串（包括指标看板文字、表单标签与占位符、动态提示语、操作按钮、表格表头及 Drawer/Modal 弹窗详情等）。

本次任务旨在依据 [`GEMINI.md`](file:///H:/HuMoLix/github/Volur-Lite/GEMINI.md) 及 [`view-Rules.md`](file:///H:/HuMoLix/github/Volur-Lite/agent/volur-design-skills/view-Rules.md) 规范：
1. 全面补全并规范 `src/volur-lite-view/src/locales/zh_CN/admin.json` 与 `src/volur-lite-view/src/locales/en_US/admin.json` 中的 `system`、`logs` 与 `broadcasts` 国际化词条；
2. 彻底重构三个视图组件，引入 Vue I18n `const { t } = useI18n()`，替换所有硬编码文本；
3. 将表格列定义 (`columns`)、下拉选项 (`SelectOptions`) 及表单校验规则 (`formRules`) 统一收敛为响应式 `computed` 属性，确保在前台/后台切换中英文时无需刷新页面即可实时无缝热重载所有文案。

---

## 2. 变更详情

### 2.1 语言包完善与对齐
- **`zh_CN/admin.json` 与 `en_US/admin.json`**：
  - **`admin.system`**：
    - `overview`：系统内核、底层环境、Redis 延迟动态插值及 SSE 总线在线状态文本；
    - `tabs`：平台基础设置、人机验证与风控、邮件服务配置 (SMTP)、容器环境与动态靶机、平台运维与风控 5 大 Tab 名称；
    - `general` / `captcha` / `mail` / `container` / `maintenance`：所有字段 label、placeholder 及详细 field-hint 说明；
    - `actions` / `messages`：重置、保存配置、SMTP 自检等操作与反馈 Toast 消息。
  - **`admin.logs`**：
    - `stat_*`：总日志流水、认证鉴权、安全拦截、系统异常 4 项指标卡片名称；
    - `search_placeholder` / `filter_*`：搜索与全级别、全模块、全状态下拉选项文本；
    - `columns`：表格所有列名（ID、级别、模块、动作、操作者、耗时、结果、记录时间、操作）；
    - `drawer_title` / `detail.*`：日志审计抽屉中的详情条目、JSON 载荷、堆栈详情与 UA 展示名称；
    - `total_prefix`：分页总条数动态插值前缀。
  - **`admin.broadcasts`**：
    - `stat_*`：全站系统公告、运营活动与推广、紧急弹窗广播、SSE 实时推送总线 4 项指标；
    - `category_*` / `display_type_*`：分类及弹窗/横幅/浮层/抽屉展示形式完整和简写映射；
    - `columns` / `actions`：表格列名、操作按钮及删除确认二次气泡弹窗文本；
    - `modal`：发布公告/活动弹窗的所有表单 label、placeholder、验证规则 (formRules) 与提交按钮；
    - `preview` / `messages`：预览弹窗和推送/删除反馈消息。

### 2.2 组件改造与响应式优化
1. **`src/volur-lite-view/src/views/admin/system/AdminSystemView.vue`**：
   - 引入 `useI18n`，全面替换看板、Tab 页、表单项与底部保存栏文本；
   - 将 `localeOptions` 改为 `computed`，动态呈现 "简体中文 (zh_CN)" / "English (en_US)"；
   - 对保存成功、失败、自检与重置 Toast 消息接入统一 `t('admin.system.messages.*')`。
2. **`src/volur-lite-view/src/views/admin/log/AdminLogView.vue`**：
   - 引入 `useI18n`，将 `levelSelectOptions`、`moduleSelectOptions`、`statusSelectOptions` 及 `columns` 全部转为 `computed` 计算属性；
   - 抽屉面板的所有字段标签与载荷区域标题接入 `t('admin.logs.detail.*')`；
   - 时间格式化函数 `formatDateTime` 适配当前语言环境（`zh-CN` / `en-US`）。
3. **`src/volur-lite-view/src/views/admin/broadcast/AdminBroadcastView.vue`**：
   - 引入 `useI18n`，将 `categoryOptions`、`displayTypeOptions`、`modalDisplayTypeOptions`、`columns` 及 `formRules` 全部重构为 `computed` 计算属性；
   - 弹窗标题根据 `modalCategory` 动态国际化展示；
   - 气泡确认提示 (`delete_confirm`)、预览详情抽屉与推送反馈消息全面接入 i18n。

---

## 3. 安全与质量验证
- **构建与类型检查**：在 `src/volur-lite-view` 执行 `npm run build` (`vue-tsc --noEmit && vite build`)，结果为 **Zero Errors (0 Exit Code)**，无任何 TypeScript 类型断言或缺失。
- **动态语言热切换**：所有下拉选项、表格表头和校验规则由于使用了 `computed` 均能响应式跟踪语言切换，杜绝了硬编码导致的界面语言残留。

---

## 4. 影响范围
- [`src/volur-lite-view/src/locales/zh_CN/admin.json`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/locales/zh_CN/admin.json)
- [`src/volur-lite-view/src/locales/en_US/admin.json`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/locales/en_US/admin.json)
- [`src/volur-lite-view/src/views/admin/system/AdminSystemView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/admin/system/AdminSystemView.vue)
- [`src/volur-lite-view/src/views/admin/log/AdminLogView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/admin/log/AdminLogView.vue)
- [`src/volur-lite-view/src/views/admin/broadcast/AdminBroadcastView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/views/admin/broadcast/AdminBroadcastView.vue)
