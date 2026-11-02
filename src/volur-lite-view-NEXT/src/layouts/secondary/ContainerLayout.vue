<template>
  <div class="secondary-container-layout flex-1 flex flex-col min-w-0 w-full" :class="paddingClasses">
    <div
      class="flex-1 flex flex-col w-full mx-auto"
      :class="[maxWidthClasses, gapClasses, contentClass]"
    >
      <header v-if="$slots.header" class="layout-container-header w-full">
        <slot name="header" />
      </header>

      <div class="layout-container-body flex-1 flex flex-col min-w-0 w-full">
        <slot />
      </div>

      <footer v-if="$slots.footer" class="layout-container-footer w-full mt-auto">
        <slot name="footer" />
      </footer>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

defineOptions({
  name: 'ContainerLayout'
})

interface Props {
  maxWidth?: 'sm' | 'md' | 'lg' | 'xl' | '2xl' | '4xl' | '6xl' | '7xl' | 'full'
  padding?: 'none' | 'small' | 'normal' | 'large'
  gap?: 'none' | 'small' | 'normal' | 'large'
  contentClass?: string
}

const props = withDefaults(defineProps<Props>(), {
  maxWidth: '7xl',
  padding: 'normal',
  gap: 'normal',
  contentClass: ''
})

const maxWidthClasses = computed(() => {
  switch (props.maxWidth) {
    case 'sm': return 'max-w-screen-sm'
    case 'md': return 'max-w-screen-md'
    case 'lg': return 'max-w-screen-lg'
    case 'xl': return 'max-w-screen-xl'
    case '2xl': return 'max-w-screen-2xl'
    case '4xl': return 'max-w-4xl'
    case '6xl': return 'max-w-6xl'
    case '7xl': return 'max-w-7xl'
    case 'full': return 'max-w-full'
    default: return 'max-w-7xl'
  }
})

const paddingClasses = computed(() => {
  switch (props.padding) {
    case 'none': return 'p-0'
    case 'small': return 'px-3 sm:px-4 py-4 sm:py-6'
    case 'normal': return 'px-4 sm:px-6 lg:px-8 py-6 sm:py-8'
    case 'large': return 'px-6 sm:px-8 lg:px-12 py-8 sm:py-12'
    default: return 'px-4 sm:px-6 lg:px-8 py-6 sm:py-8'
  }
})

const gapClasses = computed(() => {
  switch (props.gap) {
    case 'none': return 'gap-0'
    case 'small': return 'space-y-3'
    case 'normal': return 'space-y-6'
    case 'large': return 'space-y-8'
    default: return 'space-y-6'
  }
})
</script>
