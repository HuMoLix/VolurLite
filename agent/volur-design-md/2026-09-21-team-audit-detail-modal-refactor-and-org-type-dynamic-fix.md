# 管理端战队详情审核视图重构与单位类型动态表头修复记录

## 1. 变更背景
在管理后台审查战队时（[`TeamAuditTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/tabs/TeamAuditTab.vue)），原先直接照搬报名模态框的左右 50% 双栏排版存在以下问题：
1. **右侧报名须知对管理员无意义**：管理端无需重复查阅报名时间、比赛时间与报名须知，双栏挤占了成员名册的展示空间；
2. **左侧表单输入控件不符合审查视图语义**：管理员只是查看已有信息，被禁用的 `<n-input>` 与 `<n-select>` 带来冗余的输入框边框与交互提示，视觉体验笨重；
3. **“学校”单位类型下仍错误展示“工号/部门/职位”Bug**：
   - 历史存量数据或接口响应中若 `organizationType` 为空/null，前端直接比对 `organizationType !== 'SCHOOL'` 产生布尔真值判定，导致学校单位队伍被误判为企业单位，错误展示为工号、部门、职位；
   - 缺少对中英文字符串（如大写/小写/中文）兼容的统一归一化计算属性。

## 2. 改动详情

### 2.1 模板与排版重构 (`TeamAuditTab.vue`)
1. **移除双栏排版与报名须知卡片**：
   - 彻底删除 `team-meta-container` 左右 50% 网格布局与垂直分割线；
   - 彻底移除 `requirements-card`（包含报名时间、比赛时间、报名须知及管理员自定义要求等卡片内容）；
2. **重构队伍基础信息为纯文本审查卡片 (`team-overview-card`)**：
   - 全宽卡片，不使用任何 `<n-input>`、`<n-select>` 表单输入框；
   - 顶部行：
     - 左侧 52px 队伍头像（[`UserAvatar`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/components/common/UserAvatar.vue)）；
     - 右侧队伍名称清晰聚焦（去除冗余重复的“已通过”、“学校”角标，避免与下方信息及弹窗底部状态重复）；
     - 队伍关键属性以文本信息块（`team-info-chips`）呈现：所属单位、单位类型、队长账号、报名时间；
   - 底部行：队伍简介文本区域（`team-desc-line`），以虚线优雅分隔，空时友好提示“暂无简介”。
3. **动态列头修复 (`isSchoolOrganization`)**：
   - 参赛成员名册（`detailMembersOnly`）与领队名册（`detailLeadersOnly`）的表头统一使用 `isSchoolOrganization ? '学号' : '工号'`、`isSchoolOrganization ? '学院' : '部门'`、`isSchoolOrganization ? '班级' : '职位'`；
   - 只要队伍是学校（或存量数据未指定单位类型时默认学校），100% 准确展示为“学号”、“学院”、“班级”；企业单位、军工、能源等其他单位则准确展示为“工号”、“部门”、“职位”。

### 2.2 脚本与后端健壮性增强
1. **前端归一化计算属性与字典映射 (`TeamAuditTab.vue`)**：
   - `organizationTypeMap`：支持学校、企业单位、军工、能源、金融、科研院所、其他 7 种单位类型；
   - `getOrganizationTypeLabel`：提供安全的大小写不敏感回显中文名称；
   - `isSchoolOrganization` 计算属性：当 `!orgType` 或归一化后等于 `SCHOOL` / `学校` 时判定为学校，其余判定为非学校；
   - 清理移除未使用的 `InformationCircleOutline`、`organizationTypeOptions`、`formattedRegTimeRange`、`formattedCompTimeRange`、`adminCustomRequirements`。
2. **后端兜底补全**：
   - 在 [`AdminTeamServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/admin/impl/AdminTeamServiceImpl.java) 与 [`CompetitionTeamServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/impl/CompetitionTeamServiceImpl.java) 的 DTO/VO 组装中，若实体 `team.getOrganizationType()` 为 null，安全兜底为 `OrganizationType.SCHOOL`，彻底杜绝返回 null 导致前端判定漂移。

### 2.3 样式系统适配
1. 日间模式：`.team-overview-card` 采用卡片灰底 (`#fafafc`)、`#111111` 标题与 `#444444` 描述文字，高雅简洁；
2. 黑夜模式：适配 Obsidian 深曜炭黑阶梯背景 (`#12141c`)、低饱和度微透明分割线与浅色反色文字。

## 3. 验证情况
- **前端构建**：执行 `npm run build`（`vue-tsc --noEmit && vite build`），0 error 顺利通过；
- **后端编译**：执行 `mvn compile -pl volur-competition -am`，BUILD SUCCESS，0 error 顺利通过。

## 4. 影响范围
- 前端：[`src/volur-lite-view/src/views/admin/competition/tabs/TeamAuditTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/tabs/TeamAuditTab.vue)
- 后端：[`src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/admin/impl/AdminTeamServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/admin/impl/AdminTeamServiceImpl.java)
- 后端：[`src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/impl/CompetitionTeamServiceImpl.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/impl/CompetitionTeamServiceImpl.java)
