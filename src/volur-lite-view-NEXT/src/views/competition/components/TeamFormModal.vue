<template>
  <n-modal
    :show="show"
    preset="card"
    class="team-form-modal w-[1040px] max-w-[96vw] rounded-2xl border border-google-border-light dark:border-google-border-dark bg-white dark:bg-google-surface-darkCard"
    :title="modalTitle"
    :bordered="false"
    @update:show="handleClose"
  >
    <div class="team-meta-container grid grid-cols-1 lg:grid-cols-[1fr_1px_1fr] gap-5 items-stretch mb-5">
      <div class="meta-left-panel min-w-0 w-full">
        <n-form
          ref="formRef"
          :model="formData"
          :rules="formRules"
          label-placement="top"
          require-mark-placement="right-hanging"
          class="flex flex-col gap-2.5"
        >
          <div class="flex items-center gap-3.5 w-full">
            <div class="relative group shrink-0">
              <VAvatar
                :src="formData.avatarUrl || ''"
                :alt="formData.teamName || 'Team'"
                :size="48"
                :radius="16"
                class="border border-google-border-light dark:border-google-border-dark shrink-0"
              >
                <template #guest>
                  <n-icon :size="24" :component="PeopleOutline" class="text-google-blue dark:text-google-blue-dark" />
                </template>
              </VAvatar>
              <n-tooltip v-if="isEditing && isCaptain" trigger="hover">
                <template #trigger>
                  <div
                    class="absolute inset-0 rounded-[16px] bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity duration-200 flex items-center justify-center cursor-pointer text-white"
                    :class="{ 'opacity-100 bg-black/50 cursor-wait': uploadingAvatar }"
                    @click="triggerAvatarUpload"
                  >
                    <n-spin v-if="uploadingAvatar" size="small" stroke="#ffffff" />
                    <n-icon v-else :component="CameraOutline" :size="20" />
                  </div>
                </template>
                {{ formData.avatarUrl ? t('competition.change_team_avatar', '更换战队头像') : t('competition.upload_team_avatar', '上传战队头像') }}
              </n-tooltip>
              <button
                v-if="formData.avatarUrl && isEditing && isCaptain && !uploadingAvatar"
                type="button"
                class="absolute -top-1.5 -right-1.5 w-5 h-5 rounded-full bg-white dark:bg-google-surface-dark border border-google-border-light dark:border-google-border-dark flex items-center justify-center text-google-text-muted dark:text-google-text-darkMuted hover:text-google-red dark:hover:text-google-red-dark shadow-sm cursor-pointer z-10 transition-colors"
                @click.stop="handleRemoveAvatar"
              >
                <n-icon :component="CloseOutline" :size="12" />
              </button>
              <input
                ref="avatarInputRef"
                type="file"
                accept="image/png,image/jpeg,image/jpg,image/webp,image/gif"
                class="hidden"
                @change="handleAvatarFileChange"
              />
            </div>
            <n-form-item
              :label="t('competition.team_name_label', '战队名称')"
              path="teamName"
              class="flex-1 min-w-0 m-0"
            >
              <VInput
                v-model:value="formData.teamName"
                :placeholder="t('competition.team_name_placeholder', '请输入战队名称 (2-32 字符)')"
                maxlength="32"
                :disabled="props.mode !== 'create'"
                show-count
                clearable
                class="w-full"
              />
            </n-form-item>
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-[1fr_140px] gap-3 w-full">
            <n-form-item
              :label="t('competition.team_organization_label', '队伍单位')"
              path="organization"
              class="m-0"
            >
              <VInput
                v-model:value="formData.organization"
                :placeholder="t('competition.team_organization_placeholder', '请输入队伍所属单位')"
                maxlength="128"
                :disabled="!isEditing"
                clearable
                class="w-full"
              />
            </n-form-item>

            <n-form-item
              :label="t('competition.team_org_type_label', '单位类型')"
              path="organizationType"
              class="m-0"
            >
              <VSelection
                v-model:value="formData.organizationType"
                :options="organizationTypeOptions"
                :disabled="!isEditing"
                class="w-full"
              />
            </n-form-item>
          </div>

          <n-form-item
            :label="t('competition.team_desc_label', '队伍简介')"
            path="description"
            class="m-0 w-full"
          >
            <VInput
              v-model:value="formData.description"
              type="textarea"
              :placeholder="t('competition.team_desc_placeholder', '请输入队伍简介或口号')"
              maxlength="200"
              :disabled="!isEditing"
              show-count
              :autosize="{ minRows: 2, maxRows: 2 }"
              class="w-full"
            />
          </n-form-item>
        </n-form>
      </div>

      <div class="meta-divider hidden lg:block w-px bg-google-border-light dark:border-google-border-dark" />

      <div class="meta-right-panel min-w-0 w-full">
        <div class="h-full rounded-xl p-3.5 bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light/80 dark:border-google-border-dark/80 flex flex-col gap-2.5">
          <div class="flex items-center justify-between pb-2 border-b border-google-border-light dark:border-google-border-dark">
            <div class="flex items-center gap-1.5 text-xs font-semibold text-google-text-primary dark:text-[#FCFCFC]">
              <n-icon :component="InformationCircleOutline" :size="16" class="text-google-blue dark:text-google-blue-dark" />
              <span>{{ t('competition.requirements_title', '竞赛报名要求与须知') }}</span>
            </div>
          </div>

          <div class="flex flex-col gap-2 text-xs flex-1">
            <div class="flex items-baseline gap-2">
              <span class="w-16 shrink-0 text-google-text-muted dark:text-google-text-darkMuted">{{ t('competition.req_reg_time', '报名时间') }}:</span>
              <span class="text-google-text-primary dark:text-google-text-darkPrimary font-mono">{{ formattedRegTimeRange }}</span>
            </div>
            <div class="flex items-baseline gap-2">
              <span class="w-16 shrink-0 text-google-text-muted dark:text-google-text-darkMuted">{{ t('competition.req_comp_time', '比赛时间') }}:</span>
              <span class="text-google-text-primary dark:text-google-text-darkPrimary font-mono">{{ formattedCompTimeRange }}</span>
            </div>
            <div class="flex items-baseline gap-2">
              <span class="w-16 shrink-0 text-google-text-muted dark:text-google-text-darkMuted">{{ t('competition.req_member_limit', '人数要求') }}:</span>
              <span class="text-google-text-primary dark:text-google-text-darkPrimary">
                每队 {{ competition?.minTeamMembers ?? 1 }} ~ {{ competition?.maxTeamMembers ?? 4 }} 人
              </span>
            </div>
            <div class="flex items-baseline gap-2">
              <span class="w-16 shrink-0 text-google-text-muted dark:text-google-text-darkMuted">{{ t('competition.req_leader_limit', '领队要求') }}:</span>
              <span class="text-google-text-primary dark:text-google-text-darkPrimary">
                最多 {{ competition?.maxLeaders ?? 1 }} 名指导老师/领队
              </span>
            </div>

            <div class="mt-1 pt-2 border-t border-dashed border-google-border-light dark:border-google-border-dark flex flex-col gap-1">
              <span class="font-semibold text-google-text-desc dark:text-google-text-darkDesc">{{ t('competition.req_admin_custom_title', '特别说明') }}:</span>
              <div class="text-[11px] leading-relaxed p-2 rounded bg-white dark:bg-google-surface-darkCard border border-google-border-light/60 dark:border-google-border-dark/60 max-h-16 overflow-y-auto whitespace-pre-wrap break-words text-google-text-desc dark:text-google-text-darkDesc">
                <template v-if="adminCustomRequirements">
                  {{ adminCustomRequirements }}
                </template>
                <template v-else>
                  <span class="text-google-text-muted dark:text-google-text-darkMuted italic">{{ t('competition.req_admin_custom_empty', '暂无额外补充要求') }}</span>
                </template>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="flex items-center justify-between mb-3 pb-2 border-b border-google-border-light dark:border-google-border-dark">
      <div class="flex items-baseline gap-3 flex-wrap">
        <h4 class="m-0 text-sm font-semibold text-google-text-primary dark:text-[#FCFCFC]">{{ t('competition.member_table_title', '参赛成员名册') }}</h4>
        <span class="text-xs text-google-text-desc dark:text-google-text-darkDesc">
          当前成员: <strong class="text-google-text-primary dark:text-[#FCFCFC]">{{ members.length }}</strong> /
          限制: {{ competition?.minTeamMembers ?? 1 }} ~ {{ competition?.maxTeamMembers ?? 4 }} 人
          <span class="text-google-text-muted dark:text-google-text-darkMuted ml-1">
            （上限: {{ competition?.maxTeamMembers ?? 4 }} 人）
          </span>
        </span>
      </div>

      <VBtn
        v-if="isEditing"
        clz="normal"
        :left-icon="AddOutline"
        :disabled="isMaxMembersReached"
        @click="addMemberRow"
      >
        {{ t('competition.add_member_row', '添加成员行') }}
      </VBtn>
    </div>

    <div class="overflow-x-auto mb-4 border border-google-border-light dark:border-google-border-dark rounded-xl">
      <n-table :bordered="true" :single-line="false" size="small" class="min-w-[1240px] text-xs">
        <thead>
          <tr class="bg-google-surface-light dark:bg-google-surface-dark text-google-text-desc dark:text-google-text-darkDesc whitespace-nowrap">
            <th class="w-24 p-2.5 font-semibold">{{ t('competition.col_name', '姓名') }} <span v-if="isEditing" class="text-google-red dark:text-google-red-dark">*</span></th>
            <th class="w-20 p-2.5 font-semibold">{{ t('competition.col_gender', '性别') }} <span v-if="isEditing" class="text-google-red dark:text-google-red-dark">*</span></th>
            <th class="w-20 p-2.5 font-semibold text-center">{{ t('competition.col_role', '角色') }}</th>
            <th class="w-32 p-2.5 font-semibold">
              {{ formData.organizationType !== 'SCHOOL' ? t('competition.col_employee_number', '工号') : t('competition.col_student_number', '学号') }}
              <span v-if="isEditing" class="text-google-red dark:text-google-red-dark">*</span>
            </th>
            <th class="w-32 p-2.5 font-semibold">{{ t('competition.col_phone', '手机号') }} <span v-if="isEditing" class="text-google-red dark:text-google-red-dark">*</span></th>
            <th class="w-40 p-2.5 font-semibold">{{ t('competition.col_email', '邮箱') }} <span v-if="isEditing" class="text-google-red dark:text-google-red-dark">*</span></th>
            <th class="w-44 p-2.5 font-semibold">{{ t('competition.col_id_card', '身份证') }} <span v-if="isEditing" class="text-google-red dark:text-google-red-dark">*</span></th>
            <th class="w-32 p-2.5 font-semibold">
              {{ formData.organizationType !== 'SCHOOL' ? t('competition.col_department_real', '部门') : t('competition.col_college', '学院') }}
              <span v-if="isEditing" class="text-google-red dark:text-google-red-dark">*</span>
            </th>
            <th class="w-28 p-2.5 font-semibold">
              {{ formData.organizationType !== 'SCHOOL' ? t('competition.col_position', '职位') : t('competition.col_class_name', '班级') }}
              <span v-if="isEditing" class="text-google-red dark:text-google-red-dark">*</span>
            </th>
            <th class="w-24 p-2.5 font-semibold text-center">{{ t('competition.col_status', '加入状态') }}</th>
            <th v-if="isEditing || hasViewActions" class="w-28 p-2.5 font-semibold text-center">{{ t('competition.col_action', '操作') }}</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-google-border-light/60 dark:divide-google-border-dark/60 bg-white dark:bg-google-surface-darkCard">
          <tr
            v-for="(row, idx) in members"
            :key="idx"
            :class="idx === 0 ? 'bg-google-surface-light/40 dark:bg-google-surface-dark/30' : ''"
          >
            <td class="p-2">
              <VInput
                v-model:value="row.realName"
                size="small"
                :disabled="!isEditing"
                :placeholder="t('competition.col_name', '姓名')"
                maxlength="64"
              />
            </td>
            <td class="p-2">
              <VSelection
                v-model:value="row.gender"
                size="small"
                :disabled="!isEditing"
                :options="genderOptions"
              />
            </td>
            <td class="p-2 text-center font-medium">
              <span :class="idx === 0 ? 'text-amber-600 dark:text-amber-400' : 'text-google-text-desc dark:text-google-text-darkDesc'">
                {{ idx === 0 ? t('competition.role_captain', '队长') : t('competition.role_member', '队员') }}
              </span>
            </td>
            <td class="p-2">
              <VInput
                v-model:value="row.employeeNumber"
                size="small"
                :disabled="!isEditing"
                :placeholder="formData.organizationType !== 'SCHOOL' ? t('competition.col_employee_number', '工号') : t('competition.col_student_number', '学号')"
                maxlength="64"
              />
            </td>
            <td class="p-2">
              <VInput
                v-model:value="row.phone"
                size="small"
                :disabled="!isEditing"
                :placeholder="t('competition.col_phone', '手机号')"
                maxlength="16"
              />
            </td>
            <td class="p-2">
              <VInput
                v-model:value="row.email"
                size="small"
                :disabled="!isEditing"
                :placeholder="t('competition.col_email', '邮箱')"
                maxlength="128"
              />
            </td>
            <td class="p-2">
              <VInput
                :value="isEditing ? row.idCard : formatMaskedIdCard(row.idCard)"
                size="small"
                :disabled="!isEditing"
                :placeholder="t('competition.col_id_card', '身份证')"
                maxlength="32"
                @update:value="(val) => { if (isEditing) row.idCard = val }"
              />
            </td>
            <td class="p-2">
              <VInput
                v-model:value="row.department"
                size="small"
                :disabled="!isEditing"
                :placeholder="formData.organizationType !== 'SCHOOL' ? t('competition.col_department_real', '部门') : t('competition.col_college', '学院')"
                maxlength="128"
              />
            </td>
            <td class="p-2">
              <VInput
                v-model:value="row.position"
                size="small"
                :disabled="!isEditing"
                :placeholder="formData.organizationType !== 'SCHOOL' ? t('competition.col_position', '职位') : t('competition.col_class_name', '班级')"
                maxlength="64"
              />
            </td>
            <td class="p-2 text-center">
              <n-tag
                size="small"
                :type="getMemberStatusTag(row).type"
                round
              >
                {{ getMemberStatusTag(row).label }}
              </n-tag>
            </td>
            <td v-if="isEditing || hasViewActions" class="p-2 text-center">
              <div v-if="isEditing">
                <n-button
                  size="tiny"
                  quaternary
                  type="error"
                  :disabled="idx === 0 || members.length <= 1"
                  @click="removeMemberRow(idx)"
                >
                  <template #icon>
                    <n-icon :component="TrashOutline" />
                  </template>
                </n-button>
              </div>
              <div v-else-if="isCaptain && props.mode !== 'create' && row.userId" class="flex items-center justify-center gap-1.5">
                <button
                  v-if="row.status === 'NOT_PARTICIPATE' || row.status === 'INVITED'"
                  type="button"
                  class="px-2 py-0.5 rounded text-[11px] text-google-blue dark:text-google-blue-dark hover:underline bg-transparent border-none cursor-pointer"
                  :disabled="reinvitingUserId === row.userId"
                  @click="handleReinvite(row)"
                >
                  {{ t('competition.reinvite', '重新邀请') }}
                </button>
                <n-popconfirm
                  v-if="row.role !== 'CAPTAIN'"
                  positive-text="确认移除"
                  negative-text="取消"
                  @positive-click="handleCancelMember(row)"
                >
                  <template #trigger>
                    <button
                      type="button"
                      class="px-2 py-0.5 rounded text-[11px] text-google-red dark:text-google-red-dark hover:underline bg-transparent border-none cursor-pointer"
                    >
                      移除
                    </button>
                  </template>
                  <span>确定将该成员从队伍中移除吗？</span>
                </n-popconfirm>
              </div>
            </td>
          </tr>
        </tbody>
      </n-table>
    </div>

    <div v-if="isEditing && (competition?.maxLeaders ?? 1) > 0" class="flex justify-end my-2">
      <button
        type="button"
        class="text-xs text-google-blue dark:text-google-blue-dark hover:underline bg-transparent border-none cursor-pointer flex items-center gap-1 font-medium"
        @click="toggleLeadersSection"
      >
        {{ showLeadersSection ? t('competition.hide_leader_link', '收起领队/指导老师') : t('competition.add_leader_link', '添加领队/指导老师') }}
      </button>
    </div>

    <transition name="fade">
      <div v-if="isEditing ? showLeadersSection : (leaders.length > 0)" class="mb-5">
        <div class="flex items-center justify-between mb-3 pb-2 border-b border-dashed border-google-border-light dark:border-google-border-dark">
          <div class="flex items-baseline gap-3 flex-wrap">
            <h4 class="m-0 text-sm font-semibold text-google-text-primary dark:text-[#FCFCFC]">{{ t('competition.leader_table_title', '领队/指导老师') }}</h4>
            <span class="text-xs text-google-text-desc dark:text-google-text-darkDesc">
              当前领队: <strong class="text-google-text-primary dark:text-[#FCFCFC]">{{ leaders.length }}</strong> /
              上限: {{ competition?.maxLeaders ?? 1 }} 人 (可选填)
            </span>
          </div>

          <VBtn
            v-if="isEditing"
            clz="normal"
            :left-icon="AddOutline"
            :disabled="isMaxLeadersReached"
            @click="addLeaderRow"
          >
            {{ t('competition.add_leader_row', '添加领队行') }}
          </VBtn>
        </div>

        <div class="overflow-x-auto border border-google-border-light dark:border-google-border-dark rounded-xl">
          <n-table :bordered="true" :single-line="false" size="small" class="min-w-[1240px] text-xs">
            <thead>
              <tr class="bg-google-surface-light dark:bg-google-surface-dark text-google-text-desc dark:text-google-text-darkDesc whitespace-nowrap">
                <th class="w-24 p-2.5 font-semibold">{{ t('competition.col_name', '姓名') }} <span v-if="isEditing" class="text-google-red dark:text-google-red-dark">*</span></th>
                <th class="w-20 p-2.5 font-semibold">{{ t('competition.col_gender', '性别') }}</th>
                <th class="w-20 p-2.5 font-semibold text-center">{{ t('competition.col_role', '角色') }}</th>
                <th class="w-32 p-2.5 font-semibold">
                  {{ formData.organizationType !== 'SCHOOL' ? t('competition.col_employee_number', '工号') : t('competition.col_student_number', '学号') }}
                </th>
                <th class="w-32 p-2.5 font-semibold">{{ t('competition.col_phone', '手机号') }} <span v-if="isEditing" class="text-google-red dark:text-google-red-dark">*</span></th>
                <th class="w-40 p-2.5 font-semibold">{{ t('competition.col_email', '邮箱') }}</th>
                <th class="w-44 p-2.5 font-semibold">{{ t('competition.col_id_card', '身份证') }}</th>
                <th class="w-32 p-2.5 font-semibold">
                  {{ formData.organizationType !== 'SCHOOL' ? t('competition.col_department_real', '部门') : t('competition.col_college', '学院') }}
                </th>
                <th class="w-28 p-2.5 font-semibold">
                  {{ formData.organizationType !== 'SCHOOL' ? t('competition.col_position', '职位') : t('competition.col_class_name', '班级') }}
                </th>
                <th v-if="isEditing" class="w-16 p-2.5 font-semibold text-center">{{ t('competition.col_action', '操作') }}</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-google-border-light/60 dark:divide-google-border-dark/60 bg-white dark:bg-google-surface-darkCard">
              <tr v-if="leaders.length === 0">
                <td :colspan="isEditing ? 10 : 9" class="p-4 text-center text-google-text-muted dark:text-google-text-darkMuted">
                  暂无领队/指导老师，点击上方按钮添加
                </td>
              </tr>
              <tr
                v-for="(row, idx) in leaders"
                :key="'leader-' + idx"
              >
                <td class="p-2">
                  <VInput
                    v-model:value="row.realName"
                    size="small"
                    :disabled="!isEditing"
                    :placeholder="t('competition.col_name', '姓名')"
                    maxlength="64"
                  />
                </td>
                <td class="p-2">
                  <VSelection
                    v-model:value="row.gender"
                    size="small"
                    :disabled="!isEditing"
                    :options="genderOptions"
                  />
                </td>
                <td class="p-2 text-center font-medium text-google-text-desc dark:text-google-text-darkDesc">
                  {{ t('competition.role_leader', '指导老师') }}
                </td>
                <td class="p-2">
                  <VInput
                    v-model:value="row.employeeNumber"
                    size="small"
                    :disabled="!isEditing"
                    placeholder="选填"
                    maxlength="64"
                  />
                </td>
                <td class="p-2">
                  <VInput
                    v-model:value="row.phone"
                    size="small"
                    :disabled="!isEditing"
                    :placeholder="t('competition.col_phone', '手机号')"
                    maxlength="16"
                  />
                </td>
                <td class="p-2">
                  <VInput
                    v-model:value="row.email"
                    size="small"
                    :disabled="!isEditing"
                    placeholder="选填"
                    maxlength="128"
                  />
                </td>
                <td class="p-2">
                  <VInput
                    :value="isEditing ? row.idCard : formatMaskedIdCard(row.idCard)"
                    size="small"
                    :disabled="!isEditing"
                    placeholder="选填"
                    maxlength="32"
                    @update:value="(val) => { if (isEditing) row.idCard = val }"
                  />
                </td>
                <td class="p-2">
                  <VInput
                    v-model:value="row.department"
                    size="small"
                    :disabled="!isEditing"
                    placeholder="选填"
                    maxlength="128"
                  />
                </td>
                <td class="p-2">
                  <VInput
                    v-model:value="row.position"
                    size="small"
                    :disabled="!isEditing"
                    placeholder="选填"
                    maxlength="64"
                  />
                </td>
                <td v-if="isEditing" class="p-2 text-center">
                  <n-button
                    size="tiny"
                    quaternary
                    type="error"
                    @click="removeLeaderRow(idx)"
                  >
                    <template #icon>
                      <n-icon :component="TrashOutline" />
                    </template>
                  </n-button>
                </td>
              </tr>
            </tbody>
          </n-table>
        </div>
      </div>
    </transition>

    <div class="flex items-center justify-between pt-4 mt-2 border-t border-google-border-light dark:border-google-border-dark flex-wrap gap-3">
      <div>
        <n-popconfirm
          v-if="props.mode !== 'create' && isCaptain"
          :positive-text="t('common.confirm', '确认')"
          :negative-text="t('common.cancel', '取消')"
          @positive-click="handleDisbandTeam"
        >
          <template #trigger>
            <VBtn
              clz="warning"
              :left-icon="TrashOutline"
              :disabled="disbanding"
            >
              {{ t('competition.disband_team', '解散队伍') }}
            </VBtn>
          </template>
          <div>
            <div class="font-bold text-sm text-google-text-primary dark:text-[#FCFCFC]">{{ t('competition.disband_confirm_title', '确认解散队伍') }}</div>
            <div class="text-xs text-google-text-desc dark:text-google-text-darkDesc mt-1">
              {{ t('competition.disband_confirm_content', '解散后不可撤销，队伍成员将自动移出。') }}
            </div>
          </div>
        </n-popconfirm>
      </div>

      <div class="flex items-center gap-3">
        <template v-if="props.mode === 'create'">
          <VBtn
            clz="normal"
            @click="handleClose"
          >
            {{ t('common.cancel', '取消') }}
          </VBtn>
          <VBtn
            clz="primary"
            :loading="submitting"
            :disabled="submitting"
            @click="handleSubmit"
          >
            {{ t('competition.submit_signup', '提交报名') }}
          </VBtn>
        </template>

        <template v-else-if="!isEditing">
          <VBtn
            clz="normal"
            @click="handleClose"
          >
            {{ t('common.close', '关闭') }}
          </VBtn>
          <VBtn
            v-if="isCaptain"
            clz="primary"
            :left-icon="CreateOutline"
            @click="startEditing"
          >
            {{ t('competition.edit_team_btn', '编辑队伍信息') }}
          </VBtn>
        </template>

        <template v-else>
          <VBtn
            clz="normal"
            @click="cancelEditing"
          >
            {{ t('competition.cancel_edit_btn', '取消修改') }}
          </VBtn>
          <VBtn
            clz="primary"
            :loading="submitting"
            :disabled="submitting"
            @click="handleSubmit"
          >
            {{ t('competition.submit_update', '提交更新') }}
          </VBtn>
        </template>
      </div>
    </div>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import {
  NModal,
  NForm,
  NFormItem,
  NTable,
  NButton,
  NIcon,
  NTag,
  NPopconfirm,
  NSpin,
  NTooltip,
  useMessage,
  type FormInst,
  type FormRules
} from 'naive-ui'
import {
  AddOutline,
  TrashOutline,
  CreateOutline,
  InformationCircleOutline,
  PeopleOutline,
  CameraOutline,
  CloseOutline
} from '@vicons/ionicons5'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/store/modules/user'
import { getCurrentUserProfileApi } from '@/api/user'
import { uploadImageApi } from '@/api/system'
import {
  signupTeamApi,
  updateMyTeamApi,
  deleteMyTeamApi,
  reinviteMemberApi,
  cancelInvitationApi
} from '@/api/competition'
import { isValidChinaPhone, isValidIdCard } from '@/utils/validation'
import VAvatar from '@/components/sidebar/VAvatar.vue'
import { VInput, VSelection, VBtn } from '@/components/common'
import type {
  CompetitionDetailVO,
  MyTeamInfoVO,
  TeamSignupRequestDTO,
  TeamUpdateRequestDTO,
  TeamMemberSignupDTO,
  TeamLeadersSignupDTO,
  OrganizationType,
  MemberStatus
} from '@/types/competition'

defineOptions({
  name: 'TeamFormModal'
})

interface FormMemberRow extends TeamMemberSignupDTO {
  id?: number | string
  userId?: number | string
  status?: MemberStatus | string
}

interface FormLeaderRow extends TeamLeadersSignupDTO {
  id?: number | string
  userId?: number | string
}

const props = withDefaults(
  defineProps<{
    show: boolean
    competition: CompetitionDetailVO | null
    mode?: 'create' | 'view' | 'edit'
    initialTeam?: MyTeamInfoVO | null
  }>(),
  {
    mode: 'view',
    initialTeam: null
  }
)

const emit = defineEmits<{
  (e: 'update:show', val: boolean): void
  (e: 'success'): void
  (e: 'updated'): void
  (e: 'disbanded'): void
}>()

const { t } = useI18n()
const message = useMessage()
const userStore = useUserStore()

const formRef = ref<FormInst | null>(null)
const submitting = ref(false)
const disbanding = ref(false)
const isEditing = ref(false)
const showLeadersSection = ref(false)
const reinvitingUserId = ref<number | string | null>(null)
const uploadingAvatar = ref(false)
const avatarInputRef = ref<HTMLInputElement | null>(null)

const isCaptain = computed(() => {
  if (!props.initialTeam) return true
  return props.initialTeam.myRole === 'CAPTAIN' || Boolean(props.initialTeam.isCaptain)
})

const hasViewActions = computed(() => {
  return isCaptain.value && props.mode !== 'create'
})

const modalTitle = computed(() => {
  if (props.mode === 'create') {
    return t('competition.signup_modal_title', '创建战队并报名竞赛')
  }
  if (isEditing.value) {
    return t('competition.edit_team_modal_title', '编辑战队信息')
  }
  return t('competition.view_team_modal_title', '战队详情与报名信息')
})

const formData = reactive<{
  teamName: string
  avatarUrl: string
  description: string
  organization: string
  organizationType: OrganizationType
}>({
  teamName: '',
  avatarUrl: '',
  description: '',
  organization: '',
  organizationType: 'SCHOOL'
})

const formRules: FormRules = {
  teamName: [
    { required: true, message: '请输入战队名称', trigger: 'blur' },
    { min: 2, max: 32, message: '战队名称长度需在 2 到 32 字符之间', trigger: 'blur' }
  ],
  description: [
    { max: 200, message: '队伍简介不能超过 200 个字符', trigger: 'blur' }
  ],
  organization: [
    { max: 128, message: '队伍单位不能超过 128 个字符', trigger: 'blur' }
  ]
}

const organizationTypeOptions = [
  { label: '高等院校', value: 'SCHOOL' },
  { label: '企业单位', value: 'ENTERPRISE' },
  { label: '军工单位', value: 'MILITARY' },
  { label: '能源行业', value: 'ENERGY' },
  { label: '金融行业', value: 'FINANCE' },
  { label: '科研院所', value: 'RESEARCH' },
  { label: '其他组织', value: 'OTHER' }
]

const genderOptions = [
  { label: '男', value: 'MALE' },
  { label: '女', value: 'FEMALE' }
]

const members = ref<FormMemberRow[]>([])
const leaders = ref<FormLeaderRow[]>([])

const isMaxMembersReached = computed(() => {
  const max = props.competition?.maxTeamMembers ?? 4
  return members.value.length >= max
})

const isMaxLeadersReached = computed(() => {
  const max = props.competition?.maxLeaders ?? 1
  return leaders.value.length >= max
})

function formatDateTime(val?: string | null): string {
  if (!val) return '-'
  try {
    const d = new Date(val)
    if (isNaN(d.getTime())) return val
    const pad = (n: number) => (n < 10 ? '0' + n : n)
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
  } catch {
    return val
  }
}

const formattedRegTimeRange = computed(() => {
  if (!props.competition?.regStartTime && !props.competition?.regEndTime) {
    return '以比赛时间为准'
  }
  const start = formatDateTime(props.competition.regStartTime)
  const end = formatDateTime(props.competition.regEndTime)
  return `${start} ~ ${end}`
})

const formattedCompTimeRange = computed(() => {
  if (!props.competition?.startTime && !props.competition?.endTime) {
    return '待定'
  }
  const start = formatDateTime(props.competition.startTime)
  const end = formatDateTime(props.competition.endTime)
  return `${start} ~ ${end}`
})

const adminCustomRequirements = computed(() => {
  return (props.competition?.regRequirements || '').trim()
})

function formatMaskedIdCard(idCard?: string | null): string {
  if (!idCard) return '-'
  const trimmed = idCard.trim()
  if (trimmed.length < 10) return trimmed
  return `${trimmed.substring(0, 6)}********${trimmed.substring(trimmed.length - 4)}`
}

function getMemberStatusTag(row: FormMemberRow): { type: 'success' | 'warning' | 'default' | 'info'; label: string } {
  if (row.role === 'CAPTAIN' || row.status === 'INVITED_ACCEPTED') {
    return { type: 'success', label: t('competition.status_joined', '已加入') }
  }
  if (row.status === 'INVITED') {
    return { type: 'warning', label: t('competition.status_invited', '已发送邀请') }
  }
  if (row.status === 'NOT_PARTICIPATE') {
    return { type: 'default', label: t('competition.status_rejected', '已拒绝邀请') }
  }
  return { type: 'info', label: t('competition.status_pending', '待加入') }
}

function startEditing() {
  isEditing.value = true
}

function cancelEditing() {
  isEditing.value = false
  initModalData()
}

function triggerAvatarUpload() {
  if (!uploadingAvatar.value && avatarInputRef.value) {
    avatarInputRef.value.click()
  }
}

function handleRemoveAvatar() {
  formData.avatarUrl = ''
  if (avatarInputRef.value) {
    avatarInputRef.value.value = ''
  }
}

async function handleAvatarFileChange(e: Event) {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  const validTypes = ['image/jpeg', 'image/png', 'image/webp', 'image/gif', 'image/jpg']
  if (!validTypes.includes(file.type)) {
    message.error(t('competition.avatar_type_error', '仅支持 JPG、PNG、WebP 或 GIF 格式图片'))
    target.value = ''
    return
  }

  if (file.size > 5 * 1024 * 1024) {
    message.error(t('competition.avatar_size_error', '头像大小不能超过 5MB'))
    target.value = ''
    return
  }

  uploadingAvatar.value = true
  try {
    const res = await uploadImageApi(file, 'team-avatars')
    if (res.data?.filePath) {
      formData.avatarUrl = res.data.filePath
      message.success(t('competition.avatar_upload_success', '战队头像上传成功'))
    }
  } catch (err: any) {
    message.error(err.message || t('competition.avatar_upload_failed', '头像上传失败'))
  } finally {
    uploadingAvatar.value = false
    target.value = ''
  }
}

watch(
  () => props.show,
  async (val) => {
    if (val) {
      isEditing.value = props.mode === 'create' || props.mode === 'edit'
      await initModalData()
    }
  }
)

async function initModalData() {
  if (props.mode !== 'create' && props.initialTeam) {
    formData.teamName = props.initialTeam.teamName || ''
    formData.avatarUrl = props.initialTeam.avatarUrl || ''
    formData.description = props.initialTeam.description || ''
    formData.organization = props.initialTeam.organization || ''
    formData.organizationType = props.initialTeam.organizationType || 'SCHOOL'

    if (props.initialTeam.members && props.initialTeam.members.length > 0) {
      const allMembers = props.initialTeam.members

      const leaderMembers = allMembers.filter((m) => m.role === 'LEADER')
      leaders.value = leaderMembers.map((m) => ({
        id: m.id,
        userId: m.userId,
        realName: m.realName || m.username || '',
        gender: (m.gender === 'FEMALE' ? 'FEMALE' : 'MALE') as 'MALE' | 'FEMALE',
        phone: m.phone || '',
        email: m.email || '',
        idCard: m.idCard || '',
        employeeNumber: m.employeeNumber || '',
        department: m.department || '',
        position: m.position || ''
      }))
      showLeadersSection.value = leaders.value.length > 0

      const normalMembers = allMembers.filter((m) => m.role !== 'LEADER')
      const sorted = [...normalMembers].sort((a, b) => {
        if (a.role === 'CAPTAIN') return -1
        if (b.role === 'CAPTAIN') return 1
        return 0
      })

      members.value = sorted.map((m) => ({
        id: m.id,
        userId: m.userId,
        realName: m.realName || m.username || '',
        gender: (m.gender === 'FEMALE' ? 'FEMALE' : 'MALE') as 'MALE' | 'FEMALE',
        role: m.role || 'MEMBER',
        phone: m.phone || '',
        email: m.email || '',
        idCard: m.idCard || '',
        employeeNumber: m.employeeNumber || '',
        department: m.department || '',
        position: m.position || '',
        status: m.status
      }))
      return
    }
  }

  formData.teamName = ''
  formData.avatarUrl = ''
  formData.description = ''
  formData.organization = ''
  formData.organizationType = 'SCHOOL'
  showLeadersSection.value = false
  leaders.value = []

  let currentUserPhone = ''
  let currentUserEmail = userStore.user?.email || ''
  let currentUserName = userStore.user?.username || ''
  let currentUserGender: 'MALE' | 'FEMALE' = 'MALE'
  let currentUserOrg = ''

  try {
    const profileRes = await getCurrentUserProfileApi()
    if (profileRes?.data) {
      currentUserPhone = profileRes.data.phone || currentUserPhone
      currentUserEmail = profileRes.data.email || currentUserEmail
      currentUserName = profileRes.data.username || currentUserName
      if (profileRes.data.gender === 'FEMALE') {
        currentUserGender = 'FEMALE'
      }
    }
  } catch {
  }

  members.value = [
    {
      realName: currentUserName,
      gender: currentUserGender,
      role: 'CAPTAIN',
      phone: currentUserPhone,
      email: currentUserEmail,
      idCard: '',
      employeeNumber: '',
      department: currentUserOrg,
      position: '',
      status: 'INVITED_ACCEPTED'
    }
  ]
}

function addMemberRow() {
  if (isMaxMembersReached.value) {
    message.warning(`战队成员人数不能超过 ${props.competition?.maxTeamMembers ?? 4} 人`)
    return
  }
  members.value.push({
    realName: '',
    gender: 'MALE',
    role: 'MEMBER',
    phone: '',
    email: '',
    idCard: '',
    employeeNumber: '',
    department: '',
    position: '',
    status: 'INVITED'
  })
}

function removeMemberRow(idx: number) {
  if (idx === 0) return
  members.value.splice(idx, 1)
}

function toggleLeadersSection() {
  if (showLeadersSection.value) {
    const hasData = leaders.value.some((l) => Boolean(l.realName?.trim() || l.phone?.trim() || l.email?.trim() || l.idCard?.trim()))
    if (hasData) {
      message.warning('领队列表中已填写相关信息，若不需要请先清空或删除成员行')
      return
    }
    leaders.value = []
    showLeadersSection.value = false
  } else {
    showLeadersSection.value = true
    if (leaders.value.length === 0) {
      addLeaderRow()
    }
  }
}

function addLeaderRow() {
  const max = props.competition?.maxLeaders ?? 1
  if (leaders.value.length >= max) {
    message.warning(`领队人数不能超过 ${max} 人`)
    return
  }
  leaders.value.push({
    realName: '',
    gender: 'MALE',
    phone: '',
    email: '',
    idCard: '',
    employeeNumber: '',
    department: '',
    position: ''
  })
}

function removeLeaderRow(idx: number) {
  leaders.value.splice(idx, 1)
}

function handleClose() {
  emit('update:show', false)
}

async function handleDisbandTeam() {
  if (!props.competition || disbanding.value) return
  disbanding.value = true
  try {
    await deleteMyTeamApi(props.competition.id)
    message.success('战队已成功解散')
    emit('update:show', false)
    emit('disbanded')
  } catch {
  } finally {
    disbanding.value = false
  }
}

async function handleReinvite(row: FormMemberRow) {
  if (!props.competition || !props.initialTeam || !row.userId) {
    message.warning('无法获取该成员的用户标识')
    return
  }
  reinvitingUserId.value = row.userId
  try {
    await reinviteMemberApi(props.competition.id, props.initialTeam.teamId, row.userId)
    row.status = 'INVITED'
    message.success(t('competition.reinvite_success', '重新发送邀请成功'))
    emit('updated')
  } catch {
  } finally {
    reinvitingUserId.value = null
  }
}

async function handleCancelMember(row: FormMemberRow) {
  if (!props.competition || !props.initialTeam || !row.userId) {
    message.warning('无法获取该成员的用户标识')
    return
  }
  try {
    await cancelInvitationApi(props.competition.id, props.initialTeam.teamId, row.userId)
    message.success('已移除该成员')
    const idx = members.value.findIndex((m) => m.userId === row.userId)
    if (idx !== -1) {
      members.value.splice(idx, 1)
    }
    emit('updated')
  } catch {
  }
}

function validateMembers(): boolean {
  if (!props.competition) return false

  const minMembers = props.competition.minTeamMembers ?? 1
  const maxMembers = props.competition.maxTeamMembers ?? 4
  const maxLeaders = props.competition.maxLeaders ?? 1

  if (members.value.length < minMembers || members.value.length > maxMembers) {
    message.error(`战队成员人数需在 ${minMembers} 至 ${maxMembers} 人之间`)
    return false
  }

  if (members.value[0]?.role !== 'CAPTAIN') {
    message.error('战队必须且只能有 1 名队长')
    return false
  }

  if (leaders.value.length > maxLeaders) {
    message.error(`领队人数不能超过 ${maxLeaders} 人`)
    return false
  }

  const isEnterprise = formData.organizationType !== 'SCHOOL'
  const studentOrEmpNumberSet = new Set<string>()
  const phoneSet = new Set<string>()
  const emailSet = new Set<string>()
  const idCardSet = new Set<string>()
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

  for (let i = 0; i < members.value.length; i++) {
    const row = members.value[i]
    const rowNum = i + 1

    const realName = (row.realName || '').trim()
    if (!realName) {
      message.error(`请填写第 ${rowNum} 行成员的姓名`)
      return false
    }

    if (!row.gender) {
      message.error(`请选择第 ${rowNum} 行成员的性别`)
      return false
    }

    const emp = (row.employeeNumber || '').trim()
    if (!emp) {
      message.error(isEnterprise ? `请填写第 ${rowNum} 行成员的工号` : `请填写第 ${rowNum} 行成员的学号`)
      return false
    }
    if (studentOrEmpNumberSet.has(emp)) {
      message.error(isEnterprise ? `战队成员中存在重复的工号: ${emp}` : `战队成员中存在重复的学号: ${emp}`)
      return false
    }
    studentOrEmpNumberSet.add(emp)

    const phone = (row.phone || '').trim()
    if (!phone) {
      message.error(`请填写第 ${rowNum} 行成员的手机号`)
      return false
    }
    if (!isValidChinaPhone(phone)) {
      message.error(`第 ${rowNum} 行成员手机号格式不正确`)
      return false
    }
    if (phoneSet.has(phone)) {
      message.error(`战队成员中存在重复的手机号: ${phone}`)
      return false
    }
    phoneSet.add(phone)

    const email = (row.email || '').trim().toLowerCase()
    if (!email) {
      message.error(`请填写第 ${rowNum} 行成员的邮箱`)
      return false
    }
    if (!emailRegex.test(email)) {
      message.error(`第 ${rowNum} 行成员邮箱格式不正确`)
      return false
    }
    if (emailSet.has(email)) {
      message.error(`战队成员中存在重复的邮箱: ${email}`)
      return false
    }
    emailSet.add(email)

    const idCard = (row.idCard || '').trim()
    if (!idCard) {
      message.error(`请填写第 ${rowNum} 行成员的身份证号`)
      return false
    }
    if (!isValidIdCard(idCard)) {
      message.error(`第 ${rowNum} 行成员身份证号格式不正确`)
      return false
    }
    const idCardUpper = idCard.toUpperCase()
    if (idCardSet.has(idCardUpper)) {
      message.error(`战队成员中存在重复的身份证号: ${idCard}`)
      return false
    }
    idCardSet.add(idCardUpper)

    const dept = (row.department || '').trim()
    if (!dept) {
      message.error(isEnterprise ? `请填写第 ${rowNum} 行成员的部门` : `请填写第 ${rowNum} 行成员的学院`)
      return false
    }

    const pos = (row.position || '').trim()
    if (!pos) {
      message.error(isEnterprise ? `请填写第 ${rowNum} 行成员的职位` : `请填写第 ${rowNum} 行成员的班级`)
      return false
    }
  }

  const captainEmail = (members.value[0]?.email || '').trim().toLowerCase()

  for (let j = 0; j < leaders.value.length; j++) {
    const l = leaders.value[j]
    const rowNum = j + 1

    const realName = (l.realName || '').trim()
    if (!realName) {
      message.error(`请填写第 ${rowNum} 行领队的姓名`)
      return false
    }

    const phone = (l.phone || '').trim()
    if (!phone) {
      message.error(`请填写第 ${rowNum} 行领队的手机号`)
      return false
    }
    if (!isValidChinaPhone(phone)) {
      message.error(`第 ${rowNum} 行领队手机号格式不正确`)
      return false
    }
    if (phoneSet.has(phone)) {
      message.error(`领队手机号与战队成员重复: ${phone}`)
      return false
    }
    phoneSet.add(phone)

    const email = (l.email || '').trim().toLowerCase()
    if (email) {
      if (!emailRegex.test(email)) {
        message.error(`第 ${rowNum} 行领队邮箱格式不正确`)
        return false
      }
      if (email === captainEmail) {
        message.error('不能填写队长自身的邮箱作为指导老师/领队')
        return false
      }
      if (emailSet.has(email)) {
        message.error(`领队邮箱与战队成员重复: ${email}`)
        return false
      }
      emailSet.add(email)
    }

    const idCard = (l.idCard || '').trim()
    if (idCard) {
      if (!isValidIdCard(idCard)) {
        message.error(`第 ${rowNum} 行领队身份证格式不正确`)
        return false
      }
      const idCardUpper = idCard.toUpperCase()
      if (idCardSet.has(idCardUpper)) {
        message.error(`领队身份证号与战队成员重复: ${idCard}`)
        return false
      }
      idCardSet.add(idCardUpper)
    }
  }

  return true
}

async function handleSubmit() {
  if (!props.competition) return

  formRef.value?.validate(async (errors) => {
    if (errors) return

    if (!validateMembers()) return

    submitting.value = true
    try {
      const sanitizedMembers: TeamMemberSignupDTO[] = members.value.map((m, idx) => ({
        realName: (m.realName || '').trim(),
        gender: m.gender,
        role: idx === 0 ? 'CAPTAIN' : 'MEMBER',
        phone: (m.phone || '').trim(),
        email: (m.email || '').trim().toLowerCase(),
        idCard: (m.idCard || '').trim().toUpperCase(),
        employeeNumber: (m.employeeNumber || '').trim() || undefined,
        department: (m.department || '').trim() || undefined,
        position: (m.position || '').trim() || undefined
      }))

      const sanitizedLeaders: TeamLeadersSignupDTO[] = leaders.value.map((l) => ({
        realName: (l.realName || '').trim(),
        phone: (l.phone || '').trim(),
        gender: l.gender,
        email: (l.email || '').trim().toLowerCase() || undefined,
        idCard: (l.idCard || '').trim().toUpperCase() || undefined,
        employeeNumber: (l.employeeNumber || '').trim() || undefined,
        department: (l.department || '').trim() || undefined,
        position: (l.position || '').trim() || undefined
      }))

      if (props.mode !== 'create') {
        const updatePayload: TeamUpdateRequestDTO = {
          teamName: formData.teamName.trim(),
          avatarUrl: formData.avatarUrl ? formData.avatarUrl.trim() : '',
          description: formData.description?.trim() || undefined,
          organization: formData.organization?.trim() || undefined,
          organizationType: formData.organizationType,
          members: sanitizedMembers,
          leaders: sanitizedLeaders
        }
        await updateMyTeamApi(props.competition!.id, updatePayload)
        message.success('战队信息已更新')
        isEditing.value = false
        emit('updated')
        emit('success')
        emit('update:show', false)
      } else {
        const signupPayload: TeamSignupRequestDTO = {
          teamName: formData.teamName.trim(),
          avatarUrl: formData.avatarUrl ? formData.avatarUrl.trim() : undefined,
          description: formData.description?.trim() || undefined,
          organization: formData.organization?.trim() || undefined,
          organizationType: formData.organizationType,
          members: sanitizedMembers,
          leaders: sanitizedLeaders
        }
        await signupTeamApi(props.competition!.id, signupPayload)
        message.success(t('competition.signup_success', '报名成功'))
        emit('success')
        emit('update:show', false)
      }
    } catch {
    } finally {
      submitting.value = false
    }
  })
}
</script>

<style scoped lang="scss">
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
