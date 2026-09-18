<template>
  <div class="admin-broadcast-view">
    <!-- Top section: Compact Metrics Statistics Cards in a single row -->
    <div class="stat-ribbon">
      <div class="stat-mini-card">
        <div class="stat-icon-wrapper primary">
          <n-icon size="18">
            <MegaphoneOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statData.totalAnnouncements }}</div>
          <div class="stat-label">{{ t('admin.broadcasts.stat_announcements') }}</div>
        </div>
      </div>

      <div class="stat-mini-card">
        <div class="stat-icon-wrapper warning">
          <n-icon size="18">
            <FlameOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statData.totalPromotions }}</div>
          <div class="stat-label">{{ t('admin.broadcasts.stat_promotions') }}</div>
        </div>
      </div>

      <div class="stat-mini-card">
        <div class="stat-icon-wrapper error">
          <n-icon size="18">
            <AlertCircleOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statData.urgentModals }}</div>
          <div class="stat-label">{{ t('admin.broadcasts.stat_urgent_modals') }}</div>
        </div>
      </div>

      <div class="stat-mini-card">
        <div class="stat-icon-wrapper success">
          <n-icon size="18">
            <RadioOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value status-badge-text">
            {{ statData.sseOnlineStatus !== false ? t('admin.broadcasts.sse_online') : t('admin.broadcasts.sse_offline') }}
          </div>
          <div class="stat-label">{{ t('admin.broadcasts.stat_sse_bus') }}</div>
        </div>
      </div>
    </div>

    <!-- Bottom section: Broadcast History Table Card -->
    <VolurTableCard
      :bordered="true"
      :table-bordered="true"
      :scroll-x="1360"
      remote
      :columns="columns"
      :data="broadcastList"
      :loading="loading"
      :pagination="pagination"
      :row-key="(row: BroadcastMessageVO) => String(row.id)"
      table-class="broadcast-data-table"
      @update:sorter="handleSorterChange"
      @update:page="handlePageChange"
      @update:page-size="handlePageSizeChange"
    >
      <template #filter-left>
        <div class="broadcast-filter-left">
          <n-input
            v-model:value="searchKeyword"
            :placeholder="t('admin.broadcasts.search_placeholder')"
            clearable
            class="filter-input-search"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <n-icon :component="SearchOutline" />
            </template>
          </n-input>

          <n-select
            v-model:value="filterCategory"
            :placeholder="t('admin.broadcasts.filter_category')"
            clearable
            :options="categoryOptions"
            class="filter-select-category"
            @update:value="handleSearch"
          />

          <n-select
            v-model:value="filterDisplayType"
            :placeholder="t('admin.broadcasts.filter_display_type')"
            clearable
            :options="displayTypeOptions"
            class="filter-select-type"
            @update:value="handleSearch"
          />

          <div class="filter-action-buttons">
            <n-button type="primary" @click="handleSearch" class="theme-primary-btn">
              <template #icon>
                <n-icon :component="SearchOutline" />
              </template>
              {{ t('admin.broadcasts.query_btn') }}
            </n-button>

            <n-button quaternary @click="handleReset">
              <template #icon>
                <n-icon :component="RefreshOutline" />
              </template>
              {{ t('admin.broadcasts.reset_btn') }}
            </n-button>
          </div>
        </div>
      </template>

      <template #filter-right>
        <div class="broadcast-filter-right">
          <n-button type="primary" class="theme-primary-btn action-create-btn" @click="openCreateModal('ANNOUNCEMENT')">
            <template #icon>
              <n-icon :component="MegaphoneOutline" />
            </template>
            {{ t('admin.broadcasts.create_announcement_btn') }}
          </n-button>

          <n-button secondary class="action-promo-btn" @click="openCreateModal('PROMOTION')">
            <template #icon>
              <n-icon :component="AddOutline" />
            </template>
            {{ t('admin.broadcasts.create_promotion_btn') }}
          </n-button>
        </div>
      </template>
    </VolurTableCard>

    <!-- Create & Dispatch Broadcast Modal -->
    <n-modal
      v-model:show="showCreateModal"
      preset="card"
      :title="modalCategory === 'ANNOUNCEMENT' ? t('admin.broadcasts.modal.title_announcement') : t('admin.broadcasts.modal.title_promotion')"
      style="width: 560px;"
      :mask-closable="false"
    >
      <n-form
        ref="broadcastFormRef"
        :model="formData"
        :rules="formRules"
        label-placement="left"
        label-width="95"
        require-mark-placement="right-hanging"
      >
        <n-form-item :label="t('admin.broadcasts.modal.field_title')" path="title">
          <n-input
            v-model:value="formData.title"
            :placeholder="t('admin.broadcasts.modal.placeholder_title')"
          />
        </n-form-item>

        <n-form-item :label="t('admin.broadcasts.modal.field_display_type')" path="displayType">
          <n-select
            v-model:value="formData.displayType"
            :options="modalDisplayTypeOptions"
            :placeholder="t('admin.broadcasts.modal.placeholder_display_type')"
          />
        </n-form-item>

        <n-form-item :label="t('admin.broadcasts.modal.field_content')" path="content">
          <n-input
            v-model:value="formData.content"
            type="textarea"
            :rows="4"
            :placeholder="t('admin.broadcasts.modal.placeholder_content')"
          />
        </n-form-item>

        <n-form-item :label="t('admin.broadcasts.modal.field_link_url')" path="linkUrl">
          <n-input
            v-model:value="formData.linkUrl"
            :placeholder="t('admin.broadcasts.modal.placeholder_link_url')"
          />
        </n-form-item>

        <n-form-item :label="t('admin.broadcasts.modal.field_cover_image')" path="coverImageUrl">
          <n-input
            v-model:value="formData.coverImageUrl"
            :placeholder="t('admin.broadcasts.modal.placeholder_cover_image')"
          />
        </n-form-item>

        <n-form-item :label="t('admin.broadcasts.modal.field_priority')" path="priority">
          <n-input-number
            v-model:value="formData.priority"
            :min="1"
            :max="10"
            style="width: 100%;"
            :placeholder="t('admin.broadcasts.modal.placeholder_priority')"
          />
        </n-form-item>

        <n-form-item :label="t('admin.broadcasts.modal.field_force_popup')" path="forcePopup">
          <div class="switch-row">
            <n-switch v-model:value="formData.forcePopup" />
            <span class="field-hint">
              {{ formData.forcePopup ? t('admin.broadcasts.modal.force_popup_checked') : t('admin.broadcasts.modal.force_popup_unchecked') }}
            </span>
          </div>
        </n-form-item>
      </n-form>

      <template #footer>
        <div class="modal-footer">
          <n-button @click="showCreateModal = false">{{ t('admin.broadcasts.modal.cancel') }}</n-button>
          <n-button
            type="primary"
            :loading="submitLoading"
            class="theme-primary-btn"
            @click="handleSubmitBroadcast"
          >
            {{ t('admin.broadcasts.modal.submit') }}
          </n-button>
        </div>
      </template>
    </n-modal>

    <!-- Preview Broadcast Detail Modal -->
    <n-modal
      v-model:show="showPreviewModal"
      preset="card"
      :title="t('admin.broadcasts.preview.modal_title')"
      style="width: 520px;"
    >
      <div v-if="previewItem" class="preview-container">
        <div class="preview-header">
          <div class="preview-title">{{ previewItem.title }}</div>
          <div class="preview-meta">
            <n-tag
              :type="previewItem.category === 'ANNOUNCEMENT' ? 'primary' : 'warning'"
              size="small"
              round
            >
              {{ previewItem.category === 'ANNOUNCEMENT' ? t('admin.broadcasts.category_announcement') : t('admin.broadcasts.category_promotion') }}
            </n-tag>
            <n-tag size="small" round>
              {{ getDisplayTypeLabel(previewItem.displayType) }}
            </n-tag>
            <span class="preview-priority">P{{ previewItem.priority || 0 }}</span>
            <span class="preview-time">{{ formatTime(previewItem.createdAt) }}</span>
          </div>
        </div>

        <div v-if="previewItem.coverImageUrl" class="preview-cover">
          <img :src="previewItem.coverImageUrl" alt="cover" />
        </div>

        <div class="preview-content">
          {{ previewItem.content }}
        </div>

        <div v-if="previewItem.linkUrl" class="preview-link">
          <span class="link-label">{{ t('admin.broadcasts.preview.link_label') }}</span>
          <a :href="previewItem.linkUrl" target="_blank" rel="noopener noreferrer">
            {{ previewItem.linkUrl }}
          </a>
        </div>
      </div>
      <template #footer>
        <div class="modal-footer">
          <n-button @click="showPreviewModal = false">{{ t('admin.broadcasts.preview.close') }}</n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, h, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  NIcon,
  NInput,
  NSelect,
  NButton,
  NModal,
  NForm,
  NFormItem,
  NSwitch,
  NTag,
  NInputNumber,
  NSpace,
  NPopconfirm,
  useMessage,
  type DataTableColumns,
  type FormInst,
  type FormRules,
  type SelectOption
} from 'naive-ui'
import { VolurTableCard } from '@/public/volurEx'
import {
  MegaphoneOutline,
  FlameOutline,
  AlertCircleOutline,
  RadioOutline,
  SearchOutline,
  RefreshOutline,
  AddOutline,
  EyeOutline,
  TrashOutline
} from '@vicons/ionicons5'
import {
  getAdminBroadcastStatisticsApi,
  getAdminBroadcastListApi,
  publishAnnouncementApi,
  publishPromotionApi,
  deleteAdminBroadcastApi
} from '@/api/notification'
import type {
  BroadcastMessageVO,
  BroadcastMessageDTO,
  BroadcastDisplayType,
  AdminBroadcastPageRequestDTO,
  AdminBroadcastStatisticsVO
} from '@/types/notification'

const { t, locale } = useI18n()
const message = useMessage()

// Metric statistics State
const statData = reactive<AdminBroadcastStatisticsVO>({
  totalAnnouncements: 0,
  totalPromotions: 0,
  urgentModals: 0,
  sseOnlineStatus: true
})

// Query & Filter State
const searchKeyword = ref('')
const filterCategory = ref<string | null>(null)
const filterDisplayType = ref<string | null>(null)
const sortBy = ref<string>('createdAt')
const sortOrder = ref<'asc' | 'desc'>('desc')

const categoryOptions = computed<SelectOption[]>(() => [
  { label: t('admin.broadcasts.category_all'), value: '' },
  { label: t('admin.broadcasts.category_announcement'), value: 'ANNOUNCEMENT' },
  { label: t('admin.broadcasts.category_promotion'), value: 'PROMOTION' }
])

const displayTypeOptions = computed<SelectOption[]>(() => [
  { label: t('admin.broadcasts.display_type_all'), value: '' },
  { label: t('admin.broadcasts.display_type_modal_full'), value: 'MODAL' },
  { label: t('admin.broadcasts.display_type_banner_full'), value: 'BANNER' },
  { label: t('admin.broadcasts.display_type_toast_full'), value: 'TOAST' },
  { label: t('admin.broadcasts.display_type_drawer_full'), value: 'DRAWER' }
])

const modalDisplayTypeOptions = computed<SelectOption[]>(() => [
  { label: t('admin.broadcasts.display_type_modal_full'), value: 'MODAL' },
  { label: t('admin.broadcasts.display_type_banner_full'), value: 'BANNER' },
  { label: t('admin.broadcasts.display_type_toast_full'), value: 'TOAST' },
  { label: t('admin.broadcasts.display_type_drawer_full'), value: 'DRAWER' }
])

// Table & Pagination State
const loading = ref(false)
const broadcastList = ref<BroadcastMessageVO[]>([])
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  showQuickJumper: true,
  pageSizes: [10, 20, 50],
  prefix: (info: { itemCount?: number }) => t('admin.broadcasts.total_prefix', { count: info.itemCount || 0 })
})

// Preview State
const showPreviewModal = ref(false)
const previewItem = ref<BroadcastMessageVO | null>(null)

function formatTime(timeStr?: string) {
  if (!timeStr) return '-'
  const date = new Date(timeStr)
  return date.toLocaleString(locale.value === 'zh_CN' ? 'zh-CN' : 'en-US', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function getDisplayTypeLabel(type: BroadcastDisplayType) {
  const map: Record<string, string> = {
    MODAL: t('admin.broadcasts.display_type_modal'),
    BANNER: t('admin.broadcasts.display_type_banner'),
    TOAST: t('admin.broadcasts.display_type_toast'),
    DRAWER: t('admin.broadcasts.display_type_drawer')
  }
  return map[type] || t('admin.broadcasts.display_type_default')
}

// Table Columns Definition
const columns = computed<DataTableColumns<BroadcastMessageVO>>(() => [
  {
    title: t('admin.broadcasts.columns.id'),
    key: 'id',
    width: 180,
    sorter: true,
    render(row) {
      return h('span', { class: 'id-text' }, String(row.id || '-'))
    }
  },
  {
    title: t('admin.broadcasts.columns.title'),
    key: 'title',
    minWidth: 200,
    render(row) {
      return h('div', { class: 'title-cell' }, [
        h('span', { class: 'title-text' }, row.title),
        row.forcePopup ? h(NTag, { type: 'error', size: 'tiny', round: true }, { default: () => t('admin.broadcasts.force_popup_tag') }) : null
      ])
    }
  },
  {
    title: t('admin.broadcasts.columns.category'),
    key: 'category',
    width: 110,
    render(row) {
      const isAnnouncement = row.category === 'ANNOUNCEMENT'
      return h(
        NTag,
        {
          type: isAnnouncement ? 'primary' : 'warning',
          size: 'small',
          round: true
        },
        { default: () => (isAnnouncement ? t('admin.broadcasts.category_announcement') : t('admin.broadcasts.category_promotion')) }
      )
    }
  },
  {
    title: t('admin.broadcasts.columns.display_type'),
    key: 'displayType',
    width: 130,
    render(row) {
      const typeMap: Record<string, { label: string; type: 'info' | 'warning' | 'error' | 'success' | 'default' }> = {
        MODAL: { label: t('admin.broadcasts.display_type_modal'), type: 'error' },
        BANNER: { label: t('admin.broadcasts.display_type_banner'), type: 'warning' },
        TOAST: { label: t('admin.broadcasts.display_type_toast'), type: 'info' },
        DRAWER: { label: t('admin.broadcasts.display_type_drawer'), type: 'success' }
      }
      const item = typeMap[row.displayType] || { label: row.displayType || t('admin.broadcasts.display_type_default'), type: 'default' }
      return h(NTag, { type: item.type, size: 'small', round: true }, { default: () => item.label })
    }
  },
  {
    title: t('admin.broadcasts.columns.content'),
    key: 'content',
    minWidth: 220,
    render(row) {
      return h('span', { class: 'content-snippet', title: row.content }, row.content)
    }
  },
  {
    title: t('admin.broadcasts.columns.priority'),
    key: 'priority',
    width: 90,
    sorter: true,
    render(row) {
      return h('span', { class: 'priority-badge' }, `P${row.priority ?? 0}`)
    }
  },
  {
    title: t('admin.broadcasts.columns.publisher'),
    key: 'publisher',
    width: 120,
    render(row) {
      return row.publisher || t('admin.broadcasts.default_publisher')
    }
  },
  {
    title: t('admin.broadcasts.columns.created_at'),
    key: 'createdAt',
    width: 170,
    sorter: true,
    render(row) {
      return formatTime(row.createdAt)
    }
  },
  {
    title: t('admin.broadcasts.columns.actions'),
    key: 'actions',
    width: 140,
    fixed: 'right',
    render(row) {
      return h(NSpace, { size: 'small' }, {
        default: () => [
          h(
            NButton,
            {
              quaternary: true,
              size: 'tiny',
              type: 'primary',
              onClick: () => handlePreview(row)
            },
            {
              icon: () => h(NIcon, null, { default: () => h(EyeOutline) }),
              default: () => t('admin.broadcasts.actions.preview')
            }
          ),
          h(
            NPopconfirm,
            {
              onPositiveClick: () => handleDeleteBroadcast(row)
            },
            {
              trigger: () =>
                h(
                  NButton,
                  {
                    quaternary: true,
                    size: 'tiny',
                    type: 'error'
                  },
                  {
                    icon: () => h(NIcon, null, { default: () => h(TrashOutline) }),
                    default: () => t('admin.broadcasts.actions.delete')
                  }
                ),
              default: () => t('admin.broadcasts.actions.delete_confirm', { title: row.title })
            }
          )
        ]
      })
    }
  }
])

// Modal & Form State
const showCreateModal = ref(false)
const modalCategory = ref<'ANNOUNCEMENT' | 'PROMOTION'>('ANNOUNCEMENT')
const submitLoading = ref(false)
const broadcastFormRef = ref<FormInst | null>(null)

const formData = reactive<{
  title: string
  content: string
  displayType: BroadcastDisplayType
  linkUrl?: string
  coverImageUrl?: string
  forcePopup: boolean
  priority: number
}>({
  title: '',
  content: '',
  displayType: 'BANNER',
  linkUrl: '',
  coverImageUrl: '',
  forcePopup: false,
  priority: 5
})

const formRules = computed<FormRules>(() => ({
  title: [{ required: true, message: t('admin.broadcasts.modal.rule_title'), trigger: 'blur' }],
  content: [{ required: true, message: t('admin.broadcasts.modal.rule_content'), trigger: 'blur' }],
  displayType: [{ required: true, message: t('admin.broadcasts.modal.rule_display_type'), trigger: 'change' }]
}))

// Data Fetching
async function fetchStatistics() {
  try {
    const res = await getAdminBroadcastStatisticsApi()
    if (res.data) {
      Object.assign(statData, res.data)
    }
  } catch (error) {
    console.error('Failed to fetch broadcast statistics:', error)
  }
}

async function fetchBroadcastList() {
  loading.value = true
  try {
    const params: AdminBroadcastPageRequestDTO = {
      page: pagination.page,
      size: pagination.pageSize,
      keyword: searchKeyword.value.trim() || undefined,
      category: filterCategory.value || undefined,
      displayType: filterDisplayType.value || undefined,
      sortBy: sortBy.value,
      sortOrder: sortOrder.value
    }
    const res = await getAdminBroadcastListApi(params)
    if (res.data) {
      broadcastList.value = res.data.list || []
      pagination.itemCount = res.data.total || 0
    }
  } catch (error) {
    console.error('Failed to fetch admin broadcasts:', error)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchBroadcastList()
}

function handleReset() {
  searchKeyword.value = ''
  filterCategory.value = null
  filterDisplayType.value = null
  sortBy.value = 'createdAt'
  sortOrder.value = 'desc'
  pagination.page = 1
  fetchBroadcastList()
}

function handleSorterChange(sorter: { columnKey: string; order: 'ascend' | 'descend' | false } | null) {
  if (sorter && sorter.order) {
    sortBy.value = sorter.columnKey
    sortOrder.value = sorter.order === 'ascend' ? 'asc' : 'desc'
  } else {
    sortBy.value = 'createdAt'
    sortOrder.value = 'desc'
  }
  fetchBroadcastList()
}

function handlePageChange(page: number) {
  pagination.page = page
  fetchBroadcastList()
}

function handlePageSizeChange(pageSize: number) {
  pagination.pageSize = pageSize
  pagination.page = 1
  fetchBroadcastList()
}

function handlePreview(row: BroadcastMessageVO) {
  previewItem.value = row
  showPreviewModal.value = true
}

async function handleDeleteBroadcast(row: BroadcastMessageVO) {
  try {
    await deleteAdminBroadcastApi(row.id)
    message.success(t('admin.broadcasts.messages.delete_success', { title: row.title }))
    fetchBroadcastList()
    fetchStatistics()
  } catch (error: any) {
    message.error(error.message || t('admin.broadcasts.messages.delete_failed'))
  }
}

function openCreateModal(category: 'ANNOUNCEMENT' | 'PROMOTION') {
  modalCategory.value = category
  formData.title = ''
  formData.content = ''
  formData.displayType = category === 'ANNOUNCEMENT' ? 'BANNER' : 'MODAL'
  formData.linkUrl = ''
  formData.coverImageUrl = ''
  formData.forcePopup = category === 'PROMOTION'
  formData.priority = category === 'ANNOUNCEMENT' ? 8 : 5
  showCreateModal.value = true
}

async function handleSubmitBroadcast() {
  broadcastFormRef.value?.validate(async (errors) => {
    if (errors) return

    submitLoading.value = true
    try {
      const payload: BroadcastMessageDTO = {
        title: formData.title.trim(),
        content: formData.content.trim(),
        displayType: formData.displayType,
        linkUrl: formData.linkUrl?.trim() || undefined,
        coverImageUrl: formData.coverImageUrl?.trim() || undefined,
        forcePopup: formData.forcePopup,
        priority: formData.priority
      }

      if (modalCategory.value === 'ANNOUNCEMENT') {
        await publishAnnouncementApi(payload)
        message.success(t('admin.broadcasts.messages.announce_success'))
      } else {
        await publishPromotionApi(payload)
        message.success(t('admin.broadcasts.messages.promo_success'))
      }

      showCreateModal.value = false
      fetchBroadcastList()
      fetchStatistics()
    } catch (error: any) {
      message.error(error.message || t('admin.broadcasts.messages.push_failed'))
    } finally {
      submitLoading.value = false
    }
  })
}

onMounted(() => {
  fetchStatistics()
  fetchBroadcastList()
})
</script>

<style scoped lang="scss">
.admin-broadcast-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stat-ribbon {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;

  @media (max-width: 992px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 576px) {
    grid-template-columns: 1fr;
  }
}

.stat-mini-card {
  background-color: var(--v-bg-card, #ffffff);
  border-radius: 10px;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  border: 1px solid var(--v-border-base, #e5e4e7);
  box-shadow: var(--v-shadow-sm, 0 1px 2px rgba(0, 0, 0, 0.02));
  transition: background-color 0.3s ease, border-color 0.3s ease, box-shadow 0.2s ease;

  &:hover {
    border-color: var(--v-border-focus, #bbbbbb);
    box-shadow: var(--v-shadow-md, 0 2px 6px rgba(0, 0, 0, 0.05));
  }

  .stat-icon-wrapper {
    width: 38px;
    height: 38px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;

    &.primary {
      background-color: rgba(99, 102, 241, 0.1);
      color: #6366f1;
    }

    &.warning {
      background-color: rgba(245, 158, 11, 0.1);
      color: #f59e0b;
    }

    &.success {
      background-color: rgba(16, 185, 129, 0.1);
      color: #10b981;
    }

    &.error {
      background-color: rgba(239, 68, 68, 0.1);
      color: #ef4444;
    }
  }

  .stat-info {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;

    .stat-value {
      font-size: 18px;
      font-weight: 700;
      color: var(--v-text-primary, #111111);
      line-height: 1.2;

      &.status-badge-text {
        font-size: 15px;
        color: #10b981;
      }
    }

    .stat-label {
      font-size: 12px;
      color: var(--v-text-desc, #444444);
      font-weight: 500;
      white-space: nowrap;
    }
  }
}

.title-cell {
  display: flex;
  align-items: center;
  gap: 8px;

  .title-text {
    font-size: 14px;
    font-weight: 600;
    color: var(--v-text-primary, #111111);
  }
}

.content-snippet {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-size: 13px;
  color: #444444;
  line-height: 1.4;
}

.priority-badge {
  font-family: 'Fira Code', monospace;
  font-weight: 600;
  color: #6366f1;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.preview-container {
  display: flex;
  flex-direction: column;
  gap: 14px;

  .preview-header {
    display: flex;
    flex-direction: column;
    gap: 6px;

    .preview-title {
      font-size: 18px;
      font-weight: 700;
      color: var(--v-text-primary, #111111);
    }

    .preview-meta {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 8px;
      font-size: 12px;

      .preview-priority {
        font-family: 'Fira Code', monospace;
        font-weight: 600;
        color: #6366f1;
      }

      .preview-time {
        color: #888888;
      }
    }
  }

  .preview-cover {
    img {
      width: 100%;
      max-height: 220px;
      object-fit: cover;
      border-radius: 6px;
      border: 1px solid rgba(0, 0, 0, 0.08);
    }
  }

  .preview-content {
    font-size: 14px;
    color: #444444;
    line-height: 1.6;
    white-space: pre-wrap;
    background: rgba(0, 0, 0, 0.02);
    padding: 12px;
    border-radius: 6px;
  }

  .preview-link {
    font-size: 13px;
    display: flex;
    align-items: center;
    gap: 6px;

    .link-label {
      color: #888888;
    }

    a {
      color: #6366f1;
      text-decoration: underline;
      word-break: break-all;
    }
  }
}

.broadcast-filter-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;

  .filter-input-search {
    width: 220px;
    min-width: 160px;
  }

  .filter-select-category,
  .filter-select-type {
    width: 135px;
    flex-shrink: 0;
  }

  .filter-action-buttons {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.broadcast-filter-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

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

  .broadcast-filter-left {
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: 10px;

    .filter-input-search,
    .filter-select-category,
    .filter-select-type {
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

  .broadcast-filter-right {
    width: 100%;
    display: flex;
    gap: 10px;

    :deep(.n-button) {
      flex: 1;
      justify-content: center;
    }
  }
}

.switch-row {
  display: flex;
  align-items: center;

  .field-hint {
    margin-left: 12px;
    font-size: 12px;
    color: #444444;
  }
}
</style>
