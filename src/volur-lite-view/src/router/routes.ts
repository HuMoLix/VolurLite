import type { RouteRecordRaw } from 'vue-router'
import { AppLayout, AuthLayout, AdminLayout } from '@/public/volurEx'

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
        path: 'competitions/:slug',
        name: 'CompetitionDetailBySlug',
        component: () => import('@/views/competition/CompetitionDetailView.vue'),
        meta: {
          title: '竞赛详情',
          titleKey: 'competition.detail_title',
          parentTitle: '竞赛演练',
          parentTitleKey: 'competition.page_title',
          parentPath: '/competitions',
          requiresAuth: false
        }
      },
      {
        path: 'c/:slug',
        name: 'CompetitionShortLink',
        component: () => import('@/views/competition/CompetitionDetailView.vue'),
        meta: {
          title: '竞赛短链直达',
          titleKey: 'competition.detail_title',
          parentTitle: '竞赛演练',
          parentTitleKey: 'competition.page_title',
          parentPath: '/competitions',
          requiresAuth: false
        }
      },
      {
        path: 'announcements',
        name: 'Announcements',
        component: () => import('@/views/announcement/AnnouncementListView.vue'),
        meta: { title: '全站公告', titleKey: 'layout.announcements', requiresAuth: false }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/ProfileView.vue'),
        meta: { title: '个人中心', titleKey: 'layout.profile', requiresAuth: true }
      }
    ]
  },
  {
    path: '/arena/:slug',
    component: () => import('@/public/volurEx/layouts/competitions/CtfCompetitionLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'CompetitionArena',
        component: () => import('@/views/competition/ctf/CtfCompetitionArenaView.vue'),
        meta: { title: '竞赛竞技场', titleKey: 'competition.arena_title', requiresAuth: true }
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
            path: 'ctf/create',
            name: 'AdminCtfChallengeCreate',
            component: () => import('@/views/admin/challenge/AdminCtfChallengeEditView.vue'),
            meta: {
              title: '新增 CTF 赛题',
              parentTitle: 'CTF 题库',
              parentTitleKey: 'admin.challenges.ctf_title'
            }
          },
          {
            path: 'ctf/:id/edit',
            name: 'AdminCtfChallengeEdit',
            component: () => import('@/views/admin/challenge/AdminCtfChallengeEditView.vue'),
            meta: {
              title: '编辑 CTF 赛题',
              parentTitle: 'CTF 题库',
              parentTitleKey: 'admin.challenges.ctf_title'
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
        component: () => import('@/views/admin/competition/AdminCompetitionListView.vue'),
        meta: { title: '竞赛管理', titleKey: 'admin.competitions.title' }
      },
      {
        path: 'competitions/create',
        name: 'AdminCompetitionCreate',
        component: () => import('@/views/admin/competition/AdminCompetitionEditView.vue'),
        meta: {
          title: '创建竞赛',
          titleKey: 'admin.competitions.modal_create_title',
          parentTitle: '竞赛管理',
          parentTitleKey: 'admin.competitions.title'
        }
      },
      {
        path: 'competitions/:id/edit',
        name: 'AdminCompetitionEdit',
        component: () => import('@/views/admin/competition/AdminCompetitionEditView.vue'),
        meta: {
          title: '比赛管理',
          titleKey: 'admin.competitions.manage_title',
          parentTitle: '竞赛管理',
          parentTitleKey: 'admin.competitions.title'
        }
      },
      {
        path: 'competitions/:id/preview',
        name: 'AdminCompetitionPreview',
        component: () => import('@/views/competition/CompetitionDetailView.vue'),
        meta: {
          title: '前台预览',
          titleKey: 'admin.competitions.preview_title',
          parentTitle: '竞赛管理',
          parentTitleKey: 'admin.competitions.title',
          parentPath: '/admin/competitions'
        }
      },
      {
        path: 'competitions/edit/:id',
        redirect: to => `/admin/competitions/${to.params.id}/edit`
      },
      {
        path: 'broadcasts',
        name: 'AdminBroadcasts',
        component: () => import('@/views/admin/broadcast/AdminBroadcastView.vue'),
        meta: { title: '公告管理', titleKey: 'admin.broadcasts.title' }
      },
      {
        path: 'clusters',
        name: 'AdminClusterSettings',
        component: () => import('@/views/admin/cluster/AdminClusterSettingsView.vue'),
        meta: {
          title: '集群设置',
          titleKey: 'admin.clusters.settings_title',
          parentTitle: '集群管理',
          parentTitleKey: 'layout.menu.clusters'
        }
      },
      {
        path: 'containers',
        name: 'AdminContainers',
        component: () => import('@/views/admin/container/AdminContainerView.vue'),
        meta: {
          title: '容器管理',
          titleKey: 'admin.containers.title',
          parentTitle: '集群管理',
          parentTitleKey: 'layout.menu.clusters'
        }
      },
      {
        path: 'images',
        name: 'AdminImages',
        component: () => import('@/views/admin/image/AdminImageView.vue'),
        meta: {
          title: '镜像管理',
          titleKey: 'admin.images.title',
          parentTitle: '集群管理',
          parentTitleKey: 'layout.menu.clusters'
        }
      },
      {
        path: 'logs',
        name: 'AdminLogs',
        component: () => import('@/views/admin/log/AdminLogView.vue'),
        meta: { title: '系统日志', titleKey: 'admin.logs.title' }
      },
      {
        path: 'tasks',
        name: 'AdminTasks',
        component: () => import('@/views/admin/task/AdminTaskView.vue'),
        meta: { title: '任务管理', titleKey: 'admin.tasks.title' }
      },
      {
        path: 'emails',
        name: 'AdminEmails',
        component: () => import('@/views/admin/email/AdminEmailView.vue'),
        meta: { title: '邮件管理', titleKey: 'admin.emails.title' }
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
        path: 'reset-password',
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
    path: '/forgot',
    redirect: (to) => ({ path: '/auth/forgot', query: to.query })
  },
  {
    path: '/forgot-password',
    redirect: (to) => ({ path: '/auth/forgot', query: to.query })
  },
  {
    path: '/maintenance',
    name: 'Maintenance',
    component: () => import('@/views/error/MaintenanceView.vue'),
    meta: { title: '全站维护中', requiresAuth: false }
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/error/NotFoundView.vue'),
    meta: { title: '404 - 页面未找到', titleKey: 'error.not_found_title' }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'CatchAll',
    redirect: '/404'
  }
]
