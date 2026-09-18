# AdminUserView 用户管理视图国际化 (i18n) 全面完善施工记录

**施工日期**：2026-09-18  
**施工模块**：`src/volur-lite-view/src/views/admin/user/AdminUserView.vue`、`src/volur-lite-view/src/locales/zh_CN/admin/users.json`、`src/volur-lite-view/src/locales/en_US/admin/users.json`  
**责任主体**：Antigravity AI Agent

---

## 1. 变更背景与目标

为满足 Volur Lite 全平台多语言国际化标准，对后台用户管理页面（`AdminUserView.vue`）进行彻底的 i18n 改造：
1. **消除硬编码**：全量清理视图模板、表格列定义、下拉筛选器、表单校验规则、操作反馈信息及模态框中的硬编码中文；
2. **完善语言包**：丰富补齐 `zh_CN/admin/users.json` 与 `en_US/admin/users.json`，确保词汇专业、术语严谨对齐；
3. **响应式国际化支持**：将数据表格列定义（`columns`）、下拉选项（`roleSelectOptions`、`statusSelectOptions` 等）及表单规则（`userFormRules`）全面采用 `computed` 封装，确保语言切换时表格与表单内容无缝即时重渲染。

---

## 2. 改造范围与详细改动

### 2.1 语言包定义与词条扩充
在 `src/volur-lite-view/src/locales/` 下的中英文语言包中，按层级补充结构化定义：
- **概览与统计**：`stat_total_users`、`stat_admin_count`、`stat_active_count`、`stat_banned_count`；
- **筛选工具栏**：`search_placeholder`、`filter_role`、`filter_status`、`start_time`、`end_time`、`query_btn`、`reset_btn`、`create_btn` 以及各枚举项翻译；
- **数据表格与分页**：`total_prefix`、`email_empty` 以及 `columns` 字段映射（`id`、`user`、`org`、`role`、`status`、`created_at`、`actions`）；
- **角色与状态标签**：`role_tag_super_admin`、`role_tag_admin`、`role_tag_user`、`status_active_short`、`status_banned_short`；
- **模态框与表单**：`modal` 对象（创建/编辑标题、各字段 Label 与 Placeholder、开关提示、取消与保存按钮）；
- **表单校验规则**：`validation` 对象（用户名必填与长度、邮箱必填与格式校验、密码长度校验）；
- **交互与操作反馈**：`actions` 对象（编辑、删除二次确认文案）及 `messages` 对象（状态切换、更新、创建、删除的成功与失败 Toast）。

### 2.2 组件代码改造 (`AdminUserView.vue`)
1. **脚本引入与状态初始化**：
   - 引入 `useI18n` 并获取 `{ t, locale }`；
   - 将 `roleSelectOptions`、`statusSelectOptions`、`roleModalOptions` 改造为 `computed<SelectOption[]>`；
   - 分页前缀配置改造为 `(info) => t('admin.users.total_prefix', { count: info.itemCount || 0 })`。
2. **表格列定义全面动态化**：
   - 将 `const columns` 声明为 `computed<DataTableColumns<AdminUserDetailsVO>>`；
   - 列标题及内部单元格渲染全部接入 `t('admin.users.columns.*')`；
   - 角色标签由硬编码字符串改为 `t('admin.users.role_tag_*')`；
   - 状态开关旁文字接入 `t('admin.users.status_*_short')`；
   - 注册时间格式化接入 `date.toLocaleString(locale.value === 'en_US' ? 'en-US' : 'zh-CN', ...)`；
   - Popconfirm 提示文案使用参数插值：`t('admin.users.actions.delete_confirm', { username: row.username })`。
3. **表单验证规则响应式改造**：
   - 将 `userFormRules` 改造为 `computed<FormRules>`，使错误提示文案在语言切换时动态刷新。
4. **模态框与异步方法全面改造**：
   - 模态框标题与全部 FormItem 的 `label`、`placeholder` 绑定为 `t(...)`；
   - `handleToggleStatus`、`handleSubmitUserForm`、`handleDeleteUser` 提示信息全部替换为 `t('admin.users.messages.*')`。

---

## 3. 验证与构建测试

1. **中文硬编码全量扫描**：
   通过正则脚本对 `AdminUserView.vue` 进行了代码行扫描，排查结果为 0 处残留硬编码中文。
2. **编译打包验证**：
   在 `src/volur-lite-view` 执行：
   ```bash
   npm run build
   ```
   **构建结果**：
   - `vue-tsc --noEmit`：0 错误，类型检查完全通过；
   - `vite build`：打包耗时 5.95s，顺利产出生产部署文件。

---

## 4. 影响范围与审计评估

- **改动文件**：
  - [`src/volur-lite-view/src/locales/zh_CN/admin/users.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/zh_CN/admin/users.json)
  - [`src/volur-lite-view/src/locales/en_US/admin/users.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/en_US/admin/users.json)
  - [`src/volur-lite-view/src/views/admin/user/AdminUserView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/user/AdminUserView.vue)
- **体验提升**：
  - 后台用户管理视图达成 100% 国际化支持；
  - 切换语言时无需刷新页面，表格列头、筛选栏、操作列及对话框均实现无缝响应式更新。
