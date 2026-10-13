import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { RouteLocationNormalizedLoaded, Router } from 'vue-router'
import {
  initSessionApi,
  submitSignInIdentifierApi,
  resendEmailOTPChallengeApi,
  selectSignInChallengeApi,
  submitSignInAuthenticateApi,
  submitDynamicChallengeApi,
  selectAccountChooserApi
} from '@/api/auth'
import { collectDeviceParams, getDeviceFingerprint, initDeviceFingerprint } from '@/utils/fingerprint'
import { isDshExpired, buildSessionExpireLocation, buildSessionExpireHref } from '@/utils/flow'
import { encryptFlowCredential } from '@/utils/flow-crypto'
import { useUserStore } from '@/store/modules/user'
import { createFlowLoading } from './flowLoading'

const STORAGE_IDENTIFIER_KEY = 'volur_auth_identifier'
const STORAGE_ACCOUNT_ERROR_KEY = 'volur_auth_account_error'

export const useSigninFlowStore = defineStore('signinFlow', () => {
  const {
    loading,
    setLoading,
    startLoading,
    stopLoading,
    withLoading,
    triggerScrim
  } = createFlowLoading()

  const dsh = ref<string>('')
  const tl = ref<string>('')
  const cid = ref<number>(0)
  const continueUrl = ref<string>('/competitions')
  const flowName = ref<string>('VlifWebLogin')
  const flowEntry = ref<string>('WebServiceLogin')
  const followup = ref<string>('/v1/signin/identifier')
  const pstMsg = ref<string>('1')
  const identifier = ref<string>(
    (typeof window !== 'undefined' && window.sessionStorage?.getItem(STORAGE_IDENTIFIER_KEY)) || ''
  )
  const activeChallengeType = ref<string>('')
  const accountError = ref<string>(
    (typeof window !== 'undefined' && window.sessionStorage?.getItem(STORAGE_ACCOUNT_ERROR_KEY)) || ''
  )

  function setAccountError(val: string): void {
    accountError.value = val
    if (typeof window !== 'undefined' && window.sessionStorage) {
      if (val) {
        window.sessionStorage.setItem(STORAGE_ACCOUNT_ERROR_KEY, val)
      } else {
        window.sessionStorage.removeItem(STORAGE_ACCOUNT_ERROR_KEY)
      }
    }
  }

  function consumeAccountError(): string {
    let err = accountError.value
    accountError.value = ''
    if (!err && typeof window !== 'undefined' && window.sessionStorage) {
      err = window.sessionStorage.getItem(STORAGE_ACCOUNT_ERROR_KEY) || ''
    }
    if (typeof window !== 'undefined' && window.sessionStorage) {
      window.sessionStorage.removeItem(STORAGE_ACCOUNT_ERROR_KEY)
    }
    return err
  }

  async function ensureFlowParams(route: RouteLocationNormalizedLoaded, router: Router): Promise<void> {
    const query = route.query
    let changed = false
    const newQuery: Record<string, string> = { ...(query as Record<string, string>) }

    if (!newQuery.continue) {
      newQuery.continue = (query.redirect as string) || '/competitions'
      changed = true
    }
    continueUrl.value = newQuery.continue

    if (!newQuery.followup || newQuery.followup === '/') {
      newQuery.followup = '/v1/signin/identifier'
      changed = true
    }
    followup.value = newQuery.followup

    if (!newQuery.flowName) {
      newQuery.flowName = 'VlifWebLogin'
      changed = true
    }
    flowName.value = newQuery.flowName

    if (!newQuery.flowEntry) {
      newQuery.flowEntry = 'WebServiceLogin'
      changed = true
    }
    flowEntry.value = newQuery.flowEntry

    if (!newQuery.pstMsg) {
      newQuery.pstMsg = '1'
      changed = true
    }
    pstMsg.value = newQuery.pstMsg

    if (query.dsh && typeof query.dsh === 'string') {
      if (isDshExpired(query.dsh)) {
        resetFlow()
        router.replace(buildSessionExpireLocation(route))
        return
      }
      dsh.value = query.dsh
    } else if (!dsh.value) {
      try {
        await initDeviceFingerprint().catch(() => {})
        const deviceParams = collectDeviceParams()
        const fp = deviceParams.fingerprint || getDeviceFingerprint()
        const t = Date.now()
        const encryptedData = await encryptFlowCredential(
          JSON.stringify(deviceParams),
          newQuery.flowName,
          newQuery.flowEntry
        )
        const initRes = await initSessionApi({
          flowName: newQuery.flowName,
          flowEntry: newQuery.flowEntry,
          fp,
          t,
          data: encryptedData
        })
        if (initRes.data?.dsh) {
          dsh.value = initRes.data.dsh
          newQuery.dsh = dsh.value
          changed = true
        }
        if ((initRes.data as any)?.tl && !tl.value) {
          tl.value = (initRes.data as any).tl
          newQuery.TL = tl.value
          changed = true
        }
      } catch {
      }
    } else {
      if (isDshExpired(dsh.value)) {
        resetFlow()
        router.replace(buildSessionExpireLocation(route))
        return
      }
      newQuery.dsh = dsh.value
      changed = true
    }

    if (query.TL && typeof query.TL === 'string') {
      tl.value = query.TL
    }
    if (query.cid) {
      cid.value = parseInt(query.cid as string, 10) || 0
    }

    if (query.identifier && typeof query.identifier === 'string') {
      setIdentifier(query.identifier)
      delete newQuery.identifier
      changed = true
    }

    if (changed) {
      router.replace({
        path: route.path,
        query: newQuery
      })
    }
  }

  async function submitIdentifier(trimmedVal: string): Promise<string> {
    if (dsh.value && isDshExpired(dsh.value)) {
      resetFlow()
      if (typeof window !== 'undefined') {
        window.location.href = buildSessionExpireHref()
      }
      throw new Error('会话已超时，请重新开始')
    }
    return withLoading(async () => {
      const res = await submitSignInIdentifierApi({
        identifier: trimmedVal,
        dsh: dsh.value,
        cid: 0
      })

      const data = res.data
      setIdentifier(trimmedVal)

      if (data.dsh) {
        dsh.value = data.dsh
      }
      if (data.tl) {
        tl.value = data.tl
      }
      cid.value = typeof data.cid === 'number' ? data.cid : 1
      if (!data.nextStep) {
        throw new Error('服务端未返回下一步挑战流程')
      }
      activeChallengeType.value = data.nextStep

      return activeChallengeType.value
    })
  }

  async function selectAccount(chosenIdentifier: string): Promise<string> {
    if (dsh.value && isDshExpired(dsh.value)) {
      resetFlow()
      if (typeof window !== 'undefined') {
        window.location.href = buildSessionExpireHref()
      }
      throw new Error('会话已超时，请重新开始')
    }
    return withLoading(async () => {
      const res = await selectAccountChooserApi({
        dsh: dsh.value,
        identifier: chosenIdentifier
      })
      const data = res.data
      setIdentifier(chosenIdentifier)
      if (data.dsh) {
        dsh.value = data.dsh
      }
      if (data.tl) {
        tl.value = data.tl
      }
      cid.value = typeof data.cid === 'number' ? data.cid : 1
      if (!data.nextStep) {
        throw new Error('服务端未返回下一步挑战流程')
      }
      activeChallengeType.value = data.nextStep
      return activeChallengeType.value
    })
  }

  async function submitChallenge(
    challengeType: string,
    payload: {
      password?: string
      captchaCode?: string
      captchaTicket?: string
      verificationCode?: string
      rememberMe?: boolean
      credential?: any
    }
  ): Promise<{ success: boolean; nextStep?: string; redirect?: string; errorMessage?: string }> {
    if (dsh.value && isDshExpired(dsh.value)) {
      resetFlow()
      if (typeof window !== 'undefined') {
        window.location.href = buildSessionExpireHref()
      }
      throw new Error('会话已超时，请重新开始')
    }
    return withLoading(async () => {
      const targetStep = (challengeType || activeChallengeType.value || '').trim().toLowerCase()
      if (!targetStep) {
        throw new Error('未指定安全核验步骤')
      }

      let passwordVal = payload.password
      if (typeof passwordVal === 'string' && passwordVal) {
        passwordVal = await encryptFlowCredential(
          passwordVal,
          flowName.value || 'VlifWebLogin',
          flowEntry.value || 'WebServiceLogin',
          dsh.value
        )
      }

      const requestBody: Record<string, any> = {
        dsh: dsh.value,
        tl: tl.value,
        cid: cid.value,
        password: passwordVal,
        code: payload.verificationCode,
        recaptchaTicket: payload.captchaTicket || payload.captchaCode,
        credential: payload.credential,
        rememberMe: payload.rememberMe,
        ...payload
      }
      if (passwordVal !== undefined) {
        requestBody.password = passwordVal
      }

      const res = await submitDynamicChallengeApi(targetStep, requestBody)
      const data = res.data

      if (data.status === 'SUCCESS') {
        const rawIdentifier = identifier.value || data.identifier || data.username || ''
        const canonicalUsername = data.username || data.identifier || rawIdentifier
        const userStore = useUserStore()
        userStore.setAuthenticatedUser({
          identifier: rawIdentifier || canonicalUsername,
          username: canonicalUsername,
          email: data.email,
          displayName: data.displayName || canonicalUsername,
          avatarUrl: data.avatarUrl,
          lastLoginTime: Date.now()
        })
        await userStore.fetchCurrentUser(true).catch(() => {})
        const targetRedirect = continueUrl.value || '/competitions'
        resetFlow(true)
        return {
          success: true,
          redirect: targetRedirect
        }
      }

      if (data.tl) {
        tl.value = data.tl
      }
      if (typeof data.cid === 'number') {
        cid.value = data.cid
      }
      if (data.nextStep) {
        activeChallengeType.value = data.nextStep
      }

      if (data.nextStep === 'authenticate') {
        const authRes = await submitSignInAuthenticateApi({
          dsh: dsh.value,
          tl: tl.value,
          cid: cid.value,
          rememberMe: Boolean(payload.rememberMe)
        })
        if (authRes.data?.status === 'SUCCESS') {
          const authData = authRes.data
          const rawIdentifier = identifier.value || authData.identifier || authData.username || ''
          const canonicalUsername = authData.username || authData.identifier || rawIdentifier
          const userStore = useUserStore()
          userStore.setAuthenticatedUser({
            identifier: rawIdentifier || canonicalUsername,
            username: canonicalUsername,
            email: authData.email,
            displayName: authData.displayName || canonicalUsername,
            avatarUrl: authData.avatarUrl,
            lastLoginTime: Date.now()
          })
          await userStore.fetchCurrentUser(true).catch(() => {})
          const targetRedirect = continueUrl.value || '/competitions'
          resetFlow(true)
          return {
            success: true,
            redirect: targetRedirect
          }
        }
      }

      return {
        success: false,
        nextStep: data.nextStep,
        errorMessage: data.errorMessage
      }
    })
  }

  async function selectChallenge(targetChallengeType: string): Promise<string> {
    if (dsh.value && isDshExpired(dsh.value)) {
      resetFlow()
      if (typeof window !== 'undefined') {
        window.location.href = buildSessionExpireHref()
      }
      throw new Error('会话已超时，请重新开始')
    }
    return withLoading(async () => {
      const res = await selectSignInChallengeApi({
        dsh: dsh.value,
        tl: tl.value,
        cid: cid.value,
        targetChallengeType
      })
      const data = res.data
      if (data.tl) {
        tl.value = data.tl
      }
      if (typeof data.cid === 'number') {
        cid.value = data.cid
      }
      if (data.nextStep) {
        activeChallengeType.value = data.nextStep
      }
      return activeChallengeType.value
    })
  }

  async function submitAuthenticate(rememberMe: boolean = true): Promise<{ success: boolean; redirect?: string }> {
    if (dsh.value && isDshExpired(dsh.value)) {
      resetFlow()
      if (typeof window !== 'undefined') {
        window.location.href = buildSessionExpireHref()
      }
      throw new Error('会话已超时，请重新开始')
    }
    return withLoading(async () => {
      const res = await submitSignInAuthenticateApi({
        dsh: dsh.value,
        tl: tl.value,
        cid: cid.value,
        rememberMe
      })
      if (res.data?.status === 'SUCCESS') {
        const rawIdentifier = identifier.value || res.data.identifier || res.data.username || ''
        const canonicalUsername = res.data.username || res.data.identifier || rawIdentifier
        const userStore = useUserStore()
        userStore.setAuthenticatedUser({
          identifier: rawIdentifier || canonicalUsername,
          username: canonicalUsername,
          email: res.data.email,
          displayName: res.data.displayName || canonicalUsername,
          avatarUrl: res.data.avatarUrl,
          lastLoginTime: Date.now()
        })
        await userStore.fetchCurrentUser(true).catch(() => {})
        const targetRedirect = continueUrl.value || '/competitions'
        resetFlow()
        return {
          success: true,
          redirect: targetRedirect
        }
      }
      throw new Error('登录认证未通过，请重试')
    })
  }

  function validateTransitionTicket(route: RouteLocationNormalizedLoaded): boolean {
    const queryTl = (route.query.TL as string) || tl.value
    return Boolean(queryTl && queryTl.startsWith('ADG-'))
  }

  function setIdentifier(val: string): void {
    identifier.value = val
    if (typeof window !== 'undefined' && window.sessionStorage) {
      if (val) {
        window.sessionStorage.setItem(STORAGE_IDENTIFIER_KEY, val)
      } else {
        window.sessionStorage.removeItem(STORAGE_IDENTIFIER_KEY)
      }
    }
  }

  function getQueryParams(extra?: Record<string, string>): Record<string, string> {
    const params: Record<string, string> = {
      continue: continueUrl.value,
      followup: followup.value,
      flowName: flowName.value,
      flowEntry: flowEntry.value,
      pstMsg: pstMsg.value
    }

    if (dsh.value) {
      params.dsh = dsh.value
    }
    if (tl.value) {
      params.TL = tl.value
    }
    if (cid.value) {
      params.cid = String(cid.value)
    }

    return { ...params, ...extra }
  }

  function resetFlow(keepLoading = false): void {
    dsh.value = ''
    tl.value = ''
    cid.value = 0
    setIdentifier('')
    activeChallengeType.value = ''
    if (!keepLoading) {
      loading.value = false
    }
  }

  function setFlowEntry(val: string): void {
    flowEntry.value = val
  }

  function setFlowName(val: string): void {
    flowName.value = val
  }

  function setFlow(name: string, entry: string): void {
    flowName.value = name
    flowEntry.value = entry
  }

  async function resendEmailOTP(): Promise<void> {
    if (!dsh.value || !tl.value) {
      throw new Error('会话凭据已失效')
    }
    return withLoading(async () => {
      await resendEmailOTPChallengeApi({ dsh: dsh.value, tl: tl.value })
    })
  }

  return {
    dsh,
    tl,
    cid,
    continueUrl,
    followup,
    flowName,
    flowEntry,
    pstMsg,
    identifier,
    activeChallengeType,
    loading,
    accountError,
    setAccountError,
    consumeAccountError,
    ensureFlowParams,
    submitIdentifier,
    selectAccount,
    submitChallenge,
    resendEmailOTP,
    selectChallenge,
    submitAuthenticate,
    validateTransitionTicket,
    getQueryParams,
    setIdentifier,
    setFlowEntry,
    setFlowName,
    setFlow,
    setLoading,
    startLoading,
    stopLoading,
    withLoading,
    triggerScrim,
    resetFlow
  }
})
