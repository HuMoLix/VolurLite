<template>
  <div class="admin-cluster-view">
    <!-- Header Section -->
    <div class="page-header">
      <h2 class="page-title">{{ t('admin.clusters.title') }}</h2>
      <p class="page-desc">{{ t('admin.clusters.desc') }}</p>
    </div>

    <!-- Statistics Ribbon -->
    <div class="stat-ribbon">
      <div class="stat-mini-card">
        <div class="stat-icon-wrapper primary">
          <n-icon size="18">
            <ServerOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ settings.totalNodes }}</div>
          <div class="stat-label">{{ t('admin.clusters.stats.total') }}</div>
        </div>
      </div>

      <div class="stat-mini-card">
        <div class="stat-icon-wrapper warning">
          <n-icon size="18">
            <ShieldCheckmarkOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ settings.masterNodes }}</div>
          <div class="stat-label">{{ t('admin.clusters.stats.master') }}</div>
        </div>
      </div>

      <div class="stat-mini-card">
        <div class="stat-icon-wrapper info">
          <n-icon size="18">
            <HardwareChipOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ settings.workerNodes }}</div>
          <div class="stat-label">{{ t('admin.clusters.stats.worker') }}</div>
        </div>
      </div>

      <div class="stat-mini-card">
        <div class="stat-icon-wrapper success">
          <n-icon size="18">
            <CheckmarkCircleOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ settings.readyNodes }}</div>
          <div class="stat-label">{{ t('admin.clusters.stats.ready') }}</div>
        </div>
      </div>
    </div>

    <!-- Main Content Tabs Card -->
    <n-card class="content-card" :bordered="false">
      <n-tabs type="line" animated v-model:value="activeTab">
        <!-- Tab 1: 集群参数与调度设置 (移植自 ContainerSettingsTab 并全面增强) -->
        <n-tab-pane name="params" :tab="t('admin.clusters.tabs.params')">
          <div class="tab-pane-container">
            <n-form
              :model="form"
              label-placement="left"
              label-width="210"
              require-mark-placement="right-hanging"
            >
              <!-- 容器编排类型 -->
              <n-form-item :label="t('admin.clusters.form.type')" path="containerType">
                <n-select
                  v-model:value="form.containerType"
                  :options="containerTypeOptions"
                  style="max-width: 380px;"
                />
                <span class="field-hint">{{ t('admin.clusters.form.type_hint') }}</span>
              </n-form-item>

              <!-- 通用公网 Host -->
              <n-form-item :label="t('admin.clusters.form.public_host')" path="publicHost">
                <n-input
                  v-model:value="form.publicHost"
                  :placeholder="t('admin.clusters.form.public_host_placeholder')"
                  style="max-width: 380px;"
                />
                <span class="field-hint">{{ t('admin.clusters.form.public_host_hint') }}</span>
              </n-form-item>

              <!-- 端口映射范围 -->
              <n-form-item :label="t('admin.clusters.form.port_range')">
                <div class="range-inputs">
                  <n-input-number
                    v-model:value="form.portRangeStart"
                    :min="1024"
                    :max="65535"
                    style="width: 160px;"
                  />
                  <span class="range-divider">-</span>
                  <n-input-number
                    v-model:value="form.portRangeEnd"
                    :min="1024"
                    :max="65535"
                    style="width: 160px;"
                  />
                </div>
                <span class="field-hint">{{ t('admin.clusters.form.port_range_hint') }}</span>
              </n-form-item>

              <!-- Docker Swarm 专有配置展开分支 -->
              <template v-if="form.containerType === 'DOCKER_SWARM'">
                <n-divider dashed title-placement="left">Docker Swarm 集群专属参数</n-divider>

                <n-form-item :label="t('admin.clusters.form.swarm_api_url')" path="dockerSwarmApiUrl">
                  <n-input
                    v-model:value="form.dockerSwarmApiUrl"
                    :placeholder="t('admin.clusters.form.swarm_api_url_placeholder')"
                    style="max-width: 520px;"
                  />
                  <span class="field-hint">{{ t('admin.clusters.form.swarm_api_url_hint') }}</span>
                </n-form-item>

                <n-form-item :label="t('admin.clusters.form.swarm_network')" path="dockerSwarmNetwork">
                  <n-input
                    v-model:value="form.dockerSwarmNetwork"
                    :placeholder="t('admin.clusters.form.swarm_network_placeholder')"
                    style="max-width: 520px;"
                  />
                  <span class="field-hint">{{ t('admin.clusters.form.swarm_network_hint') }}</span>
                </n-form-item>

                <n-form-item :label="t('admin.clusters.form.swarm_cert_path')" path="dockerSwarmCertPath">
                  <n-input
                    v-model:value="form.dockerSwarmCertPath"
                    :placeholder="t('admin.clusters.form.swarm_cert_path_placeholder')"
                    style="max-width: 520px;"
                  />
                  <span class="field-hint">{{ t('admin.clusters.form.swarm_cert_path_hint') }}</span>
                </n-form-item>
              </template>

              <!-- K3s 专有配置展开分支 -->
              <template v-if="form.containerType === 'K3S'">
                <n-divider dashed title-placement="left">K3s / Kubernetes 集群专属参数</n-divider>

                <n-form-item :label="t('admin.clusters.form.k3s_master_url')" path="k3sMasterUrl">
                  <n-input
                    v-model:value="form.k3sMasterUrl"
                    :placeholder="t('admin.clusters.form.k3s_master_url_placeholder')"
                    style="max-width: 520px;"
                  />
                  <span class="field-hint">{{ t('admin.clusters.form.k3s_master_url_hint') }}</span>
                </n-form-item>

                <n-form-item :label="t('admin.clusters.form.k3s_namespace')" path="k3sNamespace">
                  <n-input
                    v-model:value="form.k3sNamespace"
                    :placeholder="t('admin.clusters.form.k3s_namespace_placeholder')"
                    style="max-width: 520px;"
                  />
                  <span class="field-hint">{{ t('admin.clusters.form.k3s_namespace_hint') }}</span>
                </n-form-item>

                <n-form-item :label="t('admin.clusters.form.k3s_kubeconfig')" path="k3sKubeconfigPath">
                  <n-input
                    v-model:value="form.k3sKubeconfigPath"
                    :placeholder="t('admin.clusters.form.k3s_kubeconfig_placeholder')"
                    style="max-width: 520px;"
                  />
                  <span class="field-hint">{{ t('admin.clusters.form.k3s_kubeconfig_hint') }}</span>
                </n-form-item>
              </template>

              <!-- 集群调度与主从隔离配置 -->
              <n-divider dashed title-placement="left">集群资源调度与隔离策略</n-divider>

              <n-form-item :label="t('admin.clusters.form.isolation')">
                <div class="switch-row">
                  <n-switch v-model:value="form.resourceIsolationEnabled" />
                  <span class="field-hint">{{ t('admin.clusters.form.isolation_hint') }}</span>
                </div>
              </n-form-item>

              <n-form-item :label="t('admin.clusters.form.auto_migrate')">
                <div class="switch-row">
                  <n-switch v-model:value="form.autoMigrateImagesEnabled" />
                  <span class="field-hint">{{ t('admin.clusters.form.auto_migrate_hint') }}</span>
                </div>
              </n-form-item>

              <!-- 集群连通性测试 -->
              <n-form-item :label="t('admin.clusters.form.test_connection')">
                <div class="test-connection-section">
                  <n-button
                    secondary
                    size="small"
                    :loading="testingConnection"
                    @click="handleTestConnection"
                  >
                    <template #icon>
                      <n-icon :component="ServerOutline" />
                    </template>
                    {{ t('admin.clusters.form.test_connection') }}
                  </n-button>

                  <div v-if="testResult" class="test-result-box">
                    <n-alert
                      :type="testResult.connected ? 'success' : 'warning'"
                      :title="testResult.connected ? t('admin.clusters.form.test_success') : t('admin.clusters.form.test_failed')"
                      style="margin-top: 10px; max-width: 620px;"
                    >
                      <div class="test-result-details">
                        <p class="res-msg">{{ testResult.message }}</p>
                        <div v-if="testResult.connected" class="res-badges">
                          <n-tag size="tiny" type="info" round v-if="testResult.clusterVersion">
                            {{ testResult.clusterVersion }}
                          </n-tag>
                          <n-tag size="tiny" type="success" round v-if="testResult.nodeCount !== undefined">
                            节点数: {{ testResult.nodeCount }}
                          </n-tag>
                          <n-tag size="tiny" round v-if="testResult.latencyMs !== undefined">
                            延迟: {{ testResult.latencyMs }}ms
                          </n-tag>
                        </div>
                      </div>
                    </n-alert>
                  </div>
                </div>
              </n-form-item>
            </n-form>

            <!-- Action Save Bar -->
            <div class="action-footer">
              <n-button
                type="primary"
                class="theme-primary-btn"
                :loading="savingSettings"
                @click="handleSaveSettings"
              >
                {{ t('admin.clusters.form.save_settings') }}
              </n-button>
            </div>
          </div>
        </n-tab-pane>

        <!-- Tab 2: 节点管理与集群扩展 -->
        <n-tab-pane name="nodes" :tab="t('admin.clusters.tabs.nodes')">
          <div class="nodes-tab-container">
            <!-- Filter & Action Controls -->
            <div class="nodes-toolbar">
              <div class="toolbar-title">
                <n-icon size="18" style="margin-right: 6px;">
                  <GitNetworkOutline />
                </n-icon>
                <span>{{ t('admin.clusters.nodes.title') }}</span>
              </div>
              <div class="toolbar-actions">
                <n-button quaternary size="small" :loading="loadingNodes" @click="fetchNodes">
                  <template #icon>
                    <n-icon :component="RefreshOutline" />
                  </template>
                  {{ t('admin.clusters.nodes.refresh') }}
                </n-button>
                <n-button
                  size="small"
                  secondary
                  type="info"
                  :loading="checkingAllHealth"
                  @click="handleCheckAllHealth"
                >
                  <template #icon>
                    <n-icon :component="PulseOutline" />
                  </template>
                  {{ t('admin.clusters.nodes.health_check_all') }}
                </n-button>
                <n-button
                  type="primary"
                  class="theme-primary-btn"
                  @click="openAddNodeModal"
                >
                  <template #icon>
                    <n-icon :component="AddOutline" />
                  </template>
                  {{ t('admin.clusters.nodes.add_node') }}
                </n-button>
              </div>
            </div>

            <!-- Nodes Table -->
            <n-data-table
              :columns="nodeColumns"
              :data="nodes"
              :loading="loadingNodes"
              :bordered="false"
              class="nodes-table"
            />
          </div>
        </n-tab-pane>

        <!-- Tab 3: 集群拓扑架构图 -->
        <n-tab-pane name="topology" :tab="t('admin.clusters.tabs.topology')">
          <ClusterTopology
            :nodes="nodes"
            :settings="settings"
            :loading="loadingNodes"
            @refresh="fetchNodes"
            @editNode="openEditNodeModal"
          />
        </n-tab-pane>
      </n-tabs>
    </n-card>

    <!-- Cluster Node Modal (Add / Edit) -->
    <ClusterNodeModal
      v-model:show="nodeModalVisible"
      :mode="nodeModalMode"
      :node="selectedNode"
      :cluster-type="form.containerType"
      @success="handleNodeModalSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, h } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  NCard,
  NTabs,
  NTabPane,
  NForm,
  NFormItem,
  NInput,
  NInputNumber,
  NSelect,
  NSwitch,
  NDivider,
  NButton,
  NIcon,
  NAlert,
  NTag,
  NDataTable,
  NPopconfirm,
  useMessage,
  type SelectOption,
  type DataTableColumns
} from 'naive-ui'
import {
  ServerOutline,
  ShieldCheckmarkOutline,
  HardwareChipOutline,
  CheckmarkCircleOutline,
  AddOutline,
  RefreshOutline,
  GitNetworkOutline,
  PulseOutline,
  CreateOutline,
  SyncOutline
} from '@vicons/ionicons5'
import {
  getAdminClusterSettingsApi,
  updateAdminClusterSettingsApi,
  testAdminClusterConnectionApi,
  getAdminClusterNodesApi,
  removeAdminClusterNodeApi,
  syncImagesToClusterNodeApi,
  checkAllAdminClusterNodesHealthApi,
  joinAdminClusterNodeApi
} from '@/api/cluster'
import type {
  AdminClusterSettingsVO,
  AdminClusterNodeVO,
  AdminContainerTestConnectionVO
} from '@/types/cluster'
import ClusterNodeModal from './components/ClusterNodeModal.vue'
import ClusterTopology from './components/ClusterTopology.vue'

const { t } = useI18n()
const message = useMessage()

const activeTab = ref<'params' | 'nodes' | 'topology'>('params')
const savingSettings = ref(false)
const testingConnection = ref(false)
const testResult = ref<AdminContainerTestConnectionVO | null>(null)
const joiningNodeId = ref<string | number | null>(null)

// Cluster Settings Form State
const settings = reactive<AdminClusterSettingsVO>({
  containerType: 'K3S',
  portRangeStart: 20000,
  portRangeEnd: 30000,
  publicHost: '127.0.0.1',
  dockerSwarmApiUrl: 'unix:///var/run/docker.sock',
  dockerSwarmNetwork: 'volur-swarm-network',
  dockerSwarmCertPath: '',
  k3sMasterUrl: 'https://127.0.0.1:6443',
  k3sNamespace: 'volur-challenges',
  k3sKubeconfigPath: '/etc/rancher/k3s/k3s.yaml',
  resourceIsolationEnabled: true,
  autoMigrateImagesEnabled: true,
  totalNodes: 1,
  masterNodes: 1,
  workerNodes: 0,
  readyNodes: 1
})

const form = reactive({
  containerType: 'K3S',
  portRangeStart: 20000,
  portRangeEnd: 30000,
  publicHost: '127.0.0.1',
  dockerSwarmApiUrl: 'unix:///var/run/docker.sock',
  dockerSwarmNetwork: 'volur-swarm-network',
  dockerSwarmCertPath: '',
  k3sMasterUrl: 'https://127.0.0.1:6443',
  k3sNamespace: 'volur-challenges',
  k3sKubeconfigPath: '/etc/rancher/k3s/k3s.yaml',
  resourceIsolationEnabled: true,
  autoMigrateImagesEnabled: true
})

const containerTypeOptions = computed<SelectOption[]>(() => [
  { label: t('admin.clusters.form.type_docker_swarm'), value: 'DOCKER_SWARM' },
  { label: t('admin.clusters.form.type_k3s'), value: 'K3S' }
])


// Nodes State
const nodes = ref<AdminClusterNodeVO[]>([])
const loadingNodes = ref(false)
// Unified Node Modal State
const nodeModalVisible = ref(false)
const nodeModalMode = ref<'add' | 'edit'>('add')
const selectedNode = ref<AdminClusterNodeVO | null>(null)
const checkingAllHealth = ref(false)

// Table Columns for Nodes
const nodeColumns = computed<DataTableColumns<AdminClusterNodeVO>>(() => [
  {
    title: t('admin.clusters.nodes.name'),
    key: 'nodeName',
    width: 200,
    render(row) {
      return h('div', { class: 'node-name-cell' }, [
        h('span', { class: 'name-text' }, row.nodeName),
        row.role === 'MASTER'
          ? h(NTag, { size: 'tiny', type: 'warning', round: true, style: 'margin-left: 6px;' }, { default: () => 'PLATFORM' })
          : null,
        row.hasSshAuth
          ? h(NTag, { size: 'tiny', type: 'success', round: true, style: 'margin-left: 6px;' }, { default: () => 'SSH' })
          : null
      ])
    }
  },
  {
    title: t('admin.clusters.nodes.ip'),
    key: 'ipAddress',
    width: 170,
    render(row) {
      return h('span', { class: 'mono-text' }, `${row.ipAddress}:${row.sshPort || 22}`)
    }
  },
  {
    title: t('admin.clusters.nodes.role'),
    key: 'role',
    width: 120,
    render(row) {
      const isMaster = row.role === 'MASTER'
      return h(
        NTag,
        { size: 'small', type: isMaster ? 'warning' : 'info', round: true },
        { default: () => (isMaster ? t('admin.clusters.nodes.role_master') : t('admin.clusters.nodes.role_worker')) }
      )
    }
  },
  {
    title: t('admin.clusters.nodes.is_resource'),
    key: 'isResourceNode',
    width: 150,
    render(row) {
      return h(
        NTag,
        { size: 'small', type: row.isResourceNode ? 'success' : 'default', round: true },
        { default: () => (row.isResourceNode ? t('admin.clusters.nodes.resource_yes') : t('admin.clusters.nodes.resource_no')) }
      )
    }
  },
  {
    title: t('admin.clusters.nodes.status'),
    key: 'status',
    width: 140,
    render(row) {
      const isMaster = row.role === 'MASTER'
      const inCluster = Boolean(row.inCluster) || isMaster
      const isReady = row.status === 'READY'
      const isJoining = row.status === 'JOINING'
      let tagType: 'success' | 'warning' | 'error' = isReady && inCluster ? 'success' : 'error'
      let text = isReady && inCluster ? t('admin.clusters.nodes.status_ready') : t('admin.clusters.nodes.status_offline')
      if (isJoining) {
        tagType = 'warning'
        text = '加入中...'
      } else if (isReady && !inCluster && !isMaster) {
        tagType = 'warning'
        text = '未入集群'
      } else if (!isReady && !inCluster && !isMaster) {
        tagType = 'error'
        text = '异常 / 未入网'
      }
      return h('div', { style: 'display: flex; flex-direction: column; gap: 4px;' }, [
        h(NTag, { size: 'small', type: tagType, round: true }, { default: () => text }),
        !isMaster && inCluster
          ? h(NTag, { size: 'tiny', type: 'success', bordered: false }, { default: () => '已入 Swarm' })
          : null
      ])
    }
  },
  {
    title: t('admin.clusters.nodes.containers'),
    key: 'containerCount',
    width: 100,
    render(row) {
      return h('span', { class: 'mono-text' }, row.containerCount || 0)
    }
  },
  {
    title: t('admin.clusters.nodes.images'),
    key: 'imageCount',
    width: 100,
    render(row) {
      return h('span', { class: 'mono-text' }, row.imageCount || 0)
    }
  },
  {
    title: t('admin.clusters.nodes.specs'),
    key: 'specs',
    width: 160,
    render(row) {
      const cores = row.cpuCores ? `${row.cpuCores} 核` : '-'
      const mem = row.memoryFormatted || '-'
      return h('span', { class: 'specs-text' }, `${cores} / ${mem}`)
    }
  },
  {
    title: t('admin.clusters.nodes.actions'),
    key: 'actions',
    width: 300,
    render(row) {
      if (row.role === 'MASTER') {
        return h(
          NTag,
          { size: 'small', type: 'default', bordered: false, style: 'color: #888888;' },
          { default: () => '本机控制面 (不可修改)' }
        )
      }
      return h('div', { class: 'action-btns' }, [
        !row.inCluster
          ? h(
              NButton,
              {
                size: 'tiny',
                quaternary: true,
                type: 'warning',
                loading: joiningNodeId.value === row.id,
                onClick: () => handleJoinCluster(row)
              },
              {
                icon: () => h(NIcon, { component: SyncOutline }),
                default: () => '加入集群'
              }
            )
          : null,
        h(
          NButton,
          {
            size: 'tiny',
            quaternary: true,
            type: 'primary',
            onClick: () => openEditNodeModal(row)
          },
          {
            icon: () => h(NIcon, { component: CreateOutline }),
            default: () => t('admin.clusters.nodes.edit')
          }
        ),
        h(
          NButton,
          {
            size: 'tiny',
            quaternary: true,
            type: 'info',
            onClick: () => handleSyncImagesToNode(row)
          },
          { default: () => t('admin.clusters.nodes.sync_images') }
        ),
        h(
          NPopconfirm,
          {
            onPositiveClick: () => handleRemoveNode(row),
            positiveText: '确认移除',
            negativeText: '取消'
          },
          {
            trigger: () =>
              h(
                NButton,
                { size: 'tiny', quaternary: true, type: 'error' },
                { default: () => t('admin.clusters.nodes.remove') }
              ),
            default: () => t('admin.clusters.nodes.remove_confirm')
          }
        )
      ])
    }
  }
])

async function fetchSettings() {
  try {
    const res = await getAdminClusterSettingsApi()
    if (res.data) {
      Object.assign(settings, res.data)
      Object.assign(form, {
        containerType: res.data.containerType || 'K3S',
        portRangeStart: res.data.portRangeStart || 20000,
        portRangeEnd: res.data.portRangeEnd || 30000,
        publicHost: res.data.publicHost || '127.0.0.1',
        dockerSwarmApiUrl: res.data.dockerSwarmApiUrl || 'unix:///var/run/docker.sock',
        dockerSwarmNetwork: res.data.dockerSwarmNetwork || 'volur-swarm-network',
        dockerSwarmCertPath: res.data.dockerSwarmCertPath || '',
        k3sMasterUrl: res.data.k3sMasterUrl || 'https://127.0.0.1:6443',
        k3sNamespace: res.data.k3sNamespace || 'volur-challenges',
        k3sKubeconfigPath: res.data.k3sKubeconfigPath || '/etc/rancher/k3s/k3s.yaml',
        resourceIsolationEnabled: res.data.resourceIsolationEnabled ?? true,
        autoMigrateImagesEnabled: res.data.autoMigrateImagesEnabled ?? true
      })
    }
  } catch (err) {
    console.warn('Failed to load cluster settings:', err)
  }
}

async function fetchNodes() {
  loadingNodes.value = true
  try {
    const res = await getAdminClusterNodesApi()
    if (res.data) {
      nodes.value = res.data
    }
  } catch (err) {
    console.warn('Failed to load nodes:', err)
  } finally {
    loadingNodes.value = false
  }
}

async function handleSaveSettings() {
  savingSettings.value = true
  try {
    await updateAdminClusterSettingsApi(form)
    message.success(t('admin.clusters.form.save_success'))
    await fetchSettings()
    await fetchNodes()
  } catch (err) {
    message.error('保存集群设置失败')
  } finally {
    savingSettings.value = false
  }
}

async function handleTestConnection() {
  testingConnection.value = true
  testResult.value = null
  try {
    const res = await testAdminClusterConnectionApi({
      containerType: form.containerType,
      dockerSwarmApiUrl: form.dockerSwarmApiUrl,
      dockerSwarmCertPath: form.dockerSwarmCertPath,
      k3sMasterUrl: form.k3sMasterUrl,
      k3sNamespace: form.k3sNamespace,
      k3sKubeconfigPath: form.k3sKubeconfigPath
    })
    if (res.data) {
      testResult.value = res.data
      if (res.data.connected) {
        message.success(res.data.message || t('admin.clusters.form.test_success'))
      } else {
        message.warning(res.data.message || t('admin.clusters.form.test_failed'))
      }
    }
  } catch {
    message.error(t('admin.clusters.form.test_failed'))
  } finally {
    testingConnection.value = false
  }
}

function openAddNodeModal() {
  nodeModalMode.value = 'add'
  selectedNode.value = null
  nodeModalVisible.value = true
}

async function handleCheckAllHealth() {
  checkingAllHealth.value = true
  try {
    const res = await checkAllAdminClusterNodesHealthApi()
    if (res.data) {
      nodes.value = res.data
      message.success(t('admin.clusters.nodes.health_check_all_success'))
      await fetchSettings()
    }
  } catch {
    message.error('全量存活检测失败')
  } finally {
    checkingAllHealth.value = false
  }
}

function openEditNodeModal(node: AdminClusterNodeVO) {
  if (node.role === 'MASTER') {
    message.warning('主控制节点为本机平台控制面，禁止修改')
    return
  }
  nodeModalMode.value = 'edit'
  selectedNode.value = node
  nodeModalVisible.value = true
}

async function handleNodeModalSuccess() {
  await fetchNodes()
  await fetchSettings()
  // 广播全局集群节点更新事件，通知镜像管理等模块同步刷新
  window.dispatchEvent(new CustomEvent('volur:cluster-nodes-updated'))
}

async function handleSyncImagesToNode(node: AdminClusterNodeVO) {
  if (node.role === 'MASTER') {
    message.warning('主控制节点无需同步镜像')
    return
  }
  try {
    await syncImagesToClusterNodeApi(node.id)
    message.success(t('admin.clusters.nodes.sync_success'))
    await fetchNodes()
    // 同步镜像后广播更新通知
    window.dispatchEvent(new CustomEvent('volur:cluster-nodes-updated'))
  } catch {
    message.error('同步镜像失败')
  }
}

async function handleJoinCluster(node: AdminClusterNodeVO) {
  joiningNodeId.value = node.id
  try {
    const res = await joinAdminClusterNodeApi(node.id)
    if (res.data) {
      message.success(t('admin.clusters.topology.action_rejoin_success'))
      await fetchNodes()
      await fetchSettings()
      window.dispatchEvent(new CustomEvent('volur:cluster-nodes-updated'))
    }
  } catch (err: unknown) {
    const errorMsg = err instanceof Error ? err.message : '重新加入集群失败'
    message.error(errorMsg)
  } finally {
    joiningNodeId.value = null
  }
}

async function handleRemoveNode(node: AdminClusterNodeVO) {
  if (node.role === 'MASTER') {
    message.warning('主控制节点为本机平台控制面，禁止移除')
    return
  }
  try {
    await removeAdminClusterNodeApi(node.id)
    message.success('节点已成功移除')
    await fetchNodes()
    await fetchSettings()
    // 广播全局集群节点更新事件，通知镜像管理等模块同步刷新
    window.dispatchEvent(new CustomEvent('volur:cluster-nodes-updated'))
  } catch {
    message.error('移除节点失败')
  }
}

onMounted(() => {
  fetchSettings()
  fetchNodes()
})
</script>

<style scoped lang="scss">
.admin-cluster-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header {
  display: flex;
  flex-direction: column;
  gap: 4px;

  .page-title {
    font-size: 22px;
    font-weight: 700;
    margin: 0;
    color: var(--v-text-primary, #111111);
    letter-spacing: -0.3px;
  }

  .page-desc {
    font-size: 13px;
    color: #444444;
    margin: 0;
  }
}

/* Statistics Ribbon */
.stat-ribbon {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;

  .stat-mini-card {
    background-color: var(--n-color, #ffffff);
    border-radius: 10px;
    padding: 16px 20px;
    display: flex;
    align-items: center;
    gap: 14px;
    box-shadow: var(--v-shadow-sm, 0 1px 3px rgba(0, 0, 0, 0.04));
    transition: transform 0.2s ease;

    &:hover {
      transform: translateY(-2px);
    }

    .stat-icon-wrapper {
      width: 42px;
      height: 42px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;

      &.primary {
        background-color: rgba(37, 99, 235, 0.1);
        color: #2563eb;
      }

      &.success {
        background-color: rgba(16, 185, 129, 0.1);
        color: #10b981;
      }

      &.warning {
        background-color: rgba(245, 158, 11, 0.1);
        color: #f59e0b;
      }

      &.info {
        background-color: rgba(99, 102, 241, 0.1);
        color: #6366f1;
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
        color: #444444;
      }
    }
  }
}

.content-card {
  border-radius: 10px;
  background-color: var(--n-color, #ffffff);

  .tab-pane-container {
    padding: 16px 8px;
  }
}

.field-hint {
  margin-left: 12px;
  font-size: 12px;
  color: #444444;
}

.range-inputs {
  display: flex;
  align-items: center;
  gap: 8px;

  .range-divider {
    color: #999999;
    font-weight: 500;
  }
}

.switch-row {
  display: flex;
  align-items: center;
}

.test-connection-section {
  display: flex;
  flex-direction: column;
  align-items: flex-start;

  .test-result-details {
    display: flex;
    flex-direction: column;
    gap: 6px;

    .res-msg {
      margin: 0;
      font-size: 13px;
      line-height: 1.4;
    }

    .res-badges {
      display: flex;
      align-items: center;
      gap: 6px;
      flex-wrap: wrap;
      margin-top: 4px;
    }
  }
}

.action-footer {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--v-border-base, #efeff5);
  display: flex;
  justify-content: flex-end;
}

/* Nodes Tab Styles */
.nodes-tab-container {
  padding: 8px 0;

  .nodes-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;

    .toolbar-title {
      display: flex;
      align-items: center;
      font-size: 15px;
      font-weight: 600;
      color: var(--v-text-primary, #111111);
    }

    .toolbar-actions {
      display: flex;
      align-items: center;
      gap: 12px;
    }
  }
}

.mono-text {
  font-family: 'Fira Code', monospace;
  font-size: 12.5px;
  color: #333333;
}

.specs-text {
  font-size: 12.5px;
  color: #444444;
}

.node-name-cell {
  display: flex;
  align-items: center;

  .name-text {
    font-weight: 600;
    color: var(--v-text-primary, #111111);
  }
}

.action-btns {
  display: flex;
  align-items: center;
  gap: 8px;
}

.theme-primary-btn {
  background-color: #000000 !important;
  color: #ffffff !important;
  font-weight: 500;
  border-radius: 6px;
  border: none !important;

  &:hover {
    background-color: #222222 !important;
  }
}
</style>
