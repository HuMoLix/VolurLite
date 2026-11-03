import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { RouteLocationNormalizedLoaded, Router } from 'vue-router'
import {
  initSessionApi,
  submitSignUpNameApi,
  submitSignUpEmailApi,
  resendSignUpEmailApi,
  submitSignUpEtotpApi,
  submitSignUpPasswordApi,
  submitTotpEnableApi
} from '@/api/auth'
import { collectDeviceParams, getDeviceFingerprint, initDeviceFingerprint } from '@/utils/fingerprint'
import { isDshExpired, buildSessionExpireHref, buildSessionExpireLocation } from '@/utils/flow'
import { encryptFlowCredential } from '@/utils/flow-crypto'
import { useUserStore } from '@/store/modules/user'
import { createFlowLoading } from './flowLoading'

const STORAGE_SIGNUP_USERNAME_KEY = 'volur_signup_username'
const STORAGE_SIGNUP_EMAIL_KEY = 'volur_signup_email'

export const useSignupFlowStore = defineStore('signupFlow', () => {
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
  const flowName = ref<string>('VlifWebSignUp')
  const flowEntry = ref<string>('WebSignUp')
  const followup = ref<string>('/v1/signup')
  const pstMsg = ref<string>('1')
  const username = ref<string>(
    (typeof window !== 'undefined' && window.sessionStorage?.getItem(STORAGE_SIGNUP_USERNAME_KEY)) || ''
  )
  const email = ref<string>(
    (typeof window !== 'undefined' && window.sessionStorage?.getItem(STORAGE_SIGNUP_EMAIL_KEY)) || ''
  )

  function setUsername(val: string): void {
    username.value = val
    if (typeof window !== 'undefined' && window.sessionStorage) {
      if (val) {
        window.sessionStorage.setItem(STORAGE_SIGNUP_USERNAME_KEY, val)
      } else {
        window.sessionStorage.removeItem(STORAGE_SIGNUP_USERNAME_KEY)
      }
    }
  }

  function setEmail(val: string): void {
    email.value = val
    if (typeof window !== 'undefined' && window.sessionStorage) {
      if (val) {
        window.sessionStorage.setItem(STORAGE_SIGNUP_EMAIL_KEY, val)
      } else {
        window.sessionStorage.removeItem(STORAGE_SIGNUP_EMAIL_KEY)
      }
    }
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
      newQuery.followup = '/v1/signup'
      changed = true
    }
    followup.value = newQuery.followup

    if (!newQuery.flowName) {
      newQuery.flowName = 'VlifWebSignUp'
      changed = true
    }
    flowName.value = newQuery.flowName

    if (!newQuery.flowEntry) {
      newQuery.flowEntry = 'WebSignUp'
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

    if (changed) {
      router.replace({
        path: route.path,
        query: newQuery
      })
    }
  }

  function validateTransitionTicket(route: RouteLocationNormalizedLoaded): boolean {
    const queryTl = (route.query.TL as string) || tl.value
    return Boolean(queryTl && queryTl.startsWith('ADG-'))
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

  function checkDshExpiration(): void {
    if (dsh.value && isDshExpired(dsh.value)) {
      resetFlow()
      if (typeof window !== 'undefined') {
        window.location.href = buildSessionExpireHref()
      }
      throw new Error('会话已超时，请重新开始')
    }
  }

  async function submitSignUpName(val: string): Promise<{ nextStep: string; status?: string }> {
    checkDshExpiration()
    return withLoading(async () => {
      const encryptedUsername = await encryptFlowCredential(
        val,
        flowName.value || 'VlifWebSignUp',
        flowEntry.value || 'WebSignUp',
        dsh.value
      )
      const res = await submitSignUpNameApi({
        username: encryptedUsername,
        dsh: dsh.value,
        tl: tl.value || undefined,
        cid: cid.value || 0
      })
      const data = res.data
      setUsername(val)
      if (data.dsh) dsh.value = data.dsh
      if (data.tl) tl.value = data.tl
      cid.value = typeof data.cid === 'number' ? data.cid : 1
      if (!data.nextStep) {
        throw new Error('服务端未返回下一步认证流程')
      }
      return {
        nextStep: data.nextStep,
        status: data.status
      }
    })
  }

  async function submitSignUpEmail(val: string): Promise<string> {
    checkDshExpiration()
    return withLoading(async () => {
      const res = await submitSignUpEmailApi({
        email: val,
        dsh: dsh.value,
        tl: tl.value,
        cid: cid.value
      })
      const data = res.data
      setEmail(val)
      if (data.dsh) dsh.value = data.dsh
      if (data.tl) tl.value = data.tl
      cid.value = typeof data.cid === 'number' ? data.cid : cid.value + 1
      if (!data.nextStep) {
        throw new Error('服务端未返回下一步认证流程')
      }
      return data.nextStep
    })
  }

  async function resendSignUpEmail(): Promise<void> {
    checkDshExpiration()
    return withLoading(async () => {
      await resendSignUpEmailApi({
        dsh: dsh.value,
        tl: tl.value,
        cid: cid.value
      })
    })
  }

  async function submitSignUpEtotp(code: string): Promise<string> {
    checkDshExpiration()
    return withLoading(async () => {
      const res = await submitSignUpEtotpApi({
        code,
        dsh: dsh.value,
        tl: tl.value,
        cid: cid.value
      })
      const data = res.data
      if (data.dsh) dsh.value = data.dsh
      if (data.tl) tl.value = data.tl
      cid.value = typeof data.cid === 'number' ? data.cid : cid.value + 1
      if (!data.nextStep) {
        throw new Error('服务端未返回下一步认证流程')
      }
      return data.nextStep
    })
  }

  async function submitSignUpPassword(
    password: string,
    confirmPassword: string
  ): Promise<{ nextStep: string; status: string }> {
    checkDshExpiration()
    return withLoading(async () => {
      const encryptedPassword = await encryptFlowCredential(
        password,
        flowName.value || 'VlifWebSignUp',
        flowEntry.value || 'WebSignUp',
        dsh.value
      )
      const encryptedConfirmPassword = await encryptFlowCredential(
        confirmPassword,
        flowName.value || 'VlifWebSignUp',
        flowEntry.value || 'WebSignUp',
        dsh.value
      )
      const res = await submitSignUpPasswordApi({
        password: encryptedPassword,
        confirmPassword: encryptedConfirmPassword,
        dsh: dsh.value,
        tl: tl.value,
        cid: cid.value
      })
      const data = res.data
      if (data.dsh) dsh.value = data.dsh
      if (data.tl) tl.value = data.tl
      cid.value = typeof data.cid === 'number' ? data.cid : cid.value + 1
      if (!data.nextStep) {
        throw new Error('服务端未返回下一步认证流程')
      }
      if (username.value) {
        const userStore = useUserStore()
        userStore.setAuthenticatedUser({
          identifier: username.value,
          username: username.value,
          email: email.value
        })
        await userStore.fetchCurrentUser(true).catch(() => {})
      }
      return {
        nextStep: data.nextStep,
        status: data.status || 'COMPLETE'
      }
    })
  }

  async function submitTotpEnable(code: string): Promise<boolean> {
    checkDshExpiration()
    return withLoading(async () => {
      const res = await submitTotpEnableApi({
        dsh: dsh.value,
        tl: tl.value,
        cid: cid.value,
        code
      })
      if (!res.data?.success) {
        throw new Error(res.data?.message || '动态验证码核验失败')
      }
      return true
    })
  }

  function resetFlow(keepLoading = false): void {
    dsh.value = ''
    tl.value = ''
    cid.value = 0
    username.value = ''
    email.value = ''
    if (typeof window !== 'undefined' && window.sessionStorage) {
      window.sessionStorage.removeItem(STORAGE_SIGNUP_USERNAME_KEY)
      window.sessionStorage.removeItem(STORAGE_SIGNUP_EMAIL_KEY)
    }
    if (!keepLoading) {
      loading.value = false
    }
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
    username,
    email,
    loading,
    ensureFlowParams,
    validateTransitionTicket,
    getQueryParams,
    setUsername,
    setEmail,
    submitSignUpName,
    submitSignUpEmail,
    resendSignUpEmail,
    submitSignUpEtotp,
    submitSignUpPassword,
    submitTotpEnable,
    setLoading,
    startLoading,
    stopLoading,
    withLoading,
    triggerScrim,
    resetFlow
  }
})
