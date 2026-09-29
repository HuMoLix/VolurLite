# 施工记录：reCAPTCHA 动态风控挑战解耦、会话一致性保护与前端多模态核验引擎

**日期**：2026-09-30  
**作者**：Antigravity Agent  
**模块**：`volur-auth`、`volur-security`、`volur-verification`、`volur-lite-view-NEXT`

---

## 1. 变更背景与架构目标

在原有的登录与风控流程中存在以下架构耦合与反模式问题：
1. **上层业务越权判定验证码形态**：`SigninServiceImpl.identifier` 在检测到 5 次失败枚举时，提前硬编码了具体验证码形态（`IMAGE` 或 `SLIDER`）并写死在会话及响应 VO 中，违反了单一真实数据源与职责正交原则；
2. **缺乏独立的人机挑战生命周期端点**：`ChallengeController` 为空壳，缺少获取挑战（`GET /api/v1/captcha/challenge/recaptcha?dsh=&tl=`）与非对称加密提交核验（`POST /api/v1/captcha/challenge/recaptcha?dsh=&tl=`）的端点；
3. **前端缺少对非对称加密与多模态弹窗的承载**：`recaptcha.vue` 原先仅在沙箱内部做离线判断，无法根据服务端风控下发动态拉起滑动拼图、扭曲字符或选图模态弹窗；
4. **全系统通用性不足**：`/api/v1/flow/initSess` 初始化端点未签发初始 `tl`，导致除登录外的其它未登录业务（如注册、找密等）无法直接复用会话流转锁与人机验证防御。

本次重构全面落地了用户提出的 8 项闭环链路，实现了服务端职责解耦、`dsh` 与 `tl` 双重会话一致性保护、RSA 原生非对称加密打包核验、以及前端多模态 Google Material Design 规格弹窗的完整闭环。

---

## 2. 核心架构时序与流转设计

```
[前端客户端 / View-NEXT]                                       [后端 SigninController]      [ChallengeController & 风控中心]
       │                                                                  │                                 │
  (1)  │── POST /api/v1/auth/signin/identifier ──────────────────────────►│                                 │
       │   (identifier, dsh, fp)                                          │── 失败>=5次 / 环境高危            │
       │                                                                  │── 只签发 nextStep="recaptcha"   │
       │◄── 200 { nextStep: "recaptcha", dsh, tl, cid: 1 } ───────────────┤                                 │
       │                                                                                                    │
       │── 前端路由转至 /v1/signin/challenge/recaptcha?dsh=...&TL=...                                         │
       │                                                                                                    │
  (2)  │── [用户点击 reCAPTCHA 复选框]                                                                      │
       │── GET /api/v1/captcha/challenge/recaptcha?dsh={dsh}&tl={tl} ──────────────────────────────────────►│
       │                                                                                                    │── 校验 dsh + tl 会话一致性
       │                                                                                                    │── 动态评估人机风险策略:
       │                                                                                                    │   CLICK / SLIDER / PUZZLE /
       │                                                                                                    │   IMAGE / SELECT_IMAGE
       │                                                                                                    │── 签发 recaptchaToken
       │◄── 200 { challengeType, recaptchaToken, publicKey, puzzleData... } ────────────────────────────────┤
       │                                                                                                    │
  (3/4)│── [分支 A: 若为 CLICK (点击验证码)]                                                                 │
       │   收集客户端沙箱环境指纹与生物遥测包 ──► RSA 公钥加密打包为 payload                                  │
       │                                                                                                    │
       │── [分支 B: 若为 PUZZLE/IMAGE 等 (弹窗验证码)]                                                      │
       │   呼出 Google 风格模态弹窗 ──► 用户完成拼图拖拽/输入字符 ──► RSA 公钥加密打包为 payload             │
       │                                                                                                    │
  (6)  │── POST /api/v1/captcha/challenge/recaptcha?dsh={dsh}&tl={tl} ─────────────────────────────────────►│
       │   Body: { recaptchaToken, payload }                                                                │── RSA 私钥解密 & 防重放校验
       │                                                                                                    │── 校验环境/轨迹/验证码结果
       │                                                                                                    │── 签发 recaptchaTicket (存入 Redis, 绑定 dsh)
       │◄── 200 { passed: true, recaptchaTicket: "rctk_xxx" } ──────────────────────────────────────────────┤
       │                                                                                                    │
       │── 前端复选框打勾变绿（若有弹窗则关闭弹窗），更新 recaptchaTicket，激活“下一步”按钮                    │
       │                                                                                                    │
  (5)  │── POST /api/v1/auth/signin/challenge ──────────────────────────►│                                 │
       │   Body: { challengeType: "recaptcha", dsh, tl, cid: 1,           │── 强核销 recaptchaTicket         │
       │           captchaTicket: "rctk_xxx" }                            │── 查库校验账号真实性              │
       │                                                                  │   ├─ 账号存在: 清零失败计数       │
       │                                                                  │   │  流转至 pwd (签发新 TL)     │
       │◄── 200 { nextStep: "pwd", dsh, tl, cid: 2 } ─────────────────────┤   └─ 账号不存在: 累加计数, 报400 │
       │                                                                                                    │
       │── 路由推进至 /v1/signin/challenge/pwd ─────────────────────────────────────────────────────────────┘
```

---

## 3. 具体改动实施清单

### 3.1 后端工程改造 (`volur-lite-platform`)

1. **`volur-security` 模块**：
   - **`domain/vo/RecaptchaChallengeVO.java`**：新建安全挑战获取视图，包含 `challengeType` (`CLICK`, `PUZZLE`, `SLIDER`, `IMAGE`, `SELECT_IMAGE`)、`recaptchaToken`、`publicKey`、背景切片 Base64、缺口 Y 坐标与提示文字；
   - **`domain/dto/RecaptchaVerifyRequestDTO.java`**：新建挑战核验入参，封装 `recaptchaToken` 与 RSA 非对称加密字符串 `payload`；
   - **`domain/vo/RecaptchaVerifyResponseVO.java`**：新建核验成功视图，返回 `passed: true` 与 `recaptchaTicket`；
   - **`domain/vo/FlowInitVO.java`**：增加 `private String tl;` 初始流转锁字段；
   - **`flow/TransitionLockManager.java`**：新增 `validateStepAndTl(dsh, tl, expectedStep)` 方法，在人机验证阶段执行会话状态与 TL 严格校验（不跨步、不销毁）；
   - **`service/ChallengeService.java` & `impl/ChallengeServiceImpl.java`**：
     - `getRecaptchaChallenge(dsh, tl)`：综合评估客户端 IP、设备指纹与风险等级，签发 `recaptchaToken` 并下发自适应验证数据与 RSA 公钥；
     - `verifyRecaptcha(dsh, tl, request)`：解密 `payload`，核验时间戳防重放，比对拼图/字符/环境遥测指标，通过后签发 `recaptchaTicket` 存入 Redis 并与 `dsh` 绑定（TTL 5分钟）；
     - `validateAndBurnTicket(dsh, ticket)`：供认证服务在最终推进流转时强核销通行票据；
   - **`controller/v1/ChallengeController.java`**：暴露 `GET /api/v1/captcha/challenge/recaptcha` 与 `POST /api/v1/captcha/challenge/recaptcha` 端点；
   - **`service/impl/FlowServiceImpl.java`**：在 `initSess` 会话初始化阶段若 `activeTL` 为空，自动签发初始 TL 并装配至 `FlowInitVO`。

2. **`volur-verification` 模块**：
   - **`crypto/CaptchaCryptoService.java`**：新增 `decryptPayload(String payload)` 方法，支持对客户端使用 RSA-PKCS1 加密的分段 Base64 密文或双段密文进行健壮解密。

3. **`volur-auth` 模块**：
   - **`service/impl/SigninServiceImpl.java`**：
     - 重构 `identifier` 方法：当失败次数 $\ge 5$ 或环境高危时，仅下发 `nextStep("recaptcha")`，彻底移除预设具体验证码类型的耦合；
     - 重构 `challenge` 的 `case "recaptcha"` 分支：注入 `ChallengeService`，读取并调用 `validateAndBurnTicket(dsh, ticket)` 强校验人机通行票据；核验通过后执行账号真实性校验并推进至 `pwd` 步骤。

### 3.2 前端工程改造 (`volur-lite-view-NEXT`)

1. **`src/utils/captcha-crypto.ts`**：
   - 引入行业标准成熟 npm 库 `jsencrypt`，杜绝非标准手写密码学逻辑；
   - 彻底废除客户端内置硬编码的 `DEFAULT_RSA_PUBLIC_KEY_PEM`，强制要求必须基于服务端每次动态下发的 RSA 公钥（PEM）进行加密；
   - 封装 UTF-8 字符安全字节分段与 RSA-PKCS1 密文块合并算法，自动注入高精防重放时间戳 `ts`。
2. **`src/api/captcha.ts`**：
   - 封装强类型 `getRecaptchaChallengeApi(dsh, tl)` 与 `verifyRecaptchaChallengeApi(dsh, tl, data)`，默认开启 `{ skipToast: true }` 实现错误就地反馈。
3. **`src/components/security/RecaptchaModal.vue`**：
   - 遵循 Google Material Design 规格的独立模态弹窗组件；
   - 支持滑动拼图（带缺口背景图、平移动态切片、轨道拖拽）、字符图形验证码与滑动条；
   - 支持亮色（`#ffffff`、`#1a73e8`）与暗色（`#292a2d`、`#8ab4f8`）沉浸式无缝切换。
4. **`src/components/security/recaptcha.vue`**：
   - 接入 `dsh`、`tl`、`ticket` 属性与双向绑定；
   - 点击复选框时触发向后端获取动态人机挑战；
   - 若为 `CLICK` 挑战，自动加密沙箱生物与环境遥测包提交，无感变绿通过；
   - 若为 `PUZZLE` / `IMAGE` 等挑战，拉起 `<RecaptchaModal>` 弹窗；弹窗完成后加密提交并关闭弹窗，通知沙箱打勾变绿并对外输出 `recaptchaTicket`。
5. **`src/views/auth/signin/ChallengeView.vue`**：
   - 挂载 `:dsh="authFlowStore.dsh"` 与 `:tl="authFlowStore.tl"`；
   - 人机验证完成后获取 `recaptchaTicket`，点击“下一步”时携带 `captchaTicket` 提交至 `/api/v1/auth/signin/challenge`；
   - 支持根据 `nextStep === 'identifier'` 判定，当账号不存在时优雅携带错误回退至账号输入页并展示错误信息；当账号存在（`nextStep === 'pwd'`）时平滑推进至密码页。
6. **`src/views/auth/signin/IdentifierView.vue`**：
   - 支持消费 `authFlowStore.accountError`，在从人机挑战页回退时回显刚才的账号，并在输入框下方渲染“用户名或邮箱不存在”错误提示。
7. **`src/store/modules/authFlow.ts`**：
   - `ensureFlowParams` 自动接收并持久化 `initSess` 下发的初始 `tl`；
   - `submitChallenge` 返回参数增加 `errorMessage`，增加 `accountError` 跨阶段流转错误存储与消费方法。

---

### 3.3 人机验证后账号存在性自适应分流引擎 (Scheme B)

为解决风控触发 reCAPTCHA 后错误提示错位及流转死胡同问题，实现了如下闭环流转：
1. **账号不存在（分支 1）**：
   - 后端在 `SigninServiceImpl.challenge`（`case "recaptcha"`）中校验验证码通过后，发现账号不存在时不抛出 400 异常；
   - 推进状态机回退：`advanceTransitionTicket(..., "recaptcha", nextCid, "identifier", ...)`，签发给 `identifier` 阶段的有效 TL，并在 `SignInFlowResponseVO` 中附加 `errorMessage = "用户名或邮箱不存在"`；
   - 前端 `ChallengeView` 判定 `nextStep === 'identifier'`，通过三重通道（Pinia 响应式状态、`sessionStorage` 持久化凭据以及路由 `query.accountError` 参数）注入错误信息，静默跳转回 `/v1/signin/identifier`；
   - `IdentifierView` 在初始化挂载期（`hasMounted` 门禁防护）阻断响应式监听器误清空，精准捕获消费错误信息并渲染在账号输入框下方，同时回显用户输入的账号；待用户开始实际键入时方执行错误重置。
2. **账号存在（分支 2）**：
   - 验证码通过且账号存在时，重置失败计数，降低风险评分；
   - 推进状态机至 `pwd` 步骤（签发密码挑战 TL），返回 `nextStep = "pwd"`；
   - 前端 `ChallengeView` 无刷新推进至 `/v1/signin/challenge/pwd`，平滑展示密码输入表单。
3. **按钮点击转动特效抑制 (Google Material 纯净交互对齐)**：
   - 在 `AuthLayout.vue` 深度选择器中全局屏蔽认证卡片内按钮加载转圈动画（`.n-base-loading`, `.n-button__icon`）；
   - 在各业务视图（`IdentifierView`, `ChallengeView`, `SignUpView`）中将操作按钮的状态由 `:loading="loading"` 调整为 `:disabled="loading"`，消除点击时图标旋转与文字抖动，由卡片顶部的 Material Linear Progress 进度条独占提供高雅加载指示。

---

## 4. 验证与审计结论

1. **后端全量测试通过**：
   - 编写了完整的单元测试用例 [`ChallengeServiceImplTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/test/java/com/humolix/volur/security/service/ChallengeServiceImplTest.java)；
   - 更新了 [`SigninServiceImplTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/test/java/com/humolix/volur/auth/service/impl/SigninServiceImplTest.java)（验证账号不存在时状态机平滑回退至 `identifier` 并返回 `errorMessage`）；
   - 执行 `mvn test -pl volur-security,volur-auth,volur-verification -am`，全平台 9 个模块 37 个测试用例全部通过（`BUILD SUCCESS`）。
2. **前端工程构建通过**：
   - 执行 `npm run build`（`vue-tsc --noEmit && vite build`），全工程 2808 个模块打包成功，0 错误，0 警告。
3. **规范符合性审计**：
   - 所有 Java、TypeScript、Vue 代码严格遵守纯粹代码与零注释铁律；
   - 主题配色与交互严格对齐 Google Workspace / Material Design 规格。
