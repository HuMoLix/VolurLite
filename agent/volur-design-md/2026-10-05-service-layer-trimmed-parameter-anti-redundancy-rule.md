# 服务层 @Trimmed 入参防冗余校验与 StringUtils.hasText 禁令规范

本文档记录了在 Volur 规范体系中确立“DTO 声明 `@Trimmed` 后 Service 层严禁使用 `StringUtils.hasText` 二次判定与手动 `.trim()`”的架构原则与实施落地。

---

## 1. 变更背景与反模式剖析

在既往代码审查中，常出现在 DTO 中已声明 `@NotBlank` 与 `@Trimmed`（由 Jackson 反序列化器与 Spring MVC 数据绑定器自动执行首尾空格去除）后，Service 层业务代码仍然重复进行防御性判白与修剪的现象：

```java
// 反模式: DTO 中已有 @NotBlank 与 @Trimmed 保证，Service 仍层层防御性判空与手动 trim()
if (!StringUtils.hasText(requestDTO.getData())) {
    throw new BusinessException(HttpStatus.BAD_REQUEST, "设备参数密文不能为空");
}
String decrypted = flowCryptoService.decrypt(requestDTO.getData().trim(), ...);
```

### 该反模式的弊端：
1. **违背单一信任源原则**：控制层入口结合 `@Valid` 与 `@Trimmed` 已经确保了数据的非空且经过了修剪，Service 层重复校验是对前置清洗流程的不信任与职责重叠；
2. **代码膨胀与噪声堆积**：大量的 `StringUtils.hasText` 和 `.trim()` 样板代码充斥在业务核心方法中，模糊了核心业务流；
3. **维护成本与不一致风险**：若入口校验规则调整，Service 层中手写的文案或判定逻辑容易出现遗漏或不同步。

---

## 2. 规范确立与核心改动

### 2.1 规范文件更新
1. **`agent/volur-design-skills/Rules.md`**：
   - **第 2.2 节 (DTO 规范)**：补充 `@Trimmed` 注解使用标准，明确在 DTO 声明 `@Trimmed` 后，进入 Service 的字段已无首尾空白；
   - **第 4 节 (服务层规范)**：重构并更名为“服务层方法设计与入参处理规范”，在 4.1.4 中确立**入参防冗余校验强约束（DTO `@Trimmed` 与 Service 层 `StringUtils.hasText` 禁令）**：
     - 严禁在 Service 层再次使用 `StringUtils.hasText(...)` 判定带有 `@Trimmed` 的入参字段；
     - 严禁在 Service 层对手写已有 `@Trimmed` 保障的入参字段重复调用 `.trim()`；
     - 必填字段直接信任消费；可选字段判空仅允许使用 `field != null`（或结合 `!field.isEmpty()`）；
   - **第 4.2 / 4.3 节**：补充反模式与推荐实践代码对照。
2. **`GEMINI.md`**：
   - 在核心规范第 1.1 节第 6 条中同步收录“入参防冗余校验强约束”条目。

### 2.2 代码实例重构 (`SessionServiceImpl.java`)
- 移除了对 `encryptedData` 的 `StringUtils.hasText(encryptedData)` 判定，直接使用 `encryptedData == null || encryptedData.isEmpty()` 快速判断；
- 移除了 `flowCryptoService.decrypt(encryptedData.trim(), ...)` 中冗余的 `.trim()` 调用，直接传入 `encryptedData`。

---

## 3. 影响范围与验证结果

- **规范完整性**：
  - `Rules.md` 与 `GEMINI.md` 全链路保持一致；
- **后端工程测试**：
  - `volur-security` 全量 32 项单元测试 100% 通过（`BUILD SUCCESS`）；
- **代码整洁度**：
  - 生产代码保持零行内解释性注释，架构约束全量外置记录于本文档。
