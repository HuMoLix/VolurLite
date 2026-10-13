# VAvatar 环状描边 (Ring) 规范应用设计与施工记录

## 1. 变更背景与诉求
在 Volur Lite NEXT 前端工程中，组件库中的 `VAvatar` 提供了内置的 `ring` 属性及相应的 `.has-ring` Google 规范环状轮廓样式（采用 `::after` 伪元素呈现 1.5px 纯净轮廓，并在亮色/暗色模式下分别自适应 `#dadce0` 与 `#3c4043` 边框，Hover 时联动主题高亮色）。

此前，在部分 CTF 赛事视图中：
1. `VCtfCompetitionStatistic.vue` 侧边栏战队统计卡片未启用 `ring` 属性，导致头像缺乏立体边界与清晰轮廓；
2. `CtfCompetitionTeamView.vue` 队伍详情视图中：
   - 队伍头部大头像使用了外挂的 Tailwind 边框类名 `border border-google-border-light dark:border-google-border-dark`，未接入 `VAvatar` 标准的 `ring` 体系；
   - 参赛成员战绩列表中的成员小头像未开启 `ring` 属性。

本次重构统一为上述组件中的 `VAvatar` 接入规范的 `ring` 属性，并清理外部冗余硬编码边框类名。

---

## 2. 详细修改点与改动文件

### 2.1 `src/components/competition/ctf/VCtfCompetitionStatistic.vue`
- 为左上角队伍信息区域的 `VAvatar` 组件增加 `ring` 布尔属性。
- 与卡片内边距及阴影层次形成更精致的视觉层次。

### 2.2 `src/views/competition/tabs/CtfCompetitionTeamView.vue`
- **队伍头部大头像**：
  - 增加 `ring` 属性；
  - 移除 `border border-google-border-light dark:border-google-border-dark` 冗余硬编码类名，保留 `shrink-0`，避免双层描边冲突，保证暗色/亮色下由 `VAvatar` 内部伪元素标准管控。
- **选手参赛成绩表格**：
  - 为 `memberAchievements` 循环中的选手头像 `VAvatar` 添加 `ring` 属性，增强表格单元格内头像的可视辨识度。

---

## 3. 安全隐患说明与影响范围评估

### 3.1 安全性说明
- 本次变动仅涉及纯展示层 UI 属性传递，未修改任何数据模型与网络请求，无安全隐患。

### 3.2 影响范围
- 受影响视图：
  - CTF 赛事竞技场右侧队伍实时统计卡片（`VCtfCompetitionStatistic`）
  - CTF 赛事战队面板（`CtfCompetitionTeamView`）
- 编译构建验证：
  - `vue-tsc --noEmit` 严格类型检测 100% 通过；
  - `vite build` 生产构建打包通过无异常。
