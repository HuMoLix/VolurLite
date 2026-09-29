import type { RouteRecordRaw } from 'vue-router'

export const competitionRoutes: RouteRecordRaw[] = [
  {
    path: '/competitions',
    name: 'Competitions',
    component: () => import('@/views/home/HomeView.vue'),
    meta: {
      title: '赛事大厅',
      requiredAuth: false
    }
  },
  {
    path: '/competitions/:id',
    name: 'CompetitionDetail',
    component: () => import('@/views/home/HomeView.vue'),
    meta: {
      title: '赛事工作台',
      requiredAuth: false
    }
  },
  {
    path: '/c/:id',
    name: 'CompetitionShortLink',
    component: () => import('@/views/home/HomeView.vue'),
    meta: {
      title: '赛事直达',
      requiredAuth: false
    }
  }
]
