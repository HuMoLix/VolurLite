<template>
  <div class="auth-card__body">
    <div class="auth-card__left">
      <h1 class="auth-card__title">{{ cardTitle }}</h1>
      <p v-if="cardSubtitle" class="auth-card__subtitle">{{ cardSubtitle }}</p>
      <div id="auth-header-extra" class="auth-card__header-extra" />
    </div>

    <div class="auth-card__right">
      <div class="auth-form-content w-full h-full flex flex-col justify-between">
        <component :is="activeChallengeComponent" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { provideCardHeader } from '@/composables/useCardHeader'
import { RecaptchaChallengeView } from './challenge'

defineOptions({
  name: 'RecoveryChallengeView'
})

const { t } = useI18n()
const route = useRoute()
const { title: cardTitle, subtitle: cardSubtitle } = provideCardHeader(
  () => t('auth.security_verification'),
  () => t('auth.security_verification_prompt')
)

const challengeType = computed(() => {
  const p = route.params.type
  const typeParam = Array.isArray(p) ? p.join('/') : (p as string) || (route.query.challengeType as string) || ''
  return typeParam ? typeParam.toLowerCase() : 'recaptcha'
})

const activeChallengeComponent = computed(() => {
  const type = challengeType.value
  if (type === 'recaptcha') {
    return RecaptchaChallengeView
  }
  return RecaptchaChallengeView
})
</script>
