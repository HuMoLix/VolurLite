<template>
  <n-modal
    :show="show"
    preset="card"
    class="team-form-modal"
    style="width: 1200px; max-width: 96vw; border-radius: 12px;"
    :title="modalTitle"
    :bordered="false"
    @update:show="handleClose"
  >
    <!-- Top Form: Team Name -->
    <n-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-placement="top"
      require-mark-placement="right-hanging"
    >
      <div class="team-meta-inputs">
        <n-form-item :label="t('competition.team_name_label')" path="teamName" class="meta-item">
          <n-input
            v-model:value="formData.teamName"
            :placeholder="t('competition.team_name_placeholder')"
            maxlength="32"
            :disabled="props.mode !== 'create'"
            show-count
            clearable
          />
        </n-form-item>
      </div>

      <!-- Member Roster Section Header -->
      <div class="roster-header">
        <div class="roster-title-area">
          <h4 class="roster-title">{{ t('competition.member_table_title') }}</h4>
          <span class="roster-count">
            当前成员: <strong>{{ memberCount }}</strong> /
            限制: {{ competition?.minTeamMembers ?? 1 }} ~ {{ competition?.maxTeamMembers ?? 4 }} 人
            <template v-if="competition?.maxLeaders !== undefined">
              (领队: {{ leaderCount }} / {{ competition.maxLeaders }} 人)
            </template>
            <span class="total-limit-text">
              （名册上限: {{ maxTotalRows }} 人）
            </span>
          </span>
        </div>
        <n-button
          v-if="isEditing"
          size="small"
          secondary
          :disabled="isMaxRowsReached"
          @click="addMemberRow"
        >
          <template #icon>
            <n-icon :component="AddOutline" />
          </template>
          {{ t('competition.add_member_row') }}
        </n-button>
      </div>

      <!-- Member Table -->
      <div class="table-scroll-container">
        <n-table :bordered="true" :single-line="false" size="small" class="roster-table">
          <thead>
            <tr>
              <th style="width: 100px;">{{ t('competition.col_name') }} <span v-if="isEditing" class="req">*</span></th>
              <th style="width: 90px;">{{ t('competition.col_gender') }} <span v-if="isEditing" class="req">*</span></th>
              <th style="width: 95px;">{{ t('competition.col_role') }} <span v-if="isEditing" class="req">*</span></th>
              <th style="width: 130px;">{{ t('competition.col_student_number') }} <span v-if="isEditing" class="req">*</span></th>
              <th style="width: 135px;">{{ t('competition.col_phone') }} <span v-if="isEditing" class="req">*</span></th>
              <th style="width: 165px;">{{ t('competition.col_email') }} <span v-if="isEditing" class="req">*</span></th>
              <th style="width: 175px;">{{ t('competition.col_id_card') }} <span v-if="isEditing" class="req">*</span></th>
              <th style="width: 140px;">{{ t('competition.col_college') }} <span v-if="isEditing" class="req">*</span></th>
              <th style="width: 105px; text-align: center;">{{ t('competition.col_status') }}</th>
              <th v-if="isEditing" style="width: 65px; text-align: center;">{{ t('competition.col_action') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, idx) in members" :key="idx" :class="{ 'captain-row': row.role === 'CAPTAIN' }">
              <!-- 1. 姓名 -->
              <td>
                <n-input
                  v-model:value="row.realName"
                  size="small"
                  :disabled="!isEditing"
                  :placeholder="t('competition.col_name')"
                  maxlength="64"
                />
              </td>

              <!-- 2. 性别 (仅男/女) -->
              <td>
                <n-select
                  v-model:value="row.gender"
                  size="small"
                  :disabled="!isEditing"
                  :options="genderOptions"
                />
              </td>

              <!-- 3. 角色 -->
              <td>
                <n-select
                  v-model:value="row.role"
                  size="small"
                  :options="idx === 0 ? captainRoleOptions : memberRoleOptions"
                  :disabled="!isEditing || idx === 0"
                />
              </td>

              <!-- 4. 学号 -->
              <td>
                <n-input
                  v-model:value="row.studentNumber"
                  size="small"
                  :disabled="!isEditing"
                  :placeholder="t('competition.col_student_number')"
                  maxlength="64"
                />
              </td>

              <!-- 5. 手机号 -->
              <td>
                <n-input
                  v-model:value="row.phone"
                  size="small"
                  :disabled="!isEditing"
                  :placeholder="t('competition.col_phone')"
                  maxlength="16"
                />
              </td>

              <!-- 6. 邮箱 (注册账号邮箱) -->
              <td>
                <n-input
                  v-model:value="row.email"
                  size="small"
                  :disabled="!isEditing"
                  :placeholder="t('competition.col_email')"
                  maxlength="128"
                />
              </td>

              <!-- 7. 身份证 -->
              <td>
                <n-input
                  v-model:value="row.idCard"
                  size="small"
                  :disabled="!isEditing"
                  :placeholder="t('competition.col_id_card')"
                  maxlength="32"
                />
              </td>

              <!-- 8. 学院 -->
              <td>
                <n-input
                  v-model:value="row.college"
                  size="small"
                  :disabled="!isEditing"
                  :placeholder="t('competition.col_college')"
                  maxlength="128"
                />
              </td>

              <!-- 9. 加入状态 -->
              <td style="text-align: center;">
                <n-tag
                  size="small"
                  :type="getMemberStatusTag(row).type"
                  round
                >
                  {{ getMemberStatusTag(row).label }}
                </n-tag>
              </td>

              <!-- 10. 操作 (仅编辑态) -->
              <td v-if="isEditing" style="text-align: center;">
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

      <!-- Action Buttons -->
      <div class="modal-actions">
        <div class="actions-left">
          <!-- Disband team button for captain (available when initialTeam exists) -->
          <n-popconfirm
            v-if="props.mode !== 'create' && isCaptain"
            :positive-text="t('common.confirm', '确认')"
            :negative-text="t('common.cancel', '取消')"
            @positive-click="handleDisbandTeam"
          >
            <template #trigger>
              <n-button type="error" quaternary :loading="disbanding">
                <template #icon>
                  <n-icon :component="TrashOutline" />
                </template>
                {{ t('competition.disband_team') }}
              </n-button>
            </template>
            <div>
              <strong>{{ t('competition.disband_confirm_title') }}</strong>
              <div style="font-size: 12px; color: #666; margin-top: 4px;">
                {{ t('competition.disband_confirm_content') }}
              </div>
            </div>
          </n-popconfirm>
        </div>

        <div class="actions-right">
          <!-- 模式 1: 战队报名创建 -->
          <template v-if="props.mode === 'create'">
            <n-button @click="handleClose">取消</n-button>
            <n-button class="theme-primary-btn" :loading="submitting" @click="handleSubmit">
              {{ t('competition.submit_signup') }}
            </n-button>
          </template>

          <!-- 模式 2: 查看状态 (未进入修改态) -->
          <template v-else-if="!isEditing">
            <n-button @click="handleClose">关闭</n-button>
            <n-button
              v-if="isCaptain"
              class="theme-primary-btn"
              @click="startEditing"
            >
              <template #icon>
                <n-icon :component="CreateOutline" />
              </template>
              {{ t('competition.edit_team_btn') }}
            </n-button>
          </template>

          <!-- 模式 3: 编辑状态中 -->
          <template v-else>
            <n-button @click="cancelEditing">
              {{ t('competition.cancel_edit_btn') }}
            </n-button>
            <n-button class="theme-primary-btn" :loading="submitting" @click="handleSubmit">
              {{ t('competition.submit_update') }}
            </n-button>
          </template>
        </div>
      </div>
    </n-form>
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
  NPopconfirm,
  useMessage,
  type FormInst,
  type FormRules
} from 'naive-ui'
import { AddOutline, TrashOutline, CreateOutline } from '@vicons/ionicons5'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/store/modules/user'
import { getCurrentUserProfileApi } from '@/api/user'
import { signupTeamApi, updateMyTeamApi, deleteMyTeamApi } from '@/api/competition'
import { isValidChinaPhone, isValidIdCard } from '@/utils/validation'
import type {
  CompetitionDetailVO,
  MyTeamInfoVO,
  TeamSignupRequestDTO,
  TeamUpdateRequestDTO,
  TeamMemberSignupDTO,
  MemberStatus
} from '@/types/competition'

interface FormMemberRow extends TeamMemberSignupDTO {
  status?: MemberStatus | string
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
  (e: 'disbanded'): void
}>()

const { t } = useI18n()
const message = useMessage()
const userStore = useUserStore()

const formRef = ref<FormInst | null>(null)
const submitting = ref<boolean>(false)
const disbanding = ref<boolean>(false)

// 内部是否处于编辑修改状态
const isEditing = ref<boolean>(false)

const isCaptain = computed(() => {
  if (!props.initialTeam) return true
  return props.initialTeam.myRole === 'CAPTAIN' || Boolean(props.initialTeam.isCaptain)
})

const modalTitle = computed(() => {
  if (props.mode === 'create') {
    return t('competition.signup_modal_title')
  }
  if (isEditing.value) {
    return t('competition.edit_team_modal_title')
  }
  return t('competition.view_team_modal_title')
})

const formData = reactive<{
  teamName: string
}>({
  teamName: ''
})

const formRules: FormRules = {
  teamName: [
    { required: true, message: '请输入战队名称', trigger: 'blur' },
    { min: 2, max: 32, message: '战队名称长度需在 2 到 32 字符之间', trigger: 'blur' }
  ]
}

const members = ref<FormMemberRow[]>([])

// 仅提供男、女两种选项，不允许未知
const genderOptions = computed(() => [
  { label: t('competition.gender_male'), value: 'MALE' },
  { label: t('competition.gender_female'), value: 'FEMALE' }
])

const captainRoleOptions = computed(() => [
  { label: t('competition.role_captain'), value: 'CAPTAIN' }
])

const memberRoleOptions = computed(() => [
  { label: t('competition.role_member'), value: 'MEMBER' },
  { label: t('competition.role_leader'), value: 'LEADER' }
])

const memberCount = computed(() => {
  return members.value.filter(m => m.role !== 'LEADER').length
})

const leaderCount = computed(() => {
  return members.value.filter(m => m.role === 'LEADER').length
})

const maxTotalRows = computed(() => {
  const maxMembers = props.competition?.maxTeamMembers ?? 4
  const maxLeaders = props.competition?.maxLeaders ?? 1
  return maxMembers + maxLeaders
})

const isMaxRowsReached = computed(() => {
  return members.value.length >= maxTotalRows.value
})

watch(
  () => props.show,
  async (newVal) => {
    if (newVal) {
      if (props.mode === 'create') {
        isEditing.value = true
      } else if (props.mode === 'edit') {
        isEditing.value = true
      } else {
        isEditing.value = false
      }
      await initModalData()
    }
  }
)

function getMemberStatusTag(row: FormMemberRow): { type: 'success' | 'warning' | 'default' | 'info'; label: string } {
  if (row.role === 'CAPTAIN' || row.status === 'INVITED_ACCEPTED') {
    return { type: 'success', label: t('competition.status_joined') }
  }
  if (row.status === 'INVITED') {
    return { type: 'warning', label: t('competition.status_invited') }
  }
  if (row.status === 'NOT_PARTICIPATE') {
    return { type: 'default', label: t('competition.status_rejected') }
  }
  return { type: 'info', label: t('competition.status_pending') }
}

function startEditing() {
  isEditing.value = true
}

function cancelEditing() {
  isEditing.value = false
  initModalData()
}

async function initModalData() {
  if (props.mode !== 'create' && props.initialTeam) {
    formData.teamName = props.initialTeam.teamName || ''
    if (props.initialTeam.members && props.initialTeam.members.length > 0) {
      // 排序确保队长处于第 0 行
      const sorted = [...props.initialTeam.members].sort((a, b) => {
        if (a.role === 'CAPTAIN') return -1
        if (b.role === 'CAPTAIN') return 1
        return 0
      })

      members.value = sorted.map((m) => ({
        realName: m.realName || m.username || '',
        gender: (m.gender === 'FEMALE' ? 'FEMALE' : 'MALE') as 'MALE' | 'FEMALE',
        role: m.role || 'MEMBER',
        studentNumber: m.studentNumber || '',
        phone: m.phone || '',
        email: m.email || '',
        idCard: m.idCard || '',
        college: m.college || '',
        status: m.status
      }))
      return
    }
  }

  // Create 模式或无初始数据：初始化队长行为当前用户
  formData.teamName = ''

  let currentUserPhone = ''
  let currentUserEmail = userStore.user?.email || ''
  let currentUserName = userStore.user?.username || ''
  let currentUserGender: 'MALE' | 'FEMALE' = 'MALE'
  let currentUserOrg = userStore.user?.org || ''

  try {
    const profileRes = await getCurrentUserProfileApi()
    if (profileRes.data) {
      currentUserPhone = profileRes.data.phone || currentUserPhone
      currentUserEmail = profileRes.data.email || currentUserEmail
      currentUserName = profileRes.data.username || currentUserName
      if (profileRes.data.gender === 'FEMALE') {
        currentUserGender = 'FEMALE'
      }
      currentUserOrg = profileRes.data.org || ''
    }
  } catch {
    // fallback to store values
  }

  // Row 0: Captain
  members.value = [
    {
      realName: currentUserName,
      gender: currentUserGender,
      role: 'CAPTAIN',
      studentNumber: '',
      phone: currentUserPhone,
      email: currentUserEmail,
      idCard: '',
      college: currentUserOrg,
      status: 'INVITED_ACCEPTED'
    }
  ]
}

function addMemberRow() {
  if (isMaxRowsReached.value) {
    message.warning(t('competition.validation_max_rows', { max: maxTotalRows.value }))
    return
  }

  members.value.push({
    realName: '',
    gender: 'MALE',
    role: 'MEMBER',
    studentNumber: '',
    phone: '',
    email: '',
    idCard: '',
    college: '',
    status: 'INVITED'
  })
}

function removeMemberRow(idx: number) {
  if (idx === 0) return
  members.value.splice(idx, 1)
}

function handleClose() {
  emit('update:show', false)
}

async function handleDisbandTeam() {
  if (!props.competition) return

  disbanding.value = true
  try {
    await deleteMyTeamApi(props.competition.id)
    message.success('战队已成功解散')
    emit('update:show', false)
    emit('disbanded')
  } catch {
    // Handled by request interceptor
  } finally {
    disbanding.value = false
  }
}

function validateMembers(): boolean {
  if (!props.competition) return false

  const minMembers = props.competition.minTeamMembers ?? 1
  const maxMembers = props.competition.maxTeamMembers ?? 4
  const maxLeaders = props.competition.maxLeaders ?? 1

  if (members.value.length > maxTotalRows.value) {
    message.error(t('competition.validation_max_rows', { max: maxTotalRows.value }))
    return false
  }

  // 1. 队长校验
  const captains = members.value.filter(m => m.role === 'CAPTAIN')
  if (captains.length !== 1) {
    message.error(t('competition.validation_captain_count'))
    return false
  }

  // 2. 领队校验
  const leaders = members.value.filter(m => m.role === 'LEADER')
  if (leaders.length > maxLeaders) {
    message.error(t('competition.validation_leader_count', { max: maxLeaders }))
    return false
  }

  // 3. 队员人数校验（含队长，不含领队）
  const regularMembers = members.value.filter(m => m.role !== 'LEADER')
  if (regularMembers.length < minMembers || regularMembers.length > maxMembers) {
    message.error(t('competition.validation_member_count', { min: minMembers, max: maxMembers }))
    return false
  }

  // 4. 每行必填与格式校验：姓名，性别，角色，学号，手机号，邮箱，身份证，学院，全部必填
  const studentNumberSet = new Set<string>()
  const phoneSet = new Set<string>()
  const emailSet = new Set<string>()
  const idCardSet = new Set<string>()

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

  for (let i = 0; i < members.value.length; i++) {
    const row = members.value[i]
    const rowNum = i + 1

    // 1) 姓名
    const realName = (row.realName || '').trim()
    if (!realName) {
      message.error(t('competition.validation_name_required', { row: rowNum }))
      return false
    }

    // 2) 性别
    if (!row.gender) {
      message.error(`请选择第 ${rowNum} 行成员的性别`)
      return false
    }

    // 3) 角色
    if (!row.role) {
      message.error(`请选择第 ${rowNum} 行成员的角色`)
      return false
    }

    // 4) 学号
    const studentNumber = (row.studentNumber || '').trim()
    if (!studentNumber) {
      message.error(t('competition.validation_student_number_required', { row: rowNum }))
      return false
    }
    if (studentNumberSet.has(studentNumber)) {
      message.error(t('competition.validation_duplicate_student_number', { studentNumber }))
      return false
    }
    studentNumberSet.add(studentNumber)

    // 5) 手机号 (严格校验中国四大基础电信运营商号段)
    const phone = (row.phone || '').trim()
    if (!phone) {
      message.error(t('competition.validation_phone_required', { row: rowNum }))
      return false
    }
    if (!isValidChinaPhone(phone)) {
      message.error(t('competition.validation_phone_format', { row: rowNum }))
      return false
    }
    if (phoneSet.has(phone)) {
      message.error(t('competition.validation_duplicate_phone', { phone }))
      return false
    }
    phoneSet.add(phone)

    // 6) 邮箱
    const email = (row.email || '').trim().toLowerCase()
    if (!email) {
      message.error(t('competition.validation_email_required', { row: rowNum }))
      return false
    }
    if (!emailRegex.test(email)) {
      message.error(t('competition.validation_email_format', { row: rowNum }))
      return false
    }
    if (emailSet.has(email)) {
      message.error(t('competition.validation_duplicate_email', { email }))
      return false
    }
    emailSet.add(email)

    // 7) 身份证 (严格校验二代身份证 GB 11643-1999 格式、行政区划、平闰年生日与校验码)
    const idCard = (row.idCard || '').trim()
    if (!idCard) {
      message.error(t('competition.validation_id_card_required', { row: rowNum }))
      return false
    }
    if (!isValidIdCard(idCard)) {
      message.error(t('competition.validation_id_card_format', { row: rowNum }))
      return false
    }
    const idCardUpper = idCard.toUpperCase()
    if (idCardSet.has(idCardUpper)) {
      message.error(t('competition.validation_duplicate_id_card', { idCard }))
      return false
    }
    idCardSet.add(idCardUpper)

    // 8) 学院
    const college = (row.college || '').trim()
    if (!college) {
      message.error(t('competition.validation_college_required', { row: rowNum }))
      return false
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
      const sanitizedMembers: TeamMemberSignupDTO[] = members.value.map(m => ({
        realName: (m.realName || '').trim(),
        gender: m.gender,
        role: m.role,
        studentNumber: (m.studentNumber || '').trim(),
        phone: (m.phone || '').trim(),
        email: (m.email || '').trim().toLowerCase(),
        idCard: (m.idCard || '').trim().toUpperCase(),
        college: (m.college || '').trim()
      }))

      if (props.mode !== 'create') {
        const updatePayload: TeamUpdateRequestDTO = {
          members: sanitizedMembers
        }
        await updateMyTeamApi(props.competition!.id, updatePayload)
        message.success('战队信息已更新并重新提交审核')
        isEditing.value = false
      } else {
        const signupPayload: TeamSignupRequestDTO = {
          teamName: formData.teamName.trim(),
          members: sanitizedMembers
        }
        await signupTeamApi(props.competition!.id, signupPayload)
        message.success(t('competition.signup_success'))
      }

      emit('update:show', false)
      emit('success')
    } catch {
      // Handled by request interceptor
    } finally {
      submitting.value = false
    }
  })
}
</script>

<style lang="scss" scoped>
.team-form-modal {
  .team-meta-inputs {
    display: grid;
    grid-template-columns: 1fr;
    gap: 16px;

    .meta-item {
      margin-bottom: 16px;
    }
  }

  .roster-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;
    padding-bottom: 8px;
    border-bottom: 1px solid var(--v-border-base, #e5e4e7);

    .roster-title-area {
      display: flex;
      align-items: baseline;
      gap: 12px;
      flex-wrap: wrap;

      .roster-title {
        margin: 0;
        font-size: 15px;
        font-weight: 600;
        color: var(--v-text-primary, #111111);
      }

      .roster-count {
        font-size: 12px;
        color: #444444;

        strong {
          color: var(--v-text-primary, #111111);
        }

        .total-limit-text {
          color: #666666;
          margin-left: 4px;
        }
      }
    }
  }

  .table-scroll-container {
    overflow-x: auto;
    margin-bottom: 20px;
    border: 1px solid var(--v-border-base, #e5e4e7);
    border-radius: 6px;

    .roster-table {
      min-width: 1240px;

      th {
        font-weight: 600;
        background: var(--v-bg-card-alt, #fafafc);
        color: #444444;
        white-space: nowrap;

        .req {
          color: var(--v-color-error, #d03050);
          margin-left: 2px;
        }
      }

      td {
        padding: 6px 8px;
        vertical-align: middle;
      }

      tr.captain-row {
        background-color: rgba(0, 0, 0, 0.015);
      }
    }
  }

  .modal-actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
    margin-top: 12px;

    .actions-left {
      display: flex;
      align-items: center;
    }

    .actions-right {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .theme-primary-btn {
      background-color: #000000 !important;
      color: #ffffff !important;
      border: none;
      font-weight: 500;

      &:hover {
        background-color: #222222 !important;
      }
    }
  }
}
</style>
