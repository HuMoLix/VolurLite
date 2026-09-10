<template>
  <div
    ref="containerRef"
    class="cluster-topology-container"
    :class="{ 'is-fullscreen': isFullscreen }"
    @wheel.prevent="handleWheel"
    @mousedown="handleCanvasMouseDown"
  >
    <!-- Top Floating Toolbar & Legend -->
    <div class="topology-toolbar">
      <div class="toolbar-left">
        <div class="topology-title-block">
          <n-icon size="20" class="title-icon">
            <GitNetworkOutline />
          </n-icon>
          <span class="title-text">{{ t('admin.clusters.topology.title') }}</span>
        </div>
        <div class="topology-pills">
          <span class="pill-badge primary">
            <span class="dot"></span>
            {{ t('admin.clusters.stats.total') }}: {{ props.nodes.length }}
          </span>
          <span class="pill-badge ready">
            <span class="dot success"></span>
            {{ t('admin.clusters.topology.in_cluster_ratio') }}: {{ inClusterCount }} / {{ props.nodes.length }}
          </span>
          <span class="pill-badge neutral">
            <n-icon size="14"><ServerOutline /></n-icon>
            {{ props.settings?.dockerSwarmNetwork || 'volur-swarm-network' }}
          </span>
        </div>
      </div>

      <!-- Controls Right: Zoom & Fullscreen -->
      <div class="toolbar-right">
        <n-button-group size="small">
          <n-button secondary @click="zoomIn" :title="t('admin.clusters.topology.zoom_in')">
            <template #icon><n-icon :component="AddOutline" /></template>
          </n-button>
          <n-button secondary @click="resetZoom" :title="t('admin.clusters.topology.zoom_reset')">
            {{ Math.round(scale * 100) }}%
          </n-button>
          <n-button secondary @click="zoomOut" :title="t('admin.clusters.topology.zoom_out')">
            <template #icon><n-icon :component="RemoveOutline" /></template>
          </n-button>
          <n-button secondary @click="toggleFullscreen" :title="t('admin.clusters.topology.fullscreen')">
            <template #icon>
              <n-icon :component="isFullscreen ? ContractOutline : ExpandOutline" />
            </template>
          </n-button>
          <n-button secondary @click="handleRefresh" :loading="props.loading" title="刷新拓扑">
            <template #icon><n-icon :component="RefreshOutline" /></template>
          </n-button>
        </n-button-group>
      </div>
    </div>

    <!-- Interactive Canvas Viewport -->
    <div
      class="topology-viewport"
      :style="{
        transform: `translate(${panX}px, ${panY}px) scale(${scale})`,
        transformOrigin: '0 0'
      }"
    >
      <!-- SVG Connecting Curves Layer -->
      <svg class="topology-svg-layer">
        <defs>
          <linearGradient id="readyLinkGrad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#10b981" stop-opacity="0.8" />
            <stop offset="100%" stop-color="#059669" stop-opacity="0.9" />
          </linearGradient>
          <linearGradient id="errorLinkGrad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#f59e0b" stop-opacity="0.8" />
            <stop offset="100%" stop-color="#ef4444" stop-opacity="0.9" />
          </linearGradient>
        </defs>

        <!-- Dynamic Connection Wires -->
        <g v-for="link in connectionLinks" :key="link.id">
          <path
            :d="link.path"
            :class="['link-wire', link.inCluster ? 'ready' : 'unjoined']"
          />
          <!-- Midpoint Status Indicator -->
          <g :transform="`translate(${link.midX}, ${link.midY})`">
            <circle
              r="11"
              :fill="link.inCluster ? '#10b981' : '#ef4444'"
              stroke="#ffffff"
              stroke-width="2"
            />
            <text
              y="3.5"
              text-anchor="middle"
              font-size="10"
              font-weight="bold"
              fill="#ffffff"
            >
              {{ link.inCluster ? '✓' : '!' }}
            </text>
          </g>
        </g>
      </svg>

      <!-- Draggable Node Cards Layer -->
      <div
        v-for="nodePos in positionedNodes"
        :key="nodePos.node.id"
        class="topology-node-wrapper"
        :class="{
          'is-master': nodePos.node.role === 'MASTER',
          'is-worker': nodePos.node.role !== 'MASTER',
          'in-cluster': nodePos.node.inCluster,
          'not-in-cluster': !nodePos.node.inCluster && nodePos.node.role !== 'MASTER',
          'is-selected': selectedNode?.id === nodePos.node.id
        }"
        :style="{
          left: `${nodePos.x}px`,
          top: `${nodePos.y}px`
        }"
        @mousedown.stop="handleNodeMouseDown($event, nodePos)"
        @click.stop="selectNode(nodePos.node)"
      >
        <div class="node-card">
          <!-- Card Header Ribbon -->
          <div class="node-header">
            <div class="node-title-group">
              <div class="node-icon-wrapper" :class="nodePos.node.role === 'MASTER' ? 'master' : (nodePos.node.inCluster ? 'worker-ready' : 'worker-error')">
                <n-icon size="18">
                  <ShieldCheckmarkOutline v-if="nodePos.node.role === 'MASTER'" />
                  <CheckmarkCircleOutline v-else-if="nodePos.node.inCluster" />
                  <AlertCircleOutline v-else />
                </n-icon>
              </div>
              <div class="node-meta-title">
                <div class="node-name-text">{{ nodePos.node.nodeName }}</div>
                <div class="node-ip-text">{{ nodePos.node.ipAddress }}</div>
              </div>
            </div>

            <!-- Role / Resource Tag -->
            <div class="node-tags">
              <span v-if="nodePos.node.role === 'MASTER'" class="node-badge master-badge">
                MASTER
              </span>
              <span v-else class="node-badge" :class="nodePos.node.inCluster ? 'in-swarm-badge' : 'not-in-swarm-badge'">
                {{ nodePos.node.inCluster ? 'IN SWARM' : 'DISCONNECTED' }}
              </span>
            </div>
          </div>

          <!-- Card Body Stats -->
          <div class="node-body">
            <div class="stat-cell">
              <span class="cell-label">容器数</span>
              <span class="cell-value">{{ nodePos.node.containerCount || 0 }}</span>
            </div>
            <div class="stat-cell">
              <span class="cell-label">镜像缓存</span>
              <span class="cell-value">{{ nodePos.node.imageCount || 0 }}</span>
            </div>
            <div class="stat-cell">
              <span class="cell-label">规格配置</span>
              <span class="cell-value">{{ nodePos.node.cpuCores ? `${nodePos.node.cpuCores}C` : '-' }} / {{ nodePos.node.memoryFormatted || '-' }}</span>
            </div>
          </div>

          <!-- Bottom Status Strip -->
          <div class="node-footer">
            <div class="cluster-status-indicator">
              <span class="status-pulse" :class="nodePos.node.inCluster ? 'pulse-ready' : 'pulse-error'"></span>
              <span class="status-label">
                {{ nodePos.node.role === 'MASTER' ? '编排控制面' : (nodePos.node.inCluster ? 'Swarm 就绪' : '未入编排集群') }}
              </span>
            </div>
            <div class="node-action-trigger">
              <n-icon size="14"><ChevronForwardOutline /></n-icon>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Canvas Navigation Hint -->
    <div class="canvas-drag-hint">
      <n-icon size="14" style="margin-right: 4px;"><GitNetworkOutline /></n-icon>
      <span>{{ t('admin.clusters.topology.tip_drag') }}</span>
    </div>

    <!-- Node Detail Drawer / Card (Slide-over on selection) -->
    <transition name="slide-panel">
      <div v-if="selectedNode" class="node-detail-panel">
        <div class="panel-header">
          <div class="panel-title-group">
            <n-icon size="18"><ServerOutline /></n-icon>
            <span class="panel-title">{{ selectedNode.nodeName }}</span>
          </div>
          <n-button quaternary circle size="small" @click="selectedNode = null">
            <template #icon><n-icon :component="CloseOutline" /></template>
          </n-button>
        </div>

        <div class="panel-content">
          <div class="detail-section">
            <div class="section-title">节点基础信息</div>
            <div class="detail-row">
              <span class="key">角色类型:</span>
              <span class="val">
                <n-tag size="tiny" :type="selectedNode.role === 'MASTER' ? 'warning' : 'info'" round>
                  {{ selectedNode.role === 'MASTER' ? '主控制节点 (Control Plane)' : '工作节点 (Worker)' }}
                </n-tag>
              </span>
            </div>
            <div class="detail-row">
              <span class="key">IP 地址 / 端口:</span>
              <span class="val mono">{{ selectedNode.ipAddress }}:{{ selectedNode.sshPort || 22 }}</span>
            </div>
            <div class="detail-row">
              <span class="key">题目资源调度:</span>
              <span class="val">
                <n-tag size="tiny" :type="selectedNode.isResourceNode ? 'success' : 'default'" round>
                  {{ selectedNode.isResourceNode ? '参与赛题调度' : '纯控制面 (不调度)' }}
                </n-tag>
              </span>
            </div>
            <div class="detail-row">
              <span class="key">集群入网状态:</span>
              <span class="val">
                <n-tag size="tiny" :type="selectedNode.inCluster ? 'success' : 'error'" round>
                  {{ selectedNode.inCluster ? '已成功入网 (Ready)' : '未加入集群 (Inactive)' }}
                </n-tag>
              </span>
            </div>
            <div v-if="selectedNode.clusterNodeId" class="detail-row">
              <span class="key">Swarm Node ID:</span>
              <span class="val mono">{{ selectedNode.clusterNodeId }}</span>
            </div>
          </div>

          <div class="detail-section">
            <div class="section-title">运行时与硬件资源</div>
            <div class="detail-row">
              <span class="key">Docker 版本:</span>
              <span class="val">{{ selectedNode.dockerVersion || 'Docker Engine' }}</span>
            </div>
            <div class="detail-row">
              <span class="key">系统环境:</span>
              <span class="val">{{ selectedNode.osInfo || 'Linux' }}</span>
            </div>
            <div class="detail-row">
              <span class="key">CPU / 内存:</span>
              <span class="val">{{ selectedNode.cpuCores || 1 }} 核 / {{ selectedNode.memoryFormatted || '1 GB' }}</span>
            </div>
            <div class="detail-row">
              <span class="key">运行容器数:</span>
              <span class="val mono">{{ selectedNode.containerCount || 0 }}</span>
            </div>
            <div class="detail-row">
              <span class="key">缓存镜像数:</span>
              <span class="val mono">{{ selectedNode.imageCount || 0 }}</span>
            </div>
            <div class="detail-row">
              <span class="key">TLS 安全通信:</span>
              <span class="val">
                <n-tag size="tiny" :type="selectedNode.tlsEnabled ? 'success' : 'default'" round>
                  {{ selectedNode.tlsEnabled ? '2376 TLS 双向加密' : '未开启' }}
                </n-tag>
              </span>
            </div>
          </div>

          <!-- Quick Action Buttons inside Topology -->
          <div class="panel-actions">
            <!-- Join / Re-join button if worker is not in cluster -->
            <n-button
              v-if="selectedNode.role !== 'MASTER' && !selectedNode.inCluster"
              type="error"
              block
              style="margin-bottom: 8px;"
              :loading="joiningNodeId === selectedNode.id"
              @click="handleRejoinCluster(selectedNode)"
            >
              <template #icon><n-icon :component="SyncOutline" /></template>
              {{ t('admin.clusters.topology.action_rejoin') }}
            </n-button>

            <div class="action-grid">
              <n-button
                secondary
                size="small"
                :loading="checkingHealthNodeId === selectedNode.id"
                @click="handleNodeHealthCheck(selectedNode)"
              >
                <template #icon><n-icon :component="PulseOutline" /></template>
                {{ t('admin.clusters.topology.action_check') }}
              </n-button>

              <n-button
                secondary
                size="small"
                type="info"
                :loading="syncingNodeId === selectedNode.id"
                @click="handleNodeSyncImages(selectedNode)"
              >
                <template #icon><n-icon :component="RefreshOutline" /></template>
                {{ t('admin.clusters.topology.action_sync') }}
              </n-button>
            </div>

            <n-button
              v-if="selectedNode.role !== 'MASTER'"
              quaternary
              size="small"
              block
              style="margin-top: 8px;"
              @click="handleEditNode(selectedNode)"
            >
              <template #icon><n-icon :component="CreateOutline" /></template>
              {{ t('admin.clusters.topology.action_edit') }}
            </n-button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  NButton,
  NButtonGroup,
  NIcon,
  NTag,
  useMessage
} from 'naive-ui'
import {
  GitNetworkOutline,
  ServerOutline,
  ShieldCheckmarkOutline,
  CheckmarkCircleOutline,
  AlertCircleOutline,
  AddOutline,
  RemoveOutline,
  ExpandOutline,
  ContractOutline,
  RefreshOutline,
  CloseOutline,
  PulseOutline,
  SyncOutline,
  CreateOutline,
  ChevronForwardOutline
} from '@vicons/ionicons5'
import type { AdminClusterNodeVO, AdminClusterSettingsVO } from '@/types/cluster'
import {
  checkAdminClusterNodeHealthApi,
  syncImagesToClusterNodeApi,
  joinAdminClusterNodeApi
} from '@/api/cluster'

interface Props {
  nodes: AdminClusterNodeVO[]
  settings?: AdminClusterSettingsVO
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  nodes: () => [],
  loading: false
})

const emit = defineEmits<{
  (e: 'refresh'): void
  (e: 'editNode', node: AdminClusterNodeVO): void
}>()

const { t } = useI18n()
const message = useMessage()

const containerRef = ref<HTMLElement | null>(null)
const isFullscreen = ref(false)

// Zoom & Pan Transform State
const scale = ref(1.0)
const panX = ref(120)
const panY = ref(60)

// Dragging Canvas State
const isPanning = ref(false)
const startPanMouseX = ref(0)
const startPanMouseY = ref(0)
const initialPanX = ref(0)
const initialPanY = ref(0)

// Node Dragging State
interface PositionedNode {
  node: AdminClusterNodeVO
  x: number
  y: number
}

const positionedNodes = ref<PositionedNode[]>([])
const draggingNode = ref<PositionedNode | null>(null)
const dragOffset = ref({ x: 0, y: 0 })

// Selected Node for Detail Side Panel
const selectedNode = ref<AdminClusterNodeVO | null>(null)

// Action Loading States
const checkingHealthNodeId = ref<string | number | null>(null)
const syncingNodeId = ref<string | number | null>(null)
const joiningNodeId = ref<string | number | null>(null)

// Computed in-cluster ratio
const inClusterCount = computed(() => {
  return props.nodes.filter(n => n.inCluster || n.role === 'MASTER').length
})

// Auto layout nodes when list changes
function initNodePositions() {
  const master = props.nodes.find(n => n.role === 'MASTER') || props.nodes[0]
  const workers = props.nodes.filter(n => n.id !== master?.id)

  const result: PositionedNode[] = []

  // Place Master at top-center
  const masterX = 360
  const masterY = 80
  if (master) {
    result.push({
      node: master,
      x: masterX,
      y: masterY
    })
  }

  // Arrange Workers in an orderly arc or grid below Master
  const workerCount = workers.length
  if (workerCount > 0) {
    const startX = Math.max(80, masterX - (workerCount - 1) * 160)
    workers.forEach((worker, index) => {
      const wx = startX + index * 320
      const wy = masterY + 240
      result.push({
        node: worker,
        x: wx,
        y: wy
      })
    })
  }

  positionedNodes.value = result
}

watch(
  () => props.nodes,
  () => {
    if (positionedNodes.value.length === 0 || positionedNodes.value.length !== props.nodes.length) {
      initNodePositions()
    } else {
      positionedNodes.value = positionedNodes.value.map(pn => {
        const found = props.nodes.find(n => n.id === pn.node.id)
        return {
          node: found || pn.node,
          x: pn.x,
          y: pn.y
        }
      })
      if (selectedNode.value) {
        selectedNode.value = props.nodes.find(n => n.id === selectedNode.value?.id) || null
      }
    }
  },
  { immediate: true, deep: true }
)

// Computed Dynamic Curved Connection Wires
interface ConnectionLink {
  id: string
  path: string
  midX: number
  midY: number
  inCluster: boolean
}

const connectionLinks = computed<ConnectionLink[]>(() => {
  const master = positionedNodes.value.find(p => p.node.role === 'MASTER')
  if (!master) return []

  const CARD_WIDTH = 260
  const CARD_HEIGHT = 130

  const masterCenterX = master.x + CARD_WIDTH / 2
  const masterBottomY = master.y + CARD_HEIGHT

  const links: ConnectionLink[] = []

  positionedNodes.value.forEach(pn => {
    if (pn.node.id === master.node.id) return

    const workerCenterX = pn.x + CARD_WIDTH / 2
    const workerTopY = pn.y

    const deltaY = Math.abs(workerTopY - masterBottomY)
    const controlPointOffset = Math.max(50, deltaY * 0.5)

    const startX = masterCenterX
    const startY = masterBottomY
    const endX = workerCenterX
    const endY = workerTopY

    const cp1X = startX
    const cp1Y = startY + controlPointOffset
    const cp2X = endX
    const cp2Y = endY - controlPointOffset

    const path = `M ${startX} ${startY} C ${cp1X} ${cp1Y}, ${cp2X} ${cp2Y}, ${endX} ${endY}`

    const t = 0.5
    const midX = Math.round((1 - t) ** 3 * startX + 3 * (1 - t) ** 2 * t * cp1X + 3 * (1 - t) * t ** 2 * cp2X + t ** 3 * endX)
    const midY = Math.round((1 - t) ** 3 * startY + 3 * (1 - t) ** 2 * t * cp1Y + 3 * (1 - t) * t ** 2 * cp2Y + t ** 3 * endY)

    links.push({
      id: `link-${master.node.id}-${pn.node.id}`,
      path,
      midX,
      midY,
      inCluster: Boolean(pn.node.inCluster)
    })
  })

  return links
})

// Mouse Events for Canvas Pan & Wheel Zoom
function handleWheel(e: WheelEvent) {
  const delta = e.deltaY > 0 ? -0.08 : 0.08
  const newScale = Math.min(2.2, Math.max(0.4, scale.value + delta))
  scale.value = Number(newScale.toFixed(2))
}

function handleCanvasMouseDown(e: MouseEvent) {
  isPanning.value = true
  startPanMouseX.value = e.clientX
  startPanMouseY.value = e.clientY
  initialPanX.value = panX.value
  initialPanY.value = panY.value

  window.addEventListener('mousemove', handleCanvasMouseMove)
  window.addEventListener('mouseup', handleCanvasMouseUp)
}

function handleCanvasMouseMove(e: MouseEvent) {
  if (isPanning.value) {
    const dx = e.clientX - startPanMouseX.value
    const dy = e.clientY - startPanMouseY.value
    panX.value = initialPanX.value + dx
    panY.value = initialPanY.value + dy
  }
}

function handleCanvasMouseUp() {
  isPanning.value = false
  window.removeEventListener('mousemove', handleCanvasMouseMove)
  window.removeEventListener('mouseup', handleCanvasMouseUp)
}

// Node Dragging Handling
function handleNodeMouseDown(e: MouseEvent, nodePos: PositionedNode) {
  draggingNode.value = nodePos
  dragOffset.value = {
    x: e.clientX / scale.value - nodePos.x,
    y: e.clientY / scale.value - nodePos.y
  }

  window.addEventListener('mousemove', handleNodeMouseMove)
  window.addEventListener('mouseup', handleNodeMouseUp)
}

function handleNodeMouseMove(e: MouseEvent) {
  if (draggingNode.value) {
    draggingNode.value.x = Math.round(e.clientX / scale.value - dragOffset.value.x)
    draggingNode.value.y = Math.round(e.clientY / scale.value - dragOffset.value.y)
  }
}

function handleNodeMouseUp() {
  draggingNode.value = null
  window.removeEventListener('mousemove', handleNodeMouseMove)
  window.removeEventListener('mouseup', handleNodeMouseUp)
}

// Zoom controls
function zoomIn() {
  scale.value = Math.min(2.2, Number((scale.value + 0.15).toFixed(2)))
}

function zoomOut() {
  scale.value = Math.max(0.4, Number((scale.value - 0.15).toFixed(2)))
}

function resetZoom() {
  scale.value = 1.0
  panX.value = 120
  panY.value = 60
}

function toggleFullscreen() {
  if (!containerRef.value) return
  if (!document.fullscreenElement) {
    containerRef.value.requestFullscreen?.().then(() => {
      isFullscreen.value = true
    })
  } else {
    document.exitFullscreen?.().then(() => {
      isFullscreen.value = false
    })
  }
}

function selectNode(node: AdminClusterNodeVO) {
  selectedNode.value = node
}

function handleRefresh() {
  emit('refresh')
}

// Quick Actions
async function handleRejoinCluster(node: AdminClusterNodeVO) {
  joiningNodeId.value = node.id
  try {
    const res = await joinAdminClusterNodeApi(node.id)
    if (res.data) {
      message.success(t('admin.clusters.topology.action_rejoin_success'))
      emit('refresh')
    }
  } catch (err: unknown) {
    const errorMsg = err instanceof Error ? err.message : '重新加入集群失败'
    message.error(errorMsg)
  } finally {
    joiningNodeId.value = null
  }
}

async function handleNodeHealthCheck(node: AdminClusterNodeVO) {
  checkingHealthNodeId.value = node.id
  try {
    const res = await checkAdminClusterNodeHealthApi(node.id)
    if (res.data) {
      message.success(`节点健康检测完成: [${res.data.status}]`)
      emit('refresh')
    }
  } catch (err) {
    message.error('检测节点存活失败')
  } finally {
    checkingHealthNodeId.value = null
  }
}

async function handleNodeSyncImages(node: AdminClusterNodeVO) {
  syncingNodeId.value = node.id
  try {
    await syncImagesToClusterNodeApi(node.id)
    message.success(t('admin.clusters.nodes.sync_success'))
  } catch (err) {
    message.error('下发同步镜像指令失败')
  } finally {
    syncingNodeId.value = null
  }
}

function handleEditNode(node: AdminClusterNodeVO) {
  emit('editNode', node)
}

onMounted(() => {
  initNodePositions()
  document.addEventListener('fullscreenchange', () => {
    isFullscreen.value = !!document.fullscreenElement
  })
})

onUnmounted(() => {
  window.removeEventListener('mousemove', handleCanvasMouseMove)
  window.removeEventListener('mouseup', handleCanvasMouseUp)
  window.removeEventListener('mousemove', handleNodeMouseMove)
  window.removeEventListener('mouseup', handleNodeMouseUp)
})
</script>

<style lang="scss" scoped>
.cluster-topology-container {
  position: relative;
  width: 100%;
  height: 620px;
  background-color: var(--n-color);
  background-image:
    radial-gradient(var(--n-border-color) 1px, transparent 1px);
  background-size: 24px 24px;
  border-radius: 10px;
  border: 1px solid var(--n-border-color);
  overflow: hidden;
  user-select: none;
  cursor: grab;

  &:active {
    cursor: grabbing;
  }

  &.is-fullscreen {
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    z-index: 9999;
    border-radius: 0;
    border: none;
  }
}

/* Floating Toolbar */
.topology-toolbar {
  position: absolute;
  top: 16px;
  left: 16px;
  right: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  z-index: 20;
  pointer-events: none;

  .toolbar-left,
  .toolbar-right {
    pointer-events: auto;
    display: flex;
    align-items: center;
    gap: 10px;
    background: rgba(var(--n-color-rgb, 255, 255, 255), 0.85);
    backdrop-filter: blur(10px);
    padding: 6px 12px;
    border-radius: 8px;
    border: 1px solid var(--n-border-color);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  }

  .topology-title-block {
    display: flex;
    align-items: center;
    gap: 6px;
    font-weight: 600;
    font-size: 13px;
    color: var(--n-text-color);

    .title-icon {
      color: #10b981;
    }
  }

  .topology-pills {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-left: 8px;

    .pill-badge {
      display: inline-flex;
      align-items: center;
      gap: 5px;
      padding: 3px 8px;
      font-size: 11px;
      font-weight: 500;
      border-radius: 12px;
      background: rgba(0, 0, 0, 0.04);
      color: #444444;

      .dot {
        width: 6px;
        height: 6px;
        border-radius: 50%;
        background-color: #6366f1;

        &.success {
          background-color: #10b981;
        }
      }
    }
  }
}

/* Canvas Viewport */
.topology-viewport {
  position: absolute;
  top: 0;
  left: 0;
  width: 4000px;
  height: 3000px;
  pointer-events: none;
}

/* SVG Layer */
.topology-svg-layer {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;

  .link-wire {
    fill: none;
    stroke-width: 2.5;

    &.ready {
      stroke: #10b981;
      stroke-dasharray: 6 4;
      animation: dashFlow 1.2s linear infinite;
    }

    &.unjoined {
      stroke: #ef4444;
      stroke-dasharray: 4 4;
      stroke-opacity: 0.75;
    }
  }
}

@keyframes dashFlow {
  from {
    stroke-dashoffset: 20;
  }
  to {
    stroke-dashoffset: 0;
  }
}

/* Node Wrapper & Card */
.topology-node-wrapper {
  position: absolute;
  width: 260px;
  pointer-events: auto;
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.15s;

  &:hover {
    transform: translateY(-2px);
  }

  &.is-selected .node-card {
    border-color: #000000;
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  }

  &.is-master .node-card {
    border-top: 3px solid #f59e0b;
  }

  &.in-cluster.is-worker .node-card {
    border-top: 3px solid #10b981;
  }

  &.not-in-cluster.is-worker .node-card {
    border-top: 3px solid #ef4444;
  }
}

.node-card {
  background: var(--n-card-color, #ffffff);
  border: 1px solid var(--n-border-color);
  border-radius: 10px;
  padding: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
  transition: all 0.2s ease;

  .node-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    padding-bottom: 8px;
    border-bottom: 1px solid var(--n-border-color);

    .node-title-group {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .node-icon-wrapper {
      width: 28px;
      height: 28px;
      border-radius: 6px;
      display: flex;
      align-items: center;
      justify-content: center;

      &.master {
        background: rgba(245, 158, 11, 0.12);
        color: #f59e0b;
      }

      &.worker-ready {
        background: rgba(16, 185, 129, 0.12);
        color: #10b981;
      }

      &.worker-error {
        background: rgba(239, 68, 68, 0.12);
        color: #ef4444;
      }
    }

    .node-meta-title {
      .node-name-text {
        font-size: 13px;
        font-weight: 600;
        color: var(--n-text-color);
        max-width: 120px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .node-ip-text {
        font-size: 11px;
        color: #666666;
        font-family: monospace;
      }
    }

    .node-badge {
      font-size: 9px;
      font-weight: 700;
      padding: 2px 6px;
      border-radius: 4px;
      text-transform: uppercase;

      &.master-badge {
        background: rgba(245, 158, 11, 0.15);
        color: #d97706;
      }

      &.in-swarm-badge {
        background: rgba(16, 185, 129, 0.15);
        color: #059669;
      }

      &.not-in-swarm-badge {
        background: rgba(239, 68, 68, 0.15);
        color: #dc2626;
      }
    }
  }

  .node-body {
    display: grid;
    grid-template-columns: 1fr 1fr 1.2fr;
    gap: 6px;
    padding: 10px 0;

    .stat-cell {
      display: flex;
      flex-direction: column;

      .cell-label {
        font-size: 10px;
        color: #777777;
      }

      .cell-value {
        font-size: 12px;
        font-weight: 600;
        color: var(--n-text-color);
        font-family: monospace;
      }
    }
  }

  .node-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-top: 6px;
    border-top: 1px dashed var(--n-border-color);

    .cluster-status-indicator {
      display: flex;
      align-items: center;
      gap: 5px;

      .status-pulse {
        width: 6px;
        height: 6px;
        border-radius: 50%;

        &.pulse-ready {
          background-color: #10b981;
          box-shadow: 0 0 6px #10b981;
        }

        &.pulse-error {
          background-color: #ef4444;
          box-shadow: 0 0 6px #ef4444;
        }
      }

      .status-label {
        font-size: 10px;
        font-weight: 500;
        color: #555555;
      }
    }

    .node-action-trigger {
      color: #999999;
      display: flex;
      align-items: center;
    }
  }
}

/* Canvas Hint */
.canvas-drag-hint {
  position: absolute;
  bottom: 14px;
  left: 16px;
  display: flex;
  align-items: center;
  font-size: 11px;
  color: #666666;
  background: rgba(var(--n-color-rgb, 255, 255, 255), 0.85);
  backdrop-filter: blur(8px);
  padding: 4px 10px;
  border-radius: 6px;
  border: 1px solid var(--n-border-color);
  pointer-events: none;
}

/* Node Detail Slide Panel */
.node-detail-panel {
  position: absolute;
  top: 14px;
  right: 14px;
  bottom: 14px;
  width: 320px;
  background: var(--n-card-color, #ffffff);
  border: 1px solid var(--n-border-color);
  border-radius: 10px;
  box-shadow: -4px 0 20px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  z-index: 30;
  overflow: hidden;

  .panel-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 14px;
    border-bottom: 1px solid var(--n-border-color);

    .panel-title-group {
      display: flex;
      align-items: center;
      gap: 6px;
      font-weight: 600;
      font-size: 13px;
      color: var(--n-text-color);
    }
  }

  .panel-content {
    flex: 1;
    padding: 14px;
    overflow-y: auto;

    .detail-section {
      margin-bottom: 14px;
      padding-bottom: 10px;
      border-bottom: 1px solid var(--n-border-color);

      .section-title {
        font-size: 11px;
        font-weight: 600;
        color: #777777;
        margin-bottom: 8px;
        text-transform: uppercase;
        letter-spacing: 0.5px;
      }

      .detail-row {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 7px;
        font-size: 12px;

        .key {
          color: #555555;
        }

        .val {
          color: var(--n-text-color);
          font-weight: 500;

          &.mono {
            font-family: monospace;
            font-size: 11px;
          }
        }
      }
    }

    .panel-actions {
      margin-top: 14px;

      .action-grid {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 8px;
      }
    }
  }
}

/* Transitions */
.slide-panel-enter-active,
.slide-panel-leave-active {
  transition: transform 0.25s ease, opacity 0.25s ease;
}

.slide-panel-enter-from,
.slide-panel-leave-to {
  transform: translateX(30px);
  opacity: 0;
}
</style>
