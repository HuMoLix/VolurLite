export { default as VConfigProvider } from './VConfigProvider.vue'
export { default as VLoadingBarProvider } from './VLoadingBarProvider.vue'
export { default as VDialogProvider } from './VDialogProvider.vue'
export { default as VNotificationProvider } from './VNotificationProvider.vue'
export { default as VMessageProvider } from './VMessageProvider.vue'
export { default as VGlobalStyle } from './VGlobalStyle.vue'
export { default as VolurConfigProvider } from './VolurConfigProvider.vue'
export * from './context'
export { createVolurTheme } from '../_styles/theme'

export function useVolurMessage() {
  if (typeof window !== 'undefined' && window.$volurMessage) {
    return window.$volurMessage
  }
  throw new Error('useVolurMessage must be used within VMessageProvider')
}

export function useVolurDialog() {
  if (typeof window !== 'undefined' && window.$volurDialog) {
    return window.$volurDialog
  }
  throw new Error('useVolurDialog must be used within VDialogProvider')
}

export function useVolurNotification() {
  if (typeof window !== 'undefined' && window.$volurNotification) {
    return window.$volurNotification
  }
  throw new Error('useVolurNotification must be used within VNotificationProvider')
}

export function useVolurLoadingBar() {
  if (typeof window !== 'undefined' && window.$volurLoadingBar) {
    return window.$volurLoadingBar
  }
  throw new Error('useVolurLoadingBar must be used within VLoadingBarProvider')
}
