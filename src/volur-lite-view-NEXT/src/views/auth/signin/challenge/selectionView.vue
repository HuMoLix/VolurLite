<template>
  <div class="flex flex-col justify-between h-full">
    <div>
      <p class="text-base text-google-text-primary dark:text-google-text-darkPrimary mb-5 font-normal leading-6">
        选择您想要使用的登录方式：
      </p>

      <div v-if="isFetchingSelection" class="py-12 flex justify-center">
        <n-spin size="medium" />
      </div>

      <div v-else class="auth-selection-list">
        <div
          v-for="item in currentSelectionSlice"
          :key="item.type"
          class="auth-selection-item"
          :isScrim="true"
          role="button"
          tabindex="0"
          @click="handleSelectMethod(item)"
          @keydown.enter.space.prevent="handleSelectMethod(item)"
        >
          <div class="auth-selection-item__left">
            <div class="auth-selection-item__icon">
              <svg
                v-if="item.icon === 'device' || item.type === 'pk'"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.75"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <rect x="5" y="2" width="14" height="20" rx="2" ry="2"/>
                <line x1="12" y1="18" x2="12.01" y2="18" stroke-width="2"/>
              </svg>
              <svg
                v-else-if="item.icon === 'email' || item.type === 'etotp'"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.75"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <rect x="3" y="5" width="18" height="14" rx="2"/>
                <polyline points="3 7 12 13 21 7"/>
              </svg>
              <svg
                v-else-if="item.icon === 'sms' || item.type === 'totp'"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.75"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                <line x1="8" y1="9" x2="16" y2="9"/>
                <line x1="8" y1="13" x2="13" y2="13"/>
              </svg>
              <svg
                v-else-if="item.icon === 'key' || item.type === 'pwd'"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.75"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <circle cx="7.5" cy="15.5" r="3.5"/>
                <path d="M10 13l9-9"/>
                <path d="M16 4l3 3"/>
                <path d="M14 6l2 2"/>
              </svg>
              <svg
                v-else
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.75"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                <polyline points="9 12 11 14 15 10"/>
              </svg>
            </div>
            <div class="auth-selection-item__content">
              <div class="auth-selection-item__title">{{ item.title }}</div>
            </div>
          </div>
        </div>

        <div
          class="auth-selection-item"
          :isScrim="true"
          role="button"
          tabindex="0"
          @click="handleTryAnotherWayInSelection"
          @keydown.enter.space.prevent="handleTryAnotherWayInSelection"
        >
          <div class="auth-selection-item__left">
            <div class="auth-selection-item__icon">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.75"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <circle cx="12" cy="12" r="9"/>
                <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/>
                <line x1="12" y1="17" x2="12.01" y2="17" stroke-width="2"/>
              </svg>
            </div>
            <div class="auth-selection-item__content">
              <div class="auth-selection-item__title">试试其他方式</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="flex items-center justify-end pt-6 sm:pt-8">
      <button
        type="button"
        class="auth-action-btn btn-try-another-way"
        :isScrim="true"
        @click="handleCancelSelection"
      >
        取消
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NSpin } from 'naive-ui'
import { fetchChallengeSelectionApi } from '@/api/auth'
import type { ChallengeOptionVO } from '@/types/auth/flow'
import { useSigninFlowStore } from '@/store/modules/flow'
import { useCardHeader } from '@/composables/useCardHeader'

defineOptions({
  name: 'SelectionView'
})

useCardHeader('2 步验证', '')

const route = useRoute()
const router = useRouter()
const signinFlowStore = useSigninFlowStore()

const availableMethods = ref<ChallengeOptionVO[]>([])
const isFetchingSelection = ref(false)
const pageSize = 2

const currentPage = computed(() => {
  const p = parseInt((route.query.page as string) || '1', 10)
  return isNaN(p) || p < 1 ? 1 : p
})

const totalPages = computed(() => {
  return Math.max(1, Math.ceil(availableMethods.value.length / pageSize))
})

const currentSelectionSlice = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return availableMethods.value.slice(start, start + pageSize)
})

watch(
  () => route.query.page,
  () => {
    loadSelectionMethods()
  }
)

onMounted(() => {
  loadSelectionMethods()
})

async function loadSelectionMethods() {
  if (isFetchingSelection.value) return
  isFetchingSelection.value = true
  try {
    const res = await fetchChallengeSelectionApi({
      dsh: signinFlowStore.dsh,
      currentType: (route.query.from as string) || 'pwd'
    })
    if (res && res.data && Array.isArray(res.data) && res.data.length > 0) {
      availableMethods.value = res.data
    } else {
      handleSelectionRejected()
    }
  } catch {
    handleSelectionRejected()
  } finally {
    isFetchingSelection.value = false
  }
}

function handleSelectionRejected() {
  const msg = '您提供的信息不足以让 Volur 认定您确实是这个账号的所有者。Google 需要使用这类信息来保证您账号的安全。'
  router.replace({
    path: '/v1/signin/challenge/rejected',
    query: signinFlowStore.getQueryParams({
      rejectedmsg: encodeURIComponent(msg)
    })
  })
}

async function handleSelectMethod(item: ChallengeOptionVO) {
  if (signinFlowStore.loading) return
  await signinFlowStore.triggerScrim(undefined, 1000)
  const targetPath = item.path || `/v1/signin/challenge/${item.type}`
  await router.push({
    path: targetPath,
    query: signinFlowStore.getQueryParams()
  })
}

async function handleTryAnotherWayInSelection() {
  if (signinFlowStore.loading) return
  await signinFlowStore.triggerScrim(undefined, 1000)
  if (currentPage.value < totalPages.value) {
    const nextPage = currentPage.value + 1
    await router.push({
      path: '/v1/signin/challenge/selection',
      query: signinFlowStore.getQueryParams({
        from: (route.query.from as string) || '',
        page: String(nextPage)
      })
    })
  } else {
    const msg = '您提供的信息不足以让 Volur 认定您确实是这个账号的所有者。Google 需要使用这类信息来保证您账号的安全。'
    await router.push({
      path: '/v1/signin/challenge/rejected',
      query: signinFlowStore.getQueryParams({
        rejectedmsg: encodeURIComponent(msg)
      })
    })
  }
}

async function handleCancelSelection() {
  if (signinFlowStore.loading) return
  await signinFlowStore.triggerScrim(undefined, 1000)
  const fromStep = (route.query.from as string) || 'pwd'
  await router.push({
    path: `/v1/signin/challenge/${fromStep}`,
    query: signinFlowStore.getQueryParams()
  })
}
</script>

<style scoped lang="scss">
.auth-action-btn {
  height: 36px !important;
  min-width: 88px !important;
  padding: 0 16px !important;
  border-radius: 9999px !important;
  font-size: 14px !important;
  font-weight: 500 !important;
  box-sizing: border-box;
}

.btn-try-another-way {
  color: #0b57d0;
  background-color: transparent;
  border: none;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  outline: none;
  user-select: none;

  &:hover {
    background-color: rgba(11, 87, 208, 0.08) !important;
    color: #0b57d0 !important;
  }

  &:active {
    background-color: rgba(11, 87, 208, 0.16) !important;
  }
}

:global(html.dark) .btn-try-another-way {
  color: #8ab4f8;

  &:hover {
    background-color: rgba(138, 180, 248, 0.12) !important;
    color: #8ab4f8 !important;
  }

  &:active {
    background-color: rgba(138, 180, 248, 0.2) !important;
  }
}

.auth-selection-list {
  border: none;
  border-radius: 0;
  overflow: visible;
  background-color: transparent;
}

:global(html.dark) .auth-selection-list {
  border: none;
}

.auth-selection-item {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  padding: 16px 8px;
  min-height: 56px;
  border: none;
  border-bottom: 1px solid var(--v-border-base, #dadce0);
  background-color: transparent;
  cursor: pointer;
  transition: background-color 0.15s cubic-bezier(0.4, 0, 0.2, 1);
  user-select: none;
  box-sizing: border-box;
  outline: none;

  &:hover {
    background-color: rgba(11, 87, 208, 0.08);
  }

  &:active {
    background-color: rgba(11, 87, 208, 0.16);
  }

  &:focus-visible {
    outline: 2px solid #0b57d0;
    outline-offset: -2px;
  }

  &__left {
    display: flex;
    align-items: center;
    gap: 16px;
    flex: 1;
    min-width: 0;
  }

  &__icon {
    width: 24px;
    height: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #0b57d0;
    flex-shrink: 0;

    svg {
      width: 24px;
      height: 24px;
    }
  }

  &__content {
    flex: 1;
    min-width: 0;
    display: flex;
    align-items: center;
  }

  &__title {
    font-size: 15px;
    font-weight: 500;
    color: var(--v-text-primary, #1f1f1f);
    line-height: 20px;
  }
}

:global(html.dark) .auth-selection-item {
  border-bottom-color: #3c4043;

  &:hover {
    background-color: rgba(138, 180, 248, 0.12);
  }

  &:active {
    background-color: rgba(138, 180, 248, 0.2);
  }

  &:focus-visible {
    outline-color: #8ab4f8;
  }

  &__icon {
    color: #8ab4f8;
  }

  &__title {
    color: var(--v-text-primary, #e3e3e3);
  }
}
</style>
