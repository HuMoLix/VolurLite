import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getPublicSystemSettingsApi } from '@/api/system'
import i18n from '@/i18n'

export const useSystemStore = defineStore('system', () => {
  const settings = ref<Record<string, string>>({})
  const loading = ref<boolean>(false)
  const isLoaded = ref<boolean>(false)
  const isServerReachable = ref<boolean>(true)
  let inFlightFetchPromise: Promise<Record<string, string>> | null = null

  // 1. 基础信息配置
  const platformName = computed<string>(() => {
    return settings.value['system.general.platform_name'] || 'Volur Lite'
  })

  const platformDesc = computed<string>(() => {
    return (
      settings.value['system.general.platform_desc'] ||
      'High-Performance Distributed Cybersecurity Competition Platform'
    )
  })

  const icpNumber = computed<string>(() => {
    return settings.value['system.general.icp_number'] || ''
  })

  const defaultLocale = computed<string>(() => {
    return settings.value['system.general.default_locale'] || 'zh_CN'
  })

  // 2. 鉴权与安全
  const isRegistrationEnabled = computed<boolean>(() => {
    return settings.value['system.auth.registration_enabled'] !== 'false'
  })

  const isEmailVerificationRequired = computed<boolean>(() => {
    return settings.value['system.auth.require_email_verification'] !== 'false'
  })

  const isCaptchaEnabled = computed<boolean>(() => {
    return settings.value['system.captcha.enabled'] !== 'false'
  })

  // 3. 运维与维护
  const isMaintenanceMode = computed<boolean>(() => {
    return settings.value['system.maintenance.enabled'] === 'true'
  })

  const maintenanceNotice = computed<string>(() => {
    return (
      settings.value['system.maintenance.notice_message'] ||
      '平台底层正在进行维护升级，暂不对外提供服务，请稍后访问。'
    )
  })

  async function fetchPublicSettings(force = false): Promise<Record<string, string>> {
    if (isLoaded.value && !force) {
      return settings.value
    }
    if (inFlightFetchPromise) {
      return inFlightFetchPromise
    }

    loading.value = true
    inFlightFetchPromise = (async () => {
      try {
        const res = await getPublicSystemSettingsApi()
        settings.value = res.data || {}
        isLoaded.value = true
        isServerReachable.value = true

        // 1. 动态初始化系统默认语言（若本地用户无手动覆盖偏好）
        const savedLocale = localStorage.getItem('locale')
        if (!savedLocale && settings.value['system.general.default_locale']) {
          const sysLocale = settings.value['system.general.default_locale']
          const targetLocale = sysLocale === 'zh' ? 'zh_CN' : (sysLocale === 'en' ? 'en_US' : sysLocale)
          const { loadLocaleMessagesAsync } = await import('@/i18n')
          await loadLocaleMessagesAsync(targetLocale)
          ;(i18n.global as any).locale.value = targetLocale
        }

        // 2. 动态更新网页标题后缀
        if (typeof document !== 'undefined') {
          const currentTitle = document.title.split(' | ')[0] || ''
          const currentPlatform = platformName.value
          document.title = currentTitle ? `${currentTitle} | ${currentPlatform}` : currentPlatform
        }

        return settings.value
      } catch (e) {
        console.error('Failed to fetch public system settings', e)
        isLoaded.value = false
        isServerReachable.value = false
        if (force) {
          throw e
        }
        return {}
      } finally {
        loading.value = false
        inFlightFetchPromise = null
      }
    })()

    return inFlightFetchPromise
  }

  function getSetting(key: string, defaultValue: string = ''): string {
    return settings.value[key] ?? defaultValue
  }

  return {
    settings,
    loading,
    isLoaded,
    isServerReachable,
    platformName,
    platformDesc,
    icpNumber,
    defaultLocale,
    isRegistrationEnabled,
    isEmailVerificationRequired,
    isCaptchaEnabled,
    isMaintenanceMode,
    maintenanceNotice,
    fetchPublicSettings,
    getSetting
  }
})
