# VSideBar 底部坞用户信息弹窗 Card 重构设计与实现

## 1. 变更背景与需求说明

在当前 [VSideBar.vue](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/sidebar/VSideBar.vue) 实现中，底部坞（Bottom Dock）的用户信息栏（`.sidebar-user-box`）在用户点击时直接执行路由跳转（已登录跳转至用户个人中心 `/user/profile`，未登录跳转至 `/v1/signin/identifier`），并且仅支持悬停通过 `n-tooltip` 显示简单的纯文本提示。

本次需求对该交互与呈现进行彻底升级与重构，并进一步针对视觉色彩、按钮排版与条款分隔细节进行优化：
1. **取消直接页面跳转，改为弹窗 Card**：参考收缩模式下的展开工具栏（`n-popover`），当用户点击 Bottom Dock 下的用户信息栏时，不在当前视口内直接进行页面跳转，而是在用户信息栏上方弹出一个卡片（Card）。
2. **Card 三层 Flex 布局体系**：
   - **上层**：居中展示 Volur Logo，最右侧放置关闭按钮。
   - **中层**：第一行展示用户头像与用户名（及邮箱/状态信息），下方放置操作按钮组（切换账号、退出账号、个人信息）。
   - **下层**：展示“隐私权政策”与“服务条款”法规指引链接。
3. **视觉与排版微调需求**：
   - **背景色彩**：日间模式卡片背景色设定为 `#FCFCFC`，夜间模式设定为 `#191919`；
   - **操作按钮排列**：下方的 `card-action-buttons` 改为垂直列式（column）排列，移除按钮内部 Icon 图标，保持紧凑纯净；
   - **条款层边框与点标字号**：彻底移除下层 `card-bottom-layer` 的顶部边框线（`border-top` / `border-top-color`），将法规分隔点标 `card-legal-dot` 的字号调整为微观 `5px`。

---

## 2. 核心架构与机理设计

### 2.1 触发器与弹出机理 (Popover Trigger)

参考收缩模式下展开工具坞的设计原理，使用 Naive UI `<n-popover>` 包裹用户栏 DOM 结构，配置为 `trigger="click"` 并绑定双向状态 `isUserCardPopoverShow`：
- **无外层直角投影穿透 (Raw Popover & Shadow Reset)**：沿用 `raw` 属性与 `:show-arrow="false"`，同时在全局非 scoped 样式块中声明 `.n-popover:has(.dock-user-popover-card) { box-shadow: none !important; background-color: transparent !important; }`，彻底杜绝 Naive UI 默认直角阴影与背景穿透；
- **自适应方位定位 (Placement Strategy)**：在展开态下卡片水平居中悬浮于用户信息栏上方（`placement="top"`）；在折叠收缩态下（侧边栏宽度仅 74px），采用 `placement="top-start"` 确保弹出的 280px 卡片左对齐于触发器，杜绝超出左侧视口边界。
- **触发反馈态 (Active State Feedback)**：在卡片展开期间，用户栏卡片自动附加 `.is-active` 样式类，提供与悬停一致的高亮背景反馈。

### 2.2 三层 Flex 布局体系结构

卡片容器 `.dock-user-popover-card` 采用纯粹的 Flex 垂直排版（`display: flex; flex-direction: column; gap: 12px;`），宽度设定为 280px，圆角为 16px：
- **背景色**：亮色模式为 `#FCFCFC`，暗色模式为 `#191919`。

1. **上层（Top Layer - `.card-top-layer`）**：
   - 采用 `display: flex; align-items: center; justify-content: space-between;`；
   - 左侧放置 28px 隐形占位块 `.card-top-spacer`，右侧放置 28px 悬停圆形关闭按钮 `.card-close-btn`；
   - 中间通过 `flex: 1; display: flex; justify-content: center;` 挂载 Volur 品牌 Logo，实现数学意义上的绝对水平居中。
2. **中层（Middle Layer - `.card-middle-layer`）**：
   - **第一行（`.card-user-info-row`）**：水平 Flex 排版，左侧挂载 `<VAvatar>`（36px 尺寸，带动态主题光环），右侧通过 `.card-user-meta` 展示加粗用户名与次级灰色邮箱/状态文本，文字溢出时使用省略号平滑截断；
   - **下方按钮组（`.card-action-buttons`）**：采用垂直列式排版（`display: flex; flex-direction: column; gap: 8px; width: 100%;`），无图标纯文本：
     - **切换账号 (`card-btn--switch`)**：宽度 100%，点击关闭卡片并引导至账号选择器路由 `/v1/signin/accountchooser`；
     - **退出账号 (`card-btn--logout`)**：宽度 100%，触发异步 `logoutApi()` 请求并清理 `logged_in` Cookie，平滑重定向至登录入口 `/v1/signin/identifier`；在悬停态下呈现 Google Red 警示高亮；
     - **个人信息 (`card-btn--profile`)**：宽度 100%，点击关闭卡片并路由跳转至个人中心 `/user/profile`。
3. **下层（Bottom Layer - `.card-bottom-layer`）**：
   - 彻底移除了顶部边框线（移除 `border-top` 与暗色下的 `border-top-color`）；
   - 水平居中展示“隐私权政策”与“服务条款”文本链接，中间由微观点标 `.card-legal-dot`（`font-size: 5px;`）优雅隔离，悬停时触发 Google 品牌蓝高亮过渡。

---

## 3. 修改文件与实施细节

### 3.1 核心组件实现与属性扩展
1. **[src/components/sidebar/VSideBar.vue](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/sidebar/VSideBar.vue)**：
   - 引入 `@vicons/ionicons5` 图标：`CloseOutline`（移除了按钮内不再需要的图标引用，确保无未使用依赖）；
   - 引入 Cookie 工具 `removeCookie` 与认证接口 `logoutApi`；
   - 新增属性 `accountChooserRoute`、`privacyRoute`、`termsRoute` 及其默认值；
   - 新增事件声明：`switch-account`、`logout`、`profile-click`、`user-card-open`、`user-card-close`；
   - 模板将原本的 `n-tooltip` 重构为 `<n-popover v-model:show="isUserCardPopoverShow" trigger="click" raw>`，并在插槽中渲染三层 Flex 结构的 `.dock-user-popover-card`；
   - 卡片背景设为 `#FCFCFC`（亮色）与 `#191919`（暗色）；
   - `card-action-buttons` 切换为 flex 列式排版（`flex-direction: column`），移除内部 `<n-icon>`；
   - `card-bottom-layer` 移除顶部 border 线，`card-legal-dot` 设置字号为 5px；
   - 重构 `handleUserClick` 仅触发点击事件与弹窗状态控制，不再执行路由跳转；
   - 在侧边栏折叠与展开动作触发时同步关闭卡片，消除动画位移违和；
   - 对外通过 `defineExpose` 暴露 `isUserCardPopoverShow` 与 `closeUserCardPopover` 方法。
2. **[src/components/sidebar/SideBar.vue](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/sidebar/SideBar.vue)**：
   - 同步透传 `accountChooserRoute`、`privacyRoute`、`termsRoute` 属性与相关动作事件；
   - 开放 `#user-card` 具名插槽，支持外部定制。
3. **[src/components/sidebar/types.ts](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/sidebar/types.ts)**：
   - 在 `VSideBarProps` 接口定义中扩展相关路由属性字段。

### 3.2 国际化语言包对齐
在 `zh-CN/common.json`、`en-US/common.json`、`zh-TW/common.json` 中统一补充多语言翻译键：
- `switch_account`：“切换账号” / “Switch account” / “切換帳號”
- `signout`：“退出账号” / “Sign out” / “退出帳號”
- `profile_info`：“个人信息” / “Personal info” / “個人資訊”
- `privacy_policy`：“隐私权政策” / “Privacy policy” / “隱私權政策”
- `terms_of_service`：“服务条款” / “Terms of service” / “服務條款”

---

## 4. 验证与构建测试

- **构建与类型检查**：执行 `npm run build`（`vue-tsc --noEmit && vite build`），全工程 14,833 个模块全部编译通过，无任何 TypeScript 语法或类型告警。
- **纯粹代码审查**：严格遵守代码零注释铁律，未在任何 `.vue`、`.ts` 或 `.json` 代码文件中编写行内解释性注释。
