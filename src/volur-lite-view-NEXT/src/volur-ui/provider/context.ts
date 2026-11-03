import type { InjectionKey, Ref } from 'vue'
import type {
  GlobalThemeOverrides,
  NLocale,
  NDateLocale,
  useMessage,
  useDialog,
  useNotification,
  useLoadingBar
} from 'naive-ui'

export type VolurMessageApi = ReturnType<typeof useMessage>
export type VolurDialogApi = ReturnType<typeof useDialog>
export type VolurNotificationApi = ReturnType<typeof useNotification>
export type VolurLoadingBarApi = ReturnType<typeof useLoadingBar>

export interface VolurContext {
  theme: Ref<'light' | 'dark'>
  locale: Ref<string>
  themeOverrides: Ref<GlobalThemeOverrides>
  naiveLocale: Ref<NLocale>
  naiveDateLocale: Ref<NDateLocale>
}

export interface VolurFeedbackContext {
  message: VolurMessageApi
  dialog: VolurDialogApi
  notification: VolurNotificationApi
  loadingBar: VolurLoadingBarApi
}

export const VOLUR_CONTEXT_KEY: InjectionKey<VolurContext> = Symbol('VOLUR_CONTEXT_KEY')
export const VOLUR_FEEDBACK_KEY: InjectionKey<VolurFeedbackContext> = Symbol('VOLUR_FEEDBACK_KEY')

declare global {
  interface Window {
    $volurMessage: VolurMessageApi
    $volurDialog: VolurDialogApi
    $volurNotification: VolurNotificationApi
    $volurLoadingBar: VolurLoadingBarApi
    $message: VolurMessageApi
    $dialog: VolurDialogApi
    $notification: VolurNotificationApi
    $loadingBar: VolurLoadingBarApi
  }
}
