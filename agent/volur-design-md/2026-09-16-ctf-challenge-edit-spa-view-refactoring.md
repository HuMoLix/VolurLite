# CTF 赛题编辑与新建功能 SPA 独立组件化重构施工文档

## 1. 变更背景
此前，管理端 CTF 赛题的创建与编辑功能（Create & Edit Challenge）以弹窗卡片（Modal Card, `n-modal preset="card"`）的形式直接内嵌在题目列表视图（`AdminCtfChallengeView.vue`）中。这带来了一系列体验与架构上的缺陷：
1. **交互空间受限**：弹窗卡片宽度有限，在处理长文本 Markdown 题目描述、附件上传预览以及动态容器（镜像名、端口、CPU/内存配额）多维配置时，垂直滚动容易误触弹窗遮罩，导致用户已编辑的内容丢失；
2. **缺乏路由语义与 SPA 架构支持**：无法通过标准前端路由直接定位到特定题目的编辑页面，刷新页面会丢失编辑状态，无法形成浏览器的历史堆栈导航；
3. **主视图代码冗余**：列表主视图混杂了题目表单校验、附件上传状态、实体响应式对象及大量弹窗专属样式，严重破坏组件的高内聚与单一职责原则。

因此，按照架构规范将编辑/创建 CTF 赛题独立为标准的 SPA 页面组件（`AdminCtfChallengeEditView.vue`），废除弹窗卡片模式。

---

## 2. 核心改动点

### 2.1 SPA 路由注册 (`src/router/routes.ts`)
在 `admin/challenges` 路由分组的子路由中，新增独立的新建与编辑路由声明：
1. `/admin/challenges/ctf/create`（路由名 `AdminCtfChallengeCreate`）：挂载新建赛题单页；
2. `/admin/challenges/ctf/:id/edit`（路由名 `AdminCtfChallengeEdit`）：挂载指定题目编辑单页；
3. 配置完整的父子级面包屑元数据（`meta.parentTitle` 为“CTF 题库”，`meta.parentTitleKey` 为 `admin.challenges.ctf_title`）。

### 2.2 平台布局侧边栏激活态优化 (`src/public/volurEx/layouts/AdminLayout.vue`)
优化了后台侧边栏的 `activeKey` 响应式计算属性，当进入以 `AdminCtfChallenge` 开头的子路由（即新建或编辑页面）时，侧边栏保持高亮“CTF 题库”菜单项，与竞管模块（`AdminCompetition*`）保持一致的导航体验。

### 2.3 新建 SPA 组件 (`src/views/admin/challenge/AdminCtfChallengeEditView.vue`)
新建了高质感的 SPA 单页视图组件，支持作为独立路由视图或内嵌组件双模运行：
1. **顶部导航栏 (`subview-header`)**：
   - 提供快速返回题库按钮（`ArrowBackOutline`），带平滑的返回动效；
   - 动态标题展示：新建时显示“新增 CTF 赛题”，编辑时显示“编辑 CTF 赛题”并附加题目标题 Badge 及赛题模式 Tag（`STATIC` / `CONTAINER` / `DYNAMIC`）；
   - 顶部快捷操作栏：取消与保存操作按钮。
2. **结构化分卡表单布局**：
   - **基本信息卡片**：题名、分类选择（集成自定义图标渲染）、模式选择、难度打星（1-5 颗星）、静态 Flag；
   - **题目说明与指引卡片**：多行 Markdown 自适应文本域；
   - **赛题附件配置卡片**：支持实时上传资源落盘、Tag 预览与下载、外部 URL 补充；
   - **动态靶机容器配置卡片**：仅当选择容器或动态题型时平滑展示，涵盖镜像全称、开放端口、CPU/内存配额及动态靶机调度说明。
3. **底部操作栏**：提供“取消并返回”与“保存赛题修改/确认创建赛题”（遵照 Volur 规范使用黑白高对比按钮 `.theme-primary-btn`）。
4. **严格表单校验**：严格依照后端 Jakarta Validation 注解规则（`@NotBlank` 题名、`@NotNull` 分类与模式）进行字段合法性判定。

### 2.4 主列表页精简 (`src/views/admin/challenge/AdminCtfChallengeView.vue`)
1. **废除弹窗卡片**：移除 `<n-modal v-model:show="showEditModal">` 及其包含的数百行表单模板；
2. **交互路由跳转**：
   - 点击顶部操作栏“新增题目”按钮，通过 `router.push('/admin/challenges/ctf/create')` 跳转至新建 SPA；
   - 表格操作列点击“编辑”按钮，通过 `router.push('/admin/challenges/ctf/${row.id}/edit')` 跳转至编辑 SPA；
3. **样式与状态清理**：完全清除了列表中已废弃的表单响应式状态、表单校验规则、附件上传临时状态及相关弹窗 CSS 样式，使列表页面由原本臃肿的代码大幅缩减，首屏渲染性能显著提升。

---

## 3. 安全隐患与健壮性说明
1. **避免数据丢失**：表单独立于 SPA 单页中，用户可通过浏览器刷新保留路由上下文；取消操作具备二次确认/明确路由回退保障。
2. **参数精度与类型对齐**：路由参数 `id` 通过 `route.params.id` 安全提取，入参载荷严格映射 `CtfChallengeCreateRequestDTO` / `CtfChallengeUpdateRequestDTO`，去除前端未定义字段（`undefined`），避免向后端发送污染数据。
3. **鉴权与防刷**：路由进入受后台全局鉴权守卫控制，未登录或无管理员权限用户将被安全重定向。

---

## 4. 影响范围
- **受影响模块**：
  - 前端路由表：`src/router/routes.ts`
  - 前端布局：`src/public/volurEx/layouts/AdminLayout.vue`
  - 题目管理界面：`AdminCtfChallengeView.vue`、`AdminCtfChallengeEditView.vue`
- **非影响模块**：后端 API 接口签名无破坏性变动；理论题、ISW、AWD 等其他题型与选手前台答题流程保持隔离不受影响。

---

## 5. 编译与构建验证
- **TypeScript 静态检查**：`npx vue-tsc --noEmit` 校验通过，退出码为 0，无任何类型推断或语法报错。
- **生产构建打包**：`npm run build` 打包成功，`AdminCtfChallengeEditView` 与 `AdminCtfChallengeView` 各自打包为独立的 Chunk 模块。
