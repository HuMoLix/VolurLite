<template>
  <VModal
    v-model:show="visible"
    :title="t('admin.challenges.import_modal_title', '导入 CTF 赛题 (ZIP)')"
    :width="620"
    @close="handleClose"
  >
    <div class="space-y-4">
      <div class="flex items-center justify-between p-3 rounded-lg bg-gray-50 dark:bg-[#202124] border border-google-border-light dark:border-google-border-dark text-xs text-google-text-secondary dark:text-google-text-darkSecondary">
        <span>{{ t('admin.challenges.import_tip', '支持标准单个赛题包 (含 challenge.json 及附件)') }}</span>
        <VBtn
          clz="normal"
          size="medium"
          :left-icon="CloudDownloadOutline"
          :loading="downloadingTemplate"
          @click="handleDownloadTemplate"
        >
          {{ t('admin.challenges.download_template', '下载题目包模板') }}
        </VBtn>
      </div>

      <div
        v-if="!importResult"
        class="border-2 border-dashed rounded-xl p-8 text-center transition-colors border-google-border-light dark:border-google-border-dark hover:border-google-blue dark:hover:border-google-blue-dark cursor-pointer bg-white dark:bg-[#292a2d]"
        @click="triggerFileInput"
        @dragover.prevent
        @drop.prevent="handleDrop"
      >
        <input
          ref="fileInputRef"
          type="file"
          accept=".zip"
          class="hidden"
          @change="handleFileChange"
        />
        <div class="flex flex-col items-center justify-center gap-2">
          <CloudUploadOutline class="w-12 h-12 text-google-text-secondary dark:text-google-text-darkSecondary" />
          <div class="text-sm font-medium text-google-text-primary dark:text-google-text-darkPrimary">
            {{ selectedFile ? selectedFile.name : t('admin.challenges.upload_click_drag', '点击或拖拽题目 ZIP 压缩包至此区域') }}
          </div>
          <p class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary">
            {{ t('admin.challenges.upload_limit', '仅支持单个赛题包 ZIP 文件，最大支持 500MB') }}
          </p>
        </div>
      </div>

      <div v-if="importing" class="p-6 text-center space-y-2">
        <div class="text-sm text-google-blue dark:text-google-blue-dark font-medium animate-pulse">
          {{ t('admin.challenges.importing_text', '正在解压解析赛题包并处理附件与镜像，请稍候...') }}
        </div>
      </div>

      <div v-if="importResult" class="p-4 rounded-xl border space-y-3" :class="importResult.successCount > 0 ? 'bg-green-50/60 dark:bg-green-950/20 border-green-200 dark:border-green-800' : 'bg-red-50/60 dark:bg-red-950/20 border-red-200 dark:border-red-800'">
        <div class="flex items-center gap-2 font-semibold text-sm" :class="importResult.successCount > 0 ? 'text-green-700 dark:text-green-400' : 'text-red-700 dark:text-red-400'">
          <span>{{ importResult.successCount > 0 ? t('admin.challenges.import_success_title', '导入成功') : t('admin.challenges.import_fail_title', '导入失败') }}</span>
        </div>
        <div class="text-xs space-y-1 text-google-text-secondary dark:text-google-text-darkSecondary">
          <div>{{ t('admin.challenges.import_total', '总解析数') }}: {{ importResult.totalCount }}</div>
          <div>{{ t('admin.challenges.import_success_count', '成功数') }}: {{ importResult.successCount }}</div>
          <div>{{ t('admin.challenges.import_failed_count', '失败数') }}: {{ importResult.failedCount }}</div>
        </div>
        <div v-if="importResult.errors && importResult.errors.length > 0" class="p-2 rounded bg-white dark:bg-[#202124] text-xs text-red-600 dark:text-red-400 space-y-1 max-h-32 overflow-y-auto">
          <div v-for="(err, idx) in importResult.errors" :key="idx">{{ err }}</div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="flex justify-end gap-3">
        <VBtn
          v-if="!importResult"
          clz="primary"
          size="medium"
          :disabled="!selectedFile"
          :loading="importing"
          @click="handleSubmitImport"
        >
          {{ t('admin.challenges.confirm_import', '确认导入') }}
        </VBtn>
        <VBtn
          v-else
          clz="normal"
          size="medium"
          @click="handleClose"
        >
          {{ t('common.close', '关闭') }}
        </VBtn>
      </div>
    </template>
  </VModal>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useMessage } from 'naive-ui'
import { CloudUploadOutline, CloudDownloadOutline } from '@vicons/ionicons5'
import VModal from '@/components/common/VModal.vue'
import VBtn from '@/components/common/VBtn.vue'
import { importAdminCtfChallengeApi, downloadAdminCtfTemplateApi } from '@/api/challenge'
import type { ChallengeImportResultVO } from '@/types/challenge'

const props = defineProps<{
  show: boolean
}>()

const emit = defineEmits<{
  (e: 'update:show', val: boolean): void
  (e: 'success'): void
}>()

const { t } = useI18n()
const message = useMessage()

const visible = computed({
  get: () => props.show,
  set: (val) => emit('update:show', val)
})

const fileInputRef = ref<HTMLInputElement | null>(null)
const selectedFile = ref<File | null>(null)
const downloadingTemplate = ref(false)
const importing = ref(false)
const importResult = ref<ChallengeImportResultVO | null>(null)

function triggerFileInput() {
  fileInputRef.value?.click()
}

function handleFileChange(event: Event) {
  const target = event.target as HTMLInputElement
  if (target.files && target.files.length > 0) {
    selectedFile.value = target.files[0]
  }
}

function handleDrop(event: DragEvent) {
  if (event.dataTransfer?.files && event.dataTransfer.files.length > 0) {
    const file = event.dataTransfer.files[0]
    if (file.name.endsWith('.zip')) {
      selectedFile.value = file
    } else {
      message.warning(t('admin.challenges.zip_only', '仅支持 ZIP 格式文件'))
    }
  }
}

async function handleDownloadTemplate() {
  downloadingTemplate.value = true
  try {
    const blob = await downloadAdminCtfTemplateApi()
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'ctf_challenge_template.zip'
    a.click()
    window.URL.revokeObjectURL(url)
  } catch {
  } finally {
    downloadingTemplate.value = false
  }
}

async function handleSubmitImport() {
  if (!selectedFile.value) return
  importing.value = true
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    const res = await importAdminCtfChallengeApi(formData)
    if (res.data) {
      importResult.value = res.data
      if (res.data.successCount > 0) {
        emit('success')
      }
    }
  } catch {
  } finally {
    importing.value = false
  }
}

function handleClose() {
  selectedFile.value = null
  importResult.value = null
  visible.value = false
}
</script>
