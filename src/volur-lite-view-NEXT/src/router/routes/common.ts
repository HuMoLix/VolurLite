import type { RouteRecordRaw } from 'vue-router'

export const commonRoutes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/competitions'
  },
  {
    path: '/v1/home',
    name: 'V1Home',
    component: () => import('@/views/home/HomeView.vue'),
    meta: {
      title: '控制台工作台',
      requiredAuth: true
    }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/competitions'
  }
]
