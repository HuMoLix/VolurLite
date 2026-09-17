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
    <AppTableCard
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
        <div class="tab-filter-left">
          <n-input
            v-model:value="searchKeyword"
            placeholder="搜索队伍名称或队长用户名..."
            clearable
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
            class="filter-status-select"
            @update:value="handleSearch"
          />
          <n-button type="primary" class="theme-primary-btn" @click="handleSearch">查询</n-button>
        </div>
      </template>

      <template #filter-right>
        <div class="tab-filter-right">
          <n-button quaternary :loading="loading" @click="() => fetchTeams()">
            <template #icon>
              <n-icon :component="RefreshOutline" />
            </template>
            刷新列表
          </n-button>
        </div>
      </template>

      <template #empty>
        <div style="padding: 24px; text-align: center; color: #888;">
          当前比赛暂无报名的参赛队伍
        </div>
      </template>
    </AppTableCard>

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
          <div class="modal-actions">
            <n-button @click="showRejectModal = false">取消</n-button>
            <n-button
              type="error"
              :loading="submittingReject"
              :disabled="!rejectReason.trim()"
              @click="handleConfirmReject"
            >
              确认驳回
            </n-button>
          </div>
        </n-form>
      </div>
    </n-modal>

    <!-- Team Full Details Modal -->
    <n-modal
      v-model:show="showDetailModal"
      preset="card"
      :title="`参赛队伍详情 - ${detailTeam?.teamName || ''}`"
      style="width: 760px; max-height: 85vh;"
    >
      <n-spin :show="detailLoading">
        <div v-if="detailTeam" class="team-detail-container">
          <!-- Overview Card -->
          <div class="team-overview-row">
            <AppAvatar
              round
              :size="56"
              :src="detailTeam.avatarUrl"
              :name="detailTeam.teamName"
            />
            <div class="team-base-info">
              <div class="t-name-row">
                <h3 class="team-title">{{ detailTeam.teamName }}</h3>
                <n-tag size="small" :type="getTeamStatusTag(detailTeam.status)" round>
                  {{ getTeamStatusLabel(detailTeam.status) }}
                </n-tag>
              </div>
              <div class="meta-tags">
                <span class="meta-item">队长：<strong>{{ detailTeam.captainUsername }}</strong></span>
                <span class="meta-item">报名时间：{{ new Date(detailTeam.createdAt).toLocaleString() }}</span>
              </div>
            </div>
          </div>

          <!-- Reject Reason Alert if Rejected -->
          <n-alert
            v-if="detailTeam.status === 'AUDIT_REJECTED' && detailTeam.auditReason"
            type="error"
            title="历史审核驳回原因"
            class="reject-alert-box"
          >
            {{ detailTeam.auditReason }}
          </n-alert>

          <!-- Members List -->
          <div class="detail-section">
            <h4 class="sec-title">战队成员名册 ({{ detailTeam.members?.length || 0 }} 人)</h4>
            <div class="members-grid">
              <div
                v-for="m in detailTeam.members || []"
                :key="m.userId"
                class="member-card"
              >
                <AppAvatar
                  round
                  :size="40"
                  :src="m.avatarUrl"
                  :name="m.realName || m.username"
                />
                <div class="m-info">
                  <div class="m-top">
                    <span class="uname">{{ m.realName || m.username }}</span>
                    <n-tag
                      size="tiny"
                      :type="m.role === 'CAPTAIN' ? 'primary' : m.role === 'LEADER' ? 'warning' : 'default'"
                      round
                    >
                      {{ m.role === 'CAPTAIN' ? '队长' : m.role === 'LEADER' ? '领队' : '队员' }}
                    </n-tag>
                  </div>
                  <div class="m-detail-line" v-if="m.username && m.realName">
                    <span class="sub-label">账号：</span>{{ m.username }}
                  </div>
                  <div class="m-detail-line" v-if="m.gender">
                    <span class="sub-label">性别：</span>{{ m.gender === 'MALE' ? '男' : m.gender === 'FEMALE' ? '女' : '未知' }}
                  </div>
                  <div class="m-detail-line" v-if="m.studentNumber">
                    <span class="sub-label">学号：</span>{{ m.studentNumber }}
                  </div>
                  <div class="m-detail-line" v-if="m.phone">
                    <span class="sub-label">手机：</span>{{ m.phone }}
                  </div>
                  <div class="m-detail-line" v-if="m.email">
                    <span class="sub-label">邮箱：</span>{{ m.email }}
                  </div>
                  <div class="m-detail-line" v-if="m.idCard">
                    <span class="sub-label">身份证：</span>{{ m.idCard }}
                  </div>
                  <div class="m-detail-line" v-if="m.college">
                    <span class="sub-label">学院：</span>{{ m.college }}
                  </div>
                  <span class="join-time">加入：{{ m.joinedAt ? new Date(m.joinedAt).toLocaleDateString() : '—' }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </n-spin>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted, watch } from 'vue'
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
  NForm,
  NFormItem,
  NAlert,
  useMessage,
  useDialog,
  type DataTableColumns,
  type SelectOption
} from 'naive-ui'
import AppTableCard from '@/components/common/AppTableCard.vue'
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
    showRejectModal.value = false
    fetchTeams()
  } catch (error: any) {
    message.error(error.message || '驳回失败')
  } finally {
    submittingReject.value = false
  }
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
}

.reject-modal-content {
  .modal-actions {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    margin-top: 20px;
  }
}

.team-detail-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-height: 70vh;
  overflow-y: auto;
  padding-right: 6px;

  .team-overview-row {
    display: flex;
    align-items: center;
    gap: 16px;
    background: var(--v-bg-muted, #f8f8f9);
    padding: 16px 20px;
    border-radius: 8px;

    .team-base-info {
      display: flex;
      flex-direction: column;
      gap: 4px;
      flex: 1;

      .t-name-row {
        display: flex;
        align-items: center;
        gap: 10px;

        .team-title {
          font-size: 18px;
          font-weight: 700;
          margin: 0;
          color: var(--v-text-primary, #111111);
        }
      }

      .slogan {
        font-size: 13px;
        color: #444444;
        margin: 0;
      }

      .meta-tags {
        display: flex;
        align-items: center;
        gap: 14px;
        font-size: 12px;
        color: var(--v-text-muted, #999999);
        margin-top: 4px;

        code {
          background: rgba(0,0,0,0.06);
          padding: 2px 6px;
          border-radius: 4px;
          color: var(--v-text-primary, #111);
        }
      }
    }
  }

  .reject-alert-box {
    margin: 0;
  }

  .detail-section {
    .sec-title {
      font-size: 15px;
      font-weight: 600;
      color: var(--v-text-primary, #111111);
      margin: 0 0 12px 0;
      padding-bottom: 6px;
      border-bottom: 1px solid var(--v-border-base, #e5e4e7);
    }

    .members-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 12px;

      @media (max-width: 600px) {
        grid-template-columns: 1fr;
      }

      .member-card {
        display: flex;
        align-items: center;
        gap: 12px;
        background: var(--v-bg-card, #ffffff);
        padding: 12px;
        border: 1px solid var(--v-border-base, #e5e4e7);
        border-radius: 6px;

        .m-info {
          display: flex;
          flex-direction: column;
          gap: 2px;
          flex: 1;

          .m-top {
            display: flex;
            align-items: center;
            justify-content: space-between;

            .uname {
              font-size: 13px;
              font-weight: 600;
              color: var(--v-text-primary, #111111);
            }
          }

          .email {
            font-size: 11px;
            color: #444444;
          }

          .m-detail-line {
            font-size: 12px;
            color: var(--v-text-primary, #222222);
            word-break: break-all;
            line-height: 1.4;

            .sub-label {
              color: #444444;
              font-weight: 500;
            }
          }

          .join-time {
            font-size: 11px;
            color: var(--v-text-muted, #999999);
            margin-top: 4px;
          }
        }
      }
    }
  }

  .tab-filter-left {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;

    .filter-search-input {
      width: 240px;
    }

    .filter-status-select {
      width: 150px;
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
</style>
