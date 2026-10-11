# 竞赛详情视图赛程标题与时间显示格式对齐记录

## 1. 变更背景与需求
在 `CompetitionDetailView.vue` 中，用户要求：
1. 将指标区块小标题由 “赛程与关键指标” 精简修改为 “赛程”；
2. 将赛程区块中的 “比赛时间窗口” 修改为 “比赛时间”；
3. 将比赛时间的值（如 `2026-10-12 04:00:00 ~ 2026-10-13 00:00:00`）完全对齐 [VCompetitionCard.vue](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/components/competition/VCompetitionCard.vue) 的时间格式化与排版样式：
   - 格式统一为 `YYYY-MM-DD hh-mm-ss - YYYY-MM-DD hh-mm-ss`（时分秒之间使用连字符 `-`，起止区间采用短横线 ` - ` 连接）；
   - 字体统一指定为 `Inter, sans-serif`（类名 `.comp-time-val`）；
   - 暗夜模式高亮色为 `#FCFCFC`，日间模式为柔和的 `text-google-text-desc`。

## 2. 改动详情

### 2.1 模板与样式重构
- 文件：[CompetitionDetailView.vue](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view-NEXT/src/views/competition/CompetitionDetailView.vue)
- 将 `h4` 标题由 `赛程与关键指标` 修改为 `赛程`；
- 将 `比赛时间窗口` 标签修改为 `比赛时间`，同步将 `报名时间窗口` 优化为 `报名时间`；
- 移除原 `font-mono` 与换行 `<br>~`，替换为使用类名 `.comp-time-val` 渲染单行/自适应流式时间段：
  ```html
  <div class="comp-time-val text-google-text-desc dark:text-[#FCFCFC]">
    {{ formatDateTime(competition.startTime) }} - {{ formatDateTime(competition.endTime) }}
  </div>
  ```
- 在 `<style scoped lang="scss">` 中补充 `.comp-time-val` 样式规则：
  ```scss
  .comp-time-val {
    font-family: Inter, sans-serif;
  }

  html.dark .comp-time-val {
    color: #FCFCFC;
  }
  ```

### 2.2 时间格式化函数对齐
- 将 `formatDateTime` 中的时间单位分隔符由冒号 `:` 调整为连字符 `-`，与 `VCompetitionCard.vue` 保持 100% 格式对齐：
  ```ts
  function formatDateTime(val?: string | null): string {
    if (!val) return '-'
    try {
      const d = new Date(val)
      if (isNaN(d.getTime())) return val
      const pad = (n: number) => (n < 10 ? '0' + n : String(n))
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}-${pad(d.getMinutes())}-${pad(d.getSeconds())}`
    } catch {
      return val
    }
  }
  ```

## 3. 验证与审计
- 遵循零行内注释铁律；
- 运行 `npm run build` 打包构建，TypeScript 类型检查与资源打包全部通过。
