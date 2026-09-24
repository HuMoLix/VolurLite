<template>
  <VolurCaptcha
    ref="captchaRef"
    v-bind="$attrs"
    @enter="emit('enter')"
    @change="emit('change', $event)"
    @success="emit('success', $event)"
  />
</template>

<script setup lang="ts">
import { ref } from 'vue'
import VolurCaptcha from '@/public/volurEx/components/captcha/VolurCaptcha.vue'
import type { CaptchaTicketVO, CaptchaVerifyRequestDTO } from '@/types/verification'

const emit = defineEmits<{
  (e: 'enter'): void
  (e: 'change', payload: CaptchaVerifyRequestDTO): void
  (e: 'success', ticketVO: CaptchaTicketVO): void
}>()

const captchaRef = ref<InstanceType<typeof VolurCaptcha> | null>(null)

defineExpose({
  getPayload: () => captchaRef.value?.getPayload(),
  verifyBehavior: (scene?: string) => captchaRef.value?.verifyBehavior(scene),
  handleFailure: (err: any) => captchaRef.value?.handleFailure(err),
  reset: () => captchaRef.value?.reset(),
  refresh: () => captchaRef.value?.refresh(),
  mode: captchaRef.value?.mode,
  captchaType: captchaRef.value?.captchaType
})
</script>
