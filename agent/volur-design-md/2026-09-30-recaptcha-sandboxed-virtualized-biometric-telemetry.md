# reCAPTCHA 独立沙箱组件与多维人机行为核验系统设计

## 1. 方案背景与目标

在原有的 Google 风格登录挑战流程 (`/v1/signin/challenge/recaptcha`) 中，reCAPTCHA 验证组件与 `ChallengeView.vue` 紧耦合在一起，缺乏运行环境隔离与底层反爬/反脚本探测能力。简单的纯前端模拟勾选容易被自动化测试脚本（如 Puppeteer、Playwright、Selenium 等）直接通过 `element.click()` 或 `dispatchEvent` 瞬间绕过。

为了实现工业级的人机对抗防护，本次重构将 `recaptcha-widget` 提取为平台级通用安全组件 [`recaptcha.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/security/recaptcha.vue)，置于 `src/components/security/` 目录下，并建立起如下核心防护机制：
1. **独立 `<iframe>` 沙箱与虚拟化混淆环境**：验证卡片运行在受限沙箱中，内部构建虚拟指令执行环境，主页面与沙箱通过结构化 `postMessage` 安全通道通信；
2. **多维行为生物识别 (Biometrics)**：实时采集分析鼠标轨迹序列 $(x_i, y_i, t_i)$、加速度方差、曲率方差、贝塞尔平滑度、机械直线、匀速瞬移、点击停留时长与二维高斯落点分布；
3. **环境与设备指纹核验 (Fingerprinting)**：前置采集 WebGL/Canvas/AudioContext 指纹，深度探测 CDP 注入变量、`navigator.webdriver`、自动化框架原型链修改以及原生 API (`Function.prototype.toString`、`document.createElement` 等) Hook 篡改；
4. **浏览器生态与历史持久性探测 (Ecosystem)**：探测导航历史长度 `history.length`、存储配额 `navigator.storage.estimate()`、IndexedDB 数据库持久性与 Navigation Timing，识别一次性无痕实例。

---

## 2. 总体架构设计

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            宿主组件: recaptcha.vue                          │
│                                                                             │
│  - 全局鼠标移动采样环形缓冲区 (Ring Buffer: x, y, t)                         │
│  - 主题动态同步 (Google Light #f9f9f9 / Dark #292a2d)                        │
│  - 双向状态驱动: v-model:passed, v-model:token, @success, @error             │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ postMessage (allow-scripts allow-same-origin)
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      单独 <iframe> 沙箱隔离运行环境                          │
│                                                                             │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                    虚拟化混淆与多维遥测执行引擎                       │  │
│  │                                                                       │  │
│  │  1. 行为生物识别矩阵 (Biometrics Pipeline)                            │  │
│  │     - 轨迹序列合成 (宿主接近轨迹 + 沙箱内部微操轨迹)                  │  │
│  │     - 速度 profile 与加速度方差计算 (σ_a^2)                            │  │
│  │     - 向量偏角变化与曲率方差计算 (σ_κ^2)                               │  │
│  │     - 贝塞尔曲线平滑度 / 弦线最大几何偏差检测                         │  │
│  │     - 机械直线 / 匀速瞬移 (dt < 8ms, d > 50px) / 零轨迹点击检测       │  │
│  │     - 事件真实性 (event.isTrusted) 强约束                             │  │
│  │     - 鼠标按压耗时 Δt = t_up - t_down (正常 50~250ms)                 │  │
│  │     - 点击几何中心偏离半径 r 与二维高斯分布概率核验                   │  │
│  │                                                                       │  │
│  │  2. 环境与设备指纹矩阵 (Environment Matrix)                           │  │
│  │     - CDP (cdc_*, __webdriver_*) 变量深度枚举                         │  │
│  │     - navigator.webdriver 与属性描述符原型链篡改检测                  │  │
│  │     - WebGL 调试渲染器检测 (拦截 SwiftShader, llvmpipe 软光栅)        │  │
│  │     - 2D Canvas 与 Web AudioContext 频域特征哈希                      │  │
│  │     - 屏幕多显示器与硬件配置 (CPU Concurrency, Device Memory) 一致性  │  │
│  │     - 原生 JavaScript 函数 Hook 探测 (Function.prototype.toString 检验)│
│  │                                                                       │  │
│  │  3. 浏览器生态与历史持久性核验 (Ecosystem Probe)                      │  │
│  │     - 页面历史长度 window.history.length > 1                          │  │
│  │     - 真实磁盘存储配额占用 navigator.storage.estimate().usage         │  │
│  │     - 长期 IndexedDB 库存在性检测                                     │  │
│  │     - Performance Navigation Timing 延迟真实度评估                    │  │
│  │                                                                       │  │
│  │  4. 综合风险决策与凭证组装                                            │  │
│  │     - 风险分 Risk ∈ [0.0, 1.0]，阈值判定 (< 0.45 放行)                │  │
│  │     - 签发携带行为矩阵防篡改验签 Token: v4_rc_<base64>                │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. 详细检测机制与数学原理

### 3.1 行为生物识别与鼠标轨迹动力学 (Mouse Trajectory & Biometrics)

1. **轨迹坐标序列与动力学特征**：
   - 设鼠标从起始点进入卡片并点击复选框产生的轨迹点集为 $P = \{(x_0, y_0, t_0), (x_1, y_1, t_1), \dots, (x_n, y_n, t_n)\}$。
   - 步长位移与速度计算：
     $$\Delta d_i = \sqrt{(x_i - x_{i-1})^2 + (y_i - y_{i-1})^2}, \quad \Delta t_i = \max(1, t_i - t_{i-1}), \quad v_i = \frac{\Delta d_i}{\Delta t_i}$$
   - 加速度与加速度方差：
     $$a_i = \frac{v_i - v_{i-1}}{\Delta t_i}, \quad \bar{a} = \frac{1}{n-1}\sum_{i=2}^n a_i, \quad \sigma_a^2 = \frac{1}{n-1}\sum_{i=2}^n (a_i - \bar{a})^2$$
     真实人类操控鼠标具有费茨法则（Fitts's Law）所刻画的自然加减速过程（钟形速度分布），若 $\sigma_a^2$ 极小（趋近于 0）且位移大于阈值，说明速度恒定，属于脚本构造的机械运动。

2. **向量偏角变化与曲率方差 (Curvature Variance)**：
   - 计算连续速度向量 $\vec{u}_i = (x_i - x_{i-1}, y_i - y_{i-1})$ 与 $\vec{v}_i = (x_{i+1} - x_i, y_{i+1} - y_i)$ 之间的夹角偏转 $\Delta \theta_i = \operatorname{atan2}(v_{i,y}, v_{i,x}) - \operatorname{atan2}(u_{i,y}, u_{i,x})$。
   - 离散曲率 $\kappa_i = \frac{|\Delta \theta_i|}{\Delta d_i}$，并计算曲率方差 $\sigma_\kappa^2$。
   - 若位移足够大且 $\sigma_\kappa^2 < 0.0001$，且轨迹点到端点连线的最大垂直几何偏差 $dev_{\max} < 0.65\text{px}$，系统判定为**绝对机械直线**。

3. **异常瞬移与零移动打击 (Teleportation & No-movement Detection)**：
   - 若某一段采样间隔 $\Delta t_i < 8\text{ms}$ 且位移 $\Delta d_i > 50\text{px}$，判定为非生物性瞬移；
   - 若在复选框被触发前，采集到的有效轨迹点数 $n < 3$ 或累计运动距离 $\sum \Delta d_i < 5\text{px}$，判定为直接派发点击事件（零运动）。

4. **点击事件元参数与落点分布**：
   - **`event.isTrusted` 铁律**：若 `isTrusted !== true`（由 `dispatchEvent` 或 `element.click()` 派发），直接赋予 `Risk = 1.0` 绝对阻断。
   - **按键停留时长**：$\Delta t_{\text{click}} = t_{\text{up}} - t_{\text{down}}$。正常人类微操平均在 $50\text{ms} \sim 250\text{ms}$ 之间。$\Delta t_{\text{click}} < 20\text{ms}$ 或 $> 3000\text{ms}$ 判定为高风险异常。
   - **二维高斯落点分布与几何中心击中检测**：
     - 复选框大小为 $28\text{px} \times 28\text{px}$，中心为 $(c_x, c_y)$。
     - 点击偏离半径 $r = \sqrt{(x - c_x)^2 + (y - c_y)^2}$。
     - 脚本往往精确计算几何中心（$r < 0.28\text{px}$，如每次命中 $(14.0, 14.0)$），而人类落点在中心周围呈现二维高斯分布（$1.5\text{px} \le r \le 14\text{px}$）。

---

### 3.2 环境与设备指纹核验 (Environment & Device Fingerprint)

1. **自动化框架与 CDP 痕迹探测**：
   - 探测 `navigator.webdriver` 是否显式为 true；
   - 探测自动化驱动特征注入变量：
     - `cdc_adoQpoasnfa76pfcZLmcfl_Array`
     - `cdc_adoQpoasnfa76pfcZLmcfl_Promise`
     - `cdc_adoQpoasnfa76pfcZLmcfl_Symbol`
     - `__webdriver_evaluate`、`__selenium_evaluate`、`__webdriver_script_fn`
     - `__playwright`、`__puppeteer_evaluation_script__`
     - `domAutomation`、`domAutomationController`
   - 探测针对 `navigator.webdriver` 的原型链属性修改（如通过 `Object.defineProperty` 将 getter 篡改为不可读或重写）。
   - 探测无头标志：`window.outerWidth === 0 && window.outerHeight === 0`，以及 User-Agent 中的 Headless 标识。

2. **硬件渲染一致性**：
   - **WebGL 渲染上下文**：通过 `WEBGL_debug_renderer_info` 扩展获取 `UNMASKED_RENDERER_WEBGL`，探测是否为虚拟化无头软光栅引擎（`SwiftShader`、`llvmpipe`、`Mesa Offscreen`、`VirtualBox`、`VMware`、`Software Rasterizer` 等）。
   - **2D Canvas 字体渲染哈希**：利用不同 OS / GPU 驱动的亚像素抗锯齿渲染差异计算 FNV-1a 32位哈希。
   - **Web AudioContext 频域特征**：创建 `OfflineAudioContext`，配置三角波振荡器与动态压缩器（DynamicsCompressor），渲染音频信道数据并计算频响指纹。
   - **屏幕与系统规格一致性**：检查 `screen.availWidth > screen.width` 等不可能出现的逻辑冲突；核验 `hardwareConcurrency` 与 `deviceMemory` 是否异常。

3. **原生 JavaScript 函数 Hook 篡改探测**：
   - 对核心底层 API（`Function.prototype.toString`、`document.createElement`、`window.setTimeout`、`CanvasRenderingContext2D.prototype.getImageData`、`WebGLRenderingContext.prototype.getParameter`）执行原生完整性检验；
   - 严格检验 `Function.prototype.toString.call(fn)` 必须匹配 `/^function\s*[\w$]*\s*\(\)\s*\{\s*\[native code\]\s*\}$/`；
   - 检查目标函数自身是否被挂载了自定义的 `toString` 属性（`hasOwnProperty('toString')`）。

---

### 3.3 浏览器生态历史持久性探测 (Ecosystem)

自动化爬虫与 Headless 实例通常运行在崭新的临时沙盒配置文件（Ephemeral Profile）中，缺乏持续浏览与存储沉淀：
1. **浏览历史栈深度**：通过 `window.history.length` 检测当前会话的历史栈深度，长期使用的常规浏览器深度一般 $\ge 2$，而全新启动的自动化爬虫通常固定为 1。
2. **磁盘配额真实占用**：调用 `navigator.storage.estimate()` 获取真实磁盘使用字节数（`usage`）。长期活跃的浏览器环境沉淀有 Service Worker、HTTP 缓存与本地持久化资产，若 `usage === 0` 且历史栈单一，则叠加生态风险权值。
3. **IndexedDB 活跃库枚举**：通过 `window.indexedDB.databases()` 探测当前配置下存在的持久化数据库数量。
4. **Navigation Timing 真实性**：通过 `performance.getEntriesByType('navigation')` 检测 DNS 查询耗时、TCP 握手耗时与 DOM 渲染耗时，排除零延迟伪造环境。

---

## 4. 实施改动清单

| 文件路径 | 变更类型 | 说明 |
| :--- | :--- | :--- |
| `src/components/security/recaptcha.vue` | 新增 | 全新提取的 reCAPTCHA 安全组件，内嵌独立 `<iframe>` 沙箱、虚拟化遥测分析与内联错误说明行，重构错误清理为仅在点击时触发 |
| `src/components/security/index.ts` | 新增 | 安全组件集中导出入口 |
| `src/utils/request.ts` | 增强 | `AxiosRequestConfig` 支持 `skipToast` 属性并归一化 `Error(errorMsg)`，阻断全局弹窗 |
| `src/api/auth.ts` | 增强 | 认证核心 API 默认配置 `{ skipToast: true }`，实现就地报错下沉 |
| `src/components/layouts/auth/AuthLayout.vue` | 优化 | 彻底剔除全屏白色半透明遮罩层 `.auth-scrim`，仅保留卡片局部进度条与透明防穿透层，清理全部历史注释 |
| `src/components/google/GoogleTextField.vue` | 优化 | 接入 `field-error-fade` 平滑淡入动效，防止单帧硬切；清理全部历史注释 |
| `src/views/auth/signin/IdentifierView.vue` | 重构 | 接入 `GoogleTextField :error-msg` 就地反馈与输入自愈，消除过早清除导致的闪烁，彻底移除行内与样式注释 |
| `src/views/auth/signin/ChallengeView.vue` | 重构 | 移除内联旧 widget，接入 `<Recaptcha>` 组件；各分支全面内联收敛错误并清理注释 |

---

## 5. 验证与审计

1. **类型安全验证**：执行 `npx vue-tsc --noEmit`，全项目无任何 TypeScript 编译错误。
2. **生产构建验证**：执行 `npm run build`，成功完成 2788 个模块打包，产物大小正常。
3. **纯粹代码与零注释审计**：所有新创建与修改的代码文件（`recaptcha.vue`、`index.ts`、`ChallengeView.vue`）均严格遵循零注释铁律，无任何多余注释残留。
4. **Google M3 视觉对齐**：验证卡片在日间模式下维持 `#f9f9f9` 浅灰 Google 风格底色与 `#dadce0` 边框；暗色模式下自适应切换为 `#292a2d` 沉浸暗色与 `#3c4043` 细边框，字体与微动效完美契合 Google reCAPTCHA 交互标准。

---

## 6. 人机风控误杀治理与权重梯度平滑调优 (Anti-False-Positive Tuning)

在初版实现中，针对 Native API 的校验（`vmDetectNativeHooks`）采取了单一布尔判定并赋予了 `+0.95` 的一票否决权，导致安装了浏览器扩展（如广告拦截、密码管理、翻译、油猴插件等无害包装）的正常用户在手动点击时，最终风险分达 `0.87`（`0.95 - 0.08 = 0.87`）而被误杀。为此实施了以下精细化治理：

1. **核心 Hook 与扩展包装（Extension Hooks）分级判定**：
   - 将 `Function.prototype.toString` 自身的隐匿篡改定义为 `coreHook`，风险权重赋为 `+0.35`；
   - 将常见扩展目标函数（`document.createElement`、`window.setTimeout`、`getImageData`、`getParameter`）定义为 `extHookCount`。对扩展包装采取阶梯轻量计数（每项 `+0.05`，上限 `+0.18`），杜绝因单插件重写 DOM 方法而导致直接阻断；
2. **点击落点判定区自适应放宽**：
   - 过去仅以复选框中心几何点为基准计算偏离半径，导致点击文字标签“我不是机器人”的用户无法命中二维高斯奖励；
   - 现引入 `actionTarget` 全域交互包围盒判定，凡在可交互卡片操作区域内点击均视为合法人机落点，并抵扣 `-0.08` 风险分；
3. **真实人类生物轨迹强激励补偿**：
   - 当检测到鼠标轨迹采样点 $\ge 4$、加速度方差 $\sigma_a^2 > 0.000005$ 且未发生机械直线或瞬移时，赋予 `-0.25` 的强烈真实生物特征抵扣；
   - 在此梯度下，带常见插件的真实用户最终评分为 $\max(0, 0.05 - 0.10 - 0.08 - 0.25) = 0.00$，远低于 `0.45` 阻断阈值；而机械脚本与无痕自动化驱动仍会因 `isNoMovement` (+0.65)、`isStraightLine` (+0.75)、`webdriver` (+0.85) 或 `isTrusted=false` (+1.0) 被精准狙击。

---

## 7. reCAPTCHA 交互内联收敛与反馈重构 (reCAPTCHA Inline Error Convergence)

针对此前验证状态依赖页面外部 Toast 浮层弹窗及外部孤立 `<p>` 标签的体验割裂问题，实施内联收敛重构：

1. **废除破坏沉浸感的浮动 Toast**：
   - 彻底移除 `ChallengeView.vue` 中的 `window.$message?.success` 与错误 Toast 调用；
   - 验证通过时，完全依靠沙箱内绿色 checkmark 弹性缩放动画和文案“验证已通过”实现静默内敛自反馈，对齐 Google reCAPTCHA 官方标准。
2. **`recaptcha.vue` 内部深度封装 `error-msg`**：
   - 支持 `error-msg`（双向绑定 `v-model:error-msg`）及内部响应式状态自驱动；
   - 在沙箱卡片正下方内建与 `GoogleTextField.vue` 视觉规格 100% 对齐的 Google Red (`#ea4335` / 暗色 `#f28b82`) 错误说明行（带圆形感叹号 SVG 图标与平滑展开），父组件不再需要维护外部的散落提示标签；
   - 用户重新进入或重置交互时，错误信息自动联动清除，达到高内聚、高复用的平台组件标准。

---

## 8. 全链路认证交互内联收敛与全局 Toast 阻断机制 (Full-stack Inline Error Convergence)

在先前的流程中，当服务端返回业务错误（例如用户名或邮箱不存在）时，前端依然触发全局 Toast 弹窗（`window.$message.error`），且输入框未能就地高亮呈现服务端返回的精确提示信息。为此实施了全链路的交互重构与收敛：

1. **强类型 Axios 请求实例扩展 (`skipToast` 参数)**：
   - 在 [`src/utils/request.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/utils/request.ts) 中对 `AxiosRequestConfig` 进行接口扩展，增加可选的 `skipToast?: boolean` 配置项；
   - 响应拦截器在捕获 HTTP 4xx/5xx 或业务 `code !== 200` 错误时，若当前请求标记了 `skipToast: true`，则静默跳过 `window.$message.error(errorMsg)` 全局弹窗派发；
   - 统一对 `Promise.reject(new Error(errorMsg))` 进行错误对象归一化，确保上层业务 catch 块中 `err.message` 稳定携带服务端返回的真实业务报错文本（如“用户名或邮箱不存在”），而非 Axios 默认的底阶文本 `"Request failed with status code 400"`。
2. **认证 API 路由静默化 (`src/api/auth.ts`)**：
   - 对 `initFlowSessApi`、`submitSignInIdentifierApi` 与 `submitSignInChallengeApi` 显式开启 `{ skipToast: true }`，完全将错误反馈职责下放至各个视图组件。
3. **`IdentifierView.vue` 就地错误联动与自愈机制**：
   - 接入 `GoogleTextField :error-msg="errorMsg"`，在账号不存在或为空时就地触发 Google Red 边框高亮与下挂提示；
   - 挂载 `watch(identifier, () => { errorMsg.value = '' })` 与 `@clear` 事件，用户一旦重新键入或清空内容，错误信息瞬时自愈消除。
4. **`ChallengeView.vue` 全挑战分支就地收敛**：
   - **`pwd` 密码挑战**：密码错误直接注入 `GoogleTextField :error-msg="errorMsg"`，监听 `password` 变动自动清空；
   - **`recaptcha` 人机挑战**：与 `<Recaptcha v-model:error-msg="recaptchaError" />` 双向联动，人机未通过或服务端拒绝时，卡片震颤并在组件内部内联渲染红色错误行；
   - **`device` 设备挑战**：在确认卡片下方就地渲染 `deviceError` 提示行；
   - **`2fa` 双重验证挑战**：验证码校验失败就地渲染于 `GoogleTextField :error-msg="errorMsg"`，监听 `verificationCode` 变动自动消除。
5. **代码极致纯净与零注释铁律审计**：
   - 彻底清理了 `IdentifierView.vue` 与 `ChallengeView.vue` 中的所有模板 HTML 注释与 SCSS 行内注释，全量业务逻辑与架构设计统一且完备地外置于本设计方案中。

---

## 9. 全屏加载遮罩移除、错误闪烁根因治理与 reCAPTCHA 触发优化

针对二次交互反馈中暴露的体验与逻辑问题，实施了专项攻坚与彻底治理：

### 9.1 全屏白色半透明加载遮罩问题解答与彻底移除
- **现状确认**：在原有 [`AuthLayout.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/layouts/auth/AuthLayout.vue) 中，确实挂载了全屏加载遮罩 `<div v-if="isCardLoading" class="auth-scrim" />`。该元素通过 `position: fixed; inset: 0; width: 100vw; height: 100vh; background-color: #ffffff; opacity: 0.5; z-index: 5;` 覆盖于视口顶层，在浅灰底色上呈现出约 80% 的泛白蒙层。
- **冗余性与危害分析**：
  1. 卡片内部已拥有 Google Accounts 原生 Material Linear Progress 顶层线性动画进度条（`.auth-card__progress.is-active`）；
  2. 卡片内部已部署透明防重复点击拦截层（`.auth-card__blocker`：`background: transparent; pointer-events: all; cursor: wait`），足以彻底阻断加载在途的穿透操作；
  3. 全屏泛白遮罩不仅破坏了 Material 3 沉浸感，还是引发下方 `errormsg` 闪烁的核心物理诱因。
- **治理动作**：彻底删除 `.auth-scrim` 模板节点与其伴随的 `auth-scrim-fade` 样式定义，让表单在等待响应期间仅依靠卡片顶部进度条与按钮 loading 进行静默、优雅的局部反馈。

### 9.2 `IdentifierView.vue` 报错提示闪烁 (Flicker) 根因与修复
- **根本原因排查**：
  1. **遮罩层淡出与错误渲染的对比度交叠**：当服务端返回 400 业务报错（如“用户名或邮箱不存在”）时，响应极快（通常 <150ms）。此时全屏白色遮罩正处于 `0.2s` 的淡出离场过渡（`opacity: 0.5 -> 0`），而红色的 `errorMsg` 已同时挂载到 DOM 中。错误文本在泛白蒙层淡出的瞬间发生了从“被蒙白变浅”到“无蒙白骤明”的亮度跳变，视觉上产生剧烈闪烁；
  2. **提交流程中过早的主动清空**：`handleNext` 函数头部原先无条件执行了 `errorMsg.value = ''`。如果用户在报错后再次点击下一步或按下回车，错误行会先瞬间销毁消失，待 100ms 报错返回后又再次挂载，形成“消失 -> 重新出现”的二次硬闪烁。
- **治理动作**：
  1. 移除了全屏遮罩 `.auth-scrim`，彻底消除图层明暗交叠；
  2. 移除 `handleNext` 中同步清空 `errorMsg.value = ''` 的冗余代码，错误重置行为严格收敛至用户实际按键输入（`watch(identifier)`）与清空按钮触发；
  3. 为 [`GoogleTextField.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/google/GoogleTextField.vue) 中的 `error-msg` 加入 `field-error-fade` 平滑过渡，确保错误信息的呈现与消除均呈柔和的微动效。

### 9.3 reCAPTCHA 错误信息清除时机优化
- **原有时序缺陷**：先前沙箱内的 `document.addEventListener('pointerenter')` 在鼠标滑入 iframe 时向父级发送了 `VM_PULL_TRAJECTORY`。宿主组件在响应此消息时错误地执行了 `internalError.value = ''` 与 `emit('update:errorMsg', '')`，导致用户仅将鼠标移过验证码卡片便意外将错误提示清除，体验极不稳定。
- **重构机制**：
  1. `VM_PULL_TRAJECTORY` 仅负责轨迹缓冲区的纯净同传，彻底剥离错误状态变动；
  2. 新增 `VM_CHALLENGE_START` 消息通道。在沙箱内的 `executeVirtualVerificationPipeline()` 执行伊始（即用户点击复选框或按下空格/回车确认触发验证时），立即向宿主发送该指令；
  3. 宿主收到 `VM_CHALLENGE_START` 后方才重置 `internalError` 与派发 `update:errorMsg: ''`，严格实现“鼠标滑过不消除，点击按钮核验时才消除”。

---

## 10. `ChallengeView.vue` 多次点击“下一步”卡片抖动与闪烁根因治理

在连续点击登录/下一步操作时，卡片产生剧烈的上下抖动与水平震颤，经过多维链路排查，定位出三项耦合根因并实施了系统性解耦重构：

### 10.1 核心根因剖析

1. **容器垂直居中与高度瞬变引起的上下弹跳 (Vertical Bouncing)**：
   - [`AuthLayout.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/layouts/auth/AuthLayout.vue) 在桌面端采用 `md:justify-center` 垂直居中弹性布局。卡片顶部的视口坐标公式为 $Y = \frac{H_{\text{viewport}} - H_{\text{card}}}{2}$；
   - 在各挑战分支执行时，原代码在函数最前部同步执行了 `errorMsg.value = ''`、`recaptchaError.value = ''`、`deviceError.value = ''`；
   - 当已有错误存在时，用户再次点击“下一步”，错误行瞬间销毁使卡片高度骤降约 22px，卡片在视口中立即向下瞬移 11px；约 100~200ms 后服务端返回错误，错误行重新挂载使卡片高度增加 22px，卡片又向上瞬移 11px。连续点击直接引发高频的垂直往复颠簸。
2. **底部栏动画滞后造成的复合涟漪效应 (Footer Transition Lag)**：
   - `.auth-footer` 原配置了 `transition: all 0.3s ease;`。当卡片高度变动推动下方 Footer 的文档流位置时，Footer 启动 300ms 的缓动位移过渡，视觉上呈现出卡片与底栏之间拉扯变形的橡皮筋弹性晃动。
3. **reCAPTCHA 沙箱水平震颤被高频重复触发 (reCAPTCHA Shake Re-trigger)**：
   - [`recaptcha.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/security/recaptcha.vue) 监听 `props.errorMsg` 的变化，当该值从空字符串变为非空报错文本时，会向内嵌沙箱发送 `VM_SET_ERROR`，沙箱挂载 `.rc-shake` 动画（500ms 水平剧烈晃动）；
   - 由于前端点击无条件先将 `recaptchaError` 清空为 `''`，随后校验失败又赋回错误文本，导致侦听器被反复激发，即使错误信息完全相同，每点一次按钮验证码框就强行晃动一次；
4. **错误文本缺失渐变过渡 (Snapping without Transition)**：
   - `recaptcha.vue` 内部的 `.recaptcha-error-msg` 原先直接通过 `v-if` 单帧硬切，突变无缓冲。

### 10.2 治理方案与落地重构

1. **解除 Footer 的布局属性过渡**：
   - 彻底移除 `AuthLayout.vue` 中 `.auth-footer` 的 `transition: all 0.3s ease;`，杜绝卡片高度变更引发底栏的 300ms 滞后缓动，使布局位置更新完全与文档流瞬时同步，阻断共振。
2. **生命周期收敛：彻底废除提交流程中的同步提前清空**：
   - 移除了 [`ChallengeView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/ChallengeView.vue) 中 `pwd`、`recaptcha`、`device`、`2fa` 各分支内部无条件执行的 `errorMsg.value = ''`、`recaptchaError.value = ''`、`deviceError.value = ''`；
   - 错误状态转由真实的输入生命周期驱动清理：
     - 密码输入监听：`watch(password, () => { if (errorMsg.value) errorMsg.value = '' })`；
     - 双重验证码监听：`watch(verificationCode, () => { if (errorMsg.value) errorMsg.value = '' })`；
     - 人机挑战通过监听：`watch(isRecaptchaPassed, (val) => { if (val && recaptchaError.value) recaptchaError.value = '' })`；
     - 人机复选框重试：点击沙箱触发 `VM_CHALLENGE_START` 时清理。
3. **并发调用与事件防重锁 (Submission Guard)**：
   - 引入 `const isSubmitting = ref(false)`，并在 `handleVerifyChallenge` 外层使用 `try ... finally` 包裹；
   - 若当前已有在途请求或重复触发，直接短路阻断，从物理层面杜绝 Enter 键与点击事件在同一微任务队列内触发的并发重复执行。
4. **reCAPTCHA 错误动效平滑化与防复触优化**：
   - 为 `recaptcha.vue` 错误提示包裹 `<transition name="field-error-fade">`，配合 `opacity 0.15s cubic-bezier(0.4, 0, 0.2, 1)` 实现柔和淡入淡出；
   - 优化 `watch(() => props.errorMsg, (newVal, oldVal))`，严格比对 `newVal !== oldVal`，当错误信息保持相同时杜绝向沙箱下发 `VM_SET_ERROR`，消除多余的 `.rc-shake` 震颤。

---

## 11. 全屏白色加载遮罩恢复与 1.5s 弹性等待协同机制 (Scrim Restoration & 1.5s Minimum Loading Guard)

针对认证流（`IdentifierView` / `ChallengeView`）向“下一步”过渡时的视觉节奏要求，恢复了全屏白色半透明遮罩并构建了 1.5s 弹性防抖等待体系：

### 11.1 业务诉求与设计目标
1. **全屏白色遮罩（80% 透明度）回归**：在用户确认点击“下一步”或“验证”时，为全局赋予 `rgba(255, 255, 255, 0.8)`（暗色模式 `rgba(32, 33, 36, 0.8)`）带轻微毛玻璃模糊的半透明全屏蒙层，统一全局焦点并彻底阻断重复无效穿透；
2. **进度条层级置顶**：卡片顶部的 Material Linear Progress 线性动画进度条赋予 `z-index: 50`，穿透浮动在蒙层之上（蒙层 `z-index: 40`），形成强烈的层级视觉焦点；
3. **固定 1.5s 前端最小等待驻留 ($\max(1500\text{ms}, t_{\text{backend}})$)**：
   - 当后端验证接口响应极快（通常 <100ms）时，若遮罩与进度条瞬开瞬关，用户眼球会遭受突兀的单帧突闪（Flash of Loading）；
   - 前端强制设定 1.5s 固定展示周期，若 1.5s 后后端尚未返回消息则继续保持遮罩与进度条动画；
   - 待后端响应完成且满足至少 1.5s 后，平滑淡出全屏遮罩并完成向下一视图的平稳过渡（或就地展现校验失败文本）。

### 11.2 架构实现与多层协同

1. **`AuthLayout.vue` 按钮动作监听与遮罩挂载**：
   - 在 `.auth-card` 容器上挂载 `@click="handleCardActionClick"`，通过事件委托侦听 `.btn-next-action, .btn-verify-action, .auth-action-btn` 的点击事件；
   - 探测到用户点击动作且当前未处于加载状态时，瞬时激活 `isActionTriggered = true` 与全屏遮罩 `.auth-scrim`；
   - 配合 `nextTick` 探测子组件的同步表单校验状态：若因必填项为空导致未发起网络请求（`!authFlowStore.loading`），则瞬时自愈关闭，防止空白表单被意外冻结。
2. **`authFlowStore.ts` 异步提交流程的弹性时间下界**：
   - 将 1500ms 魔法值提取为模块级强类型常量：
     ```ts
     export const MIN_LOADING_DURATION_MS = 1500
     ```
   - 在核心包装函数 `withLoading<T>(action)` 中植入 `startTime` 时间戳监测；
   - 无论接口成功（`resolve`）还是抛出业务异常（`reject`），均严格依赖该常量进行弹性下界补偿：
     ```ts
     const elapsed = Date.now() - startTime
     if (elapsed < MIN_LOADING_DURATION_MS) {
       await new Promise(resolve => setTimeout(resolve, MIN_LOADING_DURATION_MS - elapsed))
     }
     ```
   - 确保 `loading.value` 维持至少 `MIN_LOADING_DURATION_MS` 的稳定活跃期，彻底将“等待后端返回响应 -> 取消遮罩 -> 流转到下一步”的原子时序固定下来。




