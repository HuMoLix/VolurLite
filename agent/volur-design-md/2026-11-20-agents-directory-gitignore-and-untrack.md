# .agents 目录 Git 忽略规范化与索引解绑施工记录

## 1. 变更背景与问题分析

在 Volur Lite 项目中，`.agents/` 目录用于存放本地 AI Agent 运行环境配置（例如 `mcp_config.json` 等本地 MCP 服务器定义及运行时缓存）。

此前仓库针对 `.agents` 忽略策略存在以下两项问题：
1. **忽略规则格式不严谨**：
   原 [`.gitignore`](file:///H:/HuMoLix/github/Volur-Lite/.gitignore#L55-L58) 第 57 行使用的是根路径前缀且未带目录尾部斜杠的声明 `/.agents`。在项目既有规范中，目录忽略统一采用后缀斜杠形式（如 `.idea/`、`.vscode/`、`logs/` 等），使用 `.agents/` 可更严谨地将该目录及其所有子路径完整识别为目录忽略范围。
2. **Git 索引残留历史追踪（核心根因）**：
   在历史提交（`311b450717`）中，`.agents/mcp_config.json` 已被提前纳入 Git 暂存区并提交入库。由于 Git 的机制限制，`.gitignore` 仅对未受追踪（untracked）的新增文件生效，对已经存在于 Git 索引树中的文件不起作用。导致开发者即使配置了忽略规则，该目录及其内部文件依然被 Git 追踪状态变更，无法真正达成忽略效果。

---

## 2. 核心改动清单

### 2.1 规范化 [`.gitignore`](file:///H:/HuMoLix/github/Volur-Lite/.gitignore#L55-L58) 规则
将 `/.agents` 规范化调整为 `.agents/`，统一项目级目录忽略格式：
```diff
# Ai directory
/src/volur-lite-platform/.workbuddy-ai/
-/.agents
+.agents/
```

### 2.2 解绑 Git 索引追踪 (Untrack)
对受追踪的历史文件执行索引移除，同时完好保留本地磁盘物理文件：
```bash
git rm --cached -r .agents
```
- 本地磁盘文件 `H:/HuMoLix/github/Volur-Lite/.agents/mcp_config.json` 保持原样，本地 Agent 与 MCP 配置正常可用；
- Git 索引中的追踪记录已安全剥离，后续变更与新增内容均会被 `.gitignore` 规则有效忽略。

---

## 3. 验证与影响范围

1. **规则有效性验证**：
   - 执行 `git check-ignore -v .agents/mcp_config.json`，正确命中 `.gitignore:57:.agents/` 规则；
   - 执行 `git check-ignore -v .agents/test.txt`，验证新增测试路径同样稳定命中忽略规则。
2. **本地环境与安全性**：
   - 本地开发环境中的 `.agents/` 配置文件完好无损；
   - 杜绝后续本地个人 MCP 凭据与自定义代理配置被意外提交至远程代码仓库。
