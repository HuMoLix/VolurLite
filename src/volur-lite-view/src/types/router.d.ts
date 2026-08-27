import 'vue-router'

declare module 'vue-router' {
  interface RouteMeta {
    /** 页面标题 */
    title?: string
    /** 国际化标题键名 */
    titleKey?: string
    /** 父级路由标题 */
    parentTitle?: string
    /** 父级路由国际化键名 */
    parentTitleKey?: string
    /** 是否需要登录鉴权 */
    requiresAuth?: boolean
    /** 所需权限角色列表 (如 ['ROLE_ADMIN', 'ROLE_SUPER_ADMIN']) */
    roles?: string[]
    /** 是否缓存组件 */
    keepAlive?: boolean
    /** 图标名称或标识 */
    icon?: string
    /** 菜单是否隐藏 */
    hidden?: boolean
  }
}

export {}
