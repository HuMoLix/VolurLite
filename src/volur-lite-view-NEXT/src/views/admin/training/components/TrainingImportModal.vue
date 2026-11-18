<template>
  <n-modal
    :show="show"
    preset="card"
    closable
    :style="{ width: '880px', maxWidth: 'calc(100vw - 32px)' }"
    class="training-import-modal rounded-2xl"
    @update:show="handleClose"
    @close="handleClose(false)"
  >
    <template #header>
      <div class="flex items-center gap-2 min-w-0 pr-2">
        <h3 class="text-base font-bold text-google-text-primary dark:text-google-text-darkPrimary truncate">
          {{ t('admin.training.import_title', '导入训练题') }}
        </h3>
      </div>
    </template>

    <div class="space-y-4">
      <p class="text-xs leading-relaxed text-google-text-muted dark:text-google-text-darkMuted">
        {{ t('admin.training.import_desc', '从 CTF 题库中选择题目加入训练池，已在池中的题目会自动跳过。') }}
      </p>

      <div class="flex flex-col sm:flex-row sm:items-center gap-2.5">
        <VSearch
          v-model:value="keyword"
          :placeholder="t('admin.training.import_search', '搜索题库题目...')"
          class="flex-1"
          @search="handleSearch"
          @clear="handleSearch"
        />
        <span class="text-xs text-google-text-muted dark:text-google-text-darkMuted shrink-0">
          {{ t('admin.training.import_selected', { count: selectedRowKeys.length }) }}
        </span>
      </div>

      <VTable
        :columns="columns"
        :data="challenges"
        :loading="loading"
        :pagination="false"
        :bordered="false"
        :checked-row-keys="selectedRowKeys"
        :row-key="(row: CtfChallengeListVO) => row.id"
        :scroll-x="760"
        :max-height="352"
        size="small"
        @update:checked-row-keys="handleCheckedRowKeysChange"
      >
        <template #empty>
          <div class="py-8 text-center text-xs text-google-text-muted dark:text-google-text-darkMuted">
            {{ t('admin.training.import_empty', '没有找到匹配的题库题目') }}
          </div>
        </template>
      </VTable>

      <div v-if="total > PAGE_SIZE" class="flex items-center justify-end">
        <VPagination
          :page="page"
          :page-size="PAGE_SIZE"
          :item-count="total"
          size="small"
          @update:page="handlePageChange"
        />
      </div>
    </div>

    <template #footer>
      <div class="flex items-center justify-between gap-2">
        <span class="text-[11px] text-google-text-muted dark:text-google-text-darkMuted">
          {{ t('admin.training.import_hint', '导入后可在训练管理中调整积分与上下架状态') }}
        </span>
        <div class="flex items-center gap-2 shrink-0">
          <VBtn clz="normal" size="medium" @click="handleClose(false)">
            {{ t('common.cancel', '取消') }}
          </VBtn>
          <VBtn
            clz="primary"
            size="medium"
            :loading="submitting"
            :disabled="selectedRowKeys.length === 0"
            @click="handleSubmit"
          >
            {{ t('admin.training.import_submit', '导入所选题目') }}
          </VBtn>
        </div>
      </div>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
import { computed, h, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { NModal, useMessage, type DataTableColumns } from 'naive-ui'
import VBtn from '@/components/common/VBtn.vue'
import VTable from '@/components/common/VTable.vue'
import VSearch from '@/components/common/VSearch.vue'
import VPagination from '@/components/common/VPagination.vue'
import { getAdminCtfChallengeListApi } from '@/api/challenge'
import { importAdminTrainingChallengesApi } from '@/api/training'
import type { CtfChallengeListVO } from '@/types/challenge'
import type { TrainingImportResultVO } from '@/types/training'

defineOptions({
  name: 'TrainingImportModal'
})

const props = defineProps<{
  show: boolean
}>()

const emit = defineEmits<{
  (e: 'update:show', value: boolean): void
  (e: 'success', payload: TrainingImportResultVO): void
}>()

const PAGE_SIZE = 8

const { t } = useI18n()
const message = useMessage()

const loading = ref(false)
const submitting = ref(false)
const keyword = ref('')
const page = ref(1)
const total = ref(0)
const challenges = ref<CtfChallengeListVO[]>([])
const selectedRowKeys = ref<(number | string)[]>([])

function getDifficultyText(difficulty?: CtfChallengeListVO['difficulty']): string {
  const value = String(difficulty || '').toLowerCase().trim()
  switch (value) {
    case 'easy':
    case '1':
      return t('admin.challenges.diff_easy', '入门')
    case 'normal':
    case '2':
      return t('admin.challenges.diff_normal', '简单')
    case 'mid':
    case '3':
      return t('admin.challenges.diff_mid', '中等')
    case 'hard':
    case '4':
      return t('admin.challenges.diff_hard', '进阶')
    case 'difficult':
    case '5':
      return t('admin.challenges.diff_difficult', '困难')
    default:
      return t('admin.training.diff_unknown', '未分级')
  }
}

const columns = computed<DataTableColumns<CtfChallengeListVO>>(() => [
  {
    type: 'selection',
    width: 44
  },
  {
    title: t('admin.training.challenge_name', '训练题'),
    key: 'name',
    minWidth: 220,
    render(row) {
      return h('div', { class: 'font-medium truncate' }, row.name || '-')
    }
  },
  {
    title: t('admin.training.category', '所属方向'),
    key: 'categoryName',
    width: 110,
    render(row) {
      return row.categoryName || '-'
    }
  },
  {
    title: t('admin.training.difficulty', '难度'),
    key: 'difficulty',
    width: 90,
    render(row) {
      return getDifficultyText(row.difficulty)
    }
  },
  {
    title: t('admin.challenges.created_at', '创建时间'),
    key: 'createdAt',
    width: 170,
    render(row) {
      return row.createdAt ? new Date(row.createdAt).toLocaleString() : '-'
    }
  }
])

async function fetchChallenges() {
  loading.value = true
  try {
    const res = await getAdminCtfChallengeListApi({
      page: page.value,
      size: PAGE_SIZE,
      keyword: keyword.value.trim() || undefined
    })
    if (res.data) {
      challenges.value = res.data.list || []
      total.value = Number(res.data.total) || 0
    }
  } catch {
    challenges.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  fetchChallenges()
}

function handlePageChange(nextPage: number) {
  page.value = nextPage
  fetchChallenges()
}

function handleCheckedRowKeysChange(keys: (number | string)[]) {
  selectedRowKeys.value = keys
}

async function handleSubmit() {
  if (selectedRowKeys.value.length === 0 || submitting.value) return
  submitting.value = true
  try {
    const res = await importAdminTrainingChallengesApi(selectedRowKeys.value)
    const result = res.data
    if (result) {
      const skipped = result.skipped?.length || 0
      message.success(
        t('admin.training.import_success', {
          imported: result.imported,
          skipped
        })
      )
      if (skipped > 0) {
        const already = result.skipped.filter((item) => item.reason === 'ALREADY_IN_POOL').length
        const notFound = result.skipped.filter((item) => item.reason === 'NOT_FOUND').length
        message.info(
          t('admin.training.import_skipped_reason', {
            already,
            notFound
          })
        )
      }
      emit('success', result)
      handleClose(false)
    }
  } catch {
  } finally {
    submitting.value = false
  }
}

function handleClose(value: boolean) {
  emit('update:show', value)
}

watch(
  () => props.show,
  (visible) => {
    if (!visible) return
    keyword.value = ''
    page.value = 1
    selectedRowKeys.value = []
    fetchChallenges()
  }
)
</script>

<style scoped lang="scss">
:global(.training-import-modal .n-card__content) {
  padding-top: 8px;
}
</style>
