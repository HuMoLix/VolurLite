<template>
  <VModal
    :show="show"
    title="提交 Write Up"
    :width="680"
    :max-width="'92vw'"
    :radius="20"
    @update:show="handleClose"
    @close="handleClose"
  >
    <div class="flex flex-col gap-4 sm:gap-5 select-none">
      <div class="flex flex-col gap-2.5 p-3.5 sm:p-4 rounded-xl bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light/70 dark:border-google-border-dark/70 text-xs sm:text-sm leading-relaxed text-[#3c4043] dark:text-google-text-darkDesc">
        <div class="font-semibold text-google-text-primary dark:text-google-text-darkPrimary text-xs sm:text-sm">
          提交说明
        </div>
        <div class="flex flex-col gap-2">
          <div class="flex items-start gap-1.5">
            <span class="font-semibold text-google-blue dark:text-google-blue-dark shrink-0">1、WriteUp提交时间：</span>
            <span>请根据首页的“比赛公告”里所要求的提交时间。</span>
          </div>
          <div class="flex items-start gap-1.5">
            <span class="font-semibold text-google-blue dark:text-google-blue-dark shrink-0">2、提交形式：</span>
            <span>将所有答对题目的writeup整理成【一个】PDF文档（文档模板可通过比赛平台下载，仅为参考，请书写内容完整），文档命名：战队名称.pdf，通过比赛平台进行上传。</span>
          </div>
          <div class="flex items-start gap-1.5">
            <span class="font-semibold text-google-blue dark:text-google-blue-dark shrink-0">3、其他说明：</span>
            <span>每个参赛队伍可提交writeup不超过5次；如文件大于100M，请按照战队名称.pdf的文档命名格式分开上传。文件名请使用汉字、字母、数字的组合，不要使用特殊字符。</span>
          </div>
        </div>
      </div>

      <div class="flex flex-col gap-2.5 p-3.5 sm:p-4 rounded-xl bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light/70 dark:border-google-border-dark/70 text-xs sm:text-sm">
        <div class="flex items-center justify-between">
          <div class="font-semibold text-google-text-primary dark:text-google-text-darkPrimary text-xs sm:text-sm">
            提交结果
          </div>
          <span class="font-mono text-xs font-normal text-google-text-muted dark:text-google-text-darkMuted">
            已提交 {{ submissions.length }} / 5 次
          </span>
        </div>
        <VTable
          :bordered="false"
          size="small"
          :radius="12"
          :columns="columns"
          :data="submissions"
          :loading="loadingSubmissions"
          :row-key="(row: CompetitionWriteupVO) => String(row.id)"
          :max-height="160"
        >
          <template #empty>
            <div class="py-6 text-center text-xs text-google-text-muted dark:text-google-text-darkMuted">
              暂无提交记录
            </div>
          </template>
        </VTable>
      </div>

      <div class="p-4 rounded-xl border border-google-border-light dark:border-google-border-dark bg-google-surface-light/40 dark:bg-google-surface-dark/40 flex flex-col gap-3.5">
        <div class="flex items-center justify-between">
          <a
            href="javascript:void(0)"
            class="inline-flex items-center gap-1.5 text-xs sm:text-sm font-medium leading-none text-google-blue dark:text-google-blue-dark hover:underline cursor-pointer"
            @click="handleDownloadTemplate"
          >
            <n-icon :size="16" :component="DownloadOutline" class="shrink-0" />
            <span class="leading-none">Writeup 示例下载</span>
          </a>
          <span v-if="templateName" class="text-xs text-google-text-muted dark:text-google-text-darkMuted font-mono truncate max-w-[200px]">
            {{ templateName }}
          </span>
        </div>

        <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div class="flex items-center gap-2.5 min-w-0 flex-1">
            <input
              ref="fileInputRef"
              type="file"
              accept=".pdf,application/pdf"
              class="hidden"
              @change="handleFileChange"
            />
            <VBtn
              clz="normal"
              :left-icon="AttachOutline"
              @click="triggerSelectFile"
            >
              选择文件
            </VBtn>
            <div class="min-w-0 flex-1 flex items-center gap-1.5 text-xs text-google-text-desc dark:text-google-text-darkDesc">
              <VTooltip v-if="selectedFile" :content="selectedFile.name">
                <span class="font-mono text-[#3c4043] dark:text-google-text-darkPrimary truncate cursor-default">
                  {{ selectedFile.name }} ({{ formatFileSize(selectedFile.size) }})
                </span>
              </VTooltip>
              <span v-else class="text-google-text-muted dark:text-google-text-darkMuted italic">
                未选择文件（支持PDF，单文件≤100M）
              </span>
              <button
                v-if="selectedFile"
                type="button"
                class="bg-transparent border-none p-0 inline-flex items-center justify-center text-google-text-muted hover:text-google-red transition-colors cursor-pointer outline-none shrink-0"
                @click="clearSelectedFile"
              >
                <n-icon :size="14" :component="CloseCircleOutline" />
              </button>
            </div>
          </div>

          <div class="flex items-center gap-2 shrink-0">
            <div class="w-32">
              <VInput
                v-model:value="captchaCode"
                maxlength="6"
                placeholder="验证码"
                @keydown.enter="handleSubmit"
              />
            </div>
            <div
              class="h-9 w-24 rounded-lg border border-google-border-light dark:border-google-border-dark overflow-hidden bg-white dark:bg-google-surface-dark flex items-center justify-center cursor-pointer select-none relative group"
              title="点击刷新验证码"
              @click="refreshCaptcha"
            >
              <img
                v-if="captchaImage"
                :src="captchaImage"
                alt="captcha"
                class="w-full h-full object-cover"
              />
              <div v-else class="text-[11px] text-google-text-muted">
                <n-spin size="small" />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <VBtn
        clz="primary"
        :loading="submitting"
        :disabled="submitting || !selectedFile || !captchaCode.trim() || submissions.length >= 5"
        @click="handleSubmit"
      >
        <template #left-icon>
          <n-icon :size="16" :component="CloudUploadOutline" />
        </template>
        提交 Write Up
      </VBtn>
    </template>
  </VModal>
</template>

<script setup lang="ts">
import { ref, watch, h, computed } from 'vue'
import { NIcon, NSpin, useMessage } from 'naive-ui'
import type { DataTableColumns } from 'naive-ui'
import {
  DownloadOutline,
  AttachOutline,
  CloseCircleOutline,
  CloudUploadOutline
} from '@vicons/ionicons5'
import { VBtn, VInput, VTooltip, VTable, VModal } from '@/components/common'
import {
  getMyWriteupsApi,
  submitWriteupApi,
  getWriteupTemplateApi
} from '@/api/competition'
import { getImageCaptchaApi } from '@/api/captcha'
import { uploadResourceApi } from '@/api/system'
import type {
  CompetitionDetailVO,
  MyTeamInfoVO,
  CompetitionWriteupVO
} from '@/types/competition'

defineOptions({
  name: 'VWriteupModal'
})

const props = withDefaults(
  defineProps<{
    show: boolean
    competition?: CompetitionDetailVO | null
    team?: MyTeamInfoVO | null
  }>(),
  {
    competition: null,
    team: null
  }
)

const emit = defineEmits<{
  (e: 'update:show', val: boolean): void
  (e: 'submitted', record: CompetitionWriteupVO): void
}>()

const message = useMessage()

const submissions = ref<CompetitionWriteupVO[]>([])
const loadingSubmissions = ref<boolean>(false)
const submitting = ref<boolean>(false)

function formatDateTime(str?: string | null): string {
  if (!str) return '-'
  try {
    const d = new Date(str)
    if (isNaN(d.getTime())) return str
    const pad = (n: number) => (n < 10 ? `0${n}` : `${n}`)
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  } catch {
    return str
  }
}

const columns = computed<DataTableColumns<CompetitionWriteupVO>>(() => [
  {
    title: '序号',
    key: 'index',
    width: 64,
    align: 'center',
    render: (_row, idx) => h('span', { class: 'font-mono' }, idx + 1)
  },
  {
    title: '提交人',
    key: 'username',
    width: 120,
    ellipsis: { tooltip: true },
    render: (row) => row.username || row.userId || '-'
  },
  {
    title: '提交时间',
    key: 'createdAt',
    width: 160,
    render: (row) =>
      h(
        'span',
        { class: 'font-mono text-[11px] text-google-text-muted dark:text-google-text-darkMuted' },
        formatDateTime(row.createdAt)
      )
  },
  {
    title: '上传WriteUp名称',
    key: 'fileName',
    ellipsis: true,
    render: (row) => {
      if (row.fileUrl) {
        return h(
          VTooltip,
          { content: row.fileName },
          {
            default: () =>
              h(
                'a',
                {
                  href: row.fileUrl,
                  target: '_blank',
                  rel: 'noopener noreferrer',
                  class: 'hover:underline truncate block font-mono text-google-blue dark:text-google-blue-dark'
                },
                row.fileName
              )
          }
        )
      }
      return h(
        VTooltip,
        { content: row.fileName },
        {
          default: () =>
            h(
              'span',
              { class: 'truncate block font-mono text-google-blue dark:text-google-blue-dark' },
              row.fileName
            )
        }
      )
    }
  }
])

const templateUrl = ref<string>('')
const templateName = ref<string>('')

const fileInputRef = ref<HTMLInputElement | null>(null)
const selectedFile = ref<File | null>(null)

const captchaId = ref<string>('')
const captchaImage = ref<string>('')
const captchaCode = ref<string>('')

function handleClose() {
  emit('update:show', false)
}

function triggerSelectFile() {
  fileInputRef.value?.click()
}

function clearSelectedFile() {
  selectedFile.value = null
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
  }
}

function handleFileChange(ev: Event) {
  const target = ev.target as HTMLInputElement
  const files = target.files
  if (!files || files.length === 0) {
    return
  }
  const file = files[0]
  if (!file.name.toLowerCase().endsWith('.pdf') && file.type !== 'application/pdf') {
    message.error('提交格式必须为 PDF 文档 (.pdf)')
    clearSelectedFile()
    return
  }
  if (file.size > 100 * 1024 * 1024) {
    message.warning('文件大小超过 100MB，请按照战队名称.pdf命名分开上传')
  }
  selectedFile.value = file
}

function formatFileSize(bytes?: number | null): string {
  if (!bytes || bytes <= 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${(bytes / Math.pow(k, i)).toFixed(1)} ${sizes[i]}`
}


async function refreshCaptcha() {
  try {
    captchaImage.value = ''
    const res = await getImageCaptchaApi({
      scene: 'DEFAULT',
      type: 'LINE',
      oldCaptchaId: captchaId.value || undefined
    })
    if (res?.data) {
      captchaId.value = res.data.captchaId
      captchaImage.value = res.data.imageBase64
    }
  } catch {
  }
}

async function fetchSubmissions() {
  if (!props.competition?.id) return
  loadingSubmissions.value = true
  try {
    const res = await getMyWriteupsApi(props.competition.id)
    submissions.value = res?.data || []
  } catch {
    submissions.value = []
  } finally {
    loadingSubmissions.value = false
  }
}

async function fetchTemplate() {
  if (!props.competition?.id) return
  try {
    const res = await getWriteupTemplateApi(props.competition.id)
    templateUrl.value = res?.data?.templateUrl || ''
    templateName.value = res?.data?.templateName || ''
  } catch {
    templateUrl.value = ''
    templateName.value = ''
  }
}

function handleDownloadTemplate() {
  if (templateUrl.value) {
    window.open(templateUrl.value, '_blank')
    return
  }
  const sampleContent = `%PDF-1.4\n% WriteUp Template\n1 0 obj\n<< /Title (CTF WriteUp Template) >>\nendobj\ntrailer\n<< /Root 1 0 R >>\n%%EOF`
  const blob = new Blob([sampleContent], { type: 'application/pdf' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = templateName.value || 'Writeup_Template.pdf'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

async function handleSubmit() {
  if (submissions.value.length >= 5) {
    message.error('每个参赛队伍提交 Write Up 不得超过 5 次')
    return
  }
  if (!selectedFile.value) {
    message.error('请先选择要上传的 Write Up PDF 文件')
    return
  }
  if (!captchaCode.value.trim()) {
    message.error('请输入图形验证码')
    return
  }
  if (!props.competition?.id) {
    message.error('竞赛信息不存在')
    return
  }

  submitting.value = true
  try {
    const uploadRes = await uploadResourceApi(selectedFile.value, 'writeups')
    const fileUrl = uploadRes?.data?.filePath || uploadRes?.data?.fileName || ''

    const submitRes = await submitWriteupApi(props.competition.id, {
      fileName: selectedFile.value.name,
      fileUrl,
      fileSize: selectedFile.value.size,
      captchaId: captchaId.value,
      captchaCode: captchaCode.value.trim()
    })

    message.success('Write Up 提交成功！')
    clearSelectedFile()
    captchaCode.value = ''
    await fetchSubmissions()
    await refreshCaptcha()
    if (submitRes?.data) {
      emit('submitted', submitRes.data)
    }
  } catch (err: any) {
    message.error(err?.message || '提交 Write Up 失败，请检查验证码或重试')
    await refreshCaptcha()
  } finally {
    submitting.value = false
  }
}

watch(
  () => props.show,
  (val) => {
    if (val) {
      fetchSubmissions()
      fetchTemplate()
      refreshCaptcha()
      captchaCode.value = ''
      clearSelectedFile()
    }
  }
)
</script>
