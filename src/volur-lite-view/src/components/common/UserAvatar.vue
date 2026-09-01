<template>
  <div
    class="user-avatar-wrapper"
    :class="[
      { 'is-editable': editable, 'is-round': round },
      customClass
    ]"
    :style="wrapperStyle"
    @click="handleClick"
  >
    <n-avatar
      :round="round"
      :size="size"
      :src="displayUrl"
      :bordered="false"
      :key="`avatar-${displayUrl || 'empty'}`"
      class="volur-user-avatar"
      :style="avatarStyle"
    >
      <template v-if="!displayUrl">
        {{ fallbackChar }}
      </template>
    </n-avatar>

    <!-- Optional Hover Edit Overlay -->
    <div v-if="editable" class="avatar-edit-overlay">
      <n-icon :size="cameraIconSize" color="#ffffff">
        <CameraOutline />
      </n-icon>
      <span v-if="computedOverlayText" class="overlay-text">{{ computedOverlayText }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { NAvatar, NIcon } from 'naive-ui'
import { CameraOutline } from '@vicons/ionicons5'
import { formatAvatarUrl } from '@/utils/auth'

interface Props {
  src?: string | null
  username?: string | null
  size?: number | 'small' | 'medium' | 'large'
  round?: boolean
  editable?: boolean
  overlayText?: string
  customClass?: string
  backgroundColor?: string
  textColor?: string
}

const props = withDefaults(defineProps<Props>(), {
  src: undefined,
  username: '',
  size: 'medium',
  round: true,
  editable: false,
  overlayText: '',
  customClass: '',
  backgroundColor: '#000000',
  textColor: '#ffffff'
})

const emit = defineEmits<{
  (e: 'click', event: MouseEvent): void
  (e: 'edit'): void
}>()

const displayUrl = computed(() => {
  return formatAvatarUrl(props.src)
})

const fallbackChar = computed(() => {
  if (!props.username || !props.username.trim()) return 'U'
  return props.username.trim().charAt(0).toUpperCase()
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

const wrapperStyle = computed(() => {
  const styles: Record<string, string> = {}
  if (typeof props.size === 'number') {
    styles.width = `${props.size}px`
    styles.height = `${props.size}px`
  }
  return styles
})

const avatarStyle = computed(() => {
  return {
    backgroundColor: props.backgroundColor,
    color: props.textColor,
    border: 'none',
    boxShadow: 'none',
    outline: 'none'
  }
})

function handleClick(event: MouseEvent) {
  emit('click', event)
  if (props.editable) {
    emit('edit')
  }
}
</script>

<style scoped lang="scss">
.user-avatar-wrapper {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  vertical-align: middle;
  border: none;
  outline: none;
  box-shadow: none;

  &.is-round {
    border-radius: 50%;
    overflow: hidden;
  }

  &.is-editable {
    cursor: pointer;

    &:hover .avatar-edit-overlay {
      opacity: 1;
    }
  }

  :deep(.volur-user-avatar) {
    display: inline-flex;
    align-items: center;
    justify-content: center;
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
