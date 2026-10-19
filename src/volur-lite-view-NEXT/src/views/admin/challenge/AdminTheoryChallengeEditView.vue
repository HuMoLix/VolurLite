<template>
  <div class="admin-theory-challenge-edit-view flex flex-col gap-4 sm:gap-6">
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <div class="flex items-center gap-2">
          <h2 class="text-xl font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
            {{ bankDetail?.name || t('admin.challenges.manage_bank_title', '题库管理') }}
          </h2>
          <span
            v-if="bankDetail?.category"
            class="px-2 py-0.5 rounded text-xs bg-google-blue/10 text-google-blue dark:text-google-blue-dark"
          >
            {{ bankDetail.category }}
          </span>
        </div>
        <p v-if="bankDetail?.description" class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary mt-1">
          {{ bankDetail.description }}
        </p>
      </div>
    </div>

    <div class="table-card p-3.5 sm:p-6 rounded-xl sm:rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark transition-colors">
      <div class="flex flex-col sm:flex-row sm:flex-wrap sm:items-center gap-2.5 sm:gap-3 mb-4 sm:mb-6">
        <VSearch
          v-model:value="questionSearchKeyword"
          :placeholder="t('admin.challenges.search_theory_placeholder', '搜索题目名称或内容...')"
          class="w-full sm:w-auto sm:flex-1 sm:min-w-[140px] sm:max-w-sm"
          @search="handleSearchQuestion"
          @clear="handleResetQuestion"
          @keydown.enter="handleSearchQuestion"
        />

        <div class="w-full sm:w-44 shrink-0">
          <VSelection
            v-model:value="questionSelectedType"
            :options="typeSelectOptions"
            :placeholder="t('admin.challenges.filter_theory_type', '全选题型')"
            clearable
            class="w-full"
            @update:value="handleSearchQuestion"
          />
        </div>

        <div class="flex items-center gap-2 w-full sm:w-auto shrink-0">
          <VBtn
            clz="normal"
            size="medium"
            :left-icon="SearchOutline"
            class="flex-1 sm:flex-none"
            @click="handleSearchQuestion"
          >
            {{ t('common.search', '搜索') }}
          </VBtn>

          <VBtn
            clz="normal"
            size="medium"
            :left-icon="RefreshOutline"
            class="flex-1 sm:flex-none"
            @click="handleResetQuestion"
          >
            {{ t('common.reset', '重置') }}
          </VBtn>
        </div>

        <div class="w-full sm:w-auto sm:ml-auto flex items-center gap-2 shrink-0 flex-wrap">
          <input
            ref="importQuestionFileInputRef"
            type="file"
            accept=".json,.zip"
            class="hidden"
            @change="handleQuestionFileSelected"
          />
          <VBtn
            clz="normal"
            size="medium"
            :left-icon="CloudUploadOutline"
            :loading="importingQuestions"
            class="flex-1 sm:flex-none"
            @click="triggerImportQuestion"
          >
            {{ t('admin.challenges.import_questions_btn', '导入题目') }}
          </VBtn>

          <VBtn
            clz="normal"
            size="medium"
            :left-icon="CloudDownloadOutline"
            :loading="exportingQuestions"
            class="flex-1 sm:flex-none"
            @click="handleExportQuestions"
          >
            {{ t('admin.challenges.export_questions_btn', '导出题目') }}
          </VBtn>

          <VBtn
            clz="primary"
            size="medium"
            :left-icon="AddOutline"
            class="w-full sm:w-auto"
            @click="openCreateQuestionModal"
          >
            {{ t('admin.challenges.create_theory_btn', '新增题目') }}
          </VBtn>
        </div>
      </div>

      <VTable
        :columns="questionColumns"
        :data="questionList"
        :loading="loadingQuestions"
        :pagination="false"
        :bordered="false"
        :checked-row-keys="checkedRowKeys"
        :row-key="(row: TheoryChallengeListVO) => row.id"
        :scroll-x="920"
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
            @update:page="handleQuestionPageChange"
            @update:page-size="handleQuestionPageSizeChange"
          />
        </div>
      </div>
    </div>

    <AdminTheoryCreateChallengeModal
      v-model:show="showQuestionModal"
      :bank-id="bankId"
      :bank-category="bankDetail?.category"
      :question-id="currentQuestionId"
      @success="handleQuestionSaved"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, h } from 'vue'
import { useRoute } from 'vue-router'
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
import VSelection from '@/components/common/VSelection.vue'
import VPagination from '@/components/common/VPagination.vue'
import AdminTheoryCreateChallengeModal from './components/AdminTheoryCreateChallengeModal.vue'
import {
  getAdminTheoryBankDetailApi,
  getAdminTheoryBankQuestionsApi,
  deleteAdminTheoryBankQuestionApi,
  importAdminTheoryBankQuestionsApi,
  exportAdminTheoryBankQuestionsApi
} from '@/api/challenge'
import type {
  TheoryBankDetailVO,
  TheoryChallengeListVO,
  TheoryType
} from '@/types/challenge'

defineOptions({
  name: 'AdminTheoryChallengeEditView'
})

const route = useRoute()
const { t } = useI18n()
const message = useMessage()
const dialog = useDialog()

const bankId = computed(() => route.params.id as string)

const bankDetail = ref<TheoryBankDetailVO | null>(null)
const loadingQuestions = ref(false)
const questionList = ref<TheoryChallengeListVO[]>([])
const checkedRowKeys = ref<(number | string)[]>([])
const questionSearchKeyword = ref('')
const questionSelectedType = ref<string | null>(null)
const importingQuestions = ref(false)
const exportingQuestions = ref(false)
const importQuestionFileInputRef = ref<HTMLInputElement | null>(null)

const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

const showQuestionModal = ref(false)
const currentQuestionId = ref<number | string | null>(null)

const typeSelectOptions = [
  { label: '全部题型', value: '' },
  { label: '单项选择题', value: 'SINGLE_CHOICE' },
  { label: '多项选择题', value: 'MULTIPLE_CHOICE' },
  { label: '判断题', value: 'TRUE_FALSE' },
  { label: '主观问答题', value: 'ESSAY' }
]

function getTypeName(type: TheoryType): string {
  switch (type) {
    case 'SINGLE_CHOICE':
      return '单项选择题'
    case 'MULTIPLE_CHOICE':
      return '多项选择题'
    case 'TRUE_FALSE':
      return '判断题'
    case 'ESSAY':
      return '主观问答题'
    default:
      return type || '-'
  }
}

const questionColumns = computed<DataTableColumns<TheoryChallengeListVO>>(() => [
  {
    type: 'selection',
    width: 48
  },
  {
    title: t('admin.challenges.name', '题目名称'),
    key: 'name',
    minWidth: 180,
    render(row) {
      return h(
        'div',
        {
          class: 'redirectable',
          style: { fontWeight: '500' },
          onClick: () => handleOpenEditQuestion(row)
        },
        row.name
      )
    }
  },
  {
    title: t('admin.challenges.type', '题目题型'),
    key: 'type',
    width: 120,
    render(row) {
      return getTypeName(row.type)
    }
  },
  {
    title: t('admin.challenges.category', '所属方向'),
    key: 'category',
    width: 130,
    render(row) {
      return row.category || '-'
    }
  },
  {
    title: t('admin.challenges.author', '创建人'),
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
            onClick: () => handleOpenEditQuestion(row)
          },
          { default: () => t('common.edit', '编辑') }
        ),
        h(
          VBtn,
          {
            clz: 'danger',
            size: 'small',
            onClick: () => handleDeleteQuestion(row)
          },
          { default: () => t('common.delete', '删除') }
        )
      ])
    }
  }
])

async function fetchBankDetail() {
  if (!bankId.value) return
  try {
    const res = await getAdminTheoryBankDetailApi(bankId.value)
    if (res.data) {
      bankDetail.value = res.data
    }
  } catch {
  }
}

async function fetchQuestionList() {
  if (!bankId.value) return
  loadingQuestions.value = true
  try {
    const res = await getAdminTheoryBankQuestionsApi(bankId.value, {
      page: pagination.page,
      size: pagination.pageSize,
      type: (questionSelectedType.value || undefined) as TheoryType | undefined,
      keyword: questionSearchKeyword.value.trim() || undefined
    })
    if (res.data) {
      questionList.value = res.data.list ?? []
      pagination.itemCount = Number(res.data.total) || 0
      if (bankDetail.value) {
        bankDetail.value.questionCount = pagination.itemCount
      }
    }
  } catch {
  } finally {
    loadingQuestions.value = false
  }
}

function handleSearchQuestion() {
  pagination.page = 1
  fetchQuestionList()
}

function handleResetQuestion() {
  questionSearchKeyword.value = ''
  questionSelectedType.value = null
  pagination.page = 1
  fetchQuestionList()
}

function handleQuestionPageChange(page: number) {
  pagination.page = page
  fetchQuestionList()
}

function handleQuestionPageSizeChange(size: number) {
  pagination.pageSize = size
  pagination.page = 1
  fetchQuestionList()
}

function handleCheckedRowKeysChange(keys: (number | string)[]) {
  checkedRowKeys.value = keys
}

function triggerImportQuestion() {
  if (importQuestionFileInputRef.value) {
    importQuestionFileInputRef.value.value = ''
    importQuestionFileInputRef.value.click()
  }
}

async function handleQuestionFileSelected(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file || !bankId.value) return

  importingQuestions.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await importAdminTheoryBankQuestionsApi(bankId.value, formData)
    if (res.data) {
      message.success(`题目导入完成，成功导入 ${res.data.successCount} 道题目`)
    }
    await fetchQuestionList()
    await fetchBankDetail()
  } catch {
  } finally {
    importingQuestions.value = false
  }
}

async function handleExportQuestions() {
  if (!bankId.value) return
  exportingQuestions.value = true
  try {
    const ids = checkedRowKeys.value.length > 0 ? checkedRowKeys.value : undefined
    const blob = await exportAdminTheoryBankQuestionsApi(bankId.value, ids)
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `bank_${bankId.value}_questions_${Date.now()}.json`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success(t('admin.challenges.export_success', '导出成功'))
  } catch {
  } finally {
    exportingQuestions.value = false
  }
}

function openCreateQuestionModal() {
  currentQuestionId.value = null
  showQuestionModal.value = true
}

function handleOpenEditQuestion(row: TheoryChallengeListVO) {
  currentQuestionId.value = row.id
  showQuestionModal.value = true
}

function handleQuestionSaved() {
  fetchQuestionList()
  fetchBankDetail()
}

function handleDeleteQuestion(row: TheoryChallengeListVO) {
  if (!bankId.value) return
  dialog.warning({
    title: t('admin.challenges.delete_confirm_title', '确认删除题目'),
    content: t('admin.challenges.delete_theory_confirm_content', '删除后该题目将从题库中彻底移除，是否继续？'),
    positiveText: t('common.confirm', '确认删除'),
    negativeText: t('common.cancel', '取消'),
    onPositiveClick: async () => {
      try {
        await deleteAdminTheoryBankQuestionApi(bankId.value, row.id)
        message.success(t('admin.challenges.deleted_success', '题目已删除'))
        await fetchQuestionList()
        await fetchBankDetail()
      } catch {
      }
    }
  })
}

onMounted(() => {
  fetchBankDetail()
  fetchQuestionList()
})
</script>
