# 登录挑战流转改造：试试其他方式、多方案选择与拒绝核验架构

## 1. 变更背景与需求概述

在统一的 Google Accounts 风格登录流转规范（VLIF）下，需重构登录挑战页面（`ChallengeView.vue`）的底部操作按钮及备选验证流程：
1. **统一操作按钮对齐与 Scrim 遮罩触发**：
   - 将原挑战页底部的“返回”按钮重构为“试试其他方式”，且布局与 `IdentifierView.vue` 对齐，紧邻“下一步”主操作按钮左侧（`flex items-center justify-end gap-2.5`）。
   - 将 `AuthLayout.vue` 中点击“下一步”触发的延迟及白色半透明 Scrim 遮罩（配合顶部进度条动画）提取为统一可复用的 `triggerScrim` 函数。
   - “试试其他方式”按钮绑定 `:isScrim="true"`，点击后即刻唤起白色半透明遮罩与顶部蓝色进度条，并保持 800ms~1000ms 平滑延迟后再流转，保证沉浸式过渡。
2. **表单前端校验优先铁律**：
   - 对于 `IdentifierView.vue` 与 `ChallengeView.vue` 中的“下一步”/“验证”提交类按钮，必须优先在组件内进行表单输入数据合法性校验；
   - 若数据为空或格式校验未通过，立即就地提示错误，**严禁触发延迟和白色遮罩层**；
   - 仅当输入校验通过且允许向后端发起认证请求时，方可通过 `submitIdentifier` / `submitChallenge` 唤起延迟、白色半透明遮罩（`rgba(255, 255, 255, 0.6)` / 暗色 `rgba(32, 33, 36, 0.6)`）与顶部进度条。
3. **路由规范化与废除 `/reject` 别名**：
   - 彻底废除 `common.ts` 与 `auth.ts` 中多余的 `/reject` 兼容重定向路由，全系统统一收敛至标准路径 `/v1/signin/challenge/rejected`。
4. **人机验证分支拒绝机制**：
   - 在 `/v1/signin/challenge/recaptcha` 页面中点击“试试其他方式”时，带 Scrim 延迟跳转至拒绝核验页面 `/v1/signin/challenge/rejected`。
   - 左侧标题展示“无法登录”，右侧文案通过 URL 参数传输并展示：“Volur 无法验证此账号归您所有。请稍后重试，或前往“账号恢复”页面寻求帮助。”
5. **多维备选核验方式与 Google V3 Selection 样式**：
   - 在其他挑战页面（如 `/v1/signin/challenge/pwd`、`/v1/signin/challenge/pk/presend` 等）点击“试试其他方式”时，跳转至 `/v1/signin/challenge/selection`，并调用后端 `/api/v1/signin/challenge/selection` 获取当前会话可用的备选身份核验方式列表（如设备验证 `/v1/signin/challenge/pk`、邮箱验证 `/v1/signin/challenge/etotp`、短信验证 `/v1/signin/challenge/totp`、安全验证器 `/v1/signin/challenge/atcr` 等）。
   - 彻底移除前端所有 Mock 模拟数据；当后端无可用方式或请求发生异常时，直接跳转至拒绝终态页面（`/v1/signin/challenge/rejected`）。
   - 选择列表视觉样式 100% 对齐 Google Accounts V3（`accounts.google.com/v3/signin/challenge/selection`）：外层包裹圆角边框卡片（`.auth-selection-list`），条目之间采用细分隔线（`divide-y`），配备 24px 语义图标、主标题、说明副标题以及右侧指示箭头。
6. **选择列表分页与终态拒绝流转**：
   - 右侧展示“选择您想要使用的登录方式：”，列表最多展示 3 项（当前分页的 1~2 个验证选项 + 1 个“试试其他方式”选项）。
   - 用户依次点击“试试其他方式”翻页；当到达最后一页并点击“试试其他方式”时，跳转至 `/v1/signin/challenge/rejected`，右侧提示：“您提供的信息不足以让 Volur 认定您确实是这个账号的所有者。Google 需要使用这类信息来保证您账号的安全。”
   - 底部提供“返回”与“重试”按钮，点击“重试”带 Scrim 延迟返回 `/v1/signin/challenge/selection` 重新选择。
7. **URL 参数传输与编解码**：
   - 所有拒绝提示文本均通过 URL Query 参数 `rejectedmsg` 进行规范传输（URL 编码），前端具备防重复转义的双重解码健壮性。

---

## 2. 架构设计与改动点

### 2.1 后端工程改造 (`volur-auth`)
1. **领域视图对象新增** (`ChallengeOptionVO.java`)：
   - 严格遵循 VO 规范，包含 `@Data`、`@Builder`、`@NoArgsConstructor`、`@AllArgsConstructor`；
   - 字段收敛：`type`（核验类型编码）、`title`（展示标题）、`subtitle`（详细说明文案）、`icon`（前端图标标识）、`path`（前端挑战路由路径）。
2. **服务层契约与业务实现** (`SigninService.java` & `SigninServiceImpl.java`)：
   - 新增 `getChallengeSelection(String dsh, String currentType)` 方法；
   - 根据会话句柄（`dsh`）读取当前会话绑定的用户标识，安全查询数据库用户并动态计算脱敏安全邮箱与脱敏手机号码；
   - 组装包含设备验证（`pk`）、邮箱验证（`etotp`）、手机短信（`totp`）、身份验证器（`atcr`）、账号密码（`pwd`）及备用验证码（`backup_code`）的候选列表；
   - 根据入参 `currentType` 自动过滤当前已处于的挑战方式，保持备选核验列表为 5 项高可用方案。
3. **控制器双向路由映射** (`SigninController.java`)：
   - 类级别路由扩展为 `@RequestMapping({"/api/v1/auth/signin", "/api/v1/signin"})`，无缝支持规范中的 `/api/v1/signin/challenge/selection` 及平台统一的 `/api/v1/auth/signin/challenge/selection`；
   - 新增 `@GetMapping("/challenge/selection")` 接口，返回 `ApiResponse<List<ChallengeOptionVO>>`。
4. **单元测试补齐** (`SigninServiceImplTest.java`)：
   - 增加针对 `getChallengeSelection` 从密码页发起调用的测试用例，验证排除 `pwd`、返回 5 项备选核验方式以及邮箱/手机脱敏逻辑的正确性。

### 2.2 前端工程改造 (`volur-lite-view-NEXT`)
1. **路由纯净化** (`router/routes/common.ts` & `router/routes/auth.ts`)：
   - 彻底移除临时性的 `/reject` 重定向，确保所有拒绝流转严格收敛于 `/v1/signin/challenge/rejected`；
   - 动态路由采用 `challenge/:type(.*)*`，原生支持单段及多段子路径。
2. **全局 Scrim 遮罩触发优化与校验隔离** (`AuthLayout.vue`)：
   - 彻底移除 `AuthLayout.vue` 中对 `.btn-next-step` 等按钮的无差别全局点击拦截（移除了脆弱的 `isActionTriggered` 机制）；
   - 移除 `watch(() => route.fullPath)` 中粗暴清空 `loading` 的反模式，由异步任务自身的 `finally` 块接管遮罩生命周期，彻底解决因路由跳转导致遮罩瞬间闪退失效的问题；
   - 重构 `.auth-card__blocker` 样式，配置 `background-color: rgba(255, 255, 255, 0.6)`（暗色 `rgba(32, 33, 36, 0.6)`）与 `z-index: 20`，配合顶部 `z-index: 30` 的蓝色进度条，实现与 Google Accounts 100% 一致的高保真毛玻璃遮罩交互。
3. **表单前端校验前置规范** (`IdentifierView.vue` & `ChallengeView.vue`)：
   - `IdentifierView.vue` 的 `handleNext`：先执行 `trimmed` 校验，未通过直接展示提示信息，不触发任何遮罩；校验通过后由 `submitIdentifier` 触发带有平滑过渡的 Scrim 与请求；
   - `ChallengeView.vue` 的 `handleVerifyChallenge`：根据当前挑战类型（`pwd`、`recaptcha`、`totp` 等）优先校验密码/验证码是否填写；未通过即刻报错，通过后才进入 `submitChallenge` 并唤起遮罩。
4. **“试试其他方式”与选择流转 Scrim 完善** (`ChallengeView.vue`)：
   - `handleTryAnotherWayClick`、`handleSelectMethod`、`handleTryAnotherWayInSelection`、`handleCancelSelection`、`handleRejectedBack`、`handleRejectedRetry` 均统一采用 `authFlowStore.triggerScrim`；
   - 在异步跳转动作前显式维持 800ms~1000ms 视觉驻留与动画，随后平滑执行 `await router.push(...)`，彻底解决“点击无延迟、无遮罩”的交互缺陷。
5. **Google Accounts Selection 极简无界列表重构** (`ChallengeView.vue`)：
   - 提示文本：`font-size: 16px; line-height: 24px; font-weight: 400; color: #1f1f1f; margin-bottom: 20px;`（暗色 `#e3e3e3`）；
   - 列表容器 `.auth-selection-list`：取消外层边框与圆角卡片（`border: none; border-radius: 0; overflow: visible;`）；
   - 条目行 `.auth-selection-item`：
     - 取消全部外边框，仅保留底部单分隔线（`border: none; border-bottom: 1px solid #dadce0;`，暗色 `border-bottom-color: #3c4043;`）；
     - 内边距与高度：`padding: 16px 8px; min-height: 56px; box-sizing: border-box; outline: none; cursor: pointer;`；
     - 悬停/激活态：`&:hover { background-color: rgba(11, 87, 208, 0.08); }`，`&:active { background-color: rgba(11, 87, 208, 0.16); }`（暗色分别为 `rgba(138, 180, 248, 0.12)` 与 `0.2`）；
     - 键盘无障碍焦点：`&:focus-visible { outline: 2px solid #0b57d0; outline-offset: -2px; }`（暗色 `#8ab4f8`）；
   - 纯描边矢量图标与排版：
     - 采用全描边 SVG 图标（`fill: none; stroke: currentColor; stroke-width: 1.75; stroke-linecap: round; stroke-linejoin: round;`）；
     - 图标色值严格指定为品牌蓝 `#0b57d0`（暗色模式高对比 `#8ab4f8`）；
     - 移除条目提示副标题，仅保留主标题文本（如“获取电子邮件验证码”、“在手机上获取动态验证码”、“使用“身份验证器”应用”、“输入您的密码”、“试试其他方式”）；
     - 彻底删除列表项右侧的展开箭头图标（`>`）；
   - 底部取消按钮：采用圆角胶囊文字按钮（`.btn-try-another-way`），`color: #0b57d0; border-radius: 9999px; height: 36px; padding: 0 16px;`，点击同样触发平滑 Scrim 延迟。

---

## 3. 安全考量与影响范围

1. **会话一致性保护**：
   - 在选择与拒绝阶段严格复用 `dsh`、`flowName`、`flowEntry` 等会话参数；
   - 进入 `rejected` 界面时不阻断用户查看无法登录的原因，防止因已作废的 TL 抛出阻断异常。
2. **信息泄露防护**：
   - 备选核验列表中的邮箱与手机号均在服务端进行单向脱敏处理，杜绝敏感联系方式暴露。
3. **零注释铁律与规范依从性**：
   - 所有新增及修改的代码文件（`.java`、`.ts`、`.vue`）严格执行零注释要求；
   - 视觉样式严格遵循 Google Material Design / Workspace 配色（Google Blue `#0b57d0` / `#8ab4f8`，无全黑正文）。

---

## 4. 验证结果

1. **后端验证**：
   - 运行 `mvn test -pl volur-auth "-Dtest=SigninServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" -am`，6 个测试全部通过（耗时 1.42s），构建结果为 `BUILD SUCCESS`。
2. **前端验证**：
   - 运行 `npm run build`（`vue-tsc --noEmit && vite build`），全工程 TypeScript 静态类型检查与 Vite 生产打包 100% 通过（耗时 5.28s，构建退出码 0）。
