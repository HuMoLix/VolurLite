<template>
  <SlidePuzzleCaptcha
    v-bind="$attrs"
    headless
    :show="show"
    :scene="scene"
    @update:show="emit('update:show', $event)"
    @success="emit('success', $event)"
    @fail="emit('fail')"
  />
</template>

<script setup lang="ts">
import SlidePuzzleCaptcha from '@/public/volurEx/components/captcha/SlidePuzzleCaptcha.vue'
import type { CaptchaTicketVO } from '@/types/verification'

interface Props {
  show?: boolean
  scene?: string
}

withDefaults(defineProps<Props>(), {
  show: false,
  scene: 'LOGIN'
})

const emit = defineEmits<{
  (e: 'update:show', val: boolean): void
  (e: 'success', ticketVO: CaptchaTicketVO): void
  (e: 'fail'): void
}>()
</script>
