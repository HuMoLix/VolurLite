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
          竞赛列表
        </h2>
      </header>

      <main class="flex-1 flex flex-col gap-6">
        <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div class="flex items-center gap-2 flex-wrap">
            <button
              v-for="tab in visibleTabs"
              :key="tab.value"
              type="button"
              class="competition-filter-btn"
              :class="{
                'is-selected': selectedType === tab.value
              }"
              @click="handleTypeChange(tab.value)"
            >
              <span>{{ tab.label }}</span>
            </button>

            <n-popover
              v-if="showMoreMenu"
              v-model:show="isMoreMenuShow"
              trigger="click"
              placement="bottom-start"
              :show-arrow="false"
              raw
            >
              <template #trigger>
                <button
                  type="button"
                  class="competition-filter-btn menu-trigger-btn"
                  :class="{
                    'is-selected': isDropdownTabSelected
                  }"
                  :title="isDropdownTabSelected ? selectedDropdownTab?.label : t('common.more', '更多')"
                >
                  <n-icon :size="16" class="flex-shrink-0">
                    <MenuOutline />
                  </n-icon>
                  <span v-if="isDropdownTabSelected" class="ml-1.5">{{ selectedDropdownTab?.label }}</span>
                </button>
              </template>

              <VCard
                :theme="currentTheme"
                :width="190"
                padding="6px"
                :gap="2"
              >
                <div class="competition-menu-list">
                  <button
                    v-for="tab in dropdownTabs"
                    :key="tab.value"
                    type="button"
                    class="competition-menu-item"
                    :class="{
                      'is-selected': selectedType === tab.value
                    }"
                    @click="handleDropdownSelect(tab.value)"
                  >
                    <span class="competition-menu-label">{{ tab.label }}</span>
                    <n-icon
                      v-if="selectedType === tab.value"
                      :size="16"
                      class="competition-menu-check"
                    >
                      <CheckmarkOutline />
                    </n-icon>
                  </button>
                </div>
              </VCard>
            </n-popover>
          </div>

          <div class="flex items-center gap-3 w-full md:w-auto">
            <n-input
              v-model:value="searchKeyword"
              placeholder="搜索竞赛名称或标识..."
              clearable
              class="competition-search-input w-full md:w-64"
              @keyup.enter="handleSearch"
              @clear="handleSearch"
            >
              <template #prefix>
                <n-icon :component="SearchOutline" />
              </template>
            </n-input>
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
              <span>正在加载更多竞赛...</span>
            </div>
          </div>
        </div>

        <div class="competition-prompt">
          <div class="competition-prompt-headline">
            这里找不到您的竞赛？
          </div>
          <p class="competition-prompt-desc">
            此列表仅显示公开到 Volur Studio 的竞赛。如果没有找到您所参与的，您可以导入竞赛的邀请码来将您参与的私人竞赛添加列表中。
          </p>
          <n-button class="competition-btn competition-import-btn" @click="handleImportClick">
            导入竞赛
          </n-button>
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
import { NIcon, NInput, NButton, NSpin, NPopover } from 'naive-ui'
import { SearchOutline, MenuOutline, CheckmarkOutline } from '@vicons/ionicons5'
import { getCompetitionSliceApi } from '@/api/competition'
import type { CompetitionSummaryVO, CompetitionType } from '@/types/competition'
import { VCompetitionCard } from '@/components/competition'
import { VCard } from '@/components/sidebar'
import { useAppStore } from '@/store/modules/app'
import AppFooter from '@/components/common/Footer.vue'

defineOptions({
  name: 'CompetitionListView'
})

const { t } = useI18n()
const router = useRouter()
const appStore = useAppStore()

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

const isMoreMenuShow = ref(false)
const windowWidth = ref(typeof window !== 'undefined' ? window.innerWidth : 1200)

const currentTheme = computed<'light' | 'dark'>(() => {
  return appStore.theme === 'dark' ? 'dark' : 'light'
})

const availableTypeTabs = computed(() => [
  { value: 'ALL', label: t('competition.type_all') },
  { value: 'CTF', label: t('competition.type_ctf') },
  { value: 'THEORY', label: t('competition.type_theory') },
  { value: 'AWD', label: t('competition.type_awd') },
  { value: 'AWD_PLUS', label: t('competition.type_awd_plus') },
  { value: 'ISW', label: t('competition.type_isw') }
])

const visibleTabs = computed(() => {
  if (windowWidth.value < 460) {
    return availableTypeTabs.value.slice(0, 2)
  }
  if (windowWidth.value < 860) {
    return availableTypeTabs.value.slice(0, 3)
  }
  return availableTypeTabs.value
})

const dropdownTabs = computed(() => {
  if (windowWidth.value < 460) {
    return availableTypeTabs.value.slice(2)
  }
  if (windowWidth.value < 860) {
    return availableTypeTabs.value.slice(3)
  }
  return []
})

const showMoreMenu = computed(() => dropdownTabs.value.length > 0)

const selectedDropdownTab = computed(() => {
  return dropdownTabs.value.find((tab) => tab.value === selectedType.value)
})

const isDropdownTabSelected = computed(() => {
  return Boolean(selectedDropdownTab.value)
})

function handleDropdownSelect(val: string) {
  isMoreMenuShow.value = false
  handleTypeChange(val)
}

function handleResize() {
  windowWidth.value = window.innerWidth
}

function handleTypeChange(val: string) {
  selectedType.value = val
  currentPage.value = 1
  hasNext.value = true
  fetchSlice(1, true)
}

function handleSearch() {
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
    window.$message.info('私人竞赛导入功能正在联调中')
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
  window.addEventListener('resize', handleResize)
  fetchSlice(1, true).then(() => {
    nextTick(() => {
      setupObserver()
    })
  })
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
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
  font-weight: 700;
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

.competition-btn {
  display: inline-flex !important;
  align-items: center !important;
  justify-content: center !important;
  box-sizing: border-box !important;
  gap: 6px !important;
  height: 36px !important;
  padding: 0 12px !important;
  border-radius: 12px !important;
  border: 1px solid #d7d8da !important;
  color: #2b2d31 !important;
  font-family: Inter, sans-serif !important;
  font-size: 14px !important;
  font-weight: 500 !important;
  line-height: 20px !important;
  letter-spacing: normal !important;
  cursor: pointer !important;
  user-select: none !important;
  white-space: nowrap !important;
  background-color: transparent !important;
  outline: none !important;
  box-shadow: none !important;
  transition: background-color 0.15s ease-in-out, border-color 0.15s ease-in-out, color 0.15s ease-in-out, box-shadow 0.1s ease-in-out !important;

  &:focus,
  &:active,
  &:focus-visible {
    outline: none !important;
    box-shadow: none !important;
  }

  :deep(.n-button__content) {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
    font-family: Inter, sans-serif;
    font-size: 14px;
    font-weight: 500;
    line-height: 20px;
    letter-spacing: normal;
    color: inherit;
  }

  :deep(.n-button__border) {
    border-color: #d7d8da !important;
    border-radius: 12px !important;
    box-shadow: none !important;
  }

  :deep(.n-button__state-border) {
    display: none !important;
  }

  :deep(.n-base-wave) {
    display: none !important;
    animation: none !important;
  }

  &:hover {
    background-color: rgba(60, 64, 67, 0.04) !important;
  }

  &:focus,
  &:active {
    border-color: #d7d8da !important;
    :deep(.n-button__border) {
      border-color: #d7d8da !important;
      box-shadow: none !important;
    }
  }

  &.n-button {
    border: none !important;
  }

  html.dark & {
    border-color: #333 !important;
    color: #fcfcfc !important;

    &.n-button {
      border: none !important;
    }

    :deep(.n-button__border) {
      border-color: #333 !important;
    }

    &:hover {
      background-color: rgba(255, 255, 255, 0.04) !important;
    }

    &:focus,
    &:active {
      border-color: #333 !important;
      :deep(.n-button__border) {
        border-color: #333 !important;
        box-shadow: none !important;
      }
    }
  }
}

.competition-import-btn {
  margin: 0 auto !important;
  border: none !important;
}

.competition-search-input {
  --n-height: 36px !important;
  height: 36px !important;

  :deep(.n-input-wrapper) {
    height: 36px !important;
    border-radius: 12px !important;
  }

  :deep(.n-input__border) {
    border-radius: 12px !important;
  }

  :deep(.n-input__state-border) {
    border-radius: 12px !important;
  }
}

.competition-filter-btn {
  display: inline-flex !important;
  align-items: center !important;
  justify-content: center !important;
  box-sizing: border-box !important;
  padding: 6px 12px !important;
  border-radius: 12px !important;
  border: 1px solid #d7d8da80 !important;
  box-shadow: 0 1px 2px 0 #0000000d !important;
  color: #2b2d31 !important;
  font-family: Inter, sans-serif !important;
  font-size: 14px !important;
  font-weight: 500 !important;
  line-height: 20px !important;
  letter-spacing: normal !important;
  cursor: pointer !important;
  user-select: none !important;
  white-space: nowrap !important;
  background-color: transparent !important;
  outline: none !important;
  transition: background-color 0.15s ease-in-out, border-color 0.15s ease-in-out, color 0.15s ease-in-out, box-shadow 0.1s ease-in-out, padding-left 0.15s ease-in-out !important;

  &:focus,
  &:active,
  &:focus-visible {
    outline: none !important;
  }

  &:hover {
    background-color: #eaeaeb !important;
  }

  &.is-selected,
  &.selected {
    padding-left: 26px !important;
    background-color: #f4f4f5 !important;
    border-color: #d7d8da !important;
    background-size: 100% !important;
    background-image: radial-gradient(circle at 16px center, var(--color-v3-text, currentColor) 3px, transparent 3.4px) !important;
    background-repeat: no-repeat !important;
  }

  html.dark & {
    border-color: #262626 !important;
    color: #fcfcfc !important;

    &:hover {
      background-color: #2a2b2d !important;
    }

    &.is-selected,
    &.selected {
      background-color: #262626 !important;
      border-color: #333 !important;
    }
  }

  &.menu-trigger-btn {
    &.is-selected,
    &.selected {
      padding-left: 12px !important;
      background-image: none !important;
      background-color: #f4f4f5 !important;
      border-color: #d7d8da !important;
    }

    html.dark & {
      &.is-selected,
      &.selected {
        background-color: #262626 !important;
        border-color: #333 !important;
      }
    }
  }
}

.competition-menu-list {
  display: flex;
  flex-direction: column;
  width: 100%;
  gap: 2px;
}

.competition-menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  height: 36px;
  padding: 0 10px;
  border-radius: 8px;
  border: none;
  background-color: transparent;
  cursor: pointer;
  color: #3c4043;
  font-family: inherit;
  font-size: 13px;
  font-weight: 500;
  text-align: left;
  outline: none;
  box-sizing: border-box;
  transition: background-color 0.15s cubic-bezier(0.2, 0, 0, 1),
              color 0.15s cubic-bezier(0.2, 0, 0, 1);

  &:hover {
    background-color: rgba(60, 64, 67, 0.08);
    color: #202124;
  }

  &:active {
    background-color: rgba(60, 64, 67, 0.12);
  }

  &.is-selected {
    color: #1a73e8;
    font-weight: 600;

    &:hover {
      background-color: rgba(26, 115, 232, 0.08);
    }
  }

  .competition-menu-label {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .competition-menu-check {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 16px;
    height: 16px;
    color: #1a73e8;
    flex-shrink: 0;
  }

  html.dark & {
    color: #bdc1c6;

    &:hover {
      background-color: rgba(255, 255, 255, 0.08);
      color: #e8eaed;
    }

    &:active {
      background-color: rgba(255, 255, 255, 0.12);
    }

    &.is-selected {
      color: #a8c7fa;

      &:hover {
        background-color: rgba(168, 199, 250, 0.12);
      }

      .competition-menu-check {
        color: #a8c7fa;
      }
    }
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
