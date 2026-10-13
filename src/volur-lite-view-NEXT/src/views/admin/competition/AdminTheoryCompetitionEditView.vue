<template>
  <div class="space-y-6 max-w-6xl mx-auto">
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h2 class="text-xl font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
          {{ isEdit ? (detailData.title || t('admin.competitions.manage_title', '理论竞赛运维')) : t('admin.competitions.modal_create_title', '创建新理论竞赛') }}
        </h2>
      </div>

      <div class="flex items-center gap-2.5 flex-wrap">
        <VBtn
          clz="primary"
          size="medium"
          :left-icon="SaveOutline"
          :loading="saving"
          @click="handleSaveBasic"
        >
          {{ isEdit ? t('common.save', '保存基本信息') : t('admin.competitions.create_btn', '确认创建竞赛') }}
        </VBtn>
      </div>
    </div>

    <div v-if="loadingDetail" class="p-12 text-center text-google-blue dark:text-google-blue-dark">
      {{ t('common.loading', '正在加载竞赛详情...') }}
    </div>

    <div v-else class="space-y-6">
      <div v-if="isEdit" class="pt-1">
        <VFilterBtnGroup
          v-model:value="activeTab"
          :options="tabs"
          size="medium"
          :bullet="false"
        />
      </div>

      <div v-if="activeTab === 'basic'" class="space-y-6">
        <div class="p-6 rounded-2xl bg-white dark:bg-[#292a2d] border border-google-border-light dark:border-google-border-dark space-y-4">
          <h3 class="text-sm font-semibold text-google-text-primary dark:text-google-text-darkPrimary border-b border-google-border-light dark:border-google-border-dark pb-3">
            {{ t('admin.competitions.tab_basic', '基本信息与排程') }}
          </h3>

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
      </div>

      <div v-if="activeTab === 'challenges' && isEdit" class="space-y-6">
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

      <div v-if="activeTab === 'announcements' && isEdit" class="space-y-4">
        <div class="flex items-center justify-between">
          <h3 class="text-sm font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
            {{ t('admin.competitions.announcements_list', '比赛专属公告列表') }}
          </h3>
          <VBtn
            clz="primary"
            size="medium"
            :left-icon="AddOutline"
            @click="openCreateAnnounceModal"
          >
            {{ t('admin.competitions.create_announce_btn', '发布公告') }}
          </VBtn>
        </div>

        <VTable
          :columns="announcementColumns"
          :data="announcements"
          :loading="loadingAnnouncements"
          :pagination="false"
          size="small"
          bordered
        />
      </div>

      <div v-if="activeTab === 'teams' && isEdit" class="space-y-4">
        <div class="p-4 rounded-xl bg-white dark:bg-[#292a2d] border border-google-border-light dark:border-google-border-dark flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div>
            <div class="text-xs font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
              开启参赛报名战队资质审核
            </div>
            <div class="text-[11px] text-google-text-secondary dark:text-google-text-darkSecondary mt-0.5">
              开启后队长提交报名需经管理员在此处审核通过方可正式参赛
            </div>
          </div>
          <div class="flex items-center gap-2.5 shrink-0">
            <n-switch
              v-model:value="formData.requireTeamAudit"
              size="small"
            />
            <span class="text-xs font-medium text-google-text-primary dark:text-google-text-darkPrimary select-none">
              {{ formData.requireTeamAudit ? '已开启审核' : '未开启审核' }}
            </span>
          </div>
        </div>

        <div class="grid grid-cols-2 md:grid-cols-4 gap-3">
          <div class="p-4 rounded-xl bg-white dark:bg-[#292a2d] border border-google-border-light dark:border-google-border-dark text-center">
            <div class="text-xl font-bold text-google-text-primary dark:text-google-text-darkPrimary">{{ teamStats.total }}</div>
            <div class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary mt-0.5">全部战队</div>
          </div>
          <div class="p-4 rounded-xl bg-white dark:bg-[#292a2d] border border-google-border-light dark:border-google-border-dark text-center">
            <div class="text-xl font-bold text-yellow-600 dark:text-yellow-400">{{ teamStats.pending }}</div>
            <div class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary mt-0.5">待审核</div>
          </div>
          <div class="p-4 rounded-xl bg-white dark:bg-[#292a2d] border border-google-border-light dark:border-google-border-dark text-center">
            <div class="text-xl font-bold text-green-600 dark:text-green-400">{{ teamStats.approved }}</div>
            <div class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary mt-0.5">已通过</div>
          </div>
          <div class="p-4 rounded-xl bg-white dark:bg-[#292a2d] border border-google-border-light dark:border-google-border-dark text-center">
            <div class="text-xl font-bold text-red-600 dark:text-red-400">{{ teamStats.rejected }}</div>
            <div class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary mt-0.5">已驳回</div>
          </div>
        </div>

        <VTable
          :columns="teamColumns"
          :data="teams"
          :loading="loadingTeams"
          :pagination="teamPagination"
          size="small"
          bordered
          @update:page="handleTeamPageChange"
        />
      </div>

      <div v-if="activeTab === 'monitor' && isEdit" class="space-y-4">
        <AdminTheoryCompetitionTeamMonitor :competition-id="compId" />
      </div>

      <div v-if="activeTab === 'logs' && isEdit" class="space-y-4">
        <div class="flex items-center justify-between">
          <h3 class="text-sm font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
            {{ t('admin.competitions.logs_list', '比赛运行与选手答题操作日志') }}
          </h3>
        </div>

        <VTable
          :columns="logColumns"
          :data="compLogs"
          :loading="loadingLogs"
          :pagination="logPagination"
          size="small"
          bordered
          @update:page="handleLogPageChange"
        />
      </div>
    </div>

    <VModal
      v-model:show="showAnnounceModal"
      :title="t('admin.competitions.create_announce_title', '发布比赛公告')"
      :width="540"
    >
      <div class="space-y-4">
        <div>
          <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
            公告标题 *
          </label>
          <VInput
            v-model:value="announceForm.title"
            placeholder="公告简述标题"
            clearable
          />
        </div>
        <div>
          <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
            公告内容 *
          </label>
          <VInput
            v-model:value="announceForm.content"
            type="textarea"
            :rows="4"
            placeholder="请输入公告正文..."
          />
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-3">
          <VBtn clz="normal" size="medium" @click="showAnnounceModal = false">
            {{ t('common.cancel', '取消') }}
          </VBtn>
          <VBtn clz="primary" size="medium" :loading="publishingAnnounce" @click="handlePublishAnnounce">
            {{ t('common.confirm', '发布') }}
          </VBtn>
        </div>
      </template>
    </VModal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useMessage, NSwitch, type DataTableColumns } from 'naive-ui'
import {
  SaveOutline,
  AddOutline,
  TrashOutline,
  CheckmarkCircleOutline,
  CloseCircleOutline,
  InformationCircleOutline,
  DocumentTextOutline,
  MegaphoneOutline,
  PeopleOutline,
  DesktopOutline,
  ListOutline
} from '@vicons/ionicons5'
import VBtn from '@/components/common/VBtn.vue'
import VInput from '@/components/common/VInput.vue'
import VSelection from '@/components/common/VSelection.vue'
import VTable from '@/components/common/VTable.vue'
import VModal from '@/components/common/VModal.vue'
import VFilterBtnGroup from '@/components/common/VFilterBtnGroup.vue'
import VBanner from '@/components/common/VBanner.vue'
import VDatePicker from '@/components/common/VDatePicker.vue'
import VMarkdownEditor from '@/components/common/VMarkdownEditor.vue'
import AdminTheoryCompetitionTeamMonitor from './components/AdminTheoryCompetitionTeamMonitor.vue'
import type { FilterOption } from '@/components/common/types'
import type {
  CompetitionType,
  CompetitionStatus,
  AdminCompetitionDetailVO,
  CompetitionAnnouncementVO,
  AdminTeamListVO,
  AdminCompetitionLogVO
} from '@/types/competition'
import {
  getAdminCompetitionByIdApi,
  createCompetitionApi,
  updateCompetitionApi,
  listAdminCompetitionAnnouncementsApi,
  createAdminCompetitionAnnouncementApi,
  deleteAdminCompetitionAnnouncementApi,
  listAdminTeamsApi,
  getAdminTeamStatsApi,
  auditAdminTeamApi,
  getAdminCompetitionLogListApi,
  getAdminTheoryCompetitionConfigApi,
  saveAdminTheoryCompetitionConfigApi
} from '@/api/competition'
import { getAdminTheoryBankListApi } from '@/api/challenge'
import type { TheoryBankListVO } from '@/types/challenge'

defineOptions({
  name: 'AdminTheoryCompetitionEditView'
})

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const message = useMessage()

const compId = computed(() => route.params.id as string | undefined)
const isEdit = computed(() => Boolean(compId.value))

const activeTab = ref('basic')
const loadingDetail = ref(false)
const saving = ref(false)

const tabs = computed<FilterOption<string>[]>(() => [
  { value: 'basic', label: t('admin.competitions.tab_basic', '基本信息与排程'), icon: InformationCircleOutline },
  { value: 'challenges', label: t('admin.competitions.tab_challenges', '理论试卷考题'), icon: DocumentTextOutline },
  { value: 'announcements', label: t('admin.competitions.tab_announcements', '比赛公告'), icon: MegaphoneOutline },
  { value: 'teams', label: t('admin.competitions.tab_teams', '战队审核'), icon: PeopleOutline },
  { value: 'monitor', label: t('admin.competitions.tab_monitor', '监考大屏'), icon: DesktopOutline },
  { value: 'logs', label: t('admin.competitions.tab_logs', '比赛日志'), icon: ListOutline }
])

const detailData = ref<Partial<AdminCompetitionDetailVO>>({})

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
  requireTeamAudit: false
})

const typeOptions = [
  { label: '理论答题赛', value: 'THEORY' }
]

const statusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '报名中', value: 'REGISTRATION' },
  { label: '进行中', value: 'RUNNING' },
  { label: '已结束', value: 'ENDED' },
  { label: '已归档', value: 'ARCHIVED' }
]

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

const announcements = ref<CompetitionAnnouncementVO[]>([])
const loadingAnnouncements = ref(false)
const showAnnounceModal = ref(false)
const publishingAnnounce = ref(false)
const announceForm = ref({
  title: '',
  content: ''
})

const announcementColumns = computed<DataTableColumns<CompetitionAnnouncementVO>>(() => [
  {
    title: '公告标题',
    key: 'title',
    render(row) {
      return h('span', { class: 'font-semibold text-google-text-primary dark:text-google-text-darkPrimary' }, row.title)
    }
  },
  {
    title: '内容简述',
    key: 'content',
    ellipsis: { tooltip: true },
    render(row) {
      return h('span', { class: 'text-xs text-google-text-secondary dark:text-google-text-darkSecondary' }, row.content)
    }
  },
  {
    title: '发布时间',
    key: 'createdAt',
    width: 160,
    render(row) {
      return h('span', { class: 'text-xs text-google-text-secondary dark:text-google-text-darkSecondary font-mono' }, row.createdAt ? new Date(row.createdAt).toLocaleString() : '-')
    }
  },
  {
    title: t('common.actions', '操作'),
    key: 'actions',
    width: 110,
    align: 'center',
    render(row) {
      return h(
        VBtn,
        {
          clz: 'danger',
          size: 'medium',
          leftIcon: TrashOutline,
          onClick: () => handleDeleteAnnounce(row.id)
        },
        { default: () => '删除' }
      )
    }
  }
])

const teams = ref<AdminTeamListVO[]>([])
const loadingTeams = ref(false)
const teamPage = ref(1)
const teamTotal = ref(0)
const teamStats = ref({ total: 0, pending: 0, approved: 0, rejected: 0 })

const teamPagination = computed(() => ({
  page: teamPage.value,
  pageSize: 10,
  itemCount: teamTotal.value
}))

const teamColumns = computed<DataTableColumns<AdminTeamListVO>>(() => [
  {
    title: '战队名称',
    key: 'teamName',
    render(row) {
      return h('span', { class: 'font-semibold text-google-text-primary dark:text-google-text-darkPrimary' }, row.teamName)
    }
  },
  {
    title: '队长用户名',
    key: 'captainUsername',
    width: 140,
    render(row) {
      return h('span', { class: 'text-xs text-google-text-secondary dark:text-google-text-darkSecondary' }, row.captainUsername)
    }
  },
  {
    title: '成员人数',
    key: 'memberCount',
    width: 100,
    render(row) {
      return h('span', { class: 'text-xs text-google-text-secondary dark:text-google-text-darkSecondary' }, `${row.memberCount} 人`)
    }
  },
  {
    title: '审核状态',
    key: 'status',
    width: 110,
    render(row) {
      const isApproved = row.status === 'AUDIT_PASSED'
      const isPending = row.status === 'PENDING_AUDIT'
      return h('span', {
        class: `px-2 py-0.5 text-xs rounded-full border ${isApproved ? 'bg-green-500/10 text-green-600 dark:text-green-400 border-green-500/20' : isPending ? 'bg-yellow-500/10 text-yellow-600 dark:text-yellow-400 border-yellow-500/20' : 'bg-red-500/10 text-red-600 dark:text-red-400 border-red-500/20'}`
      }, row.status)
    }
  },
  {
    title: t('common.actions', '操作'),
    key: 'actions',
    width: 180,
    align: 'center',
    render(row) {
      return h('div', { class: 'flex items-center justify-center gap-1.5' }, [
        h(
          VBtn,
          {
            clz: 'primary',
            size: 'medium',
            leftIcon: CheckmarkCircleOutline,
            onClick: () => handleAuditTeam(row.id, 'AUDIT_PASSED')
          },
          { default: () => '通过' }
        ),
        h(
          VBtn,
          {
            clz: 'danger',
            size: 'medium',
            leftIcon: CloseCircleOutline,
            onClick: () => handleAuditTeam(row.id, 'AUDIT_REJECTED')
          },
          { default: () => '驳回' }
        )
      ])
    }
  }
])

const compLogs = ref<AdminCompetitionLogVO[]>([])
const loadingLogs = ref(false)
const logPage = ref(1)
const logTotal = ref(0)

const logPagination = computed(() => ({
  page: logPage.value,
  pageSize: 10,
  itemCount: logTotal.value
}))

const logColumns = computed<DataTableColumns<AdminCompetitionLogVO>>(() => [
  {
    title: '操作人',
    key: 'username',
    width: 120,
    render(row) {
      return h('span', { class: 'font-medium text-xs text-google-text-primary dark:text-google-text-darkPrimary' }, row.username || '-')
    }
  },
  {
    title: '战队',
    key: 'teamName',
    width: 130,
    render(row) {
      return h('span', { class: 'text-xs text-google-text-secondary dark:text-google-text-darkSecondary' }, row.teamName || '-')
    }
  },
  {
    title: '动作/事件',
    key: 'action',
    minWidth: 150,
    render(row) {
      return h('span', { class: 'text-xs font-mono' }, row.action)
    }
  },
  {
    title: '状态',
    key: 'status',
    width: 80,
    align: 'center',
    render(row) {
      return h('span', {
        class: `px-1.5 py-0.5 text-[11px] rounded ${row.status ? 'bg-green-500/10 text-green-600 dark:text-green-400' : 'bg-red-500/10 text-red-600 dark:text-red-400'}`
      }, row.status ? '成功' : '失败')
    }
  },
  {
    title: '记录时间',
    key: 'createdAt',
    width: 160,
    render(row) {
      return h('span', { class: 'text-xs text-google-text-secondary dark:text-google-text-darkSecondary font-mono' }, row.createdAt ? new Date(row.createdAt).toLocaleString() : '-')
    }
  }
])

async function fetchDetail() {
  if (!compId.value) return
  loadingDetail.value = true
  try {
    const res = await getAdminCompetitionByIdApi(compId.value)
    if (res.data) {
      detailData.value = res.data
      formData.value = {
        title: res.data.title || '',
        slug: res.data.slug || '',
        type: 'THEORY',
        status: res.data.status || 'DRAFT',
        bannerUrl: res.data.bannerUrl || '',
        description: res.data.description || '',
        startTime: res.data.startTime || '',
        endTime: res.data.endTime || '',
        regStartTime: res.data.regStartTime || '',
        regEndTime: res.data.regEndTime || '',
        requireTeamAudit: Boolean(res.data.requireTeamAudit)
      }
    }
  } catch (err: any) {
    message.error(err.message || t('admin.competitions.fetch_detail_failed', '获取竞赛详情失败'))
    router.push('/admin/competitions')
  } finally {
    loadingDetail.value = false
  }
}

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

async function handleSaveBasic() {
  if (!formData.value.title.trim() || !formData.value.slug.trim()) {
    message.warning(t('admin.competitions.title_slug_required', '竞赛名称与 Slug 为必填项'))
    return
  }
  if (!formData.value.startTime || !formData.value.endTime) {
    message.warning(t('admin.competitions.time_required', '请填写竞赛起止时间'))
    return
  }

  saving.value = true
  try {
    const payload = {
      title: formData.value.title.trim(),
      slug: formData.value.slug.trim(),
      type: 'THEORY' as CompetitionType,
      status: formData.value.status,
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

    if (isEdit.value && compId.value) {
      await updateCompetitionApi(compId.value, payload)
      message.success(t('admin.competitions.saved_success', '竞赛基本信息已更新'))
      await fetchDetail()
    } else {
      const res = await createCompetitionApi(payload)
      message.success(t('admin.competitions.create_success', '竞赛创建成功'))
      if (res.data) {
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

async function fetchAnnouncements() {
  if (!compId.value) return
  loadingAnnouncements.value = true
  try {
    const res = await listAdminCompetitionAnnouncementsApi(compId.value)
    if (res.data) {
      announcements.value = res.data
    }
  } catch {
    announcements.value = []
  } finally {
    loadingAnnouncements.value = false
  }
}

function openCreateAnnounceModal() {
  announceForm.value = { title: '', content: '' }
  showAnnounceModal.value = true
}

async function handlePublishAnnounce() {
  if (!compId.value || !announceForm.value.title.trim() || !announceForm.value.content.trim()) {
    message.warning('请完整填写公告标题与内容')
    return
  }
  publishingAnnounce.value = true
  try {
    await createAdminCompetitionAnnouncementApi(compId.value, {
      title: announceForm.value.title.trim(),
      content: announceForm.value.content.trim()
    })
    message.success('公告发布成功')
    showAnnounceModal.value = false
    await fetchAnnouncements()
  } catch (err: any) {
    message.error(err.message || '发布公告失败')
  } finally {
    publishingAnnounce.value = false
  }
}

async function handleDeleteAnnounce(id: number | string) {
  if (!compId.value) return
  try {
    await deleteAdminCompetitionAnnouncementApi(compId.value, id)
    message.success('公告已删除')
    await fetchAnnouncements()
  } catch (err: any) {
    message.error(err.message || '删除公告失败')
  }
}

async function fetchTeams() {
  if (!compId.value) return
  loadingTeams.value = true
  try {
    const [listRes, statsRes] = await Promise.all([
      listAdminTeamsApi(compId.value, { page: teamPage.value, size: 10 }),
      getAdminTeamStatsApi(compId.value)
    ])
    if (listRes.data) {
      teams.value = listRes.data.list ?? []
      teamTotal.value = Number(listRes.data.total) || 0
    }
    if (statsRes.data) {
      teamStats.value = statsRes.data
    }
  } catch {
    teams.value = []
  } finally {
    loadingTeams.value = false
  }
}

function handleTeamPageChange(page: number) {
  teamPage.value = page
  fetchTeams()
}

async function handleAuditTeam(teamId: number | string, status: any) {
  if (!compId.value) return
  try {
    await auditAdminTeamApi(compId.value, teamId, { status })
    message.success('审核操作成功')
    await fetchTeams()
  } catch (err: any) {
    message.error(err.message || '审核失败')
  }
}

async function fetchLogs() {
  if (!compId.value) return
  loadingLogs.value = true
  try {
    const res = await getAdminCompetitionLogListApi(compId.value, { page: logPage.value, size: 10 })
    if (res.data) {
      compLogs.value = res.data.list ?? []
      logTotal.value = Number(res.data.total) || 0
    }
  } catch {
    compLogs.value = []
  } finally {
    loadingLogs.value = false
  }
}

function handleLogPageChange(page: number) {
  logPage.value = page
  fetchLogs()
}

async function fetchTheoryData() {
  if (!compId.value) return
  try {
    const bankRes = await getAdminTheoryBankListApi({ page: 1, size: 100 })
    if (bankRes.data) {
      availableBanks.value = bankRes.data.list ?? []
    }
    const cfgRes = await getAdminTheoryCompetitionConfigApi(compId.value)
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
  if (!compId.value) return
  if (!theoryConfig.value.bankIds || theoryConfig.value.bankIds.length === 0) {
    message.warning('请选择至少一个引用的理论题库')
    return
  }
  savingTheoryConfig.value = true
  try {
    const res = await saveAdminTheoryCompetitionConfigApi(compId.value, {
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

watch(activeTab, (tab) => {
  if (tab === 'challenges') {
    fetchTheoryData()
  }
})

onMounted(async () => {
  if (isEdit.value) {
    await fetchDetail()
    fetchTheoryData()
    fetchAnnouncements()
    fetchTeams()
    fetchLogs()
  }
})
</script>
