# 竞技场布局顶部栏优化与规范对齐施工审计文档

## 1. 变更背景与目标
在完成 `VolurHeader.vue` 平台级顶部导航组件的封装与 `AppLayout` / `AdminLayout` 的对齐之后，前台竞技场布局组件 [`CompetitionLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/CompetitionLayout.vue) 顶部栏存在以下体验与规范问题：
1. **右侧排版与组件风格未对齐**：右侧用户资料、中英文语言切换与日夜主题切换按钮的间隔、按钮样式及头像/昵称结构与 `VolurHeader` 标准规范存在偏差（例如旧语言切换使用的是带图标的小按钮而非标准纯文本圆形按钮 `EN`/`中`；主题切换未采用 18px 图标与标准圆形按钮；用户资料项样式与间距不一致）；
2. **语言切换存储键名不一致**：旧代码将语言偏好持久化在 `volur_locale`，而全平台其他模块统一使用 `locale`，导致竞技场中切换语言后刷新或跳转时状态脱节；
3. **左侧信息过密与冗余**：左侧竞技场标题旁展示了“CTF 解题赛”类型标签与“进行中”状态标签，导致在非超宽屏下占用大量空间，分散选手视觉重心；
4. **背景颜色与主题未对齐平台铁律**：顶部栏高度为 60px（平台标准为 64px），日间与夜间背景色及下边框颜色未严格对齐 `VolurSidebar` 与 `VolurHeader` 标准配色（日间纯白 `#ffffff`，夜间曜黑 `#121826`）。

---

## 2. 核心改动点说明

### 2.1 移除左侧竞赛类型与状态标签
- **模板层**：从 `.header-left` 中彻底移除了 `<div class="badges-wrap">`（包含 `typeTagType` / `typeLabel` 与 `statusTagType` / `statusLabel` 两个 `<n-tag>` 标签）；
- **脚本层**：
  - 移除了从 `naive-ui` 导入的 `NTag`；
  - 移除了 `CompetitionType` 与 `CompetitionStatus` 两个未使用的领域类型导入；
  - 清理了已冗余的 `typeLabel`、`typeTagType`、`statusLabel`、`statusTagType` 计算属性；
- **样式层**：清理了已废弃的 `.badges-wrap` 与 `.type-tag` 相关 SCSS 规则。

### 2.2 右侧操作区域全面对齐 `VolurHeader` 规范
- **元素间距与排版**：
  - `.header-right` 间距统一由 `gap: 12px` 升级为平台标准 `gap: 16px`；
  - 保留了竞赛专属的 `.team-pill`（队伍信息胶囊），并使其与后续工具按钮保持统一的横向流动流。
- **中英文语言切换按钮**：
  - 按钮样式统一升级为 `quaternary`、`circle`、`class="header-icon-btn"`；
  - 文本结构参考 `VolurHeader`，使用 `<span class="lang-text">{{ locale === 'zh_CN' ? 'EN' : '中' }}</span>` 醒目直观呈现；
  - 修复持久化键名为 `localStorage.setItem('locale', newLocale)`，保证全平台语言设置无缝同步；
  - 清理未使用的 `LanguageOutline` 图标导入。
- **亮暗主题切换按钮**：
  - 按钮样式统一采用 `quaternary`、`circle`、`class="header-icon-btn"`；
  - 图标采用 `<n-icon size="18">` 包裹 `SunnyOutline` 与 `MoonOutline`，悬浮提示文案对齐平台标准语言包键名。
- **用户资料与下拉菜单**：
  - 下拉触发器类名与结构更新为 `<div class="user-profile cursor-pointer">`；
  - `AppAvatar` 统一传递 `size="small"`、`:src`、`:name`、`:username`；
  - 用户名文本升级为 `<span class="user-nickname">`，样式对齐 `VolurHeader` 的 13px 500 字重，且适配亮/暗主题颜色。
- **响应式断点规则**：
  - 新增 `@media (max-width: 768px)` 与 `@media (max-width: 520px)` 样式规则，在移动端自动缩小间距（10px）并支持昵称截断/隐藏，保障在小屏设备上竞技场倒计时与关键操作按钮不发生溢出换行。

### 2.3 顶部容器视觉与主题对齐
- **高度标准化**：将 `.arena-header` 高度从 `60px` 调整为 `64px`，padding 调整为 `0 24px`（移动端 `0 12px`）；
- **背景与边框色彩对齐**：
  - 亮色模式：背景颜色 `var(--n-color, #ffffff)`，下边框 `1px solid var(--v-border-base, #efeff5)`；
  - 暗色模式（通过 `@include theme-dark` 与 `.dark &`）：背景颜色 `var(--n-color, #121826)`，下边框 `1px solid var(--v-border-base, #2e303a)`；
  - 添加 `transition: background-color 0.25s ease, border-color 0.25s ease`，确保切换主题时平滑丝滑过渡。

---

## 3. 影响范围与审计结果
1. **修改文件**：
   - [`src/volur-lite-view/src/public/volurEx/layouts/CompetitionLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/layouts/CompetitionLayout.vue)
2. **构建与类型验证**：
   - `npx vue-tsc --noEmit`：0 错误通过；
   - `npm run build`：成功在 5.17 秒内构建生成生产包，无任何语法错误或类型断言失败；
3. **安全性与向后兼容性**：
   - 移除的标签与计算属性仅影响竞技场顶部标题右侧的纯视图装饰，未对任何比赛逻辑、队伍接口、倒计时轮询以及解题核心功能产生任何副作用；
   - 修复了 `locale` 本地存储键名，使得选手在比赛界面切换语言时能够全局生效。
