# VButton 变体扩充与 AuthActionBar 迁移施工记录

## 1. 变更背景与动因

1. **废弃历史包袱 `buttons.scss` 推进**：
   在旧版认证流中，认证动作按钮依赖 Naive UI `<n-button>` 并通过全局 `buttons.scss` 中大量的 `!important` 规则强行修改样式（如 `.btn-next-step`、`.btn-create-account`）。这种方式破坏了 scoped 样式隔离，产生了硬编码 Hex 色值与伪双模覆写（`html.dark`），难以维护。
2. **统一设计系统单一真实源 (SSOT)**：
   将 `vcss.scss` 中残留的 `--color-v1-auth-primary` 统一升级迁移为全局标准设计令牌 `--color-v1-primary-highest`（对应文字色 `--color-v1-on-primary-highest`），彻底消除针对单一功能模块设立的特异性令牌别名。
3. **增强自研 `VButton` 原子组件能力**：
   为平台自研组件 `VButton` 增加 `primary-highest`、`secondary` 与 `secondary-lowest` 三种标准变体，使其在涵盖常规基础功能的同时，原生支持 Google Auth 认证流的高对比度实色主按钮与透明副按钮。

---

## 2. 变更内容与修改点

### 2.1 设计令牌统一 (`src/styles/vcss.scss`)
- 移除 `--color-v1-auth-primary`；
- 全局确立标准设计令牌：
  - `--color-v1-primary-highest: light-dark(#0b57d0, #a8c7fa);`
  - `--color-v1-on-primary-highest: light-dark(#fff, #062e6f);`

### 2.2 输入框组件设计令牌同步 (`src/volur-ui/components/input/VFieldInput.vue`)
- 将所有引用 `--color-v1-auth-primary` 的地方（聚焦边框、发光外阴影、输入光标 `caret-color`、浮动标签高亮色）同步切换为 `var(--color-v1-primary-highest)`。

### 2.3 `VButton` 变体扩展 (`types.ts` & `VButton.vue`)
- 在 `VButtonVariant` 类型中扩充 `'primary-highest' | 'secondary' | 'secondary-lowest'`；
- 在 `VButton.vue` 的 `<style scoped>` 中新增三种变体的现代化 CSS：
  - **`volur-btn--primary-highest`**：
    - 背景色采用 `var(--color-v1-primary-highest)`；
    - 文字色采用 `var(--color-v1-on-primary-highest)`；
    - 鼠标悬停采用 `light-dark(#0e4eb5, #b6d0f8)` 及 Google M3 阴影；
    - 点击激活态采用 `light-dark(#063980, #a8c7fa)` 及微缩放 `transform: scale(0.98)`；
    - 波纹光晕颜色绑定至 `var(--color-v1-primary-highest)`。
  - **`volur-btn--secondary-lowest`**：
    - 背景透明，文字采用 `var(--color-v1-primary-highest)`；
    - 悬浮与激活采用淡色微着色，禁用态保持纯透明底色。
  - **`volur-btn--secondary`**：
    - 带有 `--color-v1-outline` 边框的标准次级线框按钮。
- 同步将 `focus-visible` 外光环底色绑定至 `var(--color-v1-surface, #ffffff)`。

### 2.4 `VButton` 胶囊圆角层叠顺序修复与差异化内边距
- **`.is-pill` 层叠优先级修复**：原 `.is-pill`（`border-radius: 9999px`）声明于 size 类前面，导致相同权重下被后声明的 `.volur-btn--large`（`border-radius: 12px`）反向覆盖。现将 `.is-pill` 与 `.is-circle` 调整至所有尺寸类之后，确保无论选用何种尺寸，`:round="true"` 都能绝对优先呈现完美胶囊药丸圆角；
- **差异化 Padding 与最小宽度控制**：
  - 对 `primary` / `primary-highest` 主按钮设定 `padding: 0 24px; min-width: 88px;`，高保真还原 Google 实色主操作按钮的饱满视觉；
  - 对 `secondary` / `secondary-lowest` 次级与文本按钮设定 `padding: 0 12px;` 且不强加 88px 最小宽度，使文本副按钮自然紧凑、视觉舒适；
  - 保持圆形按钮独立适配（`&.is-circle { min-width: 44px; width: 44px; padding: 0; }`）。

### 2.5 `AuthActionBar.vue` 及认证视图深度纯化升级
- 按钮规格全面升级为 `size="large"`，完美呈现 Google 规范的 88px 最小宽度与大气比例；
- 彻底剔除 AntD 风格的扩散波纹动效（移除 `:is-press-wave="true"`），回归 Google 简洁克制的点击微缩放与阴影反馈；
- 移除多余的防御性三元判断，直接绑定 `is-scrim="true"`；
- 清理无用的外部补丁类名 `'auth-action-btn'`，仅透传语义行为类名（如 `btn-next-step`）；
- 同步将 `AccountChooserView.vue` 与 `PasskeyModal.vue` 中的动作按钮升级为大规格 `VButton`。

### 2.6 `buttons.scss` 彻底物理废除与非按钮样式迁移
- **非按钮辅助类收敛至 `index.scss`**：
  - 将原先残留于 `buttons.scss` 中的 4 类非普通按钮类名（`.auth-forgot-link`、`.auth-learn-more-link`、`.auth-resend-link`/`.auth-resend-btn`、`.auth-account-chip`）规范化并收敛至 `src/styles/index.scss`；
  - 色彩与边框全面绑定单一真实源设计令牌（`--color-v1-primary-highest`、`--color-v1-on-primary-highest`、`--color-v1-text`、`--v-border-base` 与现代化 `light-dark()` 函数），消除对 `.n-button` 的样式打补丁机制；
- **物理删除 `src/styles/buttons.scss`**：
  - 从 `index.scss` 根样式入口中彻底移除 `@use './buttons.scss';` 引用；
  - 物理删除 `buttons.scss` 文件，彻底消除了针对全局 `.n-button` 的样式覆盖历史包袱。

---

## 3. 验证与审计

1. **类型检查与编译验证**：
   - 执行 `vue-tsc --noEmit`：0 错误，严格类型全部通过；
   - 执行 `vite build`：31,718 个模块全部构建成功，产物打包无异常，无任何对 `buttons.scss` 的残留引用。
2. **代码规范性**：
   - 代码文件中保持零注释与文档外置原则；
   - 彻底消除了 `--color-v1-auth-primary` 与 `buttons.scss`，无兼容别名残留。
