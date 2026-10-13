# 镜像管理表格表头简化、字体规范统一与 Docker Desktop 风格分层抽屉集成

## 1. 变更背景与需求说明

在管理后台镜像管理视图（`AdminImageView.vue`）的原实现中，存在以下体验与交互细节需要优化：
1. **表头双语冗余**：表格标题采用了中英文混排描述（如“仓库名称 (Repository)”、“标签 (Tag)”），在整体中文管理视图中显得冗余笨重，需精简为统一规范的“镜像名称”与“镜像标签”；
2. **文字字体不统一**：表格中标签、体积与时间等字段硬编码指定了 `font-mono`（即 `Fira Code, monospace`），与整体系统和 `VTable.vue` 默认继承的标准 Inter 字体体系不一致，需移除 mono 特殊字体，全部统一继承 `VTable.vue` 样式；
3. **缺少直观的分层可视化查看能力**：管理员需要查看 Docker 镜像的构建层级步骤（Layers）。参考 Docker Desktop 中的体验，点击镜像名称（设置 VTable 的 `redirectable` 可点击样式）即可从右侧拉出抽屉（`VDrawer.vue`），深入查看镜像的元数据概要和各层构建指令、体积与步骤编号。

---

## 2. 核心改动点

### 2.1 表头精简化与双语描述取消
- 在 `tableColumns` 中：
  - 将原 `仓库名称 (Repository)` 修改为 `镜像名称`；
  - 将原 `标签 (Tag)` 修改为 `镜像标签`；
  - 保留其余列的简洁中文定义（`占用体积`、`创建时间`、`操作`）。

### 2.2 字体样式统一与 Mono 样式剥离
- 移除表格各列中多余的 `font-mono` 类名绑定：
  - 标签列由 `font-mono text-xs` 调整为 `text-xs`；
  - 占用体积列由 `text-xs font-mono ...` 调整为 `text-xs ...`；
  - 创建时间列由 `text-xs ... font-mono` 调整为 `text-xs ...`；
- 所有单元格文字样式直接继承 `VTable.vue` 组件的 `Inter, sans-serif` 全局排版规范。

### 2.3 镜像名称 redirectable 交互与 VDrawer Layers 抽屉集成
1. **镜像名称单元格交互**：
   - 为镜像名称列赋予 `redirectable` 类名（由 `VTable.vue` 集中定义高亮、指针悬停与下划线动效）；
   - 绑定 `onClick` 事件触发 `handleOpenImageLayers(row)`，激活分层抽屉并调用 `getAdminImageDetailApi(row.id)` 请求深度镜像元数据与分层列表。
2. **Docker Desktop 风格分层抽屉 (`VDrawer.vue`)**：
   - 抽屉采用 680px 宽度，提供充足的指令阅读空间，支持深色与浅色自适应切换；
   - **顶部概要卡片**：展示镜像完整标签名、分层总大小（MB/GB/KB 自适应）、分层总数、系统架构（OS/Arch）以及镜像摘要 ID（附带一键复制功能）；
   - **分层时间线卡片列表**：
     - 展示分层步骤序号（`#1`、`#2`...）；
     - 自动解析并高亮 Docker 指令徽标（RUN: 蓝色, COPY/ADD: 绿色, ENV/ARG: 黄色, CMD/ENTRYPOINT: 紫色, EXPOSE: 青色等）；
     - 结构化清洗构建指令（剥离 `/bin/sh -c #(nop)` 噪点）；
     - 显示单层物理体积，支持 `0 B` 灰度呈现；
     - 每层均配备独立的一键复制指令按钮，支持 SecureContext Clipboard API 与回退兼容机制；
   - **加载与空状态处理**：内置 `n-spin` 加载遮罩与暂无数据友好提示。

### 2.4 状态统计卡片标题与状态文案精简
- 将顶部 Registry 统计卡片标题由“自建私有 Registry”修改为“Registry 仓库状态”；
- 将离线时的展示文案由“未连接/离线”精简为“离线”；
- 状态文本颜色动态绑定：在线时为 Google Green，离线时自动切换为 Google Red 警示高亮。

---

## 3. 规范自查与审查

1. **纯粹代码与零注释铁律**：修改后的 `AdminImageView.vue` 代码中不包含任何解释性注释与冗余行内备注，代码逻辑清晰自解释。
2. **Fail-Fast & Zero-Mock 原则**：镜像分层数据严格调用 `getAdminImageDetailApi` 服务端真实接口，无任何隐式 mock 假数据或硬编码伪造兜底。
3. **Google Material Design 色彩阶梯规范**：严格使用 Google Blue/Green/Yellow/Cyan 语义配色与 Google Gray 阶梯表面色彩，严禁正文纯黑。
4. **影响范围**：修改仅作用于 `src/views/admin/image/AdminImageView.vue`，不破坏任何已有的分发、导入、导出、拉取与删除功能。
