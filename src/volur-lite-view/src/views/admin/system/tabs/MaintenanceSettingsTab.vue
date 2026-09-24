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
        <div class="switch-row">
          <n-switch v-model:value="form.maintenanceMode" />
          <span class="field-hint">{{ t('admin.system.maintenance.switch_hint') }}</span>
        </div>
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
    </n-form>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import {
  NForm,
  NFormItem,
  NInput,
  NSwitch,
  NAlert
} from 'naive-ui'

interface MaintenanceSettings {
  maintenanceMode: boolean
  maintenanceNotice: string
}

defineProps<{
  form: MaintenanceSettings
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
