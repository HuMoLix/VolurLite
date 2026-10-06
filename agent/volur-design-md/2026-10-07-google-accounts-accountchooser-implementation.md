# Google Accounts 风格账号选择器 (Account Chooser) 全链路协同改造方案报告

## 1. 变更背景与架构动机

在原有的 Volur Lite 登录设计中，无论客户端是否曾在当前设备或浏览器上成功登录过账号，每次访问登录入口均单调直奔空白的账号标识输入页面（`/v1/signin/identifier`），要求用户重复输入邮箱或用户名。

与 Google Accounts 现代身份认证体验相比，存在显著断层：
1. **缺少已知账号的持久识别与记忆呈现（Account Chooser）**：用户在多账号或常驻设备场景下，无法直观看到自己的历史登录身份（头像、昵称、邮箱及会话存活状态）；
2. **缺少免输入邮箱的直推流转通道**：用户在账号选择器中点击已知账号时，服务端缺少直接承接该账号身份并签发针对首个安全挑战（密码 `pwd`、人机验证 `recaptcha`）的原子流转能力；
3. **缺少客户端账号批量探活端点（Probe/Discovery）**：前端无法向后端批量探查本地已知账号的实时状态（是仍处于有效登录态、已登出还是已被平台封禁）；
4. **缺少设备侧已知账号安全移除与凭据吊销端点（Remove）**：用户在公共或共享设备上移除账号时，无法同步撤销服务端记住我（Remember-Me）或受信任设备（`UserTrustedDevice`）记录。

基于上述背景与规范要求，本次对后端 `volur-auth`（[`SigninController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/controller/v1/SigninController.java)）与新一代前端工程 `volur-lite-view-NEXT` 进行了全链路的闭环协同改造。

---

## 2. 核心改动清单与实现细节

### 2.1 后端工程架构扩展 (`volur-auth`)

1. **领域模型与请求契约扩展 (`domain.dto` & `domain.vo`)**：
   - [`AccountChooserProbeRequestDTO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/dto/AccountChooserProbeRequestDTO.java)：接收客户端本地已记住的 `identifiers` 列表；
   - [`AccountChooserSelectRequestDTO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/dto/AccountChooserSelectRequestDTO.java)：包含会话句柄 `dsh` 与所选账号标识 `identifier`；
   - [`AccountChooserRemoveRequestDTO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/dto/AccountChooserRemoveRequestDTO.java)：包含待移除账号标识 `identifier`；
   - [`AccountChooserItemVO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/vo/AccountChooserItemVO.java)：输出账号的脱敏信息、头像 `avatarUrl` 以及状态 `sessionStatus`（`ACTIVE`、`SIGNED_OUT`、`DISABLED`）；
   - [`AccountChooserProbeResponseVO.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/domain/vo/AccountChooserProbeResponseVO.java)：包装列表响应。

2. **服务层抽象与实现 (`SigninService` & `SigninServiceImpl`)**：
   - `probeAccounts`：批量查询用户与其画像资料（[`UserProfileRepository`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/repository/UserProfileRepository.java)），利用当前请求上下文（[`SecurityUtils`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/security/util/SecurityUtils.java)）判定是否为当前有效登录者，严格防止异常并确保零敏感信息外泄；
   - `selectAccount`：通过会话句柄恢复状态机会话，注入所选 `identifier`，联合风控评估引擎（[`RiskEvaluationEngine`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/risk/engine/RiskEvaluationEngine.java)）与设备矩阵（`DeviceEnvironmentMatrixEvaluator`），动态签发第一阶段事务锁（`tl`）与首个挑战步骤（`nextStep="pwd"` 或高危下的 `"recaptcha"`）；
   - `removeAccount`：当用户在选择器中移除账号时，若为当前登录态自动清理 Cookie，并物理删除当前设备指纹在 [`UserTrustedDeviceRepository`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-user/src/main/java/com/humolix/volur/user/repository/UserTrustedDeviceRepository.java) 中的信任绑定。

3. **控制器端点建设 ([`SigninController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-auth/src/main/java/com/humolix/volur/auth/controller/v1/SigninController.java))**：
   - `POST /api/v1/auth/signin/accountchooser/probe`：批量探测账号有效性与资料；
   - `POST /api/v1/auth/signin/accountchooser/select`：选中账号直接进入安全挑战流转；
   - `POST /api/v1/auth/signin/accountchooser/remove`：移除设备侧账号凭据；
   - 严格遵守 Thin Controller 原则、小驼峰工厂方法与零行内注释规范。

---

### 2.2 前端工程对齐与视图重构 (`volur-lite-view-NEXT`)

1. **类型定义与 API 封装 (`types/auth/flow.ts` & `api/auth.ts`)**：
   - 增加 `AccountChooserProbeRequestDTO`、`AccountChooserSelectRequestDTO`、`AccountChooserRemoveRequestDTO`、`AccountChooserProbeResponseVO`、`AccountChooserItemVO` 与 `KnownAccount` 类型声明；
   - 封装 `probeAccountChooserApi`、`selectAccountChooserApi` 与 `removeAccountChooserApi`。

2. **设备持久化工具模块 (`utils/accountChooser.ts`)**：
   - 封装 `getKnownAccounts`、`saveKnownAccount`、`removeKnownAccount` 与 `hasKnownAccounts`；
   - 自动在客户端维护最多 10 个最近登录账号的元数据。

3. **状态机 Store 扩展 (`store/modules/flow/signinFlow.ts`)**：
   - 扩展 `selectAccount(chosenIdentifier)` 动作，打通选中账号与 `dsh`/`tl`/`cid` 的状态机绑定；
   - 在 `submitAuthenticate` 成功终态时自动触发 `saveKnownAccount`，将登录成功的身份持久化到本地列表中。

4. **路由架构与跳转逻辑优化 (`router/routes/auth.ts`)**：
   - 访问 `/v1/signin` 统一重定向至 `/v1/signin/accountchooser`；
   - 访问 `/v1/signin/accountchooser` 时，**即使当前设备本地无任何已登录/已保存账号，亦绝不执行强制跳步或重定向至标识输入页**；
   - 页面始终稳定驻留于 `accountchooser`，无账号状态下仅单独渲染“使用其他账号”列表项。

5. **Google Accounts 风格选择器视图 (`views/auth/signin/AccountChooserView.vue`)**：
   - 全面遵循 Google Material Design 3 / Workspace 色彩体系；
   - 移除 `loadAndProbeAccounts` 与 `handleRemove` 中在账号为空时强制跳回 `/v1/signin/identifier` 的跳转行为；
   - 当无账号（`accountList.length === 0`）时，自适应仅显示“使用其他账号”（点击跳转至空白标识输入页面），并自动隐藏“移除账号”控制按钮；
   - 完整支持：账号卡片（首字母/自定义头像、昵称、邮箱、状态徽章）、“使用其他账号”、以及“移除账号”编辑与确认模式。

6. **标准 Auth 列表选择器样式提取 (`styles/auth-list.scss`)**：
   - 将原分散在 `OptionsChallengeStepView.vue`、`selectionView.vue` 中的列表渲染样式抽象提取为全局共享的 `auth-list.scss`；
   - 统一定义 `.auth-selection-list` 与 `.auth-selection-item`（涵盖 `__left`、`__icon`、`__avatar`、`__content`、`__title`、`__subtitle`、`__right`、`:hover`、`:active`、`:focus-visible` 以及暗色模式适配）；
   - 在 `index.scss` 中全局引入，并在 `AccountChooserView.vue`、`OptionsChallengeStepView.vue`、`selectionView.vue` 中全量复用，消除冗余 scoped CSS 代码。

7. **全量国际化 (i18n) 适配**：
   - 在 `src/locales/zh-CN/auth.json`、`src/locales/zh-TW/auth.json`、`src/locales/en-US/auth.json` 中统一注入账号选择器专用国际化键值（`choose_account`、`choose_account_subtitle`、`use_another_account`、`remove_account`、`status_signed_in`、`status_disabled`、`status_signed_out`、`account_disabled`、`account_flow_failed`）；
   - `AccountChooserView.vue` 结合 `useCardHeader` 与 `useI18n` 实现双端卡片标题与多语言响应式切换。

---

## 3. Account Chooser 访问无请求与账号未呈现根因分析 (Deep Dive)

### 3.1 现象重现
当在浏览器首次或未记住状态下直接访问 `/v1/signin/accountchooser?continue=/competitions&followup=/v1/signin/identifier&flowName=VlifWebLogin&flowEntry=WebServiceLogin&pstMsg=1&dsh=S-100000001:1791316521104000&TL=...&lang=zh-cn` 时：
1. 打开浏览器开发者工具（DevTools）Network 面板，未观察到任何发送往后端 `/api/v1/auth/signin/accountchooser/probe` 的网络请求；
2. 即使在另一个标签页或历史中已处于登录态，选择器中亦未呈现任何可供快速进入的已登录账号。

### 3.2 深度根因排查 (Root Cause)
1. **前端本地缓存空值短路 (Client Early-Exit Bypass)**：
   - 在改造前的 `AccountChooserView.vue` 中，`loadAndProbeAccounts` 存在逻辑：`const cached = getKnownAccounts(); if (cached.length === 0) return;`；
   - 当浏览器首次访问或在无痕模式下（`localStorage` 中尚无 `volur_known_accounts` 记录）时，代码直接提前 `return`，根本未触发 `probeAccountChooserApi` 调用，导致 DevTools 中看不到任何网络请求。
2. **后端 DTO 参数硬约束拦截空探活 (DTO Constraint Inflexibility)**：
   - 改造前 `AccountChooserProbeRequestDTO.identifiers` 字段标记有 `@NotEmpty`；
   - 若前端发送空数组，后端将直接以 400 Bad Request 快速失败，无法支持“仅探测当前请求上下文有效会话”的场景。
3. **后端探活服务被动单向匹配缺陷 (Passive Matching Only)**：
   - 改造前的 `SigninServiceImpl.probeAccounts` 纯粹只遍历入参 `identifiers` 列表并在数据库中查询；
   - 若入参未传任何 identifier，即使当前 HTTP 请求通过 Cookie/Authorization 携带了有效登录凭证，服务端也未主动自审 `SecurityContextHolder` / `SecurityUtils`，导致无法主动下发当前设备已激活的账户信息。

### 3.3 闭环解决方案与协同演进
1. **放宽 DTO 约束，支持零入参主动探活**：
   - 移除 `AccountChooserProbeRequestDTO.identifiers` 上的 `@NotEmpty` 限制，默认赋初值 `List.of()`。
2. **服务端主动发掘当前已认证会话 (Active Session Discovery)**：
   - 在 `SigninServiceImpl.probeAccounts` 中，如果当前请求已经过身份认证（`SecurityUtils.isAuthenticated()`），自动提取当前登录用户名并查询其完整个人资料（包含头像与昵称），作为 `ACTIVE` 状态账号优先置顶放入响应列表。
3. **前端全时发送探活并双向同步 (Bidirectional Client Sync)**：
   - `AccountChooserView.vue` 移除 `if (cached.length === 0) return;` 阻断，无论本地是否有缓存均无条件调用 `probeAccountChooserApi`；
   - 若本地有缓存则传已知 identifier 列表探活；若本地为空则发空列表让服务端自主发现当前 Cookie/凭证关联的有效账号；
   - 当前端从探活响应中接收到有效账号后，立即通过 `saveKnownAccount` 回写至客户端本地 `localStorage`，完成闭环同步。

---

## 4. 安全防护与设计原则保证

1. **Fail-Fast & Zero-Mock 铁律**：
   - 严禁隐式伪造假数据，对未配置或不存在的依赖严格抛出业务异常中断；
   - 服务端所有状态均基于真实数据库、Redis 会话及风控引擎计算下发。
2. **防账号枚举安全设计**：
   - 探测接口仅供客户端核验已知标识的展示元数据与状态；若不存在统一作为 `SIGNED_OUT` 安全降级返回，杜绝泄露敏感账户属性。
3. **事务锁（TransitionLock）强绑定防篡改**：
   - 用户在 Account Chooser 点击账号后，服务端下发的每个 `tl` 均深度绑定该 `identifier` 与目标挑战步骤，防止客户端篡改或越权跳步。
4. **规范与代码纯净性**：
   - 所有新建与修改的代码文件均严格剔除了解释性行内注释，保持极致简洁与自解释性；
   - 遵循 Lombok `@RequiredArgsConstructor` 与不可变依赖注入。

---

## 5. 验证与审计结论

1. **后端单元测试与集成测试验证**：
   - 运行 `mvn test -pl volur-auth -Dtest=SigninServiceImplTest`：包括新增的主动会话识别、探活、选择与移除等全部用例 100% 通过（7/7 绿灯）。
2. **前端类型检查与生产打包验证**：
   - 在 `src/volur-lite-view-NEXT` 运行 `npm run build`（`vue-tsc --noEmit && vite build`）：在 6.08 秒内无任何告警和错误，生产打包完全通过。

