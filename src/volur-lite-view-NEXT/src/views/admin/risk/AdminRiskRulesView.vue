<template>
  <div class="space-y-6 max-w-4xl mx-auto">
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h2 class="text-xl font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
          {{ t('admin.risk.policy.title', '风控策略') }}
        </h2>
        <p class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary mt-1">
          {{ t('admin.risk.policy.subtitle', '频率限制限流阈值、多维失败惩罚阶梯与自动化熔断封禁防御规则') }}
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
          {{ t('common.save', '保存策略') }}
        </VBtn>
      </div>
    </div>

    <div v-if="loading" class="p-12 text-center text-google-blue dark:text-google-blue-dark">
      {{ t('common.loading', '正在加载风控策略...') }}
    </div>

    <div v-else class="space-y-6">
      <div class="p-6 rounded-2xl bg-white dark:bg-[#292a2d] border border-google-border-light dark:border-google-border-dark space-y-4">
        <h3 class="text-sm font-semibold text-google-text-primary dark:text-google-text-darkPrimary border-b border-google-border-light dark:border-google-border-dark pb-3">
          {{ t('admin.risk.policy.rate_limit_card', '全局请求频率限制 (Rate Limiting)') }}
        </h3>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
              限流时间窗口 (秒)
            </label>
            <VInput
              v-model:value="rateLimitWindowStr"
              placeholder="'60'"
              clearable
            />
          </div>

          <div>
            <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
              单 IP 窗口内最大请求数
            </label>
            <VInput
              v-model:value="rateLimitMaxStr"
              placeholder="'120'"
              clearable
            />
          </div>
        </div>
      </div>

      <div class="p-6 rounded-2xl bg-white dark:bg-[#292a2d] border border-google-border-light dark:border-google-border-dark space-y-4">
        <h3 class="text-sm font-semibold text-google-text-primary dark:text-google-text-darkPrimary border-b border-google-border-light dark:border-google-border-dark pb-3">
          {{ t('admin.risk.policy.failure_thresholds_card', '多维行为异常与失败容忍惩罚阈值') }}
        </h3>

        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
              单账号密码连续失败中危阈值
            </label>
            <VInput
              v-model:value="accFailMedStr"
              placeholder="'3'"
              clearable
            />
          </div>

          <div>
            <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
              单账号密码连续失败高危阈值
            </label>
            <VInput
              v-model:value="accFailHighStr"
              placeholder="'5'"
              clearable
            />
          </div>

          <div>
            <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
              单 IP 触发自动封禁阈值
            </label>
            <VInput
              v-model:value="failBanStr"
              placeholder="'10'"
              clearable
            />
          </div>

          <div>
            <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
              单 IP 失败次数中危阈值
            </label>
            <VInput
              v-model:value="failMedStr"
              placeholder="'5'"
              clearable
            />
          </div>

          <div>
            <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
              单 IP 失败次数高危阈值
            </label>
            <VInput
              v-model:value="failHighStr"
              placeholder="'8'"
              clearable
            />
          </div>

          <div>
            <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
              阶梯降级恢复步长
            </label>
            <VInput
              v-model:value="recoveryStepStr"
              placeholder="'1'"
              clearable
            />
          </div>
        </div>
      </div>

      <div class="p-6 rounded-2xl bg-white dark:bg-[#292a2d] border border-google-border-light dark:border-google-border-dark space-y-4">
        <h3 class="text-sm font-semibold text-google-text-primary dark:text-google-text-darkPrimary border-b border-google-border-light dark:border-google-border-dark pb-3">
          {{ t('admin.risk.policy.whitelist_suffixes', '受信任邮箱域名白名单后缀') }}
        </h3>

        <div>
          <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
            邮箱后缀 (逗号分隔，如: @edu.cn, @example.com)
          </label>
          <VInput
            v-model:value="emailWhitelistStr"
            placeholder="'@edu.cn, @gmail.com'"
            clearable
          />
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
import type { AdminRiskPolicyVO } from '@/types/risk'
import { getRiskPolicyApi, updateRiskPolicyApi } from '@/api/risk'

const { t } = useI18n()
const message = useMessage()

const loading = ref(false)
const saving = ref(false)

const policy = ref<AdminRiskPolicyVO>({
  emailWhitelistSuffixes: ['@edu.cn'],
  rateLimitWindow: 60,
  rateLimitMaxRequests: 120,
  velocityMediumThreshold: 20,
  accountFailuresMediumThreshold: 3,
  accountFailuresHighThreshold: 5,
  failuresMediumThreshold: 5,
  failuresHighThreshold: 8,
  failuresBanThreshold: 10,
  recoveryStepCount: 1
})

const emailWhitelistStr = computed<string>({
  get: () => (policy.value.emailWhitelistSuffixes || []).join(', '),
  set: (val) => {
    policy.value.emailWhitelistSuffixes = val
      .split(',')
      .map(s => s.trim())
      .filter(Boolean)
  }
})

const rateLimitWindowStr = computed<string>({
  get: () => String(policy.value.rateLimitWindow || '60'),
  set: (val) => {
    policy.value.rateLimitWindow = parseInt(val, 10) || 60
  }
})

const rateLimitMaxStr = computed<string>({
  get: () => String(policy.value.rateLimitMaxRequests || '120'),
  set: (val) => {
    policy.value.rateLimitMaxRequests = parseInt(val, 10) || 120
  }
})

const accFailMedStr = computed<string>({
  get: () => String(policy.value.accountFailuresMediumThreshold || '3'),
  set: (val) => {
    policy.value.accountFailuresMediumThreshold = parseInt(val, 10) || 3
  }
})

const accFailHighStr = computed<string>({
  get: () => String(policy.value.accountFailuresHighThreshold || '5'),
  set: (val) => {
    policy.value.accountFailuresHighThreshold = parseInt(val, 10) || 5
  }
})

const failMedStr = computed<string>({
  get: () => String(policy.value.failuresMediumThreshold || '5'),
  set: (val) => {
    policy.value.failuresMediumThreshold = parseInt(val, 10) || 5
  }
})

const failHighStr = computed<string>({
  get: () => String(policy.value.failuresHighThreshold || '8'),
  set: (val) => {
    policy.value.failuresHighThreshold = parseInt(val, 10) || 8
  }
})

const failBanStr = computed<string>({
  get: () => String(policy.value.failuresBanThreshold || '10'),
  set: (val) => {
    policy.value.failuresBanThreshold = parseInt(val, 10) || 10
  }
})

const recoveryStepStr = computed<string>({
  get: () => String(policy.value.recoveryStepCount || '1'),
  set: (val) => {
    policy.value.recoveryStepCount = parseInt(val, 10) || 1
  }
})

async function fetchPolicy() {
  loading.value = true
  try {
    const res = await getRiskPolicyApi()
    if (res.data) {
      policy.value = res.data
    }
  } catch (err: any) {
    message.error(err.message || '获取风控策略失败')
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    await updateRiskPolicyApi(policy.value)
    message.success('风控策略配置已保存')
    await fetchPolicy()
  } catch (err: any) {
    message.error(err.message || '保存风控策略失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  fetchPolicy()
})
</script>
