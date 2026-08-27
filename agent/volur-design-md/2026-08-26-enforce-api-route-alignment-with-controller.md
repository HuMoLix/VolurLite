# 前端 API 请求路径与 Controller 路由严格对齐规范施工记录

**施工日期**：2026-08-26  
**施工模块**：`src/volur-lite-view/src/api/`、`agent/volur-design-skills/view-Rules.md`、`GEMINI.md`  
**责任主体**：Antigravity AI Agent

---

## 1. 变更背景与问题发现

在审查前端 API 请求层（`src/volur-lite-view/src/api/`）时发现：
部分接口请求路径存在主观臆测添加动词或单复数拼写错误的反模式。例如：
- 错误示例：`getCompetitionListApi` 错误请求了 `/competition/list`；
- 正确后端：`CompetitionController.java` 类级别注解为 `@RequestMapping("/api/v1/competitions")`，方法级别注解为 `@GetMapping`，实际真实暴露的 RESTful 路由应为 `/api/v1/competitions`（Axios 相对路径 `/competitions`）。

---

## 2. 核心改动点与实施细节

### 2.1 全量重构对齐前端 API 请求函数
对照后端各模块 Controller 层 `@RequestMapping` 与方法级映射注解（`@GetMapping`、`@PostMapping`、`@PutMapping`、`@DeleteMapping`、`@PatchMapping`），全面重构了前端 `src/api/` 下的所有请求函数：
1. **竞赛模块 (`src/api/competition/index.ts`)**：
   - 列表接口修正为 `GET /competitions`（对应 `CompetitionController.java` 的 `@GetMapping`）；
   - 详情接口修正为 `GET /competitions/{id}` 与 `GET /competitions/by-slug/{slug}`；
   - 战队接口修正为 `POST /competitions/{competitionId}/teams` 与 `POST /competitions/{competitionId}/teams/invite`；
   - 排行榜接口修正为 `GET /competitions/{competitionId}/scoreboard`。
2. **认证模块 (`src/api/auth/index.ts` & `src/utils/request.ts`)**：
   - Token 刷新接口严格对齐 `POST /auth/refresh`（对应 `TokenController.java`）；
   - 登录/登出/注册/修改密码等与 `AuthController.java` 100% 对齐。
3. **用户模块 (`src/api/user/index.ts`)**：
   - 个人资料对齐 `GET /users/profile` 与 `GET /users/me`；
   - 管理端对齐 `GET /admin/users`、`DELETE /admin/users/{id}`、`GET /admin/users/roles` 等。
4. **验证模块 (`src/api/verification/index.ts`)**：
   - 场景验证码对齐 `GET /captcha/challenge`、`POST /captcha/verify-ticket`；
   - 邮件验证码对齐 `POST /email/captcha/register` 与 `POST /email/captcha/change-password`。
5. **靶机与通知模块 (`src/api/container/` & `src/api/notification/`)**：
   - 靶机容器对齐 `POST /challenges/instances/{id}/start`、`stop`、`extend`；
   - 站内信通知对齐 `GET /notifications`、`PATCH /notifications/{id}/read`、`PATCH /notifications/read-all`。
6. **系统设置模块 (`src/api/system/index.ts`)**：
   - 公开设置对齐 `GET /system/settings/public`；
   - 管理端对齐 `GET /admin/system/settings`、`POST /admin/system/settings/update`。

### 2.2 规范制定与强化 (`view-Rules.md` & `GEMINI.md`)
- **`view-Rules.md`**：
  - 新增 **第 5 节「API 请求路径与后端 Controller 路由严格 1:1 对齐规范 (API Route Alignment Standard)」**；
  - 明确指出禁止擅自拼接 `/list`、`/doLogin` 等非 Controller 声明的子路径；
  - 提供反模式代码（如 `/competition/list`）与正确 Controller 对照代码剖析；
  - 编制 **全系统核心 Controller 路由映射速查矩阵表**。
- **`GEMINI.md`**：
  - 在前端核心规范准则与 AI 行为指引中加入强约束：**API 请求路径与 HTTP 动词必须 100% 严格与后端 Controller 声明对齐**。

---

## 3. 验证与构建测试

在 `src/volur-lite-view` 目录下执行全量验证：
```powershell
npm run build
```
**验证结果**：
- `vue-tsc --noEmit`：0 错误；
- `vite build`：打包成功（耗时 ~5.08s）。

---

## 4. 影响范围与审计评估

- **代码影响范围**：
  - `src/volur-lite-view/src/api/` (全部模块)
  - `src/volur-lite-view/src/utils/request.ts`
  - `src/volur-lite-view/src/store/`
  - `agent/volur-design-skills/view-Rules.md`
  - `GEMINI.md`
- **稳定性评估**：
  - 彻底解决了前后端接口联调时由于路径臆测（如 `/competition/list`）导致 404 或由于 HTTP Method 不匹配导致 405 的严重 Bug，保障全栈契约的一致性与严谨性。
