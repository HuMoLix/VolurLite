import type { Component, CSSProperties, VNodeChild } from 'vue'
import type { CompetitionSummaryVO } from '@/types/competition'

export interface SidebarNavItem {
  key: string
  route?: string
  titleKey?: string
  label?: string | (() => string | VNodeChild)
  icon?: Component | string | (() => VNodeChild)
  badge?: string | number | (() => string | number | VNodeChild)
  badgeColor?: string
  children?: SidebarNavItem[]
  disabled?: boolean
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
  items?: SidebarNavItem[]
  options?: any[]
}

export interface SideBarLayoutProps {
  collapsed?: boolean
  width?: number | string
  collapsedWidth?: number | string
  bordered?: boolean
  collapseMode?: 'width' | 'transform'
  contentStyle?: string | CSSProperties
  siderClass?: string
}

export interface SideBarNavHeaderProps {
  collapsed?: boolean
  showLogo?: boolean
  logoRoute?: string | null
  logoExpandedSrc?: string
  logoCollapsedSrc?: string
  logoAlt?: string
  showToggleTrigger?: boolean
  theme?: 'light' | 'dark'
}

export interface SideBarNavContentProps {
  collapsed?: boolean
  groups: SidebarNavGroup[]
  currentValue?: string
  accordion?: boolean
  collapsedIconSize?: number
  showPrimaryAction?: boolean
  primaryActionText?: string
  primaryActionCollapsedText?: string
  primaryActionIcon?: Component
  primaryActionRoute?: string
}

export interface SideBarNavGroupProps {
  group: SidebarNavGroup
  groupIndex?: number
  collapsed?: boolean
  currentValue?: string
  accordion?: boolean
  collapsedIconSize?: number
}

export interface SideBarNavFooterProps {
  collapsed?: boolean
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
  breakpoint?: number
}

export interface VSideBarEmits {
  (e: 'select', key: string, item: SidebarNavItem): void
  (e: 'collapse'): void
  (e: 'expand'): void
  (e: 'logo-click', ev: MouseEvent): void
  (e: 'primary-action-click', ev: MouseEvent): void
  (e: 'notification-click', ev: MouseEvent): void
  (e: 'settings-click', ev: MouseEvent): void
  (e: 'search-click', ev: MouseEvent): void
  (e: 'search-select', item: CompetitionSummaryVO): void
  (e: 'search-open'): void
  (e: 'search-close'): void
  (e: 'theme-toggle'): void
  (e: 'user-click', ev: MouseEvent): void
  (e: 'switch-account', ev: MouseEvent): void
  (e: 'logout', ev: MouseEvent): void
  (e: 'profile-click', ev: MouseEvent): void
  (e: 'user-card-open'): void
  (e: 'user-card-close'): void
  (e: 'notice-card-open'): void
  (e: 'notice-card-close'): void
  (e: 'setting-card-open'): void
  (e: 'setting-card-close'): void
  (e: 'setting-item-click', key: string): void
  (e: 'admin-click', ev: MouseEvent): void
  (e: 'bottom-action-click', ev: MouseEvent): void
}

export interface NoticeCardProps {
  theme?: 'light' | 'dark'
  notificationRoute?: string
  signinRoute?: string
  maxItems?: number
}

export interface UserCardProps {
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

export interface SettingCardProps {
  theme?: 'light' | 'dark'
  width?: number | string
  userSettingsRoute?: string
  adminRoute?: string
  feedbackRoute?: string
  termsRoute?: string
  privacyRoute?: string
  closeAriaLabel?: string
}

export interface SearchProps {
  show?: boolean
  placeholder?: string
  debounceDelay?: number
  searchApi?: (keyword: string) => Promise<any>
  theme?: 'light' | 'dark'
}

export interface FeedbackProps {
  show?: boolean
  title?: string
  width?: number | string
  placeholder?: string
  theme?: 'light' | 'dark'
  api?: (content: string) => Promise<any>
}
