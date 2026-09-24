<template>
  <div class="admin-risk-rules-view">
    <div class="view-header">
      <div class="header-left">
        <h2 class="view-title">{{ t('admin.risk.policy.title') }}</h2>
        <p class="view-subtitle">{{ t('admin.risk.policy.subtitle') }}</p>
      </div>
    </div>

    <n-card class="policy-card" :bordered="false">
      <n-spin :show="loading">
        <n-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-placement="top"
          class="policy-form"
        >
          <n-tabs v-model:value="activeTab" type="line" animated class="policy-tabs">
            <!-- ===================== Tab 1: 邮件策略 ===================== -->
            <n-tab-pane name="email" :tab="t('admin.risk.policy.tabs.email')">
              <div class="tab-content-wrapper">
                <!-- 策略介绍说明卡片 -->
                <div class="policy-banner">
                  <div class="banner-icon-box">
                    <n-icon size="22">
                      <MailOutline />
                    </n-icon>
                  </div>
                  <div class="banner-content">
                    <span class="banner-title">{{ t('admin.risk.policy.email.title') }}</span>
                    <span class="banner-desc">{{ t('admin.risk.policy.email.desc') }}</span>
                  </div>
                </div>

                <!-- 邮箱白名单后缀动态列表管理区 (严格遵守非下拉框设计，提供添加输入与列表) -->
                <div class="email-whitelist-section">
                  <div class="section-toolbar">
                    <div class="input-group-row">
                      <n-input
                        v-model:value="newSuffix"
                        :placeholder="t('admin.risk.policy.email.input_placeholder')"
                        clearable
                        class="suffix-input"
                        @keydown.enter.prevent="handleAddSuffix"
                      >
                        <template #prefix>
                          <span class="prefix-at">@</span>
                        </template>
                      </n-input>
                      <n-button
                        type="primary"
                        class="theme-primary-btn add-btn"
                        @click="handleAddSuffix"
                      >
                        <template #icon>
                          <n-icon>
                            <AddOutline />
                          </n-icon>
                        </template>
                        {{ t('admin.risk.policy.email.add_btn') }}
                      </n-button>
                    </div>
                    <span class="input-hint-text">{{ t('admin.risk.policy.email.input_hint') }}</span>
                  </div>

                  <!-- 列表状态栏与统计 -->
                  <div class="whitelist-list-header">
                    <div class="list-counter">
                      <span class="counter-text">
                        {{ t('admin.risk.policy.email.count_label', { count: form.emailWhitelistSuffixes.length }) }}
                      </span>
                    </div>
                    <n-popconfirm
                      v-if="form.emailWhitelistSuffixes.length > 0"
                      @positive-click="handleClearAllSuffixes"
                    >
                      <template #trigger>
                        <n-button text type="error" size="small" class="clear-all-btn">
                          {{ t('admin.risk.policy.email.clear_all') }}
                        </n-button>
                      </template>
                      {{ t('admin.risk.policy.email.clear_confirm') }}
                    </n-popconfirm>
                  </div>

                  <!-- 列表容器：有数据时展示独立条目列表，无数据展示空状态 -->
                  <div v-if="form.emailWhitelistSuffixes.length === 0" class="whitelist-empty-box">
                    <n-empty
                      :description="t('admin.risk.policy.email.empty_title')"
                      class="custom-empty"
                    >
                      <template #extra>
                        <span class="empty-sub-desc">{{ t('admin.risk.policy.email.empty_desc') }}</span>
                      </template>
                    </n-empty>
                  </div>

                  <div v-else class="whitelist-items-list">
                    <div
                      v-for="(suffix, index) in form.emailWhitelistSuffixes"
                      :key="suffix"
                      class="whitelist-item-card"
                    >
                      <div class="item-left">
                        <span class="item-badge">@</span>
                        <div class="item-domain-info">
                          <span class="domain-text">{{ suffix }}</span>
                          <span class="domain-rule">
                            {{ t('admin.risk.policy.email.rule_pattern_text', { suffix }) }}
                          </span>
                        </div>
                      </div>

                      <div class="item-right">
                        <n-button
                          size="small"
                          quaternary
                          type="error"
                          class="remove-btn"
                          @click="handleRemoveSuffix(index)"
                        >
                          <template #icon>
                            <n-icon size="14">
                              <TrashOutline />
                            </n-icon>
                          </template>
                          {{ t('admin.risk.policy.email.remove_btn') }}
                        </n-button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </n-tab-pane>

            <!-- ===================== Tab 2: 拦截策略 ===================== -->
            <n-tab-pane name="interception" :tab="t('admin.risk.policy.tabs.interception')">
              <div class="tab-content-wrapper">
                <!-- 1. 接口安全限流时间窗口 -->
                <div class="policy-row">
                  <div class="policy-meta">
                    <span class="policy-label">{{ t('admin.risk.policy.interception.rate_limit_window') }}</span>
                    <span class="policy-desc">{{ t('admin.risk.policy.interception.rate_limit_window_desc') }}</span>
                  </div>
                  <div class="policy-control">
                    <n-input-number
                      v-model:value="form.rateLimitWindow"
                      :min="10"
                      :max="600"
                      class="policy-input-number"
                    />
                  </div>
                </div>

                <n-divider class="row-divider" />

                <!-- 2. 窗口内最大请求次数 -->
                <div class="policy-row">
                  <div class="policy-meta">
                    <span class="policy-label">{{ t('admin.risk.policy.interception.rate_limit_max_requests') }}</span>
                    <span class="policy-desc">{{ t('admin.risk.policy.interception.rate_limit_max_requests_desc') }}</span>
                  </div>
                  <div class="policy-control">
                    <n-input-number
                      v-model:value="form.rateLimitMaxRequests"
                      :min="10"
                      :max="100000"
                      class="policy-input-number"
                    />
                  </div>
                </div>

                <n-divider class="row-divider" />

                <!-- 3. 1分钟突发速率阈值 -->
                <div class="policy-row">
                  <div class="policy-meta">
                    <span class="policy-label">{{ t('admin.risk.policy.velocity_medium') }}</span>
                    <span class="policy-desc">{{ t('admin.risk.policy.velocity_medium_desc') }}</span>
                  </div>
                  <div class="policy-control">
                    <n-input-number
                      v-model:value="form.velocityMediumThreshold"
                      :min="1"
                      :max="1000"
                      class="policy-input-number"
                    />
                  </div>
                </div>

                <n-divider class="row-divider" />

                <!-- 4. 单账号全网失败升中风险阈值 -->
                <div class="policy-row">
                  <div class="policy-meta">
                    <span class="policy-label">{{ t('admin.risk.policy.account_failures_medium') }}</span>
                    <span class="policy-desc">{{ t('admin.risk.policy.account_failures_medium_desc') }}</span>
                  </div>
                  <div class="policy-control">
                    <n-input-number
                      v-model:value="form.accountFailuresMediumThreshold"
                      :min="1"
                      :max="100"
                      class="policy-input-number"
                    />
                  </div>
                </div>

                <n-divider class="row-divider" />

                <!-- 5. 单账号全网失败升高风险阈值 -->
                <div class="policy-row">
                  <div class="policy-meta">
                    <span class="policy-label">{{ t('admin.risk.policy.account_failures_high') }}</span>
                    <span class="policy-desc">{{ t('admin.risk.policy.account_failures_high_desc') }}</span>
                  </div>
                  <div class="policy-control">
                    <n-input-number
                      v-model:value="form.accountFailuresHighThreshold"
                      :min="2"
                      :max="100"
                      class="policy-input-number"
                    />
                  </div>
                </div>
              </div>
            </n-tab-pane>

            <!-- ===================== Tab 3: 封禁策略 ===================== -->
            <n-tab-pane name="ban" :tab="t('admin.risk.policy.tabs.ban')">
              <div class="tab-content-wrapper">
                <!-- 1. 连续失败升中风险阈值 -->
                <div class="policy-row">
                  <div class="policy-meta">
                    <span class="policy-label">{{ t('admin.risk.policy.failures_medium') }}</span>
                    <span class="policy-desc">{{ t('admin.risk.policy.failures_medium_desc') }}</span>
                  </div>
                  <div class="policy-control">
                    <n-input-number
                      v-model:value="form.failuresMediumThreshold"
                      :min="1"
                      :max="100"
                      class="policy-input-number"
                    />
                  </div>
                </div>

                <n-divider class="row-divider" />

                <!-- 2. 连续失败升高风险阈值 -->
                <div class="policy-row">
                  <div class="policy-meta">
                    <span class="policy-label">{{ t('admin.risk.policy.failures_high') }}</span>
                    <span class="policy-desc">{{ t('admin.risk.policy.failures_high_desc') }}</span>
                  </div>
                  <div class="policy-control">
                    <n-input-number
                      v-model:value="form.failuresHighThreshold"
                      :min="2"
                      :max="100"
                      class="policy-input-number"
                    />
                  </div>
                </div>

                <n-divider class="row-divider" />

                <!-- 3. 连续失败自动封禁阈值 -->
                <div class="policy-row">
                  <div class="policy-meta">
                    <span class="policy-label">{{ t('admin.risk.policy.failures_ban') }}</span>
                    <span class="policy-desc">{{ t('admin.risk.policy.failures_ban_desc') }}</span>
                  </div>
                  <div class="policy-control">
                    <n-input-number
                      v-model:value="form.failuresBanThreshold"
                      :min="0"
                      :max="200"
                      class="policy-input-number"
                    />
                  </div>
                </div>

                <n-divider class="row-divider" />

                <!-- 4. 降险恢复扣减步长 -->
                <div class="policy-row">
                  <div class="policy-meta">
                    <span class="policy-label">{{ t('admin.risk.policy.recovery_step') }}</span>
                    <span class="policy-desc">{{ t('admin.risk.policy.recovery_step_desc') }}</span>
                  </div>
                  <div class="policy-control">
                    <n-input-number
                      v-model:value="form.recoveryStepCount"
                      :min="1"
                      :max="50"
                      class="policy-input-number"
                    />
                  </div>
                </div>
              </div>
            </n-tab-pane>
          </n-tabs>
        </n-form>

        <!-- 统一底部操作栏 -->
        <div class="action-footer">
          <n-button quaternary @click="handleResetDefault">
            {{ t('admin.risk.policy.reset_btn') }}
          </n-button>
          <n-button
            type="primary"
            class="theme-primary-btn"
            :loading="saveLoading"
            @click="handleSave"
          >
            {{ t('admin.risk.policy.save_btn') }}
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
  NTabs,
  NTabPane,
  NForm,
  NInput,
  NInputNumber,
  NDivider,
  NButton,
  NIcon,
  NEmpty,
  NPopconfirm,
  useMessage,
  type FormInst,
  type FormRules
} from 'naive-ui'
import {
  AddOutline,
  TrashOutline,
  MailOutline
} from '@vicons/ionicons5'
import { useI18n } from 'vue-i18n'
import { getRiskPolicyApi, updateRiskPolicyApi } from '@/api/risk'
import type { AdminRiskPolicyDTO } from '@/types/risk'

const { t } = useI18n()
const message = useMessage()

const activeTab = ref('email')
const loading = ref(false)
const saveLoading = ref(false)
const formRef = ref<FormInst | null>(null)

// 动态添加白名单后缀输入模型
const newSuffix = ref('')

const form = ref<AdminRiskPolicyDTO>({
  emailWhitelistSuffixes: [],
  rateLimitWindow: 60,
  rateLimitMaxRequests: 120,
  velocityMediumThreshold: 12,
  accountFailuresMediumThreshold: 4,
  accountFailuresHighThreshold: 8,
  failuresMediumThreshold: 5,
  failuresHighThreshold: 10,
  failuresBanThreshold: 20,
  recoveryStepCount: 6
})

const rules: FormRules = {
  rateLimitWindow: [{ required: true, type: 'number', message: '限流窗口不能为空', trigger: ['blur', 'change'] }],
  rateLimitMaxRequests: [{ required: true, type: 'number', message: '最大请求数不能为空', trigger: ['blur', 'change'] }],
  failuresMediumThreshold: [{ required: true, type: 'number', message: '阈值不能为空', trigger: ['blur', 'change'] }],
  failuresHighThreshold: [{ required: true, type: 'number', message: '阈值不能为空', trigger: ['blur', 'change'] }],
  failuresBanThreshold: [{ required: true, type: 'number', message: '阈值不能为空', trigger: ['blur', 'change'] }],
  velocityMediumThreshold: [{ required: true, type: 'number', message: '速率阈值不能为空', trigger: ['blur', 'change'] }],
  accountFailuresMediumThreshold: [{ required: true, type: 'number', message: '阈值不能为空', trigger: ['blur', 'change'] }],
  accountFailuresHighThreshold: [{ required: true, type: 'number', message: '阈值不能为空', trigger: ['blur', 'change'] }],
  recoveryStepCount: [{ required: true, type: 'number', message: '步长不能为空', trigger: ['blur', 'change'] }]
}

function handleAddSuffix() {
  const raw = newSuffix.value.trim()
  if (!raw) {
    return
  }

  // 自动剔除首部 @ 与多余空格，并统一转换为小写
  let clean = raw.toLowerCase()
  if (clean.startsWith('@')) {
    clean = clean.substring(1).trim()
  }

  // 域名合法性基础校验（包含点分隔符且仅含英数字、中划线和点）
  const domainPattern = /^[a-z0-9]([a-z0-9-]*[a-z0-9])?(\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)+$/
  if (!domainPattern.test(clean)) {
    message.error(t('admin.risk.policy.email.invalid_error'))
    return
  }

  // 重复性排重
  if (form.value.emailWhitelistSuffixes.includes(clean)) {
    message.warning(t('admin.risk.policy.email.duplicate_error'))
    return
  }

  form.value.emailWhitelistSuffixes.push(clean)
  newSuffix.value = ''
  message.success(t('admin.risk.policy.email.add_success', { suffix: clean }))
}

function handleRemoveSuffix(index: number) {
  form.value.emailWhitelistSuffixes.splice(index, 1)
}

function handleClearAllSuffixes() {
  form.value.emailWhitelistSuffixes = []
}

async function fetchPolicy() {
  loading.value = true
  try {
    const res = await getRiskPolicyApi()
    if (res.data) {
      form.value = {
        emailWhitelistSuffixes: Array.isArray(res.data.emailWhitelistSuffixes) ? [...res.data.emailWhitelistSuffixes] : [],
        rateLimitWindow: res.data.rateLimitWindow ?? 60,
        rateLimitMaxRequests: res.data.rateLimitMaxRequests ?? 120,
        failuresMediumThreshold: res.data.failuresMediumThreshold ?? 5,
        failuresHighThreshold: res.data.failuresHighThreshold ?? 10,
        failuresBanThreshold: res.data.failuresBanThreshold ?? 20,
        velocityMediumThreshold: res.data.velocityMediumThreshold ?? 12,
        accountFailuresMediumThreshold: res.data.accountFailuresMediumThreshold ?? 4,
        accountFailuresHighThreshold: res.data.accountFailuresHighThreshold ?? 8,
        recoveryStepCount: res.data.recoveryStepCount ?? 6
      }
    }
  } catch (err: any) {
    message.error(err?.message || '获取策略设置失败')
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
    await updateRiskPolicyApi(form.value)
    message.success(t('admin.risk.policy.save_success'))
  } catch (err: any) {
    message.error(err?.message || '更新策略配置失败')
  } finally {
    saveLoading.value = false
  }
}

function handleResetDefault() {
  form.value = {
    emailWhitelistSuffixes: [],
    rateLimitWindow: 60,
    rateLimitMaxRequests: 120,
    failuresMediumThreshold: 5,
    failuresHighThreshold: 10,
    failuresBanThreshold: 20,
    velocityMediumThreshold: 12,
    accountFailuresMediumThreshold: 4,
    accountFailuresHighThreshold: 8,
    recoveryStepCount: 6
  }
  message.info(t('admin.risk.policy.reset_success'))
}

onMounted(() => {
  fetchPolicy()
})
</script>

<style scoped lang="scss">
.admin-risk-rules-view {
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

.policy-card {
  background-color: var(--v-bg-card, #ffffff);
  border-radius: 12px;
  border: 1px solid var(--v-border-base, #e5e4e7);
  box-shadow: var(--v-shadow-sm, 0 1px 3px rgba(0, 0, 0, 0.03));
  padding: 8px 16px;
}

.tab-content-wrapper {
  padding: 16px 4px 8px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 邮件策略介绍卡片 */
.policy-banner {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 14px 16px;
  border-radius: 8px;
  background-color: var(--v-bg-subtle, #f7f7f9);
  border: 1px solid var(--v-border-base, #e5e4e7);

  .banner-icon-box {
    width: 36px;
    height: 36px;
    border-radius: 8px;
    background-color: rgba(99, 102, 241, 0.12);
    color: #6366f1;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .banner-content {
    display: flex;
    flex-direction: column;
    gap: 4px;

    .banner-title {
      font-size: 14px;
      font-weight: 600;
      color: var(--v-text-primary, #111111);
    }

    .banner-desc {
      font-size: 13px;
      color: var(--v-text-desc, #444444);
      line-height: 1.5;
    }
  }
}

/* 邮箱白名单管理区 */
.email-whitelist-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 4px;

  .section-toolbar {
    display: flex;
    flex-direction: column;
    gap: 8px;

    .input-group-row {
      display: flex;
      align-items: center;
      gap: 10px;
      max-width: 540px;

      .suffix-input {
        flex: 1;

        .prefix-at {
          font-weight: 600;
          color: var(--v-text-desc, #666666);
          margin-right: 2px;
        }
      }

      .add-btn {
        flex-shrink: 0;
      }
    }

    .input-hint-text {
      font-size: 12px;
      color: var(--v-text-desc, #444444);
    }
  }

  .whitelist-list-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-bottom: 8px;
    border-bottom: 1px solid var(--v-border-base, #e5e4e7);

    .counter-text {
      font-size: 13px;
      font-weight: 600;
      color: var(--v-text-primary, #111111);
    }

    .clear-all-btn {
      font-size: 12px;
    }
  }

  .whitelist-empty-box {
    padding: 36px 0;
    display: flex;
    justify-content: center;
    align-items: center;

    .empty-sub-desc {
      font-size: 12px;
      color: var(--v-text-desc, #444444);
      max-width: 480px;
      display: block;
      text-align: center;
    }
  }

  .whitelist-items-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
    max-width: 720px;

    .whitelist-item-card {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 12px 16px;
      border-radius: 8px;
      border: 1px solid var(--v-border-base, #e5e4e7);
      background-color: var(--v-bg-card, #ffffff);
      transition: border-color 0.2s ease, box-shadow 0.2s ease;

      &:hover {
        border-color: rgba(99, 102, 241, 0.4);
        box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);
      }

      .item-left {
        display: flex;
        align-items: center;
        gap: 12px;

        .item-badge {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          width: 26px;
          height: 26px;
          border-radius: 6px;
          background-color: rgba(99, 102, 241, 0.1);
          color: #6366f1;
          font-weight: 700;
          font-size: 13px;
        }

        .item-domain-info {
          display: flex;
          flex-direction: column;
          gap: 2px;

          .domain-text {
            font-family: 'Fira Code', monospace, Consolas, Courier;
            font-size: 14px;
            font-weight: 600;
            color: var(--v-text-primary, #111111);
          }

          .domain-rule {
            font-size: 12px;
            color: var(--v-text-desc, #444444);
          }
        }
      }

      .item-right {
        display: flex;
        align-items: center;
      }
    }
  }
}

/* 策略通用行配置 */
.policy-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 0;
  gap: 24px;

  .policy-meta {
    display: flex;
    flex-direction: column;
    gap: 4px;
    flex: 1;

    .policy-label {
      font-size: 14px;
      font-weight: 600;
      color: var(--v-text-primary, #111111);
    }

    .policy-desc {
      font-size: 13px;
      color: var(--v-text-desc, #444444);
      line-height: 1.5;
    }
  }

  .policy-control {
    display: flex;
    justify-content: flex-end;
    align-items: center;
    width: 200px;

    .policy-input-number {
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
}

.theme-primary-btn {
  background-color: #000000;
  color: #ffffff;
  border-radius: 6px;
  font-weight: 600;
  padding: 0 20px;
  transition: opacity 0.2s ease, background-color 0.2s ease;

  &:hover {
    background-color: #222222;
    opacity: 0.92;
  }
}

@media (max-width: 768px) {
  .policy-row {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;

    .policy-control {
      width: 100%;
      justify-content: flex-start;
    }
  }

  .email-whitelist-section {
    .section-toolbar {
      .input-group-row {
        flex-direction: column;
        align-items: stretch;
      }
    }
  }
}
</style>
