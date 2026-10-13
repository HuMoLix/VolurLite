<template>
  <div class="admin-risk-ip-view flex flex-col gap-4 sm:gap-6">
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h2 class="text-xl font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
          {{ t('admin.risk.ip.title', 'IP 风控治理') }}
        </h2>
        <p class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary mt-1">
          {{ t('admin.risk.ip.subtitle', '黑白名单策略、实时动态熔断阻断与全链路 IP 行为诊断探测') }}
        </p>
      </div>
    </div>

    <div class="grid grid-cols-2 md:grid-cols-5 gap-3 sm:gap-4">
      <div class="p-3.5 sm:p-5 rounded-xl sm:rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark flex items-center gap-3 sm:gap-4 transition-colors">
        <div class="w-10 h-10 sm:w-12 sm:h-12 rounded-lg sm:rounded-xl flex items-center justify-center bg-amber-50 dark:bg-amber-900/30 text-google-yellow dark:text-google-yellow-dark shrink-0">
          <n-icon size="20" class="sm:hidden">
            <ShieldOutline />
          </n-icon>
          <n-icon size="24" class="hidden sm:inline-flex">
            <ShieldOutline />
          </n-icon>
        </div>
        <div class="min-w-0 flex-1">
          <div class="text-[11px] sm:text-xs font-medium text-google-text-muted dark:text-google-text-darkMuted truncate">
            {{ t('admin.risk.ip.stat_total_banned', '历史封禁总数') }}
          </div>
          <div class="text-xl sm:text-2xl font-bold text-google-text-primary dark:text-google-text-darkPrimary mt-0.5 truncate">
            {{ statistics.totalBannedCount }}
          </div>
        </div>
      </div>

      <div class="p-3.5 sm:p-5 rounded-xl sm:rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark flex items-center gap-3 sm:gap-4 transition-colors">
        <div class="w-10 h-10 sm:w-12 sm:h-12 rounded-lg sm:rounded-xl flex items-center justify-center bg-red-50 dark:bg-red-900/30 text-google-red dark:text-google-red-dark shrink-0">
          <n-icon size="20" class="sm:hidden">
            <BanOutline />
          </n-icon>
          <n-icon size="24" class="hidden sm:inline-flex">
            <BanOutline />
          </n-icon>
        </div>
        <div class="min-w-0 flex-1">
          <div class="text-[11px] sm:text-xs font-medium text-google-text-muted dark:text-google-text-darkMuted truncate">
            {{ t('admin.risk.ip.stat_active_banned', '当前生效封禁') }}
          </div>
          <div class="text-xl sm:text-2xl font-bold text-google-text-primary dark:text-google-text-darkPrimary mt-0.5 truncate">
            {{ statistics.activeBannedCount }}
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
            {{ t('admin.risk.ip.stat_total_whitelisted', '白名单数量') }}
          </div>
          <div class="text-xl sm:text-2xl font-bold text-google-text-primary dark:text-google-text-darkPrimary mt-0.5 truncate">
            {{ statistics.totalWhitelistedCount }}
          </div>
        </div>
      </div>

      <div class="p-3.5 sm:p-5 rounded-xl sm:rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark flex items-center gap-3 sm:gap-4 transition-colors">
        <div class="w-10 h-10 sm:w-12 sm:h-12 rounded-lg sm:rounded-xl flex items-center justify-center bg-blue-50 dark:bg-blue-900/30 text-google-blue dark:text-google-blue-dark shrink-0">
          <n-icon size="20" class="sm:hidden">
            <FlashOutline />
          </n-icon>
          <n-icon size="24" class="hidden sm:inline-flex">
            <FlashOutline />
          </n-icon>
        </div>
        <div class="min-w-0 flex-1">
          <div class="text-[11px] sm:text-xs font-medium text-google-text-muted dark:text-google-text-darkMuted truncate">
            {{ t('admin.risk.ip.stat_today_auto_banned', '今日自动触发') }}
          </div>
          <div class="text-xl sm:text-2xl font-bold text-google-text-primary dark:text-google-text-darkPrimary mt-0.5 truncate">
            {{ statistics.todayAutoBannedCount }}
          </div>
        </div>
      </div>

      <div class="p-3.5 sm:p-5 rounded-xl sm:rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark flex items-center gap-3 sm:gap-4 transition-colors col-span-2 md:col-span-1">
        <div class="w-10 h-10 sm:w-12 sm:h-12 rounded-lg sm:rounded-xl flex items-center justify-center bg-purple-50 dark:bg-purple-900/30 text-purple-600 dark:text-purple-400 shrink-0">
          <n-icon size="20" class="sm:hidden">
            <HammerOutline />
          </n-icon>
          <n-icon size="24" class="hidden sm:inline-flex">
            <HammerOutline />
          </n-icon>
        </div>
        <div class="min-w-0 flex-1">
          <div class="text-[11px] sm:text-xs font-medium text-google-text-muted dark:text-google-text-darkMuted truncate">
            {{ t('admin.risk.ip.stat_today_manual_banned', '今日手动处置') }}
          </div>
          <div class="text-xl sm:text-2xl font-bold text-google-text-primary dark:text-google-text-darkPrimary mt-0.5 truncate">
            {{ statistics.todayManualBannedCount }}
          </div>
        </div>
      </div>
    </div>

    <div class="table-card p-3.5 sm:p-6 rounded-xl sm:rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark transition-colors">
      <div class="flex flex-col sm:flex-row sm:flex-wrap sm:items-center gap-2.5 sm:gap-3 mb-4 sm:mb-6">
        <VSearch
          v-model:value="searchKeyword"
          :placeholder="t('admin.risk.ip.search_placeholder', '搜索 IP / CIDR / 原因...')"
          class="w-full sm:w-auto sm:flex-1 sm:min-w-[140px] sm:max-w-sm"
          @search="handleSearch"
          @clear="handleReset"
          @keydown.enter="handleSearch"
        />

        <div class="grid grid-cols-2 sm:grid-cols-3 gap-2.5 w-full sm:flex sm:w-auto shrink-0">
          <VSelection
            v-model:value="filterRuleType"
            :options="ruleTypeOptions"
            :placeholder="t('admin.risk.ip.filter_rule_type', '规则类型')"
            clearable
            class="w-full sm:w-36"
            @update:value="handleSearch"
          />

          <VSelection
            v-model:value="filterSourceType"
            :options="sourceTypeOptions"
            :placeholder="t('admin.risk.ip.filter_source_type', '触发来源')"
            clearable
            class="w-full sm:w-36"
            @update:value="handleSearch"
          />

          <VSelection
            v-model:value="filterStatus"
            :options="statusOptions"
            :placeholder="t('admin.risk.ip.filter_status', '状态筛选')"
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
            :left-icon="PulseOutline"
            class="flex-1 sm:flex-none"
            @click="openDiagnoseModal"
          >
            {{ t('admin.risk.ip.diagnose_btn', '实时探测') }}
          </VBtn>

          <VBtn
            clz="primary"
            size="medium"
            :left-icon="AddOutline"
            class="flex-1 sm:flex-none"
            @click="openAddModal"
          >
            {{ t('admin.risk.ip.add_rule_btn', '添加规则') }}
          </VBtn>
        </div>
      </div>

      <VTable
        :columns="columns"
        :data="ruleList"
        :loading="loading"
        :pagination="false"
        :bordered="false"
        :row-key="(row: AdminIpRuleVO) => String(row.id)"
        :scroll-x="1050"
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
      v-model:show="showAddModal"
      :title="t('admin.risk.ip.add_modal_title', '新建 IP 风控规则')"
      :width="500"
    >
      <div class="flex flex-col gap-4 py-2">
        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
            IP 地址 / CIDR <span class="text-red-500">*</span>
          </label>
          <VInput
            v-model:value="addFormData.ip"
            placeholder="例如: 192.168.1.100 或 10.0.0.0/24"
            clearable
          />
        </div>

        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
            规则类型 <span class="text-red-500">*</span>
          </label>
          <VSelection
            v-model:value="addFormData.ruleType"
            :options="addRuleTypeOptions"
          />
        </div>

        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
            封禁时长 (分钟，0 为永久)
          </label>
          <VInput
            v-model:value="addDurationStr"
            placeholder="默认 0 为永久有效"
            clearable
          />
        </div>

        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
            处置原因 / 备注
          </label>
          <VInput
            v-model:value="addFormData.reason"
            placeholder="例如: 恶意撞库攻击防护"
            clearable
          />
        </div>
      </div>

      <template #footer>
        <div class="flex items-center justify-end gap-3 w-full">
          <VBtn
            clz="normal"
            @click="showAddModal = false"
          >
            {{ t('common.cancel', '取消') }}
          </VBtn>
          <VBtn
            clz="primary"
            :loading="addSubmitting"
            @click="handleSubmitAdd"
          >
            {{ t('common.confirm', '确认创建') }}
          </VBtn>
        </div>
      </template>
    </VModal>

    <VModal
      v-model:show="showDiagnoseModal"
      :title="t('admin.risk.ip.diagnose_modal_title', '实时 IP 风险诊断探测')"
      :width="580"
    >
      <div class="space-y-4 py-2">
        <div class="flex items-center gap-2">
          <div class="flex-1">
            <VInput
              v-model:value="probeIp"
              placeholder="输入目标 IP 地址进行诊断..."
              clearable
              @keydown.enter="handleExecuteProbe"
            />
          </div>
          <VBtn
            clz="primary"
            size="medium"
            :left-icon="PulseOutline"
            :loading="probeLoading"
            @click="handleExecuteProbe"
          >
            {{ t('admin.risk.ip.diagnose_query_btn', '探测') }}
          </VBtn>
        </div>

        <div v-if="diagnosisResult" class="p-4 rounded-xl bg-google-surface-light dark:bg-[#202124] border border-google-border-light dark:border-google-border-dark space-y-3">
          <div class="flex items-center justify-between pb-2 border-b border-google-border-light dark:border-google-border-dark">
            <div class="flex items-center gap-2">
              <span class="font-mono text-sm font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
                {{ diagnosisResult.ip }}
              </span>
            </div>
            <span
              class="px-2 py-0.5 rounded text-xs font-semibold"
              :class="getRiskBadgeClass(diagnosisResult.riskLevel)"
            >
              {{ diagnosisResult.riskLevel }}
            </span>
          </div>

          <div class="grid grid-cols-2 gap-3 text-xs">
            <div>
              <span class="text-google-text-secondary dark:text-google-text-darkSecondary">当前状态:</span>
              <span class="ml-1.5 font-medium" :class="diagnosisResult.isBanned ? 'text-red-600 dark:text-red-400' : 'text-green-600 dark:text-green-400'">
                {{ diagnosisResult.isWhitelisted ? '白名单' : diagnosisResult.isBypassed ? '回环放行' : diagnosisResult.isBanned ? '已封禁 (403)' : '正常放行' }}
              </span>
            </div>

            <div>
              <span class="text-google-text-secondary dark:text-google-text-darkSecondary">近时失败次数:</span>
              <span class="ml-1.5 font-mono font-medium text-amber-600 dark:text-amber-400">{{ diagnosisResult.recentFailures }} 次</span>
            </div>

            <div>
              <span class="text-google-text-secondary dark:text-google-text-darkSecondary">请求速率:</span>
              <span class="ml-1.5 font-mono font-medium text-blue-600 dark:text-blue-400">{{ diagnosisResult.recentVelocity }} req/min</span>
            </div>

            <div v-if="diagnosisResult.browserFamily">
              <span class="text-google-text-secondary dark:text-google-text-darkSecondary">客户端环境:</span>
              <span class="ml-1.5 text-google-text-primary dark:text-google-text-darkPrimary">{{ diagnosisResult.browserFamily }}</span>
            </div>

            <div v-if="diagnosisResult.deviceFingerprint" class="col-span-2">
              <span class="text-google-text-secondary dark:text-google-text-darkSecondary">设备指纹:</span>
              <span class="ml-1.5 font-mono text-[11px] text-google-text-primary dark:text-google-text-darkPrimary">{{ diagnosisResult.deviceFingerprint }}</span>
            </div>

            <div v-if="diagnosisResult.tlsProtocol" class="col-span-2">
              <span class="text-google-text-secondary dark:text-google-text-darkSecondary">TLS 协议:</span>
              <span class="ml-1.5 font-mono text-[11px] text-google-text-primary dark:text-google-text-darkPrimary">
                {{ diagnosisResult.tlsProtocol }} {{ diagnosisResult.tlsAlpn ? `(${diagnosisResult.tlsAlpn})` : '' }}
              </span>
            </div>

            <div v-if="diagnosisResult.banExpiresAt" class="col-span-2">
              <span class="text-google-text-secondary dark:text-google-text-darkSecondary">解封时间:</span>
              <span class="ml-1.5 font-mono text-google-text-primary dark:text-google-text-darkPrimary">{{ diagnosisResult.banExpiresAt }}</span>
            </div>

            <div v-if="diagnosisResult.banReason" class="col-span-2">
              <span class="text-google-text-secondary dark:text-google-text-darkSecondary">处置原因:</span>
              <span class="ml-1.5 text-google-text-primary dark:text-google-text-darkPrimary">{{ diagnosisResult.banReason }}</span>
            </div>
          </div>
        </div>

        <div v-else-if="!probeLoading" class="py-10 text-center text-xs text-google-text-secondary dark:text-google-text-darkSecondary">
          请输入待查询 IP 并点击探测，系统将实时执行规则与指纹行为核验
        </div>
      </div>

      <template #footer>
        <div class="flex items-center justify-end w-full">
          <VBtn
            clz="normal"
            @click="showDiagnoseModal = false"
          >
            {{ t('common.close', '关闭') }}
          </VBtn>
        </div>
      </template>
    </VModal>

    <VModal
      v-model:show="showRevokeModal"
      :title="t('admin.risk.ip.revoke_modal_title', '解封 IP 确认')"
      :width="420"
    >
      <div class="py-2 text-xs text-google-text-secondary dark:text-google-text-darkSecondary">
        确定要解除对 IP <span class="font-mono font-semibold text-google-text-primary dark:text-google-text-darkPrimary">{{ targetRule?.ip }}</span> 的限制吗？该规则状态将被撤销。
      </div>

      <template #footer>
        <div class="flex items-center justify-end gap-3 w-full">
          <VBtn
            clz="normal"
            @click="showRevokeModal = false"
          >
            {{ t('common.cancel', '取消') }}
          </VBtn>
          <VBtn
            clz="primary"
            :loading="actionLoading"
            @click="handleConfirmRevoke"
          >
            {{ t('admin.risk.ip.revoke_confirm_btn', '确认解封') }}
          </VBtn>
        </div>
      </template>
    </VModal>

    <VModal
      v-model:show="showDeleteModal"
      :title="t('admin.risk.ip.delete_modal_title', '删除 IP 规则确认')"
      :width="420"
    >
      <div class="py-2 text-xs text-google-text-secondary dark:text-google-text-darkSecondary">
        确定彻底删除针对 IP <span class="font-mono font-semibold text-google-text-primary dark:text-google-text-darkPrimary">{{ targetRule?.ip }}</span> 的规则记录吗？该操作不可撤销。
      </div>

      <template #footer>
        <div class="flex items-center justify-end gap-3 w-full">
          <VBtn
            clz="normal"
            @click="showDeleteModal = false"
          >
            {{ t('common.cancel', '取消') }}
          </VBtn>
          <VBtn
            clz="danger"
            :loading="actionLoading"
            @click="handleConfirmDelete"
          >
            {{ t('common.delete', '彻底删除') }}
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
  ShieldOutline,
  BanOutline,
  CheckmarkCircleOutline,
  FlashOutline,
  HammerOutline,
  AddOutline,
  RefreshOutline,
  PulseOutline,
  SearchOutline
} from '@vicons/ionicons5'
import VBtn from '@/components/common/VBtn.vue'
import VTable from '@/components/common/VTable.vue'
import VModal from '@/components/common/VModal.vue'
import VInput from '@/components/common/VInput.vue'
import VSelection from '@/components/common/VSelection.vue'
import VSearch from '@/components/common/VSearch.vue'
import VPagination from '@/components/common/VPagination.vue'
import type {
  AdminIpRuleVO,
  AdminIpRuleCreateDTO,
  AdminIpDiagnosisVO,
  AdminRiskStatisticsVO,
  IpRuleType,
  IpRuleSource,
  IpRuleStatus
} from '@/types/risk'
import {
  listIpRulesApi,
  createIpRuleApi,
  revokeIpRuleApi,
  deleteIpRuleApi,
  diagnoseIpApi,
  getRiskStatisticsApi
} from '@/api/risk'

defineOptions({
  name: 'AdminRiskIpView'
})

const { t } = useI18n()
const message = useMessage()

const statistics = reactive<AdminRiskStatisticsVO>({
  totalBannedCount: 0,
  activeBannedCount: 0,
  totalWhitelistedCount: 0,
  todayAutoBannedCount: 0,
  todayManualBannedCount: 0
})

const loading = ref(false)
const ruleList = ref<AdminIpRuleVO[]>([])
const searchKeyword = ref('')
const filterRuleType = ref<IpRuleType | undefined>(undefined)
const filterSourceType = ref<IpRuleSource | undefined>(undefined)
const filterStatus = ref<IpRuleStatus | undefined>(undefined)

const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

const ruleTypeOptions = [
  { label: '全部类型', value: '' },
  { label: '黑名单 (拦截)', value: 'BLACKLIST' },
  { label: '白名单 (放行)', value: 'WHITELIST' }
]

const sourceTypeOptions = [
  { label: '全部来源', value: '' },
  { label: '人工处置', value: 'MANUAL' },
  { label: '系统自动触发', value: 'AUTO_TRIGGER' }
]

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '生效中', value: 'ACTIVE' },
  { label: '已过期', value: 'EXPIRED' },
  { label: '已解除', value: 'REVOKED' }
]

const addRuleTypeOptions = [
  { label: '黑名单 (BLACKLIST)', value: 'BLACKLIST' },
  { label: '白名单 (WHITELIST)', value: 'WHITELIST' }
]

const showAddModal = ref(false)
const addSubmitting = ref(false)
const addDurationStr = ref('0')
const addFormData = reactive<AdminIpRuleCreateDTO>({
  ip: '',
  ruleType: 'BLACKLIST',
  reason: '',
  durationMinutes: 0
})

const showDiagnoseModal = ref(false)
const probeLoading = ref(false)
const probeIp = ref('')
const diagnosisResult = ref<AdminIpDiagnosisVO | null>(null)

const showRevokeModal = ref(false)
const showDeleteModal = ref(false)
const actionLoading = ref(false)
const targetRule = ref<AdminIpRuleVO | null>(null)

const columns = computed<DataTableColumns<AdminIpRuleVO>>(() => [
  {
    title: 'IP 地址 / CIDR',
    key: 'ip',
    width: 170,
    render(row) {
      return h('span', { class: 'font-mono text-xs font-semibold text-google-text-primary dark:text-google-text-darkPrimary' }, row.ip)
    }
  },
  {
    title: '规则类型',
    key: 'ruleType',
    width: 110,
    render(row) {
      return row.ruleType === 'BLACKLIST' ? '黑名单' : '白名单'
    }
  },
  {
    title: '触发来源',
    key: 'sourceType',
    width: 120,
    render(row) {
      return row.sourceType === 'AUTO_TRIGGER' ? '自动触发' : '人工处置'
    }
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render(row) {
      return row.status === 'ACTIVE' ? '生效中' : row.status === 'REVOKED' ? '已解除' : '已过期'
    }
  },
  {
    title: '处置原因 / 备注',
    key: 'reason',
    ellipsis: { tooltip: true },
    render(row) {
      return h('span', { class: 'text-xs text-google-text-secondary dark:text-google-text-darkSecondary' }, row.reason || '-')
    }
  },
  {
    title: '到期时间',
    key: 'expiresAt',
    width: 160,
    render(row) {
      return h('span', { class: 'text-xs font-mono text-google-text-secondary dark:text-google-text-darkSecondary' }, row.expiresAt || '永久有效')
    }
  },
  {
    title: '创建时间',
    key: 'createdAt',
    width: 160,
    render(row) {
      return h('span', { class: 'text-xs font-mono text-google-text-secondary dark:text-google-text-darkSecondary' }, row.createdAt || '-')
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 140,
    render(row) {
      const buttons = []
      if (row.status === 'ACTIVE') {
        buttons.push(
          h(
            VBtn,
            {
              clz: 'normal',
              size: 'small',
              onClick: () => {
                targetRule.value = row
                showRevokeModal.value = true
              }
            },
            () => '解封'
          )
        )
      }
      buttons.push(
        h(
          VBtn,
          {
            clz: 'danger',
            size: 'small',
            onClick: () => {
              targetRule.value = row
              showDeleteModal.value = true
            }
          },
          () => '删除'
        )
      )
      return h('div', { class: 'flex items-center gap-1.5' }, buttons)
    }
  }
])

function getRiskBadgeClass(level: string) {
  switch (level?.toUpperCase()) {
    case 'BANNED':
      return 'bg-red-500 text-white'
    case 'HIGH':
      return 'bg-orange-500 text-white'
    case 'MEDIUM':
      return 'bg-amber-500 text-white'
    default:
      return 'bg-green-500 text-white'
  }
}

async function fetchStatistics() {
  try {
    const res = await getRiskStatisticsApi()
    if (res.data) {
      Object.assign(statistics, res.data)
    }
  } catch (err) {
    message.error(err instanceof Error ? err.message : '获取风控统计失败')
  }
}

async function loadRules() {
  loading.value = true
  try {
    const res = await listIpRulesApi({
      page: pagination.page,
      size: pagination.pageSize,
      ip: searchKeyword.value || undefined,
      ruleType: filterRuleType.value || undefined,
      sourceType: filterSourceType.value || undefined,
      status: filterStatus.value || undefined
    })
    ruleList.value = res.data.content || []
    pagination.itemCount = res.data.totalElements || 0
  } catch (err) {
    message.error(err instanceof Error ? err.message : '加载 IP 规则列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  loadRules()
}

function handleReset() {
  searchKeyword.value = ''
  filterRuleType.value = undefined
  filterSourceType.value = undefined
  filterStatus.value = undefined
  pagination.page = 1
  loadRules()
}

function handlePageChange(page: number) {
  pagination.page = page
  loadRules()
}

function handlePageSizeChange(size: number) {
  pagination.pageSize = size
  pagination.page = 1
  loadRules()
}

function openAddModal() {
  addFormData.ip = ''
  addFormData.ruleType = 'BLACKLIST'
  addFormData.reason = ''
  addDurationStr.value = '0'
  showAddModal.value = true
}

async function handleSubmitAdd() {
  if (!addFormData.ip.trim()) {
    message.warning('请输入 IP 地址或 CIDR 段')
    return
  }
  addSubmitting.value = true
  try {
    const parsedDuration = parseInt(addDurationStr.value, 10)
    await createIpRuleApi({
      ip: addFormData.ip.trim(),
      ruleType: addFormData.ruleType,
      reason: addFormData.reason?.trim() || undefined,
      durationMinutes: isNaN(parsedDuration) ? 0 : parsedDuration
    })
    message.success('IP 规则创建成功')
    showAddModal.value = false
    loadRules()
    fetchStatistics()
  } catch (err) {
    message.error(err instanceof Error ? err.message : '创建 IP 规则失败')
  } finally {
    addSubmitting.value = false
  }
}

function openDiagnoseModal() {
  probeIp.value = ''
  diagnosisResult.value = null
  showDiagnoseModal.value = true
}

async function handleExecuteProbe() {
  if (!probeIp.value.trim()) {
    message.warning('请输入待探测 IP 地址')
    return
  }
  probeLoading.value = true
  try {
    const res = await diagnoseIpApi(probeIp.value.trim())
    diagnosisResult.value = res.data
  } catch (err) {
    message.error(err instanceof Error ? err.message : '执行 IP 探测诊断失败')
  } finally {
    probeLoading.value = false
  }
}

async function handleConfirmRevoke() {
  if (!targetRule.value) return
  actionLoading.value = true
  try {
    await revokeIpRuleApi(targetRule.value.id)
    message.success('已成功解除 IP 规则限制')
    showRevokeModal.value = false
    targetRule.value = null
    loadRules()
    fetchStatistics()
  } catch (err) {
    message.error(err instanceof Error ? err.message : '解封操作失败')
  } finally {
    actionLoading.value = false
  }
}

async function handleConfirmDelete() {
  if (!targetRule.value) return
  actionLoading.value = true
  try {
    await deleteIpRuleApi(targetRule.value.id)
    message.success('已彻底删除该 IP 规则')
    showDeleteModal.value = false
    targetRule.value = null
    loadRules()
    fetchStatistics()
  } catch (err) {
    message.error(err instanceof Error ? err.message : '删除规则失败')
  } finally {
    actionLoading.value = false
  }
}

onMounted(() => {
  fetchStatistics()
  loadRules()
})
</script>
