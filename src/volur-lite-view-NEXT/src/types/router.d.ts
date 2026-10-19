import 'vue-router'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    titleKey?: string
    cardTitle?: string
    cardTitleKey?: string
    cardSubtitle?: string
    cardSubtitleKey?: string
    requiredAuth?: boolean
    requiredAdmin?: boolean
    roles?: string[]
    keepAlive?: boolean
    icon?: string
    hidden?: boolean
  }
}

export {}
