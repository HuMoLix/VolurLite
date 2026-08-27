# 前后端注册实体变量名严格 1:1 对齐（移除别名注解）

## 1. 规范要求与背景
根据 [`view-Rules.md`](file:///H:/HuMoLix/github/Volur-Lite/agent/volur-design-skills/view-Rules.md) 核心设计规范第 4 条：
> **TS API 实体与后端 API 实体严格对齐**：
> 请求入参实体（`*DTO`）必须严格映射后端 `domain.dto.*DTO` 字段名与类型（小驼峰命名），严禁依靠后端宽松别名容错，强制要求前后端模型定义 1:1 对齐。

---

## 2. 改造点清单

### 2.1 后端工程 (`volur-auth`)
1. **[`RegisterRequestDTO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/dto/RegisterRequestDTO.java)**：
   - 彻底移除 `@JsonAlias({"emailCaptcha", "emailCode"})` 及其 Jackson 导入；
   - 纯净声明规范字段：`private String emailCaptcha;`。

### 2.2 前端工程 (`volur-lite-view`)
1. **[`src/types/auth/index.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/types/auth/index.ts)**：
   - 更新 `RegisterRequestDTO` 接口，将原 `emailCode` 字段重命名为 `emailCaptcha?: string`，与后端 DTO 保持 100% 一致。
2. **[`src/components/auth/AuthForm.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/components/auth/AuthForm.vue)**：
   - 表单项路径与绑定值统一为 `registerForm.emailCaptcha`；
   - 表单校验规则路径统一为 `emailCaptcha`；
   - 调用 `registerApi` 提交载荷统一为 `{ username, password, email, emailCaptcha }`。

---

## 3. 验证与审计结论
- **前端构建**：`vue-tsc --noEmit && vite build` 编译打包 100% 通过。
- **后端测试**：全平台 11 个模块执行 `mvn test` 100% 编译并测试通过。
- **契约合规**：前后端实体字段名称严格 1:1 对齐，消除一切别名容错负担。
