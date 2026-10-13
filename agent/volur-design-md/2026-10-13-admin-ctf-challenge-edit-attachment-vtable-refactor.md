# AdminCtfChallengeEditView 题目附件栏重构为 VTable 报告

## 1. 变更背景与需求概述

在 `volur-lite-view-NEXT` 平台的管理员 CTF 赛题编辑视图（[`AdminCtfChallengeEditView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/admin/challenge/AdminCtfChallengeEditView.vue)）中，原附件上传流程在选择文件后立即调用服务端接口上传，存在以下体验与交互缺陷：
1. 用户在新增或修改赛题附件时，若后续取消操作，已上传的附件会成为服务端孤儿文件；
2. 表格展示样式与平台标准 UI 组件规范不统一；
3. 缺少文件上传前本地计算与阶段性展示能力。

本次变更对附件上传交互进行深度重构：
1. 引入标准通用表格组件 [`VTable.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/common/VTable.vue) 用于展示附件；
2. 规范表头列排列与定义，依次为：**序号**、**附件名称**、**上传人**、**上传时间**、**文件大小**、**操作**；
3. **改造为延迟上传机制**：用户选择附件后，在本地立即提取文件名、计算文件尺寸、记录当前上传人与上传时间，并打上待上传标识填入表格；仅当用户点击“保存修改”或“创建题目”时，才统一发起文件上传网络请求，并在完成后提交赛题数据；
4. 将“上传题目附件”按钮移至列表下方右侧；
5. 删除原有“暂未上传任何题目附件”提示文本，由 `VTable` 内置的统一空数据状态接管。

---

## 2. 核心改动点

1. **实体与类型扩展**：
   - 在 [`CtfAttachmentVO`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/types/challenge.ts#L53-L61) 中扩展可选字段 `uploader?: string`、`uploadedAt?: string` 以及用于暂存本地文件的 `rawFile?: File`。

2. **声明附件表格列配置 (`attachmentColumns`)**（按顺序）：
   - **序号 (`index`)**：宽度 60px，使用等宽数字渲染行号 (`index + 1`)；
   - **附件名称 (`name`)**：最小宽度 160px，若存在已生成的 `url` 时渲染为外链高亮跳转（在新标签页打开）；若为待上传本地文件（`rawFile` 存在），则展示文件名并附带柔和黄色“待上传”标签；
   - **上传人 (`uploader`)**：宽度 120px，展示附件上传者（新建上传自动记录当前用户，历史附件支持作者回退）；
   - **上传时间 (`uploadedAt`)**：宽度 170px，通过 `formatDateTime` 格式化为 `YYYY-MM-DD HH:mm:ss`；
   - **文件大小 (`size`)**：宽度 110px，通过 `formatFileSize` 函数实现 B / KB / MB / GB 级别的平滑格式化；
   - **操作 (`actions`)**：宽度 90px，渲染红色危险操作按钮 `VBtn`（`clz="danger"`、`size="tiny"`），支持直接点击从本地列表移除（未上传的文件直接销毁本地暂存）。

3. **本地元数据计算与延迟上传流转**：
   - **选择文件 (`handleUploadAttachment`)**：支持一次性多选文件，同步读取文件对象属性，装配 `CtfAttachmentVO` 本地元数据并推入 `formData.attachments`，不发生任何网络请求；
   - **保存/创建动作 (`handleSave`)**：点击保存时，遍历附件列表识别带有 `rawFile` 的项，使用 `FormData` 逐一调用 `uploadAdminCtfAttachmentApi`，上传成功后回填真实 `url` 与服务端确认的 `size`，并清除 `rawFile` 引用；
   - **提交净荷清洗**：将附件数组映射剔除 `rawFile` 后序列化至最终请求体中，提交创建或更新接口。

4. **规范与原则遵循**：
   - 严格遵循**纯粹代码与零注释铁律**，代码中无任何冗余行内注释或解释性说明；
   - 移除 `AdminCompetitionEditView.vue` 中未引用的冗余导入；
   - 通过 `vue-tsc` 与生产打包全量静态类型与构建验证。

---

## 3. 文件变更清单

| 文件路径 | 变更类型 | 核心变更说明 |
| :--- | :--- | :--- |
| `src/types/challenge.ts` | 修改 | `CtfAttachmentVO` 实体添加可选字段 `uploader`、`uploadedAt` 与 `rawFile` |
| `src/views/admin/challenge/AdminCtfChallengeEditView.vue` | 修改 | 引入 `VTable` 与 6 列配置；改造为本地元数据计算与保存时延迟上传机制；上传按钮下移右对齐；删除无附件提示 |
| `src/views/admin/competition/AdminCompetitionEditView.vue` | 修改 | 移除未使用的 `defineAsyncComponent` 导入以符合严格 TypeScript 规则 |
| `agent/volur-design-md/2026-10-13-admin-ctf-challenge-edit-attachment-vtable-refactor.md` | 修改 | 记录施工与设计文档，便于后续审计与溯源 |

---

## 4. 验证与审计

1. **零注释检查**：源码中无任何解释性注释。
2. **静态类型检查 (`npx vue-tsc --noEmit`)**：运行退出码为 0，类型检查通过无报错。
3. **构建测试 (`npm run build`)**：打包编译成功，产物正常生成。
