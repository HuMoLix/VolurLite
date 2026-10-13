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
            自建私有 Registry
          </div>
          <div class="text-sm sm:text-base font-bold text-google-green dark:text-google-green-dark mt-1 truncate">
            {{ stats.registryOnline !== false ? '服务在线就绪' : '未连接/离线' }}
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, h } from 'vue'
import { useI18n } from 'vue-i18n'
import { NIcon, useMessage, useDialog, type DataTableColumns } from 'naive-ui'
import {
  LayersOutline,
  HardwareChipOutline,
  CheckmarkCircleOutline,
  SearchOutline,
  RefreshOutline,
  SyncOutline,
  CloudDownloadOutline
} from '@vicons/ionicons5'
import VBtn from '@/components/common/VBtn.vue'
import VTable from '@/components/common/VTable.vue'
import VSearch from '@/components/common/VSearch.vue'
import VPagination from '@/components/common/VPagination.vue'
import VModal from '@/components/common/VModal.vue'
import VInput from '@/components/common/VInput.vue'
import type {
  AdminClusterImageVO,
  AdminClusterImageStatisticsVO
} from '@/types/image'
import {
  getAdminImageStatisticsApi,
  getAdminImageListApi,
  scanAndSyncAdminImagesApi,
  pullAdminImageApi,
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
const transferring = ref(false)

const stats = ref<AdminClusterImageStatisticsVO>({
  totalImages: 0,
  totalSizeBytes: 0,
  registryOnline: true,
  registryImagesCount: 0
})

const showPullModal = ref(false)
const pullImageInput = ref('')

const showTransferModal = ref(false)
const transferImageId = ref<string | number | null>(null)
const transferForm = ref({
  sourceNodeIp: '',
  targetNodeIp: ''
})

const formattedTotalSize = computed(() => {
  const bytes = stats.value.totalSizeBytes || 0
  if (bytes > 1024 * 1024 * 1024) {
    return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
})

function formatBytes(bytes?: number) {
  if (!bytes) return '-'
  if (bytes > 1024 * 1024 * 1024) {
    return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

const tableColumns = computed<DataTableColumns<AdminClusterImageVO>>(() => [
  {
    title: '仓库名称 (Repository)',
    key: 'repository',
    minWidth: 180,
    render(row) {
      return h('span', { class: 'font-semibold text-google-text-primary dark:text-google-text-darkPrimary' }, row.repository)
    }
  },
  {
    title: '标签 (Tag)',
    key: 'tag',
    width: 120,
    render(row) {
      return h('span', { class: 'font-mono text-xs' }, row.tag || 'latest')
    }
  },
  {
    title: '占用体积',
    key: 'sizeBytes',
    width: 110,
    render(row) {
      return h('span', { class: 'text-xs font-mono text-google-text-secondary dark:text-google-text-darkSecondary' }, formatBytes(row.sizeBytes))
    }
  },
  {
    title: '创建时间',
    key: 'createdAt',
    width: 160,
    render(row) {
      return h('span', { class: 'text-xs text-google-text-secondary dark:text-google-text-darkSecondary font-mono' }, row.createdAt ? new Date(row.createdAt).toLocaleString() : '-')
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
