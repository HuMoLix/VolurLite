import { ref, computed, watch, type WatchSource } from 'vue'
import { useI18n } from 'vue-i18n'

export function useFormError(watchSources?: WatchSource | WatchSource[]) {
  const { t } = useI18n()
  const errorKey = ref('')
  const rawErrorMsg = ref('')

  const errorMsg = computed(() => {
    if (errorKey.value) {
      return t(errorKey.value)
    }
    return rawErrorMsg.value
  })

  function clearError(): void {
    errorKey.value = ''
    rawErrorMsg.value = ''
  }

  function setError(keyOrMsg: string): void {
    if (!keyOrMsg) {
      clearError()
      return
    }
    if (keyOrMsg.startsWith('auth.') || keyOrMsg.startsWith('common.') || keyOrMsg.startsWith('error.')) {
      errorKey.value = keyOrMsg
      rawErrorMsg.value = ''
    } else {
      rawErrorMsg.value = keyOrMsg
      errorKey.value = ''
    }
  }

  function setErrorKey(key: string): void {
    errorKey.value = key
    rawErrorMsg.value = ''
  }

  function setRawError(msg: string): void {
    rawErrorMsg.value = msg
    errorKey.value = ''
  }

  if (watchSources) {
    watch(watchSources, () => {
      if (errorKey.value || rawErrorMsg.value) {
        clearError()
      }
    })
  }

  return {
    errorKey,
    rawErrorMsg,
    errorMsg,
    clearError,
    setError,
    setErrorKey,
    setRawError
  }
}
