<template>
  <div class="auth-card__body">
    <div class="auth-card__left">
      <h1 class="auth-card__title">{{ cardTitle }}</h1>
      <p v-if="cardSubtitle" class="auth-card__subtitle">{{ cardSubtitle }}</p>
      <div id="auth-header-extra" class="auth-card__header-extra">
        <button
          type="button"
          class="auth-account-chip"
          :disabled="signinFlowStore.loading"
          :isScrim="true"
          @click="goBackIdentifier"
          :title="t('auth.switch_account')"
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
        <component :is="activeChallengeComponent" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useSigninFlowStore } from '@/store/modules/flow'
import { provideCardHeader } from '@/composables/useCardHeader'
import {
  PwdChallengeView,
  RecaptchaChallengeView,
  DeviceChallengeView,
  TotpChallengeView,
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
const { title: cardTitle, subtitle: cardSubtitle } = provideCardHeader(() => t('auth.security_verification'), '')

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
  if (type === 'device' || type === 'pk') {
    return DeviceChallengeView
  }
  return TotpChallengeView
})

const displayIdentifier = computed(() => {
  return signinFlowStore.identifier || ''
})

const displayInitial = computed(() => {
  return (displayIdentifier.value.charAt(0) || 'U').toUpperCase()
})

onMounted(() => {
  signinFlowStore.ensureFlowParams(route, router)

  if (!signinFlowStore.identifier) {
    window.$message?.warning?.(t('auth.identifier_required'))
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

async function goBackIdentifier() {
  if (signinFlowStore.loading) return
  await signinFlowStore.triggerScrim(undefined, 1000)
  await router.push({
    path: '/v1/signin/identifier',
    query: signinFlowStore.getQueryParams()
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
