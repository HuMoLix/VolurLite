import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { RouteLocationNormalizedLoaded, Router } from 'vue-router'
import { initFlowSessApi, submitSignInIdentifierApi, submitSignInChallengeApi } from '@/api/auth'
import { getDeviceFingerprint } from '@/utils/fingerprint'

const STORAGE_IDENTIFIER_KEY = 'volur_auth_identifier'

export const useAuthFlowStore = defineStore('authFlow', () => {
  const dsh = ref<string>('')
  const tl = ref<string>('')
  const cid = ref<number>(0)
  const continueUrl = ref<string>('/')
  const flowName = ref<string>('VlifWebLogin')
  const flowEntry = ref<string>('WebServiceLogin')
  const followup = ref<string>('/')
  const pstMsg = ref<string>('1')
  const identifier = ref<string>(
    (typeof window !== 'undefined' && window.sessionStorage?.getItem(STORAGE_IDENTIFIER_KEY)) || ''
  )
  const activeChallengeType = ref<string>('')
  const loading = ref<boolean>(false)

  async function ensureFlowParams(route: RouteLocationNormalizedLoaded, router: Router): Promise<void> {
    const query = route.query
    let changed = false
    const newQuery: Record<string, string> = { ...(query as Record<string, string>) }

    if (!newQuery.continue) {
      newQuery.continue = (query.redirect as string) || '/'
      changed = true
    }
    continueUrl.value = newQuery.continue

    if (!newQuery.followup) {
      newQuery.followup = newQuery.continue
      changed = true
    }
    followup.value = newQuery.followup

    const isSignUpPath = route.path.includes('/signup')
    const defaultFlowName = isSignUpPath ? 'VlifWebSignUp' : 'VlifWebLogin'
    if (!newQuery.flowName) {
      newQuery.flowName = defaultFlowName
      changed = true
    } else if (isSignUpPath && newQuery.flowName === 'VlifWebLogin') {
      newQuery.flowName = 'VlifWebSignUp'
      changed = true
    } else if (!isSignUpPath && newQuery.flowName === 'VlifWebSignUp') {
      newQuery.flowName = 'VlifWebLogin'
      changed = true
    }
    flowName.value = newQuery.flowName

    const defaultFlowEntry = isSignUpPath ? 'WebSignUp' : 'WebServiceLogin'
    if (!newQuery.flowEntry) {
      newQuery.flowEntry = defaultFlowEntry
      changed = true
    } else if (isSignUpPath && newQuery.flowEntry === 'WebServiceLogin') {
      newQuery.flowEntry = 'WebSignUp'
      changed = true
    } else if (!isSignUpPath && newQuery.flowEntry === 'WebSignUp') {
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
      dsh.value = query.dsh
    } else if (!dsh.value) {
      try {
        const fp = getDeviceFingerprint()
        const t = Date.now()
        const initRes = await initFlowSessApi({
          flowName: newQuery.flowName,
          flowEntry: newQuery.flowEntry,
          fp,
          t,
          dsh: typeof query.dsh === 'string' ? query.dsh : undefined
        })
        if (initRes.data?.dsh) {
          dsh.value = initRes.data.dsh
          newQuery.dsh = dsh.value
          changed = true
        }
      } catch {
      }
    } else {
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
      activeChallengeType.value = data.nextStep || 'pwd'

      return activeChallengeType.value
    })
  }

  async function submitChallenge(
    challengeType: string,
    payload: {
      password?: string
      captchaCode?: string
      verificationCode?: string
      rememberMe?: boolean
    }
  ): Promise<{ success: boolean; nextStep?: string; redirect?: string }> {
    return withLoading(async () => {
      const res = await submitSignInChallengeApi({
        dsh: dsh.value,
        tl: tl.value,
        cid: cid.value,
        challengeType,
        ...payload
      })

      const data = res.data

      if (data.status === 'SUCCESS') {
        if (data.accessToken && typeof localStorage !== 'undefined') {
          localStorage.setItem('access_token', data.accessToken)
        }
        if (data.refreshToken && typeof localStorage !== 'undefined') {
          localStorage.setItem('refresh_token', data.refreshToken)
        }
        return {
          success: true,
          redirect: continueUrl.value || '/competitions'
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

      return {
        success: false,
        nextStep: data.nextStep
      }
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
      dsh: dsh.value,
      pstMsg: pstMsg.value
    }

    if (tl.value) {
      params.TL = tl.value
    }
    params.cid = String(cid.value)

    return { ...params, ...extra }
  }

  function setLoading(val: boolean): void {
    loading.value = val
  }

  function startLoading(): void {
    loading.value = true
  }

  function stopLoading(): void {
    loading.value = false
  }

  async function withLoading<T>(action: () => Promise<T>): Promise<T> {
    if (loading.value) {
      throw new Error('AUTH_FLOW_REQUEST_IN_FLIGHT')
    }
    loading.value = true
    try {
      return await action()
    } finally {
      loading.value = false
    }
  }

  function resetFlow(): void {
    dsh.value = ''
    tl.value = ''
    cid.value = 0
    setIdentifier('')
    activeChallengeType.value = ''
    loading.value = false
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
    ensureFlowParams,
    submitIdentifier,
    submitChallenge,
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
    resetFlow
  }
})
