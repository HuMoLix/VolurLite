# VolurMarkdownEditor 工具栏“编辑/预览”垂直对齐修复与聚焦态环形阴影移除

## 1. 变更背景

在对平台级组件 [`VolurMarkdownEditor.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/components/VolurMarkdownEditor.vue) 升级后，用户反馈两处视觉与排版问题：
1. **聚焦态环形阴影过于突兀**：当输入框聚焦时，外层容器存在 `box-shadow: 0 0 0 1px #000000` 环形阴影，用户要求彻底移除；
2. **“编辑 / 预览”按钮偏上未与工具栏完全对齐**：工具栏左侧的分段 Tab（“编辑 / 预览”）相较于工具栏垂直中心线及右侧格式化按钮、提示文字在视觉上偏高，未处于工具栏的真正水平中心轴。

---

## 2. “编辑/预览”按钮偏上根因深度分析

经过对 Naive UI `NTabs (type="segment", size="small")` 组件渲染结构与 CSS 盒模型的分析，发现偏上问题是由以下**三层原因叠加**造成：

1. **工具栏高度与内边距在 1px 边框下的不对称分布**：
   - 工具栏 `.editor-toolbar` 先前声明了 `min-height: 44px; padding: 6px 14px;`；
   - 在底部分隔线 `border-bottom: 1px solid var(--v-border-base)` 的 `border-box` 约束下，内部实际可用高度为 43px；上下同时设置 `6px` 的 padding 时，未指定高度的 flex 容器内元素因微小的边框扣减无法做到纯粹的绝对数学居中，整体视觉上浮。
2. **Naive UI `NTabs` 内部层级行高（line-height）基线漂移**：
   - Naive UI 内部组件结构为：`.n-tabs` -> `.n-tabs-nav` -> `.n-tabs-rail` -> `.n-tabs-tab-wrapper` -> `.n-tabs-tab` -> `.n-tabs-tab__label` -> 自定义 `.tab-label`；
   - Naive UI 的 `.n-tabs-nav` 原生附带了 `line-height: 1.5`（14px * 1.5 = 21px）；
   - `.n-tabs-tab__label` 默认未设置 flex，属于行内元素（inline），其内部包含的 `inline-flex` 元素 `.tab-label` 在参与行内格式化上下文时，按其文字 baseline 对齐到父级行框基线，导致整体子元素文字与图标被迫向上抬升。
3. **分段滑轨（Rail）与 Tab 项缺乏显式垂直居中与固定高度**：
   - 先前仅覆盖了 `.n-tabs-rail { padding: 2px; }`，但未对其内部 `.n-tabs-tab`、`.n-tabs-tab-wrapper` 与 `.n-tabs-tab__label` 显式声明 flex 居中与高度约束，使得不同字体渲染下（中文字体 PingFang SC 等的光学 baseline）滑块内部内容垂直偏离。

---

## 3. 修复方案实施

### 3.1 彻底移除聚焦态环形阴影
在 `VolurMarkdownEditor.vue` 中移除所有聚焦相关的 `box-shadow`：
```scss
// 亮色模式与暗色模式下均完全移除环形阴影
&.is-focused {
  box-shadow: none !important;
}

html.dark &,
[data-theme='dark'] & {
  &.is-focused {
    box-shadow: none !important;
  }
}
```

### 3.2 工具栏与分段按钮绝对像素级垂直居中重构
1. **工具栏容器定高与零上下 Padding 方案**：
   - 将 `.editor-toolbar` 改造为 `height: 40px; min-height: 40px; padding: 0 14px;`；
   - 彻底避免上下 padding 计算导致的 1px 偏差，由父级 Flex 容器统一进行 `align-items: center` 居中分发。
2. **全链路补齐高度与 Flex 居中约束**：
   ```scss
   .toolbar-left {
     display: inline-flex;
     align-items: center;
     height: 100%;
     gap: 12px;

     .editor-mode-tabs {
       width: 155px;
       display: inline-flex;
       align-items: center;

       :deep(.n-tabs-nav) {
         line-height: 1;
         display: flex;
         align-items: center;
       }

       :deep(.n-tabs-rail) {
         padding: 2px;
         height: 28px;
         box-sizing: border-box;
         border-radius: 6px;
         display: flex;
         align-items: center;
       }

       :deep(.n-tabs-tab-wrapper) {
         height: 24px;
         display: flex;
         align-items: center;
         justify-content: center;
       }

       :deep(.n-tabs-tab) {
         height: 24px;
         padding: 0;
         display: flex;
         align-items: center;
         justify-content: center;
         border-radius: 4px;
       }

       :deep(.n-tabs-tab__label) {
         display: inline-flex;
         align-items: center;
         justify-content: center;
         line-height: 1;
         height: 100%;
       }

       .tab-label {
         display: inline-flex;
         align-items: center;
         justify-content: center;
         gap: 6px;
         font-size: 12px;
         line-height: 1;

         .n-icon {
           font-size: 14px;
           display: inline-flex;
           align-items: center;
           justify-content: center;
         }
       }
     }
   }
   ```
3. 经过此重构，胶囊轨道（28px 高度）、滑块选项（24px 高度）、胶囊背景指示器与左侧快捷工具栏按钮（26px 高度）及右侧提示文本严格对齐在同一条水平中心线上。

---

## 4. 影响范围与构建验证

- **涉及文件**：
  - `src/volur-lite-view/src/public/volurEx/components/VolurMarkdownEditor.vue`
- **构建测试**：
  - 执行 `npm run build`，5.45s 编译打包通过（Exit Code 0），无任何语法、TypeScript 或样式异常。
