# CTF 赛题方向分类管理模态框 VIconPicker 落地与按钮尺寸标准化重构记录

## 1. 变更背景与需求目标
在管理后台赛题方向分类管理（`src/views/admin/challenge/components/CtfCategoryModal.vue`）中，存在两个体验与视觉规范缺陷：
1. **按钮纵向视觉偏扁**：
   - 表格操作列内的“编辑”与“删除”按钮配置为 `size: 'tiny'`（高度仅 24px，padding 仅 6px），纵向极度扁小，点击热区过窄；
   - 顶部工具栏“添加分类”与弹窗底部的“关闭”、“取消”、“保存”按钮被配置为 `size="small"`（高度 28px），破坏了 Google Material 胶囊按钮（规范高度 36px）的饱满度，并与页面其它 36px 控件存在高度割裂。
2. **图标选择机制缺失与生硬文本输入**：
   - 此前分类图标使用普通的文本框（`<VInput>`）手输图标名称（例如 `globe-outline`），缺乏可视化预览和检索选择机制；
   - 经典工程 `volur-lite-view` 中具备 `VolurSvgPicker` / `VolurIconPicker`，支持基于 `@vicons/ionicons5` 弹出卡片、分类 Tabs、模糊搜索与分页网格选择；需要在新一代工程 `volur-lite-view-NEXT` 中标准化移植落地为 Google Workspace 主题风格的通用组件。

## 2. 方案设计与技术实现

### 2.1 平台通用图标组件库落地
1. **动态图标渲染组件 [`VIcon.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/common/VIcon.vue)**：
   - 统一封装 `resolveIonIcon(name)` 解析器，支持 `@vicons/ionicons5` 全量图标名称精准匹配、`Outline` 后缀自适应补全、大小写不敏感容错查找；
   - 导出 `getAllIconNames()` 与平台精选分类 `CURATED_ICON_CATEGORIES`（涵盖安全赛题、开发技术、存储数据、交互操作等高频 CTF 场景）；
   - 使用 Vue 动态组件 `<component :is="iconComponent" />` 挂载至 Naive UI `<n-icon>`，支持尺寸、颜色与层深参数。
2. **可视化图标选择器 [`VIconPicker.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/common/VIconPicker.vue)**：
   - **触发框**：严格对齐 Google Workspace 表单控件体系，支持 `small` (28px)、`medium` (36px)、`large` (44px) 尺寸；左侧带图标实时预览，右侧带一键清空与旋转指示箭头；
   - **弹出卡片**：基于 `<n-popover>` 展开浮层，包含实时模糊搜索栏、分类 Tabs 切换胶囊、6 列 CSS Grid 图标展示网格（带 `<n-tooltip>` 悬浮提示）、轻量分页器（每页 48 项，彻底避免千级 SVG 渲染卡顿）以及底部已选项回显与操作按钮；
   - **色彩体系**：严格使用 Google Blue（`#1a73e8` / `#8ab4f8`）、Google Gray 表面色与深色模式阶梯；
3. **接口定义与集中导出**：
   - 在 [`types.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/common/types.ts) 中增加 `VIconProps` 与 `VIconPickerProps` 强类型定义；
   - 在 [`index.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/common/index.ts) 中对外统一导出。

### 2.2 `CtfCategoryModal.vue` 业务落地与重构
1. **替换为可视化图标选择器**：
   - 顶部工具栏中的新建分类图标输入项替换为 `<VIconPicker v-model:value="newCategoryIcon" placeholder="选择图标" size="medium" />`；
   - 编辑分类模态框中的图标输入项替换为 `<VIconPicker v-model:value="editCategoryIcon" placeholder="选择图标" size="medium" />`；
   - 表格“图标”列重构为同时渲染真实 SVG 矢量图形与代码标签：`h(VIcon, { name: row.icon, size: 18 })` + `h('code', ...)`。
2. **按钮尺寸与饱满度标准化**：
   - 顶部新建栏输入框与“添加分类”按钮统一调整为规范尺寸 `medium`（高度 36px，左右 padding 16px 胶囊圆角），消除矮小扁平感；
   - 主弹窗底部“关闭”按钮与编辑弹窗底部“取消”、“保存”按钮统一使用 `medium`（36px）；
   - 表格操作列“编辑”与“删除”按钮由 `size: 'tiny'`（24px）调整为 `size: 'small'`（28px，padding 8px），大幅提升视觉舒适度与点击热区。

## 3. 影响范围与验证
- 影响范围涵盖公共通用组件库 `src/components/common` 及后台赛题方向分类管理视图；
- 全局遵循零行内注释铁律与文档外置原则；
- 执行 `npm run build`，TypeScript 类型检查与 Vite 生产构建全部通过（退出码 0）。
