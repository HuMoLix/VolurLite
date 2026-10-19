<template>
  <n-modal
    :show="show"
    preset="card"
    class="w-[640px] max-w-[95vw] rounded-2xl border border-google-border-light dark:border-google-border-dark bg-white dark:bg-google-surface-darkCard shadow-lg"
    :title="t('admin.competitions.export_scores_modal_title', '导出竞赛数据报表')"
    :bordered="false"
    @update:show="handleClose"
  >
    <div class="space-y-4 max-h-[80vh] overflow-y-auto pr-1">
      <div class="p-3.5 rounded-xl bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light/60 dark:border-google-border-dark/60 flex items-center justify-between">
        <div>
          <div class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary">
            {{ t('admin.competitions.export_target_competition', '目标竞赛') }}
          </div>
          <div class="text-sm font-semibold text-google-text-primary dark:text-google-text-darkPrimary mt-0.5">
            {{ competitionTitle || '-' }}
          </div>
        </div>
        <span class="px-2.5 py-1 rounded-md text-xs font-mono font-medium bg-google-blue/10 text-google-blue dark:text-google-blue-dark">
          {{ competitionType }}
        </span>
      </div>

      <div class="space-y-2">
        <label class="block text-xs font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
          {{ t('admin.competitions.export_format_label', '导出文件格式') }}
        </label>
        <n-radio-group v-model:value="selectedFormat" name="export-format" class="w-full">
          <div class="grid grid-cols-3 gap-2.5">
            <label
              class="flex flex-col items-center justify-center p-3 rounded-xl border cursor-pointer transition-all select-none text-center"
              :class="[
                selectedFormat === 'xlsx'
                  ? 'border-google-blue bg-google-blue/5 dark:bg-google-blue/10 text-google-blue dark:text-google-blue-dark shadow-sm'
                  : 'border-google-border-light dark:border-google-border-dark text-google-text-primary dark:text-google-text-darkPrimary hover:border-google-border-light/80'
              ]"
            >
              <n-radio value="xlsx" class="hidden" />
              <div class="font-bold text-sm">Excel (.xlsx)</div>
              <div class="text-[11px] text-google-text-muted dark:text-google-text-darkMuted mt-0.5">多工作表报表</div>
            </label>

            <label
              class="flex flex-col items-center justify-center p-3 rounded-xl border cursor-pointer transition-all select-none text-center"
              :class="[
                selectedFormat === 'csv'
                  ? 'border-google-blue bg-google-blue/5 dark:bg-google-blue/10 text-google-blue dark:text-google-blue-dark shadow-sm'
                  : 'border-google-border-light dark:border-google-border-dark text-google-text-primary dark:text-google-text-darkPrimary hover:border-google-border-light/80'
              ]"
            >
              <n-radio value="csv" class="hidden" />
              <div class="font-bold text-sm">CSV (.csv)</div>
              <div class="text-[11px] text-google-text-muted dark:text-google-text-darkMuted mt-0.5">UTF-8 纯文本</div>
            </label>

            <label
              class="flex flex-col items-center justify-center p-3 rounded-xl border cursor-pointer transition-all select-none text-center"
              :class="[
                selectedFormat === 'json'
                  ? 'border-google-blue bg-google-blue/5 dark:bg-google-blue/10 text-google-blue dark:text-google-blue-dark shadow-sm'
                  : 'border-google-border-light dark:border-google-border-dark text-google-text-primary dark:text-google-text-darkPrimary hover:border-google-border-light/80'
              ]"
            >
              <n-radio value="json" class="hidden" />
              <div class="font-bold text-sm">JSON (.json)</div>
              <div class="text-[11px] text-google-text-muted dark:text-google-text-darkMuted mt-0.5">结构化数据</div>
            </label>
          </div>
        </n-radio-group>
      </div>

      <div class="space-y-2">
        <label class="block text-xs font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
          {{ t('admin.competitions.export_scope_label', '数据包含范围') }}
        </label>
        <n-radio-group v-model:value="selectedScope" name="export-scope" class="w-full" @update:value="handleScopeChange">
          <div class="grid grid-cols-2 gap-2.5">
            <label
              class="flex flex-col p-3 rounded-xl border cursor-pointer transition-all select-none"
              :class="[
                selectedScope === 'summary'
                  ? 'border-google-blue bg-google-blue/5 dark:bg-google-blue/10 text-google-blue dark:text-google-blue-dark shadow-sm'
                  : 'border-google-border-light dark:border-google-border-dark text-google-text-primary dark:text-google-text-darkPrimary hover:border-google-border-light/80'
              ]"
            >
              <n-radio value="summary" class="hidden" />
              <div class="font-semibold text-xs">成绩总榜</div>
              <div class="text-[11px] text-google-text-muted dark:text-google-text-darkMuted mt-0.5">排名、得分与基本信息</div>
            </label>

            <label
              class="flex flex-col p-3 rounded-xl border cursor-pointer transition-all select-none"
              :class="[
                selectedScope === 'members'
                  ? 'border-google-blue bg-google-blue/5 dark:bg-google-blue/10 text-google-blue dark:text-google-blue-dark shadow-sm'
                  : 'border-google-border-light dark:border-google-border-dark text-google-text-primary dark:text-google-text-darkPrimary hover:border-google-border-light/80'
              ]"
            >
              <n-radio value="members" class="hidden" />
              <div class="font-semibold text-xs">选手及报名明细</div>
              <div class="text-[11px] text-google-text-muted dark:text-google-text-darkMuted mt-0.5">含学号、身份证、手机号等</div>
            </label>

            <label
              class="flex flex-col p-3 rounded-xl border cursor-pointer transition-all select-none"
              :class="[
                selectedScope === 'detail'
                  ? 'border-google-blue bg-google-blue/5 dark:bg-google-blue/10 text-google-blue dark:text-google-blue-dark shadow-sm'
                  : 'border-google-border-light dark:border-google-border-dark text-google-text-primary dark:text-google-text-darkPrimary hover:border-google-border-light/80'
              ]"
            >
              <n-radio value="detail" class="hidden" />
              <div class="font-semibold text-xs">答题/解题流水明细</div>
              <div class="text-[11px] text-google-text-muted dark:text-google-text-darkMuted mt-0.5">包含每题作答与判分流水</div>
            </label>

            <label
              class="flex flex-col p-3 rounded-xl border cursor-pointer transition-all select-none"
              :class="[
                selectedScope === 'all'
                  ? 'border-google-blue bg-google-blue/5 dark:bg-google-blue/10 text-google-blue dark:text-google-blue-dark shadow-sm'
                  : 'border-google-border-light dark:border-google-border-dark text-google-text-primary dark:text-google-text-darkPrimary hover:border-google-border-light/80'
              ]"
            >
              <n-radio value="all" class="hidden" />
              <div class="font-semibold text-xs">完整聚合导出</div>
              <div class="text-[11px] text-google-text-muted dark:text-google-text-darkMuted mt-0.5">总榜 + 报名明细 + 解题流水</div>
            </label>
          </div>
        </n-radio-group>
      </div>

      <div class="space-y-2 pt-1 border-t border-google-border-light/60 dark:border-google-border-dark/60">
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-2">
            <label class="block text-xs font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
              {{ t('admin.competitions.export_fields_label', '自定义导出字段') }}
            </label>
            <span class="text-[11px] font-mono text-google-blue dark:text-google-blue-dark">
              已选 {{ selectedFields.length }} / {{ availableFields.length }}
            </span>
          </div>

          <div class="flex items-center gap-1.5">
            <button
              type="button"
              class="text-[11px] font-medium text-google-blue dark:text-google-blue-dark hover:underline cursor-pointer"
              @click="handleSelectAllFields"
            >
              全选
            </button>
            <span class="text-xs text-google-text-muted dark:text-google-text-darkMuted">·</span>
            <button
              type="button"
              class="text-[11px] font-medium text-google-text-secondary dark:text-google-text-darkSecondary hover:underline cursor-pointer"
              @click="handleResetDefaultFields"
            >
              默认
            </button>
            <span class="text-xs text-google-text-muted dark:text-google-text-darkMuted">·</span>
            <button
              type="button"
              class="text-[11px] font-medium text-google-text-secondary dark:text-google-text-darkSecondary hover:underline cursor-pointer"
              @click="handleClearFields"
            >
              清空
            </button>
          </div>
        </div>

        <div v-if="loadingFields" class="py-6 text-center text-xs text-google-text-muted dark:text-google-text-darkMuted">
          正在加载导出字段配置...
        </div>

        <div v-else class="space-y-2.5 p-3 rounded-xl bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light/60 dark:border-google-border-dark/60">
          <div v-if="groupedFields.registration && groupedFields.registration.length > 0" class="space-y-1.5">
            <div class="flex items-center gap-1.5 text-[11px] font-semibold text-google-blue dark:text-google-blue-dark">
              <span>选手报名信息 (支持学号、身份证、手机号等)</span>
            </div>
            <div class="grid grid-cols-2 sm:grid-cols-3 gap-2">
              <label
                v-for="field in groupedFields.registration"
                :key="field.key"
                class="flex items-center gap-2 px-2.5 py-1.5 rounded-lg border text-xs cursor-pointer select-none transition-colors"
                :class="[
                  selectedFields.includes(field.key)
                    ? 'border-google-blue/60 bg-google-blue/10 text-google-blue dark:text-google-blue-dark font-medium'
                    : 'border-google-border-light dark:border-google-border-dark text-google-text-secondary dark:text-google-text-darkSecondary hover:bg-black/5 dark:hover:bg-white/5'
                ]"
              >
                <input
                  type="checkbox"
                  class="rounded text-google-blue focus:ring-google-blue h-3.5 w-3.5"
                  :checked="selectedFields.includes(field.key)"
                  @change="toggleField(field.key)"
                />
                <span class="truncate">{{ field.label }}</span>
              </label>
            </div>
          </div>

          <div v-if="groupedFields.score && groupedFields.score.length > 0" class="space-y-1.5 pt-2 border-t border-google-border-light/40 dark:border-google-border-dark/40">
            <div class="text-[11px] font-semibold text-google-text-secondary dark:text-google-text-darkSecondary">
              成绩与考评数据
            </div>
            <div class="grid grid-cols-2 sm:grid-cols-3 gap-2">
              <label
                v-for="field in groupedFields.score"
                :key="field.key"
                class="flex items-center gap-2 px-2.5 py-1.5 rounded-lg border text-xs cursor-pointer select-none transition-colors"
                :class="[
                  selectedFields.includes(field.key)
                    ? 'border-google-blue/60 bg-google-blue/10 text-google-blue dark:text-google-blue-dark font-medium'
                    : 'border-google-border-light dark:border-google-border-dark text-google-text-secondary dark:text-google-text-darkSecondary hover:bg-black/5 dark:hover:bg-white/5'
                ]"
              >
                <input
                  type="checkbox"
                  class="rounded text-google-blue focus:ring-google-blue h-3.5 w-3.5"
                  :checked="selectedFields.includes(field.key)"
                  @change="toggleField(field.key)"
                />
                <span class="truncate">{{ field.label }}</span>
              </label>
            </div>
          </div>

          <div v-if="groupedFields.basic && groupedFields.basic.length > 0" class="space-y-1.5 pt-2 border-t border-google-border-light/40 dark:border-google-border-dark/40">
            <div class="text-[11px] font-semibold text-google-text-secondary dark:text-google-text-darkSecondary">
              基础与战队信息
            </div>
            <div class="grid grid-cols-2 sm:grid-cols-3 gap-2">
              <label
                v-for="field in groupedFields.basic"
                :key="field.key"
                class="flex items-center gap-2 px-2.5 py-1.5 rounded-lg border text-xs cursor-pointer select-none transition-colors"
                :class="[
                  selectedFields.includes(field.key)
                    ? 'border-google-blue/60 bg-google-blue/10 text-google-blue dark:text-google-blue-dark font-medium'
                    : 'border-google-border-light dark:border-google-border-dark text-google-text-secondary dark:text-google-text-darkSecondary hover:bg-black/5 dark:hover:bg-white/5'
                ]"
              >
                <input
                  type="checkbox"
                  class="rounded text-google-blue focus:ring-google-blue h-3.5 w-3.5"
                  :checked="selectedFields.includes(field.key)"
                  @change="toggleField(field.key)"
                />
                <span class="truncate">{{ field.label }}</span>
              </label>
            </div>
          </div>

          <div v-if="groupedFields.detail && groupedFields.detail.length > 0" class="space-y-1.5 pt-2 border-t border-google-border-light/40 dark:border-google-border-dark/40">
            <div class="text-[11px] font-semibold text-google-text-secondary dark:text-google-text-darkSecondary">
              答题/解题流水明细字段
            </div>
            <div class="grid grid-cols-2 sm:grid-cols-3 gap-2">
              <label
                v-for="field in groupedFields.detail"
                :key="field.key"
                class="flex items-center gap-2 px-2.5 py-1.5 rounded-lg border text-xs cursor-pointer select-none transition-colors"
                :class="[
                  selectedFields.includes(field.key)
                    ? 'border-google-blue/60 bg-google-blue/10 text-google-blue dark:text-google-blue-dark font-medium'
                    : 'border-google-border-light dark:border-google-border-dark text-google-text-secondary dark:text-google-text-darkSecondary hover:bg-black/5 dark:hover:bg-white/5'
                ]"
              >
                <input
                  type="checkbox"
                  class="rounded text-google-blue focus:ring-google-blue h-3.5 w-3.5"
                  :checked="selectedFields.includes(field.key)"
                  @change="toggleField(field.key)"
                />
                <span class="truncate">{{ field.label }}</span>
              </label>
            </div>
          </div>
        </div>
      </div>

      <div class="flex items-center justify-end gap-3 pt-3 border-t border-google-border-light/70 dark:border-google-border-dark/70">
        <VBtn
          clz="normal"
          size="medium"
          @click="handleClose"
        >
          {{ t('common.cancel', '取消') }}
        </VBtn>
        <VBtn
          clz="primary"
          size="medium"
          :left-icon="CloudDownloadOutline"
          :loading="exporting"
          @click="handleExport"
        >
          {{ t('admin.competitions.confirm_export_btn', '确认下载导出') }}
        </VBtn>
      </div>
    </div>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useMessage } from 'naive-ui'
import { CloudDownloadOutline } from '@vicons/ionicons5'
import VBtn from '@/components/common/VBtn.vue'
import {
  exportAdminCompetitionScoresApi,
  getAdminCompetitionExportFieldsApi,
  type ExportFieldOptionVO
} from '@/api/competition'

defineOptions({
  name: 'AdminCompetitionScoreExportModal'
})

const props = defineProps<{
  show: boolean
  competitionId: string | number
  competitionType: string
  competitionTitle?: string
  initialScope?: string
}>()

const emit = defineEmits<{
  (e: 'update:show', value: boolean): void
}>()

const { t } = useI18n()
const message = useMessage()

const selectedFormat = ref<'xlsx' | 'csv' | 'json'>('xlsx')
const selectedScope = ref<string>('summary')
const availableFields = ref<ExportFieldOptionVO[]>([])
const selectedFields = ref<string[]>([])
const loadingFields = ref(false)
const exporting = ref(false)

const groupedFields = computed(() => {
  const groups: Record<string, ExportFieldOptionVO[]> = {
    registration: [],
    score: [],
    basic: [],
    detail: []
  }
  for (const field of availableFields.value) {
    if (groups[field.group]) {
      groups[field.group].push(field)
    } else {
      groups.basic.push(field)
    }
  }
  return groups
})

watch(
  () => props.show,
  (val) => {
    if (val && props.competitionId) {
      if (props.initialScope) {
        selectedScope.value = props.initialScope
      }
      fetchFieldOptions()
    }
  },
  { immediate: true }
)

function handleScopeChange() {
  fetchFieldOptions()
}

async function fetchFieldOptions() {
  if (!props.competitionId) return
  loadingFields.value = true
  try {
    const res = await getAdminCompetitionExportFieldsApi(props.competitionId, selectedScope.value)
    if (res.data) {
      availableFields.value = res.data
      selectedFields.value = res.data.filter((f) => f.defaultSelected).map((f) => f.key)
    }
  } catch {
  } finally {
    loadingFields.value = false
  }
}

function toggleField(key: string) {
  const idx = selectedFields.value.indexOf(key)
  if (idx >= 0) {
    selectedFields.value.splice(idx, 1)
  } else {
    selectedFields.value.push(key)
  }
}

function handleSelectAllFields() {
  selectedFields.value = availableFields.value.map((f) => f.key)
}

function handleResetDefaultFields() {
  selectedFields.value = availableFields.value.filter((f) => f.defaultSelected).map((f) => f.key)
}

function handleClearFields() {
  selectedFields.value = []
}

function handleClose() {
  if (exporting.value) return
  emit('update:show', false)
}

async function handleExport() {
  if (!props.competitionId) return
  exporting.value = true
  try {
    const fieldsToExport = selectedFields.value.length > 0 ? selectedFields.value : undefined
    const blob = await exportAdminCompetitionScoresApi(
      props.competitionId,
      selectedFormat.value,
      selectedScope.value,
      fieldsToExport
    )
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    const safeTitle = (props.competitionTitle || 'competition').replace(/[\\/:*?"<>|\s]/g, '_')
    a.download = `${safeTitle}_${props.competitionType}_${selectedScope.value}_${Date.now()}.${selectedFormat.value}`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success(t('admin.competitions.export_scores_success', '数据报表导出成功'))
    emit('update:show', false)
  } catch {
  } finally {
    exporting.value = false
  }
}
</script>
