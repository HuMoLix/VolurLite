<template>
  <n-modal
    v-model:show="visible"
    preset="card"
    :title="modalTitle"
    style="max-width: 600px; border-radius: 12px;"
    :bordered="false"
  >
    <p class="modal-subtext">{{ modalSubtitle }}</p>

    <n-form
      ref="formRef"
      :model="form"
      :rules="formRules"
      label-placement="top"
      style="margin-top: 16px;"
    >
      <!-- 节点名称 -->
      <n-form-item :label="t('admin.clusters.add_modal.node_name')" path="nodeName">
        <n-input
          v-model:value="form.nodeName"
          :placeholder="t('admin.clusters.add_modal.node_name_placeholder')"
        />
      </n-form-item>

      <!-- 节点 IP 与 SSH 端口 -->
      <n-grid :cols="24" :x-gap="12">
        <n-form-item-gi :span="16" :label="t('admin.clusters.add_modal.ip_address')" path="ipAddress">
          <n-input
            v-model:value="form.ipAddress"
            :placeholder="t('admin.clusters.add_modal.ip_address_placeholder')"
          />
        </n-form-item-gi>
        <n-form-item-gi :span="8" :label="t('admin.clusters.add_modal.ssh_port')" path="sshPort">
          <n-input-number
            v-model:value="form.sshPort"
            :min="1"
            :max="65535"
            style="width: 100%;"
          />
        </n-form-item-gi>
      </n-grid>

      <!-- 节点 IP 变更安全提示 -->
      <n-alert
        v-if="props.mode === 'edit' && form.ipAddress && form.ipAddress.trim() !== (props.node?.ipAddress || '')"
        type="warning"
        title="节点 IP 变更安全提示"
        style="margin-bottom: 16px;"
      >
        <div style="font-size: 12.5px; line-height: 1.6;">
          平台将在提交修改前严格校验当前节点是否存在运行中的容器实例（如存在将自动拦截并拒绝修改以保障安全）。在安全状态下变更 IP 后，平台将自动为新 IP 重新生成 Docker 2376 TLS 双向安全证书、重新加入 Swarm 集群，并同步镜像管理（自动从镜像拓扑中移除旧节点关联；若新节点为资源节点，将自动同步分发题目镜像）。
        </div>
      </n-alert>

      <!-- SSH 用户名与认证方式（紧接在 IP 地址下方，不设单独分界标题） -->
      <n-grid :cols="24" :x-gap="12">
        <n-form-item-gi :span="12" :label="t('admin.clusters.add_modal.ssh_user')" path="sshUser">
          <n-input
            v-model:value="form.sshUser"
            :placeholder="t('admin.clusters.add_modal.ssh_user_placeholder')"
          />
        </n-form-item-gi>
        <n-form-item-gi :span="12" label="凭据认证方式">
          <n-radio-group v-model:value="form.authType" size="small" style="margin-top: 4px;">
            <n-radio-button value="password">密码认证</n-radio-button>
            <n-radio-button value="key">私钥认证</n-radio-button>
          </n-radio-group>
        </n-form-item-gi>
      </n-grid>

      <!-- 凭据输入：密码或私钥文件导入 -->
      <n-form-item
        v-if="form.authType === 'password'"
        :label="t('admin.clusters.add_modal.ssh_password')"
        path="sshPassword"
      >
        <n-input
          v-model:value="form.sshPassword"
          type="password"
          show-password-on="click"
          :placeholder="props.mode === 'edit' ? '留空表示保持原有密码凭据不变' : t('admin.clusters.add_modal.ssh_password_placeholder')"
        />
      </n-form-item>

      <n-form-item
        v-else
        label="SSH 私钥凭据"
        path="sshKey"
      >
        <div class="key-import-wrapper">
          <!-- 已导入私钥卡片展示 -->
          <div v-if="form.sshKey" class="imported-key-card">
            <div class="key-card-left">
              <n-icon size="22" color="#10b981">
                <KeyOutline />
              </n-icon>
              <div class="key-card-info">
                <div class="key-file-name">{{ keyFileName || '已载入 SSH 私钥凭据' }}</div>
                <div class="key-file-meta">
                  <n-tag size="tiny" type="success" round>已读取</n-tag>
                  <span v-if="keyFileSize > 0" class="file-size-text">{{ (keyFileSize / 1024).toFixed(1) }} KB</span>
                </div>
              </div>
            </div>
            <div class="key-card-actions">
              <n-upload
                :show-file-list="false"
                :default-upload="false"
                accept=".pem,.key,.pub,id_rsa,id_ed25519,id_ecdsa,*"
                @change="handleKeyFileChange"
              >
                <n-button size="tiny" secondary type="primary">
                  重新导入
                </n-button>
              </n-upload>
              <n-button size="tiny" quaternary type="error" @click="clearKeyFile">
                清空
              </n-button>
            </div>
          </div>

          <!-- 未导入时显示拖拽/点击上传区 -->
          <n-upload
            v-else
            :show-file-list="false"
            :default-upload="false"
            accept=".pem,.key,.pub,id_rsa,id_ed25519,id_ecdsa,*"
            @change="handleKeyFileChange"
          >
            <n-upload-dragger>
              <div class="upload-dragger-content">
                <n-icon size="26" :depth="3">
                  <CloudUploadOutline />
                </n-icon>
                <div class="upload-title">点击或拖拽私钥文件至此处导入</div>
                <div class="upload-hint">
                  {{ props.mode === 'edit' ? '留空表示保持原有私钥凭据不变；支持 .pem, .key, id_rsa 等文本私钥' : '支持 OpenSSH 私钥、RSA / ED25519 (.pem, .key, id_rsa 等文本文件)' }}
                </div>
              </div>
            </n-upload-dragger>
          </n-upload>

          <div v-if="props.mode === 'edit' && !form.sshKey" class="existing-key-hint">
            <n-icon size="14" style="margin-right: 4px;">
              <KeyOutline />
            </n-icon>
            <span>当前节点若已配置私钥，未导入新私钥文件时将继续保持原有凭据不变。</span>
          </div>
        </div>
      </n-form-item>

      <!-- 节点角色与资源调度分配 -->
      <n-form-item :label="t('admin.clusters.add_modal.role')" path="role">
        <n-select
          v-model:value="form.role"
          :options="roleOptions"
        />
      </n-form-item>

      <n-form-item :label="t('admin.clusters.add_modal.is_resource')">
        <div class="modal-switch-box">
          <n-switch
            v-model:value="form.isResourceNode"
          />
          <span class="field-hint">
            {{ form.isResourceNode ? '启用后，该节点可承载运行题目靶机容器（需节点已加入集群）' : '关闭后，平台不会将题目容器调度至该节点' }}
          </span>
        </div>
      </n-form-item>

      <!-- 添加节点特有：Docker 运行时要求与 2376+TLS 双向证书自动化安全纳管提示 -->
      <div v-if="props.mode === 'add'" class="tls-security-tip" style="margin-bottom: 16px;">
        <n-alert type="info" :bordered="false">
          <template #icon>
            <n-icon :component="ShieldCheckmarkOutline" />
          </template>
          <div><strong>Docker 运行时与 TLS 2376 安全标准：</strong></div>
          <div style="font-size: 12px; margin-top: 2px;">
            目标节点须已预先安装并启动 Docker 服务。连接成功后，平台将自动为该节点配置 2376 端口与 TLS 双向安全证书认证。
          </div>
        </n-alert>
      </div>

      <!-- 修改节点特有：存活测试区域 -->
      <template v-if="props.mode === 'edit'">
        <n-divider dashed title-placement="left">{{ t('admin.clusters.edit_modal.live_test_section') }}</n-divider>

        <div class="live-test-box">
          <div class="live-test-action-row">
            <span class="field-hint">{{ t('admin.clusters.edit_modal.live_test_hint') }}</span>
            <n-button
              size="small"
              secondary
              type="warning"
              :loading="testingConnection"
              @click="handleTestConnection"
            >
              <template #icon>
                <n-icon :component="PulseOutline" />
              </template>
              {{ t('admin.clusters.edit_modal.live_test_btn') }}
            </n-button>
          </div>

          <n-alert
            v-if="nodeTestResult"
            :type="nodeTestResult.connected ? 'success' : 'error'"
            :title="nodeTestResult.connected ? '节点连通正常' : '节点连通失败'"
            style="margin-top: 12px;"
          >
            <div>{{ nodeTestResult.message }}</div>
            <div v-if="nodeTestResult.latencyMs !== undefined" style="margin-top: 4px; font-size: 12px; opacity: 0.85;">
              网络响应耗时: {{ nodeTestResult.latencyMs }} ms
              <span v-if="nodeTestResult.clusterVersion" style="margin-left: 12px;">
                Docker 版本: {{ nodeTestResult.clusterVersion }}
              </span>
            </div>
          </n-alert>
        </div>
      </template>
    </n-form>

    <template #footer>
      <div class="modal-footer">
        <n-button quaternary @click="handleClose">
          {{ t('admin.clusters.add_modal.cancel') }}
        </n-button>
        <n-button
          type="primary"
          class="theme-primary-btn"
          :loading="submitting"
          @click="handleSubmit"
        >
          {{ props.mode === 'add' ? t('admin.clusters.add_modal.submit') : t('admin.clusters.edit_modal.submit') }}
        </n-button>
      </div>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  NModal,
  NForm,
  NFormItem,
  NFormItemGi,
  NGrid,
  NInput,
  NInputNumber,
  NSelect,
  NSwitch,
  NDivider,
  NRadioGroup,
  NRadioButton,
  NButton,
  NIcon,
  NAlert,
  NTag,
  NUpload,
  NUploadDragger,
  useMessage,
  type SelectOption,
  type FormInst,
  type FormRules,
  type UploadFileInfo
} from 'naive-ui'
import {
  CloudUploadOutline,
  KeyOutline,
  PulseOutline,
  ShieldCheckmarkOutline
} from '@vicons/ionicons5'
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
    ? t('admin.clusters.add_modal.title')
    : t('admin.clusters.edit_modal.title')
})

const modalSubtitle = computed(() => {
  return props.mode === 'add'
    ? t('admin.clusters.add_modal.subtitle')
    : t('admin.clusters.edit_modal.subtitle')
})

const roleOptions = computed<SelectOption[]>(() => [
  { label: t('admin.clusters.nodes.role_worker'), value: 'WORKER' },
  { label: `${t('admin.clusters.nodes.role_master')} (仅限本机平台控制面)`, value: 'MASTER', disabled: true }
])

const formRef = ref<FormInst | null>(null)
const submitting = ref(false)
const testingConnection = ref(false)
const nodeTestResult = ref<AdminContainerTestConnectionVO | null>(null)

// File upload state for SSH private key
const keyFileName = ref('')
const keyFileSize = ref(0)

const form = reactive({
  nodeName: '',
  ipAddress: '',
  sshPort: 22,
  clusterType: 'DOCKER_SWARM',
  role: 'WORKER',
  isResourceNode: true,
  enableSshAuth: true,
  sshUser: 'root',
  authType: 'password' as 'password' | 'key',
  sshPassword: '',
  sshKey: ''
})

const formRules = computed<FormRules>(() => ({
  nodeName: [
    { required: true, message: () => t('admin.clusters.add_modal.node_name_required'), trigger: 'blur' },
    { min: 2, max: 64, message: '节点名称长度在2-64个字符之间', trigger: 'blur' }
  ],
  ipAddress: [
    {
      validator(_rule, value) {
        if (!value) {
          return new Error(t('admin.clusters.add_modal.ip_address_required'))
        }
        if (!/^((25[0-5]|(2[0-4]|1\d|[1-9]|)\d)\.?\b){4}$|^localhost$/.test(value)) {
          return new Error('请输入规范的 IPv4 地址')
        }
        return true
      },
      trigger: ['blur', 'input']
    }
  ],
  sshPort: [
    { required: true, type: 'number', message: 'SSH 端口不能为空', trigger: 'blur' }
  ],
  sshUser: [
    { required: true, message: 'SSH 登录用户名不能为空', trigger: 'blur' },
    { min: 1, max: 64, message: 'SSH 用户名长度在1-64个字符之间', trigger: 'blur' }
  ],
  sshPassword: [
    {
      validator(_rule, value) {
        if (props.mode === 'add' && form.authType === 'password' && !value) {
          return new Error(t('admin.clusters.add_modal.ssh_password_required'))
        }
        return true
      },
      trigger: ['blur', 'input']
    }
  ],
  sshKey: [
    {
      validator(_rule, value) {
        if (props.mode === 'add' && form.authType === 'key' && !value) {
          return new Error('请导入 SSH 私钥文件')
        }
        return true
      },
      trigger: ['blur', 'change']
    }
  ]
}))

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
    form.enableSshAuth = true
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
    form.enableSshAuth = true
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
      message.warning('所选私钥文件内容为空，请选择有效的私钥文件')
      return
    }
    form.sshKey = text.trim()
    keyFileName.value = rawFile.name
    keyFileSize.value = rawFile.size
    message.success(`已成功导入私钥文件: ${rawFile.name}`)
  } catch (err) {
    message.error('读取私钥文件失败，请确保文件具有读取权限')
  }
}

function clearKeyFile() {
  form.sshKey = ''
  keyFileName.value = ''
  keyFileSize.value = 0
}

function handleClose() {
  visible.value = false
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
  } catch {
    message.error('执行节点存活测试异常')
  } finally {
    testingConnection.value = false
  }
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()

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
      message.success(t('admin.clusters.add_modal.success_notice'))
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
        message.warning('节点配置已更新；该节点尚未入网，暂未激活为题目资源节点，请在配置网络后点击“加入集群”', { duration: 6000 })
      } else {
        message.success(t('admin.clusters.edit_modal.success_notice'))
      }
    }

    emit('update:show', false)
    emit('success')
  } catch {
    // Handled by Axios interceptor
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
.modal-subtext {
  font-size: 13px;
  color: #444444;
  margin: 0;
}

.modal-switch-box {
  display: flex;
  align-items: center;
}

.field-hint {
  font-size: 12px;
  color: #444444;
  margin-left: 8px;
}

.auto-docker-box {
  display: flex;
  flex-direction: column;
}

.key-import-wrapper {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.upload-dragger-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 12px 16px;
  gap: 6px;

  .upload-title {
    font-size: 13px;
    font-weight: 500;
    color: var(--v-text-primary, #111111);
  }

  .upload-hint {
    font-size: 12px;
    color: #666666;
  }
}

.imported-key-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background-color: var(--v-bg-card, #f9fafb);
  border: 1px solid var(--v-border-base, #e5e7eb);
  border-radius: 8px;
  transition: all 0.2s ease;

  .key-card-left {
    display: flex;
    align-items: center;
    gap: 10px;

    .key-card-info {
      display: flex;
      flex-direction: column;
      gap: 2px;

      .key-file-name {
        font-size: 13px;
        font-weight: 600;
        color: var(--v-text-primary, #111111);
      }

      .key-file-meta {
        display: flex;
        align-items: center;
        gap: 6px;

        .file-size-text {
          font-size: 11px;
          color: #666666;
          font-family: 'Fira Code', monospace;
        }
      }
    }
  }

  .key-card-actions {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.existing-key-hint {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #666666;
  margin-top: 4px;
}

.live-test-box {
  width: 100%;

  .live-test-action-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
  }
}

.modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
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
