  <template>
  <n-modal
    :show="show"
    @update:show="handleUpdateShow"
    :mask-closable="false"
    preset="card"
    class="slide-puzzle-modal"
    :style="{ width: '360px', borderRadius: '12px' }"
  >
    <template #header>
      <div class="slide-header">
        <span class="slide-title">{{ t('auth.slide_puzzle_title') }}</span>
        <span class="slide-subtitle">{{ t('auth.slide_puzzle_tip') }}</span>
      </div>
    </template>

    <div class="slide-body" :class="{ 'is-fail': isFail, 'is-success': isSuccess }">
      <!-- Image Display Area -->
      <div class="puzzle-canvas-wrapper" ref="wrapperRef">
        <div v-if="loading" class="puzzle-loading-mask">
          <n-spin size="medium" />
        </div>

        <template v-else-if="captchaData">
          <!-- Background Image -->
          <img
            :src="formatBase64(captchaData.imageBase64)"
            alt="Puzzle Background"
            class="puzzle-bg-img"
          />

          <!-- Slider Piece Image -->
          <img
            v-if="captchaData.sliderBase64"
            :src="formatBase64(captchaData.sliderBase64)"
            alt="Slider Piece"
            class="puzzle-slider-piece"
            :style="{
              transform: `translateX(${sliderX}px)`
            }"
          />
        </template>

        <!-- Feedback Overlay -->
        <transition name="fade">
          <div v-if="isSuccess" class="puzzle-result-badge success">
            <span>✓ {{ t('auth.slide_puzzle_success') }}</span>
          </div>
          <div v-else-if="isFail" class="puzzle-result-badge fail">
            <span>✕ {{ t('auth.slide_puzzle_fail') }}</span>
          </div>
        </transition>
      </div>

      <!-- Action / Refresh Row below image on the right -->
      <div class="puzzle-refresh-bar">
        <n-button
          text
          size="tiny"
          :loading="loading"
          :disabled="isDragging || verifying || isSuccess"
          @click="fetchChallenge"
          class="puzzle-refresh-btn"
        >
          <template #icon>
            <n-icon size="14">
              <RefreshOutline />
            </n-icon>
          </template>
          {{ t('auth.slide_puzzle_refresh_tip') }}
        </n-button>
      </div>

      <!-- Dragging Track Area -->
      <div
        class="puzzle-track"
        ref="trackRef"
        :class="{ 'is-active': isDragging, 'is-success': isSuccess, 'is-fail': isFail }"
      >
        <div class="track-progress" :style="{ width: `${sliderX + 20}px` }" />
        <div class="track-tip" v-show="!isDragging && !verifying && !isSuccess">
          {{ t('auth.slide_puzzle_drag_tip') }}
        </div>
        <div class="track-tip verifying" v-show="verifying">
          {{ t('auth.slide_puzzle_verifying') }}
        </div>

        <!-- Draggable Handle -->
        <div
          class="track-handle"
          :style="{ transform: `translateX(${sliderX}px)` }"
          @mousedown="handleDragStart"
          @touchstart.passive="handleTouchStart"
        >
          <span v-if="isSuccess">✓</span>
          <span v-else-if="isFail">✕</span>
          <span v-else>➔</span>
        </div>
      </div>
    </div>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, watch, onBeforeUnmount } from 'vue'
import { NModal, NButton, NSpin, NIcon, useMessage } from 'naive-ui'
import { RefreshOutline } from '@vicons/ionicons5'
import { useI18n } from 'vue-i18n'
import { getCaptchaChallengeApi, verifyCaptchaTicketApi } from '@/api/verification'
import type { CaptchaResponseVO, CaptchaTicketVO } from '@/types/verification'

interface Props {
  show: boolean
  scene?: string
}

const props = withDefaults(defineProps<Props>(), {
  show: false,
  scene: 'LOGIN'
})

const emit = defineEmits<{
  (e: 'update:show', val: boolean): void
  (e: 'success', ticketVO: CaptchaTicketVO): void
  (e: 'fail'): void
}>()

const { t } = useI18n()
const message = useMessage()

const wrapperRef = ref<HTMLElement | null>(null)
const trackRef = ref<HTMLElement | null>(null)

const loading = ref<boolean>(false)
const verifying = ref<boolean>(false)
const isDragging = ref<boolean>(false)
const isSuccess = ref<boolean>(false)
const isFail = ref<boolean>(false)

const captchaData = ref<CaptchaResponseVO | null>(null)
const sliderX = ref<number>(0)
const startX = ref<number>(0)
const startTime = ref<number>(0)

const CANVAS_WIDTH = 320
const HANDLE_WIDTH = 40
const MAX_SLIDER_X = CANVAS_WIDTH - HANDLE_WIDTH

function formatBase64(raw: string | undefined): string {
  if (!raw) return ''
  if (raw.startsWith('data:image')) return raw
  return `data:image/png;base64,${raw}`
}

function handleUpdateShow(val: boolean) {
  emit('update:show', val)
}

async function fetchChallenge() {
  loading.value = true
  isSuccess.value = false
  isFail.value = false
  sliderX.value = 0

  try {
    const res = await getCaptchaChallengeApi({
      scene: props.scene,
      type: 'SLIDE_PUZZLE',
      oldCaptchaId: captchaData.value?.captchaId || undefined
    })
    captchaData.value = res.data
  } catch (err: any) {
    message.error(err.message || t('auth.failed_load_captcha'))
  } finally {
    loading.value = false
  }
}

// ----------------- Drag Event Handling -----------------

function startDrag(clientX: number) {
  if (loading.value || verifying.value || isSuccess.value) return
  isDragging.value = true
  isFail.value = false
  startX.value = clientX - sliderX.value
  startTime.value = Date.now()

  window.addEventListener('mousemove', onDragMove)
  window.addEventListener('mouseup', onDragEnd)
  window.addEventListener('touchmove', onTouchMove, { passive: false })
  window.addEventListener('touchend', onTouchEnd)
}

function handleDragStart(e: MouseEvent) {
  e.preventDefault()
  startDrag(e.clientX)
}

function handleTouchStart(e: TouchEvent) {
  if (e.touches.length > 0) {
    startDrag(e.touches[0].clientX)
  }
}

function updatePosition(clientX: number) {
  if (!isDragging.value) return
  let nextX = clientX - startX.value
  if (nextX < 0) nextX = 0
  if (nextX > MAX_SLIDER_X) nextX = MAX_SLIDER_X
  sliderX.value = nextX
}

function onDragMove(e: MouseEvent) {
  updatePosition(e.clientX)
}

function onTouchMove(e: TouchEvent) {
  if (e.touches.length > 0) {
    e.preventDefault()
    updatePosition(e.touches[0].clientX)
  }
}

async function onDragEnd() {
  if (!isDragging.value) return
  isDragging.value = false

  window.removeEventListener('mousemove', onDragMove)
  window.removeEventListener('mouseup', onDragEnd)
  window.removeEventListener('touchmove', onTouchMove)
  window.removeEventListener('touchend', onTouchEnd)

  if (!captchaData.value?.captchaId) return

  verifying.value = true
  const costTime = Date.now() - startTime.value

  try {
    const res = await verifyCaptchaTicketApi({
      captchaId: captchaData.value.captchaId,
      userInput: String(Math.round(sliderX.value)),
      scene: props.scene,
      costTime
    })

    isSuccess.value = true
    emit('success', res.data)

    setTimeout(() => {
      emit('update:show', false)
      verifying.value = false
      isSuccess.value = false
      sliderX.value = 0
    }, 600)
  } catch {
    isFail.value = true
    emit('fail')

    setTimeout(() => {
      isFail.value = false
      sliderX.value = 0
      verifying.value = false
      fetchChallenge()
    }, 800)
  }
}

function onTouchEnd() {
  onDragEnd()
}

watch(
  () => props.show,
  (val) => {
    if (val) {
      fetchChallenge()
    } else {
      isDragging.value = false
      isSuccess.value = false
      isFail.value = false
      sliderX.value = 0
    }
  }
)

onBeforeUnmount(() => {
  window.removeEventListener('mousemove', onDragMove)
  window.removeEventListener('mouseup', onDragEnd)
  window.removeEventListener('touchmove', onTouchMove)
  window.removeEventListener('touchend', onTouchEnd)
})
</script>

<style scoped lang="scss">
.slide-puzzle-modal {
  user-select: none;
}

.slide-header {
  display: flex;
  flex-direction: column;
  gap: 2px;

  .slide-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--v-text-primary);
  }

  .slide-subtitle {
    font-size: 12px;
    color: var(--v-text-desc);
  }
}

.slide-body {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 4px;
}

.puzzle-refresh-bar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-top: -4px;
  margin-bottom: -2px;

  .puzzle-refresh-btn {
    font-size: 12px;
    color: #444444;
    cursor: pointer;
    transition: color 0.2s ease;

    &:hover {
      color: var(--v-text-primary, #000000);
    }
  }
}

.puzzle-canvas-wrapper {
  position: relative;
  width: 320px;
  height: 160px;
  border-radius: 8px;
  overflow: hidden;
  background-color: var(--v-bg-muted);
  box-shadow: inset 0 0 4px rgba(0, 0, 0, 0.1);

  .puzzle-bg-img {
    width: 320px;
    height: 160px;
    display: block;
    object-fit: cover;
  }

  .puzzle-slider-piece {
    position: absolute;
    top: 0;
    left: 0;
    width: 320px;
    height: 160px;
    pointer-events: none;
    z-index: 5;
    filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.4));
  }

  .puzzle-loading-mask {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(255, 255, 255, 0.85);
    z-index: 10;
  }

  .puzzle-result-badge {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    height: 30px;
    display: flex;
    align-items: center;
    padding: 0 12px;
    font-size: 13px;
    font-weight: 500;
    z-index: 10;
    transition: all 0.2s ease;

    &.success {
      background-color: rgba(16, 185, 129, 0.9);
      color: #ffffff;
    }

    &.fail {
      background-color: rgba(239, 68, 68, 0.9);
      color: #ffffff;
    }
  }
}

.puzzle-track {
  position: relative;
  width: 320px;
  height: 40px;
  background-color: var(--v-bg-muted);
  border: 1px solid var(--v-border-base);
  border-radius: 20px;
  overflow: hidden;
  box-sizing: border-box;

  .track-progress {
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    background-color: rgba(99, 102, 241, 0.2);
    border-radius: 20px 0 0 20px;
    pointer-events: none;
    transition: background-color 0.2s;
  }

  .track-tip {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    color: var(--v-text-muted);
    pointer-events: none;

    &.verifying {
      color: var(--v-color-primary);
    }
  }

  .track-handle {
    position: absolute;
    top: 0;
    left: 0;
    width: 40px;
    height: 38px;
    background-color: var(--v-btn-primary-bg);
    color: var(--v-btn-primary-text);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    font-weight: 600;
    cursor: grab;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
    transition: background-color 0.2s, box-shadow 0.2s;
    z-index: 2;

    &:active {
      cursor: grabbing;
      box-shadow: 0 4px 10px rgba(0, 0, 0, 0.3);
    }
  }

  &.is-success {
    border-color: var(--v-color-success);

    .track-progress {
      background-color: rgba(16, 185, 129, 0.3);
    }

    .track-handle {
      background-color: var(--v-color-success);
      color: #ffffff;
    }
  }

  &.is-fail {
    border-color: var(--v-color-error);

    .track-progress {
      background-color: rgba(239, 68, 68, 0.3);
    }

    .track-handle {
      background-color: var(--v-color-error);
      color: #ffffff;
    }
  }
}

// Fail Shake Animation
.is-fail .puzzle-canvas-wrapper {
  animation: shake 0.4s ease-in-out;
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  20%, 60% { transform: translateX(-6px); }
  40%, 80% { transform: translateX(6px); }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
