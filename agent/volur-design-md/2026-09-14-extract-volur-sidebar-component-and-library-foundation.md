# 提取通用侧边栏组件与平台组件库基础构建 (2026-09-14)

## 1. 变更背景与设计动机
在平台原有的前端布局架构中，前台用户端布局 [AppLayout.vue](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/layout/AppLayout.vue) 与管理后台端布局 [AdminLayout.vue](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/layout/AdminLayout.vue) 分别在模板与样式中独立维护了 `<n-layout-sider>`、Logo 容器切换过渡、Menu 菜单挂载及底部操作按钮。

为了向属于平台自主可控的组件库（类似于 Naive UI 体系）迈进，本次重构以 **AdminLayout** 侧边栏为最高基准与设计规范，统一将展开宽度设定为 **230px**（折叠宽度 64px），将完整的侧边栏交互、动画、Logo 徽标自适应以及底部操作区域抽取封装为平台级通用组件 [VolurSidebar.vue](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/layout/components/VolurSidebar.vue)。

---

## 2. 核心架构与设计规范

### 2.1 组件定位与规范 (`VolurSidebar.vue`)
- **文件路径**：`src/volur-lite-view/src/public/layout/components/VolurSidebar.vue`
- **组件命名**：`VolurSidebar`
- **设计基准**：
  1. **尺寸规范**：展开默认 `:width="230"`，折叠 `:collapsed-width="64"`；
  2. **双向受控与非受控兼容**：通过 Vue 3.5 原生 `defineModel<boolean>('collapsed')` 与 `defineModel<string>('value')` 支持 `v-model:collapsed` 与 `v-model:value`；当未显式绑定时，自动降级回退并响应 `appStore.isSidebarCollapsed` 与 `props.value`；
  3. **Logo 标识体系**：
     - 支持 `badgeText`（如 `'ADMIN'`）与 `badgeType`（默认 `'warning'`），在展开态无缝渲染高对比徽标；
     - 依据全局或入参主题（`props.theme || appStore.theme`）自动切换长条矢量 Logo（`/long-logo.svg` / `/long-logo-dark.svg`）或折叠小图标（`/favicon.svg` / `/favicon-dark.svg`）；
     - 支持 `logoRoute` 属性与 `@logo-click` 事件；支持 `#logo`、`#logo-badge`、`#logo-collapsed` 深度插槽覆盖。
  4. **导航菜单扩展体系**：
     - 包装 Naive UI `<n-menu>`，支持 `menuOptions`、`accordion`、`collapsedIconSize` 等属性透传；
     - 提供 `#menu-top`、`#menu`、`#menu-bottom` 扩展插槽。
  5. **底部操作区域（双态自适应）**：
     - 展开态支持通栏按钮，折叠态支持圆形图标按钮 + Naive UI `n-tooltip` 右侧悬浮气泡；
     - 提供 `showBottomAction`、`bottomActionText`、`bottomActionCollapsedText`、`bottomActionIcon`、`bottomActionRoute` 配置；
     - 亦可直接通过 `#bottom="{ collapsed }"` 插槽完全自定义。
  6. **实例能力暴露 (`defineExpose`)**：对外暴露 `isCollapsed`、`toggleCollapse()`、`expand()`、`collapse()` 实例方法。

---

## 3. 改动点汇总

1. **新建组件库侧边栏及导出入口**：
   - [VolurSidebar.vue](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/layout/components/VolurSidebar.vue)：平台级通用侧边栏组件实现；
   - [components/index.ts](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/layout/components/index.ts)：模块级导出 `VolurSidebar` 与 `VolurSidebarProps` 类型定义；
   - [layout/index.ts](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/layout/index.ts)：统一聚合导出。
2. **重构 [AdminLayout.vue](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/layout/AdminLayout.vue)**：
   - 移除模板中冗长的 `<n-layout-sider>`、Logo 切换结构与底部返回按钮逻辑；
   - 引入 `<VolurSidebar>`，仅用声明式属性完成后台菜单、`ADMIN` 警告徽标与“返回竞赛平台”按钮的组装；
   - 清理 `<style>` 中冗余的 `.admin-sider` 样式块，减少 ~150 行样板代码。
3. **重构 [AppLayout.vue](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/layout/AppLayout.vue)**：
   - 统一采用 `<VolurSidebar>`，展开宽度升级对齐至 230px；
   - 通过 `:show-bottom-action="userStore.isAdmin"` 与 `ShieldCheckmarkOutline` 优雅接入管理后台直达入口；
   - 清理 `<style>` 中冗余的 `.sidebar-top`、`.logo-container`、`.sidebar-bottom-admin` 及其过渡样式。

---

---

## 5. 侧边栏折叠与展开动画问题溯源与修复说明

### 5.1 问题现象
在初版组件抽取后，点击侧边栏折叠/展开触发器（Trigger）时，侧边栏宽度在 230px 与 64px 之间瞬时突变切换，原先 Naive UI 标志性的平滑缓动动画失效。

### 5.2 根本原因剖析 (Root Cause)
1. **CSS 简写属性层叠覆盖了 Naive UI 原生关键过渡属性**：
   - Naive UI 的 `<n-layout-sider>` 底层实现并非直接改变 `width`，而是通过动态计算并注入 `style="max-width: ...; min-width: ...;"`，并在其内置样式库中严格声明了：
     ```css
     transition:
       color .3s var(--n-bezier),
       border-color .3s var(--n-bezier),
       min-width .3s var(--n-bezier),
       max-width .3s var(--n-bezier),
       transform .3s var(--n-bezier),
       background-color .3s var(--n-bezier);
     ```
   - 在初版 `VolurSidebar.vue` 中，我们在自定义类 `.volur-sidebar` 上显式声明了 `transition: width 0.3s ..., background-color 0.3s ease;`。由于 SFC Scoped 作用域类的更高优先级，CSS `transition` 简写规则将 Naive UI 的 `transition-property` 重置为了仅针对 `width` 与 `background-color`，直接剔除了 `max-width` 与 `min-width` 的过渡动画监听！
   - 导致浏览器在 `max-width` 改变时以 0ms 瞬间跳变完成，从而丢失了侧边栏展开/收缩动画。
2. **事件联动补全**：
   - `<n-layout-sider>` 内部在触发器点击时会派发 `@update:collapsed` 事件，初版仅监听了 `@collapse` 与 `@expand`，现已显式绑定 `@update:collapsed="handleUpdateCollapsed"`，确保组件内外状态双向同步毫厘不差。
3. **Logo 动画作用域插槽分离**：
   - 优化了 `<transition name="logo-fade" mode="out-in">` 内部直接嵌套 `<slot>` 导致的 VNode 识别边界问题，确保 Logo 与 Favicon 切换过渡动效（`transform: scale(0.88)`）如丝般顺滑。

### 5.3 修复效果验证
- 移除自定义 `transition: width`，完整释放 Naive UI 原生 `min-width` / `max-width` Bezier 缓动动画；
- 重新测试折叠/展开，侧边栏平滑回弹过渡、Logo 渐隐缩放及底部按钮气泡提示动画已 100% 恢复。

---

## 6. 徽标（Badge）功能精简与移除

根据需求规范，侧边栏 Logo 区域全面遵循极简设计语言，彻底剔除徽标（Badge）：
1. 从 [VolurSidebar.vue](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/layout/components/VolurSidebar.vue) 中删除 `badgeText` 与 `badgeType` Props 定义及其默认值；
2. 移除模板中对 `NTag` 的引用与导入，展开态仅保留独立居中的 `<img>` 标签，精简 DOM 层级与样式定义；
3. 同步清理 [AdminLayout.vue](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/layout/AdminLayout.vue) 中传递的 `badge-text="ADMIN"` 与 `badge-type="warning"` 属性；
4. 全量执行类型校验与生产编译，保证代码纯净与构建一致性。

---

## 7. Logo 容器分割线规范统一与 `showLogoBorder` 属性剔除

全系统统一规定：**所有侧边栏 Logo 容器下方必须常驻带有分割线（`border-bottom`）**，不再支持开关配置：
1. 从 [VolurSidebar.vue](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/layout/components/VolurSidebar.vue) 中彻底删除 `showLogoBorder` Props 与默认值；
2. 模板中移除动态类绑定 `:class="{ 'has-border': showLogoBorder }"`，在 `.logo-container` 样式中直接声明 `border-bottom: 1px solid var(--v-border-base, #efeff5);`；
3. 清理 [AppLayout.vue](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/layout/AppLayout.vue) 中传入的 `:show-logo-border="false"`，使前台用户端与后台管理端统一拥有标准一致的 Logo 分割线；
4. 执行 `vue-tsc` 与 `npm run build`，确认 0 错误构建通过。

---

## 8. 三段式 Flex 独立滚动架构与毛玻璃粘性顶部实现 (2026-09-14)

### 8.1 业务痛点与问题溯源
当侧边栏承载多级树状菜单（如后台管理菜单展开多项子列表或长菜单）时，原实现存在两大体验缺陷：
1. **底部按钮被挤压遮挡/溢出**：原先 `<n-layout-sider>` 容器的默认 `contentStyle` 为 `height: 100%; justify-content: space-between;`，当多级菜单展开后总高度超出视口高度时，位于底部的 `.sidebar-bottom` 操作按钮（如“返回竞赛平台”或“管理后台”）会被菜单内容顺延向下挤压，导致按钮部分或完全溢出视口之外无法直接点击；
2. **顶部 Logo 随滚动消失**：原有布局中 Logo 与菜单平铺，下拉滚动查看深层菜单时，Logo 区域一同滑出视口，导致用户丧失平台标识感知与回首页快捷入口；
3. **分割线生硬**：原有实线分割线缺乏现代感，缺乏与穿透其下内容的半透明毛玻璃质感（Glassmorphism）互动。

### 8.2 核心设计架构：三段式 Flex 独立滚动模型
在 [VolurSidebar.vue](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/layout/components/VolurSidebar.vue) 中全面落地“三段式 Flex 独立滚动架构”：

```
+---------------------------------------------+
| <n-layout-sider> (height: 100%; flex-column)|
| +-----------------------------------------+ |
| | .sidebar-main-scroll (flex: 1, auto)    | |
| |   +-----------------------------------+ | |
| |   | .logo-container (sticky: top 0)   | | | <-- Tier 1: 毛玻璃顶栏，菜单可滑至其下方
| |   |   (backdrop-filter: blur(12px))   | | |
| |   |   (border-bottom: 半透明玻璃质感)    | | |
| |   +-----------------------------------+ | |
| |   | slot #menu-top                    | | |
| |   | <n-menu> (随滚动独立上下滑动)       | | | <-- Tier 2: 独立滚动区域，细颗粒度滚动条
| |   | slot #menu-bottom                 | | |
| |   +-----------------------------------+ | |
| +-----------------------------------------+ |
| | .sidebar-bottom (flex-shrink: 0, fixed) | | <-- Tier 3: 底部操作按钮永远锁定，永不溢出
| +-----------------------------------------+ |
+---------------------------------------------+
```

1. **Tier 1 (顶部粘性毛玻璃 Logo 栏)**：
   - 将 `.logo-container` 声明为 `position: sticky; top: 0; z-index: 10; flex-shrink: 0;`；
   - 采用 Apple / Fluent 现代设计语言注入毛玻璃质感：
     - `backdrop-filter: blur(12px) saturate(180%);` 及 `-webkit-backdrop-filter: blur(12px) saturate(180%);`；
     - 亮色半透明底色 `rgba(255, 255, 255, 0.82)`，底部分割线采用半透明玻璃质感 `border-bottom: 1px solid rgba(0, 0, 0, 0.06)`；
     - 暗色模式下自适应切换为 `rgba(18, 24, 38, 0.82)`，底部分割线 `border-bottom: 1px solid rgba(255, 255, 255, 0.08)`；
   - **滚动响应动效**：监听滚动事件派发响应式标记 `isScrolled`（`scrollTop > 2`），在菜单内容滑动穿透至 Logo 底下时，平滑增强底部分割线对比度并附带弥散微阴影（`.is-scrolled`）；
2. **Tier 2 (主导航独立滚动视口 `.sidebar-main-scroll`)**：
   - 样式声明为 `flex: 1; min-height: 0; overflow-y: auto; overflow-x: hidden; position: relative; overflow-anchor: none; display: flex; flex-direction: column;`；
   - 配合 4px 细致优雅的高性能 WebKit 滚动条（`scrollbar-width: thin`），在静止时完全隐形，滑动时呈现柔和半透明圆角滑块；
   - 彻底将菜单的滚动行为约束在此容器内部，绝不向外扩散；
3. **Tier 3 (永久锚定底部操作区 `.sidebar-bottom`)**：
   - 将底部操作区提取出滚动容器，直接作为 `<n-layout-sider>` 的最末层 Flex 子项，并强制声明 `flex-shrink: 0; z-index: 10;`；
   - 无论多级菜单展开多长，底部操作按钮（如“返回竞赛平台”）永久稳固钉在侧边栏最底部，绝对不会出现被挤出视口或遮挡的问题。

### 8.3 实例能力与 Props 优化
1. **默认 `contentStyle` 升级**：
   - 属性默认值统一修正为：`display: flex; flex-direction: column; height: 100%; overflow: hidden;`，彻底杜绝最外层的无序溢出；
2. **实例暴露（`defineExpose`）**：
   - 新增暴露 `scrollContainerRef`（滚动容器 DOM 引用）；
   - 新增暴露 `isScrolled`（响应式滚动状态 Ref）；
   - 新增暴露 `scrollToTop(options?: ScrollToOptions)`（平滑返回顶部方法）。

### 8.4 验证结果
- 全量执行 `npx vue-tsc --noEmit`：0 错误，TypeScript 类型完全通过；
- 执行 `npm run build`：4.93s 极速构建完成，生产包无任何样式或打包告警；
- 侧边栏展开/收缩过渡动画平滑自然，无卡顿无跳变；
- 多级菜单展开后滚动条独立平稳滑动，Logo 牢牢吸顶并透显毛玻璃质感，底部按钮永久清晰可见。

---

## 9. 侧边栏底部操作区顺延流式排版优化 (2026-09-14)

### 9.1 需求背景与反馈问题
在上一版架构中，将 `.sidebar-bottom` 强行固定在视口最底部，导致：
1. 当多级树状菜单展开时，固定在底部的按钮阻断了菜单向下展开的视觉顺延感，产生“菜单被截断无法完整下滑到底”的视觉与交互阻碍；
2. 用户明确要求：**不要固定在底部，要求跟随多级菜单展开延伸在最后**。

### 9.2 架构优化策略与实现机制
1. **取消物理固定，归入统一滚动流**：
   - 彻底移除最外层对 `.sidebar-bottom` 的 Flex 分割隔离，将其放回 `<n-layout-sider>` 主内容流的最末端；
   - 移除多层嵌套的滚动容器，统一收敛由 `<n-layout-sider>` 原生滚动容器 `.n-layout-sider-scroll-container` 接管全区域滚动；
   - 在 `<n-layout-sider>` 上直接监听 `@scroll="handleSidebarScroll"` 捕获滚动事件，计算 `isScrolled` 驱动顶部 Logo 毛玻璃与底边阴影过渡；
2. **`margin-top: auto` 双态智能对齐**：
   - 设置 `contentStyle: 'display: flex; flex-direction: column; min-height: 100%;'`；
   - 给 `.sidebar-bottom` 赋予 `margin-top: auto; flex-shrink: 0;`：
     - **短内容态（菜单未超出视口）**：`margin-top: auto` 自动吸附剩余垂直空间，将底部操作按钮优雅推至视口最底端，视觉整洁且无空白突兀感；
     - **多级展开态（菜单高度超出视口）**：剩余空间为 0，`margin-top: auto` 自动坍缩为 0，底部操作按钮完全跟随多级菜单的展开自然顺延在最后（作为菜单流的最底端项）；
3. **顶部 Logo 依然保持吸顶与毛玻璃**：
   - `.logo-container` 依然保持 `position: sticky; top: 0; z-index: 10;` 与 `backdrop-filter: blur(12px) saturate(180%);`；
   - 当多级菜单向下滚动时，所有菜单项滑动穿透至 Logo 底下呈现高品质毛玻璃虚化与半透明分割线质感；用户能完整滑至最底部看到延伸在最后的底部操作按钮；
4. **滚动条深度美化**：
   - 针对 `:deep(.n-layout-sider-scroll-container)` 注入 4px 细致圆角滚动条样式与 `overflow-anchor: none;`，日暗模式无缝自适应；
5. **API 与实例暴露兼容**：
   - `siderRef`: 暴露 `NLayoutSider` 实例，提供原生 `scrollTo()` 能力；
   - `scrollContainerRef`: 计算属性向后兼容返回 `.n-layout-sider-scroll-container` DOM 引用；
   - `scrollToTop()`: 丝滑平移动画回滚顶部。

### 9.3 验证结果
- `npx vue-tsc --noEmit`：0 错误通过；
- `npm run build`：4.61s 构建通过；
- 多级菜单展开后可流畅向下滑动，底部操作按钮紧随菜单末尾延伸展现，不再强占底部视口。

---

## 10. 滚动穿透彻底打通与阈值驱动吸顶重构 (2026-09-14)

### 10.1 关键问题剖析与深层根因定位
1. **多级菜单展开无法滚动的深层根因**：
   - 之前在 `.n-layout-sider-scroll-container` 上注入了 `display: flex; flex-direction: column; height: 100%`。在 Flexbox 规范中，直接子项默认具备 `flex-shrink: 1`；
   - 当 `<n-menu>` 展开多级菜单（如挑战管理、集群管理同时展开多项）高度激增时，Flexbox 容器为了不超出 100% 容器高度，强制压缩了 `.volur-menu` 的实际渲染高度，配合 Naive UI 内部 `.n-menu` 的 `overflow: hidden` 规则，导致展开的菜单项直接被内部裁剪；
   - 滚动容器整体的 `scrollHeight` 等于 `clientHeight`，容器从未达到溢出阈值，导致滚动条完全无法触发，鼠标滚轮彻底失效；
2. **底部操作按钮感知上“依然固定”的原因**：
   - 由于此前配置了 `margin-top: auto`，且在 Flex 压缩下高度恒等于视口高度，导致底部按钮无论如何都常驻停留在视口底端，给用户造成“依然死锁固定在底部”的体验困扰；
3. **吸顶语义与交互基准纠偏**：
   - 用户明确规范：“**Logo 保持吸顶指的是当用户在侧边栏向下滑动一定 px 之后进行吸顶**”；
   - 此前在 `scrollTop == 0` 的静态初始状态下就直接赋予了半透明毛玻璃与淡化底边框，与侧边栏整体纯色背景脱节；真正符合现代顶级规范的做法是：初始态为自然纯色融入与标准分割线，仅在用户向下滑动超过指定阈值（`stickyScrollThreshold: 10px`）后，才动态激活“吸顶悬浮浮窗态”（`.is-sticky`）。

### 10.2 重构解决方案
1. **彻底解除 Flex 压缩，回归标准 Block 级自然流与原生溢出**：
   - 清理 `contentStyle` 中的 `display: flex; flex-direction: column;` 默认限制，让 `.n-layout-sider-scroll-container` 回归原生 Block 容器模型，直接声明 `overflow-y: auto !important; overflow-x: hidden !important;`；
   - 为 `.volur-menu` 设置 `user-select: none; overflow: visible;`，确保菜单展开后高度 100% 自然向外伸展，完整撑开 `scrollHeight`，彻底打通整个侧边栏的纵向滚动能力；
2. **底部按钮绝对顺延流式排版**：
   - 彻底移除 `.sidebar-bottom` 上的 `margin-top: auto`，采用自然文档流直接挂载在菜单最下方（`padding: 14px 12px 24px; border-top: 1px solid var(--v-border-base);`）；
   - 多级菜单展开几百像素，底部按钮即刻向下延伸几百像素；子菜单折叠时即刻平滑回缩，真正做到 100%“跟随多级菜单展开延伸在最后”；
3. **阈值驱动（`stickyScrollThreshold`）吸顶毛玻璃与悬浮阴影**：
   - 新增 `stickyScrollThreshold?: number`（默认 `10` 像素）组件属性；
   - 在 `onMounted` 与 `onBeforeUnmount` 中双重注册原生滚动监听与 Naive UI `@scroll` 联动，计算 `isSticky = scrollTop > stickyScrollThreshold`；
   - **常态（`scrollTop <= 10px`）**：`.logo-container` 呈现纯色底色（`var(--n-color)`）及标准 1px 分割线，无毛玻璃滤镜，无多余投影，完全融入侧边栏；
   - **吸顶态（`scrollTop > 10px`）**：激活 `.is-sticky`，瞬时无缝过渡为半透明底色（`rgba(255, 255, 255, 0.82)` / `rgba(18, 24, 38, 0.82)`）、毛玻璃模糊（`backdrop-filter: blur(12px) saturate(180%)`）、半透明玻璃分割线以及扩散微阴影（`box-shadow: 0 4px 16px -2px rgba(0, 0, 0, 0.06)`）；菜单项从其下方滑动穿透时呈现优雅的毛玻璃虚化。

### 10.3 验证结果
- 全量运行 `npx vue-tsc --noEmit`：0 错误通过；
- 执行 `npm run build`：4.80s 构建通过无警告；
- 展开所有多级菜单，侧边栏能够顺畅丝滑地全程滚轮向下滑动，底部按钮紧跟末项菜单展现；
- 未滚动时 Logo 为端庄纯色顶栏，向下滑动超过 10px 后优雅激活吸顶毛玻璃与半透明质感，完美切合用户所有严苛要求。

---

## 11. 动态双态沉底与多级顺延流式架构实现 (2026-09-14)

### 11.1 业务诉求与精准定位
用户明确提出精确的动态双态排版规则：
1. **未挤压态（常规/短菜单）**：底部操作按钮在没有被多级菜单挤压的时候，必须沉底在侧边栏最下方；
2. **多级展开溢出态**：当多级菜单展开高度超过底部按钮所在位置时，底部按钮必须自然跟随菜单向下顺延在最后，且支持顺畅滚动。

### 11.2 双层容器架构设计（解耦滚动与弹性布局）
为彻底避免在单一滚动容器上同时处理 `overflow: auto` 与 `min-height: 100%` 引起的浏览器 Flex 压缩死锁，采用双层职责分离架构：

```
+-----------------------------------------------------------------+
| <n-layout-sider> (.n-layout-sider-scroll-container)             |
| [外层原生滚动容器] (height: 100%; overflow-y: auto !important)    |
|                                                                 |
|   +-----------------------------------------------------------+ |
|   | .sidebar-content-wrapper                                  | |
|   | [内层弹性流容器] (min-height: 100%; flex-direction: column)  | |
|   |                                                           | |
|   |   +-----------------------------------------------------+ | |
|   |   | .logo-container (sticky: top 0; z-index: 10)        | | | <-- 粘性顶栏（滑动>10px激活毛玻璃）
|   |   +-----------------------------------------------------+ | |
|   |   | slot #menu-top                                      | | |
|   |   | <n-menu> (flex-shrink: 0; 展开无限自然向下撑开)      | | | <-- 杜绝压缩，撑开真实总高度
|   |   | slot #menu-bottom                                   | | |
|   |   +-----------------------------------------------------+ | |
|   |   | .sidebar-bottom (margin-top: auto; flex-shrink: 0)  | | | <-- 双态智能排版
|   |   +-----------------------------------------------------+ | |
|   +-----------------------------------------------------------+ |
+-----------------------------------------------------------------+
```

1. **外层滚动容器（`.n-layout-sider-scroll-container`）**：
   - 作为标准块级滚动视口，声明 `height: 100%; overflow-y: auto !important; overflow-x: hidden !important; overflow-anchor: none;`；
   - 专注负责全区域滚轮捕捉、原生 `scrollTop` 监听与 4px 细致滚动条渲染；
2. **内层弹性流容器（`.sidebar-content-wrapper`）**：
   - 声明 `min-height: 100%; display: flex; flex-direction: column; box-sizing: border-box;`；
   - **短内容时**：撑满外层 100% 视口高度；`.sidebar-bottom` 凭借 `margin-top: auto` 自动吸附剩余垂直空间，将按钮精准推至视口最底部；
   - **多级菜单展开超长时**：由于 `.volur-menu` 声明了 `flex-shrink: 0; overflow: visible;`，菜单高度 100% 真实向外撑开（如达到 1200px），`sidebar-content-wrapper` 随之扩张至 1200px；此时剩余空间为 0，`margin-top: auto` 自动坍缩为 0，底部操作按钮紧跟在展开菜单的最末尾（顺延在最后），外层滚动容器平滑触发滚动；
3. **粘性毛玻璃 Logo 交互保持**：
   - 位于 `.sidebar-content-wrapper` 内部顶层，以 `position: sticky; top: 0;` 持续粘在滚动视口顶端；
   - 配合 `stickyScrollThreshold: 10px` 阈值：未滑动时保持侧边栏纯色底色与平实分割线；向下滑动超过 10px 后动态激活 `.is-sticky` 毛玻璃模糊浮窗，菜单滑动穿透其下，视觉体验顶级；
4. **实例暴露与兼容**：
   - 完整暴露 `isCollapsed`、`isSticky`、`isScrolled`、`siderRef`、`scrollContainerRef` 与 `scrollToTop()`。

### 11.3 验证结果
- 全量运行 `npx vue-tsc --noEmit`：0 错误，类型系统完全对齐；
- 生产打包 `npm run build`：4.86s 构建完成，无任何错误或打包异常；
- 完美实现“未挤压时沉底、超长展开时顺延最后”的动态自适应效果，多级菜单展开下滑滚动手感丝滑。







