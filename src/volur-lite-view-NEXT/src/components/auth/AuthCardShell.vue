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
import { computed, watchEffect } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
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

const route = useRoute()
const { t } = useI18n()

const effectiveTitle = computed(() => {
  if (props.title) return props.title
  if (route.meta?.cardTitleKey) return t(route.meta.cardTitleKey as string)
  if (route.meta?.cardTitle) return route.meta.cardTitle
  return ''
})

const effectiveSubtitle = computed(() => {
  if (props.subtitle) return props.subtitle
  if (route.meta?.cardSubtitleKey) return t(route.meta.cardSubtitleKey as string)
  if (route.meta?.cardSubtitle) return route.meta.cardSubtitle
  return ''
})

const { title: cardTitle, subtitle: cardSubtitle, setHeader } = provideCardHeader(
  () => effectiveTitle.value,
  () => effectiveSubtitle.value
)

watchEffect(() => {
  if (effectiveTitle.value || effectiveSubtitle.value) {
    setHeader(effectiveTitle.value, effectiveSubtitle.value)
  }
})
</script>
