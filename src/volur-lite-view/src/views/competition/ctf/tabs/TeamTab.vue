<template>
  <div class="team-tab">
    <n-spin :show="teamLoading">
      <div v-if="myTeam" class="team-workspace">
        <n-card :bordered="false" class="team-summary-card">
          <div class="team-header-row">
            <div class="team-left">
              <AppAvatar
                round
                :size="64"
                :src="myTeam.avatarUrl"
                :name="myTeam.teamName"
              />
              <div class="team-titles">
                <h2>{{ myTeam.teamName }}</h2>
              </div>
            </div>

            <div class="team-right">
              <n-tag type="success" round size="medium">
                {{ myTeam.status === 'AUDIT_PASSED' ? '已核准参赛' : myTeam.status }}
              </n-tag>
            </div>
          </div>
        </n-card>

        <!-- Members List -->
        <n-card :bordered="false" title="战队成员阵容" class="members-card">
          <n-data-table
            :columns="memberColumns"
            :data="myTeam.members || []"
            :bordered="false"
          />
        </n-card>
      </div>

      <div v-else class="empty-wrap">
        <n-empty description="未查询到您的参赛战队记录" />
      </div>
    </n-spin>
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: 'TeamTab'
})

import { computed, watch, h } from 'vue'
import {
  NCard,
  NTag,
  NEmpty,
  NSpin,
  NDataTable,
  type DataTableColumns
} from 'naive-ui'
import AppAvatar from '@/components/common/AppAvatar.vue'
import { useCompetitionStore } from '@/store/modules/competition'

interface Props {
  competitionId?: number | string | null
}

const props = withDefaults(defineProps<Props>(), {
  competitionId: null
})

const competitionStore = useCompetitionStore()
const currentCompId = computed(() => props.competitionId ?? competitionStore.currentCompetition?.id)

const teamLoading = computed(() => competitionStore.teamLoading)
const myTeam = computed(() => competitionStore.myTeam)

const memberColumns: DataTableColumns<any> = [
  {
    title: '用户',
    key: 'username',
    render(row) {
      return h('span', { style: 'font-weight: 600;' }, row.username)
    }
  },
  {
    title: '战队角色',
    key: 'role',
    width: 120,
    render(row) {
      return h(NTag, { size: 'small', type: row.role === 'CAPTAIN' ? 'primary' : 'default', round: true }, {
        default: () => (row.role === 'CAPTAIN' ? '队长' : '队员')
      })
    }
  },
  {
    title: '状态',
    key: 'status',
    width: 120,
    render: (row) => row.status || '已加入'
  }
]

async function fetchMyTeam(force = false) {
  if (!currentCompId.value) return
  try {
    await competitionStore.fetchMyTeam(currentCompId.value, force)
  } catch {
    // not in team
  }
}

watch(
  () => currentCompId.value,
  (newId) => {
    if (newId) {
      fetchMyTeam()
    }
  },
  { immediate: true }
)

defineExpose({
  fetchMyTeam
})
</script>

<style scoped lang="scss">
.team-tab {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.team-workspace {
  display: flex;
  flex-direction: column;
  gap: 20px;

  .team-summary-card {
    border-radius: 12px;

    .team-header-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 16px;

      .team-left {
        display: flex;
        align-items: center;
        gap: 16px;

        .team-titles {
          h2 {
            font-size: 22px;
            font-weight: 700;
            margin: 0 0 4px;
          }
        }
      }

      .team-right {
        display: flex;
        align-items: center;
        gap: 12px;
      }
    }
  }

  .members-card {
    border-radius: 12px;
  }
}

.empty-wrap {
  padding: 60px 0;
  display: flex;
  justify-content: center;
}
</style>
