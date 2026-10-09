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
          {{ t('auth.signup.passkey_protect_title') }}
        </h2>
        <p class="text-sm text-google-text-secondary dark:text-google-text-darkSecondary max-w-sm leading-relaxed">
          {{ t('auth.signup.passkey_protect_desc') }}
        </p>
      </div>

      <div v-else class="flex flex-col items-center text-center py-4">
        <div class="w-16 h-16 rounded-full bg-green-500/10 dark:bg-green-500/20 flex items-center justify-center text-green-600 dark:text-green-400 mb-4">
          <svg class="w-8 h-8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="20 6 9 17 4 12" />
          </svg>
        </div>
        <h2 class="text-base font-medium text-google-text-primary dark:text-google-text-darkPrimary mb-2">
          {{ t('auth.signup.passkey_registered_title') }}
        </h2>
        <p class="text-sm text-google-text-secondary dark:text-google-text-darkSecondary max-w-sm leading-relaxed">
          {{ t('auth.signup.passkey_registered_desc') }}
        </p>
      </div>
    </div>

    <AuthActionBar
      :primary-text="passkeyRegistered ? t('common.next') : t('auth.signup.create_passkey')"
      :secondary-text="passkeyRegistered ? t('auth.signup.back_options') : t('auth.later')"
      :primary-disabled="loading || isRegistering"
      :secondary-disabled="loading || isRegistering"
      :primary-loading="isRegistering"
      @primary="passkeyRegistered ? handleFinish() : handleCreatePasskey()"
      @secondary="handleBackOptions"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useSignupFlowStore } from '@/store/modules/flow'
import { fetchPasskeyCreationOptionsApi, submitPasskeyRegisterApi } from '@/api/auth'
import type { PasskeyRegisterRequestDTO, PasskeyCreationOptionsVO } from '@/types/auth/flow'
import { useCardHeader } from '@/composables/useCardHeader'
import { AuthActionBar } from '@/components/auth'

defineOptions({
  name: 'PasskeyProtectView'
})

const { t } = useI18n()
useCardHeader(() => t('auth.signin.passkey_method_name'), () => t('auth.signup.passkey_protect_desc'))

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
        window.$message?.success?.(t('auth.signup.passkey_registered_title'))
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
