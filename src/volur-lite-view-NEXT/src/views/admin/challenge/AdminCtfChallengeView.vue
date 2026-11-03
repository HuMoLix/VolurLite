<template>
  <div class="admin-ctf-challenge-view flex flex-col gap-4 sm:gap-6">
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h2 class="text-xl font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
          {{ t('admin.challenges.ctf_title', 'CTF 题库') }}
        </h2>
        <p class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary mt-1">
          {{ t('admin.challenges.ctf_desc', '管理全平台标准 CTF 竞赛赛题、解题 Flag、静态附件及动态靶机环境') }}
        </p>
      </div>
    </div>

    <div class="table-card p-3.5 sm:p-6 rounded-xl sm:rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark transition-colors">
      <div class="flex flex-col sm:flex-row sm:flex-wrap sm:items-center gap-2.5 sm:gap-3 mb-4 sm:mb-6">
        <VSearch
          v-model:value="searchName"
          :placeholder="t('admin.challenges.search_name_placeholder', '搜索题目名称...')"
          class="w-full sm:w-auto sm:flex-1 sm:min-w-[140px] sm:max-w-sm"
          @search="handleSearch"
          @clear="handleReset"
          @keydown.enter="handleSearch"
        />

        <div class="grid grid-cols-2 sm:grid-cols-3 gap-2.5 w-full sm:flex sm:w-auto shrink-0">
          <VSelection
            v-model:value="selectedCategoryId"
            :options="categorySelectOptions"
            :placeholder="t('admin.challenges.filter_category', '全部分类')"
            clearable
            class="w-full sm:w-36"
            @update:value="handleSearch"
          />

          <VSelection
            v-model:value="filterDifficulty"
            :options="difficultySelectOptions"
            :placeholder="t('admin.challenges.filter_difficulty', '全部难度')"
            clearable
            class="w-full sm:w-36"
            @update:value="handleSearch"
          />

          <VSelection
            v-model:value="filterType"
            :options="typeSelectOptions"
            :placeholder="t('admin.challenges.filter_type', '全部类型')"
            clearable
            class="w-full sm:w-36 col-span-2 sm:col-span-1"
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
            clz="normal"
            size="medium"
            :left-icon="FolderOutline"
            class="flex-1 sm:flex-none"
            @click="showCategoryModal = true"
          >
            {{ t('admin.challenges.categories_btn', '分类管理') }}
          </VBtn>
          <VBtn
            clz="normal"
            size="medium"
            :left-icon="CloudUploadOutline"
            class="flex-1 sm:flex-none"
            @click="showImportModal = true"
          >
            {{ t('admin.challenges.import_btn', '导入赛题') }}
          </VBtn>
          <VBtn
            clz="normal"
            size="medium"
            :left-icon="CloudDownloadOutline"
            :loading="exporting"
            class="flex-1 sm:flex-none"
            @click="handleExportChallenges"
          >
            {{ t('admin.challenges.export_btn', '导出赛题') }}
          </VBtn>
          <VBtn
            clz="primary"
            size="medium"
            :left-icon="AddOutline"
            class="w-full sm:w-auto"
            @click="router.push('/admin/challenges/ctf/create')"
          >
            {{ t('admin.challenges.create_btn', '新增赛题') }}
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
        :row-key="(row: CtfChallengeListVO) => row.id"
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

    <CtfCategoryModal
      v-model:show="showCategoryModal"
      @change="fetchCategories"
    />

    <CtfImportModal
      v-model:show="showImportModal"
      @success="fetchChallenges"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, h } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useMessage, useDialog, type DataTableColumns } from 'naive-ui'
import {
  SearchOutline,
  RefreshOutline,
  AddOutline,
  CloudUploadOutline,
  CloudDownloadOutline,
  FolderOutline
} from '@vicons/ionicons5'
import VBtn from '@/components/common/VBtn.vue'
import VTable from '@/components/common/VTable.vue'
import VSearch from '@/components/common/VSearch.vue'
import VSelection from '@/components/common/VSelection.vue'
import VPagination from '@/components/common/VPagination.vue'
import CtfCategoryModal from './components/CtfCategoryModal.vue'
import CtfImportModal from './components/CtfImportModal.vue'
import {
  getAdminCtfChallengeListApi,
  deleteAdminCtfChallengeApi,
  exportAdminCtfChallengesApi,
  getAdminCtfCategoriesApi
} from '@/api/challenge'
import type { CtfChallengeListVO, CtfCategoryVO, ChallengeDifficulty, ChallengeType } from '@/types/challenge'

defineOptions({
  name: 'AdminCtfChallengeView'
})

const router = useRouter()
const { t } = useI18n()
const message = useMessage()
const dialog = useDialog()

const loadingTable = ref(false)
const challengeList = ref<CtfChallengeListVO[]>([])
const checkedRowKeys = ref<(number | string)[]>([])

const selectedCategoryId = ref<number | string | null>(null)
const filterDifficulty = ref<string | null>(null)
const filterType = ref<string | null>(null)
const searchName = ref('')
const categoryList = ref<CtfCategoryVO[]>([])

const showCategoryModal = ref(false)
const showImportModal = ref(false)
const exporting = ref(false)

const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

const categorySelectOptions = computed(() => {
  return [
    { label: t('admin.challenges.filter_category', '全部分类'), value: '' },
    ...categoryList.value.map(cat => ({
      label: cat.name,
      value: String(cat.id)
    }))
  ]
})

const difficultySelectOptions = computed(() => [
  { label: t('admin.challenges.filter_difficulty', '全部难度'), value: '' },
  { label: t('admin.challenges.diff_easy', '入门'), value: 'easy' },
  { label: t('admin.challenges.diff_normal', '简单'), value: 'normal' },
  { label: t('admin.challenges.diff_mid', '中等'), value: 'mid' },
  { label: t('admin.challenges.diff_hard', '进阶'), value: 'hard' },
  { label: t('admin.challenges.diff_difficult', '困难'), value: 'difficult' }
])

const typeSelectOptions = computed(() => [
  { label: t('admin.challenges.filter_type', '全部类型'), value: '' },
  { label: t('admin.challenges.type_static_attachment', '静态附件'), value: 'STATIC' },
  { label: t('admin.challenges.type_dynamic_container', '动态容器'), value: 'CONTAINER' }
])

function getDifficultyText(diffVal?: ChallengeDifficulty | number | string): string {
  const s = String(diffVal || '').toLowerCase().trim()
  switch (s) {
    case '1':
    case 'easy':
      return t('admin.challenges.diff_easy', '入门')
    case '2':
    case 'normal':
      return t('admin.challenges.diff_normal', '简单')
    case '3':
    case 'mid':
      return t('admin.challenges.diff_mid', '中等')
    case '4':
    case 'hard':
      return t('admin.challenges.diff_hard', '进阶')
    case '5':
    case 'difficult':
      return t('admin.challenges.diff_difficult', '困难')
    default:
      return s || t('admin.challenges.diff_easy', '入门')
  }
}

function getTypeText(type?: ChallengeType | string): string {
  const s = String(type || '').toUpperCase().trim()
  if (s === 'CONTAINER' || s === 'DYNAMIC') {
    return t('admin.challenges.type_dynamic_container', '动态容器')
  }
  return t('admin.challenges.type_static_attachment', '静态附件')
}

function matchDifficulty(diffVal: any, filterVal: string): boolean {
  if (!filterVal) return true
  const s = String(diffVal || '').toLowerCase().trim()
  switch (filterVal) {
    case 'easy':
      return s === 'easy' || s === '1'
    case 'normal':
      return s === 'normal' || s === '2'
    case 'mid':
      return s === 'mid' || s === '3'
    case 'hard':
      return s === 'hard' || s === '4'
    case 'difficult':
      return s === 'difficult' || s === '5'
    default:
      return s === filterVal
  }
}

function matchType(typeVal: any, filterVal: string): boolean {
  if (!filterVal) return true
  const s = String(typeVal || 'STATIC').toUpperCase().trim()
  if (filterVal === 'CONTAINER') {
    return s === 'CONTAINER' || s === 'DYNAMIC'
  }
  if (filterVal === 'STATIC') {
    return s === 'STATIC' || !s
  }
  return s === filterVal
}

const tableColumns = computed<DataTableColumns<CtfChallengeListVO>>(() => [
  {
    type: 'selection',
    width: 48
  },
  {
    title: t('admin.challenges.challenge_name', '赛题名称'),
    key: 'name',
    minWidth: 160,
    render(row) {
      return h('div', { class: 'space-y-0.5' }, [
        h(
          'div',
          {
            class: 'redirectable',
            style: { fontWeight: '500' },
            onClick: () => router.push(`/admin/challenges/ctf/${row.id}/edit`)
          },
          row.name
        ),
        row.imageName
          ? h(
              'div',
              {
                class: 'text-[12px] truncate max-w-xs',
                style: {
                  color: 'var(--color-v3-text-var)',
                  fontSize: '12px'
                }
              },
              row.imageName
            )
          : null
      ])
    }
  },
  {
    title: t('admin.challenges.category', '方向分类'),
    key: 'categoryName',
    width: 120,
    render(row) {
      return row.categoryName || '-'
    }
  },
  {
    title: t('admin.challenges.type', '类型'),
    key: 'type',
    width: 110,
    render(row) {
      return getTypeText(row.type)
    }
  },
  {
    title: t('admin.challenges.difficulty', '难度'),
    key: 'difficulty',
    width: 90,
    render(row) {
      return getDifficultyText(row.difficulty)
    }
  },
  {
    title: t('admin.challenges.author', '命题人'),
    key: 'authorName',
    width: 120,
    render(row) {
      return row.authorName || '-'
    }
  },
  {
    title: t('admin.challenges.created_at', '创建时间'),
    key: 'createdAt',
    width: 170,
    render(row) {
      return row.createdAt ? new Date(row.createdAt).toLocaleString() : '-'
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
            onClick: () => router.push(`/admin/challenges/ctf/${row.id}/edit`)
          },
          { default: () => t('common.edit', '编辑') }
        ),
        h(
          VBtn,
          {
            clz: 'danger',
            size: 'small',
            onClick: () => handleDeleteChallenge(row)
          },
          { default: () => t('common.delete', '删除') }
        )
      ])
    }
  }
])

async function fetchCategories() {
  try {
    const res = await getAdminCtfCategoriesApi()
    if (res.data) {
      categoryList.value = res.data
    }
  } catch {
    categoryList.value = []
  }
}

async function fetchChallenges() {
  loadingTable.value = true
  try {
    const res = await getAdminCtfChallengeListApi({
      page: pagination.page,
      size: pagination.pageSize,
      categoryId: selectedCategoryId.value || undefined,
      keyword: searchName.value.trim() || undefined
    })
    if (res.data) {
      let list = res.data.list ?? []
      if (filterDifficulty.value) {
        list = list.filter(item => matchDifficulty(item.difficulty, filterDifficulty.value!))
      }
      if (filterType.value) {
        list = list.filter(item => matchType(item.type, filterType.value!))
      }
      challengeList.value = list
      pagination.itemCount = Number(res.data.total) || 0
    }
  } catch {
  } finally {
    loadingTable.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchChallenges()
}

function handleReset() {
  selectedCategoryId.value = null
  filterDifficulty.value = null
  filterType.value = null
  searchName.value = ''
  pagination.page = 1
  fetchChallenges()
}

function handlePageChange(page: number) {
  pagination.page = page
  fetchChallenges()
}

function handlePageSizeChange(size: number) {
  pagination.pageSize = size
  pagination.page = 1
  fetchChallenges()
}

function handleCheckedRowKeysChange(keys: (number | string)[]) {
  checkedRowKeys.value = keys
}

async function handleExportChallenges() {
  exporting.value = true
  try {
    const ids = checkedRowKeys.value.length > 0 ? checkedRowKeys.value : undefined
    const blob = await exportAdminCtfChallengesApi(ids)
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `ctf_challenges_export_${Date.now()}.zip`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success(t('admin.challenges.export_success', '导出成功'))
  } catch {
  } finally {
    exporting.value = false
  }
}

function handleDeleteChallenge(row: CtfChallengeListVO) {
  dialog.warning({
    title: t('admin.challenges.delete_confirm_title', '确认删除赛题'),
    content: t('admin.challenges.delete_confirm_content', '删除后将彻底移除该赛题及其附件关联，是否继续？'),
    positiveText: t('common.confirm', '确认删除'),
    negativeText: t('common.cancel', '取消'),
    onPositiveClick: async () => {
      try {
        await deleteAdminCtfChallengeApi(row.id)
        message.success(t('admin.challenges.deleted_success', '赛题已删除'))
        await fetchChallenges()
      } catch {
      }
    }
  })
}

onMounted(() => {
  fetchCategories()
  fetchChallenges()
})
</script>
