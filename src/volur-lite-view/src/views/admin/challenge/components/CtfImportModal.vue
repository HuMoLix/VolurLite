<template>
  <n-modal
    v-model:show="visible"
    preset="card"
    title="导入 CTF 赛题 (ZIP)"
    style="width: 600px; max-width: 95vw;"
    :mask-closable="false"
    class="ctf-import-modal"
  >
    <div class="import-dialog-content">
      <!-- Action & Template Download Bar -->
      <div class="template-action-bar">
        <div class="template-tip">
          支持单个标准赛题包（含 <code>challenge.json</code> 及题目附件）
        </div>
        <n-button
          size="small"
          secondary
          :loading="downloadingTemplate"
          @click="handleDownloadTemplate"
        >
          <template #icon>
            <n-icon :component="CloudDownloadOutline" />
          </template>
          下载标准题目包模版
        </n-button>
      </div>

      <!-- Upload Dragger (Shown when not imported yet) -->
      <n-upload
        v-if="!importResult"
        directory-dnd
        :max="1"
        accept=".zip"
        :custom-request="handleCustomImportRequest"
        :show-file-list="false"
      >
        <n-upload-dragger>
          <div class="upload-dropzone">
            <n-icon size="48" :depth="3" :component="CloudUploadOutline" />
            <div class="upload-text">点击或拖拽题目 ZIP 压缩包至此区域上传</div>
            <p class="upload-hint">仅支持上传单个赛题包 ZIP 文件，最大支持 500MB</p>
          </div>
        </n-upload-dragger>
      </n-upload>

      <!-- Loading State -->
      <div v-if="importing" class="import-loading-state">
        <n-spin size="large" />
        <p class="loading-text">正在解压解析赛题包并处理附件转存与镜像拉取调度，请稍候...</p>
      </div>

      <!-- Import Result Summary -->
      <div v-if="importResult" class="import-result-view">
        <div class="result-card" :class="importResult.successCount > 0 ? 'success' : 'failed'">
          <div class="status-indicator">
            <n-icon
              size="28"
              :component="importResult.successCount > 0 ? CheckmarkCircleOutline : CloseCircleOutline"
            />
            <div class="status-text">
              <span class="main-status">
                {{ importResult.successCount > 0 ? '赛题导入成功' : '赛题导入失败' }}
              </span>
              <span class="sub-status">
                {{ importResult.successCount > 0 ? '赛题已成功录入系统题库' : '请根据错误提示调整题目包内容后重新上传' }}
              </span>
            </div>
          </div>
        </div>

        <!-- Triggered Image Pull Tasks -->
        <div
          v-if="importResult.triggeredImagePullTasks && importResult.triggeredImagePullTasks.length > 0"
          class="image-tasks-box"
        >
          <div class="sub-title">
            <n-icon :component="CubeOutline" />
            <span>已自动触发以下 Docker 镜像后台拉取任务：</span>
          </div>
          <div class="tags-wrap">
            <n-tag
              v-for="img in importResult.triggeredImagePullTasks"
              :key="img"
              type="success"
              size="small"
            >
              {{ img }}
            </n-tag>
          </div>
        </div>

        <!-- Error Messages -->
        <div
          v-if="importResult.errors && importResult.errors.length > 0"
          class="errors-box"
        >
          <div class="sub-title error-text">
            <n-icon :component="CloseCircleOutline" />
            <span>导入失败说明：</span>
          </div>
          <ul class="error-items">
            <li v-for="(err, idx) in importResult.errors" :key="idx">{{ err }}</li>
          </ul>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="modal-footer">
        <n-button v-if="importResult" secondary @click="resetModal">继续导入其他</n-button>
        <n-button type="primary" class="theme-primary-btn" @click="closeModal">
          {{ importResult ? '完成并刷新列表' : '取消' }}
        </n-button>
      </div>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  NModal,
  NUpload,
  NUploadDragger,
  NSpin,
  NIcon,
  NTag,
  NButton,
  useMessage,
  type UploadCustomRequestOptions
} from 'naive-ui'
import {
  CloudUploadOutline,
  CloudDownloadOutline,
  CubeOutline,
  CloseCircleOutline,
  CheckmarkCircleOutline
} from '@vicons/ionicons5'
import { importAdminCtfChallengeApi, downloadAdminCtfTemplateApi } from '@/api/challenge'
import type { ChallengeImportResultVO } from '@/types/challenge'

interface Props {
  show: boolean
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:show', value: boolean): void
  (e: 'success', result: ChallengeImportResultVO): void
}>()

const message = useMessage()

const visible = computed({
  get: () => props.show,
  set: (val: boolean) => emit('update:show', val)
})

const importing = ref(false)
const downloadingTemplate = ref(false)
const importResult = ref<ChallengeImportResultVO | null>(null)

function resetModal() {
  importResult.value = null
  importing.value = false
}

function closeModal() {
  const hasSuccess = importResult.value && importResult.value.successCount > 0
  const savedResult = importResult.value
  visible.value = false
  resetModal()
  if (hasSuccess && savedResult) {
    emit('success', savedResult)
  }
}

async function handleDownloadTemplate() {
  downloadingTemplate.value = true
  try {
    const blob = await downloadAdminCtfTemplateApi()
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = 'volur-ctf-challenge-template.zip'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    message.success('标准题目包模版下载成功')
  } catch (err) {
    console.error('Download template error:', err)
    message.error('下载标准题目模版失败')
  } finally {
    downloadingTemplate.value = false
  }
}

async function handleCustomImportRequest(options: UploadCustomRequestOptions) {
  const file = options.file.file
  if (!file) return
  importing.value = true
  importResult.value = null
  try {
    const uploadForm = new FormData()
    uploadForm.append('file', file)
    const res = await importAdminCtfChallengeApi(uploadForm)
    if (res.success && res.data) {
      importResult.value = res.data
      if (res.data.successCount > 0) {
        message.success(`题目【${options.file.name.replace(/\.zip$/i, '')}】导入成功！`)
      } else {
        message.warning('导入未成功，请查看详细错误信息。')
      }
    }
  } catch (err: unknown) {
    console.error('Import challenge failed:', err)
    const errorMsg = err instanceof Error ? err.message : '题目导入异常，请检查 ZIP 结构'
    message.error(errorMsg)
  } finally {
    importing.value = false
  }
}
</script>

<style lang="scss" scoped>
.import-dialog-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.template-action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: var(--v-bg-secondary, #f8f9fa);
  border: 1px solid var(--v-border-color, #ebebeb);
  border-radius: 8px;
  gap: 12px;

  .template-tip {
    font-size: 13px;
    color: #444444;

    code {
      background-color: rgba(0, 0, 0, 0.06);
      padding: 1px 5px;
      border-radius: 4px;
      font-size: 12px;
      font-family: monospace;
    }
  }
}

.upload-dropzone {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 20px;

  .upload-text {
    margin-top: 12px;
    font-size: 15px;
    font-weight: 500;
  }

  .upload-hint {
    font-size: 12px;
    color: #888888;
    margin-top: 6px;
  }
}

.import-loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;

  .loading-text {
    margin-top: 16px;
    font-size: 13px;
    color: #555555;
  }
}

.import-result-view {
  display: flex;
  flex-direction: column;
  gap: 14px;

  .result-card {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 14px 16px;
    border-radius: 8px;
    border: 1px solid var(--v-border-color, #ebebeb);

    &.success {
      background: #f0fdf4;
      border-color: #bbf7d0;

      .status-indicator {
        color: #16a34a;
      }
    }

    &.failed {
      background: #fef2f2;
      border-color: #fecaca;

      .status-indicator {
        color: #dc2626;
      }
    }

    .status-indicator {
      display: flex;
      align-items: center;
      gap: 12px;

      .status-text {
        display: flex;
        flex-direction: column;
        gap: 2px;

        .main-status {
          font-size: 15px;
          font-weight: 600;
          color: #1f2937;
        }

        .sub-status {
          font-size: 12px;
          color: #444444;
        }
      }
    }
  }

  .image-tasks-box {
    border: 1px solid #bbf7d0;
    background: #f0fdf4;
    border-radius: 8px;
    padding: 12px 14px;

    .sub-title {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;
      font-weight: 600;
      color: #166534;
      margin-bottom: 8px;
    }

    .tags-wrap {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
    }
  }

  .errors-box {
    border: 1px solid #fecaca;
    background: #fef2f2;
    border-radius: 8px;
    padding: 12px 14px;

    .sub-title {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;
      font-weight: 600;
      color: #991b1b;
      margin-bottom: 6px;
    }

    .error-items {
      margin: 0;
      padding-left: 18px;
      font-size: 12px;
      color: #b91c1c;
      line-height: 1.5;
    }
  }
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.theme-primary-btn {
  background-color: #000000 !important;
  color: #ffffff !important;
  border: none !important;

  &:hover {
    background-color: #333333 !important;
  }
}
</style>
