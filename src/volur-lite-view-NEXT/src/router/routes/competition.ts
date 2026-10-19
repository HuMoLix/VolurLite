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
      }
    ]
  },
  {
    path: '/competitions/:id/theory/require',
    name: 'TheoryCompetitionRequire',
    component: () => import('@/views/competition/TheoryCompetitionRequireView.vue'),
    meta: {
      title: '理论知识赛准入检查',
      requiredAuth: true
    }
  },
  {
    path: '/c/:id/theory/require',
    name: 'TheoryCompetitionRequireShortLink',
    component: () => import('@/views/competition/TheoryCompetitionRequireView.vue'),
    meta: {
      title: '理论知识赛准入检查',
      requiredAuth: true
    }
  },
  {
    path: '/competitions/:id/theory',
    name: 'TheoryCompetitionArena',
    component: () => import('@/views/competition/TheoryCompetitionView.vue'),
    meta: {
      title: '理论知识赛考场',
      requiredAuth: true
    }
  },
  {
    path: '/c/:id/theory',
    name: 'TheoryCompetitionArenaShortLink',
    component: () => import('@/views/competition/TheoryCompetitionView.vue'),
    meta: {
      title: '理论知识赛考场',
      requiredAuth: true
    }
  },
  {
    path: '/competitions/:id/ctf',
    name: 'CtfCompetitionArena',
    component: () => import('@/views/competition/CtfCompetitionView.vue'),
    meta: {
      title: 'CTF 竞技场',
      requiredAuth: true
    }
  },
  {
    path: '/competitions/:id/ctf/challenges',
    name: 'CtfCompetitionChallenges',
    component: () => import('@/views/competition/CtfCompetitionView.vue'),
    meta: {
      title: 'CTF 竞技场 - 赛题列表',
      requiredAuth: true
    }
  },
  {
    path: '/competitions/:id/ctf/annoucements',
    alias: '/competitions/:id/ctf/announcements',
    name: 'CtfCompetitionAnnouncements',
    component: () => import('@/views/competition/CtfCompetitionView.vue'),
    meta: {
      title: 'CTF 竞技场 - 公告列表',
      requiredAuth: true
    }
  },
  {
    path: '/competitions/:id/ctf/scoreboard',
    name: 'CtfCompetitionScoreboard',
    component: () => import('@/views/competition/CtfCompetitionView.vue'),
    meta: {
      title: 'CTF 竞技场 - 排行榜',
      requiredAuth: true
    }
  },
  {
    path: '/competitions/:id/ctf/team',
    name: 'CtfCompetitionMyTeam',
    component: () => import('@/views/competition/CtfCompetitionView.vue'),
    meta: {
      title: 'CTF 竞技场 - 我的队伍',
      requiredAuth: true
    }
  },
  {
    path: '/competitions/:id/ctf/team/:teamId',
    name: 'CtfCompetitionTeamDetail',
    component: () => import('@/views/competition/CtfCompetitionView.vue'),
    meta: {
      title: 'CTF 竞技场 - 队伍详情',
      requiredAuth: true
    }
  },
  {
    path: '/c/:id/ctf',
    name: 'CtfCompetitionArenaShortLink',
    component: () => import('@/views/competition/CtfCompetitionView.vue'),
    meta: {
      title: 'CTF 竞技场',
      requiredAuth: true
    }
  },
  {
    path: '/c/:id/ctf/challenges',
    component: () => import('@/views/competition/CtfCompetitionView.vue'),
    meta: {
      title: 'CTF 竞技场 - 赛题列表',
      requiredAuth: true
    }
  },
  {
    path: '/c/:id/ctf/annoucements',
    alias: '/c/:id/ctf/announcements',
    component: () => import('@/views/competition/CtfCompetitionView.vue'),
    meta: {
      title: 'CTF 竞技场 - 公告列表',
      requiredAuth: true
    }
  },
  {
    path: '/c/:id/ctf/scoreboard',
    component: () => import('@/views/competition/CtfCompetitionView.vue'),
    meta: {
      title: 'CTF 竞技场 - 排行榜',
      requiredAuth: true
    }
  },
  {
    path: '/c/:id/ctf/team',
    component: () => import('@/views/competition/CtfCompetitionView.vue'),
    meta: {
      title: 'CTF 竞技场 - 我的队伍',
      requiredAuth: true
    }
  },
  {
    path: '/c/:id/ctf/team/:teamId',
    component: () => import('@/views/competition/CtfCompetitionView.vue'),
    meta: {
      title: 'CTF 竞技场 - 队伍详情',
      requiredAuth: true
    }
  }
]

