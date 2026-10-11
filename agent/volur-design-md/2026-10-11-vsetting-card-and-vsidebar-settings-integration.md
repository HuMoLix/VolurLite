# VSettingCard 组件创建与 VSideBar 设置面板弹出层集成审计记录

## 1. 变更背景与架构动机
在 `volur-lite-view-NEXT` 现代前端工程中，侧边栏底部停靠区（Bottom Dock）提供了包括搜索、通知、设置及主题切换等在内的核心快捷入口。此前 Bottom Dock 中的设置按钮仅作为直接路由跳转或简单事件派发触发器，缺少与整体侧边栏交互设计语言保持一致的二级设置浮层体验。

为了使设置操作与已有的通知卡片（`VNotice.vue`）和用户卡片（`VUserCard.vue`）在视觉风格与交互结构上达到高度统一，本次重构对 `src/components/sidebar/VSideBar.vue` 中 Bottom Dock 的设置按钮进行全面升级：
1. 以平台级通用无边框卡片基底 `src/components/sidebar/VCard.vue` 为容器基础，独立创建 `src/components/sidebar/VSettingCard.vue` 设置卡片组件；
2. 在设置列表中提供清晰规范的功能项目：
   - **User Settings**（用户设置，使用 `SettingsOutline` 图标）；
   - **Language Settings**（语言设置，使用 `GlobeOutline` 图标，并在右侧配置 `IosArrowForward` 箭头图标指示存在下级选项）；
   - **分割线**（`setting-divider`）；
   - **Send feedback**（发送反馈，使用 `FlagOutline` 图标）；
   - **Terms of service**（服务条款，使用 `DocumentTextOutline` 图标）；
   - **Privacy policy**（隐私权政策，使用 `ShieldCheckmarkOutline` 图标）；
3. 为 Language Settings 引入 Hover 即时触发的二级语言选择菜单（二级浮层同样继承 `VCard` 设计与主题），支持对简体中文（`zh-CN`）、繁體中文（`zh-TW`）与英文（`en-US`）的无缝动态切换与高亮选中。

---

## 2. 核心改动点

### 2.1 新增设置卡片组件 `VSettingCard.vue`
- 文件路径：`src/components/sidebar/VSettingCard.vue`；
- 容器结构：
  - 以 `<VCard>` 作为顶层根容器，默认宽度 240px，内边距 `6px`，元素间距 `2px`；
  - 严格遵循 Google Material 3 / Google Workspace 主题规范与阶梯色体系（亮色 `#fcfcfc`、深色 `#191919`，搭配阴影与 `:global(.n-popover:has(.v-setting-card))` 穿透重置）；
  - 单个选项高度 38px，圆角 8px，包含 Google 灰阶悬停微动效（`rgba(60, 64, 67, 0.08)` / 深色 `rgba(255, 255, 255, 0.08)`）；
  - 列表项：
    1. **User Settings**：点击后通过 Vue Router 路由跳转至 `userSettingsRoute`（默认 `/settings`），派发 `user-settings-click` 与 `item-click` 事件并关闭弹窗；
    2. **Language Settings**：左侧展示 `GlobeOutline` 图标及文案，右侧展示 `@vicons/ionicons4` 的 `IosArrowForward` 箭头指示符；
    3. **分割线**：1px 细线分隔系统功能项与合规反馈项；
    4. **Send feedback**：点击后若配置 `feedbackRoute` 则执行路由跳转，并派发 `feedback-click` 事件；
    5. **Terms of service**：点击后路由跳转至 `termsRoute`（默认 `/accounts/terms`），派发 `terms-click` 事件；
    6. **Privacy policy**：点击后路由跳转至 `privacyRoute`（默认 `/accounts/privacy`），派发 `privacy-click` 事件。

### 2.2 Language Settings 二级悬浮子菜单实现
- 在 Language Settings 选项上嵌套 Naive UI 的 `<n-popover>`：
  - 触发机制：`trigger="hover"`；
  - 对齐方式：`placement="right-start"`，平滑承接鼠标横向移动；
  - 容器承载：内部使用 `<VCard>`（宽度 150px）渲染语言列表；
- 语言列表选项：
  - 简体中文（`zh-CN`）；
  - 繁體中文（`zh-TW`）；
  - English（`en-US`）；
- 交互流：
  - 通过 `appStore.locale` 实时判定当前活跃语言，并展示 `CheckmarkOutline` 对勾状态图标与高亮文字色；
  - 选中语言后，调用 `appStore.switchLanguage` 异步加载当前路由对应的多语言词条资源并持久化，同时关闭二级菜单与上层设置卡片。

### 2.3 `VSideBar.vue` 侧边栏停靠区弹出层重构
- 状态协同与互斥：
  - 新增 `isSettingPopoverShow`（展开态）与 `isCollapsedSettingPopoverShow`（收起态工具箱内）响应式状态；
  - 在已有的 `isNoticePopoverShow`、`isCollapsedNoticePopoverShow`、`isUserCardPopoverShow` 之间建立联动互斥监听（任意卡片打开时立即关闭其余卡片）；
  - 在侧边栏折叠与展开状态发生切换（`toggleCollapse`、`expand`、`collapse`、`handleUpdateCollapsed`）时统一重置关闭所有弹出层；
- 展开态 Bottom Dock：
  - 设置按钮接入 `<n-popover v-model:show="isSettingPopoverShow" trigger="click" placement="top">`；
  - 提供 `#setting-card` 插槽，默认插槽内容实例化 `VSettingCard`，并绑定关闭回调与事件转发；
- 收起态 Bottom Dock（Apps 工具箱内）：
  - 嵌套设置按钮接入 `<n-popover v-model:show="isCollapsedSettingPopoverShow" trigger="click" placement="right-end">`；
  - 提供同步的 `#setting-card` 插槽支持；
- 统一方法暴露：
  - 新增 `closeSettingPopover()` 方法，并在 `defineExpose` 中向外导出 `isSettingPopoverShow`、`isCollapsedSettingPopoverShow` 与 `closeSettingPopover`。

### 2.4 响应式外层容器 `SideBar.vue` 适配
- 透传 `:feedback-route` 属性；
- 转发 `#setting-card` 插槽；
- 转发 `@setting-card-open`、`@setting-card-close` 与 `@setting-item-click` 事件。

### 2.5 类型定义、模块导出与多语言资源对齐
- `src/components/sidebar/types.ts`：
  - 增加 `VSettingCardProps` 接口；
  - 为 `VSideBarProps` 补充可选属性 `feedbackRoute?: string`；
- `src/components/sidebar/index.ts`：
  - 显式导出 `VSettingCard`；
- 多语言资源（`src/locales/zh-CN/common.json`、`en-US/common.json`、`zh-TW/common.json`）：
  - 补齐 `user_settings`、`language_settings` 与 `send_feedback` 词条。

---

## 3. 规范检查与纯粹代码铁律 (Zero Comments Rule)
- 所有修改及新增的代码文件（`VSettingCard.vue`、`VSideBar.vue`、`SideBar.vue`、`types.ts`、`index.ts`）严格遵守零注释铁律，无任何多余行内注释或临时说明；
- 组件与样式结构高内聚且自解释，全部遵循 Google Material 3 灰阶与纯色规范。

---

## 4. 验证结果
- **类型检查**：执行 `npx vue-tsc --noEmit`，结果代码 0，无任何类型缺失或属性未对齐问题；
- **全量构建打包**：执行 `npm run build`（Vite 生产构建），成功输出所有静态 Chunk，校验通过。
