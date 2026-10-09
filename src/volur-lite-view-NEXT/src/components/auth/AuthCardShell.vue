<template>
  <div class="auth-card__body">
    <div class="auth-card__left">
      <h1 class="auth-card__title">{{ cardTitle }}</h1>
      <p v-if="cardSubtitle" class="auth-card__subtitle">{{ cardSubtitle }}</p>
      <div id="auth-header-extra" class="auth-card__header-extra">
        <slot name="header-extra" />
      </div>
    </div>

    <div class="auth-card__right">
      <div class="auth-form-content w-full h-full flex flex-col justify-between">
        <slot />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { watchEffect } from 'vue'
import { provideCardHeader } from '@/composables/useCardHeader'

defineOptions({
  name: 'AuthCardShell'
})

interface Props {
  title?: string
  subtitle?: string
}

const props = withDefaults(defineProps<Props>(), {
  title: '',
  subtitle: ''
})

const { title: cardTitle, subtitle: cardSubtitle, setHeader } = provideCardHeader(
  () => props.title,
  () => props.subtitle
)

watchEffect(() => {
  if (props.title || props.subtitle) {
    setHeader(props.title, props.subtitle)
  }
})
</script>
