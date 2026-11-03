# VSideBar 后台管理导航分组重构与强类型对齐设计记录

## 1. 变更背景与核心根因剖析

### 1.1 历史缺陷与根因
在重构 `src/volur-ui/components/sidebar/VSideBar.vue` 为零第三方 UI 依赖的自研纯组件后，后台管理页面（`AdminLayout.vue`）出现了侧边栏仍然显示前台内容（“赛事大厅”、“题目训练”等）的严重缺陷。

经全链路排查，根因如下：
1. **历史属性遗漏**：原有旧侧边栏依赖 Naive UI 的 `NMenu` 并接收 `:menu-options="adminMenuOptions"`。重构后的自研 `VSideBar.vue` 内部计算属性 `resolvedNavGroups` 仅处理了 `props.groups`、`props.menuGroups` 与 `props.items`，未处理 `props.menuOptions`；
2. **兜底回退前台默认菜单**：由于 `menuOptions` 未被解析，组件判定无外部传入菜单，直接执行了 `return defaultNavGroups` 分支，从而将前台写死的默认分组（EXPLORE 赛事大厅 / LEARN / MORE）渲染到了后台管理界面；
3. **架构违背单一真实源原则**：旧版实现混合使用了 Naive UI 的 `MenuOption`、`h(NIcon, ...)` 与函数式 `label`，未能与平台自研强类型 `SidebarNavGroup[]` / `SidebarNavItem[]` 全面对齐。

### 1.2 架构决策：拒绝旧兼容层，强制强类型规范化
根据平台“重构彻底性铁律”与纯粹性规范：
- **拒绝在 `VSideBar` 中追加 `menuOptions` 适配层**，坚决杜绝在自研组件库中残留废弃桥接或针对第三方组件的兼容包袱；
- **强制要求调用方传入标准化的 `SidebarNavGroup[]`**；
- 将后台管理功能按照平台架构规划，收敛归类为 **“主要”**、**“赛题”**、**“调度”** 与 **“安全”** 四大专属分组。

---

## 2. 详细改动清单与业务分组结构

### 2.1 后台导航结构标准化 (AdminLayout.vue)
将原有杂乱平铺且混杂 `MenuOption` / `NIcon` 的 `adminMenuOptions` 彻底重构为响应式的 `adminNavGroups: ComputedRef<SidebarNavGroup[]>`，结构划分如下：

1. **“主要”分组 (`key: 'main'`, `titleKey: 'admin.nav.main'`)**：
   - **工作台** (`dashboard`, `/admin/dashboard`, `SpeedometerOutline`)
   - **用户管理** (`users`, `/admin/users`, `PeopleOutline`)
   - **竞赛管理** (`competitions`, `/admin/competitions`, `TrophyOutline`)
   - **公告管理** (`broadcasts`, `/admin/broadcasts`, `MegaphoneOutline`)

2. **“赛题”分组 (`key: 'challenges'`, `titleKey: 'admin.nav.challenges'`)**：
   - **题库管理** (`challenges`, `LibraryOutline`)：
     - CTF 题库 (`challenge-ctf`, `/admin/challenges/ctf`, `FlagOutline`)
     - 理论题库 (`challenge-theory`, `/admin/challenges/theory`, `DocumentTextOutline`)
     - ISW 题库 (`challenge-isw`, `/admin/challenges/isw`, `TerminalOutline`)
     - AWD 题库 (`challenge-awd`, `/admin/challenges/awd`, `ShieldOutline`)
     - AWD+ 题库 (`challenge-awdp`, `/admin/challenges/awdp`, `ShieldCheckmarkOutline`)

3. **“调度”分组 (`key: 'dispatch'`, `titleKey: 'admin.nav.dispatch'`)**：
   - **集群设置** (`clusters`, `/admin/clusters`, `HardwareChipOutline`)
   - **容器管理** (`containers`, `/admin/containers`, `CubeOutline`)
   - **镜像管理** (`images`, `/admin/images`, `LayersOutline`)
   - **系统日志** (`logs`, `/admin/logs`, `DocumentTextOutline`)
   - **任务管理 / 任务调度** (`tasks`, `/admin/tasks`, `ListOutline`)
   - **邮件管理 / 邮件通知** (`emails`, `/admin/emails`, `MailOutline`)

4. **“安全”分组 (`key: 'security'`, `titleKey: 'admin.nav.security'`)**：
   - **风控管理** (`risk-group`, `ShieldCheckmarkOutline`)：
     - 风控设置 (`risk-settings`, `/admin/risk/settings`, `OptionsOutline`)
     - 策略设置 (`risk-rules`, `/admin/risk/rules`, `FlashOutline`)
     - IP 治理 (`risk-ip`, `/admin/risk/ip`, `GlobeOutline`)
   - **后台设置** (`system`, `/admin/system`, `SettingsOutline`)

### 2.2 彻底移除 Naive UI 依赖与类型解耦
- `AdminLayout.vue` 与 `AppLayout.vue` 彻底移除 `h`、`NIcon` 以及 `type { MenuOption } from 'naive-ui'` 的导入；
- 图标统一使用标准 Vue 矢量组件传递（如 `icon: SpeedometerOutline`），由 `<component :is="resolveItemIcon(item.icon)" />` 原生渲染为矢量 SVG；
- 选定事件 `handleSelect` 入参严格调整为强类型 `(key: string, item: SidebarNavItem)`。

### 2.3 `VSideBar.vue` 与 `types.ts` 纯粹化与体验优化
1. **移除无用属性**：彻底从 `VSideBarProps` 和默认 props 移除历史无用的 `menuOptions` 声明；
2. **多级子路由递归高亮**：
   在 `currentValue` 计算属性中增加 `matchNavKey(items, path)` 递归查找函数，即便在未显式传递 `:value` 绑定的场景下，也能自适应高亮匹配二级乃至更深层的路由项（如 `/admin/challenges/ctf/:id/edit`）；
3. **折叠态子菜单交互体验增强 (`SideBarNavGroup.vue`)**：
   通过 `SIDEBAR_INJECTION_KEY` 注入侧边栏上下文。当侧边栏处于收缩态（`collapsed`）时，若用户点击具有二级子菜单且无独立跳转路由的父级目录（如“题库管理”或“风控管理”），自动触发展开侧边栏并自动展开该父项手风琴，彻底消除折叠态下子项目录无法触达的操作死角。

### 2.4 多语言词条补齐
在国际化资源目录中新增后台分组专有键名：
- `src/locales/zh-CN/admin.json`: `"nav": { "main": "主要", "challenges": "赛题", "dispatch": "调度", "security": "安全" }`
- `src/locales/zh-TW/admin.json`: `"nav": { "main": "主要", "challenges": "賽題", "dispatch": "調度", "security": "安全" }`
- `src/locales/en-US/admin.json`: `"nav": { "main": "MAIN", "challenges": "CHALLENGES", "dispatch": "DISPATCH", "security": "SECURITY" }`

### 2.5 模块分块与循环依赖排查修复
在生产打包排查时，修正了 `src/volur-ui/components/sidebar/components/actions/UserCard.vue` 错误引用根级 `@/volur-ui` Barrel 导致的 Rollup 循环依赖警告，调整为从 `@/volur-ui/components/tooltip` 精确导入 `VTooltip`，使 Vite / Rollup 生产打包告警彻底归零。

---

## 3. 验证与审计结论

1. **类型校验**：执行 `vue-tsc --noEmit`，结果 `0 errors`，全局强类型安全验证通过；
2. **构建测试**：执行 `npm run build`，Vite 生产包顺利构建完成（耗时 ~12.6s），无循环依赖警告；
3. **前后台差异隔离**：
   - 前台（`AppLayout`）继续维持 EXPLORE / LEARN / MORE 的用户态大厅导航；
   - 后台（`AdminLayout`）严格按照“主要 / 赛题 / 调度 / 安全”四大职能分组呈现，子菜单展开折叠与路由激活状态完全正常；
4. **规范遵守**：严格执行“零注释铁律”，所有业务与架构设计沉淀于本文档。
