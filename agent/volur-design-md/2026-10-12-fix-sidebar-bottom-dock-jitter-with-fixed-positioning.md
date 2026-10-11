# 修复侧边栏 Bottom Dock 页面触底反弹抖动问题设计记录

## 1. 变更背景与现象分析

### 1.1 异常现象
在长业务页面（如 `CompetitionDetailView.vue` 赛事详情页）中，当用户垂直下滑页面至最底部后，执行往上回划页面的操作时，左侧侧边栏 `SideBar.vue` 底部的 Bottom Dock（包含 Action Box 操作框与 User Profile 用户卡片）出现高频、剧烈的像素级垂直抖动（Jitter / Shaking）。

### 1.2 根因定位与机理剖析
1. **`position: sticky` 包含块底边碰撞截断（Sticky Boundary Collision）**：
   - 全局主布局 `AppLayout.vue` 采用 `min-h-screen flex` 弹性双栏架构；
   - 桌面端侧边栏容器 `.sidebar-desktop-view` 此前配置为 `position: sticky; top: 0; height: 100vh; align-self: flex-start;`，外层包裹容器为 `display: contents;`；
   - 侧边栏的 Sticky 包含块（Containing Block）为父容器 `.app-layout`，其高度由右侧 `CompetitionDetailView` 主体内容高度驱动（长文本与各类组件撑开至数千像素）；
   - 根据 CSS 规范，Sticky 元素绝对不能超出其包含块底边界。当页面滚动至最底端时，`scrollTop == scrollHeight - clientHeight`，侧边栏底边与 `.app-layout` 底边重合；
   - 状态由“视口吸顶（Stuck to Viewport Top）”切换至“包含块底边锁定（Clamped to Parent Bottom）”。
2. **反向滚动与亚像素横跳（Sub-pixel Oscillation）**：
   - 当用户从底部反向向上划动时，瞬时滚动坐标处于阈值边界；
   - 结合鼠标滚轮步长、触控板动量惯性以及 Windows 系统 DPI 缩放（如 125%、150%）产生的非整数像素差（0.2px~0.8px）；
   - 渲染合成引擎在连续渲染帧中频繁在“包含块底边截断”和“视口顶部释放”两种定位模式间交替震荡；
   - 由于 Bottom Dock 依靠 `margin-top: auto` 定位在 `100vh` 容器的最下沿，底边坐标的反复跳跃被直接投射在 Action Box 与用户信息卡片边框上，肉眼可见剧烈抖动。

---

## 2. 核心架构重构与改动细节

### 2.1 桌面端侧边栏 Viewport 物理固定定位 (`src/components/sidebar/SideBar.vue`)
1. **Flex 占位容器与固定定位解耦**：
   - 桌面端（断点 `>= 840px`）将外层 `.volur-responsive-sidebar` 从 `display: contents` 改为标准的 Flex 弹性流占位项目：
     - `display: block; flex-shrink: 0;`；
     - 宽度由响应式计算属性 `sidebarWidth` 驱动（展开态 `256px`，折叠态 `74px`），并通过 CSS 变量 `--v-sidebar-width` 动态注入；
     - 配置 `transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1);`，与内部 `VSideBar` 折叠动画完全同步；
   - 确保右侧主视口 `.app-main`（`flex: 1; min-w-0;`）的布局宽度分配不受任何影响，且折叠动画平滑自然。
2. **内部视口绝对固定（`position: fixed`）**：
   - 将内部渲染的 `.sidebar-desktop-view` 定位属性重构为：
     - `position: fixed; top: 0; bottom: 0; left: 0;`；
     - `height: 100vh; height: 100dvh; width: var(--v-sidebar-width, 256px);`；
     - `z-index: 30; transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1);`；
   - 包含块直接提升为浏览器视口（Viewport），彻底与父级 `.app-layout` 及其内部滚动画布高度解绑，消除包含块底边碰撞条件，杜绝抖动根源。
3. **移动端平滑兼容**：
   - 在移动端断点（`< 840px`）下，`.volur-responsive-sidebar` 保持 `display: contents`，桌面端容器保持 `display: none`，底部导航栏（BottomBar）正常生效，零侵入性。

### 2.2 全局视口防触底冲量回弹 (`src/styles/index.scss`)
- 在全局 `html, body, #app` 规则中补充 `overscroll-behavior-y: none;`；
- 消除浏览器触底/触顶时默认的橡皮筋回弹冲量（Bounce / Rubber-banding），保证长页面滑动触底过程平稳利落。

---

## 3. 代码纯洁性与工程审计

1. **零注释铁律**：所有修改的源码文件（`SideBar.vue`、`index.scss`）严格遵循零注释原则，无冗余行内说明；
2. **类型与逻辑安全**：`sidebarWidth` 具备完善的类型推导与数值单位容错（数字转 `px`，字符串直接保留）；
3. **构建验证**：全量执行生产级构建命令通过，无任何 TypeScript 报错与模板异常。
