<template>
  <AppAvatar
    :src="src"
    :name="username"
    :username="username"
    :size="size"
    :round="round"
    :bordered="false"
    :editable="editable"
    :overlay-text="overlayText"
    :custom-class="customClass"
    :background-color="backgroundColor"
    :text-color="textColor"
    fallback-text="U"
    @click="(e: MouseEvent) => emit('click', e)"
    @edit="() => emit('edit')"
  />
</template>

<script setup lang="ts">
import AppAvatar from './AppAvatar.vue'

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

withDefaults(defineProps<Props>(), {
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
</script>
