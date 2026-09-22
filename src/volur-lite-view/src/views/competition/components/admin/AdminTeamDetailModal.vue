<template>
  <n-modal
    :show="show"
    preset="card"
    class="team-form-modal team-detail-modal"
    style="width: 1060px; max-width: 96vw; border-radius: 12px;"
    :title="isEditing ? `编辑队伍信息 - ${detailTeam?.teamName || ''}` : `参赛队伍详情 - ${detailTeam?.teamName || ''}`"
    :bordered="false"
    @update:show="handleUpdateShow"
  >
    <n-spin :show="loading">
      <div v-if="detailTeam" class="team-detail-content">
        <!-- 历史审核驳回原因展示 (若有) -->
        <div
          v-if="detailTeam.status === 'AUDIT_REJECTED' && detailTeam.auditReason"
          class="reject-alert-banner"
        >
          <n-icon :component="CloseCircleOutline" class="alert-icon" />
          <div class="alert-text">
            <strong>历史审核驳回原因：</strong>{{ detailTeam.auditReason }}
          </div>
        </div>

        <!-- ==================== 1. 查看模式 (View Mode) ==================== -->
        <template v-if="!isEditing">
          <!-- 队伍基础信息展示卡片 -->
          <div class="team-overview-card">
            <div class="team-overview-top">
              <div class="team-avatar-box">
                <UserAvatar
                  :size="52"
                  :src="detailTeamDisplayAvatarUrl"
                  :username="detailTeam.teamName || 'Team'"
                  :editable="false"
                />
              </div>

              <div class="team-main-meta">
                <div class="team-name-line">
                  <h3 class="team-name">{{ detailTeam.teamName }}</h3>
                  <n-button
                    quaternary
                    circle
                    size="tiny"
                    class="edit-name-trigger-btn"
                    title="切换到编辑模式"
                    @click="enterEditMode"
                  >
                    <template #icon>
                      <n-icon :component="CreateOutline" />
                    </template>
                  </n-button>
                </div>

                <div class="team-info-chips">
                  <div class="info-chip">
                    <span class="chip-label">所属单位:</span>
                    <span class="chip-value">{{ detailTeam.organization || '—' }}</span>
                  </div>
                  <span class="meta-sep">|</span>
                  <div class="info-chip">
                    <span class="chip-label">单位类型:</span>
                    <span class="chip-value">{{ getOrganizationTypeLabel(detailTeam.organizationType) }}</span>
                  </div>
                  <span class="meta-sep">|</span>
                  <div class="info-chip">
                    <span class="chip-label">队长:</span>
                    <span class="chip-value font-medium">{{ detailTeam.captainUsername }}</span>
                  </div>
                  <span class="meta-sep">|</span>
                  <div class="info-chip">
                    <span class="chip-label">报名时间:</span>
                    <span class="chip-value">{{ formatDateTime(detailTeam.createdAt) }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 队伍简介 (修正基线对齐) -->
            <div class="team-desc-line">
              <span class="desc-label">队伍简介:</span>
              <span class="desc-content">{{ detailTeam.description || '暂无简介' }}</span>
            </div>
          </div>

          <!-- 战队成员名单 (只读名册) -->
          <div class="roster-header">
            <div class="roster-title-area">
              <h4 class="roster-title">参赛成员</h4>
              <span class="roster-count">
                当前成员: <strong>{{ detailMembersOnly.length }}</strong> /
                限制: {{ compMinMembers }} ~ {{ compMaxMembers }} 人
                <span class="total-limit-text">（上限: {{ compMaxMembers }} 人）</span>
              </span>
            </div>
          </div>

          <div class="table-scroll-container">
            <n-table :bordered="true" :single-line="false" size="small" class="roster-table">
              <thead>
                <tr>
                  <th style="width: 140px;">姓名 / 账号</th>
                  <th style="width: 75px; text-align: center;">性别</th>
                  <th style="width: 85px; text-align: center;">角色</th>
                  <th style="width: 130px;">
                    {{ isSchoolOrganization ? '学号' : '工号' }}
                  </th>
                  <th style="width: 135px;">手机号</th>
                  <th style="width: 165px;">邮箱</th>
                  <th style="width: 175px;">身份证</th>
                  <th style="width: 140px;">
                    {{ isSchoolOrganization ? '学院' : '部门' }}
                  </th>
                  <th style="width: 120px;">
                    {{ isSchoolOrganization ? '班级' : '职位' }}
                  </th>
                  <th style="width: 95px; text-align: center;">加入状态</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="(row, idx) in detailMembersOnly"
                  :key="idx"
                  :class="{ 'captain-row': row.role === 'CAPTAIN' }"
                >
                  <!-- 姓名 / 账号 -->
                  <td>
                    <div class="member-profile-cell">
                      <AppAvatar
                        round
                        :size="24"
                        :src="row.avatarUrl"
                        :name="row.realName || row.username"
                      />
                      <div class="member-name-block">
                        <span class="real-name">{{ row.realName || row.username }}</span>
                        <span v-if="row.realName && row.username" class="user-handle">@{{ row.username }}</span>
                      </div>
                    </div>
                  </td>

                  <!-- 性别 -->
                  <td style="text-align: center;">
                    <span v-if="row.gender && row.gender !== 'UNKNOWN'" class="gender-text">
                      {{ row.gender === 'MALE' ? '男' : '女' }}
                    </span>
                    <span v-else class="text-muted">—</span>
                  </td>

                  <!-- 角色 -->
                  <td style="text-align: center;">
                    <span
                      class="role-text"
                      :class="row.role === 'CAPTAIN' ? 'role-captain' : 'role-member'"
                    >
                      {{ row.role === 'CAPTAIN' ? '队长' : '队员' }}
                    </span>
                  </td>

                  <!-- 学号 / 工号 -->
                  <td>
                    <span class="code-text">{{ row.employeeNumber || '—' }}</span>
                  </td>

                  <!-- 手机号 -->
                  <td>
                    <span class="code-text">{{ row.phone || '—' }}</span>
                  </td>

                  <!-- 邮箱 -->
                  <td>
                    <span class="email-text" :title="row.email">{{ row.email || '—' }}</span>
                  </td>

                  <!-- 身份证 -->
                  <td>
                    <span v-if="row.idCard" class="id-card-badge">{{ row.idCard }}</span>
                    <span v-else class="text-muted">未填写</span>
                  </td>

                  <!-- 学院 / 部门 -->
                  <td>
                    <span class="college-text" :title="row.department">{{ row.department || '—' }}</span>
                  </td>

                  <!-- 班级 / 职位 -->
                  <td>
                    <span class="college-text" :title="row.position">{{ row.position || '—' }}</span>
                  </td>

                  <!-- 加入状态 -->
                  <td style="text-align: center;">
                    <n-tag
                      size="small"
                      :type="getMemberStatusTag(row).type"
                      round
                    >
                      {{ getMemberStatusTag(row).label }}
                    </n-tag>
                  </td>
                </tr>
              </tbody>
            </n-table>
          </div>

          <!-- 领队/指导老师名册 (只读) -->
          <div v-if="detailLeadersOnly.length > 0" class="leaders-section">
            <div class="roster-header leader-roster-header">
              <div class="roster-title-area">
                <h4 class="roster-title">领队/指导老师</h4>
                <span class="roster-count">
                  当前领队: <strong>{{ detailLeadersOnly.length }}</strong> /
                  上限: {{ compMaxLeaders }} 人
                </span>
              </div>
            </div>

            <div class="table-scroll-container">
              <n-table :bordered="true" :single-line="false" size="small" class="roster-table">
                <thead>
                  <tr>
                    <th style="width: 140px;">姓名 / 账号</th>
                    <th style="width: 75px; text-align: center;">性别</th>
                    <th style="width: 85px; text-align: center;">角色</th>
                    <th style="width: 130px;">
                      {{ isSchoolOrganization ? '学号' : '工号' }}
                    </th>
                    <th style="width: 135px;">手机号</th>
                    <th style="width: 165px;">邮箱</th>
                    <th style="width: 175px;">身份证</th>
                    <th style="width: 140px;">
                      {{ isSchoolOrganization ? '学院' : '部门' }}
                    </th>
                    <th style="width: 120px;">
                      {{ isSchoolOrganization ? '班级' : '职位' }}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(row, idx) in detailLeadersOnly" :key="'leader-' + idx">
                    <td>
                      <div class="member-profile-cell">
                        <AppAvatar
                          round
                          :size="24"
                          :src="row.avatarUrl"
                          :name="row.realName || row.username"
                        />
                        <div class="member-name-block">
                          <span class="real-name">{{ row.realName || row.username }}</span>
                          <span v-if="row.realName && row.username" class="user-handle">@{{ row.username }}</span>
                        </div>
                      </div>
                    </td>
                    <td style="text-align: center;">
                      <span v-if="row.gender && row.gender !== 'UNKNOWN'" class="gender-text">
                        {{ row.gender === 'MALE' ? '男' : '女' }}
                      </span>
                      <span v-else class="text-muted">—</span>
                    </td>
                    <td style="text-align: center;">
                      <span class="role-text role-leader">指导老师</span>
                    </td>
                    <td>
                      <span class="code-text">{{ row.employeeNumber || '—' }}</span>
                    </td>
                    <td>
                      <span class="code-text">{{ row.phone || '—' }}</span>
                    </td>
                    <td>
                      <span class="email-text" :title="row.email">{{ row.email || '—' }}</span>
                    </td>
                    <td>
                      <span v-if="row.idCard" class="id-card-badge">{{ row.idCard }}</span>
                      <span v-else class="text-muted">未填写</span>
                    </td>
                    <td>
                      <span class="college-text" :title="row.department">{{ row.department || '—' }}</span>
                    </td>
                    <td>
                      <span class="college-text" :title="row.position">{{ row.position || '—' }}</span>
                    </td>
                  </tr>
                </tbody>
              </n-table>
            </div>
          </div>
        </template>

        <!-- ==================== 2. 编辑模式 (Edit Mode) ==================== -->
        <template v-else>
          <!-- 队伍基本信息编辑卡片 -->
          <div class="team-edit-card">
            <div class="edit-card-grid">
              <div class="edit-field-item">
                <span class="edit-field-label required">队伍名称</span>
                <n-input
                  v-model:value="editForm.teamName"
                  placeholder="请输入队伍名称 (2-32 字符)"
                  maxlength="32"
                  size="small"
                  show-count
                />
              </div>

              <div class="edit-field-item">
                <span class="edit-field-label">所属单位</span>
                <n-input
                  v-model:value="editForm.organization"
                  placeholder="请输入学校或企事业单位全称"
                  maxlength="64"
                  size="small"
                />
              </div>

              <div class="edit-field-item">
                <span class="edit-field-label">单位类型</span>
                <n-select
                  v-model:value="editForm.organizationType"
                  :options="organizationTypeOptions"
                  placeholder="请选择单位类型"
                  size="small"
                />
              </div>

              <div class="edit-field-item full-width">
                <span class="edit-field-label">队伍简介</span>
                <n-input
                  v-model:value="editForm.description"
                  type="textarea"
                  placeholder="简要描述队伍口号或特色 (不超过200字符)"
                  :rows="2"
                  maxlength="200"
                  size="small"
                  show-count
                />
              </div>
            </div>
          </div>

          <!-- 参赛成员编辑列表 (支持修改全信息与指定队长) -->
          <div class="roster-header">
            <div class="roster-title-area">
              <h4 class="roster-title">参赛成员</h4>
              <span class="roster-count">
                当前成员: <strong>{{ editMembers.length }}</strong> /
                限制: {{ compMinMembers }} ~ {{ compMaxMembers }} 人
              </span>
            </div>
            <n-button
              size="small"
              secondary
              :disabled="editMembers.length >= compMaxMembers"
              @click="addMemberRow"
            >
              <template #icon>
                <n-icon :component="AddOutline" />
              </template>
              添加队员
            </n-button>
          </div>

          <div class="table-scroll-container">
            <n-table :bordered="true" :single-line="false" size="small" class="roster-table edit-mode-table">
              <thead>
                <tr>
                  <th style="width: 120px;">姓名 <span class="req">*</span></th>
                  <th style="width: 85px; text-align: center;">性别 <span class="req">*</span></th>
                  <th style="width: 85px; text-align: center;">角色</th>
                  <th style="width: 130px;">
                    {{ editForm.organizationType !== 'SCHOOL' ? '工号' : '学号' }}
                  </th>
                  <th style="width: 135px;">手机号</th>
                  <th style="width: 165px;">邮箱</th>
                  <th style="width: 175px;">身份证</th>
                  <th style="width: 130px;">
                    {{ editForm.organizationType !== 'SCHOOL' ? '部门' : '学院' }}
                  </th>
                  <th style="width: 120px;">
                    {{ editForm.organizationType !== 'SCHOOL' ? '职位' : '班级' }}
                  </th>
                  <th style="width: 70px; text-align: center;">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="(row, idx) in editMembers"
                  :key="idx"
                  :class="{ 'captain-row': row.role === 'CAPTAIN' }"
                >
                  <!-- 姓名 -->
                  <td>
                    <n-input
                      v-model:value="row.realName"
                      size="small"
                      placeholder="姓名"
                      maxlength="32"
                    />
                  </td>

                  <!-- 性别 -->
                  <td style="text-align: center;">
                    <n-select
                      v-model:value="row.gender"
                      size="small"
                      :options="genderOptions"
                    />
                  </td>

                  <!-- 角色 (下拉选择队长/队员) -->
                  <td style="text-align: center;">
                    <n-select
                      v-model:value="row.role"
                      size="small"
                      :options="memberRoleOptions"
                      style="width: 85px; margin: 0 auto;"
                    />
                  </td>

                  <!-- 学号 / 工号 -->
                  <td>
                    <n-input
                      v-model:value="row.employeeNumber"
                      size="small"
                      :placeholder="editForm.organizationType !== 'SCHOOL' ? '工号' : '学号'"
                      maxlength="64"
                    />
                  </td>

                  <!-- 手机号 -->
                  <td>
                    <n-input
                      v-model:value="row.phone"
                      size="small"
                      placeholder="手机号"
                      maxlength="20"
                    />
                  </td>

                  <!-- 邮箱 -->
                  <td>
                    <n-input
                      v-model:value="row.email"
                      size="small"
                      placeholder="邮箱"
                      maxlength="128"
                    />
                  </td>

                  <!-- 身份证 -->
                  <td>
                    <n-input
                      v-model:value="row.idCard"
                      size="small"
                      placeholder="身份证"
                      maxlength="32"
                    />
                  </td>

                  <!-- 学院 / 部门 -->
                  <td>
                    <n-input
                      v-model:value="row.department"
                      size="small"
                      :placeholder="editForm.organizationType !== 'SCHOOL' ? '部门' : '学院'"
                      maxlength="64"
                    />
                  </td>

                  <!-- 班级 / 职位 -->
                  <td>
                    <n-input
                      v-model:value="row.position"
                      size="small"
                      :placeholder="editForm.organizationType !== 'SCHOOL' ? '职位' : '班级'"
                      maxlength="64"
                    />
                  </td>

                  <!-- 操作: 删除行 -->
                  <td style="text-align: center;">
                    <n-button
                      size="tiny"
                      quaternary
                      type="error"
                      :disabled="editMembers.length <= 1"
                      title="删除该成员"
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

          <!-- 领队/指导老师编辑列表 -->
          <div class="leaders-section">
            <div class="roster-header leader-roster-header">
              <div class="roster-title-area">
                <h4 class="roster-title">领队/指导老师</h4>
                <span class="roster-count">
                  当前领队: <strong>{{ editLeaders.length }}</strong> /
                  上限: {{ compMaxLeaders }} 人
                </span>
              </div>
              <n-button
                size="small"
                secondary
                :disabled="editLeaders.length >= compMaxLeaders"
                @click="addLeaderRow"
              >
                <template #icon>
                  <n-icon :component="AddOutline" />
                </template>
                添加领队
              </n-button>
            </div>

            <div v-if="editLeaders.length > 0" class="table-scroll-container">
              <n-table :bordered="true" :single-line="false" size="small" class="roster-table edit-mode-table">
                <thead>
                  <tr>
                    <th style="width: 120px;">姓名 <span class="req">*</span></th>
                    <th style="width: 85px; text-align: center;">性别</th>
                    <th style="width: 85px; text-align: center;">角色</th>
                    <th style="width: 130px;">
                      {{ editForm.organizationType !== 'SCHOOL' ? '工号' : '学号' }}
                    </th>
                    <th style="width: 135px;">手机号</th>
                    <th style="width: 165px;">邮箱</th>
                    <th style="width: 175px;">身份证</th>
                    <th style="width: 130px;">
                      {{ editForm.organizationType !== 'SCHOOL' ? '部门' : '学院' }}
                    </th>
                    <th style="width: 120px;">
                      {{ editForm.organizationType !== 'SCHOOL' ? '职位' : '班级' }}
                    </th>
                    <th style="width: 65px; text-align: center;">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(row, idx) in editLeaders" :key="'leader-' + idx">
                    <td>
                      <n-input v-model:value="row.realName" size="small" placeholder="姓名" maxlength="32" />
                    </td>
                    <td style="text-align: center;">
                      <n-select v-model:value="row.gender" size="small" :options="genderOptions" />
                    </td>
                    <td style="text-align: center;">
                      <span class="role-text role-leader">指导老师</span>
                    </td>
                    <td>
                      <n-input v-model:value="row.employeeNumber" size="small" placeholder="工号/学号" maxlength="64" />
                    </td>
                    <td>
                      <n-input v-model:value="row.phone" size="small" placeholder="手机号" maxlength="20" />
                    </td>
                    <td>
                      <n-input v-model:value="row.email" size="small" placeholder="邮箱" maxlength="128" />
                    </td>
                    <td>
                      <n-input v-model:value="row.idCard" size="small" placeholder="身份证" maxlength="32" />
                    </td>
                    <td>
                      <n-input v-model:value="row.department" size="small" placeholder="部门/学院" maxlength="64" />
                    </td>
                    <td>
                      <n-input v-model:value="row.position" size="small" placeholder="职位/班级" maxlength="64" />
                    </td>
                    <td style="text-align: center;">
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
        </template>

        <!-- 3. 底部操作栏 (Modal Actions) -->
        <div class="modal-actions">
          <div class="actions-left">
            <template v-if="!isEditing">
              <span class="status-indicator">
                当前审核状态：
                <n-tag size="small" :type="getTeamStatusTag(detailTeam.status)" round>
                  {{ getTeamStatusLabel(detailTeam.status) }}
                </n-tag>
                <span class="meta-divider">|</span>
                <span>队长：<strong>{{ detailTeam.captainUsername }}</strong></span>
                <span class="meta-divider">|</span>
                <span>报名时间：{{ formatDateTime(detailTeam.createdAt) }}</span>
              </span>
            </template>
            <template v-else>
              <span class="edit-mode-hint">
                管理员编辑模式：可修改全量队伍资料、参赛队员信息及指定队长
              </span>
            </template>
          </div>

          <div class="actions-right">
            <template v-if="!isEditing">
              <n-button @click="handleClose">关闭</n-button>
              <n-button
                v-if="detailTeam.status !== 'AUDIT_REJECTED'"
                type="error"
                secondary
                @click="handleReject"
              >
                驳回报名
              </n-button>
              <n-button
                v-if="detailTeam.status !== 'AUDIT_PASSED'"
                class="theme-primary-btn"
                :loading="approving"
                @click="handleApprove"
              >
                通过资质审核
              </n-button>
              <n-button
                class="theme-primary-btn"
                @click="enterEditMode"
              >
                <template #icon>
                  <n-icon :component="CreateOutline" />
                </template>
                编辑队伍
              </n-button>
            </template>

            <template v-else>
              <n-button :disabled="savingAll" @click="cancelEditMode">
                取消
              </n-button>
              <n-button
                type="primary"
                class="theme-primary-btn"
                :loading="savingAll"
                @click="handleSaveAll"
              >
                保存修改
              </n-button>
            </template>
          </div>
        </div>
      </div>
    </n-spin>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import {
  NModal,
  NSpin,
  NButton,
  NIcon,
  NInput,
  NSelect,
  NTag,
  NTable,
  useMessage,
  type SelectOption
} from 'naive-ui'
import {
  CloseCircleOutline,
  CreateOutline,
  AddOutline,
  TrashOutline
} from '@vicons/ionicons5'
import AppAvatar from '@/components/common/AppAvatar.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import { formatAvatarUrl } from '@/utils/imageCache'
import {
  getAdminTeamDetailApi,
  auditAdminTeamApi,
  updateAdminTeamApi
} from '@/api/competition'
import type {
  AdminTeamDetailVO,
  CompetitionMemberVO,
  OrganizationType,
  TeamStatus,
  MemberRole
} from '@/types/competition'

interface Props {
  show: boolean
  compId: number | string
  teamId?: number | string | null
  compMinMembers?: number
  compMaxMembers?: number
  compMaxLeaders?: number
}

const props = withDefaults(defineProps<Props>(), {
  teamId: null,
  compMinMembers: 1,
  compMaxMembers: 5,
  compMaxLeaders: 2
})

const emit = defineEmits<{
  (e: 'update:show', val: boolean): void
  (e: 'approved', teamId: number | string): void
  (e: 'reject', team: AdminTeamDetailVO): void
  (e: 'updated', team: AdminTeamDetailVO): void
  (e: 'audit-success'): void
  (e: 'update-success'): void
}>()

const message = useMessage()

const loading = ref(false)
const approving = ref(false)
const detailTeam = ref<AdminTeamDetailVO | null>(null)

// 编辑模式状态
const isEditing = ref(false)
const savingAll = ref(false)

const editForm = reactive<{
  teamName: string
  organization: string
  organizationType: OrganizationType
  description: string
}>({
  teamName: '',
  organization: '',
  organizationType: 'SCHOOL',
  description: ''
})

interface EditableMemberRow {
  id?: number | string
  realName: string
  gender: 'MALE' | 'FEMALE'
  role: MemberRole
  phone: string
  email: string
  idCard: string
  employeeNumber: string
  department: string
  position: string
}

const editMembers = ref<EditableMemberRow[]>([])
const editLeaders = ref<EditableMemberRow[]>([])

const detailTeamDisplayAvatarUrl = computed(() => {
  return formatAvatarUrl(detailTeam.value?.avatarUrl)
})

const detailMembersOnly = computed<CompetitionMemberVO[]>(() => {
  if (!detailTeam.value?.members) return []
  return detailTeam.value.members
    .filter(m => m.role !== 'LEADER')
    .sort((a, b) => (a.role === 'CAPTAIN' ? -1 : b.role === 'CAPTAIN' ? 1 : 0))
})

const detailLeadersOnly = computed<CompetitionMemberVO[]>(() => {
  if (!detailTeam.value?.members) return []
  return detailTeam.value.members.filter(m => m.role === 'LEADER')
})

const organizationTypeMap: Record<string, string> = {
  SCHOOL: '学校',
  ENTERPRISE: '企业单位',
  MILITARY: '军工',
  ENERGY: '能源',
  FINANCE: '金融',
  RESEARCH: '科研院所',
  OTHER: '其他'
}

function getOrganizationTypeLabel(type?: string | null): string {
  if (!type) return '学校'
  const upper = String(type).trim().toUpperCase()
  return organizationTypeMap[upper] || type
}

const isSchoolOrganization = computed(() => {
  const orgType = detailTeam.value?.organizationType
  if (!orgType) return true
  const upper = String(orgType).trim().toUpperCase()
  return upper === 'SCHOOL' || upper === '学校'
})

const organizationTypeOptions: SelectOption[] = [
  { label: '学校', value: 'SCHOOL' },
  { label: '企业单位', value: 'ENTERPRISE' },
  { label: '军工', value: 'MILITARY' },
  { label: '能源', value: 'ENERGY' },
  { label: '金融', value: 'FINANCE' },
  { label: '科研院所', value: 'RESEARCH' },
  { label: '其他', value: 'OTHER' }
]

const genderOptions: SelectOption[] = [
  { label: '男', value: 'MALE' },
  { label: '女', value: 'FEMALE' }
]

const memberRoleOptions: SelectOption[] = [
  { label: '队长', value: 'CAPTAIN' },
  { label: '队员', value: 'MEMBER' }
]

watch(
  () => [props.show, props.teamId],
  async ([newShow, newTeamId]) => {
    if (newShow && newTeamId) {
      isEditing.value = false
      await fetchDetail()
    } else {
      detailTeam.value = null
      isEditing.value = false
    }
  },
  { immediate: true }
)

async function fetchDetail() {
  if (!props.teamId) return
  loading.value = true
  try {
    const res = await getAdminTeamDetailApi(props.compId, props.teamId)
    detailTeam.value = res.data
  } catch (error: any) {
    message.error(error.message || '获取战队详情失败')
  } finally {
    loading.value = false
  }
}

function handleUpdateShow(val: boolean) {
  emit('update:show', val)
  if (!val) {
    isEditing.value = false
  }
}

function handleClose() {
  emit('update:show', false)
  isEditing.value = false
}

// 切换到编辑模式
function enterEditMode() {
  if (!detailTeam.value) return
  editForm.teamName = detailTeam.value.teamName || ''
  editForm.organization = detailTeam.value.organization || ''
  editForm.organizationType = (detailTeam.value.organizationType as OrganizationType) || 'SCHOOL'
  editForm.description = detailTeam.value.description || ''

  editMembers.value = (detailTeam.value.members || [])
    .filter(m => m.role !== 'LEADER')
    .map(m => ({
      id: m.id,
      realName: m.realName || '',
      gender: m.gender === 'FEMALE' ? 'FEMALE' : 'MALE',
      role: m.role || 'MEMBER',
      phone: m.phone || '',
      email: m.email || '',
      idCard: m.idCard || '',
      employeeNumber: m.employeeNumber || '',
      department: m.department || '',
      position: m.position || ''
    }))

  if (!editMembers.value.some(m => m.role === 'CAPTAIN') && editMembers.value.length > 0) {
    editMembers.value[0].role = 'CAPTAIN'
  }

  editLeaders.value = (detailTeam.value.members || [])
    .filter(m => m.role === 'LEADER')
    .map(m => ({
      id: m.id,
      realName: m.realName || '',
      gender: m.gender === 'FEMALE' ? 'FEMALE' : 'MALE',
      role: 'LEADER',
      phone: m.phone || '',
      email: m.email || '',
      idCard: m.idCard || '',
      employeeNumber: m.employeeNumber || '',
      department: m.department || '',
      position: m.position || ''
    }))

  isEditing.value = true
}

function cancelEditMode() {
  isEditing.value = false
}

function addMemberRow() {
  if (editMembers.value.length >= props.compMaxMembers) return
  editMembers.value.push({
    realName: '',
    gender: 'MALE',
    role: editMembers.value.length === 0 ? 'CAPTAIN' : 'MEMBER',
    phone: '',
    email: '',
    idCard: '',
    employeeNumber: '',
    department: '',
    position: ''
  })
}

function removeMemberRow(idx: number) {
  if (editMembers.value.length <= 1) {
    message.warning('战队至少需要保留 1 名参赛成员')
    return
  }
  const isRemovingCaptain = editMembers.value[idx].role === 'CAPTAIN'
  editMembers.value.splice(idx, 1)
  if (isRemovingCaptain && editMembers.value.length > 0) {
    editMembers.value[0].role = 'CAPTAIN'
  }
}

function addLeaderRow() {
  if (editLeaders.value.length >= props.compMaxLeaders) return
  editLeaders.value.push({
    realName: '',
    gender: 'MALE',
    role: 'LEADER',
    phone: '',
    email: '',
    idCard: '',
    employeeNumber: '',
    department: '',
    position: ''
  })
}

function removeLeaderRow(idx: number) {
  editLeaders.value.splice(idx, 1)
}

// 保存全部修改
async function handleSaveAll() {
  if (!detailTeam.value) return
  const teamName = editForm.teamName.trim()
  if (!teamName || teamName.length < 2 || teamName.length > 32) {
    message.warning('队伍名称长度需在 2 到 32 个字符之间')
    return
  }

  if (editMembers.value.length === 0) {
    message.warning('参赛成员名册不能为空')
    return
  }

  // 校验是否且仅有唯一一个队长角色的成员
  const captainCount = editMembers.value.filter(m => m.role === 'CAPTAIN').length
  if (captainCount === 0) {
    message.warning('参赛队伍必须指定 1 名队长，请在成员角色列表中选择一名队长')
    return
  }
  if (captainCount > 1) {
    message.warning(`参赛队伍只能且必须包含 1 名队长，当前指定了 ${captainCount} 名队长，请调整设置`)
    return
  }

  for (let i = 0; i < editMembers.value.length; i++) {
    const m = editMembers.value[i]
    if (!m.realName?.trim()) {
      message.warning(`请填写第 ${i + 1} 位成员的姓名`)
      return
    }
  }

  savingAll.value = true
  try {
    await updateAdminTeamApi(props.compId, detailTeam.value.id, {
      teamName,
      organization: editForm.organization.trim() || undefined,
      organizationType: editForm.organizationType,
      description: editForm.description.trim() || undefined,
      members: editMembers.value.map(m => ({
        id: m.id,
        realName: m.realName?.trim(),
        gender: m.gender,
        role: m.role,
        phone: m.phone?.trim() || undefined,
        email: m.email?.trim() || undefined,
        idCard: m.idCard?.trim() || undefined,
        employeeNumber: m.employeeNumber?.trim() || undefined,
        department: m.department?.trim() || undefined,
        position: m.position?.trim() || undefined
      })),
      leaders: editLeaders.value.map(l => ({
        id: l.id,
        realName: l.realName?.trim(),
        gender: l.gender,
        role: 'LEADER',
        phone: l.phone?.trim() || undefined,
        email: l.email?.trim() || undefined,
        idCard: l.idCard?.trim() || undefined,
        employeeNumber: l.employeeNumber?.trim() || undefined,
        department: l.department?.trim() || undefined,
        position: l.position?.trim() || undefined
      }))
    })

    message.success('战队资料及成员信息已成功保存')
    isEditing.value = false
    await fetchDetail()
    if (detailTeam.value) {
      emit('updated', detailTeam.value)
    }
    emit('update-success')
  } catch (error: any) {
    message.error(error.message || '保存队伍信息失败')
  } finally {
    savingAll.value = false
  }
}

function handleReject() {
  if (!detailTeam.value) return
  emit('reject', detailTeam.value)
}

async function handleApprove() {
  if (!detailTeam.value) return
  approving.value = true
  try {
    await auditAdminTeamApi(props.compId, detailTeam.value.id, {
      status: 'AUDIT_PASSED'
    })
    message.success(`战队「${detailTeam.value.teamName}」资质审核已通过`)
    detailTeam.value.status = 'AUDIT_PASSED'
    emit('approved', detailTeam.value.id)
    emit('audit-success')
  } catch (error: any) {
    message.error(error.message || '审核通过操作失败')
  } finally {
    approving.value = false
  }
}

function getMemberStatusTag(row: CompetitionMemberVO) {
  switch (row.status) {
    case 'INVITED_ACCEPTED':
      return { type: 'success' as const, label: '已加入' }
    case 'INVITED':
      return { type: 'warning' as const, label: '待同意邀请' }
    case 'NOT_PARTICIPATE':
      return { type: 'default' as const, label: '已放弃/未参与' }
    default:
      return { type: 'info' as const, label: row.status || '—' }
  }
}

function getTeamStatusTag(status?: TeamStatus): 'default' | 'error' | 'primary' | 'info' | 'success' | 'warning' {
  switch (status) {
    case 'AUDIT_PASSED':
      return 'success'
    case 'AUDIT_REJECTED':
      return 'error'
    case 'AUDIT_DRAFT':
    case 'PENDING_AUDIT':
      return 'warning'
    default:
      return 'default'
  }
}

function getTeamStatusLabel(status?: TeamStatus): string {
  switch (status) {
    case 'AUDIT_PASSED':
      return '资质已通过'
    case 'AUDIT_REJECTED':
      return '已被驳回'
    case 'AUDIT_DRAFT':
    case 'PENDING_AUDIT':
      return '待审核'
    default:
      return '未知状态'
  }
}

function formatDateTime(val?: string) {
  if (!val) return '—'
  return new Date(val).toLocaleString()
}
</script>

<style lang="scss" scoped>
.team-detail-content {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .reject-alert-banner {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 14px;
    background: #fef2f2;
    border: 1px solid #fecaca;
    border-radius: 6px;
    color: #dc2626;
    font-size: 13px;

    .alert-icon {
      font-size: 16px;
      flex-shrink: 0;
    }

    .alert-text {
      flex: 1;
      line-height: 1.4;
    }
  }

  // 基础信息卡片 (查看模式)
  .team-overview-card {
    background: #f8f9fa;
    border: 1px solid #e9ecef;
    border-radius: 8px;
    padding: 16px;
    display: flex;
    flex-direction: column;
    gap: 12px;

    .team-overview-top {
      display: flex;
      align-items: center;
      gap: 14px;

      .team-avatar-box {
        flex-shrink: 0;
      }

      .team-main-meta {
        flex: 1;
        min-width: 0;

        .team-name-line {
          display: flex;
          align-items: center;
          gap: 6px;

          .team-name {
            margin: 0;
            font-size: 18px;
            font-weight: 700;
            color: var(--v-text-primary, #111111);
          }

          .edit-name-trigger-btn {
            color: #666;
            &:hover {
              color: #000;
            }
          }
        }

        .team-info-chips {
          display: flex;
          align-items: center;
          gap: 8px;
          margin-top: 6px;
          font-size: 12px;
          color: #666666;
          flex-wrap: wrap;

          .info-chip {
            display: inline-flex;
            align-items: center;
            gap: 4px;

            .chip-label {
              color: #888888;
            }

            .chip-value {
              color: var(--v-text-primary, #111111);

              &.font-medium {
                font-weight: 500;
              }
            }
          }

          .meta-sep {
            color: #d1d5db;
          }
        }
      }
    }

    // 队伍简介行 (精确基线对齐)
    .team-desc-line {
      display: flex;
      align-items: baseline;
      gap: 8px;
      font-size: 13px;
      line-height: 1.5;
      padding-top: 10px;
      border-top: 1px dashed var(--v-border-base, #e5e4e7);

      .desc-label {
        color: #666666;
        font-weight: 500;
        line-height: 1.5;
        flex-shrink: 0;
      }

      .desc-content {
        color: var(--v-text-primary, #333333);
        line-height: 1.5;
        flex: 1;
        word-break: break-word;
      }
    }
  }

  // 编辑模式卡片 (基本资料表单)
  .team-edit-card {
    background: var(--v-bg-card-alt, #fafafc);
    border: 1px solid var(--v-border-base, #e5e4e7);
    border-radius: 8px;
    padding: 16px;

    .edit-card-grid {
      display: grid;
      grid-template-columns: 1fr 1fr 1fr;
      gap: 12px;

      @media (max-width: 768px) {
        grid-template-columns: 1fr;
      }

      .edit-field-item {
        display: flex;
        flex-direction: column;
        gap: 6px;

        &.full-width {
          grid-column: 1 / -1;
        }

        .edit-field-label {
          font-size: 13px;
          font-weight: 500;
          color: #444444;

          &.required::after {
            content: ' *';
            color: #d03050;
          }
        }
      }
    }
  }

  // 名册头部
  .roster-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 6px;

    .roster-title-area {
      display: flex;
      align-items: baseline;
      gap: 8px;

      .roster-title {
        margin: 0;
        font-size: 15px;
        font-weight: 600;
        color: var(--v-text-primary, #111111);
      }

      .roster-count {
        font-size: 12px;
        color: #666666;

        strong {
          color: var(--v-text-primary, #111111);
        }

        .total-limit-text {
          color: #888888;
          margin-left: 4px;
        }
      }
    }
  }

  // 名册滚动表格
  .table-scroll-container {
    overflow-x: auto;
    border: 1px solid var(--v-border-base, #e5e4e7);
    border-radius: 6px;

    .roster-table {
      min-width: 1100px;

      th {
        font-weight: 600;
        background: #f8f9fa;
        color: #444444;
        white-space: nowrap;
        padding: 8px 10px;

        .req {
          color: #d03050;
        }
      }

      td {
        padding: 6px 8px;
        vertical-align: middle;
      }

      &.edit-mode-table {
        td {
          padding: 4px 6px;
        }
      }

      .role-text {
        font-size: 13px;

        &.role-captain {
          font-weight: 600;
          color: var(--v-text-primary, #111111);
        }

        &.role-member {
          color: #666666;
        }

        &.role-leader {
          font-weight: 500;
          color: #444444;
        }
      }

      tr.captain-row {
        background-color: rgba(0, 0, 0, 0.015);
      }
    }
  }

  .leaders-section {
    display: flex;
    flex-direction: column;
    gap: 10px;

    .leader-roster-header {
      border-bottom: 1px dashed var(--v-border-base, #e5e4e7);
      padding-bottom: 6px;
    }
  }

  .member-profile-cell {
    display: flex;
    align-items: center;
    gap: 8px;

    .member-name-block {
      display: flex;
      flex-direction: column;
      line-height: 1.2;

      .real-name {
        font-weight: 500;
        font-size: 13px;
        color: var(--v-text-primary, #111111);
      }

      .user-handle {
        font-size: 11px;
        color: #888888;
      }
    }
  }

  .code-text {
    font-family: var(--v-font-mono, monospace);
    font-size: 12px;
  }

  .email-text {
    font-size: 12px;
    color: #444444;
    word-break: break-all;
  }

  .id-card-badge {
    font-family: var(--v-font-mono, monospace);
    font-size: 12px;
    background: rgba(0, 0, 0, 0.04);
    padding: 2px 4px;
    border-radius: 4px;
  }

  .gender-text,
  .college-text {
    font-size: 12px;
    color: #444444;
  }

  .text-muted {
    color: #999999;
    font-size: 12px;
  }

  // 底部操作区
  .modal-actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
    margin-top: 8px;
    padding-top: 12px;
    border-top: 1px solid var(--v-border-base, #e5e4e7);

    .actions-left {
      display: flex;
      align-items: center;

      .status-indicator {
        font-size: 13px;
        color: #444444;
        display: flex;
        align-items: center;
        gap: 6px;
      }

      .edit-mode-hint {
        font-size: 12px;
        color: #666666;
      }

      .meta-divider {
        color: #d1d5db;
        margin: 0 4px;
      }
    }

    .actions-right {
      display: flex;
      align-items: center;
      gap: 10px;
    }
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

// 暗色主题适配
html.dark,
[data-theme='dark'] {
  .team-detail-content {
    .reject-alert-banner {
      background: rgba(239, 68, 68, 0.15);
      border-color: rgba(239, 68, 68, 0.3);
      color: #fca5a5;
    }

    .team-overview-card {
      background: var(--v-bg-muted, #12141c);
      border-color: var(--v-border-base, rgba(255, 255, 255, 0.09));

      .team-overview-top {
        .team-main-meta {
          .team-name-line {
            .team-name {
              color: var(--v-text-primary, #f8fafc);
            }

            .edit-name-trigger-btn {
              color: #94a3b8;
              &:hover {
                color: #f8fafc;
              }
            }
          }

          .team-info-chips {
            color: #94a3b8;

            .info-chip {
              .chip-label {
                color: #64748b;
              }

              .chip-value {
                color: var(--v-text-primary, #f8fafc);
              }
            }

            .meta-sep {
              color: rgba(255, 255, 255, 0.2);
            }
          }
        }
      }

      .team-desc-line {
        border-top-color: rgba(255, 255, 255, 0.09);

        .desc-label {
          color: #94a3b8;
        }

        .desc-content {
          color: #cbd5e1;
        }
      }
    }

    .team-edit-card {
      background: var(--v-bg-muted, #12141c);
      border-color: var(--v-border-base, rgba(255, 255, 255, 0.09));

      .edit-card-grid .edit-field-item .edit-field-label {
        color: #94a3b8;
      }
    }

    .roster-header {
      .roster-title-area {
        .roster-title {
          color: var(--v-text-primary, #f8fafc);
        }

        .roster-count {
          color: #94a3b8;

          strong {
            color: var(--v-text-primary, #f8fafc);
          }

          .total-limit-text {
            color: #64748b;
          }
        }
      }
    }

    .table-scroll-container {
      border-color: var(--v-border-base, rgba(255, 255, 255, 0.09));

      .roster-table {
        th {
          background: var(--v-bg-muted, #12141c);
          color: #94a3b8;
        }

        tr.captain-row {
          background-color: rgba(255, 255, 255, 0.03);
        }

        .role-text {
          &.role-captain {
            color: #f8fafc;
          }

          &.role-member {
            color: #94a3b8;
          }

          &.role-leader {
            color: #cbd5e1;
          }
        }
      }
    }

    .member-profile-cell .member-name-block {
      .real-name {
        color: #f8fafc;
      }
      .user-handle {
        color: #64748b;
      }
    }

    .email-text,
    .gender-text,
    .college-text {
      color: #94a3b8;
    }

    .id-card-badge {
      background: rgba(255, 255, 255, 0.06);
      color: #f8fafc;
    }

    .modal-actions {
      border-top-color: var(--v-border-base, rgba(255, 255, 255, 0.09));

      .actions-left {
        .status-indicator {
          color: #94a3b8;
        }
        .edit-mode-hint {
          color: #94a3b8;
        }
        .meta-divider {
          color: rgba(255, 255, 255, 0.2);
        }
      }
    }

    .theme-primary-btn {
      background-color: var(--v-btn-primary-bg, #ffffff) !important;
      color: var(--v-btn-primary-text, #0b0c10) !important;

      &:hover {
        background-color: var(--v-btn-primary-hover, #e2e8f0) !important;
      }
    }
  }
}
</style>
