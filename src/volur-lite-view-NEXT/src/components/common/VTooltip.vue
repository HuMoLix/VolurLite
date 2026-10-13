<template>
  <n-tooltip
    ref="tooltipRef"
    :show="innerShow"
    :placement="placement"
    :trigger="trigger"
    :show-arrow="showArrow"
    :disabled="disabled"
    :delay="delay"
    :duration="duration"
    :z-index="zIndex"
    :to="to"
    :width="width"
    :raw="raw"
    :keep-alive-on-hover="keepAliveOnHover"
    :class="computedTooltipClass"
    :style="computedTooltipStyle"
    :internal-sync-target-with-parent="internalSyncTargetWithParent"
    :internal-inherited-event-handlers="internalInheritedEventHandlers"
    @update:show="handleUpdateShow"
  >
    <template #trigger>
      <slot v-if="$slots.trigger" name="trigger" />
      <slot v-else />
    </template>

    <template v-if="$slots.trigger">
      <slot v-if="$slots.content" name="content" />
      <slot v-else />
    </template>
    <template v-else>
      <slot v-if="$slots.content" name="content" />
      <span v-else-if="hasContent">{{ content }}</span>
    </template>
  </n-tooltip>
</template>

<script setup lang="ts">
import { ref, computed, type CSSProperties } from 'vue'
import { NTooltip } from 'naive-ui'
import { useAppStore } from '@/store/modules/app'
import type { VTooltipProps } from './types'

defineOptions({
  name: 'VTooltip',
  __popover__: true
})

const props = withDefaults(defineProps<VTooltipProps>(), {
  content: undefined,
  placement: 'top',
  trigger: 'hover',
  showArrow: false,
  disabled: false,
  show: undefined,
  delay: undefined,
  duration: undefined,
  nowrap: true,
  maxWidth: undefined,
  width: undefined,
  zIndex: undefined,
  to: undefined,
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
const appStore = useAppStore()
const tooltipRef = ref<InstanceType<typeof NTooltip> | null>(null)

const innerShow = computed<boolean | undefined>({
  get() {
    if (modelShow.value !== undefined) {
      return modelShow.value
    }
    return props.show
  },
  set(val: boolean | undefined) {
    if (val !== undefined) {
      if (modelShow.value !== undefined) {
        modelShow.value = val
      }
      emit('update:show', val)
      emit('updateShow', val)
    }
  }
})

function handleUpdateShow(value: boolean) {
  innerShow.value = value
}

const hasContent = computed<boolean>(() => {
  return props.content !== undefined && props.content !== null && props.content !== ''
})

const effectiveTheme = computed<'light' | 'dark'>(() => {
  return props.theme || (appStore.theme === 'dark' ? 'dark' : 'light')
})

const computedTooltipClass = computed(() => {
  return [
    'v-tooltip-popover',
    `size-${props.size}`,
    {
      'is-nowrap': props.nowrap,
      [`theme-${effectiveTheme.value}`]: true
    },
    props.tooltipClass
  ]
})

const computedTooltipStyle = computed<CSSProperties>(() => {
  const base: CSSProperties = {}
  if (props.maxWidth !== undefined) {
    base.maxWidth = typeof props.maxWidth === 'number' ? `${props.maxWidth}px` : props.maxWidth
  }
  if (typeof props.tooltipStyle === 'object' && props.tooltipStyle !== null) {
    return { ...base, ...props.tooltipStyle }
  }
  return base
})

defineExpose({
  tooltipRef
})
</script>

<style lang="scss">
.n-popover.n-tooltip.v-tooltip-popover {
  border: 1px solid #d7d8da80 !important;
  padding: 8px 12px !important;
  font-size: 14px !important;
  line-height: 14px !important;
  color: #2b2d31 !important;
  background-color: #ffffff !important;
  border-radius: 8px !important;
  box-shadow: 0 1px 3px 0 rgba(10, 13, 18, 0.1), 0 1px 2px -1px rgba(10, 13, 18, 0.1) !important;
  transition: background-color 0.2s cubic-bezier(0.2, 0, 0, 1),
              color 0.2s cubic-bezier(0.2, 0, 0, 1),
              border-color 0.2s cubic-bezier(0.2, 0, 0, 1),
              box-shadow 0.2s cubic-bezier(0.2, 0, 0, 1) !important;

  &.size-small {
    padding: 4px 8px !important;
    font-size: 11px !important;
    line-height: 11px !important;
    border-radius: 6px !important;
  }

  &.size-large {
    padding: 10px 14px !important;
    font-size: 16px !important;
    line-height: 16px !important;
    border-radius: 10px !important;
  }

  &.is-nowrap {
    white-space: nowrap !important;
    writing-mode: horizontal-tb !important;
    width: max-content !important;
    min-width: max-content !important;
    max-width: none !important;
    word-break: keep-all !important;

    .n-popover__content {
      white-space: nowrap !important;
      writing-mode: horizontal-tb !important;
      width: max-content !important;
      min-width: max-content !important;
      max-width: none !important;
      word-break: keep-all !important;
      display: inline-block !important;
      font-size: inherit !important;
      line-height: inherit !important;
    }
  }

  &.theme-dark,
  html.dark & {
    background-color: #292a2d !important;
    border: 1px solid #262626 !important;
    color: #bdc1c6 !important;
    box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.4), 0 1px 2px -1px rgba(0, 0, 0, 0.4) !important;
  }
}
</style>
