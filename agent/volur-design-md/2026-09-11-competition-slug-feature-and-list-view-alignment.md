# 竞赛公开大厅 Slug 短链接功能与前后端 API 行为全栈对齐审计说明

## 1. 变更背景与需求说明
在 Volur Lite 平台中，每场网络安全竞赛（一赛一制架构）均配备有具备全局唯一约束的人类可读短标识（Slug，如 `national-ctf-2026`、`awd-championship-2026`）。
此前前端公开端竞赛大厅（`CompetitionListView.vue`）存在如下待优化项：
1. **API 与行为未完全对齐**：列表仅拉取固定数量，未对接后端 `GET /api/v1/competitions` 的分页机制（`page`, `size`）和关键字检索（`keyword`）；领域模型中 `participantCount` 引用了后端未暴露的统计字段；
2. **Slug 短链接功能缺失**：
   - 赛事卡片中未直观呈现短链接标识；
   - 缺少一键复制短链接（Copy to Clipboard）功能；
   - 缺少根据短链接直接访问与路由解析机制（例如通过 `/c/:slug` 或 `/competitions/:slug` 直接打开比赛主页或赛事详情）；
   - 后端关键字搜索仅匹配了 `title`，导致按短链接搜索时无法命中对应竞赛。

为此，本次施工对前端 [`CompetitionListView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/CompetitionListView.vue)、路由配置、国际化字典、领域模型以及后端检索逻辑进行了系统化重构与对齐。

---

## 2. 核心改动点

### 2.1 后端查询能力升级 (`volur-competition`)
1. **公开端列表检索支持 Slug (`CompetitionServiceImpl.java`)**：
   - 将 `keyword` 检索条件升级为复合匹配：
     ```java
     String kw = "%" + keyword.trim() + "%";
     predicates.add(cb.or(
         cb.like(root.get("title"), kw),
         cb.like(root.get("slug"), kw)
     ));
     ```
   - 用户在搜索框中无论是输入中文竞赛标题，还是输入英文字母短标识（Slug），均可被模糊命中。
2. **管理端列表检索同步支持 Slug (`AdminCompetitionServiceImpl.java`)**：
   - 同步升级管理端检索 Predicate，满足占位提示中“搜索竞赛标题或短链接...”的预期行为。

### 2.2 前端领域模型与路由升级 (`volur-lite-view`)
1. **领域模型对齐 (`src/types/competition/index.ts`)**：
   - `CompetitionSummaryVO`：`participantCount` 标记为可选（`participantCount?: number`），避免前端运行时访问产生未定义异常；
   - `CompetitionDetailVO`：1:1 补齐与后端 Java 实体对齐的展开属性（`maxTeamMembers`, `minTeamMembers`, `requireTeamAudit`, `allowWriteupSubmission`, `registrationFields`, `teamCount`, `createdAt` 等）。
2. **Vue Router 路由挂载 (`src/router/routes.ts`)**：
   - 新增 `/c/:slug`（`CompetitionShortLink`）与 `/competitions/:slug`（`CompetitionDetailBySlug`）路由，指向 `CompetitionListView.vue`；
   - 无论用户通过根路径短链 `/c/xxx` 访问，还是带前缀 `/competitions/xxx` 访问，均可被优雅拦截与解析。

### 2.3 公开端竞赛大厅重构 (`CompetitionListView.vue`)
1. **搜索栏与分页联动**：
   - 顶部增加响应式搜索输入框（`n-input` 搭配 `SearchOutline`），支持回车与清空实时检索；
   - 绑定后端 `PageResponse`，支持分页器（`n-pagination`）翻页加载。
2. **卡片 Slug 胶囊与一键复制**：
   - 卡片徽章行增加等宽字体设计的短链接药丸（`.slug-pill`），直观显示 `/c/{{ comp.slug }}`；
   - 点击药丸或卡片下方的「复制短链接」按钮，自动将 `${window.location.origin}/c/${slug}` 写入系统剪贴板，并触发成功 Toast 提示；
   - 兼容 `navigator.clipboard` 与隐藏 `textarea` 回退复制方案。
3. **赛事大厅抽屉（Competition Lobby Drawer）与短链直达**：
   - 点击卡片或操作栏「进入竞赛大厅」，唤出右侧详情大厅抽屉；
   - 抽屉内置海报 Banner、短链接展示与一键复制区域、单一赛制特性指引（`getTypeGuide`）、赛程时间跨度、队伍人数限制与审核规则说明；
   - 路由监听器 `watch(() => route.params.slug)`：当选手直接通过短链接访问（如浏览器输入 `/c/national-ctf-2026`）或刷新页面时，页面自动调用 `getCompetitionBySlugApi` 精准解析赛事并自动弹出赛事大厅抽屉；
   - 关闭抽屉时自动平滑恢复 URL 至 `/competitions`。
4. **严格遵循设计规范**：
   - 遵循 `GEMINI.md` 与 `view-Rules.md` 铁律：
     - 主操作按钮采用 `#000000` / `#ffffff`；
     - 辅助说明与标签文字采用 `#444444`；
     - 彻底杜绝 `any` 类型。

### 2.4 国际化多语言对齐 (`locales/zh_CN` & `locales/en_US`)
- 补全中英文字典中的搜索占位符、短链接标签、复制成功/失败提示、大厅标题、队伍规则说明等所有词条。

---

## 3. 验证与构建结果

1. **后端编译与自动化测试**：
   - 执行 `mvn compile -pl volur-competition`：0 错误编译通过。
   - 执行 `mvn test -pl volur-competition`：单元测试与 MockMvc 测试执行完毕，**BUILD SUCCESS**（0 Failures, 0 Errors）。
2. **前端类型检查与打包构建**：
   - 执行 `npm run build`（`vue-tsc --noEmit && vite build`）：
     - TypeScript 静态类型推断 100% 通过；
     - Vite 打包顺利生成 `dist/assets/CompetitionListView-isB0-4E0.js`，构建成功，用时 4.20s。

---

## 4. 影响范围与审计结论
- **影响范围**：`CompetitionListView.vue`、`routes.ts`、`competition.json`、`src/types/competition/`、`CompetitionServiceImpl.java`、`AdminCompetitionServiceImpl.java`。
- **审计结论**：全面打通了从短链接生成、展示、一键分享、路由直达到后端数据匹配的完整业务闭环，前后端 API 契约与行为实现严格统一。
