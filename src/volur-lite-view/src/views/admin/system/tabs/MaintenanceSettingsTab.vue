<template>
  <div class="tab-content-container">
    <n-alert
      v-if="form.maintenanceMode"
      type="error"
      :title="t('admin.system.maintenance.alert_title')"
      style="margin-bottom: 20px; max-width: 600px;"
    >
      {{ t('admin.system.maintenance.alert_desc') }}
    </n-alert>

    <n-form
      :model="form"
      label-placement="left"
      label-width="160"
      require-mark-placement="right-hanging"
    >
      <n-form-item :label="t('admin.system.maintenance.switch')" path="maintenanceMode">
        <n-switch v-model:value="form.maintenanceMode">
          <template #checked>ON</template>
          <template #unchecked>OFF</template>
        </n-switch>
        <span class="field-hint">{{ t('admin.system.maintenance.switch_hint') }}</span>
      </n-form-item>

      <n-form-item :label="t('admin.system.maintenance.notice')" path="maintenanceNotice">
        <n-input
          v-model:value="form.maintenanceNotice"
          type="textarea"
          :rows="3"
          :placeholder="t('admin.system.maintenance.notice_placeholder')"
          style="max-width: 480px;"
        />
      </n-form-item>

      <n-form-item :label="t('admin.system.maintenance.rate_limit_window')" path="rateLimitWindow">
        <n-input-number
          v-model:value="form.rateLimitWindow"
          :min="10"
          :max="600"
          style="width: 200px;"
        />
      </n-form-item>

      <n-form-item :label="t('admin.system.maintenance.rate_limit_max_requests')" path="rateLimitMaxRequests">
        <n-input-number
          v-model:value="form.rateLimitMaxRequests"
          :min="10"
          :max="10000"
          style="width: 200px;"
        />
      </n-form-item>
    </n-form>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import {
  NForm,
  NFormItem,
  NInput,
  NInputNumber,
  NSwitch,
  NAlert
} from 'naive-ui'

interface MaintenanceSettings {
  maintenanceMode: boolean
  maintenanceNotice: string
  rateLimitWindow: number
  rateLimitMaxRequests: number
}

defineProps<{
  form: MaintenanceSettings
}>()

const { t } = useI18n()
</script>

<style scoped lang="scss">
.tab-content-container {
  padding: 16px 8px;

  .field-hint {
    margin-left: 12px;
    font-size: 12px;
    color: #444444;
  }
}
</style>
