# 前端 ES2026 标准升级与 Pinia 全局状态管理规范施工记录

**施工日期**：2026-08-26  
**施工模块**：`src/volur-lite-view`、`GEMINI.md`、`agent/volur-design-skills/view-Rules.md`  
**责任主体**：Antigravity AI Agent

---

## 1. 变更背景与需求概述

为了保持前端架构的前瞻性与现代化，本次施工执行以下两项核心任务：
1. **ES2026 语言标准强制升级**：将原工程配置中的 `ES2020` 全面升级为 **`ES2026`（ECMAScript 2026 Standard / ESNext）**，并在 `view-Rules.md` 与 `GEMINI.md` 中增加强制使用 ES2026 的设计规范。
2. **Pinia 全局状态管理架构收敛**：规范化引入 Pinia 作为全局状态管理器，全面推行 Composition API Setup Store 语法风格，杜绝 Options Store 陈旧写法，并建立分模块（`user`, `app`, `notification`, `competition`）的状态收敛机制。

---

## 2. 核心改动点与实施细节

### 2.1 编译与工程目标升级为 ES2026
- **`tsconfig.json`**：
  - `"target": "ESNext"`（映射 ES2026+ 语言标准）；
  - `"lib": ["ESNext", "DOM", "DOM.Iterable"]`；
  - 开启最新原生特性支持（`Promise.withResolvers`, `Object.groupBy`, `Array.fromAsync`, Set 原生运算, `using` 声明等）。
- **`vite.config.ts`**：
  - `build.target: 'esnext'`；
  - `esbuild.target: 'esnext'`。

### 2.2 Pinia 全局状态管理架构完善
- **模块化 Setup Store 编写**：
  - `src/store/modules/user.ts`：用户登录、凭据存取、资料更新与角色判定；
  - `src/store/modules/app.ts`：深色/浅色主题切换与侧边栏状态控制；
  - `src/store/modules/notification.ts`：实时未读通知与广播队列；
  - `src/store/modules/competition.ts`：比赛详情、实时记分板与比赛状态维护；
  - `src/store/index.ts`：Pinia 根实例创建与分模块聚合导出。

### 2.3 规范文档更新与强化
- **`agent/volur-design-skills/view-Rules.md`**：
  - 新增 **第 2 节「ECMAScript 2026 (ES2026) 语言与编译规范」**：明确工程基准配置与现代 ES2026 语法应用指引；
  - 新增 **第 3 节「Pinia 全局状态管理规范」**：详尽规定统一采用 Setup Store 组合式语法，禁止 Options API，提供反模式 vs 推荐实践代码对比；
  - 更新合规检查清单。
- **`GEMINI.md`**：
  - 在前端规范准则中明确强调 **ES2026 语言基准** 与 **Pinia Setup Store 规范**。

---

## 3. 验证与构建测试

在 `src/volur-lite-view` 目录下执行全量构建验证：
```powershell
npm run build
```
**验证结果**：
- `vue-tsc --noEmit`：0 错误，严格模式类型检查通过；
- `vite build`：生产环境打包成功，耗时 ~3.35s。

---

## 4. 影响范围与审计评估

- **代码影响范围**：
  - `src/volur-lite-view/tsconfig.json`
  - `src/volur-lite-view/vite.config.ts`
  - `src/volur-lite-view/src/store/`
  - `agent/volur-design-skills/view-Rules.md`
  - `GEMINI.md`
- **稳定性与规范性**：
  - 现代化 ES2026 目标消除不必要的向下兼容 polyfill 开销，提升代码执行效率与可维护性；
  - Pinia Setup Store 统一了状态响应模型，与 Vue 3 Composition API 保持高度一致。
