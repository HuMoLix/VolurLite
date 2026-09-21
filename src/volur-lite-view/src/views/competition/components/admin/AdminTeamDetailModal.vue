<template>
  <n-modal
    :show="show"
    preset="card"
    class="team-form-modal team-detail-modal"
    style="width: 1040px; max-width: 96vw; border-radius: 12px;"
    :title="`参赛队伍详情 - ${detailTeam?.teamName || ''}`"
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

        <!-- Top Team Overview Card: 队伍基础信息展示卡片 -->
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
                <!-- 查看模式 -->
                <template v-if="!isEditingName">
                  <h3 class="team-name">{{ detailTeam.teamName }}</h3>
                  <n-button
                    quaternary
                    circle
                    size="tiny"
                    class="edit-name-trigger-btn"
                    title="管理员修改队伍基本信息"
                    @click="startEditName"
                  >
                    <template #icon>
                      <n-icon :component="CreateOutline" />
                    </template>
                  </n-button>
                </template>

                <!-- 快速编辑队伍名称模式 -->
                <template v-else>
                  <n-input
                    ref="nameInputRef"
                    v-model:value="editNameForm.teamName"
                    placeholder="输入新队伍名称 (2-32字符)"
                    size="small"
                    style="width: 260px;"
                    @keyup.enter="handleSaveTeamName"
                  />
                  <n-space :size="6" align="center" style="margin-left: 8px;">
                    <n-button
                      type="primary"
                      size="tiny"
                      class="theme-primary-btn"
                      :loading="savingName"
                      :disabled="!editNameForm.teamName.trim()"
                      @click="handleSaveTeamName"
                    >
                      保存
                    </n-button>
                    <n-button
                      quaternary
                      size="tiny"
                      :disabled="savingName"
                      @click="cancelEditName"
                    >
                      取消
                    </n-button>
                  </n-space>
                </template>
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

          <!-- 队伍简介 -->
          <div class="team-desc-line">
            <span class="desc-label">队伍简介:</span>
            <span class="desc-content">{{ detailTeam.description || '暂无简介' }}</span>
          </div>
        </div>

        <!-- 2. 战队成员名单 (Roster Header & Table) -->
        <div class="roster-header">
          <div class="roster-title-area">
            <h4 class="roster-title">参赛成员</h4>
            <span class="roster-count">
              当前成员: <strong>{{ detailMembersOnly.length }}</strong> /
              限制: {{ compMinMembers }} ~ {{ compMaxMembers }} 人
              <span class="total-limit-text">
                （上限: {{ compMaxMembers }} 人）
              </span>
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
                <!-- 1. 姓名 / 账号 -->
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

                <!-- 2. 性别 -->
                <td style="text-align: center;">
                  <span v-if="row.gender && row.gender !== 'UNKNOWN'" class="gender-text">
                    {{ row.gender === 'MALE' ? '男' : '女' }}
                  </span>
                  <span v-else class="text-muted">—</span>
                </td>

                <!-- 3. 角色 -->
                <td style="text-align: center;">
                  <span
                    class="role-text"
                    :class="row.role === 'CAPTAIN' ? 'role-captain' : 'role-member'"
                  >
                    {{ row.role === 'CAPTAIN' ? '队长' : '队员' }}
                  </span>
                </td>

                <!-- 4. 学号 / 工号 -->
                <td>
                  <span class="code-text">{{ row.employeeNumber || '—' }}</span>
                </td>

                <!-- 5. 手机号 -->
                <td>
                  <span class="code-text">{{ row.phone || '—' }}</span>
                </td>

                <!-- 6. 邮箱 -->
                <td>
                  <span class="email-text" :title="row.email">{{ row.email || '—' }}</span>
                </td>

                <!-- 7. 身份证 -->
                <td>
                  <span v-if="row.idCard" class="id-card-badge">{{ row.idCard }}</span>
                  <span v-else class="text-muted">未填写</span>
                </td>

                <!-- 8. 学院 / 部门 -->
                <td>
                  <span class="college-text" :title="row.department">{{ row.department || '—' }}</span>
                </td>

                <!-- 9. 班级 / 职位 -->
                <td>
                  <span class="college-text" :title="row.position">{{ row.position || '—' }}</span>
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
              </tr>
            </tbody>
          </n-table>
        </div>

        <!-- 领队/指导老师名册 (当存在领队时展示) -->
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
                  <!-- 1. 姓名 / 账号 -->
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

                  <!-- 2. 性别 -->
                  <td style="text-align: center;">
                    <span v-if="row.gender && row.gender !== 'UNKNOWN'" class="gender-text">
                      {{ row.gender === 'MALE' ? '男' : '女' }}
                    </span>
                    <span v-else class="text-muted">—</span>
                  </td>

                  <!-- 3. 角色 -->
                  <td style="text-align: center;">
                    <span class="role-text role-leader">
                      指导老师
                    </span>
                  </td>

                  <!-- 4. 学号 / 工号 -->
                  <td>
                    <span class="code-text">{{ row.employeeNumber || '—' }}</span>
                  </td>

                  <!-- 5. 手机号 -->
                  <td>
                    <span class="code-text">{{ row.phone || '—' }}</span>
                  </td>

                  <!-- 6. 邮箱 -->
                  <td>
                    <span class="email-text" :title="row.email">{{ row.email || '—' }}</span>
                  </td>

                  <!-- 7. 身份证 -->
                  <td>
                    <span v-if="row.idCard" class="id-card-badge">{{ row.idCard }}</span>
                    <span v-else class="text-muted">未填写</span>
                  </td>

                  <!-- 8. 学院 / 部门 -->
                  <td>
                    <span class="college-text" :title="row.department">{{ row.department || '—' }}</span>
                  </td>

                  <!-- 9. 班级 / 职位 -->
                  <td>
                    <span class="college-text" :title="row.position">{{ row.position || '—' }}</span>
                  </td>
                </tr>
              </tbody>
            </n-table>
          </div>
        </div>

        <!-- 3. 底部操作栏 (Modal Actions) -->
        <div class="modal-actions">
          <div class="actions-left">
            <span class="status-indicator">
              当前审核状态：
              <n-tag size="small" :type="getTeamStatusTag(detailTeam.status)" round>
                {{ getTeamStatusLabel(detailTeam.status) }}
              </n-tag>
              <span class="meta-divider" style="margin: 0 8px; color: #d1d1d6;">|</span>
              <span>队长：<strong>{{ detailTeam.captainUsername }}</strong></span>
              <span class="meta-divider" style="margin: 0 8px; color: #d1d1d6;">|</span>
              <span>报名时间：{{ formatDateTime(detailTeam.createdAt) }}</span>
            </span>
          </div>
          <div class="actions-right">
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
          </div>
        </div>
      </div>
    </n-spin>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, nextTick } from 'vue'
import {
  NModal,
  NSpin,
  NButton,
  NIcon,
  NInput,
  NTag,
  NTable,
  NSpace,
  useMessage
} from 'naive-ui'
import {
  CloseCircleOutline,
  CreateOutline
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
  TeamStatus
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

// 编辑队伍信息状态
const isEditingName = ref(false)
const savingName = ref(false)
const nameInputRef = ref<any>(null)
const editNameForm = reactive({
  teamName: '',
  description: '',
  organization: '',
  organizationType: undefined as OrganizationType | undefined
})

const detailTeamDisplayAvatarUrl = computed(() => {
  return formatAvatarUrl(detailTeam.value?.avatarUrl)
})

const isSchoolOrganization = computed(() => {
  return !detailTeam.value?.organizationType || detailTeam.value?.organizationType === 'SCHOOL'
})

const detailMembersOnly = computed(() => {
  if (!detailTeam.value?.members) return []
  return detailTeam.value.members.filter(m => m.role !== 'LEADER')
})

const detailLeadersOnly = computed(() => {
  if (!detailTeam.value?.members) return []
  return detailTeam.value.members.filter(m => m.role === 'LEADER')
})

watch(
  () => [props.show, props.teamId],
  ([newShow, newTeamId]) => {
    if (newShow && newTeamId) {
      fetchTeamDetail(newTeamId as number | string)
    } else {
      isEditingName.value = false
    }
  },
  { immediate: true }
)

async function fetchTeamDetail(id: number | string) {
  loading.value = true
  try {
    const res = await getAdminTeamDetailApi(props.compId, id)
    detailTeam.value = res.data
  } catch (error: any) {
    message.error(error.message || '获取队伍详细资料失败')
  } finally {
    loading.value = false
  }
}

function handleUpdateShow(val: boolean) {
  emit('update:show', val)
}

function handleClose() {
  emit('update:show', false)
}

// 快速编辑队伍名称
function startEditName() {
  if (!detailTeam.value) return
  editNameForm.teamName = detailTeam.value.teamName || ''
  editNameForm.description = detailTeam.value.description || ''
  editNameForm.organization = detailTeam.value.organization || ''
  editNameForm.organizationType = detailTeam.value.organizationType
  isEditingName.value = true
  nextTick(() => {
    nameInputRef.value?.focus()
  })
}

function cancelEditName() {
  isEditingName.value = false
}

async function handleSaveTeamName() {
  const newName = editNameForm.teamName.trim()
  if (!newName) {
    message.warning('队伍名称不能为空')
    return
  }
  if (!detailTeam.value) return

  savingName.value = true
  try {
    await updateAdminTeamApi(props.compId, detailTeam.value.id, {
      teamName: newName,
      description: editNameForm.description,
      organization: editNameForm.organization,
      organizationType: editNameForm.organizationType
    })
    message.success('战队名称已成功更新')
    detailTeam.value.teamName = newName
    isEditingName.value = false
    emit('updated', detailTeam.value)
    emit('update-success')
  } catch (error: any) {
    message.error(error.message || '修改队伍名称失败')
  } finally {
    savingName.value = false
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
    emit('update:show', false)
  } catch (error: any) {
    message.error(error.message || '审核通过操作失败')
  } finally {
    approving.value = false
  }
}

function getOrganizationTypeLabel(type?: string): string {
  switch (type) {
    case 'SCHOOL':
      return '高校/科研机构'
    case 'ENTERPRISE':
      return '政企/行业单位'
    case 'COMMUNITY':
      return '安全战队/社会组织'
    case 'OTHER':
      return '其他组织'
    default:
      return '高校/科研机构'
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

    .team-desc-line {
      display: flex;
      align-items: flex-start;
      gap: 8px;
      font-size: 13px;
      padding-top: 10px;
      border-top: 1px solid #e9ecef;

      .desc-label {
        color: #888888;
        flex-shrink: 0;
      }

      .desc-content {
        color: #444444;
        line-height: 1.4;
      }
    }
  }

  .roster-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 8px;
    border-bottom: 1px solid var(--v-border-base, #e5e4e7);

    .roster-title-area {
      display: flex;
      align-items: center;
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
    max-height: 440px;
    overflow-y: auto;
    margin-bottom: 8px;
    border: 1px solid var(--v-border-base, #e5e4e7);
    border-radius: 6px;

    .roster-table {
      min-width: 1240px;

      th {
        font-weight: 600;
        background: var(--v-bg-card-alt, #fafafc);
        color: #444444;
        white-space: nowrap;
        padding: 8px 10px;
        position: sticky;
        top: 0;
        z-index: 2;
        border-bottom: 1px solid var(--v-border-base, #e5e4e7);
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

      tr.captain-row {
        background-color: rgba(0, 0, 0, 0.015);
      }
    }
  }

  .leaders-section {
    margin-top: 8px;
    margin-bottom: 8px;

    .leader-roster-header {
      border-bottom-style: dashed;
    }
  }

  .member-profile-cell {
    display: flex;
    align-items: center;
    gap: 8px;

    .member-name-block {
      display: flex;
      flex-direction: column;
      line-height: 1.3;

      .real-name {
        font-weight: 600;
        font-size: 13px;
        color: var(--v-text-primary, #111111);
      }

      .user-handle {
        font-size: 11px;
        color: #777777;
      }
    }
  }

  .code-text {
    font-family: var(--v-font-mono, 'Fira Code', monospace);
    font-size: 12px;
    font-weight: 500;
    color: var(--v-text-primary, #111111);
  }

  .email-text {
    font-size: 12px;
    color: #444444;
    word-break: break-all;
  }

  .id-card-badge {
    font-family: var(--v-font-mono, 'Fira Code', monospace);
    font-size: 12px;
    background: var(--v-bg-muted, rgba(0, 0, 0, 0.04));
    padding: 2px 6px;
    border-radius: 4px;
    display: inline-block;
    color: var(--v-text-primary, #111111);
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

  .modal-actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
    margin-top: 16px;

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

// Dark Mode Support
html.dark,
[data-theme='dark'] {
  .team-detail-content {
    .reject-alert-banner {
      background: rgba(208, 48, 80, 0.15);
      border-color: rgba(208, 48, 80, 0.35);
      color: #f87171;
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
                color: #ffffff;
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
          color: #64748b;
        }

        .desc-content {
          color: #cbd5e1;
        }
      }
    }

    .roster-header {
      border-color: var(--v-border-base, rgba(255, 255, 255, 0.09));

      .roster-title {
        color: var(--v-text-primary, #f8fafc);
      }

      .roster-count {
        color: var(--v-text-desc, #94a3b8);

        strong {
          color: var(--v-text-primary, #f8fafc);
        }

        .total-limit-text {
          color: var(--v-text-muted, #64748b);
        }
      }
    }

    .table-scroll-container {
      border-color: var(--v-border-base, rgba(255, 255, 255, 0.09));

      .roster-table {
        th {
          background: var(--v-bg-muted, #12141c);
          color: var(--v-text-desc, #94a3b8);
          border-bottom-color: var(--v-border-base, rgba(255, 255, 255, 0.09));
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

    .member-profile-cell {
      .member-name-block {
        .real-name {
          color: #f8fafc;
        }

        .user-handle {
          color: #64748b;
        }
      }
    }

    .code-text {
      color: #f8fafc;
    }

    .id-card-badge {
      background: rgba(255, 255, 255, 0.06);
      color: var(--v-text-primary, #f8fafc);
    }

    .email-text,
    .college-text,
    .gender-text {
      color: var(--v-text-desc, #94a3b8);
    }

    .text-muted {
      color: #64748b;
    }

    .modal-actions {
      .actions-left .status-indicator {
        color: var(--v-text-desc, #94a3b8);
      }

      .meta-divider {
        color: rgba(255, 255, 255, 0.2);
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
}
</style>
