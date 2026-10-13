# CTF 赛题题型 DYNAMIC_CONTAINER 对齐修正审计文档

## 变更背景
用户在调用赛题创建接口 `POST /api/v1/admin/challenges/ctf` 时遇到 HTTP 400 异常：
`"message": "请求体格式不正确或缺少请求体"`。

经排查，前端（`volur-lite-view-NEXT`）在重构赛题编辑表单选择器时，将“动态靶机”选项绑定的枚举值写为了 `DYNAMIC_CONTAINER`，并在 TypeScript 类型定义 `ChallengeType` 中包含了该联合成员。而后端领域枚举 `ChallengeType` 严格遵循单一数据源，仅包含 `STATIC`、`DYNAMIC` 与 `CONTAINER`，不存在 `DYNAMIC_CONTAINER`。当 Jackson 在反序列化请求体时无法匹配该值，抛出 `HttpMessageNotReadableException`，导致请求在解析层被拦截。

为彻底消除契约不一致与历史遗留混淆，现将前端全链路涉及的 `DYNAMIC_CONTAINER` 全面收敛对齐为 `DYNAMIC`，并在后端枚举中增强反序列化自愈解析能力。

## 详细改动点

### 1. 新版前端工程 (volur-lite-view-NEXT)
- **类型定义对齐 (`src/types/challenge.ts`)**：
  - 从联合类型 `ChallengeType` 中彻底剔除 `DYNAMIC_CONTAINER`，统一收敛为 `'STATIC' | 'DYNAMIC' | 'CONTAINER'`。
- **编辑视图选项对齐 (`src/views/admin/challenge/AdminCtfChallengeEditView.vue`)**：
  - 将 `typeOptions` 中“动态靶机”项的绑定值从 `DYNAMIC_CONTAINER` 修正为 `DYNAMIC`；
  - 容器赛题条件判断 `isContainerType` 中移除冗余 `DYNAMIC_CONTAINER` 分支，对齐为 `type === 'CONTAINER' || type === 'DYNAMIC'`。
- **列表视图对齐 (`src/views/admin/challenge/AdminCtfChallengeView.vue`)**：
  - 赛题类型格式化函数 `getTypeText` 与筛选匹配函数 `matchType` 中移除 `DYNAMIC_CONTAINER` 分支。

### 2. 经典前端工程 (volur-lite-view)
- **类型定义对齐 (`src/types/challenge/index.ts`)**：
  - 从联合类型 `ChallengeType` 中剔除 `DYNAMIC_CONTAINER`。
- **竞赛挂载赛题组件修复 (`src/views/admin/competition/tabs/ChallengeTab.vue`)**：
  - 将题库表格题型渲染逻辑 `row.type === 'DYNAMIC_CONTAINER'` 修正为 `(row.type === 'DYNAMIC' || row.type === 'CONTAINER')`，修复原逻辑中动态容器赛题因枚举不匹配被误识别为“静态题目”的缺陷。

### 3. 后端服务平台 (volur-challenge)
- **枚举反序列化健壮性强化 (`ChallengeType.java`)**：
  - 保持 `STATIC`、`DYNAMIC`、`CONTAINER` 为唯一法定枚举常量；
  - 引入 `@JsonCreator` 的 `fromValue(Object value)` 与 `fromCode(int code)` 方法，支持忽略大小写字符串映射及整型 code 自动解析，增强全链路防御。

## 影响范围与验证
- `src/volur-lite-view-NEXT/src/types/challenge.ts`
- `src/volur-lite-view-NEXT/src/views/admin/challenge/AdminCtfChallengeEditView.vue`
- `src/volur-lite-view-NEXT/src/views/admin/challenge/AdminCtfChallengeView.vue`
- `src/volur-lite-view/src/types/challenge/index.ts`
- `src/volur-lite-view/src/views/admin/competition/tabs/ChallengeTab.vue`
- `src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge/domain/enums/ChallengeType.java`

经 Maven 编译验证，`volur-challenge` 模块 `BUILD SUCCESS`；前端 `vue-tsc --noEmit` 编译无任何类型报错。
