<template>
  <transition name="lang-mask-fade">
    <div v-if="isLangSwitching" class="global-lang-loading-mask">
      <div class="lang-loading-card">
        <n-spin size="large" />
        <span class="lang-loading-text">{{ langSwitchingText }}</span>
      </div>
    </div>
  </transition>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { NSpin } from 'naive-ui'
import { useAppStore } from '@/store/modules/app'

defineOptions({
  name: 'LanguageSwitcher'
})

const appStore = useAppStore()

const isLangSwitching = computed<boolean>(() => appStore.isLangSwitching)
const langSwitchingText = computed<string>(() => appStore.langSwitchingText)

defineExpose({
  isLangSwitching,
  langSwitchingText
})
</script>

<style scoped lang="scss">
.global-lang-loading-mask {
  position: fixed;
  inset: 0;
  z-index: 99999;
  background-color: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: wait;
  user-select: none;
}

html.dark .global-lang-loading-mask {
  background-color: rgba(0, 0, 0, 0.6);
}

.lang-loading-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 24px 32px;
  background-color: transparent;
  box-shadow: none;
}

html.dark .lang-loading-card {
  background-color: transparent;
  box-shadow: none;
}

.lang-loading-text {
  font-size: 14px;
  color: var(--v-text-primary, #202124);
  font-weight: 500;
}

html.dark .lang-loading-text {
  color: #e8eaed;
}

.lang-mask-fade-enter-active,
.lang-mask-fade-leave-active {
  transition: opacity 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.lang-mask-fade-enter-from,
.lang-mask-fade-leave-to {
  opacity: 0;
}
</style>
