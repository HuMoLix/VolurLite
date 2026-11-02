<template>
  <slot />
</template>

<script setup lang="ts">
import { provide } from 'vue'
import {
  useMessage,
  useDialog,
  useNotification,
  useLoadingBar
} from 'naive-ui'
import { VOLUR_FEEDBACK_KEY, type VolurMessageApi } from './context'

type MessageOptions = Parameters<VolurMessageApi['error']>[1]
type MessageReactive = ReturnType<VolurMessageApi['error']>

const rawMessage = useMessage()
const dialog = useDialog()
const notification = useNotification()
const loadingBar = useLoadingBar()

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

provide(VOLUR_FEEDBACK_KEY, {
  message: throttledMessage,
  dialog,
  notification,
  loadingBar
})

provide('n-message-api', throttledMessage)

if (typeof window !== 'undefined') {
  window.$volurMessage = throttledMessage
  window.$volurDialog = dialog
  window.$volurNotification = notification
  window.$volurLoadingBar = loadingBar

  window.$message = throttledMessage
  window.$dialog = dialog
  window.$notification = notification
  window.$loadingBar = loadingBar
}
</script>
