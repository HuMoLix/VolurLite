<template>
  <div class="space-y-6">
    <div class="p-6 rounded-2xl bg-white dark:bg-[#292a2d] border border-google-border-light dark:border-google-border-dark space-y-4">
      <div class="flex items-center justify-between border-b border-google-border-light dark:border-google-border-dark pb-3">
        <h3 class="text-sm font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
          {{ t('admin.competitions.tab_basic', '基础信息') }}
        </h3>
        <VBtn
          clz="primary"
          size="medium"
          :left-icon="SaveOutline"
          :loading="saving"
          @click="handleSave"
        >
          {{ isEdit ? t('common.save', '保存') : t('admin.competitions.create_btn', '确认创建竞赛') }}
        </VBtn>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
            {{ t('admin.competitions.title', '竞赛名称') }} *
          </label>
          <VInput
            v-model:value="formData.title"
            placeholder="例如: 2026 网络安全理论知识考核初赛"
            clearable
          />
        </div>

        <div>
          <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
            Slug (URL 唯一标识) *
          </label>
          <VInput
            v-model:value="formData.slug"
            placeholder="例如: theory-2026"
            clearable
          />
        </div>

        <div>
          <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
            {{ t('admin.competitions.col_type', '赛制类型') }} *
          </label>
          <VSelection
            v-model:value="formData.type"
            :options="typeOptions"
            :disabled="true"
          />
        </div>

        <div>
          <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
            {{ t('admin.competitions.col_status', '竞赛状态') }}
          </label>
          <VSelection
            v-model:value="formData.status"
            :options="statusOptions"
            :disabled="runtimeStatus === 'ARCHIVED'"
          />
        </div>

        <div class="md:col-span-2">
          <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
            Banner 封面图
          </label>
          <VBanner
            v-model:value="formData.bannerUrl"
            upload-folder="banners"
            height="180px"
          />
        </div>

        <div class="md:col-span-2">
          <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
            竞赛简介与规则
          </label>
          <VMarkdownEditor
            v-model:value="formData.description"
            placeholder="竞赛详细介绍与考核规则，支持 Markdown 语法..."
            :rows="8"
          />
        </div>

        <div class="md:col-span-2">
          <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
            自定义报名要求与须知
          </label>
          <VMarkdownEditor
            v-model:value="formData.regRequirements"
            placeholder="填写自定义报名要求与须知，支持 Markdown 语法..."
            :rows="6"
          />
        </div>
      </div>
    </div>

    <div class="p-6 rounded-2xl bg-white dark:bg-[#292a2d] border border-google-border-light dark:border-google-border-dark space-y-4">
      <h3 class="text-sm font-semibold text-google-text-primary dark:text-google-text-darkPrimary border-b border-google-border-light dark:border-google-border-dark pb-3">
        {{ t('admin.competitions.schedule_settings', '赛程与时间设置') }}
      </h3>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
            竞赛起止时间 *
          </label>
          <VDatePicker
            v-model:start-value="formData.startTime"
            v-model:end-value="formData.endTime"
            type="datetimerange"
            start-placeholder="竞赛开始时间"
            end-placeholder="竞赛结束时间"
          />
        </div>

        <div>
          <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
            报名起止时间
          </label>
          <VDatePicker
            v-model:start-value="formData.regStartTime"
            v-model:end-value="formData.regEndTime"
            type="datetimerange"
            start-placeholder="报名开始时间"
            end-placeholder="报名截止时间"
          />
        </div>
      </div>
    </div>

    <div class="p-6 rounded-2xl bg-white dark:bg-[#292a2d] border border-google-border-light dark:border-google-border-dark space-y-4">
      <h3 class="text-sm font-semibold text-google-text-primary dark:text-google-text-darkPrimary border-b border-google-border-light dark:border-google-border-dark pb-3">
        {{ t('admin.competitions.team_limit_settings', '队伍人数限制') }}
      </h3>

      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <div>
          <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
            单队人数下限
          </label>
          <VInput
            v-model:value="formData.minTeamMembers"
            placeholder="默认 1"
            clearable
          />
        </div>

        <div>
          <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
            单队人数上限
          </label>
          <VInput
            v-model:value="formData.maxTeamMembers"
            placeholder="默认 4"
            clearable
          />
        </div>

        <div>
          <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
            最大领队人数
          </label>
          <VInput
            v-model:value="formData.maxLeaders"
            placeholder="默认 1"
            clearable
          />
        </div>

        <div>
          <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
            参赛队伍上限
          </label>
          <VInput
            v-model:value="formData.maxTeams"
            placeholder="默认 500"
            clearable
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useMessage } from 'naive-ui'
import { SaveOutline } from '@vicons/ionicons5'
import VBtn from '@/components/common/VBtn.vue'
import VInput from '@/components/common/VInput.vue'
import VSelection from '@/components/common/VSelection.vue'
import VBanner from '@/components/common/VBanner.vue'
import VDatePicker from '@/components/common/VDatePicker.vue'
import VMarkdownEditor from '@/components/common/VMarkdownEditor.vue'
import type { CompetitionType, CompetitionStatus } from '@/types/competition'
import {
  getAdminCompetitionByIdApi,
  createCompetitionApi,
  updateCompetitionApi
} from '@/api/competition'

defineOptions({
  name: 'AdminTheoryCompetitionDetailView'
})

const props = defineProps<{
  compId?: string | number
  isEdit?: boolean
}>()

const emit = defineEmits<{
  (e: 'saved', id?: string | number): void
  (e: 'updateTitle', title: string): void
}>()

const router = useRouter()
const { t } = useI18n()
const message = useMessage()

const saving = ref(false)

const formData = ref({
  title: '',
  slug: '',
  type: 'THEORY' as CompetitionType,
  status: 'DRAFT' as CompetitionStatus,
  bannerUrl: '',
  description: '',
  startTime: '',
  endTime: '',
  regStartTime: '',
  regEndTime: '',
  requireTeamAudit: false,
  minTeamMembers: '1',
  maxTeamMembers: '4',
  maxLeaders: '1',
  maxTeams: '500',
  regRequirements: ''
})

const typeOptions = [
  { label: '理论答题赛', value: 'THEORY' }
]

const runtimeStatus = ref<CompetitionStatus>('DRAFT')

const statusOptions = computed(() => {
  if (runtimeStatus.value === 'ARCHIVED') {
    return [
      { label: t('competition.status_archived', '已归档 (只读)'), value: 'ARCHIVED' }
    ]
  }
  return [
    { label: t('competition.status_draft', '草稿'), value: 'DRAFT' },
    { label: t('competition.status_published', '已发布 (按时间自动运行)'), value: 'PUBLISHED' },
    { label: t('competition.status_hidden', '隐藏'), value: 'HIDDEN' }
  ]
})

function formatToInstant(val?: string | number | null): string | undefined {
  if (!val) return undefined
  const str = String(val).trim()
  if (!str) return undefined
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?(Z|[+-]\d{2}:\d{2})?$/.test(str)) {
    if (str.endsWith('Z') || /[+-]\d{2}:\d{2}$/.test(str)) {
      return str
    }
  }
  const ts = typeof val === 'number' ? val : Date.parse(str.includes('T') ? str : str.replace(' ', 'T'))
  if (isNaN(ts)) return undefined
  return new Date(ts).toISOString()
}

async function fetchDetail() {
  if (!props.compId) return
  try {
    const res = await getAdminCompetitionByIdApi(props.compId)
    if (res.data) {
      runtimeStatus.value = (res.data.status || 'DRAFT') as CompetitionStatus
      const isPublished = ['PUBLISHED', 'REGISTRATION', 'RUNNING', 'ENDED'].includes(runtimeStatus.value)
      const currentStatus = runtimeStatus.value === 'ARCHIVED'
        ? 'ARCHIVED'
        : (runtimeStatus.value === 'HIDDEN' ? 'HIDDEN' : (isPublished ? 'PUBLISHED' : 'DRAFT'))
      formData.value = {
        title: res.data.title || '',
        slug: res.data.slug || '',
        type: 'THEORY',
        status: currentStatus,
        bannerUrl: res.data.bannerUrl || '',
        description: res.data.description || '',
        startTime: res.data.startTime || '',
        endTime: res.data.endTime || '',
        regStartTime: res.data.regStartTime || '',
        regEndTime: res.data.regEndTime || '',
        requireTeamAudit: Boolean(res.data.requireTeamAudit),
        minTeamMembers: res.data.minTeamMembers !== undefined && res.data.minTeamMembers !== null ? String(res.data.minTeamMembers) : '1',
        maxTeamMembers: res.data.maxTeamMembers !== undefined && res.data.maxTeamMembers !== null ? String(res.data.maxTeamMembers) : '4',
        maxLeaders: res.data.maxLeaders !== undefined && res.data.maxLeaders !== null ? String(res.data.maxLeaders) : '1',
        maxTeams: res.data.maxTeams !== undefined && res.data.maxTeams !== null ? String(res.data.maxTeams) : '500',
        regRequirements: res.data.regRequirements || ''
      }
      emit('updateTitle', res.data.title || '')
    }
  } catch (err: any) {
    message.error(err.message || t('admin.competitions.fetch_detail_failed', '获取竞赛详情失败'))
  }
}

async function handleSave() {
  if (!formData.value.title.trim() || !formData.value.slug.trim()) {
    message.warning(t('admin.competitions.title_slug_required', '竞赛名称与 Slug 为必填项'))
    return
  }
  if (!formData.value.startTime || !formData.value.endTime) {
    message.warning(t('admin.competitions.time_required', '请填写竞赛起止时间'))
    return
  }
  const minM = formData.value.minTeamMembers ? Number(formData.value.minTeamMembers) : undefined
  const maxM = formData.value.maxTeamMembers ? Number(formData.value.maxTeamMembers) : undefined
  if (minM !== undefined && maxM !== undefined && minM > maxM) {
    message.warning('单队人数下限不能大于单队人数上限')
    return
  }

  saving.value = true
  try {
    const targetStatus = formData.value.status === 'DRAFT'
      ? ('DRAFT' as CompetitionStatus)
      : (formData.value.status === 'HIDDEN' ? ('HIDDEN' as CompetitionStatus) : ('PUBLISHED' as CompetitionStatus))
    const payload = {
      title: formData.value.title.trim(),
      slug: formData.value.slug.trim(),
      type: 'THEORY' as CompetitionType,
      status: targetStatus,
      bannerUrl: formData.value.bannerUrl.trim() || undefined,
      description: formData.value.description.trim() || undefined,
      startTime: formatToInstant(formData.value.startTime)!,
      endTime: formatToInstant(formData.value.endTime)!,
      regStartTime: formatToInstant(formData.value.regStartTime),
      regEndTime: formatToInstant(formData.value.regEndTime),
      config: {
        requireTeamAudit: formData.value.requireTeamAudit,
        minTeamMembers: minM,
        maxTeamMembers: maxM,
        maxLeaders: formData.value.maxLeaders ? Number(formData.value.maxLeaders) : undefined,
        maxTeams: formData.value.maxTeams ? Number(formData.value.maxTeams) : undefined,
        regRequirements: formData.value.regRequirements.trim() || undefined
      }
    }

    if (props.isEdit && props.compId) {
      await updateCompetitionApi(props.compId, payload)
      message.success(t('admin.competitions.saved_success', '竞赛基本信息已更新'))
      await fetchDetail()
      emit('saved', props.compId)
    } else {
      const res = await createCompetitionApi(payload)
      message.success(t('admin.competitions.create_success', '竞赛创建成功'))
      if (res.data) {
        emit('saved', res.data)
        router.push(`/admin/competitions/theory/${res.data}/edit`)
      } else {
        router.push('/admin/competitions')
      }
    }
  } catch (err: any) {
    message.error(err.message || t('admin.competitions.save_failed', '保存竞赛失败'))
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  if (props.isEdit) {
    fetchDetail()
  }
})
</script>
