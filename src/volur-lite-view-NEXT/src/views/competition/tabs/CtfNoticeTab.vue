<template>
  <div class="space-y-4">
    <div v-if="announcements.length === 0" class="py-20 text-center space-y-3 rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark">
      <n-icon :size="48" :component="MegaphoneOutline" class="text-google-text-muted dark:text-google-text-darkMuted mx-auto" />
      <div class="text-sm text-google-text-desc dark:text-google-text-darkDesc">
        {{ t('competition.no_announcements', '暂无官方公告发布') }}
      </div>
    </div>

    <div
      v-for="ann in announcements"
      :key="ann.id"
      class="rounded-2xl p-5 sm:p-6 bg-white dark:bg-google-surface-darkCard border shadow-sm space-y-3 transition-colors"
      :class="ann.isPinned ? 'border-amber-300 dark:border-amber-800/80 bg-amber-50/10' : 'border-google-border-light dark:border-google-border-dark'"
    >
      <div class="flex items-center justify-between flex-wrap gap-2">
        <div class="flex items-center gap-2">
          <span
            v-if="ann.isPinned"
            class="px-2 py-0.5 rounded text-[11px] font-semibold bg-amber-100 text-google-yellow dark:bg-amber-950 dark:text-google-yellow-dark"
          >
            {{ t('competition.pinned', '置顶') }}
          </span>
          <span
            v-if="ann.challengeName"
            class="px-2 py-0.5 rounded text-[11px] font-medium bg-blue-50 text-google-blue dark:bg-blue-950 dark:text-google-blue-dark"
          >
            {{ ann.challengeName }}
          </span>
          <h3 class="text-base font-bold text-google-text-primary dark:text-google-text-darkPrimary">
            {{ ann.title }}
          </h3>
        </div>

        <div class="text-xs text-google-text-muted dark:text-google-text-darkMuted font-mono">
          {{ ann.createdAt }}
        </div>
      </div>

      <p class="text-xs sm:text-sm text-google-text-desc dark:text-google-text-darkDesc leading-relaxed whitespace-pre-wrap">
        {{ ann.content }}
      </p>

      <div v-if="ann.createdByName" class="pt-2 text-[11px] text-google-text-muted dark:text-google-text-darkMuted">
        {{ t('competition.publisher', '发布人') }}: {{ ann.createdByName }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { NIcon } from 'naive-ui'
import { MegaphoneOutline } from '@vicons/ionicons5'
import type { CompetitionAnnouncementVO } from '@/types/competition'

defineOptions({
  name: 'CtfCompetitionAnnouncementListView'
})

defineProps<{
  announcements: CompetitionAnnouncementVO[]
}>()

const { t } = useI18n()
</script>
