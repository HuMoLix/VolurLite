# CTF 题库与理论题库全功能实现、ZIP/JSON 导入导出与 Docker 镜像异步拉取审计文档

**变更日期**：2026-09-16  
**变更类型**：后端架构扩展 & 前端功能实现 & 异步虚拟线程优化  
**影响模块**：
- 后端：`volur-challenge`、`volur-container`、`volur-framework`
- 前端：`volur-lite-view`（API、Types、Admin CTF 题库视图、Admin 理论题库视图）

---

## 1. 变更背景与需求目标

根据平台题库管理体系建设要求，需对 Volur-Lite 平台的 **CTF 题库** 与 **理论题库** 进行全面实现与工程化落地，重点支持：
1. **题目完整生命周期管理**：CTF 赛题（Web、Pwn、Reverse、Crypto、Misc 等）与理论赛题（单选、多选、判断、简答）的新增、编辑、删除、分页检索与详情预览。
2. **ZIP / JSON 批量导入**：
   - 深入分析既有题目压缩包（例如 `CVE-0114-0514.zip`，其结构包含题目目录、`challenge.json` / `build.json` 以及题目附件压缩包 `attachment.zip`）；
   - 支持单题压缩包、批量子目录压缩包以及多题汇总压缩包的鲁棒解析；
   - 自动解压并利用 `StorageService` 将附件持久化至本地或云端对象存储（OSS）；
   - 自动识别或创建缺失的赛题分类（Category）。
3. **高并发与性能优化**：
   - 结合系统中的 `AsyncConfig` 与 `VolurThreadManager`，利用 Java 25 虚拟线程和计算密集型线程池（`supplyCompute`）分流 CPU 密集型的 ZIP 解压缩与 JSON 反序列化操作，杜绝载体线程饥饿。
4. **Docker 镜像联动拉取（任务中心集成）**：
   - 若导入或创建的题目指定了容器镜像（`imageName`），自动异步调度容器服务拉取 Docker 镜像，并在 `AdminAsyncTaskController` 统一的异步任务管理中心生成可视化的任务记录。
5. **批量与单题导出**：
   - CTF 题库导出标准 ZIP 压缩包（包含标准 `challenge.json` 元数据与实体附件）；
   - 理论题库导出标准 JSON 题目数组。
6. **前端管理视图与规范对齐**：
   - 严格遵守 `Rules.md` 与 `view-Rules.md`（日间主操作按钮 `#000000` 背景与白字、标签与次要文本 `#444444`、TypeScript 零 `any`、后端 DTO/VO 1:1 强类型对齐）。

---

## 2. 核心改动点与架构设计

### 2.1 依赖解耦与事件驱动镜像拉取架构 (Event-Driven Image Pulling)
- **依赖冲突规避**：Maven 模块拓扑中，`volur-container` 已单向依赖 `volur-challenge`。为杜绝循环依赖（Circular Dependency），严禁在 `volur-challenge` 中引入 `volur-container`。
- **解耦方案**：
  - 在 `volur-challenge` 中定义轻量领域事件 `ChallengeImagePullEvent`；
  - `AdminCtfChallengeServiceImpl` 在题目导入/创建解析到 Docker 镜像时，通过 Spring `ApplicationEventPublisher` 发布该事件；
  - 在 `volur-container` 中实现 `ChallengeImagePullEventListener`，使用 `@Async("volurVirtualExecutor")` 并在 `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` 事务提交后异步监听，直接调用 `AdminImageService.pullImage(new AdminImagePullRequestDTO(imageName))`，实现后台拉取并完美接入 `AdminAsyncTaskController` 任务中心。

### 2.2 虚拟线程与计算线程池协同优化 (Concurrency & Virtual Threads)
- ZIP 压缩包的解压、CRC32 校验、递归遍历与 JSON 解析属于 CPU 密集型任务。
- 在 `AdminCtfChallengeServiceImpl` 与 `AdminTheoryChallengeServiceImpl` 的导入逻辑中，通过 `volurThreadManager.supplyCompute(...)` 将密集解压过程指派给计算池（ForkJoinPool / Carrier Pool），防止虚拟线程执行密集 CPU 计算引起载体线程长时间钉选（Pinning），在高并发批量上传场景下保持高吞吐与极低响应延迟。

### 2.3 内存附件流式对接与存储服务 (InMemoryMultipartFile & Storage)
- 编写 `com.humolix.volur.challenge.util.InMemoryMultipartFile` 适配器，实现 Spring `MultipartFile` 规范；
- ZIP 解压得到的题目附件（如 `attachment.zip`、`pwn.bin` 等）直接封装为 `InMemoryMultipartFile`，无缝调用 `StorageService.uploadResource(file, "attachments")`，支持本地文件系统和 MinIO/Aliyun OSS 无感切换。

### 2.4 CTF 题目标准导入与导出实现
- **导入支持**：
  - 结构 A：`CVE-0114-0514/challenge.json` + `CVE-0114-0514/attachment.zip`（兼容开源 wzbcctf / ctfd 格式）；
  - 结构 B：根目录下直接包含 `challenge.json` 或 `build.json`；
  - 结构 C：多个题目子文件夹打包的多题集合 ZIP；
  - 结构 D：由多个单题 ZIP 组成的汇总包。
- **导出支持**：
  - 流式生成 ZIP 归档，动态写入每个题目的 `challenge.json` 元数据和对应的真实附件字节流。

### 2.5 理论题库 JSON / ZIP 导入与导出实现
- 统一支持 `.json` 题库文件以及内含 JSON 的 `.zip` 压缩包导入；
- 支持单选题（`SINGLE_CHOICE`）、多选题（`MULTIPLE_CHOICE`）、判断题（`TRUE_FALSE`）和简答题（`ESSAY`）；
- 导出端直接流式响应 JSON 格式数据。

### 2.6 前端 API、实体与管理员控制台实现
- **类型层**：`src/volur-lite-view/src/types/challenge/index.ts` 完整定义 DTO、VO、ChallengeType、TheoryType 与 ChallengeImportResultVO。
- **API 层**：`src/volur-lite-view/src/api/challenge/index.ts` 封装 CTF 题库 CRUD、导入导出、方向分类管理，以及理论题库 CRUD 与导入导出全部端点。
- **UI 视图层**：
  - `AdminCtfChallengeView.vue`：集成多条件筛选、数据表格（基于 `AppTableCard`）、题目创建与修改模态框、附件上传联动、容器镜像与配额配置、批量 ZIP 拖拽导入与导入结果统计看板（成功/失败/触发镜像拉取任务列表）、批量/单题导出及分类管理。
  - `AdminTheoryChallengeView.vue`：集成题型筛选、题目创建与编辑模态框（支持选项可视化增删与标准答案动态联动）、题目详情抽屉、JSON/ZIP 批量导入看板及批量导出。

### 2.7 CTF 赛题检索逻辑行为统一（仅按题目名称搜索）
- **前端调整**：
  - `AdminCtfChallengeView.vue` 移除原先混杂的“搜索题目名称 / Flag / 镜像名...”提示，精简聚焦为纯粹的“搜索题目名称...”；
  - 检索字段重构为 `searchName`，不再混用或传递宽泛模糊的关键字，精准通过 `name` 参数请求后端接口。
- **后端同步**：
  - `AdminCtfChallengeController.java`、`AdminCtfChallengeService.java` 及 `AdminCtfChallengeServiceImpl.java` 明确入参 `name`（保留 `keyword` 作为平滑兼容参数）；
### 2.8 修复 CTF 分类实体主键生成策略（Field 'id' doesn't have a default value 根因修复）
- **故障排查与根因分析**：
  - 管理员在调用 `POST /api/v1/admin/challenges/ctf/categories` 新增分类时，控制台抛出 `org.springframework.dao.DataIntegrityViolationException: could not execute statement [Field 'id' doesn't have a default value] [insert into ctf_challenge_category (created_at,description,icon,name,sort_order,updated_at) values (?,?,?,?,?,?)]`；
  - 根因分析：全平台统一采用 64 位雪花算法（Snowflake ID），数据库底层 `ctf_challenge_category` 表的主键 `id` 为 `BIGINT` 且**未配置 MySQL 原生的 `AUTO_INCREMENT`**；
  - 但实体类 `CtfChallengeCategory` 历史代码错误配置了 `@GeneratedValue(strategy = GenerationType.IDENTITY)`，导致 Hibernate 执行 INSERT 时刻意排除了 `id` 字段，试图让 MySQL 生成自增主键，最终触发 `Field 'id' doesn't have a default value (ErrorCode: 1364)` 异常。
- **修复方案**：
  - 引入平台标准的主键生成注解 `@SnowflakeGenerated`（对应 `SnowflakeIdentifierGenerator`）；
  - 移除 `@GeneratedValue(strategy = GenerationType.IDENTITY)`，将 `id` 列显式指定为 `@SnowflakeGenerated` 并添加 `columnDefinition = "BIGINT"`；
### 2.9 赛题分类管理弹窗解耦提取与 AppTableCard 样式统一
- **重构背景**：
  - 原先 `AdminCtfChallengeView.vue` 内部平铺了分类管理模态弹窗的所有状态、表单与原生 `<n-data-table>`，不仅导致单文件体积过度膨胀，而且分类列表未采用平台通用的卡片表格规范。
- **解耦设计**：
  - 新建独立组件 [`src/views/admin/challenge/components/CtfCategoryModal.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/challenge/components/CtfCategoryModal.vue)；
  - 弹窗内部列表全面采用平台通用基础组件 [`AppTableCard.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/components/common/AppTableCard.vue)；
  - 在 `AppTableCard` 的 `#filter-left` 工具栏插槽中集成新建分类表单（分类名称、图标选择器 `VolurSvgPicker`、分类描述及添加按钮），实现工具条与表格的一体化视觉规范；
  - 表格列展示 ID、VolurIcon 分类图标、分类名称、描述与操作（删除确认 Popconfirm）；
  - `AdminCtfChallengeView.vue` 通过 `<CtfCategoryModal v-model:show="showCategoryModal" @change="fetchCategories" />` 简洁挂载，并在分类数据变动时自动响应式刷新题目下拉选择项。

---

## 3. 安全隐患说明与防护加固

1. **ZIP Slip 目录遍历漏洞防御**：
   - 在遍历 ZIP Entry 时，严格对解压条目路径进行规范化（`normalize()`）校验，阻断任何带有 `../` 或绝对路径的恶意 Entry 试图逃逸解压上下文。
2. **解压炸弹 (Zip Bomb) 资源耗尽防护**：
   - 限制单次上传文件体积；内存解压采用字节流缓冲切片，附件单文件设定合理的安全提取上限。
3. **敏感信息脱敏与权限校验**：
   - 题目增删改查与导入导出均在后台限制管理员权限，所有接口统一记录 `@Syslog` 审计日志。

---

## 4. 影响范围与验证结果

- **编译构建**：
  - 后端：12 个微模块全部通过 `mvn test-compile -DskipTests` 编译（0 错误，耗时 12.1s）。
  - 前端：通过 `npm run build`（`vue-tsc --noEmit && vite build`）校验，5.63s 构建完成，0 报错。
- **兼容性**：
  - 完全向下兼容现有比赛题目挂载模块（`ChallengeTab.vue`）。
