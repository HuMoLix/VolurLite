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
    <n-card class="content-card" :bordered="true">
      <n-tabs type="line" animated v-model:value="activeTab">
        <!-- Tab 1: 集群参数与调度设置 (ParamsTab) -->
        <n-tab-pane name="params" :tab="t('admin.clusters.tabs.params')">
          <ParamsTab
            :settings="settings"
            @saved="handleSettingsSaved"
          />
        </n-tab-pane>

        <!-- Tab 2: 节点管理与集群扩展 (NodesTab) -->
        <n-tab-pane name="nodes" :tab="t('admin.clusters.tabs.nodes')">
          <NodesTab
            :nodes="nodes"
            :cluster-type="settings.containerType"
            :loading="loadingNodes"
            @refresh="fetchNodes"
            @add-node="openAddNodeModal"
            @edit-node="openEditNodeModal"
            @node-updated="handleNodeUpdated"
          />
        </n-tab-pane>

        <!-- Tab 3: 集群拓扑架构图 (TopologyTab) -->
        <n-tab-pane name="topology" :tab="t('admin.clusters.tabs.topology')">
          <TopologyTab
            :nodes="nodes"
            :settings="settings"
            :loading="loadingNodes"
            @refresh="fetchNodes"
            @edit-node="openEditNodeModal"
          />
        </n-tab-pane>
      </n-tabs>
    </n-card>

    <!-- Cluster Node Modal (Add / Edit) -->
    <ClusterNodeModal
      v-model:show="nodeModalVisible"
      :mode="nodeModalMode"
      :node="selectedNode"
      :cluster-type="settings.containerType"
      @success="handleNodeModalSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  NCard,
  NTabs,
  NTabPane,
  NIcon,
  useMessage
} from 'naive-ui'
import {
  ServerOutline,
  ShieldCheckmarkOutline,
  HardwareChipOutline,
  CheckmarkCircleOutline
} from '@vicons/ionicons5'
import {
  getAdminClusterSettingsApi,
  getAdminClusterNodesApi
} from '@/api/cluster'
import type {
  AdminClusterSettingsVO,
  AdminClusterNodeVO
} from '@/types/cluster'
import ParamsTab from './tabs/ParamsTab.vue'
import NodesTab from './tabs/NodesTab.vue'
import TopologyTab from './tabs/TopologyTab.vue'
import ClusterNodeModal from './components/ClusterNodeModal.vue'

const { t } = useI18n()
const message = useMessage()

const activeTab = ref<'params' | 'nodes' | 'topology'>('params')

// Cluster Settings State
const settings = reactive<AdminClusterSettingsVO>({
  containerType: 'K3S',
  portRangeStart: 20000,
  portRangeEnd: 30000,
  publicHost: '127.0.0.1',
  dockerSwarmApiUrl: 'unix:///var/run/docker.sock',
  dockerSwarmNetwork: 'volur-swarm-network',
  dockerSwarmCertPath: '',
  dockerSwarmOutboundEnabled: false,
  k3sMasterUrl: 'https://127.0.0.1:6443',
  k3sNamespace: 'volur-challenges',
  k3sKubeconfigPath: '/etc/rancher/k3s/k3s.yaml',
  resourceIsolationEnabled: true,
  totalNodes: 1,
  masterNodes: 1,
  workerNodes: 0,
  readyNodes: 1
})

// Nodes State
const nodes = ref<AdminClusterNodeVO[]>([])
const loadingNodes = ref(false)

// Unified Node Modal State
const nodeModalVisible = ref(false)
const nodeModalMode = ref<'add' | 'edit'>('add')
const selectedNode = ref<AdminClusterNodeVO | null>(null)

async function fetchSettings() {
  try {
    const res = await getAdminClusterSettingsApi()
    if (res.data) {
      Object.assign(settings, res.data)
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

async function handleSettingsSaved() {
  await fetchSettings()
  await fetchNodes()
}

async function handleNodeUpdated() {
  await fetchNodes()
  await fetchSettings()
  window.dispatchEvent(new CustomEvent('volur:cluster-nodes-updated'))
}

function openAddNodeModal() {
  nodeModalMode.value = 'add'
  selectedNode.value = null
  nodeModalVisible.value = true
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
  window.dispatchEvent(new CustomEvent('volur:cluster-nodes-updated'))
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
    color: var(--v-text-desc, #444444);
    margin: 0;
  }
}

/* Statistics Ribbon */
.stat-ribbon {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;

  .stat-mini-card {
    background-color: var(--v-bg-card, #ffffff);
    border-radius: 10px;
    padding: 16px 20px;
    display: flex;
    align-items: center;
    gap: 14px;
    border: 1px solid var(--v-border-base, #e5e4e7);
    box-shadow: var(--v-shadow-sm, 0 1px 3px rgba(0, 0, 0, 0.04));
    transition: background-color 0.3s ease, border-color 0.3s ease, transform 0.2s ease, box-shadow 0.2s ease;

    &:hover {
      transform: translateY(-2px);
      border-color: var(--v-border-focus, #bbbbbb);
      box-shadow: var(--v-shadow-md, 0 4px 8px rgba(0, 0, 0, 0.06));
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
        color: var(--v-text-desc, #444444);
      }
    }
  }
}

.content-card {
  border-radius: 10px;
  background-color: var(--v-bg-card, #ffffff);
  border: 1px solid var(--v-border-base, #e5e4e7);
}
</style>
