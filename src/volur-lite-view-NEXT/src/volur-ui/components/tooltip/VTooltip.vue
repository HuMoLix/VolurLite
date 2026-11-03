<template>
  <span
    ref="triggerRef"
    v-bind="$attrs"
    class="volur-tooltip-trigger"
  >
    <slot v-if="$slots.trigger" name="trigger" />
    <slot v-else />
  </span>

  <Teleport :to="teleportTarget" :disabled="!isTeleportEnabled">
    <Transition name="volur-tooltip-fade">
      <div
        v-if="isVisible"
        ref="popperRef"
        role="tooltip"
        :data-placement="actualPlacement"
        :class="computedTooltipClass"
        :style="computedPopperStyle"
        @mouseenter="handlePopperMouseEnter"
        @mouseleave="handlePopperMouseLeave"
      >
        <span class="volur-tooltip__content">
          <slot v-if="$slots.trigger && $slots.content" name="content" />
          <slot v-else-if="$slots.trigger" />
          <slot v-else-if="$slots.content" name="content" />
          <span v-else-if="hasContent">{{ content }}</span>
        </span>

        <span
          v-if="showArrow"
          class="volur-tooltip-arrow"
          :style="computedArrowStyle"
        />
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import {
  ref,
  computed,
  watch,
  nextTick,
  onMounted,
  onBeforeUnmount,
  useSlots,
  type CSSProperties
} from 'vue'
import type { VTooltipProps } from './types'

defineOptions({
  name: 'VTooltip',
  inheritAttrs: false
})

let activeTooltipCloser: (() => void) | null = null

const props = withDefaults(defineProps<VTooltipProps>(), {
  content: undefined,
  placement: 'top',
  trigger: 'hover',
  showArrow: false,
  disabled: false,
  show: undefined,
  delay: 150,
  duration: undefined,
  nowrap: true,
  maxWidth: undefined,
  width: undefined,
  zIndex: 100000,
  to: 'body',
  tooltipClass: undefined,
  tooltipStyle: undefined,
  keepAliveOnHover: true,
  raw: false,
  theme: undefined,
  size: 'medium',
  internalSyncTargetWithParent: undefined,
  internalInheritedEventHandlers: undefined
})

const emit = defineEmits<{
  'update:show': [value: boolean]
  updateShow: [value: boolean]
}>()

const modelShow = defineModel<boolean>('show')
const slots = useSlots()

const triggerRef = ref<HTMLElement | null>(null)
const popperRef = ref<HTMLElement | null>(null)
const isVisible = ref(false)
const isPositioned = ref(false)

const actualPlacement = ref<NonNullable<VTooltipProps['placement']>>(props.placement || 'top')
const popperCoordinates = ref<{ top: number; left: number }>({ top: -9999, left: -9999 })
const arrowCoordinates = ref<{ left?: number; top?: number }>({})

let showTimer: number | null = null
let hideTimer: number | null = null
let rafId: number | null = null
let currentTargetEl: HTMLElement | null = null
let mutationObserver: MutationObserver | null = null

const hasContent = computed<boolean>(() => {
  return props.content !== undefined && props.content !== null && props.content !== ''
})

const shouldShow = computed<boolean>(() => {
  if (props.disabled) return false
  if (hasContent.value) return true
  if (Boolean(slots.content)) return true
  return Boolean(slots.trigger) && Boolean(slots.default)
})

const isTeleportEnabled = computed<boolean>(() => {
  return props.to !== false
})

const teleportTarget = computed<string | HTMLElement>(() => {
  if (props.to === false || props.to === undefined) return 'body'
  return props.to
})

const computedTooltipClass = computed(() => {
  return [
    'volur-tooltip-popover',
    'v-tooltip-popover',
    `size-${props.size}`,
    {
      'is-nowrap': props.nowrap,
      'is-raw': props.raw,
      [`theme-${props.theme}`]: Boolean(props.theme)
    },
    props.tooltipClass
  ]
})

function isTargetHovered(): boolean {
  if (currentTargetEl) {
    try {
      if (currentTargetEl.matches(':hover')) return true
    } catch {
      return false
    }
  }
  if (triggerRef.value) {
    try {
      if (triggerRef.value.matches(':hover')) return true
    } catch {
      return false
    }
  }
  return false
}

function getTargetRect(): DOMRect | null {
  if (currentTargetEl) {
    return currentTargetEl.getBoundingClientRect()
  }
  if (!triggerRef.value) return null
  const el = triggerRef.value
  const child = el.firstElementChild as HTMLElement | null
  if (child && (child.offsetWidth > 0 || child.offsetHeight > 0)) {
    return child.getBoundingClientRect()
  }
  if (child) {
    const rect = child.getBoundingClientRect()
    if (rect.width > 0 || rect.height > 0) return rect
  }
  const range = document.createRange()
  range.selectNodeContents(el)
  const rect = range.getBoundingClientRect()
  if (rect.width > 0 || rect.height > 0) {
    return rect
  }
  return el.getBoundingClientRect()
}

const computedPopperStyle = computed<CSSProperties>(() => {
  const style: CSSProperties = {
    top: `${popperCoordinates.value.top}px`,
    left: `${popperCoordinates.value.left}px`,
    visibility: isPositioned.value ? 'visible' : 'hidden',
    zIndex: props.zIndex ?? 100000
  }

  if (props.maxWidth !== undefined) {
    style.maxWidth = typeof props.maxWidth === 'number' ? `${props.maxWidth}px` : props.maxWidth
  }

  if (props.width === 'trigger') {
    const rect = getTargetRect()
    if (rect) {
      style.width = `${rect.width}px`
    }
  } else if (typeof props.width === 'number') {
    style.width = `${props.width}px`
  }

  if (props.theme) {
    style.colorScheme = props.theme
  }

  if (typeof props.tooltipStyle === 'object' && props.tooltipStyle !== null) {
    return { ...style, ...props.tooltipStyle }
  }

  return style
})

const computedArrowStyle = computed<CSSProperties>(() => {
  const style: CSSProperties = {}
  if (arrowCoordinates.value.left !== undefined) {
    style.left = `${arrowCoordinates.value.left}px`
  }
  if (arrowCoordinates.value.top !== undefined) {
    style.top = `${arrowCoordinates.value.top}px`
  }
  return style
})

function computeCoordinates(
  triggerRect: DOMRect,
  popperWidth: number,
  popperHeight: number,
  placement: NonNullable<VTooltipProps['placement']>,
  showArrow: boolean
) {
  const gap = showArrow ? 10 : 8
  const viewportWidth = window.innerWidth
  const viewportHeight = window.innerHeight
  const padding = 6

  let effectivePlacement = placement

  const hasTopSpace = triggerRect.top - popperHeight - gap >= padding
  const hasBottomSpace = triggerRect.bottom + popperHeight + gap <= viewportHeight - padding
  const hasLeftSpace = triggerRect.left - popperWidth - gap >= padding
  const hasRightSpace = triggerRect.right + popperWidth + gap <= viewportWidth - padding

  if (effectivePlacement.startsWith('top') && !hasTopSpace && hasBottomSpace) {
    effectivePlacement = effectivePlacement.replace('top', 'bottom') as NonNullable<VTooltipProps['placement']>
  } else if (effectivePlacement.startsWith('bottom') && !hasBottomSpace && hasTopSpace) {
    effectivePlacement = effectivePlacement.replace('bottom', 'top') as NonNullable<VTooltipProps['placement']>
  } else if (effectivePlacement.startsWith('left') && !hasLeftSpace && hasRightSpace) {
    effectivePlacement = effectivePlacement.replace('left', 'right') as NonNullable<VTooltipProps['placement']>
  } else if (effectivePlacement.startsWith('right') && !hasRightSpace && hasLeftSpace) {
    effectivePlacement = effectivePlacement.replace('right', 'left') as NonNullable<VTooltipProps['placement']>
  }

  let top = 0
  let left = 0

  const triggerCenterX = triggerRect.left + triggerRect.width / 2
  const triggerCenterY = triggerRect.top + triggerRect.height / 2

  switch (effectivePlacement) {
    case 'top':
      top = triggerRect.top - popperHeight - gap
      left = triggerCenterX - popperWidth / 2
      break
    case 'top-start':
      top = triggerRect.top - popperHeight - gap
      left = triggerRect.left
      break
    case 'top-end':
      top = triggerRect.top - popperHeight - gap
      left = triggerRect.right - popperWidth
      break
    case 'bottom':
      top = triggerRect.bottom + gap
      left = triggerCenterX - popperWidth / 2
      break
    case 'bottom-start':
      top = triggerRect.bottom + gap
      left = triggerRect.left
      break
    case 'bottom-end':
      top = triggerRect.bottom + gap
      left = triggerRect.right - popperWidth
      break
    case 'left':
      top = triggerCenterY - popperHeight / 2
      left = triggerRect.left - popperWidth - gap
      break
    case 'left-start':
      top = triggerRect.top
      left = triggerRect.left - popperWidth - gap
      break
    case 'left-end':
      top = triggerRect.bottom - popperHeight
      left = triggerRect.left - popperWidth - gap
      break
    case 'right':
      top = triggerCenterY - popperHeight / 2
      left = triggerRect.right + gap
      break
    case 'right-start':
      top = triggerRect.top
      left = triggerRect.right + gap
      break
    case 'right-end':
      top = triggerRect.bottom - popperHeight
      left = triggerRect.right + gap
      break
  }

  const boundedLeft = Math.max(padding, Math.min(left, viewportWidth - popperWidth - padding))
  const boundedTop = Math.max(padding, Math.min(top, viewportHeight - popperHeight - padding))

  let arrowLeft: number | undefined
  let arrowTop: number | undefined

  if (showArrow) {
    if (effectivePlacement.startsWith('top') || effectivePlacement.startsWith('bottom')) {
      arrowLeft = Math.max(10, Math.min(triggerCenterX - boundedLeft, popperWidth - 10))
    } else {
      arrowTop = Math.max(10, Math.min(triggerCenterY - boundedTop, popperHeight - 10))
    }
  }

  return {
    top: Math.round(boundedTop),
    left: Math.round(boundedLeft),
    placement: effectivePlacement,
    arrowLeft: arrowLeft !== undefined ? Math.round(arrowLeft) : undefined,
    arrowTop: arrowTop !== undefined ? Math.round(arrowTop) : undefined
  }
}

function updatePosition() {
  if (!triggerRef.value || !popperRef.value) return
  const triggerRect = getTargetRect()
  if (!triggerRect) return

  const popperWidth = popperRef.value.offsetWidth || popperRef.value.scrollWidth || 0
  const popperHeight = popperRef.value.offsetHeight || popperRef.value.scrollHeight || 0
  if (popperWidth === 0 && popperHeight === 0) return

  const result = computeCoordinates(
    triggerRect,
    popperWidth,
    popperHeight,
    props.placement || 'top',
    props.showArrow
  )

  actualPlacement.value = result.placement
  popperCoordinates.value = { top: result.top, left: result.left }
  arrowCoordinates.value = { left: result.arrowLeft, top: result.arrowTop }
}

function handleScrollOrResize() {
  if (!isVisible.value) return
  if (rafId !== null) {
    cancelAnimationFrame(rafId)
  }
  rafId = requestAnimationFrame(() => {
    updatePosition()
    rafId = null
  })
}

function getTargetElement(): HTMLElement | null {
  if (!triggerRef.value) return null
  const child = triggerRef.value.firstElementChild as HTMLElement | null
  if (child) return child
  return triggerRef.value
}

function bindTargetEvents(el: HTMLElement | null) {
  if (currentTargetEl === el) return
  if (currentTargetEl) {
    currentTargetEl.removeEventListener('mouseenter', handleTriggerMouseEnter)
    currentTargetEl.removeEventListener('mouseleave', handleTriggerMouseLeave)
    currentTargetEl.removeEventListener('focusin', handleTriggerFocusIn)
    currentTargetEl.removeEventListener('focusout', handleTriggerFocusOut)
    currentTargetEl.removeEventListener('click', handleTriggerClick)
  }
  currentTargetEl = el
  if (currentTargetEl) {
    currentTargetEl.addEventListener('mouseenter', handleTriggerMouseEnter)
    currentTargetEl.addEventListener('mouseleave', handleTriggerMouseLeave)
    currentTargetEl.addEventListener('focusin', handleTriggerFocusIn)
    currentTargetEl.addEventListener('focusout', handleTriggerFocusOut)
    currentTargetEl.addEventListener('click', handleTriggerClick)
  }
}

function handleDocumentClick(e: MouseEvent) {
  if (!isVisible.value) return
  const target = e.target as Node | null
  if (!target) return
  if (currentTargetEl?.contains(target)) return
  if (triggerRef.value?.contains(target)) return
  if (popperRef.value?.contains(target)) return
  close()
}

function open() {
  if (props.disabled || !shouldShow.value) return
  if (props.trigger === 'hover' && !isTargetHovered()) return

  if (showTimer !== null) {
    window.clearTimeout(showTimer)
    showTimer = null
  }

  if (activeTooltipCloser && activeTooltipCloser !== close) {
    activeTooltipCloser()
  }
  activeTooltipCloser = close

  isPositioned.value = false
  isVisible.value = true
  if (modelShow.value !== undefined) {
    modelShow.value = true
  }
  emit('update:show', true)
  emit('updateShow', true)

  nextTick(() => {
    if (!isVisible.value) return
    if (props.trigger === 'hover' && !isTargetHovered()) {
      close()
      return
    }
    updatePosition()
    isPositioned.value = true
    window.addEventListener('scroll', handleScrollOrResize, true)
    window.addEventListener('resize', handleScrollOrResize)
    if (props.trigger === 'click') {
      document.addEventListener('pointerdown', handleDocumentClick)
    }
  })
}

function close() {
  if (showTimer !== null) {
    window.clearTimeout(showTimer)
    showTimer = null
  }
  if (hideTimer !== null) {
    window.clearTimeout(hideTimer)
    hideTimer = null
  }
  if (activeTooltipCloser === close) {
    activeTooltipCloser = null
  }
  if (!isVisible.value) return
  isVisible.value = false
  isPositioned.value = false
  if (modelShow.value !== undefined) {
    modelShow.value = false
  }
  emit('update:show', false)
  emit('updateShow', false)

  window.removeEventListener('scroll', handleScrollOrResize, true)
  window.removeEventListener('resize', handleScrollOrResize)
  document.removeEventListener('pointerdown', handleDocumentClick)
}

function handleTriggerMouseEnter() {
  if (props.trigger !== 'hover') return
  if (hideTimer !== null) {
    window.clearTimeout(hideTimer)
    hideTimer = null
  }
  if (showTimer !== null) {
    window.clearTimeout(showTimer)
    showTimer = null
  }
  if (isVisible.value) return
  const delay = props.delay ?? 150
  if (delay > 0) {
    showTimer = window.setTimeout(() => {
      showTimer = null
      open()
    }, delay)
  } else {
    open()
  }
}

function handleTriggerMouseLeave(e?: MouseEvent) {
  if (props.trigger !== 'hover') return
  if (showTimer !== null) {
    window.clearTimeout(showTimer)
    showTimer = null
  }
  if (!isVisible.value) return
  if (e && props.keepAliveOnHover && popperRef.value) {
    const related = e.relatedTarget as Node | null
    if (related && popperRef.value.contains(related)) {
      return
    }
  }
  const duration = props.duration ?? (props.keepAliveOnHover ? 80 : 0)
  if (duration > 0) {
    hideTimer = window.setTimeout(() => {
      hideTimer = null
      close()
    }, duration)
  } else {
    close()
  }
}

function handlePopperMouseEnter() {
  if (props.trigger !== 'hover' || !props.keepAliveOnHover) return
  if (hideTimer !== null) {
    window.clearTimeout(hideTimer)
    hideTimer = null
  }
}

function handlePopperMouseLeave(e: MouseEvent) {
  if (props.trigger !== 'hover' || !props.keepAliveOnHover) return
  if (!isVisible.value) return
  const related = e.relatedTarget as Node | null
  if (related && (currentTargetEl?.contains(related) || triggerRef.value?.contains(related))) {
    return
  }
  hideTimer = window.setTimeout(() => {
    hideTimer = null
    close()
  }, 80)
}

function handleTriggerFocusIn() {
  if (props.trigger !== 'focus') return
  open()
}

function handleTriggerFocusOut() {
  if (props.trigger !== 'focus') return
  close()
}

function handleTriggerClick() {
  if (props.trigger !== 'click') return
  if (isVisible.value) {
    close()
  } else {
    open()
  }
}

watch(
  () => props.disabled,
  (val) => {
    if (val) {
      close()
    }
  }
)

watch(
  [() => props.show, modelShow],
  ([pShow, mShow]) => {
    const target = mShow !== undefined ? mShow : pShow
    if (target !== undefined) {
      if (target) {
        open()
      } else {
        close()
      }
    }
  },
  { immediate: true }
)

watch(
  [() => props.content, () => props.placement],
  () => {
    if (isVisible.value) {
      nextTick(updatePosition)
    }
  }
)

onMounted(() => {
  bindTargetEvents(getTargetElement())
  if (triggerRef.value) {
    mutationObserver = new MutationObserver(() => {
      bindTargetEvents(getTargetElement())
    })
    mutationObserver.observe(triggerRef.value, { childList: true })
  }
})

onBeforeUnmount(() => {
  if (mutationObserver) {
    mutationObserver.disconnect()
    mutationObserver = null
  }
  bindTargetEvents(null)
  if (showTimer !== null) {
    window.clearTimeout(showTimer)
  }
  if (hideTimer !== null) {
    window.clearTimeout(hideTimer)
  }
  if (rafId !== null) {
    cancelAnimationFrame(rafId)
  }
  if (activeTooltipCloser === close) {
    activeTooltipCloser = null
  }
  window.removeEventListener('scroll', handleScrollOrResize, true)
  window.removeEventListener('resize', handleScrollOrResize)
  document.removeEventListener('pointerdown', handleDocumentClick)
})

defineExpose({
  triggerEl: triggerRef,
  popperEl: popperRef,
  open,
  close,
  updatePosition
})
</script>

<style lang="scss">
.volur-tooltip-trigger {
  display: contents;
}

.volur-tooltip-popover {
  position: fixed;
  box-sizing: border-box;
  border: 1px solid var(--color-v1-outline-var);
  padding: 8px 12px;
  font-size: 12px;
  line-height: 14px;
  color: light-dark(#2b2d31, #bdc1c6);
  background-color: light-dark(#ffffff, #292a2d);
  border-radius: 8px;
  box-shadow: var(--v1-shadow-sm);
  font-family: 'Lato', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  user-select: none;
  pointer-events: auto;
  transition:
    background-color 0.2s cubic-bezier(0.2, 0, 0, 1),
    color 0.2s cubic-bezier(0.2, 0, 0, 1),
    border-color 0.2s cubic-bezier(0.2, 0, 0, 1),
    box-shadow 0.2s cubic-bezier(0.2, 0, 0, 1);

  &[data-placement^="top"] {
    transform-origin: center bottom;
  }

  &[data-placement^="bottom"] {
    transform-origin: center top;
  }

  &[data-placement^="left"] {
    transform-origin: right center;
  }

  &[data-placement^="right"] {
    transform-origin: left center;
  }

  &.size-small {
    padding: 4px 8px;
    font-size: 11px;
    line-height: 11px;
    border-radius: 6px;
  }

  &.size-large {
    padding: 10px 14px;
    font-size: 16px;
    line-height: 16px;
    border-radius: 10px;
  }

  &.is-nowrap {
    white-space: nowrap;
    writing-mode: horizontal-tb;
    width: max-content;
    min-width: max-content;
    max-width: none;
    word-break: keep-all;

    .volur-tooltip__content {
      white-space: nowrap;
      writing-mode: horizontal-tb;
      width: max-content;
      min-width: max-content;
      max-width: none;
      word-break: keep-all;
      display: inline-block;
      font-size: inherit;
      line-height: inherit;
    }
  }

  &.theme-light {
    color-scheme: light;
  }

  &.theme-dark {
    color-scheme: dark;
  }

  &.is-raw {
    padding: 0;
    border: none;
    background-color: transparent;
    box-shadow: none;
    border-radius: 0;
  }
}

.volur-tooltip-arrow {
  position: absolute;
  width: 8px;
  height: 8px;
  background-color: light-dark(#ffffff, #292a2d);
  border: 1px solid var(--color-v1-outline-var);
  pointer-events: none;
  z-index: 1;
}

.volur-tooltip-popover[data-placement^="top"] > .volur-tooltip-arrow {
  bottom: -5px;
  border-top: none;
  border-left: none;
  transform: translateX(-50%) rotate(45deg);
}

.volur-tooltip-popover[data-placement^="bottom"] > .volur-tooltip-arrow {
  top: -5px;
  border-bottom: none;
  border-right: none;
  transform: translateX(-50%) rotate(45deg);
}

.volur-tooltip-popover[data-placement^="left"] > .volur-tooltip-arrow {
  right: -5px;
  border-bottom: none;
  border-left: none;
  transform: translateY(-50%) rotate(45deg);
}

.volur-tooltip-popover[data-placement^="right"] > .volur-tooltip-arrow {
  left: -5px;
  border-top: none;
  border-right: none;
  transform: translateY(-50%) rotate(45deg);
}

.volur-tooltip-fade-enter-active,
.volur-tooltip-fade-leave-active {
  transition:
    opacity 0.15s cubic-bezier(0, 0, 0.2, 1),
    transform 0.15s cubic-bezier(0, 0, 0.2, 1);
}

.volur-tooltip-fade-enter-from,
.volur-tooltip-fade-leave-to {
  opacity: 0;
  transform: scale(0.88);
}

.v-directive-tooltip {
  position: fixed;
  z-index: 100000;
  pointer-events: none;
  box-sizing: border-box;
  padding: 8px 12px;
  font-size: 14px;
  line-height: 14px;
  color: light-dark(#2b2d31, #bdc1c6);
  background-color: light-dark(#ffffff, #292a2d);
  border: 1px solid var(--color-v1-outline-var);
  border-radius: 8px;
  box-shadow: var(--v1-shadow-sm);
  white-space: nowrap;
  opacity: 0;
  transform: scale(0.85);
  transition:
    opacity 0.15s cubic-bezier(0, 0, 0.2, 1),
    transform 0.15s cubic-bezier(0, 0, 0.2, 1),
    background-color 0.2s cubic-bezier(0.2, 0, 0, 1),
    color 0.2s cubic-bezier(0.2, 0, 0, 1),
    border-color 0.2s cubic-bezier(0.2, 0, 0, 1),
    box-shadow 0.2s cubic-bezier(0.2, 0, 0, 1);

  &[data-placement^="top"] {
    transform-origin: center bottom;
  }

  &[data-placement^="bottom"] {
    transform-origin: center top;
  }

  &[data-placement^="left"] {
    transform-origin: right center;
  }

  &[data-placement^="right"] {
    transform-origin: left center;
  }

  &.is-visible {
    opacity: 1;
    transform: scale(1);
  }

  &.theme-light {
    color-scheme: light;
  }

  &.theme-dark {
    color-scheme: dark;
  }
}
</style>
