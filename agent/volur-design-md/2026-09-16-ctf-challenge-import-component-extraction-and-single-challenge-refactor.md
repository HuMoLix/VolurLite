# CTF 赛题导入组件化抽离、取消批量导入与标准模版下载功能施工文档

## 1. 变更背景
在原本的管理端 CTF 赛题管理视图（`AdminCtfChallengeView.vue`）中，赛题导入模态框与导入业务状态混杂在主视图中，导致主视图代码臃肿、职责未解耦。同时，旧版本支持多赛题聚合压缩包及嵌套 Zip 的批量导入，导致文件解压处理复杂度过高、部分元数据冲突难以定位。为了规范化题目导入流程、提升用户体验与系统可维护性，需对该功能进行重构：
1. 将题目导入功能从 `AdminCtfChallengeView.vue` 完整抽离为独立的单文件组件（`CtfImportModal.vue`）；
2. 取消批量导入逻辑，前后端统一调整为单赛题包 ZIP 导入模式；
3. 新增标准题目包模版下载端点及前端下载按钮，为命题人提供标准格式样例（包含标准 `challenge.json` 及示例附件）；
4. 移除旧界面中的“导入说明与规范”冗余横幅及字样，保持操作界面干净聚焦。

---

## 2. 核心改动点

### 2.1 后端工程改造 (Backend: `volur-challenge`)
1. **接口定义 (`AdminCtfChallengeService.java`)**：
   - 将批量导入接口方法更新为 `ChallengeImportResultVO importChallenge(MultipartFile file, Long authorId)`，并保留 `importChallenges` 作为默认兼容方法；
   - 新增 `void downloadTemplate(HttpServletResponse response)` 抽象方法，支持流式下载官方标准题目包 ZIP 模版。
2. **接口实现 (`AdminCtfChallengeServiceImpl.java`)**：
   - **取消批量导入限制**：移除了针对多嵌套 ZIP 的解包与批量导入机制；增加包内题目数量严格校验，若扫描到多个元数据 JSON（`jsonEntryKeys.size() > 1`）或纯嵌套 ZIP，主动抛出业务异常拒绝导入，引导管理员按单题上传；
   - **单题导入收敛**：解析唯一的题目元数据并落盘，返回精准的 `ChallengeImportResultVO`；
   - **标准模版流式生成 (`downloadTemplate`)**：动态生成包含标准化 `challenge.json`（涵盖题名、Markdown 描述、所属方向、分值、难度、Static/Dynamic 模式、Docker 容器配置与 Attachments 附件声明）及示例 `attachment.txt` 文件的 ZIP 压缩包，响应头设置 `Content-Disposition: attachment; filename="volur-ctf-challenge-template.zip"`。
3. **控制器层 (`AdminCtfChallengeController.java`)**：
   - 将路由 `POST /api/v1/admin/challenges/ctf/import` 的 Swagger 说明与方法签名调整为单题导入模式 `importChallenge`；
   - 新增 `GET /api/v1/admin/challenges/ctf/template` 路由，调用 `challengeService.downloadTemplate(response)` 实现模版下载。

### 2.2 前端 API 层改造 (`src/api/challenge/index.ts`)
1. 新增并导出 `importAdminCtfChallengeApi`（同时保留 `importAdminCtfChallengesApi` 别名确保兼容性）；
2. 新增并导出 `downloadAdminCtfTemplateApi`，使用 `request.download('/admin/challenges/ctf/template')` 流式获取模版二进制 Blob。

### 2.3 独立组件抽离 (`src/views/admin/challenge/components/CtfImportModal.vue`)
1. 新建 `CtfImportModal.vue` 组件，遵循 Vue 3 `<script setup lang="ts">` 与 Naive UI 规范：
   - 接收 `show` 属性与 `v-model:show`，成功时触发 `success` 事件通知父组件刷新题库与分类；
   - 窗口标题改为“导入 CTF 赛题 (ZIP)”；
   - 顶部工具栏集成“下载标准题目包模版”按钮，带流式下载反馈及加载状态；
   - 彻底删除原有的“导入说明与规范”横幅与字样；
   - 拖拽上传区更新为针对单题目包的引导提示；
   - 导入结果视图调整为单题结果卡片（成功/失败状态与 Docker 镜像后台拉取任务标签反馈）。
2. 主按钮样式严格遵守 Volur 视觉规范，亮色模式下黑色背景 `#000000` 与白色文字 `#ffffff`（`.theme-primary-btn`），描述文字保持 `#444444`。

### 2.4 主页面简化 (`AdminCtfChallengeView.vue`)
1. 移除内联的导入模态框代码（~100 行模板代码及 ~150 行 SCSS 样式）；
2. 引入 `<CtfImportModal v-model:show="showImportModal" @success="handleImportSuccess" />`；
3. 清理主页面中未使用的变量（`importing`、`importResult`）、函数（`handleCustomImportRequest`、`resetImportModal`）及废弃的图标引用（`CloseCircleOutline`）。

---

## 3. 安全隐患与健壮性说明
1. **ZIP 路径穿越防范**：在解压读取 ZIP 条目时，使用 `entry.getName().replace('\\', '/')` 并结合 `shouldIgnoreEntry` 过滤非法与系统隐蔽文件（如 `__MACOSX/`、`.DS_Store`、`._*` 等）；附件路径写入统一通过 `Paths.get(pathInJson).getFileName().toString()` 截取纯文件名，防止恶意构造的 `../` 路径破坏服务器文件系统。
2. **多题包主动熔断拦截**：取消批量导入后，服务端在检测到压缩包包含多个题目定义或嵌套 ZIP 时即刻终止并返回明确提示，避免误写、脏数据或高并发解压对内存的过度占用。
3. **资源流与临时对象释放**：流式下载使用 `ZipOutputStream` 自动关闭机制，前端使用 `window.URL.revokeObjectURL(url)` 及时释放 Blob 占用的浏览器内存。

---

## 4. 影响范围
- **受影响模块**：
  - 前端：CTF 管理员赛题界面（`AdminCtfChallengeView.vue`）、CTF 题目 API 模块（`api/challenge/index.ts`）；
  - 后端：`volur-challenge` 模块中的 CTF 赛题管理服务与控制器。
- **非影响模块**：普通选手端 CTF 赛题展示与答题、理论题导入导出逻辑、比赛环境与容器调度等功能均保持隔离，不受任何影响。

---

## 5. 编译与构建验证
1. **后端编译**：
   - 执行 `mvn clean test-compile -pl volur-challenge` 编译通过，状态为 `BUILD SUCCESS`；
   - 执行 `mvn compile -DskipTests` 全平台 12 个模块反应器编译通过，状态为 `BUILD SUCCESS`。
2. **前端类型检查与打包**：
   - 执行 `npx vue-tsc --noEmit` 严格类型检查通过（退出码 0）；
   - 执行 `npm run build` Vite 生产打包通过，各页面 chunk 生成正常。
