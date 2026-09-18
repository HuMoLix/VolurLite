<template>
  <div class="team-audit-tab">
    <!-- Section 1: Team Audit Mode Switch -->
    <n-card class="audit-config-card" size="small" :bordered="false">
      <div class="audit-config-inner">
        <div class="config-left">
          <div class="config-title">报名团队人工资质审核机制</div>
          <div class="config-desc">
            开启后，新报名或组建的参赛队伍默认进入「待审核 (PENDING)」状态，必须由管理员审核通过后方可正式进入赛场并解题。关闭后，队伍报名成功即自动拥有参赛资格。
          </div>
        </div>
        <div class="config-right">
          <div class="switch-wrap">
            <span class="label">人工审核模式：</span>
            <n-switch v-model:value="requireAudit" @update:value="handleToggleAuditSwitch" />
            <span class="field-hint">
              {{ requireAudit ? '需审核' : '免审直接参赛' }}
            </span>
          </div>
        </div>
      </div>
    </n-card>

    <!-- Section 2: Stats Cards -->
    <div class="stat-ribbon">
      <div class="stat-mini-card">
        <div class="stat-icon-wrapper primary">
          <n-icon size="18">
            <PeopleOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statData.total }}</div>
          <div class="stat-label">报名总队伍</div>
        </div>
      </div>

      <div class="stat-mini-card">
        <div class="stat-icon-wrapper warning">
          <n-icon size="18">
            <TimeOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statData.pending }}</div>
          <div class="stat-label">待审核队伍</div>
        </div>
      </div>

      <div class="stat-mini-card">
        <div class="stat-icon-wrapper success">
          <n-icon size="18">
            <CheckmarkCircleOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statData.approved }}</div>
          <div class="stat-label">已通过资质</div>
        </div>
      </div>

      <div class="stat-mini-card">
        <div class="stat-icon-wrapper error">
          <n-icon size="18">
            <CloseCircleOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statData.rejected }}</div>
          <div class="stat-label">已驳回报名</div>
        </div>
      </div>
    </div>

    <!-- Section 3: Registered Teams Table Card -->
    <VolurTableCard
      :bordered="true"
      :table-bordered="true"
      :columns="columns"
      :data="teams"
      :loading="loading"
      :scroll-x="970"
      :row-key="(row: AdminTeamListVO) => row.id"
      table-class="teams-audit-table"
      standalone-pagination
      :page="pagination.page"
      :page-size="pagination.pageSize"
      :item-count="pagination.total"
      @update:page="handlePageChange"
      @update:page-size="handlePageSizeChange"
    >
      <template #filter-left>
        <n-space align="center" :size="12" wrap class="tab-filter-left">
          <n-input
            v-model:value="searchKeyword"
            placeholder="搜索队伍名称或队长用户名..."
            clearable
            style="width: 280px;"
            class="filter-search-input"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <n-icon :component="SearchOutline" />
            </template>
          </n-input>
          <n-select
            v-model:value="selectedStatus"
            :options="statusFilterOptions"
            placeholder="审核状态筛选"
            clearable
            style="width: 180px;"
            class="filter-status-select"
            @update:value="handleSearch"
          />
          <n-button type="primary" class="theme-primary-btn" @click="handleSearch">
            <template #icon>
              <n-icon :component="SearchOutline" />
            </template>
            查询
          </n-button>
          <n-button quaternary @click="handleReset">重置</n-button>
        </n-space>
      </template>

      <template #filter-right>
        <n-space align="center" :size="12" class="tab-filter-right">
          <n-button quaternary :loading="loading" @click="() => fetchTeams()">
            <template #icon>
              <n-icon :component="RefreshOutline" />
            </template>
            刷新列表
          </n-button>
        </n-space>
      </template>

      <template #empty>
        <div style="padding: 24px; text-align: center; color: #888;">
          当前比赛暂无报名的参赛队伍
        </div>
      </template>
    </VolurTableCard>

    <!-- Reject Reason Modal -->
    <n-modal
      v-model:show="showRejectModal"
      preset="card"
      :title="`驳回队伍报名 - ${rejectingTeam?.teamName || ''}`"
      style="width: 480px;"
    >
      <div v-if="rejectingTeam" class="reject-modal-content">
        <n-form label-placement="top">
          <n-form-item label="驳回原因 (选手及队长可见)" required>
            <n-input
              v-model:value="rejectReason"
              type="textarea"
              placeholder="请输入驳回的具体原因，例如：队员实名信息不完整或队伍名包含违规字符..."
              :rows="4"
            />
          </n-form-item>
        </n-form>
      </div>
      <template #footer>
        <n-space justify="end" :size="12">
          <n-button @click="showRejectModal = false">取消</n-button>
          <n-button
            type="error"
            :loading="submittingReject"
            :disabled="!rejectReason.trim()"
            @click="handleConfirmReject"
          >
            确认驳回
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- Team Full Details Modal -->
    <n-modal
      v-model:show="showDetailModal"
      preset="card"
      class="team-detail-modal"
      style="width: 1200px; max-width: 96vw; border-radius: 12px;"
      :title="`参赛队伍详情 - ${detailTeam?.teamName || ''}`"
      :bordered="false"
    >
      <n-spin :show="detailLoading">
        <div v-if="detailTeam" class="team-detail-content">
          <!-- 1. 队伍基本概况 (Team Meta Panel) -->
          <div class="team-meta-panel">
            <div class="team-header-identity">
              <AppAvatar
                round
                :size="46"
                :src="detailTeam.avatarUrl"
                :name="detailTeam.teamName"
              />
              <div class="identity-info">
                <div class="team-name-row">
                  <span class="team-name">{{ detailTeam.teamName }}</span>
                </div>
                <div class="team-sub-meta">
                  <span class="meta-entry">队长: <strong>{{ detailTeam.captainUsername }}</strong></span>
                  <span class="meta-divider">|</span>
                  <span class="meta-entry">报名提交: <span>{{ new Date(detailTeam.createdAt).toLocaleString() }}</span></span>
                  <template v-if="detailTeam.competitionTitle">
                    <span class="meta-divider">|</span>
                    <span class="meta-entry">所属赛事: <span>{{ detailTeam.competitionTitle }}</span></span>
                  </template>
                </div>
              </div>
            </div>

            <!-- 历史驳回原因展示 -->
            <div
              v-if="detailTeam.status === 'AUDIT_REJECTED' && detailTeam.auditReason"
              class="reject-alert-banner"
            >
              <n-icon :component="CloseCircleOutline" class="alert-icon" />
              <div class="alert-text">
                <strong>历史审核驳回原因：</strong>{{ detailTeam.auditReason }}
              </div>
            </div>
          </div>

          <!-- 2. 战队成员名单 (Roster Header & Table) -->
          <div class="roster-header">
            <div class="roster-title-area">
              <h4 class="roster-title">参赛成员资质名册</h4>
              <span class="roster-count">
                当前成员: <strong>{{ detailMemberCount }}</strong> /
                限制: {{ compMinMembers }} ~ {{ compMaxMembers }} 人
                <template v-if="compMaxLeaders !== undefined">
                  (领队: {{ detailLeaderCount }} / {{ compMaxLeaders }} 人)
                </template>
                <span class="total-limit-text">
                  （名册上限: {{ compMaxTotalRows }} 人）
                </span>
              </span>
            </div>
          </div>

          <div class="table-scroll-container">
            <n-table :bordered="true" :single-line="false" size="small" class="roster-table">
              <thead>
                <tr>
                  <th style="width: 155px;">成员姓名 / 账号</th>
                  <th style="width: 75px; text-align: center;">性别</th>
                  <th style="width: 95px; text-align: center;">队内角色</th>
                  <th style="width: 130px;">学号 / 工号</th>
                  <th style="width: 135px;">联系电话</th>
                  <th style="width: 165px;">电子邮箱</th>
                  <th style="width: 175px;">身份证件号码</th>
                  <th style="width: 140px;">所属院系</th>
                  <th style="width: 95px; text-align: center;">加入状态</th>
                  <th style="width: 105px; text-align: center;">加入时间</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="(row, idx) in sortedDetailMembers"
                  :key="idx"
                  :class="{ 'captain-row': row.role === 'CAPTAIN' }"
                >
                  <!-- 1. 姓名 / 账号 -->
                  <td>
                    <div class="member-profile-cell">
                      <AppAvatar
                        round
                        :size="26"
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

                  <!-- 3. 队内角色 -->
                  <td style="text-align: center;">
                    <n-tag
                      size="small"
                      round
                      :type="row.role === 'CAPTAIN' ? 'primary' : row.role === 'LEADER' ? 'warning' : 'default'"
                    >
                      {{ row.role === 'CAPTAIN' ? '队长' : row.role === 'LEADER' ? '领队' : '队员' }}
                    </n-tag>
                  </td>

                  <!-- 4. 学号 / 工号 -->
                  <td>
                    <span class="code-text">{{ row.studentNumber || '—' }}</span>
                  </td>

                  <!-- 5. 手机号 -->
                  <td>
                    <span class="code-text">{{ row.phone || '—' }}</span>
                  </td>

                  <!-- 6. 邮箱 -->
                  <td>
                    <span class="email-text" :title="row.email">{{ row.email || '—' }}</span>
                  </td>

                  <!-- 7. 身份证件号码 -->
                  <td>
                    <span v-if="row.idCard" class="id-card-badge">{{ row.idCard }}</span>
                    <span v-else class="text-muted">未填写</span>
                  </td>

                  <!-- 8. 所属院系 -->
                  <td>
                    <span class="college-text" :title="row.college">{{ row.college || '—' }}</span>
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

                  <!-- 10. 加入时间 -->
                  <td style="text-align: center;">
                    <span class="date-text">{{ row.joinedAt ? new Date(row.joinedAt).toLocaleDateString() : '—' }}</span>
                  </td>
                </tr>
              </tbody>
            </n-table>
          </div>

          <!-- 3. 底部操作栏 (Modal Actions) -->
          <div class="modal-actions">
            <div class="actions-left">
              <span class="status-indicator">
                当前审核状态：
                <n-tag size="small" :type="getTeamStatusTag(detailTeam.status)" round>
                  {{ getTeamStatusLabel(detailTeam.status) }}
                </n-tag>
              </span>
            </div>
            <div class="actions-right">
              <n-button @click="showDetailModal = false">关闭</n-button>
              <n-button
                v-if="detailTeam.status !== 'AUDIT_REJECTED'"
                type="error"
                secondary
                @click="handleRejectFromDetail"
              >
                驳回报名
              </n-button>
              <n-button
                v-if="detailTeam.status !== 'AUDIT_PASSED'"
                class="theme-primary-btn"
                @click="handleApproveFromDetail"
              >
                通过资质审核
              </n-button>
            </div>
          </div>
        </div>
      </n-spin>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, h, onMounted, watch } from 'vue'
import {
  NCard,
  NButton,
  NIcon,
  NSpin,
  NInput,
  NSelect,
  NSwitch,
  NTag,
  NModal,
  NTable,
  NForm,
  NFormItem,
  NSpace,
  useMessage,
  useDialog,
  type DataTableColumns,
  type SelectOption
} from 'naive-ui'
import { VolurTableCard } from '@/public/volurEx'
import AppAvatar from '@/components/common/AppAvatar.vue'
import {
  PeopleOutline,
  TimeOutline,
  CheckmarkCircleOutline,
  CloseCircleOutline,
  SearchOutline,
  RefreshOutline
} from '@vicons/ionicons5'
import type {
  AdminTeamListVO,
  AdminTeamDetailVO,
  CompetitionMemberVO,
  TeamAuditRequestDTO,
  AdminCompetitionDetailVO,
  TeamStatus
} from '@/types/competition'
import {
  listAdminTeamsApi,
  getAdminTeamDetailApi,
  auditAdminTeamApi,
  deleteAdminTeamApi,
  updateCompetitionApi
} from '@/api/competition'

interface Props {
  compId: string | number
  initialData: AdminCompetitionDetailVO | null
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'refresh'): void
}>()

const message = useMessage()
const dialog = useDialog()

// Audit Configuration State
const requireAudit = ref<boolean>(false)

// Table State
const loading = ref<boolean>(false)
const teams = ref<AdminTeamListVO[]>([])
const searchKeyword = ref<string>('')
const selectedStatus = ref<TeamStatus | null>(null)

const statData = reactive({
  total: 0,
  pending: 0,
  approved: 0,
  rejected: 0
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

// Reject Modal State
const showRejectModal = ref<boolean>(false)
const rejectingTeam = ref<AdminTeamListVO | null>(null)
const rejectReason = ref<string>('')
const submittingReject = ref<boolean>(false)

// Detail Modal State
const showDetailModal = ref<boolean>(false)
const detailLoading = ref<boolean>(false)
const detailTeam = ref<AdminTeamDetailVO | null>(null)

const sortedDetailMembers = computed<CompetitionMemberVO[]>(() => {
  if (!detailTeam.value?.members) return []
  const rolePriority: Record<string, number> = {
    CAPTAIN: 1,
    LEADER: 2,
    MEMBER: 3
  }
  return [...detailTeam.value.members].sort((a, b) => {
    const pA = rolePriority[a.role] ?? 99
    const pB = rolePriority[b.role] ?? 99
    return pA - pB
  })
})

const detailMemberCount = computed(() => {
  return detailTeam.value?.members?.filter(m => m.role !== 'LEADER').length || 0
})

const detailLeaderCount = computed(() => {
  return detailTeam.value?.members?.filter(m => m.role === 'LEADER').length || 0
})

const compMinMembers = computed(() => {
  return props.initialData?.minTeamMembers ?? 1
})

const compMaxMembers = computed(() => {
  return props.initialData?.maxTeamMembers ?? 4
})

const compMaxLeaders = computed(() => {
  return props.initialData?.maxLeaders ?? 1
})

const compMaxTotalRows = computed(() => {
  return compMaxMembers.value + compMaxLeaders.value
})

function getMemberStatusTag(row: CompetitionMemberVO): { type: 'success' | 'warning' | 'default' | 'info'; label: string } {
  if (row.role === 'CAPTAIN' || row.status === 'INVITED_ACCEPTED') {
    return { type: 'success', label: '已加入' }
  }
  if (row.status === 'INVITED') {
    return { type: 'warning', label: '待确认' }
  }
  if (row.status === 'NOT_PARTICIPATE') {
    return { type: 'default', label: '已拒绝' }
  }
  return { type: 'info', label: '待加入' }
}

const statusFilterOptions: SelectOption[] = [
  { label: '全部状态', value: '' },
  { label: '待审核 (AUDIT_DRAFT)', value: 'AUDIT_DRAFT' },
  { label: '已通过 (AUDIT_PASSED)', value: 'AUDIT_PASSED' },
  { label: '已驳回 (AUDIT_REJECTED)', value: 'AUDIT_REJECTED' }
]

const columns: DataTableColumns<AdminTeamListVO> = [
  {
    title: '战队名称',
    key: 'teamName',
    render(row) {
      return h('div', { style: 'display: flex; align-items: center; gap: 8px;' }, [
        h('span', { style: 'font-weight: 600; color: var(--v-text-primary, #111); font-size: 14px;' }, row.teamName)
      ])
    }
  },
  {
    title: '队长',
    key: 'captainUsername',
    width: 140,
    render(row) {
      return h('span', { style: 'font-size: 13px; font-weight: 500;' }, row.captainUsername)
    }
  },
  {
    title: '成员人数',
    key: 'memberCount',
    width: 100,
    render(row) {
      return `${row.memberCount || 1} 人`
    }
  },
  {
    title: '审核状态',
    key: 'status',
    width: 120,
    render(row) {
      return h(NTag, { size: 'small', type: getTeamStatusTag(row.status), round: true }, {
        default: () => getTeamStatusLabel(row.status)
      })
    }
  },
  {
    title: '报名时间',
    key: 'createdAt',
    width: 170,
    render(row) {
      return new Date(row.createdAt).toLocaleString()
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 240,
    fixed: 'right',
    render(row) {
      return h('div', { style: 'display: flex; gap: 6px;' }, [
        row.status !== 'AUDIT_PASSED'
          ? h(
              NButton,
              {
                size: 'tiny',
                quaternary: true,
                type: 'primary',
                onClick: () => handleApprove(row)
              },
              {
                default: () => '通过'
              }
            )
          : null,
        row.status !== 'AUDIT_REJECTED'
          ? h(
              NButton,
              {
                size: 'tiny',
                quaternary: true,
                type: 'warning',
                onClick: () => openRejectModal(row)
              },
              {
                default: () => '驳回'
              }
            )
          : null,
        h(
          NButton,
          {
            size: 'tiny',
            quaternary: true,
            onClick: () => openDetailModal(row)
          },
          {
            default: () => '详情'
          }
        ),
        h(
          NButton,
          {
            size: 'tiny',
            quaternary: true,
            type: 'error',
            onClick: () => handleDeleteTeam(row)
          },
          {
            default: () => '删除'
          }
        )
      ])
    }
  }
]

watch(
  () => props.initialData,
  (d) => {
    if (d) {
      const cfg = d.config || d
      requireAudit.value = cfg.requireTeamAudit ?? false
    }
  },
  { immediate: true }
)

onMounted(() => {
  if (props.compId) {
    fetchTeams()
  }
})

async function fetchTeams() {
  loading.value = true
  try {
    const res = await listAdminTeamsApi(props.compId, {
      page: pagination.page,
      size: pagination.pageSize,
      status: selectedStatus.value ? selectedStatus.value : undefined,
      keyword: searchKeyword.value.trim() ? searchKeyword.value.trim() : undefined
    })

    const list = res.data?.list || []
    teams.value = list
    pagination.total = res.data?.total || 0

    // Refresh stats
    statData.total = pagination.total
    statData.pending = list.filter(t => t.status === 'AUDIT_DRAFT').length
    statData.approved = list.filter(t => t.status === 'AUDIT_PASSED').length
    statData.rejected = list.filter(t => t.status === 'AUDIT_REJECTED').length
  } catch (error: any) {
    console.error('Fetch teams error:', error)
    message.error(error.message || '获取参赛队伍列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchTeams()
}

function handleReset() {
  searchKeyword.value = ''
  selectedStatus.value = null
  pagination.page = 1
  fetchTeams()
}

function handlePageChange(page: number) {
  pagination.page = page
  fetchTeams()
}

function handlePageSizeChange(pageSize: number) {
  pagination.pageSize = pageSize
  pagination.page = 1
  fetchTeams()
}

async function handleToggleAuditSwitch(val: boolean) {
  try {
    await updateCompetitionApi(props.compId, {
      config: {
        requireTeamAudit: val
      }
    })
    message.success(val ? '已开启报名资质人工审核模式' : '已切换为免审核直接参赛模式')
    emit('refresh')
  } catch (error: any) {
    requireAudit.value = !val
    message.error(error.message || '更新审核配置失败')
  }
}

async function handleApprove(row: AdminTeamListVO) {
  try {
    const req: TeamAuditRequestDTO = {
      status: 'AUDIT_PASSED'
    }
    await auditAdminTeamApi(props.compId, row.id, req)
    message.success(`队伍「${row.teamName}」资质已审核通过`)
    fetchTeams()
  } catch (error: any) {
    message.error(error.message || '审核操作失败')
  }
}

function openRejectModal(row: AdminTeamListVO) {
  rejectingTeam.value = row
  rejectReason.value = ''
  showRejectModal.value = true
}

async function handleConfirmReject() {
  if (!rejectingTeam.value) return
  submittingReject.value = true
  try {
    const req: TeamAuditRequestDTO = {
      status: 'AUDIT_REJECTED',
      auditReason: rejectReason.value.trim()
    }
    await auditAdminTeamApi(props.compId, rejectingTeam.value.id, req)
    message.success(`队伍「${rejectingTeam.value.teamName}」已驳回`)
    if (detailTeam.value && String(detailTeam.value.id) === String(rejectingTeam.value.id)) {
      detailTeam.value.status = 'AUDIT_REJECTED'
      detailTeam.value.auditReason = rejectReason.value.trim()
    }
    showRejectModal.value = false
    fetchTeams()
  } catch (error: any) {
    message.error(error.message || '驳回失败')
  } finally {
    submittingReject.value = false
  }
}

async function handleApproveFromDetail() {
  if (!detailTeam.value) return
  try {
    const req: TeamAuditRequestDTO = {
      status: 'AUDIT_PASSED'
    }
    await auditAdminTeamApi(props.compId, detailTeam.value.id, req)
    message.success(`队伍「${detailTeam.value.teamName}」资质已审核通过`)
    detailTeam.value.status = 'AUDIT_PASSED'
    fetchTeams()
  } catch (error: any) {
    message.error(error.message || '审核操作失败')
  }
}

function handleRejectFromDetail() {
  if (!detailTeam.value) return
  openRejectModal({
    id: detailTeam.value.id,
    competitionId: detailTeam.value.competitionId,
    teamName: detailTeam.value.teamName,
    captainId: detailTeam.value.captainId,
    captainUsername: detailTeam.value.captainUsername,
    memberCount: detailTeam.value.members?.length || 1,
    status: detailTeam.value.status,
    auditReason: detailTeam.value.auditReason,
    createdAt: detailTeam.value.createdAt
  })
}

async function openDetailModal(row: AdminTeamListVO) {
  showDetailModal.value = true
  detailLoading.value = true
  try {
    const res = await getAdminTeamDetailApi(props.compId, row.id)
    detailTeam.value = res.data
  } catch (error: any) {
    message.error(error.message || '获取队伍详细资料失败')
  } finally {
    detailLoading.value = false
  }
}

function handleDeleteTeam(row: AdminTeamListVO) {
  dialog.warning({
    title: '确认删除参赛队伍',
    content: `确定要彻底删除战队「${row.teamName}」吗？删除后该战队及所有成员的参赛状态将完全解除。`,
    positiveText: '确认删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteAdminTeamApi(props.compId, row.id)
        message.success('战队已成功删除')
        fetchTeams()
      } catch (error: any) {
        message.error(error.message || '删除战队失败')
      }
    }
  })
}

function getTeamStatusTag(status?: string) {
  switch (status) {
    case 'AUDIT_PASSED':
      return 'success'
    case 'AUDIT_REJECTED':
      return 'error'
    case 'AUDIT_DRAFT':
    default:
      return 'warning'
  }
}

function getTeamStatusLabel(status?: string) {
  switch (status) {
    case 'AUDIT_PASSED':
      return '已通过'
    case 'AUDIT_REJECTED':
      return '已驳回'
    case 'AUDIT_DRAFT':
    default:
      return '待审核'
  }
}
</script>

<style scoped lang="scss">
.team-audit-tab {
  display: flex;
  flex-direction: column;
  gap: 20px;

  .audit-config-card {
    background: var(--v-bg-muted, #f8f8f9);
    border: 1px solid var(--v-border-base, #e5e4e7);

    .audit-config-inner {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 16px;

      .config-left {
        .config-title {
          font-size: 15px;
          font-weight: 600;
          color: var(--v-text-primary, #111111);
          margin-bottom: 4px;
        }

        .config-desc {
          font-size: 13px;
          color: #444444;
        }
      }

      .config-right {
        .switch-wrap {
          display: flex;
          align-items: center;
          gap: 10px;

          .label {
            font-size: 13px;
            font-weight: 500;
            color: var(--v-text-primary, #111111);
          }

          .field-hint {
            font-size: 12px;
            color: #444444;
          }
        }
      }
    }
  }

  .stat-ribbon {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;

    @media (max-width: 900px) {
      grid-template-columns: repeat(2, 1fr);
    }

    .stat-mini-card {
      display: flex;
      align-items: center;
      gap: 14px;
      padding: 16px 20px;
      background: var(--v-bg-card, #ffffff);
      border: 1px solid var(--v-border-base, #e5e4e7);
      border-radius: 8px;

      .stat-icon-wrapper {
        width: 42px;
        height: 42px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;

        &.primary {
          background: rgba(0, 0, 0, 0.05);
          color: var(--v-text-primary, #111111);
        }

        &.warning {
          background: rgba(240, 160, 32, 0.1);
          color: #f0a020;
        }

        &.success {
          background: rgba(24, 160, 88, 0.1);
          color: #18a058;
        }

        &.error {
          background: rgba(208, 48, 80, 0.1);
          color: #d03050;
        }
      }

      .stat-info {
        display: flex;
        flex-direction: column;

        .stat-value {
          font-size: 18px;
          font-weight: 700;
          color: var(--v-text-primary, #111111);
        }

        .stat-label {
          font-size: 12px;
          color: #444444;
        }
      }
    }
  }

  .teams-table-card {
    .filter-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
      flex-wrap: wrap;
      gap: 12px;

      .filter-left {
        display: flex;
        align-items: center;
        gap: 12px;
      }
    }

    .pagination-footer {
      display: flex;
      justify-content: flex-end;
      margin-top: 20px;
    }
  }

  .tab-filter-left {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;

    .filter-search-input {
      width: 280px;
    }

    .filter-status-select {
      width: 180px;
    }
  }

  .tab-filter-right {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  @media (max-width: 768px) {
    .tab-filter-left {
      flex-direction: column;
      align-items: stretch;
      width: 100%;
      gap: 10px;

      .filter-search-input,
      .filter-status-select {
        width: 100% !important;
      }

      .n-button {
        width: 100%;
        justify-content: center;
      }
    }

    .tab-filter-right {
      display: flex;
      width: 100%;
      gap: 10px;
      margin-top: 8px;

      .n-button {
        width: 100%;
        justify-content: center;
      }
    }
  }
}

.team-detail-content {
  display: flex;
  flex-direction: column;
  min-height: 200px;

  .team-meta-panel {
    display: flex;
    flex-direction: column;
    gap: 12px;
    margin-bottom: 16px;
    padding: 14px 18px;
    background: var(--v-bg-card-alt, #fafafc);
    border: 1px solid var(--v-border-base, #e5e4e7);
    border-radius: 8px;

    .team-header-identity {
      display: flex;
      align-items: center;
      gap: 16px;

      .identity-info {
        display: flex;
        flex-direction: column;
        gap: 6px;

        .team-name-row {
          display: flex;
          align-items: center;
          gap: 10px;

          .team-name {
            font-size: 16px;
            font-weight: 700;
            color: var(--v-text-primary, #111111);
          }
        }

        .team-sub-meta {
          display: flex;
          align-items: center;
          gap: 10px;
          font-size: 12px;
          color: var(--v-text-desc, #444444);
          flex-wrap: wrap;

          .meta-divider {
            color: #d1d1d6;
          }

          .meta-entry {
            strong {
              color: var(--v-text-primary, #111111);
            }
          }
        }
      }
    }

    .reject-alert-banner {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 12px;
      background: rgba(208, 48, 80, 0.06);
      border: 1px solid rgba(208, 48, 80, 0.2);
      border-radius: 6px;
      font-size: 13px;
      color: #d03050;

      .alert-icon {
        font-size: 16px;
        flex-shrink: 0;
      }

      .alert-text {
        line-height: 1.4;
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
        color: var(--v-text-desc, #444444);

        strong {
          color: var(--v-text-primary, #111111);
        }

        .total-limit-text {
          color: var(--v-text-muted, #666666);
          margin-left: 4px;
        }
      }
    }
  }

  .table-scroll-container {
    overflow-x: auto;
    max-height: 480px;
    overflow-y: auto;
    margin-bottom: 16px;
    border: 1px solid var(--v-border-base, #e5e4e7);
    border-radius: 6px;

    .roster-table {
      min-width: 1240px;

      th {
        font-weight: 600;
        background: var(--v-bg-card-alt, #fafafc);
        color: var(--v-text-desc, #444444);
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

      tr.captain-row {
        background-color: var(--v-bg-muted, rgba(0, 0, 0, 0.015));
      }
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
    color: var(--v-text-desc, #444444);
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

  .gender-text {
    font-size: 13px;
    color: var(--v-text-desc, #444444);
  }

  .college-text {
    font-size: 12px;
    color: var(--v-text-desc, #444444);
  }

  .date-text {
    font-size: 12px;
    color: var(--v-text-muted, #555555);
  }

  .text-muted {
    color: var(--v-text-disabled, #999999);
    font-size: 12px;
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

      .status-indicator {
        font-size: 13px;
        color: var(--v-text-desc, #444444);
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
      background-color: var(--v-btn-primary-bg, #000000) !important;
      color: var(--v-btn-primary-text, #ffffff) !important;
      border: none;
      font-weight: 500;

      &:hover {
        background-color: var(--v-btn-primary-hover, #222222) !important;
      }
    }
  }
}

// Dark Mode Support
html.dark,
[data-theme='dark'] {
  .team-detail-content {
    .team-meta-panel {
      background: var(--v-bg-muted, #12141c);
      border-color: var(--v-border-base, rgba(255, 255, 255, 0.09));

      .team-header-identity .identity-info .team-sub-meta {
        color: var(--v-text-desc, #94a3b8);

        .meta-divider {
          color: rgba(255, 255, 255, 0.2);
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
      }
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

    .date-text {
      color: var(--v-text-muted, #64748b);
    }

    .modal-actions {
      .actions-left .status-indicator {
        color: var(--v-text-desc, #94a3b8);
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

<style lang="scss">
.team-detail-modal {
  border-radius: 12px !important;
}
</style>
