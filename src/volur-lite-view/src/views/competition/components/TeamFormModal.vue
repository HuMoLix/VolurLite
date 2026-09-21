<template>
  <n-modal
    :show="show"
    preset="card"
    class="team-form-modal"
    style="width: 1040px; max-width: 96vw; border-radius: 12px;"
    :title="modalTitle"
    :bordered="false"
    @update:show="handleClose"
  >
    <!-- Top Meta Container: 左右分栏 (左框: 队伍基础信息，右框: 竞赛报名要求与须知) -->
    <div class="team-meta-container">
      <!-- 左框: 队伍基础信息表单 (占 50%) -->
      <div class="meta-left-panel">
        <n-form
          ref="formRef"
          :model="formData"
          :rules="formRules"
          label-placement="top"
          require-mark-placement="right-hanging"
        >
          <div class="team-meta-inputs">
            <!-- 第 1 行: 战队头像 + 战队名称 -->
            <div class="team-meta-row team-meta-row-1">
              <div class="team-avatar-wrapper">
                <UserAvatar
                  :size="52"
                  :src="teamAvatarDisplayUrl"
                  :username="formData.teamName || 'Team'"
                  :editable="isEditing && isCaptain"
                  overlay-text="更换"
                  @edit="showCropperModal = true"
                />
              </div>
              <n-form-item :label="t('competition.team_name_label')" path="teamName" class="meta-item team-name-item">
                <n-input
                  v-model:value="formData.teamName"
                  :placeholder="t('competition.team_name_placeholder')"
                  maxlength="32"
                  :disabled="props.mode !== 'create'"
                  show-count
                  clearable
                  style="width: 100%;"
                />
              </n-form-item>
            </div>

            <!-- 第 2 行: 战队单位 + 单位类型 -->
            <div class="team-meta-row team-meta-row-2">
              <n-form-item
                :label="t('competition.team_organization_label', '队伍单位')"
                path="organization"
                class="meta-item unit-input-item"
              >
                <n-input
                  v-model:value="formData.organization"
                  :placeholder="t('competition.team_organization_placeholder', '请输入队伍所属单位')"
                  maxlength="128"
                  :disabled="!isEditing"
                  clearable
                  style="width: 100%;"
                />
              </n-form-item>

              <n-form-item
                :label="t('competition.team_org_type_label', '单位类型')"
                path="organizationType"
                class="meta-item unit-type-item"
              >
                <n-select
                  v-model:value="formData.organizationType"
                  :options="organizationTypeOptions"
                  :disabled="!isEditing"
                  style="width: 100%;"
                />
              </n-form-item>
            </div>

            <!-- 第 3 行: 队伍简介 -->
            <div class="team-meta-row team-meta-row-3">
              <n-form-item :label="t('competition.team_desc_label', '队伍简介')" path="description" class="meta-item desc-item">
                <n-input
                  v-model:value="formData.description"
                  type="textarea"
                  :placeholder="t('competition.team_desc_placeholder', '请输入队伍简介')"
                  maxlength="200"
                  :disabled="!isEditing"
                  show-count
                  :autosize="{ minRows: 2, maxRows: 2 }"
                  style="width: 100%;"
                />
              </n-form-item>
            </div>
          </div>
        </n-form>
      </div>

      <!-- 中间分割线 -->
      <div class="meta-divider" />

      <!-- 右框: 竞赛报名要求与须知 (占 50%) -->
      <div class="meta-right-panel">
        <div class="requirements-card">
          <div class="requirements-header">
            <div class="req-title">
              <n-icon :component="InformationCircleOutline" class="req-icon" />
              <span>{{ t('competition.requirements_title', '竞赛报名要求与须知') }}</span>
            </div>
          </div>

          <div class="requirements-body">
            <!-- 系统生成部分 -->
            <div class="system-req-list">
              <div class="req-line">
                <span class="line-label">{{ t('competition.req_reg_time', '报名时间') }}:</span>
                <span class="line-val">{{ formattedRegTimeRange }}</span>
              </div>
              <div class="req-line">
                <span class="line-label">{{ t('competition.req_comp_time', '比赛时间') }}:</span>
                <span class="line-val">{{ formattedCompTimeRange }}</span>
              </div>
              <div class="req-line">
                <span class="line-label">{{ t('competition.req_member_limit', '人数要求') }}:</span>
                <span class="line-val">
                  每队 {{ competition?.minTeamMembers ?? 1 }} ~ {{ competition?.maxTeamMembers ?? 4 }} 人
                </span>
              </div>
              <div class="req-line">
                <span class="line-label">{{ t('competition.req_leader_limit', '领队要求') }}:</span>
                <span class="line-val">
                  最多 {{ competition?.maxLeaders ?? 1 }} 名指导老师/领队
                </span>
              </div>
            </div>

            <!-- 管理员自定义报名要求 -->
            <div class="custom-req-wrapper">
              <div class="custom-req-header">
                <span>{{ t('competition.req_admin_custom_title', '特别说明') }}:</span>
              </div>
              <div class="custom-req-content">
                <template v-if="adminCustomRequirements">
                  {{ adminCustomRequirements }}
                </template>
                <template v-else>
                  <span class="custom-empty-tip">{{ t('competition.req_admin_custom_empty', '暂无额外补充要求') }}</span>
                </template>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

      <!-- Member Roster Section Header -->
      <div class="roster-header">
        <div class="roster-title-area">
          <h4 class="roster-title">{{ t('competition.member_table_title') }}</h4>
          <span class="roster-count">
            当前成员: <strong>{{ members.length }}</strong> /
            限制: {{ competition?.minTeamMembers ?? 1 }} ~ {{ competition?.maxTeamMembers ?? 4 }} 人
            <span class="total-limit-text">
              （上限: {{ competition?.maxTeamMembers ?? 4 }} 人）
            </span>
          </span>
        </div>
        <n-button
          v-if="isEditing"
          size="small"
          secondary
          :disabled="isMaxMembersReached"
          @click="addMemberRow"
        >
          <template #icon>
            <n-icon :component="AddOutline" />
          </template>
          {{ t('competition.add_member_row') }}
        </n-button>
      </div>

      <!-- Member Table (参赛成员名册) -->
      <div class="table-scroll-container">
        <n-table :bordered="true" :single-line="false" size="small" class="roster-table">
          <thead>
            <tr>
              <th style="width: 100px;">{{ t('competition.col_name') }} <span v-if="isEditing" class="req">*</span></th>
              <th style="width: 90px;">{{ t('competition.col_gender') }} <span v-if="isEditing" class="req">*</span></th>
              <th style="width: 85px; text-align: center;">{{ t('competition.col_role') }}</th>

              <!-- 学校单位: 学号 / 企业及其他单位: 工号 -->
              <th style="width: 130px;">
                {{ formData.organizationType !== 'SCHOOL' ? t('competition.col_employee_number', '工号') : t('competition.col_student_number', '学号') }}
                <span v-if="isEditing" class="req">*</span>
              </th>

              <th style="width: 135px;">{{ t('competition.col_phone') }} <span v-if="isEditing" class="req">*</span></th>
              <th style="width: 165px;">{{ t('competition.col_email') }} <span v-if="isEditing" class="req">*</span></th>
              <th style="width: 175px;">{{ t('competition.col_id_card') }} <span v-if="isEditing" class="req">*</span></th>

              <!-- 学校单位: 学院 / 企业及其他单位: 部门 -->
              <th style="width: 140px;">
                {{ formData.organizationType !== 'SCHOOL' ? t('competition.col_department_real', '部门') : t('competition.col_college', '学院') }}
                <span v-if="isEditing" class="req">*</span>
              </th>

              <!-- 学校单位: 班级 / 企业及其他单位: 职位 -->
              <th style="width: 120px;">
                {{ formData.organizationType !== 'SCHOOL' ? t('competition.col_position', '职位') : t('competition.col_class_name', '班级') }}
                <span v-if="isEditing" class="req">*</span>
              </th>

              <th style="width: 105px; text-align: center;">{{ t('competition.col_status') }}</th>
              <th v-if="isEditing || hasReinvitableMembers" style="width: 85px; text-align: center;">{{ t('competition.col_action') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, idx) in members" :key="idx" :class="{ 'captain-row': idx === 0 }">
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

              <!-- 3. 角色 (普通文字样式，非胶囊) -->
              <td style="text-align: center;">
                <span :class="['role-text', idx === 0 ? 'role-captain' : 'role-member']">
                  {{ idx === 0 ? t('competition.role_captain') : t('competition.role_member') }}
                </span>
              </td>

              <!-- 4. 学号 / 工号 -->
              <td>
                <n-input
                  v-model:value="row.employeeNumber"
                  size="small"
                  :disabled="!isEditing"
                  :placeholder="formData.organizationType !== 'SCHOOL' ? t('competition.col_employee_number', '工号') : t('competition.col_student_number', '学号')"
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

              <!-- 8. 学院 / 部门 -->
              <td>
                <n-input
                  v-model:value="row.department"
                  size="small"
                  :disabled="!isEditing"
                  :placeholder="formData.organizationType !== 'SCHOOL' ? t('competition.col_department_real', '部门') : t('competition.col_college', '学院')"
                  maxlength="128"
                />
              </td>

              <!-- 9. 班级 / 职位 -->
              <td>
                <n-input
                  v-model:value="row.position"
                  size="small"
                  :disabled="!isEditing"
                  :placeholder="formData.organizationType !== 'SCHOOL' ? t('competition.col_position', '职位') : t('competition.col_class_name', '班级')"
                  maxlength="64"
                />
              </td>

              <!-- 10. 加入状态 -->
              <td style="text-align: center;">
                <n-tag
                  size="small"
                  :type="getMemberStatusTag(row).type"
                  round
                >
                  {{ getMemberStatusTag(row).label }}
                </n-tag>
              </td>

              <!-- 11. 操作 -->
              <td v-if="isEditing || hasReinvitableMembers" style="text-align: center;">
                <div class="row-actions-cell">
                  <!-- 重新邀请按钮 (当受邀队员拒绝且当前用户为队长时可见) -->
                  <n-button
                    v-if="row.status === 'NOT_PARTICIPATE' && isCaptain && props.mode !== 'create'"
                    size="tiny"
                    secondary
                    type="warning"
                    :loading="reinvitingUserId === row.userId"
                    @click="handleReinvite(row)"
                  >
                    {{ t('competition.reinvite', '重新邀请') }}
                  </n-button>

                  <!-- 删除行按钮 (仅编辑态且非队长) -->
                  <n-button
                    v-if="isEditing"
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
              </td>
            </tr>
          </tbody>
        </n-table>
      </div>

      <!-- 领队/指导老师切换文本行: 仅在编辑模式下显示（编辑已有战队时） -->
      <div v-if="isEditing && props.mode !== 'create'" class="leader-toggle-link-wrapper">
        <span class="leader-toggle-link" @click="toggleLeadersSection">
          {{ showLeadersSection ? t('competition.hide_leader_link', '收起领队/指导老师') : t('competition.add_leader_link', '添加领队/指导老师') }}
        </span>
      </div>

      <!-- 领队/指导老师名册 (可选项，无状态列) -->
      <transition name="fade">
        <div v-if="isEditing ? showLeadersSection : (leaders.length > 0)" class="leaders-section">
          <div class="roster-header leader-roster-header">
            <div class="roster-title-area">
              <h4 class="roster-title">{{ t('competition.leader_table_title', '领队/指导老师') }}</h4>
              <span class="roster-count">
                当前领队: <strong>{{ leaders.length }}</strong> /
                上限: {{ competition?.maxLeaders ?? 1 }} 人 (可选填)
              </span>
            </div>
            <n-button
              v-if="isEditing"
              size="small"
              secondary
              :disabled="isMaxLeadersReached"
              @click="addLeaderRow"
            >
              <template #icon>
                <n-icon :component="AddOutline" />
              </template>
              {{ t('competition.add_leader_row', '添加成员行') }}
            </n-button>
          </div>

          <div class="table-scroll-container">
            <n-table :bordered="true" :single-line="false" size="small" class="roster-table">
              <thead>
                <tr>
                  <th style="width: 100px;">{{ t('competition.col_name') }} <span v-if="isEditing" class="req">*</span></th>
                  <th style="width: 90px;">{{ t('competition.col_gender') }}</th>
                  <th style="width: 85px; text-align: center;">{{ t('competition.col_role') }}</th>

                  <!-- 学校单位: 学号 / 企业及其他单位: 工号 -->
                  <th style="width: 130px;">
                    {{ formData.organizationType !== 'SCHOOL' ? t('competition.col_employee_number', '工号') : t('competition.col_student_number', '学号') }}
                  </th>

                  <th style="width: 135px;">{{ t('competition.col_phone') }} <span v-if="isEditing" class="req">*</span></th>
                  <th style="width: 165px;">{{ t('competition.col_email') }}</th>
                  <th style="width: 175px;">{{ t('competition.col_id_card') }}</th>

                  <!-- 学校单位: 学院 / 企业及其他单位: 部门 -->
                  <th style="width: 140px;">
                    {{ formData.organizationType !== 'SCHOOL' ? t('competition.col_department_real', '部门') : t('competition.col_college', '学院') }}
                  </th>

                  <!-- 学校单位: 班级 / 企业及其他单位: 职位 -->
                  <th style="width: 120px;">
                    {{ formData.organizationType !== 'SCHOOL' ? t('competition.col_position', '职位') : t('competition.col_class_name', '班级') }}
                  </th>

                  <!-- 领队无需加入状态列 -->
                  <th v-if="isEditing" style="width: 70px; text-align: center;">{{ t('competition.col_action') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="leaders.length === 0">
                  <td :colspan="isEditing ? 10 : 9" style="text-align: center; color: #888; padding: 16px;">
                    暂无领队/指导老师，点击上方按钮添加
                  </td>
                </tr>
                <tr v-for="(row, idx) in leaders" :key="'leader-' + idx">
                  <!-- 1. 姓名 (必填) -->
                  <td>
                    <n-input
                      v-model:value="row.realName"
                      size="small"
                      :disabled="!isEditing"
                      :placeholder="t('competition.col_name')"
                      maxlength="64"
                    />
                  </td>

                  <!-- 2. 性别 (选填) -->
                  <td>
                    <n-select
                      v-model:value="row.gender"
                      size="small"
                      :disabled="!isEditing"
                      :options="genderOptions"
                    />
                  </td>

                  <!-- 3. 角色 (普通文字样式，非胶囊) -->
                  <td style="text-align: center;">
                    <span class="role-text role-leader">
                      {{ t('competition.role_leader', '指导老师') }}
                    </span>
                  </td>

                  <!-- 4. 学号 / 工号 (选填) -->
                  <td>
                    <n-input
                      v-model:value="row.employeeNumber"
                      size="small"
                      :disabled="!isEditing"
                      placeholder="可选"
                      maxlength="64"
                    />
                  </td>

                  <!-- 5. 手机号 (必填) -->
                  <td>
                    <n-input
                      v-model:value="row.phone"
                      size="small"
                      :disabled="!isEditing"
                      :placeholder="t('competition.col_phone')"
                      maxlength="16"
                    />
                  </td>

                  <!-- 6. 邮箱 (选填) -->
                  <td>
                    <n-input
                      v-model:value="row.email"
                      size="small"
                      :disabled="!isEditing"
                      placeholder="可选"
                      maxlength="128"
                    />
                  </td>

                  <!-- 7. 身份证 (选填) -->
                  <td>
                    <n-input
                      v-model:value="row.idCard"
                      size="small"
                      :disabled="!isEditing"
                      placeholder="可选"
                      maxlength="32"
                    />
                  </td>

                  <!-- 8. 学院 / 部门 (选填) -->
                  <td>
                    <n-input
                      v-model:value="row.department"
                      size="small"
                      :disabled="!isEditing"
                      placeholder="可选"
                      maxlength="128"
                    />
                  </td>

                  <!-- 9. 班级 / 职位 (选填) -->
                  <td>
                    <n-input
                      v-model:value="row.position"
                      size="small"
                      :disabled="!isEditing"
                      placeholder="可选"
                      maxlength="64"
                    />
                  </td>

                  <!-- 10. 操作 (无状态列) -->
                  <td v-if="isEditing" style="text-align: center;">
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

    <!-- 队伍头像裁剪弹窗 (参考 ProfileView) -->
    <AvatarCropperModal
      v-model:show="showCropperModal"
      :current-avatar="formData.avatarUrl"
      @success="handleTeamAvatarSuccess"
    />
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
import { AddOutline, TrashOutline, CreateOutline, InformationCircleOutline } from '@vicons/ionicons5'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/store/modules/user'
import { getCurrentUserProfileApi } from '@/api/user'
import { signupTeamApi, updateMyTeamApi, deleteMyTeamApi, reinviteMemberApi } from '@/api/competition'
import { isValidChinaPhone, isValidIdCard } from '@/utils/validation'
import UserAvatar from '@/components/common/UserAvatar.vue'
import AvatarCropperModal from '@/components/user/AvatarCropperModal.vue'
import { formatAvatarUrl } from '@/utils/imageCache'
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

// 控制是否展示领队/指导老师列表
const showLeadersSection = ref<boolean>(false)

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

const showCropperModal = ref<boolean>(false)
const reinvitingUserId = ref<number | string | null>(null)

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

const organizationTypeOptions = computed(() => [
  { label: t('competition.org_type_school', '学校'), value: 'SCHOOL' },
  { label: t('competition.org_type_enterprise', '企业单位'), value: 'ENTERPRISE' },
  { label: t('competition.org_type_military', '军工'), value: 'MILITARY' },
  { label: t('competition.org_type_energy', '能源'), value: 'ENERGY' },
  { label: t('competition.org_type_finance', '金融'), value: 'FINANCE' },
  { label: t('competition.org_type_research', '科研院所'), value: 'RESEARCH' },
  { label: t('competition.org_type_other', '其他'), value: 'OTHER' }
])

const teamAvatarDisplayUrl = computed(() => {
  return formatAvatarUrl(formData.avatarUrl)
})

const hasReinvitableMembers = computed(() => {
  return isCaptain.value && members.value.some(m => m.status === 'NOT_PARTICIPATE')
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
  return (props.competition?.regRequirements || props.competition?.config?.regRequirements || '').trim()
})

function handleTeamAvatarSuccess(avatarUrl: string) {
  formData.avatarUrl = avatarUrl
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
    emit('success')
  } catch {
    // Handled by request interceptor
  } finally {
    reinvitingUserId.value = null
  }
}

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

const members = ref<FormMemberRow[]>([])
const leaders = ref<FormLeaderRow[]>([])

// 仅提供男、女两种选项，不允许未知
const genderOptions = computed(() => [
  { label: t('competition.gender_male'), value: 'MALE' },
  { label: t('competition.gender_female'), value: 'FEMALE' }
])

const isMaxMembersReached = computed(() => {
  const maxMembers = props.competition?.maxTeamMembers ?? 4
  return members.value.length >= maxMembers
})

const isMaxLeadersReached = computed(() => {
  const maxLeaders = props.competition?.maxLeaders ?? 1
  return leaders.value.length >= maxLeaders
})

watch(
  () => props.show,
  async (newVal) => {
    if (newVal) {
      if (props.mode === 'create' || props.mode === 'edit') {
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

function hasSubstantiveLeaderInfo(): boolean {
  return leaders.value.some(l => {
    const realName = (l.realName || '').trim()
    const phone = (l.phone || '').trim()
    const email = (l.email || '').trim()
    const idCard = (l.idCard || '').trim()
    const emp = (l.employeeNumber || '').trim()
    const dept = (l.department || '').trim()
    const pos = (l.position || '').trim()
    return Boolean(realName || phone || email || idCard || emp || dept || pos)
  })
}

function toggleLeadersSection() {
  if (showLeadersSection.value) {
    if (hasSubstantiveLeaderInfo()) {
      message.warning('领队/指导老师列表中已填写相关信息，无法直接收起；若不需要请先清空或删除成员行')
      return
    }
    leaders.value = []
    showLeadersSection.value = false
  } else {
    showLeadersSection.value = true
    if (leaders.value.length === 0 && isEditing.value) {
      addLeaderRow()
    }
  }
}

async function initModalData() {
  if (props.mode !== 'create' && props.initialTeam) {
    formData.teamName = props.initialTeam.teamName || ''
    formData.avatarUrl = props.initialTeam.avatarUrl || ''
    formData.description = props.initialTeam.description || ''
    formData.organization = props.initialTeam.organization || ''
    formData.organizationType = props.initialTeam.organizationType || 'SCHOOL'

    if (props.initialTeam.members && props.initialTeam.members.length > 0) {
      const allMembers = props.initialTeam.members

      // 提取领队
      const leaderMembers = allMembers.filter(m => m.role === 'LEADER')
      leaders.value = leaderMembers.map(m => ({
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

      // 提取普通队员及队长 (排序确保队长处于第 0 行)
      const normalMembers = allMembers.filter(m => m.role !== 'LEADER')
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

  // Create 模式或无初始数据：初始化队长行为当前用户
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
    message.warning(t('competition.validation_member_count', {
      min: props.competition?.minTeamMembers ?? 1,
      max: props.competition?.maxTeamMembers ?? 4
    }))
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

function addLeaderRow() {
  const maxLeaders = props.competition?.maxLeaders ?? 1
  if (leaders.value.length >= maxLeaders) {
    message.warning(t('competition.validation_leader_count', { max: maxLeaders }))
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

  // 1. 队员人数校验（含队长）
  if (members.value.length < minMembers || members.value.length > maxMembers) {
    message.error(t('competition.validation_member_count', { min: minMembers, max: maxMembers }))
    return false
  }

  // 2. 队长校验 (首行必须为队长)
  if (members.value[0]?.role !== 'CAPTAIN') {
    message.error(t('competition.validation_captain_count'))
    return false
  }

  // 3. 领队人数校验
  if (leaders.value.length > maxLeaders) {
    message.error(t('competition.validation_leader_count', { max: maxLeaders }))
    return false
  }

  const isEnterprise = formData.organizationType !== 'SCHOOL'
  const studentOrEmpNumberSet = new Set<string>()
  const phoneSet = new Set<string>()
  const emailSet = new Set<string>()
  const idCardSet = new Set<string>()
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

  // 4. 参赛成员必填与唯一性校验
  for (let i = 0; i < members.value.length; i++) {
    const row = members.value[i]
    const rowNum = i + 1

    // 姓名
    const realName = (row.realName || '').trim()
    if (!realName) {
      message.error(t('competition.validation_name_required', { row: rowNum }))
      return false
    }

    // 性别
    if (!row.gender) {
      message.error(`请选择第 ${rowNum} 行成员的性别`)
      return false
    }

    // 学号 / 工号 (同一字段 employeeNumber)
    const emp = (row.employeeNumber || '').trim()
    if (!emp) {
      message.error(isEnterprise
        ? t('competition.validation_employee_number_required', { row: rowNum })
        : t('competition.validation_student_number_required', { row: rowNum }))
      return false
    }
    if (studentOrEmpNumberSet.has(emp)) {
      message.error(isEnterprise
        ? `战队成员中存在重复的工号: ${emp}`
        : t('competition.validation_duplicate_student_number', { studentNumber: emp }))
      return false
    }
    studentOrEmpNumberSet.add(emp)

    // 手机号 (严格校验公众移动号段)
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

    // 邮箱
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

    // 身份证 (严格校验二代身份证格式)
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

    // 学院 / 部门 (同一字段 department)
    const dept = (row.department || '').trim()
    if (!dept) {
      message.error(isEnterprise
        ? t('competition.validation_department_required', { row: rowNum })
        : t('competition.validation_college_required', { row: rowNum }))
      return false
    }

    // 班级 / 职位 (同一字段 position)
    const pos = (row.position || '').trim()
    if (!pos) {
      message.error(isEnterprise
        ? t('competition.validation_position_required', { row: rowNum })
        : t('competition.validation_class_name_required', { row: rowNum }))
      return false
    }
  }

  // 5. 领队校验 (若填写指导老师，姓名和手机号为必填项，其他为选填项)
  const captainEmail = (members.value[0]?.email || '').trim().toLowerCase()

  for (let i = 0; i < leaders.value.length; i++) {
    const l = leaders.value[i]
    const rowNum = i + 1

    // 姓名必填
    const realName = (l.realName || '').trim()
    if (!realName) {
      message.error(`请填写第 ${rowNum} 行领队的姓名`)
      return false
    }

    // 手机号必填且需合规
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

    // 邮箱 (选填)
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

    // 身份证 (选填)
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

      const sanitizedLeaders: TeamLeadersSignupDTO[] = leaders.value.map(l => ({
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
          avatarUrl: formData.avatarUrl || undefined,
          description: formData.description?.trim() || undefined,
          organization: formData.organization?.trim() || undefined,
          organizationType: formData.organizationType,
          members: sanitizedMembers,
          leaders: sanitizedLeaders
        }
        await updateMyTeamApi(props.competition!.id, updatePayload)
        message.success('战队信息已更新并重新提交审核')
        isEditing.value = false
      } else {
        const signupPayload: TeamSignupRequestDTO = {
          teamName: formData.teamName.trim(),
          avatarUrl: formData.avatarUrl || undefined,
          description: formData.description?.trim() || undefined,
          organization: formData.organization?.trim() || undefined,
          organizationType: formData.organizationType,
          members: sanitizedMembers,
          leaders: sanitizedLeaders
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
  .team-meta-container {
    display: grid;
    grid-template-columns: 1fr 1px 1fr;
    gap: 20px;
    align-items: stretch;
    margin-bottom: 16px;

    @media (max-width: 860px) {
      grid-template-columns: 1fr;

      .meta-divider {
        display: none;
      }
    }

    .meta-left-panel {
      min-width: 0;
      width: 100%;
    }

    .meta-divider {
      background-color: var(--v-border-base, #e5e4e7);
      width: 1px;
      height: 100%;
      min-height: 160px;
    }

    .meta-right-panel {
      min-width: 0;
      width: 100%;
    }
  }

  .team-meta-inputs {
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: 6px;

    :deep(.n-form-item) {
      margin-bottom: 0;

      .n-form-item-feedback-wrapper {
        min-height: 0;
        line-height: 1.2;
        padding-top: 2px;
        font-size: 11px;
      }

      .n-form-item-label {
        padding-bottom: 2px;
        font-size: 13px;
        color: #444444;
      }
    }

    .team-meta-row-1 {
      display: flex;
      align-items: center;
      gap: 14px;
      width: 100%;

      .team-avatar-wrapper {
        flex-shrink: 0;
        display: flex;
        align-items: center;
        justify-content: center;
      }

      .team-name-item {
        flex: 1;
        width: 100%;
        min-width: 0;
        margin-bottom: 0;
      }
    }

    .team-meta-row-2 {
      display: grid;
      grid-template-columns: 1fr 130px;
      gap: 12px;
      width: 100%;

      @media (max-width: 640px) {
        grid-template-columns: 1fr;
      }

      .unit-input-item,
      .unit-type-item {
        width: 100%;
        margin-bottom: 0;
      }
    }

    .team-meta-row-3 {
      width: 100%;

      .desc-item {
        width: 100%;
        margin-bottom: 0;
      }
    }
  }

  .requirements-card {
    background: var(--v-bg-card-alt, #fafafc);
    border: 1px solid var(--v-border-base, #e5e4e7);
    border-radius: 8px;
    padding: 12px 14px;
    height: 100%;
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
    gap: 8px;

    .requirements-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding-bottom: 8px;
      border-bottom: 1px solid var(--v-border-base, #e5e4e7);

      .req-title {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 13px;
        font-weight: 600;
        color: var(--v-text-primary, #111111);

        .req-icon {
          font-size: 16px;
          color: #444444;
        }
      }
    }

    .requirements-body {
      display: flex;
      flex-direction: column;
      gap: 8px;
      flex: 1;

      .system-req-list {
        display: flex;
        flex-direction: column;
        gap: 5px;

        .req-line {
          display: flex;
          align-items: baseline;
          font-size: 12px;
          line-height: 1.4;

          .line-label {
            flex-shrink: 0;
            width: 65px;
            color: #444444;
            font-weight: 500;
          }

          .line-val {
            color: var(--v-text-primary, #111111);
            font-family: var(--v-font-mono, inherit);
            word-break: break-all;

            strong {
              color: var(--v-text-primary, #111111);
              font-weight: 600;
            }
          }
        }
      }

      .custom-req-wrapper {
        margin-top: 2px;
        padding-top: 6px;
        border-top: 1px dashed var(--v-border-base, #e5e4e7);
        display: flex;
        flex-direction: column;
        gap: 4px;

        .custom-req-header {
          font-size: 12px;
          font-weight: 600;
          color: #444444;
        }

        .custom-req-content {
          font-size: 12px;
          line-height: 1.5;
          color: #333333;
          max-height: 60px;
          overflow-y: auto;
          white-space: pre-wrap;
          word-break: break-word;
          padding: 4px 6px;
          background: rgba(0, 0, 0, 0.02);
          border-radius: 4px;

          .custom-empty-tip {
            color: #888888;
            font-style: italic;
          }
        }
      }
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
    margin-bottom: 16px;
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

      .role-text {
        font-size: 13px;
        display: inline-block;

        &.role-captain {
          font-weight: 600;
          color: var(--v-text-primary, #111111);
        }

        &.role-member {
          font-weight: 400;
          color: #555555;
        }

        &.role-leader {
          font-weight: 500;
          color: #444444;
        }
      }

      .row-actions-cell {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 6px;
      }

      tr.captain-row {
        background-color: rgba(0, 0, 0, 0.015);
      }
    }
  }

  .leader-toggle-link-wrapper {
    margin: 4px 0 16px 0;
    text-align: left;

    .leader-toggle-link {
      display: inline-block;
      font-size: 13px;
      color: #444444;
      cursor: pointer;
      user-select: none;
      transition: color 0.2s ease;

      &:hover {
        color: #000000;
      }
    }
  }

  .leaders-section {
    margin-top: 8px;
    margin-bottom: 16px;

    .leader-roster-header {
      border-bottom-style: dashed;
    }
  }

  .modal-actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
    margin-top: 16px;

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
