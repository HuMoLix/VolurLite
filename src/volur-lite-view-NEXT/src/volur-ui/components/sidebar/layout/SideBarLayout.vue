<template>
  <aside
    ref="layoutRef"
    class="volur-sidebar-layout"
    :class="[
      {
        'is-collapsed': collapsed,
        'is-bordered': bordered
      },
      siderClass
    ]"
    :style="[computedStyle, contentStyle as any]"
  >
    <header v-if="$slots.header" class="volur-sidebar-layout__header">
      <slot name="header" />
    </header>

    <main
      ref="mainRef"
      class="volur-sidebar-layout__main"
      @scroll="handleScroll"
      @wheel="handleWheel"
    >
      <slot />
    </main>

    <footer v-if="$slots.footer" class="volur-sidebar-layout__footer">
      <slot name="footer" />
    </footer>
  </aside>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { CSSProperties } from 'vue'
import type { SideBarLayoutProps } from '../types'

defineOptions({
  name: 'SideBarLayout'
})

const props = withDefaults(defineProps<SideBarLayoutProps>(), {
  collapsed: false,
  width: 220,
  collapsedWidth: 74,
  bordered: true,
  collapseMode: 'width',
  contentStyle: '',
  siderClass: ''
})

const emit = defineEmits<{
  (e: 'scroll', ev: Event): void
  (e: 'wheel', ev: WheelEvent): void
}>()

const layoutRef = ref<HTMLElement | null>(null)
const mainRef = ref<HTMLElement | null>(null)

const computedWidth = computed<string>(() => {
  const w = props.collapsed ? props.collapsedWidth : props.width
  return typeof w === 'number' ? `${w}px` : String(w)
})

const computedStyle = computed<CSSProperties>(() => ({
  width: computedWidth.value
}))

function handleScroll(ev: Event) {
  emit('scroll', ev)
}

function handleWheel(ev: WheelEvent) {
  emit('wheel', ev)
}

function scrollToTop(options?: ScrollToOptions) {
  if (mainRef.value) {
    mainRef.value.scrollTo({
      top: 0,
      behavior: 'smooth',
      ...options
    })
  }
}

defineExpose({
  layoutRef,
  mainRef,
  scrollToTop
})
</script>

<style scoped lang="scss">
.volur-sidebar-layout {
  position: relative;
  height: 100%;
  max-height: 100%;
  display: flex;
  flex-direction: column;
  background-color: var(--color-v1-surface-left-nav);
  box-sizing: border-box;
  padding: 0 8px;
  overflow: hidden;
  z-index: 10;
  flex-shrink: 0;
  transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1),
              background-color 0.25s ease,
              border-color 0.25s ease;

  @media (max-width: 839px) {
    display: none !important;
  }

  &.is-bordered {
    border-right: 1px solid var(--color-v1-outline-var);
  }

  &__header {
    flex-shrink: 0;
    width: 100%;
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
  }

  &__main {
    flex: 1;
    min-height: 0;
    width: 100%;
    overflow-y: auto;
    overflow-x: hidden;
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
    scrollbar-width: none;
    -ms-overflow-style: none;

    &::-webkit-scrollbar {
      display: none;
      width: 0;
      height: 0;
    }
  }

  &__footer {
    flex-shrink: 0;
    width: 100%;
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
  }
}
</style>
