# CTF 竞赛 Writeup 提交流程与 VWriteupModal 模态框实现与重构方案

## 1. 变更背景与需求分析
在 CTF 竞技演练场景中，选手与参赛队伍在攻克赛题后需在规定窗口期内提交 Write Up（解题报告，简称 WP），以供裁判和组委会进行解题思路复核、防作弊审计及积分定级。此前系统前端侧边栏中仅展示了队伍基础得分、解题数与倒计时信息，缺少直观的 WP 提交触发入口与符合赛事规范的提交模态框交互。

本方案在 CTF 竞赛挑战主视图及相关统计组件基础上进行功能扩展与架构对齐：
1. 在 `VCtfCompetitionStatistic.vue` 组件底部新增提交 WP 按钮，采用规范封装的通用组件 `VBtn.vue`（`clz="normal"` 样式基线）；
2. 抽离并创建独立的 `VWriteupModal.vue` 模态框组件，严格基于 Flex 弹性盒模型进行分层组织；
3. 第一行呈现标题“提交 Write Up”；
4. 第二行呈现结构化提交说明（包含提交时间规范、单一 PDF 命名规则及队伍限次不超过 5 次与大文件分卷规范）；
5. 第三行呈现提交结果列表（包含序号、提交人、提交时间、上传 WriteUp 文件名称）；
6. 第四行呈现上传 Writeup 卡片（卡片采用 Flex 布局，卡片第一行为 Writeup 示例下载链接/按钮，第二行左侧为选择文件按钮与选定文件详情，右侧为图形安全验证码组件）；
7. 第五行呈现主操作提交按钮，采用 `VBtn.vue`（`clz="primary"` 样式基线）；
8. 后端在 `volur-competition` 模块中补充对齐选手侧的 `CompetitionWriteupController` 接口及服务层校验（5次限制拦截、验证码校核、队伍身份校验），并同步增量迁移脚本。

---

## 2. 核心架构设计与改动点

### 2.1 统计组件按钮与模态框集成 (`VCtfCompetitionStatistic.vue`)
- 在计时器区块下方引入 `VBtn` 按钮组件，声明 `clz="normal"` 并设置为 `block` 占满卡片宽度；
- 点击按钮触发模态框显示（`v-model:show="showWriteupModal"`）并对外广播 `submit-writeup` 交互事件；
- 模态框挂载并绑定当前竞赛上下文数据（`:competition="competition"`）及当前参赛队伍上下文数据（`:team="team"`）。

### 2.2 提交模态框组件开发 (`VWriteupModal.vue`)
- **模态框基座**：基于 Naive UI `n-modal` preset="card" 构建，无缝适配 Google Workspace 亮暗主题阶梯色系；
- **Flex 布局体系**：
  - **第一行**：模态框头部标题行，包含文档图标、`提交 Write Up` 文本与关闭操作按钮；
  - **第二行**：提交说明区，规范呈现三大核心规则：
    1. WriteUp提交时间规则；
    2. 提交形式（整理成单个 PDF 且命名为 `战队名称.pdf`）；
    3. 其他说明（队伍提交不超过 5 次，大于 100M 分卷规范，禁止特殊字符）；
  - **第三行**：提交结果列表，展示序号、提交人、提交时间、上传 WriteUp 名称，支持记录动态加载、空状态展示及文件超链接回显；
  - **第四行**：上传 Writeup 卡片，卡片内部采用 Flex 布局：
    - 卡片首行：`Writeup 示例下载` 动作，支持获取官方设定模板或兜底标准规范模板；
    - 卡片次行：左侧为 `选择文件` 按钮（受控触发隐藏 file input，仅允许 `.pdf` 格式并进行 100MB 阈值校验）与已选文件状态标签；右侧为图形验证码输入框及动态刷新验证码图片组件（集成 `GET /api/v1/captcha/challenge`）；
  - **第五行**：提交操作栏，右对齐放置 `VBtn`（`clz="primary"`），在提交中展示加载状态，并在队伍达到 5 次上限或必填项缺失时实施防护禁用。

### 2.3 前端 API 与类型定义补齐
- **类型层 (`src/types/competition/index.ts`)**：
  - `CompetitionWriteupVO`：定义提交记录视图结构（序号关联、文件名、地址、大小、提交人、时间等）；
  - `CompetitionWriteupSubmitDTO`：定义提交入参（文件名、地址、大小、验证码 ID 与验证码字符）；
  - `CompetitionWriteupTemplateVO`：定义官方模板元数据。
- **验证码 API (`src/api/captcha.ts`)**：新增 `getImageCaptchaApi`，直连后端字符图形验证码服务。
- **竞赛 API (`src/api/competition.ts`)**：新增 `getMyWriteupsApi`、`submitWriteupApi` 及 `getWriteupTemplateApi`，严格对齐后端 Controller 路由。

### 2.4 后端接口与服务层实现 (`volur-competition`)
- **依赖补充**：`volur-competition/pom.xml` 声明依赖 `volur-verification` 模块；
- **服务层 (`CompetitionWriteupService` & `CompetitionWriteupServiceImpl`)**：
  - `getMyWriteups`：基于当前登录用户判定归属队伍并检索 Writeup 列表；
  - `submitWriteup`：
    - 校验竞赛存在性与 `allowWriteupSubmission` 开关；
    - 校验用户参赛队伍身份；
    - 校验队伍累计提交次数（超过 5 次抛出 `BusinessException` 快速失败）；
    - 调用 `CaptchaService.verifyCaptcha` 校验图形验证码；
    - 持久化 `CompetitionWriteup` 实体并返回 VO。
  - `getTemplate`：提取竞赛配置中的模板地址与名称。
- **控制层 (`CompetitionWriteupController`)**：遵循 Thin Controller 原则，声明 `@RequestMapping("/api/v1/competitions/{competitionId}/writeups")`，向外暴露 `GET /my`、`POST /` 与 `GET /template`。
- **数据库增量迁移 (`scripts/upgrade/upgrade_to_latest.sql`)**：新增 `competition_writeup` 表幂等性检查与创建逻辑，严格遵循 Rule 9 规范。

---

## 3. 安全与合规性保障
1. **严格类型防伪与快速失败 (Fail-Fast & Zero-Mock)**：绝不伪造虚假提交成功或虚假记录，提交状态、文件与验证码全链路真实校验；
2. **纯净代码与零注释铁律**：所有修改及新增的 `.vue`、`.ts`、`.java` 代码均不包含任何行内解释性注释或废弃旧别名；
3. **色彩与无障碍规范**：严格遵循 Google Material Design 护眼阶梯，正文坚决不使用刺眼纯黑 `#000000`，亮色使用 Google Blue `#1a73e8`，暗色使用 `#8ab4f8` 与 `#292a2d` 表面。

---

## 4. 影响范围
- `src/volur-lite-view-NEXT/src/components/competition/ctf/VCtfCompetitionStatistic.vue`
- `src/volur-lite-view-NEXT/src/components/competition/ctf/VWriteupModal.vue`
- `src/volur-lite-view-NEXT/src/components/competition/ctf/index.ts`
- `src/volur-lite-view-NEXT/src/views/competition/components/index.ts`
- `src/volur-lite-view-NEXT/src/api/captcha.ts`
- `src/volur-lite-view-NEXT/src/api/competition.ts`
- `src/volur-lite-view-NEXT/src/types/competition/index.ts`
- `src/volur-lite-view-NEXT/src/locales/zh-CN/competition.json`
- `src/volur-lite-view-NEXT/src/locales/en-US/competition.json`
- `src/volur-lite-platform/volur-competition/pom.xml`
- `src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/controller/v1/CompetitionWriteupController.java`
- `src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/CompetitionWriteupService.java`
- `src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/impl/CompetitionWriteupServiceImpl.java`
- `src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/dto/CompetitionWriteupSubmitDTO.java`
- `src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/vo/CompetitionWriteupTemplateVO.java`
- `src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/repository/CompetitionWriteupRepository.java`
- `scripts/upgrade/upgrade_to_latest.sql`
