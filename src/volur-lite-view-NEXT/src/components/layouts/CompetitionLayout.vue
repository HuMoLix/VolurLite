<template>
  <div
    class="competition-layout h-screen max-h-screen flex flex-col overflow-hidden bg-google-surface-light dark:bg-google-surface-dark transition-colors duration-300"
    :class="layoutClass"
  >
    <VCompetitionHeader
      :competition="competition"
      :mode="mode"
      :title="title"
      :default-title="defaultTitle"
      :start-time="startTime"
      :end-time="endTime"
      :exit-text="exitText"
      :exit-route="exitRoute"
      :home-route="homeRoute"
      :user-role-fallback="userRoleFallback"
      :team-name="teamName"
      :candidate-name="candidateName"
      :show-candidate-pill="showCandidatePill"
      :warning-threshold-minutes="warningThresholdMinutes"
      :countdown-labels="countdownLabels"
      @exit="emit('exit', $event)"
      @time-up="emit('time-up')"
    >
      <template #header-left>
        <slot name="header-left" />
      </template>
      <template #header-center>
        <slot name="header-center" />
      </template>
      <template #header-right>
        <slot name="header-right" />
      </template>
      <template #header-team>
        <slot name="header-team" />
      </template>
    </VCompetitionHeader>

    <div
      ref="competitionMainRef"
      class="competition-main flex-1 flex flex-col min-w-0 w-full h-full overflow-y-auto"
    >
      <main class="flex-1 flex flex-col min-w-0 w-full" :class="mainClass">
        <slot>
          <router-view />
        </slot>
      </main>

      <slot name="footer">
        <AppFooter v-if="showFooter" />
      </slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppFooter from '@/components/common/Footer.vue'
import VCompetitionHeader from '@/components/competition/VCompetitionHeader.vue'
import type { CompetitionDetailVO, CompetitionSummaryVO } from '@/types/competition'
import type { CountdownLabelsConfig } from '@/components/competition/VCompetitionHeader.vue'

defineOptions({
  name: 'CompetitionLayout'
})

export type { CountdownLabelsConfig }

export interface CompetitionLayoutProps {
  competition?: CompetitionDetailVO | CompetitionSummaryVO | null
  mode?: 'ctf' | 'theory' | 'custom'
  title?: string
  defaultTitle?: string
  startTime?: string | null
  endTime?: string | null
  exitText?: string
  exitRoute?: string
  homeRoute?: string
  userRoleFallback?: string
  teamName?: string | null
  candidateName?: string | null
  showCandidatePill?: boolean
  warningThresholdMinutes?: number
  countdownLabels?: CountdownLabelsConfig
  layoutClass?: string
  mainClass?: string
  showFooter?: boolean
}

withDefaults(defineProps<CompetitionLayoutProps>(), {
  competition: null,
  mode: 'ctf',
  title: undefined,
  defaultTitle: undefined,
  startTime: undefined,
  endTime: undefined,
  exitText: undefined,
  exitRoute: undefined,
  homeRoute: undefined,
  userRoleFallback: 'Player',
  teamName: undefined,
  candidateName: undefined,
  showCandidatePill: true,
  warningThresholdMinutes: 15,
  countdownLabels: undefined,
  layoutClass: '',
  mainClass: '',
  showFooter: true
})

const emit = defineEmits<{
  exit: [ev: MouseEvent]
  'time-up': []
}>()

const route = useRoute()
const competitionMainRef = ref<HTMLElement | null>(null)

watch(
  () => route?.path,
  () => {
    competitionMainRef.value?.scrollTo({ top: 0, behavior: 'auto' })
  }
)

function scrollToTop(options?: ScrollToOptions) {
  competitionMainRef.value?.scrollTo(options ?? { top: 0, behavior: 'smooth' })
}

defineExpose({
  competitionMainRef,
  scrollToTop
})
</script>

<style scoped lang="scss">
.competition-layout {
  position: relative;
  width: 100%;
  height: 100vh;
  height: 100dvh;
  max-height: 100vh;
  max-height: 100dvh;
  overflow: hidden;
}

.competition-main {
  position: relative;
  height: 100%;
  max-height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
  overscroll-behavior-y: contain;
  -webkit-overflow-scrolling: touch;
  scrollbar-gutter: stable;
  scrollbar-width: thin;
  scrollbar-color: var(--v-scrollbar-thumb) transparent;

  &::-webkit-scrollbar {
    width: 8px;
    height: 8px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background-color: var(--v-scrollbar-thumb);
    border-radius: 9999px;
    border: 2px solid transparent;
    background-clip: padding-box;

    &:hover {
      background-color: var(--v-scrollbar-thumb-hover);
    }
  }

  &::-webkit-scrollbar-corner {
    background: transparent;
  }
}
</style>
