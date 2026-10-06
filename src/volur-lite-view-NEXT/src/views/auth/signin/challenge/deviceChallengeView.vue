<template>
  <form class="space-y-6 flex flex-col justify-between h-full" @submit.prevent="handleVerifyChallenge">
    <div class="space-y-4">
      <div class="auth-challenge-banner flex items-start gap-3 p-3.5 rounded-lg border border-google-border-light dark:border-google-border-dark bg-google-surface-light dark:bg-google-surface-darkElevated">
        <svg class="w-5 h-5 text-google-blue dark:text-google-blue-dark shrink-0 mt-0.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="7.5" cy="15.5" r="3.5" />
          <path d="M10 13l9-9" />
          <path d="M16 4l3 3" />
          <path d="M14 6l2 2" />
        </svg>
        <div class="text-xs text-google-text-desc dark:text-google-text-darkDesc leading-relaxed">
          <p class="font-medium text-google-text-primary dark:text-google-text-darkPrimary mb-0.5">
            {{ t('auth.passkey_verify_title') }}
          </p>
          {{ t('auth.passkey_verify_desc') }}
        </div>
      </div>

      <div class="p-4 rounded-lg border border-google-border-light dark:border-google-border-dark space-y-2 text-xs">
        <div class="flex justify-between">
          <span class="text-google-text-muted">{{ t('auth.login_account') }}</span>
          <span class="font-medium text-google-text-primary dark:text-google-text-darkPrimary">{{ signinFlowStore.identifier }}</span>
        </div>
        <div class="flex justify-between">
          <span class="text-google-text-muted">{{ t('auth.attempt_time') }}</span>
          <span class="font-medium text-google-text-primary dark:text-google-text-darkPrimary">{{ currentTimeFormatted }}</span>
        </div>
        <div class="flex justify-between">
          <span class="text-google-text-muted">{{ t('auth.verify_method') }}</span>
          <span class="font-medium text-google-text-primary dark:text-google-text-darkPrimary">{{ t('auth.passkey_method_name') }}</span>
        </div>
      </div>

      <div v-if="deviceError" class="flex items-center gap-1.5 text-xs text-google-red dark:text-google-red-dark">
        <svg viewBox="0 0 24 24" class="w-3.5 h-3.5 fill-current shrink-0">
          <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/>
        </svg>
        <span>{{ deviceError }}</span>
      </div>
    </div>

    <div class="flex items-center justify-end gap-2.5 pt-6 sm:pt-8">
      <button
        type="button"
        class="auth-action-btn btn-try-another-way"
        :isScrim="true"
        @click="handleTryAnotherWayClick"
      >
        {{ t('auth.try_another_way') }}
      </button>
      <n-button
        type="primary"
        class="auth-action-btn btn-verify-action"
        :loading="isSubmitting"
        :disabled="loading || isSubmitting"
        @click="handleVerifyChallenge"
      >
        {{ t('common.verify') }}
      </n-button>
    </div>
  </form>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { NButton } from 'naive-ui'
import { useI18n } from 'vue-i18n'
import { useSigninFlowStore } from '@/store/modules/flow'
import { fetchPasskeyAssertionOptionsApi } from '@/api/auth'
import { useCardHeader } from '@/composables/useCardHeader'

defineOptions({
  name: 'DeviceChallengeView'
})

const { t } = useI18n()
useCardHeader(() => t('auth.passkey_verify_title'), () => t('auth.passkey_verify_desc'))

const router = useRouter()
const signinFlowStore = useSigninFlowStore()

const deviceErrorKey = ref('')
const rawDeviceError = ref('')

const deviceError = computed(() => {
  if (deviceErrorKey.value) {
    return t(deviceErrorKey.value)
  }
  return rawDeviceError.value
})

function clearDeviceError(): void {
  deviceErrorKey.value = ''
  rawDeviceError.value = ''
}

const isSubmitting = ref(false)
const loading = computed(() => signinFlowStore.loading)

const currentTimeFormatted = computed(() => {
  return new Date().toLocaleTimeString('zh-CN', { hour12: false })
})

function copyToPureArrayBuffer(bytes: Uint8Array): ArrayBuffer {
  const buffer = new ArrayBuffer(bytes.byteLength)
  new Uint8Array(buffer).set(bytes)
  return buffer
}

function base64UrlToBuffer(base64Url: unknown): ArrayBuffer {
  if (!base64Url) {
    return new ArrayBuffer(0)
  }
  if (base64Url instanceof ArrayBuffer) {
    return base64Url
  }
  if (ArrayBuffer.isView(base64Url)) {
    return copyToPureArrayBuffer(new Uint8Array(base64Url.buffer, base64Url.byteOffset, base64Url.byteLength))
  }
  if (Array.isArray(base64Url)) {
    return copyToPureArrayBuffer(new Uint8Array(base64Url))
  }
  let str = ''
  if (typeof base64Url === 'string') {
    str = base64Url
  } else if (typeof base64Url === 'object') {
    const rec = base64Url as Record<string, unknown>
    const candidate = rec.value || rec.bytes || rec.id
    if (typeof candidate === 'string') {
      str = candidate
    } else if (Array.isArray(candidate)) {
      return copyToPureArrayBuffer(new Uint8Array(candidate))
    }
  }
  if (!str) {
    return new ArrayBuffer(0)
  }
  const base64 = str.replace(/-/g, '+').replace(/_/g, '/')
  const padLen = (4 - (base64.length % 4)) % 4
  const padded = base64.padEnd(base64.length + padLen, '=')
  const binary = atob(padded)
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i)
  }
  return copyToPureArrayBuffer(bytes)
}

function bufferToBase64Url(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer)
  let binary = ''
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i])
  }
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

async function handleTryAnotherWayClick() {
  if (signinFlowStore.loading || isSubmitting.value) return
  await signinFlowStore.triggerScrim(undefined, 1000)
  await router.push({
    path: '/v1/signin/challenge/selection',
    query: signinFlowStore.getQueryParams({
      from: 'pk',
      page: '1'
    })
  })
}

async function handleVerifyChallenge() {
  if (signinFlowStore.loading || isSubmitting.value) return
  clearDeviceError()

  if (typeof window === 'undefined' || !window.PublicKeyCredential || !navigator.credentials?.get) {
    deviceErrorKey.value = 'auth.passkey_not_supported'
    return
  }

  const hostname = window.location.hostname
  const isIp = /^(\d{1,3}\.){3}\d{1,3}$/.test(hostname) || hostname.includes(':')
  if (isIp) {
    deviceErrorKey.value = 'auth.passkey_ip_not_supported'
    return
  }

  isSubmitting.value = true
  try {
    const optRes = await fetchPasskeyAssertionOptionsApi({
      dsh: signinFlowStore.dsh,
      tl: signinFlowStore.tl
    })
    const optionsData = optRes.data
    if (!optionsData || !optionsData.challenge) {
      throw new Error('获取通行密钥认证选项失败')
    }

    const allowCredentials: PublicKeyCredentialDescriptor[] | undefined = Array.isArray(optionsData.allowCredentials)
      ? optionsData.allowCredentials.map((c: any) => ({
          id: base64UrlToBuffer(c.id),
          type: (c.type || 'public-key') as PublicKeyCredentialType,
          transports: c.transports
        }))
      : undefined

    const requestOptions: CredentialRequestOptions = {
      publicKey: {
        challenge: base64UrlToBuffer(optionsData.challenge),
        rpId: hostname && !isIp ? hostname : (optionsData.rpId || window.location.hostname),
        userVerification: optionsData.userVerification || 'preferred',
        timeout: 60000,
        ...(allowCredentials && allowCredentials.length > 0 ? { allowCredentials } : {})
      }
    }

    const credential = (await navigator.credentials.get(requestOptions)) as PublicKeyCredential | null
    if (!credential) {
      throw new Error('获取通行密钥断言失败')
    }

    const assertionResponse = credential.response as AuthenticatorAssertionResponse
    const credentialPayload = {
      id: credential.id,
      rawId: bufferToBase64Url(credential.rawId),
      response: {
        authenticatorData: bufferToBase64Url(assertionResponse.authenticatorData),
        clientDataJSON: bufferToBase64Url(assertionResponse.clientDataJSON),
        signature: bufferToBase64Url(assertionResponse.signature),
        userHandle: assertionResponse.userHandle ? bufferToBase64Url(assertionResponse.userHandle) : undefined
      },
      type: credential.type,
      clientExtensionResults: credential.getClientExtensionResults ? credential.getClientExtensionResults() : {}
    }

    const res = await signinFlowStore.submitChallenge('pk', {
      credential: credentialPayload,
      rememberMe: true
    })

    if (res.success) {
      await router.push(res.redirect || '/competitions')
      return
    }

    if (res.nextStep) {
      await router.push({
        path: `/v1/signin/challenge/${res.nextStep}`,
        query: signinFlowStore.getQueryParams()
      })
      return
    }

    if (res.errorMessage) {
      rawDeviceError.value = res.errorMessage
    }
  } catch (err: any) {
    if (err.name === 'NotAllowedError') {
      deviceErrorKey.value = 'auth.passkey_cancelled'
    } else {
      if (err?.message) {
        rawDeviceError.value = err.message
      } else {
        deviceErrorKey.value = 'auth.passkey_verify_failed'
      }
    }
  } finally {
    isSubmitting.value = false
  }
}
</script>

<style scoped lang="scss">
.auth-action-btn {
  height: 36px !important;
  min-width: 88px !important;
  padding: 0 16px !important;
  border-radius: 9999px !important;
  font-size: 14px !important;
  font-weight: 500 !important;
  box-sizing: border-box;
}

.btn-try-another-way {
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

  &:hover {
    background-color: rgba(11, 87, 208, 0.08) !important;
    color: #0b57d0 !important;
  }

  &:active {
    background-color: rgba(11, 87, 208, 0.16) !important;
  }
}

:global(html.dark) .btn-try-another-way {
  color: #8ab4f8;

  &:hover {
    background-color: rgba(138, 180, 248, 0.12) !important;
    color: #8ab4f8 !important;
  }

  &:active {
    background-color: rgba(138, 180, 248, 0.2) !important;
  }
}

.btn-verify-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background-color: #0b57d0 !important;
  color: #ffffff !important;
  border: none !important;
  transition: all 0.15s cubic-bezier(0.4, 0, 0.2, 1);
  outline: none;
  user-select: none;

  :deep(.n-button__content),
  .n-button__content {
    color: #ffffff !important;
  }

  :deep(.n-button__state-border) {
    border: none !important;
  }

  &:hover {
    background-color: #0842a0 !important;
    color: #ffffff !important;
    box-shadow: 0 1px 2px 0 rgba(60, 64, 67, 0.3), 0 1px 3px 1px rgba(60, 64, 67, 0.15);

    :deep(.n-button__content),
    .n-button__content {
      color: #ffffff !important;
    }
  }

  &:active {
    background-color: #063980 !important;
    color: #ffffff !important;
    transform: scale(0.98);
    box-shadow: 0 1px 2px 0 rgba(60, 64, 67, 0.3);

    :deep(.n-button__content),
    .n-button__content {
      color: #ffffff !important;
    }
  }

  &:focus,
  &:focus-visible {
    background-color: #0b57d0 !important;
    color: #ffffff !important;

    :deep(.n-button__content),
    .n-button__content {
      color: #ffffff !important;
    }
  }
}

:global(html.dark) .btn-verify-action {
  background-color: #8ab4f8 !important;
  color: #202124 !important;

  :deep(.n-button__content),
  .n-button__content {
    color: #202124 !important;
  }

  &:hover {
    background-color: #aecbfa !important;
    color: #202124 !important;
  }

  &:active {
    background-color: #669df6 !important;
    color: #202124 !important;
    transform: scale(0.98);

    :deep(.n-button__content),
    .n-button__content {
      color: #202124 !important;
    }
  }
}
</style>
