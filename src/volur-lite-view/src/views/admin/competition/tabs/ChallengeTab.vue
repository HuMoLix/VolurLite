<template>
  <div class="challenge-tab">
    <!-- Mounted Challenges Table Card -->
    <AppTableCard
      :bordered="true"
      :table-bordered="true"
      :columns="columns"
      :data="mountedChallenges"
      :loading="loading"
      :scroll-x="960"
      :row-key="(row) => row.id"
      table-class="mounted-challenges-table"
    >
      <template #toolbar>
        <div class="tab-top-actions">
          <div class="actions-left">
            <div class="stats-row">
              <div class="stat-pill">
                <span class="label">已挂载赛题：</span>
                <span class="value">{{ mountedChallenges.length }} 题</span>
              </div>
              <div class="stat-pill">
                <span class="label">总初始分值：</span>
                <span class="value">{{ totalInitialPoints }} pts</span>
              </div>
              <div class="stat-pill">
                <span class="label">累计解出人次：</span>
                <span class="value">{{ totalSolves }} 次</span>
              </div>
            </div>
          </div>
          <div class="actions-right">
            <n-button quaternary size="small" :loading="loading" @click="fetchMountedChallenges">
              <template #icon>
                <n-icon :component="RefreshOutline" />
              </template>
              刷新
            </n-button>
            <n-button class="theme-primary-btn" size="small" @click="openImportModal">
              <template #icon>
                <n-icon :component="AddOutline" />
              </template>
              从题库导入赛题
            </n-button>
          </div>
        </div>
      </template>
      <template #empty>
        <div style="padding: 24px; text-align: center; color: #888;">
          暂无挂载赛题，请点击右上角「从题库导入赛题」进行挂载
        </div>
      </template>
    </AppTableCard>

    <!-- Import Challenges Modal -->
    <n-modal
      v-model:show="showImportModal"
      preset="card"
      title="从公共题库导入赛题"
      style="width: 860px; max-height: 85vh;"
      :mask-closable="false"
    >
      <div class="import-modal-container">
        <!-- Filter Controls -->
        <div class="bank-filter-bar">
          <n-select
            v-model:value="filterCategory"
            :options="categoryOptions"
            placeholder="按赛题分类筛选"
            clearable
            style="width: 180px;"
            @update:value="fetchBankChallenges"
          />
          <n-input
            v-model:value="searchKeyword"
            placeholder="搜索题库题目名称..."
            clearable
            style="width: 240px;"
            @keyup.enter="fetchBankChallenges"
          />
          <n-button size="small" @click="fetchBankChallenges">查询</n-button>
        </div>

        <!-- Default Scoring Batch Settings -->
        <n-card class="batch-scoring-card" size="small" title="挂载分值策略默认参数" :bordered="false">
          <n-grid :x-gap="16" :y-gap="8" cols="2 s:4" responsive="screen">
            <n-grid-item>
              <div class="score-input-item">
                <span class="label">初始分 (pts)</span>
                <n-input-number v-model:value="batchScoring.initialPoints" :min="10" :max="10000" size="small" />
              </div>
            </n-grid-item>
            <n-grid-item>
              <div class="score-input-item">
                <span class="label">保底分 (pts)</span>
                <n-input-number v-model:value="batchScoring.minPoints" :min="10" :max="5000" size="small" />
              </div>
            </n-grid-item>
            <n-grid-item>
              <div class="score-input-item">
                <span class="label">衰减解出数</span>
                <n-input-number v-model:value="batchScoring.decayCount" :min="1" :max="500" size="small" />
              </div>
            </n-grid-item>
            <n-grid-item>
              <div class="score-input-item">
                <span class="label">排序权重</span>
                <n-input-number v-model:value="batchScoring.sortOrder" :min="0" :max="9999" size="small" />
              </div>
            </n-grid-item>
          </n-grid>
        </n-card>

        <!-- Bank Challenge Table with Selection -->
        <n-spin :show="bankLoading">
          <n-data-table
            v-model:checked-row-keys="selectedBankKeys"
            :columns="bankColumns"
            :data="bankChallenges"
            :bordered="false"
            :row-key="(row) => row.id"
            :pagination="bankPagination"
            max-height="320"
            empty-text="暂无匹配题库赛题"
          />
        </n-spin>

        <!-- Footer -->
        <div class="modal-actions">
          <span class="selected-hint">已选中 {{ selectedBankKeys.length }} 道题目</span>
          <div class="btn-group">
            <n-button @click="showImportModal = false">取消</n-button>
            <n-button
              class="theme-primary-btn"
              :loading="importing"
              :disabled="selectedBankKeys.length === 0"
              @click="handleConfirmImport"
            >
              确认批量挂载
            </n-button>
          </div>
        </div>
      </div>
    </n-modal>

    <!-- Edit Mounted Challenge Config Modal -->
    <n-modal
      v-model:show="showConfigModal"
      preset="card"
      :title="`配置赛题参数 - ${editingChallenge?.name || ''}`"
      style="width: 520px;"
    >
      <div v-if="editingChallenge" class="config-modal-form">
        <n-form label-placement="top">
          <n-grid :x-gap="16" cols="2">
            <n-grid-item>
              <n-form-item label="初始基准分 (Initial Points)">
                <n-input-number v-model:value="configForm.initialPoints" :min="10" :max="10000" style="width: 100%;" />
              </n-form-item>
            </n-grid-item>
            <n-grid-item>
              <n-form-item label="保底分 (Min Points)">
                <n-input-number v-model:value="configForm.minPoints" :min="10" :max="5000" style="width: 100%;" />
              </n-form-item>
            </n-grid-item>
            <n-grid-item>
              <n-form-item label="衰减梯度解出次数">
                <n-input-number v-model:value="configForm.decayCount" :min="1" :max="500" style="width: 100%;" />
              </n-form-item>
            </n-grid-item>
            <n-grid-item>
              <n-form-item label="赛区排序权重">
                <n-input-number v-model:value="configForm.sortOrder" :min="0" :max="9999" style="width: 100%;" />
              </n-form-item>
            </n-grid-item>
          </n-grid>
          <div class="modal-actions">
            <n-button @click="showConfigModal = false">取消</n-button>
            <n-button class="theme-primary-btn" :loading="configSubmitting" @click="handleSaveChallengeConfig">
              保存配置
            </n-button>
          </div>
        </n-form>
      </div>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, h, onMounted } from 'vue'
import {
  NButton,
  NIcon,
  NSpin,
  NDataTable,
  NModal,
  NCard,
  NForm,
  NFormItem,
  NInput,
  NInputNumber,
  NSelect,
  NTag,
  NGrid,
  NGridItem,
  useMessage,
  useDialog,
  type DataTableColumns
} from 'naive-ui'
import AppTableCard from '@/components/common/AppTableCard.vue'
import {
  RefreshOutline,
  AddOutline
} from '@vicons/ionicons5'
import type {
  CtfCompetitionChallengeVO,
  CtfChallengeMountRequestDTO,
  CtfChallengeBatchMountRequestDTO
} from '@/types/competition'
import type {
  CtfChallengeListVO,
  CtfCategoryVO
} from '@/types/challenge'
import {
  listMountedCtfChallengesApi,
  batchMountCtfChallengesApi,
  updateMountedCtfChallengeApi,
  unmountCtfChallengeApi
} from '@/api/competition'
import {
  getAdminCtfChallengeListApi,
  getAdminCtfCategoriesApi
} from '@/api/challenge'

interface Props {
  compId: string | number
}

const props = defineProps<Props>()
const message = useMessage()
const dialog = useDialog()

// Mounted State
const loading = ref<boolean>(false)
const mountedChallenges = ref<CtfCompetitionChallengeVO[]>([])

// Import Modal State
const showImportModal = ref<boolean>(false)
const bankLoading = ref<boolean>(false)
const importing = ref<boolean>(false)
const bankChallenges = ref<CtfChallengeListVO[]>([])
const selectedBankKeys = ref<Array<string | number>>([])
const categories = ref<CtfCategoryVO[]>([])
const filterCategory = ref<number | string | null>(null)
const searchKeyword = ref<string>('')

const batchScoring = reactive({
  initialPoints: 1000,
  minPoints: 100,
  decayCount: 30,
  sortOrder: 0
})

const bankPagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  onChange: (page: number) => {
    bankPagination.page = page
    fetchBankChallenges()
  }
})

// Config Modal State
const showConfigModal = ref<boolean>(false)
const configSubmitting = ref<boolean>(false)
const editingChallenge = ref<CtfCompetitionChallengeVO | null>(null)
const configForm = reactive({
  initialPoints: 1000,
  minPoints: 100,
  decayCount: 30,
  sortOrder: 0
})

// Computed
const totalInitialPoints = computed(() => {
  return mountedChallenges.value.reduce((acc, c) => acc + (c.initialPoints || 0), 0)
})

const totalSolves = computed(() => {
  return mountedChallenges.value.reduce((acc, c) => acc + (c.solvesCount || 0), 0)
})

const categoryOptions = computed(() => {
  return categories.value.map(c => ({
    label: c.name,
    value: c.id
  }))
})

// Table Columns for Mounted Challenges
const columns: DataTableColumns<CtfCompetitionChallengeVO> = [
  {
    title: '排序',
    key: 'sortOrder',
    width: 70,
    render(row) {
      return h('span', { style: 'color: #888; font-weight: 500;' }, row.sortOrder ?? 0)
    }
  },
  {
    title: '题目名称',
    key: 'name',
    render(row) {
      return h('div', { style: 'display: flex; align-items: center; gap: 8px;' }, [
        h(NTag, { size: 'small', type: 'info', bordered: false }, { default: () => row.categoryName || 'CTF' }),
        h('span', { style: 'font-weight: 600; color: var(--v-text-primary, #111);' }, row.name)
      ])
    }
  },
  {
    title: '难度',
    key: 'difficulty',
    width: 100,
    render(row) {
      const diff = typeof row.difficulty === 'number' ? row.difficulty : Number(row.difficulty) || 1
      const type = diff <= 2 ? 'success' : diff <= 4 ? 'warning' : 'error'
      return h(NTag, { size: 'small', type, round: true }, { default: () => `Lv.${diff}` })
    }
  },
  {
    title: '初始基准分',
    key: 'initialPoints',
    width: 110,
    render(row) {
      return `${row.initialPoints ?? 1000} pts`
    }
  },
  {
    title: '当前分值 (衰减)',
    key: 'currentPoints',
    width: 130,
    render(row) {
      return h('span', { style: 'font-weight: 600; color: #18a058;' }, `${row.currentPoints ?? row.initialPoints ?? 1000} pts`)
    }
  },
  {
    title: '解出队伍数',
    key: 'solvesCount',
    width: 110,
    render(row) {
      return `${row.solvesCount ?? 0} 解`
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 160,
    fixed: 'right',
    render(row) {
      return h('div', { style: 'display: flex; gap: 8px;' }, [
        h(
          NButton,
          {
            size: 'tiny',
            quaternary: true,
            onClick: () => openEditConfig(row)
          },
          {
            default: () => '配置'
          }
        ),
        h(
          NButton,
          {
            size: 'tiny',
            quaternary: true,
            type: 'error',
            onClick: () => handleUnmount(row)
          },
          {
            default: () => '卸载'
          }
        )
      ])
    }
  }
]

// Table Columns for Question Bank
const bankColumns: DataTableColumns<CtfChallengeListVO> = [
  {
    type: 'selection'
  },
  {
    title: '题目名称',
    key: 'name',
    render(row) {
      return h('div', { style: 'display: flex; align-items: center; gap: 8px;' }, [
        h(NTag, { size: 'small', type: 'info', bordered: false }, { default: () => row.categoryName || 'Misc' }),
        h('span', { style: 'font-weight: 500;' }, row.name)
      ])
    }
  },
  {
    title: '题型',
    key: 'type',
    width: 120,
    render(row) {
      return row.type === 'DYNAMIC_CONTAINER' ? '动态独立容器' : '静态题目'
    }
  },
  {
    title: '难度',
    key: 'difficulty',
    width: 90,
    render(row) {
      return `Lv.${row.difficulty || 1}`
    }
  }
]

onMounted(() => {
  if (props.compId) {
    fetchMountedChallenges()
  }
})

async function fetchMountedChallenges() {
  loading.value = true
  try {
    const res = await listMountedCtfChallengesApi(props.compId)
    mountedChallenges.value = res.data || []
  } catch (error: any) {
    console.error('Fetch mounted challenges error:', error)
    message.error(error.message || '获取比赛赛题失败')
  } finally {
    loading.value = false
  }
}

async function openImportModal() {
  showImportModal.value = true
  selectedBankKeys.value = []
  if (categories.value.length === 0) {
    try {
      const catRes = await getAdminCtfCategoriesApi()
      categories.value = catRes.data || []
    } catch (e) {
      console.error(e)
    }
  }
  fetchBankChallenges()
}

async function fetchBankChallenges() {
  bankLoading.value = true
  try {
    const res = await getAdminCtfChallengeListApi({
      page: bankPagination.page,
      size: bankPagination.pageSize,
      categoryId: filterCategory.value ? filterCategory.value : undefined,
      keyword: searchKeyword.value.trim() ? searchKeyword.value.trim() : undefined
    })
    bankChallenges.value = res.data?.list || []
    bankPagination.itemCount = res.data?.total || 0
  } catch (error: any) {
    console.error('Fetch bank challenges error:', error)
  } finally {
    bankLoading.value = false
  }
}

async function handleConfirmImport() {
  if (selectedBankKeys.value.length === 0) return

  importing.value = true
  try {
    const items: CtfChallengeMountRequestDTO[] = selectedBankKeys.value.map(id => ({
      challengeId: Number(id),
      initialPoints: batchScoring.initialPoints,
      minPoints: batchScoring.minPoints,
      decayCount: batchScoring.decayCount,
      sortOrder: batchScoring.sortOrder,
      isVisible: true
    }))

    const batchDTO: CtfChallengeBatchMountRequestDTO = { items }
    await batchMountCtfChallengesApi(props.compId, batchDTO)
    message.success(`成功挂载 ${items.length} 道赛题到竞赛`)
    showImportModal.value = false
    fetchMountedChallenges()
  } catch (error: any) {
    console.error('Batch mount error:', error)
    message.error(error.message || '挂载赛题失败')
  } finally {
    importing.value = false
  }
}

function openEditConfig(row: CtfCompetitionChallengeVO) {
  editingChallenge.value = row
  configForm.initialPoints = row.initialPoints || 1000
  configForm.minPoints = 100
  configForm.decayCount = 30
  configForm.sortOrder = row.sortOrder || 0
  showConfigModal.value = true
}

async function handleSaveChallengeConfig() {
  if (!editingChallenge.value) return
  configSubmitting.value = true
  try {
    const dto: CtfChallengeMountRequestDTO = {
      challengeId: editingChallenge.value.challengeId,
      initialPoints: configForm.initialPoints,
      minPoints: configForm.minPoints,
      decayCount: configForm.decayCount,
      sortOrder: configForm.sortOrder,
      isVisible: true
    }
    await updateMountedCtfChallengeApi(props.compId, editingChallenge.value.id, dto)
    message.success('赛题配置保存成功')
    showConfigModal.value = false
    fetchMountedChallenges()
  } catch (error: any) {
    message.error(error.message || '更新赛题配置失败')
  } finally {
    configSubmitting.value = false
  }
}

function handleUnmount(row: CtfCompetitionChallengeVO) {
  dialog.warning({
    title: '确认卸载赛题',
    content: `确定从当前比赛中卸载赛题「${row.name}」吗？卸载后参赛选手的解题记录也将从本赛题剥离。`,
    positiveText: '确认卸载',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await unmountCtfChallengeApi(props.compId, row.id)
        message.success('赛题已卸载')
        fetchMountedChallenges()
      } catch (error: any) {
        message.error(error.message || '卸载赛题失败')
      }
    }
  })
}
</script>

<style scoped lang="scss">
.challenge-tab {
  .tab-top-actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 16px;
    margin-bottom: 20px;
    border-bottom: 1px solid var(--v-border-base, #e5e4e7);

    .stats-row {
      display: flex;
      align-items: center;
      gap: 12px;

      .stat-pill {
        background: var(--v-bg-muted, #f8f8f9);
        padding: 6px 14px;
        border-radius: 6px;
        font-size: 13px;

        .label {
          color: #444444;
        }

        .value {
          font-weight: 600;
          color: var(--v-text-primary, #111111);
        }
      }
    }

    .actions-right {
      display: flex;
      align-items: center;
      gap: 12px;
    }
  }

  .import-modal-container {
    display: flex;
    flex-direction: column;
    gap: 16px;

    .bank-filter-bar {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .batch-scoring-card {
      background: var(--v-bg-muted, #f8f8f9);

      .score-input-item {
        display: flex;
        flex-direction: column;
        gap: 4px;

        .label {
          font-size: 12px;
          color: #444444;
        }
      }
    }

    .modal-actions {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-top: 10px;

      .selected-hint {
        font-size: 13px;
        color: var(--v-color-primary, #18a058);
        font-weight: 500;
      }

      .btn-group {
        display: flex;
        gap: 12px;
      }
    }
  }

  .config-modal-form {
    .modal-actions {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
      margin-top: 20px;
    }
  }
}
</style>
