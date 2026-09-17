# 用户管理表格“账号状态”开关组件无文字药丸风格对齐

## 1. 变更背景

在用户管理模块（`AdminUserView.vue`）中，虽然新增/编辑用户的弹窗表单已重构为简洁无内嵌文字的药丸开关，但在列表表格（通过 `AppTableCard.vue` 渲染的 `n-data-table`）的“账号状态”列（`status`）的列定义（`render` 函数）中，先前仍保留了 `{ checked: () => '正常', unchecked: () => '封禁' }` 插槽。
这导致表格单元格内部呈现的开关依然带有旧式的滑轨内嵌汉字，与平台统一的简洁开关风格不一致。

---

## 2. 改动详情

### `AdminUserView.vue` 列渲染函数升级

- **定位**：`status` 列渲染函数（`columns` 数组）。
- **修改前**：
  ```typescript
  render(row) {
    return h(
      NSwitch,
      {
        value: row.status,
        size: 'small',
        disabled: row.username === 'VolurSuperAdmin',
        'onUpdate:value': (val: boolean) => handleToggleStatus(row, val)
      },
      {
        checked: () => '正常',
        unchecked: () => '封禁'
      }
    )
  }
  ```
- **修改后**：
  ```typescript
  render(row) {
    return h(
      'div',
      {
        class: 'switch-row',
        style: 'display: flex; align-items: center; gap: 8px;'
      },
      [
        h(NSwitch, {
          value: row.status,
          size: 'small',
          disabled: row.username === 'VolurSuperAdmin',
          'onUpdate:value': (val: boolean) => handleToggleStatus(row, val)
        }),
        h(
          'span',
          {
            class: 'field-hint',
            style: 'font-size: 12px; color: #444444;'
          },
          row.status ? '正常' : '封禁'
        )
      ]
    )
  }
  ```

---

## 3. 规范对齐与效果

1. **滑块极简纯净**：开关本体彻底移除内嵌文字插槽，变回 Naive UI 紧凑平滑的原生小药丸滑块（`size="small"`）。
2. **状态指示直观清晰**：将“正常”与“封禁”文字移动到开关右侧，采用 12px、`#444444` 颜色并随状态响应式切换，垂直居中对齐，与全站风格完全统一。
3. **构建验证**：前端执行 `npm run build` 耗时 5.20s，编译成功且无报错。
