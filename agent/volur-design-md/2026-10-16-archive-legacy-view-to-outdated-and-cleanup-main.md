# 归档经典前端至 outdated 分支并清理主分支 (master) 工程文档

## 1. 变更背景与动因
随着新一代前端工程 `volur-lite-view-NEXT`（基于 Vue 3 + Tailwind CSS + Naive UI + Google Material / Workspace 色彩体系与自研组件库）全面研发、功能对齐并投入使用，旧版经典前端工程 `volur-lite-view` 已完成其历史使命。

为精简仓库结构、降低依赖包与 CI/CD 维护成本，并明确以 `volur-lite-view-NEXT` 作为单一主干前端真实源，需要对当前版本库执行分支结构治理：
1. 提交工作区内最新的后端与新一代前端核心改动；
2. 合并当前开发分支至主分支 `master`；
3. 从合并后的完整状态切出并保留 `outdated` 分支，用于长久归档和追溯经典版 `volur-lite-view` 代码资产；
4. 在 `master` 主分支彻底移除 `src/volur-lite-view` 及其全部依赖，实现主干轻量化与工程架构纯净化。

---

## 2. 改动点清单

### 2.1 本地提交与分支合并
- 在 `feat/auth-login-refactor` 分支完整提交容器配额持久化、理论赛结束态及环境变量精简重构；
- 切换至 `master` 主分支，合并最新开发提交，使其具备平台最新功能。

### 2.2 归档分支创建
- 基于合并后的完整状态创建并保留 `outdated` 分支，永久封存包含 `volur-lite-view` 的历史版本状态。

### 2.3 `master` 主分支清理
- 在 `master` 分支上彻底移除 `src/volur-lite-view` 目录及其下的全部源代码、配置文件与静态资产；
- 清理本地未纳入版本追踪的 `dist` 与 `node_modules` 缓存目录，确保工作树彻底干净。

---

## 3. 影响范围与迁移指引
- **主干工程结构**：`src/` 目录下仅保留后端核心服务平台 `volur-lite-platform` 与新一代前端 `volur-lite-view-NEXT`；
- **历史代码回溯**：任何针对旧版 `volur-lite-view` 视图或样式的历史追溯与查阅，均可通过 `git checkout outdated` 进行访问；
- **构建与部署**：后续 Docker 构建、前端打包及部署流水线统一以 `volur-lite-view-NEXT` 为基准。
