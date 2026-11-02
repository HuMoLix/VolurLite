<template>
  <teleport to="body">
    <transition name="rc-modal-fade">
      <div v-if="visible" class="rc-modal-backdrop" @click.self="handleClose">
        <div class="rc-modal-card">
          <div class="rc-modal-header">
            <span class="rc-modal-title">{{ displayTitle }}</span>
            <div class="rc-modal-tools">
              <button
                type="button"
                class="rc-tool-btn"
                :title="t('captcha.recaptcha.refresh')"
                :disabled="loading"
                @click="emit('refresh')"
              >
                <svg viewBox="0 0 24 24" class="w-4 h-4 fill-current">
                  <path d="M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"/>
                </svg>
              </button>
              <button
                type="button"
                class="rc-tool-btn"
                :title="t('common.close')"
                @click="handleClose"
              >
                <svg viewBox="0 0 24 24" class="w-4 h-4 fill-current">
                  <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12 19 6.41z"/>
                </svg>
              </button>
            </div>
          </div>

          <div class="rc-modal-body">
            <div v-if="challengeType === 'PUZZLE'" class="rc-puzzle-container">
              <div class="rc-puzzle-canvas-wrap" ref="canvasWrapRef">
                <img
                  v-if="backgroundImage"
                  :src="formattedBgImage"
                  class="rc-puzzle-bg"
                  alt="Puzzle background"
                  draggable="false"
                />
                <img
                  v-if="puzzleImage"
                  :src="formattedPuzzleImage"
                  class="rc-puzzle-piece"
                  :style="puzzlePieceStyle"
                  alt="Puzzle piece"
                  draggable="false"
                />
              </div>

              <div
                class="rc-slider-track"
                ref="trackRef"
                @mousedown="handleDragStart"
                @touchstart="handleTouchStart"
              >
                <div class="rc-slider-track-highlight" :style="{ width: sliderProgress + 'px' }" />
                <div class="rc-slider-track-text" :class="{ 'opacity-0': isDragging || sliderProgress > 20 }">
                  {{ t('captcha.recaptcha.drag_slider') }}
                </div>
                <div
                  class="rc-slider-thumb"
                  :style="{ transform: `translateX(${sliderProgress}px)` }"
                  :class="{ 'is-dragging': isDragging }"
                >
                  <svg viewBox="0 0 24 24" class="w-4 h-4 fill-current">
                    <path d="M8.59 16.59L13.17 12 8.59 7.41 10 6l6 6-6 6-1.41-1.41z"/>
                  </svg>
                </div>
              </div>
            </div>

            <div v-else-if="challengeType === 'IMAGE'" class="rc-image-container">
              <div class="rc-image-wrap" @click="emit('refresh')" :title="t('captcha.recaptcha.click_to_refresh')">
                <img
                  v-if="captchaImage"
                  :src="formattedCaptchaImage"
                  class="rc-captcha-img"
                  alt="Captcha"
                  draggable="false"
                />
              </div>
              <div class="rc-image-input-wrap">
                <input
                  type="text"
                  v-model="imageInput"
                  class="rc-image-input"
                  :placeholder="t('captcha.recaptcha.enter_image_text')"
                  maxlength="8"
                  autofocus
                  @keydown.enter="handleImageSubmit"
                />
              </div>
            </div>

            <div v-if="errorMsg" class="rc-modal-error">
              <svg viewBox="0 0 24 24" class="w-3.5 h-3.5 fill-current shrink-0">
                <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/>
              </svg>
              <span>{{ errorMsg }}</span>
            </div>
          </div>

          <div class="rc-modal-footer">
            <span class="rc-brand-text">Volur reCAPTCHA</span>
            <button
              type="button"
              class="rc-submit-btn"
              :disabled="loading || isSubmitDisabled"
              @click="handleModalSubmit"
            >
              <span v-if="loading" class="rc-spinner" />
              <span v-else>{{ t('common.verify') }}</span>
            </button>
          </div>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<script setup lang="ts">
import { ref, computed, watch, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'

defineOptions({
  name: 'RecaptchaModal'
})

const { t } = useI18n()

const props = withDefaults(
  defineProps<{
    visible: boolean
    challengeType: 'CLICK' | 'PUZZLE' | 'SLIDER' | 'IMAGE' | 'SELECT_IMAGE'
    backgroundImage?: string
    puzzleImage?: string
    puzzleY?: number
    captchaImage?: string
    promptText?: string
    loading?: boolean
    errorMsg?: string
  }>(),
  {
    visible: false,
    challengeType: 'PUZZLE',
    backgroundImage: '',
    puzzleImage: '',
    puzzleY: 0,
    captchaImage: '',
    promptText: '',
    loading: false,
    errorMsg: ''
  }
)

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
  (e: 'close'): void
  (e: 'refresh'): void
  (e: 'submit', result: { x?: number; code?: string; userInput?: string }): void
}>()

const trackRef = ref<HTMLDivElement | null>(null)
const canvasWrapRef = ref<HTMLDivElement | null>(null)
const isDragging = ref(false)
const dragStartX = ref(0)
const sliderProgress = ref(0)
const imageInput = ref('')

const displayTitle = computed(() => {
  if (props.promptText) return props.promptText
  if (props.challengeType === 'PUZZLE') return t('captcha.recaptcha.puzzle_title')
  if (props.challengeType === 'IMAGE') return t('captcha.recaptcha.image_title')
  return t('auth.security_verification')
})

const formattedBgImage = computed(() => {
  if (!props.backgroundImage) return ''
  return props.backgroundImage.startsWith('data:')
    ? props.backgroundImage
    : `data:image/png;base64,${props.backgroundImage}`
})

const formattedPuzzleImage = computed(() => {
  if (!props.puzzleImage) return ''
  return props.puzzleImage.startsWith('data:')
    ? props.puzzleImage
    : `data:image/png;base64,${props.puzzleImage}`
})

const formattedCaptchaImage = computed(() => {
  if (!props.captchaImage) return ''
  return props.captchaImage.startsWith('data:')
    ? props.captchaImage
    : `data:image/png;base64,${props.captchaImage}`
})

const puzzlePieceStyle = computed(() => {
  const top = props.puzzleY ? `${props.puzzleY}px` : '0px'
  return {
    top,
    transform: `translateX(${sliderProgress.value}px)`
  }
})

const isSubmitDisabled = computed(() => {
  if (props.challengeType === 'PUZZLE') {
    return sliderProgress.value < 10
  }
  if (props.challengeType === 'IMAGE') {
    return !imageInput.value.trim()
  }
  return false
})

watch(() => props.visible, (val) => {
  if (val) {
    resetState()
  }
})

function resetState() {
  sliderProgress.value = 0
  isDragging.value = false
  imageInput.value = ''
}

function handleClose() {
  emit('update:visible', false)
  emit('close')
}

function getMaxSlide(): number {
  if (!trackRef.value) return 260
  return trackRef.value.clientWidth - 42
}

function handleDragStart(e: MouseEvent) {
  if (props.loading) return
  isDragging.value = true
  dragStartX.value = e.clientX - sliderProgress.value
  window.addEventListener('mousemove', handleDragMove)
  window.addEventListener('mouseup', handleDragEnd)
}

function handleDragMove(e: MouseEvent) {
  if (!isDragging.value) return
  const max = getMaxSlide()
  let next = e.clientX - dragStartX.value
  if (next < 0) next = 0
  if (next > max) next = max
  sliderProgress.value = next
}

function handleDragEnd() {
  if (!isDragging.value) return
  isDragging.value = false
  window.removeEventListener('mousemove', handleDragMove)
  window.removeEventListener('mouseup', handleDragEnd)
}

function handleTouchStart(e: TouchEvent) {
  if (props.loading || !e.touches[0]) return
  isDragging.value = true
  dragStartX.value = e.touches[0].clientX - sliderProgress.value
  window.addEventListener('touchmove', handleTouchMove, { passive: false })
  window.addEventListener('touchend', handleTouchEnd)
}

function handleTouchMove(e: TouchEvent) {
  if (!isDragging.value || !e.touches[0]) return
  e.preventDefault()
  const max = getMaxSlide()
  let next = e.touches[0].clientX - dragStartX.value
  if (next < 0) next = 0
  if (next > max) next = max
  sliderProgress.value = next
}

function handleTouchEnd() {
  if (!isDragging.value) return
  isDragging.value = false
  window.removeEventListener('touchmove', handleTouchMove)
  window.removeEventListener('touchend', handleTouchEnd)
}

function handleImageSubmit() {
  if (isSubmitDisabled.value || props.loading) return
  handleModalSubmit()
}

function handleModalSubmit() {
  if (props.challengeType === 'PUZZLE') {
    emit('submit', {
      x: Math.round(sliderProgress.value),
      userInput: String(Math.round(sliderProgress.value))
    })
  } else if (props.challengeType === 'IMAGE') {
    emit('submit', {
      code: imageInput.value.trim(),
      userInput: imageInput.value.trim()
    })
  } else {
    emit('submit', {
      x: Math.round(sliderProgress.value)
    })
  }
}

onUnmounted(() => {
  window.removeEventListener('mousemove', handleDragMove)
  window.removeEventListener('mouseup', handleDragEnd)
  window.removeEventListener('touchmove', handleTouchMove)
  window.removeEventListener('touchend', handleTouchEnd)
})
</script>

<style scoped>
.rc-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(2px);
  padding: 16px;
}

.rc-modal-card {
  width: 100%;
  max-width: 348px;
  background-color: #ffffff;
  border-radius: 8px;
  border: 1px solid #dadce0;
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.28);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

html.dark .rc-modal-card {
  background-color: #292a2d;
  border-color: #3c4043;
  box-shadow: 0 16px 32px rgba(0, 0, 0, 0.5);
}

.rc-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-bottom: 1px solid #f1f3f4;
}

html.dark .rc-modal-header {
  border-bottom-color: #3c4043;
}

.rc-modal-title {
  font-size: 14px;
  font-weight: 500;
  color: #202124;
}

html.dark .rc-modal-title {
  color: #e8eaed;
}

.rc-modal-tools {
  display: flex;
  align-items: center;
  gap: 6px;
}

.rc-tool-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: none;
  background: transparent;
  color: #5f6368;
  cursor: pointer;
  transition: background-color 0.15s ease, color 0.15s ease;
}

html.dark .rc-tool-btn {
  color: #bdc1c6;
}

.rc-tool-btn:hover {
  background-color: #f1f3f4;
  color: #202124;
}

html.dark .rc-tool-btn:hover {
  background-color: #3c4043;
  color: #e8eaed;
}

.rc-modal-body {
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.rc-puzzle-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.rc-puzzle-canvas-wrap {
  position: relative;
  width: 100%;
  height: 155px;
  border-radius: 4px;
  overflow: hidden;
  background-color: #f1f3f4;
}

html.dark .rc-puzzle-canvas-wrap {
  background-color: #202124;
}

.rc-puzzle-bg {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.rc-puzzle-piece {
  position: absolute;
  left: 0;
  height: 100%;
  object-fit: contain;
  pointer-events: none;
  will-change: transform;
}

.rc-slider-track {
  position: relative;
  height: 40px;
  border-radius: 20px;
  background-color: #f1f3f4;
  border: 1px solid #dadce0;
  display: flex;
  align-items: center;
  user-select: none;
  cursor: pointer;
  overflow: hidden;
}

html.dark .rc-slider-track {
  background-color: #202124;
  border-color: #3c4043;
}

.rc-slider-track-highlight {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  background-color: rgba(26, 115, 232, 0.15);
  pointer-events: none;
}

html.dark .rc-slider-track-highlight {
  background-color: rgba(138, 180, 248, 0.2);
}

.rc-slider-track-text {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #5f6368;
  pointer-events: none;
  transition: opacity 0.2s ease;
}

html.dark .rc-slider-track-text {
  color: #bdc1c6;
}

.rc-slider-thumb {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #1a73e8;
  color: #ffffff;
  border-radius: 20px;
  cursor: grab;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.25);
  transition: box-shadow 0.15s ease, background-color 0.15s ease;
  will-change: transform;
}

html.dark .rc-slider-thumb {
  background-color: #8ab4f8;
  color: #202124;
}

.rc-slider-thumb.is-dragging {
  cursor: grabbing;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.35);
}

.rc-image-container {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.rc-image-wrap {
  width: 100%;
  height: 72px;
  background-color: #f8f9fa;
  border: 1px solid #dadce0;
  border-radius: 4px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

html.dark .rc-image-wrap {
  background-color: #202124;
  border-color: #3c4043;
}

.rc-captcha-img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.rc-image-input-wrap {
  width: 100%;
}

.rc-image-input {
  width: 100%;
  height: 40px;
  border-radius: 4px;
  border: 1px solid #dadce0;
  background-color: #ffffff;
  padding: 0 12px;
  font-size: 14px;
  color: #202124;
  outline: none;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

html.dark .rc-image-input {
  border-color: #3c4043;
  background-color: #202124;
  color: #e8eaed;
}

.rc-image-input:focus {
  border-color: #1a73e8;
  box-shadow: 0 0 0 2px rgba(26, 115, 232, 0.2);
}

html.dark .rc-image-input:focus {
  border-color: #8ab4f8;
  box-shadow: 0 0 0 2px rgba(138, 180, 248, 0.2);
}

.rc-modal-error {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #ea4335;
}

html.dark .rc-modal-error {
  color: #f28b82;
}

.rc-modal-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px 12px;
  border-top: 1px solid #f1f3f4;
}

html.dark .rc-modal-footer {
  border-top-color: #3c4043;
}

.rc-brand-text {
  font-size: 11px;
  color: #5f6368;
}

html.dark .rc-brand-text {
  color: #bdc1c6;
}

.rc-submit-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 72px;
  height: 36px;
  padding: 0 16px;
  border-radius: 4px;
  border: none;
  background-color: #1a73e8;
  color: #ffffff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.15s ease, opacity 0.15s ease;
}

html.dark .rc-submit-btn {
  background-color: #8ab4f8;
  color: #202124;
}

.rc-submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.rc-submit-btn:not(:disabled):hover {
  background-color: #1765cc;
}

html.dark .rc-submit-btn:not(:disabled):hover {
  background-color: #7aa7f7;
}

.rc-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid transparent;
  border-top-color: currentColor;
  border-radius: 50%;
  animation: rcSpin 0.7s linear infinite;
}

@keyframes rcSpin {
  to {
    transform: rotate(360deg);
  }
}

.rc-modal-fade-enter-active,
.rc-modal-fade-leave-active {
  transition: opacity 0.2s ease;
}

.rc-modal-fade-enter-from,
.rc-modal-fade-leave-to {
  opacity: 0;
}
</style>
