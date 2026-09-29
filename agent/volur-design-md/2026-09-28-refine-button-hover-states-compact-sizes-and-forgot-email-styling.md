# 审计与施工记录：优化按钮 Hover 交互色彩、微调紧凑尺寸及重构忘记邮箱悬浮背景与边距

## 1. 变更背景与需求目标

根据用户的进一步精细化 UI/UX 优化需求，对登录标识视图（[`IdentifierView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/IdentifierView.vue)）进行以下微调：
1. **“下一步”与“创建账号”按钮 Hover 色彩升级**：悬浮（hover）时的交互背景颜色统一设置为 Google M3 规范的软质交互层 `rgba(11, 87, 208, 0.08)`；
2. **“忘记了邮箱？”链接交互重构**：
   - 默认正常状态下移除常驻背景色（呈现为干净纯净的文本链接）；
   - 仅在用户悬浮（hover）时呈现 `rgba(11, 87, 208, 0.08)` 的微透背景，且保持无下划线；
   - 内边距（padding）紧凑收敛为 `1px 2px`；
3. **按钮尺寸紧凑轻量化**：
   - 将“创建账号”与“下一步”两操作按钮的尺寸进一步微调收敛（由原先的 40px 高度、104px 最小宽度，进一步优化为更紧凑的 36px 高度、88px 最小宽度与 16px 水平内边距），使卡片右下角整体视觉比例更加轻巧现代。

---

## 2. 核心代码变更清单

### 2.1 样式重构与规则调整 (`src/volur-lite-view-NEXT/src/views/auth/signin/IdentifierView.vue`)

```scss
// 忘记邮箱链接：默认透明，仅在 hover 时展示背景颜色 rgba(11, 87, 208, 0.08)，无下划线，padding 为 1px 2px
.auth-forgot-link {
  display: inline-flex;
  align-items: center;
  padding: 1px 2px !important;
  border-radius: 4px;
  background-color: transparent !important;
  color: #0b57d0 !important;
  font-size: 14px;
  font-weight: 500;
  text-decoration: none !important;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;

  &:hover {
    background-color: rgba(11, 87, 208, 0.08) !important;
    text-decoration: none !important;
    color: #0b57d0 !important;
  }

  &:active {
    background-color: rgba(11, 87, 208, 0.16) !important;
  }
}

// 统一紧凑动作按钮规格 (创建账号与下一步尺寸严格相同：高 36px、宽 88px)
.auth-action-btn {
  height: 36px !important;
  min-width: 88px !important;
  padding: 0 16px !important;
  border-radius: 9999px !important;
  font-size: 14px !important;
  font-weight: 500 !important;
  box-sizing: border-box;
}

// 创建账号按钮：正常状态为透明底/蓝色文字，hover 时背景颜色为 rgba(11, 87, 208, 0.08)
.btn-create-account {
  color: #0b57d0;
  background-color: transparent;
  border: none;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  outline: none;
  user-select: none;

  &:hover {
    background-color: rgba(11, 87, 208, 0.08) !important;
    color: #0b57d0 !important;
  }
}

// 下一步按钮：正常实心蓝底白字，hover 时背景颜色为 rgba(11, 87, 208, 0.08)
.btn-next-step {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background-color: #0b57d0 !important;
  color: #ffffff !important;
  border: none !important;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    background-color: rgba(11, 87, 208, 0.08) !important;
    color: #0b57d0 !important;
    border: none !important;

    :deep(.n-button__content),
    .n-button__content {
      color: #0b57d0 !important;
    }

    :deep(.n-button__state-border) {
      border: none !important;
    }
  }
}
```

---

## 3. 测量数据与验证测试

在 Chrome DevTools 环境中实测验证结果：

1. **按钮几何尺寸实测**：
   ```json
   {
     "rectCreate": { "width": 88, "height": 36, "top": 395.0625, "left": 290 },
     "rectNext": { "width": 88, "height": 36, "top": 395.0625, "left": 388 }
   }
   ```
   - 两按钮宽高均精准对齐为 `88px × 36px`（高度减少 4px，宽度减少 16px，实现紧凑微缩）；
   - 两按钮垂直基线同高（`top: 395.0625px`）；
   - “创建账号”（`left: 290`）与“下一步”（`left: 388`）间距为 `10px`。
2. **“忘记了邮箱？”链接属性实测**：
   - 默认背景色：`rgba(0, 0, 0, 0)`（透明）；
   - Padding：`1px 2px`；
   - Hover 悬浮 CSS 规则：`background-color: rgba(11, 87, 208, 0.08) !important; text-decoration: none !important;`。
3. **“下一步”与“创建账号” Hover 样式规则验证**：
   - 两者在 `:hover` 下均被注入 `background-color: rgba(11, 87, 208, 0.08) !important; color: #0b57d0 !important;`。
4. **项目打包验证**：
   - 执行 `npm run build`，耗时 3.01s 构建完成，全量 TypeScript 静态类型检查零错误。
