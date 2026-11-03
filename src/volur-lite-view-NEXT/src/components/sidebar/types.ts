import type { Component, CSSProperties, VNodeChild } from 'vue'
import type { MenuOption } from 'naive-ui'
import type { VAvatarProps, VCardProps, VDrawerProps } from '@/components/common'

export type { VAvatarProps, VCardProps, VDrawerProps }

export interface SidebarNavItem {
  key: string
  route?: string
  titleKey?: string
  label?: string | (() => string | VNodeChild)
  icon?: Component | string | (() => VNodeChild)
  children?: SidebarNavItem[]
}

export interface SidebarNavGroup {
  key: string
  title?: string
  titleKey?: string
  label?: string | (() => string | VNodeChild)
  items: SidebarNavItem[]
}

export interface VSideBarGroup {
  key: string
  title?: string
  titleKey?: string
  label?: string | (() => string | VNodeChild)
  options: MenuOption[]
}

export interface VSearchToolProps {
  show?: boolean
  placeholder?: string
  debounceDelay?: number
  searchApi?: (keyword: string) => Promise<any>
  theme?: 'light' | 'dark'
}


export interface VSideBarProps {
  width?: number | string
  collapsedWidth?: number | string
  collapsed?: boolean
  showTrigger?: boolean | 'bar' | 'arrow-circle'
  bordered?: boolean
  collapseMode?: 'width' | 'transform'
  items?: SidebarNavItem[]
  groups?: SidebarNavGroup[]
  menuOptions?: MenuOption[]
  menuGroups?: VSideBarGroup[]
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
  groups?: SidebarNavGroup[]
  menuOptions?: MenuOption[]
  menuGroups?: VSideBarGroup[]
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

