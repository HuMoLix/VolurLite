<template>
  <div class="space-y-6 max-w-6xl mx-auto">
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h2 class="text-xl font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
          {{ isEdit ? (competitionTitle || t('admin.competitions.manage_title', '理论竞赛运维')) : t('admin.competitions.modal_create_title', '创建新理论竞赛') }}
        </h2>
      </div>
    </div>

    <div class="space-y-6">
      <div v-if="isEdit" class="pt-1">
        <VFilterBtnGroup
          v-model:value="activeTab"
          :options="tabs"
          size="medium"
          :bullet="false"
        />
      </div>

      <div class="space-y-6">
        <AdminTheoryCompetitionDetailView
          v-if="activeTab === 'basic'"
          :comp-id="compId"
          :is-edit="isEdit"
          @update-title="(title) => competitionTitle = title"
          @saved="handleDetailSaved"
        />

        <AdminTheoryCompetitionChallengeDetailView
          v-else-if="activeTab === 'challenges' && compId"
          :comp-id="compId"
        />

        <AdminTheoryCompetitionAnnounceView
          v-else-if="activeTab === 'announcements' && compId"
          :comp-id="compId"
        />

        <AdminTheoryCompetitionTeamView
          v-else-if="activeTab === 'teams' && compId"
          :comp-id="compId"
        />

        <AdminTheoryCompetitionMonitorView
          v-else-if="activeTab === 'monitor' && compId"
          :comp-id="compId"
        />

        <AdminTheoryCompetitionLogView
          v-else-if="activeTab === 'logs' && compId"
          :comp-id="compId"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  InformationCircleOutline,
  DocumentTextOutline,
  MegaphoneOutline,
  PeopleOutline,
  DesktopOutline,
  ListOutline
} from '@vicons/ionicons5'
import VFilterBtnGroup from '@/components/common/VFilterBtnGroup.vue'
import type { FilterOption } from '@/components/common/types'
import AdminTheoryCompetitionDetailView from './AdminTheoryCompetitionDetailView.vue'
import AdminTheoryCompetitionChallengeDetailView from './AdminTheoryCompetitionChallengeDetailView.vue'
import AdminTheoryCompetitionAnnounceView from './AdminTheoryCompetitionAnnounceView.vue'
import AdminTheoryCompetitionTeamView from './AdminTheoryCompetitionTeamView.vue'
import AdminTheoryCompetitionMonitorView from './AdminTheoryCompetitionMonitorView.vue'
import AdminTheoryCompetitionLogView from './AdminTheoryCompetitionLogView.vue'

defineOptions({
  name: 'AdminTheoryCompetitionEditView'
})

const route = useRoute()
const { t } = useI18n()

const compId = computed(() => route.params.id as string | undefined)
const isEdit = computed(() => Boolean(compId.value))

const activeTab = ref('basic')
const competitionTitle = ref('')

const tabs = computed<FilterOption<string>[]>(() => [
  { value: 'basic', label: t('admin.competitions.tab_basic', '基础信息'), icon: InformationCircleOutline },
  { value: 'challenges', label: t('admin.competitions.tab_challenges', '理论试卷考题'), icon: DocumentTextOutline },
  { value: 'announcements', label: t('admin.competitions.tab_announcements', '比赛公告'), icon: MegaphoneOutline },
  { value: 'teams', label: t('admin.competitions.tab_teams', '战队审核'), icon: PeopleOutline },
  { value: 'monitor', label: t('admin.competitions.tab_monitor', '监考大屏'), icon: DesktopOutline },
  { value: 'logs', label: t('admin.competitions.tab_logs', '比赛日志'), icon: ListOutline }
])

function handleDetailSaved(id?: string | number) {
  if (id && !compId.value) {
    activeTab.value = 'basic'
  }
}
</script>
