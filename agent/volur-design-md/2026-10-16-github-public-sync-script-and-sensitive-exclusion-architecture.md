# GitHub 开源发布自动化过滤与初始化推送实施记录

## 1. 变更背景与诉求目标
仓库当前配置有两个远程源：
- 本地私有仓（`origin`: `http://192.168.3.39:3000/HuMoLix/Volur-Lite.git`）
- GitHub 外部开源仓（`github`: `https://github.com/HuMoLix/VolurLite.git`）

为防止团队敏感配置、内部构建逻辑、设计文档与 Agent 指令泄露，向 GitHub 推送时必须严格排除以下 6 项私有资产：
1. `.env`（生产与开发环境变量及凭据）
2. `GEMINI.md`（内部协作规范与 Agent 规则）
3. `package-amd64.sh`（内部特定打包脚本）
4. `scripts/`（内部数据库平滑迁移与运维脚本集）
5. `agent/`（全套施工审计记录、批量作业脚本与设计规范）
6. `.agents/`（Agent 上下文与运行时缓存）

而在本地私有仓库中，上述所有文件必须全量保留与追踪。

---

## 2. 核心架构与技术实现

### 2.1 杜绝历史泄漏的 Orphan 分支快照流
在 Git 历史中，部分私有文件曾存在于旧 Commit 中。若采用普通分支直接推送或简单删除，GitHub 历史记录依然可追溯全部敏感文件。
为此采用孤儿分支机制（`git checkout --orphan`）：
- 每次发布在内存中构建无父提交的根提交；
- 通过 `git rm -rf --cached --ignore-unmatch` 从 Git 索引层剥离指定路径，同时保持本地磁盘物理文件完好无损；
- 在临时分支的 `.gitignore` 中固化排除规则；
- 彻底斩断历史引用链，保证 GitHub 端为 100% 干净的开源快照。

### 2.2 提交元数据无损继承与传参重载
- 自动在切换分支前提取本地当前分支的元数据：
  - 作者姓名与邮箱：`git log -1 --pretty='%an <%ae>'`
  - 原始提交时间：`git log -1 --pretty=%ad`
  - 默认提交信息：`git log -1 --pretty=%B`
- 支持通过命令行参数 `${1:-$LOCAL_MSG}` 自由覆盖自定义 Commit Message。

### 2.3 异常中断自动现场恢复 (Crash-Safe Cleanup)
- 引入 Bash `trap cleanup EXIT` 机制；
- 无论脚本是成功、执行报错还是收到终止信号，均自动保证恢复到原始开发分支，清理临时分支并恢复工作区状态。

### 2.4 网络层代理自适应与 HTTP/2 Framing 容错
- 针对本地代理环境下 Git 大数据包推送易触发 `Error in the HTTP2 framing layer` / `Empty reply from server` 的问题：
- 脚本自动探测本地 `127.0.0.1:7890` 端口；
- 若代理存活，强制切换至 `socks5h://` 纯 TCP 隧道并调大 `http.postBuffer`，彻底消除 HTTP/2 握手与代理中间件流截断隐患；
- 若无本地代理，自动回退到原生 `HTTP/1.1` 安全模式推送。

---

## 3. 执行验证结果
1. 本地工作区变更已规范提交至 `master`；
2. 执行 `scripts/git/github.sh` 完成 GitHub 远端初始化推送；
3. 经 `git ls-remote github` 与 `git ls-tree -r github/main` 全量审计确认：
   - 远程主分支 `main` 成功创建；
   - 6 项私有资产在 GitHub 远端索引中完全不存在；
   - 本地 `master` 分支代码与物理文件完整保留，工作区保持纯净。
