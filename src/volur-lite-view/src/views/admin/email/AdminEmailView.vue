<template>
  <div class="admin-email-view">
    <!-- Top section: Compact Metrics Statistics Cards in a single row -->
    <div class="stat-ribbon">
      <div class="stat-mini-card">
        <div class="stat-icon-wrapper primary">
          <n-icon size="18">
            <MailOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ emailStats.totalEmails }}</div>
          <div class="stat-label">{{ t('admin.emails.stat_total') }}</div>
        </div>
      </div>

      <div class="stat-mini-card">
        <div class="stat-icon-wrapper info">
          <n-icon size="18">
            <TimeOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ emailStats.todayEmails }}</div>
          <div class="stat-label">{{ t('admin.emails.stat_today') }}</div>
        </div>
      </div>

      <div class="stat-mini-card">
        <div class="stat-icon-wrapper success">
          <n-icon size="18">
            <CheckmarkCircleOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ emailStats.successEmails }}</div>
          <div class="stat-label">{{ t('admin.emails.stat_success') }}</div>
        </div>
      </div>

      <div class="stat-mini-card">
        <div class="stat-icon-wrapper error">
          <n-icon size="18">
            <CloseCircleOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ emailStats.failedEmails }}</div>
          <div class="stat-label">{{ t('admin.emails.stat_failed') }}</div>
        </div>
      </div>
    </div>

    <!-- Bottom section: Email Table Card -->
    <VolurTableCard
      :bordered="true"
      :table-bordered="true"
      :scroll-x="1250"
      remote
      :columns="columns"
      :data="emailList"
      :loading="loading"
      :pagination="pagination"
      :row-key="(row: EmailLogVO) => String(row.id)"
      table-class="email-data-table"
      @update:sorter="handleSorterChange"
      @update:page="handlePageChange"
      @update:page-size="handlePageSizeChange"
    >
      <template #filter-left>
        <div class="email-filter-left">
          <n-input
            v-model:value="searchKeyword"
            :placeholder="t('admin.emails.search_placeholder')"
            clearable
            class="filter-input-search"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <n-icon :component="SearchOutline" />
            </template>
          </n-input>

          <n-select
            v-model:value="filterType"
            :placeholder="t('admin.emails.filter_type')"
            clearable
            :options="typeSelectOptions"
            class="filter-select-type"
            @update:value="handleSearch"
          />

          <n-select
            v-model:value="filterStatus"
            :placeholder="t('admin.emails.filter_status')"
            clearable
            :options="statusSelectOptions"
            class="filter-select-status"
            @update:value="handleSearch"
          />

          <n-date-picker
            v-model:value="dateRange"
            type="datetimerange"
            clearable
            format="yyyy-MM-dd HH:mm"
            :start-placeholder="t('admin.emails.start_time')"
            :end-placeholder="t('admin.emails.end_time')"
            class="filter-date-picker"
            @update:value="handleSearch"
          />

          <div class="filter-action-buttons">
            <n-button type="primary" @click="handleSearch" class="theme-primary-btn">
              <template #icon>
                <n-icon :component="SearchOutline" />
              </template>
              {{ t('admin.emails.query_btn') }}
            </n-button>

            <n-button quaternary @click="handleReset">
              <template #icon>
                <n-icon :component="RefreshOutline" />
              </template>
              {{ t('admin.emails.reset_btn') }}
            </n-button>
          </div>
        </div>
      </template>

      <template #filter-right>
        <div class="email-filter-right">
          <n-button type="primary" class="theme-primary-btn action-send-btn" @click="openSendModal">
            <template #icon>
              <n-icon :component="PaperPlaneOutline" />
            </template>
            {{ t('admin.emails.send_btn') }}
          </n-button>
        </div>
      </template>
    </VolurTableCard>

    <!-- Email Detail Drawer -->
    <n-drawer v-model:show="showDetailDrawer" :width="680" placement="right">
      <n-drawer-content :title="t('admin.emails.drawer_title')" closable>
        <div v-if="selectedEmail" class="drawer-detail-body">
          <n-descriptions label-placement="left" :column="2" bordered size="small">
            <n-descriptions-item :label="t('admin.emails.detail.id')">
              <span class="id-text">{{ selectedEmail.id }}</span>
            </n-descriptions-item>
            <n-descriptions-item :label="t('admin.emails.detail.status')">
              <n-tag :type="getStatusTagProps(selectedEmail.status).type" size="small">
                {{ getStatusTagProps(selectedEmail.status).label }}
              </n-tag>
            </n-descriptions-item>
            <n-descriptions-item :label="t('admin.emails.detail.message_id') || 'Message-ID'" :span="2" v-if="selectedEmail.messageId">
              <span class="id-text">{{ selectedEmail.messageId }}</span>
            </n-descriptions-item>
            <n-descriptions-item :label="t('admin.emails.detail.type')" :span="2">
              <n-tag :type="getTypeTagType(selectedEmail.type)" size="small">
                {{ formatTypeLabel(selectedEmail.type) }}
              </n-tag>
            </n-descriptions-item>
            <n-descriptions-item :label="t('admin.emails.detail.recipient')" :span="2">
              <span class="recipient-highlight">{{ selectedEmail.recipient }}</span>
            </n-descriptions-item>
            <n-descriptions-item :label="t('admin.emails.detail.subject')" :span="2">
              <strong>{{ selectedEmail.subject }}</strong>
            </n-descriptions-item>
            <n-descriptions-item :label="t('admin.emails.detail.sender')" :span="2">
              <span class="id-text">{{ selectedEmail.sender || '-' }}</span>
            </n-descriptions-item>
            <n-descriptions-item :label="t('admin.emails.detail.created_at')">
              {{ formatDateTime(selectedEmail.createdAt) }}
            </n-descriptions-item>
            <n-descriptions-item :label="t('admin.emails.detail.operator_ip')">
              <span class="id-text">{{ selectedEmail.operatorIp || '-' }}</span>
            </n-descriptions-item>
          </n-descriptions>

          <!-- Error Alert Box -->
          <div v-if="selectedEmail.errorMessage" class="code-section error-box">
            <div class="section-title error-title">{{ t('admin.emails.detail.error_message') }}</div>
            <pre class="code-block error-block">{{ selectedEmail.errorMessage }}</pre>
          </div>

          <!-- Email Content Tabs -->
          <div class="email-content-section">
            <n-tabs type="segment" animated size="small" v-model:value="detailContentTab">
              <n-tab-pane name="rendered" :tab="t('admin.emails.detail.content_tab_rendered')">
                <div v-if="selectedEmail.content" class="iframe-container">
                  <iframe
                    :srcdoc="selectedEmail.content"
                    class="email-preview-iframe"
                    sandbox="allow-same-origin"
                  />
                </div>
                <div v-else class="empty-content">
                  {{ t('admin.emails.detail.content_empty') }}
                </div>
              </n-tab-pane>

              <n-tab-pane name="raw" :tab="t('admin.emails.detail.content_tab_raw')">
                <div class="raw-actions">
                  <n-button size="tiny" quaternary @click="copyRawContent">
                    <template #icon>
                      <n-icon :component="CopyOutline" />
                    </template>
                    {{ t('common.copy') }}
                  </n-button>
                </div>
                <pre class="code-block source-block">{{ selectedEmail.content || '-' }}</pre>
              </n-tab-pane>
            </n-tabs>
          </div>
        </div>

        <template #footer>
          <div class="drawer-footer-actions">
            <n-button
              type="warning"
              secondary
              @click="handleResend(selectedEmail!)"
              :loading="resendingId === selectedEmail?.id"
              v-if="selectedEmail"
            >
              <template #icon>
                <n-icon :component="RefreshOutline" />
              </template>
              {{ t('admin.emails.actions.resend') }}
            </n-button>
            <n-button @click="showDetailDrawer = false">
              {{ t('common.close') }}
            </n-button>
          </div>
        </template>
      </n-drawer-content>
    </n-drawer>

    <!-- Send Email Modal -->
    <n-modal
      v-model:show="showSendModal"
      preset="card"
      :title="t('admin.emails.send_modal.title')"
      style="width: 660px; max-width: 95vw;"
      :bordered="false"
      class="send-email-modal"
    >
      <n-form
        ref="sendFormRef"
        :model="sendModel"
        :rules="sendRules"
        label-placement="top"
        require-mark-placement="right-hanging"
      >
        <n-form-item :label="t('admin.emails.send_modal.recipient_label')" path="recipient">
          <n-input
            v-model:value="sendModel.recipient"
            :placeholder="t('admin.emails.send_modal.recipient_placeholder')"
            clearable
          />
        </n-form-item>

        <n-form-item :label="t('admin.emails.send_modal.subject_label')" path="subject">
          <n-input
            v-model:value="sendModel.subject"
            :placeholder="t('admin.emails.send_modal.subject_placeholder')"
            clearable
          />
        </n-form-item>

        <n-form-item :label="t('admin.emails.send_modal.content_label')" path="content">
          <n-tabs type="segment" animated size="small" style="width: 100%;">
            <n-tab-pane name="edit" tab="编写正文">
              <n-input
                v-model:value="sendModel.content"
                type="textarea"
                :rows="9"
                :placeholder="t('admin.emails.send_modal.content_placeholder')"
                style="font-family: 'Fira Code', monospace; font-size: 13px;"
              />
            </n-tab-pane>
            <n-tab-pane name="preview" :tab="t('admin.emails.send_modal.preview_title')">
              <div class="send-preview-container">
                <iframe
                  v-if="sendModel.content"
                  :srcdoc="sendModel.content"
                  class="email-preview-iframe modal-iframe"
                  sandbox="allow-same-origin"
                />
                <div v-else class="empty-content">
                  {{ t('admin.emails.detail.content_empty') }}
                </div>
              </div>
            </n-tab-pane>
          </n-tabs>
        </n-form-item>

        <div class="form-switch-row">
          <n-switch v-model:value="sendModel.html" size="small" />
          <span class="switch-label">{{ t('admin.emails.send_modal.is_html') }}</span>
        </div>
      </n-form>

      <template #footer>
        <div class="modal-footer-actions">
          <n-button @click="showSendModal = false">
            {{ t('admin.emails.send_modal.cancel') }}
          </n-button>
          <n-button
            type="primary"
            class="theme-primary-btn"
            :loading="sendLoading"
            @click="handleSendEmail"
          >
            <template #icon>
              <n-icon :component="PaperPlaneOutline" />
            </template>
            {{ t('admin.emails.send_modal.send') }}
          </n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, h, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  NIcon,
  NInput,
  NSelect,
  NDatePicker,
  NButton,
  NDrawer,
  NDrawerContent,
  NDescriptions,
  NDescriptionsItem,
  NTag,
  NModal,
  NForm,
  NFormItem,
  NSwitch,
  NTabs,
  NTabPane,
  NTooltip,
  useDialog,
  type DataTableColumns,
  type SelectOption,
  type FormInst,
  type FormRules
} from 'naive-ui'
import { VolurTableCard } from '@/public/volurEx'
import {
  MailOutline,
  TimeOutline,
  CheckmarkCircleOutline,
  CloseCircleOutline,
  SearchOutline,
  RefreshOutline,
  PaperPlaneOutline,
  EyeOutline,
  CopyOutline
} from '@vicons/ionicons5'
import {
  getAdminEmailListApi,
  getAdminEmailStatisticsApi,
  getAdminEmailDetailApi,
  sendAdminManualEmailApi,
  resendAdminEmailApi
} from '@/api/verification'
import type {
  EmailLogVO,
  AdminEmailStatisticsVO,
  AdminEmailQueryDTO,
  AdminSendEmailDTO
} from '@/types/verification'

const { t, locale } = useI18n()
const dialog = useDialog()

// Statistics State
const emailStats = reactive<AdminEmailStatisticsVO>({
  totalEmails: 0,
  todayEmails: 0,
  successEmails: 0,
  failedEmails: 0
})

// Query & Filter State
const searchKeyword = ref('')
const filterType = ref<string | null>(null)
const filterStatus = ref<string | null>(null)
const dateRange = ref<[number, number] | null>(null)
const sortBy = ref('createdAt')
const sortOrder = ref<'asc' | 'desc'>('desc')

const typeSelectOptions = computed<SelectOption[]>(() => [
  { label: t('admin.emails.type_all'), value: '' },
  { label: t('admin.emails.type_register'), value: 'REGISTER_CAPTCHA' },
  { label: t('admin.emails.type_reset_pwd'), value: 'RESET_PASSWORD' },
  { label: t('admin.emails.type_change_pwd'), value: 'CHANGE_PASSWORD' },
  { label: t('admin.emails.type_delete_account'), value: 'DELETE_ACCOUNT' },
  { label: t('admin.emails.type_test'), value: 'TEST' },
  { label: t('admin.emails.type_manual'), value: 'MANUAL' }
])

function getStatusTagProps(status?: string): { type: 'default' | 'info' | 'warning' | 'success' | 'error'; label: string } {
  switch (status) {
    case 'SENDING':
      return { type: 'info', label: t('admin.emails.status_sending') || '发送中 (SENDING)' }
    case 'SENT':
      return { type: 'warning', label: t('admin.emails.status_sent') || '已转交服务商 (SENT)' }
    case 'DELIVERED':
    case 'SUCCESS':
      return { type: 'success', label: t('admin.emails.status_delivered') || '已真实送达 (DELIVERED)' }
    case 'BOUNCED':
      return { type: 'error', label: t('admin.emails.status_bounced') || '发生退信 (BOUNCED)' }
    case 'COMPLAINED':
      return { type: 'error', label: t('admin.emails.status_complained') || '垃圾投诉 (COMPLAINED)' }
    case 'FAILED':
    default:
      return { type: 'error', label: t('admin.emails.status_failed') || '发送失败 (FAILED)' }
  }
}

const statusSelectOptions = computed<SelectOption[]>(() => [
  { label: t('admin.emails.status_all'), value: '' },
  { label: t('admin.emails.status_sending') || '发送中 (SENDING)', value: 'SENDING' },
  { label: t('admin.emails.status_sent') || '已转交服务商 (SENT)', value: 'SENT' },
  { label: t('admin.emails.status_delivered') || '已真实送达 (DELIVERED)', value: 'DELIVERED' },
  { label: t('admin.emails.status_bounced') || '发生退信 (BOUNCED)', value: 'BOUNCED' },
  { label: t('admin.emails.status_complained') || '垃圾投诉 (COMPLAINED)', value: 'COMPLAINED' },
  { label: t('admin.emails.status_failed') || '发送失败 (FAILED)', value: 'FAILED' }
])

// Table & Pagination State
const loading = ref(false)
const emailList = ref<EmailLogVO[]>([])
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
  prefix: (info: { itemCount?: number }) => t('admin.emails.total_prefix', { count: info.itemCount || 0 })
})

// Drawer State
const showDetailDrawer = ref(false)
const selectedEmail = ref<EmailLogVO | null>(null)
const detailContentTab = ref<'rendered' | 'raw'>('rendered')
const resendingId = ref<number | string | null>(null)

// Send Modal State
const showSendModal = ref(false)
const sendLoading = ref(false)
const sendFormRef = ref<FormInst | null>(null)
const sendModel = reactive<AdminSendEmailDTO>({
  recipient: '',
  subject: '',
  content: '',
  html: true
})

const sendRules: FormRules = {
  recipient: [
    { required: true, message: () => t('admin.emails.validation.recipient_required'), trigger: ['input', 'blur'] },
    {
      type: 'email',
      message: () => t('admin.emails.validation.recipient_invalid'),
      trigger: ['input', 'blur']
    }
  ],
  subject: [
    { required: true, message: () => t('admin.emails.validation.subject_required'), trigger: ['input', 'blur'] },
    { max: 256, message: () => t('admin.emails.validation.subject_max'), trigger: ['input', 'blur'] }
  ],
  content: [
    { required: true, message: () => t('admin.emails.validation.content_required'), trigger: ['input', 'blur'] }
  ]
}

function getTypeTagType(type: string): 'info' | 'warning' | 'error' | 'primary' | 'default' {
  switch (type) {
    case 'REGISTER_CAPTCHA':
      return 'info'
    case 'RESET_PASSWORD':
    case 'CHANGE_PASSWORD':
      return 'warning'
    case 'DELETE_ACCOUNT':
      return 'error'
    case 'MANUAL':
      return 'primary'
    case 'TEST':
    default:
      return 'default'
  }
}

function formatTypeLabel(type: string): string {
  switch (type) {
    case 'REGISTER_CAPTCHA':
      return '注册验证码'
    case 'RESET_PASSWORD':
      return '密码重置'
    case 'CHANGE_PASSWORD':
      return '修改密码'
    case 'DELETE_ACCOUNT':
      return '注销账号'
    case 'MANUAL':
      return '手动发信'
    case 'TEST':
      return '系统测试'
    default:
      return type || '-'
  }
}

function formatDateTime(timeStr?: string): string {
  if (!timeStr) return '-'
  const date = new Date(timeStr)
  return date.toLocaleString(locale.value === 'zh_CN' ? 'zh-CN' : 'en-US', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// Table Columns Definition
const columns = computed<DataTableColumns<EmailLogVO>>(() => [
  {
    title: t('admin.emails.columns.id'),
    key: 'id',
    width: 180,
    render(row) {
      return h('span', { class: 'id-text' }, String(row.id))
    }
  },
  {
    title: t('admin.emails.columns.recipient'),
    key: 'recipient',
    minWidth: 170,
    render(row) {
      return h(
        'span',
        {
          class: 'recipient-cell',
          title: row.recipient
        },
        row.recipient
      )
    }
  },
  {
    title: t('admin.emails.columns.subject'),
    key: 'subject',
    minWidth: 200,
    render(row) {
      return h(
        NTooltip,
        { trigger: 'hover' },
        {
          trigger: () =>
            h(
              'span',
              {
                class: 'subject-cell'
              },
              row.subject
            ),
          default: () => row.subject
        }
      )
    }
  },
  {
    title: t('admin.emails.columns.type'),
    key: 'type',
    width: 130,
    render(row) {
      return h(
        NTag,
        {
          type: getTypeTagType(row.type),
          size: 'small',
          bordered: false
        },
        { default: () => formatTypeLabel(row.type) }
      )
    }
  },
  {
    title: t('admin.emails.columns.sender'),
    key: 'sender',
    minWidth: 160,
    render(row) {
      return h('span', { class: 'sender-text' }, row.sender || '-')
    }
  },
  {
    title: t('admin.emails.columns.status'),
    key: 'status',
    width: 130,
    render(row) {
      const tag = getStatusTagProps(row.status)
      return h(
        NTag,
        {
          type: tag.type,
          size: 'small',
          bordered: false
        },
        { default: () => tag.label }
      )
    }
  },
  {
    title: t('admin.emails.columns.created_at'),
    key: 'createdAt',
    width: 170,
    sorter: true,
    render(row) {
      return formatDateTime(row.createdAt)
    }
  },
  {
    title: t('admin.emails.columns.actions'),
    key: 'actions',
    width: 140,
    fixed: 'right',
    render(row) {
      return h('div', { class: 'table-action-btns' }, [
        h(
          NButton,
          {
            quaternary: true,
            size: 'tiny',
            onClick: () => handleViewDetail(row)
          },
          {
            icon: () => h(NIcon, null, { default: () => h(EyeOutline) }),
            default: () => t('admin.emails.actions.view_detail')
          }
        ),
        h(
          NButton,
          {
            quaternary: true,
            size: 'tiny',
            type: 'warning',
            loading: resendingId.value === row.id,
            onClick: () => handleResend(row)
          },
          {
            icon: () => h(NIcon, null, { default: () => h(RefreshOutline) }),
            default: () => t('admin.emails.actions.resend')
          }
        )
      ])
    }
  }
])

// Fetch Data Actions
async function fetchStatistics() {
  try {
    const res = await getAdminEmailStatisticsApi()
    if (res.data) {
      emailStats.totalEmails = res.data.totalEmails
      emailStats.todayEmails = res.data.todayEmails
      emailStats.successEmails = res.data.successEmails
      emailStats.failedEmails = res.data.failedEmails
    }
  } catch {}
}

let pollTimer: ReturnType<typeof setInterval> | null = null

function checkAndStartPolling() {
  const hasInFlight = emailList.value.some((item) => item.status === 'SENDING' || item.status === 'SENT')
  if (hasInFlight && !pollTimer) {
    pollTimer = setInterval(async () => {
      await fetchEmailList(false)
      await fetchStatistics()
      if (selectedEmail.value && (selectedEmail.value.status === 'SENDING' || selectedEmail.value.status === 'SENT')) {
        const found = emailList.value.find((item) => item.id === selectedEmail.value?.id)
        if (found) {
          selectedEmail.value = found
        }
      }
      if (!emailList.value.some((item) => item.status === 'SENDING' || item.status === 'SENT')) {
        stopPolling()
      }
    }, 2500)
  } else if (!hasInFlight && pollTimer) {
    stopPolling()
  }
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

onUnmounted(() => {
  stopPolling()
})

async function fetchEmailList(showLoading = true) {
  if (showLoading) {
    loading.value = true
  }
  try {
    const queryDTO: AdminEmailQueryDTO = {
      page: pagination.page,
      size: pagination.pageSize,
      sortBy: sortBy.value,
      sortOrder: sortOrder.value
    }

    if (searchKeyword.value.trim()) {
      queryDTO.keyword = searchKeyword.value.trim()
    }
    if (filterType.value) {
      queryDTO.type = filterType.value
    }
    if (filterStatus.value) {
      queryDTO.status = filterStatus.value
    }
    if (dateRange.value && dateRange.value.length === 2) {
      queryDTO.startTime = new Date(dateRange.value[0]).toISOString()
      queryDTO.endTime = new Date(dateRange.value[1]).toISOString()
    }

    const res = await getAdminEmailListApi(queryDTO)
    if (res.data) {
      emailList.value = res.data.list || []
      pagination.itemCount = res.data.total || 0
      checkAndStartPolling()
    }
  } catch {
    emailList.value = []
    pagination.itemCount = 0
  } finally {
    if (showLoading) {
      loading.value = false
    }
  }
}

function handleSearch() {
  pagination.page = 1
  fetchEmailList()
}

function handleReset() {
  stopPolling()
  searchKeyword.value = ''
  filterType.value = null
  filterStatus.value = null
  dateRange.value = null
  sortBy.value = 'createdAt'
  sortOrder.value = 'desc'
  pagination.page = 1
  fetchEmailList()
  fetchStatistics()
}

function handlePageChange(page: number) {
  pagination.page = page
  fetchEmailList()
}

function handlePageSizeChange(pageSize: number) {
  pagination.pageSize = pageSize
  pagination.page = 1
  fetchEmailList()
}

function handleSorterChange(sorter: { columnKey: string; order: 'ascend' | 'descend' | false } | null) {
  if (sorter && sorter.order) {
    sortBy.value = sorter.columnKey
    sortOrder.value = sorter.order === 'ascend' ? 'asc' : 'desc'
  } else {
    sortBy.value = 'createdAt'
    sortOrder.value = 'desc'
  }
  fetchEmailList()
}

async function handleViewDetail(row: EmailLogVO) {
  selectedEmail.value = row
  detailContentTab.value = 'rendered'
  showDetailDrawer.value = true

  // Fetch full details if content is truncated or needed
  try {
    const res = await getAdminEmailDetailApi(row.id)
    if (res.data) {
      selectedEmail.value = res.data
    }
  } catch {}
}

function handleResend(row: EmailLogVO) {
  dialog.warning({
    title: t('admin.emails.actions.resend_confirm_title'),
    content: t('admin.emails.actions.resend_confirm_content', { recipient: row.recipient }),
    positiveText: t('common.confirm') || '确定',
    negativeText: t('common.cancel') || '取消',
    onPositiveClick: async () => {
      resendingId.value = row.id
      try {
        await resendAdminEmailApi(row.id)
        if (window.$message) {
          window.$message.success(t('admin.emails.send_modal.resend_success'))
        }
        await fetchEmailList()
        await fetchStatistics()
        checkAndStartPolling()
      } catch {
      } finally {
        resendingId.value = null
      }
    }
  })
}

function openSendModal() {
  sendModel.recipient = ''
  sendModel.subject = ''
  sendModel.content = ''
  sendModel.html = true
  showSendModal.value = true
}

async function handleSendEmail() {
  sendFormRef.value?.validate(async (errors) => {
    if (!errors) {
      sendLoading.value = true
      try {
        await sendAdminManualEmailApi({
          recipient: sendModel.recipient.trim(),
          subject: sendModel.subject.trim(),
          content: sendModel.content,
          html: sendModel.html
        })
        if (window.$message) {
          window.$message.success(t('admin.emails.send_modal.send_success'))
        }
        showSendModal.value = false
        await fetchEmailList()
        await fetchStatistics()
        checkAndStartPolling()
      } catch {
      } finally {
        sendLoading.value = false
      }
    }
  })
}

function copyRawContent() {
  if (!selectedEmail.value?.content) return
  navigator.clipboard.writeText(selectedEmail.value.content).then(() => {
    if (window.$message) {
      window.$message.success(t('common.copied_to_clipboard'))
    }
  })
}

onMounted(() => {
  fetchStatistics()
  fetchEmailList()
})
</script>

<style scoped lang="scss">
.admin-email-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* Stat Ribbon Grid */
.stat-ribbon {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;

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

    &.info {
      background-color: rgba(59, 130, 246, 0.1);
      color: #3b82f6;
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

    .stat-value {
      font-size: 20px;
      font-weight: 700;
      color: var(--v-text-primary, #111111);
      line-height: 1.2;
    }

    .stat-label {
      font-size: 12px;
      color: var(--v-text-desc, #444444);
      font-weight: 500;
    }
  }
}

.id-text {
  font-family: 'Fira Code', monospace;
  font-size: 12px;
  color: #666666;
}

.recipient-cell {
  font-size: 13px;
  font-weight: 600;
  color: var(--v-text-primary, #111111);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recipient-highlight {
  font-family: 'Fira Code', monospace;
  font-size: 13px;
  font-weight: 600;
  color: #6366f1;
}

.subject-cell {
  font-size: 13px;
  color: var(--v-text-primary, #111111);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: block;
}

.sender-text {
  font-size: 12px;
  color: #444444;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.table-action-btns {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* Drawer & Content Styles */
.drawer-detail-body {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .code-section {
    display: flex;
    flex-direction: column;
    gap: 6px;

    .section-title {
      font-size: 13px;
      font-weight: 600;
      color: #444444;

      &.error-title {
        color: #ef4444;
      }
    }

    .code-block {
      padding: 12px;
      border-radius: 8px;
      font-family: 'Fira Code', monospace;
      font-size: 12px;
      overflow-x: auto;
      white-space: pre-wrap;
      margin: 0;

      &.error-block {
        background-color: rgba(239, 68, 68, 0.08);
        color: #dc2626;
        border: 1px solid rgba(239, 68, 68, 0.2);
      }

      &.source-block {
        background-color: #1e1e1e;
        color: #d4d4d4;
        max-height: 480px;
      }
    }
  }

  .email-content-section {
    margin-top: 8px;

    .iframe-container {
      width: 100%;
      border-radius: 8px;
      overflow: hidden;
      border: 1px solid #e5e7eb;
      background: #ffffff;
    }

    .email-preview-iframe {
      width: 100%;
      min-height: 460px;
      border: none;
      background: #ffffff;
      display: block;
    }

    .raw-actions {
      display: flex;
      justify-content: flex-end;
      margin-bottom: 6px;
    }

    .empty-content {
      text-align: center;
      padding: 40px;
      color: #999999;
      font-size: 13px;
    }
  }
}

.drawer-footer-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  width: 100%;
}

/* Modal Form Styles */
.form-switch-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: -8px;
  margin-bottom: 8px;

  .switch-label {
    font-size: 13px;
    color: #444444;
  }
}

.send-preview-container {
  width: 100%;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  background: #ffffff;

  .modal-iframe {
    min-height: 240px;
  }
}

.modal-footer-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}

.email-filter-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;

  .filter-input-search {
    width: 200px;
    min-width: 150px;
  }

  .filter-select-type {
    width: 140px;
    flex-shrink: 0;
  }

  .filter-select-status {
    width: 125px;
    flex-shrink: 0;
  }

  .filter-date-picker {
    width: 290px;
  }

  .filter-action-buttons {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.email-filter-right {
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

  .email-filter-left {
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: 10px;

    .filter-input-search,
    .filter-select-type,
    .filter-select-status,
    .filter-date-picker {
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

  .email-filter-right {
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
