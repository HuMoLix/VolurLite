<template>
  <div class="auth-card__body">
    <div class="auth-card__left">
      <h1 class="auth-card__title">{{ t('auth.choose_account') }}</h1>
      <p class="auth-card__subtitle">{{ t('auth.choose_account_subtitle') }}</p>
      <div id="auth-header-extra" class="auth-card__header-extra" />
    </div>

    <div class="auth-card__right">
      <div class="auth-form-content w-full h-full flex flex-col justify-between">
        <div class="flex flex-col">
          <div v-if="errorMsg" class="mb-4 text-sm text-google-red-light dark:text-google-red-dark">
            {{ errorMsg }}
          </div>

          <div class="auth-selection-list">
            <div
              v-for="acc in accountList"
              :key="acc.identifier"
              class="auth-selection-item"
              :class="{ 'is-disabled': loading || (acc.sessionStatus === 'DISABLED' && !isRemoveMode) }"
              role="button"
              tabindex="0"
              :isScrim="true"
              @click="handleSelect(acc)"
              @keydown.enter.space.prevent="handleSelect(acc)"
            >
              <div class="auth-selection-item__left">
                <div class="auth-selection-item__avatar">
                  <img
                    v-if="acc.avatarUrl"
                    :src="acc.avatarUrl"
                    alt=""
                  />
                  <span v-else>
                    {{ (acc.displayName || acc.username || acc.identifier).charAt(0) }}
                  </span>
                </div>
                <div class="auth-selection-item__content">
                  <div class="auth-selection-item__title">
                    {{ acc.displayName || acc.username || acc.identifier }}
                  </div>
                  <div
                    v-if="getAccountSubtitle(acc)"
                    class="auth-selection-item__subtitle"
                  >
                    {{ getAccountSubtitle(acc) }}
                  </div>
                </div>
              </div>

              <div class="auth-selection-item__right">
                <button
                  v-if="isRemoveMode"
                  type="button"
                  class="remove-icon-btn flex items-center justify-center w-8 h-8 rounded-full text-google-red-light dark:text-google-red-dark transition-colors"
                  :disabled="loading"
                  :isScrim="true"
                  @click.stop="handleRemove(acc)"
                >
                  <svg class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="3 6 5 6 21 6" />
                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                    <line x1="10" y1="11" x2="10" y2="17" />
                    <line x1="14" y1="11" x2="14" y2="17" />
                  </svg>
                </button>

                <template v-else>
                  <span
                    v-if="acc.sessionStatus === 'DISABLED'"
                    class="status-tag status--disabled text-xs font-medium px-2 py-0.5 rounded-full"
                  >
                    {{ t('auth.status_disabled') }}
                  </span>
                  <span
                    v-else-if="acc.sessionStatus !== 'ACTIVE'"
                    class="status-tag status--signed-out text-xs font-medium px-2 py-0.5 rounded-full"
                  >
                    {{ t('auth.status_signed_out') }}
                  </span>
                </template>
              </div>
            </div>

            <div
              v-if="!isRemoveMode"
              class="auth-selection-item auth-selection-item--add"
              :class="{ 'is-disabled': loading }"
              role="button"
              tabindex="0"
              :isScrim="true"
              @click="handleUseAnotherAccount"
              @keydown.enter.space.prevent="handleUseAnotherAccount"
            >
              <div class="auth-selection-item__left">
                <div class="auth-selection-item__icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                    <circle cx="8.5" cy="7" r="4" />
                    <line x1="20" y1="8" x2="20" y2="14" />
                    <line x1="23" y1="11" x2="17" y2="11" />
                  </svg>
                </div>
                <div class="auth-selection-item__content">
                  <div class="auth-selection-item__title">
                    {{ t('auth.use_another_account') }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="flex items-center justify-between pt-6 sm:pt-8">
          <div>
            <button
              v-if="!isRemoveMode && accountList.length > 0"
              type="button"
              class="auth-action-btn btn-remove-account"
              :disabled="loading"
              :isScrim="true"
              @click="handleEnterRemoveMode"
            >
              {{ t('auth.remove_account') }}
            </button>
          </div>

          <div class="flex items-center gap-2.5">
            <n-button
              v-if="isRemoveMode"
              type="primary"
              class="auth-action-btn btn-next-step"
              :disabled="loading"
              :isScrim="true"
              @click="handleExitRemoveMode"
            >
              {{ t('common.done') }}
            </n-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NButton } from 'naive-ui'
import { useI18n } from 'vue-i18n'
import { useSigninFlowStore } from '@/store/modules/flow'
import { probeAccountChooserApi, removeAccountChooserApi } from '@/api/auth'
import { getKnownAccounts, saveKnownAccount, removeKnownAccount } from '@/utils/accountChooser'
import type { AccountChooserItemVO } from '@/types/auth/flow'
import { useCardHeader } from '@/composables/useCardHeader'

defineOptions({
  name: 'AccountChooserView'
})

const { t } = useI18n()
useCardHeader(() => t('auth.choose_account'), () => t('auth.choose_account_subtitle'))

const route = useRoute()
const router = useRouter()
const signinFlowStore = useSigninFlowStore()

const loading = computed(() => signinFlowStore.loading)
const isRemoveMode = ref(false)
const errorKey = ref('')
const rawErrorMsg = ref('')

const errorMsg = computed(() => {
  if (errorKey.value) {
    return t(errorKey.value)
  }
  return rawErrorMsg.value
})

const accountList = ref<AccountChooserItemVO[]>([])

function getAccountSubtitle(acc: AccountChooserItemVO): string {
  if (acc.email) {
    return acc.email
  }
  const title = acc.displayName || acc.username || acc.identifier
  if (acc.username && acc.username !== title) {
    return acc.username
  }
  if (acc.identifier && acc.identifier !== title) {
    return acc.identifier
  }
  return ''
}

async function loadAndProbeAccounts(): Promise<void> {
  const cached = getKnownAccounts()
  if (cached.length > 0) {
    accountList.value = cached.map((c) => ({
      identifier: c.identifier,
      username: c.username,
      email: c.email,
      displayName: c.displayName || c.username,
      avatarUrl: c.avatarUrl,
      sessionStatus: 'SIGNED_OUT'
    }))
  }

  try {
    const probeRes = await probeAccountChooserApi({
      identifiers: cached.map((c) => c.identifier)
    })
    if (probeRes.data?.accounts && probeRes.data.accounts.length > 0) {
      accountList.value = probeRes.data.accounts
      probeRes.data.accounts.forEach((acc) => {
        saveKnownAccount({
          identifier: acc.identifier,
          username: acc.username || acc.identifier,
          email: acc.email,
          displayName: acc.displayName,
          avatarUrl: acc.avatarUrl
        })
      })
    }
  } catch {
  }
}

onMounted(async () => {
  await signinFlowStore.ensureFlowParams(route, router)
  await loadAndProbeAccounts()
})

async function handleSelect(acc: AccountChooserItemVO): Promise<void> {
  if (loading.value) return
  if (isRemoveMode.value) {
    await handleRemove(acc)
    return
  }
  if (acc.sessionStatus === 'DISABLED') {
    errorKey.value = 'auth.account_disabled'
    rawErrorMsg.value = ''
    return
  }

  if (acc.sessionStatus === 'ACTIVE') {
    await signinFlowStore.triggerScrim(undefined, 1000)
    const continueTarget = signinFlowStore.continueUrl || '/competitions'
    signinFlowStore.resetFlow()
    await router.replace(continueTarget)
    return
  }

  try {
    const nextStep = await signinFlowStore.selectAccount(acc.identifier)
    await router.push({
      path: `/v1/signin/challenge/${nextStep}`,
      query: signinFlowStore.getQueryParams()
    })
  } catch (err: unknown) {
    if (err && typeof err === 'object' && 'message' in err && typeof (err as { message: unknown }).message === 'string') {
      rawErrorMsg.value = (err as { message: string }).message
      errorKey.value = ''
    } else {
      errorKey.value = 'auth.account_flow_failed'
      rawErrorMsg.value = ''
    }
  }
}

async function handleRemove(acc: AccountChooserItemVO): Promise<void> {
  if (loading.value) return
  await signinFlowStore.withLoading(async () => {
    try {
      await removeAccountChooserApi({ identifier: acc.identifier })
    } catch {
    }

    removeKnownAccount(acc.identifier)
    accountList.value = accountList.value.filter((a) => a.identifier !== acc.identifier)

    if (accountList.value.length === 0) {
      isRemoveMode.value = false
    }
  }, 1000)
}

async function handleEnterRemoveMode(): Promise<void> {
  if (loading.value) return
  await signinFlowStore.triggerScrim(undefined, 1000)
  isRemoveMode.value = true
}

async function handleExitRemoveMode(): Promise<void> {
  if (loading.value) return
  await signinFlowStore.triggerScrim(undefined, 1000)
  isRemoveMode.value = false
}

async function handleUseAnotherAccount(): Promise<void> {
  if (loading.value) return
  signinFlowStore.setIdentifier('')
  signinFlowStore.tl = ''
  signinFlowStore.cid = 0
  await signinFlowStore.triggerScrim(undefined, 1000)
  await router.push({
    path: '/v1/signin/identifier',
    query: signinFlowStore.getQueryParams()
  })
}
</script>

<style scoped lang="scss">
.remove-icon-btn {
  background-color: transparent;
  border: none;
  cursor: pointer;
  transition: background-color 0.2s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    background-color: rgba(234, 67, 53, 0.12);
  }

  &:active {
    background-color: rgba(234, 67, 53, 0.24);
  }

  &:disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }
}

:global(.dark) .remove-icon-btn {
  &:hover {
    background-color: rgba(242, 139, 130, 0.16);
  }

  &:active {
    background-color: rgba(242, 139, 130, 0.28);
  }

  &:disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }
}

.status-tag {
  &.status--disabled {
    background-color: rgba(234, 67, 53, 0.12);
    color: #d93025;
  }

  &.status--signed-out {
    background-color: transparent;
    border: 1px solid #dadce0;
    color: #5f6368;
  }
}

:global(html.dark) .status-tag,
html.dark .status-tag {
  &.status--disabled {
    background-color: rgba(242, 139, 130, 0.2);
    color: #f28b82;
  }

  &.status--signed-out {
    background-color: transparent;
    border: 1px solid #3c4043;
    color: #bdc1c6;
  }
}

.btn-remove-account {
  color: #0b57d0;
  background-color: transparent;
  border: none;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  outline: none;
  user-select: none;

  &:hover:not(:disabled) {
    background-color: rgba(11, 87, 208, 0.08) !important;
    color: #0b57d0 !important;
  }

  &:active:not(:disabled) {
    background-color: rgba(11, 87, 208, 0.16) !important;
    color: #0b57d0 !important;
  }

  &:disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }
}

:global(.dark) .btn-remove-account {
  color: #8ab4f8;

  &:hover:not(:disabled) {
    background-color: rgba(138, 180, 248, 0.12) !important;
    color: #8ab4f8 !important;
  }

  &:disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }
}

.auth-action-btn {
  height: 36px !important;
  min-width: 88px !important;
  padding: 0 16px !important;
  border-radius: 9999px !important;
  font-size: 14px !important;
  font-weight: 500 !important;
  box-sizing: border-box;
}

.btn-next-step {
  cursor: pointer;
  background-color: #0b57d0 !important;
  color: #ffffff !important;
  border: none !important;
  transition: all 0.15s cubic-bezier(0.4, 0, 0.2, 1);

  &:disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }

  :deep(.n-button__content),
  .n-button__content {
    color: #ffffff !important;
  }

  &:hover {
    background-color: #0842a0 !important;
  }
}

:global(html.dark) .btn-next-step,
html.dark .btn-next-step {
  background-color: #8ab4f8 !important;
  color: #202124 !important;

  &:disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }

  :deep(.n-button__content),
  .n-button__content {
    color: #202124 !important;
  }

  &:hover {
    background-color: #aecbfa !important;
  }
}
</style>
