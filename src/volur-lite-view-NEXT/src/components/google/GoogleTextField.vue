<template>
  <div
    class="google-text-field"
    :class="{
      'google-text-field--error': !!errorMsg,
      'google-text-field--disabled': disabled
    }"
  >
    <div
      class="google-text-field__box"
      :class="{
        'google-text-field__box--focused': isFocused,
        'google-text-field__box--error': !!errorMsg,
        'google-text-field__box--disabled': disabled
      }"
      @click="focusInput"
    >
      <input
        ref="inputRef"
        :id="inputId"
        :type="computedType"
        :name="name"
        :value="modelValue"
        :disabled="disabled"
        :readonly="readonly"
        :autocomplete="autocomplete"
        :autofocus="autofocus"
        class="google-text-field__input"
        placeholder=" "
        @input="handleInput"
        @focus="handleFocus"
        @blur="handleBlur"
        @keydown.enter="$emit('enter', $event)"
      />

      <label
        :for="inputId"
        class="google-text-field__label"
        :class="{
          'google-text-field__label--floating': isFloating,
          'google-text-field__label--focused': isFocused,
          'google-text-field__label--error': !!errorMsg
        }"
      >
        {{ displayLabel }}
      </label>

      <div class="google-text-field__trailing">
        <button
          v-if="clearable && hasValue && !disabled && !readonly"
          type="button"
          tabindex="-1"
          class="google-text-field__action-btn"
          :title="t('common.clear')"
          @click.stop="handleClear"
        >
          <svg viewBox="0 0 24 24" class="w-4 h-4 fill-current">
            <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
          </svg>
        </button>

        <button
          v-if="type === 'password' && showPasswordToggle"
          type="button"
          tabindex="-1"
          class="google-text-field__action-btn"
          :title="isPasswordVisible ? t('auth.signin.hide_password') : t('auth.signin.show_password')"
          @click.stop="togglePasswordVisibility"
        >
          <svg v-if="!isPasswordVisible" viewBox="0 0 24 24" class="w-4 h-4 fill-current">
            <path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/>
          </svg>
          <svg v-else viewBox="0 0 24 24" class="w-4 h-4 fill-current">
            <path d="M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.44-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z"/>
          </svg>
        </button>

        <slot name="trailing" />
      </div>
    </div>

    <transition name="field-error-fade">
      <div v-if="errorMsg" class="google-text-field__error-msg">
        <svg viewBox="0 0 24 24" class="w-3.5 h-3.5 fill-current shrink-0">
          <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/>
        </svg>
        <span>{{ errorMsg }}</span>
      </div>
    </transition>

    <div v-if="!errorMsg && helperText" class="google-text-field__helper-text">
      {{ helperText }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, useId } from 'vue'
import { useI18n } from 'vue-i18n'

defineOptions({
  name: 'GoogleTextField'
})

const { t } = useI18n()

interface Props {
  modelValue?: string | number
  label?: string
  placeholder?: string
  type?: string
  id?: string
  name?: string
  autocomplete?: string
  autofocus?: boolean
  disabled?: boolean
  readonly?: boolean
  errorMsg?: string
  helperText?: string
  clearable?: boolean
  showPasswordToggle?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  label: '',
  placeholder: '',
  type: 'text',
  id: '',
  name: '',
  autocomplete: 'off',
  autofocus: false,
  disabled: false,
  readonly: false,
  errorMsg: '',
  helperText: '',
  clearable: false,
  showPasswordToggle: false
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'focus', ev: FocusEvent): void
  (e: 'blur', ev: FocusEvent): void
  (e: 'clear'): void
  (e: 'enter', ev: KeyboardEvent): void
}>()

const generatedId = useId()
const inputId = computed(() => props.id || generatedId)
const displayLabel = computed(() => props.label || props.placeholder)

const inputRef = ref<HTMLInputElement | null>(null)
const isFocused = ref(false)
const isPasswordVisible = ref(false)

const computedType = computed(() => {
  if (props.type === 'password') {
    return isPasswordVisible.value ? 'text' : 'password'
  }
  return props.type
})

const hasValue = computed(() => {
  return props.modelValue !== undefined && props.modelValue !== null && String(props.modelValue).length > 0
})

const isFloating = computed(() => isFocused.value || hasValue.value)

function focusInput() {
  if (!props.disabled) {
    inputRef.value?.focus()
  }
}

function handleInput(event: Event) {
  const target = event.target as HTMLInputElement
  emit('update:modelValue', target.value)
}

function handleFocus(ev: FocusEvent) {
  isFocused.value = true
  emit('focus', ev)
}

function handleBlur(ev: FocusEvent) {
  isFocused.value = false
  emit('blur', ev)
}

function handleClear() {
  emit('update:modelValue', '')
  emit('clear')
  inputRef.value?.focus()
}

function togglePasswordVisibility() {
  isPasswordVisible.value = !isPasswordVisible.value
}

defineExpose({
  focus: () => inputRef.value?.focus(),
  blur: () => inputRef.value?.blur(),
  input: inputRef
})
</script>

<style scoped lang="scss">
.google-text-field {
  position: relative;
  width: 100%;
  display: flex;
  flex-direction: column;

  &__box {
    position: relative;
    display: flex;
    align-items: center;
    border-radius: 4px;
    border: 1px solid var(--v-border-base, #dadce0);
    background-color: transparent;
    transition: border-color 0.15s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.15s cubic-bezier(0.4, 0, 0.2, 1);
    box-sizing: border-box;
    cursor: text;

    &--focused {
      border-color: #0b57d0;
      box-shadow: 0 0 0 1px #0b57d0;
    }

    &--error,
    &--error.google-text-field__box--focused {
      border-color: #ea4335 !important;
      box-shadow: 0 0 0 1px #ea4335 !important;
    }

    &--disabled {
      opacity: 0.6;
      cursor: not-allowed;
      border-color: #dadce0 !important;
    }
  }

  &__input {
    margin: 2px 2px 0 2px;
    padding: 12px 12px;
    flex: 1 1 auto;
    width: 100%;
    min-width: 0;
    height: 28px;
    line-height: 28px;
    font-size: 16px;
    font-family: inherit;
    color: var(--v-text-primary, #202124);
    background: transparent;
    border: none;
    outline: none;
    box-sizing: content-box;
    z-index: 1;

    &::placeholder {
      color: transparent;
    }

    &:disabled {
      cursor: not-allowed;
      color: var(--v-text-sub, #80868b);
    }
  }

  &--error &__input {
    caret-color: #ea4335;
  }

  &__label {
    position: absolute;
    left: 14px;
    top: 50%;
    transform: translateY(-50%);
    font-size: 16px;
    line-height: 1;
    color: var(--v-text-muted, #5f6368);
    pointer-events: none;
    user-select: none;
    white-space: nowrap;
    z-index: 2;
    padding: 0 4px;
    transform-origin: left center;
    background-color: transparent;
    transition: transform 0.15s cubic-bezier(0.4, 0, 0.2, 1),
                top 0.15s cubic-bezier(0.4, 0, 0.2, 1),
                font-size 0.15s cubic-bezier(0.4, 0, 0.2, 1),
                color 0.15s cubic-bezier(0.4, 0, 0.2, 1),
                background-color 0.15s cubic-bezier(0.4, 0, 0.2, 1);

    &--floating {
      top: 0 !important;
      transform: translateY(-50%) scale(0.75) !important;
      left: 10px !important;
      background-color: var(--v-bg-card, #ffffff);
    }

    &--focused {
      color: #0b57d0;
    }

    &--error,
    &--error.google-text-field__label--focused {
      color: #ea4335 !important;
    }
  }

  &__input:focus ~ &__label,
  &__input:not(:placeholder-shown) ~ &__label {
    top: 0 !important;
    transform: translateY(-50%) scale(0.75) !important;
    left: 10px !important;
    background-color: var(--v-bg-card, #ffffff);
  }

  &:not(&--error) &__input:focus ~ &__label {
    color: #0b57d0 !important;
  }

  &--error &__label,
  &--error &__input:focus ~ &__label,
  &--error &__input:not(:placeholder-shown) ~ &__label {
    color: #ea4335 !important;
  }

  &__trailing {
    display: flex;
    align-items: center;
    padding-right: 8px;
    gap: 4px;
    z-index: 2;
  }

  &__action-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 28px;
    height: 28px;
    border-radius: 50%;
    border: none;
    background: transparent;
    color: var(--v-text-muted, #5f6368);
    cursor: pointer;
    transition: background-color 0.15s ease, color 0.15s ease;
    outline: none;

    &:hover {
      background-color: rgba(60, 64, 67, 0.08);
      color: var(--v-text-primary, #202124);
    }

    &:active {
      background-color: rgba(60, 64, 67, 0.16);
    }
  }

  &__error-msg {
    margin-top: 4px;
    margin-left: 0;
    padding-left: 0;
    font-size: 12px;
    line-height: 16px;
    color: #ea4335;
    display: flex;
    align-items: center;
    gap: 6px;
  }

  &__helper-text {
    margin-top: 4px;
    margin-left: 0;
    padding-left: 0;
    font-size: 12px;
    line-height: 16px;
    color: var(--v-text-muted, #5f6368);
  }
}

.field-error-fade-enter-active,
.field-error-fade-leave-active {
  transition: opacity 0.15s cubic-bezier(0.4, 0, 0.2, 1);
}

.field-error-fade-enter-from,
.field-error-fade-leave-to {
  opacity: 0;
}

html.dark {
  .google-text-field {
    &__box {
      border-color: #1e1f20;

      &--focused {
        border-color: #a8c7fa !important;
        box-shadow: 0 0 0 1px #a8c7fa !important;
      }

      &--error,
      &--error.google-text-field__box--focused {
        border-color: #f28b82 !important;
        box-shadow: 0 0 0 1px #f28b82 !important;
      }

      &--disabled {
        border-color: #1e1f20 !important;
      }
    }

    &__input {
      color: var(--v-text-primary, #e3e3e3);
      caret-color: #fff;

      &:disabled {
        color: var(--v-text-sub, #80868b);
      }
    }

    &--error &__input {
      caret-color: #f28b82;
    }

    &__label {
      color: var(--v-text-muted, #bdc1c6);

      &--floating {
        background-color: var(--v-bg-card, #0e0e0e);
      }

      &--focused {
        color: #a8c7fa;
      }

      &--error,
      &--error.google-text-field__label--focused {
        color: #f28b82 !important;
      }
    }

    &__input:focus ~ &__label,
    &__input:not(:placeholder-shown) ~ &__label {
      background-color: var(--v-bg-card, #0e0e0e);
    }

    &:not(&--error) &__input:focus ~ &__label {
      color: #a8c7fa !important;
    }

    &--error &__label,
    &--error &__input:focus ~ &__label,
    &--error &__input:not(:placeholder-shown) ~ &__label {
      color: #f28b82 !important;
    }

    &__action-btn {
      color: var(--v-text-muted, #bdc1c6);

      &:hover {
        background-color: rgba(255, 255, 255, 0.08);
        color: var(--v-text-primary, #e8eaed);
      }

      &:active {
        background-color: rgba(255, 255, 255, 0.16);
      }
    }

    &__error-msg {
      color: #f28b82;
    }

    &__helper-text {
      color: var(--v-text-muted, #bdc1c6);
    }
  }
}

@media (max-width: 839px) {
  .google-text-field__label--floating,
  .google-text-field__input:focus ~ .google-text-field__label,
  .google-text-field__input:not(:placeholder-shown) ~ .google-text-field__label {
    background-color: var(--v-bg-surface, #ffffff) !important;
  }

  html.dark .google-text-field__label--floating,
  html.dark .google-text-field__input:focus ~ .google-text-field__label,
  html.dark .google-text-field__input:not(:placeholder-shown) ~ .google-text-field__label {
    background-color: #1e1f20 !important;
  }
}
</style>
