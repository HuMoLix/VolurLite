# CTF 赛题衰减梯度可视化配置与 200 分降至 50 分模型分析方案

## 1. 变更背景与需求

1. **赛题衰减梯度可视化配置缺失**：
   在 `volur-lite-view-NEXT` 的竞赛运维管理后台中，“添加赛题”与“编辑赛题分值配置”弹窗此前仅支持配置“初始分数 (`initialPoints`)”与“衰减底分 (`minPoints`)”，`decayCount`（衰减梯度常数）被隐藏且固定默认传入 30。运维管理员无法直接在前端界面针对特定难题或水题定制差异化的衰减速度。
2. **算力与分值衰减分析**：
   需明确回答在初始分 200 分、最低保底分 50 分的情况下，如何通过调整 `decayCount` 实现“仅用 10 支队伍解出即可让分值降至保底分（50分左右）”的具体参数配置与数学推导。

---

## 2. 衰减模型推导：200 分降至 50 分需要什么 `decayCount`？

### 2.1 系统衰减公式定义
在 `CtfCompetitionServiceImpl.java` 中：
$$S = \text{已解题队伍数（solvesCount）}, \quad I = \text{初始分（200）}, \quad M = \text{底分（50）}, \quad D = \text{decayCount}$$

分值计算公式为：
$$\text{points}(S) = M + \frac{I - M}{1 + \left(\frac{S - 1}{D}\right)^2}$$

最终返回分值为四舍五入整数：$\text{finalPoints} = \text{round}(\max(M, \text{points}))$.

### 2.2 求解第 10 支队伍（$S = 10$）达到最低分（50分）

#### 理论连续解：
要让 $\text{round}(\text{points}(10)) = 50$，必须满足 $\text{points}(10) < 50.5$：
$$50 + \frac{200 - 50}{1 + \left(\frac{10 - 1}{D}\right)^2} < 50.5$$
$$\frac{150}{1 + \left(\frac{9}{D}\right)^2} < 0.5 \implies 1 + \frac{81}{D^2} > 300 \implies \frac{81}{D^2} > 299$$
$$D^2 < \frac{81}{299} \approx 0.2709 \implies D < 0.5205$$

#### 离散整数实操配置：
因为系统数据库与业务层中 `decayCount` 为正整数（$\text{Integer} \ge 1$）：

1. **推荐配置：`decayCount = 1`（最激进、最快衰减）**：
   - 第 1 队（一血）：**200 分**（满分）
   - 第 2 队：$50 + 150 / 2 = \mathbf{125}$ 分
   - 第 3 队：$50 + 150 / 5 = \mathbf{80}$ 分
   - 第 5 队：$50 + 150 / 17 \approx \mathbf{59}$ 分
   - 第 10 队：$50 + 150 / (1 + 81) = 50 + 1.83 = \mathbf{52}$ 分（**已衰减 98.7% 的分值空间，与保底分 50 分仅差 1.8 分，基本见底**）
   - 第 19 队：$50 + 150 / (1 + 324) = 50.46 \to \mathbf{50}$ 分（**精确四舍五入到最低保底分 50 分**）

2. **备选配置：`decayCount = 2`**：
   - 第 1 队：**200 分**
   - 第 2 队：**170 分**
   - 第 3 队：**125 分**（半衰点）
   - 第 10 队：$50 + 150 / (1 + 20.25) \approx \mathbf{57}$ 分

因此，在当前倒数平方平滑衰减模型下，**设置 `decayCount = 1`** 是满足“第 10 队解出时分值降到最低分（52分，几乎等同于50分）”的最佳整数配置。

---

## 3. 改动点明细

### 3.1 前端工程 (`src/volur-lite-view-NEXT`)
1. **实体定义 (`src/types/competition/index.ts`)**：
   - 在 `CtfCompetitionChallengeVO` 中追加 `decayCount?: number`。
2. **运维管理视图 (`AdminCtfCompetitionChallengeDetailView.vue`)**：
   - 挂载弹窗 (`showMountModal`) 与编辑弹窗 (`showEditModal`)：均拓展为 3 列自适应栅格（`初始分数 (Pts)`、`衰减底分 (Min)`、`衰减梯度 (Decay)`），使用规范的 `VInput` 组件双向绑定 `decayCount`；
   - 响应式状态与表单：在 `mountForm` 与 `editForm` 中引入 `decayCount: '30'`；
   - 赛题表格列定义：新增“衰减梯度”展示列（`decayCount`），默认回显 30；
   - 弹窗回填与提交：`openEditModal` 自动读取 `row.decayCount` 回填，`handleConfirmMount` 与 `handleConfirmEdit` 正确解析并提交 `decayCount`。

### 3.2 后端工程 (`src/volur-lite-platform`)
1. **领域模型 (`volur-competition`)**：
   - `CtfCompetitionChallengeVO.java`：新增 `private Integer decayCount` 字段，使 MapStruct 自动对齐持久化实体中的 `decayCount`。
2. **挂载管理服务 (`AdminCtfMountServiceImpl.java`)**：
   - 在 `updateMountedChallenge` 中，若赛题已有队伍解出（`solvesCount > 0`），管理员在后台调整 `initialPoints`、`minPoints` 或 `decayCount` 时，立即重新执行动态分值衰减计算并更新 `currentPoints`，同时清除 Redis 缓存，保证前台看板即刻生效。

---

## 4. 验证结果

1. **后端验证**：
   - 编译检查：`mvn test-compile -pl volur-container,volur-competition -am -DskipTests`（Exit Code 0）；
   - 单元测试：`mvn test -pl volur-competition -am -Dtest=CtfCompetitionCacheTest` 全部 8 项测试通过。
2. **前端验证**：
   - `npm run build`（`vue-tsc --noEmit && vite build`）通过，无任何类型与语法报错。
