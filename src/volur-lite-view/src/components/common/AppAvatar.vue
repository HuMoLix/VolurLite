<template>
  <div
    class="app-avatar-wrapper"
    :class="[
      {
        'is-editable': editable,
        'is-round': round,
        'is-clickable': clickable || editable,
        'has-border': bordered
      },
      customClass
    ]"
    :style="wrapperStyle"
    @click="handleClick"
  >
    <n-avatar
      :round="round"
      :size="size"
      :src="shouldShowImage ? displayUrl : undefined"
      :fallback-src="fallbackSrc"
      :bordered="bordered"
      :object-fit="objectFit"
      :key="`avatar-${displayUrl || 'empty'}-${imgError}`"
      class="volur-app-avatar"
      :style="avatarStyle"
      @error="handleError"
    >
      <template v-if="!shouldShowImage">
        {{ fallbackChar }}
      </template>
    </n-avatar>

    <!-- Optional Hover Edit Overlay (for editable avatar like profile change) -->
    <div v-if="editable" class="avatar-edit-overlay">
      <n-icon :size="cameraIconSize" color="#ffffff">
        <CameraOutline />
      </n-icon>
      <span v-if="computedOverlayText" class="overlay-text">{{ computedOverlayText }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { NAvatar, NIcon } from 'naive-ui'
import { CameraOutline } from '@vicons/ionicons5'
import { formatAvatarUrl } from '@/utils/auth'

interface Props {
  /**
   * 头像图片原始或相对地址 (将通过 formatAvatarUrl 解析为可访问地址并支持内存 Blob 缓存)
   */
  src?: string | null
  /**
   * 实体名称 (如用户名、战队名、比赛名等，用于无头像时生成首字母缩写)
   */
  name?: string | null
  /**
   * 别名，兼容原有 UserAvatar 组件的 username 传参
   */
  username?: string | null
  /**
   * 尺寸大小 (支持数字像素，或 Naive UI 预设 'small' | 'medium' | 'large')
   */
  size?: number | 'small' | 'medium' | 'large'
  /**
   * 是否为圆形头像
   */
  round?: boolean
  /**
   * 是否显示边框
   */
  bordered?: boolean
  /**
   * 图片加载失败时的回退图片地址
   */
  fallbackSrc?: string
  /**
   * 自定义回退文本 (若指定则优先于 name/username 首字符)
   */
  fallbackText?: string
  /**
   * 是否支持悬浮更换头像遮罩
   */
  editable?: boolean
  /**
   * 是否具备点击交互手势
   */
  clickable?: boolean
  /**
   * 更换头像遮罩文本
   */
  overlayText?: string
  /**
   * 自定义容器类名
   */
  customClass?: string
  /**
   * 头像底色 (默认 Volur 极简黑 #000000)
   */
  backgroundColor?: string
  /**
   * 回退文本颜色 (默认白 #ffffff)
   */
  textColor?: string
  /**
   * 图片缩放适应模式
   */
  objectFit?: 'fill' | 'contain' | 'cover' | 'none' | 'scale-down'
}

const props = withDefaults(defineProps<Props>(), {
  src: undefined,
  name: undefined,
  username: undefined,
  size: 'medium',
  round: true,
  bordered: false,
  fallbackSrc: undefined,
  fallbackText: undefined,
  editable: false,
  clickable: false,
  overlayText: '',
  customClass: '',
  backgroundColor: '#000000',
  textColor: '#ffffff',
  objectFit: 'cover'
})

const emit = defineEmits<{
  (e: 'click', event: MouseEvent): void
  (e: 'edit'): void
  (e: 'error', event: Event): void
}>()

const imgError = ref(false)

watch(
  () => props.src,
  () => {
    imgError.value = false
  }
)

const displayUrl = computed(() => {
  return formatAvatarUrl(props.src)
})

const shouldShowImage = computed(() => {
  return !!displayUrl.value && (!imgError.value || !!props.fallbackSrc)
})

const fallbackChar = computed(() => {
  if (props.fallbackText) return props.fallbackText
  const str = props.name || props.username
  if (!str || !str.trim()) return 'V'
  // 正确处理 Unicode 字符与中文字符，提取首字并大写
  return Array.from(str.trim())[0].toUpperCase()
})

const computedOverlayText = computed(() => {
  if (props.overlayText) return props.overlayText
  if (typeof props.size === 'number' && props.size < 60) return ''
  return '更换头像'
})

const cameraIconSize = computed(() => {
  if (typeof props.size === 'number') {
    return Math.max(16, Math.floor(props.size * 0.28))
  }
  if (props.size === 'large') return 24
  if (props.size === 'small') return 14
  return 18
})

const sizeInPx = computed(() => {
  if (typeof props.size === 'number') return props.size
  if (props.size === 'small') return 28
  if (props.size === 'large') return 40
  return 34
})

const wrapperStyle = computed(() => {
  const px = sizeInPx.value
  return {
    width: `${px}px`,
    height: `${px}px`,
    flexShrink: 0
  }
})

const avatarStyle = computed(() => {
  return {
    backgroundColor: props.backgroundColor,
    color: props.textColor,
    border: props.bordered ? '1px solid var(--v-border-base, #e5e4e7)' : 'none',
    boxShadow: 'none',
    outline: 'none'
  }
})

function handleError(event: Event) {
  imgError.value = true
  emit('error', event)
}

function handleClick(event: MouseEvent) {
  emit('click', event)
  if (props.editable) {
    emit('edit')
  }
}
</script>

<style scoped lang="scss">
.app-avatar-wrapper {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  vertical-align: middle;
  border: none;
  outline: none;
  box-shadow: none;
  flex-shrink: 0;

  &.is-round {
    border-radius: 50%;
    overflow: hidden;
  }

  &.is-clickable {
    cursor: pointer;
  }

  &.is-editable {
    cursor: pointer;

    &:hover .avatar-edit-overlay {
      opacity: 1;
    }
  }

  :deep(.volur-app-avatar) {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;
    flex-shrink: 0;
    border: none !important;
    outline: none !important;
    box-shadow: none !important;
    --n-border: none !important;

    img {
      border: none !important;
      outline: none !important;
      box-shadow: none !important;
    }
  }

  .avatar-edit-overlay {
    position: absolute;
    inset: 0;
    background-color: rgba(0, 0, 0, 0.55);
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 4px;
    opacity: 0;
    transition: opacity 0.2s ease;
    border-radius: inherit;
    user-select: none;
    z-index: 2;

    .overlay-text {
      color: #ffffff;
      font-size: 11px;
      font-weight: 500;
      letter-spacing: 0.5px;
    }
  }
}
</style>
