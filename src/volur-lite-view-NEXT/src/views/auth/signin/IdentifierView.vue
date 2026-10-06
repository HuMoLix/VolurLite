<template>
  <div class="auth-card__body">
    <div class="auth-card__left">
      <h1 class="auth-card__title">{{ t('auth.signin_title') }}</h1>
      <p class="auth-card__subtitle">{{ t('auth.signin_subtitle') }}</p>
      <div id="auth-header-extra" class="auth-card__header-extra" />
    </div>

    <div class="auth-card__right">
      <div class="auth-form-content w-full h-full flex flex-col justify-between">
        <div class="flex flex-col">
          <div>
            <GoogleTextField
              v-model="identifier"
              :label="t('auth.identifier_label')"
              autocomplete="username"
              autofocus
              :error-msg="errorMsg"
              @clear="clearError"
              @enter="handleNext"
            />
          </div>

          <div class="flex items-center justify-between mt-2.5 mb-8">
            <a
              href="#"
              class="auth-forgot-link"
              @click.prevent="handleForgotAccount"
            >
              {{ t('auth.forgot_email') }}
            </a>
            <a
              v-if="hasKnownAccounts()"
              href="#"
              class="auth-forgot-link"
              @click.prevent="handleBackToChooser"
            >
              {{ t('auth.choose_account') }}
            </a>
          </div>

          <p class="text-sm text-google-text-muted dark:text-google-text-darkMuted leading-relaxed">
            {{ t('auth.guest_mode_tip') }}<a
              href="#"
              class="auth-learn-more-link"
            >{{ t('auth.learn_more') }}</a>
          </p>
        </div>

        <div class="flex items-center justify-end gap-2.5 pt-6 sm:pt-8">
          <button
            type="button"
            class="auth-action-btn btn-create-account"
            :disabled="loading"
            :isScrim="true"
            @click="handleCreateAccount"
          >
            {{ t('auth.create_account') }}
          </button>
          <n-button
            type="primary"
            class="auth-action-btn btn-next-step"
            :disabled="loading"
            @click="handleNext"
          >
            {{ t('auth.next') }}
          </n-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NButton } from 'naive-ui'
import { useI18n } from 'vue-i18n'
import GoogleTextField from '@/components/google/GoogleTextField.vue'
import { useSigninFlowStore } from '@/store/modules/flow'
import { hasKnownAccounts } from '@/utils/accountChooser'

defineOptions({
  name: 'IdentifierView'
})

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const signinFlowStore = useSigninFlowStore()

const identifier = ref(signinFlowStore.identifier || '')
const errorKey = ref('')
const rawErrorMsg = ref('')

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
let hasMounted = false

watch(identifier, () => {
  if (hasMounted && (errorKey.value || rawErrorMsg.value)) {
    clearError()
  }
})

function checkPendingError(): void {
  const fromStore = signinFlowStore.consumeAccountError()
  const fromQuery = (route.query.accountError as string) || ''
  const pending = fromStore || fromQuery
  if (pending) {
    if (pending.startsWith('auth.') || pending.startsWith('common.')) {
      errorKey.value = pending
      rawErrorMsg.value = ''
    } else {
      rawErrorMsg.value = pending
      errorKey.value = ''
    }
    if (fromQuery) {
      const nextQuery = { ...route.query }
      delete nextQuery.accountError
      router.replace({ path: route.path, query: nextQuery })
    }
  }
}

onMounted(() => {
  signinFlowStore.ensureFlowParams(route, router)
  if (signinFlowStore.identifier && !identifier.value) {
    identifier.value = signinFlowStore.identifier
  }
  checkPendingError()
  setTimeout(() => {
    hasMounted = true
  }, 100)
})

watch(
  () => route.query.accountError,
  (newVal) => {
    if (newVal && typeof newVal === 'string') {
      checkPendingError()
    }
  }
)

watch(
  () => signinFlowStore.accountError,
  (val) => {
    if (val) {
      checkPendingError()
    }
  }
)

async function handleNext() {
  if (signinFlowStore.loading) return

  const trimmed = identifier.value.trim()
  if (!trimmed) {
    errorKey.value = 'auth.identifier_required'
    rawErrorMsg.value = ''
    return
  }

  try {
    const nextStep = await signinFlowStore.submitIdentifier(trimmed)
    await router.push({
      path: `/v1/signin/challenge/${nextStep}`,
      query: signinFlowStore.getQueryParams()
    })
  } catch (err: any) {
    if (err?.message) {
      rawErrorMsg.value = err.message
      errorKey.value = ''
    } else {
      errorKey.value = 'auth.identifier_failed'
      rawErrorMsg.value = ''
    }
  }
}

function handleForgotAccount() {
  if (signinFlowStore.loading) return
  router.push({
    path: '/v1/recovery/identifier',
    query: signinFlowStore.getQueryParams()
  }).catch(() => {})
}

function handleBackToChooser() {
  if (signinFlowStore.loading) return
  router.push({
    path: '/v1/signin/accountchooser',
    query: signinFlowStore.getQueryParams()
  })
}

async function handleCreateAccount() {
  if (signinFlowStore.loading) return
  await signinFlowStore.triggerScrim(undefined, 1000)
  await router.push({
    path: '/v1/signup/steps/name',
    query: {
      continue: signinFlowStore.continueUrl || '/competitions',
      followup: '/v1/signup',
      flowName: 'VlifWebSignUp',
      flowEntry: 'WebSignUp'
    }
  })
}
</script>

<style scoped lang="scss">
.auth-forgot-link {
  display: inline-flex;
  align-items: center;
  padding: 1px 2px !important;
  border-radius: 4px;
  background-color: transparent !important;
  color: #0b57d0 !important;
  font-size: 14px;
  font-weight: 500;
  text-decoration: none !important;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;

  &:hover {
    background-color: rgba(11, 87, 208, 0.08) !important;
    text-decoration: none !important;
    color: #0b57d0 !important;
  }

  &:active {
    background-color: rgba(11, 87, 208, 0.16) !important;
  }
}

:global(.dark) .auth-forgot-link {
  background-color: transparent !important;
  color: #8ab4f8 !important;

  &:hover {
    background-color: rgba(138, 180, 248, 0.12) !important;
    color: #8ab4f8 !important;
  }
}

.auth-learn-more-link {
  color: #0b57d0 !important;
  font-weight: 500;
  text-decoration: none;
  cursor: pointer;

  &:hover {
    text-decoration: underline !important;
    color: #0b57d0 !important;
  }
}

:global(.dark) .auth-learn-more-link {
  color: #8ab4f8 !important;

  &:hover {
    color: #8ab4f8 !important;
  }
}

.auth-action-btn {
  height: 36px !important;
  min-width: 88px !important;
  padding: 0 16px !important;
  border-radius: 9999px !important;
  font-size: 14px !important;
  font-weight: 500 !important;
  box-sizing: border-box;
}

.btn-create-account {
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

:global(.dark) .btn-create-account {
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

.btn-next-step {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background-color: #0b57d0 !important;
  color: #ffffff !important;
  border: none !important;
  transition: all 0.15s cubic-bezier(0.4, 0, 0.2, 1);
  outline: none;
  user-select: none;

  :deep(.n-button__content),
  .n-button__content {
    color: #ffffff !important;
  }

  :deep(.n-button__state-border) {
    border: none !important;
  }

  &:hover {
    background-color: #0842a0 !important;
    color: #ffffff !important;
    box-shadow: 0 1px 2px 0 rgba(60, 64, 67, 0.3), 0 1px 3px 1px rgba(60, 64, 67, 0.15);

    :deep(.n-button__content),
    .n-button__content {
      color: #ffffff !important;
    }
  }

  &:active {
    background-color: #063980 !important;
    color: #ffffff !important;
    transform: scale(0.98);
    box-shadow: 0 1px 2px 0 rgba(60, 64, 67, 0.3);

    :deep(.n-button__content),
    .n-button__content {
      color: #ffffff !important;
    }
  }

  &:focus,
  &:focus-visible {
    background-color: #0b57d0 !important;
    color: #ffffff !important;

    :deep(.n-button__content),
    .n-button__content {
      color: #ffffff !important;
    }
  }
}

:global(.dark) .btn-next-step {
  background-color: #8ab4f8 !important;
  color: #202124 !important;

  :deep(.n-button__content),
  .n-button__content {
    color: #202124 !important;
  }

  &:hover {
    background-color: #aecbfa !important;
    color: #202124 !important;

    :deep(.n-button__content),
    .n-button__content {
      color: #202124 !important;
    }
  }

  &:active {
    background-color: #669df6 !important;
    color: #202124 !important;
    transform: scale(0.98);

    :deep(.n-button__content),
    .n-button__content {
      color: #202124 !important;
    }
  }
}
</style>
