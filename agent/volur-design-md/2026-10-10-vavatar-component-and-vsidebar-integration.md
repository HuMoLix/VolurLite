# VAvatar 头像框组件独立封装与 VSideBar 侧边栏对接记录

本文档记录了 Volur Lite NEXT 前端工程（`src/volur-lite-view-NEXT`）中通用头像框组件 `VAvatar.vue` 的设计实现，以及将其接入侧边栏 `VSideBar.vue` / `SideBar.vue` 底部 Dock 个人信息区域、实现 Google 风格头像 ring 边框、多行 Tooltip 详情提示与 `#565b61` 12px 字体规范的施工记录。

---

## 1. 变更背景与需求目标

在 Volur Lite NEXT 侧边栏设计演进中，原侧边栏 `VSideBar.vue` 底部的个人信息卡片头像渲染代码直接平铺在模板与 SCSS 样式中，缺乏复用性，且包含与整体极简设计不符的在线状态绿点（`.online-status-dot`）。

为提升组件化内聚性并提供高度灵活的头像样式控制能力：
1. **独立组件封装**：如同 `VSearch.vue` 般将头像框抽象为独立的业务通用组件 `VAvatar.vue`，存放于 `src/components/sidebar/`，并在统一导出入口 `index.ts` 中对外导出；
2. **多形态圆角参数支持与默认圆形**：支持参数灵活切换方形头像框（`radius = 8px`）与圆形头像框（`radius = 64px`）；侧边栏默认采用 `radius = 64px` 的圆形头像框；
3. **Google 风格头像 ring 边框**：参考 Google Workspace / Google Account 的头像环形设计，通过伪元素 `::after` 叠加 1.5px 细致外环（日间 `#dadce0`，悬浮高亮 Google Blue `#1a73e8`；暗色 `#3c4043`，悬浮高亮 `#8ab4f8`），确保在不同底色与图片边缘下具有高辨识度与极简美感；
4. **全态个人信息 Tooltip**：无论侧边栏处于展开态还是折叠态，鼠标悬浮至底部 Dock 个人信息栏均触发气泡提示（折叠靠右 `placement="right"`，展开靠上 `placement="top"`），登录态下遵循严格的多行展示格式：
   ```
   Volur 账号：DisplayName
   （Username）
   （Email）
   ```
5. **个人信息文字排版与颜色对齐**：底部 Dock 个人信息栏文字颜色统一设置为 `#565b61`，字号统一为 `12px`；
6. **彻底移除在线绿点**：从 `VSideBar.vue` 的模板结构与亮色/暗色 SCSS 样式规则中全面清理 `.online-status-dot` 相关元素及属性定义；
7. **多态优雅降级**：头像支持优先加载用户头像图片（`src`，监听 `@error` 自动降级）；无图片时降级至首字母文字徽章（`text` / 首字母）；未登录访客态无缝呈现访客轮廓图标（`<PersonOutline />`）；
8. **对齐 Google Material 色彩体系**：严格对齐亮色 Google Blue/Gray 与暗色 Google Workspace 炭灰调色盘，纯粹代码零注释。

---

## 2. 核心架构与功能设计

### 2.1 组件接口规范 (`VAvatarProps`)

在 `src/components/sidebar/types.ts` 中新增强类型接口声明：
```ts
export interface VAvatarProps {
  src?: string
  alt?: string
  text?: string
  size?: number | string
  shape?: 'square' | 'circle'
  round?: boolean
  radius?: number | string
  ring?: boolean
  ringColor?: string
  theme?: 'light' | 'dark'
}
```

并在 `VSideBarProps` 与 `SideBarProps` 中补充透传字段：
```ts
avatarShape?: 'square' | 'circle'
avatarRadius?: number | string
avatarRing?: boolean
```

### 2.2 半径计算与形状状态机

`VAvatar.vue` 内部实现确定性圆角推导逻辑：
- 若显式传递 `radius` 属性（支持数值 `8` / `64` 或字符串 `'8px'` / `'64px'`），优先采用该指定值；
- 若声明 `shape === 'circle'` 或 `round === true`，推导为圆形半径 `64px`；
- 默认或声明 `shape === 'square'` 时，推导为方形圆角 `8px`；
- 容器应用 `overflow: hidden` 与 `borderRadius: computedRadius`，内部子元素（`<img>`、回退文字徽章、访客图标）继承圆角修剪，确保边缘平滑无溢出。

### 2.3 Google 风格 Ring 边框设计

- 采用内部绝对定位的 `::after` 遮罩环：
  ```scss
  &.has-ring {
    &::after {
      content: '';
      position: absolute;
      inset: 0;
      border-radius: inherit;
      border: 1.5px solid var(--v-avatar-ring-color, #dadce0);
      box-sizing: border-box;
      pointer-events: none;
      transition: border-color 0.2s cubic-bezier(0.4, 0, 0.2, 1);
    }
    &:hover::after {
      border-color: var(--v-avatar-ring-color, #1a73e8);
    }
  }
  ```
- 悬浮联动：当用户鼠标悬浮在底部 Dock `.user-card` 卡片时，通过 `:deep(.v-avatar.has-ring::after)` 同步激活 Google Blue（亮色 `#1a73e8`，暗色 `#8ab4f8`）高亮边框；
- 解决容器裁剪与层级遮挡问题：由于 `::after` 位于内部顶层并不超出容器宽高，完全避免了受父级 `overflow: hidden` 截断的问题，亦不会被内部图片覆盖。

### 2.4 全态个人信息 Tooltip 格式化

- 移除原先的 `:disabled="!isCollapsed"` 限制，展开与折叠态均保持可交互提示；
- 布局自适应：动态响应 `:placement="isCollapsed ? 'right' : 'top'"`；
- 结构化格式：
  ```ts
  const userTooltipText = computed<string>(() => {
    if (!isLoggedIn.value) {
      return t('common.signin')
    }
    const displayName = currentUser.value?.displayName || currentUser.value?.username || 'Volur User'
    const lines: string[] = [`Volur 账号：${displayName}`]
    if (currentUser.value?.username) {
      lines.push(`（${currentUser.value.username}）`)
    }
    if (currentUser.value?.email) {
      lines.push(`（${currentUser.value.email}）`)
    }
    return lines.join('\n')
  })
  ```
- 样式通过 `white-space: pre-line; line-height: 1.45; font-size: 12px;` 保证换行规整。

### 2.5 个人信息栏文字颜色与字号规格

- `.user-info-text`、`.user-name` 与 `.user-desc` 统一设定为：
  - `font-size: 12px;`
  - `color: #565b61;`
  - `line-height: 1.3;`
- 深色模式下优雅适配为柔和文本灰 `#8c8c8c`（hover 时 `#d4d4d4`）。

### 2.6 折叠态 AppsSharp 悬浮小工具 Popover 架构与样式体系

- **极简折叠底栏 (Dual-Item Dock)**：当侧边栏折叠收起时（`isCollapsed === true`），Bottom Dock 底部区域彻底精简，仅垂直排列两大核心图元：
  1. 上方：Google 风格九宫格应用与工具箱图标按钮（`AppsSharp`，38×38px，圆角 12px）；
  2. 下方：高辨识度圆环头像（`VAvatar`，26px）。
- **右侧垂直悬浮挂载 (Vertical Flex Flyout Popover)**：
  - 基于 Naive UI `NPopover` 配置 `trigger="hover"`、`placement="right"`、`raw` 与 `:show-arrow="false"`；
  - 内部浮层（`.dock-apps-popover`）采用 `display: flex; flex-direction: column; align-items: center; gap: 6px; padding: 8px 6px;` 实现纯净的**从上而下垂直流式排列**；
  - 垂直小工具列表依次收敛：
    1. 搜索（Search）；
    2. 通知（Notifications）；
    3. 设置（Settings）；
    4. 亮暗主题切换（Theme Toggle）；
  - 用户悬浮或激活 `AppsSharp` 时保持内敛 hover 反馈；点击小工具时自动无缝关闭浮层并触发对应交互；
  - 折叠态下自动隐藏冗余的底部辅助按钮（`sidebar-bottom-action`），严格保证底栏空间的极简与纯粹。
- **列表背景与卡片系统 (List Card Container)**：
  - 由于 `NPopover` 默认通过 Teleport 挂载至 `body` 根节点，浮层样式统一放置在非作用域 `<style lang="scss">` 块中；
  - **日间模式**：背景色统一为 `#fafafa`，边框采用 `#d7d8da80`，圆角 16px，柔和阴影 `box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08), 0 1px 3px rgba(0, 0, 0, 0.04)`；
  - **暗夜模式**：背景色统一为 `#191919`，边框采用 `#262626`，深色阴影 `box-shadow: 0 4px 16px rgba(0, 0, 0, 0.4), 0 1px 3px rgba(0, 0, 0, 0.3)`。
- **小工具按钮与原装 Hover 色彩回滚 (Tool Button System & Hover Color Rollback)**：
  - 按钮类名独立定义为 `.dock-tool-btn`，与底栏的 38×38px 规格 1:1 对齐（`width: 38px; height: 38px; border-radius: 12px;`）；
  - **严格回滚并保持原装 Hover 颜色规范**：
    - **日间模式**：默认 `color: #3c4043; border: 1px solid #d7d8da80;`，hover 与 active 时回滚为 `color: #202124; background-color: #f4f5f5;`（边框颜色保持不变）；
    - **暗夜模式**：默认 `color: #8c8c8c; border: 1px solid #262626;`，hover 与 active 时回滚为 `color: #d4d4d4; background-color: #323232;`（边框颜色保持不变）；
    - 同步将折叠底栏触发按钮 `.action-box-collapsed .action-icon-btn` 的 hover 激活态同步回滚为上述原装配色。

---

## 3. 文件变更清单

| 文件路径 | 变更类型 | 说明 |
| :--- | :--- | :--- |
| `src/components/sidebar/VAvatar.vue` | 新增 | 头像框基础组件，支持方形 (8px) / 圆形 (64px) 圆角状态机、Google 环形 ring 边框、图片加载错误降级、文字首字母徽章与访客图标 |
| `src/components/sidebar/types.ts` | 修改 | 新增 `VAvatarProps`（含 `ring` / `ringColor`），在 `VSideBarProps` 与 `SideBarProps` 中增加 `avatarShape`、`avatarRadius` 与 `avatarRing` |
| `src/components/sidebar/index.ts` | 修改 | 统一向外导出 `VAvatar` 组件 |
| `src/components/sidebar/VSideBar.vue` | 修改 | 默认启用圆形头像与 ring 边框；全态 Tooltip 格式化展示账号详情；文本颜色更新为 `#565b61, 12px`；折叠态收敛为 AppsSharp + 头像，hover 时右侧展开小工具浮层；彻底清理历史绿点 |
| `src/components/sidebar/SideBar.vue` | 修改 | 在响应式外壳中透传 `avatarShape`、`avatarRadius` 与 `avatarRing` 属性至 `VSideBar` |

---

## 4. 验证与构建测试

执行前端构建检查：
```bash
npm run build
```
执行结果：
- `vue-tsc --noEmit` 静态类型检查 100% 通过；
- Vite 生产构建顺利打包输出，无任何语法错误或类型断言失败；
- 严格践行 GEMINI.md 零注释铁律与 Google Material 设计规范。
