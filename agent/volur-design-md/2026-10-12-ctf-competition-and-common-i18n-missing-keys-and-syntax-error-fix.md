# 竞赛模块与通用多语言国际化缺失键补全及模板编译异常修复记录

## 1. 变更背景与问题归因

在进入 CTF 竞赛演练主界面及打开赛题详情弹窗（`CtfCompetitionChallengeModal.vue`）时，前端控制台抛出多条 `[intlify] Not found '<key>' in 'zh' locale messages` 警告，并在弹出模态框时触发了严重未捕获语法异常：

1. **国际化缺失警告 (Vue I18n Missing Key Warnings)**：
   - 竞赛核心选项卡与状态键：`competition.tab_challenges`、`competition.tab_announcements`、`competition.tab_scoreboard`、`competition.tab_my_team`、`competition.no_challenges_found`、`competition.search_challenge`、`competition.awaiting_first_blood` 等；
   - 模态框及赛题详情键：`competition.challenge_tab_detail`、`competition.challenge_tab_solves`、`competition.challenge_description`、`competition.no_description`、`competition.flag_submission`、`competition.flag_input_placeholder` 等；
   - 通用及头部控制键：`common.switch_dark_mode`、`common.switch_light_mode`、`common.tools`、`common.copy`、`common.submitting`、`common.copied_to_clipboard`，以及 `auth.log_out`、`auth.logout_success`、`auth.code_error`。

2. **Vue I18n 消息编译器语法致命崩溃 (Message Compilation SyntaxError)**：
   - 报错栈：
     ```
     vue-i18n.js: SyntaxError: Message compilation error: Invalid token in placeholder: '...'
     1  |  请输入 Flag，如 volur{...}
        |                   ^^^
     ```
   - **根本成因**：
     在 `CtfCompetitionChallengeModal.vue` 中，Flag 输入框占位符使用了 `:placeholder="t('competition.flag_input_placeholder', '请输入 Flag，如 volur{...}')"`。由于 `competition.flag_input_placeholder` 原先未在语言包中声明，Vue-i18n 的 `t()` 方法退化使用回退默认字符串并对其进行解析编译。Vue-i18n 默认将一对大括号 `{...}` 识别为插值占位符（Placeholder），而 `...` 属于非法标识符 token，导致 Vue-i18n 抛出编译期语法错误；由于该调用发生在组件 render 周期，导致 Vue 组件渲染管线异常崩溃，模态框无法正常显示。

---

## 2. 实施方案与改动点

### 2.1 修复模态框 Flag 输入框占位符调用与模板转义
- 在 `src/views/competition/components/CtfCompetitionChallengeModal.vue` 中：
  - 将 `:placeholder="t('competition.flag_input_placeholder', '请输入 Flag，如 volur{...}')"` 规范重构为 `:placeholder="t('competition.flag_input_placeholder')"`；
  - 彻底杜绝在模板内使用带有未转义大括号的回退字面量。

### 2.2 补齐国际化资源文件中的 literal 大括号转义与键值定义
按照 Vue-i18n 的字面量语法（Literal Interpolation），字面量形式的 `{...}` 需采用 `volur{'{...}'}` 进行转义。本次在各语言包中同步补充了：

1. **`src/locales/zh-CN/competition.json`、`src/locales/zh-TW/competition.json`、`src/locales/en-US/competition.json`**：
   - 补全 `flag_input_placeholder`（`"请输入 Flag，如 volur{'{...}'}"` / `"請輸入 Flag，如 volur{'{...}'}"` / `"Enter Flag, e.g. volur{'{...}'}"`）；
   - 补齐全部缺失的 74 个 CTF 竞赛演练相关键，涵盖：
     - `search_challenge`、`challenge_categories`
     - `go_to_login`、`signup_btn`、`view_team_btn`
     - `team_pending_audit`、`team_audit_rejected`、`team_registered_waiting`
     - `section_overview`、`section_rules`、`category_misc`
     - `container_challenge`、`awaiting_first_blood`、`challenge_detail`、`challenge_description`、`no_description`
     - `challenge_attachment`、`download_attachment_tip`、`download_attachment`
     - `container_environment`、`instance_target`、`instance_remaining_time`、`instance_extend_tip`、`instance_extend`、`instance_stop_tip`、`instance_stop`、`instance_start`
     - `flag_submission`、`challenge_already_solved_tip`、`submit_flag`、`first_blood`、`second_blood`、`third_blood`
     - `challenge_tab_detail`、`challenge_tab_solves`、`flag_empty_tip`、`flag_correct`、`flag_incorrect`
     - `instance_started`、`instance_start_failed`、`instance_extended`、`instance_extend_failed`、`instance_stopped`、`instance_stop_failed`
     - `team_name_label`、`team_name_placeholder`、`team_organization_label`、`team_organization_placeholder`、`team_org_type_label`、`team_desc_label`、`team_desc_placeholder`
     - `requirements_title`、`req_reg_time`、`req_comp_time`、`req_member_limit`、`req_leader_limit`、`req_admin_custom_title`、`req_admin_custom_empty`
     - `member_table_title`、`add_member_row`、`col_name`、`col_gender`、`col_role`、`col_employee_number`、`col_student_number`、`col_phone`、`col_email`、`col_id_card`、`col_department_real`、`col_college`、`col_position`、`col_class_name`、`col_status`、`col_action`
     - `role_captain`、`role_member`、`reinvite`、`hide_leader_link`、`add_leader_link`、`leader_table_title`、`add_leader_row`、`role_leader`
     - `disband_team`、`disband_confirm_title`、`disband_confirm_content`
     - `submit_signup`、`edit_team_btn`、`cancel_edit_btn`、`submit_update`
     - `signup_modal_title`、`edit_team_modal_title`、`view_team_modal_title`
     - `status_joined`、`status_invited`、`status_rejected`、`reinvite_success`、`signup_success`
     - `my_invitations_btn`、`no_invitations`、`reject_invitation`、`accept_invitation`
     - `tab_challenges`、`tab_announcements`、`tab_scoreboard`、`tab_my_team`
     - `fetch_failed`、`no_announcements`、`pinned`、`publisher`、`no_challenges_found`、`fetch_challenge_failed`
     - `rank`、`team_name`、`score`、`rank_champion`、`rank_runner_up`、`rank_third`、`my_team_tooltip`、`my_team_tag`
     - `solves`、`last_solve`、`challenge_name`、`category`、`current_points`、`solves_count`、`top_solvers`
     - `no_team_joined`、`team_no_desc`、`organization`、`team_role`、`team_members`、`team_captain_role`、`captain`、`member`、`team_solves_history`
     - `theory_read_only_tip`、`theory_exam_desc`、`theory_total_questions_tip`、`theory_total_questions`、`theory_total_score_tip`、`theory_total_score`、`theory_my_score_tip`、`theory_my_score`。

2. **`src/locales/zh-CN/common.json`、`src/locales/zh-TW/common.json`、`src/locales/en-US/common.json`**：
   - 补充 `tools`、`copy`、`copied_to_clipboard`、`submitting`、`switch_light_mode`、`switch_dark_mode`。

3. **`src/locales/zh-CN/auth/auth.json`、`src/locales/zh-TW/auth/auth.json`、`src/locales/en-US/auth/auth.json`**：
   - 补充 `log_out`、`logout_success`、`code_error`。

---

## 3. 验证与审计结论

1. **语法与编译校验**：
   - 编写专门脚本扫描工程全量 Vue/TS 文件以及全部 JSON 语言包，使用 `vue-i18n` 官方编译器模拟实际解析编译：未再发现任何模板语法或解析异常（0 errors）；
   - 执行全局键名对齐检查：所有在组件中调用的 `competition.*` 与 `common.*` 键已实现 100% 覆盖。
2. **构建验证**：
   - 执行 `npm run build`，生产构建打包顺利通过（`✓ built in 8.14s`），无任何 TypeScript 编译与 Vite 打包报错。
