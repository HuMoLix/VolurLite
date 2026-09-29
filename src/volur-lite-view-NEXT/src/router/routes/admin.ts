import type { RouteRecordRaw } from 'vue-router'

export const adminRoutes: RouteRecordRaw[] = [
  {
    path: '/admin',
    redirect: '/admin/dashboard'
  },
  {
    path: '/admin/dashboard',
    name: 'AdminDashboard',
    component: () => import('@/views/admin/AdminDashboardView.vue'),
    meta: {
      title: '管理员控制台',
      requiredAuth: true,
      roles: ['ROLE_ADMIN', 'ROLE_SUPER_ADMIN']
    }
  }
]
