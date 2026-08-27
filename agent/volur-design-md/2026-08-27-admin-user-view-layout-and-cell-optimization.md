# 用户管理视图布局精简与单元格排版优化 (AdminUserView Optimization)

## 1. 变更背景

在用户管理视图（`AdminUserView.vue`）中，对顶部统计色带与用户列表表格“用户”单元格的展示效果进行全面精细化重构：
1. 顶部统计色带轻量化排开在同一行，节省首屏空间；
2. 用户单元格采用现代左右结构展示：左侧为 34px 圆润头像，右侧为水平并排的“用户名 + 邮箱胶囊标签”；
3. 邮箱采用自适应浅灰徽章气泡（`.user-email-badge`），既提供弱化辅助色的清晰层级，又避免与主文字黏连；
4. 针对空邮箱场景提供优雅的斜体缺省提示（`未设置邮箱`），并在悬浮时支持全文本 Native Tooltip。

---

## 2. 改动内容与设计要点

### 2.1 顶部数据精简与单行紧凑排开
- 将原有的粗重 `n-grid` + `n-card` 结构重构为轻量级 **统计色带（Stat Ribbon）**；
- 4 个核心统计指标（系统总用户、管理员数、正常启用、已封禁）以紧凑规整的轻量级小卡片形式排在同一行（高度压缩至 58px）；
- 极大地节约了首屏垂直空间，使用户管理列表与筛选栏作为整个页面的绝对视觉主体。

### 2.2 用户单元格视觉展示效果深度优化
- **左右分栏排版**：
  - 左侧：34px 精致头像（`NAvatar`），圆润微阴影与渐变底色降级支持；
  - 右侧：水平单行容器（`user-info-row`），用户名与邮箱徽章水平并排。
- **现代化邮箱微胶囊设计**：
  - 采用 `user-email-badge` 样式（`padding: 1px 8px; border-radius: 4px; background-color: var(--n-tag-color, rgba(125,125,125,0.08)); color: #666; font-size: 12px;`）；
  - 悬浮时微高亮反馈，限制 `max-width: 200px` 并自动省略过长文本；
  - 支持 `title` 悬浮 Tooltip 提示完整用户名与邮箱。
- **缺省状态优雅降级**：当未绑定邮箱时展示 `user-email-empty`（`未设置邮箱`）。
- **列宽保障**：用户列 `minWidth: 280px`，ID 列保持 `180px`。

---

## 3. 验证与质量测试

1. **类型与语法检查**：
   - 运行 `npx vue-tsc --noEmit`：0 错误，0 告警；
2. **构建打包测试**：
   - 运行 `npm run build`：Vite 编译打包成功（5.58s）。

---

## 4. 涉及文件清单

- **修改**：
  - `src/volur-lite-view/src/views/admin/user/AdminUserView.vue`
- **归档**：
  - `agent/volur-design-md/2026-08-27-admin-user-view-layout-and-cell-optimization.md`
