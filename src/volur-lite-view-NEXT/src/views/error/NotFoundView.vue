<template>
  <div class="min-h-screen flex flex-col justify-between items-center bg-google-surface-light dark:bg-google-surface-dark transition-colors duration-300 p-6 md:p-10 select-none">
    <main class="w-full max-w-xl mx-auto flex flex-col items-center text-center my-auto py-12">
      <div class="relative flex items-center justify-center mb-6">
        <span class="text-8xl sm:text-9xl font-black tracking-widest text-google-blue/15 dark:text-google-blue-dark/20 font-mono select-none">
          404
        </span>
        <div class="absolute inset-0 flex items-center justify-center">
          <div class="flex items-center gap-2">
            <span class="w-3.5 h-3.5 rounded-full bg-google-blue animate-pulse"></span>
            <span class="w-3.5 h-3.5 rounded-full bg-google-red"></span>
            <span class="w-3.5 h-3.5 rounded-full bg-google-yellow"></span>
            <span class="w-3.5 h-3.5 rounded-full bg-google-green"></span>
          </div>
        </div>
      </div>

      <h1 class="text-2xl sm:text-3xl font-normal text-google-text-primary dark:text-google-text-darkPrimary mb-3">
        {{ t('common.not_found_title') }}
      </h1>
      <p class="text-sm sm:text-base text-google-text-desc dark:text-google-text-darkDesc max-w-md mx-auto leading-relaxed mb-8">
        {{ t('common.not_found_desc') }}
      </p>

      <div class="flex items-center justify-center">
        <button
          type="button"
          class="h-10 px-6 rounded-full bg-google-blue hover:bg-google-blue-hover active:scale-95 text-white dark:bg-google-blue-dark dark:text-google-surface-dark dark:hover:bg-google-blue-dark-hover font-medium text-sm shadow-sm transition-all flex items-center gap-2 cursor-pointer border-none outline-none"
          @click="goBack"
        >
          <span>{{ t('common.back_prev') }}</span>
        </button>
      </div>
    </main>

    <footer class="w-full max-w-5xl">
      <AppFooter />
    </footer>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import AppFooter from '@/components/common/Footer.vue'

defineOptions({
  name: 'NotFoundView'
})

const router = useRouter()
const { t } = useI18n()

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/competitions')
  }
}
</script>
