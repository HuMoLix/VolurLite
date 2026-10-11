# VSideBar 底部坞用户信息弹窗 Card 退出账号按钮样式统一

## 1. 变更背景与需求说明

在 [VSideBar.vue](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/sidebar/VSideBar.vue) 的底部坞（Bottom Dock）用户信息卡片（`.dock-user-popover-card`）中，中层操作按钮组包含三个按钮：
1. **切换账号**（`card-btn card-btn--switch`）
2. **退出账号**（`card-btn card-btn--logout`）
3. **个人信息**（`card-btn card-btn--profile`）

此前设计中，为体现退出账号的警告语义，在日间模式与夜间模式中分别为 `&.card-btn--logout:hover` 单独设置了浅红/警示红 hover 高亮样式（日间模式红色字体 `#d93025` 与浅红背景，暗色模式浅红字体 `#f28b82` 与半透明浅红背景）。

根据最新需求，**“退出账号”按钮需要与其他两个按钮保持相同样式**，不再采用单独的红色高亮差异化表现，统一呈现 Google 规范下的标准中性操作按钮样式与悬停交互反馈。

---

## 2. 核心改动点

1. **移除日间模式单独的悬停警示样式**：
   - 移除了 `.dock-user-popover-card .card-middle-layer .card-action-buttons .card-btn` 内针对 `&.card-btn--logout:hover` 的特定红色高亮规则；
   - “退出账号”按钮在悬停时统一继承 `.card-btn:hover` 的灰色微高亮（`background-color: #f1f3f4; border-color: #dadce0; color: #202124;`），active 态继承 `#e8eaed`。

2. **移除夜间模式单独的悬停警示样式**：
   - 移除了 `html.dark &` 作用域下针对 `&.card-btn--logout:hover` 的特定浅红高亮规则；
   - “退出账号”按钮在悬停时统一继承暗色模式 `.card-btn:hover` 的中性深灰反馈（`background-color: #303134; color: #e8eaed; border-color: #5f6368;`），active 态继承 `#3c4043`。

---

## 3. 影响范围与纯粹代码审计

- **受影响文件**：
  - [src/components/sidebar/VSideBar.vue](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/sidebar/VSideBar.vue)
- **代码零注释规范**：
  - 严格遵守纯粹代码与零注释铁律，未在 [VSideBar.vue](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/sidebar/VSideBar.vue) 中添加任何行内注释或解释性说明，所有设计与实施细节统一记录于本文档中。
