<template>
  <div class="space-y-6 max-w-6xl mx-auto">
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h2 class="text-xl font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
          {{ isEdit ? (detailData.title || t('admin.competitions.manage_title', 'CTF 竞赛运维')) : t('admin.competitions.modal_create_title', '创建新 CTF 竞赛') }}
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
                开启参赛战队资质审核 (队长报名后需管理员通过)
              </label>
            </div>
          </div>
        </div>
      </div>

      <div v-if="activeTab === 'challenges' && isEdit" class="space-y-4">
        <div class="flex items-center justify-between">
          <h3 class="text-sm font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
            {{ t('admin.competitions.mounted_challenges', '已挂载赛题列表') }} ({{ mountedChallenges.length }})
          </h3>
          <VBtn
            clz="primary"
            size="medium"
            :left-icon="AddOutline"
            @click="openMountModal"
          >
            {{ t('admin.competitions.mount_challenge_btn', '挂载题目') }}
          </VBtn>
        </div>

        <VTable
          :columns="mountedChallengeColumns"
          :data="mountedChallenges"
          :loading="loadingChallenges"
          :pagination="false"
          size="small"
          bordered
        />
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

      <div v-if="activeTab === 'writeups' && isEdit" class="space-y-4">
        <div class="flex items-center justify-between">
          <h3 class="text-sm font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
            {{ t('admin.competitions.writeups_list', '参赛队伍题解 (Writeup) 提交记录') }}
          </h3>
        </div>

        <VTable
          :columns="writeupColumns"
          :data="writeups"
          :loading="loadingWriteups"
          :pagination="false"
          size="small"
          bordered
        />
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
      v-model:show="showMountModal"
      :title="t('admin.competitions.mount_challenge_title', '挂载题库题目到竞赛')"
      :width="540"
    >
      <div class="space-y-4">
        <div>
          <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
            选择题库赛题 *
          </label>
          <VSelection
            v-model:value="mountForm.challengeId"
            :options="repoChallengeOptions"
            placeholder="请选择题库中的赛题"
          />
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
              初始分数 (Pts)
            </label>
            <VInput
              v-model:value="mountForm.initialPoints"
              placeholder="1000"
              clearable
            />
          </div>
          <div>
            <label class="block text-xs font-medium text-google-text-secondary dark:text-google-text-darkSecondary mb-1.5">
              衰减底分 (Min)
            </label>
            <VInput
              v-model:value="mountForm.minPoints"
              placeholder="100"
              clearable
            />
          </div>
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-3">
          <VBtn clz="normal" size="medium" @click="showMountModal = false">
            {{ t('common.cancel', '取消') }}
          </VBtn>
          <VBtn clz="primary" size="medium" :loading="mounting" @click="handleConfirmMount">
            {{ t('admin.competitions.confirm_mount', '确认挂载') }}
          </VBtn>
        </div>
      </template>
    </VModal>

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
import { useMessage, useDialog, type DataTableColumns } from 'naive-ui'
import {
  SaveOutline,
  AddOutline,
  TrashOutline,
  CheckmarkCircleOutline,
  CloseCircleOutline,
  InformationCircleOutline,
  FlagOutline,
  MegaphoneOutline,
  PeopleOutline,
  DocumentAttachOutline,
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
import type { FilterOption } from '@/components/common/types'
import type {
  CompetitionType,
  CompetitionStatus,
  AdminCompetitionDetailVO,
  CtfCompetitionChallengeVO,
  CompetitionAnnouncementVO,
  AdminTeamListVO,
  CompetitionWriteupVO,
  AdminCompetitionLogVO
} from '@/types/competition'
import {
  getAdminCompetitionByIdApi,
  createCompetitionApi,
  updateCompetitionApi,
  listMountedCtfChallengesApi,
  mountCtfChallengeApi,
  unmountCtfChallengeApi,
  listAdminCompetitionAnnouncementsApi,
  createAdminCompetitionAnnouncementApi,
  deleteAdminCompetitionAnnouncementApi,
  listAdminTeamsApi,
  getAdminTeamStatsApi,
  auditAdminTeamApi,
  listAdminCompetitionWriteupsApi,
  getAdminCompetitionLogListApi
} from '@/api/competition'
import { getAdminCtfChallengeListApi } from '@/api/challenge'
import type { CtfChallengeListVO } from '@/types/challenge'

defineOptions({
  name: 'AdminCtfCompetitionEditView'
})

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const message = useMessage()
const dialog = useDialog()

const compId = computed(() => route.params.id as string | undefined)
const isEdit = computed(() => Boolean(compId.value))

const activeTab = ref('basic')
const loadingDetail = ref(false)
const saving = ref(false)

const tabs = computed<FilterOption<string>[]>(() => [
  { value: 'basic', label: t('admin.competitions.tab_basic', '基本信息与排程'), icon: InformationCircleOutline },
  { value: 'challenges', label: t('admin.competitions.tab_challenges', '赛题管理'), icon: FlagOutline },
  { value: 'announcements', label: t('admin.competitions.tab_announcements', '比赛公告'), icon: MegaphoneOutline },
  { value: 'teams', label: t('admin.competitions.tab_teams', '战队审核'), icon: PeopleOutline },
  { value: 'writeups', label: t('admin.competitions.tab_writeups', '题解 WP'), icon: DocumentAttachOutline },
  { value: 'logs', label: t('admin.competitions.tab_logs', '比赛日志'), icon: ListOutline }
])

const detailData = ref<Partial<AdminCompetitionDetailVO>>({})

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

const typeOptions = [
  { label: 'CTF 解题赛', value: 'CTF' }
]

const statusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '报名中', value: 'REGISTRATION' },
  { label: '进行中', value: 'RUNNING' },
  { label: '已结束', value: 'ENDED' },
  { label: '已归档', value: 'ARCHIVED' }
]

const mountedChallenges = ref<CtfCompetitionChallengeVO[]>([])
const loadingChallenges = ref(false)
const showMountModal = ref(false)
const repoChallenges = ref<CtfChallengeListVO[]>([])
const mounting = ref(false)
const mountForm = ref({
  challengeId: '',
  initialPoints: '1000',
  minPoints: '100'
})

const repoChallengeOptions = computed(() => {
  return repoChallenges.value.map(c => ({
    label: `[${c.categoryName || 'Misc'}] ${c.name}`,
    value: String(c.id)
  }))
})

const mountedChallengeColumns = computed<DataTableColumns<CtfCompetitionChallengeVO>>(() => [
  {
    title: t('admin.challenges.name', '题目名称'),
    key: 'title',
    render(row) {
      return h('span', { class: 'font-semibold text-google-text-primary dark:text-google-text-darkPrimary' }, row.title || row.name || '-')
    }
  },
  {
    title: t('admin.challenges.category', '方向分类'),
    key: 'category',
    width: 120,
    render(row) {
      return h('span', { class: 'px-2 py-0.5 text-xs rounded-full bg-google-blue/10 text-google-blue dark:text-google-blue-dark' }, row.category || row.categoryName || '-')
    }
  },
  {
    title: '当前分值',
    key: 'currentPoints',
    width: 100,
    render(row) {
      return h('span', { class: 'font-mono text-xs font-semibold' }, `${row.currentPoints} Pts`)
    }
  },
  {
    title: '解题次数',
    key: 'solvesCount',
    width: 90,
    render(row) {
      return h('span', { class: 'text-xs text-google-text-secondary dark:text-google-text-darkSecondary' }, `${row.solvesCount || 0} 次`)
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
          onClick: () => handleUnmountChallenge(row.id)
        },
        { default: () => '卸载' }
      )
    }
  }
])

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

const writeups = ref<CompetitionWriteupVO[]>([])
const loadingWriteups = ref(false)

const writeupColumns = computed<DataTableColumns<CompetitionWriteupVO>>(() => [
  {
    title: '战队名称',
    key: 'teamName',
    width: 160,
    render(row) {
      return h('span', { class: 'font-semibold text-google-text-primary dark:text-google-text-darkPrimary' }, row.teamName)
    }
  },
  {
    title: '提交人',
    key: 'username',
    width: 120,
    render(row) {
      return h('span', { class: 'text-xs text-google-text-secondary dark:text-google-text-darkSecondary' }, row.username)
    }
  },
  {
    title: '文件名称',
    key: 'fileName',
    render(row) {
      return row.fileUrl
        ? h('a', { href: row.fileUrl, target: '_blank', class: 'text-xs text-google-blue dark:text-google-blue-dark hover:underline truncate' }, row.fileName)
        : h('span', { class: 'text-xs' }, row.fileName)
    }
  },
  {
    title: '评阅状态',
    key: 'status',
    width: 110,
    render(row) {
      return h('span', { class: 'px-2 py-0.5 text-xs rounded-full bg-gray-500/10 text-gray-600 dark:text-gray-400' }, row.status || 'SUBMITTED')
    }
  },
  {
    title: '得分',
    key: 'score',
    width: 90,
    render(row) {
      return h('span', { class: 'font-mono text-xs font-semibold' }, row.score !== null && row.score !== undefined ? `${row.score} 分` : '-')
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
        type: 'CTF',
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
      type: 'CTF' as CompetitionType,
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
        router.push(`/admin/competitions/ctf/${res.data}/edit`)
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

async function fetchMountedChallenges() {
  if (!compId.value) return
  loadingChallenges.value = true
  try {
    const res = await listMountedCtfChallengesApi(compId.value)
    if (res.data) {
      mountedChallenges.value = res.data
    }
  } catch {
    mountedChallenges.value = []
  } finally {
    loadingChallenges.value = false
  }
}

async function openMountModal() {
  showMountModal.value = true
  try {
    const res = await getAdminCtfChallengeListApi({ page: 1, size: 100 })
    if (res.data) {
      repoChallenges.value = res.data.list ?? []
    }
  } catch {
    repoChallenges.value = []
  }
}

async function handleConfirmMount() {
  if (!compId.value || !mountForm.value.challengeId) {
    message.warning('请选择赛题')
    return
  }
  mounting.value = true
  try {
    await mountCtfChallengeApi(compId.value, {
      challengeId: mountForm.value.challengeId,
      initialPoints: parseInt(mountForm.value.initialPoints, 10) || 1000,
      minPoints: parseInt(mountForm.value.minPoints, 10) || 100
    })
    message.success('赛题挂载成功')
    showMountModal.value = false
    await fetchMountedChallenges()
  } catch (err: any) {
    message.error(err.message || '挂载赛题失败')
  } finally {
    mounting.value = false
  }
}

async function handleUnmountChallenge(id: number | string) {
  if (!compId.value) return
  dialog.warning({
    title: '确认卸载赛题',
    content: '卸载后该题目将不再向参赛选手展示，是否继续？',
    positiveText: '确认卸载',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await unmountCtfChallengeApi(compId.value!, id)
        message.success('赛题已卸载')
        await fetchMountedChallenges()
      } catch (err: any) {
        message.error(err.message || '卸载赛题失败')
      }
    }
  })
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

async function fetchWriteups() {
  if (!compId.value) return
  loadingWriteups.value = true
  try {
    const res = await listAdminCompetitionWriteupsApi(compId.value)
    if (res.data) {
      writeups.value = res.data
    }
  } catch {
    writeups.value = []
  } finally {
    loadingWriteups.value = false
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

watch(activeTab, (tab) => {
  if (tab === 'challenges') {
    fetchMountedChallenges()
  }
})

onMounted(async () => {
  if (isEdit.value) {
    await fetchDetail()
    fetchMountedChallenges()
    fetchAnnouncements()
    fetchTeams()
    fetchWriteups()
    fetchLogs()
  }
})
</script>
