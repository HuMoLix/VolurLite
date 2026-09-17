<template>
  <div class="admin-ctf-challenge-view">
    <!-- Header Section -->
    <div class="page-header">
      <h2 class="page-title">{{ t('admin.challenges.ctf_title') }}</h2>
      <p class="page-desc">{{ t('admin.challenges.ctf_desc') }}</p>
    </div>

    <!-- Main Table Card -->
    <AppTableCard
      :bordered="true"
      :table-bordered="true"
      :scroll-x="800"
      :columns="tableColumns"
      :data="challengeList"
      :loading="loadingTable"
      table-class="ctf-challenges-table"
      standalone-pagination
      :page="currentPage"
      :page-size="pageSize"
      :item-count="totalCount"
      :checked-row-keys="checkedRowKeys"
      @update:page="handlePageChange"
      @update:page-size="handlePageSizeChange"
      @update:checked-row-keys="handleCheckedRowKeysChange"
    >
      <template #filter-left>
        <div class="ctf-filter-left">
          <n-select
            v-model:value="selectedCategoryId"
            :options="categorySelectOptions"
            :render-label="renderCategoryOptionLabel"
            placeholder="全部分类"
            clearable
            class="filter-select-category"
            @update:value="handleSearch"
          />

          <n-input
            v-model:value="searchName"
            placeholder="搜索题目名称..."
            clearable
            class="filter-input-search"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <n-icon :component="SearchOutline" />
            </template>
          </n-input>

          <div class="filter-action-buttons">
            <n-button type="primary" class="theme-primary-btn" @click="handleSearch">
              <template #icon>
                <n-icon :component="SearchOutline" />
              </template>
              搜索
            </n-button>

            <n-button secondary @click="handleResetFilters">
              <template #icon>
                <n-icon :component="RefreshOutline" />
              </template>
              重置
            </n-button>
          </div>
        </div>
      </template>

      <template #filter-right>
        <div class="ctf-filter-right">
          <n-button type="primary" class="theme-primary-btn action-create-btn" @click="handleCreateChallenge">
            <template #icon>
              <n-icon :component="AddOutline" />
            </template>
            新增题目
          </n-button>

          <n-dropdown
            :options="moreActionsOptions"
            trigger="click"
            placement="bottom-end"
            @select="handleMoreActionSelect"
          >
            <n-button secondary class="action-more-btn" :loading="exporting">
              <template #icon>
                <n-icon :component="EllipsisHorizontalOutline" />
              </template>
              <span>更多操作</span>
              <n-icon :component="ChevronDownOutline" class="dropdown-chevron-icon" />
            </n-button>
          </n-dropdown>
        </div>
      </template>
    </AppTableCard>

    <!-- Import Challenge Modal Component -->
    <CtfImportModal
      v-model:show="showImportModal"
      @success="handleImportSuccess"
    />

    <!-- Category Management Modal Component -->
    <CtfCategoryModal
      v-model:show="showCategoryModal"
      @change="fetchCategories"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, h } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  NButton,
  NIcon,
  NInput,
  NSelect,
  NTag,
  NPopconfirm,
  NDropdown,
  useMessage,
  type DataTableColumns,
  type DropdownOption
} from 'naive-ui'
import AppTableCard from '@/components/common/AppTableCard.vue'
import CtfCategoryModal from './components/CtfCategoryModal.vue'
import CtfImportModal from './components/CtfImportModal.vue'
import {
  SearchOutline,
  RefreshOutline,
  AddOutline,
  CloudUploadOutline,
  CloudDownloadOutline,
  FolderOutline,
  EllipsisHorizontalOutline,
  ChevronDownOutline
} from '@vicons/ionicons5'
import {
  getAdminCtfChallengeListApi,
  deleteAdminCtfChallengeApi,
  exportAdminCtfChallengesApi,
  getAdminCtfCategoriesApi
} from '@/api/challenge'
import type {
  CtfChallengeListVO,
  CtfCategoryVO,
  ChallengeDifficulty
} from '@/types/challenge'
import { VolurIcon } from '@/public/volurEx'

const router = useRouter()
const { t } = useI18n()
const message = useMessage()

// State
const loadingTable = ref(false)
const challengeList = ref<CtfChallengeListVO[]>([])
const totalCount = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const checkedRowKeys = ref<(number | string)[]>([])

const selectedCategoryId = ref<number | string | null>(null)
const searchName = ref('')
const categoryList = ref<CtfCategoryVO[]>([])

// Export State
const exporting = ref(false)

/**
 * 难度等级映射 (对齐后端 CtfChallenge 与 challenge.json)
 * 1 -> 入门 (easy)
 * 2 -> 简单 (normal)
 * 3 -> 中等 (mid)
 * 4 -> 进阶 (hard)
 * 5 -> 困难 (difficult)
 */
function getDifficultyBadge(diffVal?: ChallengeDifficulty | number | string) {
  if (!diffVal) return { label: '入门', type: 'info' as const }
  const s = String(diffVal).toLowerCase().trim()
  switch (s) {
    case '1':
    case 'easy':
    case '入门':
      return { label: '入门', type: 'info' as const }
    case '2':
    case 'normal':
    case '简单':
      return { label: '简单', type: 'success' as const }
    case '3':
    case 'mid':
    case 'medium':
    case '中等':
      return { label: '中等', type: 'warning' as const }
    case '4':
    case 'hard':
    case '进阶':
      return { label: '进阶', type: 'error' as const }
    case '5':
    case 'difficult':
    case 'hardcore':
    case '困难':
      return { label: '困难', type: 'error' as const }
    default:
      return { label: String(diffVal), type: 'default' as const }
  }
}

// Category Options
const categorySelectOptions = computed(() => {
  return categoryList.value.map(cat => ({
    label: cat.name,
    value: cat.id,
    icon: cat.icon
  }))
})

function renderCategoryOptionLabel(option: { label?: string; icon?: string; [key: string]: any }) {
  return h(
    'div',
    {
      class: 'category-option-item',
      style: {
        display: 'inline-flex',
        alignItems: 'center',
        gap: '8px',
        lineHeight: '1',
        height: '100%',
        verticalAlign: 'middle'
      }
    },
    [
      option.icon
        ? h(VolurIcon, {
            name: option.icon as string,
            size: 16,
            style: { display: 'inline-flex', alignItems: 'center', flexShrink: 0 }
          })
        : null,
      h(
        'span',
        {
          class: 'category-option-text',
          style: {
            lineHeight: '1',
            display: 'inline-flex',
            alignItems: 'center'
          }
        },
        (option.label as string) || ''
      )
    ]
  )
}

// Import State
const showImportModal = ref(false)

// Category Manage State
const showCategoryModal = ref(false)

// Columns
const tableColumns: DataTableColumns<CtfChallengeListVO> = [
  {
    type: 'selection'
  },
  {
    title: 'ID',
    key: 'id',
    width: 190,
    render(row) {
      return h('span', { class: 'mono-text font-bold' }, String(row.id))
    }
  },
  {
    title: '题目名称',
    key: 'name',
    minWidth: 160,
    render(row) {
      return h('span', { class: 'title-text font-medium' }, row.name)
    }
  },
  {
    title: '分类',
    key: 'categoryName',
    width: 120,
    render(row) {
      if (!row.categoryName) {
        return h('span', { class: 'text-muted' }, '-')
      }
      return h(
        NTag,
        { size: 'small', bordered: false, type: 'info' },
        {
          default: () => [
            row.categoryIcon
              ? h(VolurIcon, {
                  name: row.categoryIcon,
                  size: 14,
                  style: 'margin-right: 4px; vertical-align: -2px;'
                })
              : null,
            row.categoryName
          ]
        }
      )
    }
  },
  {
    title: '类型',
    key: 'type',
    width: 110,
    render(row) {
      const typeMap: Record<string, { label: string; type: 'default' | 'success' | 'warning' | 'info' }> = {
        STATIC: { label: '静态', type: 'default' },
        DYNAMIC: { label: '动态Flag', type: 'info' },
        CONTAINER: { label: '动态靶机', type: 'success' }
      }
      const tInfo = (row.type && typeMap[row.type]) || { label: row.type || '未知', type: 'default' }
      return h(NTag, { size: 'small', type: tInfo.type, bordered: false }, { default: () => tInfo.label })
    }
  },
  {
    title: '难度',
    key: 'difficulty',
    width: 90,
    render(row) {
      const badge = getDifficultyBadge(row.difficulty)
      return h(
        NTag,
        { size: 'small', type: badge.type, bordered: false },
        { default: () => badge.label }
      )
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 160,
    fixed: 'right',
    render(row) {
      return h('div', { class: 'table-actions' }, [
        h(
          NButton,
          {
            size: 'tiny',
            quaternary: true,
            onClick: () => router.push(`/admin/challenges/ctf/${row.id}/edit`)
          },
          { default: () => '编辑' }
        ),
        h(
          NButton,
          {
            size: 'tiny',
            quaternary: true,
            onClick: () => handleExportSingle(row)
          },
          { default: () => '导出' }
        ),
        h(
          NPopconfirm,
          {
            onPositiveClick: () => handleDeleteChallenge(row.id)
          },
          {
            trigger: () =>
              h(
                NButton,
                { size: 'tiny', quaternary: true, type: 'error' },
                { default: () => '删除' }
              ),
            default: () => `确定要删除赛题 "${row.name}" 吗？`
          }
        )
      ])
    }
  }
]

// Methods
async function fetchCategories() {
  try {
    const res = await getAdminCtfCategoriesApi()
    if (res.success && res.data) {
      categoryList.value = res.data
    }
  } catch (err) {
    console.error('Failed to load categories:', err)
  }
}

async function fetchChallenges() {
  loadingTable.value = true
  try {
    const res = await getAdminCtfChallengeListApi({
      page: currentPage.value,
      size: pageSize.value,
      categoryId: selectedCategoryId.value || undefined,
      name: searchName.value.trim() || undefined
    })
    if (res.success && res.data) {
      challengeList.value = res.data.list || []
      totalCount.value = Number(res.data.total) || 0
    }
  } catch (err) {
    console.error('Failed to load ctf challenges:', err)
  } finally {
    loadingTable.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  fetchChallenges()
}

function handleResetFilters() {
  selectedCategoryId.value = null
  searchName.value = ''
  currentPage.value = 1
  fetchChallenges()
}

function handlePageChange(page: number) {
  currentPage.value = page
  fetchChallenges()
}

function handlePageSizeChange(size: number) {
  pageSize.value = size
  currentPage.value = 1
  fetchChallenges()
}

function handleCheckedRowKeysChange(keys: (number | string)[]) {
  checkedRowKeys.value = keys
}

// Navigation to SPA Create
function handleCreateChallenge() {
  router.push('/admin/challenges/ctf/create')
}

async function handleDeleteChallenge(id: number | string) {
  try {
    await deleteAdminCtfChallengeApi(id)
    message.success('赛题已成功删除')
    fetchChallenges()
  } catch (err) {
    console.error('Failed to delete challenge:', err)
  }
}

// Export
async function handleExport() {
  exporting.value = true
  try {
    const ids = checkedRowKeys.value.length > 0 ? checkedRowKeys.value : undefined
    const blob = await exportAdminCtfChallengesApi(ids)
    triggerFileDownload(blob, `ctf_challenges_${Date.now()}.zip`)
    message.success('题目导出完成')
  } catch (err) {
    console.error('Export failed:', err)
    message.error('题目导出失败')
  } finally {
    exporting.value = false
  }
}

async function handleExportSingle(row: CtfChallengeListVO) {
  try {
    const blob = await exportAdminCtfChallengesApi([row.id])
    triggerFileDownload(blob, `${row.name.replace(/\s+/g, '_')}.zip`)
    message.success(`已导出赛题 "${row.name}"`)
  } catch (err) {
    console.error('Single export failed:', err)
    message.error('导出失败')
  }
}

function triggerFileDownload(blob: Blob, filename: string) {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

// Import
function openImportModal() {
  showImportModal.value = true
}

function handleImportSuccess() {
  fetchCategories()
  fetchChallenges()
}

// Category Management
function openCategoryModal() {
  showCategoryModal.value = true
}

async function handleExportAll() {
  exporting.value = true
  try {
    const blob = await exportAdminCtfChallengesApi(undefined)
    triggerFileDownload(blob, `ctf_challenges_all_${Date.now()}.zip`)
    message.success('全部题目导出完成')
  } catch (err) {
    console.error('Export all failed:', err)
    message.error('导出失败')
  } finally {
    exporting.value = false
  }
}

// More Actions Dropdown Options
const moreActionsOptions = computed<DropdownOption[]>(() => {
  const options: DropdownOption[] = [
    {
      label: '导入题目 (ZIP)',
      key: 'import',
      icon: () => h(NIcon, null, { default: () => h(CloudUploadOutline) })
    },
    {
      label: checkedRowKeys.value.length > 0
        ? `导出所选 (${checkedRowKeys.value.length})`
        : '导出全部题目',
      key: 'export',
      disabled: challengeList.value.length === 0 || exporting.value,
      icon: () => h(NIcon, null, { default: () => h(CloudDownloadOutline) })
    }
  ]

  if (checkedRowKeys.value.length > 0) {
    options.push({
      label: '导出全部题目',
      key: 'export-all',
      disabled: challengeList.value.length === 0 || exporting.value,
      icon: () => h(NIcon, null, { default: () => h(CloudDownloadOutline) })
    })
  }

  options.push(
    {
      type: 'divider',
      key: 'd1'
    },
    {
      label: '分类管理',
      key: 'category',
      icon: () => h(NIcon, null, { default: () => h(FolderOutline) })
    }
  )

  return options
})

function handleMoreActionSelect(key: string | number) {
  if (key === 'import') {
    openImportModal()
  } else if (key === 'export') {
    handleExport()
  } else if (key === 'export-all') {
    handleExportAll()
  } else if (key === 'category') {
    openCategoryModal()
  }
}

onMounted(() => {
  fetchCategories()
  fetchChallenges()
})
</script>

<style scoped lang="scss">
.admin-ctf-challenge-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header {
  .page-title {
    font-size: 22px;
    font-weight: 600;
    margin: 0;
    color: var(--v-text-primary, #111111);
  }

  .page-desc {
    font-size: 13px;
    color: #444444;
    margin-top: 4px;
  }
}

/* Primary Button Light Mode Iron Rule */
:deep(.theme-primary-btn) {
  background-color: var(--v-btn-primary-bg, #000000) !important;
  color: var(--v-btn-primary-text, #ffffff) !important;
  border: none !important;

  &:hover {
    background-color: var(--v-btn-primary-hover, #222222) !important;
  }

  &:active {
    background-color: var(--v-btn-primary-active, #333333) !important;
  }
}

.ctf-filter-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;

  .filter-select-category {
    width: 150px;
    flex-shrink: 0;
  }

  .filter-input-search {
    width: 220px;
    min-width: 160px;
  }

  .filter-action-buttons {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.ctf-filter-right {
  display: flex;
  align-items: center;
  gap: 10px;

  .dropdown-chevron-icon {
    font-size: 13px;
    margin-left: 4px;
    opacity: 0.8;
  }
}

:deep(.category-option-item) {
  display: inline-flex !important;
  align-items: center !important;
  gap: 8px;
  line-height: 1 !important;
  height: 100%;
  vertical-align: middle;

  .category-option-text {
    line-height: 1 !important;
    display: inline-flex;
    align-items: center;
  }
}

.mono-text {
  font-family: 'Fira Code', monospace;
  font-size: 13px;
}

.font-bold {
  font-weight: 600;
}

.text-muted {
  color: #888888;
  font-size: 12px;
}

.title-text {
  font-weight: 500;
  color: var(--v-text-primary, #222222);
}

.table-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

// 移动端响应式适配 (Mobile & Phone Viewport Optimization)
@media (max-width: 768px) {
  :deep(.filter-header) {
    flex-direction: column;
    align-items: stretch !important;
    gap: 12px;

    .filter-left,
    .filter-right {
      width: 100%;
    }
  }

  .ctf-filter-left {
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: 10px;

    .filter-select-category {
      width: 100% !important;
    }

    .filter-input-search {
      width: 100% !important;
    }

    .filter-action-buttons {
      width: 100%;
      display: flex;
      gap: 10px;

      :deep(.n-button) {
        flex: 1;
        justify-content: center;
      }
    }
  }

  .ctf-filter-right {
    width: 100%;
    display: flex;
    gap: 10px;

    :deep(.n-button) {
      flex: 1;
      justify-content: center;
    }
  }
}
</style>
