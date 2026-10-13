# 管理端用户视图表格样式精简与 VAvatar 审计方案

## 1. 变更背景与诉求

在 `src/volur-lite-view-NEXT` 项目的 `AdminUserView.vue`（管理端用户管理视图）中，用户列表表格（`VTable`）的部分列渲染定义了冗余的行内类名与徽章胶囊样式：
1. **ID 列**：原渲染包含 `font-mono` 与 `whitespace-nowrap`，强行引入等宽字体与 monospace 风格，破坏了与平台统一 Inter 字体的整体协调；
2. **角色身份列**：原渲染包含 `inline-flex px-2 py-0.5 text-xs font-medium rounded-full ${badgeClz}` 胶囊样式与彩底徽章背景；
3. **账号状态列**：原渲染同样包含 `inline-flex px-2 py-0.5 text-xs font-medium rounded-full ${badgeClz}` 胶囊徽章；
4. **所属组织列**：包裹了冗余的 `h('span', { class: 'text-sm text-google-text-desc dark:text-google-text-darkDesc' })` 元素；
5. **用户列下项头像规范确认**：需要全面核查确保用户列下的头像渲染严格采用平台统一的自研组件 `VAvatar`（`@/components/sidebar/VAvatar.vue`）。

为了消除多余的胶囊与等宽字体样式定义，使表格单元格完全、直接继承 `VTable.vue` 自带的设计系统（Inter 字体族、字号、字重与 Google 浅暗灰度调色板），需要对表格渲染列定义实施精简重构。

---

## 2. 变更内容与改动点

### 2.1 删除多余样式定义并直接继承 VTable

- **ID 列**：
  - 移除 `h('span', { class: 'whitespace-nowrap font-mono' }, String(row.id))`；
  - 统一直接返回 `String(row.id)`，完全继承 `VTable.vue` 内置的 `Inter, sans-serif` 与默认单元格颜色、字重。
- **角色身份列**：
  - 移除全部胶囊类名 `inline-flex px-2 py-0.5 text-xs font-medium rounded-full` 与 `badgeClz` 相关的背景色/文字色切换；
  - 直接返回国际化角色名称字符串（`超级管理员` / `管理员` / `普通用户`），由 `VTable` 单元格统一样式渲染。
- **账号状态列**：
  - 移除全部胶囊类名 `inline-flex px-2 py-0.5 text-xs font-medium rounded-full` 与徽章类名；
  - 直接返回国际化状态文本（`正常` / `封禁`）。
- **所属组织列**：
  - 简化为直接返回 `row.org || '-'` 纯文本，移除外层冗余的 `span` 与色彩类。

### 2.2 用户列下项组件审计

- 确认 `AdminUserView.vue` 中用户列（`username`）的渲染结构：
  - 头像组件明确引入并调用 `@/components/sidebar/VAvatar.vue`；
  - 正确传递 `src: row.avatarUrl || ''`、`text: (row.username || '').charAt(0).toUpperCase()`、`size: 32`、`shape: 'circle'` 等规范属性；
  - 下方两行文本紧凑展示用户名（Primary 色阶）与邮箱（Muted 色阶），层次清晰且自适应容器省略截断。

---

## 3. 影响范围

- **修改文件**：
  - `src/views/admin/AdminUserView.vue`
- **影响视图**：
  - 管理后台 - 用户管理页面 (`/admin/users`) 的用户列表数据表格。
- **无破坏性影响**：
  - 所有操作按钮（编辑、封禁/解封、删除）与数据绑定逻辑均保持不变，业务功能与交互 100% 正常。
