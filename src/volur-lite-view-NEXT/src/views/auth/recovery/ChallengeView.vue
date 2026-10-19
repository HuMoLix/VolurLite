<template>
  <AuthCardShell>
    <component :is="activeChallengeComponent" />
  </AuthCardShell>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { AuthCardShell } from '@/components/auth'
import { RecaptchaChallengeView } from './challenge'

defineOptions({
  name: 'RecoveryChallengeView'
})

const route = useRoute()

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
