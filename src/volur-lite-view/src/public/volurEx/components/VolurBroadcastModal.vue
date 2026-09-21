<template>
  <n-modal
    v-model:show="notificationStore.showBroadcastModal"
    preset="card"
    class="volur-broadcast-modal"
    :mask-closable="!isForcePopup"
    :closable="!isForcePopup"
    :style="{ width: '540px', maxWidth: '92vw' }"
    @close="handleClose"
  >
    <template #header>
      <div class="modal-header-container">
        <div class="header-tags">
          <n-tag
            :type="isAnnouncement ? 'primary' : 'warning'"
            size="small"
            round
          >
            <template #icon>
              <n-icon :component="isAnnouncement ? MegaphoneOutline : SparklesOutline" />
            </template>
            {{ isAnnouncement ? t('layout.broadcast_category_announcement') : t('layout.broadcast_category_promotion') }}
          </n-tag>

          <n-tag
            v-if="priority >= 7"
            type="error"
            size="small"
            round
          >
            {{ t('layout.broadcast_urgent_tag') }} P{{ priority }}
          </n-tag>

          <n-tag
            v-else-if="priority > 0"
            size="small"
            round
          >
            P{{ priority }}
          </n-tag>
        </div>

        <h3 class="broadcast-title">{{ broadcast?.title || t('layout.broadcast_modal_title') }}</h3>

        <div class="broadcast-meta">
          <span v-if="broadcast?.publisher" class="meta-item">
            {{ t('layout.broadcast_publisher_prefix') }}{{ broadcast.publisher }}
          </span>
          <span v-if="formattedTime" class="meta-item">
            {{ formattedTime }}
          </span>
        </div>
      </div>
    </template>

    <div v-if="broadcast" class="modal-body-container">
      <!-- Optional Cover Image -->
      <div v-if="broadcast.coverImageUrl" class="cover-image-wrapper">
        <img :src="broadcast.coverImageUrl" alt="cover" class="cover-image" />
      </div>

      <!-- Main Body Text -->
      <div class="broadcast-content">
        {{ broadcast.content }}
      </div>

      <!-- Optional Target Link Card -->
      <div v-if="broadcast.linkUrl" class="link-card" @click="handleOpenLink">
        <n-icon size="16" :component="OpenOutline" class="link-icon" />
        <span class="link-url" :title="broadcast.linkUrl">{{ broadcast.linkUrl }}</span>
      </div>
    </div>

    <template #footer>
      <div class="modal-footer">
        <n-button
          v-if="broadcast?.linkUrl"
          secondary
          class="link-action-btn"
          @click="handleOpenLink"
        >
          <template #icon>
            <n-icon :component="OpenOutline" />
          </template>
          {{ t('layout.broadcast_view_link') }}
        </n-button>

        <n-button
          type="primary"
          class="confirm-action-btn"
          @click="handleClose"
        >
          {{ t('layout.broadcast_dismiss') }}
        </n-button>
      </div>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { NModal, NTag, NButton, NIcon } from 'naive-ui'
import {
  MegaphoneOutline,
  SparklesOutline,
  OpenOutline
} from '@vicons/ionicons5'
import { useNotificationStore } from '@/store/modules/notification'

defineOptions({
  name: 'VolurBroadcastModal'
})

const { t, locale } = useI18n()
const notificationStore = useNotificationStore()

const broadcast = computed(() => notificationStore.currentBroadcast)

const isAnnouncement = computed(() => broadcast.value?.category !== 'PROMOTION')
const isForcePopup = computed(() => !!broadcast.value?.forcePopup)
const priority = computed(() => broadcast.value?.priority ?? 0)

const formattedTime = computed(() => {
  const timeStr = broadcast.value?.publishTime || broadcast.value?.createdAt
  if (!timeStr) return ''
  try {
    const date = new Date(timeStr)
    return date.toLocaleString(locale.value === 'zh_CN' ? 'zh-CN' : 'en-US', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch {
    return ''
  }
})

function handleOpenLink() {
  if (broadcast.value?.linkUrl) {
    window.open(broadcast.value.linkUrl, '_blank', 'noopener,noreferrer')
  }
}

function handleClose() {
  notificationStore.closeBroadcastModal()
}
</script>

<style scoped lang="scss">
.volur-broadcast-modal {
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.18);

  :deep(.n-card-header) {
    padding: 20px 24px 12px;
  }

  :deep(.n-card__content) {
    padding: 12px 24px 20px;
  }

  :deep(.n-card__footer) {
    padding: 12px 24px 18px;
    border-top: 1px solid var(--n-border-color);
  }
}

.modal-header-container {
  display: flex;
  flex-direction: column;
  gap: 8px;

  .header-tags {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .broadcast-title {
    margin: 4px 0 0 0;
    font-size: 1.18rem;
    font-weight: 700;
    line-height: 1.4;
    color: var(--n-title-text-color, inherit);
    word-break: break-word;
  }

  .broadcast-meta {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 0.82rem;
    color: #888888;

    .meta-item {
      display: inline-flex;
      align-items: center;
    }
  }
}

.modal-body-container {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .cover-image-wrapper {
    width: 100%;
    max-height: 200px;
    border-radius: 8px;
    overflow: hidden;
    background-color: rgba(0, 0, 0, 0.04);

    .cover-image {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
    }
  }

  .broadcast-content {
    font-size: 0.94rem;
    line-height: 1.65;
    color: #444444;
    white-space: pre-wrap;
    word-break: break-word;
    max-height: 420px;
    overflow-y: auto;
    padding-right: 4px;
  }

  .link-card {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 14px;
    border-radius: 6px;
    background-color: rgba(0, 0, 0, 0.03);
    border: 1px dashed rgba(0, 0, 0, 0.12);
    cursor: pointer;
    transition: all 0.2s ease;

    .link-icon {
      color: #666666;
      flex-shrink: 0;
    }

    .link-url {
      font-size: 0.85rem;
      color: #2563eb;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      text-decoration: underline;
    }

    &:hover {
      background-color: rgba(0, 0, 0, 0.06);
      border-color: #2563eb;
    }
  }
}

.modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;

  .confirm-action-btn {
    min-width: 90px;
    font-weight: 500;
  }

  .link-action-btn {
    font-weight: 500;
  }
}

/* Dark Theme Overrides */
:global(body.dark), :global([data-theme='dark']) {
  .volur-broadcast-modal {
    .broadcast-content {
      color: #cbd5e1;
    }

    .broadcast-meta {
      color: #94a3b8;
    }

    .modal-body-container .link-card {
      background-color: rgba(255, 255, 255, 0.05);
      border-color: rgba(255, 255, 255, 0.15);

      .link-url {
        color: #60a5fa;
      }

      &:hover {
        background-color: rgba(255, 255, 255, 0.08);
        border-color: #60a5fa;
      }
    }
  }
}
</style>
