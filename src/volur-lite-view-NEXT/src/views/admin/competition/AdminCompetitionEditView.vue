<template>
  <div v-if="loading" class="p-12 text-center text-google-blue dark:text-google-blue-dark">
    {{ t('common.loading', '正在加载竞赛详情...') }}
  </div>
  <component
    :is="resolvedComponent"
    v-else
  />
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { getAdminCompetitionByIdApi } from '@/api/competition'
import AdminCtfCompetitionEditView from './AdminCtfCompetitionEditView.vue'
import AdminTheoryCompetitionEditView from './AdminTheoryCompetitionEditView.vue'

defineOptions({
  name: 'AdminCompetitionEditView'
})

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const loading = ref(false)
const competitionType = ref<string>('CTF')

const compId = computed(() => route.params.id as string | undefined)

const resolvedComponent = computed(() => {
  return competitionType.value === 'THEORY'
    ? AdminTheoryCompetitionEditView
    : AdminCtfCompetitionEditView
})

onMounted(async () => {
  if (compId.value) {
    loading.value = true
    try {
      const res = await getAdminCompetitionByIdApi(compId.value)
      if (res.data?.type) {
        competitionType.value = res.data.type
      }
    } finally {
      loading.value = false
    }
  } else {
    router.replace('/admin/competitions/create')
  }
})
</script>
