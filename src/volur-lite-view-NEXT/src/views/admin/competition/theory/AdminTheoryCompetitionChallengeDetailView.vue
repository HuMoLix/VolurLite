<template>
  <div class="space-y-6">
    <div class="p-6 rounded-2xl bg-white dark:bg-[#292a2d] border border-google-border-light dark:border-google-border-dark space-y-5">
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-google-border-light dark:border-google-border-dark pb-4">
        <div>
          <h3 class="text-sm font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
            {{ t('admin.competitions.theory_paper_config', '理论题库引用与抽题分布规则') }}
          </h3>
          <p class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary mt-0.5">
            {{ t('admin.competitions.theory_paper_config_desc', '选择引用的题库并配置题目分布与分值，选手进入考场时系统将自动根据规则随机抽取生成专属试卷') }}
          </p>
        </div>
        <VBtn
          clz="primary"
          size="medium"
          :left-icon="SaveOutline"
          :loading="savingTheoryConfig"
          @click="handleSaveTheoryConfig"
        >
          {{ t('common.save', '保存赛题配置') }}
        </VBtn>
      </div>

      <div class="space-y-4">
        <div>
          <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
            {{ t('admin.competitions.referenced_banks', '引用题库 (支持多选)') }} *
          </label>
          <VSelection
            v-model:value="theoryConfig.bankIds"
            :options="bankSelectOptions"
            :multiple="true"
            :filterable="true"
            placeholder="请选择本场竞赛引用的理论题库..."
            class="w-full"
          />
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 pt-2">
          <div class="p-4 rounded-xl bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light/70 dark:border-google-border-dark/70 space-y-3">
            <div class="text-xs font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
              单项选择题
            </div>
            <div class="space-y-2">
              <div>
                <span class="text-[11px] text-google-text-secondary dark:text-google-text-darkSecondary">题目数量</span>
                <VInput
                  :value="String(theoryConfig.singleChoiceCount)"
                  @update:value="(val) => theoryConfig.singleChoiceCount = parseInt(val, 10) || 0"
                />
              </div>
              <div>
                <span class="text-[11px] text-google-text-secondary dark:text-google-text-darkSecondary">每题分值</span>
                <VInput
                  :value="String(theoryConfig.singleChoiceScore)"
                  @update:value="(val) => theoryConfig.singleChoiceScore = parseInt(val, 10) || 0"
                />
              </div>
            </div>
          </div>

          <div class="p-4 rounded-xl bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light/70 dark:border-google-border-dark/70 space-y-3">
            <div class="text-xs font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
              多项选择题
            </div>
            <div class="space-y-2">
              <div>
                <span class="text-[11px] text-google-text-secondary dark:text-google-text-darkSecondary">题目数量</span>
                <VInput
                  :value="String(theoryConfig.multipleChoiceCount)"
                  @update:value="(val) => theoryConfig.multipleChoiceCount = parseInt(val, 10) || 0"
                />
              </div>
              <div>
                <span class="text-[11px] text-google-text-secondary dark:text-google-text-darkSecondary">每题分值</span>
                <VInput
                  :value="String(theoryConfig.multipleChoiceScore)"
                  @update:value="(val) => theoryConfig.multipleChoiceScore = parseInt(val, 10) || 0"
                />
              </div>
            </div>
          </div>

          <div class="p-4 rounded-xl bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light/70 dark:border-google-border-dark/70 space-y-3">
            <div class="text-xs font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
              判断题
            </div>
            <div class="space-y-2">
              <div>
                <span class="text-[11px] text-google-text-secondary dark:text-google-text-darkSecondary">题目数量</span>
                <VInput
                  :value="String(theoryConfig.trueFalseCount)"
                  @update:value="(val) => theoryConfig.trueFalseCount = parseInt(val, 10) || 0"
                />
              </div>
              <div>
                <span class="text-[11px] text-google-text-secondary dark:text-google-text-darkSecondary">每题分值</span>
                <VInput
                  :value="String(theoryConfig.trueFalseScore)"
                  @update:value="(val) => theoryConfig.trueFalseScore = parseInt(val, 10) || 0"
                />
              </div>
            </div>
          </div>

          <div class="p-4 rounded-xl bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light/70 dark:border-google-border-dark/70 space-y-3">
            <div class="text-xs font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
              主观问答题
            </div>
            <div class="space-y-2">
              <div>
                <span class="text-[11px] text-google-text-secondary dark:text-google-text-darkSecondary">题目数量</span>
                <VInput
                  :value="String(theoryConfig.essayCount)"
                  @update:value="(val) => theoryConfig.essayCount = parseInt(val, 10) || 0"
                />
              </div>
              <div>
                <span class="text-[11px] text-google-text-secondary dark:text-google-text-darkSecondary">每题分值</span>
                <VInput
                  :value="String(theoryConfig.essayScore)"
                  @update:value="(val) => theoryConfig.essayScore = parseInt(val, 10) || 0"
                />
              </div>
            </div>
          </div>
        </div>

        <div class="flex flex-col sm:flex-row items-start sm:items-center gap-4 pt-2">
          <div class="flex items-center gap-2.5">
            <input
              id="isRandomCheckbox"
              v-model="theoryConfig.isRandom"
              type="checkbox"
              class="w-4 h-4 rounded text-google-blue focus:ring-google-blue border-google-border-light dark:border-google-border-dark cursor-pointer"
            />
            <label for="isRandomCheckbox" class="text-xs font-medium text-google-text-primary dark:text-google-text-darkPrimary cursor-pointer select-none">
              {{ t('admin.competitions.is_random_label', '随机抽取题目并打乱题目顺序 (推荐开启)') }}
            </label>
          </div>

          <div class="flex items-center gap-2.5">
            <input
              id="shuffleOptionsCheckbox"
              v-model="theoryConfig.shuffleOptions"
              type="checkbox"
              class="w-4 h-4 rounded text-google-blue focus:ring-google-blue border-google-border-light dark:border-google-border-dark cursor-pointer"
            />
            <label for="shuffleOptionsCheckbox" class="text-xs font-medium text-google-text-primary dark:text-google-text-darkPrimary cursor-pointer select-none">
              {{ t('admin.competitions.shuffle_options_label', '打乱选项与答案顺序 (推荐开启)') }}
            </label>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useMessage } from 'naive-ui'
import { SaveOutline } from '@vicons/ionicons5'
import VBtn from '@/components/common/VBtn.vue'
import VInput from '@/components/common/VInput.vue'
import VSelection from '@/components/common/VSelection.vue'
import {
  getAdminTheoryCompetitionConfigApi,
  saveAdminTheoryCompetitionConfigApi
} from '@/api/competition'
import { getAdminTheoryBankListApi } from '@/api/challenge'
import type { TheoryBankListVO } from '@/types/challenge'

defineOptions({
  name: 'AdminTheoryCompetitionChallengeDetailView'
})

const props = defineProps<{
  compId: string | number
}>()

const { t } = useI18n()
const message = useMessage()

const availableBanks = ref<TheoryBankListVO[]>([])
const savingTheoryConfig = ref(false)
const theoryConfig = ref({
  bankIds: [] as (number | string)[],
  singleChoiceCount: 10,
  singleChoiceScore: 2,
  multipleChoiceCount: 5,
  multipleChoiceScore: 4,
  trueFalseCount: 10,
  trueFalseScore: 2,
  essayCount: 2,
  essayScore: 10,
  isRandom: true,
  shuffleOptions: true,
  totalQuestions: 0,
  totalScore: 0
})

const bankSelectOptions = computed(() => {
  return availableBanks.value.map(b => ({
    label: `${b.name} (${b.questionCount ?? 0} 题)${b.category ? ' - ' + b.category : ''}`,
    value: b.id
  }))
})

async function fetchTheoryData() {
  if (!props.compId) return
  try {
    const bankRes = await getAdminTheoryBankListApi({ page: 1, size: 100 })
    if (bankRes.data) {
      availableBanks.value = bankRes.data.list ?? []
    }
    const cfgRes = await getAdminTheoryCompetitionConfigApi(props.compId)
    if (cfgRes.data) {
      const c = cfgRes.data
      theoryConfig.value = {
        bankIds: c.bankIds ?? [],
        singleChoiceCount: c.singleChoiceCount ?? 0,
        singleChoiceScore: c.singleChoiceScore ?? 2,
        multipleChoiceCount: c.multipleChoiceCount ?? 0,
        multipleChoiceScore: c.multipleChoiceScore ?? 4,
        trueFalseCount: c.trueFalseCount ?? 0,
        trueFalseScore: c.trueFalseScore ?? 2,
        essayCount: c.essayCount ?? 0,
        essayScore: c.essayScore ?? 10,
        isRandom: c.isRandom ?? true,
        shuffleOptions: c.shuffleOptions ?? true,
        totalQuestions: c.totalQuestions ?? 0,
        totalScore: c.totalScore ?? 0
      }
    }
  } catch (err: any) {
    message.error(err.message || '获取理论试卷配置失败')
  }
}

async function handleSaveTheoryConfig() {
  if (!props.compId) return
  if (!theoryConfig.value.bankIds || theoryConfig.value.bankIds.length === 0) {
    message.warning('请选择至少一个引用的理论题库')
    return
  }
  savingTheoryConfig.value = true
  try {
    const res = await saveAdminTheoryCompetitionConfigApi(props.compId, {
      bankIds: theoryConfig.value.bankIds,
      singleChoiceCount: theoryConfig.value.singleChoiceCount,
      singleChoiceScore: theoryConfig.value.singleChoiceScore,
      multipleChoiceCount: theoryConfig.value.multipleChoiceCount,
      multipleChoiceScore: theoryConfig.value.multipleChoiceScore,
      trueFalseCount: theoryConfig.value.trueFalseCount,
      trueFalseScore: theoryConfig.value.trueFalseScore,
      essayCount: theoryConfig.value.essayCount,
      essayScore: theoryConfig.value.essayScore,
      isRandom: theoryConfig.value.isRandom,
      shuffleOptions: theoryConfig.value.shuffleOptions
    })
    message.success(res.message || '赛题规则配置已保存')
    await fetchTheoryData()
  } catch (err: any) {
    message.error(err.message || '保存赛题配置失败')
  } finally {
    savingTheoryConfig.value = false
  }
}

onMounted(() => {
  fetchTheoryData()
})
</script>
