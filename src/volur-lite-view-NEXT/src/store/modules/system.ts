import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getPublicSystemSettingsApi } from '@/api/system'

export const useSystemStore = defineStore('system', () => {
  const settings = ref<Record<string, string>>({})
  const isLoaded = ref<boolean>(false)
  let inFlightPromise: Promise<Record<string, string>> | null = null

  async function fetchPublicSettings(force = false): Promise<Record<string, string>> {
    if (isLoaded.value && !force) {
      return settings.value
    }
    if (inFlightPromise) {
      return inFlightPromise
    }

    inFlightPromise = (async () => {
      try {
        const res = await getPublicSystemSettingsApi()
        if (res.data) {
          settings.value = res.data
          isLoaded.value = true
        }
        return settings.value
      } catch {
        return settings.value
      } finally {
        inFlightPromise = null
      }
    })()

    return inFlightPromise
  }

  const icpNumber = computed(() => settings.value['system.general.icp_number'] || '')
  const copyright = computed(() => settings.value['system.general.copyright'] || '© 2026 Volur, Inc.')
  const platformName = computed(() => settings.value['system.general.platform_name'] || 'Volur Lite')
  const platformDesc = computed(() => settings.value['system.general.platform_desc'] || '')
  const isMaintenanceMode = computed(() => settings.value['system.maintenance.enabled'] === 'true')
  const maintenanceNotice = computed(() => settings.value['system.maintenance.notice_message'] || '')

  return {
    settings,
    isLoaded,
    icpNumber,
    copyright,
    platformName,
    platformDesc,
    isMaintenanceMode,
    maintenanceNotice,
    fetchPublicSettings
  }
})
