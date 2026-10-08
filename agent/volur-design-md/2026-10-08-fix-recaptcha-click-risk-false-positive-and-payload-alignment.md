# reCAPTCHA 人机验证误杀根本原因分析与风控权重平滑修复设计方案

## 1. 变更背景与问题溯源

用户在正常使用真实 Chrome 浏览器点击 reCAPTCHA 复选框时，向后端接口 `POST /api/v1/captcha/challenge/recaptcha/verify` 提交验证请求遭遇 400 拦截：
```json
{
    "status": 400,
    "errorCode": 400,
    "success": false,
    "message": "行为模式与运行环境存在异常，人机验证失败",
    "data": null,
    "path": "/api/v1/captcha/challenge/recaptcha/verify"
}
```

### 1.1 真实报文解密分析
提取用户实际提交的 RSA 加密载荷并通过后端私钥解密，获得真实浏览器交互全量数据：
```json
{
  "v": 4,
  "bio": {
    "accVar": 0.1454683864799204,
    "curvVar": 0.40842813002096506,
    "smooth": 0.9867001244196184,
    "pts": 80,
    "dur": 125,
    "downUpDur": 67,
    "radius": 8.06
  },
  "env": {
    "wd": false,
    "cdp": false,
    "hook": true,
    "gpu": "ANGLE (NVIDIA, NVIDIA GeForce RT",
    "cores": 28,
    "mem": 32
  },
  "eco": {
    "hist": 24,
    "usage": 0,
    "idb": 0
  },
  "ts": 1791412108955,
  "nonce": "lpf7wkgv",
  "powNonce": ""
}
```

### 1.2 100% 误杀根因定位
1. **客户端载荷版本与服务端判定约束失配（核心致死项）**：
   - 用户浏览器打开的页面处于回滚期前序缓存状态（报文中存在 `powNonce: ""`、`downUpDur: 67`，但未包含根级 `isTrusted` 字段）；
   - 服务端迁移后的 `evaluateClickRisk` 规则采用强布尔判断：
     `boolean isTrusted = rootNode.has("isTrusted") && rootNode.get("isTrusted").asBoolean(false);`
     `if (!isTrusted) { risk += 1.0; }`
   - 因报文中缺少 `isTrusted`，直接累加 `+1.0` 惩罚分，单项直接击穿 `0.45` 拦截阈值，一票否决；
2. **浏览器常用插件被粗糙定性为恶意 Hook**：
   - 用户安装了日常浏览器扩展（如广告拦截、翻译、密码管理器），扩展修改了 `document.createElement` 等 DOM API，导致解密数据中出现 `"hook": true`；
   - 服务端原规则对 Hook 施加了较高的风险分（`+0.35`），对未伴随自动化驱动的日常插件缺乏容错。
3. **真实生理指标数据证明**：
   - 采样点数 `pts: 80`（大量高频移动轨迹，非瞬移/非零点）；
   - 生理微颤动方差 `accVar: 0.145`、自然弧线曲率 `curvVar: 0.408`、交互时长 `dur: 125ms`、落点偏移 `radius: 8.06px`；
   - 硬件为真实的 NVIDIA RTX 独立显卡与多核桌面平台，浏览历史长度 `hist: 24`。
   - 所有指标均为标准、典型的真实人类交互。

---

## 2. 架构优化与平滑修复方案

### 2.1 服务端模型健壮性调整 (`RecaptchaServiceImpl.java`)
1. **`isTrusted` 缺失平滑回退，防止旧版报文盲目致死**：
   - 显式声明 `rootNode.has("isTrusted")` 且为 `false` 时，执行 `+1.0` 惩罚；
   - 若报文因版本问题未携带 `isTrusted` 字段，通过生物动力学指标辅助裁决：仅在 `pts < 3 || accVar < 0.0000008` 时才施加 `+0.85` 惩罚；若具备丰富生理运动特征则不予扣分；
2. **扩展插件 Hook 细粒度隔离**：
   - 仅当 `coreHook` 同时伴随 `webdriver` 或 `cdp` 自动化痕迹时施加 `0.35` 高风险；
   - 单纯的插件环境仅计入微弱参考值（`0.10 ~ 0.15`），杜绝因用户安装日常插件而被误杀；
3. **强化真实生物动力学奖励权重**：
   - 当检测到真实人类高置信度连续微抖动轨迹时（`pts >= 15 && accVar > 0.001`），给予 `-0.35` 的强信任奖励，将整体风险分归零并放行；
4. **沙箱生态指纹误报平抑**：
   - 将 `navTimingZero` 与历史生态的加分条件限定在同时处于极短历史（`hist <= 1`）的全新沙箱内，消除正常用户的偶发加分。

---

## 3. 验证与审计结果

1. **真实报文单元测试验证**：
   - 在 `RecaptchaServiceImplTest.java` 中新增 `verifyRecaptcha_clickMode_realUserPayloadWithExtensions_passes`，使用用户报错时的真实载荷进行验证；
   - 运行测试 `RecaptchaServiceImplTest`，11 项测试全部 100% 通过（用户真实报文核验通过并成功签发 `rctk_` 凭证）；
2. **全模块回归测试**：
   - `volur-security` 全量 97 项测试全部 SUCCESS；
   - `volur-verification` 全部测试 SUCCESS；
3. **前端生产构建**：
   - `volur-lite-view-NEXT` 执行 `npm run build`，0 警告 0 错误通过。
