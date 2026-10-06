<template>
  <div class="auth-card__body">
    <div class="auth-card__left">
      <h1 class="auth-card__title">{{ cardTitle }}</h1>
      <p v-if="cardSubtitle" class="auth-card__subtitle">{{ cardSubtitle }}</p>
      <div id="auth-header-extra" class="auth-card__header-extra">
        <button
          v-if="showAccountChip"
          type="button"
          class="auth-account-chip"
          :disabled="signupFlowStore.loading"
          :isScrim="true"
          @click="goBackName"
          :title="t('auth.back_to_name')"
        >
          <span class="auth-account-chip__avatar">
            {{ displayInitial }}
          </span>
          <span class="auth-account-chip__label">{{ displayIdentifier }}</span>
          <svg
            class="w-3 h-3 text-google-text-muted dark:text-google-text-darkMuted fill-current"
            viewBox="7 10 10 5"
            aria-hidden="true"
          >
            <polygon points="7 10 12 15 17 10" />
          </svg>
        </button>
      </div>
    </div>

    <div class="auth-card__right">
      <div class="auth-form-content w-full h-full flex flex-col justify-between">
        <component :is="activeStepComponent" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useSignupFlowStore } from '@/store/modules/flow'
import { provideCardHeader } from '@/composables/useCardHeader'
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
const { title: cardTitle, subtitle: cardSubtitle } = provideCardHeader(() => t('auth.signup_create_title'), () => t('auth.signup_create_subtitle'))

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

const displayInitial = computed(() => {
  return (displayIdentifier.value.charAt(0) || 'V').toUpperCase()
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

<style scoped lang="scss">
.auth-account-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px 4px 6px;
  border-radius: 9999px;
  border: 1px solid var(--v-border-base, #dadce0);
  background: transparent;
  color: var(--v-text-primary, #202124);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  outline: none;

  &:hover {
    background-color: rgba(60, 64, 67, 0.08);
  }

  &__avatar {
    width: 20px;
    height: 20px;
    border-radius: 50%;
    background-color: #0b57d0;
    color: #ffffff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 10px;
    font-weight: 700;
  }

  &__label {
    max-width: 180px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

:global(html.dark) .auth-account-chip {
  border-color: #3c4043;
  color: var(--v-text-primary, #e8eaed);

  &:hover {
    background-color: rgba(255, 255, 255, 0.08);
  }

  &__avatar {
    background-color: #8ab4f8;
    color: #202124;
  }
}
</style>
