<template>
  <div class="space-y-6 max-w-5xl mx-auto">
    <div class="flex items-center justify-between gap-4">
      <div class="flex items-center gap-3">
        <h2 class="text-xl font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
          {{ isEditMode ? t('admin.challenges.edit_challenge_title', '编辑 CTF 赛题') : t('admin.challenges.create_challenge_title', '新增 CTF 赛题') }}
        </h2>
      </div>

      <div class="flex items-center gap-3">
        <Transition
          enter-active-class="transition duration-200 ease-out"
          enter-from-class="opacity-0 translate-y-1 scale-95"
          enter-to-class="opacity-100 translate-y-0 scale-100"
          leave-active-class="transition duration-150 ease-in"
          leave-from-class="opacity-100 translate-y-0 scale-100"
          leave-to-class="opacity-0 translate-y-1 scale-95"
        >
          <div v-if="isDirty" class="flex items-center gap-3">
            <VBtn clz="normal" size="medium" @click="handleCancel">
              {{ t('common.cancel', '取消') }}
            </VBtn>
            <VBtn
              clz="primary"
              size="medium"
              :left-icon="SaveOutline"
              :loading="saving"
              @click="handleSave"
            >
              {{ isEditMode ? t('common.save', '保存修改') : t('admin.challenges.create_btn', '创建题目') }}
            </VBtn>
          </div>
        </Transition>
      </div>
    </div>

    <div v-if="loadingDetail" class="p-12 text-center text-google-blue dark:text-google-blue-dark">
      {{ t('common.loading', '正在加载数据...') }}
    </div>

    <div v-else class="space-y-6">
      <div class="p-6 rounded-2xl bg-white dark:bg-[#292a2d] border border-google-border-light dark:border-google-border-dark space-y-5">
        <h3 class="text-sm font-semibold text-google-text-primary dark:text-google-text-darkPrimary border-b border-google-border-light dark:border-google-border-dark pb-3">
          {{ t('admin.challenges.basic_info', '基本信息') }}
        </h3>

        <div class="space-y-4">
          <div>
            <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
              {{ t('admin.challenges.challenge_name', '赛题名称') }} *
            </label>
            <VInput
              v-model:value="formData.name"
              :placeholder="t('admin.challenges.name_placeholder', '请输入赛题名称 (如: Easy SQL)')"
              clearable
            />
          </div>

          <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
                {{ t('admin.challenges.category', '方向分类') }} *
              </label>
              <VSelection
                v-model:value="formData.categoryId"
                :options="categoryOptions"
                :placeholder="t('admin.challenges.category_placeholder', '请选择赛题方向')"
              />
            </div>

            <div>
              <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
                {{ t('admin.challenges.type', '赛题类型') }} *
              </label>
              <VSelection
                v-model:value="formData.type"
                :options="typeOptions"
                :render-label="renderTypeOptionLabel"
              />
            </div>

            <div>
              <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
                {{ t('admin.challenges.difficulty', '难度级别') }} *
              </label>
              <VSelection
                v-model:value="formData.difficulty"
                :options="difficultyOptions"
              />
            </div>
          </div>

          <div>
            <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
              {{ t('admin.challenges.flag', '静态 Flag (动态靶机可留空)') }}
            </label>
            <VInput
              v-model:value="formData.flag"
              :placeholder="t('admin.challenges.flag_placeholder', '例如: flag{example_flag_here}')"
              clearable
            />
          </div>
        </div>
      </div>

      <div class="p-6 rounded-2xl bg-white dark:bg-[#292a2d] border border-google-border-light dark:border-google-border-dark space-y-4">
        <h3 class="text-sm font-semibold text-google-text-primary dark:text-google-text-darkPrimary border-b border-google-border-light dark:border-google-border-dark pb-3">
          {{ t('admin.challenges.desc_section', '赛题描述 (Markdown)') }}
        </h3>

        <VMarkdownEditor
          v-model:value="formData.description"
          :placeholder="t('admin.challenges.desc_placeholder', '支持标准 Markdown 语法，详细书写题目背景与指引...')"
          :rows="8"
          min-height="200px"
          max-height="600px"
        />
      </div>

      <div
        v-if="isContainerType"
        class="p-6 rounded-2xl bg-white dark:bg-[#292a2d] border border-google-border-light dark:border-google-border-dark space-y-4"
      >
        <h3 class="text-sm font-semibold text-google-text-primary dark:text-google-text-darkPrimary border-b border-google-border-light dark:border-google-border-dark pb-3">
          {{ t('admin.challenges.container_settings', '动态靶机与容器配置') }}
        </h3>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div class="md:col-span-2">
            <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
              {{ t('admin.challenges.image_name', 'Docker 镜像名称') }} *
            </label>
            <VInput
              v-model:value="formData.imageName"
              :placeholder="t('admin.challenges.image_name_placeholder', '如: registry.ctf.local/web/challenge:latest')"
              clearable
            />
          </div>

          <div>
            <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
              {{ t('admin.challenges.container_port', '靶机暴露端口') }}
            </label>
            <VInput
              v-model:value="containerPortStr"
              type="text"
              :placeholder="'80'"
              clearable
            />
          </div>

          <div>
            <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
              {{ t('admin.challenges.memory_limit', '内存限制 (MB)') }}
            </label>
            <VInput
              v-model:value="memoryLimitStr"
              type="text"
              :placeholder="'256'"
              clearable
            />
          </div>
        </div>
      </div>

      <div class="p-6 rounded-2xl bg-white dark:bg-[#292a2d] border border-google-border-light dark:border-google-border-dark space-y-4">
        <h3 class="text-sm font-semibold text-google-text-primary dark:text-google-text-darkPrimary border-b border-google-border-light dark:border-google-border-dark pb-3">
          {{ t('admin.challenges.attachment_section', '题目附件') }}
        </h3>

        <div class="space-y-4">
          <VTable
            :columns="attachmentColumns"
            :data="formData.attachments"
            :pagination="false"
            :bordered="false"
            :scroll-x="680"
            :row-key="(row: CtfAttachmentVO) => row.url || `${row.name}-${row.size}-${row.uploadedAt}`"
          />

          <div class="flex justify-end">
            <input
              ref="attachmentInputRef"
              type="file"
              class="hidden"
              multiple
              @change="handleUploadAttachment"
            />
            <VBtn
              clz="normal"
              size="medium"
              :left-icon="CloudUploadOutline"
              @click="triggerAttachmentUpload"
            >
              {{ t('admin.challenges.upload_attachment', '上传题目附件') }}
            </VBtn>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useMessage, type DataTableColumns } from 'naive-ui'
import {
  SaveOutline,
  CloudUploadOutline,
  TrashOutline
} from '@vicons/ionicons5'
import VBtn from '@/components/common/VBtn.vue'
import VInput from '@/components/common/VInput.vue'
import VSelection from '@/components/common/VSelection.vue'
import VIcon from '@/components/common/VIcon.vue'
import VMarkdownEditor from '@/components/common/VMarkdownEditor.vue'
import VTable from '@/components/common/VTable.vue'
import { useUserStore } from '@/store/modules/user'
import {
  getAdminCtfChallengeDetailApi,
  createAdminCtfChallengeApi,
  updateAdminCtfChallengeApi,
  getAdminCtfCategoriesApi,
  uploadAdminCtfAttachmentApi
} from '@/api/challenge'
import type {
  ChallengeType,
  ChallengeDifficulty,
  CtfCategoryVO,
  CtfAttachmentVO
} from '@/types/challenge'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const message = useMessage()
const userStore = useUserStore()

const challengeId = computed(() => route.params.id as string | undefined)
const isEditMode = computed(() => Boolean(challengeId.value))

const loadingDetail = ref(false)
const saving = ref(false)
const categoryList = ref<CtfCategoryVO[]>([])

const attachmentInputRef = ref<HTMLInputElement | null>(null)

const formData = ref({
  name: '',
  description: '',
  categoryId: '',
  type: 'STATIC' as ChallengeType,
  difficulty: 'normal' as ChallengeDifficulty,
  flag: '',
  imageName: '',
  containerPort: 80,
  memoryLimit: 256,
  attachments: [] as CtfAttachmentVO[]
})

const initialSnapshot = ref('')

const isDirty = computed(() => {
  if (!initialSnapshot.value) return false
  return JSON.stringify(formData.value) !== initialSnapshot.value
})

const containerPortStr = computed<string>({
  get: () => String(formData.value.containerPort || ''),
  set: (val) => {
    formData.value.containerPort = parseInt(val, 10) || 80
  }
})

const memoryLimitStr = computed<string>({
  get: () => String(formData.value.memoryLimit || ''),
  set: (val) => {
    formData.value.memoryLimit = parseInt(val, 10) || 256
  }
})

const isContainerType = computed(() => {
  return formData.value.type === 'CONTAINER' || formData.value.type === 'DYNAMIC'
})

const categoryOptions = computed(() => {
  return categoryList.value.map(cat => ({
    label: cat.name,
    value: String(cat.id)
  }))
})

const typeOptions = [
  { label: '静态赛题', value: 'STATIC', icon: 'DocumentTextOutline' },
  { label: '动态靶机', value: 'DYNAMIC', icon: 'CubeOutline' },
  { label: '独立容器', value: 'CONTAINER', icon: 'ServerOutline' }
]

const difficultyOptions = [
  { label: '入门', value: 'easy' },
  { label: '简单', value: 'normal' },
  { label: '中等', value: 'mid' },
  { label: '进阶', value: 'hard' },
  { label: '困难', value: 'difficult' }
]

function renderTypeOptionLabel(option: any) {
  return h(
    'div',
    { class: 'flex items-center gap-2' },
    [
      option.icon
        ? h(VIcon, {
            name: option.icon as string,
            size: 16
          })
        : null,
      h('span', option.label as string)
    ]
  )
}

function handleCancel() {
  if (isEditMode.value && initialSnapshot.value) {
    formData.value = JSON.parse(initialSnapshot.value)
    message.info(t('admin.challenges.changes_cancelled', '已取消修改'))
  } else {
    router.push('/admin/challenges/ctf')
  }
}

async function fetchCategories() {
  try {
    const res = await getAdminCtfCategoriesApi()
    if (res.data) {
      categoryList.value = res.data
    }
  } catch {
    categoryList.value = []
  }
}

async function fetchDetail() {
  if (!challengeId.value) return
  loadingDetail.value = true
  try {
    const res = await getAdminCtfChallengeDetailApi(challengeId.value)
    if (res.data) {
      const d = res.data
      const author = d.authorName || ''
      const createdAt = d.createdAt || ''
      const mappedAttachments = (d.attachments || []).map((att) => ({
        ...att,
        uploader: att.uploader || author || '-',
        uploadedAt: att.uploadedAt || createdAt || undefined
      }))
      formData.value = {
        name: d.name || '',
        description: d.description || '',
        categoryId: String(d.categoryId || ''),
        type: d.type || 'STATIC',
        difficulty: (d.difficulty ? String(d.difficulty).toLowerCase() : 'normal') as ChallengeDifficulty,
        flag: d.flag || '',
        imageName: d.imageName || '',
        containerPort: d.containerPort || 80,
        memoryLimit: d.memoryLimit || 256,
        attachments: mappedAttachments
      }
      initialSnapshot.value = JSON.stringify(formData.value)
    }
  } catch {
    router.push('/admin/challenges/ctf')
  } finally {
    loadingDetail.value = false
  }
}

function triggerAttachmentUpload() {
  attachmentInputRef.value?.click()
}

function handleUploadAttachment(event: Event) {
  const target = event.target as HTMLInputElement
  if (target.files && target.files.length > 0) {
    for (let i = 0; i < target.files.length; i++) {
      const file = target.files[i]
      const item: CtfAttachmentVO = {
        name: file.name,
        url: '',
        size: file.size,
        uploader: userStore.displayName || userStore.user?.username || '-',
        uploadedAt: new Date().toISOString(),
        rawFile: file
      }
      formData.value.attachments.push(item)
    }
    target.value = ''
  }
}

function formatFileSize(bytes?: number | null): string {
  if (bytes === undefined || bytes === null) return '-'
  if (bytes <= 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${(bytes / Math.pow(k, i)).toFixed(1)} ${sizes[i]}`
}

function formatDateTime(instantOrString: string | number | Date | null | undefined): string {
  if (!instantOrString) return '-'
  const date = new Date(instantOrString)
  if (isNaN(date.getTime())) return String(instantOrString)
  const Y = date.getFullYear()
  const M = String(date.getMonth() + 1).padStart(2, '0')
  const D = String(date.getDate()).padStart(2, '0')
  const h = String(date.getHours()).padStart(2, '0')
  const m = String(date.getMinutes()).padStart(2, '0')
  const s = String(date.getSeconds()).padStart(2, '0')
  return `${Y}-${M}-${D} ${h}:${m}:${s}`
}

const attachmentColumns = computed<DataTableColumns<CtfAttachmentVO>>(() => [
  {
    title: t('admin.challenges.attachment_index_col', '序号'),
    key: 'index',
    width: 60,
    render(_row, index) {
      return h(
        'span',
        { class: 'text-xs text-google-text-secondary dark:text-google-text-darkSecondary font-mono' },
        index + 1
      )
    }
  },
  {
    title: t('admin.challenges.attachment_name_col', '附件名称'),
    key: 'name',
    minWidth: 160,
    render(row) {
      if (row.url) {
        return h(
          'a',
          {
            href: row.url,
            target: '_blank',
            rel: 'noopener noreferrer',
            class: 'font-medium text-google-blue dark:text-google-blue-dark hover:underline truncate inline-block max-w-xs'
          },
          row.name
        )
      }
      return h('div', { class: 'flex items-center gap-2 max-w-xs' }, [
        h('span', { class: 'font-medium text-google-text-primary dark:text-google-text-darkPrimary truncate' }, row.name),
        row.rawFile
          ? h('span', { class: 'text-[11px] px-1.5 py-0.5 rounded bg-amber-500/10 text-amber-600 dark:text-amber-400 flex-shrink-0' }, t('admin.challenges.pending_upload', '待上传'))
          : null
      ])
    }
  },
  {
    title: t('admin.challenges.attachment_uploader_col', '上传人'),
    key: 'uploader',
    width: 120,
    render(row) {
      return h(
        'span',
        { class: 'text-xs text-google-text-secondary dark:text-google-text-darkSecondary truncate inline-block max-w-[100px]' },
        row.uploader || '-'
      )
    }
  },
  {
    title: t('admin.challenges.attachment_upload_time_col', '上传时间'),
    key: 'uploadedAt',
    width: 170,
    render(row) {
      return h(
        'span',
        { class: 'text-xs text-google-text-secondary dark:text-google-text-darkSecondary whitespace-nowrap' },
        formatDateTime(row.uploadedAt)
      )
    }
  },
  {
    title: t('admin.challenges.attachment_size_col', '文件大小'),
    key: 'size',
    width: 110,
    render(row) {
      return h(
        'span',
        { class: 'text-xs text-google-text-secondary dark:text-google-text-darkSecondary' },
        formatFileSize(row.size)
      )
    }
  },
  {
    title: t('common.actions', '操作'),
    key: 'actions',
    width: 90,
    render(row) {
      return h(
        VBtn,
        {
          clz: 'danger',
          size: 'tiny',
          leftIcon: TrashOutline,
          onClick: () => handleRemoveAttachment(row)
        },
        { default: () => t('common.remove', '移除') }
      )
    }
  }
])

function handleRemoveAttachment(target: CtfAttachmentVO | number) {
  if (typeof target === 'number') {
    formData.value.attachments.splice(target, 1)
  } else {
    const idx = formData.value.attachments.indexOf(target)
    if (idx !== -1) {
      formData.value.attachments.splice(idx, 1)
    }
  }
}

async function handleSave() {
  if (!formData.value.name.trim()) {
    message.warning(t('admin.challenges.name_required', '请填写赛题名称'))
    return
  }
  if (!formData.value.categoryId) {
    message.warning(t('admin.challenges.category_required', '请选择赛题分类'))
    return
  }
  if (isContainerType.value && !formData.value.imageName.trim()) {
    message.warning(t('admin.challenges.image_name_required', '容器赛题必须填写 Docker 镜像名称'))
    return
  }

  saving.value = true
  try {
    for (const att of formData.value.attachments) {
      if (att.rawFile) {
        const fileData = new FormData()
        fileData.append('file', att.rawFile)
        const uploadRes = await uploadAdminCtfAttachmentApi(fileData)
        if (!uploadRes.data?.url) {
          throw new Error(t('admin.challenges.upload_failed', '上传附件失败'))
        }
        att.url = uploadRes.data.url
        if (uploadRes.data.size) {
          att.size = uploadRes.data.size
        }
        att.rawFile = undefined
      }
    }

    const cleanAttachments: CtfAttachmentVO[] = formData.value.attachments.map((att) => ({
      name: att.name,
      url: att.url,
      size: att.size,
      uploader: att.uploader,
      uploadedAt: att.uploadedAt
    }))

    const payload = {
      name: formData.value.name.trim(),
      description: formData.value.description.trim(),
      categoryId: formData.value.categoryId,
      type: formData.value.type,
      difficulty: formData.value.difficulty,
      flag: formData.value.flag.trim() || undefined,
      imageName: isContainerType.value ? formData.value.imageName.trim() : undefined,
      containerPort: isContainerType.value ? formData.value.containerPort : undefined,
      memoryLimit: isContainerType.value ? formData.value.memoryLimit : undefined,
      attachments: cleanAttachments
    }

    if (isEditMode.value && challengeId.value) {
      await updateAdminCtfChallengeApi(challengeId.value, payload)
      message.success(t('admin.challenges.update_success', '赛题更新成功'))
    } else {
      await createAdminCtfChallengeApi(payload)
      message.success(t('admin.challenges.create_success', '赛题创建成功'))
    }
    initialSnapshot.value = JSON.stringify(formData.value)
    router.push('/admin/challenges/ctf')
  } catch {
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await fetchCategories()
  if (isEditMode.value) {
    await fetchDetail()
  } else {
    initialSnapshot.value = JSON.stringify(formData.value)
  }
})
</script>
