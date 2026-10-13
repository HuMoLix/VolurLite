# VSideBar 与 VAvatar 登出状态头像残留修复及未登录跳转重构设计文档

## 1. 变更背景与问题溯源

在用户退出登录及未登录场景下，存在以下两个核心缺陷：
1. **退出登录后头像残留缺陷**：
   - 当用户在 `VUserCard.vue` 中点击“退出账号”后，底栏 Dock 与弹层中的 `VAvatar.vue` 仍然展示已注销用户的头像，未切换为未登录的访客默认占位图标（`PersonOutline`）。
   - 溯源分析发现：
     - 在 Pinia 状态层 `src/store/modules/user.ts` 中，`currentUser` 计算属性在 `user.value` 为空时，错误地兜底回退到了 `knownAccounts.value[0]`（存储在本地用于 Google 风格账号选择器的已知账号列表）。导致用户登出后，`userStore.currentUser` 仍持有原账号数据，进而导致 `userStore.avatarUrl`、`userStore.displayName`、`userStore.userEmail` 等字段未被清空。
     - 在 `VSideBar.vue` 中，底栏头像 `:src="avatarUrl"` 未进行登录状态（`isLoggedIn`）守卫校验，直接传递了非空头像链接。
     - 在 `VUserCard.vue` 中，`effectiveAvatarUrl`、`effectiveDisplayName`、`effectiveUserEmail` 等计算属性未在首层对 `effectiveIsLoggedIn` 进行快速短路阻断，导致退出后继承了外部传递的残留数据。
     - 在 `VAvatar.vue` 的渲染逻辑中，当 `src` 存在且无错误时直接渲染 `<img>` 标签，仅在 `src` 与 `text` 皆为空时才渲染访客默认图标。由于上述数据残留，导致始终渲染头像图片。
2. **未登录状态下点击个人信息栏未跳转登录页缺陷**：
   - 在 `VSideBar.vue` 中，底栏个人信息卡片（`.user-card`）由 `n-popover` 包装，其 `trigger="click"` 始终处于激活状态（未配置 `:disabled="!isLoggedIn"`）。
   - 当用户处于未登录状态时，点击底栏个人信息栏会直接触发弹出 `n-popover`（展示 `VUserCard` 登录占位与相关无意义操作），且 `handleUserClick` 处理函数未实现向登录页面（`/v1/signin/identifier`）的路由导航跳转。

---

## 2. 核心重构与改动明细

### 2.1 用户状态管理层修复 (`src/store/modules/user.ts`)
- **严格登录状态断言**：在 `currentUser` 计算属性首行注入 `if (!isLoggedIn.value) return null` 阻断判定，杜绝未登录或登出状态下向下渗透至 `knownAccounts` 历史列表。
- **全链路派生数据安全置空**：在 `displayName`、`userEmail`、`avatarUrl` 与 `userInitial` 计算属性中统一注入未登录判定。当 `!isLoggedIn.value` 时，分别返回 `''` 或 `undefined`，确保登出后状态树完全纯净。

### 2.2 平台侧边栏组件重构 (`src/components/sidebar/VSideBar.vue`)
- **底栏卡片浮层禁用约束**：在 `<n-popover>` 根节点增加 `:disabled="!isLoggedIn"` 约束。当用户未登录时，彻底阻断浮层弹出。
- **头像数据输入守卫**：将 `VAvatar` 传入属性由 `:src="avatarUrl"` 调整为 `:src="isLoggedIn ? avatarUrl : ''"`，同时对内部 `const avatarUrl` 增加 `isLoggedIn.value ? userStore.avatarUrl : undefined` 派生保护。
- **未登录路由跳转分发**：在 `handleUserClick` 函数中引入未登录判断。当 `!isLoggedIn.value` 时，自动调用 `router.push(props.signinRoute)`（默认导航至 `/v1/signin/identifier`）。
- **登出时浮层与气泡自动清理**：在 `watch(() => userStore.isLoggedIn)` 监听器中，当检测到登录态失效（`val === false`）时，强制同步关闭 `isUserCardPopoverShow.value = false` 与 `isUserTooltipShow.value = false`。

### 2.3 用户信息弹层卡片组件修复 (`src/components/sidebar/VUserCard.vue`)
- **计算属性首层阻断保护**：重构 `effectiveDisplayName`、`effectiveUserEmail`、`effectiveUserInitial` 及 `effectiveAvatarUrl` 计算属性，在最前置优先执行 `if (!effectiveIsLoggedIn.value) return ''` 短路判断，彻底切断登出后继承残留 Props 的路径。

---

## 3. 影响范围与回归测试验证

1. **影响模块范围**：
   - `src/store/modules/user.ts`
   - `src/components/sidebar/VSideBar.vue`
   - `src/components/sidebar/VUserCard.vue`
2. **构建与类型验证**：
   - 执行 `npm run build`，TypeScript 类型检查与 Vite 打包通过，零语法与类型报错。
3. **业务行为一致性保证**：
   - 用户登出后，底栏 Dock 头像即时切换为 Google 风格灰阶 `PersonOutline` 访客占位图标；
   - 未登录状态下点击底栏 Dock 个人信息卡片，不再弹出 `VUserCard` 浮层，而是直接平滑跳转至 `/v1/signin/identifier` 登录账号输入界面；
   - 登录状态下点击底栏卡片正常激活 `VUserCard` 浮层，多账号切换、退出登录等操作保持原有行为。
