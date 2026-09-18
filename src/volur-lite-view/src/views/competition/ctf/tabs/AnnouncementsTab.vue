<template>
  <div class="announcements-tab">
    <n-card :bordered="false" class="announcements-card">
      <div class="announcements-header">
        <div class="title-wrap">
          <h3>📢 比赛公告 (Announcements)</h3>
          <p class="sub">实时发布关于赛题变更、规则通报及重要赛况通知。</p>
        </div>
        <n-button quaternary size="small" :loading="announcementsLoading" @click="fetchAnnouncements">
          <template #icon>
            <n-icon :component="SyncOutline" />
          </template>
          刷新公告
        </n-button>
      </div>

      <n-spin :show="announcementsLoading">
        <div v-if="announcements.length > 0" class="announcements-list">
          <div
            v-for="item in announcements"
            :key="item.id"
            class="announcement-item"
            :class="{ 'is-pinned': item.isPinned }"
          >
            <div class="item-header">
              <div class="item-title-group">
                <n-tag v-if="item.isPinned" size="small" type="error" round :bordered="false" class="pin-tag">
                  置顶
                </n-tag>
                <h4 class="item-title">{{ item.title }}</h4>
              </div>
              <span class="item-time">{{ formatTime(item.createdAt) }}</span>
            </div>
            <div class="item-content markdown-body" v-html="renderMarkdown(item.content)" />
            <div v-if="item.createdByName" class="item-footer">
              <span class="author-label">发布人: {{ item.createdByName }}</span>
            </div>
          </div>
        </div>
        <div v-else class="empty-wrap">
          <n-empty description="当前比赛暂无公告" />
        </div>
      </n-spin>
    </n-card>
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: 'AnnouncementsTab'
})

import { ref, computed, watch } from 'vue'
import {
  NCard,
  NTag,
  NButton,
  NEmpty,
  NSpin,
  NIcon
} from 'naive-ui'
import { SyncOutline } from '@vicons/ionicons5'
import { renderMarkdown } from '@/utils/markdown'
import { useCompetitionStore } from '@/store/modules/competition'
import { listCompetitionAnnouncementsApi } from '@/api/competition'
import type { CompetitionAnnouncementVO } from '@/types/competition'

interface Props {
  competitionId?: number | string | null
}

const props = withDefaults(defineProps<Props>(), {
  competitionId: null
})

const competitionStore = useCompetitionStore()
const currentCompId = computed(() => props.competitionId ?? competitionStore.currentCompetition?.id)

const announcementsLoading = ref<boolean>(false)
const announcements = ref<CompetitionAnnouncementVO[]>([])

function formatTime(isoStr?: string | null): string {
  if (!isoStr) return '-'
  try {
    const d = new Date(isoStr)
    return d.toLocaleString()
  } catch {
    return isoStr
  }
}

async function fetchAnnouncements() {
  if (!currentCompId.value) return
  announcementsLoading.value = true
  try {
    const res = await listCompetitionAnnouncementsApi(currentCompId.value)
    if (res.data) {
      announcements.value = res.data
    }
  } catch (err) {
    console.warn('Failed to fetch competition announcements:', err)
  } finally {
    announcementsLoading.value = false
  }
}

watch(
  () => currentCompId.value,
  (newId) => {
    if (newId) {
      fetchAnnouncements()
    }
  },
  { immediate: true }
)

defineExpose({
  fetchAnnouncements
})
</script>

<style scoped lang="scss">
.announcements-tab {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.announcements-card {
  border-radius: 12px;

  .announcements-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    .title-wrap {
      h3 {
        font-size: 18px;
        font-weight: 700;
        margin: 0 0 4px;
      }

      .sub {
        font-size: 13px;
        color: #444444;
        margin: 0;
      }
    }
  }

  .announcements-list {
    display: flex;
    flex-direction: column;
    gap: 16px;

    .announcement-item {
      background: var(--v-bg-muted, rgba(0, 0, 0, 0.02));
      border: 1px solid var(--v-border-subtle, rgba(0, 0, 0, 0.08));
      border-radius: 10px;
      padding: 16px 20px;
      transition: all 0.2s ease;

      &.is-pinned {
        border-left: 4px solid #d03050;
        background: rgba(208, 48, 80, 0.02);
      }

      .item-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 10px;

        .item-title-group {
          display: flex;
          align-items: center;
          gap: 8px;

          .item-title {
            margin: 0;
            font-size: 16px;
            font-weight: 600;
            color: var(--v-text-primary, #111111);
          }
        }

        .item-time {
          font-size: 12px;
          color: var(--v-text-desc, #888888);
          font-family: var(--v-font-mono, monospace);
        }
      }

      .item-content {
        font-size: 14px;
        line-height: 1.6;
        color: var(--v-text-primary, #333333);
        word-break: break-word;
      }

      .item-footer {
        margin-top: 10px;
        padding-top: 8px;
        border-top: 1px dashed var(--v-border-subtle, rgba(0, 0, 0, 0.06));
        font-size: 12px;
        color: var(--v-text-desc, #666666);
      }
    }
  }
}

.empty-wrap {
  padding: 60px 0;
  display: flex;
  justify-content: center;
}
</style>
