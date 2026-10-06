import type { RouteRecordRaw } from 'vue-router'

export const authRoutes: RouteRecordRaw[] = [
  {
    path: '/v1',
    component: () => import('@/components/layouts/auth/AuthLayout.vue'),
    children: [
      {
        path: 'signin',
        children: [
          {
            path: '',
            redirect: (to) => ({
              path: '/v1/signin/accountchooser',
              query: to.query
            })
          },
          {
            path: 'accountchooser',
            name: 'V1SignInAccountChooser',
            component: () => import('@/views/auth/signin/AccountChooserView.vue'),
            meta: {
              title: '选择账号 - Volur 账号',
              cardTitle: '选择账号',
              cardSubtitle: '以继续访问 Volur',
              requiredAuth: false
            }
          },
          {
            path: 'identifier',
            name: 'V1SignInIdentifier',
            component: () => import('@/views/auth/signin/IdentifierView.vue'),
            meta: {
              title: '登录 - Volur 账号',
              cardTitle: '登录',
              cardSubtitle: '使用您的 Volur 账号',
              requiredAuth: false
            }
          },
          {
            path: 'challenge/:type(.*)*',
            name: 'V1SignInChallenge',
            component: () => import('@/views/auth/signin/ChallengeView.vue'),
            meta: {
              title: '登录 - 安全核验与挑战',
              cardTitle: '安全核验与挑战',
              cardSubtitle: '',
              requiredAuth: false
            }
          }
        ]
      },
      {
        path: 'signup',
        children: [
          {
            path: '',
            redirect: (to) => ({
              path: '/v1/signup/steps/name',
              query: to.query
            })
          },
          {
            path: 'steps/:step(.*)*',
            name: 'V1SignUpSteps',
            component: () => import('@/views/auth/signup/SignUpView.vue'),
            meta: {
              title: '注册 - Volur 账号',
              cardTitle: '创建您的 Volur 账号',
              cardSubtitle: '输入您的基本信息以开启网络安全演练之旅',
              requiredAuth: false
            }
          }
        ]
      },
      {
        path: 'info/sessionexpire',
        name: 'V1SessionExpire',
        component: () => import('@/views/auth/sessionExpireView.vue'),
        meta: {
          title: '会话已超时 - Volur 账号',
          cardTitle: '会话已超时',
          cardSubtitle: '由于长时间未进行任何操作，此会话已结束',
          requiredAuth: false
        }
      }
    ]
  }
]
