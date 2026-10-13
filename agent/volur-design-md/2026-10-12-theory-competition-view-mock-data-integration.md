# 理论知识竞赛考场前端 Mock 数据接入与交互验证

## 变更背景
为了直观验证 `TheoryCompetitionView.vue` 及配套理论题型组件（`TheoryQuestionCard.vue`、`TheoryPaperSheet.vue`、`TheoryResultModal.vue`）在真实作答场景下的界面排版、响应式交互、状态机流转与视觉一致性，在前端界面中构造标准 Mock 试卷数据集，覆盖单选题（Single Choice）、多选题（Multiple Choice）与判断题（True / False）。

## 改动范围与核心实现

### 1. 目标文件
- `src/volur-lite-view-NEXT/src/views/competition/TheoryCompetitionView.vue`

### 2. 核心调整内容
1. **Mock 竞赛与试卷模型设计**：
   - 构造 `MOCK_COMPETITION`（`CompetitionDetailVO`）：提供标准的竞赛基础元数据，包含有效时间窗口以驱动 `CompetitionLayout` 的倒计时计算；
   - 构造 `MOCK_THEORY_PAPER`（`TheoryPaperExamVO`）：
     - **题型覆盖**：
       - 第 1 题：单选题（`SINGLE_CHOICE`）- Web 安全核心防御策略（SQL 注入参数化查询）；
       - 第 2 题：单选题（`SINGLE_CHOICE`）- 现代密码学非对称加密与数字签名机制；
       - 第 3 题：多选题（`MULTIPLE_CHOICE`）- 典型 C/C++ 内存破坏与漏洞利用（多选 Token 聚合验证）；
       - 第 4 题：多选题（`MULTIPLE_CHOICE`）- TLS 协议核心安全保障分析；
       - 第 5 题：判断题（`TRUE_FALSE`）- Linux SUID 进程有效权限运行机制；
       - 第 6 题：判断题（`TRUE_FALSE`）- 密码学安全哈希函数单向抗原像性质；
     - **试卷参数**：总分 100 分，总题数 6 题，各题分值合理分配（15 / 15 / 20 / 20 / 15 / 15）。
2. **优雅降级与本地 Mock 流转机制**：
   - 在 `fetchCompetitionAndPaper` 中增加数据保障逻辑：当路由无标识或后端接口离线/未配置题目时，平滑加载 Mock 数据集，保证前端开发态即开即看；
   - 在 `handleConfirmSubmit` 中增加本地模拟评分算法与结果回填：当离线或 Mock 环境下提交答卷时，自动根据标准答案计算得分、正确率与正确题数，并弹出 `TheoryResultModal` 结果报告，随后无缝切换为只读复核模式（Read-Only Review）。

## 安全与质量验证
- 严格遵循代码零注释铁律，代码内不包含冗余注释；
- 通过 `vue-tsc --noEmit && vite build` 严格模式编译校验，0 类型错误与打包告警。
