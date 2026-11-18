<template>
  <div class="admin-training-view flex flex-col gap-4 sm:gap-6">
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h2 class="text-xl font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
          {{ t('admin.training.title', '训练管理') }}
        </h2>
        <p class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary mt-1">
          {{ t('admin.training.desc', '管理训练池中的题目：从 CTF 题库导入、调整分值或上下架、移出训练池') }}
        </p>
      </div>
    </div>

    <div class="table-card p-3.5 sm:p-6 rounded-xl sm:rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark transition-colors">
      <div class="flex flex-col sm:flex-row sm:flex-wrap sm:items-center gap-2.5 sm:gap-3 mb-4 sm:mb-6">
        <VSearch
          v-model:value="searchKeyword"
          :placeholder="t('admin.training.search_placeholder', '搜索题目名称...')"
          class="w-full sm:w-auto sm:flex-1 sm:min-w-[140px] sm:max-w-sm"
          @search="handleSearch"
          @clear="handleReset"
        />

        <VSelection
          v-model:value="filterStatus"
          :options="statusSelectOptions"
          :placeholder="t('admin.training.filter_status', '全部状态')"
          clearable
          class="w-full sm:w-36"
          @update:value="handleSearch"
        />

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
            v-if="checkedRowKeys.length > 0"
            clz="danger"
            size="medium"
            :left-icon="TrashOutline"
            class="flex-1 sm:flex-none"
            @click="handleBatchRemove"
          >
            {{ t('admin.training.batch_remove', '批量移除') }} ({{ checkedRowKeys.length }})
          </VBtn>
          <VBtn
            clz="primary"
            size="medium"
            :left-icon="CloudUploadOutline"
            class="w-full sm:w-auto"
            @click="showImportModal = true"
          >
            {{ t('admin.training.import_btn', '导入训练题') }}
          </VBtn>
        </div>
      </div>

      <VTable
        :columns="tableColumns"
        :data="challengeList"
        :loading="loadingTable"
        :pagination="false"
        :bordered="false"
        :checked-row-keys="checkedRowKeys"
        :row-key="(row: TrainingChallengeVO) => row.id"
        :scroll-x="980"
        @update:checked-row-keys="handleCheckedRowKeysChange"
      >
        <template #empty>
          <div class="py-10 text-center text-xs text-google-text-muted dark:text-google-text-darkMuted">
            {{ t('admin.training.no_data', '训练池暂无题目，点击右上角「导入训练题」从题库添加') }}
          </div>
        </template>
      </VTable>

      <div
        v-if="pagination.itemCount > 0"
        class="flex flex-col sm:flex-row items-center justify-between gap-3 pt-4 border-t border-google-border-light/70 dark:border-google-border-dark/70"
      >
        <div class="text-xs text-google-text-muted dark:text-google-text-darkMuted text-center sm:text-left">
          {{ t('admin.training.showing', '显示第') }}
          {{ (pagination.page - 1) * pagination.pageSize + 1 }} - {{ Math.min(pagination.page * pagination.pageSize, pagination.itemCount) }}
          {{ t('admin.training.records', '条，共') }} {{ pagination.itemCount }} {{ t('admin.training.records_unit', '条') }}
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

    <TrainingImportModal
      v-model:show="showImportModal"
      @success="handleImportSuccess"
    />

    <n-modal
      :show="showEditModal"
      preset="card"
      closable
      class="max-w-md w-full rounded-2xl"
      @update:show="showEditModal = $event"
      @close="showEditModal = false"
    >
      <template #header>
        <div class="flex items-center gap-2 min-w-0 pr-2">
          <h3 class="text-base font-bold text-google-text-primary dark:text-google-text-darkPrimary truncate">
            {{ t('admin.training.edit_title', '编辑训练题') }}
          </h3>
        </div>
      </template>

      <div class="space-y-4">
        <div class="text-sm font-medium text-google-text-primary dark:text-google-text-darkPrimary break-all">
          {{ editingChallenge?.name || '-' }}
        </div>

        <div class="space-y-1.5">
          <label class="text-xs font-semibold text-google-text-muted dark:text-google-text-darkMuted">
            {{ t('admin.training.edit_points', '积分') }}
          </label>
          <n-input-number
            v-model:value="editPoints"
            :min="0"
            :precision="0"
            class="w-full"
          />
        </div>

        <div class="space-y-1.5">
          <label class="text-xs font-semibold text-google-text-muted dark:text-google-text-darkMuted">
            {{ t('admin.training.edit_status', '状态') }}
          </label>
          <VSelection
            v-model:value="editStatus"
            :options="statusOptions"
            class="w-full"
          />
        </div>
      </div>

      <template #footer>
        <div class="flex items-center justify-end gap-2">
          <VBtn clz="normal" size="medium" @click="showEditModal = false">
            {{ t('common.cancel', '取消') }}
          </VBtn>
          <VBtn clz="primary" size="medium" :loading="savingEdit" @click="handleSaveEdit">
            {{ t('common.save', '保存') }}
          </VBtn>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { NInputNumber, NModal, useDialog, useMessage, type DataTableColumns } from 'naive-ui'
import {
  CloudUploadOutline,
  RefreshOutline,
  SearchOutline,
  TrashOutline
} from '@vicons/ionicons5'
import VBtn from '@/components/common/VBtn.vue'
import VTable from '@/components/common/VTable.vue'
import VSearch from '@/components/common/VSearch.vue'
import VSelection from '@/components/common/VSelection.vue'
import VPagination from '@/components/common/VPagination.vue'
import {
  deleteAdminTrainingChallengeApi,
  getAdminTrainingChallengeListApi,
  updateAdminTrainingChallengeApi
} from '@/api/training'
import type {
  TrainingChallengeStatus,
  TrainingChallengeVO,
  TrainingImportResultVO
} from '@/types/training'
import TrainingImportModal from './components/TrainingImportModal.vue'

defineOptions({
  name: 'AdminTrainingView'
})

const { t } = useI18n()
const message = useMessage()
const dialog = useDialog()

const loadingTable = ref(false)
const challengeList = ref<TrainingChallengeVO[]>([])
const checkedRowKeys = ref<(number | string)[]>([])

const searchKeyword = ref('')
const filterStatus = ref<string | null>(null)

const showImportModal = ref(false)

const showEditModal = ref(false)
const editingChallenge = ref<TrainingChallengeVO | null>(null)
const editPoints = ref<number | null>(0)
const editStatus = ref<TrainingChallengeStatus>('ENABLED')
const savingEdit = ref(false)

const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  pageSizes: [10, 20, 50]
})

const statusOptions = computed(() => [
  { label: t('admin.training.status_enabled', '启用'), value: 'ENABLED' },
  { label: t('admin.training.status_disabled', '停用'), value: 'DISABLED' }
])

const statusSelectOptions = computed(() => [
  { label: t('admin.training.filter_status', '全部状态'), value: '' },
  ...statusOptions.value
])

function getDifficultyText(difficulty?: string | null): string {
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

function getDifficultyClass(difficulty?: string | null): string {
  const value = String(difficulty || '').toLowerCase().trim()
  if (value === 'easy' || value === '1') return 'is-easy'
  if (value === 'normal' || value === '2') return 'is-normal'
  if (value === 'mid' || value === '3') return 'is-mid'
  if (value === 'hard' || value === '4') return 'is-hard'
  if (value === 'difficult' || value === '5') return 'is-difficult'
  return 'is-unknown'
}

const tableColumns = computed<DataTableColumns<TrainingChallengeVO>>(() => [
  {
    type: 'selection',
    width: 48
  },
  {
    title: t('admin.training.challenge_name', '训练题'),
    key: 'name',
    minWidth: 220,
    render(row) {
      return h('div', { class: 'space-y-0.5 min-w-0' }, [
        h('div', { class: 'font-medium truncate' }, row.name || '-'),
        h(
          'div',
          { class: 'text-[12px] text-google-text-muted dark:text-google-text-darkMuted truncate' },
          `${t('admin.training.source_id', '原题 ID')}: ${row.sourceChallengeId}`
        )
      ])
    }
  },
  {
    title: t('admin.training.category', '所属方向'),
    key: 'categoryName',
    width: 120,
    render(row) {
      return row.categoryName || '-'
    }
  },
  {
    title: t('admin.training.difficulty', '难度'),
    key: 'difficulty',
    width: 90,
    render(row) {
      return h(
        'span',
        { class: ['difficulty-text', getDifficultyClass(row.difficulty)] },
        getDifficultyText(row.difficulty)
      )
    }
  },
  {
    title: t('admin.training.points', '积分'),
    key: 'points',
    width: 80,
    render(row) {
      return h('span', { class: 'font-mono font-semibold' }, String(row.points ?? 0))
    }
  },
  {
    title: t('admin.training.solved_count', '解出人数'),
    key: 'solvedCount',
    width: 100,
    render(row) {
      return h('span', { class: 'font-mono' }, String(row.solvedCount ?? 0))
    }
  },
  {
    title: t('admin.training.status', '状态'),
    key: 'status',
    width: 90,
    render(row) {
      const enabled = row.status !== 'DISABLED'
      return h(
        'span',
        { class: ['status-badge', enabled ? 'is-enabled' : 'is-disabled'] },
        enabled
          ? t('admin.training.status_enabled', '启用')
          : t('admin.training.status_disabled', '停用')
      )
    }
  },
  {
    title: t('common.actions', '操作'),
    key: 'actions',
    width: 140,
    align: 'center',
    render(row) {
      return h('div', { class: 'flex items-center justify-center gap-2' }, [
        h(
          VBtn,
          {
            clz: 'normal',
            size: 'small',
            onClick: () => openEditModal(row)
          },
          { default: () => t('common.edit', '编辑') }
        ),
        h(
          VBtn,
          {
            clz: 'danger',
            size: 'small',
            onClick: () => handleRemove(row)
          },
          { default: () => t('admin.training.remove', '移除') }
        )
      ])
    }
  }
])

async function fetchChallenges() {
  loadingTable.value = true
  try {
    const res = await getAdminTrainingChallengeListApi({
      page: pagination.page,
      size: pagination.pageSize,
      keyword: searchKeyword.value.trim() || undefined,
      status: (filterStatus.value || undefined) as TrainingChallengeStatus | undefined
    })
    if (res.data) {
      challengeList.value = res.data.list || []
      pagination.itemCount = Number(res.data.total) || 0
    }
  } catch {
  } finally {
    loadingTable.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  checkedRowKeys.value = []
  fetchChallenges()
}

function handleReset() {
  searchKeyword.value = ''
  filterStatus.value = null
  pagination.page = 1
  checkedRowKeys.value = []
  fetchChallenges()
}

function handlePageChange(page: number) {
  pagination.page = page
  checkedRowKeys.value = []
  fetchChallenges()
}

function handlePageSizeChange(size: number) {
  pagination.pageSize = size
  pagination.page = 1
  checkedRowKeys.value = []
  fetchChallenges()
}

function handleCheckedRowKeysChange(keys: (number | string)[]) {
  checkedRowKeys.value = keys
}

function openEditModal(row: TrainingChallengeVO) {
  editingChallenge.value = row
  editPoints.value = Number(row.points) || 0
  editStatus.value = row.status === 'DISABLED' ? 'DISABLED' : 'ENABLED'
  showEditModal.value = true
}

async function handleSaveEdit() {
  if (!editingChallenge.value || savingEdit.value) return
  const points = Number(editPoints.value)
  if (!Number.isFinite(points) || points < 0) {
    message.warning(t('admin.training.points_invalid', '请输入不小于 0 的积分'))
    return
  }

  savingEdit.value = true
  try {
    await updateAdminTrainingChallengeApi(editingChallenge.value.id, {
      points,
      status: editStatus.value
    })
    message.success(t('admin.training.update_success', '训练题已更新'))
    showEditModal.value = false
    await fetchChallenges()
  } catch {
  } finally {
    savingEdit.value = false
  }
}

function handleRemove(row: TrainingChallengeVO) {
  dialog.warning({
    title: t('admin.training.remove_confirm_title', '移出训练池'),
    content: t('admin.training.remove_confirm_content', {
      name: row.name || String(row.sourceChallengeId)
    }),
    positiveText: t('common.confirm', '确认'),
    negativeText: t('common.cancel', '取消'),
    onPositiveClick: async () => {
      try {
        await deleteAdminTrainingChallengeApi(row.id)
        message.success(t('admin.training.remove_success', '已移出训练池'))
        await fetchChallenges()
      } catch {
      }
    }
  })
}

function handleBatchRemove() {
  const ids = [...checkedRowKeys.value]
  if (ids.length === 0) return
  dialog.warning({
    title: t('admin.training.batch_remove_confirm_title', '批量移出训练池'),
    content: t('admin.training.batch_remove_confirm_content', { count: ids.length }),
    positiveText: t('common.confirm', '确认'),
    negativeText: t('common.cancel', '取消'),
    onPositiveClick: async () => {
      const results = await Promise.allSettled(
        ids.map((id) => deleteAdminTrainingChallengeApi(id))
      )
      const successCount = results.filter((item) => item.status === 'fulfilled').length
      if (successCount > 0) {
        message.success(
          t('admin.training.batch_remove_success', {
            count: successCount
          })
        )
      }
      checkedRowKeys.value = []
      await fetchChallenges()
    }
  })
}

function handleImportSuccess(_result: TrainingImportResultVO) {
  checkedRowKeys.value = []
  pagination.page = 1
  fetchChallenges()
}

onMounted(() => {
  fetchChallenges()
})
</script>

<style scoped lang="scss">
.difficulty-text {
  font-size: 12px;
  font-weight: 600;

  &.is-easy {
    color: #1e8e3e;
  }

  &.is-normal {
    color: #1a73e8;
  }

  &.is-mid {
    color: #b06000;
  }

  &.is-hard {
    color: #c5520a;
  }

  &.is-difficult {
    color: #c5221f;
  }

  &.is-unknown {
    color: #5f6368;
  }
}

html.dark .difficulty-text {
  &.is-easy {
    color: #81c995;
  }

  &.is-normal {
    color: #a8c7fa;
  }

  &.is-mid {
    color: #fdd663;
  }

  &.is-hard {
    color: #fcad70;
  }

  &.is-difficult {
    color: #f28b82;
  }

  &.is-unknown {
    color: #9aa0a6;
  }
}

.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 1px 8px;
  border-radius: 9999px;
  font-size: 11px;
  font-weight: 600;
  line-height: 18px;
  border: 1px solid transparent;

  &.is-enabled {
    color: #1e8e3e;
    background: rgba(52, 168, 83, 0.1);
    border-color: rgba(52, 168, 83, 0.24);
  }

  &.is-disabled {
    color: #5f6368;
    background: rgba(95, 99, 104, 0.08);
    border-color: rgba(95, 99, 104, 0.2);
  }
}

html.dark .status-badge {
  &.is-enabled {
    color: #81c995;
    background: rgba(129, 201, 149, 0.12);
    border-color: rgba(129, 201, 149, 0.28);
  }

  &.is-disabled {
    color: #9aa0a6;
    background: rgba(154, 160, 166, 0.12);
    border-color: rgba(154, 160, 166, 0.26);
  }
}
</style>
