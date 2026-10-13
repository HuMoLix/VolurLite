# 理论题库题目导入实体类重构与 JSON 校验升级报告

## 1. 变更背景与需求概述

针对管理员在理论题库管理中导入题目的业务场景，原后端实现使用非结构化的 `JsonNode` 进行逐字段手动提取，存在如下缺陷：
1. 缺乏强类型领域模型定义，无法在反序列化阶段利用 Jackson 的契约机制进行数据结构合法性判定；
2. 选项（`options`）在不同格式（如 JSON 字典对象 `{"A": "...", "B": "..."}` 与数组）下解析易退化；
3. 导入数据出现字段缺失或格式畸变时无法第一时间快速失败，排查成本高。

基于标准导入模板 [`模拟卷一_reverted.json`](file:///H:/HuMoLix/github/VolurCTF2026/theory/模拟卷一_reverted.json)，本次对后端 [`AdminTheoryChallengeServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/service/admin/theory/impl/AdminTheoryChallengeServiceImpl.java) 的题目导入与校验逻辑进行重构。

---

## 2. 架构设计与改动点

### 2.1 引入强类型导入实体类

在 `volur-challenge` 模块 `domain.dto.theory` 包下新建：
1. **[`TheoryChallengeImportDTO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/domain/dto/theory/TheoryChallengeImportDTO.java)**：
   - 字段对齐：`name`、`content`、`type`、`options`（`JsonNode`）、`standardAnswer`、`category`、`explanation`、`score`、`difficulty`；
   - 别名支持：通过 `@JsonAlias` 容错匹配 `title`、`question`、`choices`、`answer` 等常见别名；
   - 业务校验：提供 `validate(int index)` 方法，严格校验 `name`、`content`、`type`、`standardAnswer` 的必填约束，并对单选/多选题的 `options` 进行非空非假值校验；
   - 语义标准化：`resolveTheoryType()` 统一映射枚举类型；`resolveOptionsString()` 将对象字典或数组无损序列化为数据库所需的 TEXT 存储串；`resolveNormalizedAnswer()` 将判断题标准化为 `TRUE`/`FALSE`，多选题标准化为排序的字母序列。
2. **[`TheoryBankImportDTO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/domain/dto/theory/TheoryBankImportDTO.java)**：
   - 支持题库维度的批量导入结构（包含题库名称、描述、分类及题目集合 `questions`）。

### 2.2 服务层导入解析与校验重构

在 [`AdminTheoryChallengeServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/service/admin/theory/impl/AdminTheoryChallengeServiceImpl.java) 中进行重构：
1. 废除原有的 `parseJsonToItemList` 与 `parseAndSaveQuestion` 纯 `JsonNode` 反模式；
2. 新增 `parseToChallengeImportDTOList` 方法，利用 Jackson `objectMapper.convertValue` / `readValue` 严格反序列化至 `List<TheoryChallengeImportDTO>`；若 JSON 语法损坏或顶层结构不符合规范，立即抛出 `BusinessException(HttpStatus.BAD_REQUEST)` 快速失败；
3. 循环中调用 `dto.validate(idx)` 校验各项数据完整性，并结合内存级同名探测与数据库排重，汇总清晰报错列表 `errors`；
4. 构建实体方法 `buildChallengeEntity` 基于标准化 DTO 属性映射至持久化实体 `TheoryChallenge`。

### 2.3 判题逻辑兼容性提升

在 [`TheoryCompetitionServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/theory/impl/TheoryCompetitionServiceImpl.java) 中升级 `evaluateAnswer`：
- 多选题支持将逗号分隔或连写的多选答案提取为字符集排序比对；
- 判断题统一转化为布尔语义进行判定，彻底解决前后端不同表达（`TRUE`/`正确` vs `FALSE`/`错误`）导致的判定误差。

---

## 3. 规范遵循与测试验证

1. **零注释铁律 (Zero Comments Rule)**：
   - 所有新建与修改的代码文件（`TheoryChallengeImportDTO.java`、`TheoryBankImportDTO.java`、`AdminTheoryChallengeServiceImpl.java`、`TheoryCompetitionServiceImpl.java`、`TheoryChallengeImportTest.java`）中无任何解释性注释。
2. **零 Mock 与快速失败原则 (Fail-Fast & Zero-Mock Rule)**：
   - 数据缺失或格式不合法直接抛出强类型业务异常。
3. **单元测试与集成编译验证**：
   - 新建 [`TheoryChallengeImportTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-challenge/src/test/java/com/humolix/volur/challenge/service/TheoryChallengeImportTest.java)；
   - 针对标准模板 [`模拟卷一_reverted.json`](file:///H:/HuMoLix/github/VolurCTF2026/theory/模拟卷一_reverted.json) 的 300 道题（150 单选、60 多选、90 判断）执行全量解析与断言，测试绿灯通过；
   - 负向缺字段测试用例断言全部通过；
   - `mvn test-compile -pl volur-challenge,volur-competition -DskipTests` 构建成功。
