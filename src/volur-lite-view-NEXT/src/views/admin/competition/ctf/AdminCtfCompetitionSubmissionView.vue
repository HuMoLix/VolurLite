<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <h3 class="text-sm font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
        {{ t('admin.competitions.tab_submissions', '提交记录') }}
      </h3>
    </div>

    <div class="flex flex-col md:flex-row md:items-center justify-between gap-3">
      <div class="flex items-center gap-3 flex-wrap flex-1">
        <div class="w-64">
          <VSearch
            v-model:value="searchKeyword"
            placeholder="搜索战队、操作人或Flag..."
            clearable
            @search="handleSearch"
            @clear="handleReset"
          />
        </div>
        <div class="w-48">
          <VSelection
            v-model:value="selectedChallengeId"
            :options="challengeFilterOptions"
            placeholder="按赛题筛选"
            clearable
            @update:value="handleSearch"
          />
        </div>
        <VBtn
          clz="normal"
          size="medium"
          :left-icon="RefreshOutline"
          :loading="loadingSubmissions"
          @click="fetchSubmissions"
        >
          刷新列表
        </VBtn>
        <VBtn clz="normal" size="medium" @click="handleReset">
          重置
        </VBtn>
      </div>

      <div class="flex items-center gap-2">
        <VBtn
          clz="normal"
          size="medium"
          :left-icon="SearchOutline"
          @click="handleSearch"
        >
          查询
        </VBtn>
      </div>
    </div>

    <VTable
      :columns="submissionColumns"
      :data="submissions"
      :loading="loadingSubmissions"
      :pagination="false"
      size="small"
      :bordered="false"
    >
      <template #empty>
        <div class="py-12 text-center text-xs text-google-text-secondary dark:text-google-text-darkSecondary">
          暂无提交记录
        </div>
      </template>
    </VTable>

    <div v-if="submissionTotal > 0" class="flex justify-end pt-2">
      <VPagination
        :page="page"
        :page-size="pageSize"
        :item-count="submissionTotal"
        @update:page="handlePageChange"
        @update:page-size="handlePageSizeChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, h } from 'vue'
import { useI18n } from 'vue-i18n'
import { SearchOutline, RefreshOutline } from '@vicons/ionicons5'
import type { DataTableColumns } from 'naive-ui'
import VBtn from '@/components/common/VBtn.vue'
import VSearch from '@/components/common/VSearch.vue'
import VSelection from '@/components/common/VSelection.vue'
import VTable from '@/components/common/VTable.vue'
import VPagination from '@/components/common/VPagination.vue'
import type {
  CtfCompetitionSubmissionVO,
  CtfCompetitionChallengeVO
} from '@/types/competition'
import {
  listAdminCtfSubmissionsApi,
  listMountedCtfChallengesApi
} from '@/api/competition'

defineOptions({
  name: 'AdminCtfCompetitionSubmissionView'
})

const props = defineProps<{
  compId: string | number
}>()

const { t } = useI18n()

const submissions = ref<CtfCompetitionSubmissionVO[]>([])
const loadingSubmissions = ref(false)
const searchKeyword = ref('')
const selectedChallengeId = ref<number | string | null>(null)
const page = ref(1)
const pageSize = ref(10)
const submissionTotal = ref(0)

const mountedChallenges = ref<CtfCompetitionChallengeVO[]>([])

const challengeFilterOptions = computed(() => {
  const options = [{ label: '全部题目', value: '' }]
  mountedChallenges.value.forEach((c) => {
    options.push({
      label: c.name || `题目 #${c.id}`,
      value: String(c.id)
    })
  })
  return options
})

const submissionColumns = computed<DataTableColumns<CtfCompetitionSubmissionVO>>(() => [
  {
    title: '序号',
    key: 'index',
    width: 70,
    align: 'center',
    render(_, index) {
      return (page.value - 1) * pageSize.value + index + 1
    }
  },
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
      return h(
        'span',
        { class: 'font-semibold text-google-text-primary dark:text-google-text-darkPrimary' },
        row.teamName || '-'
      )
    }
  },
  {
    title: '题目名称',
    key: 'challengeName',
    render(row) {
      return row.challengeName || '-'
    }
  },
  {
    title: '题目类型',
    key: 'challengeCategory',
    width: 120,
    align: 'center',
    render(row) {
      return row.challengeCategory || '-'
    }
  },
  {
    title: '提交内容',
    key: 'submittedFlag',
    render(row) {
      return h(
        'code',
        {
          class:
            'px-2 py-0.5 rounded text-xs font-mono bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light dark:border-google-border-dark text-google-text-primary dark:text-google-text-darkPrimary break-all select-all'
        },
        row.submittedFlag
      )
    }
  },
  {
    title: '提交时间',
    key: 'createdAt',
    width: 170,
    render(row) {
      return row.createdAt ? new Date(row.createdAt).toLocaleString() : '-'
    }
  },
  {
    title: '提交状态',
    key: 'status',
    width: 100,
    align: 'center',
    render(row) {
      const isCorrect = row.status === 'CORRECT'
      const isWrong = row.status === 'WRONG'
      const isDuplicate = row.status === 'DUPLICATE'
      const label = isCorrect ? '正确' : isWrong ? '错误' : isDuplicate ? '重复' : '无效'
      const clz = isCorrect
        ? 'text-green-600 dark:text-green-400 font-semibold'
        : isWrong
          ? 'text-red-600 dark:text-red-400 font-medium'
          : isDuplicate
            ? 'text-yellow-600 dark:text-yellow-400 font-medium'
            : 'text-google-text-secondary dark:text-google-text-darkSecondary font-medium'
      return h('span', { class: clz }, label)
    }
  }
])

async function fetchMountedChallenges() {
  if (!props.compId) return
  try {
    const res = await listMountedCtfChallengesApi(props.compId)
    if (res.data) {
      mountedChallenges.value = res.data
    }
  } catch {
    mountedChallenges.value = []
  }
}

async function fetchSubmissions() {
  if (!props.compId) return
  loadingSubmissions.value = true
  try {
    const res = await listAdminCtfSubmissionsApi(props.compId, {
      page: page.value,
      size: pageSize.value,
      challengeId: selectedChallengeId.value ? Number(selectedChallengeId.value) : undefined,
      keyword: searchKeyword.value.trim() || undefined
    })
    if (res.data) {
      submissions.value = res.data.list ?? []
      submissionTotal.value = Number(res.data.total) || 0
    }
  } catch {
    submissions.value = []
    submissionTotal.value = 0
  } finally {
    loadingSubmissions.value = false
  }
}

function handleSearch() {
  page.value = 1
  fetchSubmissions()
}

function handleReset() {
  searchKeyword.value = ''
  selectedChallengeId.value = null
  page.value = 1
  fetchSubmissions()
}

function handlePageChange(newPage: number) {
  page.value = newPage
  fetchSubmissions()
}

function handlePageSizeChange(newSize: number) {
  pageSize.value = newSize
  page.value = 1
  fetchSubmissions()
}

onMounted(() => {
  fetchMountedChallenges()
  fetchSubmissions()
})
</script>
