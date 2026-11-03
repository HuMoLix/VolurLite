<template>
  <n-notification-provider v-bind="$attrs">
    <v-notification-bridge>
      <slot />
    </v-notification-bridge>
  </n-notification-provider>
</template>

<script setup lang="ts">
import { defineComponent, renderSlot } from 'vue'
import { NNotificationProvider, useNotification } from 'naive-ui'

defineOptions({
  name: 'VNotificationProvider'
})

const VNotificationBridge = defineComponent({
  name: 'VNotificationBridge',
  setup(_, { slots }) {
    const notification = useNotification()
    if (typeof window !== 'undefined') {
      window.$volurNotification = notification
      window.$notification = notification
    }
    return () => renderSlot(slots, 'default')
  }
})
</script>
