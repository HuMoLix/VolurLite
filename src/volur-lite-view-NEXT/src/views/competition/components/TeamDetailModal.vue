<template>
  <n-modal
    :show="show"
    preset="card"
    class="team-detail-modal w-[1040px] max-w-[96vw] rounded-2xl border border-google-border-light dark:border-google-border-dark bg-white dark:bg-google-surface-darkCard"
    :title="t('competition.view_team_modal_title', '战队详情与报名信息')"
    :bordered="false"
    @update:show="handleClose"
  >
    <div v-if="team" class="space-y-5">
      <div class="p-4 rounded-xl bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light/80 dark:border-google-border-dark/80 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div class="flex items-center gap-4 min-w-0">
          <div class="w-14 h-14 rounded-2xl overflow-hidden shrink-0 bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark flex items-center justify-center">
            <img
              v-if="team.avatarUrl"
              :src="team.avatarUrl"
              :alt="team.teamName"
              class="w-full h-full object-cover"
            />
            <n-icon v-else :size="28" :component="PeopleOutline" class="text-google-blue dark:text-google-blue-dark" />
          </div>

          <div class="space-y-1 min-w-0">
            <div class="flex items-center gap-2.5 flex-wrap">
              <h3 class="m-0 text-base font-bold text-google-text-primary dark:text-[#FCFCFC] truncate">
                {{ team.teamName }}
              </h3>
              <span
                class="px-2.5 py-0.5 rounded-full text-xs font-semibold"
                :class="getTeamStatusClass(team.status)"
              >
                {{ getTeamStatusText(team.status) }}
              </span>
              <span
                v-if="team.myRole"
                class="px-2 py-0.5 rounded-full text-xs font-medium bg-blue-50 text-google-blue dark:bg-blue-950/60 dark:text-google-blue-dark"
              >
                {{ getRoleLabel(team.myRole) }}
              </span>
            </div>

            <p v-if="team.description" class="m-0 text-xs text-google-text-desc dark:text-google-text-darkDesc line-clamp-2">
              {{ team.description }}
            </p>

            <div class="flex items-center gap-3 text-xs text-google-text-muted dark:text-google-text-darkMuted flex-wrap">
              <span v-if="team.organization">
                所属单位: <span class="text-google-text-desc dark:text-google-text-darkDesc">{{ team.organization }}</span>
              </span>
              <span v-if="team.organizationType">
                单位类型: <span class="text-google-text-desc dark:text-google-text-darkDesc">{{ getOrgTypeLabel(team.organizationType) }}</span>
              </span>
            </div>
          </div>
        </div>

        <div v-if="team.isCaptain" class="shrink-0 self-end sm:self-center">
          <n-popconfirm
            positive-text="确认解散"
            negative-text="取消"
            @positive-click="handleDisbandTeam"
          >
            <template #trigger>
              <button
                type="button"
                class="h-8 px-3.5 rounded-full border border-red-300 dark:border-red-900/60 text-google-red dark:text-google-red-dark bg-white dark:bg-google-surface-darkCard hover:bg-red-50 dark:hover:bg-red-950/30 text-xs font-semibold transition-colors cursor-pointer flex items-center gap-1.5"
                :disabled="disbanding"
              >
                <n-icon :size="14" :component="TrashOutline" />
                <span>解散战队</span>
              </button>
            </template>
            <span>确定解散该战队吗？解散后全部报名信息将失效且不可撤回。</span>
          </n-popconfirm>
        </div>
      </div>

      <div class="space-y-3">
        <div class="flex items-center justify-between pb-2 border-b border-google-border-light dark:border-google-border-dark">
          <h4 class="m-0 text-sm font-semibold text-google-text-primary dark:text-[#FCFCFC] flex items-center gap-2">
            <n-icon :size="16" :component="PersonOutline" class="text-google-blue dark:text-google-blue-dark" />
            <span>{{ t('competition.member_table_title', '参赛成员名册') }} ({{ normalMembers.length }})</span>
          </h4>
        </div>

        <div class="overflow-x-auto border border-google-border-light dark:border-google-border-dark rounded-xl">
          <n-table :bordered="true" :single-line="false" size="small" class="min-w-[1100px] text-xs">
            <thead>
              <tr class="bg-google-surface-light dark:bg-google-surface-dark text-google-text-desc dark:text-google-text-darkDesc whitespace-nowrap">
                <th class="w-24 p-2.5 font-semibold">{{ t('competition.col_name', '姓名') }}</th>
                <th class="w-20 p-2.5 font-semibold">{{ t('competition.col_gender', '性别') }}</th>
                <th class="w-20 p-2.5 font-semibold text-center">{{ t('competition.col_role', '角色') }}</th>
                <th class="w-32 p-2.5 font-semibold">
                  {{ team.organizationType !== 'SCHOOL' ? t('competition.col_employee_number', '工号') : t('competition.col_student_number', '学号') }}
                </th>
                <th class="w-32 p-2.5 font-semibold">{{ t('competition.col_phone', '手机号') }}</th>
                <th class="w-40 p-2.5 font-semibold">{{ t('competition.col_email', '邮箱') }}</th>
                <th class="w-40 p-2.5 font-semibold">{{ t('competition.col_id_card', '身份证') }}</th>
                <th class="w-32 p-2.5 font-semibold">
                  {{ team.organizationType !== 'SCHOOL' ? t('competition.col_department_real', '部门') : t('competition.col_college', '学院') }}
                </th>
                <th class="w-28 p-2.5 font-semibold">
                  {{ team.organizationType !== 'SCHOOL' ? t('competition.col_position', '职位') : t('competition.col_class_name', '班级') }}
                </th>
                <th class="w-24 p-2.5 font-semibold text-center">{{ t('competition.col_status', '加入状态') }}</th>
                <th v-if="team.isCaptain" class="w-24 p-2.5 font-semibold text-center">{{ t('competition.col_action', '操作') }}</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-google-border-light/60 dark:divide-google-border-dark/60 bg-white dark:bg-google-surface-darkCard">
              <tr
                v-for="member in normalMembers"
                :key="member.id"
                :class="member.role === 'CAPTAIN' ? 'bg-google-surface-light/40 dark:bg-google-surface-dark/30' : ''"
              >
                <td class="p-2.5 font-medium text-google-text-primary dark:text-[#FCFCFC]">
                  {{ member.realName || member.username }}
                </td>
                <td class="p-2.5 text-google-text-desc dark:text-google-text-darkDesc">
                  {{ getGenderLabel(member.gender) }}
                </td>
                <td class="p-2.5 text-center">
                  <span
                    class="px-2 py-0.5 rounded-full text-[11px] font-medium"
                    :class="member.role === 'CAPTAIN' ? 'bg-amber-100 text-amber-800 dark:bg-amber-950/60 dark:text-amber-300' : 'bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-300'"
                  >
                    {{ getRoleLabel(member.role) }}
                  </span>
                </td>
                <td class="p-2.5 text-google-text-desc dark:text-google-text-darkDesc font-mono">
                  {{ member.employeeNumber || '-' }}
                </td>
                <td class="p-2.5 text-google-text-desc dark:text-google-text-darkDesc font-mono">
                  {{ member.phone || '-' }}
                </td>
                <td class="p-2.5 text-google-text-desc dark:text-google-text-darkDesc font-mono">
                  {{ member.email || '-' }}
                </td>
                <td class="p-2.5 text-google-text-desc dark:text-google-text-darkDesc font-mono">
                  {{ formatMaskedIdCard(member.idCard) }}
                </td>
                <td class="p-2.5 text-google-text-desc dark:text-google-text-darkDesc">
                  {{ member.department || '-' }}
                </td>
                <td class="p-2.5 text-google-text-desc dark:text-google-text-darkDesc">
                  {{ member.position || '-' }}
                </td>
                <td class="p-2.5 text-center">
                  <span
                    class="px-2 py-0.5 rounded-full text-[11px] font-medium"
                    :class="member.status === 'INVITED_ACCEPTED' ? 'text-google-green dark:text-google-green-dark bg-green-50 dark:bg-green-950/40' : member.status === 'INVITED' ? 'text-google-yellow dark:text-google-yellow-dark bg-yellow-50 dark:bg-yellow-950/40' : 'text-google-text-muted dark:text-google-text-darkMuted bg-gray-50 dark:bg-gray-900/40'"
                  >
                    {{ getMemberStatusLabel(member.status) }}
                  </span>
                </td>
                <td v-if="team.isCaptain" class="p-2.5 text-center">
                  <div v-if="member.role !== 'CAPTAIN'" class="flex items-center justify-center gap-1.5">
                    <button
                      v-if="member.status === 'INVITED' || member.status === 'NOT_PARTICIPATE'"
                      type="button"
                      class="px-2 py-0.5 rounded text-[11px] text-google-blue dark:text-google-blue-dark hover:underline bg-transparent border-none cursor-pointer"
                      @click="handleReinvite(member.userId)"
                    >
                      重新邀请
                    </button>
                    <n-popconfirm
                      positive-text="确认移除"
                      negative-text="取消"
                      @positive-click="handleCancelMember(member.userId)"
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
      </div>

      <div v-if="leaderMembers.length > 0" class="space-y-3 pt-2">
        <div class="flex items-center justify-between pb-2 border-b border-dashed border-google-border-light dark:border-google-border-dark">
          <h4 class="m-0 text-sm font-semibold text-google-text-primary dark:text-[#FCFCFC] flex items-center gap-2">
            <n-icon :size="16" :component="PersonOutline" class="text-google-green dark:text-google-green-dark" />
            <span>{{ t('competition.leader_table_title', '领队/指导老师') }} ({{ leaderMembers.length }})</span>
          </h4>
        </div>

        <div class="overflow-x-auto border border-google-border-light dark:border-google-border-dark rounded-xl">
          <n-table :bordered="true" :single-line="false" size="small" class="min-w-[1100px] text-xs">
            <thead>
              <tr class="bg-google-surface-light dark:bg-google-surface-dark text-google-text-desc dark:text-google-text-darkDesc whitespace-nowrap">
                <th class="w-24 p-2.5 font-semibold">{{ t('competition.col_name', '姓名') }}</th>
                <th class="w-20 p-2.5 font-semibold">{{ t('competition.col_gender', '性别') }}</th>
                <th class="w-20 p-2.5 font-semibold text-center">{{ t('competition.col_role', '角色') }}</th>
                <th class="w-32 p-2.5 font-semibold">
                  {{ team.organizationType !== 'SCHOOL' ? t('competition.col_employee_number', '工号') : t('competition.col_student_number', '学号') }}
                </th>
                <th class="w-32 p-2.5 font-semibold">{{ t('competition.col_phone', '手机号') }}</th>
                <th class="w-40 p-2.5 font-semibold">{{ t('competition.col_email', '邮箱') }}</th>
                <th class="w-40 p-2.5 font-semibold">{{ t('competition.col_id_card', '身份证') }}</th>
                <th class="w-32 p-2.5 font-semibold">
                  {{ team.organizationType !== 'SCHOOL' ? t('competition.col_department_real', '部门') : t('competition.col_college', '学院') }}
                </th>
                <th class="w-28 p-2.5 font-semibold">
                  {{ team.organizationType !== 'SCHOOL' ? t('competition.col_position', '职位') : t('competition.col_class_name', '班级') }}
                </th>
                <th v-if="team.isCaptain" class="w-24 p-2.5 font-semibold text-center">{{ t('competition.col_action', '操作') }}</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-google-border-light/60 dark:divide-google-border-dark/60 bg-white dark:bg-google-surface-darkCard">
              <tr v-for="leader in leaderMembers" :key="leader.id">
                <td class="p-2.5 font-medium text-google-text-primary dark:text-[#FCFCFC]">
                  {{ leader.realName || leader.username }}
                </td>
                <td class="p-2.5 text-google-text-desc dark:text-google-text-darkDesc">
                  {{ getGenderLabel(leader.gender) }}
                </td>
                <td class="p-2.5 text-center">
                  <span class="px-2 py-0.5 rounded-full text-[11px] font-medium bg-emerald-100 text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300">
                    {{ t('competition.role_leader', '指导老师') }}
                  </span>
                </td>
                <td class="p-2.5 text-google-text-desc dark:text-google-text-darkDesc font-mono">
                  {{ leader.employeeNumber || '-' }}
                </td>
                <td class="p-2.5 text-google-text-desc dark:text-google-text-darkDesc font-mono">
                  {{ leader.phone || '-' }}
                </td>
                <td class="p-2.5 text-google-text-desc dark:text-google-text-darkDesc font-mono">
                  {{ leader.email || '-' }}
                </td>
                <td class="p-2.5 text-google-text-desc dark:text-google-text-darkDesc font-mono">
                  {{ formatMaskedIdCard(leader.idCard) }}
                </td>
                <td class="p-2.5 text-google-text-desc dark:text-google-text-darkDesc">
                  {{ leader.department || '-' }}
                </td>
                <td class="p-2.5 text-google-text-desc dark:text-google-text-darkDesc">
                  {{ leader.position || '-' }}
                </td>
                <td v-if="team.isCaptain" class="p-2.5 text-center">
                  <n-popconfirm
                    positive-text="确认移除"
                    negative-text="取消"
                    @positive-click="handleCancelMember(leader.userId)"
                  >
                    <template #trigger>
                      <button
                        type="button"
                        class="px-2 py-0.5 rounded text-[11px] text-google-red dark:text-google-red-dark hover:underline bg-transparent border-none cursor-pointer"
                      >
                        移除
                      </button>
                    </template>
                    <span>确定将该领队从队伍中移除吗？</span>
                  </n-popconfirm>
                </td>
              </tr>
            </tbody>
          </n-table>
        </div>
      </div>
    </div>

    <div class="flex items-center justify-end gap-3 pt-4 border-t border-google-border-light dark:border-google-border-dark mt-5">
      <button
        type="button"
        class="h-9 px-5 rounded-full border border-google-border-light dark:border-google-border-dark text-google-text-desc dark:text-google-text-darkDesc bg-transparent hover:bg-google-surface-light dark:hover:bg-google-surface-dark text-xs font-semibold transition-colors cursor-pointer"
        @click="handleClose"
      >
        {{ t('common.close', '关闭') }}
      </button>
    </div>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { NModal, NTable, NIcon, NPopconfirm, useMessage } from 'naive-ui'
import { PeopleOutline, PersonOutline, TrashOutline } from '@vicons/ionicons5'
import { useI18n } from 'vue-i18n'
import type { MyTeamInfoVO, MemberRole, MemberStatus, TeamStatus, OrganizationType } from '@/types/competition'
import { deleteMyTeamApi, cancelInvitationApi, reinviteMemberApi } from '@/api/competition'

defineOptions({
  name: 'TeamDetailModal'
})

const props = defineProps<{
  show: boolean
  competitionId: number | string
  team: MyTeamInfoVO | null
}>()

const emit = defineEmits<{
  (e: 'update:show', val: boolean): void
  (e: 'disbanded'): void
  (e: 'updated'): void
}>()

const { t } = useI18n()
const message = useMessage()

const disbanding = ref(false)

const normalMembers = computed(() => {
  if (!props.team?.members) return []
  return props.team.members.filter((m) => m.role !== 'LEADER')
})

const leaderMembers = computed(() => {
  if (!props.team?.members) return []
  return props.team.members.filter((m) => m.role === 'LEADER')
})

function handleClose() {
  emit('update:show', false)
}

function getRoleLabel(role?: MemberRole | null): string {
  if (role === 'CAPTAIN') return '队长'
  if (role === 'LEADER') return '领队/老师'
  return '参赛队员'
}

function getGenderLabel(gender?: string | null): string {
  if (gender === 'FEMALE') return '女'
  if (gender === 'MALE') return '男'
  return '-'
}

function getOrgTypeLabel(type?: OrganizationType | null): string {
  if (type === 'SCHOOL') return '高等院校'
  if (type === 'ENTERPRISE') return '企业单位'
  if (type === 'MILITARY') return '军工单位'
  if (type === 'ENERGY') return '能源行业'
  if (type === 'FINANCE') return '金融行业'
  if (type === 'RESEARCH') return '科研院所'
  return '其他组织'
}

function formatMaskedIdCard(idCard?: string | null): string {
  if (!idCard) return '-'
  const trimmed = idCard.trim()
  if (trimmed.length < 10) return trimmed
  return `${trimmed.substring(0, 6)}********${trimmed.substring(trimmed.length - 4)}`
}

function getMemberStatusLabel(status?: MemberStatus | null): string {
  if (status === 'INVITED_ACCEPTED') return '已加入'
  if (status === 'INVITED') return '待确认'
  if (status === 'NOT_PARTICIPATE') return '已拒绝'
  return '已加入'
}

function getTeamStatusText(status?: TeamStatus): string {
  if (status === 'AUDIT_PASSED') return '审核通过'
  if (status === 'PENDING_AUDIT' || status === 'AUDIT_DRAFT') return '审核中'
  if (status === 'AUDIT_REJECTED') return '审核被拒'
  if (status === 'DISBANDED') return '已解散'
  return '-'
}

function getTeamStatusClass(status?: TeamStatus): string {
  if (status === 'AUDIT_PASSED') {
    return 'bg-emerald-100 text-google-green dark:bg-emerald-950/60 dark:text-google-green-dark'
  }
  if (status === 'PENDING_AUDIT' || status === 'AUDIT_DRAFT') {
    return 'bg-amber-100 text-amber-800 dark:bg-amber-950/60 dark:text-amber-300'
  }
  if (status === 'AUDIT_REJECTED') {
    return 'bg-red-100 text-google-red dark:bg-red-950/60 dark:text-google-red-dark'
  }
  return 'bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-300'
}

async function handleDisbandTeam() {
  if (disbanding.value || !props.team) return
  disbanding.value = true
  try {
    await deleteMyTeamApi(props.competitionId)
    message.success('战队已成功解散')
    emit('disbanded')
    handleClose()
  } catch {
  } finally {
    disbanding.value = false
  }
}

async function handleReinvite(userId: number | string) {
  if (!props.team) return
  try {
    await reinviteMemberApi(props.competitionId, props.team.teamId, userId)
    message.success('重新发送邀请成功')
    emit('updated')
  } catch {
  }
}

async function handleCancelMember(userId: number | string) {
  if (!props.team) return
  try {
    await cancelInvitationApi(props.competitionId, props.team.teamId, userId)
    message.success('已移除该成员')
    emit('updated')
  } catch {
  }
}
</script>
