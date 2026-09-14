<template>
  <AppTableCard
    :bordered="true"
    :table-bordered="true"
    :scroll-x="1440"
    :columns="nodeColumns"
    :data="nodes"
    :loading="loading"
    table-class="nodes-table"
  >
    <template #toolbar>
      <div class="nodes-toolbar">
        <div class="toolbar-title">
          <n-icon size="18" style="margin-right: 6px;">
            <GitNetworkOutline />
          </n-icon>
          <span>{{ t('admin.clusters.nodes.title') }}</span>
        </div>
        <div class="toolbar-actions">
          <n-button quaternary size="small" :loading="loading" @click="emit('refresh')">
            <template #icon>
              <n-icon :component="RefreshOutline" />
            </template>
            {{ t('admin.clusters.nodes.refresh') }}
          </n-button>
          <n-button
            v-if="isK3sCluster"
            size="small"
            secondary
            type="primary"
            :loading="syncingK3s"
            @click="handleSyncK3s"
          >
            <template #icon>
              <n-icon :component="SyncOutline" />
            </template>
            {{ t('admin.clusters.nodes.sync_k3s') }}
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
            @click="emit('add-node')"
          >
            <template #icon>
              <n-icon :component="AddOutline" />
            </template>
            {{ t('admin.clusters.nodes.add_node') }}
          </n-button>
        </div>
      </div>
    </template>
  </AppTableCard>
</template>

<script setup lang="ts">
import { ref, computed, h } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  NButton,
  NIcon,
  NTag,
  NPopconfirm,
  useMessage,
  type DataTableColumns
} from 'naive-ui'
import AppTableCard from '@/components/common/AppTableCard.vue'
import {
  AddOutline,
  RefreshOutline,
  GitNetworkOutline,
  PulseOutline,
  CreateOutline,
  SyncOutline
} from '@vicons/ionicons5'
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

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'refresh'): void
  (e: 'add-node'): void
  (e: 'edit-node', node: AdminClusterNodeVO): void
  (e: 'node-updated'): void
}>()

const { t } = useI18n()
const message = useMessage()

const checkingAllHealth = ref(false)
const syncingK3s = ref(false)
const joiningNodeId = ref<string | number | null>(null)

const isK3sCluster = computed(() => props.clusterType === 'K3S' || props.nodes?.some(n => n.clusterType === 'K3S'))

async function handleSyncK3s() {
  syncingK3s.value = true
  try {
    await syncK3sClusterNodesApi()
    message.success(t('admin.clusters.nodes.sync_k3s_success'))
    emit('refresh')
  } catch (err: unknown) {
    const errorMsg = err instanceof Error ? err.message : '扫描同步 K3s 节点失败'
    message.error(errorMsg)
  } finally {
    syncingK3s.value = false
  }
}

// Table Columns for Nodes
const nodeColumns = computed<DataTableColumns<AdminClusterNodeVO>>(() => [
  {
    title: t('admin.clusters.nodes.name'),
    key: 'nodeName',
    width: 200,
    render(row) {
      return h('div', { class: 'node-name-cell' }, [
        h('span', { class: 'name-text' }, row.nodeName),
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
          ? h(NTag, { size: 'tiny', type: 'success', bordered: false }, { default: () => (row.clusterType === 'K3S' ? '已入 K3s' : '已入 Swarm') })
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
    fixed: 'right',
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
            onClick: () => emit('edit-node', row)
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

async function handleCheckAllHealth() {
  checkingAllHealth.value = true
  try {
    const res = await checkAllAdminClusterNodesHealthApi()
    if (res.data) {
      message.success(t('admin.clusters.nodes.health_check_all_success'))
      emit('node-updated')
    }
  } catch {
    message.error('全量存活检测失败')
  } finally {
    checkingAllHealth.value = false
  }
}

async function handleSyncImagesToNode(node: AdminClusterNodeVO) {
  if (node.role === 'MASTER') {
    message.warning('主控制节点无需同步镜像')
    return
  }
  try {
    await syncImagesToClusterNodeApi(node.id)
    message.success(t('admin.clusters.nodes.sync_success'))
    emit('node-updated')
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
      emit('node-updated')
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
    emit('node-updated')
  } catch {
    message.error('移除节点失败')
  }
}
</script>

<style scoped lang="scss">
.nodes-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;

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
</style>
