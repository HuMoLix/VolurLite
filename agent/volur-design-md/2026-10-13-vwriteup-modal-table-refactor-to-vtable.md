# VWriteupModal 提交结果列表重构为 VTable 报告

## 1. 变更背景与需求概述

在 `volur-lite-view-NEXT` 平台的 CTF 竞赛 Write Up 提交弹窗（[`VWriteupModal.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/competition/ctf/VWriteupModal.vue)）中，历史提交记录原本采用手写的 Grid 表格布局进行展示。该实现缺乏统一的表格交互封装、表格分页/排序/加载能力的统一抽象，且样式与平台正在推进的标准 UI 组件库存在割裂。

现根据规范，将 `VWriteupModal.vue` 中的提交记录表格全面迁移替换为最新封装的通用列表组件 [`VTable.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/common/VTable.vue)。

---

## 2. 核心改动点

1. **引入与注册 `VTable`**：
   - 从 `@/components/common` 集中引入 `VTable`；
   - 从 `naive-ui` 引入强类型 `DataTableColumns`。

2. **声明结构化列配置 (`columns`)**：
   - **序号 (`index`)**：宽度 64px，居中对齐，使用 `font-mono` 等宽字体渲染；
   - **提交人 (`username`)**：宽度 120px，支持气泡截断展示；
   - **提交时间 (`createdAt`)**：宽度 160px，使用 `formatDateTime` 格式化，并采用统一次级描述文本色彩；
   - **上传 WriteUp 名称 (`fileName`)**：保持原有的 `VTooltip` 悬浮完整文件名提示与 PDF 文档外链/纯文本高亮。

3. **重构模板表格结构**：
   - 彻底移除原有的手写 `grid grid-cols-12` 结构；
   - 使用 `<VTable clz="bordered" size="small" :radius="12" :columns="columns" :data="submissions" :loading="loadingSubmissions" :row-key="(row) => row.id" :max-height="160">` 代替；
   - 嵌入 `#empty` 插槽保持统一的空数据“暂无提交记录”提示；
   - 设定 `:radius="12"` 与外部 `rounded-xl` 卡片圆角保持一致。

---

## 3. 文件变更清单

| 文件路径 | 变更类型 | 核心变更说明 |
| :--- | :--- | :--- |
| `src/components/competition/ctf/VWriteupModal.vue` | 修改 | 将提交结果表格重构为 `VTable`，声明 `DataTableColumns` 列定义 |
| `agent/volur-design-md/2026-10-13-vwriteup-modal-table-refactor-to-vtable.md` | 新建 | 记录施工方案、变更背景及审计溯源信息 |

---

## 4. 验证与审计

1. **零注释铁律**：重构后的代码保持极致纯净，无任何解释性注释。
2. **静态类型检查 (`vue-tsc --noEmit`)**：类型校验通过，列配置与数据模型完全契合 `CompetitionWriteupVO`。
3. **构建测试 (`npm run build`)**：打包编译成功，产物正常生成。
