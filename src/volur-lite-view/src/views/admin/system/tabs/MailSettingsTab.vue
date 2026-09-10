<template>
  <div class="tab-content-container">
    <n-form
      :model="form"
      label-placement="left"
      label-width="160"
      require-mark-placement="right-hanging"
    >
      <n-form-item :label="t('admin.system.mail.host')" path="smtpHost">
        <n-input
          v-model:value="form.smtpHost"
          :placeholder="t('admin.system.mail.host_placeholder')"
          style="max-width: 420px;"
        />
      </n-form-item>

      <n-form-item :label="t('admin.system.mail.port')" path="smtpPort">
        <n-input-number
          v-model:value="form.smtpPort"
          :min="1"
          :max="65535"
          style="width: 200px;"
        />
        <span class="field-hint">{{ t('admin.system.mail.port_hint') }}</span>
      </n-form-item>

      <n-form-item :label="t('admin.system.mail.user')" path="smtpUser">
        <n-input
          v-model:value="form.smtpUser"
          :placeholder="t('admin.system.mail.user_placeholder')"
          style="max-width: 420px;"
        />
      </n-form-item>

      <n-form-item :label="t('admin.system.mail.pass')" path="smtpPass">
        <n-input
          v-model:value="form.smtpPass"
          type="password"
          show-password-on="click"
          :placeholder="t('admin.system.mail.pass_placeholder')"
          style="max-width: 420px;"
        />
      </n-form-item>

      <n-form-item :label="t('admin.system.mail.ssl')" path="smtpSsl">
        <n-switch v-model:value="form.smtpSsl">
          <template #checked>ON</template>
          <template #unchecked>OFF</template>
        </n-switch>
        <span class="field-hint">{{ t('admin.system.mail.ssl_hint') }}</span>
      </n-form-item>

      <n-form-item :label="t('admin.system.mail.sender_name')" path="senderName">
        <n-input
          v-model:value="form.senderName"
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
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  NForm,
  NFormItem,
  NInput,
  NInputNumber,
  NSwitch,
  NButton,
  NIcon,
  useMessage
} from 'naive-ui'
import { MailOutline } from '@vicons/ionicons5'
import { testSmtpConnectionApi } from '@/api/system'

interface MailSettings {
  smtpHost: string
  smtpPort: number
  smtpUser: string
  smtpPass: string
  smtpSsl: boolean
  senderName: string
}

defineProps<{
  form: MailSettings
}>()

const { t } = useI18n()
const message = useMessage()
const testMailLoading = ref(false)

async function handleSendTestEmail() {
  testMailLoading.value = true
  try {
    const res = await testSmtpConnectionApi()
    if (res.status === 200 || res.success) {
      message.success(t('admin.system.messages.test_mail_success'))
    }
  } catch (error: unknown) {
    message.error(t('admin.system.messages.test_mail_failed'))
  } finally {
    testMailLoading.value = false
  }
}
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
