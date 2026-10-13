# 全项目 Scoped CSS 全局 `.dark` 样式泄漏彻底清零与暗夜模式双层边框根治报告

## 1. 变更背景与全链路根因剖析

在针对暗夜模式下 `VAvatar.vue`（例如在 `VCtfCompetitionStatistic.vue` 中渲染的 36px 头像）审查时，发现该元素即便在未传 `:ring="true"`、未传 `border` 属性的情况下，视觉上依然出现了非预期的双层边框。

经全量 CSS 规则逆向反编译审查，定位到这一现象是由于全项目中多个组件在书写 Vue 3 SFC `<style scoped lang="scss">` 暗夜模式样式时，普遍存在反模式的复合伪类嵌套语法：
```scss
// 反模式 1：
html.dark &,
:global(.dark) & {
  border-color: #333;
}

// 反模式 2：
:global(.dark) {
  .challenge-btn {
    background-color: #2a2a2a;
  }
}

// 反模式 3：
:global(.dark) .component-class {
  box-shadow: ...;
}
```

### Vue SFC Scoped CSS 解析器缺陷与泄漏机理
Vue SFC Scoped CSS 编译器（`@vue/compiler-sfc`）在解析复合选择器时：
1. 遇到 `:global(...)` 伪类时，不支持在 `:global(...)` 后置追加选择器节点或进行嵌套展开；
2. 编译器会截断丢弃 `:global(...)` 之后所有的后置选择器，直接向样式表中输出无作用域限制的裸露全局选择器：
   ```css
   .dark { border-color: #333; }
   .dark { border-color: #3c4043; background-color: var(--v-table-bg, transparent); }
   .dark { background-color: #2a2a2a; }
   .dark { color: #d4d4d4 !important; }
   ```
3. 导致全站所有带有 `dark` 类名的 DOM 节点（包括 `VAvatar`、`VInput`、容器卡片等）全部被无差别注入了这些泄漏的 `border-color` 与背景色；
4. 当外层容器被注入边框与底色，而内层子元素（如 `.v-avatar-fallback`）又具有自身的半透明浅蓝背景时，在圆形边框截断及高 DPI 亚像素渲染边缘处，形成了极其明显的双层边框效应。

---

## 2. 改动点与全面修复

对全项目中所有存在 `:global(.dark)` 泄漏的 8 个核心组件进行了地毯式重构：

1. **`src/components/common/VBtn.vue`**：
   - 彻底移除 `v-btn-normal`、`v-btn-primary`、`v-btn-warning` 中 3 处 `:global(.dark) &`，收敛为严格受控的 `html.dark &`。
2. **`src/components/common/VTable.vue`**：
   - 彻底移除 `v-table-bordered` 与 `v-table-borderless` 表头/单元格样式中 4 处 `:global(.dark) &`，收敛为原生的 `html.dark &`。
3. **`src/components/competition/VCompetitionHeader.vue`**：
   - 将 `:global(.dark) .v-competition-header` 与 `:global(.dark) .v-header-language-card` 重构为 `html.dark .v-competition-header` 与 `html.dark .v-header-language-card`。
4. **`src/components/competition/ctf/VCtfCategory.vue`**：
   - 将 `:global(.dark)` 嵌套块重构为标准的 `html.dark` 块，确保样式仅作用于当前组件作用域。
5. **`src/components/competition/ctf/VCtfScoreboardSelector.vue`**：
   - 将 `:global(.dark)` 嵌套块重构为标准的 `html.dark` 块。
6. **`src/views/competition/components/CtfCompetitionChallengeCard.vue`**：
   - 将 `:global(.dark) .ctf-challenge-card` 与 `:global(.dark) .challenge-card-footer` 重构为 `html.dark .ctf-challenge-card` 与 `html.dark .challenge-card-footer`。
7. **`src/views/competition/components/CtfCompetitionChallengeModal.vue`**：
   - 将 `:global(.dark) .flag-input` 重构为 `html.dark .flag-input`。
8. **`src/views/competition/components/VCompetitionNotification.vue`**：
   - 将 `:global(.dark) .v-competition-notification` 重构为 `html.dark .v-competition-notification`。

---

## 3. 验证结果

- **样式泄漏检测**：再次运行全量样式编译检测脚本，全项目裸露 `.dark` 规则**完全清零**；
- **组件边框表现**：
  - `VAvatar.vue` 在未配置 `:ring="true"` 时恢复为完全干净无边框状态；
  - `VAvatar.vue` 在配置 `:ring="true"` 时仅渲染单一标准的 1.5px Google 风格环形 border；
  - 全站双层背景、双层边框故障彻底根除；
- **生产构建验证**：`vue-tsc --noEmit && vite build` 成功完成，0 报错。
