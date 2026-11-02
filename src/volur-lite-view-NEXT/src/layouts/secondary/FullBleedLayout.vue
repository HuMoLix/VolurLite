<template>
  <div
    class="secondary-full-bleed-layout w-full flex flex-col min-w-0"
    :class="[
      fillHeight ? 'flex-1 h-full' : '',
      scrollable ? 'overflow-y-auto' : 'overflow-hidden'
    ]"
  >
    <slot />
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: 'FullBleedLayout'
})

interface Props {
  scrollable?: boolean
  fillHeight?: boolean
}

withDefaults(defineProps<Props>(), {
  scrollable: true,
  fillHeight: true
})
</script>
