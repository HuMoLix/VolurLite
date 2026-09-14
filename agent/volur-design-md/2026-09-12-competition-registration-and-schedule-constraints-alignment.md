# 竞赛报名起止时间与赛程时间约束对齐设计与实现文档

## 1. 变更背景与需求概述
在 Volur Lite 平台中，原竞赛报名与赛程管理逻辑存在以下缺陷与边界漏洞：
1. **报名起止时间校验缺失赛程维度**：后端在 `CompetitionTeamServiceImpl.java` 中仅校验了 `regStartTime` 和 `regEndTime`，未对竞赛本身的 `startTime`（开赛时间）和竞赛状态 `status` 进行阻断校验。若未配置报名截止时间或报名截止时间晚于开赛时间，参赛选手在比赛开始甚至结束后仍能创建战队或加入战队；
2. **比赛起止时间与过去时间未约束**：在后台新建竞赛或更新未开赛草稿竞赛时，系统未阻断管理员选择“过去的时间”，容易导致创建即失效或与定时状态流转器产生冲突；
3. **报名截止晚于开赛时间的非理性配置**：后台竞赛基础信息中，管理员未被限制在比赛开始之后开放报名，允许出现 `regEndTime > startTime` 的反逻辑配置；
4. **前端与后端约束未统一对齐**：
   - 前端前台展示 `CompetitionDetailView.vue` 中 `isRegistrationOpen` 和 `getRegistrationStatusTip()` 未联动竞赛 `startTime` 和状态（`DRAFT`, `ARCHIVED`, `ENDED`）；
   - 前端报名提交错误时未透传后端错误消息，导致实际阻断原因被吞没；
   - 后台编辑视图 `AdminCompetitionEditView.vue`（子组件 `BasicInfoTab.vue`）中的日期选择器缺少过去日期禁用与报名截止晚于比赛开始时间的禁用限制，以及动态表单提交校验。

针对上述问题，前后端必须实现 **100% 严格对齐** 的业务逻辑、时间模型校验与用户交互体验。

---

## 2. 核心架构与修改点说明

### 2.1 后端约束与校验架构

#### 2.1.1 参赛报名校验：`CompetitionTeamServiceImpl.java`
更新内部私有校验方法 `validateRegistrationPeriod(Competition comp)`：
```java
private void validateRegistrationPeriod(Competition comp) {
    if (comp.getStatus() == CompetitionStatus.DRAFT) {
        throw new BusinessException(HttpStatus.BAD_REQUEST, "竞赛尚未发布，暂不开放报名");
    }
    if (comp.getStatus() == CompetitionStatus.ARCHIVED || comp.getStatus() == CompetitionStatus.ENDED) {
        throw new BusinessException(HttpStatus.BAD_REQUEST, "竞赛已结束或已归档，报名通道已关闭");
    }

    Instant now = Instant.now();
    // 比赛开始后严格禁止报名
    if (comp.getStartTime() != null && !now.isBefore(comp.getStartTime())) {
        throw new BusinessException(HttpStatus.BAD_REQUEST, "比赛已开始，报名通道已关闭");
    }

    if (comp.getRegStartTime() != null && now.isBefore(comp.getRegStartTime())) {
        throw new BusinessException(HttpStatus.BAD_REQUEST, "报名尚未开启");
    }
    if (comp.getRegEndTime() != null && now.isAfter(comp.getRegEndTime())) {
        throw new BusinessException(HttpStatus.BAD_REQUEST, "报名已截止");
    }
}
```
- 覆盖报名创建（`signupTeam`）、战队信息修改（`updateTeam`）与队员受邀入队（`acceptInvitation`）三处入口。

#### 2.1.2 赛程与报名时间有效性校验：`AdminCompetitionServiceImpl.java`
在创建竞赛（`createCompetition`）与更新竞赛（`updateCompetition`）前置统一调用：
```java
private void validateCompetitionSchedule(Instant startTime, Instant endTime, Instant regStartTime, Instant regEndTime, boolean isNewOrNotStarted) {
    if (startTime == null || endTime == null) {
        throw new BusinessException(HttpStatus.BAD_REQUEST, "比赛起止时间不能为空");
    }
    if (!startTime.isBefore(endTime)) {
        throw new BusinessException(HttpStatus.BAD_REQUEST, "比赛开始时间必须早于结束时间");
    }
    Instant now = Instant.now();
    // 比赛起止时间不能是过去的时间（允许 60 秒时钟误差）
    if (isNewOrNotStarted) {
        if (startTime.isBefore(now.minusSeconds(60))) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "比赛开始时间不能早于当前时间，比赛起止时间不能是过去的时间");
        }
        if (endTime.isBefore(now)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "比赛结束时间不能早于当前时间，比赛起止时间不能是过去的时间");
        }
    }

    // 报名时间约束：比赛开始之后不可报名
    if (regStartTime != null && regEndTime != null) {
        if (!regStartTime.isBefore(regEndTime)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "报名开始时间必须早于报名截止时间");
        }
    }
    if (regEndTime != null) {
        if (regEndTime.isAfter(startTime)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "报名起止时间不能在比赛开始之后进行报名（报名截止时间必须早于或等于比赛开始时间）");
        }
    }
    if (regStartTime != null) {
        if (!regStartTime.isBefore(startTime)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "报名开始时间必须早于比赛开始时间");
        }
    }
}
```

---

### 2.2 前端视图与交互对齐架构

#### 2.2.1 选手前台详情页：`CompetitionDetailView.vue`
1. **`isRegistrationOpen` 计算属性收敛**：
   - 处于 `DRAFT`、`ARCHIVED` 或 `ENDED` 状态时均不开放报名；
   - 当前时间 `now >= startTime` 时，严格关闭报名按钮并展示对应提示；
   - 满足 `regStartTime <= now <= regEndTime` 时才允许点击“立即报名”。
2. **`getRegistrationStatusTip()` 状态提示文本精细化**：
   - 分别返回“竞赛未发布，暂不开放报名”、“竞赛已结束或已归档，报名通道已关闭”、“比赛已开始，报名通道已关闭”、“报名开放时间: ...”、“报名已截止”及“报名进行中”。
3. **错误提示透传**：`handleSignupSubmit` 捕获异常透传 `err?.message`，避免吞没后端业务校验返回。

#### 2.2.2 管理后台编辑视图：`BasicInfoTab.vue`
1. **日期选择器日历禁用 (`is-date-disabled`)**：
   - `isCompDateDisabled(ts)`：新建或草稿未开始时，禁止选择今天之前的日期；
   - `isRegDateDisabled(ts)`：若已选择比赛起止时间，禁止选择比赛开始日期之后的日期，且禁止选择过去日期。
2. **时间联动监听 (`watch`)**：
   - 监听比赛时间调整，一旦比赛开始时间提前且早于当前报名截止时间，自动将报名截止时间对齐为开赛时间并给出警告提示；
   - 监听报名时间选择，防止报名时间跨越比赛开始时间。
3. **表单提交校验 (`handleSubmit`)**：
   - 比赛开始时间不能早于当前时间（60秒裕度）；
   - 比赛开始时间必须早于结束时间；
   - 报名开始时间必须早于报名截止时间；
   - 报名开始时间必须早于比赛开始时间；
   - 报名截止时间必须早于或等于比赛开始时间（“报名起止时间不能在比赛开始之后进行报名”）。

---

## 3. 安全隐患修复与质量保证
1. **时钟竞争与时序溢出防护**：
   - 过去时间校验引入 60 秒误差裕度（`now.minusSeconds(60)`），防止因网络传输、时区转换微小延迟造成合法请求被拒；
   - 运行中（`RUNNING`）或已结束竞赛更新配置时，豁免过去时间校验，确保管理员仍能对既有赛事进行必要修补或调整。
2. **类型安全与全量编译保障**：
   - 后端全部通过 `mvn clean compile -DskipTests`（12 个子模块全部 `SUCCESS`）；
   - 前端通过 `vue-tsc --noEmit && vite build` 0 报错验证。

---

## 4. 影响范围与回归测试建议
- **受影响模块**：
  - `volur-competition`（后台竞赛管理 Service、选手战队 Service）；
  - `volur-lite-view`（后台竞赛管理基本信息 Tab、选手前台竞赛详情 View）。
- **回归测试建议**：
  1. 在后台新建竞赛时，尝试选择过去日期，确认前端报错阻断且接口抛出 400；
  2. 尝试选择报名截止时间晚于开赛时间，验证日期组件自动联动与表单阻断；
  3. 待比赛开始后（当前时间 >= startTime），进入前台详情页，验证报名按钮自动置为“比赛已开始，报名通道已关闭”，调用报名接口返回 400。
