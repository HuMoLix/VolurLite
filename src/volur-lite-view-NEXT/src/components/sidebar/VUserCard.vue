<template>
  <VCard
    class="v-user-card"
    :theme="currentTheme"
    :width="280"
    :gap="20"
  >
    <div class="card-top-layer">
      <div class="card-top-spacer" />
      <div class="card-logo-container">
        <img
          v-if="effectiveLogoSrc"
          :src="effectiveLogoSrc"
          :alt="logoAlt"
          class="card-brand-logo"
        />
      </div>
      <VTooltip placement="bottom" :content="t('common.close', '关闭')">
        <button
          type="button"
          class="card-close-btn"
          :aria-label="t('common.close', '关闭')"
          @click="handleClose"
        >
          <n-icon :size="16">
            <CloseOutline />
          </n-icon>
        </button>
      </VTooltip>
    </div>

    <div class="card-middle-layer">
      <div class="card-user-info-row">
        <div class="card-avatar-wrap">
          <VAvatar
            :src="effectiveAvatarUrl"
            :text="effectiveIsLoggedIn ? effectiveUserInitial : ''"
            :size="42"
            :shape="avatarShape"
            :radius="avatarRadius"
            :ring="avatarRing"
            :theme="currentTheme"
          />
        </div>
        <div class="card-user-meta">
          <div class="card-user-name">
            <n-ellipsis :tooltip="{ placement: 'top' }">
              {{ effectiveIsLoggedIn ? effectiveDisplayName : t('common.signin', '登录') }}
            </n-ellipsis>
          </div>
          <div v-if="effectiveIsLoggedIn && effectiveUserEmail" class="card-user-email">
            <n-ellipsis :tooltip="{ placement: 'top' }">
              {{ effectiveUserEmail }}
            </n-ellipsis>
          </div>
        </div>
      </div>

      <div class="card-action-buttons">
        <button
          type="button"
          class="card-btn card-btn--switch"
          @click="handleSwitchAccount"
        >
          <span>{{ t('common.switch_account', '切换账号') }}</span>
        </button>

        <button
          type="button"
          class="card-btn card-btn--logout"
          @click="handleLogout"
        >
          <span>{{ t('common.signout', '退出账号') }}</span>
        </button>

        <button
          type="button"
          class="card-btn card-btn--profile"
          @click="handleGoProfile"
        >
          <span>{{ t('common.profile_info', '个人信息') }}</span>
        </button>
      </div>
    </div>

    <div class="card-bottom-layer">
      <a
        :href="privacyRoute"
        class="card-legal-link"
        @click.prevent="handlePrivacyClick"
      >
        {{ t('common.privacy_policy', '隐私权政策') }}
      </a>
      <span class="card-legal-dot">•</span>
      <a
        :href="termsRoute"
        class="card-legal-link"
        @click.prevent="handleTermsClick"
      >
        {{ t('common.terms_of_service', '服务条款') }}
      </a>
    </div>
  </VCard>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { NIcon, NEllipsis } from 'naive-ui'
import { CloseOutline } from '@vicons/ionicons5'
import { useAppStore } from '@/store/modules/app'
import { useUserStore } from '@/store/modules/user'
import { logoutApi } from '@/api/auth'
import { removeCookie } from '@/utils/cookie'
import VTooltip from '@/components/common/VTooltip.vue'
import VCard from './VCard.vue'
import VAvatar from './VAvatar.vue'
import type { VUserCardProps } from './types'

defineOptions({
  name: 'VUserCard'
})

const props = withDefaults(defineProps<VUserCardProps>(), {
  theme: undefined,
  user: undefined,
  isLoggedIn: undefined,
  displayName: '',
  userEmail: '',
  userInitial: '',
  avatarUrl: '',
  avatarShape: 'square',
  avatarRadius: undefined,
  avatarRing: false,
  logoSrc: '',
  logoAlt: 'Volur',
  accountChooserRoute: '/v1/signin/accountchooser',
  userProfileRoute: '/user/profile',
  signinRoute: '/v1/signin/identifier',
  privacyRoute: '/privacy',
  termsRoute: '/terms'
})

const emit = defineEmits<{
  close: []
  'switch-account': [ev: MouseEvent]
  logout: [ev: MouseEvent]
  'profile-click': [ev: MouseEvent]
}>()

const router = useRouter()
const { t } = useI18n()
const appStore = useAppStore()
const userStore = useUserStore()

const currentTheme = computed<'light' | 'dark'>(() => {
  return props.theme || (appStore.theme === 'dark' ? 'dark' : 'light')
})

const effectiveIsLoggedIn = computed<boolean>(() => {
  if (typeof props.isLoggedIn === 'boolean') {
    return props.isLoggedIn
  }
  return userStore.isLoggedIn
})

const currentUser = computed(() => {
  return props.user || userStore.user
})

const effectiveDisplayName = computed<string>(() => {
  if (!effectiveIsLoggedIn.value) return ''
  if (props.displayName) {
    return props.displayName
  }
  return currentUser.value?.nickname || currentUser.value?.username || ''
})

const effectiveUserEmail = computed<string>(() => {
  if (!effectiveIsLoggedIn.value) return ''
  if (props.userEmail) {
    return props.userEmail
  }
  return currentUser.value?.email || ''
})

const effectiveUserInitial = computed<string>(() => {
  if (!effectiveIsLoggedIn.value) return ''
  if (props.userInitial) {
    return props.userInitial
  }
  const name = effectiveDisplayName.value || currentUser.value?.username || ''
  return name ? name.charAt(0).toUpperCase() : ''
})

const effectiveAvatarUrl = computed<string>(() => {
  if (!effectiveIsLoggedIn.value) return ''
  if (props.avatarUrl) {
    return props.avatarUrl
  }
  return currentUser.value?.avatar || currentUser.value?.avatarUrl || ''
})

const effectiveLogoSrc = computed<string>(() => {
  return props.logoSrc || ''
})

function handleClose() {
  emit('close')
}

function handleSwitchAccount(ev: MouseEvent) {
  emit('switch-account', ev)
  emit('close')
  if (props.accountChooserRoute) {
    router.push(props.accountChooserRoute).catch(() => {})
  }
}

async function handleLogout(ev: MouseEvent) {
  emit('logout', ev)
  emit('close')
  try {
    await logoutApi()
  } catch {
  } finally {
    removeCookie('logged_in')
    userStore.clearUser()
    if (props.signinRoute) {
      router.push(props.signinRoute).catch(() => {})
    }
  }
}

function handleGoProfile(ev: MouseEvent) {
  emit('profile-click', ev)
  emit('close')
  if (effectiveIsLoggedIn.value) {
    if (props.userProfileRoute) {
      router.push(props.userProfileRoute).catch(() => {})
    }
  } else {
    if (props.signinRoute) {
      router.push(props.signinRoute).catch(() => {})
    }
  }
}

function handlePrivacyClick() {
  emit('close')
  if (props.privacyRoute) {
    router.push(props.privacyRoute).catch(() => {})
  }
}

function handleTermsClick() {
  emit('close')
  if (props.termsRoute) {
    router.push(props.termsRoute).catch(() => {})
  }
}
</script>

<style scoped lang="scss">
.v-user-card {
  .card-top-layer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    height: 28px;
    position: relative;

    .card-top-spacer {
      width: 28px;
      height: 28px;
      flex-shrink: 0;
    }

    .card-logo-container {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;

      .card-brand-logo {
        height: 16px;
        width: auto;
        max-width: 140px;
        object-fit: contain;
      }
    }

    .card-close-btn {
      width: 28px;
      height: 28px;
      border-radius: 50%;
      border: none;
      background: transparent;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      color: #5f6368;
      transition: background-color 0.2s cubic-bezier(0.2, 0, 0, 1),
                  color 0.2s cubic-bezier(0.2, 0, 0, 1);
      outline: none;
      padding: 0;
      flex-shrink: 0;

      &:hover {
        background-color: rgba(60, 64, 67, 0.08);
        color: #202124;
      }

      &:active {
        background-color: rgba(60, 64, 67, 0.12);
      }
    }
  }

  .card-middle-layer {
    display: flex;
    flex-direction: column;
    gap: 12px;
    width: 100%;
    margin-bottom: 40px;

    .card-user-info-row {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 2px 0;

      .card-avatar-wrap {
        flex-shrink: 0;
      }

      .card-user-meta {
        flex: 1;
        min-width: 0;
        display: flex;
        flex-direction: column;
        gap: 2px;

        .card-user-name {
          font-size: 14px;
          font-weight: 600;
          color: #202124;
          line-height: 1.3;
          width: 100%;
          min-width: 0;
        }

        .card-user-email {
          font-size: 12px;
          color: #5f6368;
          line-height: 1.3;
          width: 100%;
          min-width: 0;
        }
      }
    }

    .card-action-buttons {
      display: flex;
      flex-direction: column;
      gap: 8px;
      width: 100%;

      .card-btn {
        width: 100%;
        height: 36px;
        border-radius: 32px;
        border: 1px solid #d7d8da;
        background-color: #ffffff;
        color: #2b2d31;
        font-size: 12px;
        font-weight: 500;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        padding: 0 12px;
        cursor: pointer;
        outline: none;
        transition: background-color 0.2s cubic-bezier(0.2, 0, 0, 1),
                    border-color 0.2s ease,
                    color 0.2s cubic-bezier(0.2, 0, 0, 1);

        span {
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        &:hover {
          background-color: #f1f3f4;
          border-color: #dadce0;
          color: #202124;
        }

        &:active {
          background-color: #e8eaed;
        }
      }
    }
  }

  .card-bottom-layer {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    width: 100%;
    padding-top: 4px;

    .card-legal-link {
      font-size: 11px;
      color: #5f6368;
      text-decoration: none;
      cursor: pointer;
      transition: color 0.2s cubic-bezier(0.2, 0, 0, 1);

      &:hover {
        color: #1a73e8;
      }
    }

    .card-legal-dot {
      font-size: 5px;
      color: #bdc1c6;
      line-height: 1;
    }
  }

  &.dark,
  html.dark & {
    .card-top-layer {
      .card-logo-container .card-brand-logo {
        filter: brightness(0) invert(1);
      }

      .card-close-btn {
        color: #9aa0a6;

        &:hover {
          background-color: rgba(255, 255, 255, 0.08);
          color: #e8eaed;
        }

        &:active {
          background-color: rgba(255, 255, 255, 0.12);
        }
      }
    }

    .card-middle-layer {
      .card-user-info-row {
        .card-user-meta {
          .card-user-name {
            color: #e8eaed;
          }

          .card-user-email {
            color: #9aa0a6;
          }
        }
      }

      .card-action-buttons {
        .card-btn {
          background-color: #292a2d;
          border-color: #3c4043;
          color: #bdc1c6;

          &:hover {
            background-color: #303134;
            color: #e8eaed;
            border-color: #5f6368;
          }

          &:active {
            background-color: #3c4043;
          }
        }
      }
    }

    .card-bottom-layer {
      .card-legal-link {
        color: #9aa0a6;

        &:hover {
          color: #8ab4f8;
        }
      }

      .card-legal-dot {
        color: #5f6368;
      }
    }
  }
}
</style>
