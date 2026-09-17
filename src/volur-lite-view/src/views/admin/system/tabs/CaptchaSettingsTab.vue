<template>
  <div class="tab-content-container">
    <n-form
      :model="form"
      label-placement="left"
      label-width="180"
      require-mark-placement="right-hanging"
    >
      <n-form-item :label="t('admin.system.captcha.enabled')" path="captchaEnabled">
        <div class="switch-row">
          <n-switch v-model:value="form.captchaEnabled" />
          <span class="field-hint">{{ t('admin.system.captcha.enabled_hint') }}</span>
        </div>
      </n-form-item>

      <n-form-item :label="t('admin.system.captcha.tolerance')" path="captchaTolerance">
        <n-input-number
          v-model:value="form.captchaTolerance"
          :min="2"
          :max="25"
          style="width: 220px;"
        />
        <span class="field-hint">{{ t('admin.system.captcha.tolerance_hint') }}</span>
      </n-form-item>

      <n-form-item :label="t('admin.system.captcha.ticket_ttl')" path="captchaTicketTtl">
        <n-input-number
          v-model:value="form.captchaTicketTtl"
          :min="60"
          :max="1800"
          style="width: 220px;"
        />
        <span class="field-hint">{{ t('admin.system.captcha.ticket_ttl_hint') }}</span>
      </n-form-item>

      <n-form-item :label="t('admin.system.captcha.max_attempts')" path="captchaMaxAttempts">
        <n-input-number
          v-model:value="form.captchaMaxAttempts"
          :min="3"
          :max="20"
          style="width: 220px;"
        />
        <span class="field-hint">{{ t('admin.system.captcha.max_attempts_hint') }}</span>
      </n-form-item>

      <n-form-item :label="t('admin.system.captcha.force_reset')" path="forceCaptchaOnReset">
        <div class="switch-row">
          <n-switch v-model:value="form.forceCaptchaOnReset" />
          <span class="field-hint">{{ t('admin.system.captcha.force_reset_hint') }}</span>
        </div>
      </n-form-item>
    </n-form>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import {
  NForm,
  NFormItem,
  NInputNumber,
  NSwitch
} from 'naive-ui'

interface CaptchaSettings {
  captchaEnabled: boolean
  captchaTolerance: number
  captchaTicketTtl: number
  captchaMaxAttempts: number
  forceCaptchaOnReset: boolean
}

defineProps<{
  form: CaptchaSettings
}>()

const { t } = useI18n()
</script>

<style scoped lang="scss">
.tab-content-container {
  padding: 16px 8px;

  .switch-row {
    display: flex;
    align-items: center;
  }

  .field-hint {
    margin-left: 12px;
    font-size: 12px;
    color: #444444;
  }
}
</style>
