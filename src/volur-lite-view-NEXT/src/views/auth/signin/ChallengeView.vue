<template>
  <AuthCardShell>
    <template #header-extra>
      <AuthAccountChip
        :identifier="displayIdentifier"
        :disabled="signinFlowStore.loading"
        @click="handleSwitchAccount"
      />
    </template>
    <component :is="activeChallengeComponent" />
  </AuthCardShell>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useSigninFlowStore } from '@/store/modules/flow'
import { AuthCardShell, AuthAccountChip } from '@/components/auth'
import {
  PwdChallengeView,
  RecaptchaChallengeView,
  PassKeyChallengeView,
  EmailotpChallengeView,
  SmsotpChallengeView,
  RejectView,
  SelectionView
} from './challenge'

defineOptions({
  name: 'ChallengeView'
})

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const signinFlowStore = useSigninFlowStore()

const challengeType = computed(() => {
  const p = route.params.type
  const typeParam = Array.isArray(p) ? p.join('/') : (p as string) || (route.query.challengeType as string) || ''
  return typeParam ? typeParam.toLowerCase() : 'pwd'
})

const activeChallengeComponent = computed(() => {
  const type = challengeType.value
  if (type === 'rejected' || type === 'reject') {
    return RejectView
  }
  if (type === 'selection') {
    return SelectionView
  }
  if (type === 'pwd' || type === 'password') {
    return PwdChallengeView
  }
  if (type === 'recaptcha') {
    return RecaptchaChallengeView
  }
  if (type === 'passkey' || type === 'pk' || type === 'device') {
    return PassKeyChallengeView
  }
  if (type === 'emailotp' || type === 'etotp' || type === 'email') {
    return EmailotpChallengeView
  }
  if (type === 'smsotp' || type === 'sms' || type === 'totp') {
    return SmsotpChallengeView
  }
  return PwdChallengeView
})

const displayIdentifier = computed(() => {
  return signinFlowStore.identifier || ''
})

onMounted(() => {
  signinFlowStore.ensureFlowParams(route, router)

  if (!signinFlowStore.identifier) {
    router.replace({
      path: '/v1/signin/identifier',
      query: signinFlowStore.getQueryParams()
    })
    return
  }

  if (challengeType.value !== 'rejected' && challengeType.value !== 'reject') {
    if (!signinFlowStore.validateTransitionTicket(route)) {
      window.$message?.warning?.(t('auth.tl_missing_warning'))
      router.replace({
        path: '/v1/signin/identifier',
        query: signinFlowStore.getQueryParams()
      })
    }
  }
})

async function handleSwitchAccount() {
  if (signinFlowStore.loading) return
  await signinFlowStore.triggerScrim(undefined, 1000)
  await router.push({
    path: '/v1/signin/accountchooser',
    query: signinFlowStore.getQueryParams()
  })
}
</script>
