import type { RouteRecordRaw } from 'vue-router'
import AppLayout from '@/layout/AppLayout.vue'
import AuthLayout from '@/layout/AuthLayout.vue'
import AdminLayout from '@/layout/AdminLayout.vue'

export const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: AppLayout,
    children: [
      {
        path: '',
        redirect: '/competitions'
      },
      {
        path: 'competitions',
        name: 'Competitions',
        component: () => import('@/views/competition/CompetitionListView.vue'),
        meta: { title: '竞赛演练', titleKey: 'competition.page_title', requiresAuth: false }
      },
      {
        path: 'announcements',
        name: 'Announcements',
        component: () => import('@/views/announcement/AnnouncementListView.vue'),
        meta: { title: '全站公告', titleKey: 'layout.announcements', requiresAuth: false }
      }
    ]
  },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { requiresAuth: true, roles: ['ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ADMIN', 'SUPER_ADMIN'] },
    children: [
      {
        path: '',
        redirect: '/admin/dashboard'
      },
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/dashboard/AdminDashboardView.vue'),
        meta: { title: '控制台', titleKey: 'admin.dashboard.title' }
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/user/AdminUserView.vue'),
        meta: { title: '用户管理', titleKey: 'admin.users.title' }
      },
      {
        path: 'challenges',
        children: [
          {
            path: '',
            redirect: '/admin/challenges/ctf'
          },
          {
            path: 'ctf',
            name: 'AdminCtfChallenges',
            component: () => import('@/views/admin/challenge/AdminCtfChallengeView.vue'),
            meta: {
              title: 'CTF 题库',
              titleKey: 'admin.challenges.ctf_title',
              parentTitle: '题库管理',
              parentTitleKey: 'admin.challenges.title'
            }
          },
          {
            path: 'theory',
            name: 'AdminTheoryChallenges',
            component: () => import('@/views/admin/challenge/AdminTheoryChallengeView.vue'),
            meta: {
              title: '理论题库',
              titleKey: 'admin.challenges.theory_title',
              parentTitle: '题库管理',
              parentTitleKey: 'admin.challenges.title'
            }
          },
          {
            path: 'isw',
            name: 'AdminIswChallenges',
            component: () => import('@/views/admin/challenge/AdminIswChallengeView.vue'),
            meta: {
              title: 'ISW 题库',
              titleKey: 'admin.challenges.isw_title',
              parentTitle: '题库管理',
              parentTitleKey: 'admin.challenges.title'
            }
          },
          {
            path: 'awd',
            name: 'AdminAwdChallenges',
            component: () => import('@/views/admin/challenge/AdminAwdChallengeView.vue'),
            meta: {
              title: 'AWD 题库',
              titleKey: 'admin.challenges.awd_title',
              parentTitle: '题库管理',
              parentTitleKey: 'admin.challenges.title'
            }
          },
          {
            path: 'awdp',
            name: 'AdminAwdPlusChallenges',
            component: () => import('@/views/admin/challenge/AdminAwdPlusChallengeView.vue'),
            meta: {
              title: 'AWD+ 题库',
              titleKey: 'admin.challenges.awd_plus_title',
              parentTitle: '题库管理',
              parentTitleKey: 'admin.challenges.title'
            }
          }
        ]
      },
      {
        path: 'competitions',
        name: 'AdminCompetitions',
        component: () => import('@/views/admin/competition/AdminCompetitionView.vue'),
        meta: { title: '竞赛管理', titleKey: 'admin.competitions.title' }
      },
      {
        path: 'broadcasts',
        name: 'AdminBroadcasts',
        component: () => import('@/views/admin/broadcast/AdminBroadcastView.vue'),
        meta: { title: '公告管理', titleKey: 'admin.broadcasts.title' }
      },
      {
        path: 'containers',
        name: 'AdminContainers',
        component: () => import('@/views/admin/container/AdminContainerView.vue'),
        meta: { title: '容器管理', titleKey: 'admin.containers.title' }
      },
      {
        path: 'logs',
        name: 'AdminLogs',
        component: () => import('@/views/admin/log/AdminLogView.vue'),
        meta: { title: '系统日志', titleKey: 'admin.logs.title' }
      },
      {
        path: 'system',
        name: 'AdminSystem',
        component: () => import('@/views/admin/system/AdminSystemView.vue'),
        meta: { title: '系统设置', titleKey: 'admin.system.title' }
      }
    ]
  },
  {
    path: '/auth',
    component: AuthLayout,
    children: [
      {
        path: '',
        redirect: '/auth/login'
      },
      {
        path: 'login',
        name: 'Login',
        component: () => import('@/views/auth/AuthView.vue'),
        meta: { title: '登录', titleKey: 'auth.log_in' }
      },
      {
        path: 'register',
        name: 'Register',
        component: () => import('@/views/auth/AuthView.vue'),
        meta: { title: '注册', titleKey: 'auth.sign_up' }
      },
      {
        path: 'forgot',
        name: 'ForgotPassword',
        component: () => import('@/views/auth/AuthView.vue'),
        meta: { title: '找回密码', titleKey: 'auth.forgot_password' }
      },
      {
        path: 'reset',
        name: 'ResetPassword',
        component: () => import('@/views/auth/ResetView.vue'),
        meta: { title: '重置密码', titleKey: 'auth.reset_password_title' }
      }
    ]
  },
  {
    path: '/login',
    redirect: '/auth/login'
  },
  {
    path: '/register',
    redirect: '/auth/register'
  },
  {
    path: '/maintenance',
    name: 'Maintenance',
    component: () => import('@/views/error/MaintenanceView.vue'),
    meta: { title: '全站维护中', requiresAuth: false }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/NotFoundView.vue'),
    meta: { title: '404 - 页面未找到', titleKey: 'error.not_found_title' }
  }
]
