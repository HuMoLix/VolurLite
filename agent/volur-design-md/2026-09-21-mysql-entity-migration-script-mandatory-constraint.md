# MySQL 实体与数据表增量平滑迁移规范落地审计说明

## 1. 变更背景与治理目标

在 Volur Lite 平台持续迭代过程中，后端业务模块（如 `volur-competition`、`volur-challenge`、`volur-auth` 等）经常涉及领域实体（`Entity`/`PO`）的模型演进与数据库表结构调整（例如新增字段、放宽非空约束、添加防重索引、新增运行日志实体等）。

为确保远程生产环境、预发环境及本地开发环境能够始终无损平滑升级，杜绝因实体代码变更而未同步维护数据库迁移脚本导致生产上线报错（如字段缺失、严格模式报错、历史数据丢失等），特在根规范文件 [`GEMINI.md`](file:///Users/humolix/Documents/Dev/Volur-Lite/GEMINI.md) 与后端开发设计规范 [`agent/volur-design-skills/Rules.md`](file:///Users/humolix/Documents/Dev/Volur-Lite/agent/volur-design-skills/Rules.md) 中增加强制约束规则：**每次对实体表进行结构或字段变更时，必须同步维护增量升级脚本 [`scripts/upgrade/upgrade_to_latest.sql`](file:///Users/humolix/Documents/Dev/Volur-Lite/scripts/upgrade/upgrade_to_latest.sql)**。

---

## 2. 核心改动点清单

### 2.1 根规范指令更新 (`GEMINI.md`)
1. **新增后端架构规范第 9 条**：
   - 明确当每次对领域持久化实体（`Entity` / `PO`）进行调整，或任何涉及 MySQL 数据表结构变更的操作（如新建表、增删改字段、字段类型/非空约束调整、变更索引等）时，必须同步更新 [`scripts/upgrade/upgrade_to_latest.sql`](file:///Users/humolix/Documents/Dev/Volur-Lite/scripts/upgrade/upgrade_to_latest.sql)。
   - 规定迁移脚本必须具备完全**幂等性（Idempotent）**设计，利用存储过程结合 `information_schema` 进行存在性探测（`IF NOT EXISTS` / `IF EXISTS`），支持可重复重入安全执行。
   - 强调涉及字段语义重命名或拆分时，必须在迁移脚本中编写安全的无损数据回填（`UPDATE`）清洗逻辑。
2. **新增 AI 行为指引第 6 条**：
   - 强制约束 AI 在涉及实体类与表结构变更时，必须在同一施工周期内同步修改 [`scripts/upgrade/upgrade_to_latest.sql`](file:///Users/humolix/Documents/Dev/Volur-Lite/scripts/upgrade/upgrade_to_latest.sql)，严禁遗漏迁移脚本而直接提交实体变更。

### 2.2 后端详细编码规范更新 (`agent/volur-design-skills/Rules.md`)
- 在第 2.4 节「持久化实体对象 (Entity / PO / DO)」中补充约束条目：强制要求每次对持久化实体新增或字段变更时，必须同步更新增量平滑升级脚本并遵循存储过程探测与幂等原则。

---

## 3. 安全隐患防范说明

1. **避免生产环境中断与数据丢失**：
   - 裸 `ALTER TABLE` 在重复执行时会因 Duplicate Column 报错；未提供数据回填的字段替换会导致历史数据丢失。通过规范化存储过程与数据回填，保障迁移幂等与数据完整性。
2. **严格模式兼容**：
   - MySQL 8.x 严格模式下，新字段若定义为 `NOT NULL` 且未提供默认值，会在写入历史代码时发生中断。迁移脚本必须显式放宽或提供合规默认值。

---

## 4. 影响范围

- **受影响规范文件**：
  - `GEMINI.md`
  - `agent/volur-design-skills/Rules.md`
- **受影响升级维护脚本**：
  - `scripts/upgrade/upgrade_to_latest.sql`
- **开发与协作流程**：
  - 开发者与 AI 助手在后续涉及持久化实体或表结构变更时，均需依据本规则在 `scripts/upgrade/upgrade_to_latest.sql` 中补充幂等迁移逻辑。
