# 实现行为式滑块拼图验证码弹窗 (SlidePuzzleModal) 施工记录

**施工日期**：2026-08-26  
**施工模块**：`src/volur-lite-view/src/components/verification/SlidePuzzleModal.vue`、`src/volur-lite-view/src/components/auth/AuthForm.vue`、`src/volur-lite-view/src/types/verification/index.ts`、`src/volur-lite-view/src/locales/zh_CN/auth.json`、`src/volur-lite-view/src/locales/en_US/auth.json`  
**参考后端**：`src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/captcha/provider/SlidePuzzleCaptchaProvider.java`  
**责任主体**：Antigravity AI Agent

---

## 1. 需求背景与设计目标

为配合后端 [`SlidePuzzleCaptchaProvider.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/captcha/provider/SlidePuzzleCaptchaProvider.java) 与 [`CaptchaController.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/controller/v1/CaptchaController.java)，在前端构建现代化、高颜值且兼具无缝响应式的 **SlidePuzzle 行为式滑块拼图人机核验弹窗**。

---

## 2. 核心架构与功能实现

### 2.1 组件封装 ([`SlidePuzzleModal.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/components/verification/SlidePuzzleModal.vue))
1. **画布拼图展示区**：
   - 动态渲染背景图（`imageBase64`，宽 320px、高 160px，包含拼图凹槽）；
   - 动态渲染滑动拼图碎片（`sliderBase64`），绝对定位在 `yPosition`，横向位置随滑块移动实时偏移。
2. **手势与拖动控制体系**：
   - 支持鼠标（`mousedown` / `mousemove` / `mouseup`）与触屏设备（`touchstart` / `touchmove` / `touchend`）手势拖动；
   - 拖动时同步渲染滑轨高亮填充进度条，滑动手柄内置方向指示；
   - 松手时精确计算滑动位移 `userInput = String(Math.round(sliderX))` 与拖拽耗时 `costTime = Date.now() - startTime`。
3. **接口核验与人机凭证核发 (Ticket Flow)**：
   - 调用 `POST /api/v1/captcha/verify-ticket`；
   - **验证成功**：手柄与滑轨转为翡翠绿，顶部浮现耗时通过提示，向父组件触发 `@success(ticketVO: CaptchaTicketVO)`，并在 600ms 后平滑关闭；
   - **验证失败**：触发画布左右抖动振动动画，手柄转为警示红，自动回弹并在 800ms 后自动刷新下一张全新拼图。
4. **一键旋转刷新**：
   - 弹窗顶部提供快速刷新按钮，支持随时切换当前验证码挑战。

---

## 3. 验证与构建测试

在 `src/volur-lite-view` 目录下执行全量构建：
```powershell
npm run build
```
**验证结果**：
- `vue-tsc --noEmit`：0 错误；
- `vite build`：生产环境打包成功（耗时 ~2.44s）。

---

## 4. 影响范围与审计评估

- **改动文件**：
  - `src/volur-lite-view/src/components/verification/SlidePuzzleModal.vue`
  - `src/volur-lite-view/src/components/auth/AuthForm.vue`
  - `src/volur-lite-view/src/types/verification/index.ts`
  - `src/volur-lite-view/src/locales/zh_CN/auth.json`
  - `src/volur-lite-view/src/locales/en_US/auth.json`
- **安全性与体验**：
  - 完美契合 Volur Lite 人机验证场景，支持高安全性行为式校验，交互体验自然流畅。
