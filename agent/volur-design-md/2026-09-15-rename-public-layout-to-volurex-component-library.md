# 重命名 public/layout 为 volurEx 并确立 Volur 前端自研组件库规范 (2026-09-15)

## 1. 变更背景与设计动机
在平台早期的前端演进中，侧边栏导航组件被抽取并临时存放在 `src/volur-lite-view/src/public/layout` 目录下。随着 Volur 平台自主可控组件生态的发展，该目录的定位从单一的“布局抽取”正式升级为平台全局自研的基础组件库体系。

为了明确目录语义、避免与业务组件（`src/components`）发生混淆，并为未来更多平台级可复用组件资产提供统一宿主，本次重构将该目录正式重命名为 **`volurEx`**（路径：`src/volur-lite-view/src/public/volurEx`），并在项目的核心规则文件 [GEMINI.md](file:///Users/humolix/Documents/Dev/Volur-Lite/GEMINI.md) 与前端规范手册 [view-Rules.md](file:///Users/humolix/Documents/Dev/Volur-Lite/agent/volur-design-skills/view-Rules.md) 中正式确立其为 **Volur 自研前端组件库** 的规范与地位。

---

## 2. 核心架构与规范定义

### 2.1 组件库定位与目录结构
- **物理路径**：`src/volur-lite-view/src/public/volurEx`
- **模块别名**：`@/public/volurEx`
- **组件库架构**：
  ```
  src/volur-lite-view/src/public/volurEx/
  ├── components/                  # 组件源码实现与组件 Props 类型定义
  │   ├── VolurSidebar.vue         # 平台通用自适应侧边栏组件
  │   └── index.ts                 # 组件模块导出
  └── index.ts                     # 组件库全局统一聚合导出入口
  ```

### 2.2 规范约束
1. **职责单一与物理隔离**：
   - `src/components/`：负责存放依赖具体业务上下文的可复用组件（如 `auth/`、`user/`、`notification/` 等）；
   - `src/public/volurEx/`：专属于 Volur 平台自主设计与研发的通用 UI 基础组件库（如 `VolurSidebar`），不耦合特定业务 API，追求高度的独立性、通用性与自适应设计。
2. **统一聚合导出机制**：
   - 组件库内部通过各层级的 `index.ts` 聚合导出组件与 TypeScript 类型；
   - 外部业务视图与全局布局引用组件库资产时，统一采用 `@/public/volurEx` 进行聚合引入，杜绝跨级深层文件路径引用。

---

## 3. 改动点汇总

1. **物理目录与文件重命名**：
   - 将 `src/volur-lite-view/src/public/layout/` 重命名为 `src/volur-lite-view/src/public/volurEx/`；
   - 保持内部 `components/VolurSidebar.vue`、`components/index.ts`、`index.ts` 的结构与导出逻辑不变。
2. **应用布局引入更新**：
   - [AdminLayout.vue](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/layout/AdminLayout.vue)：更新引入语句为 `import { VolurSidebar } from '@/public/volurEx'`；
   - [AppLayout.vue](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/layout/AppLayout.vue)：更新引入语句为 `import { VolurSidebar } from '@/public/volurEx'`。
3. **协作根规范文档更新 ([GEMINI.md](file:///Users/humolix/Documents/Dev/Volur-Lite/GEMINI.md))**：
   - 在第 1.2 节第 2 条“目录结构镜像与模块化收敛”中补充自研组件库 `src/public/volurEx/` 的说明；
   - 新增第 1.2 节第 10 条“**Volur 前端自研组件库规范 (volurEx)**”，明确定义 `volurEx` 的职责、隔离原则及导入导出标准；
   - 在第 3 节“AI 行为指引”第 2 条中补充遵循 `volurEx` 自研组件库架构与规范的指令要求。
4. **前端详细规范手册更新 ([view-Rules.md](file:///Users/humolix/Documents/Dev/Volur-Lite/agent/volur-design-skills/view-Rules.md))**：
   - 在第 4.1 节标准目录树中补充 `public/volurEx/` 结构展示；
   - 新增第 4.2 节“平台自研前端组件库规范 (volurEx)”。

---

## 4. 影响与安全性评估

- **代码兼容性**：全量更新了平台中所有直接引用旧路径的布局组件，通过统一 `@/public/volurEx` 入口引入，解耦了深层内部路径依赖。
- **构建验证**：在 `src/volur-lite-view` 执行 `npm run build`（`vue-tsc --noEmit && vite build`）顺利通过，TypeScript 静态类型检查与 Vite 打包构建 0 错误、0 告警。
- **命名规范**：组件库命名 `volurEx` 简明贴切，既保留了 Volur 品牌标识，又体现了 Extension / Ecosystem 的可扩展组件库内涵。
