<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <h3 class="text-sm font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
        {{ t('admin.competitions.logs_list', '比赛运行与选手答题操作日志') }}
      </h3>
      <VBtn
        clz="normal"
        size="medium"
        :left-icon="RefreshOutline"
        :loading="loadingLogs"
        @click="fetchLogs"
      >
        刷新日志
      </VBtn>
    </div>

    <VTable
      :columns="logColumns"
      :data="compLogs"
      :loading="loadingLogs"
      :pagination="false"
      size="small"
      :bordered="false"
    >
      <template #empty>
        <div class="py-12 text-center text-xs text-google-text-secondary dark:text-google-text-darkSecondary">
          暂无比赛日志记录
        </div>
      </template>
    </VTable>

    <div v-if="logTotal > 0" class="flex justify-end pt-2">
      <VPagination
        v-model:page="logPage"
        v-model:page-size="logPageSize"
        :item-count="logTotal"
        :show-size-picker="true"
        :page-sizes="[10, 20, 50]"
        size="small"
        @update:page="handleLogPageChange"
        @update:page-size="handleLogPageSizeChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, h } from 'vue'
import { useI18n } from 'vue-i18n'
import { RefreshOutline } from '@vicons/ionicons5'
import type { DataTableColumns } from 'naive-ui'
import VBtn from '@/components/common/VBtn.vue'
import VTable from '@/components/common/VTable.vue'
import VPagination from '@/components/common/VPagination.vue'
import type { AdminCompetitionLogVO } from '@/types/competition'
import { getAdminCompetitionLogListApi } from '@/api/competition'

defineOptions({
  name: 'AdminTheoryCompetitionLogView'
})

const props = defineProps<{
  compId: string | number
}>()

const { t } = useI18n()

const compLogs = ref<AdminCompetitionLogVO[]>([])
const loadingLogs = ref(false)
const logPage = ref(1)
const logPageSize = ref(10)
const logTotal = ref(0)

const logColumns = computed<DataTableColumns<AdminCompetitionLogVO>>(() => [
  {
    title: '操作人',
    key: 'username',
    width: 140,
    render(row) {
      return row.username || '-'
    }
  },
  {
    title: '战队',
    key: 'teamName',
    width: 160,
    render(row) {
      return row.teamName || '-'
    }
  },
  {
    title: '动作/事件',
    key: 'action',
    minWidth: 160,
    render(row) {
      return row.action
    }
  },
  {
    title: '状态',
    key: 'status',
    width: 90,
    align: 'center',
    render(row) {
      return h(
        'span',
        {
          class: row.status ? 'text-green-600 dark:text-green-400 font-medium' : 'text-red-600 dark:text-red-400 font-medium'
        },
        row.status ? '成功' : '失败'
      )
    }
  },
  {
    title: '记录时间',
    key: 'createdAt',
    width: 180,
    render(row) {
      return row.createdAt ? new Date(row.createdAt).toLocaleString() : '-'
    }
  }
])

async function fetchLogs() {
  if (!props.compId) return
  loadingLogs.value = true
  try {
    const res = await getAdminCompetitionLogListApi(props.compId, {
      page: logPage.value,
      size: logPageSize.value
    })
    if (res.data) {
      compLogs.value = res.data.list ?? []
      logTotal.value = Number(res.data.total) || 0
    }
  } catch {
    compLogs.value = []
  } finally {
    loadingLogs.value = false
  }
}

function handleLogPageChange(page: number) {
  logPage.value = page
  fetchLogs()
}

function handleLogPageSizeChange(pageSize: number) {
  logPageSize.value = pageSize
  logPage.value = 1
  fetchLogs()
}

onMounted(() => {
  fetchLogs()
})
</script>
