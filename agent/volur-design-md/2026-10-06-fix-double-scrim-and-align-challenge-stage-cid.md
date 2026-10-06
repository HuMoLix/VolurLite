# 登录方式选择双重遮罩修复与挑战层级 CID 归一化升级方案

## 1. 变更背景与问题溯源

在对新一代认证前端工程（`src/volur-lite-view-NEXT`）与底层安全服务（`volur-security`）的用户体验与状态机架构进行审计与调试时，定位到以下两个关键问题：

1. **选择备选登录方式时的双重遮罩与界面闪烁问题（Double-Scrim Flicker）**：
   - 用户在 `/v1/signin/challenge/selection`（选择登录核验方式页面）点击任意核验选项（如“使用安全密钥或通行密钥”）时，页面发生了连续两次遮罩层（Scrim）与顶部加载进度条的淡入与淡出，产生了严重的闪屏（Flicker）感知缺陷；
   - **根本原因定位**：在 `selectionView.vue` 的 `handleSelectMethod` 方法中，显式调用了 `await signinFlowStore.triggerScrim(undefined, 1000)`，等待 1 秒空动画结束并关闭遮罩；紧接着调用的 `signinFlowStore.selectChallenge(item.type)` 内部又由 `withLoading(async () => { ... }, 1000)` 包裹。这导致在同一点击动作后，连续触发了两个独立的、串行的最小 1 秒遮罩周期（遮罩显现 -> 消失 -> 又显现 -> 消失 -> 路由跳转）。

2. **验证方式横向切换时 CID 步进模型与业务层级脱节问题**：
   - 原 `ChallengeServiceImpl.selectChallenge` 采用简单的无差别步进 `int nextCid = cid + 1`；
   - 从认证漏斗深度与因子层级模型（Stage Depth / Funnel Level）审视：
     - `cid = 0` 代表身份标识阶段（Identifier）；
     - `cid = 1` 代表第一因子核验阶段（Primary Challenge），在此阶段，密码（`pwd`）、通行密钥（`pk`）、邮箱验证码（`etotp`）属于平行的同级备选方案；
     - 用户在选择页进行横向分支切换时，并未完成第一因子，也并未纵向深入到下一阶段（如 2FA 或终态认证），因此该阶段的 `cid` 理应保持为 `1`；
     - 原设计中切换一次验证方式导致 `cid` 递增，破坏了层级深度语义的自解释性。

---

## 2. 核心改动清单与实现细节

### 2.1 前端工程：消除冗余遮罩，实现单次平滑过渡 (`src/volur-lite-view-NEXT`)

- **文件**：[`selectionView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/challenge/selectionView.vue)
- **改动点**：
  - 移除 `handleSelectMethod` 中多余的 `await signinFlowStore.triggerScrim(undefined, 1000)` 显式调用；
  - 异步请求与加载反馈全权交由 `signinFlowStore.selectChallenge` 内部的 `withLoading` 统一管理；
  - 保持纯客户端切页按钮（`handleTryAnotherWayInSelection` 与 `handleCancelSelection`）原有的 `triggerScrim` 体验；
  - 彻底消除了选项点击后的双重遮罩与闪屏异常，恢复了一次性流畅加载过渡。

### 2.2 后端工程：同级分支横向切换保持 CID 不变 (`volur-security`)

- **文件**：[`ChallengeServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/main/java/com/humolix/volur/security/service/impl/ChallengeServiceImpl.java)
- **改动点**：
  - 在 `selectChallenge(ChallengeSelectRequestDTO request)` 中，将 `int nextCid = cid + 1;` 调整为 `int nextCid = cid;`；
  - 保持当前因子阶段序号，通过 `transitionLockManager.nextTransLock(dsh, tl, cid, currentStep, nextCid, targetType, identifier)` 完成目标挑战分支的切换与新票据生成；
  - 返回的 `SignInFlowResponseVO` 中 `cid` 保持为当前层级序号。
- **单测同步更新**：
  - 同步更新 [`ChallengeServiceImplTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-security/src/test/java/com/humolix/volur/security/service/impl/ChallengeServiceImplTest.java) 中 `selectChallenge_toEtotp_shouldSendEmailAndAdvanceTL` 测试用例中对 `nextTransLock` 传参与返回值中 `cid` 的断言预期（由期望 `2` 调整为期望 `1`）。

---

## 3. 架构安全性与正确性论证

1. **防重放与票据作废机制依然严格可靠**：
   - 尽管横向切换时 `nextCid = cid` 保持一致，但在生成新事务锁 `nextTL` 时，`step` 已经从旧步骤（如 `"pwd"`）切换为新目标步骤（如 `"pk"`），且内部包含当前毫秒时间戳与随机高熵 `nonce`；
   - 生成的新 `TL` 签名负载为全新的不可伪造 Token，且 Redis 中会话的 `activeTL` 立即被覆写，旧 `TL` 瞬间失效，重放防御与防串改机制完好无损。
2. **状态机全链路兼容性验证**：
   - 当用户在横向切换后提交新方式（如输入通行密钥或邮箱验证码）时，客户端携带保持一致的 `cid` 进行校验，`validateTransLock(dsh, tl, cid, currentStep)` 顺利匹配；
   - 验证通过后推进至 `STEP_AUTHENTICATE` 时，`verify*` 方法执行 `cid + 1` 纵向深入，状态机平滑推进，全链路闭环顺畅。

---

## 4. 验证结论

1. **后端单元测试**：
   - 运行 `mvn test -pl volur-security,volur-auth`：`volur-security` 全量 85 个用例与 `volur-auth` 全量 47 个用例全部 100% 通过（**132/132 全绿，0 失败，0 错误**）。
2. **前端编译与类型检查**：
   - 运行 `npm run build`：`vue-tsc --noEmit && vite build` 耗时 2.66s，0 错误、0 警告构建成功。
