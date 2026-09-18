# 参赛队伍详情弹窗参考系统统一风格的重构施工记录

**施工日期**：2026-09-18  
**施工模块**：`src/volur-lite-view/src/views/admin/competition/tabs/TeamAuditTab.vue`  
**责任主体**：Antigravity AI Agent  

---

## 1. 变更背景与系统级设计反思

在上一个版本中，队伍详情弹窗虽然补充了信息字段，但在排版上存在**视觉碎片化**的问题：
- 采用了大量的自定义 div 卡片、双列网格、自定义小标签与虚线分割线，视觉上被切分成数十个散乱的小方块；
- 脱离了 Volur Lite 平台其他核心模块（如比赛详情、邮件详情、系统日志等）成熟通用的后台设计语言；
- 成员名册在多个小卡片并排时缺少工业级后台数据对齐的美感与严谨度。

### 系统整体风格基准：
- **队伍概况**：统一采用 Naive UI 标准原生组件 **`<n-descriptions bordered size="small" :column="2">`**；
- **成员资质名单**：统一采用 Naive UI 工业级数据表格 **`<n-data-table size="small" :bordered="true">`**；
- **操作底栏**：统一采用 `<template #footer>` 两端弹性布局。

---

## 2. 核心重构方案与落地细节

### 2.1 队伍基础资料模块 (`NDescriptions`)
- 使用 `<n-descriptions label-placement="left" :column="2" bordered size="small">` 规整组织基础元数据：
  - **战队名称**：集成 26px 战队头像与加粗标题；
  - **审核状态**：圆角彩色状态胶囊 Tag；
  - **队长账号**：强调加粗展示；
  - **战队规模**：成员总数；
  - **报名提交时间**：格式化时间戳；
  - **归属竞赛**：展示赛事名称；
  - **历史驳回原因**：跨双列（`:span="2"`）红字醒目提示（仅当被驳回且有原因时展示）。

### 2.2 参赛成员实名资质名册表格 (`NDataTable`)
放弃视觉碎片的网格卡片，全面切换为标准清晰的成员数据表格：
- **列 1：成员信息 (170px)**：圆形头像 (30px) + 真实姓名（加粗）+ `@username` 账号（灰字），紧凑双行；
- **列 2：队内角色 (110px)**：角色胶囊（战队队长 `primary`、带队领队 `warning`、参赛队员 `default`）；
- **列 3：性别 (65px)**：迷你胶囊（男 `info` / 女 `error`）；
- **列 4：学号/工号 (130px)**：等宽字体 (`font-family: monospace`) 清晰显示；
- **列 5：所属院系 (minWidth 120px)**：支持长文本自动省略提示（`ellipsis: { tooltip: true }`）；
- **列 6：联系方式 (170px)**：手机号码（等宽字体）+ 电子邮箱（细灰小字换行），规整呈现；
- **列 7：身份证件号码 (180px)**：使用等宽字体搭配浅灰安全底色包裹（`display: inline-block; background: rgba(0, 0, 0, 0.04); padding: 2px 6px; border-radius: 4px;`），绝对不折行，极度便于肉眼核对；
- **列 8：加入时间 (105px)**：格式化日期显示。
- **智能排序**：队长始终置于首行，领队次之，队员排后。
- **表格滚动**：配置 `max-height="340"` 与 `:scroll-x="960"`，在大规模队伍场景下支持丝滑滚动查验。

### 2.3 弹窗底栏快捷审核闭环
- 底栏左侧提示队伍当前审核状态；
- 底栏右侧保留一键“通过资质审核”（`theme-primary-btn`）、“驳回报名”（`error secondary`）以及“关闭”按钮；
- 审核动作触发后，弹窗内与外部列表同时无缝刷新。

---

## 3. 验证与构建测试

在 `src/volur-lite-view` 目录下执行全量构建验证：
```bash
npm run build
# 执行: vue-tsc --noEmit && vite build
```

**验证结果**：
- `vue-tsc --noEmit`：0 错误，TypeScript 严格类型安全；
- `vite build`：生产打包成功（耗时 5.74s）。

---

## 4. 影响范围与审计评估

- **改动文件**：
  - `src/volur-lite-view/src/views/admin/competition/tabs/TeamAuditTab.vue`
- **视觉一致性评估**：
  - 彻底消除了杂乱的小卡片拼凑感；
  - 视觉风格 100% 融入 Volur Lite 统一的 Naive UI 原生设计系统（`n-descriptions` + `n-data-table`），排版规整、信息严谨、交互流畅。
