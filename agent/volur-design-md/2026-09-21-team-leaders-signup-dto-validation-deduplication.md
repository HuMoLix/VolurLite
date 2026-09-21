# 战队领队报名 DTO 校验注解去重与规范优化记录

## 1. 变更背景与原因
在 [`TeamLeadersSignupDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/dto/TeamLeadersSignupDTO.java) 中，`realName`（领队姓名）与 `phone`（领队手机号）字段此前同时标注了 `@NotNull` 和 `@NotBlank`。
在 Jakarta Bean Validation 规范中，`@NotBlank` 专门作用于 `CharSequence`（如 `String`），其内部校验逻辑为 `value != null && !value.toString().trim().isEmpty()`，已完全且严格涵盖了 `@NotNull` 的判空功能。
同时标注两个注解属于典型的**校验注解冗余（Redundancy Anti-pattern）**，在未配置 `failFast=true` 时会导致传入 `null` 时产生重复的校验错误信息，且破坏了与同模块 [`TeamMemberSignupDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/dto/TeamMemberSignupDTO.java) 规范的一致性。

## 2. 改动详情
修改文件：[`src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/dto/TeamLeadersSignupDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/dto/TeamLeadersSignupDTO.java)

1. **移除冗余注解**：
   - 移除 `realName` 字段上的 `@NotNull(message = "领队姓名不能为空")`，仅保留 `@NotBlank(message = "领队姓名不能为空")`；
   - 移除 `phone` 字段上的 `@NotNull(message = "领队手机号不能为空")`，仅保留 `@NotBlank(message = "领队手机号不能为空")` 及 `@Phone(message = "手机号格式不正确")`。
2. **清理无用导包**：
   - 移除未再使用的 `import jakarta.validation.constraints.NotNull;`。

## 3. 安全隐患说明与影响范围
- **安全与校验能力**：无任何降级。当入参为 `null`、空字符串 `""` 或全空格 `"   "` 时，`@NotBlank` 均能 100% 准确拦截并抛出对应的异常提示。
- **避免重复违规消息**：消除了 `null` 入参时在 `ConstraintViolationException` 或 `BindingResult` 中生成双份重复提示消息的问题。
- **影响范围**：
  - 后端 DTO：[`TeamLeadersSignupDTO.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition/domain/dto/TeamLeadersSignupDTO.java)
  - 单元测试：[`TeamMemberSignupDTOValidationTest.java`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform/volur-competition/src/test/java/com/humolix/volur/competition/domain/dto/TeamMemberSignupDTOValidationTest.java) 针对 `TeamLeadersSignupDTO` 的必填字段校验测试全部顺利通过。
  - 前端：前端类型 [`TeamLeadersSignupDTO`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/types/competition/index.ts#L225-L234) 与表单逻辑保持 1:1 严格契合，无需任何额外调整。

## 4. 验证情况
- 运行对应模块单元测试：
  `mvn test -pl volur-competition`
  运行 26 个测试，0 failures, 0 errors，全部顺利通过（`BUILD SUCCESS`）。
