<template>
  <div class="training-list-view flex-1 flex flex-col min-w-0">
    <VViewlayout>
      <template #header>
        <section class="training-hero">
          <div class="hero-grid" aria-hidden="true" />
          <div class="hero-orb hero-orb-a" aria-hidden="true" />
          <div class="hero-orb hero-orb-b" aria-hidden="true" />

          <div class="hero-inner">
            <div class="min-w-0 flex-1">
              <div class="hero-topline">
                <span class="hero-eyebrow">
                  <n-icon :size="14" :component="BarbellOutline" />
                  {{ t('training.hero_eyebrow', 'PRACTICE LAB') }}
                </span>
                <VBtn
                  clz="normal"
                  size="small"
                  class="hero-refresh"
                  :left-icon="RefreshOutline"
                  :loading="refreshing"
                  @click="refreshAll"
                >
                  {{ t('training.refresh', '刷新') }}
                </VBtn>
              </div>

              <h1 class="hero-title">
                {{ t('training.title', '题目训练') }}
              </h1>
              <p class="hero-desc">
                {{ t('training.subtitle', '从题库中挑选的练习题，随时开练，稳步积累实战经验') }}
              </p>

              <div class="hero-stats">
                <div class="hero-stat">
                  <span class="hero-stat-value">{{ progress?.totalChallenges ?? 0 }}</span>
                  <span class="hero-stat-label">{{ t('training.stat_total', '训练题') }}</span>
                </div>
                <span class="hero-stat-divider" />
                <div class="hero-stat">
                  <span class="hero-stat-value">{{ progress?.solvedCount ?? 0 }}</span>
                  <span class="hero-stat-label">{{ t('training.stat_solved', '已解出') }}</span>
                </div>
                <span class="hero-stat-divider" />
                <div class="hero-stat">
                  <span class="hero-stat-value">{{ progress?.totalPoints ?? 0 }}</span>
                  <span class="hero-stat-label">{{ t('training.stat_points', '总积分') }}</span>
                </div>
                <span class="hero-stat-divider" />
                <div class="hero-stat">
                  <span class="hero-stat-value">#{{ progress?.rank ?? '-' }}</span>
                  <span class="hero-stat-label">{{ t('training.stat_rank', '当前排名') }}</span>
                </div>
              </div>
            </div>

            <div class="hero-ring">
              <svg class="hero-ring-svg" viewBox="0 0 100 100" aria-hidden="true">
                <defs>
                  <linearGradient id="training-ring-gradient" x1="0%" y1="0%" x2="100%" y2="100%">
                    <stop offset="0%" stop-color="#1a73e8" />
                    <stop offset="100%" stop-color="#6aa9ff" />
                  </linearGradient>
                </defs>
                <circle
                  class="hero-ring-track"
                  cx="50"
                  cy="50"
                  :r="RING_RADIUS"
                  fill="none"
                  stroke-width="8"
                />
                <circle
                  class="hero-ring-value"
                  cx="50"
                  cy="50"
                  :r="RING_RADIUS"
                  fill="none"
                  stroke-width="8"
                  stroke="url(#training-ring-gradient)"
                  stroke-linecap="round"
                  :stroke-dasharray="RING_CIRCUMFERENCE"
                  :stroke-dashoffset="ringDashOffset"
                  transform="rotate(-90 50 50)"
                />
              </svg>
              <div class="hero-ring-label">
                <strong>{{ completionPercent }}%</strong>
                <span>{{ t('training.completion_label', '完成度') }}</span>
              </div>
            </div>
          </div>
        </section>
      </template>

      <div class="grid grid-cols-1 xl:grid-cols-[minmax(0,1fr)_320px] gap-6 items-start">
        <main class="min-w-0 space-y-5">
          <div class="filter-bar">
            <VSelection
              v-model:value="selectedCategoryId"
              :options="categoryOptions"
              :placeholder="t('training.filter_category', '全部分类')"
              clearable
              class="filter-category"
              @update:value="handleCategoryChange"
            />

            <VFilterBtnGroup
              v-model="selectedDifficulty"
              :options="difficultyOptions"
              @change="handleDifficultyChange"
            />

            <VSearch
              v-model:value="searchKeyword"
              :placeholder="t('training.search_placeholder', '搜索题目名称')"
              clearable
              class="filter-search"
              @search="handleSearch"
            />
          </div>

          <div v-if="loading" class="training-grid">
            <div v-for="i in 6" :key="i" class="skeleton-card">
              <div class="flex items-center gap-2.5">
                <span class="sk sk-tile" />
                <span class="sk sk-cat" />
                <span class="sk sk-points" />
              </div>
              <span class="sk sk-title" />
              <span class="sk sk-title is-short" />
              <div class="mt-auto flex items-center gap-2 pt-4">
                <span class="sk sk-pill" />
                <span class="sk sk-meta" />
              </div>
            </div>
          </div>

          <div v-else-if="challenges.length === 0" class="empty-panel">
            <div class="empty-illustration">
              <span class="empty-ring is-outer" />
              <span class="empty-ring is-inner" />
              <span class="empty-icon">
                <n-icon :size="32" :component="RocketOutline" />
              </span>
            </div>
            <h3 class="empty-title">
              {{ t('training.no_results_title', '暂无匹配的训练题') }}
            </h3>
            <p class="empty-desc">
              {{ t('training.no_results_desc', '换个难度或关键词试试，也可以去赛事大厅看看') }}
            </p>
            <VBtn clz="normal" size="medium" @click="goToCompetitions">
              {{ t('training.go_competitions', '去赛事大厅看看') }}
            </VBtn>
          </div>

          <template v-else>
            <div class="training-grid">
              <TrainingChallengeCard
                v-for="challenge in challenges"
                :key="String(challenge.id)"
                :challenge="challenge"
                @click="openDetail"
              />
            </div>

            <div v-if="total > PAGE_SIZE" class="flex items-center justify-center pt-2">
              <n-pagination
                :page="currentPage"
                :page-size="PAGE_SIZE"
                :item-count="total"
                @update:page="handlePageChange"
              />
            </div>
          </template>
        </main>

        <aside class="min-w-0 space-y-5 xl:sticky xl:top-6">
          <TrainingLeaderboardCard
            :leaderboard="leaderboard"
            :loading="leaderboardLoading"
            :my-username="currentUsername"
          />
          <TrainingProgressCard
            :progress="progress"
            :loading="progressLoading"
          />
        </aside>
      </div>

      <TrainingChallengeModal
        v-model:show="detailVisible"
        :challenge="activeDetail"
        :loading="detailLoading"
        @solved="handleSolved"
      />
    </VViewlayout>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { NIcon, NPagination, useMessage, type SelectOption } from 'naive-ui'
import {
  BarbellOutline,
  RefreshOutline,
  RocketOutline
} from '@vicons/ionicons5'
import { VBtn, VFilterBtnGroup, VSearch, VSelection } from '@/components/common'
import type { FilterOption, SelectValue } from '@/components/common'
import { VViewlayout } from '@/volur-ui'
import { useUserStore } from '@/store/modules/user'
import {
  getTrainingCategoriesApi,
  getTrainingChallengeDetailApi,
  getTrainingChallengeListApi,
  getTrainingLeaderboardApi,
  getTrainingProgressApi
} from '@/api/training'
import type {
  TrainingCategoryVO,
  TrainingChallengeDetailVO,
  TrainingChallengeVO,
  TrainingLeaderboardVO,
  TrainingProgressVO
} from '@/types/training'
import {
  TrainingChallengeCard,
  TrainingChallengeModal,
  TrainingLeaderboardCard,
  TrainingProgressCard
} from './components'

defineOptions({
  name: 'TrainingListView'
})

const PAGE_SIZE = 12
const RING_RADIUS = 42
const RING_CIRCUMFERENCE = 2 * Math.PI * RING_RADIUS

const { t } = useI18n()
const router = useRouter()
const message = useMessage()
const userStore = useUserStore()

const loading = ref(false)
const refreshing = ref(false)
const challenges = ref<TrainingChallengeVO[]>([])
const total = ref(0)
const currentPage = ref(1)
const searchKeyword = ref('')
const selectedDifficulty = ref('')
const selectedCategoryId = ref<string | null>(null)
const categories = ref<TrainingCategoryVO[]>([])

const progress = ref<TrainingProgressVO | null>(null)
const progressLoading = ref(false)
const leaderboard = ref<TrainingLeaderboardVO | null>(null)
const leaderboardLoading = ref(false)

const detailVisible = ref(false)
const detailLoading = ref(false)
const activeDetail = ref<TrainingChallengeDetailVO | null>(null)

const currentUsername = computed<string>(() => userStore.user?.username || '')

const completionPercent = computed<number>(() => {
  const totalChallenges = progress.value?.totalChallenges ?? 0
  const solved = progress.value?.solvedCount ?? 0
  if (totalChallenges <= 0) return 0
  return Math.min(100, Math.round((solved / totalChallenges) * 100))
})

const ringDashOffset = computed<number>(() => {
  return RING_CIRCUMFERENCE * (1 - completionPercent.value / 100)
})

const categoryOptions = computed<SelectOption[]>(() => [
  { value: '', label: t('training.filter_category', '全部分类') },
  ...categories.value
    .filter((category) => category.id !== null && category.id !== undefined)
    .map((category) => ({
      value: String(category.id),
      label: `${category.name} (${category.total})`
    }))
])

const difficultyOptions = computed<FilterOption<string>[]>(() => [
  { value: '', label: t('training.filter_all', '全部') },
  { value: 'easy', label: t('training.difficulty_easy', '入门') },
  { value: 'normal', label: t('training.difficulty_normal', '简单') },
  { value: 'mid', label: t('training.difficulty_mid', '中等') },
  { value: 'hard', label: t('training.difficulty_hard', '进阶') },
  { value: 'difficult', label: t('training.difficulty_difficult', '困难') }
])

async function fetchList(page = 1) {
  loading.value = true
  try {
    const res = await getTrainingChallengeListApi({
      page,
      size: PAGE_SIZE,
      categoryId: selectedCategoryId.value || undefined,
      difficulty: selectedDifficulty.value || undefined,
      keyword: searchKeyword.value.trim() || undefined
    })
    const data = res?.data
    challenges.value = data?.list || []
    total.value = Number(data?.total ?? 0)
    currentPage.value = data?.currentPage ?? page
  } catch {
    challenges.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function fetchProgress() {
  progressLoading.value = true
  try {
    const res = await getTrainingProgressApi()
    progress.value = res?.data || null
  } catch {
    progress.value = null
  } finally {
    progressLoading.value = false
  }
}

async function fetchLeaderboard() {
  leaderboardLoading.value = true
  try {
    const res = await getTrainingLeaderboardApi(8)
    leaderboard.value = res?.data || null
  } catch {
    leaderboard.value = null
  } finally {
    leaderboardLoading.value = false
  }
}

async function fetchCategories() {
  try {
    const res = await getTrainingCategoriesApi()
    categories.value = res?.data || []
  } catch {
    categories.value = []
  }
}

function handleDifficultyChange(value: string | number) {
  selectedDifficulty.value = String(value)
  fetchList(1)
}

function handleCategoryChange(value: SelectValue) {
  if (value === null || Array.isArray(value) || value === '') {
    selectedCategoryId.value = null
  } else {
    selectedCategoryId.value = String(value)
  }
  fetchList(1)
}

function handleSearch(value?: string) {
  if (typeof value === 'string') {
    searchKeyword.value = value
  }
  fetchList(1)
}

function handlePageChange(page: number) {
  currentPage.value = page
  fetchList(page)
}

async function openDetail(challenge: TrainingChallengeVO) {
  detailVisible.value = true
  detailLoading.value = true
  activeDetail.value = null
  try {
    const res = await getTrainingChallengeDetailApi(challenge.id)
    if (res?.data) {
      activeDetail.value = res.data
    } else {
      detailVisible.value = false
      message.warning(t('training.detail_empty', '未获取到题目详情'))
    }
  } catch {
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

function handleSolved(payload: { challengeId: number | string; points: number }) {
  const found = challenges.value.find((item) => String(item.id) === String(payload.challengeId))
  if (found) {
    found.solvedByMe = true
    if (payload.points > 0) {
      found.solvedCount += 1
    }
  }
  fetchProgress()
  fetchLeaderboard()
}

async function refreshAll() {
  refreshing.value = true
  try {
    await Promise.all([
      fetchList(currentPage.value),
      fetchProgress(),
      fetchLeaderboard(),
      fetchCategories()
    ])
  } finally {
    refreshing.value = false
  }
}

function goToCompetitions() {
  router.push('/competitions')
}

onMounted(() => {
  fetchList(1)
  fetchProgress()
  fetchLeaderboard()
  fetchCategories()
})
</script>

<style scoped lang="scss">
.training-list-view {
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

/* ------------------------------------------------------------------ */
/* Hero                                                                */
/* ------------------------------------------------------------------ */

.training-hero {
  position: relative;
  overflow: hidden;
  margin-bottom: 26px;
  padding: 34px 36px 32px;
  border-radius: 24px;
  border: 1px solid rgba(26, 115, 232, 0.12);
  background:
    radial-gradient(760px 320px at 6% -30%, rgba(26, 115, 232, 0.16), transparent 62%),
    radial-gradient(620px 300px at 100% 120%, rgba(36, 193, 224, 0.12), transparent 60%),
    linear-gradient(135deg, #f7faff 0%, #eef4ff 52%, #f9fbff 100%);
}

html.dark .training-hero {
  border-color: rgba(138, 180, 248, 0.16);
  background:
    radial-gradient(760px 320px at 6% -30%, rgba(138, 180, 248, 0.16), transparent 62%),
    radial-gradient(620px 300px at 100% 120%, rgba(120, 217, 236, 0.09), transparent 60%),
    linear-gradient(135deg, #10141b 0%, #0e1522 52%, #0f1115 100%);
}

.hero-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(26, 115, 232, 0.055) 1px, transparent 1px),
    linear-gradient(90deg, rgba(26, 115, 232, 0.055) 1px, transparent 1px);
  background-size: 34px 34px;
  mask-image: linear-gradient(to bottom, rgba(0, 0, 0, 0.9), transparent 82%);
  pointer-events: none;
}

html.dark .hero-grid {
  background-image:
    linear-gradient(rgba(138, 180, 248, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(138, 180, 248, 0.06) 1px, transparent 1px);
}

.hero-orb {
  position: absolute;
  border-radius: 9999px;
  filter: blur(46px);
  pointer-events: none;
}

.hero-orb-a {
  width: 220px;
  height: 220px;
  top: -110px;
  right: 16%;
  background: rgba(26, 115, 232, 0.16);
}

.hero-orb-b {
  width: 180px;
  height: 180px;
  bottom: -120px;
  left: 8%;
  background: rgba(36, 193, 224, 0.12);
}

html.dark .hero-orb-a {
  background: rgba(138, 180, 248, 0.14);
}

html.dark .hero-orb-b {
  background: rgba(120, 217, 236, 0.08);
}

.hero-inner {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 28px;
}

@media (min-width: 1024px) {
  .hero-inner {
    flex-direction: row;
    align-items: center;
    gap: 40px;
  }
}

.hero-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.hero-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  border-radius: 9999px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.12em;
  color: #1a73e8;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(26, 115, 232, 0.18);
  backdrop-filter: blur(6px);
}

html.dark .hero-eyebrow {
  color: #a8c7fa;
  background: rgba(168, 199, 250, 0.08);
  border-color: rgba(168, 199, 250, 0.2);
}

.hero-refresh {
  background: rgba(255, 255, 255, 0.7) !important;
  backdrop-filter: blur(8px);
}

html.dark .hero-refresh {
  background: rgba(255, 255, 255, 0.05) !important;
}

.hero-title {
  margin: 0;
  font-size: 30px;
  font-weight: 700;
  line-height: 1.18;
  letter-spacing: -0.02em;
  color: #1b1b1e;
}

html.dark .hero-title {
  color: #e8eaed;
}

.hero-desc {
  max-width: 620px;
  margin: 10px 0 0;
  font-size: 13px;
  line-height: 20px;
  color: #5f6368;
}

html.dark .hero-desc {
  color: #9aa0a6;
}

.hero-stats {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 18px;
  margin-top: 24px;
}

.hero-stat {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.hero-stat-value {
  font-family: 'Fira Code', ui-monospace, monospace;
  font-size: 20px;
  font-weight: 700;
  line-height: 26px;
  letter-spacing: -0.02em;
  color: #202124;
}

html.dark .hero-stat-value {
  color: #e3e3e3;
}

.hero-stat-label {
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.06em;
  color: #80868b;
}

html.dark .hero-stat-label {
  color: #9aa0a6;
}

.hero-stat-divider {
  width: 1px;
  height: 26px;
  background: rgba(26, 115, 232, 0.14);
}

html.dark .hero-stat-divider {
  background: rgba(168, 199, 250, 0.16);
}

.hero-ring {
  position: relative;
  width: 118px;
  height: 118px;
  flex-shrink: 0;
  align-self: center;
}

.hero-ring-svg {
  width: 100%;
  height: 100%;
  display: block;
}

.hero-ring-track {
  stroke: rgba(26, 115, 232, 0.12);
}

html.dark .hero-ring-track {
  stroke: rgba(168, 199, 250, 0.14);
}

.hero-ring-value {
  transition: stroke-dashoffset 0.6s cubic-bezier(0.2, 0, 0, 1);
}

.hero-ring-label {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1px;

  strong {
    font-family: 'Fira Code', ui-monospace, monospace;
    font-size: 22px;
    font-weight: 700;
    line-height: 26px;
    color: #1a73e8;
  }

  span {
    font-size: 9px;
    font-weight: 600;
    letter-spacing: 0.08em;
    color: #80868b;
  }
}

html.dark .hero-ring-label strong {
  color: #a8c7fa;
}

html.dark .hero-ring-label span {
  color: #9aa0a6;
}

/* ------------------------------------------------------------------ */
/* Toolbar / grid                                                      */
/* ------------------------------------------------------------------ */

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid #e5e8ec;
  box-shadow: 0 1px 2px rgba(60, 64, 67, 0.04);
}

html.dark .filter-bar {
  background: #0e0e0e;
  border-color: #2a2b2e;
  box-shadow: none;
}

.filter-category {
  width: 160px;
  flex-shrink: 0;
}

@media (max-width: 767px) {
  .filter-category {
    width: 100%;
  }
}

.filter-search {
  width: 268px;
  margin-left: auto;
  --n-height: 36px !important;
  height: 36px !important;

  :deep(.n-input-wrapper) {
    height: 36px !important;
  }
}

@media (max-width: 767px) {
  .filter-search {
    width: 100%;
    margin-left: 0;
  }
}

.training-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 16px;
}

@media (min-width: 640px) {
  .training-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (min-width: 1536px) {
  .training-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

/* ------------------------------------------------------------------ */
/* Skeleton                                                            */
/* ------------------------------------------------------------------ */

.skeleton-card {
  display: flex;
  flex-direction: column;
  min-height: 184px;
  padding: 20px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid #e5e8ec;
}

html.dark .skeleton-card {
  background: #0e0e0e;
  border-color: #2a2b2e;
}

.sk {
  display: block;
  border-radius: 7px;
  background: linear-gradient(90deg, #f1f3f4 25%, #e8eaed 37%, #f1f3f4 63%);
  background-size: 400% 100%;
  animation: training-shimmer 1.4s ease infinite;
}

html.dark .sk {
  background: linear-gradient(90deg, #1e1f20 25%, #2a2b2e 37%, #1e1f20 63%);
  background-size: 400% 100%;
}

.sk-tile {
  width: 34px;
  height: 34px;
  flex-shrink: 0;
}

.sk-cat {
  width: 64px;
  height: 10px;
}

.sk-points {
  width: 54px;
  height: 22px;
  margin-left: auto;
  border-radius: 9999px;
}

.sk-title {
  width: 88%;
  height: 13px;
  margin-top: 22px;
}

.sk-title.is-short {
  width: 56%;
  margin-top: 9px;
}

.sk-pill {
  width: 58px;
  height: 20px;
  border-radius: 9999px;
}

.sk-meta {
  width: 74px;
  height: 10px;
}

@keyframes training-shimmer {
  0% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0 50%;
  }
}

/* ------------------------------------------------------------------ */
/* Empty                                                               */
/* ------------------------------------------------------------------ */

.empty-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 56px 24px 52px;
  border-radius: 20px;
  background: #fff;
  border: 1px dashed #dfe3e8;
  text-align: center;
}

html.dark .empty-panel {
  background: #0e0e0e;
  border-color: #2f3034;
}

.empty-illustration {
  position: relative;
  width: 96px;
  height: 96px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 18px;
}

.empty-ring {
  position: absolute;
  border-radius: 9999px;

  &.is-outer {
    inset: 0;
    border: 1px dashed rgba(26, 115, 232, 0.25);
    animation: empty-spin 24s linear infinite;
  }

  &.is-inner {
    inset: 14px;
    border: 1px solid rgba(26, 115, 232, 0.14);
    background: radial-gradient(circle at 50% 40%, rgba(26, 115, 232, 0.12), rgba(26, 115, 232, 0.03));
  }
}

html.dark .empty-ring {
  &.is-outer {
    border-color: rgba(168, 199, 250, 0.24);
  }

  &.is-inner {
    border-color: rgba(168, 199, 250, 0.14);
    background: radial-gradient(circle at 50% 40%, rgba(168, 199, 250, 0.12), rgba(168, 199, 250, 0.03));
  }
}

.empty-icon {
  position: relative;
  z-index: 1;
  color: #1a73e8;
}

html.dark .empty-icon {
  color: #a8c7fa;
}

@keyframes empty-spin {
  to {
    transform: rotate(360deg);
  }
}

.empty-title {
  margin: 0;
  font-size: 15px;
  font-weight: 650;
  line-height: 22px;
  color: #202124;
}

html.dark .empty-title {
  color: #e3e3e3;
}

.empty-desc {
  max-width: 360px;
  margin: 7px 0 20px;
  font-size: 12px;
  line-height: 19px;
  color: #80868b;
}

html.dark .empty-desc {
  color: #9aa0a6;
}
</style>
