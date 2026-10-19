<template>
  <div class="competition-list-view flex-1 flex flex-col min-w-0" style="padding: 48px 24px 24px 24px;">
    <div
      v-if="loading"
      class="fixed top-0 left-0 right-0 h-1 z-50 overflow-hidden bg-blue-100 dark:bg-blue-950"
    >
      <div class="progress-bar-primary"></div>
      <div class="progress-bar-secondary"></div>
    </div>

    <div class="flex-1 flex flex-col w-full max-w-7xl mx-auto">
      <header class="competition-header">
        <h2 class="competition-title">
          {{ t('competition.competition_list') }}
        </h2>
      </header>

      <main class="flex-1 flex flex-col gap-6">
        <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <VFilterBtnGroup
            v-model="selectedType"
            :options="availableTypeTabs"
            @change="handleTypeChange"
          />

          <div class="flex items-center gap-3 w-full md:w-auto">
            <VSearch
              v-model:value="searchKeyword"
              placeholder="搜索竞赛名称"
              clearable
              class="competition-search-input w-full md:w-64"
              @search="handleSearch"
              @clear="handleSearch"
            />
          </div>
        </div>

        <div v-if="competitionList.length > 0" class="space-y-4">
          <VCompetitionCard
            v-for="comp in competitionList"
            :key="comp.id"
            :competition="comp"
            @click="goToDetail(comp)"
          />

          <div ref="sentinelRef" class="h-4 w-full"></div>

          <div v-if="loadingMore" class="flex items-center justify-center py-4">
            <div class="flex items-center gap-2 text-sm text-google-text-muted dark:text-google-text-darkMuted">
              <n-spin size="small" />
              <span>{{ t('competition.loading_more') }}</span>
            </div>
          </div>
        </div>

        <div class="competition-prompt">
          <div class="competition-prompt-headline">
            {{ t('competition.prompt_headline') }}
          </div>
          <p class="competition-prompt-desc">
            {{ t('competition.prompt_desc') }}
          </p>
          <VBtn
            clz="normal"
            class="competition-import-btn"
            @click="handleImportClick"
          >
            {{ t('competition.import_competition') }}
          </VBtn>
        </div>
      </main>

      <footer class="mt-auto pt-8">
        <AppFooter />
      </footer>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { NSpin } from 'naive-ui'
import { getCompetitionSliceApi } from '@/api/competition'
import type { CompetitionSummaryVO, CompetitionType } from '@/types/competition'
import { VCompetitionCard } from '@/components/competition'
import { VFilterBtnGroup, VSearch, VBtn } from '@/components/common'
import AppFooter from '@/components/common/Footer.vue'

defineOptions({
  name: 'CompetitionListView'
})

const { t } = useI18n()
const router = useRouter()

const PAGE_SIZE = 5

const loading = ref(false)
const loadingMore = ref(false)
const searchKeyword = ref('')
const selectedType = ref('ALL')
const competitionList = ref<CompetitionSummaryVO[]>([])
const currentPage = ref(1)
const hasNext = ref(true)
const sentinelRef = ref<HTMLElement | null>(null)
let observer: IntersectionObserver | null = null

const availableTypeTabs = computed(() => [
  { value: 'ALL', label: t('competition.type_all') },
  { value: 'CTF', label: t('competition.type_ctf') },
  { value: 'THEORY', label: t('competition.type_theory') },
  { value: 'AWD', label: t('competition.type_awd') },
  { value: 'AWD_PLUS', label: t('competition.type_awd_plus') },
  { value: 'ISW', label: t('competition.type_isw') }
])

function handleTypeChange(val: string | number) {
  selectedType.value = String(val)
  currentPage.value = 1
  hasNext.value = true
  fetchSlice(1, true)
}

function handleSearch(val?: string) {
  if (typeof val === 'string') {
    searchKeyword.value = val
  }
  currentPage.value = 1
  hasNext.value = true
  fetchSlice(1, true)
}

function goToDetail(comp: CompetitionSummaryVO) {
  if (comp.slug) {
    router.push(`/c/${comp.slug}`)
  } else if (comp.id) {
    router.push(`/competitions/${comp.id}`)
  }
}

function handleImportClick() {
  if (window.$message) {
    window.$message.info(t('competition.import_wip_tip'))
  }
}

async function fetchSlice(page: number, isInitial = false) {
  if (isInitial) {
    loading.value = true
  } else {
    loadingMore.value = true
  }

  try {
    const res = await getCompetitionSliceApi({
      page,
      size: PAGE_SIZE,
      keyword: searchKeyword.value.trim() || undefined,
      type: selectedType.value === 'ALL' ? undefined : (selectedType.value as CompetitionType)
    })

    if (res && res.data) {
      const slice = res.data
      if (isInitial) {
        competitionList.value = slice.list || []
      } else {
        const existingIds = new Set(competitionList.value.map((item) => String(item.id)))
        const newItems = (slice.list || []).filter((item) => !existingIds.has(String(item.id)))
        competitionList.value.push(...newItems)
      }
      currentPage.value = slice.currentPage
      hasNext.value = slice.hasNext
    } else {
      if (isInitial) {
        competitionList.value = []
      }
      hasNext.value = false
    }
  } catch {
    if (isInitial) {
      competitionList.value = []
    }
    hasNext.value = false
  } finally {
    if (isInitial) {
      loading.value = false
    } else {
      loadingMore.value = false
    }
  }
}

function loadMore() {
  if (loading.value || loadingMore.value || !hasNext.value) return
  fetchSlice(currentPage.value + 1, false)
}

function setupObserver() {
  if (observer) {
    observer.disconnect()
    observer = null
  }
  if (!sentinelRef.value) return

  observer = new IntersectionObserver(
    (entries) => {
      const target = entries[0]
      if (target && target.isIntersecting) {
        loadMore()
      }
    },
    {
      root: null,
      rootMargin: '100px',
      threshold: 0.1
    }
  )

  observer.observe(sentinelRef.value)
}

watch(sentinelRef, (el) => {
  if (el) {
    setupObserver()
  }
})

onMounted(() => {
  fetchSlice(1, true).then(() => {
    nextTick(() => {
      setupObserver()
    })
  })
})

onUnmounted(() => {
  if (observer) {
    observer.disconnect()
    observer = null
  }
})
</script>

<style scoped lang="scss">
.competition-list-view {
  display: flex;
  flex-direction: column;
  padding: 48px 24px 24px 24px;
  box-sizing: border-box;
}

.competition-header {
  padding-bottom: 0;
}

.competition-title {
  margin-top: 0;
  margin-bottom: 32px;
  color: #1b1b1e;
  font-size: 24px;
  font-weight: 600;
  line-height: 1.25;

  html.dark & {
    color: #e8eaed;
  }
}

.competition-prompt {
  margin: 0 auto;
  max-width: 400px;
  padding: 32px 0;
  text-align: center;
}

.competition-prompt-headline {
  color: #1b1b1e;
  font-size: 14px;
  font-weight: 500;
  margin: 36px 0 8px;

  html.dark & {
    color: #e8eaed;
  }
}

.competition-prompt-desc {
  font-family: Inter, sans-serif;
  font-optical-sizing: auto;
  font-size: 12px;
  font-weight: 400;
  line-height: 16px;
  color: var(--color-v3-text-var, #5f6368);
  margin-bottom: 28px;

  html.dark & {
    color: var(--color-v3-text-var, #bdc1c6);
  }
}

.competition-import-btn {
  margin: 0 auto;
}

.competition-search-input {
  --n-height: 36px !important;
  height: 36px !important;

  :deep(.n-input-wrapper) {
    height: 36px !important;
  }
}

.progress-bar-primary {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 100%;
  transform-origin: top left;
  background-color: #1a73e8;
  animation: mdc-linear-progress-primary-indeterminate-translate 2s linear infinite;
}

.progress-bar-secondary {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 100%;
  transform-origin: top left;
  background-color: #1a73e8;
  animation: mdc-linear-progress-secondary-indeterminate-translate 2s linear infinite;
}

html.dark {
  .progress-bar-primary,
  .progress-bar-secondary {
    background-color: #8ab4f8;
  }
}

@keyframes mdc-linear-progress-primary-indeterminate-translate {
  0% {
    transform: translateX(0);
  }
  20% {
    animation-timing-function: cubic-bezier(0.5, 0, 0.701732, 0.495819);
    transform: translateX(0);
  }
  59.15% {
    animation-timing-function: cubic-bezier(0.302435, 0.381352, 0.55, 0.956352);
    transform: translateX(83.67142%);
  }
  to {
    transform: translateX(200.611057%);
  }
}

@keyframes mdc-linear-progress-secondary-indeterminate-translate {
  0% {
    transform: translateX(0);
  }
  25% {
    animation-timing-function: cubic-bezier(0.15, 0, 0.515058, 0.409685);
    transform: translateX(0);
  }
  48.35% {
    animation-timing-function: cubic-bezier(0.4, 0.627035, 0.6, 0.902026);
    transform: translateX(84.386165%);
  }
  to {
    transform: translateX(160.277782%);
  }
}
</style>
