<template>
  <footer class="w-full bg-transparent py-4 px-4 sm:px-6 flex flex-col sm:flex-row items-center justify-center gap-2 sm:gap-4 text-xs text-google-text-muted dark:text-google-text-darkMuted select-none transition-colors duration-200">
    <div v-if="effectiveCopyright" class="flex items-center gap-1.5">
      <span>{{ effectiveCopyright }}</span>
    </div>
    <div v-if="effectiveIcpText" class="hidden sm:inline-block text-google-border-light dark:text-google-border-dark">·</div>
    <div v-if="effectiveIcpText" class="flex items-center gap-1.5">
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
        <span>{{ effectiveIcpText }}</span>
      </a>
    </div>
  </footer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useSystemStore } from '@/store/modules/system'

defineOptions({
  name: 'VFooter'
})

interface Props {
  copyright?: string
  icpText?: string
  icpUrl?: string
}

const props = withDefaults(defineProps<Props>(), {
  copyright: undefined,
  icpText: undefined,
  icpUrl: 'https://beian.miit.gov.cn/'
})

const systemStore = useSystemStore()
const effectiveCopyright = computed(() => (props.copyright !== undefined ? props.copyright : systemStore.copyright || '© 2026 Volur, Inc.'))
const effectiveIcpText = computed(() => (props.icpText !== undefined ? props.icpText : systemStore.icpNumber))
</script>
