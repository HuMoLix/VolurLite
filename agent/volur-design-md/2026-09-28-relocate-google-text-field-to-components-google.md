# 施工设计与审计记录：迁移 GoogleTextField 组件至独立 components/google 目录

- **文件编号**：`2026-09-28-relocate-google-text-field-to-components-google`
- **日期**：2026-09-28
- **影响范围**：
  - `src/volur-lite-view-NEXT/src/components/google/GoogleTextField.vue` (迁移目标路径)
  - `src/volur-lite-view-NEXT/src/views/auth/signup/SignUpView.vue` (更新导入路径)
  - `src/volur-lite-view-NEXT/src/views/auth/signin/IdentifierView.vue` (更新导入路径)
  - `src/volur-lite-view-NEXT/src/views/auth/signin/ChallengeView.vue` (更新导入路径)

---

## 1. 变更背景与架构收敛

为保持 `volur-lite-view-NEXT` 组件库结构职责清晰、领域收敛，针对 Google 风格定制组件设立专属目录 `src/components/google/`，将此前存放在 `components/common/` 下的 `GoogleTextField.vue` 迁移至 `components/google/`，以便后续集中收敛 Google Material 3 体系下的定制原子组件与表单组件。

---

## 2. 变更内容

1. **文件物理迁移**：
   - 将 `src/volur-lite-view-NEXT/src/components/common/GoogleTextField.vue` 迁移至 `src/volur-lite-view-NEXT/src/components/google/GoogleTextField.vue`；
2. **全局引用路径同步更新**：
   - 在 [`SignUpView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signup/SignUpView.vue) 中更新：
     ```ts
     import GoogleTextField from '@/components/google/GoogleTextField.vue'
     ```
   - 在 [`IdentifierView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/IdentifierView.vue) 中更新：
     ```ts
     import GoogleTextField from '@/components/google/GoogleTextField.vue'
     ```
   - 在 [`ChallengeView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/auth/signin/ChallengeView.vue) 中更新：
     ```ts
     import GoogleTextField from '@/components/google/GoogleTextField.vue'
     ```

---

## 3. 构建与验证结果

- 执行 `npm run build` (`vue-tsc --noEmit && vite build`)；
- TypeScript 类型推导与打包验证 100% 通过（耗时 2.46s），旧路径无任何残留引用。
