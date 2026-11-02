# VGlobalStyle 全局 Body 注入设计令牌迁移记录

## 1. 变更背景与动因

在 `volur-lite-view-NEXT` 工程中，全局样式同步器组件 `VGlobalStyle`（以及聚合别名 `VolurGlobalStyle`）负责接管宿主 DOM 中 `document.body` 的基础背景与文字主色同步。原实现中，注入到 `body.style` 的 CSS 变量为 `var(--v-bg-base)` 与 `var(--v-text-primary)`。

为了全面对齐系统全局设计令牌单一真实数据源（SSOT）规范，将 `body` 底色与主文字色由原先的 `--v-bg-base` 与 `--v-text-primary` 统一升级迁移为全局标准化设计令牌 `--color-v1-surface` 与 `--color-v1-text`。

---

## 2. 变更内容与修改点

### 2.1 全局样式同步器更新 (`VGlobalStyle.vue` & `VolurGlobalStyle.vue`)
- 在 `VGlobalStyle.vue` 的 `watchEffect` 副作用中：
  - 将 `body.style.backgroundColor` 赋值从 `'var(--v-bg-base)'` 修改为 `'var(--color-v1-surface)'`；
  - 将 `body.style.color` 赋值从 `'var(--v-text-primary)'` 修改为 `'var(--color-v1-text)'`。
- 在 `VolurGlobalStyle.vue` 同步对齐相同修改。

### 2.2 基础全局样式对齐 (`src/styles/index.scss`)
- 在全局静态样式表 `index.scss` 的 `body` 规则块中，同步将 `background-color` 与 `color` 的默认声明从 `var(--v-bg-base)`、`var(--v-text-primary)` 更新为 `var(--color-v1-surface)` 与 `var(--color-v1-text)`，确保首屏静态渲染与运行时动态注入保持绝对一致。

---

## 3. 设计令牌映射与设计规范验证

在 `src/styles/vcss.scss` 中，设计令牌已使用原生 CSS `light-dark()` 进行了严格声明：
- `--color-v1-surface: light-dark(#fff, #191919);`
- `--color-v1-text: light-dark(#1b1b1e, #d4d4d4);`

当处于亮色模式时：
- `body.style.backgroundColor` 解析为 `#ffffff`；
- `body.style.color` 解析为 `#1b1b1e`。

当处于暗色模式时：
- `body.style.backgroundColor` 解析为 `#191919`；
- `body.style.color` 解析为 `#d4d4d4`。

配合 `transition: background-color 0.25s ease, color 0.25s ease`，深浅色切换过渡顺畅无闪烁。

---

## 4. 验证与审计

1. **类型检查与编译验证**：
   - 执行 `vue-tsc --noEmit`：0 错误，严格类型校验全部通过；
   - 执行 `vite build`：31,717 个模块全部构建成功，产物打包无异常。
2. **代码规范性**：
   - 代码文件中坚决贯彻零注释铁律与文档外置原则，代码结构高度自解释。
