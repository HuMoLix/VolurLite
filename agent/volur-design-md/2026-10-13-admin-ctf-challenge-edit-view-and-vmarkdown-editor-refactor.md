# AdminCtfChallengeEditView 交互优化与 VMarkdownEditor 组件重构审计

## 变更背景
为了进一步提升 CTF 赛题编辑与创建页面的操作体验，避免界面操作按钮的冗余和视觉混淆，对 `AdminCtfChallengeEditView.vue` 进行交互层级规范化与组件化重构：
1. 移除标题左侧与全局侧边导航功能重叠的“返回CTF题库”按钮，提升头部信息密度与视觉清爽度；
2. 规范标题右侧核心操作按钮尺寸，统一调整为标准 `medium` 规格；
3. 将“取消”与“保存修改”按钮设计为基于赛题表单脏状态（`isDirty`）的动态响应展示，在用户未对数据做出实质性变更时隐蔽显示，在变更后平滑滑入，点击取消可自动放弃未保存更改；
4. 增强赛题类型（Challenge Type）选择器的视觉识别度，集成 `VIcon` 呈现不同题型语义图标；
5. 去除选择项中的冗余双语后缀（如 `(DYNAMIC_CONTAINER)`、`(Easy)`），保持单一纯净的语言呈现；
6. 参考旧版前端优秀实现，在 `NEXT` 通用组件库中原生研发 `VMarkdownEditor.vue`，集中收敛编辑/预览切换、快捷排版工具条、Tab 缩进、自动补全括号引号、快捷键支持及光标状态栏，并在赛题编辑视图中深度复用。

## 详细改动点

### 1. VMarkdownEditor 通用组件实现 (`src/components/common/VMarkdownEditor.vue`)
- **双模切换**：支持滑动胶囊动画的“编辑”与“预览”模式切换；
- **格式工具栏**：支持粗体（B）、斜体（I）、行内代码、引用块、无序列表、有序列表、链接插入、快速表格模板等操作；
- **键盘辅助交互**：
  - 支持 `Tab` 与 `Shift+Tab` 单行/多行缩进；
  - 支持 `Enter` 智能换行续行（任务列表、无序列表、有序列表、引用块及缩进继承）；
  - 支持括号、引号、反引号自动成对输入与成对退格删除；
  - 支持 `Ctrl/Cmd + B`、`Ctrl/Cmd + I`、`Ctrl/Cmd + K` 快捷键；
- **状态栏展示**：实时展示光标行列定位、总字符数及总行数统计；
- **Google Workspace 设计语言**：全面采用 Google Blue、Google Gray 表面色与纯净的暗黑模式适配；
- **模块导出**：在 `src/components/common/index.ts` 集中导出，在 `types.ts` 中声明强类型 `VMarkdownEditorProps`。

### 2. AdminCtfChallengeEditView 改造 (`src/views/admin/challenge/AdminCtfChallengeEditView.vue`)
- **移除冗余返回按钮**：删除 `<h2>` 标题左侧的返回列表按钮与 `ArrowBackOutline` 依赖；
- **按钮规格统一**：将顶部操作按钮（取消、保存）及附件上传按钮统一调整为 `size="medium"`；
- **表单脏状态响应 (`isDirty`) 与平滑动效**：
  - 维护初始表单数据快照 `initialSnapshot`；
  - 计算属性 `isDirty` 精确判定表单内容是否发生变动；
  - 配合 `<Transition>` 实现“取消”与“保存修改”按钮在表单变动后平滑弹出；
  - 点击“取消”按钮时，在编辑模式下无损回滚至原始快照状态并消除按钮，在新增模式下返回列表；
- **题型选择器图标化 (`VIcon`)**：
  - `STATIC` 映射 `DocumentTextOutline`；
  - `DYNAMIC_CONTAINER` 映射 `CubeOutline`；
  - `CONTAINER` 映射 `ServerOutline`；
  - 通过 `VSelection` 的 `:render-label` 插槽函数进行图标+文本渲染；
- **单语言文本净化**：
  - 赛题类型仅保留“静态赛题”、“动态靶机”、“独立容器”；
  - 难度级别仅保留“入门”、“简单”、“中等”、“进阶”、“困难”；
- **整合 VMarkdownEditor**：完全替换原有的外部临时编辑/预览切换卡片，以统一组件承载赛题描述。

## 影响范围与回归验证
- `src/views/admin/challenge/AdminCtfChallengeEditView.vue`
- `src/components/common/VMarkdownEditor.vue`
- `src/components/common/index.ts`
- `src/components/common/types.ts`
- 经过 TypeScript 编译检查，上述所有文件均无任何编译错误或类型隐患，且严格遵循零注释与 ES2026 规范。
