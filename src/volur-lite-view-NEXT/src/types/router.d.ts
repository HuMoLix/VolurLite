import 'vue-router'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    cardTitle?: string
    cardSubtitle?: string
    requiredAuth?: boolean
    roles?: string[]
    keepAlive?: boolean
    icon?: string
    hidden?: boolean
  }
}

export {}
