# 施工记录：自动化脚本编写规范 (Script-rules.md) 确立与行为指引绑定

## 1. 基本信息

- **新增/修改文件**：
  - 新增规范：[`volur-design-scripts/Script-rules.md`](file:///H:/HuMoLix/github/Volur-Lite/volur-design-scripts/Script-rules.md)
  - 更新指引：[`GEMINI.md`](file:///H:/HuMoLix/github/Volur-Lite/GEMINI.md)（第 2 节及第 3 节 AI 行为指引第 4 条）
  - 优化脚本：[`volur-design-scripts/check_and_fix_controller_api_response.py`](file:///H:/HuMoLix/github/Volur-Lite/volur-design-scripts/check_and_fix_controller_api_response.py)
- **执行时间**：2026-08-24
- **规范依据**：[`GEMINI.md`](file:///H:/HuMoLix/github/Volur-Lite/GEMINI.md) 及用户定制指令

---

## 2. 核心要求与规范内容

针对 Agent 使用脚本辅助开发的需求，制定并落地了 [`Script-rules.md`](file:///H:/HuMoLix/github/Volur-Lite/volur-design-scripts/Script-rules.md)，核心约束如下：

1. **定位纯粹**：
   - 脚本专供 Agent 自身执行与内部消费，非面向终端用户的交互程序；
2. **Token 零浪费原则**：
   - 严禁撰写长篇 docstring、类/函数冗余注释及占位符；
   - 严禁打印装饰性 UI、ASCII 分割线或冗长的说明文本；
3. **适用场景严格限制**：
   - 当且仅当需要处理跨多文件的大批量、高重复性作业（如全量合规扫描、大规模模式替换、批量 AST 审查）时方可编写脚本；
   - 严禁在针对性的单文件/少量文件修改任务中滥用脚本；
4. **输出直击要害**：
   - 脚本标准输出（stdout）仅返回 Agent 决策与操作所需的最小必要信息（如问题行、精简列表或关键确认）。

---

## 3. 验证与执行

- 在 `GEMINI.md` 中强制绑定了 `Script-rules.md`；
- 对已有的 `check_and_fix_controller_api_response.py` 按照新规范进行了彻底精简，执行输出精简为单行 `ALL_CONTROLLERS_COMPLIANT`，验证全项目 14 个 Controller 响应规范 100% 合规。
