# AdminTeamDetailModal 身份证防换行排版优化与审核状态文本对齐规范

## 1. 变更背景

在竞赛管理后台查看/编辑参赛队伍详情模态框（[`AdminTeamDetailModal.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/competition/components/admin/AdminTeamDetailModal.vue)）中，存在以下细节排版与状态语义展示问题：
1. **身份证号自动换行缺陷**：18 位身份证号码在表格中由于列宽限制或等宽字体排版时，被浏览器自动切断折行（例如后几位换到下一行），严重影响管理员阅读与核验。需要确保身份证绝不换行，宁可横向延伸宽度并通过外层横向滚动条展示。
2. **审核状态显示文案不精确**：模态框底部状态栏原先显示为“资质已通过”、“已被驳回”、“待审核”。根据业务规范要求，需要统一优化为：“已通过”、“已驳回”、“未审核”。

---

## 2. 核心改动点

### 2.1 身份证防换行与表格宽度延伸优化
1. **表头宽度加宽**：
   - 查看模式（参赛成员表格、指导老师表格）与编辑模式（参赛成员表格、指导老师表格）中的 4 处身份证表头，由原 `width: 175px` 调整为 `width: 200px; min-width: 200px;`。
2. **单元格与徽章强制不换行**：
   - 查看模式：身份证单元格显式设置 `<td style="white-space: nowrap;">`。
   - 身份证徽章样式 `.id-card-badge` 增强：
     ```scss
     .id-card-badge {
       font-family: var(--v-font-mono, monospace);
       font-size: 12px;
       background: rgba(0, 0, 0, 0.04);
       padding: 2px 6px;
       border-radius: 4px;
       white-space: nowrap;
       display: inline-block;
       word-break: keep-all;
       letter-spacing: 0.5px;
     }
     ```
   - 编辑模式：单元格增加 `white-space: nowrap;`，内部输入框设为 `style="min-width: 185px;"`。
   - 学号/工号/手机号 `.code-text` 增加 `white-space: nowrap;`，杜绝一切关键数字或编号折行。
3. **表格横向延伸与滚动支持**：
   - 将 `.roster-table` 的最小宽度由 `1100px` 调整为 `1200px`，编辑态 `.edit-mode-table` 调整为 `1260px`，充分保证各列充足宽度，超出时由外层容器 `.table-scroll-container` 产生平滑水平滚动条。

---

### 2.2 审核状态显示文案对齐
优化 `AdminTeamDetailModal.vue` 中的状态解析方法 `getTeamStatusLabel`：
- `AUDIT_PASSED` -> **`已通过`**（原为“资质已通过”）
- `AUDIT_REJECTED` -> **`已驳回`**（原为“已被驳回”）
- `AUDIT_DRAFT` / `PENDING_AUDIT` / 默认 -> **`未审核`**（原为“待审核”）

同时保持 `getTeamStatusTag` 对应状态标签类型为 `success`、`error`、`warning`。

---

## 3. 验证与构建

- 执行前端构建 `npm run build`（含 TypeScript 严格类型检查与 Vite 生产打包），顺利编译通过无任何异常。
