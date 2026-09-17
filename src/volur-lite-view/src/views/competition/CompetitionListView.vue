<template>
  <div class="competition-list-view">
    <!-- Header & Search Bar -->
    <div class="header-action-bar">
      <div class="page-title-row">
        <div class="page-title">
          <h2>{{ t('competition.page_title') }}</h2>
          <p class="subtitle">{{ t('competition.page_subtitle') }}</p>
        </div>
      </div>

      <!-- Filter Tabs & Search Toolbar -->
      <div class="filter-toolbar-row">
        <div class="filter-tabs">
          <n-tabs
            v-model:value="selectedType"
            type="segment"
            size="medium"
            @update:value="handleTypeChange"
          >
            <n-tab
              v-for="tab in availableTypeTabs"
              :key="tab.value"
              :name="tab.value"
            >
              <div class="tab-item-inner">
                <span class="tab-label">{{ tab.label }}</span>
                <span
                  class="tab-count-badge"
                  :class="{ 'is-active': selectedType === tab.value }"
                >
                  {{ tab.count }}
                </span>
              </div>
            </n-tab>
          </n-tabs>
        </div>

        <div class="search-bar">
          <n-input
            v-model:value="searchKeyword"
            :placeholder="t('competition.search_placeholder')"
            clearable
            style="width: 280px;"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix>
              <n-icon :component="SearchOutline" />
            </template>
          </n-input>
          <n-button class="theme-primary-btn" @click="handleSearch">
            搜索
          </n-button>
        </div>
      </div>
    </div>

    <!-- Competition Cards: 一行一个 (Single Item Per Row) -->
    <n-spin :show="loading">
      <div class="competition-row-list">
        <n-card
          v-for="comp in filteredList"
          :key="comp.id"
          hoverable
          class="competition-row-card"
          :bordered="false"
          @click="goToDetail(comp.slug)"
        >
          <div class="row-card-inner">
            <!-- 1. 竞赛 Logo / Banner 封面区 -->
            <div class="comp-logo-box">
              <img
                v-if="comp.bannerUrl"
                :src="formatBannerUrl(comp.bannerUrl)"
                :alt="comp.title"
                class="comp-logo-img"
                loading="lazy"
              />
              <div v-else class="comp-logo-placeholder">
                <n-icon size="44" :component="TrophyOutline" class="fallback-icon" />
              </div>
            </div>

            <!-- 2. 竞赛核心信息区 (标题、状态、赛程时间、报名队伍数量) -->
            <div class="comp-info-main">
              <div class="title-badge-row">
                <h3 class="comp-title">{{ comp.title }}</h3>
                <div class="badges-wrap">
                  <n-tag :type="getTypeTagType(comp.type)" round size="small" :bordered="false" class="type-tag">
                    {{ getTypeLabel(comp.type) }}
                  </n-tag>
                  <n-tag :type="getStatusType(comp.status)" round size="small">
                    {{ getStatusText(comp.status) }}
                  </n-tag>
                </div>
              </div>

              <div
                v-if="comp.description"
                class="comp-desc"
                v-html="renderMarkdown(comp.description)"
              />

              <div class="comp-meta-row">
                <!-- 比赛时间 -->
                <div class="meta-item">
                  <n-icon :component="CalendarOutline" class="meta-icon" />
                  <span class="meta-label">{{ t('competition.start_time') }}:</span>
                  <span class="meta-val">{{ formatDateTime(comp.startTime) }} ~ {{ formatDateTime(comp.endTime) }}</span>
                </div>

                <!-- 报名时间 -->
                <div class="meta-item">
                  <n-icon :component="TimeOutline" class="meta-icon" />
                  <span class="meta-label">{{ t('competition.reg_time') }}:</span>
                  <span class="meta-val">{{ formatRegTime(comp) }}</span>
                </div>
              </div>
            </div>

            <!-- 3. 右侧操作区 (查看竞赛详情) -->
            <div class="comp-action-right">
              <n-button
                class="theme-primary-btn detail-btn"
                size="medium"
                @click.stop="goToDetail(comp.slug)"
              >
                <template #icon>
                  <n-icon :component="ArrowForwardOutline" />
                </template>
                {{ t('competition.view_details') }}
              </n-button>
            </div>
          </div>
        </n-card>
      </div>

      <!-- Empty State -->
      <div v-if="filteredList.length === 0 && !loading" class="empty-state">
        <n-empty :description="t('competition.empty_desc')" />
      </div>

      <!-- Pagination -->
      <div v-if="pagination.total > pagination.pageSize" class="pagination-wrap">
        <n-pagination
          v-model:page="pagination.page"
          :page-size="pagination.pageSize"
          :item-count="pagination.total"
          @update:page="handlePageChange"
        />
      </div>
    </n-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  NCard,
  NTag,
  NButton,
  NTabs,
  NTab,
  NEmpty,
  NInput,
  NIcon,
  NPagination,
  NSpin
} from 'naive-ui'
import {
  SearchOutline,
  ArrowForwardOutline,
  CalendarOutline,
  TimeOutline,
  TrophyOutline
} from '@vicons/ionicons5'
import { useI18n } from 'vue-i18n'
import type {
  CompetitionSummaryVO,
  CompetitionType,
  CompetitionStatus
} from '@/types/competition'
import { getCompetitionListApi } from '@/api/competition'
import { formatBannerUrl } from '@/utils/imageCache'
import { renderMarkdown } from '@/utils/markdown'

const { t } = useI18n()
const router = useRouter()

interface FilterTabItem {
  value: string
  label: string
  count: number
}

const SUPPORTED_COMPETITION_TYPES: CompetitionType[] = [
  'CTF',
  'THEORY',
  'AWD',
  'AWD_PLUS',
  'ISW'
]

// State
const loading = ref<boolean>(false)
const searchKeyword = ref<string>('')
const selectedType = ref<string>('ALL')
const competitionList = ref<CompetitionSummaryVO[]>([])

// Pagination
const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

// 统计当前响应列表中的每种赛制出现频次
const typeCountMap = computed<Record<string, number>>(() => {
  const map: Record<string, number> = {}
  for (const item of competitionList.value) {
    if (item.type) {
      map[item.type] = (map[item.type] || 0) + 1
    }
  }
  return map
})

// 根据 /api/v1/competitions 响应数据动态判别并生成赛制过滤标签
const availableTypeTabs = computed<FilterTabItem[]>(() => {
  const tabs: FilterTabItem[] = [
    {
      value: 'ALL',
      label: t('competition.type_all'),
      count: competitionList.value.length
    }
  ]

  // 按照标准赛制顺序，仅展示响应数据中真实存在的赛制
  for (const type of SUPPORTED_COMPETITION_TYPES) {
    const count = typeCountMap.value[type] || 0
    if (count > 0) {
      tabs.push({
        value: type,
        label: getTypeLabel(type),
        count
      })
    }
  }

  // 兜底：若后台返回了未在标准列表中的其他赛制类型，也动态追加显示
  for (const type of Object.keys(typeCountMap.value)) {
    if (!SUPPORTED_COMPETITION_TYPES.includes(type as CompetitionType) && type !== 'ALL') {
      tabs.push({
        value: type,
        label: getTypeLabel(type as CompetitionType),
        count: typeCountMap.value[type]
      })
    }
  }

  return tabs
})

// 监听可用赛制标签列表：若当前选中的类型在最新列表中不再存在，平滑重置为全部
watch(availableTypeTabs, (newTabs) => {
  if (selectedType.value !== 'ALL' && !newTabs.some((tab) => tab.value === selectedType.value)) {
    selectedType.value = 'ALL'
  }
})

// Filtered List by Single Mode Type
const filteredList = computed(() => {
  if (selectedType.value === 'ALL') {
    return competitionList.value
  }
  return competitionList.value.filter((item) => item.type === selectedType.value)
})

function handleTypeChange(val: string) {
  selectedType.value = val
}

function handleSearch() {
  pagination.page = 1
  fetchData()
}

function handlePageChange(page: number) {
  pagination.page = page
  fetchData()
}

function formatDateTime(val?: string | null): string {
  if (!val) return '-'
  try {
    const d = new Date(val)
    if (isNaN(d.getTime())) return val
    const pad = (n: number) => (n < 10 ? '0' + n : String(n))
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
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

function getTypeLabel(type?: CompetitionType): string {
  if (!type) return '-'
  switch (type) {
    case 'CTF': return t('competition.type_ctf')
    case 'THEORY': return t('competition.type_theory')
    case 'AWD': return t('competition.type_awd')
    case 'AWD_PLUS': return t('competition.type_awd_plus')
    case 'ISW': return t('competition.type_isw')
    default: return type
  }
}

function getTypeTagType(type?: CompetitionType): 'info' | 'success' | 'warning' | 'error' | 'default' {
  if (!type) return 'default'
  switch (type) {
    case 'CTF': return 'info'
    case 'THEORY': return 'success'
    case 'AWD': return 'warning'
    case 'AWD_PLUS': return 'error'
    case 'ISW': return 'default'
    default: return 'default'
  }
}

function getStatusType(status?: CompetitionStatus): 'warning' | 'success' | 'info' | 'error' | 'default' {
  if (!status) return 'default'
  switch (status) {
    case 'PUBLISHED':
    case 'REGISTRATION': return 'info'
    case 'RUNNING': return 'success'
    case 'FROZEN': return 'warning'
    case 'ENDED':
    case 'ARCHIVED': return 'default'
    default: return 'default'
  }
}

function getStatusText(status?: CompetitionStatus): string {
  if (!status) return '-'
  switch (status) {
    case 'DRAFT': return t('competition.status_draft')
    case 'PUBLISHED': return t('competition.status_published')
    case 'REGISTRATION': return t('competition.status_registration')
    case 'RUNNING': return t('competition.status_running')
    case 'FROZEN': return t('competition.status_frozen')
    case 'ENDED': return t('competition.status_ended')
    case 'ARCHIVED': return t('competition.status_archived')
    default: return status
  }
}

function goToDetail(slug: string) {
  router.push(`/c/${slug}`)
}

/**
 * 加载公开竞赛列表
 */
async function fetchData() {
  loading.value = true
  try {
    const res = await getCompetitionListApi({
      page: pagination.page,
      size: pagination.pageSize,
      keyword: searchKeyword.value || undefined
    })
    if (res.data && res.data.list) {
      competitionList.value = res.data.list
      pagination.total = res.data.total
    }
  } catch {
    competitionList.value = [ ]
    pagination.total = competitionList.value.length
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="scss">
.competition-list-view {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.header-action-bar {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .page-title-row {
    .page-title {
      h2 {
        font-size: 24px;
        font-weight: 700;
        margin-bottom: 4px;
        color: var(--v-text-primary, #111111);
      }
      .subtitle {
        font-size: 13px;
        color: #444444;
      }
    }
  }

  .filter-toolbar-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 16px;

    .filter-tabs {
      width: fit-content;
      max-width: 100%;
      overflow-x: auto;
      scrollbar-width: none;
      &::-webkit-scrollbar {
        display: none;
      }

      :deep(.n-tabs) {
        width: fit-content;
      }

      :deep(.n-tabs-rail) {
        padding: 3px;
        border-radius: 8px;
        background: rgba(0, 0, 0, 0.05);

        @include theme-dark {
          background: rgba(255, 255, 255, 0.08);
        }
      }

      :deep(.n-tabs-tab) {
        padding: 6px 14px;
        transition: all 0.2s ease;
      }

      .tab-item-inner {
        display: flex;
        align-items: center;
        gap: 6px;
        white-space: nowrap;

        .tab-label {
          font-weight: 500;
        }

        .tab-count-badge {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          min-width: 18px;
          height: 18px;
          padding: 0 5px;
          border-radius: 9px;
          font-size: 11px;
          font-weight: 600;
          background: rgba(0, 0, 0, 0.06);
          color: #555555;
          transition: all 0.2s ease;

          @include theme-dark {
            background: rgba(255, 255, 255, 0.12);
            color: #cccccc;
          }

          &.is-active {
            background: #000000;
            color: #ffffff;

            @include theme-dark {
              background: #ffffff;
              color: #000000;
            }
          }
        }
      }
    }

    .search-bar {
      display: flex;
      align-items: center;
      gap: 10px;

      @media (max-width: 768px) {
        width: 100%;

        :deep(.n-input) {
          flex: 1;
          width: auto !important;
        }
      }
    }
  }
}

/* 一行一个列表容器 */
.competition-row-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.competition-row-card {
  border-radius: 12px;
  cursor: pointer;
  background: var(--v-bg-card, #ffffff);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  transition: transform 0.2s ease, box-shadow 0.2s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
  }

  :deep(.n-card__content) {
    padding: 16px 20px;
  }
}

.row-card-inner {
  display: flex;
  align-items: center;
  gap: 20px;

  @media (max-width: 768px) {
    flex-direction: column;
    align-items: stretch;
  }
}

/* 1. 竞赛 Logo / Banner 封面区 */
.comp-logo-box {
  width: 210px;
  aspect-ratio: 21 / 9;
  height: auto;
  flex-shrink: 0;
  border-radius: 8px;
  overflow: hidden;
  background: rgba(0, 0, 0, 0.04);

  @media (max-width: 768px) {
    width: 100%;
    aspect-ratio: 21 / 9;
    height: auto;
  }

  .comp-logo-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
    transition: transform 0.3s ease;

    &:hover {
      transform: scale(1.04);
    }
  }

  .comp-logo-placeholder {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, rgba(0, 0, 0, 0.03) 0%, rgba(0, 0, 0, 0.07) 100%);

    .fallback-icon {
      color: rgba(0, 0, 0, 0.25);
    }
  }
}

/* 2. 竞赛核心信息区 */
.comp-info-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;

  .title-badge-row {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 10px;

    .comp-title {
      font-size: 17px;
      font-weight: 700;
      margin: 0;
      color: var(--v-text-primary, #111111);
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      max-width: 480px;

      @media (max-width: 768px) {
        max-width: 100%;
      }
    }

    .badges-wrap {
      display: flex;
      align-items: center;
      gap: 6px;

      .type-tag {
        font-weight: 600;
      }
    }
  }

  .comp-desc {
    font-size: 13px;
    color: var(--v-text-desc, #444444);
    line-height: 1.5;
    margin: 0;
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
      color: var(--v-text-primary, #111111);
    }

    :deep(em),
    :deep(i) {
      font-style: italic;
    }

    :deep(code) {
      font-family: var(--v-font-mono, 'Fira Code', monospace);
      font-size: 12px;
      padding: 1px 4px;
      background: var(--v-bg-code, #f4f3ec);
      border-radius: 3px;
      color: var(--v-text-primary, #111111);
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

  .comp-meta-row {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 20px;
    font-size: 13px;
    margin-top: 2px;

    .meta-item {
      display: flex;
      align-items: center;
      gap: 6px;

      .meta-icon {
        font-size: 15px;
        color: #666666;
      }

      .meta-label {
        color: #444444;
      }

      .meta-val {
        font-weight: 500;
        color: var(--v-text-primary, #222222);
      }
    }
  }
}

/* 3. 右侧操作区 */
.comp-action-right {
  flex-shrink: 0;

  @media (max-width: 768px) {
    display: flex;
    justify-content: flex-end;
  }

  .detail-btn {
    padding: 0 20px;
    height: 38px;
    font-size: 14px;
  }
}

.theme-primary-btn {
  background: #000000 !important;
  color: #ffffff !important;
  border-radius: 8px;
  font-weight: 600;

  &:hover {
    background: #222222 !important;
  }
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

.empty-state {
  padding: 60px 0;
  display: flex;
  justify-content: center;
}
</style>
