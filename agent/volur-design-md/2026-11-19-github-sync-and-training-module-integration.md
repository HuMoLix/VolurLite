# GitHub 远端流转合流与全平台架构统一交付实施记录

## 1. 变更背景与目标

此前平台针对敏捷重构与代码交付体系制定了以下关键设计：
1. **统一双源仓库（SSOT 原则）**：废弃依赖外围 `scripts/git/github.sh` 构建孤立临时分支（Orphan Branch）强行剥离资产的旧推送流程，强化根目录 [`.gitignore`](file:///Users/humolix/Documents/Dev/Volur-Lite/.gitignore) 并从 Git 索引中解绑敏感私有资产，统一向内网与公共仓库推送完整、一致的提交历史与架构方案（`agent/`）；
2. **控制器与流转架构现代化**：彻底下线已标注废弃的旧版 `AuthController` 全套堆栈，迁移至新一代状态机流转端点（`/signin/**`、`/signup/**`、`/recovery/**`）；
3. **领域实体与 REST 契约重构**：将 `AccountChooserProbeResponseVO` 重构更名为 `AccountChooserResponseVO`，实现前后端 1:1 强类型对齐；
4. **GitHub 远端协作资产合流**：GitHub 远端公共仓库近期通过 Pull Request #1 引入了完整的 `volur-training` 模块（包含题目池、实例管理、Flag 提交、排行榜与进度统计 API 及前端配套页面）、用户软删除幂等与长度截断加固、Swarm 容器服务名边界保护以及 Dev 代理覆盖能力。为保证内网与公共端代码资产的绝对统一，需将其完整合入并确保全平台通过构建。

---

## 2. 施工与提交清单

### 2.1 历史重构原子提交组织

1. **`refactor(auth): rename account chooser probe response vo to account chooser response vo`**
   - 领域实体重命名为 `AccountChooserResponseVO`；
   - 同步更新 Controller、Service、ServiceImpl 及单元测试；
   - 对齐前端 `volur-lite-view-NEXT` 中的 `auth.ts` 与 `flow.ts` 契约定义；
   - 归档设计与 JLS 协变约束分析文档。

2. **`refactor(auth): retire legacy auth controller stack and align security endpoints`**
   - 物理移除废弃的 `AuthController`、`AuthService`、`AuthServiceImpl`、历史 DTO/VO、专属异常及相关单测；
   - 收敛 `SecurityWhitelistProperties` 与 `MaintenanceModeFilter`，移除废弃接口白名单；
   - 同步适配 `nginx.conf.example` 限流规则至 `/signin`、`/signup`、`/recovery`；
   - 归档旧控制器下线方案文档。

3. **`chore(scripts): reinforce gitignore untrack sensitive files and retire github sync`**
   - 强化 `.gitignore` 规则，将 `.env`、`*.key`、`*.crt`、`package.sh`、`nginx.conf` 纳入敏感忽略；
   - 执行 `git rm --cached` 解绑敏感私有文件 Git 索引，磁盘物理实体完好保留；
   - 彻底删除 `scripts/git/github.sh`。

### 2.2 GitHub PR #1 协作资产原子合流

1. **`fix(user): make soft delete idempotent and hide deleted accounts from admin views`**
   - 修复软删除幂等性，杜绝重复删除导致追加后缀溢出；
   - 限制用户名与邮箱追加后缀后的最大长度，避免超长 SQL 报错；
   - 管理员用户列表过滤已软删除账号并补齐单测。

2. **`feat(training): add training module backend with pool, flag, progress and instance APIs`**
   - 新增 `volur-training` 后端模块；
   - 实现分类题库、动态/静态 Flag 核验、用户解题进度与排行榜统计；
   - 对接容器实例生命周期与删除防护 SPI。

3. **`fix(container): cap swarm service names and sync startup status on query`**
   - 限制 Swarm 容器服务名字符上限；
   - 查询时同步容器启动状态。

4. **`feat(view-NEXT): add training, announcement and admin training management pages`**
   - 新增训练场大厅 `TrainingListView`、答题弹窗、积分榜卡片与进度卡片；
   - 新增管理员训练场管理后台 `AdminTrainingView` 与题目批量导入功能；
   - 新增公告中心 `AnnouncementListView`。

5. **`fix(view-NEXT): surface flow crypto errors and allow dev proxy overrides`**
   - 前端认证流加密错误显式抛出与捕获；
   - `vite.config.ts` 增加本地开发环境变量代理覆盖能力；
   - `.gitignore` 纳入 `vite.https.config.ts`。

---

## 3. 验证与审计结果

1. **后端验证**：
   - 执行 `mvn test -f src/volur-lite-platform/pom.xml`；
   - 全平台 14 个模块全部编译通过，全部 53 个单元测试用例全部执行通过（Tests run: 53, Failures: 0, Errors: 0, Skipped: 0），Reactor Summary 全部 `SUCCESS`。
2. **前端验证**：
   - 执行 `npm run build --prefix src/volur-lite-view-NEXT`（包含 `vue-tsc --noEmit && vite build`）；
   - TypeScript 类型推导 100% 校验通过，前端生产构建无报错成功产出。
3. **敏感凭据隔离审计**：
   - 确认 `.env`、私钥、证书及私有部署配置未纳入 Git 索引；
   - 准备向 `github` 远端推送单一真实源提交历史。
