<template>
  <AuthCardShell>
    <template #header-extra>
      <AuthAccountChip
        v-if="showAccountChip"
        :identifier="displayIdentifier"
        :disabled="signupFlowStore.loading"
        :title="t('auth.signup.back_to_name')"
        @click="goBackName"
      />
    </template>
    <component :is="activeStepComponent" />
  </AuthCardShell>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useSignupFlowStore } from '@/store/modules/flow'
import { AuthCardShell, AuthAccountChip } from '@/components/auth'
import {
  NameStepView,
  EmailStepView,
  EtotpChallengeStepView,
  PasswordStepView,
  OptionsChallengeStepView
} from './steps'
import { PasskeyProtectView, TotpProtectView } from './protect'

defineOptions({
  name: 'SignUpView'
})

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const signupFlowStore = useSignupFlowStore()

const stepParam = computed(() => {
  const p = route.params.step
  const step = Array.isArray(p) ? p.join('/') : (p as string) || ''
  return step ? step.toLowerCase() : 'name'
})

const activeStepComponent = computed(() => {
  const s = stepParam.value
  if (s === 'email') return EmailStepView
  if (s === 'challenge/etotp' || s === 'etotp') return EtotpChallengeStepView
  if (s === 'password') return PasswordStepView
  if (s === 'pt/options' || s === 'challenge/options' || s === 'options') return OptionsChallengeStepView
  if (s === 'pt/pk' || s === 'protect/pk' || s === 'pt/passkey') return PasskeyProtectView
  if (s === 'pt/totp' || s === 'protect/totp' || s === 'pt/2fa') return TotpProtectView
  return NameStepView
})

const showAccountChip = computed(() => {
  return stepParam.value !== 'name' && (Boolean(signupFlowStore.username) || Boolean(signupFlowStore.email))
})

const displayIdentifier = computed(() => {
  return signupFlowStore.username || signupFlowStore.email || ''
})

onMounted(() => {
  signupFlowStore.ensureFlowParams(route, router)
  if (stepParam.value !== 'name') {
    if (!signupFlowStore.validateTransitionTicket(route)) {
      window.$message?.warning?.(t('auth.tl_missing_warning'))
      router.replace({
        path: '/v1/signup/steps/name',
        query: signupFlowStore.getQueryParams()
      })
    }
  }
})

async function goBackName() {
  if (signupFlowStore.loading) return
  await signupFlowStore.triggerScrim(undefined, 1000)
  await router.push({
    path: '/v1/signup/steps/name',
    query: signupFlowStore.getQueryParams()
  })
}
</script>
