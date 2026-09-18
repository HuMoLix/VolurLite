<template>
  <div class="competition-arena-view ctf-competition-arena-view">
    <!-- Sub-navigation Tabs (Width fits content with padding, not stretching to 100%) -->
    <div class="arena-nav-bar">
      <n-tabs v-model:value="activeTab" type="line" size="large" @update:value="handleTabChange">
        <n-tab name="challenges">
          <template #default>
            <div class="tab-label">
              <n-icon :component="FlashOutline" />
              <span>{{ t('competition.tab_challenges') }}</span>
            </div>
          </template>
        </n-tab>
        <n-tab name="announcements">
          <template #default>
            <div class="tab-label">
              <n-icon :component="MegaphoneOutline" />
              <span>{{ t('competition.tab_announcements') }}</span>
            </div>
          </template>
        </n-tab>
        <n-tab name="scoreboard">
          <template #default>
            <div class="tab-label">
              <n-icon :component="TrophyOutline" />
              <span>{{ t('competition.tab_scoreboard') }}</span>
            </div>
          </template>
        </n-tab>
        <n-tab name="team">
          <template #default>
            <div class="tab-label">
              <n-icon :component="PeopleOutline" />
              <span>{{ t('competition.tab_my_team') }}</span>
            </div>
          </template>
        </n-tab>
      </n-tabs>
    </div>

    <!-- Tab Content Sections -->
    <div class="arena-content-section">
      <ChallengesTab
        v-if="activeTab === 'challenges'"
        ref="challengesTabRef"
        :competition="competition"
      />
      <AnnouncementsTab
        v-else-if="activeTab === 'announcements'"
        ref="announcementsTabRef"
        :competition-id="competition?.id"
      />
      <ScoreboardTab
        v-else-if="activeTab === 'scoreboard'"
        ref="scoreboardTabRef"
        :competition-id="competition?.id"
      />
      <TeamTab
        v-else-if="activeTab === 'team'"
        ref="teamTabRef"
        :competition-id="competition?.id"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: 'CtfCompetitionArenaView'
})

import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { NTabs, NTab, NIcon, useMessage } from 'naive-ui'
import {
  FlashOutline,
  TrophyOutline,
  PeopleOutline,
  MegaphoneOutline
} from '@vicons/ionicons5'
import { useI18n } from 'vue-i18n'
import { useCompetitionStore } from '@/store/modules/competition'
import {
  ChallengesTab,
  AnnouncementsTab,
  ScoreboardTab,
  TeamTab
} from './tabs'

const { t } = useI18n()
const message = useMessage()
const route = useRoute()
const competitionStore = useCompetitionStore()

const slug = computed(() => String(route.params.slug || ''))
const activeTab = ref<'challenges' | 'announcements' | 'scoreboard' | 'team'>('challenges')

const challengesTabRef = ref<InstanceType<typeof ChallengesTab> | null>(null)
const announcementsTabRef = ref<InstanceType<typeof AnnouncementsTab> | null>(null)
const scoreboardTabRef = ref<InstanceType<typeof ScoreboardTab> | null>(null)
const teamTabRef = ref<InstanceType<typeof TeamTab> | null>(null)

// Competition State (由全局 competitionStore 集中管理并消除重复请求)
const competition = computed(() => competitionStore.currentCompetition)

function handleTabChange(tab: 'challenges' | 'announcements' | 'scoreboard' | 'team') {
  activeTab.value = tab
  if (tab === 'scoreboard') {
    scoreboardTabRef.value?.fetchScoreboard()
  } else if (tab === 'team') {
    teamTabRef.value?.fetchMyTeam(true)
  } else if (tab === 'announcements') {
    announcementsTabRef.value?.fetchAnnouncements()
  }
}

async function init() {
  if (!slug.value) return

  try {
    const comp = await competitionStore.fetchCompetitionBySlug(slug.value)
    if (comp) {
      competitionStore.fetchMyTeam(comp.id)
    }
  } catch {
    message.error(t('competition.not_found_by_slug'))
  }
}

watch(slug, (newSlug) => {
  if (newSlug) {
    init()
  }
})

onMounted(() => {
  init()
})
</script>

<style scoped lang="scss">
.competition-arena-view {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 0 32px; // 两侧更宽的边距
  max-width: 1400px;
  margin: 0 auto;
  box-sizing: border-box;

  @media (max-width: 768px) {
    padding: 0 12px;
  }
}

// 选项卡包裹卡片：宽度不铺满，自适应为 tabs 的内容宽度加边距
.arena-nav-bar {
  width: fit-content;
  max-width: 100%;
  display: inline-flex;
  align-items: center;
  background: var(--v-bg-card, #ffffff);
  border-radius: 12px;
  padding: 4px 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  border: 1px solid var(--v-border-subtle, rgba(0, 0, 0, 0.06));
  box-sizing: border-box;

  .tab-label {
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 600;
  }
}

.arena-content-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
</style>
