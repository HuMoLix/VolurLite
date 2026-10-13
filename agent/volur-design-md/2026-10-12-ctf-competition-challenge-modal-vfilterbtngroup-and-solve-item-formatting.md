# CTF 赛题模态框 Tabs 按钮组组件替换与解题项格式化施工记录

## 1. 变更背景与需求概述
针对 `CtfCompetitionChallengeModal.vue`（CTF 赛题详情模态框），进一步规范组件复用与视觉呈现：
1. 将自定义手写的 Tabs 按钮切换栏统一重构为平台通用组件 `VFilterBtnGroup`；
2. 彻底移除「已解出战队」右侧多余的数字角标徽章（`<span class="px-1.5 py-0.2 rounded-full ...">3</span>`），保持按钮标题纯净；
3. 解出队伍列表项右侧的时间展示栏移除 `TimeOutline` 图标，并将解出时间统一格式化为 `YYYY-MM-DD hh-mm-ss` 格式；
4. 获得积分展示栏移除数字前面的 `+` 符号，保持 `[分值] pts` 规范显示。

## 2. 核心改动点

### 2.1 引入 `VFilterBtnGroup` 接管 Tabs
- 从 `@/components/common` 导入 `VFilterBtnGroup` 及 `FilterOption` 类型定义；
- 将手写按钮组替换为：
  ```vue
  <div class="border-b border-google-border-light/80 dark:border-google-border-dark/80 pb-3 mb-5">
    <VFilterBtnGroup
      v-model="activeTab"
      :options="tabOptions"
      :responsive="false"
    />
  </div>
  ```
- 声明 `tabOptions` 计算属性，选项不配置 `count` 字段，彻底移除角标徽章：
  - 选项 1：`value: 'content'`，`label: '赛题内容'`，`icon: DocumentTextOutline`；
  - 选项 2：`value: 'solves'`，`label: '已解出战队'`，`icon: TrophyOutline`。

### 2.2 解出战队列表项展示格式重构
- **时间列图标移除**：移除原时间文本前的 `<n-icon :size="12" :component="TimeOutline" />`，仅保留纯文本渲染；
- **时间格式严格对齐 `YYYY-MM-DD hh-mm-ss`**：
  在 `formatSolveTime` 中升级解析器，兼容 ISO 8601 标准时间戳、相对时间描述（如 `10 分钟前`）以及标准日期串，统一输出为 `${YYYY}-${MM}-${DD} ${HH}-${mm}-${ss}` 格式；
- **分值符号规范**：移除 `+` 号，改为纯数值与单位组合：
  ```vue
  <span class="font-bold text-google-blue dark:text-google-blue-dark">
    {{ team.points ?? team.pointsAwarded ?? challenge.currentPoints }} pts
  </span>
  ```

## 3. 规范遵循与验证
- 严格遵循纯代码零注释铁律，代码内无冗余行内解释性注释；
- 符合 Google Workspace 色彩体系与 ES2026 / Vue 3 Setup 语法规范；
- 全量构建验证：执行 `vue-tsc --noEmit && vite build`，零类型报错与构建警告。

## 4. 影响范围
- `src/views/competition/components/CtfCompetitionChallengeModal.vue`
