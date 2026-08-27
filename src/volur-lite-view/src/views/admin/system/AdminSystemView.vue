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
          <div class="tab-content-container">
            <n-form
              ref="generalFormRef"
              :model="settingsForm"
              label-placement="left"
              label-width="160"
              require-mark-placement="right-hanging"
            >
              <n-form-item :label="t('admin.system.general.platform_name')" path="platformName">
                <n-input
                  v-model:value="settingsForm.platformName"
                  :placeholder="t('admin.system.general.platform_name_placeholder')"
                  style="max-width: 480px;"
                />
              </n-form-item>

              <n-form-item :label="t('admin.system.general.platform_desc')" path="platformDesc">
                <n-input
                  v-model:value="settingsForm.platformDesc"
                  type="textarea"
                  :rows="2"
                  :placeholder="t('admin.system.general.platform_desc_placeholder')"
                  style="max-width: 480px;"
                />
              </n-form-item>

              <n-form-item :label="t('admin.system.general.icp_number')" path="icpNumber">
                <n-input
                  v-model:value="settingsForm.icpNumber"
                  :placeholder="t('admin.system.general.icp_placeholder')"
                  style="max-width: 480px;"
                />
              </n-form-item>

              <n-form-item :label="t('admin.system.general.default_locale')" path="defaultLocale">
                <n-select
                  v-model:value="settingsForm.defaultLocale"
                  :options="localeOptions"
                  style="max-width: 260px;"
                />
              </n-form-item>

              <n-form-item :label="t('admin.system.general.registration_enabled')" path="registrationEnabled">
                <n-switch v-model:value="settingsForm.registrationEnabled">
                  <template #checked>ON</template>
                  <template #unchecked>OFF</template>
                </n-switch>
                <span class="field-hint">{{ t('admin.system.general.registration_hint') }}</span>
              </n-form-item>
            </n-form>
          </div>
        </n-tab-pane>

        <!-- Tab 2: 人机验证与风控 -->
        <n-tab-pane name="captcha" :tab="t('admin.system.tabs.captcha')">
          <div class="tab-content-container">
            <n-form
              :model="settingsForm"
              label-placement="left"
              label-width="180"
              require-mark-placement="right-hanging"
            >
              <n-form-item :label="t('admin.system.captcha.enabled')" path="captchaEnabled">
                <n-switch v-model:value="settingsForm.captchaEnabled">
                  <template #checked>ON</template>
                  <template #unchecked>OFF</template>
                </n-switch>
                <span class="field-hint">{{ t('admin.system.captcha.enabled_hint') }}</span>
              </n-form-item>

              <n-form-item :label="t('admin.system.captcha.tolerance')" path="captchaTolerance">
                <n-input-number
                  v-model:value="settingsForm.captchaTolerance"
                  :min="2"
                  :max="25"
                  style="width: 220px;"
                />
                <span class="field-hint">{{ t('admin.system.captcha.tolerance_hint') }}</span>
              </n-form-item>

              <n-form-item :label="t('admin.system.captcha.ticket_ttl')" path="captchaTicketTtl">
                <n-input-number
                  v-model:value="settingsForm.captchaTicketTtl"
                  :min="60"
                  :max="1800"
                  style="width: 220px;"
                />
                <span class="field-hint">{{ t('admin.system.captcha.ticket_ttl_hint') }}</span>
              </n-form-item>

              <n-form-item :label="t('admin.system.captcha.max_attempts')" path="captchaMaxAttempts">
                <n-input-number
                  v-model:value="settingsForm.captchaMaxAttempts"
                  :min="3"
                  :max="20"
                  style="width: 220px;"
                />
                <span class="field-hint">{{ t('admin.system.captcha.max_attempts_hint') }}</span>
              </n-form-item>

              <n-form-item :label="t('admin.system.captcha.force_reset')" path="forceCaptchaOnReset">
                <n-switch v-model:value="settingsForm.forceCaptchaOnReset">
                  <template #checked>ON</template>
                  <template #unchecked>OFF</template>
                </n-switch>
                <span class="field-hint">{{ t('admin.system.captcha.force_reset_hint') }}</span>
              </n-form-item>
            </n-form>
          </div>
        </n-tab-pane>

        <!-- Tab 3: 发信与 SMTP 设置 -->
        <n-tab-pane name="mail" :tab="t('admin.system.tabs.mail')">
          <div class="tab-content-container">
            <n-form
              :model="settingsForm"
              label-placement="left"
              label-width="160"
              require-mark-placement="right-hanging"
            >
              <n-form-item :label="t('admin.system.mail.host')" path="smtpHost">
                <n-input
                  v-model:value="settingsForm.smtpHost"
                  :placeholder="t('admin.system.mail.host_placeholder')"
                  style="max-width: 420px;"
                />
              </n-form-item>

              <n-form-item :label="t('admin.system.mail.port')" path="smtpPort">
                <n-input-number
                  v-model:value="settingsForm.smtpPort"
                  :min="1"
                  :max="65535"
                  style="width: 200px;"
                />
                <span class="field-hint">{{ t('admin.system.mail.port_hint') }}</span>
              </n-form-item>

              <n-form-item :label="t('admin.system.mail.user')" path="smtpUser">
                <n-input
                  v-model:value="settingsForm.smtpUser"
                  :placeholder="t('admin.system.mail.user_placeholder')"
                  style="max-width: 420px;"
                />
              </n-form-item>

              <n-form-item :label="t('admin.system.mail.pass')" path="smtpPass">
                <n-input
                  v-model:value="settingsForm.smtpPass"
                  type="password"
                  show-password-on="click"
                  :placeholder="t('admin.system.mail.pass_placeholder')"
                  style="max-width: 420px;"
                />
              </n-form-item>

              <n-form-item :label="t('admin.system.mail.ssl')" path="smtpSsl">
                <n-switch v-model:value="settingsForm.smtpSsl">
                  <template #checked>ON</template>
                  <template #unchecked>OFF</template>
                </n-switch>
                <span class="field-hint">{{ t('admin.system.mail.ssl_hint') }}</span>
              </n-form-item>

              <n-form-item :label="t('admin.system.mail.sender_name')" path="senderName">
                <n-input
                  v-model:value="settingsForm.senderName"
                  :placeholder="t('admin.system.mail.sender_name_placeholder')"
                  style="max-width: 420px;"
                />
              </n-form-item>

              <n-form-item :label="t('admin.system.mail.test_connection')">
                <n-button secondary size="small" @click="handleSendTestEmail" :loading="testMailLoading">
                  <template #icon>
                    <n-icon :component="MailOutline" />
                  </template>
                  {{ t('admin.system.mail.send_test') }}
                </n-button>
              </n-form-item>
            </n-form>
          </div>
        </n-tab-pane>

        <!-- Tab 4: 容器环境与动态靶机 -->
        <n-tab-pane name="container" :tab="t('admin.system.tabs.container')">
          <div class="tab-content-container">
            <n-form
              :model="settingsForm"
              label-placement="left"
              label-width="180"
              require-mark-placement="right-hanging"
            >
              <n-form-item :label="t('admin.system.container.port_range')">
                <div class="range-inputs">
                  <n-input-number
                    v-model:value="settingsForm.portRangeStart"
                    :min="1024"
                    :max="65535"
                    style="width: 150px;"
                  />
                  <span class="range-divider">-</span>
                  <n-input-number
                    v-model:value="settingsForm.portRangeEnd"
                    :min="1024"
                    :max="65535"
                    style="width: 150px;"
                  />
                </div>
                <span class="field-hint">{{ t('admin.system.container.port_range_hint') }}</span>
              </n-form-item>
            </n-form>
          </div>
        </n-tab-pane>

        <!-- Tab 5: 平台运维与风控 -->
        <n-tab-pane name="maintenance" :tab="t('admin.system.tabs.maintenance')">
          <div class="tab-content-container">
            <n-alert
              v-if="settingsForm.maintenanceMode"
              type="error"
              :title="t('admin.system.maintenance.alert_title')"
              style="margin-bottom: 20px; max-width: 600px;"
            >
              {{ t('admin.system.maintenance.alert_desc') }}
            </n-alert>

            <n-form
              :model="settingsForm"
              label-placement="left"
              label-width="160"
              require-mark-placement="right-hanging"
            >
              <n-form-item :label="t('admin.system.maintenance.switch')" path="maintenanceMode">
                <n-switch v-model:value="settingsForm.maintenanceMode">
                  <template #checked>ON</template>
                  <template #unchecked>OFF</template>
                </n-switch>
                <span class="field-hint">{{ t('admin.system.maintenance.switch_hint') }}</span>
              </n-form-item>

              <n-form-item :label="t('admin.system.maintenance.notice')" path="maintenanceNotice">
                <n-input
                  v-model:value="settingsForm.maintenanceNotice"
                  type="textarea"
                  :rows="3"
                  :placeholder="t('admin.system.maintenance.notice_placeholder')"
                  style="max-width: 480px;"
                />
              </n-form-item>

              <n-form-item :label="t('admin.system.maintenance.rate_limit_window')" path="rateLimitWindow">
                <n-input-number
                  v-model:value="settingsForm.rateLimitWindow"
                  :min="10"
                  :max="600"
                  style="width: 200px;"
                />
              </n-form-item>

              <n-form-item :label="t('admin.system.maintenance.rate_limit_max_requests')" path="rateLimitMaxRequests">
                <n-input-number
                  v-model:value="settingsForm.rateLimitMaxRequests"
                  :min="10"
                  :max="10000"
                  style="width: 200px;"
                />
              </n-form-item>
            </n-form>
          </div>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  NCard,
  NIcon,
  NTabs,
  NTabPane,
  NForm,
  NFormItem,
  NInput,
  NInputNumber,
  NSelect,
  NSwitch,
  NButton,
  NAlert,
  useMessage,
  type SelectOption
} from 'naive-ui'
import {
  ServerOutline,
  CodeSlashOutline,
  HardwareChipOutline,
  RadioOutline,
  MailOutline
} from '@vicons/ionicons5'
import {
  getAdminSystemOverviewApi,
  getAdminSystemSettingsApi,
  batchUpdateAdminSystemSettingsApi,
  testSmtpConnectionApi
} from '@/api/system'
import type { SystemSettingVO, SystemSettingUpdateDTO, AdminSystemOverviewVO } from '@/types/system'

const { t } = useI18n()
const message = useMessage()
const saveLoading = ref(false)
const testMailLoading = ref(false)

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

const localeOptions = computed<SelectOption[]>(() => [
  { label: t('admin.system.general.locale_zh_cn'), value: 'zh_CN' },
  { label: t('admin.system.general.locale_en_us'), value: 'en_US' }
])

// Settings Form State
const settingsForm = reactive({
  // General
  platformName: 'Volur Lite',
  platformDesc: 'High-Performance Distributed Cybersecurity Competition Platform',
  icpNumber: '',
  defaultLocale: 'zh_CN',
  registrationEnabled: true,
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
  smtpPass: '',
  smtpSsl: true,
  senderName: 'Volur Lite Security Team',
  // Container Infrastructure
  portRangeStart: 20000,
  portRangeEnd: 30000,
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
        if (item.key === 'system.general.default_locale') settingsForm.defaultLocale = item.value
        if (item.key === 'system.captcha.enabled') settingsForm.captchaEnabled = item.value === 'true'
        if (item.key === 'system.captcha.tolerance_px') settingsForm.captchaTolerance = Number(item.value) || 8
        if (item.key === 'system.captcha.ticket_ttl_seconds') settingsForm.captchaTicketTtl = Number(item.value) || 300
        if (item.key === 'system.captcha.max_attempts') settingsForm.captchaMaxAttempts = Number(item.value) || 5
        if (item.key === 'system.captcha.force_reset_captcha') settingsForm.forceCaptchaOnReset = item.value === 'true'
        if (item.key === 'system.mail.smtp_host') settingsForm.smtpHost = item.value
        if (item.key === 'system.mail.smtp_port') settingsForm.smtpPort = Number(item.value) || 465
        if (item.key === 'system.mail.smtp_user') settingsForm.smtpUser = item.value
        if (item.key === 'system.mail.smtp_ssl') settingsForm.smtpSsl = item.value === 'true'
        if (item.key === 'system.mail.sender_name') settingsForm.senderName = item.value
        if (item.key === 'system.container.port_range_start') settingsForm.portRangeStart = Number(item.value) || 20000
        if (item.key === 'system.container.port_range_end') settingsForm.portRangeEnd = Number(item.value) || 30000
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
  saveLoading.value = true
  try {
    const payload: SystemSettingUpdateDTO[] = [
      { key: 'system.general.platform_name', value: settingsForm.platformName },
      { key: 'system.general.platform_desc', value: settingsForm.platformDesc },
      { key: 'system.general.icp_number', value: settingsForm.icpNumber },
      { key: 'system.auth.registration_enabled', value: String(settingsForm.registrationEnabled) },
      { key: 'system.general.default_locale', value: settingsForm.defaultLocale },
      { key: 'system.captcha.enabled', value: String(settingsForm.captchaEnabled) },
      { key: 'system.captcha.tolerance_px', value: String(settingsForm.captchaTolerance) },
      { key: 'system.captcha.ticket_ttl_seconds', value: String(settingsForm.captchaTicketTtl) },
      { key: 'system.captcha.max_attempts', value: String(settingsForm.captchaMaxAttempts) },
      { key: 'system.captcha.force_reset_captcha', value: String(settingsForm.forceCaptchaOnReset) },
      { key: 'system.mail.smtp_host', value: settingsForm.smtpHost },
      { key: 'system.mail.smtp_port', value: String(settingsForm.smtpPort) },
      { key: 'system.mail.smtp_user', value: settingsForm.smtpUser },
      { key: 'system.mail.smtp_ssl', value: String(settingsForm.smtpSsl) },
      { key: 'system.mail.sender_name', value: settingsForm.senderName },
      { key: 'system.container.port_range_start', value: String(settingsForm.portRangeStart) },
      { key: 'system.container.port_range_end', value: String(settingsForm.portRangeEnd) },
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
  settingsForm.smtpPass = ''
  settingsForm.smtpSsl = true
  settingsForm.senderName = 'Volur Lite Security Team'
  settingsForm.portRangeStart = 20000
  settingsForm.portRangeEnd = 30000
  settingsForm.maintenanceMode = false
  settingsForm.maintenanceNotice = '平台底层正在维护，请稍后访问。'
  settingsForm.rateLimitWindow = 60
  settingsForm.rateLimitMaxRequests = 120
  message.info(t('admin.system.messages.reset_info'))
}

async function handleSendTestEmail() {
  testMailLoading.value = true
  try {
    await testSmtpConnectionApi()
    message.success(t('admin.system.messages.test_mail_success'))
  } catch (error: any) {
    message.error(error.message || t('admin.system.messages.test_mail_failed'))
  } finally {
    testMailLoading.value = false
  }
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
  background-color: var(--n-color, #ffffff);

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
          color: #666666;
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
  background-color: var(--n-color, #ffffff);

  .tab-content-container {
    padding: 24px 8px;
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
