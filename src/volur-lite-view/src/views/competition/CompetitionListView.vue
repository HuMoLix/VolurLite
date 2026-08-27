<template>
  <div class="competition-list-view">
    <div class="header-action-bar">
      <div class="page-title">
        <h2>🏆 {{ t('competition.page_title') }}</h2>
        <p class="subtitle">{{ t('competition.page_subtitle') }}</p>
      </div>
    </div>

    <n-grid :x-gap="24" :y-gap="24" cols="1 m:2" responsive="screen">
      <n-grid-item v-for="comp in competitionList" :key="comp.id">
        <n-card hoverable class="competition-card">
          <div class="comp-header">
            <div class="comp-badge">
              <n-tag :type="getStatusType(comp.status)" round size="small">
                {{ getStatusText(comp.status) }}
              </n-tag>
            </div>
            <h3 class="comp-title">{{ comp.title }}</h3>
          </div>

          <p class="comp-desc">{{ comp.description }}</p>

          <div class="comp-meta">
            <div class="meta-row">
              <span class="meta-label">{{ t('competition.start_time') }}:</span>
              <span class="meta-val">{{ comp.startTime }}</span>
            </div>
            <div class="meta-row">
              <span class="meta-label">{{ t('competition.end_time') }}:</span>
              <span class="meta-val">{{ comp.endTime }}</span>
            </div>
            <div class="meta-row">
              <span class="meta-label">{{ t('competition.scale') }}:</span>
              <span class="meta-val">{{ t('competition.scale_format', { teamCount: comp.teamCount, participantCount: comp.participantCount }) }}</span>
            </div>
          </div>

          <template #action>
            <n-button type="primary" block @click="viewCompetition(comp.id)">
              {{ t('competition.enter_lobby') }}
            </n-button>
          </template>
        </n-card>
      </n-grid-item>
    </n-grid>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { NGrid, NGridItem, NCard, NTag, NButton, useMessage } from 'naive-ui'
import { useI18n } from 'vue-i18n'
import type { CompetitionSummaryVO } from '@/types/competition'
import { getCompetitionListApi } from '@/api/competition'

const { t } = useI18n()
const message = useMessage()
const competitionList = ref<CompetitionSummaryVO[]>([])

function getStatusType(status: string): 'warning' | 'success' | 'default' {
  switch (status) {
    case 'PENDING':
      return 'warning'
    case 'RUNNING':
      return 'success'
    default:
      return 'default'
  }
}

function getStatusText(status: string): string {
  switch (status) {
    case 'PENDING':
      return t('competition.status_pending')
    case 'RUNNING':
      return t('competition.status_running')
    case 'ENDED':
      return t('competition.status_ended')
    default:
      return status
  }
}

async function fetchData() {
  try {
    const res = await getCompetitionListApi({ page: 1, size: 10 })
    competitionList.value = res.data.list
  } catch {
    competitionList.value = [
      {
        id: 1,
        title: '2026 全国网络安全高校精英挑战赛',
        description: '涵盖 CTF 解题赛与 AWD+ 攻防对抗阶段，全面考核选手实战应急与攻防对抗能力。',
        status: 'RUNNING',
        startTime: '2026-08-26 09:00:00',
        endTime: '2026-08-27 18:00:00',
        teamCount: 64,
        participantCount: 192
      },
      {
        id: 2,
        title: 'Volur Lite 夏季全维实战演练赛',
        description: '包含理论测试、CTF、AWD 与 ISW 靶场实战全阶段综合竞技演练。',
        status: 'PENDING',
        startTime: '2026-09-01 10:00:00',
        endTime: '2026-09-02 20:00:00',
        teamCount: 32,
        participantCount: 96
      }
    ]
  }
}

function viewCompetition(id: number | string) {
  message.info(t('competition.enter_competition_msg', { id }))
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="scss">
.competition-list-view {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.header-action-bar {
  .page-title {
    h2 {
      font-size: 24px;
      font-weight: 700;
      margin-bottom: 4px;
    }
    .subtitle {
      font-size: 13px;
      opacity: 0.6;
    }
  }
}

.competition-card {
  border-radius: 16px;

  .comp-header {
    margin-bottom: 12px;

    .comp-badge {
      margin-bottom: 8px;
    }

    .comp-title {
      font-size: 18px;
      font-weight: 700;
    }
  }

  .comp-desc {
    font-size: 14px;
    opacity: 0.8;
    line-height: 1.6;
    margin-bottom: 16px;
    min-height: 44px;
  }

  .comp-meta {
    background: rgba(0, 0, 0, 0.05);
    padding: 12px;
    border-radius: 8px;
    display: flex;
    flex-direction: column;
    gap: 6px;
    font-size: 13px;

    .meta-row {
      display: flex;
      justify-content: space-between;

      .meta-label {
        opacity: 0.6;
      }

      .meta-val {
        font-weight: 500;
      }
    }
  }
}
</style>
