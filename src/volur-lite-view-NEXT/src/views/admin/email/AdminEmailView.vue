<template>
  <div class="admin-email-view flex flex-col gap-4 sm:gap-6">
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h2 class="text-xl font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
          {{ t('admin.emails.title', '邮件管理') }}
        </h2>
        <p class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary mt-1">
          {{ t('admin.emails.desc', '监控平台发信记录、系统验证码邮件、批量通知与失败自动重试') }}
        </p>
      </div>
    </div>

    <div class="grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4">
      <div class="p-3.5 sm:p-5 rounded-xl sm:rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark flex items-center gap-3 sm:gap-4 transition-colors">
        <div class="w-10 h-10 sm:w-12 sm:h-12 rounded-lg sm:rounded-xl flex items-center justify-center bg-blue-50 dark:bg-blue-900/30 text-google-blue dark:text-google-blue-dark shrink-0">
          <n-icon size="20" class="sm:hidden">
            <MailOutline />
          </n-icon>
          <n-icon size="24" class="hidden sm:inline-flex">
            <MailOutline />
          </n-icon>
        </div>
        <div class="min-w-0 flex-1">
          <div class="text-[11px] sm:text-xs font-medium text-google-text-muted dark:text-google-text-darkMuted truncate">
            累计发信总量
          </div>
          <div class="text-xl sm:text-2xl font-bold text-google-text-primary dark:text-google-text-darkPrimary mt-0.5 truncate">
            {{ stats.totalEmails }}
          </div>
        </div>
      </div>

      <div class="p-3.5 sm:p-5 rounded-xl sm:rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark flex items-center gap-3 sm:gap-4 transition-colors">
        <div class="w-10 h-10 sm:w-12 sm:h-12 rounded-lg sm:rounded-xl flex items-center justify-center bg-cyan-50 dark:bg-cyan-900/30 text-cyan-600 dark:text-cyan-400 shrink-0">
          <n-icon size="20" class="sm:hidden">
            <TimeOutline />
          </n-icon>
          <n-icon size="24" class="hidden sm:inline-flex">
            <TimeOutline />
          </n-icon>
        </div>
        <div class="min-w-0 flex-1">
          <div class="text-[11px] sm:text-xs font-medium text-google-text-muted dark:text-google-text-darkMuted truncate">
            今日发信量
          </div>
          <div class="text-xl sm:text-2xl font-bold text-cyan-600 dark:text-cyan-400 mt-0.5 truncate">
            {{ stats.todayEmails }}
          </div>
        </div>
      </div>

      <div class="p-3.5 sm:p-5 rounded-xl sm:rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark flex items-center gap-3 sm:gap-4 transition-colors">
        <div class="w-10 h-10 sm:w-12 sm:h-12 rounded-lg sm:rounded-xl flex items-center justify-center bg-green-50 dark:bg-green-900/30 text-google-green dark:text-google-green-dark shrink-0">
          <n-icon size="20" class="sm:hidden">
            <CheckmarkCircleOutline />
          </n-icon>
          <n-icon size="24" class="hidden sm:inline-flex">
            <CheckmarkCircleOutline />
          </n-icon>
        </div>
        <div class="min-w-0 flex-1">
          <div class="text-[11px] sm:text-xs font-medium text-google-text-muted dark:text-google-text-darkMuted truncate">
            送达成功率
          </div>
          <div class="text-xl sm:text-2xl font-bold text-google-green dark:text-google-green-dark mt-0.5 truncate">
            {{ stats.successEmails }}
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
            发送失败退信
          </div>
          <div class="text-xl sm:text-2xl font-bold text-google-red dark:text-google-red-dark mt-0.5 truncate">
            {{ stats.failedEmails }}
          </div>
        </div>
      </div>
    </div>

    <div class="table-card p-3.5 sm:p-6 rounded-xl sm:rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark transition-colors">
      <div class="flex flex-col sm:flex-row sm:flex-wrap sm:items-center gap-2.5 sm:gap-3 mb-4 sm:mb-6">
        <VSearch
          v-model:value="searchKeyword"
          :placeholder="'搜索收件人邮箱或邮件主题...'"
          class="w-full sm:w-auto sm:flex-1 sm:min-w-[140px] sm:max-w-sm"
          @search="handleSearch"
          @clear="handleReset"
          @keydown.enter="handleSearch"
        />

        <div class="w-full sm:w-44 shrink-0">
          <VSelection
            v-model:value="filterStatus"
            :options="statusOptions"
            :placeholder="'全部状态'"
            clearable
            class="w-full"
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

        <div class="w-full sm:w-auto sm:ml-auto shrink-0">
          <VBtn
            clz="primary"
            size="medium"
            :left-icon="SendOutline"
            class="w-full sm:w-auto"
            @click="showSendModal = true"
          >
            {{ t('admin.emails.manual_send_btn', '手动发信') }}
          </VBtn>
        </div>
      </div>

      <VTable
        :columns="tableColumns"
        :data="emailList"
        :loading="loading"
        :pagination="false"
        :bordered="false"
        :row-key="(row: EmailLogVO) => String(row.id)"
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
      v-model:show="showSendModal"
      :title="'手动发送邮件'"
      :width="540"
    >
      <div class="flex flex-col gap-4 py-2">
        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
            收件人邮箱 *
          </label>
          <VInput
            v-model:value="sendForm.recipient"
            placeholder="'user@example.com'"
            clearable
          />
        </div>

        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
            邮件主题 *
          </label>
          <VInput
            v-model:value="sendForm.subject"
            placeholder="'重要通知主题'"
            clearable
          />
        </div>

        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
            邮件正文内容 *
          </label>
          <VInput
            v-model:value="sendForm.content"
            type="textarea"
            :rows="5"
            placeholder="'支持纯文本或 HTML 正文...'"
          />
        </div>
      </div>

      <template #footer>
        <div class="flex items-center justify-end gap-3 w-full">
          <VBtn clz="normal" @click="showSendModal = false">
            {{ t('common.cancel', '取消') }}
          </VBtn>
          <VBtn clz="primary" :loading="sending" @click="handleSendEmail">
            {{ t('common.send', '发送') }}
          </VBtn>
        </div>
      </template>
    </VModal>

    <VModal
      v-model:show="showDetailModal"
      :title="'邮件投递详情'"
      :width="580"
    >
      <div v-if="selectedEmail" class="space-y-4 text-xs py-2">
        <div class="space-y-2 p-3 rounded-lg bg-gray-50 dark:bg-[#202124] border border-google-border-light dark:border-google-border-dark">
          <div><span class="text-google-text-secondary dark:text-google-text-darkSecondary">收件人: </span><span class="font-semibold font-mono">{{ selectedEmail.recipient }}</span></div>
          <div><span class="text-google-text-secondary dark:text-google-text-darkSecondary">邮件主题: </span><span>{{ selectedEmail.subject }}</span></div>
          <div><span class="text-google-text-secondary dark:text-google-text-darkSecondary">发信类型: </span><span>{{ selectedEmail.type }}</span></div>
          <div><span class="text-google-text-secondary dark:text-google-text-darkSecondary">发件信箱: </span><span class="font-mono">{{ selectedEmail.sender || '-' }}</span></div>
        </div>

        <div v-if="selectedEmail.content">
          <div class="font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1">邮件正文预览:</div>
          <div class="p-3 rounded-lg bg-gray-100 dark:bg-[#303134] overflow-x-auto max-h-48 whitespace-pre-wrap font-mono">{{ selectedEmail.content }}</div>
        </div>

        <div v-if="selectedEmail.errorMessage">
          <div class="font-medium text-red-500 mb-1">退信/错误日志:</div>
          <pre class="p-3 rounded-lg bg-red-950/40 text-red-300 font-mono text-[11px] whitespace-pre-wrap">{{ selectedEmail.errorMessage }}</pre>
        </div>
      </div>

      <template #footer>
        <div class="flex justify-end w-full">
          <VBtn clz="normal" @click="showDetailModal = false">
            {{ t('common.close', '关闭') }}
          </VBtn>
        </div>
      </template>
    </VModal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, h } from 'vue'
import { useI18n } from 'vue-i18n'
import { NIcon, useMessage, type DataTableColumns } from 'naive-ui'
import {
  MailOutline,
  TimeOutline,
  CheckmarkCircleOutline,
  AlertCircleOutline,
  SearchOutline,
  RefreshOutline,
  SendOutline
} from '@vicons/ionicons5'
import VBtn from '@/components/common/VBtn.vue'
import VTable from '@/components/common/VTable.vue'
import VSearch from '@/components/common/VSearch.vue'
import VSelection from '@/components/common/VSelection.vue'
import VPagination from '@/components/common/VPagination.vue'
import VModal from '@/components/common/VModal.vue'
import VInput from '@/components/common/VInput.vue'
import type {
  EmailLogVO,
  AdminEmailStatisticsVO
} from '@/types/email'
import {
  getAdminEmailStatisticsApi,
  getAdminEmailListApi,
  sendAdminManualEmailApi,
  resendAdminEmailApi
} from '@/api/email'

defineOptions({
  name: 'AdminEmailView'
})

const { t } = useI18n()
const message = useMessage()

const loading = ref(false)
const emailList = ref<EmailLogVO[]>([])

const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

const searchKeyword = ref('')
const filterStatus = ref<string | null>(null)

const stats = ref<AdminEmailStatisticsVO>({
  totalEmails: 0,
  todayEmails: 0,
  successEmails: 0,
  failedEmails: 0
})

const showSendModal = ref(false)
const sending = ref(false)
const sendForm = ref({
  recipient: '',
  subject: '',
  content: '',
  html: false
})

const showDetailModal = ref(false)
const selectedEmail = ref<EmailLogVO | null>(null)

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '投递成功 (SENT / SUCCESS)', value: 'SENT' },
  { label: '发送失败 (FAILED)', value: 'FAILED' },
  { label: '正在发送 (SENDING)', value: 'SENDING' }
]

function getStatusText(status: string): string {
  switch (status) {
    case 'SENT':
    case 'SUCCESS':
    case 'DELIVERED':
      return '投递成功'
    case 'FAILED':
    case 'BOUNCED':
      return '发送失败'
    case 'SENDING':
      return '正在发送'
    default:
      return status || '-'
  }
}

const tableColumns = computed<DataTableColumns<EmailLogVO>>(() => [
  {
    title: '收件人邮箱',
    key: 'recipient',
    minWidth: 160,
    render(row) {
      return h('span', { class: 'font-semibold font-mono text-google-text-primary dark:text-google-text-darkPrimary' }, row.recipient)
    }
  },
  {
    title: '邮件主题',
    key: 'subject',
    minWidth: 180,
    ellipsis: { tooltip: true },
    render(row) {
      return h('span', { class: 'text-xs' }, row.subject)
    }
  },
  {
    title: '发信场景',
    key: 'type',
    width: 120,
    render(row) {
      return row.type || '-'
    }
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render(row) {
      return getStatusText(row.status)
    }
  },
  {
    title: '发送时间',
    key: 'createdAt',
    width: 160,
    render(row) {
      return h('span', { class: 'text-xs text-google-text-secondary dark:text-google-text-darkSecondary font-mono' }, row.createdAt ? new Date(row.createdAt).toLocaleString() : '-')
    }
  },
  {
    title: t('common.actions', '操作'),
    key: 'actions',
    width: 140,
    align: 'center',
    render(row) {
      return h('div', { class: 'flex items-center justify-center gap-1.5' }, [
        h(
          VBtn,
          {
            clz: 'normal',
            size: 'small',
            onClick: () => {
              selectedEmail.value = row
              showDetailModal.value = true
            }
          },
          { default: () => '详情' }
        ),
        h(
          VBtn,
          {
            clz: 'normal',
            size: 'small',
            onClick: () => handleResend(row.id)
          },
          { default: () => '重发' }
        )
      ])
    }
  }
])

async function fetchStats() {
  try {
    const res = await getAdminEmailStatisticsApi()
    if (res.data) {
      stats.value = res.data
    }
  } catch {}
}

async function fetchEmails() {
  loading.value = true
  try {
    const res = await getAdminEmailListApi({
      page: pagination.page,
      size: pagination.pageSize,
      keyword: searchKeyword.value.trim() || undefined,
      status: filterStatus.value || undefined
    })
    if (res.data) {
      emailList.value = res.data.list ?? []
      pagination.itemCount = Number(res.data.total) || 0
    }
  } catch (err: any) {
    message.error(err.message || '获取发信记录失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchEmails()
}

function handleReset() {
  searchKeyword.value = ''
  filterStatus.value = null
  pagination.page = 1
  fetchEmails()
}

function handlePageChange(page: number) {
  pagination.page = page
  fetchEmails()
}

function handlePageSizeChange(size: number) {
  pagination.pageSize = size
  pagination.page = 1
  fetchEmails()
}

async function handleSendEmail() {
  if (!sendForm.value.recipient.trim() || !sendForm.value.subject.trim() || !sendForm.value.content.trim()) {
    message.warning('请完整填写收件人、主题与正文内容')
    return
  }
  sending.value = true
  try {
    await sendAdminManualEmailApi({
      recipient: sendForm.value.recipient.trim(),
      subject: sendForm.value.subject.trim(),
      content: sendForm.value.content.trim(),
      html: sendForm.value.html
    })
    message.success('邮件已提交发送队列')
    showSendModal.value = false
    sendForm.value = { recipient: '', subject: '', content: '', html: false }
    await Promise.all([fetchStats(), fetchEmails()])
  } catch (err: any) {
    message.error(err.message || '发信失败')
  } finally {
    sending.value = false
  }
}

async function handleResend(id: string | number) {
  try {
    await resendAdminEmailApi(id)
    message.success('已触发重发请求')
    await Promise.all([fetchStats(), fetchEmails()])
  } catch (err: any) {
    message.error(err.message || '重发邮件失败')
  }
}

onMounted(() => {
  fetchStats()
  fetchEmails()
})
</script>
