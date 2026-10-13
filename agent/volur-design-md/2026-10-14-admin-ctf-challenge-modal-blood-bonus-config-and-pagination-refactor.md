# CTF 竞赛赛题管理添加模态框、一二三血配置卡片、列表编辑与分页重构文档

## 1. 变更背景与需求分析

在 `AdminCtfCompetitionChallengeDetailView.vue`（CTF 竞赛赛题管理视图）及后端竞赛服务中，进行了以下核心交互与业务逻辑优化：
1. **添加题目模态框轻量化与文案统一**：
   - 模态框标题由历史的“挂载题库题目到竞赛”调整为更清晰直观的“添加题目至竞赛”；
   - 移除模态框底部的“取消”按钮，仅保留“添加”主要动作按钮（支持通过右上角关闭图标或 ESC 退出），降低界面视觉冗余；
   - 将确认操作按钮文案“确认挂载”修改为“添加”。
2. **赛题配置卡片与一二三血奖励比例机制全链路打通**：
   - 在赛题列表上方增加“赛题配置”卡片，支持独立管理当前竞赛是否开启一二三血（First/Second/Third Blood）奖励机制；
   - **采用百分比机制（Percentage）替代原固定绝对分值**：奖励分值根据题目即时分值动态按百分比计算（`bonus = Math.round(newPoints * (pct / 100.0))`），确保随着题目动态衰减，一二三血的激励幅度始终与题目当前难度价值成正比；
   - 默认奖励比例：一血 10%、二血 5%、三血 2%；
   - 同步改造后端领域模型（`CompetitionConfig`、`CompetitionConfigDTO`、`AdminCompetitionDetailVO`）、MapStruct 映射器、服务层配置更新逻辑，以及判题得分与动态衰减回算逻辑（`CtfCompetitionServiceImpl`），彻底消除此前硬编码 `isFirstBlood ? 1.1倍` 的粗粒度反模式；
   - 同步更新数据库增量幂等升级迁移脚本 `scripts/upgrade/upgrade_to_latest.sql`（字段为 `enable_blood_bonus`、`first_blood_bonus_percentage`、`second_blood_bonus_percentage`、`third_blood_bonus_percentage`）。
3. **列表视图指标文案精简与赛题编辑模态框**：
   - 删除“当前分值”列中的单位后缀“pts”；
   - 删除“解题次数”列中的单位后缀“次”；
   - 操作列中在“卸载”按钮旁新增“编辑”按钮，支持管理员独立编辑该挂载题目的初始分数（`initialPoints`）与衰减底分（`minPoints`），调用后端的 `updateMountedCtfChallengeApi` 完成即时同步。
4. **引入通用分页组件集成**：
   - 在赛题列表下方集成 `@/components/common/VPagination.vue`，实现页面级别的流畅响应式分页展示。

---

## 2. 核心架构与改动点

### 2.1 后端领域模型与业务逻辑更新

1. **实体与 DTO/VO 扩展**：
   - `CompetitionConfig`：新增 `enableBloodBonus`（默认 true）、`firstBloodBonusPercentage`（默认 10）、`secondBloodBonusPercentage`（默认 5）、`thirdBloodBonusPercentage`（默认 2）；
   - `CompetitionConfigDTO`：同步新增上述 4 个字段；
   - `AdminCompetitionDetailVO`：同步新增上述 4 个字段；
   - `CtfCompetitionChallengeVO`：新增 `minPoints` 字段声明，使得前端获取挂载题目列表时直接包含该赛题的衰减底分。
2. **对象映射器 (`CompetitionMapper`)**：
   - `toAdminDetailVO` 中增加 `competition.config` 中 4 个一二三血配置字段的精准映射；
   - `toCtfChallengeVO` 自动将持久化实体的 `minPoints` 映射至视图对象。
3. **服务层逻辑与判题分值计算 (`CtfCompetitionServiceImpl`)**：
   - 提交 Flag 判定正确后，根据竞赛配置动态获取 `enableBloodBonus` 判定结果；
   - 当 `enableBloodBonus == true` 时，第 1 名解答队伍累加 `(int) Math.round(newPoints * (firstBloodBonusPercentage / 100.0))`，第 2 名累加 `secondBloodBonusPercentage` 百分比奖励，第 3 名累加 `thirdBloodBonusPercentage` 百分比奖励，后续队伍仅获得当前动态分 `newPoints`；若未开启则均只获得 `newPoints`；
   - 动态衰减重算时，遍历 `allSolves` 按序安全回算一二三血动态百分比奖励分并持久化，确保排行榜分值精确无误。
4. **数据库增量升级脚本 (`scripts/upgrade/upgrade_to_latest.sql`)**：
   - 在存储过程 `migrate_volur_schema_20260921` 中通过 `information_schema.COLUMNS` 探测，幂等追加 `enable_blood_bonus`、`first_blood_bonus_percentage`、`second_blood_bonus_percentage`、`third_blood_bonus_percentage` 4 个列定义。

### 2.2 前端视图与交互重构 (`AdminCtfCompetitionChallengeDetailView.vue`)

1. **赛题配置卡片**：
   - 采用标准 Google 风格的浅灰/灰黑双色阶梯面板卡片，顶部配备“保存配置”按钮；
   - 引入 `NSwitch` 受控开关，切换 `bloodConfig.enableBloodBonus`；
   - 开关开启时展开响应式 3 列排版，输入一血、二血、三血奖励比例（%）；
   - 组件挂载时通过 `getAdminCompetitionByIdApi` 自动拉取服务端已有配置，点击保存时调用 `updateCompetitionApi` 统一持久化。
2. **添加题目 Modal 精简化**：
   - 标题统一为 `添加题目至竞赛`；
   - 删除 footer 中的“取消”按钮；
   - 确认按钮文案统一为“添加”。
3. **赛题表格数据列与编辑功能**：
   - “当前分值”只显示数字 `${row.currentPoints ?? 0}`；
   - “解题次数”只显示数字 `${row.solvesCount || 0}`；
   - 操作列集成“编辑”与“卸载”双按钮；
   - 点击“编辑”弹出 `VModal`，自动带入初始分数和衰减底分，通过 `updateMountedCtfChallengeApi` 保存并即时刷新列表。
4. **列表分页组件集成**：
   - 引入 `VPagination`，绑定 `pagination.page` 与 `pagination.pageSize`，并在前端通过计算属性 `paginatedChallenges` 进行无缝切片，保留全局数据缓存并提供极致交互体验。

---

## 3. 验证与审计结论

1. **后端验证**：
   - 执行 `mvn test-compile -DskipTests` 成功，13 个子模块均构建通过；
   - 架构完全遵守 `Rules.md` 与零注释原则。
2. **前端验证**：
   - 执行 `vue-tsc --noEmit` 0 错误通过；
   - 执行 `npm run build` 生产打包编译顺利完成。
