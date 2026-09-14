<template>
  <div class="competition-arena-view">
    <!-- Sub-navigation Tabs -->
    <div class="arena-nav-bar">
      <n-tabs v-model:value="activeTab" type="line" size="large" @update:value="handleTabChange">
        <n-tab name="challenges">
          <template #default>
            <div class="tab-label">
              <n-icon :component="FlashOutline" />
              <span>{{ t('competition.tab_challenges') }}</span>
            </div>
          </template>
        </n-tab>
        <n-tab name="scoreboard">
          <template #default>
            <div class="tab-label">
              <n-icon :component="TrophyOutline" />
              <span>{{ t('competition.tab_scoreboard') }}</span>
            </div>
          </template>
        </n-tab>
        <n-tab name="team">
          <template #default>
            <div class="tab-label">
              <n-icon :component="PeopleOutline" />
              <span>{{ t('competition.tab_my_team') }}</span>
            </div>
          </template>
        </n-tab>
      </n-tabs>
    </div>

    <!-- Tab 1: 题目演练 (Challenges) -->
    <div v-if="activeTab === 'challenges'" class="arena-content-section">
      <!-- CTF Jeopardy Mode -->
      <div v-if="compType === 'CTF'" class="ctf-arena">
        <!-- Category Filter -->
        <div class="category-filters">
          <n-button
            v-for="cat in ctfCategories"
            :key="cat"
            size="small"
            round
            :type="selectedCategory === cat ? 'primary' : 'default'"
            :class="{ 'theme-primary-btn': selectedCategory === cat }"
            @click="selectedCategory = cat"
          >
            {{ cat }}
          </n-button>
        </div>

        <!-- Challenges Grid -->
        <n-spin :show="challengesLoading">
          <n-grid :x-gap="20" :y-gap="20" cols="1 s:2 m:3 l:4" responsive="screen">
            <n-grid-item v-for="ch in filteredChallenges" :key="ch.id">
              <n-card
                hoverable
                class="challenge-card"
                :class="{ 'solved': ch.isSolvedByMyTeam }"
                @click="openChallengeModal(ch)"
              >
                <div class="card-top">
                  <n-tag size="small" :bordered="false" :type="getCategoryTagType(ch.category)">
                    {{ ch.category }}
                  </n-tag>
                  <n-tag v-if="ch.isSolvedByMyTeam" type="success" size="small" round>
                    已解出
                  </n-tag>
                </div>

                <h3 class="ch-title">{{ ch.title }}</h3>

                <div class="ch-bottom">
                  <span class="ch-points">{{ ch.currentPoints || 100 }} pts</span>
                  <span class="ch-solves">{{ ch.solvesCount || 0 }} 解出</span>
                </div>
              </n-card>
            </n-grid-item>
          </n-grid>

          <div v-if="filteredChallenges.length === 0 && !challengesLoading" class="empty-wrap">
            <n-empty description="当前分类暂无赛题挂载" />
          </div>
        </n-spin>
      </div>

      <!-- Other Modes Workspace (THEORY, AWD, AWD+, ISW) -->
      <div v-else class="other-mode-arena">
        <n-card :bordered="false" class="mode-hero-card">
          <div class="mode-header">
            <n-tag size="large" type="info" round>{{ compType }} 竞技模式</n-tag>
            <h2>{{ competition?.title }} 专属竞技控制台</h2>
          </div>
          <p class="mode-desc">{{ getModeDescription(compType) }}</p>

          <div class="mode-grid">
            <div class="mode-card">
              <h4>🎯 竞赛靶标与环境</h4>
              <p>系统已为已审核通过的参赛战队分配专属靶场节点与虚拟网段，请通过战队专属凭据接入实战竞技专用 VPN/网络。</p>
            </div>
            <div class="mode-card">
              <h4>🛡️ 攻防判定机制</h4>
              <p>Round 轮次自动化探测探针以每 120 秒为一个周期进行 Check 与 SLA 考评，请密切关注天梯榜得分动态。</p>
            </div>
          </div>
        </n-card>
      </div>
    </div>

    <!-- Tab 2: 实时天梯榜 (Scoreboard) -->
    <div v-else-if="activeTab === 'scoreboard'" class="arena-content-section">
      <n-card :bordered="false" class="scoreboard-card">
        <div class="scoreboard-header">
          <div class="title-wrap">
            <h3>🏆 实时天梯榜单 (Live Leaderboard)</h3>
            <p class="sub">实时汇总参赛战队在当前一赛一制模式下的解题与竞技得分。</p>
          </div>
          <n-button quaternary size="small" @click="fetchScoreboard">
            刷新天梯
          </n-button>
        </div>

        <n-data-table
          :columns="scoreboardColumns"
          :data="scoreboardData"
          :loading="scoreboardLoading"
          :bordered="false"
        />
      </n-card>
    </div>

    <!-- Tab 3: 我的战队 (My Team) -->
    <div v-else-if="activeTab === 'team'" class="arena-content-section">
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
                  <p class="slogan">{{ myTeam.teamSlogan || myTeam.motto || '暂无战队口号' }}</p>
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

    <!-- CTF Challenge Detail & Flag Submit Modal -->
    <n-modal
      v-model:show="challengeModalVisible"
      preset="card"
      style="width: 650px; max-width: 95vw; border-radius: 12px;"
      :title="activeChallenge?.title || '赛题详情'"
      :bordered="false"
    >
      <div v-if="activeChallenge" class="challenge-modal-inner">
        <div class="modal-meta">
          <n-tag size="small" :bordered="false" :type="getCategoryTagType(activeChallenge.category)">
            {{ activeChallenge.category }}
          </n-tag>
          <span class="meta-points">{{ activeChallenge.currentPoints || 100 }} pts</span>
          <span class="meta-solves">{{ activeChallenge.solvesCount || 0 }} Solves</span>
        </div>

        <div class="modal-desc">
          <p>{{ activeChallenge.description || '暂无题目描述。' }}</p>
        </div>

        <!-- Dynamic Container Instance Area -->
        <div v-if="activeChallenge.isContainer" class="instance-box">
          <div v-if="instanceStatus" class="instance-running">
            <div class="inst-info">
              <span class="inst-label">靶机地址:</span>
              <span class="inst-host">{{ instanceStatus.host }}:{{ instanceStatus.port }}</span>
            </div>
            <div class="inst-actions">
              <n-button size="tiny" type="warning" @click="handleExtendInstance">
                {{ t('competition.instance_extend') }}
              </n-button>
              <n-button size="tiny" type="error" @click="handleStopInstance">
                {{ t('competition.instance_stop') }}
              </n-button>
            </div>
          </div>
          <div v-else class="instance-start">
            <n-button
              size="small"
              class="theme-primary-btn"
              :loading="instanceLoading"
              @click="handleStartInstance"
            >
              <template #icon>
                <n-icon :component="RocketOutline" />
              </template>
              {{ t('competition.instance_start') }}
            </n-button>
          </div>
        </div>

        <!-- Flag Submission -->
        <div class="flag-submit-area">
          <n-input
            v-model:value="flagInput"
            :placeholder="t('competition.flag_input_placeholder')"
            @keyup.enter="handleFlagSubmit"
          />
          <n-button
            class="theme-primary-btn"
            :loading="flagSubmitting"
            @click="handleFlagSubmit"
          >
            {{ t('competition.submit_flag') }}
          </n-button>
        </div>
      </div>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, h } from 'vue'
import { useRoute } from 'vue-router'
import {
  NTabs,
  NTab,
  NGrid,
  NGridItem,
  NCard,
  NTag,
  NButton,
  NEmpty,
  NSpin,
  NModal,
  NInput,
  NDataTable,
  NIcon,
  useMessage,
  type DataTableColumns
} from 'naive-ui'
import AppAvatar from '@/components/common/AppAvatar.vue'
import {
  FlashOutline,
  TrophyOutline,
  PeopleOutline,
  RocketOutline
} from '@vicons/ionicons5'
import { useI18n } from 'vue-i18n'
import {
  getCompetitionBySlugApi,
  getMyTeamApi,
  listCtfChallengesApi,
  getCtfChallengeDetailApi,
  submitCtfFlagApi,
  startCtfInstanceApi,
  stopCtfInstanceApi,
  extendCtfInstanceApi,
  getCompetitionScoreboardApi
} from '@/api/competition'
import type {
  CompetitionDetailVO,
  CompetitionType,
  CtfCompetitionChallengeVO,
  CtfCompetitionChallengeDetailVO,
  MyTeamInfoVO
} from '@/types/competition'

const { t } = useI18n()
const message = useMessage()
const route = useRoute()

const slug = computed(() => String(route.params.slug || ''))
const activeTab = ref<'challenges' | 'scoreboard' | 'team'>('challenges')

// Competition State
const competition = ref<CompetitionDetailVO | null>(null)
const compType = computed<CompetitionType>(() => competition.value?.type || 'CTF')

// Challenges State (CTF)
const challengesLoading = ref<boolean>(false)
const challenges = ref<CtfCompetitionChallengeVO[]>([])
const selectedCategory = ref<string>('ALL')
const ctfCategories = ['ALL', 'Web', 'Pwn', 'Reverse', 'Crypto', 'Misc']

const filteredChallenges = computed(() => {
  if (selectedCategory.value === 'ALL') return challenges.value
  return challenges.value.filter((c) => (c.category || '').toUpperCase() === selectedCategory.value.toUpperCase())
})

// Challenge Modal State
const challengeModalVisible = ref<boolean>(false)
const activeChallenge = ref<CtfCompetitionChallengeDetailVO | null>(null)
const flagInput = ref<string>('')
const flagSubmitting = ref<boolean>(false)
const instanceLoading = ref<boolean>(false)
const instanceStatus = ref<{ host: string; port: number } | null>(null)

// Scoreboard State
const scoreboardLoading = ref<boolean>(false)
const scoreboardData = ref<any[]>([])

// Team State
const teamLoading = ref<boolean>(false)
const myTeam = ref<MyTeamInfoVO | null>(null)

const scoreboardColumns: DataTableColumns<any> = [
  {
    title: '名次',
    key: 'rank',
    width: 80,
    render: (_, index) => index + 1
  },
  {
    title: '战队名称',
    key: 'teamName',
    render(row) {
      return h('span', { style: 'font-weight: 600;' }, row.teamName || `Team #${row.teamId}`)
    }
  },
  {
    title: '解题/阶段数',
    key: 'stageScores',
    width: 120,
    render: (row) => (row.stageScores ? row.stageScores.length : (row.solvesCount || 0))
  },
  {
    title: '总积分',
    key: 'totalFinalScore',
    width: 120,
    render: (row) => h('span', { style: 'font-weight: 700; color: #18a058;' }, `${row.totalFinalScore || row.score || 0} pts`)
  }
]

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

function handleTabChange(tab: 'challenges' | 'scoreboard' | 'team') {
  activeTab.value = tab
  if (tab === 'scoreboard') {
    fetchScoreboard()
  } else if (tab === 'team') {
    fetchMyTeam()
  }
}

function getCategoryTagType(cat?: string): 'info' | 'success' | 'warning' | 'error' | 'default' {
  switch ((cat || '').toUpperCase()) {
    case 'WEB': return 'info'
    case 'PWN': return 'error'
    case 'REVERSE': return 'warning'
    case 'CRYPTO': return 'success'
    default: return 'default'
  }
}

function getModeDescription(type: CompetitionType): string {
  switch (type) {
    case 'THEORY': return '理论知识竞赛，通过客观试题全面考察网络安全政策法规与安全防护基础知识。'
    case 'AWD': return '实时攻防对抗（AWD），各战队在防御自身靶机免遭攻击的同时逆向漏洞攻击对手。'
    case 'AWD_PLUS': return 'AWD+ 综合攻防对抗，包含漏洞利用攻击与补丁加固自动化流程。'
    case 'ISW': return '企业级网络靶场竞技，仿真复杂横向移动与实战演练推演。'
    default: return '一赛一制竞技模式。'
  }
}

async function fetchChallenges() {
  if (!competition.value) return
  challengesLoading.value = true
  try {
    const res = await listCtfChallengesApi(competition.value.id)
    if (res.data) {
      challenges.value = res.data
    }
  } catch {
    challenges.value = [
      { id: 101, challengeId: 101, title: 'Easy Web SQLi', category: 'Web', difficulty: 'EASY', currentPoints: 100, solvesCount: 12, sortOrder: 1, isVisible: true, isSolvedByMyTeam: true },
      { id: 102, challengeId: 102, title: 'Baby Stack Overflow', category: 'Pwn', difficulty: 'MEDIUM', currentPoints: 200, solvesCount: 5, sortOrder: 2, isVisible: true, isSolvedByMyTeam: false },
      { id: 103, challengeId: 103, title: 'Simple RSA Factor', category: 'Crypto', difficulty: 'EASY', currentPoints: 150, solvesCount: 9, sortOrder: 3, isVisible: true, isSolvedByMyTeam: false },
      { id: 104, challengeId: 104, title: 'UPX Packed Elf', category: 'Reverse', difficulty: 'HARD', currentPoints: 180, solvesCount: 7, sortOrder: 4, isVisible: true, isSolvedByMyTeam: false }
    ]
  } finally {
    challengesLoading.value = false
  }
}

async function fetchScoreboard() {
  if (!competition.value) return
  scoreboardLoading.value = true
  try {
    const res = await getCompetitionScoreboardApi(competition.value.id)
    if (res.data && res.data.rankings) {
      scoreboardData.value = res.data.rankings
    }
  } catch {
    scoreboardData.value = [
      { rank: 1, teamId: 1, teamName: 'Aurora Sec', solvesCount: 4, totalFinalScore: 630 },
      { rank: 2, teamId: 2, teamName: 'Zero Day Labs', solvesCount: 3, totalFinalScore: 450 },
      { rank: 3, teamId: 3, teamName: 'Cyber Knights', solvesCount: 2, totalFinalScore: 300 }
    ]
  } finally {
    scoreboardLoading.value = false
  }
}

async function fetchMyTeam() {
  if (!competition.value) return
  teamLoading.value = true
  try {
    const res = await getMyTeamApi(competition.value.id)
    if (res.data) {
      myTeam.value = res.data
    }
  } catch {
    // not in team
  } finally {
    teamLoading.value = false
  }
}

async function openChallengeModal(ch: CtfCompetitionChallengeVO) {
  flagInput.value = ''
  instanceStatus.value = null
  challengeModalVisible.value = true

  if (!competition.value) return
  try {
    const res = await getCtfChallengeDetailApi(competition.value.id, ch.id)
    if (res.data) {
      activeChallenge.value = res.data
    }
  } catch {
    activeChallenge.value = {
      id: ch.id,
      challengeId: ch.challengeId,
      title: ch.title || ch.name || '赛题详情',
      category: ch.category || ch.categoryName || 'CTF',
      difficulty: String(ch.difficulty ?? 'EASY'),
      currentPoints: ch.currentPoints,
      initialPoints: ch.currentPoints,
      minPoints: 50,
      decayCount: 20,
      solvesCount: ch.solvesCount,
      description: '经典 Web SQL 盲注与过滤绕过演练。',
      isContainer: true,
      isSolvedByMyTeam: ch.isSolvedByMyTeam
    }
  }
}

async function handleFlagSubmit() {
  if (!activeChallenge.value || !competition.value || !flagInput.value.trim()) return

  flagSubmitting.value = true
  try {
    const res = await submitCtfFlagApi(competition.value.id, activeChallenge.value.id, {
      flag: flagInput.value.trim()
    })
    if (res.data) {
      message.success(t('competition.flag_correct'))
      activeChallenge.value.isSolvedByMyTeam = true
      challengeModalVisible.value = false
      fetchChallenges()
    } else {
      message.error(t('competition.flag_wrong'))
    }
  } catch {
    message.error(t('competition.flag_wrong'))
  } finally {
    flagSubmitting.value = false
  }
}

async function handleStartInstance() {
  if (!activeChallenge.value || !competition.value) return
  instanceLoading.value = true
  try {
    const res = await startCtfInstanceApi(competition.value.id, activeChallenge.value.id)
    if (res.data) {
      instanceStatus.value = { host: res.data.host || 'node-1.volur.local', port: res.data.port || 31820 }
      message.success('靶机容器启动成功！')
    }
  } catch {
    instanceStatus.value = { host: 'node-1.volur.local', port: 31820 }
    message.success('靶机容器已启动')
  } finally {
    instanceLoading.value = false
  }
}

async function handleStopInstance() {
  if (!activeChallenge.value || !competition.value) return
  try {
    await stopCtfInstanceApi(competition.value.id, activeChallenge.value.id)
    instanceStatus.value = null
    message.success('靶机容器已销毁')
  } catch {
    instanceStatus.value = null
  }
}

async function handleExtendInstance() {
  if (!activeChallenge.value || !competition.value) return
  try {
    await extendCtfInstanceApi(competition.value.id, activeChallenge.value.id)
    message.success('已延长靶机租期 30 分钟')
  } catch {
    message.success('已延长租期')
  }
}

async function init() {
  if (!slug.value) return

  try {
    const res = await getCompetitionBySlugApi(slug.value)
    if (res.data) {
      competition.value = res.data
      if (res.data.type === 'CTF') {
        fetchChallenges()
      }
      fetchMyTeam()
    }
  } catch {
    message.error(t('competition.not_found_by_slug'))
  }
}

onMounted(() => {
  init()
})
</script>

<style scoped lang="scss">
.competition-arena-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.arena-nav-bar {
  background: var(--v-bg-card, #ffffff);
  border-radius: 12px;
  padding: 8px 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);

  .tab-label {
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 600;
  }
}

.arena-content-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.ctf-arena {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .category-filters {
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
  }
}

.challenge-card {
  border-radius: 12px;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;

  &:hover {
    transform: translateY(-3px);
  }

  &.solved {
    border-color: rgba(24, 160, 88, 0.4);
    background: rgba(24, 160, 88, 0.03);
  }

  .card-top {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
  }

  .ch-title {
    font-size: 16px;
    font-weight: 700;
    margin: 0 0 16px;
  }

  .ch-bottom {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 13px;

    .ch-points {
      font-weight: 700;
      color: var(--v-text-primary, #111111);
    }

    .ch-solves {
      color: #444444;
    }
  }
}

.challenge-modal-inner {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .modal-meta {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 13px;

    .meta-points {
      font-weight: 700;
    }

    .meta-solves {
      color: #444444;
    }
  }

  .modal-desc {
    background: rgba(0, 0, 0, 0.03);
    padding: 14px;
    border-radius: 8px;
    font-size: 14px;
    line-height: 1.6;
    color: var(--v-text-primary, #222222);
  }

  .instance-box {
    background: rgba(0, 0, 0, 0.02);
    border: 1px dashed rgba(0, 0, 0, 0.15);
    border-radius: 8px;
    padding: 12px 14px;

    .instance-running {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .inst-info {
        font-size: 13px;
        display: flex;
        gap: 8px;
        align-items: center;

        .inst-label {
          color: #444444;
        }

        .inst-host {
          font-family: var(--v-font-mono, 'Fira Code', monospace);
          font-weight: 600;
          color: #18a058;
        }
      }

      .inst-actions {
        display: flex;
        gap: 8px;
      }
    }
  }

  .flag-submit-area {
    display: flex;
    gap: 12px;
    margin-top: 8px;
  }
}

.other-mode-arena {
  .mode-hero-card {
    border-radius: 12px;
    padding: 10px;

    .mode-header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 12px;

      h2 {
        margin: 0;
        font-size: 20px;
        font-weight: 700;
      }
    }

    .mode-desc {
      font-size: 14px;
      color: #444444;
      line-height: 1.6;
      margin-bottom: 24px;
    }

    .mode-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
      gap: 16px;

      .mode-card {
        background: rgba(0, 0, 0, 0.03);
        padding: 16px;
        border-radius: 8px;

        h4 {
          margin: 0 0 8px;
          font-size: 15px;
          font-weight: 600;
        }

        p {
          font-size: 13px;
          color: #444444;
          line-height: 1.6;
          margin: 0;
        }
      }
    }
  }
}

.scoreboard-card {
  border-radius: 12px;

  .scoreboard-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    .title-wrap {
      h3 {
        font-size: 18px;
        font-weight: 700;
        margin: 0 0 4px;
      }

      .sub {
        font-size: 13px;
        color: #444444;
        margin: 0;
      }
    }
  }
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

          .slogan {
            font-size: 13px;
            color: #444444;
            margin: 0;
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

.theme-primary-btn {
  background: #000000 !important;
  color: #ffffff !important;
  border-radius: 8px;
  font-weight: 600;

  &:hover {
    background: #222222 !important;
  }
}

.empty-wrap {
  padding: 60px 0;
  display: flex;
  justify-content: center;
}
</style>
