# VSideBar 二级菜单异常修复、下滑滚动流式容器及 AppLayout 容器重构

## 1. 变更背景与问题定位

在将原 `volur-lite-view` 工程的平台菜单与侧边栏体系迁移至新一代 `volur-lite-view-NEXT` 现代前端架构后，发现存在如下几项缺陷与视觉布局问题：

1. **命名统一**：
   - 管理后台首屏由“管理员控制台”规范重命名为“工作台”（Dashboard）；
   - 异步任务调度由“异步任务管理”精简重命名为“任务管理”（Tasks）。
2. **二级菜单文本隐藏与尺寸变形缺陷**：
   - **根本诱因**：Naive UI 的 `n-menu` 内部机制中，对于含 `children` 的父级菜单项（`n-submenu`），在未主动展开其手风琴时，其内部 `MenuOptionContent` 组件会自动追加 `.n-menu-item-content--collapsed` 类名；
   - 原样式文件在未限定于 `.n-menu--collapsed` 父级作用域的情况下，全局声明了 `:deep(.n-menu-item-content.n-menu-item-content--collapsed)`，强制将其设为 `width: 38px !important; height: 38px !important;`，并将文本容器 `.n-menu-item-content-header` 设为 `max-width: 0 !important; opacity: 0 !important; visibility: hidden !important;`，导致侧边栏在完全展开状态下，只要二级菜单处于收起态，其父级菜单文字便被彻底隐藏且按钮被强行压缩成 38px 方块；
   - 此外，缺少针对 `.n-submenu` 折叠态的集中收敛与子项缩进（`padding-left: 32px !important;`），以及在暗色模式下的展开箭头 `.n-menu-item-content__arrow` 和激活态 `.n-menu-item-content--child-active` 高亮响应。
3. **二级菜单下拉展开时列表项超出容器长度且无法下滑问题（中间还是不能滑动缺陷定位）**：
   - **根本诱因（已通过 Chrome DevTools 实时抓取底层样式与盒模型精确定位）**：
     1. Naive UI 内置样式（`naive-ui/lib/menu/src/styles/index.cssr.js`）向 `.n-menu` 默认注入了 `overflow: hidden;`；
     2. 当 `.sidebar-nav-container` 声明为弹性列容器（`display: flex; flex-direction: column; overflow-y: auto;`）时，其直接子元素 `.v-menu`（即 `.n-menu`）作为 Flex Item 默认具有 `flex-shrink: 1`；在自带 `overflow: hidden` 的 BFC 影响下，浏览器 Flexbox 布局算法强制将 `.v-menu` 的高度裁切并压缩至与父容器一致的视口高度（如 663px），超出部分在 `.v-menu` 内部被硬性截断；
     3. 这直接导致父容器 `.sidebar-nav-container` 的 `scrollHeight` 始终等于 `clientHeight`（均为 663px），判定 `canScroll` 为 `false`，中间区域根本不产生任何可滚动的差值，用户在菜单项上使用鼠标滑轮或触控板时完全无法滑动；
     4. 此外，当鼠标悬浮在侧边栏顶部 Header 或底部 Dock 等非滚动菜单区域时，滚轮事件未能拦截默认窗口冒泡并向中间容器派发穿透滚动。
4. **AppLayout 主体容器重构**：
   - 回滚原先对 `.app-content` 类选择器绑定的 `max-width` 样式；
   - 将 `max-width: 100rem` 直接挂载至 `main` 标签（HTML 标签与 Tailwind `max-w-[100rem] w-full mx-auto` 双重生效），保障组件内其他类名引用的纯粹性与一致性。

---

## 2. 核心改动点与实现

### 2.1 语言包与文案同步
- **[`src/locales/zh-CN/admin.json`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/locales/zh-CN/admin.json)**、**[`src/locales/zh-TW/admin.json`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/locales/zh-TW/admin.json)**、**[`src/locales/en-US/admin.json`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/locales/en-US/admin.json)**：
  - `dashboard.title`: `"工作台"` / `"工作台"` / `"Workspace"`；
  - `tasks.title`: `"任务管理"` / `"任務管理"` / `"Task Management"`；
  - 补齐二级菜单分组标题 `clusters.group_title: "集群容器"` 与 `risk.title: "风控管理"`。
- **[`src/router/routes/admin.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/router/routes/admin.ts)** 与 **[`src/views/admin/AdminDashboardView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/admin/AdminDashboardView.vue)**：
  - 路由元信息及后备兜底文案同步更新为“工作台”。

### 2.2 VSideBar 列表项超出与下滑滚动容器架构重构
- **[`src/components/sidebar/VSideBar.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/sidebar/VSideBar.vue)**：
  - **覆盖 Naive UI 内部溢出截断与 Flex Shrink 强制解除（彻底根治无法滚动）**：
    - 在展开态 `.sidebar-nav-container .v-menu` 及 `&.n-menu, :deep(.n-menu)` 上显式声明：
      ```scss
      overflow: visible !important;
      flex-shrink: 0 !important;
      height: auto !important;
      min-height: min-content !important;
      ```
    - 在 `:deep(.n-submenu)`、`.n-submenu-children` 以及 `:deep(.n-menu-item)` 上全面追加 `overflow: visible !important; flex-shrink: 0 !important; height: auto !important;`，彻底阻止 Flex 算法对子菜单及菜单项高度的挤压与截断；
    - 折叠态（`&.is-collapsed` / `&.n-layout-sider--collapsed`）下的 `.sidebar-nav-container .v-menu` 同样配置 `overflow: visible !important; flex-shrink: 0 !important; height: auto !important; min-height: min-content !important;`；
  - **滚动层级约束与触控平滑增强**：
    - `:deep(.n-layout-sider-scroll-container)` 与 `.sidebar-content-wrapper` 均严格约束为 `height: 100%; max-height: 100%; overflow: hidden;`，杜绝外层无限延展将底部栏挤出屏幕；
    - `.sidebar-header`、`.sidebar-primary-action-wrap` 以及 `.sidebar-bottom-area` 显式设为 `flex-shrink: 0;`，确保顶部 Logo/折叠触发器与底部 Dock/用户卡片在任何滚动态下牢固钉在两端；
    - `.sidebar-nav-container` 作为独立纵向弹性中间容器，配置 `flex: 1; min-height: 0; overflow-y: auto; overflow-x: hidden; overscroll-behavior: contain; -webkit-overflow-scrolling: touch; touch-action: pan-y;`，自动承接剩余高度；
    - 当二级菜单下拉时，内容自然且仅在其内部产生顺滑的上下滚动；`scrollHeight` 从 663px 实时扩展至 1052px+，`canScroll` 恒为 `true`；
  - **细致原生滚动条样式**：
    - 配置 4px 细致半透明滚动条（亮色 `rgba(60, 64, 67, 0.2)`，暗色 `rgba(255, 255, 255, 0.2)`），hover 时适度加深；
  - **滚轮穿透事件与防冒泡处理**：
    - 在 `handleSidebarWheel` 中，当鼠标位于顶部 Header 或底部 Dock 时，判定 `el.scrollHeight > el.clientHeight` 并调用 `e.preventDefault()` 阻止外层主页面滚动，同时向 `el.scrollTop += e.deltaY` 穿透派发滚动增量；
  - **滚动状态与吸顶联动监听**：
    - 将 `navContainerRef` 绑定至 `<div class="sidebar-nav-container">`，并在其上挂载 `@scroll="handleSidebarScroll"`，确保菜单内部滚动时能精准驱动顶栏毛玻璃与分割线的吸顶判定（`isSticky`）；
    - 同步升级 `scrollToTop` 与 `scrollContainerRef` 优先操纵 `navContainerRef`；
  - **二级菜单类名与尺寸规范**：
    - 彻底移除了全局泄露的 `:deep(.n-menu-item-content.n-menu-item-content--collapsed)` 规则块；
    - 规范展开态 `.n-submenu` 样式（子项 `padding-left: 32px !important;` 保持视觉层级，箭头靠右对齐平滑过渡）；
    - 折叠态（`&.n-menu--collapsed`）内收敛 `:deep(.n-submenu)` 居中与 38px 约束；
    - 暗黑模式（`html.dark`）下同步适配二级菜单箭头与激活态高亮背景。
  - **消除 n-submenu 异常顶部间距与按钮居中重构**：
    - **删除 n-submenu margin-top**：Naive UI 内置样式注入了 `.n-menu .n-submenu { margin-top: 6px; }`，导致二级菜单项与上方普通菜单项之间出现突兀的过大空隙；在 `:deep(.n-submenu)` 上显式声明 `margin-top: 0 !important; margin-bottom: 0 !important;`，恢复统一的 4px 垂直节奏；
    - **按钮靠左偏移根因与居中重构**：侧边栏容器可用内宽为 240px（256px 减去左右 padding 各 8px），此前菜单项写死了硬编码 `width: 220px; margin: 2px 0;`（左外边距为 0），导致按钮左边贴紧 8px 边界而右边空留 20px 巨大空白（出现 8px vs 28px 的视觉不对称，明显靠左偏斜）；全面将 `.v-menu`、`:deep(.n-menu-item)`、`:deep(.n-submenu) > .n-menu-item` 及子项的宽度调整为 `width: 100%`，与底部 Dock（240px）和用户卡片（240px）实现完美 100% 左右对称居中对齐；
    - **滚动条负边距穿透归位（方案 A）**：通过在 `.sidebar-nav-container` 上配置 `width: calc(100% + 8px); margin-right: -8px; padding-right: 8px;`，成功突破父级 `.sidebar-content-wrapper` 的 `padding-right: 8px` 限制，使滚动条自然、精确地坐落在最右侧 8px padding 槽位（贴紧右边框），同时为内部菜单按钮保留完整的 8px 缓冲与对称对齐。

### 2.3 AppLayout 背景与 main 标签尺寸重构
- **[`src/components/layouts/AppLayout.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/layouts/AppLayout.vue)**：
  - 根元素类名与 SCSS scoped 样式中背景统一设为 `#fff`；
  - 回滚 `.app-content` 类选择器上的 `max-width` 样式；
  - 在模板 `<main class="app-content ...">` 上声明 Tailwind `max-w-[100rem] w-full mx-auto`，并在样式中显式声明 `main { max-width: 100rem; width: 100%; margin: 0 auto; }`。
- **[`src/components/layouts/AdminLayout.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/layouts/AdminLayout.vue)**：
  - `<main>` 标签上的最大宽度类同步更新为 `max-w-[100rem] w-full mx-auto`。

---

## 3. 影响范围与验证结果

- **影响模块**：
  - `AdminLayout.vue` 管理后台侧边导航菜单与主视图区域；
  - `AppLayout.vue` 全局页面主布局框架；
  - `VSideBar.vue` 平台核心侧边栏组件。
- **验证结果**：
  - `npx vue-tsc --noEmit` 静态类型检查通过（0 错误）；
  - `npm run build` 生产构建打包成功（15.84s）；
  - 浏览器计算样式验证：
    - `n-submenu` 的 `margin-top` 准确归零（0px）；
    - 菜单按钮与子菜单、底部 Dock、用户卡片左右边距完全一致（8px 对称）；
    - `navContainer.distanceFromSiderRight` 准确为 `0px`，滚动条已精准对齐至侧边栏最右边缘（x = 252px ~ 256px）。
