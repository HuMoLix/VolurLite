<template>
  <button
    ref="buttonRef"
    v-bind="$attrs"
    :type="attrType"
    :disabled="disabled || loading"
    class="volur-btn"
    :class="[
      `volur-btn--${variant}`,
      isPresetSize ? `volur-btn--${size}` : '',
      {
        'is-pill': round,
        'is-circle': circle,
        'is-dashed': dashed,
        'is-block': block,
        'is-loading': loading,
        'is-disabled': disabled || loading,
        'is-focus-ring': isFocusRing
      }
    ]"
    :style="[computedStyle, ($attrs.style as any)]"
    @click="handleClick"
    @keydown="handleKeyDown"
  >
    <span
      v-if="iconPlacement === 'left' && (loading || $slots.icon)"
      class="volur-btn__icon volur-btn__icon--left"
    >
      <span v-if="loading" class="volur-btn__spinner" aria-hidden="true">
        <svg viewBox="0 0 24 24" class="volur-btn__spinner-svg">
          <circle
            cx="12"
            cy="12"
            r="9"
            stroke="currentColor"
            stroke-width="2.75"
            fill="none"
            stroke-linecap="round"
            stroke-dasharray="28 28"
          />
        </svg>
      </span>
      <slot v-else name="icon" />
    </span>

    <span v-if="$slots.default" class="volur-btn__content">
      <slot />
    </span>

    <span
      v-if="iconPlacement === 'right' && (loading || $slots.icon)"
      class="volur-btn__icon volur-btn__icon--right"
    >
      <span v-if="loading" class="volur-btn__spinner" aria-hidden="true">
        <svg viewBox="0 0 24 24" class="volur-btn__spinner-svg">
          <circle
            cx="12"
            cy="12"
            r="9"
            stroke="currentColor"
            stroke-width="2.75"
            fill="none"
            stroke-linecap="round"
            stroke-dasharray="28 28"
          />
        </svg>
      </span>
      <slot v-else name="icon" />
    </span>

    <span
      ref="waveRef"
      aria-hidden="true"
      class="volur-btn__wave"
      :class="{ 'is-active': isWaveActive }"
    />
  </button>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onBeforeUnmount } from 'vue'
import type { CSSProperties } from 'vue'
import type { VButtonProps, VButtonEmits } from './types'

defineOptions({
  name: 'VButton',
  inheritAttrs: false
})

const props = withDefaults(defineProps<VButtonProps>(), {
  variant: 'normal',
  size: 'medium',
  radius: undefined,
  round: false,
  circle: false,
  dashed: false,
  block: false,
  disabled: false,
  loading: false,
  attrType: 'button',
  iconPlacement: 'left',
  isPressWave: false,
  isFocusRing: true
})

const emit = defineEmits<VButtonEmits>()

const isPresetSize = computed<boolean>(() => {
  return typeof props.size === 'string' && ['tiny', 'small', 'medium', 'large'].includes(props.size)
})

const computedStyle = computed<CSSProperties>(() => {
  const style: CSSProperties = {}

  if (props.radius !== undefined && props.radius !== null && props.radius !== '') {
    const r = String(props.radius).trim()
    style.borderRadius = /^\d+(\.\d+)?$/.test(r) ? `${r}px` : r
  }

  if (!isPresetSize.value && props.size !== undefined && props.size !== null && props.size !== '') {
    const s = String(props.size).trim()
    const sizeVal = /^\d+(\.\d+)?$/.test(s) ? `${s}px` : s
    style.height = sizeVal
    if (props.circle) {
      style.width = sizeVal
    }
  }

  return style
})

const buttonRef = ref<HTMLButtonElement | null>(null)
const waveRef = ref<HTMLSpanElement | null>(null)
const isWaveActive = ref(false)
let waveTimer: number | null = null

function playWave() {
  if (!props.isPressWave || props.variant === 'text' || props.disabled || props.loading) return
  if (waveTimer !== null) {
    window.clearTimeout(waveTimer)
    isWaveActive.value = false
    waveTimer = null
  }
  nextTick(() => {
    if (waveRef.value) {
      void waveRef.value.offsetWidth
    }
    isWaveActive.value = true
    waveTimer = window.setTimeout(() => {
      isWaveActive.value = false
      waveTimer = null
    }, 600)
  })
}

function handleClick(e: MouseEvent) {
  if (props.disabled || props.loading) {
    e.preventDefault()
    e.stopPropagation()
    return
  }
  playWave()
  emit('click', e)
}

function handleKeyDown(e: KeyboardEvent) {
  if (props.disabled || props.loading) return
  if (e.key === ' ' || e.key === 'Enter') {
    playWave()
  }
}

onBeforeUnmount(() => {
  if (waveTimer !== null) {
    window.clearTimeout(waveTimer)
  }
})

defineExpose({
  nativeEl: buttonRef,
  playWave
})
</script>

<style scoped lang="scss">
.volur-btn {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  font-family: 'Lato', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-weight: 500;
  line-height: 1;
  white-space: nowrap;
  vertical-align: middle;
  text-align: center;
  border-style: solid;
  border-width: 1px;
  border-color: transparent;
  cursor: pointer;
  user-select: none;
  outline: none;
  overflow: visible;
  text-decoration: none;
  -webkit-tap-highlight-color: transparent;
  transition:
    background-color 0.2s cubic-bezier(0.4, 0, 0.2, 1),
    border-color 0.2s cubic-bezier(0.4, 0, 0.2, 1),
    color 0.2s cubic-bezier(0.4, 0, 0.2, 1),
    box-shadow 0.2s cubic-bezier(0.4, 0, 0.2, 1),
    opacity 0.2s ease,
    transform 0.1s ease;

  &:active:not(.is-disabled) {
    transform: scale(0.985);
  }

  &.is-block {
    display: flex;
    width: 100%;
  }

  &.is-dashed {
    border-style: dashed;
  }

  &.is-disabled {
    cursor: not-allowed;
    opacity: 0.55;
    transform: none;
    pointer-events: none;
  }

  &.is-focus-ring {
    --volur-btn-focus-ring: light-dark(#1a73e8, #a8c7fa);

    &:focus-visible {
      box-shadow: 0 0 0 2px var(--color-v1-surface, #ffffff), 0 0 0 4px var(--volur-btn-focus-ring, light-dark(#1a73e8, #a8c7fa));
    }
  }

  &.volur-btn--primary {
    --volur-btn-ripple: light-dark(#1a73e8, #a8c7fa);
    background-color: var(--v-btn-primary-bg, light-dark(#1a73e8, #a8c7fa));
    color: var(--v-btn-primary-text, light-dark(#ffffff, #062e6f));
    border-color: transparent;

    &:hover:not(.is-disabled) {
      background-color: var(--v-btn-primary-hover, light-dark(#1765cc, #b6d0f8));
      box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.15), 0 1px 2px -1px rgba(0, 0, 0, 0.1);
    }

    &:active:not(.is-disabled) {
      background-color: var(--v-btn-primary-active, light-dark(#1557b0, #a8c7fa));
    }
  }

  &.volur-btn--primary-highest {
    --volur-btn-ripple: var(--color-v1-primary-highest);
    background-color: var(--color-v1-primary-highest);
    color: var(--color-v1-on-primary-highest);
    border-color: transparent;
    min-width: 88px;

    &:hover:not(.is-disabled) {
      background-color: light-dark(#0e4eb5, #b6d0f8);
      box-shadow: 0 1px 2px 0 rgba(60, 64, 67, 0.3), 0 1px 3px 1px rgba(60, 64, 67, 0.15);
    }

    &:active:not(.is-disabled) {
      background-color: light-dark(#063980, #a8c7fa);
      transform: scale(0.98);
      box-shadow: 0 1px 2px 0 rgba(60, 64, 67, 0.3);
    }
  }

  &.volur-btn--secondary {
    --volur-btn-ripple: var(--color-v1-primary-highest);
    background-color: transparent;
    color: var(--color-v1-primary-highest);
    border-color: var(--color-v1-outline);

    &:hover:not(.is-disabled) {
      background-color: light-dark(rgba(11, 87, 208, 0.08), rgba(168, 199, 250, 0.08));
    }

    &:active:not(.is-disabled) {
      background-color: light-dark(rgba(11, 87, 208, 0.16), rgba(168, 199, 250, 0.16));
    }
  }

  &.volur-btn--secondary-lowest {
    --volur-btn-ripple: light-dark(rgba(11, 87, 208, 0.25), rgba(168, 199, 250, 0.25));
    background-color: transparent;
    color: var(--color-v1-primary-highest);
    border-color: transparent;

    &:hover:not(.is-disabled) {
      background-color: light-dark(#f7f8fe, #141517);
      color: var(--color-v1-primary-highest);
    }

    &:active:not(.is-disabled) {
      background-color: light-dark(rgba(11, 87, 208, 0.16), #141517);
      transform: scale(0.985);
    }

    &.is-disabled {
      opacity: 0.38;
      background-color: transparent;
    }
  }

  &.volur-btn--normal {
    --volur-btn-ripple: light-dark(rgba(60, 64, 67, 0.25), rgba(255, 255, 255, 0.25));
    background-color: var(--color-v1-button-container);
    color: var(--color-v1-text-on-button);
    border-color: var(--color-v1-outline);
    line-height: 20px;
    letter-spacing: normal;

    &:hover:not(.is-disabled) {
      background-color: var(--color-v1-button-container-high);
    }

    &:focus,
    &:active:not(.is-disabled) {
      border-color: var(--color-v1-outline);
    }
  }

  &.volur-btn--tonal {
    --volur-btn-ripple: light-dark(#1a73e8, #a8c7fa);
    background-color: var(--color-v3-button-container-accent, light-dark(#dbeafe, #393f51));
    color: var(--color-v3-text-link, light-dark(#1a73e8, #a8c7fa));
    border-color: transparent;

    &:hover:not(.is-disabled) {
      background-color: var(--color-v3-button-container-accent-hover, light-dark(#bfdbfe, #454d63));
    }
  }

  &.volur-btn--danger {
    background-color: var(--color-v3-error-container, light-dark(#ffdad6, #402427));
    color: var(--color-v3-error-text, light-dark(#ba1a1a, #ffb4ab));
    border-color: var(--v-color-error, light-dark(#ea43354d, #f28b824d));

    &:hover:not(.is-disabled) {
      background-color: light-dark(#ffc9c2, #4d2b2f);
    }
  }

  &.volur-btn--warning {
    background-color: var(--color-v3-warning-container, light-dark(#ffddb7, #362f1a));
    color: var(--color-v3-warning-text, light-dark(#685708, #fdd663));
    border-color: var(--v-color-warning, light-dark(#fbbc044d, #fdd6634d));

    &:hover:not(.is-disabled) {
      background-color: light-dark(#ffd099, #423a20);
    }
  }

  &.volur-btn--text {
    background-color: transparent;
    color: var(--color-v3-text, light-dark(#202124, #e3e3e3));
    border-color: transparent;

    &:hover:not(.is-disabled) {
      background-color: var(--v-hover-subtle, light-dark(rgba(60, 64, 67, 0.08), rgba(255, 255, 255, 0.08)));
    }
  }

  &.volur-btn--tiny {
    height: 24px;
    padding: 0 6px;
    font-size: 11px;
    border-radius: 6px;
    gap: 4px;

    &.is-circle {
      width: 24px;
      padding: 0;
    }
  }

  &.volur-btn--small {
    height: 28px;
    padding: 0 8px;
    font-size: 12px;
    border-radius: 8px;
    gap: 4px;

    &.is-circle {
      width: 28px;
      padding: 0;
    }
  }

  &.volur-btn--medium {
    height: 36px;
    padding: 0 12px;
    font-size: 14px;
    border-radius: 12px;
    gap: 6px;

    &.is-circle {
      width: 36px;
      padding: 0;
    }
  }

  &.volur-btn--large {
    height: 40px;
    font-size: 0.875rem;
    border-radius: 12px;
    gap: 8px;

    &.volur-btn--primary,
    &.volur-btn--primary-highest {
      min-width: 88px;
      padding: 0 24px;
    }

    &.volur-btn--secondary,
    &.volur-btn--secondary-lowest {
      padding: 0 14px;
    }

    &.is-circle {
      min-width: 44px;
      width: 44px;
      padding: 0;
    }
  }

  &.is-pill {
    border-radius: 9999px;
  }

  &.is-circle {
    border-radius: 50%;
    padding: 0;
  }

  .volur-btn__icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    line-height: 1;
  }

  .volur-btn__content {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    line-height: inherit;
  }

  .volur-btn__spinner {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 1em;
    height: 1em;
    animation: volur-spin 0.8s linear infinite;

    .volur-btn__spinner-svg {
      width: 100%;
      height: 100%;
    }
  }

  .volur-btn__wave {
    position: absolute;
    inset: 0;
    border-radius: inherit;
    pointer-events: none;
    z-index: 1;

    &.is-active {
      animation:
        volur-wave-spread 0.6s cubic-bezier(0, 0, 0.2, 1) forwards,
        volur-wave-opacity 0.6s cubic-bezier(0, 0, 0.2, 1) forwards;
    }
  }
}

@keyframes volur-spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@keyframes volur-wave-spread {
  from {
    box-shadow: 0 0 0.5px 0 var(--volur-btn-ripple, currentColor);
  }
  to {
    box-shadow: 0 0 0.5px 5.5px var(--volur-btn-ripple, currentColor);
  }
}

@keyframes volur-wave-opacity {
  from {
    opacity: 0.65;
  }
  to {
    opacity: 0;
  }
}
</style>
