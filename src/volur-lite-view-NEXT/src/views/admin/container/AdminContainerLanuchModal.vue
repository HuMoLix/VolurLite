<template>
  <VModal
    :show="show"
    :title="'启动容器'"
    :width="680"
    :max-height="'86vh'"
    @update:show="handleUpdateShow"
  >
    <div class="flex flex-col gap-3 py-1">
      <div class="flex flex-col gap-2.5 p-3 sm:p-3.5 rounded-xl border border-google-border-light dark:border-google-border-dark bg-white dark:bg-google-surface-darkCard/80">
        <div class="flex items-center justify-between border-b border-google-border-light/60 dark:border-google-border-dark/60 pb-2">
          <span class="text-xs font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
            基础设置
          </span>
        </div>

        <div class="flex flex-col gap-1.5">
          <label class="text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary">
            镜像选择 *
          </label>
          <VSelection
            v-model:value="form.imageName"
            :options="imageOptions"
            :loading="loadingImages"
            placeholder="请选择 registry 中存在的镜像"
            filterable
            clearable
            class="w-full"
          />
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <div class="flex flex-col gap-1.5">
            <label class="text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary">
              容器名称
            </label>
            <VInput
              v-model:value="form.containerName"
              placeholder="自定义容器名称 (留空随机生成)"
              clearable
              class="w-full"
            />
          </div>

          <div class="flex flex-col gap-1.5">
            <label class="text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary">
              运行节点
            </label>
            <VSelection
              v-model:value="form.nodeIp"
              :options="nodeOptions"
              :loading="loadingNodes"
              placeholder="自动调度 (集群推荐节点)"
              clearable
              class="w-full"
            />
          </div>
        </div>
      </div>

      <div class="flex flex-col gap-3 p-3 sm:p-3.5 rounded-xl border border-google-border-light dark:border-google-border-dark bg-white dark:bg-google-surface-darkCard/80">
        <div class="flex items-center justify-between border-b border-google-border-light/60 dark:border-google-border-dark/60 pb-2">
          <span class="text-xs font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
            高级设置
          </span>
        </div>

        <div class="flex flex-col gap-2">
          <div class="flex items-center justify-between">
            <span class="text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary">
              端口映射 (Port mappings)
            </span>
            <VBtn
              clz="normal"
              size="tiny"
              :left-icon="AddOutline"
              @click="addPortMapping"
            >
              添加端口
            </VBtn>
          </div>

          <div v-if="ports.length === 0" class="text-xs text-google-text-muted dark:text-google-text-darkMuted py-1 italic">
            未配置端口映射 (默认暴露镜像原生端口)
          </div>

          <div v-else class="flex flex-col gap-2">
            <div
              v-for="(item, index) in ports"
              :key="index"
              class="grid grid-cols-12 gap-2 items-center"
            >
              <div class="col-span-4">
                <VInput
                  v-model:value="item.hostPort"
                  placeholder="主机端口 (如 8080)"
                  size="small"
                  clearable
                />
              </div>
              <div class="col-span-4">
                <VInput
                  v-model:value="item.containerPort"
                  placeholder="容器端口 (如 80)"
                  size="small"
                  clearable
                />
              </div>
              <div class="col-span-3">
                <VSelection
                  v-model:value="item.protocol"
                  :options="protocolOptions"
                  size="small"
                />
              </div>
              <div class="col-span-1 flex justify-center">
                <VBtn
                  clz="danger"
                  size="tiny"
                  :left-icon="TrashOutline"
                  @click="removePortMapping(index)"
                />
              </div>
            </div>
          </div>
        </div>

        <div class="flex flex-col gap-2 pt-2 border-t border-google-border-light/40 dark:border-google-border-dark/40">
          <div class="flex items-center justify-between">
            <span class="text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary">
              数据卷挂载 (Volumes)
            </span>
            <VBtn
              clz="normal"
              size="tiny"
              :left-icon="AddOutline"
              @click="addVolumeMapping"
            >
              添加挂载
            </VBtn>
          </div>

          <div v-if="volumes.length === 0" class="text-xs text-google-text-muted dark:text-google-text-darkMuted py-1 italic">
            未挂载宿主机目录
          </div>

          <div v-else class="flex flex-col gap-2">
            <div
              v-for="(item, index) in volumes"
              :key="index"
              class="grid grid-cols-12 gap-2 items-center"
            >
              <div class="col-span-4">
                <VInput
                  v-model:value="item.hostPath"
                  placeholder="宿主机路径 (如 /opt/app)"
                  size="small"
                  clearable
                />
              </div>
              <div class="col-span-4">
                <VInput
                  v-model:value="item.containerPath"
                  placeholder="容器内路径 (如 /app)"
                  size="small"
                  clearable
                />
              </div>
              <div class="col-span-3">
                <VSelection
                  v-model:value="item.mode"
                  :options="volumeModeOptions"
                  size="small"
                />
              </div>
              <div class="col-span-1 flex justify-center">
                <VBtn
                  clz="danger"
                  size="tiny"
                  :left-icon="TrashOutline"
                  @click="removeVolumeMapping(index)"
                />
              </div>
            </div>
          </div>
        </div>

        <div class="flex flex-col gap-2 pt-2 border-t border-google-border-light/40 dark:border-google-border-dark/40">
          <div class="flex items-center justify-between">
            <span class="text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary">
              环境变量 (Environment variables)
            </span>
            <VBtn
              clz="normal"
              size="tiny"
              :left-icon="AddOutline"
              @click="addEnvironment"
            >
              添加环境变量
            </VBtn>
          </div>

          <div v-if="environments.length === 0" class="text-xs text-google-text-muted dark:text-google-text-darkMuted py-1 italic">
            未配置自定义环境变量
          </div>

          <div v-else class="flex flex-col gap-2">
            <div
              v-for="(item, index) in environments"
              :key="index"
              class="grid grid-cols-12 gap-2 items-center"
            >
              <div class="col-span-5">
                <VInput
                  v-model:value="item.name"
                  placeholder="变量名 (如 PORT)"
                  size="small"
                  clearable
                />
              </div>
              <div class="col-span-6">
                <VInput
                  v-model:value="item.value"
                  placeholder="变量值 (如 8080)"
                  size="small"
                  clearable
                />
              </div>
              <div class="col-span-1 flex justify-center">
                <VBtn
                  clz="danger"
                  size="tiny"
                  :left-icon="TrashOutline"
                  @click="removeEnvironment(index)"
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="flex flex-col gap-2.5 p-3 sm:p-3.5 rounded-xl border border-google-border-light dark:border-google-border-dark bg-white dark:bg-google-surface-darkCard/80">
        <div class="flex items-center justify-between border-b border-google-border-light/60 dark:border-google-border-dark/60 pb-2">
          <span class="text-xs font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
            资源配置
          </span>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <div class="flex flex-col gap-1.5">
            <label class="text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary">
              CPU 限制 (核)
            </label>
            <VInput
              v-model:value="cpuLimitStr"
              placeholder="如 1 或 2 (留空不限)"
              clearable
              class="w-full"
            />
          </div>

          <div class="flex flex-col gap-1.5">
            <label class="text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary">
              内存限制 (MB)
            </label>
            <VInput
              v-model:value="memoryLimitStr"
              placeholder="如 512 或 1024"
              clearable
              class="w-full"
            />
          </div>

          <div class="flex flex-col gap-1.5">
            <label class="text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary">
              容器时间 (分钟)
            </label>
            <VInput
              v-model:value="durationMinutesStr"
              placeholder="60"
              clearable
              class="w-full"
            />
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="flex items-center justify-end w-full">
        <VBtn
          clz="primary"
          size="medium"
          :loading="submitting"
          @click="handleLaunch"
        >
          启动
        </VBtn>
      </div>
    </template>
  </VModal>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useMessage, type SelectOption } from 'naive-ui'
import { AddOutline, TrashOutline } from '@vicons/ionicons5'
import VModal from '@/components/common/VModal.vue'
import VBtn from '@/components/common/VBtn.vue'
import VInput from '@/components/common/VInput.vue'
import VSelection from '@/components/common/VSelection.vue'
import { manualStartAdminContainerApi, getAdminContainerCandidateNodesApi } from '@/api/container'
import { getAdminImageListApi } from '@/api/image'

defineOptions({
  name: 'AdminContainerLanuchModal'
})

interface PortMappingItem {
  hostPort: string
  containerPort: string
  protocol: 'TCP' | 'UDP'
}

interface VolumeMappingItem {
  hostPath: string
  containerPath: string
  mode: 'rw' | 'ro'
}

interface EnvVarItem {
  name: string
  value: string
}

const props = withDefaults(
  defineProps<{
    show: boolean
  }>(),
  {
    show: false
  }
)

const emit = defineEmits<{
  'update:show': [value: boolean]
  success: []
}>()

const message = useMessage()

const submitting = ref(false)
const loadingImages = ref(false)
const loadingNodes = ref(false)

const form = ref({
  imageName: '',
  containerName: '',
  nodeIp: ''
})

const cpuLimitStr = ref('')
const memoryLimitStr = ref('')
const durationMinutesStr = ref('60')

const ports = ref<PortMappingItem[]>([])
const volumes = ref<VolumeMappingItem[]>([])
const environments = ref<EnvVarItem[]>([])

const imageOptions = ref<SelectOption[]>([])
const nodeOptions = ref<SelectOption[]>([])

const protocolOptions: SelectOption[] = [
  { label: 'TCP', value: 'TCP' },
  { label: 'UDP', value: 'UDP' }
]

const volumeModeOptions: SelectOption[] = [
  { label: '读写 (rw)', value: 'rw' },
  { label: '只读 (ro)', value: 'ro' }
]

function handleUpdateShow(val: boolean) {
  emit('update:show', val)
}

function addPortMapping() {
  ports.value.push({
    hostPort: '',
    containerPort: '',
    protocol: 'TCP'
  })
}

function removePortMapping(index: number) {
  ports.value.splice(index, 1)
}

function addVolumeMapping() {
  volumes.value.push({
    hostPath: '',
    containerPath: '',
    mode: 'rw'
  })
}

function removeVolumeMapping(index: number) {
  volumes.value.splice(index, 1)
}

function addEnvironment() {
  environments.value.push({
    name: '',
    value: ''
  })
}

function removeEnvironment(index: number) {
  environments.value.splice(index, 1)
}

async function fetchRegistryImages() {
  loadingImages.value = true
  try {
    const res = await getAdminImageListApi({ page: 1, size: 200 })
    if (res.data?.list) {
      const registryImages = res.data.list.filter(item => item.inRegistry !== false)
      const targetList = registryImages.length > 0 ? registryImages : res.data.list
      imageOptions.value = targetList.map(item => ({
        label: item.fullImageName || `${item.repository}:${item.tag}`,
        value: item.fullImageName || `${item.repository}:${item.tag}`
      }))
    }
  } catch {
  } finally {
    loadingImages.value = false
  }
}

async function fetchCandidateNodes() {
  loadingNodes.value = true
  try {
    const res = await getAdminContainerCandidateNodesApi()
    if (res.data) {
      const options: SelectOption[] = [
        { label: '自动调度 (集群推荐节点)', value: '' }
      ]
      res.data.forEach(node => {
        options.push({
          label: `${node.nodeName} (${node.ipAddress})`,
          value: node.ipAddress
        })
      })
      nodeOptions.value = options
    }
  } catch {
  } finally {
    loadingNodes.value = false
  }
}

function resetForm() {
  form.value = {
    imageName: '',
    containerName: '',
    nodeIp: ''
  }
  cpuLimitStr.value = ''
  memoryLimitStr.value = ''
  durationMinutesStr.value = '60'
  ports.value = []
  volumes.value = []
  environments.value = []
}

async function handleLaunch() {
  if (!form.value.imageName?.trim()) {
    message.warning('请选择镜像')
    return
  }

  const durationMinutes = parseInt(durationMinutesStr.value, 10) || 60
  const cpuLimit = cpuLimitStr.value.trim() ? parseInt(cpuLimitStr.value, 10) : null
  const memoryLimit = memoryLimitStr.value.trim() ? parseInt(memoryLimitStr.value, 10) : null

  const formattedPorts = ports.value
    .filter(p => p.containerPort.trim())
    .map(p => ({
      hostPort: p.hostPort.trim() ? parseInt(p.hostPort, 10) : null,
      containerPort: parseInt(p.containerPort, 10),
      protocol: p.protocol || 'TCP'
    }))

  const formattedVolumes = volumes.value
    .filter(v => v.containerPath.trim())
    .map(v => ({
      hostPath: v.hostPath.trim() || undefined,
      containerPath: v.containerPath.trim(),
      mode: v.mode || 'rw'
    }))

  const formattedEnvironments = environments.value
    .filter(e => e.name.trim())
    .map(e => ({
      name: e.name.trim(),
      value: e.value
    }))

  submitting.value = true
  try {
    await manualStartAdminContainerApi({
      imageName: form.value.imageName.trim(),
      containerName: form.value.containerName.trim() || undefined,
      nodeIp: form.value.nodeIp.trim() || undefined,
      ports: formattedPorts.length > 0 ? formattedPorts : undefined,
      volumes: formattedVolumes.length > 0 ? formattedVolumes : undefined,
      environments: formattedEnvironments.length > 0 ? formattedEnvironments : undefined,
      cpuLimit: cpuLimit || undefined,
      memoryLimit: memoryLimit || undefined,
      durationMinutes
    })
    message.success('容器启动成功')
    emit('update:show', false)
    emit('success')
  } catch {
  } finally {
    submitting.value = false
  }
}

watch(
  () => props.show,
  (val) => {
    if (val) {
      resetForm()
      fetchRegistryImages()
      fetchCandidateNodes()
    }
  }
)
</script>
