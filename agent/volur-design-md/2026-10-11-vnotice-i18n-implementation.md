# VNotice 站内通知气泡组件国际化 (i18n) 施工记录

## 1. 变更背景与动因
在 `volur-lite-view-NEXT` 架构中，侧边栏底部停靠气泡组件 [`VNotice.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/sidebar/VNotice.vue) 承担了站内消息提醒、未读红点、分类筛选与通知详情查看的核心交互。此前该组件存在多处未完备的硬编码中文字符串与缺失的国际化字典项：
1. 底部总数提示使用硬编码模板字符串 `` `共 ${totalCount} 条通知` ``，在切换至英文或繁体中文时无法完成语言转换；
2. 相对时间格式化函数 `formatTime` 中的 `'刚刚'`、`'${diffMins} 分钟前'`、`'${diffHours} 小时前'`、`'昨天'`、`'${diffDays} 天前'` 均为硬编码中文，未能接入 i18n 动态参数插值体系；
3. 通用字典文件 `common.json` 缺少 `mark_all_read`、`no_unread_notice`、`no_notifications`、`signin_to_view_notice`、`notification_detail`、`go_to_competition` 等通知业务字段；
4. 通知类型映射文案（`competition_notice`、`team_notice`、`award_notice`、`system_notice`）缺少完整的跨语言词条。

为了确保 `VNotice` 在全语言环境（`zh-CN`、`en-US`、`zh-TW`）下的完整无缝本地化，本次对其进行了全面规范的 i18n 改造。

---

## 2. 核心改动点

### 2.1 语言包补充与规范对齐
在各语言环境的通用字典文件 [`common.json`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/locales/zh-CN/common.json) 及竞赛字典 [`competition.json`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/locales/zh-CN/competition.json) 中补充以下词条：
- **基础操作与标签**：
  - `all`: 全部 / All / 全部
  - `unread`: 未读 / Unread / 未讀
  - `refresh`: 刷新 / Refresh / 重新整理
  - `delete`: 删除 / Delete / 刪除
  - `mark_all_read`: 全部已读 / Mark all as read / 全部已讀
- **通知状态与提示**：
  - `signin_to_view_notice`: 登录后查看站内通知 / Sign in to view notifications / 登入後查看站內通知
  - `no_unread_notice`: 暂无未读通知 / No unread notifications / 暫無未讀通知
  - `no_notifications`: 暂无通知 / No notifications / 暫無通知
  - `total_notifications`: 共 {total} 条通知 / {total} notifications in total / 共 {total} 則通知
  - `notification_detail`: 通知详情 / Notification Details / 通知詳情
  - `go_to_competition`: 前往竞赛详情 / View Competition Details / 前往競賽詳情
- **通知分类类型**：
  - `competition_notice`: 竞赛消息 / Competition / 競賽消息
  - `team_notice`: 组队消息 / Team / 組隊消息
  - `award_notice`: 荣誉奖励 / Award / 榮譽獎勵
  - `system_notice`: 系统通知 / System / 系統通知
- **相对时间参数化模板**：
  - `time_just_now`: 刚刚 / Just now / 剛剛
  - `time_minutes_ago`: {n} 分钟前 / {n}m ago / {n} 分鐘前
  - `time_hours_ago`: {n} 小时前 / {n}h ago / {n} 小時前
  - `time_yesterday`: 昨天 / Yesterday / 昨天
  - `time_days_ago`: {n} 天前 / {n}d ago / {n} 天前

### 2.2 `VNotice.vue` 模板与脚本国际化重构
1. **通知总数国际化**：
   - 将原硬编码字符串改为 `t('common.total_notifications', { total: totalCount })`，支持参数插值。
2. **跨路由跳转文案归一**：
   - 将原依赖非全局加载的 `competition.go_to_competition` 统一重构为 `common.go_to_competition`，确保在任何页面下均能 100% 稳定读取译文。
3. **相对时间响应式参数化**：
   - `formatTime` 全面改用 `t('common.time_just_now')`、`t('common.time_minutes_ago', { n: diffMins })`、`t('common.time_hours_ago', { n: diffHours })`、`t('common.time_yesterday')`、`t('common.time_days_ago', { n: diffDays })`。

---

## 3. 安全隐患修复与防御性说明
- **零注释原则（Zero-Comment Rule）**：代码文件保持极致纯净，无任何行内注释，设计意图由本审计文档承载。
- **快速失败一致性（Fast-Fail）**：未定义参数由 i18n 规范抛出或依约定兜底，绝无假数据逻辑。
- **全链路构建通过**：通过 `vue-tsc --noEmit && vite build` 严格验证，零类型与构建错误。

---

## 4. 影响范围
- [`src/locales/zh-CN/common.json`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/locales/zh-CN/common.json)
- [`src/locales/en-US/common.json`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/locales/en-US/common.json)
- [`src/locales/zh-TW/common.json`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/locales/zh-TW/common.json)
- [`src/locales/zh-CN/competition.json`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/locales/zh-CN/competition.json)
- [`src/locales/en-US/competition.json`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/locales/en-US/competition.json)
- [`src/locales/zh-TW/competition.json`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/locales/zh-TW/competition.json)
- [`src/components/sidebar/VNotice.vue`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/sidebar/VNotice.vue)
