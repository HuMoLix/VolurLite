# 管理后台语言包 (admin.json) 模块化拆分与多层级动态加载改造文档

## 1. 变更背景与目标
随着 Volur Lite 后台管理系统业务功能不断丰富，原先扁平的单体语言包 `admin.json` 体积快速膨胀，涵盖了控制台仪表盘、用户管理、全类型题库管理、竞赛编排、公告广播、容器集群、系统日志审计与系统动态设置等全部模块，维护复杂度高且不利于模块化协同。

为进一步优化前端工程架构，提高词条组织清晰度并符合微前端/模块化规范：
1. 升级 `src/volur-lite-view/src/i18n.ts` 中的 `loadLocaleMessages` 动态加载器，支持多层级目录递归构筑命名空间；
2. 将 `zh_CN/admin.json` 和 `en_US/admin.json` 单体大文件拆分为独立的 `admin/` 目录；
3. 将后台 8 大业务域分别独立为子 JSON 文件（`dashboard.json`、`users.json`、`challenges.json`、`competitions.json`、`broadcasts.json`、`containers.json`、`logs.json`、`system.json`）；
4. 保证在视图与路由中原有 `t('admin.<module>.<key>')` 调用链 100% 完全向下兼容，无需改动任何既有组件调用。

---

## 2. 变更详情

### 2.1 `i18n.ts` 动态递归层级加载器升级
- 修改 `src/volur-lite-view/src/i18n.ts` 中的 `loadLocaleMessages()`：
  - 由原先仅根据末尾文件名绑定的逻辑，升级为根据 `parts` 路径各层级（例如 `['zh_CN', 'admin', 'system']`）自动在 `messages[locale]` 中创建中间对象层级；
  - 自动将 `admin/system.json` 的内容挂载至 `messages[locale].admin.system`，从而完美支持 `t('admin.system.*')` 访问。

### 2.2 目录拆分与文件结构
在 `zh_CN` 与 `en_US` 下创建 `admin/` 文件夹并分别拆分为 8 个模块文件：
```
src/volur-lite-view/src/locales/
├── zh_CN/
│   ├── admin/
│   │   ├── dashboard.json      # 控制台概览与指标
│   │   ├── users.json          # 用户管理
│   │   ├── challenges.json     # CTF/理论/ISW/AWD/AWD+ 题库
│   │   ├── competitions.json   # 竞赛管理
│   │   ├── broadcasts.json     # 广播与公告管理
│   │   ├── containers.json     # 容器环境管理
│   │   ├── logs.json           # 系统日志审计
│   │   └── system.json         # 平台系统设置与运维
│   ├── auth.json
│   ├── common.json
│   ├── competition.json
│   ├── error.json
│   └── layout.json
└── en_US/
    ├── admin/
    │   ├── dashboard.json
    │   ├── users.json
    │   ├── challenges.json
    │   ├── competitions.json
    │   ├── broadcasts.json
    │   ├── containers.json
    │   ├── logs.json
    │   └── system.json
    ├── auth.json
    ├── common.json
    ├── competition.json
    ├── error.json
    └── layout.json
```
- 删除旧版单体 `zh_CN/admin.json` 与 `en_US/admin.json`。

---

## 3. 验证与回归测试
- **构建测试**：在 `src/volur-lite-view` 执行 `npm run build`，`vue-tsc` 与 `vite build` 顺利通过，未出现任何未定义键或 TypeScript 类型报错。
- **调用兼容性**：各管理后台视图（`AdminSystemView.vue`、`AdminLogView.vue`、`AdminBroadcastView.vue`、`AdminCompetitionView.vue`、`AdminChallengeView.vue` 等）与 `router/routes.ts` 中的 `titleKey`（例如 `admin.challenges.ctf_title`）均正常解析与生效。

---

## 4. 影响范围
- [`src/volur-lite-view/src/i18n.ts`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/i18n.ts)
- [`src/volur-lite-view/src/locales/zh_CN/admin/`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/locales/zh_CN/admin)
- [`src/volur-lite-view/src/locales/en_US/admin/`](file:///H:/HuMoLix/github/Volur-Lite/src/volur-lite-view/src/locales/en_US/admin)
