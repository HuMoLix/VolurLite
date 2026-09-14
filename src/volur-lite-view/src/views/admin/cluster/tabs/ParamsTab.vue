<template>
  <div class="tab-pane-container">
    <n-form
      :model="form"
      label-placement="left"
      label-width="210"
      require-mark-placement="right-hanging"
    >
      <!-- 容器编排类型 -->
      <n-form-item :label="t('admin.clusters.form.type')" path="containerType">
        <n-select
          v-model:value="form.containerType"
          :options="containerTypeOptions"
          style="max-width: 380px;"
        />
        <span class="field-hint">{{ t('admin.clusters.form.type_hint') }}</span>
      </n-form-item>

      <!-- 通用公网 Host -->
      <n-form-item :label="t('admin.clusters.form.public_host')" path="publicHost">
        <n-input
          v-model:value="form.publicHost"
          :placeholder="t('admin.clusters.form.public_host_placeholder')"
          style="max-width: 380px;"
        />
        <span class="field-hint">{{ t('admin.clusters.form.public_host_hint') }}</span>
      </n-form-item>

      <!-- 端口映射范围 -->
      <n-form-item :label="t('admin.clusters.form.port_range')">
        <div class="range-inputs">
          <n-input-number
            v-model:value="form.portRangeStart"
            :min="1024"
            :max="65535"
            style="width: 160px;"
          />
          <span class="range-divider">-</span>
          <n-input-number
            v-model:value="form.portRangeEnd"
            :min="1024"
            :max="65535"
            style="width: 160px;"
          />
        </div>
        <span class="field-hint">{{ t('admin.clusters.form.port_range_hint') }}</span>
      </n-form-item>

      <!-- Docker Swarm 专有配置展开分支 -->
      <template v-if="form.containerType === 'DOCKER_SWARM'">
        <n-divider dashed title-placement="left">Docker Swarm 集群专属参数</n-divider>

        <n-form-item :label="t('admin.clusters.form.swarm_api_url')" path="dockerSwarmApiUrl">
          <n-input
            v-model:value="form.dockerSwarmApiUrl"
            :placeholder="t('admin.clusters.form.swarm_api_url_placeholder')"
            style="max-width: 520px;"
          />
          <span class="field-hint">{{ t('admin.clusters.form.swarm_api_url_hint') }}</span>
        </n-form-item>

        <n-form-item :label="t('admin.clusters.form.swarm_network')" path="dockerSwarmNetwork">
          <n-input
            v-model:value="form.dockerSwarmNetwork"
            :placeholder="t('admin.clusters.form.swarm_network_placeholder')"
            style="max-width: 520px;"
          />
          <span class="field-hint">{{ t('admin.clusters.form.swarm_network_hint') }}</span>
        </n-form-item>

        <n-form-item :label="t('admin.clusters.form.swarm_cert_path')" path="dockerSwarmCertPath">
          <n-input
            v-model:value="form.dockerSwarmCertPath"
            :placeholder="t('admin.clusters.form.swarm_cert_path_placeholder')"
            style="max-width: 520px;"
          />
          <span class="field-hint">{{ t('admin.clusters.form.swarm_cert_path_hint') }}</span>
        </n-form-item>

        <n-form-item :label="t('admin.clusters.form.swarm_outbound')" path="dockerSwarmOutboundEnabled">
          <n-space align="center">
            <n-switch v-model:value="form.dockerSwarmOutboundEnabled" />
            <span class="field-hint">{{ t('admin.clusters.form.swarm_outbound_hint') }}</span>
          </n-space>
        </n-form-item>
      </template>

      <!-- K3s 专有配置展开分支 -->
      <template v-if="form.containerType === 'K3S'">
        <n-divider dashed title-placement="left">K3s / Kubernetes 集群专属参数</n-divider>

        <n-form-item :label="t('admin.clusters.form.k3s_master_url')" path="k3sMasterUrl">
          <n-input
            v-model:value="form.k3sMasterUrl"
            :placeholder="t('admin.clusters.form.k3s_master_url_placeholder')"
            style="max-width: 520px;"
          />
          <span class="field-hint">{{ t('admin.clusters.form.k3s_master_url_hint') }}</span>
        </n-form-item>

        <n-form-item :label="t('admin.clusters.form.k3s_namespace')" path="k3sNamespace">
          <n-input
            v-model:value="form.k3sNamespace"
            :placeholder="t('admin.clusters.form.k3s_namespace_placeholder')"
            style="max-width: 520px;"
          />
          <span class="field-hint">{{ t('admin.clusters.form.k3s_namespace_hint') }}</span>
        </n-form-item>

        <n-form-item :label="t('admin.clusters.form.k3s_kubeconfig')" path="k3sKubeconfigPath">
          <n-input
            v-model:value="form.k3sKubeconfigPath"
            :placeholder="t('admin.clusters.form.k3s_kubeconfig_placeholder')"
            style="max-width: 520px;"
          />
          <span class="field-hint">{{ t('admin.clusters.form.k3s_kubeconfig_hint') }}</span>
        </n-form-item>

        <n-form-item :label="t('admin.clusters.form.k3s_token')" path="k3sToken">
          <n-input
            v-model:value="form.k3sToken"
            type="password"
            show-password-on="click"
            :placeholder="t('admin.clusters.form.k3s_token_placeholder')"
            style="max-width: 520px;"
          />
          <span class="field-hint">{{ t('admin.clusters.form.k3s_token_hint') }}</span>
        </n-form-item>
      </template>

      <!-- 集群调度与主从隔离配置 -->
      <n-divider dashed title-placement="left">集群资源调度与隔离策略</n-divider>

      <n-form-item :label="t('admin.clusters.form.isolation')">
        <div class="switch-row">
          <n-switch v-model:value="form.resourceIsolationEnabled" />
          <span class="field-hint">{{ t('admin.clusters.form.isolation_hint') }}</span>
        </div>
      </n-form-item>

      <!-- 私有镜像仓库配置 (Docker Registry) -->
      <n-divider dashed title-placement="left">私有镜像仓库管理 (Docker Registry)</n-divider>

      <n-form-item label="启用 Registry 中心">
        <div class="switch-row">
          <n-switch v-model:value="form.registryEnabled" />
          <span class="field-hint">开启后作为集群镜像分发中心，主从节点及 K3s 均可自动共享与拉取镜像</span>
        </div>
      </n-form-item>

      <template v-if="form.registryEnabled">
        <n-form-item label="Registry 内部地址" path="registryInternalUrl">
          <n-input
            v-model:value="form.registryInternalUrl"
            placeholder="http://volur-registry:5000"
            style="max-width: 520px;"
          />
          <span class="field-hint">后端服务在 Docker 容器网络内直接访问 Registry 的 REST 地址</span>
        </n-form-item>

        <n-form-item label="Registry 对外端口" path="registryPublicPort">
          <n-input-number
            v-model:value="form.registryPublicPort"
            :min="1"
            :max="65535"
            style="width: 160px;"
          />
          <span class="field-hint">各子节点与外部主机访问该镜像仓库的公开端口（默认 5000）</span>
        </n-form-item>

        <n-form-item label="Registry 运行状态">
          <n-space align="center">
            <n-tag :type="props.settings?.registryOnline ? 'success' : 'error'" round size="small">
              {{ props.settings?.registryOnline ? '在线服务正常 (200 OK)' : '离线或未连通' }}
            </n-tag>
            <span class="field-hint">后端通过 Docker 内部网络实时探测 Registry /v2/ 接口探针</span>
          </n-space>
        </n-form-item>
      </template>

      <!-- 集群连通性测试 -->
      <n-form-item :label="t('admin.clusters.form.test_connection')">
        <div class="test-connection-section">
          <n-button
            secondary
            size="small"
            :loading="testingConnection"
            @click="handleTestConnection"
          >
            <template #icon>
              <n-icon :component="ServerOutline" />
            </template>
            {{ t('admin.clusters.form.test_connection') }}
          </n-button>

          <div v-if="testResult" class="test-result-box">
            <n-alert
              :type="testResult.connected ? 'success' : 'warning'"
              :title="testResult.connected ? t('admin.clusters.form.test_success') : t('admin.clusters.form.test_failed')"
              style="margin-top: 10px; max-width: 620px;"
            >
              <div class="test-result-details">
                <p class="res-msg">{{ testResult.message }}</p>
                <div v-if="testResult.connected" class="res-badges">
                  <n-tag size="tiny" type="info" round v-if="testResult.clusterVersion">
                    {{ testResult.clusterVersion }}
                  </n-tag>
                  <n-tag size="tiny" type="success" round v-if="testResult.nodeCount !== undefined">
                    节点数: {{ testResult.nodeCount }}
                  </n-tag>
                  <n-tag size="tiny" round v-if="testResult.latencyMs !== undefined">
                    延迟: {{ testResult.latencyMs }}ms
                  </n-tag>
                </div>
              </div>
            </n-alert>
          </div>
        </div>
      </n-form-item>
    </n-form>

    <!-- Action Save Bar -->
    <div class="action-footer">
      <n-button
        type="primary"
        class="theme-primary-btn"
        :loading="savingSettings"
        @click="handleSaveSettings"
      >
        {{ t('admin.clusters.form.save_settings') }}
      </n-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  NForm,
  NFormItem,
  NInput,
  NInputNumber,
  NSelect,
  NSwitch,
  NDivider,
  NButton,
  NIcon,
  NAlert,
  NTag,
  NSpace,
  useMessage,
  type SelectOption
} from 'naive-ui'
import { ServerOutline } from '@vicons/ionicons5'
import {
  updateAdminClusterSettingsApi,
  testAdminClusterConnectionApi
} from '@/api/cluster'
import type {
  AdminClusterSettingsVO,
  AdminContainerTestConnectionVO
} from '@/types/cluster'

interface Props {
  settings?: AdminClusterSettingsVO | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'saved'): void
}>()

const { t } = useI18n()
const message = useMessage()

const savingSettings = ref(false)
const testingConnection = ref(false)
const testResult = ref<AdminContainerTestConnectionVO | null>(null)

const form = reactive({
  containerType: 'K3S',
  portRangeStart: 20000,
  portRangeEnd: 30000,
  publicHost: '127.0.0.1',
  dockerSwarmApiUrl: 'unix:///var/run/docker.sock',
  dockerSwarmNetwork: 'volur-swarm-network',
  dockerSwarmCertPath: '',
  dockerSwarmOutboundEnabled: false,
  k3sMasterUrl: 'https://127.0.0.1:6443',
  k3sNamespace: 'volur-challenges',
  k3sKubeconfigPath: '/etc/rancher/k3s/k3s.yaml',
  k3sToken: '',
  resourceIsolationEnabled: true,
  registryEnabled: true,
  registryInternalUrl: 'http://volur-registry:5000',
  registryPublicPort: 5000
})

watch(
  () => props.settings,
  (val) => {
    if (val) {
      Object.assign(form, {
        containerType: val.containerType || 'K3S',
        portRangeStart: val.portRangeStart || 20000,
        portRangeEnd: val.portRangeEnd || 30000,
        publicHost: val.publicHost || '127.0.0.1',
        dockerSwarmApiUrl: val.dockerSwarmApiUrl || 'unix:///var/run/docker.sock',
        dockerSwarmNetwork: val.dockerSwarmNetwork || 'volur-swarm-network',
        dockerSwarmCertPath: val.dockerSwarmCertPath || '',
        dockerSwarmOutboundEnabled: val.dockerSwarmOutboundEnabled ?? false,
        k3sMasterUrl: val.k3sMasterUrl || 'https://127.0.0.1:6443',
        k3sNamespace: val.k3sNamespace || 'volur-challenges',
        k3sKubeconfigPath: val.k3sKubeconfigPath || '/etc/rancher/k3s/k3s.yaml',
        k3sToken: val.k3sToken || '',
        resourceIsolationEnabled: val.resourceIsolationEnabled ?? true,
        registryEnabled: val.registryEnabled ?? true,
        registryInternalUrl: val.registryInternalUrl || 'http://volur-registry:5000',
        registryPublicPort: val.registryPublicPort || 5000
      })
    }
  },
  { immediate: true, deep: true }
)

const containerTypeOptions = computed<SelectOption[]>(() => [
  { label: t('admin.clusters.form.type_docker_swarm'), value: 'DOCKER_SWARM' },
  { label: t('admin.clusters.form.type_k3s'), value: 'K3S' }
])

async function handleSaveSettings() {
  savingSettings.value = true
  try {
    await updateAdminClusterSettingsApi(form)
    message.success(t('admin.clusters.form.save_success'))
    emit('saved')
  } catch {
    message.error('保存集群设置失败')
  } finally {
    savingSettings.value = false
  }
}

async function handleTestConnection() {
  testingConnection.value = true
  testResult.value = null
  try {
    const res = await testAdminClusterConnectionApi({
      containerType: form.containerType,
      dockerSwarmApiUrl: form.dockerSwarmApiUrl,
      dockerSwarmCertPath: form.dockerSwarmCertPath,
      k3sMasterUrl: form.k3sMasterUrl,
      k3sNamespace: form.k3sNamespace,
      k3sKubeconfigPath: form.k3sKubeconfigPath
    })
    if (res.data) {
      testResult.value = res.data
      if (res.data.connected) {
        message.success(res.data.message || t('admin.clusters.form.test_success'))
      } else {
        message.warning(res.data.message || t('admin.clusters.form.test_failed'))
      }
    }
  } catch {
    message.error(t('admin.clusters.form.test_failed'))
  } finally {
    testingConnection.value = false
  }
}
</script>

<style scoped lang="scss">
.tab-pane-container {
  padding: 16px 8px;
}

.field-hint {
  margin-left: 12px;
  font-size: 12px;
  color: #444444;
}

.range-inputs {
  display: flex;
  align-items: center;
  gap: 8px;

  .range-divider {
    color: #999999;
    font-weight: 500;
  }
}

.switch-row {
  display: flex;
  align-items: center;
}

.test-connection-section {
  display: flex;
  flex-direction: column;
  align-items: flex-start;

  .test-result-details {
    display: flex;
    flex-direction: column;
    gap: 6px;

    .res-msg {
      margin: 0;
      font-size: 13px;
      line-height: 1.4;
    }

    .res-badges {
      display: flex;
      align-items: center;
      gap: 6px;
      flex-wrap: wrap;
      margin-top: 4px;
    }
  }
}

.action-footer {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--v-border-base, #efeff5);
  display: flex;
  justify-content: flex-end;
}

.theme-primary-btn {
  background-color: #000000 !important;
  color: #ffffff !important;
  font-weight: 500;
  border-radius: 6px;
  border: none !important;

  &:hover {
    background-color: #222222 !important;
  }
}
</style>
