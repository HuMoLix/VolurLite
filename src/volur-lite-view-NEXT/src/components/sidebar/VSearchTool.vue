<template>
  <Teleport to="body">
    <Transition name="v-search-modal">
      <div
        v-if="isVisible"
        class="v-search-backdrop"
        :class="[currentTheme, { dark: currentTheme === 'dark' }]"
        @click="handleBackdropClick"
      >
        <div
          class="v-search-dialog"
          role="dialog"
          aria-modal="true"
          @click.stop
        >
          <div class="search-header">
            <button
              type="button"
              class="search-action-btn search-trigger-icon"
              :aria-label="t('common.search', '搜索')"
              @click="handleSearchIconClick"
            >
              <n-icon :size="20">
                <SearchOutline />
              </n-icon>
            </button>

            <input
              ref="inputRef"
              v-model="keyword"
              type="text"
              class="search-native-input"
              :placeholder="computedPlaceholder"
              @input="handleInput"
              @keydown.enter="handleEnterSearch"
            />

            <div class="search-header-actions">
              <button
                v-if="keyword"
                type="button"
                class="search-action-btn clear-btn"
                :aria-label="t('common.clear', '清空')"
                @click="handleClear"
              >
                <n-icon :size="18">
                  <CloseCircleOutline />
                </n-icon>
              </button>

              <button
                type="button"
                class="search-action-btn close-btn"
                :aria-label="t('common.close', '关闭')"
                @click="close"
              >
                <n-icon :size="18">
                  <CloseOutline />
                </n-icon>
              </button>
            </div>
          </div>

          <template v-if="loading || hasSearched || results.length > 0">
            <div class="search-divider"></div>

            <div class="search-body">
              <div v-if="loading" class="search-status-box">
                <n-spin size="medium" />
                <span class="status-tip-text">{{ t('common.loading', '加载中...') }}</span>
              </div>

              <div v-else-if="hasSearched && results.length === 0" class="search-status-box">
                <div class="empty-icon-wrap">
                  <n-icon :size="36">
                    <SearchOutline />
                  </n-icon>
                </div>
                <span class="empty-primary-title">{{ t('competition.empty_desc', '暂无符合条件的竞赛') }}</span>
                <span class="empty-sub-tip">{{ t('competition.empty_tip', '尝试更换关键词或清除筛选条件') }}</span>
              </div>

              <div v-else-if="results.length > 0" class="search-result-container">
                <div class="result-list-header">
                  <span>{{ t('common.search_results', '搜索结果') }} ({{ results.length }})</span>
                </div>
                <div class="result-item-list">
                  <div
                    v-for="item in results"
                    :key="item.id"
                    class="search-result-row"
                    @click="handleSelect(item)"
                  >
                    <div class="result-type-badge" :class="item.type ? item.type.toLowerCase() : 'ctf'">
                      {{ getTypeLabel(item.type) }}
                    </div>

                    <div class="result-info-box">
                      <div class="result-title-row">
                        <span class="result-title-text">{{ item.title }}</span>
                        <span
                          v-if="item.status"
                          class="result-status-tag"
                          :class="getStatusBadgeClass(item.status)"
                        >
                          {{ getStatusLabel(item.status) }}
                        </span>
                      </div>
                      <p v-if="item.description" class="result-desc-text">
                        {{ item.description }}
                      </p>
                    </div>

                    <div class="result-action-icon">
                      <n-icon :size="18">
                        <ArrowForwardOutline />
                      </n-icon>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { NIcon, NSpin } from 'naive-ui'
import {
  SearchOutline,
  CloseOutline,
  CloseCircleOutline,
  ArrowForwardOutline
} from '@vicons/ionicons5'
import { useAppStore } from '@/store/modules/app'
import { loadLocaleMessagesAsync } from '@/i18n'
import { getCompetitionListApi } from '@/api/competition'
import type {
  CompetitionSummaryVO,
  CompetitionStatus,
  CompetitionType
} from '@/types/competition'
import type { VSearchToolProps } from './types'

defineOptions({
  name: 'VSearchTool'
})

const props = withDefaults(defineProps<VSearchToolProps>(), {
  show: undefined,
  placeholder: '',
  debounceDelay: 3000,
  searchApi: undefined,
  theme: undefined
})

const emit = defineEmits<{
  'update:show': [val: boolean]
  search: [keyword: string]
  select: [item: CompetitionSummaryVO]
  close: []
}>()

const showModel = defineModel<boolean>('show', { default: false })

const router = useRouter()
const { t } = useI18n()
const appStore = useAppStore()

const inputRef = ref<HTMLInputElement | null>(null)
const keyword = ref('')
const loading = ref(false)
const hasSearched = ref(false)
const results = ref<CompetitionSummaryVO[]>([])

let debounceTimer: ReturnType<typeof setTimeout> | null = null

const isVisible = computed<boolean>({
  get() {
    if (showModel.value !== undefined) {
      return showModel.value
    }
    return props.show ?? false
  },
  set(val: boolean) {
    if (showModel.value !== undefined) {
      showModel.value = val
    }
    emit('update:show', val)
    if (!val) {
      emit('close')
    }
  }
})

const currentTheme = computed<'light' | 'dark'>(() => {
  return props.theme || (appStore.theme === 'dark' ? 'dark' : 'light')
})

const computedPlaceholder = computed<string>(() => {
  return props.placeholder || t('common.search_placeholder', '搜索...')
})

function clearDebounceTimer() {
  if (debounceTimer) {
    clearTimeout(debounceTimer)
    debounceTimer = null
  }
}

function handleInput() {
  clearDebounceTimer()
  const trimmed = keyword.value.trim()
  if (!trimmed) {
    results.value = []
    hasSearched.value = false
    loading.value = false
    return
  }

  debounceTimer = setTimeout(() => {
    executeSearch()
  }, props.debounceDelay)
}

function handleEnterSearch() {
  clearDebounceTimer()
  executeSearch()
}

function handleSearchIconClick() {
  clearDebounceTimer()
  executeSearch()
}

function handleClear() {
  keyword.value = ''
  clearDebounceTimer()
  results.value = []
  hasSearched.value = false
  loading.value = false
  nextTick(() => {
    inputRef.value?.focus()
  })
}

async function executeSearch() {
  clearDebounceTimer()
  const trimmed = keyword.value.trim()
  if (!trimmed) {
    results.value = []
    hasSearched.value = false
    loading.value = false
    return
  }

  loading.value = true
  hasSearched.value = true
  emit('search', trimmed)

  try {
    if (props.searchApi) {
      const res = await props.searchApi(trimmed)
      if (res && res.data && Array.isArray(res.data.list)) {
        results.value = res.data.list
      } else if (res && Array.isArray(res.data)) {
        results.value = res.data
      } else if (Array.isArray(res)) {
        results.value = res
      } else {
        results.value = []
      }
    } else {
      const res = await getCompetitionListApi({
        keyword: trimmed,
        page: 1,
        size: 8
      })
      if (res && res.data && Array.isArray(res.data.list)) {
        results.value = res.data.list
      } else {
        results.value = []
      }
    }
  } catch {
    results.value = []
  } finally {
    loading.value = false
  }
}

function handleSelect(item: CompetitionSummaryVO) {
  close()
  emit('select', item)
  if (item.slug) {
    router.push(`/c/${item.slug}`).catch(() => {})
  } else if (item.id) {
    router.push(`/competitions/${item.id}`).catch(() => {})
  }
}

function close() {
  clearDebounceTimer()
  isVisible.value = false
}

function handleBackdropClick(ev: MouseEvent) {
  if (ev.target === ev.currentTarget) {
    close()
  }
}

function handleGlobalKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape' && isVisible.value) {
    close()
  }
}

function getStatusBadgeClass(status?: CompetitionStatus | null): string {
  switch (status) {
    case 'RUNNING':
      return 'badge-running'
    case 'REGISTRATION':
    case 'PUBLISHED':
      return 'badge-registration'
    case 'FROZEN':
      return 'badge-frozen'
    case 'ENDED':
    case 'ARCHIVED':
    default:
      return 'badge-ended'
  }
}

function getStatusLabel(status?: CompetitionStatus | null): string {
  if (!status) return '-'
  switch (status) {
    case 'DRAFT':
      return t('competition.status_draft', '草稿')
    case 'PUBLISHED':
      return t('competition.status_published', '已发布')
    case 'REGISTRATION':
      return t('competition.status_registration', '报名中')
    case 'RUNNING':
      return t('competition.status_running', '进行中')
    case 'FROZEN':
      return t('competition.status_frozen', '已封榜')
    case 'ENDED':
      return t('competition.status_ended', '已结束')
    case 'ARCHIVED':
      return t('competition.status_archived', '已归档')
    case 'HIDDEN':
      return t('competition.status_hidden', '隐藏')
    case 'PENDING' as CompetitionStatus:
      return t('competition.status_pending', '未开始')
    default:
      return status
  }
}

function getTypeLabel(type?: CompetitionType | null): string {
  if (!type) return 'VOLUR'
  switch (type) {
    case 'CTF':
      return t('competition.type_ctf', 'CTF 解题赛')
    case 'THEORY':
      return t('competition.type_theory', '理论知识赛')
    case 'AWD':
      return t('competition.type_awd', 'AWD 攻防对抗')
    case 'AWD_PLUS':
      return t('competition.type_awd_plus', 'AWD+ 综合攻防')
    case 'ISW':
      return t('competition.type_isw', 'ISW 靶场竞技')
    default:
      return type
  }
}

async function ensureCompetitionLocale(): Promise<void> {
  if (appStore.locale !== 'zh-CN') {
    try {
      await loadLocaleMessagesAsync(appStore.locale, 'competition')
    } catch {}
  }
}

watch(
  isVisible,
  (val) => {
    if (val) {
      ensureCompetitionLocale()
      nextTick(() => {
        inputRef.value?.focus()
      })
    } else {
      clearDebounceTimer()
    }
  },
  { immediate: true }
)

watch(
  () => appStore.locale,
  () => {
    ensureCompetitionLocale()
  }
)

onMounted(() => {
  ensureCompetitionLocale()
  window.addEventListener('keydown', handleGlobalKeydown)
})

onBeforeUnmount(() => {
  clearDebounceTimer()
  window.removeEventListener('keydown', handleGlobalKeydown)
})

defineExpose({
  keyword,
  results,
  loading,
  executeSearch,
  close,
  clear: handleClear
})
</script>

<style scoped lang="scss">
.v-search-backdrop {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 16px;
  padding-top: clamp(60px, 14vh, 140px);
  backdrop-filter: blur(2px) saturate(180%);
  -webkit-backdrop-filter: blur(2px) saturate(180%);
  box-sizing: border-box;

  &.dark {
    backdrop-filter: blur(2px) saturate(180%);
    -webkit-backdrop-filter: blur(2px) saturate(180%);

    .v-search-dialog {
      background-color: #202124;
      border-color: #3c4043;
      box-shadow: 0 24px 48px -12px rgba(0, 0, 0, 0.6), 0 0 0 1px rgba(255, 255, 255, 0.08);

      .search-header {
        .search-trigger-icon {
          color: #a8c7fa;

          &:hover {
            background-color: rgba(168, 199, 250, 0.12);
          }
        }

        .search-native-input {
          color: #e3e3e3;

          &::placeholder {
            color: #9aa0a6;
          }
        }

        .search-action-btn {
          color: #9aa0a6;

          &:hover {
            color: #e3e3e3;
            background-color: rgba(255, 255, 255, 0.08);
          }
        }
      }

      .search-divider {
        background-color: #3c4043;
      }

      .search-body {
        .search-status-box {
          .status-tip-text {
            color: #bdc1c6;
          }

          .empty-icon-wrap {
            color: #9aa0a6;
          }

          .empty-primary-title {
            color: #e3e3e3;
          }

          .empty-sub-tip {
            color: #9aa0a6;
          }
        }

        .search-result-container {
          .result-list-header {
            color: #9aa0a6;
          }

          .search-result-row {
            &:hover {
              background-color: #303134;
            }

            .result-info-box {
              .result-title-text {
                color: #e3e3e3;
              }

              .result-desc-text {
                color: #bdc1c6;
              }
            }

            .result-action-icon {
              color: #9aa0a6;
            }
          }
        }
      }
    }
  }
}

.v-search-dialog {
  width: 100%;
  max-width: 640px;
  background-color: #ffffff;
  border: 1px solid #dadce0;
  border-radius: 32px;
  box-shadow: 0 24px 48px -12px rgba(0, 0, 0, 0.25), 0 0 0 1px rgba(0, 0, 0, 0.04);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  transition: transform 0.2s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.2s cubic-bezier(0.4, 0, 0.2, 1);

  .search-header {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 16px;
    height: 56px;
    box-sizing: border-box;

    .search-trigger-icon {
      color: #1a73e8;

      &:hover {
        background-color: rgba(26, 115, 232, 0.08);
      }
    }

    .search-native-input {
      flex: 1;
      height: 100%;
      border: none;
      outline: none;
      background: transparent;
      font-size: 16px;
      font-family: inherit;
      color: #202124;

      &::placeholder {
        color: #80868b;
      }
    }

    .search-header-actions {
      display: flex;
      align-items: center;
      gap: 6px;
    }

    .search-action-btn {
      width: 34px;
      height: 34px;
      border-radius: 50%;
      border: none;
      background: transparent;
      color: #5f6368;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      outline: none;
      transition: background-color 0.15s ease, color 0.15s ease;

      &:hover {
        color: #202124;
        background-color: rgba(60, 64, 67, 0.08);
      }
    }
  }

  .search-divider {
    height: 1px;
    background-color: #dadce0;
    width: 100%;
  }

  .search-body {
    max-height: 420px;
    min-height: 120px;
    overflow-y: auto;
    padding: 12px 16px;
    box-sizing: border-box;
    display: flex;
    flex-direction: column;

    .search-status-box {
      flex: 1;
      min-height: 140px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 12px;
      padding: 24px 16px;

      .status-tip-text {
        font-size: 13px;
        color: #5f6368;
      }

      .empty-icon-wrap {
        color: #80868b;
      }

      .empty-primary-title {
        font-size: 14px;
        font-weight: 500;
        color: #202124;
      }

      .empty-sub-tip {
        font-size: 12px;
        color: #80868b;
      }
    }

    .search-result-container {
      display: flex;
      flex-direction: column;
      gap: 8px;

      .result-list-header {
        font-size: 12px;
        font-weight: 600;
        color: #5f6368;
        padding: 4px 8px;
        text-transform: uppercase;
        letter-spacing: 0.5px;
      }

      .result-item-list {
        display: flex;
        flex-direction: column;
        gap: 4px;
      }

      .search-result-row {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 10px 12px;
        border-radius: 12px;
        cursor: pointer;
        transition: background-color 0.15s ease;

        &:hover {
          background-color: #f1f3f4;
        }

        .result-type-badge {
          padding: 4px 8px;
          border-radius: 8px;
          font-size: 11px;
          font-weight: 600;
          font-family: 'Fira Code', monospace;
          text-transform: uppercase;
          background-color: rgba(26, 115, 232, 0.1);
          color: #1a73e8;
          white-space: nowrap;
          flex-shrink: 0;

          &.theory {
            background-color: rgba(52, 168, 83, 0.1);
            color: #34a853;
          }

          &.awd {
            background-color: rgba(234, 67, 53, 0.1);
            color: #ea4335;
          }

          &.awd_plus {
            background-color: rgba(251, 188, 4, 0.15);
            color: #ea8600;
          }

          &.isw {
            background-color: rgba(36, 193, 224, 0.12);
            color: #0b7a8f;
          }
        }

        .result-info-box {
          flex: 1;
          min-width: 0;
          display: flex;
          flex-direction: column;
          gap: 2px;

          .result-title-row {
            display: flex;
            align-items: center;
            gap: 8px;

            .result-title-text {
              font-size: 14px;
              font-weight: 500;
              color: #202124;
              overflow: hidden;
              text-overflow: ellipsis;
              white-space: nowrap;
            }

            .result-status-tag {
              padding: 1px 6px;
              border-radius: 6px;
              font-size: 11px;
              font-weight: 500;

              &.badge-running {
                background-color: rgba(52, 168, 83, 0.12);
                color: #34a853;
              }

              &.badge-registration {
                background-color: rgba(26, 115, 232, 0.12);
                color: #1a73e8;
              }

              &.badge-frozen {
                background-color: rgba(251, 188, 4, 0.16);
                color: #ea8600;
              }

              &.badge-ended {
                background-color: rgba(95, 99, 104, 0.12);
                color: #5f6368;
              }
            }
          }

          .result-desc-text {
            font-size: 12px;
            color: #5f6368;
            margin: 0;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }
        }

        .result-action-icon {
          color: #80868b;
          display: flex;
          align-items: center;
          justify-content: center;
        }
      }
    }
  }
}

.v-search-modal-enter-active,
.v-search-modal-leave-active {
  transition: opacity 0.2s cubic-bezier(0.4, 0, 0.2, 1);

  .v-search-dialog {
    transition: transform 0.2s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  }
}

.v-search-modal-enter-from,
.v-search-modal-leave-to {
  opacity: 0;

  .v-search-dialog {
    transform: scale(0.96) translateY(-8px);
    opacity: 0;
  }
}
</style>
