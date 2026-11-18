import type { RouteRecordRaw } from 'vue-router'

export const trainingRoutes: RouteRecordRaw[] = [
  {
    path: '/challenges',
    component: () => import('@/layouts/primary/AppLayout.vue'),
    children: [
      {
        path: '',
        name: 'TrainingChallenges',
        component: () => import('@/views/training/TrainingListView.vue'),
        meta: {
          title: '题目训练',
          titleKey: 'training.title',
          requiredAuth: true
        }
      }
    ]
  }
]
