<template>
  <div class="admin-task-view flex flex-col gap-4 sm:gap-6">
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h2 class="text-xl font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
          {{ t('admin.tasks.title', '任务管理') }}
        </h2>
        <p class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary mt-1">
          {{ t('admin.tasks.desc', '调度管理后台镜像拉取、跨节点分发转移、批量停止靶机与耗时异步任务队列') }}
        </p>
      </div>
    </div>

    <div class="grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4">
      <div class="p-3.5 sm:p-5 rounded-xl sm:rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark flex items-center gap-3 sm:gap-4 transition-colors">
        <div class="w-10 h-10 sm:w-12 sm:h-12 rounded-lg sm:rounded-xl flex items-center justify-center bg-blue-50 dark:bg-blue-900/30 text-google-blue dark:text-google-blue-dark shrink-0">
          <n-icon size="20" class="sm:hidden">
            <TimeOutline />
          </n-icon>
          <n-icon size="24" class="hidden sm:inline-flex">
            <TimeOutline />
          </n-icon>
        </div>
        <div class="min-w-0 flex-1">
          <div class="text-[11px] sm:text-xs font-medium text-google-text-muted dark:text-google-text-darkMuted truncate">
            累计异步任务
          </div>
          <div class="text-xl sm:text-2xl font-bold text-google-text-primary dark:text-google-text-darkPrimary mt-0.5 truncate">
            {{ stats.totalCount }}
          </div>
        </div>
      </div>

      <div class="p-3.5 sm:p-5 rounded-xl sm:rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark flex items-center gap-3 sm:gap-4 transition-colors">
        <div class="w-10 h-10 sm:w-12 sm:h-12 rounded-lg sm:rounded-xl flex items-center justify-center bg-amber-50 dark:bg-amber-900/30 text-google-yellow dark:text-google-yellow-dark shrink-0">
          <n-icon size="20" class="sm:hidden">
            <PlayOutline />
          </n-icon>
          <n-icon size="24" class="hidden sm:inline-flex">
            <PlayOutline />
          </n-icon>
        </div>
        <div class="min-w-0 flex-1">
          <div class="text-[11px] sm:text-xs font-medium text-google-text-muted dark:text-google-text-darkMuted truncate">
            正在队列执行
          </div>
          <div class="text-xl sm:text-2xl font-bold text-google-text-primary dark:text-google-text-darkPrimary mt-0.5 truncate">
            {{ stats.runningCount }}
          </div>
        </div>
      </div>

      <div class="p-3.5 sm:p-5 rounded-xl sm:rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark flex items-center gap-3 sm:gap-4 transition-colors">
        <div class="w-10 h-10 sm:w-12 sm:h-12 rounded-lg sm:rounded-xl flex items-center justify-center bg-green-50 dark:bg-green-900/30 text-google-green dark:text-google-green-dark shrink-0">
          <n-icon size="20" class="sm:hidden">
            <CheckmarkCircleOutline />
          </n-icon>
          <n-icon size="24" class="hidden sm:inline-flex">
            <CheckmarkCircleOutline />
          </n-icon>
        </div>
        <div class="min-w-0 flex-1">
          <div class="text-[11px] sm:text-xs font-medium text-google-text-muted dark:text-google-text-darkMuted truncate">
            成功执行结束
          </div>
          <div class="text-xl sm:text-2xl font-bold text-google-text-primary dark:text-google-text-darkPrimary mt-0.5 truncate">
            {{ stats.successCount }}
          </div>
        </div>
      </div>

      <div class="p-3.5 sm:p-5 rounded-xl sm:rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark flex items-center gap-3 sm:gap-4 transition-colors">
        <div class="w-10 h-10 sm:w-12 sm:h-12 rounded-lg sm:rounded-xl flex items-center justify-center bg-red-50 dark:bg-red-900/30 text-google-red dark:text-google-red-dark shrink-0">
          <n-icon size="20" class="sm:hidden">
            <AlertCircleOutline />
          </n-icon>
          <n-icon size="24" class="hidden sm:inline-flex">
            <AlertCircleOutline />
          </n-icon>
        </div>
        <div class="min-w-0 flex-1">
          <div class="text-[11px] sm:text-xs font-medium text-google-text-muted dark:text-google-text-darkMuted truncate">
            失败异常中断
          </div>
          <div class="text-xl sm:text-2xl font-bold text-google-text-primary dark:text-google-text-darkPrimary mt-0.5 truncate">
            {{ stats.failedCount }}
          </div>
        </div>
      </div>
    </div>

    <div class="table-card p-3.5 sm:p-6 rounded-xl sm:rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark transition-colors">
      <div class="flex flex-col sm:flex-row sm:flex-wrap sm:items-center gap-2.5 sm:gap-3 mb-4 sm:mb-6">
        <VSearch
          v-model:value="searchKeyword"
          :placeholder="'搜索任务名称或执行人...'"
          class="w-full sm:w-auto sm:flex-1 sm:min-w-[140px] sm:max-w-sm"
          @search="handleSearch"
          @clear="handleReset"
          @keydown.enter="handleSearch"
        />

        <div class="grid grid-cols-2 gap-2.5 w-full sm:flex sm:w-auto shrink-0">
          <VSelection
            v-model:value="filterType"
            :options="taskTypeOptions"
            :placeholder="'全部类型'"
            clearable
            class="w-full sm:w-44"
            @update:value="handleSearch"
          />

          <VSelection
            v-model:value="filterStatus"
            :options="taskStatusOptions"
            :placeholder="'全部状态'"
            clearable
            class="w-full sm:w-36"
            @update:value="handleSearch"
          />
        </div>

        <div class="flex items-center gap-2 w-full sm:w-auto shrink-0">
          <VBtn
            clz="normal"
            size="medium"
            :left-icon="SearchOutline"
            class="flex-1 sm:flex-none"
            @click="handleSearch"
          >
            {{ t('common.search', '搜索') }}
          </VBtn>

          <VBtn
            clz="normal"
            size="medium"
            :left-icon="RefreshOutline"
            class="flex-1 sm:flex-none"
            @click="handleReset"
          >
            {{ t('common.reset', '重置') }}
          </VBtn>
        </div>

        <div class="w-full sm:w-auto sm:ml-auto shrink-0">
          <VBtn
            clz="danger"
            size="medium"
            :left-icon="TrashOutline"
            class="w-full sm:w-auto"
            @click="handleClearHistory"
          >
            {{ t('admin.tasks.clear_history', '清理历史任务') }}
          </VBtn>
        </div>
      </div>

      <VTable
        :columns="tableColumns"
        :data="taskList"
        :loading="loading"
        :pagination="false"
        :bordered="false"
        :row-key="(row: AdminAsyncTaskVO) => String(row.id)"
        :scroll-x="980"
      />

      <div v-if="pagination.itemCount > 0" class="flex flex-col sm:flex-row items-center justify-between gap-3 pt-4 border-t border-google-border-light/70 dark:border-google-border-dark/70">
        <div class="text-xs text-google-text-muted dark:text-google-text-darkMuted text-center sm:text-left">
          显示第 {{ (pagination.page - 1) * pagination.pageSize + 1 }} - {{ Math.min(pagination.page * pagination.pageSize, pagination.itemCount) }} 条，共 {{ pagination.itemCount }} 条
        </div>
        <div class="w-full sm:w-auto overflow-x-auto flex justify-center sm:justify-end">
          <VPagination
            v-model:page="pagination.page"
            v-model:page-size="pagination.pageSize"
            :item-count="pagination.itemCount"
            :page-sizes="pagination.pageSizes"
            show-size-picker
            show-quick-jumper
            size="small"
            @update:page="handlePageChange"
            @update:page-size="handlePageSizeChange"
          />
        </div>
      </div>
    </div>

    <VModal
      v-model:show="showDetailModal"
      :title="'异步任务详情'"
      :width="560"
    >
      <div v-if="selectedTask" class="space-y-4 text-xs py-2">
        <div class="space-y-2 p-3 rounded-lg bg-gray-50 dark:bg-[#202124] border border-google-border-light dark:border-google-border-dark">
          <div><span class="text-google-text-secondary dark:text-google-text-darkSecondary">任务名称: </span><span class="font-semibold">{{ selectedTask.taskName }}</span></div>
          <div><span class="text-google-text-secondary dark:text-google-text-darkSecondary">任务类型: </span><span>{{ selectedTask.taskType }}</span></div>
          <div><span class="text-google-text-secondary dark:text-google-text-darkSecondary">执行进度: </span><span class="font-mono">{{ selectedTask.progress }}%</span></div>
          <div><span class="text-google-text-secondary dark:text-google-text-darkSecondary">耗时: </span><span class="font-mono">{{ selectedTask.executionDurationMs ?? 0 }} ms</span></div>
          <div><span class="text-google-text-secondary dark:text-google-text-darkSecondary">执行提交人: </span><span>{{ selectedTask.operatorName || '-' }}</span></div>
        </div>

        <div v-if="selectedTask.statusMessage">
          <div class="font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1">执行状态信息:</div>
          <div class="p-2.5 rounded bg-gray-100 dark:bg-[#303134] font-mono">{{ selectedTask.statusMessage }}</div>
        </div>

        <div v-if="selectedTask.errorMessage">
          <div class="font-medium text-red-500 mb-1">失败原因:</div>
          <pre class="p-2.5 rounded bg-red-950/40 text-red-300 font-mono text-[11px] whitespace-pre-wrap">{{ selectedTask.errorMessage }}</pre>
        </div>
      </div>

      <template #footer>
        <div class="flex justify-end w-full">
          <VBtn clz="normal" @click="showDetailModal = false">
            {{ t('common.close', '关闭') }}
          </VBtn>
        </div>
      </template>
    </VModal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, h } from 'vue'
import { useI18n } from 'vue-i18n'
import { NIcon, useMessage, useDialog, type DataTableColumns } from 'naive-ui'
import {
  TimeOutline,
  PlayOutline,
  CheckmarkCircleOutline,
  AlertCircleOutline,
  SearchOutline,
  RefreshOutline,
  TrashOutline
} from '@vicons/ionicons5'
import VBtn from '@/components/common/VBtn.vue'
import VTable from '@/components/common/VTable.vue'
import VSearch from '@/components/common/VSearch.vue'
import VSelection from '@/components/common/VSelection.vue'
import VPagination from '@/components/common/VPagination.vue'
import VModal from '@/components/common/VModal.vue'
import type {
  AdminAsyncTaskVO,
  AdminAsyncTaskStatisticsVO,
  AsyncTaskType,
  AsyncTaskStatus
} from '@/types/task'
import {
  getAdminTaskStatisticsApi,
  getAdminTaskListApi,
  cancelAdminTaskApi,
  deleteAdminTaskApi,
  clearAdminTaskHistoryApi
} from '@/api/task'

defineOptions({
  name: 'AdminTaskView'
})

const { t } = useI18n()
const message = useMessage()
const dialog = useDialog()

const loading = ref(false)
const taskList = ref<AdminAsyncTaskVO[]>([])

const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

const searchKeyword = ref('')
const filterType = ref<string | null>(null)
const filterStatus = ref<string | null>(null)

const stats = ref<AdminAsyncTaskStatisticsVO>({
  totalCount: 0,
  runningCount: 0,
  pendingCount: 0,
  successCount: 0,
  failedCount: 0
})

const showDetailModal = ref(false)
const selectedTask = ref<AdminAsyncTaskVO | null>(null)

const taskTypeOptions = [
  { label: '全部类型', value: '' },
  { label: '镜像拉取 (IMAGE_PULL)', value: 'IMAGE_PULL' },
  { label: '节点转移 (IMAGE_TRANSFER)', value: 'IMAGE_TRANSFER' },
  { label: '镜像备份 (IMAGE_BACKUP)', value: 'IMAGE_BACKUP' },
  { label: '镜像同步 (IMAGE_SYNC)', value: 'IMAGE_SYNC' },
  { label: '批量销毁 (CONTAINER_BATCH_STOP)', value: 'CONTAINER_BATCH_STOP' }
]

const taskStatusOptions = [
  { label: '全部状态', value: '' },
  { label: '队列排队 (PENDING)', value: 'PENDING' },
  { label: '执行中 (RUNNING)', value: 'RUNNING' },
  { label: '成功 (SUCCESS)', value: 'SUCCESS' },
  { label: '失败 (FAILED)', value: 'FAILED' },
  { label: '已取消 (CANCELLED)', value: 'CANCELLED' }
]

function getStatusText(status: AsyncTaskStatus): string {
  switch (status) {
    case 'PENDING':
      return '队列排队'
    case 'RUNNING':
      return '执行中'
    case 'SUCCESS':
      return '成功'
    case 'FAILED':
      return '失败'
    case 'CANCELLED':
      return '已取消'
    default:
      return status || '-'
  }
}

const tableColumns = computed<DataTableColumns<AdminAsyncTaskVO>>(() => [
  {
    title: '任务名称',
    key: 'taskName',
    minWidth: 160,
    render(row) {
      return h('span', { class: 'font-semibold text-google-text-primary dark:text-google-text-darkPrimary' }, row.taskName)
    }
  },
  {
    title: '任务类型',
    key: 'taskType',
    width: 140,
    render(row) {
      return row.taskType || '-'
    }
  },
  {
    title: '进度',
    key: 'progress',
    width: 120,
    render(row) {
      return h('div', { class: 'flex items-center gap-2' }, [
        h('div', { class: 'flex-1 h-1.5 rounded-full bg-gray-200 dark:bg-gray-700 overflow-hidden' }, [
          h('div', {
            class: 'h-full bg-google-blue dark:bg-google-blue-dark transition-all duration-300',
            style: { width: `${row.progress || 0}%` }
          })
        ]),
        h('span', { class: 'text-[11px] font-mono' }, `${row.progress || 0}%`)
      ])
    }
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render(row) {
      return getStatusText(row.status)
    }
  },
  {
    title: '执行人',
    key: 'operatorName',
    width: 110,
    render(row) {
      return row.operatorName || '-'
    }
  },
  {
    title: '创建时间',
    key: 'createdAt',
    width: 160,
    render(row) {
      return h('span', { class: 'text-xs text-google-text-secondary dark:text-google-text-darkSecondary font-mono' }, row.createdAt ? new Date(row.createdAt).toLocaleString() : '-')
    }
  },
  {
    title: t('common.actions', '操作'),
    key: 'actions',
    width: 140,
    align: 'center',
    render(row) {
      return h('div', { class: 'flex items-center justify-center gap-1.5' }, [
        h(
          VBtn,
          {
            clz: 'normal',
            size: 'small',
            onClick: () => {
              selectedTask.value = row
              showDetailModal.value = true
            }
          },
          { default: () => '详情' }
        ),
        row.status === 'RUNNING' || row.status === 'PENDING'
          ? h(
              VBtn,
              {
                clz: 'danger',
                size: 'small',
                onClick: () => handleCancelTask(row.id)
              },
              { default: () => '取消' }
            )
          : h(
              VBtn,
              {
                clz: 'danger',
                size: 'small',
                onClick: () => handleDeleteTask(row.id)
              },
              { default: () => '删除' }
            )
      ])
    }
  }
])

async function fetchStats() {
  try {
    const res = await getAdminTaskStatisticsApi()
    if (res.data) {
      stats.value = res.data
    }
  } catch {}
}

async function fetchTasks() {
  loading.value = true
  try {
    const res = await getAdminTaskListApi({
      page: pagination.page,
      size: pagination.pageSize,
      keyword: searchKeyword.value.trim() || undefined,
      taskType: (filterType.value || undefined) as AsyncTaskType | undefined,
      status: (filterStatus.value || undefined) as AsyncTaskStatus | undefined
    })
    if (res.data) {
      taskList.value = res.data.list ?? []
      pagination.itemCount = Number(res.data.total) || 0
    }
  } catch (err: any) {
    message.error(err.message || '获取任务列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchTasks()
}

function handleReset() {
  searchKeyword.value = ''
  filterType.value = null
  filterStatus.value = null
  pagination.page = 1
  fetchTasks()
}

function handlePageChange(page: number) {
  pagination.page = page
  fetchTasks()
}

function handlePageSizeChange(size: number) {
  pagination.pageSize = size
  pagination.page = 1
  fetchTasks()
}

async function handleCancelTask(id: string | number) {
  try {
    await cancelAdminTaskApi(id)
    message.success('任务已请求中断取消')
    await Promise.all([fetchStats(), fetchTasks()])
  } catch (err: any) {
    message.error(err.message || '取消任务失败')
  }
}

async function handleDeleteTask(id: string | number) {
  try {
    await deleteAdminTaskApi(id)
    message.success('任务记录已删除')
    await Promise.all([fetchStats(), fetchTasks()])
  } catch (err: any) {
    message.error(err.message || '删除任务失败')
  }
}

function handleClearHistory() {
  dialog.warning({
    title: '确认清空历史任务',
    content: '将彻底清空所有已成功或已失败的历史任务执行记录，是否继续？',
    positiveText: '确认清空',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await clearAdminTaskHistoryApi()
        message.success('历史任务已清空')
        await Promise.all([fetchStats(), fetchTasks()])
      } catch (err: any) {
        message.error(err.message || '清空失败')
      }
    }
  })
}

onMounted(() => {
  fetchStats()
  fetchTasks()
})
</script>
