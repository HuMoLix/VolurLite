# 竞赛成绩导出功能架构与前后端实现方案 (CTF & Theory Score Export)

## 一、 背景与业务目标
在网络安全竞赛（CTF 解题赛）和理论知识答题赛（Theory 知识赛）运维管理中，赛后成绩汇总、颁奖评定、学分归档及答题明细复盘均依赖高可靠的成绩导出功能。
此前平台仅支持赛题导入导出及镜像备份，缺少统一的成绩导出体系。本方案实现了一整套覆盖 CTF 竞赛与理论知识赛的成绩导出功能，全面支持 **Excel (.xlsx)**、**CSV (.csv)** 与 **JSON (.json)** 三种标准格式，并支持选择导出**仅总榜 (Summary)** 或**包含答题/解题流水明细 (Detail)**。

---

## 二、 架构设计与改动点

### 1. 后端架构设计 (`volur-competition`)
1. **依赖扩充**：
   - 在平台父 POM `src/volur-lite-platform/pom.xml` 中将 Apache POI (`org.apache.poi:poi-ooxml:5.2.5`) 纳入依赖版本管理；
   - 在 `volur-competition/pom.xml` 中引入 `poi-ooxml`。
2. **领域模型 (Domain VOs)**：
   - 收敛在 `com.humolix.volur.competition.domain.vo.export` 包下：
     - `CtfTeamRankExportVO`: CTF 战队榜单排名载体（排名、队伍ID、队伍名称、所属单位、队长、积分、解题数、最后解题时间）；
     - `CtfSolveDetailExportVO`: CTF 解题流水明细（解题ID、队伍名、赛题名、分类、解题者、积分、解题时间、一血标记）；
     - `CtfScoreExportVO`: CTF 全量导出聚合模型（元数据 + rankings + details）；
     - `TheoryCandidateScoreExportVO`: 理论知识赛考生总评载体（排名、队伍名、考生账号、姓名、交卷状态、正确题数、总题数、满分、最终得分、正确率、交卷时间）；
     - `TheoryAnswerDetailExportVO`: 理论赛每题答题流水明细（考生账号、姓名、队伍名、题号、题型、题干简述、满分、考生作答、标准答案、正误判定、单题得分）；
     - `TheoryScoreExportVO`: 理论赛全量导出聚合模型。
3. **格式生成引擎工具类**：
   - `CsvExportUtil`:
     - 显式写出 `UTF-8 BOM` (`\uFEFF`)，防止 Windows Excel 打开中文 CSV 时产生乱码；
     - 遵循 RFC 4180 标准对逗号、双引号、换行符进行转义；
     - 防范 CSV/Formula Injection（若首字符匹配 `=`, `+`, `-`, `@`, `\t`, `\r`，强制添加单引号前缀转义）；
     - 支持单段与多段平铺宽表输出。
   - `ExcelExportUtil`:
     - 基于 Apache POI `SXSSFWorkbook` 进行流式高性能写入，支持超大数据集且有效控制 JVM 堆内存开销；
     - 自动设置表头加粗、居中对齐、浅灰色背景与细边框网格线，并自适应列宽；
     - 支持多 Sheet 架构（Sheet 1 为总榜，Sheet 2 为流水明细），在 `finally` 块中严格释放临时文件与工作簿资源。
4. **服务层与控制器层 (Thin Controller & Services)**：
   - 控制器：`AdminCompetitionScoreExportController`（路径 `/api/v1/admin/competitions/{competitionId}/scores/export`，方法返回 `void` 直接向 `HttpServletResponse` 流式写出，标注 `@Syslog` 审计注解）；
   - 服务层：`AdminCompetitionScoreExportService` 与 `AdminCompetitionScoreExportServiceImpl`。

### 2. 前端架构设计 (`volur-lite-view-NEXT`)
1. **API 请求层**：
   - 在 `src/utils/request.ts` 中显式向外导出强类型 `download(url, config): Promise<Blob>` 函数；
   - 在 `src/api/competition.ts` 中封装 `exportAdminCompetitionScoresApi(competitionId, format, scope): Promise<Blob>`。
2. **管理端通用导出交互组件**：
   - 新增 `src/views/admin/competition/components/AdminCompetitionScoreExportModal.vue`；
   - 采用 Google Material Design 现代风格设计，提供卡片式单选文件格式（Excel .xlsx / CSV .csv / JSON .json）与包含范围（仅成绩总榜 / 含答题与解题流水）；
   - 具备加载状态反馈与 Blob 本地自触发下载机制。
3. **管理端与选手端视图集成**：
   - `AdminCtfCompetitionEditView.vue`: 顶部导航区集成【导出成绩】按钮并绑定模态框；
   - `AdminTheoryCompetitionEditView.vue`: 顶部导航区集成【导出成绩】按钮并绑定模态框；
   - `CtfCompetitionScoreboardView.vue`: 选手端榜单右上角在有数据时提供【导出榜单】按钮，支持一键下载公开名次 CSV。

---

## 三、 安全隐患与防护考量
1. **CSV 公式注入防御**：
   - 用户名或战队名称若被恶意填入以 `=cmd|' /C ...'` 或 `@SUM(...)` 等字符开头的恶意 payload，导出到 CSV 并被管理员在本地 Excel 打开时可能触发动态命令或外部链接请求。在 `CsvExportUtil.escapeCell` 中对首字符进行安全加引号隔离。
2. **审计与操作可溯源**：
   - 导出接口配置了 `@Syslog(module = "COMPETITION", value = "导出竞赛成绩报表", recordParams = true)`，记录管理员账号、目标竞赛 ID、导出时间及范围，防止越权批量导出无痕迹。
3. **资源占用防雪崩**：
   - Excel 导出采用 SXSSF 磁盘流式换页写入，避免一次性在 JVM 堆内存中生成大量 DOM 对象导致 OOM；
   - 响应头均设置编码后的 `Content-Disposition` 与 RFC 5987 `filename*` 标准。
