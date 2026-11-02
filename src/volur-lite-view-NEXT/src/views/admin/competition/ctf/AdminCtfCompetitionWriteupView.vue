<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <h3 class="text-sm font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
        {{ t('admin.competitions.writeups_list', '参赛队伍题解 (Writeup) 提交记录') }}
      </h3>
    </div>

    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
      <div class="w-full sm:w-80">
        <VSearch
          v-model:value="searchKeyword"
          placeholder="搜索战队名称、提交人或文件名..."
          clearable
          @search="handleSearch"
        />
      </div>
      <div class="flex items-center gap-2">
        <VBtn
          clz="normal"
          size="medium"
          :left-icon="SearchOutline"
          @click="handleSearch"
        >
          搜索
        </VBtn>
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

    <div v-if="writeupTotal > 0" class="flex justify-end pt-2">
      <VPagination
        :page="page"
        :page-size="pageSize"
        :item-count="writeupTotal"
        @update:page="handlePageChange"
        @update:page-size="handlePageSizeChange"
      />
    </div>

    <VModal
      v-model:show="showReviewModal"
      title="批阅队伍题解 (Writeup)"
      :width="520"
    >
      <div v-if="selectedWriteup" class="space-y-4 text-xs">
        <div class="p-3 rounded-lg bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light dark:border-google-border-dark space-y-1.5">
          <div class="flex items-center justify-between">
            <span class="text-google-text-secondary dark:text-google-text-darkSecondary">战队名称:</span>
            <span class="font-semibold text-google-text-primary dark:text-google-text-darkPrimary">{{ selectedWriteup.teamName }}</span>
          </div>
          <div class="flex items-center justify-between">
            <span class="text-google-text-secondary dark:text-google-text-darkSecondary">提交人:</span>
            <span class="text-google-text-primary dark:text-google-text-darkPrimary">{{ selectedWriteup.username }}</span>
          </div>
          <div class="flex items-center justify-between">
            <span class="text-google-text-secondary dark:text-google-text-darkSecondary">题解文件:</span>
            <a
              v-if="selectedWriteup.fileUrl"
              :href="selectedWriteup.fileUrl"
              target="_blank"
              class="text-google-blue dark:text-google-blue-dark hover:underline truncate max-w-[260px]"
            >
              {{ selectedWriteup.fileName }}
            </a>
            <span v-else class="text-google-text-secondary dark:text-google-text-darkSecondary">{{ selectedWriteup.fileName }}</span>
          </div>
        </div>

        <div class="space-y-1.5">
          <label class="block font-medium text-google-text-primary dark:text-google-text-darkPrimary">
            评阅状态
          </label>
          <VSelection
            v-model:value="reviewForm.status"
            :options="reviewStatusOptions"
            placeholder="请选择评阅状态"
          />
        </div>

        <div class="space-y-1.5">
          <label class="block font-medium text-google-text-primary dark:text-google-text-darkPrimary">
            评定得分
          </label>
          <VInput
            v-model:value="reviewForm.score"
            placeholder="请输入评定得分 (可选)"
          />
        </div>

        <div class="space-y-1.5">
          <label class="block font-medium text-google-text-primary dark:text-google-text-darkPrimary">
            评阅评语
          </label>
          <VInput
            v-model:value="reviewForm.comment"
            type="textarea"
            :rows="3"
            placeholder="请输入对该队伍题解的批阅评语或反馈建议 (可选)"
          />
        </div>
      </div>

      <template #footer>
        <div class="flex justify-end gap-3">
          <VBtn clz="normal" size="medium" @click="showReviewModal = false">
            取消
          </VBtn>
          <VBtn
            clz="primary"
            size="medium"
            :loading="submittingReview"
            @click="handleConfirmReview"
          >
            保存批阅
          </VBtn>
        </div>
      </template>
    </VModal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive, h } from 'vue'
import { useI18n } from 'vue-i18n'
import { useMessage, type DataTableColumns } from 'naive-ui'
import {
  RefreshOutline,
  SearchOutline,
  CreateOutline
} from '@vicons/ionicons5'
import VBtn from '@/components/common/VBtn.vue'
import VSearch from '@/components/common/VSearch.vue'
import VTable from '@/components/common/VTable.vue'
import VPagination from '@/components/common/VPagination.vue'
import VModal from '@/components/common/VModal.vue'
import VInput from '@/components/common/VInput.vue'
import VSelection from '@/components/common/VSelection.vue'
import type { CompetitionWriteupVO } from '@/types/competition'
import {
  listAdminCompetitionWriteupsApi,
  auditAdminCompetitionWriteupApi
} from '@/api/competition'

defineOptions({
  name: 'AdminCtfCompetitionWriteupView'
})

const props = defineProps<{
  compId: string | number
}>()

const { t } = useI18n()
const message = useMessage()

const writeups = ref<CompetitionWriteupVO[]>([])
const loadingWriteups = ref(false)
const searchKeyword = ref('')
const page = ref(1)
const pageSize = ref(10)
const writeupTotal = ref(0)

const showReviewModal = ref(false)
const submittingReview = ref(false)
const selectedWriteup = ref<CompetitionWriteupVO | null>(null)

const reviewForm = reactive({
  status: 'REVIEWED',
  score: '',
  comment: ''
})

const reviewStatusOptions = [
  { label: '已评阅 (REVIEWED)', value: 'REVIEWED' },
  { label: '待评阅 (SUBMITTED)', value: 'SUBMITTED' },
  { label: '已驳回 (REJECTED)', value: 'REJECTED' }
]

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
        ? h('a', { href: row.fileUrl, target: '_blank', class: 'text-xs text-google-blue dark:text-google-blue-dark hover:underline truncate block max-w-xs' }, row.fileName)
        : h('span', { class: 'text-xs text-google-text-primary dark:text-google-text-darkPrimary' }, row.fileName)
    }
  },
  {
    title: '评阅状态',
    key: 'status',
    width: 120,
    align: 'center',
    render(row) {
      const isReviewed = row.status === 'REVIEWED'
      const isRejected = row.status === 'REJECTED'
      const clz = isReviewed
        ? 'text-green-600 dark:text-green-400 font-medium'
        : isRejected
          ? 'text-red-600 dark:text-red-400 font-medium'
          : 'text-yellow-600 dark:text-yellow-400 font-medium'
      const label = isReviewed ? '已评阅' : isRejected ? '已驳回' : '待评阅'
      return h('span', { class: clz }, label)
    }
  },
  {
    title: '得分',
    key: 'score',
    width: 90,
    align: 'center',
    render(row) {
      return row.score !== null && row.score !== undefined ? `${row.score} 分` : '-'
    }
  },
  {
    title: '评阅评语',
    key: 'comment',
    width: 160,
    render(row) {
      return h('span', { class: 'text-xs text-google-text-secondary dark:text-google-text-darkSecondary truncate block max-w-[150px]' }, row.comment || '-')
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
    title: '操作',
    key: 'actions',
    width: 100,
    align: 'center',
    render(row) {
      return h(
        VBtn,
        {
          clz: 'primary',
          size: 'small',
          leftIcon: CreateOutline,
          onClick: () => openReviewModal(row)
        },
        { default: () => '批阅' }
      )
    }
  }
])

function openReviewModal(row: CompetitionWriteupVO) {
  selectedWriteup.value = row
  reviewForm.status = row.status || 'REVIEWED'
  reviewForm.score = row.score !== null && row.score !== undefined ? String(row.score) : ''
  reviewForm.comment = row.comment || ''
  showReviewModal.value = true
}

async function handleConfirmReview() {
  if (!props.compId || !selectedWriteup.value) return
  submittingReview.value = true
  try {
    const scoreVal = reviewForm.score !== '' ? Number(reviewForm.score) : undefined
    await auditAdminCompetitionWriteupApi(props.compId, selectedWriteup.value.id, {
      status: reviewForm.status,
      score: scoreVal,
      comment: reviewForm.comment.trim() || undefined
    })
    message.success('题解批阅保存成功')
    showReviewModal.value = false
    await fetchWriteups()
  } catch (err: any) {
    message.error(err?.message || '批阅失败')
  } finally {
    submittingReview.value = false
  }
}

async function fetchWriteups() {
  if (!props.compId) return
  loadingWriteups.value = true
  try {
    const res = await listAdminCompetitionWriteupsApi(props.compId, {
      page: page.value,
      size: pageSize.value,
      keyword: searchKeyword.value.trim() || undefined
    })
    if (res.data) {
      writeups.value = res.data.list ?? []
      writeupTotal.value = Number(res.data.total) || 0
    }
  } catch {
    writeups.value = []
    writeupTotal.value = 0
  } finally {
    loadingWriteups.value = false
  }
}

function handleSearch() {
  page.value = 1
  fetchWriteups()
}

function handlePageChange(newPage: number) {
  page.value = newPage
  fetchWriteups()
}

function handlePageSizeChange(newSize: number) {
  pageSize.value = newSize
  page.value = 1
  fetchWriteups()
}

onMounted(() => {
  fetchWriteups()
})
</script>
