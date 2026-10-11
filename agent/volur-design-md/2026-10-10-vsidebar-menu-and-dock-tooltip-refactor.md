# VSideBar 菜单项实体背景、Bottom Dock 悬停与 Tooltip 样式重构

## 1. 变更背景与需求说明

本次重构主要解决侧边栏如下视觉与交互体验问题：
1. **Logo 视觉素材替换**：将展开态 Logo 替换为 `VolurCTFStudio.svg`，并适配暗夜模式下的反色渲染；
2. **菜单项背景承载节点优化**：评估并优化菜单项背景渲染节点，消除 Naive UI 原生 `::before` 伪元素带来的层叠上下文污染与调试困难；
3. **Bottom Dock 悬停体验优化**：去除底部操作坞各项在 hover 时的边框颜色突变，悬停背景色统一为 `#f4f5f5`（暗夜模式适配为深灰半透明）；
4. **Tooltip 样式优化**：去除底部坞各项提示气泡的底部尖角指示箭头（arrow），在日间模式下背景统一为 `#fff`，并赋予柔和阴影（Shadow）。

---

## 2. 核心架构与代码变更

### 2.1 Logo 替换与暗夜自适应
- 在 `VSideBar.vue` 中，计算属性 `expandedLogoSrc` 统一映射至 `/r/static/svg/VolurCTFStudio.svg`；
- 根据宽长比（632:68 ≈ 9.29:1）优化展开态 Logo 的尺寸规则为 `height: 20px; max-width: 160px; object-fit: contain;`；
- 在暗夜模式下，通过 `.logo-container .brand-logo-expanded { filter: brightness(0) invert(1); }` 将纯黑 SVG 路径反转为纯白，确保高对比可读。

### 2.2 菜单项背景渲染架构升级与色彩规范
- **根因分析**：Naive UI 默认将背景渲染在 `.n-menu-item-content::before` 上，伪元素在自定义主题重写时需大量 `!important` 且容易在折叠/缩放时与子元素产生 z-index 冲突，且在 DevTools 调试时不直观；
- **优化方案与色彩规范**：
  - 将背景色、圆角（`border-radius: 8px`）、鼠标指针及过渡动效直接绑定在 `.n-menu-item` 物理外层容器上；
  - 彻底禁用内容层的背景垫底：`.n-menu-item-content::before { display: none !important; }`；
  - **未激活态**：容器无背景色（`background-color: transparent`），图标与文本颜色统一为 `#565b61`；
  - **悬停态（Hover）**：容器背景色为 `#efefef`，图标与文本颜色高亮为 `#1b1b1e`；
  - **激活态（Active / Selected）**：基于现代 CSS `:has(.n-menu-item-content--selected)` 渲染容器背景色为 `#ececef`，图标与文本颜色为 `#1b1b1e`（字重设为 600）；处于激活态悬停时背景维持 `#ececef`；
  - 该色彩行为在展开态与折叠态（`&.n-menu--collapsed`）全面保持对齐。

### 2.3 Bottom Dock 悬停样式收敛
- 移除 `.action-icon-btn`、`.user-card-expanded`、`.user-btn-collapsed`、`.bottom-action-btn-expanded` 与 `.bottom-action-btn-collapsed` 在 hover 时的 `border-color` 变化；
- 日间模式下悬停背景统一设置为 `#f4f5f5`，暗夜模式下统一设置为 `rgba(255, 255, 255, 0.08)`，边框保持不变，消除跳动感。

### 2.4 Tooltip 无角化与白底阴影设计
- 侧边栏全部 `<n-tooltip>` 组件显式声明 `:show-arrow="false"`，直接从 AST 层面移除气泡三角指示尖角；
- 在 `App.vue` 的全局 `themeOverrides` 中注入 `Tooltip` 主题变量：
  - 日间模式：`color: '#ffffff'`，`textColor: '#2b2d31'`，`boxShadow: '0 1px 3px 0 rgba(10, 13, 18, 0.1), 0 1px 2px -1px rgba(10, 13, 18, 0.1)'`，`padding: '8px 12px'`，`borderRadius: '8px'`；
  - 暗夜模式：`color: '#292a2d'`，`textColor: '#bdc1c6'`，`boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.4), 0 1px 2px -1px rgba(0, 0, 0, 0.4)'`，`padding: '8px 12px'`，`borderRadius: '8px'`；
- 在 `src/styles/index.scss` 中补充全局 `.n-popover.n-tooltip` 强制层叠覆盖样式（规避 Naive UI Tooltip 缺少 border、font-size 等部分 CSS Variables 的限制）：
  - `border: 1px solid #d7d8da80`（暗夜模式 `1px solid #262626`）；
  - `padding: 8px 12px !important;`；
  - `font-size: 12px !important;`；
  - `color: #2b2d31 !important;`（暗夜模式 `#bdc1c6 !important;`）；
  - `box-shadow: 0 1px 3px 0 rgba(10, 13, 18, 0.1), 0 1px 2px -1px rgba(10, 13, 18, 0.1) !important;`。

### 2.5 Header 侧边栏折叠/展开按钮 Tooltip 统一
- 将 `sidebar-header` 中展开与折叠态下的切换按钮原生 `:title` 属性移除；
- 使用 `<n-tooltip :show-arrow="false">` 分别包裹两态切换按钮：
  - 展开态：`placement="bottom"`，提示文本为 `t('common.collapse_sidebar')`；
  - 折叠态：`placement="right"`，提示文本为 `t('common.expand_sidebar')`；
- 与全局无角化纯白/深灰 Tooltip 样式无缝对齐，彻底替代浏览器原生延时且粗糙的默认黑底提示条。

### 2.6 折叠态菜单项 Header 隐藏与 Icon 居中修复
- **根因分析**：Naive UI 原生设计中对折叠态 `.n-menu-item-content-header` 仅赋予 `opacity: 0`，未设置 `display: none`。在自定义为 Flex 容器后，该 Header 节点仍作为 Flex 弹性子项占据水平空间；同时展开态为文本留白的 `margin-right: 12px` 未在折叠态被完全抹平，导致收缩态下标题溢出渲染，且 `.n-menu-item-content__icon` 受到挤压发生偏左偏移，无法水平居中；
- **优化方案**：
  - 在 `.v-sidebar.is-collapsed`、`.n-menu--collapsed` 及 `.n-menu-item-content--collapsed` 多重折叠状态选择器下，强制将 `.n-menu-item-content-header` 设置为 `display: none !important; width: 0 !important; height: 0 !important; overflow: hidden !important;`，彻底从渲染树中剔除占位；
  - 将折叠态下的 `.n-menu-item-content__icon` 显式声明 `margin: 0 !important; margin-right: 0 !important; display: flex !important; align-items: center !important; justify-content: center !important;`，并在 38×38 容器内实现严格的绝对水平/垂直居中；
  - 隐藏可能存在的子菜单指示箭头 `.n-menu-item-content__arrow { display: none !important; }`，暗夜模式同步对齐。

### 2.7 侧边栏列表项与 Bottom Dock 黑夜模式色彩精细化重构
- **侧边栏列表项（黑夜模式）**：
  - **激活态（Active / Selected）**：背景颜色更新为 `#2a2a2a`，文本与图标颜色统一为 `#d4d4d4`（字重设为 600）；激活态悬停时背景保持 `#2a2a2a`；
  - **未激活态（Inactive）**：文本与图标颜色更新为 `#8c8c8c`，背景为透明（`transparent`）；
  - **悬停态（Hover）**：背景颜色更新为 `#2a2a2a`，文本与图标颜色高亮为 `#d4d4d4`；
  - 折叠态（`&.n-menu--collapsed`）下的图标着色与背景同步保持严格对齐；
- **Bottom Dock 底部操作坞（黑夜模式）**：
  - **边框与文本统一**：所有项（操作图标按钮、用户卡片、底部动作按钮）的边框颜色统一调整为 `#262626`，未悬停时的默认文本/图标颜色统一调整为 `#8c8c8c`；
  - **悬停高亮**：悬停时文本/图标颜色提亮为 `#d4d4d4`，背景色过渡至 `#323232`，边框颜色保持 `#262626` 消除视觉跳动；
  - **用户项背景体系**：用户卡片（`.user-card-expanded` 及折叠态 `.user-btn-collapsed`）默认背景色明确设置为 `#1f1f1f`，悬停态背景色调整为 `#323232`，在线状态小绿点外边框在默认态为 `#1f1f1f`、悬停态平滑过渡为 `#323232`。

---

## 3. 验证情况

- 执行 `vue-tsc --noEmit && vite build`，编译顺利通过（Exit Code 0）。
- 涉及改动文件：
  - `src/components/sidebar/VSideBar.vue`
  - `src/App.vue`
  - `src/styles/index.scss`



