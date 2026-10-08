<template>
  <div class="auth-card__body">
    <div class="auth-card__left">
      <h1 class="auth-card__title">{{ t('auth.recovery_password_title') }}</h1>
      <p class="auth-card__subtitle">{{ t('auth.recovery_password_subtitle') }}</p>
      <div id="auth-header-extra" class="auth-card__header-extra" />
    </div>

    <div class="auth-card__right">
      <div class="auth-form-content w-full h-full flex flex-col justify-between">
        <div class="flex flex-col">
          <div v-if="errorMsg" class="mb-4 text-sm text-google-red-light dark:text-google-red-dark">
            {{ errorMsg }}
          </div>

          <div v-if="isFetching" class="auth-selection-list">
            <div class="auth-selection-item is-disabled animate-pulse">
              <div class="auth-selection-item__left">
                <div class="w-10 h-10 rounded-full bg-google-border-light dark:bg-google-border-dark" />
                <div class="flex flex-col gap-2">
                  <div class="w-28 h-4 rounded bg-google-border-light dark:bg-google-border-dark" />
                  <div class="w-48 h-3 rounded bg-google-border-light dark:bg-google-border-dark" />
                </div>
              </div>
            </div>
          </div>

          <div v-else-if="methods.length > 0" class="auth-selection-list">
            <div
              v-for="item in methods"
              :key="item.method"
              class="auth-selection-item"
              :class="{ 'is-disabled': loading }"
              role="button"
              tabindex="0"
              :isScrim="true"
              @click="handleSelectMethod(item)"
              @keydown.enter.space.prevent="handleSelectMethod(item)"
            >
              <div class="auth-selection-item__left">
                <div class="auth-selection-item__icon">
                  <svg
                    v-if="item.method === 'EMAIL'"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1.75"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  >
                    <rect x="3" y="5" width="18" height="14" rx="2" />
                    <polyline points="3 7 12 13 21 7" />
                  </svg>
                  <svg
                    v-else-if="item.method === 'PHONE'"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1.75"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  >
                    <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                    <line x1="8" y1="9" x2="16" y2="9" />
                    <line x1="8" y1="13" x2="13" y2="13" />
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
                    <circle cx="12" cy="12" r="10" />
                    <line x1="12" y1="8" x2="12" y2="12" />
                    <line x1="12" y1="16" x2="12.01" y2="16" />
                  </svg>
                </div>
                <div class="auth-selection-item__content">
                  <div class="auth-selection-item__title">
                    {{ getMethodTitle(item) }}
                  </div>
                  <div class="auth-selection-item__subtitle">
                    {{ getMethodDesc(item) }}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div v-else class="p-4 rounded-lg bg-google-surface-light dark:bg-google-surface-darkElevated border border-google-border-light dark:border-google-border-dark text-sm text-google-text-muted dark:text-google-text-darkMuted leading-relaxed">
            {{ t('auth.recovery_password_disabled_tip') }}
          </div>
        </div>

        <div class="flex items-center justify-end pt-6 sm:pt-8">
          <button
            type="button"
            class="auth-action-btn btn-secondary-action"
            :disabled="loading"
            :isScrim="true"
            @click="handleBackToSignin"
          >
            {{ t('auth.back_to_signin') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useSigninFlowStore } from '@/store/modules/flow'
import { getPasswordRecoveryOptionsApi, selectPasswordRecoveryMethodApi } from '@/api/auth'
import type { RecoveryMethodVO } from '@/types/auth/recovery'
import { useCardHeader } from '@/composables/useCardHeader'

defineOptions({
  name: 'PasswordSelectionView'
})

const { t } = useI18n()
useCardHeader(() => t('auth.recovery_password_title'), () => t('auth.recovery_password_subtitle'))

const route = useRoute()
const router = useRouter()
const signinFlowStore = useSigninFlowStore()

const loading = computed(() => signinFlowStore.loading)
const isFetching = ref(true)
const methods = ref<RecoveryMethodVO[]>([])
const maskedEmail = ref('')
const maskedPhone = ref('')
const errorKey = ref('')
const rawErrorMsg = ref('')

const errorMsg = computed(() => {
  if (errorKey.value) {
    return t(errorKey.value)
  }
  return rawErrorMsg.value
})

function getMethodTitle(item: RecoveryMethodVO): string {
  if (item.method === 'EMAIL') {
    return t('auth.recovery_method_email')
  }
  if (item.method === 'PHONE') {
    return t('auth.recovery_method_phone')
  }
  return item.name || item.method
}

function getMethodDesc(item: RecoveryMethodVO): string {
  if (item.method === 'EMAIL' && maskedEmail.value) {
    return t('auth.recovery_password_emailotp_desc', { email: maskedEmail.value })
  }
  if (item.method === 'PHONE' && maskedPhone.value) {
    return t('auth.two_step_sms_tip')
  }
  return item.description || ''
}

async function loadOptions(): Promise<void> {
  if (!signinFlowStore.dsh) {
    await router.push({
      path: '/v1/signin/identifier',
      query: signinFlowStore.getQueryParams()
    })
    return
  }
  isFetching.value = true
  try {
    const res = await getPasswordRecoveryOptionsApi(signinFlowStore.dsh)
    if (res.data?.methods) {
      methods.value = res.data.methods
    } else {
      methods.value = []
    }
    maskedEmail.value = res.data?.maskedEmail || ''
    maskedPhone.value = res.data?.maskedPhone || ''
  } catch (err: unknown) {
    if (err && typeof err === 'object' && 'message' in err && typeof (err as { message: unknown }).message === 'string') {
      rawErrorMsg.value = (err as { message: string }).message
      errorKey.value = ''
    } else {
      errorKey.value = 'auth.recovery_password_options_failed'
      rawErrorMsg.value = ''
    }
  } finally {
    isFetching.value = false
  }
}

onMounted(async () => {
  await signinFlowStore.ensureFlowParams(route, router)
  await loadOptions()
})

async function handleSelectMethod(item: RecoveryMethodVO): Promise<void> {
  if (loading.value) return
  await signinFlowStore.triggerScrim(undefined, 1000)
  try {
    const res = await selectPasswordRecoveryMethodApi({
      dsh: signinFlowStore.dsh,
      tl: signinFlowStore.tl,
      cid: signinFlowStore.cid,
      method: item.method
    })
    if (res.data?.tl) {
      signinFlowStore.tl = res.data.tl
    }
    if (typeof res.data?.cid === 'number') {
      signinFlowStore.cid = res.data.cid
    }
  } catch (err: unknown) {
    if (err && typeof err === 'object' && 'message' in err && typeof (err as { message: unknown }).message === 'string') {
      rawErrorMsg.value = (err as { message: string }).message
      errorKey.value = ''
    }
    return
  }

  await router.push({
    path: '/v1/recovery/challenge/recaptcha',
    query: signinFlowStore.getQueryParams({
      recoveryType: 'password',
      recoveryMethod: item.method
    })
  })
}

async function handleBackToSignin(): Promise<void> {
  if (loading.value) return
  signinFlowStore.tl = ''
  signinFlowStore.cid = 0
  await signinFlowStore.triggerScrim(undefined, 1000)
  await router.push({
    path: '/v1/signin/identifier',
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
</style>
