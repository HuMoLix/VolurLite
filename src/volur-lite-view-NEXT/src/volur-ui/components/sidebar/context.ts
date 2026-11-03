import type { InjectionKey, Ref, ComputedRef } from 'vue'
import type { SidebarNavItem } from './types'

export interface SidebarContext {
  isCollapsed: Ref<boolean>
  currentValue: Ref<string | undefined>
  theme: ComputedRef<'light' | 'dark'>
  selectItem: (key: string, item: SidebarNavItem) => void
  toggleCollapse: () => void
}

export const SIDEBAR_INJECTION_KEY: InjectionKey<SidebarContext> = Symbol('SIDEBAR_INJECTION_KEY')
