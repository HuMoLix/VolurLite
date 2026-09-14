<template>
  <div class="container-tab">
    <!-- Section 1: Team Container Concurrency Quota Configuration -->
    <n-card class="quota-config-card" size="small" :bordered="false">
      <div class="quota-config-inner">
        <div class="quota-left">
          <div class="quota-title">队伍靶机容器并发配额配置</div>
          <div class="quota-desc">
            控制比赛中每支战队最多能同时启动的题目容器数量。超过该上限时，选手需主动销毁已启动的容器后方可开启新靶机。
          </div>
        </div>
        <div class="quota-right">
          <div class="input-wrap">
            <span class="label">单队伍上限：</span>
            <n-input-number
              v-model:value="maxContainers"
              :min="1"
              :max="10"
              size="small"
              style="width: 110px;"
            />
            <span class="unit">个 / 队</span>
          </div>
          <n-button
            class="theme-primary-btn"
            size="small"
            :loading="savingQuota"
            @click="handleSaveQuota"
          >
            保存配额设置
          </n-button>
        </div>
      </div>
    </n-card>

    <!-- Section 2: Statistics Ribbon (reference AdminContainerView.vue) -->
    <div class="stat-ribbon">
      <div class="stat-mini-card">
        <div class="stat-icon-wrapper primary">
          <n-icon size="18">
            <CubeOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.totalInstances }}</div>
          <div class="stat-label">比赛容器总量</div>
        </div>
      </div>

      <div class="stat-mini-card">
        <div class="stat-icon-wrapper success">
          <n-icon size="18">
            <PlayCircleOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.runningCount }}</div>
          <div class="stat-label">运行中实例</div>
        </div>
      </div>

      <div class="stat-mini-card">
        <div class="stat-icon-wrapper warning">
          <n-icon size="18">
            <TimeOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.startingCount }}</div>
          <div class="stat-label">启动中</div>
        </div>
      </div>

      <div class="stat-mini-card">
        <div class="stat-icon-wrapper secondary">
          <n-icon size="18">
            <ServerOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ maxContainers }} 个/队</div>
          <div class="stat-label">队伍配额上限</div>
        </div>
      </div>
    </div>

    <!-- Section 3: Active Competition Containers Table Card -->
    <AppTableCard
      :bordered="true"
      :table-bordered="true"
      :columns="columns"
      :data="instances"
      :loading="tableLoading"
      :scroll-x="980"
      :row-key="(row) => row.id"
      table-class="container-tab-table"
      standalone-pagination
      :page="pagination.page"
      :page-size="pagination.pageSize"
      :item-count="pagination.total"
      @update:page="handlePageChange"
      @update:page-size="handlePageSizeChange"
    >
      <template #filter-left>
        <n-input
          v-model:value="searchKeyword"
          placeholder="搜索题目、用户名或容器ID..."
          clearable
          style="width: 260px;"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <n-icon :component="SearchOutline" />
          </template>
        </n-input>
        <n-select
          v-model:value="selectedStatus"
          :options="statusOptions"
          placeholder="容器运行状态"
          clearable
          style="width: 160px;"
          @update:value="handleSearch"
        />
        <n-button size="small" @click="handleSearch">查询</n-button>
      </template>

      <template #filter-right>
        <n-button quaternary size="small" :loading="tableLoading" @click="() => fetchInstances()">
          <template #icon>
            <n-icon :component="RefreshOutline" />
          </template>
          刷新容器列表
        </n-button>
      </template>

      <template #empty>
        <div style="padding: 24px; text-align: center; color: #888;">
          当前比赛暂无运行中的题目靶机容器
        </div>
      </template>
    </AppTableCard>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, h, onMounted, onUnmounted } from 'vue'
import {
  NCard,
  NButton,
  NIcon,
  NInput,
  NInputNumber,
  NSelect,
  NTag,
  useMessage,
  useDialog,
  type DataTableColumns,
  type SelectOption
} from 'naive-ui'
import AppTableCard from '@/components/common/AppTableCard.vue'
import {
  CubeOutline,
  PlayCircleOutline,
  TimeOutline,
  ServerOutline,
  SearchOutline,
  RefreshOutline,
  CopyOutline,
  OpenOutline
} from '@vicons/ionicons5'
import type { AdminContainerInstanceVO } from '@/types/container'
import {
  getAdminContainerListApi,
  stopAdminContainerApi,
  renewAdminContainerApi
} from '@/api/container'
import { updateCompetitionApi } from '@/api/competition'

interface Props {
  compId: string | number
  initialMaxContainers?: number
}

const props = withDefaults(defineProps<Props>(), {
  initialMaxContainers: 2
})

const emit = defineEmits<{
  (e: 'updateQuota', val: number): void
}>()

const message = useMessage()
const dialog = useDialog()

// Quota State
const maxContainers = ref<number>(props.initialMaxContainers)
const savingQuota = ref<boolean>(false)

watch(
  () => props.initialMaxContainers,
  (val) => {
    if (val != null) {
      maxContainers.value = val
    }
  }
)

// Table & Filter State
const tableLoading = ref<boolean>(false)
const instances = ref<AdminContainerInstanceVO[]>([])
const searchKeyword = ref<string>('')
const selectedStatus = ref<string | null>(null)

const statistics = reactive({
  totalInstances: 0,
  runningCount: 0,
  startingCount: 0
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

const statusOptions: SelectOption[] = [
  { label: '全部状态', value: '' },
  { label: '运行中 (RUNNING)', value: 'RUNNING' },
  { label: '启动中 (STARTING)', value: 'STARTING' },
  { label: '已停止 (STOPPED)', value: 'STOPPED' },
  { label: '启动失败 (FAILED)', value: 'FAILED' }
]

const columns: DataTableColumns<AdminContainerInstanceVO> = [
  {
    title: '赛题名称',
    key: 'challengeTitle',
    width: 180,
    render(row) {
      return h('div', { style: 'display: flex; flex-direction: column; gap: 4px;' }, [
        h('span', { style: 'font-weight: 600; color: var(--v-text-primary, #111);' }, row.challengeTitle || row.containerName || '独立赛题容器'),
        row.challengeCategory
          ? h(NTag, { size: 'tiny', type: 'info', bordered: false, style: 'width: fit-content;' }, {
              default: () => row.challengeCategory
            })
          : null
      ])
    }
  },
  {
    title: '队伍 / 使用者',
    key: 'username',
    width: 140,
    render(row) {
      return h('div', { style: 'display: flex; flex-direction: column;' }, [
        h('span', { style: 'font-weight: 500;' }, row.username || `UID:${row.userId}`),
        h('span', { style: 'font-size: 11px; color: #888;' }, `ID: ${row.userId}`)
      ])
    }
  },
  {
    title: '访问地址 (Access URL)',
    key: 'accessUrl',
    render(row) {
      if (!row.accessUrl) {
        return h('span', { style: 'color: #999; font-size: 12px;' }, '尚未分配端口')
      }
      return h('div', { style: 'display: flex; align-items: center; gap: 8px;' }, [
        h('code', {
          style: 'font-size: 12px; background: rgba(0,0,0,0.04); padding: 2px 6px; border-radius: 4px;'
        }, row.accessUrl),
        h(
          NButton,
          {
            size: 'tiny',
            quaternary: true,
            circle: true,
            onClick: () => copyText(row.accessUrl)
          },
          {
            icon: () => h(NIcon, null, { default: () => h(CopyOutline) })
          }
        ),
        h(
          NButton,
          {
            size: 'tiny',
            quaternary: true,
            circle: true,
            onClick: () => window.open(row.accessUrl, '_blank')
          },
          {
            icon: () => h(NIcon, null, { default: () => h(OpenOutline) })
          }
        )
      ])
    }
  },
  {
    title: '剩余租期 (TTL)',
    key: 'remainingSeconds',
    width: 140,
    render(row) {
      if (row.remainingSeconds <= 0) {
        return h(NTag, { size: 'small', type: 'error', round: true }, { default: () => '已到期' })
      }
      const mins = Math.floor(row.remainingSeconds / 60)
      const secs = row.remainingSeconds % 60
      return h('span', { style: 'font-family: monospace; font-size: 13px;' }, `${mins}分 ${secs}秒`)
    }
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render(row) {
      return h(NTag, { size: 'small', type: getStatusType(row.status), round: true }, {
        default: () => getStatusLabel(row.status)
      })
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 180,
    fixed: 'right',
    render(row) {
      const isAlive = row.status === 'RUNNING' || row.status === 'STARTING'
      return h('div', { style: 'display: flex; gap: 8px;' }, [
        h(
          NButton,
          {
            size: 'tiny',
            quaternary: true,
            disabled: !isAlive,
            onClick: () => handleRenew(row)
          },
          {
            default: () => '续期'
          }
        ),
        h(
          NButton,
          {
            size: 'tiny',
            quaternary: true,
            type: 'error',
            disabled: row.status === 'STOPPED',
            onClick: () => handleStop(row)
          },
          {
            default: () => '强制销毁'
          }
        )
      ])
    }
  }
]

let pollTimer: number | null = null

onMounted(() => {
  if (props.compId) {
    fetchInstances()
    pollTimer = window.setInterval(() => {
      fetchInstances(true)
    }, 15000)
  }
})

onUnmounted(() => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
})

async function handleSaveQuota() {
  savingQuota.value = true
  try {
    await updateCompetitionApi(props.compId, {
      config: {
        maxContainersPerTeam: maxContainers.value
      }
    })
    message.success('队伍靶机配额设置已保存')
    emit('updateQuota', maxContainers.value)
  } catch (error: any) {
    message.error(error.message || '保存配额失败')
  } finally {
    savingQuota.value = false
  }
}

async function fetchInstances(isSilent = false) {
  if (!isSilent) tableLoading.value = true
  try {
    const res = await getAdminContainerListApi({
      page: pagination.page,
      size: pagination.pageSize,
      stageId: Number(props.compId),
      keyword: searchKeyword.value.trim() ? searchKeyword.value.trim() : undefined,
      status: selectedStatus.value ? (selectedStatus.value as any) : undefined
    })

    const list = res.data?.list || []
    instances.value = list
    pagination.total = res.data?.total || 0

    // Update stats
    statistics.totalInstances = pagination.total
    statistics.runningCount = list.filter(i => i.status === 'RUNNING').length
    statistics.startingCount = list.filter(i => i.status === 'STARTING').length
  } catch (error: any) {
    if (!isSilent) {
      console.error('Fetch competition containers error:', error)
      message.error(error.message || '获取容器列表失败')
    }
  } finally {
    if (!isSilent) tableLoading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchInstances()
}

function handlePageChange(page: number) {
  pagination.page = page
  fetchInstances()
}

function handlePageSizeChange(pageSize: number) {
  pagination.pageSize = pageSize
  pagination.page = 1
  fetchInstances()
}

function handleRenew(row: AdminContainerInstanceVO) {
  dialog.info({
    title: '延长靶机容器时间',
    content: `确定为实例「${row.containerName || row.challengeTitle || '靶机容器'}」延长 60 分钟运行时间吗？`,
    positiveText: '确认续期',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await renewAdminContainerApi(row.id, { durationMinutes: 60 })
        message.success('容器租期延长成功')
        fetchInstances()
      } catch (error: any) {
        message.error(error.message || '续期失败')
      }
    }
  })
}

function handleStop(row: AdminContainerInstanceVO) {
  dialog.warning({
    title: '强制销毁并释放容器',
    content: `确定立即强制停止并销毁容器「${row.containerName || row.challengeTitle || '靶机容器'}」吗？容器释放后端口与环境将立即销毁。`,
    positiveText: '强制销毁',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await stopAdminContainerApi(row.id)
        message.success('容器已销毁并释放资源')
        fetchInstances()
      } catch (error: any) {
        message.error(error.message || '销毁容器失败')
      }
    }
  })
}

function copyText(text: string) {
  navigator.clipboard.writeText(text).then(() => {
    message.success('已复制到剪贴板')
  })
}

function getStatusType(status: string) {
  switch (status) {
    case 'RUNNING':
      return 'success'
    case 'STARTING':
      return 'warning'
    case 'STOPPED':
      return 'default'
    case 'FAILED':
      return 'error'
    default:
      return 'default'
  }
}

function getStatusLabel(status: string) {
  switch (status) {
    case 'RUNNING':
      return '运行中'
    case 'STARTING':
      return '启动中'
    case 'STOPPED':
      return '已停止'
    case 'FAILED':
      return '故障'
    default:
      return status
  }
}
</script>

<style scoped lang="scss">
.container-tab {
  display: flex;
  flex-direction: column;
  gap: 20px;

  .quota-config-card {
    background: var(--v-bg-muted, #f8f8f9);
    border: 1px solid var(--v-border-base, #e5e4e7);

    .quota-config-inner {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 16px;

      .quota-left {
        .quota-title {
          font-size: 15px;
          font-weight: 600;
          color: var(--v-text-primary, #111111);
          margin-bottom: 4px;
        }

        .quota-desc {
          font-size: 13px;
          color: #444444;
        }
      }

      .quota-right {
        display: flex;
        align-items: center;
        gap: 12px;

        .input-wrap {
          display: flex;
          align-items: center;
          gap: 6px;

          .label {
            font-size: 13px;
            color: #444444;
          }

          .unit {
            font-size: 13px;
            color: var(--v-text-muted, #999999);
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

        &.success {
          background: rgba(24, 160, 88, 0.1);
          color: #18a058;
        }

        &.warning {
          background: rgba(240, 160, 32, 0.1);
          color: #f0a020;
        }

        &.secondary {
          background: rgba(32, 128, 240, 0.1);
          color: #2080f0;
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

  .table-card {
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
</style>
