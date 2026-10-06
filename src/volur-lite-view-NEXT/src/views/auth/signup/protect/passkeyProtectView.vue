<template>
  <div class="flex flex-col justify-between h-full">
    <div class="flex-1 flex flex-col justify-center">
      <div v-if="!passkeyRegistered" class="flex flex-col items-center text-center py-4">
        <div class="w-16 h-16 rounded-full bg-google-blue/10 dark:bg-google-blue-dark/15 flex items-center justify-center text-google-blue dark:text-google-blue-dark mb-4">
          <svg class="w-8 h-8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="7.5" cy="15.5" r="3.5" />
            <path d="M10 13l9-9" />
            <path d="M16 4l3 3" />
            <path d="M14 6l2 2" />
          </svg>
        </div>
        <h2 class="text-base font-medium text-google-text-primary dark:text-google-text-darkPrimary mb-2">
          {{ t('auth.passkey_protect_title') }}
        </h2>
        <p class="text-sm text-google-text-secondary dark:text-google-text-darkSecondary max-w-sm leading-relaxed">
          {{ t('auth.passkey_protect_desc') }}
        </p>
      </div>

      <div v-else class="flex flex-col items-center text-center py-4">
        <div class="w-16 h-16 rounded-full bg-green-500/10 dark:bg-green-500/20 flex items-center justify-center text-green-600 dark:text-green-400 mb-4">
          <svg class="w-8 h-8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="20 6 9 17 4 12" />
          </svg>
        </div>
        <h2 class="text-base font-medium text-google-text-primary dark:text-google-text-darkPrimary mb-2">
          {{ t('auth.passkey_registered_title') }}
        </h2>
        <p class="text-sm text-google-text-secondary dark:text-google-text-darkSecondary max-w-sm leading-relaxed">
          {{ t('auth.passkey_registered_desc') }}
        </p>
      </div>
    </div>

    <div class="flex items-center justify-end gap-2.5 pt-6 sm:pt-8">
      <button
        type="button"
        class="auth-action-btn btn-secondary-action"
        :disabled="loading || isRegistering"
        :isScrim="true"
        @click="handleBackOptions"
      >
        {{ passkeyRegistered ? t('auth.back_options') : t('auth.later') }}
      </button>
      <n-button
        v-if="!passkeyRegistered"
        type="primary"
        class="auth-action-btn btn-primary-action"
        :disabled="loading || isRegistering"
        :loading="isRegistering"
        :isScrim="true"
        @click="handleCreatePasskey"
      >
        {{ t('auth.create_passkey') }}
      </n-button>
      <n-button
        v-else
        type="primary"
        class="auth-action-btn btn-primary-action"
        :disabled="loading"
        :isScrim="true"
        @click="handleFinish"
      >
        {{ t('common.next') }}
      </n-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NButton } from 'naive-ui'
import { useI18n } from 'vue-i18n'
import { useSignupFlowStore } from '@/store/modules/flow'
import { fetchPasskeyCreationOptionsApi, submitPasskeyRegisterApi } from '@/api/auth'
import type { PasskeyRegisterRequestDTO, PasskeyCreationOptionsVO } from '@/types/auth/flow'
import { useCardHeader } from '@/composables/useCardHeader'

defineOptions({
  name: 'PasskeyProtectView'
})

const { t } = useI18n()
useCardHeader(() => t('auth.passkey_method_name'), () => t('auth.passkey_protect_desc'))

const router = useRouter()
const signupFlowStore = useSignupFlowStore()
const loading = computed(() => signupFlowStore.loading)

const isRegistering = ref(false)
const passkeyRegistered = ref(false)

let prefetchPromise: Promise<PasskeyCreationOptionsVO | null> | null = null

function prefetchCreationOptions() {
  if (prefetchPromise || passkeyRegistered.value) return
  if (!signupFlowStore.dsh || !signupFlowStore.tl) return
  prefetchPromise = fetchPasskeyCreationOptionsApi({
    dsh: signupFlowStore.dsh,
    tl: signupFlowStore.tl
  })
    .then((res) => res.data || null)
    .catch(() => null)
}

onMounted(() => {
  prefetchCreationOptions()
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

async function handleCreatePasskey() {
  if (isRegistering.value) return
  if (typeof window === 'undefined' || !window.PublicKeyCredential || !navigator.credentials?.create) {
    window.$message?.warning?.(t('auth.passkey_not_supported'))
    return
  }

  const hostname = window.location.hostname
  const isIp = /^(\d{1,3}\.){3}\d{1,3}$/.test(hostname) || hostname.includes(':')
  if (isIp) {
    window.$message?.error?.(t('auth.passkey_ip_not_supported'))
    return
  }

  isRegistering.value = true
  try {
    await signupFlowStore.withLoading(async () => {
      let optionsData: PasskeyCreationOptionsVO | null = null
      if (prefetchPromise) {
        optionsData = await prefetchPromise
      }
      if (!optionsData || !optionsData.challenge) {
        const res = await fetchPasskeyCreationOptionsApi({
          dsh: signupFlowStore.dsh,
          tl: signupFlowStore.tl
        })
        optionsData = res.data
      }
      if (!optionsData || !optionsData.challenge) {
        throw new Error('获取通行密钥配置失败')
      }

      const rawUserId = optionsData.user?.id || optionsData.user?.name || signupFlowStore.username || 'volur_user'
      const userIdBuffer = base64UrlToBuffer(rawUserId)

      const normalizedPubKeyCredParams: PublicKeyCredentialParameters[] = optionsData.pubKeyCredParams?.length
        ? optionsData.pubKeyCredParams
            .filter((p: any) => p && typeof p.alg === 'number')
            .map((p: any) => ({
              type: 'public-key' as const,
              alg: p.alg
            }))
        : [
            { type: 'public-key' as const, alg: -7 },
            { type: 'public-key' as const, alg: -257 }
          ]

      const rp: PublicKeyCredentialRpEntity = {
        name: optionsData.rp?.name || 'Volur Lite'
      }
      if (hostname && !isIp) {
        rp.id = hostname
      } else if (optionsData.rp?.id) {
        rp.id = optionsData.rp.id
      }

      const authenticatorSelection: AuthenticatorSelectionCriteria = {
        residentKey: 'required',
        userVerification: 'preferred'
      }
      if (optionsData.authenticatorSelection?.authenticatorAttachment) {
        const att = String(optionsData.authenticatorSelection.authenticatorAttachment).toLowerCase()
        if (att === 'platform' || att === 'cross-platform') {
          authenticatorSelection.authenticatorAttachment = att as AuthenticatorAttachment
        }
      }

      const creationOptions: CredentialCreationOptions = {
        publicKey: {
          rp,
          user: {
            name: optionsData.user?.name || signupFlowStore.username || 'volur_user',
            displayName: optionsData.user?.displayName || optionsData.user?.name || signupFlowStore.username || 'Volur User',
            id: userIdBuffer.byteLength > 0
              ? userIdBuffer
              : copyToPureArrayBuffer(new TextEncoder().encode(signupFlowStore.username || 'volur_user'))
          },
          challenge: base64UrlToBuffer(optionsData.challenge),
          pubKeyCredParams: normalizedPubKeyCredParams.length > 0 ? normalizedPubKeyCredParams : [
            { type: 'public-key' as const, alg: -7 },
            { type: 'public-key' as const, alg: -257 }
          ],
          authenticatorSelection,
          timeout: 60000,
          attestation: 'none'
        }
      }

      const credential = (await navigator.credentials.create(creationOptions)) as PublicKeyCredential | null
      if (!credential) {
        throw new Error('创建通行密钥凭据失败')
      }

      const attestationResponse = credential.response as AuthenticatorAttestationResponse
      const transports = typeof attestationResponse.getTransports === 'function' ? attestationResponse.getTransports() : []

      const registerReq: PasskeyRegisterRequestDTO = {
        label: navigator.userAgent.includes('Windows')
          ? 'Windows Hello'
          : navigator.userAgent.includes('Mac')
            ? 'Apple Touch ID / Passkey'
            : 'Passkey',
        credential: {
          id: credential.id,
          rawId: bufferToBase64Url(credential.rawId),
          response: {
            attestationObject: bufferToBase64Url(attestationResponse.attestationObject),
            clientDataJSON: bufferToBase64Url(attestationResponse.clientDataJSON),
            transports
          },
          type: credential.type,
          clientExtensionResults: credential.getClientExtensionResults ? credential.getClientExtensionResults() : {}
        }
      }

      const regRes = await submitPasskeyRegisterApi(
        {
          dsh: signupFlowStore.dsh,
          tl: signupFlowStore.tl
        },
        registerReq
      )

      if (regRes.data?.success) {
        passkeyRegistered.value = true
        window.$message?.success?.(t('auth.passkey_registered_title'))
      } else {
        throw new Error(regRes.message || '通行密钥注册未通过')
      }
    })
  } catch (err: any) {
    if (err.name === 'NotAllowedError') {
      window.$message?.info?.(t('auth.passkey_cancelled'))
    } else {
      window.$message?.error?.(err?.message || t('auth.passkey_verify_failed'))
    }
  } finally {
    isRegistering.value = false
  }
}

async function handleBackOptions() {
  if (signupFlowStore.loading || isRegistering.value) return
  await signupFlowStore.triggerScrim(undefined, 1000)
  await router.push({
    path: '/v1/signup/steps/pt/options',
    query: signupFlowStore.getQueryParams()
  })
}

async function handleFinish() {
  if (signupFlowStore.loading || isRegistering.value) return
  const redirect = signupFlowStore.continueUrl || '/competitions'
  await signupFlowStore.triggerScrim(undefined, 1000)
  signupFlowStore.resetFlow()
  await router.push(redirect)
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

.btn-secondary-action {
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

:global(html.dark) .btn-secondary-action {
  color: #8ab4f8;

  &:hover {
    background-color: rgba(138, 180, 248, 0.12) !important;
    color: #8ab4f8 !important;
  }

  &:active {
    background-color: rgba(138, 180, 248, 0.2) !important;
  }
}

.btn-primary-action {
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

:global(html.dark) .btn-primary-action {
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
