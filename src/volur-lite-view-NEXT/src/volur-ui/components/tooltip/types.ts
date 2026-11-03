import type { CSSProperties } from 'vue'

export type VTooltipSize = 'small' | 'medium' | 'large'

export interface VTooltipProps {
  content?: string
  placement?:
    | 'top'
    | 'bottom'
    | 'left'
    | 'right'
    | 'top-start'
    | 'top-end'
    | 'bottom-start'
    | 'bottom-end'
    | 'left-start'
    | 'left-end'
    | 'right-start'
    | 'right-end'
  trigger?: 'hover' | 'click' | 'focus' | 'manual'
  showArrow?: boolean
  disabled?: boolean
  show?: boolean
  delay?: number
  duration?: number
  nowrap?: boolean
  maxWidth?: number | string
  width?: number | 'trigger'
  zIndex?: number
  to?: string | HTMLElement | false
  tooltipClass?: string | Record<string, any> | any[]
  tooltipStyle?: CSSProperties | string
  keepAliveOnHover?: boolean
  raw?: boolean
  theme?: 'light' | 'dark'
  size?: VTooltipSize
  internalSyncTargetWithParent?: boolean
  internalInheritedEventHandlers?: any[]
}
