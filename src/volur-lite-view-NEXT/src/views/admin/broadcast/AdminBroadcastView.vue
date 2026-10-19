<template>
  <div class="admin-broadcast-view flex flex-col gap-4 sm:gap-6">
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h2 class="text-xl font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
          {{ t('admin.broadcasts.title', '公告管理') }}
        </h2>
        <p class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary mt-1">
          {{ t('admin.broadcasts.desc', '全平台系统广播、活动促销弹窗、横幅通知与 SSE 实时总线调度') }}
        </p>
      </div>
    </div>

    <div class="grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4">
      <div class="p-3.5 sm:p-5 rounded-xl sm:rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark flex items-center gap-3 sm:gap-4 transition-colors">
        <div class="w-10 h-10 sm:w-12 sm:h-12 rounded-lg sm:rounded-xl flex items-center justify-center bg-blue-50 dark:bg-blue-900/30 text-google-blue dark:text-google-blue-dark shrink-0">
          <n-icon size="20" class="sm:hidden">
            <MegaphoneOutline />
          </n-icon>
          <n-icon size="24" class="hidden sm:inline-flex">
            <MegaphoneOutline />
          </n-icon>
        </div>
        <div class="min-w-0 flex-1">
          <div class="text-[11px] sm:text-xs font-medium text-google-text-muted dark:text-google-text-darkMuted truncate">
            {{ t('admin.broadcasts.stat_announcements', '系统公告数') }}
          </div>
          <div class="text-xl sm:text-2xl font-bold text-google-text-primary dark:text-google-text-darkPrimary mt-0.5 truncate">
            {{ statData.totalAnnouncements }}
          </div>
        </div>
      </div>

      <div class="p-3.5 sm:p-5 rounded-xl sm:rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark flex items-center gap-3 sm:gap-4 transition-colors">
        <div class="w-10 h-10 sm:w-12 sm:h-12 rounded-lg sm:rounded-xl flex items-center justify-center bg-amber-50 dark:bg-amber-900/30 text-google-yellow dark:text-google-yellow-dark shrink-0">
          <n-icon size="20" class="sm:hidden">
            <FlameOutline />
          </n-icon>
          <n-icon size="24" class="hidden sm:inline-flex">
            <FlameOutline />
          </n-icon>
        </div>
        <div class="min-w-0 flex-1">
          <div class="text-[11px] sm:text-xs font-medium text-google-text-muted dark:text-google-text-darkMuted truncate">
            {{ t('admin.broadcasts.stat_promotions', '运营活动数') }}
          </div>
          <div class="text-xl sm:text-2xl font-bold text-google-text-primary dark:text-google-text-darkPrimary mt-0.5 truncate">
            {{ statData.totalPromotions }}
          </div>
        </div>
      </div>

      <div class="p-3.5 sm:p-5 rounded-xl sm:rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark flex items-center gap-3 sm:gap-4 transition-colors">
        <div class="w-10 h-10 sm:w-12 sm:h-12 rounded-lg sm:rounded-xl flex items-center justify-center bg-red-50 dark:bg-red-900/30 text-google-red dark:text-google-red-dark shrink-0">
          <n-icon size="20" class="sm:hidden">
            <AlertCircleOutline />
          </n-icon>
          <n-icon size="24" class="hidden sm:inline-flex">
            <AlertCircleOutline />
          </n-icon>
        </div>
        <div class="min-w-0 flex-1">
          <div class="text-[11px] sm:text-xs font-medium text-google-text-muted dark:text-google-text-darkMuted truncate">
            {{ t('admin.broadcasts.stat_urgent_modals', '强弹窗提醒') }}
          </div>
          <div class="text-xl sm:text-2xl font-bold text-google-text-primary dark:text-google-text-darkPrimary mt-0.5 truncate">
            {{ statData.urgentModals }}
          </div>
        </div>
      </div>

      <div class="p-3.5 sm:p-5 rounded-xl sm:rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark flex items-center gap-3 sm:gap-4 transition-colors">
        <div class="w-10 h-10 sm:w-12 sm:h-12 rounded-lg sm:rounded-xl flex items-center justify-center bg-green-50 dark:bg-green-900/30 text-google-green dark:text-google-green-dark shrink-0">
          <n-icon size="20" class="sm:hidden">
            <RadioOutline />
          </n-icon>
          <n-icon size="24" class="hidden sm:inline-flex">
            <RadioOutline />
          </n-icon>
        </div>
        <div class="min-w-0 flex-1">
          <div class="text-[11px] sm:text-xs font-medium text-google-text-muted dark:text-google-text-darkMuted truncate">
            {{ t('admin.broadcasts.stat_sse_bus', 'SSE 实时总线') }}
          </div>
          <div class="text-sm sm:text-base font-bold text-google-green dark:text-google-green-dark mt-1 truncate">
            {{ statData.sseOnlineStatus !== false ? t('admin.broadcasts.sse_online', '总线在线') : t('admin.broadcasts.sse_offline', '已离线') }}
          </div>
        </div>
      </div>
    </div>

    <div class="table-card p-3.5 sm:p-6 rounded-xl sm:rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark transition-colors">
      <div class="flex flex-col sm:flex-row sm:flex-wrap sm:items-center gap-2.5 sm:gap-3 mb-4 sm:mb-6">
        <VSearch
          v-model:value="searchKeyword"
          :placeholder="t('admin.broadcasts.search_placeholder', '搜索公告标题或内容...')"
          class="w-full sm:w-auto sm:flex-1 sm:min-w-[140px] sm:max-w-sm"
          @search="handleSearch"
          @clear="handleReset"
          @keydown.enter="handleSearch"
        />

        <div class="grid grid-cols-2 gap-2.5 w-full sm:flex sm:w-auto shrink-0">
          <VSelection
            v-model:value="filterCategory"
            :options="categoryOptions"
            :placeholder="'全部类别'"
            clearable
            class="w-full sm:w-44"
            @update:value="handleSearch"
          />

          <VSelection
            v-model:value="filterDisplayType"
            :options="displayTypeOptions"
            :placeholder="'全部展现形式'"
            clearable
            class="w-full sm:w-44"
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

        <div class="w-full sm:w-auto sm:ml-auto flex items-center gap-2 shrink-0">
          <VBtn
            clz="normal"
            size="medium"
            :left-icon="MegaphoneOutline"
            class="flex-1 sm:flex-none"
            @click="openCreateModal('ANNOUNCEMENT')"
          >
            {{ t('admin.broadcasts.create_announcement_btn', '发布系统公告') }}
          </VBtn>

          <VBtn
            clz="primary"
            size="medium"
            :left-icon="AddOutline"
            class="flex-1 sm:flex-none"
            @click="openCreateModal('PROMOTION')"
          >
            {{ t('admin.broadcasts.create_promotion_btn', '发布营销推广') }}
          </VBtn>
        </div>
      </div>

      <VTable
        :columns="tableColumns"
        :data="broadcastList"
        :loading="loading"
        :pagination="false"
        :bordered="false"
        :row-key="(row: BroadcastMessageVO) => String(row.id)"
        :scroll-x="960"
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
      v-model:show="showCreateModal"
      :title="modalCategory === 'ANNOUNCEMENT' ? t('admin.broadcasts.modal.title_announcement', '发布系统公告') : t('admin.broadcasts.modal.title_promotion', '发布运营活动')"
      :width="560"
    >
      <div class="flex flex-col gap-4 py-2">
        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
            {{ t('admin.broadcasts.modal.field_title', '消息标题') }} *
          </label>
          <VInput
            v-model:value="createForm.title"
            :placeholder="'请输入公告标题'"
            clearable
          />
        </div>

        <div class="grid grid-cols-2 gap-4">
          <div class="flex flex-col gap-1.5">
            <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
              {{ t('admin.broadcasts.modal.field_display_type', '呈现形式') }} *
            </label>
            <VSelection
              v-model:value="createForm.displayType"
              :options="displayTypeFormOptions"
            />
          </div>

          <div class="flex flex-col gap-1.5">
            <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
              {{ t('admin.broadcasts.modal.field_priority', '优先级 (数值越大越靠前)') }}
            </label>
            <VInput
              v-model:value="priorityStr"
              placeholder="'0'"
              clearable
            />
          </div>
        </div>

        <div v-if="modalCategory === 'PROMOTION'" class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
            {{ t('admin.broadcasts.modal.field_cover', '封面图片链接') }}
          </label>
          <VInput
            v-model:value="createForm.coverImageUrl"
            placeholder="'https://example.com/banner.png'"
            clearable
          />
        </div>

        <div v-if="modalCategory === 'PROMOTION'" class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
            {{ t('admin.broadcasts.modal.field_link', '跳转跳转链接') }}
          </label>
          <VInput
            v-model:value="createForm.linkUrl"
            placeholder="'https://example.com/activity'"
            clearable
          />
        </div>

        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
            {{ t('admin.broadcasts.modal.field_content', '正文内容') }} *
          </label>
          <VInput
            v-model:value="createForm.content"
            type="textarea"
            :rows="4"
            placeholder="'请输入公告完整正文内容...'"
          />
        </div>

        <div class="flex items-center justify-between p-3 rounded-xl bg-google-surface-light dark:bg-google-surface-darkElevated">
          <span class="text-sm font-medium text-google-text-primary dark:text-google-text-darkPrimary">
            {{ t('admin.broadcasts.modal.field_force_popup', '登录强制弹窗拦截提示') }}
          </span>
          <n-switch v-model:value="createForm.forcePopup" />
        </div>
      </div>

      <template #footer>
        <div class="flex items-center justify-end gap-3 w-full">
          <VBtn clz="normal" @click="showCreateModal = false">
            {{ t('common.cancel', '取消') }}
          </VBtn>
          <VBtn
            clz="primary"
            :loading="submitting"
            @click="handleSubmitCreate"
          >
            {{ t('admin.broadcasts.modal.submit_btn', '立即广播推送') }}
          </VBtn>
        </div>
      </template>
    </VModal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, h } from 'vue'
import { useI18n } from 'vue-i18n'
import { NIcon, NSwitch, useMessage, useDialog, type DataTableColumns } from 'naive-ui'
import {
  MegaphoneOutline,
  FlameOutline,
  AlertCircleOutline,
  RadioOutline,
  SearchOutline,
  RefreshOutline,
  AddOutline
} from '@vicons/ionicons5'
import VBtn from '@/components/common/VBtn.vue'
import VTable from '@/components/common/VTable.vue'
import VSearch from '@/components/common/VSearch.vue'
import VSelection from '@/components/common/VSelection.vue'
import VPagination from '@/components/common/VPagination.vue'
import VModal from '@/components/common/VModal.vue'
import VInput from '@/components/common/VInput.vue'
import type {
  BroadcastMessageVO,
  BroadcastDisplayType,
  AdminBroadcastStatisticsVO
} from '@/types/notification'
import {
  getAdminBroadcastStatisticsApi,
  getAdminBroadcastListApi,
  publishAnnouncementApi,
  publishPromotionApi,
  deleteAdminBroadcastApi
} from '@/api/notification'

defineOptions({
  name: 'AdminBroadcastView'
})

const { t } = useI18n()
const message = useMessage()
const dialog = useDialog()

const loading = ref(false)
const broadcastList = ref<BroadcastMessageVO[]>([])

const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

const searchKeyword = ref('')
const filterCategory = ref<string | null>(null)
const filterDisplayType = ref<string | null>(null)

const statData = ref<AdminBroadcastStatisticsVO>({
  totalAnnouncements: 0,
  totalPromotions: 0,
  urgentModals: 0,
  sseOnlineStatus: true
})

const showCreateModal = ref(false)
const modalCategory = ref<'ANNOUNCEMENT' | 'PROMOTION'>('ANNOUNCEMENT')
const submitting = ref(false)

const createForm = ref({
  title: '',
  content: '',
  displayType: 'BANNER' as BroadcastDisplayType,
  coverImageUrl: '',
  linkUrl: '',
  forcePopup: false,
  priority: 0
})

const priorityStr = computed<string>({
  get: () => String(createForm.value.priority || '0'),
  set: (val) => {
    createForm.value.priority = parseInt(val, 10) || 0
  }
})

const categoryOptions = [
  { label: '全部类别', value: '' },
  { label: '系统公告 (ANNOUNCEMENT)', value: 'ANNOUNCEMENT' },
  { label: '运营活动 (PROMOTION)', value: 'PROMOTION' }
]

const displayTypeOptions = [
  { label: '全部展现形式', value: '' },
  { label: '横幅通告 (BANNER)', value: 'BANNER' },
  { label: '弹窗提醒 (MODAL)', value: 'MODAL' },
  { label: '浮动气泡 (TOAST)', value: 'TOAST' },
  { label: '侧边抽屉 (DRAWER)', value: 'DRAWER' }
]

const displayTypeFormOptions = [
  { label: '横幅通告 (BANNER)', value: 'BANNER' },
  { label: '弹窗提醒 (MODAL)', value: 'MODAL' },
  { label: '浮动气泡 (TOAST)', value: 'TOAST' },
  { label: '侧边抽屉 (DRAWER)', value: 'DRAWER' }
]

const tableColumns = computed<DataTableColumns<BroadcastMessageVO>>(() => [
  {
    title: t('admin.broadcasts.col_title', '标题'),
    key: 'title',
    minWidth: 200,
    render(row) {
      return h('div', { class: 'space-y-0.5' }, [
        h('div', { class: 'font-semibold text-google-text-primary dark:text-google-text-darkPrimary' }, row.title),
        h('div', { class: 'text-xs text-google-text-secondary dark:text-google-text-darkSecondary truncate max-w-sm' }, row.content)
      ])
    }
  },
  {
    title: t('admin.broadcasts.col_category', '类型'),
    key: 'category',
    width: 120,
    render(row) {
      return row.category === 'ANNOUNCEMENT' ? '系统公告' : '运营活动'
    }
  },
  {
    title: t('admin.broadcasts.col_display_type', '展现形式'),
    key: 'displayType',
    width: 120,
    render(row) {
      return row.displayType || '-'
    }
  },
  {
    title: t('admin.broadcasts.col_priority', '优先级'),
    key: 'priority',
    width: 80,
    render(row) {
      return String(row.priority ?? 0)
    }
  },
  {
    title: t('admin.broadcasts.col_created_at', '发布时间'),
    key: 'createdAt',
    width: 160,
    render(row) {
      return h('span', { class: 'text-xs text-google-text-secondary dark:text-google-text-darkSecondary font-mono' }, row.createdAt ? new Date(row.createdAt).toLocaleString() : '-')
    }
  },
  {
    title: t('common.actions', '操作'),
    key: 'actions',
    width: 100,
    align: 'center',
    render(row) {
      return h(
        VBtn,
        {
          clz: 'danger',
          size: 'small',
          onClick: () => handleDeleteBroadcast(row.id)
        },
        { default: () => t('common.delete', '撤回') }
      )
    }
  }
])

async function fetchStats() {
  try {
    const res = await getAdminBroadcastStatisticsApi()
    if (res.data) {
      statData.value = res.data
    }
  } catch {}
}

async function fetchBroadcasts() {
  loading.value = true
  try {
    const res = await getAdminBroadcastListApi({
      page: pagination.page,
      size: pagination.pageSize,
      keyword: searchKeyword.value.trim() || undefined,
      category: filterCategory.value || undefined,
      displayType: filterDisplayType.value || undefined
    })
    if (res.data) {
      broadcastList.value = res.data.list ?? []
      pagination.itemCount = Number(res.data.total) || 0
    }
  } catch (err: any) {
    message.error(err.message || t('admin.broadcasts.fetch_failed', '获取广播列表失败'))
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchBroadcasts()
}

function handleReset() {
  searchKeyword.value = ''
  filterCategory.value = null
  filterDisplayType.value = null
  pagination.page = 1
  fetchBroadcasts()
}

function handlePageChange(page: number) {
  pagination.page = page
  fetchBroadcasts()
}

function handlePageSizeChange(size: number) {
  pagination.pageSize = size
  pagination.page = 1
  fetchBroadcasts()
}

function openCreateModal(cat: 'ANNOUNCEMENT' | 'PROMOTION') {
  modalCategory.value = cat
  createForm.value = {
    title: '',
    content: '',
    displayType: cat === 'ANNOUNCEMENT' ? 'BANNER' : 'MODAL',
    coverImageUrl: '',
    linkUrl: '',
    forcePopup: false,
    priority: 0
  }
  showCreateModal.value = true
}

async function handleSubmitCreate() {
  if (!createForm.value.title.trim() || !createForm.value.content.trim()) {
    message.warning(t('admin.broadcasts.title_content_required', '请完整填写公告标题与内容'))
    return
  }

  submitting.value = true
  try {
    const payload = {
      title: createForm.value.title.trim(),
      content: createForm.value.content.trim(),
      displayType: createForm.value.displayType,
      coverImageUrl: createForm.value.coverImageUrl.trim() || undefined,
      linkUrl: createForm.value.linkUrl.trim() || undefined,
      forcePopup: createForm.value.forcePopup,
      priority: createForm.value.priority
    }

    if (modalCategory.value === 'ANNOUNCEMENT') {
      await publishAnnouncementApi(payload)
      message.success(t('admin.broadcasts.announcement_published', '系统公告发布成功'))
    } else {
      await publishPromotionApi(payload)
      message.success(t('admin.broadcasts.promotion_published', '营销活动发布成功'))
    }
    showCreateModal.value = false
    await Promise.all([fetchStats(), fetchBroadcasts()])
  } catch (err: any) {
    message.error(err.message || t('admin.broadcasts.publish_failed', '发布广播失败'))
  } finally {
    submitting.value = false
  }
}

function handleDeleteBroadcast(id: number | string) {
  dialog.warning({
    title: t('admin.broadcasts.delete_confirm_title', '确认撤回广播'),
    content: t('admin.broadcasts.delete_confirm_content', '撤回后客户端将不再展示该条公告或活动弹窗，是否继续？'),
    positiveText: t('common.confirm', '确认撤回'),
    negativeText: t('common.cancel', '取消'),
    onPositiveClick: async () => {
      try {
        await deleteAdminBroadcastApi(id)
        message.success(t('admin.broadcasts.deleted_success', '广播已撤回'))
        await Promise.all([fetchStats(), fetchBroadcasts()])
      } catch (err: any) {
        message.error(err.message || t('admin.broadcasts.delete_failed', '撤回广播失败'))
      }
    }
  })
}

onMounted(() => {
  fetchStats()
  fetchBroadcasts()
})
</script>
