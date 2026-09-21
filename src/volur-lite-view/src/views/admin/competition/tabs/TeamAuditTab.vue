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
      :scroll-x="1060"
      :row-key="(row: AdminTeamListVO) => row.id"
      :checked-row-keys="checkedRowKeys"
      table-class="teams-audit-table"
      standalone-pagination
      :page="pagination.page"
      :page-size="pagination.pageSize"
      :item-count="pagination.total"
      @update:checked-row-keys="handleCheckedRowKeysChange"
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

      <!-- Table Header Extra: 批量操作快捷栏 -->
      <template #table-header>
        <div v-if="checkedRowKeys.length > 0" class="bulk-actions-bar">
          <div class="bulk-info">
            已勾选 <span class="highlight-count">{{ checkedRowKeys.length }}</span> 支队伍
          </div>
          <n-space :size="8" align="center">
            <n-button
              size="small"
              class="theme-primary-btn"
              :loading="batchOperating"
              @click="handleBatchApprove"
            >
              <template #icon>
                <n-icon :component="CheckmarkOutline" />
              </template>
              批量通过
            </n-button>
            <n-button
              size="small"
              type="warning"
              secondary
              :loading="batchOperating"
              @click="openBatchRejectModal"
            >
              <template #icon>
                <n-icon :component="CloseOutline" />
              </template>
              批量驳回
            </n-button>
            <n-button
              size="small"
              type="error"
              secondary
              :loading="batchOperating"
              @click="handleBatchDelete"
            >
              <template #icon>
                <n-icon :component="TrashOutline" />
              </template>
              批量删除
            </n-button>
            <n-button size="small" quaternary @click="checkedRowKeys = []">
              取消选择
            </n-button>
          </n-space>
        </div>
      </template>

      <template #empty>
        <div style="padding: 24px; text-align: center; color: #888;">
          当前比赛暂无报名的参赛队伍
        </div>
      </template>
    </VolurTableCard>

    <!-- Reject Reason Modal (Single Team) -->
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

    <!-- Batch Reject Reason Modal -->
    <n-modal
      v-model:show="showBatchRejectModal"
      preset="card"
      :title="`批量驳回报名 (已选择 ${checkedRowKeys.length} 支队伍)`"
      style="width: 480px;"
    >
      <div class="reject-modal-content">
        <n-form label-placement="top">
          <n-form-item label="统一驳回原因 (所选各队伍队长可见)" required>
            <n-input
              v-model:value="batchRejectReason"
              type="textarea"
              placeholder="请输入批量驳回的原因，例如：未按期提交资格审核凭据..."
              :rows="4"
            />
          </n-form-item>
        </n-form>
      </div>
      <template #footer>
        <n-space justify="end" :size="12">
          <n-button @click="showBatchRejectModal = false">取消</n-button>
          <n-button
            type="error"
            :loading="batchOperating"
            :disabled="!batchRejectReason.trim()"
            @click="handleConfirmBatchReject"
          >
            确认批量驳回
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- Edit Team Modal (Admin Update Team Info) -->
    <n-modal
      v-model:show="showEditModal"
      preset="card"
      :title="`修改队伍信息 - ${editingTeam?.teamName || ''}`"
      style="width: 520px;"
    >
      <div v-if="editingTeam" class="edit-modal-content">
        <n-form label-placement="top" :model="editForm">
          <n-form-item label="队伍名称" required>
            <n-input
              v-model:value="editForm.teamName"
              placeholder="请输入队伍名称 (2-32 字符)"
              maxlength="32"
              show-count
            />
          </n-form-item>

          <n-form-item label="所属单位">
            <n-input
              v-model:value="editForm.organization"
              placeholder="请输入学校或企事业单位全称"
              maxlength="64"
            />
          </n-form-item>

          <n-form-item label="单位类型">
            <n-select
              v-model:value="editForm.organizationType"
              :options="organizationTypeOptions"
              placeholder="请选择单位类型"
            />
          </n-form-item>

          <n-form-item label="队伍简介">
            <n-input
              v-model:value="editForm.description"
              type="textarea"
              placeholder="简要描述队伍口号或特色 (选填，不超过200字符)"
              :rows="3"
              maxlength="200"
              show-count
            />
          </n-form-item>
        </n-form>
      </div>
      <template #footer>
        <n-space justify="end" :size="12">
          <n-button @click="showEditModal = false">取消</n-button>
          <n-button
            type="primary"
            class="theme-primary-btn"
            :loading="submittingEdit"
            :disabled="!editForm.teamName.trim()"
            @click="handleConfirmEdit"
          >
            保存修改
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- Team Full Details Modal (Extracted into reusable admin component) -->
    <AdminTeamDetailModal
      v-model:show="showDetailModal"
      :comp-id="compId"
      :team-id="selectedTeamId"
      @audit-success="fetchTeams"
      @update-success="fetchTeams"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted, watch } from 'vue'
import {
  NCard,
  NButton,
  NIcon,
  NInput,
  NSelect,
  NSwitch,
  NTag,
  NModal,
  NForm,
  NFormItem,
  NSpace,
  useMessage,
  useDialog,
  type DataTableColumns,
  type SelectOption,
  type DataTableRowKey
} from 'naive-ui'
import { VolurTableCard } from '@/public/volurEx'
import AdminTeamDetailModal from '@/views/competition/components/admin/AdminTeamDetailModal.vue'
import {
  PeopleOutline,
  TimeOutline,
  CheckmarkCircleOutline,
  CloseCircleOutline,
  SearchOutline,
  RefreshOutline,
  CheckmarkOutline,
  CloseOutline,
  TrashOutline
} from '@vicons/ionicons5'
import type {
  AdminTeamListVO,
  TeamAuditRequestDTO,
  AdminCompetitionDetailVO,
  TeamStatus,
  OrganizationType
} from '@/types/competition'
import {
  listAdminTeamsApi,
  getAdminTeamDetailApi,
  auditAdminTeamApi,
  deleteAdminTeamApi,
  updateCompetitionApi,
  updateAdminTeamApi,
  batchAuditAdminTeamsApi,
  batchDeleteAdminTeamsApi
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

// Checkbox Selection State
const checkedRowKeys = ref<DataTableRowKey[]>([])
const batchOperating = ref<boolean>(false)

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

// Single Reject Modal State
const showRejectModal = ref<boolean>(false)
const rejectingTeam = ref<AdminTeamListVO | null>(null)
const rejectReason = ref<string>('')
const submittingReject = ref<boolean>(false)

// Batch Reject Modal State
const showBatchRejectModal = ref<boolean>(false)
const batchRejectReason = ref<string>('')

// Edit Team Modal State (Admin Rename and Update)
const showEditModal = ref<boolean>(false)
const editingTeam = ref<AdminTeamListVO | null>(null)
const submittingEdit = ref<boolean>(false)
const editForm = reactive<{
  teamName: string
  description: string
  organization: string
  organizationType: OrganizationType
}>({
  teamName: '',
  description: '',
  organization: '',
  organizationType: 'SCHOOL'
})

// Detail Modal State (AdminTeamDetailModal)
const showDetailModal = ref<boolean>(false)
const selectedTeamId = ref<string | number | null>(null)

const statusFilterOptions: SelectOption[] = [
  { label: '全部状态', value: '' },
  { label: '待审核 (AUDIT_DRAFT)', value: 'AUDIT_DRAFT' },
  { label: '已通过 (AUDIT_PASSED)', value: 'AUDIT_PASSED' },
  { label: '已驳回 (AUDIT_REJECTED)', value: 'AUDIT_REJECTED' }
]

const organizationTypeOptions: SelectOption[] = [
  { label: '学校 (SCHOOL)', value: 'SCHOOL' },
  { label: '企业单位 (ENTERPRISE)', value: 'ENTERPRISE' },
  { label: '科研院所 (RESEARCH)', value: 'RESEARCH' },
  { label: '军工 (MILITARY)', value: 'MILITARY' },
  { label: '能源 (ENERGY)', value: 'ENERGY' },
  { label: '金融 (FINANCE)', value: 'FINANCE' },
  { label: '其他 (OTHER)', value: 'OTHER' }
]

const columns: DataTableColumns<AdminTeamListVO> = [
  {
    type: 'selection',
    fixed: 'left',
    width: 48
  },
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
    width: 260,
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
            onClick: () => openEditTeamModal(row)
          },
          {
            default: () => '编辑'
          }
        ),
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
  checkedRowKeys.value = []
  fetchTeams()
}

function handleReset() {
  searchKeyword.value = ''
  selectedStatus.value = null
  pagination.page = 1
  checkedRowKeys.value = []
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

function handleCheckedRowKeysChange(keys: DataTableRowKey[]) {
  checkedRowKeys.value = keys
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
    showRejectModal.value = false
    fetchTeams()
  } catch (error: any) {
    message.error(error.message || '驳回失败')
  } finally {
    submittingReject.value = false
  }
}

// Admin Edit Team
function openEditTeamModal(row: AdminTeamListVO) {
  editingTeam.value = row
  editForm.teamName = row.teamName || ''
  editForm.description = ''
  editForm.organization = ''
  editForm.organizationType = 'SCHOOL'
  showEditModal.value = true

  // Fetch full details to pre-populate form
  getAdminTeamDetailApi(props.compId, row.id).then((res) => {
    if (res.data) {
      editForm.teamName = res.data.teamName || ''
      editForm.description = res.data.description || ''
      editForm.organization = res.data.organization || ''
      editForm.organizationType = res.data.organizationType || 'SCHOOL'
    }
  }).catch(() => {})
}

async function handleConfirmEdit() {
  if (!editingTeam.value) return
  const trimmedName = editForm.teamName.trim()
  if (!trimmedName || trimmedName.length < 2 || trimmedName.length > 32) {
    message.warning('队伍名称长度必须在 2 到 32 个字符之间')
    return
  }
  submittingEdit.value = true
  try {
    await updateAdminTeamApi(props.compId, editingTeam.value.id, {
      teamName: trimmedName,
      description: editForm.description ? editForm.description.trim() : undefined,
      organization: editForm.organization ? editForm.organization.trim() : undefined,
      organizationType: editForm.organizationType
    })
    message.success('队伍信息已成功更新')
    showEditModal.value = false
    fetchTeams()
  } catch (error: any) {
    message.error(error.message || '修改队伍信息失败')
  } finally {
    submittingEdit.value = false
  }
}

function openDetailModal(row: AdminTeamListVO) {
  selectedTeamId.value = row.id
  showDetailModal.value = true
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

// Batch Actions
function handleBatchApprove() {
  if (checkedRowKeys.value.length === 0) return
  dialog.info({
    title: '批量通过审核',
    content: `确定要将选中的 ${checkedRowKeys.value.length} 支队伍批量审核通过吗？通过后成员将获得正式参赛资格。`,
    positiveText: '确认通过',
    negativeText: '取消',
    onPositiveClick: async () => {
      batchOperating.value = true
      try {
        await batchAuditAdminTeamsApi(props.compId, {
          teamIds: checkedRowKeys.value,
          status: 'AUDIT_PASSED'
        })
        message.success(`已成功批量通过 ${checkedRowKeys.value.length} 支队伍`)
        checkedRowKeys.value = []
        fetchTeams()
      } catch (error: any) {
        message.error(error.message || '批量审核通过失败')
      } finally {
        batchOperating.value = false
      }
    }
  })
}

function openBatchRejectModal() {
  if (checkedRowKeys.value.length === 0) return
  batchRejectReason.value = ''
  showBatchRejectModal.value = true
}

async function handleConfirmBatchReject() {
  if (checkedRowKeys.value.length === 0) return
  const reason = batchRejectReason.value.trim()
  if (!reason) {
    message.warning('请输入批量驳回原因')
    return
  }
  batchOperating.value = true
  try {
    await batchAuditAdminTeamsApi(props.compId, {
      teamIds: checkedRowKeys.value,
      status: 'AUDIT_REJECTED',
      auditReason: reason
    })
    message.success(`已成功批量驳回 ${checkedRowKeys.value.length} 支队伍`)
    checkedRowKeys.value = []
    showBatchRejectModal.value = false
    fetchTeams()
  } catch (error: any) {
    message.error(error.message || '批量驳回失败')
  } finally {
    batchOperating.value = false
  }
}

function handleBatchDelete() {
  if (checkedRowKeys.value.length === 0) return
  dialog.warning({
    title: '批量彻底删除队伍',
    content: `确定要彻底删除选中的 ${checkedRowKeys.value.length} 支队伍吗？此操作不可恢复，队伍及队员参赛报名信息将被完全解除！`,
    positiveText: '确认批量删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      batchOperating.value = true
      try {
        await batchDeleteAdminTeamsApi(props.compId, {
          teamIds: checkedRowKeys.value
        })
        message.success(`已成功删除 ${checkedRowKeys.value.length} 支队伍`)
        checkedRowKeys.value = []
        fetchTeams()
      } catch (error: any) {
        message.error(error.message || '批量删除队伍失败')
      } finally {
        batchOperating.value = false
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

  .bulk-actions-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 10px 16px;
    background: var(--v-bg-card-alt, #fafafc);
    border: 1px solid var(--v-border-base, #e5e4e7);
    border-radius: 6px;
    margin-bottom: 12px;

    .bulk-info {
      font-size: 13px;
      color: #444444;

      .highlight-count {
        font-weight: 700;
        color: var(--v-text-primary, #111111);
        margin: 0 4px;
      }
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

  .theme-primary-btn {
    background-color: #000000 !important;
    color: #ffffff !important;
    border: none;
    font-weight: 500;

    &:hover {
      background-color: #222222 !important;
    }
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

    .bulk-actions-bar {
      flex-direction: column;
      align-items: flex-start;
      gap: 10px;
    }
  }
}

// Dark Theme Overrides
html.dark,
[data-theme='dark'] {
  .team-audit-tab {
    .audit-config-card {
      background: var(--v-bg-muted, #12141c);
      border-color: var(--v-border-base, rgba(255, 255, 255, 0.09));

      .audit-config-inner {
        .config-left {
          .config-title {
            color: var(--v-text-primary, #f8fafc);
          }
          .config-desc {
            color: var(--v-text-desc, #94a3b8);
          }
        }
        .config-right {
          .label {
            color: var(--v-text-primary, #f8fafc);
          }
          .field-hint {
            color: var(--v-text-muted, #64748b);
          }
        }
      }
    }

    .stat-ribbon {
      .stat-mini-card {
        background: var(--v-bg-card, #12141c);
        border-color: var(--v-border-base, rgba(255, 255, 255, 0.09));

        .stat-icon-wrapper.primary {
          background: rgba(255, 255, 255, 0.08);
          color: var(--v-text-primary, #f8fafc);
        }

        .stat-info {
          .stat-value {
            color: var(--v-text-primary, #f8fafc);
          }
          .stat-label {
            color: var(--v-text-desc, #94a3b8);
          }
        }
      }
    }

    .bulk-actions-bar {
      background: var(--v-bg-muted, #12141c);
      border-color: var(--v-border-base, rgba(255, 255, 255, 0.09));

      .bulk-info {
        color: #94a3b8;

        .highlight-count {
          color: #f8fafc;
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
