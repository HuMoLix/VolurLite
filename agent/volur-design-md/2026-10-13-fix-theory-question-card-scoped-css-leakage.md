# 修复 TheoryQuestionCard 样式污染导致的暗夜模式双层背景与边框异常

## 1. 变更背景与问题溯源

在暗夜模式下，全局通用组件 `VSearch.vue`、`VAvatar.vue` 以及 `VInput.vue` 在视觉审查时均出现了异常的双层背景（半透明暗红背景叠在正常背景之上）与双层 Border，并且审查元素时发现命中了一条带有最高权重的全局样式：
```css
border-color: #f28b82 !important;
background-color: rgba(242, 139, 130, 0.12) !important;
color: #f28b82 !important;
```

经全项目样式审计与 AST 编译推导，定位到该样式的物理来源为 `src/components/competition/theory/TheoryQuestionCard.vue` 中的 `<style scoped lang="scss">` 样式块：
```scss
:deep(.theory-tf-btn) {
  &.is-false-selected {
    border-color: #ea4335 !important;
    background-color: rgba(234, 67, 53, 0.08) !important;
    color: #ea4335 !important;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);

    html.dark &,
    :global(.dark) & {
      border-color: #f28b82 !important;
      background-color: rgba(242, 139, 130, 0.12) !important;
      color: #f28b82 !important;
    }
  }
}
```

### 为什么 Scoped 样式会污染全站？
1. **Sass 展开行为**：Sass 展开 `:global(.dark) &` 后得到选择器 `:global(.dark) :deep(.theory-tf-btn).is-false-selected`。
2. **Vue SFC Scoped CSS 编译器（`@vue/compiler-sfc`）行为缺陷**：Vue 编译 Scoped CSS 时，当在复合选择器前置使用 `:global(...)` 并在其后追加后代选择器或 `:deep()` 时，解析器会直接截断后置选择器，错误输出为单独的 `.dark` 全局类选择器：
   ```css
   html.dark[data-v-card] .theory-tf-btn.is-false-selected, .dark {
     border-color: #f28b82 !important;
     background-color: rgba(242, 139, 130, 0.12) !important;
     color: #f28b82 !important;
   }
   ```
3. **全局命中影响**：
   - 规则末尾生成的逗号选择器 `, .dark` 是全局选择器，且带有 `!important`；
   - `VAvatar.vue` 根节点在暗黑模式下包含 `dark` 类；
   - `VInput.vue`（以及包装它的 `VSearch.vue`）根节点在暗黑模式下挂载了 `dark` 类；
   - 导致上述组件的外层容器全部被强行注入了半透明红色背景与红色 Border，与内层结构产生叠加，形成两层背景与两层 Border 的故障。

---

## 2. 改动点与修复方案

修改文件：`src/components/competition/theory/TheoryQuestionCard.vue`

1. **废除错误的伪类嵌套**：彻底移除 `:deep` 内部混用 `html.dark &, :global(.dark) &` 的反模式语法。
2. **使用安全的单体 `:global(...)` 作用域规避编译器截断**：
   将包含完整作用域父级路径的选择器整体包裹在 `:global()` 中，编译后直接生成目标父子关系选择器，不泄露任何单独的 `.dark`：
   - `:global(html.dark .theory-question-card .theory-option-btn.is-selected)`
   - `:global(html.dark .theory-question-card .theory-tf-btn.is-true-selected)`
   - `:global(html.dark .theory-question-card .theory-tf-btn.is-false-selected)`
3. **消除代码内冗余注释**：严格遵守文档外置原则与纯粹代码铁律，样式块内保持绝对自解释与纯净。

---

## 3. 影响范围与验证结果

- **影响文件**：
  - `src/components/competition/theory/TheoryQuestionCard.vue`
- **验证结果**：
  - Vue SFC Scoped CSS 编译产物中不再产生任何裸露的 `.dark` 选择器；
  - 暗黑模式下，`VSearch.vue`、`VAvatar.vue`、`VInput.vue` 彻底摆脱非法红底与双层 Border 污染，恢复为预期设计的单层正常样式；
  - `TheoryQuestionCard.vue` 中的单选、判断题选中状态在亮色与暗色模式下仍能精准应用对应 Google Red/Blue/Green 配色。
