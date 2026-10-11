# 提取 VUserCard 组件与移除 VNotice 底部边框色审计记录

## 1. 变更背景与需求
根据用户交互与组件化重构指令，本项目进行了以下两项关键演进：
1. **提取 `VUserCard.vue`**：将 `src/components/sidebar/VSideBar.vue` 内部原耦合在 Bottom Dock 中的用户详细信息卡片（`.dock-user-popover-card`）提取为独立、可复用的组件 `src/components/sidebar/VUserCard.vue`；
2. **移除 `VNotice.vue` 中的 `border-top-color`**：彻底移除 `src/components/sidebar/VNotice.vue` 底部栏（`.card-bottom-layer`）中暗黑模式下的 `border-top-color`，使底部分割与主题阶梯更加自然纯净。

---

## 2. 核心改动点

### 2.1 新增 `VUserCard.vue` 独立组件 (`src/components/sidebar/VUserCard.vue`)
- **基于 `VCard` 基底构建**：
  - 容器继承 `VCard`（`width: 280px`，`gap: 12px`，无多余边框与默认投影）；
  - 封装顶部图层（品牌 Logo 与圆形关闭按钮）、中部图层（用户头像 `VAvatar`、昵称/邮箱与三项账号操作按钮）以及底部图层（隐私政策与服务条款协议链接）。
- **数据与交互自闭环**：
  - 支持传入 `user`、`isLoggedIn`、`displayName`、`userEmail`、`avatarUrl` 等 Props；若未传入则自动通过 Pinia `userStore` 与 `appStore` 进行响应式推导兜底；
  - 内置账号切换、登出（调用 `logoutApi` 并清除 Cookie 与用户状态）、个人主页跳转与条款跳转逻辑，同时向上抛出事件（`@close`、`@switch-account`、`@logout`、`@profile-click`）。

### 2.2 类型与模块导出更新
- `src/components/sidebar/types.ts`：新增 `VUserCardProps` 接口；
- `src/components/sidebar/index.ts`：集中导出 `VUserCard` 组件。

### 2.3 精简 `VSideBar.vue`
- 在模板中用 `<VUserCard ... />` 替代原先长达百行的内联卡片节点；
- 移除不再需要的图标 `CloseOutline` 导入，以及内部不再被引用的 `removeCookie`、`logoutApi` 与事件处理函数；
- 移除样式中冗余的 `.dock-user-popover-card` SCSS 规则集（超过 250 行），并在 `.n-popover:has(...)` 选择器中添加 `.v-user-card` 支持。

### 2.4 清理 `VNotice.vue` 底部边框色
- 在 `src/components/sidebar/VNotice.vue` 暗黑模式样式中，移除 `.card-bottom-layer` 下的 `border-top-color: #3c4043;` 声明。

---

## 3. 验证与影响范围

1. **构建与类型验证**：
   - 执行 `vue-tsc --noEmit && vite build`，编译打包在 5.65 秒内顺利完成，无任何 TS 类型错误与无用变量警告（TS6133 已完全解决）。
2. **零注释铁律遵守**：
   - 新增的 `VUserCard.vue` 以及改动的 `VSideBar.vue`、`VNotice.vue` 均严格保持纯粹代码，无任何行内注释。
3. **功能一致性与影响范围**：
   - 侧边栏用户抽屉与底栏交互完全保持原有功能，用户卡片模块化程度显著提升；
   - 站内通知弹层底部样式按预期完成边框色剔除。
