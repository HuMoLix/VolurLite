<template>
  <div class="space-y-6 max-w-6xl mx-auto">
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h2 class="text-xl font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
          {{ isEdit ? (competitionTitle || t('admin.competitions.manage_title', 'CTF 竞赛运维')) : t('admin.competitions.modal_create_title', '创建新 CTF 竞赛') }}
        </h2>
      </div>
      <div v-if="isEdit" class="flex items-center gap-2">
        <VBtn
          clz="normal"
          size="medium"
          :left-icon="CloudDownloadOutline"
          @click="showExportModal = true"
        >
          {{ t('admin.competitions.export_scores_btn', '导出数据报表') }}
        </VBtn>
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
        <AdminCtfCompetitionDetailView
          v-if="activeTab === 'basic'"
          :comp-id="compId"
          :is-edit="isEdit"
          @update-title="(title) => competitionTitle = title"
          @saved="handleDetailSaved"
        />

        <AdminCtfCompetitionChallengeDetailView
          v-else-if="activeTab === 'challenges' && compId"
          :comp-id="compId"
        />

        <AdminCtfCompetitionAnnounceView
          v-else-if="activeTab === 'announcements' && compId"
          :comp-id="compId"
        />

        <AdminCtfCompetitionTeamView
          v-else-if="activeTab === 'teams' && compId"
          :comp-id="compId"
        />

        <AdminCtfCompetitionSubmissionView
          v-else-if="activeTab === 'submissions' && compId"
          :comp-id="compId"
        />

        <AdminCtfCompetitionWriteupView
          v-else-if="activeTab === 'writeups' && compId"
          :comp-id="compId"
        />

        <AdminCtfCompetitionLogView
          v-else-if="activeTab === 'logs' && compId"
          :comp-id="compId"
        />
      </div>
    </div>

    <AdminCompetitionScoreExportModal
      v-if="compId"
      v-model:show="showExportModal"
      :competition-id="compId"
      competition-type="CTF"
      :competition-title="competitionTitle"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  InformationCircleOutline,
  FlagOutline,
  MegaphoneOutline,
  PeopleOutline,
  CheckmarkDoneOutline,
  DocumentAttachOutline,
  ListOutline,
  CloudDownloadOutline
} from '@vicons/ionicons5'
import VBtn from '@/components/common/VBtn.vue'
import VFilterBtnGroup from '@/components/common/VFilterBtnGroup.vue'
import type { FilterOption } from '@/components/common/types'
import AdminCompetitionScoreExportModal from './components/AdminCompetitionScoreExportModal.vue'
import AdminCtfCompetitionDetailView from './AdminCtfCompetitionDetailView.vue'
import AdminCtfCompetitionChallengeDetailView from './AdminCtfCompetitionChallengeDetailView.vue'
import AdminCtfCompetitionAnnounceView from './AdminCtfCompetitionAnnounceView.vue'
import AdminCtfCompetitionTeamView from './AdminCtfCompetitionTeamView.vue'
import AdminCtfCompetitionSubmissionView from './AdminCtfCompetitionSubmissionView.vue'
import AdminCtfCompetitionWriteupView from './AdminCtfCompetitionWriteupView.vue'
import AdminCtfCompetitionLogView from './AdminCtfCompetitionLogView.vue'

defineOptions({
  name: 'AdminCtfCompetitionEditView'
})

const route = useRoute()
const { t } = useI18n()

const compId = computed(() => route.params.id as string | undefined)
const isEdit = computed(() => Boolean(compId.value))

const activeTab = ref('basic')
const competitionTitle = ref('')
const showExportModal = ref(false)

const tabs = computed<FilterOption<string>[]>(() => [
  { value: 'basic', label: t('admin.competitions.tab_basic', '基础信息'), icon: InformationCircleOutline },
  { value: 'challenges', label: t('admin.competitions.tab_challenges', '赛题管理'), icon: FlagOutline },
  { value: 'announcements', label: t('admin.competitions.tab_announcements', '比赛公告'), icon: MegaphoneOutline },
  { value: 'teams', label: t('admin.competitions.tab_teams', '战队审核'), icon: PeopleOutline },
  { value: 'submissions', label: t('admin.competitions.tab_submissions', '提交记录'), icon: CheckmarkDoneOutline },
  { value: 'writeups', label: t('admin.competitions.tab_writeups', '题解 WP'), icon: DocumentAttachOutline },
  { value: 'logs', label: t('admin.competitions.tab_logs', '比赛日志'), icon: ListOutline }
])

function handleDetailSaved(id?: string | number) {
  if (id && !compId.value) {
    activeTab.value = 'basic'
  }
}
</script>
