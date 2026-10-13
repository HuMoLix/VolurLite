<template>
  <div class="flex flex-col justify-between h-full">
    <div>
      <p class="text-base text-google-text-primary dark:text-google-text-darkPrimary mb-5 font-normal leading-6">
        {{ t('auth.signup.options_prompt') }}
      </p>

      <AuthMethodSelector
        :items="mappedOptions"
        :disabled="loading"
        @select="handleSelectOption"
      />
    </div>

    <AuthActionBar
      :primary-text="t('common.next')"
      :secondary-text="t('auth.later')"
      :primary-disabled="loading"
      :secondary-disabled="loading"
      @primary="handleContinue"
      @secondary="handleSkip"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useSignupFlowStore } from '@/store/modules/flow'
import { useUserStore } from '@/store/modules/user'
import { fetchSignUpOptionsApi } from '@/api/auth'
import type { SignUpOptionVO } from '@/types/auth/flow'
import { useCardHeader } from '@/composables/useCardHeader'
import { AuthMethodSelector, AuthActionBar, type AuthMethodOption } from '@/components/auth'

defineOptions({
  name: 'OptionsChallengeStepView'
})

const { t } = useI18n()
useCardHeader(() => t('auth.security_verification'), () => t('auth.signup.options_prompt'))

const router = useRouter()
const signupFlowStore = useSignupFlowStore()
const loading = computed(() => signupFlowStore.loading)

const options = ref<SignUpOptionVO[]>([
  {
    type: 'passkey',
    title: '',
    icon: 'key',
    available: true
  },
  {
    type: 'totp',
    title: '',
    icon: 'shield',
    available: true
  }
])

const mappedOptions = computed<AuthMethodOption[]>(() => {
  return options.value.map((opt) => ({
    ...opt,
    icon: opt.type === 'passkey' ? 'key' : 'shield',
    title: opt.type === 'passkey' ? t('auth.signin.passkey_verify_title') : t('auth.signup.totp_protect_title')
  }))
})

onMounted(async () => {
  if (signupFlowStore.dsh && signupFlowStore.tl) {
    try {
      const res = await fetchSignUpOptionsApi({
        dsh: signupFlowStore.dsh,
        tl: signupFlowStore.tl
      })
      if (res.data && res.data.length > 0) {
        options.value = res.data
      }
    } catch {
    }
  }
})

async function handleSelectOption(opt: AuthMethodOption) {
  if (signupFlowStore.loading) return
  const targetPath = opt.type === 'passkey' ? '/v1/signup/steps/pt/pk' : '/v1/signup/steps/pt/totp'
  await signupFlowStore.triggerScrim(undefined, 1000)
  await router.push({
    path: targetPath,
    query: signupFlowStore.getQueryParams()
  })
}

async function handleSkip() {
  if (signupFlowStore.loading) return
  const redirect = signupFlowStore.continueUrl || '/competitions'
  await signupFlowStore.triggerScrim(undefined, 1000)
  if (signupFlowStore.username) {
    const userStore = useUserStore()
    userStore.setAuthenticatedUser({
      identifier: signupFlowStore.username,
      username: signupFlowStore.username,
      email: signupFlowStore.email
    })
    await userStore.fetchCurrentUser(true).catch(() => {})
  }
  signupFlowStore.resetFlow()
  await router.push(redirect)
}

async function handleContinue() {
  if (signupFlowStore.loading) return
  const redirect = signupFlowStore.continueUrl || '/competitions'
  await signupFlowStore.triggerScrim(undefined, 1000)
  if (signupFlowStore.username) {
    const userStore = useUserStore()
    userStore.setAuthenticatedUser({
      identifier: signupFlowStore.username,
      username: signupFlowStore.username,
      email: signupFlowStore.email
    })
    await userStore.fetchCurrentUser(true).catch(() => {})
  }
  signupFlowStore.resetFlow()
  await router.push(redirect)
}
</script>
