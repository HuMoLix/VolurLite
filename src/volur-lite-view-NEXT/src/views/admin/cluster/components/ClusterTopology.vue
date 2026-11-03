<template>
  <div
    ref="containerRef"
    class="cluster-topology-container relative w-full h-[640px] rounded-2xl border border-google-border-light dark:border-google-border-dark overflow-hidden select-none cursor-grab active:cursor-grabbing bg-white dark:bg-[#202124]"
    :class="{ 'fixed inset-0 z-50 rounded-none border-none h-screen w-screen': isFullscreen }"
    @wheel.prevent="handleWheel"
    @mousedown="handleCanvasMouseDown"
  >
    <div class="absolute top-4 left-4 right-4 flex items-center justify-between gap-3 z-20 pointer-events-none">
      <div class="pointer-events-auto flex items-center gap-2.5 bg-white/90 dark:bg-[#292a2d]/90 backdrop-blur-md p-2 rounded-xl border border-google-border-light dark:border-google-border-dark shadow-sm">
        <div class="flex items-center gap-1.5 font-semibold text-xs text-google-text-primary dark:text-google-text-darkPrimary px-1">
          <GitNetworkOutline class="w-4 h-4 text-google-blue dark:text-google-blue-dark" />
          <span>{{ t('admin.clusters.topology.title', '集群拓扑架构') }}</span>
        </div>

        <div class="hidden sm:flex items-center gap-2 pl-2 border-l border-google-border-light dark:border-google-border-dark text-[11px]">
          <span class="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full bg-google-surface-light dark:bg-google-surface-dark text-google-text-secondary dark:text-google-text-darkSecondary">
            <span class="w-1.5 h-1.5 rounded-full bg-google-blue"></span>
            总计算节点: {{ props.nodes.length }}
          </span>
          <span class="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full bg-green-500/10 text-green-600 dark:text-green-400">
            <span class="w-1.5 h-1.5 rounded-full bg-green-500"></span>
            就绪: {{ inClusterCount }} / {{ props.nodes.length }}
          </span>
          <span class="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full bg-google-surface-light dark:bg-google-surface-dark text-google-text-secondary dark:text-google-text-darkSecondary font-mono">
            <ServerOutline class="w-3.5 h-3.5" />
            {{ props.settings?.dockerSwarmNetwork || 'volur_network' }}
          </span>
        </div>
      </div>

      <div class="pointer-events-auto flex items-center gap-1.5 bg-white/90 dark:bg-[#292a2d]/90 backdrop-blur-md p-1.5 rounded-xl border border-google-border-light dark:border-google-border-dark shadow-sm">
        <VBtn clz="normal" size="tiny" :left-icon="AddOutline" @click="zoomIn" />
        <VBtn clz="normal" size="tiny" @click="resetZoom">
          {{ Math.round(scale * 100) }}%
        </VBtn>
        <VBtn clz="normal" size="tiny" :left-icon="RemoveOutline" @click="zoomOut" />
        <VBtn clz="normal" size="tiny" :left-icon="isFullscreen ? ContractOutline : ExpandOutline" @click="toggleFullscreen" />
        <VBtn clz="normal" size="tiny" :left-icon="RefreshOutline" :loading="props.loading" @click="handleRefresh" />
      </div>
    </div>

    <div
      class="topology-viewport absolute top-0 left-0 w-[4000px] h-[4000px]"
      :style="{
        transform: `translate(${panX}px, ${panY}px) scale(${scale})`,
        transformOrigin: '0 0'
      }"
    >
      <svg class="absolute inset-0 w-full h-full pointer-events-none">
        <g v-for="link in connectionLinks" :key="link.id">
          <path
            :d="link.path"
            fill="none"
            :stroke="link.inCluster ? '#10b981' : '#ea4335'"
            stroke-width="2.5"
            :stroke-dasharray="link.inCluster ? 'none' : '6 4'"
            class="transition-colors duration-300"
          />
          <g :transform="`translate(${link.midX}, ${link.midY})`">
            <circle
              r="10"
              :fill="link.inCluster ? '#10b981' : '#ea4335'"
              stroke="#ffffff"
              stroke-width="2"
            />
            <text
              y="3.5"
              text-anchor="middle"
              font-size="9"
              font-weight="bold"
              fill="#ffffff"
            >
              {{ link.inCluster ? '✓' : '!' }}
            </text>
          </g>
        </g>
      </svg>

      <div
        v-for="nodePos in positionedNodes"
        :key="nodePos.node.id"
        class="absolute select-none cursor-pointer w-[280px] rounded-2xl bg-white dark:bg-[#292a2d] border transition-shadow duration-200"
        :class="[
          selectedNode?.id === nodePos.node.id
            ? 'border-google-blue dark:border-google-blue-dark shadow-lg ring-2 ring-google-blue/20'
            : 'border-google-border-light dark:border-google-border-dark shadow-sm hover:shadow-md'
        ]"
        :style="{
          left: `${nodePos.x}px`,
          top: `${nodePos.y}px`
        }"
        @mousedown.stop="handleNodeMouseDown($event, nodePos)"
        @click.stop="selectNode(nodePos.node)"
      >
        <div class="p-3.5 space-y-3">
          <div class="flex items-center justify-between gap-2">
            <div class="flex items-center gap-2.5 min-w-0">
              <div
                class="w-8 h-8 rounded-xl flex items-center justify-center shrink-0"
                :class="nodePos.node.role === 'MASTER' ? 'bg-purple-500/10 text-purple-600 dark:text-purple-400' : (nodePos.node.inCluster ? 'bg-green-500/10 text-green-600 dark:text-green-400' : 'bg-red-500/10 text-red-600 dark:text-red-400')"
              >
                <ShieldCheckmarkOutline v-if="nodePos.node.role === 'MASTER'" class="w-4 h-4" />
                <CheckmarkCircleOutline v-else-if="nodePos.node.inCluster" class="w-4 h-4" />
                <AlertCircleOutline v-else class="w-4 h-4" />
              </div>
              <div class="min-w-0">
                <div class="text-xs font-bold text-google-text-primary dark:text-google-text-darkPrimary truncate">
                  {{ nodePos.node.nodeName }}
                </div>
                <div class="text-[11px] text-google-text-secondary dark:text-google-text-darkSecondary font-mono">
                  {{ nodePos.node.ipAddress }}
                </div>
              </div>
            </div>

            <span
              class="px-2 py-0.5 text-[10px] font-semibold rounded-full uppercase shrink-0"
              :class="nodePos.node.role === 'MASTER' ? 'bg-purple-500/10 text-purple-600 dark:text-purple-400' : (nodePos.node.inCluster ? 'bg-green-500/10 text-green-600 dark:text-green-400' : 'bg-red-500/10 text-red-600 dark:text-red-400')"
            >
              {{ nodePos.node.role === 'MASTER' ? 'MASTER' : (nodePos.node.inCluster ? 'IN CLUSTER' : 'OFFLINE') }}
            </span>
          </div>

          <div class="grid grid-cols-3 gap-2 py-2 border-y border-google-border-light/70 dark:border-google-border-dark/70 text-center">
            <div>
              <div class="text-[10px] text-google-text-secondary dark:text-google-text-darkSecondary">容器</div>
              <div class="text-xs font-semibold text-google-text-primary dark:text-google-text-darkPrimary mt-0.5">
                {{ nodePos.node.containerCount || 0 }}
              </div>
            </div>
            <div>
              <div class="text-[10px] text-google-text-secondary dark:text-google-text-darkSecondary">镜像</div>
              <div class="text-xs font-semibold text-google-text-primary dark:text-google-text-darkPrimary mt-0.5">
                {{ nodePos.node.imageCount || 0 }}
              </div>
            </div>
            <div>
              <div class="text-[10px] text-google-text-secondary dark:text-google-text-darkSecondary">配置</div>
              <div class="text-xs font-semibold text-google-text-primary dark:text-google-text-darkPrimary mt-0.5 truncate">
                {{ nodePos.node.cpuCores ? `${nodePos.node.cpuCores}C` : '-' }}
              </div>
            </div>
          </div>

          <div class="flex items-center justify-between text-[11px]">
            <div class="flex items-center gap-1.5 text-google-text-secondary dark:text-google-text-darkSecondary">
              <span
                class="w-2 h-2 rounded-full"
                :class="nodePos.node.inCluster ? 'bg-green-500' : 'bg-red-500'"
              ></span>
              <span>{{ nodePos.node.role === 'MASTER' ? '编排控制面' : (nodePos.node.inCluster ? '调度就绪' : '未入网') }}</span>
            </div>
            <ChevronForwardOutline class="w-3.5 h-3.5 text-google-text-muted dark:text-google-text-darkMuted" />
          </div>
        </div>
      </div>
    </div>

    <div class="absolute bottom-4 left-4 flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-white/80 dark:bg-[#292a2d]/80 backdrop-blur-md border border-google-border-light dark:border-google-border-dark text-[11px] text-google-text-secondary dark:text-google-text-darkSecondary pointer-events-none">
      <GitNetworkOutline class="w-3.5 h-3.5 text-google-blue" />
      <span>按住空白区域拖拽平移，滚轮缩放画布；点击节点卡片查看详情与控制</span>
    </div>

    <transition name="slide-panel">
      <div
        v-if="selectedNode"
        class="absolute top-0 right-0 bottom-0 w-80 sm:w-96 bg-white dark:bg-[#292a2d] border-l border-google-border-light dark:border-google-border-dark shadow-2xl z-30 flex flex-col overflow-y-auto"
      >
        <div class="p-4 border-b border-google-border-light dark:border-google-border-dark flex items-center justify-between">
          <div class="flex items-center gap-2 min-w-0">
            <ServerOutline class="w-4 h-4 text-google-blue dark:text-google-blue-dark shrink-0" />
            <span class="font-bold text-sm text-google-text-primary dark:text-google-text-darkPrimary truncate">
              {{ selectedNode.nodeName }}
            </span>
          </div>
          <button
            type="button"
            class="p-1 rounded-lg hover:bg-google-surface-light dark:hover:bg-google-surface-dark text-google-text-muted"
            @click="selectedNode = null"
          >
            <CloseOutline class="w-4 h-4" />
          </button>
        </div>

        <div class="p-4 space-y-5 flex-1">
          <div class="space-y-2.5">
            <div class="text-xs font-bold text-google-text-primary dark:text-google-text-darkPrimary">
              节点基础配置
            </div>
            <div class="space-y-1.5 text-xs">
              <div class="flex items-center justify-between">
                <span class="text-google-text-secondary dark:text-google-text-darkSecondary">角色类型:</span>
                <span class="font-semibold">{{ selectedNode.role === 'MASTER' ? '主控制节点' : '工作节点' }}</span>
              </div>
              <div class="flex items-center justify-between">
                <span class="text-google-text-secondary dark:text-google-text-darkSecondary">IP 地址:</span>
                <span class="font-mono">{{ selectedNode.ipAddress }}:{{ selectedNode.sshPort || 22 }}</span>
              </div>
              <div class="flex items-center justify-between">
                <span class="text-google-text-secondary dark:text-google-text-darkSecondary">赛题调度:</span>
                <span :class="selectedNode.isResourceNode ? 'text-green-600 dark:text-green-400 font-semibold' : 'text-google-text-muted'">
                  {{ selectedNode.isResourceNode ? '参与赛题调度' : '纯控制面 (不调度)' }}
                </span>
              </div>
              <div class="flex items-center justify-between">
                <span class="text-google-text-secondary dark:text-google-text-darkSecondary">集群状态:</span>
                <span :class="selectedNode.inCluster ? 'text-green-600 dark:text-green-400 font-semibold' : 'text-red-600 dark:text-red-400 font-semibold'">
                  {{ selectedNode.inCluster ? '已入网 (Ready)' : '未加入集群' }}
                </span>
              </div>
              <div v-if="selectedNode.clusterNodeId" class="flex items-center justify-between">
                <span class="text-google-text-secondary dark:text-google-text-darkSecondary">Node ID:</span>
                <span class="font-mono text-[11px] truncate max-w-[160px]">{{ selectedNode.clusterNodeId }}</span>
              </div>
            </div>
          </div>

          <div class="space-y-2.5 pt-3 border-t border-google-border-light dark:border-google-border-dark">
            <div class="text-xs font-bold text-google-text-primary dark:text-google-text-darkPrimary">
              运行时与资源
            </div>
            <div class="space-y-1.5 text-xs">
              <div class="flex items-center justify-between">
                <span class="text-google-text-secondary dark:text-google-text-darkSecondary">Docker 版本:</span>
                <span>{{ selectedNode.dockerVersion || 'Docker Engine' }}</span>
              </div>
              <div class="flex items-center justify-between">
                <span class="text-google-text-secondary dark:text-google-text-darkSecondary">CPU / 内存:</span>
                <span>{{ selectedNode.cpuCores || 1 }} 核 / {{ selectedNode.memoryFormatted || '1 GB' }}</span>
              </div>
              <div class="flex items-center justify-between">
                <span class="text-google-text-secondary dark:text-google-text-darkSecondary">运行容器数:</span>
                <span class="font-mono font-semibold">{{ selectedNode.containerCount || 0 }}</span>
              </div>
              <div class="flex items-center justify-between">
                <span class="text-google-text-secondary dark:text-google-text-darkSecondary">缓存镜像数:</span>
                <span class="font-mono font-semibold">{{ selectedNode.imageCount || 0 }}</span>
              </div>
            </div>
          </div>

          <div class="pt-3 border-t border-google-border-light dark:border-google-border-dark space-y-2">
            <div class="text-xs font-bold text-google-text-primary dark:text-google-text-darkPrimary mb-2">
              节点快捷操作
            </div>

            <VBtn
              v-if="selectedNode.role !== 'MASTER' && !selectedNode.inCluster"
              clz="danger"
              size="small"
              block
              :left-icon="SyncOutline"
              :loading="joiningNodeId === selectedNode.id"
              @click="handleRejoinCluster(selectedNode)"
            >
              重新编排加入集群
            </VBtn>

            <div class="grid grid-cols-2 gap-2">
              <VBtn
                clz="normal"
                size="small"
                :left-icon="PulseOutline"
                :loading="checkingHealthNodeId === selectedNode.id"
                @click="handleNodeHealthCheck(selectedNode)"
              >
                存活检测
              </VBtn>

              <VBtn
                clz="normal"
                size="small"
                :left-icon="RefreshOutline"
                :loading="syncingNodeId === selectedNode.id"
                @click="handleNodeSyncImages(selectedNode)"
              >
                同步镜像
              </VBtn>
            </div>

            <VBtn
              v-if="selectedNode.role !== 'MASTER'"
              clz="normal"
              size="small"
              block
              :left-icon="CreateOutline"
              @click="handleEditNode(selectedNode)"
            >
              编辑此节点配置
            </VBtn>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useMessage } from 'naive-ui'
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
import VBtn from '@/components/common/VBtn.vue'
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

const scale = ref(1.0)
const panX = ref(100)
const panY = ref(60)

const isPanning = ref(false)
const startPanMouseX = ref(0)
const startPanMouseY = ref(0)
const initialPanX = ref(0)
const initialPanY = ref(0)

interface PositionedNode {
  node: AdminClusterNodeVO
  x: number
  y: number
}

const positionedNodes = ref<PositionedNode[]>([])
const draggingNode = ref<PositionedNode | null>(null)
const dragOffset = ref({ x: 0, y: 0 })

const selectedNode = ref<AdminClusterNodeVO | null>(null)

const checkingHealthNodeId = ref<string | number | null>(null)
const syncingNodeId = ref<string | number | null>(null)
const joiningNodeId = ref<string | number | null>(null)

const inClusterCount = computed(() => {
  return props.nodes.filter(n => n.inCluster || n.role === 'MASTER').length
})

function initNodePositions() {
  const master = props.nodes.find(n => n.role === 'MASTER') || props.nodes[0]
  const workers = props.nodes.filter(n => n.id !== master?.id)

  const result: PositionedNode[] = []

  const masterX = 360
  const masterY = 80
  if (master) {
    result.push({
      node: master,
      x: masterX,
      y: masterY
    })
  }

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

  const CARD_WIDTH = 280
  const CARD_HEIGHT = 140

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

function zoomIn() {
  scale.value = Math.min(2.2, Number((scale.value + 0.15).toFixed(2)))
}

function zoomOut() {
  scale.value = Math.max(0.4, Number((scale.value - 0.15).toFixed(2)))
}

function resetZoom() {
  scale.value = 1.0
  panX.value = 100
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

async function handleRejoinCluster(node: AdminClusterNodeVO) {
  joiningNodeId.value = node.id
  try {
    const res = await joinAdminClusterNodeApi(node.id)
    if (res.data) {
      message.success('已下发重新加入集群指令')
      emit('refresh')
    }
  } catch (err: any) {
    message.error(err.message || '加入集群失败')
  } finally {
    joiningNodeId.value = null
  }
}

async function handleNodeHealthCheck(node: AdminClusterNodeVO) {
  checkingHealthNodeId.value = node.id
  try {
    const res = await checkAdminClusterNodeHealthApi(node.id)
    if (res.data) {
      message.success(`节点健康状态: [${res.data.status}]`)
      emit('refresh')
    }
  } catch {
    message.error('检测节点存活失败')
  } finally {
    checkingHealthNodeId.value = null
  }
}

async function handleNodeSyncImages(node: AdminClusterNodeVO) {
  syncingNodeId.value = node.id
  try {
    await syncImagesToClusterNodeApi(node.id)
    message.success('已下发镜像同步指令')
  } catch {
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

<style scoped>
.slide-panel-enter-active,
.slide-panel-leave-active {
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.slide-panel-enter-from,
.slide-panel-leave-to {
  transform: translateX(100%);
}
</style>
