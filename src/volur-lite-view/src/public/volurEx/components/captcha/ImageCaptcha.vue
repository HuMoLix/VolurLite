<template>
  <div class="volur-captcha-image">
    <n-input
      :value="modelValue"
      @update:value="handleInputChange"
      :placeholder="placeholder || t('auth.code')"
      size="large"
      class="volur-captcha-image__input"
      @keyup.enter.stop="emit('enter')"
    />
    <div
      class="volur-captcha-image__box"
      role="button"
      tabindex="0"
      :title="t('auth.captcha_refresh_title')"
      @click="emit('refresh')"
      @keydown.enter="emit('refresh')"
    >
      <img
        v-if="captchaImage"
        :src="captchaImage"
        :alt="t('auth.captcha_alt')"
        class="volur-captcha-image__img"
      />
      <n-button v-else quaternary size="large" class="volur-captcha-image__btn">
        {{ t('auth.verification_code') }}
      </n-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { NInput, NButton } from 'naive-ui'
import { useI18n } from 'vue-i18n'

interface Props {
  modelValue?: string
  captchaImage?: string
  placeholder?: string
}

withDefaults(defineProps<Props>(), {
  modelValue: '',
  captchaImage: '',
  placeholder: ''
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'refresh'): void
  (e: 'enter'): void
}>()

const { t } = useI18n()

function handleInputChange(val: string) {
  emit('update:modelValue', val)
}
</script>

<style lang="scss" scoped>
.volur-captcha-image {
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

  &__box {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 120px;
    height: 40px;
    border-radius: 6px;
    overflow: hidden;
    cursor: pointer;
    background-color: var(--n-color-embedded);
    border: 1px solid var(--n-border-color);
    transition: opacity 0.2s ease, border-color 0.2s ease;
    user-select: none;

    &:hover {
      opacity: 0.85;
      border-color: var(--n-color-target);
    }
  }

  &__img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  &__btn {
    width: 100%;
    height: 100%;
    font-size: 13px;
    color: #444444;
  }
}
</style>
