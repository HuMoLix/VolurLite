<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <h3 class="text-sm font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
        {{ t('admin.competitions.writeups_list', '参赛队伍题解 (Writeup) 提交记录') }}
      </h3>
      <VBtn
        clz="normal"
        size="medium"
        :left-icon="RefreshOutline"
        :loading="loadingWriteups"
        @click="fetchWriteups"
      >
        刷新列表
      </VBtn>
    </div>

    <VTable
      :columns="writeupColumns"
      :data="writeups"
      :loading="loadingWriteups"
      :pagination="false"
      size="small"
      :bordered="false"
    >
      <template #empty>
        <div class="py-12 text-center text-xs text-google-text-secondary dark:text-google-text-darkSecondary">
          暂无参赛队伍提交的题解记录
        </div>
      </template>
    </VTable>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, h } from 'vue'
import { useI18n } from 'vue-i18n'
import { RefreshOutline } from '@vicons/ionicons5'
import type { DataTableColumns } from 'naive-ui'
import VBtn from '@/components/common/VBtn.vue'
import VTable from '@/components/common/VTable.vue'
import type { CompetitionWriteupVO } from '@/types/competition'
import { listAdminCompetitionWriteupsApi } from '@/api/competition'

defineOptions({
  name: 'AdminCtfCompetitionWriteupView'
})

const props = defineProps<{
  compId: string | number
}>()

const { t } = useI18n()

const writeups = ref<CompetitionWriteupVO[]>([])
const loadingWriteups = ref(false)

const writeupColumns = computed<DataTableColumns<CompetitionWriteupVO>>(() => [
  {
    title: '战队名称',
    key: 'teamName',
    width: 180,
    render(row) {
      return h('span', { class: 'font-semibold text-google-text-primary dark:text-google-text-darkPrimary' }, row.teamName)
    }
  },
  {
    title: '提交人',
    key: 'username',
    width: 140,
    render(row) {
      return row.username
    }
  },
  {
    title: '文件名称',
    key: 'fileName',
    render(row) {
      return row.fileUrl
        ? h('a', { href: row.fileUrl, target: '_blank', class: 'text-xs text-google-blue dark:text-google-blue-dark hover:underline truncate' }, row.fileName)
        : h('span', { class: 'text-xs' }, row.fileName)
    }
  },
  {
    title: '评阅状态',
    key: 'status',
    width: 120,
    render(row) {
      return row.status || 'SUBMITTED'
    }
  },
  {
    title: '得分',
    key: 'score',
    width: 100,
    render(row) {
      return row.score !== null && row.score !== undefined ? `${row.score} 分` : '-'
    }
  }
])

async function fetchWriteups() {
  if (!props.compId) return
  loadingWriteups.value = true
  try {
    const res = await listAdminCompetitionWriteupsApi(props.compId)
    if (res.data) {
      writeups.value = res.data
    }
  } catch {
    writeups.value = []
  } finally {
    loadingWriteups.value = false
  }
}

onMounted(() => {
  fetchWriteups()
})
</script>
