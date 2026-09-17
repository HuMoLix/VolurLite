<template>
  <div class="admin-competition-edit-view">
    <!-- Subview Top Header with Breadcrumb & Back action -->
    <div class="subview-header">
      <div class="header-left">
        <n-button quaternary size="small" class="back-btn" @click="backToList">
          <template #icon>
            <n-icon :component="ArrowBackOutline" />
          </template>
          返回竞赛列表
        </n-button>
        <div class="title-wrap">
          <h2 class="subview-title">
            {{ isEdit ? (competitionDetail?.title ? `${competitionDetail.title} - ${t('admin.competitions.manage_title')}` : t('admin.competitions.manage_title')) : t('admin.competitions.modal_create_title') }}
          </h2>
          <n-tag
            v-if="competitionDetail?.status"
            size="small"
            :type="getStatusTagType(competitionDetail.status)"
            round
          >
            {{ getStatusText(competitionDetail.status) }}
          </n-tag>
        </div>
      </div>
    </div>

    <!-- Main Tabs Layout -->
    <n-card class="main-tabs-card" :bordered="false">
      <n-spin :show="loading">
        <n-tabs
          v-model:value="activeTab"
          type="line"
          size="large"
          class="comp-manage-tabs"
          @update:value="handleTabChange"
        >
          <!-- Tab: 编辑竞赛 (基础信息与赛程) -->
          <n-tab-pane name="basic" :tab="t('admin.competitions.tab_basic')">
            <basic-info-tab
              :comp-id="compId"
              :is-edit="isEdit"
              :initial-data="competitionDetail"
              @saved="handleSaved"
              @refresh="fetchDetail"
            />
          </n-tab-pane>

          <!-- Tab: 比赛公告功能 -->
          <n-tab-pane name="announcements" :tab="t('admin.competitions.tab_announcements')">
            <div v-if="!isEdit" class="tab-disabled-state">
              <n-alert type="warning" title="功能暂未就绪">
                {{ t('admin.competitions.tip_create_first') }}
              </n-alert>
            </div>
            <announce-tab
              v-else
              :comp-id="compId!"
            />
          </n-tab-pane>

          <!-- Tab: 比赛题目管理功能 -->
          <n-tab-pane name="challenges" :tab="t('admin.competitions.tab_challenges')">
            <div v-if="!isEdit" class="tab-disabled-state">
              <n-alert type="warning" title="功能暂未就绪">
                {{ t('admin.competitions.tip_create_first') }}
              </n-alert>
            </div>
            <challenge-tab
              v-else
              :comp-id="compId!"
            />
          </n-tab-pane>

          <!-- Tab: 容器管理功能 -->
          <n-tab-pane name="containers" :tab="t('admin.competitions.tab_containers')">
            <div v-if="!isEdit" class="tab-disabled-state">
              <n-alert type="warning" title="功能暂未就绪">
                {{ t('admin.competitions.tip_create_first') }}
              </n-alert>
            </div>
            <container-tab
              v-else
              :comp-id="compId!"
              :initial-max-containers="competitionDetail?.maxContainersPerTeam ?? competitionDetail?.config?.maxContainersPerTeam ?? 2"
              @update-quota="handleQuotaUpdated"
            />
          </n-tab-pane>

          <!-- Tab: WP 管理 -->
          <n-tab-pane name="writeups" :tab="t('admin.competitions.tab_writeups')">
            <div v-if="!isEdit" class="tab-disabled-state">
              <n-alert type="warning" title="功能暂未就绪">
                {{ t('admin.competitions.tip_create_first') }}
              </n-alert>
            </div>
            <writeup-tab
              v-else
              :comp-id="compId!"
              :initial-data="competitionDetail"
              @refresh="fetchDetail"
            />
          </n-tab-pane>

          <!-- Tab: 队伍审核 -->
          <n-tab-pane name="teams" :tab="t('admin.competitions.tab_teams')">
            <div v-if="!isEdit" class="tab-disabled-state">
              <n-alert type="warning" title="功能暂未就绪">
                {{ t('admin.competitions.tip_create_first') }}
              </n-alert>
            </div>
            <team-audit-tab
              v-else
              :comp-id="compId!"
              :initial-data="competitionDetail"
              @refresh="fetchDetail"
            />
          </n-tab-pane>
        </n-tabs>
      </n-spin>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  NCard,
  NButton,
  NTag,
  NTabs,
  NTabPane,
  NAlert,
  NIcon,
  NSpin,
  useMessage
} from 'naive-ui'
import {
  ArrowBackOutline
} from '@vicons/ionicons5'
import { useI18n } from 'vue-i18n'
import type {
  CompetitionStatus,
  AdminCompetitionDetailVO
} from '@/types/competition'
import {
  getAdminCompetitionByIdApi
} from '@/api/competition'

import BasicInfoTab from './tabs/BasicInfoTab.vue'
import AnnounceTab from './tabs/AnnounceTab.vue'
import ChallengeTab from './tabs/ChallengeTab.vue'
import ContainerTab from './tabs/ContainerTab.vue'
import WriteupTab from './tabs/WriteupTab.vue'
import TeamAuditTab from './tabs/TeamAuditTab.vue'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const message = useMessage()

// State
const loading = ref<boolean>(false)
const competitionDetail = ref<AdminCompetitionDetailVO | null>(null)

const compId = computed(() => route.params.id as string | undefined)
const isEdit = computed(() => !!compId.value)

// Tabs State
const activeTab = ref<string>((route.query.tab as string) || 'basic')

onMounted(() => {
  if (isEdit.value && compId.value) {
    fetchDetail()
  }
})

async function fetchDetail() {
  if (!compId.value) return
  loading.value = true
  try {
    const res = await getAdminCompetitionByIdApi(compId.value)
    competitionDetail.value = res.data
  } catch (error: any) {
    console.error('Fetch competition detail error:', error)
    message.error(error.message || '获取竞赛详情失败')
  } finally {
    loading.value = false
  }
}

function handleTabChange(val: string) {
  activeTab.value = val
  router.replace({
    query: {
      ...route.query,
      tab: val
    }
  })
}

function handleSaved(newId: string | number) {
  if (!isEdit.value) {
    router.replace(`/admin/competitions/${newId}/edit?tab=basic`)
  } else {
    fetchDetail()
  }
}

function handleQuotaUpdated(quota: number) {
  if (competitionDetail.value) {
    competitionDetail.value.maxContainersPerTeam = quota
    if (competitionDetail.value.config) {
      competitionDetail.value.config.maxContainersPerTeam = quota
    }
  }
}

function getStatusTagType(st: CompetitionStatus) {
  switch (st) {
    case 'RUNNING':
      return 'success'
    case 'PUBLISHED':
    case 'REGISTRATION':
      return 'info'
    case 'FROZEN':
      return 'warning'
    case 'DRAFT':
    case 'ENDED':
    case 'ARCHIVED':
    default:
      return 'default'
  }
}

function getStatusText(st: CompetitionStatus): string {
  switch (st) {
    case 'DRAFT': return t('competition.status_draft')
    case 'PUBLISHED': return t('competition.status_published')
    case 'RUNNING': return t('competition.status_running')
    case 'ENDED': return t('competition.status_ended')
    case 'ARCHIVED': return t('competition.status_archived')
    case 'REGISTRATION': return t('competition.status_registration')
    case 'FROZEN': return t('competition.status_frozen')
    default: return st
  }
}

function backToList() {
  router.push('/admin/competitions')
}
</script>

<style scoped lang="scss">
.admin-competition-edit-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding-bottom: 40px;

  .subview-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    padding-bottom: 20px;
    border-bottom: 1px solid var(--v-border-base, #e5e4e7);

    .header-left {
      display: flex;
      flex-direction: column;
      gap: 8px;

      .back-btn {
        align-self: flex-start;
        padding-left: 0;
        margin-bottom: 2px;
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
      }
    }
  }

  .main-tabs-card {
    border-radius: 8px;
    background: var(--v-bg-card, #ffffff);

    .comp-manage-tabs {
      :deep(.n-tabs-nav) {
        border-bottom: 1px solid var(--v-border-base, #e5e4e7);
        margin-bottom: 20px;
      }
    }

    .tab-disabled-state {
      padding: 30px 0;
    }
  }
}
</style>
