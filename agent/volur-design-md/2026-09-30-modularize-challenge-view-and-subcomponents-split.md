# ChallengeView 架构拆分与多挑战子视图模块化收敛

## 1. 变更背景与架构解耦目标

在 Volur Lite NEXT 的阶段化身份认证流（Google Accounts VLIF 规范）中，原有安全挑战页面 [`ChallengeView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/ChallengeView.vue) 曾聚合了所有安全挑战分支与辅助流程，单文件体积超过 1120 行。在该结构下：
1. **状态与变量过度拥挤**：密码状态、人机验证沙箱 Token/Ticket、设备时间与特征、2FA 动态口令、选择列表分页加载、被拒文案多重解码全部杂糅在同一组件上下文中；
2. **逻辑耦合严重**：各挑战类型的表单校验、防重提交、错误自愈与跳转行为互相交错，增加了后续维护和扩展新挑战类型（如通行密钥 WebAuthn / Passkey、硬件密钥等）的阻力；
3. **视觉与交互规范复用成本高**：左侧账号芯片（`auth-account-chip`）、卡片标题、不同分支的操作按钮（`.btn-try-another-way`、`.btn-verify-action`）样式重复度高。

因此，按照架构解耦原则，将所有 challenge 分支、selection 页面及 reject 页面拆分至独立子组件，收敛于 `src/views/auth/signin/challenge/` 目录，并由 `ChallengeView.vue` 统一作为响应式调度器与布局承载器根据需求按需渲染。

---

## 2. 架构设计与拆分工程落地

### 2.1 目录结构与职责分工

子视图统一收敛在 `src/volur-lite-view-NEXT/src/views/auth/signin/challenge/`：

```
src/views/auth/signin/
├── ChallengeView.vue              # 调度与承载容器：管理左侧账号芯片、Title/Subtitle及顶层安全拦截守卫
├── IdentifierView.vue             # 第一阶段：账号输入与风控探测
└── challenge/                     # 各细分挑战与流程子组件
    ├── pwdChallengeView.vue       # 密码安全挑战
    ├── recaptchaChallengeView.vue # 人机安全核验
    ├── deviceChallengeView.vue    # 新设备/异常环境确认挑战
    ├── totpChallengeView.vue      # 两步验证 (2FA / 动态口令)
    ├── selectionView.vue          # 多维身份核验方式选择列表
    ├── rejectView.vue             # 拒绝核验终态页
    └── index.ts                   # 统一导出层 (CamelCase 与 PascalCase 兼容导出)
```

### 2.2 各子组件职责定义

1. **`pwdChallengeView.vue`**：
   - 负责密码表单输入与响应式状态（`password`、`showPassword`）；
   - 对接 `GoogleTextField`，并监听输入变化执行错误自动抹除；
   - 包含“忘记密码？”重置入口引导；
   - 底部操作：“试试其他方式”（带 Scrim 过渡至 selection）与“下一步”（触发 `authFlowStore.submitChallenge('pwd', ...)`）。

2. **`recaptchaChallengeView.vue`**：
   - 挂载 `<Recaptcha>` 人机核验组件，双向绑定 `passed`、`token`、`ticket` 与 `errorMsg`；
   - 监听人机验证通过事件自愈错误；
   - 当后端判定用户不存在（`nextStep === 'identifier'`）时，注入错误并安全重定向回账号输入阶段；
   - 点击“试试其他方式”流转至 `rejected` 终态。

3. **`deviceChallengeView.vue`**：
   - 展示登录时间、客户端特征与会话 DSH 状态；
   - 提供确认核验提交与“试试其他方式”备选方案流转。

4. **`totpChallengeView.vue`**：
   - 负责 6 位动态验证码输入与设备信任复选框（“在此设备上信任 30 天”）；
   - 提交 `authFlowStore.submitChallenge('2fa', ...)` 并处理成功或失效响应；
   - 提供“改用其他方式”与底部“试试其他方式”双入口。

5. **`selectionView.vue`**：
   - 维护选择列表分页状态（每页 2 项 + 1 项“试试其他方式”翻页项）；
   - 调用 `fetchChallengeSelectionApi` 动态拉取当前会话可用的备选核验手段（排查当前已处于的挑战）；
   - 条目点击与翻页统一通过 `authFlowStore.triggerScrim` 唤起平滑等待与路由下发；最后一页翻页流转至 `rejected`。

6. **`rejectView.vue`**：
   - 针对 URL 参数传输的 `rejectedmsg` 进行鲁棒的安全递归解码；
   - 提供“返回”（返回账号输入页）与“重试”（带 Scrim 返回第一页选择列表）操作。

7. **`index.ts`**：
   - 严格遵循单一真实源与 Vue SFC 导出规范，统一导出大驼峰 PascalCase 组件（`PwdChallengeView`、`RecaptchaChallengeView`、`DeviceChallengeView`、`TotpChallengeView`、`RejectView`、`SelectionView`），杜绝别名双写与重复导出残留。

### 2.3 `ChallengeView.vue` 调度器演进

重构后的 `ChallengeView.vue` 由 1125 行精简至约 150 行，专门聚焦顶层调度职责：
- **公共左侧容器收敛**：持有 `cardTitle`、`cardSubtitle` 计算属性以及账号气泡芯片（`auth-account-chip`，展示首字母头像、标识脱敏、切换账号点击）；
- **动态组件渲染**：通过 `computed(() => activeChallengeComponent)` 根据 `route.params.type` 自动解析并匹配渲染对应的子视图；
- **全流程路由守卫**：在生命周期 `onMounted` 中统一执行会话参数补全（`ensureFlowParams`）、账号存在性校验与 TL 状态锁流转票据校验（非 `rejected` 状态下校验），失效自动拦截回退。

---

## 3. 设计规范与交互铁律遵从性

1. **纯粹代码与零注释铁律**：
   - 所有新建与重构的文件（`pwdChallengeView.vue`、`recaptchaChallengeView.vue`、`deviceChallengeView.vue`、`totpChallengeView.vue`、`selectionView.vue`、`rejectView.vue`、`index.ts`、`ChallengeView.vue`）均做到 100% 零注释，代码凭借清晰的命名与完备的结构自解释；
2. **Google Material Theme 视觉一致性**：
   - 严格采用 Google 经典主品牌蓝 `#0b57d0`（暗色 `#8ab4f8`）、柔和浅灰底色 `#f8f9fa` 与暗色 `#202124`，正文文本严禁使用粗暴全黑；
3. **输入自愈错误生命周期**：
   - 提交流程不强制清空错误，避免 DOM 尺寸剧烈伸缩造成卡片抖动；错误由 `watch` 监听输入实质变化精准驱动自愈；
4. **互斥防重锁 (`isSubmitting`)**：
   - 所有表单提交动作均配置物理并发锁，短路高频连击。

---

## 4. 验证结果

1. **静态类型与生产构建验证**：
   - 在 `src/volur-lite-view-NEXT` 运行 `npm run build`（`vue-tsc --noEmit && vite build`）；
   - 结果：**`✓ 2827 modules transformed. ✓ built in 2.62s`**，编译与类型检查 100% 通过，无任何 TypeScript 报错或打包告警。
