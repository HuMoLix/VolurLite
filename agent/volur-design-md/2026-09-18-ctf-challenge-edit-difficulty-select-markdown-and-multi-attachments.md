# 2026-09-18 CTF 赛题编辑视图重构与多附件支持改造审计报告

---

## 1. 变更背景与需求目标

在 Volur Lite 平台的 CTF 题库管理中，原有的题目编辑页面（[`AdminCtfChallengeEditView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/challenge/AdminCtfChallengeEditView.vue)）与后端实现存在以下问题与提升空间：
1. **难度展示不够直观**：原难度等级使用星星打分组件（`n-rate`），存在视觉歧义，不够精确；需统一更换为带有规范等级标签的下拉选择框（`n-select`）；
2. **题目描述排版能力受限**：原题目描述采用简单多行文本域（`<n-input type="textarea">`），缺少实时富文本预览与 Markdown 语法快捷工具；需统一替换为平台自研组件库中的标准 Markdown 编辑器 [`VolurMarkdownEditor.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/components/VolurMarkdownEditor.vue)；
3. **单附件限制无法满足现代复杂题型**：原系统仅支持单文件附件（`attachmentUrl` / `attachmentName`），无法满足现代 CTF 赛题常需同时分发源码包、补丁文件、流量包（pcap）、靶机环境启动脚本、外部网盘超大镜像等多种附件的实际需求；
4. **前后端与数据协议未统一解耦**：后端实体与接口需扩展为支持多个附件（包含附件名称、下载链接及文件大小等元数据），同时保持对历史单一附件字段的平滑向下兼容，并保障导入/导出（`importChallenge` / `exportChallenges`）时附件资产的完整打包与流转。

---

## 2. 核心改动点与架构设计

### 2.1 后端领域模型与持久层升级 (`volur-challenge`)

1. **新增附件领域对象**：
   - [`CtfAttachmentVO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/domain/vo/ctf/CtfAttachmentVO.java)：包含 `name`、`url`、`size` 属性，符合平台 VO 规范（`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`）；
   - [`CtfAttachmentDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/domain/dto/ctf/CtfAttachmentDTO.java)：包含 `name`、`url`、`size` 属性，符合平台 DTO 规范（`@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`）；
2. **实体与数据库表扩展**：
   - 在 [`CtfChallenge.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/domain/entity/CtfChallenge.java) 中新增 `@Column(name = "attachments", columnDefinition = "TEXT") private String attachments;`，以 JSON 数组格式持久化多附件元数据；保留原有 `attachmentUrl` 与 `attachmentName` 列以提供向下兼容能力。
3. **请求与视图对象对齐**：
   - [`CtfChallengeCreateRequestDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/domain/dto/ctf/CtfChallengeCreateRequestDTO.java) & [`CtfChallengeUpdateRequestDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/domain/dto/ctf/CtfChallengeUpdateRequestDTO.java)：新增 `private List<CtfAttachmentDTO> attachments;`；
   - [`CtfChallengeDetailVO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/domain/vo/ctf/CtfChallengeDetailVO.java)：新增 `private List<CtfAttachmentVO> attachments;`。
4. **MapStruct 映射器对齐**：
   - 在 [`ChallengeMapper.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/domain/mapper/ChallengeMapper.java) 中增加 `@Mapping(target = "attachments", ignore = true)`，并通过 `@AfterMapping` 自动解析 `attachments` JSON 字符串；若无多附件 JSON 但存在历史 `attachmentUrl`，则平滑构造包含该单一附件的列表注入到 `CtfChallengeDetailVO`。

### 2.2 后端服务层与控制器扩展 (`volur-challenge`)

1. **附件上传专有接口**：
   - 在 [`AdminCtfChallengeController.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/controller/v1/admin/ctf/AdminCtfChallengeController.java) 中新增接口：
     `POST /api/v1/admin/challenges/ctf/attachments/upload`
   - 服务层实现 [`uploadAttachment`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/service/impl/AdminCtfChallengeServiceImpl.java)，基于存储层 `storageService.saveResource(file, "attachments")` 安全落盘并返回包含文件名、下载地址及文件字节大小的 `CtfAttachmentVO`。
2. **赛题创建与更新多附件处理**：
   - 在 `createChallenge` 与 `updateChallenge` 中序列化保存 `dto.getAttachments()` 至 `ctf_challenge.attachments`；
   - 同步提取首个附件的 URL 与名称填入 `attachmentUrl` 和 `attachmentName`，确保历史依赖该字段的组件与查询无需同步变更。
3. **导入导出功能的多附件资产完整性对齐**：
   - **导出 (`exportChallenges`)**：读取题目 `attachments` 列表中的所有附件，将物理存在的文件全量写入导出压缩包的 `attachments/` 目录下，并在 `challenge.json` 的配置中完整保留附件数组结构；
   - **导入 (`importChallenge`)**：不仅解析首个附件，而是遍历 ZIP 包中 `attachments/` 下的所有文件，统一保存至持久化存储并自动构建完整的多附件元数据列表。

### 2.3 前端 TypeScript 类型与 API 对齐 (`src/volur-lite-view`)

1. **类型定义层 (`src/types/challenge/index.ts`)**：
   - 导出 `CtfAttachmentVO` 与 `CtfAttachmentDTO`：`{ name: string; url: string; size?: number }`；
   - `CtfChallengeDetailVO`、`CtfChallengeCreateRequestDTO`、`CtfChallengeUpdateRequestDTO` 同步新增 `attachments?: CtfAttachmentVO[]` 字段。
2. **API 请求层 (`src/api/challenge/index.ts`)**：
   - 新增并导出 [`uploadAdminCtfAttachmentApi(formData: FormData): Promise<ApiResponse<CtfAttachmentVO>>`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/api/challenge/index.ts)，严格 1:1 对齐后端控制器的 RESTful URI。

### 2.4 前端编辑视图全功能升级 (`AdminCtfChallengeEditView.vue`)

1. **顶部导航栏与标题规范化**：
   - “返回CTF题库”按钮参考前台 `CompetitionDetailView.vue` 样式规范，采用纯文本按钮与无边框透明交互；
   - 标题参考 `AdminCompetitionEditView.vue` 格式规范，统一格式化为 `<赛题名称> - CTF题目管理`（或 `新增 CTF 赛题`），并彻底移除标题旁侧冗余的类型胶囊（如 `STATIC`）及重复的名称徽标。
2. **表单选项与标签精简对齐**：
   - 将“赛题模式”修改为“题目类型”，选项精简为“静态附件”（`STATIC`）与“动态容器”（`DYNAMIC`），彻底废除冗余的“动态容器/靶机（CONTAINER）”；
   - 将“静态 Flag”修改为“flag”；
   - 难度等级选择框纯净显示“入门”、“简单”、“中等”、“进阶”、“困难”等级字样，去除英文及多余胶囊装饰，界面更加凝练克制。
3. **Markdown 编辑器深度集成**：
   - 移除原有普通 textarea，引入平台自研 [`VolurMarkdownEditor`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/components/VolurMarkdownEditor.vue)；
   - 支持实时分屏编辑/预览无缝切换、加粗/斜体/代码块/引用等快捷工具栏，以及字符与行数实时统计。
4. **多附件管理器设计与交互**：
   - **卡片头部状态**：动态呈现已配置的附件总数徽标；
   - **本地文件上传**：基于 `n-upload` (`multiple: true`) 调用专有接口 `uploadAdminCtfAttachmentApi`，支持多文件同时批量上传并自动读取文件大小（Byte/KB/MB/GB 自动格式化）；
   - **外部网盘/URL 快速挂载**：提供附件名称 + 外部下载链接输入行，支持直接填入超大附件网盘链接或对象存储外链，回车或点击按钮即可添加；
   - **附件条目管理**：展示附件图标、名称、大小与地址链接，提供“查看/下载”新标签页打开与“移除”操作；
   - **空状态呈现**：无附件时友好提示指引。

### 2.5 标签回退、冗余提示清理与全页面国际化 (i18n)

1. **静态 Flag 标签复原**：
   - 根据需求将“flag”标签重新调整回“静态flag”（`static_flag: "静态flag"` / `Static Flag`），在界面与逻辑语义上更加精准。
2. **靶机镜像提示清理**：
   - 彻底移除了动态容器配置卡片中冗余的 `<n-alert>` 提示（“系统将基于此镜像按需为选手下发独立的动态靶机实例；镜像若在当前集群节点中未就绪，保存后将自动提交异步镜像拉取任务。”），界面整体更加紧凑、整洁，去除不必要的信息干扰。
3. **全页面多语言国际化 (i18n)**：
   - 在 [`src/locales/zh_CN/admin/challenges.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/zh_CN/admin/challenges.json) 与 [`src/locales/en_US/admin/challenges.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/en_US/admin/challenges.json) 中完整补充了 CTF 赛题编辑相关的全部中英文键值；
   - 包含：返回按钮、主副标题、保存与取消按钮、题目基本信息、题目类型选项（静态附件 / 动态容器）、难度等级（入门 / 简单 / 中等 / 进阶 / 困难）、题目描述与 Markdown 编辑器占位符、多附件卡片与外部挂载控件、动态容器靶机配置、表单验证规则错误提示、网络加载及异步操作结果提示等；
   - 模板内文本、属性（label、placeholder 等）、选项列表（`difficultyOptions`、`typeOptions`）以及校验规则（`formRules`）全面采用 `computed(() => [...])` 与 `t('admin.challenges.*')` 响应式绑定，确保语言切换时全量文本即时刷新。

---

## 3. 验证结果与编译测试

### 3.1 后端工程全量编译
在 `/Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform` 目录下执行全量构建：
```bash
mvn clean compile -DskipTests
```
- **结果**：Reactor 全部 12 个模块（`volur-framework`, `volur-logging`, `volur-system`, `volur-user`, `volur-verification`, `volur-notification`, `volur-auth`, `volur-challenge`, `volur-container`, `volur-competition`, `volur-server`）均编译成功，耗时 11.2s，**`BUILD SUCCESS`**，0 个错误。

### 3.2 前端工程打包编译
在 `/Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view` 目录下执行完整生产打包：
```bash
npm run build
```
- **结果**：Vite 成功转换模块，TypeScript 类型检查无任何报错，打包输出成功（`built in 5.54s`），`AdminCtfChallengeEditView` 成功构建入生产 bundle，0 个错误。

---

## 4. 影响范围与兼容性说明

1. **数据库向后兼容**：`CtfChallenge` 实体新增的 `attachments` 列为 TEXT 格式，Hibernate `ddl-auto: update` 可平滑就地扩展；历史未设置 `attachments` 的记录在读取时会自动由 MapStruct 从 `attachmentUrl` 与 `attachmentName` 构造单项附件列表，数据无损兼容。
2. **API 向后兼容**：原有题目详情 `attachmentUrl` 依然作为首个附件的主链接返回，调用方如不需要多附件无需做任何改动。
3. **导入导出兼容**：旧版仅含单文件的 challenge.json 导入时无缝兼容；新版导出的 challenge.json 包含 `attachments` 数组，重新导入时所有附件均完整解压并重新落盘。
4. **国际化支持**：中英双语下均可完整无死角渲染，所有动态计算属性与表单验证即时响应多语言切换。

