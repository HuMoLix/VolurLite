# VSideBar 导航容器滚动条布局与挤压防护方案设计文档

## 1. 变更背景与问题分析

在 `AdminLayout` 等具有多层级复杂菜单的视图中，二级/多级菜单项（如“题库管理”、“集群容器”、“风控管理”等）展开后，内部列表高度超出容器可用可视区，浏览器触发垂直溢出并动态生成纵向滚动条。

原实现中存在以下问题：
1. **浏览器原生盒模型物理挤压**：`.sidebar-nav-container` 声明了 `overflow-y: auto`，但在未溢出时不占据物理槽位。一旦二级菜单展开，浏览器将滚动条物理宽度（4px~8px）直接从可视客户区（`clientWidth`）中扣除，使得子元素 `.v-menu` 与 `.n-menu-item`（`width: 100%`）计算宽度骤缩，导致列表项按钮产生明显的横向压缩与布局抖动（Layout Shift）。
2. **负边距补偿假象失效**：原有样式试图通过 `width: calc(100% + 8px); margin-right: -8px; padding-right: 8px;` 将滚动条贴向父容器内边距外边缘，但该内边距是常驻静态属性，滚动条生成时依然在 `padding-box` 右边界渲染并扣减内容空间，无法消除动态差值。
3. **折叠/展开对齐问题**：若在外部包装器中调整全局内边距，会导致顶部 Logo、主要操作按钮、用户资料卡片与导航菜单产生视觉参考线偏差。

## 2. 核心设计与改造方案

基于 W3C CSS Overflow Module Level 4 标准规范，采用标准属性 `scrollbar-gutter: stable` 彻底替代负外边距 hack：

### 2.1 常规展开态重构
1. **统一容器宽度与边距**：
   - 将 `width: calc(100% + 8px)` 重构为 `width: 100%`；
   - 移除 `margin-right: -8px` 与 `padding-right: 8px`，重置为 `margin-right: 0` 与 `padding-right: 0`。
2. **声明 `scrollbar-gutter: stable`**：
   - 在 `.sidebar-nav-container` 上配置 `scrollbar-gutter: stable`；
   - 浏览器在未溢出（未展开二级菜单）时即在右侧固定预留滚动槽位；当二级菜单展开出现滚动条时，滚动条直接在预留槽位中绘制，内容区物理宽度恒定不变，列表项按钮零挤压、零抖动。

### 2.2 折叠态适配
在 `.v-sidebar.is-collapsed .sidebar-nav-container` 下显式覆盖 `scrollbar-gutter: auto`，确保折叠态下 38px 图标按钮维持绝对居中，不受非展开槽位干扰。

## 3. 影响范围与验证

- **影响文件**：
  - `src/volur-lite-view-NEXT/src/components/sidebar/VSideBar.vue`
- **验证结果**：
  - `npm run build` 成功完成，类型检查与资源打包全部通过；
  - 常规展开状态下，多级菜单展开折叠时菜单项按钮宽度无任何挤压与跳动；
  - 折叠状态下侧边栏按钮居中正常。
