<template>
  <div class="admin-theory-challenge-view">
    <!-- Header Section -->
    <div class="page-header">
      <h2 class="page-title">{{ t('admin.challenges.theory_title') }}</h2>
      <p class="page-desc">{{ t('admin.challenges.theory_desc') }}</p>
    </div>

    <!-- Main Table Card -->
    <VolurTableCard
      :bordered="true"
      :table-bordered="true"
      :scroll-x="1000"
      :columns="tableColumns"
      :data="challengeList"
      :loading="loadingTable"
      table-class="theory-challenges-table"
      standalone-pagination
      :page="currentPage"
      :page-size="pageSize"
      :item-count="totalCount"
      :row-key="(row: TheoryChallengeListVO) => row.id"
      :checked-row-keys="checkedRowKeys"
      @update:page="handlePageChange"
      @update:page-size="handlePageSizeChange"
      @update:checked-row-keys="handleCheckedRowKeysChange"
    >
      <template #filter-left>
        <div class="theory-filter-left">
          <n-select
            v-model:value="selectedType"
            :options="typeSelectOptions"
            placeholder="全部题型"
            clearable
            class="filter-select-type"
            @update:value="handleSearch"
          />

          <n-input
            v-model:value="searchCategory"
            placeholder="分类 (如: Web基础)"
            clearable
            class="filter-input-category"
            @keyup.enter="handleSearch"
          />

          <n-input
            v-model:value="searchKeyword"
            placeholder="搜索题目标题 / 题干..."
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
        <div class="theory-filter-right">
          <n-button type="primary" class="theme-primary-btn action-create-btn" @click="openCreateModal">
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
    </VolurTableCard>

    <!-- Create / Edit Theory Challenge Modal -->
    <n-modal
      v-model:show="showEditModal"
      preset="card"
      :title="isEditMode ? '编辑理论赛题' : '新增理论赛题'"
      style="width: 760px; max-width: 95vw;"
      :mask-closable="false"
    >
      <n-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-placement="left"
        label-width="100"
        require-mark-placement="left"
      >
        <n-form-item label="题目标题" path="name">
          <n-input v-model:value="formData.name" placeholder="请输入题目简述或标题 (例: SQL注入防御原理)" />
        </n-form-item>

        <div class="form-row-2">
          <n-form-item label="所属分类" path="category">
            <n-input v-model:value="formData.category" placeholder="分类标签 (例: Web安全基础, 密码学)" />
          </n-form-item>

          <n-form-item label="题目类型" path="type">
            <n-select
              v-model:value="formData.type"
              :options="theoryTypeOptions"
              placeholder="选择题目类型"
              @update:value="handleTypeChange"
            />
          </n-form-item>
        </div>

        <div class="form-row-2">
          <n-form-item label="题目分值" path="score">
            <n-input-number v-model:value="formData.score" :min="1" :max="100" style="width: 100%;" />
          </n-form-item>

          <n-form-item label="难度等级" path="difficulty">
            <n-rate v-model:value="formData.difficulty" :count="5" />
          </n-form-item>
        </div>

        <n-form-item label="题干内容" path="content">
          <n-input
            v-model:value="formData.content"
            type="textarea"
            :autosize="{ minRows: 3, maxRows: 6 }"
            placeholder="请输入完整的题目题干与问答描述，支持换行与代码格式..."
          />
        </n-form-item>

        <!-- Options Builder for Choice questions -->
        <div v-if="formData.type === 'SINGLE_CHOICE' || formData.type === 'MULTIPLE_CHOICE'" class="options-builder-box">
          <div class="options-header">
            <span class="options-title">选项设置 (A, B, C, D...)</span>
            <n-button size="tiny" secondary @click="addOption">
              <template #icon>
                <n-icon :component="AddOutline" />
              </template>
              添加选项
            </n-button>
          </div>

          <div class="option-items-list">
            <div v-for="(item, idx) in visualOptions" :key="idx" class="option-row">
              <span class="option-tag">{{ item.key }}.</span>
              <n-input
                v-model:value="item.text"
                size="small"
                placeholder="选项文本内容..."
                style="flex: 1;"
              />
              <n-button
                size="tiny"
                quaternary
                type="error"
                :disabled="visualOptions.length <= 2"
                @click="removeOption(idx)"
              >
                删除
              </n-button>
            </div>
          </div>
        </div>

        <!-- Standard Answer Config -->
        <n-form-item label="标准答案" path="standardAnswer">
          <!-- Single Choice Answer -->
          <n-radio-group
            v-if="formData.type === 'SINGLE_CHOICE'"
            v-model:value="formData.standardAnswer"
            name="single-answer"
          >
            <n-radio v-for="item in visualOptions" :key="item.key" :value="item.key">
              {{ item.key }}
            </n-radio>
          </n-radio-group>

          <!-- Multiple Choice Answer -->
          <n-checkbox-group
            v-else-if="formData.type === 'MULTIPLE_CHOICE'"
            v-model:value="multiChoiceAnswer"
            @update:value="handleMultiAnswerChange"
          >
            <n-checkbox v-for="item in visualOptions" :key="item.key" :value="item.key">
              {{ item.key }}
            </n-checkbox>
          </n-checkbox-group>

          <!-- True/False Answer -->
          <n-radio-group
            v-else-if="formData.type === 'TRUE_FALSE'"
            v-model:value="formData.standardAnswer"
            name="tf-answer"
          >
            <n-radio value="TRUE">正确 (TRUE)</n-radio>
            <n-radio value="FALSE">错误 (FALSE)</n-radio>
          </n-radio-group>

          <!-- Essay / Fill-in Answer -->
          <n-input
            v-else
            v-model:value="formData.standardAnswer"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 4 }"
            placeholder="请输入标准参考答案或关键关键词..."
          />
        </n-form-item>

        <n-form-item label="答案解析" path="explanation">
          <n-input
            v-model:value="formData.explanation"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 4 }"
            placeholder="请输入针对本题的深度解析、解题思路或知识点延伸..."
          />
        </n-form-item>
      </n-form>

      <template #footer>
        <div class="modal-footer">
          <n-button @click="showEditModal = false">取消</n-button>
          <n-button type="primary" class="theme-primary-btn" :loading="saving" @click="handleSaveChallenge">
            {{ isEditMode ? '保存修改' : '确认创建' }}
          </n-button>
        </div>
      </template>
    </n-modal>

    <!-- Detail Drawer -->
    <n-drawer v-model:show="showDetailDrawer" :width="500">
      <n-drawer-content title="理论题目详细信息" closable>
        <div v-if="detailChallenge" class="detail-content">
          <div class="detail-header">
            <h3 class="detail-title">{{ detailChallenge.name }}</h3>
            <div class="detail-tags">
              <n-tag size="small" :type="getTypeTagType(detailChallenge.type)">
                {{ getTypeLabel(detailChallenge.type) }}
              </n-tag>
              <n-tag v-if="detailChallenge.category" size="small" type="info">
                {{ detailChallenge.category }}
              </n-tag>
              <span class="detail-score">{{ detailChallenge.score || 5 }} 分</span>
            </div>
          </div>

          <div class="detail-section">
            <span class="section-label">题干内容：</span>
            <div class="content-box">{{ detailChallenge.content }}</div>
          </div>

          <div v-if="parsedDetailOptions.length > 0" class="detail-section">
            <span class="section-label">选项列表：</span>
            <div class="options-display">
              <div
                v-for="opt in parsedDetailOptions"
                :key="opt.key"
                class="opt-item"
                :class="{ 'is-correct': isOptionCorrect(opt.key) }"
              >
                <span class="opt-key">{{ opt.key }}.</span>
                <span class="opt-text">{{ opt.text }}</span>
                <n-tag v-if="isOptionCorrect(opt.key)" size="tiny" type="success" style="margin-left: auto;">
                  标准答案
                </n-tag>
              </div>
            </div>
          </div>

          <div class="detail-section">
            <span class="section-label">标准答案：</span>
            <div class="answer-box">
              <span class="answer-badge">{{ detailChallenge.standardAnswer }}</span>
            </div>
          </div>

          <div v-if="detailChallenge.explanation" class="detail-section">
            <span class="section-label">解析说明：</span>
            <div class="explanation-box">{{ detailChallenge.explanation }}</div>
          </div>

          <div class="detail-meta">
            <div>出题人: {{ detailChallenge.authorName || '管理员' }}</div>
            <div>创建时间: {{ detailChallenge.createdAt || '-' }}</div>
          </div>
        </div>
      </n-drawer-content>
    </n-drawer>

    <!-- Import JSON / ZIP Modal -->
    <n-modal
      v-model:show="showImportModal"
      preset="card"
      title="批量导入理论赛题 (JSON / ZIP)"
      style="width: 600px; max-width: 95vw;"
      :mask-closable="false"
    >
      <div class="import-dialog-content">
        <n-alert type="info" title="导入规范说明" style="margin-bottom: 16px;">
          <p>支持上传单个 <code>.json</code> 文件或包含 JSON 题库文件的 <code>.zip</code> 压缩包。</p>
          <p>JSON 格式需包含题目数组，每个元素包含 <code>name</code>、<code>type</code>（SINGLE_CHOICE / MULTIPLE_CHOICE / TRUE_FALSE / ESSAY）、<code>content</code>、<code>options</code> 及 <code>standardAnswer</code>。</p>
        </n-alert>

        <n-upload
          v-if="!importResult"
          directory-dnd
          :max="1"
          accept=".json,.zip"
          :custom-request="handleCustomImportRequest"
          :show-file-list="false"
        >
          <n-upload-dragger>
            <div class="upload-dropzone">
              <n-icon size="48" :depth="3" :component="CloudUploadOutline" />
              <div class="upload-text">点击或拖拽题目文件 (.json 或 .zip) 至此区域上传</div>
              <p class="upload-hint">支持单次导入成百上千道理论选择题与问答题</p>
            </div>
          </n-upload-dragger>
        </n-upload>

        <!-- Loading State -->
        <div v-if="importing" class="import-loading-state">
          <n-spin size="large" />
          <p class="loading-text">正在解析理论题目并批量写入题库，请稍候...</p>
        </div>

        <!-- Import Result Summary -->
        <div v-if="importResult" class="import-result-view">
          <div class="result-stats-ribbon">
            <div class="res-stat-card total">
              <span class="num">{{ importResult.totalCount }}</span>
              <span class="label">读取题目总数</span>
            </div>
            <div class="res-stat-card success">
              <span class="num">{{ importResult.successCount }}</span>
              <span class="label">成功入库</span>
            </div>
            <div class="res-stat-card failed">
              <span class="num">{{ importResult.failedCount }}</span>
              <span class="label">失败数量</span>
            </div>
          </div>

          <div v-if="importResult.errors && importResult.errors.length > 0" class="errors-box">
            <div class="sub-title error-text">
              <n-icon :component="CloseCircleOutline" />
              <span>导入异常与失败信息：</span>
            </div>
            <ul class="error-items">
              <li v-for="(err, idx) in importResult.errors" :key="idx">{{ err }}</li>
            </ul>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="modal-footer">
          <n-button v-if="importResult" secondary @click="resetImportModal">继续导入</n-button>
          <n-button type="primary" class="theme-primary-btn" @click="closeImportModal">
            {{ importResult ? '完成并刷新列表' : '取消' }}
          </n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, h } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  NButton,
  NIcon,
  NInput,
  NInputNumber,
  NSelect,
  NRadioGroup,
  NRadio,
  NCheckboxGroup,
  NCheckbox,
  NRate,
  NTag,
  NModal,
  NDrawer,
  NDrawerContent,
  NForm,
  NFormItem,
  NAlert,
  NUpload,
  NUploadDragger,
  NSpin,
  NPopconfirm,
  NDropdown,
  useMessage,
  type DataTableColumns,
  type DropdownOption,
  type FormInst,
  type FormRules,
  type UploadCustomRequestOptions
} from 'naive-ui'
import { VolurTableCard } from '@/public/volurEx'
import {
  SearchOutline,
  RefreshOutline,
  AddOutline,
  CloudUploadOutline,
  CloudDownloadOutline,
  CloseCircleOutline,
  EllipsisHorizontalOutline,
  ChevronDownOutline
} from '@vicons/ionicons5'
import {
  getAdminTheoryChallengeListApi,
  getAdminTheoryChallengeDetailApi,
  createAdminTheoryChallengeApi,
  updateAdminTheoryChallengeApi,
  deleteAdminTheoryChallengeApi,
  importAdminTheoryChallengesApi,
  exportAdminTheoryChallengesApi
} from '@/api/challenge'
import type {
  TheoryChallengeListVO,
  TheoryChallengeDetailVO,
  TheoryChallengeCreateRequestDTO,
  TheoryType,
  ChallengeImportResultVO
} from '@/types/challenge'

const { t } = useI18n()
const message = useMessage()

// State
const loadingTable = ref(false)
const challengeList = ref<TheoryChallengeListVO[]>([])
const totalCount = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const checkedRowKeys = ref<(number | string)[]>([])

const selectedType = ref<TheoryType | null>(null)
const searchCategory = ref('')
const searchKeyword = ref('')

// Export State
const exporting = ref(false)

// Edit / Create Modal State
const showEditModal = ref(false)
const isEditMode = ref(false)
const editingId = ref<number | string | null>(null)
const saving = ref(false)
const formRef = ref<FormInst | null>(null)

// Detail Drawer State
const showDetailDrawer = ref(false)
const detailChallenge = ref<TheoryChallengeDetailVO | null>(null)

const visualOptions = ref<{ key: string; text: string }[]>([
  { key: 'A', text: '' },
  { key: 'B', text: '' },
  { key: 'C', text: '' },
  { key: 'D', text: '' }
])
const multiChoiceAnswer = ref<(string | number)[]>(['A'])

const formData = reactive<{
  name: string
  category: string
  type: TheoryType
  content: string
  options: string
  standardAnswer: string
  explanation: string
  score: number
  difficulty: number
}>({
  name: '',
  category: '',
  type: 'SINGLE_CHOICE',
  content: '',
  options: '',
  standardAnswer: 'A',
  explanation: '',
  score: 5,
  difficulty: 1
})

const formRules: FormRules = {
  name: [{ required: true, message: '请输入题目名称', trigger: 'blur' }],
  content: [{ required: true, message: '请输入题干内容', trigger: 'blur' }],
  type: [{ required: true, message: '请选择题目类型', trigger: 'change' }],
  standardAnswer: [{ required: true, message: '请输入或选择标准答案', trigger: 'change' }]
}

const typeSelectOptions = [
  { label: '单选题 (SINGLE_CHOICE)', value: 'SINGLE_CHOICE' },
  { label: '多选题 (MULTIPLE_CHOICE)', value: 'MULTIPLE_CHOICE' },
  { label: '判断题 (TRUE_FALSE)', value: 'TRUE_FALSE' },
  { label: '简答/填空题 (ESSAY)', value: 'ESSAY' }
]

const theoryTypeOptions = [
  { label: '单选题 (Single Choice)', value: 'SINGLE_CHOICE' },
  { label: '多选题 (Multiple Choice)', value: 'MULTIPLE_CHOICE' },
  { label: '判断题 (True/False)', value: 'TRUE_FALSE' },
  { label: '简答题 (Essay)', value: 'ESSAY' }
]

// Import State
const showImportModal = ref(false)
const importing = ref(false)
const importResult = ref<ChallengeImportResultVO | null>(null)

// Columns
const tableColumns: DataTableColumns<TheoryChallengeListVO> = [
  {
    type: 'selection'
  },
  {
    title: 'ID',
    key: 'id',
    width: 80,
    render(row) {
      return h('span', { class: 'mono-text font-bold' }, `#${row.id}`)
    }
  },
  {
    title: '题目标题 / 题干',
    key: 'name',
    minWidth: 220,
    render(row) {
      return h('div', { class: 'theory-title-cell' }, [
        h('span', { class: 'title-text' }, row.name),
        row.category
          ? h(NTag, { size: 'tiny', bordered: false, type: 'info', style: 'margin-left: 6px;' }, { default: () => row.category })
          : null
      ])
    }
  },
  {
    title: '题型',
    key: 'type',
    width: 110,
    render(row) {
      return h(
        NTag,
        { size: 'small', type: getTypeTagType(row.type), bordered: false },
        { default: () => getTypeLabel(row.type) }
      )
    }
  },
  {
    title: '分值',
    key: 'score',
    width: 90,
    render(row) {
      return h('span', { class: 'font-bold' }, `${row.score || 5} 分`)
    }
  },
  {
    title: '难度',
    key: 'difficulty',
    width: 120,
    render(row) {
      return h(NRate, { readonly: true, size: 'small', value: row.difficulty || 1, count: 5 })
    }
  },
  {
    title: '创建人',
    key: 'authorName',
    width: 110,
    render(row) {
      return h('span', { class: 'text-muted' }, row.authorName || '管理员')
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 180,
    fixed: 'right',
    render(row) {
      return h('div', { class: 'table-actions' }, [
        h(
          NButton,
          {
            size: 'tiny',
            quaternary: true,
            onClick: () => handleViewDetail(row.id)
          },
          { default: () => '详情' }
        ),
        h(
          NButton,
          {
            size: 'tiny',
            quaternary: true,
            onClick: () => handleEditChallenge(row.id)
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
            default: () => `确定要删除理论题目 "${row.name}" 吗？`
          }
        )
      ])
    }
  }
]

// Helper Functions
function getTypeLabel(type: TheoryType): string {
  const map: Record<TheoryType, string> = {
    SINGLE_CHOICE: '单选',
    MULTIPLE_CHOICE: '多选',
    TRUE_FALSE: '判断',
    ESSAY: '简答'
  }
  return map[type] || type
}

function getTypeTagType(type: TheoryType): 'default' | 'info' | 'success' | 'warning' {
  const map: Record<TheoryType, 'default' | 'info' | 'success' | 'warning'> = {
    SINGLE_CHOICE: 'info',
    MULTIPLE_CHOICE: 'success',
    TRUE_FALSE: 'warning',
    ESSAY: 'default'
  }
  return map[type] || 'default'
}

const parsedDetailOptions = computed(() => {
  if (!detailChallenge.value?.options) return []
  try {
    const raw = JSON.parse(detailChallenge.value.options)
    if (Array.isArray(raw)) {
      return raw.map((item, idx) => ({
        key: String.fromCharCode(65 + idx),
        text: typeof item === 'string' ? item : item.text || item.content || JSON.stringify(item)
      }))
    }
    if (typeof raw === 'object' && raw !== null) {
      return Object.entries(raw).map(([key, val]) => ({
        key,
        text: String(val)
      }))
    }
  } catch {
    // Plain string lines
    return detailChallenge.value.options.split('\n').filter(Boolean).map((line, idx) => ({
      key: String.fromCharCode(65 + idx),
      text: line
    }))
  }
  return []
})

function isOptionCorrect(key: string): boolean {
  if (!detailChallenge.value?.standardAnswer) return false
  const ans = detailChallenge.value.standardAnswer.trim().toUpperCase()
  return ans.includes(key.toUpperCase())
}

// Option management in form
function addOption() {
  const nextKey = String.fromCharCode(65 + visualOptions.value.length)
  visualOptions.value.push({ key: nextKey, text: '' })
}

function removeOption(idx: number) {
  visualOptions.value.splice(idx, 1)
  // Re-index keys
  visualOptions.value.forEach((item, i) => {
    item.key = String.fromCharCode(65 + i)
  })
}

function handleTypeChange(val: TheoryType) {
  if (val === 'TRUE_FALSE') {
    formData.standardAnswer = 'TRUE'
  } else if (val === 'SINGLE_CHOICE') {
    formData.standardAnswer = visualOptions.value[0]?.key || 'A'
  } else if (val === 'MULTIPLE_CHOICE') {
    multiChoiceAnswer.value = ['A']
    formData.standardAnswer = 'A'
  } else {
    formData.standardAnswer = ''
  }
}

function handleMultiAnswerChange(vals: (string | number)[]) {
  multiChoiceAnswer.value = vals
  formData.standardAnswer = vals.map(String).sort().join('')
}

// API Methods
async function fetchChallenges() {
  loadingTable.value = true
  try {
    const res = await getAdminTheoryChallengeListApi({
      page: currentPage.value,
      size: pageSize.value,
      type: selectedType.value || undefined,
      category: searchCategory.value.trim() || undefined,
      keyword: searchKeyword.value.trim() || undefined
    })
    if (res.success && res.data) {
      challengeList.value = res.data.list || []
      totalCount.value = Number(res.data.total) || 0
    }
  } catch (err) {
    console.error('Failed to load theory challenges:', err)
  } finally {
    loadingTable.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  fetchChallenges()
}

function handleResetFilters() {
  selectedType.value = null
  searchCategory.value = ''
  searchKeyword.value = ''
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

// Create & Edit
function openCreateModal() {
  isEditMode.value = false
  editingId.value = null
  formData.name = ''
  formData.category = ''
  formData.type = 'SINGLE_CHOICE'
  formData.content = ''
  formData.options = ''
  formData.standardAnswer = 'A'
  formData.explanation = ''
  formData.score = 5
  formData.difficulty = 1
  visualOptions.value = [
    { key: 'A', text: '' },
    { key: 'B', text: '' },
    { key: 'C', text: '' },
    { key: 'D', text: '' }
  ]
  multiChoiceAnswer.value = ['A']
  showEditModal.value = true
}

async function handleViewDetail(id: number | string) {
  try {
    const res = await getAdminTheoryChallengeDetailApi(id)
    if (res.success && res.data) {
      detailChallenge.value = res.data
      showDetailDrawer.value = true
    }
  } catch (err) {
    console.error('Failed to view challenge:', err)
    message.error('获取题目详情失败')
  }
}

async function handleEditChallenge(id: number | string) {
  isEditMode.value = true
  editingId.value = id
  try {
    const res = await getAdminTheoryChallengeDetailApi(id)
    if (res.success && res.data) {
      const d = res.data
      formData.name = d.name
      formData.category = d.category || ''
      formData.type = d.type
      formData.content = d.content
      formData.options = d.options || ''
      formData.standardAnswer = d.standardAnswer
      formData.explanation = d.explanation || ''
      formData.score = d.score || 5
      formData.difficulty = d.difficulty || 1

      // Parse options back to visual options if choice
      if (d.options) {
        try {
          const parsed = JSON.parse(d.options)
          if (typeof parsed === 'object' && parsed !== null && !Array.isArray(parsed)) {
            visualOptions.value = Object.entries(parsed).map(([k, v]) => ({ key: k, text: String(v) }))
          } else if (Array.isArray(parsed)) {
            visualOptions.value = parsed.map((v, i) => ({
              key: String.fromCharCode(65 + i),
              text: typeof v === 'string' ? v : JSON.stringify(v)
            }))
          }
        } catch {
          visualOptions.value = [
            { key: 'A', text: '' },
            { key: 'B', text: '' }
          ]
        }
      }

      if (d.type === 'MULTIPLE_CHOICE') {
        multiChoiceAnswer.value = d.standardAnswer.split('').filter(Boolean)
      }

      showEditModal.value = true
    }
  } catch (err) {
    console.error('Failed to load challenge for edit:', err)
    message.error('加载题目失败')
  }
}

async function handleSaveChallenge() {
  formRef.value?.validate(async (errors) => {
    if (errors) return
    saving.value = true
    try {
      // Build options JSON string if choice type
      let optionsStr = ''
      if (formData.type === 'SINGLE_CHOICE' || formData.type === 'MULTIPLE_CHOICE') {
        const optObj: Record<string, string> = {}
        visualOptions.value.forEach(item => {
          optObj[item.key] = item.text.trim()
        })
        optionsStr = JSON.stringify(optObj)
      } else if (formData.type === 'TRUE_FALSE') {
        optionsStr = JSON.stringify({ A: '正确', B: '错误' })
      }

      const payload: TheoryChallengeCreateRequestDTO = {
        name: formData.name.trim(),
        category: formData.category.trim() || undefined,
        type: formData.type,
        content: formData.content.trim(),
        options: optionsStr || undefined,
        standardAnswer: formData.standardAnswer.trim(),
        explanation: formData.explanation.trim() || undefined,
        score: formData.score,
        difficulty: formData.difficulty
      }

      if (isEditMode.value && editingId.value) {
        await updateAdminTheoryChallengeApi(editingId.value, payload)
        message.success('理论题目更新成功')
      } else {
        await createAdminTheoryChallengeApi(payload)
        message.success('理论题目创建成功')
      }
      showEditModal.value = false
      fetchChallenges()
    } catch (err) {
      console.error('Failed to save theory challenge:', err)
    } finally {
      saving.value = false
    }
  })
}

async function handleDeleteChallenge(id: number | string) {
  try {
    await deleteAdminTheoryChallengeApi(id)
    message.success('理论题目已删除')
    fetchChallenges()
  } catch (err) {
    console.error('Failed to delete theory challenge:', err)
  }
}

// Export
async function handleExport() {
  exporting.value = true
  try {
    const ids = checkedRowKeys.value.length > 0 ? checkedRowKeys.value : undefined
    const blob = await exportAdminTheoryChallengesApi(ids)
    triggerFileDownload(blob, `theory_challenges_${Date.now()}.json`)
    message.success('理论题目导出成功')
  } catch (err) {
    console.error('Theory export failed:', err)
    message.error('理论题目导出失败')
  } finally {
    exporting.value = false
  }
}

async function handleExportAll() {
  exporting.value = true
  try {
    const blob = await exportAdminTheoryChallengesApi(undefined)
    triggerFileDownload(blob, `theory_challenges_all_${Date.now()}.json`)
    message.success('全部理论题目导出成功')
  } catch (err) {
    console.error('Theory export all failed:', err)
    message.error('理论题目导出失败')
  } finally {
    exporting.value = false
  }
}

// More Actions Dropdown Options
const moreActionsOptions = computed<DropdownOption[]>(() => {
  const options: DropdownOption[] = [
    {
      label: '导入题目 (JSON/ZIP)',
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

  return options
})

function handleMoreActionSelect(key: string | number) {
  if (key === 'import') {
    openImportModal()
  } else if (key === 'export') {
    handleExport()
  } else if (key === 'export-all') {
    handleExportAll()
  }
}

async function handleExportSingle(row: TheoryChallengeListVO) {
  try {
    const blob = await exportAdminTheoryChallengesApi([row.id])
    triggerFileDownload(blob, `${row.name.replace(/\s+/g, '_')}.json`)
    message.success(`已导出题目 "${row.name}"`)
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
  importResult.value = null
  importing.value = false
  showImportModal.value = true
}

function resetImportModal() {
  importResult.value = null
  importing.value = false
}

function closeImportModal() {
  showImportModal.value = false
  if (importResult.value) {
    fetchChallenges()
  }
  importResult.value = null
}

async function handleCustomImportRequest(options: UploadCustomRequestOptions) {
  const file = options.file.file
  if (!file) return
  importing.value = true
  importResult.value = null
  try {
    const uploadForm = new FormData()
    uploadForm.append('file', file)
    const res = await importAdminTheoryChallengesApi(uploadForm)
    if (res.success && res.data) {
      importResult.value = res.data
      if (res.data.successCount > 0) {
        message.success(`成功导入 ${res.data.successCount} 道理论题目！`)
      } else {
        message.warning('导入完成，但未成功导入任何题目，请查看详细错误信息。')
      }
    }
  } catch (err) {
    console.error('Import theory challenge failed:', err)
    message.error('导入异常，请检查 JSON 格式')
  } finally {
    importing.value = false
  }
}

onMounted(() => {
  fetchChallenges()
})
</script>

<style scoped lang="scss">
.admin-theory-challenge-view {
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

.theory-filter-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;

  .filter-select-type {
    width: 140px;
    flex-shrink: 0;
  }

  .filter-input-category {
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

.theory-filter-right {
  display: flex;
  align-items: center;
  gap: 10px;

  .dropdown-chevron-icon {
    font-size: 13px;
    margin-left: 4px;
    opacity: 0.8;
  }
}

// 移动端响应式与视口自适应 (Mobile & Phone Viewport Optimization)
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

  .theory-filter-left {
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: 10px;

    .filter-select-type,
    .filter-input-category,
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

  .theory-filter-right {
    width: 100%;
    display: flex;
    gap: 10px;

    :deep(.n-button) {
      flex: 1;
      justify-content: center;
    }
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

.theory-title-cell {
  display: flex;
  align-items: center;

  .title-text {
    font-weight: 500;
    color: var(--v-text-primary, #222222);
  }
}

.table-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.form-row-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.options-builder-box {
  border: 1px solid var(--v-border-color, #e5e7eb);
  border-radius: 8px;
  padding: 14px;
  margin-bottom: 16px;
  background-color: var(--v-bg-secondary, #fafafa);

  .options-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 10px;

    .options-title {
      font-size: 13px;
      font-weight: 600;
      color: var(--v-text-primary, #222222);
    }
  }

  .option-items-list {
    display: flex;
    flex-direction: column;
    gap: 8px;

    .option-row {
      display: flex;
      align-items: center;
      gap: 10px;

      .option-tag {
        font-family: 'Fira Code', monospace;
        font-weight: 700;
        font-size: 14px;
        width: 20px;
        text-align: center;
      }
    }
  }
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* Detail Drawer */
.detail-content {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .detail-header {
    border-bottom: 1px solid var(--v-border-color, #ebebeb);
    padding-bottom: 12px;

    .detail-title {
      font-size: 18px;
      font-weight: 600;
      margin: 0 0 8px 0;
      color: var(--v-text-primary, #111111);
    }

    .detail-tags {
      display: flex;
      align-items: center;
      gap: 8px;

      .detail-score {
        font-weight: 700;
        font-size: 14px;
        color: #f59e0b;
        margin-left: auto;
      }
    }
  }

  .detail-section {
    display: flex;
    flex-direction: column;
    gap: 6px;

    .section-label {
      font-size: 13px;
      font-weight: 600;
      color: #444444;
    }

    .content-box {
      font-size: 14px;
      line-height: 1.6;
      padding: 10px 12px;
      background: var(--v-bg-secondary, #f8f9fa);
      border-radius: 6px;
      white-space: pre-wrap;
    }

    .options-display {
      display: flex;
      flex-direction: column;
      gap: 6px;

      .opt-item {
        display: flex;
        align-items: center;
        padding: 8px 12px;
        border-radius: 6px;
        background: var(--v-bg-secondary, #f8f9fa);
        border: 1px solid var(--v-border-color, #eeeeee);

        &.is-correct {
          background: #f0fdf4;
          border-color: #86efac;
        }

        .opt-key {
          font-family: 'Fira Code', monospace;
          font-weight: 700;
          margin-right: 8px;
        }

        .opt-text {
          font-size: 13px;
        }
      }
    }

    .answer-box {
      .answer-badge {
        display: inline-block;
        font-family: 'Fira Code', monospace;
        font-weight: 700;
        font-size: 16px;
        color: #16a34a;
        background: #dcfce7;
        padding: 4px 12px;
        border-radius: 4px;
      }
    }

    .explanation-box {
      font-size: 13px;
      line-height: 1.6;
      color: #333333;
      padding: 10px 12px;
      background: #eff6ff;
      border-left: 3px solid #3b82f6;
      border-radius: 4px;
    }
  }

  .detail-meta {
    margin-top: 20px;
    padding-top: 12px;
    border-top: 1px solid var(--v-border-color, #f0f0f0);
    font-size: 12px;
    color: #888888;
    display: flex;
    flex-direction: column;
    gap: 4px;
  }
}

/* Import dialog */
.import-dialog-content {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.upload-dropzone {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 30px 20px;

  .upload-text {
    margin-top: 12px;
    font-size: 15px;
    font-weight: 500;
  }

  .upload-hint {
    font-size: 12px;
    color: #888888;
    margin-top: 6px;
  }
}

.import-loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;

  .loading-text {
    margin-top: 16px;
    font-size: 13px;
    color: #555555;
  }
}

.import-result-view {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .result-stats-ribbon {
    display: grid;
    grid-template-columns: 1fr 1fr 1fr;
    gap: 12px;

    .res-stat-card {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 14px;
      border-radius: 8px;
      background: var(--v-bg-secondary, #f8f9fa);
      border: 1px solid var(--v-border-color, #ebebeb);

      .num {
        font-size: 24px;
        font-weight: 700;
        line-height: 1.2;
      }

      .label {
        font-size: 12px;
        color: #444444;
        margin-top: 4px;
      }

      &.success .num {
        color: #16a34a;
      }

      &.failed .num {
        color: #dc2626;
      }

      &.total .num {
        color: #2563eb;
      }
    }
  }

  .errors-box {
    border: 1px solid #fecaca;
    background: #fef2f2;
    border-radius: 8px;
    padding: 12px 14px;

    .sub-title {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;
      font-weight: 600;
      color: #991b1b;
      margin-bottom: 6px;
    }

    .error-items {
      margin: 0;
      padding-left: 18px;
      font-size: 12px;
      color: #b91c1c;
      line-height: 1.5;
    }
  }
}
</style>
