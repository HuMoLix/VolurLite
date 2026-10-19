<template>
  <div class="space-y-6 max-w-6xl mx-auto">
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h2 class="text-xl font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
          {{ t('admin.competitions.modal_create_title', '创建新竞赛') }}
        </h2>
        <p class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary mt-1">
          {{ t('admin.competitions.create_desc', '配置竞赛基础信息、排程及赛制类型，创建后将自动进入对应赛制控制台') }}
        </p>
      </div>

      <div class="flex items-center gap-2.5 flex-wrap">
        <VBtn
          clz="normal"
          size="medium"
          :left-icon="ArrowBackOutline"
          @click="router.push('/admin/competitions')"
        >
          {{ t('common.back', '返回列表') }}
        </VBtn>

        <VBtn
          clz="primary"
          size="medium"
          :left-icon="SaveOutline"
          :loading="saving"
          @click="handleCreate"
        >
          {{ t('admin.competitions.create_btn', '确认创建竞赛') }}
        </VBtn>
      </div>
    </div>

    <div class="space-y-6">
      <div class="p-6 rounded-2xl bg-white dark:bg-[#292a2d] border border-google-border-light dark:border-google-border-dark space-y-4">
        <h3 class="text-sm font-semibold text-google-text-primary dark:text-google-text-darkPrimary border-b border-google-border-light dark:border-google-border-dark pb-3">
          {{ t('admin.competitions.tab_basic', '基本信息与赛制') }}
        </h3>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
              {{ t('admin.competitions.title', '竞赛名称') }} *
            </label>
            <VInput
              v-model:value="formData.title"
              placeholder="例如: 2026 全国网络安全高校邀请赛"
              clearable
            />
          </div>

          <div>
            <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
              Slug (URL 唯一标识) *
            </label>
            <VInput
              v-model:value="formData.slug"
              placeholder="例如: nctf-2026"
              clearable
            />
          </div>

          <div>
            <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
              {{ t('admin.competitions.col_type', '赛制模式') }} *
            </label>
            <VSelection
              v-model:value="formData.type"
              :options="typeOptions"
            />
          </div>

          <div>
            <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
              {{ t('admin.competitions.col_status', '竞赛状态') }}
            </label>
            <VSelection
              v-model:value="formData.status"
              :options="statusOptions"
              :disabled="true"
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
              placeholder="竞赛详细介绍与规则，支持 Markdown 语法..."
              :rows="8"
            />
          </div>
        </div>
      </div>

      <div class="p-6 rounded-2xl bg-white dark:bg-[#292a2d] border border-google-border-light dark:border-google-border-dark space-y-4">
        <h3 class="text-sm font-semibold text-google-text-primary dark:text-google-text-darkPrimary border-b border-google-border-light dark:border-google-border-dark pb-3">
          {{ t('admin.competitions.schedule_settings', '赛程与组队规则') }}
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

          <div class="flex items-center gap-3 pt-6 md:col-span-2">
            <input
              id="teamAuditCheck"
              v-model="formData.requireTeamAudit"
              type="checkbox"
              class="rounded border-gray-300 text-google-blue focus:ring-google-blue h-4 w-4"
            />
            <label for="teamAuditCheck" class="text-xs font-medium text-google-text-primary dark:text-google-text-darkPrimary cursor-pointer">
              {{ t('admin.competitions.require_team_audit', '开启战队成员/队伍加入人工审核') }}
            </label>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useMessage } from 'naive-ui'
import { ArrowBackOutline, SaveOutline } from '@vicons/ionicons5'
import VBtn from '@/components/common/VBtn.vue'
import VInput from '@/components/common/VInput.vue'
import VSelection from '@/components/common/VSelection.vue'
import VBanner from '@/components/common/VBanner.vue'
import VDatePicker from '@/components/common/VDatePicker.vue'
import VMarkdownEditor from '@/components/common/VMarkdownEditor.vue'
import type {
  CompetitionType,
  CompetitionStatus,
  CompetitionCreateRequestDTO
} from '@/types/competition'
import { createCompetitionApi } from '@/api/competition'

defineOptions({
  name: 'AdminCompetitionCreateView'
})

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const message = useMessage()

const saving = ref(false)

const formData = ref({
  title: '',
  slug: '',
  type: 'CTF' as CompetitionType,
  status: 'DRAFT' as CompetitionStatus,
  bannerUrl: '',
  description: '',
  startTime: '',
  endTime: '',
  regStartTime: '',
  regEndTime: '',
  requireTeamAudit: false
})

const typeOptions = computed(() => [
  { label: t('competition.type_ctf', 'CTF 解题赛'), value: 'CTF' },
  { label: t('competition.type_theory', '理论答题赛'), value: 'THEORY' },
  { label: t('competition.type_awd', 'AWD 攻防对抗'), value: 'AWD' },
  { label: t('competition.type_awd_plus', 'AWD+ 综合攻防'), value: 'AWD_PLUS' },
  { label: t('competition.type_isw', 'ISW 靶场实战'), value: 'ISW' }
])

const statusOptions = computed(() => [
  { label: t('competition.status_draft', '草稿'), value: 'DRAFT' }
])

function formatToInstant(val?: string | number | null): string | undefined {
  if (!val) return undefined
  if (typeof val === 'number') {
    return new Date(val).toISOString()
  }
  const d = new Date(val)
  return isNaN(d.getTime()) ? undefined : d.toISOString()
}

async function handleCreate() {
  if (!formData.value.title.trim()) {
    message.warning(t('admin.competitions.form_title_placeholder', '请输入竞赛名称'))
    return
  }

  if (!formData.value.slug.trim()) {
    message.warning('请输入竞赛 Slug')
    return
  }

  if (!formData.value.startTime || !formData.value.endTime) {
    message.warning('请选择竞赛起止时间')
    return
  }

  const startTs = new Date(formData.value.startTime).getTime()
  const endTs = new Date(formData.value.endTime).getTime()
  if (startTs >= endTs) {
    message.warning('竞赛开始时间必须早于结束时间')
    return
  }

  if (formData.value.regStartTime && formData.value.regEndTime) {
    const regStartTs = new Date(formData.value.regStartTime).getTime()
    const regEndTs = new Date(formData.value.regEndTime).getTime()
    if (regStartTs >= regEndTs) {
      message.warning('报名开始时间必须早于报名结束时间')
      return
    }
    if (regEndTs > endTs) {
      message.warning('报名截止时间不能晚于比赛结束时间')
      return
    }
  }

  saving.value = true
  try {
    const payload: CompetitionCreateRequestDTO = {
      title: formData.value.title.trim(),
      slug: formData.value.slug.trim(),
      type: formData.value.type,
      status: 'DRAFT',
      bannerUrl: formData.value.bannerUrl.trim() || undefined,
      description: formData.value.description.trim() || undefined,
      startTime: formatToInstant(formData.value.startTime)!,
      endTime: formatToInstant(formData.value.endTime)!,
      regStartTime: formatToInstant(formData.value.regStartTime),
      regEndTime: formatToInstant(formData.value.regEndTime),
      config: {
        requireTeamAudit: formData.value.requireTeamAudit
      }
    }

    const res = await createCompetitionApi(payload)
    message.success(t('admin.competitions.create_success', '竞赛创建成功'))

    if (res.data) {
      const newId = res.data
      if (formData.value.type === 'THEORY') {
        router.push(`/admin/competitions/theory/${newId}/edit`)
      } else {
        router.push(`/admin/competitions/ctf/${newId}/edit`)
      }
    } else {
      router.push('/admin/competitions')
    }
  } catch (err: any) {
    message.error(err.message || t('admin.competitions.save_failed', '创建竞赛失败'))
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  if (route.path.includes('/theory/create')) {
    formData.value.type = 'THEORY'
  } else if (route.path.includes('/ctf/create')) {
    formData.value.type = 'CTF'
  } else {
    const qType = String(route.query.type || '').toUpperCase() as CompetitionType
    if (['CTF', 'THEORY', 'AWD', 'AWD_PLUS', 'ISW'].includes(qType)) {
      formData.value.type = qType
    }
  }
})
</script>
