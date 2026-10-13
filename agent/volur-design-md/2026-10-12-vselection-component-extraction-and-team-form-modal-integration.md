# 通用下拉选择组件 VSelection 研发与 TeamFormModal 全量替换施工记录

## 1. 变更背景与目标

为持续统一 Volur-Lite-View-NEXT 前端通用表单组件库的设计规范，消除直接使用 Naive UI 原生 `<n-select>` 带来的视觉割裂感（原生默认聚焦蓝边框、默认阴影投影、未沉降的禁用背景与不一致的圆角）：
1. 参照 `VInput.vue` 的视觉规范与状态架构，研发通用的下拉选择组件 `src/components/common/VSelection.vue`；
2. 扩充 `types.ts` 定义 `VSelectionProps` 与 `SelectValue` 类型，并在通用组件库 `index.ts` 中集中导出；
3. 将战队信息创建/编辑弹窗 `TeamFormModal.vue` 中的所有下拉选择框（单位类型、队员性别、指导老师性别）全量替换为 `VSelection`。

---

## 2. 核心改动点

### 2.1 通用组件 VSelection.vue 研发
- **文件路径**：`src/components/common/VSelection.vue`；
- **组件结构**：基于 Naive UI `<n-select>` 进行二次封装与高内聚定制，透传常用属性（`options`、`placeholder`、`clearable`、`disabled`、`size`、`multiple`、`filterable`、`maxTagCount`、`consistentMenuWidth`、`placement`、`loading` 等）与常用插槽（`arrow`、`empty`、`action`、`header`）；
- **双向数据流支持**：
  - 支持 `v-model` 与 `v-model:value` 双向绑定；
  - `innerValue` 判定逻辑兼容 `string | number | (string | number)[] | null`，保障强类型安全；
- **自定义圆角 radius**：
  - 默认圆角 `8px`；
  - 支持 `round`（胶囊 `9999px`）与数字/字符串自定义传入；
  - 动态注入 `--v-selection-radius` CSS 自定义属性与主题覆盖层 `borderRadius`；
- **主题覆盖层 (selectThemeOverrides)**：
  - 配置 Naive UI peer 依赖覆盖 `InternalSelection`：
    - 边框色：亮色 `#d7d8da80`，暗夜 `#262626`；
    - 聚焦/激活边框色：亮色 `#b7babd`，暗夜 `#333`；
    - 禁用边框色：亮色 `#d7d8da80`，暗夜 `#262626`；
    - 文本色：亮色 `#1b1b1e`，暗夜 `#d4d4d4`；
    - 禁用文本色：亮色 `#80868b`，暗夜 `#73777d`；
    - 禁用背景底色：亮色 `#f5f6f7`，暗夜 `rgba(255, 255, 255, 0.04)`；
    - 箭头色：亮色 `#565b61`，暗夜 `#8c8c8c`；禁用箭头色：亮色 `#9aa0a6`，暗夜 `#55595d`；
    - 消除任何默认 boxShadow；
  - 配置 `InternalSelectMenu` 下拉浮层主题：
    - 圆角 `10px`；
    - 浮层背景：亮色 `#ffffff`，暗夜 `#292a2d`；
    - 选项文本与悬浮高亮色对齐 Google Blue 系列。
- **Scoped SCSS 深度定制**：
  - 对 `.n-base-selection`、`.n-base-selection-label`、`.n-base-selection__border`、`.n-base-selection__state-border` 进行严格的样式锁定；
  - 完善 `size="small"`（表格内紧凑视图，高度 28px、字号 13px）适配；
  - 隔离禁用状态选择器（`&:hover:not(.n-select--disabled)` 与 `&:focus-within:not(.n-select--disabled)`），彻底消除禁用状态下的悬浮/聚焦边框跳变。

### 2.2 通用组件库导出与类型定义
- **`src/components/common/types.ts`**：
  - 导出 `SelectValue` 类型；
  - 导出 `VSelectionProps` 接口；
- **`src/components/common/index.ts`**：
  - 统一导出 `VSelection`。

### 2.3 `TeamFormModal.vue` 模板全量替换
在 `src/views/competition/components/TeamFormModal.vue` 中完成 3 处替换：
1. **战队元数据表单**：单位类型选择框 (`formData.organizationType`) 替换为 `<VSelection>`；
2. **队员名单表格 (Members Table)**：队员性别选择框 (`row.gender`，`size="small"`) 替换为 `<VSelection>`；
3. **指导老师表格 (Leaders Table)**：指导老师性别选择框 (`row.gender`，`size="small"`) 替换为 `<VSelection>`；
4. **依赖清理**：移除 `naive-ui` 中的 `NSelect` 导入，统一从 `@/components/common` 引入 `VSelection`。

---

## 3. 规范审计与验证

1. **纯粹代码与零注释原则**：所有源码文件（`VSelection.vue`、`TeamFormModal.vue`、`types.ts`、`index.ts`）严格贯彻无行内解释性注释铁律；
2. **构建与类型验证**：执行 `npm run build`（`vue-tsc --noEmit && vite build`），全端 TypeScript 类型检查与生产打包一次性 0 错误通过；
3. **视觉与交互一致性**：`VSelection` 与 `VInput` 在圆角、边框、状态变换、字体与夜间模式表现上实现 100% 统一协同。
