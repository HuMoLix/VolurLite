<template>
  <n-loading-bar-provider v-bind="$attrs">
    <v-loading-bar-bridge>
      <slot />
    </v-loading-bar-bridge>
  </n-loading-bar-provider>
</template>

<script setup lang="ts">
import { defineComponent, renderSlot } from 'vue'
import { NLoadingBarProvider, useLoadingBar } from 'naive-ui'

defineOptions({
  name: 'VLoadingBarProvider'
})

const VLoadingBarBridge = defineComponent({
  name: 'VLoadingBarBridge',
  setup(_, { slots }) {
    const loadingBar = useLoadingBar()
    if (typeof window !== 'undefined') {
      window.$volurLoadingBar = loadingBar
      window.$loadingBar = loadingBar
    }
    return () => renderSlot(slots, 'default')
  }
})
</script>
