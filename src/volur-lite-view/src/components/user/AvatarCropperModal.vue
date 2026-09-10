<template>
  <n-modal
    :show="show"
    preset="card"
    :title="t('profile.avatar_modal_title')"
    :bordered="false"
    class="avatar-cropper-modal"
    :style="{ width: '560px', maxWidth: '95vw' }"
    @update:show="(val: boolean) => emit('update:show', val)"
    @after-leave="resetState"
  >
    <div class="cropper-container">
      <div v-if="!imageSrc" class="upload-dropzone" @dragover.prevent @drop.prevent="handleFileDrop">
        <input
          ref="fileInputRef"
          type="file"
          accept="image/png,image/jpeg,image/jpg,image/webp"
          class="hidden-file-input"
          @change="handleFileChange"
        />
        <n-icon size="48" class="upload-icon">
          <CloudUploadOutline />
        </n-icon>
        <div class="upload-text">{{ t('profile.avatar_drop_hint') }}</div>
        <div class="upload-subtext">{{ t('profile.avatar_format_hint') }}</div>
        <n-button size="medium" secondary class="select-file-btn" style="pointer-events: none;">
          {{ t('profile.select_local_file') }}
        </n-button>
      </div>

      <div v-else class="editor-section">
        <div class="canvas-wrapper" @mousedown="startDrag" @mousemove="onDrag" @mouseup="endDrag" @mouseleave="endDrag" @wheel.prevent="handleWheel">
          <canvas ref="canvasRef" :width="canvasSize" :height="canvasSize" class="cropper-canvas"></canvas>
          <div class="crop-circle-overlay"></div>
        </div>

        <div class="cropper-controls">
          <div class="zoom-control">
            <n-icon size="18" class="zoom-icon" @click="zoomOut">
              <RemoveOutline />
            </n-icon>
            <n-slider
              :value="scale"
              :min="minScale"
              :max="maxScale"
              :step="0.005"
              :tooltip="false"
              class="zoom-slider"
              @update:value="handleSliderChange"
            />
            <n-icon size="18" class="zoom-icon" @click="zoomIn">
              <AddOutline />
            </n-icon>
          </div>

          <div class="action-buttons">
            <n-button size="small" quaternary @click="reselectImage">
              <template #icon>
                <n-icon><RefreshOutline /></n-icon>
              </template>
              {{ t('profile.reselect_image') }}
            </n-button>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="modal-footer">
        <n-button quaternary @click="emit('update:show', false)">
          {{ t('common.cancel') }}
        </n-button>
        <n-button
          type="primary"
          :disabled="!imageSrc"
          :loading="isUploading"
          class="confirm-btn"
          @click="handleCropAndUpload"
        >
          {{ t('profile.confirm_and_upload') }}
        </n-button>
      </div>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, reactive, nextTick } from 'vue'
import {
  NModal,
  NButton,
  NIcon,
  NSlider,
  useMessage
} from 'naive-ui'
import {
  CloudUploadOutline,
  AddOutline,
  RemoveOutline,
  RefreshOutline
} from '@vicons/ionicons5'
import { useI18n } from 'vue-i18n'
import { uploadUserAvatarApi } from '@/api/user'
import { setMemoryImageCache } from '@/utils/imageCache'

defineProps<{
  show: boolean
  currentAvatar?: string
}>()

const emit = defineEmits<{
  (e: 'update:show', value: boolean): void
  (e: 'success', avatarUrl: string): void
}>()

const { t } = useI18n()
const message = useMessage()

const canvasSize = 340
const fileInputRef = ref<HTMLInputElement | null>(null)
const canvasRef = ref<HTMLCanvasElement | null>(null)

const imageSrc = ref<string | null>(null)
const imageObj = ref<HTMLImageElement | null>(null)
const isUploading = ref(false)

const scale = ref(1)
const minScale = ref(0.2)
const maxScale = ref(3)

const position = reactive({ x: 0, y: 0 })
const isDragging = ref(false)
const dragStart = reactive({ x: 0, y: 0 })

function handleFileChange(e: Event) {
  const target = e.target as HTMLInputElement
  if (target.files && target.files[0]) {
    loadFile(target.files[0])
  }
  target.value = ''
}

function handleFileDrop(e: DragEvent) {
  if (e.dataTransfer?.files && e.dataTransfer.files[0]) {
    loadFile(e.dataTransfer.files[0])
  }
}

function loadFile(file: File) {
  if (!file.type.startsWith('image/')) {
    message.error(t('profile.invalid_image_type'))
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    message.error(t('profile.image_size_limit_5mb'))
    return
  }

  const reader = new FileReader()
  reader.onload = (event) => {
    const dataUrl = event.target?.result as string
    imageSrc.value = dataUrl
    const img = new Image()
    img.crossOrigin = 'anonymous'
    img.onload = () => {
      imageObj.value = img
      initCropperState(img)
      nextTick(() => renderCanvas())
    }
    img.src = dataUrl
  }
  reader.readAsDataURL(file)
}

function initCropperState(img: HTMLImageElement) {
  const baseScale = Math.max(canvasSize / img.width, canvasSize / img.height)
  minScale.value = baseScale
  scale.value = baseScale
  maxScale.value = baseScale * 4

  position.x = (canvasSize - img.width * baseScale) / 2
  position.y = (canvasSize - img.height * baseScale) / 2
  clampPosition()
}

function clampPosition() {
  if (!imageObj.value) return
  const w = imageObj.value.width * scale.value
  const h = imageObj.value.height * scale.value
  const minX = canvasSize - w
  const minY = canvasSize - h
  position.x = Math.min(0, Math.max(minX, position.x))
  position.y = Math.min(0, Math.max(minY, position.y))
}

function renderCanvas() {
  const canvas = canvasRef.value
  if (!canvas || !imageObj.value) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  ctx.clearRect(0, 0, canvasSize, canvasSize)
  ctx.save()
  ctx.drawImage(
    imageObj.value,
    position.x,
    position.y,
    imageObj.value.width * scale.value,
    imageObj.value.height * scale.value
  )
  ctx.restore()
}

function applyScale(newScale: number) {
  if (!imageObj.value) return
  const oldScale = scale.value
  const clampedScale = Math.min(maxScale.value, Math.max(minScale.value, newScale))
  if (Math.abs(oldScale - clampedScale) < 0.0001) return

  const centerX = canvasSize / 2
  const centerY = canvasSize / 2
  const ratio = clampedScale / oldScale

  position.x = centerX - (centerX - position.x) * ratio
  position.y = centerY - (centerY - position.y) * ratio
  scale.value = clampedScale
  clampPosition()
  renderCanvas()
}

function handleSliderChange(val: number) {
  applyScale(val)
}

function zoomIn() {
  applyScale(scale.value + (maxScale.value - minScale.value) * 0.08)
}

function zoomOut() {
  applyScale(scale.value - (maxScale.value - minScale.value) * 0.08)
}

function handleWheel(e: WheelEvent) {
  const step = (maxScale.value - minScale.value) * 0.05
  const delta = e.deltaY > 0 ? -step : step
  applyScale(scale.value + delta)
}

function startDrag(e: MouseEvent) {
  isDragging.value = true
  dragStart.x = e.clientX - position.x
  dragStart.y = e.clientY - position.y
}

function onDrag(e: MouseEvent) {
  if (!isDragging.value) return
  position.x = e.clientX - dragStart.x
  position.y = e.clientY - dragStart.y
  clampPosition()
  renderCanvas()
}

function endDrag() {
  isDragging.value = false
}

function reselectImage() {
  imageSrc.value = null
  imageObj.value = null
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
  }
}

function resetState() {
  imageSrc.value = null
  imageObj.value = null
  scale.value = 1
  position.x = 0
  position.y = 0
  isUploading.value = false
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
  }
}

async function handleCropAndUpload() {
  if (!canvasRef.value || !imageObj.value) return

  // Create an offscreen canvas for the circular crop output (256x256)
  const outputSize = 256
  const outputCanvas = document.createElement('canvas')
  outputCanvas.width = outputSize
  outputCanvas.height = outputSize
  const outCtx = outputCanvas.getContext('2d')
  if (!outCtx) return

  // Mapping from view canvas coordinates to output canvas
  const ratio = outputSize / canvasSize

  outCtx.save()
  outCtx.drawImage(
    imageObj.value,
    position.x * ratio,
    position.y * ratio,
    imageObj.value.width * scale.value * ratio,
    imageObj.value.height * scale.value * ratio
  )
  outCtx.restore()

  outputCanvas.toBlob(async (blob) => {
    if (!blob) {
      message.error(t('profile.crop_failed'))
      return
    }

    isUploading.value = true
    try {
      const formData = new FormData()
      formData.append('file', blob, 'avatar.png')
      const res = await uploadUserAvatarApi(formData)
      const localBlobUrl = URL.createObjectURL(blob)
      setMemoryImageCache(res.data, localBlobUrl)
      message.success(t('profile.avatar_update_success'))
      emit('success', res.data)
      emit('update:show', false)
    } catch (err: any) {
      message.error(err.message || t('profile.avatar_update_failed'))
    } finally {
      isUploading.value = false
    }
  }, 'image/png', 0.95)
}
</script>

<style scoped lang="scss">
.cropper-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 380px;
}

.upload-dropzone {
  position: relative;
  overflow: hidden;
  width: 100%;
  height: 320px;
  border: 2px dashed var(--v-border-base, #e5e4e7);
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  background-color: var(--v-bg-muted, #fbfbfb);
  transition: all 0.25s ease;

  .hidden-file-input {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
    opacity: 0;
    cursor: pointer;
    z-index: 5;
  }

  &:hover {
    border-color: #000000;
    background-color: rgba(0, 0, 0, 0.02);

    @include theme-dark {
      border-color: #ffffff;
      background-color: rgba(255, 255, 255, 0.04);
    }
  }

  .upload-icon {
    color: var(--v-text-desc, #666666);
    margin-bottom: 12px;
  }

  .upload-text {
    font-size: 15px;
    font-weight: 600;
    color: var(--v-text-primary, #111111);
    margin-bottom: 6px;
  }

  .upload-subtext {
    font-size: 13px;
    color: var(--v-text-desc, #444444);
    margin-bottom: 20px;
  }

  .select-file-btn {
    border-radius: 6px;
    font-weight: 500;
  }
}

.editor-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
}

.canvas-wrapper {
  position: relative;
  width: 340px;
  height: 340px;
  border-radius: 12px;
  overflow: hidden;
  background-color: #1a1a1a;
  cursor: grab;
  user-select: none;
  box-shadow: inset 0 0 20px rgba(0, 0, 0, 0.5);

  &:active {
    cursor: grabbing;
  }

  .cropper-canvas {
    display: block;
    width: 100%;
    height: 100%;
  }

  .crop-circle-overlay {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    pointer-events: none;
    box-shadow: 0 0 0 9999px rgba(0, 0, 0, 0.55);
    border-radius: 50%;
    border: 2px solid rgba(255, 255, 255, 0.85);
  }
}

.cropper-controls {
  width: 340px;
  margin-top: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;

  .zoom-control {
    display: flex;
    align-items: center;
    gap: 10px;
    width: 200px;

    .zoom-icon {
      cursor: pointer;
      color: var(--v-text-desc, #666666);
      transition: color 0.2s;

      &:hover {
        color: var(--v-text-primary, #111111);
      }
    }

    .zoom-slider {
      flex: 1;
    }
  }
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  width: 100%;

  .confirm-btn {
    background-color: var(--v-btn-primary-bg, #000000);
    color: var(--v-btn-primary-text, #ffffff);
    font-weight: 500;
    border-radius: 6px;

    &:hover {
      background-color: var(--v-btn-primary-hover, #222222);
    }
  }
}
</style>
