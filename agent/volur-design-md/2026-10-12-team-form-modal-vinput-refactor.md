# 战队报名与编辑弹窗 TeamFormModal 全量输入框替换为 VInput 施工记录

## 1. 变更背景与目标

为推进 Volur-Lite-View-NEXT 前端通用组件库的统一与收敛，将战队信息创建/编辑弹窗 `TeamFormModal.vue` 中的所有输入框（包括战队名称、所属单位、队伍简介文本域、成员表格及指导老师表格中的各字段输入框）从 Naive UI 原生 `<n-input>` 全量替换为通用基础输入组件 `src/components/common/VInput.vue`。

---

## 2. 核心改动点

### 2.1 底层通用组件 `VInput.vue` 与类型扩充
- **类型定义更新** (`src/components/common/types.ts`)：
  - `VInputProps` 扩展支持 `autosize?: boolean | { minRows?: number; maxRows?: number }`；
  - 扩展支持 `rows?: number | string`；
  - `value` 与 `modelValue` 兼容 `string | null` 类型。
- **模板与属性透传** (`src/components/common/VInput.vue`)：
  - `<n-input>` 绑定透传 `:autosize="autosize"` 与 `:rows="rows"`；
  - `innerValue` 强化计算属性判定，兼容 `null` 与 `undefined` 安全解构与回退；
  - 完善文本域样式支持：为 Scoped SCSS 补充 `:deep(.n-input__textarea-el)` 样式，与单行输入框一致应用 Inter 字体、纯净边框交互及光标颜色（亮色 `#2b2d31`，暗色 `#d4d4d4`）。
- **上层搜索组件协同** (`src/components/common/VSearch.vue`)：
  - 同步更新 `innerValue`，兼容 `null` 值判定，保障 TypeScript 强类型严格检查无报错。

### 2.2 `TeamFormModal.vue` 模板全量替换
在 `src/views/competition/components/TeamFormModal.vue` 中，共计 17 处 `<n-input>` 全部替换为 `<VInput>`：
1. **战队元数据表单**：
   - 战队名称 (`formData.teamName`)；
   - 队伍所属单位 (`formData.organization`)；
   - 队伍简介多行文本域 (`formData.description`，`type="textarea"`，`:autosize="{ minRows: 2, maxRows: 2 }"`，`maxlength="200"`)。
2. **队员名单表格 (Members Table)**：
   - 姓名 (`row.realName`，`size="small"`)；
   - 工号/学号 (`row.employeeNumber`，`size="small"`)；
   - 手机号 (`row.phone`，`size="small"`)；
   - 邮箱 (`row.email`，`size="small"`)；
   - 身份证 (`row.idCard`，`:value` 与 `@update:value` 响应式脱敏格式化)；
   - 部门/学院 (`row.department`，`size="small"`)；
   - 职位/班级 (`row.position`，`size="small"`)。
3. **指导老师/领队表格 (Leaders Table)**：
   - 姓名 (`row.realName`，`size="small"`)；
   - 工号 (`row.employeeNumber`，`size="small"`)；
   - 手机号 (`row.phone`，`size="small"`)；
   - 邮箱 (`row.email`，`size="small"`)；
   - 身份证 (`row.idCard`，`:value` 与 `@update:value` 响应式脱敏格式化)；
   - 部门 (`row.department`，`size="small"`)；
   - 职位 (`row.position`，`size="small"`)。

### 2.3 依赖清理与导入重构
- 从 `naive-ui` 导入列表中彻底清理移除 `NInput`；
- 统一从 `@/components/common` 集中导入 `VInput`。

---

## 3. 规范审计与验证

1. **纯粹代码与零注释原则**：所有修改文件（`TeamFormModal.vue`、`VInput.vue`、`VSearch.vue`、`types.ts`）严格遵守无行内解释性注释铁律；
2. **构建与类型验证**：执行 `npm run build`（`vue-tsc --noEmit && vite build`），全部通过，0 错误；
3. **视觉与交互一致性**：表格内各小尺寸输入框与顶部大输入框、多行文本域均统一继承 VInput 设计系统规范（圆角 8px、精致边框状态、#2b2d31 光标及夜间模式自适应）。
