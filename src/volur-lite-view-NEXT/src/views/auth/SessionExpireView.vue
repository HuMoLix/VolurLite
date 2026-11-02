<template>
  <AuthCardShell
    :title="t('auth.session_expired')"
    :subtitle="t('auth.session_expired_subtitle')"
  >
    <div class="flex flex-col text-left py-4">
      <p class="text-sm text-google-text-muted dark:text-google-text-darkMuted leading-relaxed max-w-sm mb-6">
        {{ t('auth.session_expired_tip') }}
      </p>
    </div>

    <AuthActionBar
      :primary-text="t('common.retry')"
      :primary-disabled="loading"
      :show-secondary="false"
      @primary="handleRetry"
    />
  </AuthCardShell>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useSigninFlowStore, useSignupFlowStore } from '@/store/modules/flow'
import { AuthCardShell, AuthActionBar } from '@/components/auth'

defineOptions({
  name: 'SessionExpireView'
})

const { t } = useI18n()

const route = useRoute()
const router = useRouter()
const signinFlowStore = useSigninFlowStore()
const signupFlowStore = useSignupFlowStore()

const activeFlowStore = computed(() => {
  const followup = (route.query.followup as string) || ''
  const cont = (route.query.continue as string) || ''
  if (followup.includes('signup') || cont.includes('signup')) {
    return signupFlowStore
  }
  return signinFlowStore
})

const loading = computed(() => activeFlowStore.value.loading)

onMounted(() => {
  if (!route.query.continue || !route.query.followup || route.query.followup === '/') {
    const cont = (route.query.continue as string) || (route.query.redirect as string) || '/competitions'
    const follow =
      route.query.followup && route.query.followup !== '/'
        ? (route.query.followup as string)
        : cont.includes('signup')
          ? '/v1/signup'
          : '/v1/signin/identifier'
    router.replace({
      path: route.path,
      query: {
        ...route.query,
        continue: cont,
        followup: follow
      }
    })
  }
})

function resolveTargetDestination(): { path: string; query?: Record<string, string> } {
  const followup = (route.query.followup as string) || ''
  const cont = (route.query.continue as string) || ''

  if (followup) {
    if (followup.startsWith('/v1/signup') || followup === 'signup') {
      return { path: '/v1/signup', query: cont ? { continue: cont } : undefined }
    }
    if (followup.startsWith('/v1/signin') || followup === 'signin') {
      return { path: '/v1/signin/identifier', query: cont ? { continue: cont } : undefined }
    }
    if (followup.startsWith('/') && followup !== '/') {
      return { path: followup, query: cont ? { continue: cont } : undefined }
    }
  }

  if (cont) {
    if (cont.startsWith('/v1/signup') || cont === 'signup') {
      return { path: '/v1/signup' }
    }
    if (cont.startsWith('/v1/signin') || cont === 'signin') {
      return { path: '/v1/signin/identifier' }
    }
  }

  return { path: '/v1/signin/identifier', query: cont && cont !== '/' ? { continue: cont } : undefined }
}

async function handleRetry() {
  if (loading.value) return
  const target = resolveTargetDestination()
  await activeFlowStore.value.triggerScrim(undefined, 1000)
  signinFlowStore.resetFlow()
  signupFlowStore.resetFlow()
  await router.push(target)
}
</script>
