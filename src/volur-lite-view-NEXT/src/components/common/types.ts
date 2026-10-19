import type { CSSProperties } from 'vue'
import type {
  PopoverPlacement,
  PopoverTrigger,
  SelectOption,
  SelectGroupOption,
  DataTableColumns,
  DataTableRowKey,
  PaginationProps,
  GlobalThemeOverrides
} from 'naive-ui'

export type VTooltipSize = 'small' | 'medium' | 'large'

export interface FilterOption<T = string | number> {
  value: T
  label?: string
  icon?: any
  count?: number | string
  disabled?: boolean
  title?: string
}

export interface VFilterBtnGroupProps<T = string | number> {
  modelValue?: T
  value?: T
  options: (FilterOption<T> | T)[]
  responsive?: boolean
  bullet?: boolean
  disabled?: boolean
  maxVisible?: number
  breakpoints?: {
    mobile?: number
    tablet?: number
  }
  collapseVisible?: {
    mobile?: number
    tablet?: number
  }
  dropdownWidth?: number | string
  moreText?: string
  size?: 'small' | 'medium' | 'large'
}

export interface VTooltipProps {
  content?: string | number
  placement?: PopoverPlacement
  trigger?: PopoverTrigger
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
  tooltipClass?: any
  tooltipStyle?: CSSProperties | string
  keepAliveOnHover?: boolean
  raw?: boolean
  theme?: 'light' | 'dark'
  size?: VTooltipSize
  internalSyncTargetWithParent?: boolean
  internalInheritedEventHandlers?: any[]
}

export interface VInputProps {
  modelValue?: string | null
  value?: string | null
  placeholder?: string
  clearable?: boolean
  disabled?: boolean
  size?: 'small' | 'medium' | 'large'
  round?: boolean
  radius?: number | string
  theme?: 'light' | 'dark'
  autoFocus?: boolean
  type?: 'text' | 'password' | 'textarea'
  maxlength?: number | string
  showCount?: boolean
  readonly?: boolean
  autosize?: boolean | { minRows?: number; maxRows?: number }
  rows?: number | string
}

export interface VSearchProps extends VInputProps {}

export interface VSelectBoxProps {
  modelValue?: boolean
  checked?: boolean
  disabled?: boolean
  indeterminate?: boolean
  size?: 'small' | 'medium' | 'large'
  round?: boolean
  radius?: number | string
  theme?: 'light' | 'dark'
  label?: string
}

export type SelectValue = string | number | (string | number)[] | null

export interface VSelectionProps {
  modelValue?: SelectValue
  value?: SelectValue
  options?: (SelectOption | SelectGroupOption)[]
  placeholder?: string
  clearable?: boolean
  disabled?: boolean
  size?: 'small' | 'medium' | 'large'
  round?: boolean
  radius?: number | string
  theme?: 'light' | 'dark'
  multiple?: boolean
  filterable?: boolean
  maxTagCount?: number | 'responsive'
  consistentMenuWidth?: boolean
  placement?: PopoverPlacement
  loading?: boolean
  to?: string | HTMLElement | false
  zIndex?: number
  menuProps?: Record<string, any>
}

export type VBtnClz = 'primary' | 'normal' | 'warning' | 'danger'
export type VBtnSize = 'tiny' | 'small' | 'medium' | 'large'

export interface VBtnProps {
  clz?: VBtnClz
  leftIcon?: any
  rightIcon?: any
  iconSize?: number
  size?: VBtnSize
  round?: boolean
  radius?: number | string
  disabled?: boolean
  loading?: boolean
  type?: 'button' | 'submit' | 'reset'
  block?: boolean
  text?: string
}

export type DataTableThemeOverrides = NonNullable<GlobalThemeOverrides['DataTable']>

export type VTableSize = 'small' | 'medium' | 'large'

export interface VTableProps<T extends object = any> {
  bordered?: boolean
  bottomBordered?: boolean
  backgroundColor?: string
  columns?: DataTableColumns<T>
  data?: T[]
  loading?: boolean
  pagination?: PaginationProps | false
  rowKey?: (row: T) => DataTableRowKey
  size?: VTableSize
  singleLine?: boolean
  singleColumn?: boolean
  striped?: boolean
  scrollX?: number | string
  maxHeight?: number | string
  minHeight?: number | string
  virtualScroll?: boolean
  checkedRowKeys?: DataTableRowKey[]
  remote?: boolean
  round?: boolean
  radius?: number | string
  theme?: 'light' | 'dark'
  themeOverrides?: DataTableThemeOverrides
}

export interface VModalProps {
  show?: boolean
  title?: string
  description?: string
  width?: number | string
  maxWidth?: number | string
  height?: number | string
  maxHeight?: number | string
  bordered?: boolean
  round?: boolean
  radius?: number | string
  closable?: boolean
  mask?: boolean
  maskClosable?: boolean
  closeOnEsc?: boolean
  autoFocus?: boolean
  trapFocus?: boolean
  zIndex?: number
  to?: string | HTMLElement
  showHeader?: boolean
  theme?: 'light' | 'dark'
  panelClass?: any
  panelStyle?: CSSProperties
  contentClass?: any
  headerClass?: any
  footerClass?: any
  closeAriaLabel?: string
}

export type VPaginationSize = 'small' | 'medium' | 'large'

export interface VPaginationSizeOption {
  label: string
  value: number
  [key: string]: unknown
}

export interface VPaginationProps {
  page?: number
  pageSize?: number
  itemCount?: number
  pageCount?: number
  pageSizes?: (number | VPaginationSizeOption)[]
  showSizePicker?: boolean
  showQuickJumper?: boolean
  disabled?: boolean
  size?: VPaginationSize
  pageSlot?: number
  simple?: boolean
  round?: boolean
  radius?: number | string
  theme?: 'light' | 'dark'
}

export interface VIconProps {
  name?: string | null
  size?: number | string
  color?: string
  depth?: 1 | 2 | 3 | 4 | 5
  component?: any
}

export interface VIconPickerProps {
  modelValue?: string | null
  value?: string | null
  placeholder?: string
  disabled?: boolean
  clearable?: boolean
  size?: 'small' | 'medium' | 'large'
  placement?: PopoverPlacement
  width?: string | number
  popoverWidth?: number
  round?: boolean
  radius?: number | string
  theme?: 'light' | 'dark'
  zIndex?: number
  to?: string | boolean | HTMLElement
}

export interface VMarkdownEditorProps {
  value?: string
  modelValue?: string
  placeholder?: string
  rows?: number
  minHeight?: string | number
  maxHeight?: string | number
  disabled?: boolean
  showTip?: boolean
  tipText?: string
  emptyPreviewText?: string
  tabSize?: number
  autoPair?: boolean
  autoIndent?: boolean
  showStatusBar?: boolean
  showToolbarActions?: boolean
  theme?: 'light' | 'dark'
}

export interface VBannerProps {
  src?: string
  value?: string
  modelValue?: string
  alt?: string
  height?: number | string
  radius?: number | string
  editable?: boolean
  uploadFolder?: string
  accept?: string
  placeholderText?: string
  theme?: 'light' | 'dark'
  uploadApi?: (file: File) => Promise<any>
  clearable?: boolean
}

export interface VDatePickerProps {
  modelValue?: string | number | (string | number)[] | null
  value?: string | number | (string | number)[] | null
  startValue?: string | number | null
  endValue?: string | number | null
  formattedValue?: string | null
  type?: 'date' | 'datetime' | 'daterange' | 'datetimerange' | 'month' | 'year' | 'quarter'
  placeholder?: string
  startPlaceholder?: string
  endPlaceholder?: string
  clearable?: boolean
  disabled?: boolean
  size?: 'small' | 'medium' | 'large'
  round?: boolean
  radius?: number | string
  theme?: 'light' | 'dark'
  format?: string
  valueFormat?: string
  separator?: string
  isDateDisabled?: (current: number) => boolean
}
