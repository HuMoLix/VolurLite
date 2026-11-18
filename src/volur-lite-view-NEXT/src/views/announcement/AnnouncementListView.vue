<template>
  <div class="announcement-list-view flex-1 flex flex-col min-w-0">
    <VViewlayout>
      <template #header>
        <section class="announcement-hero">
          <div class="hero-grid" aria-hidden="true" />
          <div class="hero-orb hero-orb-a" aria-hidden="true" />
          <div class="hero-orb hero-orb-b" aria-hidden="true" />

          <div class="hero-inner">
            <div class="min-w-0 flex-1">
              <span class="hero-eyebrow">
                <n-icon :size="14" :component="MegaphoneOutline" />
                {{ t('announcement.hero_eyebrow', 'NEWSROOM') }}
              </span>

              <h1 class="hero-title">
                {{ t('announcement.title', '公告中心') }}
              </h1>
              <p class="hero-desc">
                {{ t('announcement.subtitle', '平台公告、活动与维护通知，重要信息不错过') }}
              </p>

              <div class="hero-search-row">
                <VSearch
                  v-model:value="searchKeyword"
                  :placeholder="t('announcement.search_placeholder', '搜索公告标题或内容')"
                  size="large"
                  :radius="13"
                  class="hero-search"
                  @search="handleSearch"
                />
              </div>
            </div>

            <div class="hero-mark-wrap">
              <div class="hero-mark">
                <span class="hero-mark-ring is-outer" />
                <span class="hero-mark-ring is-inner" />
                <n-icon :size="38" :component="MegaphoneOutline" />
              </div>
              <div class="hero-count">
                <strong>{{ total }}</strong>
                <span>{{ t('announcement.total_suffix', '条公告') }}</span>
              </div>
            </div>
          </div>
        </section>
      </template>

      <div class="space-y-6">
        <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <VFilterBtnGroup
            v-model="selectedCategory"
            :options="categoryOptions"
            @change="handleCategoryChange"
          />
          <span class="total-text">
            {{ t('announcement.total_prefix', '共') }} {{ total }} {{ t('announcement.total_suffix', '条公告') }}
          </span>
        </div>

        <div v-if="loading" class="space-y-6">
          <div class="skeleton-featured">
            <span class="sk sk-badge" />
            <span class="sk sk-title" />
            <span class="sk sk-line" />
            <span class="sk sk-line is-short" />
          </div>
          <div class="announcement-grid">
            <div v-for="i in 4" :key="i" class="skeleton-card">
              <div class="flex items-center justify-between">
                <span class="sk sk-date" />
                <span class="sk sk-badge" />
              </div>
              <span class="sk sk-card-title" />
              <span class="sk sk-line" />
              <span class="sk sk-line is-short" />
              <div class="mt-auto flex items-center justify-between pt-5">
                <span class="sk sk-meta" />
                <span class="sk sk-meta is-short" />
              </div>
            </div>
          </div>
        </div>

        <div v-else-if="announcements.length === 0" class="empty-panel">
          <div class="empty-illustration">
            <span class="empty-ring is-outer" />
            <span class="empty-ring is-inner" />
            <span class="empty-icon">
              <n-icon :size="32" :component="MegaphoneOutline" />
            </span>
          </div>
          <h3 class="empty-title">
            {{ t('announcement.empty_title', '还没有公告') }}
          </h3>
          <p class="empty-desc">
            {{ t('announcement.empty_desc', '平台公告、活动与维护通知会第一时间发布在这里') }}
          </p>
        </div>

        <template v-else>
          <article
            v-if="featured"
            class="featured-card"
            :class="{ 'has-cover': Boolean(featured.coverImageUrl) }"
            role="button"
            tabindex="0"
            @click="openDetail(featured)"
            @keydown.enter="openDetail(featured)"
          >
            <div class="featured-glow" aria-hidden="true" />
            <div class="featured-body">
              <div class="flex items-center gap-2 flex-wrap">
                <span
                  class="category-pill"
                  :class="isPromotion(featured) ? 'is-promotion' : 'is-system'"
                >
                  {{ categoryLabel(featured) }}
                </span>
                <span v-if="featured.forcePopup" class="featured-flag">
                  <n-icon :size="12" :component="FlameOutline" />
                  {{ t('announcement.featured', '置顶') }}
                </span>
                <span class="featured-date">
                  {{ formatTime(featured.publishTime || featured.createdAt) }}
                </span>
              </div>

              <h2 class="featured-title">
                {{ featured.title }}
              </h2>
              <p class="featured-excerpt">
                {{ excerpt(featured.content) }}
              </p>

              <div class="featured-foot">
                <span v-if="featured.publisher" class="meta-item">
                  <n-icon :size="14" :component="PersonOutline" />
                  {{ featured.publisher }}
                </span>
                <span class="read-more">
                  {{ t('announcement.read_more', '阅读全文') }}
                  <n-icon :size="15" :component="ArrowForwardOutline" />
                </span>
              </div>
            </div>

            <div v-if="featured.coverImageUrl" class="featured-cover">
              <img :src="featured.coverImageUrl" :alt="featured.title" loading="lazy" />
            </div>
          </article>

          <div v-if="restAnnouncements.length > 0" class="announcement-grid">
            <article
              v-for="item in restAnnouncements"
              :key="String(item.id)"
              class="announcement-card"
              role="button"
              tabindex="0"
              @click="openDetail(item)"
              @keydown.enter="openDetail(item)"
            >
              <div class="card-top">
                <div class="date-block">
                  <strong>{{ dateParts(item.publishTime || item.createdAt).day }}</strong>
                  <span>{{ dateParts(item.publishTime || item.createdAt).label }}</span>
                </div>
                <span
                  class="category-pill"
                  :class="isPromotion(item) ? 'is-promotion' : 'is-system'"
                >
                  {{ categoryLabel(item) }}
                </span>
              </div>

              <h3 class="card-title">
                {{ item.title }}
              </h3>
              <p class="card-excerpt">
                {{ excerpt(item.content) }}
              </p>

              <div class="card-foot">
                <span v-if="item.publisher" class="meta-item">
                  <n-icon :size="13" :component="PersonOutline" />
                  {{ item.publisher }}
                </span>
                <span v-else class="meta-item">
                  <n-icon :size="13" :component="PersonOutline" />
                  {{ t('announcement.platform', '平台团队') }}
                </span>
                <span class="view-link">
                  {{ t('announcement.view_detail', '查看详情') }}
                  <n-icon :size="14" :component="ChevronForwardOutline" />
                </span>
              </div>
            </article>
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
      </div>

      <n-modal
        :show="detailVisible"
        preset="card"
        closable
        class="announcement-detail-modal w-full"
        :style="{ width: '780px', maxWidth: 'calc(100vw - 32px)' }"
        @update:show="detailVisible = $event"
        @close="detailVisible = false"
      >
        <template #header>
          <div class="min-w-0 pr-3">
            <div class="flex items-center gap-2 flex-wrap mb-2">
              <span
                v-if="activeAnnouncement"
                class="category-pill"
                :class="isPromotion(activeAnnouncement) ? 'is-promotion' : 'is-system'"
              >
                {{ categoryLabel(activeAnnouncement) }}
              </span>
              <span class="detail-date">
                {{ formatTime(activeAnnouncement?.publishTime || activeAnnouncement?.createdAt) }}
              </span>
            </div>
            <h2 class="detail-title">
              {{ activeAnnouncement?.title }}
            </h2>
          </div>
        </template>

        <div v-if="activeAnnouncement" class="detail-body">
          <div v-if="activeAnnouncement.coverImageUrl" class="detail-cover">
            <img :src="activeAnnouncement.coverImageUrl" :alt="activeAnnouncement.title" loading="lazy" />
          </div>

          <div class="detail-meta">
            <span v-if="activeAnnouncement.publisher" class="meta-item">
              <n-icon :size="14" :component="PersonOutline" />
              {{ activeAnnouncement.publisher }}
            </span>
            <span v-if="activeAnnouncement.expireTime" class="meta-item">
              <n-icon :size="14" :component="TimeOutline" />
              {{ t('announcement.expire_at', '有效期至') }}
              {{ formatTime(activeAnnouncement.expireTime) }}
            </span>
          </div>

          <div
            v-if="activeAnnouncement.content"
            class="volur-markdown-body detail-prose"
            v-html="renderMarkdown(activeAnnouncement.content)"
          />
          <div v-else class="detail-empty">
            {{ t('announcement.no_content', '暂无公告内容') }}
          </div>

          <div v-if="activeAnnouncement.linkUrl" class="pt-1">
            <a
              :href="activeAnnouncement.linkUrl"
              target="_blank"
              rel="noopener noreferrer"
              class="detail-link"
            >
              <n-icon :size="15" :component="OpenOutline" />
              <span>{{ t('announcement.open_link', '查看相关链接') }}</span>
            </a>
          </div>
        </div>
      </n-modal>
    </VViewlayout>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { NIcon, NModal, NPagination } from 'naive-ui'
import {
  ArrowForwardOutline,
  ChevronForwardOutline,
  FlameOutline,
  MegaphoneOutline,
  OpenOutline,
  PersonOutline,
  TimeOutline
} from '@vicons/ionicons5'
import { VFilterBtnGroup, VSearch } from '@/components/common'
import type { FilterOption } from '@/components/common'
import { VViewlayout } from '@/volur-ui'
import { getBroadcastListApi } from '@/api/notification'
import { renderMarkdown } from '@/utils/markdown'
import type { BroadcastMessageVO } from '@/types/notification'

defineOptions({
  name: 'AnnouncementListView'
})

const PAGE_SIZE = 8

const { t } = useI18n()

const loading = ref(false)
const announcements = ref<BroadcastMessageVO[]>([])
const total = ref(0)
const currentPage = ref(1)
const selectedCategory = ref('')
const searchKeyword = ref('')

const detailVisible = ref(false)
const activeAnnouncement = ref<BroadcastMessageVO | null>(null)

const featured = computed<BroadcastMessageVO | null>(() => announcements.value[0] || null)
const restAnnouncements = computed<BroadcastMessageVO[]>(() => announcements.value.slice(1))

const categoryOptions = computed<FilterOption<string>[]>(() => [
  { value: '', label: t('announcement.category_all', '全部') },
  { value: 'ANNOUNCEMENT', label: t('announcement.category_system', '系统公告') },
  { value: 'PROMOTION', label: t('announcement.category_promotion', '运营活动') }
])

function isPromotion(item: BroadcastMessageVO | null): boolean {
  return item?.category === 'PROMOTION'
}

function categoryLabel(item: BroadcastMessageVO | null): string {
  if (isPromotion(item)) {
    return t('announcement.category_promotion', '运营活动')
  }
  return t('announcement.category_system', '系统公告')
}

function excerpt(content?: string | null): string {
  if (!content) return ''
  return content
    .replace(/```[\s\S]*?```/g, ' ')
    .replace(/!\[[^\]]*]\([^)]*\)/g, ' ')
    .replace(/\[([^\]]*)]\([^)]*\)/g, '$1')
    .replace(/[#>*_`~-]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
    .slice(0, 190)
}

function formatTime(value?: string | null): string {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const pad = (n: number) => (n < 10 ? `0${n}` : String(n))
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function dateParts(value?: string | null): { day: string; label: string } {
  if (!value) return { day: '--', label: '' }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return { day: '--', label: '' }
  const pad = (n: number) => (n < 10 ? `0${n}` : String(n))
  return {
    day: pad(date.getDate()),
    label: `${date.getFullYear()}.${pad(date.getMonth() + 1)}`
  }
}

async function fetchList(page = 1) {
  loading.value = true
  try {
    const res = await getBroadcastListApi({
      page,
      size: PAGE_SIZE,
      category: selectedCategory.value || undefined,
      keyword: searchKeyword.value.trim() || undefined,
      sortBy: 'publishTime',
      sortOrder: 'desc'
    })
    const data = res?.data
    announcements.value = data?.list || []
    total.value = Number(data?.total ?? 0)
    currentPage.value = data?.currentPage ?? page
  } catch {
    announcements.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleCategoryChange(value: string | number) {
  selectedCategory.value = String(value)
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

function openDetail(item: BroadcastMessageVO) {
  activeAnnouncement.value = item
  detailVisible.value = true
}

onMounted(() => {
  fetchList(1)
})
</script>

<style scoped lang="scss">
.announcement-list-view {
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

/* ------------------------------------------------------------------ */
/* Hero                                                                */
/* ------------------------------------------------------------------ */

.announcement-hero {
  position: relative;
  overflow: hidden;
  margin-bottom: 26px;
  padding: 34px 36px 32px;
  border-radius: 24px;
  border: 1px solid rgba(36, 193, 224, 0.16);
  background:
    radial-gradient(720px 300px at 4% -20%, rgba(26, 115, 232, 0.15), transparent 62%),
    radial-gradient(640px 300px at 100% 130%, rgba(36, 193, 224, 0.16), transparent 60%),
    linear-gradient(135deg, #f6fbff 0%, #eef8fd 52%, #f8fbff 100%);
}

html.dark .announcement-hero {
  border-color: rgba(120, 217, 236, 0.16);
  background:
    radial-gradient(720px 300px at 4% -20%, rgba(138, 180, 248, 0.15), transparent 62%),
    radial-gradient(640px 300px at 100% 130%, rgba(120, 217, 236, 0.12), transparent 60%),
    linear-gradient(135deg, #10141b 0%, #0d171d 52%, #0f1115 100%);
}

.hero-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(26, 115, 232, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(26, 115, 232, 0.05) 1px, transparent 1px);
  background-size: 34px 34px;
  mask-image: linear-gradient(to bottom, rgba(0, 0, 0, 0.9), transparent 84%);
  pointer-events: none;
}

html.dark .hero-grid {
  background-image:
    linear-gradient(rgba(138, 180, 248, 0.055) 1px, transparent 1px),
    linear-gradient(90deg, rgba(138, 180, 248, 0.055) 1px, transparent 1px);
}

.hero-orb {
  position: absolute;
  border-radius: 9999px;
  filter: blur(46px);
  pointer-events: none;
}

.hero-orb-a {
  width: 210px;
  height: 210px;
  top: -110px;
  right: 22%;
  background: rgba(26, 115, 232, 0.15);
}

.hero-orb-b {
  width: 190px;
  height: 190px;
  bottom: -130px;
  left: 6%;
  background: rgba(36, 193, 224, 0.14);
}

html.dark .hero-orb-a {
  background: rgba(138, 180, 248, 0.13);
}

html.dark .hero-orb-b {
  background: rgba(120, 217, 236, 0.1);
}

.hero-inner {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 28px;
}

@media (min-width: 900px) {
  .hero-inner {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    gap: 40px;
  }
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

.hero-title {
  margin: 16px 0 0;
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
  max-width: 560px;
  margin: 10px 0 0;
  font-size: 13px;
  line-height: 20px;
  color: #5f6368;
}

html.dark .hero-desc {
  color: #9aa0a6;
}

.hero-search-row {
  margin-top: 22px;
  width: 100%;
  max-width: 460px;
}

.hero-search {
  --n-height: 44px !important;
  height: 44px !important;

  :deep(.n-input-wrapper) {
    height: 44px !important;
  }
}

.hero-mark-wrap {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 18px;
  flex-shrink: 0;
  align-self: center;
}

.hero-mark {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 104px;
  height: 104px;
  border-radius: 30px;
  color: #fff;
  background: linear-gradient(135deg, #1a73e8 0%, #4c9bf5 55%, #24c1e0 100%);
  box-shadow: 0 14px 30px rgba(26, 115, 232, 0.24);
}

html.dark .hero-mark {
  color: #0b1220;
  background: linear-gradient(135deg, #a8c7fa 0%, #8ab4f8 55%, #78d9ec 100%);
  box-shadow: 0 14px 30px rgba(0, 0, 0, 0.4);
}

.hero-mark-ring {
  position: absolute;
  border-radius: 9999px;
  border: 1px solid rgba(26, 115, 232, 0.16);

  &.is-outer {
    inset: -14px;
    border-style: dashed;
    animation: mark-spin 26s linear infinite;
  }

  &.is-inner {
    inset: -7px;
  }
}

html.dark .hero-mark-ring {
  border-color: rgba(168, 199, 250, 0.2);
}

@keyframes mark-spin {
  to {
    transform: rotate(360deg);
  }
}

.hero-count {
  display: flex;
  flex-direction: column;
  gap: 1px;

  strong {
    font-family: 'Fira Code', ui-monospace, monospace;
    font-size: 26px;
    font-weight: 700;
    line-height: 30px;
    letter-spacing: -0.02em;
    color: #1b1b1e;
  }

  span {
    font-size: 10px;
    font-weight: 600;
    letter-spacing: 0.06em;
    color: #80868b;
  }
}

html.dark .hero-count strong {
  color: #e8eaed;
}

html.dark .hero-count span {
  color: #9aa0a6;
}

.total-text {
  font-size: 11px;
  font-weight: 500;
  color: #80868b;
}

html.dark .total-text {
  color: #9aa0a6;
}

/* ------------------------------------------------------------------ */
/* Cards                                                               */
/* ------------------------------------------------------------------ */

.featured-card {
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  border-radius: 22px;
  border: 1px solid rgba(26, 115, 232, 0.16);
  background:
    radial-gradient(560px 260px at 0% 0%, rgba(26, 115, 232, 0.1), transparent 58%),
    linear-gradient(120deg, #f7faff 0%, #eef4ff 60%, #f3f9ff 100%);
  cursor: pointer;
  transition:
    transform 0.22s cubic-bezier(0.2, 0, 0, 1),
    box-shadow 0.22s cubic-bezier(0.2, 0, 0, 1),
    border-color 0.22s cubic-bezier(0.2, 0, 0, 1);

  &:hover {
    transform: translateY(-2px);
    border-color: rgba(26, 115, 232, 0.32);
    box-shadow: 0 14px 34px rgba(26, 115, 232, 0.12);
  }

  &:focus-visible {
    outline: 2px solid rgba(26, 115, 232, 0.6);
    outline-offset: 2px;
  }

  &.has-cover {
    flex-direction: row;
  }
}

html.dark .featured-card {
  border-color: rgba(138, 180, 248, 0.18);
  background:
    radial-gradient(560px 260px at 0% 0%, rgba(138, 180, 248, 0.11), transparent 58%),
    linear-gradient(120deg, #0f141c 0%, #0d1522 60%, #0e141b 100%);

  &:hover {
    border-color: rgba(138, 180, 248, 0.34);
    box-shadow: 0 14px 34px rgba(0, 0, 0, 0.45);
  }
}

.featured-glow {
  position: absolute;
  width: 240px;
  height: 240px;
  right: 12%;
  top: -140px;
  border-radius: 9999px;
  background: rgba(36, 193, 224, 0.14);
  filter: blur(50px);
  pointer-events: none;
}

html.dark .featured-glow {
  background: rgba(120, 217, 236, 0.1);
}

.featured-body {
  position: relative;
  z-index: 1;
  flex: 1;
  min-width: 0;
  padding: 28px 30px;
  display: flex;
  flex-direction: column;
}

.featured-title {
  margin: 14px 0 0;
  font-size: 22px;
  font-weight: 700;
  line-height: 31px;
  letter-spacing: -0.01em;
  color: #1b1b1e;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

html.dark .featured-title {
  color: #e8eaed;
}

.featured-excerpt {
  margin: 10px 0 0;
  max-width: 720px;
  font-size: 13px;
  line-height: 21px;
  color: #5f6368;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

html.dark .featured-excerpt {
  color: #9aa0a6;
}

.featured-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 22px;
  padding-top: 16px;
  border-top: 1px solid rgba(26, 115, 232, 0.1);
}

html.dark .featured-foot {
  border-top-color: rgba(138, 180, 248, 0.1);
}

.featured-date {
  font-family: 'Fira Code', ui-monospace, monospace;
  font-size: 10px;
  color: #80868b;
  margin-left: auto;
}

html.dark .featured-date {
  color: #9aa0a6;
}

.featured-cover {
  position: relative;
  z-index: 1;
  width: 300px;
  flex-shrink: 0;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }
}

@media (max-width: 767px) {
  .featured-card.has-cover {
    flex-direction: column;
  }

  .featured-body {
    padding: 22px 20px;
  }

  .featured-cover {
    width: 100%;
    max-height: 200px;
  }
}

.featured-flag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 9999px;
  font-size: 10px;
  font-weight: 700;
  line-height: 16px;
  color: #c5221f;
  background: rgba(234, 67, 53, 0.1);
  border: 1px solid rgba(234, 67, 53, 0.22);
}

html.dark .featured-flag {
  color: #f28b82;
  background: rgba(242, 139, 130, 0.12);
  border-color: rgba(242, 139, 130, 0.26);
}

.read-more {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  font-weight: 600;
  color: #1a73e8;
  flex-shrink: 0;
}

html.dark .read-more {
  color: #a8c7fa;
}

.featured-card:hover .read-more :deep(svg) {
  transform: translateX(2px);
}

.read-more :deep(svg) {
  transition: transform 0.18s ease;
}

.announcement-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 16px;
}

@media (min-width: 768px) {
  .announcement-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (min-width: 1536px) {
  .announcement-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

.announcement-card {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 208px;
  padding: 20px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid #e5e8ec;
  box-shadow: 0 1px 2px rgba(60, 64, 67, 0.04);
  cursor: pointer;
  transition:
    transform 0.22s cubic-bezier(0.2, 0, 0, 1),
    box-shadow 0.22s cubic-bezier(0.2, 0, 0, 1),
    border-color 0.22s cubic-bezier(0.2, 0, 0, 1);

  &::after {
    content: '';
    position: absolute;
    left: 20px;
    right: 20px;
    bottom: 0;
    height: 2px;
    border-radius: 2px 2px 0 0;
    background: linear-gradient(90deg, #1a73e8, #24c1e0);
    transform: scaleX(0);
    transform-origin: left;
    transition: transform 0.28s cubic-bezier(0.2, 0, 0, 1);
  }

  &:hover {
    transform: translateY(-3px);
    border-color: rgba(26, 115, 232, 0.3);
    box-shadow: 0 12px 28px rgba(26, 115, 232, 0.1);
  }

  &:hover::after {
    transform: scaleX(1);
  }

  &:focus-visible {
    outline: 2px solid rgba(26, 115, 232, 0.6);
    outline-offset: 2px;
  }
}

html.dark .announcement-card {
  background: #0e0e0e;
  border-color: #2a2b2e;
  box-shadow: none;

  &:hover {
    border-color: rgba(138, 180, 248, 0.34);
    box-shadow: 0 12px 30px rgba(0, 0, 0, 0.45);
  }
}

.card-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.date-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  border-radius: 12px;
  background: #f6f8fa;
  border: 1px solid #eceef1;
  flex-shrink: 0;

  strong {
    font-family: 'Fira Code', ui-monospace, monospace;
    font-size: 17px;
    font-weight: 700;
    line-height: 19px;
    color: #202124;
  }

  span {
    font-family: 'Fira Code', ui-monospace, monospace;
    font-size: 8px;
    line-height: 11px;
    color: #9aa0a6;
  }
}

html.dark .date-block {
  background: #1a1b1d;
  border-color: #2a2b2e;

  strong {
    color: #e3e3e3;
  }
}

.card-title {
  margin: 14px 0 0;
  font-size: 15px;
  font-weight: 650;
  line-height: 22px;
  color: #202124;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
}

html.dark .card-title {
  color: #e3e3e3;
}

.card-excerpt {
  margin: 7px 0 0;
  font-size: 12px;
  line-height: 19px;
  color: #5f6368;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

html.dark .card-excerpt {
  color: #9aa0a6;
}

.card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: auto;
  padding-top: 16px;
  font-size: 11px;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #80868b;
}

html.dark .meta-item {
  color: #9aa0a6;
}

.view-link {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
  font-size: 11px;
  font-weight: 600;
  color: #1a73e8;
}

html.dark .view-link {
  color: #a8c7fa;
}

.announcement-card:hover .view-link :deep(svg) {
  transform: translateX(2px);
}

.view-link :deep(svg) {
  transition: transform 0.18s ease;
}

/* ------------------------------------------------------------------ */
/* Category pill                                                       */
/* ------------------------------------------------------------------ */

.category-pill {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 9999px;
  font-size: 10px;
  font-weight: 600;
  line-height: 17px;
  border: 1px solid transparent;

  &.is-system {
    color: #1a73e8;
    background: rgba(26, 115, 232, 0.08);
    border-color: rgba(26, 115, 232, 0.18);
  }

  &.is-promotion {
    color: #b06000;
    background: rgba(251, 188, 4, 0.13);
    border-color: rgba(251, 188, 4, 0.28);
  }
}

html.dark .category-pill {
  &.is-system {
    color: #a8c7fa;
    background: rgba(168, 199, 250, 0.1);
    border-color: rgba(168, 199, 250, 0.22);
  }

  &.is-promotion {
    color: #fdd663;
    background: rgba(253, 214, 99, 0.11);
    border-color: rgba(253, 214, 99, 0.24);
  }
}

/* ------------------------------------------------------------------ */
/* Skeleton                                                            */
/* ------------------------------------------------------------------ */

.skeleton-featured {
  display: flex;
  flex-direction: column;
  min-height: 208px;
  padding: 28px 30px;
  border-radius: 22px;
  background: #fff;
  border: 1px solid #e5e8ec;
}

html.dark .skeleton-featured {
  background: #0e0e0e;
  border-color: #2a2b2e;
}

.skeleton-card {
  display: flex;
  flex-direction: column;
  min-height: 208px;
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
  animation: announcement-shimmer 1.4s ease infinite;
}

html.dark .sk {
  background: linear-gradient(90deg, #1e1f20 25%, #2a2b2e 37%, #1e1f20 63%);
  background-size: 400% 100%;
}

.sk-badge {
  width: 58px;
  height: 20px;
  border-radius: 9999px;
}

.sk-title {
  width: 62%;
  height: 18px;
  margin-top: 18px;
}

.sk-card-title {
  width: 84%;
  height: 13px;
  margin-top: 16px;
}

.sk-line {
  width: 94%;
  height: 10px;
  margin-top: 12px;
}

.sk-line.is-short {
  width: 58%;
  margin-top: 8px;
}

.sk-date {
  width: 46px;
  height: 46px;
  border-radius: 12px;
}

.sk-meta {
  width: 82px;
  height: 10px;
}

.sk-meta.is-short {
  width: 56px;
}

@keyframes announcement-shimmer {
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
  padding: 64px 24px 60px;
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
  max-width: 380px;
  margin: 7px 0 0;
  font-size: 12px;
  line-height: 19px;
  color: #80868b;
}

html.dark .empty-desc {
  color: #9aa0a6;
}

/* ------------------------------------------------------------------ */
/* Detail modal                                                        */
/* ------------------------------------------------------------------ */

.detail-date {
  font-family: 'Fira Code', ui-monospace, monospace;
  font-size: 10px;
  color: #80868b;
}

html.dark .detail-date {
  color: #9aa0a6;
}

.detail-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  line-height: 29px;
  letter-spacing: -0.01em;
  color: #202124;
}

html.dark .detail-title {
  color: #e8eaed;
}

.detail-body {
  max-height: 66vh;
  overflow-y: auto;
  padding-right: 4px;
}

.detail-cover {
  margin-bottom: 18px;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid #eceef1;

  img {
    display: block;
    width: 100%;
    max-height: 280px;
    object-fit: cover;
  }
}

html.dark .detail-cover {
  border-color: #2a2b2e;
}

.detail-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 14px;
  padding-bottom: 16px;
  margin-bottom: 18px;
  border-bottom: 1px solid #f0f2f4;
  font-size: 11px;
}

html.dark .detail-meta {
  border-bottom-color: #26272a;
}

.detail-prose {
  font-size: 13px;
  line-height: 1.8;
  color: #3c4043;
}

html.dark .detail-prose {
  color: #bdc1c6;
}

.detail-prose :deep(h1),
.detail-prose :deep(h2),
.detail-prose :deep(h3),
.detail-prose :deep(h4) {
  margin-top: 1.2em;
  margin-bottom: 0.5em;
  font-weight: 650;
  line-height: 1.35;
  color: #202124;
}

html.dark .detail-prose :deep(h1),
html.dark .detail-prose :deep(h2),
html.dark .detail-prose :deep(h3),
html.dark .detail-prose :deep(h4) {
  color: #e8eaed;
}

.detail-prose :deep(p) {
  margin: 0.6em 0;
}

.detail-prose :deep(ul),
.detail-prose :deep(ol) {
  margin: 0.6em 0;
  padding-left: 1.5rem;
}

.detail-prose :deep(code) {
  font-family: 'Fira Code', ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  padding: 0.15rem 0.35rem;
  border-radius: 0.3rem;
  font-size: 0.86em;
  background-color: rgba(60, 64, 67, 0.07);
}

.detail-prose :deep(pre) {
  padding: 0.85rem 1rem;
  border-radius: 0.6rem;
  overflow-x: auto;
  background-color: #f6f8fa;
  border: 1px solid #eceef1;
}

html.dark .detail-prose :deep(code) {
  background-color: rgba(255, 255, 255, 0.08);
}

html.dark .detail-prose :deep(pre) {
  background-color: #17181a;
  border-color: #2a2b2e;
}

.detail-prose :deep(blockquote) {
  margin: 0.8em 0;
  padding: 0.4em 1em;
  border-left: 3px solid rgba(26, 115, 232, 0.4);
  color: #5f6368;
  background: rgba(26, 115, 232, 0.04);
  border-radius: 0 8px 8px 0;
}

html.dark .detail-prose :deep(blockquote) {
  color: #9aa0a6;
  background: rgba(168, 199, 250, 0.06);
}

.detail-prose :deep(img) {
  max-width: 100%;
  border-radius: 12px;
}

.detail-empty {
  padding: 28px 0;
  text-align: center;
  font-size: 12px;
  color: #80868b;
}

.detail-link {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  margin-top: 4px;
  padding: 9px 16px;
  border-radius: 12px;
  border: 1px solid #e5e8ec;
  background: #fff;
  font-size: 12px;
  font-weight: 600;
  color: #1a73e8;
  text-decoration: none;
  transition: background-color 0.15s ease, border-color 0.15s ease;

  &:hover {
    background: #f6f9ff;
    border-color: rgba(26, 115, 232, 0.3);
  }
}

html.dark .detail-link {
  background: #161719;
  border-color: #2a2b2e;
  color: #a8c7fa;

  &:hover {
    background: #1c1d20;
    border-color: rgba(168, 199, 250, 0.3);
  }
}
</style>
