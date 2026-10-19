<template>
  <div class="flex items-center justify-end gap-2.5 pt-6 sm:pt-8">
    <slot name="extra" />
    <n-button
      v-if="showSecondary && secondaryText"
      quaternary
      type="primary"
      :round="true"
      :class="['auth-action-btn', secondaryClass]"
      :disabled="secondaryDisabled"
      :loading="secondaryLoading"
      :isScrim="isScrim"
      @click="emit('secondary')"
    >
      {{ secondaryText }}
    </n-button>
    <n-button
      v-if="primaryText"
      type="primary"
      :round="true"
      :class="['auth-action-btn', primaryClass]"
      :disabled="primaryDisabled"
      :loading="primaryLoading"
      :isScrim="isScrim"
      @click="emit('primary')"
    >
      {{ primaryText }}
    </n-button>
  </div>
</template>

<script setup lang="ts">
import { NButton } from 'naive-ui'

defineOptions({
  name: 'AuthActionBar'
})

interface Props {
  primaryText?: string
  secondaryText?: string
  primaryDisabled?: boolean
  secondaryDisabled?: boolean
  primaryLoading?: boolean
  secondaryLoading?: boolean
  showSecondary?: boolean
  primaryClass?: string
  secondaryClass?: string
  isScrim?: boolean
}

withDefaults(defineProps<Props>(), {
  primaryText: '',
  secondaryText: '',
  primaryDisabled: false,
  secondaryDisabled: false,
  primaryLoading: false,
  secondaryLoading: false,
  showSecondary: true,
  primaryClass: 'btn-primary-action',
  secondaryClass: 'btn-secondary-action',
  isScrim: true
})

const emit = defineEmits<{
  (e: 'primary'): void
  (e: 'secondary'): void
}>()
</script>
