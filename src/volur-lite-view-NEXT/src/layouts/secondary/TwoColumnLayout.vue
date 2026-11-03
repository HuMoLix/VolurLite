<template>
  <div
    class="secondary-two-column-layout flex-1 flex min-w-0 w-full"
    :class="[
      responsiveClasses,
      gapClasses,
      { 'flex-row-reverse': sidebarPlacement === 'right' }
    ]"
  >
    <aside
      class="layout-sidebar shrink-0 w-full"
      :class="[
        sidebarWidthClass,
        { 'lg:sticky lg:top-6 self-start': stickySidebar }
      ]"
      :style="sidebarCustomStyle"
    >
      <slot name="sidebar" />
    </aside>

    <div class="layout-main-content flex-1 min-w-0 w-full">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { CSSProperties } from 'vue'

defineOptions({
  name: 'TwoColumnLayout'
})

interface Props {
  sidebarPlacement?: 'left' | 'right'
  sidebarWidth?: string
  responsiveBreakpoint?: 'md' | 'lg' | 'xl'
  gap?: 'small' | 'normal' | 'large'
  stickySidebar?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  sidebarPlacement: 'left',
  sidebarWidth: '18rem',
  responsiveBreakpoint: 'lg',
  gap: 'normal',
  stickySidebar: true
})

const responsiveClasses = computed(() => {
  switch (props.responsiveBreakpoint) {
    case 'md': return 'flex-col md:flex-row'
    case 'lg': return 'flex-col lg:flex-row'
    case 'xl': return 'flex-col xl:flex-row'
    default: return 'flex-col lg:flex-row'
  }
})

const gapClasses = computed(() => {
  switch (props.gap) {
    case 'small': return 'gap-4'
    case 'normal': return 'gap-6'
    case 'large': return 'gap-8'
    default: return 'gap-6'
  }
})

const isPredefinedTailwindWidth = computed(() => {
  return props.sidebarWidth.startsWith('w-')
})

const sidebarWidthClass = computed(() => {
  if (isPredefinedTailwindWidth.value) {
    return props.sidebarWidth
  }
  return ''
})

const sidebarCustomStyle = computed<CSSProperties | undefined>(() => {
  if (!isPredefinedTailwindWidth.value && props.sidebarWidth) {
    return {
      width: props.sidebarWidth,
      maxWidth: '100%'
    }
  }
  return undefined
})
</script>
