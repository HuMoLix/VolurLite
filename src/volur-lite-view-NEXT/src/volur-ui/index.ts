import type { App, Plugin } from 'vue'
import {
  VConfigProvider,
  VLoadingBarProvider,
  VDialogProvider,
  VNotificationProvider,
  VMessageProvider,
  VGlobalStyle,
  VolurConfigProvider
} from './provider'
import { VButton } from './components/button'
import { VFieldInput } from './components/input'

export * from './_styles/tokens'
export * from './_styles/theme'
export * from './_mixins/use-namespace'
export * from './_mixins/use-theme'
export * from './provider'
export * from './components/button'
export * from './components/input'

const components = [
  VConfigProvider,
  VLoadingBarProvider,
  VDialogProvider,
  VNotificationProvider,
  VMessageProvider,
  VGlobalStyle,
  VolurConfigProvider,
  VButton,
  VFieldInput
]

export const VolurUI: Plugin = {
  install(app: App) {
    components.forEach((component) => {
      if (component.name) {
        app.component(component.name, component)
      }
    })
  }
}

export default VolurUI
