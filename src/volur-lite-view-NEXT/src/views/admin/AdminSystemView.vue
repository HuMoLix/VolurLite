<template>
  <div class="admin-system-view flex flex-col gap-6">
    <div class="overview-card p-6 rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark transition-colors">
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div class="flex items-center gap-3">
          <div class="w-11 h-11 rounded-xl flex items-center justify-center bg-blue-50 dark:bg-blue-900/30 text-google-blue dark:text-google-blue-dark shrink-0">
            <n-icon size="22">
              <ServerOutline />
            </n-icon>
          </div>
          <div class="min-w-0">
            <span class="text-xs text-google-text-muted dark:text-google-text-darkMuted block">
              {{ t('admin.system.overview.kernel_version', '系统内核版本') }}
            </span>
            <span class="text-sm font-semibold text-google-text-primary dark:text-google-text-darkPrimary truncate block">
              {{ overview.kernelVersion || 'Volur Lite v1.0.0-SNAPSHOT' }}
            </span>
          </div>
        </div>

        <div class="flex items-center gap-3">
          <div class="w-11 h-11 rounded-xl flex items-center justify-center bg-cyan-50 dark:bg-cyan-900/30 text-cyan-600 dark:text-cyan-400 shrink-0">
            <n-icon size="22">
              <CodeSlashOutline />
            </n-icon>
          </div>
          <div class="min-w-0">
            <span class="text-xs text-google-text-muted dark:text-google-text-darkMuted block">
              {{ t('admin.system.overview.runtime_env', '运行底层环境') }}
            </span>
            <span class="text-sm font-semibold text-google-text-primary dark:text-google-text-darkPrimary truncate block">
              {{ overview.runtimeEnv || 'Spring Boot 3.4.3 / JDK 25' }}
            </span>
          </div>
        </div>

        <div class="flex items-center gap-3">
          <div class="w-11 h-11 rounded-xl flex items-center justify-center bg-green-50 dark:bg-green-900/30 text-google-green dark:text-google-green-dark shrink-0">
            <n-icon size="22">
              <HardwareChipOutline />
            </n-icon>
          </div>
          <div class="min-w-0">
            <span class="text-xs text-google-text-muted dark:text-google-text-darkMuted block">
              {{ t('admin.system.overview.redis_status', 'Redis 缓存状态') }}
            </span>
            <span class="text-sm font-semibold text-google-green dark:text-google-green-dark truncate block">
              {{ overview.redisStatus === 'CONNECTED' ? t('admin.system.overview.redis_connected', { latency: overview.redisLatencyMs }) : t('admin.system.overview.redis_disconnected') }}
            </span>
          </div>
        </div>

        <div class="flex items-center gap-3">
          <div class="w-11 h-11 rounded-xl flex items-center justify-center bg-amber-50 dark:bg-amber-900/30 text-google-yellow dark:text-google-yellow-dark shrink-0">
            <n-icon size="22">
              <RadioOutline />
            </n-icon>
          </div>
          <div class="min-w-0">
            <span class="text-xs text-google-text-muted dark:text-google-text-darkMuted block">
              {{ t('admin.system.overview.sse_status', '实时 SSE 推送总线') }}
            </span>
            <span class="text-sm font-semibold text-google-blue dark:text-google-blue-dark truncate block">
              {{ overview.sseStatus === 'ONLINE' ? t('admin.system.overview.sse_online') : t('admin.system.overview.sse_offline') }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <div class="settings-card p-6 md:p-8 rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark transition-colors">
      <n-tabs type="line" animated>
        <n-tab-pane name="general" :tab="t('admin.system.tabs.general', '平台基础设置')">
          <div class="flex flex-col gap-5 pt-4 max-w-2xl">
            <div class="form-item flex flex-col gap-1.5">
              <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
                {{ t('admin.system.general.platform_name', '平台全称') }}
              </label>
              <VInput
                v-model:value="settingsForm.platformName"
                :placeholder="t('admin.system.general.platform_name_placeholder', '请输入平台系统全称')"
              />
            </div>

            <div class="form-item flex flex-col gap-1.5">
              <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
                {{ t('admin.system.general.platform_desc', '平台描述与标语') }}
              </label>
              <VInput
                v-model:value="settingsForm.platformDesc"
                type="textarea"
                :rows="2"
                :placeholder="t('admin.system.general.platform_desc_placeholder', '请输入展示在首页与页脚的平台简述')"
              />
            </div>

            <div class="form-item flex flex-col gap-1.5">
              <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
                {{ t('admin.system.general.icp_number', 'ICP 备案号') }}
              </label>
              <VInput
                v-model:value="settingsForm.icpNumber"
                :placeholder="t('admin.system.general.icp_placeholder', '如：京ICP备XXXXXXXX号')"
              />
            </div>

            <div class="form-item flex flex-col gap-1.5">
              <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
                {{ t('admin.system.general.default_locale', '默认展示语言') }}
              </label>
              <VSelection
                v-model:value="settingsForm.defaultLocale"
                :options="localeOptions"
              />
            </div>

            <div class="flex items-center justify-between p-4 rounded-xl bg-google-surface-light dark:bg-google-surface-darkElevated">
              <div>
                <div class="text-sm font-medium text-google-text-primary dark:text-google-text-darkPrimary">
                  {{ t('admin.system.general.registration_enabled', '开放新用户注册') }}
                </div>
                <div class="text-xs text-google-text-muted dark:text-google-text-darkMuted mt-0.5">
                  {{ t('admin.system.general.registration_hint', '开启后允许普通访客在前台注册新账号；关闭时仅限管理员在后台创建') }}
                </div>
              </div>
              <n-switch v-model:value="settingsForm.registrationEnabled" />
            </div>

            <div class="flex items-center justify-between p-4 rounded-xl bg-google-surface-light dark:bg-google-surface-darkElevated">
              <div>
                <div class="text-sm font-medium text-google-text-primary dark:text-google-text-darkPrimary">
                  {{ t('admin.system.general.require_email_verification', '注册强制邮箱验证') }}
                </div>
                <div class="text-xs text-google-text-muted dark:text-google-text-darkMuted mt-0.5">
                  {{ t('admin.system.general.require_email_verification_hint', '开启后新用户注册必须验证邮箱验证码') }}
                </div>
              </div>
              <n-switch v-model:value="settingsForm.requireEmailVerification" />
            </div>
          </div>
        </n-tab-pane>

        <n-tab-pane name="mail" :tab="t('admin.system.tabs.mail', '邮件服务配置 (SMTP)')">
          <div class="flex flex-col gap-5 pt-4 max-w-2xl">
            <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div class="sm:col-span-2 form-item flex flex-col gap-1.5">
                <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
                  {{ t('admin.system.mail.host', 'SMTP 服务器主机') }}
                </label>
                <VInput
                  v-model:value="settingsForm.smtpHost"
                  :placeholder="t('admin.system.mail.host_placeholder', '如：smtp.example.com')"
                />
              </div>

              <div class="form-item flex flex-col gap-1.5">
                <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
                  {{ t('admin.system.mail.port', 'SMTP 服务端口') }}
                </label>
                <VInput
                  v-model:value="settingsFormPortStr"
                  placeholder="465"
                />
              </div>
            </div>

            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div class="form-item flex flex-col gap-1.5">
                <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
                  {{ t('admin.system.mail.user', '发信人邮箱账号') }}
                </label>
                <VInput
                  v-model:value="settingsForm.smtpUser"
                  :placeholder="t('admin.system.mail.user_placeholder', '如：service@volur.local')"
                />
              </div>

              <div class="form-item flex flex-col gap-1.5">
                <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
                  {{ t('admin.system.mail.from_address', '发件人邮箱地址') }}
                </label>
                <VInput
                  v-model:value="settingsForm.fromAddress"
                  :placeholder="t('admin.system.mail.from_address_placeholder', '如：noreply@volur.local')"
                />
              </div>
            </div>

            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div class="form-item flex flex-col gap-1.5">
                <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
                  {{ t('admin.system.mail.pass', '授权密码 / Token') }}
                </label>
                <VInput
                  v-model:value="settingsForm.smtpPass"
                  type="password"
                  :placeholder="t('admin.system.mail.pass_placeholder', '请输入邮箱授权码或 SMTP 密码')"
                />
              </div>

              <div class="form-item flex flex-col gap-1.5">
                <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
                  {{ t('admin.system.mail.sender_name', '发件人显示昵称') }}
                </label>
                <VInput
                  v-model:value="settingsForm.senderName"
                  :placeholder="t('admin.system.mail.sender_name_placeholder', '如：Volur Lite 安全演练平台')"
                />
              </div>
            </div>

            <div class="flex items-center justify-between p-4 rounded-xl bg-google-surface-light dark:bg-google-surface-darkElevated">
              <div>
                <div class="text-sm font-medium text-google-text-primary dark:text-google-text-darkPrimary">
                  {{ t('admin.system.mail.ssl', 'SSL / TLS 加密传输') }}
                </div>
                <div class="text-xs text-google-text-muted dark:text-google-text-darkMuted mt-0.5">
                  {{ t('admin.system.mail.ssl_hint', '开启 SSL/TLS 安全加密传输（端口 465 建议开启）') }}
                </div>
              </div>
              <n-switch v-model:value="settingsForm.smtpSsl" />
            </div>

            <div class="flex items-center gap-3 pt-2">
              <VBtn
                clz="normal"
                size="medium"
                :loading="testMailLoading"
                :left-icon="MailOutline"
                @click="handleTestMail"
              >
                {{ t('admin.system.mail.test_connection', '测试 SMTP 连通性') }}
              </VBtn>
            </div>
          </div>
        </n-tab-pane>

        <n-tab-pane name="maintenance" :tab="t('admin.system.tabs.maintenance', '平台运维模式')">
          <div class="flex flex-col gap-5 pt-4 max-w-2xl">
            <div class="flex items-center justify-between p-4 rounded-xl border border-amber-200 dark:border-amber-800/40 bg-amber-50/50 dark:bg-amber-900/10">
              <div>
                <div class="text-sm font-semibold text-google-yellow dark:text-google-yellow-dark">
                  {{ t('admin.system.maintenance.alert_title', '平台全站维护模式') }}
                </div>
                <div class="text-xs text-google-text-desc dark:text-google-text-darkDesc mt-0.5">
                  {{ t('admin.system.maintenance.alert_desc', '开启后非管理员用户将无法访问竞赛与提交 Flag，请谨慎操作。') }}
                </div>
              </div>
              <n-switch v-model:value="settingsForm.maintenanceMode" />
            </div>

            <div class="form-item flex flex-col gap-1.5">
              <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
                {{ t('admin.system.maintenance.notice', '维护公告通知内容') }}
              </label>
              <VInput
                v-model:value="settingsForm.maintenanceNotice"
                type="textarea"
                :rows="3"
                :placeholder="t('admin.system.maintenance.notice_placeholder', '如：平台正在进行底层集群维护升级，预计将于 04:00 恢复开放。')"
              />
            </div>
          </div>
        </n-tab-pane>
      </n-tabs>

      <div class="flex items-center justify-end gap-3 pt-8 mt-6 border-t border-google-border-light dark:border-google-border-dark">
        <VBtn
          clz="normal"
          size="medium"
          @click="handleResetDefault"
        >
          {{ t('admin.system.actions.reset_default', '重置为默认值') }}
        </VBtn>
        <VBtn
          clz="primary"
          size="medium"
          :loading="saveLoading"
          @click="handleSaveSettings"
        >
          {{ t('admin.system.actions.save_all', '保存所有配置') }}
        </VBtn>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  NIcon,
  NTabs,
  NTabPane,
  NSwitch,
  useMessage
} from 'naive-ui'
import {
  ServerOutline,
  CodeSlashOutline,
  HardwareChipOutline,
  RadioOutline,
  MailOutline
} from '@vicons/ionicons5'
import VInput from '@/components/common/VInput.vue'
import VSelection from '@/components/common/VSelection.vue'
import VBtn from '@/components/common/VBtn.vue'
import {
  getAdminSystemOverviewApi,
  getAdminSystemSettingsApi,
  batchUpdateAdminSystemSettingsApi,
  testSmtpConnectionApi
} from '@/api/system'
import type { SystemSettingVO, SystemSettingUpdateDTO, AdminSystemOverviewVO } from '@/types/system'

defineOptions({
  name: 'AdminSystemView'
})

const { t } = useI18n()
const message = useMessage()
const saveLoading = ref(false)
const testMailLoading = ref(false)

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

const settingsForm = reactive({
  platformName: 'Volur Lite',
  platformDesc: 'High-Performance Distributed Cybersecurity Competition Platform',
  icpNumber: '',
  defaultLocale: 'zh-CN',
  registrationEnabled: true,
  requireEmailVerification: true,
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
  maintenanceMode: false,
  maintenanceNotice: '平台底层正在维护，请稍后访问。'
})

const settingsFormPortStr = computed<string>({
  get() {
    return String(settingsForm.smtpPort || '')
  },
  set(val: string) {
    settingsForm.smtpPort = Number(val) || 465
  }
})

const localeOptions = [
  { label: '简体中文 (zh-CN)', value: 'zh-CN' },
  { label: '繁體中文 (zh-TW)', value: 'zh-TW' },
  { label: 'English (en-US)', value: 'en-US' }
]

async function fetchOverview() {
  try {
    const res = await getAdminSystemOverviewApi()
    if (res.data) {
      Object.assign(overview, res.data)
    }
  } catch {}
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
      })
    }
  } catch {}
}

async function handleTestMail() {
  testMailLoading.value = true
  try {
    await testSmtpConnectionApi()
    message.success(t('admin.system.messages.test_mail_success', '邮件服务器连通性测试成功'))
  } catch (err: any) {
    message.error(err?.message || t('admin.system.messages.test_mail_failed', 'SMTP 连接测试失败'))
  } finally {
    testMailLoading.value = false
  }
}

async function handleSaveSettings() {
  if (!settingsForm.smtpHost?.trim()) {
    message.error(t('admin.system.messages.smtp_host_required', 'SMTP 服务器主机不能为空'))
    return
  }
  if (!settingsForm.smtpUser?.trim()) {
    message.error(t('admin.system.messages.smtp_user_required', '发信人邮箱账号不能为空'))
    return
  }
  if (!settingsForm.fromAddress?.trim()) {
    message.error(t('admin.system.messages.from_address_required', '发件人邮箱地址不能为空'))
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
      { key: 'system.maintenance.notice_message', value: settingsForm.maintenanceNotice }
    ]

    if (settingsForm.smtpPass) {
      payload.push({ key: 'system.mail.smtp_pass', value: settingsForm.smtpPass })
    }

    await batchUpdateAdminSystemSettingsApi(payload)
    message.success(t('admin.system.messages.save_success', '系统配置参数已成功保存'))
    await fetchSettings()
    await fetchOverview()
  } catch (error: any) {
    message.error(error.message || t('admin.system.messages.save_failed', '保存设置失败'))
  } finally {
    saveLoading.value = false
  }
}

function handleResetDefault() {
  settingsForm.platformName = 'Volur Lite'
  settingsForm.platformDesc = 'High-Performance Distributed Cybersecurity Competition Platform'
  settingsForm.icpNumber = ''
  settingsForm.defaultLocale = 'zh-CN'
  settingsForm.registrationEnabled = true
  settingsForm.requireEmailVerification = true
  settingsForm.smtpHost = 'smtp.example.com'
  settingsForm.smtpPort = 465
  settingsForm.smtpUser = 'noreply@volur.local'
  settingsForm.fromAddress = 'noreply@volur.local'
  settingsForm.smtpPass = ''
  settingsForm.smtpSsl = true
  settingsForm.senderName = 'Volur Lite Security Team'
  settingsForm.maintenanceMode = false
  settingsForm.maintenanceNotice = '平台底层正在维护，请稍后访问。'
  message.info(t('admin.system.messages.reset_info', '已恢复为系统推荐默认参数'))
}

onMounted(() => {
  fetchOverview()
  fetchSettings()
})
</script>
