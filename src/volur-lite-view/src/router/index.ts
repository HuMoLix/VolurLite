import { createRouter, createWebHistory } from 'vue-router'
import { routes } from './routes'
import { getAccessToken } from '@/utils/auth'
import { useUserStore } from '@/store/modules/user'
import { useSystemStore } from '@/store/modules/system'
import i18n from '@/i18n'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach(async (to) => {
  const token = getAccessToken()
  const userStore = useUserStore()
  const systemStore = useSystemStore()

  if (!systemStore.isLoaded) {
    await systemStore.fetchPublicSettings()
  }

  const siteName = systemStore.platformName || 'Volur Lite'
  const title = to.meta.titleKey
    ? (i18n.global as any).t(to.meta.titleKey as string)
    : (to.meta.title as string) || ''
  document.title = title ? `${title} | ${siteName}` : siteName

  if (systemStore.isMaintenanceMode && !userStore.isAdmin) {
    if (to.path !== '/maintenance' && !to.path.startsWith('/auth') && to.path !== '/reset-password' && !to.path.startsWith('/admin')) {
      return { path: '/maintenance' }
    }
  } else if (to.path === '/maintenance' && systemStore.isLoaded && systemStore.isServerReachable && !systemStore.isMaintenanceMode) {
    return { path: '/' }
  }

  const requiresAuth = to.matched.some((record) => record.meta.requiresAuth)
  const requiredRoles = to.matched.find((record) => record.meta.roles)?.meta.roles

  if (requiresAuth && !token) {
    return { path: '/auth/login', query: { redirect: to.fullPath } }
  }

  if (requiredRoles && requiredRoles.length > 0) {
    if (!userStore.isAdmin) {
      return { path: '/competitions' }
    }
  }

  return true
})

export default router
