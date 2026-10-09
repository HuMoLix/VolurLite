<template>
  <div class="flex flex-col justify-between h-full">
    <div class="flex-1 flex flex-col justify-center">
      <div class="flex flex-col items-center text-center py-4">
        <div class="w-16 h-16 rounded-full bg-google-blue/10 dark:bg-google-blue-dark/15 flex items-center justify-center text-google-blue dark:text-google-blue-dark mb-4">
          <svg class="w-8 h-8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
            <polyline points="9 12 11 14 15 10" />
          </svg>
        </div>
        <h2 class="text-base font-medium text-google-text-primary dark:text-google-text-darkPrimary mb-2">
          {{ t('auth.totp_protect_title') }}
        </h2>
        <p class="text-sm text-google-text-secondary dark:text-google-text-darkSecondary max-w-sm leading-relaxed mb-4">
          {{ t('auth.totp_protect_desc') }}
        </p>
        <div class="p-3.5 rounded-xl bg-gray-50 dark:bg-neutral-800/60 border border-google-border-base dark:border-neutral-700/60 max-w-sm text-xs text-google-text-muted dark:text-google-text-darkMuted leading-relaxed text-left">
          {{ t('auth.totp_protect_notice') }}
        </div>
      </div>
    </div>

    <AuthActionBar
      :primary-text="t('auth.got_it')"
      :secondary-text="t('common.back')"
      :primary-disabled="loading"
      :secondary-disabled="loading"
      @primary="handleBackOptions"
      @secondary="handleBackOptions"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useSignupFlowStore } from '@/store/modules/flow'
import { useCardHeader } from '@/composables/useCardHeader'
import { AuthActionBar } from '@/components/auth'

defineOptions({
  name: 'TotpProtectView'
})

const { t } = useI18n()
useCardHeader(() => t('auth.totp_protect_title'), () => t('auth.totp_protect_desc'))

const router = useRouter()
const signupFlowStore = useSignupFlowStore()
const loading = computed(() => signupFlowStore.loading)

async function handleBackOptions() {
  if (signupFlowStore.loading) return
  await signupFlowStore.triggerScrim(undefined, 1000)
  await router.push({
    path: '/v1/signup/steps/pt/options',
    query: signupFlowStore.getQueryParams()
  })
}
</script>
