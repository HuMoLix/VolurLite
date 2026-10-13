# VTable 组件边框属性与透明背景重构报告

## 1. 变更背景与需求概述

根据用户指令与最新 UI 设计决策，对通用列表组件 [`VTable.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/common/VTable.vue) 进行如下调整：
1. **替换 `clz` 属性为强类型布尔值 `:bordered="true" / "false"`**：废除字符串 `clz="bordered"` / `clz="borderless"` 变体，改用更加符合 Vue 与 Naive UI 标准的布尔属性 `bordered?: boolean`（默认值为 `true`）；
2. **新增背景颜色配置参数，默认为透明**：提供 `backgroundColor?: string` 参数，默认取值 `'transparent'`，使表格能自然融入各类父级容器卡片与背景底色，并支持自定义颜色值。

同时，根据 GEMINI.md 第 3 条“重构彻底性铁律”，同步更新调用处（如 [`VWriteupModal.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/competition/ctf/VWriteupModal.vue)）并清理废弃类型（`VTableClz`）。

---

## 2. 核心改动点

### 2.1 类型定义调整 (`types.ts`)
- 移除已废弃的联合类型 `VTableClz`；
- 在 `VTableProps` 接口中：
  - 移除 `clz?: VTableClz`；
  - 显式声明 `bordered?: boolean`；
  - 显式声明 `backgroundColor?: string`。

### 2.2 VTable 组件实现调整 (`VTable.vue`)
- **属性默认值**：`bordered: true`，`backgroundColor: 'transparent'`；
- **动态类名计算**：根容器动态类名从 `v-table-${clz}` 重构为 `bordered ? 'v-table-bordered' : 'v-table-borderless'`，既保留了成熟的阶梯样式体系，又契合布尔属性；
- **透明背景与 CSS 变量**：
  - 声明 `resolvedBgColor = computed(() => props.backgroundColor || 'transparent')`；
  - 注入 CSS 变量 `--v-table-bg: resolvedBgColor.value`；
  - 联动 Naive UI `tableThemeOverrides` 中的 `tdColor: resolvedBgColor.value`，并让 `n-data-table`、`n-data-table-wrapper`、`n-data-table-table` 基础底色全部为 `transparent`，由 `--v-table-bg` 与外层容器统一度量。

### 2.3 业务组件调用同步 (`VWriteupModal.vue`)
- 将 Write Up 提交弹窗中的 `<VTable clz="bordered" ...>` 全链路同步调整为 `<VTable :bordered="true" ...>`。

---

## 3. 文件变更清单

| 文件路径 | 变更类型 | 核心变更说明 |
| :--- | :--- | :--- |
| `src/components/common/VTable.vue` | 修改 | 将 `clz` 重构为 `bordered: boolean`，添加 `backgroundColor` 属性并默认 `'transparent'` |
| `src/components/common/types.ts` | 修改 | 移除 `VTableClz`，更新 `VTableProps` 类型定义 |
| `src/components/competition/ctf/VWriteupModal.vue` | 修改 | 同步将表格调用更新为 `:bordered="true"` |
| `agent/volur-design-md/2026-10-13-vtable-bordered-prop-and-transparent-background-refactor.md` | 新建 | 记录施工与重构文档 |

---

## 4. 验证与审计

1. **类型校验 (`vue-tsc --noEmit`)**：执行通过，0 错误；
2. **生产构建 (`npm run build`)**：Vite 构建打包成功；
3. **零注释铁律**：全部代码文件严格保持纯净零注释。
