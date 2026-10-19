import { ref } from 'vue'

export const MIN_LOADING_DURATION_MS = 1000

export function createFlowLoading(defaultMinDurationMs = MIN_LOADING_DURATION_MS) {
  const loading = ref<boolean>(false)

  function setLoading(val: boolean): void {
    loading.value = val
  }

  function startLoading(): void {
    loading.value = true
  }

  function stopLoading(): void {
    loading.value = false
  }

  async function withLoading<T>(
    action: () => Promise<T>,
    minDurationMs = defaultMinDurationMs
  ): Promise<T> {
    if (loading.value) {
      throw new Error('AUTH_FLOW_REQUEST_IN_FLIGHT')
    }
    loading.value = true
    const startTime = Date.now()
    try {
      const result = await action()
      const elapsed = Date.now() - startTime
      if (elapsed < minDurationMs) {
        await new Promise(resolve => setTimeout(resolve, minDurationMs - elapsed))
      }
      return result
    } catch (err) {
      const elapsed = Date.now() - startTime
      if (elapsed < minDurationMs) {
        await new Promise(resolve => setTimeout(resolve, minDurationMs - elapsed))
      }
      throw err
    } finally {
      loading.value = false
    }
  }

  async function triggerScrim<T = void>(
    action?: () => Promise<T> | T,
    durationMs = defaultMinDurationMs
  ): Promise<T | undefined> {
    if (loading.value) return undefined
    return withLoading(async () => {
      if (action) {
        return await action()
      }
      return undefined as unknown as T
    }, durationMs)
  }

  return {
    loading,
    setLoading,
    startLoading,
    stopLoading,
    withLoading,
    triggerScrim
  }
}
