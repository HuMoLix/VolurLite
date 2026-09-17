<template>
  <div class="admin-log-view">
    <!-- Top section: Compact Metrics Statistics Cards in a single row -->
    <div class="stat-ribbon">
      <div class="stat-mini-card">
        <div class="stat-icon-wrapper primary">
          <n-icon size="18">
            <ReaderOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ logStats.totalLogs }}</div>
          <div class="stat-label">{{ t('admin.logs.stat_total') }}</div>
        </div>
      </div>

      <div class="stat-mini-card">
        <div class="stat-icon-wrapper info">
          <n-icon size="18">
            <KeyOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ logStats.authLogs }}</div>
          <div class="stat-label">{{ t('admin.logs.stat_auth') }}</div>
        </div>
      </div>

      <div class="stat-mini-card">
        <div class="stat-icon-wrapper warning">
          <n-icon size="18">
            <ShieldCheckmarkOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ logStats.securityLogs }}</div>
          <div class="stat-label">{{ t('admin.logs.stat_security') }}</div>
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
          <div class="stat-label">{{ t('admin.logs.stat_error') }}</div>
        </div>
      </div>
    </div>

    <!-- Bottom section: Log Table Card -->
    <AppTableCard
      :bordered="true"
      :table-bordered="true"
      :scroll-x="1140"
      remote
      :columns="columns"
      :data="logList"
      :loading="loading"
      :pagination="pagination"
      :row-key="rowKey"
      table-class="log-data-table"
      @update:sorter="handleSorterChange"
      @update:page="handlePageChange"
      @update:page-size="handlePageSizeChange"
    >
      <template #filter-left>
        <div class="log-filter-left">
          <n-input
            v-model:value="searchKeyword"
            :placeholder="t('admin.logs.search_placeholder')"
            clearable
            class="filter-search-input"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <n-icon :component="SearchOutline" />
            </template>
          </n-input>

          <n-select
            v-model:value="filterLevel"
            :placeholder="t('admin.logs.filter_level')"
            clearable
            :options="levelSelectOptions"
            class="filter-level-select"
            @update:value="handleSearch"
          />

          <n-select
            v-model:value="filterModule"
            :placeholder="t('admin.logs.filter_module')"
            clearable
            :options="moduleSelectOptions"
            class="filter-module-select"
            @update:value="handleSearch"
          />

          <n-select
            v-model:value="filterStatus"
            :placeholder="t('admin.logs.filter_status')"
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
            :start-placeholder="t('admin.logs.start_time')"
            :end-placeholder="t('admin.logs.end_time')"
            class="filter-date-picker"
            @update:value="handleSearch"
          />

          <div class="filter-actions-group">
            <n-button type="primary" @click="handleSearch" class="theme-primary-btn">
              <template #icon>
                <n-icon :component="SearchOutline" />
              </template>
              {{ t('admin.logs.query_btn') }}
            </n-button>

            <n-button quaternary @click="handleReset">
              <template #icon>
                <n-icon :component="RefreshOutline" />
              </template>
              {{ t('admin.logs.reset_btn') }}
            </n-button>
          </div>
        </div>
      </template>
    </AppTableCard>

    <!-- Log Detail Drawer -->
    <n-drawer v-model:show="showDetailDrawer" :width="560" placement="right">
      <n-drawer-content :title="t('admin.logs.drawer_title')" closable>
        <div v-if="selectedLog" class="drawer-detail-body">
          <n-descriptions label-placement="left" :column="1" bordered size="small">
            <n-descriptions-item :label="t('admin.logs.detail.id')">
              <span class="id-text">{{ selectedLog.id }}</span>
            </n-descriptions-item>
            <n-descriptions-item :label="t('admin.logs.detail.level')">
              <n-tag :type="getLevelTagType(selectedLog.level)" size="small" round>
                {{ selectedLog.level }}
              </n-tag>
            </n-descriptions-item>
            <n-descriptions-item :label="t('admin.logs.detail.module')">
              <n-tag size="small">{{ selectedLog.module || '-' }}</n-tag>
            </n-descriptions-item>
            <n-descriptions-item :label="t('admin.logs.detail.action')">
              <strong>{{ selectedLog.action || '-' }}</strong>
            </n-descriptions-item>
            <n-descriptions-item :label="t('admin.logs.detail.operator')">
              {{ selectedLog.operator || 'ANONYMOUS' }}
            </n-descriptions-item>
            <n-descriptions-item :label="t('admin.logs.detail.ip')">
              <span class="id-text">{{ selectedLog.operatorIp || '-' }}</span>
            </n-descriptions-item>
            <n-descriptions-item :label="t('admin.logs.detail.uri')">
              <span class="id-text">{{ selectedLog.requestMethod }} {{ selectedLog.requestUri }}</span>
            </n-descriptions-item>
            <n-descriptions-item :label="t('admin.logs.detail.duration')">
              <span class="duration-text">{{ selectedLog.durationMs ? `${selectedLog.durationMs} ms` : '-' }}</span>
            </n-descriptions-item>
            <n-descriptions-item :label="t('admin.logs.detail.created_at')">
              {{ formatDateTime(selectedLog.createdAt) }}
            </n-descriptions-item>
            <n-descriptions-item :label="t('admin.logs.detail.status')">
              <n-tag :type="selectedLog.status ? 'success' : 'error'" size="small">
                {{ selectedLog.status ? t('admin.logs.detail.status_success') : t('admin.logs.detail.status_failed') }}
              </n-tag>
            </n-descriptions-item>
          </n-descriptions>

          <div v-if="selectedLog.params" class="code-section">
            <div class="section-title">{{ t('admin.logs.detail.request_params') }}</div>
            <pre class="code-block">{{ formatJson(selectedLog.params) }}</pre>
          </div>

          <div v-if="selectedLog.result" class="code-section">
            <div class="section-title">{{ t('admin.logs.detail.response_result') }}</div>
            <pre class="code-block">{{ formatJson(selectedLog.result) }}</pre>
          </div>

          <div v-if="selectedLog.errorMessage" class="code-section error-box">
            <div class="section-title error-title">{{ t('admin.logs.detail.error_details') }}</div>
            <pre class="code-block error-block">{{ selectedLog.errorMessage }}</pre>
          </div>

          <div v-if="selectedLog.userAgent" class="code-section">
            <div class="section-title">{{ t('admin.logs.detail.user_agent') }}</div>
            <div class="ua-text">{{ selectedLog.userAgent }}</div>
          </div>
        </div>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, h, onMounted } from 'vue'
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
import AppTableCard from '@/components/common/AppTableCard.vue'
import {
  ReaderOutline,
  KeyOutline,
  ShieldCheckmarkOutline,
  BugOutline,
  SearchOutline,
  RefreshOutline,
  EyeOutline
} from '@vicons/ionicons5'
import { getAdminLogListApi, getAdminLogStatisticsApi } from '@/api/log'
import type { AdminLogVO, AdminLogStatisticsVO, AdminLogQueryDTO } from '@/types/log'

const { t, locale } = useI18n()

// Statistics State
const logStats = reactive<AdminLogStatisticsVO>({
  totalLogs: 0,
  authLogs: 0,
  securityLogs: 0,
  errorLogs: 0
})

// Query & Filter State
const searchKeyword = ref('')
const filterLevel = ref<string | null>(null)
const filterModule = ref<string | null>(null)
const filterStatus = ref<string | null>(null)
const dateRange = ref<[number, number] | null>(null)
const sortBy = ref('createdAt')
const sortOrder = ref<'asc' | 'desc'>('desc')

const levelSelectOptions = computed<SelectOption[]>(() => [
  { label: t('admin.logs.level_all'), value: '' },
  { label: t('admin.logs.level_info'), value: 'INFO' },
  { label: t('admin.logs.level_warn'), value: 'WARN' },
  { label: t('admin.logs.level_error'), value: 'ERROR' }
])

const moduleSelectOptions = computed<SelectOption[]>(() => [
  { label: t('admin.logs.module_all'), value: '' },
  { label: t('admin.logs.module_auth'), value: 'AUTH' },
  { label: t('admin.logs.module_user'), value: 'USER' },
  { label: t('admin.logs.module_challenge'), value: 'CHALLENGE' },
  { label: t('admin.logs.module_competition'), value: 'COMPETITION' },
  { label: t('admin.logs.module_container'), value: 'CONTAINER' },
  { label: t('admin.logs.module_system'), value: 'SYSTEM' },
  { label: t('admin.logs.module_notification'), value: 'NOTIFICATION' }
])

const statusSelectOptions = computed<SelectOption[]>(() => [
  { label: t('admin.logs.status_all'), value: '' },
  { label: t('admin.logs.status_success'), value: 'true' },
  { label: t('admin.logs.status_failed'), value: 'false' }
])

// Table & Pagination State
const loading = ref(false)
const logList = ref<AdminLogVO[]>([])
const rowKey = (row: AdminLogVO) => String(row.id)
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
const selectedLog = ref<AdminLogVO | null>(null)

function getLevelTagType(level: string): 'success' | 'warning' | 'error' | 'default' {
  if (level === 'ERROR') return 'error'
  if (level === 'WARN') return 'warning'
  if (level === 'INFO') return 'success'
  return 'default'
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
const columns = computed<DataTableColumns<AdminLogVO>>(() => [
  {
    title: t('admin.logs.columns.id'),
    key: 'id',
    width: 180,
    render(row) {
      return h('span', { class: 'id-text' }, String(row.id))
    }
  },
  {
    title: t('admin.logs.columns.level'),
    key: 'level',
    width: 90,
    render(row) {
      return h(
        NTag,
        {
          type: getLevelTagType(row.level),
          size: 'small',
          round: true
        },
        { default: () => row.level }
      )
    }
  },
  {
    title: t('admin.logs.columns.module'),
    key: 'module',
    width: 120,
    render(row) {
      return h(NTag, { size: 'small', bordered: false }, { default: () => row.module || 'SYSTEM' })
    }
  },
  {
    title: t('admin.logs.columns.action'),
    key: 'action',
    minWidth: 160,
    render(row) {
      return h('span', { class: 'action-text' }, row.action || '-')
    }
  },
  {
    title: t('admin.logs.columns.operator'),
    key: 'operator',
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
              title: row.operator || 'ANONYMOUS',
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
            row.operator || 'ANONYMOUS'
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
    title: t('admin.logs.columns.duration'),
    key: 'durationMs',
    width: 90,
    sorter: true,
    render(row) {
      return h('span', { class: 'duration-badge' }, row.durationMs ? `${row.durationMs}ms` : '-')
    }
  },
  {
    title: t('admin.logs.columns.status'),
    key: 'status',
    width: 90,
    render(row) {
      return h(
        NTag,
        {
          type: row.status ? 'success' : 'error',
          size: 'small'
        },
        { default: () => (row.status ? t('admin.logs.success_tag') : t('admin.logs.failed_tag')) }
      )
    }
  },
  {
    title: t('admin.logs.columns.created_at'),
    key: 'createdAt',
    width: 170,
    sorter: true,
    render(row) {
      return formatDateTime(row.createdAt)
    }
  },
  {
    title: t('admin.logs.columns.actions'),
    key: 'actions',
    width: 90,
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
          default: () => t('admin.logs.view_detail')
        }
      )
    }
  }
])

// Data Fetching
async function fetchLogStats() {
  try {
    const res = await getAdminLogStatisticsApi()
    if (res.data) {
      Object.assign(logStats, res.data)
    }
  } catch (error) {
    console.error('Failed to fetch log statistics:', error)
  }
}

let currentFetchRequestId = 0

async function fetchLogList() {
  const requestId = ++currentFetchRequestId
  loading.value = true
  try {
    const params: AdminLogQueryDTO = {
      page: pagination.page,
      size: pagination.pageSize,
      keyword: searchKeyword.value.trim() || undefined,
      level: filterLevel.value || undefined,
      module: filterModule.value || undefined,
      status: filterStatus.value === 'true' ? true : filterStatus.value === 'false' ? false : undefined,
      sortBy: sortBy.value,
      sortOrder: sortOrder.value
    }

    if (dateRange.value && dateRange.value.length === 2) {
      params.startTime = new Date(dateRange.value[0]).toISOString()
      params.endTime = new Date(dateRange.value[1]).toISOString()
    }

    const res = await getAdminLogListApi(params)
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
    console.error('Failed to fetch log list:', error)
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
  searchKeyword.value = ''
  filterLevel.value = null
  filterModule.value = null
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

function openLogDetail(row: AdminLogVO) {
  selectedLog.value = row
  showDetailDrawer.value = true
}

onMounted(() => {
  fetchLogStats()
  fetchLogList()
})
</script>

<style scoped lang="scss">
.admin-log-view {
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
  background-color: var(--n-color, #ffffff);
  border-radius: 8px;
  padding: 10px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);
  transition: all 0.2s ease-in-out;

  &:hover {
    border-color: rgba(0, 0, 0, 0.12);
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
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
      color: #444444;
      font-weight: 500;
    }
  }
}

.action-text {
  font-weight: 600;
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
  gap: 12px;
  flex-wrap: wrap;

  .filter-search-input {
    width: 200px;
  }

  .filter-level-select {
    width: 120px;
  }

  .filter-module-select {
    width: 130px;
  }

  .filter-status-select {
    width: 120px;
  }

  .filter-date-picker {
    width: 280px;
  }

  .filter-actions-group {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

@media (max-width: 768px) {
  .log-filter-left {
    flex-direction: column;
    align-items: stretch;
    width: 100%;
    gap: 10px;

    .filter-search-input,
    .filter-level-select,
    .filter-module-select,
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
