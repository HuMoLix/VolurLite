<template>
  <n-message-provider v-bind="$attrs">
    <v-message-bridge>
      <slot />
    </v-message-bridge>
  </n-message-provider>
</template>

<script setup lang="ts">
import { defineComponent, renderSlot, provide } from 'vue'
import {
  NMessageProvider,
  useMessage,
  type MessageOptions,
  type MessageReactive
} from 'naive-ui'
import type { VolurMessageApi } from './context'

defineOptions({
  name: 'VMessageProvider'
})

const VMessageBridge = defineComponent({
  name: 'VMessageBridge',
  setup(_, { slots }) {
    const rawMessage = useMessage()
    const recentErrorMessages = new Map<string, number>()
    const THROTTLE_MS = 1000

    function normalizeContent(content: unknown): string {
      if (typeof content === 'string') return content
      if (typeof content === 'number' || typeof content === 'boolean') return String(content)
      try {
        return JSON.stringify(content)
      } catch {
        return String(content)
      }
    }

    function shouldThrottle(content: unknown): boolean {
      const key = normalizeContent(content)
      if (!key) return false
      const now = Date.now()
      const lastTime = recentErrorMessages.get(key)
      if (lastTime && now - lastTime < THROTTLE_MS) {
        return true
      }
      recentErrorMessages.set(key, now)
      if (recentErrorMessages.size > 50) {
        for (const [k, time] of recentErrorMessages.entries()) {
          if (now - time > THROTTLE_MS * 2) {
            recentErrorMessages.delete(k)
          }
        }
      }
      return false
    }

    const throttledMessage: VolurMessageApi = new Proxy(rawMessage, {
      get(target, prop, receiver) {
        if (prop === 'error') {
          return (content: any, options?: MessageOptions): MessageReactive => {
            if (shouldThrottle(content)) {
              return {
                id: '',
                content,
                destroy: () => {},
                type: 'error'
              } as unknown as MessageReactive
            }
            return target.error(content, options)
          }
        }
        return Reflect.get(target, prop, receiver)
      }
    })

    provide('n-message-api', throttledMessage)

    if (typeof window !== 'undefined') {
      window.$volurMessage = throttledMessage
      window.$message = throttledMessage
    }

    return () => renderSlot(slots, 'default')
  }
})
</script>
