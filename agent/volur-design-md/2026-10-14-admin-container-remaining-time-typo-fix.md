# 容器监控列表“剩余时长”表头文本错别字修正方案

## 1. 变更背景与问题定位

在容器管理监控页面 ([`AdminContainerView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/admin/container/AdminContainerView.vue)) 的表格表头中，排查发现容器存活倒计时列存在笔误错别字：
- 原文案为：“剩余市场”；
- 目标正确文案：“剩余时长”。

---

## 2. 核心改动点

- **文件**：[`src/volur-lite-view-NEXT/src/views/admin/container/AdminContainerView.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/admin/container/AdminContainerView.vue)
- **改动位置**：`tableColumns` 中 `remainingSeconds` 列的 `title` 配置。
- **改动内容**：
  将 `title: '剩余市场'` 修复为国际化统一绑定的 `title: t('admin.containers.table.remainingTime', '剩余时长')`，使其精准对应多语言字典中的标准表头键值。

---

## 3. 验证情况

- **前端打包**：执行 `npm run build` 打包构建，16.90s 顺利通过，无报错。
