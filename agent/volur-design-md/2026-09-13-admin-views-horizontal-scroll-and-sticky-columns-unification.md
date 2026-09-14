# 管理后台全量管理视图横向滑动与操作列固定规范加固

## 1. 变更背景与问题定位

在 Volur Lite 管理后台中，各管理视图的数据表格列数较多且包含雪花算法 ID、长文本标题、时间、状态指示等多种复合型单元格。

此前排查发现，不同视图在不同分辨率屏幕下的横向滑动体验存在严重不一致：
1. **部分视图无法横向滑动**：
   - 如 `AdminTaskView.vue` 由于在 `taskName` 列上配置了 Naive UI 的 `ellipsis: { tooltip: true }`，触发了 Naive UI 内部将表格布局由默认的 `"auto"` 强制切换为 `"fixed"`；
   - 在未显式声明 `scroll-x` 宽度的情况下，Naive UI 底层将 `xScrollableRef` 判定为 `false`，导致 `Scrollbar` 组件禁用了横向滚动，表格被死死压缩在 100% 容器宽度内，导致列内容严重挤压、无法通过触控板或滚动条横向滑动。
2. **操作列固定（Sticky）体验缺失**：
   - 部分视图（如 `AdminCompetitionListView.vue`、`NodesTab.vue`、`AdminImageView.vue`）的操作列未声明 `fixed: 'right'`，在宽表场景下管理员若横向滚动，操作按钮会滑出视口，降低了交互操作的便利性与一致性。

---

## 2. 改造方案与改动点汇总

对 Volur Lite 核心九大后台管理视图统一配置 `:scroll-x` 属性，并为所有操作列统一声明 `fixed: 'right'`，确保在各种分辨率屏幕下均能平滑横向滚动，操作列始终优雅吸附在右侧：

| 管理视图文件 | 设定的 `scroll-x` 宽度 | 计算依据（各列最小/设定宽度之和） | 操作列 `fixed: 'right'` 状态 |
| :--- | :--- | :--- | :--- |
| [`AdminUserView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/user/AdminUserView.vue) | `1170` | 180(ID) + 260(用户) + 150(组织) + 130(角色) + 120(状态) + 180(时间) + 150(操作) | 已具备 |
| [`AdminCompetitionListView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/AdminCompetitionListView.vue) | `1210` | 180(ID) + 200(标题) + 120(类型) + 100(状态) + 260(时间) + 130(规模) + 220(操作) | **新增配置** |
| [`AdminBroadcastView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/broadcast/AdminBroadcastView.vue) | `1360` | 180(ID) + 200(标题) + 110(分类) + 130(形式) + 220(内容) + 90(优先级) + 120(发布者) + 170(时间) + 140(操作) | 已具备 |
| [`NodesTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/cluster/tabs/NodesTab.vue) | `1440` | 200(节点) + 170(IP) + 120(角色) + 150(资源) + 140(状态) + 100(容器) + 100(镜像) + 160(规格) + 300(操作) | **新增配置** |
| [`AdminContainerView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/container/AdminContainerView.vue) | `1650` | 48(选择) + 180(ID) + 170(题目) + 140(用户) + 160(容器ID) + 160(节点) + 200(URL) + 180(Flag) + 110(状态) + 120(剩余时间) + 180(操作) | 已具备 |
| [`AdminImageView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/image/AdminImageView.vue) | `850` | 320(镜像全名) + 120(大小) + 170(拉取时间) + 220(操作) | **新增配置** |
| [`AdminLogView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/log/AdminLogView.vue) | `1140` | 180(ID) + 90(级别) + 120(模块) + 160(动作) + 150(操作人) + 90(耗时) + 90(状态) + 170(时间) + 90(操作) | 已具备 |
| [`AdminTaskView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/task/AdminTaskView.vue) | `1260` | 180(ID) + 220(名称) + 130(类型) + 120(状态) + 190(进度) + 100(操作人) + 150(时间) + 170(操作) | 已具备 |
| [`AdminEmailView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/email/AdminEmailView.vue) | `1250` | 180(ID) + 170(收件人) + 200(主题) + 130(类型) + 160(发件人) + 100(状态) + 170(时间) + 140(操作) | 已具备 |

---

## 3. 验证与影响范围

1. **响应式与交互验证**：
   - 彻底解决了由于列配置带有 `ellipsis` 导致 Naive UI 内部禁用横向滚动的隐蔽机制；
   - 所有九大管理视图在屏幕宽度小于表格设定宽度时，均能自动展现横向滚动条，支持鼠标滚轮（Shift+滚轮）、触控板横向平滑滚动；
   - 所有视图右侧操作列均已启用 `fixed: 'right'`，并在横向滚动时自动展示右侧半透明阴影（Naive UI 内置 fixed-col 阴影），保持一致的操作体验。
2. **构建与类型验证**：
   - 运行 `npm run build`（`vue-tsc --noEmit && vite build`）通过，耗时 4.88s，无任何编译与语法错误。
