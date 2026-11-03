<template>
  <div
    class="v-avatar"
    :class="[
      `is-${computedShape}`,
      currentTheme,
      {
        'has-ring': ring,
        'is-editable': editable,
        'is-uploading': uploading
      }
    ]"
    :style="avatarStyle"
    @click="handleClick"
  >
    <slot>
      <img
        v-if="effectiveSrc && !hasError"
        :src="effectiveSrc"
        :alt="alt"
        class="v-avatar-img"
        @error="handleError"
      />
      <div
        v-else-if="text"
        class="v-avatar-fallback"
        :style="{ fontSize }"
      >
        <slot name="fallback">{{ text }}</slot>
      </div>
      <div
        v-else
        class="v-avatar-guest"
      >
        <slot name="guest">
          <n-icon :size="iconSize">
            <PersonOutline />
          </n-icon>
        </slot>
      </div>
    </slot>
    <div
      v-if="editable"
      class="v-avatar-edit-overlay"
      :class="{ 'show-uploading': uploading }"
    >
      <n-spin v-if="uploading" :size="Math.max(14, Math.round(numericSize * 0.35))" stroke="#ffffff" />
      <n-icon v-else :size="Math.max(14, Math.round(numericSize * 0.38))" class="text-white">
        <CameraOutline />
      </n-icon>
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { NIcon, NSpin, useMessage } from 'naive-ui'
import { PersonOutline, CameraOutline } from '@vicons/ionicons5'
import { useAppStore } from '@/store/modules/app'
import { uploadImageApi } from '@/api/system'
import type { VAvatarProps } from './types'

defineOptions({
  name: 'VAvatar'
})

const props = withDefaults(defineProps<VAvatarProps>(), {
  src: '',
  alt: 'Avatar',
  text: '',
  size: 32,
  shape: 'square',
  round: false,
  radius: undefined,
  ring: false,
  ringColor: '',
  theme: undefined,
  editable: false,
  uploadFolder: 'avatars',
  accept: 'image/png,image/jpeg,image/jpg,image/webp,image/gif'
})

const emit = defineEmits<{
  error: [ev: Event]
  click: [ev: MouseEvent]
  'update:src': [url: string]
  'upload-success': [url: string, file: File, res: any]
  'upload-error': [error: any]
  change: [file: File]
}>()

const appStore = useAppStore()
const hasError = ref(false)
const uploading = ref(false)
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
  return internalSrc.value || props.src
})

const computedRadius = computed<string>(() => {
  if (props.radius !== undefined && props.radius !== null && props.radius !== '') {
    return typeof props.radius === 'number' ? `${props.radius}px` : String(props.radius)
  }
  if (props.shape === 'circle' || props.round) {
    return '64px'
  }
  return '8px'
})

const computedShape = computed<'square' | 'circle'>(() => {
  if (props.shape === 'circle' || props.round || computedRadius.value === '64px') {
    return 'circle'
  }
  return 'square'
})

const numericSize = computed<number>(() => {
  if (typeof props.size === 'number') {
    return props.size
  }
  const parsed = parseInt(String(props.size), 10)
  return isNaN(parsed) ? 32 : parsed
})

const sizeStyle = computed<string>(() => {
  if (typeof props.size === 'number') {
    return `${props.size}px`
  }
  return String(props.size)
})

const iconSize = computed<number>(() => {
  return Math.max(12, Math.round(numericSize.value * 0.56))
})

const fontSize = computed<string>(() => {
  return `${Math.max(11, Math.round(numericSize.value * 0.42))}px`
})

const avatarStyle = computed(() => {
  const style: Record<string, string> = {
    width: sizeStyle.value,
    height: sizeStyle.value,
    borderRadius: computedRadius.value
  }
  if (props.ringColor) {
    style['--v-avatar-ring-color'] = props.ringColor
  }
  return style
})

watch(
  () => props.src,
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
    notifyError('头像大小不能超过 5MB')
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
      res = await uploadImageApi(file, props.uploadFolder || 'avatars')
      uploadedUrl = res?.data?.filePath || ''
    }

    if (uploadedUrl) {
      internalSrc.value = uploadedUrl
      emit('update:src', uploadedUrl)
      emit('upload-success', uploadedUrl, file, res)
      notifySuccess('头像上传成功')
    }
  } catch (err: any) {
    emit('upload-error', err)
    notifyError(err?.message || '头像上传失败')
  } finally {
    uploading.value = false
    target.value = ''
  }
}

defineExpose({
  hasError,
  triggerUpload
})
</script>

<style scoped lang="scss">
.v-avatar {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-sizing: border-box;
  overflow: hidden;
  user-select: none;
  vertical-align: middle;
  background-color: transparent;
  transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1),
              height 0.25s cubic-bezier(0.4, 0, 0.2, 1),
              border-radius 0.2s cubic-bezier(0.4, 0, 0.2, 1),
              background-color 0.2s cubic-bezier(0.2, 0, 0, 1);

  .hidden-file-input {
    display: none;
  }

  .v-avatar-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
    border-radius: inherit;
  }

  .v-avatar-fallback {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #e8f0fe;
    color: #1a73e8;
    font-weight: 600;
    line-height: 1;
    border-radius: inherit;
  }

  .v-avatar-guest {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #e8eaed;
    color: #5f6368;
    border-radius: inherit;
  }

  .v-avatar-edit-overlay {
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

    &.show-uploading {
      opacity: 1;
      cursor: wait;
    }
  }

  &.is-editable {
    cursor: pointer;

    &:hover .v-avatar-edit-overlay {
      opacity: 1;
    }
  }

  &.is-uploading {
    cursor: wait !important;
  }

  &.has-ring {
    &::after {
      content: '';
      position: absolute;
      inset: 0;
      border-radius: inherit;
      border: 1.5px solid var(--v-avatar-ring-color, #dadce0);
      box-sizing: border-box;
      pointer-events: none;
      transition: border-color 0.2s cubic-bezier(0.4, 0, 0.2, 1);
    }

    &:hover::after {
      border-color: var(--v-avatar-ring-color, #1a73e8);
    }

    &.dark::after {
      border-color: var(--v-avatar-ring-color, #3c4043);
    }

    &.dark:hover::after {
      border-color: var(--v-avatar-ring-color, #8ab4f8);
    }
  }

  &.dark {
    .v-avatar-fallback {
      background-color: rgba(168, 199, 250, 0.16);
      color: #a8c7fa;
    }

    .v-avatar-guest {
      background-color: #2a2a2a;
      color: #8c8c8c;
    }
  }
}

html.dark {
  .v-avatar {
    .v-avatar-fallback {
      background-color: rgba(168, 199, 250, 0.16);
      color: #a8c7fa;
    }

    .v-avatar-guest {
      background-color: #2a2a2a;
      color: #8c8c8c;
    }

    &.has-ring::after {
      border-color: var(--v-avatar-ring-color, #3c4043);
    }

    &.has-ring:hover::after {
      border-color: var(--v-avatar-ring-color, #8ab4f8);
    }
  }
}
</style>
