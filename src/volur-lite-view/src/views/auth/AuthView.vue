<template>
  <div class="auth-view-container">
    <AuthForm
      :initial-mode="currentMode"
      @success="handleAuthSuccess"
      @mode-change="handleModeChange"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import AuthForm from '@/components/auth/AuthForm.vue'

const router = useRouter()
const route = useRoute()

const currentMode = computed<'login' | 'register' | 'forgot'>(() => {
  if (route.name === 'Register' || route.path.endsWith('/register')) {
    return 'register'
  }
  if (route.name === 'ForgotPassword' || route.path.endsWith('/forgot')) {
    return 'forgot'
  }
  const queryMode = route.query.mode as string
  if (queryMode === 'register' || queryMode === 'forgot') {
    return queryMode
  }
  return 'login'
})

function handleModeChange(mode: string) {
  router.replace({
    query: {
      ...route.query,
      mode: mode === 'login' ? undefined : mode
    }
  })
}

function handleAuthSuccess(mode: 'login' | 'register') {
  if (mode === 'login') {
    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  }
}
</script>

<style scoped lang="scss">
.auth-view-container {
  width: 100%;
}
</style>
