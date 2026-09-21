<template>
  <div class="competition-log-tab">
    <!-- Top Ribbon: Compact Metrics Statistics Cards in a single row -->
    <div class="stat-ribbon">
      <div class="stat-mini-card">
        <div class="stat-icon-wrapper primary">
          <n-icon size="18">
            <ReaderOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ logStats.totalLogs }}</div>
          <div class="stat-label">{{ t('admin.competitions.logs.stat_total', '全部操作流水') }}</div>
        </div>
      </div>

      <div class="stat-mini-card">
        <div class="stat-icon-wrapper info">
          <n-icon size="18">
            <CubeOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ logStats.containerLogs }}</div>
          <div class="stat-label">{{ t('admin.competitions.logs.stat_container', '靶机容器调度') }}</div>
        </div>
      </div>

      <div class="stat-mini-card">
        <div class="stat-icon-wrapper warning">
          <n-icon size="18">
            <FlagOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ logStats.flagLogs }}</div>
          <div class="stat-label">{{ t('admin.competitions.logs.stat_flag', 'Flag 提交流水') }}</div>
        </div>
      </div>

      <div class="stat-mini-card">
        <div class="stat-icon-wrapper error">
          <n-icon size="18">
            <BugOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ logStats.errorLogs }}</div>
          <div class="stat-label">{{ t('admin.competitions.logs.stat_error', '异常与错误') }}</div>
        </div>
      </div>
    </div>

    <!-- Main Table Card -->
    <VolurTableCard
      :bordered="true"
      :table-bordered="true"
      :scroll-x="1260"
      remote
      :columns="columns"
      :data="logList"
      :loading="loading"
      :pagination="pagination"
      :row-key="rowKey"
      table-class="competition-log-table"
      @update:sorter="handleSorterChange"
      @update:page="handlePageChange"
      @update:page-size="handlePageSizeChange"
    >
      <template #filter-left>
        <div class="log-filter-left">
          <n-input
            v-model:value="filterUsername"
            :placeholder="t('admin.competitions.logs.filter_username', '按用户名筛选...')"
            clearable
            class="filter-input-small"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <n-icon :component="SearchOutline" />
            </template>
          </n-input>

          <n-input
            v-model:value="filterTeamName"
            :placeholder="t('admin.competitions.logs.filter_team', '按队伍名筛选...')"
            clearable
            class="filter-input-small"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <n-icon :component="SearchOutline" />
            </template>
          </n-input>

          <n-input
            v-model:value="filterChallengeTitle"
            :placeholder="t('admin.competitions.logs.filter_challenge', '按题目名筛选...')"
            clearable
            class="filter-input-small"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <n-icon :component="SearchOutline" />
            </template>
          </n-input>

          <n-select
            v-model:value="filterLogType"
            :placeholder="t('admin.competitions.logs.filter_type', '操作类别')"
            clearable
            :options="typeSelectOptions"
            class="filter-type-select"
            @update:value="handleSearch"
          />

          <n-select
            v-model:value="filterStatus"
            :placeholder="t('admin.competitions.logs.filter_status', '状态')"
            clearable
            :options="statusSelectOptions"
            class="filter-status-select"
            @update:value="handleSearch"
          />

          <n-date-picker
            v-model:value="dateRange"
            type="datetimerange"
            clearable
            format="yyyy-MM-dd HH:mm"
            :start-placeholder="t('admin.competitions.logs.start_time', '起始时间')"
            :end-placeholder="t('admin.competitions.logs.end_time', '结束时间')"
            class="filter-date-picker"
            @update:value="handleSearch"
          />

          <div class="filter-actions-group">
            <n-button type="primary" class="theme-primary-btn" @click="handleSearch">
              <template #icon>
                <n-icon :component="SearchOutline" />
              </template>
              {{ t('common.search', '查询') }}
            </n-button>

            <n-button quaternary @click="handleReset">
              <template #icon>
                <n-icon :component="RefreshOutline" />
              </template>
              {{ t('common.reset', '重置') }}
            </n-button>
          </div>
        </div>
      </template>
    </VolurTableCard>

    <!-- Competition Log Detail Drawer -->
    <n-drawer v-model:show="showDetailDrawer" :width="560" placement="right">
      <n-drawer-content :title="t('admin.competitions.logs.drawer_title', '竞赛日志详情')" closable>
        <div v-if="selectedLog" class="drawer-detail-body">
          <n-descriptions label-placement="left" :column="1" bordered size="small">
            <n-descriptions-item :label="t('admin.competitions.logs.detail.id', '流水 ID')">
              <span class="id-text">{{ selectedLog.id }}</span>
            </n-descriptions-item>
            <n-descriptions-item :label="t('admin.competitions.logs.detail.status', '操作状态')">
              <n-tag :type="selectedLog.status ? 'success' : 'error'" size="small">
                {{ selectedLog.status ? t('admin.competitions.logs.status_success', '成功') : t('admin.competitions.logs.status_failed', '失败') }}
              </n-tag>
            </n-descriptions-item>
            <n-descriptions-item :label="t('admin.competitions.logs.detail.type', '操作类别')">
              <n-tag :type="getTypeTagType(selectedLog.logType)" size="small" round>
                {{ selectedLog.logType }}
              </n-tag>
            </n-descriptions-item>
            <n-descriptions-item :label="t('admin.competitions.logs.detail.action', '操作动作')">
              <strong>{{ selectedLog.action || '-' }}</strong>
            </n-descriptions-item>
            <n-descriptions-item :label="t('admin.competitions.logs.detail.user', '操作选手')">
              {{ selectedLog.username || 'ANONYMOUS' }} (ID: {{ selectedLog.userId || '-' }})
            </n-descriptions-item>
            <n-descriptions-item :label="t('admin.competitions.logs.detail.team', '所属队伍')">
              {{ selectedLog.teamName || '-' }} (ID: {{ selectedLog.teamId || '-' }})
            </n-descriptions-item>
            <n-descriptions-item :label="t('admin.competitions.logs.detail.challenge', '关联题目')">
              {{ selectedLog.challengeTitle || '-' }} (ID: {{ selectedLog.challengeId || '-' }})
            </n-descriptions-item>
            <n-descriptions-item :label="t('admin.competitions.logs.detail.ip', '客户端 IP')">
              <span class="id-text">{{ selectedLog.operatorIp || '-' }}</span>
            </n-descriptions-item>
            <n-descriptions-item :label="t('admin.competitions.logs.detail.uri', '请求路径')">
              <span class="id-text">{{ selectedLog.requestMethod }} {{ selectedLog.requestUri }}</span>
            </n-descriptions-item>
            <n-descriptions-item :label="t('admin.competitions.logs.detail.duration', '执行耗时')">
              <span class="duration-text">{{ selectedLog.durationMs ? `${selectedLog.durationMs} ms` : '-' }}</span>
            </n-descriptions-item>
            <n-descriptions-item :label="t('admin.competitions.logs.detail.created_at', '记录时间')">
              {{ formatDateTime(selectedLog.createdAt) }}
            </n-descriptions-item>
          </n-descriptions>

          <div v-if="selectedLog.params" class="code-section">
            <div class="section-title">{{ t('admin.competitions.logs.detail.request_params', '请求参数') }}</div>
            <pre class="code-block">{{ formatJson(selectedLog.params) }}</pre>
          </div>

          <div v-if="selectedLog.result" class="code-section">
            <div class="section-title">{{ t('admin.competitions.logs.detail.response_result', '响应结果') }}</div>
            <pre class="code-block">{{ formatJson(selectedLog.result) }}</pre>
          </div>

          <div v-if="selectedLog.errorMessage" class="code-section error-box">
            <div class="section-title error-title">{{ t('admin.competitions.logs.detail.error_details', '异常与错误信息') }}</div>
            <pre class="code-block error-block">{{ selectedLog.errorMessage }}</pre>
          </div>

          <div v-if="selectedLog.userAgent" class="code-section">
            <div class="section-title">{{ t('admin.competitions.logs.detail.user_agent', '客户端 User-Agent') }}</div>
            <div class="ua-text">{{ selectedLog.userAgent }}</div>
          </div>
        </div>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, h, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  NIcon,
  NInput,
  NSelect,
  NDatePicker,
  NButton,
  NDrawer,
  NDrawerContent,
  NDescriptions,
  NDescriptionsItem,
  NTag,
  type DataTableColumns,
  type SelectOption
} from 'naive-ui'
import { VolurTableCard } from '@/public/volurEx'
import {
  ReaderOutline,
  CubeOutline,
  FlagOutline,
  BugOutline,
  SearchOutline,
  RefreshOutline,
  EyeOutline
} from '@vicons/ionicons5'
import {
  getAdminCompetitionLogListApi,
  getAdminCompetitionLogStatisticsApi
} from '@/api/competition'
import type {
  AdminCompetitionLogVO,
  AdminCompetitionLogStatisticsVO,
  AdminCompetitionLogQueryDTO
} from '@/types/competition'

const props = defineProps<{
  compId: string | number
}>()

const { t, locale } = useI18n()

// Statistics State
const logStats = reactive<AdminCompetitionLogStatisticsVO>({
  totalLogs: 0,
  containerLogs: 0,
  flagLogs: 0,
  errorLogs: 0
})

// Query & Filter State
const filterUsername = ref('')
const filterTeamName = ref('')
const filterChallengeTitle = ref('')
const filterLogType = ref<string | null>(null)
const filterStatus = ref<string | null>(null)
const dateRange = ref<[number, number] | null>(null)
const sortBy = ref('createdAt')
const sortOrder = ref<'asc' | 'desc'>('desc')

const typeSelectOptions = computed<SelectOption[]>(() => [
  { label: t('admin.competitions.logs.type_all', '全部类别'), value: '' },
  { label: t('admin.competitions.logs.type_container', '容器调度 (CONTAINER)'), value: 'CONTAINER' },
  { label: t('admin.competitions.logs.type_flag', 'Flag提交 (FLAG)'), value: 'FLAG' },
  { label: t('admin.competitions.logs.type_team', '战队变更 (TEAM)'), value: 'TEAM' },
  { label: t('admin.competitions.logs.type_defense', '靶机防御 (DEFENSE)'), value: 'DEFENSE' },
  { label: t('admin.competitions.logs.type_theory', '理论答卷 (THEORY)'), value: 'THEORY' }
])

const statusSelectOptions = computed<SelectOption[]>(() => [
  { label: t('admin.competitions.logs.status_all', '全部状态'), value: '' },
  { label: t('admin.competitions.logs.status_success', '成功'), value: 'true' },
  { label: t('admin.competitions.logs.status_failed', '失败'), value: 'false' }
])

// Table & Pagination State
const loading = ref(false)
const logList = ref<AdminCompetitionLogVO[]>([])
const rowKey = (row: AdminCompetitionLogVO) => String(row.id)
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
  prefix: (info: { itemCount?: number }) => t('admin.logs.total_prefix', { count: Number(info.itemCount) || 0 })
})

// Drawer State
const showDetailDrawer = ref(false)
const selectedLog = ref<AdminCompetitionLogVO | null>(null)

function getTypeTagType(type?: string): 'info' | 'warning' | 'primary' | 'success' | 'default' {
  switch (type) {
    case 'CONTAINER': return 'info'
    case 'FLAG': return 'warning'
    case 'TEAM': return 'primary'
    case 'DEFENSE': return 'success'
    default: return 'default'
  }
}

function formatDateTime(timeStr?: string): string {
  if (!timeStr) return '-'
  const date = new Date(timeStr)
  return date.toLocaleString(locale.value === 'zh_CN' ? 'zh-CN' : 'en-US', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

function formatJson(jsonStr?: string): string {
  if (!jsonStr) return '-'
  try {
    const obj = JSON.parse(jsonStr)
    return JSON.stringify(obj, null, 2)
  } catch {
    return jsonStr
  }
}

// Table Columns Definition
const columns = computed<DataTableColumns<AdminCompetitionLogVO>>(() => [
  {
    title: t('admin.competitions.logs.columns.id', 'ID'),
    key: 'id',
    width: 170,
    render(row) {
      return h('span', { class: 'id-text' }, String(row.id))
    }
  },
  {
    title: t('admin.competitions.logs.columns.type', '类别'),
    key: 'logType',
    width: 110,
    render(row) {
      return h(
        NTag,
        {
          type: getTypeTagType(row.logType),
          size: 'small',
          round: true
        },
        { default: () => row.logType || 'OTHER' }
      )
    }
  },
  {
    title: t('admin.competitions.logs.columns.action', '操作动作'),
    key: 'action',
    minWidth: 150,
    render(row) {
      return h('span', { class: 'action-text' }, row.action || '-')
    }
  },
  {
    title: t('admin.competitions.logs.columns.user', '选手 / IP'),
    key: 'username',
    minWidth: 150,
    render(row) {
      return h(
        'div',
        {
          class: 'operator-cell',
          style: {
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'center',
            gap: '2px',
            minWidth: '0'
          }
        },
        [
          h(
            'span',
            {
              class: 'operator-name',
              title: row.username || 'ANONYMOUS',
              style: {
                fontSize: '13px',
                fontWeight: '600',
                color: 'var(--v-text-primary, #111111)',
                lineHeight: '1.3',
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap'
              }
            },
            row.username || 'ANONYMOUS'
          ),
          row.operatorIp
            ? h(
                'span',
                {
                  class: 'operator-ip',
                  title: row.operatorIp,
                  style: {
                    fontSize: '11px',
                    color: 'var(--v-text-desc, #444444)',
                    lineHeight: '1.3',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap'
                  }
                },
                row.operatorIp
              )
            : h(
                'span',
                {
                  class: 'operator-ip-empty',
                  style: {
                    fontSize: '11px',
                    color: 'var(--v-text-disabled, #999999)',
                    lineHeight: '1.3',
                    fontStyle: 'italic'
                  }
                },
                '-'
              )
        ]
      )
    }
  },
  {
    title: t('admin.competitions.logs.columns.team', '战队名称'),
    key: 'teamName',
    minWidth: 140,
    render(row) {
      return h('span', { class: 'team-text' }, row.teamName || '-')
    }
  },
  {
    title: t('admin.competitions.logs.columns.challenge', '对应题目'),
    key: 'challengeTitle',
    minWidth: 150,
    render(row) {
      return h('span', { class: 'challenge-text' }, row.challengeTitle || '-')
    }
  },
  {
    title: t('admin.competitions.logs.columns.duration', '耗时'),
    key: 'durationMs',
    width: 90,
    sorter: true,
    render(row) {
      return h('span', { class: 'duration-badge' }, row.durationMs != null ? `${row.durationMs}ms` : '-')
    }
  },
  {
    title: t('admin.competitions.logs.columns.status', '状态'),
    key: 'status',
    width: 90,
    render(row) {
      return h(
        NTag,
        {
          type: row.status ? 'success' : 'error',
          size: 'small'
        },
        { default: () => (row.status ? t('admin.competitions.logs.status_success', '成功') : t('admin.competitions.logs.status_failed', '失败')) }
      )
    }
  },
  {
    title: t('admin.competitions.logs.columns.created_at', '时间'),
    key: 'createdAt',
    width: 170,
    sorter: true,
    render(row) {
      return formatDateTime(row.createdAt)
    }
  },
  {
    title: t('common.actions', '操作'),
    key: 'actions',
    width: 80,
    fixed: 'right',
    render(row) {
      return h(
        NButton,
        {
          quaternary: true,
          size: 'tiny',
          type: 'primary',
          onClick: () => openLogDetail(row)
        },
        {
          icon: () => h(NIcon, null, { default: () => h(EyeOutline) }),
          default: () => t('common.detail', '详情')
        }
      )
    }
  }
])

// Data Fetching
async function fetchLogStats() {
  if (!props.compId) return
  try {
    const res = await getAdminCompetitionLogStatisticsApi(props.compId)
    if (res.data) {
      Object.assign(logStats, res.data)
    }
  } catch (error) {
    console.error('Failed to fetch competition log statistics:', error)
  }
}

let currentFetchRequestId = 0

async function fetchLogList() {
  if (!props.compId) return
  const requestId = ++currentFetchRequestId
  loading.value = true
  try {
    const params: AdminCompetitionLogQueryDTO = {
      page: pagination.page,
      size: pagination.pageSize,
      username: filterUsername.value.trim() || undefined,
      teamName: filterTeamName.value.trim() || undefined,
      challengeTitle: filterChallengeTitle.value.trim() || undefined,
      logType: filterLogType.value || undefined,
      status: filterStatus.value === 'true' ? true : filterStatus.value === 'false' ? false : undefined,
      sortBy: sortBy.value,
      sortOrder: sortOrder.value
    }

    if (dateRange.value && dateRange.value.length === 2) {
      params.startTime = new Date(dateRange.value[0]).toISOString()
      params.endTime = new Date(dateRange.value[1]).toISOString()
    }

    const res = await getAdminCompetitionLogListApi(props.compId, params)
    if (requestId !== currentFetchRequestId) return
    if (res.data) {
      logList.value = res.data.list || []
      pagination.itemCount = Number(res.data.total) || 0
      if (res.data.currentPage !== undefined && res.data.currentPage !== null) {
        pagination.page = Number(res.data.currentPage) || pagination.page
      }
    }
  } catch (error) {
    if (requestId !== currentFetchRequestId) return
    console.error('Failed to fetch competition log list:', error)
  } finally {
    if (requestId === currentFetchRequestId) {
      loading.value = false
    }
  }
}

function handleSorterChange(sorter: { columnKey?: string; order?: 'ascend' | 'descend' | false }) {
  if (sorter && sorter.order) {
    sortBy.value = sorter.columnKey || 'createdAt'
    sortOrder.value = sorter.order === 'ascend' ? 'asc' : 'desc'
  } else {
    sortBy.value = 'createdAt'
    sortOrder.value = 'desc'
  }
  fetchLogList()
}

function handleSearch() {
  pagination.page = 1
  fetchLogList()
  fetchLogStats()
}

function handleReset() {
  filterUsername.value = ''
  filterTeamName.value = ''
  filterChallengeTitle.value = ''
  filterLogType.value = null
  filterStatus.value = null
  dateRange.value = null
  pagination.page = 1
  sortBy.value = 'createdAt'
  sortOrder.value = 'desc'
  fetchLogList()
  fetchLogStats()
}

function handlePageChange(page: number) {
  pagination.page = page
  fetchLogList()
}

function handlePageSizeChange(pageSize: number) {
  pagination.pageSize = pageSize
  pagination.page = 1
  fetchLogList()
}

function openLogDetail(row: AdminCompetitionLogVO) {
  selectedLog.value = row
  showDetailDrawer.value = true
}

watch(
  () => props.compId,
  (newId) => {
    if (newId) {
      fetchLogStats()
      fetchLogList()
    }
  }
)

onMounted(() => {
  if (props.compId) {
    fetchLogStats()
    fetchLogList()
  }
})
</script>

<style scoped lang="scss">
.competition-log-tab {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stat-ribbon {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;

  @media (max-width: 992px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 576px) {
    grid-template-columns: 1fr;
  }
}

.stat-mini-card {
  background-color: var(--v-bg-card, #ffffff);
  border-radius: 10px;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  border: 1px solid var(--v-border-base, #e5e4e7);
  box-shadow: var(--v-shadow-sm, 0 1px 2px rgba(0, 0, 0, 0.02));
  transition: background-color 0.3s ease, border-color 0.3s ease, box-shadow 0.2s ease;

  &:hover {
    border-color: var(--v-border-focus, #bbbbbb);
    box-shadow: var(--v-shadow-md, 0 2px 6px rgba(0, 0, 0, 0.05));
  }

  .stat-icon-wrapper {
    width: 38px;
    height: 38px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;

    &.primary {
      background-color: rgba(99, 102, 241, 0.1);
      color: #6366f1;
    }

    &.info {
      background-color: rgba(59, 130, 246, 0.1);
      color: #3b82f6;
    }

    &.warning {
      background-color: rgba(245, 158, 11, 0.1);
      color: #f59e0b;
    }

    &.error {
      background-color: rgba(239, 68, 68, 0.1);
      color: #ef4444;
    }
  }

  .stat-info {
    display: flex;
    flex-direction: column;
    gap: 2px;

    .stat-value {
      font-size: 20px;
      font-weight: 700;
      color: var(--v-text-primary, #111111);
      line-height: 1.2;
    }

    .stat-label {
      font-size: 12px;
      color: var(--v-text-desc, #444444);
      font-weight: 500;
    }
  }
}

.action-text {
  font-weight: 600;
  font-size: 13px;
  color: var(--v-text-primary, #111111);
}

.team-text,
.challenge-text {
  font-size: 13px;
  color: var(--v-text-primary, #111111);
}

.operator-cell {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 2px;
  min-width: 0;

  .operator-name {
    font-size: 13px;
    font-weight: 600;
    color: var(--v-text-primary, #111111);
    line-height: 1.3;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .operator-ip {
    font-size: 11px;
    color: var(--v-text-desc, #444444);
    line-height: 1.3;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .operator-ip-empty {
    font-size: 11px;
    color: var(--v-text-disabled, #999999);
    line-height: 1.3;
    font-style: italic;
  }
}

.duration-badge {
  font-family: 'Fira Code', monospace;
  font-size: 12px;
  color: #6366f1;
}

.drawer-detail-body {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .duration-text {
    font-family: 'Fira Code', monospace;
    font-weight: 600;
    color: #6366f1;
  }

  .code-section {
    display: flex;
    flex-direction: column;
    gap: 6px;

    .section-title {
      font-size: 13px;
      font-weight: 600;
      color: #444444;

      &.error-title {
        color: #ef4444;
      }
    }

    .code-block {
      background-color: #1e1e1e;
      color: #d4d4d4;
      padding: 12px;
      border-radius: 8px;
      font-family: 'Fira Code', monospace;
      font-size: 12px;
      overflow-x: auto;
      white-space: pre-wrap;
      margin: 0;

      &.error-block {
        background-color: rgba(239, 68, 68, 0.08);
        color: #dc2626;
        border: 1px solid rgba(239, 68, 68, 0.2);
      }
    }

    .ua-text {
      font-size: 12px;
      color: #666666;
      background-color: var(--v-bg-page, #f8f9fa);
      padding: 8px 12px;
      border-radius: 6px;
      word-break: break-all;
    }
  }
}

.log-filter-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;

  .filter-input-small {
    width: 140px;
  }

  .filter-type-select {
    width: 150px;
  }

  .filter-status-select {
    width: 110px;
  }

  .filter-date-picker {
    width: 270px;
  }

  .filter-actions-group {
    display: flex;
    align-items: center;
    gap: 8px;

    .theme-primary-btn {
      background-color: #000000;
      color: #ffffff;
      border-color: #000000;

      &:hover,
      &:focus {
        background-color: #222222;
        border-color: #222222;
        color: #ffffff;
      }
    }
  }
}

@media (max-width: 768px) {
  .log-filter-left {
    flex-direction: column;
    align-items: stretch;
    width: 100%;
    gap: 10px;

    .filter-input-small,
    .filter-type-select,
    .filter-status-select,
    .filter-date-picker {
      width: 100% !important;
    }

    .filter-actions-group {
      display: flex;
      width: 100%;
      gap: 10px;

      .n-button {
        flex: 1;
        justify-content: center;
      }
    }
  }
}
</style>
