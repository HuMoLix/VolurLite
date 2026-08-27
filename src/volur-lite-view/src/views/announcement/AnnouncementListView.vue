<template>
  <div class="announcement-view">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">{{ t('layout.announcements') }}</h2>
        <p class="page-desc">查看平台最新发布的系统维护升级、重要通知与赛事活动公告</p>
      </div>
      <div class="header-right">
        <n-button quaternary circle @click="fetchAnnouncements">
          <template #icon>
            <n-icon :component="RefreshOutline" />
          </template>
        </n-button>
      </div>
    </div>

    <!-- Search & Filter Controls -->
    <n-card class="filter-card" :bordered="false">
      <div class="filter-bar">
        <div class="filter-inputs">
          <n-input
            v-model:value="searchKeyword"
            placeholder="搜索公告标题 / 正文内容"
            clearable
            style="width: 260px;"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <n-icon :component="SearchOutline" />
            </template>
          </n-input>

          <n-select
            v-model:value="filterCategory"
            placeholder="消息分类"
            clearable
            :options="categoryOptions"
            style="width: 140px;"
            @update:value="handleSearch"
          />

          <n-select
            v-model:value="filterDisplayType"
            placeholder="展示形式"
            clearable
            :options="displayTypeOptions"
            style="width: 140px;"
            @update:value="handleSearch"
          />

          <n-button type="primary" class="theme-primary-btn" @click="handleSearch">
            <template #icon>
              <n-icon :component="SearchOutline" />
            </template>
            查询
          </n-button>

          <n-button quaternary @click="handleReset">
            重置
          </n-button>
        </div>
      </div>
    </n-card>

    <!-- Announcements List -->
    <div class="announcements-container" :class="{ 'is-loading': loading }">
      <div v-if="loading" class="loading-wrapper">
        <n-spin size="large" />
      </div>

      <div v-else-if="announcementList.length === 0" class="empty-wrapper">
        <n-empty description="暂无相关公告通知" />
      </div>

      <div v-else class="announcement-grid">
        <n-card
          v-for="item in announcementList"
          :key="item.id"
          class="announcement-card"
          hoverable
          :bordered="false"
          @click="openDetail(item)"
        >
          <div v-if="item.coverImageUrl" class="card-cover">
            <img :src="item.coverImageUrl" alt="cover" loading="lazy" />
          </div>

          <div class="card-body">
            <div class="card-meta">
              <n-tag
                :type="item.category === 'ANNOUNCEMENT' ? 'primary' : 'warning'"
                size="small"
                round
              >
                {{ item.category === 'ANNOUNCEMENT' ? '系统公告' : '运营推广' }}
              </n-tag>
              <n-tag size="small" round>
                {{ getDisplayTypeLabel(item.displayType) }}
              </n-tag>
              <span class="card-priority">P{{ item.priority ?? 0 }}</span>
              <span class="card-time">{{ formatTime(item.createdAt || item.publishTime) }}</span>
            </div>

            <h3 class="card-title" :title="item.title">{{ item.title }}</h3>

            <p class="card-snippet">{{ item.content }}</p>

            <div class="card-footer">
              <span class="card-publisher">发布人：{{ item.publisher || '系统管理员' }}</span>
              <n-button text type="primary" size="small" class="detail-btn">
                查看详情 →
              </n-button>
            </div>
          </div>
        </n-card>
      </div>

      <!-- Pagination -->
      <div v-if="pagination.itemCount > 0" class="pagination-wrapper">
        <n-pagination
          v-model:page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :item-count="pagination.itemCount"
          :page-sizes="[12, 24, 48]"
          show-size-picker
          @update:page="handlePageChange"
          @update:page-size="handlePageSizeChange"
        />
      </div>
    </div>

    <!-- Announcement Detail Modal -->
    <n-modal
      v-model:show="showDetailModal"
      preset="card"
      title="公告详情"
      style="width: 580px;"
    >
      <div v-if="selectedItem" class="detail-wrapper">
        <div class="detail-header">
          <h2 class="modal-title">{{ selectedItem.title }}</h2>
          <div class="modal-meta">
            <n-tag
              :type="selectedItem.category === 'ANNOUNCEMENT' ? 'primary' : 'warning'"
              size="small"
              round
            >
              {{ selectedItem.category === 'ANNOUNCEMENT' ? '系统公告' : '运营推广' }}
            </n-tag>
            <n-tag size="small" round>
              {{ getDisplayTypeLabel(selectedItem.displayType) }}
            </n-tag>
            <span class="modal-publisher">发布人：{{ selectedItem.publisher || '系统管理员' }}</span>
            <span class="modal-time">{{ formatTime(selectedItem.createdAt || selectedItem.publishTime) }}</span>
          </div>
        </div>

        <div v-if="selectedItem.coverImageUrl" class="modal-cover">
          <img :src="selectedItem.coverImageUrl" alt="cover" />
        </div>

        <div class="modal-content">
          {{ selectedItem.content }}
        </div>

        <div v-if="selectedItem.linkUrl" class="modal-link">
          <n-button
            type="primary"
            secondary
            block
            tag="a"
            :href="selectedItem.linkUrl"
            target="_blank"
            rel="noopener noreferrer"
          >
            前往关联详情链接 ↗
          </n-button>
        </div>
      </div>

      <template #footer>
        <div class="modal-actions">
          <n-button @click="showDetailModal = false">关闭</n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import {
  NCard,
  NIcon,
  NInput,
  NSelect,
  NButton,
  NEmpty,
  NSpin,
  NTag,
  NModal,
  NPagination,
  type SelectOption
} from 'naive-ui'
import { SearchOutline, RefreshOutline } from '@vicons/ionicons5'
import { useI18n } from 'vue-i18n'
import { getBroadcastListApi } from '@/api/notification'
import type { BroadcastMessageVO, AdminBroadcastPageRequestDTO } from '@/types/notification'

const { t } = useI18n()

const loading = ref(false)
const searchKeyword = ref('')
const filterCategory = ref<string | null>(null)
const filterDisplayType = ref<string | null>(null)
const announcementList = ref<BroadcastMessageVO[]>([])

const showDetailModal = ref(false)
const selectedItem = ref<BroadcastMessageVO | null>(null)

const pagination = reactive({
  page: 1,
  pageSize: 12,
  itemCount: 0
})

const categoryOptions: SelectOption[] = [
  { label: '全部分类', value: '' },
  { label: '系统公告', value: 'ANNOUNCEMENT' },
  { label: '运营推广', value: 'PROMOTION' }
]

const displayTypeOptions: SelectOption[] = [
  { label: '全部形式', value: '' },
  { label: '居中弹窗 (MODAL)', value: 'MODAL' },
  { label: '顶部横幅 (BANNER)', value: 'BANNER' },
  { label: '浮层提示 (TOAST)', value: 'TOAST' },
  { label: '侧边抽屉 (DRAWER)', value: 'DRAWER' }
]

function formatTime(timeStr?: string) {
  if (!timeStr) return '-'
  const date = new Date(timeStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function getDisplayTypeLabel(type?: string) {
  const map: Record<string, string> = {
    MODAL: '居中弹窗',
    BANNER: '顶部横幅',
    TOAST: '浮层提示',
    DRAWER: '侧边抽屉'
  }
  return (type && map[type]) || type || '默认'
}

async function fetchAnnouncements() {
  loading.value = true
  try {
    const params: AdminBroadcastPageRequestDTO = {
      page: pagination.page,
      size: pagination.pageSize,
      keyword: searchKeyword.value.trim() || undefined,
      category: filterCategory.value || undefined,
      displayType: filterDisplayType.value || undefined,
      sortBy: 'createdAt',
      sortOrder: 'desc'
    }
    const res = await getBroadcastListApi(params)
    if (res.data) {
      announcementList.value = res.data.list || []
      pagination.itemCount = res.data.total || 0
    }
  } catch (error) {
    console.error('Failed to fetch announcements:', error)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchAnnouncements()
}

function handleReset() {
  searchKeyword.value = ''
  filterCategory.value = null
  filterDisplayType.value = null
  pagination.page = 1
  fetchAnnouncements()
}

function handlePageChange(page: number) {
  pagination.page = page
  fetchAnnouncements()
}

function handlePageSizeChange(pageSize: number) {
  pagination.pageSize = pageSize
  pagination.page = 1
  fetchAnnouncements()
}

function openDetail(item: BroadcastMessageVO) {
  selectedItem.value = item
  showDetailModal.value = true
}

onMounted(() => {
  fetchAnnouncements()
})
</script>

<style scoped lang="scss">
.announcement-view {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;

  .page-title {
    font-size: 22px;
    font-weight: 700;
    margin: 0;
    color: var(--v-text-primary, #111111);
  }

  .page-desc {
    font-size: 13px;
    color: #444444;
    margin-top: 4px;
  }
}

.filter-card {
  border-radius: 8px;
  background-color: var(--n-color, #ffffff);

  .filter-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 12px;

    .filter-inputs {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 12px;
    }
  }
}

.theme-primary-btn {
  background-color: #000000;
  color: #ffffff;
  font-weight: 500;
  border-radius: 6px;

  &:hover {
    background-color: #222222;
  }
}

.announcements-container {
  min-height: 300px;
  display: flex;
  flex-direction: column;
  gap: 20px;

  .loading-wrapper,
  .empty-wrapper {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 80px 0;
  }

  .announcement-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
    gap: 16px;

    .announcement-card {
      border-radius: 10px;
      background-color: var(--n-color, #ffffff);
      cursor: pointer;
      display: flex;
      flex-direction: column;
      overflow: hidden;
      border: 1px solid rgba(0, 0, 0, 0.05);
      transition: transform 0.2s ease, box-shadow 0.2s ease;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 14px rgba(0, 0, 0, 0.06);
      }

      .card-cover {
        width: 100%;
        height: 140px;
        overflow: hidden;

        img {
          width: 100%;
          height: 100%;
          object-fit: cover;
          transition: transform 0.3s ease;
        }

        &:hover img {
          transform: scale(1.03);
        }
      }

      .card-body {
        padding: 16px;
        display: flex;
        flex-direction: column;
        gap: 10px;
        flex: 1;

        .card-meta {
          display: flex;
          align-items: center;
          gap: 8px;
          flex-wrap: wrap;

          .card-priority {
            font-family: 'Fira Code', monospace;
            font-weight: 600;
            font-size: 12px;
            color: #6366f1;
          }

          .card-time {
            font-size: 12px;
            color: #888888;
            margin-left: auto;
          }
        }

        .card-title {
          font-size: 16px;
          font-weight: 600;
          color: var(--v-text-primary, #111111);
          margin: 0;
          line-height: 1.4;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .card-snippet {
          font-size: 13px;
          color: #444444;
          line-height: 1.5;
          margin: 0;
          display: -webkit-box;
          -webkit-line-clamp: 3;
          -webkit-box-orient: vertical;
          overflow: hidden;
          flex: 1;
        }

        .card-footer {
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding-top: 10px;
          border-top: 1px solid rgba(0, 0, 0, 0.04);
          font-size: 12px;

          .card-publisher {
            color: #777777;
          }

          .detail-btn {
            font-weight: 500;
          }
        }
      }
    }
  }

  .pagination-wrapper {
    display: flex;
    justify-content: center;
    margin-top: 12px;
  }
}

.detail-wrapper {
  display: flex;
  flex-direction: column;
  gap: 14px;

  .detail-header {
    display: flex;
    flex-direction: column;
    gap: 8px;

    .modal-title {
      font-size: 20px;
      font-weight: 700;
      color: var(--v-text-primary, #111111);
      margin: 0;
    }

    .modal-meta {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 8px;
      font-size: 12px;
      color: #888888;
    }
  }

  .modal-cover {
    img {
      width: 100%;
      max-height: 240px;
      object-fit: cover;
      border-radius: 6px;
    }
  }

  .modal-content {
    font-size: 14px;
    color: #444444;
    line-height: 1.7;
    white-space: pre-wrap;
    background: rgba(0, 0, 0, 0.02);
    padding: 14px;
    border-radius: 6px;
  }

  .modal-link {
    margin-top: 4px;
  }
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
}
</style>
