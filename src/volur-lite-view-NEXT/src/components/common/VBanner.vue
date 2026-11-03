<template>
  <div
    class="v-banner"
    :class="[
      currentTheme,
      {
        'is-editable': editable,
        'is-uploading': uploading,
        'has-image': Boolean(effectiveSrc && !hasError)
      }
    ]"
    :style="bannerStyle"
    @click="handleClick"
  >
    <div v-if="effectiveSrc && !hasError" class="v-banner-img-wrap">
      <img
        :src="effectiveSrc"
        :alt="alt"
        class="v-banner-img"
        @error="handleError"
      />
      <div v-if="editable" class="v-banner-overlay">
        <n-spin v-if="uploading" :size="24" stroke="#ffffff" />
        <div v-else class="v-banner-overlay-content">
          <n-icon :size="22" class="text-white">
            <CameraOutline />
          </n-icon>
          <span class="text-xs text-white font-medium mt-1">更换横幅封面</span>
        </div>
      </div>
      <div class="v-banner-actions">
        <button
          type="button"
          class="v-banner-action-btn"
          title="查看原图"
          @click.stop="openPreview"
        >
          <n-icon :size="15">
            <EyeOutline />
          </n-icon>
        </button>
        <button
          v-if="clearable && !uploading"
          type="button"
          class="v-banner-action-btn danger"
          title="移除横幅"
          @click.stop="handleClear"
        >
          <n-icon :size="14">
            <TrashOutline />
          </n-icon>
        </button>
      </div>
    </div>

    <div v-else class="v-banner-empty">
      <n-spin v-if="uploading" :size="28" stroke="#1a73e8" />
      <template v-else>
        <div class="v-banner-empty-icon">
          <n-icon :size="32">
            <ImageOutline />
          </n-icon>
        </div>
        <div class="v-banner-empty-text">
          <span class="v-banner-empty-title">
            {{ placeholderText || '点击上传竞赛横幅 Banner' }}
          </span>
          <span class="v-banner-empty-subtitle">
            推荐比例 16:9 或 1920×400，支持 JPG、PNG、WebP、GIF（不超过 5MB）
          </span>
        </div>
      </template>
    </div>

    <input
      v-if="editable"
      ref="fileInputRef"
      type="file"
      :accept="accept"
      class="hidden-file-input"
      @click.stop
      @change="handleFileChange"
    />

    <Teleport to="body">
      <Transition name="v-banner-fade">
        <div
          v-if="showPreview"
          class="v-banner-lightbox"
          @click="closePreview"
        >
          <div class="v-banner-lightbox-content" @click.stop>
            <button
              type="button"
              class="v-banner-lightbox-close"
              title="关闭预览"
              @click="closePreview"
            >
              <n-icon :size="20">
                <CloseOutline />
              </n-icon>
            </button>
            <img
              :src="effectiveSrc"
              :alt="alt"
              class="v-banner-lightbox-img"
            />
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onUnmounted } from 'vue'
import { NIcon, NSpin, useMessage } from 'naive-ui'
import { ImageOutline, CameraOutline, TrashOutline, EyeOutline, CloseOutline } from '@vicons/ionicons5'
import { useAppStore } from '@/store/modules/app'
import { uploadImageApi } from '@/api/system'
import type { VBannerProps } from './types'

defineOptions({
  name: 'VBanner'
})

const props = withDefaults(defineProps<VBannerProps>(), {
  src: '',
  value: undefined,
  modelValue: undefined,
  alt: 'Competition Banner',
  height: '160px',
  radius: '12px',
  editable: true,
  uploadFolder: 'banners',
  accept: 'image/png,image/jpeg,image/jpg,image/webp,image/gif',
  placeholderText: '',
  theme: undefined,
  uploadApi: undefined,
  clearable: true
})

const emit = defineEmits<{
  (e: 'update:src', url: string): void
  (e: 'update:value', url: string): void
  (e: 'update:modelValue', url: string): void
  (e: 'upload-success', url: string, file: File, res: any): void
  (e: 'upload-error', error: any): void
  (e: 'change', file: File): void
  (e: 'click', ev: MouseEvent): void
  (e: 'clear'): void
  (e: 'error', ev: Event): void
}>()

const appStore = useAppStore()
const hasError = ref(false)
const uploading = ref(false)
const showPreview = ref(false)
const internalSrc = ref('')
const fileInputRef = ref<HTMLInputElement | null>(null)

let message: any = null
try {
  message = useMessage()
} catch {
  message = null
}

function notifyError(content: string) {
  const m = message || (typeof window !== 'undefined' ? (window as any).$message : null)
  m?.error(content)
}

function notifySuccess(content: string) {
  const m = message || (typeof window !== 'undefined' ? (window as any).$message : null)
  m?.success(content)
}

const currentTheme = computed<'light' | 'dark'>(() => {
  return props.theme || (appStore.theme === 'dark' ? 'dark' : 'light')
})

const effectiveSrc = computed<string>(() => {
  if (internalSrc.value) return internalSrc.value
  if (props.value !== undefined && props.value !== null) return props.value
  if (props.modelValue !== undefined && props.modelValue !== null) return props.modelValue
  return props.src || ''
})

const computedRadius = computed<string>(() => {
  if (props.radius !== undefined && props.radius !== null && props.radius !== '') {
    return typeof props.radius === 'number' ? `${props.radius}px` : String(props.radius)
  }
  return '12px'
})

const heightStyle = computed<string>(() => {
  if (typeof props.height === 'number') {
    return `${props.height}px`
  }
  return String(props.height)
})

const bannerStyle = computed(() => ({
  height: heightStyle.value,
  borderRadius: computedRadius.value
}))

watch(
  () => [props.src, props.value, props.modelValue],
  () => {
    internalSrc.value = ''
    hasError.value = false
  }
)

function handleError(ev: Event) {
  hasError.value = true
  emit('error', ev)
}

function triggerUpload() {
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
    fileInputRef.value.click()
  }
}

function handleClick(ev: MouseEvent) {
  emit('click', ev)
  if (props.editable) {
    if (uploading.value) return
    triggerUpload()
  }
}

function handleClear() {
  internalSrc.value = ''
  emit('update:src', '')
  emit('update:value', '')
  emit('update:modelValue', '')
  emit('clear')
}

function openPreview() {
  showPreview.value = true
  window.addEventListener('keydown', handleKeydown)
}

function closePreview() {
  showPreview.value = false
  window.removeEventListener('keydown', handleKeydown)
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') {
    closePreview()
  }
}

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})

async function handleFileChange(e: Event) {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  emit('change', file)

  const validTypes = ['image/jpeg', 'image/png', 'image/webp', 'image/gif', 'image/jpg']
  if (!validTypes.includes(file.type)) {
    notifyError('仅支持 JPG、PNG、WebP 或 GIF 格式图片')
    target.value = ''
    return
  }

  if (file.size > 5 * 1024 * 1024) {
    notifyError('Banner 大小不能超过 5MB')
    target.value = ''
    return
  }

  uploading.value = true
  try {
    let uploadedUrl = ''
    let res: any = null
    if (props.uploadApi) {
      res = await props.uploadApi(file)
      uploadedUrl = typeof res === 'string' ? res : (res?.data?.filePath || res?.filePath || '')
    } else {
      res = await uploadImageApi(file, props.uploadFolder || 'banners')
      uploadedUrl = res?.data?.filePath || ''
    }

    if (uploadedUrl) {
      internalSrc.value = uploadedUrl
      emit('update:src', uploadedUrl)
      emit('update:value', uploadedUrl)
      emit('update:modelValue', uploadedUrl)
      emit('upload-success', uploadedUrl, file, res)
      notifySuccess('Banner 上传成功')
    }
  } catch (err: any) {
    emit('upload-error', err)
    notifyError(err?.message || 'Banner 上传失败')
  } finally {
    uploading.value = false
    target.value = ''
  }
}

defineExpose({
  hasError,
  triggerUpload,
  clear: handleClear,
  openPreview
})
</script>

<style scoped lang="scss">
.v-banner {
  position: relative;
  width: 100%;
  box-sizing: border-box;
  overflow: hidden;
  user-select: none;
  background-color: transparent;
  transition: border-color 0.2s cubic-bezier(0.4, 0, 0.2, 1),
              background-color 0.2s cubic-bezier(0.2, 0, 0, 1);

  .hidden-file-input {
    display: none;
  }

  &.is-editable {
    cursor: pointer;
  }

  &.is-uploading {
    cursor: wait !important;
  }

  .v-banner-img-wrap {
    position: relative;
    width: 100%;
    height: 100%;
    border-radius: inherit;
    overflow: hidden;

    &:hover .v-banner-overlay {
      opacity: 1;
    }

    &:hover .v-banner-actions {
      opacity: 1;
    }
  }

  .v-banner-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
    border-radius: inherit;
  }

  .v-banner-overlay {
    position: absolute;
    inset: 0;
    border-radius: inherit;
    background-color: rgba(0, 0, 0, 0.45);
    display: flex;
    align-items: center;
    justify-content: center;
    opacity: 0;
    transition: opacity 0.2s cubic-bezier(0.4, 0, 0.2, 1);
    pointer-events: none;
    z-index: 2;

    .v-banner-overlay-content {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
    }
  }

  .v-banner-actions {
    position: absolute;
    top: 10px;
    right: 10px;
    display: flex;
    align-items: center;
    gap: 6px;
    opacity: 0;
    transition: opacity 0.2s cubic-bezier(0.4, 0, 0.2, 1);
    z-index: 4;
  }

  .v-banner-action-btn {
    width: 30px;
    height: 30px;
    border-radius: 8px;
    background-color: rgba(0, 0, 0, 0.6);
    color: #ffffff;
    border: none;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: background-color 0.15s ease-in-out, transform 0.1s ease-in-out;

    &:hover {
      background-color: #1a73e8;
    }

    &:active {
      transform: scale(0.95);
    }

    &.danger:hover {
      background-color: #ea4335;
    }
  }

  .v-banner-empty {
    width: 100%;
    height: 100%;
    border-radius: inherit;
    border: 1.5px dashed #d7d8da;
    background-color: #f8f9fa;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 10px;
    padding: 16px;
    box-sizing: border-box;
    transition: border-color 0.2s cubic-bezier(0.4, 0, 0.2, 1),
                background-color 0.2s cubic-bezier(0.4, 0, 0.2, 1);

    &:hover {
      border-color: #1a73e8;
      background-color: #f1f3f4;
    }

    .v-banner-empty-icon {
      width: 48px;
      height: 48px;
      border-radius: 50%;
      background-color: #e8f0fe;
      color: #1a73e8;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }

    .v-banner-empty-text {
      display: flex;
      flex-direction: column;
      align-items: center;
      text-align: center;
      gap: 3px;

      .v-banner-empty-title {
        font-size: 13px;
        font-weight: 500;
        color: #202124;
      }

      .v-banner-empty-subtitle {
        font-size: 11px;
        color: #5f6368;
      }
    }
  }

  &.dark,
  html.dark & {
    .v-banner-empty {
      border-color: #3c4043;
      background-color: #202124;

      &:hover {
        border-color: #8ab4f8;
        background-color: #292a2d;
      }

      .v-banner-empty-icon {
        background-color: rgba(138, 180, 248, 0.16);
        color: #8ab4f8;
      }

      .v-banner-empty-text {
        .v-banner-empty-title {
          color: #e8eaed;
        }

        .v-banner-empty-subtitle {
          color: #9aa0a6;
        }
      }
    }
  }
}

.v-banner-lightbox {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background-color: rgba(0, 0, 0, 0.75);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  cursor: zoom-out;
}

.v-banner-lightbox-content {
  position: relative;
  max-width: 90vw;
  max-height: 85vh;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: default;
}

.v-banner-lightbox-close {
  position: absolute;
  top: -44px;
  right: 0;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: rgba(255, 255, 255, 0.2);
  border: none;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background-color 0.15s ease-in-out;

  &:hover {
    background-color: rgba(255, 255, 255, 0.35);
  }
}

.v-banner-lightbox-img {
  max-width: 100%;
  max-height: 85vh;
  object-fit: contain;
  border-radius: 12px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.15);
}

.v-banner-fade-enter-active,
.v-banner-fade-leave-active {
  transition: opacity 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.v-banner-fade-enter-from,
.v-banner-fade-leave-to {
  opacity: 0;
}
</style>
