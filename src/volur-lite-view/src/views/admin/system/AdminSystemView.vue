<template>
  <div class="admin-system-view">
    <!-- Top section: System Health Overview Card -->
    <n-card class="overview-card" :bordered="false">
      <div class="overview-grid">
        <div class="overview-item">
          <div class="item-icon-box primary">
            <n-icon size="24">
              <ServerOutline />
            </n-icon>
          </div>
          <div class="item-meta">
            <span class="meta-label">{{ t('admin.system.overview.kernel_version') }}</span>
            <span class="meta-value">{{ overview.kernelVersion || 'Volur Lite v1.0.0-SNAPSHOT' }}</span>
          </div>
        </div>

        <div class="overview-item">
          <div class="item-icon-box info">
            <n-icon size="24">
              <CodeSlashOutline />
            </n-icon>
          </div>
          <div class="item-meta">
            <span class="meta-label">{{ t('admin.system.overview.runtime_env') }}</span>
            <span class="meta-value">{{ overview.runtimeEnv || 'Spring Boot 3.4.3 / JDK 25' }}</span>
          </div>
        </div>

        <div class="overview-item">
          <div class="item-icon-box success">
            <n-icon size="24">
              <HardwareChipOutline />
            </n-icon>
          </div>
          <div class="item-meta">
            <span class="meta-label">{{ t('admin.system.overview.redis_status') }}</span>
            <span
              class="meta-value"
              :class="overview.redisStatus === 'CONNECTED' ? 'status-online' : 'status-offline'"
            >
              {{ overview.redisStatus === 'CONNECTED' ? t('admin.system.overview.redis_connected', { latency: overview.redisLatencyMs }) : t('admin.system.overview.redis_disconnected') }}
            </span>
          </div>
        </div>

        <div class="overview-item">
          <div class="item-icon-box warning">
            <n-icon size="24">
              <RadioOutline />
            </n-icon>
          </div>
          <div class="item-meta">
            <span class="meta-label">{{ t('admin.system.overview.sse_status') }}</span>
            <span
              class="meta-value"
              :class="overview.sseStatus === 'ONLINE' ? 'status-online' : 'status-offline'"
            >
              {{ overview.sseStatus === 'ONLINE' ? t('admin.system.overview.sse_online') : t('admin.system.overview.sse_offline') }}
            </span>
          </div>
        </div>
      </div>
    </n-card>

    <!-- Bottom section: System Settings Form Tabs Card -->
    <n-card class="settings-card" :bordered="false">
      <n-tabs type="line" animated class="settings-tabs">
        <!-- Tab 1: 平台基础设置 -->
        <n-tab-pane name="general" :tab="t('admin.system.tabs.general')">
          <GeneralSettingsTab :form="settingsForm" />
        </n-tab-pane>

        <!-- Tab 2: 人机验证与风控 -->
        <n-tab-pane name="captcha" :tab="t('admin.system.tabs.captcha')">
          <CaptchaSettingsTab :form="settingsForm" />
        </n-tab-pane>

        <!-- Tab 3: 发信与 SMTP 设置 -->
        <n-tab-pane name="mail" :tab="t('admin.system.tabs.mail')">
          <MailSettingsTab :form="settingsForm" />
        </n-tab-pane>

        <!-- Tab 4: 平台运维与风控 -->
        <n-tab-pane name="maintenance" :tab="t('admin.system.tabs.maintenance')">
          <MaintenanceSettingsTab :form="settingsForm" />
        </n-tab-pane>
      </n-tabs>

      <!-- Bottom Action Footer -->
      <div class="settings-action-bar">
        <n-button quaternary @click="handleResetDefault">
          {{ t('admin.system.actions.reset_default') }}
        </n-button>
        <n-button
          type="primary"
          :loading="saveLoading"
          class="theme-primary-btn"
          @click="handleSaveSettings"
        >
          {{ t('admin.system.actions.save_all') }}
        </n-button>
      </div>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  NCard,
  NIcon,
  NTabs,
  NTabPane,
  NButton,
  useMessage
} from 'naive-ui'
import {
  ServerOutline,
  CodeSlashOutline,
  HardwareChipOutline,
  RadioOutline
} from '@vicons/ionicons5'
import {
  getAdminSystemOverviewApi,
  getAdminSystemSettingsApi,
  batchUpdateAdminSystemSettingsApi
} from '@/api/system'
import type { SystemSettingVO, SystemSettingUpdateDTO, AdminSystemOverviewVO } from '@/types/system'

// Tab Components
import GeneralSettingsTab from './tabs/GeneralSettingsTab.vue'
import CaptchaSettingsTab from './tabs/CaptchaSettingsTab.vue'
import MailSettingsTab from './tabs/MailSettingsTab.vue'
import MaintenanceSettingsTab from './tabs/MaintenanceSettingsTab.vue'

const { t } = useI18n()
const message = useMessage()
const saveLoading = ref(false)

// System Health Overview State
const overview = reactive<AdminSystemOverviewVO>({
  kernelVersion: 'Volur Lite v1.0.0-SNAPSHOT',
  springBootVersion: '3.4.3',
  javaVersion: 'JDK 25',
  runtimeEnv: 'Spring Boot 3.4.3 / JDK 25',
  osInfo: '',
  redisStatus: 'CONNECTED',
  redisLatencyMs: 0,
  sseStatus: 'ONLINE',
  uptimeSeconds: 0,
  heapMemoryUsedMb: 0,
  heapMemoryMaxMb: 0
})

// Settings Form State
const settingsForm = reactive({
  // General
  platformName: 'Volur Lite',
  platformDesc: 'High-Performance Distributed Cybersecurity Competition Platform',
  icpNumber: '',
  defaultLocale: 'zh_CN',
  registrationEnabled: true,
  requireEmailVerification: true,
  // Captcha
  captchaEnabled: true,
  captchaTolerance: 8,
  captchaTicketTtl: 300,
  captchaMaxAttempts: 5,
  forceCaptchaOnReset: true,
  // Mail
  smtpHost: 'smtp.example.com',
  smtpPort: 465,
  smtpUser: 'noreply@volur.local',
  fromAddress: 'noreply@volur.local',
  smtpPass: '',
  smtpSsl: true,
  senderName: 'Volur Lite Security Team',
  webhookEnabled: false,
  webhookSecret: '',
  webhookFallbackStrategy: 'AUTO_DELIVERED',
  // Maintenance
  maintenanceMode: false,
  maintenanceNotice: '平台底层正在维护，请稍后访问。',
  rateLimitWindow: 60,
  rateLimitMaxRequests: 120
})

async function fetchOverview() {
  try {
    const res = await getAdminSystemOverviewApi()
    if (res.data) {
      Object.assign(overview, res.data)
    }
  } catch (error) {
    console.warn('Fallback to local default system overview:', error)
  }
}

async function fetchSettings() {
  try {
    const res = await getAdminSystemSettingsApi()
    if (res.data && Array.isArray(res.data)) {
      res.data.forEach((item: SystemSettingVO) => {
        if (item.key === 'system.general.platform_name') settingsForm.platformName = item.value
        if (item.key === 'system.general.platform_desc') settingsForm.platformDesc = item.value
        if (item.key === 'system.general.icp_number') settingsForm.icpNumber = item.value
        if (item.key === 'system.auth.registration_enabled') settingsForm.registrationEnabled = item.value === 'true'
        if (item.key === 'system.auth.require_email_verification') settingsForm.requireEmailVerification = item.value !== 'false'
        if (item.key === 'system.general.default_locale') settingsForm.defaultLocale = item.value
        if (item.key === 'system.captcha.enabled') settingsForm.captchaEnabled = item.value === 'true'
        if (item.key === 'system.captcha.tolerance_px') settingsForm.captchaTolerance = Number(item.value) || 8
        if (item.key === 'system.captcha.ticket_ttl_seconds') settingsForm.captchaTicketTtl = Number(item.value) || 300
        if (item.key === 'system.captcha.max_attempts') settingsForm.captchaMaxAttempts = Number(item.value) || 5
        if (item.key === 'system.captcha.force_reset_captcha') settingsForm.forceCaptchaOnReset = item.value === 'true'
        if (item.key === 'system.mail.smtp_host') settingsForm.smtpHost = item.value
        if (item.key === 'system.mail.smtp_port') settingsForm.smtpPort = Number(item.value) || 465
        if (item.key === 'system.mail.smtp_user') settingsForm.smtpUser = item.value
        if (item.key === 'system.mail.from_address') settingsForm.fromAddress = item.value
        if (item.key === 'system.mail.smtp_ssl') settingsForm.smtpSsl = item.value === 'true'
        if (item.key === 'system.mail.sender_name') settingsForm.senderName = item.value
        if (item.key === 'system.mail.webhook_enabled') settingsForm.webhookEnabled = item.value === 'true'
        if (item.key === 'system.mail.webhook_secret') settingsForm.webhookSecret = item.value
        if (item.key === 'system.mail.webhook_fallback_strategy') settingsForm.webhookFallbackStrategy = item.value || 'AUTO_DELIVERED'
        if (item.key === 'system.maintenance.enabled') settingsForm.maintenanceMode = item.value === 'true'
        if (item.key === 'system.maintenance.notice_message') settingsForm.maintenanceNotice = item.value
        if (item.key === 'system.security.rate_limit_window') settingsForm.rateLimitWindow = Number(item.value) || 60
        if (item.key === 'system.security.rate_limit_max_requests') settingsForm.rateLimitMaxRequests = Number(item.value) || 120
      })
    }
  } catch (error) {
    console.warn('Fallback to local default settings:', error)
  }
}

async function handleSaveSettings() {
  if (!settingsForm.smtpHost?.trim()) {
    message.error(t('admin.system.messages.smtp_host_required'))
    return
  }
  if (!settingsForm.smtpUser?.trim()) {
    message.error(t('admin.system.messages.smtp_user_required'))
    return
  }
  if (!settingsForm.fromAddress?.trim()) {
    message.error(t('admin.system.messages.from_address_required'))
    return
  }

  saveLoading.value = true
  try {
    const payload: SystemSettingUpdateDTO[] = [
      { key: 'system.general.platform_name', value: settingsForm.platformName },
      { key: 'system.general.platform_desc', value: settingsForm.platformDesc },
      { key: 'system.general.icp_number', value: settingsForm.icpNumber },
      { key: 'system.auth.registration_enabled', value: String(settingsForm.registrationEnabled) },
      { key: 'system.auth.require_email_verification', value: String(settingsForm.requireEmailVerification) },
      { key: 'system.general.default_locale', value: settingsForm.defaultLocale },
      { key: 'system.captcha.enabled', value: String(settingsForm.captchaEnabled) },
      { key: 'system.captcha.tolerance_px', value: String(settingsForm.captchaTolerance) },
      { key: 'system.captcha.ticket_ttl_seconds', value: String(settingsForm.captchaTicketTtl) },
      { key: 'system.captcha.max_attempts', value: String(settingsForm.captchaMaxAttempts) },
      { key: 'system.captcha.force_reset_captcha', value: String(settingsForm.forceCaptchaOnReset) },
      { key: 'system.mail.smtp_host', value: settingsForm.smtpHost },
      { key: 'system.mail.smtp_port', value: String(settingsForm.smtpPort) },
      { key: 'system.mail.smtp_user', value: settingsForm.smtpUser },
      { key: 'system.mail.from_address', value: settingsForm.fromAddress },
      { key: 'system.mail.smtp_ssl', value: String(settingsForm.smtpSsl) },
      { key: 'system.mail.sender_name', value: settingsForm.senderName },
      { key: 'system.mail.webhook_enabled', value: String(settingsForm.webhookEnabled) },
      { key: 'system.mail.webhook_secret', value: settingsForm.webhookSecret || '' },
      { key: 'system.mail.webhook_fallback_strategy', value: settingsForm.webhookFallbackStrategy || 'AUTO_DELIVERED' },
      { key: 'system.maintenance.enabled', value: String(settingsForm.maintenanceMode) },
      { key: 'system.maintenance.notice_message', value: settingsForm.maintenanceNotice },
      { key: 'system.security.rate_limit_window', value: String(settingsForm.rateLimitWindow) },
      { key: 'system.security.rate_limit_max_requests', value: String(settingsForm.rateLimitMaxRequests) }
    ]

    if (settingsForm.smtpPass) {
      payload.push({ key: 'system.mail.smtp_pass', value: settingsForm.smtpPass })
    }

    await batchUpdateAdminSystemSettingsApi(payload)
    message.success(t('admin.system.messages.save_success'))
    fetchSettings()
    fetchOverview()
  } catch (error: any) {
    message.error(error.message || t('admin.system.messages.save_failed'))
  } finally {
    saveLoading.value = false
  }
}

function handleResetDefault() {
  settingsForm.platformName = 'Volur Lite'
  settingsForm.platformDesc = 'High-Performance Distributed Cybersecurity Competition Platform'
  settingsForm.icpNumber = ''
  settingsForm.defaultLocale = 'zh_CN'
  settingsForm.registrationEnabled = true
  settingsForm.captchaEnabled = true
  settingsForm.captchaTolerance = 8
  settingsForm.captchaTicketTtl = 300
  settingsForm.captchaMaxAttempts = 5
  settingsForm.forceCaptchaOnReset = true
  settingsForm.smtpHost = 'smtp.example.com'
  settingsForm.smtpPort = 465
  settingsForm.smtpUser = 'noreply@volur.local'
  settingsForm.fromAddress = 'noreply@volur.local'
  settingsForm.smtpPass = ''
  settingsForm.smtpSsl = true
  settingsForm.senderName = 'Volur Lite Security Team'
  settingsForm.maintenanceMode = false
  settingsForm.maintenanceNotice = '平台底层正在维护，请稍后访问。'
  settingsForm.rateLimitWindow = 60
  settingsForm.rateLimitMaxRequests = 120
  message.info(t('admin.system.messages.reset_info'))
}

onMounted(() => {
  fetchOverview()
  fetchSettings()
})
</script>

<style scoped lang="scss">
.admin-system-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.overview-card {
  border-radius: 10px;
  background-color: var(--v-bg-card, #ffffff);
  border: 1px solid var(--v-border-base, #e5e4e7);

  .overview-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
    gap: 16px;

    .overview-item {
      display: flex;
      align-items: center;
      gap: 12px;

      .item-icon-box {
        width: 44px;
        height: 44px;
        border-radius: 10px;
        display: flex;
        align-items: center;
        justify-content: center;

        &.primary {
          background-color: rgba(99, 102, 241, 0.1);
          color: #6366f1;
        }

        &.info {
          background-color: rgba(59, 130, 246, 0.1);
          color: #3b82f6;
        }

        &.success {
          background-color: rgba(16, 185, 129, 0.1);
          color: #10b981;
        }

        &.warning {
          background-color: rgba(245, 158, 11, 0.1);
          color: #f59e0b;
        }
      }

      .item-meta {
        display: flex;
        flex-direction: column;
        gap: 2px;

        .meta-label {
          font-size: 12px;
          color: var(--v-text-desc, #666666);
        }

        .meta-value {
          font-size: 14px;
          font-weight: 600;
          color: var(--v-text-primary, #111111);

          &.status-online {
            color: #10b981;
          }

          &.status-offline {
            color: #ef4444;
          }
        }
      }
    }
  }
}

.settings-card {
  border-radius: 10px;
  background-color: var(--v-bg-card, #ffffff);
  border: 1px solid var(--v-border-base, #e5e4e7);

  .tab-content-container {
    padding: 24px 8px;
  }

  .switch-row {
    display: flex;
    align-items: center;
  }

  .field-hint {
    font-size: 12px;
    color: #888888;
    margin-left: 12px;
  }

  .range-inputs {
    display: flex;
    align-items: center;
    gap: 8px;

    .range-divider {
      color: #888888;
    }
  }

  .settings-action-bar {
    display: flex;
    justify-content: flex-end;
    align-items: center;
    gap: 16px;
    padding-top: 20px;
    border-top: 1px solid var(--v-border-base, #efeff5);
    margin-top: 20px;
  }
}

.theme-primary-btn {
  background-color: #000000;
  color: #ffffff;
  font-weight: 500;
  border-radius: 6px;

  &:hover {
    background-color: #222222;
  }
}
</style>
