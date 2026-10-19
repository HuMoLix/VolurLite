<template>
  <VModal
    v-model:show="visible"
    :title="modalTitle"
    :description="modalSubtitle"
    :max-width="620"
  >
    <div class="space-y-4">
      <div>
        <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
          {{ t('admin.clusters.add_modal.node_name', '节点名称') }} *
        </label>
        <VInput
          v-model:value="form.nodeName"
          :placeholder="t('admin.clusters.add_modal.node_name_placeholder', '例如: worker-node-01')"
          clearable
        />
      </div>

      <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <div class="sm:col-span-2">
          <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
            {{ t('admin.clusters.add_modal.ip_address', '节点 IP 地址') }} *
          </label>
          <VInput
            v-model:value="form.ipAddress"
            :placeholder="t('admin.clusters.add_modal.ip_address_placeholder', '192.168.1.10')"
            clearable
          />
        </div>

        <div>
          <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
            {{ t('admin.clusters.add_modal.ssh_port', 'SSH 端口') }} *
          </label>
          <VInput
            v-model:value="sshPortStr"
            placeholder="22"
            clearable
          />
        </div>
      </div>

      <div
        v-if="props.mode === 'edit' && form.ipAddress && form.ipAddress.trim() !== (props.node?.ipAddress || '')"
        class="p-3 rounded-xl bg-amber-500/10 border border-amber-500/20 text-amber-700 dark:text-amber-400 text-xs leading-relaxed"
      >
        平台将在提交前严格校验当前节点是否存在运行中的容器实例。变更 IP 后，平台将自动为新 IP 重新签发 Docker 2376 TLS 双向安全证书、重新编排加入集群并同步题目镜像。
      </div>

      <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <div>
          <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
            {{ t('admin.clusters.add_modal.ssh_user', 'SSH 登录用户名') }} *
          </label>
          <VInput
            v-model:value="form.sshUser"
            :placeholder="t('admin.clusters.add_modal.ssh_user_placeholder', 'root')"
            clearable
          />
        </div>

        <div>
          <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
            凭据认证方式
          </label>
          <div class="flex items-center gap-2 pt-1">
            <VBtn
              :clz="form.authType === 'password' ? 'primary' : 'normal'"
              size="small"
              class="flex-1"
              @click="form.authType = 'password'"
            >
              密码认证
            </VBtn>
            <VBtn
              :clz="form.authType === 'key' ? 'primary' : 'normal'"
              size="small"
              class="flex-1"
              @click="form.authType = 'key'"
            >
              私钥认证
            </VBtn>
          </div>
        </div>
      </div>

      <div v-if="form.authType === 'password'">
        <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
          {{ t('admin.clusters.add_modal.ssh_password', 'SSH 登录密码') }}
          <span v-if="props.mode === 'add'">*</span>
        </label>
        <VInput
          v-model:value="form.sshPassword"
          type="password"
          :placeholder="props.mode === 'edit' ? '留空表示保持原有密码凭据不变' : t('admin.clusters.add_modal.ssh_password_placeholder', '请输入 SSH 登录密码')"
          clearable
        />
      </div>

      <div v-else class="space-y-2">
        <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
          SSH 私钥凭据
          <span v-if="props.mode === 'add'">*</span>
        </label>

        <div v-if="form.sshKey" class="p-3 rounded-xl border border-google-border-light dark:border-google-border-dark bg-google-surface-light dark:bg-google-surface-dark flex items-center justify-between gap-3">
          <div class="flex items-center gap-2.5 min-w-0">
            <div class="w-8 h-8 rounded-lg bg-green-500/10 text-green-600 dark:text-green-400 flex items-center justify-center shrink-0">
              <KeyOutline class="w-4 h-4" />
            </div>
            <div class="min-w-0">
              <div class="text-xs font-semibold text-google-text-primary dark:text-google-text-darkPrimary truncate">
                {{ keyFileName || '已载入 SSH 私钥凭据' }}
              </div>
              <div class="text-[11px] text-google-text-secondary dark:text-google-text-darkSecondary">
                {{ keyFileSize > 0 ? `${(keyFileSize / 1024).toFixed(1)} KB` : '私钥内容已就绪' }}
              </div>
            </div>
          </div>

          <div class="flex items-center gap-2">
            <n-upload
              :show-file-list="false"
              :default-upload="false"
              accept=".pem,.key,.pub,id_rsa,id_ed25519,id_ecdsa,*"
              @change="handleKeyFileChange"
            >
              <VBtn clz="normal" size="tiny">
                重新选择
              </VBtn>
            </n-upload>
            <VBtn clz="danger" size="tiny" @click="clearKeyFile">
              清除
            </VBtn>
          </div>
        </div>

        <div v-else class="space-y-2">
          <n-upload
            :show-file-list="false"
            :default-upload="false"
            accept=".pem,.key,.pub,id_rsa,id_ed25519,id_ecdsa,*"
            @change="handleKeyFileChange"
          >
            <div class="p-4 rounded-xl border border-dashed border-google-border-light dark:border-google-border-dark hover:border-google-blue dark:hover:border-google-blue-dark transition-colors cursor-pointer text-center">
              <CloudUploadOutline class="w-6 h-6 mx-auto text-google-text-muted dark:text-google-text-darkMuted mb-1.5" />
              <div class="text-xs font-medium text-google-text-primary dark:text-google-text-darkPrimary">
                点击上传私钥文件 (.pem, .key, id_rsa 等)
              </div>
              <div class="text-[11px] text-google-text-secondary dark:text-google-text-darkSecondary mt-0.5">
                支持 OpenSSH、RSA、ED25519 等格式私钥
              </div>
            </div>
          </n-upload>

          <VInput
            v-model:value="form.sshKey"
            type="textarea"
            :rows="3"
            placeholder="或在此处直接粘贴私钥文本（-----BEGIN OPENSSH PRIVATE KEY-----）"
            clearable
          />
        </div>

        <div v-if="props.mode === 'edit' && !form.sshKey" class="text-[11px] text-google-text-secondary dark:text-google-text-darkSecondary">
          当前节点若已配置私钥，留空将继续沿用原有凭据。
        </div>
      </div>

      <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <div>
          <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
            {{ t('admin.clusters.add_modal.role', '节点角色') }} *
          </label>
          <VSelection
            v-model:value="form.role"
            :options="roleOptions"
          />
        </div>

        <div>
          <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
            {{ t('admin.clusters.add_modal.is_resource', '赛题容器调度') }}
          </label>
          <div class="flex items-center gap-3 pt-2">
            <n-switch v-model:value="form.isResourceNode" />
            <span class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary">
              {{ form.isResourceNode ? '承载运行赛题靶机容器' : '仅作管理，不分配靶机' }}
            </span>
          </div>
        </div>
      </div>

      <div
        v-if="props.mode === 'add'"
        class="p-3 rounded-xl bg-google-blue/10 border border-google-blue/20 text-google-blue dark:text-google-blue-dark text-xs flex items-start gap-2.5"
      >
        <ShieldCheckmarkOutline class="w-4 h-4 shrink-0 mt-0.5" />
        <div>
          <span class="font-semibold">Docker 运行时与 TLS 2376 安全标准：</span>
          目标节点须已启动 Docker。节点连通后，平台将自动为该节点配置 2376 端口与 TLS 双向安全证书认证。
        </div>
      </div>

      <template v-if="props.mode === 'edit'">
        <div class="pt-3 border-t border-google-border-light dark:border-google-border-dark space-y-3">
          <div class="flex items-center justify-between">
            <span class="text-xs font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
              {{ t('admin.clusters.edit_modal.live_test_section', '节点连通性实时测试') }}
            </span>
            <VBtn
              clz="normal"
              size="tiny"
              :left-icon="PulseOutline"
              :loading="testingConnection"
              @click="handleTestConnection"
            >
              {{ t('admin.clusters.edit_modal.live_test_btn', '测试连通性') }}
            </VBtn>
          </div>

          <div
            v-if="nodeTestResult"
            class="p-3 rounded-xl text-xs space-y-1"
            :class="nodeTestResult.connected ? 'bg-green-500/10 text-green-700 dark:text-green-400 border border-green-500/20' : 'bg-red-500/10 text-red-700 dark:text-red-400 border border-red-500/20'"
          >
            <div class="font-semibold">{{ nodeTestResult.connected ? '连通性测试通过' : '连通性测试失败' }}</div>
            <div>{{ nodeTestResult.message }}</div>
            <div v-if="nodeTestResult.latencyMs !== undefined" class="text-[11px] opacity-80">
              网络延迟: {{ nodeTestResult.latencyMs }} ms
              <span v-if="nodeTestResult.clusterVersion" class="ml-2">版本: {{ nodeTestResult.clusterVersion }}</span>
            </div>
          </div>
        </div>
      </template>
    </div>

    <template #footer>
      <div class="flex justify-end gap-2.5">
        <VBtn clz="normal" size="small" @click="visible = false">
          {{ t('common.cancel', '取消') }}
        </VBtn>
        <VBtn clz="primary" size="small" :loading="submitting" @click="handleSubmit">
          {{ props.mode === 'add' ? t('admin.clusters.add_modal.submit', '确认添加') : t('admin.clusters.edit_modal.submit', '保存修改') }}
        </VBtn>
      </div>
    </template>
  </VModal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { NSwitch, NUpload, useMessage, type UploadFileInfo } from 'naive-ui'
import {
  KeyOutline,
  CloudUploadOutline,
  ShieldCheckmarkOutline,
  PulseOutline
} from '@vicons/ionicons5'
import VModal from '@/components/common/VModal.vue'
import VInput from '@/components/common/VInput.vue'
import VSelection from '@/components/common/VSelection.vue'
import VBtn from '@/components/common/VBtn.vue'
import {
  addAdminClusterNodeApi,
  updateAdminClusterNodeApi,
  testAdminClusterNodeConnectionApi
} from '@/api/cluster'
import type {
  AdminClusterNodeVO,
  AdminClusterNodeCreateDTO,
  AdminClusterNodeUpdateDTO,
  AdminContainerTestConnectionVO
} from '@/types/cluster'

interface Props {
  show: boolean
  mode: 'add' | 'edit'
  node?: AdminClusterNodeVO | null
  clusterType?: string
}

const props = withDefaults(defineProps<Props>(), {
  show: false,
  mode: 'add',
  node: null,
  clusterType: 'DOCKER_SWARM'
})

const emit = defineEmits<{
  (e: 'update:show', val: boolean): void
  (e: 'success'): void
}>()

const { t } = useI18n()
const message = useMessage()

const visible = computed({
  get: () => props.show,
  set: (val: boolean) => emit('update:show', val)
})

const modalTitle = computed(() => {
  return props.mode === 'add'
    ? t('admin.clusters.add_modal.title', '添加集群计算节点')
    : t('admin.clusters.edit_modal.title', '编辑集群计算节点')
})

const modalSubtitle = computed(() => {
  return props.mode === 'add'
    ? t('admin.clusters.add_modal.subtitle', '将外部计算实例纳管加入调度集群')
    : t('admin.clusters.edit_modal.subtitle', '调整计算节点的网络、凭据与调度策略')
})

const roleOptions = [
  { label: '工作负载节点 (WORKER)', value: 'WORKER' },
  { label: '控制主节点 (MASTER - 仅限控制面)', value: 'MASTER', disabled: true }
]

const submitting = ref(false)
const testingConnection = ref(false)
const nodeTestResult = ref<AdminContainerTestConnectionVO | null>(null)

const keyFileName = ref('')
const keyFileSize = ref(0)

const form = reactive({
  nodeName: '',
  ipAddress: '',
  sshPort: 22,
  clusterType: 'DOCKER_SWARM',
  role: 'WORKER',
  isResourceNode: true,
  sshUser: 'root',
  authType: 'password' as 'password' | 'key',
  sshPassword: '',
  sshKey: ''
})

const sshPortStr = computed<string>({
  get: () => String(form.sshPort || '22'),
  set: (val: string) => {
    form.sshPort = parseInt(val, 10) || 22
  }
})

function initForm() {
  nodeTestResult.value = null
  keyFileName.value = ''
  keyFileSize.value = 0

  if (props.mode === 'edit' && props.node) {
    form.nodeName = props.node.nodeName || ''
    form.ipAddress = props.node.ipAddress || ''
    form.sshPort = props.node.sshPort || 22
    form.clusterType = props.node.clusterType || 'DOCKER_SWARM'
    form.role = props.node.role || 'WORKER'
    form.isResourceNode = props.node.isResourceNode ?? true
    form.sshUser = props.node.sshUser || 'root'
    form.authType = (props.node.authType as 'password' | 'key') || 'password'
    form.sshPassword = ''
    form.sshKey = ''
  } else {
    form.nodeName = ''
    form.ipAddress = ''
    form.sshPort = 22
    form.clusterType = props.clusterType || 'DOCKER_SWARM'
    form.role = 'WORKER'
    form.isResourceNode = true
    form.sshUser = 'root'
    form.authType = 'password'
    form.sshPassword = ''
    form.sshKey = ''
  }
}

watch(
  () => props.show,
  (val) => {
    if (val) initForm()
  },
  { immediate: true }
)

watch(
  () => props.node,
  () => {
    if (props.show) initForm()
  }
)

async function handleKeyFileChange(data: { file: UploadFileInfo }) {
  const rawFile = data.file.file
  if (!rawFile) return

  try {
    const text = await rawFile.text()
    if (!text || !text.trim()) {
      message.warning('所选私钥文件内容为空')
      return
    }
    form.sshKey = text.trim()
    keyFileName.value = rawFile.name
    keyFileSize.value = rawFile.size
    message.success(`已导入私钥文件: ${rawFile.name}`)
  } catch {
    message.error('读取私钥文件失败')
  }
}

function clearKeyFile() {
  form.sshKey = ''
  keyFileName.value = ''
  keyFileSize.value = 0
}

async function handleTestConnection() {
  if (!props.node?.id) return
  testingConnection.value = true
  nodeTestResult.value = null
  try {
    const payload: AdminClusterNodeUpdateDTO = {
      nodeName: form.nodeName.trim(),
      ipAddress: form.ipAddress.trim(),
      sshPort: form.sshPort,
      sshUser: form.sshUser.trim(),
      sshPassword: form.authType === 'password' && form.sshPassword.trim() ? form.sshPassword.trim() : undefined,
      sshKey: form.authType === 'key' && form.sshKey.trim() ? form.sshKey.trim() : undefined,
      role: form.role,
      isResourceNode: form.isResourceNode
    }
    const res = await testAdminClusterNodeConnectionApi(props.node.id, payload)
    if (res.data) {
      nodeTestResult.value = res.data
      if (res.data.connected) {
        message.success(res.data.message || '节点存活测试通过')
      } else {
        message.warning(res.data.message || '节点存活测试失败')
      }
    }
  } catch (err: any) {
    message.error(err.message || '执行节点存活测试异常')
  } finally {
    testingConnection.value = false
  }
}

async function handleSubmit() {
  if (!form.nodeName.trim()) {
    message.warning('请输入节点名称')
    return
  }
  if (!form.ipAddress.trim()) {
    message.warning('请输入节点 IP 地址')
    return
  }
  if (!/^((25[0-5]|(2[0-4]|1\d|[1-9]|)\d)\.?\b){4}$|^localhost$/.test(form.ipAddress.trim())) {
    message.warning('请输入规范的 IPv4 地址或 localhost')
    return
  }
  if (props.mode === 'add') {
    if (form.authType === 'password' && !form.sshPassword.trim()) {
      message.warning('请输入 SSH 登录密码')
      return
    }
    if (form.authType === 'key' && !form.sshKey.trim()) {
      message.warning('请导入或填写 SSH 私钥凭据')
      return
    }
  }

  submitting.value = true
  try {
    if (props.mode === 'add') {
      const payload: AdminClusterNodeCreateDTO = {
        nodeName: form.nodeName.trim(),
        ipAddress: form.ipAddress.trim(),
        sshPort: form.sshPort,
        clusterType: form.clusterType,
        role: form.role,
        isResourceNode: form.isResourceNode,
        enableSshAuth: true,
        sshUser: form.sshUser.trim(),
        sshPassword: form.authType === 'password' && form.sshPassword.trim() ? form.sshPassword.trim() : undefined,
        sshKey: form.authType === 'key' && form.sshKey.trim() ? form.sshKey.trim() : undefined
      }
      await addAdminClusterNodeApi(payload)
      message.success('节点已成功加入集群')
    } else {
      if (!props.node?.id) return
      const payload: AdminClusterNodeUpdateDTO = {
        nodeName: form.nodeName.trim(),
        ipAddress: form.ipAddress.trim(),
        sshPort: form.sshPort,
        sshUser: form.sshUser.trim(),
        sshPassword: form.authType === 'password' && form.sshPassword.trim() ? form.sshPassword.trim() : undefined,
        sshKey: form.authType === 'key' && form.sshKey.trim() ? form.sshKey.trim() : undefined,
        role: form.role,
        isResourceNode: form.isResourceNode
      }
      const res = await updateAdminClusterNodeApi(props.node.id, payload)
      if (res.data && !res.data.inCluster) {
        message.warning('节点配置已更新；该节点尚未入网，请在配置网络后点击“加入集群”', { duration: 6000 })
      } else {
        message.success('节点信息已更新')
      }
    }

    emit('update:show', false)
    emit('success')
  } catch (err: any) {
    message.error(err.message || '保存节点失败')
  } finally {
    submitting.value = false
  }
}
</script>
