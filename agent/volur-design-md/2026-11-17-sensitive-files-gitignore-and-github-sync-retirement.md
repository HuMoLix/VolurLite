# 敏感文件 Git 忽略强化与 GitHub 导出脚本下线实施记录

## 1. 变更背景与架构目标

此前平台维护了两套独立的 Git 远程源推送逻辑：
- 本地内网仓库（`origin`）：接收包含内部打包脚本、私有环境配置在内的完整历史；
- 公共开源仓库（`github`）：依赖 `scripts/git/github.sh` 脚本在内存中构建临时孤儿分支，对每个提交增量执行粗暴的文件物理剥离（切除 `agent/`、`scripts/`、`package.sh`、`nginx.conf`、`.env` 等）后再推送。

该旧模式存在以下缺陷：
1. **仓库历史与资产严重割裂**：GitHub 端丢失了架构方案（`agent/`）、数据库平滑迁移脚本（`scripts/`）及项目交付规范（`GEMINI.md`），且两个远程源 Commit SHA 完全不同，无法进行分支合并与标签共享。
2. **凭据保护滞后与脆弱**：敏感凭据依赖外围脚本事后切除，而非在源码仓库层面进行单一真实源（SSOT）的解耦隔离。

**本次变更目标**：
- 废弃双仓库不同步导出的模式，统一向内网与 GitHub 推送完全相同的内容；
- 强化 [`.gitignore`](file:///Users/humolix/Documents/Dev/Volur-Lite/.gitignore)，将 `package.sh`、`nginx.conf`、`.env`、TLS 证书及私钥等敏感私有资产纳入忽略规则；
- 将已纳入追踪的敏感私有文件从 Git 索引中解绑（`git rm --cached`），本地磁盘物理文件保持完好；
- 彻底删除 `scripts/git/github.sh` 脚本及其相关流程。

---

## 2. 核心变更清单

### 2.1 Git 忽略规则强化 ([`.gitignore`](file:///Users/humolix/Documents/Dev/Volur-Lite/.gitignore))
在根目录 `.gitignore` 中完善并明确以下敏感规则声明：
- **敏感环境凭据**：
  - `.env`
  - `.env.*`
  - `!.env.example`（显式放行生产/开发脱敏配置模版）
- **TLS / SSL 证书与私钥**：
  - `*.key`
  - `*.crt`
  - `*.pem`
  - `**/nginx/ssl/`
- **内部部署交付脚本**：
  - `package.sh`
- **私有反向代理配置**：
  - `nginx.conf`
  - `**/nginx.conf`（保留 `nginx.conf.example` 模版在 Git 中正常追踪）

### 2.2 Git 索引解绑 (Untrack Sensitive Assets)
由于此前 `package.sh`、`.env`、`src/volur-lite-view-NEXT/nginx/conf/nginx.conf` 已被 Git 追踪，直接添加 `.gitignore` 不会对已追踪文件生效。因此执行安全解绑：
- 执行 `git rm --cached` 移除上述文件的 Git 索引；
- 物理磁盘上的文件完好保留，本地开发与运行不受任何影响。

### 2.3 彻底下线并移除 GitHub 同步脚本
- 执行 `git rm -f scripts/git/github.sh` 彻底删除旧有同步脚本；
- 清理空的 `scripts/git` 模块，消灭遗留维护负担。

---

## 3. 影响范围与后续推送指引

1. **本地环境完好性**：
   - 开发者本地磁盘中的 `.env`、`package.sh`、`nginx.conf` 均完整保留，本地 Docker 构建、服务拉起与开发联调不受任何干扰。
2. **统一推送流程**：
   - 敏感信息已在源码层物理隔离，后续无需再运行任何中间处理脚本；
   - 可直接使用常规 Git 命令统一向 `origin` 及 `github` 远端推送相同的分支与提交历史。
