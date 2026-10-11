# Google AI Studio 风格侧边栏上中下分层重构与顶部栏移除设计审计记录

## 1. 变更背景与需求目标
为进一步深度对齐 Google AI Studio 与 Google Workspace 沉浸式工作台的设计交互，本次重构包含以下两大核心改动：
1. **移除业务页面顶部栏**：针对 [`CompetitionListView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/competition/CompetitionListView.vue) 移除冗余的 `<header>` 顶部导航栏，释放垂直视觉空间；
2. **侧边栏上中下三层物理分层架构**：对 [`VSideBar.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/sidebar/VSideBar.vue) 实施清晰的“上、中、下”分层设计：
   - **上层 (Header)**：Logo 居左，收缩侧边栏按钮（汉堡按钮）居右；
   - **中层 (Navigation)**：独立的侧边栏主导航列表项（采用 Material Design 3 胶囊高亮与 Rail 胶囊指示器）；
   - **下层 (Bottom Dock)**：底部固定 Action 操作框（集成搜索、通知、设置、深色/浅色模式切换 4 大快捷按钮）以及用户信息框（自适应登录/未登录态）。

---

## 2. 关键重构与实现细节

### 2.1 CompetitionListView.vue 顶部栏安全移除
- 移除了原有的 `<header class="sticky top-0 ...">` 结构（包含长 Logo、搜索输入框与登录按钮）；
- 清理了未引用的 `SearchOutline`、`CloseOutline` 图标导入，以及 `isLoggedIn`、`handleSearch`、`clearSearch`、`goToLogin`、`goToProfile` 等未消费方法与响应式变量；
- 避免了 `vue-tsc` 的 `noUnusedLocals` 编译报错，全页直接由侧边栏驱动全局导航。

### 2.2 VSideBar.vue 上中下三层布局重构

#### 1. 上层 (Top Header - Logo 在左，收缩按钮在右)
- **展开态 (`!isCollapsed`)**：
  - 左侧：品牌 Logo 容器（`brand-container`），内边距与边距优化（`padding: 0 16px 0 24px; margin-left: 6px`），将 Logo 略微往居中方向微调，避免过度贴靠左侧边缘；点击可跳转目标路由，支持 `#logo` 插槽定制；
  - 右侧：收缩按钮（`sidebar-toggle-btn`），带圆形悬浮涟漪与 `t('common.collapse_sidebar')` 提示，点击收折侧边栏。
- **折叠态 (`isCollapsed`)**：
  - 侧边栏宽度 68px，在顶栏正中居中常驻切换按钮（点击即可平滑展开回“左 Logo 右按钮”结构）。

#### 2. 中层 (Middle Navigation - 独立列表项)
- 容器采用 `flex: 1; min-height: 0; overflow-y: auto;` 承载可滚动的菜单项；
- 支持 `computedMenuOptions` 与 Naive UI `n-menu`，保持 Google M3 Navigation Drawer（`border-radius: 9999px` 悬浮药丸胶囊、`#c2e7ff` / `rgba(168, 199, 250, 0.16)` 激活底色）与 Navigation Rail（56px × 36px 居中胶囊指示器）的视觉标准；
- 保留 `#menu-top` 与 `#menu-bottom` 扩展插槽。

#### 3. 下层 (Bottom Dock - Flex 布局、独立微边框与精简用户信息)
- **取消顶边线**：彻底移除 Bottom Dock 的 `border-top` 分隔线，保持底栏通透与轻量质感；
- **纯 Flex 弹性布局**：下层容器与子元素全面采用 `display: flex; flex-direction: column; gap: 8px;` 弹性排列；
- **去胶囊化与独立边框卡片**：
  - 底部所有元素均不再使用大圆角药丸胶囊设计，统一采用规整且轻量的矩形卡片边框：
    - 边框颜色统一采用 `#d7d8da80`（支持暗色模式自适应联动高亮）；
    - 圆角统一采用 `border-radius: 8px`；
  - **Action 框 (Action Box)**：
    - 包含 4 个核心功能图标按钮（搜索、通知、设置、深浅模式切换）；
    - 展开态：横向 `flex` 分布，每个按钮单独具备 `border: 1px solid #d7d8da80; border-radius: 8px; height: 36px;` 质感边框；
    - 折叠态：纵向居中紧凑排列，同样采用 38px × 38px 的 `border-radius: 8px` 独立边框按钮。
  - **精简用户信息框 (User Profile Box)**：
    - 采用独立的 `border: 1px solid #d7d8da80; border-radius: 8px; padding: 8px 10px;` 卡片呈现；
    - **极简信息布局**：彻底移除了原先右侧的 `>` 箭头标志，并移除了“个人中心”字样标签，仅保留纯粹的用户头像（或首字母/访客图标 + 在线小绿点）与用户名、邮箱/未登录引导文本，界面极致纯粹高雅；
    - 折叠态：居中展示 38px × 38px、`border-radius: 8px` 的头像按钮。

### 2.3 尺寸、边距与色彩精细化微调
1. **背景颜色与表面通透度**：
   - 侧边栏 `.v-sidebar` 与粘性顶栏 `.sidebar-header` 的亮色背景色统一调整为干净轻薄的 `#fafafa`；
   - 滚动容器背景设置为 `transparent`，与整站 Google 护眼灰底自然融为一体。
2. **边框色统一为 #d7d8da80**：
   - 侧边栏右边框 `.v-sidebar`、Naivie UI 内部边框 `.n-layout-sider__border` 以及粘性顶栏底部分割线统一采用 `#d7d8da80`（支持暗色模式联动自适应）；
   - 与底栏 Action 框与用户信息框的卡片边框形成统一的微边框质感视觉层级。
3. **Extend (展开) 模式宽度定义澄清（内部列表项/元素 220px，不计入 Padding）**：
   - 展开态下“宽度 220px”明确指内部列表项（`.n-menu-item`、`.v-menu`）、顶栏内容与底栏 Action 框/用户卡片宽度本身为 `220px`；
   - 配合外层设定的 `padding: 0 18px`（左右各 18px，总计 36px），侧边栏展开总宽度自然为 `calc(220px + 36px) = 256px`；
   - 在 `VSideBar.vue`、`SideBar.vue` 与 `types.ts` 中同步默认值 `width: 256`，在 CSS 中通过 `width: calc(220px + 36px) !important;` 严格约束；
   - 业务视图 `CompetitionListView.vue` 浮动控制器相应保持 `md:left-[272px]`。
4. **内部元素决定宽度与 0 18px 边距规范**：
   - 侧边栏内容外层容器 `.sidebar-content-wrapper` 统一设定水平内边距 `padding: 0 18px`；
   - 折叠模式下放弃硬编码宽度，采用 `width: fit-content !important; max-width: fit-content !important;`，由内部统一规格的 38px 元素（切换按钮、菜单项、Action 按钮、头像按钮）加上两侧各 18px padding 动态自然撑开（总宽度自然收敛为 74px），达到由内部元素决定的自适应表现。

### 2.4 折叠逻辑修复、Menu Flex 布局重构与全局滚动条隐藏
1. **收缩/展开响应性与物理尺寸收缩修复**：
   - 为 `<n-layout-sider>` 根节点补充 `:class="['v-sidebar', { 'is-collapsed': isCollapsed }, siderClass]"`，确保折叠态具备显式的 `.is-collapsed` 类名；
   - 修复 CSS 中折叠态宽度规则：在 `&.is-collapsed, &.n-layout-sider--collapsed` 下显式约束总宽度为 `calc(38px + 36px) !important;`（即 74px），内部各子元素（头部切换按钮、菜单项、Action 按钮、用户头像）在折叠态强制收敛为 `38px` 并在两边 18px 内边距间居中，避免被内部原本 220px 宽度的菜单项撑大导致无法收缩；
   - 在 `toggleCollapse`、`collapse`、`expand` 与 `handleUpdateCollapsed` 全链路中同步 `isCollapsed`、`modelCollapsed`、`appStore.isSidebarCollapsed`，并在包装组件 `SideBar.vue` 中绑定 `@collapse` 与 `@expand` 回调，全链路保障状态机响应；按钮点击添加 `@click.stop` 阻断冒泡干扰。
2. **中间内容 Menu 容器全面采用 Flex 布局**：
   - 导航主容器 `.sidebar-nav-container` 声明 `display: flex; flex-direction: column;`；
   - 菜单主体 `.v-menu` 与 Naive UI `:deep(.n-menu)` 声明 `display: flex; flex-direction: column; gap: 4px;`；
   - 菜单项 `:deep(.n-menu-item)` 与项内容 `:deep(.n-menu-item-content)` 均采用 `display: flex; align-items: center;`，实现标准的 Flex 弹性流布局。
3. **全局无感滚动条隐藏**：
   - 针对 `.v-sidebar`、内部滚动容器 `:deep(.n-layout-sider-scroll-container)`、`.sidebar-content-wrapper` 与 `.sidebar-nav-container`，在亮色和暗色模式下统一应用：
     `scrollbar-width: none !important; -ms-overflow-style: none !important; &::-webkit-scrollbar { display: none !important; width: 0 !important; height: 0 !important; }`；
   - 保留内容超出时的垂直滚动交互，同时彻底移除了原本可见的滚动条轨道与滑块，保证沉浸式轻量质感。

---

## 3. 规范与工程验证
- **纯粹代码与零注释铁律**：所有修改的 `.vue` 与 `.ts` 代码文件均保持 0 行注释；
- **国际化词条补充**：在 `locales/{zh-CN, zh-TW, en-US}/common.json` 补充了 `search`、`notifications`、`settings`、`theme_dark`、`theme_light`、`signin`、`user_center` 词条；
- **组件属性与事件透传**：在包装组件 `SideBar.vue` 与 `types.ts` 中完整支持并向下透传上述所有配置项与回调事件；
- **构建验证**：全项目执行 `npm run build`（`vue-tsc --noEmit && vite build`）通过，无任何类型错误或模板报错。



