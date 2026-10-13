<template>
  <div class="space-y-4">
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
      <div class="flex items-center gap-2">
        <GitNetworkOutline class="w-4 h-4 text-google-blue dark:text-google-blue-dark" />
        <span class="text-xs font-bold text-google-text-primary dark:text-google-text-darkPrimary">
          {{ t('admin.clusters.nodes.title', '计算节点与调度拓扑') }}
        </span>
      </div>

      <div class="flex items-center gap-2 flex-wrap">
        <VBtn
          clz="normal"
          size="medium"
          :left-icon="RefreshOutline"
          :loading="props.loading"
          @click="emit('refresh')"
        >
          {{ t('admin.clusters.nodes.refresh', '刷新') }}
        </VBtn>

        <VBtn
          v-if="isK3sCluster"
          clz="normal"
          size="medium"
          :left-icon="SyncOutline"
          :loading="syncingK3s"
          @click="handleSyncK3s"
        >
          {{ t('admin.clusters.nodes.sync_k3s', '节点扫描') }}
        </VBtn>

        <VBtn
          clz="normal"
          size="medium"
          :left-icon="PulseOutline"
          :loading="checkingAllHealth"
          @click="handleCheckAllHealth"
        >
          {{ t('admin.clusters.nodes.check_all_health', '节点检测') }}
        </VBtn>

        <VBtn
          clz="primary"
          size="medium"
          :left-icon="AddOutline"
          @click="emit('add-node')"
        >
          {{ t('admin.clusters.nodes.add_node', '添加节点') }}
        </VBtn>
      </div>
    </div>

    <VTable
      :columns="nodeColumns"
      :data="props.nodes"
      :loading="props.loading"
      :pagination="false"
      :bordered="false"
      :row-key="(row: AdminClusterNodeVO) => String(row.id)"
      :scroll-x="1200"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, h } from 'vue'
import { useI18n } from 'vue-i18n'
import { useMessage, useDialog, type DataTableColumns } from 'naive-ui'
import {
  AddOutline,
  RefreshOutline,
  GitNetworkOutline,
  PulseOutline,
  CreateOutline,
  SyncOutline,
  TrashOutline
} from '@vicons/ionicons5'
import VBtn from '@/components/common/VBtn.vue'
import VTable from '@/components/common/VTable.vue'
import {
  removeAdminClusterNodeApi,
  syncImagesToClusterNodeApi,
  checkAllAdminClusterNodesHealthApi,
  joinAdminClusterNodeApi,
  syncK3sClusterNodesApi
} from '@/api/cluster'
import type { AdminClusterNodeVO } from '@/types/cluster'

interface Props {
  nodes: AdminClusterNodeVO[]
  loading?: boolean
  clusterType?: string
}

const props = withDefaults(defineProps<Props>(), {
  nodes: () => [],
  loading: false,
  clusterType: 'DOCKER_SWARM'
})

const emit = defineEmits<{
  (e: 'refresh'): void
  (e: 'add-node'): void
  (e: 'edit-node', node: AdminClusterNodeVO): void
  (e: 'node-updated'): void
}>()

const { t } = useI18n()
const message = useMessage()
const dialog = useDialog()

const checkingAllHealth = ref(false)
const syncingK3s = ref(false)
const joiningNodeId = ref<string | number | null>(null)
const syncingImagesNodeId = ref<string | number | null>(null)

const isK3sCluster = computed(() => {
  return props.clusterType === 'K3S' || props.nodes?.some((n) => n.clusterType === 'K3S')
})

const nodeColumns = computed<DataTableColumns<AdminClusterNodeVO>>(() => [
  {
    title: t('admin.clusters.nodes.name', '节点名称'),
    key: 'nodeName',
    width: 200,
    render(row) {
      return h('div', { class: 'flex items-center gap-1.5' }, [
        h('span', row.nodeName),
        row.hasSshAuth
          ? h('span', { class: 'text-green-600 dark:text-green-400' }, 'SSH')
          : null
      ])
    }
  },
  {
    title: t('admin.clusters.nodes.ip', 'IP / 端口'),
    key: 'ipAddress',
    width: 170,
    render(row) {
      return `${row.ipAddress}:${row.sshPort || 22}`
    }
  },
  {
    title: t('admin.clusters.nodes.role', '角色'),
    key: 'role',
    width: 110,
    render(row) {
      const isMaster = row.role === 'MASTER'
      return h(
        'span',
        {
          class: isMaster ? 'text-purple-600 dark:text-purple-400' : 'text-cyan-600 dark:text-cyan-400'
        },
        isMaster ? t('admin.clusters.nodes.role_master', '主节点') : t('admin.clusters.nodes.role_worker', '子节点')
      )
    }
  },
  {
    title: t('admin.clusters.nodes.is_resource', '赛题调度'),
    key: 'isResourceNode',
    width: 130,
    render(row) {
      return h(
        'span',
        {
          class: row.isResourceNode ? 'text-green-600 dark:text-green-400' : 'text-google-text-muted dark:text-google-text-darkMuted'
        },
        row.isResourceNode ? '参与赛题调度' : '纯控制面'
      )
    }
  },
  {
    title: t('admin.clusters.nodes.status', '集群状态'),
    key: 'status',
    width: 150,
    render(row) {
      const isMaster = row.role === 'MASTER'
      const inCluster = Boolean(row.inCluster) || isMaster
      const isReady = row.status === 'READY'
      const isJoining = row.status === 'JOINING'

      let colorClass = 'text-red-600 dark:text-red-400'
      let label = '异常 / 离线'

      if (isJoining) {
        colorClass = 'text-amber-600 dark:text-amber-400'
        label = '加入中...'
      } else if (isReady && inCluster) {
        colorClass = 'text-green-600 dark:text-green-400'
        label = '调度就绪'
      } else if (isReady && !inCluster && !isMaster) {
        colorClass = 'text-amber-600 dark:text-amber-400'
        label = '未入集群'
      }

      return h('div', { class: 'flex flex-col' }, [
        h('span', { class: colorClass }, label),
        !isMaster && inCluster
          ? h('span', { class: 'text-xs text-google-text-secondary dark:text-google-text-darkSecondary' }, row.clusterType === 'K3S' ? '已入 K3s' : '已入 Swarm')
          : null
      ])
    }
  },
  {
    title: t('admin.clusters.nodes.containers', '容器 / 镜像'),
    key: 'counts',
    width: 140,
    render(row) {
      return `${row.containerCount ?? 0} 容器 / ${row.imageCount ?? 0} 镜像`
    }
  },
  {
    title: t('admin.clusters.nodes.specs', '硬件配置'),
    key: 'specs',
    width: 150,
    render(row) {
      const cores = row.cpuCores ? `${row.cpuCores} 核` : '-'
      const mem = row.memoryFormatted || '-'
      return `${cores} / ${mem}`
    }
  },
  {
    title: t('common.actions', '操作'),
    key: 'actions',
    width: 250,
    align: 'center',
    render(row) {
      if (row.role === 'MASTER') {
        return h('span', { class: 'text-xs text-google-text-muted dark:text-google-text-darkMuted' }, '本机控制面 (不可修改)')
      }

      const actions = []

      if (!row.inCluster) {
        actions.push(
          h(
            VBtn,
            {
              clz: 'warning',
              size: 'tiny',
              leftIcon: SyncOutline,
              loading: joiningNodeId.value === row.id,
              onClick: () => handleJoinCluster(row)
            },
            { default: () => '加入集群' }
          )
        )
      }

      actions.push(
        h(
          VBtn,
          {
            clz: 'primary',
            size: 'tiny',
            leftIcon: CreateOutline,
            onClick: () => emit('edit-node', row)
          },
          { default: () => t('admin.clusters.nodes.edit', '编辑') }
        )
      )

      actions.push(
        h(
          VBtn,
          {
            clz: 'normal',
            size: 'tiny',
            leftIcon: RefreshOutline,
            loading: syncingImagesNodeId.value === row.id,
            onClick: () => handleSyncImagesToNode(row)
          },
          { default: () => t('admin.clusters.nodes.sync_images', '同步镜像') }
        )
      )

      actions.push(
        h(
          VBtn,
          {
            clz: 'danger',
            size: 'tiny',
            leftIcon: TrashOutline,
            onClick: () => handleRemoveNode(row)
          },
          { default: () => t('admin.clusters.nodes.remove', '移除') }
        )
      )

      return h('div', { class: 'flex items-center justify-center gap-1.5' }, actions)
    }
  }
])

async function handleCheckAllHealth() {
  checkingAllHealth.value = true
  try {
    const res = await checkAllAdminClusterNodesHealthApi()
    if (res.data) {
      message.success(t('admin.clusters.nodes.health_check_all_success', '全量存活对账完成'))
      emit('node-updated')
    }
  } catch (err: any) {
    message.error(err.message || '全量存活检测失败')
  } finally {
    checkingAllHealth.value = false
  }
}

async function handleSyncK3s() {
  syncingK3s.value = true
  try {
    await syncK3sClusterNodesApi()
    message.success(t('admin.clusters.nodes.sync_k3s_success', 'K3s 节点同步扫描完成'))
    emit('refresh')
  } catch (err: any) {
    message.error(err.message || '扫描同步 K3s 节点失败')
  } finally {
    syncingK3s.value = false
  }
}

async function handleJoinCluster(node: AdminClusterNodeVO) {
  joiningNodeId.value = node.id
  try {
    const res = await joinAdminClusterNodeApi(node.id)
    if (res.data) {
      message.success(t('admin.clusters.topology.action_rejoin_success', '节点重新编排加入集群指令已下发'))
      emit('node-updated')
    }
  } catch (err: any) {
    message.error(err.message || '加入集群失败')
  } finally {
    joiningNodeId.value = null
  }
}

async function handleSyncImagesToNode(node: AdminClusterNodeVO) {
  syncingImagesNodeId.value = node.id
  try {
    await syncImagesToClusterNodeApi(node.id)
    message.success(t('admin.clusters.nodes.sync_success', '镜像同步指令已下发至节点'))
    emit('node-updated')
  } catch (err: any) {
    message.error(err.message || '同步镜像失败')
  } finally {
    syncingImagesNodeId.value = null
  }
}

function handleRemoveNode(node: AdminClusterNodeVO) {
  dialog.warning({
    title: '确认移除集群计算节点',
    content: `确认将节点【${node.nodeName}】(${node.ipAddress}) 从集群中移除？移除后该节点上的赛题容器调度将终止。`,
    positiveText: '确认移除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await removeAdminClusterNodeApi(node.id)
        message.success('节点已成功从集群移除')
        emit('node-updated')
      } catch (err: any) {
        message.error(err.message || '移除节点失败')
      }
    }
  })
}
</script>
