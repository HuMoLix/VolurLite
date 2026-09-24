<template>
  <div class="volur-captcha-sms">
    <n-input
      :value="modelValue"
      @update:value="emit('update:modelValue', $event)"
      placeholder="请输入6位短信验证码"
      size="large"
      maxlength="6"
      class="volur-captcha-sms__input"
      @keyup.enter.stop="emit('enter')"
    />
    <n-button
      size="large"
      :disabled="effectiveCountdown > 0 || disabled || loading"
      :loading="loading"
      class="volur-captcha-sms__btn"
      @click="emit('send')"
    >
      {{ effectiveCountdown > 0 ? `${effectiveCountdown}s` : '获取短信码' }}
    </n-button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { NInput, NButton } from 'naive-ui'

interface Props {
  modelValue?: string
  phone?: string
  countdown?: number
  loading?: boolean
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  phone: '',
  countdown: 0,
  loading: false,
  disabled: false
})

const emit = defineEmits<{
  (e: 'update:modelValue', val: string): void
  (e: 'send'): void
  (e: 'enter'): void
}>()

const internalCountdown = ref<number>(0)

const effectiveCountdown = computed<number>(() => {
  return props.countdown > 0 ? props.countdown : internalCountdown.value
})

function startCountdown(seconds = 60) {
  internalCountdown.value = seconds
  const timer = setInterval(() => {
    internalCountdown.value--
    if (internalCountdown.value <= 0) {
      clearInterval(timer)
    }
  }, 1000)
}

defineExpose({
  startCountdown
})
</script>

<style lang="scss" scoped>
.volur-captcha-sms {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;

  &__input {
    flex: 1;
    border-radius: 8px;

    &:hover,
    &:focus-within {
      border-color: #bbb;
    }
  }

  &__btn {
    min-width: 110px;
    height: 40px;
    border-radius: 8px;
    font-size: 14px;
    font-weight: 500;
  }
}
</style>
