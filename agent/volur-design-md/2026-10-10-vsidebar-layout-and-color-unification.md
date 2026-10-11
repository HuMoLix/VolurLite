# VSideBar 布局几何与暗夜模式色彩统一重构

## 1. 变更背景与问题溯源

在 `VSideBar.vue`（基于 Naive UI `<n-layout-sider>` 的桌面侧边栏组件）原有实现中，存在如下布局几何与视觉样式缺陷：
1. **硬编码展开宽度公式**：`.v-sidebar:not(.is-collapsed):not(.n-layout-sider--collapsed)` 使用了 `calc(220px + 36px) !important`（即 256px），其根源是旧版菜单项宽度 220px 叠加旧版包装容器左右 18px 内边距（`18px * 2 = 36px`），破坏了 `:width` prop 的解耦与新边距适配。
2. **边缘破窗负 Margin 导致右侧边缘与边框错位**：旧版 `.sidebar-header` 使用了 `width: calc(100% + 36px); margin: 0 -18px;` 试图抵消外层 `sidebar-content-wrapper` 的 `padding: 0 18px`。而下方的 `sidebar-nav-container` 至 `sidebar-bottom-area` 均为普通的 `width: 100%` 正常盒模型。这导致 header 的物理右边缘超出了下方区域 18px，不仅视觉不对齐，更会突兀溢出 aside 边框。
3. **背景色与边框权责混乱**：`.sidebar-header` 与 `<aside>` 同时声明背景色；暗夜模式下 aside 采用 `#1e1f20`，未能对齐 Google Workspace `#191919`；aside 右侧边框与 header 自身下边框及子元素边框存在遮挡与重叠。

---

## 2. 核心改动点

### 2.1 展开宽度规范收敛
- 将 `.v-sidebar:not(.is-collapsed):not(.n-layout-sider--collapsed)` 的宽度重构为明确的 `256px !important`，废除过时的 `calc(220px + 36px)` 魔法数字计算。

### 2.2 统一包装容器内边距
- 将 `.sidebar-content-wrapper` 的水平内边距调整为 `padding: 0 8px`，展开态与折叠态同步收敛，释放侧边栏内容可视宽度（`256px - 16px = 240px`）。

### 2.3 Aside 统一管理背景色
- 移除 `.sidebar-header` 常态下的 `background-color`，使头部直接透出 aside 的统一底色。
- aside（`.v-sidebar`）统管全栏背景色：
  - 日间模式：`#fafafa !important`；
  - 暗夜模式：`#191919 !important`；
- 同步更新在线状态指示点（`.online-status-dot`）在暗夜模式下的外圈描边为 `#191919`，吸顶模糊态底色更新为 `rgba(25, 25, 25, 0.95)`。

### 2.4 Brand 规范重命名为 Logo
- 模板与样式将 `.brand-container` 全链路更名为 `.logo-container`；
- 为 `.logo-container` 赋予专属的 `padding: 0 18px;`，保证 Logo 图标内嵌安全呼吸区域。

### 2.5 边框统一由 Aside 单一源管理与消除双重边框
- 废除 `.sidebar-header` 的 `margin: 0 -18px; width: calc(100% + 36px);` 负外边距，恢复为标准的 `width: 100%; margin: 0; padding: 0;`。
- `sidebar-header`、`sidebar-nav-container` 与 `sidebar-bottom-area` 全部处于统一的 `width: 100%` 盒模型下，宽度完全一致。
- **消除双重边框（变粗）根因**：
  - Naive UI `<n-layout-sider :bordered="true">` 原生会在内部渲染 `<div class="n-layout-sider__border">`（`position: absolute; right: 0; width: 1px;`）。
  - 若在 `<aside>`（`.v-sidebar`）自身叠加 `border-right: 1px solid`，会使得 `<aside>` 的物理右边框（1px）与内部绝对定位贴在 padding-box 边缘的 `.n-layout-sider__border`（1px）并排紧挨，导致边框物理宽度直接翻倍变成 2px。
  - **解决方案**：将 `.v-sidebar` 自身的 `border-right` 显式设为 `none !important`，完全交由 Naive UI 原生 `.n-layout-sider__border` 统一管理 1px 细线：
    - 日间模式：`background-color: #d7d8da80 !important`；
    - 暗夜模式：`background-color: #262626 !important`。
    保证侧边栏右边框无论在何种模式下均呈现极致纯粹的 1px。

---

## 3. 影响范围与验证

- **影响文件**：
  - `src/components/sidebar/VSideBar.vue`
- **构建与类型验证**：
  - 执行 `vue-tsc --noEmit && vite build`，编译通过且零类型/语法告警。
