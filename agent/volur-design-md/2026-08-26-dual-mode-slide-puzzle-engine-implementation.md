# Volur Lite 滑块拼图验证码「本地/内置图库优先 + 动态算法兜底」架构改造归档

- **执行日期**：2026-08-26
- **施工模块**：`volur-verification`, `volur-server`
- **文档类型**：人机安全验证引擎重构与高可用机制落地

---

## 1. 变更背景

原 `SLIDE_PUZZLE`（滑块拼图验证码）采用纯内存 Java 2D 渐变几何算法动态绘制背景图。为了：
1. **大幅提升视觉观感与人机交互体验**：支持真实风景、科技风格等背景图；
2. **增强对抗 OCR / 自动化机器人的能力**：真实复杂背景拥有更丰富的信息量与纹理干扰；
3. **保障 100% 极限环境高可用**：设计三级容灾加载链路，即便图片缺失或 IO 损坏也能毫秒级回退至纯算法生成，绝不阻断系统登录/注册等关键链路。

---

## 2. 核心架构与改造点

### 2.1 三级图片供给器 ([`PuzzleImageLoader.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/captcha/engine/PuzzleImageLoader.java))
- **Tier 1 (外部自定义磁盘路径)**：读取配置 `volur.captcha.puzzle.custom-path`，优先扫描外部目录图片，支持运维免重启热更新；
- **Tier 2 (Classpath 内置精选图库)**：内置 5 张高清精美科技与风景素材（`captcha/puzzle/puzzle_bg_*.png`）；
- **Tier 3 (Java 2D 动态算法兜底)**：若未找到可用图片或解码异常，返回 `null` 自动触发极限算法生成。

### 2.2 智能尺寸适配与抠图引擎 ([`SlidePuzzleEngine.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/main/java/com/humolix/volur/verification/captcha/engine/SlidePuzzleEngine.java))
- **等比居中裁切算法 (`adaptBackground`)**：根据目标尺寸（`280 × 150`）长宽比，对任意分辨率的外部原图进行最大化 Center Crop 居中裁切，并使用双线性插值（`VALUE_INTERPOLATION_BILINEAR`）平滑缩放，杜绝拉伸变形；
- **贝塞尔拼图块裁切**：使用 `GeneralPath` 构造齿轮凸凹路径截取滑块，并生成半透明黑色缺口阴影。

### 2.3 配置与测试覆盖
- **配置属性**：在 [`application.yml`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-server/src/main/resources/application.yml) 增加 `volur.captcha.puzzle.custom-path` 属性支持；
- **单元测试**：在 [`SlidePuzzleEngineTest.java`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-platform/volur-verification/src/test/java/com/humolix/volur/verification/SlidePuzzleEngineTest.java) 中编写了内置图库正常加载与图库为空时算法平滑降级的单元测试用例。

---

## 3. 验证结果

1. 执行 `mvn clean test`：单元测试用例全部通过（`Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`）；
2. 执行全平台 Maven 构建：`mvn clean compile -DskipTests` 全部成功（`BUILD SUCCESS`）。
