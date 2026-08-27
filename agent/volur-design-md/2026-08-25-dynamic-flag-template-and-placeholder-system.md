# 动态 Flag 模版引擎与多维占位符解析系统设计与实现

## 1. 变更背景与需求

在 CTF 赛事与动态靶场运维中，不同竞赛、教学或演练场景对动态 Flag 的格式要求各异。传统模式下通过代码硬编码拼接 `flag{UUID}` 存在以下痛点：
1. **格式不可定制**：无法满足不同主办方自定义前缀（如 `VolurCTF{...}`、`DASCTF{...}`）或嵌套结构（如 `flag{flag-...}`）的诉求；
2. **缺乏防作弊与溯源关联**：无法将队伍凭证（Team Token）、用户 ID、赛题 ID 等维度动态嵌入 Flag 中，难以实现 Flag 泄露精准溯源；
3. **维护繁琐**：此前散落在环境变量中的配置无法热生效，必须重新部署服务。

本次改造目标：
1. 在 `volur-container` 模块构建强大的 [`DynamicFlagGenerator`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/flag/DynamicFlagGenerator.java) 模版解析引擎；
2. 从 [`SystemSettingService`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-system/src/main/java/com/humolix/volur/system/service/SystemSettingService.java)（MySQL + 二级缓存）中动态拉取 `DYNAMIC_FLAG_TEMPLATE`，实现免重启热更新；
3. 支持丰富的上下文占位符（`{UUID}`, `{UUID_SIMPLE}`, `{TEAM_TOKEN}`, `{USER_ID}`, `{TEAM_ID}`, `{CHALLENGE_ID}`, `{STAGE_ID}`, `{TIMESTAMP}`, `{RANDOM_STR}`, `{MD5}` 等）。

---

## 2. 核心架构与设计

### 2.1 占位符规范一览

模版引擎支持在模版中自由组合以下占位符：

| 占位符 | 说明 | 示例输出 |
| :--- | :--- | :--- |
| `{UUID}` | 标准 36 位连字符 UUID | `47dd666f-e12e-4049-bea6-9a04bbcfd6a3` |
| `{UUID_SIMPLE}` / `{SIMPLE_UUID}` | 32 位无连字符 UUID | `47dd666fe12e4049bea69a04bbcfd6a3` |
| `{TEAM_TOKEN}` | 参赛队伍 Token / 邀请码 / 防作弊特征码 | `ALPHA_TOKEN_9X` |
| `{USER_ID}` | 当前启动容器的用户 ID | `1001` |
| `{TEAM_ID}` | 参赛队伍 ID | `501` |
| `{CHALLENGE_ID}` | 题目 ID | `2001` |
| `{STAGE_ID}` | 赛事阶段 ID | `101` |
| `{RANDOM_STR}` / `{RANDOM_16}` | 16 位随机字母数字字符串 | `a8B9zK12mNop34Qx` |
| `{RANDOM_32}` / `{RANDOM_8}` | 32 位 / 8 位随机字母数字串 | `eF3...` |
| `{RANDOM_NUM_6}` | 6 位随机数字串 | `849201` |
| `{TIMESTAMP}` / `{TIMESTAMP_SEC}` | 毫秒级 / 秒级 Unix 时间戳 | `1771934400000` |
| `{DATE}` / `{YYYYMMDD}` | 当前日期格式串 | `20260825` |
| `{HASH}` / `{MD5}` | 结合用户、赛题与 UUID 的 16 位防篡改散列 | `b3f18e9a2c4d01aa` |

### 2.2 模版配置与计算逻辑示例

1. **示例 1：自定义 CTF 品牌前缀**
   - 系统设置配置：`DYNAMIC_FLAG_TEMPLATE = "VolurCTF{{UUID}}"`
   - 生成结果：`VolurCTF{47dd666f-e12e-4049-bea6-9a04bbcfd6a3}`
2. **示例 2：标准 Flag 格式**
   - 系统设置配置：`DYNAMIC_FLAG_TEMPLATE = "flag{{UUID}}"`
   - 生成结果：`flag{47dd666f-e12e-4049-bea6-9a04bbcfd6a3}`
3. **示例 3：嵌套前缀 Flag**
   - 系统设置配置：`DYNAMIC_FLAG_TEMPLATE = "flag{flag-{UUID}}"`
   - 生成结果：`flag{flag-47dd666f-e12e-4049-bea6-9a04bbcfd6a3}`
4. **示例 4：队伍防作弊溯源 Flag**
   - 系统设置配置：`DYNAMIC_FLAG_TEMPLATE = "VolurCTF{team_{TEAM_TOKEN}_{USER_ID}_{UUID_SIMPLE}}"`
   - 生成结果：`VolurCTF{team_ALPHA_TOKEN_9X_1001_47dd666fe12e4049bea69a04bbcfd6a3}`

---

## 3. 改动涉及文件

1. **[`DynamicFlagContext.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/flag/DynamicFlagContext.java)**：
   - 封装 `userId`, `username`, `challengeId`, `stageId`, `teamId`, `teamToken` 等上下文属性。
2. **[`DynamicFlagGenerator.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/flag/DynamicFlagGenerator.java)**：
   - 核心解析组件，优先读取 `DYNAMIC_FLAG_TEMPLATE`，其次兼容 `FLAG_TEMPLATE` 和历史 `FLAG_PREFIX` / `FLAG_SUFFIX`，具备完善的回退容错。
3. **[`CtfInstanceService.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/service/CtfInstanceService.java) & [`CtfInstanceServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/service/impl/CtfInstanceServiceImpl.java)**：
   - 增加支持传入阶段 ID、队伍 ID 与队伍 Token 的重载启动方法，调用 `DynamicFlagGenerator` 完成 Flag 注入与持久化。
4. **[`CtfStageServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/service/stage/ctf/impl/CtfStageServiceImpl.java)**：
   - 比赛模式下启动动态容器时，自动传递 `stageId`、`team.getId()` 和 `team.getInviteCode()`。
5. **[`DynamicFlagGeneratorTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-container/src/test/java/com/humolix/volur/container/flag/DynamicFlagGeneratorTest.java)**：
   - 编写了针对自定义模版、嵌套模版、队伍 Token 占位符、回退默认格式等 4 组单元测试，全部 100% 验证通过。

---

## 4. 验证结果

- 执行 `mvn test -pl volur-container -am`，测试用例全部通过（4/4 Passed, 0 Failures, 0 Errors）。
- 执行全工程编译 `mvn clean compile -DskipTests`，12 个模块全部成功通过编译。
