<template>
  <div class="admin-image-view">
    <!-- Header Section -->
    <div class="page-header">
      <h2 class="page-title">{{ t('admin.images.title') }}</h2>
      <p class="page-desc">{{ t('admin.images.desc') }}</p>
    </div>

    <!-- Statistics Ribbon -->
    <div class="stat-ribbon">
      <div class="stat-mini-card">
        <div class="stat-icon-wrapper" :class="statistics.registryOnline ? 'success' : 'error'">
          <n-icon size="18">
            <CloudDoneOutline v-if="statistics.registryOnline" />
            <CloudOfflineOutline v-else />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value" :style="{ color: statistics.registryOnline ? '#16a34a' : '#dc2626', fontSize: '15px' }">
            {{ statistics.registryOnline ? '在线 (200 OK)' : '离线脱机' }}
          </div>
          <div class="stat-label">
            {{ t('admin.images.stats.registryStatus') }}
          </div>
        </div>
      </div>

      <div class="stat-mini-card">
        <div class="stat-icon-wrapper primary">
          <n-icon size="18">
            <LayersOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.totalImages }}</div>
          <div class="stat-label">{{ t('admin.images.stats.total') }}</div>
        </div>
      </div>

      <div class="stat-mini-card">
        <div class="stat-icon-wrapper success">
          <n-icon size="18">
            <ArchiveOutline />
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ formattedTotalSize }}</div>
          <div class="stat-label">{{ t('admin.images.stats.storage') }}</div>
        </div>
      </div>
    </div>

    <!-- Main Table Card -->
    <VolurTableCard
      :bordered="true"
      :table-bordered="true"
      :scroll-x="850"
      :columns="imageColumns"
      :data="imageList"
      :loading="loadingTable"
      table-class="images-table"
      standalone-pagination
      :page="currentPage"
      :page-size="pageSize"
      :item-count="totalCount"
      @update:page="handlePageChange"
      @update:page-size="handlePageSizeChange"
    >
      <template #filter-left>
        <div class="image-filter-left">
          <n-input
            v-model:value="searchKeyword"
            :placeholder="t('admin.images.search.placeholder')"
            clearable
            class="filter-input-search"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <n-icon :component="SearchOutline" />
            </template>
          </n-input>

          <n-button type="primary" class="theme-primary-btn search-btn" @click="handleSearch">
            <template #icon>
              <n-icon :component="SearchOutline" />
            </template>
            {{ t('admin.images.search.refresh') }}
          </n-button>
        </div>
      </template>

      <template #filter-right>
        <div class="image-filter-right">
          <!-- Scan & Sync Docker Images Button -->
          <n-popconfirm
            positive-text="确认同步"
            negative-text="取消"
            @positive-click="handleScanAndSync"
          >
            <template #trigger>
              <n-button secondary class="action-sync-btn" :loading="scanningImages">
                <template #icon>
                  <n-icon :component="SyncOutline" />
                </template>
                {{ t('admin.images.search.scanBtn') }}
              </n-button>
            </template>
            {{ t('admin.images.search.scanConfirm') }}
          </n-popconfirm>

          <n-button type="primary" class="theme-primary-btn action-pull-btn" @click="openPullModal">
            <template #icon>
              <n-icon :component="DownloadOutline" />
            </template>
            {{ t('admin.images.search.pullBtn') }}
          </n-button>
        </div>
      </template>
    </VolurTableCard>

    <!-- Pull Image Modal -->
    <n-modal
      v-model:show="showPullModal"
      preset="card"
      :title="t('admin.images.pullModal.title')"
      style="max-width: 580px; border-radius: 12px;"
      :bordered="false"
    >
      <p class="modal-subtext">{{ t('admin.images.pullModal.subtitle') }}</p>

      <n-alert type="info" style="margin-top: 14px; font-size: 12.5px;" :bordered="false">
        {{ t('admin.images.pullModal.registryNotice') }}
      </n-alert>

      <n-form
        ref="pullFormRef"
        :model="pullForm"
        :rules="pullRules"
        label-placement="top"
        style="margin-top: 16px;"
      >
        <n-form-item :label="t('admin.images.pullModal.imageName')" path="image">
          <n-input
            v-model:value="pullForm.image"
            :placeholder="t('admin.images.pullModal.imageNamePlaceholder')"
          />
        </n-form-item>
      </n-form>

      <template #footer>
        <div class="modal-footer">
          <n-button quaternary @click="showPullModal = false">
            {{ t('admin.images.pullModal.cancel') }}
          </n-button>
          <n-button
            type="primary"
            class="theme-primary-btn"
            :loading="submittingPull"
            @click="handleSubmitPull"
          >
            {{ t('admin.images.pullModal.submit') }}
          </n-button>
        </div>
      </template>
    </n-modal>

    <!-- Docker Hub Style Image Details & Layers Drawer -->
    <n-drawer
      v-model:show="showDetailDrawer"
      :width="740"
      placement="right"
      style="max-width: 92vw;"
    >
      <n-drawer-content :title="detailImage?.fullImageName || 'Docker 镜像详情'" closable>
        <div v-if="loadingDetail" class="detail-loading-wrapper">
          <n-spin size="large" />
          <p class="loading-subtext">正在从底层 Docker 引擎提取分层 Layer 与 Inspect 元数据...</p>
        </div>

        <div v-else-if="detailImage" class="image-detail-content">
          <!-- Overview Header Banner -->
          <div class="detail-header-card">
            <div class="header-top">
              <div class="image-full-title">
                {{ detailImage.fullImageName }}
              </div>
            </div>
            <div class="header-meta-row">
              <div class="meta-item">
                <span class="meta-label">摘要 ID:</span>
                <span class="mono-text truncate" style="max-width: 380px;">{{ detailImage.imageId || '-' }}</span>
              </div>
              <div class="meta-item">
                <span class="meta-label">总大小:</span>
                <span class="mono-text font-bold">{{ formatBytes(detailImage.sizeBytes) }}</span>
              </div>
            </div>
            <div class="header-meta-row">
              <div class="meta-item">
                <span class="meta-label">系统架构:</span>
                <n-tag size="tiny" :bordered="false">{{ detailImage.os }}/{{ detailImage.architecture }}</n-tag>
              </div>
              <div class="meta-item">
                <span class="meta-label">引擎版本:</span>
                <span class="mono-text">{{ detailImage.dockerVersion || 'Docker Engine' }}</span>
              </div>
              <div class="meta-item">
                <span class="meta-label">分层数:</span>
                <span class="mono-text">{{ detailImage.layers ? detailImage.layers.length : 0 }} Layers</span>
              </div>
            </div>
          </div>

          <!-- Tabs for Layers, Inspect, and Overview -->
          <n-tabs type="line" animated style="margin-top: 14px;">
            <!-- Tab 1: Docker Hub Style Layers Breakdown -->
            <n-tab-pane name="layers" :tab="t('admin.images.detailDrawer.layersTab')">
              <div class="layers-container">
                <div class="layers-summary-bar">
                  <span>{{ t('admin.images.detailDrawer.layersCount', { count: detailImage.layers ? detailImage.layers.length : 0 }) }}</span>
                  <span class="total-size-badge">{{ t('admin.images.detailDrawer.totalSize') }}: {{ formatBytes(detailImage.sizeBytes) }}</span>
                </div>

                <div v-if="!detailImage.layers || detailImage.layers.length === 0" class="empty-layers">
                  <n-empty :description="t('admin.images.detailDrawer.noLayers')" />
                </div>

                <div v-else class="layers-timeline">
                  <div
                    v-for="(layer, idx) in detailImage.layers"
                    :key="idx"
                    class="layer-item-card"
                  >
                    <div class="layer-header">
                      <div class="layer-left">
                        <span class="layer-badge">#{{ layer.layerIndex }}</span>
                        <n-tag
                          size="tiny"
                          :type="getInstructionTagType(layer.instruction)"
                          class="instruction-tag"
                        >
                          {{ getInstructionKeyword(layer.instruction) }}
                        </n-tag>
                        <span v-if="layer.layerId" class="layer-id-text truncate">
                          {{ layer.layerId.startsWith('sha256:') ? layer.layerId.substring(7, 19) : layer.layerId }}
                        </span>
                      </div>
                      <div class="layer-right">
                        <span class="layer-size mono-text">{{ formatBytes(layer.sizeBytes) }}</span>
                        <span v-if="layer.createdAt" class="layer-time">{{ layer.createdAt }}</span>
                      </div>
                    </div>

                    <div class="layer-command-box">
                      <code class="command-text">{{ layer.instruction || layer.createdBy }}</code>
                      <n-button
                        size="tiny"
                        quaternary
                        circle
                        class="copy-btn"
                        @click="copyToClipboard(layer.instruction || layer.createdBy)"
                      >
                        <template #icon>
                          <n-icon :size="12" :component="CopyOutline" />
                        </template>
                      </n-button>
                    </div>
                  </div>
                </div>
              </div>
            </n-tab-pane>

            <!-- Tab 2: Runtime Environment / Inspect -->
            <n-tab-pane name="inspect" :tab="t('admin.images.detailDrawer.inspectTab')">
              <div class="inspect-container">
                <div class="inspect-section">
                  <div class="section-title">{{ t('admin.images.detailDrawer.entrypoint') }}</div>
                  <div class="code-box">
                    <span v-if="detailImage.entrypoint && detailImage.entrypoint.length > 0">
                      {{ detailImage.entrypoint.join(' ') }}
                    </span>
                    <span v-else class="text-muted">（未指定 Entrypoint）</span>
                  </div>
                </div>

                <div class="inspect-section">
                  <div class="section-title">{{ t('admin.images.detailDrawer.cmd') }}</div>
                  <div class="code-box">
                    <span v-if="detailImage.cmd && detailImage.cmd.length > 0">
                      {{ detailImage.cmd.join(' ') }}
                    </span>
                    <span v-else class="text-muted">（未指定 Cmd）</span>
                  </div>
                </div>

                <div class="inspect-section">
                  <div class="section-title">{{ t('admin.images.detailDrawer.workingDir') }}</div>
                  <div class="code-box">
                    <span>{{ detailImage.workingDir || '/' }}</span>
                  </div>
                </div>

                <div class="inspect-section">
                  <div class="section-title">{{ t('admin.images.detailDrawer.user') }}</div>
                  <div class="code-box">
                    <span>{{ detailImage.user || 'root' }}</span>
                  </div>
                </div>

                <div v-if="detailImage.exposedPorts && detailImage.exposedPorts.length > 0" class="inspect-section">
                  <div class="section-title">{{ t('admin.images.detailDrawer.exposedPorts') }}</div>
                  <div class="tag-row">
                    <n-tag
                      v-for="port in detailImage.exposedPorts"
                      :key="port"
                      size="small"
                      type="info"
                    >
                      {{ port }}
                    </n-tag>
                  </div>
                </div>

                <div v-if="detailImage.env && detailImage.env.length > 0" class="inspect-section">
                  <div class="section-title">{{ t('admin.images.detailDrawer.envVars') }}</div>
                  <div class="env-list">
                    <div v-for="(envItem, eIdx) in detailImage.env" :key="eIdx" class="env-row">
                      <span class="mono-text">{{ envItem }}</span>
                    </div>
                  </div>
                </div>

                <div v-if="detailImage.labels && Object.keys(detailImage.labels).length > 0" class="inspect-section">
                  <div class="section-title">{{ t('admin.images.detailDrawer.labels') }}</div>
                  <div class="labels-list">
                    <div v-for="(val, k) in detailImage.labels" :key="k" class="label-row">
                      <span class="label-key">{{ k }}:</span>
                      <span class="label-val">{{ val }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </n-tab-pane>

            <!-- Tab 3: Basic Metadata -->
            <n-tab-pane name="basic" :tab="t('admin.images.detailDrawer.basicTab')">
              <div class="basic-meta-container">
                <div class="meta-table">
                  <div class="meta-row">
                    <span class="row-label">仓库名称 (Repository):</span>
                    <span class="row-value mono-text">{{ detailImage.repository }}</span>
                  </div>
                  <div class="meta-row">
                    <span class="row-label">标签版本 (Tag):</span>
                    <span class="row-value mono-text">{{ detailImage.tag }}</span>
                  </div>
                  <div class="meta-row">
                    <span class="row-label">完整镜像全名:</span>
                    <span class="row-value mono-text font-bold">{{ detailImage.fullImageName }}</span>
                  </div>
                  <div class="meta-row">
                    <span class="row-label">镜像摘要 ID:</span>
                    <span class="row-value mono-text">{{ detailImage.imageId }}</span>
                  </div>
                  <div class="meta-row">
                    <span class="row-label">物理存储体积:</span>
                    <span class="row-value mono-text">{{ formatBytes(detailImage.sizeBytes) }} ({{ detailImage.sizeBytes }} 字节)</span>
                  </div>
                  <div class="meta-row">
                    <span class="row-label">托管仓库位置:</span>
                    <span class="row-value">
                      <n-tag size="small" type="success">
                        中心私有 Registry
                      </n-tag>
                    </span>
                  </div>
                  <div class="meta-row">
                    <span class="row-label">入库创建时间:</span>
                    <span class="row-value">{{ detailImage.createdAt }}</span>
                  </div>
                  <div v-if="detailImage.updatedAt" class="meta-row">
                    <span class="row-label">最近更新时间:</span>
                    <span class="row-value">{{ detailImage.updatedAt }}</span>
                  </div>
                </div>
              </div>
            </n-tab-pane>
          </n-tabs>
        </div>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, h } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  NForm,
  NFormItem,
  NInput,
  NButton,
  NIcon,
  NTag,
  NModal,
  NAlert,
  NPopconfirm,
  NDrawer,
  NDrawerContent,
  NTabs,
  NTabPane,
  NSpin,
  NEmpty,
  useMessage,
  type DataTableColumns,
  type FormInst,
  type FormRules
} from 'naive-ui'
import { VolurTableCard } from '@/public/volurEx'
import {
  LayersOutline,
  SearchOutline,
  DownloadOutline,
  CopyOutline,
  TrashOutline,
  SyncOutline,
  EyeOutline,
  ArchiveOutline,
  CloudDoneOutline,
  CloudOfflineOutline
} from '@vicons/ionicons5'
import {
  getAdminImageStatisticsApi,
  getAdminImageListApi,
  getAdminImageDetailApi,
  scanAndSyncAdminImagesApi,
  pullAdminImageApi,
  exportAdminImageApi,
  deleteAdminImageApi
} from '@/api/image'
import type {
  AdminClusterImageVO,
  AdminClusterImageDetailVO,
  AdminClusterImageStatisticsVO
} from '@/types/image'
import { formatBytes } from '@/utils/parser'

const { t } = useI18n()
const message = useMessage()

// Statistics State
const statistics = reactive<AdminClusterImageStatisticsVO>({
  totalImages: 0,
  totalSizeBytes: 0,
  registryOnline: false,
  registryImagesCount: 0
})

const formattedTotalSize = computed(() => formatBytes(statistics.totalSizeBytes))

// Search & Filter State
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const totalCount = ref(0)
const loadingTable = ref(false)
const imageList = ref<AdminClusterImageVO[]>([])
const scanningImages = ref(false)

// Action State
const exportingImageId = ref<string | number | null>(null)

// Detail Drawer State
const showDetailDrawer = ref(false)
const loadingDetail = ref(false)
const detailImage = ref<AdminClusterImageDetailVO | null>(null)

// Pull Modal State
const showPullModal = ref(false)
const submittingPull = ref(false)
const pullFormRef = ref<FormInst | null>(null)
const pullForm = reactive({
  image: ''
})

const pullRules: FormRules = {
  image: [
    { required: true, message: () => t('admin.images.pullModal.imageNameRequired'), trigger: 'blur' },
    { min: 3, max: 255, message: '镜像名称长度必须在3到255个字符之间', trigger: 'blur' }
  ]
}

// Table Columns (Registry-centric, no node distribution or status columns)
const imageColumns = computed<DataTableColumns<AdminClusterImageVO>>(() => [
  {
    title: t('admin.images.table.image'),
    key: 'fullImageName',
    width: 320,
    render(row) {
      return h('div', { class: 'image-id-cell' }, [
        h('span', { class: 'mono-text truncate', title: row.fullImageName }, row.fullImageName),
        h(
          NTag,
          {
            size: 'tiny',
            round: true,
            type: 'info',
            bordered: false,
            style: { marginLeft: '6px', fontSize: '10px' }
          },
          { default: () => 'Registry' }
        ),
        h(
          NButton,
          {
            size: 'tiny',
            quaternary: true,
            circle: true,
            onClick: () => copyToClipboard(row.fullImageName)
          },
          { default: () => h(NIcon, { size: 13, component: CopyOutline }) }
        )
      ])
    }
  },
  {
    title: t('admin.images.table.size'),
    key: 'sizeBytes',
    width: 120,
    render(row) {
      return h('span', { class: 'mono-text' }, formatBytes(row.sizeBytes))
    }
  },
  {
    title: t('admin.images.table.pullTime'),
    key: 'createdAt',
    width: 170,
    render(row) {
      return h('span', { class: 'time-text' }, row.createdAt)
    }
  },
  {
    title: t('admin.images.table.actions'),
    key: 'actions',
    width: 220,
    fixed: 'right',
    render(row) {
      return h('div', { class: 'table-action-btns' }, [
        // 查看分层详情按钮
        h(
          NButton,
          {
            size: 'tiny',
            quaternary: true,
            type: 'default',
            onClick: () => openDetailDrawer(row)
          },
          {
            icon: () => h(NIcon, { size: 13, component: EyeOutline }),
            default: () => t('admin.images.table.detail')
          }
        ),
        // 导出按钮
        h(
          NButton,
          {
            size: 'tiny',
            quaternary: true,
            type: 'default',
            loading: exportingImageId.value === row.id,
            disabled: exportingImageId.value !== null,
            onClick: () => handleExportImage(row)
          },
          {
            icon: () => h(NIcon, { size: 13, component: DownloadOutline }),
            default: () => t('admin.images.table.export')
          }
        ),
        // 删除按钮
        h(
          NPopconfirm,
          {
            positiveText: '确认删除',
            negativeText: '取消',
            onPositiveClick: () => handleDeleteImage(row)
          },
          {
            trigger: () =>
              h(
                NButton,
                { size: 'tiny', quaternary: true, type: 'error' },
                {
                  icon: () => h(NIcon, { size: 13, component: TrashOutline }),
                  default: () => t('admin.images.table.delete')
                }
              ),
            default: () =>
              `确定要从中心 Registry 彻底删除镜像 [${row.fullImageName}] 吗？相关的分层数据与数据库元数据将被物理清除。`
          }
        )
      ])
    }
  }
])

function getInstructionKeyword(inst?: string): string {
  if (!inst) return 'CMD'
  const firstWord = inst.trim().split(' ')[0]
  return firstWord.toUpperCase()
}

function getInstructionTagType(inst?: string): 'info' | 'success' | 'warning' | 'error' | 'default' {
  const kw = getInstructionKeyword(inst)
  switch (kw) {
    case 'RUN':
      return 'info'
    case 'COPY':
    case 'ADD':
      return 'warning'
    case 'CMD':
    case 'ENTRYPOINT':
      return 'success'
    case 'ENV':
    case 'ARG':
      return 'default'
    case 'EXPOSE':
      return 'info'
    default:
      return 'default'
  }
}

async function fetchStatistics() {
  try {
    const res = await getAdminImageStatisticsApi()
    if (res.data) {
      Object.assign(statistics, res.data)
    }
  } catch (err) {
    console.warn('Failed to load image statistics:', err)
  }
}

async function fetchImages() {
  loadingTable.value = true
  try {
    const res = await getAdminImageListApi({
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value || undefined
    })
    if (res.data) {
      imageList.value = res.data.list
      totalCount.value = res.data.total
    }
  } catch (err) {
    console.warn('Failed to load images:', err)
  } finally {
    loadingTable.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  fetchImages()
  fetchStatistics()
}

function handlePageChange(page: number) {
  currentPage.value = page
  fetchImages()
}

function handlePageSizeChange(size: number) {
  pageSize.value = size
  currentPage.value = 1
  fetchImages()
}

async function handleScanAndSync() {
  scanningImages.value = true
  try {
    await scanAndSyncAdminImagesApi()
    message.success('已完成中心私有 Registry 镜像扫描同步与元数据核验！')
    await fetchImages()
    await fetchStatistics()
  } catch {
    message.error('扫描同步 Registry 镜像失败')
  } finally {
    scanningImages.value = false
  }
}

async function openDetailDrawer(image: AdminClusterImageVO) {
  detailImage.value = null
  showDetailDrawer.value = true
  loadingDetail.value = true
  try {
    const res = await getAdminImageDetailApi(image.id)
    if (res.data) {
      detailImage.value = res.data
    }
  } catch {
    message.error('获取镜像深度分层详情失败')
  } finally {
    loadingDetail.value = false
  }
}

function openPullModal() {
  pullForm.image = ''
  showPullModal.value = true
}

async function handleSubmitPull() {
  if (!pullFormRef.value) return
  await pullFormRef.value.validate()

  submittingPull.value = true
  try {
    await pullAdminImageApi({
      image: pullForm.image
    })
    message.success(t('admin.images.pullModal.taskSubmitted'))
    showPullModal.value = false
    await fetchImages()
    await fetchStatistics()
  } catch {
    // handled by axios interceptor
  } finally {
    submittingPull.value = false
  }
}

async function handleExportImage(image: AdminClusterImageVO) {
  if (exportingImageId.value !== null) {
    message.warning('当前有镜像正在导出打包中，请稍候...')
    return
  }

  exportingImageId.value = image.id
  const msgReactive = message.loading(`正在实时打包导出镜像 ${image.fullImageName}，请稍候...`, {
    duration: 0
  })

  try {
    const blob = await exportAdminImageApi(image.id)
    const safeRepo = image.repository.replace(/\//g, '_')
    const filename = `${safeRepo}_${image.tag}.tar`

    const downloadUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(downloadUrl)

    message.success('镜像归档文件已成功导出并开始下载！')
  } catch (err) {
    console.error('Failed to export image:', err)
  } finally {
    msgReactive.destroy()
    exportingImageId.value = null
  }
}

async function handleDeleteImage(image: AdminClusterImageVO) {
  try {
    await deleteAdminImageApi(image.id)
    message.success('已从中心 Registry 物理删除该镜像及元数据！')
    await fetchImages()
    await fetchStatistics()
  } catch {
    // handled by axios interceptor
  }
}

async function copyToClipboard(text: string) {
  try {
    await navigator.clipboard.writeText(text)
    message.success(t('admin.images.table.copied'))
  } catch {
    message.warning(text)
  }
}

function handleAsyncTaskCompleted(e: Event) {
  const customEvent = e as CustomEvent
  const task = customEvent.detail
  if (
    task &&
    (task.taskType === 'IMAGE_PULL' || task.taskType === 'IMAGE_SYNC')
  ) {
    fetchStatistics()
    fetchImages()
  }
}

onMounted(() => {
  fetchStatistics()
  fetchImages()
  window.addEventListener('volur:async-task-completed', handleAsyncTaskCompleted)
})

onUnmounted(() => {
  window.removeEventListener('volur:async-task-completed', handleAsyncTaskCompleted)
})
</script>

<style scoped lang="scss">
.admin-image-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header {
  display: flex;
  flex-direction: column;
  gap: 4px;

  .page-title {
    font-size: 22px;
    font-weight: 700;
    margin: 0;
    color: var(--v-text-primary, #111111);
    letter-spacing: -0.3px;
  }

  .page-desc {
    font-size: 13px;
    color: #444444;
    margin: 0;
  }
}

/* Statistics Ribbon */
.stat-ribbon {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;

  .stat-mini-card {
    background-color: var(--v-bg-card, #ffffff);
    border-radius: 10px;
    padding: 16px 20px;
    display: flex;
    align-items: center;
    gap: 14px;
    border: 1px solid var(--v-border-base, #e5e4e7);
    box-shadow: var(--v-shadow-sm, 0 1px 3px rgba(0, 0, 0, 0.04));
    transition: background-color 0.3s ease, border-color 0.3s ease, transform 0.2s ease, box-shadow 0.2s ease;

    &:hover {
      transform: translateY(-2px);
      border-color: var(--v-border-focus, #bbbbbb);
      box-shadow: var(--v-shadow-md, 0 4px 8px rgba(0, 0, 0, 0.06));
    }

    .stat-icon-wrapper {
      width: 42px;
      height: 42px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;

      &.primary {
        background-color: rgba(37, 99, 235, 0.1);
        color: #2563eb;
      }

      &.warning {
        background-color: rgba(245, 158, 11, 0.1);
        color: #f59e0b;
      }

      &.info {
        background-color: rgba(99, 102, 241, 0.1);
        color: #6366f1;
      }

      &.success {
        background-color: rgba(16, 185, 129, 0.1);
        color: #10b981;
      }

      &.error {
        background-color: rgba(239, 68, 68, 0.1);
        color: #ef4444;
      }
    }

    .stat-info {
      display: flex;
      flex-direction: column;
      gap: 2px;

      .stat-value {
        font-size: 18px;
        font-weight: 700;
        color: var(--v-text-primary, #111111);
        line-height: 1.2;
      }

      .stat-label {
        font-size: 12px;
        color: var(--v-text-desc, #444444);
      }
    }
  }
}

.image-id-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  max-width: 300px;
}

.mono-text {
  font-family: 'Fira Code', monospace;
  font-size: 12.5px;
  color: #333333;
}

.font-bold {
  font-weight: 600;
}

.truncate {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.time-text {
  font-size: 12px;
  color: #444444;
}

.text-muted {
  color: #999999;
}

.table-action-btns {
  display: flex;
  align-items: center;
  gap: 6px;
}

.modal-subtext {
  font-size: 13px;
  color: #444444;
  margin: 0;
}

.modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}

/* Detail Drawer Styles */
.detail-loading-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  gap: 16px;

  .loading-subtext {
    font-size: 13px;
    color: #666666;
  }
}

.image-detail-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-header-card {
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;

  .header-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;

    .image-full-title {
      font-size: 16px;
      font-weight: 700;
      color: #0f172a;
      font-family: 'Fira Code', monospace;
    }
  }

  .header-meta-row {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 18px;
    font-size: 12.5px;

    .meta-item {
      display: flex;
      align-items: center;
      gap: 6px;

      .meta-label {
        color: #64748b;
      }
    }
  }
}

.layers-container {
  display: flex;
  flex-direction: column;
  gap: 12px;

  .layers-summary-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 13px;
    font-weight: 600;
    color: #475569;
    padding-bottom: 4px;

    .total-size-badge {
      font-family: 'Fira Code', monospace;
      color: #0f172a;
    }
  }
}

.layers-timeline {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.layer-item-card {
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  transition: all 0.2s ease;

  &:hover {
    border-color: #cbd5e1;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  }

  .layer-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 12px;

    .layer-left {
      display: flex;
      align-items: center;
      gap: 8px;

      .layer-badge {
        font-family: 'Fira Code', monospace;
        font-weight: 700;
        color: #64748b;
        background-color: #f1f5f9;
        padding: 2px 6px;
        border-radius: 4px;
      }

      .instruction-tag {
        font-weight: 700;
        font-family: 'Fira Code', monospace;
      }

      .layer-id-text {
        font-family: 'Fira Code', monospace;
        color: #94a3b8;
        max-width: 140px;
      }
    }

    .layer-right {
      display: flex;
      align-items: center;
      gap: 12px;

      .layer-size {
        font-size: 12px;
        color: #0f172a;
        font-weight: 600;
      }

      .layer-time {
        color: #94a3b8;
        font-size: 11.5px;
      }
    }
  }

  .layer-command-box {
    background-color: #f8fafc;
    border-radius: 6px;
    padding: 8px 10px;
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 8px;
    position: relative;

    .command-text {
      font-family: 'Fira Code', monospace;
      font-size: 12px;
      line-height: 1.5;
      color: #1e293b;
      word-break: break-all;
      white-space: pre-wrap;
    }

    .copy-btn {
      opacity: 0.6;
      flex-shrink: 0;

      &:hover {
        opacity: 1;
      }
    }
  }
}

.inspect-container {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .inspect-section {
    display: flex;
    flex-direction: column;
    gap: 6px;

    .section-title {
      font-size: 13px;
      font-weight: 600;
      color: #334155;
    }

    .code-box {
      background-color: #f8fafc;
      border: 1px solid #e2e8f0;
      border-radius: 6px;
      padding: 10px 12px;
      font-family: 'Fira Code', monospace;
      font-size: 12px;
      color: #1e293b;
      word-break: break-all;
    }

    .tag-row {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
    }

    .env-list, .labels-list {
      display: flex;
      flex-direction: column;
      gap: 4px;
      background-color: #f8fafc;
      border: 1px solid #e2e8f0;
      border-radius: 6px;
      padding: 8px 12px;

      .env-row, .label-row {
        font-family: 'Fira Code', monospace;
        font-size: 12px;
        line-height: 1.6;
        word-break: break-all;

        .label-key {
          font-weight: 600;
          color: #2563eb;
          margin-right: 6px;
        }

        .label-val {
          color: #334155;
        }
      }
    }
  }
}

.basic-meta-container {
  .meta-table {
    display: flex;
    flex-direction: column;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    overflow: hidden;

    .meta-row {
      display: flex;
      align-items: center;
      padding: 12px 16px;
      border-bottom: 1px solid #f1f5f9;
      font-size: 13px;

      &:last-child {
        border-bottom: none;
      }

      .row-label {
        width: 190px;
        color: #64748b;
        flex-shrink: 0;
      }

      .row-value {
        color: #0f172a;
        word-break: break-all;
      }
    }
  }
}

.image-filter-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;

  .filter-input-search {
    width: 260px;
    min-width: 180px;
  }
}

.image-filter-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

@media (max-width: 768px) {
  :deep(.filter-header) {
    flex-direction: column;
    align-items: stretch !important;
    gap: 12px;

    .filter-left,
    .filter-right {
      width: 100%;
    }
  }

  .image-filter-left {
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: 10px;

    .filter-input-search {
      width: 100% !important;
    }

    .search-btn {
      width: 100%;
      justify-content: center;
    }
  }

  .image-filter-right {
    width: 100%;
    display: flex;
    gap: 10px;

    :deep(.n-button) {
      flex: 1;
      justify-content: center;
    }
  }
}
</style>
