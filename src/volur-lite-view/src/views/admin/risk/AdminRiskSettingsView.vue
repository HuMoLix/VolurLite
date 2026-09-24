<template>
  <div class="admin-risk-settings-view">
    <div class="view-header">
      <div class="header-left">
        <h2 class="view-title">{{ t('admin.risk.settings.title') }}</h2>
        <p class="view-subtitle">{{ t('admin.risk.settings.subtitle') }}</p>
      </div>
    </div>

    <n-card class="settings-card" :bordered="false">
      <n-spin :show="loading">
        <n-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-placement="top"
          class="settings-form"
        >
          <!-- Section 1: 智能风控引擎与网络防御 -->
          <div class="section-title-box">
            <h3 class="section-title">{{ t('admin.risk.settings.risk_engine_card') }}</h3>
          </div>

          <div class="setting-row">
            <div class="setting-meta">
              <span class="setting-label">{{ t('admin.risk.settings.enabled') }}</span>
              <span class="setting-desc">{{ t('admin.risk.settings.enabled_desc') }}</span>
            </div>
            <div class="setting-control">
              <n-switch v-model:value="form.enabled" />
            </div>
          </div>

          <n-divider class="row-divider" />

          <div class="setting-row">
            <div class="setting-meta">
              <span class="setting-label">{{ t('admin.risk.settings.ip_filter') }}</span>
              <span class="setting-desc">{{ t('admin.risk.settings.ip_filter_desc') }}</span>
            </div>
            <div class="setting-control">
              <n-switch v-model:value="form.ipFilterEnabled" />
            </div>
          </div>

          <n-divider class="row-divider" />

          <div class="setting-row">
            <div class="setting-meta">
              <span class="setting-label">{{ t('admin.risk.settings.bypass_loopback') }}</span>
              <span class="setting-desc">{{ t('admin.risk.settings.bypass_loopback_desc') }}</span>
            </div>
            <div class="setting-control">
              <n-switch v-model:value="form.bypassLoopback" />
            </div>
          </div>

          <n-divider class="row-divider" />

          <div class="setting-row form-input-row">
            <div class="setting-meta">
              <span class="setting-label">{{ t('admin.risk.settings.default_duration') }}</span>
              <span class="setting-desc">{{ t('admin.risk.settings.default_duration_desc') }}</span>
            </div>
            <div class="setting-control input-control">
              <n-input-number
                v-model:value="form.defaultBanDurationMinutes"
                :min="1"
                :max="525600"
                :step="60"
                class="duration-number-input"
              >
                <template #suffix>{{ t('common.minute', '分钟') }}</template>
              </n-input-number>
            </div>
          </div>

          <n-divider class="row-divider" />

          <div class="setting-row form-input-row">
            <div class="setting-meta">
              <span class="setting-label">{{ t('admin.risk.settings.ban_notice') }}</span>
              <span class="setting-desc">{{ t('admin.risk.settings.ban_notice_desc') }}</span>
            </div>
            <div class="setting-control textarea-control">
              <n-input
                v-model:value="form.banNoticeMessage"
                type="textarea"
                :rows="3"
                maxlength="255"
                show-count
                placeholder="请输入封禁提示信息"
              />
            </div>
          </div>

          <!-- Section 2: 人机验证与行为挑战 -->
          <div class="section-title-box" style="margin-top: 24px;">
            <h3 class="section-title">{{ t('admin.risk.settings.captcha_card') }}</h3>
          </div>

          <div class="setting-row">
            <div class="setting-meta">
              <span class="setting-label">{{ t('admin.risk.settings.captcha_enabled') }}</span>
              <span class="setting-desc">{{ t('admin.risk.settings.captcha_enabled_desc') }}</span>
            </div>
            <div class="setting-control">
              <n-switch v-model:value="form.captchaEnabled" />
            </div>
          </div>

          <n-divider class="row-divider" />

          <div class="setting-row form-input-row">
            <div class="setting-meta">
              <span class="setting-label">{{ t('admin.risk.settings.captcha_tolerance') }}</span>
              <span class="setting-desc">{{ t('admin.risk.settings.captcha_tolerance_desc') }}</span>
            </div>
            <div class="setting-control input-control">
              <n-input-number
                v-model:value="form.captchaTolerance"
                :min="2"
                :max="25"
                class="duration-number-input"
              >
                <template #suffix>px</template>
              </n-input-number>
            </div>
          </div>

          <n-divider class="row-divider" />

          <div class="setting-row form-input-row">
            <div class="setting-meta">
              <span class="setting-label">{{ t('admin.risk.settings.captcha_ticket_ttl') }}</span>
              <span class="setting-desc">{{ t('admin.risk.settings.captcha_ticket_ttl_desc') }}</span>
            </div>
            <div class="setting-control input-control">
              <n-input-number
                v-model:value="form.captchaTicketTtl"
                :min="60"
                :max="1800"
                :step="30"
                class="duration-number-input"
              >
                <template #suffix>{{ t('common.second', '秒') }}</template>
              </n-input-number>
            </div>
          </div>

          <n-divider class="row-divider" />

          <div class="setting-row form-input-row">
            <div class="setting-meta">
              <span class="setting-label">{{ t('admin.risk.settings.captcha_max_attempts') }}</span>
              <span class="setting-desc">{{ t('admin.risk.settings.captcha_max_attempts_desc') }}</span>
            </div>
            <div class="setting-control input-control">
              <n-input-number
                v-model:value="form.captchaMaxAttempts"
                :min="3"
                :max="20"
                class="duration-number-input"
              >
                <template #suffix>{{ t('common.times', '次') }}</template>
              </n-input-number>
            </div>
          </div>

          <n-divider class="row-divider" />

          <div class="setting-row">
            <div class="setting-meta">
              <span class="setting-label">{{ t('admin.risk.settings.captcha_force_reset') }}</span>
              <span class="setting-desc">{{ t('admin.risk.settings.captcha_force_reset_desc') }}</span>
            </div>
            <div class="setting-control">
              <n-switch v-model:value="form.forceCaptchaOnReset" />
            </div>
          </div>
        </n-form>

        <div class="action-footer">
          <n-button quaternary @click="fetchSettings">
            {{ t('admin.risk.settings.reset_btn') }}
          </n-button>
          <n-button
            type="primary"
            class="theme-primary-btn"
            :loading="saveLoading"
            @click="handleSave"
          >
            {{ t('admin.risk.settings.save_btn') }}
          </n-button>
        </div>
      </n-spin>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  NCard,
  NSpin,
  NForm,
  NSwitch,
  NInputNumber,
  NInput,
  NDivider,
  NButton,
  useMessage,
  type FormInst,
  type FormRules
} from 'naive-ui'
import { useI18n } from 'vue-i18n'
import { getRiskSettingsApi, updateRiskSettingsApi } from '@/api/risk'
import type { AdminRiskSettingsDTO } from '@/types/risk'

const { t } = useI18n()
const message = useMessage()

const loading = ref(false)
const saveLoading = ref(false)
const formRef = ref<FormInst | null>(null)

const form = ref<AdminRiskSettingsDTO>({
  enabled: true,
  ipFilterEnabled: true,
  defaultBanDurationMinutes: 1440,
  banNoticeMessage: '您的 IP 存在异常访问行为，已被系统安全机制拦截。如需解封请联系平台管理员。',
  bypassLoopback: true,
  captchaEnabled: true,
  captchaTolerance: 8,
  captchaTicketTtl: 300,
  captchaMaxAttempts: 5,
  forceCaptchaOnReset: true
})

const rules: FormRules = {
  defaultBanDurationMinutes: [
    { required: true, type: 'number', message: '封禁时长不能为空', trigger: ['blur', 'change'] }
  ],
  banNoticeMessage: [
    { required: true, message: '拦截提示信息不能为空', trigger: ['blur', 'input'] }
  ],
  captchaTolerance: [
    { required: true, type: 'number', message: '容差阈值不能为空', trigger: ['blur', 'change'] }
  ],
  captchaTicketTtl: [
    { required: true, type: 'number', message: '凭证有效时长不能为空', trigger: ['blur', 'change'] }
  ],
  captchaMaxAttempts: [
    { required: true, type: 'number', message: '最大尝试次数不能为空', trigger: ['blur', 'change'] }
  ]
}

async function fetchSettings() {
  loading.value = true
  try {
    const res = await getRiskSettingsApi()
    if (res.data) {
      form.value = {
        enabled: res.data.enabled,
        ipFilterEnabled: res.data.ipFilterEnabled,
        defaultBanDurationMinutes: res.data.defaultBanDurationMinutes,
        banNoticeMessage: res.data.banNoticeMessage,
        bypassLoopback: res.data.bypassLoopback,
        captchaEnabled: res.data.captchaEnabled ?? true,
        captchaTolerance: res.data.captchaTolerance ?? 8,
        captchaTicketTtl: res.data.captchaTicketTtl ?? 300,
        captchaMaxAttempts: res.data.captchaMaxAttempts ?? 5,
        forceCaptchaOnReset: res.data.forceCaptchaOnReset ?? true
      }
    }
  } catch (err: any) {
    message.error(err?.message || '获取风控设置失败')
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  saveLoading.value = true
  try {
    await updateRiskSettingsApi(form.value)
    message.success(t('admin.risk.settings.save_success'))
  } catch (err: any) {
    message.error(err?.message || '更新风控设置失败')
  } finally {
    saveLoading.value = false
  }
}

onMounted(() => {
  fetchSettings()
})
</script>

<style scoped lang="scss">
.admin-risk-settings-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.view-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .view-title {
    font-size: 20px;
    font-weight: 700;
    color: var(--v-text-primary, #111111);
    margin: 0;
  }

  .view-subtitle {
    font-size: 13px;
    color: var(--v-text-desc, #444444);
    margin: 4px 0 0 0;
  }
}

.settings-card {
  background-color: var(--v-bg-card, #ffffff);
  border-radius: 12px;
  border: 1px solid var(--v-border-base, #e5e4e7);
  box-shadow: var(--v-shadow-sm, 0 1px 3px rgba(0, 0, 0, 0.03));
  padding: 8px 16px;
}

.settings-form {
  display: flex;
  flex-direction: column;

  .section-title-box {
    padding: 12px 0 8px 0;
    border-bottom: 2px solid var(--v-border-base, #efeff5);
    margin-bottom: 6px;

    .section-title {
      font-size: 15px;
      font-weight: 700;
      color: var(--v-text-primary, #111111);
      margin: 0;
    }
  }
}

.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 0;
  gap: 24px;

  &.form-input-row {
    align-items: flex-start;
  }

  .setting-meta {
    display: flex;
    flex-direction: column;
    gap: 4px;
    flex: 1;

    .setting-label {
      font-size: 14px;
      font-weight: 600;
      color: var(--v-text-primary, #111111);
    }

    .setting-desc {
      font-size: 13px;
      color: var(--v-text-desc, #444444);
      line-height: 1.5;
    }
  }

  .setting-control {
    display: flex;
    justify-content: flex-end;
    align-items: center;

    &.input-control {
      width: 260px;
    }

    &.textarea-control {
      width: 420px;
    }

    .duration-number-input {
      width: 100%;
    }
  }
}

.row-divider {
  margin: 0;
  border-color: var(--v-border-base, #e5e4e7);
  opacity: 0.6;
}

.action-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--v-border-base, #e5e4e7);

  .theme-primary-btn {
    background-color: #000000;
    color: #ffffff;
    border-radius: 6px;
    font-weight: 600;
    padding: 0 20px;
    transition: opacity 0.2s ease;

    &:hover {
      opacity: 0.88;
    }
  }
}

@media (max-width: 768px) {
  .setting-row {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;

    .setting-control {
      justify-content: flex-start;

      &.input-control,
      &.textarea-control {
        width: 100%;
      }
    }
  }
}
</style>
