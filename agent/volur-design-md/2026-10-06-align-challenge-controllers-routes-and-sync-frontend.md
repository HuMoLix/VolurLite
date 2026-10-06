# 挑战控制器路由规范对齐与全端同步实施方案

## 1. 变更背景与架构目标
在认证状态机与多阶段安全核验（MFA / Step-up Authentication）演进中，针对各个具体核验方式（如口令验证、邮件一次性验证码、短信动态口令、人机验证、WebAuthn 通行密钥）的控制器入口需要保持严密的单一职责与高度一致的 RESTful 路由规范。

此前部分挑战控制器采用了多别名映射数组以兼容历史命名（例如 `PasswordController` 映射了 `{"/pwd", "/password"}`，`PasskeyController` 映射了 `{"/passkey", "/pk", "/device"}`，`SMSOTPChallengeController` 映射了 `{"/totp", "/sms", "/smsotp", "/2fa"}`）。依照项目根规范《GEMINI.md》中的“彻底重构铁律（严禁妥协保留与旧命名别名）”以及“单一真实数据源”原则，彻底清理所有过渡期兼容别名，统一对齐规范路由映射，并实现前端工程动态路由与 API 调用的严格 1:1 对齐。

---

## 2. 后端控制器路由重构详情

### 2.1 规范路由映射矩阵
所有挑战核验控制器统一定位在 `com.humolix.volur.security.controller.v1.challenge` 包下，其类级别 `@RequestMapping` 统一为单一真实源：

| 控制器名称 | 规范路由映射 | 职责描述 |
| :--- | :--- | :--- |
| `PasswordController` | `/api/v1/captcha/challenge/pwd` | 账号密码安全核验 |
| `EmailOTPChallengeController` | `/api/v1/captcha/challenge/etotp` | 电子邮箱一次性验证码核验与重发 |
| `SMSOTPChallengeController` | `/api/v1/captcha/challenge/smsotp` | 手机短信/动态安全口令核验 |
| `ReCAPTCHAChallengeController` | `/api/v1/captcha/challenge/recaptcha` | 行为式人机验证挑战获取、非对称加密核验与流转推进 |
| `PasskeyController` | `/api/v1/captcha/challenge/pk` | FIDO2 / WebAuthn 通行密钥断言获取、注册与登录断言核销 |
| `ChallengeController` | `/api/v1/captcha/challenge` | 安全挑战方式切换 (`/select`) 与备选方式查询 (`/selection`) |

### 2.2 服务层流转参数与选项生成对齐 (`ChallengeServiceImpl`)
1. **备选核验列表 (`getChallengeSelection`)**：
   - 手机动态验证码选项：`type` 规范为 `smsotp`，`path` 规范为 `/v1/signin/challenge/smsotp`；
   - 密码核验选项：`type` 规范为 `pwd`，`path` 规范为 `/v1/signin/challenge/pwd`；
   - 邮箱核验选项：`type` 规范为 `etotp`，`path` 规范为 `/v1/signin/challenge/etotp`；
   - 通行密钥选项：`type` 规范为 `pk`，`path` 规范为 `/v1/signin/challenge/pk`。
2. **核验步骤默认值 (`verifySMSOTP`)**：
   - 当前步骤获取默认回退值对齐为 `smsotp`，与流转锁及会话状态机完全契合。

---

## 3. 前端工程同步实施详情 (`volur-lite-view-NEXT`)

### 3.1 API 请求层路径对齐 (`src/api/auth.ts`)
- `submitSMSOTPChallengeApi`: 请求路径统一更新为 `/captcha/challenge/smsotp`；
- `fetchPasskeyCreationOptionsApi`: 请求路径统一更新为 `/captcha/challenge/pk`；
- `fetchSupportedPasskeyAuthenticatorsApi`: 请求路径统一更新为 `/captcha/challenge/pk/authenticators`；
- `submitPasskeyRegisterApi`: 请求路径统一更新为 `/captcha/challenge/pk`；
- `fetchPasskeyAssertionOptionsApi`: 请求路径统一更新为 `/captcha/challenge/pk/assertion-options`；
- `submitPasskeyChallengeApi`: 请求路径统一更新为 `/captcha/challenge/pk`。

### 3.2 动态挑战请求机制与视图组件适配
1. **动态派发器 (`submitDynamicChallengeApi`)**：
   - 前端认证状态机通过 `POST /captcha/challenge/${targetStep}` 纯动态请求后端对应控制器，与后端控制器路由形成无缝直连（如 `nextStep = 'pwd'` -> `/captcha/challenge/pwd`，`nextStep = 'pk'` -> `/captcha/challenge/pk`，`nextStep = 'smsotp'` -> `/captcha/challenge/smsotp`）。
2. **视图层组件 (`totpChallengeView.vue` & `deviceChallengeView.vue`)**：
   - `totpChallengeView.vue`: 当流转参数为 `smsotp` 时，动态提交 `smsotp` 挑战，展示短信动态码核验文案；
   - `deviceChallengeView.vue`: 统一向状态机派发 `pk` 挑战核验请求，备选切换回跳参数统一为 `from: 'pk'`；
   - `selectionView.vue`: 备选方式列表支持识别 `smsotp` 图标与高亮渲染。

---

## 4. 测试与构建验证

### 4.1 后端工程测试验证
- 执行指令：`mvn test`
- 覆盖模块：全平台 13 个子模块（`volur-framework`, `volur-logging`, `volur-system`, `volur-user`, `volur-verification`, `volur-security`, `volur-notification`, `volur-auth`, `volur-challenge`, `volur-container`, `volur-competition`, `volur-server` 等）
- 测试结果：全部测试通过，BUILD SUCCESS（0 Failures, 0 Errors）。其中 `volur-security` 87 个用例、`volur-auth` 47 个用例 100% 绿灯。

### 4.2 前端工程构建验证
- **`volur-lite-view-NEXT`**：
  - 执行指令：`npm run build`
  - 验证内容：`vue-tsc --noEmit` 全量 TypeScript 类型检查及 Vite 生产打包
  - 结果：0 报错，成功生成构建产物。
- **`volur-lite-view` (经典前端)**：
  - 执行指令：`npm run build`
  - 结果：7.00s 内打包完成，构建顺利通过。
