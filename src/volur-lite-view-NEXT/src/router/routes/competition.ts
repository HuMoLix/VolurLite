import type { RouteRecordRaw } from 'vue-router'

export const competitionRoutes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/components/layouts/AppLayout.vue'),
    children: [
      {
        path: 'competitions',
        name: 'Competitions',
        component: () => import('@/views/competition/CompetitionListView.vue'),
        meta: {
          title: '赛事大厅',
          requiredAuth: false
        }
      },
      {
        path: 'competitions/:id',
        name: 'CompetitionDetail',
        component: () => import('@/views/competition/CompetitionDetailView.vue'),
        meta: {
          title: '赛事详情',
          requiredAuth: false
        }
      },
      {
        path: 'c/:id',
        name: 'CompetitionShortLink',
        component: () => import('@/views/competition/CompetitionDetailView.vue'),
        meta: {
          title: '赛事直达',
          requiredAuth: false
        }
      },
      {
        path: 'competitions/:id/theory',
        name: 'TheoryCompetitionArena',
        component: () => import('@/views/competition/TheoryCompetitionView.vue'),
        meta: {
          title: '理论知识赛考场',
          requiredAuth: true
        }
      }
    ]
  }
]

