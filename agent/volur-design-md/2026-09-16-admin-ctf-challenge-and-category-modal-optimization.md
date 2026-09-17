# 施工与修改文档：CTF 赛题列表视图与赛题分类管理重构优化

## 1. 变更背景与需求概述
为了提升管理端 CTF 赛题（Admin CTF Challenges）的交互体验与信息层级呈现效率，本次迭代对赛题管理视图及分类管理模态框进行了以下针对性优化：
1. **CTF 分类管理弹窗 (`CtfCategoryModal.vue`)**：
   - 增加对已有分类的在线编辑功能（支持修改分类名称、分类图标、描述等信息）；
   - 移除列表中分类 ID 展示的前缀 `#` 符号，保持等宽长数字 ID 的规范直观呈现。
2. **CTF 赛题列表视图 (`AdminCtfChallengeView.vue`)**：
   - 移除赛题列表中 ID 的前缀 `#` 符号，并扩展列宽适配雪花算法长整型展示；
   - 将赛题的“分类”从题目名称单元格中抽离，并在题目名称后单独列为一个独立单元格；
   - **难度与后端及 `challenge.json` 严格对齐**：
     - `1`：入门 (`easy`)
     - `2`：简单 (`normal`)
     - `3`：中等 (`mid`)
     - `4`：进阶 (`hard`)
     - `5`：困难 (`difficult`)
     - 表格使用语义色彩文字徽标（`NTag`）替代旧版的星级（`NRate`）评分；
   - **信息列精简**：
     - 移除题目列表中的 **Flag** 以及 **靶机镜像 / 端口** 列，收敛至赛题详情/编辑页；
     - 移除题目列表中的 **附件** 列，统一在编辑详情页进行管理；
     - 表格横向滚动自适应宽度优化为 `:scroll-x="800"`。
3. **后端枚举设计与架构对齐 (`ChallengeDifficulty` 参考 `ChallengeType`)**：
   - 新增 `ChallengeDifficulty` 枚举（实现 `EnumEntity`，包含 `code`, `description`, `label`，提供 `@JsonValue` 与 `@JsonCreator`）；
   - 在 `CtfChallenge.java` 中采用 `@Enumerated(EnumType.STRING)` 强类型持久化 `difficulty`；
   - 在 `CtfChallengeCreateRequestDTO`、`CtfChallengeUpdateRequestDTO`、`CtfChallengeListVO`、`CtfChallengeDetailVO` 中全面收敛为 `ChallengeDifficulty`；
   - 在 `AdminCtfChallengeServiceImpl.java` 中统一使用 `ChallengeDifficulty.fromValue(...)` 与 `getDescription()` 进行导入导出序列化。
4. **编辑与创建页对齐 (`AdminCtfChallengeEditView.vue`)**：
   - 难度等级输入项联动展示对应的中文与英文标识徽章，并兼容字符串与数字解析。

---

## 2. 改动点清单与详细说明

### 2.1 后端工程 (`volur-challenge`)
- **新增枚举 `ChallengeDifficulty.java`**：
  - 路径：`com.humolix.volur.challenge.domain.enums.ChallengeDifficulty`
  - 继承规范：`implements EnumEntity`
  - 常量定义：
    - `EASY(1, "easy", "入门")`
    - `NORMAL(2, "normal", "简单")`
    - `MID(3, "mid", "中等")`
    - `HARD(4, "hard", "进阶")`
    - `DIFFICULT(5, "difficult", "困难")`
  - 序列化与反序列化：`@JsonValue` 导出小写 `description`，`@JsonCreator` 兼容数字 `1~5`、英文（如 `easy`, `EASY`）及中文别名；
- **基础接口调整 `Challenge.java`**：
  - `default Object getDifficulty()`，兼顾不同实体对难度的类型拓展；
- **实体改造 `CtfChallenge.java`**：
  - `private Integer difficulty;` -> `private ChallengeDifficulty difficulty = ChallengeDifficulty.EASY;`；
  - 注解标注 `@Enumerated(EnumType.STRING)` 与 `@Column(name = "difficulty", nullable = false, columnDefinition = "VARCHAR(32) DEFAULT 'EASY'")`；
- **DTO / VO 强类型统一**：
  - `CtfChallengeCreateRequestDTO.java`: `difficulty` 升级为 `ChallengeDifficulty`；
  - `CtfChallengeUpdateRequestDTO.java`: `difficulty` 升级为 `ChallengeDifficulty`；
  - `CtfChallengeListVO.java`: `difficulty` 升级为 `ChallengeDifficulty`；
  - `CtfChallengeDetailVO.java`: `difficulty` 升级为 `ChallengeDifficulty`；
- **业务实现 `AdminCtfChallengeServiceImpl.java`**：
  - 创建题目默认难度升级为 `ChallengeDifficulty.EASY`；
  - 导入 ZIP 包解析简化为 `ChallengeDifficulty.fromValue(diffObj)`；
  - 导出 ZIP 包使用 `challenge.getDifficulty().getDescription()`。

### 2.2 前端工程 (`volur-lite-view`)
- **类型定义 `src/types/challenge/index.ts`**：
  - 新增 `export type ChallengeDifficulty = 'easy' | 'normal' | 'mid' | 'hard' | 'difficult'`；
  - 更新 `CtfChallengeListVO`, `CtfChallengeDetailVO`, `CtfChallengeCreateRequestDTO`, `CtfChallengeUpdateRequestDTO` 的 `difficulty` 类型；
- **`CtfCategoryModal.vue`**：
  - 引入 `updateAdminCtfCategoryApi`；
  - 新增编辑分类二级模态框 (`n-modal`)，支持分类名称、图标选择器（`VolurSvgPicker`）与说明信息编辑；
  - 表格操作列中新增“编辑”按钮（`handleOpenEditModal`）；
  - 去除 ID 前缀 `#`。
- **`AdminCtfChallengeView.vue`**：
  - 去除 ID 列 `#` 前缀；
  - 题目名称列仅保留题目名；
  - 题目名称列后增加独立“分类”列；
  - 难度列升级 `getDifficultyBadge`，支持 `ChallengeDifficulty`、字符串与数字；
  - 彻底删除“Flag”、“靶机镜像 / 端口”、“附件”列及对应的 CSS 样式类；
  - 表格水平滚动宽度设为 `:scroll-x="800"`。
- **`AdminCtfChallengeEditView.vue`**：
  - 支持 `parseDifficultyToNumber`，在难度评分控件旁动态展示对应难度的文字徽标。

---

## 3. 安全隐患说明与健壮性保障
1. **输入校验与类型系统保障**：前后端通过枚举类型实现单一真实数据源，消除了字符串魔法值或越界数字的安全隐患；
2. **向后兼容与容错性**：`ChallengeDifficulty.@JsonCreator` 兼容数字与多种历史别名，保证了导入老旧 `challenge.json` 赛题包时的无缝兼容；
3. **编译与打包测试**：
   - 前端 `npm run build`（`vue-tsc --noEmit && vite build`）通过，0 错误；
   - 后端 `mvn -pl volur-challenge test-compile` 通过，BUILD SUCCESS。

---

## 4. 影响范围 (Impact Scope)
- `src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/domain/enums/ChallengeDifficulty.java`
- `src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/domain/entity/Challenge.java`
- `src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/domain/entity/ctf/CtfChallenge.java`
- `src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/domain/dto/ctf/CtfChallengeCreateRequestDTO.java`
- `src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/domain/dto/ctf/CtfChallengeUpdateRequestDTO.java`
- `src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/domain/vo/ctf/CtfChallengeListVO.java`
- `src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/domain/vo/ctf/CtfChallengeDetailVO.java`
- `src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/service/admin/ctf/impl/AdminCtfChallengeServiceImpl.java`
- `src/volur-lite-view/src/types/challenge/index.ts`
- `src/volur-lite-view/src/views/admin/challenge/components/CtfCategoryModal.vue`
- `src/volur-lite-view/src/views/admin/challenge/AdminCtfChallengeView.vue`
- `src/volur-lite-view/src/views/admin/challenge/AdminCtfChallengeEditView.vue`
