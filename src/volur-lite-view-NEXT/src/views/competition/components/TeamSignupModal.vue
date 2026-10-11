<template>
  <n-modal
    :show="show"
    preset="card"
    class="team-signup-modal w-[1040px] max-w-[96vw] rounded-2xl border border-google-border-light dark:border-google-border-dark bg-white dark:bg-google-surface-darkCard"
    :title="t('competition.signup_modal_title', '创建战队并报名竞赛')"
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
            <div class="w-12 h-12 rounded-2xl overflow-hidden shrink-0 bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light dark:border-google-border-dark flex items-center justify-center text-google-blue dark:text-google-blue-dark">
              <n-icon :size="24" :component="PeopleOutline" />
            </div>
            <n-form-item
              :label="t('competition.team_name_label', '战队名称')"
              path="teamName"
              class="flex-1 min-w-0 m-0"
            >
              <n-input
                v-model:value="formData.teamName"
                :placeholder="t('competition.team_name_placeholder', '请输入战队名称 (2-32 字符)')"
                maxlength="32"
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
              <n-input
                v-model:value="formData.organization"
                :placeholder="t('competition.team_organization_placeholder', '请输入队伍所属单位')"
                maxlength="128"
                clearable
                class="w-full"
              />
            </n-form-item>

            <n-form-item
              :label="t('competition.team_org_type_label', '单位类型')"
              path="organizationType"
              class="m-0"
            >
              <n-select
                v-model:value="formData.organizationType"
                :options="organizationTypeOptions"
                class="w-full"
              />
            </n-form-item>
          </div>

          <n-form-item
            :label="t('competition.team_desc_label', '队伍简介')"
            path="description"
            class="m-0 w-full"
          >
            <n-input
              v-model:value="formData.description"
              type="textarea"
              :placeholder="t('competition.team_desc_placeholder', '请输入队伍简介或口号')"
              maxlength="200"
              show-count
              :autosize="{ minRows: 2, maxRows: 2 }"
              class="w-full"
            />
          </n-form-item>
        </n-form>
      </div>

      <div class="meta-divider hidden lg:block w-px bg-google-border-light dark:bg-google-border-dark" />

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

      <n-button
        size="small"
        secondary
        :disabled="isMaxMembersReached"
        @click="addMemberRow"
      >
        <template #icon>
          <n-icon :component="AddOutline" />
        </template>
        {{ t('competition.add_member_row', '添加成员行') }}
      </n-button>
    </div>

    <div class="overflow-x-auto mb-4 border border-google-border-light dark:border-google-border-dark rounded-xl">
      <n-table :bordered="true" :single-line="false" size="small" class="min-w-[1240px] text-xs">
        <thead>
          <tr class="bg-google-surface-light dark:bg-google-surface-dark text-google-text-desc dark:text-google-text-darkDesc whitespace-nowrap">
            <th class="w-24 p-2.5 font-semibold">{{ t('competition.col_name', '姓名') }} <span class="text-google-red dark:text-google-red-dark">*</span></th>
            <th class="w-20 p-2.5 font-semibold">{{ t('competition.col_gender', '性别') }} <span class="text-google-red dark:text-google-red-dark">*</span></th>
            <th class="w-20 p-2.5 font-semibold text-center">{{ t('competition.col_role', '角色') }}</th>
            <th class="w-32 p-2.5 font-semibold">
              {{ formData.organizationType !== 'SCHOOL' ? t('competition.col_employee_number', '工号') : t('competition.col_student_number', '学号') }}
              <span class="text-google-red dark:text-google-red-dark">*</span>
            </th>
            <th class="w-32 p-2.5 font-semibold">{{ t('competition.col_phone', '手机号') }} <span class="text-google-red dark:text-google-red-dark">*</span></th>
            <th class="w-40 p-2.5 font-semibold">{{ t('competition.col_email', '邮箱') }} <span class="text-google-red dark:text-google-red-dark">*</span></th>
            <th class="w-44 p-2.5 font-semibold">{{ t('competition.col_id_card', '身份证') }} <span class="text-google-red dark:text-google-red-dark">*</span></th>
            <th class="w-32 p-2.5 font-semibold">
              {{ formData.organizationType !== 'SCHOOL' ? t('competition.col_department_real', '部门') : t('competition.col_college', '学院') }}
              <span class="text-google-red dark:text-google-red-dark">*</span>
            </th>
            <th class="w-28 p-2.5 font-semibold">
              {{ formData.organizationType !== 'SCHOOL' ? t('competition.col_position', '职位') : t('competition.col_class_name', '班级') }}
              <span class="text-google-red dark:text-google-red-dark">*</span>
            </th>
            <th class="w-24 p-2.5 font-semibold text-center">{{ t('competition.col_status', '加入状态') }}</th>
            <th class="w-16 p-2.5 font-semibold text-center">{{ t('competition.col_action', '操作') }}</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-google-border-light/60 dark:divide-google-border-dark/60 bg-white dark:bg-google-surface-darkCard">
          <tr
            v-for="(row, idx) in members"
            :key="idx"
            :class="idx === 0 ? 'bg-google-surface-light/40 dark:bg-google-surface-dark/30' : ''"
          >
            <td class="p-2">
              <n-input
                v-model:value="row.realName"
                size="small"
                :placeholder="t('competition.col_name', '姓名')"
                maxlength="64"
              />
            </td>
            <td class="p-2">
              <n-select
                v-model:value="row.gender"
                size="small"
                :options="genderOptions"
              />
            </td>
            <td class="p-2 text-center">
              <span :class="idx === 0 ? 'font-bold text-google-blue dark:text-google-blue-dark' : 'text-google-text-desc dark:text-google-text-darkDesc'">
                {{ idx === 0 ? t('competition.role_captain', '队长') : t('competition.role_member', '队员') }}
              </span>
            </td>
            <td class="p-2">
              <n-input
                v-model:value="row.employeeNumber"
                size="small"
                :placeholder="formData.organizationType !== 'SCHOOL' ? t('competition.col_employee_number', '工号') : t('competition.col_student_number', '学号')"
                maxlength="64"
              />
            </td>
            <td class="p-2">
              <n-input
                v-model:value="row.phone"
                size="small"
                :placeholder="t('competition.col_phone', '手机号')"
                maxlength="16"
              />
            </td>
            <td class="p-2">
              <n-input
                v-model:value="row.email"
                size="small"
                :placeholder="t('competition.col_email', '邮箱')"
                maxlength="128"
              />
            </td>
            <td class="p-2">
              <n-input
                v-model:value="row.idCard"
                size="small"
                :placeholder="t('competition.col_id_card', '身份证')"
                maxlength="32"
              />
            </td>
            <td class="p-2">
              <n-input
                v-model:value="row.department"
                size="small"
                :placeholder="formData.organizationType !== 'SCHOOL' ? t('competition.col_department_real', '部门') : t('competition.col_college', '学院')"
                maxlength="128"
              />
            </td>
            <td class="p-2">
              <n-input
                v-model:value="row.position"
                size="small"
                :placeholder="formData.organizationType !== 'SCHOOL' ? t('competition.col_position', '职位') : t('competition.col_class_name', '班级')"
                maxlength="64"
              />
            </td>
            <td class="p-2 text-center">
              <n-tag size="small" :type="idx === 0 ? 'success' : 'warning'" round>
                {{ idx === 0 ? t('competition.status_joined', '已加入') : t('competition.status_pending', '待确认') }}
              </n-tag>
            </td>
            <td class="p-2 text-center">
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
            </td>
          </tr>
        </tbody>
      </n-table>
    </div>

    <div v-if="(competition?.maxLeaders ?? 1) > 0" class="my-3 text-left">
      <span
        class="inline-block text-xs font-semibold text-google-blue dark:text-google-blue-dark cursor-pointer select-none hover:underline"
        @click="toggleLeadersSection"
      >
        {{ showLeadersSection ? t('competition.hide_leader_link', '收起领队/指导老师') : t('competition.add_leader_link', '添加领队/指导老师') }}
      </span>
    </div>

    <div v-if="showLeadersSection" class="mt-2 mb-4">
      <div class="flex items-center justify-between mb-2 pb-2 border-b border-dashed border-google-border-light dark:border-google-border-dark">
        <div class="flex items-baseline gap-3">
          <h4 class="m-0 text-sm font-semibold text-google-text-primary dark:text-[#FCFCFC]">{{ t('competition.leader_table_title', '领队/指导老师') }}</h4>
          <span class="text-xs text-google-text-desc dark:text-google-text-darkDesc">
            当前领队: <strong class="text-google-text-primary dark:text-[#FCFCFC]">{{ leaders.length }}</strong> /
            上限: {{ competition?.maxLeaders ?? 1 }} 人 (可选填)
          </span>
        </div>

        <n-button
          size="small"
          secondary
          :disabled="isMaxLeadersReached"
          @click="addLeaderRow"
        >
          <template #icon>
            <n-icon :component="AddOutline" />
          </template>
          {{ t('competition.add_leader_row', '添加领队行') }}
        </n-button>
      </div>

      <div class="overflow-x-auto border border-google-border-light dark:border-google-border-dark rounded-xl">
        <n-table :bordered="true" :single-line="false" size="small" class="min-w-[1240px] text-xs">
          <thead>
            <tr class="bg-google-surface-light dark:bg-google-surface-dark text-google-text-desc dark:text-google-text-darkDesc whitespace-nowrap">
              <th class="w-24 p-2.5 font-semibold">{{ t('competition.col_name', '姓名') }} <span class="text-google-red dark:text-google-red-dark">*</span></th>
              <th class="w-20 p-2.5 font-semibold">{{ t('competition.col_gender', '性别') }}</th>
              <th class="w-20 p-2.5 font-semibold text-center">{{ t('competition.col_role', '角色') }}</th>
              <th class="w-32 p-2.5 font-semibold">
                {{ formData.organizationType !== 'SCHOOL' ? t('competition.col_employee_number', '工号') : t('competition.col_student_number', '学号') }}
              </th>
              <th class="w-32 p-2.5 font-semibold">{{ t('competition.col_phone', '手机号') }} <span class="text-google-red dark:text-google-red-dark">*</span></th>
              <th class="w-40 p-2.5 font-semibold">{{ t('competition.col_email', '邮箱') }}</th>
              <th class="w-44 p-2.5 font-semibold">{{ t('competition.col_id_card', '身份证') }}</th>
              <th class="w-32 p-2.5 font-semibold">
                {{ formData.organizationType !== 'SCHOOL' ? t('competition.col_department_real', '部门') : t('competition.col_college', '学院') }}
              </th>
              <th class="w-28 p-2.5 font-semibold">
                {{ formData.organizationType !== 'SCHOOL' ? t('competition.col_position', '职位') : t('competition.col_class_name', '班级') }}
              </th>
              <th class="w-16 p-2.5 font-semibold text-center">{{ t('competition.col_action', '操作') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-google-border-light/60 dark:divide-google-border-dark/60 bg-white dark:bg-google-surface-darkCard">
            <tr v-if="leaders.length === 0">
              <td colspan="10" class="text-center p-4 text-google-text-muted dark:text-google-text-darkMuted">
                暂无领队/指导老师，点击上方按钮添加
              </td>
            </tr>
            <tr v-for="(row, idx) in leaders" :key="'leader-' + idx">
              <td class="p-2">
                <n-input v-model:value="row.realName" size="small" :placeholder="t('competition.col_name', '姓名')" maxlength="64" />
              </td>
              <td class="p-2">
                <n-select v-model:value="row.gender" size="small" :options="genderOptions" />
              </td>
              <td class="p-2 text-center text-google-text-desc dark:text-google-text-darkDesc font-medium">
                {{ t('competition.role_leader', '指导老师') }}
              </td>
              <td class="p-2">
                <n-input v-model:value="row.employeeNumber" size="small" placeholder="可选" maxlength="64" />
              </td>
              <td class="p-2">
                <n-input v-model:value="row.phone" size="small" :placeholder="t('competition.col_phone', '手机号')" maxlength="16" />
              </td>
              <td class="p-2">
                <n-input v-model:value="row.email" size="small" placeholder="可选" maxlength="128" />
              </td>
              <td class="p-2">
                <n-input v-model:value="row.idCard" size="small" placeholder="可选" maxlength="32" />
              </td>
              <td class="p-2">
                <n-input v-model:value="row.department" size="small" placeholder="可选" maxlength="128" />
              </td>
              <td class="p-2">
                <n-input v-model:value="row.position" size="small" placeholder="可选" maxlength="64" />
              </td>
              <td class="p-2 text-center">
                <n-button size="tiny" quaternary type="error" @click="removeLeaderRow(idx)">
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

    <div class="flex items-center justify-end gap-3 pt-4 border-t border-google-border-light dark:border-google-border-dark">
      <button
        type="button"
        class="h-9 px-5 rounded-full border border-google-border-light dark:border-google-border-dark text-google-text-desc dark:text-google-text-darkDesc bg-transparent hover:bg-google-surface-light dark:hover:bg-google-surface-dark text-xs font-semibold transition-colors cursor-pointer"
        @click="handleClose"
      >
        {{ t('common.cancel', '取消') }}
      </button>

      <button
        type="button"
        class="h-9 px-6 rounded-full bg-google-blue hover:bg-google-blue-hover text-white dark:bg-google-blue-dark dark:text-google-surface-dark text-xs font-semibold transition-colors cursor-pointer border-none flex items-center gap-1.5"
        :disabled="submitting"
        @click="handleSubmit"
      >
        <n-icon v-if="submitting" :size="14" class="animate-spin" :component="RefreshOutline" />
        <span>{{ t('competition.submit_signup', '确认报名创建战队') }}</span>
      </button>
    </div>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import {
  NModal,
  NForm,
  NFormItem,
  NInput,
  NSelect,
  NTable,
  NButton,
  NIcon,
  NTag,
  useMessage,
  type FormInst,
  type FormRules
} from 'naive-ui'
import {
  PeopleOutline,
  InformationCircleOutline,
  AddOutline,
  TrashOutline,
  RefreshOutline
} from '@vicons/ionicons5'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/store/modules/user'
import { getCurrentUserProfileApi } from '@/api/user'
import { signupTeamApi } from '@/api/competition'
import { isValidChinaPhone, isValidIdCard } from '@/utils/validation'
import type {
  CompetitionDetailVO,
  TeamMemberSignupDTO,
  TeamLeadersSignupDTO,
  OrganizationType,
  TeamSignupRequestDTO
} from '@/types/competition'

defineOptions({
  name: 'TeamSignupModal'
})

const props = defineProps<{
  show: boolean
  competition: CompetitionDetailVO | null
}>()

const emit = defineEmits<{
  (e: 'update:show', val: boolean): void
  (e: 'success'): void
}>()

const { t } = useI18n()
const message = useMessage()
const userStore = useUserStore()

const formRef = ref<FormInst | null>(null)
const submitting = ref(false)
const showLeadersSection = ref(false)

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
  { label: '高等院校 (School)', value: 'SCHOOL' },
  { label: '企业单位 (Enterprise)', value: 'ENTERPRISE' },
  { label: '军工单位 (Military)', value: 'MILITARY' },
  { label: '能源行业 (Energy)', value: 'ENERGY' },
  { label: '金融行业 (Finance)', value: 'FINANCE' },
  { label: '科研院所 (Research)', value: 'RESEARCH' },
  { label: '其他组织 (Other)', value: 'OTHER' }
]

const genderOptions = [
  { label: '男 (Male)', value: 'MALE' },
  { label: '女 (Female)', value: 'FEMALE' }
]

const members = ref<TeamMemberSignupDTO[]>([])
const leaders = ref<TeamLeadersSignupDTO[]>([])

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

watch(
  () => props.show,
  async (val) => {
    if (val) {
      await initModalData()
    }
  }
)

async function initModalData() {
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
      position: ''
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
    position: ''
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
  if (!props.competition?.id || submitting.value) return

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

      const signupPayload: TeamSignupRequestDTO = {
        teamName: formData.teamName.trim(),
        avatarUrl: formData.avatarUrl || undefined,
        description: formData.description?.trim() || undefined,
        organization: formData.organization?.trim() || undefined,
        organizationType: formData.organizationType,
        members: sanitizedMembers,
        leaders: sanitizedLeaders.length > 0 ? sanitizedLeaders : undefined
      }

      await signupTeamApi(props.competition!.id, signupPayload)
      message.success(t('competition.signup_success', '战队创建与报名成功！'))
      emit('update:show', false)
      emit('success')
    } catch {
    } finally {
      submitting.value = false
    }
  })
}
</script>
