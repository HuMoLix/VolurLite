# 侧边栏 JSON 配置驱动与路由自派发重构记录

## 1. 变更背景与架构痛点分析

在原有的侧边栏与布局设计中，侧边栏列表项的渲染和路由处理存在以下核心痛点：
1. **展示与路由断层**：桌面端 `VSideBar.vue` 与移动端 `VBottomBar.vue` 强耦合 Naive UI 原生 `MenuOption` 数据类型，列表项仅通过 `@select` 向上层冒泡选中事件，组件自身不具备路由自驱动与跳转能力；
2. **硬编码路由拦截**：路由跳转被侵入式外置在最外层 `AppLayout.vue` 中，通过硬编码匹配 `if (key === 'competitions')` 和 `if (key === 'announcements')` 执行跳转。当上层传递自定义列表项时，默认路由行为直接失效；
3. **高亮失步**：当前激活项（`currentValue`）在 `AppLayout.vue` 中硬编码判断路径前缀，无法根据数据项的路由元数据实现自适应匹配；
4. **无法纯 JSON 配置**：Naive UI 的 `MenuOption` 要求直接传递虚拟节点渲染函数（`label: () => VNodeChild`、`icon: () => VNodeChild`），无法直接通过声明式 JSON 或纯配置数组进行数据驱动。

---

## 2. 核心架构设计与重构方案

### 2.1 规范化 JSON 配置模型 (`SidebarNavItem`)
在 `src/components/sidebar/types.ts` 中升级 `SidebarNavItem` 声明，原生支持声明式路由、i18n 语言键与图标名称：
- `key`: 唯一标识符；
- `route`: 目标路由路径（如 `/competitions`、`/announcements`）；
- `titleKey`: 响应式多语言翻译 Key（如 `common.competitions`）；
- `label`: 静态回退标签；
- `icon`: 支持组件对象、图标标识字符串或渲染函数；
- `children`: 递归支持多级嵌套导航。

并在 `VSideBarProps`、`SideBarProps`、`VBottomBarProps` 与 `AppLayoutProps` 中同步支持 `items?: SidebarNavItem[]`。

### 2.2 响应式解析管道 (`transformItemsToOptions`)
在 `SideBar.vue` 内部实现配置管道转换：
- **图标解析 (`resolveItemIcon`)**：内置 `iconRegistry` 图标映射池（`TrophyOutline`、`MegaphoneOutline` 等），自动兼容传入的组件引用或字符串键名，统一转换成标准 Naive UI 图标 VNode；
- **国际化集成**：集成 `vue-i18n` 的 `t()`，优先消费 `titleKey` 并响应式跟踪语言切换；
- **元数据绑定**：转换后的每一个 `MenuOption` 均挂载 `route` 属性，并自动构建双向 `keyRouteMap`。

### 2.3 路由自驱动与自适应高亮
- **自驱动跳转 (`handleSelect`)**：用户在桌面端或移动端点击任意列表项时，组件优先从 `item.route` 或 `keyRouteMap` 中检索目标路由，当目标路由与当前路由不一致时自动执行 `router.push(targetRoute)`，外部无需再手写判断；
- **双向激活态同步 (`currentValue`)**：根据 `route.path` 自动对齐匹配最长公共路由前缀，并针对 `/c/` 等短链别名进行智能归一化，支持浏览器前进、后退、刷新与直接访问时的精准激活高亮。

### 2.4 上层布局解耦 (`AppLayout.vue`)
- 移除 `AppLayout.vue` 中侵入式的 `if (key === 'competitions')` 硬编码逻辑；
- `AppLayout.vue` 仅负责透传 `items` 并遵循非受控/受控属性原则，将菜单状态机完整委托给 `SideBar.vue`。

---

## 3. 受影响文件清单

- `src/components/sidebar/types.ts`
- `src/components/sidebar/SideBar.vue`
- `src/components/sidebar/VSideBar.vue`
- `src/components/sidebar/VBottomBar.vue`
- `src/components/layouts/AppLayout.vue`

---

## 4. 验证与审计

- 执行 `npm run build`（`vue-tsc --noEmit && vite build`），全工程 TypeScript 静态类型检查 100% 通过，构建无任何警告与错误。
- 遵循零注释铁律与文档外置原则，所有设计推导与技术细节均收敛于本记录文档。
