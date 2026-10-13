import type { RouteRecordRaw } from 'vue-router'

export const adminRoutes: RouteRecordRaw[] = [
  {
    path: '/admin',
    component: () => import('@/components/layouts/AdminLayout.vue'),
    redirect: '/admin/dashboard',
    meta: {
      requiredAuth: true,
      requiredAdmin: true
    },
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/AdminDashboardView.vue'),
        meta: {
          title: '工作台',
          titleKey: 'admin.dashboard.title',
          requiredAuth: true,
          requiredAdmin: true,
          roles: ['ROLE_ADMIN', 'ROLE_SUPER_ADMIN']
        }
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/AdminUserView.vue'),
        meta: {
          title: '用户管理',
          titleKey: 'admin.users.title',
          requiredAuth: true,
          requiredAdmin: true,
          roles: ['ROLE_ADMIN', 'ROLE_SUPER_ADMIN']
        }
      },
      {
        path: 'challenges/ctf',
        name: 'AdminCtfChallenges',
        component: () => import('@/views/admin/challenge/AdminCtfChallengeView.vue'),
        meta: {
          title: 'CTF 题库',
          titleKey: 'admin.challenges.ctf_title',
          requiredAuth: true,
          requiredAdmin: true,
          roles: ['ROLE_ADMIN', 'ROLE_SUPER_ADMIN']
        }
      },
      {
        path: 'challenges/ctf/create',
        name: 'AdminCtfChallengeCreate',
        component: () => import('@/views/admin/challenge/AdminCtfChallengeEditView.vue'),
        meta: {
          title: '新增 CTF 赛题',
          titleKey: 'admin.challenges.create_title',
          requiredAuth: true,
          requiredAdmin: true,
          roles: ['ROLE_ADMIN', 'ROLE_SUPER_ADMIN']
        }
      },
      {
        path: 'challenges/ctf/:id/edit',
        name: 'AdminCtfChallengeEdit',
        component: () => import('@/views/admin/challenge/AdminCtfChallengeEditView.vue'),
        meta: {
          title: '编辑 CTF 赛题',
          titleKey: 'admin.challenges.edit_title',
          requiredAuth: true,
          requiredAdmin: true,
          roles: ['ROLE_ADMIN', 'ROLE_SUPER_ADMIN']
        }
      },
      {
        path: 'challenges/theory',
        name: 'AdminTheoryChallenges',
        component: () => import('@/views/admin/challenge/AdminTheoryChallengeView.vue'),
        meta: {
          title: '理论题库',
          titleKey: 'admin.challenges.theory_title',
          requiredAuth: true,
          requiredAdmin: true,
          roles: ['ROLE_ADMIN', 'ROLE_SUPER_ADMIN']
        }
      },
      {
        path: 'challenges/theory/:id/edit',
        name: 'AdminTheoryChallengeEdit',
        component: () => import('@/views/admin/challenge/AdminTheoryChallengeEditView.vue'),
        meta: {
          title: '管理理论题库题目',
          titleKey: 'admin.challenges.manage_bank_title',
          requiredAuth: true,
          requiredAdmin: true,
          roles: ['ROLE_ADMIN', 'ROLE_SUPER_ADMIN']
        }
      },
      {
        path: 'challenges/isw',
        name: 'AdminIswChallenges',
        component: () => import('@/views/admin/challenge/AdminIswChallengeView.vue'),
        meta: {
          title: 'ISW 题库',
          titleKey: 'admin.challenges.isw_title',
          requiredAuth: true,
          requiredAdmin: true,
          roles: ['ROLE_ADMIN', 'ROLE_SUPER_ADMIN']
        }
      },
      {
        path: 'challenges/awd',
        name: 'AdminAwdChallenges',
        component: () => import('@/views/admin/challenge/AdminAwdChallengeView.vue'),
        meta: {
          title: 'AWD 题库',
          titleKey: 'admin.challenges.awd_title',
          requiredAuth: true,
          requiredAdmin: true,
          roles: ['ROLE_ADMIN', 'ROLE_SUPER_ADMIN']
        }
      },
      {
        path: 'challenges/awdp',
        name: 'AdminAwdPlusChallenges',
        component: () => import('@/views/admin/challenge/AdminAwdPlusChallengeView.vue'),
        meta: {
          title: 'AWD+ 题库',
          titleKey: 'admin.challenges.awd_plus_title',
          requiredAuth: true,
          requiredAdmin: true,
          roles: ['ROLE_ADMIN', 'ROLE_SUPER_ADMIN']
        }
      },
      {
        path: 'competitions',
        name: 'AdminCompetitions',
        component: () => import('@/views/admin/competition/AdminCompetitionListView.vue'),
        meta: {
          title: '竞赛管理',
          titleKey: 'admin.competitions.title',
          requiredAuth: true,
          requiredAdmin: true,
          roles: ['ROLE_ADMIN', 'ROLE_SUPER_ADMIN']
        }
      },
      {
        path: 'competitions/create',
        name: 'AdminCompetitionCreate',
        component: () => import('@/views/admin/competition/AdminCompetitionCreateView.vue'),
        meta: {
          title: '创建竞赛',
          titleKey: 'admin.competitions.modal_create_title',
          requiredAuth: true,
          requiredAdmin: true,
          roles: ['ROLE_ADMIN', 'ROLE_SUPER_ADMIN']
        }
      },
      {
        path: 'competitions/ctf/create',
        name: 'AdminCtfCompetitionCreate',
        component: () => import('@/views/admin/competition/AdminCompetitionCreateView.vue'),
        meta: {
          title: '创建 CTF 竞赛',
          titleKey: 'admin.competitions.modal_create_title',
          requiredAuth: true,
          requiredAdmin: true,
          roles: ['ROLE_ADMIN', 'ROLE_SUPER_ADMIN']
        }
      },
      {
        path: 'competitions/ctf/:id/edit',
        name: 'AdminCtfCompetitionEdit',
        component: () => import('@/views/admin/competition/AdminCtfCompetitionEditView.vue'),
        meta: {
          title: 'CTF 竞赛运维',
          titleKey: 'admin.competitions.manage_title',
          requiredAuth: true,
          requiredAdmin: true,
          roles: ['ROLE_ADMIN', 'ROLE_SUPER_ADMIN']
        }
      },
      {
        path: 'competitions/theory/create',
        name: 'AdminTheoryCompetitionCreate',
        component: () => import('@/views/admin/competition/AdminCompetitionCreateView.vue'),
        meta: {
          title: '创建理论竞赛',
          titleKey: 'admin.competitions.modal_create_title',
          requiredAuth: true,
          requiredAdmin: true,
          roles: ['ROLE_ADMIN', 'ROLE_SUPER_ADMIN']
        }
      },
      {
        path: 'competitions/theory/:id/edit',
        name: 'AdminTheoryCompetitionEdit',
        component: () => import('@/views/admin/competition/AdminTheoryCompetitionEditView.vue'),
        meta: {
          title: '理论竞赛运维',
          titleKey: 'admin.competitions.manage_title',
          requiredAuth: true,
          requiredAdmin: true,
          roles: ['ROLE_ADMIN', 'ROLE_SUPER_ADMIN']
        }
      },
      {
        path: 'competitions/:id/edit',
        name: 'AdminCompetitionEdit',
        component: () => import('@/views/admin/competition/AdminCompetitionEditView.vue'),
        meta: {
          title: '竞赛运维',
          titleKey: 'admin.competitions.manage_title',
          requiredAuth: true,
          requiredAdmin: true,
          roles: ['ROLE_ADMIN', 'ROLE_SUPER_ADMIN']
        }
      },
      {
        path: 'broadcasts',
        name: 'AdminBroadcasts',
        component: () => import('@/views/admin/broadcast/AdminBroadcastView.vue'),
        meta: {
          title: '公告管理',
          titleKey: 'admin.broadcasts.title',
          requiredAuth: true,
          requiredAdmin: true,
          roles: ['ROLE_ADMIN', 'ROLE_SUPER_ADMIN']
        }
      },
      {
        path: 'clusters',
        redirect: '/admin/clusters/settings'
      },
      {
        path: 'clusters/settings',
        name: 'AdminClusterSettings',
        component: () => import('@/views/admin/cluster/AdminClusterSettingsView.vue'),
        meta: {
          title: '集群设置',
          titleKey: 'admin.clusters.tabs.settings',
          requiredAuth: true,
          requiredAdmin: true,
          roles: ['ROLE_ADMIN', 'ROLE_SUPER_ADMIN']
        }
      },
      {
        path: 'clusters/nodes',
        name: 'AdminClusterNodes',
        component: () => import('@/views/admin/cluster/AdminClusterSettingsView.vue'),
        meta: {
          title: '节点管理',
          titleKey: 'admin.clusters.tabs.nodes',
          requiredAuth: true,
          requiredAdmin: true,
          roles: ['ROLE_ADMIN', 'ROLE_SUPER_ADMIN']
        }
      },
      {
        path: 'clusters/topology',
        name: 'AdminClusterTopology',
        component: () => import('@/views/admin/cluster/AdminClusterSettingsView.vue'),
        meta: {
          title: '集群拓扑',
          titleKey: 'admin.clusters.tabs.topology',
          requiredAuth: true,
          requiredAdmin: true,
          roles: ['ROLE_ADMIN', 'ROLE_SUPER_ADMIN']
        }
      },
      {
        path: 'containers',
        name: 'AdminContainers',
        component: () => import('@/views/admin/container/AdminContainerView.vue'),
        meta: {
          title: '容器管理',
          titleKey: 'admin.containers.title',
          requiredAuth: true,
          requiredAdmin: true,
          roles: ['ROLE_ADMIN', 'ROLE_SUPER_ADMIN']
        }
      },
      {
        path: 'images',
        name: 'AdminImages',
        component: () => import('@/views/admin/image/AdminImageView.vue'),
        meta: {
          title: '镜像管理',
          titleKey: 'admin.images.title',
          requiredAuth: true,
          requiredAdmin: true,
          roles: ['ROLE_ADMIN', 'ROLE_SUPER_ADMIN']
        }
      },
      {
        path: 'logs',
        name: 'AdminLogs',
        component: () => import('@/views/admin/log/AdminLogView.vue'),
        meta: {
          title: '系统日志',
          titleKey: 'admin.logs.title',
          requiredAuth: true,
          requiredAdmin: true,
          roles: ['ROLE_ADMIN', 'ROLE_SUPER_ADMIN']
        }
      },
      {
        path: 'tasks',
        name: 'AdminTasks',
        component: () => import('@/views/admin/task/AdminTaskView.vue'),
        meta: {
          title: '任务管理',
          titleKey: 'admin.tasks.title',
          requiredAuth: true,
          requiredAdmin: true,
          roles: ['ROLE_ADMIN', 'ROLE_SUPER_ADMIN']
        }
      },
      {
        path: 'emails',
        name: 'AdminEmails',
        component: () => import('@/views/admin/email/AdminEmailView.vue'),
        meta: {
          title: '邮件管理',
          titleKey: 'admin.emails.title',
          requiredAuth: true,
          requiredAdmin: true,
          roles: ['ROLE_ADMIN', 'ROLE_SUPER_ADMIN']
        }
      },
      {
        path: 'risk/settings',
        name: 'AdminRiskSettings',
        component: () => import('@/views/admin/risk/AdminRiskSettingsView.vue'),
        meta: {
          title: '风控设置',
          titleKey: 'admin.risk.settings.title',
          requiredAuth: true,
          requiredAdmin: true,
          roles: ['ROLE_ADMIN', 'ROLE_SUPER_ADMIN']
        }
      },
      {
        path: 'risk/rules',
        name: 'AdminRiskRules',
        component: () => import('@/views/admin/risk/AdminRiskRulesView.vue'),
        meta: {
          title: '策略规则',
          titleKey: 'admin.risk.policy.title',
          requiredAuth: true,
          requiredAdmin: true,
          roles: ['ROLE_ADMIN', 'ROLE_SUPER_ADMIN']
        }
      },
      {
        path: 'risk/ip',
        name: 'AdminRiskIp',
        component: () => import('@/views/admin/risk/AdminRiskIpView.vue'),
        meta: {
          title: 'IP风控治理',
          titleKey: 'admin.risk.ip.title',
          requiredAuth: true,
          requiredAdmin: true,
          roles: ['ROLE_ADMIN', 'ROLE_SUPER_ADMIN']
        }
      },
      {
        path: 'system',
        name: 'AdminSystem',
        component: () => import('@/views/admin/AdminSystemView.vue'),
        meta: {
          title: '系统设置',
          titleKey: 'admin.system.title',
          requiredAuth: true,
          requiredAdmin: true,
          roles: ['ROLE_ADMIN', 'ROLE_SUPER_ADMIN']
        }
      }
    ]
  }
]
