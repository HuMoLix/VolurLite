<template>
  <div class="space-y-4">
    <div class="p-4 rounded-xl bg-white dark:bg-[#292a2d] border border-google-border-light dark:border-google-border-dark flex flex-col sm:flex-row sm:items-center justify-between gap-3">
      <div>
        <div class="text-xs font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
          开启参赛报名战队资质审核
        </div>
        <div class="text-[11px] text-google-text-secondary dark:text-google-text-darkSecondary mt-0.5">
          开启后队长提交报名需经管理员在此处审核通过方可正式参赛
        </div>
      </div>
      <div class="flex items-center gap-2.5 shrink-0">
        <n-switch
          v-model:value="requireAudit"
          size="small"
          :loading="updatingAuditSwitch"
          @update:value="handleToggleAuditSwitch"
        />
        <span class="text-xs font-medium text-google-text-primary dark:text-google-text-darkPrimary select-none">
          {{ requireAudit ? '已开启审核' : '未开启审核' }}
        </span>
      </div>
    </div>

    <div class="grid grid-cols-2 md:grid-cols-4 gap-3">
      <div class="p-4 rounded-xl bg-white dark:bg-[#292a2d] border border-google-border-light dark:border-google-border-dark text-center">
        <div class="text-xl font-bold text-google-text-primary dark:text-google-text-darkPrimary">{{ teamStats.total }}</div>
        <div class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary mt-0.5">全部战队</div>
      </div>
      <div class="p-4 rounded-xl bg-white dark:bg-[#292a2d] border border-google-border-light dark:border-google-border-dark text-center">
        <div class="text-xl font-bold text-yellow-600 dark:text-yellow-400">{{ teamStats.pending }}</div>
        <div class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary mt-0.5">待审核</div>
      </div>
      <div class="p-4 rounded-xl bg-white dark:bg-[#292a2d] border border-google-border-light dark:border-google-border-dark text-center">
        <div class="text-xl font-bold text-green-600 dark:text-green-400">{{ teamStats.approved }}</div>
        <div class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary mt-0.5">已通过</div>
      </div>
      <div class="p-4 rounded-xl bg-white dark:bg-[#292a2d] border border-google-border-light dark:border-google-border-dark text-center">
        <div class="text-xl font-bold text-red-600 dark:text-red-400">{{ teamStats.rejected }}</div>
        <div class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary mt-0.5">已驳回</div>
      </div>
    </div>

    <div class="flex flex-col md:flex-row md:items-center justify-between gap-3">
      <div class="flex items-center gap-3 flex-wrap flex-1">
        <div class="w-64">
          <VInput
            v-model:value="searchKeyword"
            placeholder="搜索战队名称或队长用户名..."
            clearable
            @keyup.enter="handleSearch"
          />
        </div>
        <div class="w-40">
          <VSelection
            v-model:value="selectedStatus"
            :options="statusFilterOptions"
            placeholder="审核状态筛选"
            clearable
            @update:value="handleSearch"
          />
        </div>
        <VBtn
          clz="normal"
          size="medium"
          :left-icon="RefreshOutline"
          :loading="loadingTeams"
          @click="fetchTeams"
        >
          刷新列表
        </VBtn>
        <VBtn clz="normal" size="medium" @click="handleReset">
          重置
        </VBtn>
        <VBtn
          clz="normal"
          size="medium"
          :left-icon="CloudDownloadOutline"
          @click="showExportModal = true"
        >
          导出名单
        </VBtn>
      </div>

      <div class="flex items-center gap-2">
        <VBtn
          clz="normal"
          size="medium"
          :left-icon="SearchOutline"
          @click="handleSearch"
        >
          查询
        </VBtn>
      </div>
    </div>

    <div
      v-if="checkedRowKeys.length > 0"
      class="p-3 rounded-xl bg-blue-50/70 dark:bg-blue-950/30 border border-blue-200 dark:border-blue-900/50 flex flex-col sm:flex-row sm:items-center justify-between gap-3"
    >
      <div class="text-xs font-medium text-google-blue dark:text-google-blue-dark">
        已勾选 <span class="font-bold font-mono">{{ checkedRowKeys.length }}</span> 支战队
      </div>
      <div class="flex items-center gap-2 flex-wrap">
        <VBtn
          clz="primary"
          size="medium"
          :left-icon="CheckmarkCircleOutline"
          :loading="batchOperating"
          @click="handleBatchApprove"
        >
          批量通过
        </VBtn>
        <VBtn
          clz="danger"
          size="medium"
          :left-icon="CloseCircleOutline"
          :loading="batchOperating"
          @click="openBatchRejectModal"
        >
          批量驳回
        </VBtn>
        <VBtn
          clz="danger"
          size="medium"
          :left-icon="TrashOutline"
          :loading="batchOperating"
          @click="handleBatchDelete"
        >
          批量删除
        </VBtn>
        <VBtn
          clz="normal"
          size="medium"
          @click="checkedRowKeys = []"
        >
          取消选择
        </VBtn>
      </div>
    </div>

    <VTable
      :columns="teamColumns"
      :data="teams"
      :loading="loadingTeams"
      :pagination="false"
      :checked-row-keys="checkedRowKeys"
      :row-key="(row: AdminTeamListVO) => String(row.id)"
      size="small"
      :bordered="false"
      @update:checked-row-keys="handleCheckedRowKeysChange"
    >
      <template #empty>
        <div class="py-12 text-center text-xs text-google-text-secondary dark:text-google-text-darkSecondary">
          当前比赛暂无报名的参赛战队
        </div>
      </template>
    </VTable>

    <div v-if="teamTotal > 0" class="flex justify-end pt-2">
      <VPagination
        v-model:page="teamPage"
        v-model:page-size="teamPageSize"
        :item-count="teamTotal"
        :show-size-picker="true"
        :page-sizes="[10, 20, 50]"
        size="small"
        @update:page="handleTeamPageChange"
        @update:page-size="handleTeamPageSizeChange"
      />
    </div>

    <VModal
      v-model:show="showRejectModal"
      :title="`驳回战队报名 - ${rejectingTeam?.teamName || ''}`"
      :width="480"
    >
      <div class="space-y-4">
        <div>
          <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
            驳回原因 (选手及队长可见) *
          </label>
          <VInput
            v-model:value="rejectReason"
            type="textarea"
            :rows="4"
            placeholder="请输入驳回的具体原因，例如：队员实名信息不完整或战队名称不合规..."
          />
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-3">
          <VBtn clz="normal" size="medium" @click="showRejectModal = false">
            取消
          </VBtn>
          <VBtn
            clz="danger"
            size="medium"
            :loading="submittingReject"
            :disabled="!rejectReason.trim()"
            @click="handleConfirmReject"
          >
            确认驳回
          </VBtn>
        </div>
      </template>
    </VModal>

    <VModal
      v-model:show="showBatchRejectModal"
      :title="`批量驳回报名 (已选择 ${checkedRowKeys.length} 支战队)`"
      :width="480"
    >
      <div class="space-y-4">
        <div>
          <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
            统一驳回原因 (所选各战队可见) *
          </label>
          <VInput
            v-model:value="batchRejectReason"
            type="textarea"
            :rows="4"
            placeholder="请输入批量驳回的原因..."
          />
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-3">
          <VBtn clz="normal" size="medium" @click="showBatchRejectModal = false">
            取消
          </VBtn>
          <VBtn
            clz="danger"
            size="medium"
            :loading="batchOperating"
            :disabled="!batchRejectReason.trim()"
            @click="handleConfirmBatchReject"
          >
            确认批量驳回
          </VBtn>
        </div>
      </template>
    </VModal>

    <VModal
      v-model:show="showDetailModal"
      :title="`战队详情 - ${selectedTeamDetail?.teamName || ''}`"
      :width="720"
    >
      <div v-if="loadingDetail" class="py-12 text-center text-xs text-google-blue dark:text-google-blue-dark">
        正在拉取战队详细资料...
      </div>
      <div v-else-if="selectedTeamDetail" class="space-y-5">
        <div class="grid grid-cols-2 sm:grid-cols-3 gap-3 p-4 rounded-xl bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light dark:border-google-border-dark text-xs">
          <div>
            <span class="text-google-text-secondary dark:text-google-text-darkSecondary">战队名称:</span>
            <div class="font-semibold text-google-text-primary dark:text-google-text-darkPrimary mt-0.5">{{ selectedTeamDetail.teamName }}</div>
          </div>
          <div>
            <span class="text-google-text-secondary dark:text-google-text-darkSecondary">队长账号:</span>
            <div class="font-semibold text-google-text-primary dark:text-google-text-darkPrimary mt-0.5">{{ selectedTeamDetail.captainUsername }}</div>
          </div>
          <div>
            <span class="text-google-text-secondary dark:text-google-text-darkSecondary">所属机构:</span>
            <div class="font-semibold text-google-text-primary dark:text-google-text-darkPrimary mt-0.5">{{ selectedTeamDetail.organization || '-' }}</div>
          </div>
          <div>
            <span class="text-google-text-secondary dark:text-google-text-darkSecondary">成员人数:</span>
            <div class="font-semibold text-google-text-primary dark:text-google-text-darkPrimary mt-0.5">{{ selectedTeamDetail.members?.length || 1 }} 人</div>
          </div>
          <div>
            <span class="text-google-text-secondary dark:text-google-text-darkSecondary">报名时间:</span>
            <div class="text-google-text-primary dark:text-google-text-darkPrimary mt-0.5">{{ selectedTeamDetail.createdAt ? new Date(selectedTeamDetail.createdAt).toLocaleString() : '-' }}</div>
          </div>
          <div>
            <span class="text-google-text-secondary dark:text-google-text-darkSecondary">审核状态:</span>
            <div class="font-semibold mt-0.5" :class="getStatusTextColor(selectedTeamDetail.status)">{{ getStatusText(selectedTeamDetail.status) }}</div>
          </div>
        </div>

        <div>
          <h4 class="text-xs font-semibold text-google-text-primary dark:text-google-text-darkPrimary mb-2">
            战队成员名单 ({{ selectedTeamDetail.members?.length || 0 }} 人)
          </h4>
          <VTable
            :columns="memberColumns"
            :data="selectedTeamDetail.members || []"
            :pagination="false"
            size="small"
            :bordered="false"
          />
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end">
          <VBtn clz="normal" size="medium" @click="showDetailModal = false">
            关闭
          </VBtn>
        </div>
      </template>
    </VModal>
    <AdminCompetitionScoreExportModal
      v-if="compId"
      v-model:show="showExportModal"
      :competition-id="compId"
      competition-type="CTF"
      initial-scope="members"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, h } from 'vue'
import { useMessage, useDialog, NSwitch, type DataTableColumns } from 'naive-ui'
import {
  SearchOutline,
  RefreshOutline,
  CheckmarkCircleOutline,
  CloseCircleOutline,
  TrashOutline,
  InformationCircleOutline,
  BanOutline,
  CloudDownloadOutline
} from '@vicons/ionicons5'
import VBtn from '@/components/common/VBtn.vue'
import VInput from '@/components/common/VInput.vue'
import VSelection from '@/components/common/VSelection.vue'
import VTable from '@/components/common/VTable.vue'
import VPagination from '@/components/common/VPagination.vue'
import VModal from '@/components/common/VModal.vue'
import AdminCompetitionScoreExportModal from './components/AdminCompetitionScoreExportModal.vue'
import type {
  AdminTeamListVO,
  AdminTeamDetailVO,
  AdminTeamStatsVO,
  CompetitionMemberVO
} from '@/types/competition'
import {
  listAdminTeamsApi,
  getAdminTeamStatsApi,
  getAdminTeamDetailApi,
  auditAdminTeamApi,
  deleteAdminTeamApi,
  updateCompetitionApi,
  batchAuditAdminTeamsApi,
  batchDeleteAdminTeamsApi,
  getAdminCompetitionByIdApi
} from '@/api/competition'

defineOptions({
  name: 'AdminCtfCompetitionTeamView'
})

const props = defineProps<{
  compId: string | number
  requireTeamAudit?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:requireTeamAudit', val: boolean): void
}>()

const message = useMessage()
const dialog = useDialog()

const requireAudit = ref<boolean>(Boolean(props.requireTeamAudit))
const updatingAuditSwitch = ref(false)

const teams = ref<AdminTeamListVO[]>([])
const loadingTeams = ref(false)
const teamPage = ref(1)
const teamPageSize = ref(10)
const teamTotal = ref(0)
const teamStats = ref<AdminTeamStatsVO>({ total: 0, pending: 0, approved: 0, rejected: 0, banned: 0 })

const searchKeyword = ref('')
const selectedStatus = ref('')
const checkedRowKeys = ref<string[]>([])
const batchOperating = ref(false)
const showExportModal = ref(false)

const showRejectModal = ref(false)
const rejectingTeam = ref<AdminTeamListVO | null>(null)
const rejectReason = ref('')
const submittingReject = ref(false)

const showBatchRejectModal = ref(false)
const batchRejectReason = ref('')

const showDetailModal = ref(false)
const loadingDetail = ref(false)
const selectedTeamDetail = ref<AdminTeamDetailVO | null>(null)

const statusFilterOptions = [
  { label: '全部状态', value: '' },
  { label: '待审核', value: 'AUDIT_DRAFT' },
  { label: '已通过', value: 'AUDIT_PASSED' },
  { label: '已驳回', value: 'AUDIT_REJECTED' },
  { label: '已封禁', value: 'BANNED' }
]

const teamColumns = computed<DataTableColumns<AdminTeamListVO>>(() => [
  {
    type: 'selection',
    fixed: 'left',
    width: 48
  },
  {
    title: '战队名称',
    key: 'teamName',
    render(row) {
      return row.teamName
    }
  },
  {
    title: '队长用户名',
    key: 'captainUsername',
    width: 140,
    render(row) {
      return row.captainUsername
    }
  },
  {
    title: '成员人数',
    key: 'memberCount',
    width: 100,
    align: 'center',
    render(row) {
      return `${row.memberCount || 1} 人`
    }
  },
  {
    title: '审核状态',
    key: 'status',
    width: 100,
    align: 'center',
    render(row) {
      const isApproved = row.status === 'AUDIT_PASSED'
      const isRejected = row.status === 'AUDIT_REJECTED'
      const isBanned = row.status === 'BANNED'
      const label = isApproved ? '已通过' : isRejected ? '已驳回' : isBanned ? '已封禁' : '待审核'
      const clz = isApproved
        ? 'text-green-600 dark:text-green-400 font-medium'
        : isRejected
          ? 'text-red-600 dark:text-red-400 font-medium'
          : isBanned
            ? 'text-red-700 dark:text-red-500 font-semibold'
            : 'text-yellow-600 dark:text-yellow-400 font-medium'
      return h('span', { class: clz }, label)
    }
  },
  {
    title: '报名时间',
    key: 'createdAt',
    width: 180,
    render(row) {
      return row.createdAt ? new Date(row.createdAt).toLocaleString() : '-'
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 320,
    align: 'center',
    render(row) {
      return h('div', { class: 'flex items-center justify-center gap-1.5' }, [
        row.status !== 'AUDIT_PASSED' && row.status !== 'BANNED'
          ? h(
              VBtn,
              {
                clz: 'primary',
                size: 'small',
                leftIcon: CheckmarkCircleOutline,
                onClick: () => handleAuditSingle(row.id, 'AUDIT_PASSED')
              },
              { default: () => '通过' }
            )
          : null,
        row.status !== 'AUDIT_REJECTED' && row.status !== 'BANNED'
          ? h(
              VBtn,
              {
                clz: 'danger',
                size: 'small',
                leftIcon: CloseCircleOutline,
                onClick: () => openRejectModal(row)
              },
              { default: () => '驳回' }
            )
          : null,
        row.status !== 'BANNED'
          ? h(
              VBtn,
              {
                clz: 'danger',
                size: 'small',
                leftIcon: BanOutline,
                onClick: () => handleBanSingle(row)
              },
              { default: () => '封禁' }
            )
          : null,
        h(
          VBtn,
          {
            clz: 'normal',
            size: 'small',
            leftIcon: InformationCircleOutline,
            onClick: () => openDetailModal(row.id)
          },
          { default: () => '详情' }
        ),
        h(
          VBtn,
          {
            clz: 'danger',
            size: 'small',
            leftIcon: TrashOutline,
            onClick: () => handleDeleteSingle(row)
          },
          { default: () => '删除' }
        )
      ])
    }
  }
])

const memberColumns = computed<DataTableColumns<CompetitionMemberVO>>(() => [
  {
    title: '真实姓名',
    key: 'realName',
    render(row) {
      return row.realName || '-'
    }
  },
  {
    title: '用户名',
    key: 'username',
    render(row) {
      return row.username
    }
  },
  {
    title: '战队角色',
    key: 'role',
    width: 100,
    align: 'center',
    render(row) {
      const isCaptain = row.role === 'CAPTAIN'
      return h(
        'span',
        {
          class: isCaptain ? 'text-google-blue dark:text-google-blue-dark font-medium' : 'text-google-text-secondary dark:text-google-text-darkSecondary'
        },
        isCaptain ? '队长' : '队员'
      )
    }
  },
  {
    title: '联系手机',
    key: 'phone',
    width: 130,
    render(row) {
      return row.phone || '-'
    }
  },
  {
    title: '电子邮箱',
    key: 'email',
    render(row) {
      return row.email || '-'
    }
  }
])

function getStatusText(status?: string): string {
  if (status === 'AUDIT_PASSED') return '已通过'
  if (status === 'AUDIT_REJECTED') return '已驳回'
  if (status === 'BANNED') return '已封禁'
  return '待审核'
}

function getStatusTextColor(status?: string): string {
  if (status === 'AUDIT_PASSED') return 'text-green-600 dark:text-green-400'
  if (status === 'AUDIT_REJECTED') return 'text-red-600 dark:text-red-400'
  if (status === 'BANNED') return 'text-red-700 dark:text-red-500'
  return 'text-yellow-600 dark:text-yellow-400'
}

async function fetchTeams() {
  if (!props.compId) return
  loadingTeams.value = true
  try {
    const [listRes, statsRes] = await Promise.all([
      listAdminTeamsApi(props.compId, {
        page: teamPage.value,
        size: teamPageSize.value,
        keyword: searchKeyword.value.trim() || undefined,
        status: selectedStatus.value || undefined
      }),
      getAdminTeamStatsApi(props.compId)
    ])
    if (listRes.data) {
      teams.value = listRes.data.list ?? []
      teamTotal.value = Number(listRes.data.total) || 0
    }
    if (statsRes.data) {
      teamStats.value = statsRes.data
    }
  } catch {
    teams.value = []
  } finally {
    loadingTeams.value = false
  }
}

function handleSearch() {
  teamPage.value = 1
  checkedRowKeys.value = []
  fetchTeams()
}

function handleReset() {
  searchKeyword.value = ''
  selectedStatus.value = ''
  teamPage.value = 1
  checkedRowKeys.value = []
  fetchTeams()
}

function handleTeamPageChange(page: number) {
  teamPage.value = page
  fetchTeams()
}

function handleTeamPageSizeChange(pageSize: number) {
  teamPageSize.value = pageSize
  teamPage.value = 1
  fetchTeams()
}

function handleCheckedRowKeysChange(keys: any[]) {
  checkedRowKeys.value = keys.map(String)
}

async function handleToggleAuditSwitch(val: boolean) {
  updatingAuditSwitch.value = true
  try {
    await updateCompetitionApi(props.compId, {
      config: {
        requireTeamAudit: val
      }
    })
    requireAudit.value = val
    emit('update:requireTeamAudit', val)
    message.success(val ? '已开启报名资质人工审核模式' : '已切换为免审核直接参赛模式')
  } catch (error: any) {
    requireAudit.value = !val
    message.error(error.message || '更新审核配置失败')
  } finally {
    updatingAuditSwitch.value = false
  }
}

async function handleAuditSingle(teamId: number | string, status: any) {
  if (!props.compId) return
  try {
    await auditAdminTeamApi(props.compId, teamId, { status })
    message.success('审核操作成功')
    await fetchTeams()
  } catch (err: any) {
    message.error(err.message || '审核失败')
  }
}

function openRejectModal(row: AdminTeamListVO) {
  rejectingTeam.value = row
  rejectReason.value = ''
  showRejectModal.value = true
}

async function handleConfirmReject() {
  if (!props.compId || !rejectingTeam.value) return
  submittingReject.value = true
  try {
    await auditAdminTeamApi(props.compId, rejectingTeam.value.id, {
      status: 'AUDIT_REJECTED',
      auditReason: rejectReason.value.trim()
    })
    message.success('已驳回该战队报名')
    showRejectModal.value = false
    await fetchTeams()
  } catch (err: any) {
    message.error(err.message || '驳回失败')
  } finally {
    submittingReject.value = false
  }
}

function openBatchRejectModal() {
  batchRejectReason.value = ''
  showBatchRejectModal.value = true
}

async function handleConfirmBatchReject() {
  if (!props.compId || checkedRowKeys.value.length === 0) return
  batchOperating.value = true
  try {
    await batchAuditAdminTeamsApi(props.compId, {
      teamIds: checkedRowKeys.value,
      status: 'AUDIT_REJECTED',
      auditReason: batchRejectReason.value.trim()
    })
    message.success(`已批量驳回 ${checkedRowKeys.value.length} 支战队`)
    checkedRowKeys.value = []
    showBatchRejectModal.value = false
    await fetchTeams()
  } catch (err: any) {
    message.error(err.message || '批量驳回失败')
  } finally {
    batchOperating.value = false
  }
}

function handleBatchApprove() {
  if (!props.compId || checkedRowKeys.value.length === 0) return
  dialog.info({
    title: '批量通过资质审核',
    content: `确定要将选中的 ${checkedRowKeys.value.length} 支战队批量审核通过吗？`,
    positiveText: '确认通过',
    negativeText: '取消',
    onPositiveClick: async () => {
      batchOperating.value = true
      try {
        await batchAuditAdminTeamsApi(props.compId, {
          teamIds: checkedRowKeys.value,
          status: 'AUDIT_PASSED'
        })
        message.success(`已成功批量通过 ${checkedRowKeys.value.length} 支战队`)
        checkedRowKeys.value = []
        await fetchTeams()
      } catch (err: any) {
        message.error(err.message || '批量审核失败')
      } finally {
        batchOperating.value = false
      }
    }
  })
}

function handleBatchDelete() {
  if (!props.compId || checkedRowKeys.value.length === 0) return
  dialog.warning({
    title: '批量删除战队',
    content: `确定要彻底删除选中的 ${checkedRowKeys.value.length} 支战队吗？此操作不可逆！`,
    positiveText: '确认删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      batchOperating.value = true
      try {
        await batchDeleteAdminTeamsApi(props.compId, {
          teamIds: checkedRowKeys.value
        })
        message.success(`已成功删除 ${checkedRowKeys.value.length} 支战队`)
        checkedRowKeys.value = []
        await fetchTeams()
      } catch (err: any) {
        message.error(err.message || '批量删除失败')
      } finally {
        batchOperating.value = false
      }
    }
  })
}

function handleBanSingle(row: AdminTeamListVO) {
  if (!props.compId) return
  dialog.warning({
    title: '封禁参赛队伍',
    content: `确定要封禁战队「${row.teamName}」吗？被封禁后，该战队所有成员将无法再进入或参加本场比赛。`,
    positiveText: '确认封禁',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await auditAdminTeamApi(props.compId, row.id, {
          status: 'BANNED',
          auditReason: '管理员执行封禁操作'
        })
        message.success('战队已成功封禁')
        await fetchTeams()
      } catch (err: any) {
        message.error(err?.message || '封禁操作失败')
      }
    }
  })
}

function handleDeleteSingle(row: AdminTeamListVO) {
  if (!props.compId) return
  dialog.warning({
    title: '确认删除战队',
    content: `确定要删除战队「${row.teamName}」吗？`,
    positiveText: '确认删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteAdminTeamApi(props.compId, row.id)
        message.success('战队已成功删除')
        await fetchTeams()
      } catch (err: any) {
        message.error(err.message || '删除失败')
      }
    }
  })
}

async function openDetailModal(teamId: number | string) {
  if (!props.compId) return
  showDetailModal.value = true
  loadingDetail.value = true
  try {
    const res = await getAdminTeamDetailApi(props.compId, teamId)
    if (res.data) {
      selectedTeamDetail.value = res.data
    }
  } catch (err: any) {
    message.error(err.message || '拉取战队详情失败')
  } finally {
    loadingDetail.value = false
  }
}

async function initAuditSetting() {
  if (!props.compId || props.requireTeamAudit !== undefined) return
  try {
    const res = await getAdminCompetitionByIdApi(props.compId)
    if (res.data) {
      requireAudit.value = Boolean(res.data.requireTeamAudit)
    }
  } catch {
  }
}

onMounted(() => {
  initAuditSetting()
  fetchTeams()
})
</script>
