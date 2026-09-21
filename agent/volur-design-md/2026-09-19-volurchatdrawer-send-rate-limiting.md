# VolurChatDrawer 消息发送频率限制 (1s 限频) 与按钮倒计时设计与施工文档

**日期**：2026-09-19  
**类型**：前端交互安全加固与频率控制 / UI 倒计时反馈优化  
**影响组件**：
- [`VolurChatDrawer.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/components/VolurChatDrawer.vue)（前台平台自研抽屉式实时聊天组件 `volurEx`）

---

## 1. 变更背景与需求说明

在竞赛或实时交流场景中，为防止选手/队员在比赛抽屉聊天框中连续快速刷屏、误触连击或恶意通过脚本极速发信导致服务器 WebSocket 压力过载，并提供直观清晰的用户交互反馈：
1. **频率阈值**：最快 1s 才能发送一条消息（1000ms 冷却间隔）；
2. **按钮动态倒计时**：
   - 发送按钮在冷却期内直观展示剩余倒计时间（如 `发送 (1s)`），让用户明确获知何时可再次发送；
   - 发送进行中展示 `发送中`，闲置状态恢复标准 `发送`；
   - 按钮样式锁定 `min-width: 88px;`，杜绝文字动态切换时导致左侧多行输入框宽度抖动（CLS 布局位移）；
3. **用户交互体验**：
   - 发送中及处于冷却期内，底部发送按钮自动置灰禁用（`disabled` 态，光标展示 `not-allowed`，半透明度反馈）；
   - 若用户在 1 秒冷却时间内连续敲击 `Enter` 键发送，予以防抖弹窗友好告警（`message.warning('发送频率过快，请等待 1 秒后再发送')`），同时保留输入框内已编辑的内容，不导致文本丢失；
   - 异步在途请求中通过 `sending` 锁保证单飞保护，防止网络慢时重复发包；
   - 组件销毁时妥善清理 `cooldownTimer` 定时器，杜绝内存泄漏。

---

## 2. 具体改动点分析

### 2.1 状态声明与定时器管理 ([`VolurChatDrawer.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/public/volurEx/components/VolurChatDrawer.vue))

1. **响应式状态与倒计时器**：
   - `SEND_COOLDOWN_SECONDS = 1`：统一定义 1 秒冷却阈值常量；
   - `cooldownSeconds = ref<number>(0)`：记录当前剩余秒数响应式变量；
   - `lastSendTime = ref<number>(0)`：记录上一次发送成功的精确时间戳；
   - `isCoolingDown = ref<boolean>(false)`：标记是否处于发送冷却状态；
   - `sending = ref<boolean>(false)`：标记当前是否有发信异步在途请求；
   - `cooldownTimer`：持有 `setInterval` 句柄；
   - `startCooldown()`：负责激活冷却、赋初始秒数并在每秒递减至 0 时安全复位与销毁定时器；
   - `notifyRateLimit()`：集成 800ms 防抖提示，动态提示剩余等待秒数；
   - `sendButtonText`：计算属性，统一收敛状态渲染文本：
     ```ts
     const sendButtonText = computed(() => {
       if (sending.value) return '发送中'
       if (isCoolingDown.value) return `发送 (${cooldownSeconds.value}s)`
       return '发送'
     })
     ```
2. **生命周期回收**：
   - 引入 `onUnmounted` 生命周期钩子，组件卸载时若存在活动中的 `cooldownTimer` 立即执行 `clearInterval` 清理。

### 2.2 按钮视觉与交互稳定性优化

1. **`canSend` 计算属性联动**：
   ```ts
   const canSend = computed(() => {
     return (
       isChatReady.value &&
       inputContent.value.trim().length > 0 &&
       !sending.value &&
       !isCoolingDown.value
     )
   })
   ```
2. **防止布局抖动的尺寸定宽**：
   - 为 `.volur-chat-send-btn` 增加 `min-width: 88px;` 与 `padding: 0 14px;`，确保“发送”、“发送中”、“发送 (1s)”三种状态切换时尺寸恒定，输入框平稳不抖动。
3. **键盘回车事件拦截 (`handleKeyDown`)**：
   - 当检测到用户敲击 `Enter` 键且处于冷却或发送中时，阻止默认换行并触发 `notifyRateLimit()` 提示，直接中断调用。

---

## 3. 验证结果

1. **编译构建校验**：
   - 执行 `npm run build`，严格通过 `vue-tsc --noEmit` 校验，打包成功（用时 5.95s，0 个编译错误）。
2. **功能行为确认**：
   - 正常发送 1 条消息后，发送按钮变为 `发送 (1s)` 并自动置灰 1s；
   - 1s 冷却结束后，按钮平滑恢复高亮并恢复为 `发送` 文案；
   - 倒计时过程中文字框宽度平稳，无任何跳动与位移。
