<template>
  <div class="space-y-6 max-w-4xl mx-auto">
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h2 class="text-xl font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
          {{ t('admin.risk.settings.title', '风控设置') }}
        </h2>
        <p class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary mt-1">
          {{ t('admin.risk.settings.subtitle', '全局安全防护引擎、IP 黑白名单过滤与自适应人机验证挑战参数') }}
        </p>
      </div>

      <div class="flex items-center gap-2.5">
        <VBtn
          clz="primary"
          size="small"
          :left-icon="SaveOutline"
          :loading="saving"
          @click="handleSave"
        >
          {{ t('common.save', '保存设置') }}
        </VBtn>
      </div>
    </div>

    <div v-if="loading" class="p-12 text-center text-google-blue dark:text-google-blue-dark">
      {{ t('common.loading', '正在加载风控设置...') }}
    </div>

    <div v-else class="space-y-6">
      <div class="p-6 rounded-2xl bg-white dark:bg-[#292a2d] border border-google-border-light dark:border-google-border-dark space-y-5">
        <h3 class="text-sm font-semibold text-google-text-primary dark:text-google-text-darkPrimary border-b border-google-border-light dark:border-google-border-dark pb-3">
          {{ t('admin.risk.settings.risk_engine_card', '风控防护引擎与网络过滤') }}
        </h3>

        <div class="space-y-4 divide-y divide-google-border-light dark:divide-google-border-dark">
          <div class="flex items-center justify-between pt-3 first:pt-0">
            <div>
              <div class="text-sm font-medium text-google-text-primary dark:text-google-text-darkPrimary">
                {{ t('admin.risk.settings.enabled', '启用全局风控防护引擎') }}
              </div>
              <div class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary mt-0.5">
                {{ t('admin.risk.settings.enabled_desc', '开启后全站 API 请求将纳入风控行为评估与动态阻断') }}
              </div>
            </div>
            <input
              v-model="form.enabled"
              type="checkbox"
              class="rounded border-gray-300 text-google-blue focus:ring-google-blue h-5 w-5"
            />
          </div>

          <div class="flex items-center justify-between pt-4">
            <div>
              <div class="text-sm font-medium text-google-text-primary dark:text-google-text-darkPrimary">
                {{ t('admin.risk.settings.ip_filter', '启用 IP 访问拦截与黑白名单') }}
              </div>
              <div class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary mt-0.5">
                {{ t('admin.risk.settings.ip_filter_desc', '拦截黑名单 IP 访问，允许白名单 IP 快速通行') }}
              </div>
            </div>
            <input
              v-model="form.ipFilterEnabled"
              type="checkbox"
              class="rounded border-gray-300 text-google-blue focus:ring-google-blue h-5 w-5"
            />
          </div>

          <div class="flex items-center justify-between pt-4">
            <div>
              <div class="text-sm font-medium text-google-text-primary dark:text-google-text-darkPrimary">
                {{ t('admin.risk.settings.bypass_loopback', '跳过本地与内网回环地址') }}
              </div>
              <div class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary mt-0.5">
                {{ t('admin.risk.settings.bypass_loopback_desc', '自动信任 127.0.0.1、内网 Private IP 与容器内联网络') }}
              </div>
            </div>
            <input
              v-model="form.bypassLoopback"
              type="checkbox"
              class="rounded border-gray-300 text-google-blue focus:ring-google-blue h-5 w-5"
            />
          </div>

          <div class="pt-4 grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
                默认自动封禁时长 (分钟)
              </label>
              <VInput
                v-model:value="banDurationStr"
                placeholder="'1440'"
                clearable
              />
            </div>

            <div>
              <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
                拦截封禁通知文案
              </label>
              <VInput
                v-model:value="form.banNoticeMessage"
                placeholder="'您的 IP 因触发平台安全策略已被临时封禁'"
                clearable
              />
            </div>
          </div>
        </div>
      </div>

      <div class="p-6 rounded-2xl bg-white dark:bg-[#292a2d] border border-google-border-light dark:border-google-border-dark space-y-5">
        <h3 class="text-sm font-semibold text-google-text-primary dark:text-google-text-darkPrimary border-b border-google-border-light dark:border-google-border-dark pb-3">
          {{ t('admin.risk.settings.captcha_card', '自适应人机验证挑战参数') }}
        </h3>

        <div class="space-y-4 divide-y divide-google-border-light dark:divide-google-border-dark">
          <div class="flex items-center justify-between pt-3 first:pt-0">
            <div>
              <div class="text-sm font-medium text-google-text-primary dark:text-google-text-darkPrimary">
                启用自适应滑块人机验证
              </div>
              <div class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary mt-0.5">
                高危场景下要求用户通过魔改 RC4 + RSA 滑块人机验证
              </div>
            </div>
            <input
              v-model="form.captchaEnabled"
              type="checkbox"
              class="rounded border-gray-300 text-google-blue focus:ring-google-blue h-5 w-5"
            />
          </div>

          <div class="flex items-center justify-between pt-4">
            <div>
              <div class="text-sm font-medium text-google-text-primary dark:text-google-text-darkPrimary">
                找回密码强制人机验证
              </div>
              <div class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary mt-0.5">
                重置密码流程中强制要求核验人机安全票据
              </div>
            </div>
            <input
              v-model="form.forceCaptchaOnReset"
              type="checkbox"
              class="rounded border-gray-300 text-google-blue focus:ring-google-blue h-5 w-5"
            />
          </div>

          <div class="pt-4 grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
                滑块验证容差 (像素)
              </label>
              <VInput
                v-model:value="captchaToleranceStr"
                placeholder="'5'"
                clearable
              />
            </div>

            <div>
              <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
                人机票据有效期 (秒)
              </label>
              <VInput
                v-model:value="ticketTtlStr"
                placeholder="'300'"
                clearable
              />
            </div>

            <div>
              <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
                最大允许失败重试次数
              </label>
              <VInput
                v-model:value="maxAttemptsStr"
                placeholder="'5'"
                clearable
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useMessage } from 'naive-ui'
import { SaveOutline } from '@vicons/ionicons5'
import VBtn from '@/components/common/VBtn.vue'
import VInput from '@/components/common/VInput.vue'
import type { AdminRiskSettingsVO } from '@/types/risk'
import { getRiskSettingsApi, updateRiskSettingsApi } from '@/api/risk'

const { t } = useI18n()
const message = useMessage()

const loading = ref(false)
const saving = ref(false)

const form = ref<AdminRiskSettingsVO>({
  enabled: true,
  ipFilterEnabled: true,
  defaultBanDurationMinutes: 1440,
  banNoticeMessage: '您的 IP 触发平台防护规则已被封禁',
  bypassLoopback: true,
  captchaEnabled: true,
  captchaTolerance: 5,
  captchaTicketTtl: 300,
  captchaMaxAttempts: 5,
  forceCaptchaOnReset: true
})

const banDurationStr = computed<string>({
  get: () => String(form.value.defaultBanDurationMinutes || '1440'),
  set: (val) => {
    form.value.defaultBanDurationMinutes = parseInt(val, 10) || 1440
  }
})

const captchaToleranceStr = computed<string>({
  get: () => String(form.value.captchaTolerance || '5'),
  set: (val) => {
    form.value.captchaTolerance = parseInt(val, 10) || 5
  }
})

const ticketTtlStr = computed<string>({
  get: () => String(form.value.captchaTicketTtl || '300'),
  set: (val) => {
    form.value.captchaTicketTtl = parseInt(val, 10) || 300
  }
})

const maxAttemptsStr = computed<string>({
  get: () => String(form.value.captchaMaxAttempts || '5'),
  set: (val) => {
    form.value.captchaMaxAttempts = parseInt(val, 10) || 5
  }
})

async function fetchSettings() {
  loading.value = true
  try {
    const res = await getRiskSettingsApi()
    if (res.data) {
      form.value = res.data
    }
  } catch (err: any) {
    message.error(err.message || '获取风控设置失败')
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    await updateRiskSettingsApi(form.value)
    message.success('风控设置保存成功')
    await fetchSettings()
  } catch (err: any) {
    message.error(err.message || '保存风控设置失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  fetchSettings()
})
</script>
