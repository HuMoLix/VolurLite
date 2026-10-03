# 统一前后端登录流转 API 路径至 /api/v1/auth/signin

## 1. 变更背景与架构治理

根据 Volur 平台统一 API 路由命名与模块收敛规范（`com.humolix.volur.auth` 模块对外暴露的端点统一归聚于 `/api/v1/auth/*`），以及前端 `volur-lite-view-NEXT` 的强类型 API 请求封装原则：
1. **彻底废除历史兼容别名与多路由声明**：
   - 严禁在控制器中采用多别名路由数组 `@RequestMapping({"/api/v1/auth/signin", "/api/v1/signin"})`，彻底杜绝历史包袱残留，保持唯一单一事实来源（SSOT）；
   - 后端登录流转控制器 `SigninController` 必须严格收敛至标准路径 `@RequestMapping("/api/v1/auth/signin")`。
2. **前后端接口请求路径 100% 对齐**：
   - 前端 `src/volur-lite-view-NEXT/src/api/auth.ts` 中基于 `baseURL: /api/v1` 的各请求函数，原 `fetchChallengeSelectionApi` 仍在使用历史路径 `/signin/challenge/selection`（映射为 `/api/v1/signin/challenge/selection`），与后端新收敛标准脱节；
   - 必须全链路同步更正为 `/auth/signin/challenge/selection`（映射为 `/api/v1/auth/signin/challenge/selection`），与 `submitSignInIdentifierApi`（`/auth/signin/identifier`）及 `submitSignInChallengeApi`（`/auth/signin/challenge`）保持严格一致。

---

## 2. 改动点清单

### 2.1 后端工程改造 (`volur-auth`)
1. **控制器层路由收敛** (`SigninController.java`)：
   - 将类级别 `@RequestMapping({"/api/v1/auth/signin", "/api/v1/signin"})` 重构为 `@RequestMapping("/api/v1/auth/signin")`；
   - 彻底废除未经鉴权体系显式分组的扁平 `/api/v1/signin` 别名。
2. **安全白名单与过滤器确认**：
   - 检查并确认 `SecurityWhitelistProperties` 与 `MaintenanceModeFilter` 中均已规范配置 `/api/v1/auth/signin/**`，无需额外调整；
   - 运行单元测试（`SecurityWhitelistMatcherTest` 及 `SigninServiceImplTest`），确保所有鉴权放行规则与业务流转正常通过。

### 2.2 前端工程改造 (`volur-lite-view-NEXT`)
1. **API 请求层统一** (`src/api/auth.ts`)：
   - 将 `fetchChallengeSelectionApi` 请求路径由 `'/signin/challenge/selection'` 调整为 `'/auth/signin/challenge/selection'`；
   - 全面验证 `src/volur-lite-view-NEXT` 下所有路由视图与状态管理，确保无任何遗留的历史路径引用；
   - 执行 `npm run build`（`vue-tsc --noEmit && vite build`），确保编译与打包零错误。

---

## 3. 影响范围与回归测试验证

1. **影响模块**：
   - 后端：`volur-auth`（`SigninController`）；
   - 前端：`volur-lite-view-NEXT`（`src/api/auth.ts`，涉及登录标识核验、挑战验证、备选验证方式列表获取）。
2. **回归验证**：
   - **Maven 依赖与测试编译**：`mvn test-compile -pl volur-auth -am` 编译通过；
   - **Maven 单元测试验证**：`mvn test -pl volur-auth -am` 运行通过（38 tests, 0 failures, 0 errors）；
   - **前端构建与类型检查**：`npm run build` 成功完成打包产物构建，未产生类型或语法异常。
