<template>
  <div class="admin-ctf-challenge-edit-view">
    <!-- Subview Top Header with Back action & Breadcrumbs -->
    <div class="subview-header">
      <div class="header-left">
        <n-button text size="small" class="back-btn" @click="handleBack">
          <template #icon>
            <n-icon :component="ArrowBackOutline" />
          </template>
          {{ t('admin.challenges.back_to_ctf') }}
        </n-button>
        <div class="title-wrap">
          <h2 class="subview-title">
            {{ pageTitle }}
          </h2>
        </div>
      </div>

      <div class="header-right">
        <n-button secondary @click="handleBack">{{ t('admin.challenges.cancel') }}</n-button>
        <n-button
          type="primary"
          class="theme-primary-btn"
          :loading="saving"
          @click="handleSaveChallenge"
        >
          <template #icon>
            <n-icon :component="SaveOutline" />
          </template>
          {{ isEditMode ? t('admin.challenges.save_challenge') : t('admin.challenges.confirm_create_challenge') }}
        </n-button>
      </div>
    </div>

    <!-- Main Content Area -->
    <n-spin :show="loadingDetail">
      <n-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-placement="left"
        label-width="120"
        require-mark-placement="left"
        class="challenge-edit-form"
      >
        <!-- 1. 基本信息 Card -->
        <n-card class="form-section-card" :bordered="false" :title="t('admin.challenges.basic_info')">
          <n-form-item :label="t('admin.challenges.challenge_name')" path="name">
            <n-input
              v-model:value="formData.name"
              :placeholder="t('admin.challenges.challenge_name_placeholder')"
              maxlength="100"
              show-count
            />
          </n-form-item>

          <div class="form-row-2">
            <n-form-item :label="t('admin.challenges.category')" path="categoryId">
              <n-select
                v-model:value="formData.categoryId"
                :options="categoryFormOptions"
                :render-label="renderCategoryOptionLabel"
                :placeholder="t('admin.challenges.category_placeholder')"
              />
            </n-form-item>

            <n-form-item :label="t('admin.challenges.challenge_type')" path="type">
              <n-select
                v-model:value="formData.type"
                :options="typeOptions"
                :placeholder="t('admin.challenges.challenge_type_placeholder')"
              />
            </n-form-item>
          </div>

          <div class="form-row-2">
            <n-form-item :label="t('admin.challenges.difficulty')" path="difficulty">
              <n-select
                v-model:value="formData.difficulty"
                :options="difficultyOptions"
                :placeholder="t('admin.challenges.difficulty_placeholder')"
              />
            </n-form-item>

            <n-form-item :label="t('admin.challenges.static_flag')" path="flag">
              <n-input
                v-model:value="formData.flag"
                :placeholder="t('admin.challenges.static_flag_placeholder')"
              />
            </n-form-item>
          </div>
        </n-card>

        <!-- 2. 题目描述与指引 Card -->
        <n-card class="form-section-card" :bordered="false" :title="t('admin.challenges.desc_section')">
          <n-form-item :label="t('admin.challenges.desc_section')" path="description" :show-label="false">
            <VolurMarkdownEditor
              v-model:value="formData.description"
              :placeholder="t('admin.challenges.desc_placeholder')"
              min-height="260px"
              max-height="600px"
              style="width: 100%;"
            />
          </n-form-item>
        </n-card>

        <!-- 3. 赛题附件 Card (支持多附件扩展) -->
        <n-card class="form-section-card attachments-section-card" :bordered="false">
          <template #header>
            <div class="card-header-with-badge">
              <span>{{ t('admin.challenges.attachments_config') }}</span>
              <n-tag
                v-if="formData.attachments.length > 0"
                size="small"
                :bordered="false"
                round
                type="info"
              >
                {{ t('admin.challenges.attachments_count', { count: formData.attachments.length }) }}
              </n-tag>
            </div>
          </template>

          <div class="attachments-container">
            <!-- 顶部操作栏: 上传本地附件 + 快速添加外部下载链接 -->
            <div class="attachment-actions-bar">
              <n-upload
                multiple
                :show-file-list="false"
                :custom-request="handleAttachmentUpload"
              >
                <n-button secondary size="small" :loading="uploadingAttachment">
                  <template #icon>
                    <n-icon :component="CloudUploadOutline" />
                  </template>
                  {{ t('admin.challenges.upload_local_attachments') }}
                </n-button>
              </n-upload>

              <div class="external-link-row">
                <n-input
                  v-model:value="externalAttachment.name"
                  size="small"
                  :placeholder="t('admin.challenges.attachment_name_placeholder')"
                  style="width: 200px;"
                />
                <n-input
                  v-model:value="externalAttachment.url"
                  size="small"
                  :placeholder="t('admin.challenges.attachment_url_placeholder')"
                  style="flex: 1;"
                  @keydown.enter.prevent="handleAddExternalAttachment"
                />
                <n-button secondary size="small" @click="handleAddExternalAttachment">
                  <template #icon>
                    <n-icon :component="AddOutline" />
                  </template>
                  {{ t('admin.challenges.add_external_link') }}
                </n-button>
              </div>
            </div>

            <!-- 附件列表展示 -->
            <div v-if="formData.attachments.length > 0" class="attachments-list">
              <div
                v-for="(item, index) in formData.attachments"
                :key="index"
                class="attachment-item"
              >
                <div class="attachment-main">
                  <div class="attachment-icon-box">
                    <n-icon :component="DocumentTextOutline" size="20" />
                  </div>
                  <div class="attachment-details">
                    <div class="attachment-title-row">
                      <span class="attachment-name" :title="item.name">{{ item.name }}</span>
                      <span v-if="item.size" class="attachment-size">
                        {{ formatFileSize(item.size) }}
                      </span>
                    </div>
                    <div class="attachment-url" :title="item.url">
                      {{ item.url }}
                    </div>
                  </div>
                </div>

                <div class="attachment-actions">
                  <n-button
                    quaternary
                    size="small"
                    tag="a"
                    :href="item.url"
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    <template #icon>
                      <n-icon :component="DownloadOutline" />
                    </template>
                    {{ t('admin.challenges.view_download') }}
                  </n-button>

                  <n-button
                    quaternary
                    size="small"
                    type="error"
                    @click="removeAttachment(index)"
                  >
                    <template #icon>
                      <n-icon :component="TrashOutline" />
                    </template>
                    {{ t('admin.challenges.remove') }}
                  </n-button>
                </div>
              </div>
            </div>

            <!-- 暂无附件时的空状态 -->
            <div v-else class="attachments-empty">
              <n-icon :component="AttachOutline" size="28" class="empty-icon" />
              <span class="empty-text">{{ t('admin.challenges.no_attachments_tip') }}</span>
            </div>
          </div>
        </n-card>

        <!-- 4. 容器靶机配置 Card (仅当容器或动态模式时展开) -->
        <n-card
          v-if="formData.type === 'CONTAINER' || formData.type === 'DYNAMIC'"
          class="form-section-card container-config-card"
          :bordered="false"
          :title="t('admin.challenges.container_config')"
        >
          <template #header-extra>
            <div class="section-badge">
              <n-icon :component="CubeOutline" />
              <span>{{ t('admin.challenges.dynamic_instance_dispatch') }}</span>
            </div>
          </template>

          <n-form-item :label="t('admin.challenges.image_name')" path="imageName">
            <n-input
              v-model:value="formData.imageName"
              :placeholder="t('admin.challenges.image_name_placeholder')"
            />
          </n-form-item>

          <div class="form-row-3">
            <n-form-item :label="t('admin.challenges.container_port')" path="containerPort">
              <n-input-number
                v-model:value="formData.containerPort"
                placeholder="80"
                :min="1"
                :max="65535"
                style="width: 100%;"
              />
            </n-form-item>

            <n-form-item :label="t('admin.challenges.cpu_limit')" path="cpuLimit">
              <n-input-number
                v-model:value="formData.cpuLimit"
                placeholder="1000"
                :min="100"
                :step="100"
                style="width: 100%;"
              />
            </n-form-item>

            <n-form-item :label="t('admin.challenges.memory_limit')" path="memoryLimit">
              <n-input-number
                v-model:value="formData.memoryLimit"
                placeholder="512"
                :min="64"
                :step="64"
                style="width: 100%;"
              />
            </n-form-item>
          </div>
        </n-card>

        <!-- Bottom Action Bar -->
        <div class="bottom-action-bar">
          <n-button secondary size="medium" @click="handleBack">{{ t('admin.challenges.cancel_and_back') }}</n-button>
          <n-button
            type="primary"
            class="theme-primary-btn"
            size="medium"
            :loading="saving"
            @click="handleSaveChallenge"
          >
            <template #icon>
              <n-icon :component="SaveOutline" />
            </template>
            {{ isEditMode ? t('admin.challenges.save_challenge') : t('admin.challenges.confirm_create_challenge') }}
          </n-button>
        </div>
      </n-form>
    </n-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, h } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  NCard,
  NForm,
  NFormItem,
  NInput,
  NInputNumber,
  NSelect,
  NTag,
  NButton,
  NIcon,
  NUpload,
  NSpin,
  useMessage,
  type FormInst,
  type FormRules,
  type UploadCustomRequestOptions
} from 'naive-ui'
import {
  ArrowBackOutline,
  SaveOutline,
  DocumentTextOutline,
  CloudUploadOutline,
  CubeOutline,
  AddOutline,
  TrashOutline,
  DownloadOutline,
  AttachOutline
} from '@vicons/ionicons5'
import {
  getAdminCtfChallengeDetailApi,
  createAdminCtfChallengeApi,
  updateAdminCtfChallengeApi,
  getAdminCtfCategoriesApi,
  uploadAdminCtfAttachmentApi
} from '@/api/challenge'
import type {
  CtfCategoryVO,
  CtfChallengeCreateRequestDTO,
  CtfAttachmentVO,
  ChallengeType,
  ChallengeDifficulty
} from '@/types/challenge'
import { VolurIcon, VolurMarkdownEditor } from '@/public/volurEx'

interface Props {
  challengeId?: number | string
  isEmbedded?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  challengeId: undefined,
  isEmbedded: false
})

const emit = defineEmits<{
  (e: 'saved', id: number | string): void
  (e: 'cancel'): void
}>()

const router = useRouter()
const route = useRoute()
const message = useMessage()
const { t } = useI18n()

const targetId = computed<string | number | undefined>(() => {
  return props.challengeId || (route.params.id as string | undefined)
})

const isEditMode = computed<boolean>(() => Boolean(targetId.value))

const pageTitle = computed(() => {
  if (isEditMode.value) {
    const titleName = editingChallengeName.value || formData.name
    return titleName
      ? `${titleName} - ${t('admin.challenges.ctf_manage_title')}`
      : t('admin.challenges.ctf_manage_title')
  }
  return t('admin.challenges.create_ctf_title')
})

const loadingDetail = ref(false)
const saving = ref(false)
const uploadingAttachment = ref(false)
const formRef = ref<FormInst | null>(null)
const editingChallengeName = ref('')
const categoryList = ref<CtfCategoryVO[]>([])

const externalAttachment = reactive<{
  name: string
  url: string
}>({
  name: '',
  url: ''
})

const formData = reactive<{
  name: string
  categoryId: number | string | null
  type: ChallengeType
  difficulty: number
  flag: string
  description: string
  attachments: CtfAttachmentVO[]
  imageName: string
  containerPort: number | null
  cpuLimit: number | null
  memoryLimit: number | null
}>({
  name: '',
  categoryId: null,
  type: 'STATIC',
  difficulty: 1,
  flag: '',
  description: '',
  attachments: [],
  imageName: '',
  containerPort: 80,
  cpuLimit: 1000,
  memoryLimit: 512
})

const formRules = computed<FormRules>(() => ({
  name: [
    { required: true, message: t('admin.challenges.rules_name_required'), trigger: ['blur', 'input'] }
  ],
  categoryId: [
    { required: true, message: t('admin.challenges.rules_category_required') }
  ],
  type: [
    { required: true, message: t('admin.challenges.rules_type_required'), trigger: 'change' }
  ]
}))

const typeOptions = computed(() => [
  { label: t('admin.challenges.type_static_attachment'), value: 'STATIC' },
  { label: t('admin.challenges.type_dynamic_container'), value: 'DYNAMIC' }
])

const difficultyOptions = computed(() => [
  { label: t('admin.challenges.diff_easy'), value: 1 },
  { label: t('admin.challenges.diff_normal'), value: 2 },
  { label: t('admin.challenges.diff_mid'), value: 3 },
  { label: t('admin.challenges.diff_hard'), value: 4 },
  { label: t('admin.challenges.diff_difficult'), value: 5 }
])

function parseDifficultyToNumber(diff?: ChallengeDifficulty | number | string): number {
  if (typeof diff === 'number') return diff
  if (!diff) return 1
  switch (String(diff).toLowerCase().trim()) {
    case '1':
    case 'easy':
    case '入门':
      return 1
    case '2':
    case 'normal':
    case '简单':
      return 2
    case '3':
    case 'mid':
    case 'medium':
    case '中等':
      return 3
    case '4':
    case 'hard':
    case '进阶':
      return 4
    case '5':
    case 'difficult':
    case 'hardcore':
    case '困难':
      return 5
    default:
      return 1
  }
}

function formatFileSize(bytes?: number): string {
  if (!bytes || bytes <= 0) return ''
  if (bytes < 1024) return `${bytes} B`
  const kb = bytes / 1024
  if (kb < 1024) return `${kb.toFixed(1)} KB`
  const mb = kb / 1024
  if (mb < 1024) return `${mb.toFixed(1)} MB`
  const gb = mb / 1024
  return `${gb.toFixed(2)} GB`
}

const categoryFormOptions = computed(() => {
  return categoryList.value.map(cat => ({
    label: cat.name,
    value: cat.id,
    icon: cat.icon
  }))
})

function renderCategoryOptionLabel(option: { label?: string; icon?: string; [key: string]: unknown }) {
  return h(
    'div',
    {
      class: 'category-option-item',
      style: {
        display: 'inline-flex',
        alignItems: 'center',
        gap: '8px',
        lineHeight: '1',
        height: '100%',
        verticalAlign: 'middle'
      }
    },
    [
      option.icon
        ? h(VolurIcon, {
            name: option.icon as string,
            size: 16,
            style: { display: 'inline-flex', alignItems: 'center', flexShrink: 0 }
          })
        : null,
      h(
        'span',
        {
          class: 'category-option-text',
          style: {
            lineHeight: '1',
            display: 'inline-flex',
            alignItems: 'center'
          }
        },
        (option.label as string) || ''
      )
    ]
  )
}

async function fetchCategories() {
  try {
    const res = await getAdminCtfCategoriesApi()
    if (res.success && res.data) {
      categoryList.value = res.data
      if (!isEditMode.value && !formData.categoryId && res.data.length > 0) {
        formData.categoryId = res.data[0].id
      }
    }
  } catch (err) {
    console.error('Failed to load categories:', err)
  }
}

async function fetchChallengeDetail(id: number | string) {
  loadingDetail.value = true
  try {
    const res = await getAdminCtfChallengeDetailApi(id)
    if (res.success && res.data) {
      const d = res.data
      formData.name = d.name
      formData.categoryId = d.categoryId
      formData.type = (d.type === 'CONTAINER' ? 'DYNAMIC' : d.type) || 'STATIC'
      formData.difficulty = parseDifficultyToNumber(d.difficulty)
      formData.flag = d.flag || ''
      formData.description = d.description || ''
      formData.imageName = d.imageName || ''
      formData.containerPort = d.containerPort || 80
      formData.cpuLimit = d.cpuLimit || 1000
      formData.memoryLimit = d.memoryLimit || 512
      editingChallengeName.value = d.name

      // 解析赛题附件列表：优先使用 attachments 数组，兼容历史单一 attachmentUrl
      if (d.attachments && Array.isArray(d.attachments) && d.attachments.length > 0) {
        formData.attachments = d.attachments.map(att => ({
          name: att.name,
          url: att.url,
          size: att.size
        }))
      } else if (d.attachmentUrl) {
        formData.attachments = [
          {
            name: d.attachmentName || t('admin.challenges.default_attachment_name'),
            url: d.attachmentUrl
          }
        ]
      } else {
        formData.attachments = []
      }
    }
  } catch (err) {
    console.error('Failed to load challenge detail:', err)
    message.error(t('admin.challenges.load_detail_failed'))
  } finally {
    loadingDetail.value = false
  }
}

function handleBack() {
  if (props.isEmbedded) {
    emit('cancel')
  } else {
    router.push('/admin/challenges/ctf')
  }
}

async function handleSaveChallenge() {
  formRef.value?.validate(async (errors) => {
    if (errors) return
    saving.value = true
    try {
      const primaryAttachment = formData.attachments.length > 0 ? formData.attachments[0] : null
      const payload: CtfChallengeCreateRequestDTO = {
        name: formData.name.trim(),
        description: formData.description,
        categoryId: formData.categoryId!,
        type: formData.type,
        difficulty: formData.difficulty,
        flag: formData.flag?.trim() || undefined,
        attachments: formData.attachments.map(att => ({
          name: att.name.trim(),
          url: att.url.trim(),
          size: att.size
        })),
        attachmentUrl: primaryAttachment ? primaryAttachment.url.trim() : undefined,
        attachmentName: primaryAttachment ? primaryAttachment.name.trim() : undefined,
        imageName: formData.imageName?.trim() || undefined,
        containerPort: formData.containerPort || undefined,
        cpuLimit: formData.cpuLimit || undefined,
        memoryLimit: formData.memoryLimit || undefined
      }

      if (isEditMode.value && targetId.value) {
        await updateAdminCtfChallengeApi(targetId.value, payload)
        message.success(t('admin.challenges.update_success'))
        if (props.isEmbedded) {
          emit('saved', targetId.value)
        } else {
          router.push('/admin/challenges/ctf')
        }
      } else {
        const createRes = await createAdminCtfChallengeApi(payload)
        message.success(t('admin.challenges.create_success'))
        const newId = createRes.data || ''
        if (props.isEmbedded) {
          emit('saved', newId)
        } else {
          router.push('/admin/challenges/ctf')
        }
      }
    } catch (err: unknown) {
      console.error('Failed to save challenge:', err)
      const errorMsg = err instanceof Error ? err.message : t('admin.challenges.save_failed')
      message.error(errorMsg)
    } finally {
      saving.value = false
    }
  })
}

async function handleAttachmentUpload(options: UploadCustomRequestOptions) {
  const file = options.file.file
  if (!file) return
  uploadingAttachment.value = true
  try {
    const uploadForm = new FormData()
    uploadForm.append('file', file)
    const res = await uploadAdminCtfAttachmentApi(uploadForm)
    if (res.success && res.data) {
      const att = res.data
      formData.attachments.push({
        name: att.name || file.name,
        url: att.url,
        size: att.size || file.size
      })
      message.success(t('admin.challenges.attachment_uploaded', { name: att.name || file.name }))
      options.onFinish()
    } else {
      message.error(res.message || t('admin.challenges.upload_attachment_failed'))
      options.onError()
    }
  } catch (err: unknown) {
    console.error('Attachment upload failed:', err)
    const msg = err instanceof Error ? err.message : t('admin.challenges.attachment_upload_failed')
    message.error(msg)
    options.onError()
  } finally {
    uploadingAttachment.value = false
  }
}

function handleAddExternalAttachment() {
  const url = externalAttachment.url.trim()
  if (!url) {
    message.warning(t('admin.challenges.input_attachment_url_tip'))
    return
  }
  let name = externalAttachment.name.trim()
  if (!name) {
    try {
      const parsedUrl = new URL(url)
      const pathname = parsedUrl.pathname
      const lastSegment = pathname.substring(pathname.lastIndexOf('/') + 1)
      name = decodeURIComponent(lastSegment) || t('admin.challenges.default_external_name')
    } catch {
      name = t('admin.challenges.default_external_name')
    }
  }

  formData.attachments.push({
    name,
    url
  })

  externalAttachment.name = ''
  externalAttachment.url = ''
  message.success(t('admin.challenges.attachment_added', { name }))
}

function removeAttachment(index: number) {
  formData.attachments.splice(index, 1)
}

onMounted(async () => {
  await fetchCategories()
  if (targetId.value) {
    await fetchChallengeDetail(targetId.value)
  }
})
</script>

<style scoped lang="scss">
.admin-ctf-challenge-edit-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding-bottom: 40px;

  .subview-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 18px;
    border-bottom: 1px solid var(--v-border-base, #e5e4e7);

    .header-left {
      display: flex;
      flex-direction: column;
      gap: 8px;

      .back-btn {
        align-self: flex-start;
        padding: 0;
        color: #444444;
        background-color: transparent !important;
        background: transparent !important;
        transition: color 0.15s ease;

        &:hover,
        &:focus,
        &:active {
          color: #111111 !important;
          background-color: transparent !important;
          background: transparent !important;

          :deep(.n-icon) {
            color: #111111 !important;
          }
        }

        :deep(.n-button__state-border) {
          display: none !important;
        }
      }

      .title-wrap {
        display: flex;
        align-items: center;
        gap: 12px;
        flex-wrap: wrap;

        .subview-title {
          font-size: 20px;
          font-weight: 700;
          color: var(--v-text-primary, #111111);
          margin: 0;
        }
      }
    }

    .header-right {
      display: flex;
      align-items: center;
      gap: 12px;
    }
  }

  .challenge-edit-form {
    display: flex;
    flex-direction: column;
    gap: 16px;

    .form-section-card {
      border-radius: 8px;
      background: var(--v-bg-card, #ffffff);
      border: 1px solid var(--v-border-base, #e5e4e7);

      :deep(.n-card-header) {
        font-size: 15px;
        font-weight: 600;
        padding-bottom: 12px;
        border-bottom: 1px solid var(--v-border-base, #f0f0f2);
        margin-bottom: 16px;
      }
    }

    .form-row-2 {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 20px;

      @media (max-width: 768px) {
        grid-template-columns: 1fr;
        gap: 0;
      }
    }

    .form-row-3 {
      display: grid;
      grid-template-columns: 1fr 1fr 1fr;
      gap: 16px;

      @media (max-width: 768px) {
        grid-template-columns: 1fr;
        gap: 0;
      }
    }

    .section-badge {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      font-size: 12px;
      color: #166534;
      background: #f0fdf4;
      padding: 2px 8px;
      border-radius: 4px;
      border: 1px solid #bbf7d0;
    }

    .card-header-with-badge {
      display: inline-flex;
      align-items: center;
      gap: 10px;
    }

    .attachments-container {
      display: flex;
      flex-direction: column;
      gap: 16px;

      .attachment-actions-bar {
        display: flex;
        align-items: center;
        gap: 14px;
        flex-wrap: wrap;

        .external-link-row {
          display: flex;
          align-items: center;
          gap: 10px;
          flex: 1;
          min-width: 320px;
        }
      }

      .attachments-list {
        display: flex;
        flex-direction: column;
        gap: 8px;

        .attachment-item {
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: 10px 14px;
          background: var(--v-bg-subtle, #f9fafb);
          border: 1px solid var(--v-border-base, #e5e4e7);
          border-radius: 6px;
          transition: all 0.2s ease;

          &:hover {
            border-color: #cbd5e1;
            background: rgba(0, 0, 0, 0.02);
          }

          .attachment-main {
            display: flex;
            align-items: center;
            gap: 12px;
            min-width: 0;
            flex: 1;

            .attachment-icon-box {
              display: flex;
              align-items: center;
              justify-content: center;
              width: 36px;
              height: 36px;
              border-radius: 6px;
              background: rgba(0, 0, 0, 0.05);
              color: #444444;
              flex-shrink: 0;
            }

            .attachment-details {
              display: flex;
              flex-direction: column;
              gap: 3px;
              min-width: 0;
              flex: 1;

              .attachment-title-row {
                display: flex;
                align-items: center;
                gap: 8px;

                .attachment-name {
                  font-size: 14px;
                  font-weight: 600;
                  color: var(--v-text-primary, #111111);
                  overflow: hidden;
                  text-overflow: ellipsis;
                  white-space: nowrap;
                }

                .attachment-size {
                  font-size: 11px;
                  color: #666666;
                  background: rgba(0, 0, 0, 0.05);
                  padding: 1px 6px;
                  border-radius: 4px;
                }
              }

              .attachment-url {
                font-size: 12px;
                color: #666666;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
              }
            }
          }

          .attachment-actions {
            display: flex;
            align-items: center;
            gap: 6px;
            flex-shrink: 0;
            margin-left: 12px;
          }
        }
      }

      .attachments-empty {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 10px;
        padding: 24px 16px;
        border: 1px dashed var(--v-border-base, #e5e4e7);
        border-radius: 6px;
        background: var(--v-bg-subtle, #fafafa);
        color: #666666;
        font-size: 13px;

        .empty-icon {
          color: #999999;
          flex-shrink: 0;
        }

        .empty-text {
          color: #666666;
        }
      }
    }

    .bottom-action-bar {
      display: flex;
      justify-content: flex-end;
      align-items: center;
      gap: 14px;
      padding: 16px 20px;
      background: var(--v-bg-card, #ffffff);
      border: 1px solid var(--v-border-base, #e5e4e7);
      border-radius: 8px;
      margin-top: 4px;
    }
  }

  .theme-primary-btn {
    background-color: #000000 !important;
    color: #ffffff !important;
    border: none !important;

    &:hover {
      background-color: #333333 !important;
    }
  }
}
</style>
