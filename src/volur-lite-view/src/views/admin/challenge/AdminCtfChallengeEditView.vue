<template>
  <div class="admin-ctf-challenge-edit-view">
    <!-- Subview Top Header with Back action & Breadcrumbs -->
    <div class="subview-header">
      <div class="header-left">
        <n-button quaternary size="small" class="back-btn" @click="handleBack">
          <template #icon>
            <n-icon :component="ArrowBackOutline" />
          </template>
          返回 CTF 题库
        </n-button>
        <div class="title-wrap">
          <h2 class="subview-title">
            {{ isEditMode ? '编辑 CTF 赛题' : '新增 CTF 赛题' }}
          </h2>
          <span v-if="isEditMode && editingChallengeName" class="challenge-title-badge">
            {{ editingChallengeName }}
          </span>
          <n-tag
            v-if="formData.type"
            size="small"
            :type="getTypeTagType(formData.type)"
            round
          >
            {{ formData.type }}
          </n-tag>
        </div>
      </div>

      <div class="header-right">
        <n-button secondary @click="handleBack">取消</n-button>
        <n-button
          type="primary"
          class="theme-primary-btn"
          :loading="saving"
          @click="handleSaveChallenge"
        >
          <template #icon>
            <n-icon :component="SaveOutline" />
          </template>
          {{ isEditMode ? '保存赛题修改' : '确认创建赛题' }}
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
        <n-card class="form-section-card" :bordered="false" title="基本信息">
          <n-form-item label="题目名称" path="name">
            <n-input
              v-model:value="formData.name"
              placeholder="请输入题目名称 (例: Easy Web, Pwn_fmt_01)"
              maxlength="100"
              show-count
            />
          </n-form-item>

          <div class="form-row-2">
            <n-form-item label="所属方向" path="categoryId">
              <n-select
                v-model:value="formData.categoryId"
                :options="categoryFormOptions"
                :render-label="renderCategoryOptionLabel"
                placeholder="选择题目分类方向"
              />
            </n-form-item>

            <n-form-item label="赛题模式" path="type">
              <n-select
                v-model:value="formData.type"
                :options="typeOptions"
                placeholder="选择赛题类型"
              />
            </n-form-item>
          </div>

          <div class="form-row-2">
            <n-form-item label="难度等级" path="difficulty">
              <div style="display: flex; align-items: center; gap: 10px;">
                <n-rate v-model:value="formData.difficulty" :count="5" />
                <n-tag size="small" :type="getDifficultyBadge(formData.difficulty).type" :bordered="false">
                  {{ getDifficultyBadge(formData.difficulty).label }}
                </n-tag>
              </div>
            </n-form-item>

            <n-form-item label="静态 Flag" path="flag">
              <n-input
                v-model:value="formData.flag"
                placeholder="flag{your_flag_here} (动态靶机可留空)"
              />
            </n-form-item>
          </div>
        </n-card>

        <!-- 2. 题目描述与指引 Card -->
        <n-card class="form-section-card" :bordered="false" title="题目说明与指引">
          <n-form-item label="题目描述" path="description">
            <n-input
              v-model:value="formData.description"
              type="textarea"
              :autosize="{ minRows: 5, maxRows: 14 }"
              placeholder="支持 Markdown 格式题目详细说明、解题提示与背景故事..."
            />
          </n-form-item>
        </n-card>

        <!-- 3. 赛题附件 Card -->
        <n-card class="form-section-card" :bordered="false" title="赛题附件配置">
          <n-form-item label="赛题附件">
            <div class="attachment-box">
              <div v-if="formData.attachmentUrl" class="attachment-preview">
                <n-tag type="info" closable @close="clearAttachment">
                  <template #icon>
                    <n-icon :component="DocumentTextOutline" />
                  </template>
                  {{ formData.attachmentName || '附件文件' }}
                </n-tag>
                <a :href="formData.attachmentUrl" target="_blank" class="attachment-link">查看/下载</a>
              </div>

              <div class="attachment-actions">
                <n-upload
                  :show-file-list="false"
                  :custom-request="handleAttachmentUpload"
                >
                  <n-button size="small" secondary :loading="uploadingAttachment">
                    <template #icon>
                      <n-icon :component="CloudUploadOutline" />
                    </template>
                    {{ formData.attachmentUrl ? '更换附件' : '上传附件' }}
                  </n-button>
                </n-upload>

                <n-input
                  v-model:value="formData.attachmentUrl"
                  size="small"
                  placeholder="或直接填入附件外部下载 URL"
                  style="flex: 1;"
                />
              </div>
            </div>
          </n-form-item>
        </n-card>

        <!-- 4. 容器靶机配置 Card (仅当容器或动态模式时展开) -->
        <n-card
          v-if="formData.type === 'CONTAINER' || formData.type === 'DYNAMIC'"
          class="form-section-card container-config-card"
          :bordered="false"
          title="动态靶机容器配置"
        >
          <template #header-extra>
            <div class="section-badge">
              <n-icon :component="CubeOutline" />
              <span>动态实例下发</span>
            </div>
          </template>

          <n-alert type="info" :show-icon="false" style="margin-bottom: 16px;">
            系统将基于此镜像按需为选手下发独立的动态靶机实例；镜像若在当前集群节点中未就绪，保存后将自动提交异步镜像拉取任务。
          </n-alert>

          <n-form-item label="镜像全称" path="imageName">
            <n-input
              v-model:value="formData.imageName"
              placeholder="例如: wzbcctf2026/cve-0114-0514:latest 或 ctf_web_sqli:v1"
            />
          </n-form-item>

          <div class="form-row-3">
            <n-form-item label="开放端口" path="containerPort">
              <n-input-number
                v-model:value="formData.containerPort"
                placeholder="80"
                :min="1"
                :max="65535"
                style="width: 100%;"
              />
            </n-form-item>

            <n-form-item label="CPU配额(m)" path="cpuLimit">
              <n-input-number
                v-model:value="formData.cpuLimit"
                placeholder="1000"
                :min="100"
                :step="100"
                style="width: 100%;"
              />
            </n-form-item>

            <n-form-item label="内存配额(MB)" path="memoryLimit">
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
          <n-button secondary size="medium" @click="handleBack">取消并返回</n-button>
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
            {{ isEditMode ? '保存赛题修改' : '确认创建赛题' }}
          </n-button>
        </div>
      </n-form>
    </n-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, h } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  NCard,
  NForm,
  NFormItem,
  NInput,
  NInputNumber,
  NSelect,
  NRate,
  NTag,
  NButton,
  NIcon,
  NAlert,
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
  CubeOutline
} from '@vicons/ionicons5'
import {
  getAdminCtfChallengeDetailApi,
  createAdminCtfChallengeApi,
  updateAdminCtfChallengeApi,
  getAdminCtfCategoriesApi
} from '@/api/challenge'
import { uploadResourceApi } from '@/api/system'
import type {
  CtfCategoryVO,
  CtfChallengeCreateRequestDTO,
  ChallengeType,
  ChallengeDifficulty
} from '@/types/challenge'
import { VolurIcon } from '@/public/volurEx'

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

const targetId = computed<string | number | undefined>(() => {
  return props.challengeId || (route.params.id as string | undefined)
})

const isEditMode = computed<boolean>(() => Boolean(targetId.value))

const loadingDetail = ref(false)
const saving = ref(false)
const uploadingAttachment = ref(false)
const formRef = ref<FormInst | null>(null)
const editingChallengeName = ref('')
const categoryList = ref<CtfCategoryVO[]>([])

const formData = reactive<{
  name: string
  categoryId: number | string | null
  type: ChallengeType
  difficulty: number
  flag: string
  description: string
  attachmentUrl: string
  attachmentName: string
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
  attachmentUrl: '',
  attachmentName: '',
  imageName: '',
  containerPort: 80,
  cpuLimit: 1000,
  memoryLimit: 512
})

const formRules: FormRules = {
  name: [
    { required: true, message: '题目名称不能为空', trigger: ['blur', 'input'] }
  ],
  categoryId: [
    { required: true, message: '分类方向不能为空' }
  ],
  type: [
    { required: true, message: '题目模式不能为空', trigger: 'change' }
  ]
}

const typeOptions = [
  { label: '静态题 (STATIC)', value: 'STATIC' },
  { label: '动态Flag (DYNAMIC)', value: 'DYNAMIC' },
  { label: '动态容器/靶机 (CONTAINER)', value: 'CONTAINER' }
]

/**
 * 难度等级映射 (对齐后端 CtfChallenge 与 challenge.json)
 * 1 -> 入门 (easy)
 * 2 -> 简单 (normal)
 * 3 -> 中等 (mid)
 * 4 -> 进阶 (hard)
 * 5 -> 困难 (difficult)
 */
function getDifficultyBadge(diffVal?: ChallengeDifficulty | number | string) {
  const num = parseDifficultyToNumber(diffVal)
  const map: Record<number, { label: string; type: 'default' | 'info' | 'success' | 'warning' | 'error' }> = {
    1: { label: '入门 (easy)', type: 'info' },
    2: { label: '简单 (normal)', type: 'success' },
    3: { label: '中等 (mid)', type: 'warning' },
    4: { label: '进阶 (hard)', type: 'error' },
    5: { label: '困难 (difficult)', type: 'error' }
  }
  return map[num] || { label: `Lv.${num}`, type: 'default' }
}

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

function getTypeTagType(type: ChallengeType): 'default' | 'primary' | 'info' | 'success' | 'warning' | 'error' {
  switch (type) {
    case 'CONTAINER': return 'info'
    case 'DYNAMIC': return 'warning'
    case 'STATIC': return 'default'
    default: return 'default'
  }
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
      formData.type = d.type
      formData.difficulty = parseDifficultyToNumber(d.difficulty)
      formData.flag = d.flag || ''
      formData.description = d.description || ''
      formData.attachmentUrl = d.attachmentUrl || ''
      formData.attachmentName = d.attachmentName || ''
      formData.imageName = d.imageName || ''
      formData.containerPort = d.containerPort || 80
      formData.cpuLimit = d.cpuLimit || 1000
      formData.memoryLimit = d.memoryLimit || 512
      editingChallengeName.value = d.name
    }
  } catch (err) {
    console.error('Failed to load challenge detail:', err)
    message.error('加载赛题详情失败')
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
      const payload: CtfChallengeCreateRequestDTO = {
        name: formData.name.trim(),
        description: formData.description,
        categoryId: formData.categoryId!,
        type: formData.type,
        difficulty: formData.difficulty,
        flag: formData.flag?.trim() || undefined,
        attachmentUrl: formData.attachmentUrl?.trim() || undefined,
        attachmentName: formData.attachmentName?.trim() || undefined,
        imageName: formData.imageName?.trim() || undefined,
        containerPort: formData.containerPort || undefined,
        cpuLimit: formData.cpuLimit || undefined,
        memoryLimit: formData.memoryLimit || undefined
      }

      if (isEditMode.value && targetId.value) {
        await updateAdminCtfChallengeApi(targetId.value, payload)
        message.success('赛题更新成功')
        if (props.isEmbedded) {
          emit('saved', targetId.value)
        } else {
          router.push('/admin/challenges/ctf')
        }
      } else {
        const createRes = await createAdminCtfChallengeApi(payload)
        message.success('赛题创建成功')
        const newId = createRes.data || ''
        if (props.isEmbedded) {
          emit('saved', newId)
        } else {
          router.push('/admin/challenges/ctf')
        }
      }
    } catch (err: unknown) {
      console.error('Failed to save challenge:', err)
      const errorMsg = err instanceof Error ? err.message : '保存赛题失败'
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
    const res = await uploadResourceApi(uploadForm, 'attachments')
    if (res.success && res.data) {
      formData.attachmentUrl = res.data.filePath
      formData.attachmentName = res.data.fileName
      message.success('附件上传成功')
    }
  } catch (err) {
    console.error('Attachment upload failed:', err)
    message.error('附件上传失败')
  } finally {
    uploadingAttachment.value = false
  }
}

function clearAttachment() {
  formData.attachmentUrl = ''
  formData.attachmentName = ''
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
      gap: 6px;

      .back-btn {
        align-self: flex-start;
        padding-left: 0;
        color: #444444;

        &:hover {
          color: var(--v-color-primary, #000000);
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

        .challenge-title-badge {
          font-size: 14px;
          font-weight: 500;
          color: #666666;
          background: rgba(0, 0, 0, 0.05);
          padding: 2px 10px;
          border-radius: 6px;
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

    .attachment-box {
      display: flex;
      flex-direction: column;
      gap: 8px;
      width: 100%;

      .attachment-preview {
        display: flex;
        align-items: center;
        gap: 12px;

        .attachment-link {
          font-size: 13px;
          color: #2563eb;
          text-decoration: none;

          &:hover {
            text-decoration: underline;
          }
        }
      }

      .attachment-actions {
        display: flex;
        align-items: center;
        gap: 12px;
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
