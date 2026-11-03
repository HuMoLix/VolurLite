<template>
  <div class="admin-log-view flex flex-col gap-4 sm:gap-6">
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h2 class="text-xl font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
          {{ t('admin.logs.title', '系统日志') }}
        </h2>
        <p class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary mt-1">
          {{ t('admin.logs.desc', '审计全平台管理员操作行为、认证鉴权审计与核心业务安全调用追踪') }}
        </p>
      </div>
    </div>

    <div class="grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4">
      <div class="p-3.5 sm:p-5 rounded-xl sm:rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark flex items-center gap-3 sm:gap-4 transition-colors">
        <div class="w-10 h-10 sm:w-12 sm:h-12 rounded-lg sm:rounded-xl flex items-center justify-center bg-blue-50 dark:bg-blue-900/30 text-google-blue dark:text-google-blue-dark shrink-0">
          <n-icon size="20" class="sm:hidden">
            <ReaderOutline />
          </n-icon>
          <n-icon size="24" class="hidden sm:inline-flex">
            <ReaderOutline />
          </n-icon>
        </div>
        <div class="min-w-0 flex-1">
          <div class="text-[11px] sm:text-xs font-medium text-google-text-muted dark:text-google-text-darkMuted truncate">
            累计审计日志
          </div>
          <div class="text-xl sm:text-2xl font-bold text-google-text-primary dark:text-google-text-darkPrimary mt-0.5 truncate">
            {{ stats.totalLogs }}
          </div>
        </div>
      </div>

      <div class="p-3.5 sm:p-5 rounded-xl sm:rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark flex items-center gap-3 sm:gap-4 transition-colors">
        <div class="w-10 h-10 sm:w-12 sm:h-12 rounded-lg sm:rounded-xl flex items-center justify-center bg-cyan-50 dark:bg-cyan-900/30 text-cyan-600 dark:text-cyan-400 shrink-0">
          <n-icon size="20" class="sm:hidden">
            <PersonOutline />
          </n-icon>
          <n-icon size="24" class="hidden sm:inline-flex">
            <PersonOutline />
          </n-icon>
        </div>
        <div class="min-w-0 flex-1">
          <div class="text-[11px] sm:text-xs font-medium text-google-text-muted dark:text-google-text-darkMuted truncate">
            登录与认证
          </div>
          <div class="text-xl sm:text-2xl font-bold text-google-text-primary dark:text-google-text-darkPrimary mt-0.5 truncate">
            {{ stats.authLogs }}
          </div>
        </div>
      </div>

      <div class="p-3.5 sm:p-5 rounded-xl sm:rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark flex items-center gap-3 sm:gap-4 transition-colors">
        <div class="w-10 h-10 sm:w-12 sm:h-12 rounded-lg sm:rounded-xl flex items-center justify-center bg-purple-50 dark:bg-purple-900/30 text-purple-600 dark:text-purple-400 shrink-0">
          <n-icon size="20" class="sm:hidden">
            <ShieldOutline />
          </n-icon>
          <n-icon size="24" class="hidden sm:inline-flex">
            <ShieldOutline />
          </n-icon>
        </div>
        <div class="min-w-0 flex-1">
          <div class="text-[11px] sm:text-xs font-medium text-google-text-muted dark:text-google-text-darkMuted truncate">
            安全风控审计
          </div>
          <div class="text-xl sm:text-2xl font-bold text-google-text-primary dark:text-google-text-darkPrimary mt-0.5 truncate">
            {{ stats.securityLogs }}
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
            异常告警记录
          </div>
          <div class="text-xl sm:text-2xl font-bold text-google-text-primary dark:text-google-text-darkPrimary mt-0.5 truncate">
            {{ stats.errorLogs }}
          </div>
        </div>
      </div>
    </div>

    <div class="table-card p-3.5 sm:p-6 rounded-xl sm:rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark transition-colors">
      <div class="flex flex-col sm:flex-row sm:flex-wrap sm:items-center gap-2.5 sm:gap-3 mb-4 sm:mb-6">
        <VSearch
          v-model:value="searchKeyword"
          :placeholder="'搜索操作人、动作或请求 URI...'"
          class="w-full sm:w-auto sm:flex-1 sm:min-w-[140px] sm:max-w-sm"
          @search="handleSearch"
          @clear="handleReset"
          @keydown.enter="handleSearch"
        />

        <div class="w-full sm:w-36 shrink-0">
          <VSelection
            v-model:value="filterLevel"
            :options="levelOptions"
            :placeholder="'全部级别'"
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

        <div class="w-full sm:w-auto sm:ml-auto shrink-0">
          <VBtn
            clz="normal"
            size="medium"
            :left-icon="RefreshOutline"
            :loading="loading"
            class="w-full sm:w-auto"
            @click="fetchLogs"
          >
            {{ t('common.refresh', '刷新日志') }}
          </VBtn>
        </div>
      </div>

      <VTable
        :columns="tableColumns"
        :data="logList"
        :loading="loading"
        :pagination="false"
        :bordered="false"
        :row-key="(row: AdminLogVO) => String(row.id)"
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
      :title="'审计日志详情'"
      :width="640"
    >
      <div v-if="selectedLog" class="space-y-4 text-xs py-2">
        <div class="grid grid-cols-2 gap-3 p-3 rounded-lg bg-gray-50 dark:bg-[#202124] border border-google-border-light dark:border-google-border-dark">
          <div><span class="text-google-text-secondary dark:text-google-text-darkSecondary">操作人: </span><span class="font-medium font-mono">{{ selectedLog.operator }}</span></div>
          <div><span class="text-google-text-secondary dark:text-google-text-darkSecondary">IP 地址: </span><span class="font-mono">{{ selectedLog.operatorIp || '-' }}</span></div>
          <div><span class="text-google-text-secondary dark:text-google-text-darkSecondary">所属模块: </span><span>{{ selectedLog.module }}</span></div>
          <div><span class="text-google-text-secondary dark:text-google-text-darkSecondary">耗时: </span><span class="font-mono">{{ selectedLog.durationMs ?? 0 }} ms</span></div>
          <div class="col-span-2"><span class="text-google-text-secondary dark:text-google-text-darkSecondary">请求接口: </span><code class="text-google-blue dark:text-google-blue-dark font-mono">{{ selectedLog.requestMethod }} {{ selectedLog.requestUri }}</code></div>
        </div>

        <div v-if="selectedLog.params">
          <div class="font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1">入参请求体 (Payload):</div>
          <pre class="p-3 rounded-lg bg-gray-900 text-gray-200 font-mono text-[11px] overflow-x-auto max-h-40 whitespace-pre-wrap">{{ selectedLog.params }}</pre>
        </div>

        <div v-if="selectedLog.result">
          <div class="font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1">执行响应结果:</div>
          <pre class="p-3 rounded-lg bg-gray-900 text-green-400 font-mono text-[11px] overflow-x-auto max-h-40 whitespace-pre-wrap">{{ selectedLog.result }}</pre>
        </div>

        <div v-if="selectedLog.errorMessage">
          <div class="font-medium text-red-500 mb-1">异常错误堆栈:</div>
          <pre class="p-3 rounded-lg bg-red-950/40 border border-red-800 text-red-300 font-mono text-[11px] overflow-x-auto max-h-40 whitespace-pre-wrap">{{ selectedLog.errorMessage }}</pre>
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
import { NIcon, useMessage, type DataTableColumns } from 'naive-ui'
import {
  ReaderOutline,
  PersonOutline,
  ShieldOutline,
  AlertCircleOutline,
  SearchOutline,
  RefreshOutline
} from '@vicons/ionicons5'
import VBtn from '@/components/common/VBtn.vue'
import VTable from '@/components/common/VTable.vue'
import VSearch from '@/components/common/VSearch.vue'
import VSelection from '@/components/common/VSelection.vue'
import VPagination from '@/components/common/VPagination.vue'
import VModal from '@/components/common/VModal.vue'
import type {
  AdminLogVO,
  AdminLogStatisticsVO
} from '@/types/log'
import {
  getAdminLogStatisticsApi,
  getAdminLogListApi
} from '@/api/log'

defineOptions({
  name: 'AdminLogView'
})

const { t } = useI18n()
const message = useMessage()

const loading = ref(false)
const logList = ref<AdminLogVO[]>([])

const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

const searchKeyword = ref('')
const filterLevel = ref<string | null>(null)

const stats = ref<AdminLogStatisticsVO>({
  totalLogs: 0,
  authLogs: 0,
  securityLogs: 0,
  errorLogs: 0
})

const showDetailModal = ref(false)
const selectedLog = ref<AdminLogVO | null>(null)

const levelOptions = [
  { label: '全部级别', value: '' },
  { label: '信息 (INFO)', value: 'INFO' },
  { label: '警告 (WARN)', value: 'WARN' },
  { label: '错误 (ERROR)', value: 'ERROR' }
]

const tableColumns = computed<DataTableColumns<AdminLogVO>>(() => [
  {
    title: '级别',
    key: 'level',
    width: 90,
    align: 'center',
    render(row) {
      return row.level || '-'
    }
  },
  {
    title: '所属模块',
    key: 'module',
    width: 120,
    render(row) {
      return h('span', { class: 'text-xs font-medium text-google-text-primary dark:text-google-text-darkPrimary' }, row.module)
    }
  },
  {
    title: '操作人',
    key: 'operator',
    width: 120,
    render(row) {
      return h('div', { class: 'space-y-0.5' }, [
        h('span', { class: 'text-xs font-semibold' }, row.operator),
        row.operatorIp ? h('div', { class: 'text-[11px] font-mono text-google-text-secondary dark:text-google-text-darkSecondary' }, row.operatorIp) : null
      ])
    }
  },
  {
    title: '动作说明',
    key: 'action',
    minWidth: 160,
    render(row) {
      return h('span', { class: 'text-xs' }, row.action)
    }
  },
  {
    title: '状态',
    key: 'status',
    width: 80,
    align: 'center',
    render(row) {
      return row.status ? '成功' : '失败'
    }
  },
  {
    title: '耗时',
    key: 'durationMs',
    width: 90,
    render(row) {
      return `${row.durationMs ?? 0} ms`
    }
  },
  {
    title: '时间',
    key: 'createdAt',
    width: 160,
    render(row) {
      return h('span', { class: 'text-xs text-google-text-secondary dark:text-google-text-darkSecondary font-mono' }, row.createdAt ? new Date(row.createdAt).toLocaleString() : '-')
    }
  },
  {
    title: t('common.actions', '操作'),
    key: 'actions',
    width: 90,
    align: 'center',
    render(row) {
      return h(
        VBtn,
        {
          clz: 'normal',
          size: 'small',
          onClick: () => {
            selectedLog.value = row
            showDetailModal.value = true
          }
        },
        { default: () => '详情' }
      )
    }
  }
])

async function fetchStats() {
  try {
    const res = await getAdminLogStatisticsApi()
    if (res.data) {
      stats.value = res.data
    }
  } catch {}
}

async function fetchLogs() {
  loading.value = true
  try {
    const res = await getAdminLogListApi({
      page: pagination.page,
      size: pagination.pageSize,
      keyword: searchKeyword.value.trim() || undefined,
      level: filterLevel.value || undefined
    })
    if (res.data) {
      logList.value = res.data.list ?? []
      pagination.itemCount = Number(res.data.total) || 0
    }
  } catch (err: any) {
    message.error(err.message || '获取系统日志失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchLogs()
}

function handleReset() {
  searchKeyword.value = ''
  filterLevel.value = null
  pagination.page = 1
  fetchLogs()
}

function handlePageChange(page: number) {
  pagination.page = page
  fetchLogs()
}

function handlePageSizeChange(size: number) {
  pagination.pageSize = size
  pagination.page = 1
  fetchLogs()
}

onMounted(() => {
  fetchStats()
  fetchLogs()
})
</script>
