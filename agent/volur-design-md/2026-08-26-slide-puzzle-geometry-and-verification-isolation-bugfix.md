# 滑块拼图（SLIDE_PUZZLE）几何渲染与万能校验漏洞深度修复

## 1. 问题根因分析

经过系统性排查与端到端链路溯源，发现导致“缺口绘制异常”与“无论滑动多少均验证通过”的两个关键隐患与缺陷：

### 1.1 缺口绘制与图片尺寸/坐标系异常根因
1. **几何多边形绘制闭合严重错误**：
   - 在 [`SlidePuzzleEngine.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/captcha/engine/SlidePuzzleEngine.java) 的 `createPuzzleShape` 中，底边绘制完毕后误跳回 `(x + size, y + size)`，且左侧缺失凸凹扣路径，直接以对角线 `lineTo(x, y)` 闭合，导致生成的拼图块呈异常斜切破损状态；
2. **前后端画布分辨率失真与位移失真**：
   - 后端 [`PuzzleImageLoader`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/captcha/engine/PuzzleImageLoader.java) 生成尺寸为 $280 \times 150$，而前端 [`SlidePuzzleModal.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/components/verification/SlidePuzzleModal.vue) 容器为 $320 \times 160$，`object-fit: cover` 缩放导致前端视觉缺口像素与后端绝对目标值 $X$ 产生 $\approx 1.14$ 倍偏差；
   - 滑块块尺寸切图使用单一固定截取宽度，且前端叠加了 `top: yPosition`，导致在全高度透贴画布上发生二次垂直偏移。

### 1.2 无论滑动多少均提示验证成功的“万能校验”根因
1. **CloudTurnstile 盲目宽松放行与全 Provider 轮询漏洞**：
   - 在 [`CaptchaServiceImpl.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/service/impl/CaptchaServiceImpl.java) 中，`verifyAndGetMatchedType` 循环遍历了所有 Provider；
   - 当用户滑动距离错误导致 `SlidePuzzleCaptchaProvider.verify()` 返回 `false` 后，循环继续向下轮询至 [`CloudTurnstileCaptchaProvider`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/captcha/provider/CloudTurnstileCaptchaProvider.java)；
   - 原 `CloudTurnstileCaptchaProvider.verify` 在未配置 `TURNSTILE_SECRET_KEY` 时，对**任意非空 userInput 均无条件返回 `true`**，且此前未校验 Redis 中是否真正由 Turnstile 签发过该 `captchaId`；
   - 结果：错误的滑块偏移量被 Turnstile 盲目放行，并作为 Level 4（最高等级）凭据颁发了 `CaptchaTicket`，造成“乱滑都能过”的严重漏洞。

---

## 2. 修复措施与实施方案

### 2.1 修复几何路径与 1:1 像素映射
1. **修正 `createPuzzleShape` 路径**：
   - 顶部（外凸）、右侧（外凸）、底部（内凹）、左侧（内凹）四边采用贝塞尔曲线平滑绘制，各顶点首尾相连，保证拼图块几何闭合完整且圆润；
2. **前后端统一 $320 \times 160$ 像素画布**：
   - [`PuzzleImageLoader`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/captcha/engine/PuzzleImageLoader.java) 与 [`SlidePuzzleEngine`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/captcha/engine/SlidePuzzleEngine.java) 基准宽高统一调整为 `320 x 160`；
   - 后端下发 $320 \times 160$ 的透明 PNG 滑块图（滑块块位于 `(0, targetY)`），前端 [`SlidePuzzleModal.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/components/verification/SlidePuzzleModal.vue) 采用 `transform: translateX(${sliderX}px)` 平移，彻底消除缩放和垂直偏差。

### 2.2 修复 Provider 隔离与 Turnstile 安全核验
1. **注入 Redis 隔离验证**：
   - [`CloudTurnstileCaptchaProvider`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/captcha/provider/CloudTurnstileCaptchaProvider.java) 在 `generate` 时将 `captchaId` 存入 Redis `vredis:verification:captcha:turnstile:{captchaId}`；
   - 在 `verify` 时，若 Redis 中不存在该 Turnstile 专用凭据，直接判定 `false`；
2. **滑块容差精确比对**：
   - 滑块必须满足 $|userX - targetX| \le 6\text{px}$ 且消费有效 Redis 键才能通过验证，滑动不正确时各 Provider 均返回 `false`，前端收到 400 提示并触发抖动刷新。

---

## 3. 验证结论
- **前后端编译与打包**：`npm run build` 和 `mvn test`（全工程 11 个模块）100% 编译通过，所有单元测试无异常。
- **安全审计**：滑块校验已恢复严格的像素级差值匹配与 Provider 独立隔离防伪。
