# Volur Lite 开发与 AI 协作规范指令

本文档为 Volur Lite 项目的根规范与 AI 协作指令文件。在本项目中进行代码生成、重构、审查或编写新功能时，必须对前后端工程规范进行明确分离并严格遵守：
- **后端工程（Java / Spring Boot / Platform）**：必须严格遵守 [`agent/volur-design-skills/Rules.md`](file:///H:/HuMoLix/github/Volur-Lite/agent/volur-design-skills/Rules.md)；
- **经典前端工程（Vue 3 / TypeScript / `src/volur-lite-view`）**：必须严格遵守 [`agent/volur-design-skills/view-Rules.md`](file:///H:/HuMoLix/github/Volur-Lite/agent/volur-design-skills/view-Rules.md)；
- **新一代前端工程（Vue 3 / Tailwind CSS / Naive UI / `src/volur-lite-view-NEXT`）**：必须严格遵守 [`agent/volur-design-skills/view-NEXT-rules.md`](file:///H:/HuMoLix/github/Volur-Lite/agent/volur-design-skills/view-NEXT-rules.md)；
- **自动化脚本（Python / C / Go）**：必须严格遵守 [`agent/volur-design-scripts/Script-rules.md`](file:///H:/HuMoLix/github/Volur-Lite/agent/volur-design-scripts/Script-rules.md)。

---

## 1. 核心规范准则

### 1.1 后端架构与编码规范 (Backend: Java & Spring Boot)
> 详细规范与完整反模式对照详见 [**`Rules.md`**](file:///H:/HuMoLix/github/Volur-Lite/agent/volur-design-skills/Rules.md)。

1. **依赖注入与组件声明**：
   - **严禁字段注入**：禁止使用 `@Autowired` 进行成员变量注入。
   - **强制不可变声明**：所有注入依赖必须显式声明为 `private final`。
   - **统一使用 Lombok `@RequiredArgsConstructor`**：构造函数由 Lombok 自动生成，严禁手动编写依赖注入构造器。
2. **领域模型规范 (DTO / VO / PO / BO)**：
   - **统一存放在 `domain` 包路径下**：各业务模块领域模型收敛在 `com.humolix.volur.<module>.domain` 下，严禁在模块根路径平铺。
   - **DTO (请求入参)**：统一添加 `@Data`、`@NoArgsConstructor`、`@AllArgsConstructor`，无需 `@Builder`，严禁手写 Getter/Setter。
   - **VO (响应视图)**：统一添加 `@Data`、`@Builder`、`@NoArgsConstructor`、`@AllArgsConstructor`。默认初始值属性显式添加 `@Builder.Default`。
3. **控制器层与版本控制 (Thin Controller 原则)**：
   - 控制器存放于 `controller/v1/`，类级别统一注解 `@RequestMapping("/api/v1/{module-name}")`。
   - 统一返回 `ApiResponse<T>`，使用 `ApiResponseEntity` 工厂方法（`ok`, `success`, `created`, `badRequest` 等，强制小写驼峰）。
   - 无数据返回（`data == null`）时必须使用 `ApiResponseEntity.success(msg)` 返回 `ApiResponse<Void>`，严禁 `ok(msg, null)` 反模式。
   - 布尔判定分支响应强制使用三元表达式。
4. **Redis 缓存规范**：
   - 常量名称统一 `VREDIS_` + 模块大写 + 功能大写 + `KEY_PREFIX`。
   - 前缀值统一 `"vredis:<module>:<feature>:"`。
   - 过期时间强制使用 `java.time.Duration`。
5. **对象映射 (MapStruct)**：
   - 严禁手写转换方法与反射拷贝，统一使用 MapStruct 映射器接口。
6. **服务层签名与返回值**：
   - 业务动作/命令式方法强制返回 `void`；校验判定类方法强制返回基本类型 `boolean`（严禁返回包装类 `Boolean`）。
7. **分页响应模型**：
   - 严禁直接暴露 `Page<T>`，统一使用 `PageResponse<T>`（传统表格）、`CursorResponse<T>`（流式/无限滚动）或 `SliceResponse<T>`（无总数切片）。
8. **系统动态设置统一键名规范**：
   - 全系统统一强制采用小写点分层级键名（`system.<group>.<field_name>`），并在 `SystemSettingKeys` 常量中统一维护。
   - **彻底废除历史兼容扁平大写别名与双写机制**（杜绝 `MAINTENANCE_MODE`、`ENABLE_CAPTCHA` 等历史遗留反模式），保证前后端、数据库与多级缓存的单一真实数据源。
9. **MySQL 实体与数据表增量平滑迁移规范**：
   - 当每次对领域持久化实体（Entity / PO）进行调整，或任何涉及 MySQL 数据表结构变更的操作（如新建表、增删改字段、字段类型/非空约束调整、变更索引等）时，**必须同步修改/更新增量平滑升级迁移脚本 [`scripts/upgrade/upgrade_to_latest.sql`](file:///Users/humolix/Documents/Dev/Volur-Lite/scripts/upgrade/upgrade_to_latest.sql)**。
   - 迁移脚本必须严格具备**幂等性（Idempotent）**设计：统一采用存储过程并结合 `information_schema` 字典进行存在性探测（`IF NOT EXISTS` / `IF EXISTS`），支持在远程生产环境中重复安全执行，杜绝因重复执行或严格模式导致报错阻断。
   - 涉及字段语义重命名或拆分时，必须在迁移脚本中编写安全的无损数据回填（`UPDATE`）清洗逻辑，确保生产已有数据资产完整平滑过渡。

---

### 1.2 经典前端架构与编码规范 (Legacy Frontend: volur-lite-view)
> 详细规范与完整反模式对照详见 [**`view-Rules.md`**](file:///H:/HuMoLix/github/Volur-Lite/agent/volur-design-skills/view-Rules.md)。

1. **基础技术栈与语言标准基准**：
   - 强制以 **ECMAScript 2026 (ES2026)** 为语言基准，基于 **Vue 3 + Vite + TypeScript (target: ESNext) + SCSS** 架构，全面采用 `<script setup lang="ts">` 与 Composition API。
   - 组件库统一使用 **Naive UI**，字体库统一使用 **`vfonts`**（在 `main.ts` 中显式导入 `Lato.css`、`FiraCode.css` 等）。
   - 全局状态管理统一采用 **Pinia Setup Store 语法风格**，彻底杜绝 Options Store。
2. **目录结构镜像与模块化收敛**：
   - 前端目录全面参考后端 `src/volur-lite-platform` 模块化结构：
   - API 请求层 `src/api/{module}/` 与后端 `volur-{module}` 控制器 1:1 对齐；
   - 实体定义层 `src/types/{module}/` 与后端 `volur-{module}/domain` 领域模型 1:1 对齐；
   - 状态管理层 `src/store/modules/{module}.ts` 按业务领域进行解耦与收敛；
   - **平台自研前端组件库 (`src/public/volurEx/`，别名 `@/public/volurEx`)**：明确为 Volur 的自研前端组件库（命名为 **`volurEx`**），集中收敛平台级高复用、高度定制的通用 UI 与交互组件（如平台级通用侧边栏 `VolurSidebar` 等），由其根目录 `index.ts` 统一向外导出。
3. **API 请求路径与 HTTP 动词严格 1:1 对齐 Controller**：
   - 前端所有 API 请求函数的 HTTP Method 与 URI 路径，**必须 100% 严格与后端对应 Controller 类级别 `@RequestMapping` 与方法级别的路由注解（`@GetMapping`、`@PostMapping` 等）声明对齐**，严禁凭空臆测不存在的动作子路径（如 `/competition/list` 等反模式）。
4. **TS API 实体与后端 API 实体严格对齐**：
   - **请求入参实体 (`*DTO`)**：严格映射后端 `domain.dto.*DTO` 字段名与类型（小驼峰命名）。
   - **响应视图实体 (`*VO`)**：严格映射后端 `domain.vo.*VO` 字段名与结构。
   - **通用响应模型**：全局统一使用 `@/types/common/api.ts` 定义的 `ApiResponse<T>`、`PageResponse<T>`、`CursorResponse<T>`、`SliceResponse<T>`。
   - **杜绝 `any`**：严禁在接口入参和返回值中使用 `any` 类型；雪花算法 ID 统一使用 `number | string` 或 `string` 声明，防止精度丢失。
5. **网络请求与状态管理**：
   - 统一封装强类型 Axios 请求实例（`src/utils/request.ts`），支持 JWT 鉴权头注入、401 自动无感 Refresh Token 队列与统一 Toast 提示。
   - 实时推送统一采用 `src/utils/sse.ts` 封装的 `SseClient`（Server-Sent Events）。
6. **Naive UI 与样式规范**：
   - `App.vue` 统一组织 `NConfigProvider` -> `NLoadingBarProvider` -> `NDialogProvider` -> `NNotificationProvider` -> `NMessageProvider` Provider 体系。
   - SCSS 全局变量与 Mixins 通过 Vite 自动注入，组件内部采用 `<style lang="scss" scoped>` 保持样式隔离。
7. **Volur 核心视觉设计铁律**：
   - **主要操作按钮**：亮色模式下背景必须为 `#000000`，字体颜色必须为 `#ffffff`；
   - **描述文本颜色**：绝大多数描述文本、表单标签及副标题颜色统一为 `#444`（`#444444`），严禁使用纯黑 `#000000`；
   - **全局控制器定位**：中英文多语言切换与夜间/亮色主题切换按钮必须固定在页面视口左下角（`bottom: 20px; left: 20px; z-index: 1000;`）。
8. **表单校验与后端 DTO 约束严格 1:1 对齐**：
   - 前端表单校验规则（Naive UI `FormRules` 必填、长度范围、密码复杂度正则表达式、邮箱等）**必须 100% 严格参考后端对应 DTO 中的 Jakarta Validation 注解（如 `@NotBlank`, `@Size`, `@Pattern`, `@Email`）**，严禁前端规则松弛或与后端规则冲突。
9. **系统动态设置层级键名规范与防抖**：
   - 前端所有读取与更新系统配置的逻辑统一采用层级规范 Key（如 `system.maintenance.enabled`），杜绝大写下划线历史键；`fetchPublicSettings` 必须具备在途 Promise 复用与去重机制。
10. **Volur 前端自研组件库规范 (volurEx)**：
    - `src/volur-lite-view/src/public/volurEx`（别名 `@/public/volurEx`）为 Volur 平台自主研发的前端组件库（命名为 **`volurEx`**）。
    - 该目录收敛平台自主设计、高内聚、高复用度的平台级通用 UI（`components/`）、全平台布局框架（`layouts/`）以及全局主题体系（`styles/`），与普通业务功能组件（`src/components/`）进行物理和职责隔离。
    - **主题色彩铁律**：日间模式严格保持当前平台视觉规范（纯黑 `#000000` 主要按钮、`#444444` 描述与标签文本、浅灰底色）；黑夜模式全面重构为深曜炭黑（Obsidian & Cyber Slate）沉浸式阶梯体系，搭配高对比反色纯白按钮。
    - 组件库内部遵循清晰的模块解耦架构，并通过根入口 `index.ts` 集中向外导出；业务视图与路由统一从 `@/public/volurEx` 引入。

---

### 1.3 新一代前端架构与编码规范 (Next-gen Frontend: volur-lite-view-NEXT)
> 详细规范与完整色彩体系、路由对照详见 [**`view-NEXT-rules.md`**](file:///H:/HuMoLix/github/Volur-Lite/agent/volur-design-skills/view-NEXT-rules.md)。

1. **工程基准与技术栈**：
   - 强制以 **ECMAScript 2026 (ES2026)** 为语言基准，基于 **Vue 3 + Vite + TypeScript (target: ESNext) + SCSS + Tailwind CSS + Naive UI** 现代全技术栈构建。
   - 全局状态管理统一采用 **Pinia Setup Store 语法风格**，彻底杜绝 Options Store；字体库统一使用 `vfonts`。
2. **Google 风格主题色彩铁律 (Google Material Design & Workspace)**：
   - **全面对齐 Google 色彩体系**：主品牌色统一采用 **Google Blue**（亮色 `#1a73e8` / 暗色 `#8ab4f8`），语义辅助色统一采用 **Google Red** (`#ea4335` / `#f28b82`)、**Google Green** (`#34a853` / `#81c995`)、**Google Yellow** (`#fbbc04` / `#fdd663`)、**Google Cyan** (`#24c1e0` / `#78d9ec`)；
   - **表面与背景阶梯**：亮色采用 Google 护眼浅灰底色 `#f8f9fa`，纯白卡片 `#ffffff`；暗色采用 Google Workspace 阶梯灰黑（基底 `#202124`、卡片 `#292a2d`、浮层 `#303134`、细微边框 `#3c4043`）；
   - **文本颜色铁律**：严禁在正文中使用刺眼的全黑 `#000000`；所有描述文本与表单标签统一采用 Google Gray 体系（`#3c4043` / `#5f6368`，暗色 `#bdc1c6`）；主要按钮在亮色下采用 Google Blue `#1a73e8` 白字，暗色下采用反色高对比 `#8ab4f8`。
3. **前端路由架构与按需版本控制 (Router Architecture & On-demand Versioning)**：
   - 前端路由采用按需渐进式版本控制（**并不强制所有业务路由都具备版本前缀**，通用公开业务路由如 `/competitions` 可采用常规语义路径，涉及特定认证流或演进模块如 `/v1/signin/identifier` 则按指令要求添加）；
   - 根路径 `/` 默认重定向至公开业务入口（如 `/competitions` 赛事大厅）；
   - 路由守卫必须严格根据路由元信息中显式配置的 `requiredAuth`（或兼容 `requiresAuth`）判定是否引导拦截至登录页面。
4. **Tailwind CSS 与 Naive UI 协同防护**：
   - `tailwind.config.ts` 强制配置 `corePlugins: { preflight: false }`，杜绝 Tailwind 默认 reset 破坏 Naive UI 按钮与输入框外观；
   - `index.html` 强制包含 `<meta name="naive-ui-style" />` 占位符；Tailwind 负责原子化排版栅格与间距，Naive UI 负责受控高交互组件。

---

### 1.4 Git 提交规范与版本控制 (Git Commit Specification)
代码仓库统一遵循 **Angular / Conventional Commits** 提交信息规范。单次提交保持原子性，信息描述直击变更核心：

1. **提交标准格式**：
   ```
   <type>(<scope>): <subject>
   ```
2. **Type 类别语义定义（强制小写）**：
   - **`feat`**：新增功能 (Feature)，如新 API、新前端组件/视图或全新业务流程；
   - **`fix`**：缺陷修复 (Bug Fix)，如修复接口 500 异常、外键约束冲突、样式溢出等；
   - **`refactor`**：代码重构 (Refactoring)，既不新增功能也不修复 Bug 的代码结构与抽象优化；
   - **`comment` / `docs`**：注释与文档变更 (Documentation / Comments)，包括 `GEMINI.md`、`agent/volur-design-md` 施工记录、JavaDoc / TSDoc 等；
   - **`style`**：代码格式变动（空格、标点、缩进等，不影响程序语义）；
   - **`test`**：测试用例的新增、补齐或维护（单元测试、Mock 测试等）；
   - **`perf`**：性能提升（SQL 查询优化、缓存策略调优、高并发与虚拟线程调优等）；
   - **`chore`**：日常维护、工程构建配置、依赖升级与运维脚本变动。
3. **Scope 影响范围标准**：
   - 后端模块：`competition`、`auth`、`system`、`challenge`、`user`、`verification`、`notification`、`container`、`framework`、`logging`；
   - 前端模块：`view`、`view-NEXT`、`volurEx`、`router`、`store`、`locale`；
   - 数据库与运维：`scripts`、`db`；
   - 规范与审计：`agent`、`docs`。
4. **Subject 规范**：
   - 精炼直击核心，句尾严禁使用句号。

---

## 2. 详细规范说明导航

关于更详尽的设计约束、反模式对照与正确代码范例，请参阅：
- [**Volur Lite 后端详细编码与设计规范 (Rules.md)**](file:///H:/HuMoLix/github/Volur-Lite/agent/volur-design-skills/Rules.md)
- [**Volur Lite 经典前端详细编码与设计规范 (view-Rules.md)**](file:///H:/HuMoLix/github/Volur-Lite/agent/volur-design-skills/view-Rules.md)
- [**Volur Lite NEXT 新一代前端详细编码与设计规范 (view-NEXT-rules.md)**](file:///H:/HuMoLix/github/Volur-Lite/agent/volur-design-skills/view-NEXT-rules.md)
- [**Volur Lite 自动化脚本编写规范 (Script-rules.md)**](file:///H:/HuMoLix/github/Volur-Lite/agent/volur-design-scripts/Script-rules.md)

---

## 3. AI 行为指引

1. **执行后端任务时**：自动遵循 `Rules.md` 规范，包括 Lombok 构造注入、DTO (`@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`)、VO (`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`)、MapStruct 映射器、Controller 路径版本化与标准分页 Record，绝不生成冗余样板代码。
2. **执行前端任务时**：
   - **经典前端工程（`src/volur-lite-view`）**：严格遵循 `view-Rules.md` 规范，包括 ES2026 语言标准、Pinia Setup Store 规范、Vue 3 Setup Script、TypeScript 严格类型对齐后端 DTO/VO、**API 请求路径与 Controller 路由 100% 严格对齐**、SCSS 样式隔离、Naive UI Provider 体系、**自研组件库 `volurEx` (`src/public/volurEx`) 架构与集中导出规范**、vfonts 字体导入与无感 Token 刷新机制，杜绝 `any` 泛滥；
   - **新一代前端工程（`src/volur-lite-view-NEXT`）**：**必须严格遵循 `view-NEXT-rules.md` 规范**，包括全面对齐 **Google Material Design / Workspace 色彩体系**（Google Blue `#1a73e8` / `#8ab4f8`、Google Gray 表面阶梯、禁止正文全黑 `#000`）、**前端 Router 路径强制版本控制**（如登录 `/v1/signin/identifier`、挑战 `/v1/signin/challenge`、注册 `/v1/signup` 等）、Tailwind CSS 与 Naive UI 无冲突协同（Preflight 隔离）、ES2026 严格规范与 Pinia Setup Store。
3. **重构彻底性铁律（严禁妥协保留与旧命名别名）**：
   - 要改东西就必须改得彻底、完整。对任何函数、类、变量、实体字段或模块进行重命名或结构调整时，必须全链路同步修改所有引用处与定义。
   - **严禁保留任何“兼容旧命名别名”（如 `export const oldName = newName`、废弃桥接或兼容层）**，坚决杜绝历史包袱残留，保持代码库的纯粹性与单一真实源。
4. **纯粹代码与零注释铁律（文档外置原则）**：
   - **严禁在任何代码文件（包括但不限于 `.ts`、`.vue`、`.java`、`.go`、`.py`、`.sql`、`.scss` 等）中书写解释性注释、设计思路说明或冗余行内备注**。代码必须凭借清晰的结构与精准的命名实现高度自解释；
   - **所有设计方案、变更背景、逻辑推导、技术选型理由及安全考量，必须统一、强制记录在 `agent/volur-design-md` 的 Markdown 方案文档中**，彻底保持代码文件的极致纯净与紧凑。
5. **开发记录与审计溯源要求**：当每次 Agent 对项目文件进行修改或重构完成之后，必须在 `agent/volur-design-md` 文件夹中使用 Markdown 形式记录完整的施工与修改文档，详细阐述变更背景、改动点、安全隐患修复说明及影响范围，以便开发者进行溯源分析和审计。
6. **自动化脚本与批量任务操作规范 (Scripting for Batch Operations)**：当且仅当需要 Agent 进行跨多文件的大批量、高重复性作业时，方可使用 Python、C 或 Go 编写自动化脚本。所有脚本必须统一存放在 `agent/volur-design-scripts` 文件夹中，且**必须严格遵守 [`agent/volur-design-scripts/Script-rules.md`](file:///H:/HuMoLix/github/Volur-Lite/agent/volur-design-scripts/Script-rules.md) 的约束要求**（仅供 Agent内部消费、杜绝冗余注释与文档字符串、禁止输出装饰性 UI/无用打印、输出直击核心）。严禁在针对性单文件任务中滥用脚本。
7. **实体与数据库变更强制同步迁移脚本**：当涉及新增/变更实体类（Entity）、新增/修改数据库表字段、表结构或索引约束时，必须在同一变更周期内同步修改 [`scripts/upgrade/upgrade_to_latest.sql`](file:///Users/humolix/Documents/Dev/Volur-Lite/scripts/upgrade/upgrade_to_latest.sql)，严格编写具备幂等性的增量 DDL 与数据清洗回填逻辑，严禁遗漏迁移脚本而直接提交实体变更。
8. **执行 Git 提交时**：严格遵循 Conventional Commits 提交规范（`feat`/`fix`/`refactor`/`comment`/`docs`/`test` 等），准确标识模块 Scope，提交信息精炼直接，严禁无意义的模糊描述。
