<template>
  <div class="w-full">
    <ClusterTopology
      :nodes="nodes"
      :settings="settings"
      :loading="loading"
      @refresh="emit('refresh')"
      @edit-node="(node) => emit('edit-node', node)"
    />
  </div>
</template>

<script setup lang="ts">
import ClusterTopology from '../components/ClusterTopology.vue'
import type { AdminClusterSettingsVO, AdminClusterNodeVO } from '@/types/cluster'

interface Props {
  nodes: AdminClusterNodeVO[]
  settings?: AdminClusterSettingsVO
  loading?: boolean
}

defineProps<Props>()

const emit = defineEmits<{
  (e: 'refresh'): void
  (e: 'edit-node', node: AdminClusterNodeVO): void
}>()
</script>
