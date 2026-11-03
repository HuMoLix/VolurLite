<template>
  <div class="flex flex-col justify-between h-full">
    <div>
      <p class="text-base text-google-text-primary dark:text-google-text-darkPrimary mb-5 font-normal leading-6">
        {{ t('auth.signin.select_method_title') }}
      </p>

      <div v-if="isFetchingSelection" class="py-12 flex justify-center">
        <n-spin size="medium" />
      </div>

      <AuthMethodSelector
        v-else
        :items="currentSelectionSlice"
        :disabled="loading"
        @select="handleSelectMethod"
      >
        <template #extra-items>
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
                  <circle cx="12" cy="12" r="9" />
                  <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3" />
                  <line x1="12" y1="17" x2="12.01" y2="17" stroke-width="2" />
                </svg>
              </div>
              <div class="auth-selection-item__content">
                <div class="auth-selection-item__title">{{ t('auth.signin.try_another_way') }}</div>
              </div>
            </div>
          </div>
        </template>
      </AuthMethodSelector>
    </div>

    <AuthActionBar
      :secondary-text="t('common.cancel')"
      :secondary-disabled="loading"
      :show-primary="false"
      @secondary="handleCancelSelection"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NSpin } from 'naive-ui'
import { useI18n } from 'vue-i18n'
import { fetchChallengeSelectionApi } from '@/api/auth'
import type { ChallengeOptionVO } from '@/types/auth/flow'
import { useSigninFlowStore } from '@/store/modules/flow'
import { useCardHeader } from '@/composables/useCardHeader'
import { AuthMethodSelector, AuthActionBar, type AuthMethodOption } from '@/components/auth'

defineOptions({
  name: 'SelectionView'
})

const { t } = useI18n()
useCardHeader(() => t('auth.signin.more_signin_options'), '')

const route = useRoute()
const router = useRouter()
const signinFlowStore = useSigninFlowStore()

const availableMethods = ref<ChallengeOptionVO[]>([])
const isFetchingSelection = ref(false)
const pageSize = 2

const loading = computed(() => signinFlowStore.loading)

const currentPage = computed(() => {
  const p = parseInt((route.query.page as string) || '1', 10)
  return isNaN(p) || p < 1 ? 1 : p
})

const totalPages = computed(() => {
  return Math.max(1, Math.ceil(availableMethods.value.length / pageSize))
})

const currentSelectionSlice = computed<AuthMethodOption[]>(() => {
  const start = (currentPage.value - 1) * pageSize
  return availableMethods.value.slice(start, start + pageSize).map((item) => ({
    ...item,
    id: item.type,
    method: item.type,
    title: item.title
  }))
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
  const msg = t('auth.signin.selection_rejected_msg')
  router.replace({
    path: '/v1/signin/challenge/rejected',
    query: signinFlowStore.getQueryParams({
      rejectedmsg: encodeURIComponent(msg)
    })
  })
}

async function handleSelectMethod(item: AuthMethodOption) {
  if (signinFlowStore.loading) return
  try {
    const targetType = (item.type || item.method || '') as string
    const nextStep = await signinFlowStore.selectChallenge(targetType)
    if (!nextStep) {
      throw new Error('服务端未返回下一步挑战流程')
    }
    await router.push({
      path: `/v1/signin/challenge/${nextStep}`,
      query: signinFlowStore.getQueryParams()
    })
  } catch (err: any) {
    window.$message?.error?.(err?.message || t('auth.switch_method_failed'))
  }
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
    handleSelectionRejected()
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
