<template>
  <div class="flex items-center justify-end gap-6 pt-6 sm:pt-8">
    <slot name="extra" />
    <VButton
      v-if="showSecondary && secondaryText"
      variant="secondary-lowest"
      size="large"
      :round="true"
      is-scrim="true"
      :class="secondaryClass"
      :disabled="secondaryDisabled"
      :loading="secondaryLoading"
      @click="emit('secondary')"
    >
      {{ secondaryText }}
    </VButton>
    <VButton
      v-if="primaryText"
      variant="primary-highest"
      size="large"
      :round="true"
      is-scrim="true"
      :class="primaryClass"
      :disabled="primaryDisabled"
      :loading="primaryLoading"
      @click="emit('primary')"
    >
      {{ primaryText }}
    </VButton>
  </div>
</template>

<script setup lang="ts">
import { VButton } from '@/volur-ui'

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
