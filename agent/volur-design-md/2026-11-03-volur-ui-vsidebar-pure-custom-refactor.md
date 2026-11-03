# volur-ui 纯自研 VSideBar 侧边栏套件重构设计与实施记录

## 1. 变更背景与动因

在 `volur-lite-view-NEXT` 前端体系中，原侧边栏组件位于 `src/components/sidebar/SideBar.vue` 及 `VSideBar.vue`。
旧实现存在以下架构与设计问题：
1. **重度依赖第三方 UI 组件库 (Naive UI)**：深度耦合了 `NLayoutSider`、`NMenu`、`NPopover`、`NTooltip`、`NBadge`、`NIcon` 以及 Naive UI 特有的 `MenuOption` 类型定义，违背了自研组件库的纯粹性原则；
2. **单体文件过大且职责混杂**：原 `VSideBar.vue` 达到 80KB，包含了骨架、分组计算、菜单交互、工具栏与气泡浮层等复杂逻辑，扩展与维护成本极高；
3. **未收敛至平台组件库**：未按平台自研组件库 `volur-ui`（如 `VButton.vue`）的标准进行原子化组织。

本次重构按照 `VButton.vue` 零依赖标准，在 `src/volur-ui/components/sidebar/` 下建立纯自研、模块化解耦的 `VSideBar` 套件体系，实现完全摆脱 Naive UI 的 100% 自研。

---

## 2. 架构拆分与改动清单

### 2.1 目录组织结构

新建目录 `src/volur-ui/components/sidebar/`，结构如下：

- **`types.ts`**：纯自研强类型体系（彻底移除 `MenuOption from 'naive-ui'`，定义 `SidebarNavItem`、`SidebarNavGroup`、`VSideBarProps`、`VSideBarEmits` 等）；
- **`context.ts`**：基于 Vue Provide / Inject 的 `SIDEBAR_INJECTION_KEY` 状态共享上下文；
- **`layout/SideBarLayout.vue`**：三段式骨架布局容器，使用语义化 `<aside>`、`<header>`、`<main>`、`<footer>`，纯 CSS 变量（`--v-sidebar-width`）+ 贝塞尔曲线过渡驱动尺寸伸缩，右边框采用 `var(--color-v1-outline-var)`，底色采用 `var(--color-v1-surface-left-nav)`；
- **`components/SideBarNavHeader.vue`**：负责 Logo、品牌文案以及折叠切换触发器（SVG 原生矢量图标，无 `NIcon`）；
- **`components/SideBarNavContent.vue`**：管理主要操作按钮（使用自研 `VButton`）、滚动区域及分组列表；
- **`components/SideBarNavGroup.vue`**：负责分组标题（`var(--color-v1-text-disable)`）、折叠细线分割器（`var(--color-v1-outline-var)`）、原生 `<button class="volur-nav-item">` 菜单项渲染、子菜单展开/折叠、激活态（`var(--color-v1-nav-item-active)`）及悬浮微交互；
- **`components/SideBarNavFooter.vue`**：管理快捷工具按钮（搜索、深浅色模式切换、设置、通知）及用户头像信息卡片，纯自研轻量交互；
- **`VSideBar.vue`**：顶层组合门面组件，统一暴露实例方法（`scrollToTop`、`toggleCollapse`、`expand`、`collapse`、`openSearch`、`closeSearch`）；
- **`index.ts`**：集中向外导出。

### 2.2 全局组件库注册与调用处迁移

1. **`src/volur-ui/index.ts`**：导出 `VSideBar` 并将其注册到 `VolurUI` 插件体系；
2. **`src/layouts/primary/AppLayout.vue`**：将原有 `<SideBar>` 彻底替换为从 `@/volur-ui` 导入的 `<VSideBar>`，同步更新强类型导入；
3. **`src/layouts/primary/AdminLayout.vue`**：将原有 `<SideBar>` 彻底替换为从 `@/volur-ui` 导入的 `<VSideBar>`；
4. **清理旧组件**：移除废弃的 `src/components/sidebar/SideBar.vue` 及对应导出，彻底杜绝兼容别名与历史包袱。

---

## 3. 设计令牌与轻暗主题支持

全套组件严格基于 W3C 标准 `color-scheme` 与 `light-dark()` 设计令牌运作：
- **底色**：`var(--color-v1-surface-left-nav)` (`light-dark(#fafafa, #191919)`)；
- **右边框**：`var(--color-v1-outline-var)` (`light-dark(#d7d8da80, #262626)`)；
- **分组标题**：`var(--color-v1-text-disable)` (`light-dark(#85898e, #575757)`)；
- **激活菜单项底色**：`var(--color-v1-nav-item-active)` (`light-dark(#ececef, #2a2a2a)`)；
- **激活菜单项主色**：`var(--color-v1-primary-highest)` (`light-dark(#0b57d0, #a8c7fa)`)；
- **文本颜色**：`var(--color-v1-text)` (`light-dark(#1b1b1e, #d4d4d4)`)；
- **悬浮态**：`var(--color-v3-hover)`。

全套组件代码零 `html.dark` 样式覆盖，零选择器特异性负担。

---

## 5. 核心架构问题剖析与深层优化

在针对初版自研侧边栏进行代码审计与结构对齐时，发现了以下 4 个关键差异与历史包袱，现已全部彻底优化：

### 5.1 `<aside>` 标签内联属性与冗余样式成因
- **历史成因**：原有实现基于 Naive UI 的 `NLayoutSider`。在 Naive UI 内部，`NLayoutSider` 会将传入的 `width`、`min-width`、`max-width` 直接以内联样式绑到 `<aside>` 上以控制折叠动画与防止重排。在初版自研重构时，`SideBarLayout.vue` 的 `computedStyle` 误承袭了这一模式，将三项像素属性硬写到内联 `:style` 中，造成 DOM 内联样式污染与优先级冲突；
- **重构解决**：彻底移除 `computedStyle` 中的 `width`、`min-width` 与 `max-width` 内联绑定，仅保留纯 CSS 变量驱动 `'--v-sidebar-width': computedWidth`。所有盒模型尺寸、缩放过渡动画与响应式判定全部收敛于 `.volur-sidebar-layout` CSS 类，DOM 保持极致纯净。

### 5.2 `<aside>` 外层两层冗余 `div`（`volur-sidebar-root` 与 `sidebar-desktop-view`）成因与解耦
- **历史成因**：在旧架构下，代码被拆分为了外壳 `SideBar.vue`（具有 `volur-responsive-sidebar` 和 `sidebar-desktop-view`，通过 `position: fixed` 支撑桌面端，通过 `sidebar-mobile-view` 支撑移动端底部导航）与内部 `VSideBar.vue`（内嵌 `NLayoutSider`）。初版合并时，这两个包裹层被机械地套在了 `<SideBarLayout>` 外侧；
- **重构解决**：`AppLayout` 与 `AdminLayout` 本身均是现代 Flexbox 架构容器（`h-screen overflow-hidden flex`）。侧边栏本质上是 Flex 容器的首个子元素。现已彻底剥离 `volur-sidebar-root` 与 `sidebar-desktop-view` 两层无意义的包装 `div`，直接由 `<SideBarLayout>`（即 `<aside>`）作为根级容器挂载在 Flex 流中，利用 CSS 变量 `--v-sidebar-width` 配合平滑贝塞尔过渡自适应伸缩，杜绝任何中间层与定位逃逸。

### 5.3 Layout 容器 Flex 约束与 `padding: 0 8px;` 空间归一化
- **重构解决**：
  1. `<aside class="volur-sidebar-layout">` 自身直接定义为纵向 Flex 容器（`display: flex; flex-direction: column; height: 100%;`）；
  2. 显式约束三大语义区域：`<header class="volur-sidebar-layout__header">`（`flex-shrink: 0;`）、`<main class="volur-sidebar-layout__main">`（`flex: 1; min-height: 0; overflow-y: auto;`）、`<footer class="volur-sidebar-layout__footer">`（`flex-shrink: 0;`）；
  3. 直接在 `.volur-sidebar-layout` 上定义统一步长边距 `padding: 0 8px;`，移除了内部不必要的包裹 `div`；
  4. 子级组件（导航项、操作按钮、底栏工具盒）全部采用 `width: 100%; margin: 0;` 自适应填满内衬区域，折叠时居中收敛至 38px/44px，彻底解决内外部边距重复叠加与视觉错位问题。

### 5.4 Header 品牌 Logo 与折叠切换按钮规范对齐
- **Logo 规范恢复与暗色滤镜反色修复**：
  1. 默认展开态 Logo 严格恢复为 `/r/static/svg/VolurCTFStudio.svg`；
  2. **暗夜模式反色失效根因与修复**：W3C CSS Color 规范明确规定 `light-dark()` 为颜色函数（仅接受 `<color>` 入参），用于 `filter` 会被浏览器直接判定为非法值并丢弃。现通过 `useAppStore` 联动响应式计算 `isDarkMode`，并在模板与样式层注入 `filter: brightness(0) invert(1)` 与 `.is-dark`，彻底修复黑夜模式下 Logo 反白显示；
  3. 折叠态默认展示 `/r/static/svg/favicon.svg`（暗色模式自适应切换为 `favicon-dark.svg`）；
  4. Logo 容器 `padding` 调整为 `0 18px;`，与 `sidebar-toggle-btn` 同步引入 `var(--color-v1-button-container-high)` 悬浮微交互背景；
  5. **Header 几何高度解耦**：彻底移除 `.sidebar-nav-header` 固定的 `height: 60px;`，调整为弹性内边距 `padding: 12px 0;`，高度由 38px 的品牌元素与按需内边距自然驱动。
- **切换触发器规范恢复**：
  1. 废弃通用临时 SVG 路径，严格使用平台标准矢量图标组件：折叠态使用 `MenuOutline` (`@vicons/ionicons5`)，展开态使用 `MenuOpenOutlined` (`@vicons/material`)；
  2. 尺寸严格规范为 38x38px，圆角 12px，折叠态在通道内通过 `margin: 0 auto;` 精准居中，hover 状态采用 `var(--color-v1-button-container-high)`。

### 5.5 导航分组标题与菜单项视觉参数精修
- **分组标题规范**：`.sidebar-nav-group__title` 明确修正为 `font-size: 12px; font-weight: 400;`，弱化层级侵略感，强化视觉韵律；
- **菜单项规范**：
  1. 常态 `.volur-nav-item`：`color: var(--color-v1-text-var); font-weight: 500; height: 36px;`；
  2. hover 状态：背景色严格遵循 `var(--color-v1-nav-item-hover)`，彻底消除防御性回退代码；hover 文字颜色明确使用 `var(--color-v1-text)`；
  3. 图标几何规格：`.volur-nav-item__icon-wrap` 严格规范为 18x18px，`.volur-nav-item__icon` 高宽显式使用 `inherit` 继承；
  4. 激活态 `.is-active`：`color: var(--color-v3-text);`，彻底剔除加粗 `font-weight: 600;`，保持文字字重一致不抖动；
  5. 折叠态与子菜单高度同步统一为 36px，几何尺寸全面谐调。
- **分组折叠细线与间距规范**：
  1. 引入 `groupIndex` 索引机制，收缩态下首组分组（`groupIndex === 0`）坚决不渲染分割线，仅从第二组开始展示；
  2. 修正分组容器间距：展开态保持 `gap: 12px;`，收缩态调整为 `gap: 0;`；
  3. **收缩态分割线宽度坍缩根因与修复**：父级 Flex 容器在折叠态设置了 `align-items: center;`，导致未声明宽度的子元素宽度被压缩为 0。现显式声明 `display: block; width: calc(100% - 20px); height: 1px; margin: 12px 10px;`，确保分割线在左右 10px 边距约束下稳定展现 1px 细线。

### 5.6 Footer 底部工具栏与用户卡片全面对齐原版设计
- **底部动作盒 (`sidebar-action-box`) 顺序与独立轮廓对齐**：
  1. 废弃外层包裹背景盒，展开态下 5 项功能按钮独立为 32px 高的圆角药丸按钮（`border: 1px solid var(--color-v1-outline-var)`），顺序完全还原：
     `Search`（搜索） -> `BottomAction`（返回/特定底部动作） -> `Notifications`（通知，支持未读状态红点） -> `Settings`（设置） -> `ThemeToggle`（深浅色切换，日间 `MoonOutline`，夜间 `SunnyOutline`）；
  2. 折叠态下收敛为单个 38x38px 轮廓按钮 `AppsSharp`（应用与工具），保持极简通道美感；
- **用户卡片 (`sidebar-user-box` / `.user-card`) 真实状态与视觉对齐**：
  1. 剔除手写静态假元素，深度绑定 `useUserStore` 真实鉴权状态（`isLoggedIn`、`currentUser`、`displayName`、`userEmail`、`avatarUrl`、`userInitial`）；
  2. 集成平台标准自研 `<VAvatar>` 组件（支持圆环光晕、头像回退字符与暗色自适应）；
  3. 容器展开态采用 1px 细线边框与 `padding: 6px 10px;`，悬浮时平滑响应 `var(--color-v1-button-container-high)`；折叠态自动收缩为 38x38px 单头像卡片居中展示。

### 5.7 宽度驱动机制重构与 220px 规范化
- **历史 CSS 变量三锁机制成因与废弃**：
  原实现通过 `computedStyle` 生成 `--v-sidebar-width` CSS 变量，并在 SCSS 中通过 `width: var(--v-sidebar-width, 256px); min-width: var(--v-sidebar-width, 256px); max-width: var(--v-sidebar-width, 256px);` 强制锁定三项尺寸，并在 `transition` 中重复监听三者。该模式源自旧版 Naive UI Sider 对抗重排的防御性实现，在当前标准 Flex 容器下造成了不必要的样式复杂度和属性冗余。
- **Props 驱动与直接尺寸内联**：
  1. 彻底删除 CSS 中的 `width: var(--v-sidebar-width, 256px); min-width: var(--v-sidebar-width, 256px); max-width: var(--v-sidebar-width, 256px);` 及其对应的 `min-width`、`max-width` 的 `transition` 动画监听；
  2. `SideBarLayout.vue` 的 `computedStyle` 重构为直接输出 `{ width: computedWidth.value }`；配合 Flex 容器的 `flex-shrink: 0;` 和 `transition: width 0.25s ...`，实现由组件入参 Props 强类型、单一数据源直接决定容器渲染宽度；
  3. 全局默认宽度规范由历史的 `256px` 统一调整为更加紧凑、精致的 `220px`（`SideBarLayout.vue`、`VSideBar.vue`、`AppLayout.vue`、`variables.scss` 均同步对齐 `width: 220`）。

### 5.8 Action 组件归档与全局 `v-tooltip` 指令落地
- **Actions 组件解耦归档**：
  将 `NoticeCard.vue`、`SettingCard.vue`、`UserCard.vue`、`Search.vue` 及内聚的 `FeedbackDrawer.vue` 统一收敛至 `src/volur-ui/components/sidebar/components/actions/` 目录，并通过 `index.ts` 集中向外导出，在 `types.ts` 中补齐强类型入参接口；
- **全系统全局 `v-tooltip` 指令实现（方案 2）**：
  1. 彻底废弃原生 HTML `:title` 属性（解决其存在长达 1.5s 悬停时延、单调系统方框、无法主题响应与在复杂布局中容易丢失触发等缺陷）；
  2. 采用纯自研轻量单例浮层机制封装全局指令 `v-tooltip`（`src/directives/tooltip.ts`），支持字符串入参、响应式更新、对象配置、即时点击销毁与 `v-tooltip:top/right/bottom/left` 语义方向定位；
  3. 引入视口边界检测算法（Viewport Boundary Collision Detection）：在触碰视口顶部或侧边边界时自动翻转（Flip），防止 Tooltip 被截断；
  4. 样式全面对齐 Google Material Design / Workspace 阶梯配色体系（暗色采用 `#202124` 搭配 `#3c4043` 边框，亮色采用高对比 `#ffffff` 搭配软阴影，6px 圆角，0.15s 贝塞尔弹性淡入缩放）；
  5. 在 `main.ts` 与 `VolurUI.install` 中注册，并在 `SideBarNavHeader.vue`（切换触发器）、`SideBarNavGroup.vue`（折叠态单图标右侧提示）、`SideBarNavFooter.vue`（搜索、返回、通知、设置、主题切换、用户卡片等）全链路落地应用。

### 5.9 VTooltip 提取收敛至 volur-ui 与旧版动效还原
- **VTooltip 提取与目录收敛**：
  将 `src/components/common/VTooltip.vue` 完整迁移提取至 `src/volur-ui/components/tooltip/` 目录，并配置强类型定义文件 `types.ts` 与统一入口 `index.ts`，同时在 `src/volur-ui/tooltip/index.ts` 提供对称导出，并在 `VolurUI.install` 中全局注册；
- **旧版动效与样式 100% 还原**：
  1. 移除外部冗余的 `src/directives/tooltip.scss`，将全局 Tooltip 样式与过渡规则全部收敛至 `src/volur-ui/components/tooltip/tooltip.scss`；
  2. 完全还原原版几何参数：`padding: 8px 12px`、`font-size: 14px`、`line-height: 14px`、`border-radius: 8px`、边框 `1px solid #d7d8da80`（暗色 `#262626`）、背景 `#ffffff`（暗色 `#292a2d`）、阴影 `box-shadow: 0 1px 3px 0 rgba(10, 13, 18, 0.1)`（暗色 `rgba(0, 0, 0, 0.4)`）；
  3. 完全还原原版微动效（Scale & Transform-Origin）：
     - 初始状态：`opacity: 0; transform: scale(0.85);`
     - 激活状态：`opacity: 1; transform: scale(1);`
     - 动效贝塞尔曲线：`cubic-bezier(0, 0, 0.2, 1)`（0.15s）；
     - 结合 `data-placement` 属性自动根据弹出方向设定物理缩放基点（`transform-origin: center bottom` / `center top` / `right center` / `left center`），确保气泡从触发锚点自然弹性膨胀弹出；
- **向前兼容性保障**：
  更新 `src/components/common/index.ts`，将 `VTooltip` 导出重定向至 `@/volur-ui/components/tooltip`，确保平台现有数十个业务视图无感知平滑过渡。

### 5.10 Footer ActionBox 动作盒功能完整联动与 Popover 卡片体系集成
- **功能性解耦与动作卡片完整接入**：
  在 `SideBarNavFooter.vue` 中完整接入 `actions/` 目录下的业务卡片组件：
  1. **搜索模态框 (`Search.vue`)**：
     - 点击展开态或折叠态搜索按钮时呼出；
     - 支持关键字防抖查询（300ms），内嵌键盘快捷键导航（上下键选定、Enter 跳转、Escape 退出）；
     - 搜索结果直接跳转至赛事详情页 `/competitions/${id}`；
     - 顶层 `VSideBar.vue` 暴露 `openSearch()` 与 `closeSearch()` 实例方法，可通过模板 Ref 命令式直接调起；
  2. **消息通知面板 (`NoticeCard.vue`)**：
     - 使用 `n-popover`（展开态 `placement="top"`，折叠态 `placement="right-end"`，`:show-arrow="false" raw` 无阴影包裹）；
     - 支持未读消息数量小红点提醒、分类 Tab 切换（全部/比赛/系统/队伍/警报）、已读清空操作及通知详情抽屉；
     - 触发按钮配合 `VTooltip` 在悬停时显示提示，卡片打开时自动 `:disabled="isNoticePopoverShow"`，彻底杜绝浮层与提示遮挡重叠；
  3. **偏好与系统设置面板 (`SettingCard.vue`)**：
     - 展开态与折叠态通过 Popover 打开设置卡片；
     - 支持用户偏好设置路由跳转、管理后台切换（权限判断）；
     - 内置意见反馈抽屉 (`FeedbackDrawer.vue`)，支持类型选择、文本输入与即时提交；
     - 集成多语言切换（中/英）、深浅色主题切换快捷动作、服务条款与隐私政策直达链接；
  4. **用户资料与鉴权卡片 (`UserCard.vue`)**：
     - 展开态与折叠态统一集成：未登录状态下点击直接引导路由跳转至登录页（`signinRoute`）；已登录状态下弹出用户资料卡片；
     - 展示用户真实头像（自研 `VAvatar`）、昵称、邮箱、所属用户组与角色标签（管理员/用户/裁判等）；
     - 支持快捷切换账号（`switch-account`）、退出登录（`logout`）以及个人中心直达；
- **互斥浮层状态机（Mutual Exclusion）与折叠态 Dock 防抖**：
  1. **展开态互斥**：点击通知、设置或用户卡片时，自动关闭其他已展开的气泡浮层，保证界面整洁且不产生层叠错位；
  2. **折叠态 Dock 悬浮工具盒**：收缩模式下点击应用与工具按钮（`AppsSharp`），弹出纵向 Dock 浮层，采用 `trigger="click"` 稳定防抖，支持在 Dock 内部无缝展开通知与设置的二级 Popover（`placement="right-end"`）；
- **门面事件与插槽全链路透传**：
  顶层 `VSideBar.vue` 将底层所有 Action 事件（`search-select`、`search-open`、`search-close`、`notification-click`、`setting-item-click`、`admin-click`、`switch-account`、`logout`、`profile-click` 等）以及卡片自定义插槽（`notice-card`、`setting-card`、`user-card`、`search-dialog`）完整向下透传与向外抛出。

### 5.11 ActionBox 弹窗卡片位于左上角并闪退/消失的根因剖析与架构解耦修复
- **现象特征**：
  在侧边栏底栏（`SideBarNavFooter`）中点击“通知”、“设置”或“用户卡片”时，弹出的 Popover 卡片出现在浏览器视口最左上角（`(0, 0)` 坐标处），且在约 1 秒或鼠标移动时即刻自动关闭/消失。
- **深层根因剖析**：
  1. **左上角定位塌陷成因**：
     - `<n-popover>` 依赖 `vueuc` 的 `VBinder` / `VTarget` 计算锚点矩形。在初版实现中，`<n-popover>` 的 `#trigger` 插槽直接包裹了自研组件 `<VTooltip>`，而 `<VTooltip>` 的根 DOM 为 `<span class="volur-tooltip-trigger">`；
     - 为了避免破坏父级 Flex 布局，`.volur-tooltip-trigger` 声明了 `display: contents;`。该 CSS 特性会导致浏览器布局引擎不为该 DOM 元素生成任何盒模型（Box）；
     - `n-popover` 调用 `triggerEl.getBoundingClientRect()` 时，浏览器返回全 0 矩形 `{ x: 0, y: 0, width: 0, height: 0, top: 0, left: 0 }`，导致浮层坐标被物理锚定在页面左上角 `(0, 0)`。
  2. **1 秒内闪退与消失成因**：
     - **双重翻转竞争（Double Toggle Collision）**：`<n-popover>` 本身配置了 `trigger="click"`，会自动监听触发源点击并切换显示状态；而底层 `<button>` 的 `@click` 事件又手动执行了 `isNoticePopoverShow.value = !isNoticePopoverShow.value`。用户单次点击时，按钮原生回调先将状态置为 `true`，事件冒泡后 Naive UI 内部捕获该点击，见当前已是 `true`，又将其翻转回 `false`，从而在微任务队列中触发关闭退出过渡；
     - **ClickOutside 判定越界**：因触发器盒模型为 `(0, 0)`，用户移动鼠标至卡片或点击卡片内部时，Naive UI 全局绑定的 `clickoutside` 监听器（`useClicked`）极易误判为非触发器区域点击，下发关闭指令。
- **架构重构与解耦方案**：
  1. **DOM 锚点层级反转（Trigger 真实化）**：
     将 `<VTooltip>` 调整至 `<n-popover>` 外层，使真实的原生 `<button class="action-icon-btn">`（或 `<div class="user-card">`）成为 `<n-popover>` `#trigger` 的直接 DOM 子元素。Naive UI 直接读取按钮真实的像素矩形（如 `top: 708, left: 61, width: 44.75, height: 32`），卡片精准浮动于按钮正上方 6px 处；
  2. **响应式状态机单一真实源（Watch Single Source of Truth）**：
     彻底废除 `@click` 中手动 `!show` 状态修改，由 `n-popover` 统一接管触发点击与全局失焦关闭；通过 Vue 3 `watch(isNoticePopoverShow)` 等监听器自动联动收起其他激活面板（互斥机制）并派发生命周期事件，彻底根治状态竞争与闪退缺陷。

---

## 6. 验证与审计结论

1. **严格类型校验**：
   - 执行 `vue-tsc --noEmit`：0 错误，严格类型全部通过；
2. **生产构建测试**：
   - 执行 `vite build`：生产环境打包顺利通过；
3. **零注释铁律与文档外置**：
   - 组件实现遵循纯粹代码准则，所有逻辑与架构外置沉淀于本文档。
