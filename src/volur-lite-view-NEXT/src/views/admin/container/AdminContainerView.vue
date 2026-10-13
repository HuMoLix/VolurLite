<template>
  <div class="admin-container-view flex flex-col gap-4 sm:gap-6">
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h2 class="text-xl font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
          {{ t('admin.containers.title', '容器管理') }}
        </h2>
        <p class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary mt-1">
          {{ t('admin.containers.desc', '监控与调度全平台动态靶机容器实例生命周期、调试 Web Shell 与续期销毁') }}
        </p>
      </div>
    </div>

    <div class="grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4">
      <div class="p-3.5 sm:p-5 rounded-xl sm:rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark flex items-center gap-3 sm:gap-4 transition-colors">
        <div class="w-10 h-10 sm:w-12 sm:h-12 rounded-lg sm:rounded-xl flex items-center justify-center bg-blue-50 dark:bg-blue-900/30 text-google-blue dark:text-google-blue-dark shrink-0">
          <n-icon size="20" class="sm:hidden">
            <CubeOutline />
          </n-icon>
          <n-icon size="24" class="hidden sm:inline-flex">
            <CubeOutline />
          </n-icon>
        </div>
        <div class="min-w-0 flex-1">
          <div class="text-[11px] sm:text-xs font-medium text-google-text-muted dark:text-google-text-darkMuted truncate">
            活跃靶机总数
          </div>
          <div class="text-xl sm:text-2xl font-bold text-google-text-primary dark:text-google-text-darkPrimary mt-0.5 truncate">
            {{ stats.totalInstances }}
          </div>
        </div>
      </div>

      <div class="p-3.5 sm:p-5 rounded-xl sm:rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark flex items-center gap-3 sm:gap-4 transition-colors">
        <div class="w-10 h-10 sm:w-12 sm:h-12 rounded-lg sm:rounded-xl flex items-center justify-center bg-green-50 dark:bg-green-900/30 text-google-green dark:text-google-green-dark shrink-0">
          <n-icon size="20" class="sm:hidden">
            <PlayOutline />
          </n-icon>
          <n-icon size="24" class="hidden sm:inline-flex">
            <PlayOutline />
          </n-icon>
        </div>
        <div class="min-w-0 flex-1">
          <div class="text-[11px] sm:text-xs font-medium text-google-text-muted dark:text-google-text-darkMuted truncate">
            正常运行中
          </div>
          <div class="text-xl sm:text-2xl font-bold text-green-600 dark:text-green-400 mt-0.5 truncate">
            {{ stats.runningCount }}
          </div>
        </div>
      </div>

      <div class="p-3.5 sm:p-5 rounded-xl sm:rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark flex items-center gap-3.5 sm:gap-4 transition-colors">
        <div class="w-10 h-10 sm:w-12 sm:h-12 rounded-lg sm:rounded-xl flex items-center justify-center bg-amber-50 dark:bg-amber-900/30 text-google-yellow dark:text-google-yellow-dark shrink-0">
          <n-icon size="20" class="sm:hidden">
            <TimeOutline />
          </n-icon>
          <n-icon size="24" class="hidden sm:inline-flex">
            <TimeOutline />
          </n-icon>
        </div>
        <div class="min-w-0 flex-1">
          <div class="text-[11px] sm:text-xs font-medium text-google-text-muted dark:text-google-text-darkMuted truncate">
            正在拉起拉取
          </div>
          <div class="text-xl sm:text-2xl font-bold text-yellow-600 dark:text-yellow-400 mt-0.5 truncate">
            {{ stats.startingCount }}
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
            启动异常失败
          </div>
          <div class="text-xl sm:text-2xl font-bold text-red-600 dark:text-red-400 mt-0.5 truncate">
            {{ stats.failedCount }}
          </div>
        </div>
      </div>
    </div>

    <div class="table-card p-3.5 sm:p-6 rounded-xl sm:rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark transition-colors">
      <div class="flex flex-col sm:flex-row sm:flex-wrap sm:items-center gap-2.5 sm:gap-3 mb-4 sm:mb-6">
        <VSearch
          v-model:value="searchKeyword"
          :placeholder="'搜索靶机 ID、赛题名称或用户...'"
          class="w-full sm:w-auto sm:flex-1 sm:min-w-[140px] sm:max-w-sm"
          @search="handleSearch"
          @clear="handleReset"
          @keydown.enter="handleSearch"
        />

        <div class="w-full sm:w-44 shrink-0">
          <VSelection
            v-model:value="filterStatus"
            :options="statusOptions"
            :placeholder="'全部状态'"
            clearable
            class="w-full"
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

        <div class="w-full sm:w-auto sm:ml-auto flex items-center gap-2 shrink-0 flex-wrap">
          <VBtn
            clz="danger"
            size="medium"
            :left-icon="StopCircleOutline"
            :disabled="checkedRowKeys.length === 0"
            class="flex-1 sm:flex-none"
            @click="handleBatchStop"
          >
            {{ t('admin.containers.batch_stop', '批量销毁') }} ({{ checkedRowKeys.length }})
          </VBtn>

          <VBtn
            clz="primary"
            size="medium"
            :left-icon="PlayOutline"
            class="flex-1 sm:flex-none"
            @click="showStartModal = true"
          >
            {{ t('admin.containers.manual_start', '启动容器') }}
          </VBtn>
        </div>
      </div>

      <VTable
        :columns="tableColumns"
        :data="containerList"
        :loading="loading"
        :pagination="false"
        :bordered="false"
        :checked-row-keys="checkedRowKeys"
        :row-key="(row: AdminContainerInstanceVO) => row.id"
        :scroll-x="1050"
        @update:checked-row-keys="handleCheckedRowKeysChange"
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

    <AdminContainerLanuchModal
      v-model:show="showStartModal"
      @success="handleLaunchSuccess"
    />

    <VModal
      v-model:show="showExecModal"
      :title="'容器在线命令调试 (Exec)'"
      :width="640"
    >
      <div class="space-y-4 py-2">
        <div class="flex items-center gap-2">
          <div class="flex-1">
            <VInput
              v-model:value="execCmd"
              placeholder="'输入调试命令 (如: ls -la / 或 ps aux)'"
              clearable
              @keyup.enter="handleExecCommand"
            />
          </div>
          <VBtn
            clz="primary"
            size="medium"
            :loading="executing"
            @click="handleExecCommand"
          >
            执行
          </VBtn>
        </div>

        <div v-if="execResult" class="space-y-2">
          <div class="text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary flex items-center justify-between">
            <span>退出状态码: <span :class="execResult.exitCode === 0 ? 'text-green-500' : 'text-red-500'">{{ execResult.exitCode }}</span></span>
          </div>
          <pre class="p-3 rounded-lg bg-gray-900 text-gray-100 font-mono text-xs overflow-x-auto max-h-64 whitespace-pre-wrap">{{ execResult.output || '(无输出结果)' }}</pre>
        </div>
      </div>

      <template #footer>
        <div class="flex justify-end w-full">
          <VBtn clz="normal" @click="showExecModal = false">
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
  CubeOutline,
  PlayOutline,
  StopCircleOutline,
  TimeOutline,
  AlertCircleOutline,
  SearchOutline,
  RefreshOutline
} from '@vicons/ionicons5'
import VBtn from '@/components/common/VBtn.vue'
import VInput from '@/components/common/VInput.vue'
import VSelection from '@/components/common/VSelection.vue'
import VTable from '@/components/common/VTable.vue'
import VPagination from '@/components/common/VPagination.vue'
import VModal from '@/components/common/VModal.vue'
import VSearch from '@/components/common/VSearch.vue'
import AdminContainerLanuchModal from './AdminContainerLanuchModal.vue'
import type {
  AdminContainerInstanceVO,
  AdminContainerStatisticsVO,
  AdminContainerExecResultVO,
  InstanceStartupStatus
} from '@/types/container'
import {
  getAdminContainerStatisticsApi,
  getAdminContainerListApi,
  stopAdminContainerApi,
  batchStopAdminContainersApi,
  renewAdminContainerApi,
  execAdminContainerApi
} from '@/api/container'

defineOptions({
  name: 'AdminContainerView'
})

const { t } = useI18n()
const message = useMessage()
const dialog = useDialog()

const loading = ref(false)
const containerList = ref<AdminContainerInstanceVO[]>([])
const checkedRowKeys = ref<(string | number)[]>([])

const searchKeyword = ref('')
const filterStatus = ref<string | null>(null)

const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

const stats = ref<AdminContainerStatisticsVO>({
  totalInstances: 0,
  runningCount: 0,
  startingCount: 0,
  failedCount: 0,
  todayCreatedCount: 0,
  activeContainerType: 'DOCKER_SWARM'
})

const showStartModal = ref(false)

const showExecModal = ref(false)
const currentExecId = ref<string | number | null>(null)
const execCmd = ref('whoami')
const executing = ref(false)
const execResult = ref<AdminContainerExecResultVO | null>(null)

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '运行中 (RUNNING)', value: 'RUNNING' },
  { label: '拉取中 (DOWNLOADING_IMAGE)', value: 'DOWNLOADING_IMAGE' },
  { label: '正在启动 (STARTING)', value: 'STARTING' },
  { label: '启动异常 (FAILED)', value: 'FAILED' },
  { label: '已停止 (STOPPED)', value: 'STOPPED' }
]

function getStatusText(status: InstanceStartupStatus): string {
  switch (status) {
    case 'RUNNING':
      return '运行中'
    case 'STARTING':
      return '启动中'
    case 'DOWNLOADING_IMAGE':
      return '镜像拉取'
    case 'FAILED':
      return '失败'
    case 'STOPPED':
      return '已停止'
    default:
      return status || '-'
  }
}

const tableColumns = computed<DataTableColumns<AdminContainerInstanceVO>>(() => [
  {
    type: 'selection',
    width: 48
  },
  {
    title: '实例ID',
    key: 'containerId',
    minWidth: 180,
    render(row) {
      const displayId = row.containerId || String(row.id)
      return h('div', { class: 'space-y-0.5 max-w-[260px]' }, [
        h(
          'div',
          {
            class: 'font-mono text-xs font-semibold text-google-text-primary dark:text-google-text-darkPrimary truncate',
            title: displayId
          },
          displayId
        ),
        row.challengeTitle
          ? h(
              'div',
              {
                class: 'text-xs text-google-text-secondary dark:text-google-text-darkSecondary truncate',
                title: row.challengeTitle
              },
              row.challengeTitle
            )
          : null
      ])
    }
  },
  {
    title: '创建用户',
    key: 'username',
    width: 120,
    render(row) {
      return row.username || '-'
    }
  },
  {
    title: '容器状态',
    key: 'status',
    width: 100,
    render(row) {
      return getStatusText(row.status)
    }
  },
  {
    title: t('admin.containers.table.remainingTime', '剩余时长'),
    key: 'remainingSeconds',
    width: 120,
    render(row) {
      const m = Math.floor((row.remainingSeconds || 0) / 60)
      return `${m} 分钟`
    }
  },
  {
    title: '访问入口',
    key: 'accessUrl',
    minWidth: 180,
    render(row) {
      return row.accessUrl
        ? h('a', { href: row.accessUrl, target: '_blank', class: 'font-mono text-xs text-google-blue dark:text-google-blue-dark hover:underline truncate' }, row.accessUrl)
        : '-'
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 180,
    align: 'center',
    render(row) {
      return h('div', { class: 'flex items-center justify-center gap-1.5' }, [
        h(
          VBtn,
          {
            clz: 'normal',
            size: 'small',
            onClick: () => handleRenew(row.id)
          },
          { default: () => '续期' }
        ),
        h(
          VBtn,
          {
            clz: 'normal',
            size: 'small',
            onClick: () => openExec(row.id)
          },
          { default: () => '调试' }
        ),
        h(
          VBtn,
          {
            clz: 'danger',
            size: 'small',
            onClick: () => handleStop(row.id)
          },
          { default: () => '销毁' }
        )
      ])
    }
  }
])

async function fetchStats() {
  try {
    const res = await getAdminContainerStatisticsApi()
    if (res.data) {
      stats.value = res.data
    }
  } catch {}
}

async function fetchContainers() {
  loading.value = true
  try {
    const res = await getAdminContainerListApi({
      page: pagination.page,
      size: pagination.pageSize,
      keyword: searchKeyword.value.trim() || undefined,
      status: (filterStatus.value || undefined) as InstanceStartupStatus | undefined
    })
    if (res.data) {
      containerList.value = res.data.list ?? []
      pagination.itemCount = Number(res.data.total) || 0
    }
  } catch {
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchContainers()
}

function handleReset() {
  searchKeyword.value = ''
  filterStatus.value = null
  pagination.page = 1
  fetchContainers()
}

function handlePageChange(page: number) {
  pagination.page = page
  fetchContainers()
}

function handlePageSizeChange(size: number) {
  pagination.pageSize = size
  pagination.page = 1
  fetchContainers()
}

function handleCheckedRowKeysChange(keys: (string | number)[]) {
  checkedRowKeys.value = keys
}

async function handleLaunchSuccess() {
  await Promise.all([fetchStats(), fetchContainers()])
}

async function handleRenew(id: string | number) {
  try {
    await renewAdminContainerApi(id, { durationMinutes: 60 })
    message.success('靶机租期已延长 60 分钟')
    await fetchContainers()
  } catch {
  }
}

function openExec(id: string | number) {
  currentExecId.value = id
  execCmd.value = 'whoami'
  execResult.value = null
  showExecModal.value = true
}

async function handleExecCommand() {
  if (!currentExecId.value || !execCmd.value.trim()) return
  executing.value = true
  try {
    const res = await execAdminContainerApi(currentExecId.value, {
      command: execCmd.value.trim()
    })
    if (res.data) {
      execResult.value = res.data
    }
  } catch {
  } finally {
    executing.value = false
  }
}

function handleStop(id: string | number) {
  dialog.warning({
    title: '确认销毁靶机',
    content: '销毁后该靶机实例及关联端口映射将立即释放，是否继续？',
    positiveText: '确认销毁',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await stopAdminContainerApi(id)
        message.success('容器已销毁释放')
        await Promise.all([fetchStats(), fetchContainers()])
      } catch {
      }
    }
  })
}

function handleBatchStop() {
  if (checkedRowKeys.value.length === 0) return
  dialog.warning({
    title: '确认批量销毁靶机',
    content: `将强制停止并释放选中的 ${checkedRowKeys.value.length} 个靶机容器，是否继续？`,
    positiveText: '确认销毁',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await batchStopAdminContainersApi({ instanceIds: checkedRowKeys.value })
        message.success('已批量触发停止')
        checkedRowKeys.value = []
        await Promise.all([fetchStats(), fetchContainers()])
      } catch {
      }
    }
  })
}

onMounted(() => {
  fetchStats()
  fetchContainers()
})
</script>
