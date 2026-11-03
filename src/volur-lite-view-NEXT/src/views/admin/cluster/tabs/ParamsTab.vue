<template>
  <div class="space-y-6 max-w-3xl">
    <div class="space-y-4">
      <div class="text-xs font-bold text-google-text-primary dark:text-google-text-darkPrimary">
        基础调度与网络访问
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
            {{ t('admin.clusters.form.type', '编排架构类型') }} *
          </label>
          <VSelection
            v-model:value="form.containerType"
            :options="containerTypeOptions"
          />
          <div class="text-[11px] text-google-text-secondary dark:text-google-text-darkSecondary mt-1">
            {{ t('admin.clusters.form.type_hint', '支持 Docker Swarm 原生集群或轻量级 K3s / Kubernetes 集群') }}
          </div>
        </div>

        <div>
          <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
            {{ t('admin.clusters.form.public_host', '对外服务访问 Host (IP 或域名)') }} *
          </label>
          <VInput
            v-model:value="form.publicHost"
            :placeholder="t('admin.clusters.form.public_host_placeholder', '127.0.0.1 或 ctf.example.com')"
            clearable
          />
          <div class="text-[11px] text-google-text-secondary dark:text-google-text-darkSecondary mt-1">
            {{ t('admin.clusters.form.public_host_hint', '前台选手访问靶机实例映射端口时使用的对外 IP 或解析域名') }}
          </div>
        </div>

        <div>
          <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
            {{ t('admin.clusters.form.port_range_start', '靶机映射起始端口') }} *
          </label>
          <VInput
            v-model:value="portRangeStartStr"
            placeholder="20000"
            clearable
          />
        </div>

        <div>
          <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
            {{ t('admin.clusters.form.port_range_end', '靶机映射终止端口') }} *
          </label>
          <VInput
            v-model:value="portRangeEndStr"
            placeholder="40000"
            clearable
          />
        </div>
      </div>
    </div>

    <div v-if="form.containerType === 'DOCKER_SWARM'" class="space-y-4 pt-4 border-t border-google-border-light dark:border-google-border-dark">
      <div class="text-xs font-bold text-google-text-primary dark:text-google-text-darkPrimary">
        Docker Swarm 集群专属参数
      </div>

      <div class="space-y-4">
        <div>
          <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
            {{ t('admin.clusters.form.swarm_api_url', 'Docker Swarm API URL') }} *
          </label>
          <VInput
            v-model:value="form.dockerSwarmApiUrl"
            :placeholder="t('admin.clusters.form.swarm_api_url_placeholder', 'tcp://127.0.0.1:2375 或 unix:///var/run/docker.sock')"
            clearable
          />
          <div class="text-[11px] text-google-text-secondary dark:text-google-text-darkSecondary mt-1">
            {{ t('admin.clusters.form.swarm_api_url_hint', '平台服务端调用 Docker Swarm 管理面的 TCP 或 Unix Socket 地址') }}
          </div>
        </div>

        <div>
          <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
            {{ t('admin.clusters.form.swarm_network', 'Swarm Overlay 网络名称') }} *
          </label>
          <VInput
            v-model:value="form.dockerSwarmNetwork"
            :placeholder="t('admin.clusters.form.swarm_network_placeholder', 'volur_network')"
            clearable
          />
          <div class="text-[11px] text-google-text-secondary dark:text-google-text-darkSecondary mt-1">
            {{ t('admin.clusters.form.swarm_network_hint', '跨计算节点容器互联所使用的 Overlay 驱动网络') }}
          </div>
        </div>

        <div>
          <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
            {{ t('admin.clusters.form.swarm_cert_path', 'Docker TLS 证书目录路径 (可选)') }}
          </label>
          <VInput
            v-model:value="form.dockerSwarmCertPath"
            :placeholder="t('admin.clusters.form.swarm_cert_path_placeholder', '例如: /etc/docker/certs')"
            clearable
          />
          <div class="text-[11px] text-google-text-secondary dark:text-google-text-darkSecondary mt-1">
            {{ t('admin.clusters.form.swarm_cert_path_hint', '启用 2376 TLS 双向认证时的客户端 ca.pem, cert.pem, key.pem 证书所在目录') }}
          </div>
        </div>

        <div class="flex items-center gap-3 pt-1">
          <n-switch v-model:value="form.dockerSwarmOutboundEnabled" />
          <span class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary">
            {{ t('admin.clusters.form.swarm_outbound_hint', '允许 Swarm 题目靶机访问外网（开启后靶机容器可主动建立外部出站网络连接）') }}
          </span>
        </div>
      </div>
    </div>

    <div v-if="form.containerType === 'K3S'" class="space-y-4 pt-4 border-t border-google-border-light dark:border-google-border-dark">
      <div class="text-xs font-bold text-google-text-primary dark:text-google-text-darkPrimary">
        K3s / Kubernetes 集群专属参数
      </div>

      <div class="space-y-4">
        <div>
          <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
            {{ t('admin.clusters.form.k3s_master_url', 'K3s Master API URL') }} *
          </label>
          <VInput
            v-model:value="form.k3sMasterUrl"
            :placeholder="t('admin.clusters.form.k3s_master_url_placeholder', 'https://127.0.0.1:6443')"
            clearable
          />
          <div class="text-[11px] text-google-text-secondary dark:text-google-text-darkSecondary mt-1">
            {{ t('admin.clusters.form.k3s_master_url_hint', 'K3s 平台控制面 APIServer 访问端点') }}
          </div>
        </div>

        <div>
          <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
            {{ t('admin.clusters.form.k3s_namespace', 'K3s 赛题命名空间 (Namespace)') }} *
          </label>
          <VInput
            v-model:value="form.k3sNamespace"
            :placeholder="t('admin.clusters.form.k3s_namespace_placeholder', 'volur-challenges')"
            clearable
          />
          <div class="text-[11px] text-google-text-secondary dark:text-google-text-darkSecondary mt-1">
            {{ t('admin.clusters.form.k3s_namespace_hint', '题目 Pod 资源隔离部署的 Kubernetes Namespace') }}
          </div>
        </div>

        <div>
          <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
            {{ t('admin.clusters.form.k3s_kubeconfig', 'Kubeconfig 配置文件路径') }}
          </label>
          <VInput
            v-model:value="form.k3sKubeconfigPath"
            :placeholder="t('admin.clusters.form.k3s_kubeconfig_placeholder', '/etc/rancher/k3s/k3s.yaml')"
            clearable
          />
          <div class="text-[11px] text-google-text-secondary dark:text-google-text-darkSecondary mt-1">
            {{ t('admin.clusters.form.k3s_kubeconfig_hint', 'K3s 凭证配置文件物理路径') }}
          </div>
        </div>

        <div>
          <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
            {{ t('admin.clusters.form.k3s_token', 'K3s Node Join Token 密钥') }}
          </label>
          <VInput
            v-model:value="form.k3sToken"
            type="password"
            :placeholder="t('admin.clusters.form.k3s_token_placeholder', 'K3s 节点入网认证 Token（可选）')"
            clearable
          />
          <div class="text-[11px] text-google-text-secondary dark:text-google-text-darkSecondary mt-1">
            {{ t('admin.clusters.form.k3s_token_hint', '新节点通过脚本自动加入 K3s 集群时使用的认证密钥') }}
          </div>
        </div>
      </div>
    </div>

    <div class="space-y-4 pt-4 border-t border-google-border-light dark:border-google-border-dark">
      <div class="text-xs font-bold text-google-text-primary dark:text-google-text-darkPrimary">
        集群调度与隔离策略
      </div>

      <div class="flex items-center gap-3">
        <n-switch v-model:value="form.resourceIsolationEnabled" />
        <span class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary">
          {{ t('admin.clusters.form.isolation_hint', '启用主从节点调度隔离：控制主节点仅负责平台调度，题目容器全部调度至 Worker 计算节点') }}
        </span>
      </div>
    </div>

    <div class="space-y-4 pt-4 border-t border-google-border-light dark:border-google-border-dark">
      <div class="flex items-center gap-3">
        <VBtn
          clz="normal"
          size="small"
          :left-icon="PulseOutline"
          :loading="testingConnection"
          @click="handleTestConnection"
        >
          {{ t('admin.clusters.form.test_connection', '测试集群连通性') }}
        </VBtn>

        <VBtn
          clz="primary"
          size="small"
          :left-icon="SaveOutline"
          :loading="savingSettings"
          @click="handleSaveSettings"
        >
          {{ t('admin.clusters.form.save_settings', '保存调度参数') }}
        </VBtn>
      </div>

      <div
        v-if="testResult"
        class="p-4 rounded-xl text-xs space-y-1.5"
        :class="testResult.connected ? 'bg-green-500/10 text-green-700 dark:text-green-400 border border-green-500/20' : 'bg-amber-500/10 text-amber-700 dark:text-amber-400 border border-amber-500/20'"
      >
        <div class="font-bold">
          {{ testResult.connected ? t('admin.clusters.form.test_success', '集群连通性测试通过') : t('admin.clusters.form.test_failed', '集群连通失败') }}
        </div>
        <div>{{ testResult.message }}</div>
        <div class="flex items-center gap-3 text-[11px] opacity-85 pt-1">
          <span v-if="testResult.clusterVersion">版本: {{ testResult.clusterVersion }}</span>
          <span v-if="testResult.nodeCount !== undefined">节点数: {{ testResult.nodeCount }}</span>
          <span v-if="testResult.latencyMs !== undefined">延迟: {{ testResult.latencyMs }}ms</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { NSwitch, useMessage } from 'naive-ui'
import { PulseOutline, SaveOutline } from '@vicons/ionicons5'
import VInput from '@/components/common/VInput.vue'
import VSelection from '@/components/common/VSelection.vue'
import VBtn from '@/components/common/VBtn.vue'
import {
  updateAdminClusterSettingsApi,
  testAdminClusterConnectionApi
} from '@/api/cluster'
import type {
  AdminClusterSettingsVO,
  AdminClusterSettingsDTO,
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

const form = reactive<AdminClusterSettingsDTO>({
  containerType: 'DOCKER_SWARM',
  portRangeStart: 20000,
  portRangeEnd: 40000,
  publicHost: '127.0.0.1',
  dockerSwarmApiUrl: '',
  dockerSwarmNetwork: 'volur_network',
  dockerSwarmCertPath: '',
  dockerSwarmOutboundEnabled: false,
  k3sMasterUrl: '',
  k3sNamespace: 'volur-challenges',
  k3sKubeconfigPath: '',
  k3sToken: '',
  resourceIsolationEnabled: true
})

const portRangeStartStr = computed<string>({
  get: () => String(form.portRangeStart || '20000'),
  set: (val: string) => {
    form.portRangeStart = parseInt(val, 10) || 20000
  }
})

const portRangeEndStr = computed<string>({
  get: () => String(form.portRangeEnd || '40000'),
  set: (val: string) => {
    form.portRangeEnd = parseInt(val, 10) || 40000
  }
})

const containerTypeOptions = [
  { label: 'Docker Swarm 原生集群', value: 'DOCKER_SWARM' },
  { label: 'Kubernetes / K3s 轻量级集群', value: 'K3S' }
]

watch(
  () => props.settings,
  (val) => {
    if (val) {
      Object.assign(form, {
        containerType: val.containerType || 'DOCKER_SWARM',
        portRangeStart: val.portRangeStart || 20000,
        portRangeEnd: val.portRangeEnd || 40000,
        publicHost: val.publicHost || '127.0.0.1',
        dockerSwarmApiUrl: val.dockerSwarmApiUrl || '',
        dockerSwarmNetwork: val.dockerSwarmNetwork || 'volur_network',
        dockerSwarmCertPath: val.dockerSwarmCertPath || '',
        dockerSwarmOutboundEnabled: val.dockerSwarmOutboundEnabled ?? false,
        k3sMasterUrl: val.k3sMasterUrl || '',
        k3sNamespace: val.k3sNamespace || 'volur-challenges',
        k3sKubeconfigPath: val.k3sKubeconfigPath || '',
        k3sToken: val.k3sToken || '',
        resourceIsolationEnabled: val.resourceIsolationEnabled ?? true
      })
    }
  },
  { immediate: true, deep: true }
)

async function handleSaveSettings() {
  savingSettings.value = true
  try {
    await updateAdminClusterSettingsApi(form)
    message.success(t('admin.clusters.form.save_success', '集群调度配置已保存'))
    emit('saved')
  } catch (err: any) {
    message.error(err.message || '保存集群调度设置失败')
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
        message.success(res.data.message || t('admin.clusters.form.test_success', '集群连通正常'))
      } else {
        message.warning(res.data.message || t('admin.clusters.form.test_failed', '集群连通失败'))
      }
    }
  } catch (err: any) {
    message.error(err.message || '测试集群连通性发生异常')
  } finally {
    testingConnection.value = false
  }
}
</script>
