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
import { VSideBar } from './components/sidebar'
import { VTooltip } from './components/tooltip'
import { VViewlayout, VFooter } from './layouts'
import { vTooltip } from '@/directives/tooltip'

export * from './_styles/tokens'
export * from './_styles/theme'
export * from './_mixins/use-namespace'
export * from './_mixins/use-theme'
export * from './provider'
export * from './components/button'
export * from './components/input'
export * from './components/sidebar'
export * from './components/tooltip'
export * from './layouts'

const components = [
  VConfigProvider,
  VLoadingBarProvider,
  VDialogProvider,
  VNotificationProvider,
  VMessageProvider,
  VGlobalStyle,
  VolurConfigProvider,
  VButton,
  VFieldInput,
  VSideBar,
  VTooltip,
  VViewlayout,
  VFooter
]

export const VolurUI: Plugin = {
  install(app: App) {
    components.forEach((component) => {
      if (component.name) {
        app.component(component.name, component)
      }
    })
    app.directive('tooltip', vTooltip)
  }
}

export default VolurUI
