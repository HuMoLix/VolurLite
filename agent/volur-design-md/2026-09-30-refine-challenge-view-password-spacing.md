# 密码挑战视图“显示密码”间距调优记录

## 1. 变更背景与诉求
在 Google Accounts 设计风格体系下，密码输入表单需具备极高的亲密性（Proximity Principle）。用户反馈在密码挑战阶段（`ChallengeView.vue` 中的 `challengeType === 'pwd'`），密码输入框与其紧随其后的“显示密码”与“忘记密码？”操作栏之间的垂直边距过大。此前移除了父容器的全局 24px `space-y-6` 之后设置的 `mt-2.5`（10px）依然略显松散，需要进一步将其紧缩调小，使复选框更贴合密码框下沿，对齐 Google M3 Text Field 标准。

## 2. 变更详情
- **目标文件**: `src/volur-lite-view-NEXT/src/views/auth/signin/ChallengeView.vue`
- **调整点**:
  - 将包含“显示密码”复选框与“忘记密码？”跳转链接的弹性布局行容器的 `margin-top` 类名从 `mt-2.5`（10px）调整为 `mt-1`（4px）。
  - 该 4px 紧凑间距与 `GoogleTextField.vue` 内部错误信息和辅助文本的 `margin-top: 4px` 保持 1:1 的视觉网格韵律，使得复选框紧邻输入框底部外边框。

## 3. 验证与审计
- **编译检查**: 运行 `npm run build`（`vue-tsc --noEmit && vite build`），构建耗时 5.77s，产物生成 100% 成功，0 错误，0 告警。
- **纯粹代码规范**: 严格保持代码零注释铁律，文档统一外置于本审计文档中。
