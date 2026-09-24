# 施工记录：修复登录直接提交无图漏洞与安全验证触发机制优化

## 1. 缺陷背景与根因定位

### 1.1 直接点击登录弹出滑动拼图无图漏洞
- **表象**：用户在登录页输入账号与密码后，未点击“点击进行安全验证”按钮，而是直接点击“登录”按钮。此时前端自动唤起滑动拼图弹窗，但弹窗内部为空白，没有发出 `/api/v1/captcha/challenge` 请求获取背景图和拼图滑块碎片。
- **根因分析**：
  1. 在 `SlidePuzzleCaptcha.vue` 中，挑战图片的拉取原本完全依赖 `watch(modalVisible)`。当调用 `openModal()` 时，组件仅设置了 `modalVisible = true`，并未显式执行 `fetchChallenge()`；
  2. 若当前组件在初始挂载时 `props.show` 即为 `true`（或异步组件刚完成渲染），Watcher 默认不会以 `immediate` 模式触发执行，造成弹窗打开而请求从未发出的死锁状态；
  3. 在 `VolurCaptcha.vue` 的 `verifyBehavior()` 中，当处于 `SLIDER` 模式时，原逻辑盲目创建了第二个 `behaviorSliderRef` 异步组件实例，而忽略了页面上早已存在的 `inlineSliderRef`，导致异步组件挂载与时序紊乱。

### 1.2 触发按钮图标冗余
- **表象**：滑动验证码内联激活按钮在未验证状态下显示了“🛡️”盾牌 emoji 图标。
- **优化**：按照产品交互规范，剔除“🛡️”图标，仅显示“点击进行安全验证”文字；核验通过后呈现绿色“✓ 安全验证通过”。

---

## 2. 修复方案实施

1. **[`SlidePuzzleCaptcha.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/components/captcha/SlidePuzzleCaptcha.vue)**：
   - 移除了未验证状态下的 `<span v-else>🛡️</span>`，纯文本展示 `点击进行安全验证`；
   - 在 `openModal(scene?: string)` 中显式添加 `fetchChallenge()` 调用，确保无论任何方式（用户点击按钮唤起或直接点击登录表单触发）打开弹窗，必定立即拉取人机拼图数据；
   - 补充 `watch(() => props.show, ..., { immediate: true })` 与 `onMounted(() => { if (modalVisible.value) fetchChallenge() })`，杜绝任何生命周期边界情况下的请求遗漏。

2. **[`VolurCaptcha.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/components/captcha/VolurCaptcha.vue)**：
   - 在 `verifyBehavior(customScene)` 中增加判断：若当前已处于内联 `SLIDER` 模式，直接复用已挂载的 `inlineSliderRef.value.openModal(targetScene)`，避免重复创建虚拟 DOM 和异步组件。

3. **[`AuthView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/auth/AuthView.vue)**：
   - 优化 `handleAction()` 的异常捕获：过滤用户主动关闭/取消弹窗的 `User cancelled verification` 错误，避免将其误判为登录失败并错误递增风控计数。

---

## 3. Chrome DevTools MCP 环境默认展示滑动验证码归因分析
针对提问：“在通过 chrome-devtools-mcp 打开的浏览器中为什么默认就是滑动验证码而不是图形验证码，这是否是触发了 TLS 还是 Canvas？”

深度审计后端风控链路（`RiskEvaluationEngine.java`、`CaptchaSDKImpl.java`）与前端探针（`fingerprint/browser.ts`、`fingerprint/webgl.ts`、`fingerprint/canvas.ts`），得出确切结论：

- **既不是 TLS 也不是 Canvas 触发，而是触碰了“无头/自动化浏览器惩罚策略 (Headless Browser / Automation Detection)”**：
  1. **自动化探针特征捕获**：`chrome-devtools-mcp` 是通过 Chrome DevTools Protocol (CDP) / Puppeteer 驱动控制 Chromium 实例的。在自动化驱动环境下，浏览器全局对象中默认暴露了 `navigator.webdriver === true`。
  2. **前端指纹生成**：`src/utils/fingerprint/browser.ts` 探针检测到 `Boolean(navigator.webdriver) === true`，随即判定 `isSuspiciousAutomation = true`，并将设备指纹载荷中的 `hl` (Headless/Automation 标识位) 置为 `1`。
  3. **后端无头浏览器严控**：后端 `RiskEvaluationEngine.evaluateDeviceRisk()` 解码 `hl == 1` 并触发规则：
     ```java
     if (profile.isHeadless() && isHeadlessPenaltyEnabled()) {
         return RiskLevel.HIGH;
     }
     ```
  4. **风控跃迁至滑动拼图**：`CaptchaSDKImpl` 评估出综合风控等级为 `RiskLevel.HIGH`，自动将默认基准的 `IMAGE`（图形验证码）自适应升级为 `SLIDER`（行为式滑块拼图）。
