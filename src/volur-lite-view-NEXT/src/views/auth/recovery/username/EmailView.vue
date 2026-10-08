<template>
  <div class="auth-card__body">
    <div class="auth-card__left">
      <h1 class="auth-card__title">{{ cardTitle }}</h1>
      <p class="auth-card__subtitle">{{ cardSubtitle }}</p>
      <div id="auth-header-extra" class="auth-card__header-extra" />
    </div>

    <div class="auth-card__right">
      <div v-if="isSubmitted" class="space-y-6 flex flex-col justify-between h-full">
        <div class="space-y-4">
          <div class="flex items-center gap-3 text-google-green-light dark:text-google-green-dark">
            <svg class="w-6 h-6 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span class="text-base font-medium text-google-text-primary dark:text-google-text-darkPrimary">
              {{ t('auth.recovery_email_sent_title') }}
            </span>
          </div>

          <p class="text-sm text-google-text-muted dark:text-google-text-darkMuted leading-relaxed">
            {{ t('auth.recovery_email_sent_desc') }}
          </p>
        </div>

        <div class="flex items-center justify-end pt-6 sm:pt-8">
          <button
            type="button"
            class="auth-action-btn btn-primary-action"
            :disabled="loading"
            :isScrim="true"
            @click="handleBackToSignin"
          >
            {{ t('auth.back_to_signin') }}
          </button>
        </div>
      </div>

      <form v-else class="space-y-6 flex flex-col justify-between h-full" @submit.prevent="handleNext">
        <div class="space-y-3">
          <div>
            <GoogleTextField
              v-model="email"
              type="email"
              :label="t('auth.email_label')"
              autocomplete="email"
              autofocus
              :error-msg="errorMsg"
              @clear="clearError"
              @enter="handleNext"
            />
          </div>

          <p class="text-xs text-google-text-muted dark:text-google-text-darkMuted leading-relaxed">
            {{ t('auth.recovery_email_tip') }}
          </p>
        </div>

        <div class="flex items-center justify-end gap-2.5 pt-6 sm:pt-8">
          <button
            type="button"
            class="auth-action-btn btn-secondary-action"
            :disabled="loading || isSubmitting"
            :isScrim="true"
            @click="handleBackToSelection"
          >
            {{ t('common.back') }}
          </button>
          <n-button
            type="primary"
            class="auth-action-btn btn-primary-action"
            :loading="isSubmitting"
            :disabled="loading || isSubmitting"
            :isScrim="true"
            @click="handleNext"
          >
            {{ t('auth.next') }}
          </n-button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NButton } from 'naive-ui'
import { useI18n } from 'vue-i18n'
import GoogleTextField from '@/components/google/GoogleTextField.vue'
import { useSigninFlowStore } from '@/store/modules/flow'
import { submitRecoveryEmailApi } from '@/api/auth'
import { useCardHeader } from '@/composables/useCardHeader'

defineOptions({
  name: 'EmailView'
})

const { t } = useI18n()

const route = useRoute()
const router = useRouter()
const signinFlowStore = useSigninFlowStore()

const email = ref((route.query.email as string) || '')
const errorKey = ref('')
const rawErrorMsg = ref('')
const isSubmitting = ref(false)
const isSubmitted = ref(false)

const cardTitle = computed(() => t('auth.recovery_username_title'))
const cardSubtitle = computed(() => {
  if (isSubmitted.value) {
    return t('auth.recovery_email_sent_title')
  }
  return t('auth.recovery_method_email_desc')
})
useCardHeader(() => cardTitle.value, () => cardSubtitle.value)

const errorMsg = computed(() => {
  if (errorKey.value) {
    return t(errorKey.value)
  }
  return rawErrorMsg.value
})

function clearError(): void {
  errorKey.value = ''
  rawErrorMsg.value = ''
}

const loading = computed(() => signinFlowStore.loading)

watch(email, () => {
  if (errorKey.value || rawErrorMsg.value) {
    clearError()
  }
})

onMounted(() => {
  signinFlowStore.ensureFlowParams(route, router)
})

async function handleBackToSelection(): Promise<void> {
  if (loading.value || isSubmitting.value) return
  await signinFlowStore.triggerScrim(undefined, 1000)
  await router.push({
    path: '/v1/recovery/username/selection',
    query: signinFlowStore.getQueryParams({ recoveryType: 'username' })
  })
}

async function handleBackToSignin(): Promise<void> {
  if (loading.value) return
  signinFlowStore.resetFlow()
  await signinFlowStore.triggerScrim(undefined, 1000)
  await router.push({
    path: '/v1/signin/identifier',
    query: signinFlowStore.getQueryParams()
  })
}

async function handleNext(): Promise<void> {
  if (loading.value || isSubmitting.value) return
  const trimmed = email.value.trim()
  if (!trimmed) {
    errorKey.value = 'auth.email_required'
    rawErrorMsg.value = ''
    return
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(trimmed)) {
    errorKey.value = 'auth.email_format_error'
    rawErrorMsg.value = ''
    return
  }

  isSubmitting.value = true
  try {
    await signinFlowStore.withLoading(async () => {
      await submitRecoveryEmailApi({
        dsh: signinFlowStore.dsh,
        email: trimmed,
        tl: signinFlowStore.tl,
        cid: signinFlowStore.cid
      })
    }, 1000)
    signinFlowStore.resetFlow()
    isSubmitted.value = true
    router.replace({
      path: route.path,
      query: signinFlowStore.getQueryParams()
    })
  } catch (err: any) {
    if (err?.message) {
      rawErrorMsg.value = err.message
      errorKey.value = ''
    } else {
      errorKey.value = 'auth.email_submit_failed'
      rawErrorMsg.value = ''
    }
  } finally {
    isSubmitting.value = false
  }
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
  cursor: pointer;
}

.btn-secondary-action {
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

  &:hover:not(:disabled) {
    background-color: rgba(11, 87, 208, 0.08) !important;
    color: #0b57d0 !important;
  }

  &:active:not(:disabled) {
    background-color: rgba(11, 87, 208, 0.16) !important;
    color: #0b57d0 !important;
  }

  &:disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }
}

.btn-primary-action {
  background-color: #0b57d0 !important;
  color: #ffffff !important;
  border: none !important;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  outline: none;
  user-select: none;

  &:hover:not(:disabled) {
    background-color: #0842a0 !important;
    box-shadow: 0 1px 2px rgba(60, 64, 67, 0.3), 0 1px 3px 1px rgba(60, 64, 67, 0.15);
  }

  &:active:not(:disabled) {
    background-color: #0b57d0 !important;
  }

  &:disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }
}

:global(.dark) .btn-secondary-action {
  color: #8ab4f8;

  &:hover:not(:disabled) {
    background-color: rgba(138, 180, 248, 0.12) !important;
    color: #8ab4f8 !important;
  }

  &:disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }
}

:global(.dark) .btn-primary-action {
  background-color: #8ab4f8 !important;
  color: #041e49 !important;

  &:hover:not(:disabled) {
    background-color: #aecbfa !important;
  }
}
</style>
