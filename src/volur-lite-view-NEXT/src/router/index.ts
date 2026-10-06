import { createRouter, createWebHistory } from 'vue-router'
import { routes } from './router'
import { isDshExpired, buildSessionExpireLocation } from '@/utils/flow'
import { useAppStore } from '@/store/modules/app'
import { normalizeLocale, loadLocaleForRouteAsync } from '@/i18n'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach(async (to) => {
  if (to.meta.title && typeof document !== 'undefined') {
    document.title = `${to.meta.title}`
  }

  const appStore = useAppStore()
  if (to.query.lang) {
    const rawLang = Array.isArray(to.query.lang) ? to.query.lang[0] : to.query.lang
    const normalized = normalizeLocale(rawLang)
    if (normalized && normalized !== appStore.locale) {
      appStore.setLocale(normalized)
    }
  }

  try {
    await loadLocaleForRouteAsync(appStore.locale, to.path)
  } catch {}

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
