# 理论知识竞赛视图与题目组件按钮 VBtn 统一重构记录

## 1. 变更背景与需求
为了统一平台按钮与可点击选项交互的视觉规范与体验，遵循 Google Material / Workspace 风格标准，消除零散的行内样式覆写与不一致的按钮配色行为：
1. 在理论知识竞赛页面 (`TheoryCompetitionView.vue`) 中，原有的导航按钮（“上一题”、“下一题”）采用原生 `<button>` 并内联冗长 Tailwind 类，样式与通用按钮规范未对齐。
2. 试卷提交确认弹窗（`n-modal preset="dialog"`）原本使用 Naive UI 内置默认按钮，无法完全契合平台统一的胶囊及圆角视觉规范。
3. 答题卡操作栏（`TheoryPaperSheet.vue`）中“提交答卷”按钮、以及成绩报告弹窗（`TheoryResultModal.vue`）中的“查看并复核答卷”按钮均采用手写内联样式类与原生 `<button>`。
4. **答题卡题目序号切换按钮**（`TheoryPaperSheet.vue`）原先采用原生 `<button>` 配合条件类拼接，需统一重构为通用组件 `VBtn`。
5. **题目卡片中的作答选项**（`TheoryQuestionCard.vue`）中，单选题（`SINGLE_CHOICE`）、多选题（`MULTIPLE_CHOICE`）以及判断题（`TRUE_FALSE`）原先使用原生 `<button>` 实现，需全面统一迁移至通用组件 `VBtn` 并规范其响应式排版与选中态高亮。

---

## 2. 详细改动点

### 2.1 理论知识竞赛视图 (`src/views/competition/TheoryCompetitionView.vue`)
1. 导入并在模板中使用通用按钮组件 `VBtn`：`import { VTooltip, VBtn } from '@/components/common'`。
2. “上一题”导航按钮重构：
   - 使用 `<VBtn clz="normal" :left-icon="ArrowBackOutline" :disabled="currentIndex === 0" @click="handlePrevQuestion">`；
   - 移除原先冗余的手写样式类与独立的 `<n-icon>` 标签，由 `VBtn` 的 `:left-icon` 统一托管渲染。
3. “下一题”导航按钮重构：
   - 使用 `<VBtn clz="normal" :right-icon="ArrowForwardOutline" :disabled="currentIndex === questions.length - 1" @click="handleNextQuestion">`；
   - 移除原先手写类与 `<n-icon>`，由 `VBtn` 的 `:right-icon` 统一托管渲染。
4. 提交确认模态框（`n-modal preset="dialog"`）：
   - 移除 `:positive-text` 与 `:negative-text` 默认按钮属性声明；
   - 新增 `#action` 插槽，包含继续作答按钮 `<VBtn clz="normal" @click="submitConfirmVisible = false">` 与确认提交按钮 `<VBtn clz="primary" :loading="submitting" @click="handleConfirmSubmit">`。

### 2.2 理论答题卡面板 (`src/components/competition/theory/TheoryPaperSheet.vue`)
1. 导入通用组件 `VBtn`，移除未再使用的 `SyncOutline` 图标导入。
2. 将原“提交答卷”原生按钮重构为：
   - `<VBtn clz="primary" block :disabled="submitting" :loading="submitting" :left-icon="CheckmarkCircleOutline" @click="emit('submit')">`；
   - 复用 `VBtn` 内置的 `loading` 加载态动画与自适应禁用拦截。
3. **题目序号按钮全量重构为 `VBtn`**：
   - 将原 `<button v-for="(q, idx) in questions">` 重构为：
     ```vue
     <VBtn
       v-for="(q, idx) in questions"
       :key="q.paperId"
       :clz="isCurrentAndAnswered(q.paperId, idx) ? 'primary' : 'normal'"
       :radius="12"
       class="theory-sheet-item font-mono text-xs font-medium w-full relative"
       :class="getQuestionButtonClass(q.paperId, idx)"
       @click="emit('select-question', idx)"
     >
       <span>{{ idx + 1 }}</span>
       <span
         v-if="isQuestionMarked(q.paperId)"
         class="absolute top-1 right-1 w-2 h-2 rounded-full bg-google-yellow dark:bg-google-yellow-dark"
       />
     </VBtn>
     ```
   - 逻辑层抽象 `isCurrentAndAnswered` 辅助方法：若为当前选中且已作答，应用 `clz="primary"` 沉浸式高亮；未作答或非当前题目统一使用 `clz="normal"`。
   - 新增组件内部样式规则 `.theory-sheet-item`，锁定 40px 高度与等宽数字展示。

### 2.3 题目卡片作答选项全量重构 (`src/components/competition/theory/TheoryQuestionCard.vue`)
1. 导入通用组件 `VBtn`：`import { VBtn } from '@/components/common'`。
2. **单选题选项重构**：
   - 替换为 `<VBtn v-for="opt in formattedOptions" :key="opt.key" clz="normal" block :radius="12" class="theory-option-btn" :class="{ 'is-selected': currentAnswer === opt.key }" :disabled="readonly" @click="selectSingle(opt.key)">`；
   - 保留选项字母圆点与正文自适应换行排版。
3. **多选题选项重构**：
   - 替换为 `<VBtn v-for="opt in formattedOptions" :key="opt.key" clz="normal" block :radius="12" class="theory-option-btn" :class="{ 'is-selected': isMultipleOptionSelected(opt.key) }" :disabled="readonly" @click="toggleMultiple(opt.key)">`；
   - 保留选项字母方角与选中状态多项同步。
4. **判断题选项重构**：
   - 替换为带 `:left-icon` 的胶囊圆角网格按钮：
     - 正确 (True)：`<VBtn clz="normal" block :radius="12" class="theory-tf-btn" :class="{ 'is-true-selected': currentAnswer === 'TRUE' }" :left-icon="CheckmarkOutline" :icon-size="18" :disabled="readonly" @click="selectSingle('TRUE')">`；
     - 错误 (False)：`<VBtn clz="normal" block :radius="12" class="theory-tf-btn" :class="{ 'is-false-selected': currentAnswer === 'FALSE' }" :left-icon="CloseOutline" :icon-size="18" :disabled="readonly" @click="selectSingle('FALSE')">`。
5. 样式层通过 scoped SCSS 深度作用域 (`:deep`) 为 `.theory-option-btn` 与 `.theory-tf-btn` 精准注入 Google Blue、Google Green 与 Google Red 主题色阶及其暗黑模式微透明色系。
6. **“标记待查”按钮边框优化**：
   - 将“标记待查”按钮在激活标记（`isMarked === true`）时的边框样式由 `border-amber-300` / `dark:border-amber-800` 移除，统一为无边框背景填充（`border-transparent`、`outline-none focus:outline-none`），实现点击后无突兀边框高亮。

### 2.4 成绩复核弹窗 (`src/components/competition/theory/TheoryResultModal.vue`)
1. 导入通用组件 `VBtn`。
2. 将底部“查看并复核答卷”操作按钮重构为：
   - `<VBtn clz="primary" block @click="handleConfirm">`。

---

## 3. 规范遵循与代码审计
1. **纯粹代码与零注释原则**：所有被修改的 `.ts` 与 `.vue` 代码文件严格保持 0 行注释，仅在设计文档中详细记录。
2. **构建与类型验证**：
   - 运行 `npx vue-tsc --noEmit`，通过（0 错误 0 告警）。
   - 运行 `npm run build`，编译打包成功通过。
