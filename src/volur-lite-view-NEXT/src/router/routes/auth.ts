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
              title: '选择账号 - Volur',
              titleKey: 'auth.signin.meta_title_accountchooser',
              cardTitle: '选择账号',
              cardTitleKey: 'auth.signin.choose_account',
              cardSubtitle: '以继续访问 Volur',
              cardSubtitleKey: 'auth.signin.choose_account_subtitle',
              requiredAuth: false
            }
          },
          {
            path: 'identifier',
            name: 'V1SignInIdentifier',
            component: () => import('@/views/auth/signin/IdentifierView.vue'),
            meta: {
              title: '登录 - Volur',
              titleKey: 'auth.signin.meta_title_signin',
              cardTitle: '登录',
              cardTitleKey: 'auth.signin.title',
              cardSubtitle: '使用您的 Volur 账号',
              cardSubtitleKey: 'auth.signin.subtitle',
              requiredAuth: false
            }
          },
          {
            path: 'challenge/:type(.*)*',
            name: 'V1SignInChallenge',
            component: () => import('@/views/auth/signin/ChallengeView.vue'),
            meta: {
              title: '安全核验与挑战 - Volur',
              titleKey: 'auth.signin.meta_title_challenge',
              cardTitle: '安全核验与挑战',
              cardTitleKey: 'auth.signin.security_verification_and_challenge',
              cardSubtitle: '',
              cardSubtitleKey: '',
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
              title: '注册 - Volur',
              titleKey: 'auth.signup.meta_title',
              cardTitle: '创建您的 Volur 账号',
              cardTitleKey: 'auth.signup.title',
              cardSubtitle: '输入您的基本信息以开启网络安全演练之旅',
              cardSubtitleKey: 'auth.signup.subtitle',
              requiredAuth: false
            }
          }
        ]
      },
      {
        path: 'recovery',
        children: [
          {
            path: '',
            redirect: (to) => ({
              path: '/v1/recovery/username/selection',
              query: to.query
            })
          },
          {
            path: 'username/selection',
            name: 'V1RecoveryUsernameSelection',
            component: () => import('@/views/auth/recovery/username/SelectionView.vue'),
            meta: {
              title: '找回用户名 - Volur',
              titleKey: 'auth.recovery.meta_title_username',
              cardTitle: '找回用户名',
              cardTitleKey: 'auth.recovery.username_title',
              cardSubtitle: '选择一种验证方式以接收您的用户名',
              cardSubtitleKey: 'auth.recovery.username_subtitle',
              requiredAuth: false
            }
          },
          {
            path: 'username/email',
            name: 'V1RecoveryUsernameEmail',
            component: () => import('@/views/auth/recovery/username/EmailView.vue'),
            meta: {
              title: '找回用户名 - 电子邮箱 - Volur',
              titleKey: 'auth.recovery.meta_title_email',
              cardTitle: '找回用户名',
              cardTitleKey: 'auth.recovery.username_title',
              cardSubtitle: '输入您账号绑定的安全电子邮箱',
              cardSubtitleKey: 'auth.recovery.email_subtitle',
              requiredAuth: false
            }
          },
          {
            path: 'password/selection',
            name: 'V1RecoveryPasswordSelection',
            component: () => import('@/views/auth/recovery/password/SelectionView.vue'),
            meta: {
              title: '找回密码 - Volur',
              titleKey: 'auth.recovery.meta_title_password',
              cardTitle: '找回密码',
              cardTitleKey: 'auth.recovery.password_title',
              cardSubtitle: '选择一种验证方式以重置您的密码',
              cardSubtitleKey: 'auth.recovery.password_subtitle',
              requiredAuth: false
            }
          },
          {
            path: 'password/emailotp',
            name: 'V1RecoveryPasswordEmailOtp',
            component: () => import('@/views/auth/recovery/password/EmailOtpView.vue'),
            meta: {
              title: '找回密码 - 邮箱验证码 - Volur',
              titleKey: 'auth.recovery.meta_title_emailotp',
              cardTitle: '验证您的身份',
              cardTitleKey: 'auth.recovery.emailotp_title',
              cardSubtitle: '输入发送至安全邮箱的验证码',
              cardSubtitleKey: 'auth.recovery.emailotp_subtitle',
              requiredAuth: false
            }
          },
          {
            path: 'password/reset',
            name: 'V1RecoveryPasswordReset',
            component: () => import('@/views/auth/recovery/password/ResetView.vue'),
            meta: {
              title: '重置密码 - Volur',
              titleKey: 'auth.recovery.meta_title_reset',
              cardTitle: '设置新密码',
              cardTitleKey: 'auth.recovery.reset_title',
              cardSubtitle: '请输入并确认您的新密码',
              cardSubtitleKey: 'auth.recovery.reset_subtitle',
              requiredAuth: false
            }
          },
          {
            path: 'challenge/:type(.*)*',
            name: 'V1RecoveryChallenge',
            component: () => import('@/views/auth/recovery/ChallengeView.vue'),
            meta: {
              title: '安全核验 - Volur',
              titleKey: 'auth.recovery.meta_title_challenge',
              cardTitle: '安全核验',
              cardTitleKey: 'auth.security_verification',
              cardSubtitle: '',
              cardSubtitleKey: '',
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
          title: '会话已超时 - Volur',
          titleKey: 'auth.meta_title_sessionexpire',
          cardTitle: '会话已超时',
          cardTitleKey: 'auth.session_expired',
          cardSubtitle: '由于长时间未进行任何操作，此会话已结束',
          cardSubtitleKey: 'auth.session_expired_subtitle',
          requiredAuth: false
        }
      }
    ]
  }
]
