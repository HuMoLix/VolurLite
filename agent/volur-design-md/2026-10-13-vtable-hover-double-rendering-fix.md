# VTable 列表项双重 Hover 背景渲染修复方案文档

## 1. 缺陷现象与背景

在 [`VWriteupModal.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/competition/ctf/VWriteupModal.vue) 等页面使用 [`VTable.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/common/VTable.vue) 渲染数据表格时，当鼠标悬浮（hover）在表格行数据项上时，背景色会出现两次过渡阶跃（即视觉上闪烁两次或呈现双层不同深浅的背景渲染叠加），破坏了平滑交互体验。

---

## 2. 根因技术排查

经过深入分析 Naive UI 底层样式体系及 `VTable.vue` 的实现机制，定位到三大核心诱因：

1. **双轨样式配置冲突与色值不一致（Double Configuration）**：
   - 轨一（Naive UI 主题变量）：在 `tableThemeOverrides` 中定义了 `tdColorHover: isDark ? 'rgba(255, 255, 255, 0.06)' : 'rgba(0, 0, 0, 0.04)'`；
   - 轨二（Scoped SCSS 穿透）：在组件内部又写了 `.n-data-table-tr:hover .n-data-table-td`，其颜色值为 `rgba(0, 0, 0, 0.03)`（暗色下为 `rgba(255, 255, 255, 0.04)`）；
   - 两轨声明的色值和透明度互不一致，导致同一单元格受两套不同规则的牵引。
2. **纯 CSS 伪类与 Naive UI JS 驱动类名的时序竞争（Two-phase Transition）**：
   - **阶段一（CSS 瞬发）**：鼠标光标进入行时，浏览器立即命中 Scoped SCSS 的 `.n-data-table-tr:hover .n-data-table-td`，背景开始向 `0.03` 过渡；
   - **阶段二（JS 响应）**：数毫秒后，Naive UI 的内部鼠标事件响应并在 DOM 节点上追加 `.n-data-table-td--hover` 类名，强行将背景色指向 `var(--n-merged-td-color-hover)`（即 `0.04`）；
   - 在过渡动画过程中发生色值重定向，形成视觉上的二次跳变。
3. **Naive UI 内部 `tr` 与 `td` 双层半透明叠加（Double Layering）**：
   - Naive UI 底层样式同时给 `.n-data-table-tr:hover` 和 `.n-data-table-tr:hover > .n-data-table-td` 应用了 `tdColorHover`；
   - 当使用半透明 RGBA 色值时，`tr` 的背景与 `td` 的背景叠加产生双倍不透明度，在光标滑动与单元格重绘过程中产生叠加跳跃。

---

## 3. 修复方案与代码实现

坚持**单一真实数据源（Single Source of Truth）**与**透明度归一化**原则：

### 3.1 废除 SCSS 手写伪类，统一由 themeOverrides 驱动
- 彻底删除 `VTable.vue` 中所有 `.n-data-table-tr:hover .n-data-table-td` 的手写样式规则；
- 在 `tableThemeOverrides` 中按边框模式与主题进行统一收敛：
  ```ts
  tdColorHover: isBordered
    ? (isDark ? '#303134' : '#f1f3f4')
    : (isDark ? 'rgba(255, 255, 255, 0.04)' : 'rgba(0, 0, 0, 0.03)'),
  ```

### 3.2 消除行级叠加背景
- 在 `.v-table` 的通用穿透层中，显式将行元素背景重置为透明：
  ```scss
  :deep(.n-data-table) {
    .n-data-table-tr {
      background-color: transparent !important;
    }
  }
  ```
- 确保有且仅有 `td` 单层渲染 Hover 背景色，杜绝半透明图层双重叠加。

---

## 4. 验证与审计结论

1. **类型检查**：
   - 执行 `npx vue-tsc --noEmit`，验证通过（0 errors）；
2. **纯粹代码与零注释铁律**：
   - `VTable.vue` 内部无任何冗余注释，相关设计思路与技术选型完整记录于本文档中；
3. **效果验证**：
   - 鼠标滑入表格行时，背景色单次平滑淡入（0.3s cubic-bezier），移出时平滑淡出，彻底消除了二次闪烁和叠加跳跃现象。
