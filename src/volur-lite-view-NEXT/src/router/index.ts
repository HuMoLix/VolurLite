import { createRouter, createWebHistory } from 'vue-router'
import { routes } from './router'
import { isDshExpired, buildSessionExpireLocation } from '@/utils/flow'
import { useAppStore } from '@/store/modules/app'
import { useUserStore } from '@/store/modules/user'
import { normalizeLocale, loadLocaleForRouteAsync, i18n } from '@/i18n'
import { formatPageTitle } from '@/utils/title'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach(async (to) => {
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

  if (typeof document !== 'undefined') {
    const rawTitle = to.meta.titleKey
      ? (i18n.global as any).t(to.meta.titleKey)
      : to.meta.title
    document.title = formatPageTitle(rawTitle as string | undefined)
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
  const isAdminRequired =
    to.matched.some((record) => record.meta.requiredAdmin === true) ||
    to.path.startsWith('/admin')

  const isLoggedIn =
    typeof document !== 'undefined' &&
    /(?:^|;\s*)logged_in=true/.test(document.cookie)

  if ((isAuthRequired || isAdminRequired) && !isLoggedIn) {
    return {
      path: '/v1/signin/identifier',
      query: { redirect: to.fullPath }
    }
  }

  if (isAdminRequired) {
    const userStore = useUserStore()
    if (!userStore.user && isLoggedIn) {
      await userStore.fetchCurrentUser()
    }
    if (!userStore.isAdmin) {
      return { path: '/competitions' }
    }
  }

  return true
})

export default router
