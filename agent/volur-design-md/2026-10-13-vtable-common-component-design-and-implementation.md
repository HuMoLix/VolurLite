# VTable 通用列表组件设计与实现报告

## 1. 变更背景与需求概述

在 `volur-lite-view-NEXT`（新一代前端工程）中，统一 UI 组件库的设计语言与交互规范至关重要。本项目前端遵循 Google Material Design / Google Workspace 色彩体系与纯净无注释的工程铁律。

为满足赛事、后台与通用业务列表视图的数据展示需求，需要基于 Naive UI 的核心表格组件封装通用的 `VTable.vue`。该组件参考 `VBtn.vue` 的样式变体与类名架构（`clz` 机制），分为：
1. **`bordered`**：外框与行列结构分明、带有圆角裁切和统一 Google 灰色阶梯边框的有边框列表；
2. **`borderless`**：纯净无外边框、适合嵌入卡片或流式布局的平铺无边框列表。

---

## 2. 核心架构与设计方案

### 2.1 变体系统 (`clz`) 与尺寸规范 (`size`)

`VTable` 借鉴 `VBtn` 的类名规范，通过 `clz` 与 `size` 进行多维度属性解耦：
- **`clz` 变体**：
  - `bordered`：默认值。外层具有 `1px solid #e0e2e6`（暗色下为 `#3c4043`）包围框，并由 `--v-table-radius` 控制整体圆角与内容裁切；表头使用浅灰底色（亮色 `#f8f9fa`，暗色 `#292a2d`），行悬浮采用柔和高亮；
  - `borderless`：无外层包围框，表头及单元格使用微透明底色与细分隔线，适用于外层已有容器边框或流式嵌入场景。
- **`size` 尺寸**：
  - `small`：字体大小 13px，单元格间距 8px 12px；
  - `medium`：字体大小 14px，单元格间距 12px 16px（默认）；
  - `large`：字体大小 15px，单元格间距 16px 20px。
- **圆角与胶囊化**：
  - 支持 `round`（大圆角 16px）与 `radius`（自定义数值或百分比）响应式注入 CSS 变量 `--v-table-radius`。

### 2.2 Naive UI 数据表格原生功能封装与方法透传

`VTable` 以 Naive UI 的 `n-data-table` 为基础底座，全面兼容并透传核心数据表格特性：
1. **泛型支持**：`<script setup lang="ts" generic="T extends DataTableRowData = DataTableRowData">`，保证强类型推导；
2. **主题覆写 (`themeOverrides`)**：
   - 自动联动 Pinia `appStore.theme`（日间/夜间模式）；
   - 计算生成 Google Gray 阶梯的主题覆写对象，支持外部传入 `themeOverrides` 进行二次覆盖；
3. **状态与属性透传**：
   - `columns`、`data`、`loading`、`pagination`、`rowKey`、`singleLine`、`singleColumn`、`striped`、`scrollX`、`maxHeight`、`minHeight`、`virtualScroll`、`checkedRowKeys` 等；
4. **事件监听转发**：
   - `@update:page`、`@update:pageSize`、`@update:sorter`、`@update:filters`、`@update:checkedRowKeys`、`@update:expandedRowKeys`、`@scroll`；
5. **实例方法暴露 (`defineExpose`)**：
   - 向上暴露 `filter`、`filters`、`clearFilter`、`clearFilters`、`clearSorter`、`page`、`sort`、`scrollTo`、`downloadCsv` 及 `dataTableInst`。
6. **插槽体系**：
   - 头部扩展插槽：`header` / `toolbar`；
   - 底部扩展插槽：`footer`；
   - 表格内部插槽透传：`empty`、`loading`。

---

## 3. 文件变更清单

| 文件路径 | 变更类型 | 核心变更说明 |
| :--- | :--- | :--- |
| `src/components/common/VTable.vue` | 新建 | 基于 Naive UI 封装具备 `bordered` / `borderless` 样式的 `VTable` 核心组件 |
| `src/components/common/types.ts` | 修改 | 新增 `VTableClz`、`VTableSize`、`VTableProps` 及 `DataTableThemeOverrides` 类型定义 |
| `src/components/common/index.ts` | 修改 | 集中向外导出 `VTable` 组件与对应类型 |

---

## 4. 验证与审计

1. **静态类型检查 (`vue-tsc --noEmit`)**：执行通过，所有类型严格遵循 Naive UI `DataTableRowData` 约束，无任何类型推断报错。
2. **生产构建测试 (`npm run build`)**：Vite 全流程编译打包成功，产物分块正常。
3. **规范遵从**：
   - 代码文件无任何行内注释与解释性残留，遵循零注释铁律；
   - 色彩体系全面对齐 Google Workspace / Material Design 阶梯灰黑与 Google Blue；
   - 记录完整施工记录于 `agent/volur-design-md`。
