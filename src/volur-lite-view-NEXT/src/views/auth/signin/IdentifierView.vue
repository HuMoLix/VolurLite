<template>
  <AuthCardShell
    :title="t('auth.signin.title')"
    :subtitle="t('auth.signin.subtitle')"
  >
    <div class="flex flex-col">
      <div>
        <GoogleTextField
          v-model="identifier"
          :label="t('auth.signin.identifier_label')"
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
          :isScrim="true"
          @click.prevent="handleForgotAccount"
        >
          {{ t('auth.signin.forgot_username') }}
        </a>
        <a
          v-if="userStore.hasKnownAccounts"
          href="#"
          class="auth-forgot-link"
          :isScrim="true"
          @click.prevent="handleBackToChooser"
        >
          {{ t('auth.signin.choose_account') }}
        </a>
      </div>

      <p class="text-sm text-google-text-muted dark:text-google-text-darkMuted leading-relaxed">
        {{ t('auth.guest_mode_tip') }}<a
          href="#"
          class="auth-learn-more-link"
        >{{ t('auth.learn_more') }}</a>
      </p>
    </div>

    <AuthActionBar
      :primary-text="t('auth.next')"
      :secondary-text="t('auth.create_account')"
      :primary-disabled="loading"
      :secondary-disabled="loading"
      primary-class="btn-next-step"
      secondary-class="btn-create-account"
      @primary="handleNext"
      @secondary="handleCreateAccount"
    />
  </AuthCardShell>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import GoogleTextField from '@/components/google/GoogleTextField.vue'
import { useSigninFlowStore } from '@/store/modules/flow'
import { useUserStore } from '@/store/modules/user'
import { AuthCardShell, AuthActionBar } from '@/components/auth'
import { useFormError } from '@/composables/useFormError'

defineOptions({
  name: 'IdentifierView'
})

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const signinFlowStore = useSigninFlowStore()
const userStore = useUserStore()

const identifier = ref(signinFlowStore.identifier || '')
const { errorMsg, clearError, setError, setErrorKey, setRawError } = useFormError(identifier)

const loading = computed(() => signinFlowStore.loading)

function checkPendingError(): void {
  const fromStore = signinFlowStore.consumeAccountError()
  const fromQuery = (route.query.accountError as string) || ''
  const pending = fromStore || fromQuery
  if (pending) {
    setError(pending)
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
    setErrorKey('auth.signin.identifier_required')
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
      setRawError(err.message)
    } else {
      setErrorKey('auth.signin.identifier_failed')
    }
  }
}

async function handleForgotAccount() {
  if (signinFlowStore.loading) return
  await signinFlowStore.triggerScrim(undefined, 1000)
  await router.push({
    path: '/v1/recovery/username/selection',
    query: signinFlowStore.getQueryParams({ recoveryType: 'username' })
  })
}

async function handleBackToChooser() {
  if (signinFlowStore.loading) return
  await signinFlowStore.triggerScrim(undefined, 1000)
  await router.push({
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
