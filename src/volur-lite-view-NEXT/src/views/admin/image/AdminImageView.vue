<template>
  <div class="admin-image-view flex flex-col gap-4 sm:gap-6">
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h2 class="text-xl font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
          {{ t('admin.images.title', '镜像管理') }}
        </h2>
        <p class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary mt-1">
          {{ t('admin.images.desc', '管理 Docker 靶机镜像资产、分层分发、节点间同步转移与备份归档') }}
        </p>
      </div>
    </div>

    <div class="grid grid-cols-1 sm:grid-cols-3 gap-3 sm:gap-4">
      <div class="p-3.5 sm:p-5 rounded-xl sm:rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark flex items-center gap-3 sm:gap-4 transition-colors">
        <div class="w-10 h-10 sm:w-12 sm:h-12 rounded-lg sm:rounded-xl flex items-center justify-center bg-blue-50 dark:bg-blue-900/30 text-google-blue dark:text-google-blue-dark shrink-0">
          <n-icon size="20" class="sm:hidden">
            <LayersOutline />
          </n-icon>
          <n-icon size="24" class="hidden sm:inline-flex">
            <LayersOutline />
          </n-icon>
        </div>
        <div class="min-w-0 flex-1">
          <div class="text-[11px] sm:text-xs font-medium text-google-text-muted dark:text-google-text-darkMuted truncate">
            镜像资源总数
          </div>
          <div class="text-xl sm:text-2xl font-bold text-google-text-primary dark:text-google-text-darkPrimary mt-0.5 truncate">
            {{ stats.totalImages }}
          </div>
        </div>
      </div>

      <div class="p-3.5 sm:p-5 rounded-xl sm:rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark flex items-center gap-3 sm:gap-4 transition-colors">
        <div class="w-10 h-10 sm:w-12 sm:h-12 rounded-lg sm:rounded-xl flex items-center justify-center bg-purple-50 dark:bg-purple-900/30 text-purple-600 dark:text-purple-400 shrink-0">
          <n-icon size="20" class="sm:hidden">
            <HardwareChipOutline />
          </n-icon>
          <n-icon size="24" class="hidden sm:inline-flex">
            <HardwareChipOutline />
          </n-icon>
        </div>
        <div class="min-w-0 flex-1">
          <div class="text-[11px] sm:text-xs font-medium text-google-text-muted dark:text-google-text-darkMuted truncate">
            集群存储占用
          </div>
          <div class="text-xl sm:text-2xl font-bold text-google-text-primary dark:text-google-text-darkPrimary mt-0.5 truncate">
            {{ formattedTotalSize }}
          </div>
        </div>
      </div>

      <div class="p-3.5 sm:p-5 rounded-xl sm:rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark flex items-center gap-3 sm:gap-4 transition-colors">
        <div class="w-10 h-10 sm:w-12 sm:h-12 rounded-lg sm:rounded-xl flex items-center justify-center bg-green-50 dark:bg-green-900/30 text-google-green dark:text-google-green-dark shrink-0">
          <n-icon size="20" class="sm:hidden">
            <CheckmarkCircleOutline />
          </n-icon>
          <n-icon size="24" class="hidden sm:inline-flex">
            <CheckmarkCircleOutline />
          </n-icon>
        </div>
        <div class="min-w-0 flex-1">
          <div class="text-[11px] sm:text-xs font-medium text-google-text-muted dark:text-google-text-darkMuted truncate">
            Registry 仓库状态
          </div>
          <div
            class="text-sm sm:text-base font-bold mt-1 truncate"
            :class="stats.registryOnline !== false ? 'text-google-green dark:text-google-green-dark' : 'text-google-red dark:text-google-red-dark'"
          >
            {{ stats.registryOnline !== false ? '服务在线就绪' : '离线' }}
          </div>
        </div>
      </div>
    </div>

    <div class="table-card p-3.5 sm:p-6 rounded-xl sm:rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark transition-colors">
      <div class="flex flex-col sm:flex-row sm:flex-wrap sm:items-center gap-2.5 sm:gap-3 mb-4 sm:mb-6">
        <VSearch
          v-model:value="searchKeyword"
          :placeholder="'搜索镜像 Repository、Tag 或镜像名称...'"
          class="w-full sm:w-auto sm:flex-1 sm:min-w-[140px] sm:max-w-sm"
          @search="handleSearch"
          @clear="handleReset"
          @keydown.enter="handleSearch"
        />

        <div class="flex items-center gap-2 w-full sm:w-auto shrink-0">
          <VBtn
            clz="normal"
            size="medium"
            :left-icon="SearchOutline"
            class="flex-1 sm:flex-none"
            @click="handleSearch"
          >
            {{ t('common.search', '搜索') }}
          </VBtn>

          <VBtn
            clz="normal"
            size="medium"
            :left-icon="RefreshOutline"
            class="flex-1 sm:flex-none"
            @click="handleReset"
          >
            {{ t('common.reset', '重置') }}
          </VBtn>
        </div>

        <div class="w-full sm:w-auto sm:ml-auto flex items-center gap-2 shrink-0 flex-wrap">
          <VBtn
            clz="normal"
            size="medium"
            :left-icon="SyncOutline"
            :loading="scanning"
            class="flex-1 sm:flex-none"
            @click="handleScanImages"
          >
            {{ t('admin.images.scan_sync', '重新扫描镜像') }}
          </VBtn>

          <VBtn
            clz="normal"
            size="medium"
            :left-icon="CloudUploadOutline"
            class="flex-1 sm:flex-none"
            @click="openImportModal"
          >
            {{ t('admin.images.import_image', '导入镜像') }}
          </VBtn>

          <VBtn
            clz="primary"
            size="medium"
            :left-icon="CloudDownloadOutline"
            class="w-full sm:w-auto"
            @click="showPullModal = true"
          >
            {{ t('admin.images.pull_image', '拉取新镜像') }}
          </VBtn>
        </div>
      </div>

      <VTable
        :columns="tableColumns"
        :data="imageList"
        :loading="loading"
        :pagination="false"
        :bordered="false"
        :row-key="(row: AdminClusterImageVO) => String(row.id)"
        :scroll-x="960"
      />

      <div v-if="pagination.itemCount > 0" class="flex flex-col sm:flex-row items-center justify-between gap-3 pt-4 border-t border-google-border-light/70 dark:border-google-border-dark/70">
        <div class="text-xs text-google-text-muted dark:text-google-text-darkMuted text-center sm:text-left">
          显示第 {{ (pagination.page - 1) * pagination.pageSize + 1 }} - {{ Math.min(pagination.page * pagination.pageSize, pagination.itemCount) }} 条，共 {{ pagination.itemCount }} 条
        </div>
        <div class="w-full sm:w-auto overflow-x-auto flex justify-center sm:justify-end">
          <VPagination
            v-model:page="pagination.page"
            v-model:page-size="pagination.pageSize"
            :item-count="pagination.itemCount"
            :page-sizes="pagination.pageSizes"
            show-size-picker
            show-quick-jumper
            size="small"
            @update:page="handlePageChange"
            @update:page-size="handlePageSizeChange"
          />
        </div>
      </div>
    </div>

    <VModal
      v-model:show="showImportModal"
      :title="'导入本地 Docker 镜像归档'"
      :width="520"
    >
      <div class="flex flex-col gap-4 py-2">
        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
            选择镜像归档文件 (.tar / .tar.gz / .tgz) *
          </label>
          <div
            class="border-2 border-dashed border-google-border-light dark:border-google-border-dark rounded-xl p-4 text-center cursor-pointer hover:border-google-blue dark:hover:border-google-blue transition-colors flex flex-col items-center justify-center gap-2 bg-gray-50/50 dark:bg-google-surface-dark/50"
            @click="triggerFileSelect"
          >
            <input
              ref="fileInputRef"
              type="file"
              accept=".tar,.tar.gz,.tgz"
              class="hidden"
              @change="handleFileChange"
            />
            <div class="w-10 h-10 rounded-full bg-blue-50 dark:bg-blue-900/30 text-google-blue dark:text-google-blue-dark flex items-center justify-center">
              <n-icon size="20">
                <CloudUploadOutline />
              </n-icon>
            </div>
            <div v-if="selectedFile" class="text-xs font-semibold text-google-text-primary dark:text-google-text-darkPrimary truncate max-w-full">
              {{ selectedFile.name }} ({{ formatBytes(selectedFile.size) }})
            </div>
            <div v-else class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary">
              点击选择或拖拽本地镜像归档包 (.tar / .tar.gz / .tgz)
            </div>
          </div>
        </div>

        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
            自定义仓库名称 (Repository)
          </label>
          <VInput
            v-model:value="importForm.repository"
            placeholder="'选填，留空将自动从归档中解析 (例如: ctf/web)'"
            clearable
          />
        </div>

        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
            自定义标签 (Tag)
          </label>
          <VInput
            v-model:value="importForm.tag"
            placeholder="'选填，留空默认 latest 或保留原标签'"
            clearable
          />
        </div>

        <div class="text-[11px] text-google-text-muted dark:text-google-text-darkMuted bg-gray-50 dark:bg-google-surface-dark p-2.5 rounded-lg border border-google-border-light/60 dark:border-google-border-dark/60">
          上传后系统将在主控节点通过 Docker 解压并加载镜像，自动打标并推送至私有 Registry 资产中心。
        </div>
      </div>

      <template #footer>
        <div class="flex items-center justify-end gap-3 w-full">
          <VBtn clz="normal" @click="closeImportModal">
            {{ t('common.cancel', '取消') }}
          </VBtn>
          <VBtn clz="primary" :loading="importing" @click="handleConfirmImport">
            {{ t('common.confirm', '确认导入') }}
          </VBtn>
        </div>
      </template>
    </VModal>

    <VModal
      v-model:show="showPullModal"
      :title="'拉取远程 Docker 镜像'"
      :width="480"
    >
      <div class="flex flex-col gap-4 py-2">
        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
            镜像完整地址 (Image:Tag) *
          </label>
          <VInput
            v-model:value="pullImageInput"
            placeholder="'例如: alpine:latest 或 ctf/web:1.0'"
            clearable
          />
        </div>
      </div>

      <template #footer>
        <div class="flex items-center justify-end gap-3 w-full">
          <VBtn clz="normal" @click="showPullModal = false">
            {{ t('common.cancel', '取消') }}
          </VBtn>
          <VBtn clz="primary" :loading="pulling" @click="handleConfirmPull">
            {{ t('common.confirm', '开始拉取') }}
          </VBtn>
        </div>
      </template>
    </VModal>

    <VModal
      v-model:show="showTransferModal"
      :title="'节点镜像转移分发'"
      :width="480"
    >
      <div class="flex flex-col gap-4 py-2">
        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
            源节点 IP 地址 *
          </label>
          <VInput
            v-model:value="transferForm.sourceNodeIp"
            placeholder="'例如: 192.168.1.101'"
            clearable
          />
        </div>

        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
            目标节点 IP 地址 *
          </label>
          <VInput
            v-model:value="transferForm.targetNodeIp"
            placeholder="'例如: 192.168.1.102'"
            clearable
          />
        </div>
      </div>

      <template #footer>
        <div class="flex items-center justify-end gap-3 w-full">
          <VBtn clz="normal" @click="showTransferModal = false">
            {{ t('common.cancel', '取消') }}
          </VBtn>
          <VBtn clz="primary" :loading="transferring" @click="handleConfirmTransfer">
            {{ t('common.confirm', '确认转移') }}
          </VBtn>
        </div>
      </template>
    </VModal>

    <VDrawer
      :show="showDetailDrawer"
      :title="detailImage ? `${detailImage.repository}:${detailImage.tag || 'latest'}` : '镜像分层详情 (Layers)'"
      :width="680"
      @update:show="(val: boolean) => showDetailDrawer = val"
      @close="showDetailDrawer = false"
    >
      <div v-if="loadingDetail" class="flex flex-col items-center justify-center py-24 gap-3">
        <n-spin size="medium" />
        <span class="text-xs text-google-text-muted dark:text-google-text-darkMuted">正在解析镜像分层元数据...</span>
      </div>

      <div v-else-if="!detailImage" class="flex flex-col items-center justify-center py-24 gap-2 text-google-text-muted dark:text-google-text-darkMuted">
        <n-icon size="36">
          <LayersOutline />
        </n-icon>
        <span class="text-xs">暂无镜像分层数据</span>
      </div>

      <div v-else class="flex flex-col gap-5">
        <div class="p-4 rounded-xl bg-gray-50/80 dark:bg-google-surface-dark border border-google-border-light dark:border-google-border-dark flex flex-col gap-3">
          <div class="flex items-center justify-between gap-3">
            <div class="min-w-0 flex-1">
              <div class="text-xs text-google-text-muted dark:text-google-text-darkMuted">镜像全名</div>
              <div class="text-sm font-semibold text-google-text-primary dark:text-google-text-darkPrimary truncate mt-0.5">
                {{ detailImage.fullImageName || `${detailImage.repository}:${detailImage.tag || 'latest'}` }}
              </div>
            </div>
            <div class="shrink-0 text-right">
              <div class="text-xs text-google-text-muted dark:text-google-text-darkMuted">分层总大小</div>
              <div class="text-sm font-semibold text-google-blue dark:text-google-blue-dark mt-0.5">
                {{ formatBytes(detailImage.sizeBytes) }}
              </div>
            </div>
          </div>

          <div class="grid grid-cols-2 sm:grid-cols-4 gap-2 pt-2 border-t border-google-border-light/60 dark:border-google-border-dark/60 text-xs">
            <div>
              <span class="text-google-text-muted dark:text-google-text-darkMuted block">分层总数</span>
              <span class="font-medium text-google-text-primary dark:text-google-text-darkPrimary">
                {{ detailImage.layers ? detailImage.layers.length : 0 }} 层
              </span>
            </div>
            <div>
              <span class="text-google-text-muted dark:text-google-text-darkMuted block">系统架构</span>
              <span class="font-medium text-google-text-primary dark:text-google-text-darkPrimary">
                {{ detailImage.architecture ? `${detailImage.os || 'linux'}/${detailImage.architecture}` : 'linux/amd64' }}
              </span>
            </div>
            <div class="col-span-2 sm:col-span-2">
              <span class="text-google-text-muted dark:text-google-text-darkMuted block">镜像 ID</span>
              <div class="flex items-center gap-1.5 mt-0.5">
                <span class="font-medium text-google-text-secondary dark:text-google-text-darkSecondary truncate">
                  {{ detailImage.imageId ? (detailImage.imageId.startsWith('sha256:') ? detailImage.imageId.substring(7, 19) : detailImage.imageId) : '-' }}
                </span>
                <button
                  v-if="detailImage.imageId"
                  type="button"
                  class="text-google-text-muted hover:text-google-blue dark:hover:text-google-blue-dark p-0.5 transition-colors cursor-pointer"
                  @click="copyToClipboard(detailImage.imageId)"
                >
                  <n-icon size="14">
                    <CopyOutline />
                  </n-icon>
                </button>
              </div>
            </div>
          </div>
        </div>

        <div class="flex flex-col gap-3">
          <div class="flex items-center justify-between">
            <h3 class="text-sm font-semibold text-google-text-primary dark:text-google-text-darkPrimary flex items-center gap-1.5">
              <n-icon size="16" class="text-google-blue dark:text-google-blue-dark">
                <LayersOutline />
              </n-icon>
              镜像构建分层 (Layers)
            </h3>
            <span class="text-xs text-google-text-muted dark:text-google-text-darkMuted">
              共 {{ detailImage.layers ? detailImage.layers.length : 0 }} 个构建步骤
            </span>
          </div>

          <div v-if="!detailImage.layers || detailImage.layers.length === 0" class="p-8 text-center text-xs text-google-text-muted dark:text-google-text-darkMuted border border-dashed border-google-border-light dark:border-google-border-dark rounded-xl">
            该镜像未提供分层元数据记录
          </div>

          <div v-else class="flex flex-col gap-2.5">
            <div
              v-for="(layer, idx) in detailImage.layers"
              :key="layer.layerId || idx"
              class="p-3 sm:p-3.5 rounded-xl border border-google-border-light dark:border-google-border-dark bg-white dark:bg-google-surface-darkCard hover:border-google-blue/40 dark:hover:border-google-blue/40 transition-colors flex flex-col gap-2"
            >
              <div class="flex items-center justify-between gap-2">
                <div class="flex items-center gap-2 min-w-0">
                  <span class="px-1.5 py-0.5 text-[11px] font-semibold rounded bg-gray-100 dark:bg-google-surface-dark text-google-text-secondary dark:text-google-text-darkSecondary shrink-0">
                    #{{ layer.layerIndex !== undefined ? layer.layerIndex : idx + 1 }}
                  </span>
                  <span
                    class="px-2 py-0.5 text-[11px] font-semibold rounded shrink-0 uppercase"
                    :class="getInstructionBadgeClass(layer.instruction, layer.createdBy)"
                  >
                    {{ getInstructionKeyword(layer.instruction, layer.createdBy) }}
                  </span>
                  <span v-if="layer.layerId" class="text-[11px] text-google-text-muted dark:text-google-text-darkMuted truncate max-w-[120px] sm:max-w-[180px]">
                    {{ layer.layerId.startsWith('sha256:') ? layer.layerId.substring(7, 19) : layer.layerId }}
                  </span>
                </div>

                <div class="flex items-center gap-2 shrink-0">
                  <span class="text-xs font-medium" :class="layer.sizeBytes > 0 ? 'text-google-text-primary dark:text-google-text-darkPrimary' : 'text-google-text-muted dark:text-google-text-darkMuted'">
                    {{ formatBytes(layer.sizeBytes) }}
                  </span>
                  <button
                    type="button"
                    class="p-1 rounded hover:bg-gray-100 dark:hover:bg-google-surface-dark text-google-text-muted hover:text-google-blue dark:hover:text-google-blue-dark transition-colors cursor-pointer"
                    title="复制指令"
                    @click="copyToClipboard(cleanInstructionText(layer.instruction, layer.createdBy))"
                  >
                    <n-icon size="14">
                      <CopyOutline />
                    </n-icon>
                  </button>
                </div>
              </div>

              <div class="p-2 rounded-lg bg-gray-50 dark:bg-google-surface-dark border border-google-border-light/60 dark:border-google-border-dark/60 text-xs text-google-text-primary dark:text-google-text-darkPrimary break-all select-text">
                {{ cleanInstructionText(layer.instruction, layer.createdBy) }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </VDrawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, h } from 'vue'
import { useI18n } from 'vue-i18n'
import { NIcon, NSpin, useMessage, useDialog, type DataTableColumns } from 'naive-ui'
import {
  LayersOutline,
  HardwareChipOutline,
  CheckmarkCircleOutline,
  SearchOutline,
  RefreshOutline,
  SyncOutline,
  CloudDownloadOutline,
  CloudUploadOutline,
  CopyOutline
} from '@vicons/ionicons5'
import VBtn from '@/components/common/VBtn.vue'
import VTable from '@/components/common/VTable.vue'
import VSearch from '@/components/common/VSearch.vue'
import VPagination from '@/components/common/VPagination.vue'
import VModal from '@/components/common/VModal.vue'
import VInput from '@/components/common/VInput.vue'
import VDrawer from '@/components/sidebar/VDrawer.vue'
import type {
  AdminClusterImageVO,
  AdminClusterImageDetailVO,
  AdminClusterImageStatisticsVO
} from '@/types/image'
import {
  getAdminImageStatisticsApi,
  getAdminImageListApi,
  getAdminImageDetailApi,
  scanAndSyncAdminImagesApi,
  pullAdminImageApi,
  importAdminImageApi,
  transferAdminImageApi,
  exportAdminImageApi,
  deleteAdminImageApi
} from '@/api/image'

defineOptions({
  name: 'AdminImageView'
})

const { t } = useI18n()
const message = useMessage()
const dialog = useDialog()

const loading = ref(false)
const imageList = ref<AdminClusterImageVO[]>([])

const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

const searchKeyword = ref('')

const scanning = ref(false)
const pulling = ref(false)
const importing = ref(false)
const transferring = ref(false)

const stats = ref<AdminClusterImageStatisticsVO>({
  totalImages: 0,
  totalSizeBytes: 0,
  registryOnline: true,
  registryImagesCount: 0
})

const showImportModal = ref(false)
const fileInputRef = ref<HTMLInputElement | null>(null)
const selectedFile = ref<File | null>(null)
const importForm = ref({
  repository: '',
  tag: ''
})

const showPullModal = ref(false)
const pullImageInput = ref('')

const showTransferModal = ref(false)
const transferImageId = ref<string | number | null>(null)
const transferForm = ref({
  sourceNodeIp: '',
  targetNodeIp: ''
})

const showDetailDrawer = ref(false)
const loadingDetail = ref(false)
const detailImage = ref<AdminClusterImageDetailVO | null>(null)

const formattedTotalSize = computed(() => {
  const bytes = stats.value.totalSizeBytes || 0
  if (bytes > 1024 * 1024 * 1024) {
    return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
})

function formatBytes(bytes?: number) {
  if (bytes === 0) return '0 B'
  if (!bytes) return '-'
  if (bytes > 1024 * 1024 * 1024) {
    return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`
  }
  if (bytes > 1024 * 1024) {
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
  }
  if (bytes > 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`
  }
  return `${bytes} B`
}

async function handleOpenImageLayers(row: AdminClusterImageVO) {
  showDetailDrawer.value = true
  loadingDetail.value = true
  detailImage.value = null
  try {
    const res = await getAdminImageDetailApi(row.id)
    if (res.data) {
      detailImage.value = res.data
    }
  } catch (err: any) {
    message.error(err.message || '获取镜像分层信息失败')
  } finally {
    loadingDetail.value = false
  }
}

async function copyToClipboard(text: string) {
  if (!text) return
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text)
    } else {
      const textarea = document.createElement('textarea')
      textarea.value = text
      textarea.style.position = 'fixed'
      textarea.style.left = '-9999px'
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
    }
    message.success('已复制到剪贴板')
  } catch {
    message.error('复制失败')
  }
}

function getInstructionKeyword(instruction?: string, createdBy?: string): string {
  const text = (instruction || createdBy || '').trim()
  const upper = text.toUpperCase()
  if (upper.startsWith('RUN ') || upper.includes('/BIN/SH -C')) return 'RUN'
  if (upper.startsWith('COPY ') || upper.includes('COPY ')) return 'COPY'
  if (upper.startsWith('ADD ') || upper.includes('ADD ')) return 'ADD'
  if (upper.startsWith('ENV ') || upper.includes('ENV ')) return 'ENV'
  if (upper.startsWith('ARG ') || upper.includes('ARG ')) return 'ARG'
  if (upper.startsWith('CMD ') || upper.includes('CMD ')) return 'CMD'
  if (upper.startsWith('ENTRYPOINT ') || upper.includes('ENTRYPOINT ')) return 'ENTRYPOINT'
  if (upper.startsWith('WORKDIR ') || upper.includes('WORKDIR ')) return 'WORKDIR'
  if (upper.startsWith('EXPOSE ') || upper.includes('EXPOSE ')) return 'EXPOSE'
  if (upper.startsWith('USER ') || upper.includes('USER ')) return 'USER'
  if (upper.startsWith('LABEL ') || upper.includes('LABEL ')) return 'LABEL'
  if (upper.startsWith('VOLUME ') || upper.includes('VOLUME ')) return 'VOLUME'
  return 'LAYER'
}

function getInstructionBadgeClass(instruction?: string, createdBy?: string): string {
  const kw = getInstructionKeyword(instruction, createdBy)
  switch (kw) {
    case 'RUN':
      return 'bg-blue-50 dark:bg-blue-900/30 text-google-blue dark:text-google-blue-dark'
    case 'COPY':
    case 'ADD':
      return 'bg-green-50 dark:bg-green-900/30 text-google-green dark:text-google-green-dark'
    case 'ENV':
    case 'ARG':
      return 'bg-yellow-50 dark:bg-yellow-900/30 text-google-yellow dark:text-google-yellow-dark'
    case 'CMD':
    case 'ENTRYPOINT':
      return 'bg-purple-50 dark:bg-purple-900/30 text-purple-600 dark:text-purple-400'
    case 'EXPOSE':
      return 'bg-cyan-50 dark:bg-cyan-900/30 text-google-cyan dark:text-google-cyan-dark'
    default:
      return 'bg-gray-100 dark:bg-google-surface-dark text-google-text-secondary dark:text-google-text-darkSecondary'
  }
}

function cleanInstructionText(instruction?: string, createdBy?: string): string {
  let text = instruction || createdBy || ''
  if (!text) return '-'
  text = text.replace(/^\/bin\/sh -c #\(nop\)\s+/, '')
  text = text.replace(/^\/bin\/sh -c\s+/, 'RUN ')
  return text.trim()
}

const tableColumns = computed<DataTableColumns<AdminClusterImageVO>>(() => [
  {
    title: '镜像名称',
    key: 'repository',
    minWidth: 180,
    render(row) {
      return h(
        'span',
        {
          class: 'redirectable',
          onClick: () => handleOpenImageLayers(row)
        },
        row.repository
      )
    }
  },
  {
    title: '镜像标签',
    key: 'tag',
    width: 120,
    render(row) {
      return h('span', { class: 'text-xs' }, row.tag || 'latest')
    }
  },
  {
    title: '占用体积',
    key: 'sizeBytes',
    width: 110,
    render(row) {
      return h('span', { class: 'text-xs text-google-text-secondary dark:text-google-text-darkSecondary' }, formatBytes(row.sizeBytes))
    }
  },
  {
    title: '创建时间',
    key: 'createdAt',
    width: 160,
    render(row) {
      return h('span', { class: 'text-xs text-google-text-secondary dark:text-google-text-darkSecondary' }, row.createdAt ? new Date(row.createdAt).toLocaleString() : '-')
    }
  },
  {
    title: t('common.actions', '操作'),
    key: 'actions',
    width: 200,
    align: 'center',
    render(row) {
      return h('div', { class: 'flex items-center justify-center gap-1.5' }, [
        h(
          VBtn,
          {
            clz: 'normal',
            size: 'small',
            onClick: () => openTransfer(row.id)
          },
          { default: () => '分发' }
        ),
        h(
          VBtn,
          {
            clz: 'normal',
            size: 'small',
            onClick: () => handleExportImage(row.id, row.repository, row.tag)
          },
          { default: () => '导出' }
        ),
        h(
          VBtn,
          {
            clz: 'danger',
            size: 'small',
            onClick: () => handleDeleteImage(row.id)
          },
          { default: () => '删除' }
        )
      ])
    }
  }
])

async function fetchStats() {
  try {
    const res = await getAdminImageStatisticsApi()
    if (res.data) {
      stats.value = res.data
    }
  } catch {}
}

async function fetchImages() {
  loading.value = true
  try {
    const res = await getAdminImageListApi({
      page: pagination.page,
      size: pagination.pageSize,
      keyword: searchKeyword.value.trim() || undefined
    })
    if (res.data) {
      imageList.value = res.data.list ?? []
      pagination.itemCount = Number(res.data.total) || 0
    }
  } catch (err: any) {
    message.error(err.message || '获取镜像列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchImages()
}

function handleReset() {
  searchKeyword.value = ''
  pagination.page = 1
  fetchImages()
}

function handlePageChange(page: number) {
  pagination.page = page
  fetchImages()
}

function handlePageSizeChange(size: number) {
  pagination.pageSize = size
  pagination.page = 1
  fetchImages()
}

async function handleScanImages() {
  scanning.value = true
  try {
    await scanAndSyncAdminImagesApi()
    message.success('已触发重新扫描镜像并同步')
    await Promise.all([fetchStats(), fetchImages()])
  } catch (err: any) {
    message.error(err.message || '重新扫描失败')
  } finally {
    scanning.value = false
  }
}

async function handleConfirmPull() {
  if (!pullImageInput.value.trim()) {
    message.warning('请填写镜像地址')
    return
  }
  pulling.value = true
  try {
    await pullAdminImageApi({ image: pullImageInput.value.trim() })
    message.success('拉取任务已提交至异步队列')
    showPullModal.value = false
    pullImageInput.value = ''
    await Promise.all([fetchStats(), fetchImages()])
  } catch (err: any) {
    message.error(err.message || '拉取任务提交失败')
  } finally {
    pulling.value = false
  }
}

function openImportModal() {
  selectedFile.value = null
  importForm.value = { repository: '', tag: '' }
  showImportModal.value = true
}

function closeImportModal() {
  showImportModal.value = false
  selectedFile.value = null
  importForm.value = { repository: '', tag: '' }
}

function triggerFileSelect() {
  fileInputRef.value?.click()
}

function handleFileChange(e: Event) {
  const target = e.target as HTMLInputElement
  if (target.files && target.files.length > 0) {
    selectedFile.value = target.files[0]
  }
}

async function handleConfirmImport() {
  if (!selectedFile.value) {
    message.warning('请选择需要导入的 Docker 镜像归档文件')
    return
  }
  importing.value = true
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    if (importForm.value.repository.trim()) {
      formData.append('repository', importForm.value.repository.trim())
    }
    if (importForm.value.tag.trim()) {
      formData.append('tag', importForm.value.tag.trim())
    }
    await importAdminImageApi(formData)
    message.success('镜像导入与中心入库任务已提交后台处理')
    closeImportModal()
    await Promise.all([fetchStats(), fetchImages()])
  } catch (err: any) {
    message.error(err.message || '镜像导入失败')
  } finally {
    importing.value = false
  }
}

function openTransfer(id: string | number) {
  transferImageId.value = id
  transferForm.value = { sourceNodeIp: '', targetNodeIp: '' }
  showTransferModal.value = true
}

async function handleConfirmTransfer() {
  if (!transferImageId.value || !transferForm.value.sourceNodeIp.trim() || !transferForm.value.targetNodeIp.trim()) {
    message.warning('请完整填写源节点与目标节点 IP')
    return
  }
  transferring.value = true
  try {
    await transferAdminImageApi({
      imageId: transferImageId.value,
      sourceNodeIp: transferForm.value.sourceNodeIp.trim(),
      targetNodeIp: transferForm.value.targetNodeIp.trim()
    })
    message.success('镜像节点转移分发任务已提交')
    showTransferModal.value = false
  } catch (err: any) {
    message.error(err.message || '提交转移任务失败')
  } finally {
    transferring.value = false
  }
}

async function handleExportImage(id: string | number, repo: string, tag: string) {
  try {
    const blob = await exportAdminImageApi(id)
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${repo.replace('/', '_')}_${tag || 'latest'}.tar`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出开始下载')
  } catch (err: any) {
    message.error(err.message || '导出镜像失败')
  }
}

function handleDeleteImage(id: string | number) {
  dialog.warning({
    title: '确认删除镜像',
    content: '删除后所有工作节点上的该镜像副本将被清空，是否继续？',
    positiveText: '确认删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteAdminImageApi(id)
        message.success('镜像已删除')
        await Promise.all([fetchStats(), fetchImages()])
      } catch (err: any) {
        message.error(err.message || '删除镜像失败')
      }
    }
  })
}

onMounted(() => {
  fetchStats()
  fetchImages()
})
</script>
