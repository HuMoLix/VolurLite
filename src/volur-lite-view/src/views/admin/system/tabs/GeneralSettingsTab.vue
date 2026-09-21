<template>
  <div class="tab-content-container">
    <n-form
      :model="form"
      label-placement="left"
      label-width="160"
      require-mark-placement="right-hanging"
    >
      <n-form-item :label="t('admin.system.general.platform_name')" path="platformName">
        <n-input
          v-model:value="form.platformName"
          :placeholder="t('admin.system.general.platform_name_placeholder')"
          style="max-width: 480px;"
        />
      </n-form-item>

      <n-form-item :label="t('admin.system.general.platform_desc')" path="platformDesc">
        <n-input
          v-model:value="form.platformDesc"
          type="textarea"
          :rows="2"
          :placeholder="t('admin.system.general.platform_desc_placeholder')"
          style="max-width: 480px;"
        />
      </n-form-item>

      <n-form-item :label="t('admin.system.general.icp_number')" path="icpNumber">
        <n-input
          v-model:value="form.icpNumber"
          :placeholder="t('admin.system.general.icp_placeholder')"
          style="max-width: 480px;"
        />
      </n-form-item>

      <n-form-item :label="t('admin.system.general.default_locale')" path="defaultLocale">
        <n-select
          v-model:value="form.defaultLocale"
          :options="localeOptions"
          style="max-width: 260px;"
        />
      </n-form-item>

      <n-form-item :label="t('admin.system.general.registration_enabled')" path="registrationEnabled">
        <div class="switch-row">
          <n-switch v-model:value="form.registrationEnabled" />
          <span class="field-hint">{{ t('admin.system.general.registration_hint') }}</span>
        </div>
      </n-form-item>

      <n-form-item :label="t('admin.system.general.require_email_verification')" path="requireEmailVerification">
        <div class="switch-row">
          <n-switch v-model:value="form.requireEmailVerification" />
          <span class="field-hint">{{ t('admin.system.general.require_email_verification_hint') }}</span>
        </div>
      </n-form-item>
    </n-form>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  NForm,
  NFormItem,
  NInput,
  NSelect,
  NSwitch,
  type SelectOption
} from 'naive-ui'

interface GeneralSettings {
  platformName: string
  platformDesc: string
  icpNumber: string
  defaultLocale: string
  registrationEnabled: boolean
  requireEmailVerification: boolean
}

defineProps<{
  form: GeneralSettings
}>()

const { t } = useI18n()

const localeOptions = computed<SelectOption[]>(() => [
  { label: t('admin.system.general.locale_zh_cn'), value: 'zh_CN' },
  { label: t('admin.system.general.locale_en_us'), value: 'en_US' }
])
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
