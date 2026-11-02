<template>
  <slot
    :verify="verifyPasskey"
    :save="createPasskey"
    :create="createPasskey"
    :loading="loading"
    :error="errorMessage"
  />
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useSigninFlowStore } from '@/store/modules/flow'
import {
  fetchPasskeyAssertionOptionsApi,
  fetchPasskeyCreationOptionsApi,
  submitPasskeyRegisterApi
} from '@/api/auth'
import type { PasskeyRegisterRequestDTO } from '@/types/auth/flow'

defineOptions({
  name: 'Passkey'
})

interface PasskeyProps {
  dsh?: string
  tl?: string
  username?: string
}

const props = withDefaults(defineProps<PasskeyProps>(), {
  dsh: '',
  tl: '',
  username: ''
})

const emit = defineEmits<{
  (e: 'success', data: any): void
  (e: 'error', error: string): void
  (e: 'cancel'): void
}>()

const signinFlowStore = useSigninFlowStore()
const loading = ref(false)
const errorMessage = ref('')

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

function isSupported(): boolean {
  if (typeof window === 'undefined' || !window.PublicKeyCredential) {
    return false
  }
  const hostname = window.location.hostname
  const isIp = /^(\d{1,3}\.){3}\d{1,3}$/.test(hostname) || hostname.includes(':')
  if (isIp) {
    return false
  }
  return true
}

async function createPasskey(options?: {
  dsh?: string
  tl?: string
  username?: string
  attachment?: AuthenticatorAttachment
  label?: string
}): Promise<any> {
  if (loading.value) return
  errorMessage.value = ''

  if (typeof window === 'undefined' || !window.PublicKeyCredential || !navigator.credentials?.create) {
    const msg = 'auth.passkey_not_supported'
    errorMessage.value = msg
    emit('error', msg)
    throw new Error(msg)
  }

  const hostname = window.location.hostname
  const isIp = /^(\d{1,3}\.){3}\d{1,3}$/.test(hostname) || hostname.includes(':')
  if (isIp) {
    const msg = 'auth.passkey_ip_not_supported'
    errorMessage.value = msg
    emit('error', msg)
    throw new Error(msg)
  }

  const effectiveDsh = options?.dsh || props.dsh || signinFlowStore.dsh
  const effectiveTl = options?.tl || props.tl || signinFlowStore.tl

  loading.value = true
  try {
    const res = await fetchPasskeyCreationOptionsApi({
      dsh: effectiveDsh,
      tl: effectiveTl
    })
    const optionsData = res.data
    if (!optionsData || !optionsData.challenge) {
      throw new Error('获取通行密钥配置失败')
    }

    const rawUserId = optionsData.user?.id || optionsData.user?.name || options?.username || props.username || signinFlowStore.identifier || 'volur_user'
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
    if (options?.attachment) {
      authenticatorSelection.authenticatorAttachment = options.attachment
    } else if (optionsData.authenticatorSelection?.authenticatorAttachment) {
      const att = String(optionsData.authenticatorSelection.authenticatorAttachment).toLowerCase()
      if (att === 'platform' || att === 'cross-platform') {
        authenticatorSelection.authenticatorAttachment = att as AuthenticatorAttachment
      }
    }

    const creationPublicKey: Record<string, any> = {
      rp,
      user: {
        name: optionsData.user?.name || options?.username || props.username || signinFlowStore.identifier || 'volur_user',
        displayName: optionsData.user?.displayName || optionsData.user?.name || options?.username || props.username || signinFlowStore.identifier || 'Volur User',
        id: userIdBuffer.byteLength > 0
          ? userIdBuffer
          : copyToPureArrayBuffer(new TextEncoder().encode(options?.username || props.username || signinFlowStore.identifier || 'volur_user'))
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

    if (options?.attachment === 'cross-platform') {
      creationPublicKey.hints = ['security-key', 'hybrid']
    } else if (options?.attachment === 'platform') {
      creationPublicKey.hints = ['client-device']
    }

    const creationOptions: CredentialCreationOptions = {
      publicKey: creationPublicKey as PublicKeyCredentialCreationOptions
    }

    const credential = (await navigator.credentials.create(creationOptions)) as PublicKeyCredential | null
    if (!credential) {
      throw new Error('创建通行密钥凭据失败')
    }

    const attestationResponse = credential.response as AuthenticatorAttestationResponse
    const transports = typeof attestationResponse.getTransports === 'function' ? attestationResponse.getTransports() : []

    const label = options?.label || (
      options?.attachment === 'cross-platform'
        ? '安全密钥'
        : navigator.userAgent.includes('Windows')
          ? 'Windows Hello'
          : navigator.userAgent.includes('Mac')
            ? 'Apple Touch ID / Passkey'
            : 'Passkey'
    )

    const registerReq: PasskeyRegisterRequestDTO = {
      label,
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
        dsh: effectiveDsh,
        tl: effectiveTl
      },
      registerReq
    )

    if (regRes.data?.success) {
      emit('success', regRes.data)
      return regRes.data
    } else {
      throw new Error(regRes.message || '通行密钥注册未通过')
    }
  } catch (err: any) {
    if (err?.name === 'NotAllowedError') {
      errorMessage.value = 'auth.passkey_cancelled'
      emit('cancel')
      throw err
    } else {
      const msg = err?.message || 'auth.passkey_verify_failed'
      errorMessage.value = msg
      emit('error', msg)
      throw err
    }
  } finally {
    loading.value = false
  }
}

async function verifyPasskey(options?: {
  dsh?: string
  tl?: string
  attachment?: AuthenticatorAttachment
  rememberMe?: boolean
}): Promise<any> {
  if (loading.value) return
  errorMessage.value = ''

  if (typeof window === 'undefined' || !window.PublicKeyCredential || !navigator.credentials?.get) {
    const msg = 'auth.passkey_not_supported'
    errorMessage.value = msg
    emit('error', msg)
    throw new Error(msg)
  }

  const hostname = window.location.hostname
  const isIp = /^(\d{1,3}\.){3}\d{1,3}$/.test(hostname) || hostname.includes(':')
  if (isIp) {
    const msg = 'auth.passkey_ip_not_supported'
    errorMessage.value = msg
    emit('error', msg)
    throw new Error(msg)
  }

  const effectiveDsh = options?.dsh || props.dsh || signinFlowStore.dsh
  const effectiveTl = options?.tl || props.tl || signinFlowStore.tl

  loading.value = true
  try {
    const optRes = await fetchPasskeyAssertionOptionsApi({
      dsh: effectiveDsh,
      tl: effectiveTl
    })
    const optionsData = optRes.data
    if (!optionsData || !optionsData.challenge) {
      throw new Error('获取通行密钥认证选项失败')
    }

    const allowCredentials: PublicKeyCredentialDescriptor[] | undefined = Array.isArray(optionsData.allowCredentials)
      ? optionsData.allowCredentials.map((c: any) => {
          let transports = c.transports
          if (options?.attachment === 'cross-platform') {
            transports = ['usb', 'nfc', 'ble', 'hybrid']
          } else if (options?.attachment === 'platform') {
            transports = ['internal']
          }
          return {
            id: base64UrlToBuffer(c.id),
            type: (c.type || 'public-key') as PublicKeyCredentialType,
            ...(transports && transports.length > 0 ? { transports } : {})
          }
        })
      : undefined

    const publicKeyOptions: Record<string, any> = {
      challenge: base64UrlToBuffer(optionsData.challenge),
      rpId: hostname && !isIp ? hostname : (optionsData.rpId || window.location.hostname),
      userVerification: optionsData.userVerification || 'preferred',
      timeout: 60000,
      ...(allowCredentials && allowCredentials.length > 0 ? { allowCredentials } : {})
    }

    if (options?.attachment === 'cross-platform') {
      publicKeyOptions.hints = ['security-key', 'hybrid']
    } else if (options?.attachment === 'platform') {
      publicKeyOptions.hints = ['client-device']
    }

    const requestOptions: CredentialRequestOptions = {
      publicKey: publicKeyOptions as PublicKeyCredentialRequestOptions
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
      rememberMe: options?.rememberMe ?? true
    })

    if (res.success || res.nextStep) {
      emit('success', res)
      return res
    } else if (res.errorMessage) {
      errorMessage.value = res.errorMessage
      emit('error', res.errorMessage)
      throw new Error(res.errorMessage)
    }
    return res
  } catch (err: any) {
    if (err?.name === 'NotAllowedError') {
      errorMessage.value = 'auth.passkey_cancelled'
      emit('cancel')
      throw err
    } else {
      const msg = err?.message || 'auth.passkey_verify_failed'
      errorMessage.value = msg
      emit('error', msg)
      throw err
    }
  } finally {
    loading.value = false
  }
}

defineExpose({
  createPasskey,
  savePasskey: createPasskey,
  verifyPasskey,
  isSupported,
  loading,
  error: errorMessage
})
</script>
