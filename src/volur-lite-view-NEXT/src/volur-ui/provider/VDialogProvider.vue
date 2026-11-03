<template>
  <n-dialog-provider v-bind="$attrs">
    <v-dialog-bridge>
      <slot />
    </v-dialog-bridge>
  </n-dialog-provider>
</template>

<script setup lang="ts">
import { defineComponent, renderSlot } from 'vue'
import { NDialogProvider, useDialog } from 'naive-ui'

defineOptions({
  name: 'VDialogProvider'
})

const VDialogBridge = defineComponent({
  name: 'VDialogBridge',
  setup(_, { slots }) {
    const dialog = useDialog()
    if (typeof window !== 'undefined') {
      window.$volurDialog = dialog
      window.$dialog = dialog
    }
    return () => renderSlot(slots, 'default')
  }
})
</script>
