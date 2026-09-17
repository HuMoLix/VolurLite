<template>
  <div class="writeup-tab">
    <!-- Section 1: Writeup Settings & Template Management -->
    <n-card class="wp-config-card" :bordered="false">
      <div class="card-header-bar">
        <div class="title-group">
          <h3 class="card-title">Writeup 提交规则与模板设置</h3>
          <p class="card-subtitle">
            配置参赛队伍是否需要在赛后提交解题过程报告 (WP)、设定提交起止时间窗口并提供官方标准模版下载。
          </p>
        </div>
        <n-button
          type="primary"
          class="theme-primary-btn"
          :loading="savingConfig"
          @click="handleSaveWpConfig"
        >
          保存 WP 规则
        </n-button>
      </div>

      <n-grid :x-gap="24" :y-gap="16" cols="1 s:2 m:3" responsive="screen">
        <n-grid-item>
          <div class="form-item-block">
            <span class="item-label">开放选手提交 WP</span>
            <div class="switch-row">
              <n-switch v-model:value="wpConfig.allowWriteupSubmission" />
              <span class="field-hint">
                {{ wpConfig.allowWriteupSubmission ? '允许提交' : '禁止提交' }}
              </span>
            </div>
          </div>
        </n-grid-item>

        <n-grid-item :span="2">
          <div class="form-item-block">
            <span class="item-label">WP 允许提交时间窗口</span>
            <n-date-picker
              v-model:value="wpTimeRange"
              type="datetimerange"
              clearable
              format="yyyy-MM-dd HH:mm:ss"
              placeholder="请设定 WP 提交开始与截止时间"
              style="width: 100%;"
            />
          </div>
        </n-grid-item>

        <!-- Template Upload & Download -->
        <n-grid-item :span="3">
          <div class="template-section">
            <span class="item-label">官方 WP 模版文件 (供选手下载)</span>
            <div class="template-action-row">
              <input
                ref="templateFileInputRef"
                type="file"
                accept=".docx,.pdf,.md,.zip"
                style="display: none;"
                @change="handleTemplateFileChange"
              />

              <n-button
                secondary
                size="small"
                :loading="uploadingTemplate"
                @click="triggerTemplateSelect"
              >
                <template #icon>
                  <n-icon :component="CloudUploadOutline" />
                </template>
                {{ wpConfig.writeupTemplateUrl ? '重新上传模板' : '上传官方 WP 模板 (.docx / .pdf / .md / .zip)' }}
              </n-button>

              <div v-if="wpConfig.writeupTemplateUrl" class="template-file-badge">
                <n-icon :component="DocumentTextOutline" />
                <span class="fname">{{ wpConfig.writeupTemplateName || '官方题解报告模板' }}</span>
                <n-button
                  quaternary
                  circle
                  size="tiny"
                  @click="downloadFile(wpConfig.writeupTemplateUrl, wpConfig.writeupTemplateName || 'template')"
                >
                  <template #icon>
                    <n-icon :component="DownloadOutline" />
                  </template>
                </n-button>
              </div>
              <span v-else class="empty-tip">未上传官方模板，选手将自由排版提交</span>
            </div>
          </div>
        </n-grid-item>
      </n-grid>
    </n-card>

    <!-- Section 2: Submitted Writeups List -->
    <AppTableCard
      :bordered="true"
      :table-bordered="true"
      :columns="columns"
      :data="filteredWriteups"
      :loading="loading"
      :scroll-x="1150"
      :row-key="(row) => row.id"
      table-class="writeup-data-table"
    >
      <template #filter-left>
        <h3 class="section-title" style="margin: 0;">选手已提交题解列表 ({{ writeups.length }})</h3>
      </template>

      <template #filter-right>
        <div class="tab-filter-right">
          <n-input
            v-model:value="searchKeyword"
            placeholder="搜索队伍名、队长或文件名..."
            clearable
            class="filter-search-input"
            @keyup.enter="fetchWriteups"
          />
          <n-button type="primary" class="theme-primary-btn" @click="fetchWriteups">查询</n-button>
          <n-button quaternary :loading="loading" @click="fetchWriteups">
            <template #icon>
              <n-icon :component="RefreshOutline" />
            </template>
            刷新
          </n-button>
        </div>
      </template>

      <template #empty>
        <div style="padding: 24px; text-align: center; color: #888;">
          当前比赛暂无队伍提交 Writeup
        </div>
      </template>
    </AppTableCard>

    <!-- Section 3: Live Document Rendering Modal (Supports DOCX, PDF, Markdown) -->
    <n-modal
      v-model:show="showPreviewModal"
      preset="card"
      :title="`题解实时在线渲染预览 - ${previewingWriteup?.fileName || ''}`"
      style="width: 92%; max-width: 1200px; height: 90vh;"
    >
      <div class="preview-modal-body">
        <!-- Top Toolbar in Modal -->
        <div class="preview-header-bar">
          <div class="meta-info">
            <span class="team-tag">战队：{{ previewingWriteup?.teamName }}</span>
            <span class="uploader-tag">提交人：{{ previewingWriteup?.username }}</span>
            <n-tag size="small" :type="getFileTypeTag(previewingWriteup?.fileType)" round>
              {{ previewingWriteup?.fileType?.toUpperCase() }}
            </n-tag>
          </div>
          <div class="download-action">
            <n-button
              size="small"
              type="primary"
              ghost
              @click="downloadFile(previewingWriteup?.fileUrl || '', previewingWriteup?.fileName || 'writeup')"
            >
              <template #icon>
                <n-icon :component="DownloadOutline" />
              </template>
              下载原文件
            </n-button>
          </div>
        </div>

        <!-- Render Viewport -->
        <div class="viewer-viewport">
          <n-spin :show="previewRendering">
            <!-- Format A: Markdown -->
            <div
              v-if="isMarkdownFormat(previewingWriteup?.fileType)"
              class="markdown-viewer-wrap volur-markdown-body"
              v-html="markdownHtmlContent"
            />

            <!-- Format B: PDF -->
            <div
              v-else-if="isPdfFormat(previewingWriteup?.fileType)"
              class="pdf-viewer-wrap"
            >
              <iframe
                v-if="previewingWriteup?.fileUrl"
                :src="previewingWriteup.fileUrl"
                class="pdf-iframe"
              />
            </div>

            <!-- Format C: DOCX -->
            <div
              v-else-if="isDocxFormat(previewingWriteup?.fileType)"
              class="docx-viewer-wrap"
            >
              <div ref="docxContainerRef" class="docx-render-container" />
            </div>

            <!-- Format D: Other (ZIP, etc.) -->
            <div v-else class="unsupported-viewer-wrap">
              <n-icon size="48" class="tip-icon"><DocumentTextOutline /></n-icon>
              <p class="title">该文件格式为压缩包或其它格式，不支持直接在线文档渲染</p>
              <p class="sub">请点击右上角「下载原文件」下载并在本地解压查阅。</p>
            </div>
          </n-spin>
        </div>
      </div>
    </n-modal>

    <!-- Audit & Score Modal -->
    <n-modal
      v-model:show="showAuditModal"
      preset="card"
      :title="`评审队伍题解 - ${auditingWriteup?.teamName || ''}`"
      style="width: 480px;"
    >
      <div v-if="auditingWriteup" class="audit-modal-form">
        <n-form label-placement="top">
          <n-form-item label="评审结论">
            <n-select
              v-model:value="auditForm.status"
              :options="auditStatusOptions"
            />
          </n-form-item>
          <n-form-item label="WP 附加得分 (可选，0~1000 pts)">
            <n-input-number
              v-model:value="auditForm.score"
              :min="0"
              :max="5000"
              style="width: 100%;"
            />
          </n-form-item>
          <n-form-item label="评语反馈 (选手可见)">
            <n-input
              v-model:value="auditForm.comment"
              type="textarea"
              placeholder="请输入针对该题解的评审意见或技术评语..."
              :rows="4"
            />
          </n-form-item>

          <div class="modal-actions">
            <n-button @click="showAuditModal = false">取消</n-button>
            <n-button class="theme-primary-btn" :loading="auditSubmitting" @click="handleSaveAudit">
              提交评审
            </n-button>
          </div>
        </n-form>
      </div>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, h, nextTick, onMounted, watch } from 'vue'
import {
  NCard,
  NButton,
  NIcon,
  NSpin,
  NModal,
  NForm,
  NFormItem,
  NInput,
  NInputNumber,
  NSelect,
  NSwitch,
  NDatePicker,
  NTag,
  NGrid,
  NGridItem,
  useMessage,
  useDialog,
  type DataTableColumns
} from 'naive-ui'
import AppTableCard from '@/components/common/AppTableCard.vue'
import {
  CloudUploadOutline,
  DocumentTextOutline,
  DownloadOutline,
  RefreshOutline
} from '@vicons/ionicons5'
import type {
  CompetitionWriteupVO,
  CompetitionWriteupAuditDTO,
  AdminCompetitionDetailVO
} from '@/types/competition'
import {
  listAdminCompetitionWriteupsApi,
  auditAdminCompetitionWriteupApi,
  deleteAdminCompetitionWriteupApi,
  updateCompetitionApi
} from '@/api/competition'
import { uploadResourceApi } from '@/api/system'
import { renderMarkdown } from '@/utils/markdown'
import { renderAsync as renderDocxAsync } from 'docx-preview'

interface Props {
  compId: string | number
  initialData: AdminCompetitionDetailVO | null
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'refresh'): void
}>()

const message = useMessage()
const dialog = useDialog()

// WP Config State
const savingConfig = ref<boolean>(false)
const uploadingTemplate = ref<boolean>(false)
const templateFileInputRef = ref<HTMLInputElement | null>(null)

const wpConfig = reactive({
  allowWriteupSubmission: true,
  writeupTemplateUrl: '',
  writeupTemplateName: '',
  writeupStartTime: undefined as string | undefined,
  writeupEndTime: undefined as string | undefined
})

const wpTimeRange = ref<[number, number] | null>(null)

// Writeup Table State
const loading = ref<boolean>(false)
const writeups = ref<CompetitionWriteupVO[]>([])
const searchKeyword = ref<string>('')

// Document Preview State
const showPreviewModal = ref<boolean>(false)
const previewRendering = ref<boolean>(false)
const previewingWriteup = ref<CompetitionWriteupVO | null>(null)
const markdownHtmlContent = ref<string>('')
const docxContainerRef = ref<HTMLElement | null>(null)

// Audit Modal State
const showAuditModal = ref<boolean>(false)
const auditSubmitting = ref<boolean>(false)
const auditingWriteup = ref<CompetitionWriteupVO | null>(null)
const auditForm = reactive<CompetitionWriteupAuditDTO>({
  status: 'AUDITED',
  score: 0,
  comment: ''
})

const auditStatusOptions = [
  { label: '审核通过 (AUDITED)', value: 'AUDITED' },
  { label: '待复核 (SUBMITTED)', value: 'SUBMITTED' },
  { label: '驳回重交 (REJECTED)', value: 'REJECTED' }
]

const filteredWriteups = computed(() => {
  if (!searchKeyword.value.trim()) return writeups.value
  const kw = searchKeyword.value.trim().toLowerCase()
  return writeups.value.filter(w =>
    (w.teamName && w.teamName.toLowerCase().includes(kw)) ||
    (w.username && w.username.toLowerCase().includes(kw)) ||
    (w.fileName && w.fileName.toLowerCase().includes(kw))
  )
})

const columns: DataTableColumns<CompetitionWriteupVO> = [
  {
    title: '战队名称',
    key: 'teamName',
    width: 180,
    render(row) {
      return h('span', { style: 'font-weight: 600; color: var(--v-text-primary, #111);' }, row.teamName)
    }
  },
  {
    title: '提交队员',
    key: 'username',
    width: 140,
    render(row) {
      return h('span', { style: 'font-size: 13px;' }, row.username)
    }
  },
  {
    title: '文件名',
    key: 'fileName',
    render(row) {
      return h('div', { style: 'display: flex; align-items: center; gap: 8px;' }, [
        h(NTag, { size: 'small', type: getFileTypeTag(row.fileType), round: true }, {
          default: () => (row.fileType || 'DOC').toUpperCase()
        }),
        h('span', { style: 'font-size: 13px;' }, row.fileName)
      ])
    }
  },
  {
    title: '大小',
    key: 'fileSize',
    width: 100,
    render(row) {
      return formatFileSize(row.fileSize)
    }
  },
  {
    title: '提交时间',
    key: 'createdAt',
    width: 170,
    render(row) {
      return new Date(row.createdAt).toLocaleString()
    }
  },
  {
    title: '状态 / 得分',
    key: 'status',
    width: 140,
    render(row) {
      return h('div', { style: 'display: flex; align-items: center; gap: 6px;' }, [
        h(NTag, { size: 'small', type: row.status === 'AUDITED' ? 'success' : row.status === 'REJECTED' ? 'error' : 'warning', round: true }, {
          default: () => getStatusLabel(row.status)
        }),
        row.score !== undefined && row.score !== null
          ? h('span', { style: 'font-weight: 600; font-size: 12px; color: #18a058;' }, `+${row.score}`)
          : null
      ])
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 220,
    fixed: 'right',
    render(row) {
      return h('div', { style: 'display: flex; gap: 8px;' }, [
        h(
          NButton,
          {
            size: 'tiny',
            quaternary: true,
            onClick: () => handlePreviewWriteup(row)
          },
          {
            default: () => '在线预览'
          }
        ),
        h(
          NButton,
          {
            size: 'tiny',
            quaternary: true,
            onClick: () => downloadFile(row.fileUrl, row.fileName)
          },
          {
            default: () => '下载'
          }
        ),
        h(
          NButton,
          {
            size: 'tiny',
            quaternary: true,
            type: 'primary',
            onClick: () => openAuditModal(row)
          },
          {
            default: () => '评分'
          }
        ),
        h(
          NButton,
          {
            size: 'tiny',
            quaternary: true,
            type: 'error',
            onClick: () => handleDeleteWriteup(row)
          },
          {
            default: () => '删除'
          }
        )
      ])
    }
  }
]

watch(
  () => props.initialData,
  (d) => {
    if (d) {
      const cfg = d.config || d
      wpConfig.allowWriteupSubmission = cfg.allowWriteupSubmission ?? true
      wpConfig.writeupTemplateUrl = cfg.writeupTemplateUrl || ''
      wpConfig.writeupTemplateName = cfg.writeupTemplateName || ''
      wpConfig.writeupStartTime = cfg.writeupStartTime
      wpConfig.writeupEndTime = cfg.writeupEndTime

      if (cfg.writeupStartTime && cfg.writeupEndTime) {
        wpTimeRange.value = [
          new Date(cfg.writeupStartTime).getTime(),
          new Date(cfg.writeupEndTime).getTime()
        ]
      } else {
        wpTimeRange.value = null
      }
    }
  },
  { immediate: true }
)

onMounted(() => {
  if (props.compId) {
    fetchWriteups()
  }
})

async function fetchWriteups() {
  loading.value = true
  try {
    const res = await listAdminCompetitionWriteupsApi(props.compId, {
      keyword: searchKeyword.value.trim() ? searchKeyword.value.trim() : undefined
    })
    writeups.value = res.data || []
  } catch (error: any) {
    console.error('Fetch writeups error:', error)
    message.error(error.message || '获取题解列表失败')
  } finally {
    loading.value = false
  }
}

// Template Upload
function triggerTemplateSelect() {
  templateFileInputRef.value?.click()
}

async function handleTemplateFileChange(e: Event) {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  if (file.size > 50 * 1024 * 1024) {
    message.error('WP 模板文件大小不能超过 50MB')
    target.value = ''
    return
  }

  uploadingTemplate.value = true
  try {
    const fd = new FormData()
    fd.append('file', file)
    const res = await uploadResourceApi(fd, 'competitions')
    const uploadedPath = res.data?.filePath || ''
    if (uploadedPath) {
      wpConfig.writeupTemplateUrl = uploadedPath
      wpConfig.writeupTemplateName = file.name
      message.success('WP 官方模板文件上传成功')
    } else {
      message.error('上传模板失败')
    }
  } catch (error: any) {
    message.error(error.message || '上传模板失败')
  } finally {
    uploadingTemplate.value = false
    target.value = ''
  }
}

async function handleSaveWpConfig() {
  savingConfig.value = true
  try {
    let startIso: string | undefined
    let endIso: string | undefined
    if (wpTimeRange.value && wpTimeRange.value.length === 2) {
      startIso = new Date(wpTimeRange.value[0]).toISOString()
      endIso = new Date(wpTimeRange.value[1]).toISOString()
    }

    await updateCompetitionApi(props.compId, {
      config: {
        allowWriteupSubmission: wpConfig.allowWriteupSubmission,
        writeupTemplateUrl: wpConfig.writeupTemplateUrl || undefined,
        writeupTemplateName: wpConfig.writeupTemplateName || undefined,
        writeupStartTime: startIso,
        writeupEndTime: endIso
      }
    })
    message.success('WP 提交规则与模板设置已保存')
    emit('refresh')
  } catch (error: any) {
    message.error(error.message || '保存 WP 规则失败')
  } finally {
    savingConfig.value = false
  }
}

// Online Document Rendering
async function handlePreviewWriteup(row: CompetitionWriteupVO) {
  previewingWriteup.value = row
  markdownHtmlContent.value = ''
  showPreviewModal.value = true
  previewRendering.value = true

  const ftype = (row.fileType || '').toLowerCase()

  try {
    if (isMarkdownFormat(ftype)) {
      // Fetch text content
      const resp = await fetch(row.fileUrl)
      const text = await resp.text()
      markdownHtmlContent.value = renderMarkdown(text)
    } else if (isDocxFormat(ftype)) {
      await nextTick()
      if (docxContainerRef.value) {
        docxContainerRef.value.innerHTML = ''
        const resp = await fetch(row.fileUrl)
        const blob = await resp.blob()
        await renderDocxAsync(blob, docxContainerRef.value, undefined, {
          className: 'volur-docx-body',
          inWrapper: true,
          ignoreWidth: false,
          ignoreHeight: false
        })
      }
    }
    // PDF is rendered directly by iframe
  } catch (error: any) {
    console.error('Render preview error:', error)
    message.error('在线渲染文档失败，请尝试直接下载原文件')
  } finally {
    previewRendering.value = false
  }
}

function openAuditModal(row: CompetitionWriteupVO) {
  auditingWriteup.value = row
  auditForm.status = row.status || 'AUDITED'
  auditForm.score = row.score || 0
  auditForm.comment = row.comment || ''
  showAuditModal.value = true
}

async function handleSaveAudit() {
  if (!auditingWriteup.value) return
  auditSubmitting.value = true
  try {
    await auditAdminCompetitionWriteupApi(props.compId, auditingWriteup.value.id, auditForm)
    message.success('评审题解成功')
    showAuditModal.value = false
    fetchWriteups()
  } catch (error: any) {
    message.error(error.message || '评审失败')
  } finally {
    auditSubmitting.value = false
  }
}

function handleDeleteWriteup(row: CompetitionWriteupVO) {
  dialog.warning({
    title: '确认删除题解',
    content: `确定要删除队伍「${row.teamName}」提交的题解报告吗？`,
    positiveText: '确认删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteAdminCompetitionWriteupApi(props.compId, row.id)
        message.success('题解记录已删除')
        fetchWriteups()
      } catch (error: any) {
        message.error(error.message || '删除失败')
      }
    }
  })
}

function downloadFile(url: string, filename: string) {
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.target = '_blank'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
}

function isMarkdownFormat(ftype?: string) {
  const f = (ftype || '').toLowerCase()
  return f === 'md' || f === 'markdown'
}

function isPdfFormat(ftype?: string) {
  const f = (ftype || '').toLowerCase()
  return f === 'pdf'
}

function isDocxFormat(ftype?: string) {
  const f = (ftype || '').toLowerCase()
  return f === 'docx' || f === 'doc'
}

function getFileTypeTag(ftype?: string) {
  const f = (ftype || '').toLowerCase()
  if (f === 'pdf') return 'error'
  if (f === 'docx' || f === 'doc') return 'info'
  if (f === 'md' || f === 'markdown') return 'success'
  return 'default'
}

function getStatusLabel(status: string) {
  switch (status) {
    case 'AUDITED':
      return '已审阅'
    case 'REJECTED':
      return '已驳回'
    case 'SUBMITTED':
    default:
      return '待评审'
  }
}

function formatFileSize(bytes: number) {
  if (!bytes || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}
</script>

<style scoped lang="scss">
.writeup-tab {
  display: flex;
  flex-direction: column;
  gap: 20px;

  .wp-config-card {
    background: var(--v-bg-muted, #f8f8f9);
    border: 1px solid var(--v-border-base, #e5e4e7);

    .card-header-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;

      .card-title {
        font-size: 15px;
        font-weight: 600;
        margin: 0 0 4px 0;
        color: var(--v-text-primary, #111111);
      }

      .card-subtitle {
        font-size: 13px;
        color: #444444;
        margin: 0;
      }
    }

    .form-item-block {
      display: flex;
      flex-direction: column;
      gap: 8px;

      .item-label {
        font-size: 13px;
        font-weight: 500;
        color: var(--v-text-primary, #111111);
      }

      .switch-row {
        display: flex;
        align-items: center;
        height: 34px;

        .field-hint {
          margin-left: 12px;
          font-size: 12px;
          color: #444444;
        }
      }
    }

    .template-section {
      display: flex;
      flex-direction: column;
      gap: 8px;

      .item-label {
        font-size: 13px;
        font-weight: 500;
        color: var(--v-text-primary, #111111);
      }

      .template-action-row {
        display: flex;
        align-items: center;
        gap: 16px;
        flex-wrap: wrap;

        .template-file-badge {
          display: flex;
          align-items: center;
          gap: 6px;
          background: var(--v-bg-card, #ffffff);
          padding: 4px 10px;
          border-radius: 6px;
          border: 1px solid var(--v-border-base, #e5e4e7);
          font-size: 13px;

          .fname {
            color: var(--v-text-primary, #111111);
            font-weight: 500;
          }
        }

        .empty-tip {
          font-size: 12px;
          color: var(--v-text-muted, #999999);
        }
      }
    }
  }

  .wp-table-card {
    .table-top-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
      flex-wrap: wrap;
      gap: 12px;

      .section-title {
        font-size: 15px;
        font-weight: 600;
        margin: 0;
        color: var(--v-text-primary, #111111);
      }

      .header-right {
        display: flex;
        align-items: center;
        gap: 12px;
      }
    }
  }
}

.preview-modal-body {
  display: flex;
  flex-direction: column;
  height: 75vh;

  .preview-header-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 12px;
    border-bottom: 1px solid var(--v-border-base, #e5e4e7);
    margin-bottom: 12px;

    .meta-info {
      display: flex;
      align-items: center;
      gap: 12px;
      font-size: 13px;

      .team-tag {
        font-weight: 600;
        color: var(--v-text-primary, #111111);
      }

      .uploader-tag {
        color: #444444;
      }
    }
  }

  .viewer-viewport {
    flex: 1;
    overflow-y: auto;
    background: var(--v-bg-muted, #f8f8f9);
    border-radius: 8px;
    padding: 16px;

    .markdown-viewer-wrap {
      background: var(--v-bg-card, #ffffff);
      padding: 24px;
      border-radius: 8px;
      border: 1px solid var(--v-border-base, #e5e4e7);
    }

    .pdf-viewer-wrap {
      width: 100%;
      height: 100%;
      min-height: 600px;

      .pdf-iframe {
        width: 100%;
        height: 100%;
        min-height: 600px;
        border: none;
        border-radius: 8px;
      }
    }

    .docx-viewer-wrap {
      display: flex;
      justify-content: center;

      .docx-render-container {
        background: #ffffff;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
        border-radius: 4px;
        padding: 40px;
        width: 100%;
        max-width: 900px;
        min-height: 600px;
      }
    }

    .unsupported-viewer-wrap {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 300px;
      text-align: center;

      .tip-icon {
        color: var(--v-text-muted, #999999);
        margin-bottom: 12px;
      }

      .title {
        font-size: 15px;
        font-weight: 600;
        color: var(--v-text-primary, #111111);
        margin-bottom: 4px;
      }

      .sub {
        font-size: 13px;
        color: #444444;
      }
    }
  }
}

.audit-modal-form {
  .modal-actions {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    margin-top: 20px;
  }
}

.tab-filter-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;

  .filter-search-input {
    width: 240px;
  }
}

@media (max-width: 768px) {
  .tab-filter-right {
    display: flex;
    flex-direction: column;
    align-items: stretch;
    width: 100%;
    gap: 10px;

    .filter-search-input {
      width: 100% !important;
    }

    .n-button {
      width: 100%;
      justify-content: center;
    }
  }
}
</style>
