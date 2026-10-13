# 管理员容器管理启动容器模态框提取与表格列重构方案记录

## 1. 变更背景与设计动机
在平台管理端容器管理视图（`AdminContainerView.vue`）中，原有的“手动启靶”功能以轻量内嵌弹框形式实现，缺乏多节点集群编排、私有镜像源直接选择、自定义端口映射、多数据卷挂载、环境变量以及细粒度资源上限（CPU / 内存限制）配置能力。
为全面贴合 Docker Desktop 等专业容器管理交互体验，提高容器启动调度的灵活性和规范性，同时解耦视图层代码，本次重构：
1. 将容器启动能力独立提取为专用模态框组件 `AdminContainerLanuchModal.vue`；
2. 按钮文案由“手动启靶”统一修改为“启动容器”；
3. 基于 `VModal.vue` 构建紧凑阶梯式卡片布局，划分“基础设置”、“高级设置”与“资源配置”三层卡片；
4. 针对 `AdminContainerView.vue` 中的表格字段与顺序进行标准化调整，严格按顺序重构为：实例ID、创建用户、容器状态、剩余市场、访问入口、操作。

---

## 2. 核心改动点说明

### 2.1 独立模态框组件 `AdminContainerLanuchModal.vue`
- **模态框基座与头部/底部规范**：
  - 基础组件：基于平台通用 `VModal.vue`，受控双向绑定 `v-model:show`。
  - 标题规范：采用 `:title="'启动容器'"` Prop 模式渲染，移除自定义 `#header` 插槽，彻底消除 Vue 3 Scoped CSS 隔离导致的字体大小、粗细与亮暗主题色差异，与 `AdminImageView` 等模态框视觉 100% 统一。
  - `<footer>`：严格仅包含一个 `VBtn.vue` Primary 样式按钮，文案为 `启动`，杜绝冗余取消按钮。
- **中间内容三层紧凑卡片布局**：
  - **卡片一：基础设置**
    - 第一行：使用 `VSelection.vue` 载入私有 Registry 中存在的镜像，支持搜索过滤；
    - 第二行：容器自定义名称（使用 `VInput.vue`）与集群调度候选节点（使用 `VSelection.vue`，集成 `getAdminContainerCandidateNodesApi` 并提供推荐自动调度选项）。
  - **卡片二：高级设置**（完全对齐 Docker Desktop 体验）
    - 端口映射（Port mappings）：动态行式配置宿主机端口（Host Port）、容器端口（Container Port）及传输协议（TCP / UDP），支持动态新增与单行移除；
    - 数据卷挂载（Volumes）：动态行式配置宿主机目录（Host Path）、容器内目录（Container Path）及挂载读写模式（读写 `rw` / 只读 `ro`）；
    - 环境变量（Environment variables）：动态行式配置键名（Variable Name）与键值（Value）。
  - **卡片三：资源配置**
    - CPU 限制（核）：使用 `VInput.vue`；
    - 内存限制（MB）：使用 `VInput.vue`；
    - 容器时间（分钟）：使用 `VInput.vue`，默认配置 60 分钟。
- **全链路 Fail-Fast 与 API 强类型绑定**：
  - 提交前进行镜像必填校验，过滤空输入项并自动转换为数字类型；
  - 接入 `manualStartAdminContainerApi` 触发后端容器调度；
  - 在启动成功后触发 `success` 事件通知父视图刷新统计卡片与容器列表。

### 2.2 视图层与表格列重构 (`AdminContainerView.vue`)
- **操作按钮重构**：
  - 按钮文案与国际化回退从“手动启靶”变更为“启动容器”；
  - 移除原视图内冗余的 `startForm`、`startingContainer` 与启动方法，替换为引用 `<AdminContainerLanuchModal>`。
- **表格列 (`VTable` `tableColumns`) 顺序与标题重构**：
  - 保持首列选择框（`selection`）供批量销毁调度使用；
  - 列标题及顺序严格按照要求调整为：
    1. `实例ID` (`containerId`)
    2. `创建用户` (`username`)
    3. `容器状态` (`status`)
    4. `剩余市场` (`remainingSeconds`)
    5. `访问入口` (`accessUrl`)
    6. `操作` (`actions`)

---

## 3. 影响范围与回归验证
- **影响范围**：
  - `src/views/admin/container/AdminContainerView.vue`
  - `src/views/admin/container/AdminContainerLanuchModal.vue`
  - `src/views/admin/container/components/AdminContainerLanuchModal.vue`
- **回归验证结果**：
  - 运行 `vue-tsc --noEmit && vite build`，编译与类型检查 100% 通过（exit code 0），无任何 TypeScript 报错。
  - 符合纯粹代码与零注释规范要求。
