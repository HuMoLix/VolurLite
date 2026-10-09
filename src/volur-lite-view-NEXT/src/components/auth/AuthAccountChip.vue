<template>
  <n-button
    quaternary
    :round="true"
    class="auth-account-chip"
    :disabled="disabled"
    :isScrim="true"
    :title="title || t('auth.signin.switch_account')"
    @click="emit('click')"
  >
    <span class="auth-account-chip__avatar">
      {{ displayInitial }}
    </span>
    <span class="auth-account-chip__label">{{ identifier }}</span>
    <svg
      class="w-3 h-3 text-google-text-muted dark:text-google-text-darkMuted fill-current shrink-0"
      viewBox="7 10 10 5"
      aria-hidden="true"
    >
      <polygon points="7 10 12 15 17 10" />
    </svg>
  </n-button>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { NButton } from 'naive-ui'
import { useI18n } from 'vue-i18n'

defineOptions({
  name: 'AuthAccountChip'
})

interface Props {
  identifier?: string
  disabled?: boolean
  title?: string
}

const props = withDefaults(defineProps<Props>(), {
  identifier: '',
  disabled: false,
  title: ''
})

const emit = defineEmits<{
  (e: 'click'): void
}>()

const { t } = useI18n()

const displayInitial = computed(() => {
  return (props.identifier?.charAt(0) || 'U').toUpperCase()
})
</script>
