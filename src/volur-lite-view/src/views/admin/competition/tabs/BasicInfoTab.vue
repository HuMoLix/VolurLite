<template>
  <div class="basic-info-tab">
    <!-- Top Action Bar for Basic Info Tab -->
    <div class="tab-top-actions">
      <div class="actions-left">
        <span class="tab-subtitle">基础配置与赛程周期</span>
      </div>
      <div class="actions-right">
        <n-button
          v-if="isEdit && compId"
          quaternary
          @click="goToPreview"
        >
          <template #icon>
            <n-icon :component="EyeOutline" />
          </template>
          {{ t('admin.competitions.modal_preview_btn') }}
        </n-button>

        <n-popconfirm
          v-if="isEdit && compId && isArchived"
          @positive-click="handleUnarchive"
        >
          <template #trigger>
            <n-button :loading="archiveLoading" secondary type="warning">
              <template #icon>
                <n-icon :component="ArchiveOutline" />
              </template>
              {{ t('admin.competitions.action_unarchive') }}
            </n-button>
          </template>
          {{ t('admin.competitions.unarchive_confirm') }}
        </n-popconfirm>

        <n-popconfirm
          v-if="isEdit && compId && !isArchived"
          @positive-click="handleArchive"
        >
          <template #trigger>
            <n-button :loading="archiveLoading" quaternary>
              <template #icon>
                <n-icon :component="ArchiveOutline" />
              </template>
              {{ t('admin.competitions.action_archive') }}
            </n-button>
          </template>
          {{ t('admin.competitions.archive_confirm') }}
        </n-popconfirm>

        <n-button
          class="theme-primary-btn"
          :loading="submitLoading"
          @click="handleSubmit"
        >
          <template #icon>
            <n-icon :component="SaveOutline" />
          </template>
          保存基础信息
        </n-button>
      </div>
    </div>

    <!-- Form Section -->
    <n-form
      ref="formRef"
      :model="formModel"
      :rules="formRules"
      label-placement="top"
      require-mark-placement="right-hanging"
    >
      <n-grid :x-gap="24" :y-gap="16" cols="1 s:2 m:2 l:2" responsive="screen">
        <n-grid-item :span="1">
          <n-form-item :label="t('admin.competitions.form_title')" path="title">
            <n-input
              v-model:value="formModel.title"
              :placeholder="t('admin.competitions.form_title_placeholder')"
              maxlength="128"
              show-count
            />
          </n-form-item>
        </n-grid-item>

        <n-grid-item :span="1">
          <n-form-item :label="t('admin.competitions.form_slug')" path="slug">
            <n-input
              v-model:value="formModel.slug"
              :placeholder="t('admin.competitions.form_slug_placeholder')"
              maxlength="64"
              show-count
            />
          </n-form-item>
        </n-grid-item>

        <n-grid-item :span="1">
          <n-form-item :label="t('admin.competitions.form_type')" path="type">
            <n-select
              v-model:value="formModel.type"
              :options="typeSelectOptions"
              :placeholder="t('admin.competitions.form_type_tip')"
              :disabled="isEdit"
            />
          </n-form-item>
        </n-grid-item>

        <n-grid-item :span="1">
          <n-form-item :label="t('admin.competitions.form_status')" path="status">
            <template v-if="!isEdit">
              <div class="status-readonly-wrap">
                <n-tag type="default" size="medium" :bordered="false">
                  {{ t('competition.status_draft') }}
                </n-tag>
                <div class="status-tip-text">
                  新建竞赛初始状态固定为草稿，创建成功后可在管理界面发布
                </div>
              </div>
            </template>
            <template v-else-if="isArchived">
              <div class="status-readonly-wrap">
                <n-tag type="default" size="medium" :bordered="false">
                  {{ t('competition.status_archived') }}
                </n-tag>
                <div class="status-tip-text">
                  竞赛已归档封存，不可直接修改状态。如需修改请点击上方“取消归档”
                </div>
              </div>
            </template>
            <template v-else>
              <n-select
                v-model:value="formModel.status"
                :options="statusSelectOptions"
              />
            </template>
          </n-form-item>
        </n-grid-item>

        <n-grid-item :span="1">
          <n-form-item :label="t('admin.competitions.form_time_range')" required>
            <n-date-picker
              v-model:value="compTimeRange"
              type="datetimerange"
              clearable
              format="yyyy-MM-dd HH:mm:ss"
              :is-date-disabled="isCompDateDisabled"
              style="width: 100%;"
            />
          </n-form-item>
        </n-grid-item>

        <n-grid-item :span="1">
          <n-form-item :label="t('admin.competitions.form_reg_time_range')">
            <n-date-picker
              v-model:value="regTimeRange"
              type="datetimerange"
              clearable
              format="yyyy-MM-dd HH:mm:ss"
              :is-date-disabled="isRegDateDisabled"
              style="width: 100%;"
            />
          </n-form-item>
        </n-grid-item>

        <!-- Banner Upload -->
        <n-grid-item :span="2">
          <n-form-item :label="t('admin.competitions.form_banner')">
            <div class="banner-uploader-container">
              <input
                ref="bannerFileInputRef"
                type="file"
                accept="image/png,image/jpeg,image/jpg,image/webp"
                style="display: none;"
                @change="handleBannerFileChange"
              />

              <div
                v-if="formModel.bannerUrl"
                class="banner-preview-box"
              >
                <div class="banner-aspect-ratio-box">
                  <img
                    :src="formatBannerUrl(formModel.bannerUrl)"
                    alt="Banner Preview"
                    class="banner-img"
                  />
                  <div class="banner-overlay-actions">
                    <n-button
                      size="small"
                      class="banner-change-btn"
                      :loading="bannerUploading"
                      @click="triggerBannerSelect"
                    >
                      <template #icon>
                        <n-icon :component="CloudUploadOutline" />
                      </template>
                      {{ t('admin.competitions.banner_change_btn') }}
                    </n-button>
                    <n-button
                      size="small"
                      class="banner-remove-btn"
                      @click="handleRemoveBanner"
                    >
                      <template #icon>
                        <n-icon :component="TrashOutline" />
                      </template>
                      {{ t('admin.competitions.banner_remove_btn') }}
                    </n-button>
                  </div>
                </div>
              </div>

              <div
                v-else
                class="banner-drop-zone"
                @click="triggerBannerSelect"
              >
                <n-spin :show="bannerUploading">
                  <div class="drop-content">
                    <n-icon size="40" class="upload-icon">
                      <CloudUploadOutline />
                    </n-icon>
                    <p class="drop-text">{{ t('admin.competitions.banner_drop_hint') }}</p>
                    <p class="drop-hint">{{ t('admin.competitions.banner_format_hint') }}</p>
                    <n-button size="small" class="theme-primary-btn select-btn">
                      {{ t('admin.competitions.banner_upload_btn') }}
                    </n-button>
                  </div>
                </n-spin>
              </div>
            </div>
          </n-form-item>
        </n-grid-item>

        <!-- Markdown Description -->
        <n-grid-item :span="2">
          <n-form-item :label="t('admin.competitions.form_desc')">
            <VolurMarkdownEditor
              v-model:value="formModel.description"
              :placeholder="t('admin.competitions.form_desc_placeholder')"
              :rows="8"
            />
          </n-form-item>
        </n-grid-item>
      </n-grid>

      <!-- Subcard: 队伍人数上限与计分板封榜规则 -->
      <n-card class="sub-config-card" title="队伍人数限制与封榜规则" :bordered="false">
        <n-grid :x-gap="24" :y-gap="16" cols="1 s:2 m:5" responsive="screen">
          <n-grid-item>
            <n-form-item :label="t('admin.competitions.form_min_team_members')">
              <n-input-number
                v-model:value="formConfig.minTeamMembers"
                :min="1"
                :max="20"
                style="width: 100%;"
              />
            </n-form-item>
          </n-grid-item>

          <n-grid-item>
            <n-form-item :label="t('admin.competitions.form_max_team_members')">
              <n-input-number
                v-model:value="formConfig.maxTeamMembers"
                :min="1"
                :max="20"
                style="width: 100%;"
              />
            </n-form-item>
          </n-grid-item>

          <n-grid-item>
            <n-form-item :label="t('admin.competitions.form_max_leaders')">
              <n-input-number
                v-model:value="formConfig.maxLeaders"
                :min="0"
                :max="10"
                style="width: 100%;"
              />
            </n-form-item>
          </n-grid-item>

          <n-grid-item>
            <n-form-item :label="t('admin.competitions.form_max_teams')">
              <n-input-number
                v-model:value="formConfig.maxTeams"
                :min="1"
                :max="5000"
                style="width: 100%;"
              />
            </n-form-item>
          </n-grid-item>

          <n-grid-item>
            <n-form-item :label="t('admin.competitions.form_freeze_minutes')">
              <n-input-number
                v-model:value="formConfig.freezeScoreboardMinutes"
                :min="0"
                :max="1440"
                style="width: 100%;"
              />
            </n-form-item>
          </n-grid-item>
        </n-grid>

        <n-form-item label="自定义报名要求与须知" style="margin-top: 16px;">
          <n-input
            v-model:value="formConfig.regRequirements"
            type="textarea"
            :rows="3"
            placeholder="例如：此次竞赛仅允许长三角地区高校战队报名，线下总决赛需出示学生证等（非系统自动校验的补充说明）"
          />
        </n-form-item>
      </n-card>
    </n-form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  NCard,
  NInput,
  NSelect,
  NButton,
  NTag,
  NPopconfirm,
  NForm,
  NFormItem,
  NInputNumber,
  NDatePicker,
  NGrid,
  NGridItem,
  NIcon,
  NSpin,
  useMessage,
  type FormInst,
  type FormRules
} from 'naive-ui'
import {
  SaveOutline,
  CloudUploadOutline,
  EyeOutline,
  ArchiveOutline,
  TrashOutline
} from '@vicons/ionicons5'
import { useI18n } from 'vue-i18n'
import type {
  CompetitionType,
  CompetitionStatus,
  CompetitionCreateRequestDTO,
  CompetitionUpdateRequestDTO,
  CompetitionConfigDTO,
  AdminCompetitionDetailVO
} from '@/types/competition'
import {
  createCompetitionApi,
  updateCompetitionApi,
  archiveCompetitionApi,
  unarchiveCompetitionApi
} from '@/api/competition'
import { uploadImageApi } from '@/api/system'
import { formatBannerUrl, setMemoryImageCache } from '@/utils/imageCache'
import { VolurMarkdownEditor } from '@/public/volurEx'

interface Props {
  compId?: string | number
  isEdit: boolean
  initialData: AdminCompetitionDetailVO | null
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'saved', id: string | number): void
  (e: 'refresh'): void
}>()

const router = useRouter()
const { t } = useI18n()
const message = useMessage()

// State
const submitLoading = ref<boolean>(false)
const formRef = ref<FormInst | null>(null)
const bannerFileInputRef = ref<HTMLInputElement | null>(null)
const bannerUploading = ref<boolean>(false)

// Date Pickers
const compTimeRange = ref<[number, number] | null>(null)
const regTimeRange = ref<[number, number] | null>(null)

// Form Model
const formModel = reactive<{
  title: string
  slug: string
  type: CompetitionType
  status: CompetitionStatus
  description: string
  bannerUrl: string
}>({
  title: '',
  slug: '',
  type: 'CTF',
  status: 'DRAFT',
  description: '',
  bannerUrl: ''
})

// Config Model
const formConfig = reactive<CompetitionConfigDTO>({
  minTeamMembers: 1,
  maxTeamMembers: 4,
  maxLeaders: 1,
  maxTeams: 500,
  requireTeamAudit: false,
  freezeScoreboardMinutes: 0,
  allowWriteupSubmission: true,
  regRequirements: ''
})

// Archive State & Actions
const archiveLoading = ref<boolean>(false)
const isArchived = computed(() => props.initialData?.status === 'ARCHIVED')

// Select Options
const typeSelectOptions = [
  { label: 'CTF', value: 'CTF' },
  { label: 'AWD', value: 'AWD' },
  { label: 'AWD+', value: 'AWDP' },
  { label: 'ISW', value: 'ISW' },
  { label: 'THEORY', value: 'THEORY' }
]

const statusSelectOptions = computed(() => [
  { label: t('competition.status_draft'), value: 'DRAFT' },
  { label: t('competition.status_published'), value: 'PUBLISHED' }
])

async function handleArchive() {
  if (!props.compId) return
  try {
    archiveLoading.value = true
    await archiveCompetitionApi(props.compId)
    message.success(t('admin.competitions.archive_success'))
    emit('refresh')
  } catch (error: any) {
    message.error(error.message || '归档失败')
  } finally {
    archiveLoading.value = false
  }
}

async function handleUnarchive() {
  if (!props.compId) return
  try {
    archiveLoading.value = true
    await unarchiveCompetitionApi(props.compId)
    message.success(t('admin.competitions.unarchive_success'))
    emit('refresh')
  } catch (error: any) {
    message.error(error.message || '取消归档失败')
  } finally {
    archiveLoading.value = false
  }
}

const formRules: FormRules = {
  title: [
    { required: true, message: t('admin.competitions.form_title_placeholder'), trigger: 'blur' },
    { max: 128, message: '竞赛标题长度不能超过128个字符', trigger: 'input' }
  ],
  slug: [
    {
      pattern: /^[a-z0-9-]+$/,
      message: 'Slug 仅支持小写字母、数字和连字符 (-)',
      trigger: 'input'
    }
  ],
  type: [{ required: true, message: '请选择赛制类型', trigger: 'change' }],
  status: [{ required: true, message: '请选择竞赛状态', trigger: 'change' }]
}

watch(
  () => props.initialData,
  (d) => {
    if (d) {
      populateData(d)
    }
  },
  { immediate: true }
)

function populateData(d: AdminCompetitionDetailVO) {
  formModel.title = d.title || ''
  formModel.slug = d.slug || ''
  formModel.type = d.type || 'CTF'
  if (d.status === 'DRAFT') {
    formModel.status = 'DRAFT'
  } else if (d.status === 'ARCHIVED') {
    formModel.status = 'ARCHIVED'
  } else {
    // PUBLISHED, RUNNING, ENDED -> publication state is PUBLISHED
    formModel.status = 'PUBLISHED'
  }
  formModel.description = d.description || ''
  formModel.bannerUrl = d.bannerUrl || ''

  if (d.startTime && d.endTime) {
    compTimeRange.value = [
      new Date(d.startTime).getTime(),
      new Date(d.endTime).getTime()
    ]
  } else {
    compTimeRange.value = null
  }

  if (d.regStartTime && d.regEndTime) {
    regTimeRange.value = [
      new Date(d.regStartTime).getTime(),
      new Date(d.regEndTime).getTime()
    ]
  } else {
    regTimeRange.value = null
  }

  const cfg = d.config || d
  formConfig.minTeamMembers = cfg.minTeamMembers ?? 1
  formConfig.maxTeamMembers = cfg.maxTeamMembers ?? 4
  formConfig.maxLeaders = cfg.maxLeaders ?? 1
  formConfig.maxTeams = cfg.maxTeams ?? 500
  formConfig.requireTeamAudit = cfg.requireTeamAudit ?? false
  formConfig.freezeScoreboardMinutes = cfg.freezeScoreboardMinutes ?? 0
  formConfig.allowWriteupSubmission = cfg.allowWriteupSubmission ?? true
  formConfig.maxContainersPerTeam = cfg.maxContainersPerTeam ?? 2
  formConfig.writeupTemplateUrl = cfg.writeupTemplateUrl
  formConfig.writeupTemplateName = cfg.writeupTemplateName
  formConfig.writeupStartTime = cfg.writeupStartTime
  formConfig.writeupEndTime = cfg.writeupEndTime
  formConfig.regRequirements = cfg.regRequirements || ''
}

// Banner Upload Handlers
function triggerBannerSelect() {
  bannerFileInputRef.value?.click()
}

async function handleBannerFileChange(e: Event) {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  if (file.size > 10 * 1024 * 1024) {
    message.error('横幅图片文件大小不能超过 10MB')
    target.value = ''
    return
  }

  try {
    bannerUploading.value = true
    const fd = new FormData()
    fd.append('file', file)
    const res = await uploadImageApi(fd)
    const uploadedPath = res.data?.filePath || ''
    if (uploadedPath) {
      const objectUrl = URL.createObjectURL(file)
      setMemoryImageCache(uploadedPath, objectUrl)
      formModel.bannerUrl = uploadedPath
      message.success(t('admin.competitions.banner_upload_success'))
    } else {
      message.error(t('admin.competitions.banner_upload_failed'))
    }
  } catch (error: any) {
    console.error('Banner upload error:', error)
    message.error(error.message || t('admin.competitions.banner_upload_failed'))
  } finally {
    bannerUploading.value = false
    target.value = ''
  }
}

function handleRemoveBanner() {
  formModel.bannerUrl = ''
}

// Preview Navigation Handler
function goToPreview() {
  if (props.compId) {
    router.push(`/admin/competitions/${props.compId}/preview`)
  }
}

// 约束：比赛起止时间不能是过去的时间（仅针对新建或草稿未开始竞赛）
function isCompDateDisabled(ts: number) {
  if (!props.isEdit || props.initialData?.status === 'DRAFT') {
    const todayStart = new Date().setHours(0, 0, 0, 0)
    return ts < todayStart
  }
  return false
}

// 约束：报名起止时间不能在比赛开始之后进行报名
function isRegDateDisabled(ts: number) {
  if (compTimeRange.value && compTimeRange.value[0]) {
    const compStartDayEnd = new Date(compTimeRange.value[0]).setHours(23, 59, 59, 999)
    if (ts > compStartDayEnd) {
      return true
    }
  }
  if (!props.isEdit || props.initialData?.status === 'DRAFT') {
    const todayStart = new Date().setHours(0, 0, 0, 0)
    if (ts < todayStart) {
      return true
    }
  }
  return false
}

// 动态联动监听
watch(
  () => compTimeRange.value,
  (newVal) => {
    if (newVal && newVal[0] && regTimeRange.value && regTimeRange.value[1]) {
      if (regTimeRange.value[1] > newVal[0]) {
        message.warning('检测到比赛开始时间调整，报名起止时间不能在比赛开始之后进行报名，已对齐报名截止时间')
        regTimeRange.value = [
          Math.min(regTimeRange.value[0], newVal[0]),
          newVal[0]
        ]
      }
    }
  }
)

watch(
  () => regTimeRange.value,
  (newVal) => {
    if (newVal && newVal[1] && compTimeRange.value && compTimeRange.value[0]) {
      if (newVal[1] > compTimeRange.value[0]) {
        message.warning('报名起止时间不能在比赛开始之后进行报名（报名截止时间不能晚于比赛开始时间）')
        regTimeRange.value = [
          Math.min(newVal[0], compTimeRange.value[0]),
          compTimeRange.value[0]
        ]
      }
    }
  }
)

// Submit Logic
async function handleSubmit() {
  if (!formRef.value) return

  await formRef.value.validate()

  if (!compTimeRange.value || compTimeRange.value.length !== 2) {
    message.warning('请配置比赛起止时间')
    return
  }

  const [startMs, endMs] = compTimeRange.value
  const nowMs = Date.now()

  if (startMs >= endMs) {
    message.error('比赛开始时间必须早于结束时间')
    return
  }

  // 比赛起止时间不能是过去的时间（对新建或未开始草稿竞赛校验，保留60秒误差裕度）
  const isNotStarted = !props.isEdit || props.initialData?.status === 'DRAFT'
  if (isNotStarted) {
    if (startMs < nowMs - 60000) {
      message.error('比赛开始时间不能早于当前时间，比赛起止时间不能是过去的时间')
      return
    }
    if (endMs < nowMs) {
      message.error('比赛结束时间不能早于当前时间，比赛起止时间不能是过去的时间')
      return
    }
  }

  // 报名起止时间不能在比赛开始之后进行报名
  if (regTimeRange.value && regTimeRange.value.length === 2) {
    const [regStartMs, regEndMs] = regTimeRange.value
    if (regStartMs >= regEndMs) {
      message.error('报名开始时间必须早于报名截止时间')
      return
    }
    if (regStartMs >= startMs) {
      message.error('报名开始时间必须早于比赛开始时间')
      return
    }
    if (regEndMs > startMs) {
      message.error('报名起止时间不能在比赛开始之后进行报名（报名截止时间必须早于或等于比赛开始时间）')
      return
    }
  }

  submitLoading.value = true
  try {
    const startTimeIso = new Date(compTimeRange.value[0]).toISOString()
    const endTimeIso = new Date(compTimeRange.value[1]).toISOString()

    let regStartIso: string | undefined
    let regEndIso: string | undefined
    if (regTimeRange.value && regTimeRange.value.length === 2) {
      regStartIso = new Date(regTimeRange.value[0]).toISOString()
      regEndIso = new Date(regTimeRange.value[1]).toISOString()
    }

    if (props.isEdit && props.compId) {
      const updateDTO: CompetitionUpdateRequestDTO = {
        title: formModel.title.trim(),
        slug: formModel.slug.trim(),
        type: formModel.type,
        status: formModel.status,
        description: formModel.description,
        bannerUrl: formModel.bannerUrl,
        startTime: startTimeIso,
        endTime: endTimeIso,
        regStartTime: regStartIso,
        regEndTime: regEndIso,
        config: formConfig
      }
      await updateCompetitionApi(props.compId, updateDTO)
      message.success(t('admin.competitions.save_success'))
      emit('saved', props.compId)
      emit('refresh')
    } else {
      const createDTO: CompetitionCreateRequestDTO = {
        title: formModel.title.trim(),
        slug: formModel.slug.trim(),
        type: formModel.type,
        status: 'DRAFT',
        description: formModel.description,
        bannerUrl: formModel.bannerUrl,
        startTime: startTimeIso,
        endTime: endTimeIso,
        regStartTime: regStartIso,
        regEndTime: regEndIso,
        config: formConfig
      }
      const res = await createCompetitionApi(createDTO)
      message.success(t('admin.competitions.save_success'))
      emit('saved', res.data)
    }
  } catch (error: any) {
    console.error('Submit error:', error)
    message.error(error.message || '保存竞赛失败')
  } finally {
    submitLoading.value = false
  }
}
</script>

<style scoped lang="scss">
.basic-info-tab {
  .tab-top-actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 16px;
    margin-bottom: 20px;
    border-bottom: 1px solid var(--v-border-base, #e5e4e7);

    .tab-subtitle {
      font-size: 15px;
      font-weight: 600;
      color: var(--v-text-primary, #111111);
    }

    .actions-right {
      display: flex;
      align-items: center;
      gap: 12px;
    }
  }

  .banner-uploader-container {
    width: 100%;

    .banner-preview-box {
      width: 100%;
      border-radius: 8px;
      overflow: hidden;
      border: 1px solid var(--v-border-base, #e5e4e7);

      .banner-aspect-ratio-box {
        position: relative;
        width: 100%;
        aspect-ratio: 16 / 9;
        max-height: 280px;
        background: #000000;
        overflow: hidden;

        .banner-img {
          width: 100%;
          height: 100%;
          object-fit: cover;
          display: block;
        }

        .banner-overlay-actions {
          position: absolute;
          bottom: 12px;
          right: 12px;
          display: flex;
          align-items: center;
          gap: 8px;
          z-index: 5;

          .banner-change-btn {
            background: #ffffff !important;
            color: #333333 !important;
            border: 1px solid transparent !important;
            border-radius: 6px;
            font-weight: 500;
            box-shadow: none !important;
            transition: all 0.2s ease;

            :deep(.n-icon) {
              color: inherit;
              transition: color 0.2s ease;
            }

            &:hover {
              background: #ffffff !important;
              border-color: #18a058 !important;
              color: #18a058 !important;
              box-shadow: none !important;
            }
          }

          .banner-remove-btn {
            background: #ffffff !important;
            color: #333333 !important;
            border: 1px solid transparent !important;
            border-radius: 6px;
            font-weight: 500;
            box-shadow: none !important;
            transition: all 0.2s ease;

            :deep(.n-icon) {
              color: inherit;
              transition: color 0.2s ease;
            }

            &:hover {
              background: #ffffff !important;
              border-color: #d03050 !important;
              color: #d03050 !important;
              box-shadow: none !important;
            }
          }
        }
      }
    }

    .banner-drop-zone {
      width: 100%;
      min-height: 140px;
      border: 2px dashed var(--v-border-base, #e5e4e7);
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: var(--v-bg-muted, #f8f8f9);
      cursor: pointer;
      transition: border-color 0.2s;

      &:hover {
        border-color: var(--v-color-primary, #000000);
      }

      .drop-content {
        display: flex;
        flex-direction: column;
        align-items: center;
        padding: 20px;
        text-align: center;

        .upload-icon {
          color: var(--v-text-muted, #999999);
          margin-bottom: 8px;
        }

        .drop-text {
          font-size: 14px;
          font-weight: 500;
          color: var(--v-text-primary, #111111);
          margin-bottom: 4px;
        }

        .drop-hint {
          font-size: 12px;
          color: #444444;
          margin-bottom: 12px;
        }
      }
    }
  }

  .sub-config-card {
    margin-top: 24px;
    background: var(--v-bg-muted, #f8f8f9);
    border-radius: 8px;
  }

  .status-readonly-wrap {
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .status-tip-text {
    font-size: 12px;
    color: #444444;
    line-height: 1.5;
  }
}
</style>
