# 优化 VAvatar 与 VInput 组件的主题 Class 绑定

## 1. 变更背景

在对暗夜模式样式审查时发现：
1. `VAvatar.vue` 的根节点渲染出的 class 包含重复的 `dark dark`（如 `class="v-avatar is-circle dark dark flex-shrink-0"`）；
2. 原因是模板中对主题类进行了双重注入：数组项包含 `currentTheme`（暗夜模式时为 `'dark'`），紧接着的对象中又包含 `{ dark: currentTheme === 'dark' }`，导致 Vue 合并 class 时输出了重复的 `dark` 类名；
3. `VInput.vue` 的根节点直接输出了裸露的 `effectiveTheme`（即 `'dark'` / `'light'`），同时输出了 `{ 'theme-dark': ... }`，向外层挂载裸露的 `dark` 存在潜在的全局样式冲突隐患。

---

## 2. 改动点

1. **`src/components/sidebar/VAvatar.vue`**：
   - 精简模板 `:class` 绑定，移除重复的 `{ dark: currentTheme === 'dark' }`，仅保留单一 `currentTheme` 响应式字符串，彻底消除 `dark dark` 重复问题。
2. **`src/components/common/VInput.vue`**：
   - 精简模板 `:class` 绑定，移除裸露的 `effectiveTheme` 类，收敛为统一的 `{ 'theme-dark': effectiveTheme === 'dark' }` 命名空间类名，与内部 SCSS 样式选择器契合且避免全局污染。

---

## 3. 影响范围与验证结果

- **影响文件**：
  - `src/components/sidebar/VAvatar.vue`
  - `src/components/common/VInput.vue`
- **验证结果**：
  - `VAvatar` 根节点在暗黑模式下渲染出干净的单一 `dark` 类名，杜绝重复输出；
  - `VInput` 根节点在暗黑模式下渲染出规范的 `theme-dark` 类名；
  - `npm run build` 构建顺利通过。
