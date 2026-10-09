<template>
  <AuthCardShell
    :title="cardTitle"
    :subtitle="cardSubtitle"
  >
    <div v-if="isSubmitted" class="space-y-6 flex flex-col justify-between h-full">
      <div class="space-y-4">
        <div class="flex items-center gap-3 text-google-green-light dark:text-google-green-dark">
          <svg class="w-6 h-6 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <span class="text-base font-medium text-google-text-primary dark:text-google-text-darkPrimary">
            {{ t('auth.recovery.email_sent_title') }}
          </span>
        </div>

        <p class="text-sm text-google-text-muted dark:text-google-text-darkMuted leading-relaxed">
          {{ t('auth.recovery.email_sent_desc') }}
        </p>
      </div>

      <AuthActionBar
        :primary-text="t('auth.back_to_signin')"
        :primary-disabled="loading"
        :show-secondary="false"
        @primary="handleBackToSignin"
      />
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
          {{ t('auth.recovery.email_tip') }}
        </p>
      </div>

      <AuthActionBar
        :primary-text="t('auth.next')"
        :secondary-text="t('common.back')"
        :primary-disabled="loading || isSubmitting"
        :secondary-disabled="loading || isSubmitting"
        :primary-loading="isSubmitting"
        @primary="handleNext"
        @secondary="handleBackToSelection"
      />
    </form>
  </AuthCardShell>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import GoogleTextField from '@/components/google/GoogleTextField.vue'
import { useSigninFlowStore } from '@/store/modules/flow'
import { submitRecoveryEmailApi } from '@/api/auth'
import { AuthCardShell, AuthActionBar } from '@/components/auth'
import { useFormError } from '@/composables/useFormError'

defineOptions({
  name: 'EmailView'
})

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const signinFlowStore = useSigninFlowStore()

const email = ref((route.query.email as string) || '')
const isSubmitting = ref(false)
const isSubmitted = ref(false)

const loading = computed(() => signinFlowStore.loading)
const { errorMsg, clearError, setErrorKey, setRawError } = useFormError(email)

const cardTitle = computed(() => t('auth.recovery.username_title'))
const cardSubtitle = computed(() => {
  if (isSubmitted.value) {
    return t('auth.recovery.email_sent_title')
  }
  return t('auth.recovery.method_email_desc')
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

const EMAIL_REGEX = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/

async function handleNext(): Promise<void> {
  if (loading.value || isSubmitting.value) return
  const trimmed = email.value.trim()
  if (!trimmed) {
    setErrorKey('auth.email_required')
    return
  }
  if (!EMAIL_REGEX.test(trimmed)) {
    setErrorKey('auth.email_invalid')
    return
  }

  isSubmitting.value = true
  try {
    await signinFlowStore.withLoading(async () => {
      await submitRecoveryEmailApi({
        dsh: signinFlowStore.dsh,
        tl: signinFlowStore.tl,
        cid: signinFlowStore.cid,
        email: trimmed
      })
    }, 1000)
    isSubmitted.value = true
  } catch (err: unknown) {
    if (err && typeof err === 'object' && 'message' in err && typeof (err as { message: unknown }).message === 'string') {
      setRawError((err as { message: string }).message)
    } else {
      setErrorKey('auth.recovery.email_submit_failed')
    }
  } finally {
    isSubmitting.value = false
  }
}
</script>
