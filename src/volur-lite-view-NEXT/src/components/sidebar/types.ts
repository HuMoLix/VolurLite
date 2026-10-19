import type { Component, CSSProperties, VNodeChild } from 'vue'
import type { MenuOption } from 'naive-ui'

export interface SidebarNavItem {
  key: string
  route?: string
  titleKey?: string
  label?: string | (() => string | VNodeChild)
  icon?: Component | string | (() => VNodeChild)
  children?: SidebarNavItem[]
}

export interface VSearchToolProps {
  show?: boolean
  placeholder?: string
  debounceDelay?: number
  searchApi?: (keyword: string) => Promise<any>
  theme?: 'light' | 'dark'
}

export interface VAvatarProps {
  src?: string
  alt?: string
  text?: string
  size?: number | string
  shape?: 'square' | 'circle'
  round?: boolean
  radius?: number | string
  ring?: boolean
  ringColor?: string
  theme?: 'light' | 'dark'
  editable?: boolean
  uploadFolder?: string
  accept?: string
  uploadApi?: (file: File) => Promise<any>
}

export interface VSideBarProps {
  width?: number | string
  collapsedWidth?: number | string
  collapsed?: boolean
  showTrigger?: boolean | 'bar' | 'arrow-circle'
  bordered?: boolean
  collapseMode?: 'width' | 'transform'
  items?: SidebarNavItem[]
  menuOptions?: MenuOption[]
  value?: string
  accordion?: boolean
  collapsedIconSize?: number
  showLogo?: boolean
  logoRoute?: string | null
  logoExpandedSrc?: string
  logoCollapsedSrc?: string
  logoAlt?: string
  theme?: 'light' | 'dark'
  showToggleTrigger?: boolean
  showPrimaryAction?: boolean
  primaryActionText?: string
  primaryActionCollapsedText?: string
  primaryActionIcon?: Component
  primaryActionRoute?: string
  showActionBox?: boolean
  showNotifications?: boolean
  showSettings?: boolean
  showSearch?: boolean
  showThemeToggle?: boolean
  showUserProfile?: boolean
  avatarShape?: 'square' | 'circle'
  avatarRadius?: number | string
  avatarRing?: boolean
  notificationRoute?: string
  settingsRoute?: string
  adminRoute?: string
  feedbackRoute?: string
  searchRoute?: string
  searchPlaceholder?: string
  searchApi?: (keyword: string) => Promise<any>
  userProfileRoute?: string
  signinRoute?: string
  accountChooserRoute?: string
  privacyRoute?: string
  termsRoute?: string
  showBottomAction?: boolean
  bottomActionText?: string
  bottomActionCollapsedText?: string
  bottomActionIcon?: Component
  bottomActionRoute?: string
  stickyScrollThreshold?: number
  contentStyle?: string | CSSProperties
  siderClass?: string
}

export interface VBottomBarProps {
  value?: string
  items?: SidebarNavItem[]
  menuOptions?: MenuOption[]
  theme?: 'light' | 'dark'
  showBottomAction?: boolean
  bottomActionText?: string
  bottomActionIcon?: Component
  bottomActionRoute?: string
}

export interface VNoticeProps {
  theme?: 'light' | 'dark'
  notificationRoute?: string
  signinRoute?: string
  maxItems?: number
}

export interface VCardProps {
  theme?: 'light' | 'dark'
  width?: number | string
  minWidth?: number | string
  maxWidth?: number | string
  height?: number | string
  padding?: string
  radius?: number | string
  gap?: number | string
  bordered?: boolean
  closable?: boolean
  title?: string
  closeAriaLabel?: string
}

export interface VUserCardProps {
  theme?: 'light' | 'dark'
  user?: any
  isLoggedIn?: boolean
  displayName?: string
  userEmail?: string
  userInitial?: string
  avatarUrl?: string
  avatarShape?: 'square' | 'circle'
  avatarRadius?: number | string
  avatarRing?: boolean
  logoSrc?: string
  logoAlt?: string
  accountChooserRoute?: string
  userProfileRoute?: string
  signinRoute?: string
  privacyRoute?: string
  termsRoute?: string
}

export interface VSettingCardProps {
  theme?: 'light' | 'dark'
  width?: number | string
  userSettingsRoute?: string
  adminRoute?: string
  feedbackRoute?: string
  termsRoute?: string
  privacyRoute?: string
  closeAriaLabel?: string
}

export interface VDrawerProps {
  show?: boolean
  title?: string
  width?: number | string
  placement?: 'right' | 'left'
  closable?: boolean
  mask?: boolean
  maskClosable?: boolean
  theme?: 'light' | 'dark'
  closeAriaLabel?: string
}

export interface VFeedbackProps {
  show?: boolean
  theme?: 'light' | 'dark'
  title?: string
  width?: number | string
  placeholder?: string
}

export interface SideBarProps extends VSideBarProps {
  breakpoint?: number
}

export type { VTooltipProps, VTooltipSize } from '@/components/common/types'

