<template>
  <div class="auth-selection-wrapper w-full">
    <div v-if="loading" class="auth-selection-list">
      <div class="auth-selection-item is-disabled animate-pulse">
        <div class="auth-selection-item__left">
          <div class="w-10 h-10 rounded-full bg-google-border-light dark:bg-google-border-dark" />
          <div class="flex flex-col gap-2">
            <div class="w-28 h-4 rounded bg-google-border-light dark:bg-google-border-dark" />
            <div class="w-48 h-3 rounded bg-google-border-light dark:bg-google-border-dark" />
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="items.length > 0" class="auth-selection-list">
      <div
        v-for="(item, index) in items"
        :key="item.id || item.type || item.method || index"
        class="auth-selection-item"
        :class="{ 'is-disabled': disabled || item.disabled }"
        role="button"
        tabindex="0"
        :isScrim="true"
        @click="handleClick(item)"
        @keydown.enter.space.prevent="handleClick(item)"
      >
        <div class="auth-selection-item__left">
          <div class="auth-selection-item__icon">
            <svg
              v-if="isDeviceIcon(item)"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.75"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <rect x="5" y="2" width="14" height="20" rx="2" ry="2" />
              <line x1="12" y1="18" x2="12.01" y2="18" stroke-width="2" />
            </svg>
            <svg
              v-else-if="isEmailIcon(item)"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.75"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <rect x="3" y="5" width="18" height="14" rx="2" />
              <polyline points="3 7 12 13 21 7" />
            </svg>
            <svg
              v-else-if="isPhoneIcon(item)"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.75"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
              <line x1="8" y1="9" x2="16" y2="9" />
              <line x1="8" y1="13" x2="13" y2="13" />
            </svg>
            <svg
              v-else-if="isKeyIcon(item)"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.75"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <circle cx="7.5" cy="15.5" r="3.5" />
              <path d="M10 13l9-9" />
              <path d="M16 4l3 3" />
              <path d="M14 6l2 2" />
            </svg>
            <svg
              v-else
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.75"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
              <polyline points="9 12 11 14 15 10" />
            </svg>
          </div>
          <div class="auth-selection-item__content">
            <div class="auth-selection-item__title">
              {{ item.title }}
            </div>
            <div v-if="item.subtitle" class="auth-selection-item__subtitle">
              {{ item.subtitle }}
            </div>
          </div>
        </div>
      </div>
      <slot name="extra-items" />
    </div>

    <div
      v-else-if="emptyTip"
      class="p-4 rounded-lg bg-google-surface-light dark:bg-google-surface-darkElevated border border-google-border-light dark:border-google-border-dark text-sm text-google-text-muted dark:text-google-text-darkMuted leading-relaxed"
    >
      {{ emptyTip }}
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: 'AuthMethodSelector'
})

export interface AuthMethodOption {
  id?: string | number
  method?: string
  type?: string
  icon?: string
  title: string
  subtitle?: string
  disabled?: boolean
  [key: string]: any
}

interface Props {
  items: AuthMethodOption[]
  loading?: boolean
  emptyTip?: string
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  items: () => [],
  loading: false,
  emptyTip: '',
  disabled: false
})

const emit = defineEmits<{
  (e: 'select', item: AuthMethodOption): void
}>()

function handleClick(item: AuthMethodOption): void {
  if (props.disabled || item.disabled) return
  emit('select', item)
}

function isDeviceIcon(item: AuthMethodOption): boolean {
  const icon = item.icon?.toLowerCase()
  const type = item.type?.toLowerCase()
  const method = item.method?.toLowerCase()
  return icon === 'device' || type === 'pk' || type === 'passkey' || method === 'passkey'
}

function isEmailIcon(item: AuthMethodOption): boolean {
  const icon = item.icon?.toLowerCase()
  const type = item.type?.toLowerCase()
  const method = item.method?.toLowerCase()
  return icon === 'email' || type === 'email' || type === 'emailotp' || type === 'etotp' || method === 'email'
}

function isPhoneIcon(item: AuthMethodOption): boolean {
  const icon = item.icon?.toLowerCase()
  const type = item.type?.toLowerCase()
  const method = item.method?.toLowerCase()
  return icon === 'phone' || icon === 'sms' || type === 'sms' || type === 'smsotp' || type === 'totp' || method === 'phone' || method === 'sms'
}

function isKeyIcon(item: AuthMethodOption): boolean {
  const icon = item.icon?.toLowerCase()
  const type = item.type?.toLowerCase()
  const method = item.method?.toLowerCase()
  return icon === 'key' || type === 'pwd' || type === 'password' || method === 'password'
}
</script>
