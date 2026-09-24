# 工作区 Chrome DevTools MCP 服务配置与安装记录

## 1. 变更背景与需求分析

开发者需要为当前工作区（`Volur-Lite`）配置并安装 `chrome-devtools-mcp@latest`。
`chrome-devtools-mcp` 是 Google Chrome DevTools 官方针对 AI 智能体（如 Antigravity、Claude Code、Cursor 等）推出的 Model Context Protocol (MCP) 服务端，允许智能体直接控制和审查 Chrome 浏览器实例，用于浏览器端到端自动化测试、控制台日志与网络流量诊断、页面性能追踪（Performance Trace）以及界面交互测试。

---

## 2. 施工与实施方案

### 2.1 全局环境依赖与预热
- 验证系统 Node.js（v24.15.0）与 npm/npx 运行环境；
- 执行 `npm install -g chrome-devtools-mcp@latest` 完成全局依赖安装与二进制包缓存预热，确保即使在弱网环境下也能免等待直接通过 `npx` 或命令调用（当前版本 `1.10.1`）。

### 2.2 工作区级配置收敛
依据 Antigravity 自定义系统与 MCP 服务规范，在项目根目录创建工作区专属 MCP 配置文件：
- **配置文件路径**：[`.agents/mcp_config.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/.agents/mcp_config.json)
- **配置内容**：
  ```json
  {
    "mcpServers": {
      "chrome-devtools": {
        "command": "npx",
        "args": [
          "-y",
          "chrome-devtools-mcp@latest"
        ]
      }
    }
  }
  ```
- **加载优先级**：工作区级配置（`.agents/mcp_config.json`）具备最高优先级，支持团队通过 VCS 共享该工具能力，确保任何在当前工作区启动的会话均可自动发现并挂载 `chrome-devtools` 工具集。

### 2.3 CLI 管理器同步检查
- 使用 `agy mcp list` 校验 MCP 服务状态；
- CLI 会话与 IDE 智能体均能正常识别并加载 `chrome-devtools` stdio 服务端。

---

## 3. 影响范围与审计说明

- **对现有业务代码的影响**：零侵入，未改动任何 Java 后端与 Vue 前端业务代码；
- **新增文件**：
  - [`.agents/mcp_config.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/.agents/mcp_config.json)：工作区 MCP 服务配置。
  - [`agent/volur-design-md/2026-09-23-workspace-chrome-devtools-mcp-installation.md`](file:///Users/humolix/Documents/Dev/Volur-Lite/agent/volur-design-md/2026-09-23-workspace-chrome-devtools-mcp-installation.md)：本审计记录文件。
