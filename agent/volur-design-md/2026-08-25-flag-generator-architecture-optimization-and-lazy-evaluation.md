# 动态 Flag 生成器架构重构与性能深度优化

## 1. 优化背景与痛点分析

在之前的实现中，`DynamicFlagGenerator` 存在以下可优化空间：
1. **全量预计算浪费与性能开销**：无论模版中是否包含某些占位符（如 `{MD5}`、`{RANDOM_32}`、`{TIMESTAMP}`、`{DATE}`），每次生成 Flag 时均会无条件执行全量 UUID、多组随机字符串生成与 MD5 哈希计算，在高并发创建容器时带来不必要的 CPU 与内存开销；
2. **缺乏开闭原则（OCP）与策略扩展能力**：所有占位符的解析与生成硬编码在一个单体方法中，未来新增扩展占位符（如参数化长度 `{RANDOM:20}`、HMAC 防作弊签名 `{SIGN}`、业务扩展属性 `{ATTR:xxx}`）时必须反复修改核心代码；
3. **缺乏模版合法性校验与预览机制**：管理员在系统后台配置模版时，无法预先校验语法正确性与检测未知非法占位符。

---

## 2. 优化方案与架构设计

重构后的 [`com.humolix.volur.container.flag`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-container/src/main/java/com/humolix/volur/container/flag) 采用**门面模式 + 策略模式 + 惰性正则流式匹配**架构：

```
com.humolix.volur.container.flag
  ├── DynamicFlagContext.java          // 增强上下文模型（支持 competitionId、challengeKey 及 extraAttributes）
  ├── DynamicFlagGenerator.java        // 门面服务（提供生成、模版预览 previewFlag 与语法校验 validateTemplate）
  ├── FlagTemplateParser.java          // 单次扫描高性能正则解析引擎（基于 Matcher 惰性求值）
  └── resolver/                        // 策略解析器集群
       ├── FlagPlaceholderResolver.java     // 策略接口
       ├── IdentityPlaceholderResolver.java // 身份标识解析器（用户、队伍 Token、赛题、扩展属性）
       ├── RandomPlaceholderResolver.java   // 随机源解析器（UUID、动态长度随机串 RANDOM:N、HEX:N、时间戳）
       └── SecurityPlaceholderResolver.java // 安全摘要解析器（MD5、SHA256、HMAC-MD5 防作弊水印 SIGN/WATERMARK）
```

---

## 3. 核心优化亮点

1. **单次扫描 + 惰性按需计算（Lazy Evaluation）**：
   - 基于 `Pattern.compile("\\{([A-Za-z0-9_:]+)\\}")` 进行单次流式扫描；
   - 仅当模版中真实匹配到占位符时，才触发对应 `FlagPlaceholderResolver` 执行具体的生成计算；未出现的占位符零开销。
2. **参数化与动态扩展占位符支持**：
   - 支持动态长度随机串：`{RANDOM:24}`、`{RANDOM_NUM:8}`、`{HEX:16}`；
   - 支持防作弊与溯源水印：`{SIGN}`、`{HMAC}`、`{WATERMARK}`，结合系统密钥对用户与队伍身份进行签名；
   - 支持上下文自定义属性透传：`{ATTR:customKey}`。
3. **模版预校验与实时效果预览**：
   - 提供 `validateTemplate(String template)`：自动提取并返回未识别的非法占位符列表；
   - 提供 `previewFlag(String template)`：使用模拟上下文即时生成预览结果，极大方便管理端 UI 交互。

---

## 4. 验证结果

- 在 [`DynamicFlagGeneratorTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-container/src/test/java/com/humolix/volur/container/flag/DynamicFlagGeneratorTest.java) 中补充完善了 6 组测试用例，覆盖自定义模版、嵌套模版、队伍 Token 占位符、参数化随机长度、HMAC 防作弊水印以及模版语法校验；
- 执行 `mvn test -pl volur-container -am` 全部测试通过（6/6 Passed, 0 Failures, 0 Errors）。
