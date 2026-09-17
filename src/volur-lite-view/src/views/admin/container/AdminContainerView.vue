<template>
  <div class="admin-container-view">
    <!-- Header Section -->
    <div class="page-header">
      <h2 class="page-title">{{ t('admin.containers.title') }}</h2>
      <p class="page-desc">{{ t('admin.containers.desc') }}</p>
    </div>

    <!-- Statistics Ribbon -->
    <div class="stat-ribbon">
      <div class="stat-mini-card">
        <div class="stat-icon-wrapper primary">
          <n-icon size="18">
            <CubeOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.totalInstances }}</div>
          <div class="stat-label">{{ t('admin.containers.stats.total') }}</div>
        </div>
      </div>

      <div class="stat-mini-card">
        <div class="stat-icon-wrapper success">
          <n-icon size="18">
            <PlayCircleOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.runningCount }}</div>
          <div class="stat-label">{{ t('admin.containers.stats.running') }}</div>
        </div>
      </div>

      <div class="stat-mini-card">
        <div class="stat-icon-wrapper warning">
          <n-icon size="18">
            <TimeOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.startingCount }}</div>
          <div class="stat-label">{{ t('admin.containers.stats.starting') }}</div>
        </div>
      </div>

      <div class="stat-mini-card">
        <div class="stat-icon-wrapper error">
          <n-icon size="18">
            <AlertCircleOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.failedCount }}</div>
          <div class="stat-label">{{ t('admin.containers.stats.failed') }}</div>
        </div>
      </div>

      <div class="stat-mini-card">
        <div class="stat-icon-wrapper info">
          <n-icon size="18">
            <CalendarOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.todayCreatedCount }}</div>
          <div class="stat-label">{{ t('admin.containers.stats.today') }}</div>
        </div>
      </div>

      <div class="stat-mini-card">
        <div class="stat-icon-wrapper secondary">
          <n-icon size="18">
            <ServerOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value engine-badge">
            <n-tag size="small" :type="statistics.activeContainerType === 'DOCKER_SWARM' ? 'warning' : 'info'" round>
              {{ statistics.activeContainerType || 'K3S' }}
            </n-tag>
          </div>
          <div class="stat-label">{{ t('admin.containers.stats.activeType') }}</div>
        </div>
      </div>
    </div>

    <!-- Main Table Card -->
    <AppTableCard
      :bordered="true"
      :table-bordered="true"
      :scroll-x="1650"
      remote
      :columns="columns"
      :data="containerList"
      :loading="loading"
      :pagination="pagination"
      :row-key="(row: AdminContainerInstanceVO) => String(row.id)"
      v-model:checked-row-keys="selectedRowKeys"
      table-class="container-data-table"
      @update:sorter="handleSorterChange"
      @update:page="handlePageChange"
      @update:page-size="handlePageSizeChange"
    >
      <template #filter-left>
        <div class="container-filter-left">
          <n-input
            v-model:value="searchKeyword"
            :placeholder="t('admin.containers.search.placeholder')"
            clearable
            class="filter-search-input"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <n-icon :component="SearchOutline" />
            </template>
          </n-input>

          <n-select
            v-model:value="filterStatus"
            :placeholder="t('admin.containers.search.statusAll')"
            clearable
            :options="statusOptions"
            class="filter-status-select"
            @update:value="handleSearch"
          />

          <div class="filter-actions-group">
            <n-button type="primary" class="theme-primary-btn" @click="handleSearch">
              <template #icon>
                <n-icon :component="SearchOutline" />
              </template>
              {{ t('admin.containers.search.refresh') }}
            </n-button>

            <n-button quaternary @click="handleReset">
              <template #icon>
                <n-icon :component="RefreshOutline" />
              </template>
              {{ t('admin.containers.search.reset') }}
            </n-button>
          </div>
        </div>
      </template>

      <template #filter-right>
        <div class="container-filter-right">
          <n-button type="primary" class="theme-primary-btn action-start-btn" @click="openManualStartModal">
            <template #icon>
              <n-icon :component="AddOutline" />
            </template>
            {{ t('admin.containers.search.manualStart') }}
          </n-button>

          <n-popconfirm
            v-if="selectedRowKeys.length > 0"
            @positive-click="handleBatchStop"
          >
            <template #trigger>
              <n-button type="error" ghost class="action-batch-btn">
                <template #icon>
                  <n-icon :component="TrashOutline" />
                </template>
                {{ t('admin.containers.search.batchStop') }} ({{ selectedRowKeys.length }})
              </n-button>
            </template>
            {{ t('admin.containers.search.batchStopConfirmContent', { count: selectedRowKeys.length }) }}
          </n-popconfirm>
        </div>
      </template>
    </AppTableCard>

    <!-- Renew Duration Modal -->
    <n-modal
      v-model:show="showRenewModal"
      preset="card"
      :title="t('admin.containers.actions.renewTitle')"
      style="width: 440px;"
      :mask-closable="false"
    >
      <div class="renew-modal-content">
        <p class="modal-subtext">
          {{ currentRenewRow ? `${currentRenewRow.challengeTitle} (${currentRenewRow.containerId})` : '' }}
        </p>

        <n-form label-placement="left" label-width="100">
          <n-form-item :label="t('admin.containers.actions.renewDuration')">
            <n-select
              v-model:value="renewDuration"
              :options="renewDurationOptions"
              style="width: 100%;"
            />
          </n-form-item>
        </n-form>
      </div>

      <template #footer>
        <div class="modal-footer">
          <n-button @click="showRenewModal = false">
            {{ t('common.cancel') || '取消' }}
          </n-button>
          <n-button type="primary" class="theme-primary-btn" :loading="renewLoading" @click="handleConfirmRenew">
            {{ t('common.confirm') || '确认续期' }}
          </n-button>
        </div>
      </template>
    </n-modal>

    <!-- Manual Start Container Modal (Docker Desktop Style) -->
    <n-modal
      v-model:show="showManualStartModal"
      preset="card"
      :title="t('admin.containers.actions.manualStartTitle')"
      style="width: 760px; max-width: 95vw;"
      :mask-closable="false"
      class="manual-start-modal"
    >
      <div class="modal-intro-header">
        <p class="modal-subtext">
          {{ t('admin.containers.actions.manualStartDesc') }}
        </p>
        <div class="engine-indicator" style="margin-top: 8px;">
          <n-tag size="small" :type="statistics.activeContainerType === 'DOCKER_SWARM' ? 'warning' : 'info'" round>
            当前调度集群引擎: {{ statistics.activeContainerType === 'DOCKER_SWARM' ? 'Docker Swarm' : 'K3s' }}
          </n-tag>
        </div>
      </div>

      <n-form label-placement="top" class="manual-container-form">
        <!-- Section 1: Basic Info -->
        <div class="form-section-card">
          <div class="section-title">
            <n-icon size="16"><CubeOutline /></n-icon>
            <span>基础配置</span>
          </div>

          <n-grid :cols="24" :x-gap="16">
            <n-grid-item :span="24">
              <n-form-item :label="t('admin.containers.actions.imageLabel')" required>
                <n-select
                  v-model:value="manualStartForm.imageName"
                  filterable
                  tag
                  :options="imageOptions"
                  :loading="imagesLoading"
                  :placeholder="t('admin.containers.actions.imagePlaceholder')"
                  @update:value="handleImageSelected"
                />
                <template #feedback>
                  <span class="text-muted" style="font-size: 12px; color: #888;">
                    提示：建议优先选取下拉列表中本地宿主机已就绪的镜像；若输入自定义镜像，请确保已预先执行 docker pull。
                  </span>
                </template>
              </n-form-item>
            </n-grid-item>

            <n-grid-item :span="12">
              <n-form-item :label="t('admin.containers.actions.containerNameLabel')">
                <n-input
                  v-model:value="manualStartForm.containerName"
                  :placeholder="t('admin.containers.actions.containerNamePlaceholder')"
                />
              </n-form-item>
            </n-grid-item>

            <n-grid-item :span="12">
              <n-form-item :label="t('admin.containers.actions.targetNodeLabel')">
                <n-select
                  v-model:value="manualStartForm.nodeIp"
                  clearable
                  :options="candidateNodeOptions"
                  :loading="candidateNodesLoading"
                  :placeholder="t('admin.containers.actions.targetNodePlaceholder')"
                />
              </n-form-item>
            </n-grid-item>

            <n-grid-item :span="12">
              <n-form-item :label="t('admin.containers.actions.outboundLabel') || '出网权限 (访问外网)'">
                <div class="switch-row">
                  <n-switch v-model:value="manualStartForm.outboundEnabled" />
                  <span class="field-hint">
                    {{ manualStartForm.outboundEnabled ? (t('admin.containers.actions.outboundAllowed') || '允许出网 (外部互联)') : (t('admin.containers.actions.outboundBlocked') || '禁止出网 (网络隔离)') }}
                  </span>
                </div>
              </n-form-item>
            </n-grid-item>
          </n-grid>
        </div>

        <!-- Section 2: Ports -->
        <div class="form-section-card">
          <div class="section-header-row">
            <div class="section-title">
              <n-icon size="16"><ServerOutline /></n-icon>
              <span>{{ t('admin.containers.actions.portsSection') }}</span>
              <span class="section-tip">{{ t('admin.containers.actions.portsDesc') }}</span>
            </div>
            <n-button size="tiny" dashed type="primary" @click="addPortRow">
              <template #icon><n-icon :component="AddOutline" /></template>
              {{ t('admin.containers.actions.addPort') }}
            </n-button>
          </div>

          <div v-if="manualStartForm.ports.length === 0" class="empty-section-tip">
            {{ t('admin.containers.actions.noPorts') }}
          </div>
          <div v-else class="dynamic-rows-wrapper">
            <div
              v-for="(port, idx) in manualStartForm.ports"
              :key="idx"
              class="dynamic-config-row"
            >
              <div class="row-field">
                <span class="field-label-mini">{{ t('admin.containers.actions.hostPort') }}</span>
                <n-input-number
                  v-model:value="port.hostPort"
                  :min="1"
                  :max="65535"
                  :placeholder="t('admin.containers.actions.hostPortPlaceholder')"
                  style="width: 100%;"
                />
              </div>
              <span class="row-arrow">:</span>
              <div class="row-field">
                <span class="field-label-mini">{{ t('admin.containers.actions.containerPort') }} *</span>
                <n-input-number
                  v-model:value="port.containerPort"
                  :min="1"
                  :max="65535"
                  :placeholder="t('admin.containers.actions.containerPortPlaceholder')"
                  style="width: 100%;"
                />
              </div>
              <div class="row-field proto-field">
                <span class="field-label-mini">{{ t('admin.containers.actions.protocol') }}</span>
                <n-select
                  v-model:value="port.protocol"
                  :options="protocolOptions"
                  style="width: 90px;"
                />
              </div>
              <n-button
                size="small"
                quaternary
                circle
                type="error"
                class="remove-row-btn"
                @click="removePortRow(idx)"
              >
                <template #icon><n-icon :component="TrashOutline" /></template>
              </n-button>
            </div>
          </div>
        </div>

        <!-- Section 3: Volumes -->
        <div class="form-section-card">
          <div class="section-header-row">
            <div class="section-title">
              <n-icon size="16"><CubeOutline /></n-icon>
              <span>{{ t('admin.containers.actions.volumesSection') }}</span>
              <span class="section-tip">{{ t('admin.containers.actions.volumesDesc') }}</span>
            </div>
            <n-button size="tiny" dashed type="primary" @click="addVolumeRow">
              <template #icon><n-icon :component="AddOutline" /></template>
              {{ t('admin.containers.actions.addVolume') }}
            </n-button>
          </div>

          <div v-if="manualStartForm.volumes.length === 0" class="empty-section-tip">
            {{ t('admin.containers.actions.noVolumes') }}
          </div>
          <div v-else class="dynamic-rows-wrapper">
            <div
              v-for="(vol, idx) in manualStartForm.volumes"
              :key="idx"
              class="dynamic-config-row"
            >
              <div class="row-field flex-2">
                <span class="field-label-mini">{{ t('admin.containers.actions.hostPath') }}</span>
                <n-input
                  v-model:value="vol.hostPath"
                  :placeholder="t('admin.containers.actions.hostPathPlaceholder')"
                />
              </div>
              <span class="row-arrow">-></span>
              <div class="row-field flex-2">
                <span class="field-label-mini">{{ t('admin.containers.actions.containerPath') }}</span>
                <n-input
                  v-model:value="vol.containerPath"
                  :placeholder="t('admin.containers.actions.containerPathPlaceholder')"
                />
              </div>
              <div class="row-field mode-field">
                <span class="field-label-mini">{{ t('admin.containers.actions.mode') }}</span>
                <n-select
                  v-model:value="vol.mode"
                  :options="volumeModeOptions"
                  style="width: 110px;"
                />
              </div>
              <n-button
                size="small"
                quaternary
                circle
                type="error"
                class="remove-row-btn"
                @click="removeVolumeRow(idx)"
              >
                <template #icon><n-icon :component="TrashOutline" /></template>
              </n-button>
            </div>
          </div>
        </div>

        <!-- Section 4: Environment Variables -->
        <div class="form-section-card">
          <div class="section-header-row">
            <div class="section-title">
              <n-icon size="16"><TerminalOutline /></n-icon>
              <span>{{ t('admin.containers.actions.envSection') }}</span>
              <span class="section-tip">{{ t('admin.containers.actions.envDesc') }}</span>
            </div>
            <n-button size="tiny" dashed type="primary" @click="addEnvRow">
              <template #icon><n-icon :component="AddOutline" /></template>
              {{ t('admin.containers.actions.addEnv') }}
            </n-button>
          </div>

          <div v-if="manualStartForm.environments.length === 0" class="empty-section-tip">
            {{ t('admin.containers.actions.noEnvs') }}
          </div>
          <div v-else class="dynamic-rows-wrapper">
            <div
              v-for="(env, idx) in manualStartForm.environments"
              :key="idx"
              class="dynamic-config-row"
            >
              <div class="row-field flex-2">
                <span class="field-label-mini">{{ t('admin.containers.actions.envName') }}</span>
                <n-input
                  v-model:value="env.name"
                  :placeholder="t('admin.containers.actions.envNamePlaceholder')"
                />
              </div>
              <span class="row-arrow">=</span>
              <div class="row-field flex-3">
                <span class="field-label-mini">{{ t('admin.containers.actions.envValue') }}</span>
                <n-input
                  v-model:value="env.value"
                  :placeholder="t('admin.containers.actions.envValuePlaceholder')"
                />
              </div>
              <n-button
                size="small"
                quaternary
                circle
                type="error"
                class="remove-row-btn"
                @click="removeEnvRow(idx)"
              >
                <template #icon><n-icon :component="TrashOutline" /></template>
              </n-button>
            </div>
          </div>
        </div>

        <!-- Section 5: Advanced Options (CPU, Memory, Duration, Custom Flag, User) -->
        <n-collapse>
          <n-collapse-item :title="t('admin.containers.actions.resourcesSection')" name="advanced">
            <n-grid :cols="24" :x-gap="16">
              <n-grid-item :span="8">
                <n-form-item :label="t('admin.containers.actions.cpuLimit')">
                  <n-input-number
                    v-model:value="manualStartForm.cpuLimit"
                    :min="1"
                    :max="16"
                    style="width: 100%;"
                  />
                </n-form-item>
              </n-grid-item>

              <n-grid-item :span="8">
                <n-form-item :label="t('admin.containers.actions.memoryLimit')">
                  <n-input-number
                    v-model:value="manualStartForm.memoryLimit"
                    :min="64"
                    :max="16384"
                    :step="64"
                    style="width: 100%;"
                  />
                </n-form-item>
              </n-grid-item>

              <n-grid-item :span="8">
                <n-form-item :label="t('admin.containers.actions.durationLabel')">
                  <n-input-number
                    v-model:value="manualStartForm.durationMinutes"
                    :min="5"
                    :max="1440"
                    :step="15"
                    style="width: 100%;"
                  />
                </n-form-item>
              </n-grid-item>

              <n-grid-item :span="12">
                <n-form-item :label="t('admin.containers.actions.userIdLabel')">
                  <n-input
                    v-model:value="manualStartForm.userId"
                    :placeholder="t('admin.containers.actions.userIdPlaceholder')"
                  />
                </n-form-item>
              </n-grid-item>

              <n-grid-item :span="12">
                <n-form-item :label="t('admin.containers.actions.customFlagLabel')">
                  <n-input
                    v-model:value="manualStartForm.customFlag"
                    :placeholder="t('admin.containers.actions.customFlagPlaceholder')"
                  />
                </n-form-item>
              </n-grid-item>
            </n-grid>
          </n-collapse-item>
        </n-collapse>
      </n-form>

      <template #footer>
        <div class="modal-footer">
          <n-button @click="showManualStartModal = false">
            {{ t('common.cancel') || '取消' }}
          </n-button>
          <n-button
            type="primary"
            class="theme-primary-btn"
            :loading="manualStartLoading"
            @click="handleConfirmManualStart"
          >
            {{ t('common.confirm') || '运行' }}
          </n-button>
        </div>
      </template>
    </n-modal>

    <!-- Debug Terminal Modal (Web Shell) -->
    <n-modal
      v-model:show="showTerminalModal"
      preset="card"
      style="width: 880px; max-width: 96vw;"
      :mask-closable="false"
      class="terminal-modal"
    >
      <template #header>
        <div class="terminal-modal-title">
          <div class="window-controls">
            <span class="control-dot red" />
            <span class="control-dot yellow" />
            <span class="control-dot green" />
          </div>
          <span class="title-text">{{ t('admin.containers.terminal.title') }}</span>
          <n-tag size="tiny" type="info" round class="term-tag">
            {{ terminalCurrentRow?.challengeTitle || terminalCurrentRow?.containerId }}
          </n-tag>
        </div>
      </template>

      <div class="terminal-container">
        <!-- Quick Command Toolbar -->
        <div class="terminal-toolbar">
          <span class="toolbar-label">{{ t('admin.containers.terminal.quickCommands') }}:</span>
          <div class="quick-cmd-list">
            <n-button
              v-for="cmd in quickCommands"
              :key="cmd"
              size="tiny"
              quaternary
              class="quick-cmd-btn"
              :disabled="terminalLoading"
              @click="handleExecuteCommand(cmd)"
            >
              {{ cmd }}
            </n-button>
          </div>
          <div class="toolbar-right">
            <n-button size="tiny" quaternary @click="handleClearTerminal">
              {{ t('admin.containers.terminal.clear') }}
            </n-button>
          </div>
        </div>

        <!-- Terminal Output Window -->
        <div ref="terminalBodyRef" class="terminal-screen">
          <div
            v-for="(log, idx) in terminalLogs"
            :key="idx"
            :class="['term-log-entry', log.type]"
          >
            <template v-if="log.type === 'system'">
              <div class="system-msg">{{ log.text }}</div>
            </template>
            <template v-else-if="log.type === 'command'">
              <div class="command-line">
                <span class="term-prompt">{{ log.prompt || terminalPrompt }}</span>
                <span class="cmd-text">{{ log.text }}</span>
              </div>
            </template>
            <template v-else>
              <pre class="term-output" :class="{ error: log.type === 'error' }">{{ log.text }}</pre>
              <div v-if="log.exitCode !== undefined || log.latencyMs !== undefined" class="cmd-meta">
                <span v-if="log.exitCode !== undefined" :class="log.exitCode === 0 ? 'exit-success' : 'exit-fail'">
                  exit: {{ log.exitCode }}
                </span>
                <span v-if="log.latencyMs !== undefined" class="meta-time">
                  {{ log.latencyMs }}ms
                </span>
              </div>
            </template>
          </div>
        </div>

        <!-- Interactive Input Line -->
        <div class="terminal-input-bar">
          <span class="term-prompt">{{ terminalPrompt }}</span>
          <input
            ref="terminalInputRef"
            v-model="terminalInput"
            class="term-input"
            :placeholder="t('admin.containers.terminal.inputPlaceholder')"
            :disabled="terminalLoading"
            @keydown.enter.prevent="() => handleExecuteCommand()"
            @keydown.up.prevent="handleHistoryUp"
            @keydown.down.prevent="handleHistoryDown"
          />
          <n-button
            size="tiny"
            type="primary"
            class="theme-primary-btn term-run-btn"
            :loading="terminalLoading"
            @click="() => handleExecuteCommand()"
          >
            {{ t('admin.containers.terminal.run') }}
          </n-button>
        </div>
      </div>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { h, ref, reactive, onMounted, onUnmounted, computed, nextTick } from 'vue'
import {
  NIcon,
  NInput,
  NInputNumber,
  NSelect,
  NButton,
  NModal,
  NForm,
  NFormItem,
  NTag,
  NPopconfirm,
  NSpace,
  NTooltip,
  NGrid,
  NGridItem,
  NCollapse,
  NCollapseItem,
  useMessage,
  type DataTableColumns,
  type SelectOption
} from 'naive-ui'
import AppTableCard from '@/components/common/AppTableCard.vue'
import {
  CubeOutline,
  PlayCircleOutline,
  TimeOutline,
  AlertCircleOutline,
  CalendarOutline,
  ServerOutline,
  SearchOutline,
  RefreshOutline,
  TrashOutline,
  CopyOutline,
  OpenOutline,
  HourglassOutline,
  AddOutline,
  TerminalOutline
} from '@vicons/ionicons5'
import { useI18n } from 'vue-i18n'
import {
  getAdminContainerStatisticsApi,
  getAdminContainerCandidateNodesApi,
  getAdminContainerListApi,
  stopAdminContainerApi,
  batchStopAdminContainersApi,
  renewAdminContainerApi,
  manualStartAdminContainerApi,
  execAdminContainerApi,
  getAdminContainerTerminalInfoApi
} from '@/api/container'
import { getAdminImageListApi } from '@/api/image'
import type {
  AdminContainerInstanceVO,
  AdminContainerStatisticsVO,
  InstanceStartupStatus,
  ContainerPortMappingDTO,
  ContainerVolumeMappingDTO,
  ContainerEnvVarDTO
} from '@/types/container'
import type { AdminClusterImageVO } from '@/types/image'
import type { AdminClusterNodeVO } from '@/types/cluster'
import { formatBytes } from '@/utils/parser'

const { t } = useI18n()
const message = useMessage()

// --- State ---
const loading = ref(false)
const searchKeyword = ref('')
const filterStatus = ref<InstanceStartupStatus | null>(null)
const selectedRowKeys = ref<string[]>([])

const statistics = reactive<AdminContainerStatisticsVO>({
  totalInstances: 0,
  runningCount: 0,
  startingCount: 0,
  failedCount: 0,
  todayCreatedCount: 0,
  activeContainerType: 'DOCKER_SWARM'
})

const containerList = ref<AdminContainerInstanceVO[]>([])

// --- Pagination ---
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
  prefix: (info: { itemCount?: number }) => `共 ${info.itemCount || 0} 个实例`
})

// --- Status Options ---
const statusOptions = computed<SelectOption[]>(() => [
  { label: t('admin.containers.search.statusRunning'), value: 'RUNNING' },
  { label: t('admin.containers.search.statusStarting'), value: 'STARTING' },
  { label: t('admin.containers.search.statusQueued'), value: 'QUEUED' },
  { label: t('admin.containers.search.statusFailed'), value: 'FAILED' },
  { label: t('admin.containers.search.statusStopped'), value: 'STOPPED' }
])

// --- Renew Modal ---
const showRenewModal = ref(false)
const renewLoading = ref(false)
const currentRenewRow = ref<AdminContainerInstanceVO | null>(null)
const renewDuration = ref<number>(30)

const renewDurationOptions = [
  { label: '15 分钟', value: 15 },
  { label: '30 分钟', value: 30 },
  { label: '60 分钟 (1小时)', value: 60 },
  { label: '120 分钟 (2小时)', value: 120 }
]

// --- Manual Start Modal State (Docker Desktop Style) ---
interface PortRow {
  hostPort: number | null
  containerPort: number | null
  protocol: 'TCP' | 'UDP'
}

interface VolumeRow {
  hostPath: string
  containerPath: string
  mode: 'rw' | 'ro'
}

interface EnvRow {
  name: string
  value: string
}

const showManualStartModal = ref(false)
const manualStartLoading = ref(false)
const imagesLoading = ref(false)
const candidateNodesLoading = ref(false)
const clusterImages = ref<AdminClusterImageVO[]>([])
const candidateNodes = ref<AdminClusterNodeVO[]>([])

const manualStartForm = reactive({
  imageName: '',
  containerName: '',
  nodeIp: '',
  userId: '',
  durationMinutes: 30,
  cpuLimit: 1,
  memoryLimit: 256,
  customFlag: '',
  outboundEnabled: true,
  ports: [] as PortRow[],
  volumes: [] as VolumeRow[],
  environments: [] as EnvRow[]
})

const protocolOptions: SelectOption[] = [
  { label: 'TCP', value: 'TCP' },
  { label: 'UDP', value: 'UDP' }
]

const volumeModeOptions: SelectOption[] = [
  { label: '读写 (rw)', value: 'rw' },
  { label: '只读 (ro)', value: 'ro' }
]

const candidateNodeOptions = computed<SelectOption[]>(() => {
  return candidateNodes.value.map(node => {
    const isWorker = (node.role || '').toUpperCase() === 'WORKER'
    return {
      label: `${node.nodeName} (${node.ipAddress}) [${isWorker ? 'Worker' : 'Master'}]`,
      value: node.ipAddress
    }
  })
})

const imageOptions = computed<SelectOption[]>(() => {
  const list: SelectOption[] = clusterImages.value.map(img => ({
    label: `${img.fullImageName} (${formatBytes(img.sizeBytes) || 'Cluster Image'})`,
    value: img.fullImageName
  }))
  if (manualStartForm.imageName && !list.some(i => i.value === manualStartForm.imageName)) {
    list.unshift({
      label: `${manualStartForm.imageName} (Custom Image)`,
      value: manualStartForm.imageName
    })
  }
  return list
})

// --- Terminal Web Shell State ---
interface TerminalLogEntry {
  type: 'system' | 'command' | 'output' | 'error'
  text: string
  prompt?: string
  exitCode?: number
  latencyMs?: number
  time: string
}

const showTerminalModal = ref(false)
const terminalLoading = ref(false)
const terminalCurrentRow = ref<AdminContainerInstanceVO | null>(null)
const terminalLogs = ref<TerminalLogEntry[]>([])
const terminalInput = ref('')
const terminalUser = ref('root')
const terminalHost = ref('container')
const terminalWorkingDir = ref('/app')
const terminalShell = ref('/bin/sh')
const terminalHistory = ref<string[]>([])
const historyIndex = ref(-1)
const terminalBodyRef = ref<HTMLElement | null>(null)
const terminalInputRef = ref<HTMLInputElement | null>(null)

const terminalPrompt = computed(() => {
  const symbol = terminalUser.value === 'root' ? '#' : '$'
  return `${terminalUser.value}@${terminalHost.value}:${terminalWorkingDir.value}${symbol}`
})

const quickCommands = [
  'cat /flag',
  'ls -la',
  'id',
  'whoami',
  'pwd',
  'env',
  'ps aux',
  'netstat -tuln'
]

// --- Clipboard Copy ---
function copyToClipboard(text: string) {
  if (!text) return
  navigator.clipboard.writeText(text).then(() => {
    message.success(t('admin.containers.actions.copySuccess'))
  }).catch(() => {
    message.error('复制失败，请手动选择复制')
  })
}

// --- Format Remaining Time ---
function formatRemainingTime(seconds: number | undefined | null): { text: string; isUrgent: boolean } {
  if (seconds === undefined || seconds === null || seconds <= 0) {
    return { text: t('admin.containers.actions.expired'), isUrgent: true }
  }
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  const isUrgent = mins < 5
  if (mins >= 60) {
    const hours = Math.floor(mins / 60)
    const remMins = mins % 60
    return { text: `${hours}h ${remMins}m`, isUrgent }
  }
  return { text: `${mins}m ${secs}s`, isUrgent }
}

// --- Table Columns ---
const columns: DataTableColumns<AdminContainerInstanceVO> = [
  {
    type: 'selection',
    width: 48
  },
  {
    title: () => t('admin.containers.table.id'),
    key: 'id',
    width: 180,
    render(row) {
      return h(
        'span',
        {
          class: 'id-text',
          style: 'cursor: pointer;',
          onClick: () => copyToClipboard(String(row.id))
        },
        [
          h(
            NTooltip,
            { trigger: 'hover' },
            {
              trigger: () => h('span', {}, String(row.id)),
              default: () => '点击复制雪花 ID'
            }
          )
        ]
      )
    }
  },
  {
    title: () => t('admin.containers.table.challenge'),
    key: 'challengeTitle',
    minWidth: 170,
    render(row) {
      const displayTitle = row.challengeTitle || row.containerName || `Container #${row.id}`
      return h('div', { class: 'challenge-cell' }, [
        h('div', { class: 'title-line' }, [
          h('span', { class: 'challenge-title' }, displayTitle),
          row.challengeCategory
            ? h(NTag, { size: 'tiny', bordered: false, style: 'margin-left: 6px;' }, { default: () => row.challengeCategory })
            : null
        ]),
        row.imageName
          ? h('div', { class: 'image-subtitle' }, [
              h(NIcon, { component: CubeOutline, size: 12, style: 'margin-right: 4px; vertical-align: -1px;' }),
              h('span', { class: 'mono-text truncate' }, row.imageName)
            ])
          : null
      ])
    }
  },
  {
    title: () => t('admin.containers.table.user'),
    key: 'username',
    minWidth: 140,
    render(row) {
      return h('div', { class: 'user-info-cell' }, [
        h('span', { class: 'user-name' }, row.username || `UID: ${row.userId}`),
        h('span', { class: 'user-uid' }, `#${row.userId}`)
      ])
    }
  },
  {
    title: () => t('admin.containers.table.containerId'),
    key: 'containerId',
    minWidth: 160,
    render(row) {
      const isSwarm = row.containerType === 'DOCKER_SWARM' || (row.containerId && row.containerId.startsWith('swarm-'))
      return h('div', { class: 'container-id-cell' }, [
        h('div', { class: 'id-line' }, [
          h(
            NTag,
            {
              size: 'tiny',
              type: isSwarm ? 'warning' : 'info',
              bordered: false,
              round: true,
              style: 'margin-right: 6px;'
            },
            { default: () => (isSwarm ? 'Swarm' : 'K3s') }
          ),
          h(
            NTooltip,
            { trigger: 'hover' },
            {
              trigger: () => h('span', { class: 'mono-text truncate' }, row.containerId || '-'),
              default: () => row.containerId
            }
          )
        ]),
        row.containerName
          ? h('div', { class: 'container-name-sub' }, row.containerName)
          : null
      ])
    }
  },
  {
    title: () => t('admin.containers.table.node'),
    key: 'nodeIp',
    minWidth: 160,
    render(row) {
      if (!row.nodeIp) return h('span', { class: 'text-muted' }, '-')
      const isWorker = (row.nodeRole || '').toUpperCase() === 'WORKER'
      return h('div', { class: 'node-info-cell' }, [
        h('div', { class: 'id-line' }, [
          h(
            NTag,
            {
              size: 'tiny',
              type: isWorker ? 'default' : 'primary',
              bordered: false,
              round: true,
              style: 'margin-right: 6px;'
            },
            { default: () => (isWorker ? 'Worker' : 'Master') }
          ),
          h(
            NTooltip,
            { trigger: 'hover' },
            {
              trigger: () => h('span', { class: 'node-name-label' }, row.nodeName || 'Node'),
              default: () => `${row.nodeName || 'Node'} (${row.nodeIp})`
            }
          )
        ]),
        h('div', { class: 'node-ip-sub mono-text' }, row.nodeIp)
      ])
    }
  },
  {
    title: () => t('admin.containers.table.accessUrl'),
    key: 'accessUrl',
    minWidth: 200,
    render(row) {
      if (!row.accessUrl) return h('span', { class: 'text-muted' }, '-')
      return h('div', { class: 'access-url-cell' }, [
        h(
          'a',
          {
            href: row.accessUrl,
            target: '_blank',
            rel: 'noopener noreferrer',
            class: 'url-link'
          },
          [
            h('span', { class: 'mono-text truncate' }, row.accessUrl),
            h(NIcon, { component: OpenOutline, size: 14, style: 'margin-left: 4px;' })
          ]
        ),
        h(
          NButton,
          {
            size: 'tiny',
            quaternary: true,
            circle: true,
            onClick: () => copyToClipboard(row.accessUrl)
          },
          { icon: () => h(NIcon, { component: CopyOutline }) }
        )
      ])
    }
  },
  {
    title: () => t('admin.containers.table.flag'),
    key: 'flag',
    minWidth: 180,
    render(row) {
      if (!row.flag) return h('span', { class: 'text-muted' }, '-')
      return h('div', { class: 'flag-cell' }, [
        h(
          NTooltip,
          { trigger: 'hover' },
          {
            trigger: () => h('span', { class: 'mono-text truncate' }, row.flag),
            default: () => row.flag
          }
        ),
        h(
          NButton,
          {
            size: 'tiny',
            quaternary: true,
            circle: true,
            onClick: () => copyToClipboard(row.flag)
          },
          { icon: () => h(NIcon, { component: CopyOutline }) }
        )
      ])
    }
  },
  {
    title: () => t('admin.containers.table.status'),
    key: 'status',
    width: 110,
    render(row) {
      if (row.isNodeOffline) {
        return h('div', { class: 'status-offline-cell' }, [
          h(NTag, { type: 'error', size: 'small', round: true }, { default: () => '节点离线' }),
          h('span', { class: 'node-offline-hint', style: 'font-size: 10.5px; color: #d03050; display: block; margin-top: 2px;' }, `[${row.nodeIp || 'Host'} 脱机]`)
        ])
      }
      let type: 'success' | 'warning' | 'error' | 'info' | 'default' = 'default'
      switch (row.status) {
        case 'RUNNING':
          type = 'success'
          break
        case 'STARTING':
          type = 'info'
          break
        case 'QUEUED':
        case 'DOWNLOADING_IMAGE':
          type = 'warning'
          break
        case 'FAILED':
          type = 'error'
          break
        default:
          type = 'default'
      }
      return h(NTag, { type, size: 'small', round: true }, { default: () => row.status })
    }
  },
  {
    title: () => t('admin.containers.table.remainingTime'),
    key: 'remainingSeconds',
    width: 120,
    render(row) {
      const { text, isUrgent } = formatRemainingTime(row.remainingSeconds)
      return h(
        'span',
        {
          class: ['remaining-time', isUrgent ? 'urgent' : 'normal']
        },
        text
      )
    }
  },
  {
    title: () => t('admin.containers.table.actions'),
    key: 'actions',
    width: 180,
    fixed: 'right',
    render(row) {
      return h(NSpace, { size: 'small' }, () => [
        h(
          NTooltip,
          { trigger: 'hover' },
          {
            trigger: () =>
              h(
                NButton,
                {
                  size: 'small',
                  quaternary: true,
                  circle: true,
                  onClick: () => openRenewModal(row)
                },
                { icon: () => h(NIcon, { component: HourglassOutline }) }
              ),
            default: () => t('admin.containers.actions.renew')
          }
        ),
        h(
          NTooltip,
          { trigger: 'hover' },
          {
            trigger: () =>
              h(
                NButton,
                {
                  size: 'small',
                  quaternary: true,
                  circle: true,
                  type: 'info',
                  onClick: () => openTerminalModal(row)
                },
                { icon: () => h(NIcon, { component: TerminalOutline }) }
              ),
            default: () => t('admin.containers.actions.terminal')
          }
        ),
        h(
          NPopconfirm,
          {
            onPositiveClick: () => handleStopInstance(row)
          },
          {
            trigger: () =>
              h(
                NTooltip,
                { trigger: 'hover' },
                {
                  trigger: () =>
                    h(
                      NButton,
                      {
                        size: 'small',
                        quaternary: true,
                        circle: true,
                        type: 'error'
                      },
                      { icon: () => h(NIcon, { component: TrashOutline }) }
                    ),
                  default: () => t('admin.containers.actions.stop')
                }
              ),
            default: () =>
              t('admin.containers.actions.stopConfirmContent', {
                name: row.challengeTitle || String(row.id)
              })
          }
        )
      ])
    }
  }
]

// --- Actions & Methods ---
async function fetchStatistics() {
  try {
    const res = await getAdminContainerStatisticsApi()
    if (res.data) {
      Object.assign(statistics, res.data)
    }
  } catch (err: unknown) {
    console.error('Failed to load container statistics', err)
  }
}

let pollTimer: ReturnType<typeof setInterval> | null = null

function checkAndStartPolling() {
  const hasPending = containerList.value.some(c =>
    c.status === 'STARTING' || c.status === 'QUEUED' || c.status === 'DOWNLOADING_IMAGE'
  )
  if (hasPending && !pollTimer) {
    pollTimer = setInterval(async () => {
      await fetchContainerList(false)
      const stillPending = containerList.value.some(c =>
        c.status === 'STARTING' || c.status === 'QUEUED' || c.status === 'DOWNLOADING_IMAGE'
      )
      if (!stillPending && pollTimer) {
        clearInterval(pollTimer)
        pollTimer = null
        fetchStatistics()
      }
    }, 3000)
  } else if (!hasPending && pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

async function fetchContainerList(showLoading = true) {
  if (showLoading) loading.value = true
  try {
    const res = await getAdminContainerListApi({
      page: pagination.page,
      size: pagination.pageSize,
      keyword: searchKeyword.value ? searchKeyword.value.trim() : undefined,
      status: filterStatus.value || undefined
    })
    if (res.data) {
      containerList.value = res.data.list || []
      pagination.itemCount = res.data.total || 0
      checkAndStartPolling()
    }
  } catch (err: unknown) {
    console.error('Failed to load container list', err)
  } finally {
    if (showLoading) loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchContainerList()
}

function handleReset() {
  searchKeyword.value = ''
  filterStatus.value = null
  pagination.page = 1
  fetchContainerList()
}

function handlePageChange(newPage: number) {
  pagination.page = newPage
  fetchContainerList()
}

function handlePageSizeChange(newSize: number) {
  pagination.pageSize = newSize
  pagination.page = 1
  fetchContainerList()
}

function handleSorterChange() {
  fetchContainerList()
}

// --- Terminate / Renew ---
async function handleStopInstance(row: AdminContainerInstanceVO) {
  try {
    await stopAdminContainerApi(row.id)
    message.success(t('admin.containers.actions.stopSuccess'))
    fetchStatistics()
    fetchContainerList()
  } catch (err: unknown) {
    console.error('Failed to stop container instance', err)
  }
}

async function handleBatchStop() {
  if (selectedRowKeys.value.length === 0) return
  try {
    await batchStopAdminContainersApi({
      instanceIds: selectedRowKeys.value
    })
    message.success(t('admin.containers.actions.batchStopSuccess'))
    selectedRowKeys.value = []
    fetchStatistics()
    fetchContainerList()
  } catch (err: unknown) {
    console.error('Failed to batch stop container instances', err)
  }
}

function openRenewModal(row: AdminContainerInstanceVO) {
  currentRenewRow.value = row
  renewDuration.value = 30
  showRenewModal.value = true
}

async function handleConfirmRenew() {
  if (!currentRenewRow.value) return
  renewLoading.value = true
  try {
    await renewAdminContainerApi(currentRenewRow.value.id, {
      durationMinutes: renewDuration.value
    })
    message.success(t('admin.containers.actions.renewSuccess'))
    showRenewModal.value = false
    fetchContainerList()
  } catch (err: unknown) {
    console.error('Failed to renew container instance', err)
  } finally {
    renewLoading.value = false
  }
}

// --- Manual Start Methods (Docker Desktop Style) ---
async function fetchClusterImages() {
  imagesLoading.value = true
  try {
    const res = await getAdminImageListApi({ size: 100 })
    if (res.data && res.data.list) {
      clusterImages.value = res.data.list
    }
  } catch (err: unknown) {
    console.error('Failed to load cluster images for modal', err)
  } finally {
    imagesLoading.value = false
  }
}

function handleImageSelected(_val: string) {
  // ClusterImage no longer carries challenge association
}

function addPortRow() {
  manualStartForm.ports.push({ hostPort: null, containerPort: 8080, protocol: 'TCP' })
}

function removePortRow(index: number) {
  manualStartForm.ports.splice(index, 1)
}

function addVolumeRow() {
  manualStartForm.volumes.push({ hostPath: '', containerPath: '', mode: 'rw' })
}

function removeVolumeRow(index: number) {
  manualStartForm.volumes.splice(index, 1)
}

function addEnvRow() {
  manualStartForm.environments.push({ name: '', value: '' })
}

function removeEnvRow(index: number) {
  manualStartForm.environments.splice(index, 1)
}

async function fetchCandidateNodes() {
  candidateNodesLoading.value = true
  try {
    const res = await getAdminContainerCandidateNodesApi()
    if (res.data) {
      candidateNodes.value = res.data
    }
  } catch (err: unknown) {
    console.error('Failed to load candidate nodes', err)
  } finally {
    candidateNodesLoading.value = false
  }
}

function openManualStartModal() {
  manualStartForm.imageName = ''
  manualStartForm.containerName = ''
  manualStartForm.nodeIp = ''
  manualStartForm.userId = ''
  manualStartForm.durationMinutes = 30
  manualStartForm.cpuLimit = 1
  manualStartForm.memoryLimit = 256
  manualStartForm.customFlag = ''
  manualStartForm.outboundEnabled = true
  manualStartForm.ports = [
    { hostPort: null, containerPort: 80, protocol: 'TCP' }
  ]
  manualStartForm.volumes = []
  manualStartForm.environments = []
  showManualStartModal.value = true
  fetchCandidateNodes()
  fetchClusterImages()
}

async function handleConfirmManualStart() {
  if (!manualStartForm.imageName || !manualStartForm.imageName.trim()) {
    message.warning(t('admin.containers.actions.imagePlaceholder'))
    return
  }

  // Filter valid ports, volumes, environments
  const validPorts: ContainerPortMappingDTO[] = manualStartForm.ports
    .filter(p => p.containerPort && p.containerPort > 0)
    .map(p => ({
      hostPort: p.hostPort ?? undefined,
      containerPort: p.containerPort as number,
      protocol: p.protocol || 'TCP'
    }))

  const validVolumes: ContainerVolumeMappingDTO[] = manualStartForm.volumes
    .filter(v => v.hostPath && v.hostPath.trim() && v.containerPath && v.containerPath.trim())
    .map(v => ({
      hostPath: v.hostPath.trim(),
      containerPath: v.containerPath.trim(),
      mode: v.mode || 'rw'
    }))

  const validEnvs: ContainerEnvVarDTO[] = manualStartForm.environments
    .filter(e => e.name && e.name.trim())
    .map(e => ({
      name: e.name.trim(),
      value: e.value != null ? e.value : ''
    }))

  manualStartLoading.value = true
  try {
    await manualStartAdminContainerApi({
      imageName: manualStartForm.imageName.trim(),
      containerName: manualStartForm.containerName ? manualStartForm.containerName.trim() : undefined,
      nodeIp: manualStartForm.nodeIp ? manualStartForm.nodeIp.trim() : undefined,
      userId: manualStartForm.userId ? manualStartForm.userId.trim() : undefined,
      durationMinutes: manualStartForm.durationMinutes || 30,
      cpuLimit: manualStartForm.cpuLimit || 1,
      memoryLimit: manualStartForm.memoryLimit || 256,
      customFlag: manualStartForm.customFlag && manualStartForm.customFlag.trim() ? manualStartForm.customFlag.trim() : undefined,
      outboundEnabled: manualStartForm.outboundEnabled,
      ports: validPorts.length > 0 ? validPorts : undefined,
      volumes: validVolumes.length > 0 ? validVolumes : undefined,
      environments: validEnvs.length > 0 ? validEnvs : undefined
    })
    message.success(t('admin.containers.actions.manualStartSuccess'))
    showManualStartModal.value = false
    fetchStatistics()
    fetchContainerList()
  } catch (err: unknown) {
    console.error('Failed to manually start container instance', err)
  } finally {
    manualStartLoading.value = false
  }
}

// --- Debug Terminal (Web Shell) Methods ---
function scrollToTerminalBottom() {
  nextTick(() => {
    if (terminalBodyRef.value) {
      terminalBodyRef.value.scrollTop = terminalBodyRef.value.scrollHeight
    }
  })
}

async function openTerminalModal(row: AdminContainerInstanceVO) {
  terminalCurrentRow.value = row
  terminalUser.value = 'root'
  terminalHost.value = row.containerId ? row.containerId.substring(0, 12) : 'container'
  terminalWorkingDir.value = '/app'
  terminalInput.value = ''
  terminalHistory.value = []
  historyIndex.value = -1
  showTerminalModal.value = true

  terminalLogs.value = [
    {
      type: 'system',
      text: `[Connecting to container ${row.containerId} (${row.containerType || 'DOCKER_SWARM'})]...`,
      time: new Date().toLocaleTimeString()
    }
  ]
  scrollToTerminalBottom()

  try {
    const res = await getAdminContainerTerminalInfoApi(row.id)
    if (res.data) {
      if (res.data.currentUser) terminalUser.value = res.data.currentUser
      if (res.data.containerShortId) terminalHost.value = res.data.containerShortId
      if (res.data.workingDir) terminalWorkingDir.value = res.data.workingDir

      if (res.data.ready) {
        terminalLogs.value.push({
          type: 'system',
          text: `Interactive shell session established as ${terminalPrompt.value}. Type commands or click quick commands below.`,
          time: new Date().toLocaleTimeString()
        })
      } else {
        terminalLogs.value.push({
          type: 'error',
          text: `[Docker Swarm 提示] ${res.data.message || '容器尚未就绪，请稍后重试'}`,
          time: new Date().toLocaleTimeString()
        })
      }
    }
  } catch (err: unknown) {
    console.error('Failed to get terminal info', err)
  }

  scrollToTerminalBottom()
  nextTick(() => {
    terminalInputRef.value?.focus()
  })
}

async function handleExecuteCommand(cmd?: string) {
  const commandToRun = (cmd !== undefined ? cmd : terminalInput.value).trim()
  if (!commandToRun || !terminalCurrentRow.value) return

  if (commandToRun === 'clear') {
    terminalLogs.value = []
    terminalInput.value = ''
    return
  }

  // Push to history
  if (!terminalHistory.value.includes(commandToRun)) {
    terminalHistory.value.push(commandToRun)
  }
  historyIndex.value = terminalHistory.value.length

  const timeStr = new Date().toLocaleTimeString()
  terminalLogs.value.push({
    type: 'command',
    prompt: terminalPrompt.value,
    text: commandToRun,
    time: timeStr
  })

  terminalInput.value = ''
  terminalLoading.value = true
  scrollToTerminalBottom()

  try {
    const res = await execAdminContainerApi(terminalCurrentRow.value.id, {
      command: commandToRun,
      workingDir: terminalWorkingDir.value,
      shell: terminalShell.value
    })
    const data = res.data
    if (data) {
      if (data.workingDir) {
        terminalWorkingDir.value = data.workingDir
      }
      if (data.currentUser) {
        terminalUser.value = data.currentUser
      }
      if (data.containerShortId) {
        terminalHost.value = data.containerShortId
      }
      if (data.output) {
        terminalLogs.value.push({
          type: 'output',
          text: data.output,
          exitCode: data.exitCode,
          latencyMs: data.executionTimeMs,
          time: new Date().toLocaleTimeString()
        })
      }
      if (data.errorOutput) {
        terminalLogs.value.push({
          type: 'error',
          text: data.errorOutput,
          exitCode: data.exitCode,
          latencyMs: data.executionTimeMs,
          time: new Date().toLocaleTimeString()
        })
      }
      if (!data.output && !data.errorOutput) {
        terminalLogs.value.push({
          type: 'output',
          text: `[Command completed with exit code ${data.exitCode ?? 0}]`,
          exitCode: data.exitCode,
          latencyMs: data.executionTimeMs,
          time: new Date().toLocaleTimeString()
        })
      }
    }
  } catch (err: unknown) {
    const errorMsg = err instanceof Error ? err.message : 'Command execution failed'
    terminalLogs.value.push({
      type: 'error',
      text: errorMsg,
      time: new Date().toLocaleTimeString()
    })
  } finally {
    terminalLoading.value = false
    scrollToTerminalBottom()
    nextTick(() => {
      terminalInputRef.value?.focus()
    })
  }
}

function handleHistoryUp() {
  if (terminalHistory.value.length === 0) return
  if (historyIndex.value > 0) {
    historyIndex.value--
    terminalInput.value = terminalHistory.value[historyIndex.value]
  } else if (historyIndex.value === -1 || historyIndex.value === terminalHistory.value.length) {
    historyIndex.value = terminalHistory.value.length - 1
    terminalInput.value = terminalHistory.value[historyIndex.value]
  }
}

function handleHistoryDown() {
  if (terminalHistory.value.length === 0) return
  if (historyIndex.value < terminalHistory.value.length - 1) {
    historyIndex.value++
    terminalInput.value = terminalHistory.value[historyIndex.value]
  } else {
    historyIndex.value = terminalHistory.value.length
    terminalInput.value = ''
  }
}

function handleClearTerminal() {
  terminalLogs.value = []
}

// --- Lifecycle ---
onMounted(() => {
  fetchStatistics()
  fetchContainerList()
})

onUnmounted(() => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
})
</script>

<style scoped lang="scss">
.admin-container-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
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

.stat-ribbon {
  display: flex;
  align-items: center;
  gap: 12px;
  overflow-x: auto;
  padding-bottom: 4px;

  .stat-mini-card {
    flex: 1;
    min-width: 160px;
    background-color: var(--n-color, #ffffff);
    border-radius: 10px;
    padding: 12px 16px;
    display: flex;
    align-items: center;
    gap: 12px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
    border: 1px solid var(--v-border-subtle, #f0f0f0);
    transition: transform 0.2s ease, box-shadow 0.2s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
    }
  }

  .stat-icon-wrapper {
    width: 36px;
    height: 36px;
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

    &.info {
      background-color: rgba(6, 182, 212, 0.1);
      color: #06b6d4;
    }

    &.secondary {
      background-color: rgba(100, 116, 139, 0.1);
      color: #64748b;
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
    }

    .engine-badge {
      display: flex;
      align-items: center;
      padding-top: 2px;
    }

    .stat-label {
      font-size: 12px;
      color: #444444;
      font-weight: 500;
      white-space: nowrap;
    }
  }
}

.id-text {
  font-family: 'Fira Code', monospace;
  font-size: 12px;
  color: #666666;
}

.challenge-cell {
  display: flex;
  align-items: center;
  gap: 4px;

  .challenge-title {
    font-weight: 600;
    color: var(--v-text-primary, #111111);
    max-width: 180px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.user-info-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;

  .user-name {
    font-weight: 600;
    font-size: 13px;
    color: var(--v-text-primary, #111111);
  }

  .user-uid {
    font-size: 11px;
    font-family: 'Fira Code', monospace;
    color: #444444;
  }
}

.node-info-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;

  .node-name-label {
    font-weight: 600;
    font-size: 13px;
    color: var(--v-text-primary, #111111);
    max-width: 140px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .node-ip-sub {
    font-size: 11px;
    font-family: 'Fira Code', monospace;
    color: #444444;
  }
}

.container-id-cell,
.access-url-cell,
.flag-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  max-width: 240px;
}

.mono-text {
  font-family: 'Fira Code', monospace;
  font-size: 12px;
}

.truncate {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.url-link {
  display: inline-flex;
  align-items: center;
  color: #2563eb;
  text-decoration: none;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;

  &:hover {
    text-decoration: underline;
  }
}

.remaining-time {
  font-family: 'Fira Code', monospace;
  font-size: 12px;
  font-weight: 600;

  &.normal {
    color: #10b981;
  }

  &.urgent {
    color: #ef4444;
  }
}

.renew-modal-content {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .modal-subtext {
    font-size: 13px;
    color: #444444;
    margin: 0;
    background-color: var(--v-bg-subtle, #f9fafb);
    padding: 8px 12px;
    border-radius: 6px;
    word-break: break-all;
  }
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.text-muted {
  color: #999999;
}

/* --- Terminal Web Shell Modal Styles --- */
.terminal-modal {
  :deep(.n-card-header) {
    background-color: #161922;
    border-bottom: 1px solid #282c37;
    padding: 12px 18px;
  }

  :deep(.n-card__content) {
    padding: 0 !important;
    background-color: #0d1117;
  }
}

.terminal-modal-title {
  display: flex;
  align-items: center;
  gap: 12px;

  .window-controls {
    display: flex;
    gap: 6px;

    .control-dot {
      width: 11px;
      height: 11px;
      border-radius: 50%;

      &.red {
        background-color: #ff5f56;
      }
      &.yellow {
        background-color: #ffbd2e;
      }
      &.green {
        background-color: #27c93f;
      }
    }
  }

  .title-text {
    font-size: 14px;
    font-weight: 600;
    color: #e6edf3;
    font-family: 'Fira Code', monospace;
  }

  .term-tag {
    font-family: 'Fira Code', monospace;
    font-size: 11px;
  }
}

.terminal-container {
  display: flex;
  flex-direction: column;
  height: 520px;
}

.terminal-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  background-color: #161b22;
  border-bottom: 1px solid #30363d;
  flex-wrap: wrap;

  .toolbar-label {
    font-size: 12px;
    color: #8b949e;
  }

  .quick-cmd-list {
    display: flex;
    align-items: center;
    gap: 6px;
    flex-wrap: wrap;
    flex: 1;

    .quick-cmd-btn {
      font-family: 'Fira Code', monospace;
      font-size: 11px;
      color: #58a6ff;
      background-color: rgba(56, 139, 253, 0.1);
      border-radius: 4px;
      padding: 2px 8px;
      height: 24px;
      transition: background-color 0.15s ease;

      &:hover {
        background-color: rgba(56, 139, 253, 0.25);
        color: #79c0ff;
      }
    }
  }

  .toolbar-right {
    display: flex;
    align-items: center;
  }
}

.terminal-screen {
  flex: 1;
  overflow-y: auto;
  padding: 14px 18px;
  font-family: 'Fira Code', 'Courier New', monospace;
  font-size: 12.5px;
  line-height: 1.6;
  color: #c9d1d9;
  background-color: #0d1117;
  display: flex;
  flex-direction: column;
  gap: 8px;

  .term-log-entry {
    word-break: break-all;

    .system-msg {
      color: #8b949e;
      font-style: italic;
      padding: 4px 0;
      white-space: pre-wrap;
    }

    .command-line {
      display: flex;
      align-items: center;
      gap: 8px;
      color: #ffffff;
      font-weight: 500;

      .cmd-text {
        color: #58a6ff;
      }
    }

    .term-output {
      margin: 2px 0 0 0;
      white-space: pre-wrap;
      word-break: break-all;
      color: #e6edf3;
      font-family: inherit;

      &.error {
        color: #f85149;
      }
    }

    .cmd-meta {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-top: 2px;
      font-size: 11px;

      .exit-success {
        color: #3fb950;
      }

      .exit-fail {
        color: #f85149;
      }

      .meta-time {
        color: #8b949e;
      }
    }
  }
}

.terminal-input-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background-color: #161b22;
  border-top: 1px solid #30363d;

  .term-prompt {
    color: #3fb950;
    font-family: 'Fira Code', monospace;
    font-size: 12px;
    white-space: nowrap;
    user-select: none;
  }

  .term-input {
    flex: 1;
    background: transparent;
    border: none;
    outline: none;
    font-family: 'Fira Code', monospace;
    font-size: 13px;
    color: #ffffff;

    &::placeholder {
      color: #484f58;
      font-size: 12px;
    }
  }

  .term-run-btn {
    height: 26px;
    padding: 0 10px;
    font-size: 12px;
  }
}

.manual-start-modal {
  .modal-intro-header {
    margin-bottom: 16px;

    .modal-subtext {
      margin: 0;
      font-size: 13px;
      color: #444444;
      line-height: 1.5;
    }
  }

  .manual-container-form {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .form-section-card {
    background-color: var(--v-card-bg-subtle, #f9fafb);
    border: 1px solid var(--v-border-subtle, #f0f0f0);
    border-radius: 8px;
    padding: 14px 16px;

    .section-header-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 12px;

      .section-title {
        margin-bottom: 0;
      }
    }

    .section-title {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;
      font-weight: 600;
      color: var(--v-text-primary, #111111);
      margin-bottom: 12px;

      .section-tip {
        font-size: 11px;
        font-weight: 400;
        color: #666666;
        margin-left: 6px;
      }
    }

    .empty-section-tip {
      font-size: 12px;
      color: #888888;
      font-style: italic;
      padding: 6px 0;
    }

    .dynamic-rows-wrapper {
      display: flex;
      flex-direction: column;
      gap: 10px;
    }

    .dynamic-config-row {
      display: flex;
      align-items: center;
      gap: 10px;

      .row-field {
        flex: 1;
        display: flex;
        flex-direction: column;
        gap: 4px;

        &.flex-2 {
          flex: 2;
        }

        &.flex-3 {
          flex: 3;
        }

        &.proto-field,
        &.mode-field {
          flex: 0 0 auto;
        }

        .field-label-mini {
          font-size: 11px;
          color: #444444;
          font-weight: 500;
        }
      }

      .row-arrow {
        font-family: 'Fira Code', monospace;
        font-size: 14px;
        font-weight: 600;
        color: #888888;
        padding-top: 16px;
      }

      .remove-row-btn {
        margin-top: 16px;
        flex-shrink: 0;
      }
    }
  }

  .modal-footer {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
  }
}

.challenge-cell {
  .title-line {
    display: flex;
    align-items: center;
  }

  .image-subtitle {
    display: flex;
    align-items: center;
    font-size: 11px;
    color: #666666;
    margin-top: 2px;
  }
}

.container-id-cell {
  .id-line {
    display: flex;
    align-items: center;
  }

  .container-name-sub {
    font-size: 11px;
    color: #444444;
    font-weight: 500;
    margin-top: 2px;
  }
}

.container-filter-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;

  .filter-search-input {
    width: 240px;
  }

  .filter-status-select {
    width: 150px;
  }

  .filter-actions-group {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.container-filter-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

@media (max-width: 768px) {
  .container-filter-left {
    flex-direction: column;
    align-items: stretch;
    width: 100%;
    gap: 10px;

    .filter-search-input,
    .filter-status-select {
      width: 100% !important;
    }

    .filter-actions-group {
      display: flex;
      width: 100%;
      gap: 10px;

      .n-button {
        flex: 1;
        justify-content: center;
      }
    }
  }

  .container-filter-right {
    display: flex;
    width: 100%;
    gap: 10px;
    margin-top: 8px;

    .action-start-btn,
    .action-batch-btn,
    .n-popconfirm {
      flex: 1;
    }

    .n-button {
      width: 100%;
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
