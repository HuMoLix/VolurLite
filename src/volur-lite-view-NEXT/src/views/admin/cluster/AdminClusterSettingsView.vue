<template>
  <div class="space-y-6">
    <div>
      <h2 class="text-xl font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
        {{ t('admin.clusters.title', '集群设置') }}
      </h2>
      <p class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary mt-1">
        {{ t('admin.clusters.desc', 'Docker Swarm / K3s 靶机容器调度、分布式节点架构与集群拓扑管控') }}
      </p>
    </div>

    <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
      <div class="p-4 rounded-2xl bg-white dark:bg-[#292a2d] border border-google-border-light dark:border-google-border-dark flex items-center gap-3.5">
        <div class="w-10 h-10 rounded-xl bg-google-blue/10 dark:bg-google-blue-dark/20 text-google-blue dark:text-google-blue-dark flex items-center justify-center shrink-0">
          <ServerOutline class="w-5 h-5" />
        </div>
        <div>
          <div class="text-lg font-bold text-google-text-primary dark:text-google-text-darkPrimary">{{ settings.totalNodes }}</div>
          <div class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary">{{ t('admin.clusters.stats.total', '总计算节点') }}</div>
        </div>
      </div>

      <div class="p-4 rounded-2xl bg-white dark:bg-[#292a2d] border border-google-border-light dark:border-google-border-dark flex items-center gap-3.5">
        <div class="w-10 h-10 rounded-xl bg-purple-500/10 text-purple-600 dark:text-purple-400 flex items-center justify-center shrink-0">
          <ShieldCheckmarkOutline class="w-5 h-5" />
        </div>
        <div>
          <div class="text-lg font-bold text-google-text-primary dark:text-google-text-darkPrimary">{{ settings.masterNodes }}</div>
          <div class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary">{{ t('admin.clusters.stats.master', '控制主节点 (Master)') }}</div>
        </div>
      </div>

      <div class="p-4 rounded-2xl bg-white dark:bg-[#292a2d] border border-google-border-light dark:border-google-border-dark flex items-center gap-3.5">
        <div class="w-10 h-10 rounded-xl bg-cyan-500/10 text-cyan-600 dark:text-cyan-400 flex items-center justify-center shrink-0">
          <HardwareChipOutline class="w-5 h-5" />
        </div>
        <div>
          <div class="text-lg font-bold text-google-text-primary dark:text-google-text-darkPrimary">{{ settings.workerNodes }}</div>
          <div class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary">{{ t('admin.clusters.stats.worker', '工作负载节点 (Worker)') }}</div>
        </div>
      </div>

      <div class="p-4 rounded-2xl bg-white dark:bg-[#292a2d] border border-google-border-light dark:border-google-border-dark flex items-center gap-3.5">
        <div class="w-10 h-10 rounded-xl bg-green-500/10 text-green-600 dark:text-green-400 flex items-center justify-center shrink-0">
          <CheckmarkCircleOutline class="w-5 h-5" />
        </div>
        <div>
          <div class="text-lg font-bold text-google-text-primary dark:text-google-text-darkPrimary">{{ settings.readyNodes }}</div>
          <div class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary">{{ t('admin.clusters.stats.ready', '存活就绪节点 (Ready)') }}</div>
        </div>
      </div>
    </div>

    <div class="p-6 rounded-2xl bg-white dark:bg-[#292a2d] border border-google-border-light dark:border-google-border-dark space-y-6">
      <div class="flex items-center justify-between flex-wrap gap-3 pb-2 border-b border-google-border-light/70 dark:border-google-border-dark/70">
        <VFilterBtnGroup
          v-model:value="activeTab"
          :options="tabOptions"
          size="medium"
          @update:value="handleTabChange"
        />
      </div>

      <ParamsTab
        v-if="activeTab === 'settings'"
        :settings="settings"
        @saved="handleSettingsSaved"
      />

      <NodesTab
        v-else-if="activeTab === 'nodes'"
        :nodes="nodes"
        :cluster-type="settings.containerType"
        :loading="loadingNodes"
        @refresh="fetchNodes"
        @add-node="openAddNodeModal"
        @edit-node="openEditNodeModal"
        @node-updated="handleNodeUpdated"
      />

      <TopologyTab
        v-else-if="activeTab === 'topology'"
        :nodes="nodes"
        :settings="settings"
        :loading="loadingNodes"
        @refresh="fetchNodes"
        @edit-node="openEditNodeModal"
      />
    </div>

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
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useMessage } from 'naive-ui'
import {
  ServerOutline,
  ShieldCheckmarkOutline,
  HardwareChipOutline,
  CheckmarkCircleOutline,
  GitNetworkOutline,
  OptionsOutline
} from '@vicons/ionicons5'
import VFilterBtnGroup from '@/components/common/VFilterBtnGroup.vue'
import type { FilterOption } from '@/components/common/types'
import type {
  AdminClusterSettingsVO,
  AdminClusterNodeVO
} from '@/types/cluster'
import {
  getAdminClusterSettingsApi,
  getAdminClusterNodesApi
} from '@/api/cluster'
import ParamsTab from './tabs/ParamsTab.vue'
import NodesTab from './tabs/NodesTab.vue'
import TopologyTab from './tabs/TopologyTab.vue'
import ClusterNodeModal from './components/ClusterNodeModal.vue'

defineOptions({
  name: 'AdminClusterSettingsView'
})

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const message = useMessage()

const activeTab = ref<'settings' | 'nodes' | 'topology'>('settings')

const settings = reactive<AdminClusterSettingsVO>({
  containerType: 'DOCKER_SWARM',
  portRangeStart: 20000,
  portRangeEnd: 40000,
  publicHost: '127.0.0.1',
  dockerSwarmApiUrl: '',
  dockerSwarmNetwork: 'volur_network',
  dockerSwarmCertPath: '',
  dockerSwarmOutboundEnabled: false,
  k3sMasterUrl: '',
  k3sNamespace: 'volur-challenges',
  k3sKubeconfigPath: '',
  k3sToken: '',
  resourceIsolationEnabled: true,
  totalNodes: 0,
  masterNodes: 0,
  workerNodes: 0,
  readyNodes: 0
})

const nodes = ref<AdminClusterNodeVO[]>([])
const loadingNodes = ref(false)

const nodeModalVisible = ref(false)
const nodeModalMode = ref<'add' | 'edit'>('add')
const selectedNode = ref<AdminClusterNodeVO | null>(null)

const tabOptions = computed<FilterOption<'settings' | 'nodes' | 'topology'>[]>(() => [
  {
    value: 'settings',
    label: t('admin.clusters.tabs.settings', '集群设置'),
    icon: OptionsOutline
  },
  {
    value: 'nodes',
    label: t('admin.clusters.tabs.nodes', '节点管理'),
    icon: GitNetworkOutline,
    count: nodes.value.length
  },
  {
    value: 'topology',
    label: t('admin.clusters.tabs.topology', '集群拓扑'),
    icon: HardwareChipOutline
  }
])

function syncTabFromRoute() {
  if (route.path.endsWith('/nodes')) {
    activeTab.value = 'nodes'
  } else if (route.path.endsWith('/topology')) {
    activeTab.value = 'topology'
  } else {
    activeTab.value = 'settings'
  }
}

watch(
  () => route.path,
  () => {
    syncTabFromRoute()
  },
  { immediate: true }
)

function handleTabChange(tab: 'settings' | 'nodes' | 'topology') {
  activeTab.value = tab
  const targetPath = `/admin/clusters/${tab}`
  if (route.path !== targetPath) {
    router.push(targetPath)
  }
}

async function fetchSettings() {
  try {
    const res = await getAdminClusterSettingsApi()
    if (res.data) {
      Object.assign(settings, res.data)
    }
  } catch {}
}

async function fetchNodes() {
  loadingNodes.value = true
  try {
    const res = await getAdminClusterNodesApi()
    if (res.data) {
      nodes.value = res.data
    }
  } catch {
    nodes.value = []
  } finally {
    loadingNodes.value = false
  }
}

async function handleSettingsSaved() {
  await Promise.all([fetchSettings(), fetchNodes()])
}

async function handleNodeUpdated() {
  await Promise.all([fetchNodes(), fetchSettings()])
}

function openAddNodeModal() {
  nodeModalMode.value = 'add'
  selectedNode.value = null
  nodeModalVisible.value = true
}

function openEditNodeModal(node: AdminClusterNodeVO) {
  if (node.role === 'MASTER') {
    message.warning('控制主节点为平台控制面，禁止修改')
    return
  }
  nodeModalMode.value = 'edit'
  selectedNode.value = node
  nodeModalVisible.value = true
}

async function handleNodeModalSuccess() {
  await Promise.all([fetchNodes(), fetchSettings()])
}

onMounted(() => {
  fetchSettings()
  fetchNodes()
})
</script>
