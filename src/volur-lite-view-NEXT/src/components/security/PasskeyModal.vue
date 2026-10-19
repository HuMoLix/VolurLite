<template>
  <teleport to="body">
    <transition name="passkey-modal-fade">
      <div v-if="visible" class="passkey-modal-backdrop" @click.self="handleClose">
        <div class="passkey-modal-card" :class="{ 'is-loading': isScrimLoading }">
          <div
            class="passkey-modal__progress"
            :class="{ 'is-active': isScrimLoading }"
            role="progressbar"
            :aria-label="t('common.processing')"
            :aria-hidden="!isScrimLoading"
          >
            <div class="passkey-modal__progress-track" />
            <div class="passkey-modal__progress-bar passkey-modal__progress-bar--primary">
              <span class="passkey-modal__progress-inner" />
            </div>
            <div class="passkey-modal__progress-bar passkey-modal__progress-bar--secondary">
              <span class="passkey-modal__progress-inner" />
            </div>
          </div>

          <div
            v-if="isScrimLoading"
            class="passkey-modal__blocker"
            aria-hidden="true"
            @click.stop.prevent
            @mousedown.stop.prevent
            @dblclick.stop.prevent
            @keydown.stop.prevent
            @touchstart.stop.prevent
          />

          <div class="passkey-modal-header">
            <h2 class="passkey-modal-title">
              {{ displayTitle }}
            </h2>
          </div>

          <div class="passkey-modal-body">
            <div class="passkey-modal-illustration-wrap">
              <img
                src="/static/svg/passkeys_scene_add_account.svg"
                alt="Passkey Illustration"
                class="passkey-modal-illustration"
                draggable="false"
              />
            </div>
            <p class="passkey-modal-description">
              {{ displayDescription }}
            </p>
            <div v-if="displayError" class="passkey-modal-error">
              <svg viewBox="0 0 24 24" class="w-4 h-4 fill-current shrink-0">
                <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/>
              </svg>
              <span>{{ displayError }}</span>
            </div>
          </div>

          <div class="passkey-modal-footer">
            <n-button
              quaternary
              type="primary"
              :round="true"
              class="auth-action-btn btn-create-account"
              :disabled="isScrimLoading"
              :isScrim="true"
              @click="handleClose"
            >
              {{ displayCancelText }}
            </n-button>
            <n-button
              quaternary
              type="primary"
              :round="true"
              class="auth-action-btn btn-create-account"
              :disabled="isScrimLoading"
              :isScrim="true"
              @click="handleOtherDevice"
            >
              {{ displayOtherDeviceText }}
            </n-button>
            <n-button
              type="primary"
              class="auth-action-btn btn-next-step"
              :disabled="isScrimLoading"
              :isScrim="true"
              @click="handlePrimaryAction"
            >
              {{ displayPrimaryText }}
            </n-button>
          </div>
        </div>
      </div>
    </transition>
  </teleport>

  <Passkey
    ref="passkeyRef"
    :dsh="dsh"
    :tl="tl"
    :username="username"
    @success="handlePasskeySuccess"
    @error="handlePasskeyError"
    @cancel="handlePasskeyCancel"
  />
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { NButton } from 'naive-ui'
import { useI18n } from 'vue-i18n'
import Passkey from './passkey.vue'

defineOptions({
  name: 'PasskeyModal'
})

interface PasskeyModalProps {
  visible?: boolean
  mode?: 'create' | 'use' | 'verify'
  title?: string
  description?: string
  cancelText?: string
  otherDeviceText?: string
  primaryText?: string
  dsh?: string
  tl?: string
  username?: string
}

const props = withDefaults(defineProps<PasskeyModalProps>(), {
  visible: false,
  mode: 'create',
  title: '',
  description: '',
  cancelText: '',
  otherDeviceText: '',
  primaryText: '',
  dsh: '',
  tl: '',
  username: ''
})

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'close'): void
  (e: 'cancel'): void
  (e: 'success', data: any): void
  (e: 'error', error: string): void
}>()

const { t } = useI18n()
const passkeyRef = ref<InstanceType<typeof Passkey> | null>(null)
const errorKey = ref('')
const rawErrorMsg = ref('')

const isScrimLoading = ref(false)

const displayTitle = computed(() => {
  if (props.title) return props.title
  return props.mode === 'create'
    ? t('auth.passkey_modal_create_title')
    : t('auth.passkey_modal_use_title')
})

const displayDescription = computed(() => {
  if (props.description) return props.description
  return props.mode === 'create'
    ? t('auth.passkey_modal_create_desc')
    : t('auth.passkey_modal_use_desc')
})

const displayPrimaryText = computed(() => {
  if (props.primaryText) return props.primaryText
  return props.mode === 'create'
    ? t('auth.passkey_modal_create_btn')
    : t('auth.passkey_modal_use_btn')
})

const displayOtherDeviceText = computed(() => {
  if (props.otherDeviceText) return props.otherDeviceText
  return t('auth.passkey_modal_choose_other_device')
})

const displayCancelText = computed(() => {
  if (props.cancelText) return props.cancelText
  return t('common.cancel')
})

const displayError = computed(() => {
  if (errorKey.value) {
    return t(errorKey.value)
  }
  return rawErrorMsg.value
})

function clearError(): void {
  errorKey.value = ''
  rawErrorMsg.value = ''
}

async function runWithScrim(action: () => Promise<void> | void): Promise<void> {
  if (isScrimLoading.value) return
  isScrimLoading.value = true
  const startTime = Date.now()
  try {
    await action()
  } finally {
    const elapsed = Date.now() - startTime
    if (elapsed < 1000) {
      await new Promise(resolve => setTimeout(resolve, 1000 - elapsed))
    }
    isScrimLoading.value = false
  }
}

async function handleClose(): Promise<void> {
  if (isScrimLoading.value) return
  clearError()
  await runWithScrim(async () => {
    emit('update:visible', false)
    emit('close')
    emit('cancel')
  })
}

async function handleOtherDevice(): Promise<void> {
  if (isScrimLoading.value) return
  clearError()
  await runWithScrim(async () => {
    try {
      if (props.mode === 'create') {
        await passkeyRef.value?.createPasskey({
          attachment: 'cross-platform',
          dsh: props.dsh,
          tl: props.tl,
          username: props.username
        })
      } else {
        await passkeyRef.value?.verifyPasskey({
          attachment: 'cross-platform',
          dsh: props.dsh,
          tl: props.tl
        })
      }
    } catch {
    }
  })
}

async function handlePrimaryAction(): Promise<void> {
  if (isScrimLoading.value) return
  clearError()
  await runWithScrim(async () => {
    try {
      if (props.mode === 'create') {
        await passkeyRef.value?.createPasskey({
          attachment: 'platform',
          dsh: props.dsh,
          tl: props.tl,
          username: props.username
        })
      } else {
        await passkeyRef.value?.verifyPasskey({
          attachment: 'platform',
          dsh: props.dsh,
          tl: props.tl
        })
      }
    } catch {
    }
  })
}

function handlePasskeySuccess(data: any): void {
  emit('success', data)
  emit('update:visible', false)
}

function handlePasskeyError(error: string): void {
  if (error.startsWith('auth.') || error.startsWith('common.')) {
    errorKey.value = error
    rawErrorMsg.value = ''
  } else {
    rawErrorMsg.value = error
    errorKey.value = ''
  }
  emit('error', error)
}

function handlePasskeyCancel(): void {
  errorKey.value = 'auth.passkey_cancelled'
  emit('cancel')
}
</script>

<style scoped lang="scss">
.passkey-modal-fade-enter-active,
.passkey-modal-fade-leave-active {
  transition: opacity 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.passkey-modal-fade-enter-from,
.passkey-modal-fade-leave-to {
  opacity: 0;
}

.passkey-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 1050;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(0, 0, 0, 0.6);
  padding: 16px;
  backdrop-filter: blur(2px);
}

.passkey-modal-card {
  position: relative;
  width: 100%;
  max-width: 480px;
  background-color: #e9eef6;
  border-radius: 28px;
  padding: 32px 36px;
  box-shadow: 0 24px 48px -12px rgba(0, 0, 0, 0.18), 0 8px 16px -8px rgba(0, 0, 0, 0.12);
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  animation: modal-pop 0.25s cubic-bezier(0.16, 1, 0.3, 1);
  overflow: hidden;

  &.is-loading {
    user-select: none;
  }
}

html.dark .passkey-modal-card {
  background-color: #282a2d;
  box-shadow: 0 24px 48px -12px rgba(0, 0, 0, 0.4), 0 8px 16px -8px rgba(0, 0, 0, 0.3);
}

@keyframes modal-pop {
  0% {
    opacity: 0;
    transform: scale(0.92) translateY(8px);
  }
  100% {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.passkey-modal__progress {
  position: absolute;
  top: 0;
  left: 28px;
  right: 28px;
  height: 4px;
  overflow: hidden;
  border-top-left-radius: 28px;
  border-top-right-radius: 28px;
  z-index: 30;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.25s cubic-bezier(0.4, 0, 0.2, 1);

  &.is-active {
    opacity: 1;
  }

  &-track {
    position: absolute;
    inset: 0;
    background-color: #e8f0fe;
    border-radius: inherit;
  }

  &-bar {
    position: absolute;
    top: 0;
    bottom: 0;
    width: 100%;
    transform-origin: top left;
    will-change: transform;

    &--primary {
      left: -145.166611%;
    }

    &--secondary {
      left: -54.888891%;
    }
  }

  &-inner {
    display: inline-block;
    position: absolute;
    inset: 0;
    background-color: #0b57d0;
    border-radius: inherit;
    will-change: transform;
  }

  &.is-active {
    .passkey-modal__progress-bar--primary {
      animation: mdc-linear-progress-primary-indeterminate-translate 2s linear infinite;

      .passkey-modal__progress-inner {
        animation: mdc-linear-progress-primary-indeterminate-scale 2s linear infinite;
      }
    }

    .passkey-modal__progress-bar--secondary {
      animation: mdc-linear-progress-secondary-indeterminate-translate 2s linear infinite;

      .passkey-modal__progress-inner {
        animation: mdc-linear-progress-secondary-indeterminate-scale 2s linear infinite;
      }
    }
  }
}

html.dark .passkey-modal__progress {
  &-track {
    background-color: #29384d;
  }
  &-inner {
    background-color: #8ab4f8;
  }
}

.passkey-modal__blocker {
  position: absolute;
  inset: 0;
  z-index: 20;
  background-color: rgba(233, 238, 246, 0.6);
  backdrop-filter: blur(0.1px);
  cursor: wait;
  pointer-events: all;
  border-radius: inherit;
  transition: opacity 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

html.dark .passkey-modal__blocker {
  background-color: rgba(0, 0, 0, 0.6);
}

@keyframes mdc-linear-progress-primary-indeterminate-translate {
  0% {
    transform: translateX(0);
  }
  20% {
    animation-timing-function: cubic-bezier(0.5, 0, 0.701732, 0.495819);
    transform: translateX(0);
  }
  59.15% {
    animation-timing-function: cubic-bezier(0.302435, 0.381352, 0.55, 0.956);
    transform: translateX(83.67142%);
  }
  100% {
    transform: translateX(200.611057%);
  }
}

@keyframes mdc-linear-progress-primary-indeterminate-scale {
  0% {
    transform: scaleX(0.08);
  }
  36.65% {
    animation-timing-function: cubic-bezier(0.334731, 0.12482, 0.785844, 1);
    transform: scaleX(0.08);
  }
  69.15% {
    animation-timing-function: cubic-bezier(0.06, 0.11, 0.6, 1);
    transform: scaleX(0.661479);
  }
  100% {
    transform: scaleX(0.08);
  }
}

@keyframes mdc-linear-progress-secondary-indeterminate-translate {
  0% {
    animation-timing-function: cubic-bezier(0.15, 0, 0.515058, 0.409685);
    transform: translateX(0);
  }
  25% {
    animation-timing-function: cubic-bezier(0.31033, 0.284058, 0.8, 0.73372);
    transform: translateX(37.651913%);
  }
  48.35% {
    animation-timing-function: cubic-bezier(0.4, 0.627035, 0.6, 0.902026);
    transform: translateX(84.386165%);
  }
  100% {
    transform: translateX(160.277782%);
  }
}

@keyframes mdc-linear-progress-secondary-indeterminate-scale {
  0% {
    animation-timing-function: cubic-bezier(0.205028, 0.057051, 0.57661, 0.453971);
    transform: scaleX(0.08);
  }
  19.15% {
    animation-timing-function: cubic-bezier(0.152442, 0.179435, 0.7, 0.72);
    transform: scaleX(0.457104);
  }
  44.15% {
    animation-timing-function: cubic-bezier(0.257759, 0.351025, 0.5, 0.89);
    transform: scaleX(0.72796);
  }
  100% {
    transform: scaleX(0.08);
  }
}

.passkey-modal-header {
  margin-bottom: 20px;
}

.passkey-modal-title {
  font-size: 24px;
  line-height: 32px;
  font-weight: 500;
  color: #1f1f1f;
  margin: 0;
}

html.dark .passkey-modal-title {
  color: #e8eaed;
}

.passkey-modal-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 28px;
}

.passkey-modal-illustration-wrap {
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 20px;
}

.passkey-modal-illustration {
  width: 100%;
  max-width: 316px;
  height: 144px;
  object-fit: contain;
  user-select: none;
}

.passkey-modal-description {
  font-size: 14px;
  line-height: 20px;
  color: #444746;
  text-align: left;
  width: 100%;
  margin: 0;
}

html.dark .passkey-modal-description {
  color: #c4c7c5;
}

.passkey-modal-error {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  width: 100%;
  font-size: 13px;
  color: #b3261e;
}

html.dark .passkey-modal-error {
  color: #f28b82;
}

.passkey-modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  width: 100%;
}
</style>
