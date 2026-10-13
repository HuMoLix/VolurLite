<template>
  <div
    class="ctf-challenge-card relative rounded-2xl bg-white dark:bg-google-surface-darkCard cursor-pointer flex flex-col justify-between overflow-hidden select-none min-h-[160px] max-h-[180px]"
    @click="emit('click', challenge)"
  >
    <div class="p-5 flex flex-col flex-1 justify-between min-h-0">
      <div class="grid grid-cols-[1fr_auto_1fr] items-center gap-2 w-full shrink-0">
        <div class="flex items-center gap-1.5 text-xs font-semibold justify-self-start min-w-0" :class="getCategoryColorClass(challenge.category || challenge.categoryName)">
          <n-icon :size="16" :component="getCategoryIcon(challenge.category || challenge.categoryName)" class="shrink-0" />
          <span class="truncate">{{ challenge.category || challenge.categoryName || t('competition.category_misc', '杂项') }}</span>
        </div>

        <div class="justify-self-center font-mono text-center whitespace-nowrap">
          <span class="font-bold text-google-blue dark:text-google-blue-dark text-base sm:text-lg leading-tight">
            {{ challenge.currentPoints }}
          </span>
          <span class="text-[11px] font-normal text-google-text-muted dark:text-google-text-darkMuted font-sans ml-0.5">
            pts
          </span>
        </div>

        <div class="justify-self-end flex items-center justify-end min-w-[20px]">
          <VTooltip
            v-if="challenge.isContainer"
            :content="t('competition.container_challenge', '动态靶机环境 (Docker)')"
          >
            <span class="inline-flex items-center text-[#2496ed] hover:text-[#1d82d2] transition-colors">
              <n-icon :size="18" :component="LogoDocker" />
            </span>
          </VTooltip>
        </div>
      </div>

      <div class="my-auto py-2 text-center min-h-0 overflow-hidden flex items-center justify-center">
        <h3 class="text-lg font-bold text-center text-google-text-primary dark:text-google-text-darkPrimary line-clamp-2 leading-snug">
          {{ challenge.title || challenge.name }}
        </h3>
      </div>

      <div class="challenge-card-footer pt-2.5 flex items-center shrink-0">
        <div
          v-if="topTeamsList.length > 0"
          class="flex items-center gap-2.5 w-full min-w-0 flex-nowrap"
        >
          <div
            v-for="(team, index) in topTeamsList"
            :key="index"
            class="min-w-0 flex-1"
          >
            <div class="inline-flex items-center gap-1 min-w-0 max-w-full cursor-pointer text-[14px] leading-[14px]">
              <n-icon :size="14" :component="Medal" :class="getMedalIconColor(index)" class="shrink-0" />
              <VTooltip :content="team" placement="top">
                <span class="truncate block min-w-0 text-google-text-desc dark:text-google-text-darkDesc text-[14px] leading-[14px]">
                  {{ formatTeamName(team) }}
                </span>
              </VTooltip>
            </div>
          </div>
        </div>
        <div
          v-else
          class="text-xs text-google-text-muted dark:text-google-text-darkMuted flex items-center min-w-0"
        >
          <span class="truncate">{{ t('competition.no_teams_solved', '暂无队伍解出') }}</span>
        </div>
      </div>
    </div>

    <div
      class="h-1 w-full shrink-0 transition-colors duration-200"
      :class="[challenge.isSolvedByMyTeam ? 'bg-google-green dark:bg-google-green-dark' : 'bg-[#1765CC]']"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Component } from 'vue'
import { useI18n } from 'vue-i18n'
import { NIcon } from 'naive-ui'
import {
  GlobeOutline,
  TerminalOutline,
  GitBranchOutline,
  KeyOutline,
  ExtensionPuzzleOutline,
  FolderOutline,
  LogoDocker
} from '@vicons/ionicons5'
import { Medal } from '@vicons/fa'
import { VTooltip } from '@/components/common'
import type { CtfCompetitionChallengeVO } from '@/types/competition'

defineOptions({
  name: 'CtfCompetitionChallengeCard'
})

const props = defineProps<{
  challenge: CtfCompetitionChallengeVO
}>()

const emit = defineEmits<{
  (e: 'click', challenge: CtfCompetitionChallengeVO): void
}>()

const { t } = useI18n()

const topTeamsList = computed<string[]>(() => {
  const raw = props.challenge.topTeams || []
  if (Array.isArray(raw)) {
    return raw
      .slice(0, 3)
      .map((item) => (typeof item === 'string' ? item : String((item as { teamName?: string })?.teamName || item)))
  }
  return []
})

function formatTeamName(name: string): string {
  if (!name) return ''
  if (name.length > 6) {
    return `${name.slice(0, 5)}..`
  }
  return name
}

function getCategoryIcon(cat?: string | null): Component {
  const normalized = (cat || '').toLowerCase()
  switch (normalized) {
    case 'web':
      return GlobeOutline
    case 'pwn':
      return TerminalOutline
    case 'reverse':
      return GitBranchOutline
    case 'crypto':
      return KeyOutline
    case 'misc':
      return ExtensionPuzzleOutline
    default:
      return FolderOutline
  }
}

function getCategoryColorClass(cat?: string | null): string {
  const normalized = (cat || '').toLowerCase()
  switch (normalized) {
    case 'web':
      return 'text-google-blue dark:text-google-blue-dark'
    case 'pwn':
      return 'text-google-red dark:text-google-red-dark'
    case 'reverse':
      return 'text-purple-600 dark:text-purple-400'
    case 'crypto':
      return 'text-google-yellow dark:text-google-yellow-dark'
    case 'misc':
      return 'text-google-green dark:text-google-green-dark'
    default:
      return 'text-google-text-desc dark:text-google-text-darkDesc'
  }
}

function getMedalIconColor(index: number): string {
  if (index === 0) {
    return 'text-amber-500 dark:text-amber-400'
  }
  if (index === 1) {
    return 'text-slate-400 dark:text-slate-300'
  }
  return 'text-orange-500 dark:text-orange-400'
}
</script>

<style scoped lang="scss">
.ctf-challenge-card {
  min-height: 160px;
  max-height: 180px;
  border: 1px solid #d7d8da80;
  transition: border-color 0.2s cubic-bezier(0.2, 0, 0, 1), box-shadow 0.2s cubic-bezier(0.2, 0, 0, 1);

  &:hover {
    border-color: #d7d8da;
  }
}

html.dark .ctf-challenge-card {
  border-color: rgba(60, 64, 67, 0.6);

  &:hover {
    border-color: #3c4043;
  }
}

.challenge-card-footer {
  border-top: 1px solid #d7d8da80;
  height: 34px;
  box-sizing: border-box;
}

html.dark .challenge-card-footer {
  border-top-color: rgba(60, 64, 67, 0.6);
}
</style>
