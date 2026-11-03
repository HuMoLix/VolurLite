<template>
  <div class="flex flex-col justify-between h-full">
    <div>
      <p class="text-sm sm:text-base leading-relaxed text-google-text-desc dark:text-google-text-darkDesc">
        {{ rejectedMessage }}
      </p>
    </div>

    <AuthActionBar
      :primary-text="t('common.retry')"
      :secondary-text="t('common.back')"
      :primary-disabled="loading"
      :secondary-disabled="loading"
      primary-class="btn-verify-action"
      secondary-class="btn-try-another-way"
      @primary="handleRejectedRetry"
      @secondary="handleRejectedBack"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useSigninFlowStore } from '@/store/modules/flow'
import { useCardHeader } from '@/composables/useCardHeader'
import { AuthActionBar } from '@/components/auth'

defineOptions({
  name: 'RejectView'
})

const { t } = useI18n()
useCardHeader(() => t('auth.cannot_signin'), '')

const route = useRoute()
const router = useRouter()
const signinFlowStore = useSigninFlowStore()
const loading = computed(() => signinFlowStore.loading)

const rejectedMessage = computed(() => {
  let msg = (route.query.rejectedmsg as string) || ''
  if (!msg) {
    return t('auth.cannot_signin_default_msg')
  }
  try {
    while (msg.includes('%')) {
      const decoded = decodeURIComponent(msg)
      if (decoded === msg) break
      msg = decoded
    }
  } catch {}
  return msg
})

async function handleRejectedBack() {
  if (signinFlowStore.loading) return
  signinFlowStore.tl = ''
  signinFlowStore.cid = 0
  await signinFlowStore.triggerScrim(undefined, 1000)
  await router.push({
    path: '/v1/signin/identifier',
    query: signinFlowStore.getQueryParams()
  })
}

async function handleRejectedRetry() {
  if (signinFlowStore.loading) return
  await signinFlowStore.triggerScrim(undefined, 1000)
  await router.push({
    path: '/v1/signin/challenge/selection',
    query: signinFlowStore.getQueryParams({
      page: '1'
    })
  })
}
</script>
