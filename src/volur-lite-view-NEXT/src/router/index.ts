import { createRouter, createWebHistory } from 'vue-router'
import { routes } from './router'

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

  const isAuthRequired = to.matched.some(
    (record) => record.meta.requiredAuth === true
  )

  const token =
    typeof localStorage !== 'undefined'
      ? localStorage.getItem('access_token') || sessionStorage.getItem('access_token')
      : null

  if (isAuthRequired && !token) {
    return {
      path: '/v1/signin/identifier',
      query: { redirect: to.fullPath }
    }
  }

  return true
})

export default router
