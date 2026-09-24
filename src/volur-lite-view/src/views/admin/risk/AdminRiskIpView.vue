<template>
  <div class="admin-risk-ip-view">
    <!-- Top section: Compact Metrics Statistics Ribbon in a single row -->
    <div class="stat-ribbon">
      <div class="stat-mini-card">
        <div class="stat-icon-wrapper warning">
          <n-icon size="18">
            <ShieldOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.totalBannedCount }}</div>
          <div class="stat-label">{{ t('admin.risk.ip.stat_total_banned') }}</div>
        </div>
      </div>

      <div class="stat-mini-card">
        <div class="stat-icon-wrapper error">
          <n-icon size="18">
            <BanOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.activeBannedCount }}</div>
          <div class="stat-label">{{ t('admin.risk.ip.stat_active_banned') }}</div>
        </div>
      </div>

      <div class="stat-mini-card">
        <div class="stat-icon-wrapper success">
          <n-icon size="18">
            <CheckmarkCircleOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.totalWhitelistedCount }}</div>
          <div class="stat-label">{{ t('admin.risk.ip.stat_total_whitelisted') }}</div>
        </div>
      </div>

      <div class="stat-mini-card">
        <div class="stat-icon-wrapper info">
          <n-icon size="18">
            <FlashOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.todayAutoBannedCount }}</div>
          <div class="stat-label">{{ t('admin.risk.ip.stat_today_auto_banned') }}</div>
        </div>
      </div>

      <div class="stat-mini-card">
        <div class="stat-icon-wrapper primary">
          <n-icon size="18">
            <HammerOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.todayManualBannedCount }}</div>
          <div class="stat-label">{{ t('admin.risk.ip.stat_today_manual_banned') }}</div>
        </div>
      </div>
    </div>

    <!-- Bottom section: Risk IP Governance Table Card -->
    <VolurTableCard
      :bordered="true"
      :table-bordered="true"
      :scroll-x="1200"
      remote
      :columns="columns"
      :data="ruleList"
      :loading="loading"
      :pagination="pagination"
      :row-key="(row: AdminIpRuleVO) => String(row.id)"
      table-class="risk-ip-table"
      @update:sorter="handleSorterChange"
      @update:page="handlePageChange"
      @update:page-size="handlePageSizeChange"
    >
      <template #filter-left>
        <div class="risk-filter-left">
          <n-input
            v-model:value="searchKeyword"
            :placeholder="t('admin.risk.ip.search_placeholder')"
            clearable
            class="filter-input-search"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <n-icon :component="SearchOutline" />
            </template>
          </n-input>

          <n-select
            v-model:value="filterRuleType"
            :placeholder="t('admin.risk.ip.filter_rule_type')"
            clearable
            :options="ruleTypeOptions"
            class="filter-select-type"
            @update:value="handleSearch"
          />

          <n-select
            v-model:value="filterSourceType"
            :placeholder="t('admin.risk.ip.filter_source_type')"
            clearable
            :options="sourceTypeOptions"
            class="filter-select-source"
            @update:value="handleSearch"
          />

          <n-select
            v-model:value="filterStatus"
            :placeholder="t('admin.risk.ip.filter_status')"
            clearable
            :options="statusOptions"
            class="filter-select-status"
            @update:value="handleSearch"
          />

          <div class="filter-action-buttons">
            <n-button type="primary" @click="handleSearch" class="theme-primary-btn">
              <template #icon>
                <n-icon :component="SearchOutline" />
              </template>
              {{ t('admin.risk.ip.query_btn') }}
            </n-button>

            <n-button quaternary @click="handleReset">
              <template #icon>
                <n-icon :component="RefreshOutline" />
              </template>
              {{ t('admin.risk.ip.reset_btn') }}
            </n-button>
          </div>
        </div>
      </template>

      <template #filter-right>
        <div class="risk-filter-right">
          <n-button secondary @click="openDiagnoseModal" class="probe-btn">
            <template #icon>
              <n-icon :component="PulseOutline" />
            </template>
            {{ t('admin.risk.ip.diagnose_btn') }}
          </n-button>

          <n-button type="primary" class="theme-primary-btn" @click="openAddModal">
            <template #icon>
              <n-icon :component="AddOutline" />
            </template>
            {{ t('admin.risk.ip.add_rule_btn') }}
          </n-button>
        </div>
      </template>
    </VolurTableCard>

    <!-- Create IP Rule Modal -->
    <n-modal
      v-model:show="showAddModal"
      preset="card"
      :title="t('admin.risk.ip.add_modal_title')"
      style="width: 520px; max-width: 92vw;"
      :mask-closable="false"
    >
      <n-form
        ref="addFormRef"
        :model="addFormData"
        :rules="addFormRules"
        label-placement="left"
        label-width="110"
        require-mark-placement="right-hanging"
      >
        <n-form-item :label="t('admin.risk.ip.add_ip')" path="ip">
          <n-input
            v-model:value="addFormData.ip"
            :placeholder="t('admin.risk.ip.add_ip_placeholder')"
          />
        </n-form-item>

        <n-form-item :label="t('admin.risk.ip.add_rule_type')" path="ruleType">
          <n-radio-group v-model:value="addFormData.ruleType">
            <n-space>
              <n-radio value="BLACKLIST">
                <n-tag type="error" size="small" round>{{ t('admin.risk.ip.type_blacklist') }}</n-tag>
              </n-radio>
              <n-radio value="WHITELIST">
                <n-tag type="success" size="small" round>{{ t('admin.risk.ip.type_whitelist') }}</n-tag>
              </n-radio>
            </n-space>
          </n-radio-group>
        </n-form-item>

        <n-form-item :label="t('admin.risk.ip.add_duration')" path="durationMinutes">
          <n-input-number
            v-model:value="addFormData.durationMinutes"
            :min="0"
            :step="60"
            :placeholder="t('admin.risk.ip.add_duration_placeholder')"
            style="width: 100%;"
          />
          <template #feedback>
            <span class="field-feedback-text">{{ t('admin.risk.ip.add_duration_help') }}</span>
          </template>
        </n-form-item>

        <n-form-item :label="t('admin.risk.ip.add_reason')" path="reason">
          <n-input
            v-model:value="addFormData.reason"
            type="textarea"
            :rows="3"
            :placeholder="t('admin.risk.ip.add_reason_placeholder')"
          />
        </n-form-item>
      </n-form>

      <template #footer>
        <div class="modal-footer">
          <n-button @click="showAddModal = false">
            {{ t('admin.risk.ip.cancel') }}
          </n-button>
          <n-button
            type="primary"
            class="theme-primary-btn"
            :loading="addSubmitting"
            @click="handleSubmitAdd"
          >
            {{ t('admin.risk.ip.confirm') }}
          </n-button>
        </div>
      </template>
    </n-modal>

    <!-- IP Real-Time Risk Probe Modal -->
    <n-modal
      v-model:show="showDiagnoseModal"
      preset="card"
      :title="t('admin.risk.ip.diagnose_modal_title')"
      style="width: 580px; max-width: 92vw;"
    >
      <div class="diagnose-modal-content">
        <div class="diagnose-input-bar">
          <n-input
            v-model:value="probeIp"
            :placeholder="t('admin.risk.ip.diagnose_input_placeholder')"
            clearable
            class="probe-input"
            @keyup.enter="handleExecuteProbe"
          >
            <template #prefix>
              <n-icon :component="SearchOutline" />
            </template>
          </n-input>
          <n-button
            type="primary"
            class="theme-primary-btn probe-action-btn"
            :loading="probeLoading"
            @click="handleExecuteProbe"
          >
            {{ t('admin.risk.ip.diagnose_query_btn') }}
          </n-button>
        </div>

        <n-spin :show="probeLoading">
          <div v-if="diagnosisResult" class="diagnose-result-card">
            <div class="result-header">
              <span class="result-title">{{ t('admin.risk.ip.diagnose_result_title') }}</span>
              <n-tag :type="getRiskTagType(diagnosisResult.riskLevel)" round size="small">
                {{ diagnosisResult.riskLevel }}
              </n-tag>
            </div>

            <n-descriptions label-placement="left" :column="1" bordered size="small">
              <n-descriptions-item :label="t('admin.risk.ip.diagnose_result_ip')">
                <span class="ip-code-text">{{ diagnosisResult.ip }}</span>
              </n-descriptions-item>

              <n-descriptions-item :label="t('admin.risk.ip.diagnose_result_status')">
                <n-space size="small">
                  <n-tag v-if="diagnosisResult.isWhitelisted" type="success" size="small">
                    {{ t('admin.risk.ip.type_whitelist') }}
                  </n-tag>
                  <n-tag v-else-if="diagnosisResult.isBypassed" type="info" size="small">
                    {{ t('admin.risk.settings.bypass_loopback') }}
                  </n-tag>
                  <n-tag v-else-if="diagnosisResult.isBanned" type="error" size="small">
                    {{ t('admin.risk.ip.status_active') }} (403 阻断)
                  </n-tag>
                  <n-tag v-else type="default" size="small">
                    正常放行
                  </n-tag>
                </n-space>
              </n-descriptions-item>

              <n-descriptions-item v-if="diagnosisResult.browserFamily" :label="t('admin.risk.ip.diagnose_result_browser')">
                <n-tag size="small" type="info">{{ diagnosisResult.browserFamily }}</n-tag>
              </n-descriptions-item>

              <n-descriptions-item v-if="diagnosisResult.deviceFingerprint" :label="t('admin.risk.ip.diagnose_result_device_fp')">
                <span class="ip-code-text">{{ diagnosisResult.deviceFingerprint }}</span>
              </n-descriptions-item>

              <n-descriptions-item v-if="diagnosisResult.tlsProtocol" :label="t('admin.risk.ip.diagnose_result_tls')">
                <n-space size="small">
                  <n-tag size="small" type="success">{{ diagnosisResult.tlsProtocol }}</n-tag>
                  <n-tag v-if="diagnosisResult.tlsAlpn" size="small">{{ diagnosisResult.tlsAlpn }}</n-tag>
                  <span v-if="diagnosisResult.tlsCipher" class="ip-code-text">{{ diagnosisResult.tlsCipher }}</span>
                </n-space>
              </n-descriptions-item>

              <n-descriptions-item :label="t('admin.risk.ip.diagnose_result_failures')">
                <span class="metric-num warning">{{ diagnosisResult.recentFailures }}</span> 次
              </n-descriptions-item>

              <n-descriptions-item :label="t('admin.risk.ip.diagnose_result_velocity')">
                <span class="metric-num info">{{ diagnosisResult.recentVelocity }}</span> req/min
              </n-descriptions-item>

              <n-descriptions-item v-if="diagnosisResult.isBanned" :label="t('admin.risk.ip.diagnose_result_expires')">
                <span>{{ diagnosisResult.banExpiresAt ? formatDateTime(diagnosisResult.banExpiresAt) : t('admin.risk.ip.permanent') }}</span>
              </n-descriptions-item>

              <n-descriptions-item v-if="diagnosisResult.banReason" :label="t('admin.risk.ip.diagnose_result_reason')">
                <span>{{ diagnosisResult.banReason }}</span>
              </n-descriptions-item>

              <n-descriptions-item v-if="diagnosisResult.banSource" :label="t('admin.risk.ip.diagnose_result_source')">
                <n-tag size="small" :type="diagnosisResult.banSource === 'AUTO_TRIGGER' ? 'warning' : 'info'">
                  {{ diagnosisResult.banSource === 'AUTO_TRIGGER' ? t('admin.risk.ip.source_auto') : t('admin.risk.ip.source_manual') }}
                </n-tag>
              </n-descriptions-item>
            </n-descriptions>
          </div>
          <div v-else-if="!probeLoading" class="diagnose-placeholder">
            <n-icon size="42" color="#cccccc">
              <PulseOutline />
            </n-icon>
            <p class="placeholder-text">{{ t('admin.risk.ip.diagnose_input_placeholder') }}</p>
          </div>
        </n-spin>
      </div>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, h, onMounted, type VNode } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  NIcon,
  NInput,
  NInputNumber,
  NSelect,
  NButton,
  NModal,
  NForm,
  NFormItem,
  NRadioGroup,
  NRadio,
  NSpace,
  NTag,
  NPopconfirm,
  NSpin,
  NDescriptions,
  NDescriptionsItem,
  useMessage,
  type DataTableColumns,
  type FormInst,
  type FormRules,
  type SelectOption
} from 'naive-ui'
import { VolurTableCard } from '@/public/volurEx'
import {
  ShieldOutline,
  BanOutline,
  CheckmarkCircleOutline,
  FlashOutline,
  HammerOutline,
  SearchOutline,
  RefreshOutline,
  AddOutline,
  PulseOutline,
  CloseCircleOutline,
  TrashOutline
} from '@vicons/ionicons5'
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

const { t, locale } = useI18n()
const message = useMessage()

// Metrics Statistics State
const statistics = reactive<AdminRiskStatisticsVO>({
  totalBannedCount: 0,
  activeBannedCount: 0,
  totalWhitelistedCount: 0,
  todayAutoBannedCount: 0,
  todayManualBannedCount: 0
})

async function fetchStatistics() {
  try {
    const res = await getRiskStatisticsApi()
    if (res.data) {
      Object.assign(statistics, res.data)
    }
  } catch {
    // Graceful fallback
  }
}

// Table & Pagination State
const loading = ref(false)
const ruleList = ref<AdminIpRuleVO[]>([])
const searchKeyword = ref('')
const filterRuleType = ref<IpRuleType | undefined>(undefined)
const filterSourceType = ref<IpRuleSource | undefined>(undefined)
const filterStatus = ref<IpRuleStatus | undefined>(undefined)
const sortBy = ref<string | undefined>(undefined)
const sortOrder = ref<'asc' | 'desc' | undefined>(undefined)

const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  showQuickJumper: true,
  pageSizes: [10, 20, 50],
  prefix: (info: { itemCount?: number }) => t('admin.risk.ip.total_prefix', { count: info.itemCount || 0 })
})

const ruleTypeOptions = computed<SelectOption[]>(() => [
  { label: t('admin.risk.ip.type_blacklist'), value: 'BLACKLIST' },
  { label: t('admin.risk.ip.type_whitelist'), value: 'WHITELIST' }
])

const sourceTypeOptions = computed<SelectOption[]>(() => [
  { label: t('admin.risk.ip.source_manual'), value: 'MANUAL' },
  { label: t('admin.risk.ip.source_auto'), value: 'AUTO_TRIGGER' }
])

const statusOptions = computed<SelectOption[]>(() => [
  { label: t('admin.risk.ip.status_active'), value: 'ACTIVE' },
  { label: t('admin.risk.ip.status_expired'), value: 'EXPIRED' },
  { label: t('admin.risk.ip.status_revoked'), value: 'REVOKED' }
])

async function fetchRuleList() {
  loading.value = true
  try {
    const res = await listIpRulesApi({
      page: pagination.page,
      size: pagination.pageSize,
      ip: searchKeyword.value.trim() || undefined,
      ruleType: filterRuleType.value,
      sourceType: filterSourceType.value,
      status: filterStatus.value,
      sortBy: sortBy.value,
      sortOrder: sortOrder.value
    })
    if (res.data) {
      ruleList.value = res.data.list || []
      pagination.itemCount = res.data.total || 0
    }
  } catch (err: unknown) {
    const errorMsg = err instanceof Error ? err.message : String(err)
    message.error(errorMsg)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchRuleList()
}

function handleReset() {
  searchKeyword.value = ''
  filterRuleType.value = undefined
  filterSourceType.value = undefined
  filterStatus.value = undefined
  sortBy.value = undefined
  sortOrder.value = undefined
  pagination.page = 1
  fetchRuleList()
}

function handlePageChange(page: number) {
  pagination.page = page
  fetchRuleList()
}

function handlePageSizeChange(pageSize: number) {
  pagination.pageSize = pageSize
  pagination.page = 1
  fetchRuleList()
}

function handleSorterChange(options: { columnKey: string; order: 'ascend' | 'descend' | false }) {
  if (!options.order) {
    sortBy.value = undefined
    sortOrder.value = undefined
  } else {
    sortBy.value = options.columnKey
    sortOrder.value = options.order === 'ascend' ? 'asc' : 'desc'
  }
  pagination.page = 1
  fetchRuleList()
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
const columns = computed<DataTableColumns<AdminIpRuleVO>>(() => [
  {
    title: t('admin.risk.ip.col_ip'),
    key: 'ip',
    minWidth: 160,
    render(row) {
      return h('span', { class: 'ip-code' }, row.ip)
    }
  },
  {
    title: t('admin.risk.ip.col_type'),
    key: 'ruleType',
    width: 130,
    render(row) {
      const isBlack = row.ruleType === 'BLACKLIST'
      return h(
        NTag,
        {
          type: isBlack ? 'error' : 'success',
          size: 'small',
          round: true
        },
        {
          default: () => isBlack ? t('admin.risk.ip.type_blacklist') : t('admin.risk.ip.type_whitelist')
        }
      )
    }
  },
  {
    title: t('admin.risk.ip.col_source'),
    key: 'sourceType',
    width: 130,
    render(row) {
      const isAuto = row.sourceType === 'AUTO_TRIGGER'
      return h(
        NTag,
        {
          type: isAuto ? 'warning' : 'info',
          size: 'small'
        },
        {
          default: () => isAuto ? t('admin.risk.ip.source_auto') : t('admin.risk.ip.source_manual')
        }
      )
    }
  },
  {
    title: t('admin.risk.ip.col_reason'),
    key: 'reason',
    minWidth: 180,
    ellipsis: { tooltip: true },
    render(row) {
      return row.reason || '-'
    }
  },
  {
    title: t('admin.risk.ip.col_status'),
    key: 'status',
    width: 100,
    render(row) {
      if (row.status === 'ACTIVE') {
        return h(
          NTag,
          {
            type: row.ruleType === 'BLACKLIST' ? 'error' : 'success',
            size: 'small'
          },
          { default: () => t('admin.risk.ip.status_active') }
        )
      }
      if (row.status === 'EXPIRED') {
        return h(NTag, { size: 'small', type: 'default' }, { default: () => t('admin.risk.ip.status_expired') })
      }
      return h(NTag, { size: 'small', type: 'default' }, { default: () => t('admin.risk.ip.status_revoked') })
    }
  },
  {
    title: t('admin.risk.ip.col_expires'),
    key: 'expiresAt',
    width: 170,
    sorter: true,
    render(row) {
      if (!row.expiresAt) {
        return h(NTag, { size: 'small', bordered: false }, { default: () => t('admin.risk.ip.permanent') })
      }
      return formatDateTime(row.expiresAt)
    }
  },
  {
    title: t('admin.risk.ip.col_operator'),
    key: 'operatorName',
    width: 130,
    render(row) {
      return row.operatorName || (row.sourceType === 'AUTO_TRIGGER' ? 'SYSTEM' : '-')
    }
  },
  {
    title: t('admin.risk.ip.col_created'),
    key: 'createdAt',
    width: 170,
    sorter: true,
    render(row) {
      return formatDateTime(row.createdAt)
    }
  },
  {
    title: t('admin.risk.ip.col_actions'),
    key: 'actions',
    width: 150,
    fixed: 'right',
    render(row) {
      const buttons: VNode[] = []

      // Revoke button (only active rules)
      if (row.status === 'ACTIVE') {
        buttons.push(
          h(
            NPopconfirm,
            {
              onPositiveClick: () => handleRevokeRule(row)
            },
            {
              trigger: () =>
                h(
                  NButton,
                  {
                    quaternary: true,
                    size: 'tiny',
                    type: 'warning'
                  },
                  {
                    icon: () => h(NIcon, null, { default: () => h(CloseCircleOutline) }),
                    default: () => t('admin.risk.ip.revoke_btn')
                  }
                ),
              default: () => t('admin.risk.ip.revoke_confirm')
            }
          )
        )
      }

      // Delete button
      buttons.push(
        h(
          NPopconfirm,
          {
            onPositiveClick: () => handleDeleteRule(row)
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
                  default: () => t('admin.risk.ip.delete_btn')
                }
              ),
            default: () => t('admin.risk.ip.delete_confirm')
          }
        )
      )

      return h(NSpace, { size: 'small' }, { default: () => buttons })
    }
  }
])

// Revoke & Delete Handlers
async function handleRevokeRule(row: AdminIpRuleVO) {
  try {
    await revokeIpRuleApi(row.id)
    message.success(t('admin.risk.ip.revoke_success'))
    fetchRuleList()
    fetchStatistics()
  } catch (err: unknown) {
    const errorMsg = err instanceof Error ? err.message : String(err)
    message.error(errorMsg)
  }
}

async function handleDeleteRule(row: AdminIpRuleVO) {
  try {
    await deleteIpRuleApi(row.id)
    message.success(t('admin.risk.ip.delete_success'))
    fetchRuleList()
    fetchStatistics()
  } catch (err: unknown) {
    const errorMsg = err instanceof Error ? err.message : String(err)
    message.error(errorMsg)
  }
}

// Add Rule Modal State & Handlers
const showAddModal = ref(false)
const addSubmitting = ref(false)
const addFormRef = ref<FormInst | null>(null)
const addFormData = reactive<AdminIpRuleCreateDTO>({
  ip: '',
  ruleType: 'BLACKLIST',
  durationMinutes: 1440,
  reason: ''
})

const addFormRules: FormRules = {
  ip: [
    {
      required: true,
      validator: (_rule, value: string) => {
        if (!value || !value.trim()) {
          return new Error(t('admin.risk.ip.add_ip_placeholder'))
        }
        return true
      },
      trigger: ['input', 'blur']
    }
  ],
  ruleType: [
    { required: true, trigger: 'change' }
  ]
}

function openAddModal() {
  addFormData.ip = ''
  addFormData.ruleType = 'BLACKLIST'
  addFormData.durationMinutes = 1440
  addFormData.reason = ''
  showAddModal.value = true
}

async function handleSubmitAdd() {
  try {
    await addFormRef.value?.validate()
  } catch {
    return
  }

  addSubmitting.value = true
  try {
    const payload: AdminIpRuleCreateDTO = {
      ip: addFormData.ip.trim(),
      ruleType: addFormData.ruleType,
      durationMinutes: addFormData.durationMinutes && addFormData.durationMinutes > 0 ? addFormData.durationMinutes : null,
      reason: addFormData.reason?.trim() || undefined
    }
    await createIpRuleApi(payload)
    message.success(t('admin.risk.ip.add_success'))
    showAddModal.value = false
    fetchRuleList()
    fetchStatistics()
  } catch (err: unknown) {
    const errorMsg = err instanceof Error ? err.message : String(err)
    message.error(errorMsg)
  } finally {
    addSubmitting.value = false
  }
}

// Probe / Diagnose Modal State & Handlers
const showDiagnoseModal = ref(false)
const probeIp = ref('')
const probeLoading = ref(false)
const diagnosisResult = ref<AdminIpDiagnosisVO | null>(null)

function openDiagnoseModal() {
  probeIp.value = ''
  diagnosisResult.value = null
  showDiagnoseModal.value = true
}

async function handleExecuteProbe() {
  if (!probeIp.value || !probeIp.value.trim()) {
    message.warning(t('admin.risk.ip.diagnose_input_placeholder'))
    return
  }

  probeLoading.value = true
  try {
    const res = await diagnoseIpApi(probeIp.value.trim())
    if (res.data) {
      diagnosisResult.value = res.data
    }
  } catch (err: unknown) {
    const errorMsg = err instanceof Error ? err.message : String(err)
    message.error(errorMsg)
  } finally {
    probeLoading.value = false
  }
}

function getRiskTagType(level?: string): 'default' | 'info' | 'success' | 'warning' | 'error' {
  if (level === 'LOW') return 'success'
  if (level === 'MEDIUM') return 'warning'
  if (level === 'HIGH' || level === 'BANNED') return 'error'
  return 'default'
}

onMounted(() => {
  fetchStatistics()
  fetchRuleList()
})
</script>

<style scoped lang="scss">
.admin-risk-ip-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px 20px;
}

/* Metrics Ribbon */
.stat-ribbon {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  margin-bottom: 4px;
}

.stat-mini-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 18px;
  background-color: var(--v-bg-card, #ffffff);
  border: 1px solid var(--v-border-subtle, #f0f0f0);
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
  transition: transform 0.15s ease, box-shadow 0.15s ease;

  &:hover {
    box-shadow: 0 4px 10px rgba(0, 0, 0, 0.06);
  }

  .stat-icon-wrapper {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    border-radius: 8px;
    flex-shrink: 0;

    &.primary {
      background-color: rgba(0, 0, 0, 0.06);
      color: #000000;
    }

    &.info {
      background-color: rgba(99, 102, 241, 0.1);
      color: #6366f1;
    }

    &.success {
      background-color: rgba(16, 185, 129, 0.1);
      color: #10b981;
    }

    &.warning {
      background-color: rgba(245, 158, 11, 0.1);
      color: #f59e0b;
    }

    &.error {
      background-color: rgba(239, 68, 68, 0.1);
      color: #ef4444;
    }
  }

  .stat-info {
    display: flex;
    flex-direction: column;
    min-width: 0;

    .stat-value {
      font-size: 18px;
      font-weight: 700;
      color: var(--v-text-title, #1a1a1a);
      line-height: 1.2;
      font-family: 'Fira Code', monospace;
    }

    .stat-label {
      font-size: 12px;
      color: #444444;
      line-height: 1.4;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }
}

/* Filter Left & Right */
.risk-filter-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;

  .filter-input-search {
    width: 190px;
    min-width: 150px;
  }

  .filter-select-type,
  .filter-select-source,
  .filter-select-status {
    width: 125px;
    flex-shrink: 0;
  }

  .filter-action-buttons {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.risk-filter-right {
  display: flex;
  align-items: center;
  gap: 10px;

  .probe-btn {
    border-color: var(--v-border-subtle, #e5e7eb);
  }
}

/* Code styling for IP */
.ip-code {
  font-family: 'Fira Code', monospace;
  font-weight: 600;
  font-size: 13px;
  color: var(--v-text-title, #1f2937);
  background-color: rgba(0, 0, 0, 0.04);
  padding: 2px 6px;
  border-radius: 4px;
}

.field-feedback-text {
  font-size: 12px;
  color: #444444;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* Probe Modal */
.diagnose-modal-content {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .diagnose-input-bar {
    display: flex;
    gap: 10px;

    .probe-input {
      flex: 1;
    }

    .probe-action-btn {
      flex-shrink: 0;
    }
  }

  .diagnose-result-card {
    display: flex;
    flex-direction: column;
    gap: 12px;
    background-color: var(--v-bg-page, #fafafa);
    padding: 16px;
    border-radius: 8px;
    border: 1px solid var(--v-border-subtle, #f0f0f0);

    .result-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .result-title {
        font-weight: 600;
        font-size: 14px;
        color: #444444;
      }
    }

    .ip-code-text {
      font-family: 'Fira Code', monospace;
      font-weight: 600;
      font-size: 13px;
    }

    .metric-num {
      font-family: 'Fira Code', monospace;
      font-weight: 700;
      font-size: 14px;

      &.warning {
        color: #f59e0b;
      }

      &.info {
        color: #6366f1;
      }
    }
  }

  .diagnose-placeholder {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 32px 0;
    gap: 10px;

    .placeholder-text {
      font-size: 13px;
      color: #444444;
      margin: 0;
    }
  }
}

/* Primary Button Design Rule: #000000 bg, #ffffff text */
.theme-primary-btn {
  background-color: #000000 !important;
  color: #ffffff !important;
  border-color: #000000 !important;

  &:hover {
    background-color: #222222 !important;
    border-color: #222222 !important;
  }

  &:active {
    background-color: #333333 !important;
  }
}

@media (max-width: 1100px) {
  .stat-ribbon {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .stat-ribbon {
    grid-template-columns: repeat(2, 1fr);
  }

  .risk-filter-left {
    width: 100%;
    flex-direction: column;
    align-items: stretch;
    gap: 10px;

    .filter-input-search,
    .filter-select-type,
    .filter-select-source,
    .filter-select-status {
      width: 100% !important;
    }

    .filter-action-buttons {
      width: 100%;
      display: flex;
      gap: 10px;

      .n-button {
        flex: 1;
        justify-content: center;
      }
    }
  }

  .risk-filter-right {
    width: 100%;
    display: flex;
    gap: 10px;

    .n-button {
      flex: 1;
      justify-content: center;
    }
  }
}
</style>
