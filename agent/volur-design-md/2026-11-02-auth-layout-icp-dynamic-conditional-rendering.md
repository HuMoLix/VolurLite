# AuthLayout 备案信息与版权声明动态条件渲染重构记录

## 1. 变更背景与缺陷剖析

在 [`AuthLayout.vue`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view-NEXT/src/layouts/primary/AuthLayout.vue) 底栏原实现中，备案与版权链接存在如下缺陷：
1. **静态空占位节点残留**：原模板硬编码了 `<span class="hidden md:inline"></span>`，内部未绑定任何响应式变量，且外层 `<a>` 未配置条件渲染逻辑。在无备案信息时，DOM 中仍会无条件挂载该空 `<span>`。
2. **多余响应式版权分流与硬编码**：原模板使用两段独立的 `<span>` 写死文案（`<span class="hidden md:inline">© 2026 Volur, Inc.</span>` 与 `<span class="inline md:hidden">© 2026 Volur</span>`），缺乏动态可配置性，且移动端被强行缩写破坏了品牌统一性。
3. **布局间距异常 (Phantom Gap)**：备案外层 `<a>` 设置了 `gap-1.5`，在桌面端（`>= 768px`）命中 `md:inline` 后，空的 `<span>` 元素参与 Flex 布局计算，导致图标右侧产生无意义的多余空白。
4. **违背 Zero-Mock 与单一真实源规范**：系统未配置备案信息时，仍对外暴露了未配置状态的工信部外部跳转链接与图标；同时此前新一代前端尚未建立基于标准小写点分键（`system.general.icp_number` / `system.general.copyright`）的全局系统配置状态管理与去重获取机制。

---

## 2. 方案设计与改动点

### 2.1 新建 System Pinia Setup Store (`src/store/modules/system.ts`)
- 基于 Composition API Setup Store 风格构建 [`useSystemStore`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view-NEXT/src/store/modules/system.ts)。
- 封装 `fetchPublicSettings(force = false)` 方法：
  - 接入 [`getPublicSystemSettingsApi`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view-NEXT/src/api/system.ts) 获取 `/system/settings/public`；
  - 引入 `inFlightPromise` 在途请求复用与 `isLoaded` 缓存校验，彻底杜绝跨组件并发重复请求。
- 导出强类型计算属性：
  - `icpNumber`: 获取 `system.general.icp_number`（默认空字符串）；
  - `copyright`: 获取 `system.general.copyright`（默认 `© 2026 Volur, Inc.`）；
  - `platformName`: 获取 `system.general.platform_name`；
  - `platformDesc`: 获取 `system.general.platform_desc`；
  - `isMaintenanceMode`: 获取 `system.maintenance.enabled`；
  - `maintenanceNotice`: 获取 `system.maintenance.notice_message`。

### 2.2 重构 AuthLayout (`src/layouts/primary/AuthLayout.vue`)
1. **备案信息条件渲染**：
   - 接入 [`useSystemStore`](file:///Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-view-NEXT/src/store/modules/system.ts)，支持通过 Props `icpNumber?: string` 传入或自动回退至全局 `systemStore.icpNumber`。
   - 在底栏备案链接添加 `v-if="icpNumber"`：
     ```html
     <a
       v-if="icpNumber"
       href="https://beian.miit.gov.cn/"
       target="_blank"
       rel="noopener noreferrer"
       class="auth-btn-link text-[12px] gap-1.5"
       :title="t('common.beian')"
     >
       <img
         src="/r/static/img/beian.png"
         alt="备案"
         class="w-3.5 h-3.5 object-contain shrink-0"
       />
       <span class="hidden md:inline">{{ icpNumber }}</span>
     </a>
     ```
   - 当 `icpNumber` 为空或未配置时，整段 `<a>` 及其内部 `<img>` 与 `<span>` 被 Vue `v-if` 从 DOM 树彻底移除。

2. **版权声明统一与硬编码删除**：
   - 彻底删除 `<span class="inline md:hidden">© 2026 Volur</span>`；
   - 增加 Props `copyright?: string`，计算属性默认指向 `systemStore.copyright || '© 2026 Volur, Inc.'`；
   - 简化模板为纯净单一动态节点并增加判空防护：
     ```html
     <span
       v-if="copyright"
       class="auth-footer__copyright whitespace-nowrap px-2 text-[12px] inline-flex items-center h-8 select-none"
     >
       {{ copyright }}
     </span>
     ```

### 2.3 协同优化 AppFooter 与 App 根组件
- **`AppFooter.vue`**：同步接入 `useSystemStore`，针对分割点 `·` 与备案外链添加 `v-if="effectiveIcpText"` 条件渲染，版权声明统一采用 `effectiveCopyright`，当系统未配置备案信息时不输出空节点与多余分隔符。
- **`App.vue`**：在挂载阶段安全调用 `systemStore.fetchPublicSettings()`，实现全局公开配置的早期预热。

---

## 3. 验证与构建结果

- 执行 `npm run build`（`vue-tsc --noEmit && vite build`）：
  - TypeScript 类型检查：0 错误，严格类型全部通过。
  - Vite 生产构建产物打包完全通过。
- 零注释与外置文档规范校验：修改的文件内均严格保持无解释性注释，所有演进记录均归档在本方案文档中。
