import type { RouteRecordRaw } from 'vue-router'

export const commonRoutes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/competitions'
  },
  {
    path: '/notFound',
    name: 'NotFound',
    component: () => import('@/views/error/NotFoundView.vue'),
    meta: {
      title: '404 - 页面未找到',
      requiredAuth: false
    }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/notFound'
  }
]
