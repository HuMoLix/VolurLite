<template>
  <div class="space-y-4">
    <AdminTheoryCompetitionTeamMonitor :competition-id="compId" />
  </div>
</template>

<script setup lang="ts">
import AdminTheoryCompetitionTeamMonitor from './components/AdminTheoryCompetitionTeamMonitor.vue'

defineOptions({
  name: 'AdminTheoryCompetitionMonitorView'
})

defineProps<{
  compId: string | number
}>()
</script>
