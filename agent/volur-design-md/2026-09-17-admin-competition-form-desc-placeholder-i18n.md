# 竞赛管理 Markdown 简述占位符国际化词条补齐审计说明

## 1. 变更背景与问题定位

在后台竞赛编辑管理页 [`AdminCompetitionEditView.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/AdminCompetitionEditView.vue) 下属的基础信息选项卡 [`BasicInfoTab.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/views/admin/competition/tabs/BasicInfoTab.vue#L237) 中，Markdown 编辑器引用了国际化占位符：
```vue
<markdown-editor
  v-model:value="formModel.description"
  :placeholder="t('admin.competitions.form_desc_placeholder')"
  :rows="8"
/>
```
经排查，[`zh_CN/admin/competitions.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/zh_CN/admin/competitions.json) 与 [`en_US/admin/competitions.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/en_US/admin/competitions.json) 语言包中原本缺失 `form_desc_placeholder` 键，导致前台在未输入内容时显示为未解析的键名路径。

---

## 2. 施工与改动细节

在两个核心语言包中补全 `admin.competitions.form_desc_placeholder` 键值：

1. **中文语言包 [`zh_CN/admin/competitions.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/zh_CN/admin/competitions.json#L32)**：
   ```json
   "form_desc_placeholder": "请输入竞赛详细介绍、赛制规则与注意事项（支持 Markdown 语法）..."
   ```

2. **英文语言包 [`en_US/admin/competitions.json`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view/src/locales/en_US/admin/competitions.json#L32)**：
   ```json
   "form_desc_placeholder": "Enter detailed competition description, rules, and notes (Markdown supported)..."
   ```

---

## 3. 验证与测试结果

- 执行 `npx vue-tsc --noEmit`：0 错误，国际化解析及静态类型推导全部通过；
- 编辑器空态下能够友好、清晰地提示管理员输入赛制规则与 Markdown 语法指引。
