<template>
  <article
    class="group relative rounded-2xl p-5 sm:p-6 bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark shadow-sm hover:shadow-md hover:border-google-blue/40 dark:hover:border-google-blue-dark/40 transition-colors duration-200 cursor-pointer flex flex-col md:flex-row items-stretch gap-6"
    @click="handleClick"
  >
    <div class="w-full md:w-64 lg:w-72 xl:w-80 aspect-[1916/821] rounded-xl overflow-hidden shrink-0 relative bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light/60 dark:border-google-border-dark/60 md:self-center">
      <img
        v-if="competition.bannerUrl"
        :src="formatBannerUrl(competition.bannerUrl)"
        :alt="competition.title"
        class="w-full h-full object-cover"
        loading="lazy"
      />
      <div
        v-else
        class="w-full h-full flex flex-col items-center justify-center p-4 bg-gradient-to-br from-blue-50/50 to-indigo-100/50 dark:from-blue-950/30 dark:to-slate-900/40 text-google-text-muted dark:text-google-text-darkMuted"
      >
        <n-icon :size="40" :component="TrophyOutline" class="text-google-blue/40 dark:text-google-blue-dark/40 mb-1" />
        <span class="text-xs font-mono font-medium tracking-wider text-google-blue/70 dark:text-google-blue-dark/70 uppercase">
          {{ competition.type || 'VOLUR' }}
        </span>
      </div>
    </div>

    <div class="flex-1 min-w-0 flex flex-col justify-between gap-3 md:gap-0 self-stretch">
      <div class="flex items-center flex-wrap gap-2.5">
        <h3 class="m-0 text-lg sm:text-xl font-semibold text-google-text-primary dark:text-[#FCFCFC] line-clamp-1">
          {{ competition.title }}
        </h3>

        <span
          class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium shrink-0"
          :class="getStatusBadgeClass(effectiveStatus)"
        >
          <span
            v-if="effectiveStatus === 'RUNNING'"
            class="w-1.5 h-1.5 rounded-full bg-google-green animate-ping"
          ></span>
          <span>{{ getStatusText(effectiveStatus) }}</span>
        </span>

        <span
          v-if="competition.type"
          class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium border border-google-border-light dark:border-google-border-dark text-google-text-desc dark:text-google-text-darkDesc bg-google-surface-light dark:bg-google-surface-dark shrink-0"
        >
          {{ getTypeLabel(competition.type) }}
        </span>
      </div>

      <div
        v-if="competition.description"
        class="comp-desc text-google-text-desc dark:text-[#D4D4D4]"
        v-html="renderMarkdown(competition.description)"
      />
      <p
        v-else
        class="m-0 text-sm text-google-text-muted dark:text-google-text-darkMuted leading-relaxed"
      >
        -
      </p>

      <div class="flex items-center flex-wrap gap-y-2 gap-x-6 text-xs text-google-text-muted dark:text-google-text-darkMuted pt-1">
        <div class="flex items-center gap-1.5">
          <n-icon :size="15" :component="CalendarOutline" class="text-google-text-muted dark:text-google-text-darkMuted shrink-0" />
          <span>{{ t('competition.start_time') }}:</span>
          <span class="comp-time-val text-google-text-desc dark:text-[#FCFCFC]">
            {{ formatDateTime(competition.startTime) }} - {{ formatDateTime(competition.endTime) }}
          </span>
        </div>

        <div v-if="competition.regStartTime || competition.regEndTime" class="flex items-center gap-1.5">
          <n-icon :size="15" :component="TimeOutline" class="text-google-text-muted dark:text-google-text-darkMuted shrink-0" />
          <span>{{ t('competition.reg_time') }}:</span>
          <span class="font-medium text-google-text-desc dark:text-google-text-darkDesc font-mono">
            {{ formatRegTime(competition) }}
          </span>
        </div>
      </div>
    </div>

    <div class="shrink-0 flex items-center justify-end md:self-center pt-2 md:pt-0">
      <VBtn
        clz="primary"
        :right-icon="ArrowForwardOutline"
        @click.stop="handleClick"
      >
        {{ actionButtonText }}
      </VBtn>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { NIcon } from 'naive-ui'
import { VBtn } from '@/components/common'
import {
  ArrowForwardOutline,
  CalendarOutline,
  TimeOutline,
  TrophyOutline
} from '@vicons/ionicons5'
import type { CompetitionSummaryVO, CompetitionType, CompetitionStatus } from '@/types/competition'
import { formatBannerUrl } from '@/utils/imageUrl'
import { renderMarkdown } from '@/utils/markdown'

defineOptions({
  name: 'VCompetitionCard'
})

interface Props {
  competition: CompetitionSummaryVO
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'click', competition: CompetitionSummaryVO): void
}>()

const { t } = useI18n()

const effectiveStatus = computed<CompetitionStatus>(() => {
  if (!props.competition.status || props.competition.status === 'DRAFT' || props.competition.status === 'ARCHIVED' || props.competition.status === 'HIDDEN') {
    return props.competition.status || 'DRAFT'
  }
  const now = Date.now()
  if (props.competition.endTime && now > new Date(props.competition.endTime).getTime()) {
    return 'ENDED'
  }
  if (props.competition.startTime && now >= new Date(props.competition.startTime).getTime()) {
    return props.competition.status === 'FROZEN' ? 'FROZEN' : 'RUNNING'
  }
  const regStart = props.competition.regStartTime ? new Date(props.competition.regStartTime).getTime() : null
  const regEnd = props.competition.regEndTime ? new Date(props.competition.regEndTime).getTime() : null
  const afterRegStart = regStart === null || now >= regStart
  const beforeRegEnd = regEnd === null || now <= regEnd
  if (afterRegStart && beforeRegEnd) {
    return 'REGISTRATION'
  }
  return 'PUBLISHED'
})

const actionButtonText = computed(() => {
  if (effectiveStatus.value === 'RUNNING') {
    return t('competition.enter_competition')
  }
  if (effectiveStatus.value === 'REGISTRATION') {
    return t('competition.status_registration')
  }
  return t('competition.view_details')
})

function formatDateTime(val?: string | null): string {
  if (!val) return '-'
  try {
    const d = new Date(val)
    if (isNaN(d.getTime())) return val
    const pad = (n: number) => (n < 10 ? '0' + n : String(n))
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}-${pad(d.getMinutes())}-${pad(d.getSeconds())}`
  } catch {
    return val
  }
}

function formatRegTime(comp: CompetitionSummaryVO): string {
  if (comp.regStartTime && comp.regEndTime) {
    return `${formatDateTime(comp.regStartTime)} ~ ${formatDateTime(comp.regEndTime)}`
  }
  if (comp.regStartTime) {
    return `${formatDateTime(comp.regStartTime)} 开启`
  }
  if (comp.regEndTime) {
    return `截止至 ${formatDateTime(comp.regEndTime)}`
  }
  return '-'
}

function getTypeLabel(type?: CompetitionType | null): string {
  if (!type) return '-'
  switch (type) {
    case 'CTF':
      return t('competition.type_ctf')
    case 'THEORY':
      return t('competition.type_theory')
    case 'AWD':
      return t('competition.type_awd')
    case 'AWD_PLUS':
      return t('competition.type_awd_plus')
    case 'ISW':
      return t('competition.type_isw')
    default:
      return type
  }
}

function getStatusText(status?: CompetitionStatus | null): string {
  if (!status) return '-'
  switch (status) {
    case 'DRAFT':
      return t('competition.status_draft')
    case 'PUBLISHED':
      return t('competition.status_published')
    case 'REGISTRATION':
      return t('competition.status_registration')
    case 'RUNNING':
      return t('competition.status_running')
    case 'FROZEN':
      return t('competition.status_frozen')
    case 'ENDED':
      return t('competition.status_ended')
    case 'ARCHIVED':
      return t('competition.status_archived')
    case 'HIDDEN':
      return t('competition.status_hidden')
    default:
      return status
  }
}

function getStatusBadgeClass(status?: CompetitionStatus | null): string {
  switch (status) {
    case 'RUNNING':
      return 'bg-green-50 text-google-green dark:bg-green-950/50 dark:text-google-green-dark border border-google-green/30'
    case 'REGISTRATION':
    case 'PUBLISHED':
      return 'bg-blue-50 text-google-blue dark:bg-blue-950/50 dark:text-google-blue-dark border border-google-blue/30'
    case 'FROZEN':
      return 'bg-yellow-50 text-google-yellow dark:bg-yellow-950/50 dark:text-google-yellow-dark border border-google-yellow/30'
    case 'ENDED':
    case 'ARCHIVED':
    default:
      return 'bg-gray-100 text-google-text-muted dark:bg-neutral-800 dark:text-google-text-darkMuted border border-google-border-light dark:border-google-border-dark'
  }
}

function handleClick() {
  emit('click', props.competition)
}
</script>

<style scoped lang="scss">
.comp-desc {
  font-size: 14px;
  line-height: 1.625;
  margin: 0;
  color: #3c4043;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  word-break: break-word;

  :deep(h1),
  :deep(h2),
  :deep(h3),
  :deep(h4),
  :deep(h5),
  :deep(h6) {
    font-size: inherit;
    font-weight: normal;
    color: inherit;
    margin: 0;
    padding: 0;
    border: none;
    line-height: inherit;
    display: inline;

    * {
      font-weight: normal;
    }

    &::after {
      content: ' ';
    }
  }

  :deep(h1:last-child::after),
  :deep(h2:last-child::after),
  :deep(h3:last-child::after),
  :deep(h4:last-child::after),
  :deep(h5:last-child::after),
  :deep(h6:last-child::after) {
    content: '';
  }

  :deep(p) {
    margin: 0;
    padding: 0;
    display: inline;

    &::after {
      content: ' ';
    }

    &:last-child::after {
      content: '';
    }
  }

  :deep(br + br) {
    display: none;
  }

  :deep(strong),
  :deep(b) {
    font-weight: 600;
    color: #202124;
  }

  :deep(em),
  :deep(i) {
    font-style: italic;
  }

  :deep(code) {
    font-family: var(--font-mono, 'Fira Code', monospace);
    font-size: 12px;
    padding: 1px 5px;
    background: #f1f3f4;
    border-radius: 4px;
    color: #202124;
  }

  :deep(pre) {
    margin: 0;
    padding: 0;
    display: inline;
    background: transparent;
  }

  :deep(a) {
    color: inherit;
    text-decoration: underline;
    text-underline-offset: 2px;
    pointer-events: none;
  }

  :deep(ul),
  :deep(ol) {
    margin: 0;
    padding: 0;
    list-style: none;
    display: inline;
  }

  :deep(li) {
    display: inline;
    margin-right: 6px;

    &::after {
      content: '; ';
    }

    &:last-child::after {
      content: '';
    }
  }

  :deep(blockquote) {
    margin: 0;
    padding: 0;
    border: none;
    display: inline;
  }

  :deep(img),
  :deep(hr) {
    display: none;
  }
}

html.dark .comp-desc {
  color: #D4D4D4;

  :deep(strong),
  :deep(b) {
    color: #e3e3e3;
  }

  :deep(code) {
    background: #303134;
    color: #e3e3e3;
  }
}

.comp-time-val {
  font-family: Inter, sans-serif;
}

html.dark .comp-time-val {
  color: #FCFCFC;
}
</style>

