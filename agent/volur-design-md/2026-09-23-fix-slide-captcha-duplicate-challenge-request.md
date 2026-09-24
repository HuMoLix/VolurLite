# 施工记录：修复点击安全验证重复发起两遍挑战请求缺陷

## 1. 缺陷表象与原因剖析
- **表象**：在登录页面用户点击“点击进行安全验证”按钮时，浏览器网络面板显示连续发起了两次 `GET /api/v1/captcha/challenge` 请求，且第二次请求携带了第一次请求刚返回的 `oldCaptchaId`（如 `d5bffcddc30a469cb0d2995ccfc42712`）。
- **根因分析**：
  在 [`SlidePuzzleCaptcha.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/components/captcha/SlidePuzzleCaptcha.vue) 中，之前为了确保弹窗一定拉取数据，在多个生命周期和 Watcher 中重复埋入了 `fetchChallenge()`：
  1. `openModal()` 方法内部调用了 `fetchChallenge()`（请求 1）；
  2. `openModal()` 将 `modalVisible.value` 置为 `true`，触发了 `watch(modalVisible)` 回调，该回调内部又调用了一次 `fetchChallenge()`（请求 2）；
  3. 请求 1 响应后在 `captchaData.value` 中存储了 `captchaId`，导致紧随其后的请求 2 带有 `oldCaptchaId=...`。
  4. 此外，`fetchChallenge()` 缺少在途（in-flight）并发防护，导致两次请求均被真实发出。

---

## 2. 修复措施

在 [`SlidePuzzleCaptcha.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/components/captcha/SlidePuzzleCaptcha.vue) 中实施了统一的单请求控制机制：

1. **唯一触发源收敛**：
   - 将弹窗打开时的验证码拉取职责统一收敛至 `watch(modalVisible, ..., { immediate: true })`；
   - 彻底移除 `openModal()` 内部的直接 `fetchChallenge()` 调用，改为通过将 `modalVisible.value = true` 由 Watcher 单一驱动；
   - 移除 `props.show` 监听器中的重复 `fetchChallenge()` 调用。

2. **请求防重入守卫 (In-flight Guard)**：
   - 在 `fetchChallenge()` 方法首行加入 `if (loading.value) return` 守卫，杜绝在途请求尚未完成时的任何并发调用。

3. **状态清理闭环**：
   - 当 `modalVisible` 变为 `false`（弹窗关闭）时，清空 `captchaData.value = null`，确保下一次打开弹窗时从全新挑战开始，避免残留无效的历史 ID。

---

## 3. 验证结果
执行全量类型检查与构建打包：
- `vue-tsc --noEmit && vite build`: 0 错误，耗时 6.55s，产物构建成功。
- 点击“点击进行安全验证”时，只会触发一次纯净的 `/api/v1/captcha/challenge` 请求。
