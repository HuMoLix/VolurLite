import type { App, Plugin } from 'vue'
import { vTooltip } from './tooltip'

export * from './tooltip'

export const DirectivesPlugin: Plugin = {
  install(app: App) {
    app.directive('tooltip', vTooltip)
  }
}

export default DirectivesPlugin
