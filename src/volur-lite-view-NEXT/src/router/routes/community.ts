import type { RouteRecordRaw } from 'vue-router'

export const communityRoutes: RouteRecordRaw[] = [
  {
    path: '/announcements',
    component: () => import('@/layouts/primary/AppLayout.vue'),
    children: [
      {
        path: '',
        name: 'AnnouncementCenter',
        component: () => import('@/views/announcement/AnnouncementListView.vue'),
        meta: {
          title: '公告中心',
          titleKey: 'announcement.title',
          requiredAuth: false
        }
      }
    ]
  },
  {
    path: '/learning',
    component: () => import('@/layouts/primary/AppLayout.vue'),
    children: [
      {
        path: '',
        name: 'LearningCenter',
        component: () => import('@/views/module/ModulePlaceholderView.vue'),
        meta: {
          title: '学习中心',
          titleKey: 'modules.learning.title',
          moduleKey: 'learning',
          requiredAuth: false
        }
      }
    ]
  },
  {
    path: '/resources',
    component: () => import('@/layouts/primary/AppLayout.vue'),
    children: [
      {
        path: '',
        name: 'ResourceCenter',
        component: () => import('@/views/module/ModulePlaceholderView.vue'),
        meta: {
          title: '资源中心',
          titleKey: 'modules.resources.title',
          moduleKey: 'resources',
          requiredAuth: false
        }
      }
    ]
  },
  {
    path: '/experimental',
    component: () => import('@/layouts/primary/AppLayout.vue'),
    children: [
      {
        path: '',
        name: 'ExperimentalFeatures',
        component: () => import('@/views/module/ModulePlaceholderView.vue'),
        meta: {
          title: '实验性功能',
          titleKey: 'modules.experimental.title',
          moduleKey: 'experimental',
          requiredAuth: false
        }
      }
    ]
  },
  {
    path: '/help',
    component: () => import('@/layouts/primary/AppLayout.vue'),
    children: [
      {
        path: '',
        name: 'HelpCenter',
        component: () => import('@/views/module/ModulePlaceholderView.vue'),
        meta: {
          title: '帮助中心',
          titleKey: 'modules.help.title',
          moduleKey: 'help',
          requiredAuth: false
        }
      }
    ]
  }
]
