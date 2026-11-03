# SideBar 与 VSideBar 侧边栏设计令牌与 light-dark 现代化重构记录

## 1. 变更背景与动因

在 `volur-lite-view-NEXT` 工程中，平台主侧边栏导航组件由 [`SideBar.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view-NEXT/src/components/sidebar/SideBar.vue)（响应式外层容器）与核心实现组件 [`VSideBar.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view-NEXT/src/components/sidebar/VSideBar.vue) 协同构成。

原实现中：
1. 侧边栏主体背景硬编码为 `#fafafa`，边框硬编码为 `#d7d8da80`，分组标题颜色硬编码为 `#5f6368`；
2. 在 `html.dark` 历史选择器中，分别使用硬编码的 `#191919`、`#262626` 和 `#9aa0a6` 强行覆盖暗色模式。

为了全面对齐系统全局 W3C 原生 CSS `light-dark()` 机制与设计令牌单一真实数据源（SSOT）规范，需要对侧边栏的背景色、右边框色及导航分组标题文本色升级为标准化设计令牌，并彻底删除相关的冗余 `.dark` 选择器样式覆盖。

---

## 2. 变更内容与改动点

### 2.1 响应式外壳组件优化 ([`SideBar.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view-NEXT/src/components/sidebar/SideBar.vue))

- 在 `.sidebar-desktop-view` 桌面固定定位容器上注入基准设计令牌：
  ```scss
  background-color: var(--color-v1-surface-left-nav);
  ```
  保证桌面端骨架在组件挂载首屏即获得准确的背景底色。

### 2.2 核心导航组件改造 ([`VSideBar.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view-NEXT/src/components/sidebar/VSideBar.vue))

1. **底色与边框令牌化**：
   - 将 `.v-sidebar` 的背景色声明由 `#fafafa` 更新为：
     ```scss
     background-color: var(--color-v1-surface-left-nav) !important;
     ```
   - 将右边框由 `border-right: none` 更新为标准边框设计令牌：
     ```scss
     border-right: 1px solid var(--color-v1-outline-var) !important;
     ```
   - 同步将 `:deep(.n-layout-sider__border)` 的背景色由 `#d7d8da80` 更新为：
     ```scss
     background-color: var(--color-v1-outline-var) !important;
     ```
2. **分组标题文本色令牌化**：
   - 将 `.sidebar-group-title` 的颜色由 `#5f6368` 更新为系统标准化禁用/次要文字设计令牌：
     ```scss
     color: var(--color-v1-text-disable);
     ```
3. **彻底清除冗余 `.dark` 覆盖规则**：
   - 在 `html.dark` 块中删除 `.v-sidebar { background-color: #191919 !important; }`；
   - 在 `html.dark` 块中删除 `:deep(.n-layout-sider__border) { background-color: #262626 !important; }`；
   - 在 `html.dark` 块中删除 `.sidebar-nav-group .sidebar-group-title { color: #9aa0a6; }`。

---

## 3. 设计令牌映射与设计规范验证

在 [`vcss.scss`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view-NEXT/src/styles/vcss.scss) 中，本次迁移涉及的设计令牌定义如下：

- `--color-v1-surface-left-nav: light-dark(#fafafa, #191919);`
- `--color-v1-outline-var: light-dark(#d7d8da80, #262626);`
- `--color-v1-text-disable: light-dark(#85898e, #575757);`

在浏览器原生 `color-scheme` 驱动下：
- **亮色模式**：自动求值为 `#fafafa` 底色、`#d7d8da80` 边框、`#85898e` 分组标题；
- **暗色模式**：自动求值为 `#191919` 底色、`#262626` 边框、`#575757` 分组标题。

---

## 4. 验证与审计

1. **类型检查与构建验证**：
   - 执行 `vue-tsc --noEmit`：0 错误，严格类型校验全部通过。
2. **零注释铁律与文档外置**：
   - 代码文件中保持纯粹代码，所有改动背景与推导全部外置记录于本文档中。
