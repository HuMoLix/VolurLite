<template>
  <div class="volur-captcha-email">
    <n-input
      :value="modelValue"
      @update:value="emit('update:modelValue', $event)"
      :placeholder="placeholder || t('auth.enter_6_digit_otp')"
      size="large"
      maxlength="6"
      class="volur-captcha-email__input"
      @keyup.enter.stop="emit('enter')"
    />
    <n-button
      size="large"
      :disabled="effectiveCountdown > 0 || disabled || loading"
      :loading="loading"
      class="volur-captcha-email__btn"
      @click="handleSendClick"
    >
      {{ effectiveCountdown > 0 ? `${effectiveCountdown}s` : (buttonText || t('auth.send_code')) }}
    </n-button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { NInput, NButton } from 'naive-ui'
import { useI18n } from 'vue-i18n'

interface Props {
  modelValue?: string
  account?: string
  countdown?: number
  loading?: boolean
  disabled?: boolean
  placeholder?: string
  buttonText?: string
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  account: '',
  countdown: 0,
  loading: false,
  disabled: false,
  placeholder: '',
  buttonText: ''
})

const emit = defineEmits<{
  (e: 'update:modelValue', val: string): void
  (e: 'send'): void
  (e: 'enter'): void
}>()

const { t } = useI18n()
const internalCountdown = ref<number>(0)

const effectiveCountdown = computed<number>(() => {
  return props.countdown > 0 ? props.countdown : internalCountdown.value
})

function handleSendClick() {
  emit('send')
}

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
.volur-captcha-email {
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
