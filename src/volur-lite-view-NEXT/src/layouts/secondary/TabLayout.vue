<template>
  <div class="secondary-tab-layout flex-1 flex flex-col min-w-0 w-full" :class="maxWidthClasses">
    <header v-if="$slots.header" class="layout-tab-header w-full pb-4">
      <slot name="header" />
    </header>

    <div
      class="layout-tab-bar flex flex-col lg:flex-row lg:items-center justify-between gap-3 border-b border-google-border-light/80 dark:border-google-border-dark/80 pb-3"
      :class="{ 'sticky top-0 z-20 bg-google-surface-light dark:bg-google-surface-dark backdrop-blur-md': stickyTabs }"
    >
      <div v-if="$slots.tabs" class="shrink-0 min-w-0">
        <slot name="tabs" />
      </div>

      <div v-if="$slots.extra" class="flex-1 min-w-0 flex items-center justify-end">
        <slot name="extra" />
      </div>
    </div>

    <main class="layout-tab-content flex-1 flex flex-col min-w-0 w-full pt-6">
      <slot />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

defineOptions({
  name: 'TabLayout'
})

interface Props {
  maxWidth?: '7xl' | 'full'
  stickyTabs?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  maxWidth: '7xl',
  stickyTabs: false
})

const maxWidthClasses = computed(() => {
  return props.maxWidth === 'full' ? 'w-full' : 'max-w-7xl mx-auto w-full'
})
</script>
