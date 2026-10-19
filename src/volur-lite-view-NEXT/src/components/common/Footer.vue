<template>
  <footer class="w-full bg-transparent py-4 px-4 sm:px-6 flex flex-col sm:flex-row items-center justify-center gap-2 sm:gap-4 text-xs text-google-text-muted dark:text-google-text-darkMuted select-none transition-colors duration-200">
    <div class="flex items-center gap-1.5">
      <span>{{ copyright }}</span>
    </div>
    <div class="hidden sm:inline-block text-google-border-light dark:text-google-border-dark">·</div>
    <div class="flex items-center gap-1.5">
      <a
        :href="icpUrl"
        target="_blank"
        rel="noopener noreferrer"
        class="inline-flex items-center gap-1 text-google-text-muted dark:text-google-text-darkMuted hover:text-google-text-primary dark:hover:text-google-text-darkPrimary transition-colors hover:underline"
      >
        <img
          src="/r/static/img/beian.png"
          alt="工信部备案图标"
          class="w-3.5 h-3.5 object-contain"
        />
        <span>{{ icpText }}</span>
      </a>
    </div>
  </footer>
</template>

<script setup lang="ts">
defineOptions({
  name: 'AppFooter'
})

interface Props {
  copyright?: string
  icpText?: string
  icpUrl?: string
}

withDefaults(defineProps<Props>(), {
  copyright: '© 2026 Volur, Inc.',
  icpText: '京ICP备',
  icpUrl: 'https://beian.miit.gov.cn/'
})
</script>
