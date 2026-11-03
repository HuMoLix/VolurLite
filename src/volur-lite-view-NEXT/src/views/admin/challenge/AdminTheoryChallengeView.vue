<template>
  <div class="admin-theory-challenge-view flex flex-col gap-4 sm:gap-6">
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h2 class="text-xl font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
          {{ t('admin.challenges.theory_title', '理论题库') }}
        </h2>
        <p class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary mt-1">
          {{ t('admin.challenges.theory_desc', '管理各科目与分类理论题库，支持批量导入导出题库与题目，并供理论答题赛引用生成试卷') }}
        </p>
      </div>
    </div>

    <div class="table-card p-3.5 sm:p-6 rounded-xl sm:rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark transition-colors">
      <div class="flex flex-col sm:flex-row sm:flex-wrap sm:items-center gap-2.5 sm:gap-3 mb-4 sm:mb-6">
        <VSearch
          v-model:value="searchKeyword"
          :placeholder="t('admin.challenges.search_bank_placeholder', '搜索题库名称或创建人')"
          class="w-full sm:w-auto sm:flex-1 sm:min-w-[140px] sm:max-w-sm"
          @search="handleSearch"
          @clear="handleReset"
          @keydown.enter="handleSearch"
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
          <input
            ref="importBankFileInputRef"
            type="file"
            accept=".json,.zip"
            class="hidden"
            @change="handleBankFileSelected"
          />
          <VBtn
            clz="normal"
            size="medium"
            :left-icon="CloudUploadOutline"
            :loading="importingBank"
            class="flex-1 sm:flex-none"
            @click="triggerImportBank"
          >
            {{ t('admin.challenges.import_bank_btn', '导入题库') }}
          </VBtn>

          <VBtn
            clz="normal"
            size="medium"
            :left-icon="CloudDownloadOutline"
            :loading="exportingBank"
            class="flex-1 sm:flex-none"
            @click="handleExportBanks"
          >
            {{ t('admin.challenges.export_btn', '导出题库') }}
          </VBtn>

          <VBtn
            clz="primary"
            size="medium"
            :left-icon="AddOutline"
            class="w-full sm:w-auto"
            @click="openCreateBankModal"
          >
            {{ t('admin.challenges.create_bank_btn', '新增题库') }}
          </VBtn>
        </div>
      </div>

      <VTable
        :columns="tableColumns"
        :data="bankList"
        :loading="loadingTable"
        :pagination="false"
        :bordered="false"
        :checked-row-keys="checkedRowKeys"
        :row-key="(row: TheoryBankListVO) => row.id"
        :scroll-x="980"
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

    <VModal
      v-model:show="showBankModal"
      :title="isEditBank ? t('admin.challenges.edit_bank_title', '编辑题库') : t('admin.challenges.create_bank_title', '新增题库')"
      :width="540"
    >
      <div class="flex flex-col gap-4 py-2">
        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
            {{ t('admin.challenges.bank_name', '题库名称') }} *
          </label>
          <VInput
            v-model:value="bankForm.name"
            :placeholder="t('admin.challenges.bank_name_placeholder', '请输入题库名称，例如: 网络安全基线题库')"
            clearable
          />
        </div>

        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
            {{ t('admin.challenges.category', '分类') }}
          </label>
          <VInput
            v-model:value="bankForm.category"
            :placeholder="'例如: Web安全 / 密码学 / 渗透攻防'"
            clearable
          />
        </div>

        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
            {{ t('admin.challenges.description', '题库描述') }}
          </label>
          <VInput
            v-model:value="bankForm.description"
            type="textarea"
            :rows="3"
            :placeholder="'可选，简要说明题库适用范围或考察重点'"
          />
        </div>
      </div>

      <template #footer>
        <div class="flex items-center justify-end gap-3 w-full">
          <VBtn clz="normal" @click="showBankModal = false">
            {{ t('common.cancel', '取消') }}
          </VBtn>
          <VBtn
            clz="primary"
            :loading="savingBank"
            @click="handleSaveBank"
          >
            {{ t('common.save', '保存题库') }}
          </VBtn>
        </div>
      </template>
    </VModal>
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
  CloudDownloadOutline,
  CloudUploadOutline
} from '@vicons/ionicons5'
import VBtn from '@/components/common/VBtn.vue'
import VTable from '@/components/common/VTable.vue'
import VSearch from '@/components/common/VSearch.vue'
import VPagination from '@/components/common/VPagination.vue'
import VModal from '@/components/common/VModal.vue'
import VInput from '@/components/common/VInput.vue'
import {
  getAdminTheoryBankListApi,
  getAdminTheoryBankDetailApi,
  createAdminTheoryBankApi,
  updateAdminTheoryBankApi,
  deleteAdminTheoryBankApi,
  importAdminTheoryBanksApi,
  exportAdminTheoryBanksApi
} from '@/api/challenge'
import type { TheoryBankListVO } from '@/types/challenge'

defineOptions({
  name: 'AdminTheoryChallengeView'
})

const router = useRouter()
const { t } = useI18n()
const message = useMessage()
const dialog = useDialog()

const loadingTable = ref(false)
const bankList = ref<TheoryBankListVO[]>([])
const checkedRowKeys = ref<(number | string)[]>([])
const searchKeyword = ref('')
const importingBank = ref(false)
const exportingBank = ref(false)
const importBankFileInputRef = ref<HTMLInputElement | null>(null)

const showBankModal = ref(false)
const isEditBank = ref(false)
const currentBankId = ref<number | string | null>(null)
const savingBank = ref(false)
const bankForm = reactive({
  name: '',
  category: '',
  description: ''
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

const tableColumns = computed<DataTableColumns<TheoryBankListVO>>(() => [
  {
    type: 'selection',
    width: 48
  },
  {
    title: t('admin.challenges.bank_name', '题库名称'),
    key: 'name',
    minWidth: 180,
    render(row) {
      return h('div', { class: 'space-y-0.5' }, [
        h(
          'div',
          {
            class: 'redirectable',
            style: { fontWeight: '500' },
            onClick: () => router.push(`/admin/challenges/theory/${row.id}/edit`)
          },
          row.name
        ),
        row.category
          ? h(
              'div',
              {
                class: 'text-[12px] truncate max-w-xs',
                style: {
                  color: 'var(--color-v3-text-var)',
                  fontSize: '12px'
                }
              },
              row.category
            )
          : null
      ])
    }
  },
  {
    title: t('admin.challenges.question_count', '题目数量'),
    key: 'questionCount',
    width: 120,
    align: 'center',
    render(row) {
      return row.questionCount ?? 0
    }
  },
  {
    title: t('admin.challenges.author', '创建人'),
    key: 'authorName',
    width: 130,
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
    title: t('admin.challenges.updated_at', '更新时间'),
    key: 'updatedAt',
    width: 170,
    render(row) {
      return row.updatedAt ? new Date(row.updatedAt).toLocaleString() : '-'
    }
  },
  {
    title: t('common.actions', '操作'),
    key: 'actions',
    width: 210,
    align: 'center',
    render(row) {
      return h('div', { class: 'flex items-center justify-center gap-2' }, [
        h(
          VBtn,
          {
            clz: 'primary',
            size: 'small',
            onClick: () => router.push(`/admin/challenges/theory/${row.id}/edit`)
          },
          { default: () => t('admin.challenges.manage_questions', '管理题目') }
        ),
        h(
          VBtn,
          {
            clz: 'normal',
            size: 'small',
            onClick: () => handleOpenEditBank(row)
          },
          { default: () => t('common.edit', '编辑') }
        ),
        h(
          VBtn,
          {
            clz: 'danger',
            size: 'small',
            onClick: () => handleDeleteBank(row)
          },
          { default: () => t('common.delete', '删除') }
        )
      ])
    }
  }
])

async function fetchBankList() {
  loadingTable.value = true
  try {
    const res = await getAdminTheoryBankListApi({
      page: pagination.page,
      size: pagination.pageSize,
      keyword: searchKeyword.value.trim() || undefined
    })
    if (res.data) {
      bankList.value = res.data.list ?? []
      pagination.itemCount = Number(res.data.total) || 0
    }
  } catch {
  } finally {
    loadingTable.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchBankList()
}

function handleReset() {
  searchKeyword.value = ''
  pagination.page = 1
  fetchBankList()
}

function handlePageChange(page: number) {
  pagination.page = page
  fetchBankList()
}

function handlePageSizeChange(size: number) {
  pagination.pageSize = size
  pagination.page = 1
  fetchBankList()
}

function handleCheckedRowKeysChange(keys: (number | string)[]) {
  checkedRowKeys.value = keys
}

function openCreateBankModal() {
  isEditBank.value = false
  currentBankId.value = null
  bankForm.name = ''
  bankForm.category = ''
  bankForm.description = ''
  showBankModal.value = true
}

async function handleOpenEditBank(row: TheoryBankListVO) {
  isEditBank.value = true
  currentBankId.value = row.id
  try {
    const res = await getAdminTheoryBankDetailApi(row.id)
    if (res.data) {
      bankForm.name = res.data.name || ''
      bankForm.category = res.data.category || ''
      bankForm.description = res.data.description || ''
      showBankModal.value = true
    }
  } catch {
  }
}

async function handleSaveBank() {
  if (!bankForm.name.trim()) {
    message.warning(t('admin.challenges.bank_name_required', '请填写题库名称'))
    return
  }

  savingBank.value = true
  try {
    if (isEditBank.value && currentBankId.value) {
      await updateAdminTheoryBankApi(currentBankId.value, {
        name: bankForm.name.trim(),
        category: bankForm.category.trim() || undefined,
        description: bankForm.description.trim() || undefined
      })
      message.success(t('admin.challenges.update_bank_success', '题库更新成功'))
    } else {
      await createAdminTheoryBankApi({
        name: bankForm.name.trim(),
        category: bankForm.category.trim() || undefined,
        description: bankForm.description.trim() || undefined
      })
      message.success(t('admin.challenges.create_bank_success', '题库创建成功'))
    }
    showBankModal.value = false
    await fetchBankList()
  } catch {
  } finally {
    savingBank.value = false
  }
}

function handleDeleteBank(row: TheoryBankListVO) {
  dialog.warning({
    title: t('admin.challenges.delete_confirm_title', '确认删除题库'),
    content: t('admin.challenges.delete_bank_confirm_content', '删除后该题库及其包含的所有题目都将被彻底移除，是否继续？'),
    positiveText: t('common.confirm', '确认删除'),
    negativeText: t('common.cancel', '取消'),
    onPositiveClick: async () => {
      try {
        await deleteAdminTheoryBankApi(row.id)
        message.success(t('admin.challenges.deleted_bank_success', '题库已删除'))
        await fetchBankList()
      } catch {
      }
    }
  })
}

function triggerImportBank() {
  if (importBankFileInputRef.value) {
    importBankFileInputRef.value.value = ''
    importBankFileInputRef.value.click()
  }
}

async function handleBankFileSelected(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  importingBank.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await importAdminTheoryBanksApi(formData)
    if (res.data) {
      message.success(`题库导入完成，成功导入 ${res.data.successCount} 条题目`)
    }
    await fetchBankList()
  } catch {
  } finally {
    importingBank.value = false
  }
}

async function handleExportBanks() {
  exportingBank.value = true
  try {
    const ids = checkedRowKeys.value.length > 0 ? checkedRowKeys.value : undefined
    const blob = await exportAdminTheoryBanksApi(ids)
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `theory_banks_export_${Date.now()}.json`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success(t('admin.challenges.export_success', '导出成功'))
  } catch {
  } finally {
    exportingBank.value = false
  }
}

onMounted(() => {
  fetchBankList()
})
</script>
