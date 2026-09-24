<template>
  <div class="auth-view-container">
    <AuthForm
      v-model:mode="currentMode"
      @success="handleAuthSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import AuthForm from '@/components/auth/AuthForm.vue'

type AuthMode = 'login' | 'register' | 'forgot'

const router = useRouter()
const route = useRoute()

const currentMode = computed<AuthMode>({
  get() {
    if (route.name === 'Register' || route.path.endsWith('/register') || route.query.mode === 'register') {
      return 'register'
    }
    if (route.name === 'ForgotPassword' || route.path.endsWith('/forgot') || route.query.mode === 'forgot') {
      return 'forgot'
    }
    return 'login'
  },
  set(val) {
    const targetPath = val === 'register' ? '/auth/register' : val === 'forgot' ? '/auth/forgot' : '/auth/login'
    if (route.path !== targetPath) {
      router.push({
        path: targetPath,
        query: route.query.redirect ? { redirect: route.query.redirect } : {}
      })
    }
  }
})

function handleAuthSuccess(type: AuthMode) {
  if (type === 'login') {
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
