export type VButtonVariant =
  | 'primary'
  | 'primary-highest'
  | 'secondary'
  | 'secondary-lowest'
  | 'normal'
  | 'tonal'
  | 'danger'
  | 'warning'
  | 'text'
export type VButtonPresetSize = 'tiny' | 'small' | 'medium' | 'large'
export type VButtonSize = VButtonPresetSize | number | string
export type VButtonIconPlacement = 'left' | 'right'
export type VButtonAttrType = 'button' | 'submit' | 'reset'

export interface VButtonProps {
  variant?: VButtonVariant
  size?: VButtonSize
  radius?: number | string
  round?: boolean
  circle?: boolean
  dashed?: boolean
  block?: boolean
  disabled?: boolean
  loading?: boolean
  attrType?: VButtonAttrType
  iconPlacement?: VButtonIconPlacement
  isPressWave?: boolean
  isFocusRing?: boolean
}

export interface VButtonEmits {
  (e: 'click', event: MouseEvent): void
}
