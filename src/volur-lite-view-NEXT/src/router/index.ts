import { createRouter, createWebHistory } from 'vue-router'
import { routes } from './router'
import { isDshExpired, buildSessionExpireLocation } from '@/utils/flow'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach((to) => {
  if (to.meta.title && typeof document !== 'undefined') {
    document.title = `${to.meta.title}`
  }

  if (to.path !== '/v1/info/sessionexpire') {
    const dshParam = typeof to.query.dsh === 'string' ? to.query.dsh : ''
    if (dshParam && isDshExpired(dshParam)) {
      return buildSessionExpireLocation(to)
    }
  }

  const isAuthRequired = to.matched.some(
    (record) => record.meta.requiredAuth === true
  )

  const isLoggedIn =
    typeof document !== 'undefined' &&
    /(?:^|;\s*)logged_in=true/.test(document.cookie)

  if (isAuthRequired && !isLoggedIn) {
    return {
      path: '/v1/signin/identifier',
      query: { redirect: to.fullPath }
    }
  }

  return true
})

export default router
