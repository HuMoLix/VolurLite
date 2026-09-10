<template>
  <div class="admin-task-view">
    <!-- Header Section -->
    <div class="page-header">
      <h2 class="page-title">{{ t('admin.tasks.title') }}</h2>
      <p class="page-desc">{{ t('admin.tasks.subtitle') }}</p>
    </div>

    <!-- Top Statistics Ribbon -->
    <div class="stat-ribbon">
      <div class="stat-mini-card">
        <div class="stat-icon-wrapper primary">
          <n-icon size="18">
            <HourglassOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statData.totalCount }}</div>
          <div class="stat-label">{{ t('admin.tasks.stat_total') }}</div>
        </div>
      </div>

      <div class="stat-mini-card" :class="{ 'pulse-active': statData.runningCount > 0 }">
        <div class="stat-icon-wrapper info">
          <n-icon size="18" :class="{ 'spin-icon': statData.runningCount > 0 }">
            <SyncOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value highlight-running">{{ statData.runningCount }}</div>
          <div class="stat-label">{{ t('admin.tasks.stat_running') }}</div>
        </div>
      </div>

      <div class="stat-mini-card">
        <div class="stat-icon-wrapper warning">
          <n-icon size="18">
            <TimeOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statData.pendingCount }}</div>
          <div class="stat-label">{{ t('admin.tasks.stat_pending') }}</div>
        </div>
      </div>

      <div class="stat-mini-card">
        <div class="stat-icon-wrapper success">
          <n-icon size="18">
            <CheckmarkCircleOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statData.successCount }}</div>
          <div class="stat-label">{{ t('admin.tasks.stat_success') }}</div>
        </div>
      </div>

      <div class="stat-mini-card">
        <div class="stat-icon-wrapper error">
          <n-icon size="18">
            <CloseCircleOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statData.failedCount }}</div>
          <div class="stat-label">{{ t('admin.tasks.stat_failed') }}</div>
        </div>
      </div>
    </div>

    <!-- Main Table Card -->
    <n-card class="table-card" :bordered="false">
      <!-- Search & Filters Header -->
      <div class="filter-header">
        <div class="filter-left">
          <n-input
            v-model:value="searchKeyword"
            :placeholder="t('admin.tasks.search_placeholder')"
            clearable
            style="width: 300px;"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <n-icon :component="SearchOutline" />
            </template>
          </n-input>

          <n-select
            v-model:value="filterType"
            :placeholder="t('admin.tasks.filter_type')"
            clearable
            :options="taskTypeOptions"
            style="width: 180px;"
            @update:value="handleSearch"
          />

          <n-select
            v-model:value="filterStatus"
            :placeholder="t('admin.tasks.filter_status')"
            clearable
            :options="statusOptions"
            style="width: 150px;"
            @update:value="handleSearch"
          />

          <n-button type="primary" @click="handleSearch" class="theme-primary-btn">
            <template #icon>
              <n-icon :component="SearchOutline" />
            </template>
            {{ t('admin.tasks.query_btn') }}
          </n-button>

          <n-button quaternary @click="handleReset">
            <template #icon>
              <n-icon :component="RefreshOutline" />
            </template>
            {{ t('admin.tasks.reset_btn') }}
          </n-button>
        </div>

        <div class="filter-right">
          <n-popconfirm @positive-click="handleClearHistory">
            <template #trigger>
              <n-button quaternary type="warning" size="medium">
                <template #icon>
                  <n-icon :component="TrashOutline" />
                </template>
                {{ t('admin.tasks.clear_history_btn') }}
              </n-button>
            </template>
            {{ t('admin.tasks.clear_history_confirm') }}
          </n-popconfirm>

          <n-button quaternary circle size="medium" @click="fetchData" :loading="loading">
            <template #icon>
              <n-icon :component="RefreshOutline" />
            </template>
          </n-button>
        </div>
      </div>

      <!-- Tasks Data Table -->
      <n-data-table
        :columns="columns"
        :data="taskList"
        :loading="loading"
        :pagination="pagination"
        :bordered="false"
        :row-key="(row: AdminAsyncTaskVO) => String(row.id)"
        class="task-table"
        remote
        @update:page="handlePageChange"
        @update:page-size="handlePageSizeChange"
      />
    </n-card>

    <!-- Task Detail Drawer (Styled like AdminLogView.vue) -->
    <n-drawer v-model:show="showDrawer" :width="620" placement="right">
      <n-drawer-content :title="t('admin.tasks.drawer_title')" closable>
        <div v-if="selectedTask" class="drawer-detail-body">
          <n-descriptions :column="2" label-placement="left" bordered size="small">
            <n-descriptions-item :label="t('admin.tasks.col_id')">
              <span class="id-text">{{ selectedTask.id }}</span>
            </n-descriptions-item>
            <n-descriptions-item :label="t('admin.tasks.col_type')">
              <n-tag size="small" :bordered="false" :type="getTypeTagType(selectedTask.taskType)">
                {{ selectedTask.taskTypeDesc || selectedTask.taskType }}
              </n-tag>
            </n-descriptions-item>
            <n-descriptions-item :label="t('admin.tasks.col_name')" :span="2">
              <strong class="action-text">{{ selectedTask.taskName }}</strong>
            </n-descriptions-item>
            <n-descriptions-item :label="t('admin.tasks.col_status')">
              <n-tag size="small" :bordered="false" :type="getStatusTagType(selectedTask.status)">
                {{ selectedTask.statusDesc || selectedTask.status }}
              </n-tag>
            </n-descriptions-item>
            <n-descriptions-item :label="t('admin.tasks.col_progress')">
              <span class="progress-percent font-mono">{{ selectedTask.progress }}%</span>
            </n-descriptions-item>
            <n-descriptions-item :label="t('admin.tasks.col_operator')">
              {{ selectedTask.operatorName || 'System' }}
            </n-descriptions-item>
            <n-descriptions-item :label="t('admin.tasks.duration')">
              <span class="duration-text font-mono">{{ formatDuration(selectedTask.executionDurationMs) }}</span>
            </n-descriptions-item>
            <n-descriptions-item :label="t('admin.tasks.created_at')">
              {{ formatDateTime(selectedTask.createdAt) }}
            </n-descriptions-item>
            <n-descriptions-item :label="t('admin.tasks.started_at')">
              {{ selectedTask.startedAt ? formatDateTime(selectedTask.startedAt) : '-' }}
            </n-descriptions-item>
            <n-descriptions-item :label="t('admin.tasks.finished_at')" :span="2">
              {{ selectedTask.finishedAt ? formatDateTime(selectedTask.finishedAt) : '-' }}
            </n-descriptions-item>
          </n-descriptions>

          <!-- Progress & Current Status Message -->
          <div class="code-section">
            <div class="section-title">{{ t('admin.tasks.col_progress') }}</div>
            <n-progress
              type="line"
              :percentage="selectedTask.progress"
              :status="getProgressStatus(selectedTask.status)"
              :indicator-placement="'inside'"
              style="margin-bottom: 8px;"
            />
            <div v-if="selectedTask.statusMessage" class="status-msg-box">
              <n-icon size="15"><InformationCircleOutline /></n-icon>
              <span>{{ selectedTask.statusMessage }}</span>
            </div>
          </div>

          <!-- Error Alert if Failed -->
          <div v-if="selectedTask.errorMessage" class="code-section error-box">
            <div class="section-title error-title">{{ t('admin.tasks.drawer_error') }}</div>
            <pre class="code-block error-block">{{ selectedTask.errorMessage }}</pre>
          </div>

          <!-- Lifecycle Timeline -->
          <div class="code-section">
            <div class="section-title">{{ t('admin.tasks.drawer_timeline') }}</div>
            <n-timeline size="medium" style="margin-top: 8px;">
              <n-timeline-item
                type="info"
                :title="t('admin.tasks.created_at')"
                :time="formatDateTime(selectedTask.createdAt)"
              />
              <n-timeline-item
                v-if="selectedTask.startedAt"
                type="warning"
                :title="t('admin.tasks.started_at')"
                :time="formatDateTime(selectedTask.startedAt)"
              />
              <n-timeline-item
                v-if="selectedTask.finishedAt"
                :type="selectedTask.status === 'SUCCESS' ? 'success' : 'error'"
                :title="selectedTask.status === 'SUCCESS' ? t('admin.tasks.status.SUCCESS') : t('admin.tasks.status.FAILED')"
                :time="formatDateTime(selectedTask.finishedAt)"
                :content="selectedTask.statusMessage || undefined"
              />
            </n-timeline>
          </div>

          <!-- Parameters JSON -->
          <div v-if="selectedTask.parameters" class="code-section">
            <div class="section-title">{{ t('admin.tasks.drawer_params') }}</div>
            <pre class="code-block">{{ formatJson(selectedTask.parameters) }}</pre>
          </div>

          <!-- Result Data JSON -->
          <div v-if="selectedTask.resultData" class="code-section">
            <div class="section-title">{{ t('admin.tasks.drawer_result') }}</div>
            <pre class="code-block">{{ formatJson(selectedTask.resultData) }}</pre>
          </div>
        </div>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, h } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  NCard,
  NDataTable,
  NInput,
  NSelect,
  NButton,
  NIcon,
  NTag,
  NProgress,
  NPopconfirm,
  NDrawer,
  NDrawerContent,
  NDescriptions,
  NDescriptionsItem,
  NTimeline,
  NTimelineItem,
  NSpin,
  NTooltip,
  useMessage,
  type DataTableColumns
} from 'naive-ui'
import {
  SearchOutline,
  RefreshOutline,
  TrashOutline,
  HourglassOutline,
  TimeOutline,
  CheckmarkCircleOutline,
  CloseCircleOutline,
  SyncOutline,
  InformationCircleOutline,
  LayersOutline,
  CubeOutline,
  MailOutline,
  SettingsOutline,
  EyeOutline
} from '@vicons/ionicons5'
import {
  getAdminTaskListApi,
  getAdminTaskStatisticsApi,
  getAdminTaskDetailApi,
  cancelAdminTaskApi,
  deleteAdminTaskApi,
  clearAdminTaskHistoryApi
} from '@/api/task'
import type {
  AdminAsyncTaskVO,
  AdminAsyncTaskStatisticsVO,
  AsyncTaskType,
  AsyncTaskStatus
} from '@/types/task'
import { SseClient } from '@/utils/sse'
import { safeJsonParse } from '@/utils/request'

const { t } = useI18n()
const message = useMessage()

// Filters state
const searchKeyword = ref('')
const filterType = ref<AsyncTaskType | null>(null)
const filterStatus = ref<AsyncTaskStatus | null>(null)
const loading = ref(false)
const taskList = ref<AdminAsyncTaskVO[]>([])
const showDrawer = ref(false)
const selectedTask = ref<AdminAsyncTaskVO | null>(null)

// Statistics
const statData = reactive<AdminAsyncTaskStatisticsVO>({
  totalCount: 0,
  runningCount: 0,
  pendingCount: 0,
  successCount: 0,
  failedCount: 0
})

// Pagination
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
  prefix: (info: { itemCount?: number }) => `共 ${info.itemCount || 0} 条记录`
})

// SSE Client
let sseClient: SseClient | null = null

// Options
const taskTypeOptions = computed(() => [
  { label: t('admin.tasks.types.IMAGE_TRANSFER'), value: 'IMAGE_TRANSFER' },
  { label: t('admin.tasks.types.IMAGE_PULL'), value: 'IMAGE_PULL' },
  { label: t('admin.tasks.types.IMAGE_BACKUP'), value: 'IMAGE_BACKUP' },
  { label: t('admin.tasks.types.IMAGE_SYNC'), value: 'IMAGE_SYNC' },
  { label: t('admin.tasks.types.CONTAINER_BATCH_STOP'), value: 'CONTAINER_BATCH_STOP' },
  { label: t('admin.tasks.types.EMAIL_BATCH_SEND'), value: 'EMAIL_BATCH_SEND' },
  { label: t('admin.tasks.types.SYSTEM_CLEANUP'), value: 'SYSTEM_CLEANUP' },
  { label: t('admin.tasks.types.OTHER'), value: 'OTHER' }
])

const statusOptions = computed(() => [
  { label: t('admin.tasks.status.PENDING'), value: 'PENDING' },
  { label: t('admin.tasks.status.RUNNING'), value: 'RUNNING' },
  { label: t('admin.tasks.status.SUCCESS'), value: 'SUCCESS' },
  { label: t('admin.tasks.status.FAILED'), value: 'FAILED' },
  { label: t('admin.tasks.status.CANCELLED'), value: 'CANCELLED' }
])

function getTypeTagType(type?: AsyncTaskType): 'default' | 'info' | 'success' | 'warning' | 'error' {
  switch (type) {
    case 'IMAGE_TRANSFER':
    case 'IMAGE_PULL':
    case 'IMAGE_SYNC':
      return 'info'
    case 'IMAGE_BACKUP':
      return 'warning'
    case 'CONTAINER_BATCH_STOP':
      return 'error'
    case 'EMAIL_BATCH_SEND':
      return 'success'
    default:
      return 'default'
  }
}

function getStatusTagType(status?: AsyncTaskStatus): 'default' | 'info' | 'success' | 'warning' | 'error' {
  switch (status) {
    case 'RUNNING':
      return 'info'
    case 'PENDING':
      return 'warning'
    case 'SUCCESS':
      return 'success'
    case 'FAILED':
      return 'error'
    case 'CANCELLED':
    default:
      return 'default'
  }
}

function getProgressStatus(status?: AsyncTaskStatus): 'info' | 'success' | 'warning' | 'error' {
  switch (status) {
    case 'RUNNING':
      return 'info'
    case 'PENDING':
      return 'warning'
    case 'SUCCESS':
      return 'success'
    case 'FAILED':
      return 'error'
    default:
      return 'info'
  }
}

function formatDuration(ms?: number): string {
  if (ms == null || ms <= 0) return '-'
  if (ms < 1000) return `${ms}ms`
  const seconds = (ms / 1000).toFixed(1)
  if (ms < 60000) return `${seconds}s`
  const minutes = Math.floor(ms / 60000)
  const remainingSeconds = Math.round((ms % 60000) / 1000)
  return `${minutes}m ${remainingSeconds}s`
}

function formatDateTime(str?: string): string {
  if (!str) return '-'
  try {
    const d = new Date(str)
    return d.toLocaleString()
  } catch {
    return str
  }
}

function formatJson(str?: string): string {
  if (!str) return ''
  try {
    const obj = safeJsonParse(str)
    return JSON.stringify(obj, null, 2)
  } catch {
    return str
  }
}

// Table columns
const columns = computed<DataTableColumns<AdminAsyncTaskVO>>(() => [
  {
    title: t('admin.tasks.col_id'),
    key: 'id',
    width: 140,
    render(row) {
      return h(
        'span',
        { class: 'task-id-text font-mono' },
        String(row.id).slice(-8)
      )
    }
  },
  {
    title: t('admin.tasks.col_name'),
    key: 'taskName',
    minWidth: 220,
    ellipsis: { tooltip: true },
    render(row) {
      let icon = LayersOutline
      if (row.taskType === 'CONTAINER_BATCH_STOP') icon = CubeOutline
      else if (row.taskType === 'EMAIL_BATCH_SEND') icon = MailOutline
      else if (row.taskType === 'SYSTEM_CLEANUP') icon = SettingsOutline

      return h('div', { class: 'task-name-cell' }, [
        h(NIcon, { size: 16, component: icon, class: 'task-type-icon' }),
        h('span', { class: 'task-name-text' }, row.taskName)
      ])
    }
  },
  {
    title: t('admin.tasks.col_type'),
    key: 'taskType',
    width: 130,
    render(row) {
      return h(
        NTag,
        { size: 'small', bordered: false, type: getTypeTagType(row.taskType) },
        { default: () => row.taskTypeDesc || row.taskType }
      )
    }
  },
  {
    title: t('admin.tasks.col_status'),
    key: 'status',
    width: 120,
    render(row) {
      const isRunning = row.status === 'RUNNING'
      return h(
        NTag,
        {
          size: 'small',
          bordered: false,
          type: getStatusTagType(row.status)
        },
        {
          default: () => [
            isRunning ? h(NSpin, { size: 12, style: 'margin-right: 4px; display: inline-block;' }) : null,
            row.statusDesc || row.status
          ]
        }
      )
    }
  },
  {
    title: t('admin.tasks.col_progress'),
    key: 'progress',
    width: 190,
    render(row) {
      return h('div', { class: 'table-progress-cell' }, [
        h(NProgress, {
          type: 'line',
          percentage: row.progress || 0,
          status: getProgressStatus(row.status),
          showIndicator: true,
          height: 8
        }),
        row.statusMessage
          ? h(
              NTooltip,
              { trigger: 'hover' },
              {
                trigger: () =>
                  h('div', { class: 'status-msg-truncated' }, row.statusMessage),
                default: () => row.statusMessage
              }
            )
          : null
      ])
    }
  },
  {
    title: t('admin.tasks.col_operator'),
    key: 'operatorName',
    width: 100,
    render(row) {
      return row.operatorName || 'System'
    }
  },
  {
    title: t('admin.tasks.col_time'),
    key: 'createdAt',
    width: 150,
    render(row) {
      return h('div', { class: 'time-cell' }, [
        h('div', { class: 'created-time' }, formatDateTime(row.createdAt)),
        h('div', { class: 'duration-time font-mono' }, `耗时: ${formatDuration(row.executionDurationMs)}`)
      ])
    }
  },
  {
    title: t('admin.tasks.col_actions'),
    key: 'actions',
    width: 170,
    fixed: 'right',
    render(row) {
      const isRunningOrPending = row.status === 'RUNNING' || row.status === 'PENDING'
      return h('div', { class: 'table-action-btns' }, [
        h(
          NButton,
          {
            size: 'tiny',
            quaternary: true,
            type: 'primary',
            onClick: () => handleOpenDetail(row)
          },
          {
            icon: () => h(NIcon, { component: EyeOutline }),
            default: () => t('admin.tasks.action_detail')
          }
        ),
        isRunningOrPending
          ? h(
              NPopconfirm,
              {
                onPositiveClick: () => handleCancelTask(row.id)
              },
              {
                trigger: () =>
                  h(
                    NButton,
                    {
                      size: 'tiny',
                      quaternary: true,
                      type: 'warning'
                    },
                    {
                      icon: () => h(NIcon, { component: CloseCircleOutline }),
                      default: () => t('admin.tasks.action_cancel')
                    }
                  ),
                default: () => t('admin.tasks.cancel_confirm')
              }
            )
          : h(
              NPopconfirm,
              {
                onPositiveClick: () => handleDeleteTask(row.id)
              },
              {
                trigger: () =>
                  h(
                    NButton,
                    {
                      size: 'tiny',
                      quaternary: true,
                      type: 'error'
                    },
                    {
                      icon: () => h(NIcon, { component: TrashOutline }),
                      default: () => t('admin.tasks.action_delete')
                    }
                  ),
                default: () => t('admin.tasks.delete_confirm')
              }
            )
      ])
    }
  }
])

async function fetchStatistics() {
  try {
    const res = await getAdminTaskStatisticsApi()
    if (res.data) {
      Object.assign(statData, res.data)
    }
  } catch {
    // handled
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getAdminTaskListApi({
      page: pagination.page,
      size: pagination.pageSize,
      keyword: searchKeyword.value.trim() || undefined,
      taskType: filterType.value || undefined,
      status: filterStatus.value || undefined
    })

    if (res.data) {
      taskList.value = res.data.list || []
      pagination.itemCount = res.data.total || 0
    }
    await fetchStatistics()
  } catch {
    // handled
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchData()
}

function handleReset() {
  searchKeyword.value = ''
  filterType.value = null
  filterStatus.value = null
  pagination.page = 1
  fetchData()
}

function handlePageChange(page: number) {
  pagination.page = page
  fetchData()
}

function handlePageSizeChange(pageSize: number) {
  pagination.pageSize = pageSize
  pagination.page = 1
  fetchData()
}

async function handleOpenDetail(row: AdminAsyncTaskVO) {
  selectedTask.value = row
  showDrawer.value = true
  try {
    const res = await getAdminTaskDetailApi(String(row.id))
    if (res.data) {
      selectedTask.value = res.data
    }
  } catch (err) {
    console.warn('[AdminTaskView] 获取任务详情异常:', err)
  }
}

async function handleCancelTask(id: string | number) {
  try {
    await cancelAdminTaskApi(id)
    message.success(t('admin.tasks.toast.cancel_success'))
    fetchData()
  } catch {
    // handled
  }
}

async function handleDeleteTask(id: string | number) {
  try {
    await deleteAdminTaskApi(id)
    message.success(t('admin.tasks.toast.delete_success'))
    fetchData()
  } catch {
    // handled
  }
}

async function handleClearHistory() {
  try {
    await clearAdminTaskHistoryApi()
    message.success(t('admin.tasks.toast.clear_success'))
    fetchData()
  } catch {
    // handled
  }
}

// Real-time SSE handler
function handleSseTaskEvent(eventData: AdminAsyncTaskVO) {
  if (!eventData || !eventData.id) return

  // 1. Update in-place if present in table
  const existing = taskList.value.find((t) => String(t.id) === String(eventData.id))
  if (existing) {
    existing.status = eventData.status
    existing.statusDesc = eventData.statusDesc
    existing.progress = eventData.progress
    existing.statusMessage = eventData.statusMessage
    existing.errorMessage = eventData.errorMessage
    existing.finishedAt = eventData.finishedAt
    existing.executionDurationMs = eventData.executionDurationMs
  } else if (pagination.page === 1 && !filterType.value && !filterStatus.value && !searchKeyword.value) {
    // Prepend if on first page with no filters
    taskList.value.unshift(eventData)
    if (taskList.value.length > pagination.pageSize) {
      taskList.value.pop()
    }
  }

  // 2. If drawer is currently viewing this task, update drawer
  if (selectedTask.value && String(selectedTask.value.id) === String(eventData.id)) {
    Object.assign(selectedTask.value, eventData)
  }

  // 3. Update stats
  fetchStatistics()
}

onMounted(() => {
  fetchData()

  // Connect SSE for task_event
  try {
    sseClient = new SseClient('/api/v1/realtime/stream')
    sseClient.on('task_event', (data: AdminAsyncTaskVO) => {
      handleSseTaskEvent(data)
    })
    sseClient.connect()
  } catch (e) {
    console.warn('[AdminTaskView] SSE connect error', e)
  }
})

onUnmounted(() => {
  sseClient?.close()
  sseClient = null
})
</script>

<style scoped lang="scss">
.admin-task-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header {
  display: flex;
  flex-direction: column;
  gap: 4px;

  .page-title {
    font-size: 22px;
    font-weight: 700;
    margin: 0;
    color: var(--v-text-primary, #111111);
    letter-spacing: -0.3px;
  }

  .page-desc {
    font-size: 13px;
    color: #444444;
    margin: 0;
  }
}

/* Statistics Ribbon (Aligned with AdminImageView.vue) */
.stat-ribbon {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;

  .stat-mini-card {
    background-color: var(--n-color, #ffffff);
    border-radius: 10px;
    padding: 16px 20px;
    display: flex;
    align-items: center;
    gap: 14px;
    box-shadow: var(--v-shadow-sm, 0 1px 3px rgba(0, 0, 0, 0.04));
    transition: transform 0.2s ease;

    &:hover {
      transform: translateY(-2px);
    }

    &.pulse-active {
      border: 1px solid #2080f0;
      box-shadow: 0 0 8px rgba(32, 128, 240, 0.2);
    }

    .stat-icon-wrapper {
      width: 42px;
      height: 42px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;

      &.primary {
        background-color: rgba(37, 99, 235, 0.1);
        color: #2563eb;
      }

      &.info {
        background-color: rgba(99, 102, 241, 0.1);
        color: #6366f1;
      }

      &.warning {
        background-color: rgba(245, 158, 11, 0.1);
        color: #f59e0b;
      }

      &.success {
        background-color: rgba(16, 185, 129, 0.1);
        color: #10b981;
      }

      &.error {
        background-color: rgba(239, 68, 68, 0.1);
        color: #ef4444;
      }

      .spin-icon {
        animation: spin 1.8s linear infinite;
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

        &.highlight-running {
          color: #2563eb;
        }
      }

      .stat-label {
        font-size: 12px;
        color: #444444;
      }
    }
  }
}

/* Main Table Card (Aligned with AdminImageView.vue) */
.table-card {
  border-radius: 10px;
  background-color: var(--n-color, #ffffff);

  .filter-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 12px;
    margin-bottom: 16px;

    .filter-left {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 12px;
    }

    .filter-right {
      display: flex;
      align-items: center;
      gap: 12px;
    }
  }

  .task-table {
    .task-id-text {
      color: #666;
      font-size: 12px;
      font-family: 'Fira Code', monospace;
    }

    .task-name-cell {
      display: flex;
      align-items: center;
      gap: 8px;

      .task-type-icon {
        color: #444;
        flex-shrink: 0;
      }

      .task-name-text {
        font-weight: 600;
        color: #333;
      }
    }

    .table-progress-cell {
      display: flex;
      flex-direction: column;
      gap: 4px;

      .status-msg-truncated {
        font-size: 11px;
        color: #666;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        max-width: 170px;
      }
    }

    .time-cell {
      display: flex;
      flex-direction: column;
      gap: 2px;

      .created-time {
        font-size: 12px;
        color: #444;
      }

      .duration-time {
        font-size: 11px;
        color: #888;
        font-family: 'Fira Code', monospace;
      }
    }
  }
}

.table-action-btns {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* Detail Drawer Styles (Aligned with AdminLogView.vue) */
.drawer-detail-body {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .id-text {
    font-family: 'Fira Code', monospace;
    font-size: 12px;
    color: #666666;
  }

  .action-text {
    font-weight: 600;
    font-size: 13px;
    color: var(--v-text-primary, #111111);
  }

  .progress-percent {
    font-family: 'Fira Code', monospace;
    font-weight: 600;
    color: #2563eb;
  }

  .duration-text {
    font-family: 'Fira Code', monospace;
    font-weight: 600;
    color: #6366f1;
  }

  .status-msg-box {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 8px 12px;
    background: rgba(32, 128, 240, 0.08);
    border-radius: 6px;
    font-size: 12px;
    color: #2080f0;
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
  }
}

/* Primary Button Rule (Volur View Rule) */
.theme-primary-btn {
  background-color: #000000 !important;
  color: #ffffff !important;
  font-weight: 500;
  border-radius: 6px;
  border: none !important;

  &:hover {
    background-color: #222222 !important;
  }
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}
</style>
